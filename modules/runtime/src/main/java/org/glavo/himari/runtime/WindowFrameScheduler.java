package org.glavo.himari.runtime;

import org.glavo.himari.platform.api.PlatformWindow;
import org.glavo.himari.platform.api.WindowEvent;
import org.glavo.himari.platform.api.WindowEventType;
import org.glavo.himari.platform.api.WindowId;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/// Coalesces explicit frame requests for one platform window without affecting other windows.
///
/// [#requestFrame()] may be called from any thread. A request that starts an idle coalescing cycle
/// obtains one host redraw; all requests accepted before that redraw callback begins are represented
/// by one [FrameTick]. Requests made while the frame callback is running obtain exactly one
/// follow-up redraw after the callback returns.
@NotNullByDefault
public final class WindowFrameScheduler implements AutoCloseable {
    /// The application scheduler that owns routing and diagnostics.
    private final UiScheduler parent;

    /// The borrowed platform window receiving redraw requests.
    private final PlatformWindow window;

    /// The application frame callback executed on the owner context.
    private final FrameCallback callback;

    /// The monitor protecting request generations and lifecycle state.
    private final Object lock = new Object();

    /// The latest accepted explicit request generation, guarded by [#lock].
    private long requestedGeneration;

    /// The latest request generation represented by a dispatched frame, guarded by [#lock].
    private long deliveredGeneration;

    /// Explicit requests waiting to be represented by a frame, guarded by [#lock].
    private long pendingRequestCount;

    /// The last timestamp sampled for an admitted frame, or `-1` before the first frame.
    private long lastTimestampNanos = -1L;

    /// Whether a requested host redraw has not begun scheduler dispatch, guarded by [#lock].
    private boolean hostRedrawOutstanding;

    /// Whether the owner thread is executing this scheduler's frame callback, guarded by [#lock].
    private boolean frameRunning;

    /// Whether this scheduler permanently stopped accepting frame requests, guarded by [#lock].
    private boolean closed;

    /// Creates an unregistered window scheduler for its parent.
    ///
    /// @param parent the owning application scheduler
    /// @param window the borrowed open window
    /// @param callback the bounded frame callback
    WindowFrameScheduler(UiScheduler parent, PlatformWindow window, FrameCallback callback) {
        this.parent = Objects.requireNonNull(parent, "parent");
        this.window = Objects.requireNonNull(window, "window");
        this.callback = Objects.requireNonNull(callback, "callback");
    }

    /// Returns the borrowed platform window represented by this scheduler.
    ///
    /// @return the target window
    public PlatformWindow window() {
        return window;
    }

    /// Requests a future frame and returns its monotonically increasing generation.
    ///
    /// This method may be called from any thread. When no redraw or frame is active, failure to
    /// submit the host redraw rejects this request and rolls back its generation. Requests accepted
    /// while a frame is running are retained until the scheduler attempts one follow-up redraw.
    ///
    /// @return the positive generation assigned to this request
    /// @throws IllegalStateException if this scheduler, its parent, its event loop, or its window is
    /// closed, or if request generations are exhausted
    public long requestFrame() {
        parent.checkAccepting();
        synchronized (lock) {
            parent.checkAccepting();
            checkOpenUnderLock();
            if (requestedGeneration == Long.MAX_VALUE) {
                throw new IllegalStateException("Window frame request generations are exhausted");
            }
            requestedGeneration++;
            pendingRequestCount++;
            long generation = requestedGeneration;
            if (hostRedrawOutstanding || frameRunning) {
                return generation;
            }

            hostRedrawOutstanding = true;
            try {
                window.requestRedraw();
            } catch (RuntimeException | Error failure) {
                hostRedrawOutstanding = false;
                pendingRequestCount--;
                requestedGeneration--;
                throw failure;
            }
            return generation;
        }
    }

    /// Returns one thread-safe immutable snapshot of this scheduler's coalescing state.
    ///
    /// @return the frame scheduler snapshot
    public WindowFrameSchedulerSnapshot snapshot() {
        synchronized (lock) {
            return new WindowFrameSchedulerSnapshot(
                    window.id(),
                    requestedGeneration,
                    deliveredGeneration,
                    pendingRequestCount,
                    hostRedrawOutstanding,
                    frameRunning,
                    closed
            );
        }
    }

    /// Permanently stops accepting requests and drops any frame not already admitted.
    ///
    /// This method may be called from any thread and is idempotent. It permanently retires frame
    /// scheduling for this open window, does not cancel a platform redraw already queued by the
    /// host, close the window, or interrupt a callback already running. A routed redraw after
    /// closure is ignored.
    @Override
    public void close() {
        boolean changed;
        synchronized (lock) {
            changed = closeUnderLock();
        }
        if (changed) {
            parent.unregister(this);
        }
    }

    /// Dispatches one matching host redraw on the owner context.
    ///
    /// @param event the matching redraw event
    /// @return whether a frame was admitted
    boolean dispatch(WindowEvent event) {
        Objects.requireNonNull(event, "event");
        parent.eventLoop().checkOwnerThread();
        if (event.type() != WindowEventType.REDRAW_REQUESTED) {
            throw new IllegalArgumentException("Only a redraw event can dispatch a frame");
        }
        WindowId windowId = window.id();
        if (!event.snapshot().id().equals(windowId)) {
            throw new IllegalArgumentException("A redraw event must match the scheduled window");
        }

        FrameTick tick;
        synchronized (lock) {
            if (closed) {
                return false;
            }
            if (frameRunning) {
                throw new IllegalStateException("A window frame callback cannot be reentered");
            }
            hostRedrawOutstanding = false;
            frameRunning = true;
            long coalesced = pendingRequestCount;
            pendingRequestCount = 0L;
            if (coalesced != 0L) {
                deliveredGeneration = requestedGeneration;
            }
            long timestampNanos = parent.eventLoop().clock().nowNanos();
            if (timestampNanos < lastTimestampNanos) {
                frameRunning = false;
                throw new IllegalStateException("The platform frame clock moved backwards");
            }
            lastTimestampNanos = timestampNanos;
            tick = new FrameTick(
                    windowId,
                    timestampNanos,
                    event.sequence(),
                    deliveredGeneration,
                    coalesced
            );
        }

        @Nullable Throwable callbackFailure = null;
        @Nullable Throwable redrawFailure = null;
        try {
            callback.runFrame(tick);
        } catch (RuntimeException | Error failure) {
            callbackFailure = failure;
        } finally {
            synchronized (lock) {
                frameRunning = false;
                if (!closed && pendingRequestCount != 0L && !hostRedrawOutstanding) {
                    hostRedrawOutstanding = true;
                    try {
                        window.requestRedraw();
                    } catch (RuntimeException | Error failure) {
                        hostRedrawOutstanding = false;
                        pendingRequestCount = 0L;
                        redrawFailure = failure;
                    }
                }
            }
            parent.recordFrame(tick, callbackFailure);
            if (redrawFailure != null) {
                parent.recordRedrawFailure(windowId, redrawFailure);
            }
        }
        return true;
    }

    /// Closes this scheduler after the parent has already removed all registrations.
    void closeFromParent() {
        synchronized (lock) {
            closeUnderLock();
        }
    }

    /// Applies the idempotent closed-state transition while holding [#lock].
    ///
    /// @return whether this call changed the lifecycle state
    private boolean closeUnderLock() {
        if (closed) {
            return false;
        }
        closed = true;
        pendingRequestCount = 0L;
        hostRedrawOutstanding = false;
        return true;
    }

    /// Verifies that this scheduler and window still accept frame requests while holding [#lock].
    ///
    /// @throws IllegalStateException if either object is closed
    private void checkOpenUnderLock() {
        if (closed) {
            throw new IllegalStateException("Window frame scheduler is closed");
        }
        if (window.isClosed()) {
            throw new IllegalStateException("Window is closed");
        }
    }
}

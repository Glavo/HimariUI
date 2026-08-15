package org.glavo.himari.runtime;

import org.glavo.himari.platform.api.PlatformEventLoop;
import org.glavo.himari.platform.api.PlatformWindow;
import org.glavo.himari.platform.api.ScheduledTask;
import org.glavo.himari.platform.api.WindowEvent;
import org.glavo.himari.platform.api.WindowEventType;
import org.glavo.himari.platform.api.WindowId;
import org.glavo.himari.state.ExternalCommitFailure;
import org.glavo.himari.state.ExternalCommitResult;
import org.glavo.himari.state.ExternalStateCommitQueue;
import org.glavo.himari.state.StateDomain;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.RejectedExecutionException;

/// Coordinates one application's owner-context state batches and window-scoped frame schedulers.
///
/// The scheduler must be created on the shared owner thread of its [PlatformEventLoop] and
/// [StateDomain]. [#enqueueStateUpdate(Runnable)] accepts bounded work from any thread and posts at
/// most one owner callback for each detached batch. Every window receives an independent
/// [WindowFrameScheduler]; callers route ordered platform events through
/// [#handleWindowEvent(WindowEvent)].
///
/// This object does not own or close the event loop, state domain, or platform windows. The host
/// event loop must remain usable until this scheduler has been closed.
@NotNullByDefault
public final class UiScheduler implements AutoCloseable {
    /// The default maximum number of state callbacks waiting in scheduler-owned ingress.
    public static final int DEFAULT_MAX_PENDING_STATE_UPDATES = 4_096;

    /// The default maximum number of retained contained failures.
    public static final int DEFAULT_FAILURE_CAPACITY = 256;

    /// The platform execution context on which state drains and frame callbacks run.
    private final PlatformEventLoop eventLoop;

    /// The application state domain sharing the event-loop owner thread.
    private final StateDomain stateDomain;

    /// The maximum number of callbacks retained before one scheduled state drain detaches them.
    private final int maxPendingStateUpdates;

    /// The maximum number of failures retained for diagnostics.
    private final int failureCapacity;

    /// The monitor protecting queues, registrations, task ownership, and lifetime counters.
    private final Object lock = new Object();

    /// State callbacks waiting to be transferred to the domain external-commit queue.
    private final ArrayDeque<Runnable> pendingStateUpdates = new ArrayDeque<>();

    /// Active frame schedulers in registration order, keyed by stable window identifier.
    private final LinkedHashMap<WindowId, WindowFrameScheduler> windowSchedulers =
            new LinkedHashMap<>();

    /// Open-window identifiers whose one permitted frame scheduler was independently closed.
    private final LinkedHashSet<WindowId> retiredWindowIds = new LinkedHashSet<>();

    /// The bounded oldest-to-newest retained diagnostic sequence.
    private final ArrayDeque<UiSchedulerFailure> failures = new ArrayDeque<>();

    /// The host task assigned to the current pending state batch, or `null` when none is assigned.
    private @Nullable ScheduledTask stateDrainTask;

    /// Whether a detached scheduler-owned batch is executing on the owner thread.
    private boolean drainingStateUpdates;

    /// Whether this scheduler permanently stopped accepting work.
    private volatile boolean closed;

    /// The latest allocated diagnostic sequence, guarded by [#lock].
    private long lastFailureSequence;

    /// Failures evicted from or rejected by the bounded diagnostic buffer, guarded by [#lock].
    private long droppedFailures;

    /// State drain attempts begun on the owner context, guarded by [#lock].
    private long stateBatches;

    /// State callbacks reported as attempted by completed drains, guarded by [#lock].
    private long stateUpdates;

    /// State callbacks reported as failed by completed drains, guarded by [#lock].
    private long stateUpdateFailures;

    /// Outer state transaction failures contained by this scheduler, guarded by [#lock].
    private long stateBatchFailures;

    /// Frame callbacks attempted across all registered windows, guarded by [#lock].
    private long frames;

    /// Explicit frame requests consumed by attempted frames, guarded by [#lock].
    private long coalescedFrameRequests;

    /// Frame callback failures contained by this scheduler, guarded by [#lock].
    private long frameCallbackFailures;

    /// Accepted follow-up requests lost to a host redraw failure, guarded by [#lock].
    private long redrawRequestFailures;

    /// Creates a scheduler with the default state-ingress and diagnostic capacities.
    ///
    /// @param eventLoop the platform event loop owned by the calling thread
    /// @param stateDomain the application state domain owned by the calling thread
    /// @throws IllegalStateException if the caller does not own both objects or the event loop is
    /// closed
    public UiScheduler(PlatformEventLoop eventLoop, StateDomain stateDomain) {
        this(
                eventLoop,
                stateDomain,
                DEFAULT_MAX_PENDING_STATE_UPDATES,
                DEFAULT_FAILURE_CAPACITY
        );
    }

    /// Creates a scheduler with explicit bounded capacities.
    ///
    /// @param eventLoop the platform event loop owned by the calling thread
    /// @param stateDomain the application state domain owned by the calling thread
    /// @param maxPendingStateUpdates the positive maximum pending scheduler-owned state callbacks
    /// @param failureCapacity the positive maximum retained contained failures
    /// @throws IllegalArgumentException if either capacity is not positive
    /// @throws IllegalStateException if the caller does not own both objects or the event loop is
    /// closed
    public UiScheduler(
            PlatformEventLoop eventLoop,
            StateDomain stateDomain,
            int maxPendingStateUpdates,
            int failureCapacity
    ) {
        this.eventLoop = Objects.requireNonNull(eventLoop, "eventLoop");
        this.stateDomain = Objects.requireNonNull(stateDomain, "stateDomain");
        if (maxPendingStateUpdates <= 0) {
            throw new IllegalArgumentException("maxPendingStateUpdates must be positive");
        }
        if (failureCapacity <= 0) {
            throw new IllegalArgumentException("failureCapacity must be positive");
        }
        eventLoop.checkOwnerThread();
        stateDomain.checkOwnerThread();
        if (eventLoop.isClosed()) {
            throw new IllegalStateException("Cannot create a UI scheduler for a closed event loop");
        }
        this.maxPendingStateUpdates = maxPendingStateUpdates;
        this.failureCapacity = failureCapacity;
    }

    /// Returns the platform event loop on which this scheduler dispatches work.
    ///
    /// @return the borrowed event loop
    public PlatformEventLoop eventLoop() {
        return eventLoop;
    }

    /// Returns the application state domain mutated by scheduled state updates.
    ///
    /// @return the borrowed state domain
    public StateDomain stateDomain() {
        return stateDomain;
    }

    /// Enqueues one bounded state callback for a future owner-context transaction batch.
    ///
    /// This method may be called from any thread and never invokes `update` on the caller. Callbacks
    /// detached by the same scheduler drain are appended to the domain's
    /// [ExternalStateCommitQueue] in FIFO order and publish together as at most one state epoch.
    /// Work submitted after detachment is assigned to a later host callback.
    ///
    /// @param update the bounded synchronous state callback
    /// @throws RejectedExecutionException if the pending scheduler ingress is full
    /// @throws IllegalStateException if this scheduler or its event loop is closed
    public void enqueueStateUpdate(Runnable update) {
        Objects.requireNonNull(update, "update");
        synchronized (lock) {
            checkAcceptingUnderLock();
            if (pendingStateUpdates.size() == maxPendingStateUpdates) {
                throw new RejectedExecutionException("UI scheduler state ingress is full");
            }
            pendingStateUpdates.addLast(update);
            if (stateDrainTask != null) {
                return;
            }
            try {
                stateDrainTask = eventLoop.post(this::drainStateUpdates);
            } catch (RuntimeException | Error failure) {
                Runnable removed = pendingStateUpdates.removeLast();
                if (removed != update) {
                    throw new IllegalStateException("State ingress rollback lost FIFO ownership", failure);
                }
                throw failure;
            }
        }
    }

    /// Registers an independent frame scheduler for one open platform window.
    ///
    /// The caller must route that window's events to [#handleWindowEvent(WindowEvent)]. A stable
    /// window identifier may have only one frame scheduler during its open lifetime. Closing that
    /// scheduler retires scheduling for the window so a stale host redraw cannot reach a replacement.
    ///
    /// @param window the open window owned by this scheduler's platform session
    /// @param callback the bounded frame callback
    /// @return the registered per-window scheduler
    /// @throws IllegalArgumentException if the identifier is already registered
    /// @throws IllegalStateException if called outside the owner thread, after scheduler closure,
    /// after scheduling was retired for this window, or for a closed window
    public WindowFrameScheduler createFrameScheduler(
            PlatformWindow window,
            FrameCallback callback
    ) {
        Objects.requireNonNull(window, "window");
        Objects.requireNonNull(callback, "callback");
        eventLoop.checkOwnerThread();
        stateDomain.checkOwnerThread();
        synchronized (lock) {
            checkAcceptingUnderLock();
            if (window.isClosed()) {
                throw new IllegalStateException("Cannot schedule frames for a closed window");
            }
            WindowId windowId = window.id();
            if (windowSchedulers.containsKey(windowId)) {
                throw new IllegalArgumentException(
                        "A frame scheduler is already registered for window " + windowId.value()
                );
            }
            if (retiredWindowIds.contains(windowId)) {
                throw new IllegalStateException(
                        "Frame scheduling already ended for window " + windowId.value()
                );
            }
            WindowFrameScheduler scheduler = new WindowFrameScheduler(this, window, callback);
            windowSchedulers.put(windowId, scheduler);
            return scheduler;
        }
    }

    /// Routes a redraw or close event to its matching window scheduler.
    ///
    /// Other event types are deliberately ignored. Routing never broadcasts invalidation or frame
    /// work to unrelated windows.
    ///
    /// @param event the ordered platform event
    /// @return whether a registered window scheduler consumed the event
    /// @throws IllegalStateException if called outside the owner thread
    public boolean handleWindowEvent(WindowEvent event) {
        Objects.requireNonNull(event, "event");
        eventLoop.checkOwnerThread();
        WindowEventType type = event.type();
        if (type != WindowEventType.REDRAW_REQUESTED && type != WindowEventType.CLOSED) {
            return false;
        }
        @Nullable WindowFrameScheduler scheduler;
        synchronized (lock) {
            scheduler = windowSchedulers.get(event.snapshot().id());
        }
        if (scheduler == null) {
            if (type == WindowEventType.CLOSED) {
                synchronized (lock) {
                    retiredWindowIds.remove(event.snapshot().id());
                }
            }
            return false;
        }
        if (type == WindowEventType.CLOSED) {
            scheduler.close();
            return true;
        }
        return scheduler.dispatch(event);
    }

    /// Returns one thread-safe immutable snapshot of queues, registrations, and lifetime counters.
    ///
    /// @return the scheduler snapshot
    public UiSchedulerSnapshot snapshot() {
        synchronized (lock) {
            return new UiSchedulerSnapshot(
                    closed,
                    pendingStateUpdates.size(),
                    stateDrainTask != null,
                    windowSchedulers.size(),
                    failures.size(),
                    droppedFailures,
                    stateBatches,
                    stateUpdates,
                    stateUpdateFailures,
                    stateBatchFailures,
                    frames,
                    coalescedFrameRequests,
                    frameCallbackFailures,
                    redrawRequestFailures
            );
        }
    }

    /// Returns an immutable oldest-to-newest snapshot of retained contained failures.
    ///
    /// @return the retained failures
    public @Unmodifiable List<UiSchedulerFailure> failures() {
        synchronized (lock) {
            return List.copyOf(failures);
        }
    }

    /// Removes and returns all retained failures in diagnostic sequence order.
    ///
    /// Concurrently recorded failures either appear in this result or remain for a later drain.
    ///
    /// @return the removed failures
    public @Unmodifiable List<UiSchedulerFailure> drainFailures() {
        synchronized (lock) {
            List<UiSchedulerFailure> result = List.copyOf(failures);
            failures.clear();
            return result;
        }
    }

    /// Stops accepting work, closes registered frame schedulers, and settles pending state ingress.
    ///
    /// Closure is idempotent and must run on the owner thread outside every state transaction. A
    /// pending host state task is cancelled, its callbacks are synchronously
    /// transferred to and drained from the domain external queue, and outer publication failure is
    /// contained in diagnostics. A drain may also execute work already pending in that shared
    /// domain queue. The event loop, state domain, and windows remain owned by their callers.
    ///
    /// @throws IllegalStateException if called outside either owner context or from a state callback
    /// active for the state domain
    @Override
    public void close() {
        eventLoop.checkOwnerThread();
        stateDomain.checkOwnerThread();
        @Nullable ScheduledTask task;
        List<Runnable> detachedUpdates;
        List<WindowFrameScheduler> detachedSchedulers;
        synchronized (lock) {
            if (closed) {
                return;
            }
            if (drainingStateUpdates) {
                throw new IllegalStateException("A UI scheduler cannot close from its state drain");
            }
            if (stateDomain.hasActiveTransaction()) {
                throw new IllegalStateException("A UI scheduler cannot close inside a state transaction");
            }
            closed = true;
            task = stateDrainTask;
            stateDrainTask = null;
            detachedUpdates = List.copyOf(pendingStateUpdates);
            pendingStateUpdates.clear();
            detachedSchedulers = List.copyOf(windowSchedulers.values());
            windowSchedulers.clear();
            retiredWindowIds.clear();
        }
        if (task != null) {
            task.cancel();
        }
        for (WindowFrameScheduler scheduler : detachedSchedulers) {
            scheduler.closeFromParent();
        }
        if (!detachedUpdates.isEmpty()) {
            drainStateBatch(detachedUpdates);
        }
    }

    /// Verifies that this scheduler and its borrowed event loop still accept work.
    ///
    /// @throws IllegalStateException if either object is closed
    void checkAccepting() {
        if (closed) {
            throw new IllegalStateException("UI scheduler is closed");
        }
        if (eventLoop.isClosed()) {
            throw new IllegalStateException("UI scheduler event loop is closed");
        }
    }

    /// Removes one exact window scheduler after independent closure.
    ///
    /// @param scheduler the scheduler to remove
    void unregister(WindowFrameScheduler scheduler) {
        synchronized (lock) {
            WindowId windowId = scheduler.window().id();
            if (windowSchedulers.remove(windowId, scheduler)
                    && !closed
                    && !scheduler.window().isClosed()) {
                retiredWindowIds.add(windowId);
            }
        }
    }

    /// Records one attempted frame and an optional contained callback failure.
    ///
    /// @param tick the dispatched frame record
    /// @param failure the contained callback failure, or `null`
    void recordFrame(FrameTick tick, @Nullable Throwable failure) {
        synchronized (lock) {
            frames = saturatedIncrement(frames);
            coalescedFrameRequests = saturatedAdd(
                    coalescedFrameRequests,
                    tick.coalescedRequestCount()
            );
            if (failure != null) {
                frameCallbackFailures = saturatedIncrement(frameCallbackFailures);
                recordFailureUnderLock(
                        UiSchedulerFailureKind.FRAME_CALLBACK,
                        tick.windowId(),
                        tick.eventSequence(),
                        failure
                );
            }
        }
    }

    /// Records accepted requests discarded after a deferred host redraw call failed.
    ///
    /// @param windowId the affected window
    /// @param failure the host failure
    void recordRedrawFailure(WindowId windowId, Throwable failure) {
        synchronized (lock) {
            redrawRequestFailures = saturatedIncrement(redrawRequestFailures);
            recordFailureUnderLock(
                    UiSchedulerFailureKind.REDRAW_REQUEST,
                    windowId,
                    -1L,
                    failure
            );
        }
    }

    /// Detaches the scheduler-owned ingress assigned to the running host callback.
    private void drainStateUpdates() {
        eventLoop.checkOwnerThread();
        stateDomain.checkOwnerThread();
        List<Runnable> detached;
        synchronized (lock) {
            stateDrainTask = null;
            if (closed || pendingStateUpdates.isEmpty()) {
                return;
            }
            detached = List.copyOf(pendingStateUpdates);
            pendingStateUpdates.clear();
            drainingStateUpdates = true;
        }
        try {
            drainStateBatch(detached);
        } finally {
            synchronized (lock) {
                drainingStateUpdates = false;
            }
        }
    }

    /// Appends and drains one detached scheduler batch through the domain external queue.
    ///
    /// @param detached the nonempty FIFO scheduler batch
    private void drainStateBatch(@Unmodifiable List<Runnable> detached) {
        ExternalStateCommitQueue queue = stateDomain.externalCommits();
        for (Runnable update : detached) {
            queue.enqueue(update);
        }
        synchronized (lock) {
            stateBatches = saturatedIncrement(stateBatches);
        }
        try {
            ExternalCommitResult result = queue.drain();
            synchronized (lock) {
                stateUpdates = saturatedAdd(stateUpdates, result.attemptedCount());
                stateUpdateFailures = saturatedAdd(
                        stateUpdateFailures,
                        result.failures().size()
                );
                for (ExternalCommitFailure failure : result.failures()) {
                    recordFailureUnderLock(
                            UiSchedulerFailureKind.STATE_UPDATE,
                            null,
                            failure.batchIndex(),
                            failure.cause()
                    );
                }
            }
        } catch (RuntimeException | Error failure) {
            synchronized (lock) {
                stateUpdates = saturatedAdd(stateUpdates, detached.size());
                stateBatchFailures = saturatedIncrement(stateBatchFailures);
                recordFailureUnderLock(
                        UiSchedulerFailureKind.STATE_BATCH,
                        null,
                        -1L,
                        failure
                );
            }
        }
    }

    /// Verifies acceptance while holding [#lock].
    ///
    /// @throws IllegalStateException if this scheduler or its event loop is closed
    private void checkAcceptingUnderLock() {
        checkAccepting();
    }

    /// Appends one failure to the bounded diagnostic buffer while holding [#lock].
    ///
    /// @param kind the failure category
    /// @param windowId the affected window, or `null`
    /// @param contextSequence the batch or event context
    /// @param cause the original failure
    private void recordFailureUnderLock(
            UiSchedulerFailureKind kind,
            @Nullable WindowId windowId,
            long contextSequence,
            Throwable cause
    ) {
        if (lastFailureSequence == Long.MAX_VALUE) {
            droppedFailures = saturatedIncrement(droppedFailures);
            return;
        }
        lastFailureSequence++;
        if (failures.size() == failureCapacity) {
            failures.removeFirst();
            droppedFailures = saturatedIncrement(droppedFailures);
        }
        failures.addLast(new UiSchedulerFailure(
                lastFailureSequence,
                kind,
                windowId,
                contextSequence,
                cause
        ));
    }

    /// Adds a nonnegative delta without allowing diagnostic counters to wrap.
    ///
    /// @param value the current nonnegative value
    /// @param delta the nonnegative delta
    /// @return the exact sum or [Long#MAX_VALUE] on overflow
    private static long saturatedAdd(long value, long delta) {
        if (delta < 0L) {
            throw new IllegalArgumentException("A scheduler counter delta must be non-negative");
        }
        if (Long.MAX_VALUE - value < delta) {
            return Long.MAX_VALUE;
        }
        return value + delta;
    }

    /// Increments a lifetime counter without allowing it to wrap.
    ///
    /// @param value the current nonnegative value
    /// @return the incremented value or [Long#MAX_VALUE]
    private static long saturatedIncrement(long value) {
        return value == Long.MAX_VALUE ? Long.MAX_VALUE : value + 1L;
    }
}

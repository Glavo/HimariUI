package org.glavo.himari.platform.windows;

import org.glavo.himari.platform.api.DisplayEventHandler;
import org.glavo.himari.platform.api.DisplayTopologySnapshot;
import org.glavo.himari.platform.api.FrameClock;
import org.glavo.himari.platform.api.ListenerRegistration;
import org.glavo.himari.platform.api.LogicalRect;
import org.glavo.himari.platform.api.PhysicalSize;
import org.glavo.himari.platform.api.PlatformEventLoop;
import org.glavo.himari.platform.api.PlatformSession;
import org.glavo.himari.platform.api.SurfaceDescriptor;
import org.glavo.himari.platform.api.SurfaceId;
import org.glavo.himari.platform.api.SurfaceKind;
import org.glavo.himari.platform.api.SurfaceRole;
import org.glavo.himari.platform.api.WindowConfiguration;
import org.glavo.himari.platform.api.WindowEvent;
import org.glavo.himari.platform.api.WindowEventHandler;
import org.glavo.himari.platform.api.WindowEventType;
import org.glavo.himari.platform.api.WindowId;
import org.glavo.himari.platform.api.WindowLifecycle;
import org.glavo.himari.platform.api.WindowRequest;
import org.glavo.himari.platform.api.WindowSnapshot;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;

/// Owns one Windows desktop session implemented through generated FFM bindings.
@NotNullByDefault
public final class WindowsPlatform implements PlatformSession<WindowsWindow>, AutoCloseable {
    /// Shared system libraries.
    private final WindowsLibraries libraries;

    /// Host-driven event loop.
    private final WindowsEventLoop eventLoop;

    /// Primary display snapshot.
    private volatile DisplayTopologySnapshot topology;

    /// Open windows.
    private final LinkedHashMap<WindowId, WindowsWindow> windows = new LinkedHashMap<>();

    /// Window handlers.
    private final LinkedHashMap<WindowId, WindowEventHandler> handlers = new LinkedHashMap<>();

    /// Display listeners.
    private final CopyOnWriteArrayList<DisplayEventHandler> displayHandlers = new CopyOnWriteArrayList<>();

    /// Next window id.
    private long nextWindowId = 1L;

    /// Next event sequence.
    private long nextEventSequence = 1L;

    /// Whether the session is closed.
    private boolean closed;

    /// Creates a session.
    ///
    /// @param libraries the libraries
    /// @param eventLoop the loop
    WindowsPlatform(WindowsLibraries libraries, WindowsEventLoop eventLoop) {
        this.libraries = libraries;
        this.eventLoop = eventLoop;
        this.topology = new DisplayTopologySnapshot(0L, List.of(libraries.primaryDisplay(96)));
    }

    /// {@inheritDoc}
    @Override
    public FrameClock clock() {
        return eventLoop.clock();
    }

    /// {@inheritDoc}
    @Override
    public PlatformEventLoop eventLoop() {
        return eventLoop;
    }

    /// {@inheritDoc}
    @Override
    public DisplayTopologySnapshot displayTopology() {
        return topology;
    }

    /// {@inheritDoc}
    @Override
    public ListenerRegistration addDisplayEventHandler(DisplayEventHandler handler) {
        Objects.requireNonNull(handler, "handler");
        displayHandlers.add(handler);
        return new ListenerRegistration() {
            private boolean cancelled;

            @Override
            public boolean cancel() {
                if (cancelled) {
                    return false;
                }
                cancelled = true;
                return displayHandlers.remove(handler);
            }

            @Override
            public boolean isCancelled() {
                return cancelled;
            }
        };
    }

    /// {@inheritDoc}
    @Override
    public CompletionStage<WindowsWindow> createWindow(WindowRequest request, WindowEventHandler eventHandler) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(eventHandler, "eventHandler");
        if (eventLoop.isOwnerThread()) {
            return CompletableFuture.completedFuture(createWindowNow(request, eventHandler));
        }
        CompletableFuture<WindowsWindow> future = new CompletableFuture<>();
        eventLoop.post(() -> {
            try {
                future.complete(createWindowNow(request, eventHandler));
            } catch (RuntimeException | Error failure) {
                future.completeExceptionally(failure);
            }
        });
        return future;
    }

    /// {@inheritDoc}
    @Override
    public CompletionStage<Void> closeAsync() {
        if (eventLoop.isOwnerThread()) {
            close();
            return CompletableFuture.completedFuture(null);
        }
        CompletableFuture<Void> future = new CompletableFuture<>();
        eventLoop.post(() -> {
            try {
                close();
                future.complete(null);
            } catch (RuntimeException | Error failure) {
                future.completeExceptionally(failure);
            }
        });
        return future;
    }

    /// {@inheritDoc}
    @Override
    public boolean isClosed() {
        return closed;
    }

    /// Pumps native messages and ready Java callbacks.
    public void pump() {
        eventLoop.checkOwnerThread();
        libraries.pumpThreadMessages();
        consumeHostInput();
        eventLoop.runUntilIdle();
    }

    /// Pumps until every HWND is closed or `WM_QUIT` arrives.
    ///
    /// After each drain this method blocks in `WaitMessage` when windows remain open. Callers must
    /// close windows from `CLOSE_REQUESTED` handlers or post close work before the wait; otherwise
    /// the call blocks until a thread message arrives.
    public void pumpUntilClosed() {
        eventLoop.checkOwnerThread();
        while (!closed && openWindowCount() > 0) {
            if (!libraries.pumpThreadMessages()) {
                consumeHostInput();
                eventLoop.runUntilIdle();
                return;
            }
            consumeHostInput();
            eventLoop.runUntilIdle();
            if (closed || openWindowCount() == 0) {
                return;
            }
            if (!libraries.waitForThreadMessage()) {
                return;
            }
        }
    }

    /// Drains WndProc input and host geometry from every open window.
    private void consumeHostInput() {
        for (WindowsWindow window : List.copyOf(windows.values())) {
            if (!window.isClosed()) {
                window.consumeNativeInput();
                publishHostGeometry(window);
            }
        }
    }

    /// Returns the session libraries.
    ///
    /// @return the libraries
    WindowsLibraries libraries() {
        return libraries;
    }

    /// Returns the number of windows that remain open.
    ///
    /// @return the open count
    public int openWindowCount() {
        int count = 0;
        for (WindowsWindow window : windows.values()) {
            if (!window.isClosed()) {
                count++;
            }
        }
        return count;
    }

    /// Closes every window and the session libraries.
    @Override
    public void close() {
        eventLoop.checkOwnerThread();
        if (closed) {
            return;
        }
        ArrayList<WindowsWindow> remaining = new ArrayList<>(windows.values());
        for (int index = remaining.size() - 1; index >= 0; index--) {
            closeWindow(remaining.get(index));
        }
        eventLoop.close();
        libraries.close();
        closed = true;
    }

    /// Creates a window on the owner thread.
    ///
    /// @param request the request
    /// @param eventHandler the handler
    /// @return the window
    WindowsWindow createWindowNow(WindowRequest request, WindowEventHandler eventHandler) {
        if (closed) {
            throw new IllegalStateException("Windows session is closed");
        }
        WindowConfiguration configuration = request.configuration();
        LogicalRect frame = configuration.frame();
        MemorySegment ownerHandle = MemorySegment.NULL;
        if (request.ownerId() != null) {
            WindowsWindow owner = windows.get(request.ownerId());
            if (owner == null || owner.isClosed()) {
                throw new IllegalArgumentException("Popup owner is not an open window");
            }
            ownerHandle = owner.nativeHandle();
        }
        WindowId id = new WindowId(nextWindowId++);
        WindowsNativeWindow nativeWindow = WindowsNativeWindow.create(
                libraries,
                configuration.title(),
                (int) Math.round(frame.x()),
                (int) Math.round(frame.y()),
                Math.max(1, (int) Math.round(frame.width())),
                Math.max(1, (int) Math.round(frame.height())),
                request.role() == SurfaceRole.POPUP,
                ownerHandle,
                new WindowLifecycleBridge(id)
        );
        if (configuration.visible()) {
            nativeWindow.show();
        }
        refreshTopology(nativeWindow.dpi());
        WindowSnapshot snapshot = snapshotOf(
                id,
                request.role(),
                request.ownerId(),
                configuration,
                nativeWindow,
                0L,
                WindowLifecycle.OPEN
        );
        WindowsWindow window = new WindowsWindow(this, nativeWindow, snapshot);
        windows.put(id, window);
        handlers.put(id, eventHandler);
        emit(window, WindowEventType.CREATED, snapshot, null);
        return window;
    }

    /// Applies a configuration on the owner thread.
    ///
    /// @param window the window
    /// @param configuration the configuration
    /// @return the new snapshot
    WindowSnapshot configureWindow(WindowsWindow window, WindowConfiguration configuration) {
        WindowSnapshot previous = window.snapshot();
        if (previous.configuration().equals(configuration)
                && previous.scaleFactor() == window.nativeWindow().scaleFactor()) {
            return previous;
        }
        LogicalRect frame = configuration.frame();
        window.nativeWindow().setBounds(
                (int) Math.round(frame.x()),
                (int) Math.round(frame.y()),
                Math.max(1, (int) Math.round(frame.width())),
                Math.max(1, (int) Math.round(frame.height()))
        );
        if (configuration.visible()) {
            window.nativeWindow().show();
        } else {
            window.nativeWindow().hide();
        }
        refreshTopology(window.nativeWindow().dpi());
        WindowSnapshot next = snapshotOf(
                previous.id(),
                previous.role(),
                previous.ownerId(),
                configuration,
                window.nativeWindow(),
                previous.configurationGeneration() + 1L,
                WindowLifecycle.OPEN
        );
        window.publish(next);
        emit(window, WindowEventType.CONFIGURATION_CHANGED, next, previous);
        return next;
    }

    /// Queues a redraw event.
    ///
    /// @param window the window
    void requestRedraw(WindowsWindow window) {
        emit(window, WindowEventType.REDRAW_REQUESTED, window.snapshot(), null);
    }

    /// Closes one window and its owned popups on the owner thread.
    ///
    /// @param window the window
    void closeWindow(WindowsWindow window) {
        WindowSnapshot previous = window.snapshot();
        if (previous.lifecycle() == WindowLifecycle.CLOSED) {
            return;
        }
        ArrayList<WindowsWindow> children = new ArrayList<>();
        for (WindowsWindow candidate : windows.values()) {
            if (window.id().equals(candidate.snapshot().ownerId())
                    && candidate.snapshot().lifecycle() != WindowLifecycle.CLOSED) {
                children.add(candidate);
            }
        }
        for (WindowsWindow child : children) {
            closeWindow(child);
        }
        WindowSnapshot next = new WindowSnapshot(
                previous.id(),
                previous.role(),
                previous.ownerId(),
                previous.configuration(),
                previous.effectiveFrame(),
                false,
                previous.surfaceSize(),
                previous.scaleFactor(),
                previous.displayId(),
                previous.surface(),
                previous.configurationGeneration() + 1L,
                WindowLifecycle.CLOSED
        );
        window.publish(next);
        window.nativeWindow().close();
        emit(window, WindowEventType.CLOSED, next, previous);
        windows.remove(window.id());
        handlers.remove(window.id());
        window.completeClose();
    }

    /// Publishes a host-driven geometry or DPI change observed during [#pump()].
    ///
    /// @param window the window
    private void publishHostGeometry(WindowsWindow window) {
        WindowSnapshot previous = window.snapshot();
        if (previous.lifecycle() != WindowLifecycle.OPEN) {
            return;
        }
        WindowsNativeWindow nativeWindow = window.nativeWindow();
        PhysicalSize surfaceSize = surfaceSizeOf(nativeWindow);
        if (previous.scaleFactor() == nativeWindow.scaleFactor()
                && previous.surfaceSize().equals(surfaceSize)) {
            return;
        }
        refreshTopology(nativeWindow.dpi());
        WindowSnapshot next = snapshotOf(
                previous.id(),
                previous.role(),
                previous.ownerId(),
                previous.configuration(),
                nativeWindow,
                previous.configurationGeneration() + 1L,
                WindowLifecycle.OPEN
        );
        window.publish(next);
        emit(window, WindowEventType.CONFIGURATION_CHANGED, next, previous);
    }

    /// Emits `CLOSE_REQUESTED` without destroying the HWND.
    ///
    /// @param id the window
    private void requestClose(WindowId id) {
        @Nullable WindowsWindow window = windows.get(id);
        if (window == null || window.isClosed()) {
            return;
        }
        emit(window, WindowEventType.CLOSE_REQUESTED, window.snapshot(), null);
    }

    /// Emits one window event.
    private void emit(
            WindowsWindow window,
            WindowEventType type,
            WindowSnapshot snapshot,
            @Nullable WindowSnapshot previous
    ) {
        WindowEvent event = new WindowEvent(
                nextEventSequence++,
                eventLoop.clock().nowNanos(),
                type,
                snapshot,
                previous
        );
        @Nullable WindowEventHandler handler = handlers.get(window.id());
        if (handler != null) {
            handler.handleWindowEvent(event);
        }
    }

    /// Rebuilds the primary display snapshot from the latest host metrics.
    ///
    /// @param dpi the DPI used to convert physical pixels to logical bounds
    private void refreshTopology(int dpi) {
        topology = new DisplayTopologySnapshot(topology.generation() + 1L, List.of(libraries.primaryDisplay(dpi)));
    }

    /// Builds a snapshot from the live HWND.
    private WindowSnapshot snapshotOf(
            WindowId id,
            SurfaceRole role,
            @Nullable WindowId ownerId,
            WindowConfiguration configuration,
            WindowsNativeWindow nativeWindow,
            long generation,
            WindowLifecycle lifecycle
    ) {
        LogicalRect frame = configuration.frame();
        return new WindowSnapshot(
                id,
                role,
                ownerId,
                configuration,
                frame,
                configuration.visible() && lifecycle == WindowLifecycle.OPEN,
                surfaceSizeOf(nativeWindow),
                nativeWindow.scaleFactor(),
                topology.displays().getFirst().id(),
                new SurfaceDescriptor(new SurfaceId(id.value()), role, SurfaceKind.HOST_PRESENTED),
                generation,
                lifecycle
        );
    }

    /// Returns a nonnegative client size, using one pixel when the HWND has not been sized yet.
    ///
    /// @param nativeWindow the HWND owner
    /// @return the surface size
    private static PhysicalSize surfaceSizeOf(WindowsNativeWindow nativeWindow) {
        return new PhysicalSize(
                Math.max(1, nativeWindow.clientWidth()),
                Math.max(1, nativeWindow.clientHeight())
        );
    }

    /// Forwards HWND lifecycle callbacks onto the owner thread's window table.
    @NotNullByDefault
    private final class WindowLifecycleBridge implements WindowsNativeWindow.Lifecycle {
        /// The window identity assigned before HWND creation.
        private final WindowId id;

        /// Creates one bridge.
        ///
        /// @param id the window identity
        private WindowLifecycleBridge(WindowId id) {
            this.id = id;
        }

        /// {@inheritDoc}
        @Override
        public void closeRequested() {
            requestClose(id);
        }

        /// {@inheritDoc}
        @Override
        public void modalTick() {
            eventLoop.drainReadyIfIdle();
        }

        /// {@inheritDoc}
        @Override
        public void destroyed() {
            @Nullable WindowsWindow window = windows.get(id);
            if (window != null && !window.isClosed()) {
                closeWindow(window);
            }
        }
    }
}

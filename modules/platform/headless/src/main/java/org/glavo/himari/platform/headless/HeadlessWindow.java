package org.glavo.himari.platform.headless;

import org.glavo.himari.platform.api.PlatformWindow;
import org.glavo.himari.platform.api.SurfaceDescriptor;
import org.glavo.himari.platform.api.SurfaceRole;
import org.glavo.himari.platform.api.WindowConfiguration;
import org.glavo.himari.platform.api.WindowEvent;
import org.glavo.himari.platform.api.WindowEventHandler;
import org.glavo.himari.platform.api.WindowId;
import org.glavo.himari.platform.api.WindowLifecycle;
import org.glavo.himari.platform.api.WindowSnapshot;
import org.glavo.himari.platform.api.WindowState;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/// Implements one deterministic virtual window owned by a [HeadlessPlatform].
///
/// The latest immutable snapshot is safe to read from any thread. Mutations are submitted to the
/// platform event loop and become visible atomically before their event callback executes.
@NotNullByDefault
public final class HeadlessWindow implements PlatformWindow {
    /// The owning Headless session, released when the close transition commits.
    private volatile @Nullable HeadlessPlatform platform;

    /// The stable window identifier.
    private final WindowId id;

    /// The stable software surface descriptor.
    private final SurfaceDescriptor surface;

    /// The immutable window role.
    private final SurfaceRole role;

    /// The application callback invoked for ordered window events, cleared after closed delivery.
    private @Nullable WindowEventHandler eventHandler;

    /// The shared close completion, completed when the close transition commits.
    private final CompletableFuture<Void> closeCompletion = new CompletableFuture<>();

    /// The latest committed immutable snapshot.
    private volatile WindowSnapshot snapshot;

    /// Whether one redraw event is queued but has not begun delivery; owner-thread confined.
    private boolean redrawPending;

    /// Creates a committed open virtual window.
    ///
    /// @param platform the owning session
    /// @param snapshot the initial open snapshot
    /// @param eventHandler the application event callback
    HeadlessWindow(
            HeadlessPlatform platform,
            WindowSnapshot snapshot,
            WindowEventHandler eventHandler
    ) {
        this.platform = platform;
        this.snapshot = snapshot;
        this.id = snapshot.id();
        this.surface = snapshot.surface();
        this.role = snapshot.role();
        this.eventHandler = eventHandler;
    }

    /// Returns the stable window identifier.
    ///
    /// @return the window identifier
    @Override
    public WindowId id() {
        return id;
    }

    /// Returns the stable software surface descriptor.
    ///
    /// @return the surface descriptor
    @Override
    public SurfaceDescriptor surface() {
        return surface;
    }

    /// Returns the latest committed immutable snapshot.
    ///
    /// @return the latest snapshot
    @Override
    public WindowSnapshot snapshot() {
        return snapshot;
    }

    /// Requests replacement of all application-controlled properties.
    ///
    /// @param configuration the complete replacement configuration
    /// @return a stage completed with the committed snapshot
    /// @throws IllegalArgumentException if a popup requests a non-normal state
    /// @throws IllegalStateException if the Headless session no longer accepts work
    @Override
    public CompletionStage<WindowSnapshot> configure(WindowConfiguration configuration) {
        Objects.requireNonNull(configuration, "configuration");
        if (role == SurfaceRole.POPUP && configuration.state() != WindowState.NORMAL) {
            throw new IllegalArgumentException("A popup window must use the normal state");
        }
        if (isClosed()) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("Cannot configure a closed Headless window")
            );
        }
        return owningPlatform().configureWindow(this, configuration);
    }

    /// Requests one coalesced redraw event.
    ///
    /// @throws IllegalStateException if the Headless session no longer accepts work
    @Override
    public void requestRedraw() {
        if (isClosed()) {
            return;
        }
        owningPlatform().requestRedraw(this);
    }

    /// Injects a host close request without closing the window.
    ///
    /// Each successful call produces one [org.glavo.himari.platform.api.WindowEventType#CLOSE_REQUESTED]
    /// event. The application may explicitly close the window from its callback.
    ///
    /// @throws IllegalStateException if the Headless session no longer accepts work
    public void injectCloseRequest() {
        if (isClosed()) {
            return;
        }
        owningPlatform().injectCloseRequest(this);
    }

    /// Explicitly closes this window and its popup descendants.
    ///
    /// @return the shared stage completed when the close transition commits
    /// @throws IllegalStateException if the Headless session no longer accepts work and the window
    /// has not already closed
    @Override
    public CompletionStage<Void> closeAsync() {
        if (isClosed()) {
            return closeCompletion;
        }
        owningPlatform().closeWindow(this);
        return closeCompletion;
    }

    /// Returns whether the close transition committed.
    ///
    /// @return whether this window is closed
    @Override
    public boolean isClosed() {
        return snapshot.lifecycle() == WindowLifecycle.CLOSED;
    }

    /// Returns the immutable role for owner-tree operations.
    ///
    /// @return the surface role
    SurfaceRole role() {
        return role;
    }

    /// Returns the current owner identifier, if this is a popup.
    ///
    /// @return the owner identifier, or `null`
    @Nullable WindowId ownerId() {
        return snapshot.ownerId();
    }

    /// Publishes a replacement immutable snapshot.
    ///
    /// @param replacement the replacement snapshot
    void publish(WindowSnapshot replacement) {
        snapshot = replacement;
    }

    /// Returns whether a redraw callback is queued.
    ///
    /// @return whether redraw delivery is pending
    boolean redrawPending() {
        return redrawPending;
    }

    /// Records whether redraw delivery is pending.
    ///
    /// @param pending the new pending state
    void setRedrawPending(boolean pending) {
        redrawPending = pending;
    }

    /// Delivers one event to the application after clearing redraw coalescing when applicable.
    ///
    /// @param event the event to deliver
    void deliver(WindowEvent event) {
        if (event.type() == org.glavo.himari.platform.api.WindowEventType.REDRAW_REQUESTED) {
            redrawPending = false;
        }
        @Nullable WindowEventHandler handler = eventHandler;
        if (handler == null) {
            return;
        }
        try {
            handler.handleWindowEvent(event);
        } finally {
            if (event.type() == org.glavo.himari.platform.api.WindowEventType.CLOSED) {
                eventHandler = null;
            }
        }
    }

    /// Completes the shared close stage after publishing a closed snapshot.
    void completeClose() {
        closeCompletion.complete(null);
    }

    /// Releases the strong reference to the owning session after this window is removed from it.
    void releasePlatform() {
        platform = null;
    }

    /// Returns the owning session while the window is open.
    ///
    /// @return the owning session
    /// @throws IllegalStateException if the close transition already released the session
    private HeadlessPlatform owningPlatform() {
        @Nullable HeadlessPlatform current = platform;
        if (current == null) {
            throw new IllegalStateException("Headless window is closed");
        }
        return current;
    }
}

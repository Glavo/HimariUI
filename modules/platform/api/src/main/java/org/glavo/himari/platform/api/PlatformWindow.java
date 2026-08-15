package org.glavo.himari.platform.api;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.concurrent.CompletionStage;

/// Represents one asynchronously configurable platform window and its presentation surface.
///
/// Operations may be requested from any thread and complete on the owning platform event loop after
/// their state transition is committed. Completion-stage dependent actions and event callbacks run
/// on that event loop and must not block it; any platform mutations they request remain queued.
/// Target handles are deliberately absent from this contract.
@NotNullByDefault
public interface PlatformWindow {
    /// Returns the stable session-local window identifier.
    ///
    /// @return the window identifier
    WindowId id();

    /// Returns the stable target-neutral surface descriptor.
    ///
    /// @return the surface descriptor
    SurfaceDescriptor surface();

    /// Returns the latest immutable window snapshot.
    ///
    /// This method may be called from any thread and does not include operations still waiting in the
    /// host event queue.
    ///
    /// @return the latest committed snapshot
    WindowSnapshot snapshot();

    /// Requests replacement of application-controlled window properties.
    ///
    /// A semantically unchanged configuration completes with the existing snapshot and does not
    /// advance its generation or publish an event.
    ///
    /// @param configuration the complete replacement configuration
    /// @return a stage completed with the committed snapshot
    /// @throws IllegalArgumentException if a popup is configured with a non-normal state
    /// @throws IllegalStateException if the platform event loop no longer accepts work
    CompletionStage<WindowSnapshot> configure(WindowConfiguration configuration);

    /// Requests a frame callback from the host.
    ///
    /// Repeated requests may be coalesced until the corresponding
    /// [WindowEventType#REDRAW_REQUESTED] callback begins.
    ///
    /// @throws IllegalStateException if the platform event loop no longer accepts work
    void requestRedraw();

    /// Explicitly closes the window and its surface.
    ///
    /// Closing is idempotent. Closing an owner also closes its popup descendants before the owner.
    ///
    /// @return a stage completed after the close transition is committed
    /// @throws IllegalStateException if the platform event loop no longer accepts work
    CompletionStage<Void> closeAsync();

    /// Returns whether the close transition has been committed.
    ///
    /// @return whether the latest snapshot is closed
    boolean isClosed();
}

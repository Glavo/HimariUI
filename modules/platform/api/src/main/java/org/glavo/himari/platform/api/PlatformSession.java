package org.glavo.himari.platform.api;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.concurrent.CompletionStage;

/// Owns one initialized platform application's displays, windows, clock, and event-loop context.
///
/// @param <W> the platform's concrete window type
@NotNullByDefault
public interface PlatformSession<W extends PlatformWindow> {
    /// Returns the monotonic frame clock.
    ///
    /// @return the session clock
    FrameClock clock();

    /// Returns the host-driven event loop.
    ///
    /// @return the session event loop
    PlatformEventLoop eventLoop();

    /// Returns one atomic snapshot of the display enumeration and topology generation.
    ///
    /// This method may be called from any thread.
    ///
    /// @return the current display topology
    DisplayTopologySnapshot displayTopology();

    /// Returns an immutable snapshot of the current display enumeration.
    ///
    /// Call [#displayTopology()] when the enumeration and its generation must come from the same
    /// atomic publication.
    ///
    /// @return displays in deterministic platform enumeration order
    default @Unmodifiable List<DisplaySnapshot> displays() {
        return displayTopology().displays();
    }

    /// Returns the current display-topology generation.
    ///
    /// @return the nonnegative topology generation
    default long displayTopologyGeneration() {
        return displayTopology().generation();
    }

    /// Registers a display event listener without replaying existing displays.
    ///
    /// The registration may be cancelled from any thread. An event already executing is not
    /// interrupted.
    ///
    /// @param handler the listener callback
    /// @return the listener registration
    ListenerRegistration addDisplayEventHandler(DisplayEventHandler handler);

    /// Requests asynchronous creation of a window.
    ///
    /// The returned stage completes after the creation transition is committed and before its
    /// [WindowEventType#CREATED] callback executes. Completion occurs on the platform event-loop
    /// owner context; dependent actions must not block it, and requested platform mutations remain
    /// asynchronously queued.
    ///
    /// @param request the complete creation request
    /// @param eventHandler the window event callback
    /// @return a stage completed with the new window
    /// @throws IllegalStateException if the session no longer accepts windows or the event loop is
    /// closed
    CompletionStage<W> createWindow(WindowRequest request, WindowEventHandler eventHandler);

    /// Closes all windows child-first, then closes the session event-loop implementation if owned.
    ///
    /// @return a stage completed after queued close callbacks and final shutdown work finish
    /// @throws IllegalStateException if shutdown cannot be submitted to the event loop
    CompletionStage<Void> closeAsync();

    /// Returns whether session shutdown completed.
    ///
    /// @return whether the session is closed
    boolean isClosed();
}

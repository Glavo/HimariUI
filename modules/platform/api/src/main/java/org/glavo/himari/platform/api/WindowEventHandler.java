package org.glavo.himari.platform.api;

import org.jetbrains.annotations.NotNullByDefault;

/// Receives window events on a platform session's event-loop owner thread.
@FunctionalInterface
@NotNullByDefault
public interface WindowEventHandler {
    /// Handles one immutable window event.
    ///
    /// @param event the event to handle
    void handleWindowEvent(WindowEvent event);
}

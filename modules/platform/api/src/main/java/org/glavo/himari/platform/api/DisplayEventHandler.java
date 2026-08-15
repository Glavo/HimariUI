package org.glavo.himari.platform.api;

import org.jetbrains.annotations.NotNullByDefault;

/// Receives display events on a platform session's event-loop owner thread.
@FunctionalInterface
@NotNullByDefault
public interface DisplayEventHandler {
    /// Handles one immutable display transition.
    ///
    /// @param event the event to handle
    void handleDisplayEvent(DisplayEvent event);
}

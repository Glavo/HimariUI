package org.glavo.himari.platform.api;

import org.jetbrains.annotations.NotNullByDefault;

/// Identifies an ordered platform-window event.
@NotNullByDefault
public enum WindowEventType {
    /// The asynchronous creation completed and the window is open.
    CREATED,

    /// Requested properties or display-derived effective properties changed.
    CONFIGURATION_CHANGED,

    /// The host accepted a coalesced request for another frame.
    REDRAW_REQUESTED,

    /// The host asked the application to decide whether to close the window.
    CLOSE_REQUESTED,

    /// The window and its presentation surface closed.
    CLOSED
}

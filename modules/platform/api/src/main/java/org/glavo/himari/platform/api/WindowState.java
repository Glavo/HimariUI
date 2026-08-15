package org.glavo.himari.platform.api;

import org.jetbrains.annotations.NotNullByDefault;

/// Identifies a target-neutral top-level window presentation state.
@NotNullByDefault
public enum WindowState {
    /// The window uses its configured normal frame.
    NORMAL,

    /// The host suppresses ordinary presentation while retaining the window.
    MINIMIZED,

    /// The host presents the window in its maximized state.
    MAXIMIZED,

    /// The host presents the window in its full-screen state.
    FULLSCREEN
}

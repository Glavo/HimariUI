package org.glavo.himari.platform.api;

import org.jetbrains.annotations.NotNullByDefault;

/// Identifies whether a platform window still accepts operations.
@NotNullByDefault
public enum WindowLifecycle {
    /// The window is active and accepts supported operations.
    OPEN,

    /// The window and its surface were explicitly closed.
    CLOSED
}

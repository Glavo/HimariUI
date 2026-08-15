package org.glavo.himari.platform.api;

import org.jetbrains.annotations.NotNullByDefault;

/// Identifies a display-topology change within a platform session.
@NotNullByDefault
public enum DisplayEventType {
    /// A new display became available.
    ADDED,

    /// An existing display changed geometry, scale, primary status, or color capabilities.
    CHANGED,

    /// A display stopped being available.
    REMOVED
}

package org.glavo.himari.layout.input;

import org.jetbrains.annotations.NotNullByDefault;

/// Identifies one normalized logical key.
@NotNullByDefault
public enum LogicalKey {
    /// Tab traversal.
    TAB,

    /// Enter activation.
    ENTER,

    /// Space activation.
    SPACE,

    /// Decrease a focused range or rewind a collection.
    ARROW_LEFT,

    /// Increase a focused range or advance a collection.
    ARROW_RIGHT,

    /// Decrease a focused vertical range.
    ARROW_UP,

    /// Increase a focused vertical range.
    ARROW_DOWN
}

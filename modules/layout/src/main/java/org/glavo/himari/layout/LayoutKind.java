package org.glavo.himari.layout;

import org.jetbrains.annotations.NotNullByDefault;

/// Identifies the measure and placement policy of one node.
@NotNullByDefault
public enum LayoutKind {
    /// Stacks children in the same cell.
    BOX,

    /// Places children on a horizontal axis.
    ROW,

    /// Places children on a vertical axis.
    COLUMN,

    /// A leaf that reports an intrinsic size.
    LEAF,

    /// Places children in a viewport and offsets them by a scroll position.
    SCROLL
}

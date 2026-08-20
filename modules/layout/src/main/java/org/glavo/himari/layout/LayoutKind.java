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
    SCROLL,

    /// Places children on a horizontal axis and distributes leftover width by grow weights.
    FLEX,

    /// Places children in wrapping rows that fill the available width.
    FLOW,

    /// Places children on a fixed-column grid.
    GRID,

    /// Delegates measure and place to a [CustomLayout].
    CUSTOM,

    /// Stacks children at explicit overlay offsets; the size is the union of those boxes.
    OVERLAY,

    /// Places trailing children at overlay offsets without growing the first child's slot.
    PORTAL
}

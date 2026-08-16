package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;

/// Identifies one command recorded from an [`OutlinePen`].
@NotNullByDefault
public enum OutlineVerb {
    /// Starts a contour. Destination is stored in [`OutlineOp#x0()`] and [`OutlineOp#y0()`].
    MOVE,

    /// Draws a line. Destination is stored in [`OutlineOp#x0()`] and [`OutlineOp#y0()`].
    LINE,

    /// Draws a quadratic. Control is [`OutlineOp#x0()`]/[`OutlineOp#y0()`]; destination is
    /// [`OutlineOp#x1()`]/[`OutlineOp#y1()`].
    QUAD,

    /// Closes the current contour. Coordinate fields are unused.
    CLOSE
}

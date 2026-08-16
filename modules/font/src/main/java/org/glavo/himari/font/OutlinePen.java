package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;

/// Receives TrueType outline commands in font units.
///
/// Coordinates use the font's em space with y increasing upward. A contour starts with
/// [`#moveTo(float, float)`] and ends with [`#close()`]. [`#close()`] returns to the most recent
/// move without a further line command when the current point is already there.
@NotNullByDefault
public interface OutlinePen {
    /// Starts a new contour at `(x, y)`.
    ///
    /// @param x the x coordinate in font units
    /// @param y the y coordinate in font units
    void moveTo(float x, float y);

    /// Draws a straight segment to `(x, y)`.
    ///
    /// @param x the destination x coordinate in font units
    /// @param y the destination y coordinate in font units
    void lineTo(float x, float y);

    /// Draws a quadratic Bézier to `(x, y)` with control point `(cx, cy)`.
    ///
    /// @param cx the control x coordinate in font units
    /// @param cy the control y coordinate in font units
    /// @param x the destination x coordinate in font units
    /// @param y the destination y coordinate in font units
    void quadTo(float cx, float cy, float x, float y);

    /// Draws a cubic Bézier to `(x, y)` with control points `(c1x, c1y)` and `(c2x, c2y)`.
    ///
    /// @param c1x the first control x coordinate in font units
    /// @param c1y the first control y coordinate in font units
    /// @param c2x the second control x coordinate in font units
    /// @param c2y the second control y coordinate in font units
    /// @param x the destination x coordinate in font units
    /// @param y the destination y coordinate in font units
    void cubicTo(float c1x, float c1y, float c2x, float c2y, float x, float y);

    /// Closes the current contour back to its starting point.
    void close();
}

package org.glavo.himari.layout;

import org.jetbrains.annotations.NotNullByDefault;

/// Stores an axis-aligned rectangle in logical pixels.
///
/// @param x the horizontal origin
/// @param y the vertical origin
/// @param width the nonnegative width
/// @param height the nonnegative height
@NotNullByDefault
public record LayoutRect(float x, float y, float width, float height) {
    /// Validates the rectangle.
    public LayoutRect {
        if (!Float.isFinite(x) || !Float.isFinite(y)
                || !Float.isFinite(width) || !Float.isFinite(height)
                || width < 0.0f || height < 0.0f) {
            throw new IllegalArgumentException("Layout rectangle values must be finite and extents nonnegative");
        }
    }

    /// Returns whether the rectangle contains the point.
    ///
    /// @param pointX the horizontal coordinate
    /// @param pointY the vertical coordinate
    /// @return whether the point lies inside inclusive-minimum exclusive-maximum bounds
    public boolean contains(float pointX, float pointY) {
        return pointX >= x && pointY >= y && pointX < x + width && pointY < y + height;
    }
}

package org.glavo.himari.layout;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

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

    /// Returns the overlapping rectangle of this and `other`.
    ///
    /// An empty overlap is a zero-size rectangle at the clamped origin.
    ///
    /// @param other the other rectangle
    /// @return the intersection
    public LayoutRect intersect(LayoutRect other) {
        Objects.requireNonNull(other, "other");
        float left = Math.max(x, other.x);
        float top = Math.max(y, other.y);
        float right = Math.min(x + width, other.x + other.width);
        float bottom = Math.min(y + height, other.y + other.height);
        return new LayoutRect(left, top, Math.max(0.0f, right - left), Math.max(0.0f, bottom - top));
    }
}

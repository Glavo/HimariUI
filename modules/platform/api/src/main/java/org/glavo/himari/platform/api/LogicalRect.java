package org.glavo.himari.platform.api;

import org.jetbrains.annotations.NotNullByDefault;

/// Describes an axis-aligned rectangle in logical pixels.
///
/// Coordinates may be negative. Extents and their corresponding maximum coordinates are finite so
/// platform implementations can compare and transform the rectangle without overflow to infinity.
///
/// @param x the horizontal origin in logical pixels
/// @param y the vertical origin in logical pixels
/// @param width the nonnegative width in logical pixels
/// @param height the nonnegative height in logical pixels
@NotNullByDefault
public record LogicalRect(double x, double y, double width, double height) {
    /// Creates a validated logical rectangle.
    ///
    /// @throws IllegalArgumentException if a value is non-finite, an extent is negative, or a
    /// maximum coordinate is non-finite
    public LogicalRect {
        if (!Double.isFinite(x) || !Double.isFinite(y)
                || !Double.isFinite(width) || !Double.isFinite(height)) {
            throw new IllegalArgumentException("Logical rectangle values must be finite");
        }
        if (width < 0.0 || height < 0.0) {
            throw new IllegalArgumentException("Logical rectangle extents must be nonnegative");
        }
        if (!Double.isFinite(x + width) || !Double.isFinite(y + height)) {
            throw new IllegalArgumentException("Logical rectangle maximum coordinates must be finite");
        }
    }

    /// Returns the exclusive horizontal maximum coordinate.
    ///
    /// @return `x + width`
    public double maxX() {
        return x + width;
    }

    /// Returns the exclusive vertical maximum coordinate.
    ///
    /// @return `y + height`
    public double maxY() {
        return y + height;
    }

    /// Returns whether this rectangle completely contains another rectangle.
    ///
    /// Edges are inclusive for this containment check, including zero-area rectangles.
    ///
    /// @param other the candidate rectangle
    /// @return whether `other` lies within this rectangle
    public boolean contains(LogicalRect other) {
        return other.x >= x && other.y >= y && other.maxX() <= maxX() && other.maxY() <= maxY();
    }

    /// Returns the intersection area with another rectangle.
    ///
    /// @param other the other rectangle
    /// @return the nonnegative intersection area, or positive infinity if the finite extents produce
    /// an area beyond `double` range
    public double intersectionArea(LogicalRect other) {
        double intersectionWidth = Math.max(0.0, Math.min(maxX(), other.maxX()) - Math.max(x, other.x));
        double intersectionHeight = Math.max(0.0, Math.min(maxY(), other.maxY()) - Math.max(y, other.y));
        return intersectionWidth * intersectionHeight;
    }
}

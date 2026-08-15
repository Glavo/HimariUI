package org.glavo.himari.layout;

import org.jetbrains.annotations.NotNullByDefault;

/// Constrains one measure pass in logical pixels.
///
/// @param minWidth the nonnegative minimum width
/// @param maxWidth the maximum width, at least `minWidth`
/// @param minHeight the nonnegative minimum height
/// @param maxHeight the maximum height, at least `minHeight`
@NotNullByDefault
public record Constraints(float minWidth, float maxWidth, float minHeight, float maxHeight) {
    /// Creates validated constraints.
    public Constraints {
        requireFiniteNonNegative(minWidth, "minWidth");
        requireFiniteNonNegative(maxWidth, "maxWidth");
        requireFiniteNonNegative(minHeight, "minHeight");
        requireFiniteNonNegative(maxHeight, "maxHeight");
        if (maxWidth < minWidth || maxHeight < minHeight) {
            throw new IllegalArgumentException("Maximum constraints must be at least the minima");
        }
    }

    /// Tight constraints that force an exact size.
    ///
    /// @param width the exact width
    /// @param height the exact height
    /// @return the tight constraints
    public static Constraints tight(float width, float height) {
        return new Constraints(width, width, height, height);
    }

    /// Loose constraints with zero minima.
    ///
    /// @param maxWidth the maximum width
    /// @param maxHeight the maximum height
    /// @return the loose constraints
    public static Constraints loose(float maxWidth, float maxHeight) {
        return new Constraints(0.0f, maxWidth, 0.0f, maxHeight);
    }

    /// Returns whether both axes are tight.
    ///
    /// @return whether min equals max on both axes
    public boolean isTight() {
        return minWidth == maxWidth && minHeight == maxHeight;
    }

    /// Constrains a candidate size into this range.
    ///
    /// @param width the candidate width
    /// @param height the candidate height
    /// @return the constrained size
    public Size constrain(float width, float height) {
        return new Size(
                Math.clamp(width, minWidth, maxWidth),
                Math.clamp(height, minHeight, maxHeight)
        );
    }

    /// Deflates these constraints by padding.
    ///
    /// @param horizontal the total horizontal padding
    /// @param vertical the total vertical padding
    /// @return the inner constraints
    public Constraints deflate(float horizontal, float vertical) {
        requireFiniteNonNegative(horizontal, "horizontal");
        requireFiniteNonNegative(vertical, "vertical");
        float nextMaxWidth = Math.max(0.0f, maxWidth - horizontal);
        float nextMaxHeight = Math.max(0.0f, maxHeight - vertical);
        return new Constraints(
                Math.min(minWidth, nextMaxWidth),
                nextMaxWidth,
                Math.min(minHeight, nextMaxHeight),
                nextMaxHeight
        );
    }

    /// Requires a finite nonnegative quantity.
    ///
    /// @param value the candidate
    /// @param name the parameter name
    private static void requireFiniteNonNegative(float value, String name) {
        if (!Float.isFinite(value) || value < 0.0f) {
            throw new IllegalArgumentException(name + " must be finite and nonnegative");
        }
    }
}

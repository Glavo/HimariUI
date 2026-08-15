package org.glavo.himari.layout;

import org.jetbrains.annotations.NotNullByDefault;

/// Stores a measured size in logical pixels.
///
/// @param width the nonnegative width
/// @param height the nonnegative height
@NotNullByDefault
public record Size(float width, float height) {
    /// A zero size.
    public static final Size ZERO = new Size(0.0f, 0.0f);

    /// Validates the size.
    public Size {
        if (!Float.isFinite(width) || !Float.isFinite(height) || width < 0.0f || height < 0.0f) {
            throw new IllegalArgumentException("Size extents must be finite and nonnegative");
        }
    }
}

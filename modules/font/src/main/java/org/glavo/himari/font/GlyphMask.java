package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Objects;

/// Stores one grayscale glyph coverage mask.
///
/// @param width the nonnegative width
/// @param height the nonnegative height
/// @param coverage row-major samples in `[0, 255]`
@NotNullByDefault
public record GlyphMask(int width, int height, byte @Unmodifiable [] coverage) {
    /// Validates the mask.
    public GlyphMask {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Glyph mask extents must be nonnegative");
        }
        Objects.requireNonNull(coverage, "coverage");
        if (coverage.length != Math.multiplyExact(width, height)) {
            throw new IllegalArgumentException("Glyph mask coverage length does not match extents");
        }
        coverage = coverage.clone();
    }
}

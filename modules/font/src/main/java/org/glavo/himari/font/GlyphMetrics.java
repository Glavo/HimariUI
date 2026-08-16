package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;

/// Stores one glyph's horizontal and vertical metrics in font units.
///
/// @param glyphId the nonnegative glyph identity
/// @param advanceWidth the nonnegative horizontal advance
/// @param leftSideBearing the left side bearing
/// @param advanceHeight the nonnegative vertical advance
@NotNullByDefault
public record GlyphMetrics(int glyphId, int advanceWidth, int leftSideBearing, int advanceHeight) {
    /// Validates the metrics.
    public GlyphMetrics {
        if (glyphId < 0 || advanceWidth < 0 || advanceHeight < 0) {
            throw new IllegalArgumentException("Glyph identity and advances must be nonnegative");
        }
    }

    /// Creates metrics with a zero vertical advance.
    ///
    /// @param glyphId the glyph identity
    /// @param advanceWidth the horizontal advance
    /// @param leftSideBearing the left side bearing
    public GlyphMetrics(int glyphId, int advanceWidth, int leftSideBearing) {
        this(glyphId, advanceWidth, leftSideBearing, 0);
    }
}

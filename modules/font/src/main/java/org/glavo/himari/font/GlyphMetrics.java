package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;

/// Stores one glyph's horizontal metrics in font units.
///
/// @param glyphId the nonnegative glyph identity
/// @param advanceWidth the nonnegative advance
/// @param leftSideBearing the left side bearing
@NotNullByDefault
public record GlyphMetrics(int glyphId, int advanceWidth, int leftSideBearing) {
    /// Validates the metrics.
    public GlyphMetrics {
        if (glyphId < 0 || advanceWidth < 0) {
            throw new IllegalArgumentException("Glyph identity and advance must be nonnegative");
        }
    }
}

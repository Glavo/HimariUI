package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;

/// Records one GSUB type-4 ligature match.
///
/// @param glyphId the substitute ligature glyph
/// @param consumed the number of input glyphs consumed, at least `2`
@NotNullByDefault
public record GlyphLigature(int glyphId, int consumed) {
    /// Validates the match.
    public GlyphLigature {
        if (glyphId < 0 || consumed < 2) {
            throw new IllegalArgumentException("Ligature glyph must be nonnegative and must consume at least two glyphs");
        }
    }
}

package org.glavo.himari.text;

import org.jetbrains.annotations.NotNullByDefault;

/// Stores one default-shaped glyph.
///
/// @param codePoint the mapped code point after Arabic or Hebrew presentation substitution
/// @param glyphId the mapped glyph identity
/// @param cluster the source cluster index
/// @param xAdvance the advance in font units
@NotNullByDefault
public record ShapedGlyph(int codePoint, int glyphId, int cluster, int xAdvance) {
    /// Validates the glyph.
    public ShapedGlyph {
        if (glyphId < 0 || cluster < 0 || xAdvance < 0) {
            throw new IllegalArgumentException("Shaped glyph fields must be nonnegative");
        }
    }
}

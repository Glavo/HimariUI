package org.glavo.himari.text;

import org.jetbrains.annotations.NotNullByDefault;

/// Stores one default-shaped glyph.
///
/// @param codePoint the mapped code point after Arabic or Hebrew presentation substitution
/// @param glyphId the mapped glyph identity
/// @param cluster the source cluster index
/// @param xAdvance the advance in layout units
/// @param xOffset the signed GPOS X offset in layout units
/// @param yOffset the signed GPOS Y offset in layout units
/// @param fontIndex the [`FontCollection`] index that supplied this glyph
/// @param unsafeToBreak whether a line break before this glyph would split a ligature or composed cluster
@NotNullByDefault
public record ShapedGlyph(
        int codePoint,
        int glyphId,
        int cluster,
        int xAdvance,
        int xOffset,
        int yOffset,
        int fontIndex,
        boolean unsafeToBreak
) {
    /// Validates the glyph.
    public ShapedGlyph {
        if (glyphId < 0 || cluster < 0 || xAdvance < 0 || fontIndex < 0) {
            throw new IllegalArgumentException(
                    "Shaped glyph identity, cluster, advance, and font index must be nonnegative"
            );
        }
    }

    /// Creates a glyph with zero GPOS offsets on the primary font.
    ///
    /// @param codePoint the mapped code point
    /// @param glyphId the glyph identity
    /// @param cluster the cluster
    /// @param xAdvance the advance
    public ShapedGlyph(int codePoint, int glyphId, int cluster, int xAdvance) {
        this(codePoint, glyphId, cluster, xAdvance, 0, 0, 0, false);
    }

    /// Creates a glyph on the primary font.
    ///
    /// @param codePoint the mapped code point
    /// @param glyphId the glyph identity
    /// @param cluster the cluster
    /// @param xAdvance the advance
    /// @param xOffset the signed X offset
    /// @param yOffset the signed Y offset
    public ShapedGlyph(int codePoint, int glyphId, int cluster, int xAdvance, int xOffset, int yOffset) {
        this(codePoint, glyphId, cluster, xAdvance, xOffset, yOffset, 0, false);
    }

    /// Creates a breakable glyph on a specific fallback font.
    ///
    /// @param codePoint the mapped code point
    /// @param glyphId the glyph identity
    /// @param cluster the cluster
    /// @param xAdvance the advance
    /// @param xOffset the signed X offset
    /// @param yOffset the signed Y offset
    /// @param fontIndex the font index
    public ShapedGlyph(
            int codePoint,
            int glyphId,
            int cluster,
            int xAdvance,
            int xOffset,
            int yOffset,
            int fontIndex
    ) {
        this(codePoint, glyphId, cluster, xAdvance, xOffset, yOffset, fontIndex, false);
    }
}

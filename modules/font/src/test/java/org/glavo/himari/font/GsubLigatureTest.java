package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/// Verifies GSUB type-4 ligatures on the constructed font.
@NotNullByDefault
final class GsubLigatureTest {
    /// Substitutes LAM plus alef through [`SfntFont#ligature(int[], int, int, int)`].
    @Test
    void ligatesLamAlefThroughRlig() {
        SfntFont font = GsubLigatureSampleFont.create();
        int[] glyphs = {GsubLigatureSampleFont.GLYPH_LAM, GsubLigatureSampleFont.GLYPH_ALEF};
        @Nullable GlyphLigature match = font.ligature(glyphs, 0, 2, SfntFont.TAG_RLIG);
        assertNotNull(match);
        assertEquals(GsubLigatureSampleFont.GLYPH_LIGATURE, match.glyphId());
        assertEquals(2, match.consumed());
        assertNull(font.ligature(glyphs, 0, 2, SfntFont.TAG_LIGA));
        assertEquals(GsubLigatureSampleFont.GLYPH_LAM, font.glyphId(0x0644));
        assertEquals(GsubLigatureSampleFont.GLYPH_ALEF, font.glyphId(0x0627));
    }
}

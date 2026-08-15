package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies the bundled SFNT sample font and grayscale rasterizer.
@NotNullByDefault
final class SfntFontTest {
    /// Maps Latin letters and rasters a non-empty coverage mask.
    @Test
    void mapsAndRastersLatinGlyphs() {
        SfntFont font = BitmapSfntFont.create();
        int glyph = font.glyphId('C');
        assertTrue(glyph > 0);
        assertEquals(6, font.metrics(glyph).advanceWidth());
        GlyphMask mask = GlyphRasterizer.rasterize(font, glyph, 16);
        assertTrue(mask.width() > 0);
        assertTrue(mask.height() > 0);
        int covered = 0;
        for (byte sample : mask.coverage()) {
            if (sample != 0) {
                covered++;
            }
        }
        assertTrue(covered > 0);
    }

    /// Maps distinct Arabic presentation forms through the script sample font.
    @Test
    void mapsArabicPresentationForms() {
        SfntFont font = ScriptSampleFont.create();
        int nominal = font.glyphId('\u0628');
        int isolated = font.glyphId('\uFE8F');
        int initial = font.glyphId('\uFE91');
        int medial = font.glyphId('\uFE92');
        int finalForm = font.glyphId('\uFE90');
        assertTrue(nominal > 0);
        assertTrue(isolated > 0);
        assertTrue(initial > 0);
        assertTrue(medial > 0);
        assertTrue(finalForm > 0);
        assertTrue(isolated != nominal);
        assertTrue(initial != medial);
        assertTrue(medial != finalForm);
        assertEquals(0, font.metrics(font.glyphId('\u064E')).advanceWidth());
        assertTrue(font.glyphId('\uFB31') > 0);
        assertTrue(font.glyphId('\uFB31') != font.glyphId('\u05D1'));
    }
}

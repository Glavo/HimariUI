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
}

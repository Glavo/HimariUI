package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies `gasp` flags and unhinted grayscale snapping through [`SfntFont`] and [`GlyphRasterizer`].
@NotNullByDefault
final class GaspTableTest {
    /// Reports grayscale permission from the shipped `gasp` ranges.
    @Test
    void flagsWithholdGrayscaleAtSmallPpem() {
        SfntFont font = GaspSampleFont.create();
        assertFalse(font.gaspAllowsGrayscale(GaspSampleFont.BINARY_PPEM));
        assertEquals(0, font.gaspFlags(GaspSampleFont.BINARY_PPEM));
        assertTrue(font.gaspAllowsGrayscale(GaspSampleFont.GRAY_PPEM));
        assertEquals(GaspTable.DOGRAY, font.gaspFlags(GaspSampleFont.GRAY_PPEM));
        assertTrue(BitmapSfntFont.create().gaspAllowsGrayscale(8));
    }

    /// Snaps coverage to 0 or 255 when `gasp` withholds grayscale.
    @Test
    void rasterizerHonorsGaspGrayscaleFlag() {
        SfntFont font = GaspSampleFont.create();
        GlyphMask binary = GlyphRasterizer.rasterize(font, GaspSampleFont.GLYPH_A, GaspSampleFont.BINARY_PPEM);
        GlyphMask gray = GlyphRasterizer.rasterize(font, GaspSampleFont.GLYPH_A, GaspSampleFont.GRAY_PPEM);
        assertFalse(hasPartial(binary));
        assertTrue(hasPartial(gray));
    }

    /// Snaps the outline box when `gasp` requests grid fitting.
    @Test
    void rasterizerHonorsGaspGridFit() {
        SfntFont fitted = GaspGridFitSampleFont.create();
        assertTrue(fitted.gaspGridFits(GaspGridFitSampleFont.GRID_PPEM));
        assertFalse(GaspSampleFont.create().gaspGridFits(GaspSampleFont.GRAY_PPEM));
        GlyphMask mask = GlyphRasterizer.rasterize(
                fitted,
                GaspGridFitSampleFont.GLYPH_A,
                GaspGridFitSampleFont.GRID_PPEM
        );
        assertEquals(10, mask.height());
    }

    /// Snaps the outline x-box when `gasp` requests symmetric grid fitting.
    @Test
    void rasterizerHonorsGaspSymmetricGridFit() {
        SfntFont fitted = GaspSymmetricGridFitSampleFont.create();
        assertTrue(fitted.gaspSymmetricGridFits(GaspSymmetricGridFitSampleFont.GRID_PPEM));
        assertFalse(GaspGridFitSampleFont.create().gaspSymmetricGridFits(GaspGridFitSampleFont.GRID_PPEM));
        GlyphMask mask = GlyphRasterizer.rasterize(
                fitted,
                GaspSymmetricGridFitSampleFont.GLYPH_A,
                GaspSymmetricGridFitSampleFont.GRID_PPEM
        );
        assertEquals(10, mask.width());
    }

    /// Returns whether `mask` contains an intermediate coverage sample.
    private static boolean hasPartial(GlyphMask mask) {
        for (byte sample : mask.coverage()) {
            int value = sample & 0xFF;
            if (value > 0 && value < 255) {
                return true;
            }
        }
        return false;
    }
}

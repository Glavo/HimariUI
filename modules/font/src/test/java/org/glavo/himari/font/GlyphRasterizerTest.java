package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies unhinted grayscale coverage comes from curves, not raw-point polylines.
@NotNullByDefault
final class GlyphRasterizerTest {
    /// `(50,70)` is inside the control triangle but outside the quadratic fill.
    @Test
    void quadraticCoverageExcludesControlTrianglePeak() {
        SfntFont font = OutlineSampleFont.create();
        GlyphMask mask = GlyphRasterizer.rasterize(font, OutlineSampleFont.GLYPH_BUMP, 100);
        assertEquals(100, mask.width());
        assertEquals(100, mask.height());
        int outside = sample(mask, 50, 70) & 0xFF;
        int inside = sample(mask, 50, 25) & 0xFF;
        assertEquals(0, outside);
        assertTrue(inside > 200, "expected interior coverage, got " + inside);
        boolean partial = false;
        for (byte value : mask.coverage()) {
            int sample = value & 0xFF;
            if (sample > 0 && sample < 255) {
                partial = true;
                break;
            }
        }
        assertTrue(partial);
    }

    /// Composite coverage is the translated bump, not an empty composite skip.
    @Test
    void compositeCoverageFollowsTranslation() {
        SfntFont font = OutlineSampleFont.create();
        GlyphMask mask = GlyphRasterizer.rasterize(font, OutlineSampleFont.GLYPH_COMPOSITE, 100);
        assertTrue(mask.width() > 0);
        assertTrue(mask.height() > 0);
        int inside = sample(mask, 50, 25) & 0xFF;
        int outside = sample(mask, 50, 70) & 0xFF;
        assertTrue(inside > 200, "expected translated interior, got " + inside);
        assertEquals(0, outside);
    }

    /// Latin rectangles still produce non-empty coverage after the curve raster.
    @Test
    void rectangleGlyphStillCoversInterior() {
        SfntFont font = BitmapSfntFont.create();
        GlyphMask mask = GlyphRasterizer.rasterize(font, font.glyphId('C'), 16);
        int covered = 0;
        for (byte sample : mask.coverage()) {
            if (sample != 0) {
                covered++;
            }
        }
        assertTrue(covered > 0);
        assertNotEquals(0, mask.width());
    }

    /// Samples the pixel whose origin is the given font-unit coordinate at 1:1 scale.
    private static byte sample(GlyphMask mask, int fontX, int fontY) {
        assertTrue(fontX >= 0 && fontX < mask.width());
        assertTrue(fontY >= 0 && fontY < mask.height());
        return mask.coverage()[fontY * mask.width() + fontX];
    }
}

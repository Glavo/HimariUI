package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies CPU glyph packing through the shipped rasterizer.
@NotNullByDefault
final class GlyphAtlasTest {
    /// Interns a Latin rectangle and returns the same packed slot on a hit.
    @Test
    void internsAndHitsLatinGlyph() {
        SfntFont font = BitmapSfntFont.create();
        int glyph = font.glyphId('C');
        GlyphAtlas atlas = new GlyphAtlas(64, 64);
        @Nullable AtlasGlyph first = atlas.intern(font, glyph, 16);
        assertNotNull(first);
        assertTrue(first.width() > 0);
        assertTrue(first.height() > 0);
        @Nullable AtlasGlyph hit = atlas.intern(font, glyph, 16);
        assertSame(first, hit);
        assertEquals(first, atlas.locate(font, glyph, 16));
        GlyphMask sheet = atlas.snapshot();
        int covered = 0;
        for (int row = first.y(); row < first.y() + first.height(); row++) {
            for (int column = first.x(); column < first.x() + first.width(); column++) {
                if ((sheet.coverage()[row * sheet.width() + column] & 0xFF) != 0) {
                    covered++;
                }
            }
        }
        assertTrue(covered > 0);
        assertEquals(1, atlas.glyphCount());
    }

    /// Packs a second glyph on the same shelf without overlapping the first.
    @Test
    void packsSecondGlyphBesideFirst() {
        SfntFont font = BitmapSfntFont.create();
        GlyphAtlas atlas = new GlyphAtlas(64, 64);
        AtlasGlyph a = atlas.intern(font, font.glyphId('A'), 16);
        AtlasGlyph b = atlas.intern(font, font.glyphId('B'), 16);
        assertNotNull(a);
        assertNotNull(b);
        assertEquals(0, a.x());
        assertEquals(0, a.y());
        assertEquals(a.x() + a.width() + 1, b.x());
        assertEquals(0, b.y());
        assertEquals(2, atlas.glyphCount());
    }

    /// Leaves a mask uncached when it cannot fit the remaining sheet.
    @Test
    void rejectsGlyphThatDoesNotFit() {
        SfntFont font = OutlineSampleFont.create();
        GlyphAtlas atlas = new GlyphAtlas(32, 32);
        assertNull(atlas.intern(font, OutlineSampleFont.GLYPH_BUMP, 100));
        assertEquals(0, atlas.glyphCount());
        assertNull(atlas.locate(font, OutlineSampleFont.GLYPH_BUMP, 100));
    }

    /// Clears occupancy so a later intern can reuse the sheet.
    @Test
    void clearAllowsRepack() {
        SfntFont font = BitmapSfntFont.create();
        int glyph = font.glyphId('C');
        GlyphAtlas atlas = new GlyphAtlas(64, 64);
        assertNotNull(atlas.intern(font, glyph, 16));
        atlas.clear();
        assertEquals(0, atlas.glyphCount());
        assertNull(atlas.locate(font, glyph, 16));
        assertNotNull(atlas.intern(font, glyph, 16));
        assertEquals(1, atlas.glyphCount());
    }
}

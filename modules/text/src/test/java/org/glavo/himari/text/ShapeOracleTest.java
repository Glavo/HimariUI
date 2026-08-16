package org.glavo.himari.text;

import org.glavo.himari.font.GsubSampleFont;
import org.glavo.himari.font.SfntFont;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/// Drives the shipped shaper through the HarfBuzz-style compare records.
@NotNullByDefault
final class ShapeOracleTest {
    /// Round-trips GSUB Arabic shaping through the compare JSON.
    @Test
    void gsubArabicMatchesSerializedRecords() {
        SfntFont font = GsubSampleFont.create();
        List<ShapedGlyph> glyphs = DefaultShaper.shape(font, "\u0628\u0628\u0628");
        String json = ShapeCompare.toJson(glyphs);
        List<ShapedGlyph> parsed = ShapeCompare.parse(json);
        @Nullable String difference = ShapeCompare.difference(glyphs, parsed);
        assertNull(difference, difference);
        assertEquals(GsubSampleFont.GLYPH_INIT, parsed.get(0).glyphId());
        assertEquals(GsubSampleFont.GLYPH_MEDI, parsed.get(1).glyphId());
        assertEquals(GsubSampleFont.GLYPH_FINA, parsed.get(2).glyphId());
    }

    /// Accepts `gid`/`ax` aliases used by some `hb-shape` JSON dumps.
    @Test
    void parsesHarfBuzzAliases() {
        List<ShapedGlyph> parsed = ShapeCompare.parse("[{\"gid\":3,\"cluster\":0,\"ax\":11}]");
        assertEquals(1, parsed.size());
        assertEquals(3, parsed.getFirst().glyphId());
        assertEquals(11, parsed.getFirst().xAdvance());
    }
}

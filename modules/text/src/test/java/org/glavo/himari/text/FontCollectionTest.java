package org.glavo.himari.text;

import org.glavo.himari.font.BitmapSfntFont;
import org.glavo.himari.font.FontDirectories;
import org.glavo.himari.font.GposMarkSampleFont;
import org.glavo.himari.font.SfntFont;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies fallback segmentation and mixed-face shaping.
@NotNullByDefault
final class FontCollectionTest {
    /// Splits Latin and Arabic onto the first covering face.
    @Test
    void segmentsLatinThenArabic() {
        SfntFont latin = BitmapSfntFont.create();
        SfntFont arabic = GposMarkSampleFont.create();
        FontCollection fonts = new FontCollection(latin, arabic);
        assertSame(latin, fonts.covering('A'));
        assertSame(arabic, fonts.covering(0x0628));
        assertNull(fonts.covering(0x4E00));
        List<FontRun> runs = fonts.segment("AB\u0628");
        assertEquals(2, runs.size());
        assertEquals(0, runs.get(0).fontIndex());
        assertEquals(2, runs.get(0).endClusterExclusive());
        assertFalse(runs.get(0).missingGlyph());
        assertEquals(1, runs.get(1).fontIndex());
        assertEquals(2, runs.get(1).startCluster());
        assertEquals(3, runs.get(1).endClusterExclusive());
        assertSame(arabic, runs.get(1).font());
    }

    /// Keeps fatha on the Arabic face that owns the preceding Beh.
    @Test
    void keepsMarkWithArabicBase() {
        FontCollection fonts = new FontCollection(BitmapSfntFont.create(), GposMarkSampleFont.create());
        List<FontRun> runs = fonts.segment("A\u0628\u064E");
        assertEquals(2, runs.size());
        assertEquals(1, runs.get(1).startCluster());
        assertEquals(3, runs.get(1).endClusterExclusive());
        assertEquals(1, runs.get(1).fontIndex());
        assertFalse(runs.get(1).missingGlyph());
    }

    /// Leaves a space on the preceding Latin run when that face covers U+0020.
    @Test
    void keepsSpaceWithPreviousRun() {
        FontCollection fonts = new FontCollection(BitmapSfntFont.create(), GposMarkSampleFont.create());
        List<FontRun> runs = fonts.segment("A \u0628");
        assertEquals(2, runs.size());
        assertEquals(0, runs.get(0).startUtf16());
        assertEquals(2, runs.get(0).endUtf16());
        assertEquals(0, runs.get(0).fontIndex());
        assertEquals(1, runs.get(1).fontIndex());
        assertEquals(2, runs.get(1).startUtf16());
    }

    /// Maps an uncovered code point to `.notdef` on the primary face once.
    @Test
    void missingGlyphStaysOnPrimary() {
        SfntFont latin = BitmapSfntFont.create();
        FontCollection fonts = new FontCollection(latin, latin);
        List<FontRun> runs = fonts.segment("A\u4E00B");
        assertEquals(1, runs.size());
        assertEquals(0, runs.getFirst().fontIndex());
        assertTrue(runs.getFirst().missingGlyph());
        assertEquals(3, runs.getFirst().endClusterExclusive());
    }

    /// Shapes each fallback run through the face that covers it.
    @Test
    void shapesMixedLatinAndMarkArabic() {
        SfntFont latin = BitmapSfntFont.create();
        SfntFont arabic = GposMarkSampleFont.create();
        FontCollection fonts = new FontCollection(latin, arabic);
        List<ShapedGlyph> glyphs = FallbackShaper.shape(fonts, "A\u0628\u064E");
        assertEquals(3, glyphs.size());
        assertEquals('A', glyphs.get(0).codePoint());
        assertEquals(0, glyphs.get(0).fontIndex());
        assertEquals(latin.glyphId('A'), glyphs.get(0).glyphId());
        assertEquals(GposMarkSampleFont.GLYPH_BEH, glyphs.get(1).glyphId());
        assertEquals(1, glyphs.get(1).fontIndex());
        assertEquals(1, glyphs.get(1).cluster());
        assertEquals(GposMarkSampleFont.GLYPH_FATHA, glyphs.get(2).glyphId());
        assertEquals(0, glyphs.get(2).xAdvance());
        assertEquals(GposMarkSampleFont.MARK_X_OFFSET, glyphs.get(2).xOffset());
        assertEquals(GposMarkSampleFont.MARK_Y_OFFSET, glyphs.get(2).yOffset());
        assertEquals(1, glyphs.get(2).fontIndex());
    }

    /// Converts mixed-em advances to the primary face's units per em.
    @Test
    void scalesAdvanceToPrimaryEm() {
        assertEquals(20, FallbackShaper.scale(10, 8, 16));
        assertEquals(-3, FallbackShaper.scale(-3, 8, 8));
        assertEquals(0, FallbackShaper.scale(0, 8, 16));
    }

    /// Loads readable Windows catalog faces behind the sample primary.
    @Test
    @EnabledOnOs(OS.WINDOWS)
    void hostCatalogAppendsSystemFaces() {
        FontCollection fonts = FontCollection.withHostCatalog(BitmapSfntFont.create());
        assertTrue(fonts.size() >= 1);
        assertSame(fonts.primary(), fonts.covering('A'));
        @Nullable SfntFont han = fonts.covering(0x4E00);
        if (han == null) {
            return;
        }
        assertTrue(han != fonts.primary());
        List<ShapedGlyph> glyphs = FallbackShaper.shape(fonts, "A\u4E00");
        assertEquals(2, glyphs.size());
        assertEquals(fonts.primary().glyphId('A'), glyphs.get(0).glyphId());
        assertTrue(glyphs.get(1).glyphId() > 0);
        assertTrue(glyphs.get(1).fontIndex() > 0);
    }

    /// Falls back from the sample Latin face onto a discovered Windows font for U+4E00.
    @Test
    @EnabledOnOs(OS.WINDOWS)
    void fallsBackToWindowsFontForHan() {
        @Nullable Path directory = FontDirectories.windowsFonts();
        assertNotNull(directory);
        @Nullable SfntFont han = openWindowsHan(directory);
        if (han == null) {
            return;
        }
        SfntFont latin = BitmapSfntFont.create();
        FontCollection fonts = new FontCollection(latin, han);
        assertSame(han, fonts.covering(0x4E00));
        List<FontRun> runs = fonts.segment("A\u4E00");
        assertEquals(2, runs.size());
        assertEquals(0, runs.get(0).fontIndex());
        assertEquals(1, runs.get(1).fontIndex());
        List<ShapedGlyph> glyphs = FallbackShaper.shape(fonts, "A\u4E00");
        assertEquals(2, glyphs.size());
        assertEquals(latin.glyphId('A'), glyphs.get(0).glyphId());
        assertTrue(glyphs.get(1).glyphId() > 0);
        assertEquals(1, glyphs.get(1).fontIndex());
    }

    /// Opens the first Windows TTF that maps U+4E00, preferring known CJK file names.
    private static @Nullable SfntFont openWindowsHan(Path directory) {
        String[] preferred = {
                "Deng.ttf", "Dengb.ttf", "Dengl.ttf", "malgun.ttf", "malgunbd.ttf", "simhei.ttf"
        };
        for (int index = 0; index < preferred.length; index++) {
            @Nullable SfntFont font = FontDirectories.tryOpen(directory.resolve(preferred[index]));
            if (font != null && font.hasGlyph(0x4E00)) {
                return font;
            }
        }
        List<Path> files = FontDirectories.listSfnt(directory);
        int inspected = 0;
        for (int index = 0; index < files.size() && inspected < 32; index++) {
            Path path = files.get(index);
            if (!path.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".ttf")) {
                continue;
            }
            inspected++;
            @Nullable SfntFont font = FontDirectories.tryOpen(path);
            if (font != null && font.hasGlyph(0x4E00)) {
                return font;
            }
        }
        return null;
    }
}

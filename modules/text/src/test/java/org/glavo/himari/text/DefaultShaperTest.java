package org.glavo.himari.text;

import org.glavo.himari.font.BitmapSfntFont;
import org.glavo.himari.font.ScriptSampleFont;
import org.glavo.himari.font.SfntFont;
import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies default, Arabic, and Hebrew presentation shaping.
@NotNullByDefault
final class DefaultShaperTest {
    /// Maps Latin one-to-one through the bundled sample font.
    @Test
    void shapesLatinOneToOne() {
        SfntFont font = BitmapSfntFont.create();
        List<ShapedGlyph> glyphs = DefaultShaper.shape(font, "AB");
        assertEquals(2, glyphs.size());
        assertEquals('A', glyphs.get(0).codePoint());
        assertEquals(font.glyphId('A'), glyphs.get(0).glyphId());
        assertEquals(0, glyphs.get(0).cluster());
        assertEquals(1, glyphs.get(1).cluster());
    }

    /// Maps isolated, initial, medial, and final Beh through Presentation Forms-B.
    @Test
    void shapesArabicInitMediFina() {
        SfntFont font = ScriptSampleFont.create();
        List<ShapedGlyph> isolated = DefaultShaper.shape(font, "\u0628");
        assertEquals(1, isolated.size());
        assertEquals(0xFE8F, isolated.getFirst().codePoint());
        assertNotEquals(font.glyphId('\u0628'), isolated.getFirst().glyphId());
        assertTrue(isolated.getFirst().glyphId() > 0);

        List<ShapedGlyph> pair = DefaultShaper.shape(font, "\u0628\u062A");
        assertEquals(0xFE91, pair.get(0).codePoint());
        assertEquals(0xFE96, pair.get(1).codePoint());

        List<ShapedGlyph> triple = DefaultShaper.shape(font, "\u0628\u0628\u0628");
        assertEquals(0xFE91, triple.get(0).codePoint());
        assertEquals(0xFE92, triple.get(1).codePoint());
        assertEquals(0xFE90, triple.get(2).codePoint());
    }

    /// Maps Beh plus Alef to initial plus final forms.
    @Test
    void shapesArabicRightJoiningAlef() {
        SfntFont font = ScriptSampleFont.create();
        List<ShapedGlyph> joined = DefaultShaper.shape(font, "\u0628\u0627");
        assertEquals(0xFE91, joined.get(0).codePoint());
        assertEquals(0xFE8E, joined.get(1).codePoint());
        List<ShapedGlyph> split = DefaultShaper.shape(font, "\u0627\u0628");
        assertEquals(0xFE8D, split.get(0).codePoint());
        assertEquals(0xFE8F, split.get(1).codePoint());
    }

    /// Keeps a fatha in the preceding letter cluster and still joins across it.
    @Test
    void joinsAcrossArabicMark() {
        SfntFont font = ScriptSampleFont.create();
        List<ShapedGlyph> glyphs = DefaultShaper.shape(font, "\u0628\u064E\u062A");
        assertEquals(3, glyphs.size());
        assertEquals(0xFE91, glyphs.get(0).codePoint());
        assertEquals(0x064E, glyphs.get(1).codePoint());
        assertEquals(0, glyphs.get(1).cluster());
        assertEquals(0xFE96, glyphs.get(2).codePoint());
        assertEquals(2, glyphs.get(2).cluster());
        assertEquals(0, glyphs.get(1).xAdvance());
    }

    /// Leaves unmarked Hebrew as one-to-one cmap mapping.
    @Test
    void shapesUnmarkedHebrew() {
        SfntFont font = ScriptSampleFont.create();
        List<ShapedGlyph> glyphs = DefaultShaper.shape(font, "\u05D0\u05D1");
        assertEquals(2, glyphs.size());
        assertEquals(0x05D0, glyphs.get(0).codePoint());
        assertEquals(0x05D1, glyphs.get(1).codePoint());
        assertTrue(glyphs.get(0).glyphId() > 0);
        assertNotEquals(glyphs.get(0).glyphId(), glyphs.get(1).glyphId());
    }

    /// Composes Bet plus dagesh onto a Presentation Forms-A glyph.
    @Test
    void composesHebrewDagesh() {
        SfntFont font = ScriptSampleFont.create();
        List<ShapedGlyph> glyphs = DefaultShaper.shape(font, "\u05D1\u05BC");
        assertEquals(1, glyphs.size());
        assertEquals(0xFB31, glyphs.getFirst().codePoint());
        assertNotEquals(font.glyphId('\u05D1'), glyphs.getFirst().glyphId());
        assertEquals(0, glyphs.getFirst().cluster());
    }

    /// Composes Hangul choseong plus jungseong into `가`.
    @Test
    void composesHangulLvSyllable() {
        SfntFont font = ScriptSampleFont.create();
        List<ShapedGlyph> glyphs = DefaultShaper.shape(font, "\u1100\u1161");
        assertEquals(1, glyphs.size());
        assertEquals(0xAC00, glyphs.getFirst().codePoint());
        assertNotEquals(font.glyphId('\u1100'), glyphs.getFirst().glyphId());
        assertEquals(0, glyphs.getFirst().cluster());
    }

    /// Composes Hangul LVT into `각`.
    @Test
    void composesHangulLvtSyllable() {
        SfntFont font = ScriptSampleFont.create();
        List<ShapedGlyph> glyphs = DefaultShaper.shape(font, "\u1100\u1161\u11A8");
        assertEquals(1, glyphs.size());
        assertEquals(0xAC01, glyphs.getFirst().codePoint());
        assertEquals(0, glyphs.getFirst().cluster());
    }

    /// Decomposes Thai SARA AM through the shipped shaper and `cmap`.
    @Test
    void shapesThaiSaraAm() {
        SfntFont font = ScriptSampleFont.create();
        List<ShapedGlyph> glyphs = DefaultShaper.shape(font, "\u0E01\u0E33");
        assertEquals(3, glyphs.size());
        assertEquals(0x0E01, glyphs.get(0).codePoint());
        assertEquals(0x0E4D, glyphs.get(1).codePoint());
        assertEquals(0x0E32, glyphs.get(2).codePoint());
        assertEquals(0, glyphs.get(0).cluster());
        assertEquals(0, glyphs.get(1).cluster());
        assertEquals(0, glyphs.get(2).cluster());
        assertTrue(glyphs.get(0).glyphId() > 0);
        assertTrue(glyphs.get(1).glyphId() > 0);
        assertNotEquals(glyphs.get(0).glyphId(), glyphs.get(1).glyphId());
        assertEquals(0, glyphs.get(1).xAdvance());
    }

    /// Reorders Nikhahit over MAI TRI on the shipped path.
    @Test
    void shapesThaiSaraAmAfterTone() {
        SfntFont font = ScriptSampleFont.create();
        List<ShapedGlyph> glyphs = DefaultShaper.shape(font, "\u0E14\u0E4B\u0E33");
        assertEquals(4, glyphs.size());
        assertEquals(0x0E14, glyphs.get(0).codePoint());
        assertEquals(0x0E4D, glyphs.get(1).codePoint());
        assertEquals(0x0E4B, glyphs.get(2).codePoint());
        assertEquals(0x0E32, glyphs.get(3).codePoint());
    }

    /// Leaves a Unicode-visual left vowel before its consonant.
    @Test
    void keepsThaiLeftVowelInVisualOrder() {
        SfntFont font = ScriptSampleFont.create();
        List<ShapedGlyph> glyphs = DefaultShaper.shape(font, "\u0E40\u0E01");
        assertEquals(2, glyphs.size());
        assertEquals(0x0E40, glyphs.get(0).codePoint());
        assertEquals(0x0E01, glyphs.get(1).codePoint());
        assertEquals(0, glyphs.get(0).cluster());
        assertEquals(1, glyphs.get(1).cluster());
    }

    /// Decomposes Lao SARA AM through the shipped shaper.
    @Test
    void shapesLaoSaraAm() {
        SfntFont font = ScriptSampleFont.create();
        List<ShapedGlyph> glyphs = DefaultShaper.shape(font, "\u0E81\u0EB3");
        assertEquals(3, glyphs.size());
        assertEquals(0x0E81, glyphs.get(0).codePoint());
        assertEquals(0x0ECD, glyphs.get(1).codePoint());
        assertEquals(0x0EB2, glyphs.get(2).codePoint());
    }

    /// Composes Shin plus shin-dot onto `U+FB2A`.
    @Test
    void composesHebrewShinDot() {
        SfntFont font = ScriptSampleFont.create();
        List<ShapedGlyph> glyphs = DefaultShaper.shape(font, "\u05E9\u05C1");
        assertEquals(1, glyphs.size());
        assertEquals(0xFB2A, glyphs.getFirst().codePoint());
    }
}

package org.glavo.himari.text;

import org.glavo.himari.font.BitmapSfntFont;
import org.glavo.himari.font.GposMarkSampleFont;
import org.glavo.himari.font.GposSampleFont;
import org.glavo.himari.font.SfntFont;
import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies GPOS pair application through the shaper and first-stable line wrapping.
@NotNullByDefault
final class ParagraphLayoutTest {
    /// Shapes `AV` with the GPOS kern delta on `A`.
    @Test
    void shapesAvWithGposKern() {
        SfntFont font = GposSampleFont.create();
        List<ShapedGlyph> glyphs = DefaultShaper.shape(font, "AV");
        assertEquals(2, glyphs.size());
        assertEquals(GposSampleFont.ADVANCE_LETTER + GposSampleFont.KERN_AV, glyphs.get(0).xAdvance());
        assertEquals(GposSampleFont.ADVANCE_LETTER, glyphs.get(1).xAdvance());
        List<ShapedGlyph> isolated = DefaultShaper.shape(font, "A");
        assertEquals(GposSampleFont.ADVANCE_LETTER, isolated.getFirst().xAdvance());
    }

    /// Attaches a GPOS mark through the shipped shaper.
    @Test
    void shapesMarkWithGposOffset() {
        SfntFont font = GposMarkSampleFont.create();
        List<ShapedGlyph> glyphs = DefaultShaper.shape(font, "\u0628\u064E");
        assertEquals(2, glyphs.size());
        assertEquals(GposMarkSampleFont.GLYPH_BEH, glyphs.get(0).glyphId());
        assertEquals(GposMarkSampleFont.GLYPH_FATHA, glyphs.get(1).glyphId());
        assertEquals(0, glyphs.get(1).xAdvance());
        assertEquals(GposMarkSampleFont.MARK_X_OFFSET, glyphs.get(1).xOffset());
        assertEquals(GposMarkSampleFont.MARK_Y_OFFSET, glyphs.get(1).yOffset());
        assertEquals(0, glyphs.get(0).xOffset());
    }

    /// Wraps after a space when the next word would overflow.
    @Test
    void wrapsAfterSpace() {
        SfntFont font = BitmapSfntFont.create();
        int letter = font.metrics(font.glyphId('A')).advanceWidth();
        int space = font.metrics(font.glyphId(' ')).advanceWidth();
        int maxWidth = 4 * letter + space + letter / 2;
        List<LaidLine> lines = ParagraphLayout.layout(font, "AAAA AAAA", maxWidth);
        assertEquals(2, lines.size());
        assertEquals(5, lines.get(0).glyphs().size());
        assertEquals(4, lines.get(1).glyphs().size());
        assertEquals(' ', lines.get(0).glyphs().get(4).codePoint());
        assertEquals('A', lines.get(1).glyphs().getFirst().codePoint());
        assertTrue(lines.get(0).width() <= maxWidth);
        assertTrue(lines.get(1).width() <= maxWidth);
    }

    /// Breaks a long unspaced run before the overflowing glyph.
    @Test
    void breaksOverflowingRun() {
        SfntFont font = BitmapSfntFont.create();
        int letter = font.metrics(font.glyphId('A')).advanceWidth();
        List<LaidLine> lines = ParagraphLayout.layout(font, "AAAA", letter * 2);
        assertEquals(2, lines.size());
        assertEquals(2, lines.get(0).glyphs().size());
        assertEquals(2, lines.get(1).glyphs().size());
        assertEquals(letter * 2, lines.get(0).width());
    }

    /// Treats U+000A as a hard break even when width remains.
    @Test
    void splitsOnNewline() {
        SfntFont font = BitmapSfntFont.create();
        List<LaidLine> lines = ParagraphLayout.layout(font, "A\nB", 1000);
        assertEquals(2, lines.size());
        assertEquals(1, lines.get(0).glyphs().size());
        assertEquals(1, lines.get(1).glyphs().size());
        assertEquals('A', lines.get(0).glyphs().getFirst().codePoint());
        assertEquals('B', lines.get(1).glyphs().getFirst().codePoint());
        assertEquals(0, lines.get(0).startCluster());
        assertEquals(2, lines.get(1).startCluster());
    }

    /// Places carets at the prefix-advance of each cluster.
    @Test
    void caretFollowsPrefixAdvance() {
        SfntFont font = BitmapSfntFont.create();
        LaidLine line = ParagraphLayout.layout(font, "AB", 1000).getFirst();
        int advance = font.metrics(font.glyphId('A')).advanceWidth();
        assertEquals(0, line.caretX(0));
        assertEquals(advance, line.caretX(1));
        assertEquals(line.width(), line.caretX(2));
    }

    /// Wraps a mixed Latin and Arabic collection without dropping the fallback run.
    @Test
    void wrapsMixedFontCollection() {
        FontCollection fonts = new FontCollection(BitmapSfntFont.create(), GposMarkSampleFont.create());
        int letter = fonts.primary().metrics(fonts.primary().glyphId('A')).advanceWidth();
        List<LaidLine> lines = ParagraphLayout.layout(fonts, "AA\u0628", letter * 2);
        assertEquals(2, lines.size());
        assertEquals(2, lines.get(0).glyphs().size());
        assertEquals(1, lines.get(1).glyphs().size());
        assertEquals('A', lines.get(0).glyphs().getFirst().codePoint());
        assertEquals(0, lines.get(0).glyphs().getFirst().fontIndex());
        assertEquals(GposMarkSampleFont.GLYPH_BEH, lines.get(1).glyphs().getFirst().glyphId());
        assertEquals(1, lines.get(1).glyphs().getFirst().fontIndex());
        assertEquals(letter * 2, lines.get(0).width());
        assertEquals(GposMarkSampleFont.ADVANCE_BEH, lines.get(1).width());
    }

    /// Shapes a GPOS span and a Latin span into one paragraph.
    @Test
    void layoutsStyledSpansWithKernAndFallbackFace() {
        SfntFont kern = GposSampleFont.create();
        SfntFont latin = BitmapSfntFont.create();
        ShapedText shaped = StyledShaper.shape(new TextSpan("AV", kern), new TextSpan("C", latin));
        assertEquals(3, shaped.glyphs().size());
        assertEquals(GposSampleFont.ADVANCE_LETTER + GposSampleFont.KERN_AV, shaped.glyphs().get(0).xAdvance());
        assertSame(kern, shaped.fontOf(shaped.glyphs().get(0)));
        assertSame(latin, shaped.fontOf(shaped.glyphs().get(2)));
        assertEquals(latin.glyphId('C'), shaped.glyphs().get(2).glyphId());
        assertEquals(2, shaped.glyphs().get(2).cluster());
        int letter = latin.metrics(latin.glyphId('C')).advanceWidth();
        List<LaidLine> lines = ParagraphLayout.layout(
                letter * 2,
                new TextSpan("AA", latin),
                new TextSpan("\u0628", GposMarkSampleFont.create())
        );
        assertEquals(2, lines.size());
        assertEquals(2, lines.get(0).glyphs().size());
        assertEquals(GposMarkSampleFont.GLYPH_BEH, lines.get(1).glyphs().getFirst().glyphId());
    }

    /// Counts the newline as a cluster when it sits at the start of a later span.
    @Test
    void styledNewlineKeepsClusterAcrossSpans() {
        SfntFont latin = BitmapSfntFont.create();
        List<LaidLine> lines = ParagraphLayout.layout(1000, new TextSpan("A", latin), new TextSpan("\nB", latin));
        assertEquals(2, lines.size());
        assertEquals(0, lines.get(0).startCluster());
        assertEquals(2, lines.get(1).startCluster());
        assertEquals('A', lines.get(0).glyphs().getFirst().codePoint());
        assertEquals('B', lines.get(1).glyphs().getFirst().codePoint());
    }
}

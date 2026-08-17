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

/// Verifies GPOS pair application, wrapping, justification, and soft-hyphen breaks.
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

    /// Truncates a wrapping paragraph to one line ending in U+2026.
    @Test
    void ellipsisTruncatesOverflowToOneLine() {
        SfntFont font = BitmapSfntFont.create();
        int letter = font.metrics(font.glyphId('A')).advanceWidth();
        List<LaidLine> wrapped = ParagraphLayout.layout(font, "AAAA", letter * 2);
        assertEquals(2, wrapped.size());
        List<LaidLine> lines = ParagraphLayout.layout(
                font,
                "AAAA",
                letter * 2,
                LineAlignment.START,
                true
        );
        assertEquals(1, lines.size());
        LaidLine line = lines.getFirst();
        assertEquals(0x2026, line.glyphs().getLast().codePoint());
        assertTrue(line.width() <= letter * 2);
        assertTrue(line.glyphs().size() < 4);
    }

    /// Advances U+0009 to the next four-space tab stop.
    @Test
    void tabAdvancesToNextStop() {
        SfntFont font = BitmapSfntFont.create();
        int space = font.metrics(font.glyphId(' ')).advanceWidth();
        int letter = font.metrics(font.glyphId('A')).advanceWidth();
        List<LaidLine> lines = ParagraphLayout.layout(font, "A\tB", 1000);
        assertEquals(1, lines.size());
        LaidLine line = lines.getFirst();
        assertEquals(3, line.glyphs().size());
        assertEquals(0x09, line.glyphs().get(1).codePoint());
        int tabStop = 4 * space;
        int expectedTab = tabStop - (letter % tabStop);
        assertEquals(expectedTab, line.glyphs().get(1).xAdvance());
        assertEquals(letter + expectedTab + letter, line.width());
    }

    /// Centers leftover width before the first glyph.
    @Test
    void centersLeftoverWidth() {
        SfntFont font = BitmapSfntFont.create();
        int letter = font.metrics(font.glyphId('A')).advanceWidth();
        LaidLine line = ParagraphLayout.layout(font, "AA", letter * 4, LineAlignment.CENTER).getFirst();
        assertEquals(letter, line.indent());
        assertEquals(letter, line.caretX(0));
        assertEquals(letter * 3, line.caretX(2));
    }

    /// Flushes leftover width to the trailing edge.
    @Test
    void endsFlushToTrailingEdge() {
        SfntFont font = BitmapSfntFont.create();
        int letter = font.metrics(font.glyphId('A')).advanceWidth();
        LaidLine line = ParagraphLayout.layout(font, "AA", letter * 4, LineAlignment.END).getFirst();
        assertEquals(letter * 2, line.indent());
        assertEquals(letter * 2, line.caretX(0));
        assertEquals(letter * 4, line.caretX(2));
    }

    /// Indents the first line of a paragraph by the requested advance.
    @Test
    void firstLineIndentShiftsCaret() {
        SfntFont font = BitmapSfntFont.create();
        int letter = font.metrics(font.glyphId('A')).advanceWidth();
        List<LaidLine> lines = ParagraphLayout.layout(font, "AA AA", letter * 4, letter);
        assertEquals(letter, lines.getFirst().indent());
        assertEquals(letter, lines.getFirst().caretX(0));
        if (lines.size() > 1) {
            assertEquals(0, lines.get(1).indent());
        }
        List<LaidLine> hanging = ParagraphLayout.layout(font, "AAAA AAAA", letter * 4, 0, letter);
        assertTrue(hanging.size() > 1);
        assertEquals(0, hanging.getFirst().indent());
        assertEquals(letter, hanging.get(1).indent());
        List<LaidLine> last = ParagraphLayout.layout(font, "AAAA AAAA", letter * 4, 0, 0, letter);
        assertTrue(last.size() > 1);
        assertEquals(0, last.getFirst().indent());
        assertEquals(letter, last.get(last.size() - 1).indent());
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

    /// Spreads leftover width onto spaces on every non-last paragraph line.
    @Test
    void justifiesNonLastLineOntoSpaces() {
        SfntFont font = BitmapSfntFont.create();
        int letter = font.metrics(font.glyphId('A')).advanceWidth();
        int space = font.metrics(font.glyphId(' ')).advanceWidth();
        int maxWidth = 2 * letter + space + 2 * letter + space + 1;
        List<LaidLine> start = ParagraphLayout.layout(font, "AA AA AA", maxWidth);
        assertEquals(2, start.size());
        assertEquals(2 * letter + space + 2 * letter + space, start.get(0).width());
        List<LaidLine> justified = ParagraphLayout.layout(font, "AA AA AA", maxWidth, LineAlignment.JUSTIFY);
        assertEquals(2, justified.size());
        assertEquals(maxWidth, justified.get(0).width());
        assertEquals(start.get(1).width(), justified.get(1).width());
        int extra = maxWidth - start.get(0).width();
        int firstSpace = justified.get(0).glyphs().get(2).xAdvance();
        int secondSpace = justified.get(0).glyphs().get(5).xAdvance();
        assertEquals(space + extra / 2 + extra % 2, firstSpace);
        assertEquals(space + extra / 2, secondSpace);
        assertEquals(maxWidth, justified.get(0).caretX(justified.get(0).endClusterExclusive()));
    }

    /// Leaves the last hard-broken line unstretched.
    @Test
    void doesNotJustifyLastLineAfterNewline() {
        SfntFont font = BitmapSfntFont.create();
        int letter = font.metrics(font.glyphId('A')).advanceWidth();
        int space = font.metrics(font.glyphId(' ')).advanceWidth();
        int maxWidth = 2 * letter + space + 2 * letter + 4;
        List<LaidLine> lines = ParagraphLayout.layout(font, "AA AA\nB", maxWidth, LineAlignment.JUSTIFY);
        assertEquals(2, lines.size());
        assertEquals(2 * letter + space + 2 * letter, lines.get(0).width());
        assertEquals(letter, lines.get(1).width());
    }

    /// Breaks at U+00AD, emits U+002D on the first line, and drops an unused soft hyphen.
    @Test
    void hyphenatesAtSoftHyphenAndOmitsUnusedMark() {
        SfntFont font = BitmapSfntFont.create();
        int letter = font.metrics(font.glyphId('A')).advanceWidth();
        int hyphen = font.metrics(font.glyphId('-')).advanceWidth();
        List<LaidLine> unused = ParagraphLayout.layout(font, "AA\u00ADAA", 1000);
        assertEquals(1, unused.size());
        assertEquals(4, unused.getFirst().glyphs().size());
        assertEquals(4 * letter, unused.getFirst().width());
        List<LaidLine> broken = ParagraphLayout.layout(font, "AAA\u00ADAAA", 3 * letter + hyphen);
        assertEquals(2, broken.size());
        LaidLine first = broken.get(0);
        assertEquals(4, first.glyphs().size());
        assertEquals('-', first.glyphs().get(3).codePoint());
        assertEquals(font.glyphId('-'), first.glyphs().get(3).glyphId());
        assertEquals(3 * letter + hyphen, first.width());
        assertEquals(3, broken.get(1).glyphs().size());
        assertEquals('A', broken.get(1).glyphs().getFirst().codePoint());
    }

    /// Breaks a dictionary word before `tion` and emits a visible hyphen.
    @Test
    void hyphenatesDictionaryWordBeforeOverflow() {
        SfntFont font = BitmapSfntFont.create();
        int letter = font.metrics(font.glyphId('h')).advanceWidth();
        int hyphen = font.metrics(font.glyphId('-')).advanceWidth();
        List<LaidLine> lines = ParagraphLayout.layout(font, "hyphenation", 6 * letter + hyphen);
        assertEquals(2, lines.size());
        LaidLine first = lines.get(0);
        assertEquals('-', first.glyphs().getLast().codePoint());
        assertEquals(6 * letter + hyphen, first.width());
        assertEquals("ation", wordOf(lines.get(1)));
    }

    /// Concatenates glyph code points on `line`.
    private static String wordOf(LaidLine line) {
        StringBuilder word = new StringBuilder();
        for (ShapedGlyph glyph : line.glyphs()) {
            word.appendCodePoint(glyph.codePoint());
        }
        return word.toString();
    }

    /// Reorders a Hebrew run into visual order and places logical carets on the leading edge.
    @Test
    void reordersHebrewRunAndMapsVisualCaret() {
        SfntFont font = BitmapSfntFont.create();
        int missing = font.metrics(0).advanceWidth();
        LaidLine line = ParagraphLayout.layout(font, "\u05D0\u05D1", 1000).getFirst();
        assertEquals(2, line.glyphs().size());
        assertEquals('\u05D1', line.glyphs().get(0).codePoint());
        assertEquals('\u05D0', line.glyphs().get(1).codePoint());
        assertEquals(2 * missing, line.width());
        assertEquals(2 * missing, line.caretX(0));
        assertEquals(missing, line.caretX(1));
        assertEquals(0, line.caretX(2));
        assertEquals(0, line.selectionLeft(0, 2));
        assertEquals(2 * missing, line.selectionWidth(0, 2));
        assertEquals(missing, line.selectionLeft(0, 1));
        assertEquals(missing, line.selectionWidth(0, 1));
        assertEquals(0, line.clusterAt(2 * missing));
        assertEquals(1, line.clusterAt(missing));
        assertEquals(2, line.clusterAt(0));
    }

    /// Keeps Latin visual order and prefix carets.
    @Test
    void mixedLatinHebrewReordersOnlyTheRtlRun() {
        SfntFont font = BitmapSfntFont.create();
        int letter = font.metrics(font.glyphId('a')).advanceWidth();
        int missing = font.metrics(0).advanceWidth();
        LaidLine line = ParagraphLayout.layout(font, "ab\u05D0\u05D1cd", 1000).getFirst();
        assertEquals(6, line.glyphs().size());
        assertEquals('a', line.glyphs().get(0).codePoint());
        assertEquals('b', line.glyphs().get(1).codePoint());
        assertEquals('\u05D1', line.glyphs().get(2).codePoint());
        assertEquals('\u05D0', line.glyphs().get(3).codePoint());
        assertEquals('c', line.glyphs().get(4).codePoint());
        assertEquals(0, line.caretX(0));
        assertEquals(letter, line.caretX(1));
        assertEquals(2 * letter + 2 * missing, line.caretX(2));
        assertEquals(2 * letter + missing, line.caretX(3));
        assertEquals(2 * letter + 2 * missing, line.caretX(4));
        assertEquals(2 * letter + 2 * missing + letter, line.caretX(5));
        assertEquals(4 * letter + 2 * missing, line.caretX(6));
        assertEquals(2 * letter, line.selectionLeft(2, 4));
        assertEquals(2 * missing, line.selectionWidth(2, 4));
        assertEquals(0, line.clusterAt(0));
        assertEquals(1, line.clusterAt(letter));
        assertEquals(6, line.clusterAt(4 * letter + 2 * missing));
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

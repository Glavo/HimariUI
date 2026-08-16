package org.glavo.himari.text;

import org.glavo.himari.font.SfntFont;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/// Wraps shaped glyphs into lines at a maximum advance.
///
/// Hard breaks follow U+000A. Soft breaks occur after U+0020 when the next glyph would exceed
/// `maxWidth`. A run without a space that exceeds `maxWidth` breaks before the overflowing glyph,
/// keeping at least one glyph per line. Leading spaces after a wrap are not skipped when they were
/// the break opportunity; they stay on the preceding line. Styled spans may change faces mid
/// paragraph; U+000A still ends the paragraph even when it sits inside a span. U+00AD is a
/// soft-hyphen break: it never contributes width, and a taken break replaces it with U+002D
/// from the same face. [`LatinHyphenator`] supplies additional letter-run breaks. [`LineAlignment#JUSTIFY`] distributes leftover width onto U+0020 on
/// every non-last paragraph line. Each line is then reordered into visual order with
/// [`BidiOrder`] paragraph-LTR levels, and [`LaidLine#caretX(int)`] uses those levels.
@NotNullByDefault
public final class ParagraphLayout {
    /// Prevents instantiation.
    private ParagraphLayout() {
    }

    /// Shapes `text` and wraps it to `maxWidth` font units.
    ///
    /// @param font the font
    /// @param text the source text
    /// @param maxWidth the positive maximum line advance
    /// @return the wrapped lines, empty when `text` is empty
    public static @Unmodifiable List<LaidLine> layout(SfntFont font, String text, int maxWidth) {
        return layout(new FontCollection(font), text, maxWidth, LineAlignment.START);
    }

    /// Shapes `text` and wraps it to `maxWidth` font units using `alignment`.
    ///
    /// @param font the font
    /// @param text the source text
    /// @param maxWidth the positive maximum line advance
    /// @param alignment leftover-width policy
    /// @return the wrapped lines, empty when `text` is empty
    public static @Unmodifiable List<LaidLine> layout(
            SfntFont font,
            String text,
            int maxWidth,
            LineAlignment alignment
    ) {
        return layout(new FontCollection(font), text, maxWidth, alignment);
    }

    /// Shapes `text` through `fonts` and wraps it to `maxWidth` primary-em units.
    ///
    /// @param fonts the ordered faces
    /// @param text the source text
    /// @param maxWidth the positive maximum line advance
    /// @return the wrapped lines, empty when `text` is empty
    public static @Unmodifiable List<LaidLine> layout(FontCollection fonts, String text, int maxWidth) {
        return layout(fonts, text, maxWidth, LineAlignment.START);
    }

    /// Shapes `text` through `fonts` and wraps it to `maxWidth` using `alignment`.
    ///
    /// @param fonts the ordered faces
    /// @param text the source text
    /// @param maxWidth the positive maximum line advance
    /// @param alignment leftover-width policy
    /// @return the wrapped lines, empty when `text` is empty
    public static @Unmodifiable List<LaidLine> layout(
            FontCollection fonts,
            String text,
            int maxWidth,
            LineAlignment alignment
    ) {
        Objects.requireNonNull(fonts, "fonts");
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(alignment, "alignment");
        if (maxWidth <= 0) {
            throw new IllegalArgumentException("maxWidth must be positive");
        }
        if (text.isEmpty()) {
            return List.of();
        }
        ArrayList<LaidLine> lines = new ArrayList<>();
        int start = 0;
        int clusterBase = 0;
        int utf16Length = text.length();
        while (start <= utf16Length) {
            int newline = text.indexOf('\n', start);
            int end = newline < 0 ? utf16Length : newline;
            wrapParagraph(fonts, text.substring(start, end), maxWidth, clusterBase, lines, alignment);
            if (newline < 0) {
                break;
            }
            clusterBase += text.codePointCount(start, newline) + 1;
            start = newline + 1;
            if (start == utf16Length) {
                lines.add(new LaidLine(List.of(), 0, clusterBase, clusterBase));
                break;
            }
        }
        return List.copyOf(lines);
    }

    /// Shapes styled spans and wraps them to `maxWidth` units of the first span's primary em.
    ///
    /// @param maxWidth the positive maximum line advance
    /// @param spans the styled runs in logical order
    /// @return the wrapped lines, empty when every span is empty
    public static @Unmodifiable List<LaidLine> layout(int maxWidth, TextSpan... spans) {
        return layout(maxWidth, LineAlignment.START, spans);
    }

    /// Shapes styled spans and wraps them using `alignment`.
    ///
    /// @param maxWidth the positive maximum line advance
    /// @param alignment leftover-width policy
    /// @param spans the styled runs in logical order
    /// @return the wrapped lines, empty when every span is empty
    public static @Unmodifiable List<LaidLine> layout(int maxWidth, LineAlignment alignment, TextSpan... spans) {
        Objects.requireNonNull(spans, "spans");
        Objects.requireNonNull(alignment, "alignment");
        if (maxWidth <= 0) {
            throw new IllegalArgumentException("maxWidth must be positive");
        }
        if (spans.length == 0) {
            return List.of();
        }
        ArrayList<LaidLine> lines = new ArrayList<>();
        ArrayList<TextSpan> paragraph = new ArrayList<>();
        int clusterBase = 0;
        boolean sawCharacter = false;
        for (int spanIndex = 0; spanIndex < spans.length; spanIndex++) {
            TextSpan span = Objects.requireNonNull(spans[spanIndex], "span");
            String text = span.text();
            int start = 0;
            int utf16Length = text.length();
            if (utf16Length > 0) {
                sawCharacter = true;
            }
            while (start <= utf16Length) {
                int newline = text.indexOf('\n', start);
                int end = newline < 0 ? utf16Length : newline;
                paragraph.add(new TextSpan(text.substring(start, end), span.fonts()));
                if (newline < 0) {
                    break;
                }
                clusterBase += flushStyledParagraph(paragraph, maxWidth, clusterBase, lines, alignment) + 1;
                start = newline + 1;
                if (start == utf16Length && spanIndex == spans.length - 1) {
                    lines.add(new LaidLine(List.of(), 0, clusterBase, clusterBase));
                    return List.copyOf(lines);
                }
            }
        }
        if (!sawCharacter) {
            return List.of();
        }
        if (!paragraph.isEmpty()) {
            flushStyledParagraph(paragraph, maxWidth, clusterBase, lines, alignment);
        }
        return List.copyOf(lines);
    }

    /// Shapes one paragraph of styled slices that contain no U+000A.
    ///
    /// @return the code-point count of every slice flushed, including empty ones
    private static int flushStyledParagraph(
            List<TextSpan> paragraph,
            int maxWidth,
            int clusterBase,
            List<LaidLine> lines,
            LineAlignment alignment
    ) {
        ArrayList<TextSpan> nonempty = new ArrayList<>(paragraph.size());
        int clusters = 0;
        for (int index = 0; index < paragraph.size(); index++) {
            TextSpan span = paragraph.get(index);
            String text = span.text();
            clusters += text.codePointCount(0, text.length());
            if (!text.isEmpty()) {
                nonempty.add(span);
            }
        }
        paragraph.clear();
        if (nonempty.isEmpty()) {
            lines.add(new LaidLine(List.of(), 0, clusterBase, clusterBase));
            return clusters;
        }
        ShapedText shaped = StyledShaper.shape(nonempty.toArray(TextSpan[]::new));
        wrapGlyphs(shaped.glyphs(), shaped.fonts(), maxWidth, clusterBase, lines, alignment);
        return clusters;
    }

    /// Wraps one paragraph that contains no U+000A.
    private static void wrapParagraph(
            FontCollection fonts,
            String paragraph,
            int maxWidth,
            int clusterBase,
            List<LaidLine> lines,
            LineAlignment alignment
    ) {
        SfntFont[] faces = new SfntFont[fonts.size()];
        for (int index = 0; index < faces.length; index++) {
            faces[index] = fonts.font(index);
        }
        wrapGlyphs(FallbackShaper.shape(fonts, paragraph), faces, maxWidth, clusterBase, lines, alignment);
    }

    /// Wraps already-shaped paragraph glyphs, applying soft-hyphen breaks and leftover alignment.
    private static void wrapGlyphs(
            List<ShapedGlyph> glyphs,
            SfntFont[] fonts,
            int maxWidth,
            int clusterBase,
            List<LaidLine> lines,
            LineAlignment alignment
    ) {
        int count = glyphs.size();
        int firstLine = lines.size();
        if (count == 0) {
            lines.add(new LaidLine(List.of(), 0, clusterBase, clusterBase));
            return;
        }
        int lineStart = 0;
        int lastBreak = -1;
        boolean lastBreakHyphen = false;
        int width = 0;
        for (int index = 0; index < count; index++) {
            ShapedGlyph glyph = glyphs.get(index);
            if (glyph.codePoint() == 0x00AD || BidiOrder.isControl(glyph.codePoint())) {
                if (glyph.codePoint() == 0x00AD) {
                    int hyphen = hyphenAdvance(fonts, glyph);
                    if (width + hyphen <= maxWidth) {
                        lastBreak = index + 1;
                        lastBreakHyphen = true;
                    }
                }
                continue;
            }
            int advance = glyph.xAdvance();
            if (width + advance > maxWidth && index > lineStart) {
                int dictionary = lastBreak > lineStart
                        ? -1
                        : dictionaryBreak(glyphs, fonts, lineStart, index, maxWidth);
                int end;
                boolean hyphenate;
                if (lastBreak > lineStart) {
                    end = lastBreak;
                    hyphenate = lastBreakHyphen;
                } else if (dictionary > lineStart) {
                    end = dictionary;
                    hyphenate = true;
                } else {
                    end = index;
                    hyphenate = false;
                }
                lines.add(slice(glyphs, fonts, lineStart, end, clusterBase, hyphenate));
                lineStart = end;
                lastBreak = -1;
                lastBreakHyphen = false;
                width = 0;
                index = lineStart - 1;
                continue;
            }
            width += advance;
            if (glyph.codePoint() == 0x20) {
                lastBreak = index + 1;
                lastBreakHyphen = false;
            }
        }
        if (lineStart < count) {
            lines.add(slice(glyphs, fonts, lineStart, count, clusterBase, false));
        }
        if (alignment == LineAlignment.JUSTIFY) {
            int last = lines.size() - 1;
            for (int index = firstLine; index < last; index++) {
                lines.set(index, justifyLine(lines.get(index), maxWidth));
            }
        }
        for (int index = firstLine; index < lines.size(); index++) {
            lines.set(index, visualize(lines.get(index)));
        }
    }

    /// Copies `glyphs[from, to)`, drops unused U+00AD, and replaces a trailing soft hyphen.
    private static LaidLine slice(
            List<ShapedGlyph> glyphs,
            SfntFont[] fonts,
            int from,
            int to,
            int clusterBase,
            boolean hyphenate
    ) {
        hyphenate = hyphenate || (to > from && glyphs.get(to - 1).codePoint() == 0x00AD);
        ArrayList<ShapedGlyph> line = new ArrayList<>(to - from + 1);
        int width = 0;
        int startCluster = clusterBase;
        int endCluster = clusterBase;
        boolean started = false;
        for (int index = from; index < to; index++) {
            ShapedGlyph glyph = glyphs.get(index);
            int cluster = glyph.cluster() + clusterBase;
            if (!started) {
                startCluster = cluster;
                started = true;
            }
            endCluster = cluster + 1;
            if (glyph.codePoint() == 0x00AD) {
                continue;
            }
            width += glyph.xAdvance();
            if (clusterBase == 0) {
                line.add(glyph);
            } else {
                line.add(new ShapedGlyph(
                        glyph.codePoint(),
                        glyph.glyphId(),
                        cluster,
                        glyph.xAdvance(),
                        glyph.xOffset(),
                        glyph.yOffset(),
                        glyph.fontIndex()
                ));
            }
        }
        if (hyphenate) {
            ShapedGlyph shy = glyphs.get(to - 1);
            int cluster = shy.cluster() + clusterBase;
            if (!started) {
                startCluster = cluster;
            }
            endCluster = cluster + 1;
            int hyphenWidth = hyphenAdvance(fonts, shy);
            int hyphenId = hyphenGlyph(fonts, shy);
            line.add(new ShapedGlyph('-', hyphenId, cluster, hyphenWidth, 0, 0, shy.fontIndex()));
            width += hyphenWidth;
        }
        return new LaidLine(line, width, startCluster, endCluster);
    }

    /// Spreads leftover width onto U+0020 advances.
    private static LaidLine justifyLine(LaidLine line, int maxWidth) {
        int extra = maxWidth - line.width();
        if (extra <= 0) {
            return line;
        }
        int spaces = 0;
        List<ShapedGlyph> glyphs = line.glyphs();
        for (int index = 0; index < glyphs.size(); index++) {
            if (glyphs.get(index).codePoint() == 0x20) {
                spaces++;
            }
        }
        if (spaces == 0) {
            return line;
        }
        int each = extra / spaces;
        int remainder = extra % spaces;
        ArrayList<ShapedGlyph> justified = new ArrayList<>(glyphs.size());
        int width = 0;
        int spaceIndex = 0;
        for (int index = 0; index < glyphs.size(); index++) {
            ShapedGlyph glyph = glyphs.get(index);
            int advance = glyph.xAdvance();
            if (glyph.codePoint() == 0x20) {
                advance += each;
                if (spaceIndex < remainder) {
                    advance++;
                }
                spaceIndex++;
                justified.add(new ShapedGlyph(
                        glyph.codePoint(),
                        glyph.glyphId(),
                        glyph.cluster(),
                        advance,
                        glyph.xOffset(),
                        glyph.yOffset(),
                        glyph.fontIndex()
                ));
            } else {
                justified.add(glyph);
            }
            width += justified.getLast().xAdvance();
        }
        return new LaidLine(justified, width, line.startCluster(), line.endClusterExclusive());
    }

    /// Returns the last dictionary hyphen break exclusive index in `[wordStart, overflow)`.
    private static int dictionaryBreak(
            List<ShapedGlyph> glyphs,
            SfntFont[] fonts,
            int wordStart,
            int overflow,
            int maxWidth
    ) {
        if (overflow <= wordStart + 1) {
            return -1;
        }
        int wordEnd = overflow;
        while (wordEnd < glyphs.size()) {
            int codePoint = glyphs.get(wordEnd).codePoint();
            if (codePoint == 0x20 || codePoint == 0x00AD || BidiOrder.isControl(codePoint)) {
                break;
            }
            wordEnd++;
        }
        int hyphen = hyphenAdvance(fonts, glyphs.get(wordStart));
        StringBuilder word = new StringBuilder();
        int width = 0;
        int lastFit = -1;
        for (int index = wordStart; index < wordEnd; index++) {
            int codePoint = glyphs.get(index).codePoint();
            if (codePoint == 0x20 || codePoint == 0x00AD || BidiOrder.isControl(codePoint)) {
                return -1;
            }
            word.appendCodePoint(codePoint);
            if (index < overflow) {
                width += glyphs.get(index).xAdvance();
                if (width + hyphen <= maxWidth) {
                    lastFit = word.length();
                }
            }
        }
        int breakAt = LatinHyphenator.lastBreak(word.toString(), lastFit);
        if (breakAt < 2) {
            return -1;
        }
        return wordStart + breakAt;
    }

    /// Reorders one line into visual order and attaches resolved bidi levels.
    private static LaidLine visualize(LaidLine line) {
        List<ShapedGlyph> glyphs = line.glyphs();
        int count = glyphs.size();
        if (count == 0) {
            return line;
        }
        ArrayList<ShapedGlyph> stripped = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            ShapedGlyph glyph = glyphs.get(index);
            if (!BidiOrder.isControl(glyph.codePoint())) {
                stripped.add(glyph);
            }
        }
        if (stripped.size() != count) {
            int width = 0;
            for (int index = 0; index < stripped.size(); index++) {
                width += stripped.get(index).xAdvance();
            }
            line = new LaidLine(stripped, width, line.startCluster(), line.endClusterExclusive());
            glyphs = line.glyphs();
            count = glyphs.size();
            if (count == 0) {
                return line;
            }
        }
        int[] points = new int[count];
        boolean rtl = false;
        for (int index = 0; index < count; index++) {
            int codePoint = glyphs.get(index).codePoint();
            points[index] = codePoint;
            if (BidiOrder.level(codePoint) == BidiOrder.RTL) {
                rtl = true;
            }
        }
        if (!rtl) {
            return line;
        }
        int[] levels = BidiOrder.levels(points);
        int[] order = new int[count];
        for (int index = 0; index < count; index++) {
            order[index] = index;
        }
        BidiOrder.reorderRtlRuns(order, levels);
        ArrayList<ShapedGlyph> visual = new ArrayList<>(count);
        int[] visualLevels = new int[count];
        for (int index = 0; index < count; index++) {
            int source = order[index];
            visual.add(glyphs.get(source));
            visualLevels[index] = levels[index];
        }
        return new LaidLine(visual, line.width(), line.startCluster(), line.endClusterExclusive(), visualLevels);
    }

    /// Returns the U+002D advance of the face that supplied `glyph`.
    private static int hyphenAdvance(SfntFont[] fonts, ShapedGlyph glyph) {
        int index = glyph.fontIndex();
        if (index < 0 || index >= fonts.length) {
            return 0;
        }
        SfntFont font = fonts[index];
        return font.metrics(font.glyphId('-')).advanceWidth();
    }

    /// Returns the U+002D glyph of the face that supplied `glyph`.
    private static int hyphenGlyph(SfntFont[] fonts, ShapedGlyph glyph) {
        int index = glyph.fontIndex();
        if (index < 0 || index >= fonts.length) {
            return 0;
        }
        return fonts[index].glyphId('-');
    }
}

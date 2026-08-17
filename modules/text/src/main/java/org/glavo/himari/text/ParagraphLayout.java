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
/// every non-last paragraph line. U+0009 advances to the next multiple of four space widths.
/// When ellipsis is requested, a paragraph that would wrap is truncated to the first line and
/// ends with U+2026. Each line is then reordered into visual order with
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
        return layout(new FontCollection(font), text, maxWidth, alignment, false);
    }

    /// Shapes `text` and wraps it, optionally truncating overflow with U+2026.
    ///
    /// @param font the font
    /// @param text the source text
    /// @param maxWidth the positive maximum line advance
    /// @param alignment leftover-width policy
    /// @param ellipsis whether overflow is truncated to one line ending in U+2026
    /// @return the wrapped lines, empty when `text` is empty
    public static @Unmodifiable List<LaidLine> layout(
            SfntFont font,
            String text,
            int maxWidth,
            LineAlignment alignment,
            boolean ellipsis
    ) {
        return layout(new FontCollection(font), text, maxWidth, alignment, ellipsis, 0);
    }

    /// Shapes `text` and wraps it with a first-line indent.
    ///
    /// @param font the font
    /// @param text the source text
    /// @param maxWidth the positive maximum line advance
    /// @param firstLineIndent extra indent applied to the first line of each paragraph
    /// @return the wrapped lines, empty when `text` is empty
    public static @Unmodifiable List<LaidLine> layout(
            SfntFont font,
            String text,
            int maxWidth,
            int firstLineIndent
    ) {
        return layout(new FontCollection(font), text, maxWidth, LineAlignment.START, false, firstLineIndent, 0);
    }

    /// Shapes `text` and wraps it with a first-line indent and a hanging indent on later lines.
    ///
    /// @param font the font
    /// @param text the source text
    /// @param maxWidth the positive maximum line advance
    /// @param firstLineIndent extra indent applied to the first line of each paragraph
    /// @param hangingIndent extra indent applied to every subsequent line
    /// @return the wrapped lines, empty when `text` is empty
    public static @Unmodifiable List<LaidLine> layout(
            SfntFont font,
            String text,
            int maxWidth,
            int firstLineIndent,
            int hangingIndent
    ) {
        return layout(
                new FontCollection(font),
                text,
                maxWidth,
                LineAlignment.START,
                false,
                firstLineIndent,
                hangingIndent,
                0
        );
    }

    /// Shapes `text` and wraps it with first-line, hanging, and last-line indents.
    ///
    /// @param font the font
    /// @param text the source text
    /// @param maxWidth the positive maximum line advance
    /// @param firstLineIndent extra indent applied to the first line of each paragraph
    /// @param hangingIndent extra indent applied to every subsequent line
    /// @param lastLineIndent extra indent applied to the last line of each paragraph
    /// @return the wrapped lines, empty when `text` is empty
    public static @Unmodifiable List<LaidLine> layout(
            SfntFont font,
            String text,
            int maxWidth,
            int firstLineIndent,
            int hangingIndent,
            int lastLineIndent
    ) {
        return layout(
                new FontCollection(font),
                text,
                maxWidth,
                LineAlignment.START,
                false,
                firstLineIndent,
                hangingIndent,
                lastLineIndent
        );
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
        return layout(fonts, text, maxWidth, alignment, false);
    }

    /// Shapes `text` through `fonts` and wraps it, optionally truncating overflow with U+2026.
    ///
    /// @param fonts the ordered faces
    /// @param text the source text
    /// @param maxWidth the positive maximum line advance
    /// @param alignment leftover-width policy
    /// @param ellipsis whether overflow is truncated to one line ending in U+2026
    /// @return the wrapped lines, empty when `text` is empty
    public static @Unmodifiable List<LaidLine> layout(
            FontCollection fonts,
            String text,
            int maxWidth,
            LineAlignment alignment,
            boolean ellipsis
    ) {
        return layout(fonts, text, maxWidth, alignment, ellipsis, 0);
    }

    /// Shapes `text` through `fonts` and wraps it, optionally truncating overflow with U+2026
    /// and indenting the first line of each paragraph.
    ///
    /// @param fonts the ordered faces
    /// @param text the source text
    /// @param maxWidth the positive maximum line advance
    /// @param alignment leftover-width policy
    /// @param ellipsis whether overflow is truncated to one line ending in U+2026
    /// @param firstLineIndent extra indent applied to the first line of each paragraph
    /// @return the wrapped lines, empty when `text` is empty
    public static @Unmodifiable List<LaidLine> layout(
            FontCollection fonts,
            String text,
            int maxWidth,
            LineAlignment alignment,
            boolean ellipsis,
            int firstLineIndent
    ) {
        return layout(fonts, text, maxWidth, alignment, ellipsis, firstLineIndent, 0);
    }

    /// Shapes `text` through `fonts` and wraps it, optionally truncating overflow with U+2026
    /// and indenting the first line plus subsequent hanging lines of each paragraph.
    ///
    /// @param fonts the ordered faces
    /// @param text the source text
    /// @param maxWidth the positive maximum line advance
    /// @param alignment leftover-width policy
    /// @param ellipsis whether overflow is truncated to one line ending in U+2026
    /// @param firstLineIndent extra indent applied to the first line of each paragraph
    /// @param hangingIndent extra indent applied to every subsequent line
    /// @return the wrapped lines, empty when `text` is empty
    public static @Unmodifiable List<LaidLine> layout(
            FontCollection fonts,
            String text,
            int maxWidth,
            LineAlignment alignment,
            boolean ellipsis,
            int firstLineIndent,
            int hangingIndent
    ) {
        return layout(fonts, text, maxWidth, alignment, ellipsis, firstLineIndent, hangingIndent, 0);
    }

    /// Shapes `text` through `fonts` and wraps it with first-line, hanging, and last-line indents.
    ///
    /// @param fonts the ordered faces
    /// @param text the source text
    /// @param maxWidth the positive maximum line advance
    /// @param alignment leftover-width policy
    /// @param ellipsis whether overflow is truncated to one line ending in U+2026
    /// @param firstLineIndent extra indent applied to the first line of each paragraph
    /// @param hangingIndent extra indent applied to every subsequent line
    /// @param lastLineIndent extra indent applied to the last line of each paragraph
    /// @return the wrapped lines, empty when `text` is empty
    public static @Unmodifiable List<LaidLine> layout(
            FontCollection fonts,
            String text,
            int maxWidth,
            LineAlignment alignment,
            boolean ellipsis,
            int firstLineIndent,
            int hangingIndent,
            int lastLineIndent
    ) {
        Objects.requireNonNull(fonts, "fonts");
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(alignment, "alignment");
        if (maxWidth <= 0) {
            throw new IllegalArgumentException("maxWidth must be positive");
        }
        if (firstLineIndent < 0) {
            throw new IllegalArgumentException("firstLineIndent must be non-negative");
        }
        if (hangingIndent < 0) {
            throw new IllegalArgumentException("hangingIndent must be non-negative");
        }
        if (lastLineIndent < 0) {
            throw new IllegalArgumentException("lastLineIndent must be non-negative");
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
            wrapParagraph(
                    fonts,
                    text.substring(start, end),
                    maxWidth,
                    clusterBase,
                    lines,
                    alignment,
                    ellipsis,
                    firstLineIndent,
                    hangingIndent,
                    lastLineIndent
            );
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
        wrapGlyphs(shaped.glyphs(), shaped.fonts(), maxWidth, clusterBase, lines, alignment, false, 0, 0, 0);
        return clusters;
    }

    /// Wraps one paragraph that contains no U+000A.
    private static void wrapParagraph(
            FontCollection fonts,
            String paragraph,
            int maxWidth,
            int clusterBase,
            List<LaidLine> lines,
            LineAlignment alignment,
            boolean ellipsis,
            int firstLineIndent,
            int hangingIndent,
            int lastLineIndent
    ) {
        SfntFont[] faces = new SfntFont[fonts.size()];
        for (int index = 0; index < faces.length; index++) {
            faces[index] = fonts.font(index);
        }
        wrapGlyphs(
                FallbackShaper.shape(fonts, paragraph),
                faces,
                maxWidth,
                clusterBase,
                lines,
                alignment,
                ellipsis,
                firstLineIndent,
                hangingIndent,
                lastLineIndent
        );
    }

    /// Wraps already-shaped paragraph glyphs, applying tabs, soft-hyphen breaks, leftover alignment,
    /// and optional first-line ellipsis.
    private static void wrapGlyphs(
            List<ShapedGlyph> glyphs,
            SfntFont[] fonts,
            int maxWidth,
            int clusterBase,
            List<LaidLine> lines,
            LineAlignment alignment,
            boolean ellipsis,
            int firstLineIndent,
            int hangingIndent,
            int lastLineIndent
    ) {
        int count = glyphs.size();
        int firstLine = lines.size();
        if (count == 0) {
            lines.add(new LaidLine(List.of(), 0, clusterBase, clusterBase).withIndent(firstLineIndent));
            return;
        }
        ArrayList<ShapedGlyph> laid = new ArrayList<>(glyphs);
        int tabStop = tabStop(fonts[0]);
        int lineStart = 0;
        int lastBreak = -1;
        boolean lastBreakHyphen = false;
        int width = 0;
        for (int index = 0; index < count; index++) {
            ShapedGlyph glyph = laid.get(index);
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
            int advance = glyph.codePoint() == 0x09 ? tabAdvance(width, tabStop) : glyph.xAdvance();
            if (glyph.codePoint() == 0x09 && advance != glyph.xAdvance()) {
                laid.set(index, withAdvance(glyph, advance));
                glyph = laid.get(index);
            }
            if (width + advance > maxWidth && index > lineStart) {
                int dictionary = lastBreak > lineStart
                        ? -1
                        : dictionaryBreak(laid, fonts, lineStart, index, maxWidth);
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
                lines.add(slice(laid, fonts, lineStart, end, clusterBase, hyphenate));
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
            lines.add(slice(laid, fonts, lineStart, count, clusterBase, false));
        }
        if (ellipsis && lines.size() - firstLine > 1) {
            LaidLine first = withEllipsis(lines.get(firstLine), fonts, maxWidth);
            while (lines.size() > firstLine + 1) {
                lines.remove(lines.size() - 1);
            }
            lines.set(firstLine, first);
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
        if (alignment == LineAlignment.CENTER || alignment == LineAlignment.END) {
            for (int index = firstLine; index < lines.size(); index++) {
                lines.set(index, alignLine(lines.get(index), maxWidth, alignment));
            }
        }
        if (firstLineIndent > 0 && firstLine < lines.size()) {
            LaidLine first = lines.get(firstLine);
            lines.set(firstLine, first.withIndent(first.indent() + firstLineIndent));
        }
        if (hangingIndent > 0) {
            for (int index = firstLine + 1; index < lines.size(); index++) {
                LaidLine line = lines.get(index);
                lines.set(index, line.withIndent(line.indent() + hangingIndent));
            }
        }
        if (lastLineIndent > 0 && firstLine < lines.size()) {
            LaidLine last = lines.get(lines.size() - 1);
            lines.set(lines.size() - 1, last.withIndent(last.indent() + lastLineIndent));
        }
    }

    /// Applies leftover-width indent for center or end alignment.
    private static LaidLine alignLine(LaidLine line, int maxWidth, LineAlignment alignment) {
        int extra = maxWidth - line.width();
        if (extra <= 0) {
            return line;
        }
        int indent = alignment == LineAlignment.END ? extra : extra / 2;
        return line.withIndent(indent);
    }

    /// Four space advances, or four units when the face has no space metric.
    private static int tabStop(SfntFont font) {
        int space = font.metrics(font.glyphId(' ')).advanceWidth();
        if (space <= 0) {
            space = Math.max(1, font.unitsPerEm() / 2);
        }
        return 4 * space;
    }

    /// Distance from `width` to the next tab stop.
    private static int tabAdvance(int width, int tabStop) {
        int used = width % tabStop;
        return used == 0 ? tabStop : tabStop - used;
    }

    /// Returns `glyph` with `advance` as its X-advance.
    private static ShapedGlyph withAdvance(ShapedGlyph glyph, int advance) {
        return new ShapedGlyph(
                glyph.codePoint(),
                glyph.glyphId(),
                glyph.cluster(),
                advance,
                glyph.xOffset(),
                glyph.yOffset(),
                glyph.fontIndex(),
                glyph.unsafeToBreak()
        );
    }

    /// Truncates `line` so U+2026 fits in `maxWidth`.
    private static LaidLine withEllipsis(LaidLine line, SfntFont[] fonts, int maxWidth) {
        ShapedGlyph sample = line.glyphs().isEmpty()
                ? new ShapedGlyph(0x2026, fonts[0].glyphId('.'), line.startCluster(), 0)
                : line.glyphs().getFirst();
        int ellipsisId = ellipsisGlyph(fonts, sample);
        int ellipsisAdvance = fonts[sample.fontIndex()].metrics(ellipsisId).advanceWidth();
        ArrayList<ShapedGlyph> kept = new ArrayList<>(line.glyphs());
        int width = line.width();
        while (!kept.isEmpty() && width + ellipsisAdvance > maxWidth) {
            ShapedGlyph last = kept.removeLast();
            width -= last.xAdvance();
        }
        int cluster = kept.isEmpty() ? line.startCluster() : kept.getLast().cluster() + 1;
        kept.add(new ShapedGlyph(0x2026, ellipsisId, cluster, ellipsisAdvance, 0, 0, sample.fontIndex()));
        width += ellipsisAdvance;
        return new LaidLine(kept, width, line.startCluster(), cluster + 1);
    }

    /// Returns a glyph that can stand in for U+2026 on `sample`'s face.
    private static int ellipsisGlyph(SfntFont[] fonts, ShapedGlyph sample) {
        SfntFont font = fonts[sample.fontIndex()];
        int ellipsis = font.glyphId(0x2026);
        if (ellipsis > 0) {
            return ellipsis;
        }
        int period = font.glyphId('.');
        return period > 0 ? period : font.glyphId('A');
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

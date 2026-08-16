package org.glavo.himari.text;

import org.glavo.himari.font.SfntFont;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/// Wraps [`DefaultShaper`] output into lines at a maximum advance.
///
/// Hard breaks follow U+000A. Soft breaks occur after U+0020 when the next glyph would exceed
/// `maxWidth`. A run without a space that exceeds `maxWidth` breaks before the overflowing glyph,
/// keeping at least one glyph per line. Leading spaces after a wrap are not skipped when they were
/// the break opportunity; they stay on the preceding line. The layout does not justify, hyphenate,
/// or reorder RTL runs.
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
        Objects.requireNonNull(font, "font");
        Objects.requireNonNull(text, "text");
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
            wrapParagraph(font, text.substring(start, end), maxWidth, clusterBase, lines);
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

    /// Wraps one paragraph that contains no U+000A.
    private static void wrapParagraph(
            SfntFont font,
            String paragraph,
            int maxWidth,
            int clusterBase,
            List<LaidLine> lines
    ) {
        List<ShapedGlyph> glyphs = DefaultShaper.shape(font, paragraph);
        int count = glyphs.size();
        if (count == 0) {
            lines.add(new LaidLine(List.of(), 0, clusterBase, clusterBase));
            return;
        }
        int lineStart = 0;
        int lastBreak = -1;
        int width = 0;
        for (int index = 0; index < count; index++) {
            ShapedGlyph glyph = glyphs.get(index);
            int advance = glyph.xAdvance();
            if (width + advance > maxWidth && index > lineStart) {
                int end = lastBreak > lineStart ? lastBreak : index;
                lines.add(slice(glyphs, lineStart, end, clusterBase));
                lineStart = end;
                lastBreak = -1;
                width = 0;
                index = lineStart - 1;
                continue;
            }
            width += advance;
            if (glyph.codePoint() == 0x20) {
                lastBreak = index + 1;
            }
        }
        if (lineStart < count) {
            lines.add(slice(glyphs, lineStart, count, clusterBase));
        }
    }

    /// Copies `glyphs[from, to)` and shifts clusters by `clusterBase`.
    private static LaidLine slice(List<ShapedGlyph> glyphs, int from, int to, int clusterBase) {
        ArrayList<ShapedGlyph> line = new ArrayList<>(to - from);
        int width = 0;
        int startCluster = clusterBase;
        int endCluster = clusterBase;
        for (int index = from; index < to; index++) {
            ShapedGlyph glyph = glyphs.get(index);
            int cluster = glyph.cluster() + clusterBase;
            if (index == from) {
                startCluster = cluster;
            }
            endCluster = cluster + 1;
            width += glyph.xAdvance();
            if (clusterBase == 0) {
                line.add(glyph);
            } else {
                line.add(new ShapedGlyph(glyph.codePoint(), glyph.glyphId(), cluster, glyph.xAdvance()));
            }
        }
        return new LaidLine(line, width, startCluster, endCluster);
    }
}

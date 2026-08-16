package org.glavo.himari.text;

import org.glavo.himari.font.SfntFont;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/// Resolves application fonts in listed order and segments text into fallback runs.
///
/// The first face is the primary font. Each code point selects the first listed face whose `cmap`
/// maps it to a nonzero glyph. Combining marks, join controls, spaces, and punctuation stay on the
/// preceding run when that face covers them so a mark does not split from its base and a space does
/// not start a new face. A code point that no face covers stays on the primary face as `.notdef`;
/// the resolver does not retry a face already considered for that code point.
///
/// Layout units after [`FallbackShaper`] are the primary face's units per em. This collection does
/// not enumerate system fonts.
@NotNullByDefault
public final class FontCollection {
    /// Ordered faces; index `0` is primary.
    private final SfntFont[] fonts;

    /// Creates a collection whose first face is `primary`.
    ///
    /// @param primary the first face tried for every code point
    /// @param fallbacks later faces, tried in order
    public FontCollection(SfntFont primary, SfntFont... fallbacks) {
        Objects.requireNonNull(primary, "primary");
        Objects.requireNonNull(fallbacks, "fallbacks");
        SfntFont[] faces = new SfntFont[1 + fallbacks.length];
        faces[0] = primary;
        for (int index = 0; index < fallbacks.length; index++) {
            faces[index + 1] = Objects.requireNonNull(fallbacks[index], "fallback");
        }
        this.fonts = faces;
    }

    /// Returns the primary face.
    ///
    /// @return the first listed font
    public SfntFont primary() {
        return fonts[0];
    }

    /// Returns the number of listed faces.
    ///
    /// @return a positive count
    public int size() {
        return fonts.length;
    }

    /// Returns the face at `index`.
    ///
    /// @param index a value in `[0, size())`
    /// @return the face
    public SfntFont font(int index) {
        if (index < 0 || index >= fonts.length) {
            throw new IllegalArgumentException("Unknown font index " + index);
        }
        return fonts[index];
    }

    /// Returns the first face that covers `codePoint`.
    ///
    /// @param codePoint the code point
    /// @return the face, or `null` when every listed face maps it to `.notdef`
    public @Nullable SfntFont covering(int codePoint) {
        int index = coveringIndex(codePoint);
        if (index < 0) {
            return null;
        }
        return fonts[index];
    }

    /// Returns the index of the first face that covers `codePoint`.
    ///
    /// @param codePoint the code point
    /// @return the index, or `-1` when uncovered
    public int coveringIndex(int codePoint) {
        for (int index = 0; index < fonts.length; index++) {
            if (fonts[index].hasGlyph(codePoint)) {
                return index;
            }
        }
        return -1;
    }

    /// Segments `text` into maximal runs that share one resolved face.
    ///
    /// @param text the source string
    /// @return the runs in logical order; empty when `text` is empty
    public @Unmodifiable List<FontRun> segment(String text) {
        Objects.requireNonNull(text, "text");
        int utf16Length = text.length();
        if (utf16Length == 0) {
            return List.of();
        }
        ArrayList<FontRun> runs = new ArrayList<>();
        int runStartUtf16 = 0;
        int runStartCluster = 0;
        int runFontIndex = -1;
        boolean runMissing = false;
        int cluster = 0;
        for (int index = 0; index < utf16Length; ) {
            int codePoint = text.codePointAt(index);
            int selected = resolve(codePoint, runFontIndex);
            boolean missing = !fonts[selected].hasGlyph(codePoint);
            if (runFontIndex < 0) {
                runFontIndex = selected;
                runMissing = missing;
            } else if (selected != runFontIndex) {
                runs.add(new FontRun(
                        runStartCluster,
                        cluster,
                        runStartUtf16,
                        index,
                        fonts[runFontIndex],
                        runFontIndex,
                        runMissing
                ));
                runStartUtf16 = index;
                runStartCluster = cluster;
                runFontIndex = selected;
                runMissing = missing;
            } else if (missing) {
                runMissing = true;
            }
            index += Character.charCount(codePoint);
            cluster++;
        }
        runs.add(new FontRun(
                runStartCluster,
                cluster,
                runStartUtf16,
                utf16Length,
                fonts[runFontIndex],
                runFontIndex,
                runMissing
        ));
        return List.copyOf(runs);
    }

    /// Selects the face for `codePoint` given the open run's font.
    ///
    /// @param codePoint the code point
    /// @param previousFontIndex the open run, or `-1` at the start of the string
    /// @return a font index in this collection
    private int resolve(int codePoint, int previousFontIndex) {
        if (previousFontIndex >= 0 && isSticky(codePoint) && fonts[previousFontIndex].hasGlyph(codePoint)) {
            return previousFontIndex;
        }
        int covered = coveringIndex(codePoint);
        if (covered < 0) {
            return 0;
        }
        return covered;
    }

    /// Returns whether `codePoint` should stay on the preceding face when that face covers it.
    ///
    /// @param codePoint the code point
    /// @return whether the character is a mark, joiner, space, or punctuation
    static boolean isSticky(int codePoint) {
        if (codePoint == 0x200C || codePoint == 0x200D || ArabicJoining.isTransparent(codePoint)) {
            return true;
        }
        int type = Character.getType(codePoint);
        return type == Character.NON_SPACING_MARK
                || type == Character.ENCLOSING_MARK
                || type == Character.COMBINING_SPACING_MARK
                || type == Character.SPACE_SEPARATOR
                || type == Character.LINE_SEPARATOR
                || type == Character.PARAGRAPH_SEPARATOR
                || type == Character.DASH_PUNCTUATION
                || type == Character.START_PUNCTUATION
                || type == Character.END_PUNCTUATION
                || type == Character.CONNECTOR_PUNCTUATION
                || type == Character.OTHER_PUNCTUATION
                || type == Character.INITIAL_QUOTE_PUNCTUATION
                || type == Character.FINAL_QUOTE_PUNCTUATION;
    }
}

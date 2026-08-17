package org.glavo.himari.controls;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Expands UTF-16 offsets to word and line ranges for first-stable editors.
@NotNullByDefault
public final class EditorSelection {
    /// Prevents instantiation.
    private EditorSelection() {
    }

    /// Returns the inclusive/exclusive word range covering `offset`.
    ///
    /// A word is a maximal run of letters, digits, or combining marks. When `offset` sits on a
    /// non-word character, the range is that single UTF-16 unit, or empty at the end of `text`.
    ///
    /// @param text the displayed text
    /// @param offset a UTF-16 offset in `[0, text.length()]`
    /// @return `{start, end}`
    public static int[] wordRange(String text, int offset) {
        Objects.requireNonNull(text, "text");
        if (offset < 0 || offset > text.length()) {
            throw new IllegalArgumentException("offset must lie within text");
        }
        if (text.isEmpty()) {
            return new int[] {0, 0};
        }
        int index = offset == text.length() ? offset - 1 : offset;
        if (!wordChar(text, index)) {
            return offset == text.length() ? new int[] {offset, offset} : new int[] {index, index + 1};
        }
        int start = index;
        while (start > 0 && wordChar(text, start - 1)) {
            start--;
        }
        int end = index + 1;
        while (end < text.length() && wordChar(text, end)) {
            end++;
        }
        return new int[] {start, end};
    }

    /// Returns the inclusive/exclusive line range covering `offset`.
    ///
    /// Lines are separated by `U+000A`. The terminating newline is excluded.
    ///
    /// @param text the displayed text
    /// @param offset a UTF-16 offset in `[0, text.length()]`
    /// @return `{start, end}`
    public static int[] lineRange(String text, int offset) {
        Objects.requireNonNull(text, "text");
        if (offset < 0 || offset > text.length()) {
            throw new IllegalArgumentException("offset must lie within text");
        }
        int start = text.lastIndexOf('\n', Math.max(0, offset - 1));
        start = start < 0 ? 0 : start + 1;
        int end = text.indexOf('\n', offset);
        return new int[] {start, end < 0 ? text.length() : end};
    }

    /// Returns whether the UTF-16 unit at `index` is a word character.
    private static boolean wordChar(String text, int index) {
        int type = Character.getType(text.charAt(index));
        return Character.isLetterOrDigit(text.charAt(index))
                || type == Character.NON_SPACING_MARK
                || type == Character.COMBINING_SPACING_MARK
                || type == Character.ENCLOSING_MARK;
    }
}

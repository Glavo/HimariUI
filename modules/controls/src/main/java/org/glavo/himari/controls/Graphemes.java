package org.glavo.himari.controls;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Walks UTF-16 offsets by extended grapheme-like clusters.
///
/// A cluster is one base scalar value plus any immediately following combining marks.
/// Surrogate pairs count as one base. This is the first-stable editor motion unit from
/// section 16.3, not a full UAX #29 implementation.
@NotNullByDefault
public final class Graphemes {
    /// Prevents instantiation.
    private Graphemes() {
    }

    /// Returns the offset after the cluster that starts at `offset`.
    ///
    /// @param text the displayed text
    /// @param offset a UTF-16 offset in `[0, text.length()]`
    /// @return the next offset, not greater than `text.length()`
    public static int next(String text, int offset) {
        Objects.requireNonNull(text, "text");
        if (offset < 0 || offset > text.length()) {
            throw new IllegalArgumentException("offset must lie within text");
        }
        if (offset >= text.length()) {
            return offset;
        }
        int index = offset + Character.charCount(text.codePointAt(offset));
        while (index < text.length() && combining(text.codePointAt(index))) {
            index += Character.charCount(text.codePointAt(index));
        }
        return index;
    }

    /// Returns the offset of the cluster that ends at `offset`.
    ///
    /// @param text the displayed text
    /// @param offset a UTF-16 offset in `[0, text.length()]`
    /// @return the previous offset, not less than `0`
    public static int previous(String text, int offset) {
        Objects.requireNonNull(text, "text");
        if (offset < 0 || offset > text.length()) {
            throw new IllegalArgumentException("offset must lie within text");
        }
        if (offset <= 0) {
            return 0;
        }
        int index = offset;
        while (index > 0) {
            int codePoint = text.codePointBefore(index);
            int width = Character.charCount(codePoint);
            index -= width;
            if (!combining(codePoint)) {
                return index;
            }
        }
        return 0;
    }

    /// Returns whether `codePoint` is a combining mark.
    private static boolean combining(int codePoint) {
        int type = Character.getType(codePoint);
        return type == Character.NON_SPACING_MARK
                || type == Character.COMBINING_SPACING_MARK
                || type == Character.ENCLOSING_MARK;
    }
}

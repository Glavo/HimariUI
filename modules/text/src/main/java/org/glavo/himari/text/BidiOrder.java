package org.glavo.himari.text;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Objects;

/// Assigns first-stable paragraph-LTR embedding levels and produces visual order.
///
/// Neutrals take the surrounding strong direction when both sides agree (UAX #9 N1). Otherwise
/// they take the paragraph-LTR embedding direction (N2). This is a bounded UAX #9 subset used
/// until the complete `TEXT-BIDI-001` resolver lands; it does not implement isolates or explicit
/// embeddings.
///
/// Resolution is one forward and one backward linear pass. [`#visual(String)`] returns `logical`
/// itself when the paragraph contains no strong RTL letters.
@NotNullByDefault
public final class BidiOrder {
    /// Left-to-right.
    public static final int LTR = 0;

    /// Right-to-left.
    public static final int RTL = 1;

    /// Shared empty level array.
    private static final int[] EMPTY_LEVELS = new int[0];

    /// Prevents instantiation.
    private BidiOrder() {
    }

    /// Returns the resolved embedding level for one code point in a paragraph-LTR context.
    ///
    /// @param codePoint the Unicode code point
    /// @return `0` for LTR or isolated-neutral-as-LTR, `1` for RTL
    public static int level(int codePoint) {
        return isRtl(codePoint) ? RTL : LTR;
    }

    /// Reorders `logical` into visual order under a paragraph-LTR base direction.
    ///
    /// @param logical the logical string
    /// @return the visual string, or `logical` when no RTL letters are present
    public static String visual(String logical) {
        Objects.requireNonNull(logical, "logical");
        int utf16Length = logical.length();
        if (utf16Length == 0 || !containsRtl(logical, utf16Length)) {
            return logical;
        }
        int[] points = new int[utf16Length];
        int count = decode(logical, utf16Length, points);
        int[] levels = new int[count];
        resolve(points, count, levels);
        reorder(points, levels, count);
        return new String(points, 0, count);
    }

    /// Returns the resolved levels for each code point.
    ///
    /// @param logical the logical string
    /// @return one level per code point; the empty string yields a shared empty array
    public static int @Unmodifiable [] levels(String logical) {
        Objects.requireNonNull(logical, "logical");
        int utf16Length = logical.length();
        if (utf16Length == 0) {
            return EMPTY_LEVELS;
        }
        if (!containsRtl(logical, utf16Length)) {
            return new int[logical.codePointCount(0, utf16Length)];
        }
        int[] points = new int[utf16Length];
        int count = decode(logical, utf16Length, points);
        int[] levels = new int[count];
        resolve(points, count, levels);
        return levels;
    }

    /// Returns whether `text` contains a strong RTL letter from the first-stable subset.
    ///
    /// The subset's RTL ranges are all BMP, so unpaired surrogates cannot match.
    ///
    /// @param text the text
    /// @param utf16Length `text.length()`
    /// @return whether a strong RTL letter is present
    private static boolean containsRtl(String text, int utf16Length) {
        for (int index = 0; index < utf16Length; index++) {
            if (isRtl(text.charAt(index))) {
                return true;
            }
        }
        return false;
    }

    /// Writes UTF-32 code points into `points` and returns the count.
    ///
    /// @param logical the logical string
    /// @param utf16Length `logical.length()`
    /// @param points a buffer of length at least `utf16Length`
    /// @return the code-point count
    private static int decode(String logical, int utf16Length, int[] points) {
        int count = 0;
        for (int index = 0; index < utf16Length; ) {
            int codePoint = logical.codePointAt(index);
            points[count++] = codePoint;
            index += Character.charCount(codePoint);
        }
        return count;
    }

    /// Resolves paragraph-LTR levels for `count` code points into `levels`.
    ///
    /// The first pass writes each strong type and tentatively assigns neutrals the left strong
    /// type. The second pass keeps that assignment when it matches the right strong type (N1)
    /// and otherwise writes the paragraph embedding level (N2).
    ///
    /// @param points the decoded code points
    /// @param count the code-point count
    /// @param levels the destination, length at least `count`
    private static void resolve(int[] points, int count, int[] levels) {
        int lastStrong = LTR;
        for (int index = 0; index < count; index++) {
            int codePoint = points[index];
            if (isRtl(codePoint)) {
                lastStrong = RTL;
                levels[index] = RTL;
            } else if (isLtr(codePoint)) {
                lastStrong = LTR;
                levels[index] = LTR;
            } else {
                levels[index] = lastStrong;
            }
        }
        int nextStrong = LTR;
        for (int index = count - 1; index >= 0; index--) {
            int codePoint = points[index];
            if (isRtl(codePoint)) {
                nextStrong = RTL;
            } else if (isLtr(codePoint)) {
                nextStrong = LTR;
            } else if (levels[index] != nextStrong) {
                levels[index] = LTR;
            }
        }
    }

    /// Reverses each contiguous RTL run in the leading `count` code points.
    ///
    /// @param points the logical code points, overwritten with visual order
    /// @param levels the resolved levels
    /// @param count the code-point count
    private static void reorder(int[] points, int[] levels, int count) {
        int index = 0;
        while (index < count) {
            if (levels[index] != RTL) {
                index++;
                continue;
            }
            int end = index + 1;
            while (end < count && levels[end] == RTL) {
                end++;
            }
            reverse(points, index, end);
            index = end;
        }
    }

    /// Reverses `[start, end)`.
    ///
    /// @param values the array
    /// @param start the inclusive start
    /// @param end the exclusive end
    private static void reverse(int[] values, int start, int end) {
        for (int left = start, right = end - 1; left < right; left++, right--) {
            int swap = values[left];
            values[left] = values[right];
            values[right] = swap;
        }
    }

    /// Returns whether the code point is a strong RTL letter in the first-stable subset.
    ///
    /// @param codePoint the code point
    /// @return whether the code point is RTL
    private static boolean isRtl(int codePoint) {
        return (codePoint >= 0x0590 && codePoint <= 0x08FF)
                || (codePoint >= 0xFB1D && codePoint <= 0xFDFF)
                || (codePoint >= 0xFE70 && codePoint <= 0xFEFF);
    }

    /// Returns whether the code point is a strong LTR letter or digit.
    ///
    /// @param codePoint the code point
    /// @return whether the code point is LTR
    private static boolean isLtr(int codePoint) {
        if (codePoint <= 0x7F) {
            return (codePoint >= 'A' && codePoint <= 'Z')
                    || (codePoint >= 'a' && codePoint <= 'z')
                    || (codePoint >= '0' && codePoint <= '9');
        }
        return Character.isLetterOrDigit(codePoint) && !isRtl(codePoint);
    }
}

package org.glavo.himari.text;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Objects;

/// Assigns first-stable paragraph-LTR embedding levels and produces visual order.
///
/// Neutrals take the surrounding strong direction when both sides agree (UAX #9 N1). Otherwise
/// they take the current embedding direction (N2): paragraph LTR, or the direction of an open
/// isolate or embedding. Supported controls are LRI/RLI/FSI/PDI and LRE/RLE/PDF. Controls are
/// omitted from [`#visual(String)`]. Isolates and embeddings do not implement the full X6a
/// overflow stack; nesting deeper than 15 is ignored.
@NotNullByDefault
public final class BidiOrder {
    /// Left-to-right.
    public static final int LTR = 0;

    /// Right-to-left.
    public static final int RTL = 1;

    /// Shared empty level array.
    private static final int[] EMPTY_LEVELS = new int[0];

    /// LRI.
    private static final int LRI = 0x2066;

    /// RLI.
    private static final int RLI = 0x2067;

    /// FSI.
    private static final int FSI = 0x2068;

    /// PDI.
    private static final int PDI = 0x2069;

    /// LRE.
    private static final int LRE = 0x202A;

    /// RLE.
    private static final int RLE = 0x202B;

    /// PDF.
    private static final int PDF = 0x202C;

    /// Maximum isolate/embedding depth, inclusive of the paragraph.
    private static final int MAX_EMBED = 16;

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
        if (utf16Length == 0) {
            return logical;
        }
        boolean controls = containsControls(logical, utf16Length);
        if (!controls && !containsRtl(logical, utf16Length)) {
            return logical;
        }
        int[] points = new int[utf16Length];
        int count = decode(logical, utf16Length, points);
        int[] levels = new int[count];
        resolve(points, count, levels);
        int kept = stripControls(points, levels, count);
        reorder(points, levels, kept);
        if (kept == count && !controls) {
            return new String(points, 0, kept);
        }
        return new String(points, 0, kept);
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
        if (!containsControls(logical, utf16Length) && !containsRtl(logical, utf16Length)) {
            return new int[logical.codePointCount(0, utf16Length)];
        }
        int[] points = new int[utf16Length];
        int count = decode(logical, utf16Length, points);
        int[] levels = new int[count];
        resolve(points, count, levels);
        return levels;
    }

    /// Returns resolved paragraph-LTR levels for decoded code points.
    ///
    /// @param points logical code points
    /// @return one level per element
    public static int @Unmodifiable [] levels(int[] points) {
        Objects.requireNonNull(points, "points");
        if (points.length == 0) {
            return EMPTY_LEVELS;
        }
        int[] levels = new int[points.length];
        resolve(points, points.length, levels);
        return levels;
    }

    /// Reverses each contiguous RTL run of `items` in place.
    ///
    /// @param items values in logical order, overwritten with visual order
    /// @param levels resolved levels, one per item
    public static void reorderRtlRuns(int[] items, int[] levels) {
        Objects.requireNonNull(items, "items");
        Objects.requireNonNull(levels, "levels");
        int count = Math.min(items.length, levels.length);
        reorder(items, levels, count);
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

    /// Resolves embedding levels for `count` code points into `levels`.
    ///
    /// Isolate and embedding controls push or pop the current embedding direction. The first
    /// pass writes each strong type and tentatively assigns neutrals the left strong type inside
    /// the same embedding. The second pass keeps that assignment when it matches the right
    /// strong type (N1) and otherwise writes the current embedding direction (N2).
    ///
    /// @param points the decoded code points
    /// @param count the code-point count
    /// @param levels the destination, length at least `count`
    private static void resolve(int[] points, int count, int[] levels) {
        int[] embed = new int[count];
        int[] stack = new int[MAX_EMBED];
        stack[0] = LTR;
        int depth = 0;
        for (int index = 0; index < count; index++) {
            int codePoint = points[index];
            if (isIsolateOrEmbed(codePoint)) {
                embed[index] = stack[depth];
                int pushed = pushDirection(points, count, index, codePoint);
                if (pushed >= 0 && depth + 1 < MAX_EMBED) {
                    stack[++depth] = pushed;
                }
                continue;
            }
            if (isPop(codePoint)) {
                if (depth > 0) {
                    depth--;
                }
                embed[index] = stack[depth];
                continue;
            }
            embed[index] = stack[depth];
        }
        int lastStrong = embed[0];
        int lastEmbed = embed[0];
        for (int index = 0; index < count; index++) {
            int codePoint = points[index];
            if (embed[index] != lastEmbed) {
                lastStrong = embed[index];
                lastEmbed = embed[index];
            }
            if (isControl(codePoint)) {
                levels[index] = embed[index];
            } else if (isRtl(codePoint)) {
                lastStrong = RTL;
                levels[index] = RTL;
            } else if (isLtr(codePoint)) {
                lastStrong = LTR;
                levels[index] = embed[index] == RTL ? 2 : LTR;
            } else {
                levels[index] = lastStrong;
            }
        }
        int nextStrong = embed[count - 1];
        int nextEmbed = embed[count - 1];
        for (int index = count - 1; index >= 0; index--) {
            int codePoint = points[index];
            if (embed[index] != nextEmbed) {
                nextStrong = embed[index];
                nextEmbed = embed[index];
            }
            if (isRtl(codePoint)) {
                nextStrong = RTL;
            } else if (isLtr(codePoint)) {
                nextStrong = LTR;
            } else if (!isControl(codePoint) && levels[index] != nextStrong) {
                levels[index] = embed[index];
            }
        }
    }

    /// Removes isolate and embedding controls, compacting `points` and `levels`.
    ///
    /// @return the remaining count
    private static int stripControls(int[] points, int[] levels, int count) {
        int written = 0;
        for (int index = 0; index < count; index++) {
            if (isControl(points[index])) {
                continue;
            }
            points[written] = points[index];
            levels[written] = levels[index];
            written++;
        }
        return written;
    }

    /// Returns the direction pushed by `codePoint`, or `-1` when it is not a push.
    private static int pushDirection(int[] points, int count, int index, int codePoint) {
        if (codePoint == RLI || codePoint == RLE) {
            return RTL;
        }
        if (codePoint == LRI || codePoint == LRE) {
            return LTR;
        }
        if (codePoint == FSI) {
            return firstStrong(points, count, index + 1);
        }
        return -1;
    }

    /// Returns the first strong direction after `start` before a matching PDI, or LTR.
    private static int firstStrong(int[] points, int count, int start) {
        int depth = 1;
        for (int index = start; index < count; index++) {
            int codePoint = points[index];
            if (codePoint == LRI || codePoint == RLI || codePoint == FSI || codePoint == LRE || codePoint == RLE) {
                depth++;
            } else if (codePoint == PDI || codePoint == PDF) {
                depth--;
                if (depth == 0) {
                    return LTR;
                }
            } else if (isRtl(codePoint)) {
                return RTL;
            } else if (isLtr(codePoint)) {
                return LTR;
            }
        }
        return LTR;
    }

    /// Returns whether `text` contains an isolate or embedding control.
    private static boolean containsControls(String text, int utf16Length) {
        for (int index = 0; index < utf16Length; index++) {
            if (isControl(text.charAt(index))) {
                return true;
            }
        }
        return false;
    }

    /// Returns whether `codePoint` is an isolate or embedding control.
    static boolean isControl(int codePoint) {
        return isIsolateOrEmbed(codePoint) || isPop(codePoint);
    }

    /// Returns whether `codePoint` opens an isolate or embedding.
    private static boolean isIsolateOrEmbed(int codePoint) {
        return codePoint == LRI || codePoint == RLI || codePoint == FSI || codePoint == LRE || codePoint == RLE;
    }

    /// Returns whether `codePoint` closes an isolate or embedding.
    private static boolean isPop(int codePoint) {
        return codePoint == PDI || codePoint == PDF;
    }

    /// Reverses each run at or above every level from the maximum down to 1 (UAX #9 L2).
    ///
    /// @param points the logical code points, overwritten with visual order
    /// @param levels the resolved levels
    /// @param count the code-point count
    private static void reorder(int[] points, int[] levels, int count) {
        int max = 0;
        for (int index = 0; index < count; index++) {
            if (levels[index] > max) {
                max = levels[index];
            }
        }
        for (int level = max; level >= 1; level--) {
            int index = 0;
            while (index < count) {
                if (levels[index] < level) {
                    index++;
                    continue;
                }
                int end = index + 1;
                while (end < count && levels[end] >= level) {
                    end++;
                }
                reverse(points, index, end);
                reverse(levels, index, end);
                index = end;
            }
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

package org.glavo.himari.text;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Objects;

/// Assigns first-stable paragraph-LTR embedding levels and produces visual order.
///
/// Neutrals inherit the surrounding strong direction. This is a bounded UAX #9 subset used until
/// the complete `TEXT-BIDI-001` resolver lands; it does not implement isolates or explicit embeddings.
@NotNullByDefault
public final class BidiOrder {
    /// Left-to-right.
    public static final int LTR = 0;

    /// Right-to-left.
    public static final int RTL = 1;

    /// Prevents instantiation.
    private BidiOrder() {
    }

    /// Returns the resolved embedding level for one code point in a paragraph-LTR context.
    ///
    /// @param codePoint the Unicode code point
    /// @return `0` for LTR or neutral-as-LTR, `1` for RTL
    public static int level(int codePoint) {
        return isRtl(codePoint) ? RTL : LTR;
    }

    /// Reorders `logical` into visual order under a paragraph-LTR base direction.
    ///
    /// @param logical the logical string
    /// @return the visual string
    public static String visual(String logical) {
        Objects.requireNonNull(logical, "logical");
        if (logical.isEmpty()) {
            return "";
        }
        int[] points = logical.codePoints().toArray();
        int[] levels = resolve(points);
        int[] visual = reorder(points, levels);
        return new String(visual, 0, visual.length);
    }

    /// Returns the resolved levels for each code point.
    ///
    /// @param logical the logical string
    /// @return one level per code point
    public static int @Unmodifiable [] levels(String logical) {
        Objects.requireNonNull(logical, "logical");
        return resolve(logical.codePoints().toArray());
    }

    /// Resolves levels for a paragraph-LTR sequence.
    private static int[] resolve(int[] points) {
        int[] levels = new int[points.length];
        int lastStrong = LTR;
        for (int index = 0; index < points.length; index++) {
            if (isRtl(points[index])) {
                lastStrong = RTL;
                levels[index] = RTL;
            } else if (isLtr(points[index])) {
                lastStrong = LTR;
                levels[index] = LTR;
            } else {
                levels[index] = lastStrong;
            }
        }
        int nextStrong = LTR;
        for (int index = points.length - 1; index >= 0; index--) {
            if (isRtl(points[index])) {
                nextStrong = RTL;
            } else if (isLtr(points[index])) {
                nextStrong = LTR;
            } else if (levels[index] != nextStrong && lastStrongOnLeft(points, index) == nextStrong) {
                levels[index] = nextStrong;
            }
        }
        return levels;
    }

    /// Finds the nearest strong direction on the left, or LTR.
    private static int lastStrongOnLeft(int[] points, int index) {
        for (int cursor = index - 1; cursor >= 0; cursor--) {
            if (isRtl(points[cursor])) {
                return RTL;
            }
            if (isLtr(points[cursor])) {
                return LTR;
            }
        }
        return LTR;
    }

    /// Reverses each contiguous odd-level run.
    private static int[] reorder(int[] points, int[] levels) {
        int[] visual = points.clone();
        int index = 0;
        while (index < visual.length) {
            if (levels[index] != RTL) {
                index++;
                continue;
            }
            int end = index + 1;
            while (end < visual.length && levels[end] == RTL) {
                end++;
            }
            reverse(visual, index, end);
            index = end;
        }
        return visual;
    }

    /// Reverses `[start, end)`.
    private static void reverse(int[] values, int start, int end) {
        for (int left = start, right = end - 1; left < right; left++, right--) {
            int swap = values[left];
            values[left] = values[right];
            values[right] = swap;
        }
    }

    /// Returns whether the code point is a strong RTL letter.
    private static boolean isRtl(int codePoint) {
        return (codePoint >= 0x0590 && codePoint <= 0x08FF)
                || (codePoint >= 0xFB1D && codePoint <= 0xFDFF)
                || (codePoint >= 0xFE70 && codePoint <= 0xFEFF);
    }

    /// Returns whether the code point is a strong LTR letter or digit.
    private static boolean isLtr(int codePoint) {
        return Character.isLetterOrDigit(codePoint) && !isRtl(codePoint);
    }
}

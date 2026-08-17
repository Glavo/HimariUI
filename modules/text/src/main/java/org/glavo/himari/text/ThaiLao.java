package org.glavo.himari.text;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

/// Applies first-stable Thai and Lao SARA AM decomposition and Nikhahit reordering.
///
/// `U+0E33` / `U+0EB3` become Nikhahit plus SARA AA. Nikhahit then moves left over the
/// above-base marks Uniscribe reorders, matching
/// `&lt;U+0E14, U+0E4B, U+0E33&gt;` → `&lt;U+0E14, U+0E4D, U+0E4B, U+0E32&gt;`.
/// Left vowels stay in Unicode visual order.
@NotNullByDefault
public final class ThaiLao {
    /// Prevents instantiation.
    private ThaiLao() {
    }

    /// Holds expanded code points and their source clusters.
    ///
    /// @param points the expanded code points
    /// @param clusters one source cluster per expanded code point
    /// @param count the used length of both arrays
    public record Expansion(int[] points, int[] clusters, int count) {
        /// Validates matching nonnegative lengths.
        public Expansion {
            if (count < 0 || count > points.length || count > clusters.length) {
                throw new IllegalArgumentException("Thai/Lao expansion count exceeds buffer length");
            }
        }
    }

    /// Returns whether `codePoint` is Thai or Lao SARA AM.
    ///
    /// @param codePoint the code point
    /// @return whether the code point is `U+0E33` or `U+0EB3`
    public static boolean isSaraAm(int codePoint) {
        return (codePoint & ~0x80) == 0x0E33;
    }

    /// Returns whether `codePoint` is a Thai or Lao above-base mark that Nikhahit skips.
    ///
    /// @param codePoint the code point
    /// @return whether Uniscribe treats the mark as above-base
    public static boolean isAboveMark(int codePoint) {
        int thai = codePoint & ~0x80;
        return thai == 0x0E31
                || thai == 0x0E3B
                || (thai >= 0x0E34 && thai <= 0x0E37)
                || (thai >= 0x0E47 && thai <= 0x0E4E);
    }

    /// Returns whether `codePoint` is in the Thai or Lao blocks.
    ///
    /// @param codePoint the code point
    /// @return whether the code point is `U+0E00`–`U+0EFF`
    public static boolean isThaiOrLao(int codePoint) {
        return codePoint >= 0x0E00 && codePoint <= 0x0EFF;
    }

    /// Returns whether `codePoint` is a Thai or Lao left vowel.
    ///
    /// @param codePoint the code point
    /// @return whether the code point is `U+0E40`–`U+0E44` or `U+0EC0`–`U+0EC4`
    public static boolean isLeftVowel(int codePoint) {
        int thai = codePoint & ~0x80;
        return thai >= 0x0E40 && thai <= 0x0E44;
    }

    /// Returns the Lao ho-no / ho-mo ligature for two code points, or `0` when none exists.
    ///
    /// @param first the first code point
    /// @param second the second code point
    /// @return `U+0EDC` or `U+0EDD`, or `0`
    public static int laoLigature(int first, int second) {
        if (first != 0x0EAB) {
            return 0;
        }
        if (second == 0x0E99) {
            return 0x0EDC;
        }
        if (second == 0x0EA1) {
            return 0x0EDD;
        }
        return 0;
    }

    /// Expands SARA AM and reorders Nikhahit, or returns `null` when no SARA AM is present.
    ///
    /// @param points the decoded code points
    /// @param count the code-point count
    /// @return the expansion, or `null` when the buffer is unchanged
    public static @Nullable Expansion expand(int[] points, int count) {
        int extra = 0;
        for (int index = 0; index < count; index++) {
            if (isSaraAm(points[index])) {
                extra++;
            }
        }
        if (extra == 0) {
            return null;
        }
        int[] out = new int[count + extra];
        int[] clusters = new int[count + extra];
        int written = 0;
        int source = 0;
        for (int index = 0; index < count; index++) {
            int codePoint = points[index];
            if (!isSaraAm(codePoint)) {
                out[written] = codePoint;
                clusters[written] = source;
                written++;
                source++;
                continue;
            }
            int nikhahit = codePoint - 0x0E33 + 0x0E4D;
            int saraAa = codePoint - 1;
            out[written] = nikhahit;
            clusters[written] = source;
            out[written + 1] = saraAa;
            clusters[written + 1] = source;
            written += 2;
            int nikhahitAt = written - 2;
            int start = nikhahitAt;
            while (start > 0 && isAboveMark(out[start - 1])) {
                start--;
            }
            if (start < nikhahitAt) {
                int savedPoint = out[nikhahitAt];
                int savedCluster = clusters[nikhahitAt];
                System.arraycopy(out, start, out, start + 1, nikhahitAt - start);
                System.arraycopy(clusters, start, clusters, start + 1, nikhahitAt - start);
                out[start] = savedPoint;
                clusters[start] = savedCluster;
            }
            if (start > 0) {
                int previous = clusters[start - 1];
                for (int cursor = start; cursor < written; cursor++) {
                    clusters[cursor] = previous;
                }
            }
            source++;
        }
        return new Expansion(out, clusters, written);
    }
}

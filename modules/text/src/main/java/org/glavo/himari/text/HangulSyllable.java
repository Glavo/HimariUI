package org.glavo.himari.text;

import org.jetbrains.annotations.NotNullByDefault;

/// Composes first-stable Hangul jamo into precomposed syllables.
///
/// The mapping is the Unicode LV/LVT arithmetic: `U+AC00 + ((L * 21) + V) * 28 + T`.
/// [`#asLead(int)`], [`#asVowel(int)`], and [`#asTrail(int)`] map Hangul Compatibility Jamo
/// onto modern choseong, jungseong, and jongseong before [`#compose(int, int, int)`].
@NotNullByDefault
public final class HangulSyllable {
    /// First precomposed Hangul syllable.
    public static final int SYLLABLE_BASE = 0xAC00;

    /// First modern choseong.
    public static final int LEAD_BASE = 0x1100;

    /// First modern jungseong.
    public static final int VOWEL_BASE = 0x1161;

    /// First modern jongseong.
    public static final int TRAIL_BASE = 0x11A8;

    /// Number of modern leading consonants.
    public static final int LEAD_COUNT = 19;

    /// Number of modern vowels.
    public static final int VOWEL_COUNT = 21;

    /// Trailing-consonant slots, including the empty trailer.
    public static final int TRAIL_COUNT = 28;

    /// Prevents instantiation.
    private HangulSyllable() {
    }

    /// Returns whether `codePoint` is a modern choseong.
    ///
    /// @param codePoint the code point
    /// @return whether the code point is `U+1100`–`U+1112`
    public static boolean isLead(int codePoint) {
        return codePoint >= LEAD_BASE && codePoint < LEAD_BASE + LEAD_COUNT;
    }

    /// Returns whether `codePoint` is a modern jungseong.
    ///
    /// @param codePoint the code point
    /// @return whether the code point is `U+1161`–`U+1175`
    public static boolean isVowel(int codePoint) {
        return codePoint >= VOWEL_BASE && codePoint < VOWEL_BASE + VOWEL_COUNT;
    }

    /// Returns whether `codePoint` is a modern jongseong.
    ///
    /// @param codePoint the code point
    /// @return whether the code point is `U+11A8`–`U+11C2`
    public static boolean isTrail(int codePoint) {
        return codePoint >= TRAIL_BASE && codePoint < TRAIL_BASE + TRAIL_COUNT - 1;
    }

    /// Choseong identities for `U+3131`–`U+314E`. Zero means the compatibility jamo is a cluster.
    private static final int[] COMPAT_LEAD = {
            0x1100, 0x1101, 0, 0x1102, 0, 0, 0x1103, 0x1104, 0x1105,
            0, 0, 0, 0, 0, 0, 0, 0x1106, 0x1107, 0x1108, 0, 0x1109, 0x110A,
            0x110B, 0x110C, 0x110D, 0x110E, 0x110F, 0x1110, 0x1111, 0x1112
    };

    /// Jongseong identities for `U+3131`–`U+314E`. Zero means the compatibility jamo is not a trailer.
    private static final int[] COMPAT_TRAIL = {
            0x11A8, 0x11A9, 0x11AA, 0x11AB, 0x11AC, 0x11AD, 0x11AE, 0, 0x11AF,
            0x11B0, 0x11B1, 0x11B2, 0x11B3, 0x11B4, 0x11B5, 0x11B6, 0x11B7, 0x11B8, 0, 0x11B9,
            0x11BA, 0x11BB, 0x11BC, 0x11BD, 0, 0x11BE, 0x11BF, 0x11C0, 0x11C1, 0x11C2
    };

    /// Returns whether `codePoint` is a modern combining jamo or a compatibility jamo.
    ///
    /// @param codePoint the code point
    /// @return whether Hangul composition may consume the code point
    public static boolean isJamo(int codePoint) {
        return isLead(codePoint) || isVowel(codePoint) || isTrail(codePoint) || isCompatibility(codePoint);
    }

    /// Returns whether `codePoint` is a Hangul Compatibility Jamo letter used by first-stable composition.
    ///
    /// @param codePoint the code point
    /// @return whether the code point is `U+3131`–`U+3163`
    public static boolean isCompatibility(int codePoint) {
        return codePoint >= 0x3131 && codePoint <= 0x3163;
    }

    /// Maps a source code point onto a modern choseong, or `0` when it is not a lead.
    ///
    /// @param codePoint the source code point
    /// @return the modern choseong, or `0`
    public static int asLead(int codePoint) {
        if (isLead(codePoint)) {
            return codePoint;
        }
        if (codePoint >= 0x3131 && codePoint <= 0x314E) {
            return COMPAT_LEAD[codePoint - 0x3131];
        }
        return 0;
    }

    /// Maps a source code point onto a modern jungseong, or `0` when it is not a vowel.
    ///
    /// @param codePoint the source code point
    /// @return the modern jungseong, or `0`
    public static int asVowel(int codePoint) {
        if (isVowel(codePoint)) {
            return codePoint;
        }
        if (codePoint >= 0x314F && codePoint <= 0x3163) {
            return VOWEL_BASE + (codePoint - 0x314F);
        }
        return 0;
    }

    /// Maps a source code point onto a modern jongseong, or `0` when it is not a trailer.
    ///
    /// @param codePoint the source code point
    /// @return the modern jongseong, or `0`
    public static int asTrail(int codePoint) {
        if (isTrail(codePoint)) {
            return codePoint;
        }
        if (codePoint >= 0x3131 && codePoint <= 0x314E) {
            return COMPAT_TRAIL[codePoint - 0x3131];
        }
        return 0;
    }

    /// Composes a lead and vowel, optionally with a trailing consonant.
    ///
    /// @param lead the choseong
    /// @param vowel the jungseong
    /// @param trail the jongseong, or `0` when the syllable has no trailer
    /// @return the syllable code point, or `0` when the triple is not a modern Hangul syllable
    public static int compose(int lead, int vowel, int trail) {
        if (!isLead(lead) || !isVowel(vowel)) {
            return 0;
        }
        int trailer = 0;
        if (trail != 0) {
            if (!isTrail(trail)) {
                return 0;
            }
            trailer = trail - TRAIL_BASE + 1;
        }
        return SYLLABLE_BASE + ((lead - LEAD_BASE) * VOWEL_COUNT + (vowel - VOWEL_BASE)) * TRAIL_COUNT + trailer;
    }
}

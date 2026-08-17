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

    /// Halfwidth Hangul vowel code points in `U+314F`–`U+3163` order.
    ///
    /// The halfwidth block skips `U+FFC0`/`U+FFC1`, `U+FFC8`/`U+FFC9`, `U+FFD0`/`U+FFD1`,
    /// and `U+FFD8`/`U+FFD9`.
    private static final int[] HALFWIDTH_VOWELS = {
            0xFFC2, 0xFFC3, 0xFFC4, 0xFFC5, 0xFFC6, 0xFFC7,
            0xFFCA, 0xFFCB, 0xFFCC, 0xFFCD, 0xFFCE, 0xFFCF,
            0xFFD2, 0xFFD3, 0xFFD4, 0xFFD5, 0xFFD6, 0xFFD7,
            0xFFDA, 0xFFDB, 0xFFDC
    };

    /// Returns whether `codePoint` is a Hangul Compatibility or halfwidth jamo letter.
    ///
    /// @param codePoint the code point
    /// @return whether the code point is `U+3131`–`U+3163` or `U+FFA1`–`U+FFDC`
    public static boolean isCompatibility(int codePoint) {
        return (codePoint >= 0x3131 && codePoint <= 0x3163) || halfwidthCompat(codePoint) != 0;
    }

    /// Maps a halfwidth Hangul letter onto its compatibility-jamo identity, or `0`.
    private static int halfwidthCompat(int codePoint) {
        if (codePoint >= 0xFFA1 && codePoint <= 0xFFBE) {
            return 0x3131 + (codePoint - 0xFFA1);
        }
        for (int index = 0; index < HALFWIDTH_VOWELS.length; index++) {
            if (HALFWIDTH_VOWELS[index] == codePoint) {
                return 0x314F + index;
            }
        }
        return 0;
    }

    /// Maps a source code point onto a modern choseong, or `0` when it is not a lead.
    ///
    /// @param codePoint the source code point
    /// @return the modern choseong, or `0`
    public static int asLead(int codePoint) {
        int mapped = halfwidthCompat(codePoint);
        int source = mapped == 0 ? codePoint : mapped;
        if (isLead(source)) {
            return source;
        }
        if (source >= 0x3131 && source <= 0x314E) {
            return COMPAT_LEAD[source - 0x3131];
        }
        return 0;
    }

    /// Maps a source code point onto a modern jungseong, or `0` when it is not a vowel.
    ///
    /// @param codePoint the source code point
    /// @return the modern jungseong, or `0`
    public static int asVowel(int codePoint) {
        int mapped = halfwidthCompat(codePoint);
        int source = mapped == 0 ? codePoint : mapped;
        if (isVowel(source)) {
            return source;
        }
        if (source >= 0x314F && source <= 0x3163) {
            return VOWEL_BASE + (source - 0x314F);
        }
        return 0;
    }

    /// Maps a source code point onto a modern jongseong, or `0` when it is not a trailer.
    ///
    /// @param codePoint the source code point
    /// @return the modern jongseong, or `0`
    public static int asTrail(int codePoint) {
        int mapped = halfwidthCompat(codePoint);
        int source = mapped == 0 ? codePoint : mapped;
        if (isTrail(source)) {
            return source;
        }
        if (source >= 0x3131 && source <= 0x314E) {
            return COMPAT_TRAIL[source - 0x3131];
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

    /// Returns whether `codePoint` is a precomposed Hangul syllable.
    ///
    /// @param codePoint the code point
    /// @return whether the code point is `U+AC00`–`U+D7A3`
    public static boolean isSyllable(int codePoint) {
        return codePoint >= SYLLABLE_BASE && codePoint <= 0xD7A3;
    }

    /// Decomposes a precomposed syllable into modern L/V, and T when present.
    ///
    /// @param syllable a code point in `U+AC00`–`U+D7A3`
    /// @return `{L, V}` or `{L, V, T}`
    public static int[] decompose(int syllable) {
        if (!isSyllable(syllable)) {
            throw new IllegalArgumentException("code point is not a Hangul syllable");
        }
        int sIndex = syllable - SYLLABLE_BASE;
        int trail = sIndex % TRAIL_COUNT;
        sIndex /= TRAIL_COUNT;
        int vowel = sIndex % VOWEL_COUNT;
        int lead = sIndex / VOWEL_COUNT;
        if (trail == 0) {
            return new int[] {LEAD_BASE + lead, VOWEL_BASE + vowel};
        }
        return new int[] {LEAD_BASE + lead, VOWEL_BASE + vowel, TRAIL_BASE + trail - 1};
    }
}

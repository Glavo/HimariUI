package org.glavo.himari.text;

import org.jetbrains.annotations.NotNullByDefault;

/// Composes first-stable Hangul jamo into precomposed syllables.
///
/// The mapping is the Unicode LV/LVT arithmetic: `U+AC00 + ((L * 21) + V) * 28 + T`.
/// Compatibility jamo are not remapped here.
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

    /// Returns whether `codePoint` is a modern combining jamo.
    ///
    /// @param codePoint the code point
    /// @return whether Hangul composition may consume the code point
    public static boolean isJamo(int codePoint) {
        return isLead(codePoint) || isVowel(codePoint) || isTrail(codePoint);
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

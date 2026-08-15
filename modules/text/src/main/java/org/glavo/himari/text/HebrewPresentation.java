package org.glavo.himari.text;

import org.jetbrains.annotations.NotNullByDefault;

/// Composes first-stable Hebrew letter-plus-mark pairs onto Presentation Forms-A.
///
/// Only pairs that have a dedicated presentation form are composed. The shaper applies a
/// composition only when the font maps that form.
@NotNullByDefault
public final class HebrewPresentation {
    /// Prevents instantiation.
    private HebrewPresentation() {
    }

    /// Returns the presentation form for `letter` plus `mark`, or `0` when none exists.
    ///
    /// @param letter the Hebrew letter
    /// @param mark the following combining mark
    /// @return the composed code point, or `0`
    public static int compose(int letter, int mark) {
        if (letter == 0x05E9 && mark == 0x05C1) {
            return 0xFB2A;
        }
        if (letter == 0x05E9 && mark == 0x05C2) {
            return 0xFB2B;
        }
        if (mark != 0x05BC) {
            return 0;
        }
        return switch (letter) {
            case 0x05D0 -> 0xFB30;
            case 0x05D1 -> 0xFB31;
            case 0x05D2 -> 0xFB32;
            case 0x05D3 -> 0xFB33;
            case 0x05D4 -> 0xFB34;
            case 0x05D5 -> 0xFB35;
            case 0x05D6 -> 0xFB36;
            case 0x05D8 -> 0xFB38;
            case 0x05D9 -> 0xFB39;
            case 0x05DA -> 0xFB3A;
            case 0x05DB -> 0xFB3B;
            case 0x05DC -> 0xFB3C;
            case 0x05DE -> 0xFB3E;
            case 0x05E0 -> 0xFB40;
            case 0x05E1 -> 0xFB41;
            case 0x05E3 -> 0xFB43;
            case 0x05E4 -> 0xFB44;
            case 0x05E6 -> 0xFB46;
            case 0x05E7 -> 0xFB47;
            case 0x05E8 -> 0xFB48;
            case 0x05E9 -> 0xFB49;
            case 0x05EA -> 0xFB4A;
            default -> 0;
        };
    }

    /// Returns whether `codePoint` is a first-stable Hebrew letter.
    ///
    /// @param codePoint the code point
    /// @return whether the code point is `U+05D0`–`U+05EA`
    public static boolean isLetter(int codePoint) {
        return codePoint >= 0x05D0 && codePoint <= 0x05EA;
    }
}

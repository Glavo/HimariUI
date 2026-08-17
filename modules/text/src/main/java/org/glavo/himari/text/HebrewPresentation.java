package org.glavo.himari.text;

import org.jetbrains.annotations.NotNullByDefault;

/// Composes first-stable Hebrew letter-plus-mark sequences onto Presentation Forms-A.
///
/// Pair and shin triple forms that have a dedicated presentation code point are composed,
/// including yod plus hiriq (`U+FB1D`), alef plus patah or qamats/qamats-qatan
/// (`U+FB2E` / `U+FB2F`), vav plus holam or holam haser (`U+FB4B`), bet/kaf/pe plus rafe
/// (`U+FB4C` / `U+FB4D` / `U+FB4E`), final nun or tsadi plus dagesh (`U+FB3F` / `U+FB45`),
/// and alef plus lamed (`U+FB4F`).
/// The shaper applies a composition only when the font maps that form.
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
        if (letter == 0x05D9 && mark == 0x05B4) {
            return 0xFB1D;
        }
        if (letter == 0x05D0 && mark == 0x05B7) {
            return 0xFB2E;
        }
        if (letter == 0x05D0 && (mark == 0x05B8 || mark == 0x05C7)) {
            return 0xFB2F;
        }
        if (letter == 0x05D0 && mark == 0x05DC) {
            return 0xFB4F;
        }
        if (letter == 0x05D5 && (mark == 0x05B9 || mark == 0x05BA)) {
            return 0xFB4B;
        }
        if (mark == 0x05BF) {
            return switch (letter) {
                case 0x05D1 -> 0xFB4C;
                case 0x05DB -> 0xFB4D;
                case 0x05E4 -> 0xFB4E;
                default -> 0;
            };
        }
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
            case 0x05DF -> 0xFB3F;
            case 0x05E0 -> 0xFB40;
            case 0x05E1 -> 0xFB41;
            case 0x05E3 -> 0xFB43;
            case 0x05E4 -> 0xFB44;
            case 0x05E5 -> 0xFB45;
            case 0x05E6 -> 0xFB46;
            case 0x05E7 -> 0xFB47;
            case 0x05E8 -> 0xFB48;
            case 0x05E9 -> 0xFB49;
            case 0x05EA -> 0xFB4A;
            default -> 0;
        };
    }

    /// Returns the presentation form for `letter` plus two marks, or `0` when none exists.
    ///
    /// Yod plus yod plus patah maps to `U+FB1F`. Shin plus shin-dot plus dagesh maps to
    /// `U+FB2C`. Shin plus sin-dot plus dagesh maps to `U+FB2D`. Mark order is accepted
    /// either way for the shin triples.
    ///
    /// @param letter the Hebrew letter
    /// @param first the first combining mark
    /// @param second the second combining mark
    /// @return the composed code point, or `0`
    public static int compose(int letter, int first, int second) {
        if (letter == 0x05D9 && first == 0x05D9 && second == 0x05B7) {
            return 0xFB1F;
        }
        if (letter != 0x05E9) {
            return 0;
        }
        boolean dagesh = first == 0x05BC || second == 0x05BC;
        boolean shin = first == 0x05C1 || second == 0x05C1;
        boolean sin = first == 0x05C2 || second == 0x05C2;
        if (dagesh && shin && !sin) {
            return 0xFB2C;
        }
        if (dagesh && sin && !shin) {
            return 0xFB2D;
        }
        return 0;
    }

    /// Returns whether `codePoint` is a first-stable Hebrew letter.
    ///
    /// @param codePoint the code point
    /// @return whether the code point is `U+05D0`–`U+05EA` or a Yiddish ligature
    public static boolean isLetter(int codePoint) {
        return (codePoint >= 0x05D0 && codePoint <= 0x05EA)
                || (codePoint >= 0x05F0 && codePoint <= 0x05F2);
    }

    /// Returns whether `codePoint` is a Hebrew combining mark that does not end a word.
    ///
    /// @param codePoint the code point
    /// @return whether the code point is a first-stable Hebrew point
    public static boolean isMark(int codePoint) {
        return (codePoint >= 0x05B0 && codePoint <= 0x05BD)
                || codePoint == 0x05BF
                || codePoint == 0x05C1
                || codePoint == 0x05C2
                || codePoint == 0x05C4
                || codePoint == 0x05C5
                || codePoint == 0x05C7;
    }

    /// Returns the word-final presentation of `letter`, or `0` when none exists.
    ///
    /// @param letter the Hebrew letter
    /// @return `U+05DA` / `U+05DD` / `U+05DF` / `U+05E3` / `U+05E5`, or `0`
    public static int finalForm(int letter) {
        return switch (letter) {
            case 0x05DB -> 0x05DA;
            case 0x05DE -> 0x05DD;
            case 0x05E0 -> 0x05DF;
            case 0x05E4 -> 0x05E3;
            case 0x05E6 -> 0x05E5;
            default -> 0;
        };
    }

    /// Returns the Yiddish ligature for two Hebrew letters, or `0` when none exists.
    ///
    /// @param first the first letter
    /// @param second the second letter
    /// @return `U+05F0` / `U+05F1` / `U+05F2`, or `0`
    public static int yiddishLigature(int first, int second) {
        if (first == 0x05D5 && second == 0x05D5) {
            return 0x05F0;
        }
        if (first == 0x05D5 && second == 0x05D9) {
            return 0x05F1;
        }
        if (first == 0x05D9 && second == 0x05D9) {
            return 0x05F2;
        }
        return 0;
    }

    /// Returns the wide Presentation Forms-A letter, or `0` when none exists.
    ///
    /// @param letter the Hebrew letter
    /// @return `U+FB21`–`U+FB28`, or `0`
    public static int wideForm(int letter) {
        return switch (letter) {
            case 0x05D0 -> 0xFB21;
            case 0x05D3 -> 0xFB22;
            case 0x05D4 -> 0xFB23;
            case 0x05DB -> 0xFB24;
            case 0x05DC -> 0xFB25;
            case 0x05DD -> 0xFB26;
            case 0x05E8 -> 0xFB27;
            case 0x05EA -> 0xFB28;
            default -> 0;
        };
    }

    /// Returns the alternative ayin presentation form.
    ///
    /// @return `U+FB20`
    public static int alternativeAyin() {
        return 0xFB20;
    }
}

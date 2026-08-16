package org.glavo.himari.text;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Maps Arabic letters onto Unicode Presentation Forms-B after joining analysis.
///
/// The table covers `U+0621`–`U+064A`. Letters without a requested form, tatweel, and unmapped
/// code points are returned unchanged. A caller must fall back to the nominal letter when the
/// font has no glyph for the presentation form.
@NotNullByDefault
public final class ArabicPresentation {
    /// Isolated Presentation Forms-B starts for `U+0621`–`U+064A`. Zero means no mapping.
    private static final int[] ISOLATED = isolatedStarts();

    /// Prevents instantiation.
    private ArabicPresentation() {
    }

    /// Returns the presentation-form code point for `letter` in `form`.
    ///
    /// @param letter the nominal Arabic letter
    /// @param form the resolved joining form
    /// @return the mapped code point, or `letter` when no form applies
    public static int apply(int letter, ArabicForm form) {
        if (form == ArabicForm.NONE || letter < 0x0621 || letter > 0x064A || letter == 0x0640) {
            return letter;
        }
        int isolated = ISOLATED[letter - 0x0621];
        if (isolated == 0) {
            return letter;
        }
        boolean dual = ArabicJoining.type(letter) == JoiningType.DUAL;
        return switch (form) {
            case ISOLATED -> isolated;
            case FINAL -> isolated + 1;
            case INITIAL -> dual ? isolated + 2 : isolated;
            case MEDIAL -> dual ? isolated + 3 : isolated;
            case NONE -> letter;
        };
    }

    /// Builds isolated-form starts for the first-stable Arabic block.
    private static int[] isolatedStarts() {
        int[] starts = new int[0x064A - 0x0621 + 1];
        starts[0x0621 - 0x0621] = 0xFE80;
        starts[0x0622 - 0x0621] = 0xFE81;
        starts[0x0623 - 0x0621] = 0xFE83;
        starts[0x0624 - 0x0621] = 0xFE85;
        starts[0x0625 - 0x0621] = 0xFE87;
        starts[0x0626 - 0x0621] = 0xFE89;
        starts[0x0627 - 0x0621] = 0xFE8D;
        starts[0x0628 - 0x0621] = 0xFE8F;
        starts[0x0629 - 0x0621] = 0xFE93;
        starts[0x062A - 0x0621] = 0xFE95;
        starts[0x062B - 0x0621] = 0xFE99;
        starts[0x062C - 0x0621] = 0xFE9D;
        starts[0x062D - 0x0621] = 0xFEA1;
        starts[0x062E - 0x0621] = 0xFEA5;
        starts[0x062F - 0x0621] = 0xFEA9;
        starts[0x0630 - 0x0621] = 0xFEAB;
        starts[0x0631 - 0x0621] = 0xFEAD;
        starts[0x0632 - 0x0621] = 0xFEAF;
        starts[0x0633 - 0x0621] = 0xFEB1;
        starts[0x0634 - 0x0621] = 0xFEB5;
        starts[0x0635 - 0x0621] = 0xFEB9;
        starts[0x0636 - 0x0621] = 0xFEBD;
        starts[0x0637 - 0x0621] = 0xFEC1;
        starts[0x0638 - 0x0621] = 0xFEC5;
        starts[0x0639 - 0x0621] = 0xFEC9;
        starts[0x063A - 0x0621] = 0xFECD;
        starts[0x0641 - 0x0621] = 0xFED1;
        starts[0x0642 - 0x0621] = 0xFED5;
        starts[0x0643 - 0x0621] = 0xFED9;
        starts[0x0644 - 0x0621] = 0xFEDD;
        starts[0x0645 - 0x0621] = 0xFEE1;
        starts[0x0646 - 0x0621] = 0xFEE5;
        starts[0x0647 - 0x0621] = 0xFEE9;
        starts[0x0648 - 0x0621] = 0xFEED;
        starts[0x0649 - 0x0621] = 0xFEEF;
        starts[0x064A - 0x0621] = 0xFEF1;
        return starts;
    }

    /// Returns whether `codePoint` is Arabic LAM.
    ///
    /// @param codePoint the code point
    /// @return whether the code point is `U+0644`
    public static boolean isLam(int codePoint) {
        return codePoint == 0x0644;
    }

    /// Returns whether `codePoint` is an alef that forms a lam-alef ligature.
    ///
    /// @param codePoint the code point
    /// @return whether the code point is madda, hamza-above, hamza-below, or plain alef
    public static boolean isAlef(int codePoint) {
        return codePoint == 0x0622 || codePoint == 0x0623 || codePoint == 0x0625 || codePoint == 0x0627;
    }

    /// Returns the Presentation Forms-B lam-alef for `alef` after joining analysis of LAM.
    ///
    /// Isolated and initial LAM produce the isolated ligature. Medial and final LAM produce the
    /// final ligature. `NONE` and unmapped alef variants return `0`.
    ///
    /// @param alef the alef variant
    /// @param lamForm the joining form of the preceding LAM
    /// @return the ligature code point, or `0`
    public static int lamAlef(int alef, ArabicForm lamForm) {
        Objects.requireNonNull(lamForm, "lamForm");
        if (lamForm == ArabicForm.NONE) {
            return 0;
        }
        boolean isolated = lamForm == ArabicForm.ISOLATED || lamForm == ArabicForm.INITIAL;
        return switch (alef) {
            case 0x0622 -> isolated ? 0xFEF5 : 0xFEF6;
            case 0x0623 -> isolated ? 0xFEF7 : 0xFEF8;
            case 0x0625 -> isolated ? 0xFEF9 : 0xFEFA;
            case 0x0627 -> isolated ? 0xFEFB : 0xFEFC;
            default -> 0;
        };
    }
}

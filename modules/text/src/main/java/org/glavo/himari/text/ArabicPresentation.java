package org.glavo.himari.text;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Maps Arabic letters onto Unicode Presentation Forms after joining analysis.
///
/// The table covers `U+0621`–`U+064A` plus first-stable Presentation Forms-A letters that
/// have dedicated isolated starts. Letters without a requested form, tatweel, and unmapped
/// code points are returned unchanged. A caller must fall back to the nominal letter when
/// the font has no glyph for the presentation form.
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
        if (form == ArabicForm.NONE || letter == 0x0640) {
            return letter;
        }
        int extended = extendedIsolated(letter);
        if (extended != 0) {
            if (ArabicJoining.type(letter) == JoiningType.DUAL) {
                return dualForm(extended, form);
            }
            return form == ArabicForm.FINAL ? extended + 1 : extended;
        }
        if (letter < 0x0621 || letter > 0x064A) {
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

    /// Returns the Presentation Forms-A isolated start for an extended Arabic letter.
    private static int extendedIsolated(int letter) {
        return switch (letter) {
            case 0x0671 -> 0xFB50;
            case 0x0679 -> 0xFB66;
            case 0x067A -> 0xFB5E;
            case 0x067B -> 0xFB52;
            case 0x067E -> 0xFB56;
            case 0x067F -> 0xFB62;
            case 0x0680 -> 0xFB5A;
            case 0x0683 -> 0xFB76;
            case 0x0684 -> 0xFB72;
            case 0x0686 -> 0xFB7A;
            case 0x0687 -> 0xFB7E;
            case 0x0688 -> 0xFB88;
            case 0x068C -> 0xFB84;
            case 0x068D -> 0xFB82;
            case 0x068E -> 0xFB86;
            case 0x0691 -> 0xFB8C;
            case 0x0698 -> 0xFB8A;
            case 0x06A4 -> 0xFB6A;
            case 0x06A6 -> 0xFB6E;
            case 0x06A9 -> 0xFB8E;
            case 0x06AD -> 0xFBD3;
            case 0x06AF -> 0xFB92;
            case 0x06B1 -> 0xFB9A;
            case 0x06B3 -> 0xFB96;
            case 0x06BA -> 0xFB9E;
            case 0x06BB -> 0xFBA0;
            case 0x06BE -> 0xFBAA;
            case 0x06C0 -> 0xFBA4;
            case 0x06C1 -> 0xFBA6;
            case 0x06C5 -> 0xFBE0;
            case 0x06C6 -> 0xFBD9;
            case 0x06C7 -> 0xFBD7;
            case 0x06C8 -> 0xFBDB;
            case 0x06C9 -> 0xFBE2;
            case 0x06CB -> 0xFBDE;
            case 0x06CC -> 0xFBFC;
            case 0x06D0 -> 0xFBE4;
            case 0x06D2 -> 0xFBAE;
            case 0x06D3 -> 0xFBB0;
            default -> 0;
        };
    }

    /// Returns the isolated-based dual-joining presentation form.
    private static int dualForm(int isolated, ArabicForm form) {
        return switch (form) {
            case ISOLATED -> isolated;
            case FINAL -> isolated + 1;
            case INITIAL -> isolated + 2;
            case MEDIAL -> isolated + 3;
            case NONE -> isolated;
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

    /// Returns the Presentation Forms-A shadda-plus-vowel ligature, or `0` when none exists.
    ///
    /// @param first the first mark
    /// @param second the second mark
    /// @return `U+FC5E`–`U+FC63`, or `0`
    public static int shaddaLigature(int first, int second) {
        int shadda;
        int vowel;
        if (first == 0x0651) {
            shadda = first;
            vowel = second;
        } else if (second == 0x0651) {
            shadda = second;
            vowel = first;
        } else {
            return 0;
        }
        if (shadda != 0x0651) {
            return 0;
        }
        return switch (vowel) {
            case 0x064C -> 0xFC5E;
            case 0x064D -> 0xFC5F;
            case 0x064E -> 0xFC60;
            case 0x064F -> 0xFC61;
            case 0x0650 -> 0xFC62;
            case 0x0670 -> 0xFC63;
            default -> 0;
        };
    }

    /// Returns `U+FDF2` when `points[index]` starts an Allah sequence.
    ///
    /// The first-stable match is LAM, optional transparent marks, LAM, optional transparent
    /// marks including shadda and superscript alef, then HEH.
    ///
    /// @param points the code points
    /// @param index the candidate LAM index
    /// @param count the populated length of `points`
    /// @return `U+FDF2`, or `0`
    public static int allahLigature(int[] points, int index, int count) {
        return allahEnd(points, index, count) < 0 ? 0 : 0xFDF2;
    }

    /// Returns how many code points [`#allahLigature(int[], int, int)`] consumes.
    ///
    /// @param points the code points
    /// @param index the candidate LAM index
    /// @param count the populated length of `points`
    /// @return the consumed count, or `0`
    public static int allahLength(int[] points, int index, int count) {
        int end = allahEnd(points, index, count);
        return end < 0 ? 0 : end - index;
    }

    /// Returns the exclusive end index of an Allah sequence, or `-1`.
    private static int allahEnd(int[] points, int index, int count) {
        Objects.requireNonNull(points, "points");
        if (index < 0 || count < 0 || count > points.length || index >= count || points[index] != 0x0644) {
            return -1;
        }
        int cursor = nextKept(points, index + 1, count);
        if (cursor < 0 || points[cursor] != 0x0644) {
            return -1;
        }
        cursor = nextKept(points, cursor + 1, count);
        if (cursor < 0 || points[cursor] != 0x0647) {
            return -1;
        }
        return cursor + 1;
    }

    /// Returns the next non-transparent index, or `-1` when none remains.
    private static int nextKept(int[] points, int index, int count) {
        int cursor = index;
        while (cursor < count && ArabicJoining.isTransparent(points[cursor])) {
            cursor++;
        }
        return cursor < count ? cursor : -1;
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

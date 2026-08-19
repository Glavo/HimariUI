package org.glavo.himari.text;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Resolves Unicode Arabic joining types and `isol`/`init`/`medi`/`fina` forms.
///
/// Neighbor search skips transparent marks. Logical previous is the visual-right side of a
/// right-to-left Arabic run.
@NotNullByDefault
public final class ArabicJoining {
    /// Prevents instantiation.
    private ArabicJoining() {
    }

    /// Returns the joining type of one code point.
    ///
    /// @param codePoint the Unicode code point
    /// @return the joining type
    public static JoiningType type(int codePoint) {
        if (codePoint == 0x200D || codePoint == 0x0640) {
            return JoiningType.JOIN_CAUSING;
        }
        if (codePoint == 0x200C) {
            return JoiningType.NON_JOINING;
        }
        if (isTransparent(codePoint)) {
            return JoiningType.TRANSPARENT;
        }
        if (codePoint >= 0x0750 && codePoint <= 0x077F) {
            return JoiningType.DUAL;
        }
        if (codePoint >= 0x0870 && codePoint <= 0x089F) {
            return extendedBJoining(codePoint);
        }
        if (codePoint >= 0x08A0 && codePoint <= 0x08FF) {
            return extendedAJoining(codePoint);
        }
        if (rightJoining(codePoint)) {
            return JoiningType.RIGHT;
        }
        if (dualJoining(codePoint)) {
            return JoiningType.DUAL;
        }
        if (codePoint < 0x0621 || codePoint > 0x064A) {
            return JoiningType.NON_JOINING;
        }
        return switch (codePoint) {
            case 0x0621 -> JoiningType.NON_JOINING;
            case 0x0622, 0x0623, 0x0624, 0x0625, 0x0627, 0x0629,
                    0x062F, 0x0630, 0x0631, 0x0632, 0x0648 -> JoiningType.RIGHT;
            default -> JoiningType.DUAL;
        };
    }

    /// Writes one presentation form per code point into `forms`.
    ///
    /// @param points the decoded code points
    /// @param count the code-point count
    /// @param forms the destination, length at least `count`
    public static void forms(int[] points, int count, ArabicForm[] forms) {
        Objects.requireNonNull(points, "points");
        Objects.requireNonNull(forms, "forms");
        if (count < 0 || count > points.length || count > forms.length) {
            throw new IllegalArgumentException("Joining count exceeds buffer length");
        }
        for (int index = 0; index < count; index++) {
            forms[index] = formAt(points, count, index);
        }
    }

    /// Returns the form of `points[index]`.
    ///
    /// @param points the decoded code points
    /// @param count the code-point count
    /// @param index the letter index
    /// @return the form
    public static ArabicForm formAt(int[] points, int count, int index) {
        Objects.requireNonNull(points, "points");
        if (index < 0 || index >= count || count > points.length) {
            throw new IllegalArgumentException("Joining index is out of range");
        }
        JoiningType type = type(points[index]);
        if (type != JoiningType.DUAL && type != JoiningType.RIGHT && type != JoiningType.LEFT) {
            return ArabicForm.NONE;
        }
        JoiningType previous = neighbor(points, count, index, -1);
        JoiningType next = neighbor(points, count, index, 1);
        boolean joinPrevious = previous == JoiningType.DUAL
                || previous == JoiningType.LEFT
                || previous == JoiningType.JOIN_CAUSING;
        boolean joinNext = next == JoiningType.DUAL
                || next == JoiningType.RIGHT
                || next == JoiningType.JOIN_CAUSING;
        if (type == JoiningType.RIGHT) {
            return joinPrevious ? ArabicForm.FINAL : ArabicForm.ISOLATED;
        }
        if (type == JoiningType.LEFT) {
            return joinNext ? ArabicForm.INITIAL : ArabicForm.ISOLATED;
        }
        if (joinPrevious && joinNext) {
            return ArabicForm.MEDIAL;
        }
        if (joinPrevious) {
            return ArabicForm.FINAL;
        }
        if (joinNext) {
            return ArabicForm.INITIAL;
        }
        return ArabicForm.ISOLATED;
    }

    /// Finds the nearest non-transparent neighbor in `direction`.
    private static JoiningType neighbor(int[] points, int count, int index, int direction) {
        for (int cursor = index + direction; cursor >= 0 && cursor < count; cursor += direction) {
            JoiningType type = type(points[cursor]);
            if (type != JoiningType.TRANSPARENT) {
                return type;
            }
        }
        return JoiningType.NON_JOINING;
    }

    /// Classifies Arabic Extended-B letters, tatweel variants, and combining marks.
    private static JoiningType extendedBJoining(int codePoint) {
        if (codePoint >= 0x0890) {
            return JoiningType.TRANSPARENT;
        }
        return switch (codePoint) {
            case 0x0883, 0x0884, 0x0885 -> JoiningType.JOIN_CAUSING;
            case 0x0888, 0x088F -> JoiningType.NON_JOINING;
            case 0x0870, 0x0871, 0x0872, 0x0873, 0x0874, 0x0875, 0x0876, 0x0877,
                    0x0878, 0x0879, 0x087A, 0x087B, 0x087C, 0x087D, 0x087E, 0x087F,
                    0x0880, 0x0881, 0x0882 -> JoiningType.RIGHT;
            default -> JoiningType.DUAL;
        };
    }

    /// Classifies Arabic Extended-A letters and combining marks.
    private static JoiningType extendedAJoining(int codePoint) {
        if (codePoint >= 0x08D3) {
            return JoiningType.TRANSPARENT;
        }
        return switch (codePoint) {
            case 0x08AA, 0x08AB, 0x08AC, 0x08AD, 0x08AE, 0x08B1, 0x08B2, 0x08B9 -> JoiningType.RIGHT;
            default -> JoiningType.DUAL;
        };
    }

    /// Returns whether `codePoint` is a first-stable right-joining extended letter.
    private static boolean rightJoining(int codePoint) {
        return codePoint == 0x0671
                || codePoint == 0x0688
                || codePoint == 0x068C
                || codePoint == 0x068D
                || codePoint == 0x068E
                || codePoint == 0x0691
                || codePoint == 0x0698
                || codePoint == 0x06BA
                || codePoint == 0x06C0
                || codePoint == 0x06C5
                || codePoint == 0x06C6
                || codePoint == 0x06C7
                || codePoint == 0x06C8
                || codePoint == 0x06C9
                || codePoint == 0x06CB
                || codePoint == 0x06D2
                || codePoint == 0x06D3
                || codePoint == 0x06D5
                || codePoint == 0x06EE
                || codePoint == 0x06EF
                || (codePoint >= 0x0672 && codePoint <= 0x0673)
                || (codePoint >= 0x0675 && codePoint <= 0x0677)
                || (codePoint >= 0x0689 && codePoint <= 0x068B)
                || codePoint == 0x068F
                || codePoint == 0x0690
                || (codePoint >= 0x0692 && codePoint <= 0x0697)
                || codePoint == 0x0699
                || codePoint == 0x06C3
                || codePoint == 0x06C4
                || codePoint == 0x06CA
                || codePoint == 0x06CD
                || codePoint == 0x06CF;
    }

    /// Returns whether `codePoint` is a first-stable dual-joining extended letter.
    private static boolean dualJoining(int codePoint) {
        return codePoint == 0x0679
                || codePoint == 0x067A
                || codePoint == 0x067B
                || codePoint == 0x067E
                || codePoint == 0x067F
                || codePoint == 0x0680
                || codePoint == 0x0683
                || codePoint == 0x0684
                || codePoint == 0x0686
                || codePoint == 0x0687
                || codePoint == 0x06A4
                || codePoint == 0x06A6
                || codePoint == 0x06A9
                || codePoint == 0x06AF
                || codePoint == 0x06B1
                || codePoint == 0x06B3
                || codePoint == 0x06BB
                || codePoint == 0x06BE
                || codePoint == 0x06C1
                || codePoint == 0x06CC
                || codePoint == 0x06D0
                || codePoint == 0x06AD
                || codePoint == 0x0678
                || codePoint == 0x0681
                || codePoint == 0x0682
                || codePoint == 0x0685
                || (codePoint >= 0x069A && codePoint <= 0x069F)
                || (codePoint >= 0x06A0 && codePoint <= 0x06A3)
                || codePoint == 0x06A5
                || codePoint == 0x06A7
                || codePoint == 0x06A8
                || (codePoint >= 0x06AA && codePoint <= 0x06AC)
                || codePoint == 0x06AE
                || codePoint == 0x06B0
                || codePoint == 0x06B2
                || (codePoint >= 0x06B4 && codePoint <= 0x06B9)
                || codePoint == 0x06BC
                || codePoint == 0x06BD
                || codePoint == 0x06BF
                || codePoint == 0x06C2
                || codePoint == 0x06CE
                || codePoint == 0x06D1;
    }

    /// Returns whether `codePoint` is a joining-transparent mark.
    ///
    /// @param codePoint the code point
    /// @return whether neighbors skip this character
    public static boolean isTransparent(int codePoint) {
        return (codePoint >= 0x0610 && codePoint <= 0x061A)
                || (codePoint >= 0x064B && codePoint <= 0x065F)
                || codePoint == 0x0670
                || (codePoint >= 0x06D6 && codePoint <= 0x06DC)
                || (codePoint >= 0x06DF && codePoint <= 0x06E4)
                || (codePoint >= 0x06E7 && codePoint <= 0x06E8)
                || (codePoint >= 0x06EA && codePoint <= 0x06ED)
                || (codePoint >= 0x0591 && codePoint <= 0x05BD)
                || codePoint == 0x05BF
                || codePoint == 0x05C1
                || codePoint == 0x05C2
                || codePoint == 0x05C4
                || codePoint == 0x05C5
                || codePoint == 0x05C7;
    }

    /// Returns whether `codePoint` is an Arabic joining letter or join cause.
    ///
    /// @param codePoint the code point
    /// @return whether Arabic presentation analysis applies
    public static boolean isArabicLetter(int codePoint) {
        JoiningType type = type(codePoint);
        return type == JoiningType.DUAL
                || type == JoiningType.RIGHT
                || type == JoiningType.LEFT
                || type == JoiningType.JOIN_CAUSING;
    }
}

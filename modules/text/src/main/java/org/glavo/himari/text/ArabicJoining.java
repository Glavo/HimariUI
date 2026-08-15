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

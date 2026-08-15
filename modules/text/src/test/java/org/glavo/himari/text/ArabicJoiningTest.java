package org.glavo.himari.text;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Verifies Arabic joining types and isol/init/medi/fina selection.
@NotNullByDefault
final class ArabicJoiningTest {
    /// Classifies dual, right-joining, tatweel, and marks.
    @Test
    void classifiesJoiningTypes() {
        assertEquals(JoiningType.DUAL, ArabicJoining.type('\u0628'));
        assertEquals(JoiningType.RIGHT, ArabicJoining.type('\u0627'));
        assertEquals(JoiningType.JOIN_CAUSING, ArabicJoining.type('\u0640'));
        assertEquals(JoiningType.JOIN_CAUSING, ArabicJoining.type('\u200D'));
        assertEquals(JoiningType.TRANSPARENT, ArabicJoining.type('\u064E'));
        assertEquals(JoiningType.NON_JOINING, ArabicJoining.type('A'));
    }

    /// Selects isol/init/medi/fina for dual-joining letters.
    @Test
    void selectsDualForms() {
        assertEquals(ArabicForm.ISOLATED, form("\u0628", 0));
        assertEquals(ArabicForm.INITIAL, form("\u0628\u062A", 0));
        assertEquals(ArabicForm.FINAL, form("\u0628\u062A", 1));
        assertEquals(ArabicForm.INITIAL, form("\u0628\u0628\u0628", 0));
        assertEquals(ArabicForm.MEDIAL, form("\u0628\u0628\u0628", 1));
        assertEquals(ArabicForm.FINAL, form("\u0628\u0628\u0628", 2));
    }

    /// Selects isolated or final forms for right-joining alef.
    @Test
    void selectsRightJoiningForms() {
        assertEquals(ArabicForm.ISOLATED, form("\u0627", 0));
        assertEquals(ArabicForm.INITIAL, form("\u0628\u0627", 0));
        assertEquals(ArabicForm.FINAL, form("\u0628\u0627", 1));
        assertEquals(ArabicForm.ISOLATED, form("\u0627\u0628", 0));
        assertEquals(ArabicForm.ISOLATED, form("\u0627\u0628", 1));
    }

    /// Skips a fatha when looking for joining neighbors.
    @Test
    void skipsTransparentMarks() {
        assertEquals(ArabicForm.INITIAL, form("\u0628\u064E\u062A", 0));
        assertEquals(ArabicForm.NONE, form("\u0628\u064E\u062A", 1));
        assertEquals(ArabicForm.FINAL, form("\u0628\u064E\u062A", 2));
    }

    /// Forces a final form after ZWJ.
    @Test
    void zwjForcesFinal() {
        assertEquals(ArabicForm.NONE, form("\u200D\u0628", 0));
        assertEquals(ArabicForm.FINAL, form("\u200D\u0628", 1));
    }

    /// Resolves the form at `index` of `text`.
    private static ArabicForm form(String text, int index) {
        int[] points = text.codePoints().toArray();
        return ArabicJoining.formAt(points, points.length, index);
    }
}

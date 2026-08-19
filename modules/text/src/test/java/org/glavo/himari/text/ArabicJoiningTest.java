package org.glavo.himari.text;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    /// Selects isolated versus final lam-alef from LAM joining.
    @Test
    void selectsLamAlefForms() {
        assertEquals(0xFEFB, ArabicPresentation.lamAlef(0x0627, ArabicForm.ISOLATED));
        assertEquals(0xFEFB, ArabicPresentation.lamAlef(0x0627, ArabicForm.INITIAL));
        assertEquals(0xFEFC, ArabicPresentation.lamAlef(0x0627, ArabicForm.MEDIAL));
        assertEquals(0xFEFC, ArabicPresentation.lamAlef(0x0627, ArabicForm.FINAL));
        assertEquals(0xFEF5, ArabicPresentation.lamAlef(0x0622, ArabicForm.ISOLATED));
        assertEquals(0, ArabicPresentation.lamAlef(0x0627, ArabicForm.NONE));
        assertTrue(ArabicPresentation.isLam(0x0644));
        assertTrue(ArabicPresentation.isAlef(0x0627));
        assertEquals(0xFC5E, ArabicPresentation.shaddaLigature(0x0651, 0x064C));
        assertEquals(0xFC5F, ArabicPresentation.shaddaLigature(0x064D, 0x0651));
        assertEquals(0xFC60, ArabicPresentation.shaddaLigature(0x0651, 0x064E));
        assertEquals(0xFC61, ArabicPresentation.shaddaLigature(0x064F, 0x0651));
        assertEquals(0xFC62, ArabicPresentation.shaddaLigature(0x0651, 0x0650));
        assertEquals(0xFC63, ArabicPresentation.shaddaLigature(0x0651, 0x0670));
        assertEquals(0, ArabicPresentation.shaddaLigature(0x064E, 0x064F));
        int[] allah = {0x0644, 0x0644, 0x0647};
        assertEquals(0xFDF2, ArabicPresentation.allahLigature(allah, 0, allah.length));
        assertEquals(3, ArabicPresentation.allahLength(allah, 0, allah.length));
        int[] marked = {0x0644, 0x0644, 0x0651, 0x0670, 0x0647};
        assertEquals(0xFDF2, ArabicPresentation.allahLigature(marked, 0, marked.length));
        assertEquals(5, ArabicPresentation.allahLength(marked, 0, marked.length));
        assertEquals(0, ArabicPresentation.allahLigature(new int[] {0x0644, 0x0627}, 0, 2));
        assertEquals(JoiningType.RIGHT, ArabicJoining.type(0x0671));
        assertEquals(0xFB50, ArabicPresentation.apply(0x0671, ArabicForm.ISOLATED));
        assertEquals(0xFB51, ArabicPresentation.apply(0x0671, ArabicForm.FINAL));
        assertEquals(JoiningType.DUAL, ArabicJoining.type(0x067E));
        assertEquals(JoiningType.DUAL, ArabicJoining.type(0x0686));
        assertEquals(0xFB56, ArabicPresentation.apply(0x067E, ArabicForm.ISOLATED));
        assertEquals(0xFB59, ArabicPresentation.apply(0x067E, ArabicForm.MEDIAL));
        assertEquals(0xFB7A, ArabicPresentation.apply(0x0686, ArabicForm.ISOLATED));
        assertEquals(0xFB7C, ArabicPresentation.apply(0x0686, ArabicForm.INITIAL));
        assertEquals(JoiningType.DUAL, ArabicJoining.type(0x0679));
        assertEquals(0xFB66, ArabicPresentation.apply(0x0679, ArabicForm.ISOLATED));
        assertEquals(0xFB69, ArabicPresentation.apply(0x0679, ArabicForm.MEDIAL));
        assertEquals(JoiningType.RIGHT, ArabicJoining.type(0x0698));
        assertEquals(0xFB8A, ArabicPresentation.apply(0x0698, ArabicForm.ISOLATED));
        assertEquals(0xFB8B, ArabicPresentation.apply(0x0698, ArabicForm.FINAL));
        assertEquals(JoiningType.DUAL, ArabicJoining.type(0x06A4));
        assertEquals(0xFB6A, ArabicPresentation.apply(0x06A4, ArabicForm.ISOLATED));
        assertEquals(JoiningType.DUAL, ArabicJoining.type(0x06A9));
        assertEquals(0xFB8E, ArabicPresentation.apply(0x06A9, ArabicForm.ISOLATED));
        assertEquals(JoiningType.DUAL, ArabicJoining.type(0x06AF));
        assertEquals(0xFB92, ArabicPresentation.apply(0x06AF, ArabicForm.ISOLATED));
        assertEquals(JoiningType.DUAL, ArabicJoining.type(0x06CC));
        assertEquals(0xFBFC, ArabicPresentation.apply(0x06CC, ArabicForm.ISOLATED));
        assertEquals(0xFBFF, ArabicPresentation.apply(0x06CC, ArabicForm.MEDIAL));
        assertEquals(JoiningType.RIGHT, ArabicJoining.type(0x06BA));
        assertEquals(0xFB9E, ArabicPresentation.apply(0x06BA, ArabicForm.ISOLATED));
        assertEquals(0xFB9F, ArabicPresentation.apply(0x06BA, ArabicForm.FINAL));
        assertEquals(JoiningType.RIGHT, ArabicJoining.type(0x06D2));
        assertEquals(0xFBAE, ArabicPresentation.apply(0x06D2, ArabicForm.ISOLATED));
        assertEquals(0xFBAF, ArabicPresentation.apply(0x06D2, ArabicForm.FINAL));
        assertEquals(JoiningType.DUAL, ArabicJoining.type(0x067B));
        assertEquals(0xFB52, ArabicPresentation.apply(0x067B, ArabicForm.ISOLATED));
        assertEquals(0xFB55, ArabicPresentation.apply(0x067B, ArabicForm.MEDIAL));
        assertEquals(JoiningType.RIGHT, ArabicJoining.type(0x0688));
        assertEquals(0xFB88, ArabicPresentation.apply(0x0688, ArabicForm.ISOLATED));
        assertEquals(0xFB89, ArabicPresentation.apply(0x0688, ArabicForm.FINAL));
        assertEquals(JoiningType.RIGHT, ArabicJoining.type(0x0691));
        assertEquals(0xFB8C, ArabicPresentation.apply(0x0691, ArabicForm.ISOLATED));
        assertEquals(JoiningType.DUAL, ArabicJoining.type(0x06C1));
        assertEquals(0xFBA6, ArabicPresentation.apply(0x06C1, ArabicForm.ISOLATED));
        assertEquals(JoiningType.DUAL, ArabicJoining.type(0x06D0));
        assertEquals(0xFBE4, ArabicPresentation.apply(0x06D0, ArabicForm.ISOLATED));
        assertEquals(0xFBE7, ArabicPresentation.apply(0x06D0, ArabicForm.MEDIAL));
        assertEquals(JoiningType.DUAL, ArabicJoining.type(0x06AD));
        assertEquals(0xFBD3, ArabicPresentation.apply(0x06AD, ArabicForm.ISOLATED));
        assertEquals(0xFBD6, ArabicPresentation.apply(0x06AD, ArabicForm.MEDIAL));
        assertEquals(JoiningType.RIGHT, ArabicJoining.type(0x06D5));
        assertEquals(JoiningType.RIGHT, ArabicJoining.type(0x06EE));
        assertEquals(JoiningType.RIGHT, ArabicJoining.type(0x06EF));
        assertEquals(JoiningType.RIGHT, ArabicJoining.type(0x0672));
        assertEquals(JoiningType.RIGHT, ArabicJoining.type(0x0693));
        assertEquals(JoiningType.RIGHT, ArabicJoining.type(0x06CD));
        assertEquals(JoiningType.DUAL, ArabicJoining.type(0x0678));
        assertEquals(JoiningType.DUAL, ArabicJoining.type(0x06A1));
        assertEquals(JoiningType.DUAL, ArabicJoining.type(0x06AA));
        assertEquals(JoiningType.DUAL, ArabicJoining.type(0x06D1));
        assertEquals(ArabicForm.FINAL, form("\u0628\u0693", 1));
        assertEquals(ArabicForm.INITIAL, form("\u06A1\u0628", 0));
        assertEquals(JoiningType.DUAL, ArabicJoining.type(0x08A0));
        assertEquals(JoiningType.RIGHT, ArabicJoining.type(0x08AA));
        assertEquals(JoiningType.RIGHT, ArabicJoining.type(0x08AD));
        assertEquals(JoiningType.TRANSPARENT, ArabicJoining.type(0x08E4));
        assertEquals(ArabicForm.FINAL, form("\u0628\u08AA", 1));
        assertEquals(ArabicForm.INITIAL, form("\u08A0\u0628", 0));
        assertEquals(JoiningType.RIGHT, ArabicJoining.type(0x0870));
        assertEquals(JoiningType.JOIN_CAUSING, ArabicJoining.type(0x0883));
        assertEquals(JoiningType.DUAL, ArabicJoining.type(0x0886));
        assertEquals(JoiningType.NON_JOINING, ArabicJoining.type(0x0888));
        assertEquals(JoiningType.TRANSPARENT, ArabicJoining.type(0x0898));
        assertEquals(ArabicForm.FINAL, form("\u0628\u0870", 1));
        assertEquals(ArabicForm.INITIAL, form("\u0886\u0628", 0));
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

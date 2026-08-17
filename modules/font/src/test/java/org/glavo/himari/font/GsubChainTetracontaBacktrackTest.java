package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Verifies forty-glyph GSUB chain backtrack through [`SfntFont#chainSubstitute(int[], int, int, int)`].
@NotNullByDefault
final class GsubChainTetracontaBacktrackTest {
    /// Substitutes `A` only when format-1 backtrack `[@?>=<;:987654321ZYXWVUTSRQPONMLKJIHGFED` and lookahead `BC` are present.
    @Test
    void format1BacktrackRequiresBracketAtQuestionGreaterEqualLessSemicolonColonNineEightSevenSixFiveFourThreeTwoOneZyxwvutsrqponmlkjihgfed() {
        SfntFont font = GsubChainTetracontaBacktrackSampleFont.create();
        int a = font.glyphId('A');
        int b = font.glyphId('B');
        int c = font.glyphId('C');
        int d = font.glyphId('D');
        int e = font.glyphId('E');
        int f = font.glyphId('F');
        int g = font.glyphId('G');
        int h = font.glyphId('H');
        int i = font.glyphId('I');
        int j = font.glyphId('J');
        int k = font.glyphId('K');
        int l = font.glyphId('L');
        int m = font.glyphId('M');
        int n = font.glyphId('N');
        int o = font.glyphId('O');
        int p = font.glyphId('P');
        int q = font.glyphId('Q');
        int r = font.glyphId('R');
        int s = font.glyphId('S');
        int t = font.glyphId('T');
        int u = font.glyphId('U');
        int v = font.glyphId('V');
        int w = font.glyphId('W');
        int x = font.glyphId('X');
        int y = font.glyphId('Y');
        int z = font.glyphId('Z');
        int zero = font.glyphId('0');
        int one = font.glyphId('1');
        int two = font.glyphId('2');
        int three = font.glyphId('3');
        int four = font.glyphId('4');
        int five = font.glyphId('5');
        int six = font.glyphId('6');
        int seven = font.glyphId('7');
        int eight = font.glyphId('8');
        int nine = font.glyphId('9');
        int colon = font.glyphId(':');
        int semicolon = font.glyphId(';');
        int less = font.glyphId('<');
        int equal = font.glyphId('=');
        int greater = font.glyphId('>');
        int question = font.glyphId('?');
        int at = font.glyphId('@');
        int bracket = font.glyphId('[');
        assertEquals(GsubChainTetracontaBacktrackSampleFont.GLYPH_Y, y);
        assertEquals(GsubChainTetracontaBacktrackSampleFont.GLYPH_Z, z);
        assertEquals(GsubChainTetracontaBacktrackSampleFont.GLYPH_ZERO, zero);
        assertEquals(GsubChainTetracontaBacktrackSampleFont.GLYPH_ONE, one);
        assertEquals(GsubChainTetracontaBacktrackSampleFont.GLYPH_TWO, two);
        assertEquals(GsubChainTetracontaBacktrackSampleFont.GLYPH_THREE, three);
        assertEquals(GsubChainTetracontaBacktrackSampleFont.GLYPH_FOUR, four);
        assertEquals(GsubChainTetracontaBacktrackSampleFont.GLYPH_FIVE, five);
        assertEquals(GsubChainTetracontaBacktrackSampleFont.GLYPH_SIX, six);
        assertEquals(GsubChainTetracontaBacktrackSampleFont.GLYPH_SEVEN, seven);
        assertEquals(GsubChainTetracontaBacktrackSampleFont.GLYPH_EIGHT, eight);
        assertEquals(GsubChainTetracontaBacktrackSampleFont.GLYPH_NINE, nine);
        assertEquals(GsubChainTetracontaBacktrackSampleFont.GLYPH_COLON, colon);
        assertEquals(GsubChainTetracontaBacktrackSampleFont.GLYPH_SEMICOLON, semicolon);
        assertEquals(GsubChainTetracontaBacktrackSampleFont.GLYPH_LESS, less);
        assertEquals(GsubChainTetracontaBacktrackSampleFont.GLYPH_EQUAL, equal);
        assertEquals(GsubChainTetracontaBacktrackSampleFont.GLYPH_GREATER, greater);
        assertEquals(GsubChainTetracontaBacktrackSampleFont.GLYPH_QUESTION, question);
        assertEquals(GsubChainTetracontaBacktrackSampleFont.GLYPH_AT, at);
        assertEquals(GsubChainTetracontaBacktrackSampleFont.GLYPH_BRACKET, bracket);
        assertEquals(a, font.chainSubstitute(a, b, c, SfntFont.TAG_CALT));
        assertEquals(
                zero,
                font.chainSubstitute(
                        new int[] {bracket, at, question, greater, equal, less, semicolon, colon, nine, eight, seven, six, five, four, three, two, one, z, y, x, w, v, u, t, s, r, q, p, o, n, m, l, k, j, i, h, g, f, e, d, a, b, c},
                        40,
                        3,
                        SfntFont.TAG_CALT
                )
        );
        assertEquals(
                a,
                font.chainSubstitute(
                        new int[] {at, question, greater, equal, less, semicolon, colon, nine, eight, seven, six, five, four, three, two, one, z, y, x, w, v, u, t, s, r, q, p, o, n, m, l, k, j, i, h, g, f, e, d, a, b, c},
                        39,
                        3,
                        SfntFont.TAG_CALT
                )
        );
        assertEquals(
                a,
                font.chainSubstitute(
                        new int[] {bracket, question, greater, equal, less, semicolon, colon, nine, eight, seven, six, five, four, three, two, one, z, y, x, w, v, u, t, s, r, q, p, o, n, m, l, k, j, i, h, g, f, e, d, a, b, c},
                        39,
                        3,
                        SfntFont.TAG_CALT
                )
        );
    }
}

package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Verifies thirty-six-glyph GPOS chain backtrack through [`SfntFont#chainAdjustment(int[], int, int)`].
@NotNullByDefault
final class GposChainHexatriacontaBacktrackTest {
    /// Applies the format-1 chain only when backtrack `=<;:987654321ZYXWVUTSRQPONMLKJIHGFED` precedes `ABC`.
    @Test
    void format1BacktrackRequiresEqualLessSemicolonColonNineEightSevenSixFiveFourThreeTwoOneZyxwvutsrqponmlkjihgfed() {
        SfntFont font = GposChainHexatriacontaBacktrackSampleFont.create();
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
        assertEquals(GposChainHexatriacontaBacktrackSampleFont.GLYPH_Y, y);
        assertEquals(GposChainHexatriacontaBacktrackSampleFont.GLYPH_Z, z);
        assertEquals(GposChainHexatriacontaBacktrackSampleFont.GLYPH_ONE, one);
        assertEquals(GposChainHexatriacontaBacktrackSampleFont.GLYPH_TWO, two);
        assertEquals(GposChainHexatriacontaBacktrackSampleFont.GLYPH_THREE, three);
        assertEquals(GposChainHexatriacontaBacktrackSampleFont.GLYPH_FOUR, four);
        assertEquals(GposChainHexatriacontaBacktrackSampleFont.GLYPH_FIVE, five);
        assertEquals(GposChainHexatriacontaBacktrackSampleFont.GLYPH_SIX, six);
        assertEquals(GposChainHexatriacontaBacktrackSampleFont.GLYPH_SEVEN, seven);
        assertEquals(GposChainHexatriacontaBacktrackSampleFont.GLYPH_EIGHT, eight);
        assertEquals(GposChainHexatriacontaBacktrackSampleFont.GLYPH_NINE, nine);
        assertEquals(GposChainHexatriacontaBacktrackSampleFont.GLYPH_COLON, colon);
        assertEquals(GposChainHexatriacontaBacktrackSampleFont.GLYPH_SEMICOLON, semicolon);
        assertEquals(GposChainHexatriacontaBacktrackSampleFont.GLYPH_LESS, less);
        assertEquals(GposChainHexatriacontaBacktrackSampleFont.GLYPH_EQUAL, equal);
        assertEquals(0, font.chainAdjustment(a, b, c));
        assertEquals(
                GposChainHexatriacontaBacktrackSampleFont.CHAIN_DELTA,
                font.chainAdjustment(
                        new int[] {equal, less, semicolon, colon, nine, eight, seven, six, five, four, three, two, one, z, y, x, w, v, u, t, s, r, q, p, o, n, m, l, k, j, i, h, g, f, e, d, a, b, c},
                        36,
                        3
                )
        );
        assertEquals(
                0,
                font.chainAdjustment(new int[] {less, semicolon, colon, nine, eight, seven, six, five, four, three, two, one, z, y, x, w, v, u, t, s, r, q, p, o, n, m, l, k, j, i, h, g, f, e, d, a, b, c}, 35, 3)
        );
        assertEquals(
                0,
                font.chainAdjustment(new int[] {equal, semicolon, colon, nine, eight, seven, six, five, four, three, two, one, z, y, x, w, v, u, t, s, r, q, p, o, n, m, l, k, j, i, h, g, f, e, d, a, b, c}, 35, 3)
        );
    }
}

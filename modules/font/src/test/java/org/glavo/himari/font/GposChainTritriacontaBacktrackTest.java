package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Verifies thirty-three-glyph GPOS chain backtrack through [`SfntFont#chainAdjustment(int[], int, int)`].
@NotNullByDefault
final class GposChainTritriacontaBacktrackTest {
    /// Applies the format-1 chain only when backtrack `:987654321ZYXWVUTSRQPONMLKJIHGFED` precedes `ABC`.
    @Test
    void format1BacktrackRequiresColonNineEightSevenSixFiveFourThreeTwoOneZyxwvutsrqponmlkjihgfed() {
        SfntFont font = GposChainTritriacontaBacktrackSampleFont.create();
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
        assertEquals(GposChainTritriacontaBacktrackSampleFont.GLYPH_Y, y);
        assertEquals(GposChainTritriacontaBacktrackSampleFont.GLYPH_Z, z);
        assertEquals(GposChainTritriacontaBacktrackSampleFont.GLYPH_ONE, one);
        assertEquals(GposChainTritriacontaBacktrackSampleFont.GLYPH_TWO, two);
        assertEquals(GposChainTritriacontaBacktrackSampleFont.GLYPH_THREE, three);
        assertEquals(GposChainTritriacontaBacktrackSampleFont.GLYPH_FOUR, four);
        assertEquals(GposChainTritriacontaBacktrackSampleFont.GLYPH_FIVE, five);
        assertEquals(GposChainTritriacontaBacktrackSampleFont.GLYPH_SIX, six);
        assertEquals(GposChainTritriacontaBacktrackSampleFont.GLYPH_SEVEN, seven);
        assertEquals(GposChainTritriacontaBacktrackSampleFont.GLYPH_EIGHT, eight);
        assertEquals(GposChainTritriacontaBacktrackSampleFont.GLYPH_NINE, nine);
        assertEquals(GposChainTritriacontaBacktrackSampleFont.GLYPH_COLON, colon);
        assertEquals(0, font.chainAdjustment(a, b, c));
        assertEquals(
                GposChainTritriacontaBacktrackSampleFont.CHAIN_DELTA,
                font.chainAdjustment(
                        new int[] {colon, nine, eight, seven, six, five, four, three, two, one, z, y, x, w, v, u, t, s, r, q, p, o, n, m, l, k, j, i, h, g, f, e, d, a, b, c},
                        33,
                        3
                )
        );
        assertEquals(
                0,
                font.chainAdjustment(new int[] {nine, eight, seven, six, five, four, three, two, one, z, y, x, w, v, u, t, s, r, q, p, o, n, m, l, k, j, i, h, g, f, e, d, a, b, c}, 32, 3)
        );
        assertEquals(
                0,
                font.chainAdjustment(new int[] {colon, eight, seven, six, five, four, three, two, one, z, y, x, w, v, u, t, s, r, q, p, o, n, m, l, k, j, i, h, g, f, e, d, a, b, c}, 32, 3)
        );
    }
}

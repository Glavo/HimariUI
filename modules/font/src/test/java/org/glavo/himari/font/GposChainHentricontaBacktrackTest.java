package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Verifies thirty-one-glyph GPOS chain backtrack through [`SfntFont#chainAdjustment(int[], int, int)`].
@NotNullByDefault
final class GposChainHentricontaBacktrackTest {
    /// Applies the format-1 chain only when backtrack `87654321ZYXWVUTSRQPONMLKJIHGFED` precedes `ABC`.
    @Test
    void format1BacktrackRequiresEightSevenSixFiveFourThreeTwoOneZyxwvutsrqponmlkjihgfed() {
        SfntFont font = GposChainHentricontaBacktrackSampleFont.create();
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
        assertEquals(GposChainHentricontaBacktrackSampleFont.GLYPH_Y, y);
        assertEquals(GposChainHentricontaBacktrackSampleFont.GLYPH_Z, z);
        assertEquals(GposChainHentricontaBacktrackSampleFont.GLYPH_ONE, one);
        assertEquals(GposChainHentricontaBacktrackSampleFont.GLYPH_TWO, two);
        assertEquals(GposChainHentricontaBacktrackSampleFont.GLYPH_THREE, three);
        assertEquals(GposChainHentricontaBacktrackSampleFont.GLYPH_FOUR, four);
        assertEquals(GposChainHentricontaBacktrackSampleFont.GLYPH_FIVE, five);
        assertEquals(GposChainHentricontaBacktrackSampleFont.GLYPH_SIX, six);
        assertEquals(GposChainHentricontaBacktrackSampleFont.GLYPH_SEVEN, seven);
        assertEquals(GposChainHentricontaBacktrackSampleFont.GLYPH_EIGHT, eight);
        assertEquals(0, font.chainAdjustment(a, b, c));
        assertEquals(
                GposChainHentricontaBacktrackSampleFont.CHAIN_DELTA,
                font.chainAdjustment(
                        new int[] {eight, seven, six, five, four, three, two, one, z, y, x, w, v, u, t, s, r, q, p, o, n, m, l, k, j, i, h, g, f, e, d, a, b, c},
                        31,
                        3
                )
        );
        assertEquals(
                0,
                font.chainAdjustment(new int[] {seven, six, five, four, three, two, one, z, y, x, w, v, u, t, s, r, q, p, o, n, m, l, k, j, i, h, g, f, e, d, a, b, c}, 30, 3)
        );
        assertEquals(
                0,
                font.chainAdjustment(new int[] {eight, six, five, four, three, two, one, z, y, x, w, v, u, t, s, r, q, p, o, n, m, l, k, j, i, h, g, f, e, d, a, b, c}, 30, 3)
        );
    }
}

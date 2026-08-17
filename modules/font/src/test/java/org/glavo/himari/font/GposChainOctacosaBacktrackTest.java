package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Verifies twenty-eight-glyph GPOS chain backtrack through [`SfntFont#chainAdjustment(int[], int, int)`].
@NotNullByDefault
final class GposChainOctacosaBacktrackTest {
    /// Applies the format-1 chain only when backtrack `54321ZYXWVUTSRQPONMLKJIHGFED` precedes `ABC`.
    @Test
    void format1BacktrackRequiresFiveFourThreeTwoOneZyxwvutsrqponmlkjihgfed() {
        SfntFont font = GposChainOctacosaBacktrackSampleFont.create();
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
        assertEquals(GposChainOctacosaBacktrackSampleFont.GLYPH_Y, y);
        assertEquals(GposChainOctacosaBacktrackSampleFont.GLYPH_Z, z);
        assertEquals(GposChainOctacosaBacktrackSampleFont.GLYPH_ONE, one);
        assertEquals(GposChainOctacosaBacktrackSampleFont.GLYPH_TWO, two);
        assertEquals(GposChainOctacosaBacktrackSampleFont.GLYPH_THREE, three);
        assertEquals(GposChainOctacosaBacktrackSampleFont.GLYPH_FOUR, four);
        assertEquals(GposChainOctacosaBacktrackSampleFont.GLYPH_FIVE, five);
        assertEquals(0, font.chainAdjustment(a, b, c));
        assertEquals(
                GposChainOctacosaBacktrackSampleFont.CHAIN_DELTA,
                font.chainAdjustment(
                        new int[] {five, four, three, two, one, z, y, x, w, v, u, t, s, r, q, p, o, n, m, l, k, j, i, h, g, f, e, d, a, b, c},
                        28,
                        3
                )
        );
        assertEquals(
                0,
                font.chainAdjustment(new int[] {four, three, two, one, z, y, x, w, v, u, t, s, r, q, p, o, n, m, l, k, j, i, h, g, f, e, d, a, b, c}, 27, 3)
        );
        assertEquals(
                0,
                font.chainAdjustment(new int[] {five, three, two, one, z, y, x, w, v, u, t, s, r, q, p, o, n, m, l, k, j, i, h, g, f, e, d, a, b, c}, 27, 3)
        );
    }
}

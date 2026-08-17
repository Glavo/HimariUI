package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Verifies twenty-four-glyph GPOS chain backtrack through [`SfntFont#chainAdjustment(int[], int, int)`].
@NotNullByDefault
final class GposChainTetracosaBacktrackTest {
    /// Applies the format-1 chain only when backtrack `1ZYXWVUTSRQPONMLKJIHGFED` precedes `ABC`.
    @Test
    void format1BacktrackRequiresOneZyxwvutsrqponmlkjihgfed() {
        SfntFont font = GposChainTetracosaBacktrackSampleFont.create();
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
        assertEquals(GposChainTetracosaBacktrackSampleFont.GLYPH_Y, y);
        assertEquals(GposChainTetracosaBacktrackSampleFont.GLYPH_Z, z);
        assertEquals(GposChainTetracosaBacktrackSampleFont.GLYPH_ONE, one);
        assertEquals(0, font.chainAdjustment(a, b, c));
        assertEquals(
                GposChainTetracosaBacktrackSampleFont.CHAIN_DELTA,
                font.chainAdjustment(
                        new int[] {one, z, y, x, w, v, u, t, s, r, q, p, o, n, m, l, k, j, i, h, g, f, e, d, a, b, c},
                        24,
                        3
                )
        );
        assertEquals(
                0,
                font.chainAdjustment(new int[] {z, y, x, w, v, u, t, s, r, q, p, o, n, m, l, k, j, i, h, g, f, e, d, a, b, c}, 23, 3)
        );
        assertEquals(
                0,
                font.chainAdjustment(new int[] {one, y, x, w, v, u, t, s, r, q, p, o, n, m, l, k, j, i, h, g, f, e, d, a, b, c}, 23, 3)
        );
    }
}

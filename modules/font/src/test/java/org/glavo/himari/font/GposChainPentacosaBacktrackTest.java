package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Verifies twenty-five-glyph GPOS chain backtrack through [`SfntFont#chainAdjustment(int[], int, int)`].
@NotNullByDefault
final class GposChainPentacosaBacktrackTest {
    /// Applies the format-1 chain only when backtrack `21ZYXWVUTSRQPONMLKJIHGFED` precedes `ABC`.
    @Test
    void format1BacktrackRequiresTwoOneZyxwvutsrqponmlkjihgfed() {
        SfntFont font = GposChainPentacosaBacktrackSampleFont.create();
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
        assertEquals(GposChainPentacosaBacktrackSampleFont.GLYPH_Y, y);
        assertEquals(GposChainPentacosaBacktrackSampleFont.GLYPH_Z, z);
        assertEquals(GposChainPentacosaBacktrackSampleFont.GLYPH_ONE, one);
        assertEquals(GposChainPentacosaBacktrackSampleFont.GLYPH_TWO, two);
        assertEquals(0, font.chainAdjustment(a, b, c));
        assertEquals(
                GposChainPentacosaBacktrackSampleFont.CHAIN_DELTA,
                font.chainAdjustment(
                        new int[] {two, one, z, y, x, w, v, u, t, s, r, q, p, o, n, m, l, k, j, i, h, g, f, e, d, a, b, c},
                        25,
                        3
                )
        );
        assertEquals(
                0,
                font.chainAdjustment(new int[] {one, z, y, x, w, v, u, t, s, r, q, p, o, n, m, l, k, j, i, h, g, f, e, d, a, b, c}, 24, 3)
        );
        assertEquals(
                0,
                font.chainAdjustment(new int[] {two, z, y, x, w, v, u, t, s, r, q, p, o, n, m, l, k, j, i, h, g, f, e, d, a, b, c}, 24, 3)
        );
    }
}

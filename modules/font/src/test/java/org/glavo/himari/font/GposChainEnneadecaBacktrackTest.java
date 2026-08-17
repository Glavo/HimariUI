package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Verifies nineteen-glyph GPOS chain backtrack through [`SfntFont#chainAdjustment(int[], int, int)`].
@NotNullByDefault
final class GposChainEnneadecaBacktrackTest {
    /// Applies the format-1 chain only when backtrack `VUTSRQPONMLKJIHGFED` precedes `ABC`.
    @Test
    void format1BacktrackRequiresVutsrqponmlkjihgfed() {
        SfntFont font = GposChainEnneadecaBacktrackSampleFont.create();
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
        assertEquals(GposChainEnneadecaBacktrackSampleFont.GLYPH_V, v);
        assertEquals(0, font.chainAdjustment(a, b, c));
        assertEquals(
                GposChainEnneadecaBacktrackSampleFont.CHAIN_DELTA,
                font.chainAdjustment(
                        new int[] {v, u, t, s, r, q, p, o, n, m, l, k, j, i, h, g, f, e, d, a, b, c},
                        19,
                        3
                )
        );
        assertEquals(
                0,
                font.chainAdjustment(new int[] {u, t, s, r, q, p, o, n, m, l, k, j, i, h, g, f, e, d, a, b, c}, 18, 3)
        );
        assertEquals(
                0,
                font.chainAdjustment(new int[] {v, t, s, r, q, p, o, n, m, l, k, j, i, h, g, f, e, d, a, b, c}, 18, 3)
        );
    }
}

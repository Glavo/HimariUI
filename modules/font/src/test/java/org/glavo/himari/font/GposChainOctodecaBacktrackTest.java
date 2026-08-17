package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Verifies eighteen-glyph GPOS chain backtrack through [`SfntFont#chainAdjustment(int[], int, int)`].
@NotNullByDefault
final class GposChainOctodecaBacktrackTest {
    /// Applies the format-1 chain only when backtrack `UTSRQPONMLKJIHGFED` precedes `ABC`.
    @Test
    void format1BacktrackRequiresUtsrqponmlkjihgfed() {
        SfntFont font = GposChainOctodecaBacktrackSampleFont.create();
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
        assertEquals(GposChainOctodecaBacktrackSampleFont.GLYPH_U, u);
        assertEquals(0, font.chainAdjustment(a, b, c));
        assertEquals(
                GposChainOctodecaBacktrackSampleFont.CHAIN_DELTA,
                font.chainAdjustment(new int[] {u, t, s, r, q, p, o, n, m, l, k, j, i, h, g, f, e, d, a, b, c}, 18, 3)
        );
        assertEquals(0, font.chainAdjustment(new int[] {t, s, r, q, p, o, n, m, l, k, j, i, h, g, f, e, d, a, b, c}, 17, 3));
        assertEquals(0, font.chainAdjustment(new int[] {u, s, r, q, p, o, n, m, l, k, j, i, h, g, f, e, d, a, b, c}, 17, 3));
    }
}

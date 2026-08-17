package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Verifies seventeen-glyph GPOS chain backtrack through [`SfntFont#chainAdjustment(int[], int, int)`].
@NotNullByDefault
final class GposChainHeptadecaBacktrackTest {
    /// Applies the format-1 chain only when backtrack `TSRQPONMLKJIHGFED` precedes `ABC`.
    @Test
    void format1BacktrackRequiresTsrqponmlkjihgfed() {
        SfntFont font = GposChainHeptadecaBacktrackSampleFont.create();
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
        assertEquals(GposChainHeptadecaBacktrackSampleFont.GLYPH_T, t);
        assertEquals(0, font.chainAdjustment(a, b, c));
        assertEquals(
                GposChainHeptadecaBacktrackSampleFont.CHAIN_DELTA,
                font.chainAdjustment(new int[] {t, s, r, q, p, o, n, m, l, k, j, i, h, g, f, e, d, a, b, c}, 17, 3)
        );
        assertEquals(0, font.chainAdjustment(new int[] {s, r, q, p, o, n, m, l, k, j, i, h, g, f, e, d, a, b, c}, 16, 3));
        assertEquals(0, font.chainAdjustment(new int[] {t, r, q, p, o, n, m, l, k, j, i, h, g, f, e, d, a, b, c}, 16, 3));
    }
}

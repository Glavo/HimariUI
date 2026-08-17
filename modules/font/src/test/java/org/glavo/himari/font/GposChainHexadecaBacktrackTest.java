package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Verifies sixteen-glyph GPOS chain backtrack through [`SfntFont#chainAdjustment(int[], int, int)`].
@NotNullByDefault
final class GposChainHexadecaBacktrackTest {
    /// Applies the format-1 chain only when backtrack `SRQPONMLKJIHGFED` precedes `ABC`.
    @Test
    void format1BacktrackRequiresSrqponmlkjihgfed() {
        SfntFont font = GposChainHexadecaBacktrackSampleFont.create();
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
        assertEquals(GposChainHexadecaBacktrackSampleFont.GLYPH_S, s);
        assertEquals(0, font.chainAdjustment(a, b, c));
        assertEquals(
                GposChainHexadecaBacktrackSampleFont.CHAIN_DELTA,
                font.chainAdjustment(new int[] {s, r, q, p, o, n, m, l, k, j, i, h, g, f, e, d, a, b, c}, 16, 3)
        );
        assertEquals(0, font.chainAdjustment(new int[] {r, q, p, o, n, m, l, k, j, i, h, g, f, e, d, a, b, c}, 15, 3));
        assertEquals(0, font.chainAdjustment(new int[] {s, q, p, o, n, m, l, k, j, i, h, g, f, e, d, a, b, c}, 15, 3));
    }
}

package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Verifies thirteen-glyph GPOS chain backtrack through [`SfntFont#chainAdjustment(int[], int, int)`].
@NotNullByDefault
final class GposChainTridecaBacktrackTest {
    /// Applies the format-1 chain only when backtrack `PONMLKJIHGFED` precedes `ABC`.
    @Test
    void format1BacktrackRequiresPonmlkjihgfed() {
        SfntFont font = GposChainTridecaBacktrackSampleFont.create();
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
        assertEquals(GposChainTridecaBacktrackSampleFont.GLYPH_P, p);
        assertEquals(0, font.chainAdjustment(a, b, c));
        assertEquals(
                GposChainTridecaBacktrackSampleFont.CHAIN_DELTA,
                font.chainAdjustment(new int[] {p, o, n, m, l, k, j, i, h, g, f, e, d, a, b, c}, 13, 3)
        );
        assertEquals(0, font.chainAdjustment(new int[] {o, n, m, l, k, j, i, h, g, f, e, d, a, b, c}, 12, 3));
        assertEquals(0, font.chainAdjustment(new int[] {p, n, m, l, k, j, i, h, g, f, e, d, a, b, c}, 12, 3));
    }
}

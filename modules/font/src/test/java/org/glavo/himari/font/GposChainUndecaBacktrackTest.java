package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Verifies eleven-glyph GPOS chain backtrack through [`SfntFont#chainAdjustment(int[], int, int)`].
@NotNullByDefault
final class GposChainUndecaBacktrackTest {
    /// Applies the format-1 chain only when backtrack `NMLKJIHGFED` precedes `ABC`.
    @Test
    void format1BacktrackRequiresNmlkjihgfed() {
        SfntFont font = GposChainUndecaBacktrackSampleFont.create();
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
        assertEquals(GposChainUndecaBacktrackSampleFont.GLYPH_N, n);
        assertEquals(0, font.chainAdjustment(a, b, c));
        assertEquals(
                GposChainUndecaBacktrackSampleFont.CHAIN_DELTA,
                font.chainAdjustment(new int[] {n, m, l, k, j, i, h, g, f, e, d, a, b, c}, 11, 3)
        );
        assertEquals(0, font.chainAdjustment(new int[] {m, l, k, j, i, h, g, f, e, d, a, b, c}, 10, 3));
        assertEquals(0, font.chainAdjustment(new int[] {n, l, k, j, i, h, g, f, e, d, a, b, c}, 10, 3));
    }
}

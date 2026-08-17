package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Verifies twelve-glyph GPOS chain backtrack through [`SfntFont#chainAdjustment(int[], int, int)`].
@NotNullByDefault
final class GposChainDodecaBacktrackTest {
    /// Applies the format-1 chain only when backtrack `ONMLKJIHGFED` precedes `ABC`.
    @Test
    void format1BacktrackRequiresOnmlkjihgfed() {
        SfntFont font = GposChainDodecaBacktrackSampleFont.create();
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
        assertEquals(GposChainDodecaBacktrackSampleFont.GLYPH_O, o);
        assertEquals(0, font.chainAdjustment(a, b, c));
        assertEquals(
                GposChainDodecaBacktrackSampleFont.CHAIN_DELTA,
                font.chainAdjustment(new int[] {o, n, m, l, k, j, i, h, g, f, e, d, a, b, c}, 12, 3)
        );
        assertEquals(0, font.chainAdjustment(new int[] {n, m, l, k, j, i, h, g, f, e, d, a, b, c}, 11, 3));
        assertEquals(0, font.chainAdjustment(new int[] {o, m, l, k, j, i, h, g, f, e, d, a, b, c}, 11, 3));
    }
}

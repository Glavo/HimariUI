package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Verifies nine-glyph GPOS chain backtrack through [`SfntFont#chainAdjustment(int[], int, int)`].
@NotNullByDefault
final class GposChainNonaBacktrackTest {
    /// Applies the format-1 chain only when backtrack `LKJIHGFED` precedes `ABC`.
    @Test
    void format1BacktrackRequiresLkjihgfed() {
        SfntFont font = GposChainNonaBacktrackSampleFont.create();
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
        assertEquals(GposChainNonaBacktrackSampleFont.GLYPH_L, l);
        assertEquals(0, font.chainAdjustment(a, b, c));
        assertEquals(
                GposChainNonaBacktrackSampleFont.CHAIN_DELTA,
                font.chainAdjustment(new int[] {l, k, j, i, h, g, f, e, d, a, b, c}, 9, 3)
        );
        assertEquals(0, font.chainAdjustment(new int[] {k, j, i, h, g, f, e, d, a, b, c}, 8, 3));
        assertEquals(0, font.chainAdjustment(new int[] {l, j, i, h, g, f, e, d, a, b, c}, 8, 3));
    }
}

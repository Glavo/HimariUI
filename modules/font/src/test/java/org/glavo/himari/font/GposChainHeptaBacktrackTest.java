package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Verifies seven-glyph GPOS chain backtrack through [`SfntFont#chainAdjustment(int[], int, int)`].
@NotNullByDefault
final class GposChainHeptaBacktrackTest {
    /// Applies the format-1 chain only when backtrack `JIHGFED` precedes `ABC`.
    @Test
    void format1BacktrackRequiresJihgfed() {
        SfntFont font = GposChainHeptaBacktrackSampleFont.create();
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
        assertEquals(GposChainHeptaBacktrackSampleFont.GLYPH_J, j);
        assertEquals(0, font.chainAdjustment(a, b, c));
        assertEquals(
                GposChainHeptaBacktrackSampleFont.CHAIN_DELTA,
                font.chainAdjustment(new int[] {j, i, h, g, f, e, d, a, b, c}, 7, 3)
        );
        assertEquals(0, font.chainAdjustment(new int[] {i, h, g, f, e, d, a, b, c}, 6, 3));
        assertEquals(0, font.chainAdjustment(new int[] {j, h, g, f, e, d, a, b, c}, 6, 3));
    }
}

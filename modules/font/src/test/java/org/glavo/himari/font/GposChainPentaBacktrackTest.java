package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Verifies five-glyph GPOS chain backtrack through [`SfntFont#chainAdjustment(int[], int, int)`].
@NotNullByDefault
final class GposChainPentaBacktrackTest {
    /// Applies the format-1 chain only when backtrack `HGFED` precedes `ABC`.
    @Test
    void format1BacktrackRequiresHgfed() {
        SfntFont font = GposChainPentaBacktrackSampleFont.create();
        int a = font.glyphId('A');
        int b = font.glyphId('B');
        int c = font.glyphId('C');
        int d = font.glyphId('D');
        int e = font.glyphId('E');
        int f = font.glyphId('F');
        int g = font.glyphId('G');
        int h = font.glyphId('H');
        assertEquals(GposChainPentaBacktrackSampleFont.GLYPH_H, h);
        assertEquals(0, font.chainAdjustment(a, b, c));
        assertEquals(
                GposChainPentaBacktrackSampleFont.CHAIN_DELTA,
                font.chainAdjustment(new int[] {h, g, f, e, d, a, b, c}, 5, 3)
        );
        assertEquals(0, font.chainAdjustment(new int[] {g, f, e, d, a, b, c}, 4, 3));
        assertEquals(0, font.chainAdjustment(new int[] {h, f, e, d, a, b, c}, 4, 3));
    }
}

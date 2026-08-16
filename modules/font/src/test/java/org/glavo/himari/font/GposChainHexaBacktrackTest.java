package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Verifies six-glyph GPOS chain backtrack through [`SfntFont#chainAdjustment(int[], int, int)`].
@NotNullByDefault
final class GposChainHexaBacktrackTest {
    /// Applies the format-1 chain only when backtrack `IHGFED` precedes `ABC`.
    @Test
    void format1BacktrackRequiresIhgfed() {
        SfntFont font = GposChainHexaBacktrackSampleFont.create();
        int a = font.glyphId('A');
        int b = font.glyphId('B');
        int c = font.glyphId('C');
        int d = font.glyphId('D');
        int e = font.glyphId('E');
        int f = font.glyphId('F');
        int g = font.glyphId('G');
        int h = font.glyphId('H');
        int i = font.glyphId('I');
        assertEquals(GposChainHexaBacktrackSampleFont.GLYPH_I, i);
        assertEquals(0, font.chainAdjustment(a, b, c));
        assertEquals(
                GposChainHexaBacktrackSampleFont.CHAIN_DELTA,
                font.chainAdjustment(new int[] {i, h, g, f, e, d, a, b, c}, 6, 3)
        );
        assertEquals(0, font.chainAdjustment(new int[] {h, g, f, e, d, a, b, c}, 5, 3));
        assertEquals(0, font.chainAdjustment(new int[] {i, g, f, e, d, a, b, c}, 5, 3));
    }
}

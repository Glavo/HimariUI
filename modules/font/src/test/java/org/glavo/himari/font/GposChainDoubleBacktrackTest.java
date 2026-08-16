package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Verifies two-glyph GPOS chain backtrack through [`SfntFont#chainAdjustment(int[], int, int)`].
@NotNullByDefault
final class GposChainDoubleBacktrackTest {
    /// Applies the class chain only when backtrack `ED` precedes `ABC`.
    @Test
    void format2ClassBacktrackRequiresEd() {
        SfntFont font = GposChainDoubleBacktrackSampleFont.create();
        int a = font.glyphId('A');
        int b = font.glyphId('B');
        int c = font.glyphId('C');
        int d = font.glyphId('D');
        int e = font.glyphId('E');
        assertEquals(0, font.chainAdjustment(a, b, c));
        assertEquals(
                GposChainDoubleBacktrackSampleFont.CHAIN_DELTA,
                font.chainAdjustment(new int[] {e, d, a, b, c}, 2, 3)
        );
        assertEquals(0, font.chainAdjustment(new int[] {d, a, b, c}, 1, 3));
        assertEquals(0, font.chainAdjustment(new int[] {e, a, b, c}, 1, 3));
    }
}

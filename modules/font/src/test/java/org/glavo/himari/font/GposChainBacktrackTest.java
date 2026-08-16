package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Verifies GPOS type-8 format-2 class backtrack through [`SfntFont#chainAdjustment(int[], int, int)`].
@NotNullByDefault
final class GposChainBacktrackTest {
    /// Applies the class chain only when backtrack `D` precedes `ABC`.
    @Test
    void format2ClassBacktrackRequiresPrecedingD() {
        SfntFont font = GposChainBacktrackSampleFont.create();
        int a = font.glyphId('A');
        int b = font.glyphId('B');
        int c = font.glyphId('C');
        int d = font.glyphId('D');
        assertEquals(0, font.chainAdjustment(a, b, c));
        assertEquals(
                GposChainBacktrackSampleFont.CHAIN_DELTA,
                font.chainAdjustment(new int[] {d, a, b, c}, 1, 3)
        );
        assertEquals(0, font.chainAdjustment(new int[] {a, b, c}, 0, 3));
        assertEquals(0, font.chainAdjustment(new int[] {b, a, b, c}, 1, 3));
    }
}

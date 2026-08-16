package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Verifies three-glyph GPOS chain backtrack through [`SfntFont#chainAdjustment(int[], int, int)`].
@NotNullByDefault
final class GposChainTripleBacktrackTest {
    /// Applies the class chain only when backtrack `FED` precedes `ABC`.
    @Test
    void format2ClassBacktrackRequiresFed() {
        SfntFont font = GposChainTripleBacktrackSampleFont.create();
        int a = font.glyphId('A');
        int b = font.glyphId('B');
        int c = font.glyphId('C');
        int d = font.glyphId('D');
        int e = font.glyphId('E');
        int f = font.glyphId('F');
        assertEquals(0, font.chainAdjustment(a, b, c));
        assertEquals(
                GposChainTripleBacktrackSampleFont.CHAIN_DELTA,
                font.chainAdjustment(new int[] {f, e, d, a, b, c}, 3, 3)
        );
        assertEquals(0, font.chainAdjustment(new int[] {e, d, a, b, c}, 2, 3));
        assertEquals(0, font.chainAdjustment(new int[] {f, d, a, b, c}, 2, 3));
    }
}

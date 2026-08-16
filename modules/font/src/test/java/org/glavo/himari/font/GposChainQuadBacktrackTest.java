package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Verifies four-glyph GPOS chain backtrack through [`SfntFont#chainAdjustment(int[], int, int)`].
@NotNullByDefault
final class GposChainQuadBacktrackTest {
    /// Applies the format-1 chain only when backtrack `GFED` precedes `ABC`.
    @Test
    void format2ClassBacktrackRequiresGfed() {
        SfntFont font = GposChainQuadBacktrackSampleFont.create();
        int a = font.glyphId('A');
        int b = font.glyphId('B');
        int c = font.glyphId('C');
        int d = font.glyphId('D');
        int e = font.glyphId('E');
        int f = font.glyphId('F');
        int g = font.glyphId('G');
        assertEquals(GposChainQuadBacktrackSampleFont.GLYPH_A, a, "A");
        assertEquals(GposChainQuadBacktrackSampleFont.GLYPH_B, b, "B");
        assertEquals(GposChainQuadBacktrackSampleFont.GLYPH_C, c, "C");
        assertEquals(GposChainQuadBacktrackSampleFont.GLYPH_D, d, "D");
        assertEquals(GposChainQuadBacktrackSampleFont.GLYPH_E, e, "E");
        assertEquals(GposChainQuadBacktrackSampleFont.GLYPH_F, f, "F");
        assertEquals(GposChainQuadBacktrackSampleFont.GLYPH_G, g, "G");
        assertEquals(0, font.chainAdjustment(a, b, c));
        assertEquals(
                GposChainQuadBacktrackSampleFont.CHAIN_DELTA,
                font.chainAdjustment(new int[] {g, f, e, d, a, b, c}, 4, 3)
        );
        assertEquals(0, font.chainAdjustment(new int[] {f, e, d, a, b, c}, 3, 3));
        assertEquals(0, font.chainAdjustment(new int[] {g, e, d, a, b, c}, 3, 3));
    }
}

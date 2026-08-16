package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Verifies GPOS type-3 cursive X-advance through [`SfntFont#pairAdjustment(int, int)`].
@NotNullByDefault
final class GposCursiveTest {
    /// Connects `A` exit to `B` entry and leaves the reverse pair at zero.
    @Test
    void appliesExitToEntryDeltaOnAbPair() {
        SfntFont font = GposCursiveSampleFont.create();
        int a = font.glyphId('A');
        int b = font.glyphId('B');
        assertEquals(GposCursiveSampleFont.GLYPH_A, a);
        assertEquals(GposCursiveSampleFont.GLYPH_B, b);
        assertEquals(GposCursiveSampleFont.CURSIVE_DELTA, font.pairAdjustment(a, b));
        assertEquals(0, font.pairAdjustment(b, a));
        assertEquals(0, font.pairAdjustment(a, a));
    }
}

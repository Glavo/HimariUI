package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Verifies GPOS type-2 format-2 class pairs through [`SfntFont#pairAdjustment(int, int)`].
@NotNullByDefault
final class GposPairClassTest {
    /// Applies the class-1/`class-1` cell to `AC` and leaves `AB` at zero.
    @Test
    void format2ClassMatrixAppliesAcOnly() {
        SfntFont font = GposPairClassSampleFont.create();
        int a = font.glyphId('A');
        int b = font.glyphId('B');
        int c = font.glyphId('C');
        assertEquals(GposPairClassSampleFont.PAIR_DELTA, font.pairAdjustment(a, c));
        assertEquals(0, font.pairAdjustment(a, b));
        assertEquals(0, font.pairAdjustment(c, a));
    }
}

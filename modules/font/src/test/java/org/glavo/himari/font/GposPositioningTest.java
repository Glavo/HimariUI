package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Verifies GPOS type-2 pair X-advance on the constructed kern font.
@NotNullByDefault
final class GposPositioningTest {
    /// Tightens `AV` and leaves other pairs unchanged.
    @Test
    void kernsAvPair() {
        SfntFont font = GposSampleFont.create();
        int a = font.glyphId('A');
        int v = font.glyphId('V');
        assertEquals(GposSampleFont.GLYPH_A, a);
        assertEquals(GposSampleFont.GLYPH_V, v);
        assertEquals(GposSampleFont.ADVANCE_LETTER, font.metrics(a).advanceWidth());
        assertEquals(GposSampleFont.KERN_AV, font.pairAdjustment(a, v));
        assertEquals(0, font.pairAdjustment(v, a));
        assertEquals(0, font.pairAdjustment(a, a));
    }

    /// Leaves fonts without GPOS or `kern` at zero adjustment.
    @Test
    void missingGposIsZero() {
        SfntFont font = OutlineSampleFont.create();
        int glyph = font.glyphId('A');
        assertEquals(0, font.pairAdjustment(glyph, glyph));
    }
}

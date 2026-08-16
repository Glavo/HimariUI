package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Verifies GPOS type-7/8 format-2 class rules through [`SfntFont`] entries.
@NotNullByDefault
final class GposClassContextTest {
    /// Applies a format-2 class context `AB` pair.
    @Test
    void format2ContextAppliesAb() {
        SfntFont font = GposContextClassSampleFont.create();
        int a = font.glyphId('A');
        int b = font.glyphId('B');
        assertEquals(GposContextClassSampleFont.CONTEXT_DELTA, font.pairAdjustment(a, b));
        assertEquals(0, font.pairAdjustment(b, a));
    }

    /// Applies a format-2 class chain `ABC` triple.
    @Test
    void format2ChainAppliesAbc() {
        SfntFont font = GposChainClassSampleFont.create();
        int a = font.glyphId('A');
        int b = font.glyphId('B');
        int c = font.glyphId('C');
        assertEquals(GposChainClassSampleFont.CHAIN_DELTA, font.chainAdjustment(a, b, c));
        assertEquals(0, font.chainAdjustment(a, b, a));
        assertEquals(0, font.pairAdjustment(a, b));
    }
}

package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Verifies GPOS type-7/8 format-3 coverage rules through [`SfntFont`] entries.
@NotNullByDefault
final class GposCoverageTest {
    /// Applies format-3 context `AB` and format-3 chain `ABC`.
    @Test
    void format3ContextAndChainApplyDistinctDeltas() {
        SfntFont font = GposCoverageSampleFont.create();
        int a = font.glyphId('A');
        int b = font.glyphId('B');
        int c = font.glyphId('C');
        assertEquals(GposCoverageSampleFont.CONTEXT_DELTA, font.pairAdjustment(a, b));
        assertEquals(0, font.pairAdjustment(a, c));
        assertEquals(GposCoverageSampleFont.CHAIN_DELTA, font.chainAdjustment(a, b, c));
        assertEquals(0, font.chainAdjustment(a, b, a));
    }
}

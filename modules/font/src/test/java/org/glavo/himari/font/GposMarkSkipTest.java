package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/// Verifies GPOS `IgnoreMarks` pair and chain maps through [`SfntFont`] entries.
@NotNullByDefault
final class GposMarkSkipTest {
    /// Stores `AC` and `ACD` under skip maps and leaves adjacent maps empty.
    @Test
    void ignoreMarksPairAndChainSkipGdefMark() {
        SfntFont font = GposMarkSkipSampleFont.create();
        int a = font.glyphId('A');
        int b = font.glyphId('B');
        int c = font.glyphId('C');
        int d = font.glyphId('D');
        assertEquals(GdefTable.CLASS_MARK, font.glyphClass(b));
        assertEquals(0, font.pairAdjustment(a, c));
        assertEquals(0, font.pairAdjustment(a, b));
        assertEquals(GposMarkSkipSampleFont.PAIR_DELTA, font.skipPairAdjustment(a, c));
        assertEquals(0, font.skipPairAdjustment(a, b));
        assertEquals(0, font.chainAdjustment(a, c, d));
        assertEquals(GposMarkSkipSampleFont.CHAIN_DELTA, font.skipChainAdjustment(a, c, d));
        assertEquals(0, font.skipChainAdjustment(a, b, c));
        assertArrayEquals(new int[0], font.markAttachmentTypes());
    }
}

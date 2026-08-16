package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/// Verifies GPOS `MarkAttachmentType` pair maps through [`SfntFont`] entries.
@NotNullByDefault
final class GposMarkAttachTest {
    /// Stores `AC` under attach class 1 and classifies `B` versus `D`.
    @Test
    void markAttachmentTypeSkipsOtherMarkClass() {
        SfntFont font = GposMarkAttachSampleFont.create();
        int a = font.glyphId('A');
        int b = font.glyphId('B');
        int c = font.glyphId('C');
        int d = font.glyphId('D');
        assertEquals(GdefTable.CLASS_MARK, font.glyphClass(b));
        assertEquals(GdefTable.CLASS_MARK, font.glyphClass(d));
        assertEquals(2, font.markAttachClass(b));
        assertEquals(GposMarkAttachSampleFont.ATTACH_TYPE, font.markAttachClass(d));
        assertEquals(0, font.pairAdjustment(a, c));
        assertEquals(0, font.skipPairAdjustment(a, c));
        assertEquals(
                GposMarkAttachSampleFont.ATTACH_DELTA,
                font.attachPairAdjustment(a, c, GposMarkAttachSampleFont.ATTACH_TYPE)
        );
        assertEquals(0, font.attachPairAdjustment(a, c, 2));
        assertEquals(0, font.attachPairAdjustment(a, d, GposMarkAttachSampleFont.ATTACH_TYPE));
        assertArrayEquals(new int[] {GposMarkAttachSampleFont.ATTACH_TYPE}, font.markAttachmentTypes());
    }
}

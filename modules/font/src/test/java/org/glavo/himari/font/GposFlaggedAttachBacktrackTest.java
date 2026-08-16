package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Verifies flagged and attach type-8 backtrack through [`SfntFont#chainAdjustment(int[], int, int)`].
@NotNullByDefault
final class GposFlaggedAttachBacktrackTest {
    /// Requires preceding `D` while `IgnoreBaseGlyphs` skips `B`.
    @Test
    void ignoreBaseChainRequiresBacktrackD() {
        SfntFont font = GposFlaggedChainBacktrackSampleFont.create();
        int a = font.glyphId('A');
        int b = font.glyphId('B');
        int c = font.glyphId('C');
        int d = font.glyphId('D');
        int e = font.glyphId('E');
        assertEquals(
                GposFlaggedChainBacktrackSampleFont.CHAIN_DELTA,
                font.chainAdjustment(new int[] {d, a, b, c, e}, 1, 4)
        );
        assertEquals(0, font.chainAdjustment(new int[] {a, b, c, e}, 0, 4));
        assertEquals(0, font.chainAdjustment(new int[] {b, a, b, c, e}, 1, 4));
    }

    /// Requires preceding `D` while `MarkAttachmentType` skips mark `B`.
    @Test
    void attachChainRequiresBacktrackD() {
        SfntFont font = GposAttachChainBacktrackSampleFont.create();
        int a = font.glyphId('A');
        int b = font.glyphId('B');
        int c = font.glyphId('C');
        int d = font.glyphId('D');
        int e = font.glyphId('E');
        assertEquals(
                GposAttachChainBacktrackSampleFont.CHAIN_DELTA,
                font.chainAdjustment(new int[] {d, a, b, c, e}, 1, 4)
        );
        assertEquals(0, font.chainAdjustment(new int[] {a, b, c, e}, 0, 4));
        assertEquals(
                0,
                font.attachChainAdjustment(
                        a,
                        c,
                        e,
                        GposAttachChainBacktrackSampleFont.ATTACH_TYPE
                )
        );
        assertEquals(
                GposAttachChainBacktrackSampleFont.CHAIN_DELTA,
                font.attachChainAdjustment(
                        a,
                        c,
                        e,
                        GposAttachChainBacktrackSampleFont.ATTACH_TYPE,
                        d,
                        0
                )
        );
    }
}

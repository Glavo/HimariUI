package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Verifies `VVAR` height deltas through [`SfntFont#metrics(int, float[])`].
@NotNullByDefault
final class VvarTableTest {
    /// Default-instance metrics keep the stored `vmtx` height and `hmtx` width.
    @Test
    void defaultInstanceKeepsStoredHeightAndWidth() {
        SfntFont font = VvarSampleFont.create();
        GlyphMetrics metrics = font.metrics(VvarSampleFont.GLYPH_A);
        assertEquals(VvarSampleFont.DEFAULT_ADVANCE, metrics.advanceWidth());
        assertEquals(VvarSampleFont.DEFAULT_HEIGHT, metrics.advanceHeight());
    }

    /// Peak weight adds the stored `VVAR` height without changing the width.
    @Test
    void peakInstanceAppliesHeightDelta() {
        SfntFont font = VvarSampleFont.create();
        GlyphMetrics metrics = font.metrics(VvarSampleFont.GLYPH_A, new float[] {VvarSampleFont.MAX_WEIGHT});
        assertEquals(VvarSampleFont.DEFAULT_ADVANCE, metrics.advanceWidth());
        assertEquals(VvarSampleFont.DEFAULT_HEIGHT + VvarSampleFont.HEIGHT_DELTA, metrics.advanceHeight());
    }
}

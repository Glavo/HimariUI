package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Verifies `HVAR` advance deltas through [`SfntFont#metrics(int, float[])`].
@NotNullByDefault
final class HvarTableTest {
    /// Default-instance metrics keep the stored `hmtx` advance and outline.
    @Test
    void defaultInstanceKeepsStoredAdvanceAndOutline() {
        SfntFont font = HvarSampleFont.create();
        assertEquals(HvarSampleFont.DEFAULT_ADVANCE, font.metrics(HvarSampleFont.GLYPH_A).advanceWidth());
        assertEquals(
                HvarSampleFont.DEFAULT_ADVANCE,
                font.metrics(HvarSampleFont.GLYPH_A, font.defaultVariation()).advanceWidth()
        );
        CollectingPen pen = new CollectingPen();
        font.outline(HvarSampleFont.GLYPH_A, pen, new float[] {HvarSampleFont.MAX_WEIGHT});
        assertEquals(0.0f, pen.commands().getFirst().x0(), 0.01f);
    }

    /// Peak weight adds the stored `HVAR` advance without moving the outline.
    @Test
    void peakInstanceAppliesAdvanceDelta() {
        SfntFont font = HvarSampleFont.create();
        assertEquals(
                HvarSampleFont.DEFAULT_ADVANCE + HvarSampleFont.ADVANCE_DELTA,
                font.metrics(HvarSampleFont.GLYPH_A, new float[] {HvarSampleFont.MAX_WEIGHT}).advanceWidth()
        );
    }

    /// Mid-axis weight scales the `HVAR` delta by one half.
    @Test
    void midInstanceScalesAdvanceDelta() {
        SfntFont font = HvarSampleFont.create();
        float mid = (HvarSampleFont.DEFAULT_WEIGHT + HvarSampleFont.MAX_WEIGHT) * 0.5f;
        assertEquals(
                HvarSampleFont.DEFAULT_ADVANCE + HvarSampleFont.ADVANCE_DELTA / 2,
                font.metrics(HvarSampleFont.GLYPH_A, new float[] {mid}).advanceWidth()
        );
    }
}

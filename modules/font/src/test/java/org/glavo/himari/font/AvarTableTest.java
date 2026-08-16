package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Verifies `avar` remapping through [`SfntFont#outline(int, OutlinePen, float[])`] and
/// [`SfntFont#metrics(int, float[])`].
@NotNullByDefault
final class AvarTableTest {
    /// Mid-axis weight without `avar` still scales gvar by one half.
    @Test
    void gvarWithoutAvarKeepsLinearMidInstance() {
        SfntFont font = GvarSampleFont.create();
        CollectingPen pen = new CollectingPen();
        font.outline(GvarSampleFont.GLYPH_A, pen, new float[] {AvarSampleFont.MID_WEIGHT});
        assertEquals(GvarSampleFont.CONTOUR_X_DELTA * 0.5f, pen.commands().getFirst().x0(), 0.01f);
        assertEquals(
                GvarSampleFont.DEFAULT_ADVANCE + GvarSampleFont.ADVANCE_PHANTOM_DELTA / 2,
                font.metrics(GvarSampleFont.GLYPH_A, new float[] {AvarSampleFont.MID_WEIGHT}).advanceWidth()
        );
    }

    /// The constructed `avar` map sends mid-axis `0.5` to the peak tuple.
    @Test
    void avarMapsMidInstanceToPeakDeltas() {
        SfntFont font = AvarSampleFont.create();
        CollectingPen mid = new CollectingPen();
        font.outline(GvarSampleFont.GLYPH_A, mid, new float[] {AvarSampleFont.MID_WEIGHT});
        assertEquals(GvarSampleFont.CONTOUR_X_DELTA, mid.commands().getFirst().x0(), 0.01f);
        assertEquals(
                GvarSampleFont.DEFAULT_ADVANCE + GvarSampleFont.ADVANCE_PHANTOM_DELTA,
                font.metrics(GvarSampleFont.GLYPH_A, new float[] {AvarSampleFont.MID_WEIGHT}).advanceWidth()
        );
        CollectingPen def = new CollectingPen();
        font.outline(GvarSampleFont.GLYPH_A, def);
        assertEquals(0.0f, def.commands().getFirst().x0(), 0.01f);
        CollectingPen peak = new CollectingPen();
        font.outline(GvarSampleFont.GLYPH_A, peak, new float[] {GvarSampleFont.MAX_WEIGHT});
        assertEquals(GvarSampleFont.CONTOUR_X_DELTA, peak.commands().getFirst().x0(), 0.01f);
    }
}

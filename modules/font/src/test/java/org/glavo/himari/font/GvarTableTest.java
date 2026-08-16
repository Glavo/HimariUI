package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Verifies `gvar` contour and phantom deltas through [`SfntFont#outline(int, OutlinePen, float[])`]
/// and [`SfntFont#metrics(int, float[])`].
@NotNullByDefault
final class GvarTableTest {
    /// Default-instance outlines and metrics match the stored `glyf`/`hmtx`.
    @Test
    void defaultInstanceKeepsStoredOutlineAndAdvance() {
        SfntFont font = GvarSampleFont.create();
        CollectingPen pen = new CollectingPen();
        font.outline(GvarSampleFont.GLYPH_A, pen);
        assertRectangle(pen.commands(), 0.0f);
        assertEquals(GvarSampleFont.DEFAULT_ADVANCE, font.metrics(GvarSampleFont.GLYPH_A).advanceWidth());
        assertEquals(0, font.metrics(GvarSampleFont.GLYPH_A).leftSideBearing());
        CollectingPen explicitDefault = new CollectingPen();
        font.outline(GvarSampleFont.GLYPH_A, explicitDefault, font.defaultVariation());
        assertRectangle(explicitDefault.commands(), 0.0f);
        assertEquals(
                GvarSampleFont.DEFAULT_ADVANCE,
                font.metrics(GvarSampleFont.GLYPH_A, font.defaultVariation()).advanceWidth()
        );
    }

    /// Peak weight moves every contour point by the stored X delta and grows the advance phantom.
    @Test
    void peakInstanceAppliesContourAndPhantomDeltas() {
        SfntFont font = GvarSampleFont.create();
        CollectingPen pen = new CollectingPen();
        font.outline(GvarSampleFont.GLYPH_A, pen, new float[] {GvarSampleFont.MAX_WEIGHT});
        assertRectangle(pen.commands(), GvarSampleFont.CONTOUR_X_DELTA);
        GlyphMetrics metrics = font.metrics(GvarSampleFont.GLYPH_A, new float[] {GvarSampleFont.MAX_WEIGHT});
        assertEquals(
                GvarSampleFont.DEFAULT_ADVANCE + GvarSampleFont.ADVANCE_PHANTOM_DELTA,
                metrics.advanceWidth()
        );
        assertEquals(0, metrics.leftSideBearing());
    }

    /// Mid-axis weight scales both the outline and the advance phantom by one half.
    @Test
    void midInstanceScalesDeltasByTupleScalar() {
        SfntFont font = GvarSampleFont.create();
        float mid = (GvarSampleFont.DEFAULT_WEIGHT + GvarSampleFont.MAX_WEIGHT) * 0.5f;
        CollectingPen pen = new CollectingPen();
        font.outline(GvarSampleFont.GLYPH_A, pen, new float[] {mid});
        assertRectangle(pen.commands(), GvarSampleFont.CONTOUR_X_DELTA * 0.5f);
        assertEquals(
                GvarSampleFont.DEFAULT_ADVANCE + GvarSampleFont.ADVANCE_PHANTOM_DELTA / 2,
                font.metrics(GvarSampleFont.GLYPH_A, new float[] {mid}).advanceWidth()
        );
    }

    /// Asserts the constructed rectangle after a uniform X translation.
    private static void assertRectangle(List<OutlineOp> commands, float dx) {
        assertEquals(5, commands.size());
        assertEquals(OutlineVerb.MOVE, commands.get(0).verb());
        assertEquals(dx, commands.get(0).x0(), 0.01f);
        assertEquals(0.0f, commands.get(0).y0(), 0.01f);
        assertEquals(OutlineVerb.LINE, commands.get(1).verb());
        assertEquals(5.0f + dx, commands.get(1).x0(), 0.01f);
        assertEquals(0.0f, commands.get(1).y0(), 0.01f);
        assertEquals(OutlineVerb.LINE, commands.get(2).verb());
        assertEquals(5.0f + dx, commands.get(2).x0(), 0.01f);
        assertEquals(7.0f, commands.get(2).y0(), 0.01f);
        assertEquals(OutlineVerb.LINE, commands.get(3).verb());
        assertEquals(dx, commands.get(3).x0(), 0.01f);
        assertEquals(7.0f, commands.get(3).y0(), 0.01f);
        assertEquals(OutlineVerb.CLOSE, commands.get(4).verb());
    }
}

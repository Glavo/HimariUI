package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies CFF 1 and CFF2 Type 2 outlines through [`SfntFont#outline(int, OutlinePen)`].
@NotNullByDefault
final class CffOutlinesTest {
    /// Walks the CFF 1 rectangle as move, three lines, and close.
    @Test
    void walksCff1Rectangle() {
        SfntFont font = CffSampleFont.create();
        assertFalse(font.hasTrueTypeOutlines());
        assertFalse(font.hasCff2Outlines());
        assertEquals(CffSampleFont.GLYPH_A, font.glyphId('A'));
        CollectingPen pen = new CollectingPen();
        font.outline(CffSampleFont.GLYPH_A, pen);
        List<OutlineOp> commands = pen.commands();
        assertEquals(5, commands.size());
        assertEquals(OutlineVerb.MOVE, commands.get(0).verb());
        assertEquals(0.0f, commands.get(0).x0(), 0.01f);
        assertEquals(0.0f, commands.get(0).y0(), 0.01f);
        assertEquals(OutlineVerb.LINE, commands.get(1).verb());
        assertEquals(5.0f, commands.get(1).x0(), 0.01f);
        assertEquals(0.0f, commands.get(1).y0(), 0.01f);
        assertEquals(OutlineVerb.LINE, commands.get(2).verb());
        assertEquals(5.0f, commands.get(2).x0(), 0.01f);
        assertEquals(7.0f, commands.get(2).y0(), 0.01f);
        assertEquals(OutlineVerb.LINE, commands.get(3).verb());
        assertEquals(0.0f, commands.get(3).x0(), 0.01f);
        assertEquals(7.0f, commands.get(3).y0(), 0.01f);
        assertEquals(OutlineVerb.CLOSE, commands.get(4).verb());
    }

    /// Walks the CFF2 cubic through the shipped outline entry.
    @Test
    void walksCff2Cubic() {
        SfntFont font = Cff2SampleFont.create();
        assertFalse(font.hasTrueTypeOutlines());
        assertTrue(font.hasCff2Outlines());
        CollectingPen pen = new CollectingPen();
        font.outline(CffSampleFont.GLYPH_A, pen);
        List<OutlineOp> commands = pen.commands();
        assertEquals(3, commands.size());
        assertEquals(OutlineVerb.MOVE, commands.get(0).verb());
        assertEquals(0.0f, commands.get(0).x0(), 0.01f);
        assertEquals(OutlineVerb.CUBIC, commands.get(1).verb());
        assertEquals(Cff2SampleFont.C1X, commands.get(1).x0(), 0.01f);
        assertEquals(Cff2SampleFont.C1Y, commands.get(1).y0(), 0.01f);
        assertEquals(Cff2SampleFont.C2X, commands.get(1).x1(), 0.01f);
        assertEquals(Cff2SampleFont.C2Y, commands.get(1).y1(), 0.01f);
        assertEquals(Cff2SampleFont.X, commands.get(1).x2(), 0.01f);
        assertEquals(Cff2SampleFont.Y, commands.get(1).y2(), 0.01f);
        assertEquals(OutlineVerb.CLOSE, commands.get(2).verb());
    }

    /// Rasters the CFF 1 rectangle through the shipped grayscale path.
    @Test
    void rastersCff1Rectangle() {
        SfntFont font = CffSampleFont.create();
        GlyphMask mask = GlyphRasterizer.rasterize(font, CffSampleFont.GLYPH_A, 16);
        assertTrue(mask.width() > 0);
        assertTrue(mask.height() > 0);
        int covered = 0;
        for (byte sample : mask.coverage()) {
            if (sample != 0) {
                covered++;
            }
        }
        assertTrue(covered > 0);
    }
}

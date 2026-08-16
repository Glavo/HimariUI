package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies simple, implied-on-curve, and composite outline emission.
@NotNullByDefault
final class OutlineWalkerTest {
    /// Emits a quadratic for the constructed bump instead of three polyline edges.
    @Test
    void walksQuadraticBumpAsCurve() {
        SfntFont font = OutlineSampleFont.create();
        CollectingPen pen = new CollectingPen();
        font.outline(OutlineSampleFont.GLYPH_BUMP, pen);
        List<OutlineOp> commands = pen.commands();
        assertEquals(3, commands.size());
        assertEquals(OutlineVerb.MOVE, commands.get(0).verb());
        assertEquals(0.0f, commands.get(0).x0(), 0.01f);
        assertEquals(0.0f, commands.get(0).y0(), 0.01f);
        assertEquals(OutlineVerb.QUAD, commands.get(1).verb());
        assertEquals(50.0f, commands.get(1).x0(), 0.01f);
        assertEquals(100.0f, commands.get(1).y0(), 0.01f);
        assertEquals(100.0f, commands.get(1).x1(), 0.01f);
        assertEquals(0.0f, commands.get(1).y1(), 0.01f);
        assertEquals(OutlineVerb.CLOSE, commands.get(2).verb());
    }

    /// Inserts the implied on-curve midpoint between two consecutive off-curve points.
    @Test
    void insertsImpliedOnCurveMidpoint() {
        SfntFont font = OutlineSampleFont.create();
        CollectingPen pen = new CollectingPen();
        font.outline(OutlineSampleFont.GLYPH_IMPLIED, pen);
        List<OutlineOp> commands = pen.commands();
        assertEquals(4, commands.size());
        assertEquals(OutlineVerb.MOVE, commands.get(0).verb());
        assertEquals(OutlineVerb.QUAD, commands.get(1).verb());
        assertEquals(20.0f, commands.get(1).x0(), 0.01f);
        assertEquals(80.0f, commands.get(1).y0(), 0.01f);
        assertEquals(50.0f, commands.get(1).x1(), 0.01f);
        assertEquals(80.0f, commands.get(1).y1(), 0.01f);
        assertEquals(OutlineVerb.QUAD, commands.get(2).verb());
        assertEquals(80.0f, commands.get(2).x0(), 0.01f);
        assertEquals(80.0f, commands.get(2).y0(), 0.01f);
        assertEquals(100.0f, commands.get(2).x1(), 0.01f);
        assertEquals(0.0f, commands.get(2).y1(), 0.01f);
        assertEquals(OutlineVerb.CLOSE, commands.get(3).verb());
    }

    /// Translates the bump through the composite glyph.
    @Test
    void walksCompositeAsTranslatedBump() {
        SfntFont font = OutlineSampleFont.create();
        CollectingPen pen = new CollectingPen();
        font.outline(OutlineSampleFont.GLYPH_COMPOSITE, pen);
        List<OutlineOp> commands = pen.commands();
        assertEquals(3, commands.size());
        assertEquals(OutlineVerb.MOVE, commands.get(0).verb());
        assertEquals(OutlineSampleFont.COMPOSITE_DX, commands.get(0).x0(), 0.01f);
        assertEquals(OutlineSampleFont.COMPOSITE_DY, commands.get(0).y0(), 0.01f);
        assertEquals(OutlineVerb.QUAD, commands.get(1).verb());
        assertEquals(50.0f + OutlineSampleFont.COMPOSITE_DX, commands.get(1).x0(), 0.01f);
        assertEquals(100.0f + OutlineSampleFont.COMPOSITE_DY, commands.get(1).y0(), 0.01f);
        assertEquals(100.0f + OutlineSampleFont.COMPOSITE_DX, commands.get(1).x1(), 0.01f);
        assertEquals(OutlineSampleFont.COMPOSITE_DY, commands.get(1).y1(), 0.01f);
        assertEquals(OutlineVerb.CLOSE, commands.get(2).verb());
    }

    /// Writes a short command dump next to the test report for verification.
    @Test
    void writesOutlineSampleDump() throws Exception {
        SfntFont font = OutlineSampleFont.create();
        CollectingPen bump = new CollectingPen();
        CollectingPen composite = new CollectingPen();
        font.outline(OutlineSampleFont.GLYPH_BUMP, bump);
        font.outline(OutlineSampleFont.GLYPH_COMPOSITE, composite);
        GlyphMask mask = GlyphRasterizer.rasterize(font, OutlineSampleFont.GLYPH_BUMP, 100);
        int interior = mask.coverage()[25 * mask.width() + 50] & 0xFF;
        int triangle = mask.coverage()[70 * mask.width() + 50] & 0xFF;
        String dump = """
                bump=%s
                composite=%s
                raster=%dx%d interior50_25=%d triangle50_70=%d
                """.formatted(
                OutlineCompare.toJson(bump.commands()),
                OutlineCompare.toJson(composite.commands()),
                mask.width(),
                mask.height(),
                interior,
                triangle
        );
        Path output = Path.of("build", "outline-sample.txt");
        Files.createDirectories(output.getParent());
        Files.writeString(output, dump, StandardCharsets.UTF_8);
        assertEquals(0, triangle);
        assertTrue(interior > 200);
    }

    /// Leaves empty glyphs silent.
    @Test
    void walksEmptyGlyphWithoutCommands() {
        SfntFont font = OutlineSampleFont.create();
        CollectingPen pen = new CollectingPen();
        font.outline(0, pen);
        assertTrue(pen.commands().isEmpty());
    }
}

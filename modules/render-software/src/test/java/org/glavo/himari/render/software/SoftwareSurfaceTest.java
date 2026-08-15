package org.glavo.himari.render.software;

import org.glavo.himari.graphics.Color;
import org.glavo.himari.graphics.DisplayList;
import org.glavo.himari.graphics.DisplayListOp;
import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies software rasterization and PNG encoding of shipped drawing commands.
@NotNullByDefault
final class SoftwareSurfaceTest {
    /// Fills a rectangle and checks that the PNG signature and a non-zero pixel are produced.
    @Test
    void rendersRectToPng() {
        SoftwareSurface surface = new SoftwareSurface(8, 8);
        surface.clear(Color.SRGB_BLACK);
        surface.replay(new DisplayList(List.of(
                new DisplayListOp.FillRect(1.0f, 1.0f, 4.0f, 4.0f, Color.SRGB_WHITE)
        )));
        MemorySegment png = surface.toSdrPng();
        assertEquals((byte) 0x89, png.get(ValueLayout.JAVA_BYTE, 0L));
        assertEquals((byte) 0x50, png.get(ValueLayout.JAVA_BYTE, 1L));
        assertEquals((byte) 0x4E, png.get(ValueLayout.JAVA_BYTE, 2L));
        assertEquals((byte) 0x47, png.get(ValueLayout.JAVA_BYTE, 3L));
        MemorySegment rgba = surface.toSdrRgba();
        int center = ((2 * 8) + 2) * 4;
        assertTrue((rgba.get(ValueLayout.JAVA_BYTE, center) & 0xFF) > 200);
        assertTrue(surface.extendedLinearPremultiplied().length == 8 * 8 * 4);
    }
}

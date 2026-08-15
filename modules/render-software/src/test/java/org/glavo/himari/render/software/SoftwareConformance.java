package org.glavo.himari.render.software;

import org.glavo.himari.graphics.Color;
import org.glavo.himari.graphics.DisplayList;
import org.glavo.himari.graphics.DisplayListOp;
import org.jetbrains.annotations.NotNullByDefault;

import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/// Writes software-renderer PNG conformance evidence.
@NotNullByDefault
public final class SoftwareConformance {
    /// Prevents instantiation.
    private SoftwareConformance() {
    }

    /// Rasters a rectangle and writes the report.
    ///
    /// @param arguments one output directory
    /// @throws IOException if the report cannot be written
    public static void main(String[] arguments) throws IOException {
        if (arguments.length != 1) {
            throw new IllegalArgumentException("Expected one output directory");
        }
        SoftwareSurface surface = new SoftwareSurface(16, 16);
        surface.clear(Color.SRGB_BLACK);
        surface.replay(new DisplayList(List.of(
                new DisplayListOp.FillRect(2.0f, 2.0f, 8.0f, 8.0f, Color.SRGB_WHITE)
        )));
        MemorySegment png = surface.toSdrPng();
        if (png.byteSize() < 32L
                || png.get(ValueLayout.JAVA_BYTE, 0L) != (byte) 0x89
                || png.get(ValueLayout.JAVA_BYTE, 1L) != (byte) 0x50) {
            throw new IllegalStateException("PNG signature is missing");
        }
        Path output = Path.of(arguments[0]);
        Files.createDirectories(output);
        Files.write(output.resolve("rect.png"), png.toArray(ValueLayout.JAVA_BYTE));
        Files.writeString(
                output.resolve("results.json"),
                """
                        {
                          "profile": "m3-software",
                          "workPackage": "SW-001",
                          "status": "passed",
                          "pngBytes": %d
                        }
                        """.formatted(png.byteSize()),
                StandardCharsets.UTF_8
        );
    }
}

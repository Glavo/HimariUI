package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/// Writes SFNT mapping and raster conformance evidence.
@NotNullByDefault
public final class FontConformance {
    /// Prevents instantiation.
    private FontConformance() {
    }

    /// Maps a Latin glyph and writes the report.
    ///
    /// @param arguments one output directory
    /// @throws IOException if the report cannot be written
    public static void main(String[] arguments) throws IOException {
        if (arguments.length != 1) {
            throw new IllegalArgumentException("Expected one output directory");
        }
        SfntFont font = BitmapSfntFont.create();
        int glyph = font.glyphId('A');
        if (glyph <= 0) {
            throw new IllegalStateException("Latin glyph was not mapped");
        }
        GlyphMask mask = GlyphRasterizer.rasterize(font, glyph, 16);
        if (mask.width() <= 0 || mask.height() <= 0) {
            throw new IllegalStateException("Glyph raster was empty");
        }
        Path output = Path.of(arguments[0]);
        Files.createDirectories(output);
        Files.writeString(
                output.resolve("results.json"),
                """
                        {
                          "profile": "m4-font",
                          "workPackage": "FONT-001",
                          "status": "passed",
                          "glyphId": %d,
                          "maskWidth": %d,
                          "maskHeight": %d
                        }
                        """.formatted(glyph, mask.width(), mask.height()),
                StandardCharsets.UTF_8
        );
    }
}

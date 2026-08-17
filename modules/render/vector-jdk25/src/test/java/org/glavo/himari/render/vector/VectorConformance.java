package org.glavo.himari.render.vector;

import org.glavo.himari.graphics.Color;
import org.jetbrains.annotations.NotNullByDefault;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/// Writes optional `VECTOR-001` fill evidence.
@NotNullByDefault
public final class VectorConformance {
    /// Prevents instantiation.
    private VectorConformance() {
    }

    /// Fills a tail-bearing buffer and writes the report.
    ///
    /// @param arguments one output directory
    /// @throws Exception if the profile fails
    public static void main(String[] arguments) throws Exception {
        if (arguments.length != 1) {
            throw new IllegalArgumentException("Expected one output directory");
        }
        Color linear = Color.srgb(1.0f, 0.0f, 0.0f, 0.5f).toExtendedLinear();
        float[] pixels = new float[9 * 4];
        VectorPremulFill.fill(pixels, linear.red(), linear.green(), linear.blue(), linear.alpha());
        float expected = linear.red() * linear.alpha();
        if (Math.abs(pixels[0] - expected) > 1.0e-5f || Math.abs(pixels[32] - expected) > 1.0e-5f) {
            throw new IllegalStateException("Vector fill did not write the first or tail pixel");
        }
        Path output = Path.of(arguments[0]);
        Files.createDirectories(output);
        Files.writeString(
                output.resolve("results.json"),
                """
                        {
                          "profile": "m10-vector",
                          "workPackage": "VECTOR-001",
                          "status": "passed",
                          "lanePixels": %d,
                          "pixels": 9
                        }
                        """.formatted(VectorPremulFill.LANE_PIXELS),
                StandardCharsets.UTF_8
        );
    }
}

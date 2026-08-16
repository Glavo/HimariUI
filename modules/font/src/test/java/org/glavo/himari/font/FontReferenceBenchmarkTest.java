package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/// Drives the reference-path measurement entry point.
@NotNullByDefault
final class FontReferenceBenchmarkTest {
    /// Runs the shipped outline, raster, and GSUB scenes.
    @Test
    void measuresReferencePath() throws Exception {
        String report = FontReferenceBenchmark.measure();
        assertTrue(report.contains("outline-walk"));
        assertTrue(report.contains("grayscale-raster"));
        assertTrue(report.contains("latin-cmap-10k"));
        assertTrue(report.contains("gsub-isol-10k"));
        assertTrue(report.contains("p50_ns="));
        Path output = Path.of("build", "perf-baseline-font.txt");
        Files.createDirectories(output.getParent());
        Files.writeString(output, report, StandardCharsets.UTF_8);
    }
}

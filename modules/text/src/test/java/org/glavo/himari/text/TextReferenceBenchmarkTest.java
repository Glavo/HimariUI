package org.glavo.himari.text;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/// Drives the shaping reference-path measurement entry point.
@NotNullByDefault
final class TextReferenceBenchmarkTest {
    /// Runs Latin and Arabic shaping scenes.
    @Test
    void measuresReferencePath() throws Exception {
        String report = TextReferenceBenchmark.measure();
        assertTrue(report.contains("shape-latin-10k"));
        assertTrue(report.contains("shape-arabic-presentation-10k"));
        assertTrue(report.contains("shape-arabic-gsub-10k"));
        assertTrue(report.contains("p50_ns="));
        Path output = Path.of("build", "perf-baseline-text.txt");
        Files.createDirectories(output.getParent());
        Files.writeString(output, report, StandardCharsets.UTF_8);
    }
}

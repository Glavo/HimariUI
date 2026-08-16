package org.glavo.himari.text;

import org.glavo.himari.font.GsubSampleFont;
import org.glavo.himari.font.ScriptSampleFont;
import org.glavo.himari.font.SfntFont;
import org.jetbrains.annotations.NotNullByDefault;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;

/// Measures the shipped default shaper on Latin and Arabic reference strings.
@NotNullByDefault
public final class TextReferenceBenchmark {
    /// Warmup iterations per scene.
    private static final int WARMUP = 8;

    /// Timed iterations per scene.
    private static final int ITERATIONS = 24;

    /// Prevents instantiation.
    private TextReferenceBenchmark() {
    }

    /// Writes the reference-path report to `arguments[0]` or stdout.
    ///
    /// @param arguments an optional output file
    /// @throws IOException if the file cannot be written
    public static void main(String[] arguments) throws IOException {
        String report = measure();
        if (arguments.length > 0 && !arguments[0].isBlank()) {
            Path output = Path.of(arguments[0]);
            Path parent = output.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(output, report, StandardCharsets.UTF_8);
        } else {
            System.out.print(report);
        }
    }

    /// Runs the shaping scenes and returns a text report.
    ///
    /// @return the report
    public static String measure() {
        SfntFont latinFont = ScriptSampleFont.create();
        SfntFont gsubFont = GsubSampleFont.create();
        String latin = "A".repeat(10_000);
        String arabic = "\u0628".repeat(10_000);
        Scene latinScene = time("shape-latin-10k", () -> DefaultShaper.shape(latinFont, latin));
        Scene arabicFallback = time("shape-arabic-presentation-10k", () -> DefaultShaper.shape(latinFont, arabic));
        Scene arabicGsub = time("shape-arabic-gsub-10k", () -> DefaultShaper.shape(gsubFont, arabic));
        return """
                # HimariUI text reference-path baseline
                # Not a HarfBuzz contest. Values are this-machine wall time.
                warmup = %d
                iterations = %d
                %s
                %s
                %s
                """.formatted(WARMUP, ITERATIONS, latinScene, arabicFallback, arabicGsub);
    }

    /// Times one scene.
    private static Scene time(String name, Runnable action) {
        for (int index = 0; index < WARMUP; index++) {
            action.run();
        }
        long[] nanos = new long[ITERATIONS];
        for (int index = 0; index < ITERATIONS; index++) {
            long start = System.nanoTime();
            action.run();
            nanos[index] = System.nanoTime() - start;
        }
        Arrays.sort(nanos);
        return new Scene(name, percentile(nanos, 50), percentile(nanos, 95));
    }

    /// Returns the nearest-rank percentile.
    private static long percentile(long[] sorted, int percentile) {
        int index = Math.min(sorted.length - 1, (sorted.length * percentile) / 100);
        return sorted[index];
    }

    /// One measured scene.
    ///
    /// @param name the scene name
    /// @param p50Nanos the p50 wall time
    /// @param p95Nanos the p95 wall time
    private record Scene(String name, long p50Nanos, long p95Nanos) {
        @Override
        public String toString() {
            return String.format(Locale.ROOT, "%s p50_ns=%d p95_ns=%d", name, p50Nanos, p95Nanos);
        }
    }
}

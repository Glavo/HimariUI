package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;

/// Measures the shipped outline walk, grayscale raster, and (via reflection-free) font decode path.
///
/// This is a reference-path baseline, not a FreeType/HarfBuzz contest. Invoke `main` with an
/// optional output path, or run [`#measure()`] from tests.
@NotNullByDefault
public final class FontReferenceBenchmark {
    /// Warmup iterations per scene.
    private static final int WARMUP = 8;

    /// Timed iterations per scene.
    private static final int ITERATIONS = 24;

    /// Prevents instantiation.
    private FontReferenceBenchmark() {
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

    /// Runs the reference scenes and returns a text report.
    ///
    /// @return the report
    public static String measure() {
        SfntFont outlineFont = OutlineSampleFont.create();
        SfntFont latinFont = BitmapSfntFont.create();
        DiscardPen pen = new DiscardPen();
        Scene outline = time("outline-walk", () -> {
            outlineFont.outline(OutlineSampleFont.GLYPH_BUMP, pen);
            outlineFont.outline(OutlineSampleFont.GLYPH_COMPOSITE, pen);
            outlineFont.outline(OutlineSampleFont.GLYPH_IMPLIED, pen);
        });
        Scene raster = time("grayscale-raster", () -> {
            GlyphRasterizer.rasterize(outlineFont, OutlineSampleFont.GLYPH_BUMP, 48);
            GlyphRasterizer.rasterize(outlineFont, OutlineSampleFont.GLYPH_COMPOSITE, 48);
        });
        Scene latin = time("latin-cmap-10k", () -> {
            for (int index = 0; index < 10_000; index++) {
                latinFont.glyphId('A' + (index % 26));
                latinFont.metrics(2);
            }
        });
        SfntFont gsubFont = GsubSampleFont.create();
        int nominal = gsubFont.glyphId('\u0628');
        Scene gsub = time("gsub-isol-10k", () -> {
            for (int index = 0; index < 10_000; index++) {
                gsubFont.substitute(nominal, GsubSampleFont.TAG_ISOL);
                gsubFont.substitute(nominal, GsubSampleFont.TAG_INIT);
                gsubFont.substitute(nominal, GsubSampleFont.TAG_MEDI);
                gsubFont.substitute(nominal, GsubSampleFont.TAG_FINA);
            }
        });
        return """
                # HimariUI font reference-path baseline
                # Not a FreeType/HarfBuzz contest. Values are this-machine wall time and allocation.
                warmup = %d
                iterations = %d
                %s
                %s
                %s
                %s
                """.formatted(WARMUP, ITERATIONS, outline, raster, latin, gsub);
    }

    /// Times one scene.
    private static Scene time(String name, Runnable action) {
        for (int index = 0; index < WARMUP; index++) {
            action.run();
        }
        long[] nanos = new long[ITERATIONS];
        Runtime runtime = Runtime.getRuntime();
        long[] heap = new long[ITERATIONS];
        for (int index = 0; index < ITERATIONS; index++) {
            long beforeHeap = runtime.totalMemory() - runtime.freeMemory();
            long start = System.nanoTime();
            action.run();
            nanos[index] = System.nanoTime() - start;
            long afterHeap = runtime.totalMemory() - runtime.freeMemory();
            heap[index] = Math.max(0L, afterHeap - beforeHeap);
        }
        Arrays.sort(nanos);
        Arrays.sort(heap);
        return new Scene(name, percentile(nanos, 50), percentile(nanos, 95), percentile(heap, 50));
    }

    /// Returns the nearest-rank percentile.
    private static long percentile(long[] sorted, int percentile) {
        int index = Math.min(sorted.length - 1, (sorted.length * percentile) / 100);
        return sorted[index];
    }

    /// Drops outline commands so the walk is measured without a growing log.
    private static final class DiscardPen implements OutlinePen {
        @Override
        public void moveTo(float x, float y) {
        }

        @Override
        public void lineTo(float x, float y) {
        }

        @Override
        public void quadTo(float cx, float cy, float x, float y) {
        }

        @Override
        public void close() {
        }
    }

    /// One measured scene.
    ///
    /// @param name the scene name
    /// @param p50Nanos the p50 wall time
    /// @param p95Nanos the p95 wall time
    /// @param p50Allocated the p50 allocated bytes, or `-1` when unavailable
    private record Scene(String name, long p50Nanos, long p95Nanos, long p50Allocated) {
        @Override
        public String toString() {
            return String.format(
                    Locale.ROOT,
                    "%s p50_ns=%d p95_ns=%d p50_heap_delta_bytes=%d",
                    name,
                    p50Nanos,
                    p95Nanos,
                    p50Allocated
            );
        }
    }
}

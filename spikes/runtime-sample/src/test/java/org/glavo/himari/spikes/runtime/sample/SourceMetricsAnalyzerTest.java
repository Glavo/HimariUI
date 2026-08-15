package org.glavo.himari.spikes.runtime.sample;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/// Verifies deterministic source-line filtering and auditable ceremony markers.
@NotNullByDefault
final class SourceMetricsAnalyzerTest {
    /// The isolated source-corpus root.
    @TempDir
    Path temporaryDirectory;

    /// Verifies that comments, imports, literals, and brace-only lines do not inflate source metrics.
    ///
    /// @throws IOException if the test fixture cannot be written
    @Test
    void measuresSignificantLinesAndMarkers() throws IOException {
        Path source = temporaryDirectory.resolve("sample/Sample.java");
        Files.createDirectories(source.getParent());
        Files.writeString(source, """
                package sample;

                /// Documentation only.
                final class Sample {
                    private final String value = "literal // not a comment";

                    void run() {
                        System.out.println(value); // trailing comment
                    }
                }
                """, StandardCharsets.UTF_8);
        SourceCorpus corpus = new SourceCorpus(
                temporaryDirectory,
                List.of(new SourceUnit("sample/Sample.java", FixtureStage.MICRO)),
                List.of(new SourceMarker(
                        "sample/Sample.java",
                        8,
                        SourceCeremonyKind.CALLBACK_WRAPPER,
                        "Synthetic test marker"
                ))
        );

        SourceMetrics metrics = SourceMetricsAnalyzer.analyze(corpus);

        assertEquals(4L, metrics.sourceLines());
        assertEquals(4L, metrics.files().getFirst().sourceLines());
        assertEquals(1L, metrics.ceremonyCounts().get("callbackWrappers"));
        assertEquals(0L, metrics.ceremonyCounts().get("explicitKeys"));
    }

    /// Verifies that a marker cannot name a comment-only line.
    ///
    /// @throws IOException if the test fixture cannot be written
    @Test
    void rejectsMarkerOutsideSignificantCode() throws IOException {
        Path source = temporaryDirectory.resolve("sample/CommentOnly.java");
        Files.createDirectories(source.getParent());
        Files.writeString(source, "package sample;\n// comment\nfinal class CommentOnly {}\n", StandardCharsets.UTF_8);
        SourceCorpus corpus = new SourceCorpus(
                temporaryDirectory,
                List.of(new SourceUnit("sample/CommentOnly.java", FixtureStage.MICRO)),
                List.of(new SourceMarker(
                        "sample/CommentOnly.java",
                        2,
                        SourceCeremonyKind.GROUP_BOUNDARY,
                        "Invalid comment marker"
                ))
        );

        assertThrows(IllegalArgumentException.class, () -> SourceMetricsAnalyzer.analyze(corpus));
    }
}

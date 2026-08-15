package org.glavo.himari.spikes.runtime.grouped;

import org.glavo.himari.spikes.runtime.sample.ComparisonJson;
import org.glavo.himari.spikes.runtime.sample.ComparisonReport;
import org.glavo.himari.spikes.runtime.sample.ComparisonStatus;
import org.glavo.himari.spikes.runtime.sample.FixtureStatus;
import org.glavo.himari.spikes.runtime.sample.RuntimeComparisonRunner;
import org.jetbrains.annotations.NotNullByDefault;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/// Generates and validates the explicit-grouped candidate report artifact.
@NotNullByDefault
public final class GroupedRuntimeConformance {
    /// Prevents construction.
    private GroupedRuntimeConformance() {
    }

    /// Runs every frozen fixture and writes the canonical candidate report.
    ///
    /// @param arguments repository root and output directory
    /// @throws IOException if the artifact cannot be written
    public static void main(String[] arguments) throws IOException {
        if (arguments.length != 2) {
            throw new IllegalArgumentException("Expected repository root and output directory");
        }
        Path repositoryRoot = Path.of(arguments[0]).toAbsolutePath().normalize();
        Path outputDirectory = Path.of(arguments[1]).toAbsolutePath().normalize();
        ComparisonReport report = RuntimeComparisonRunner.run(new GroupedRuntimeCandidate(repositoryRoot));
        if (report.status() != ComparisonStatus.INCOMPLETE) {
            throw new IllegalStateException("Grouped report must remain incomplete pending external evidence: " + report.status());
        }
        if (!report.disqualifications().isEmpty()
                || report.fixtures().stream().anyMatch(result -> result.status() != FixtureStatus.PASSED)) {
            throw new IllegalStateException("Grouped candidate failed the frozen correctness gate");
        }
        Files.createDirectories(outputDirectory);
        Files.writeString(
                outputDirectory.resolve("report.json"),
                ComparisonJson.report(report),
                StandardCharsets.UTF_8
        );
    }
}

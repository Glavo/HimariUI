package org.glavo.himari.spikes.runtime.sample;

import org.jetbrains.annotations.NotNullByDefault;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/// Validates and writes the reproducible RUNTIME-SAMPLE-001 evidence artifacts.
@NotNullByDefault
public final class RuntimeSampleConformance {
    /// Prevents construction.
    private RuntimeSampleConformance() {
    }

    /// Runs the ineligible oracle replay and writes `suite.json`, `rubric.json`, and `self-test-report.json`.
    ///
    /// @param arguments repository root followed by evidence directory
    public static void main(String[] arguments) {
        if (arguments.length != 2) {
            throw new IllegalArgumentException("Expected: <repository-root> <evidence-directory>");
        }
        Path repositoryRoot = Path.of(arguments[0]).toAbsolutePath().normalize();
        Path evidenceDirectory = Path.of(arguments[1]).toAbsolutePath().normalize();
        ComparisonReport report = RuntimeComparisonRunner.run(new ReferenceProtocolCandidate(repositoryRoot));
        if (report.status() != ComparisonStatus.SELF_TEST_PASSED) {
            throw new IllegalStateException("Runtime comparison self-test failed: " + report.disqualifications());
        }
        long passedFixtures = report.fixtures().stream()
                .filter(result -> result.status() == FixtureStatus.PASSED)
                .count();
        if (passedFixtures != FixtureCatalog.fixtures().size()) {
            throw new IllegalStateException(
                    "Runtime comparison self-test passed " + passedFixtures + " of "
                            + FixtureCatalog.fixtures().size() + " fixtures"
            );
        }

        String suite = ComparisonJson.suite();
        String rubric = ComparisonJson.rubric();
        String reportJson = ComparisonJson.report(report);
        if (!reportJson.equals(ComparisonJson.report(report))) {
            throw new IllegalStateException("Canonical report encoding is not deterministic");
        }
        write(evidenceDirectory.resolve("suite.json"), suite);
        write(evidenceDirectory.resolve("rubric.json"), rubric);
        write(evidenceDirectory.resolve("self-test-report.json"), reportJson);
        System.out.println(
                "RUNTIME-SAMPLE-001 conformance passed: fixtures=" + passedFixtures
                        + ", rubric=" + DecisionRubric.VERSION
                        + ", suite=" + FixtureCatalog.VERSION
        );
    }

    /// Writes one UTF-8 artifact after creating its parent directory.
    ///
    /// @param path the output path
    /// @param content the complete content
    private static void write(Path path, String content) {
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, content, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot write runtime comparison evidence " + path, exception);
        }
    }
}

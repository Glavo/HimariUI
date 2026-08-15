package org.glavo.himari.spikes.runtime.decision;

import org.glavo.himari.spikes.runtime.sample.ComparisonJson;
import org.glavo.himari.spikes.runtime.sample.ComparisonReport;
import org.glavo.himari.spikes.runtime.sample.ComparisonStatus;
import org.glavo.himari.spikes.runtime.sample.DecisionRubric;
import org.glavo.himari.spikes.runtime.sample.FixtureCatalog;
import org.glavo.himari.spikes.runtime.sample.FixtureStatus;
import org.glavo.himari.spikes.runtime.sample.RuntimeComparisonRunner;
import org.jetbrains.annotations.NotNullByDefault;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.TreeMap;

/// Executes every real structural-runtime candidate from inside one Native Image executable.
@NotNullByDefault
public final class NativeImageDecisionConformance {
    /// Prevents construction.
    private NativeImageDecisionConformance() {
    }

    /// Runs the frozen suite for all candidates and writes compact and complete native evidence.
    ///
    /// @param arguments repository root and native evidence output directory
    /// @throws IOException if evidence cannot be written
    public static void main(String[] arguments) throws IOException {
        if (arguments.length != 2) {
            throw new IllegalArgumentException("Expected repository root and Native Image evidence output directory");
        }
        String imageCode = System.getProperty("org.graalvm.nativeimage.imagecode", "");
        if (!imageCode.equals("runtime")) {
            throw new IllegalStateException("This conformance entry point must run inside Native Image");
        }
        Path repositoryRoot = Path.of(arguments[0]).toAbsolutePath().normalize();
        Path outputDirectory = Path.of(arguments[1]).toAbsolutePath().normalize();
        Files.createDirectories(outputDirectory);

        TreeMap<String, String> results = new TreeMap<>();
        results.put("schemaVersion", "1");
        results.put("suiteVersion", FixtureCatalog.VERSION);
        results.put("rubricVersion", DecisionRubric.VERSION);
        results.put("imageCode", imageCode);
        results.put("javaRuntimeVersion", System.getProperty("java.runtime.version"));
        results.put("vmName", System.getProperty("java.vm.name"));
        results.put("osName", System.getProperty("os.name"));
        results.put("osArchitecture", System.getProperty("os.arch"));

        for (RuntimeDecisionCandidate candidate : RuntimeDecisionCandidate.values()) {
            ComparisonReport report = RuntimeComparisonRunner.run(candidate.create(repositoryRoot));
            long passedFixtures = report.fixtures().stream()
                    .filter(result -> result.status() == FixtureStatus.PASSED)
                    .count();
            if (report.status() != ComparisonStatus.INCOMPLETE
                    || !report.disqualifications().isEmpty()
                    || passedFixtures != FixtureCatalog.fixtures().size()) {
                throw new IllegalStateException("Native Image candidate failed: " + candidate.key());
            }
            Path reportPath = outputDirectory.resolve(candidate.key() + "-report.json");
            Files.writeString(reportPath, ComparisonJson.report(report), StandardCharsets.UTF_8);
            String prefix = "candidate." + candidate.key() + '.';
            results.put(prefix + "id", report.candidate().id());
            results.put(prefix + "status", "incomplete");
            results.put(prefix + "passedFixtures", Long.toString(passedFixtures));
            results.put(prefix + "totalFixtures", Integer.toString(report.fixtures().size()));
            results.put(prefix + "disqualifications", Integer.toString(report.disqualifications().size()));
            results.put(prefix + "reportSha256", DecisionProperties.sha256(reportPath));
        }
        results.put("status", "passed");
        DecisionProperties.write(outputDirectory.resolve("results.properties"), results);
    }
}

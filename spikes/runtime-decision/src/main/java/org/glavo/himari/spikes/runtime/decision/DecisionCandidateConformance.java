package org.glavo.himari.spikes.runtime.decision;

import org.glavo.himari.spikes.runtime.sample.CandidateEvidence;
import org.glavo.himari.spikes.runtime.sample.ComparisonJson;
import org.glavo.himari.spikes.runtime.sample.ComparisonReport;
import org.glavo.himari.spikes.runtime.sample.ComparisonStatus;
import org.glavo.himari.spikes.runtime.sample.EvidenceStatus;
import org.glavo.himari.spikes.runtime.sample.FixtureStatus;
import org.glavo.himari.spikes.runtime.sample.RuntimeCandidate;
import org.glavo.himari.spikes.runtime.sample.RuntimeComparisonRunner;
import org.jetbrains.annotations.NotNullByDefault;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/// Produces one evidence-backed JVM candidate report in an isolated comparison process.
@NotNullByDefault
public final class DecisionCandidateConformance {
    /// Prevents construction.
    private DecisionCandidateConformance() {
    }

    /// Validates external evidence, runs one candidate, and writes its report and scoring inputs.
    ///
    /// @param arguments candidate key, repository root, output directory, checked review, generated
    /// packet manifest, and Native Image result
    /// @throws IOException if evidence or output files cannot be read or written
    public static void main(String[] arguments) throws IOException {
        if (arguments.length != 6) {
            throw new IllegalArgumentException(
                    "Expected candidate key, repository root, output directory, ceremony review, packet manifest, "
                            + "and Native Image result"
            );
        }
        RuntimeDecisionCandidate candidate = RuntimeDecisionCandidate.fromKey(arguments[0]);
        Path repositoryRoot = Path.of(arguments[1]).toAbsolutePath().normalize();
        Path outputDirectory = Path.of(arguments[2]).toAbsolutePath().normalize();
        Path reviewPath = Path.of(arguments[3]).toAbsolutePath().normalize();
        Path manifestPath = Path.of(arguments[4]).toAbsolutePath().normalize();
        Path nativeResultsPath = Path.of(arguments[5]).toAbsolutePath().normalize();

        CeremonyReviewEvidence ceremony = new CeremonyReviewEvidence(reviewPath, manifestPath);
        NativeImageEvidence nativeImage = new NativeImageEvidence(nativeResultsPath);
        Path packetPath = manifestPath.getParent().resolve(
                "candidate-" + candidate.reviewLabel().toLowerCase(java.util.Locale.ROOT) + ".txt"
        );
        CandidateEvidence evidence = new CandidateEvidence(
                EvidenceStatus.PASSED,
                EvidenceStatus.NOT_APPLICABLE,
                EvidenceStatus.PASSED,
                Map.of(
                        "ceremonyPacket", artifactPath(repositoryRoot, packetPath),
                        "ceremonyReview", artifactPath(repositoryRoot, ceremony.reviewPath()),
                        "nativeImageResults", artifactPath(repositoryRoot, nativeImage.resultsPath())
                )
        );
        RuntimeCandidate adapter = new EvidenceBackedRuntimeCandidate(candidate.create(repositoryRoot), evidence);
        ComparisonReport report = RuntimeComparisonRunner.run(adapter);
        if (report.status() != ComparisonStatus.PASSED
                || !report.disqualifications().isEmpty()
                || report.fixtures().stream().anyMatch(result -> result.status() != FixtureStatus.PASSED)) {
            throw new IllegalStateException("Evidence-backed candidate failed: " + candidate.key());
        }
        Files.createDirectories(outputDirectory);
        Path reportPath = outputDirectory.resolve("report.json");
        Files.writeString(reportPath, ComparisonJson.report(report), StandardCharsets.UTF_8);
        CandidateMeasurements.from(report, DecisionProperties.sha256(reportPath))
                .write(outputDirectory.resolve("measurements.properties"));
    }

    /// Returns a stable forward-slash artifact path, relative to the repository when possible.
    ///
    /// @param repositoryRoot the normalized repository root
    /// @param artifact the artifact path
    /// @return the artifact path recorded in candidate evidence
    private static String artifactPath(Path repositoryRoot, Path artifact) {
        Path normalized = artifact.toAbsolutePath().normalize();
        Path value = normalized.startsWith(repositoryRoot) ? repositoryRoot.relativize(normalized) : normalized;
        return value.toString().replace('\\', '/');
    }
}

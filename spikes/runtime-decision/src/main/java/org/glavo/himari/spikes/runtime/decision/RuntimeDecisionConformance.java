package org.glavo.himari.spikes.runtime.decision;

import org.glavo.himari.spikes.runtime.sample.DecisionRubric;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/// Verifies the reviewed architecture selection against freshly produced isolated JVM reports.
@NotNullByDefault
public final class RuntimeDecisionConformance {
    /// Prevents construction.
    private RuntimeDecisionConformance() {
    }

    /// Scores the three measurement files and writes the current canonical decision artifacts.
    ///
    /// @param arguments output directory, checked reviewed-decision record, then grouped, one-shot,
    /// and hybrid measurement files
    /// @throws IOException if evidence cannot be read or written
    public static void main(String[] arguments) throws IOException {
        if (arguments.length != 5) {
            throw new IllegalArgumentException(
                    "Expected output directory, reviewed decision, and three candidate measurement files"
            );
        }
        Path outputDirectory = Path.of(arguments[0]).toAbsolutePath().normalize();
        Path reviewedDecisionPath = Path.of(arguments[1]).toAbsolutePath().normalize();
        @Unmodifiable Map<String, String> reviewed = DecisionProperties.read(reviewedDecisionPath);
        validateReviewedMetadata(reviewed);

        ArrayList<CandidateMeasurements> measurements = new ArrayList<>();
        for (int index = 2; index < arguments.length; index++) {
            measurements.add(CandidateMeasurements.read(Path.of(arguments[index])));
        }
        RuntimeDecisionResult reviewedScore = reproduceReviewedScore(reviewed, measurements);
        RuntimeDecisionResult decision = RuntimeDecisionScorer.score(measurements);
        requireReviewedSelection(decision, reviewed, "Current evidence");

        Files.createDirectories(outputDirectory);
        @Unmodifiable Map<String, String> qualitativeReview = Map.of(
                "benchmarkNoise", DecisionProperties.require(reviewed, "benchmarkNoiseReview"),
                "materialOutliers", DecisionProperties.require(reviewed, "materialOutlierReview"),
                "pareto", DecisionProperties.require(reviewed, "paretoReview"),
                "semanticCapability", DecisionProperties.require(reviewed, "semanticCapabilityReview")
        );
        Files.writeString(
                outputDirectory.resolve("decision.json"),
                RuntimeDecisionJson.write(
                        decision,
                        reviewedScore,
                        Math.toIntExact(DecisionProperties.requireNonNegativeLong(reviewed, "scoringRun")),
                        DecisionProperties.sha256(reviewedDecisionPath),
                        qualitativeReview
                ),
                StandardCharsets.UTF_8
        );
        Files.writeString(
                outputDirectory.resolve("decision.md"),
                markdown(decision, reviewedScore, reviewed),
                StandardCharsets.UTF_8
        );
        TreeMap<String, String> selection = new TreeMap<>();
        selection.put("schemaVersion", "1");
        selection.put("rubricVersion", DecisionRubric.VERSION);
        selection.put("selectedCandidateId", Objects.requireNonNull(decision.selectedCandidateId()));
        selection.put("selectionReason", decision.selectionReason());
        for (CandidateScore score : decision.scores()) {
            selection.put(
                    "candidate." + score.measurements().candidateId() + ".totalScoreMicropoints",
                    Long.toString(score.totalScore())
            );
        }
        DecisionProperties.write(outputDirectory.resolve("selection.properties"), selection);
    }

    /// Validates the checked human review fields that cannot be regenerated from counters alone.
    ///
    /// @param reviewed the checked property map
    private static void validateReviewedMetadata(@Unmodifiable Map<String, String> reviewed) {
        requireEquals(reviewed, "schemaVersion", "1");
        requireEquals(reviewed, "rubricVersion", DecisionRubric.VERSION);
        requireEquals(reviewed, "status", "accepted");
        LocalDate.parse(DecisionProperties.require(reviewed, "reviewedOn"));
        DecisionProperties.require(reviewed, "reviewer");
        DecisionProperties.require(reviewed, "selectedCandidateId");
        DecisionProperties.require(reviewed, "selectionReason");
        DecisionProperties.require(reviewed, "benchmarkNoiseReview");
        DecisionProperties.require(reviewed, "materialOutlierReview");
        DecisionProperties.require(reviewed, "paretoReview");
        DecisionProperties.require(reviewed, "semanticCapabilityReview");
    }

    /// Recreates the accepted scoring run from its exact environment-sensitive samples.
    ///
    /// Deterministic metrics come from the freshly generated reports. The checked record supplies
    /// only the elapsed-time and allocation observations from the reviewed run, verifies that they
    /// belong to its recorded three-run ranges, and binds the resulting score and selection.
    ///
    /// @param reviewed the checked review record
    /// @param currentMeasurements freshly generated measurements
    /// @return the reproduced reviewed decision
    private static RuntimeDecisionResult reproduceReviewedScore(
            @Unmodifiable Map<String, String> reviewed,
            List<CandidateMeasurements> currentMeasurements
    ) {
        long noiseRuns = DecisionProperties.requireNonNegativeLong(reviewed, "noiseRuns");
        long scoringRun = DecisionProperties.requireNonNegativeLong(reviewed, "scoringRun");
        if (noiseRuns < 3L || scoringRun < 1L || scoringRun > noiseRuns) {
            throw new IllegalArgumentException("Reviewed scoring run is outside the recorded noise runs");
        }

        ArrayList<CandidateMeasurements> snapshot = new ArrayList<>(currentMeasurements.size());
        for (CandidateMeasurements current : currentMeasurements) {
            RuntimeDecisionCandidate candidate = candidateFor(current.candidateId());
            String prefix = "candidate." + candidate.key() + '.';
            long allocatedBytes = DecisionProperties.requireNonNegativeLong(
                    reviewed,
                    prefix + "reviewedAllocatedBytes"
            );
            long elapsedNanos = DecisionProperties.requireNonNegativeLong(
                    reviewed,
                    prefix + "reviewedElapsedNanos"
            );
            requireWithinRecordedRange(
                    reviewed,
                    prefix + "allocatedBytesMin",
                    prefix + "allocatedBytesMax",
                    allocatedBytes
            );
            requireWithinRecordedRange(
                    reviewed,
                    prefix + "elapsedNanosMin",
                    prefix + "elapsedNanosMax",
                    elapsedNanos
            );
            snapshot.add(current.withBenchmarkSample(allocatedBytes, elapsedNanos));
        }

        RuntimeDecisionResult result = RuntimeDecisionScorer.score(snapshot);
        requireReviewedSelection(result, reviewed, "Reviewed scoring snapshot");
        for (CandidateScore score : result.scores()) {
            RuntimeDecisionCandidate candidate = candidateFor(score.measurements().candidateId());
            long expected = DecisionProperties.requireNonNegativeLong(
                    reviewed,
                    "candidate." + candidate.key() + ".reviewedScoreMicropoints"
            );
            if (expected > RuntimeDecisionScorer.MAX_SCORE || score.totalScore() != expected) {
                throw new IllegalStateException(
                        "Reviewed score changed for " + score.measurements().candidateId()
                                + ": expected " + expected + ", found " + score.totalScore()
                );
            }
        }
        return result;
    }

    /// Requires a sampled value to lie in an inclusive checked range.
    ///
    /// @param reviewed the checked review record
    /// @param minimumKey property containing the inclusive minimum
    /// @param maximumKey property containing the inclusive maximum
    /// @param value the exact reviewed sample
    private static void requireWithinRecordedRange(
            @Unmodifiable Map<String, String> reviewed,
            String minimumKey,
            String maximumKey,
            long value
    ) {
        long minimum = DecisionProperties.requireNonNegativeLong(reviewed, minimumKey);
        long maximum = DecisionProperties.requireNonNegativeLong(reviewed, maximumKey);
        if (minimum > maximum || value < minimum || value > maximum) {
            throw new IllegalArgumentException(
                    "Reviewed sample " + value + " is outside " + minimumKey + ".." + maximumKey
            );
        }
    }

    /// Requires a scored result to reproduce the checked candidate and selection rule.
    ///
    /// @param decision the score and tie-break result
    /// @param reviewed the checked review record
    /// @param source description used in a mismatch diagnostic
    private static void requireReviewedSelection(
            RuntimeDecisionResult decision,
            @Unmodifiable Map<String, String> reviewed,
            String source
    ) {
        String selected = Objects.requireNonNull(
                decision.selectedCandidateId(),
                "The frozen score left the final semantic-simplicity tie-break unresolved"
        );
        String reviewedSelection = DecisionProperties.require(reviewed, "selectedCandidateId");
        String reviewedReason = DecisionProperties.require(reviewed, "selectionReason");
        if (!selected.equals(reviewedSelection) || !decision.selectionReason().equals(reviewedReason)) {
            throw new IllegalStateException(
                    source + " selects " + selected + " via " + decision.selectionReason()
                            + ", but the reviewed decision records " + reviewedSelection + " via " + reviewedReason
            );
        }
    }

    /// Resolves one candidate descriptor identifier to its frozen decision entry.
    ///
    /// @param candidateId the descriptor identifier
    /// @return the matching candidate entry
    private static RuntimeDecisionCandidate candidateFor(String candidateId) {
        for (RuntimeDecisionCandidate candidate : RuntimeDecisionCandidate.values()) {
            if (candidate.candidateId().equals(candidateId)) {
                return candidate;
            }
        }
        throw new IllegalArgumentException("Unknown runtime decision candidate: " + candidateId);
    }

    /// Renders a concise current-run decision table and reviewed qualitative notes.
    ///
    /// @param decision the current score
    /// @param reviewed the checked review fields
    /// @return the Markdown report
    private static String markdown(
            RuntimeDecisionResult decision,
            RuntimeDecisionResult reviewedScore,
            @Unmodifiable Map<String, String> reviewed
    ) {
        StringBuilder output = new StringBuilder();
        output.append("# M1 structural-runtime decision conformance\n\n")
                .append("- Rubric: `").append(DecisionRubric.VERSION).append("`\n")
                .append("- Selected candidate: `").append(decision.selectedCandidateId()).append("`\n")
                .append("- Selection rule: `").append(decision.selectionReason()).append("`\n")
                .append("- Reviewed scoring run: ").append(DecisionProperties.require(reviewed, "scoringRun")).append("\n")
                .append("- Reviewed on: ").append(DecisionProperties.require(reviewed, "reviewedOn")).append("\n\n")
                .append("| Candidate | Reviewed score | Current score | Accidental ceremony | Phase invalidations | Peak retained bytes |\n")
                .append("|---|---:|---:|---:|---:|---:|\n");
        for (CandidateScore score : decision.scores()) {
            CandidateMeasurements candidate = score.measurements();
            output.append("| ").append(candidate.candidateId())
                    .append(" | ").append(formatScore(scoreFor(reviewedScore, candidate.candidateId())))
                    .append(" | ").append(formatScore(score.totalScore()))
                    .append(" | ").append(candidate.accidentalCeremony())
                    .append(" | ").append(candidate.metric("phaseInvalidations"))
                    .append(" | ").append(candidate.metric("peakRetainedBytes"))
                    .append(" |\n");
        }
        output.append("\n## Reviewed qualitative evidence\n\n")
                .append("- Benchmark noise: ").append(DecisionProperties.require(reviewed, "benchmarkNoiseReview")).append("\n")
                .append("- Material outliers: ").append(DecisionProperties.require(reviewed, "materialOutlierReview")).append("\n")
                .append("- Pareto review: ").append(DecisionProperties.require(reviewed, "paretoReview")).append("\n")
                .append("- Semantic capability review: ")
                .append(DecisionProperties.require(reviewed, "semanticCapabilityReview")).append("\n");
        return output.toString();
    }

    /// Returns one candidate's score from a decision result.
    ///
    /// @param decision the decision result
    /// @param candidateId the candidate identifier
    /// @return the score in millionths of one point
    private static long scoreFor(RuntimeDecisionResult decision, String candidateId) {
        for (CandidateScore score : decision.scores()) {
            if (score.measurements().candidateId().equals(candidateId)) {
                return score.totalScore();
            }
        }
        throw new IllegalArgumentException("Decision result has no candidate " + candidateId);
    }

    /// Formats a millionth-point score with six fractional digits.
    ///
    /// @param score the millionth-point score
    /// @return the decimal point score
    private static String formatScore(long score) {
        return String.format(Locale.ROOT, "%.6f", score / 1_000_000.0);
    }

    /// Requires one checked property value.
    ///
    /// @param values the property map
    /// @param key the property key
    /// @param expected the required value
    private static void requireEquals(
            @Unmodifiable Map<String, String> values,
            String key,
            String expected
    ) {
        String actual = DecisionProperties.require(values, key);
        if (!actual.equals(expected)) {
            throw new IllegalArgumentException("Unexpected reviewed decision property " + key + ": " + actual);
        }
    }
}

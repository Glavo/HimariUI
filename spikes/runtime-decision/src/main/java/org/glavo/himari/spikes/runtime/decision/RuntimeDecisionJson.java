package org.glavo.himari.spikes.runtime.decision;

import org.glavo.himari.spikes.runtime.sample.CanonicalJson;
import org.glavo.himari.spikes.runtime.sample.DecisionRubric;
import org.glavo.himari.spikes.runtime.sample.FixtureCatalog;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/// Encodes the M1 decision result without introducing a second JSON implementation.
@NotNullByDefault
final class RuntimeDecisionJson {
    /// Prevents construction.
    private RuntimeDecisionJson() {
    }

    /// Encodes one complete current decision result.
    ///
    /// @param decision the scored decision
    /// @param reviewedDecision the reproduced accepted scoring run
    /// @param reviewedScoringRun the one-based scoring run selected from the noise sample
    /// @param reviewedDecisionSha256 the checked reviewed-decision digest
    /// @param reviewValues the checked qualitative review fields
    /// @return canonical JSON with one trailing line feed
    static String write(
            RuntimeDecisionResult decision,
            RuntimeDecisionResult reviewedDecision,
            int reviewedScoringRun,
            String reviewedDecisionSha256,
            @Unmodifiable Map<String, String> reviewValues
    ) {
        if (reviewedScoringRun < 1) {
            throw new IllegalArgumentException("reviewedScoringRun must be positive");
        }
        return CanonicalJson.write(map(
                "schemaVersion", 1,
                "suiteVersion", FixtureCatalog.VERSION,
                "rubricVersion", DecisionRubric.VERSION,
                "scoreUnit", "millionth-of-one-point",
                "selectedCandidateId", decision.selectedCandidateId(),
                "selectionReason", decision.selectionReason(),
                "tieContenders", decision.tieContenders(),
                "paretoDominance", decision.paretoDominance(),
                "semanticDifferences", decision.semanticDifferences(),
                "reviewedDecisionSha256", reviewedDecisionSha256,
                "reviewedScoringRun", reviewedScoringRun,
                "reviewedSelectedCandidateId", reviewedDecision.selectedCandidateId(),
                "reviewedSelectionReason", reviewedDecision.selectionReason(),
                "reviewedScoreMicropoints", reviewedScores(reviewedDecision),
                "review", reviewValues,
                "candidates", decision.scores().stream().map(RuntimeDecisionJson::candidate).toList()
        ));
    }

    /// Returns the accepted candidate totals keyed by stable candidate identifier.
    ///
    /// @param decision the reproduced reviewed decision
    /// @return an immutable identifier-to-score map
    private static @Unmodifiable Map<String, Long> reviewedScores(RuntimeDecisionResult decision) {
        TreeMap<String, Long> scores = new TreeMap<>();
        for (CandidateScore score : decision.scores()) {
            Long previous = scores.put(score.measurements().candidateId(), score.totalScore());
            if (previous != null) {
                throw new IllegalArgumentException(
                        "Duplicate reviewed score candidate: " + score.measurements().candidateId()
                );
            }
        }
        return Collections.unmodifiableMap(scores);
    }

    /// Converts one candidate score to a canonical JSON value tree.
    ///
    /// @param score the score
    /// @return the JSON value
    private static @Unmodifiable Map<String, Object> candidate(CandidateScore score) {
        CandidateMeasurements measurements = score.measurements();
        return map(
                "id", measurements.candidateId(),
                "displayName", measurements.displayName(),
                "structuralModel", measurements.structuralModel(),
                "measureMaterializationMode", measurements.measureMaterializationMode(),
                "cancellationSupport", measurements.cancellationSupport(),
                "reloadIdentityClaimed", measurements.reloadIdentityClaimed(),
                "environment", map(
                        "javaRuntimeVersion", measurements.environment().javaRuntimeVersion(),
                        "vmName", measurements.environment().vmName(),
                        "osName", measurements.environment().osName(),
                        "osArchitecture", measurements.environment().osArchitecture(),
                        "availableProcessors", measurements.environment().availableProcessors()
                ),
                "allocationAvailable", measurements.allocationAvailable(),
                "measuredCommands", measurements.measuredCommands(),
                "rawMetrics", measurements.metrics(),
                "reportSha256", measurements.reportSha256(),
                "metricScoreMicropoints", score.metricScores(),
                "dimensionScoreMicropoints", score.dimensionScores(),
                "totalScoreMicropoints", score.totalScore()
        );
    }

    /// Creates an insertion-ordered immutable map from alternating keys and values.
    ///
    /// Canonical encoding subsequently sorts keys; insertion order only makes construction explicit.
    ///
    /// @param entries alternating string keys and values
    /// @return the immutable map
    private static @Unmodifiable Map<String, Object> map(Object... entries) {
        if (entries.length % 2 != 0) {
            throw new IllegalArgumentException("Decision JSON map requires alternating keys and values");
        }
        LinkedHashMap<String, Object> values = new LinkedHashMap<>();
        for (int index = 0; index < entries.length; index += 2) {
            String key = (String) entries[index];
            Object previous = values.put(key, entries[index + 1]);
            if (previous != null) {
                throw new IllegalArgumentException("Duplicate decision JSON key: " + key);
            }
        }
        return Collections.unmodifiableMap(values);
    }
}

package org.glavo.himari.spikes.runtime.decision;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/// Reports normalized frozen-rubric scores for one evidence-backed candidate.
///
/// Score values use millionths of one point: `100_000_000` represents 100 points.
///
/// @param measurements the auditable raw measurements
/// @param metricScores immutable per-metric normalized scores
/// @param dimensionScores immutable geometric-mean dimension scores
/// @param totalScore the fixed-weight total score
@NotNullByDefault
record CandidateScore(
        CandidateMeasurements measurements,
        @Unmodifiable Map<String, Long> metricScores,
        @Unmodifiable Map<String, Long> dimensionScores,
        long totalScore
) {
    /// Creates and validates an immutable candidate score.
    CandidateScore {
        Objects.requireNonNull(measurements, "measurements");
        metricScores = scoreMap(metricScores, "metricScores");
        dimensionScores = scoreMap(dimensionScores, "dimensionScores");
        requireScore(totalScore, "totalScore");
    }

    /// Copies and validates one map of millionth-point scores.
    ///
    /// @param values the score map
    /// @param name the diagnostic name
    /// @return the immutable key-sorted map
    private static @Unmodifiable Map<String, Long> scoreMap(Map<String, Long> values, String name) {
        TreeMap<String, Long> copy = new TreeMap<>(Objects.requireNonNull(values, name));
        for (Map.Entry<String, Long> entry : copy.entrySet()) {
            requireScore(entry.getValue(), name + '[' + entry.getKey() + ']');
        }
        return Collections.unmodifiableMap(copy);
    }

    /// Validates one score against the inclusive zero-to-one-hundred range.
    ///
    /// @param value the score
    /// @param name the diagnostic name
    private static void requireScore(long value, String name) {
        if (value < 0L || value > RuntimeDecisionScorer.MAX_SCORE) {
            throw new IllegalArgumentException(name + " is outside the score range: " + value);
        }
    }
}

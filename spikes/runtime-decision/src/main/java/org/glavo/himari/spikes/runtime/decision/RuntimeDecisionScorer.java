package org.glavo.himari.spikes.runtime.decision;

import org.glavo.himari.spikes.runtime.sample.ComparisonStatus;
import org.glavo.himari.spikes.runtime.sample.DecisionRubric;
import org.glavo.himari.spikes.runtime.sample.RubricDimension;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/// Applies the frozen non-compensatory score and fixed three-point tie-break sequence.
@NotNullByDefault
final class RuntimeDecisionScorer {
    /// One hundred points expressed in millionths of a point.
    static final long MAX_SCORE = 100_000_000L;

    /// The inclusive score-distance admitted to the fixed tie-break sequence.
    private static final long TIE_DISTANCE = 3_000_000L;

    /// Metrics normalized with the frozen lower-is-better formula.
    private static final @Unmodifiable Set<String> LOWER_IS_BETTER = Set.of(
            "sourceLines", "explicitKeys", "deferredGetters", "structuralControls",
            "groupBoundaries", "genericTypeNoise", "callbackWrappers",
            "callbacksExecuted", "nodesVisited", "dependencyEdges", "phaseInvalidations",
            "steadyStateAllocatedBytes", "peakRetainedBytes", "postCloseRetainedBytes",
            "steadyStateElapsedNanos"
    );

    /// Metrics assigned an absolute pass or diagnostic-quality percentage.
    private static final @Unmodifiable Set<String> DIRECT_SCORES = Set.of(
            "traceQuality", "requiredDiagnosticCoverage", "deterministicRecovery",
            "nativeImage", "reloadIdentity"
    );

    /// Prevents construction.
    private RuntimeDecisionScorer() {
    }

    /// Scores three eligible reports and applies the fixed tie-breakers that can be mechanical.
    ///
    /// @param candidates evidence-backed measurements in frozen candidate order
    /// @return the decision result
    static RuntimeDecisionResult score(@Unmodifiable List<CandidateMeasurements> candidates) {
        validateCandidates(candidates);
        DecisionRubric.validate();
        validateRubricMetrics();

        HashMap<String, Long> best = new HashMap<>();
        for (String metric : LOWER_IS_BETTER) {
            long minimum = Long.MAX_VALUE;
            for (CandidateMeasurements candidate : candidates) {
                minimum = Math.min(minimum, candidate.metric(metric));
            }
            best.put(metric, minimum);
        }

        ArrayList<CandidateScore> scores = new ArrayList<>();
        for (CandidateMeasurements candidate : candidates) {
            TreeMap<String, Long> metricScores = new TreeMap<>();
            for (String metric : LOWER_IS_BETTER) {
                metricScores.put(metric, lowerIsBetterScore(candidate.metric(metric), best.get(metric)));
            }
            metricScores.put("traceQuality", ratioScore(candidate.metric("traceQuality"), 4L));
            metricScores.put("requiredDiagnosticCoverage", ratioScore(
                    candidate.metric("requiredDiagnosticMatched"),
                    candidate.metric("requiredDiagnosticExpected")
            ));
            metricScores.put("deterministicRecovery", ratioScore(
                    candidate.metric("deterministicRecoveryMatched"),
                    candidate.metric("deterministicRecoveryExpected")
            ));
            metricScores.put("nativeImage", candidate.metric("nativeImage") == 1L ? MAX_SCORE : 0L);
            metricScores.put("reloadIdentity", candidate.metric("reloadIdentity") == 1L ? MAX_SCORE : 0L);

            TreeMap<String, Long> dimensionScores = new TreeMap<>();
            long weightedTotal = 0L;
            for (RubricDimension dimension : DecisionRubric.dimensions()) {
                ArrayList<Long> inputs = new ArrayList<>();
                for (String metric : dimension.metrics()) {
                    inputs.add(Objects.requireNonNull(metricScores.get(metric), "score metric " + metric));
                }
                long dimensionScore = geometricMean(inputs);
                dimensionScores.put(dimension.id(), dimensionScore);
                weightedTotal = Math.addExact(
                        weightedTotal,
                        Math.multiplyExact(dimensionScore, dimension.weight())
                );
            }
            scores.add(new CandidateScore(candidate, metricScores, dimensionScores, weightedTotal / 100L));
        }

        long highest = scores.stream().mapToLong(CandidateScore::totalScore).max().orElseThrow();
        ArrayList<CandidateScore> contenders = scores.stream()
                .filter(score -> highest - score.totalScore() <= TIE_DISTANCE)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        Selection selection = select(contenders);
        return new RuntimeDecisionResult(
                scores,
                contenders.stream().map(score -> score.measurements().candidateId()).toList(),
                selection.candidateId(),
                selection.reason(),
                paretoDominance(candidates),
                candidates.stream().map(candidate -> candidate.candidateId()
                        + ": measure=" + candidate.measureMaterializationMode()
                        + ", cancellation=" + candidate.cancellationSupport()
                        + ", reloadIdentityClaimed=" + candidate.reloadIdentityClaimed()).toList()
        );
    }

    /// Applies the frozen lower-is-better normalization in millionth-point units.
    ///
    /// @param value the candidate value
    /// @param best the lowest eligible value
    /// @return the normalized score
    static long lowerIsBetterScore(long value, long best) {
        if (value < 0L || best < 0L || value < best) {
            throw new IllegalArgumentException("Lower-is-better values are inconsistent");
        }
        if (best == 0L) {
            return value == 0L ? MAX_SCORE : Math.round((double) MAX_SCORE / (1.0 + value));
        }
        return Math.round((double) MAX_SCORE * best / value);
    }

    /// Computes a geometric mean of normalized millionth-point scores.
    ///
    /// @param values the nonempty normalized scores
    /// @return the rounded geometric mean
    static long geometricMean(@Unmodifiable List<Long> values) {
        if (values.isEmpty()) {
            throw new IllegalArgumentException("A rubric dimension must contain at least one metric");
        }
        double logarithms = 0.0;
        for (long value : values) {
            if (value < 0L || value > MAX_SCORE) {
                throw new IllegalArgumentException("Metric score is outside the score range");
            }
            if (value == 0L) {
                return 0L;
            }
            logarithms += StrictMath.log(value);
        }
        return Math.round(StrictMath.exp(logarithms / values.size()));
    }

    /// Converts a nonnegative matched/possible ratio into an absolute score.
    ///
    /// An empty requirement receives full credit because it imposes no missing obligation.
    ///
    /// @param matched the satisfied count
    /// @param possible the required count
    /// @return the absolute score
    private static long ratioScore(long matched, long possible) {
        if (matched < 0L || possible < 0L || matched > possible) {
            throw new IllegalArgumentException("Invalid direct-score ratio");
        }
        return possible == 0L ? MAX_SCORE : Math.round((double) MAX_SCORE * matched / possible);
    }

    /// Applies score, accidental-ceremony, phase, and retention tie-breakers in order.
    ///
    /// @param initial the candidates within three points of the highest score
    /// @return the selected candidate or a semantic-review requirement
    private static Selection select(List<CandidateScore> initial) {
        if (initial.size() == 1) {
            return new Selection(initial.getFirst().measurements().candidateId(), "weighted-score");
        }
        List<CandidateScore> contenders = retainMinimum(
                initial,
                score -> score.measurements().accidentalCeremony()
        );
        if (contenders.size() == 1) {
            return new Selection(contenders.getFirst().measurements().candidateId(), "tie-breaker:accidental-ceremony");
        }
        contenders = retainMinimum(contenders, score -> score.measurements().metric("phaseInvalidations"));
        if (contenders.size() == 1) {
            return new Selection(contenders.getFirst().measurements().candidateId(), "tie-breaker:phase-invalidation");
        }
        contenders = retainMinimum(contenders, score -> score.measurements().metric("peakRetainedBytes"));
        if (contenders.size() == 1) {
            return new Selection(contenders.getFirst().measurements().candidateId(), "tie-breaker:peak-retained-memory");
        }
        return new Selection(null, "tie-breaker:semantic-simplicity-review-required");
    }

    /// Retains every candidate tied for the minimum selected long value.
    ///
    /// @param candidates the current tie set
    /// @param metric the next tie-break metric
    /// @return the narrowed immutable list
    private static List<CandidateScore> retainMinimum(
            List<CandidateScore> candidates,
            java.util.function.ToLongFunction<CandidateScore> metric
    ) {
        long minimum = candidates.stream().mapToLong(metric).min().orElseThrow();
        return candidates.stream().filter(candidate -> metric.applyAsLong(candidate) == minimum).toList();
    }

    /// Validates candidate eligibility and environment comparability before compensation.
    ///
    /// @param candidates the measurements
    private static void validateCandidates(List<CandidateMeasurements> candidates) {
        if (candidates.size() != RuntimeDecisionCandidate.values().length) {
            throw new IllegalArgumentException("The decision requires exactly three candidates");
        }
        HashSet<String> identifiers = new HashSet<>();
        @Nullable CandidateMeasurements first = null;
        for (CandidateMeasurements candidate : candidates) {
            Objects.requireNonNull(candidate, "candidate measurements");
            if (!identifiers.add(candidate.candidateId())) {
                throw new IllegalArgumentException("Duplicate candidate measurements: " + candidate.candidateId());
            }
            if (candidate.status() != ComparisonStatus.PASSED || !candidate.allocationAvailable()) {
                throw new IllegalArgumentException("Candidate evidence is incomplete: " + candidate.candidateId());
            }
            if (candidate.metric("nativeImage") != 1L) {
                throw new IllegalArgumentException("Candidate has no Native Image evidence: " + candidate.candidateId());
            }
            if (candidate.reloadIdentityClaimed() && candidate.metric("reloadIdentity") != 1L) {
                throw new IllegalArgumentException("Claimed reload identity lacks evidence: " + candidate.candidateId());
            }
            if (first == null) {
                first = candidate;
            } else if (!first.environment().equals(candidate.environment())
                    || first.measuredCommands() != candidate.measuredCommands()) {
                throw new IllegalArgumentException("Candidate JVM environments or command counts are not comparable");
            }
        }
        for (RuntimeDecisionCandidate expected : RuntimeDecisionCandidate.values()) {
            if (!identifiers.contains(expected.candidateId())) {
                throw new IllegalArgumentException("Missing candidate measurements: " + expected.candidateId());
            }
        }
    }

    /// Verifies that the compiled scorer still covers exactly the frozen dimension metrics.
    private static void validateRubricMetrics() {
        HashSet<String> expected = new HashSet<>(LOWER_IS_BETTER);
        expected.addAll(DIRECT_SCORES);
        HashSet<String> actual = new HashSet<>();
        for (RubricDimension dimension : DecisionRubric.dimensions()) {
            actual.addAll(dimension.metrics());
        }
        if (!actual.equals(expected)) {
            throw new IllegalStateException("Decision scorer metrics no longer match the frozen rubric");
        }
    }

    /// Records purely numeric Pareto dominance without erasing capability differences.
    ///
    /// @param candidates the raw measurements
    /// @return immutable dominance observations
    private static @Unmodifiable List<String> paretoDominance(List<CandidateMeasurements> candidates) {
        ArrayList<String> observations = new ArrayList<>();
        for (CandidateMeasurements left : candidates) {
            for (CandidateMeasurements right : candidates) {
                if (left == right) {
                    continue;
                }
                boolean noWorse = true;
                boolean better = false;
                for (String metric : LOWER_IS_BETTER) {
                    long leftValue = left.metric(metric);
                    long rightValue = right.metric(metric);
                    noWorse &= leftValue <= rightValue;
                    better |= leftValue < rightValue;
                }
                for (String metric : List.of("traceQuality", "nativeImage", "reloadIdentity")) {
                    long leftValue = left.metric(metric);
                    long rightValue = right.metric(metric);
                    noWorse &= leftValue >= rightValue;
                    better |= leftValue > rightValue;
                }
                if (noWorse && better) {
                    observations.add(left.candidateId() + " numerically dominates " + right.candidateId());
                }
            }
        }
        return observations.isEmpty() ? List.of("none") : List.copyOf(observations);
    }

    /// Holds a mechanical selection or an unresolved final review.
    ///
    /// @param candidateId the selected identifier, or `null`
    /// @param reason the selection state
    @NotNullByDefault
    private record Selection(@Nullable String candidateId, String reason) {
        /// Creates a selection result.
        private Selection {
            Objects.requireNonNull(reason, "reason");
        }
    }
}

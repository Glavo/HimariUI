package org.glavo.himari.spikes.runtime.decision;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Objects;

/// Captures the complete scoring and tie-break outcome for `RUNTIME-ADR-001`.
///
/// @param scores immutable candidate scores in frozen candidate order
/// @param tieContenders immutable candidate identifiers admitted by the three-point rule
/// @param selectedCandidateId the selected candidate, or `null` if a final semantic review is required
/// @param selectionReason the score or tie-break rule that produced the outcome
/// @param paretoDominance immutable numeric Pareto-dominance observations
/// @param semanticDifferences immutable capability differences reviewed outside compensation
@NotNullByDefault
record RuntimeDecisionResult(
        @Unmodifiable List<CandidateScore> scores,
        @Unmodifiable List<String> tieContenders,
        @Nullable String selectedCandidateId,
        String selectionReason,
        @Unmodifiable List<String> paretoDominance,
        @Unmodifiable List<String> semanticDifferences
) {
    /// Creates an immutable decision result.
    RuntimeDecisionResult {
        scores = List.copyOf(scores);
        tieContenders = List.copyOf(tieContenders);
        selectionReason = Objects.requireNonNull(selectionReason, "selectionReason");
        paretoDominance = List.copyOf(paretoDominance);
        semanticDifferences = List.copyOf(semanticDifferences);
        if (scores.isEmpty() || selectionReason.isBlank()) {
            throw new IllegalArgumentException("Decision result must contain scores and a selection reason");
        }
    }
}

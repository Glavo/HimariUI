package org.glavo.himari.spikes.runtime.sample;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Objects;

/// Defines one scored tradeoff dimension after all disqualifiers and evidence gates pass.
///
/// @param id the stable dimension identifier
/// @param weight the integer percentage weight in the final score
/// @param metrics immutable report metric names used by the dimension
/// @param method the fixed normalization and aggregation method
@NotNullByDefault
public record RubricDimension(
        String id,
        int weight,
        @Unmodifiable List<String> metrics,
        String method
) {
    /// Creates a validated dimension.
    public RubricDimension {
        id = ComparisonContracts.requireIdentifier(id, "rubric dimension id");
        if (weight <= 0 || weight > 100) {
            throw new IllegalArgumentException("rubric dimension weight must be between 1 and 100");
        }
        Objects.requireNonNull(metrics, "metrics");
        metrics = List.copyOf(metrics);
        if (metrics.isEmpty()) {
            throw new IllegalArgumentException("rubric dimension must contain at least one metric");
        }
        for (String metric : metrics) {
            ComparisonContracts.requireText(metric, "rubric metric");
        }
        method = ComparisonContracts.requireText(method, "rubric dimension method");
    }
}

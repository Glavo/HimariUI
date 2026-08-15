package org.glavo.himari.spikes.runtime.sample;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/// Reports one applicable correctness step.
///
/// @param id the fixture-local step identifier
/// @param passed whether command execution, oracle comparison, diagnostics, and phases passed
/// @param actual the actual observation, or `null` when execution failed before observation
/// @param metrics the instrumentation captured before failure or successful completion
/// @param failure deterministic failure detail, or `null` when the step passed
@NotNullByDefault
public record StepResult(
        String id,
        boolean passed,
        @Nullable FixtureObservation actual,
        StepMetrics metrics,
        @Nullable String failure
) {
    /// Creates a validated step result.
    public StepResult {
        id = ComparisonContracts.requireIdentifier(id, "step result id");
        Objects.requireNonNull(metrics, "metrics");
        if (passed && (actual == null || failure != null)) {
            throw new IllegalArgumentException("a passed step must have an observation and no failure");
        }
        if (!passed && failure == null) {
            throw new IllegalArgumentException("a failed step must have failure detail");
        }
        if (failure != null) {
            failure = ComparisonContracts.requireText(failure, "step failure");
        }
    }
}

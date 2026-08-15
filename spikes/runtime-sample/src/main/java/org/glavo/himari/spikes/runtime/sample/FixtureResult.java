package org.glavo.himari.spikes.runtime.sample;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Objects;

/// Reports one fixture's correctness, instrumentation, benchmark, and cleanup outcome.
///
/// @param id the fixture identifier
/// @param stage the fixture stage
/// @param status the fixture outcome
/// @param steps immutable applicable step results
/// @param scenarioProbe complete correctness-session instrumentation after cleanup
/// @param benchmark benchmark metrics, or `null` when absent, skipped, or not reached
/// @param postCloseHealth candidate health after correctness-session closure
/// @param failures immutable deterministic fixture-level failure details
@NotNullByDefault
public record FixtureResult(
        String id,
        FixtureStage stage,
        FixtureStatus status,
        @Unmodifiable List<StepResult> steps,
        ProbeMetrics scenarioProbe,
        @Nullable BenchmarkMetrics benchmark,
        RuntimeHealth postCloseHealth,
        @Unmodifiable List<String> failures
) {
    /// Creates an immutable validated fixture result.
    public FixtureResult {
        id = ComparisonContracts.requireIdentifier(id, "fixture result id");
        Objects.requireNonNull(stage, "stage");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(steps, "steps");
        steps = List.copyOf(steps);
        for (StepResult step : steps) {
            Objects.requireNonNull(step, "step result");
        }
        Objects.requireNonNull(scenarioProbe, "scenarioProbe");
        Objects.requireNonNull(postCloseHealth, "postCloseHealth");
        Objects.requireNonNull(failures, "failures");
        failures = List.copyOf(failures);
        for (String failure : failures) {
            ComparisonContracts.requireText(failure, "fixture failure");
        }
        if (status == FixtureStatus.PASSED && !failures.isEmpty()) {
            throw new IllegalArgumentException("passed fixture must not contain failures");
        }
        if (status == FixtureStatus.FAILED && failures.isEmpty()) {
            throw new IllegalArgumentException("failed fixture must contain a failure");
        }
    }

    /// Creates a skipped result with empty instrumentation.
    ///
    /// @param fixture the skipped fixture
    /// @param reason the checkpoint reason
    /// @return the skipped result
    public static FixtureResult skipped(FixtureDefinition fixture, String reason) {
        ProbeMetrics empty = new ProbeMetrics(
                java.util.Map.of(), 0L, 0L, 0L, 0L, 0L, 0L, 0L, java.util.Map.of(), List.of()
        );
        return new FixtureResult(
                fixture.id(), fixture.stage(), FixtureStatus.SKIPPED, List.of(), empty, null,
                RuntimeHealth.CLEAN, List.of(ComparisonContracts.requireText(reason, "skip reason"))
        );
    }
}

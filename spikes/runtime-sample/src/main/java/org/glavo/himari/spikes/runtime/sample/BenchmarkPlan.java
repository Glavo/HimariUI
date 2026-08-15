package org.glavo.himari.spikes.runtime.sample;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Objects;

/// Defines a repeatable steady-state command cycle that returns to its starting observation.
///
/// @param warmupIterations cycles excluded from reported counters and allocation bytes
/// @param measuredIterations cycles included in the report
/// @param cycle the nonempty command sequence that restores the post-mount state
@NotNullByDefault
public record BenchmarkPlan(
        int warmupIterations,
        int measuredIterations,
        @Unmodifiable List<FixtureCommand> cycle
) {
    /// Creates a validated benchmark plan.
    public BenchmarkPlan {
        if (warmupIterations < 0) {
            throw new IllegalArgumentException("warmupIterations must be nonnegative");
        }
        if (measuredIterations <= 0) {
            throw new IllegalArgumentException("measuredIterations must be positive");
        }
        Objects.requireNonNull(cycle, "cycle");
        cycle = List.copyOf(cycle);
        if (cycle.isEmpty()) {
            throw new IllegalArgumentException("benchmark cycle must not be empty");
        }
        for (FixtureCommand command : cycle) {
            Objects.requireNonNull(command, "benchmark command");
        }
    }
}

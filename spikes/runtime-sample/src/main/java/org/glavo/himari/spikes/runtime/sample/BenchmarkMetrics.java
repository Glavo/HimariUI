package org.glavo.himari.spikes.runtime.sample;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Reports one fixture's fixed steady-state command window.
///
/// @param warmupIterations completed unmeasured cycles
/// @param measuredIterations completed measured cycles
/// @param commandsPerCycle commands executed by each cycle
/// @param elapsedNanos monotonic elapsed nanoseconds for measured commands and Headless drains
/// @param allocation current-thread bytes allocated by the measured commands and drains
/// @param probe candidate instrumentation for the measured window before session cleanup
@NotNullByDefault
public record BenchmarkMetrics(
        int warmupIterations,
        int measuredIterations,
        int commandsPerCycle,
        long elapsedNanos,
        AllocationMeasurement allocation,
        ProbeMetrics probe
) {
    /// Creates a validated benchmark result.
    public BenchmarkMetrics {
        if (warmupIterations < 0 || measuredIterations <= 0 || commandsPerCycle <= 0) {
            throw new IllegalArgumentException("benchmark iteration and command counts are invalid");
        }
        ComparisonContracts.requireNonNegative(elapsedNanos, "elapsedNanos");
        Objects.requireNonNull(allocation, "allocation");
        Objects.requireNonNull(probe, "probe");
    }
}

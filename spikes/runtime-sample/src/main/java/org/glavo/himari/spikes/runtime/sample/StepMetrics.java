package org.glavo.himari.spikes.runtime.sample;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/// Reports instrumentation attributable to one correctness command and its observation.
///
/// @param callbacksExecuted callback deltas by kind
/// @param nodesVisited logical node-visit delta
/// @param dependencyEdgesAttached edge-attachment delta
/// @param dependencyEdgesDetached edge-detachment delta
/// @param activeDependencyEdges active edges after the command
/// @param retainedBytes registered retained bytes after the command
/// @param phaseInvalidations invalidation deltas by phase
/// @param allocation current-thread allocation bytes for command, drain, and observation
/// @param traces diagnostic traces emitted by the command
@NotNullByDefault
public record StepMetrics(
        @Unmodifiable Map<String, Long> callbacksExecuted,
        long nodesVisited,
        long dependencyEdgesAttached,
        long dependencyEdgesDetached,
        long activeDependencyEdges,
        long retainedBytes,
        @Unmodifiable Map<String, Long> phaseInvalidations,
        AllocationMeasurement allocation,
        @Unmodifiable List<DiagnosticTrace> traces
) {
    /// Creates an immutable validated step-metrics snapshot.
    public StepMetrics {
        callbacksExecuted = metricMap(callbacksExecuted, "callbacksExecuted");
        ComparisonContracts.requireNonNegative(nodesVisited, "nodesVisited");
        ComparisonContracts.requireNonNegative(dependencyEdgesAttached, "dependencyEdgesAttached");
        ComparisonContracts.requireNonNegative(dependencyEdgesDetached, "dependencyEdgesDetached");
        ComparisonContracts.requireNonNegative(activeDependencyEdges, "activeDependencyEdges");
        ComparisonContracts.requireNonNegative(retainedBytes, "retainedBytes");
        phaseInvalidations = metricMap(phaseInvalidations, "phaseInvalidations");
        Objects.requireNonNull(allocation, "allocation");
        Objects.requireNonNull(traces, "traces");
        traces = List.copyOf(traces);
        for (DiagnosticTrace trace : traces) {
            Objects.requireNonNull(trace, "trace");
        }
    }

    /// Returns one immutable validated metric map.
    ///
    /// @param values the metric map
    /// @param name the diagnostic name
    /// @return the immutable copy
    private static @Unmodifiable Map<String, Long> metricMap(Map<String, Long> values, String name) {
        Map<String, Long> copy = ComparisonContracts.immutableSortedMap(values, name);
        for (Map.Entry<String, Long> entry : copy.entrySet()) {
            ComparisonContracts.requireNonNegative(entry.getValue(), name + '[' + entry.getKey() + ']');
        }
        return copy;
    }
}

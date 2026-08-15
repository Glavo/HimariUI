package org.glavo.himari.spikes.runtime.sample;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/// Captures one immutable instrumentation window.
///
/// @param callbacksExecuted callback counts by canonical kind
/// @param nodesVisited total logical mounted-node visits
/// @param dependencyEdgesAttached total edge attachments
/// @param dependencyEdgesDetached total edge detachments
/// @param activeDependencyEdges active edges at the snapshot
/// @param peakDependencyEdges maximum simultaneously active edges in the window
/// @param retainedBytes registered framework-owned shallow bytes at the snapshot
/// @param peakRetainedBytes maximum registered framework-owned shallow bytes in the window
/// @param phaseInvalidations invalidation counts by canonical phase
/// @param traces diagnostic traces emitted during the window
@NotNullByDefault
public record ProbeMetrics(
        @Unmodifiable Map<String, Long> callbacksExecuted,
        long nodesVisited,
        long dependencyEdgesAttached,
        long dependencyEdgesDetached,
        long activeDependencyEdges,
        long peakDependencyEdges,
        long retainedBytes,
        long peakRetainedBytes,
        @Unmodifiable Map<String, Long> phaseInvalidations,
        @Unmodifiable List<DiagnosticTrace> traces
) {
    /// Creates an immutable validated metrics snapshot.
    public ProbeMetrics {
        callbacksExecuted = metricMap(callbacksExecuted, "callbacksExecuted");
        ComparisonContracts.requireNonNegative(nodesVisited, "nodesVisited");
        ComparisonContracts.requireNonNegative(dependencyEdgesAttached, "dependencyEdgesAttached");
        ComparisonContracts.requireNonNegative(dependencyEdgesDetached, "dependencyEdgesDetached");
        ComparisonContracts.requireNonNegative(activeDependencyEdges, "activeDependencyEdges");
        ComparisonContracts.requireNonNegative(peakDependencyEdges, "peakDependencyEdges");
        if (peakDependencyEdges < activeDependencyEdges) {
            throw new IllegalArgumentException("peakDependencyEdges must cover activeDependencyEdges");
        }
        ComparisonContracts.requireNonNegative(retainedBytes, "retainedBytes");
        ComparisonContracts.requireNonNegative(peakRetainedBytes, "peakRetainedBytes");
        if (peakRetainedBytes < retainedBytes) {
            throw new IllegalArgumentException("peakRetainedBytes must cover retainedBytes");
        }
        phaseInvalidations = metricMap(phaseInvalidations, "phaseInvalidations");
        Objects.requireNonNull(traces, "traces");
        traces = List.copyOf(traces);
        for (DiagnosticTrace trace : traces) {
            Objects.requireNonNull(trace, "diagnostic trace");
        }
    }

    /// Returns the highest diagnostic quality score in this window.
    ///
    /// @return zero when no trace exists, otherwise the best trace score
    public int maximumTraceQuality() {
        int maximum = 0;
        for (DiagnosticTrace trace : traces) {
            maximum = Math.max(maximum, trace.qualityScore());
        }
        return maximum;
    }

    /// Returns the invalidation count for a phase.
    ///
    /// @param phase the phase
    /// @return the nonnegative count
    public long phaseInvalidations(RuntimePhase phase) {
        return phaseInvalidations.getOrDefault(canonical(phase), 0L);
    }

    /// Returns the difference in phase invalidations from an earlier snapshot.
    ///
    /// @param earlier the earlier snapshot from the same probe window
    /// @param phase the phase
    /// @return the nonnegative delta
    /// @throws IllegalArgumentException if the snapshots are not ordered
    public long phaseDeltaFrom(ProbeMetrics earlier, RuntimePhase phase) {
        long delta = phaseInvalidations(phase) - earlier.phaseInvalidations(phase);
        if (delta < 0L) {
            throw new IllegalArgumentException("Probe snapshots are not ordered");
        }
        return delta;
    }

    /// Validates and copies a metric map.
    ///
    /// @param values the metric map
    /// @param name the diagnostic name
    /// @return the immutable key-sorted copy
    private static @Unmodifiable Map<String, Long> metricMap(Map<String, Long> values, String name) {
        Map<String, Long> copy = ComparisonContracts.immutableSortedMap(values, name);
        for (Map.Entry<String, Long> entry : copy.entrySet()) {
            ComparisonContracts.requireNonNegative(entry.getValue(), name + '[' + entry.getKey() + ']');
        }
        return copy;
    }

    /// Returns the lower-camel report spelling of a phase.
    ///
    /// @param phase the phase
    /// @return the report key
    private static String canonical(RuntimePhase phase) {
        return switch (phase) {
            case STRUCTURE -> "structure";
            case MEASURE -> "measure";
            case PLACE -> "place";
            case PAINT -> "paint";
            case COMPOSITE -> "composite";
            case SEMANTICS -> "semantics";
            case HIT_TEST -> "hitTest";
        };
    }
}

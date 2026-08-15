package org.glavo.himari.spikes.runtime.sample;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/// Captures candidate-independent observable application state at one fixture checkpoint.
///
/// Values use stable fixture-defined paths. Node and event order is observable; diagnostics contain
/// stable diagnostic codes rather than implementation-specific prose.
///
/// @param values immutable path-to-value observations
/// @param mountedNodes immutable mounted-node identifiers in semantic order
/// @param events immutable lifecycle, effect, editing, or fallback events since the previous checkpoint
/// @param diagnostics immutable stable diagnostic codes since the previous checkpoint
@NotNullByDefault
public record FixtureObservation(
        @Unmodifiable Map<String, String> values,
        @Unmodifiable List<String> mountedNodes,
        @Unmodifiable List<String> events,
        @Unmodifiable List<String> diagnostics
) {
    /// Creates an immutable observation.
    public FixtureObservation {
        values = ComparisonContracts.immutableSortedMap(values, "observation values");
        mountedNodes = copyStrings(mountedNodes, "mountedNodes");
        events = copyStrings(events, "events");
        diagnostics = copyStrings(diagnostics, "diagnostics");
    }

    /// Creates an observation without events or diagnostics.
    ///
    /// @param values the observed values
    /// @param mountedNodes the mounted-node identifiers
    /// @return the observation
    public static FixtureObservation stable(
            @Unmodifiable Map<String, String> values,
            @Unmodifiable List<String> mountedNodes
    ) {
        return new FixtureObservation(values, mountedNodes, List.of(), List.of());
    }

    /// Copies and validates one ordered string list.
    ///
    /// @param values the source list
    /// @param name the diagnostic name
    /// @return the immutable copy
    private static @Unmodifiable List<String> copyStrings(@Unmodifiable List<String> values, String name) {
        Objects.requireNonNull(values, name);
        for (String value : values) {
            ComparisonContracts.requireText(value, name + " element");
        }
        return List.copyOf(values);
    }
}

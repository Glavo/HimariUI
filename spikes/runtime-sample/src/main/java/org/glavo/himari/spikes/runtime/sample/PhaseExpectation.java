package org.glavo.himari.spikes.runtime.sample;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Set;

/// Declares phases that a fixture action must invalidate at least once.
///
/// Additional invalidations remain measured rather than rejected so the comparison can expose
/// coarse candidates without prejudging the selected structural model.
///
/// @param required the phases required by the action's observable effect
@NotNullByDefault
public record PhaseExpectation(@Unmodifiable Set<RuntimePhase> required) {
    /// Creates an immutable phase expectation.
    public PhaseExpectation {
        required = ComparisonContracts.immutableEnumSet(required, RuntimePhase.class, "required phases");
    }

    /// Returns an expectation with no mandatory phase.
    ///
    /// @return the empty expectation
    public static PhaseExpectation none() {
        return new PhaseExpectation(Set.of());
    }

    /// Returns an expectation containing the specified phases.
    ///
    /// @param phases the required phases
    /// @return the expectation
    public static PhaseExpectation of(RuntimePhase... phases) {
        return new PhaseExpectation(Set.of(phases));
    }
}

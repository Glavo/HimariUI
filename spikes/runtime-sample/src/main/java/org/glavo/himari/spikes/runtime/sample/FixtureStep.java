package org.glavo.himari.spikes.runtime.sample;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/// Defines one command, checkpoint oracle, and optional capability condition.
///
/// @param id the stable step identifier within its fixture
/// @param command the application-domain command
/// @param requirement the capability condition, or `null` when every candidate runs the step
/// @param expected the exact candidate-independent observation after dispatch and Headless drain
/// @param phases phases that must have been invalidated while processing the command
@NotNullByDefault
public record FixtureStep(
        String id,
        FixtureCommand command,
        @Nullable CapabilityRequirement requirement,
        FixtureObservation expected,
        PhaseExpectation phases
) {
    /// Creates a validated step.
    public FixtureStep {
        id = ComparisonContracts.requireIdentifier(id, "fixture step id");
        Objects.requireNonNull(command, "command");
        Objects.requireNonNull(expected, "expected");
        Objects.requireNonNull(phases, "phases");
    }

    /// Returns whether this step applies to a candidate.
    ///
    /// @param capabilities the candidate capabilities
    /// @return whether the runner must execute the step
    public boolean appliesTo(CandidateCapabilities capabilities) {
        return requirement == null || requirement.matches(capabilities);
    }
}

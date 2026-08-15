package org.glavo.himari.spikes.runtime.sample;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Identifies one independently implemented structural-runtime candidate.
///
/// @param id the stable lower-kebab-case candidate identifier
/// @param displayName the human-readable candidate name
/// @param structuralModel the candidate-specific structural representation summary
/// @param capabilities the behavior used to select capability-dependent fixture paths
/// @param applicationCodeTransformed whether candidate application source requires generation or transformation
/// @param comparisonEligible whether this is a real decision candidate rather than a harness self-test
@NotNullByDefault
public record CandidateDescriptor(
        String id,
        String displayName,
        String structuralModel,
        CandidateCapabilities capabilities,
        boolean applicationCodeTransformed,
        boolean comparisonEligible
) {
    /// Creates a validated descriptor.
    public CandidateDescriptor {
        id = ComparisonContracts.requireIdentifier(id, "candidate id");
        displayName = ComparisonContracts.requireText(displayName, "candidate display name");
        structuralModel = ComparisonContracts.requireText(structuralModel, "structural model");
        Objects.requireNonNull(capabilities, "capabilities");
    }
}

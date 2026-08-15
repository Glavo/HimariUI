package org.glavo.himari.spikes.runtime.sample;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/// Selects a fixture step only when a candidate declares one canonical capability value.
///
/// @param capability the canonical capability key
/// @param value the required canonical value
@NotNullByDefault
public record CapabilityRequirement(String capability, String value) {
    /// Creates a validated requirement.
    public CapabilityRequirement {
        capability = ComparisonContracts.requireIdentifier(capability, "capability");
        value = ComparisonContracts.requireIdentifier(value, "capability value");
    }

    /// Returns whether the candidate declares the required value.
    ///
    /// @param capabilities the candidate capabilities
    /// @return whether the requirement matches
    public boolean matches(CandidateCapabilities capabilities) {
        Objects.requireNonNull(capabilities, "capabilities");
        @Nullable String actual = capabilities.value(capability);
        return value.equals(actual);
    }
}

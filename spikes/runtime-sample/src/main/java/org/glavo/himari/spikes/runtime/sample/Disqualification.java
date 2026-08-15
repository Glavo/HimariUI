package org.glavo.himari.spikes.runtime.sample;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

/// Records one non-compensatory failure without discarding partial comparison evidence.
///
/// @param code the stable lower-kebab-case failure code
/// @param fixtureId the fixture identifier, or `null` for a candidate-wide failure
/// @param detail the deterministic failure detail
@NotNullByDefault
public record Disqualification(String code, @Nullable String fixtureId, String detail) {
    /// Creates a validated disqualification.
    public Disqualification {
        code = ComparisonContracts.requireIdentifier(code, "disqualification code");
        if (fixtureId != null) {
            fixtureId = ComparisonContracts.requireIdentifier(fixtureId, "disqualification fixture id");
        }
        detail = ComparisonContracts.requireText(detail, "disqualification detail");
    }
}

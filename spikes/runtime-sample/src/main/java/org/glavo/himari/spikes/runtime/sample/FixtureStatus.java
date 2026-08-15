package org.glavo.himari.spikes.runtime.sample;

import org.jetbrains.annotations.NotNullByDefault;

/// Identifies the outcome of one fixture.
@NotNullByDefault
public enum FixtureStatus {
    /// Every applicable step, benchmark, and cleanup check passed.
    PASSED,

    /// At least one correctness, instrumentation, benchmark, or cleanup check failed.
    FAILED,

    /// An earlier checkpoint disqualified the candidate before this fixture ran.
    SKIPPED
}

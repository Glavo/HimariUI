package org.glavo.himari.spikes.runtime.sample;

import org.jetbrains.annotations.NotNullByDefault;

/// Declares whether staged UI attempts may be cancelled before commit.
@NotNullByDefault
public enum CancellationSupport {
    /// The candidate is non-preemptive and makes no cancellation claim.
    NONE,

    /// The candidate recognizes cancellation only at explicit cooperative checkpoints.
    COOPERATIVE,

    /// The candidate may preempt an in-progress staged attempt and guarantees complete cleanup.
    PREEMPTIVE
}

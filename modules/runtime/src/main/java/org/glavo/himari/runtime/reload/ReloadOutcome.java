package org.glavo.himari.runtime.reload;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Records one Headless-simulated reload generation.
///
/// @param generation the visible generation after the attempt, starting at `1` for the first apply
/// @param stateRetained whether compatible keyed values survived
/// @param callbackReplaced whether the application callback was swapped
/// @param effectRestarted whether owned effects were asked to restart
/// @param fallback the fallback that ran
/// @param failed whether the attempt was rejected without publishing a new generation
@NotNullByDefault
public record ReloadOutcome(
        int generation,
        boolean stateRetained,
        boolean callbackReplaced,
        boolean effectRestarted,
        ReloadFallback fallback,
        boolean failed
) {
    /// Validates the outcome.
    public ReloadOutcome {
        Objects.requireNonNull(fallback, "fallback");
        if (generation < 0) {
            throw new IllegalArgumentException("Reload generation must be nonnegative");
        }
    }
}

package org.glavo.himari.runtime.reload;

import org.jetbrains.annotations.NotNullByDefault;

/// Records one theme, style, image, or font publish attempt.
///
/// @param generation the visible generation after the attempt
/// @param published whether a new generation was installed
/// @param lastValidRetained whether the previous payload is still current
/// @param notifiedKeys the number of watched keys notified for this attempt
/// @param failed whether the attempt was rejected without publishing
@NotNullByDefault
public record ResourceReloadOutcome(
        int generation,
        boolean published,
        boolean lastValidRetained,
        int notifiedKeys,
        boolean failed
) {
    /// Validates the outcome.
    public ResourceReloadOutcome {
        if (generation < 0 || notifiedKeys < 0) {
            throw new IllegalArgumentException("Resource generation and notified-key count must be nonnegative");
        }
    }
}

package org.glavo.himari.platform.api;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Publishes a display color description with a semantic capability generation.
///
/// @param description the current color description
/// @param generation the nonnegative generation, advanced when the description changes
@NotNullByDefault
public record DisplayColorCapabilities(DisplayColorDescription description, long generation) {
    /// Creates display color capabilities.
    ///
    /// @throws IllegalArgumentException if `generation` is negative
    public DisplayColorCapabilities {
        Objects.requireNonNull(description, "description");
        if (generation < 0L) {
            throw new IllegalArgumentException("Color capability generation must be nonnegative");
        }
    }
}

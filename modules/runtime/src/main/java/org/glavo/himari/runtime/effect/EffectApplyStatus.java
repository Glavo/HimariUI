package org.glavo.himari.runtime.effect;

import org.jetbrains.annotations.NotNullByDefault;

/// Identifies the outcome of one post-commit effect apply.
@NotNullByDefault
public enum EffectApplyStatus {
    /// The current state epoch already received its effect apply.
    ALREADY_APPLIED,

    /// No keyed effect required mount, update, or cleanup.
    NO_CHANGES,

    /// At least one effect lifecycle callback ran.
    APPLIED,

    /// A lifecycle callback failed; remaining owned cleanup still ran.
    FAILED
}

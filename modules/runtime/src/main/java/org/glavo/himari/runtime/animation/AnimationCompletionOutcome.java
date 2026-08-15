package org.glavo.himari.runtime.animation;

import org.jetbrains.annotations.NotNullByDefault;

/// Identifies the terminal outcome of one animation transaction completion group.
@NotNullByDefault
public enum AnimationCompletionOutcome {
    /// Every active target reached its declared terminal condition.
    COMPLETED,

    /// At least one active target was superseded by a later transaction.
    REPLACED,

    /// Registry or property closure cancelled at least one active target.
    CANCELLED,

    /// Staging failed before any model or presentation mutation was committed.
    FAILED,

    /// The transaction produced no active timeline, including disabled-motion substitution.
    SKIPPED
}

package org.glavo.himari.runtime.structure;

import org.jetbrains.annotations.NotNullByDefault;

/// Identifies the externally observable result of one structural API call.
@NotNullByDefault
public enum StructuralAttemptStatus {
    /// No invalidated structural or measure group required execution.
    NO_CHANGES,

    /// The requested structure committed without an application failure.
    COMMITTED,

    /// A boundary contained a failure and a fresh fallback attempt committed.
    CONTAINED_FAILURE,

    /// Cooperative cancellation discarded every staged change.
    CANCELLED,

    /// No healthy boundary remained, so the runtime retained its last snapshot and stopped.
    ROOT_FAILED
}

package org.glavo.himari.runtime.mount;

import org.jetbrains.annotations.NotNullByDefault;

/// Identifies the outcome of one incremental mount apply.
@NotNullByDefault
public enum MountApplyStatus {
    /// No binding required a property publication.
    NO_CHANGES,

    /// At least one property target was published atomically.
    COMMITTED,

    /// Capture or commit failed and the previous property targets remain.
    FAILED
}

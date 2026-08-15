package org.glavo.himari.runtime;

import org.jetbrains.annotations.NotNullByDefault;

/// Identifies the scheduler operation that contained a failure.
@NotNullByDefault
public enum UiSchedulerFailureKind {
    /// One callback in an externally submitted state batch failed and rolled back to its savepoint.
    STATE_UPDATE,

    /// The outer transaction failed while publishing a complete externally submitted batch.
    STATE_BATCH,

    /// A window frame callback failed after frame admission.
    FRAME_CALLBACK,

    /// A previously accepted in-frame request could not obtain its follow-up host redraw.
    REDRAW_REQUEST
}

package org.glavo.himari.spikes.runtime.sample;

import org.jetbrains.annotations.NotNullByDefault;

/// Reports the result of a required external evidence run.
@NotNullByDefault
public enum EvidenceStatus {
    /// The evidence has not been collected.
    NOT_RUN,

    /// The evidence completed successfully and its artifact is recorded.
    PASSED,

    /// The evidence ran and failed.
    FAILED,

    /// The evidence does not apply to the declared candidate behavior.
    NOT_APPLICABLE
}

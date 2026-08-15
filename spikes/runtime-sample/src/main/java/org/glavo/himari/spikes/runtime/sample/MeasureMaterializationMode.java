package org.glavo.himari.spikes.runtime.sample;

import org.jetbrains.annotations.NotNullByDefault;

/// Declares how a candidate resolves structure that depends on measure-time viewport information.
@NotNullByDefault
public enum MeasureMaterializationMode {
    /// A bounded structural scope may materialize children during the current measure attempt.
    SCOPED_MEASURE_TIME,

    /// Structure consumes the previous committed viewport and may converge one frame later.
    PREVIOUS_VIEWPORT
}

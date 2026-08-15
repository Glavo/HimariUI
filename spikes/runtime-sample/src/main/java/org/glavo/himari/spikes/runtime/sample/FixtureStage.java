package org.glavo.himari.spikes.runtime.sample;

import org.jetbrains.annotations.NotNullByDefault;

/// Identifies the checkpoint at which a comparison fixture must run.
@NotNullByDefault
public enum FixtureStage {
    /// A bounded behavior or API-ceremony probe run before any realistic application is ported.
    MICRO,

    /// A multi-subsystem behavior probe run after all micro-fixtures pass.
    INTEGRATION,

    /// The approximately five-hundred-line API-charter application.
    REALISTIC
}

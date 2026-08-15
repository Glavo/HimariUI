package org.glavo.himari.spikes.runtime.sample;

import org.jetbrains.annotations.NotNullByDefault;

/// Identifies an independently tracked UI invalidation phase.
@NotNullByDefault
public enum RuntimePhase {
    /// Mounted topology or structural declarations must be reconsidered.
    STRUCTURE,

    /// Size negotiation must be rerun.
    MEASURE,

    /// Previously measured nodes must be repositioned.
    PLACE,

    /// Visual commands must be regenerated.
    PAINT,

    /// Retained-layer properties must be recomposited.
    COMPOSITE,

    /// The semantic accessibility representation must be updated.
    SEMANTICS,

    /// The spatial hit-test index must be updated.
    HIT_TEST
}

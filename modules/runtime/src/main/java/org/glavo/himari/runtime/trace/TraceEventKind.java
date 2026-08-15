package org.glavo.himari.runtime.trace;

import org.jetbrains.annotations.NotNullByDefault;

/// Identifies one deterministic runtime-trace record kind.
@NotNullByDefault
public enum TraceEventKind {
    /// A state-domain epoch publication.
    STATE_EPOCH,

    /// A structural attempt outcome.
    STRUCTURE_ATTEMPT,

    /// A mounted-property apply.
    MOUNT_APPLY,

    /// A keyed-effect apply.
    EFFECT_APPLY,

    /// An animation presentation sample.
    ANIMATION_SAMPLE
}

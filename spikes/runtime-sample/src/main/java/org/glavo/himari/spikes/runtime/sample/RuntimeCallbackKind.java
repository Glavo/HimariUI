package org.glavo.himari.spikes.runtime.sample;

import org.jetbrains.annotations.NotNullByDefault;

/// Classifies candidate callbacks counted by the comparison probe.
@NotNullByDefault
public enum RuntimeCallbackKind {
    /// A callback that may change mounted topology.
    STRUCTURE,

    /// A fine-grained property or value binding callback.
    BINDING,

    /// An application event-handler callback.
    EVENT,

    /// A measure callback.
    MEASURE,

    /// A placement callback.
    PLACEMENT,

    /// A paint callback.
    PAINT,

    /// An effect mount or update callback.
    EFFECT,

    /// An effect, owner, or staged-resource cleanup callback.
    CLEANUP
}

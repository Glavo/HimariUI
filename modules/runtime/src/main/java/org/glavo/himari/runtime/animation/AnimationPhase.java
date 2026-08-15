package org.glavo.himari.runtime.animation;

import org.jetbrains.annotations.NotNullByDefault;

/// Identifies one UI pipeline phase that a presentation-value change may invalidate.
@NotNullByDefault
public enum AnimationPhase {
    /// Mounted topology or structural declaration work.
    STRUCTURE,

    /// Size and intrinsic measurement work.
    MEASURE,

    /// Positioning work that reuses measured sizes.
    PLACE,

    /// Display-list or other paint recording work.
    PAINT,

    /// Retained-layer composition work.
    COMPOSITE,

    /// Accessibility and semantic-value work.
    SEMANTICS,

    /// Authoritative input hit-test index work.
    HIT_TEST
}

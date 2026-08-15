package org.glavo.himari.runtime.transition;

import org.jetbrains.annotations.NotNullByDefault;

/// Identifies one structural-transition presentation phase.
@NotNullByDefault
public enum TransitionPhase {
    /// Presentation progress is increasing toward a visible mounted element.
    ENTERING,

    /// Presentation is fully visible and participates in ordinary layout.
    VISIBLE,

    /// Presentation progress is decreasing after hide, detach, or removal.
    EXITING,

    /// No presentation remains after an exit completes or before the first show.
    GONE
}

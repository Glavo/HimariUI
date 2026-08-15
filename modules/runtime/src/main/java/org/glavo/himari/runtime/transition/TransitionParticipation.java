package org.glavo.himari.runtime.transition;

import org.jetbrains.annotations.NotNullByDefault;

/// Declares whether an exiting presentation still occupies layout space.
@NotNullByDefault
public enum TransitionParticipation {
    /// The exiting presentation continues to participate in layout.
    LAYOUT,

    /// The exiting presentation moves to a transition-owned overlay.
    OVERLAY
}

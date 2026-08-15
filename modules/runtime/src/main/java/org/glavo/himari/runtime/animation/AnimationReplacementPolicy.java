package org.glavo.himari.runtime.animation;

import org.jetbrains.annotations.NotNullByDefault;

/// Selects the starting presentation state when a new model target interrupts active motion.
@NotNullByDefault
public enum AnimationReplacementPolicy {
    /// Preserves current value and velocity when both old and new motions support compatible
    /// velocity retargeting; otherwise preserves value and resets velocity.
    PRESERVE_VELOCITY,

    /// Preserves current value and resets inferred velocity before starting the replacement. An
    /// explicit compatible gesture-handoff velocity may replace zero.
    PRESERVE_VALUE,

    /// Restarts from the previous committed model target with zero velocity.
    RESTART,

    /// Applies the replacement model target to presentation immediately.
    SNAP
}

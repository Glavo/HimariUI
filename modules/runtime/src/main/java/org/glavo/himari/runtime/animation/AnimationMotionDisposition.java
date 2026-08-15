package org.glavo.himari.runtime.animation;

import org.jetbrains.annotations.NotNullByDefault;

/// Records how presentation policy transformed the requested motion specification.
@NotNullByDefault
public enum AnimationMotionDisposition {
    /// Requested motion is used without accessibility substitution.
    STANDARD,

    /// Requested motion was transformed by a reduced-motion policy.
    REDUCED,

    /// Motion was disabled and the effective specification snaps to the target.
    DISABLED
}

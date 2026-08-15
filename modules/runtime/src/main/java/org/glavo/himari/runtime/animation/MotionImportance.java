package org.glavo.himari.runtime.animation;

import org.jetbrains.annotations.NotNullByDefault;

/// Declares whether a presentation target remains after reduced-motion substitution.
@NotNullByDefault
public enum MotionImportance {
    /// Nonessential motion snaps to its model target when reduced motion is active.
    NONESSENTIAL,

    /// Essential motion keeps a shortened, bounce-free specification when reduced motion is active.
    ESSENTIAL
}

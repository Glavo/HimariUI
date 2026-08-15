package org.glavo.himari.runtime.animation;

import org.jetbrains.annotations.NotNullByDefault;

/// Describes deterministic temporal evolution independently of an animatable value adapter.
@NotNullByDefault
public sealed interface MotionSpec permits SnapMotionSpec, SpringSpec, TweenSpec {
    /// Returns whether this specification produces no active timeline.
    ///
    /// @return whether targets apply immediately
    boolean isImmediate();

    /// Returns whether compatible replacement may preserve incoming scalar velocity.
    ///
    /// @return whether velocity-preserving retargeting is supported
    boolean supportsVelocityRetargeting();
}

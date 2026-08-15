package org.glavo.himari.runtime.animation;

import org.jetbrains.annotations.NotNullByDefault;

/// Applies a model target to presentation immediately without creating an active animation.
@NotNullByDefault
public enum SnapMotionSpec implements MotionSpec {
    /// The shared immediate-motion specification.
    INSTANCE;

    /// Returns that this specification is immediate.
    ///
    /// @return `true`
    @Override
    public boolean isImmediate() {
        return true;
    }

    /// Returns that an immediate change has no velocity-retargeting model.
    ///
    /// @return `false`
    @Override
    public boolean supportsVelocityRetargeting() {
        return false;
    }
}

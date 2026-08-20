package org.glavo.himari.layout.input.gesture;

import org.jetbrains.annotations.NotNullByDefault;

/// Converts requested scroll indices into a clamped window origin.
@FunctionalInterface
@NotNullByDefault
public interface ScrollPhysics {
    /// Clamps `index` into `[min, max]`.
    ///
    /// @param index the requested origin
    /// @param min the inclusive minimum
    /// @param max the inclusive maximum
    /// @return the accepted origin
    int clampIndex(int index, int min, int max);

    /// Applies signed `delta` to `offset` and then clamps.
    ///
    /// @param offset the current origin
    /// @param delta the signed step
    /// @param min the inclusive minimum
    /// @param max the inclusive maximum
    /// @return the accepted origin
    default int applyIndex(int offset, int delta, int min, int max) {
        return clampIndex(offset + delta, min, max);
    }

    /// Applies a signed pixel `delta` and clamps into `[min, max]`.
    ///
    /// @param offset the current offset
    /// @param delta the signed step
    /// @param min the inclusive minimum
    /// @param max the inclusive maximum
    /// @return the accepted offset
    default float applyOffset(float offset, float delta, float min, float max) {
        float next = offset + delta;
        if (next < min) {
            return min;
        }
        if (next > max) {
            return max;
        }
        return next;
    }

    /// Decays a fling velocity over `elapsedNanos`.
    ///
    /// A magnitude below the rest threshold becomes `0`.
    ///
    /// @param velocity the velocity in units per second
    /// @param elapsedNanos the nonnegative sample duration
    /// @return the remaining velocity
    default float decayVelocity(float velocity, long elapsedNanos) {
        return ClampingScrollPhysics.decay(velocity, elapsedNanos);
    }
}

package org.glavo.himari.runtime.animation;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/// Captures one scalar property's model target and atomically published presentation state.
///
/// @param propertyId the positive registry-local property identity
/// @param debugName the stable diagnostic name
/// @param closed whether the property stopped accepting targets
/// @param modelTarget the authoritative committed target
/// @param presentationValue the current presentation value
/// @param velocity the current scalar-per-second presentation velocity
/// @param active whether a motion timeline is active
/// @param transactionId the active transaction identity, or zero when inactive
/// @param replacementGeneration the number of semantically changed committed targets
/// @param phaseImpact the phases affected by presentation-value changes
/// @param effectiveMotion the active effective motion, or `null` when inactive
@NotNullByDefault
public record AnimatedScalarSnapshot(
        long propertyId,
        String debugName,
        boolean closed,
        double modelTarget,
        double presentationValue,
        double velocity,
        boolean active,
        long transactionId,
        long replacementGeneration,
        AnimationPhaseImpact phaseImpact,
        @Nullable MotionSpec effectiveMotion
) {
    /// Validates scalar state and active-motion presence.
    ///
    /// @throws IllegalArgumentException if an identity, generation, scalar, or lifecycle invariant
    /// is invalid
    public AnimatedScalarSnapshot {
        if (propertyId <= 0L) {
            throw new IllegalArgumentException("propertyId must be positive");
        }
        Objects.requireNonNull(debugName, "debugName");
        if (!Double.isFinite(modelTarget)
                || !Double.isFinite(presentationValue)
                || !Double.isFinite(velocity)) {
            throw new IllegalArgumentException("Animated scalar snapshots require finite values");
        }
        if (transactionId < 0L || replacementGeneration < 0L) {
            throw new IllegalArgumentException("Animation identities and generations must be non-negative");
        }
        Objects.requireNonNull(phaseImpact, "phaseImpact");
        if (active != (effectiveMotion != null) || active != (transactionId != 0L)) {
            throw new IllegalArgumentException("Active state must match motion and transaction presence");
        }
        if (closed && active) {
            throw new IllegalArgumentException("A closed scalar property must not remain active");
        }
    }
}

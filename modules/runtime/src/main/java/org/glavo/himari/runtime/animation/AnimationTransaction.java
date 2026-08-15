package org.glavo.himari.runtime.animation;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Carries immutable animation metadata through one atomic model-target commit.
///
/// The transaction is passed explicitly; it is not installed in process-global or thread-local
/// mutable state. `transactionId` also identifies its exactly-once completion group.
///
/// @param transactionId the positive monotonically increasing registry-local transaction identity
/// @param causalTraceId the nonnegative application or trace correlation identity
/// @param scopeId the nonnegative subtree or policy-scope identity
/// @param requestedMotion the motion requested by application policy
/// @param effectiveMotion the motion after accessibility and target policy resolution
/// @param replacementPolicy the interruption policy
/// @param motionDisposition how requested motion became effective motion
@NotNullByDefault
public record AnimationTransaction(
        long transactionId,
        long causalTraceId,
        long scopeId,
        MotionSpec requestedMotion,
        MotionSpec effectiveMotion,
        AnimationReplacementPolicy replacementPolicy,
        AnimationMotionDisposition motionDisposition
) {
    /// Validates identifiers and requested-versus-effective policy consistency.
    ///
    /// @throws IllegalArgumentException if an identity is outside its declared range or requested
    /// and effective motion contradict `motionDisposition`
    public AnimationTransaction {
        if (transactionId <= 0L) {
            throw new IllegalArgumentException("transactionId must be positive");
        }
        if (causalTraceId < 0L || scopeId < 0L) {
            throw new IllegalArgumentException("Animation correlation identifiers must be non-negative");
        }
        Objects.requireNonNull(requestedMotion, "requestedMotion");
        Objects.requireNonNull(effectiveMotion, "effectiveMotion");
        Objects.requireNonNull(replacementPolicy, "replacementPolicy");
        Objects.requireNonNull(motionDisposition, "motionDisposition");
        if (motionDisposition == AnimationMotionDisposition.STANDARD
                && !requestedMotion.equals(effectiveMotion)) {
            throw new IllegalArgumentException("Standard motion must preserve the requested specification");
        }
        if (motionDisposition == AnimationMotionDisposition.DISABLED
                && effectiveMotion != SnapMotionSpec.INSTANCE) {
            throw new IllegalArgumentException("Disabled motion must use the snap specification");
        }
    }

    /// Creates a standard transaction whose requested motion is effective unchanged.
    ///
    /// @param transactionId the positive transaction identity
    /// @param causalTraceId the nonnegative correlation identity
    /// @param scopeId the nonnegative policy-scope identity
    /// @param motion the requested and effective motion
    /// @param replacementPolicy the interruption policy
    /// @return the transaction metadata
    /// @throws IllegalArgumentException if an identity is outside its declared range
    public static AnimationTransaction standard(
            long transactionId,
            long causalTraceId,
            long scopeId,
            MotionSpec motion,
            AnimationReplacementPolicy replacementPolicy
    ) {
        return new AnimationTransaction(
                transactionId,
                causalTraceId,
                scopeId,
                motion,
                motion,
                replacementPolicy,
                AnimationMotionDisposition.STANDARD
        );
    }

    /// Creates a transaction transformed by a reduced-motion policy.
    ///
    /// @param transactionId the positive transaction identity
    /// @param causalTraceId the nonnegative correlation identity
    /// @param scopeId the nonnegative policy-scope identity
    /// @param requestedMotion the originally requested motion
    /// @param effectiveMotion the deterministic reduced-motion substitute
    /// @param replacementPolicy the interruption policy
    /// @return the transaction metadata
    /// @throws IllegalArgumentException if an identity is outside its declared range
    public static AnimationTransaction reduced(
            long transactionId,
            long causalTraceId,
            long scopeId,
            MotionSpec requestedMotion,
            MotionSpec effectiveMotion,
            AnimationReplacementPolicy replacementPolicy
    ) {
        return new AnimationTransaction(
                transactionId,
                causalTraceId,
                scopeId,
                requestedMotion,
                effectiveMotion,
                replacementPolicy,
                AnimationMotionDisposition.REDUCED
        );
    }

    /// Creates a disabled-motion transaction that applies targets immediately.
    ///
    /// @param transactionId the positive transaction identity
    /// @param causalTraceId the nonnegative correlation identity
    /// @param scopeId the nonnegative policy-scope identity
    /// @param requestedMotion the motion suppressed by policy
    /// @return the transaction metadata
    /// @throws IllegalArgumentException if an identity is outside its declared range
    public static AnimationTransaction disabled(
            long transactionId,
            long causalTraceId,
            long scopeId,
            MotionSpec requestedMotion
    ) {
        return new AnimationTransaction(
                transactionId,
                causalTraceId,
                scopeId,
                requestedMotion,
                SnapMotionSpec.INSTANCE,
                AnimationReplacementPolicy.SNAP,
                AnimationMotionDisposition.DISABLED
        );
    }
}

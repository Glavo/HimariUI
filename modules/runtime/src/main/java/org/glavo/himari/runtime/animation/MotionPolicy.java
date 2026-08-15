package org.glavo.himari.runtime.animation;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Resolves requested motion into an effective specification under accessibility policy.
///
/// Reduced motion is a per-target transformation of the requested [MotionSpec]. Nonessential
/// targets snap immediately. Essential targets keep a deterministic shortened tween or a
/// critically damped spring. The caller records both requested and effective specifications on
/// the resulting [AnimationTransaction].
@NotNullByDefault
public final class MotionPolicy {
    /// Maximum active duration retained for essential tweens under reduced motion.
    public static final long REDUCED_TWEEN_MAX_NANOS = 80_000_000L;

    /// Prevents instantiation.
    private MotionPolicy() {
    }

    /// Returns the effective specification after applying reduced-motion policy.
    ///
    /// @param requested the motion requested by application policy
    /// @param reducedMotion whether reduced motion is in effect for this presentation target
    /// @param importance whether the target is essential
    /// @return the effective specification
    public static MotionSpec transform(
            MotionSpec requested,
            boolean reducedMotion,
            MotionImportance importance
    ) {
        Objects.requireNonNull(requested, "requested");
        Objects.requireNonNull(importance, "importance");
        if (!reducedMotion) {
            return requested;
        }
        if (importance == MotionImportance.NONESSENTIAL) {
            return SnapMotionSpec.INSTANCE;
        }
        return switch (requested) {
            case SnapMotionSpec snap -> snap;
            case TweenSpec tween -> tween.shortened(REDUCED_TWEEN_MAX_NANOS);
            case SpringSpec spring -> spring.withoutBounce();
        };
    }

    /// Returns how requested motion becomes effective motion.
    ///
    /// @param reducedMotion whether reduced motion is in effect
    /// @param importance whether the target is essential
    /// @return the disposition
    public static AnimationMotionDisposition disposition(
            boolean reducedMotion,
            MotionImportance importance
    ) {
        Objects.requireNonNull(importance, "importance");
        if (!reducedMotion) {
            return AnimationMotionDisposition.STANDARD;
        }
        if (importance == MotionImportance.NONESSENTIAL) {
            return AnimationMotionDisposition.DISABLED;
        }
        return AnimationMotionDisposition.REDUCED;
    }

    /// Creates a transaction whose effective motion is resolved from reduced-motion policy.
    ///
    /// @param reducedMotion whether reduced motion is in effect for this presentation target
    /// @param importance whether the target is essential
    /// @param transactionId the positive transaction identity
    /// @param causalTraceId the nonnegative correlation identity
    /// @param scopeId the nonnegative policy-scope identity
    /// @param requested the motion requested by application policy
    /// @param replacementPolicy the interruption policy used when motion remains active
    /// @return the resolved transaction
    public static AnimationTransaction resolve(
            boolean reducedMotion,
            MotionImportance importance,
            long transactionId,
            long causalTraceId,
            long scopeId,
            MotionSpec requested,
            AnimationReplacementPolicy replacementPolicy
    ) {
        MotionSpec effective = transform(requested, reducedMotion, importance);
        return switch (disposition(reducedMotion, importance)) {
            case STANDARD -> AnimationTransaction.standard(
                    transactionId,
                    causalTraceId,
                    scopeId,
                    requested,
                    replacementPolicy
            );
            case REDUCED -> AnimationTransaction.reduced(
                    transactionId,
                    causalTraceId,
                    scopeId,
                    requested,
                    effective,
                    replacementPolicy
            );
            case DISABLED -> AnimationTransaction.disabled(
                    transactionId,
                    causalTraceId,
                    scopeId,
                    requested
            );
        };
    }
}

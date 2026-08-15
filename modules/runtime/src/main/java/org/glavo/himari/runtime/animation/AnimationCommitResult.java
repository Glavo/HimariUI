package org.glavo.himari.runtime.animation;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/// Summarizes one successful atomic animation target commit.
///
/// @param transactionId the committed transaction identity
/// @param timestampNanos the single clock timestamp used for interruption and commit
/// @param changedTargetCount the number of semantically changed model targets
/// @param activeAnimationCount the registry-wide active count after commit
/// @param presentationEpoch the presentation epoch after commit
/// @param phaseImpact phases invalidated by immediate presentation changes
/// @param immediateOutcome the terminal outcome emitted by this commit, or `null` while active
@NotNullByDefault
public record AnimationCommitResult(
        long transactionId,
        long timestampNanos,
        int changedTargetCount,
        int activeAnimationCount,
        long presentationEpoch,
        AnimationPhaseImpact phaseImpact,
        @Nullable AnimationCompletionOutcome immediateOutcome
) {
    /// Validates identifiers, counts, and phase impact.
    ///
    /// @throws IllegalArgumentException if the transaction identity is not positive or a timestamp,
    /// epoch, or count is negative
    public AnimationCommitResult {
        if (transactionId <= 0L) {
            throw new IllegalArgumentException("transactionId must be positive");
        }
        if (timestampNanos < 0L || presentationEpoch < 0L) {
            throw new IllegalArgumentException("Animation timestamps and epochs must be non-negative");
        }
        if (changedTargetCount < 0 || activeAnimationCount < 0) {
            throw new IllegalArgumentException("Animation counts must be non-negative");
        }
        Objects.requireNonNull(phaseImpact, "phaseImpact");
    }
}

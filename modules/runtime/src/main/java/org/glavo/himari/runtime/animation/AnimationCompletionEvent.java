package org.glavo.himari.runtime.animation;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Records the exactly-once terminal outcome of one animation transaction.
///
/// Completion is data rather than an application callback executed by the sampler. A later runtime
/// stabilization boundary may drain events and invoke effects without making sampling reentrant.
///
/// @param transactionId the positive transaction and completion-group identity
/// @param timestampNanos the nonnegative terminal timestamp
/// @param outcome the terminal group outcome
/// @param targetCount the number of semantically changed model targets in the transaction
@NotNullByDefault
public record AnimationCompletionEvent(
        long transactionId,
        long timestampNanos,
        AnimationCompletionOutcome outcome,
        int targetCount
) {
    /// Validates identity, timestamp, and target count.
    ///
    /// @throws IllegalArgumentException if the identity is not positive or a timestamp or count is
    /// negative
    public AnimationCompletionEvent {
        if (transactionId <= 0L) {
            throw new IllegalArgumentException("transactionId must be positive");
        }
        if (timestampNanos < 0L) {
            throw new IllegalArgumentException("timestampNanos must be non-negative");
        }
        Objects.requireNonNull(outcome, "outcome");
        if (targetCount < 0) {
            throw new IllegalArgumentException("targetCount must be non-negative");
        }
    }
}

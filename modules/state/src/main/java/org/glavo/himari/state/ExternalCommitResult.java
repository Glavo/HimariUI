package org.glavo.himari.state;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Objects;

/// Summarizes one [ExternalStateCommitQueue#drain()] operation.
///
/// A successful update is one whose callback returned normally; it need not have changed a source.
/// All successful updates from the captured batch share the single outer state transaction.
///
/// @param attemptedCount the number of callbacks captured and invoked
/// @param successfulCount the number of callbacks that returned normally
/// @param epochBefore the domain epoch before batch execution
/// @param epochAfter the domain epoch after successful writes were published
/// @param failures the immutable failures in FIFO batch order
@NotNullByDefault
public record ExternalCommitResult(
        int attemptedCount,
        int successfulCount,
        long epochBefore,
        long epochAfter,
        @Unmodifiable List<ExternalCommitFailure> failures
) {
    /// Validates counts and defensively copies the failure list.
    public ExternalCommitResult {
        if (attemptedCount < 0) {
            throw new IllegalArgumentException("attemptedCount must be non-negative");
        }
        if (successfulCount < 0 || successfulCount > attemptedCount) {
            throw new IllegalArgumentException("successfulCount must be between zero and attemptedCount");
        }
        Objects.requireNonNull(failures, "failures");
        failures = List.copyOf(failures);
        if (successfulCount + failures.size() != attemptedCount) {
            throw new IllegalArgumentException("Every attempted callback must either succeed or fail");
        }
        if (epochAfter < epochBefore) {
            throw new IllegalArgumentException("epochAfter must not precede epochBefore");
        }
    }

    /// Returns whether at least one semantic state change was published.
    ///
    /// @return whether the domain epoch advanced
    public boolean changed() {
        return epochAfter != epochBefore;
    }

    /// Returns whether every captured callback returned normally.
    ///
    /// @return whether the failure list is empty
    public boolean allSucceeded() {
        return failures.isEmpty();
    }
}

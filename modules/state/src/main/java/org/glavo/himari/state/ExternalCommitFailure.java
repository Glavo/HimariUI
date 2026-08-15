package org.glavo.himari.state;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Describes one failed update in a drained external-commit batch.
///
/// @param batchIndex the zero-based position in the captured FIFO batch
/// @param cause the exception or error thrown by the update
@NotNullByDefault
public record ExternalCommitFailure(int batchIndex, Throwable cause) {
    /// Validates the batch position and failure cause.
    public ExternalCommitFailure {
        if (batchIndex < 0) {
            throw new IllegalArgumentException("batchIndex must be non-negative");
        }
        Objects.requireNonNull(cause, "cause");
    }
}

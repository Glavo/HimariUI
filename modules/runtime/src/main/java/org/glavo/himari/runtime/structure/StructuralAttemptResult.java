package org.glavo.himari.runtime.structure;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/// Summarizes one normal structural update or current-measure materialization call.
///
/// @param status externally observable attempt result
/// @param revisionBefore committed revision before the call
/// @param revisionAfter committed revision after the call
/// @param stateEpoch stable state epoch used by executed attempts
/// @param attemptedGroupCount number of root drafts executed across fresh attempts
/// @param committedGroupCount number of group drafts published by the final successful attempt
/// @param failure originating contained or root failure, or `null` otherwise
@NotNullByDefault
public record StructuralAttemptResult(
        StructuralAttemptStatus status,
        long revisionBefore,
        long revisionAfter,
        long stateEpoch,
        int attemptedGroupCount,
        int committedGroupCount,
        @Nullable StructuralFailure failure
) {
    /// Validates one result.
    public StructuralAttemptResult {
        Objects.requireNonNull(status, "status");
        if (revisionBefore < 0L || revisionAfter < revisionBefore || stateEpoch < 0L) {
            throw new IllegalArgumentException("Attempt revisions and state epoch are inconsistent");
        }
        if (attemptedGroupCount < 0 || committedGroupCount < 0) {
            throw new IllegalArgumentException("Attempt group counts must be nonnegative");
        }
    }
}

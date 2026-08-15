package org.glavo.himari.runtime.mount;

import org.glavo.himari.runtime.animation.AnimationPhaseImpact;
import org.glavo.himari.runtime.animation.AnimationTransaction;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/// Reports one atomic UI commit of topology, property targets, and inherited animation metadata.
///
/// @param revision the mount-tree revision after the commit
/// @param insertedElementCount newly visible mounted elements
/// @param removedElementCount disposed mounted elements
/// @param changedBindingCount bindings whose semantic value changed
/// @param appliedPhases the union of phase impacts for changed bindings
/// @param animationTransaction the staged animation metadata, or `null`
@NotNullByDefault
public record UiCommitResult(
        long revision,
        int insertedElementCount,
        int removedElementCount,
        int changedBindingCount,
        AnimationPhaseImpact appliedPhases,
        @Nullable AnimationTransaction animationTransaction
) {
    /// Validates one commit result.
    public UiCommitResult {
        if (revision < 0L) {
            throw new IllegalArgumentException("revision must be nonnegative");
        }
        if (insertedElementCount < 0 || removedElementCount < 0 || changedBindingCount < 0) {
            throw new IllegalArgumentException("Commit counts must be nonnegative");
        }
        Objects.requireNonNull(appliedPhases, "appliedPhases");
    }
}

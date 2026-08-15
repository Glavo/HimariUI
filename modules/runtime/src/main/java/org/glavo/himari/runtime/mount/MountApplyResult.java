package org.glavo.himari.runtime.mount;

import org.glavo.himari.runtime.animation.AnimationPhaseImpact;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/// Reports one incremental mount apply.
///
/// @param status whether property targets were published
/// @param revision the mount-tree revision after the apply
/// @param stateEpoch the observed state-domain epoch
/// @param changedBindingCount bindings whose semantic value changed
/// @param appliedPhases the union of phase impacts for changed bindings
/// @param failure a contained apply failure, or `null`
@NotNullByDefault
public record MountApplyResult(
        MountApplyStatus status,
        long revision,
        long stateEpoch,
        int changedBindingCount,
        AnimationPhaseImpact appliedPhases,
        @Nullable String failure
) {
    /// Validates one apply result.
    public MountApplyResult {
        Objects.requireNonNull(status, "status");
        if (revision < 0L || stateEpoch < 0L) {
            throw new IllegalArgumentException("Revision and state epoch must be nonnegative");
        }
        if (changedBindingCount < 0) {
            throw new IllegalArgumentException("changedBindingCount must be nonnegative");
        }
        Objects.requireNonNull(appliedPhases, "appliedPhases");
        if (status == MountApplyStatus.FAILED && (failure == null || failure.isBlank())) {
            throw new IllegalArgumentException("Failed apply must include a diagnostic");
        }
        if (status != MountApplyStatus.FAILED && failure != null) {
            throw new IllegalArgumentException("Successful apply cannot include a diagnostic");
        }
    }
}

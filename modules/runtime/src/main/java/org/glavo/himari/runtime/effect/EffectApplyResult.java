package org.glavo.himari.runtime.effect;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/// Reports one post-commit keyed-effect apply.
///
/// @param status whether lifecycle callbacks ran
/// @param stateEpoch the state-domain epoch consumed by this apply
/// @param mountedCount newly mounted effects
/// @param updatedCount existing effects whose dependency identity changed
/// @param cleanedCount effects removed from the live set
/// @param failure a contained lifecycle diagnostic, or `null`
@NotNullByDefault
public record EffectApplyResult(
        EffectApplyStatus status,
        long stateEpoch,
        int mountedCount,
        int updatedCount,
        int cleanedCount,
        @Nullable String failure
) {
    /// Validates one apply result.
    public EffectApplyResult {
        Objects.requireNonNull(status, "status");
        if (stateEpoch < 0L) {
            throw new IllegalArgumentException("stateEpoch must be nonnegative");
        }
        if (mountedCount < 0 || updatedCount < 0 || cleanedCount < 0) {
            throw new IllegalArgumentException("Effect counts must be nonnegative");
        }
        if (status == EffectApplyStatus.FAILED && (failure == null || failure.isBlank())) {
            throw new IllegalArgumentException("Failed apply must include a diagnostic");
        }
        if (status != EffectApplyStatus.FAILED && failure != null) {
            throw new IllegalArgumentException("Successful apply cannot include a diagnostic");
        }
    }
}

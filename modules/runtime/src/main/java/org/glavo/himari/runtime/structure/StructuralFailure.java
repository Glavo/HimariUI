package org.glavo.himari.runtime.structure;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Objects;

/// Reports one contained application failure and its deterministic recovery action.
///
/// @param sequence positive runtime-local diagnostic sequence
/// @param code stable failure code
/// @param phase callback phase
/// @param ownerPath group path at the failure point
/// @param boundaryPath selected boundary path, or `root` for root containment
/// @param recoveryAction stable recovery action text
/// @param cause callback failure in debug mode, or `null` in release mode
/// @param cleanupFailures immutable cleanup failures aggregated under this failure
@NotNullByDefault
public record StructuralFailure(
        long sequence,
        String code,
        StructuralCallbackPhase phase,
        String ownerPath,
        String boundaryPath,
        String recoveryAction,
        @Nullable Throwable cause,
        @Unmodifiable List<StructuralCleanupFailure> cleanupFailures
) {
    /// Validates and snapshots one failure.
    public StructuralFailure {
        if (sequence < 1L) {
            throw new IllegalArgumentException("sequence must be positive");
        }
        code = StructuralContracts.requireName(code, "code");
        Objects.requireNonNull(phase, "phase");
        ownerPath = StructuralContracts.requireName(ownerPath, "ownerPath");
        boundaryPath = StructuralContracts.requireName(boundaryPath, "boundaryPath");
        recoveryAction = StructuralContracts.requireName(recoveryAction, "recoveryAction");
        cleanupFailures = List.copyOf(cleanupFailures);
    }
}

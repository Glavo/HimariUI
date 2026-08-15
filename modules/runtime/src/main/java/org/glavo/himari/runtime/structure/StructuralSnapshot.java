package org.glavo.himari.runtime.structure;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Objects;

/// Captures the committed structural topology and runtime state.
///
/// @param revision committed UI revision, initially zero
/// @param stateEpoch state-domain epoch represented by the latest commit
/// @param status runtime lifecycle status
/// @param groups immutable depth-first groups, with active children before dormant children
/// @param pendingFailureCount retained diagnostic count
@NotNullByDefault
public record StructuralSnapshot(
        long revision,
        long stateEpoch,
        StructuralRuntimeStatus status,
        @Unmodifiable List<StructuralGroupSnapshot> groups,
        int pendingFailureCount
) {
    /// Validates and snapshots one runtime state.
    public StructuralSnapshot {
        if (revision < 0L || stateEpoch < 0L) {
            throw new IllegalArgumentException("Revision and state epoch must be nonnegative");
        }
        Objects.requireNonNull(status, "status");
        groups = List.copyOf(groups);
        if (pendingFailureCount < 0) {
            throw new IllegalArgumentException("pendingFailureCount must be nonnegative");
        }
    }
}

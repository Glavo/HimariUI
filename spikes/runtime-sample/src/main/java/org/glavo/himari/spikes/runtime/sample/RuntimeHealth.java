package org.glavo.himari.spikes.runtime.sample;

import org.jetbrains.annotations.NotNullByDefault;

/// Reports candidate-owned live resources after one fixture session closes.
///
/// @param liveNodes mounted or staged nodes still reachable from the session
/// @param liveOwners reactive or structural owners still registered
/// @param liveEffects mounted effects or cleanup records still registered
/// @param stagedMutations uncommitted topology or property mutations
/// @param pendingCallbacks candidate callbacks still queued outside the Headless event loop
@NotNullByDefault
public record RuntimeHealth(
        long liveNodes,
        long liveOwners,
        long liveEffects,
        long stagedMutations,
        long pendingCallbacks
) {
    /// The canonical leak-free health snapshot.
    public static final RuntimeHealth CLEAN = new RuntimeHealth(0L, 0L, 0L, 0L, 0L);

    /// Creates a validated health snapshot.
    public RuntimeHealth {
        ComparisonContracts.requireNonNegative(liveNodes, "liveNodes");
        ComparisonContracts.requireNonNegative(liveOwners, "liveOwners");
        ComparisonContracts.requireNonNegative(liveEffects, "liveEffects");
        ComparisonContracts.requireNonNegative(stagedMutations, "stagedMutations");
        ComparisonContracts.requireNonNegative(pendingCallbacks, "pendingCallbacks");
    }

    /// Returns whether no candidate-owned resource remains live.
    ///
    /// @return whether all counts are zero
    public boolean clean() {
        return equals(CLEAN);
    }
}

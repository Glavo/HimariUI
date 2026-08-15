package org.glavo.himari.runtime.structure;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Objects;

/// Describes one committed structural group without exposing its storage record.
///
/// @param groupId positive runtime-local identity preserved by reconciliation
/// @param ownerPath deterministic group/key path
/// @param sourceIdentity handwritten source identity
/// @param semanticKey diagnostic semantic-key text, or `null` for positional identity
/// @param state active or retained-dormant state
/// @param measureMaterialization whether the group is layout-owned materialization scope
/// @param boundaryStatus boundary recovery state, or `null` for a non-boundary group
/// @param rememberedSlotIds immutable positional-memory identities
/// @param activeChildCount number of directly active children
/// @param dormantChildCount number of directly retained dormant children
@NotNullByDefault
public record StructuralGroupSnapshot(
        long groupId,
        String ownerPath,
        String sourceIdentity,
        @Nullable String semanticKey,
        StructuralGroupState state,
        boolean measureMaterialization,
        @Nullable ErrorBoundaryStatus boundaryStatus,
        @Unmodifiable List<Long> rememberedSlotIds,
        int activeChildCount,
        int dormantChildCount
) {
    /// Validates and snapshots one group.
    public StructuralGroupSnapshot {
        if (groupId < 1L) {
            throw new IllegalArgumentException("groupId must be positive");
        }
        ownerPath = StructuralContracts.requireName(ownerPath, "ownerPath");
        sourceIdentity = StructuralContracts.requireName(sourceIdentity, "sourceIdentity");
        Objects.requireNonNull(state, "state");
        rememberedSlotIds = List.copyOf(rememberedSlotIds);
        if (activeChildCount < 0 || dormantChildCount < 0) {
            throw new IllegalArgumentException("Child counts must be nonnegative");
        }
    }
}

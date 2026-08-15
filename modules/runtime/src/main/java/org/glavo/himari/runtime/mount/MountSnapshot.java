package org.glavo.himari.runtime.mount;

import org.glavo.himari.runtime.animation.AnimationPhaseImpact;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Objects;

/// Captures committed mounted elements and the latest incremental apply result.
///
/// @param revision the nonnegative mount-tree revision
/// @param stateEpoch the state-domain epoch represented by the latest apply
/// @param elements committed elements in declaration order
/// @param lastAppliedPhases the phase union published by the latest successful apply
@NotNullByDefault
public record MountSnapshot(
        long revision,
        long stateEpoch,
        @Unmodifiable List<MountedElement> elements,
        AnimationPhaseImpact lastAppliedPhases
) {
    /// Validates one mount snapshot.
    public MountSnapshot {
        if (revision < 0L || stateEpoch < 0L) {
            throw new IllegalArgumentException("Revision and state epoch must be nonnegative");
        }
        elements = List.copyOf(elements);
        Objects.requireNonNull(lastAppliedPhases, "lastAppliedPhases");
    }

    /// Returns the committed element with the given identity.
    ///
    /// @param identity the mount identity
    /// @return the element
    /// @throws IllegalArgumentException if the identity is not committed
    public MountedElement element(MountIdentity identity) {
        Objects.requireNonNull(identity, "identity");
        for (MountedElement element : elements) {
            if (element.identity().equals(identity)) {
                return element;
            }
        }
        throw new IllegalArgumentException("Unknown mounted element: " + identity);
    }
}

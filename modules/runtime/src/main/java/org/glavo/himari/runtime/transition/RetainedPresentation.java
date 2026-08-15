package org.glavo.himari.runtime.transition;

import org.glavo.himari.platform.api.LogicalRect;
import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Holds the immutable presentation data retained after an element's owner is disposed.
///
/// Removal drops the element from focus, hit testing, and semantics. The retained record exists
/// only so an exit presentation can keep drawing until it reaches [TransitionPhase#GONE].
///
/// @param identity the structural identity
/// @param debugName the diagnostic name
/// @param bounds the last committed presentation bounds
/// @param participation whether the exit occupies layout or an overlay
/// @param ownerDisposed whether the element's owner was disposed
@NotNullByDefault
public record RetainedPresentation(
        TransitionIdentity identity,
        String debugName,
        LogicalRect bounds,
        TransitionParticipation participation,
        boolean ownerDisposed
) {
    /// Validates the retained presentation.
    public RetainedPresentation {
        Objects.requireNonNull(identity, "identity");
        Objects.requireNonNull(debugName, "debugName");
        Objects.requireNonNull(bounds, "bounds");
        Objects.requireNonNull(participation, "participation");
    }
}

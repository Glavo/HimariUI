package org.glavo.himari.runtime.effect;

import org.jetbrains.annotations.NotNullByDefault;

/// Receives keyed effect lifecycle events after a successful UI commit.
///
/// Callbacks run on the [org.glavo.himari.state.StateDomain] owner thread. They must not write
/// application [org.glavo.himari.state.State] directly; asynchronous completion posts work through
/// the domain external-commit queue.
@NotNullByDefault
public interface EffectCallbacks {
    /// Activates the effect after its first successful commit.
    ///
    /// @param session the owner-thread session for this activation
    void onMount(EffectSession session);

    /// Updates an already-mounted effect after its dependency identity changes.
    ///
    /// @param session the owner-thread session for this update
    void onUpdate(EffectSession session);

    /// Releases every resource owned by this effect.
    ///
    /// Cleanup runs child-before-parent relative to later host-owned cleanup and continues after
    /// a failure so remaining owned work still executes.
    void onCleanup();
}

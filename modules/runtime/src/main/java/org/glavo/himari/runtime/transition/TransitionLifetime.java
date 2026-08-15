package org.glavo.himari.runtime.transition;

import org.jetbrains.annotations.NotNullByDefault;

/// Distinguishes hidden, detached, and removed element lifetimes.
///
/// Hidden and detached elements retain local state. Removal disposes the element's owner even
/// when an exit presentation is still visible.
@NotNullByDefault
public enum TransitionLifetime {
    /// The element is mounted and its owner remains live.
    MOUNTED,

    /// The element is hidden and retains local state.
    HIDDEN,

    /// The element is detached from the tree and retains local state.
    DETACHED,

    /// The element is removed and its owner is disposed.
    REMOVED
}

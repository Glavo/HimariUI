package org.glavo.himari.runtime.structure;

import org.jetbrains.annotations.NotNullByDefault;

/// Selects the lifetime of a conditional group's inactive state.
@NotNullByDefault
public enum BranchRetention {
    /// Disposes the hidden group, including local memory, effects, resources, and dependencies.
    DISPOSE,

    /// Keeps local memory and identity while deactivating effects and reactive dependencies.
    RETAIN
}

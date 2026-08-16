package org.glavo.himari.runtime.reload;

import org.jetbrains.annotations.NotNullByDefault;

/// Names the explicit fallback chosen when a reload generation cannot retain state.
@NotNullByDefault
public enum ReloadFallback {
    /// Compatible generation: keyed state is kept and callbacks are replaced.
    NONE,

    /// Discard local state and effects below a declared boundary.
    SUBTREE_RESET,

    /// Dispose and recreate every application UI root in this process.
    FULL_UI_RESET,

    /// Start a new process and drop all non-persisted state.
    PROCESS_RESTART
}

package org.glavo.himari.runtime.structure;

import org.jetbrains.annotations.NotNullByDefault;

/// Identifies the callback category that produced a structural diagnostic.
@NotNullByDefault
public enum StructuralCallbackPhase {
    /// A normal restartable group callback failed.
    STRUCTURE,

    /// A scoped current-measure materialization callback failed.
    MEASURE_MATERIALIZATION,

    /// A newly committed structural effect failed to mount.
    EFFECT_MOUNT,

    /// An effect or remembered resource failed during child-first cleanup.
    CLEANUP
}

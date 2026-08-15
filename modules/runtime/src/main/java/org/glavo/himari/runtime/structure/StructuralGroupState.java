package org.glavo.himari.runtime.structure;

import org.jetbrains.annotations.NotNullByDefault;

/// Identifies whether a committed group participates in active structure.
@NotNullByDefault
public enum StructuralGroupState {
    /// The group participates in the current topology.
    ACTIVE,

    /// A retain-on-hide branch preserves identity and memory without effects or dependencies.
    DORMANT
}

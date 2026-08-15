package org.glavo.himari.runtime.structure;

import org.jetbrains.annotations.NotNullByDefault;

/// Identifies whether a structural runtime accepts application callbacks.
@NotNullByDefault
public enum StructuralRuntimeStatus {
    /// The runtime may execute structural or measure attempts.
    ACTIVE,

    /// An uncontained root failure stopped callbacks until explicit reset.
    FAILED,

    /// The runtime released all owned groups and no longer accepts operations.
    CLOSED
}

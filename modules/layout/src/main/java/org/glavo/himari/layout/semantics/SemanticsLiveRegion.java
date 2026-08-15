package org.glavo.himari.layout.semantics;

import org.jetbrains.annotations.NotNullByDefault;

/// Declares how a semantics node announces changes to assistive technology.
@NotNullByDefault
public enum SemanticsLiveRegion {
    /// The node is not a live region.
    OFF,

    /// Changes may be announced when the user is idle.
    POLITE,

    /// Changes must interrupt the current announcement.
    ASSERTIVE
}

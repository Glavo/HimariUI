package org.glavo.himari.runtime.structure;

import org.jetbrains.annotations.NotNullByDefault;

/// Identifies the recovery state of a declared error boundary.
@NotNullByDefault
public enum ErrorBoundaryStatus {
    /// Normal content executes when the boundary is composed.
    HEALTHY,

    /// Normal content failed and only declared fallback content may execute until reset.
    FAILED,

    /// Fallback content failed and containment escalated to a parent boundary.
    ESCALATED
}

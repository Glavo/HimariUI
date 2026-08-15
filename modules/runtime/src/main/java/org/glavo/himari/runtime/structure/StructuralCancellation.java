package org.glavo.himari.runtime.structure;

import org.jetbrains.annotations.NotNullByDefault;

/// Supplies an any-thread cooperative-cancellation flag to structural work.
///
/// Cancellation never interrupts a callback. A structural or measure callback observes it only at
/// an explicit [StructuralScope#checkpoint()] or [MeasureStructuralScope#checkpoint()] call.
@NotNullByDefault
public final class StructuralCancellation {
    /// Whether cancellation has been requested.
    private volatile boolean cancelled;

    /// Creates a non-cancelled token.
    public StructuralCancellation() {
    }

    /// Requests cancellation.
    ///
    /// Repeated and concurrent calls have no additional effect.
    public void cancel() {
        cancelled = true;
    }

    /// Returns whether cancellation was requested.
    ///
    /// @return the current flag
    public boolean isCancelled() {
        return cancelled;
    }
}

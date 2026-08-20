package org.glavo.himari.layout.input.gesture;

import org.jetbrains.annotations.NotNullByDefault;

/// Allows a bounded overscroll past `[min, max]` before the origin is accepted.
@NotNullByDefault
public final class BouncingScrollPhysics implements ScrollPhysics {
    /// Extra items permitted past each edge.
    private final int overscroll;

    /// Creates a bounce policy.
    ///
    /// @param overscroll the nonnegative extra range past each edge
    public BouncingScrollPhysics(int overscroll) {
        if (overscroll < 0) {
            throw new IllegalArgumentException("overscroll must be nonnegative");
        }
        this.overscroll = overscroll;
    }

    /// Returns the extra range past each edge.
    ///
    /// @return the overscroll
    public int overscroll() {
        return overscroll;
    }

    /// {@inheritDoc}
    @Override
    public int clampIndex(int index, int min, int max) {
        return Math.min(max + overscroll, Math.max(min - overscroll, index));
    }
}

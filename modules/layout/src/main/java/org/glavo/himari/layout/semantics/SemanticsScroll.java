package org.glavo.himari.layout.semantics;

import org.jetbrains.annotations.NotNullByDefault;

/// Publishes scroll position for accessibility.
///
/// Percents use the UIA convention of `0` at the start and `100` at the end.
///
/// @param verticalPercent the finite vertical percent in `[0, 100]`
/// @param verticalViewSize the finite vertical visible-fraction percent in `(0, 100]`
/// @param verticallyScrollable whether the viewport can move vertically
/// @param horizontalPercent the finite horizontal percent in `[0, 100]`
/// @param horizontalViewSize the finite horizontal visible-fraction percent in `(0, 100]`
/// @param horizontallyScrollable whether the viewport can move horizontally
@NotNullByDefault
public record SemanticsScroll(
        double verticalPercent,
        double verticalViewSize,
        boolean verticallyScrollable,
        double horizontalPercent,
        double horizontalViewSize,
        boolean horizontallyScrollable
) {
    /// Validates the scroll snapshot.
    public SemanticsScroll {
        if (!Double.isFinite(verticalPercent) || verticalPercent < 0.0 || verticalPercent > 100.0) {
            throw new IllegalArgumentException("Vertical scroll percent must be in [0, 100]");
        }
        if (!Double.isFinite(verticalViewSize) || verticalViewSize <= 0.0 || verticalViewSize > 100.0) {
            throw new IllegalArgumentException("Vertical view size must be in (0, 100]");
        }
        if (!Double.isFinite(horizontalPercent) || horizontalPercent < 0.0 || horizontalPercent > 100.0) {
            throw new IllegalArgumentException("Horizontal scroll percent must be in [0, 100]");
        }
        if (!Double.isFinite(horizontalViewSize) || horizontalViewSize <= 0.0 || horizontalViewSize > 100.0) {
            throw new IllegalArgumentException("Horizontal view size must be in (0, 100]");
        }
    }

    /// Creates a vertical-only snapshot.
    ///
    /// @param verticalPercent the finite percent in `[0, 100]`
    /// @param verticalViewSize the finite visible-fraction percent in `(0, 100]`
    /// @param verticallyScrollable whether the viewport can move vertically
    public SemanticsScroll(double verticalPercent, double verticalViewSize, boolean verticallyScrollable) {
        this(verticalPercent, verticalViewSize, verticallyScrollable, 0.0, 100.0, false);
    }
}

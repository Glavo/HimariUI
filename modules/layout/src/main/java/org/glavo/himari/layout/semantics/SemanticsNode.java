package org.glavo.himari.layout.semantics;

import org.glavo.himari.layout.LayoutRect;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Objects;
import java.util.Set;

/// Captures one committed semantics node.
///
/// @param id the layout-node identity
/// @param role the semantic role
/// @param label the accessible name
/// @param actions the exposed actions
/// @param bounds the root-relative bounds
/// @param focused whether the node owns focus
/// @param selected the toggle state, or `null` when the node does not expose a boolean value
/// @param rangeValue the numeric range value, or `null` when the node does not expose a range
/// @param liveRegion the live-region politeness
/// @param textRange the UTF-16 selection and caret, or `null` when the node is not an editor
@NotNullByDefault
public record SemanticsNode(
        long id,
        SemanticsRole role,
        String label,
        @Unmodifiable Set<SemanticsAction> actions,
        LayoutRect bounds,
        boolean focused,
        @Nullable Boolean selected,
        @Nullable Double rangeValue,
        SemanticsLiveRegion liveRegion,
        @Nullable SemanticsTextRange textRange
) {
    /// Validates one semantics node.
    public SemanticsNode {
        if (id <= 0L) {
            throw new IllegalArgumentException("id must be positive");
        }
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(label, "label");
        actions = Set.copyOf(actions);
        Objects.requireNonNull(bounds, "bounds");
        if (rangeValue != null && !Double.isFinite(rangeValue)) {
            throw new IllegalArgumentException("rangeValue must be finite when present");
        }
        Objects.requireNonNull(liveRegion, "liveRegion");
    }
}

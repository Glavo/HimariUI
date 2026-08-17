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
/// @param grid the table or grid extent, or `null` when the node does not expose a grid
/// @param scroll the scroll snapshot, or `null` when the node does not expose scroll
/// @param gridItem the cell position, or `null` when the node is not a grid item
/// @param disabled whether the node is disabled
/// @param readOnly whether the node is read-only
/// @param hint the accessible hint, empty when absent
/// @param focusable whether the node can receive keyboard focus
/// @param password whether the node is a password field
/// @param accessKey the access key, empty when absent
/// @param acceleratorKey the accelerator key, empty when absent
/// @param required whether the node is required for form submission
/// @param itemStatus the item-status text, empty when absent
/// @param locale the BCP-47 locale, empty when unspecified
/// @param level the hierarchical level, or `0` when unspecified
/// @param positionInSet the one-based set position, or `0` when unspecified
/// @param sizeOfSet the set size, or `0` when unspecified
/// @param description the full description, empty when absent
/// @param error whether the node currently reports invalid form data
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
        @Nullable SemanticsTextRange textRange,
        @Nullable SemanticsGrid grid,
        @Nullable SemanticsScroll scroll,
        @Nullable SemanticsGridItem gridItem,
        boolean disabled,
        boolean readOnly,
        String hint,
        boolean focusable,
        boolean password,
        String accessKey,
        String acceleratorKey,
        boolean required,
        String itemStatus,
        String locale,
        int level,
        int positionInSet,
        int sizeOfSet,
        String description,
        boolean error
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
        Objects.requireNonNull(hint, "hint");
        Objects.requireNonNull(accessKey, "accessKey");
        Objects.requireNonNull(acceleratorKey, "acceleratorKey");
        Objects.requireNonNull(itemStatus, "itemStatus");
        Objects.requireNonNull(locale, "locale");
        if (level < 0 || positionInSet < 0 || sizeOfSet < 0) {
            throw new IllegalArgumentException("set metrics must be nonnegative");
        }
        Objects.requireNonNull(description, "description");
    }
}

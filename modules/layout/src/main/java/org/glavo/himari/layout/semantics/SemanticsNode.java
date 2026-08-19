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
/// @param itemType the item-type text, empty when absent
/// @param locale the BCP-47 locale, empty when unspecified
/// @param level the hierarchical level, or `0` when unspecified
/// @param positionInSet the one-based set position, or `0` when unspecified
/// @param sizeOfSet the set size, or `0` when unspecified
/// @param description the full description, empty when absent
/// @param error whether the node currently reports invalid form data
/// @param landmarkType the landmark type, or `0` when unspecified
/// @param localizedLandmarkType the localized landmark type, empty when absent
/// @param ariaRole the ARIA role, empty when absent
/// @param ariaProperties the ARIA properties string, empty when absent
/// @param controllerFor the controller-for target identity, empty when absent
/// @param describedBy the described-by target identity, empty when absent
/// @param flowsTo the flows-to target identity, empty when absent
/// @param labeledBy the labeled-by target identity, empty when absent
/// @param flowsFrom the flows-from target identity, empty when absent
/// @param optimizeForVisualContent whether the node prefers visual content over textual equivalents
/// @param fillColor the fill color as ARGB, or `0` when unspecified
/// @param outlineColor the outline color as ARGB, or `0` when unspecified
/// @param fillType the fill type, or `0` when unspecified
/// @param visualEffects the visual-effects flags, or `0` when unspecified
/// @param outlineThickness the outline thickness, or `0` when unspecified
/// @param rotation the element rotation in degrees, or `0` when unspecified
/// @param peripheral whether the node is peripheral to the main task
/// @param annotationType the annotation type, or `0` when unspecified
/// @param annotationObjects the annotation-object identities, empty when absent
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
        String itemType,
        String locale,
        int level,
        int positionInSet,
        int sizeOfSet,
        String description,
        boolean error,
        int landmarkType,
        String localizedLandmarkType,
        String ariaRole,
        String ariaProperties,
        String controllerFor,
        String describedBy,
        String flowsTo,
        String labeledBy,
        String flowsFrom,
        boolean optimizeForVisualContent,
        int fillColor,
        int outlineColor,
        int fillType,
        int visualEffects,
        int outlineThickness,
        int rotation,
        boolean peripheral,
        int annotationType,
        String annotationObjects
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
        Objects.requireNonNull(itemType, "itemType");
        Objects.requireNonNull(locale, "locale");
        if (level < 0 || positionInSet < 0 || sizeOfSet < 0 || landmarkType < 0
                || fillType < 0 || visualEffects < 0 || outlineThickness < 0 || annotationType < 0) {
            throw new IllegalArgumentException("set metrics must be nonnegative");
        }
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(localizedLandmarkType, "localizedLandmarkType");
        Objects.requireNonNull(ariaRole, "ariaRole");
        Objects.requireNonNull(ariaProperties, "ariaProperties");
        Objects.requireNonNull(controllerFor, "controllerFor");
        Objects.requireNonNull(describedBy, "describedBy");
        Objects.requireNonNull(flowsTo, "flowsTo");
        Objects.requireNonNull(labeledBy, "labeledBy");
        Objects.requireNonNull(flowsFrom, "flowsFrom");
        Objects.requireNonNull(annotationObjects, "annotationObjects");
    }
}

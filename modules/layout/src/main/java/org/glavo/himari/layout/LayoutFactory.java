package org.glavo.himari.layout;

import org.glavo.himari.layout.semantics.SemanticsAction;
import org.glavo.himari.layout.semantics.SemanticsRole;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntConsumer;

/// Creates layout nodes bound to one [LayoutTree] identity allocator.
@NotNullByDefault
public final class LayoutFactory {
    /// The owning tree.
    private final LayoutTree tree;

    /// Creates one factory.
    ///
    /// @param tree the owning tree
    public LayoutFactory(LayoutTree tree) {
        this.tree = Objects.requireNonNull(tree, "tree");
    }

    /// Creates a box that stacks children in one cell.
    ///
    /// @param name the diagnostic name
    /// @param modifiers the modifiers
    /// @param children the children
    /// @return the box
    public LayoutNode box(String name, List<LayoutModifier> modifiers, LayoutNode... children) {
        return container(name, LayoutKind.BOX, Alignment.START, modifiers, SemanticsRole.NONE, name, children);
    }

    /// Creates a horizontal row.
    ///
    /// @param name the diagnostic name
    /// @param alignment the cross-axis alignment
    /// @param modifiers the modifiers
    /// @param children the children
    /// @return the row
    public LayoutNode row(
            String name,
            Alignment alignment,
            List<LayoutModifier> modifiers,
            LayoutNode... children
    ) {
        return row(name, alignment, modifiers, SemanticsRole.NONE, name, children);
    }

    /// Creates a horizontal row with an explicit semantics role.
    ///
    /// @param name the diagnostic name
    /// @param alignment the cross-axis alignment
    /// @param modifiers the modifiers
    /// @param role the semantics role
    /// @param label the semantics label
    /// @param children the children
    /// @return the row
    public LayoutNode row(
            String name,
            Alignment alignment,
            List<LayoutModifier> modifiers,
            SemanticsRole role,
            String label,
            LayoutNode... children
    ) {
        return container(name, LayoutKind.ROW, alignment, modifiers, role, label, children);
    }

    /// Creates a vertical column.
    ///
    /// @param name the diagnostic name
    /// @param alignment the cross-axis alignment
    /// @param modifiers the modifiers
    /// @param children the children
    /// @return the column
    public LayoutNode column(
            String name,
            Alignment alignment,
            List<LayoutModifier> modifiers,
            LayoutNode... children
    ) {
        return container(name, LayoutKind.COLUMN, alignment, modifiers, SemanticsRole.NONE, name, children);
    }

    /// Creates a vertical column with an explicit semantics role.
    ///
    /// @param name the diagnostic name
    /// @param alignment the cross-axis alignment
    /// @param modifiers the modifiers
    /// @param role the semantics role
    /// @param label the semantics label
    /// @param children the children
    /// @return the column
    public LayoutNode column(
            String name,
            Alignment alignment,
            List<LayoutModifier> modifiers,
            SemanticsRole role,
            String label,
            LayoutNode... children
    ) {
        return container(name, LayoutKind.COLUMN, alignment, modifiers, role, label, children);
    }

    /// Creates a horizontal flex row that distributes leftover width by grow weights.
    ///
    /// @param name the diagnostic name
    /// @param alignment the cross-axis alignment
    /// @param modifiers the modifiers
    /// @param children the children
    /// @return the flex row
    public LayoutNode flex(
            String name,
            Alignment alignment,
            List<LayoutModifier> modifiers,
            LayoutNode... children
    ) {
        return container(name, LayoutKind.FLEX, alignment, modifiers, SemanticsRole.NONE, name, children);
    }

    /// Creates a wrapping flow that fills the available width.
    ///
    /// @param name the diagnostic name
    /// @param alignment the cross-axis alignment within each wrapping line
    /// @param modifiers the modifiers
    /// @param children the children
    /// @return the flow
    public LayoutNode flow(
            String name,
            Alignment alignment,
            List<LayoutModifier> modifiers,
            LayoutNode... children
    ) {
        Objects.requireNonNull(alignment, "alignment");
        return container(name, LayoutKind.FLOW, alignment, modifiers, SemanticsRole.NONE, name, children);
    }

    /// Creates a fixed-column grid.
    ///
    /// @param name the diagnostic name
    /// @param columns the positive column count
    /// @param alignment the in-cell alignment, including [`Alignment#BASELINE`] on each row
    /// @param modifiers the modifiers
    /// @param children the children
    /// @return the grid
    public LayoutNode grid(
            String name,
            int columns,
            Alignment alignment,
            List<LayoutModifier> modifiers,
            LayoutNode... children
    ) {
        Objects.requireNonNull(alignment, "alignment");
        ArrayList<LayoutModifier> withColumns = new ArrayList<>(modifiers);
        withColumns.add(new LayoutModifier.GridColumns(columns));
        return container(name, LayoutKind.GRID, alignment, withColumns, SemanticsRole.NONE, name, children);
    }

    /// Creates a custom-layout container.
    ///
    /// @param name the diagnostic name
    /// @param modifiers the modifiers
    /// @param layout the measure and place delegate
    /// @param children the children
    /// @return the custom container
    public LayoutNode custom(
            String name,
            List<LayoutModifier> modifiers,
            CustomLayout layout,
            LayoutNode... children
    ) {
        Objects.requireNonNull(layout, "layout");
        LayoutNode node = new LayoutNode(
                tree.allocateId(),
                name,
                LayoutKind.CUSTOM,
                Alignment.START,
                modifiers,
                null,
                false,
                SemanticsRole.NONE,
                name,
                Set.of(),
                null,
                null,
                layout
        );
        for (LayoutNode child : children) {
            node.add(child);
        }
        return node;
    }

    /// Creates an overlay that sizes to the union of offset children.
    ///
    /// @param name the diagnostic name
    /// @param modifiers the modifiers
    /// @param children the children
    /// @return the overlay
    public LayoutNode overlay(String name, List<LayoutModifier> modifiers, LayoutNode... children) {
        return container(name, LayoutKind.OVERLAY, Alignment.START, modifiers, SemanticsRole.NONE, name, children);
    }

    /// Creates a portal whose size is the first child's slot.
    ///
    /// @param name the diagnostic name
    /// @param modifiers the modifiers
    /// @param children the slot followed by portaled children
    /// @return the portal
    public LayoutNode portal(String name, List<LayoutModifier> modifiers, LayoutNode... children) {
        return container(name, LayoutKind.PORTAL, Alignment.START, modifiers, SemanticsRole.NONE, name, children);
    }

    /// Creates a vertical scroll viewport.
    ///
    /// @param name the diagnostic name
    /// @param modifiers the modifiers
    /// @param children the content
    /// @return the viewport
    public LayoutNode scroll(String name, List<LayoutModifier> modifiers, LayoutNode... children) {
        return container(name, LayoutKind.SCROLL, Alignment.START, modifiers, SemanticsRole.NONE, name, children);
    }

    /// Creates a leaf with an intrinsic size.
    ///
    /// @param name the diagnostic name
    /// @param size the intrinsic size
    /// @param modifiers the modifiers
    /// @param focusable whether the leaf is focusable
    /// @param role the semantics role
    /// @param label the semantics label
    /// @param actions the semantics actions
    /// @param onActivate the activation callback, or `null`
    /// @return the leaf
    public LayoutNode leaf(
            String name,
            Size size,
            List<LayoutModifier> modifiers,
            boolean focusable,
            SemanticsRole role,
            String label,
            Set<SemanticsAction> actions,
            @Nullable Runnable onActivate
    ) {
        return leaf(name, size, modifiers, focusable, role, label, actions, onActivate, null);
    }

    /// Creates a leaf that may expose activation and increment/decrement handlers.
    ///
    /// @param name the diagnostic name
    /// @param size the intrinsic size
    /// @param modifiers the modifiers
    /// @param focusable whether the leaf is focusable
    /// @param role the semantics role
    /// @param label the semantics label
    /// @param actions the semantics actions
    /// @param onActivate the activation callback, or `null`
    /// @param onAdjust the increment/decrement callback, or `null`
    /// @return the leaf
    public LayoutNode leaf(
            String name,
            Size size,
            List<LayoutModifier> modifiers,
            boolean focusable,
            SemanticsRole role,
            String label,
            Set<SemanticsAction> actions,
            @Nullable Runnable onActivate,
            @Nullable IntConsumer onAdjust
    ) {
        return new LayoutNode(
                tree.allocateId(),
                name,
                LayoutKind.LEAF,
                Alignment.START,
                modifiers,
                size,
                focusable,
                role,
                label,
                actions,
                onActivate,
                onAdjust
        );
    }

    /// Creates a container and attaches its children.
    ///
    /// @param name the diagnostic name
    /// @param kind the policy
    /// @param alignment the cross-axis alignment
    /// @param modifiers the modifiers
    /// @param role the semantics role
    /// @param label the semantics label
    /// @param children the children
    /// @return the container
    private LayoutNode container(
            String name,
            LayoutKind kind,
            Alignment alignment,
            List<LayoutModifier> modifiers,
            SemanticsRole role,
            String label,
            LayoutNode... children
    ) {
        LayoutNode node = new LayoutNode(
                tree.allocateId(),
                name,
                kind,
                alignment,
                modifiers,
                null,
                false,
                role,
                label,
                Set.of(),
                null,
                null
        );
        for (LayoutNode child : children) {
            node.add(child);
        }
        return node;
    }
}

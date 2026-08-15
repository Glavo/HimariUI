package org.glavo.himari.layout;

import org.glavo.himari.layout.semantics.SemanticsAction;
import org.glavo.himari.layout.semantics.SemanticsRole;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

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
        return container(name, LayoutKind.BOX, Alignment.START, modifiers, children);
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
        return container(name, LayoutKind.ROW, alignment, modifiers, children);
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
        return container(name, LayoutKind.COLUMN, alignment, modifiers, children);
    }

    /// Creates a vertical scroll viewport.
    ///
    /// @param name the diagnostic name
    /// @param modifiers the modifiers
    /// @param children the content
    /// @return the viewport
    public LayoutNode scroll(String name, List<LayoutModifier> modifiers, LayoutNode... children) {
        return container(name, LayoutKind.SCROLL, Alignment.START, modifiers, children);
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
    /// @param children the children
    /// @return the container
    private LayoutNode container(
            String name,
            LayoutKind kind,
            Alignment alignment,
            List<LayoutModifier> modifiers,
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
                SemanticsRole.NONE,
                name,
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

package org.glavo.himari.controls;

import org.glavo.himari.layout.Alignment;
import org.glavo.himari.layout.LayoutFactory;
import org.glavo.himari.layout.LayoutModifier;
import org.glavo.himari.layout.LayoutNode;
import org.glavo.himari.layout.Size;
import org.glavo.himari.layout.semantics.SemanticsAction;
import org.glavo.himari.layout.semantics.SemanticsRole;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/// Materializes a hierarchical outline of expandable items.
///
/// An item is visible when every ancestor with a smaller depth is expanded. Toggle expands or
/// collapses an expandable item. Selection is a single visible-row index into the source list.
@NotNullByDefault
public final class Tree {
    /// Row height.
    private static final float ROW_HEIGHT = 20.0f;

    /// Source items in document order.
    private final List<Item> items;

    /// Expansion flags parallel to [`#items`].
    private final boolean[] expanded;

    /// Selected source index, or `-1`.
    private int selected = -1;

    /// Whether the outline ignores toggle and selection.
    private boolean disabled;

    /// Mounted row leaves that receive the published disabled state.
    private @Nullable List<LayoutNode> nodes;

    /// Creates a tree.
    ///
    /// @param items the items
    public Tree(List<Item> items) {
        this.items = List.copyOf(items);
        if (this.items.isEmpty()) {
            throw new IllegalArgumentException("Tree must contain at least one item");
        }
        this.expanded = new boolean[this.items.size()];
        int lastDepth = -1;
        for (int index = 0; index < this.items.size(); index++) {
            Item item = Objects.requireNonNull(this.items.get(index), "item");
            if (item.depth() < 0 || item.depth() > lastDepth + 1) {
                throw new IllegalArgumentException("Tree depths must start at 0 and increase by at most one");
            }
            lastDepth = item.depth();
            expanded[index] = item.expandable();
        }
    }

    /// Returns the source items.
    ///
    /// @return the items
    public @Unmodifiable List<Item> items() {
        return items;
    }

    /// Returns the selected source index, or `-1`.
    ///
    /// @return the index
    public int selected() {
        return selected;
    }

    /// Returns whether `index` is expanded.
    ///
    /// @param index the source index
    /// @return whether it is expanded
    public boolean isExpanded(int index) {
        return expanded[index];
    }

    /// Toggles expansion of an expandable item.
    ///
    /// @param index the source index
    public void toggle(int index) {
        Item item = items.get(index);
        if (!item.expandable()) {
            throw new IllegalArgumentException("Tree item is not expandable");
        }
        if (disabled) {
            return;
        }
        expanded[index] = !expanded[index];
    }

    /// Selects a visible source index.
    ///
    /// @param index the source index
    public void select(int index) {
        if (!isVisible(index)) {
            throw new IllegalArgumentException("Tree item is not visible");
        }
        if (disabled) {
            return;
        }
        selected = index;
    }

    /// Returns whether the outline is disabled.
    ///
    /// @return whether the outline is disabled
    public boolean disabled() {
        return disabled;
    }

    /// Sets the disabled state and publishes it to mounted row leaves when present.
    ///
    /// @param disabled the state
    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
        if (nodes != null) {
            for (LayoutNode row : nodes) {
                row.setDisabled(disabled);
            }
        }
    }

    /// Returns visible source indices in document order.
    ///
    /// @return the indices
    public @Unmodifiable List<Integer> visibleIndices() {
        ArrayList<Integer> visible = new ArrayList<>();
        for (int index = 0; index < items.size(); index++) {
            if (isVisible(index)) {
                visible.add(index);
            }
        }
        return List.copyOf(visible);
    }

    /// Builds the visible rows.
    ///
    /// @param factory the node factory
    /// @param name the diagnostic name
    /// @return the tree
    public LayoutNode create(LayoutFactory factory, String name) {
        Objects.requireNonNull(factory, "factory");
        Objects.requireNonNull(name, "name");
        ArrayList<LayoutNode> rows = new ArrayList<>();
        for (int index : visibleIndices()) {
            Item item = items.get(index);
            int target = index;
            Set<SemanticsAction> actions = item.expandable()
                    ? Set.of(SemanticsAction.ACTIVATE, SemanticsAction.INCREMENT, SemanticsAction.DECREMENT)
                    : Set.of(SemanticsAction.ACTIVATE);
            LayoutNode row = factory.leaf(
                    name + "-item-" + item.key(),
                    new Size(160.0f, ROW_HEIGHT),
                    List.of(new LayoutModifier.Padding(item.depth() * 8.0f)),
                    true,
                    SemanticsRole.TREE_ITEM,
                    item.label(),
                    actions,
                    () -> select(target),
                    item.expandable() ? ignored -> toggle(target) : null
            );
            row.setSelected(index == selected);
            row.setDisabled(disabled);
            rows.add(row);
        }
        this.nodes = List.copyOf(rows);
        return factory.column(
                name,
                Alignment.START,
                List.of(new LayoutModifier.Padding(0.0f)),
                SemanticsRole.TREE,
                name,
                rows.toArray(LayoutNode[]::new)
        );
    }

    /// Returns whether every ancestor of `index` is expanded.
    private boolean isVisible(int index) {
        int depth = items.get(index).depth();
        int cursor = index - 1;
        while (depth > 0 && cursor >= 0) {
            Item ancestor = items.get(cursor);
            if (ancestor.depth() == depth - 1) {
                if (!ancestor.expandable() || !expanded[cursor]) {
                    return false;
                }
                depth = ancestor.depth();
            }
            cursor--;
        }
        return depth == 0;
    }

    /// Stores one outline row.
    ///
    /// @param key the stable key
    /// @param label the accessible name
    /// @param depth the nonnegative nesting depth
    /// @param expandable whether the row may hide descendants
    public record Item(String key, String label, int depth, boolean expandable) {
        /// Validates the item.
        public Item {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(label, "label");
            if (key.isEmpty() || label.isEmpty()) {
                throw new IllegalArgumentException("Tree key and label must not be empty");
            }
            if (depth < 0) {
                throw new IllegalArgumentException("Tree depth must be nonnegative");
            }
        }
    }
}

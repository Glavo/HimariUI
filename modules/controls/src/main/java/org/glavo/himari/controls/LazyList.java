package org.glavo.himari.controls;

import org.glavo.himari.layout.Alignment;
import org.glavo.himari.layout.LayoutFactory;
import org.glavo.himari.layout.LayoutModifier;
import org.glavo.himari.layout.LayoutNode;
import org.glavo.himari.layout.Size;
import org.glavo.himari.layout.semantics.SemanticsAction;
import org.glavo.himari.layout.semantics.SemanticsRole;
import org.glavo.himari.layout.semantics.SemanticsScroll;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/// Materializes a bounded window of list items.
@NotNullByDefault
public final class LazyList {
    /// Height of one item leaf.
    private static final float ITEM_HEIGHT = 20.0f;

    /// Total logical item count.
    private final int itemCount;

    /// Number of simultaneously materialized items.
    private final int windowSize;

    /// Index of the first materialized item.
    private int firstVisible;

    /// Creates a list.
    ///
    /// @param itemCount the nonnegative total count
    /// @param windowSize the positive window size
    public LazyList(int itemCount, int windowSize) {
        if (itemCount < 0) {
            throw new IllegalArgumentException("itemCount must be nonnegative");
        }
        if (windowSize <= 0) {
            throw new IllegalArgumentException("windowSize must be positive");
        }
        this.itemCount = itemCount;
        this.windowSize = windowSize;
    }

    /// Returns the first visible index.
    ///
    /// @return the index
    public int firstVisible() {
        return firstVisible;
    }

    /// Returns the total logical item count.
    ///
    /// @return the count
    public int itemCount() {
        return itemCount;
    }

    /// Returns labels for every logical item, including unmounted rows.
    ///
    /// @return the labels in document order
    public @Unmodifiable List<String> logicalLabels() {
        ArrayList<String> labels = new ArrayList<>(itemCount);
        for (int index = 0; index < itemCount; index++) {
            labels.add("Item " + index);
        }
        return List.copyOf(labels);
    }

    /// Returns labels for items outside the materialized window.
    ///
    /// @return the unmounted labels in document order
    public @Unmodifiable List<String> unmountedLabels() {
        ArrayList<String> labels = new ArrayList<>();
        int last = Math.min(itemCount, firstVisible + windowSize);
        for (int index = 0; index < itemCount; index++) {
            if (index < firstVisible || index >= last) {
                labels.add("Item " + index);
            }
        }
        return List.copyOf(labels);
    }

    /// Builds the current window as a column of leaves.
    ///
    /// @param factory the node factory
    /// @param name the diagnostic name
    /// @return the column
    public LayoutNode create(LayoutFactory factory, String name) {
        Objects.requireNonNull(factory, "factory");
        Objects.requireNonNull(name, "name");
        ArrayList<LayoutNode> items = new ArrayList<>();
        int last = Math.min(itemCount, firstVisible + windowSize);
        for (int index = firstVisible; index < last; index++) {
            String label = "Item " + index;
            items.add(factory.leaf(
                    name + "-item-" + index,
                    new Size(160.0f, ITEM_HEIGHT),
                    List.of(),
                    index == firstVisible,
                    SemanticsRole.LIST,
                    label,
                    index == firstVisible
                            ? Set.of(
                                    SemanticsAction.INCREMENT,
                                    SemanticsAction.DECREMENT,
                                    SemanticsAction.SCROLL_INTO_VIEW)
                            : Set.of(SemanticsAction.SCROLL_INTO_VIEW),
                    null,
                    index == firstVisible ? this::adjust : null
            ));
        }
        LayoutNode column = factory.column(
                name,
                Alignment.START,
                List.of(new LayoutModifier.Padding(0.0f)),
                SemanticsRole.LIST,
                name,
                items.toArray(LayoutNode[]::new)
        );
        column.setScroll(scrollSnapshot());
        return column;
    }

    /// Builds the vertical-scroll snapshot for the current window.
    ///
    /// @return the snapshot
    public SemanticsScroll scrollSnapshot() {
        if (itemCount <= windowSize) {
            return new SemanticsScroll(0.0, 100.0, false);
        }
        int maximum = itemCount - windowSize;
        double percent = 100.0 * firstVisible / maximum;
        double viewSize = 100.0 * windowSize / itemCount;
        return new SemanticsScroll(percent, viewSize, true);
    }

    /// Moves the window by one item.
    ///
    /// @param delta `1` or `-1`
    private void adjust(int delta) {
        int next = firstVisible + delta;
        int maximum = Math.max(0, itemCount - windowSize);
        firstVisible = Math.min(maximum, Math.max(0, next));
    }
}

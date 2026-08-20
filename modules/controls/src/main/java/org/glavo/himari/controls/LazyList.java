package org.glavo.himari.controls;

import org.glavo.himari.layout.Alignment;
import org.glavo.himari.layout.LayoutFactory;
import org.glavo.himari.layout.LayoutModifier;
import org.glavo.himari.layout.LayoutNode;
import org.glavo.himari.layout.Size;
import org.glavo.himari.layout.semantics.SemanticsAction;
import org.glavo.himari.layout.semantics.SemanticsRole;
import org.glavo.himari.layout.input.gesture.ClampingScrollPhysics;
import org.glavo.himari.layout.input.gesture.ScrollPhysics;
import org.glavo.himari.layout.semantics.SemanticsScroll;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
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
    private int itemCount;

    /// Number of simultaneously materialized items.
    private final int windowSize;

    /// Extra items materialized before and after the visible window.
    private final int overscan;

    /// Measured item heights; `0` means [`#ITEM_HEIGHT`] is still in use.
    private final ArrayList<Float> measured = new ArrayList<>();

    /// Index of the first materialized item.
    private int firstVisible;

    /// Whether the list ignores scroll and mutation.
    private boolean disabled;

    /// Policy that clamps window origins.
    private ScrollPhysics physics = ClampingScrollPhysics.INSTANCE;

    /// Remaining fling velocity in items per second.
    private float flingVelocity;

    /// Mounted column that receives the published disabled state.
    private @Nullable LayoutNode node;

    /// Creates a list.
    ///
    /// @param itemCount the nonnegative total count
    /// @param windowSize the positive window size
    public LazyList(int itemCount, int windowSize) {
        this(itemCount, windowSize, 0);
    }

    /// Creates a list with overscan.
    ///
    /// @param itemCount the nonnegative total count
    /// @param windowSize the positive window size
    /// @param overscan the nonnegative prefetch count on each side
    public LazyList(int itemCount, int windowSize, int overscan) {
        if (itemCount < 0) {
            throw new IllegalArgumentException("itemCount must be nonnegative");
        }
        if (windowSize <= 0) {
            throw new IllegalArgumentException("windowSize must be positive");
        }
        if (overscan < 0) {
            throw new IllegalArgumentException("overscan must be nonnegative");
        }
        this.itemCount = itemCount;
        this.windowSize = windowSize;
        this.overscan = overscan;
    }

    /// Returns the overscan in items.
    ///
    /// @return the overscan
    public int overscan() {
        return overscan;
    }

    /// Returns the scroll-physics policy.
    ///
    /// @return the policy
    public ScrollPhysics physics() {
        return physics;
    }

    /// Replaces the scroll-physics policy.
    ///
    /// @param physics the policy
    public void setPhysics(ScrollPhysics physics) {
        this.physics = Objects.requireNonNull(physics, "physics");
        clampWindow();
    }

    /// Returns the remaining fling velocity.
    ///
    /// @return the velocity in items per second
    public float flingVelocity() {
        return flingVelocity;
    }

    /// Starts a fling with `velocity` items per second.
    ///
    /// @param velocity the finite velocity
    public void fling(float velocity) {
        if (disabled) {
            return;
        }
        if (!Float.isFinite(velocity)) {
            throw new IllegalArgumentException("Fling velocity must be finite");
        }
        flingVelocity = velocity;
    }

    /// Advances an active fling by `elapsedNanos` using [#physics()].
    ///
    /// @param elapsedNanos the nonnegative sample duration
    /// @return whether the fling is still moving
    public boolean advanceFling(long elapsedNanos) {
        if (disabled || flingVelocity == 0.0f) {
            return false;
        }
        float next = physics.decayVelocity(flingVelocity, elapsedNanos);
        float dt = elapsedNanos / 1_000_000_000.0f;
        int delta = Math.round((flingVelocity + next) * 0.5f * dt);
        flingVelocity = next;
        int maximum = Math.max(0, itemCount - windowSize);
        firstVisible = physics.applyIndex(firstVisible, delta, 0, maximum);
        return flingVelocity != 0.0f;
    }

    /// Returns the height used for `index`, preferring a measured height.
    ///
    /// @param index the item index
    /// @return the height
    public float heightAt(int index) {
        if (index < 0 || index >= itemCount) {
            throw new IllegalArgumentException("Item index is out of range");
        }
        if (index < measured.size()) {
            float recorded = measured.get(index);
            if (recorded > 0.0f) {
                return recorded;
            }
        }
        return ITEM_HEIGHT;
    }

    /// Records a measured height for `index`.
    ///
    /// @param index the item index
    /// @param height the positive measured height
    public void correctHeight(int index, float height) {
        if (index < 0 || index >= itemCount) {
            throw new IllegalArgumentException("Item index is out of range");
        }
        if (!(height > 0.0f) || !Float.isFinite(height)) {
            throw new IllegalArgumentException("Measured item height must be finite and positive");
        }
        while (measured.size() <= index) {
            measured.add(0.0f);
        }
        measured.set(index, height);
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

    /// Returns whether the list is disabled.
    ///
    /// @return whether the list is disabled
    public boolean disabled() {
        return disabled;
    }

    /// Sets the disabled state and publishes it to the mounted column when present.
    ///
    /// @param disabled the state
    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
        if (node != null) {
            node.setDisabled(disabled);
        }
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
        int first = materializedFirst();
        int last = materializedLast();
        for (int index = 0; index < itemCount; index++) {
            if (index < first || index >= last) {
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
        int last = materializedLast();
        for (int index = materializedFirst(); index < last; index++) {
            String label = "Item " + index;
            items.add(factory.leaf(
                    name + "-item-" + index,
                    new Size(160.0f, heightAt(index)),
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
        column.setDisabled(disabled);
        this.node = column;
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

    /// Scrolls so `index` is the first visible item, clamped to the valid range.
    ///
    /// @param index the requested first-visible index
    public void scrollTo(int index) {
        if (disabled) {
            return;
        }
        int maximum = Math.max(0, itemCount - windowSize);
        firstVisible = physics.clampIndex(index, 0, maximum);
    }

    /// Inserts one logical item at `index` and preserves the first-visible anchor when possible.
    ///
    /// @param index the insertion index in `[0, itemCount]`
    public void insert(int index) {
        if (disabled) {
            return;
        }
        if (index < 0 || index > itemCount) {
            throw new IllegalArgumentException("insert index is out of range");
        }
        itemCount++;
        if (index < measured.size()) {
            measured.add(index, 0.0f);
        }
        if (index <= firstVisible) {
            firstVisible++;
        }
        clampWindow();
    }

    /// Removes the logical item at `index` and clamps the window.
    ///
    /// @param index the removal index in `[0, itemCount)`
    public void remove(int index) {
        if (disabled) {
            return;
        }
        if (index < 0 || index >= itemCount) {
            throw new IllegalArgumentException("remove index is out of range");
        }
        itemCount--;
        if (index < measured.size()) {
            measured.remove(index);
        }
        if (index < firstVisible) {
            firstVisible--;
        }
        clampWindow();
    }

    /// Returns the first materialized index, including leading overscan.
    ///
    /// @return the index
    public int materializedFirst() {
        return Math.max(0, firstVisible - overscan);
    }

    /// Returns the exclusive last materialized index, including trailing overscan.
    ///
    /// @return the exclusive index
    public int materializedLast() {
        return Math.min(itemCount, firstVisible + windowSize + overscan);
    }

    /// Clamps [`#firstVisible`] after a count change.
    private void clampWindow() {
        int maximum = Math.max(0, itemCount - windowSize);
        firstVisible = physics.clampIndex(firstVisible, 0, maximum);
    }

    /// Pages the window by `pages` windows of [windowSize] items.
    ///
    /// @param pages signed page count; negative pages backward
    public void page(int pages) {
        scrollTo(firstVisible + pages * windowSize);
    }

    /// Moves the window by one item.
    ///
    /// @param delta `1` or `-1`
    private void adjust(int delta) {
        if (disabled) {
            return;
        }
        int maximum = Math.max(0, itemCount - windowSize);
        firstVisible = physics.applyIndex(firstVisible, delta, 0, maximum);
    }
}

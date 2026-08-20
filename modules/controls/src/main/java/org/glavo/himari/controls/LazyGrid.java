package org.glavo.himari.controls;

import org.glavo.himari.layout.Alignment;
import org.glavo.himari.layout.LayoutFactory;
import org.glavo.himari.layout.LayoutModifier;
import org.glavo.himari.layout.LayoutNode;
import org.glavo.himari.layout.Size;
import org.glavo.himari.layout.semantics.SemanticsAction;
import org.glavo.himari.layout.semantics.SemanticsGrid;
import org.glavo.himari.layout.semantics.SemanticsGridItem;
import org.glavo.himari.layout.semantics.SemanticsRole;
import org.glavo.himari.layout.semantics.SemanticsScroll;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/// Materializes a bounded window of grid cells.
///
/// Rows are virtualized. Each logical item occupies one cell in row-major order.
/// Inserts and removals keep the first visible row when that row still exists.
@NotNullByDefault
public final class LazyGrid {
    /// Width of one cell leaf.
    private static final float CELL_WIDTH = 32.0f;

    /// Height of one cell leaf.
    private static final float CELL_HEIGHT = 16.0f;

    /// Total logical item count.
    private int itemCount;

    /// Number of cells in one row.
    private final int columnCount;

    /// Number of simultaneously materialized rows.
    private final int windowRows;

    /// Extra rows materialized before and after the visible window.
    private final int overscan;

    /// Index of the first visible item; [`#firstVisible()`] is this index divided by [`#columnCount`].
    private int firstIndex;

    /// Whether the grid ignores scroll and mutation.
    private boolean disabled;

    /// Mounted column that receives the published disabled state.
    private @Nullable LayoutNode node;

    /// Creates a grid without overscan.
    ///
    /// @param itemCount the nonnegative total count
    /// @param columnCount the positive column count
    /// @param windowRows the positive visible-row count
    public LazyGrid(int itemCount, int columnCount, int windowRows) {
        this(itemCount, columnCount, windowRows, 0);
    }

    /// Creates a grid with overscan.
    ///
    /// @param itemCount the nonnegative total count
    /// @param columnCount the positive column count
    /// @param windowRows the positive visible-row count
    /// @param overscan the nonnegative prefetch count on each side
    public LazyGrid(int itemCount, int columnCount, int windowRows, int overscan) {
        if (itemCount < 0) {
            throw new IllegalArgumentException("itemCount must be nonnegative");
        }
        if (columnCount <= 0) {
            throw new IllegalArgumentException("columnCount must be positive");
        }
        if (windowRows <= 0) {
            throw new IllegalArgumentException("windowRows must be positive");
        }
        if (overscan < 0) {
            throw new IllegalArgumentException("overscan must be nonnegative");
        }
        this.itemCount = itemCount;
        this.columnCount = columnCount;
        this.windowRows = windowRows;
        this.overscan = overscan;
    }

    /// Returns the overscan in rows.
    ///
    /// @return the overscan
    public int overscan() {
        return overscan;
    }

    /// Returns the column count.
    ///
    /// @return the count
    public int columnCount() {
        return columnCount;
    }

    /// Returns the logical row count.
    ///
    /// @return the count
    public int rowCount() {
        if (itemCount == 0) {
            return 0;
        }
        return (itemCount + columnCount - 1) / columnCount;
    }

    /// Returns the first visible row.
    ///
    /// @return the row
    public int firstVisible() {
        return firstIndex / columnCount;
    }

    /// Returns the total logical item count.
    ///
    /// @return the count
    public int itemCount() {
        return itemCount;
    }

    /// Returns whether the grid is disabled.
    ///
    /// @return whether the grid is disabled
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

    /// Returns labels for every logical item, including unmounted cells.
    ///
    /// @return the labels in document order
    public @Unmodifiable List<String> logicalLabels() {
        ArrayList<String> labels = new ArrayList<>(itemCount);
        for (int index = 0; index < itemCount; index++) {
            labels.add(labelAt(index));
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
            int row = index / columnCount;
            if (row < first || row >= last) {
                labels.add(labelAt(index));
            }
        }
        return List.copyOf(labels);
    }

    /// Builds the current window as a column of rows.
    ///
    /// @param factory the node factory
    /// @param name the diagnostic name
    /// @return the column
    public LayoutNode create(LayoutFactory factory, String name) {
        Objects.requireNonNull(factory, "factory");
        Objects.requireNonNull(name, "name");
        ArrayList<LayoutNode> rows = new ArrayList<>();
        int last = materializedLast();
        for (int row = materializedFirst(); row < last; row++) {
            ArrayList<LayoutNode> cells = new ArrayList<>();
            for (int column = 0; column < columnCount; column++) {
                int index = row * columnCount + column;
                if (index >= itemCount) {
                    break;
                }
                boolean focusable = row == firstVisible() && column == 0;
                LayoutNode cell = factory.leaf(
                        name + "-cell-" + index,
                        new Size(CELL_WIDTH, CELL_HEIGHT),
                        List.of(),
                        focusable,
                        SemanticsRole.TABLE_CELL,
                        labelAt(index),
                        focusable
                                ? Set.of(
                                        SemanticsAction.INCREMENT,
                                        SemanticsAction.DECREMENT,
                                        SemanticsAction.SCROLL_INTO_VIEW)
                                : Set.of(SemanticsAction.SCROLL_INTO_VIEW),
                        null,
                        focusable ? this::adjust : null
                );
                cell.setGridItem(new SemanticsGridItem(row, column));
                cells.add(cell);
            }
            rows.add(factory.row(
                    name + "-row-" + row,
                    Alignment.START,
                    List.of(new LayoutModifier.Padding(0.0f)),
                    SemanticsRole.TABLE_ROW,
                    name + "-row-" + row,
                    cells.toArray(LayoutNode[]::new)
            ));
        }
        LayoutNode column = factory.column(
                name,
                Alignment.START,
                List.of(new LayoutModifier.Padding(0.0f)),
                SemanticsRole.GRID,
                name,
                rows.toArray(LayoutNode[]::new)
        );
        column.setGrid(new SemanticsGrid(rowCount(), columnCount));
        column.setScroll(scrollSnapshot());
        column.setDisabled(disabled);
        this.node = column;
        return column;
    }

    /// Builds the vertical-scroll snapshot for the current window.
    ///
    /// @return the snapshot
    public SemanticsScroll scrollSnapshot() {
        int rows = rowCount();
        if (rows <= windowRows) {
            return new SemanticsScroll(0.0, 100.0, false);
        }
        int maximum = rows - windowRows;
        double percent = 100.0 * firstVisible() / maximum;
        double viewSize = 100.0 * windowRows / rows;
        return new SemanticsScroll(percent, viewSize, true);
    }

    /// Scrolls so `row` is the first visible row, clamped to the valid range.
    ///
    /// @param row the requested first-visible row
    public void scrollTo(int row) {
        if (disabled) {
            return;
        }
        int maximum = Math.max(0, rowCount() - windowRows);
        int clamped = Math.min(maximum, Math.max(0, row));
        firstIndex = clamped * columnCount;
    }

    /// Inserts one logical item at `index` and preserves the first-visible row when possible.
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
        if (index <= firstIndex) {
            firstIndex++;
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
        if (index < firstIndex) {
            firstIndex--;
        }
        clampWindow();
    }

    /// Returns the first materialized row, including leading overscan.
    ///
    /// @return the row
    public int materializedFirst() {
        return Math.max(0, firstVisible() - overscan);
    }

    /// Returns the exclusive last materialized row, including trailing overscan.
    ///
    /// @return the exclusive row
    public int materializedLast() {
        return Math.min(rowCount(), firstVisible() + windowRows + overscan);
    }

    /// Pages the window by `pages` windows of [windowRows] rows.
    ///
    /// @param pages signed page count; negative pages backward
    public void page(int pages) {
        scrollTo(firstVisible() + pages * windowRows);
    }

    /// Returns the label for `index`.
    ///
    /// @param index the item index
    /// @return the label
    private static String labelAt(int index) {
        return "Cell " + index;
    }

    /// Clamps [`#firstIndex`] after a count change.
    private void clampWindow() {
        int maximum = Math.max(0, rowCount() - windowRows) * columnCount;
        firstIndex = Math.min(maximum, Math.max(0, firstIndex));
    }

    /// Moves the window by one row.
    ///
    /// @param delta `1` or `-1`
    private void adjust(int delta) {
        if (disabled) {
            return;
        }
        scrollTo(firstVisible() + delta);
    }
}

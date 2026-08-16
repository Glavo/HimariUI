package org.glavo.himari.controls;

import org.glavo.himari.layout.Alignment;
import org.glavo.himari.layout.LayoutFactory;
import org.glavo.himari.layout.LayoutModifier;
import org.glavo.himari.layout.LayoutNode;
import org.glavo.himari.layout.Size;
import org.glavo.himari.layout.semantics.SemanticsGrid;
import org.glavo.himari.layout.semantics.SemanticsGridItem;
import org.glavo.himari.layout.semantics.SemanticsRole;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/// Materializes a keyed table window with overscan, variable-height correction, and anchors.
///
/// Viewport-based materialization uses estimated row heights until [`#correctHeight(int, float)`]
/// records a measured height. Inserts and removals keep the current first materialized row key
/// visible when that key still exists.
@NotNullByDefault
public final class LazyTable {
    /// Default estimated row height.
    private static final float DEFAULT_ROW_HEIGHT = 20.0f;

    /// Default cell width.
    private static final float CELL_WIDTH = 80.0f;

    /// Column count.
    private final int columnCount;

    /// Extra rows materialized above and below the viewport.
    private final int overscan;

    /// Row keys and estimates, in document order.
    private final ArrayList<RowSpec> rows = new ArrayList<>();

    /// Measured heights parallel to [`#rows`]. Zero means the estimate is still in use.
    private final ArrayList<Float> measured = new ArrayList<>();

    /// Viewport origin in table-local logical pixels.
    private float viewportOffset;

    /// Viewport height in logical pixels.
    private float viewportHeight;

    /// Creates an empty table.
    ///
    /// @param columnCount the positive column count
    /// @param overscan the nonnegative prefetch count
    public LazyTable(int columnCount, int overscan) {
        if (columnCount <= 0) {
            throw new IllegalArgumentException("columnCount must be positive");
        }
        if (overscan < 0) {
            throw new IllegalArgumentException("overscan must be nonnegative");
        }
        this.columnCount = columnCount;
        this.overscan = overscan;
        this.viewportHeight = DEFAULT_ROW_HEIGHT;
    }

    /// Returns the column count.
    ///
    /// @return the count
    public int columnCount() {
        return columnCount;
    }

    /// Returns the overscan in rows.
    ///
    /// @return the overscan
    public int overscan() {
        return overscan;
    }

    /// Returns the row count.
    ///
    /// @return the count
    public int rowCount() {
        return rows.size();
    }

    /// Returns the row key at `index`.
    ///
    /// @param index the row index
    /// @return the key
    public String keyAt(int index) {
        return rows.get(index).key();
    }

    /// Returns the height used for `index`, preferring a measured height.
    ///
    /// @param index the row index
    /// @return the height
    public float heightAt(int index) {
        float recorded = measured.get(index);
        return recorded > 0.0f ? recorded : rows.get(index).estimatedHeight();
    }

    /// Appends one row at the end.
    ///
    /// @param key the stable key
    /// @param estimatedHeight the positive estimated height
    public void addRow(String key, float estimatedHeight) {
        insertRow(rows.size(), key, estimatedHeight);
    }

    /// Inserts one row at `index` without moving the current first materialized key.
    ///
    /// @param index the insertion index
    /// @param key the stable key
    /// @param estimatedHeight the positive estimated height
    public void insertRow(int index, String key, float estimatedHeight) {
        Objects.requireNonNull(key, "key");
        if (index < 0 || index > rows.size()) {
            throw new IllegalArgumentException("Row index is out of range");
        }
        if (!(estimatedHeight > 0.0f) || !Float.isFinite(estimatedHeight)) {
            throw new IllegalArgumentException("Estimated row height must be finite and positive");
        }
        @Nullable String anchor = firstMaterializedKey();
        rows.add(index, new RowSpec(key, estimatedHeight));
        measured.add(index, 0.0f);
        restoreAnchor(anchor);
    }

    /// Removes the row at `index` and keeps the remaining first materialized key.
    ///
    /// @param index the row index
    public void removeRow(int index) {
        if (index < 0 || index >= rows.size()) {
            throw new IllegalArgumentException("Row index is out of range");
        }
        @Nullable String anchor = firstMaterializedKey();
        String removed = rows.get(index).key();
        rows.remove(index);
        measured.remove(index);
        if (anchor != null && !anchor.equals(removed)) {
            restoreAnchor(anchor);
        }
    }

    /// Records a measured height and keeps the current first materialized key.
    ///
    /// @param index the row index
    /// @param measuredHeight the positive measured height
    public void correctHeight(int index, float measuredHeight) {
        if (index < 0 || index >= rows.size()) {
            throw new IllegalArgumentException("Row index is out of range");
        }
        if (!(measuredHeight > 0.0f) || !Float.isFinite(measuredHeight)) {
            throw new IllegalArgumentException("Measured row height must be finite and positive");
        }
        @Nullable String anchor = firstMaterializedKey();
        measured.set(index, measuredHeight);
        restoreAnchor(anchor);
    }

    /// Replaces the viewport used for materialization.
    ///
    /// @param offset the nonnegative origin
    /// @param height the positive viewport height
    public void setViewport(float offset, float height) {
        if (!(offset >= 0.0f) || !Float.isFinite(offset)) {
            throw new IllegalArgumentException("Viewport offset must be finite and nonnegative");
        }
        if (!(height > 0.0f) || !Float.isFinite(height)) {
            throw new IllegalArgumentException("Viewport height must be finite and positive");
        }
        viewportOffset = offset;
        viewportHeight = height;
    }

    /// Returns the first in-viewport row index, excluding overscan.
    ///
    /// @return the index, or `0` when the table is empty
    public int firstVisible() {
        return window().visibleFirst();
    }

    /// Returns the first materialized row index, including leading overscan.
    ///
    /// @return the index, or `0` when the table is empty
    public int firstMaterialized() {
        return window().first();
    }

    /// Returns the exclusive last materialized row index, including trailing overscan.
    ///
    /// @return the exclusive index
    public int lastMaterialized() {
        return window().last();
    }

    /// Returns the first in-viewport row key used as the scroll anchor.
    ///
    /// @return the key, or `null` when the table is empty
    public @Nullable String firstMaterializedKey() {
        if (rows.isEmpty()) {
            return null;
        }
        return rows.get(firstVisible()).key();
    }

    /// Returns the materialized keys in document order.
    ///
    /// @return the keys
    public @Unmodifiable List<String> materializedKeys() {
        Window window = window();
        ArrayList<String> keys = new ArrayList<>();
        for (int index = window.first(); index < window.last(); index++) {
            keys.add(rows.get(index).key());
        }
        return List.copyOf(keys);
    }

    /// Builds the current window as a table of rows.
    ///
    /// @param factory the node factory
    /// @param name the diagnostic name
    /// @return the table
    public LayoutNode create(LayoutFactory factory, String name) {
        Objects.requireNonNull(factory, "factory");
        Objects.requireNonNull(name, "name");
        Window window = window();
        ArrayList<LayoutNode> rowNodes = new ArrayList<>();
        for (int index = window.first(); index < window.last(); index++) {
            RowSpec row = rows.get(index);
            ArrayList<LayoutNode> cells = new ArrayList<>();
            for (int column = 0; column < columnCount; column++) {
                LayoutNode cell = factory.leaf(
                        name + "-cell-" + row.key() + "-" + column,
                        new Size(CELL_WIDTH, heightAt(index)),
                        List.of(),
                        false,
                        SemanticsRole.TABLE_CELL,
                        row.key() + ":" + column,
                        Set.of(),
                        null
                );
                cell.setGridItem(new SemanticsGridItem(index, column));
                cells.add(cell);
            }
            rowNodes.add(factory.row(
                    name + "-row-" + row.key(),
                    Alignment.START,
                    List.of(),
                    SemanticsRole.TABLE_ROW,
                    row.key(),
                    cells.toArray(LayoutNode[]::new)
            ));
        }
        LayoutNode table = factory.column(
                name,
                Alignment.START,
                List.of(new LayoutModifier.Padding(0.0f)),
                SemanticsRole.TABLE,
                name,
                rowNodes.toArray(LayoutNode[]::new)
        );
        table.setGrid(new SemanticsGrid(rows.size(), columnCount));
        return table;
    }

    /// Computes the inclusive-exclusive materialized window.
    private Window window() {
        if (rows.isEmpty()) {
            return new Window(0, 0, 0);
        }
        float running = 0.0f;
        int visibleFirst = 0;
        for (int index = 0; index < rows.size(); index++) {
            float height = heightAt(index);
            if (running + height > viewportOffset) {
                visibleFirst = index;
                break;
            }
            running += height;
            visibleFirst = index;
        }
        int visibleLast = visibleFirst;
        float covered = 0.0f;
        while (visibleLast < rows.size() && covered < viewportHeight) {
            covered += heightAt(visibleLast);
            visibleLast++;
        }
        int first = Math.max(0, visibleFirst - overscan);
        int last = Math.min(rows.size(), visibleLast + overscan);
        if (last <= first) {
            last = Math.min(rows.size(), first + 1);
        }
        return new Window(first, last, visibleFirst);
    }

    /// Restores the viewport so `anchor` is the first materialized key when it still exists.
    private void restoreAnchor(@Nullable String anchor) {
        if (anchor == null) {
            return;
        }
        int found = -1;
        for (int index = 0; index < rows.size(); index++) {
            if (rows.get(index).key().equals(anchor)) {
                found = index;
                break;
            }
        }
        if (found < 0) {
            return;
        }
        float offset = 0.0f;
        for (int index = 0; index < found; index++) {
            offset += heightAt(index);
        }
        viewportOffset = offset;
    }

    /// Stores one row estimate.
    ///
    /// @param key the stable key
    /// @param estimatedHeight the estimated height
    public record RowSpec(String key, float estimatedHeight) {
        /// Validates the row.
        public RowSpec {
            Objects.requireNonNull(key, "key");
            if (key.isEmpty()) {
                throw new IllegalArgumentException("Row key must not be empty");
            }
            if (!(estimatedHeight > 0.0f) || !Float.isFinite(estimatedHeight)) {
                throw new IllegalArgumentException("Estimated row height must be finite and positive");
            }
        }
    }

    /// Stores a materialized window.
    ///
    /// @param first the inclusive first materialized index
    /// @param last the exclusive last materialized index
    /// @param visibleFirst the inclusive first in-viewport index
    private record Window(int first, int last, int visibleFirst) {
    }
}

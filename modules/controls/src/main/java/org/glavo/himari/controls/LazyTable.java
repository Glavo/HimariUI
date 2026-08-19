package org.glavo.himari.controls;

import org.glavo.himari.layout.Alignment;
import org.glavo.himari.layout.LayoutFactory;
import org.glavo.himari.layout.LayoutModifier;
import org.glavo.himari.layout.LayoutNode;
import org.glavo.himari.layout.Size;
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

    /// Per-column cell widths, defaulting to [`#CELL_WIDTH`].
    private final float[] columnWidths;

    /// Per-column header names, empty when unpublished.
    private final String[] columnHeaders;

    /// Row keys and estimates, in document order.
    private final ArrayList<RowSpec> rows = new ArrayList<>();

    /// Measured heights parallel to [`#rows`]. Zero means the estimate is still in use.
    private final ArrayList<Float> measured = new ArrayList<>();

    /// Per-row header names parallel to [`#rows`]. Empty when unpublished.
    private final ArrayList<String> rowHeaders = new ArrayList<>();

    /// Viewport origin in table-local logical pixels.
    private float viewportOffset;

    /// Viewport height in logical pixels.
    private float viewportHeight;

    /// Whether the table ignores scroll and mutation.
    private boolean disabled;

    /// Mounted table that receives the published disabled state.
    private @Nullable LayoutNode node;

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
        this.columnWidths = new float[columnCount];
        java.util.Arrays.fill(this.columnWidths, CELL_WIDTH);
        this.columnHeaders = new String[columnCount];
        java.util.Arrays.fill(this.columnHeaders, "");
        this.viewportHeight = DEFAULT_ROW_HEIGHT;
    }

    /// Returns the column count.
    ///
    /// @return the count
    public int columnCount() {
        return columnCount;
    }

    /// Returns the width of `column`.
    ///
    /// @param column the column index
    /// @return the width
    public float columnWidth(int column) {
        if (column < 0 || column >= columnCount) {
            throw new IllegalArgumentException("Column index is out of range");
        }
        return columnWidths[column];
    }

    /// Sets the width of `column` used when materializing cells.
    ///
    /// @param column the column index
    /// @param width the positive width
    public void setColumnWidth(int column, float width) {
        if (column < 0 || column >= columnCount) {
            throw new IllegalArgumentException("Column index is out of range");
        }
        if (!(width > 0.0f) || !Float.isFinite(width)) {
            throw new IllegalArgumentException("Column width must be finite and positive");
        }
        columnWidths[column] = width;
    }

    /// Sets the accessible name of `column` published as a column header.
    ///
    /// @param column the column index
    /// @param name the header name
    public void setColumnHeader(int column, String name) {
        if (column < 0 || column >= columnCount) {
            throw new IllegalArgumentException("Column index is out of range");
        }
        columnHeaders[column] = Objects.requireNonNull(name, "name");
    }

    /// Returns the column-header name of `column`, empty when unpublished.
    ///
    /// @param column the column index
    /// @return the name
    public String columnHeader(int column) {
        if (column < 0 || column >= columnCount) {
            throw new IllegalArgumentException("Column index is out of range");
        }
        return columnHeaders[column];
    }

    /// Sets the accessible name of the row at `index` published as a row header.
    ///
    /// @param index the row index
    /// @param name the header name
    public void setRowHeader(int index, String name) {
        if (index < 0 || index >= rows.size()) {
            throw new IllegalArgumentException("Row index is out of range");
        }
        rowHeaders.set(index, Objects.requireNonNull(name, "name"));
    }

    /// Returns the row-header name at `index`, empty when unpublished.
    ///
    /// @param index the row index
    /// @return the name
    public String rowHeader(int index) {
        if (index < 0 || index >= rows.size()) {
            throw new IllegalArgumentException("Row index is out of range");
        }
        return rowHeaders.get(index);
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

    /// Returns whether the table is disabled.
    ///
    /// @return whether the table is disabled
    public boolean disabled() {
        return disabled;
    }

    /// Sets the disabled state and publishes it to the mounted table when present.
    ///
    /// @param disabled the state
    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
        if (node != null) {
            node.setDisabled(disabled);
        }
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
        if (disabled) {
            return;
        }
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
        rowHeaders.add(index, "");
        restoreAnchor(anchor);
    }

    /// Removes the row at `index` and keeps the remaining first materialized key.
    ///
    /// @param index the row index
    public void removeRow(int index) {
        if (disabled) {
            return;
        }
        if (index < 0 || index >= rows.size()) {
            throw new IllegalArgumentException("Row index is out of range");
        }
        @Nullable String anchor = firstMaterializedKey();
        String removed = rows.get(index).key();
        rows.remove(index);
        measured.remove(index);
        rowHeaders.remove(index);
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

    /// Scrolls so `index` is the first visible row, clamped to the valid range.
    ///
    /// Updates [`#viewportOffset()`] to the sum of row heights before `index`.
    ///
    /// @param index the requested first-visible row
    public void scrollTo(int index) {
        if (disabled) {
            return;
        }
        if (rows.isEmpty()) {
            viewportOffset = 0.0f;
            return;
        }
        int maximum = Math.max(0, rows.size() - 1);
        int clamped = Math.min(maximum, Math.max(0, index));
        float offset = 0.0f;
        for (int row = 0; row < clamped; row++) {
            offset += heightAt(row);
        }
        viewportOffset = offset;
    }

    /// Pages the viewport by `pages` windows of the current viewport height.
    ///
    /// @param pages signed page count; negative pages backward
    public void page(int pages) {
        int first = firstVisible();
        int visibleCount = 0;
        float covered = 0.0f;
        while (first + visibleCount < rows.size() && covered < viewportHeight) {
            covered += heightAt(first + visibleCount);
            visibleCount++;
        }
        if (visibleCount < 1) {
            visibleCount = 1;
        }
        scrollTo(first + pages * visibleCount);
    }

    /// Returns the viewport origin in table-local logical pixels.
    ///
    /// @return the origin
    public float viewportOffset() {
        return viewportOffset;
    }

    /// Returns keys for every logical row, including unmounted rows.
    ///
    /// @return the keys in document order
    public @Unmodifiable List<String> logicalLabels() {
        ArrayList<String> labels = new ArrayList<>(rows.size());
        for (int index = 0; index < rows.size(); index++) {
            labels.add(rows.get(index).key());
        }
        return List.copyOf(labels);
    }

    /// Returns keys for rows outside the materialized window.
    ///
    /// @return the unmounted keys in document order
    public @Unmodifiable List<String> unmountedLabels() {
        Window window = window();
        ArrayList<String> labels = new ArrayList<>();
        for (int index = 0; index < rows.size(); index++) {
            if (index < window.first() || index >= window.last()) {
                labels.add(rows.get(index).key());
            }
        }
        return List.copyOf(labels);
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
                String columnHeader = columnHeaders[column];
                String rowHeader = rowHeaders.get(index);
                boolean rowHeaderCell = !rowHeader.isEmpty() && column == 0;
                SemanticsRole cellRole = rowHeaderCell
                        ? SemanticsRole.TABLE_ROW_HEADER
                        : SemanticsRole.TABLE_CELL;
                LayoutNode cell = factory.leaf(
                        name + "-cell-" + row.key() + "-" + column,
                        new Size(columnWidths[column], heightAt(index)),
                        List.of(),
                        false,
                        cellRole,
                        rowHeaderCell ? rowHeader : row.key() + ":" + column,
                        Set.of(),
                        null
                );
                cell.setGridItem(new SemanticsGridItem(index, column, 1, 1, columnHeader, rowHeader));
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
        if (hasColumnHeaders()) {
            ArrayList<LayoutNode> headerCells = new ArrayList<>();
            for (int column = 0; column < columnCount; column++) {
                LayoutNode header = factory.leaf(
                        name + "-column-header-" + column,
                        new Size(columnWidths[column], DEFAULT_ROW_HEIGHT),
                        List.of(),
                        false,
                        SemanticsRole.TABLE_COLUMN_HEADER,
                        columnHeaders[column],
                        Set.of(),
                        null
                );
                header.setGridItem(new SemanticsGridItem(0, column, 1, 1, columnHeaders[column], ""));
                headerCells.add(header);
            }
            rowNodes.add(0, factory.row(
                    name + "-column-headers",
                    Alignment.START,
                    List.of(),
                    SemanticsRole.TABLE_ROW,
                    name + "-headers",
                    headerCells.toArray(LayoutNode[]::new)
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
        table.setGrid(new SemanticsGrid(rows.size(), columnCount, publishedColumnHeaders(), publishedRowHeaders()));
        table.setScroll(scrollSnapshot());
        table.setDisabled(disabled);
        this.node = table;
        return table;
    }

    /// Returns whether any column header name is non-empty.
    private boolean hasColumnHeaders() {
        for (String header : columnHeaders) {
            if (!header.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /// Returns non-empty column-header names.
    private String[] publishedColumnHeaders() {
        ArrayList<String> names = new ArrayList<>();
        for (String header : columnHeaders) {
            if (!header.isEmpty()) {
                names.add(header);
            }
        }
        return names.toArray(String[]::new);
    }

    /// Returns row-header names when any row header is non-empty.
    private String[] publishedRowHeaders() {
        ArrayList<String> names = new ArrayList<>();
        boolean any = false;
        for (String header : rowHeaders) {
            if (!header.isEmpty()) {
                any = true;
            }
            names.add(header);
        }
        return any ? names.toArray(String[]::new) : new String[0];
    }

    /// Builds the vertical-scroll snapshot for the current viewport.
    ///
    /// @return the snapshot
    public SemanticsScroll scrollSnapshot() {
        float content = 0.0f;
        for (int index = 0; index < rows.size(); index++) {
            content += heightAt(index);
        }
        if (content <= viewportHeight || content <= 0.0f) {
            return new SemanticsScroll(0.0, 100.0, false);
        }
        float maximum = content - viewportHeight;
        double percent = 100.0 * Math.min(1.0, Math.max(0.0, viewportOffset / maximum));
        double viewSize = 100.0 * viewportHeight / content;
        return new SemanticsScroll(percent, viewSize, true);
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

package org.glavo.himari.layout.semantics;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Arrays;
import java.util.Objects;

/// Publishes a table or grid extent for accessibility.
///
/// @param rowCount the nonnegative row count
/// @param columnCount the nonnegative column count
/// @param columnHeaders column-header names, empty when the table has no column headers
/// @param rowHeaders row-header names, empty when the table has no row headers
@NotNullByDefault
public record SemanticsGrid(
        int rowCount,
        int columnCount,
        String @Unmodifiable [] columnHeaders,
        String @Unmodifiable [] rowHeaders
) {
    /// Validates the extent.
    public SemanticsGrid {
        if (rowCount < 0 || columnCount < 0) {
            throw new IllegalArgumentException("Grid extents must be nonnegative");
        }
        Objects.requireNonNull(columnHeaders, "columnHeaders");
        Objects.requireNonNull(rowHeaders, "rowHeaders");
        if (columnHeaders.length > columnCount) {
            throw new IllegalArgumentException("Column header count exceeds columnCount");
        }
        if (rowHeaders.length > rowCount) {
            throw new IllegalArgumentException("Row header count exceeds rowCount");
        }
        for (String header : columnHeaders) {
            Objects.requireNonNull(header, "columnHeaders");
        }
        for (String header : rowHeaders) {
            Objects.requireNonNull(header, "rowHeaders");
        }
        columnHeaders = Arrays.copyOf(columnHeaders, columnHeaders.length);
        rowHeaders = Arrays.copyOf(rowHeaders, rowHeaders.length);
    }

    /// Creates a grid with no header names.
    ///
    /// @param rowCount the nonnegative row count
    /// @param columnCount the nonnegative column count
    public SemanticsGrid(int rowCount, int columnCount) {
        this(rowCount, columnCount, new String[0], new String[0]);
    }
}

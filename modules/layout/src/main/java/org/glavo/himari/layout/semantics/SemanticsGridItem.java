package org.glavo.himari.layout.semantics;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Publishes one cell's position inside a table or grid.
///
/// @param row the nonnegative zero-based row
/// @param column the nonnegative zero-based column
/// @param rowSpan the positive row span
/// @param columnSpan the positive column span
/// @param columnHeader the column-header name, empty when absent
/// @param rowHeader the row-header name, empty when absent
@NotNullByDefault
public record SemanticsGridItem(
        int row,
        int column,
        int rowSpan,
        int columnSpan,
        String columnHeader,
        String rowHeader
) {
    /// Validates the cell.
    public SemanticsGridItem {
        if (row < 0 || column < 0) {
            throw new IllegalArgumentException("Grid item row and column must be nonnegative");
        }
        if (rowSpan <= 0 || columnSpan <= 0) {
            throw new IllegalArgumentException("Grid item spans must be positive");
        }
        Objects.requireNonNull(columnHeader, "columnHeader");
        Objects.requireNonNull(rowHeader, "rowHeader");
    }

    /// Creates a one-by-one cell without header names.
    ///
    /// @param row the nonnegative zero-based row
    /// @param column the nonnegative zero-based column
    public SemanticsGridItem(int row, int column) {
        this(row, column, 1, 1, "", "");
    }

    /// Creates a spanned cell without header names.
    ///
    /// @param row the nonnegative zero-based row
    /// @param column the nonnegative zero-based column
    /// @param rowSpan the positive row span
    /// @param columnSpan the positive column span
    public SemanticsGridItem(int row, int column, int rowSpan, int columnSpan) {
        this(row, column, rowSpan, columnSpan, "", "");
    }
}

package org.glavo.himari.layout.semantics;

import org.jetbrains.annotations.NotNullByDefault;

/// Publishes one cell's position inside a table or grid.
///
/// @param row the nonnegative zero-based row
/// @param column the nonnegative zero-based column
/// @param rowSpan the positive row span
/// @param columnSpan the positive column span
@NotNullByDefault
public record SemanticsGridItem(int row, int column, int rowSpan, int columnSpan) {
    /// Validates the cell.
    public SemanticsGridItem {
        if (row < 0 || column < 0) {
            throw new IllegalArgumentException("Grid item row and column must be nonnegative");
        }
        if (rowSpan <= 0 || columnSpan <= 0) {
            throw new IllegalArgumentException("Grid item spans must be positive");
        }
    }

    /// Creates a one-by-one cell.
    ///
    /// @param row the nonnegative zero-based row
    /// @param column the nonnegative zero-based column
    public SemanticsGridItem(int row, int column) {
        this(row, column, 1, 1);
    }
}

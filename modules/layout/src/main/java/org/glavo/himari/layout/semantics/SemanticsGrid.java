package org.glavo.himari.layout.semantics;

import org.jetbrains.annotations.NotNullByDefault;

/// Publishes a table or grid extent for accessibility.
///
/// @param rowCount the nonnegative row count
/// @param columnCount the nonnegative column count
@NotNullByDefault
public record SemanticsGrid(int rowCount, int columnCount) {
    /// Validates the extent.
    public SemanticsGrid {
        if (rowCount < 0 || columnCount < 0) {
            throw new IllegalArgumentException("Grid extents must be nonnegative");
        }
    }
}

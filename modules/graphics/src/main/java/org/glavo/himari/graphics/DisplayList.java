package org.glavo.himari.graphics;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

/// Stores an immutable pointer-free display list.
///
/// @param ops the commands in painter order
@NotNullByDefault
public record DisplayList(@Unmodifiable List<DisplayListOp> ops) {
    /// An empty display list.
    public static final DisplayList EMPTY = new DisplayList(List.of());

    /// Validates the list.
    public DisplayList {
        ops = List.copyOf(ops);
    }
}

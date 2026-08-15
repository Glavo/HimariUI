package org.glavo.himari.layout.semantics;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Objects;

/// Captures the committed semantics tree.
///
/// @param nodes depth-first nodes
/// @param focusedId the focused node identity, or `null`
@NotNullByDefault
public record SemanticsSnapshot(
        @Unmodifiable List<SemanticsNode> nodes,
        @Nullable Long focusedId
) {
    /// Validates the snapshot.
    public SemanticsSnapshot {
        nodes = List.copyOf(nodes);
    }

    /// Returns the first node that exposes the action.
    ///
    /// @param action the required action
    /// @return the node
    /// @throws IllegalArgumentException if no node exposes the action
    public SemanticsNode nodeWith(SemanticsAction action) {
        Objects.requireNonNull(action, "action");
        for (SemanticsNode node : nodes) {
            if (node.actions().contains(action)) {
                return node;
            }
        }
        throw new IllegalArgumentException("No semantics node exposes " + action);
    }
}

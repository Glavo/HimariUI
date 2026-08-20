package org.glavo.himari.controls;

import org.glavo.himari.layout.Alignment;
import org.glavo.himari.layout.LayoutFactory;
import org.glavo.himari.layout.LayoutModifier;
import org.glavo.himari.layout.LayoutNode;
import org.glavo.himari.layout.Size;
import org.glavo.himari.layout.semantics.SemanticsRole;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/// Creates an unstyled two-column grid of four compact leaves.
@NotNullByDefault
public final class Grid {
    /// Shared child size.
    private static final Size CELL = new Size(16.0f, 12.0f);

    /// Column count.
    private final int columns;

    /// Whether the grid ignores mutation.
    private boolean disabled;

    /// Mounted grid that receives the published disabled state.
    private @Nullable LayoutNode node;

    /// Creates a two-column grid.
    public Grid() {
        this(2);
    }

    /// Creates a grid.
    ///
    /// @param columns the positive column count
    public Grid(int columns) {
        if (columns <= 0) {
            throw new IllegalArgumentException("columns must be positive");
        }
        this.columns = columns;
    }

    /// Returns the column count.
    ///
    /// @return the count
    public int columns() {
        return columns;
    }

    /// Returns whether the grid is disabled.
    ///
    /// @return whether the grid is disabled
    public boolean disabled() {
        return disabled;
    }

    /// Sets the disabled state and publishes it to the mounted grid when present.
    ///
    /// @param disabled the state
    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
        if (node != null) {
            node.setDisabled(disabled);
        }
    }

    /// Builds the grid.
    ///
    /// @param factory the node factory
    /// @param name the diagnostic name
    /// @return the grid
    public LayoutNode create(LayoutFactory factory, String name) {
        Objects.requireNonNull(factory, "factory");
        Objects.requireNonNull(name, "name");
        LayoutNode grid = factory.grid(
                name,
                columns,
                Alignment.START,
                List.of(new LayoutModifier.Padding(0.0f)),
                cell(factory, name + "-0", "A"),
                cell(factory, name + "-1", "B"),
                cell(factory, name + "-2", "C"),
                cell(factory, name + "-3", "D")
        );
        grid.setDisabled(disabled);
        this.node = grid;
        return grid;
    }

    /// Builds one compact leaf.
    ///
    /// @param factory the node factory
    /// @param name the diagnostic name
    /// @param label the leaf label
    /// @return the leaf
    private static LayoutNode cell(LayoutFactory factory, String name, String label) {
        return factory.leaf(name, CELL, List.of(), false, SemanticsRole.NONE, label, Set.of(), null);
    }
}

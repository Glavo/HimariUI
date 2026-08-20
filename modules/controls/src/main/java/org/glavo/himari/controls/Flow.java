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

/// Creates an unstyled wrapping flow of three compact leaves.
@NotNullByDefault
public final class Flow {
    /// Shared child size.
    private static final Size CELL = new Size(16.0f, 12.0f);

    /// Whether the flow ignores mutation.
    private boolean disabled;

    /// Mounted flow that receives the published disabled state.
    private @Nullable LayoutNode node;

    /// Creates a flow.
    public Flow() {
    }

    /// Returns whether the flow is disabled.
    ///
    /// @return whether the flow is disabled
    public boolean disabled() {
        return disabled;
    }

    /// Sets the disabled state and publishes it to the mounted flow when present.
    ///
    /// @param disabled the state
    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
        if (node != null) {
            node.setDisabled(disabled);
        }
    }

    /// Builds the wrapping flow.
    ///
    /// @param factory the node factory
    /// @param name the diagnostic name
    /// @return the flow
    public LayoutNode create(LayoutFactory factory, String name) {
        Objects.requireNonNull(factory, "factory");
        Objects.requireNonNull(name, "name");
        LayoutNode first = cell(factory, name + "-0", "A");
        LayoutNode second = cell(factory, name + "-1", "B");
        LayoutNode third = cell(factory, name + "-2", "C");
        LayoutNode flow = factory.flow(
                name,
                Alignment.START,
                List.of(new LayoutModifier.Padding(0.0f)),
                first,
                second,
                third
        );
        flow.setDisabled(disabled);
        this.node = flow;
        return flow;
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

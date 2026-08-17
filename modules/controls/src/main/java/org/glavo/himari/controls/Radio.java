package org.glavo.himari.controls;

import org.glavo.himari.layout.Alignment;
import org.glavo.himari.layout.LayoutFactory;
import org.glavo.himari.layout.LayoutModifier;
import org.glavo.himari.layout.LayoutNode;
import org.glavo.himari.layout.Size;
import org.glavo.himari.layout.semantics.SemanticsAction;
import org.glavo.himari.layout.semantics.SemanticsRole;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/// Creates an unstyled exclusive radio group.
@NotNullByDefault
public final class Radio {
    /// Option size.
    private static final Size SIZE = new Size(120.0f, 20.0f);

    /// Option labels in document order.
    private final List<String> options;

    /// Selected option index.
    private int selected;

    /// Whether the group ignores selection.
    private boolean disabled;

    /// Mounted option leaves that receive the published disabled state.
    private @Nullable List<LayoutNode> nodes;

    /// Creates a radio group with the first option selected.
    ///
    /// @param options the non-empty option labels
    public Radio(List<String> options) {
        this.options = List.copyOf(options);
        if (this.options.size() < 2) {
            throw new IllegalArgumentException("Radio group must contain at least two options");
        }
        for (String option : this.options) {
            Objects.requireNonNull(option, "option");
            if (option.isEmpty()) {
                throw new IllegalArgumentException("Radio option must not be empty");
            }
        }
    }

    /// Returns the options.
    ///
    /// @return the options
    public @Unmodifiable List<String> options() {
        return options;
    }

    /// Returns the selected index.
    ///
    /// @return the index
    public int selected() {
        return selected;
    }

    /// Selects `index`.
    ///
    /// @param index the option index
    public void select(int index) {
        if (index < 0 || index >= options.size()) {
            throw new IllegalArgumentException("Radio index is out of range");
        }
        if (disabled) {
            return;
        }
        selected = index;
        if (nodes != null) {
            for (int option = 0; option < nodes.size(); option++) {
                nodes.get(option).setSelected(option == selected);
            }
        }
    }

    /// Returns whether the group is disabled.
    ///
    /// @return whether the group is disabled
    public boolean disabled() {
        return disabled;
    }

    /// Sets the disabled state and publishes it to mounted option leaves when present.
    ///
    /// @param disabled the state
    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
        if (nodes != null) {
            for (LayoutNode option : nodes) {
                option.setDisabled(disabled);
            }
        }
    }

    /// Builds a column of exclusive radio options.
    ///
    /// @param factory the node factory
    /// @param name the diagnostic name
    /// @return the column
    public LayoutNode create(LayoutFactory factory, String name) {
        Objects.requireNonNull(factory, "factory");
        Objects.requireNonNull(name, "name");
        ArrayList<LayoutNode> nodes = new ArrayList<>();
        for (int index = 0; index < options.size(); index++) {
            int target = index;
            LayoutNode option = factory.leaf(
                    name + "-option-" + index,
                    SIZE,
                    List.of(),
                    true,
                    SemanticsRole.RADIO,
                    options.get(index),
                    Set.of(SemanticsAction.ACTIVATE),
                    () -> select(target)
            );
            option.setSelected(index == selected);
            option.setDisabled(disabled);
            nodes.add(option);
        }
        this.nodes = List.copyOf(nodes);
        return factory.column(
                name,
                Alignment.START,
                List.of(new LayoutModifier.Padding(0.0f)),
                nodes.toArray(LayoutNode[]::new)
        );
    }
}

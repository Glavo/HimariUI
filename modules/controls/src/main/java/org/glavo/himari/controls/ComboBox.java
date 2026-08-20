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

import java.util.List;
import java.util.Objects;
import java.util.Set;

/// Creates an unstyled combo box with a collapsed value and expandable options.
@NotNullByDefault
public final class ComboBox {
    /// Collapsed control size.
    private static final Size SIZE = new Size(160.0f, 24.0f);

    /// Option labels in document order.
    private final List<String> options;

    /// Selected option index.
    private int selected;

    /// Whether the option list is open.
    private boolean open;

    /// Whether the control ignores activation.
    private boolean disabled;

    /// Mounted combo-box leaf.
    private @Nullable LayoutNode node;

    /// Creates a combo box with the first option selected and the list collapsed.
    ///
    /// @param options the non-empty option labels
    public ComboBox(List<String> options) {
        this.options = List.copyOf(options);
        if (this.options.size() < 2) {
            throw new IllegalArgumentException("Combo box must contain at least two options");
        }
        for (String option : this.options) {
            Objects.requireNonNull(option, "option");
            if (option.isEmpty()) {
                throw new IllegalArgumentException("Combo box option must not be empty");
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

    /// Returns the selected option label.
    ///
    /// @return the label
    public String value() {
        return options.get(selected);
    }

    /// Returns whether the option list is open.
    ///
    /// @return whether it is open
    public boolean isOpen() {
        return open;
    }

    /// Selects `index` and publishes the new value when mounted.
    ///
    /// @param index the option index
    public void select(int index) {
        if (index < 0 || index >= options.size()) {
            throw new IllegalArgumentException("Combo box index is out of range");
        }
        if (disabled) {
            return;
        }
        selected = index;
        publish();
    }

    /// Opens or closes the option list.
    ///
    /// @param open the next open state
    public void setOpen(boolean open) {
        if (disabled) {
            return;
        }
        this.open = open;
        publish();
    }

    /// Toggles the option list.
    public void toggle() {
        setOpen(!open);
    }

    /// Selects the next option, wrapping at the end.
    public void selectNext() {
        if (disabled) {
            return;
        }
        selected = (selected + 1) % options.size();
        publish();
    }

    /// Selects the previous option, wrapping at the start.
    public void selectPrevious() {
        if (disabled) {
            return;
        }
        selected = (selected + options.size() - 1) % options.size();
        publish();
    }

    /// Returns whether the control is disabled.
    ///
    /// @return whether the control is disabled
    public boolean disabled() {
        return disabled;
    }

    /// Sets the disabled state and publishes it when mounted.
    ///
    /// @param disabled the state
    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
        publish();
    }

    /// Builds the combo-box leaf.
    ///
    /// @param factory the node factory
    /// @param name the diagnostic name
    /// @return the leaf
    public LayoutNode create(LayoutFactory factory, String name) {
        Objects.requireNonNull(factory, "factory");
        Objects.requireNonNull(name, "name");
        LayoutNode leaf = factory.leaf(
                name,
                SIZE,
                List.of(),
                true,
                SemanticsRole.COMBO_BOX,
                value(),
                Set.of(SemanticsAction.ACTIVATE, SemanticsAction.INCREMENT, SemanticsAction.DECREMENT),
                this::toggle,
                delta -> {
                    if (delta > 0) {
                        selectNext();
                    } else {
                        selectPrevious();
                    }
                }
        );
        this.node = leaf;
        publish();
        return factory.column(
                name + "-host",
                Alignment.START,
                List.of(new LayoutModifier.Padding(0.0f)),
                leaf
        );
    }

    /// Publishes value, expand state, and disabled onto the mounted leaf.
    private void publish() {
        if (node == null) {
            return;
        }
        node.setLabel(value());
        node.setDisabled(disabled);
        node.setItemStatus(open ? "expanded" : "collapsed");
        node.setPositionInSet(selected);
        node.setSizeOfSet(options.size());
    }
}

package org.glavo.himari.controls;

import org.glavo.himari.layout.LayoutFactory;
import org.glavo.himari.layout.LayoutModifier;
import org.glavo.himari.layout.LayoutNode;
import org.glavo.himari.layout.Size;
import org.glavo.himari.layout.semantics.SemanticsAction;
import org.glavo.himari.layout.semantics.SemanticsRole;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/// Creates an unstyled selectable filter chip.
@NotNullByDefault
public final class Chip {
    /// Default control size.
    private static final Size SIZE = new Size(88.0f, 24.0f);

    /// Accessible name.
    private final String label;

    /// Whether the chip is selected.
    private boolean selected;

    /// Whether the chip ignores activation.
    private boolean disabled;

    /// Mounted leaf that receives the published selection.
    private @Nullable LayoutNode node;

    /// Creates an unselected chip.
    ///
    /// @param label the accessible name
    public Chip(String label) {
        this.label = Objects.requireNonNull(label, "label");
        if (this.label.isEmpty()) {
            throw new IllegalArgumentException("Chip label must not be empty");
        }
    }

    /// Returns the accessible name.
    ///
    /// @return the label
    public String label() {
        return label;
    }

    /// Returns whether the chip is selected.
    ///
    /// @return whether it is selected
    public boolean selected() {
        return selected;
    }

    /// Returns whether the chip is disabled.
    ///
    /// @return whether the chip is disabled
    public boolean disabled() {
        return disabled;
    }

    /// Sets the selection and publishes it when mounted.
    ///
    /// @param selected the next state
    public void setSelected(boolean selected) {
        if (disabled) {
            return;
        }
        this.selected = selected;
        publish();
    }

    /// Toggles the selection.
    public void toggle() {
        setSelected(!selected);
    }

    /// Sets the disabled state and publishes it when mounted.
    ///
    /// @param disabled the state
    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
        publish();
    }

    /// Builds the chip leaf.
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
                List.of(new LayoutModifier.Padding(0.0f)),
                true,
                SemanticsRole.CHIP,
                label,
                Set.of(SemanticsAction.ACTIVATE),
                this::toggle
        );
        this.node = leaf;
        publish();
        return leaf;
    }

    /// Publishes selection and disabled onto the mounted leaf.
    private void publish() {
        if (node == null) {
            return;
        }
        node.setLabel(label);
        node.setSelected(selected);
        node.setDisabled(disabled);
        node.setItemStatus(selected ? "selected" : "unselected");
    }
}

package org.glavo.himari.controls;

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

/// Creates an unstyled toolbar of labeled commands.
@NotNullByDefault
public final class Toolbar {
    /// Default control size.
    private static final Size SIZE = new Size(200.0f, 28.0f);

    /// Command labels in document order.
    private final List<String> items;

    /// Selected command index.
    private int selected;

    /// Last activated command index, or `-1` when none has been activated.
    private int lastActivated = -1;

    /// Whether the control ignores activation.
    private boolean disabled;

    /// Mounted leaf that receives the published selection.
    private @Nullable LayoutNode node;

    /// Creates a toolbar.
    ///
    /// @param items the non-empty command labels
    public Toolbar(List<String> items) {
        this.items = List.copyOf(items);
        if (this.items.isEmpty()) {
            throw new IllegalArgumentException("Toolbar must contain at least one item");
        }
        for (String item : this.items) {
            Objects.requireNonNull(item, "item");
            if (item.isEmpty()) {
                throw new IllegalArgumentException("Toolbar item must not be empty");
            }
        }
    }

    /// Returns the command labels.
    ///
    /// @return the labels
    public @Unmodifiable List<String> items() {
        return items;
    }

    /// Returns the selected index.
    ///
    /// @return the index
    public int selected() {
        return selected;
    }

    /// Returns the selected command label.
    ///
    /// @return the label
    public String value() {
        return items.get(selected);
    }

    /// Returns the last activated index, or `-1` when none has been activated.
    ///
    /// @return the index
    public int lastActivated() {
        return lastActivated;
    }

    /// Returns whether the control is disabled.
    ///
    /// @return whether the control is disabled
    public boolean disabled() {
        return disabled;
    }

    /// Selects `index` and publishes it when mounted.
    ///
    /// @param index the command index
    public void select(int index) {
        if (index < 0 || index >= items.size()) {
            throw new IllegalArgumentException("Toolbar index is out of range");
        }
        if (disabled) {
            return;
        }
        selected = index;
        publish();
    }

    /// Selects the next command, wrapping at the end.
    public void selectNext() {
        if (disabled) {
            return;
        }
        selected = (selected + 1) % items.size();
        publish();
    }

    /// Selects the previous command, wrapping at the start.
    public void selectPrevious() {
        if (disabled) {
            return;
        }
        selected = (selected + items.size() - 1) % items.size();
        publish();
    }

    /// Activates the selected command.
    public void activate() {
        if (disabled) {
            return;
        }
        lastActivated = selected;
        publish();
    }

    /// Sets the disabled state and publishes it when mounted.
    ///
    /// @param disabled the state
    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
        publish();
    }

    /// Builds the toolbar leaf.
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
                SemanticsRole.TOOLBAR,
                value(),
                Set.of(SemanticsAction.ACTIVATE, SemanticsAction.INCREMENT, SemanticsAction.DECREMENT),
                this::activate,
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
        return leaf;
    }

    /// Publishes selection and disabled onto the mounted leaf.
    private void publish() {
        if (node == null) {
            return;
        }
        node.setLabel(value());
        node.setDisabled(disabled);
        node.setPositionInSet(selected);
        node.setSizeOfSet(items.size());
    }
}

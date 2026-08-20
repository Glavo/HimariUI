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

/// Creates an unstyled disclosure control that expands or collapses a labeled section.
@NotNullByDefault
public final class Disclosure {
    /// Default control size.
    private static final Size SIZE = new Size(160.0f, 24.0f);

    /// Accessible name.
    private final String label;

    /// Whether the section is expanded.
    private boolean expanded;

    /// Whether the control ignores activation.
    private boolean disabled;

    /// Mounted leaf that receives the published expand state.
    private @Nullable LayoutNode node;

    /// Creates a collapsed disclosure.
    ///
    /// @param label the accessible name
    public Disclosure(String label) {
        this.label = Objects.requireNonNull(label, "label");
    }

    /// Returns the accessible name.
    ///
    /// @return the label
    public String label() {
        return label;
    }

    /// Returns whether the section is expanded.
    ///
    /// @return whether it is expanded
    public boolean isExpanded() {
        return expanded;
    }

    /// Returns whether the control is disabled.
    ///
    /// @return whether the control is disabled
    public boolean disabled() {
        return disabled;
    }

    /// Sets the expanded state and publishes it when mounted.
    ///
    /// @param expanded the next state
    public void setExpanded(boolean expanded) {
        if (disabled) {
            return;
        }
        this.expanded = expanded;
        publish();
    }

    /// Toggles the expanded state.
    public void toggle() {
        setExpanded(!expanded);
    }

    /// Sets the disabled state and publishes it when mounted.
    ///
    /// @param disabled the state
    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
        publish();
    }

    /// Builds the disclosure leaf.
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
                SemanticsRole.DISCLOSURE,
                label,
                Set.of(SemanticsAction.ACTIVATE),
                this::toggle
        );
        this.node = leaf;
        publish();
        return leaf;
    }

    /// Publishes expand state and disabled onto the mounted leaf.
    private void publish() {
        if (node == null) {
            return;
        }
        node.setLabel(label);
        node.setDisabled(disabled);
        node.setItemStatus(expanded ? "expanded" : "collapsed");
        node.setSelected(expanded);
    }
}

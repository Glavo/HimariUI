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

/// Creates an unstyled hyperlink.
@NotNullByDefault
public final class Hyperlink {
    /// Default control size.
    private static final Size SIZE = new Size(160.0f, 20.0f);

    /// Accessible name.
    private final String label;

    /// Destination identifier.
    private final String href;

    /// Number of activations.
    private int activations;

    /// Whether the control ignores activation.
    private boolean disabled;

    /// Mounted leaf that receives the published state.
    private @Nullable LayoutNode node;

    /// Creates a hyperlink.
    ///
    /// @param label the accessible name
    /// @param href the destination identifier
    public Hyperlink(String label, String href) {
        this.label = Objects.requireNonNull(label, "label");
        this.href = Objects.requireNonNull(href, "href");
        if (this.label.isEmpty() || this.href.isEmpty()) {
            throw new IllegalArgumentException("Hyperlink label and href must not be empty");
        }
    }

    /// Returns the accessible name.
    ///
    /// @return the label
    public String label() {
        return label;
    }

    /// Returns the destination identifier.
    ///
    /// @return the href
    public String href() {
        return href;
    }

    /// Returns how many times the link has been activated.
    ///
    /// @return the count
    public int activations() {
        return activations;
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

    /// Activates the link.
    public void activate() {
        if (disabled) {
            return;
        }
        activations++;
        publish();
    }

    /// Builds the hyperlink leaf.
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
                SemanticsRole.LINK,
                label,
                Set.of(SemanticsAction.ACTIVATE),
                this::activate
        );
        this.node = leaf;
        publish();
        return leaf;
    }

    /// Publishes href and disabled onto the mounted leaf.
    private void publish() {
        if (node == null) {
            return;
        }
        node.setLabel(label);
        node.setDisabled(disabled);
        node.setItemStatus(href);
    }
}

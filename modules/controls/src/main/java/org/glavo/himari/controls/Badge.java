package org.glavo.himari.controls;

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

/// Creates an unstyled non-interactive status badge.
@NotNullByDefault
public final class Badge {
    /// Default control size.
    private static final Size SIZE = new Size(72.0f, 20.0f);

    /// Visible label.
    private String label;

    /// Whether the badge is disabled.
    private boolean disabled;

    /// Mounted leaf that receives the published label.
    private @Nullable LayoutNode node;

    /// Creates a badge.
    ///
    /// @param label the visible text
    public Badge(String label) {
        this.label = Objects.requireNonNull(label, "label");
        if (this.label.isEmpty()) {
            throw new IllegalArgumentException("Badge label must not be empty");
        }
    }

    /// Returns the visible text.
    ///
    /// @return the label
    public String label() {
        return label;
    }

    /// Returns whether the badge is disabled.
    ///
    /// @return whether the badge is disabled
    public boolean disabled() {
        return disabled;
    }

    /// Replaces the visible text and publishes it when mounted.
    ///
    /// @param label the next text
    public void setLabel(String label) {
        this.label = Objects.requireNonNull(label, "label");
        if (this.label.isEmpty()) {
            throw new IllegalArgumentException("Badge label must not be empty");
        }
        publish();
    }

    /// Sets the disabled state and publishes it when mounted.
    ///
    /// @param disabled the state
    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
        publish();
    }

    /// Builds the badge leaf.
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
                false,
                SemanticsRole.BADGE,
                label,
                Set.of(),
                null
        );
        this.node = leaf;
        publish();
        return leaf;
    }

    /// Publishes label and disabled onto the mounted leaf.
    private void publish() {
        if (node == null) {
            return;
        }
        node.setLabel(label);
        node.setDisabled(disabled);
    }
}

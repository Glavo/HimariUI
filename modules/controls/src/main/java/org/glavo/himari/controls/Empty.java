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

/// Creates an unstyled no-content placeholder.
///
/// Unlike [`Skeleton`], this control is not a loading stand-in. It publishes a
/// permanent empty status and does not become ready.
@NotNullByDefault
public final class Empty {
    /// Default control size.
    private static final Size SIZE = new Size(160.0f, 24.0f);

    /// Visible description.
    private final String description;

    /// Whether the placeholder is disabled.
    private boolean disabled;

    /// Mounted leaf that receives the published description.
    private @Nullable LayoutNode node;

    /// Creates an empty-state placeholder.
    ///
    /// @param description the visible text
    public Empty(String description) {
        this.description = Objects.requireNonNull(description, "description");
        if (this.description.isEmpty()) {
            throw new IllegalArgumentException("Empty description must not be empty");
        }
    }

    /// Returns the visible text.
    ///
    /// @return the description
    public String description() {
        return description;
    }

    /// Returns whether the placeholder is disabled.
    ///
    /// @return whether the placeholder is disabled
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

    /// Builds the empty-state leaf.
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
                SemanticsRole.EMPTY,
                description,
                Set.of(),
                null
        );
        this.node = leaf;
        publish();
        return leaf;
    }

    /// Publishes description and disabled onto the mounted leaf.
    private void publish() {
        if (node == null) {
            return;
        }
        node.setLabel(description);
        node.setDisabled(disabled);
        node.setItemStatus("empty");
    }
}

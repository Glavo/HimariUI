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

/// Creates an unstyled titled grouping card.
@NotNullByDefault
public final class Card {
    /// Default control size.
    private static final Size SIZE = new Size(200.0f, 48.0f);

    /// Visible title.
    private final String title;

    /// Whether the card is disabled.
    private boolean disabled;

    /// Mounted leaf that receives the published title.
    private @Nullable LayoutNode node;

    /// Creates a card.
    ///
    /// @param title the visible title
    public Card(String title) {
        this.title = Objects.requireNonNull(title, "title");
        if (this.title.isEmpty()) {
            throw new IllegalArgumentException("Card title must not be empty");
        }
    }

    /// Returns the visible title.
    ///
    /// @return the title
    public String title() {
        return title;
    }

    /// Returns whether the card is disabled.
    ///
    /// @return whether the card is disabled
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

    /// Builds the card leaf.
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
                List.of(new LayoutModifier.Padding(8.0f)),
                false,
                SemanticsRole.CARD,
                title,
                Set.of(),
                null
        );
        this.node = leaf;
        publish();
        return leaf;
    }

    /// Publishes title and disabled onto the mounted leaf.
    private void publish() {
        if (node == null) {
            return;
        }
        node.setLabel(title);
        node.setDisabled(disabled);
    }
}

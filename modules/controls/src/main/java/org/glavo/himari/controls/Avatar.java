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

/// Creates an unstyled identity avatar with initials and an optional image source.
@NotNullByDefault
public final class Avatar {
    /// Default control size.
    private static final Size SIZE = new Size(32.0f, 32.0f);

    /// Accessible name.
    private final String name;

    /// Visible initials.
    private final String initials;

    /// Optional image source, empty when unused.
    private String source = "";

    /// Whether the avatar is disabled.
    private boolean disabled;

    /// Mounted leaf that receives the published identity.
    private @Nullable LayoutNode node;

    /// Creates an avatar.
    ///
    /// @param name the accessible name
    /// @param initials the visible initials, one or two characters
    public Avatar(String name, String initials) {
        this.name = Objects.requireNonNull(name, "name");
        this.initials = Objects.requireNonNull(initials, "initials");
        if (this.name.isEmpty()) {
            throw new IllegalArgumentException("Avatar name must not be empty");
        }
        if (this.initials.isEmpty() || this.initials.length() > 2) {
            throw new IllegalArgumentException("Avatar initials must be one or two characters");
        }
    }

    /// Returns the accessible name.
    ///
    /// @return the name
    public String name() {
        return name;
    }

    /// Returns the visible initials.
    ///
    /// @return the initials
    public String initials() {
        return initials;
    }

    /// Returns the image source, empty when unused.
    ///
    /// @return the source
    public String source() {
        return source;
    }

    /// Returns whether the avatar is disabled.
    ///
    /// @return whether the avatar is disabled
    public boolean disabled() {
        return disabled;
    }

    /// Sets the image source and publishes it when mounted.
    ///
    /// @param source the source, empty to clear
    public void setSource(String source) {
        this.source = Objects.requireNonNull(source, "source");
        publish();
    }

    /// Sets the disabled state and publishes it when mounted.
    ///
    /// @param disabled the state
    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
        publish();
    }

    /// Builds the avatar leaf.
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
                SemanticsRole.AVATAR,
                this.name,
                Set.of(),
                null
        );
        this.node = leaf;
        publish();
        return leaf;
    }

    /// Publishes initials, source, and disabled onto the mounted leaf.
    private void publish() {
        if (node == null) {
            return;
        }
        node.setLabel(name);
        node.setDisabled(disabled);
        node.setItemStatus(source.isEmpty() ? initials : source);
    }
}

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

/// Creates an unstyled dismissible inline banner.
@NotNullByDefault
public final class Banner {
    /// Default control size.
    private static final Size SIZE = new Size(200.0f, 24.0f);

    /// Visible message.
    private final String message;

    /// Whether the banner is visible.
    private boolean visible = true;

    /// Whether the banner ignores activation.
    private boolean disabled;

    /// Mounted leaf that receives the published visibility.
    private @Nullable LayoutNode node;

    /// Creates a visible banner.
    ///
    /// @param message the visible text
    public Banner(String message) {
        this.message = Objects.requireNonNull(message, "message");
        if (this.message.isEmpty()) {
            throw new IllegalArgumentException("Banner message must not be empty");
        }
    }

    /// Returns the visible text.
    ///
    /// @return the message
    public String message() {
        return message;
    }

    /// Returns whether the banner is visible.
    ///
    /// @return whether it is visible
    public boolean visible() {
        return visible;
    }

    /// Returns whether the banner is disabled.
    ///
    /// @return whether the banner is disabled
    public boolean disabled() {
        return disabled;
    }

    /// Dismisses the banner when enabled.
    public void dismiss() {
        if (disabled) {
            return;
        }
        visible = false;
        publish();
    }

    /// Shows the banner when enabled.
    public void show() {
        if (disabled) {
            return;
        }
        visible = true;
        publish();
    }

    /// Sets the disabled state and publishes it when mounted.
    ///
    /// @param disabled the state
    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
        publish();
    }

    /// Builds the banner leaf.
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
                SemanticsRole.BANNER,
                message,
                Set.of(SemanticsAction.ACTIVATE),
                this::dismiss
        );
        this.node = leaf;
        publish();
        return leaf;
    }

    /// Publishes visibility and disabled onto the mounted leaf.
    private void publish() {
        if (node == null) {
            return;
        }
        node.setLabel(message);
        node.setDisabled(disabled);
        node.setItemStatus(visible ? "visible" : "dismissed");
    }
}

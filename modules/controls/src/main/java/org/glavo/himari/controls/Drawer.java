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

/// Creates an unstyled non-modal drawer pane.
///
/// Unlike [`Dialog`], the drawer is a focusable leaf rather than a [`Popup`] overlay.
/// Activation toggles open and closed. Escape and outside-pointer dismissal are not used.
@NotNullByDefault
public final class Drawer {
    /// Default control size.
    private static final Size SIZE = new Size(160.0f, 24.0f);

    /// Accessible title.
    private final String title;

    /// Whether the drawer is open.
    private boolean open;

    /// Whether the drawer ignores activation.
    private boolean disabled;

    /// Mounted leaf that receives the published open state.
    private @Nullable LayoutNode node;

    /// Creates a closed drawer.
    ///
    /// @param title the accessible title
    public Drawer(String title) {
        this.title = Objects.requireNonNull(title, "title");
        if (this.title.isEmpty()) {
            throw new IllegalArgumentException("Drawer title must not be empty");
        }
    }

    /// Returns the accessible title.
    ///
    /// @return the title
    public String title() {
        return title;
    }

    /// Returns whether the drawer is open.
    ///
    /// @return whether it is open
    public boolean isOpen() {
        return open;
    }

    /// Returns whether the drawer is disabled.
    ///
    /// @return whether the drawer is disabled
    public boolean disabled() {
        return disabled;
    }

    /// Opens the drawer when enabled.
    public void open() {
        if (disabled) {
            return;
        }
        open = true;
        publish();
    }

    /// Closes the drawer when enabled.
    public void close() {
        if (disabled) {
            return;
        }
        open = false;
        publish();
    }

    /// Toggles the open state when enabled.
    public void toggle() {
        if (open) {
            close();
        } else {
            open();
        }
    }

    /// Sets the disabled state and publishes it when mounted.
    ///
    /// @param disabled the state
    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
        publish();
    }

    /// Builds the drawer leaf.
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
                SemanticsRole.DRAWER,
                title,
                Set.of(SemanticsAction.ACTIVATE),
                this::toggle
        );
        this.node = leaf;
        publish();
        return leaf;
    }

    /// Publishes open state and disabled onto the mounted leaf.
    private void publish() {
        if (node == null) {
            return;
        }
        node.setLabel(title);
        node.setDisabled(disabled);
        node.setItemStatus(open ? "open" : "closed");
    }
}

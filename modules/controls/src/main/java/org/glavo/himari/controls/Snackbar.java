package org.glavo.himari.controls;

import org.glavo.himari.layout.LayoutFactory;
import org.glavo.himari.layout.LayoutModifier;
import org.glavo.himari.layout.LayoutNode;
import org.glavo.himari.layout.Size;
import org.glavo.himari.layout.semantics.SemanticsAction;
import org.glavo.himari.layout.semantics.SemanticsLiveRegion;
import org.glavo.himari.layout.semantics.SemanticsRole;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/// Creates an unstyled transient snackbar with an action.
///
/// The snackbar starts hidden. [`#show()`] reveals it as a polite live region.
/// Activation runs the action then dismisses. Unlike [`Banner`], it does not start visible
/// and does not dismiss without running the action.
@NotNullByDefault
public final class Snackbar {
    /// Default control size.
    private static final Size SIZE = new Size(200.0f, 24.0f);

    /// Visible message.
    private final String message;

    /// Action label.
    private final String action;

    /// Number of action invocations.
    private int actions;

    /// Whether the snackbar is visible.
    private boolean visible;

    /// Whether the snackbar ignores show and activation.
    private boolean disabled;

    /// Mounted leaf that receives the published visibility.
    private @Nullable LayoutNode node;

    /// Creates a hidden snackbar.
    ///
    /// @param message the visible text
    /// @param action the action label
    public Snackbar(String message, String action) {
        this.message = Objects.requireNonNull(message, "message");
        this.action = Objects.requireNonNull(action, "action");
        if (this.message.isEmpty() || this.action.isEmpty()) {
            throw new IllegalArgumentException("Snackbar message and action must not be empty");
        }
    }

    /// Returns the visible text.
    ///
    /// @return the message
    public String message() {
        return message;
    }

    /// Returns the action label.
    ///
    /// @return the action
    public String action() {
        return action;
    }

    /// Returns how many times the action has run.
    ///
    /// @return the count
    public int actions() {
        return actions;
    }

    /// Returns whether the snackbar is visible.
    ///
    /// @return whether it is visible
    public boolean visible() {
        return visible;
    }

    /// Returns whether the snackbar is disabled.
    ///
    /// @return whether the snackbar is disabled
    public boolean disabled() {
        return disabled;
    }

    /// Shows the snackbar when enabled.
    public void show() {
        if (disabled) {
            return;
        }
        visible = true;
        publish();
    }

    /// Hides the snackbar without running the action.
    public void dismiss() {
        if (disabled) {
            return;
        }
        visible = false;
        publish();
    }

    /// Runs the action and dismisses when enabled and visible.
    public void activate() {
        if (disabled || !visible) {
            return;
        }
        actions++;
        visible = false;
        publish();
    }

    /// Sets the disabled state and publishes it when mounted.
    ///
    /// @param disabled the state
    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
        publish();
    }

    /// Builds the snackbar leaf.
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
                SemanticsRole.SNACKBAR,
                message,
                Set.of(SemanticsAction.ACTIVATE),
                this::activate
        );
        this.node = leaf;
        publish();
        return leaf;
    }

    /// Publishes visibility, action status, and live-region politeness.
    private void publish() {
        if (node == null) {
            return;
        }
        node.setLabel(message);
        node.setDisabled(disabled);
        node.setItemStatus(visible ? action : "hidden");
        node.setLiveRegion(visible ? SemanticsLiveRegion.POLITE : SemanticsLiveRegion.OFF);
    }
}

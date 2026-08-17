package org.glavo.himari.controls;

import org.glavo.himari.layout.LayoutFactory;
import org.glavo.himari.layout.LayoutModifier;
import org.glavo.himari.layout.LayoutNode;
import org.glavo.himari.layout.LayoutTree;
import org.glavo.himari.layout.Size;
import org.glavo.himari.layout.input.KeyEvent;
import org.glavo.himari.layout.input.KeyEventType;
import org.glavo.himari.layout.input.LogicalKey;
import org.glavo.himari.layout.input.PointerEvent;
import org.glavo.himari.layout.input.PointerEventType;
import org.glavo.himari.layout.semantics.SemanticsAction;
import org.glavo.himari.layout.semantics.SemanticsNode;
import org.glavo.himari.layout.semantics.SemanticsRole;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/// Creates an in-window overlay popup with Escape and outside-pointer dismissal.
@NotNullByDefault
public final class Popup {
    /// Accessible name.
    private final String label;

    /// Surface kind.
    private final PopupKind kind;

    /// Whether the popup is visible.
    private boolean open;

    /// Whether the popup ignores show and dismissal input.
    private boolean disabled;

    /// Mounted leaf that receives the published disabled state.
    private @Nullable LayoutNode node;

    /// Creates a closed overlay popup.
    ///
    /// @param label the accessible name
    public Popup(String label) {
        this(label, PopupKind.OVERLAY);
    }

    /// Creates a closed popup of the requested kind.
    ///
    /// @param label the accessible name
    /// @param kind the surface kind
    public Popup(String label, PopupKind kind) {
        this.label = Objects.requireNonNull(label, "label");
        this.kind = Objects.requireNonNull(kind, "kind");
    }

    /// Returns the surface kind.
    ///
    /// @return the kind
    public PopupKind kind() {
        return kind;
    }

    /// Returns whether the popup is open.
    ///
    /// @return whether it is open
    public boolean isOpen() {
        return open;
    }

    /// Shows the popup.
    public void show() {
        if (disabled) {
            return;
        }
        open = true;
    }

    /// Returns whether the popup is disabled.
    ///
    /// @return whether the popup is disabled
    public boolean disabled() {
        return disabled;
    }

    /// Sets the disabled state and publishes it to the mounted leaf when present.
    ///
    /// @param disabled the state
    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
        if (node != null) {
            node.setDisabled(disabled);
        }
    }

    /// Dismisses the popup.
    public void dismiss() {
        open = false;
    }

    /// Dismisses an open popup when `Escape` is pressed.
    ///
    /// @param event the key event
    /// @return whether this popup consumed the event
    public boolean handleKey(KeyEvent event) {
        Objects.requireNonNull(event, "event");
        if (disabled || !open || event.type() != KeyEventType.DOWN || event.key() != LogicalKey.ESCAPE) {
            return false;
        }
        dismiss();
        return true;
    }

    /// Dismisses an open popup when a pointer-down lands outside its placed bounds.
    ///
    /// @param tree the placed tree that contains this popup
    /// @param event the pointer event
    /// @return whether this popup consumed the event
    public boolean handlePointer(LayoutTree tree, PointerEvent event) {
        Objects.requireNonNull(tree, "tree");
        Objects.requireNonNull(event, "event");
        if (disabled || !open || event.type() != PointerEventType.DOWN) {
            return false;
        }
        @Nullable SemanticsNode node = find(tree, role());
        if (node != null && node.bounds().contains(event.x(), event.y())) {
            return false;
        }
        dismiss();
        return true;
    }

    /// Builds the overlay leaf.
    ///
    /// @param factory the node factory
    /// @param name the diagnostic name
    /// @return the leaf
    public LayoutNode create(LayoutFactory factory, String name) {
        Objects.requireNonNull(factory, "factory");
        Objects.requireNonNull(name, "name");
        float height = open ? 48.0f : 0.0f;
        LayoutNode created = factory.leaf(
                name,
                new Size(160.0f, height),
                List.of(new LayoutModifier.Padding(open ? 4.0f : 0.0f)),
                open && !disabled,
                role(),
                open ? label : "",
                open ? Set.of(SemanticsAction.ACTIVATE) : Set.of(),
                this::dismiss
        );
        created.setDisabled(disabled);
        this.node = created;
        return created;
    }

    /// Returns the semantics role for [#kind()].
    ///
    /// @return the role
    SemanticsRole role() {
        return switch (kind) {
            case OVERLAY -> SemanticsRole.POPUP;
            case MENU -> SemanticsRole.MENU;
            case DIALOG -> SemanticsRole.DIALOG;
            case TOOLTIP -> SemanticsRole.TOOLTIP;
        };
    }

    /// Finds the first node with `role`.
    ///
    /// @param tree the placed tree
    /// @param role the role
    /// @return the node, or `null` when absent
    static @Nullable SemanticsNode find(LayoutTree tree, SemanticsRole role) {
        for (SemanticsNode node : tree.semantics().nodes()) {
            if (node.role() == role) {
                return node;
            }
        }
        return null;
    }
}

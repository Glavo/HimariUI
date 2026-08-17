package org.glavo.himari.controls;

import org.glavo.himari.layout.Alignment;
import org.glavo.himari.layout.LayoutFactory;
import org.glavo.himari.layout.LayoutModifier;
import org.glavo.himari.layout.LayoutNode;
import org.glavo.himari.layout.LayoutTree;
import org.glavo.himari.layout.Size;
import org.glavo.himari.layout.bootstrap.BootstrapLabel;
import org.glavo.himari.layout.input.KeyEvent;
import org.glavo.himari.layout.input.PointerEvent;
import org.glavo.himari.layout.semantics.SemanticsRole;
import org.jetbrains.annotations.NotNullByDefault;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/// Creates a modal in-window dialog with Escape, outside-pointer, and button dismissal.
@NotNullByDefault
public final class Dialog {
    /// Accessible title.
    private final String title;

    /// Shared overlay used for Escape and outside-pointer dismissal.
    private final Popup popup;

    /// Explicit dismiss control.
    private final Button close;

    /// Creates a closed dialog.
    ///
    /// @param title the accessible title
    public Dialog(String title) {
        this.title = Objects.requireNonNull(title, "title");
        this.popup = new Popup(title, PopupKind.DIALOG);
        this.close = new Button("Close", this::dismiss);
    }

    /// Returns whether the dialog is open.
    ///
    /// @return whether it is open
    public boolean isOpen() {
        return popup.isOpen();
    }

    /// Returns the dismiss button.
    ///
    /// @return the button
    public Button closeButton() {
        return close;
    }

    /// Shows the dialog.
    public void show() {
        popup.show();
    }

    /// Returns whether the dialog is disabled.
    ///
    /// @return whether the dialog is disabled
    public boolean disabled() {
        return popup.disabled();
    }

    /// Sets the disabled state on the overlay and the close button.
    ///
    /// @param disabled the state
    public void setDisabled(boolean disabled) {
        popup.setDisabled(disabled);
        close.setDisabled(disabled);
    }

    /// Dismisses the dialog.
    public void dismiss() {
        popup.dismiss();
    }

    /// Dismisses the dialog on `Escape`.
    ///
    /// @param event the key event
    /// @return whether the dialog consumed the event
    public boolean handleKey(KeyEvent event) {
        return popup.handleKey(event);
    }

    /// Dismisses the dialog when a pointer-down lands outside it.
    ///
    /// @param tree the placed tree
    /// @param event the pointer event
    /// @return whether the dialog consumed the event
    public boolean handlePointer(LayoutTree tree, PointerEvent event) {
        return popup.handlePointer(tree, event);
    }

    /// Builds the dialog column, or a zero-height placeholder when closed.
    ///
    /// @param factory the node factory
    /// @param name the diagnostic name
    /// @return the node
    public LayoutNode create(LayoutFactory factory, String name) {
        Objects.requireNonNull(factory, "factory");
        Objects.requireNonNull(name, "name");
        if (!popup.isOpen()) {
            LayoutNode closed = factory.leaf(
                    name,
                    new Size(200.0f, 0.0f),
                    List.of(),
                    false,
                    SemanticsRole.DIALOG,
                    "",
                    Set.of(),
                    null
            );
            closed.setDisabled(popup.disabled());
            return closed;
        }
        LayoutNode created = factory.column(
                name,
                Alignment.START,
                List.of(new LayoutModifier.Padding(8.0f)),
                SemanticsRole.DIALOG,
                title,
                BootstrapLabel.create(factory, name + "-title", title),
                close.create(factory, name + "-close")
        );
        created.setDisabled(popup.disabled());
        return created;
    }
}

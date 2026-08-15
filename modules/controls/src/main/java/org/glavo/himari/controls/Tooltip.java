package org.glavo.himari.controls;

import org.glavo.himari.layout.LayoutFactory;
import org.glavo.himari.layout.LayoutModifier;
import org.glavo.himari.layout.LayoutNode;
import org.glavo.himari.layout.LayoutTree;
import org.glavo.himari.layout.Size;
import org.glavo.himari.layout.input.KeyEvent;
import org.glavo.himari.layout.input.PointerEvent;
import org.glavo.himari.layout.semantics.SemanticsRole;
import org.jetbrains.annotations.NotNullByDefault;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/// Creates a non-activating in-window tooltip with Escape and outside-pointer dismissal.
@NotNullByDefault
public final class Tooltip {
    /// Visible text.
    private final String text;

    /// Shared overlay used for Escape and outside-pointer dismissal.
    private final Popup popup;

    /// Creates a closed tooltip.
    ///
    /// @param text the tooltip text
    public Tooltip(String text) {
        this.text = Objects.requireNonNull(text, "text");
        this.popup = new Popup(text, PopupKind.TOOLTIP);
    }

    /// Returns whether the tooltip is open.
    ///
    /// @return whether it is open
    public boolean isOpen() {
        return popup.isOpen();
    }

    /// Shows the tooltip.
    public void show() {
        popup.show();
    }

    /// Dismisses the tooltip.
    public void dismiss() {
        popup.dismiss();
    }

    /// Dismisses the tooltip on `Escape`.
    ///
    /// @param event the key event
    /// @return whether the tooltip consumed the event
    public boolean handleKey(KeyEvent event) {
        return popup.handleKey(event);
    }

    /// Dismisses the tooltip when a pointer-down lands outside it.
    ///
    /// @param tree the placed tree
    /// @param event the pointer event
    /// @return whether the tooltip consumed the event
    public boolean handlePointer(LayoutTree tree, PointerEvent event) {
        return popup.handlePointer(tree, event);
    }

    /// Builds the tooltip leaf.
    ///
    /// @param factory the node factory
    /// @param name the diagnostic name
    /// @return the leaf
    public LayoutNode create(LayoutFactory factory, String name) {
        Objects.requireNonNull(factory, "factory");
        Objects.requireNonNull(name, "name");
        float height = popup.isOpen() ? 20.0f : 0.0f;
        return factory.leaf(
                name,
                new Size(Math.max(48.0f, text.length() * 8.0f), height),
                List.of(new LayoutModifier.Padding(popup.isOpen() ? 2.0f : 0.0f)),
                false,
                SemanticsRole.TOOLTIP,
                popup.isOpen() ? text : "",
                Set.of(),
                null
        );
    }
}

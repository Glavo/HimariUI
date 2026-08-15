package org.glavo.himari.controls;

import org.glavo.himari.layout.Alignment;
import org.glavo.himari.layout.LayoutFactory;
import org.glavo.himari.layout.LayoutModifier;
import org.glavo.himari.layout.LayoutNode;
import org.glavo.himari.layout.LayoutTree;
import org.glavo.himari.layout.Size;
import org.glavo.himari.layout.input.KeyEvent;
import org.glavo.himari.layout.input.PointerEvent;
import org.glavo.himari.layout.semantics.SemanticsAction;
import org.glavo.himari.layout.semantics.SemanticsRole;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/// Creates an in-window menu with item activation and overlay dismissal.
@NotNullByDefault
public final class Menu {
    /// Accessible name.
    private final String label;

    /// Shared overlay used for Escape and outside-pointer dismissal.
    private final Popup popup;

    /// Activatable items.
    private final List<Button> items;

    /// Creates a closed menu.
    ///
    /// @param label the accessible name
    /// @param items the menu items
    public Menu(String label, List<Button> items) {
        this.label = Objects.requireNonNull(label, "label");
        this.popup = new Popup(label, PopupKind.MENU);
        this.items = List.copyOf(items);
        if (this.items.isEmpty()) {
            throw new IllegalArgumentException("Menu must contain at least one item");
        }
        for (Button item : this.items) {
            Objects.requireNonNull(item, "item");
        }
    }

    /// Returns whether the menu is open.
    ///
    /// @return whether it is open
    public boolean isOpen() {
        return popup.isOpen();
    }

    /// Returns the items in document order.
    ///
    /// @return the items
    public @Unmodifiable List<Button> items() {
        return items;
    }

    /// Shows the menu.
    public void show() {
        popup.show();
    }

    /// Dismisses the menu.
    public void dismiss() {
        popup.dismiss();
    }

    /// Dismisses the menu on `Escape`.
    ///
    /// @param event the key event
    /// @return whether the menu consumed the event
    public boolean handleKey(KeyEvent event) {
        return popup.handleKey(event);
    }

    /// Dismisses the menu when a pointer-down lands outside it.
    ///
    /// @param tree the placed tree
    /// @param event the pointer event
    /// @return whether the menu consumed the event
    public boolean handlePointer(LayoutTree tree, PointerEvent event) {
        return popup.handlePointer(tree, event);
    }

    /// Builds the menu column, or a zero-height placeholder when closed.
    ///
    /// @param factory the node factory
    /// @param name the diagnostic name
    /// @return the node
    public LayoutNode create(LayoutFactory factory, String name) {
        Objects.requireNonNull(factory, "factory");
        Objects.requireNonNull(name, "name");
        if (!popup.isOpen()) {
            return factory.leaf(
                    name,
                    new Size(160.0f, 0.0f),
                    List.of(),
                    false,
                    SemanticsRole.MENU,
                    "",
                    Set.of(),
                    null
            );
        }
        ArrayList<LayoutNode> children = new ArrayList<>();
        for (int index = 0; index < items.size(); index++) {
            Button item = items.get(index);
            children.add(factory.leaf(
                    name + "-item-" + index,
                    new Size(160.0f, 24.0f),
                    List.of(new LayoutModifier.Padding(4.0f)),
                    true,
                    SemanticsRole.MENU_ITEM,
                    item.label(),
                    Set.of(SemanticsAction.ACTIVATE),
                    () -> {
                        item.press();
                        dismiss();
                    }
            ));
        }
        return factory.column(
                name,
                Alignment.START,
                List.of(new LayoutModifier.Padding(4.0f)),
                SemanticsRole.MENU,
                label,
                children.toArray(LayoutNode[]::new)
        );
    }
}

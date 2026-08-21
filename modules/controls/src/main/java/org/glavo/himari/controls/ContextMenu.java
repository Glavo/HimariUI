package org.glavo.himari.controls;

import org.glavo.himari.layout.Alignment;
import org.glavo.himari.layout.LayoutFactory;
import org.glavo.himari.layout.LayoutModifier;
import org.glavo.himari.layout.LayoutNode;
import org.glavo.himari.layout.LayoutTree;
import org.glavo.himari.layout.Size;
import org.glavo.himari.layout.input.KeyEvent;
import org.glavo.himari.layout.input.PointerEvent;
import org.glavo.himari.layout.input.PointerEventType;
import org.glavo.himari.layout.semantics.SemanticsAction;
import org.glavo.himari.layout.semantics.SemanticsRole;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/// Creates an unstyled context-menu target that opens on secondary pointer press.
@NotNullByDefault
public final class ContextMenu {
    /// Visible closed-target size.
    private static final Size TARGET_SIZE = new Size(160.0f, 24.0f);

    /// Accessible name of the target.
    private final String label;

    /// Shared overlay used for Escape and outside-pointer dismissal.
    private final Popup popup;

    /// Activatable items shown while open.
    private final List<Button> items;

    /// Creates a closed context menu.
    ///
    /// @param label the target name
    /// @param items the menu items
    public ContextMenu(String label, List<Button> items) {
        this.label = Objects.requireNonNull(label, "label");
        this.popup = new Popup(label, PopupKind.CONTEXT_MENU);
        this.items = List.copyOf(items);
        if (this.items.isEmpty()) {
            throw new IllegalArgumentException("ContextMenu must contain at least one item");
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

    /// Opens the menu.
    public void show() {
        popup.show();
    }

    /// Returns whether the control is disabled.
    ///
    /// @return whether the control is disabled
    public boolean disabled() {
        return popup.disabled();
    }

    /// Sets the disabled state on the overlay and every item.
    ///
    /// @param disabled the state
    public void setDisabled(boolean disabled) {
        popup.setDisabled(disabled);
        for (Button item : items) {
            item.setDisabled(disabled);
        }
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

    /// Dismisses an open menu when a primary pointer-down lands outside it.
    ///
    /// @param tree the placed tree
    /// @param event the pointer event
    /// @return whether the menu consumed the event
    public boolean handlePointer(LayoutTree tree, PointerEvent event) {
        return popup.handlePointer(tree, event);
    }

    /// Builds the always-visible target, plus item rows while open.
    ///
    /// A secondary pointer press on the target opens the menu. Primary activation
    /// does not.
    ///
    /// @param factory the node factory
    /// @param name the diagnostic name
    /// @return the node
    public LayoutNode create(LayoutFactory factory, String name) {
        Objects.requireNonNull(factory, "factory");
        Objects.requireNonNull(name, "name");
        LayoutNode target = factory.leaf(
                name,
                TARGET_SIZE,
                List.of(new LayoutModifier.Padding(0.0f)),
                true,
                SemanticsRole.CONTEXT_MENU,
                label,
                Set.of(),
                null
        );
        target.setDisabled(popup.disabled());
        target.addPointerListener(event -> {
            if (popup.disabled() || event.type() != PointerEventType.SECONDARY_DOWN) {
                return false;
            }
            show();
            return true;
        });
        if (!popup.isOpen()) {
            return target;
        }
        ArrayList<LayoutNode> children = new ArrayList<>();
        children.add(target);
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
        LayoutNode created = factory.column(
                name + "-open",
                Alignment.START,
                List.of(new LayoutModifier.Padding(4.0f)),
                SemanticsRole.CONTEXT_MENU,
                label,
                children.toArray(LayoutNode[]::new)
        );
        created.setDisabled(popup.disabled());
        return created;
    }
}

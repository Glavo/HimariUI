package org.glavo.himari.controls;

import org.glavo.himari.layout.Constraints;
import org.glavo.himari.layout.LayoutTree;
import org.glavo.himari.layout.input.KeyEvent;
import org.glavo.himari.layout.input.KeyEventType;
import org.glavo.himari.layout.input.LogicalKey;
import org.glavo.himari.layout.input.PointerEvent;
import org.glavo.himari.layout.input.PointerEventType;
import org.glavo.himari.layout.semantics.SemanticsNode;
import org.glavo.himari.layout.semantics.SemanticsRole;
import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies the unstyled context menu through the shipped gallery control.
@NotNullByDefault
final class ContextMenuTest {
    /// Opens on secondary press, ignores primary press, and dismisses on Escape.
    @Test
    void opensOnSecondaryPressThroughShippedLeaf() {
        LayoutTree tree = new LayoutTree();
        ControlGallery gallery = new ControlGallery();
        assertFalse(gallery.contextMenu().isOpen());
        tree.setRoot(gallery.create(tree));
        tree.measure(Constraints.loose(400.0f, 3000.0f));
        tree.place();
        SemanticsNode target = first(tree, SemanticsRole.CONTEXT_MENU);
        assertEquals("Context", target.label());
        assertTrue(target.bounds().height() > 0.0f);
        click(tree, target, PointerEventType.DOWN, PointerEventType.UP);
        assertFalse(gallery.contextMenu().isOpen());
        click(tree, target, PointerEventType.SECONDARY_DOWN, PointerEventType.SECONDARY_UP);
        assertTrue(gallery.contextMenu().isOpen());
        rebuild(tree, gallery);
        SemanticsNode item = first(tree, SemanticsRole.MENU_ITEM);
        assertEquals("Rename", item.label());
        assertTrue(gallery.dispatchKey(tree, new KeyEvent(KeyEventType.DOWN, LogicalKey.ESCAPE)));
        assertFalse(gallery.contextMenu().isOpen());
        gallery.contextMenu().setDisabled(true);
        rebuild(tree, gallery);
        click(tree, first(tree, SemanticsRole.CONTEXT_MENU),
                PointerEventType.SECONDARY_DOWN, PointerEventType.SECONDARY_UP);
        assertFalse(gallery.contextMenu().isOpen());
        assertTrue(gallery.contextMenu().disabled());
    }

    /// Dispatches a press/release pair on `node`.
    private static void click(
            LayoutTree tree,
            SemanticsNode node,
            PointerEventType down,
            PointerEventType up
    ) {
        float x = node.bounds().x() + 1.0f;
        float y = node.bounds().y() + 1.0f;
        tree.dispatch(new PointerEvent(down, x, y));
        tree.dispatch(new PointerEvent(up, x, y));
    }

    /// Rebuilds and places the gallery tree.
    private static void rebuild(LayoutTree tree, ControlGallery gallery) {
        tree.setRoot(gallery.create(tree));
        tree.measure(Constraints.loose(400.0f, 3000.0f));
        tree.place();
    }

    /// Returns the first node with the role.
    private static SemanticsNode first(LayoutTree tree, SemanticsRole role) {
        for (SemanticsNode node : tree.semantics().nodes()) {
            if (node.role() == role) {
                return node;
            }
        }
        throw new AssertionError("Missing " + role);
    }
}

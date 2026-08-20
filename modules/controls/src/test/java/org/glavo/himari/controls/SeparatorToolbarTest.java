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
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies unstyled separator and toolbar controls through the shipped gallery.
@NotNullByDefault
final class SeparatorToolbarTest {
    /// Places a non-focusable separator leaf.
    @Test
    void separatorOccupiesSpaceWithoutFocus() {
        LayoutTree tree = new LayoutTree();
        ControlGallery gallery = new ControlGallery();
        tree.setRoot(gallery.create(tree));
        tree.measure(Constraints.loose(400.0f, 1800.0f));
        tree.place();
        SemanticsNode separator = first(tree, SemanticsRole.SEPARATOR);
        assertEquals("separator", separator.label());
        assertEquals(160.0f, gallery.separator().size().width());
        assertEquals(1.0f, gallery.separator().size().height());
        assertTrue(separator.bounds().height() > 0.0f);
    }

    /// Selects and activates a toolbar command through pointer and arrow keys.
    @Test
    void toolbarSelectsAndActivatesThroughShippedLeaf() {
        LayoutTree tree = new LayoutTree();
        ControlGallery gallery = new ControlGallery();
        assertEquals("Cut", gallery.toolbar().value());
        assertEquals(-1, gallery.toolbar().lastActivated());
        tree.setRoot(gallery.create(tree));
        tree.measure(Constraints.loose(400.0f, 1800.0f));
        tree.place();
        SemanticsNode toolbar = first(tree, SemanticsRole.TOOLBAR);
        assertEquals("Cut", toolbar.label());
        click(tree, toolbar);
        assertEquals(0, gallery.toolbar().lastActivated());
        assertTrue(tree.dispatch(new KeyEvent(KeyEventType.DOWN, LogicalKey.ARROW_RIGHT)));
        assertEquals("Copy", gallery.toolbar().value());
        assertTrue(tree.dispatch(new KeyEvent(KeyEventType.DOWN, LogicalKey.ENTER)));
        assertEquals(1, gallery.toolbar().lastActivated());
        gallery.toolbar().setDisabled(true);
        gallery.toolbar().select(2);
        gallery.toolbar().activate();
        assertEquals("Copy", gallery.toolbar().value());
        assertEquals(1, gallery.toolbar().lastActivated());
        assertTrue(gallery.toolbar().disabled());
    }

    /// Dispatches a pointer press on `node`.
    private static void click(LayoutTree tree, SemanticsNode node) {
        float x = node.bounds().x() + 1.0f;
        float y = node.bounds().y() + 1.0f;
        tree.dispatch(new PointerEvent(PointerEventType.DOWN, x, y));
        tree.dispatch(new PointerEvent(PointerEventType.UP, x, y));
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

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

/// Verifies empty-state, carousel, and drawer controls through the shipped gallery.
@NotNullByDefault
final class EmptyCarouselDrawerTest {
    /// Publishes a no-content placeholder through the gallery leaf.
    @Test
    void emptyPublishesThroughShippedLeaf() {
        LayoutTree tree = new LayoutTree();
        ControlGallery gallery = new ControlGallery();
        assertEquals("No items", gallery.empty().description());
        tree.setRoot(gallery.create(tree));
        tree.measure(Constraints.loose(400.0f, 3000.0f));
        tree.place();
        SemanticsNode empty = first(tree, SemanticsRole.EMPTY);
        assertEquals("No items", empty.label());
        assertEquals("empty", empty.itemStatus());
        assertTrue(empty.bounds().height() > 0.0f);
        gallery.empty().setDisabled(true);
        assertTrue(gallery.empty().disabled());
        assertTrue(first(tree, SemanticsRole.EMPTY).disabled());
    }

    /// Advances labeled slides without wrapping through pointer and keyboard.
    @Test
    void carouselClampsThroughShippedLeaf() {
        LayoutTree tree = new LayoutTree();
        ControlGallery gallery = new ControlGallery();
        assertEquals("One", gallery.carousel().value());
        tree.setRoot(gallery.create(tree));
        tree.measure(Constraints.loose(400.0f, 3000.0f));
        tree.place();
        SemanticsNode carousel = first(tree, SemanticsRole.CAROUSEL);
        assertEquals("One", carousel.label());
        assertEquals(0.0, carousel.rangeMinimum());
        assertEquals(2.0, carousel.rangeMaximum());
        click(tree, carousel);
        assertEquals("Two", gallery.carousel().value());
        assertTrue(tree.dispatch(new KeyEvent(KeyEventType.DOWN, LogicalKey.ARROW_RIGHT)));
        assertEquals("Three", gallery.carousel().value());
        gallery.carousel().next();
        assertEquals("Three", gallery.carousel().value());
        assertTrue(tree.dispatch(new KeyEvent(KeyEventType.DOWN, LogicalKey.ARROW_LEFT)));
        assertEquals("Two", gallery.carousel().value());
        gallery.carousel().setDisabled(true);
        gallery.carousel().next();
        assertEquals("Two", gallery.carousel().value());
        assertTrue(gallery.carousel().disabled());
    }

    /// Toggles a non-modal drawer through pointer activation.
    @Test
    void drawerTogglesThroughShippedLeaf() {
        LayoutTree tree = new LayoutTree();
        ControlGallery gallery = new ControlGallery();
        assertFalse(gallery.drawer().isOpen());
        tree.setRoot(gallery.create(tree));
        tree.measure(Constraints.loose(400.0f, 3000.0f));
        tree.place();
        SemanticsNode drawer = first(tree, SemanticsRole.DRAWER);
        assertEquals("Sidebar", drawer.label());
        assertEquals("closed", drawer.itemStatus());
        click(tree, drawer);
        assertTrue(gallery.drawer().isOpen());
        assertEquals("open", first(tree, SemanticsRole.DRAWER).itemStatus());
        click(tree, first(tree, SemanticsRole.DRAWER));
        assertFalse(gallery.drawer().isOpen());
        gallery.drawer().setDisabled(true);
        gallery.drawer().open();
        assertFalse(gallery.drawer().isOpen());
        assertTrue(gallery.drawer().disabled());
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

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

/// Verifies the unstyled rating control through the shipped gallery leaf.
@NotNullByDefault
final class RatingTest {
    /// Increments, clamps, and disables the gallery rating through pointer and keyboard.
    @Test
    void ratesThroughShippedLeaf() {
        LayoutTree tree = new LayoutTree();
        ControlGallery gallery = new ControlGallery();
        assertEquals(3, gallery.rating().value());
        assertEquals("3 of 5", gallery.rating().label());
        tree.setRoot(gallery.create(tree));
        tree.measure(Constraints.loose(400.0f, 3000.0f));
        tree.place();
        SemanticsNode rating = first(tree, SemanticsRole.RATING);
        assertEquals("3 of 5", rating.label());
        assertEquals(3.0, rating.rangeValue());
        assertEquals(0.0, rating.rangeMinimum());
        assertEquals(5.0, rating.rangeMaximum());
        click(tree, rating);
        assertEquals(4, gallery.rating().value());
        assertEquals("4 of 5", first(tree, SemanticsRole.RATING).label());
        assertTrue(tree.dispatch(new KeyEvent(KeyEventType.DOWN, LogicalKey.ARROW_RIGHT)));
        assertEquals(5, gallery.rating().value());
        gallery.rating().increment();
        assertEquals(5, gallery.rating().value());
        assertTrue(tree.dispatch(new KeyEvent(KeyEventType.DOWN, LogicalKey.ARROW_LEFT)));
        assertEquals(4, gallery.rating().value());
        gallery.rating().setValue(0);
        gallery.rating().decrement();
        assertEquals(0, gallery.rating().value());
        gallery.rating().setDisabled(true);
        gallery.rating().increment();
        assertEquals(0, gallery.rating().value());
        assertTrue(gallery.rating().disabled());
        assertTrue(first(tree, SemanticsRole.RATING).disabled());
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

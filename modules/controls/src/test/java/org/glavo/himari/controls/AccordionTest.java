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

/// Verifies the unstyled accordion through the shipped gallery control.
@NotNullByDefault
final class AccordionTest {
    /// Advances the expanded section through pointer activation and arrow keys.
    @Test
    void expandsThroughShippedLeaf() {
        LayoutTree tree = new LayoutTree();
        ControlGallery gallery = new ControlGallery();
        assertEquals("Alpha", gallery.accordion().value());
        tree.setRoot(gallery.create(tree));
        tree.measure(Constraints.loose(400.0f, 2200.0f));
        tree.place();
        SemanticsNode accordion = first(tree, SemanticsRole.ACCORDION);
        assertEquals("Alpha", accordion.label());
        assertEquals("expanded", accordion.itemStatus());
        click(tree, accordion);
        assertEquals("Beta", gallery.accordion().value());
        assertTrue(tree.dispatch(new KeyEvent(KeyEventType.DOWN, LogicalKey.ARROW_RIGHT)));
        assertEquals("Gamma", gallery.accordion().value());
        gallery.accordion().setDisabled(true);
        gallery.accordion().expand(0);
        assertEquals("Gamma", gallery.accordion().value());
        assertTrue(gallery.accordion().disabled());
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

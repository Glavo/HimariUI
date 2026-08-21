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

/// Verifies unstyled chip and card controls through the shipped gallery.
@NotNullByDefault
final class ChipCardTest {
    /// Toggles a filter chip through pointer activation and Space.
    @Test
    void chipTogglesThroughShippedLeaf() {
        LayoutTree tree = new LayoutTree();
        ControlGallery gallery = new ControlGallery();
        assertFalse(gallery.chip().selected());
        tree.setRoot(gallery.create(tree));
        tree.measure(Constraints.loose(400.0f, 3000.0f));
        tree.place();
        SemanticsNode chip = first(tree, SemanticsRole.CHIP);
        assertEquals("Filter", chip.label());
        assertEquals("unselected", chip.itemStatus());
        click(tree, chip);
        assertTrue(gallery.chip().selected());
        assertEquals(Boolean.TRUE, first(tree, SemanticsRole.CHIP).selected());
        assertEquals("selected", first(tree, SemanticsRole.CHIP).itemStatus());
        assertTrue(tree.dispatch(new KeyEvent(KeyEventType.DOWN, LogicalKey.SPACE)));
        assertFalse(gallery.chip().selected());
        gallery.chip().setDisabled(true);
        gallery.chip().toggle();
        assertFalse(gallery.chip().selected());
        assertTrue(gallery.chip().disabled());
    }

    /// Places a titled card that occupies space without focus.
    @Test
    void cardOccupiesSpaceThroughShippedLeaf() {
        LayoutTree tree = new LayoutTree();
        ControlGallery gallery = new ControlGallery();
        assertEquals("Summary", gallery.card().title());
        tree.setRoot(gallery.create(tree));
        tree.measure(Constraints.loose(400.0f, 3000.0f));
        tree.place();
        SemanticsNode card = first(tree, SemanticsRole.CARD);
        assertEquals("Summary", card.label());
        assertTrue(card.bounds().height() > 0.0f);
        gallery.card().setDisabled(true);
        assertTrue(gallery.card().disabled());
        assertTrue(first(tree, SemanticsRole.CARD).disabled());
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

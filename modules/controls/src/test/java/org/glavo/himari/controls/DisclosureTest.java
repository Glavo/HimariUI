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

/// Verifies the unstyled disclosure through the shipped gallery control.
@NotNullByDefault
final class DisclosureTest {
    /// Expands and collapses through pointer activation and the space key.
    @Test
    void togglesThroughShippedLeaf() {
        LayoutTree tree = new LayoutTree();
        ControlGallery gallery = new ControlGallery();
        assertFalse(gallery.disclosure().isExpanded());
        tree.setRoot(gallery.create(tree));
        tree.measure(Constraints.loose(400.0f, 1600.0f));
        tree.place();
        SemanticsNode disclosure = first(tree, SemanticsRole.DISCLOSURE);
        assertEquals("More", disclosure.label());
        assertEquals("collapsed", disclosure.itemStatus());
        click(tree, disclosure);
        assertTrue(gallery.disclosure().isExpanded());
        assertEquals("expanded", first(tree, SemanticsRole.DISCLOSURE).itemStatus());
        assertTrue(tree.dispatch(new KeyEvent(KeyEventType.DOWN, LogicalKey.SPACE)));
        assertFalse(gallery.disclosure().isExpanded());
        gallery.disclosure().setDisabled(true);
        gallery.disclosure().toggle();
        assertFalse(gallery.disclosure().isExpanded());
        assertTrue(gallery.disclosure().disabled());
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

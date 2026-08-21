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

/// Verifies Scrollbar through the shipped gallery path.
@NotNullByDefault
final class ScrollbarTest {
    /// Steps the gallery scrollbar through keyboard increment and decrement.
    @Test
    void stepsScrollbarFromKeyboard() {
        LayoutTree tree = new LayoutTree();
        ControlGallery gallery = new ControlGallery();
        tree.setRoot(gallery.create(tree));
        tree.measure(Constraints.loose(400.0f, 3000.0f));
        tree.place();
        SemanticsNode bar = first(tree, SemanticsRole.SCROLLBAR);
        tree.dispatch(new PointerEvent(
                PointerEventType.DOWN,
                bar.bounds().x() + 1.0f,
                bar.bounds().y() + 1.0f
        ));
        assertEquals(20.0, bar.rangeValue());
        assertEquals(20.0f, gallery.scrollbar().value());
        tree.dispatch(new KeyEvent(KeyEventType.DOWN, LogicalKey.ARROW_RIGHT));
        assertEquals(30.0f, gallery.scrollbar().value());
        assertEquals(30.0, first(tree, SemanticsRole.SCROLLBAR).rangeValue());
        tree.dispatch(new KeyEvent(KeyEventType.DOWN, LogicalKey.ARROW_LEFT));
        assertEquals(20.0f, gallery.scrollbar().value());
        gallery.scrollbar().setDisabled(true);
        tree.dispatch(new KeyEvent(KeyEventType.DOWN, LogicalKey.ARROW_RIGHT));
        assertEquals(20.0f, gallery.scrollbar().value());
        assertTrue(first(tree, SemanticsRole.SCROLLBAR).disabled());
        gallery.scrollbar().setDisabled(false);
        gallery.progress().setDisabled(true);
        assertTrue(gallery.progress().disabled());
        assertTrue(first(tree, SemanticsRole.PROGRESS).disabled());
        gallery.progress().setDisabled(false);
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

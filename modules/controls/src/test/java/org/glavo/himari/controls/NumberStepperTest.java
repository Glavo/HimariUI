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

/// Verifies the unstyled integer stepper through the shipped gallery control.
@NotNullByDefault
final class NumberStepperTest {
    /// Advances and clamps the stepper through pointer activation and arrow keys.
    @Test
    void incrementsThroughShippedLeaf() {
        LayoutTree tree = new LayoutTree();
        ControlGallery gallery = new ControlGallery();
        assertEquals(3, gallery.stepper().value());
        tree.setRoot(gallery.create(tree));
        tree.measure(Constraints.loose(400.0f, 3000.0f));
        tree.place();
        SemanticsNode stepper = first(tree, SemanticsRole.STEPPER);
        assertEquals("3", stepper.label());
        assertEquals(3.0, stepper.rangeValue());
        assertEquals(0.0, stepper.rangeMinimum());
        assertEquals(10.0, stepper.rangeMaximum());
        click(tree, stepper);
        assertEquals(4, gallery.stepper().value());
        assertEquals("4", first(tree, SemanticsRole.STEPPER).label());
        assertTrue(tree.dispatch(new KeyEvent(KeyEventType.DOWN, LogicalKey.ARROW_RIGHT)));
        assertEquals(5, gallery.stepper().value());
        assertTrue(tree.dispatch(new KeyEvent(KeyEventType.DOWN, LogicalKey.ARROW_LEFT)));
        assertEquals(4, gallery.stepper().value());
        gallery.stepper().setValue(10);
        gallery.stepper().increment();
        assertEquals(10, gallery.stepper().value());
        gallery.stepper().setValue(0);
        gallery.stepper().decrement();
        assertEquals(0, gallery.stepper().value());
        gallery.stepper().setDisabled(true);
        gallery.stepper().increment();
        assertEquals(0, gallery.stepper().value());
        assertTrue(gallery.stepper().disabled());
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

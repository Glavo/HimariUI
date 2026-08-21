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

/// Verifies the unstyled RGB color picker through the shipped gallery control.
@NotNullByDefault
final class ColorPickerTest {
    /// Advances the red channel through pointer activation and arrow keys.
    @Test
    void incrementsRedThroughShippedLeaf() {
        LayoutTree tree = new LayoutTree();
        ControlGallery gallery = new ControlGallery();
        assertEquals("#336699", gallery.colorPicker().value());
        tree.setRoot(gallery.create(tree));
        tree.measure(Constraints.loose(400.0f, 3000.0f));
        tree.place();
        SemanticsNode color = first(tree, SemanticsRole.COLOR_PICKER);
        assertEquals("#336699", color.label());
        click(tree, color);
        assertEquals("#346699", gallery.colorPicker().value());
        assertEquals("#346699", first(tree, SemanticsRole.COLOR_PICKER).label());
        assertTrue(tree.dispatch(new KeyEvent(KeyEventType.DOWN, LogicalKey.ARROW_RIGHT)));
        assertEquals("#356699", gallery.colorPicker().value());
        assertTrue(tree.dispatch(new KeyEvent(KeyEventType.DOWN, LogicalKey.ARROW_LEFT)));
        assertEquals("#346699", gallery.colorPicker().value());
        gallery.colorPicker().setColor(255, 0, 0);
        gallery.colorPicker().increment();
        assertEquals("#000000", gallery.colorPicker().value());
        gallery.colorPicker().decrement();
        assertEquals("#FF0000", gallery.colorPicker().value());
        gallery.colorPicker().setDisabled(true);
        gallery.colorPicker().increment();
        assertEquals("#FF0000", gallery.colorPicker().value());
        assertTrue(gallery.colorPicker().disabled());
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

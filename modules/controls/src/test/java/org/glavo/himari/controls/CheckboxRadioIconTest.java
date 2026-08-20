package org.glavo.himari.controls;

import org.glavo.himari.layout.Constraints;
import org.glavo.himari.layout.LayoutTree;
import org.glavo.himari.layout.input.PointerEvent;
import org.glavo.himari.layout.input.PointerEventType;
import org.glavo.himari.layout.semantics.SemanticsNode;
import org.glavo.himari.layout.semantics.SemanticsRole;
import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies Checkbox, Radio, and IconButton through the shipped gallery path.
@NotNullByDefault
final class CheckboxRadioIconTest {
    /// Activates a checkbox through pointer input.
    @Test
    void checkboxTogglesSelectedState() {
        LayoutTree tree = new LayoutTree();
        ControlGallery gallery = new ControlGallery();
        tree.setRoot(gallery.create(tree));
        tree.measure(Constraints.loose(400.0f, 800.0f));
        tree.place();
        assertFalse(gallery.checkbox().isChecked());
        click(tree, first(tree, SemanticsRole.CHECKBOX));
        assertTrue(gallery.checkbox().isChecked());
        assertEquals(Boolean.TRUE, first(tree, SemanticsRole.CHECKBOX).selected());
    }

    /// Selects the second radio option through the shipped group.
    @Test
    void radioSelectsExclusively() {
        LayoutTree tree = new LayoutTree();
        ControlGallery gallery = new ControlGallery();
        assertEquals(0, gallery.radio().selected());
        gallery.radio().select(1);
        assertEquals(1, gallery.radio().selected());
        tree.setRoot(gallery.create(tree));
        tree.measure(Constraints.loose(400.0f, 800.0f));
        tree.place();
        assertTrue(tree.semantics().nodes().stream().anyMatch(node ->
                node.role() == SemanticsRole.RADIO && node.label().equals("B") && Boolean.TRUE.equals(node.selected())));
        gallery.radio().setDisabled(true);
        gallery.radio().select(0);
        assertEquals(1, gallery.radio().selected());
        assertTrue(gallery.radio().disabled());
        assertTrue(tree.semantics().nodes().stream().anyMatch(node ->
                node.role() == SemanticsRole.RADIO && node.disabled()));
        gallery.radio().setDisabled(false);
    }

    /// Selects a combo-box option through the shipped control.
    @Test
    void comboBoxSelectsAndExpands() {
        LayoutTree tree = new LayoutTree();
        ControlGallery gallery = new ControlGallery();
        assertEquals("Red", gallery.combo().value());
        assertFalse(gallery.combo().isOpen());
        gallery.combo().select(1);
        assertEquals("Green", gallery.combo().value());
        tree.setRoot(gallery.create(tree));
        tree.measure(Constraints.loose(400.0f, 800.0f));
        tree.place();
        SemanticsNode combo = first(tree, SemanticsRole.COMBO_BOX);
        assertEquals("Green", combo.label());
        assertEquals("collapsed", combo.itemStatus());
        click(tree, combo);
        assertTrue(gallery.combo().isOpen());
        gallery.combo().selectNext();
        assertEquals("Blue", gallery.combo().value());
        gallery.combo().setDisabled(true);
        gallery.combo().select(0);
        assertEquals("Blue", gallery.combo().value());
        assertTrue(gallery.combo().disabled());
        gallery.combo().setDisabled(false);
    }

    /// Activates the gallery icon button through pointer input.
    @Test
    void iconButtonActivates() {
        LayoutTree tree = new LayoutTree();
        ControlGallery gallery = new ControlGallery();
        tree.setRoot(gallery.create(tree));
        tree.measure(Constraints.loose(400.0f, 800.0f));
        tree.place();
        SemanticsNode icon = tree.semantics().nodes().stream()
                .filter(node -> node.role() == SemanticsRole.BUTTON && node.label().equals("plus"))
                .findFirst()
                .orElseThrow();
        click(tree, icon);
        assertEquals(1, gallery.iconButton().activations());
        assertEquals("plus", gallery.iconButton().icon());
        gallery.iconButton().setDisabled(true);
        click(tree, icon);
        assertEquals(1, gallery.iconButton().activations());
        assertTrue(gallery.iconButton().disabled());
        gallery.iconButton().setDisabled(false);
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

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

/// Verifies unstyled button, toggle, slider, scroll, lazy-list, and text-field interactions.
@NotNullByDefault
final class ControlGalleryTest {
    /// Activates the gallery button and toggle through pointer and keyboard.
    @Test
    void activatesButtonAndToggle() {
        LayoutTree tree = new LayoutTree();
        ControlGallery gallery = new ControlGallery();
        tree.setRoot(gallery.create(tree));
        tree.measure(Constraints.loose(400.0f, 400.0f));
        tree.place();
        SemanticsNode button = first(tree, SemanticsRole.BUTTON);
        click(tree, button);
        assertEquals(1, gallery.button().activations());
        assertEquals(1, gallery.externalClicks());
        SemanticsNode toggle = first(tree, SemanticsRole.TOGGLE);
        assertEquals(Boolean.FALSE, toggle.selected());
        click(tree, toggle);
        assertTrue(gallery.toggle().isOn());
        assertEquals(Boolean.TRUE, first(tree, SemanticsRole.TOGGLE).selected());
        tree.dispatch(new KeyEvent(KeyEventType.DOWN, LogicalKey.SPACE));
        assertFalse(gallery.toggle().isOn());
    }

    /// Steps the slider through keyboard increment and decrement.
    @Test
    void stepsSliderFromKeyboard() {
        LayoutTree tree = new LayoutTree();
        ControlGallery gallery = new ControlGallery();
        tree.setRoot(gallery.create(tree));
        tree.measure(Constraints.loose(400.0f, 400.0f));
        tree.place();
        SemanticsNode slider = first(tree, SemanticsRole.SLIDER);
        tree.dispatch(new PointerEvent(
                PointerEventType.DOWN,
                slider.bounds().x() + 1.0f,
                slider.bounds().y() + 1.0f
        ));
        assertEquals(3.0, slider.rangeValue());
        tree.dispatch(new KeyEvent(KeyEventType.DOWN, LogicalKey.ARROW_RIGHT));
        assertEquals(4.0f, gallery.slider().value());
        assertEquals(4.0, first(tree, SemanticsRole.SLIDER).rangeValue());
        tree.dispatch(new KeyEvent(KeyEventType.DOWN, LogicalKey.ARROW_LEFT));
        assertEquals(3.0f, gallery.slider().value());
    }

    /// Scrolls the viewport and advances the lazy-list window.
    @Test
    void scrollsAndPagesLazyList() {
        LayoutTree tree = new LayoutTree();
        ControlGallery gallery = new ControlGallery();
        tree.setRoot(gallery.create(tree));
        tree.measure(Constraints.loose(400.0f, 400.0f));
        tree.place();
        gallery.scroll().scrollForward();
        assertEquals(16.0f, gallery.scroll().offset());
        SemanticsNode list = first(tree, SemanticsRole.LIST);
        tree.dispatch(new PointerEvent(
                PointerEventType.DOWN,
                list.bounds().x() + 1.0f,
                list.bounds().y() + 1.0f
        ));
        tree.dispatch(new KeyEvent(KeyEventType.DOWN, LogicalKey.ARROW_DOWN));
        assertEquals(1, gallery.list().firstVisible());
    }

    /// Commits IME composition into the text field.
    @Test
    void commitsTextFieldComposition() {
        TextField field = new TextField();
        field.updateComposition("ni");
        assertEquals("ni", field.composition());
        assertEquals("ni", field.commitComposition());
        assertEquals("ni", field.text());
        field.updateComposition("hao");
        field.cancelComposition();
        assertEquals("ni", field.text());
        assertEquals(null, field.composition());
        assertTrue(field.undo());
        assertEquals("", field.text());
        assertTrue(field.redo());
        assertEquals("ni", field.text());
    }

    /// Shows and dismisses the in-window popup and reads theme tokens.
    @Test
    void popupAndThemeAreExercisable() {
        ControlGallery gallery = new ControlGallery();
        assertFalse(gallery.popup().isOpen());
        gallery.popup().show();
        assertTrue(gallery.popup().isOpen());
        gallery.popup().dismiss();
        assertFalse(gallery.popup().isOpen());
        assertFalse(gallery.theme().highContrast());
        assertTrue(ThemeTokens.highContrastTheme().highContrast());
    }

    /// Returns the first node with the role.
    ///
    /// @param tree the tree
    /// @param role the role
    /// @return the node
    private static SemanticsNode first(LayoutTree tree, SemanticsRole role) {
        for (SemanticsNode node : tree.semantics().nodes()) {
            if (node.role() == role) {
                return node;
            }
        }
        throw new AssertionError("Missing " + role);
    }

    /// Clicks the center of one node.
    ///
    /// @param tree the tree
    /// @param node the target
    private static void click(LayoutTree tree, SemanticsNode node) {
        float x = node.bounds().x() + 1.0f;
        float y = node.bounds().y() + 1.0f;
        tree.dispatch(new PointerEvent(PointerEventType.DOWN, x, y));
        tree.dispatch(new PointerEvent(PointerEventType.UP, x, y));
    }
}

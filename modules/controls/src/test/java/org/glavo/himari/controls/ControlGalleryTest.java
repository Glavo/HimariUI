package org.glavo.himari.controls;

import org.glavo.himari.layout.Constraints;
import org.glavo.himari.layout.LayoutTree;
import org.glavo.himari.layout.input.KeyEvent;
import org.glavo.himari.layout.input.KeyEventType;
import org.glavo.himari.layout.input.LogicalKey;
import org.glavo.himari.layout.input.PointerEvent;
import org.glavo.himari.layout.input.PointerEventType;
import org.glavo.himari.layout.input.gesture.GestureArena;
import org.glavo.himari.layout.semantics.SemanticsLiveRegion;
import org.glavo.himari.layout.semantics.SemanticsNode;
import org.glavo.himari.layout.semantics.SemanticsRole;
import org.glavo.himari.layout.semantics.TextDirection;
import org.glavo.himari.runtime.animation.AnimationMotionDisposition;
import org.glavo.himari.runtime.animation.AnimationTransaction;
import org.glavo.himari.runtime.animation.MotionImportance;
import org.glavo.himari.runtime.animation.MotionPolicy;
import org.glavo.himari.runtime.animation.SnapMotionSpec;
import org.glavo.himari.runtime.animation.TweenSpec;
import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
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
        tree.measure(Constraints.loose(400.0f, 800.0f));
        tree.place();
        SemanticsNode button = first(tree, SemanticsRole.BUTTON);
        click(tree, button);
        assertEquals(1, gallery.button().activations());
        assertEquals(1, gallery.externalClicks());
        assertTrue(tree.dispatch(new KeyEvent(KeyEventType.DOWN, LogicalKey.ENTER)));
        assertEquals(2, gallery.button().activations());
        assertEquals(2, gallery.externalClicks());
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
        tree.measure(Constraints.loose(400.0f, 800.0f));
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
        tree.measure(Constraints.loose(400.0f, 800.0f));
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
        assertEquals(TextDirection.LTR, gallery.theme().textDirection());
        assertFalse(gallery.theme().reducedMotion());
        assertTrue(ThemeTokens.highContrastTheme().highContrast());
    }

    /// Commits IME composition into the multiline text area.
    @Test
    void commitsTextAreaComposition() {
        TextArea area = new TextArea();
        area.updateComposition("hello");
        assertEquals("hello", area.commitComposition());
        area.updateComposition("world");
        assertEquals("world", area.commitComposition());
        assertEquals("hello\nworld", area.text());
        assertTrue(area.undo());
        assertEquals("hello", area.text());
    }

    /// Publishes a polite live-region announcement through the gallery status.
    @Test
    void announcesPoliteLiveRegion() {
        LayoutTree tree = new LayoutTree();
        ControlGallery gallery = new ControlGallery();
        gallery.status().announce("Saved");
        rebuild(tree, gallery);
        SemanticsNode status = first(tree, SemanticsRole.STATUS);
        assertEquals("Saved", status.label());
        assertEquals(SemanticsLiveRegion.POLITE, status.liveRegion());
        assertEquals(SemanticsLiveRegion.OFF, first(tree, SemanticsRole.BUTTON).liveRegion());
    }

    /// Packs gallery children to the end when the theme is RTL.
    @Test
    void rtlThemePacksChildrenToEnd() {
        LayoutTree ltr = new LayoutTree();
        ControlGallery gallery = new ControlGallery();
        rebuild(ltr, gallery);
        float startX = first(ltr, SemanticsRole.BUTTON).bounds().x();
        gallery.setTheme(ThemeTokens.standard().withTextDirection(TextDirection.RTL));
        LayoutTree rtl = new LayoutTree();
        rebuild(rtl, gallery);
        assertEquals(TextDirection.RTL, gallery.theme().textDirection());
        assertTrue(first(rtl, SemanticsRole.BUTTON).bounds().x() > startX);
    }

    /// Scrolls the viewport when a drag wins the gallery gesture arena.
    @Test
    void dragGestureScrollsViewport() {
        LayoutTree tree = new LayoutTree();
        ControlGallery gallery = new ControlGallery();
        rebuild(tree, gallery);
        float before = gallery.scroll().offset();
        gallery.dispatchPointer(tree, new PointerEvent(PointerEventType.DOWN, 20.0f, 40.0f), 0L);
        gallery.dispatchPointer(tree, new PointerEvent(PointerEventType.MOVE, 20.0f, 20.0f), 16_000_000L);
        assertTrue(gallery.gestures().dragAccepted());
        assertEquals(before + 20.0f, gallery.scroll().offset());
    }

    /// Announces a long press through the live-region status.
    @Test
    void longPressAnnouncesStatus() {
        LayoutTree tree = new LayoutTree();
        ControlGallery gallery = new ControlGallery();
        rebuild(tree, gallery);
        gallery.dispatchPointer(tree, new PointerEvent(PointerEventType.DOWN, 12.0f, 12.0f), 0L);
        gallery.gestures().tick(GestureArena.LONG_PRESS_NANOS);
        gallery.dispatchPointer(
                tree,
                new PointerEvent(PointerEventType.UP, 12.0f, 12.0f),
                GestureArena.LONG_PRESS_NANOS
        );
        assertTrue(gallery.gestures().longPressAccepted());
        assertEquals("Long press", gallery.status().message());
    }

    /// Resolves reduced-motion theme tokens into effective animation specifications.
    @Test
    void reducedMotionThemeTransformsMotionSpecs() {
        ControlGallery gallery = new ControlGallery();
        TweenSpec requested = TweenSpec.linear(1_000_000_000L);
        AnimationTransaction standard = gallery.resolveMotion(1L, requested, MotionImportance.NONESSENTIAL);
        assertSame(requested, standard.effectiveMotion());
        gallery.setTheme(ThemeTokens.standard().withReducedMotion(true));
        AnimationTransaction snapped = gallery.resolveMotion(2L, requested, MotionImportance.NONESSENTIAL);
        assertSame(SnapMotionSpec.INSTANCE, snapped.effectiveMotion());
        assertSame(AnimationMotionDisposition.DISABLED, snapped.motionDisposition());
        AnimationTransaction essential = gallery.resolveMotion(3L, requested, MotionImportance.ESSENTIAL);
        assertTrue(essential.effectiveMotion() instanceof TweenSpec);
        assertEquals(
                MotionPolicy.REDUCED_TWEEN_MAX_NANOS,
                ((TweenSpec) essential.effectiveMotion()).durationNanos()
        );
        assertSame(AnimationMotionDisposition.REDUCED, essential.motionDisposition());
    }

    /// Dismisses an open menu with Escape after the overlay is placed.
    @Test
    void escapeDismissesOpenMenu() {
        LayoutTree tree = new LayoutTree();
        ControlGallery gallery = new ControlGallery();
        gallery.menu().show();
        rebuild(tree, gallery);
        assertTrue(first(tree, SemanticsRole.MENU).bounds().height() > 0.0f);
        assertTrue(gallery.dispatchKey(tree, new KeyEvent(KeyEventType.DOWN, LogicalKey.ESCAPE)));
        assertFalse(gallery.menu().isOpen());
    }

    /// Dismisses an open dialog when a pointer-down lands outside it.
    @Test
    void outsidePointerDismissesOpenDialog() {
        LayoutTree tree = new LayoutTree();
        ControlGallery gallery = new ControlGallery();
        gallery.dialog().show();
        rebuild(tree, gallery);
        SemanticsNode dialog = first(tree, SemanticsRole.DIALOG);
        assertTrue(dialog.bounds().height() > 0.0f);
        assertTrue(gallery.dispatchPointer(tree, new PointerEvent(
                PointerEventType.DOWN,
                dialog.bounds().x() - 4.0f,
                dialog.bounds().y() - 4.0f
        )));
        assertFalse(gallery.dialog().isOpen());
    }

    /// Activates a menu item and dismisses the menu.
    @Test
    void menuItemActivationDismissesMenu() {
        LayoutTree tree = new LayoutTree();
        ControlGallery gallery = new ControlGallery();
        gallery.menu().show();
        rebuild(tree, gallery);
        SemanticsNode item = first(tree, SemanticsRole.MENU_ITEM);
        assertTrue(gallery.dispatchPointer(tree, new PointerEvent(
                PointerEventType.DOWN,
                item.bounds().x() + 1.0f,
                item.bounds().y() + 1.0f
        )));
        assertTrue(gallery.dispatchPointer(tree, new PointerEvent(
                PointerEventType.UP,
                item.bounds().x() + 1.0f,
                item.bounds().y() + 1.0f
        )));
        assertEquals(1, gallery.menu().items().getFirst().activations());
        assertFalse(gallery.menu().isOpen());
    }

    /// Rebuilds and places the gallery tree.
    ///
    /// @param tree the tree
    /// @param gallery the gallery
    private static void rebuild(LayoutTree tree, ControlGallery gallery) {
        tree.setRoot(gallery.create(tree));
        tree.measure(Constraints.loose(400.0f, 800.0f));
        tree.place();
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

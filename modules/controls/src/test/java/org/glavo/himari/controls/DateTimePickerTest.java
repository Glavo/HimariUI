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

/// Verifies unstyled date and time pickers through the shipped gallery controls.
@NotNullByDefault
final class DateTimePickerTest {
    /// Advances a Gregorian date through pointer activation and arrow keys.
    @Test
    void datePickerIncrementsThroughShippedLeaf() {
        LayoutTree tree = new LayoutTree();
        ControlGallery gallery = new ControlGallery();
        assertEquals("2026-08-20", gallery.datePicker().value());
        tree.setRoot(gallery.create(tree));
        tree.measure(Constraints.loose(400.0f, 3000.0f));
        tree.place();
        SemanticsNode date = first(tree, SemanticsRole.DATE_PICKER);
        assertEquals("2026-08-20", date.label());
        click(tree, date);
        assertEquals("2026-08-21", gallery.datePicker().value());
        assertEquals("2026-08-21", first(tree, SemanticsRole.DATE_PICKER).label());
        assertTrue(tree.dispatch(new KeyEvent(KeyEventType.DOWN, LogicalKey.ARROW_RIGHT)));
        assertEquals("2026-08-22", gallery.datePicker().value());
        assertTrue(tree.dispatch(new KeyEvent(KeyEventType.DOWN, LogicalKey.ARROW_LEFT)));
        assertEquals("2026-08-21", gallery.datePicker().value());
        gallery.datePicker().setDate(2024, 2, 28);
        gallery.datePicker().increment();
        assertEquals("2024-02-29", gallery.datePicker().value());
        gallery.datePicker().setDate(2023, 2, 28);
        gallery.datePicker().increment();
        assertEquals("2023-03-01", gallery.datePicker().value());
        gallery.datePicker().setDisabled(true);
        gallery.datePicker().increment();
        assertEquals("2023-03-01", gallery.datePicker().value());
        assertTrue(gallery.datePicker().disabled());
    }

    /// Wraps a 24-hour clock through pointer activation and arrow keys.
    @Test
    void timePickerWrapsThroughShippedLeaf() {
        LayoutTree tree = new LayoutTree();
        ControlGallery gallery = new ControlGallery();
        assertEquals("13:45", gallery.timePicker().value());
        tree.setRoot(gallery.create(tree));
        tree.measure(Constraints.loose(400.0f, 3000.0f));
        tree.place();
        SemanticsNode time = first(tree, SemanticsRole.TIME_PICKER);
        assertEquals("13:45", time.label());
        click(tree, time);
        assertEquals("13:46", gallery.timePicker().value());
        assertTrue(tree.dispatch(new KeyEvent(KeyEventType.DOWN, LogicalKey.ARROW_RIGHT)));
        assertEquals("13:47", gallery.timePicker().value());
        gallery.timePicker().setTime(23, 59);
        gallery.timePicker().increment();
        assertEquals("00:00", gallery.timePicker().value());
        gallery.timePicker().decrement();
        assertEquals("23:59", gallery.timePicker().value());
        gallery.timePicker().setDisabled(true);
        gallery.timePicker().increment();
        assertEquals("23:59", gallery.timePicker().value());
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

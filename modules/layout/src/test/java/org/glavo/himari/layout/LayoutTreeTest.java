package org.glavo.himari.layout;

import org.glavo.himari.layout.bootstrap.BootstrapCounterPane;
import org.glavo.himari.layout.input.KeyEvent;
import org.glavo.himari.layout.input.KeyEventType;
import org.glavo.himari.layout.input.LogicalKey;
import org.glavo.himari.layout.input.PointerEvent;
import org.glavo.himari.layout.input.PointerEventType;
import org.glavo.himari.layout.semantics.SemanticsAction;
import org.glavo.himari.layout.semantics.SemanticsNode;
import org.glavo.himari.layout.semantics.SemanticsRole;
import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies measurement, placement, hit testing, focus, and bootstrap activation.
@NotNullByDefault
final class LayoutTreeTest {
    /// Verifies single-measure and placement-only invalidation for a column.
    @Test
    void measuresOnceAndPlacesChildren() {
        LayoutTree tree = new LayoutTree();
        LayoutFactory factory = new LayoutFactory(tree);
        LayoutNode leaf = factory.leaf(
                "box",
                new Size(10.0f, 20.0f),
                java.util.List.of(),
                false,
                SemanticsRole.NONE,
                "box",
                java.util.Set.of(),
                null
        );
        tree.setRoot(factory.column("root", Alignment.START, java.util.List.of(), leaf));
        Size size = tree.measure(Constraints.loose(100.0f, 100.0f));
        assertEquals(10.0f, size.width());
        assertEquals(20.0f, size.height());
        tree.place();
        assertEquals(0.0f, leaf.origin().x());
        assertEquals(0.0f, leaf.origin().y());
        assertThrows(IllegalStateException.class, () -> tree.root().measure(Constraints.loose(100.0f, 100.0f)));
    }

    /// Verifies pointer and keyboard activation of the bootstrap increment button.
    @Test
    void pointerAndKeyboardActivateBootstrapCounter() {
        LayoutTree tree = new LayoutTree();
        AtomicInteger count = new AtomicInteger();
        tree.setRoot(BootstrapCounterPane.create(tree, count));
        tree.measure(Constraints.loose(200.0f, 200.0f));
        tree.place();
        SemanticsNode button = tree.semantics().nodeWith(SemanticsAction.ACTIVATE);
        assertEquals(SemanticsRole.BUTTON, button.role());
        assertEquals("Increment", button.label());
        assertNotNull(tree.focus().focusedId());
        assertTrue(tree.dispatch(new PointerEvent(
                PointerEventType.DOWN,
                button.bounds().x() + 1.0f,
                button.bounds().y() + 1.0f
        )));
        assertTrue(tree.dispatch(new PointerEvent(
                PointerEventType.UP,
                button.bounds().x() + 1.0f,
                button.bounds().y() + 1.0f
        )));
        assertEquals(1, count.get());
        assertTrue(tree.dispatch(new KeyEvent(KeyEventType.DOWN, LogicalKey.ENTER)));
        assertEquals(2, count.get());
        assertEquals(button.bounds(), tree.semantics().nodeWith(SemanticsAction.ACTIVATE).bounds());
    }
}

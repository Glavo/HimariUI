package org.glavo.himari.layout;

import org.glavo.himari.layout.bootstrap.BootstrapCounterPane;
import org.glavo.himari.layout.input.KeyEvent;
import org.glavo.himari.layout.input.KeyEventType;
import org.glavo.himari.layout.input.LogicalKey;
import org.glavo.himari.layout.input.PointerEvent;
import org.glavo.himari.layout.input.PointerEventType;
import org.glavo.himari.layout.semantics.SemanticsAction;
import org.glavo.himari.layout.semantics.SemanticsLiveRegion;
import org.glavo.himari.layout.semantics.SemanticsNode;
import org.glavo.himari.layout.semantics.SemanticsRole;
import org.glavo.himari.layout.semantics.SemanticsTextRange;
import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    /// Moves document-order focus backward when Tab is dispatched with shift.
    @Test
    void shiftTabMovesFocusToPreviousNode() {
        LayoutTree tree = new LayoutTree();
        LayoutFactory factory = new LayoutFactory(tree);
        LayoutNode first = factory.leaf(
                "first",
                new Size(20.0f, 12.0f),
                java.util.List.of(),
                true,
                SemanticsRole.BUTTON,
                "One",
                java.util.Set.of(SemanticsAction.ACTIVATE),
                () -> { }
        );
        LayoutNode second = factory.leaf(
                "second",
                new Size(20.0f, 12.0f),
                java.util.List.of(),
                true,
                SemanticsRole.BUTTON,
                "Two",
                java.util.Set.of(SemanticsAction.ACTIVATE),
                () -> { }
        );
        tree.setRoot(factory.column("root", Alignment.START, java.util.List.of(), first, second));
        tree.measure(Constraints.loose(100.0f, 100.0f));
        tree.place();
        assertEquals(first.id(), tree.focus().focusedId());
        assertTrue(tree.dispatch(new KeyEvent(KeyEventType.DOWN, LogicalKey.TAB)));
        assertEquals(second.id(), tree.focus().focusedId());
        assertTrue(tree.dispatch(new KeyEvent(KeyEventType.DOWN, LogicalKey.TAB, true)));
        assertEquals(first.id(), tree.focus().focusedId());
        assertTrue(tree.focus().focusVisible());
        assertTrue(tree.dispatch(new PointerEvent(
                PointerEventType.DOWN,
                second.bounds().x() + 1.0f,
                second.bounds().y() + 1.0f
        )));
        assertEquals(second.id(), tree.focus().focusedId());
        assertFalse(tree.focus().focusVisible());
    }

    /// Restricts Tab traversal to a trapped subtree until the trap is cleared.
    @Test
    void trapKeepsTabInsideSubtree() {
        LayoutTree tree = new LayoutTree();
        LayoutFactory factory = new LayoutFactory(tree);
        LayoutNode outside = factory.leaf(
                "outside",
                new Size(20.0f, 12.0f),
                java.util.List.of(),
                true,
                SemanticsRole.BUTTON,
                "Outside",
                java.util.Set.of(SemanticsAction.ACTIVATE),
                () -> { }
        );
        LayoutNode innerFirst = factory.leaf(
                "inner-first",
                new Size(20.0f, 12.0f),
                java.util.List.of(),
                true,
                SemanticsRole.BUTTON,
                "InnerOne",
                java.util.Set.of(SemanticsAction.ACTIVATE),
                () -> { }
        );
        LayoutNode innerSecond = factory.leaf(
                "inner-second",
                new Size(20.0f, 12.0f),
                java.util.List.of(),
                true,
                SemanticsRole.BUTTON,
                "InnerTwo",
                java.util.Set.of(SemanticsAction.ACTIVATE),
                () -> { }
        );
        LayoutNode dialog = factory.column(
                "dialog",
                Alignment.START,
                java.util.List.of(),
                innerFirst,
                innerSecond
        );
        tree.setRoot(factory.column("root", Alignment.START, java.util.List.of(), outside, dialog));
        tree.measure(Constraints.loose(100.0f, 100.0f));
        tree.place();
        assertEquals(outside.id(), tree.focus().focusedId());
        tree.focus().trap(dialog);
        assertEquals(dialog.id(), tree.focus().trapId());
        assertEquals(innerFirst.id(), tree.focus().focusedId());
        assertFalse(tree.focus().request(outside));
        assertTrue(tree.dispatch(new KeyEvent(KeyEventType.DOWN, LogicalKey.TAB)));
        assertEquals(innerSecond.id(), tree.focus().focusedId());
        assertTrue(tree.dispatch(new KeyEvent(KeyEventType.DOWN, LogicalKey.TAB)));
        assertEquals(innerFirst.id(), tree.focus().focusedId());
        tree.focus().clearTrap();
        assertEquals(null, tree.focus().trapId());
        assertTrue(tree.focus().request(outside));
        assertEquals(outside.id(), tree.focus().focusedId());
    }

    /// Transfers keyboard focus from one window tree to another and restores it.
    @Test
    void transferMovesFocusBetweenWindowTrees() {
        LayoutTree firstTree = new LayoutTree();
        LayoutFactory firstFactory = new LayoutFactory(firstTree);
        LayoutNode first = firstFactory.leaf(
                "first",
                new Size(20.0f, 12.0f),
                java.util.List.of(),
                true,
                SemanticsRole.BUTTON,
                "One",
                java.util.Set.of(SemanticsAction.ACTIVATE),
                () -> { }
        );
        firstTree.setRoot(firstFactory.column("root", Alignment.START, java.util.List.of(), first));
        firstTree.measure(Constraints.loose(100.0f, 100.0f));
        firstTree.place();
        firstTree.dispatch(new KeyEvent(KeyEventType.DOWN, LogicalKey.TAB));
        LayoutTree secondTree = new LayoutTree();
        LayoutFactory secondFactory = new LayoutFactory(secondTree);
        LayoutNode second = secondFactory.leaf(
                "second",
                new Size(20.0f, 12.0f),
                java.util.List.of(),
                true,
                SemanticsRole.BUTTON,
                "Two",
                java.util.Set.of(SemanticsAction.ACTIVATE),
                () -> { }
        );
        secondTree.setRoot(secondFactory.column("root", Alignment.START, java.util.List.of(), second));
        secondTree.measure(Constraints.loose(100.0f, 100.0f));
        secondTree.place();
        assertTrue(firstTree.focus().transferTo(secondTree.focus()));
        assertFalse(firstTree.focus().focusVisible());
        assertEquals(first.id(), firstTree.focus().focusedId());
        assertTrue(secondTree.focus().focusVisible());
        assertEquals(second.id(), secondTree.focus().focusedId());
        assertTrue(secondTree.focus().transferTo(firstTree.focus()));
        assertTrue(firstTree.focus().focusVisible());
        assertEquals(first.id(), firstTree.focus().focusedId());
        assertFalse(secondTree.focus().focusVisible());
    }

    /// Publishes live-region politeness through the semantics snapshot.
    @Test
    void publishesLiveRegionOnSemanticsSnapshot() {
        LayoutTree tree = new LayoutTree();
        LayoutFactory factory = new LayoutFactory(tree);
        LayoutNode status = factory.leaf(
                "status",
                new Size(80.0f, 16.0f),
                java.util.List.of(),
                false,
                SemanticsRole.STATUS,
                "Ready",
                java.util.Set.of(),
                null
        );
        status.setLiveRegion(SemanticsLiveRegion.ASSERTIVE);
        AtomicInteger announced = new AtomicInteger();
        status.addLabelListener(announced::incrementAndGet);
        status.setLabel("Updated");
        assertEquals(1, announced.get());
        tree.setRoot(factory.column("root", Alignment.START, java.util.List.of(), status));
        tree.measure(Constraints.loose(100.0f, 100.0f));
        tree.place();
        SemanticsNode snapshot = tree.semantics().nodes().stream()
                .filter(node -> node.role() == SemanticsRole.STATUS)
                .findFirst()
                .orElseThrow();
        assertEquals(SemanticsLiveRegion.ASSERTIVE, snapshot.liveRegion());
        assertEquals("Updated", snapshot.label());
        assertEquals(SemanticsLiveRegion.OFF, tree.semantics().nodes().getFirst().liveRegion());
    }

    /// Publishes a UTF-16 text range through the semantics snapshot.
    @Test
    void publishesTextRangeOnSemanticsSnapshot() {
        LayoutTree tree = new LayoutTree();
        LayoutFactory factory = new LayoutFactory(tree);
        LayoutNode field = factory.leaf(
                "field",
                new Size(80.0f, 16.0f),
                java.util.List.of(),
                true,
                SemanticsRole.TEXT_FIELD,
                "hello",
                java.util.Set.of(),
                null
        );
        field.setTextRange(new SemanticsTextRange(1, 4, 4));
        tree.setRoot(factory.column("root", Alignment.START, java.util.List.of(), field));
        tree.measure(Constraints.loose(100.0f, 100.0f));
        tree.place();
        SemanticsNode snapshot = tree.semantics().nodes().stream()
                .filter(node -> node.role() == SemanticsRole.TEXT_FIELD)
                .findFirst()
                .orElseThrow();
        assertEquals(new SemanticsTextRange(1, 4, 4), snapshot.textRange());
        assertFalse(snapshot.disabled());
        assertFalse(snapshot.readOnly());
        field.setDisabled(true);
        field.setReadOnly(true);
        SemanticsNode gated = tree.semantics().nodes().stream()
                .filter(node -> node.role() == SemanticsRole.TEXT_FIELD)
                .findFirst()
                .orElseThrow();
        assertTrue(gated.disabled());
        assertTrue(gated.readOnly());
        field.setHint("Greeting");
        SemanticsNode hinted = tree.semantics().nodes().stream()
                .filter(node -> node.role() == SemanticsRole.TEXT_FIELD)
                .findFirst()
                .orElseThrow();
        assertEquals("Greeting", hinted.hint());
        assertTrue(hinted.focusable());
        assertFalse(hinted.password());
        field.setPassword(true);
        SemanticsNode secret = tree.semantics().nodes().stream()
                .filter(node -> node.role() == SemanticsRole.TEXT_FIELD)
                .findFirst()
                .orElseThrow();
        assertTrue(secret.password());
        field.setAccessKey("G");
        field.setAcceleratorKey("Ctrl+G");
        SemanticsNode keyed = tree.semantics().nodes().stream()
                .filter(node -> node.role() == SemanticsRole.TEXT_FIELD)
                .findFirst()
                .orElseThrow();
        assertEquals("G", keyed.accessKey());
        assertEquals("Ctrl+G", keyed.acceleratorKey());
        field.setRequired(true);
        field.setItemStatus("invalid");
        field.setItemType("edit");
        field.setLandmarkType(80002);
        field.setLocalizedLandmarkType("main");
        field.setAriaRole("textbox");
        field.setAriaProperties("required=true");
        field.setControllerFor("submit");
        field.setDescribedBy("hint");
        field.setFlowsTo("next");
        field.setLabeledBy("title");
        field.setFlowsFrom("prev");
        field.setOptimizeForVisualContent(true);
        field.setFillColor(0xFF1565C0);
        field.setOutlineColor(0xFFE0E0E0);
        field.setFillType(1);
        field.setVisualEffects(1);
        field.setOutlineThickness(2);
        field.setRotation(90);
        field.setPeripheral(true);
        field.setAnnotationType(60000);
        field.setAnnotationObjects("note");
        field.setLocale("en-US");
        SemanticsNode form = tree.semantics().nodes().stream()
                .filter(node -> node.role() == SemanticsRole.TEXT_FIELD)
                .findFirst()
                .orElseThrow();
        assertTrue(form.required());
        assertEquals("invalid", form.itemStatus());
        assertEquals("edit", form.itemType());
        assertEquals(80002, form.landmarkType());
        assertEquals("main", form.localizedLandmarkType());
        assertEquals("textbox", form.ariaRole());
        assertEquals("required=true", form.ariaProperties());
        assertEquals("submit", form.controllerFor());
        assertEquals("hint", form.describedBy());
        assertEquals("next", form.flowsTo());
        assertEquals("title", form.labeledBy());
        assertEquals("prev", form.flowsFrom());
        assertTrue(form.optimizeForVisualContent());
        assertEquals(0xFF1565C0, form.fillColor());
        assertEquals(0xFFE0E0E0, form.outlineColor());
        assertEquals(1, form.fillType());
        assertEquals(1, form.visualEffects());
        assertEquals(2, form.outlineThickness());
        assertEquals(90, form.rotation());
        assertTrue(form.peripheral());
        assertEquals(60000, form.annotationType());
        assertEquals("note", form.annotationObjects());
        assertEquals("en-US", form.locale());
        field.setLevel(2);
        field.setPositionInSet(1);
        field.setSizeOfSet(3);
        field.setDescription("Guest name");
        SemanticsNode set = tree.semantics().nodes().stream()
                .filter(node -> node.role() == SemanticsRole.TEXT_FIELD)
                .findFirst()
                .orElseThrow();
        assertEquals(2, set.level());
        assertEquals(1, set.positionInSet());
        assertEquals(3, set.sizeOfSet());
        assertEquals("Guest name", set.description());
        field.setError(true);
        SemanticsNode invalid = tree.semantics().nodes().stream()
                .filter(node -> node.role() == SemanticsRole.TEXT_FIELD)
                .findFirst()
                .orElseThrow();
        assertTrue(invalid.error());
        assertEquals("", tree.semantics().nodes().getFirst().hint());
        assertEquals(null, tree.semantics().nodes().getFirst().textRange());
        assertThrows(IllegalArgumentException.class, () -> new SemanticsTextRange(2, 1, 2));
    }
}

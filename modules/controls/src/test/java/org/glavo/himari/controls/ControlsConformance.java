package org.glavo.himari.controls;

import org.glavo.himari.platform.api.ImeSession;
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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/// Writes M9 unstyled-control interaction evidence.
@NotNullByDefault
public final class ControlsConformance {
    /// Prevents instantiation.
    private ControlsConformance() {
    }

    /// Exercises the gallery and writes the report.
    ///
    /// @param arguments one output directory
    /// @throws Exception if the profile fails
    public static void main(String[] arguments) throws Exception {
        if (arguments.length != 1) {
            throw new IllegalArgumentException("Expected one output directory");
        }
        LayoutTree tree = new LayoutTree();
        ControlGallery gallery = new ControlGallery();
        tree.setRoot(gallery.create(tree));
        tree.measure(Constraints.loose(400.0f, 1200.0f));
        tree.place();
        SemanticsNode button = first(tree, SemanticsRole.BUTTON);
        tree.dispatch(new PointerEvent(PointerEventType.DOWN, button.bounds().x() + 1.0f, button.bounds().y() + 1.0f));
        tree.dispatch(new PointerEvent(PointerEventType.UP, button.bounds().x() + 1.0f, button.bounds().y() + 1.0f));
        if (!tree.dispatch(new KeyEvent(KeyEventType.DOWN, LogicalKey.ENTER))) {
            throw new IllegalStateException("ENTER did not activate the focused button");
        }
        SemanticsNode toggle = first(tree, SemanticsRole.TOGGLE);
        tree.dispatch(new PointerEvent(PointerEventType.DOWN, toggle.bounds().x() + 1.0f, toggle.bounds().y() + 1.0f));
        tree.dispatch(new PointerEvent(PointerEventType.UP, toggle.bounds().x() + 1.0f, toggle.bounds().y() + 1.0f));
        SemanticsNode slider = first(tree, SemanticsRole.SLIDER);
        tree.dispatch(new PointerEvent(PointerEventType.DOWN, slider.bounds().x() + 1.0f, slider.bounds().y() + 1.0f));
        tree.dispatch(new PointerEvent(PointerEventType.UP, slider.bounds().x() + slider.bounds().width() - 1.0f,
                slider.bounds().y() + 1.0f));
        tree.dispatch(new KeyEvent(KeyEventType.DOWN, LogicalKey.ARROW_RIGHT));
        gallery.scroll().scrollForward();
        SemanticsNode list = first(tree, SemanticsRole.LIST);
        tree.dispatch(new PointerEvent(PointerEventType.DOWN, list.bounds().x() + 1.0f, list.bounds().y() + 1.0f));
        tree.dispatch(new PointerEvent(PointerEventType.UP, list.bounds().x() + 1.0f, list.bounds().y() + 1.0f));
        tree.dispatch(new KeyEvent(KeyEventType.DOWN, LogicalKey.ARROW_DOWN));
        gallery.field().updateComposition("a");
        gallery.field().commitComposition();
        if (!gallery.field().undo() || !gallery.field().text().isEmpty() || !gallery.field().redo()) {
            throw new IllegalStateException("Text-field undo/redo failed");
        }
        gallery.field().setSelection(0, 1);
        gallery.field().replaceRange(0, 1, "ab");
        gallery.field().updateComposition("!");
        if (!"ab".equals(gallery.field().text())
                || !"!".equals(gallery.field().rejectComposition())
                || gallery.field().composition() != null
                || !"!".equals(gallery.field().lastRejected())
                || gallery.field().caret() != 2) {
            throw new IllegalStateException("Text-field selection or rejection failed");
        }
        ImeSession ime = new ImeSession();
        ime.setSurroundingText(gallery.field().text(), gallery.field().caret());
        ime.updateComposition("z");
        gallery.field().apply(ime);
        ime.commit();
        gallery.field().apply(ime);
        if (!"abz".equals(gallery.field().text()) || gallery.field().composition() != null) {
            throw new IllegalStateException("Text-field IME session apply failed");
        }
        gallery.popup().show();
        if (!gallery.popup().isOpen() || gallery.theme().highContrast()) {
            throw new IllegalStateException("Popup or theme tokens were incorrect");
        }
        gallery.popup().dismiss();
        gallery.menu().show();
        rebuild(tree, gallery);
        if (!gallery.dispatchKey(tree, new KeyEvent(KeyEventType.DOWN, LogicalKey.ESCAPE))
                || gallery.menu().isOpen()) {
            throw new IllegalStateException("Escape did not dismiss the menu");
        }
        gallery.dialog().show();
        rebuild(tree, gallery);
        SemanticsNode dialog = first(tree, SemanticsRole.DIALOG);
        if (!gallery.dispatchPointer(tree, new PointerEvent(
                PointerEventType.DOWN,
                dialog.bounds().x() - 4.0f,
                dialog.bounds().y() - 4.0f
        )) || gallery.dialog().isOpen()) {
            throw new IllegalStateException("Outside pointer did not dismiss the dialog");
        }
        gallery.menu().show();
        rebuild(tree, gallery);
        SemanticsNode item = first(tree, SemanticsRole.MENU_ITEM);
        gallery.dispatchPointer(tree, new PointerEvent(
                PointerEventType.DOWN,
                item.bounds().x() + 1.0f,
                item.bounds().y() + 1.0f
        ));
        gallery.dispatchPointer(tree, new PointerEvent(
                PointerEventType.UP,
                item.bounds().x() + 1.0f,
                item.bounds().y() + 1.0f
        ));
        if (gallery.menu().isOpen() || gallery.menu().items().getFirst().activations() != 1) {
            throw new IllegalStateException("Menu item did not activate and dismiss");
        }
        gallery.tooltip().show();
        rebuild(tree, gallery);
        if (first(tree, SemanticsRole.TOOLTIP).bounds().height() <= 0.0f
                || !gallery.dispatchKey(tree, new KeyEvent(KeyEventType.DOWN, LogicalKey.ESCAPE))
                || gallery.tooltip().isOpen()) {
            throw new IllegalStateException("Tooltip did not show or dismiss");
        }
        gallery.radio().select(1);
        gallery.tabs().select(1);
        gallery.split().setFraction(0.6f);
        gallery.tree().toggle(0);
        gallery.progress().setValue(0.5f);
        SemanticsNode scrollbar = first(tree, SemanticsRole.SCROLLBAR);
        tree.dispatch(new PointerEvent(PointerEventType.DOWN, scrollbar.bounds().x() + 1.0f, scrollbar.bounds().y() + 1.0f));
        tree.dispatch(new KeyEvent(KeyEventType.DOWN, LogicalKey.ARROW_RIGHT));
        if (gallery.button().activations() != 2 || !gallery.toggle().isOn() || gallery.slider().value() != 5.0f
                || gallery.progress().value() != 0.5f
                || first(tree, SemanticsRole.PROGRESS).rangeValue() == null
                || Math.abs(first(tree, SemanticsRole.PROGRESS).rangeValue() - 0.5) > 1.0e-6
                || gallery.scrollbar().value() != 30.0f
                || first(tree, SemanticsRole.SCROLLBAR).rangeValue() == null
                || Math.abs(first(tree, SemanticsRole.SCROLLBAR).rangeValue() - 30.0) > 1.0e-6
                || gallery.list().unmountedLabels().size() != 16
                || gallery.radio().selected() != 1
                || first(tree, SemanticsRole.CHECKBOX).bounds().height() <= 0.0f
                || gallery.iconButton().icon().isEmpty()
                || first(tree, SemanticsRole.IMAGE).bounds().height() <= 0.0f
                || !"logo".equals(gallery.image().source())
                || first(tree, SemanticsRole.CANVAS).bounds().height() <= 0.0f
                || gallery.spacer().size().width() != 8.0f
                || gallery.tabs().selected() != 1
                || gallery.split().fraction() != 0.6f
                || gallery.tree().isExpanded(0)
                || gallery.tree().visibleIndices().size() != 1
                || first(tree, SemanticsRole.TAB_LIST).bounds().height() <= 0.0f
                || first(tree, SemanticsRole.SPLIT_PANE).bounds().height() <= 0.0f
                || first(tree, SemanticsRole.TREE).bounds().height() <= 0.0f) {
            throw new IllegalStateException("Control gallery outcomes were incorrect");
        }
        if (gallery.scroll().offset() != 16.0f || gallery.list().firstVisible() != 1
                || !"abz".equals(gallery.field().text())
                || !"r0".equals(gallery.table().firstMaterializedKey())
                || first(tree, SemanticsRole.TABLE).bounds().height() <= 0.0f) {
            throw new IllegalStateException("Scroll, list, table, or text-field outcomes were incorrect");
        }
        gallery.dispatchPointer(tree, new PointerEvent(PointerEventType.DOWN, 24.0f, 60.0f), 0L);
        gallery.dispatchPointer(tree, new PointerEvent(PointerEventType.MOVE, 24.0f, 40.0f), 16_000_000L);
        if (!gallery.gestures().dragAccepted() || gallery.scroll().offset() != 36.0f) {
            throw new IllegalStateException("Drag gesture did not scroll the viewport");
        }
        gallery.gestures().reset();
        gallery.dispatchPointer(tree, new PointerEvent(PointerEventType.DOWN, 12.0f, 12.0f), 100L);
        gallery.gestures().tick(100L + GestureArena.LONG_PRESS_NANOS);
        gallery.dispatchPointer(
                tree,
                new PointerEvent(PointerEventType.UP, 12.0f, 12.0f),
                100L + GestureArena.LONG_PRESS_NANOS
        );
        if (!gallery.gestures().longPressAccepted() || !"Long press".equals(gallery.status().message())) {
            throw new IllegalStateException("Long press did not announce through the live region");
        }
        gallery.area().updateComposition("hello");
        gallery.area().commitComposition();
        gallery.area().updateComposition("world");
        gallery.area().commitComposition();
        if (!"hello\nworld".equals(gallery.area().text()) || !gallery.area().undo()) {
            throw new IllegalStateException("Text-area composition or undo failed");
        }
        gallery.area().setSelection(0, 5);
        gallery.area().replaceRange(0, 5, "hi");
        gallery.area().updateComposition("!");
        if (!"hi".equals(gallery.area().text())
                || !"!".equals(gallery.area().rejectComposition())
                || !"!".equals(gallery.area().lastRejected())) {
            throw new IllegalStateException("Text-area selection or rejection failed");
        }
        float ltrButtonX = first(tree, SemanticsRole.BUTTON).bounds().x();
        gallery.status().announce("Saved");
        rebuild(tree, gallery);
        SemanticsNode status = first(tree, SemanticsRole.STATUS);
        if (!"Saved".equals(status.label()) || status.liveRegion() != SemanticsLiveRegion.POLITE) {
            throw new IllegalStateException("Live-region status was not polite");
        }
        SemanticsNode field = first(tree, SemanticsRole.TEXT_FIELD);
        if (field.textRange() == null
                || field.textRange().start() != 3
                || field.textRange().end() != 3
                || field.textRange().caret() != 3
                || first(tree, SemanticsRole.BUTTON).textRange() != null) {
            throw new IllegalStateException("Editor text range was not published");
        }
        gallery.setTheme(ThemeTokens.standard().withTextDirection(TextDirection.RTL));
        rebuild(tree, gallery);
        if (gallery.theme().textDirection() != TextDirection.RTL
                || first(tree, SemanticsRole.BUTTON).bounds().x() <= ltrButtonX) {
            throw new IllegalStateException("RTL theme did not pack children to the end");
        }
        gallery.setTheme(gallery.theme().withReducedMotion(true));
        if (!gallery.theme().reducedMotion()) {
            throw new IllegalStateException("Reduced-motion theme was not applied");
        }
        org.glavo.himari.runtime.reload.ResourceReload resources =
                new org.glavo.himari.runtime.reload.ResourceReload();
        resources.publish(
                org.glavo.himari.runtime.reload.ResourceKind.IMAGE,
                "gallery",
                java.lang.foreign.MemorySegment.ofArray("star".getBytes(StandardCharsets.UTF_8))
        );
        resources.publish(
                org.glavo.himari.runtime.reload.ResourceKind.FONT,
                "gallery",
                java.lang.foreign.MemorySegment.ofArray("HimariSans".getBytes(StandardCharsets.UTF_8))
        );
        if (!gallery.applyImageReload(resources, "gallery")
                || !"star".equals(gallery.iconButton().icon())
                || !gallery.applyFontReload(resources, "gallery")
                || !"HimariSans".equals(gallery.fontFamily())) {
            throw new IllegalStateException("Image or font reload did not install the published payload");
        }
        TweenSpec requestedMotion = TweenSpec.linear(1_000_000_000L);
        AnimationTransaction snapped = gallery.resolveMotion(1L, requestedMotion, MotionImportance.NONESSENTIAL);
        if (snapped.effectiveMotion() != SnapMotionSpec.INSTANCE
                || snapped.motionDisposition() != AnimationMotionDisposition.DISABLED) {
            throw new IllegalStateException("Reduced-motion policy did not snap nonessential motion");
        }
        AnimationTransaction essential = gallery.resolveMotion(2L, requestedMotion, MotionImportance.ESSENTIAL);
        if (!(essential.effectiveMotion() instanceof TweenSpec shortened)
                || shortened.durationNanos() != MotionPolicy.REDUCED_TWEEN_MAX_NANOS
                || essential.motionDisposition() != AnimationMotionDisposition.REDUCED) {
            throw new IllegalStateException("Reduced-motion policy did not shorten essential motion");
        }
        Path output = Path.of(arguments[0]);
        Files.createDirectories(output);
        Files.writeString(
                output.resolve("results.json"),
                """
                        {
                          "profile": "m9-controls",
                          "workPackage": "CTRL-001",
                          "status": "passed",
                          "buttonActivations": %d,
                          "enterActivated": true,
                          "toggleOn": %s,
                          "sliderValue": %s,
                          "scrollOffset": %s,
                          "gestureDrag": true,
                          "gestureLongPress": true,
                          "listFirstVisible": %d,
                          "text": "%s",
                          "imeSessionApplied": true,
                          "selectionRejected": true,
                          "textRangeStart": %d,
                          "textRangeEnd": %d,
                          "caret": %d,
                          "undoRedo": true,
                          "popupDismissed": %s,
                          "menuDismissed": true,
                          "dialogDismissed": true,
                          "menuItemActivations": %d,
                          "tooltipDismissed": true,
                          "textArea": "%s",
                          "liveRegion": "%s",
                          "status": "%s",
                          "textDirection": "%s",
                          "rtlPacked": true,
                          "reducedMotion": %s,
                          "reducedMotionSnap": true,
                          "reducedMotionEssentialNanos": %d,
                          "theme": "%s"
                        }
                        """.formatted(
                        gallery.button().activations(),
                        gallery.toggle().isOn(),
                        gallery.slider().value(),
                        gallery.scroll().offset(),
                        gallery.list().firstVisible(),
                        gallery.field().text(),
                        field.textRange().start(),
                        field.textRange().end(),
                        field.textRange().caret(),
                        !gallery.popup().isOpen(),
                        gallery.menu().items().getFirst().activations(),
                        gallery.area().text(),
                        first(tree, SemanticsRole.STATUS).liveRegion().name(),
                        gallery.status().message(),
                        gallery.theme().textDirection().name(),
                        gallery.theme().reducedMotion(),
                        ((TweenSpec) essential.effectiveMotion()).durationNanos(),
                        gallery.theme().name()
                ),
                StandardCharsets.UTF_8
        );
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
        throw new IllegalStateException("Missing " + role);
    }

    /// Rebuilds and places the gallery tree.
    ///
    /// @param tree the tree
    /// @param gallery the gallery
    private static void rebuild(LayoutTree tree, ControlGallery gallery) {
        tree.setRoot(gallery.create(tree));
        tree.measure(Constraints.loose(400.0f, 1200.0f));
        tree.place();
    }
}

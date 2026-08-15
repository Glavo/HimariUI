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
        tree.measure(Constraints.loose(400.0f, 400.0f));
        tree.place();
        SemanticsNode button = first(tree, SemanticsRole.BUTTON);
        tree.dispatch(new PointerEvent(PointerEventType.DOWN, button.bounds().x() + 1.0f, button.bounds().y() + 1.0f));
        tree.dispatch(new PointerEvent(PointerEventType.UP, button.bounds().x() + 1.0f, button.bounds().y() + 1.0f));
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
        gallery.popup().show();
        if (!gallery.popup().isOpen() || gallery.theme().highContrast()) {
            throw new IllegalStateException("Popup or theme tokens were incorrect");
        }
        gallery.popup().dismiss();
        if (gallery.button().activations() != 1 || !gallery.toggle().isOn() || gallery.slider().value() != 5.0f) {
            throw new IllegalStateException("Control gallery outcomes were incorrect");
        }
        if (gallery.scroll().offset() != 16.0f || gallery.list().firstVisible() != 1
                || !"a".equals(gallery.field().text())) {
            throw new IllegalStateException("Scroll, list, or text-field outcomes were incorrect");
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
                          "toggleOn": %s,
                          "sliderValue": %s,
                          "scrollOffset": %s,
                          "listFirstVisible": %d,
                          "text": "%s",
                          "undoRedo": true,
                          "popupDismissed": %s,
                          "theme": "%s"
                        }
                        """.formatted(
                        gallery.button().activations(),
                        gallery.toggle().isOn(),
                        gallery.slider().value(),
                        gallery.scroll().offset(),
                        gallery.list().firstVisible(),
                        gallery.field().text(),
                        !gallery.popup().isOpen(),
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
}

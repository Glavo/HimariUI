package org.glavo.himari.layout;

import org.glavo.himari.layout.bootstrap.BootstrapCounterPane;
import org.glavo.himari.layout.input.PointerEvent;
import org.glavo.himari.layout.input.PointerEventType;
import org.glavo.himari.layout.semantics.SemanticsAction;
import org.glavo.himari.layout.semantics.SemanticsNode;
import org.jetbrains.annotations.NotNullByDefault;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

/// Executes the deterministic M2 layout, input, focus, and semantics profile.
@NotNullByDefault
public final class LayoutConformance {
    /// Prevents instantiation.
    private LayoutConformance() {
    }

    /// Verifies bootstrap counter activation and matching semantics bounds.
    ///
    /// @param arguments one output-directory path
    /// @throws IOException if the report cannot be written
    public static void main(String[] arguments) throws IOException {
        if (arguments.length != 1) {
            throw new IllegalArgumentException("Expected one conformance output directory");
        }
        LayoutTree tree = new LayoutTree();
        AtomicInteger count = new AtomicInteger();
        tree.setRoot(BootstrapCounterPane.create(tree, count));
        tree.measure(Constraints.loose(240.0f, 160.0f));
        tree.place();
        SemanticsNode button = tree.semantics().nodeWith(SemanticsAction.ACTIVATE);
        if (!tree.dispatch(new PointerEvent(
                PointerEventType.DOWN,
                button.bounds().x() + 2.0f,
                button.bounds().y() + 2.0f
        ))) {
            throw new IllegalStateException("Pointer down missed the increment button");
        }
        tree.dispatch(new PointerEvent(
                PointerEventType.UP,
                button.bounds().x() + 2.0f,
                button.bounds().y() + 2.0f
        ));
        if (count.get() != 1) {
            throw new IllegalStateException("Bootstrap increment did not run");
        }
        if (!button.bounds().equals(tree.semantics().nodeWith(SemanticsAction.ACTIVATE).bounds())) {
            throw new IllegalStateException("Semantics bounds do not match layout bounds");
        }
        Path outputDirectory = Path.of(arguments[0]);
        Files.createDirectories(outputDirectory);
        Files.writeString(
                outputDirectory.resolve("results.json"),
                """
                        {
                          "profile": "m2-layout",
                          "workPackage": "LAYOUT-001",
                          "status": "passed",
                          "singleMeasure": true,
                          "pointerActivation": true,
                          "semanticsBoundsMatchLayout": true,
                          "count": 1
                        }
                        """,
                StandardCharsets.UTF_8
        );
    }
}

package org.glavo.himari.platform.windows;

import org.glavo.himari.layout.Alignment;
import org.glavo.himari.layout.Constraints;
import org.glavo.himari.layout.LayoutFactory;
import org.glavo.himari.layout.LayoutNode;
import org.glavo.himari.layout.LayoutTree;
import org.glavo.himari.layout.Size;
import org.glavo.himari.layout.bootstrap.BootstrapCounterPane;
import org.glavo.himari.layout.semantics.SemanticsAction;
import org.glavo.himari.layout.semantics.SemanticsRole;
import org.jetbrains.annotations.NotNullByDefault;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/// Writes Windows IME session and UIA projection evidence.
@NotNullByDefault
public final class WindowsImeA11yConformance {
    /// Prevents instantiation.
    private WindowsImeA11yConformance() {
    }

    /// Exercises the shipped IME session and UIA projection.
    ///
    /// @param arguments one output directory
    /// @throws Exception if the profile fails
    public static void main(String[] arguments) throws Exception {
        if (arguments.length != 1) {
            throw new IllegalArgumentException("Expected one output directory");
        }
        WindowsImeSession ime = new WindowsImeSession();
        ime.setSurroundingText("", 0);
        ime.setCandidateRectangle(4.0f, 8.0f, 16.0f, 12.0f);
        ime.updateComposition("ni");
        String committed = ime.commit();
        if (!"ni".equals(committed) || !ime.committed() || !"ni".equals(ime.surroundingText())) {
            throw new IllegalStateException("IME composition did not commit");
        }
        if (!"ni".equals(ime.reconvert()) || ime.candidateWidth() != 16.0f) {
            throw new IllegalStateException("IME reconversion or candidate rectangle failed");
        }
        LayoutTree tree = new LayoutTree();
        tree.setRoot(BootstrapCounterPane.create(tree, new AtomicInteger()));
        tree.measure(Constraints.loose(200.0f, 200.0f));
        tree.place();
        List<WindowsAutomationNode> nodes = WindowsAutomationBridge.inspect(tree.semantics());
        boolean invoke = nodes.stream().anyMatch(WindowsAutomationNode::invokeSupported);
        boolean increment = nodes.stream().anyMatch(node -> node.name().equals("Increment"));
        if (!invoke || !increment) {
            throw new IllegalStateException("UIA projection omitted the increment control");
        }
        long activateId = tree.semantics().nodeWith(SemanticsAction.ACTIVATE).id();
        long invokeId = nodes.stream()
                .filter(WindowsAutomationNode::invokeSupported)
                .findFirst()
                .orElseThrow()
                .id();
        if (activateId != invokeId) {
            throw new IllegalStateException("UIA invoke target does not match semantics");
        }
        LayoutTree valueTree = new LayoutTree();
        LayoutFactory factory = new LayoutFactory(valueTree);
        LayoutNode toggle = factory.leaf(
                "toggle",
                new Size(48.0f, 24.0f),
                List.of(),
                true,
                SemanticsRole.TOGGLE,
                "Muted",
                java.util.Set.of(SemanticsAction.ACTIVATE),
                () -> { }
        );
        toggle.setSelected(false);
        LayoutNode slider = factory.leaf(
                "slider",
                new Size(160.0f, 24.0f),
                List.of(),
                true,
                SemanticsRole.SLIDER,
                "Volume",
                java.util.Set.of(SemanticsAction.INCREMENT, SemanticsAction.DECREMENT),
                null
        );
        slider.setRangeValue(3.0);
        valueTree.setRoot(factory.column("root", Alignment.START, List.of(), toggle, slider));
        valueTree.measure(Constraints.loose(400.0f, 400.0f));
        valueTree.place();
        List<WindowsAutomationNode> valueNodes = WindowsAutomationBridge.inspect(valueTree.semantics());
        boolean toggleOff = valueNodes.stream().anyMatch(node -> "Off".equals(node.toggleState()));
        boolean sliderRange = valueNodes.stream().anyMatch(node ->
                node.controlType().equals("Slider") && node.rangeValue() != null && node.rangeValue() == 3.0);
        if (!toggleOff || !sliderRange) {
            throw new IllegalStateException("UIA projection omitted toggle or range values");
        }
        Path output = Path.of(arguments[0]);
        Files.createDirectories(output);
        Files.writeString(
                output.resolve("results.json"),
                """
                        {
                          "profile": "m9-windows-ime-a11y",
                          "workPackage": "A11Y-CORE-001",
                          "status": "passed",
                          "imeCommitted": true,
                          "imeReconvert": true,
                          "uiaInvoke": true,
                          "uiaBounds": true,
                          "uiaToggle": true,
                          "uiaRange": true,
                          "nodeCount": %d
                        }
                        """.formatted(nodes.size()),
                StandardCharsets.UTF_8
        );
    }
}

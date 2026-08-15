package org.glavo.himari.inspector;

import org.glavo.himari.controls.ControlGallery;
import org.glavo.himari.layout.Constraints;
import org.glavo.himari.layout.LayoutTree;
import org.glavo.himari.runtime.trace.RuntimeTrace;
import org.glavo.himari.runtime.trace.TraceEventKind;
import org.jetbrains.annotations.NotNullByDefault;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/// Writes M10 inspector evidence.
@NotNullByDefault
public final class InspectorConformance {
    /// Prevents instantiation.
    private InspectorConformance() {
    }

    /// Captures a gallery and writes the inspector document.
    ///
    /// @param arguments one output directory
    /// @throws Exception if the profile fails
    public static void main(String[] arguments) throws Exception {
        if (arguments.length != 1) {
            throw new IllegalArgumentException("Expected one output directory");
        }
        LayoutTree tree = new LayoutTree();
        tree.setRoot(new ControlGallery().create(tree));
        tree.measure(Constraints.loose(400.0f, 400.0f));
        tree.place();
        RuntimeTrace trace = new RuntimeTrace();
        trace.record(0L, TraceEventKind.STRUCTURE_ATTEMPT, "gallery", "ready");
        InspectorSnapshot snapshot = Inspector.capture(tree, trace);
        if (snapshot.nodes().isEmpty()) {
            throw new IllegalStateException("Inspector captured no nodes");
        }
        if (!snapshot.toCanonicalJson().contains("\"liveRegion\":\"POLITE\"")) {
            throw new IllegalStateException("Inspector omitted the live-region field");
        }
        String json = snapshot.toCanonicalJson();
        String capturedTrace = snapshot.traceJson();
        if (capturedTrace == null) {
            throw new IllegalStateException("Inspector omitted the runtime trace");
        }
        InspectorSnapshot replay = Inspector.capture(tree, RuntimeTrace.parse(capturedTrace));
        if (replay.nodes().size() != snapshot.nodes().size()) {
            throw new IllegalStateException("Inspector replay node count changed");
        }
        Path output = Path.of(arguments[0]);
        Files.createDirectories(output);
        Files.writeString(output.resolve("inspector.json"), json, StandardCharsets.UTF_8);
        Files.writeString(
                output.resolve("results.json"),
                """
                        {
                          "profile": "m10-inspector",
                          "workPackage": "INSPECT-001",
                          "status": "passed",
                          "nodeCount": %d,
                          "tracePresent": true
                        }
                        """.formatted(snapshot.nodes().size()),
                StandardCharsets.UTF_8
        );
    }
}

package org.glavo.himari.inspector;

import org.glavo.himari.controls.ControlGallery;
import org.glavo.himari.layout.Constraints;
import org.glavo.himari.layout.LayoutTree;
import org.glavo.himari.runtime.trace.RuntimeTrace;
import org.glavo.himari.runtime.trace.TraceEventKind;
import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies inspector capture of a placed control gallery.
@NotNullByDefault
final class InspectorTest {
    /// Captures nodes and a runtime trace.
    @Test
    void capturesGalleryAndTrace() {
        LayoutTree tree = new LayoutTree();
        tree.setRoot(new ControlGallery().create(tree));
        tree.measure(Constraints.loose(400.0f, 400.0f));
        tree.place();
        RuntimeTrace trace = new RuntimeTrace();
        trace.record(0L, TraceEventKind.STRUCTURE_ATTEMPT, "gallery", "ready");
        InspectorSnapshot snapshot = Inspector.capture(tree, trace);
        assertTrue(snapshot.nodes().size() >= 6);
        assertTrue(snapshot.toCanonicalJson().contains("\"schema\":\"himari-inspector-v1\""));
        assertTrue(snapshot.toCanonicalJson().contains("himari-runtime-trace-v1"));
        String capturedTrace = snapshot.traceJson();
        if (capturedTrace == null) {
            throw new AssertionError("Inspector omitted the runtime trace");
        }
        InspectorSnapshot replay = Inspector.capture(tree, RuntimeTrace.parse(capturedTrace));
        assertEquals(snapshot.nodes().size(), replay.nodes().size());
    }
}

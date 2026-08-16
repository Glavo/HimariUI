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
        assertTrue(snapshot.toCanonicalJson().contains("\"liveRegion\":\"POLITE\""));
        assertTrue(snapshot.toCanonicalJson().contains("\"textStart\":-1"));
        assertTrue(snapshot.nodes().stream().anyMatch(node ->
                node.role().equals("TEXT_FIELD") && node.textStart() == 0 && node.textEnd() == 0 && node.caret() == 0));
        assertTrue(snapshot.toCanonicalJson().contains("himari-runtime-trace-v1"));
        String capturedTrace = snapshot.traceJson();
        if (capturedTrace == null) {
            throw new AssertionError("Inspector omitted the runtime trace");
        }
        InspectorSnapshot replay = Inspector.capture(tree, RuntimeTrace.parse(capturedTrace));
        assertEquals(snapshot.nodes().size(), replay.nodes().size());
        InspectorSnapshot isolated = InspectorSnapshot.parse(snapshot.toCanonicalJson());
        assertEquals(snapshot.nodes().size(), isolated.nodes().size());
        assertEquals(snapshot.focusedId(), isolated.focusedId());
        assertEquals(snapshot.traceJson(), isolated.traceJson());
        assertEquals(snapshot.nodes().getFirst().role(), isolated.nodes().getFirst().role());
        assertEquals(snapshot.nodes().getFirst().liveRegion(), isolated.nodes().getFirst().liveRegion());
        assertEquals(snapshot.toCanonicalJson(), isolated.toCanonicalJson());
    }
}

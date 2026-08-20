package org.glavo.himari.inspector;

import org.glavo.himari.controls.ControlGallery;
import org.glavo.himari.layout.Constraints;
import org.glavo.himari.layout.LayoutFactory;
import org.glavo.himari.layout.LayoutModifier;
import org.glavo.himari.layout.LayoutNode;
import org.glavo.himari.layout.LayoutTree;
import org.glavo.himari.layout.Size;
import org.glavo.himari.layout.semantics.SemanticsRole;
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
        assertTrue(snapshot.nodes().stream().anyMatch(node ->
                "COLUMN".equals(node.kind()) || "LEAF".equals(node.kind())));
        assertTrue(snapshot.toCanonicalJson().contains("\"kind\":\""));
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
        assertEquals("NONE", snapshot.nodes().getFirst().phase());
        assertEquals(snapshot.nodes().getFirst().kind(), isolated.nodes().getFirst().kind());
        assertEquals(snapshot.nodes().getFirst().phase(), isolated.nodes().getFirst().phase());
        assertEquals(snapshot.nodes().getFirst().role(), isolated.nodes().getFirst().role());
        assertEquals(snapshot.nodes().getFirst().liveRegion(), isolated.nodes().getFirst().liveRegion());
        assertEquals(snapshot.toCanonicalJson(), isolated.toCanonicalJson());
    }

    /// Publishes MEASURE, PLACE, then NONE across one layout pass.
    @Test
    void publishesInvalidationPhaseAcrossLayoutPass() {
        LayoutTree tree = new LayoutTree();
        LayoutFactory factory = new LayoutFactory(tree);
        tree.setRoot(factory.box(
                "root",
                java.util.List.of(),
                factory.leaf(
                        "leaf",
                        new Size(10.0f, 8.0f),
                        java.util.List.of(),
                        false,
                        SemanticsRole.NONE,
                        "leaf",
                        java.util.Set.of(),
                        null
                )
        ));
        InspectorSnapshot unmeasured = Inspector.capture(tree, null);
        assertEquals("MEASURE", unmeasured.nodes().getFirst().phase());
        tree.measure(Constraints.loose(100.0f, 40.0f));
        InspectorSnapshot unplaced = Inspector.capture(tree, null);
        assertEquals("PLACE", unplaced.nodes().getFirst().phase());
        tree.place();
        InspectorSnapshot placed = Inspector.capture(tree, null);
        assertEquals("NONE", placed.nodes().getFirst().phase());
        InspectorSnapshot isolated = InspectorSnapshot.parse(placed.toCanonicalJson());
        assertEquals("NONE", isolated.nodes().getFirst().phase());
    }

    /// Publishes rotation and translation declared on the captured node.
    @Test
    void publishesRotationAndTranslation() {
        LayoutTree tree = new LayoutTree();
        LayoutFactory factory = new LayoutFactory(tree);
        tree.setRoot(factory.box(
                "root",
                java.util.List.of(
                        new LayoutModifier.Rotate(90.0f),
                        new LayoutModifier.Translate(6.0f, 4.0f)
                ),
                factory.leaf(
                        "leaf",
                        new Size(10.0f, 8.0f),
                        java.util.List.of(),
                        false,
                        SemanticsRole.NONE,
                        "leaf",
                        java.util.Set.of(),
                        null
                )
        ));
        tree.measure(Constraints.loose(100.0f, 40.0f));
        tree.place();
        InspectorSnapshot snapshot = Inspector.capture(tree, null);
        assertEquals(90.0f, snapshot.nodes().getFirst().rotation());
        assertEquals(6.0f, snapshot.nodes().getFirst().translationX());
        assertEquals(4.0f, snapshot.nodes().getFirst().translationY());
        InspectorSnapshot isolated = InspectorSnapshot.parse(snapshot.toCanonicalJson());
        assertEquals(90.0f, isolated.nodes().getFirst().rotation());
        assertEquals(6.0f, isolated.nodes().getFirst().translationX());
        assertEquals(4.0f, isolated.nodes().getFirst().translationY());
        assertEquals(snapshot.toCanonicalJson(), isolated.toCanonicalJson());
    }

    /// Publishes inclusive range extents declared on the captured node.
    @Test
    void publishesRangeExtent() {
        LayoutTree tree = new LayoutTree();
        LayoutFactory factory = new LayoutFactory(tree);
        LayoutNode leaf = factory.leaf(
                "volume",
                new Size(160.0f, 24.0f),
                java.util.List.of(),
                true,
                SemanticsRole.SLIDER,
                "Volume",
                java.util.Set.of(),
                null
        );
        leaf.setRangeValue(3.0);
        leaf.setRangeExtent(0.0, 10.0);
        tree.setRoot(leaf);
        tree.measure(Constraints.loose(200.0f, 40.0f));
        tree.place();
        InspectorSnapshot snapshot = Inspector.capture(tree, null);
        assertEquals(0.0, snapshot.nodes().getFirst().rangeMinimum());
        assertEquals(10.0, snapshot.nodes().getFirst().rangeMaximum());
        InspectorSnapshot isolated = InspectorSnapshot.parse(snapshot.toCanonicalJson());
        assertEquals(0.0, isolated.nodes().getFirst().rangeMinimum());
        assertEquals(10.0, isolated.nodes().getFirst().rangeMaximum());
        assertEquals(snapshot.toCanonicalJson(), isolated.toCanonicalJson());
    }

    /// Publishes the hit-clip kind for a rounded-rect clip.
    @Test
    void publishesRoundedClipKind() {
        LayoutTree tree = new LayoutTree();
        LayoutFactory factory = new LayoutFactory(tree);
        tree.setRoot(factory.leaf(
                "round",
                new Size(20.0f, 20.0f),
                java.util.List.of(new LayoutModifier.ClipRRect(8.0f)),
                false,
                SemanticsRole.NONE,
                "round",
                java.util.Set.of(),
                null
        ));
        tree.measure(Constraints.loose(100.0f, 40.0f));
        tree.place();
        InspectorSnapshot snapshot = Inspector.capture(tree, null);
        assertEquals("ROUNDED", snapshot.nodes().getFirst().clipKind());
        InspectorSnapshot isolated = InspectorSnapshot.parse(snapshot.toCanonicalJson());
        assertEquals("ROUNDED", isolated.nodes().getFirst().clipKind());
    }

    /// Rejects inspector documents that name producer-process handles.
    @Test
    void parseRejectsProducerHandles() {
        for (String token : InspectorSnapshot.producerHandleTokens()) {
            try {
                InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"" + token + "\":1}");
                throw new AssertionError("parse accepted an " + token + " handle");
            } catch (IllegalArgumentException ignored) {
                // expected
            }
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hwndtarget\":1}");
            throw new AssertionError("parse accepted an hwndtarget handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
    }
}

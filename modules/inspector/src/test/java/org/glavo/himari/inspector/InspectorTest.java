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

    /// Rejects inspector documents that name producer-process handles.
    @Test
    void parseRejectsProducerHandles() {
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hwnd\":1}");
            throw new AssertionError("parse accepted a producer handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hdc\":1}");
            throw new AssertionError("parse accepted an hdc handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"id3d12\":1}");
            throw new AssertionError("parse accepted an id3d12 handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hbitmap\":1}");
            throw new AssertionError("parse accepted an hbitmap handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hfont\":1}");
            throw new AssertionError("parse accepted an hfont handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"idxgi\":1}");
            throw new AssertionError("parse accepted an idxgi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hicon\":1}");
            throw new AssertionError("parse accepted an hicon handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"himc\":1}");
            throw new AssertionError("parse accepted an himc handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcursor\":1}");
            throw new AssertionError("parse accepted an hcursor handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hmenu\":1}");
            throw new AssertionError("parse accepted an hmenu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hbrush\":1}");
            throw new AssertionError("parse accepted an hbrush handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hrgn\":1}");
            throw new AssertionError("parse accepted an hrgn handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hpalette\":1}");
            throw new AssertionError("parse accepted an hpalette handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hpen\":1}");
            throw new AssertionError("parse accepted an hpen handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"haccel\":1}");
            throw new AssertionError("parse accepted an haccel handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hinstance\":1}");
            throw new AssertionError("parse accepted an hinstance handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hhook\":1}");
            throw new AssertionError("parse accepted an hhook handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hkl\":1}");
            throw new AssertionError("parse accepted an hkl handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hglobal\":1}");
            throw new AssertionError("parse accepted an hglobal handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hmodule\":1}");
            throw new AssertionError("parse accepted an hmodule handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hthread\":1}");
            throw new AssertionError("parse accepted an hthread handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hprocess\":1}");
            throw new AssertionError("parse accepted an hprocess handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hfile\":1}");
            throw new AssertionError("parse accepted an hfile handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hkey\":1}");
            throw new AssertionError("parse accepted an hkey handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hdrop\":1}");
            throw new AssertionError("parse accepted an hdrop handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hdcmem\":1}");
            throw new AssertionError("parse accepted an hdcmem handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hdesk\":1}");
            throw new AssertionError("parse accepted an hdesk handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"htoken\":1}");
            throw new AssertionError("parse accepted an htoken handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hmutex\":1}");
            throw new AssertionError("parse accepted an hmutex handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
    }
}

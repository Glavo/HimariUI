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
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hevent\":1}");
            throw new AssertionError("parse accepted an hevent handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hsemaphore\":1}");
            throw new AssertionError("parse accepted an hsemaphore handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hjob\":1}");
            throw new AssertionError("parse accepted an hjob handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hwaitabletimer\":1}");
            throw new AssertionError("parse accepted an hwaitabletimer handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hsection\":1}");
            throw new AssertionError("parse accepted an hsection handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hmailslot\":1}");
            throw new AssertionError("parse accepted an hmailslot handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hprinter\":1}");
            throw new AssertionError("parse accepted an hprinter handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hpipe\":1}");
            throw new AssertionError("parse accepted an hpipe handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hglrc\":1}");
            throw new AssertionError("parse accepted an hglrc handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hscmanager\":1}");
            throw new AssertionError("parse accepted an hscmanager handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hservice\":1}");
            throw new AssertionError("parse accepted an hservice handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"htimerevent\":1}");
            throw new AssertionError("parse accepted an htimerevent handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hfind\":1}");
            throw new AssertionError("parse accepted an hfind handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hsnapshot\":1}");
            throw new AssertionError("parse accepted an hsnapshot handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hiconsm\":1}");
            throw new AssertionError("parse accepted an hiconsm handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hdesktop\":1}");
            throw new AssertionError("parse accepted an hdesktop handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"henum\":1}");
            throw new AssertionError("parse accepted an henum handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hheap\":1}");
            throw new AssertionError("parse accepted an hheap handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hwinsta\":1}");
            throw new AssertionError("parse accepted an hwinsta handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"htheme\":1}");
            throw new AssertionError("parse accepted an htheme handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"himagelist\":1}");
            throw new AssertionError("parse accepted an himagelist handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"htouchinput\":1}");
            throw new AssertionError("parse accepted an htouchinput handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hactctx\":1}");
            throw new AssertionError("parse accepted an hactctx handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hrawinput\":1}");
            throw new AssertionError("parse accepted an hrawinput handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hpowernotify\":1}");
            throw new AssertionError("parse accepted an hpowernotify handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"henhmetafile\":1}");
            throw new AssertionError("parse accepted an henhmetafile handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hdwp\":1}");
            throw new AssertionError("parse accepted an hdwp handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgesture\":1}");
            throw new AssertionError("parse accepted an hgesture handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hsynch\":1}");
            throw new AssertionError("parse accepted an hsynch handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"htransact\":1}");
            throw new AssertionError("parse accepted an htransact handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgdiobj\":1}");
            throw new AssertionError("parse accepted an hgdiobj handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hdevinfo\":1}");
            throw new AssertionError("parse accepted an hdevinfo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hdevmode\":1}");
            throw new AssertionError("parse accepted an hdevmode handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hdevnames\":1}");
            throw new AssertionError("parse accepted an hdevnames handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcolorplace\":1}");
            throw new AssertionError("parse accepted an hcolorplace handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcolortransform\":1}");
            throw new AssertionError("parse accepted an hcolortransform handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcolorspace\":1}");
            throw new AssertionError("parse accepted an hcolorspace handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hprofile\":1}");
            throw new AssertionError("parse accepted an hprofile handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hicc\":1}");
            throw new AssertionError("parse accepted an hicc handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hkernel\":1}");
            throw new AssertionError("parse accepted an hkernel handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hfilter\":1}");
            throw new AssertionError("parse accepted an hfilter handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hstream\":1}");
            throw new AssertionError("parse accepted an hstream handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hadapter\":1}");
            throw new AssertionError("parse accepted an hadapter handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hresource\":1}");
            throw new AssertionError("parse accepted an hresource handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hshader\":1}");
            throw new AssertionError("parse accepted an hshader handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hswapchain\":1}");
            throw new AssertionError("parse accepted an hswapchain handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hsurface\":1}");
            throw new AssertionError("parse accepted an hsurface handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hfence\":1}");
            throw new AssertionError("parse accepted an hfence handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hpipeline\":1}");
            throw new AssertionError("parse accepted an hpipeline handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcommandqueue\":1}");
            throw new AssertionError("parse accepted an hcommandqueue handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hrtv\":1}");
            throw new AssertionError("parse accepted an hrtv handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hdsv\":1}");
            throw new AssertionError("parse accepted an hdsv handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hsrv\":1}");
            throw new AssertionError("parse accepted an hsrv handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"huav\":1}");
            throw new AssertionError("parse accepted an huav handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcbv\":1}");
            throw new AssertionError("parse accepted an hcbv handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hsampler\":1}");
            throw new AssertionError("parse accepted an hsampler handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hblob\":1}");
            throw new AssertionError("parse accepted an hblob handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hquery\":1}");
            throw new AssertionError("parse accepted an hquery handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hroot\":1}");
            throw new AssertionError("parse accepted an hroot handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hsignature\":1}");
            throw new AssertionError("parse accepted an hsignature handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hmesh\":1}");
            throw new AssertionError("parse accepted an hmesh handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hacceleration\":1}");
            throw new AssertionError("parse accepted an hacceleration handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hlibrary\":1}");
            throw new AssertionError("parse accepted an hlibrary handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcommandallocator\":1}");
            throw new AssertionError("parse accepted an hcommandallocator handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcommandlist\":1}");
            throw new AssertionError("parse accepted an hcommandlist handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hvertexbuffer\":1}");
            throw new AssertionError("parse accepted an hvertexbuffer handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hindexbuffer\":1}");
            throw new AssertionError("parse accepted an hindexbuffer handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"htexture\":1}");
            throw new AssertionError("parse accepted an htexture handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hbundle\":1}");
            throw new AssertionError("parse accepted an hbundle handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hconstantbuffer\":1}");
            throw new AssertionError("parse accepted an hconstantbuffer handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hdepthstencil\":1}");
            throw new AssertionError("parse accepted an hdepthstencil handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hrendertarget\":1}");
            throw new AssertionError("parse accepted an hrendertarget handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hindirectargs\":1}");
            throw new AssertionError("parse accepted an hindirectargs handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"huploadbuffer\":1}");
            throw new AssertionError("parse accepted an huploadbuffer handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackbuffer\":1}");
            throw new AssertionError("parse accepted an hreadbackbuffer handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hstagingbuffer\":1}");
            throw new AssertionError("parse accepted an hstagingbuffer handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hscratchbuffer\":1}");
            throw new AssertionError("parse accepted an hscratchbuffer handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyqueue\":1}");
            throw new AssertionError("parse accepted an hcopyqueue handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hpresentable\":1}");
            throw new AssertionError("parse accepted an hpresentable handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hbindgroup\":1}");
            throw new AssertionError("parse accepted an hbindgroup handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hframegraph\":1}");
            throw new AssertionError("parse accepted an hframegraph handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hbindless\":1}");
            throw new AssertionError("parse accepted an hbindless handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcomputepass\":1}");
            throw new AssertionError("parse accepted an hcomputepass handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hpresentqueue\":1}");
            throw new AssertionError("parse accepted an hpresentqueue handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hrenderpass\":1}");
            throw new AssertionError("parse accepted an hrenderpass handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcomputequeue\":1}");
            throw new AssertionError("parse accepted an hcomputequeue handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hindirectbuffer\":1}");
            throw new AssertionError("parse accepted an hindirectbuffer handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgraphicsqueue\":1}");
            throw new AssertionError("parse accepted an hgraphicsqueue handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"htransferqueue\":1}");
            throw new AssertionError("parse accepted an htransferqueue handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hbindtable\":1}");
            throw new AssertionError("parse accepted an hbindtable handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcommandpool\":1}");
            throw new AssertionError("parse accepted an hcommandpool handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hframebuffer\":1}");
            throw new AssertionError("parse accepted an hframebuffer handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hpushconstant\":1}");
            throw new AssertionError("parse accepted an hpushconstant handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hdrawindirect\":1}");
            throw new AssertionError("parse accepted an hdrawindirect handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hdispatch\":1}");
            throw new AssertionError("parse accepted an hdispatch handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hblitcmd\":1}");
            throw new AssertionError("parse accepted an hblitcmd handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyengine\":1}");
            throw new AssertionError("parse accepted an hcopyengine handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hdrawargs\":1}");
            throw new AssertionError("parse accepted an hdrawargs handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hblitqueue\":1}");
            throw new AssertionError("parse accepted an hblitqueue handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hindirectcount\":1}");
            throw new AssertionError("parse accepted an hindirectcount handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hblas\":1}");
            throw new AssertionError("parse accepted an hblas handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"htlas\":1}");
            throw new AssertionError("parse accepted an htlas handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hrtas\":1}");
            throw new AssertionError("parse accepted an hrtas handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeompass\":1}");
            throw new AssertionError("parse accepted an hgeompass handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcomputeindirect\":1}");
            throw new AssertionError("parse accepted an hcomputeindirect handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hbuildargs\":1}");
            throw new AssertionError("parse accepted an hbuildargs handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcompaction\":1}");
            throw new AssertionError("parse accepted an hcompaction handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomcmd\":1}");
            throw new AssertionError("parse accepted an hgeomcmd handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyargs\":1}");
            throw new AssertionError("parse accepted an hcopyargs handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomstate\":1}");
            throw new AssertionError("parse accepted an hgeomstate handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hbuildqueue\":1}");
            throw new AssertionError("parse accepted an hbuildqueue handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomargs\":1}");
            throw new AssertionError("parse accepted an hgeomargs handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hbuildpass\":1}");
            throw new AssertionError("parse accepted an hbuildpass handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hsyncargs\":1}");
            throw new AssertionError("parse accepted an hsyncargs handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomcache\":1}");
            throw new AssertionError("parse accepted an hgeomcache handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hsyncqueue\":1}");
            throw new AssertionError("parse accepted an hsyncqueue handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"huploadargs\":1}");
            throw new AssertionError("parse accepted an huploadargs handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomset\":1}");
            throw new AssertionError("parse accepted an hgeomset handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyset\":1}");
            throw new AssertionError("parse accepted an hcopyset handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackargs\":1}");
            throw new AssertionError("parse accepted an hreadbackargs handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomlist\":1}");
            throw new AssertionError("parse accepted an hgeomlist handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopylist\":1}");
            throw new AssertionError("parse accepted an hcopylist handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackset\":1}");
            throw new AssertionError("parse accepted an hreadbackset handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeombind\":1}");
            throw new AssertionError("parse accepted an hgeombind handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopybind\":1}");
            throw new AssertionError("parse accepted an hcopybind handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackcmd\":1}");
            throw new AssertionError("parse accepted an hreadbackcmd handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomview\":1}");
            throw new AssertionError("parse accepted an hgeomview handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyview\":1}");
            throw new AssertionError("parse accepted an hcopyview handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackview\":1}");
            throw new AssertionError("parse accepted an hreadbackview handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomdraw\":1}");
            throw new AssertionError("parse accepted an hgeomdraw handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopydraw\":1}");
            throw new AssertionError("parse accepted an hcopydraw handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackdraw\":1}");
            throw new AssertionError("parse accepted an hreadbackdraw handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeombatch\":1}");
            throw new AssertionError("parse accepted an hgeombatch handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopybatch\":1}");
            throw new AssertionError("parse accepted an hcopybatch handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackbatch\":1}");
            throw new AssertionError("parse accepted an hreadbackbatch handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomprim\":1}");
            throw new AssertionError("parse accepted an hgeomprim handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyprim\":1}");
            throw new AssertionError("parse accepted an hcopyprim handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackprim\":1}");
            throw new AssertionError("parse accepted an hreadbackprim handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomwork\":1}");
            throw new AssertionError("parse accepted an hgeomwork handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopywork\":1}");
            throw new AssertionError("parse accepted an hcopywork handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackwork\":1}");
            throw new AssertionError("parse accepted an hreadbackwork handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomsink\":1}");
            throw new AssertionError("parse accepted an hgeomsink handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopysink\":1}");
            throw new AssertionError("parse accepted an hcopysink handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacksink\":1}");
            throw new AssertionError("parse accepted an hreadbacksink handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomtile\":1}");
            throw new AssertionError("parse accepted an hgeomtile handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopytile\":1}");
            throw new AssertionError("parse accepted an hcopytile handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacktile\":1}");
            throw new AssertionError("parse accepted an hreadbacktile handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomquad\":1}");
            throw new AssertionError("parse accepted an hgeomquad handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyquad\":1}");
            throw new AssertionError("parse accepted an hcopyquad handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackquad\":1}");
            throw new AssertionError("parse accepted an hreadbackquad handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomcube\":1}");
            throw new AssertionError("parse accepted an hgeomcube handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopycube\":1}");
            throw new AssertionError("parse accepted an hcopycube handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackcube\":1}");
            throw new AssertionError("parse accepted an hreadbackcube handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomslab\":1}");
            throw new AssertionError("parse accepted an hgeomslab handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyslab\":1}");
            throw new AssertionError("parse accepted an hcopyslab handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackslab\":1}");
            throw new AssertionError("parse accepted an hreadbackslab handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomhull\":1}");
            throw new AssertionError("parse accepted an hgeomhull handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyhull\":1}");
            throw new AssertionError("parse accepted an hcopyhull handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackhull\":1}");
            throw new AssertionError("parse accepted an hreadbackhull handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomring\":1}");
            throw new AssertionError("parse accepted an hgeomring handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyring\":1}");
            throw new AssertionError("parse accepted an hcopyring handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackring\":1}");
            throw new AssertionError("parse accepted an hreadbackring handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomloop\":1}");
            throw new AssertionError("parse accepted an hgeomloop handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyloop\":1}");
            throw new AssertionError("parse accepted an hcopyloop handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackloop\":1}");
            throw new AssertionError("parse accepted an hreadbackloop handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomfan\":1}");
            throw new AssertionError("parse accepted an hgeomfan handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyfan\":1}");
            throw new AssertionError("parse accepted an hcopyfan handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackfan\":1}");
            throw new AssertionError("parse accepted an hreadbackfan handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomarc\":1}");
            throw new AssertionError("parse accepted an hgeomarc handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyarc\":1}");
            throw new AssertionError("parse accepted an hcopyarc handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackarc\":1}");
            throw new AssertionError("parse accepted an hreadbackarc handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomcap\":1}");
            throw new AssertionError("parse accepted an hgeomcap handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopycap\":1}");
            throw new AssertionError("parse accepted an hcopycap handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackcap\":1}");
            throw new AssertionError("parse accepted an hreadbackcap handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeombend\":1}");
            throw new AssertionError("parse accepted an hgeombend handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopybend\":1}");
            throw new AssertionError("parse accepted an hcopybend handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackbend\":1}");
            throw new AssertionError("parse accepted an hreadbackbend handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomjoin\":1}");
            throw new AssertionError("parse accepted an hgeomjoin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyjoin\":1}");
            throw new AssertionError("parse accepted an hcopyjoin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackjoin\":1}");
            throw new AssertionError("parse accepted an hreadbackjoin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomsplice\":1}");
            throw new AssertionError("parse accepted an hgeomsplice handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopysplice\":1}");
            throw new AssertionError("parse accepted an hcopysplice handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacksplice\":1}");
            throw new AssertionError("parse accepted an hreadbacksplice handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomweld\":1}");
            throw new AssertionError("parse accepted an hgeomweld handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyweld\":1}");
            throw new AssertionError("parse accepted an hcopyweld handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackweld\":1}");
            throw new AssertionError("parse accepted an hreadbackweld handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomseam\":1}");
            throw new AssertionError("parse accepted an hgeomseam handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyseam\":1}");
            throw new AssertionError("parse accepted an hcopyseam handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackseam\":1}");
            throw new AssertionError("parse accepted an hreadbackseam handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeommiter\":1}");
            throw new AssertionError("parse accepted an hgeommiter handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopymiter\":1}");
            throw new AssertionError("parse accepted an hcopymiter handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackmiter\":1}");
            throw new AssertionError("parse accepted an hreadbackmiter handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeombevel\":1}");
            throw new AssertionError("parse accepted an hgeombevel handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopybevel\":1}");
            throw new AssertionError("parse accepted an hcopybevel handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackbevel\":1}");
            throw new AssertionError("parse accepted an hreadbackbevel handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomfillet\":1}");
            throw new AssertionError("parse accepted an hgeomfillet handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyfillet\":1}");
            throw new AssertionError("parse accepted an hcopyfillet handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackfillet\":1}");
            throw new AssertionError("parse accepted an hreadbackfillet handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomnotch\":1}");
            throw new AssertionError("parse accepted an hgeomnotch handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopynotch\":1}");
            throw new AssertionError("parse accepted an hcopynotch handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacknotch\":1}");
            throw new AssertionError("parse accepted an hreadbacknotch handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkerf\":1}");
            throw new AssertionError("parse accepted an hgeomkerf handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykerf\":1}");
            throw new AssertionError("parse accepted an hcopykerf handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkerf\":1}");
            throw new AssertionError("parse accepted an hreadbackkerf handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomdado\":1}");
            throw new AssertionError("parse accepted an hgeomdado handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopydado\":1}");
            throw new AssertionError("parse accepted an hcopydado handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackdado\":1}");
            throw new AssertionError("parse accepted an hreadbackdado handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomrabbet\":1}");
            throw new AssertionError("parse accepted an hgeomrabbet handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyrabbet\":1}");
            throw new AssertionError("parse accepted an hcopyrabbet handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackrabbet\":1}");
            throw new AssertionError("parse accepted an hreadbackrabbet handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomtenon\":1}");
            throw new AssertionError("parse accepted an hgeomtenon handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopytenon\":1}");
            throw new AssertionError("parse accepted an hcopytenon handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacktenon\":1}");
            throw new AssertionError("parse accepted an hreadbacktenon handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeommortise\":1}");
            throw new AssertionError("parse accepted an hgeommortise handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopymortise\":1}");
            throw new AssertionError("parse accepted an hcopymortise handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackmortise\":1}");
            throw new AssertionError("parse accepted an hreadbackmortise handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomdovetail\":1}");
            throw new AssertionError("parse accepted an hgeomdovetail handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopydovetail\":1}");
            throw new AssertionError("parse accepted an hcopydovetail handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackdovetail\":1}");
            throw new AssertionError("parse accepted an hreadbackdovetail handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomscarf\":1}");
            throw new AssertionError("parse accepted an hgeomscarf handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyscarf\":1}");
            throw new AssertionError("parse accepted an hcopyscarf handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackscarf\":1}");
            throw new AssertionError("parse accepted an hreadbackscarf handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomspline\":1}");
            throw new AssertionError("parse accepted an hgeomspline handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyspline\":1}");
            throw new AssertionError("parse accepted an hcopyspline handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackspline\":1}");
            throw new AssertionError("parse accepted an hreadbackspline handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeombiscuit\":1}");
            throw new AssertionError("parse accepted an hgeombiscuit handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopybiscuit\":1}");
            throw new AssertionError("parse accepted an hcopybiscuit handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackbiscuit\":1}");
            throw new AssertionError("parse accepted an hreadbackbiscuit handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomdowel\":1}");
            throw new AssertionError("parse accepted an hgeomdowel handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopydowel\":1}");
            throw new AssertionError("parse accepted an hcopydowel handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackdowel\":1}");
            throw new AssertionError("parse accepted an hreadbackdowel handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomwedge\":1}");
            throw new AssertionError("parse accepted an hgeomwedge handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopywedge\":1}");
            throw new AssertionError("parse accepted an hcopywedge handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackwedge\":1}");
            throw new AssertionError("parse accepted an hreadbackwedge handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomhaunch\":1}");
            throw new AssertionError("parse accepted an hgeomhaunch handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyhaunch\":1}");
            throw new AssertionError("parse accepted an hcopyhaunch handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackhaunch\":1}");
            throw new AssertionError("parse accepted an hreadbackhaunch handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomjoggle\":1}");
            throw new AssertionError("parse accepted an hgeomjoggle handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyjoggle\":1}");
            throw new AssertionError("parse accepted an hcopyjoggle handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackjoggle\":1}");
            throw new AssertionError("parse accepted an hreadbackjoggle handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeombridle\":1}");
            throw new AssertionError("parse accepted an hgeombridle handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopybridle\":1}");
            throw new AssertionError("parse accepted an hcopybridle handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackbridle\":1}");
            throw new AssertionError("parse accepted an hreadbackbridle handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomsplay\":1}");
            throw new AssertionError("parse accepted an hgeomsplay handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopysplay\":1}");
            throw new AssertionError("parse accepted an hcopysplay handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacksplay\":1}");
            throw new AssertionError("parse accepted an hreadbacksplay handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomlap\":1}");
            throw new AssertionError("parse accepted an hgeomlap handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopylap\":1}");
            throw new AssertionError("parse accepted an hcopylap handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacklap\":1}");
            throw new AssertionError("parse accepted an hreadbacklap handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomsnip\":1}");
            throw new AssertionError("parse accepted an hgeomsnip handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopysnip\":1}");
            throw new AssertionError("parse accepted an hcopysnip handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacksnip\":1}");
            throw new AssertionError("parse accepted an hreadbacksnip handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomchamfer\":1}");
            throw new AssertionError("parse accepted an hgeomchamfer handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopychamfer\":1}");
            throw new AssertionError("parse accepted an hcopychamfer handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackchamfer\":1}");
            throw new AssertionError("parse accepted an hreadbackchamfer handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomburr\":1}");
            throw new AssertionError("parse accepted an hgeomburr handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyburr\":1}");
            throw new AssertionError("parse accepted an hcopyburr handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackburr\":1}");
            throw new AssertionError("parse accepted an hreadbackburr handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomrebate\":1}");
            throw new AssertionError("parse accepted an hgeomrebate handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyrebate\":1}");
            throw new AssertionError("parse accepted an hcopyrebate handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackrebate\":1}");
            throw new AssertionError("parse accepted an hreadbackrebate handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomhousing\":1}");
            throw new AssertionError("parse accepted an hgeomhousing handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyhousing\":1}");
            throw new AssertionError("parse accepted an hcopyhousing handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackhousing\":1}");
            throw new AssertionError("parse accepted an hreadbackhousing handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomtongue\":1}");
            throw new AssertionError("parse accepted an hgeomtongue handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopytongue\":1}");
            throw new AssertionError("parse accepted an hcopytongue handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacktongue\":1}");
            throw new AssertionError("parse accepted an hreadbacktongue handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomshim\":1}");
            throw new AssertionError("parse accepted an hgeomshim handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyshim\":1}");
            throw new AssertionError("parse accepted an hcopyshim handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackshim\":1}");
            throw new AssertionError("parse accepted an hreadbackshim handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomcleat\":1}");
            throw new AssertionError("parse accepted an hgeomcleat handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopycleat\":1}");
            throw new AssertionError("parse accepted an hcopycleat handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackcleat\":1}");
            throw new AssertionError("parse accepted an hreadbackcleat handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomfinger\":1}");
            throw new AssertionError("parse accepted an hgeomfinger handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyfinger\":1}");
            throw new AssertionError("parse accepted an hcopyfinger handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackfinger\":1}");
            throw new AssertionError("parse accepted an hreadbackfinger handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomgroove\":1}");
            throw new AssertionError("parse accepted an hgeomgroove handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopygroove\":1}");
            throw new AssertionError("parse accepted an hcopygroove handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackgroove\":1}");
            throw new AssertionError("parse accepted an hreadbackgroove handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomplow\":1}");
            throw new AssertionError("parse accepted an hgeomplow handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyplow\":1}");
            throw new AssertionError("parse accepted an hcopyplow handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackplow\":1}");
            throw new AssertionError("parse accepted an hreadbackplow handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomfeather\":1}");
            throw new AssertionError("parse accepted an hgeomfeather handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyfeather\":1}");
            throw new AssertionError("parse accepted an hcopyfeather handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackfeather\":1}");
            throw new AssertionError("parse accepted an hreadbackfeather handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomsplint\":1}");
            throw new AssertionError("parse accepted an hgeomsplint handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopysplint\":1}");
            throw new AssertionError("parse accepted an hcopysplint handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacksplint\":1}");
            throw new AssertionError("parse accepted an hreadbacksplint handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomwhittle\":1}");
            throw new AssertionError("parse accepted an hgeomwhittle handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopywhittle\":1}");
            throw new AssertionError("parse accepted an hcopywhittle handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackwhittle\":1}");
            throw new AssertionError("parse accepted an hreadbackwhittle handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomgraft\":1}");
            throw new AssertionError("parse accepted an hgeomgraft handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopygraft\":1}");
            throw new AssertionError("parse accepted an hcopygraft handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackgraft\":1}");
            throw new AssertionError("parse accepted an hreadbackgraft handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomflute\":1}");
            throw new AssertionError("parse accepted an hgeomflute handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyflute\":1}");
            throw new AssertionError("parse accepted an hcopyflute handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackflute\":1}");
            throw new AssertionError("parse accepted an hreadbackflute handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomscribe\":1}");
            throw new AssertionError("parse accepted an hgeomscribe handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyscribe\":1}");
            throw new AssertionError("parse accepted an hcopyscribe handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackscribe\":1}");
            throw new AssertionError("parse accepted an hreadbackscribe handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomscore\":1}");
            throw new AssertionError("parse accepted an hgeomscore handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyscore\":1}");
            throw new AssertionError("parse accepted an hcopyscore handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackscore\":1}");
            throw new AssertionError("parse accepted an hreadbackscore handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomchase\":1}");
            throw new AssertionError("parse accepted an hgeomchase handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopychase\":1}");
            throw new AssertionError("parse accepted an hcopychase handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackchase\":1}");
            throw new AssertionError("parse accepted an hreadbackchase handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomincise\":1}");
            throw new AssertionError("parse accepted an hgeomincise handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyincise\":1}");
            throw new AssertionError("parse accepted an hcopyincise handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackincise\":1}");
            throw new AssertionError("parse accepted an hreadbackincise handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomgash\":1}");
            throw new AssertionError("parse accepted an hgeomgash handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopygash\":1}");
            throw new AssertionError("parse accepted an hcopygash handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackgash\":1}");
            throw new AssertionError("parse accepted an hreadbackgash handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomcarve\":1}");
            throw new AssertionError("parse accepted an hgeomcarve handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopycarve\":1}");
            throw new AssertionError("parse accepted an hcopycarve handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackcarve\":1}");
            throw new AssertionError("parse accepted an hreadbackcarve handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeometch\":1}");
            throw new AssertionError("parse accepted an hgeometch handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyetch\":1}");
            throw new AssertionError("parse accepted an hcopyetch handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacketch\":1}");
            throw new AssertionError("parse accepted an hreadbacketch handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomincuse\":1}");
            throw new AssertionError("parse accepted an hgeomincuse handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyincuse\":1}");
            throw new AssertionError("parse accepted an hcopyincuse handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackincuse\":1}");
            throw new AssertionError("parse accepted an hreadbackincuse handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomengrave\":1}");
            throw new AssertionError("parse accepted an hgeomengrave handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyengrave\":1}");
            throw new AssertionError("parse accepted an hcopyengrave handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackengrave\":1}");
            throw new AssertionError("parse accepted an hreadbackengrave handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomintaglio\":1}");
            throw new AssertionError("parse accepted an hgeomintaglio handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyintaglio\":1}");
            throw new AssertionError("parse accepted an hcopyintaglio handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackintaglio\":1}");
            throw new AssertionError("parse accepted an hreadbackintaglio handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomrepousse\":1}");
            throw new AssertionError("parse accepted an hgeomrepousse handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyrepousse\":1}");
            throw new AssertionError("parse accepted an hcopyrepousse handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackrepousse\":1}");
            throw new AssertionError("parse accepted an hreadbackrepousse handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomfiligree\":1}");
            throw new AssertionError("parse accepted an hgeomfiligree handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyfiligree\":1}");
            throw new AssertionError("parse accepted an hcopyfiligree handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackfiligree\":1}");
            throw new AssertionError("parse accepted an hreadbackfiligree handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomniello\":1}");
            throw new AssertionError("parse accepted an hgeomniello handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyniello\":1}");
            throw new AssertionError("parse accepted an hcopyniello handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackniello\":1}");
            throw new AssertionError("parse accepted an hreadbackniello handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomchampleve\":1}");
            throw new AssertionError("parse accepted an hgeomchampleve handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopychampleve\":1}");
            throw new AssertionError("parse accepted an hcopychampleve handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackchampleve\":1}");
            throw new AssertionError("parse accepted an hreadbackchampleve handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomcloisonne\":1}");
            throw new AssertionError("parse accepted an hgeomcloisonne handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopycloisonne\":1}");
            throw new AssertionError("parse accepted an hcopycloisonne handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackcloisonne\":1}");
            throw new AssertionError("parse accepted an hreadbackcloisonne handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomdamascene\":1}");
            throw new AssertionError("parse accepted an hgeomdamascene handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopydamascene\":1}");
            throw new AssertionError("parse accepted an hcopydamascene handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackdamascene\":1}");
            throw new AssertionError("parse accepted an hreadbackdamascene handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomvermeil\":1}");
            throw new AssertionError("parse accepted an hgeomvermeil handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyvermeil\":1}");
            throw new AssertionError("parse accepted an hcopyvermeil handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackvermeil\":1}");
            throw new AssertionError("parse accepted an hreadbackvermeil handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomormolu\":1}");
            throw new AssertionError("parse accepted an hgeomormolu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyormolu\":1}");
            throw new AssertionError("parse accepted an hcopyormolu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackormolu\":1}");
            throw new AssertionError("parse accepted an hreadbackormolu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomparcel\":1}");
            throw new AssertionError("parse accepted an hgeomparcel handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyparcel\":1}");
            throw new AssertionError("parse accepted an hcopyparcel handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackparcel\":1}");
            throw new AssertionError("parse accepted an hreadbackparcel handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeominlay\":1}");
            throw new AssertionError("parse accepted an hgeominlay handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyinlay\":1}");
            throw new AssertionError("parse accepted an hcopyinlay handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackinlay\":1}");
            throw new AssertionError("parse accepted an hreadbackinlay handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeommakie\":1}");
            throw new AssertionError("parse accepted an hgeommakie handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopymakie\":1}");
            throw new AssertionError("parse accepted an hcopymakie handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackmakie\":1}");
            throw new AssertionError("parse accepted an hreadbackmakie handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomurushi\":1}");
            throw new AssertionError("parse accepted an hgeomurushi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyurushi\":1}");
            throw new AssertionError("parse accepted an hcopyurushi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackurushi\":1}");
            throw new AssertionError("parse accepted an hreadbackurushi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomgilt\":1}");
            throw new AssertionError("parse accepted an hgeomgilt handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopygilt\":1}");
            throw new AssertionError("parse accepted an hcopygilt handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackgilt\":1}");
            throw new AssertionError("parse accepted an hreadbackgilt handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkoftgari\":1}");
            throw new AssertionError("parse accepted an hgeomkoftgari handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykoftgari\":1}");
            throw new AssertionError("parse accepted an hcopykoftgari handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkoftgari\":1}");
            throw new AssertionError("parse accepted an hreadbackkoftgari handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomzogan\":1}");
            throw new AssertionError("parse accepted an hgeomzogan handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyzogan\":1}");
            throw new AssertionError("parse accepted an hcopyzogan handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackzogan\":1}");
            throw new AssertionError("parse accepted an hreadbackzogan handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomnunome\":1}");
            throw new AssertionError("parse accepted an hgeomnunome handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopynunome\":1}");
            throw new AssertionError("parse accepted an hcopynunome handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacknunome\":1}");
            throw new AssertionError("parse accepted an hreadbacknunome handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomlacquer\":1}");
            throw new AssertionError("parse accepted an hgeomlacquer handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopylacquer\":1}");
            throw new AssertionError("parse accepted an hcopylacquer handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacklacquer\":1}");
            throw new AssertionError("parse accepted an hreadbacklacquer handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomdamask\":1}");
            throw new AssertionError("parse accepted an hgeomdamask handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopydamask\":1}");
            throw new AssertionError("parse accepted an hcopydamask handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackdamask\":1}");
            throw new AssertionError("parse accepted an hreadbackdamask handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkintsugi\":1}");
            throw new AssertionError("parse accepted an hgeomkintsugi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykintsugi\":1}");
            throw new AssertionError("parse accepted an hcopykintsugi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkintsugi\":1}");
            throw new AssertionError("parse accepted an hreadbackkintsugi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomshippo\":1}");
            throw new AssertionError("parse accepted an hgeomshippo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyshippo\":1}");
            throw new AssertionError("parse accepted an hcopyshippo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackshippo\":1}");
            throw new AssertionError("parse accepted an hreadbackshippo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomraku\":1}");
            throw new AssertionError("parse accepted an hgeomraku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyraku\":1}");
            throw new AssertionError("parse accepted an hcopyraku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackraku\":1}");
            throw new AssertionError("parse accepted an hreadbackraku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomoribe\":1}");
            throw new AssertionError("parse accepted an hgeomoribe handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyoribe\":1}");
            throw new AssertionError("parse accepted an hcopyoribe handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackoribe\":1}");
            throw new AssertionError("parse accepted an hreadbackoribe handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkutani\":1}");
            throw new AssertionError("parse accepted an hgeomkutani handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykutani\":1}");
            throw new AssertionError("parse accepted an hcopykutani handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkutani\":1}");
            throw new AssertionError("parse accepted an hreadbackkutani handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomsatsuma\":1}");
            throw new AssertionError("parse accepted an hgeomsatsuma handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopysatsuma\":1}");
            throw new AssertionError("parse accepted an hcopysatsuma handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacksatsuma\":1}");
            throw new AssertionError("parse accepted an hreadbacksatsuma handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomshino\":1}");
            throw new AssertionError("parse accepted an hgeomshino handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyshino\":1}");
            throw new AssertionError("parse accepted an hcopyshino handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackshino\":1}");
            throw new AssertionError("parse accepted an hreadbackshino handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkaratsu\":1}");
            throw new AssertionError("parse accepted an hgeomkaratsu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykaratsu\":1}");
            throw new AssertionError("parse accepted an hcopykaratsu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkaratsu\":1}");
            throw new AssertionError("parse accepted an hreadbackkaratsu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeombizen\":1}");
            throw new AssertionError("parse accepted an hgeombizen handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopybizen\":1}");
            throw new AssertionError("parse accepted an hcopybizen handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackbizen\":1}");
            throw new AssertionError("parse accepted an hreadbackbizen handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomhagi\":1}");
            throw new AssertionError("parse accepted an hgeomhagi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyhagi\":1}");
            throw new AssertionError("parse accepted an hcopyhagi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackhagi\":1}");
            throw new AssertionError("parse accepted an hreadbackhagi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomshigaraki\":1}");
            throw new AssertionError("parse accepted an hgeomshigaraki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyshigaraki\":1}");
            throw new AssertionError("parse accepted an hcopyshigaraki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackshigaraki\":1}");
            throw new AssertionError("parse accepted an hreadbackshigaraki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomtanba\":1}");
            throw new AssertionError("parse accepted an hgeomtanba handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopytanba\":1}");
            throw new AssertionError("parse accepted an hcopytanba handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacktanba\":1}");
            throw new AssertionError("parse accepted an hreadbacktanba handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomtokoname\":1}");
            throw new AssertionError("parse accepted an hgeomtokoname handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopytokoname\":1}");
            throw new AssertionError("parse accepted an hcopytokoname handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacktokoname\":1}");
            throw new AssertionError("parse accepted an hreadbacktokoname handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomechizen\":1}");
            throw new AssertionError("parse accepted an hgeomechizen handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyechizen\":1}");
            throw new AssertionError("parse accepted an hcopyechizen handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackechizen\":1}");
            throw new AssertionError("parse accepted an hreadbackechizen handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeommashiko\":1}");
            throw new AssertionError("parse accepted an hgeommashiko handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopymashiko\":1}");
            throw new AssertionError("parse accepted an hcopymashiko handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackmashiko\":1}");
            throw new AssertionError("parse accepted an hreadbackmashiko handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkasama\":1}");
            throw new AssertionError("parse accepted an hgeomkasama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykasama\":1}");
            throw new AssertionError("parse accepted an hcopykasama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkasama\":1}");
            throw new AssertionError("parse accepted an hreadbackkasama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomarita\":1}");
            throw new AssertionError("parse accepted an hgeomarita handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyarita\":1}");
            throw new AssertionError("parse accepted an hcopyarita handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackarita\":1}");
            throw new AssertionError("parse accepted an hreadbackarita handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomimari\":1}");
            throw new AssertionError("parse accepted an hgeomimari handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyimari\":1}");
            throw new AssertionError("parse accepted an hcopyimari handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackimari\":1}");
            throw new AssertionError("parse accepted an hreadbackimari handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeombanko\":1}");
            throw new AssertionError("parse accepted an hgeombanko handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopybanko\":1}");
            throw new AssertionError("parse accepted an hcopybanko handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackbanko\":1}");
            throw new AssertionError("parse accepted an hreadbackbanko handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomontay\":1}");
            throw new AssertionError("parse accepted an hgeomontay handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyontay\":1}");
            throw new AssertionError("parse accepted an hcopyontay handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackontay\":1}");
            throw new AssertionError("parse accepted an hreadbackontay handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomtsuboya\":1}");
            throw new AssertionError("parse accepted an hgeomtsuboya handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopytsuboya\":1}");
            throw new AssertionError("parse accepted an hcopytsuboya handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacktsuboya\":1}");
            throw new AssertionError("parse accepted an hreadbacktsuboya handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomhasami\":1}");
            throw new AssertionError("parse accepted an hgeomhasami handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyhasami\":1}");
            throw new AssertionError("parse accepted an hcopyhasami handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackhasami\":1}");
            throw new AssertionError("parse accepted an hreadbackhasami handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomagano\":1}");
            throw new AssertionError("parse accepted an hgeomagano handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyagano\":1}");
            throw new AssertionError("parse accepted an hcopyagano handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackagano\":1}");
            throw new AssertionError("parse accepted an hreadbackagano handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomtakatori\":1}");
            throw new AssertionError("parse accepted an hgeomtakatori handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopytakatori\":1}");
            throw new AssertionError("parse accepted an hcopytakatori handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacktakatori\":1}");
            throw new AssertionError("parse accepted an hreadbacktakatori handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomnabeshima\":1}");
            throw new AssertionError("parse accepted an hgeomnabeshima handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopynabeshima\":1}");
            throw new AssertionError("parse accepted an hcopynabeshima handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacknabeshima\":1}");
            throw new AssertionError("parse accepted an hreadbacknabeshima handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkakiemon\":1}");
            throw new AssertionError("parse accepted an hgeomkakiemon handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykakiemon\":1}");
            throw new AssertionError("parse accepted an hcopykakiemon handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkakiemon\":1}");
            throw new AssertionError("parse accepted an hreadbackkakiemon handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomiroe\":1}");
            throw new AssertionError("parse accepted an hgeomiroe handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyiroe\":1}");
            throw new AssertionError("parse accepted an hcopyiroe handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackiroe\":1}");
            throw new AssertionError("parse accepted an hreadbackiroe handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomsometsuke\":1}");
            throw new AssertionError("parse accepted an hgeomsometsuke handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopysometsuke\":1}");
            throw new AssertionError("parse accepted an hcopysometsuke handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacksometsuke\":1}");
            throw new AssertionError("parse accepted an hreadbacksometsuke handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomtenmoku\":1}");
            throw new AssertionError("parse accepted an hgeomtenmoku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopytenmoku\":1}");
            throw new AssertionError("parse accepted an hcopytenmoku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacktenmoku\":1}");
            throw new AssertionError("parse accepted an hreadbacktenmoku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomyohen\":1}");
            throw new AssertionError("parse accepted an hgeomyohen handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyyohen\":1}");
            throw new AssertionError("parse accepted an hcopyyohen handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackyohen\":1}");
            throw new AssertionError("parse accepted an hreadbackyohen handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomjian\":1}");
            throw new AssertionError("parse accepted an hgeomjian handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyjian\":1}");
            throw new AssertionError("parse accepted an hcopyjian handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackjian\":1}");
            throw new AssertionError("parse accepted an hreadbackjian handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkoishiwara\":1}");
            throw new AssertionError("parse accepted an hgeomkoishiwara handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykoishiwara\":1}");
            throw new AssertionError("parse accepted an hcopykoishiwara handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkoishiwara\":1}");
            throw new AssertionError("parse accepted an hreadbackkoishiwara handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeommikawachi\":1}");
            throw new AssertionError("parse accepted an hgeommikawachi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopymikawachi\":1}");
            throw new AssertionError("parse accepted an hcopymikawachi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackmikawachi\":1}");
            throw new AssertionError("parse accepted an hreadbackmikawachi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomiwami\":1}");
            throw new AssertionError("parse accepted an hgeomiwami handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyiwami\":1}");
            throw new AssertionError("parse accepted an hcopyiwami handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackiwami\":1}");
            throw new AssertionError("parse accepted an hreadbackiwami handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomotani\":1}");
            throw new AssertionError("parse accepted an hgeomotani handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyotani\":1}");
            throw new AssertionError("parse accepted an hcopyotani handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackotani\":1}");
            throw new AssertionError("parse accepted an hreadbackotani handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomyomitan\":1}");
            throw new AssertionError("parse accepted an hgeomyomitan handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyyomitan\":1}");
            throw new AssertionError("parse accepted an hcopyyomitan handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackyomitan\":1}");
            throw new AssertionError("parse accepted an hreadbackyomitan handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomiga\":1}");
            throw new AssertionError("parse accepted an hgeomiga handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyiga\":1}");
            throw new AssertionError("parse accepted an hcopyiga handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackiga\":1}");
            throw new AssertionError("parse accepted an hreadbackiga handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeommino\":1}");
            throw new AssertionError("parse accepted an hgeommino handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopymino\":1}");
            throw new AssertionError("parse accepted an hcopymino handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackmino\":1}");
            throw new AssertionError("parse accepted an hreadbackmino handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomtamba\":1}");
            throw new AssertionError("parse accepted an hgeomtamba handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopytamba\":1}");
            throw new AssertionError("parse accepted an hcopytamba handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacktamba\":1}");
            throw new AssertionError("parse accepted an hreadbacktamba handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomyokkaichi\":1}");
            throw new AssertionError("parse accepted an hgeomyokkaichi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyyokkaichi\":1}");
            throw new AssertionError("parse accepted an hcopyyokkaichi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackyokkaichi\":1}");
            throw new AssertionError("parse accepted an hreadbackyokkaichi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomamakusa\":1}");
            throw new AssertionError("parse accepted an hgeomamakusa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyamakusa\":1}");
            throw new AssertionError("parse accepted an hcopyamakusa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackamakusa\":1}");
            throw new AssertionError("parse accepted an hreadbackamakusa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomizumiyama\":1}");
            throw new AssertionError("parse accepted an hgeomizumiyama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyizumiyama\":1}");
            throw new AssertionError("parse accepted an hcopyizumiyama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackizumiyama\":1}");
            throw new AssertionError("parse accepted an hreadbackizumiyama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomtetsuyu\":1}");
            throw new AssertionError("parse accepted an hgeomtetsuyu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopytetsuyu\":1}");
            throw new AssertionError("parse accepted an hcopytetsuyu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacktetsuyu\":1}");
            throw new AssertionError("parse accepted an hreadbacktetsuyu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomnoborigama\":1}");
            throw new AssertionError("parse accepted an hgeomnoborigama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopynoborigama\":1}");
            throw new AssertionError("parse accepted an hcopynoborigama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacknoborigama\":1}");
            throw new AssertionError("parse accepted an hreadbacknoborigama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomanagama\":1}");
            throw new AssertionError("parse accepted an hgeomanagama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyanagama\":1}");
            throw new AssertionError("parse accepted an hcopyanagama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackanagama\":1}");
            throw new AssertionError("parse accepted an hreadbackanagama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomtokikabe\":1}");
            throw new AssertionError("parse accepted an hgeomtokikabe handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopytokikabe\":1}");
            throw new AssertionError("parse accepted an hcopytokikabe handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacktokikabe\":1}");
            throw new AssertionError("parse accepted an hreadbacktokikabe handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomshodai\":1}");
            throw new AssertionError("parse accepted an hgeomshodai handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyshodai\":1}");
            throw new AssertionError("parse accepted an hcopyshodai handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackshodai\":1}");
            throw new AssertionError("parse accepted an hreadbackshodai handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomyatsushiro\":1}");
            throw new AssertionError("parse accepted an hgeomyatsushiro handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyyatsushiro\":1}");
            throw new AssertionError("parse accepted an hcopyyatsushiro handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackyatsushiro\":1}");
            throw new AssertionError("parse accepted an hreadbackyatsushiro handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkiyomizu\":1}");
            throw new AssertionError("parse accepted an hgeomkiyomizu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykiyomizu\":1}");
            throw new AssertionError("parse accepted an hcopykiyomizu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkiyomizu\":1}");
            throw new AssertionError("parse accepted an hreadbackkiyomizu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomasahi\":1}");
            throw new AssertionError("parse accepted an hgeomasahi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyasahi\":1}");
            throw new AssertionError("parse accepted an hcopyasahi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackasahi\":1}");
            throw new AssertionError("parse accepted an hreadbackasahi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomakahada\":1}");
            throw new AssertionError("parse accepted an hgeomakahada handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyakahada\":1}");
            throw new AssertionError("parse accepted an hcopyakahada handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackakahada\":1}");
            throw new AssertionError("parse accepted an hreadbackakahada handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomtachikui\":1}");
            throw new AssertionError("parse accepted an hgeomtachikui handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopytachikui\":1}");
            throw new AssertionError("parse accepted an hcopytachikui handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacktachikui\":1}");
            throw new AssertionError("parse accepted an hreadbacktachikui handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeommumyoi\":1}");
            throw new AssertionError("parse accepted an hgeommumyoi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopymumyoi\":1}");
            throw new AssertionError("parse accepted an hcopymumyoi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackmumyoi\":1}");
            throw new AssertionError("parse accepted an hreadbackmumyoi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomizushi\":1}");
            throw new AssertionError("parse accepted an hgeomizushi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyizushi\":1}");
            throw new AssertionError("parse accepted an hcopyizushi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackizushi\":1}");
            throw new AssertionError("parse accepted an hreadbackizushi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkokuji\":1}");
            throw new AssertionError("parse accepted an hgeomkokuji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykokuji\":1}");
            throw new AssertionError("parse accepted an hcopykokuji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkokuji\":1}");
            throw new AssertionError("parse accepted an hreadbackkokuji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomsuzu\":1}");
            throw new AssertionError("parse accepted an hgeomsuzu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopysuzu\":1}");
            throw new AssertionError("parse accepted an hcopysuzu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacksuzu\":1}");
            throw new AssertionError("parse accepted an hreadbacksuzu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomnaraoka\":1}");
            throw new AssertionError("parse accepted an hgeomnaraoka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopynaraoka\":1}");
            throw new AssertionError("parse accepted an hcopynaraoka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacknaraoka\":1}");
            throw new AssertionError("parse accepted an hreadbacknaraoka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomtobe\":1}");
            throw new AssertionError("parse accepted an hgeomtobe handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopytobe\":1}");
            throw new AssertionError("parse accepted an hcopytobe handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacktobe\":1}");
            throw new AssertionError("parse accepted an hreadbacktobe handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomtanegashima\":1}");
            throw new AssertionError("parse accepted an hgeomtanegashima handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopytanegashima\":1}");
            throw new AssertionError("parse accepted an hcopytanegashima handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacktanegashima\":1}");
            throw new AssertionError("parse accepted an hreadbacktanegashima handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomzeshin\":1}");
            throw new AssertionError("parse accepted an hgeomzeshin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyzeshin\":1}");
            throw new AssertionError("parse accepted an hcopyzeshin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackzeshin\":1}");
            throw new AssertionError("parse accepted an hreadbackzeshin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkyoho\":1}");
            throw new AssertionError("parse accepted an hgeomkyoho handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykyoho\":1}");
            throw new AssertionError("parse accepted an hcopykyoho handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkyoho\":1}");
            throw new AssertionError("parse accepted an hreadbackkyoho handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomotsu\":1}");
            throw new AssertionError("parse accepted an hgeomotsu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyotsu\":1}");
            throw new AssertionError("parse accepted an hcopyotsu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackotsu\":1}");
            throw new AssertionError("parse accepted an hreadbackotsu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomaizuhongo\":1}");
            throw new AssertionError("parse accepted an hgeomaizuhongo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyaizuhongo\":1}");
            throw new AssertionError("parse accepted an hcopyaizuhongo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackaizuhongo\":1}");
            throw new AssertionError("parse accepted an hreadbackaizuhongo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkanazawa\":1}");
            throw new AssertionError("parse accepted an hgeomkanazawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykanazawa\":1}");
            throw new AssertionError("parse accepted an hcopykanazawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkanazawa\":1}");
            throw new AssertionError("parse accepted an hreadbackkanazawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomogama\":1}");
            throw new AssertionError("parse accepted an hgeomogama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyogama\":1}");
            throw new AssertionError("parse accepted an hcopyogama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackogama\":1}");
            throw new AssertionError("parse accepted an hreadbackogama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomwaritake\":1}");
            throw new AssertionError("parse accepted an hgeomwaritake handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopywaritake\":1}");
            throw new AssertionError("parse accepted an hcopywaritake handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackwaritake\":1}");
            throw new AssertionError("parse accepted an hreadbackwaritake handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkameyama\":1}");
            throw new AssertionError("parse accepted an hgeomkameyama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykameyama\":1}");
            throw new AssertionError("parse accepted an hcopykameyama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkameyama\":1}");
            throw new AssertionError("parse accepted an hreadbackkameyama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomhizen\":1}");
            throw new AssertionError("parse accepted an hgeomhizen handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyhizen\":1}");
            throw new AssertionError("parse accepted an hcopyhizen handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackhizen\":1}");
            throw new AssertionError("parse accepted an hreadbackhizen handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomfukakusa\":1}");
            throw new AssertionError("parse accepted an hgeomfukakusa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyfukakusa\":1}");
            throw new AssertionError("parse accepted an hcopyfukakusa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackfukakusa\":1}");
            throw new AssertionError("parse accepted an hreadbackfukakusa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomawaji\":1}");
            throw new AssertionError("parse accepted an hgeomawaji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyawaji\":1}");
            throw new AssertionError("parse accepted an hcopyawaji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackawaji\":1}");
            throw new AssertionError("parse accepted an hreadbackawaji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomsoma\":1}");
            throw new AssertionError("parse accepted an hgeomsoma handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopysoma\":1}");
            throw new AssertionError("parse accepted an hcopysoma handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacksoma\":1}");
            throw new AssertionError("parse accepted an hreadbacksoma handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomawata\":1}");
            throw new AssertionError("parse accepted an hgeomawata handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyawata\":1}");
            throw new AssertionError("parse accepted an hcopyawata handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackawata\":1}");
            throw new AssertionError("parse accepted an hreadbackawata handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomseikanji\":1}");
            throw new AssertionError("parse accepted an hgeomseikanji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyseikanji\":1}");
            throw new AssertionError("parse accepted an hcopyseikanji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackseikanji\":1}");
            throw new AssertionError("parse accepted an hreadbackseikanji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomomuro\":1}");
            throw new AssertionError("parse accepted an hgeomomuro handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyomuro\":1}");
            throw new AssertionError("parse accepted an hcopyomuro handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackomuro\":1}");
            throw new AssertionError("parse accepted an hreadbackomuro handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomninsei\":1}");
            throw new AssertionError("parse accepted an hgeomninsei handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyninsei\":1}");
            throw new AssertionError("parse accepted an hcopyninsei handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackninsei\":1}");
            throw new AssertionError("parse accepted an hreadbackninsei handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkenzan\":1}");
            throw new AssertionError("parse accepted an hgeomkenzan handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykenzan\":1}");
            throw new AssertionError("parse accepted an hcopykenzan handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkenzan\":1}");
            throw new AssertionError("parse accepted an hreadbackkenzan handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomdobachi\":1}");
            throw new AssertionError("parse accepted an hgeomdobachi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopydobachi\":1}");
            throw new AssertionError("parse accepted an hcopydobachi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackdobachi\":1}");
            throw new AssertionError("parse accepted an hreadbackdobachi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkinkozan\":1}");
            throw new AssertionError("parse accepted an hgeomkinkozan handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykinkozan\":1}");
            throw new AssertionError("parse accepted an hcopykinkozan handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkinkozan\":1}");
            throw new AssertionError("parse accepted an hreadbackkinkozan handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomyasaka\":1}");
            throw new AssertionError("parse accepted an hgeomyasaka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyyasaka\":1}");
            throw new AssertionError("parse accepted an hcopyyasaka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackyasaka\":1}");
            throw new AssertionError("parse accepted an hreadbackyasaka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomotowa\":1}");
            throw new AssertionError("parse accepted an hgeomotowa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyotowa\":1}");
            throw new AssertionError("parse accepted an hcopyotowa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackotowa\":1}");
            throw new AssertionError("parse accepted an hreadbackotowa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomgojozaka\":1}");
            throw new AssertionError("parse accepted an hgeomgojozaka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopygojozaka\":1}");
            throw new AssertionError("parse accepted an hcopygojozaka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackgojozaka\":1}");
            throw new AssertionError("parse accepted an hreadbackgojozaka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomrokuhara\":1}");
            throw new AssertionError("parse accepted an hgeomrokuhara handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyrokuhara\":1}");
            throw new AssertionError("parse accepted an hcopyrokuhara handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackrokuhara\":1}");
            throw new AssertionError("parse accepted an hreadbackrokuhara handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomfushimi\":1}");
            throw new AssertionError("parse accepted an hgeomfushimi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyfushimi\":1}");
            throw new AssertionError("parse accepted an hcopyfushimi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackfushimi\":1}");
            throw new AssertionError("parse accepted an hreadbackfushimi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkennin\":1}");
            throw new AssertionError("parse accepted an hgeomkennin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykennin\":1}");
            throw new AssertionError("parse accepted an hcopykennin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkennin\":1}");
            throw new AssertionError("parse accepted an hreadbackkennin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomnanzen\":1}");
            throw new AssertionError("parse accepted an hgeomnanzen handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopynanzen\":1}");
            throw new AssertionError("parse accepted an hcopynanzen handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacknanzen\":1}");
            throw new AssertionError("parse accepted an hreadbacknanzen handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomginkaku\":1}");
            throw new AssertionError("parse accepted an hgeomginkaku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyginkaku\":1}");
            throw new AssertionError("parse accepted an hcopyginkaku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackginkaku\":1}");
            throw new AssertionError("parse accepted an hreadbackginkaku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkinkaku\":1}");
            throw new AssertionError("parse accepted an hgeomkinkaku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykinkaku\":1}");
            throw new AssertionError("parse accepted an hcopykinkaku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkinkaku\":1}");
            throw new AssertionError("parse accepted an hreadbackkinkaku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomtofuku\":1}");
            throw new AssertionError("parse accepted an hgeomtofuku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopytofuku\":1}");
            throw new AssertionError("parse accepted an hcopytofuku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacktofuku\":1}");
            throw new AssertionError("parse accepted an hreadbacktofuku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeombyodoin\":1}");
            throw new AssertionError("parse accepted an hgeombyodoin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopybyodoin\":1}");
            throw new AssertionError("parse accepted an hcopybyodoin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackbyodoin\":1}");
            throw new AssertionError("parse accepted an hreadbackbyodoin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomninnaji\":1}");
            throw new AssertionError("parse accepted an hgeomninnaji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyninnaji\":1}");
            throw new AssertionError("parse accepted an hcopyninnaji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackninnaji\":1}");
            throw new AssertionError("parse accepted an hreadbackninnaji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomdaigo\":1}");
            throw new AssertionError("parse accepted an hgeomdaigo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopydaigo\":1}");
            throw new AssertionError("parse accepted an hcopydaigo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackdaigo\":1}");
            throw new AssertionError("parse accepted an hreadbackdaigo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomseiryo\":1}");
            throw new AssertionError("parse accepted an hgeomseiryo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyseiryo\":1}");
            throw new AssertionError("parse accepted an hcopyseiryo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackseiryo\":1}");
            throw new AssertionError("parse accepted an hreadbackseiryo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomtenryu\":1}");
            throw new AssertionError("parse accepted an hgeomtenryu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopytenryu\":1}");
            throw new AssertionError("parse accepted an hcopytenryu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacktenryu\":1}");
            throw new AssertionError("parse accepted an hreadbacktenryu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomsaiho\":1}");
            throw new AssertionError("parse accepted an hgeomsaiho handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopysaiho\":1}");
            throw new AssertionError("parse accepted an hcopysaiho handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacksaiho\":1}");
            throw new AssertionError("parse accepted an hreadbacksaiho handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomryoan\":1}");
            throw new AssertionError("parse accepted an hgeomryoan handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyryoan\":1}");
            throw new AssertionError("parse accepted an hcopyryoan handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackryoan\":1}");
            throw new AssertionError("parse accepted an hreadbackryoan handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomdaitoku\":1}");
            throw new AssertionError("parse accepted an hgeomdaitoku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopydaitoku\":1}");
            throw new AssertionError("parse accepted an hcopydaitoku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackdaitoku\":1}");
            throw new AssertionError("parse accepted an hreadbackdaitoku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeommyoshin\":1}");
            throw new AssertionError("parse accepted an hgeommyoshin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopymyoshin\":1}");
            throw new AssertionError("parse accepted an hcopymyoshin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackmyoshin\":1}");
            throw new AssertionError("parse accepted an hreadbackmyoshin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomshokoku\":1}");
            throw new AssertionError("parse accepted an hgeomshokoku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyshokoku\":1}");
            throw new AssertionError("parse accepted an hcopyshokoku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackshokoku\":1}");
            throw new AssertionError("parse accepted an hreadbackshokoku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomnijo\":1}");
            throw new AssertionError("parse accepted an hgeomnijo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopynijo\":1}");
            throw new AssertionError("parse accepted an hcopynijo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacknijo\":1}");
            throw new AssertionError("parse accepted an hreadbacknijo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkatsura\":1}");
            throw new AssertionError("parse accepted an hgeomkatsura handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykatsura\":1}");
            throw new AssertionError("parse accepted an hcopykatsura handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkatsura\":1}");
            throw new AssertionError("parse accepted an hreadbackkatsura handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomshugaku\":1}");
            throw new AssertionError("parse accepted an hgeomshugaku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyshugaku\":1}");
            throw new AssertionError("parse accepted an hcopyshugaku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackshugaku\":1}");
            throw new AssertionError("parse accepted an hreadbackshugaku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkamigamo\":1}");
            throw new AssertionError("parse accepted an hgeomkamigamo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykamigamo\":1}");
            throw new AssertionError("parse accepted an hcopykamigamo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkamigamo\":1}");
            throw new AssertionError("parse accepted an hreadbackkamigamo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomhonen\":1}");
            throw new AssertionError("parse accepted an hgeomhonen handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyhonen\":1}");
            throw new AssertionError("parse accepted an hcopyhonen handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackhonen\":1}");
            throw new AssertionError("parse accepted an hreadbackhonen handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomchionin\":1}");
            throw new AssertionError("parse accepted an hgeomchionin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopychionin\":1}");
            throw new AssertionError("parse accepted an hcopychionin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackchionin\":1}");
            throw new AssertionError("parse accepted an hreadbackchionin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomeikando\":1}");
            throw new AssertionError("parse accepted an hgeomeikando handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyeikando\":1}");
            throw new AssertionError("parse accepted an hcopyeikando handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackeikando\":1}");
            throw new AssertionError("parse accepted an hreadbackeikando handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeommanpuku\":1}");
            throw new AssertionError("parse accepted an hgeommanpuku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopymanpuku\":1}");
            throw new AssertionError("parse accepted an hcopymanpuku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackmanpuku\":1}");
            throw new AssertionError("parse accepted an hreadbackmanpuku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkosho\":1}");
            throw new AssertionError("parse accepted an hgeomkosho handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykosho\":1}");
            throw new AssertionError("parse accepted an hcopykosho handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkosho\":1}");
            throw new AssertionError("parse accepted an hreadbackkosho handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomtoji\":1}");
            throw new AssertionError("parse accepted an hgeomtoji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopytoji\":1}");
            throw new AssertionError("parse accepted an hcopytoji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacktoji\":1}");
            throw new AssertionError("parse accepted an hreadbacktoji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomnishi\":1}");
            throw new AssertionError("parse accepted an hgeomnishi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopynishi\":1}");
            throw new AssertionError("parse accepted an hcopynishi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacknishi\":1}");
            throw new AssertionError("parse accepted an hreadbacknishi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomhigashi\":1}");
            throw new AssertionError("parse accepted an hgeomhigashi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyhigashi\":1}");
            throw new AssertionError("parse accepted an hcopyhigashi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackhigashi\":1}");
            throw new AssertionError("parse accepted an hreadbackhigashi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkurama\":1}");
            throw new AssertionError("parse accepted an hgeomkurama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykurama\":1}");
            throw new AssertionError("parse accepted an hcopykurama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkurama\":1}");
            throw new AssertionError("parse accepted an hreadbackkurama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkibune\":1}");
            throw new AssertionError("parse accepted an hgeomkibune handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykibune\":1}");
            throw new AssertionError("parse accepted an hcopykibune handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkibune\":1}");
            throw new AssertionError("parse accepted an hreadbackkibune handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomdaikaku\":1}");
            throw new AssertionError("parse accepted an hgeomdaikaku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopydaikaku\":1}");
            throw new AssertionError("parse accepted an hcopydaikaku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackdaikaku\":1}");
            throw new AssertionError("parse accepted an hreadbackdaikaku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomgion\":1}");
            throw new AssertionError("parse accepted an hgeomgion handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopygion\":1}");
            throw new AssertionError("parse accepted an hcopygion handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackgion\":1}");
            throw new AssertionError("parse accepted an hreadbackgion handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeommaruyama\":1}");
            throw new AssertionError("parse accepted an hgeommaruyama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopymaruyama\":1}");
            throw new AssertionError("parse accepted an hcopymaruyama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackmaruyama\":1}");
            throw new AssertionError("parse accepted an hreadbackmaruyama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomheian\":1}");
            throw new AssertionError("parse accepted an hgeomheian handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyheian\":1}");
            throw new AssertionError("parse accepted an hcopyheian handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackheian\":1}");
            throw new AssertionError("parse accepted an hreadbackheian handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomokazaki\":1}");
            throw new AssertionError("parse accepted an hgeomokazaki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyokazaki\":1}");
            throw new AssertionError("parse accepted an hcopyokazaki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackokazaki\":1}");
            throw new AssertionError("parse accepted an hreadbackokazaki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomshirakawa\":1}");
            throw new AssertionError("parse accepted an hgeomshirakawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyshirakawa\":1}");
            throw new AssertionError("parse accepted an hcopyshirakawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackshirakawa\":1}");
            throw new AssertionError("parse accepted an hreadbackshirakawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeominari\":1}");
            throw new AssertionError("parse accepted an hgeominari handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyinari\":1}");
            throw new AssertionError("parse accepted an hcopyinari handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackinari\":1}");
            throw new AssertionError("parse accepted an hreadbackinari handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomarashiyama\":1}");
            throw new AssertionError("parse accepted an hgeomarashiyama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyarashiyama\":1}");
            throw new AssertionError("parse accepted an hcopyarashiyama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackarashiyama\":1}");
            throw new AssertionError("parse accepted an hreadbackarashiyama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomsagano\":1}");
            throw new AssertionError("parse accepted an hgeomsagano handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopysagano\":1}");
            throw new AssertionError("parse accepted an hcopysagano handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacksagano\":1}");
            throw new AssertionError("parse accepted an hreadbacksagano handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomadashino\":1}");
            throw new AssertionError("parse accepted an hgeomadashino handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyadashino\":1}");
            throw new AssertionError("parse accepted an hcopyadashino handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackadashino\":1}");
            throw new AssertionError("parse accepted an hreadbackadashino handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomohara\":1}");
            throw new AssertionError("parse accepted an hgeomohara handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyohara\":1}");
            throw new AssertionError("parse accepted an hcopyohara handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackohara\":1}");
            throw new AssertionError("parse accepted an hreadbackohara handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomsanzen\":1}");
            throw new AssertionError("parse accepted an hgeomsanzen handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopysanzen\":1}");
            throw new AssertionError("parse accepted an hcopysanzen handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacksanzen\":1}");
            throw new AssertionError("parse accepted an hreadbacksanzen handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomjakko\":1}");
            throw new AssertionError("parse accepted an hgeomjakko handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyjakko\":1}");
            throw new AssertionError("parse accepted an hcopyjakko handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackjakko\":1}");
            throw new AssertionError("parse accepted an hreadbackjakko handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomgiou\":1}");
            throw new AssertionError("parse accepted an hgeomgiou handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopygiou\":1}");
            throw new AssertionError("parse accepted an hcopygiou handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackgiou\":1}");
            throw new AssertionError("parse accepted an hreadbackgiou handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomnisonin\":1}");
            throw new AssertionError("parse accepted an hgeomnisonin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopynisonin\":1}");
            throw new AssertionError("parse accepted an hcopynisonin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacknisonin\":1}");
            throw new AssertionError("parse accepted an hreadbacknisonin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomtakao\":1}");
            throw new AssertionError("parse accepted an hgeomtakao handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopytakao\":1}");
            throw new AssertionError("parse accepted an hcopytakao handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacktakao\":1}");
            throw new AssertionError("parse accepted an hreadbacktakao handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeommizuo\":1}");
            throw new AssertionError("parse accepted an hgeommizuo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopymizuo\":1}");
            throw new AssertionError("parse accepted an hcopymizuo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackmizuo\":1}");
            throw new AssertionError("parse accepted an hreadbackmizuo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomumegahata\":1}");
            throw new AssertionError("parse accepted an hgeomumegahata handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyumegahata\":1}");
            throw new AssertionError("parse accepted an hcopyumegahata handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackumegahata\":1}");
            throw new AssertionError("parse accepted an hreadbackumegahata handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomhiei\":1}");
            throw new AssertionError("parse accepted an hgeomhiei handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyhiei\":1}");
            throw new AssertionError("parse accepted an hcopyhiei handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackhiei\":1}");
            throw new AssertionError("parse accepted an hreadbackhiei handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomenryaku\":1}");
            throw new AssertionError("parse accepted an hgeomenryaku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyenryaku\":1}");
            throw new AssertionError("parse accepted an hcopyenryaku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackenryaku\":1}");
            throw new AssertionError("parse accepted an hreadbackenryaku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomyokawa\":1}");
            throw new AssertionError("parse accepted an hgeomyokawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyyokawa\":1}");
            throw new AssertionError("parse accepted an hcopyyokawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackyokawa\":1}");
            throw new AssertionError("parse accepted an hreadbackyokawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomtodoin\":1}");
            throw new AssertionError("parse accepted an hgeomtodoin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopytodoin\":1}");
            throw new AssertionError("parse accepted an hcopytodoin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacktodoin\":1}");
            throw new AssertionError("parse accepted an hreadbacktodoin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomsakamoto\":1}");
            throw new AssertionError("parse accepted an hgeomsakamoto handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopysakamoto\":1}");
            throw new AssertionError("parse accepted an hcopysakamoto handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacksakamoto\":1}");
            throw new AssertionError("parse accepted an hreadbacksakamoto handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeommudouji\":1}");
            throw new AssertionError("parse accepted an hgeommudouji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopymudouji\":1}");
            throw new AssertionError("parse accepted an hcopymudouji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackmudouji\":1}");
            throw new AssertionError("parse accepted an hreadbackmudouji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomshaka\":1}");
            throw new AssertionError("parse accepted an hgeomshaka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyshaka\":1}");
            throw new AssertionError("parse accepted an hcopyshaka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackshaka\":1}");
            throw new AssertionError("parse accepted an hreadbackshaka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkonpon\":1}");
            throw new AssertionError("parse accepted an hgeomkonpon handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykonpon\":1}");
            throw new AssertionError("parse accepted an hcopykonpon handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkonpon\":1}");
            throw new AssertionError("parse accepted an hreadbackkonpon handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomjodoin\":1}");
            throw new AssertionError("parse accepted an hgeomjodoin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyjodoin\":1}");
            throw new AssertionError("parse accepted an hcopyjodoin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackjodoin\":1}");
            throw new AssertionError("parse accepted an hreadbackjodoin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkaidanin\":1}");
            throw new AssertionError("parse accepted an hgeomkaidanin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykaidanin\":1}");
            throw new AssertionError("parse accepted an hcopykaidanin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkaidanin\":1}");
            throw new AssertionError("parse accepted an hreadbackkaidanin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomsaito\":1}");
            throw new AssertionError("parse accepted an hgeomsaito handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopysaito\":1}");
            throw new AssertionError("parse accepted an hcopysaito handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacksaito\":1}");
            throw new AssertionError("parse accepted an hreadbacksaito handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomishiyama\":1}");
            throw new AssertionError("parse accepted an hgeomishiyama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyishiyama\":1}");
            throw new AssertionError("parse accepted an hcopyishiyama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackishiyama\":1}");
            throw new AssertionError("parse accepted an hreadbackishiyama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeommiidera\":1}");
            throw new AssertionError("parse accepted an hgeommiidera handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopymiidera\":1}");
            throw new AssertionError("parse accepted an hcopymiidera handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackmiidera\":1}");
            throw new AssertionError("parse accepted an hreadbackmiidera handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeombanna\":1}");
            throw new AssertionError("parse accepted an hgeombanna handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopybanna\":1}");
            throw new AssertionError("parse accepted an hcopybanna handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackbanna\":1}");
            throw new AssertionError("parse accepted an hreadbackbanna handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomonin\":1}");
            throw new AssertionError("parse accepted an hgeomonin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyonin\":1}");
            throw new AssertionError("parse accepted an hcopyonin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackonin\":1}");
            throw new AssertionError("parse accepted an hreadbackonin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomzeze\":1}");
            throw new AssertionError("parse accepted an hgeomzeze handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyzeze\":1}");
            throw new AssertionError("parse accepted an hcopyzeze handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackzeze\":1}");
            throw new AssertionError("parse accepted an hreadbackzeze handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkatata\":1}");
            throw new AssertionError("parse accepted an hgeomkatata handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykatata\":1}");
            throw new AssertionError("parse accepted an hcopykatata handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkatata\":1}");
            throw new AssertionError("parse accepted an hreadbackkatata handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkarasaki\":1}");
            throw new AssertionError("parse accepted an hgeomkarasaki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykarasaki\":1}");
            throw new AssertionError("parse accepted an hcopykarasaki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkarasaki\":1}");
            throw new AssertionError("parse accepted an hreadbackkarasaki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomhira\":1}");
            throw new AssertionError("parse accepted an hgeomhira handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyhira\":1}");
            throw new AssertionError("parse accepted an hcopyhira handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackhira\":1}");
            throw new AssertionError("parse accepted an hreadbackhira handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomwani\":1}");
            throw new AssertionError("parse accepted an hgeomwani handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopywani\":1}");
            throw new AssertionError("parse accepted an hcopywani handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackwani\":1}");
            throw new AssertionError("parse accepted an hreadbackwani handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomshigasato\":1}");
            throw new AssertionError("parse accepted an hgeomshigasato handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyshigasato\":1}");
            throw new AssertionError("parse accepted an hcopyshigasato handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackshigasato\":1}");
            throw new AssertionError("parse accepted an hreadbackshigasato handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomawazu\":1}");
            throw new AssertionError("parse accepted an hgeomawazu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyawazu\":1}");
            throw new AssertionError("parse accepted an hcopyawazu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackawazu\":1}");
            throw new AssertionError("parse accepted an hreadbackawazu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomnotogawa\":1}");
            throw new AssertionError("parse accepted an hgeomnotogawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopynotogawa\":1}");
            throw new AssertionError("parse accepted an hcopynotogawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacknotogawa\":1}");
            throw new AssertionError("parse accepted an hreadbacknotogawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomeigenji\":1}");
            throw new AssertionError("parse accepted an hgeomeigenji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyeigenji\":1}");
            throw new AssertionError("parse accepted an hcopyeigenji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackeigenji\":1}");
            throw new AssertionError("parse accepted an hreadbackeigenji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkonan\":1}");
            throw new AssertionError("parse accepted an hgeomkonan handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykonan\":1}");
            throw new AssertionError("parse accepted an hcopykonan handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkonan\":1}");
            throw new AssertionError("parse accepted an hreadbackkonan handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkusatsu\":1}");
            throw new AssertionError("parse accepted an hgeomkusatsu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykusatsu\":1}");
            throw new AssertionError("parse accepted an hcopykusatsu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkusatsu\":1}");
            throw new AssertionError("parse accepted an hreadbackkusatsu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomritto\":1}");
            throw new AssertionError("parse accepted an hgeomritto handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyritto\":1}");
            throw new AssertionError("parse accepted an hcopyritto handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackritto\":1}");
            throw new AssertionError("parse accepted an hreadbackritto handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeommoriyama\":1}");
            throw new AssertionError("parse accepted an hgeommoriyama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopymoriyama\":1}");
            throw new AssertionError("parse accepted an hcopymoriyama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackmoriyama\":1}");
            throw new AssertionError("parse accepted an hreadbackmoriyama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomyasu\":1}");
            throw new AssertionError("parse accepted an hgeomyasu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyyasu\":1}");
            throw new AssertionError("parse accepted an hcopyyasu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackyasu\":1}");
            throw new AssertionError("parse accepted an hreadbackyasu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomhino\":1}");
            throw new AssertionError("parse accepted an hgeomhino handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyhino\":1}");
            throw new AssertionError("parse accepted an hcopyhino handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackhino\":1}");
            throw new AssertionError("parse accepted an hreadbackhino handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkoka\":1}");
            throw new AssertionError("parse accepted an hgeomkoka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykoka\":1}");
            throw new AssertionError("parse accepted an hcopykoka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkoka\":1}");
            throw new AssertionError("parse accepted an hreadbackkoka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomminakuchi\":1}");
            throw new AssertionError("parse accepted an hgeomminakuchi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyminakuchi\":1}");
            throw new AssertionError("parse accepted an hcopyminakuchi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackminakuchi\":1}");
            throw new AssertionError("parse accepted an hreadbackminakuchi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomtsuchi\":1}");
            throw new AssertionError("parse accepted an hgeomtsuchi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopytsuchi\":1}");
            throw new AssertionError("parse accepted an hcopytsuchi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacktsuchi\":1}");
            throw new AssertionError("parse accepted an hreadbacktsuchi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomishibe\":1}");
            throw new AssertionError("parse accepted an hgeomishibe handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyishibe\":1}");
            throw new AssertionError("parse accepted an hcopyishibe handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackishibe\":1}");
            throw new AssertionError("parse accepted an hreadbackishibe handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomtakamiya\":1}");
            throw new AssertionError("parse accepted an hgeomtakamiya handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopytakamiya\":1}");
            throw new AssertionError("parse accepted an hcopytakamiya handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacktakamiya\":1}");
            throw new AssertionError("parse accepted an hreadbacktakamiya handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomyokaichi\":1}");
            throw new AssertionError("parse accepted an hgeomyokaichi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyyokaichi\":1}");
            throw new AssertionError("parse accepted an hcopyyokaichi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackyokaichi\":1}");
            throw new AssertionError("parse accepted an hreadbackyokaichi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomhikone\":1}");
            throw new AssertionError("parse accepted an hgeomhikone handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyhikone\":1}");
            throw new AssertionError("parse accepted an hcopyhikone handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackhikone\":1}");
            throw new AssertionError("parse accepted an hreadbackhikone handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomnagahama\":1}");
            throw new AssertionError("parse accepted an hgeomnagahama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopynagahama\":1}");
            throw new AssertionError("parse accepted an hcopynagahama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacknagahama\":1}");
            throw new AssertionError("parse accepted an hreadbacknagahama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomazuchi\":1}");
            throw new AssertionError("parse accepted an hgeomazuchi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyazuchi\":1}");
            throw new AssertionError("parse accepted an hcopyazuchi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackazuchi\":1}");
            throw new AssertionError("parse accepted an hreadbackazuchi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomyokkaichi\":1}");
            throw new AssertionError("parse accepted an hgeomyokkaichi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyyokkaichi\":1}");
            throw new AssertionError("parse accepted an hcopyyokkaichi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackyokkaichi\":1}");
            throw new AssertionError("parse accepted an hreadbackyokkaichi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomomihachiman\":1}");
            throw new AssertionError("parse accepted an hgeomomihachiman handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyomihachiman\":1}");
            throw new AssertionError("parse accepted an hcopyomihachiman handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackomihachiman\":1}");
            throw new AssertionError("parse accepted an hreadbackomihachiman handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeommaibara\":1}");
            throw new AssertionError("parse accepted an hgeommaibara handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopymaibara\":1}");
            throw new AssertionError("parse accepted an hcopymaibara handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackmaibara\":1}");
            throw new AssertionError("parse accepted an hreadbackmaibara handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomtakatsuki\":1}");
            throw new AssertionError("parse accepted an hgeomtakatsuki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopytakatsuki\":1}");
            throw new AssertionError("parse accepted an hcopytakatsuki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacktakatsuki\":1}");
            throw new AssertionError("parse accepted an hreadbacktakatsuki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomsamegai\":1}");
            throw new AssertionError("parse accepted an hgeomsamegai handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopysamegai\":1}");
            throw new AssertionError("parse accepted an hcopysamegai handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacksamegai\":1}");
            throw new AssertionError("parse accepted an hreadbacksamegai handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkunitomo\":1}");
            throw new AssertionError("parse accepted an hgeomkunitomo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykunitomo\":1}");
            throw new AssertionError("parse accepted an hcopykunitomo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkunitomo\":1}");
            throw new AssertionError("parse accepted an hreadbackkunitomo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomchikubushima\":1}");
            throw new AssertionError("parse accepted an hgeomchikubushima handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopychikubushima\":1}");
            throw new AssertionError("parse accepted an hcopychikubushima handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackchikubushima\":1}");
            throw new AssertionError("parse accepted an hreadbackchikubushima handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomhachiman\":1}");
            throw new AssertionError("parse accepted an hgeomhachiman handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyhachiman\":1}");
            throw new AssertionError("parse accepted an hcopyhachiman handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackhachiman\":1}");
            throw new AssertionError("parse accepted an hreadbackhachiman handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomsawayama\":1}");
            throw new AssertionError("parse accepted an hgeomsawayama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopysawayama\":1}");
            throw new AssertionError("parse accepted an hcopysawayama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacksawayama\":1}");
            throw new AssertionError("parse accepted an hreadbacksawayama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkinomoto\":1}");
            throw new AssertionError("parse accepted an hgeomkinomoto handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykinomoto\":1}");
            throw new AssertionError("parse accepted an hcopykinomoto handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkinomoto\":1}");
            throw new AssertionError("parse accepted an hreadbackkinomoto handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomodani\":1}");
            throw new AssertionError("parse accepted an hgeomodani handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyodani\":1}");
            throw new AssertionError("parse accepted an hcopyodani handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackodani\":1}");
            throw new AssertionError("parse accepted an hreadbackodani handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomtorahime\":1}");
            throw new AssertionError("parse accepted an hgeomtorahime handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopytorahime\":1}");
            throw new AssertionError("parse accepted an hcopytorahime handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacktorahime\":1}");
            throw new AssertionError("parse accepted an hreadbacktorahime handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomibuki\":1}");
            throw new AssertionError("parse accepted an hgeomibuki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyibuki\":1}");
            throw new AssertionError("parse accepted an hcopyibuki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackibuki\":1}");
            throw new AssertionError("parse accepted an hreadbackibuki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomtsuruga\":1}");
            throw new AssertionError("parse accepted an hgeomtsuruga handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopytsuruga\":1}");
            throw new AssertionError("parse accepted an hcopytsuruga handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacktsuruga\":1}");
            throw new AssertionError("parse accepted an hreadbacktsuruga handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomobama\":1}");
            throw new AssertionError("parse accepted an hgeomobama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyobama\":1}");
            throw new AssertionError("parse accepted an hcopyobama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackobama\":1}");
            throw new AssertionError("parse accepted an hreadbackobama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomechizen\":1}");
            throw new AssertionError("parse accepted an hgeomechizen handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyechizen\":1}");
            throw new AssertionError("parse accepted an hcopyechizen handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackechizen\":1}");
            throw new AssertionError("parse accepted an hreadbackechizen handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomimajo\":1}");
            throw new AssertionError("parse accepted an hgeomimajo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyimajo\":1}");
            throw new AssertionError("parse accepted an hcopyimajo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackimajo\":1}");
            throw new AssertionError("parse accepted an hreadbackimajo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomsekigahara\":1}");
            throw new AssertionError("parse accepted an hgeomsekigahara handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopysekigahara\":1}");
            throw new AssertionError("parse accepted an hcopysekigahara handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacksekigahara\":1}");
            throw new AssertionError("parse accepted an hreadbacksekigahara handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomogaki\":1}");
            throw new AssertionError("parse accepted an hgeomogaki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyogaki\":1}");
            throw new AssertionError("parse accepted an hcopyogaki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackogaki\":1}");
            throw new AssertionError("parse accepted an hreadbackogaki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomtarui\":1}");
            throw new AssertionError("parse accepted an hgeomtarui handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopytarui\":1}");
            throw new AssertionError("parse accepted an hcopytarui handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacktarui\":1}");
            throw new AssertionError("parse accepted an hreadbacktarui handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkuwana\":1}");
            throw new AssertionError("parse accepted an hgeomkuwana handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykuwana\":1}");
            throw new AssertionError("parse accepted an hcopykuwana handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkuwana\":1}");
            throw new AssertionError("parse accepted an hreadbackkuwana handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomtakefu\":1}");
            throw new AssertionError("parse accepted an hgeomtakefu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopytakefu\":1}");
            throw new AssertionError("parse accepted an hcopytakefu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacktakefu\":1}");
            throw new AssertionError("parse accepted an hreadbacktakefu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomsabae\":1}");
            throw new AssertionError("parse accepted an hgeomsabae handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopysabae\":1}");
            throw new AssertionError("parse accepted an hcopysabae handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacksabae\":1}");
            throw new AssertionError("parse accepted an hreadbacksabae handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomawara\":1}");
            throw new AssertionError("parse accepted an hgeomawara handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyawara\":1}");
            throw new AssertionError("parse accepted an hcopyawara handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackawara\":1}");
            throw new AssertionError("parse accepted an hreadbackawara handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeommihama\":1}");
            throw new AssertionError("parse accepted an hgeommihama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopymihama\":1}");
            throw new AssertionError("parse accepted an hcopymihama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackmihama\":1}");
            throw new AssertionError("parse accepted an hreadbackmihama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomwakasa\":1}");
            throw new AssertionError("parse accepted an hgeomwakasa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopywakasa\":1}");
            throw new AssertionError("parse accepted an hcopywakasa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackwakasa\":1}");
            throw new AssertionError("parse accepted an hreadbackwakasa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomtsurugi\":1}");
            throw new AssertionError("parse accepted an hgeomtsurugi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopytsurugi\":1}");
            throw new AssertionError("parse accepted an hcopytsurugi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacktsurugi\":1}");
            throw new AssertionError("parse accepted an hreadbacktsurugi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomowase\":1}");
            throw new AssertionError("parse accepted an hgeomowase handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyowase\":1}");
            throw new AssertionError("parse accepted an hcopyowase handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackowase\":1}");
            throw new AssertionError("parse accepted an hreadbackowase handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkumano\":1}");
            throw new AssertionError("parse accepted an hgeomkumano handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykumano\":1}");
            throw new AssertionError("parse accepted an hcopykumano handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkumano\":1}");
            throw new AssertionError("parse accepted an hreadbackkumano handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkihoku\":1}");
            throw new AssertionError("parse accepted an hgeomkihoku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykihoku\":1}");
            throw new AssertionError("parse accepted an hcopykihoku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkihoku\":1}");
            throw new AssertionError("parse accepted an hreadbackkihoku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeommatsusaka\":1}");
            throw new AssertionError("parse accepted an hgeommatsusaka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopymatsusaka\":1}");
            throw new AssertionError("parse accepted an hcopymatsusaka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackmatsusaka\":1}");
            throw new AssertionError("parse accepted an hreadbackmatsusaka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomtoba\":1}");
            throw new AssertionError("parse accepted an hgeomtoba handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopytoba\":1}");
            throw new AssertionError("parse accepted an hcopytoba handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacktoba\":1}");
            throw new AssertionError("parse accepted an hreadbacktoba handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomfukui\":1}");
            throw new AssertionError("parse accepted an hgeomfukui handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyfukui\":1}");
            throw new AssertionError("parse accepted an hcopyfukui handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackfukui\":1}");
            throw new AssertionError("parse accepted an hreadbackfukui handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkomatsu\":1}");
            throw new AssertionError("parse accepted an hgeomkomatsu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykomatsu\":1}");
            throw new AssertionError("parse accepted an hcopykomatsu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkomatsu\":1}");
            throw new AssertionError("parse accepted an hreadbackkomatsu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkaga\":1}");
            throw new AssertionError("parse accepted an hgeomkaga handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykaga\":1}");
            throw new AssertionError("parse accepted an hcopykaga handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkaga\":1}");
            throw new AssertionError("parse accepted an hreadbackkaga handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeommikuni\":1}");
            throw new AssertionError("parse accepted an hgeommikuni handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopymikuni\":1}");
            throw new AssertionError("parse accepted an hcopymikuni handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackmikuni\":1}");
            throw new AssertionError("parse accepted an hreadbackmikuni handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomeiheiji\":1}");
            throw new AssertionError("parse accepted an hgeomeiheiji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyeiheiji\":1}");
            throw new AssertionError("parse accepted an hcopyeiheiji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackeiheiji\":1}");
            throw new AssertionError("parse accepted an hreadbackeiheiji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkatsuyama\":1}");
            throw new AssertionError("parse accepted an hgeomkatsuyama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykatsuyama\":1}");
            throw new AssertionError("parse accepted an hcopykatsuyama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkatsuyama\":1}");
            throw new AssertionError("parse accepted an hreadbackkatsuyama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomnagaoka\":1}");
            throw new AssertionError("parse accepted an hgeomnagaoka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopynagaoka\":1}");
            throw new AssertionError("parse accepted an hcopynagaoka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacknagaoka\":1}");
            throw new AssertionError("parse accepted an hreadbacknagaoka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeommaruoka\":1}");
            throw new AssertionError("parse accepted an hgeommaruoka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopymaruoka\":1}");
            throw new AssertionError("parse accepted an hcopymaruoka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackmaruoka\":1}");
            throw new AssertionError("parse accepted an hreadbackmaruoka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomhashidate\":1}");
            throw new AssertionError("parse accepted an hgeomhashidate handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyhashidate\":1}");
            throw new AssertionError("parse accepted an hcopyhashidate handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackhashidate\":1}");
            throw new AssertionError("parse accepted an hreadbackhashidate handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeommaizuru\":1}");
            throw new AssertionError("parse accepted an hgeommaizuru handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopymaizuru\":1}");
            throw new AssertionError("parse accepted an hcopymaizuru handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackmaizuru\":1}");
            throw new AssertionError("parse accepted an hreadbackmaizuru handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeommiyazu\":1}");
            throw new AssertionError("parse accepted an hgeommiyazu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopymiyazu\":1}");
            throw new AssertionError("parse accepted an hcopymiyazu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackmiyazu\":1}");
            throw new AssertionError("parse accepted an hreadbackmiyazu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomamino\":1}");
            throw new AssertionError("parse accepted an hgeomamino handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyamino\":1}");
            throw new AssertionError("parse accepted an hcopyamino handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackamino\":1}");
            throw new AssertionError("parse accepted an hreadbackamino handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkumihama\":1}");
            throw new AssertionError("parse accepted an hgeomkumihama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykumihama\":1}");
            throw new AssertionError("parse accepted an hcopykumihama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkumihama\":1}");
            throw new AssertionError("parse accepted an hreadbackkumihama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeommineyama\":1}");
            throw new AssertionError("parse accepted an hgeommineyama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopymineyama\":1}");
            throw new AssertionError("parse accepted an hcopymineyama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackmineyama\":1}");
            throw new AssertionError("parse accepted an hreadbackmineyama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomnodagawa\":1}");
            throw new AssertionError("parse accepted an hgeomnodagawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopynodagawa\":1}");
            throw new AssertionError("parse accepted an hcopynodagawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacknodagawa\":1}");
            throw new AssertionError("parse accepted an hreadbacknodagawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomyosano\":1}");
            throw new AssertionError("parse accepted an hgeomyosano handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyyosano\":1}");
            throw new AssertionError("parse accepted an hcopyyosano handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackyosano\":1}");
            throw new AssertionError("parse accepted an hreadbackyosano handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkyotango\":1}");
            throw new AssertionError("parse accepted an hgeomkyotango handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykyotango\":1}");
            throw new AssertionError("parse accepted an hcopykyotango handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkyotango\":1}");
            throw new AssertionError("parse accepted an hreadbackkyotango handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomfukuchiyama\":1}");
            throw new AssertionError("parse accepted an hgeomfukuchiyama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyfukuchiyama\":1}");
            throw new AssertionError("parse accepted an hcopyfukuchiyama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackfukuchiyama\":1}");
            throw new AssertionError("parse accepted an hreadbackfukuchiyama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomayabe\":1}");
            throw new AssertionError("parse accepted an hgeomayabe handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyayabe\":1}");
            throw new AssertionError("parse accepted an hcopyayabe handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackayabe\":1}");
            throw new AssertionError("parse accepted an hreadbackayabe handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomnantan\":1}");
            throw new AssertionError("parse accepted an hgeomnantan handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopynantan\":1}");
            throw new AssertionError("parse accepted an hcopynantan handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacknantan\":1}");
            throw new AssertionError("parse accepted an hreadbacknantan handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomsonobe\":1}");
            throw new AssertionError("parse accepted an hgeomsonobe handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopysonobe\":1}");
            throw new AssertionError("parse accepted an hcopysonobe handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacksonobe\":1}");
            throw new AssertionError("parse accepted an hreadbacksonobe handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomhiyoshi\":1}");
            throw new AssertionError("parse accepted an hgeomhiyoshi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyhiyoshi\":1}");
            throw new AssertionError("parse accepted an hcopyhiyoshi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackhiyoshi\":1}");
            throw new AssertionError("parse accepted an hreadbackhiyoshi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeommiyama\":1}");
            throw new AssertionError("parse accepted an hgeommiyama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopymiyama\":1}");
            throw new AssertionError("parse accepted an hcopymiyama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackmiyama\":1}");
            throw new AssertionError("parse accepted an hreadbackmiyama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomwachi\":1}");
            throw new AssertionError("parse accepted an hgeomwachi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopywachi\":1}");
            throw new AssertionError("parse accepted an hcopywachi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackwachi\":1}");
            throw new AssertionError("parse accepted an hreadbackwachi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkeihoku\":1}");
            throw new AssertionError("parse accepted an hgeomkeihoku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykeihoku\":1}");
            throw new AssertionError("parse accepted an hcopykeihoku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkeihoku\":1}");
            throw new AssertionError("parse accepted an hreadbackkeihoku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomumekoji\":1}");
            throw new AssertionError("parse accepted an hgeomumekoji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyumekoji\":1}");
            throw new AssertionError("parse accepted an hcopyumekoji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackumekoji\":1}");
            throw new AssertionError("parse accepted an hreadbackumekoji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeommomoyama\":1}");
            throw new AssertionError("parse accepted an hgeommomoyama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopymomoyama\":1}");
            throw new AssertionError("parse accepted an hcopymomoyama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackmomoyama\":1}");
            throw new AssertionError("parse accepted an hreadbackmomoyama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomuzumasa\":1}");
            throw new AssertionError("parse accepted an hgeomuzumasa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyuzumasa\":1}");
            throw new AssertionError("parse accepted an hcopyuzumasa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackuzumasa\":1}");
            throw new AssertionError("parse accepted an hreadbackuzumasa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomhanazono\":1}");
            throw new AssertionError("parse accepted an hgeomhanazono handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyhanazono\":1}");
            throw new AssertionError("parse accepted an hcopyhanazono handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackhanazono\":1}");
            throw new AssertionError("parse accepted an hreadbackhanazono handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkamogawa\":1}");
            throw new AssertionError("parse accepted an hgeomkamogawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykamogawa\":1}");
            throw new AssertionError("parse accepted an hcopykamogawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkamogawa\":1}");
            throw new AssertionError("parse accepted an hreadbackkamogawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomyase\":1}");
            throw new AssertionError("parse accepted an hgeomyase handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyyase\":1}");
            throw new AssertionError("parse accepted an hcopyyase handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackyase\":1}");
            throw new AssertionError("parse accepted an hreadbackyase handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomimamiya\":1}");
            throw new AssertionError("parse accepted an hgeomimamiya handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyimamiya\":1}");
            throw new AssertionError("parse accepted an hcopyimamiya handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackimamiya\":1}");
            throw new AssertionError("parse accepted an hreadbackimamiya handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomhorikawa\":1}");
            throw new AssertionError("parse accepted an hgeomhorikawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyhorikawa\":1}");
            throw new AssertionError("parse accepted an hcopyhorikawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackhorikawa\":1}");
            throw new AssertionError("parse accepted an hreadbackhorikawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkawaramachi\":1}");
            throw new AssertionError("parse accepted an hgeomkawaramachi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykawaramachi\":1}");
            throw new AssertionError("parse accepted an hcopykawaramachi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkawaramachi\":1}");
            throw new AssertionError("parse accepted an hreadbackkawaramachi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomteramachi\":1}");
            throw new AssertionError("parse accepted an hgeomteramachi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyteramachi\":1}");
            throw new AssertionError("parse accepted an hcopyteramachi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackteramachi\":1}");
            throw new AssertionError("parse accepted an hreadbackteramachi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkiyamachi\":1}");
            throw new AssertionError("parse accepted an hgeomkiyamachi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykiyamachi\":1}");
            throw new AssertionError("parse accepted an hcopykiyamachi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkiyamachi\":1}");
            throw new AssertionError("parse accepted an hreadbackkiyamachi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeompontocho\":1}");
            throw new AssertionError("parse accepted an hgeompontocho handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopypontocho\":1}");
            throw new AssertionError("parse accepted an hcopypontocho handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackpontocho\":1}");
            throw new AssertionError("parse accepted an hreadbackpontocho handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkitano\":1}");
            throw new AssertionError("parse accepted an hgeomkitano handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykitano\":1}");
            throw new AssertionError("parse accepted an hcopykitano handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkitano\":1}");
            throw new AssertionError("parse accepted an hreadbackkitano handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomsanjo\":1}");
            throw new AssertionError("parse accepted an hgeomsanjo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopysanjo\":1}");
            throw new AssertionError("parse accepted an hcopysanjo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacksanjo\":1}");
            throw new AssertionError("parse accepted an hreadbacksanjo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomshijo\":1}");
            throw new AssertionError("parse accepted an hgeomshijo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyshijo\":1}");
            throw new AssertionError("parse accepted an hcopyshijo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackshijo\":1}");
            throw new AssertionError("parse accepted an hreadbackshijo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkarasuma\":1}");
            throw new AssertionError("parse accepted an hgeomkarasuma handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykarasuma\":1}");
            throw new AssertionError("parse accepted an hcopykarasuma handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkarasuma\":1}");
            throw new AssertionError("parse accepted an hreadbackkarasuma handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomtakoyakushi\":1}");
            throw new AssertionError("parse accepted an hgeomtakoyakushi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopytakoyakushi\":1}");
            throw new AssertionError("parse accepted an hcopytakoyakushi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacktakoyakushi\":1}");
            throw new AssertionError("parse accepted an hreadbacktakoyakushi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomoike\":1}");
            throw new AssertionError("parse accepted an hgeomoike handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyoike\":1}");
            throw new AssertionError("parse accepted an hcopyoike handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackoike\":1}");
            throw new AssertionError("parse accepted an hreadbackoike handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeommarutamachi\":1}");
            throw new AssertionError("parse accepted an hgeommarutamachi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopymarutamachi\":1}");
            throw new AssertionError("parse accepted an hcopymarutamachi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackmarutamachi\":1}");
            throw new AssertionError("parse accepted an hreadbackmarutamachi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomimadegawa\":1}");
            throw new AssertionError("parse accepted an hgeomimadegawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyimadegawa\":1}");
            throw new AssertionError("parse accepted an hcopyimadegawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackimadegawa\":1}");
            throw new AssertionError("parse accepted an hreadbackimadegawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkitaoji\":1}");
            throw new AssertionError("parse accepted an hgeomkitaoji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykitaoji\":1}");
            throw new AssertionError("parse accepted an hcopykitaoji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkitaoji\":1}");
            throw new AssertionError("parse accepted an hreadbackkitaoji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkawabata\":1}");
            throw new AssertionError("parse accepted an hgeomkawabata handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykawabata\":1}");
            throw new AssertionError("parse accepted an hcopykawabata handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkawabata\":1}");
            throw new AssertionError("parse accepted an hreadbackkawabata handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomtakatsuji\":1}");
            throw new AssertionError("parse accepted an hgeomtakatsuji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopytakatsuji\":1}");
            throw new AssertionError("parse accepted an hcopytakatsuji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacktakatsuji\":1}");
            throw new AssertionError("parse accepted an hreadbacktakatsuji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeommatsubara\":1}");
            throw new AssertionError("parse accepted an hgeommatsubara handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopymatsubara\":1}");
            throw new AssertionError("parse accepted an hcopymatsubara handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackmatsubara\":1}");
            throw new AssertionError("parse accepted an hreadbackmatsubara handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomshiokoji\":1}");
            throw new AssertionError("parse accepted an hgeomshiokoji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyshiokoji\":1}");
            throw new AssertionError("parse accepted an hcopyshiokoji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackshiokoji\":1}");
            throw new AssertionError("parse accepted an hreadbackshiokoji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomhachijo\":1}");
            throw new AssertionError("parse accepted an hgeomhachijo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyhachijo\":1}");
            throw new AssertionError("parse accepted an hcopyhachijo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackhachijo\":1}");
            throw new AssertionError("parse accepted an hreadbackhachijo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomjujo\":1}");
            throw new AssertionError("parse accepted an hgeomjujo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyjujo\":1}");
            throw new AssertionError("parse accepted an hcopyjujo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackjujo\":1}");
            throw new AssertionError("parse accepted an hreadbackjujo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkujo\":1}");
            throw new AssertionError("parse accepted an hgeomkujo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykujo\":1}");
            throw new AssertionError("parse accepted an hcopykujo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkujo\":1}");
            throw new AssertionError("parse accepted an hreadbackkujo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomshichijo\":1}");
            throw new AssertionError("parse accepted an hgeomshichijo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyshichijo\":1}");
            throw new AssertionError("parse accepted an hcopyshichijo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackshichijo\":1}");
            throw new AssertionError("parse accepted an hreadbackshichijo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomshinkyogoku\":1}");
            throw new AssertionError("parse accepted an hgeomshinkyogoku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyshinkyogoku\":1}");
            throw new AssertionError("parse accepted an hcopyshinkyogoku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackshinkyogoku\":1}");
            throw new AssertionError("parse accepted an hreadbackshinkyogoku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomuji\":1}");
            throw new AssertionError("parse accepted an hgeomuji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyuji\":1}");
            throw new AssertionError("parse accepted an hcopyuji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackuji\":1}");
            throw new AssertionError("parse accepted an hreadbackuji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomyamashina\":1}");
            throw new AssertionError("parse accepted an hgeomyamashina handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyyamashina\":1}");
            throw new AssertionError("parse accepted an hcopyyamashina handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackyamashina\":1}");
            throw new AssertionError("parse accepted an hreadbackyamashina handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomyodo\":1}");
            throw new AssertionError("parse accepted an hgeomyodo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyyodo\":1}");
            throw new AssertionError("parse accepted an hcopyyodo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackyodo\":1}");
            throw new AssertionError("parse accepted an hreadbackyodo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomogura\":1}");
            throw new AssertionError("parse accepted an hgeomogura handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyogura\":1}");
            throw new AssertionError("parse accepted an hcopyogura handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackogura\":1}");
            throw new AssertionError("parse accepted an hreadbackogura handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkohata\":1}");
            throw new AssertionError("parse accepted an hgeomkohata handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykohata\":1}");
            throw new AssertionError("parse accepted an hcopykohata handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkohata\":1}");
            throw new AssertionError("parse accepted an hreadbackkohata handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomrokujizo\":1}");
            throw new AssertionError("parse accepted an hgeomrokujizo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyrokujizo\":1}");
            throw new AssertionError("parse accepted an hcopyrokujizo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackrokujizo\":1}");
            throw new AssertionError("parse accepted an hreadbackrokujizo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomoubaku\":1}");
            throw new AssertionError("parse accepted an hgeomoubaku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyoubaku\":1}");
            throw new AssertionError("parse accepted an hcopyoubaku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackoubaku\":1}");
            throw new AssertionError("parse accepted an hreadbackoubaku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeommakishima\":1}");
            throw new AssertionError("parse accepted an hgeommakishima handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopymakishima\":1}");
            throw new AssertionError("parse accepted an hcopymakishima handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackmakishima\":1}");
            throw new AssertionError("parse accepted an hreadbackmakishima handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomobaku\":1}");
            throw new AssertionError("parse accepted an hgeomobaku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyobaku\":1}");
            throw new AssertionError("parse accepted an hcopyobaku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackobaku\":1}");
            throw new AssertionError("parse accepted an hreadbackobaku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomtowada\":1}");
            throw new AssertionError("parse accepted an hgeomtowada handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopytowada\":1}");
            throw new AssertionError("parse accepted an hcopytowada handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacktowada\":1}");
            throw new AssertionError("parse accepted an hreadbacktowada handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeommuko\":1}");
            throw new AssertionError("parse accepted an hgeommuko handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopymuko\":1}");
            throw new AssertionError("parse accepted an hcopymuko handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackmuko\":1}");
            throw new AssertionError("parse accepted an hreadbackmuko handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomoyamazaki\":1}");
            throw new AssertionError("parse accepted an hgeomoyamazaki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyoyamazaki\":1}");
            throw new AssertionError("parse accepted an hcopyoyamazaki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackoyamazaki\":1}");
            throw new AssertionError("parse accepted an hreadbackoyamazaki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomyawata\":1}");
            throw new AssertionError("parse accepted an hgeomyawata handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyyawata\":1}");
            throw new AssertionError("parse accepted an hcopyyawata handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackyawata\":1}");
            throw new AssertionError("parse accepted an hreadbackyawata handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkyotanabe\":1}");
            throw new AssertionError("parse accepted an hgeomkyotanabe handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykyotanabe\":1}");
            throw new AssertionError("parse accepted an hcopykyotanabe handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkyotanabe\":1}");
            throw new AssertionError("parse accepted an hreadbackkyotanabe handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkizu\":1}");
            throw new AssertionError("parse accepted an hgeomkizu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykizu\":1}");
            throw new AssertionError("parse accepted an hcopykizu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkizu\":1}");
            throw new AssertionError("parse accepted an hreadbackkizu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomide\":1}");
            throw new AssertionError("parse accepted an hgeomide handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyide\":1}");
            throw new AssertionError("parse accepted an hcopyide handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackide\":1}");
            throw new AssertionError("parse accepted an hreadbackide handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomwazuka\":1}");
            throw new AssertionError("parse accepted an hgeomwazuka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopywazuka\":1}");
            throw new AssertionError("parse accepted an hcopywazuka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackwazuka\":1}");
            throw new AssertionError("parse accepted an hreadbackwazuka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkasagi\":1}");
            throw new AssertionError("parse accepted an hgeomkasagi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykasagi\":1}");
            throw new AssertionError("parse accepted an hcopykasagi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkasagi\":1}");
            throw new AssertionError("parse accepted an hreadbackkasagi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkatano\":1}");
            throw new AssertionError("parse accepted an hgeomkatano handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykatano\":1}");
            throw new AssertionError("parse accepted an hcopykatano handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkatano\":1}");
            throw new AssertionError("parse accepted an hreadbackkatano handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomneyagawa\":1}");
            throw new AssertionError("parse accepted an hgeomneyagawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyneyagawa\":1}");
            throw new AssertionError("parse accepted an hcopyneyagawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackneyagawa\":1}");
            throw new AssertionError("parse accepted an hreadbackneyagawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkadoma\":1}");
            throw new AssertionError("parse accepted an hgeomkadoma handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykadoma\":1}");
            throw new AssertionError("parse accepted an hcopykadoma handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkadoma\":1}");
            throw new AssertionError("parse accepted an hreadbackkadoma handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeommoriguchi\":1}");
            throw new AssertionError("parse accepted an hgeommoriguchi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopymoriguchi\":1}");
            throw new AssertionError("parse accepted an hcopymoriguchi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackmoriguchi\":1}");
            throw new AssertionError("parse accepted an hreadbackmoriguchi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomsuita\":1}");
            throw new AssertionError("parse accepted an hgeomsuita handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopysuita\":1}");
            throw new AssertionError("parse accepted an hcopysuita handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacksuita\":1}");
            throw new AssertionError("parse accepted an hreadbacksuita handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomibaraki\":1}");
            throw new AssertionError("parse accepted an hgeomibaraki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyibaraki\":1}");
            throw new AssertionError("parse accepted an hcopyibaraki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackibaraki\":1}");
            throw new AssertionError("parse accepted an hreadbackibaraki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomikeda\":1}");
            throw new AssertionError("parse accepted an hgeomikeda handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyikeda\":1}");
            throw new AssertionError("parse accepted an hcopyikeda handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackikeda\":1}");
            throw new AssertionError("parse accepted an hreadbackikeda handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomtoyonaka\":1}");
            throw new AssertionError("parse accepted an hgeomtoyonaka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopytoyonaka\":1}");
            throw new AssertionError("parse accepted an hcopytoyonaka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacktoyonaka\":1}");
            throw new AssertionError("parse accepted an hreadbacktoyonaka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomyao\":1}");
            throw new AssertionError("parse accepted an hgeomyao handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyyao\":1}");
            throw new AssertionError("parse accepted an hcopyyao handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackyao\":1}");
            throw new AssertionError("parse accepted an hreadbackyao handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkashiwara\":1}");
            throw new AssertionError("parse accepted an hgeomkashiwara handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykashiwara\":1}");
            throw new AssertionError("parse accepted an hcopykashiwara handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkashiwara\":1}");
            throw new AssertionError("parse accepted an hreadbackkashiwara handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomhabikino\":1}");
            throw new AssertionError("parse accepted an hgeomhabikino handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyhabikino\":1}");
            throw new AssertionError("parse accepted an hcopyhabikino handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackhabikino\":1}");
            throw new AssertionError("parse accepted an hreadbackhabikino handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomtondabayashi\":1}");
            throw new AssertionError("parse accepted an hgeomtondabayashi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopytondabayashi\":1}");
            throw new AssertionError("parse accepted an hcopytondabayashi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacktondabayashi\":1}");
            throw new AssertionError("parse accepted an hreadbacktondabayashi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkawachinagano\":1}");
            throw new AssertionError("parse accepted an hgeomkawachinagano handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykawachinagano\":1}");
            throw new AssertionError("parse accepted an hcopykawachinagano handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkawachinagano\":1}");
            throw new AssertionError("parse accepted an hreadbackkawachinagano handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkishiwada\":1}");
            throw new AssertionError("parse accepted an hgeomkishiwada handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykishiwada\":1}");
            throw new AssertionError("parse accepted an hcopykishiwada handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkishiwada\":1}");
            throw new AssertionError("parse accepted an hreadbackkishiwada handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkaizuka\":1}");
            throw new AssertionError("parse accepted an hgeomkaizuka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykaizuka\":1}");
            throw new AssertionError("parse accepted an hcopykaizuka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkaizuka\":1}");
            throw new AssertionError("parse accepted an hreadbackkaizuka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomsennan\":1}");
            throw new AssertionError("parse accepted an hgeomsennan handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopysennan\":1}");
            throw new AssertionError("parse accepted an hcopysennan handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacksennan\":1}");
            throw new AssertionError("parse accepted an hreadbacksennan handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomhannan\":1}");
            throw new AssertionError("parse accepted an hgeomhannan handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyhannan\":1}");
            throw new AssertionError("parse accepted an hcopyhannan handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackhannan\":1}");
            throw new AssertionError("parse accepted an hreadbackhannan handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomtajiri\":1}");
            throw new AssertionError("parse accepted an hgeomtajiri handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopytajiri\":1}");
            throw new AssertionError("parse accepted an hcopytajiri handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacktajiri\":1}");
            throw new AssertionError("parse accepted an hreadbacktajiri handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkumatori\":1}");
            throw new AssertionError("parse accepted an hgeomkumatori handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykumatori\":1}");
            throw new AssertionError("parse accepted an hcopykumatori handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkumatori\":1}");
            throw new AssertionError("parse accepted an hreadbackkumatori handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomtadaoka\":1}");
            throw new AssertionError("parse accepted an hgeomtadaoka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopytadaoka\":1}");
            throw new AssertionError("parse accepted an hcopytadaoka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacktadaoka\":1}");
            throw new AssertionError("parse accepted an hreadbacktadaoka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomtaishi\":1}");
            throw new AssertionError("parse accepted an hgeomtaishi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopytaishi\":1}");
            throw new AssertionError("parse accepted an hcopytaishi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacktaishi\":1}");
            throw new AssertionError("parse accepted an hreadbacktaishi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkanan\":1}");
            throw new AssertionError("parse accepted an hgeomkanan handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykanan\":1}");
            throw new AssertionError("parse accepted an hcopykanan handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkanan\":1}");
            throw new AssertionError("parse accepted an hreadbackkanan handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomchihaya\":1}");
            throw new AssertionError("parse accepted an hgeomchihaya handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopychihaya\":1}");
            throw new AssertionError("parse accepted an hcopychihaya handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackchihaya\":1}");
            throw new AssertionError("parse accepted an hreadbackchihaya handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomyamatokoriyama\":1}");
            throw new AssertionError("parse accepted an hgeomyamatokoriyama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyyamatokoriyama\":1}");
            throw new AssertionError("parse accepted an hcopyyamatokoriyama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackyamatokoriyama\":1}");
            throw new AssertionError("parse accepted an hreadbackyamatokoriyama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkashihara\":1}");
            throw new AssertionError("parse accepted an hgeomkashihara handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykashihara\":1}");
            throw new AssertionError("parse accepted an hcopykashihara handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkashihara\":1}");
            throw new AssertionError("parse accepted an hreadbackkashihara handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomsakurai\":1}");
            throw new AssertionError("parse accepted an hgeomsakurai handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopysakurai\":1}");
            throw new AssertionError("parse accepted an hcopysakurai handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacksakurai\":1}");
            throw new AssertionError("parse accepted an hreadbacksakurai handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomgose\":1}");
            throw new AssertionError("parse accepted an hgeomgose handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopygose\":1}");
            throw new AssertionError("parse accepted an hcopygose handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackgose\":1}");
            throw new AssertionError("parse accepted an hreadbackgose handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomtenri\":1}");
            throw new AssertionError("parse accepted an hgeomtenri handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopytenri\":1}");
            throw new AssertionError("parse accepted an hcopytenri handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacktenri\":1}");
            throw new AssertionError("parse accepted an hreadbacktenri handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomikoma\":1}");
            throw new AssertionError("parse accepted an hgeomikoma handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyikoma\":1}");
            throw new AssertionError("parse accepted an hcopyikoma handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackikoma\":1}");
            throw new AssertionError("parse accepted an hreadbackikoma handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomyamatotakada\":1}");
            throw new AssertionError("parse accepted an hgeomyamatotakada handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyyamatotakada\":1}");
            throw new AssertionError("parse accepted an hcopyyamatotakada handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackyamatotakada\":1}");
            throw new AssertionError("parse accepted an hreadbackyamatotakada handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkashiba\":1}");
            throw new AssertionError("parse accepted an hgeomkashiba handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykashiba\":1}");
            throw new AssertionError("parse accepted an hcopykashiba handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkashiba\":1}");
            throw new AssertionError("parse accepted an hreadbackkashiba handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkanmaki\":1}");
            throw new AssertionError("parse accepted an hgeomkanmaki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykanmaki\":1}");
            throw new AssertionError("parse accepted an hcopykanmaki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkanmaki\":1}");
            throw new AssertionError("parse accepted an hreadbackkanmaki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomheijo\":1}");
            throw new AssertionError("parse accepted an hgeomheijo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyheijo\":1}");
            throw new AssertionError("parse accepted an hcopyheijo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackheijo\":1}");
            throw new AssertionError("parse accepted an hreadbackheijo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomsaidaiji\":1}");
            throw new AssertionError("parse accepted an hgeomsaidaiji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopysaidaiji\":1}");
            throw new AssertionError("parse accepted an hcopysaidaiji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacksaidaiji\":1}");
            throw new AssertionError("parse accepted an hreadbacksaidaiji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomtodaiji\":1}");
            throw new AssertionError("parse accepted an hgeomtodaiji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopytodaiji\":1}");
            throw new AssertionError("parse accepted an hcopytodaiji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacktodaiji\":1}");
            throw new AssertionError("parse accepted an hreadbacktodaiji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomhoryuji\":1}");
            throw new AssertionError("parse accepted an hgeomhoryuji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyhoryuji\":1}");
            throw new AssertionError("parse accepted an hcopyhoryuji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackhoryuji\":1}");
            throw new AssertionError("parse accepted an hreadbackhoryuji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomyakushiji\":1}");
            throw new AssertionError("parse accepted an hgeomyakushiji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyyakushiji\":1}");
            throw new AssertionError("parse accepted an hcopyyakushiji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackyakushiji\":1}");
            throw new AssertionError("parse accepted an hreadbackyakushiji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomtoshodaiji\":1}");
            throw new AssertionError("parse accepted an hgeomtoshodaiji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopytoshodaiji\":1}");
            throw new AssertionError("parse accepted an hcopytoshodaiji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacktoshodaiji\":1}");
            throw new AssertionError("parse accepted an hreadbacktoshodaiji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkofukuji\":1}");
            throw new AssertionError("parse accepted an hgeomkofukuji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykofukuji\":1}");
            throw new AssertionError("parse accepted an hcopykofukuji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkofukuji\":1}");
            throw new AssertionError("parse accepted an hreadbackkofukuji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkasuga\":1}");
            throw new AssertionError("parse accepted an hgeomkasuga handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykasuga\":1}");
            throw new AssertionError("parse accepted an hcopykasuga handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkasuga\":1}");
            throw new AssertionError("parse accepted an hreadbackkasuga handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomnaramachi\":1}");
            throw new AssertionError("parse accepted an hgeomnaramachi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopynaramachi\":1}");
            throw new AssertionError("parse accepted an hcopynaramachi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacknaramachi\":1}");
            throw new AssertionError("parse accepted an hreadbacknaramachi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomasuka\":1}");
            throw new AssertionError("parse accepted an hgeomasuka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyasuka\":1}");
            throw new AssertionError("parse accepted an hcopyasuka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackasuka\":1}");
            throw new AssertionError("parse accepted an hreadbackasuka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomyoshino\":1}");
            throw new AssertionError("parse accepted an hgeomyoshino handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyyoshino\":1}");
            throw new AssertionError("parse accepted an hcopyyoshino handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackyoshino\":1}");
            throw new AssertionError("parse accepted an hreadbackyoshino handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomhashimoto\":1}");
            throw new AssertionError("parse accepted an hgeomhashimoto handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyhashimoto\":1}");
            throw new AssertionError("parse accepted an hcopyhashimoto handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackhashimoto\":1}");
            throw new AssertionError("parse accepted an hreadbackhashimoto handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomikaruga\":1}");
            throw new AssertionError("parse accepted an hgeomikaruga handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyikaruga\":1}");
            throw new AssertionError("parse accepted an hcopyikaruga handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackikaruga\":1}");
            throw new AssertionError("parse accepted an hreadbackikaruga handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomchuguji\":1}");
            throw new AssertionError("parse accepted an hgeomchuguji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopychuguji\":1}");
            throw new AssertionError("parse accepted an hcopychuguji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackchuguji\":1}");
            throw new AssertionError("parse accepted an hreadbackchuguji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomhokkiji\":1}");
            throw new AssertionError("parse accepted an hgeomhokkiji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyhokkiji\":1}");
            throw new AssertionError("parse accepted an hcopyhokkiji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackhokkiji\":1}");
            throw new AssertionError("parse accepted an hreadbackhokkiji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomhorinji\":1}");
            throw new AssertionError("parse accepted an hgeomhorinji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyhorinji\":1}");
            throw new AssertionError("parse accepted an hcopyhorinji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackhorinji\":1}");
            throw new AssertionError("parse accepted an hreadbackhorinji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomjoruriji\":1}");
            throw new AssertionError("parse accepted an hgeomjoruriji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyjoruriji\":1}");
            throw new AssertionError("parse accepted an hcopyjoruriji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackjoruriji\":1}");
            throw new AssertionError("parse accepted an hreadbackjoruriji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomakishinodera\":1}");
            throw new AssertionError("parse accepted an hgeomakishinodera handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyakishinodera\":1}");
            throw new AssertionError("parse accepted an hcopyakishinodera handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackakishinodera\":1}");
            throw new AssertionError("parse accepted an hreadbackakishinodera handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomshigisan\":1}");
            throw new AssertionError("parse accepted an hgeomshigisan handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyshigisan\":1}");
            throw new AssertionError("parse accepted an hcopyshigisan handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackshigisan\":1}");
            throw new AssertionError("parse accepted an hreadbackshigisan handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomishibutai\":1}");
            throw new AssertionError("parse accepted an hgeomishibutai handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyishibutai\":1}");
            throw new AssertionError("parse accepted an hcopyishibutai handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackishibutai\":1}");
            throw new AssertionError("parse accepted an hreadbackishibutai handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkitora\":1}");
            throw new AssertionError("parse accepted an hgeomkitora handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykitora\":1}");
            throw new AssertionError("parse accepted an hcopykitora handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkitora\":1}");
            throw new AssertionError("parse accepted an hreadbackkitora handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomhasedera\":1}");
            throw new AssertionError("parse accepted an hgeomhasedera handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyhasedera\":1}");
            throw new AssertionError("parse accepted an hcopyhasedera handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackhasedera\":1}");
            throw new AssertionError("parse accepted an hreadbackhasedera handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomfujiwara\":1}");
            throw new AssertionError("parse accepted an hgeomfujiwara handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyfujiwara\":1}");
            throw new AssertionError("parse accepted an hcopyfujiwara handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackfujiwara\":1}");
            throw new AssertionError("parse accepted an hreadbackfujiwara handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeommiwa\":1}");
            throw new AssertionError("parse accepted an hgeommiwa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopymiwa\":1}");
            throw new AssertionError("parse accepted an hcopymiwa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackmiwa\":1}");
            throw new AssertionError("parse accepted an hreadbackmiwa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomtaima\":1}");
            throw new AssertionError("parse accepted an hgeomtaima handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopytaima\":1}");
            throw new AssertionError("parse accepted an hcopytaima handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacktaima\":1}");
            throw new AssertionError("parse accepted an hreadbacktaima handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeommurou\":1}");
            throw new AssertionError("parse accepted an hgeommurou handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopymurou\":1}");
            throw new AssertionError("parse accepted an hcopymurou handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackmurou\":1}");
            throw new AssertionError("parse accepted an hreadbackmurou handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomhaibara\":1}");
            throw new AssertionError("parse accepted an hgeomhaibara handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyhaibara\":1}");
            throw new AssertionError("parse accepted an hcopyhaibara handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackhaibara\":1}");
            throw new AssertionError("parse accepted an hreadbackhaibara handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomouda\":1}");
            throw new AssertionError("parse accepted an hgeomouda handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyouda\":1}");
            throw new AssertionError("parse accepted an hcopyouda handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackouda\":1}");
            throw new AssertionError("parse accepted an hreadbackouda handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomtakamado\":1}");
            throw new AssertionError("parse accepted an hgeomtakamado handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopytakamado\":1}");
            throw new AssertionError("parse accepted an hcopytakamado handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacktakamado\":1}");
            throw new AssertionError("parse accepted an hreadbacktakamado handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomwakakusa\":1}");
            throw new AssertionError("parse accepted an hgeomwakakusa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopywakakusa\":1}");
            throw new AssertionError("parse accepted an hcopywakakusa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackwakakusa\":1}");
            throw new AssertionError("parse accepted an hreadbackwakakusa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomumami\":1}");
            throw new AssertionError("parse accepted an hgeomumami handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyumami\":1}");
            throw new AssertionError("parse accepted an hcopyumami handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackumami\":1}");
            throw new AssertionError("parse accepted an hreadbackumami handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomyagyu\":1}");
            throw new AssertionError("parse accepted an hgeomyagyu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyyagyu\":1}");
            throw new AssertionError("parse accepted an hcopyyagyu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackyagyu\":1}");
            throw new AssertionError("parse accepted an hreadbackyagyu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomtsuge\":1}");
            throw new AssertionError("parse accepted an hgeomtsuge handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopytsuge\":1}");
            throw new AssertionError("parse accepted an hcopytsuge handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacktsuge\":1}");
            throw new AssertionError("parse accepted an hreadbacktsuge handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomtotsukawa\":1}");
            throw new AssertionError("parse accepted an hgeomtotsukawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopytotsukawa\":1}");
            throw new AssertionError("parse accepted an hcopytotsukawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacktotsukawa\":1}");
            throw new AssertionError("parse accepted an hreadbacktotsukawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomnosegawa\":1}");
            throw new AssertionError("parse accepted an hgeomnosegawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopynosegawa\":1}");
            throw new AssertionError("parse accepted an hcopynosegawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacknosegawa\":1}");
            throw new AssertionError("parse accepted an hreadbacknosegawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomtenkawa\":1}");
            throw new AssertionError("parse accepted an hgeomtenkawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopytenkawa\":1}");
            throw new AssertionError("parse accepted an hcopytenkawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacktenkawa\":1}");
            throw new AssertionError("parse accepted an hreadbacktenkawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkawakami\":1}");
            throw new AssertionError("parse accepted an hgeomkawakami handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykawakami\":1}");
            throw new AssertionError("parse accepted an hcopykawakami handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkawakami\":1}");
            throw new AssertionError("parse accepted an hreadbackkawakami handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkurotaki\":1}");
            throw new AssertionError("parse accepted an hgeomkurotaki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykurotaki\":1}");
            throw new AssertionError("parse accepted an hcopykurotaki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkurotaki\":1}");
            throw new AssertionError("parse accepted an hreadbackkurotaki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomnigatsudo\":1}");
            throw new AssertionError("parse accepted an hgeomnigatsudo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopynigatsudo\":1}");
            throw new AssertionError("parse accepted an hcopynigatsudo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacknigatsudo\":1}");
            throw new AssertionError("parse accepted an hreadbacknigatsudo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomtsukigase\":1}");
            throw new AssertionError("parse accepted an hgeomtsukigase handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopytsukigase\":1}");
            throw new AssertionError("parse accepted an hcopytsukigase handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacktsukigase\":1}");
            throw new AssertionError("parse accepted an hreadbacktsukigase handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomsoekami\":1}");
            throw new AssertionError("parse accepted an hgeomsoekami handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopysoekami\":1}");
            throw new AssertionError("parse accepted an hcopysoekami handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacksoekami\":1}");
            throw new AssertionError("parse accepted an hreadbacksoekami handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomhorai\":1}");
            throw new AssertionError("parse accepted an hgeomhorai handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyhorai\":1}");
            throw new AssertionError("parse accepted an hcopyhorai handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackhorai\":1}");
            throw new AssertionError("parse accepted an hreadbackhorai handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomsangatsudo\":1}");
            throw new AssertionError("parse accepted an hgeomsangatsudo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopysangatsudo\":1}");
            throw new AssertionError("parse accepted an hcopysangatsudo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacksangatsudo\":1}");
            throw new AssertionError("parse accepted an hreadbacksangatsudo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomchogakuji\":1}");
            throw new AssertionError("parse accepted an hgeomchogakuji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopychogakuji\":1}");
            throw new AssertionError("parse accepted an hcopychogakuji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackchogakuji\":1}");
            throw new AssertionError("parse accepted an hreadbackchogakuji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomabe\":1}");
            throw new AssertionError("parse accepted an hgeomabe handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyabe\":1}");
            throw new AssertionError("parse accepted an hcopyabe handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackabe\":1}");
            throw new AssertionError("parse accepted an hreadbackabe handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkamikitayama\":1}");
            throw new AssertionError("parse accepted an hgeomkamikitayama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykamikitayama\":1}");
            throw new AssertionError("parse accepted an hcopykamikitayama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkamikitayama\":1}");
            throw new AssertionError("parse accepted an hreadbackkamikitayama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomtakenouchi\":1}");
            throw new AssertionError("parse accepted an hgeomtakenouchi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopytakenouchi\":1}");
            throw new AssertionError("parse accepted an hcopytakenouchi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacktakenouchi\":1}");
            throw new AssertionError("parse accepted an hreadbacktakenouchi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomtsutsui\":1}");
            throw new AssertionError("parse accepted an hgeomtsutsui handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopytsutsui\":1}");
            throw new AssertionError("parse accepted an hcopytsutsui handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacktsutsui\":1}");
            throw new AssertionError("parse accepted an hreadbacktsutsui handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkosanji\":1}");
            throw new AssertionError("parse accepted an hgeomkosanji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykosanji\":1}");
            throw new AssertionError("parse accepted an hcopykosanji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkosanji\":1}");
            throw new AssertionError("parse accepted an hreadbackkosanji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomfutaiji\":1}");
            throw new AssertionError("parse accepted an hgeomfutaiji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyfutaiji\":1}");
            throw new AssertionError("parse accepted an hcopyfutaiji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackfutaiji\":1}");
            throw new AssertionError("parse accepted an hreadbackfutaiji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomhokkeji\":1}");
            throw new AssertionError("parse accepted an hgeomhokkeji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyhokkeji\":1}");
            throw new AssertionError("parse accepted an hcopyhokkeji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackhokkeji\":1}");
            throw new AssertionError("parse accepted an hreadbackhokkeji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomtoganoo\":1}");
            throw new AssertionError("parse accepted an hgeomtoganoo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopytoganoo\":1}");
            throw new AssertionError("parse accepted an hcopytoganoo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacktoganoo\":1}");
            throw new AssertionError("parse accepted an hreadbacktoganoo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkozanj\":1}");
            throw new AssertionError("parse accepted an hgeomkozanj handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykozanj\":1}");
            throw new AssertionError("parse accepted an hcopykozanj handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkozanj\":1}");
            throw new AssertionError("parse accepted an hreadbackkozanj handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"sourcedevice\":1}");
            throw new AssertionError("parse accepted an sourcedevice handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hwndtarget\":1}");
            throw new AssertionError("parse accepted an hwndtarget handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomjingoji\":1}");
            throw new AssertionError("parse accepted an hgeomjingoji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyjingoji\":1}");
            throw new AssertionError("parse accepted an hcopyjingoji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackjingoji\":1}");
            throw new AssertionError("parse accepted an hreadbackjingoji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomshinnyodo\":1}");
            throw new AssertionError("parse accepted an hgeomshinnyodo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyshinnyodo\":1}");
            throw new AssertionError("parse accepted an hcopyshinnyodo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackshinnyodo\":1}");
            throw new AssertionError("parse accepted an hreadbackshinnyodo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomanrakuji\":1}");
            throw new AssertionError("parse accepted an hgeomanrakuji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyanrakuji\":1}");
            throw new AssertionError("parse accepted an hcopyanrakuji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackanrakuji\":1}");
            throw new AssertionError("parse accepted an hreadbackanrakuji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomshorenin\":1}");
            throw new AssertionError("parse accepted an hgeomshorenin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyshorenin\":1}");
            throw new AssertionError("parse accepted an hcopyshorenin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackshorenin\":1}");
            throw new AssertionError("parse accepted an hreadbackshorenin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkurodani\":1}");
            throw new AssertionError("parse accepted an hgeomkurodani handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykurodani\":1}");
            throw new AssertionError("parse accepted an hcopykurodani handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkurodani\":1}");
            throw new AssertionError("parse accepted an hreadbackkurodani handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomyoshida\":1}");
            throw new AssertionError("parse accepted an hgeomyoshida handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyyoshida\":1}");
            throw new AssertionError("parse accepted an hcopyyoshida handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackyoshida\":1}");
            throw new AssertionError("parse accepted an hreadbackyoshida handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomhyakumanben\":1}");
            throw new AssertionError("parse accepted an hgeomhyakumanben handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyhyakumanben\":1}");
            throw new AssertionError("parse accepted an hcopyhyakumanben handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackhyakumanben\":1}");
            throw new AssertionError("parse accepted an hreadbackhyakumanben handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomichijoji\":1}");
            throw new AssertionError("parse accepted an hgeomichijoji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyichijoji\":1}");
            throw new AssertionError("parse accepted an hcopyichijoji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackichijoji\":1}");
            throw new AssertionError("parse accepted an hreadbackichijoji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomtakano\":1}");
            throw new AssertionError("parse accepted an hgeomtakano handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopytakano\":1}");
            throw new AssertionError("parse accepted an hcopytakano handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacktakano\":1}");
            throw new AssertionError("parse accepted an hreadbacktakano handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomraigoin\":1}");
            throw new AssertionError("parse accepted an hgeomraigoin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyraigoin\":1}");
            throw new AssertionError("parse accepted an hcopyraigoin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackraigoin\":1}");
            throw new AssertionError("parse accepted an hreadbackraigoin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkinugasa\":1}");
            throw new AssertionError("parse accepted an hgeomkinugasa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykinugasa\":1}");
            throw new AssertionError("parse accepted an hcopykinugasa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkinugasa\":1}");
            throw new AssertionError("parse accepted an hreadbackkinugasa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomatago\":1}");
            throw new AssertionError("parse accepted an hgeomatago handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyatago\":1}");
            throw new AssertionError("parse accepted an hcopyatago handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackatago\":1}");
            throw new AssertionError("parse accepted an hreadbackatago handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomiwakura\":1}");
            throw new AssertionError("parse accepted an hgeomiwakura handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyiwakura\":1}");
            throw new AssertionError("parse accepted an hcopyiwakura handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackiwakura\":1}");
            throw new AssertionError("parse accepted an hreadbackiwakura handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomosawa\":1}");
            throw new AssertionError("parse accepted an hgeomosawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyosawa\":1}");
            throw new AssertionError("parse accepted an hcopyosawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackosawa\":1}");
            throw new AssertionError("parse accepted an hreadbackosawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomhirozawa\":1}");
            throw new AssertionError("parse accepted an hgeomhirozawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyhirozawa\":1}");
            throw new AssertionError("parse accepted an hcopyhirozawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackhirozawa\":1}");
            throw new AssertionError("parse accepted an hreadbackhirozawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomnenbutsu\":1}");
            throw new AssertionError("parse accepted an hgeomnenbutsu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopynenbutsu\":1}");
            throw new AssertionError("parse accepted an hcopynenbutsu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacknenbutsu\":1}");
            throw new AssertionError("parse accepted an hreadbacknenbutsu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkiyotaki\":1}");
            throw new AssertionError("parse accepted an hgeomkiyotaki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykiyotaki\":1}");
            throw new AssertionError("parse accepted an hcopykiyotaki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkiyotaki\":1}");
            throw new AssertionError("parse accepted an hreadbackkiyotaki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomtakaragaike\":1}");
            throw new AssertionError("parse accepted an hgeomtakaragaike handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopytakaragaike\":1}");
            throw new AssertionError("parse accepted an hcopytakaragaike handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacktakaragaike\":1}");
            throw new AssertionError("parse accepted an hreadbacktakaragaike handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeommatsugasaki\":1}");
            throw new AssertionError("parse accepted an hgeommatsugasaki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopymatsugasaki\":1}");
            throw new AssertionError("parse accepted an hcopymatsugasaki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackmatsugasaki\":1}");
            throw new AssertionError("parse accepted an hreadbackmatsugasaki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomnarutaki\":1}");
            throw new AssertionError("parse accepted an hgeomnarutaki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopynarutaki\":1}");
            throw new AssertionError("parse accepted an hcopynarutaki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacknarutaki\":1}");
            throw new AssertionError("parse accepted an hreadbacknarutaki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomtsukinowa\":1}");
            throw new AssertionError("parse accepted an hgeomtsukinowa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopytsukinowa\":1}");
            throw new AssertionError("parse accepted an hcopytsukinowa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacktsukinowa\":1}");
            throw new AssertionError("parse accepted an hreadbacktsukinowa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeommakinoo\":1}");
            throw new AssertionError("parse accepted an hgeommakinoo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopymakinoo\":1}");
            throw new AssertionError("parse accepted an hcopymakinoo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackmakinoo\":1}");
            throw new AssertionError("parse accepted an hreadbackmakinoo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomutano\":1}");
            throw new AssertionError("parse accepted an hgeomutano handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyutano\":1}");
            throw new AssertionError("parse accepted an hcopyutano handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackutano\":1}");
            throw new AssertionError("parse accepted an hreadbackutano handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomhogoin\":1}");
            throw new AssertionError("parse accepted an hgeomhogoin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyhogoin\":1}");
            throw new AssertionError("parse accepted an hcopyhogoin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackhogoin\":1}");
            throw new AssertionError("parse accepted an hreadbackhogoin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkaikoji\":1}");
            throw new AssertionError("parse accepted an hgeomkaikoji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykaikoji\":1}");
            throw new AssertionError("parse accepted an hcopykaikoji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkaikoji\":1}");
            throw new AssertionError("parse accepted an hreadbackkaikoji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomjokoin\":1}");
            throw new AssertionError("parse accepted an hgeomjokoin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyjokoin\":1}");
            throw new AssertionError("parse accepted an hcopyjokoin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackjokoin\":1}");
            throw new AssertionError("parse accepted an hreadbackjokoin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomtakiguchi\":1}");
            throw new AssertionError("parse accepted an hgeomtakiguchi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopytakiguchi\":1}");
            throw new AssertionError("parse accepted an hcopytakiguchi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacktakiguchi\":1}");
            throw new AssertionError("parse accepted an hreadbacktakiguchi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomritsumeikan\":1}");
            throw new AssertionError("parse accepted an hgeomritsumeikan handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyritsumeikan\":1}");
            throw new AssertionError("parse accepted an hcopyritsumeikan handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackritsumeikan\":1}");
            throw new AssertionError("parse accepted an hreadbackritsumeikan handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkinukake\":1}");
            throw new AssertionError("parse accepted an hgeomkinukake handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykinukake\":1}");
            throw new AssertionError("parse accepted an hcopykinukake handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkinukake\":1}");
            throw new AssertionError("parse accepted an hreadbackkinukake handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomsagaden\":1}");
            throw new AssertionError("parse accepted an hgeomsagaden handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopysagaden\":1}");
            throw new AssertionError("parse accepted an hcopysagaden handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacksagaden\":1}");
            throw new AssertionError("parse accepted an hreadbacksagaden handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkumogahata\":1}");
            throw new AssertionError("parse accepted an hgeomkumogahata handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykumogahata\":1}");
            throw new AssertionError("parse accepted an hcopykumogahata handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkumogahata\":1}");
            throw new AssertionError("parse accepted an hreadbackkumogahata handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomhanase\":1}");
            throw new AssertionError("parse accepted an hgeomhanase handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyhanase\":1}");
            throw new AssertionError("parse accepted an hcopyhanase handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackhanase\":1}");
            throw new AssertionError("parse accepted an hreadbackhanase handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomhirogawara\":1}");
            throw new AssertionError("parse accepted an hgeomhirogawara handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyhirogawara\":1}");
            throw new AssertionError("parse accepted an hcopyhirogawara handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackhirogawara\":1}");
            throw new AssertionError("parse accepted an hreadbackhirogawara handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeommidorogaike\":1}");
            throw new AssertionError("parse accepted an hgeommidorogaike handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopymidorogaike\":1}");
            throw new AssertionError("parse accepted an hcopymidorogaike handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackmidorogaike\":1}");
            throw new AssertionError("parse accepted an hreadbackmidorogaike handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomseryo\":1}");
            throw new AssertionError("parse accepted an hgeomseryo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyseryo\":1}");
            throw new AssertionError("parse accepted an hcopyseryo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackseryo\":1}");
            throw new AssertionError("parse accepted an hreadbackseryo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkebera\":1}");
            throw new AssertionError("parse accepted an hgeomkebera handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykebera\":1}");
            throw new AssertionError("parse accepted an hcopykebera handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkebera\":1}");
            throw new AssertionError("parse accepted an hreadbackkebera handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomippensuji\":1}");
            throw new AssertionError("parse accepted an hgeomippensuji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyippensuji\":1}");
            throw new AssertionError("parse accepted an hcopyippensuji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackippensuji\":1}");
            throw new AssertionError("parse accepted an hreadbackippensuji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomsennyuji\":1}");
            throw new AssertionError("parse accepted an hgeomsennyuji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopysennyuji\":1}");
            throw new AssertionError("parse accepted an hcopysennyuji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacksennyuji\":1}");
            throw new AssertionError("parse accepted an hreadbacksennyuji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomrozanji\":1}");
            throw new AssertionError("parse accepted an hgeomrozanji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyrozanji\":1}");
            throw new AssertionError("parse accepted an hcopyrozanji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackrozanji\":1}");
            throw new AssertionError("parse accepted an hreadbackrozanji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkotoin\":1}");
            throw new AssertionError("parse accepted an hgeomkotoin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykotoin\":1}");
            throw new AssertionError("parse accepted an hcopykotoin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkotoin\":1}");
            throw new AssertionError("parse accepted an hreadbackkotoin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomryogenin\":1}");
            throw new AssertionError("parse accepted an hgeomryogenin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyryogenin\":1}");
            throw new AssertionError("parse accepted an hcopyryogenin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackryogenin\":1}");
            throw new AssertionError("parse accepted an hreadbackryogenin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomzuihoin\":1}");
            throw new AssertionError("parse accepted an hgeomzuihoin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyzuihoin\":1}");
            throw new AssertionError("parse accepted an hcopyzuihoin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackzuihoin\":1}");
            throw new AssertionError("parse accepted an hreadbackzuihoin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomdaisenin\":1}");
            throw new AssertionError("parse accepted an hgeomdaisenin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopydaisenin\":1}");
            throw new AssertionError("parse accepted an hcopydaisenin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackdaisenin\":1}");
            throw new AssertionError("parse accepted an hreadbackdaisenin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkodaiji\":1}");
            throw new AssertionError("parse accepted an hgeomkodaiji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykodaiji\":1}");
            throw new AssertionError("parse accepted an hcopykodaiji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkodaiji\":1}");
            throw new AssertionError("parse accepted an hreadbackkodaiji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomentokuin\":1}");
            throw new AssertionError("parse accepted an hgeomentokuin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyentokuin\":1}");
            throw new AssertionError("parse accepted an hcopyentokuin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackentokuin\":1}");
            throw new AssertionError("parse accepted an hreadbackentokuin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomgosho\":1}");
            throw new AssertionError("parse accepted an hgeomgosho handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopygosho\":1}");
            throw new AssertionError("parse accepted an hcopygosho handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackgosho\":1}");
            throw new AssertionError("parse accepted an hreadbackgosho handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomdemachiyanagi\":1}");
            throw new AssertionError("parse accepted an hgeomdemachiyanagi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopydemachiyanagi\":1}");
            throw new AssertionError("parse accepted an hcopydemachiyanagi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackdemachiyanagi\":1}");
            throw new AssertionError("parse accepted an hreadbackdemachiyanagi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomyoshimine\":1}");
            throw new AssertionError("parse accepted an hgeomyoshimine handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyyoshimine\":1}");
            throw new AssertionError("parse accepted an hcopyyoshimine handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackyoshimine\":1}");
            throw new AssertionError("parse accepted an hreadbackyoshimine handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomtaizoin\":1}");
            throw new AssertionError("parse accepted an hgeomtaizoin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopytaizoin\":1}");
            throw new AssertionError("parse accepted an hcopytaizoin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacktaizoin\":1}");
            throw new AssertionError("parse accepted an hreadbacktaizoin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomtenmangu\":1}");
            throw new AssertionError("parse accepted an hgeomtenmangu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopytenmangu\":1}");
            throw new AssertionError("parse accepted an hcopytenmangu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacktenmangu\":1}");
            throw new AssertionError("parse accepted an hreadbacktenmangu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkamishichiken\":1}");
            throw new AssertionError("parse accepted an hgeomkamishichiken handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykamishichiken\":1}");
            throw new AssertionError("parse accepted an hcopykamishichiken handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkamishichiken\":1}");
            throw new AssertionError("parse accepted an hreadbackkamishichiken handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomsenbon\":1}");
            throw new AssertionError("parse accepted an hgeomsenbon handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopysenbon\":1}");
            throw new AssertionError("parse accepted an hcopysenbon handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacksenbon\":1}");
            throw new AssertionError("parse accepted an hreadbacksenbon handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkumiyama\":1}");
            throw new AssertionError("parse accepted an hgeomkumiyama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykumiyama\":1}");
            throw new AssertionError("parse accepted an hcopykumiyama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkumiyama\":1}");
            throw new AssertionError("parse accepted an hreadbackkumiyama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomjoyo\":1}");
            throw new AssertionError("parse accepted an hgeomjoyo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyjoyo\":1}");
            throw new AssertionError("parse accepted an hcopyjoyo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackjoyo\":1}");
            throw new AssertionError("parse accepted an hreadbackjoyo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomminamiyamashiro\":1}");
            throw new AssertionError("parse accepted an hgeomminamiyamashiro handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyminamiyamashiro\":1}");
            throw new AssertionError("parse accepted an hcopyminamiyamashiro handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackminamiyamashiro\":1}");
            throw new AssertionError("parse accepted an hreadbackminamiyamashiro handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomchishakuin\":1}");
            throw new AssertionError("parse accepted an hgeomchishakuin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopychishakuin\":1}");
            throw new AssertionError("parse accepted an hcopychishakuin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackchishakuin\":1}");
            throw new AssertionError("parse accepted an hreadbackchishakuin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeommyohoin\":1}");
            throw new AssertionError("parse accepted an hgeommyohoin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopymyohoin\":1}");
            throw new AssertionError("parse accepted an hcopymyohoin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackmyohoin\":1}");
            throw new AssertionError("parse accepted an hreadbackmyohoin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomsanjusangendo\":1}");
            throw new AssertionError("parse accepted an hgeomsanjusangendo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopysanjusangendo\":1}");
            throw new AssertionError("parse accepted an hcopysanjusangendo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacksanjusangendo\":1}");
            throw new AssertionError("parse accepted an hreadbacksanjusangendo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomninenzaka\":1}");
            throw new AssertionError("parse accepted an hgeomninenzaka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyninenzaka\":1}");
            throw new AssertionError("parse accepted an hcopyninenzaka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackninenzaka\":1}");
            throw new AssertionError("parse accepted an hreadbackninenzaka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomsannenzaka\":1}");
            throw new AssertionError("parse accepted an hgeomsannenzaka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopysannenzaka\":1}");
            throw new AssertionError("parse accepted an hcopysannenzaka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacksannenzaka\":1}");
            throw new AssertionError("parse accepted an hreadbacksannenzaka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkeage\":1}");
            throw new AssertionError("parse accepted an hgeomkeage handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykeage\":1}");
            throw new AssertionError("parse accepted an hcopykeage handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkeage\":1}");
            throw new AssertionError("parse accepted an hreadbackkeage handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomtetsugaku\":1}");
            throw new AssertionError("parse accepted an hgeomtetsugaku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopytetsugaku\":1}");
            throw new AssertionError("parse accepted an hcopytetsugaku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacktetsugaku\":1}");
            throw new AssertionError("parse accepted an hreadbacktetsugaku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomshogunzuka\":1}");
            throw new AssertionError("parse accepted an hgeomshogunzuka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyshogunzuka\":1}");
            throw new AssertionError("parse accepted an hcopyshogunzuka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackshogunzuka\":1}");
            throw new AssertionError("parse accepted an hreadbackshogunzuka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomchawanzaka\":1}");
            throw new AssertionError("parse accepted an hgeomchawanzaka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopychawanzaka\":1}");
            throw new AssertionError("parse accepted an hcopychawanzaka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackchawanzaka\":1}");
            throw new AssertionError("parse accepted an hreadbackchawanzaka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomreikanji\":1}");
            throw new AssertionError("parse accepted an hgeomreikanji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyreikanji\":1}");
            throw new AssertionError("parse accepted an hcopyreikanji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackreikanji\":1}");
            throw new AssertionError("parse accepted an hreadbackreikanji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomchionji\":1}");
            throw new AssertionError("parse accepted an hgeomchionji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopychionji\":1}");
            throw new AssertionError("parse accepted an hcopychionji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackchionji\":1}");
            throw new AssertionError("parse accepted an hreadbackchionji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomnyakuoji\":1}");
            throw new AssertionError("parse accepted an hgeomnyakuoji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopynyakuoji\":1}");
            throw new AssertionError("parse accepted an hcopynyakuoji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacknyakuoji\":1}");
            throw new AssertionError("parse accepted an hreadbacknyakuoji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomshishigatani\":1}");
            throw new AssertionError("parse accepted an hgeomshishigatani handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyshishigatani\":1}");
            throw new AssertionError("parse accepted an hcopyshishigatani handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackshishigatani\":1}");
            throw new AssertionError("parse accepted an hreadbackshishigatani handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomtanukidani\":1}");
            throw new AssertionError("parse accepted an hgeomtanukidani handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopytanukidani\":1}");
            throw new AssertionError("parse accepted an hcopytanukidani handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacktanukidani\":1}");
            throw new AssertionError("parse accepted an hreadbacktanukidani handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkaguraoka\":1}");
            throw new AssertionError("parse accepted an hgeomkaguraoka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykaguraoka\":1}");
            throw new AssertionError("parse accepted an hcopykaguraoka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkaguraoka\":1}");
            throw new AssertionError("parse accepted an hreadbackkaguraoka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomjodoji\":1}");
            throw new AssertionError("parse accepted an hgeomjodoji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyjodoji\":1}");
            throw new AssertionError("parse accepted an hcopyjodoji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackjodoji\":1}");
            throw new AssertionError("parse accepted an hreadbackjodoji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomreisen\":1}");
            throw new AssertionError("parse accepted an hgeomreisen handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyreisen\":1}");
            throw new AssertionError("parse accepted an hcopyreisen handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackreisen\":1}");
            throw new AssertionError("parse accepted an hreadbackreisen handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomshogoin\":1}");
            throw new AssertionError("parse accepted an hgeomshogoin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyshogoin\":1}");
            throw new AssertionError("parse accepted an hcopyshogoin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackshogoin\":1}");
            throw new AssertionError("parse accepted an hreadbackshogoin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkonkaikomyo\":1}");
            throw new AssertionError("parse accepted an hgeomkonkaikomyo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykonkaikomyo\":1}");
            throw new AssertionError("parse accepted an hcopykonkaikomyo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkonkaikomyo\":1}");
            throw new AssertionError("parse accepted an hreadbackkonkaikomyo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomshinmonzen\":1}");
            throw new AssertionError("parse accepted an hgeomshinmonzen handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyshinmonzen\":1}");
            throw new AssertionError("parse accepted an hcopyshinmonzen handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackshinmonzen\":1}");
            throw new AssertionError("parse accepted an hreadbackshinmonzen handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomfurumonzen\":1}");
            throw new AssertionError("parse accepted an hgeomfurumonzen handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyfurumonzen\":1}");
            throw new AssertionError("parse accepted an hcopyfurumonzen handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackfurumonzen\":1}");
            throw new AssertionError("parse accepted an hreadbackfurumonzen handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomtakasegawa\":1}");
            throw new AssertionError("parse accepted an hgeomtakasegawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopytakasegawa\":1}");
            throw new AssertionError("parse accepted an hcopytakasegawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacktakasegawa\":1}");
            throw new AssertionError("parse accepted an hreadbacktakasegawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomshisendo\":1}");
            throw new AssertionError("parse accepted an hgeomshisendo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyshisendo\":1}");
            throw new AssertionError("parse accepted an hcopyshisendo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackshisendo\":1}");
            throw new AssertionError("parse accepted an hreadbackshisendo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeommanshuin\":1}");
            throw new AssertionError("parse accepted an hgeommanshuin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopymanshuin\":1}");
            throw new AssertionError("parse accepted an hcopymanshuin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackmanshuin\":1}");
            throw new AssertionError("parse accepted an hreadbackmanshuin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomenkoji\":1}");
            throw new AssertionError("parse accepted an hgeomenkoji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyenkoji\":1}");
            throw new AssertionError("parse accepted an hcopyenkoji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackenkoji\":1}");
            throw new AssertionError("parse accepted an hreadbackenkoji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomhosenin\":1}");
            throw new AssertionError("parse accepted an hgeomhosenin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyhosenin\":1}");
            throw new AssertionError("parse accepted an hcopyhosenin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackhosenin\":1}");
            throw new AssertionError("parse accepted an hreadbackhosenin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkifune\":1}");
            throw new AssertionError("parse accepted an hgeomkifune handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykifune\":1}");
            throw new AssertionError("parse accepted an hcopykifune handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkifune\":1}");
            throw new AssertionError("parse accepted an hreadbackkifune handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomshizuhara\":1}");
            throw new AssertionError("parse accepted an hgeomshizuhara handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyshizuhara\":1}");
            throw new AssertionError("parse accepted an hcopyshizuhara handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackshizuhara\":1}");
            throw new AssertionError("parse accepted an hreadbackshizuhara handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeommatsuo\":1}");
            throw new AssertionError("parse accepted an hgeommatsuo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopymatsuo\":1}");
            throw new AssertionError("parse accepted an hcopymatsuo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackmatsuo\":1}");
            throw new AssertionError("parse accepted an hreadbackmatsuo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkyotoeki\":1}");
            throw new AssertionError("parse accepted an hgeomkyotoeki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykyotoeki\":1}");
            throw new AssertionError("parse accepted an hcopykyotoeki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkyotoeki\":1}");
            throw new AssertionError("parse accepted an hreadbackkyotoeki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomhanamikoji\":1}");
            throw new AssertionError("parse accepted an hgeomhanamikoji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyhanamikoji\":1}");
            throw new AssertionError("parse accepted an hcopyhanamikoji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackhanamikoji\":1}");
            throw new AssertionError("parse accepted an hreadbackhanamikoji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeommiyagawacho\":1}");
            throw new AssertionError("parse accepted an hgeommiyagawacho handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopymiyagawacho\":1}");
            throw new AssertionError("parse accepted an hcopymiyagawacho handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackmiyagawacho\":1}");
            throw new AssertionError("parse accepted an hreadbackmiyagawacho handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomsujin\":1}");
            throw new AssertionError("parse accepted an hgeomsujin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopysujin\":1}");
            throw new AssertionError("parse accepted an hcopysujin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacksujin\":1}");
            throw new AssertionError("parse accepted an hreadbacksujin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomsento\":1}");
            throw new AssertionError("parse accepted an hgeomsento handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopysento\":1}");
            throw new AssertionError("parse accepted an hcopysento handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacksento\":1}");
            throw new AssertionError("parse accepted an hreadbacksento handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomgyoen\":1}");
            throw new AssertionError("parse accepted an hgeomgyoen handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopygyoen\":1}");
            throw new AssertionError("parse accepted an hcopygyoen handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackgyoen\":1}");
            throw new AssertionError("parse accepted an hreadbackgyoen handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomkawadoko\":1}");
            throw new AssertionError("parse accepted an hgeomkawadoko handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopykawadoko\":1}");
            throw new AssertionError("parse accepted an hcopykawadoko handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackkawadoko\":1}");
            throw new AssertionError("parse accepted an hreadbackkawadoko handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomyamazaki\":1}");
            throw new AssertionError("parse accepted an hgeomyamazaki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyyamazaki\":1}");
            throw new AssertionError("parse accepted an hcopyyamazaki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackyamazaki\":1}");
            throw new AssertionError("parse accepted an hreadbackyamazaki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomashiya\":1}");
            throw new AssertionError("parse accepted an hgeomashiya handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyashiya\":1}");
            throw new AssertionError("parse accepted an hcopyashiya handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackashiya\":1}");
            throw new AssertionError("parse accepted an hreadbackashiya handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomarima\":1}");
            throw new AssertionError("parse accepted an hgeomarima handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyarima\":1}");
            throw new AssertionError("parse accepted an hcopyarima handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackarima\":1}");
            throw new AssertionError("parse accepted an hreadbackarima handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomrokko\":1}");
            throw new AssertionError("parse accepted an hgeomrokko handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopyrokko\":1}");
            throw new AssertionError("parse accepted an hcopyrokko handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbackrokko\":1}");
            throw new AssertionError("parse accepted an hreadbackrokko handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hgeomsuma\":1}");
            throw new AssertionError("parse accepted an hgeomsuma handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hcopysuma\":1}");
            throw new AssertionError("parse accepted an hcopysuma handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            InspectorSnapshot.parse("{\"schema\":\"himari-inspector-v1\",\"hreadbacksuma\":1}");
            throw new AssertionError("parse accepted an hreadbacksuma handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
    }
}

package org.glavo.himari.render.software;

import org.glavo.himari.graphics.Color;
import org.glavo.himari.graphics.DisplayList;
import org.glavo.himari.graphics.DisplayListOp;
import org.glavo.himari.graphics.SceneEnvelope;
import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/// Verifies offline scene replay through the shipped software surface.
@NotNullByDefault
final class SceneReplayTest {
    /// Rasters a document and compares it to a direct replay.
    @Test
    void replaysCanonicalDocument() {
        SceneEnvelope envelope = new SceneEnvelope(
                SceneEnvelope.CURRENT_SCHEMA,
                8,
                8,
                new DisplayList(List.of(new DisplayListOp.FillRect(1.0f, 1.0f, 4.0f, 4.0f, Color.SRGB_WHITE)))
        );
        SoftwareSurface expected = new SoftwareSurface(8, 8);
        expected.replay(envelope.displayList());
        SoftwareSurface actual = SceneReplay.replay(envelope.toCanonicalJson());
        assertEquals(expected.width(), actual.width());
        assertEquals(expected.height(), actual.height());
        assertArrayEquals(expected.extendedLinearPremultiplied(), actual.extendedLinearPremultiplied());
    }

    /// Rejects scene documents that name producer-process handles.
    @Test
    void replayRejectsProducerHandles() {
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hwnd\":1}");
            throw new AssertionError("replay accepted a producer handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hmonitor\":1}");
            throw new AssertionError("replay accepted an hmonitor handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"vkdevice\":1}");
            throw new AssertionError("replay accepted a vkdevice handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hbitmap\":1}");
            throw new AssertionError("replay accepted an hbitmap handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hfont\":1}");
            throw new AssertionError("replay accepted an hfont handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"idxgi\":1}");
            throw new AssertionError("replay accepted an idxgi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hicon\":1}");
            throw new AssertionError("replay accepted an hicon handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"himc\":1}");
            throw new AssertionError("replay accepted an himc handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcursor\":1}");
            throw new AssertionError("replay accepted an hcursor handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hmenu\":1}");
            throw new AssertionError("replay accepted an hmenu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hbrush\":1}");
            throw new AssertionError("replay accepted an hbrush handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hrgn\":1}");
            throw new AssertionError("replay accepted an hrgn handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hpalette\":1}");
            throw new AssertionError("replay accepted an hpalette handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hpen\":1}");
            throw new AssertionError("replay accepted an hpen handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"haccel\":1}");
            throw new AssertionError("replay accepted an haccel handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hinstance\":1}");
            throw new AssertionError("replay accepted an hinstance handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hhook\":1}");
            throw new AssertionError("replay accepted an hhook handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hkl\":1}");
            throw new AssertionError("replay accepted an hkl handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hglobal\":1}");
            throw new AssertionError("replay accepted an hglobal handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hmodule\":1}");
            throw new AssertionError("replay accepted an hmodule handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hthread\":1}");
            throw new AssertionError("replay accepted an hthread handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hprocess\":1}");
            throw new AssertionError("replay accepted an hprocess handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hfile\":1}");
            throw new AssertionError("replay accepted an hfile handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hkey\":1}");
            throw new AssertionError("replay accepted an hkey handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hdrop\":1}");
            throw new AssertionError("replay accepted an hdrop handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hdcmem\":1}");
            throw new AssertionError("replay accepted an hdcmem handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hdesk\":1}");
            throw new AssertionError("replay accepted an hdesk handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"htoken\":1}");
            throw new AssertionError("replay accepted an htoken handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hmutex\":1}");
            throw new AssertionError("replay accepted an hmutex handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hevent\":1}");
            throw new AssertionError("replay accepted an hevent handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hsemaphore\":1}");
            throw new AssertionError("replay accepted an hsemaphore handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hjob\":1}");
            throw new AssertionError("replay accepted an hjob handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hwaitabletimer\":1}");
            throw new AssertionError("replay accepted an hwaitabletimer handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hsection\":1}");
            throw new AssertionError("replay accepted an hsection handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hmailslot\":1}");
            throw new AssertionError("replay accepted an hmailslot handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hprinter\":1}");
            throw new AssertionError("replay accepted an hprinter handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hpipe\":1}");
            throw new AssertionError("replay accepted an hpipe handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hglrc\":1}");
            throw new AssertionError("replay accepted an hglrc handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hscmanager\":1}");
            throw new AssertionError("replay accepted an hscmanager handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hservice\":1}");
            throw new AssertionError("replay accepted an hservice handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"htimerevent\":1}");
            throw new AssertionError("replay accepted an htimerevent handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hfind\":1}");
            throw new AssertionError("replay accepted an hfind handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hsnapshot\":1}");
            throw new AssertionError("replay accepted an hsnapshot handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hiconsm\":1}");
            throw new AssertionError("replay accepted an hiconsm handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hdesktop\":1}");
            throw new AssertionError("replay accepted an hdesktop handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"henum\":1}");
            throw new AssertionError("replay accepted an henum handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hheap\":1}");
            throw new AssertionError("replay accepted an hheap handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hwinsta\":1}");
            throw new AssertionError("replay accepted an hwinsta handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"htheme\":1}");
            throw new AssertionError("replay accepted an htheme handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"himagelist\":1}");
            throw new AssertionError("replay accepted an himagelist handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"htouchinput\":1}");
            throw new AssertionError("replay accepted an htouchinput handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hactctx\":1}");
            throw new AssertionError("replay accepted an hactctx handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hrawinput\":1}");
            throw new AssertionError("replay accepted an hrawinput handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hpowernotify\":1}");
            throw new AssertionError("replay accepted an hpowernotify handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"henhmetafile\":1}");
            throw new AssertionError("replay accepted an henhmetafile handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hdwp\":1}");
            throw new AssertionError("replay accepted an hdwp handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgesture\":1}");
            throw new AssertionError("replay accepted an hgesture handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hsynch\":1}");
            throw new AssertionError("replay accepted an hsynch handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"htransact\":1}");
            throw new AssertionError("replay accepted an htransact handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgdiobj\":1}");
            throw new AssertionError("replay accepted an hgdiobj handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hdevinfo\":1}");
            throw new AssertionError("replay accepted an hdevinfo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hdevmode\":1}");
            throw new AssertionError("replay accepted an hdevmode handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hdevnames\":1}");
            throw new AssertionError("replay accepted an hdevnames handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcolorplace\":1}");
            throw new AssertionError("replay accepted an hcolorplace handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcolortransform\":1}");
            throw new AssertionError("replay accepted an hcolortransform handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcolorspace\":1}");
            throw new AssertionError("replay accepted an hcolorspace handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hprofile\":1}");
            throw new AssertionError("replay accepted an hprofile handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hicc\":1}");
            throw new AssertionError("replay accepted an hicc handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hkernel\":1}");
            throw new AssertionError("replay accepted an hkernel handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hfilter\":1}");
            throw new AssertionError("replay accepted an hfilter handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hstream\":1}");
            throw new AssertionError("replay accepted an hstream handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hadapter\":1}");
            throw new AssertionError("replay accepted an hadapter handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hresource\":1}");
            throw new AssertionError("replay accepted an hresource handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hshader\":1}");
            throw new AssertionError("replay accepted an hshader handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hswapchain\":1}");
            throw new AssertionError("replay accepted an hswapchain handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hsurface\":1}");
            throw new AssertionError("replay accepted an hsurface handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hfence\":1}");
            throw new AssertionError("replay accepted an hfence handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hpipeline\":1}");
            throw new AssertionError("replay accepted an hpipeline handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcommandqueue\":1}");
            throw new AssertionError("replay accepted an hcommandqueue handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hrtv\":1}");
            throw new AssertionError("replay accepted an hrtv handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hdsv\":1}");
            throw new AssertionError("replay accepted an hdsv handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hsrv\":1}");
            throw new AssertionError("replay accepted an hsrv handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"huav\":1}");
            throw new AssertionError("replay accepted an huav handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcbv\":1}");
            throw new AssertionError("replay accepted an hcbv handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hsampler\":1}");
            throw new AssertionError("replay accepted an hsampler handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hblob\":1}");
            throw new AssertionError("replay accepted an hblob handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hquery\":1}");
            throw new AssertionError("replay accepted an hquery handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hroot\":1}");
            throw new AssertionError("replay accepted an hroot handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hsignature\":1}");
            throw new AssertionError("replay accepted an hsignature handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hmesh\":1}");
            throw new AssertionError("replay accepted an hmesh handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hacceleration\":1}");
            throw new AssertionError("replay accepted an hacceleration handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hlibrary\":1}");
            throw new AssertionError("replay accepted an hlibrary handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcommandallocator\":1}");
            throw new AssertionError("replay accepted an hcommandallocator handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcommandlist\":1}");
            throw new AssertionError("replay accepted an hcommandlist handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hvertexbuffer\":1}");
            throw new AssertionError("replay accepted an hvertexbuffer handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hindexbuffer\":1}");
            throw new AssertionError("replay accepted an hindexbuffer handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"htexture\":1}");
            throw new AssertionError("replay accepted an htexture handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hbundle\":1}");
            throw new AssertionError("replay accepted an hbundle handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hconstantbuffer\":1}");
            throw new AssertionError("replay accepted an hconstantbuffer handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hdepthstencil\":1}");
            throw new AssertionError("replay accepted an hdepthstencil handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hrendertarget\":1}");
            throw new AssertionError("replay accepted an hrendertarget handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hindirectargs\":1}");
            throw new AssertionError("replay accepted an hindirectargs handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"huploadbuffer\":1}");
            throw new AssertionError("replay accepted an huploadbuffer handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackbuffer\":1}");
            throw new AssertionError("replay accepted an hreadbackbuffer handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hstagingbuffer\":1}");
            throw new AssertionError("replay accepted an hstagingbuffer handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hscratchbuffer\":1}");
            throw new AssertionError("replay accepted an hscratchbuffer handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyqueue\":1}");
            throw new AssertionError("replay accepted an hcopyqueue handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hpresentable\":1}");
            throw new AssertionError("replay accepted an hpresentable handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hbindgroup\":1}");
            throw new AssertionError("replay accepted an hbindgroup handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hframegraph\":1}");
            throw new AssertionError("replay accepted an hframegraph handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hbindless\":1}");
            throw new AssertionError("replay accepted an hbindless handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcomputepass\":1}");
            throw new AssertionError("replay accepted an hcomputepass handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hpresentqueue\":1}");
            throw new AssertionError("replay accepted an hpresentqueue handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hrenderpass\":1}");
            throw new AssertionError("replay accepted an hrenderpass handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcomputequeue\":1}");
            throw new AssertionError("replay accepted an hcomputequeue handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hindirectbuffer\":1}");
            throw new AssertionError("replay accepted an hindirectbuffer handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgraphicsqueue\":1}");
            throw new AssertionError("replay accepted an hgraphicsqueue handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"htransferqueue\":1}");
            throw new AssertionError("replay accepted an htransferqueue handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hbindtable\":1}");
            throw new AssertionError("replay accepted an hbindtable handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcommandpool\":1}");
            throw new AssertionError("replay accepted an hcommandpool handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hframebuffer\":1}");
            throw new AssertionError("replay accepted an hframebuffer handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hpushconstant\":1}");
            throw new AssertionError("replay accepted an hpushconstant handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hdrawindirect\":1}");
            throw new AssertionError("replay accepted an hdrawindirect handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hdispatch\":1}");
            throw new AssertionError("replay accepted an hdispatch handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hblitcmd\":1}");
            throw new AssertionError("replay accepted an hblitcmd handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyengine\":1}");
            throw new AssertionError("replay accepted an hcopyengine handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hdrawargs\":1}");
            throw new AssertionError("replay accepted an hdrawargs handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hblitqueue\":1}");
            throw new AssertionError("replay accepted an hblitqueue handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hindirectcount\":1}");
            throw new AssertionError("replay accepted an hindirectcount handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hblas\":1}");
            throw new AssertionError("replay accepted an hblas handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"htlas\":1}");
            throw new AssertionError("replay accepted an htlas handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hrtas\":1}");
            throw new AssertionError("replay accepted an hrtas handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeompass\":1}");
            throw new AssertionError("replay accepted an hgeompass handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcomputeindirect\":1}");
            throw new AssertionError("replay accepted an hcomputeindirect handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hbuildargs\":1}");
            throw new AssertionError("replay accepted an hbuildargs handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcompaction\":1}");
            throw new AssertionError("replay accepted an hcompaction handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomcmd\":1}");
            throw new AssertionError("replay accepted an hgeomcmd handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyargs\":1}");
            throw new AssertionError("replay accepted an hcopyargs handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomstate\":1}");
            throw new AssertionError("replay accepted an hgeomstate handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hbuildqueue\":1}");
            throw new AssertionError("replay accepted an hbuildqueue handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomargs\":1}");
            throw new AssertionError("replay accepted an hgeomargs handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hbuildpass\":1}");
            throw new AssertionError("replay accepted an hbuildpass handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hsyncargs\":1}");
            throw new AssertionError("replay accepted an hsyncargs handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomcache\":1}");
            throw new AssertionError("replay accepted an hgeomcache handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hsyncqueue\":1}");
            throw new AssertionError("replay accepted an hsyncqueue handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"huploadargs\":1}");
            throw new AssertionError("replay accepted an huploadargs handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomset\":1}");
            throw new AssertionError("replay accepted an hgeomset handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyset\":1}");
            throw new AssertionError("replay accepted an hcopyset handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackargs\":1}");
            throw new AssertionError("replay accepted an hreadbackargs handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomlist\":1}");
            throw new AssertionError("replay accepted an hgeomlist handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopylist\":1}");
            throw new AssertionError("replay accepted an hcopylist handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackset\":1}");
            throw new AssertionError("replay accepted an hreadbackset handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeombind\":1}");
            throw new AssertionError("replay accepted an hgeombind handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopybind\":1}");
            throw new AssertionError("replay accepted an hcopybind handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackcmd\":1}");
            throw new AssertionError("replay accepted an hreadbackcmd handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomview\":1}");
            throw new AssertionError("replay accepted an hgeomview handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyview\":1}");
            throw new AssertionError("replay accepted an hcopyview handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackview\":1}");
            throw new AssertionError("replay accepted an hreadbackview handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomdraw\":1}");
            throw new AssertionError("replay accepted an hgeomdraw handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopydraw\":1}");
            throw new AssertionError("replay accepted an hcopydraw handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackdraw\":1}");
            throw new AssertionError("replay accepted an hreadbackdraw handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeombatch\":1}");
            throw new AssertionError("replay accepted an hgeombatch handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopybatch\":1}");
            throw new AssertionError("replay accepted an hcopybatch handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackbatch\":1}");
            throw new AssertionError("replay accepted an hreadbackbatch handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomprim\":1}");
            throw new AssertionError("replay accepted an hgeomprim handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyprim\":1}");
            throw new AssertionError("replay accepted an hcopyprim handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackprim\":1}");
            throw new AssertionError("replay accepted an hreadbackprim handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomwork\":1}");
            throw new AssertionError("replay accepted an hgeomwork handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopywork\":1}");
            throw new AssertionError("replay accepted an hcopywork handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackwork\":1}");
            throw new AssertionError("replay accepted an hreadbackwork handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomsink\":1}");
            throw new AssertionError("replay accepted an hgeomsink handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopysink\":1}");
            throw new AssertionError("replay accepted an hcopysink handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacksink\":1}");
            throw new AssertionError("replay accepted an hreadbacksink handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomtile\":1}");
            throw new AssertionError("replay accepted an hgeomtile handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopytile\":1}");
            throw new AssertionError("replay accepted an hcopytile handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacktile\":1}");
            throw new AssertionError("replay accepted an hreadbacktile handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomquad\":1}");
            throw new AssertionError("replay accepted an hgeomquad handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyquad\":1}");
            throw new AssertionError("replay accepted an hcopyquad handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackquad\":1}");
            throw new AssertionError("replay accepted an hreadbackquad handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomcube\":1}");
            throw new AssertionError("replay accepted an hgeomcube handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopycube\":1}");
            throw new AssertionError("replay accepted an hcopycube handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackcube\":1}");
            throw new AssertionError("replay accepted an hreadbackcube handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomslab\":1}");
            throw new AssertionError("replay accepted an hgeomslab handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyslab\":1}");
            throw new AssertionError("replay accepted an hcopyslab handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackslab\":1}");
            throw new AssertionError("replay accepted an hreadbackslab handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomhull\":1}");
            throw new AssertionError("replay accepted an hgeomhull handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyhull\":1}");
            throw new AssertionError("replay accepted an hcopyhull handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackhull\":1}");
            throw new AssertionError("replay accepted an hreadbackhull handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomring\":1}");
            throw new AssertionError("replay accepted an hgeomring handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyring\":1}");
            throw new AssertionError("replay accepted an hcopyring handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackring\":1}");
            throw new AssertionError("replay accepted an hreadbackring handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomloop\":1}");
            throw new AssertionError("replay accepted an hgeomloop handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyloop\":1}");
            throw new AssertionError("replay accepted an hcopyloop handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackloop\":1}");
            throw new AssertionError("replay accepted an hreadbackloop handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomfan\":1}");
            throw new AssertionError("replay accepted an hgeomfan handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyfan\":1}");
            throw new AssertionError("replay accepted an hcopyfan handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackfan\":1}");
            throw new AssertionError("replay accepted an hreadbackfan handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomarc\":1}");
            throw new AssertionError("replay accepted an hgeomarc handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyarc\":1}");
            throw new AssertionError("replay accepted an hcopyarc handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackarc\":1}");
            throw new AssertionError("replay accepted an hreadbackarc handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomcap\":1}");
            throw new AssertionError("replay accepted an hgeomcap handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopycap\":1}");
            throw new AssertionError("replay accepted an hcopycap handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackcap\":1}");
            throw new AssertionError("replay accepted an hreadbackcap handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeombend\":1}");
            throw new AssertionError("replay accepted an hgeombend handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopybend\":1}");
            throw new AssertionError("replay accepted an hcopybend handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackbend\":1}");
            throw new AssertionError("replay accepted an hreadbackbend handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomjoin\":1}");
            throw new AssertionError("replay accepted an hgeomjoin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyjoin\":1}");
            throw new AssertionError("replay accepted an hcopyjoin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackjoin\":1}");
            throw new AssertionError("replay accepted an hreadbackjoin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomsplice\":1}");
            throw new AssertionError("replay accepted an hgeomsplice handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopysplice\":1}");
            throw new AssertionError("replay accepted an hcopysplice handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacksplice\":1}");
            throw new AssertionError("replay accepted an hreadbacksplice handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomweld\":1}");
            throw new AssertionError("replay accepted an hgeomweld handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyweld\":1}");
            throw new AssertionError("replay accepted an hcopyweld handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackweld\":1}");
            throw new AssertionError("replay accepted an hreadbackweld handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomseam\":1}");
            throw new AssertionError("replay accepted an hgeomseam handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyseam\":1}");
            throw new AssertionError("replay accepted an hcopyseam handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackseam\":1}");
            throw new AssertionError("replay accepted an hreadbackseam handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeommiter\":1}");
            throw new AssertionError("replay accepted an hgeommiter handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopymiter\":1}");
            throw new AssertionError("replay accepted an hcopymiter handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackmiter\":1}");
            throw new AssertionError("replay accepted an hreadbackmiter handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeombevel\":1}");
            throw new AssertionError("replay accepted an hgeombevel handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopybevel\":1}");
            throw new AssertionError("replay accepted an hcopybevel handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackbevel\":1}");
            throw new AssertionError("replay accepted an hreadbackbevel handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomfillet\":1}");
            throw new AssertionError("replay accepted an hgeomfillet handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyfillet\":1}");
            throw new AssertionError("replay accepted an hcopyfillet handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackfillet\":1}");
            throw new AssertionError("replay accepted an hreadbackfillet handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomnotch\":1}");
            throw new AssertionError("replay accepted an hgeomnotch handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopynotch\":1}");
            throw new AssertionError("replay accepted an hcopynotch handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacknotch\":1}");
            throw new AssertionError("replay accepted an hreadbacknotch handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkerf\":1}");
            throw new AssertionError("replay accepted an hgeomkerf handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykerf\":1}");
            throw new AssertionError("replay accepted an hcopykerf handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkerf\":1}");
            throw new AssertionError("replay accepted an hreadbackkerf handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomdado\":1}");
            throw new AssertionError("replay accepted an hgeomdado handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopydado\":1}");
            throw new AssertionError("replay accepted an hcopydado handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackdado\":1}");
            throw new AssertionError("replay accepted an hreadbackdado handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomrabbet\":1}");
            throw new AssertionError("replay accepted an hgeomrabbet handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyrabbet\":1}");
            throw new AssertionError("replay accepted an hcopyrabbet handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackrabbet\":1}");
            throw new AssertionError("replay accepted an hreadbackrabbet handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomtenon\":1}");
            throw new AssertionError("replay accepted an hgeomtenon handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopytenon\":1}");
            throw new AssertionError("replay accepted an hcopytenon handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacktenon\":1}");
            throw new AssertionError("replay accepted an hreadbacktenon handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeommortise\":1}");
            throw new AssertionError("replay accepted an hgeommortise handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopymortise\":1}");
            throw new AssertionError("replay accepted an hcopymortise handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackmortise\":1}");
            throw new AssertionError("replay accepted an hreadbackmortise handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomdovetail\":1}");
            throw new AssertionError("replay accepted an hgeomdovetail handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopydovetail\":1}");
            throw new AssertionError("replay accepted an hcopydovetail handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackdovetail\":1}");
            throw new AssertionError("replay accepted an hreadbackdovetail handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomscarf\":1}");
            throw new AssertionError("replay accepted an hgeomscarf handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyscarf\":1}");
            throw new AssertionError("replay accepted an hcopyscarf handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackscarf\":1}");
            throw new AssertionError("replay accepted an hreadbackscarf handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomspline\":1}");
            throw new AssertionError("replay accepted an hgeomspline handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyspline\":1}");
            throw new AssertionError("replay accepted an hcopyspline handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackspline\":1}");
            throw new AssertionError("replay accepted an hreadbackspline handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeombiscuit\":1}");
            throw new AssertionError("replay accepted an hgeombiscuit handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopybiscuit\":1}");
            throw new AssertionError("replay accepted an hcopybiscuit handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackbiscuit\":1}");
            throw new AssertionError("replay accepted an hreadbackbiscuit handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomdowel\":1}");
            throw new AssertionError("replay accepted an hgeomdowel handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopydowel\":1}");
            throw new AssertionError("replay accepted an hcopydowel handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackdowel\":1}");
            throw new AssertionError("replay accepted an hreadbackdowel handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomwedge\":1}");
            throw new AssertionError("replay accepted an hgeomwedge handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopywedge\":1}");
            throw new AssertionError("replay accepted an hcopywedge handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackwedge\":1}");
            throw new AssertionError("replay accepted an hreadbackwedge handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomhaunch\":1}");
            throw new AssertionError("replay accepted an hgeomhaunch handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyhaunch\":1}");
            throw new AssertionError("replay accepted an hcopyhaunch handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackhaunch\":1}");
            throw new AssertionError("replay accepted an hreadbackhaunch handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomjoggle\":1}");
            throw new AssertionError("replay accepted an hgeomjoggle handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyjoggle\":1}");
            throw new AssertionError("replay accepted an hcopyjoggle handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackjoggle\":1}");
            throw new AssertionError("replay accepted an hreadbackjoggle handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeombridle\":1}");
            throw new AssertionError("replay accepted an hgeombridle handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopybridle\":1}");
            throw new AssertionError("replay accepted an hcopybridle handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackbridle\":1}");
            throw new AssertionError("replay accepted an hreadbackbridle handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomsplay\":1}");
            throw new AssertionError("replay accepted an hgeomsplay handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopysplay\":1}");
            throw new AssertionError("replay accepted an hcopysplay handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacksplay\":1}");
            throw new AssertionError("replay accepted an hreadbacksplay handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomlap\":1}");
            throw new AssertionError("replay accepted an hgeomlap handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopylap\":1}");
            throw new AssertionError("replay accepted an hcopylap handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacklap\":1}");
            throw new AssertionError("replay accepted an hreadbacklap handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomsnip\":1}");
            throw new AssertionError("replay accepted an hgeomsnip handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopysnip\":1}");
            throw new AssertionError("replay accepted an hcopysnip handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacksnip\":1}");
            throw new AssertionError("replay accepted an hreadbacksnip handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomchamfer\":1}");
            throw new AssertionError("replay accepted an hgeomchamfer handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopychamfer\":1}");
            throw new AssertionError("replay accepted an hcopychamfer handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackchamfer\":1}");
            throw new AssertionError("replay accepted an hreadbackchamfer handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomburr\":1}");
            throw new AssertionError("replay accepted an hgeomburr handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyburr\":1}");
            throw new AssertionError("replay accepted an hcopyburr handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackburr\":1}");
            throw new AssertionError("replay accepted an hreadbackburr handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomrebate\":1}");
            throw new AssertionError("replay accepted an hgeomrebate handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyrebate\":1}");
            throw new AssertionError("replay accepted an hcopyrebate handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackrebate\":1}");
            throw new AssertionError("replay accepted an hreadbackrebate handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomhousing\":1}");
            throw new AssertionError("replay accepted an hgeomhousing handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyhousing\":1}");
            throw new AssertionError("replay accepted an hcopyhousing handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackhousing\":1}");
            throw new AssertionError("replay accepted an hreadbackhousing handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomtongue\":1}");
            throw new AssertionError("replay accepted an hgeomtongue handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopytongue\":1}");
            throw new AssertionError("replay accepted an hcopytongue handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacktongue\":1}");
            throw new AssertionError("replay accepted an hreadbacktongue handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomshim\":1}");
            throw new AssertionError("replay accepted an hgeomshim handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyshim\":1}");
            throw new AssertionError("replay accepted an hcopyshim handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackshim\":1}");
            throw new AssertionError("replay accepted an hreadbackshim handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomcleat\":1}");
            throw new AssertionError("replay accepted an hgeomcleat handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopycleat\":1}");
            throw new AssertionError("replay accepted an hcopycleat handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackcleat\":1}");
            throw new AssertionError("replay accepted an hreadbackcleat handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomfinger\":1}");
            throw new AssertionError("replay accepted an hgeomfinger handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyfinger\":1}");
            throw new AssertionError("replay accepted an hcopyfinger handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackfinger\":1}");
            throw new AssertionError("replay accepted an hreadbackfinger handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomgroove\":1}");
            throw new AssertionError("replay accepted an hgeomgroove handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopygroove\":1}");
            throw new AssertionError("replay accepted an hcopygroove handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackgroove\":1}");
            throw new AssertionError("replay accepted an hreadbackgroove handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomplow\":1}");
            throw new AssertionError("replay accepted an hgeomplow handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyplow\":1}");
            throw new AssertionError("replay accepted an hcopyplow handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackplow\":1}");
            throw new AssertionError("replay accepted an hreadbackplow handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomfeather\":1}");
            throw new AssertionError("replay accepted an hgeomfeather handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyfeather\":1}");
            throw new AssertionError("replay accepted an hcopyfeather handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackfeather\":1}");
            throw new AssertionError("replay accepted an hreadbackfeather handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomsplint\":1}");
            throw new AssertionError("replay accepted an hgeomsplint handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopysplint\":1}");
            throw new AssertionError("replay accepted an hcopysplint handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacksplint\":1}");
            throw new AssertionError("replay accepted an hreadbacksplint handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomwhittle\":1}");
            throw new AssertionError("replay accepted an hgeomwhittle handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopywhittle\":1}");
            throw new AssertionError("replay accepted an hcopywhittle handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackwhittle\":1}");
            throw new AssertionError("replay accepted an hreadbackwhittle handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomgraft\":1}");
            throw new AssertionError("replay accepted an hgeomgraft handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopygraft\":1}");
            throw new AssertionError("replay accepted an hcopygraft handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackgraft\":1}");
            throw new AssertionError("replay accepted an hreadbackgraft handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomflute\":1}");
            throw new AssertionError("replay accepted an hgeomflute handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyflute\":1}");
            throw new AssertionError("replay accepted an hcopyflute handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackflute\":1}");
            throw new AssertionError("replay accepted an hreadbackflute handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomscribe\":1}");
            throw new AssertionError("replay accepted an hgeomscribe handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyscribe\":1}");
            throw new AssertionError("replay accepted an hcopyscribe handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackscribe\":1}");
            throw new AssertionError("replay accepted an hreadbackscribe handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomscore\":1}");
            throw new AssertionError("replay accepted an hgeomscore handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyscore\":1}");
            throw new AssertionError("replay accepted an hcopyscore handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackscore\":1}");
            throw new AssertionError("replay accepted an hreadbackscore handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomchase\":1}");
            throw new AssertionError("replay accepted an hgeomchase handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopychase\":1}");
            throw new AssertionError("replay accepted an hcopychase handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackchase\":1}");
            throw new AssertionError("replay accepted an hreadbackchase handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomincise\":1}");
            throw new AssertionError("replay accepted an hgeomincise handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyincise\":1}");
            throw new AssertionError("replay accepted an hcopyincise handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackincise\":1}");
            throw new AssertionError("replay accepted an hreadbackincise handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomgash\":1}");
            throw new AssertionError("replay accepted an hgeomgash handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopygash\":1}");
            throw new AssertionError("replay accepted an hcopygash handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackgash\":1}");
            throw new AssertionError("replay accepted an hreadbackgash handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomcarve\":1}");
            throw new AssertionError("replay accepted an hgeomcarve handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopycarve\":1}");
            throw new AssertionError("replay accepted an hcopycarve handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackcarve\":1}");
            throw new AssertionError("replay accepted an hreadbackcarve handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeometch\":1}");
            throw new AssertionError("replay accepted an hgeometch handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyetch\":1}");
            throw new AssertionError("replay accepted an hcopyetch handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacketch\":1}");
            throw new AssertionError("replay accepted an hreadbacketch handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomincuse\":1}");
            throw new AssertionError("replay accepted an hgeomincuse handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyincuse\":1}");
            throw new AssertionError("replay accepted an hcopyincuse handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackincuse\":1}");
            throw new AssertionError("replay accepted an hreadbackincuse handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomengrave\":1}");
            throw new AssertionError("replay accepted an hgeomengrave handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyengrave\":1}");
            throw new AssertionError("replay accepted an hcopyengrave handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackengrave\":1}");
            throw new AssertionError("replay accepted an hreadbackengrave handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomintaglio\":1}");
            throw new AssertionError("replay accepted an hgeomintaglio handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyintaglio\":1}");
            throw new AssertionError("replay accepted an hcopyintaglio handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackintaglio\":1}");
            throw new AssertionError("replay accepted an hreadbackintaglio handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomrepousse\":1}");
            throw new AssertionError("replay accepted an hgeomrepousse handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyrepousse\":1}");
            throw new AssertionError("replay accepted an hcopyrepousse handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackrepousse\":1}");
            throw new AssertionError("replay accepted an hreadbackrepousse handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomfiligree\":1}");
            throw new AssertionError("replay accepted an hgeomfiligree handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyfiligree\":1}");
            throw new AssertionError("replay accepted an hcopyfiligree handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackfiligree\":1}");
            throw new AssertionError("replay accepted an hreadbackfiligree handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomniello\":1}");
            throw new AssertionError("replay accepted an hgeomniello handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyniello\":1}");
            throw new AssertionError("replay accepted an hcopyniello handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackniello\":1}");
            throw new AssertionError("replay accepted an hreadbackniello handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomchampleve\":1}");
            throw new AssertionError("replay accepted an hgeomchampleve handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopychampleve\":1}");
            throw new AssertionError("replay accepted an hcopychampleve handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackchampleve\":1}");
            throw new AssertionError("replay accepted an hreadbackchampleve handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomcloisonne\":1}");
            throw new AssertionError("replay accepted an hgeomcloisonne handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopycloisonne\":1}");
            throw new AssertionError("replay accepted an hcopycloisonne handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackcloisonne\":1}");
            throw new AssertionError("replay accepted an hreadbackcloisonne handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomdamascene\":1}");
            throw new AssertionError("replay accepted an hgeomdamascene handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopydamascene\":1}");
            throw new AssertionError("replay accepted an hcopydamascene handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackdamascene\":1}");
            throw new AssertionError("replay accepted an hreadbackdamascene handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomvermeil\":1}");
            throw new AssertionError("replay accepted an hgeomvermeil handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyvermeil\":1}");
            throw new AssertionError("replay accepted an hcopyvermeil handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackvermeil\":1}");
            throw new AssertionError("replay accepted an hreadbackvermeil handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomormolu\":1}");
            throw new AssertionError("replay accepted an hgeomormolu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyormolu\":1}");
            throw new AssertionError("replay accepted an hcopyormolu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackormolu\":1}");
            throw new AssertionError("replay accepted an hreadbackormolu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomparcel\":1}");
            throw new AssertionError("replay accepted an hgeomparcel handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyparcel\":1}");
            throw new AssertionError("replay accepted an hcopyparcel handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackparcel\":1}");
            throw new AssertionError("replay accepted an hreadbackparcel handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeominlay\":1}");
            throw new AssertionError("replay accepted an hgeominlay handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyinlay\":1}");
            throw new AssertionError("replay accepted an hcopyinlay handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackinlay\":1}");
            throw new AssertionError("replay accepted an hreadbackinlay handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeommakie\":1}");
            throw new AssertionError("replay accepted an hgeommakie handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopymakie\":1}");
            throw new AssertionError("replay accepted an hcopymakie handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackmakie\":1}");
            throw new AssertionError("replay accepted an hreadbackmakie handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomurushi\":1}");
            throw new AssertionError("replay accepted an hgeomurushi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyurushi\":1}");
            throw new AssertionError("replay accepted an hcopyurushi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackurushi\":1}");
            throw new AssertionError("replay accepted an hreadbackurushi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomgilt\":1}");
            throw new AssertionError("replay accepted an hgeomgilt handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopygilt\":1}");
            throw new AssertionError("replay accepted an hcopygilt handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackgilt\":1}");
            throw new AssertionError("replay accepted an hreadbackgilt handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkoftgari\":1}");
            throw new AssertionError("replay accepted an hgeomkoftgari handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykoftgari\":1}");
            throw new AssertionError("replay accepted an hcopykoftgari handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkoftgari\":1}");
            throw new AssertionError("replay accepted an hreadbackkoftgari handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomzogan\":1}");
            throw new AssertionError("replay accepted an hgeomzogan handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyzogan\":1}");
            throw new AssertionError("replay accepted an hcopyzogan handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackzogan\":1}");
            throw new AssertionError("replay accepted an hreadbackzogan handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomnunome\":1}");
            throw new AssertionError("replay accepted an hgeomnunome handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopynunome\":1}");
            throw new AssertionError("replay accepted an hcopynunome handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacknunome\":1}");
            throw new AssertionError("replay accepted an hreadbacknunome handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomlacquer\":1}");
            throw new AssertionError("replay accepted an hgeomlacquer handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopylacquer\":1}");
            throw new AssertionError("replay accepted an hcopylacquer handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacklacquer\":1}");
            throw new AssertionError("replay accepted an hreadbacklacquer handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomdamask\":1}");
            throw new AssertionError("replay accepted an hgeomdamask handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopydamask\":1}");
            throw new AssertionError("replay accepted an hcopydamask handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackdamask\":1}");
            throw new AssertionError("replay accepted an hreadbackdamask handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkintsugi\":1}");
            throw new AssertionError("replay accepted an hgeomkintsugi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykintsugi\":1}");
            throw new AssertionError("replay accepted an hcopykintsugi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkintsugi\":1}");
            throw new AssertionError("replay accepted an hreadbackkintsugi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomshippo\":1}");
            throw new AssertionError("replay accepted an hgeomshippo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyshippo\":1}");
            throw new AssertionError("replay accepted an hcopyshippo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackshippo\":1}");
            throw new AssertionError("replay accepted an hreadbackshippo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomraku\":1}");
            throw new AssertionError("replay accepted an hgeomraku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyraku\":1}");
            throw new AssertionError("replay accepted an hcopyraku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackraku\":1}");
            throw new AssertionError("replay accepted an hreadbackraku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomoribe\":1}");
            throw new AssertionError("replay accepted an hgeomoribe handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyoribe\":1}");
            throw new AssertionError("replay accepted an hcopyoribe handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackoribe\":1}");
            throw new AssertionError("replay accepted an hreadbackoribe handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkutani\":1}");
            throw new AssertionError("replay accepted an hgeomkutani handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykutani\":1}");
            throw new AssertionError("replay accepted an hcopykutani handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkutani\":1}");
            throw new AssertionError("replay accepted an hreadbackkutani handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomsatsuma\":1}");
            throw new AssertionError("replay accepted an hgeomsatsuma handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopysatsuma\":1}");
            throw new AssertionError("replay accepted an hcopysatsuma handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacksatsuma\":1}");
            throw new AssertionError("replay accepted an hreadbacksatsuma handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomshino\":1}");
            throw new AssertionError("replay accepted an hgeomshino handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyshino\":1}");
            throw new AssertionError("replay accepted an hcopyshino handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackshino\":1}");
            throw new AssertionError("replay accepted an hreadbackshino handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkaratsu\":1}");
            throw new AssertionError("replay accepted an hgeomkaratsu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykaratsu\":1}");
            throw new AssertionError("replay accepted an hcopykaratsu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkaratsu\":1}");
            throw new AssertionError("replay accepted an hreadbackkaratsu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeombizen\":1}");
            throw new AssertionError("replay accepted an hgeombizen handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopybizen\":1}");
            throw new AssertionError("replay accepted an hcopybizen handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackbizen\":1}");
            throw new AssertionError("replay accepted an hreadbackbizen handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomhagi\":1}");
            throw new AssertionError("replay accepted an hgeomhagi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyhagi\":1}");
            throw new AssertionError("replay accepted an hcopyhagi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackhagi\":1}");
            throw new AssertionError("replay accepted an hreadbackhagi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomshigaraki\":1}");
            throw new AssertionError("replay accepted an hgeomshigaraki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyshigaraki\":1}");
            throw new AssertionError("replay accepted an hcopyshigaraki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackshigaraki\":1}");
            throw new AssertionError("replay accepted an hreadbackshigaraki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomtanba\":1}");
            throw new AssertionError("replay accepted an hgeomtanba handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopytanba\":1}");
            throw new AssertionError("replay accepted an hcopytanba handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacktanba\":1}");
            throw new AssertionError("replay accepted an hreadbacktanba handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomtokoname\":1}");
            throw new AssertionError("replay accepted an hgeomtokoname handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopytokoname\":1}");
            throw new AssertionError("replay accepted an hcopytokoname handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacktokoname\":1}");
            throw new AssertionError("replay accepted an hreadbacktokoname handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomechizen\":1}");
            throw new AssertionError("replay accepted an hgeomechizen handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyechizen\":1}");
            throw new AssertionError("replay accepted an hcopyechizen handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackechizen\":1}");
            throw new AssertionError("replay accepted an hreadbackechizen handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeommashiko\":1}");
            throw new AssertionError("replay accepted an hgeommashiko handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopymashiko\":1}");
            throw new AssertionError("replay accepted an hcopymashiko handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackmashiko\":1}");
            throw new AssertionError("replay accepted an hreadbackmashiko handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkasama\":1}");
            throw new AssertionError("replay accepted an hgeomkasama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykasama\":1}");
            throw new AssertionError("replay accepted an hcopykasama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkasama\":1}");
            throw new AssertionError("replay accepted an hreadbackkasama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomarita\":1}");
            throw new AssertionError("replay accepted an hgeomarita handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyarita\":1}");
            throw new AssertionError("replay accepted an hcopyarita handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackarita\":1}");
            throw new AssertionError("replay accepted an hreadbackarita handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomimari\":1}");
            throw new AssertionError("replay accepted an hgeomimari handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyimari\":1}");
            throw new AssertionError("replay accepted an hcopyimari handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackimari\":1}");
            throw new AssertionError("replay accepted an hreadbackimari handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeombanko\":1}");
            throw new AssertionError("replay accepted an hgeombanko handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopybanko\":1}");
            throw new AssertionError("replay accepted an hcopybanko handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackbanko\":1}");
            throw new AssertionError("replay accepted an hreadbackbanko handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomontay\":1}");
            throw new AssertionError("replay accepted an hgeomontay handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyontay\":1}");
            throw new AssertionError("replay accepted an hcopyontay handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackontay\":1}");
            throw new AssertionError("replay accepted an hreadbackontay handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomtsuboya\":1}");
            throw new AssertionError("replay accepted an hgeomtsuboya handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopytsuboya\":1}");
            throw new AssertionError("replay accepted an hcopytsuboya handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacktsuboya\":1}");
            throw new AssertionError("replay accepted an hreadbacktsuboya handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomhasami\":1}");
            throw new AssertionError("replay accepted an hgeomhasami handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyhasami\":1}");
            throw new AssertionError("replay accepted an hcopyhasami handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackhasami\":1}");
            throw new AssertionError("replay accepted an hreadbackhasami handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomagano\":1}");
            throw new AssertionError("replay accepted an hgeomagano handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyagano\":1}");
            throw new AssertionError("replay accepted an hcopyagano handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackagano\":1}");
            throw new AssertionError("replay accepted an hreadbackagano handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomtakatori\":1}");
            throw new AssertionError("replay accepted an hgeomtakatori handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopytakatori\":1}");
            throw new AssertionError("replay accepted an hcopytakatori handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacktakatori\":1}");
            throw new AssertionError("replay accepted an hreadbacktakatori handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomnabeshima\":1}");
            throw new AssertionError("replay accepted an hgeomnabeshima handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopynabeshima\":1}");
            throw new AssertionError("replay accepted an hcopynabeshima handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacknabeshima\":1}");
            throw new AssertionError("replay accepted an hreadbacknabeshima handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkakiemon\":1}");
            throw new AssertionError("replay accepted an hgeomkakiemon handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykakiemon\":1}");
            throw new AssertionError("replay accepted an hcopykakiemon handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkakiemon\":1}");
            throw new AssertionError("replay accepted an hreadbackkakiemon handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomiroe\":1}");
            throw new AssertionError("replay accepted an hgeomiroe handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyiroe\":1}");
            throw new AssertionError("replay accepted an hcopyiroe handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackiroe\":1}");
            throw new AssertionError("replay accepted an hreadbackiroe handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomsometsuke\":1}");
            throw new AssertionError("replay accepted an hgeomsometsuke handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopysometsuke\":1}");
            throw new AssertionError("replay accepted an hcopysometsuke handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacksometsuke\":1}");
            throw new AssertionError("replay accepted an hreadbacksometsuke handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomtenmoku\":1}");
            throw new AssertionError("replay accepted an hgeomtenmoku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopytenmoku\":1}");
            throw new AssertionError("replay accepted an hcopytenmoku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacktenmoku\":1}");
            throw new AssertionError("replay accepted an hreadbacktenmoku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomyohen\":1}");
            throw new AssertionError("replay accepted an hgeomyohen handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyyohen\":1}");
            throw new AssertionError("replay accepted an hcopyyohen handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackyohen\":1}");
            throw new AssertionError("replay accepted an hreadbackyohen handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomjian\":1}");
            throw new AssertionError("replay accepted an hgeomjian handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyjian\":1}");
            throw new AssertionError("replay accepted an hcopyjian handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackjian\":1}");
            throw new AssertionError("replay accepted an hreadbackjian handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkoishiwara\":1}");
            throw new AssertionError("replay accepted an hgeomkoishiwara handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykoishiwara\":1}");
            throw new AssertionError("replay accepted an hcopykoishiwara handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkoishiwara\":1}");
            throw new AssertionError("replay accepted an hreadbackkoishiwara handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeommikawachi\":1}");
            throw new AssertionError("replay accepted an hgeommikawachi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopymikawachi\":1}");
            throw new AssertionError("replay accepted an hcopymikawachi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackmikawachi\":1}");
            throw new AssertionError("replay accepted an hreadbackmikawachi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomiwami\":1}");
            throw new AssertionError("replay accepted an hgeomiwami handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyiwami\":1}");
            throw new AssertionError("replay accepted an hcopyiwami handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackiwami\":1}");
            throw new AssertionError("replay accepted an hreadbackiwami handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomotani\":1}");
            throw new AssertionError("replay accepted an hgeomotani handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyotani\":1}");
            throw new AssertionError("replay accepted an hcopyotani handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackotani\":1}");
            throw new AssertionError("replay accepted an hreadbackotani handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomyomitan\":1}");
            throw new AssertionError("replay accepted an hgeomyomitan handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyyomitan\":1}");
            throw new AssertionError("replay accepted an hcopyyomitan handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackyomitan\":1}");
            throw new AssertionError("replay accepted an hreadbackyomitan handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomiga\":1}");
            throw new AssertionError("replay accepted an hgeomiga handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyiga\":1}");
            throw new AssertionError("replay accepted an hcopyiga handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackiga\":1}");
            throw new AssertionError("replay accepted an hreadbackiga handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeommino\":1}");
            throw new AssertionError("replay accepted an hgeommino handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopymino\":1}");
            throw new AssertionError("replay accepted an hcopymino handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackmino\":1}");
            throw new AssertionError("replay accepted an hreadbackmino handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomtamba\":1}");
            throw new AssertionError("replay accepted an hgeomtamba handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopytamba\":1}");
            throw new AssertionError("replay accepted an hcopytamba handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacktamba\":1}");
            throw new AssertionError("replay accepted an hreadbacktamba handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomyokkaichi\":1}");
            throw new AssertionError("replay accepted an hgeomyokkaichi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyyokkaichi\":1}");
            throw new AssertionError("replay accepted an hcopyyokkaichi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackyokkaichi\":1}");
            throw new AssertionError("replay accepted an hreadbackyokkaichi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomamakusa\":1}");
            throw new AssertionError("replay accepted an hgeomamakusa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyamakusa\":1}");
            throw new AssertionError("replay accepted an hcopyamakusa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackamakusa\":1}");
            throw new AssertionError("replay accepted an hreadbackamakusa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomizumiyama\":1}");
            throw new AssertionError("replay accepted an hgeomizumiyama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyizumiyama\":1}");
            throw new AssertionError("replay accepted an hcopyizumiyama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackizumiyama\":1}");
            throw new AssertionError("replay accepted an hreadbackizumiyama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomtetsuyu\":1}");
            throw new AssertionError("replay accepted an hgeomtetsuyu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopytetsuyu\":1}");
            throw new AssertionError("replay accepted an hcopytetsuyu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacktetsuyu\":1}");
            throw new AssertionError("replay accepted an hreadbacktetsuyu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomnoborigama\":1}");
            throw new AssertionError("replay accepted an hgeomnoborigama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopynoborigama\":1}");
            throw new AssertionError("replay accepted an hcopynoborigama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacknoborigama\":1}");
            throw new AssertionError("replay accepted an hreadbacknoborigama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomanagama\":1}");
            throw new AssertionError("replay accepted an hgeomanagama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyanagama\":1}");
            throw new AssertionError("replay accepted an hcopyanagama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackanagama\":1}");
            throw new AssertionError("replay accepted an hreadbackanagama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomtokikabe\":1}");
            throw new AssertionError("replay accepted an hgeomtokikabe handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopytokikabe\":1}");
            throw new AssertionError("replay accepted an hcopytokikabe handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacktokikabe\":1}");
            throw new AssertionError("replay accepted an hreadbacktokikabe handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomshodai\":1}");
            throw new AssertionError("replay accepted an hgeomshodai handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyshodai\":1}");
            throw new AssertionError("replay accepted an hcopyshodai handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackshodai\":1}");
            throw new AssertionError("replay accepted an hreadbackshodai handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomyatsushiro\":1}");
            throw new AssertionError("replay accepted an hgeomyatsushiro handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyyatsushiro\":1}");
            throw new AssertionError("replay accepted an hcopyyatsushiro handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackyatsushiro\":1}");
            throw new AssertionError("replay accepted an hreadbackyatsushiro handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkiyomizu\":1}");
            throw new AssertionError("replay accepted an hgeomkiyomizu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykiyomizu\":1}");
            throw new AssertionError("replay accepted an hcopykiyomizu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkiyomizu\":1}");
            throw new AssertionError("replay accepted an hreadbackkiyomizu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomasahi\":1}");
            throw new AssertionError("replay accepted an hgeomasahi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyasahi\":1}");
            throw new AssertionError("replay accepted an hcopyasahi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackasahi\":1}");
            throw new AssertionError("replay accepted an hreadbackasahi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomakahada\":1}");
            throw new AssertionError("replay accepted an hgeomakahada handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyakahada\":1}");
            throw new AssertionError("replay accepted an hcopyakahada handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackakahada\":1}");
            throw new AssertionError("replay accepted an hreadbackakahada handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomtachikui\":1}");
            throw new AssertionError("replay accepted an hgeomtachikui handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopytachikui\":1}");
            throw new AssertionError("replay accepted an hcopytachikui handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacktachikui\":1}");
            throw new AssertionError("replay accepted an hreadbacktachikui handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeommumyoi\":1}");
            throw new AssertionError("replay accepted an hgeommumyoi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopymumyoi\":1}");
            throw new AssertionError("replay accepted an hcopymumyoi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackmumyoi\":1}");
            throw new AssertionError("replay accepted an hreadbackmumyoi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomizushi\":1}");
            throw new AssertionError("replay accepted an hgeomizushi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyizushi\":1}");
            throw new AssertionError("replay accepted an hcopyizushi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackizushi\":1}");
            throw new AssertionError("replay accepted an hreadbackizushi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkokuji\":1}");
            throw new AssertionError("replay accepted an hgeomkokuji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykokuji\":1}");
            throw new AssertionError("replay accepted an hcopykokuji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkokuji\":1}");
            throw new AssertionError("replay accepted an hreadbackkokuji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomsuzu\":1}");
            throw new AssertionError("replay accepted an hgeomsuzu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopysuzu\":1}");
            throw new AssertionError("replay accepted an hcopysuzu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacksuzu\":1}");
            throw new AssertionError("replay accepted an hreadbacksuzu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomnaraoka\":1}");
            throw new AssertionError("replay accepted an hgeomnaraoka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopynaraoka\":1}");
            throw new AssertionError("replay accepted an hcopynaraoka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacknaraoka\":1}");
            throw new AssertionError("replay accepted an hreadbacknaraoka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomtobe\":1}");
            throw new AssertionError("replay accepted an hgeomtobe handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopytobe\":1}");
            throw new AssertionError("replay accepted an hcopytobe handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacktobe\":1}");
            throw new AssertionError("replay accepted an hreadbacktobe handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomtanegashima\":1}");
            throw new AssertionError("replay accepted an hgeomtanegashima handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopytanegashima\":1}");
            throw new AssertionError("replay accepted an hcopytanegashima handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacktanegashima\":1}");
            throw new AssertionError("replay accepted an hreadbacktanegashima handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomzeshin\":1}");
            throw new AssertionError("replay accepted an hgeomzeshin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyzeshin\":1}");
            throw new AssertionError("replay accepted an hcopyzeshin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackzeshin\":1}");
            throw new AssertionError("replay accepted an hreadbackzeshin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkyoho\":1}");
            throw new AssertionError("replay accepted an hgeomkyoho handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykyoho\":1}");
            throw new AssertionError("replay accepted an hcopykyoho handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkyoho\":1}");
            throw new AssertionError("replay accepted an hreadbackkyoho handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomotsu\":1}");
            throw new AssertionError("replay accepted an hgeomotsu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyotsu\":1}");
            throw new AssertionError("replay accepted an hcopyotsu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackotsu\":1}");
            throw new AssertionError("replay accepted an hreadbackotsu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomaizuhongo\":1}");
            throw new AssertionError("replay accepted an hgeomaizuhongo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyaizuhongo\":1}");
            throw new AssertionError("replay accepted an hcopyaizuhongo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackaizuhongo\":1}");
            throw new AssertionError("replay accepted an hreadbackaizuhongo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkanazawa\":1}");
            throw new AssertionError("replay accepted an hgeomkanazawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykanazawa\":1}");
            throw new AssertionError("replay accepted an hcopykanazawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkanazawa\":1}");
            throw new AssertionError("replay accepted an hreadbackkanazawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomogama\":1}");
            throw new AssertionError("replay accepted an hgeomogama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyogama\":1}");
            throw new AssertionError("replay accepted an hcopyogama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackogama\":1}");
            throw new AssertionError("replay accepted an hreadbackogama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomwaritake\":1}");
            throw new AssertionError("replay accepted an hgeomwaritake handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopywaritake\":1}");
            throw new AssertionError("replay accepted an hcopywaritake handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackwaritake\":1}");
            throw new AssertionError("replay accepted an hreadbackwaritake handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkameyama\":1}");
            throw new AssertionError("replay accepted an hgeomkameyama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykameyama\":1}");
            throw new AssertionError("replay accepted an hcopykameyama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkameyama\":1}");
            throw new AssertionError("replay accepted an hreadbackkameyama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomhizen\":1}");
            throw new AssertionError("replay accepted an hgeomhizen handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyhizen\":1}");
            throw new AssertionError("replay accepted an hcopyhizen handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackhizen\":1}");
            throw new AssertionError("replay accepted an hreadbackhizen handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomfukakusa\":1}");
            throw new AssertionError("replay accepted an hgeomfukakusa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyfukakusa\":1}");
            throw new AssertionError("replay accepted an hcopyfukakusa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackfukakusa\":1}");
            throw new AssertionError("replay accepted an hreadbackfukakusa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomawaji\":1}");
            throw new AssertionError("replay accepted an hgeomawaji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyawaji\":1}");
            throw new AssertionError("replay accepted an hcopyawaji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackawaji\":1}");
            throw new AssertionError("replay accepted an hreadbackawaji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomsoma\":1}");
            throw new AssertionError("replay accepted an hgeomsoma handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopysoma\":1}");
            throw new AssertionError("replay accepted an hcopysoma handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacksoma\":1}");
            throw new AssertionError("replay accepted an hreadbacksoma handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomawata\":1}");
            throw new AssertionError("replay accepted an hgeomawata handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyawata\":1}");
            throw new AssertionError("replay accepted an hcopyawata handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackawata\":1}");
            throw new AssertionError("replay accepted an hreadbackawata handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomseikanji\":1}");
            throw new AssertionError("replay accepted an hgeomseikanji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyseikanji\":1}");
            throw new AssertionError("replay accepted an hcopyseikanji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackseikanji\":1}");
            throw new AssertionError("replay accepted an hreadbackseikanji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomomuro\":1}");
            throw new AssertionError("replay accepted an hgeomomuro handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyomuro\":1}");
            throw new AssertionError("replay accepted an hcopyomuro handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackomuro\":1}");
            throw new AssertionError("replay accepted an hreadbackomuro handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomninsei\":1}");
            throw new AssertionError("replay accepted an hgeomninsei handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyninsei\":1}");
            throw new AssertionError("replay accepted an hcopyninsei handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackninsei\":1}");
            throw new AssertionError("replay accepted an hreadbackninsei handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkenzan\":1}");
            throw new AssertionError("replay accepted an hgeomkenzan handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykenzan\":1}");
            throw new AssertionError("replay accepted an hcopykenzan handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkenzan\":1}");
            throw new AssertionError("replay accepted an hreadbackkenzan handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomdobachi\":1}");
            throw new AssertionError("replay accepted an hgeomdobachi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopydobachi\":1}");
            throw new AssertionError("replay accepted an hcopydobachi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackdobachi\":1}");
            throw new AssertionError("replay accepted an hreadbackdobachi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkinkozan\":1}");
            throw new AssertionError("replay accepted an hgeomkinkozan handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykinkozan\":1}");
            throw new AssertionError("replay accepted an hcopykinkozan handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkinkozan\":1}");
            throw new AssertionError("replay accepted an hreadbackkinkozan handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomyasaka\":1}");
            throw new AssertionError("replay accepted an hgeomyasaka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyyasaka\":1}");
            throw new AssertionError("replay accepted an hcopyyasaka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackyasaka\":1}");
            throw new AssertionError("replay accepted an hreadbackyasaka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomotowa\":1}");
            throw new AssertionError("replay accepted an hgeomotowa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyotowa\":1}");
            throw new AssertionError("replay accepted an hcopyotowa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackotowa\":1}");
            throw new AssertionError("replay accepted an hreadbackotowa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomgojozaka\":1}");
            throw new AssertionError("replay accepted an hgeomgojozaka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopygojozaka\":1}");
            throw new AssertionError("replay accepted an hcopygojozaka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackgojozaka\":1}");
            throw new AssertionError("replay accepted an hreadbackgojozaka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomrokuhara\":1}");
            throw new AssertionError("replay accepted an hgeomrokuhara handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyrokuhara\":1}");
            throw new AssertionError("replay accepted an hcopyrokuhara handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackrokuhara\":1}");
            throw new AssertionError("replay accepted an hreadbackrokuhara handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomfushimi\":1}");
            throw new AssertionError("replay accepted an hgeomfushimi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyfushimi\":1}");
            throw new AssertionError("replay accepted an hcopyfushimi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackfushimi\":1}");
            throw new AssertionError("replay accepted an hreadbackfushimi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkennin\":1}");
            throw new AssertionError("replay accepted an hgeomkennin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykennin\":1}");
            throw new AssertionError("replay accepted an hcopykennin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkennin\":1}");
            throw new AssertionError("replay accepted an hreadbackkennin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomnanzen\":1}");
            throw new AssertionError("replay accepted an hgeomnanzen handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopynanzen\":1}");
            throw new AssertionError("replay accepted an hcopynanzen handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacknanzen\":1}");
            throw new AssertionError("replay accepted an hreadbacknanzen handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomginkaku\":1}");
            throw new AssertionError("replay accepted an hgeomginkaku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyginkaku\":1}");
            throw new AssertionError("replay accepted an hcopyginkaku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackginkaku\":1}");
            throw new AssertionError("replay accepted an hreadbackginkaku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkinkaku\":1}");
            throw new AssertionError("replay accepted an hgeomkinkaku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykinkaku\":1}");
            throw new AssertionError("replay accepted an hcopykinkaku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkinkaku\":1}");
            throw new AssertionError("replay accepted an hreadbackkinkaku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomtofuku\":1}");
            throw new AssertionError("replay accepted an hgeomtofuku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopytofuku\":1}");
            throw new AssertionError("replay accepted an hcopytofuku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacktofuku\":1}");
            throw new AssertionError("replay accepted an hreadbacktofuku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeombyodoin\":1}");
            throw new AssertionError("replay accepted an hgeombyodoin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopybyodoin\":1}");
            throw new AssertionError("replay accepted an hcopybyodoin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackbyodoin\":1}");
            throw new AssertionError("replay accepted an hreadbackbyodoin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomninnaji\":1}");
            throw new AssertionError("replay accepted an hgeomninnaji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyninnaji\":1}");
            throw new AssertionError("replay accepted an hcopyninnaji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackninnaji\":1}");
            throw new AssertionError("replay accepted an hreadbackninnaji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomdaigo\":1}");
            throw new AssertionError("replay accepted an hgeomdaigo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopydaigo\":1}");
            throw new AssertionError("replay accepted an hcopydaigo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackdaigo\":1}");
            throw new AssertionError("replay accepted an hreadbackdaigo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomseiryo\":1}");
            throw new AssertionError("replay accepted an hgeomseiryo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyseiryo\":1}");
            throw new AssertionError("replay accepted an hcopyseiryo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackseiryo\":1}");
            throw new AssertionError("replay accepted an hreadbackseiryo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomtenryu\":1}");
            throw new AssertionError("replay accepted an hgeomtenryu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopytenryu\":1}");
            throw new AssertionError("replay accepted an hcopytenryu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacktenryu\":1}");
            throw new AssertionError("replay accepted an hreadbacktenryu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomsaiho\":1}");
            throw new AssertionError("replay accepted an hgeomsaiho handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopysaiho\":1}");
            throw new AssertionError("replay accepted an hcopysaiho handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacksaiho\":1}");
            throw new AssertionError("replay accepted an hreadbacksaiho handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomryoan\":1}");
            throw new AssertionError("replay accepted an hgeomryoan handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyryoan\":1}");
            throw new AssertionError("replay accepted an hcopyryoan handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackryoan\":1}");
            throw new AssertionError("replay accepted an hreadbackryoan handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomdaitoku\":1}");
            throw new AssertionError("replay accepted an hgeomdaitoku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopydaitoku\":1}");
            throw new AssertionError("replay accepted an hcopydaitoku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackdaitoku\":1}");
            throw new AssertionError("replay accepted an hreadbackdaitoku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeommyoshin\":1}");
            throw new AssertionError("replay accepted an hgeommyoshin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopymyoshin\":1}");
            throw new AssertionError("replay accepted an hcopymyoshin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackmyoshin\":1}");
            throw new AssertionError("replay accepted an hreadbackmyoshin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomshokoku\":1}");
            throw new AssertionError("replay accepted an hgeomshokoku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyshokoku\":1}");
            throw new AssertionError("replay accepted an hcopyshokoku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackshokoku\":1}");
            throw new AssertionError("replay accepted an hreadbackshokoku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomnijo\":1}");
            throw new AssertionError("replay accepted an hgeomnijo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopynijo\":1}");
            throw new AssertionError("replay accepted an hcopynijo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacknijo\":1}");
            throw new AssertionError("replay accepted an hreadbacknijo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkatsura\":1}");
            throw new AssertionError("replay accepted an hgeomkatsura handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykatsura\":1}");
            throw new AssertionError("replay accepted an hcopykatsura handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkatsura\":1}");
            throw new AssertionError("replay accepted an hreadbackkatsura handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomshugaku\":1}");
            throw new AssertionError("replay accepted an hgeomshugaku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyshugaku\":1}");
            throw new AssertionError("replay accepted an hcopyshugaku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackshugaku\":1}");
            throw new AssertionError("replay accepted an hreadbackshugaku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkamigamo\":1}");
            throw new AssertionError("replay accepted an hgeomkamigamo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykamigamo\":1}");
            throw new AssertionError("replay accepted an hcopykamigamo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkamigamo\":1}");
            throw new AssertionError("replay accepted an hreadbackkamigamo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomhonen\":1}");
            throw new AssertionError("replay accepted an hgeomhonen handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyhonen\":1}");
            throw new AssertionError("replay accepted an hcopyhonen handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackhonen\":1}");
            throw new AssertionError("replay accepted an hreadbackhonen handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomchionin\":1}");
            throw new AssertionError("replay accepted an hgeomchionin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopychionin\":1}");
            throw new AssertionError("replay accepted an hcopychionin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackchionin\":1}");
            throw new AssertionError("replay accepted an hreadbackchionin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomeikando\":1}");
            throw new AssertionError("replay accepted an hgeomeikando handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyeikando\":1}");
            throw new AssertionError("replay accepted an hcopyeikando handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackeikando\":1}");
            throw new AssertionError("replay accepted an hreadbackeikando handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeommanpuku\":1}");
            throw new AssertionError("replay accepted an hgeommanpuku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopymanpuku\":1}");
            throw new AssertionError("replay accepted an hcopymanpuku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackmanpuku\":1}");
            throw new AssertionError("replay accepted an hreadbackmanpuku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkosho\":1}");
            throw new AssertionError("replay accepted an hgeomkosho handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykosho\":1}");
            throw new AssertionError("replay accepted an hcopykosho handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkosho\":1}");
            throw new AssertionError("replay accepted an hreadbackkosho handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomtoji\":1}");
            throw new AssertionError("replay accepted an hgeomtoji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopytoji\":1}");
            throw new AssertionError("replay accepted an hcopytoji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacktoji\":1}");
            throw new AssertionError("replay accepted an hreadbacktoji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomnishi\":1}");
            throw new AssertionError("replay accepted an hgeomnishi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopynishi\":1}");
            throw new AssertionError("replay accepted an hcopynishi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacknishi\":1}");
            throw new AssertionError("replay accepted an hreadbacknishi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomhigashi\":1}");
            throw new AssertionError("replay accepted an hgeomhigashi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyhigashi\":1}");
            throw new AssertionError("replay accepted an hcopyhigashi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackhigashi\":1}");
            throw new AssertionError("replay accepted an hreadbackhigashi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkurama\":1}");
            throw new AssertionError("replay accepted an hgeomkurama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykurama\":1}");
            throw new AssertionError("replay accepted an hcopykurama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkurama\":1}");
            throw new AssertionError("replay accepted an hreadbackkurama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkibune\":1}");
            throw new AssertionError("replay accepted an hgeomkibune handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykibune\":1}");
            throw new AssertionError("replay accepted an hcopykibune handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkibune\":1}");
            throw new AssertionError("replay accepted an hreadbackkibune handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomdaikaku\":1}");
            throw new AssertionError("replay accepted an hgeomdaikaku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopydaikaku\":1}");
            throw new AssertionError("replay accepted an hcopydaikaku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackdaikaku\":1}");
            throw new AssertionError("replay accepted an hreadbackdaikaku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomgion\":1}");
            throw new AssertionError("replay accepted an hgeomgion handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopygion\":1}");
            throw new AssertionError("replay accepted an hcopygion handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackgion\":1}");
            throw new AssertionError("replay accepted an hreadbackgion handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeommaruyama\":1}");
            throw new AssertionError("replay accepted an hgeommaruyama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopymaruyama\":1}");
            throw new AssertionError("replay accepted an hcopymaruyama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackmaruyama\":1}");
            throw new AssertionError("replay accepted an hreadbackmaruyama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomheian\":1}");
            throw new AssertionError("replay accepted an hgeomheian handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyheian\":1}");
            throw new AssertionError("replay accepted an hcopyheian handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackheian\":1}");
            throw new AssertionError("replay accepted an hreadbackheian handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomokazaki\":1}");
            throw new AssertionError("replay accepted an hgeomokazaki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyokazaki\":1}");
            throw new AssertionError("replay accepted an hcopyokazaki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackokazaki\":1}");
            throw new AssertionError("replay accepted an hreadbackokazaki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomshirakawa\":1}");
            throw new AssertionError("replay accepted an hgeomshirakawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyshirakawa\":1}");
            throw new AssertionError("replay accepted an hcopyshirakawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackshirakawa\":1}");
            throw new AssertionError("replay accepted an hreadbackshirakawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeominari\":1}");
            throw new AssertionError("replay accepted an hgeominari handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyinari\":1}");
            throw new AssertionError("replay accepted an hcopyinari handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackinari\":1}");
            throw new AssertionError("replay accepted an hreadbackinari handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomarashiyama\":1}");
            throw new AssertionError("replay accepted an hgeomarashiyama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyarashiyama\":1}");
            throw new AssertionError("replay accepted an hcopyarashiyama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackarashiyama\":1}");
            throw new AssertionError("replay accepted an hreadbackarashiyama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomsagano\":1}");
            throw new AssertionError("replay accepted an hgeomsagano handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopysagano\":1}");
            throw new AssertionError("replay accepted an hcopysagano handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacksagano\":1}");
            throw new AssertionError("replay accepted an hreadbacksagano handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomadashino\":1}");
            throw new AssertionError("replay accepted an hgeomadashino handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyadashino\":1}");
            throw new AssertionError("replay accepted an hcopyadashino handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackadashino\":1}");
            throw new AssertionError("replay accepted an hreadbackadashino handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomohara\":1}");
            throw new AssertionError("replay accepted an hgeomohara handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyohara\":1}");
            throw new AssertionError("replay accepted an hcopyohara handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackohara\":1}");
            throw new AssertionError("replay accepted an hreadbackohara handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomsanzen\":1}");
            throw new AssertionError("replay accepted an hgeomsanzen handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopysanzen\":1}");
            throw new AssertionError("replay accepted an hcopysanzen handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacksanzen\":1}");
            throw new AssertionError("replay accepted an hreadbacksanzen handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomjakko\":1}");
            throw new AssertionError("replay accepted an hgeomjakko handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyjakko\":1}");
            throw new AssertionError("replay accepted an hcopyjakko handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackjakko\":1}");
            throw new AssertionError("replay accepted an hreadbackjakko handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomgiou\":1}");
            throw new AssertionError("replay accepted an hgeomgiou handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopygiou\":1}");
            throw new AssertionError("replay accepted an hcopygiou handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackgiou\":1}");
            throw new AssertionError("replay accepted an hreadbackgiou handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomnisonin\":1}");
            throw new AssertionError("replay accepted an hgeomnisonin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopynisonin\":1}");
            throw new AssertionError("replay accepted an hcopynisonin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacknisonin\":1}");
            throw new AssertionError("replay accepted an hreadbacknisonin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomtakao\":1}");
            throw new AssertionError("replay accepted an hgeomtakao handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopytakao\":1}");
            throw new AssertionError("replay accepted an hcopytakao handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacktakao\":1}");
            throw new AssertionError("replay accepted an hreadbacktakao handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeommizuo\":1}");
            throw new AssertionError("replay accepted an hgeommizuo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopymizuo\":1}");
            throw new AssertionError("replay accepted an hcopymizuo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackmizuo\":1}");
            throw new AssertionError("replay accepted an hreadbackmizuo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomumegahata\":1}");
            throw new AssertionError("replay accepted an hgeomumegahata handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyumegahata\":1}");
            throw new AssertionError("replay accepted an hcopyumegahata handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackumegahata\":1}");
            throw new AssertionError("replay accepted an hreadbackumegahata handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomhiei\":1}");
            throw new AssertionError("replay accepted an hgeomhiei handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyhiei\":1}");
            throw new AssertionError("replay accepted an hcopyhiei handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackhiei\":1}");
            throw new AssertionError("replay accepted an hreadbackhiei handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomenryaku\":1}");
            throw new AssertionError("replay accepted an hgeomenryaku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyenryaku\":1}");
            throw new AssertionError("replay accepted an hcopyenryaku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackenryaku\":1}");
            throw new AssertionError("replay accepted an hreadbackenryaku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomyokawa\":1}");
            throw new AssertionError("replay accepted an hgeomyokawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyyokawa\":1}");
            throw new AssertionError("replay accepted an hcopyyokawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackyokawa\":1}");
            throw new AssertionError("replay accepted an hreadbackyokawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomtodoin\":1}");
            throw new AssertionError("replay accepted an hgeomtodoin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopytodoin\":1}");
            throw new AssertionError("replay accepted an hcopytodoin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacktodoin\":1}");
            throw new AssertionError("replay accepted an hreadbacktodoin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomsakamoto\":1}");
            throw new AssertionError("replay accepted an hgeomsakamoto handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopysakamoto\":1}");
            throw new AssertionError("replay accepted an hcopysakamoto handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacksakamoto\":1}");
            throw new AssertionError("replay accepted an hreadbacksakamoto handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeommudouji\":1}");
            throw new AssertionError("replay accepted an hgeommudouji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopymudouji\":1}");
            throw new AssertionError("replay accepted an hcopymudouji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackmudouji\":1}");
            throw new AssertionError("replay accepted an hreadbackmudouji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomshaka\":1}");
            throw new AssertionError("replay accepted an hgeomshaka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyshaka\":1}");
            throw new AssertionError("replay accepted an hcopyshaka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackshaka\":1}");
            throw new AssertionError("replay accepted an hreadbackshaka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkonpon\":1}");
            throw new AssertionError("replay accepted an hgeomkonpon handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykonpon\":1}");
            throw new AssertionError("replay accepted an hcopykonpon handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkonpon\":1}");
            throw new AssertionError("replay accepted an hreadbackkonpon handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomjodoin\":1}");
            throw new AssertionError("replay accepted an hgeomjodoin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyjodoin\":1}");
            throw new AssertionError("replay accepted an hcopyjodoin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackjodoin\":1}");
            throw new AssertionError("replay accepted an hreadbackjodoin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkaidanin\":1}");
            throw new AssertionError("replay accepted an hgeomkaidanin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykaidanin\":1}");
            throw new AssertionError("replay accepted an hcopykaidanin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkaidanin\":1}");
            throw new AssertionError("replay accepted an hreadbackkaidanin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomsaito\":1}");
            throw new AssertionError("replay accepted an hgeomsaito handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopysaito\":1}");
            throw new AssertionError("replay accepted an hcopysaito handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacksaito\":1}");
            throw new AssertionError("replay accepted an hreadbacksaito handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomishiyama\":1}");
            throw new AssertionError("replay accepted an hgeomishiyama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyishiyama\":1}");
            throw new AssertionError("replay accepted an hcopyishiyama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackishiyama\":1}");
            throw new AssertionError("replay accepted an hreadbackishiyama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeommiidera\":1}");
            throw new AssertionError("replay accepted an hgeommiidera handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopymiidera\":1}");
            throw new AssertionError("replay accepted an hcopymiidera handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackmiidera\":1}");
            throw new AssertionError("replay accepted an hreadbackmiidera handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeombanna\":1}");
            throw new AssertionError("replay accepted an hgeombanna handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopybanna\":1}");
            throw new AssertionError("replay accepted an hcopybanna handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackbanna\":1}");
            throw new AssertionError("replay accepted an hreadbackbanna handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomonin\":1}");
            throw new AssertionError("replay accepted an hgeomonin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyonin\":1}");
            throw new AssertionError("replay accepted an hcopyonin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackonin\":1}");
            throw new AssertionError("replay accepted an hreadbackonin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomzeze\":1}");
            throw new AssertionError("replay accepted an hgeomzeze handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyzeze\":1}");
            throw new AssertionError("replay accepted an hcopyzeze handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackzeze\":1}");
            throw new AssertionError("replay accepted an hreadbackzeze handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkatata\":1}");
            throw new AssertionError("replay accepted an hgeomkatata handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykatata\":1}");
            throw new AssertionError("replay accepted an hcopykatata handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkatata\":1}");
            throw new AssertionError("replay accepted an hreadbackkatata handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkarasaki\":1}");
            throw new AssertionError("replay accepted an hgeomkarasaki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykarasaki\":1}");
            throw new AssertionError("replay accepted an hcopykarasaki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkarasaki\":1}");
            throw new AssertionError("replay accepted an hreadbackkarasaki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomhira\":1}");
            throw new AssertionError("replay accepted an hgeomhira handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyhira\":1}");
            throw new AssertionError("replay accepted an hcopyhira handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackhira\":1}");
            throw new AssertionError("replay accepted an hreadbackhira handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomwani\":1}");
            throw new AssertionError("replay accepted an hgeomwani handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopywani\":1}");
            throw new AssertionError("replay accepted an hcopywani handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackwani\":1}");
            throw new AssertionError("replay accepted an hreadbackwani handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomshigasato\":1}");
            throw new AssertionError("replay accepted an hgeomshigasato handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyshigasato\":1}");
            throw new AssertionError("replay accepted an hcopyshigasato handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackshigasato\":1}");
            throw new AssertionError("replay accepted an hreadbackshigasato handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomawazu\":1}");
            throw new AssertionError("replay accepted an hgeomawazu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyawazu\":1}");
            throw new AssertionError("replay accepted an hcopyawazu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackawazu\":1}");
            throw new AssertionError("replay accepted an hreadbackawazu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomnotogawa\":1}");
            throw new AssertionError("replay accepted an hgeomnotogawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopynotogawa\":1}");
            throw new AssertionError("replay accepted an hcopynotogawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacknotogawa\":1}");
            throw new AssertionError("replay accepted an hreadbacknotogawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomeigenji\":1}");
            throw new AssertionError("replay accepted an hgeomeigenji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyeigenji\":1}");
            throw new AssertionError("replay accepted an hcopyeigenji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackeigenji\":1}");
            throw new AssertionError("replay accepted an hreadbackeigenji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkonan\":1}");
            throw new AssertionError("replay accepted an hgeomkonan handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykonan\":1}");
            throw new AssertionError("replay accepted an hcopykonan handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkonan\":1}");
            throw new AssertionError("replay accepted an hreadbackkonan handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkusatsu\":1}");
            throw new AssertionError("replay accepted an hgeomkusatsu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykusatsu\":1}");
            throw new AssertionError("replay accepted an hcopykusatsu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkusatsu\":1}");
            throw new AssertionError("replay accepted an hreadbackkusatsu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomritto\":1}");
            throw new AssertionError("replay accepted an hgeomritto handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyritto\":1}");
            throw new AssertionError("replay accepted an hcopyritto handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackritto\":1}");
            throw new AssertionError("replay accepted an hreadbackritto handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeommoriyama\":1}");
            throw new AssertionError("replay accepted an hgeommoriyama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopymoriyama\":1}");
            throw new AssertionError("replay accepted an hcopymoriyama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackmoriyama\":1}");
            throw new AssertionError("replay accepted an hreadbackmoriyama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomyasu\":1}");
            throw new AssertionError("replay accepted an hgeomyasu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyyasu\":1}");
            throw new AssertionError("replay accepted an hcopyyasu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackyasu\":1}");
            throw new AssertionError("replay accepted an hreadbackyasu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomhino\":1}");
            throw new AssertionError("replay accepted an hgeomhino handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyhino\":1}");
            throw new AssertionError("replay accepted an hcopyhino handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackhino\":1}");
            throw new AssertionError("replay accepted an hreadbackhino handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkoka\":1}");
            throw new AssertionError("replay accepted an hgeomkoka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykoka\":1}");
            throw new AssertionError("replay accepted an hcopykoka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkoka\":1}");
            throw new AssertionError("replay accepted an hreadbackkoka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomminakuchi\":1}");
            throw new AssertionError("replay accepted an hgeomminakuchi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyminakuchi\":1}");
            throw new AssertionError("replay accepted an hcopyminakuchi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackminakuchi\":1}");
            throw new AssertionError("replay accepted an hreadbackminakuchi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomtsuchi\":1}");
            throw new AssertionError("replay accepted an hgeomtsuchi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopytsuchi\":1}");
            throw new AssertionError("replay accepted an hcopytsuchi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacktsuchi\":1}");
            throw new AssertionError("replay accepted an hreadbacktsuchi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomishibe\":1}");
            throw new AssertionError("replay accepted an hgeomishibe handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyishibe\":1}");
            throw new AssertionError("replay accepted an hcopyishibe handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackishibe\":1}");
            throw new AssertionError("replay accepted an hreadbackishibe handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomtakamiya\":1}");
            throw new AssertionError("replay accepted an hgeomtakamiya handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopytakamiya\":1}");
            throw new AssertionError("replay accepted an hcopytakamiya handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacktakamiya\":1}");
            throw new AssertionError("replay accepted an hreadbacktakamiya handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomyokaichi\":1}");
            throw new AssertionError("replay accepted an hgeomyokaichi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyyokaichi\":1}");
            throw new AssertionError("replay accepted an hcopyyokaichi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackyokaichi\":1}");
            throw new AssertionError("replay accepted an hreadbackyokaichi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomhikone\":1}");
            throw new AssertionError("replay accepted an hgeomhikone handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyhikone\":1}");
            throw new AssertionError("replay accepted an hcopyhikone handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackhikone\":1}");
            throw new AssertionError("replay accepted an hreadbackhikone handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomnagahama\":1}");
            throw new AssertionError("replay accepted an hgeomnagahama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopynagahama\":1}");
            throw new AssertionError("replay accepted an hcopynagahama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacknagahama\":1}");
            throw new AssertionError("replay accepted an hreadbacknagahama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomazuchi\":1}");
            throw new AssertionError("replay accepted an hgeomazuchi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyazuchi\":1}");
            throw new AssertionError("replay accepted an hcopyazuchi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackazuchi\":1}");
            throw new AssertionError("replay accepted an hreadbackazuchi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomyokkaichi\":1}");
            throw new AssertionError("replay accepted an hgeomyokkaichi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyyokkaichi\":1}");
            throw new AssertionError("replay accepted an hcopyyokkaichi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackyokkaichi\":1}");
            throw new AssertionError("replay accepted an hreadbackyokkaichi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomomihachiman\":1}");
            throw new AssertionError("replay accepted an hgeomomihachiman handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyomihachiman\":1}");
            throw new AssertionError("replay accepted an hcopyomihachiman handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackomihachiman\":1}");
            throw new AssertionError("replay accepted an hreadbackomihachiman handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeommaibara\":1}");
            throw new AssertionError("replay accepted an hgeommaibara handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopymaibara\":1}");
            throw new AssertionError("replay accepted an hcopymaibara handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackmaibara\":1}");
            throw new AssertionError("replay accepted an hreadbackmaibara handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomtakatsuki\":1}");
            throw new AssertionError("replay accepted an hgeomtakatsuki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopytakatsuki\":1}");
            throw new AssertionError("replay accepted an hcopytakatsuki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacktakatsuki\":1}");
            throw new AssertionError("replay accepted an hreadbacktakatsuki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomsamegai\":1}");
            throw new AssertionError("replay accepted an hgeomsamegai handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopysamegai\":1}");
            throw new AssertionError("replay accepted an hcopysamegai handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacksamegai\":1}");
            throw new AssertionError("replay accepted an hreadbacksamegai handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkunitomo\":1}");
            throw new AssertionError("replay accepted an hgeomkunitomo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykunitomo\":1}");
            throw new AssertionError("replay accepted an hcopykunitomo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkunitomo\":1}");
            throw new AssertionError("replay accepted an hreadbackkunitomo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomchikubushima\":1}");
            throw new AssertionError("replay accepted an hgeomchikubushima handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopychikubushima\":1}");
            throw new AssertionError("replay accepted an hcopychikubushima handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackchikubushima\":1}");
            throw new AssertionError("replay accepted an hreadbackchikubushima handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomhachiman\":1}");
            throw new AssertionError("replay accepted an hgeomhachiman handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyhachiman\":1}");
            throw new AssertionError("replay accepted an hcopyhachiman handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackhachiman\":1}");
            throw new AssertionError("replay accepted an hreadbackhachiman handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomsawayama\":1}");
            throw new AssertionError("replay accepted an hgeomsawayama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopysawayama\":1}");
            throw new AssertionError("replay accepted an hcopysawayama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacksawayama\":1}");
            throw new AssertionError("replay accepted an hreadbacksawayama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkinomoto\":1}");
            throw new AssertionError("replay accepted an hgeomkinomoto handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykinomoto\":1}");
            throw new AssertionError("replay accepted an hcopykinomoto handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkinomoto\":1}");
            throw new AssertionError("replay accepted an hreadbackkinomoto handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomodani\":1}");
            throw new AssertionError("replay accepted an hgeomodani handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyodani\":1}");
            throw new AssertionError("replay accepted an hcopyodani handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackodani\":1}");
            throw new AssertionError("replay accepted an hreadbackodani handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomtorahime\":1}");
            throw new AssertionError("replay accepted an hgeomtorahime handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopytorahime\":1}");
            throw new AssertionError("replay accepted an hcopytorahime handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacktorahime\":1}");
            throw new AssertionError("replay accepted an hreadbacktorahime handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomibuki\":1}");
            throw new AssertionError("replay accepted an hgeomibuki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyibuki\":1}");
            throw new AssertionError("replay accepted an hcopyibuki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackibuki\":1}");
            throw new AssertionError("replay accepted an hreadbackibuki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomtsuruga\":1}");
            throw new AssertionError("replay accepted an hgeomtsuruga handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopytsuruga\":1}");
            throw new AssertionError("replay accepted an hcopytsuruga handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacktsuruga\":1}");
            throw new AssertionError("replay accepted an hreadbacktsuruga handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomobama\":1}");
            throw new AssertionError("replay accepted an hgeomobama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyobama\":1}");
            throw new AssertionError("replay accepted an hcopyobama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackobama\":1}");
            throw new AssertionError("replay accepted an hreadbackobama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomechizen\":1}");
            throw new AssertionError("replay accepted an hgeomechizen handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyechizen\":1}");
            throw new AssertionError("replay accepted an hcopyechizen handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackechizen\":1}");
            throw new AssertionError("replay accepted an hreadbackechizen handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomimajo\":1}");
            throw new AssertionError("replay accepted an hgeomimajo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyimajo\":1}");
            throw new AssertionError("replay accepted an hcopyimajo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackimajo\":1}");
            throw new AssertionError("replay accepted an hreadbackimajo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomsekigahara\":1}");
            throw new AssertionError("replay accepted an hgeomsekigahara handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopysekigahara\":1}");
            throw new AssertionError("replay accepted an hcopysekigahara handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacksekigahara\":1}");
            throw new AssertionError("replay accepted an hreadbacksekigahara handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomogaki\":1}");
            throw new AssertionError("replay accepted an hgeomogaki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyogaki\":1}");
            throw new AssertionError("replay accepted an hcopyogaki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackogaki\":1}");
            throw new AssertionError("replay accepted an hreadbackogaki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomtarui\":1}");
            throw new AssertionError("replay accepted an hgeomtarui handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopytarui\":1}");
            throw new AssertionError("replay accepted an hcopytarui handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacktarui\":1}");
            throw new AssertionError("replay accepted an hreadbacktarui handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkuwana\":1}");
            throw new AssertionError("replay accepted an hgeomkuwana handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykuwana\":1}");
            throw new AssertionError("replay accepted an hcopykuwana handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkuwana\":1}");
            throw new AssertionError("replay accepted an hreadbackkuwana handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomtakefu\":1}");
            throw new AssertionError("replay accepted an hgeomtakefu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopytakefu\":1}");
            throw new AssertionError("replay accepted an hcopytakefu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacktakefu\":1}");
            throw new AssertionError("replay accepted an hreadbacktakefu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomsabae\":1}");
            throw new AssertionError("replay accepted an hgeomsabae handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopysabae\":1}");
            throw new AssertionError("replay accepted an hcopysabae handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacksabae\":1}");
            throw new AssertionError("replay accepted an hreadbacksabae handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomawara\":1}");
            throw new AssertionError("replay accepted an hgeomawara handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyawara\":1}");
            throw new AssertionError("replay accepted an hcopyawara handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackawara\":1}");
            throw new AssertionError("replay accepted an hreadbackawara handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeommihama\":1}");
            throw new AssertionError("replay accepted an hgeommihama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopymihama\":1}");
            throw new AssertionError("replay accepted an hcopymihama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackmihama\":1}");
            throw new AssertionError("replay accepted an hreadbackmihama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomwakasa\":1}");
            throw new AssertionError("replay accepted an hgeomwakasa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopywakasa\":1}");
            throw new AssertionError("replay accepted an hcopywakasa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackwakasa\":1}");
            throw new AssertionError("replay accepted an hreadbackwakasa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomtsurugi\":1}");
            throw new AssertionError("replay accepted an hgeomtsurugi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopytsurugi\":1}");
            throw new AssertionError("replay accepted an hcopytsurugi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacktsurugi\":1}");
            throw new AssertionError("replay accepted an hreadbacktsurugi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomowase\":1}");
            throw new AssertionError("replay accepted an hgeomowase handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyowase\":1}");
            throw new AssertionError("replay accepted an hcopyowase handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackowase\":1}");
            throw new AssertionError("replay accepted an hreadbackowase handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkumano\":1}");
            throw new AssertionError("replay accepted an hgeomkumano handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykumano\":1}");
            throw new AssertionError("replay accepted an hcopykumano handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkumano\":1}");
            throw new AssertionError("replay accepted an hreadbackkumano handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkihoku\":1}");
            throw new AssertionError("replay accepted an hgeomkihoku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykihoku\":1}");
            throw new AssertionError("replay accepted an hcopykihoku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkihoku\":1}");
            throw new AssertionError("replay accepted an hreadbackkihoku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeommatsusaka\":1}");
            throw new AssertionError("replay accepted an hgeommatsusaka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopymatsusaka\":1}");
            throw new AssertionError("replay accepted an hcopymatsusaka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackmatsusaka\":1}");
            throw new AssertionError("replay accepted an hreadbackmatsusaka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomtoba\":1}");
            throw new AssertionError("replay accepted an hgeomtoba handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopytoba\":1}");
            throw new AssertionError("replay accepted an hcopytoba handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacktoba\":1}");
            throw new AssertionError("replay accepted an hreadbacktoba handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomfukui\":1}");
            throw new AssertionError("replay accepted an hgeomfukui handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyfukui\":1}");
            throw new AssertionError("replay accepted an hcopyfukui handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackfukui\":1}");
            throw new AssertionError("replay accepted an hreadbackfukui handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkomatsu\":1}");
            throw new AssertionError("replay accepted an hgeomkomatsu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykomatsu\":1}");
            throw new AssertionError("replay accepted an hcopykomatsu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkomatsu\":1}");
            throw new AssertionError("replay accepted an hreadbackkomatsu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkaga\":1}");
            throw new AssertionError("replay accepted an hgeomkaga handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykaga\":1}");
            throw new AssertionError("replay accepted an hcopykaga handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkaga\":1}");
            throw new AssertionError("replay accepted an hreadbackkaga handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeommikuni\":1}");
            throw new AssertionError("replay accepted an hgeommikuni handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopymikuni\":1}");
            throw new AssertionError("replay accepted an hcopymikuni handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackmikuni\":1}");
            throw new AssertionError("replay accepted an hreadbackmikuni handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomeiheiji\":1}");
            throw new AssertionError("replay accepted an hgeomeiheiji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyeiheiji\":1}");
            throw new AssertionError("replay accepted an hcopyeiheiji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackeiheiji\":1}");
            throw new AssertionError("replay accepted an hreadbackeiheiji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkatsuyama\":1}");
            throw new AssertionError("replay accepted an hgeomkatsuyama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykatsuyama\":1}");
            throw new AssertionError("replay accepted an hcopykatsuyama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkatsuyama\":1}");
            throw new AssertionError("replay accepted an hreadbackkatsuyama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomnagaoka\":1}");
            throw new AssertionError("replay accepted an hgeomnagaoka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopynagaoka\":1}");
            throw new AssertionError("replay accepted an hcopynagaoka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacknagaoka\":1}");
            throw new AssertionError("replay accepted an hreadbacknagaoka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeommaruoka\":1}");
            throw new AssertionError("replay accepted an hgeommaruoka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopymaruoka\":1}");
            throw new AssertionError("replay accepted an hcopymaruoka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackmaruoka\":1}");
            throw new AssertionError("replay accepted an hreadbackmaruoka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomhashidate\":1}");
            throw new AssertionError("replay accepted an hgeomhashidate handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyhashidate\":1}");
            throw new AssertionError("replay accepted an hcopyhashidate handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackhashidate\":1}");
            throw new AssertionError("replay accepted an hreadbackhashidate handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeommaizuru\":1}");
            throw new AssertionError("replay accepted an hgeommaizuru handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopymaizuru\":1}");
            throw new AssertionError("replay accepted an hcopymaizuru handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackmaizuru\":1}");
            throw new AssertionError("replay accepted an hreadbackmaizuru handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeommiyazu\":1}");
            throw new AssertionError("replay accepted an hgeommiyazu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopymiyazu\":1}");
            throw new AssertionError("replay accepted an hcopymiyazu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackmiyazu\":1}");
            throw new AssertionError("replay accepted an hreadbackmiyazu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomamino\":1}");
            throw new AssertionError("replay accepted an hgeomamino handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyamino\":1}");
            throw new AssertionError("replay accepted an hcopyamino handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackamino\":1}");
            throw new AssertionError("replay accepted an hreadbackamino handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkumihama\":1}");
            throw new AssertionError("replay accepted an hgeomkumihama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykumihama\":1}");
            throw new AssertionError("replay accepted an hcopykumihama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkumihama\":1}");
            throw new AssertionError("replay accepted an hreadbackkumihama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeommineyama\":1}");
            throw new AssertionError("replay accepted an hgeommineyama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopymineyama\":1}");
            throw new AssertionError("replay accepted an hcopymineyama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackmineyama\":1}");
            throw new AssertionError("replay accepted an hreadbackmineyama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomnodagawa\":1}");
            throw new AssertionError("replay accepted an hgeomnodagawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopynodagawa\":1}");
            throw new AssertionError("replay accepted an hcopynodagawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacknodagawa\":1}");
            throw new AssertionError("replay accepted an hreadbacknodagawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomyosano\":1}");
            throw new AssertionError("replay accepted an hgeomyosano handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyyosano\":1}");
            throw new AssertionError("replay accepted an hcopyyosano handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackyosano\":1}");
            throw new AssertionError("replay accepted an hreadbackyosano handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkyotango\":1}");
            throw new AssertionError("replay accepted an hgeomkyotango handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykyotango\":1}");
            throw new AssertionError("replay accepted an hcopykyotango handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkyotango\":1}");
            throw new AssertionError("replay accepted an hreadbackkyotango handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomfukuchiyama\":1}");
            throw new AssertionError("replay accepted an hgeomfukuchiyama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyfukuchiyama\":1}");
            throw new AssertionError("replay accepted an hcopyfukuchiyama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackfukuchiyama\":1}");
            throw new AssertionError("replay accepted an hreadbackfukuchiyama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomayabe\":1}");
            throw new AssertionError("replay accepted an hgeomayabe handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyayabe\":1}");
            throw new AssertionError("replay accepted an hcopyayabe handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackayabe\":1}");
            throw new AssertionError("replay accepted an hreadbackayabe handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomnantan\":1}");
            throw new AssertionError("replay accepted an hgeomnantan handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopynantan\":1}");
            throw new AssertionError("replay accepted an hcopynantan handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacknantan\":1}");
            throw new AssertionError("replay accepted an hreadbacknantan handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomsonobe\":1}");
            throw new AssertionError("replay accepted an hgeomsonobe handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopysonobe\":1}");
            throw new AssertionError("replay accepted an hcopysonobe handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacksonobe\":1}");
            throw new AssertionError("replay accepted an hreadbacksonobe handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomhiyoshi\":1}");
            throw new AssertionError("replay accepted an hgeomhiyoshi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyhiyoshi\":1}");
            throw new AssertionError("replay accepted an hcopyhiyoshi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackhiyoshi\":1}");
            throw new AssertionError("replay accepted an hreadbackhiyoshi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeommiyama\":1}");
            throw new AssertionError("replay accepted an hgeommiyama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopymiyama\":1}");
            throw new AssertionError("replay accepted an hcopymiyama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackmiyama\":1}");
            throw new AssertionError("replay accepted an hreadbackmiyama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomwachi\":1}");
            throw new AssertionError("replay accepted an hgeomwachi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopywachi\":1}");
            throw new AssertionError("replay accepted an hcopywachi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackwachi\":1}");
            throw new AssertionError("replay accepted an hreadbackwachi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkeihoku\":1}");
            throw new AssertionError("replay accepted an hgeomkeihoku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykeihoku\":1}");
            throw new AssertionError("replay accepted an hcopykeihoku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkeihoku\":1}");
            throw new AssertionError("replay accepted an hreadbackkeihoku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomumekoji\":1}");
            throw new AssertionError("replay accepted an hgeomumekoji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyumekoji\":1}");
            throw new AssertionError("replay accepted an hcopyumekoji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackumekoji\":1}");
            throw new AssertionError("replay accepted an hreadbackumekoji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeommomoyama\":1}");
            throw new AssertionError("replay accepted an hgeommomoyama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopymomoyama\":1}");
            throw new AssertionError("replay accepted an hcopymomoyama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackmomoyama\":1}");
            throw new AssertionError("replay accepted an hreadbackmomoyama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomuzumasa\":1}");
            throw new AssertionError("replay accepted an hgeomuzumasa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyuzumasa\":1}");
            throw new AssertionError("replay accepted an hcopyuzumasa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackuzumasa\":1}");
            throw new AssertionError("replay accepted an hreadbackuzumasa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomhanazono\":1}");
            throw new AssertionError("replay accepted an hgeomhanazono handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyhanazono\":1}");
            throw new AssertionError("replay accepted an hcopyhanazono handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackhanazono\":1}");
            throw new AssertionError("replay accepted an hreadbackhanazono handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkamogawa\":1}");
            throw new AssertionError("replay accepted an hgeomkamogawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykamogawa\":1}");
            throw new AssertionError("replay accepted an hcopykamogawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkamogawa\":1}");
            throw new AssertionError("replay accepted an hreadbackkamogawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomyase\":1}");
            throw new AssertionError("replay accepted an hgeomyase handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyyase\":1}");
            throw new AssertionError("replay accepted an hcopyyase handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackyase\":1}");
            throw new AssertionError("replay accepted an hreadbackyase handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomimamiya\":1}");
            throw new AssertionError("replay accepted an hgeomimamiya handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyimamiya\":1}");
            throw new AssertionError("replay accepted an hcopyimamiya handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackimamiya\":1}");
            throw new AssertionError("replay accepted an hreadbackimamiya handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomhorikawa\":1}");
            throw new AssertionError("replay accepted an hgeomhorikawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyhorikawa\":1}");
            throw new AssertionError("replay accepted an hcopyhorikawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackhorikawa\":1}");
            throw new AssertionError("replay accepted an hreadbackhorikawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkawaramachi\":1}");
            throw new AssertionError("replay accepted an hgeomkawaramachi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykawaramachi\":1}");
            throw new AssertionError("replay accepted an hcopykawaramachi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkawaramachi\":1}");
            throw new AssertionError("replay accepted an hreadbackkawaramachi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomteramachi\":1}");
            throw new AssertionError("replay accepted an hgeomteramachi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyteramachi\":1}");
            throw new AssertionError("replay accepted an hcopyteramachi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackteramachi\":1}");
            throw new AssertionError("replay accepted an hreadbackteramachi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkiyamachi\":1}");
            throw new AssertionError("replay accepted an hgeomkiyamachi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykiyamachi\":1}");
            throw new AssertionError("replay accepted an hcopykiyamachi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkiyamachi\":1}");
            throw new AssertionError("replay accepted an hreadbackkiyamachi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeompontocho\":1}");
            throw new AssertionError("replay accepted an hgeompontocho handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopypontocho\":1}");
            throw new AssertionError("replay accepted an hcopypontocho handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackpontocho\":1}");
            throw new AssertionError("replay accepted an hreadbackpontocho handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkitano\":1}");
            throw new AssertionError("replay accepted an hgeomkitano handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykitano\":1}");
            throw new AssertionError("replay accepted an hcopykitano handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkitano\":1}");
            throw new AssertionError("replay accepted an hreadbackkitano handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomsanjo\":1}");
            throw new AssertionError("replay accepted an hgeomsanjo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopysanjo\":1}");
            throw new AssertionError("replay accepted an hcopysanjo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacksanjo\":1}");
            throw new AssertionError("replay accepted an hreadbacksanjo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomshijo\":1}");
            throw new AssertionError("replay accepted an hgeomshijo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyshijo\":1}");
            throw new AssertionError("replay accepted an hcopyshijo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackshijo\":1}");
            throw new AssertionError("replay accepted an hreadbackshijo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkarasuma\":1}");
            throw new AssertionError("replay accepted an hgeomkarasuma handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykarasuma\":1}");
            throw new AssertionError("replay accepted an hcopykarasuma handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkarasuma\":1}");
            throw new AssertionError("replay accepted an hreadbackkarasuma handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomtakoyakushi\":1}");
            throw new AssertionError("replay accepted an hgeomtakoyakushi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopytakoyakushi\":1}");
            throw new AssertionError("replay accepted an hcopytakoyakushi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacktakoyakushi\":1}");
            throw new AssertionError("replay accepted an hreadbacktakoyakushi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomoike\":1}");
            throw new AssertionError("replay accepted an hgeomoike handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyoike\":1}");
            throw new AssertionError("replay accepted an hcopyoike handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackoike\":1}");
            throw new AssertionError("replay accepted an hreadbackoike handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeommarutamachi\":1}");
            throw new AssertionError("replay accepted an hgeommarutamachi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopymarutamachi\":1}");
            throw new AssertionError("replay accepted an hcopymarutamachi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackmarutamachi\":1}");
            throw new AssertionError("replay accepted an hreadbackmarutamachi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomimadegawa\":1}");
            throw new AssertionError("replay accepted an hgeomimadegawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyimadegawa\":1}");
            throw new AssertionError("replay accepted an hcopyimadegawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackimadegawa\":1}");
            throw new AssertionError("replay accepted an hreadbackimadegawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkitaoji\":1}");
            throw new AssertionError("replay accepted an hgeomkitaoji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykitaoji\":1}");
            throw new AssertionError("replay accepted an hcopykitaoji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkitaoji\":1}");
            throw new AssertionError("replay accepted an hreadbackkitaoji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkawabata\":1}");
            throw new AssertionError("replay accepted an hgeomkawabata handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykawabata\":1}");
            throw new AssertionError("replay accepted an hcopykawabata handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkawabata\":1}");
            throw new AssertionError("replay accepted an hreadbackkawabata handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomtakatsuji\":1}");
            throw new AssertionError("replay accepted an hgeomtakatsuji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopytakatsuji\":1}");
            throw new AssertionError("replay accepted an hcopytakatsuji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacktakatsuji\":1}");
            throw new AssertionError("replay accepted an hreadbacktakatsuji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeommatsubara\":1}");
            throw new AssertionError("replay accepted an hgeommatsubara handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopymatsubara\":1}");
            throw new AssertionError("replay accepted an hcopymatsubara handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackmatsubara\":1}");
            throw new AssertionError("replay accepted an hreadbackmatsubara handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomshiokoji\":1}");
            throw new AssertionError("replay accepted an hgeomshiokoji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyshiokoji\":1}");
            throw new AssertionError("replay accepted an hcopyshiokoji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackshiokoji\":1}");
            throw new AssertionError("replay accepted an hreadbackshiokoji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomhachijo\":1}");
            throw new AssertionError("replay accepted an hgeomhachijo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyhachijo\":1}");
            throw new AssertionError("replay accepted an hcopyhachijo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackhachijo\":1}");
            throw new AssertionError("replay accepted an hreadbackhachijo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomjujo\":1}");
            throw new AssertionError("replay accepted an hgeomjujo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyjujo\":1}");
            throw new AssertionError("replay accepted an hcopyjujo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackjujo\":1}");
            throw new AssertionError("replay accepted an hreadbackjujo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkujo\":1}");
            throw new AssertionError("replay accepted an hgeomkujo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykujo\":1}");
            throw new AssertionError("replay accepted an hcopykujo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkujo\":1}");
            throw new AssertionError("replay accepted an hreadbackkujo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomshichijo\":1}");
            throw new AssertionError("replay accepted an hgeomshichijo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyshichijo\":1}");
            throw new AssertionError("replay accepted an hcopyshichijo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackshichijo\":1}");
            throw new AssertionError("replay accepted an hreadbackshichijo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomshinkyogoku\":1}");
            throw new AssertionError("replay accepted an hgeomshinkyogoku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyshinkyogoku\":1}");
            throw new AssertionError("replay accepted an hcopyshinkyogoku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackshinkyogoku\":1}");
            throw new AssertionError("replay accepted an hreadbackshinkyogoku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomuji\":1}");
            throw new AssertionError("replay accepted an hgeomuji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyuji\":1}");
            throw new AssertionError("replay accepted an hcopyuji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackuji\":1}");
            throw new AssertionError("replay accepted an hreadbackuji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomyamashina\":1}");
            throw new AssertionError("replay accepted an hgeomyamashina handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyyamashina\":1}");
            throw new AssertionError("replay accepted an hcopyyamashina handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackyamashina\":1}");
            throw new AssertionError("replay accepted an hreadbackyamashina handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomyodo\":1}");
            throw new AssertionError("replay accepted an hgeomyodo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyyodo\":1}");
            throw new AssertionError("replay accepted an hcopyyodo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackyodo\":1}");
            throw new AssertionError("replay accepted an hreadbackyodo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomogura\":1}");
            throw new AssertionError("replay accepted an hgeomogura handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyogura\":1}");
            throw new AssertionError("replay accepted an hcopyogura handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackogura\":1}");
            throw new AssertionError("replay accepted an hreadbackogura handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkohata\":1}");
            throw new AssertionError("replay accepted an hgeomkohata handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykohata\":1}");
            throw new AssertionError("replay accepted an hcopykohata handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkohata\":1}");
            throw new AssertionError("replay accepted an hreadbackkohata handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomrokujizo\":1}");
            throw new AssertionError("replay accepted an hgeomrokujizo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyrokujizo\":1}");
            throw new AssertionError("replay accepted an hcopyrokujizo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackrokujizo\":1}");
            throw new AssertionError("replay accepted an hreadbackrokujizo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomoubaku\":1}");
            throw new AssertionError("replay accepted an hgeomoubaku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyoubaku\":1}");
            throw new AssertionError("replay accepted an hcopyoubaku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackoubaku\":1}");
            throw new AssertionError("replay accepted an hreadbackoubaku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeommakishima\":1}");
            throw new AssertionError("replay accepted an hgeommakishima handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopymakishima\":1}");
            throw new AssertionError("replay accepted an hcopymakishima handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackmakishima\":1}");
            throw new AssertionError("replay accepted an hreadbackmakishima handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomobaku\":1}");
            throw new AssertionError("replay accepted an hgeomobaku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyobaku\":1}");
            throw new AssertionError("replay accepted an hcopyobaku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackobaku\":1}");
            throw new AssertionError("replay accepted an hreadbackobaku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomtowada\":1}");
            throw new AssertionError("replay accepted an hgeomtowada handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopytowada\":1}");
            throw new AssertionError("replay accepted an hcopytowada handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacktowada\":1}");
            throw new AssertionError("replay accepted an hreadbacktowada handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeommuko\":1}");
            throw new AssertionError("replay accepted an hgeommuko handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopymuko\":1}");
            throw new AssertionError("replay accepted an hcopymuko handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackmuko\":1}");
            throw new AssertionError("replay accepted an hreadbackmuko handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomoyamazaki\":1}");
            throw new AssertionError("replay accepted an hgeomoyamazaki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyoyamazaki\":1}");
            throw new AssertionError("replay accepted an hcopyoyamazaki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackoyamazaki\":1}");
            throw new AssertionError("replay accepted an hreadbackoyamazaki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomyawata\":1}");
            throw new AssertionError("replay accepted an hgeomyawata handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyyawata\":1}");
            throw new AssertionError("replay accepted an hcopyyawata handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackyawata\":1}");
            throw new AssertionError("replay accepted an hreadbackyawata handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkyotanabe\":1}");
            throw new AssertionError("replay accepted an hgeomkyotanabe handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykyotanabe\":1}");
            throw new AssertionError("replay accepted an hcopykyotanabe handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkyotanabe\":1}");
            throw new AssertionError("replay accepted an hreadbackkyotanabe handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkizu\":1}");
            throw new AssertionError("replay accepted an hgeomkizu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykizu\":1}");
            throw new AssertionError("replay accepted an hcopykizu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkizu\":1}");
            throw new AssertionError("replay accepted an hreadbackkizu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomide\":1}");
            throw new AssertionError("replay accepted an hgeomide handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyide\":1}");
            throw new AssertionError("replay accepted an hcopyide handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackide\":1}");
            throw new AssertionError("replay accepted an hreadbackide handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomwazuka\":1}");
            throw new AssertionError("replay accepted an hgeomwazuka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopywazuka\":1}");
            throw new AssertionError("replay accepted an hcopywazuka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackwazuka\":1}");
            throw new AssertionError("replay accepted an hreadbackwazuka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkasagi\":1}");
            throw new AssertionError("replay accepted an hgeomkasagi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykasagi\":1}");
            throw new AssertionError("replay accepted an hcopykasagi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkasagi\":1}");
            throw new AssertionError("replay accepted an hreadbackkasagi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkatano\":1}");
            throw new AssertionError("replay accepted an hgeomkatano handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykatano\":1}");
            throw new AssertionError("replay accepted an hcopykatano handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkatano\":1}");
            throw new AssertionError("replay accepted an hreadbackkatano handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomneyagawa\":1}");
            throw new AssertionError("replay accepted an hgeomneyagawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyneyagawa\":1}");
            throw new AssertionError("replay accepted an hcopyneyagawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackneyagawa\":1}");
            throw new AssertionError("replay accepted an hreadbackneyagawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkadoma\":1}");
            throw new AssertionError("replay accepted an hgeomkadoma handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykadoma\":1}");
            throw new AssertionError("replay accepted an hcopykadoma handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkadoma\":1}");
            throw new AssertionError("replay accepted an hreadbackkadoma handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeommoriguchi\":1}");
            throw new AssertionError("replay accepted an hgeommoriguchi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopymoriguchi\":1}");
            throw new AssertionError("replay accepted an hcopymoriguchi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackmoriguchi\":1}");
            throw new AssertionError("replay accepted an hreadbackmoriguchi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomsuita\":1}");
            throw new AssertionError("replay accepted an hgeomsuita handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopysuita\":1}");
            throw new AssertionError("replay accepted an hcopysuita handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacksuita\":1}");
            throw new AssertionError("replay accepted an hreadbacksuita handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomibaraki\":1}");
            throw new AssertionError("replay accepted an hgeomibaraki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyibaraki\":1}");
            throw new AssertionError("replay accepted an hcopyibaraki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackibaraki\":1}");
            throw new AssertionError("replay accepted an hreadbackibaraki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomikeda\":1}");
            throw new AssertionError("replay accepted an hgeomikeda handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyikeda\":1}");
            throw new AssertionError("replay accepted an hcopyikeda handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackikeda\":1}");
            throw new AssertionError("replay accepted an hreadbackikeda handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomtoyonaka\":1}");
            throw new AssertionError("replay accepted an hgeomtoyonaka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopytoyonaka\":1}");
            throw new AssertionError("replay accepted an hcopytoyonaka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacktoyonaka\":1}");
            throw new AssertionError("replay accepted an hreadbacktoyonaka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomyao\":1}");
            throw new AssertionError("replay accepted an hgeomyao handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyyao\":1}");
            throw new AssertionError("replay accepted an hcopyyao handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackyao\":1}");
            throw new AssertionError("replay accepted an hreadbackyao handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkashiwara\":1}");
            throw new AssertionError("replay accepted an hgeomkashiwara handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykashiwara\":1}");
            throw new AssertionError("replay accepted an hcopykashiwara handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkashiwara\":1}");
            throw new AssertionError("replay accepted an hreadbackkashiwara handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomhabikino\":1}");
            throw new AssertionError("replay accepted an hgeomhabikino handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyhabikino\":1}");
            throw new AssertionError("replay accepted an hcopyhabikino handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackhabikino\":1}");
            throw new AssertionError("replay accepted an hreadbackhabikino handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomtondabayashi\":1}");
            throw new AssertionError("replay accepted an hgeomtondabayashi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopytondabayashi\":1}");
            throw new AssertionError("replay accepted an hcopytondabayashi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacktondabayashi\":1}");
            throw new AssertionError("replay accepted an hreadbacktondabayashi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkawachinagano\":1}");
            throw new AssertionError("replay accepted an hgeomkawachinagano handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykawachinagano\":1}");
            throw new AssertionError("replay accepted an hcopykawachinagano handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkawachinagano\":1}");
            throw new AssertionError("replay accepted an hreadbackkawachinagano handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkishiwada\":1}");
            throw new AssertionError("replay accepted an hgeomkishiwada handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykishiwada\":1}");
            throw new AssertionError("replay accepted an hcopykishiwada handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkishiwada\":1}");
            throw new AssertionError("replay accepted an hreadbackkishiwada handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkaizuka\":1}");
            throw new AssertionError("replay accepted an hgeomkaizuka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykaizuka\":1}");
            throw new AssertionError("replay accepted an hcopykaizuka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkaizuka\":1}");
            throw new AssertionError("replay accepted an hreadbackkaizuka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomsennan\":1}");
            throw new AssertionError("replay accepted an hgeomsennan handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopysennan\":1}");
            throw new AssertionError("replay accepted an hcopysennan handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacksennan\":1}");
            throw new AssertionError("replay accepted an hreadbacksennan handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomhannan\":1}");
            throw new AssertionError("replay accepted an hgeomhannan handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyhannan\":1}");
            throw new AssertionError("replay accepted an hcopyhannan handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackhannan\":1}");
            throw new AssertionError("replay accepted an hreadbackhannan handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomtajiri\":1}");
            throw new AssertionError("replay accepted an hgeomtajiri handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopytajiri\":1}");
            throw new AssertionError("replay accepted an hcopytajiri handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacktajiri\":1}");
            throw new AssertionError("replay accepted an hreadbacktajiri handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkumatori\":1}");
            throw new AssertionError("replay accepted an hgeomkumatori handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykumatori\":1}");
            throw new AssertionError("replay accepted an hcopykumatori handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkumatori\":1}");
            throw new AssertionError("replay accepted an hreadbackkumatori handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomtadaoka\":1}");
            throw new AssertionError("replay accepted an hgeomtadaoka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopytadaoka\":1}");
            throw new AssertionError("replay accepted an hcopytadaoka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacktadaoka\":1}");
            throw new AssertionError("replay accepted an hreadbacktadaoka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomtaishi\":1}");
            throw new AssertionError("replay accepted an hgeomtaishi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopytaishi\":1}");
            throw new AssertionError("replay accepted an hcopytaishi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacktaishi\":1}");
            throw new AssertionError("replay accepted an hreadbacktaishi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkanan\":1}");
            throw new AssertionError("replay accepted an hgeomkanan handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykanan\":1}");
            throw new AssertionError("replay accepted an hcopykanan handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkanan\":1}");
            throw new AssertionError("replay accepted an hreadbackkanan handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomchihaya\":1}");
            throw new AssertionError("replay accepted an hgeomchihaya handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopychihaya\":1}");
            throw new AssertionError("replay accepted an hcopychihaya handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackchihaya\":1}");
            throw new AssertionError("replay accepted an hreadbackchihaya handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomyamatokoriyama\":1}");
            throw new AssertionError("replay accepted an hgeomyamatokoriyama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyyamatokoriyama\":1}");
            throw new AssertionError("replay accepted an hcopyyamatokoriyama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackyamatokoriyama\":1}");
            throw new AssertionError("replay accepted an hreadbackyamatokoriyama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkashihara\":1}");
            throw new AssertionError("replay accepted an hgeomkashihara handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykashihara\":1}");
            throw new AssertionError("replay accepted an hcopykashihara handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkashihara\":1}");
            throw new AssertionError("replay accepted an hreadbackkashihara handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomsakurai\":1}");
            throw new AssertionError("replay accepted an hgeomsakurai handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopysakurai\":1}");
            throw new AssertionError("replay accepted an hcopysakurai handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacksakurai\":1}");
            throw new AssertionError("replay accepted an hreadbacksakurai handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomgose\":1}");
            throw new AssertionError("replay accepted an hgeomgose handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopygose\":1}");
            throw new AssertionError("replay accepted an hcopygose handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackgose\":1}");
            throw new AssertionError("replay accepted an hreadbackgose handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomtenri\":1}");
            throw new AssertionError("replay accepted an hgeomtenri handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopytenri\":1}");
            throw new AssertionError("replay accepted an hcopytenri handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacktenri\":1}");
            throw new AssertionError("replay accepted an hreadbacktenri handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomikoma\":1}");
            throw new AssertionError("replay accepted an hgeomikoma handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyikoma\":1}");
            throw new AssertionError("replay accepted an hcopyikoma handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackikoma\":1}");
            throw new AssertionError("replay accepted an hreadbackikoma handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomyamatotakada\":1}");
            throw new AssertionError("replay accepted an hgeomyamatotakada handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyyamatotakada\":1}");
            throw new AssertionError("replay accepted an hcopyyamatotakada handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackyamatotakada\":1}");
            throw new AssertionError("replay accepted an hreadbackyamatotakada handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkashiba\":1}");
            throw new AssertionError("replay accepted an hgeomkashiba handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykashiba\":1}");
            throw new AssertionError("replay accepted an hcopykashiba handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkashiba\":1}");
            throw new AssertionError("replay accepted an hreadbackkashiba handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkanmaki\":1}");
            throw new AssertionError("replay accepted an hgeomkanmaki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykanmaki\":1}");
            throw new AssertionError("replay accepted an hcopykanmaki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkanmaki\":1}");
            throw new AssertionError("replay accepted an hreadbackkanmaki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomheijo\":1}");
            throw new AssertionError("replay accepted an hgeomheijo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyheijo\":1}");
            throw new AssertionError("replay accepted an hcopyheijo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackheijo\":1}");
            throw new AssertionError("replay accepted an hreadbackheijo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomsaidaiji\":1}");
            throw new AssertionError("replay accepted an hgeomsaidaiji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopysaidaiji\":1}");
            throw new AssertionError("replay accepted an hcopysaidaiji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacksaidaiji\":1}");
            throw new AssertionError("replay accepted an hreadbacksaidaiji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomtodaiji\":1}");
            throw new AssertionError("replay accepted an hgeomtodaiji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopytodaiji\":1}");
            throw new AssertionError("replay accepted an hcopytodaiji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacktodaiji\":1}");
            throw new AssertionError("replay accepted an hreadbacktodaiji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomhoryuji\":1}");
            throw new AssertionError("replay accepted an hgeomhoryuji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyhoryuji\":1}");
            throw new AssertionError("replay accepted an hcopyhoryuji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackhoryuji\":1}");
            throw new AssertionError("replay accepted an hreadbackhoryuji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomyakushiji\":1}");
            throw new AssertionError("replay accepted an hgeomyakushiji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyyakushiji\":1}");
            throw new AssertionError("replay accepted an hcopyyakushiji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackyakushiji\":1}");
            throw new AssertionError("replay accepted an hreadbackyakushiji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomtoshodaiji\":1}");
            throw new AssertionError("replay accepted an hgeomtoshodaiji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopytoshodaiji\":1}");
            throw new AssertionError("replay accepted an hcopytoshodaiji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacktoshodaiji\":1}");
            throw new AssertionError("replay accepted an hreadbacktoshodaiji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkofukuji\":1}");
            throw new AssertionError("replay accepted an hgeomkofukuji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykofukuji\":1}");
            throw new AssertionError("replay accepted an hcopykofukuji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkofukuji\":1}");
            throw new AssertionError("replay accepted an hreadbackkofukuji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkasuga\":1}");
            throw new AssertionError("replay accepted an hgeomkasuga handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykasuga\":1}");
            throw new AssertionError("replay accepted an hcopykasuga handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkasuga\":1}");
            throw new AssertionError("replay accepted an hreadbackkasuga handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomnaramachi\":1}");
            throw new AssertionError("replay accepted an hgeomnaramachi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopynaramachi\":1}");
            throw new AssertionError("replay accepted an hcopynaramachi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacknaramachi\":1}");
            throw new AssertionError("replay accepted an hreadbacknaramachi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomasuka\":1}");
            throw new AssertionError("replay accepted an hgeomasuka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyasuka\":1}");
            throw new AssertionError("replay accepted an hcopyasuka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackasuka\":1}");
            throw new AssertionError("replay accepted an hreadbackasuka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomyoshino\":1}");
            throw new AssertionError("replay accepted an hgeomyoshino handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyyoshino\":1}");
            throw new AssertionError("replay accepted an hcopyyoshino handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackyoshino\":1}");
            throw new AssertionError("replay accepted an hreadbackyoshino handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomhashimoto\":1}");
            throw new AssertionError("replay accepted an hgeomhashimoto handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyhashimoto\":1}");
            throw new AssertionError("replay accepted an hcopyhashimoto handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackhashimoto\":1}");
            throw new AssertionError("replay accepted an hreadbackhashimoto handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomikaruga\":1}");
            throw new AssertionError("replay accepted an hgeomikaruga handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyikaruga\":1}");
            throw new AssertionError("replay accepted an hcopyikaruga handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackikaruga\":1}");
            throw new AssertionError("replay accepted an hreadbackikaruga handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomchuguji\":1}");
            throw new AssertionError("replay accepted an hgeomchuguji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopychuguji\":1}");
            throw new AssertionError("replay accepted an hcopychuguji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackchuguji\":1}");
            throw new AssertionError("replay accepted an hreadbackchuguji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomhokkiji\":1}");
            throw new AssertionError("replay accepted an hgeomhokkiji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyhokkiji\":1}");
            throw new AssertionError("replay accepted an hcopyhokkiji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackhokkiji\":1}");
            throw new AssertionError("replay accepted an hreadbackhokkiji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomhorinji\":1}");
            throw new AssertionError("replay accepted an hgeomhorinji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyhorinji\":1}");
            throw new AssertionError("replay accepted an hcopyhorinji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackhorinji\":1}");
            throw new AssertionError("replay accepted an hreadbackhorinji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomjoruriji\":1}");
            throw new AssertionError("replay accepted an hgeomjoruriji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyjoruriji\":1}");
            throw new AssertionError("replay accepted an hcopyjoruriji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackjoruriji\":1}");
            throw new AssertionError("replay accepted an hreadbackjoruriji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomakishinodera\":1}");
            throw new AssertionError("replay accepted an hgeomakishinodera handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyakishinodera\":1}");
            throw new AssertionError("replay accepted an hcopyakishinodera handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackakishinodera\":1}");
            throw new AssertionError("replay accepted an hreadbackakishinodera handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomshigisan\":1}");
            throw new AssertionError("replay accepted an hgeomshigisan handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyshigisan\":1}");
            throw new AssertionError("replay accepted an hcopyshigisan handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackshigisan\":1}");
            throw new AssertionError("replay accepted an hreadbackshigisan handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomishibutai\":1}");
            throw new AssertionError("replay accepted an hgeomishibutai handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyishibutai\":1}");
            throw new AssertionError("replay accepted an hcopyishibutai handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackishibutai\":1}");
            throw new AssertionError("replay accepted an hreadbackishibutai handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkitora\":1}");
            throw new AssertionError("replay accepted an hgeomkitora handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykitora\":1}");
            throw new AssertionError("replay accepted an hcopykitora handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkitora\":1}");
            throw new AssertionError("replay accepted an hreadbackkitora handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomhasedera\":1}");
            throw new AssertionError("replay accepted an hgeomhasedera handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyhasedera\":1}");
            throw new AssertionError("replay accepted an hcopyhasedera handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackhasedera\":1}");
            throw new AssertionError("replay accepted an hreadbackhasedera handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomfujiwara\":1}");
            throw new AssertionError("replay accepted an hgeomfujiwara handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyfujiwara\":1}");
            throw new AssertionError("replay accepted an hcopyfujiwara handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackfujiwara\":1}");
            throw new AssertionError("replay accepted an hreadbackfujiwara handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeommiwa\":1}");
            throw new AssertionError("replay accepted an hgeommiwa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopymiwa\":1}");
            throw new AssertionError("replay accepted an hcopymiwa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackmiwa\":1}");
            throw new AssertionError("replay accepted an hreadbackmiwa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomtaima\":1}");
            throw new AssertionError("replay accepted an hgeomtaima handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopytaima\":1}");
            throw new AssertionError("replay accepted an hcopytaima handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacktaima\":1}");
            throw new AssertionError("replay accepted an hreadbacktaima handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeommurou\":1}");
            throw new AssertionError("replay accepted an hgeommurou handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopymurou\":1}");
            throw new AssertionError("replay accepted an hcopymurou handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackmurou\":1}");
            throw new AssertionError("replay accepted an hreadbackmurou handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomhaibara\":1}");
            throw new AssertionError("replay accepted an hgeomhaibara handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyhaibara\":1}");
            throw new AssertionError("replay accepted an hcopyhaibara handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackhaibara\":1}");
            throw new AssertionError("replay accepted an hreadbackhaibara handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomouda\":1}");
            throw new AssertionError("replay accepted an hgeomouda handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyouda\":1}");
            throw new AssertionError("replay accepted an hcopyouda handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackouda\":1}");
            throw new AssertionError("replay accepted an hreadbackouda handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomtakamado\":1}");
            throw new AssertionError("replay accepted an hgeomtakamado handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopytakamado\":1}");
            throw new AssertionError("replay accepted an hcopytakamado handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacktakamado\":1}");
            throw new AssertionError("replay accepted an hreadbacktakamado handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomwakakusa\":1}");
            throw new AssertionError("replay accepted an hgeomwakakusa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopywakakusa\":1}");
            throw new AssertionError("replay accepted an hcopywakakusa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackwakakusa\":1}");
            throw new AssertionError("replay accepted an hreadbackwakakusa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomumami\":1}");
            throw new AssertionError("replay accepted an hgeomumami handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyumami\":1}");
            throw new AssertionError("replay accepted an hcopyumami handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackumami\":1}");
            throw new AssertionError("replay accepted an hreadbackumami handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomyagyu\":1}");
            throw new AssertionError("replay accepted an hgeomyagyu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyyagyu\":1}");
            throw new AssertionError("replay accepted an hcopyyagyu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackyagyu\":1}");
            throw new AssertionError("replay accepted an hreadbackyagyu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomtsuge\":1}");
            throw new AssertionError("replay accepted an hgeomtsuge handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopytsuge\":1}");
            throw new AssertionError("replay accepted an hcopytsuge handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacktsuge\":1}");
            throw new AssertionError("replay accepted an hreadbacktsuge handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomtotsukawa\":1}");
            throw new AssertionError("replay accepted an hgeomtotsukawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopytotsukawa\":1}");
            throw new AssertionError("replay accepted an hcopytotsukawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacktotsukawa\":1}");
            throw new AssertionError("replay accepted an hreadbacktotsukawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomnosegawa\":1}");
            throw new AssertionError("replay accepted an hgeomnosegawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopynosegawa\":1}");
            throw new AssertionError("replay accepted an hcopynosegawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacknosegawa\":1}");
            throw new AssertionError("replay accepted an hreadbacknosegawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomtenkawa\":1}");
            throw new AssertionError("replay accepted an hgeomtenkawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopytenkawa\":1}");
            throw new AssertionError("replay accepted an hcopytenkawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacktenkawa\":1}");
            throw new AssertionError("replay accepted an hreadbacktenkawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkawakami\":1}");
            throw new AssertionError("replay accepted an hgeomkawakami handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykawakami\":1}");
            throw new AssertionError("replay accepted an hcopykawakami handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkawakami\":1}");
            throw new AssertionError("replay accepted an hreadbackkawakami handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkurotaki\":1}");
            throw new AssertionError("replay accepted an hgeomkurotaki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykurotaki\":1}");
            throw new AssertionError("replay accepted an hcopykurotaki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkurotaki\":1}");
            throw new AssertionError("replay accepted an hreadbackkurotaki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomnigatsudo\":1}");
            throw new AssertionError("replay accepted an hgeomnigatsudo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopynigatsudo\":1}");
            throw new AssertionError("replay accepted an hcopynigatsudo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacknigatsudo\":1}");
            throw new AssertionError("replay accepted an hreadbacknigatsudo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomtsukigase\":1}");
            throw new AssertionError("replay accepted an hgeomtsukigase handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopytsukigase\":1}");
            throw new AssertionError("replay accepted an hcopytsukigase handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacktsukigase\":1}");
            throw new AssertionError("replay accepted an hreadbacktsukigase handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomsoekami\":1}");
            throw new AssertionError("replay accepted an hgeomsoekami handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopysoekami\":1}");
            throw new AssertionError("replay accepted an hcopysoekami handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacksoekami\":1}");
            throw new AssertionError("replay accepted an hreadbacksoekami handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomhorai\":1}");
            throw new AssertionError("replay accepted an hgeomhorai handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyhorai\":1}");
            throw new AssertionError("replay accepted an hcopyhorai handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackhorai\":1}");
            throw new AssertionError("replay accepted an hreadbackhorai handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomsangatsudo\":1}");
            throw new AssertionError("replay accepted an hgeomsangatsudo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopysangatsudo\":1}");
            throw new AssertionError("replay accepted an hcopysangatsudo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacksangatsudo\":1}");
            throw new AssertionError("replay accepted an hreadbacksangatsudo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomchogakuji\":1}");
            throw new AssertionError("replay accepted an hgeomchogakuji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopychogakuji\":1}");
            throw new AssertionError("replay accepted an hcopychogakuji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackchogakuji\":1}");
            throw new AssertionError("replay accepted an hreadbackchogakuji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomabe\":1}");
            throw new AssertionError("replay accepted an hgeomabe handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyabe\":1}");
            throw new AssertionError("replay accepted an hcopyabe handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackabe\":1}");
            throw new AssertionError("replay accepted an hreadbackabe handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkamikitayama\":1}");
            throw new AssertionError("replay accepted an hgeomkamikitayama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykamikitayama\":1}");
            throw new AssertionError("replay accepted an hcopykamikitayama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkamikitayama\":1}");
            throw new AssertionError("replay accepted an hreadbackkamikitayama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomtakenouchi\":1}");
            throw new AssertionError("replay accepted an hgeomtakenouchi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopytakenouchi\":1}");
            throw new AssertionError("replay accepted an hcopytakenouchi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacktakenouchi\":1}");
            throw new AssertionError("replay accepted an hreadbacktakenouchi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomtsutsui\":1}");
            throw new AssertionError("replay accepted an hgeomtsutsui handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopytsutsui\":1}");
            throw new AssertionError("replay accepted an hcopytsutsui handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacktsutsui\":1}");
            throw new AssertionError("replay accepted an hreadbacktsutsui handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkosanji\":1}");
            throw new AssertionError("replay accepted an hgeomkosanji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykosanji\":1}");
            throw new AssertionError("replay accepted an hcopykosanji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkosanji\":1}");
            throw new AssertionError("replay accepted an hreadbackkosanji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomfutaiji\":1}");
            throw new AssertionError("replay accepted an hgeomfutaiji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyfutaiji\":1}");
            throw new AssertionError("replay accepted an hcopyfutaiji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackfutaiji\":1}");
            throw new AssertionError("replay accepted an hreadbackfutaiji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomhokkeji\":1}");
            throw new AssertionError("replay accepted an hgeomhokkeji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyhokkeji\":1}");
            throw new AssertionError("replay accepted an hcopyhokkeji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackhokkeji\":1}");
            throw new AssertionError("replay accepted an hreadbackhokkeji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomtoganoo\":1}");
            throw new AssertionError("replay accepted an hgeomtoganoo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopytoganoo\":1}");
            throw new AssertionError("replay accepted an hcopytoganoo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacktoganoo\":1}");
            throw new AssertionError("replay accepted an hreadbacktoganoo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkozanj\":1}");
            throw new AssertionError("replay accepted an hgeomkozanj handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykozanj\":1}");
            throw new AssertionError("replay accepted an hcopykozanj handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkozanj\":1}");
            throw new AssertionError("replay accepted an hreadbackkozanj handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"sourcedevice\":1}");
            throw new AssertionError("replay accepted an sourcedevice handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hwndtarget\":1}");
            throw new AssertionError("replay accepted an hwndtarget handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomjingoji\":1}");
            throw new AssertionError("replay accepted an hgeomjingoji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyjingoji\":1}");
            throw new AssertionError("replay accepted an hcopyjingoji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackjingoji\":1}");
            throw new AssertionError("replay accepted an hreadbackjingoji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomshinnyodo\":1}");
            throw new AssertionError("replay accepted an hgeomshinnyodo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyshinnyodo\":1}");
            throw new AssertionError("replay accepted an hcopyshinnyodo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackshinnyodo\":1}");
            throw new AssertionError("replay accepted an hreadbackshinnyodo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomanrakuji\":1}");
            throw new AssertionError("replay accepted an hgeomanrakuji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyanrakuji\":1}");
            throw new AssertionError("replay accepted an hcopyanrakuji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackanrakuji\":1}");
            throw new AssertionError("replay accepted an hreadbackanrakuji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomshorenin\":1}");
            throw new AssertionError("replay accepted an hgeomshorenin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyshorenin\":1}");
            throw new AssertionError("replay accepted an hcopyshorenin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackshorenin\":1}");
            throw new AssertionError("replay accepted an hreadbackshorenin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkurodani\":1}");
            throw new AssertionError("replay accepted an hgeomkurodani handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykurodani\":1}");
            throw new AssertionError("replay accepted an hcopykurodani handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkurodani\":1}");
            throw new AssertionError("replay accepted an hreadbackkurodani handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomyoshida\":1}");
            throw new AssertionError("replay accepted an hgeomyoshida handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyyoshida\":1}");
            throw new AssertionError("replay accepted an hcopyyoshida handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackyoshida\":1}");
            throw new AssertionError("replay accepted an hreadbackyoshida handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomhyakumanben\":1}");
            throw new AssertionError("replay accepted an hgeomhyakumanben handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyhyakumanben\":1}");
            throw new AssertionError("replay accepted an hcopyhyakumanben handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackhyakumanben\":1}");
            throw new AssertionError("replay accepted an hreadbackhyakumanben handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomichijoji\":1}");
            throw new AssertionError("replay accepted an hgeomichijoji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyichijoji\":1}");
            throw new AssertionError("replay accepted an hcopyichijoji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackichijoji\":1}");
            throw new AssertionError("replay accepted an hreadbackichijoji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomtakano\":1}");
            throw new AssertionError("replay accepted an hgeomtakano handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopytakano\":1}");
            throw new AssertionError("replay accepted an hcopytakano handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacktakano\":1}");
            throw new AssertionError("replay accepted an hreadbacktakano handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomraigoin\":1}");
            throw new AssertionError("replay accepted an hgeomraigoin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyraigoin\":1}");
            throw new AssertionError("replay accepted an hcopyraigoin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackraigoin\":1}");
            throw new AssertionError("replay accepted an hreadbackraigoin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkinugasa\":1}");
            throw new AssertionError("replay accepted an hgeomkinugasa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykinugasa\":1}");
            throw new AssertionError("replay accepted an hcopykinugasa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkinugasa\":1}");
            throw new AssertionError("replay accepted an hreadbackkinugasa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomatago\":1}");
            throw new AssertionError("replay accepted an hgeomatago handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyatago\":1}");
            throw new AssertionError("replay accepted an hcopyatago handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackatago\":1}");
            throw new AssertionError("replay accepted an hreadbackatago handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomiwakura\":1}");
            throw new AssertionError("replay accepted an hgeomiwakura handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyiwakura\":1}");
            throw new AssertionError("replay accepted an hcopyiwakura handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackiwakura\":1}");
            throw new AssertionError("replay accepted an hreadbackiwakura handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomosawa\":1}");
            throw new AssertionError("replay accepted an hgeomosawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyosawa\":1}");
            throw new AssertionError("replay accepted an hcopyosawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackosawa\":1}");
            throw new AssertionError("replay accepted an hreadbackosawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomhirozawa\":1}");
            throw new AssertionError("replay accepted an hgeomhirozawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyhirozawa\":1}");
            throw new AssertionError("replay accepted an hcopyhirozawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackhirozawa\":1}");
            throw new AssertionError("replay accepted an hreadbackhirozawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomnenbutsu\":1}");
            throw new AssertionError("replay accepted an hgeomnenbutsu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopynenbutsu\":1}");
            throw new AssertionError("replay accepted an hcopynenbutsu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacknenbutsu\":1}");
            throw new AssertionError("replay accepted an hreadbacknenbutsu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkiyotaki\":1}");
            throw new AssertionError("replay accepted an hgeomkiyotaki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykiyotaki\":1}");
            throw new AssertionError("replay accepted an hcopykiyotaki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkiyotaki\":1}");
            throw new AssertionError("replay accepted an hreadbackkiyotaki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomtakaragaike\":1}");
            throw new AssertionError("replay accepted an hgeomtakaragaike handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopytakaragaike\":1}");
            throw new AssertionError("replay accepted an hcopytakaragaike handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacktakaragaike\":1}");
            throw new AssertionError("replay accepted an hreadbacktakaragaike handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeommatsugasaki\":1}");
            throw new AssertionError("replay accepted an hgeommatsugasaki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopymatsugasaki\":1}");
            throw new AssertionError("replay accepted an hcopymatsugasaki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackmatsugasaki\":1}");
            throw new AssertionError("replay accepted an hreadbackmatsugasaki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomnarutaki\":1}");
            throw new AssertionError("replay accepted an hgeomnarutaki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopynarutaki\":1}");
            throw new AssertionError("replay accepted an hcopynarutaki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacknarutaki\":1}");
            throw new AssertionError("replay accepted an hreadbacknarutaki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomtsukinowa\":1}");
            throw new AssertionError("replay accepted an hgeomtsukinowa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopytsukinowa\":1}");
            throw new AssertionError("replay accepted an hcopytsukinowa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacktsukinowa\":1}");
            throw new AssertionError("replay accepted an hreadbacktsukinowa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeommakinoo\":1}");
            throw new AssertionError("replay accepted an hgeommakinoo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopymakinoo\":1}");
            throw new AssertionError("replay accepted an hcopymakinoo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackmakinoo\":1}");
            throw new AssertionError("replay accepted an hreadbackmakinoo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomutano\":1}");
            throw new AssertionError("replay accepted an hgeomutano handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyutano\":1}");
            throw new AssertionError("replay accepted an hcopyutano handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackutano\":1}");
            throw new AssertionError("replay accepted an hreadbackutano handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomhogoin\":1}");
            throw new AssertionError("replay accepted an hgeomhogoin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyhogoin\":1}");
            throw new AssertionError("replay accepted an hcopyhogoin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackhogoin\":1}");
            throw new AssertionError("replay accepted an hreadbackhogoin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkaikoji\":1}");
            throw new AssertionError("replay accepted an hgeomkaikoji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykaikoji\":1}");
            throw new AssertionError("replay accepted an hcopykaikoji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkaikoji\":1}");
            throw new AssertionError("replay accepted an hreadbackkaikoji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomjokoin\":1}");
            throw new AssertionError("replay accepted an hgeomjokoin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyjokoin\":1}");
            throw new AssertionError("replay accepted an hcopyjokoin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackjokoin\":1}");
            throw new AssertionError("replay accepted an hreadbackjokoin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomtakiguchi\":1}");
            throw new AssertionError("replay accepted an hgeomtakiguchi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopytakiguchi\":1}");
            throw new AssertionError("replay accepted an hcopytakiguchi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacktakiguchi\":1}");
            throw new AssertionError("replay accepted an hreadbacktakiguchi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomritsumeikan\":1}");
            throw new AssertionError("replay accepted an hgeomritsumeikan handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyritsumeikan\":1}");
            throw new AssertionError("replay accepted an hcopyritsumeikan handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackritsumeikan\":1}");
            throw new AssertionError("replay accepted an hreadbackritsumeikan handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkinukake\":1}");
            throw new AssertionError("replay accepted an hgeomkinukake handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykinukake\":1}");
            throw new AssertionError("replay accepted an hcopykinukake handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkinukake\":1}");
            throw new AssertionError("replay accepted an hreadbackkinukake handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomsagaden\":1}");
            throw new AssertionError("replay accepted an hgeomsagaden handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopysagaden\":1}");
            throw new AssertionError("replay accepted an hcopysagaden handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacksagaden\":1}");
            throw new AssertionError("replay accepted an hreadbacksagaden handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkumogahata\":1}");
            throw new AssertionError("replay accepted an hgeomkumogahata handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykumogahata\":1}");
            throw new AssertionError("replay accepted an hcopykumogahata handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkumogahata\":1}");
            throw new AssertionError("replay accepted an hreadbackkumogahata handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomhanase\":1}");
            throw new AssertionError("replay accepted an hgeomhanase handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyhanase\":1}");
            throw new AssertionError("replay accepted an hcopyhanase handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackhanase\":1}");
            throw new AssertionError("replay accepted an hreadbackhanase handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomhirogawara\":1}");
            throw new AssertionError("replay accepted an hgeomhirogawara handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyhirogawara\":1}");
            throw new AssertionError("replay accepted an hcopyhirogawara handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackhirogawara\":1}");
            throw new AssertionError("replay accepted an hreadbackhirogawara handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeommidorogaike\":1}");
            throw new AssertionError("replay accepted an hgeommidorogaike handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopymidorogaike\":1}");
            throw new AssertionError("replay accepted an hcopymidorogaike handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackmidorogaike\":1}");
            throw new AssertionError("replay accepted an hreadbackmidorogaike handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomseryo\":1}");
            throw new AssertionError("replay accepted an hgeomseryo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyseryo\":1}");
            throw new AssertionError("replay accepted an hcopyseryo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackseryo\":1}");
            throw new AssertionError("replay accepted an hreadbackseryo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkebera\":1}");
            throw new AssertionError("replay accepted an hgeomkebera handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykebera\":1}");
            throw new AssertionError("replay accepted an hcopykebera handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkebera\":1}");
            throw new AssertionError("replay accepted an hreadbackkebera handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomippensuji\":1}");
            throw new AssertionError("replay accepted an hgeomippensuji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyippensuji\":1}");
            throw new AssertionError("replay accepted an hcopyippensuji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackippensuji\":1}");
            throw new AssertionError("replay accepted an hreadbackippensuji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomsennyuji\":1}");
            throw new AssertionError("replay accepted an hgeomsennyuji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopysennyuji\":1}");
            throw new AssertionError("replay accepted an hcopysennyuji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacksennyuji\":1}");
            throw new AssertionError("replay accepted an hreadbacksennyuji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomrozanji\":1}");
            throw new AssertionError("replay accepted an hgeomrozanji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyrozanji\":1}");
            throw new AssertionError("replay accepted an hcopyrozanji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackrozanji\":1}");
            throw new AssertionError("replay accepted an hreadbackrozanji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkotoin\":1}");
            throw new AssertionError("replay accepted an hgeomkotoin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykotoin\":1}");
            throw new AssertionError("replay accepted an hcopykotoin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkotoin\":1}");
            throw new AssertionError("replay accepted an hreadbackkotoin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomryogenin\":1}");
            throw new AssertionError("replay accepted an hgeomryogenin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyryogenin\":1}");
            throw new AssertionError("replay accepted an hcopyryogenin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackryogenin\":1}");
            throw new AssertionError("replay accepted an hreadbackryogenin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomzuihoin\":1}");
            throw new AssertionError("replay accepted an hgeomzuihoin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyzuihoin\":1}");
            throw new AssertionError("replay accepted an hcopyzuihoin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackzuihoin\":1}");
            throw new AssertionError("replay accepted an hreadbackzuihoin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomdaisenin\":1}");
            throw new AssertionError("replay accepted an hgeomdaisenin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopydaisenin\":1}");
            throw new AssertionError("replay accepted an hcopydaisenin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackdaisenin\":1}");
            throw new AssertionError("replay accepted an hreadbackdaisenin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkodaiji\":1}");
            throw new AssertionError("replay accepted an hgeomkodaiji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykodaiji\":1}");
            throw new AssertionError("replay accepted an hcopykodaiji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkodaiji\":1}");
            throw new AssertionError("replay accepted an hreadbackkodaiji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomentokuin\":1}");
            throw new AssertionError("replay accepted an hgeomentokuin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyentokuin\":1}");
            throw new AssertionError("replay accepted an hcopyentokuin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackentokuin\":1}");
            throw new AssertionError("replay accepted an hreadbackentokuin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomgosho\":1}");
            throw new AssertionError("replay accepted an hgeomgosho handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopygosho\":1}");
            throw new AssertionError("replay accepted an hcopygosho handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackgosho\":1}");
            throw new AssertionError("replay accepted an hreadbackgosho handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomdemachiyanagi\":1}");
            throw new AssertionError("replay accepted an hgeomdemachiyanagi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopydemachiyanagi\":1}");
            throw new AssertionError("replay accepted an hcopydemachiyanagi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackdemachiyanagi\":1}");
            throw new AssertionError("replay accepted an hreadbackdemachiyanagi handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomyoshimine\":1}");
            throw new AssertionError("replay accepted an hgeomyoshimine handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyyoshimine\":1}");
            throw new AssertionError("replay accepted an hcopyyoshimine handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackyoshimine\":1}");
            throw new AssertionError("replay accepted an hreadbackyoshimine handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomtaizoin\":1}");
            throw new AssertionError("replay accepted an hgeomtaizoin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopytaizoin\":1}");
            throw new AssertionError("replay accepted an hcopytaizoin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacktaizoin\":1}");
            throw new AssertionError("replay accepted an hreadbacktaizoin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomtenmangu\":1}");
            throw new AssertionError("replay accepted an hgeomtenmangu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopytenmangu\":1}");
            throw new AssertionError("replay accepted an hcopytenmangu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacktenmangu\":1}");
            throw new AssertionError("replay accepted an hreadbacktenmangu handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkamishichiken\":1}");
            throw new AssertionError("replay accepted an hgeomkamishichiken handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykamishichiken\":1}");
            throw new AssertionError("replay accepted an hcopykamishichiken handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkamishichiken\":1}");
            throw new AssertionError("replay accepted an hreadbackkamishichiken handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomsenbon\":1}");
            throw new AssertionError("replay accepted an hgeomsenbon handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopysenbon\":1}");
            throw new AssertionError("replay accepted an hcopysenbon handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacksenbon\":1}");
            throw new AssertionError("replay accepted an hreadbacksenbon handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkumiyama\":1}");
            throw new AssertionError("replay accepted an hgeomkumiyama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykumiyama\":1}");
            throw new AssertionError("replay accepted an hcopykumiyama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkumiyama\":1}");
            throw new AssertionError("replay accepted an hreadbackkumiyama handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomjoyo\":1}");
            throw new AssertionError("replay accepted an hgeomjoyo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyjoyo\":1}");
            throw new AssertionError("replay accepted an hcopyjoyo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackjoyo\":1}");
            throw new AssertionError("replay accepted an hreadbackjoyo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomminamiyamashiro\":1}");
            throw new AssertionError("replay accepted an hgeomminamiyamashiro handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyminamiyamashiro\":1}");
            throw new AssertionError("replay accepted an hcopyminamiyamashiro handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackminamiyamashiro\":1}");
            throw new AssertionError("replay accepted an hreadbackminamiyamashiro handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomchishakuin\":1}");
            throw new AssertionError("replay accepted an hgeomchishakuin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopychishakuin\":1}");
            throw new AssertionError("replay accepted an hcopychishakuin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackchishakuin\":1}");
            throw new AssertionError("replay accepted an hreadbackchishakuin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeommyohoin\":1}");
            throw new AssertionError("replay accepted an hgeommyohoin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopymyohoin\":1}");
            throw new AssertionError("replay accepted an hcopymyohoin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackmyohoin\":1}");
            throw new AssertionError("replay accepted an hreadbackmyohoin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomsanjusangendo\":1}");
            throw new AssertionError("replay accepted an hgeomsanjusangendo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopysanjusangendo\":1}");
            throw new AssertionError("replay accepted an hcopysanjusangendo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacksanjusangendo\":1}");
            throw new AssertionError("replay accepted an hreadbacksanjusangendo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomninenzaka\":1}");
            throw new AssertionError("replay accepted an hgeomninenzaka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyninenzaka\":1}");
            throw new AssertionError("replay accepted an hcopyninenzaka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackninenzaka\":1}");
            throw new AssertionError("replay accepted an hreadbackninenzaka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomsannenzaka\":1}");
            throw new AssertionError("replay accepted an hgeomsannenzaka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopysannenzaka\":1}");
            throw new AssertionError("replay accepted an hcopysannenzaka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacksannenzaka\":1}");
            throw new AssertionError("replay accepted an hreadbacksannenzaka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkeage\":1}");
            throw new AssertionError("replay accepted an hgeomkeage handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykeage\":1}");
            throw new AssertionError("replay accepted an hcopykeage handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkeage\":1}");
            throw new AssertionError("replay accepted an hreadbackkeage handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomtetsugaku\":1}");
            throw new AssertionError("replay accepted an hgeomtetsugaku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopytetsugaku\":1}");
            throw new AssertionError("replay accepted an hcopytetsugaku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacktetsugaku\":1}");
            throw new AssertionError("replay accepted an hreadbacktetsugaku handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomshogunzuka\":1}");
            throw new AssertionError("replay accepted an hgeomshogunzuka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyshogunzuka\":1}");
            throw new AssertionError("replay accepted an hcopyshogunzuka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackshogunzuka\":1}");
            throw new AssertionError("replay accepted an hreadbackshogunzuka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomchawanzaka\":1}");
            throw new AssertionError("replay accepted an hgeomchawanzaka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopychawanzaka\":1}");
            throw new AssertionError("replay accepted an hcopychawanzaka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackchawanzaka\":1}");
            throw new AssertionError("replay accepted an hreadbackchawanzaka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomreikanji\":1}");
            throw new AssertionError("replay accepted an hgeomreikanji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyreikanji\":1}");
            throw new AssertionError("replay accepted an hcopyreikanji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackreikanji\":1}");
            throw new AssertionError("replay accepted an hreadbackreikanji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomchionji\":1}");
            throw new AssertionError("replay accepted an hgeomchionji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopychionji\":1}");
            throw new AssertionError("replay accepted an hcopychionji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackchionji\":1}");
            throw new AssertionError("replay accepted an hreadbackchionji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomnyakuoji\":1}");
            throw new AssertionError("replay accepted an hgeomnyakuoji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopynyakuoji\":1}");
            throw new AssertionError("replay accepted an hcopynyakuoji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacknyakuoji\":1}");
            throw new AssertionError("replay accepted an hreadbacknyakuoji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomshishigatani\":1}");
            throw new AssertionError("replay accepted an hgeomshishigatani handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyshishigatani\":1}");
            throw new AssertionError("replay accepted an hcopyshishigatani handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackshishigatani\":1}");
            throw new AssertionError("replay accepted an hreadbackshishigatani handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomtanukidani\":1}");
            throw new AssertionError("replay accepted an hgeomtanukidani handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopytanukidani\":1}");
            throw new AssertionError("replay accepted an hcopytanukidani handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacktanukidani\":1}");
            throw new AssertionError("replay accepted an hreadbacktanukidani handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkaguraoka\":1}");
            throw new AssertionError("replay accepted an hgeomkaguraoka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykaguraoka\":1}");
            throw new AssertionError("replay accepted an hcopykaguraoka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkaguraoka\":1}");
            throw new AssertionError("replay accepted an hreadbackkaguraoka handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomjodoji\":1}");
            throw new AssertionError("replay accepted an hgeomjodoji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyjodoji\":1}");
            throw new AssertionError("replay accepted an hcopyjodoji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackjodoji\":1}");
            throw new AssertionError("replay accepted an hreadbackjodoji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomreisen\":1}");
            throw new AssertionError("replay accepted an hgeomreisen handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyreisen\":1}");
            throw new AssertionError("replay accepted an hcopyreisen handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackreisen\":1}");
            throw new AssertionError("replay accepted an hreadbackreisen handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomshogoin\":1}");
            throw new AssertionError("replay accepted an hgeomshogoin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyshogoin\":1}");
            throw new AssertionError("replay accepted an hcopyshogoin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackshogoin\":1}");
            throw new AssertionError("replay accepted an hreadbackshogoin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkonkaikomyo\":1}");
            throw new AssertionError("replay accepted an hgeomkonkaikomyo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykonkaikomyo\":1}");
            throw new AssertionError("replay accepted an hcopykonkaikomyo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkonkaikomyo\":1}");
            throw new AssertionError("replay accepted an hreadbackkonkaikomyo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomshinmonzen\":1}");
            throw new AssertionError("replay accepted an hgeomshinmonzen handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyshinmonzen\":1}");
            throw new AssertionError("replay accepted an hcopyshinmonzen handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackshinmonzen\":1}");
            throw new AssertionError("replay accepted an hreadbackshinmonzen handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomfurumonzen\":1}");
            throw new AssertionError("replay accepted an hgeomfurumonzen handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyfurumonzen\":1}");
            throw new AssertionError("replay accepted an hcopyfurumonzen handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackfurumonzen\":1}");
            throw new AssertionError("replay accepted an hreadbackfurumonzen handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomtakasegawa\":1}");
            throw new AssertionError("replay accepted an hgeomtakasegawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopytakasegawa\":1}");
            throw new AssertionError("replay accepted an hcopytakasegawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacktakasegawa\":1}");
            throw new AssertionError("replay accepted an hreadbacktakasegawa handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomshisendo\":1}");
            throw new AssertionError("replay accepted an hgeomshisendo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyshisendo\":1}");
            throw new AssertionError("replay accepted an hcopyshisendo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackshisendo\":1}");
            throw new AssertionError("replay accepted an hreadbackshisendo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeommanshuin\":1}");
            throw new AssertionError("replay accepted an hgeommanshuin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopymanshuin\":1}");
            throw new AssertionError("replay accepted an hcopymanshuin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackmanshuin\":1}");
            throw new AssertionError("replay accepted an hreadbackmanshuin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomenkoji\":1}");
            throw new AssertionError("replay accepted an hgeomenkoji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyenkoji\":1}");
            throw new AssertionError("replay accepted an hcopyenkoji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackenkoji\":1}");
            throw new AssertionError("replay accepted an hreadbackenkoji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomhosenin\":1}");
            throw new AssertionError("replay accepted an hgeomhosenin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyhosenin\":1}");
            throw new AssertionError("replay accepted an hcopyhosenin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackhosenin\":1}");
            throw new AssertionError("replay accepted an hreadbackhosenin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkifune\":1}");
            throw new AssertionError("replay accepted an hgeomkifune handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykifune\":1}");
            throw new AssertionError("replay accepted an hcopykifune handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkifune\":1}");
            throw new AssertionError("replay accepted an hreadbackkifune handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomshizuhara\":1}");
            throw new AssertionError("replay accepted an hgeomshizuhara handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyshizuhara\":1}");
            throw new AssertionError("replay accepted an hcopyshizuhara handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackshizuhara\":1}");
            throw new AssertionError("replay accepted an hreadbackshizuhara handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeommatsuo\":1}");
            throw new AssertionError("replay accepted an hgeommatsuo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopymatsuo\":1}");
            throw new AssertionError("replay accepted an hcopymatsuo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackmatsuo\":1}");
            throw new AssertionError("replay accepted an hreadbackmatsuo handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkyotoeki\":1}");
            throw new AssertionError("replay accepted an hgeomkyotoeki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykyotoeki\":1}");
            throw new AssertionError("replay accepted an hcopykyotoeki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkyotoeki\":1}");
            throw new AssertionError("replay accepted an hreadbackkyotoeki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomhanamikoji\":1}");
            throw new AssertionError("replay accepted an hgeomhanamikoji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyhanamikoji\":1}");
            throw new AssertionError("replay accepted an hcopyhanamikoji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackhanamikoji\":1}");
            throw new AssertionError("replay accepted an hreadbackhanamikoji handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeommiyagawacho\":1}");
            throw new AssertionError("replay accepted an hgeommiyagawacho handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopymiyagawacho\":1}");
            throw new AssertionError("replay accepted an hcopymiyagawacho handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackmiyagawacho\":1}");
            throw new AssertionError("replay accepted an hreadbackmiyagawacho handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomsujin\":1}");
            throw new AssertionError("replay accepted an hgeomsujin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopysujin\":1}");
            throw new AssertionError("replay accepted an hcopysujin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacksujin\":1}");
            throw new AssertionError("replay accepted an hreadbacksujin handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomsento\":1}");
            throw new AssertionError("replay accepted an hgeomsento handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopysento\":1}");
            throw new AssertionError("replay accepted an hcopysento handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacksento\":1}");
            throw new AssertionError("replay accepted an hreadbacksento handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomgyoen\":1}");
            throw new AssertionError("replay accepted an hgeomgyoen handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopygyoen\":1}");
            throw new AssertionError("replay accepted an hcopygyoen handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackgyoen\":1}");
            throw new AssertionError("replay accepted an hreadbackgyoen handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomkawadoko\":1}");
            throw new AssertionError("replay accepted an hgeomkawadoko handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopykawadoko\":1}");
            throw new AssertionError("replay accepted an hcopykawadoko handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackkawadoko\":1}");
            throw new AssertionError("replay accepted an hreadbackkawadoko handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomyamazaki\":1}");
            throw new AssertionError("replay accepted an hgeomyamazaki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyyamazaki\":1}");
            throw new AssertionError("replay accepted an hcopyyamazaki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackyamazaki\":1}");
            throw new AssertionError("replay accepted an hreadbackyamazaki handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomashiya\":1}");
            throw new AssertionError("replay accepted an hgeomashiya handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyashiya\":1}");
            throw new AssertionError("replay accepted an hcopyashiya handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackashiya\":1}");
            throw new AssertionError("replay accepted an hreadbackashiya handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomarima\":1}");
            throw new AssertionError("replay accepted an hgeomarima handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyarima\":1}");
            throw new AssertionError("replay accepted an hcopyarima handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackarima\":1}");
            throw new AssertionError("replay accepted an hreadbackarima handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomrokko\":1}");
            throw new AssertionError("replay accepted an hgeomrokko handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopyrokko\":1}");
            throw new AssertionError("replay accepted an hcopyrokko handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbackrokko\":1}");
            throw new AssertionError("replay accepted an hreadbackrokko handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hgeomsuma\":1}");
            throw new AssertionError("replay accepted an hgeomsuma handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hcopysuma\":1}");
            throw new AssertionError("replay accepted an hcopysuma handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        try {
            SceneReplay.replay("{\"schemaVersion\":1,\"hreadbacksuma\":1}");
            throw new AssertionError("replay accepted an hreadbacksuma handle");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
    }
}

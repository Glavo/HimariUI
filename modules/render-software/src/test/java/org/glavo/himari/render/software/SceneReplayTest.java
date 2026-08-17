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
    }
}

package org.glavo.himari.render.software;

import org.glavo.himari.graphics.SceneEnvelope;
import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Replays a canonical scene document onto a fresh software surface.
@NotNullByDefault
public final class SceneReplay {
    /// Prevents instantiation.
    private SceneReplay() {
    }

    /// Parses `json` and rasters it without ambient fonts or producer objects.
    ///
    /// @param json the canonical scene document
    /// @return the rasterized surface
    public static SoftwareSurface replay(String json) {
        Objects.requireNonNull(json, "json");
        rejectProducerHandles(json);
        SceneEnvelope envelope = SceneEnvelope.parse(json);
        SoftwareSurface surface = new SoftwareSurface(envelope.width(), envelope.height());
        surface.replay(envelope.displayList());
        return surface;
    }

    /// Rejects documents that name producer-process handles or ambient font objects.
    ///
    /// @param json the document
    static void rejectProducerHandles(String json) {
        String lower = json.toLowerCase();
        if (lower.contains("hwnd")
                || lower.contains("memorysegment")
                || lower.contains("nativehandle")
                || lower.contains("systemfont")
                || lower.contains("hdc")
                || lower.contains("hmonitor")
                || lower.contains("id3d12")
                || lower.contains("vkdevice")
                || lower.contains("hbitmap")
                || lower.contains("hfont")
                || lower.contains("hicon")
                || lower.contains("himc")
                || lower.contains("hcursor")
                || lower.contains("hmenu")
                || lower.contains("hbrush")
                || lower.contains("hrgn")
                || lower.contains("hpalette")
                || lower.contains("hpen")
                || lower.contains("haccel")
                || lower.contains("hinstance")
                || lower.contains("hhook")
                || lower.contains("hkl")
                || lower.contains("hglobal")
                || lower.contains("hmodule")
                || lower.contains("hthread")
                || lower.contains("hprocess")
                || lower.contains("hfile")
                || lower.contains("hkey")
                || lower.contains("hdrop")
                || lower.contains("hdcmem")
                || lower.contains("hdesk")
                || lower.contains("htoken")
                || lower.contains("hmutex")
                || lower.contains("idxgi")) {
            throw new IllegalArgumentException("Scene replay rejects producer-process handles");
        }
    }
}

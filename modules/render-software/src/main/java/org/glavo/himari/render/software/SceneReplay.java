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
        SceneEnvelope envelope = SceneEnvelope.parse(Objects.requireNonNull(json, "json"));
        SoftwareSurface surface = new SoftwareSurface(envelope.width(), envelope.height());
        surface.replay(envelope.displayList());
        return surface;
    }
}

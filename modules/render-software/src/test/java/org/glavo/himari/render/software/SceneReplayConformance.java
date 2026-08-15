package org.glavo.himari.render.software;

import org.glavo.himari.graphics.Color;
import org.glavo.himari.graphics.DisplayList;
import org.glavo.himari.graphics.DisplayListOp;
import org.glavo.himari.graphics.SceneEnvelope;
import org.jetbrains.annotations.NotNullByDefault;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/// Writes M10 offline scene-replay evidence.
@NotNullByDefault
public final class SceneReplayConformance {
    /// Prevents instantiation.
    private SceneReplayConformance() {
    }

    /// Replays a canonical scene in a fresh surface and writes the report.
    ///
    /// @param arguments one output directory
    /// @throws Exception if the profile fails
    public static void main(String[] arguments) throws Exception {
        if (arguments.length != 1) {
            throw new IllegalArgumentException("Expected one output directory");
        }
        SceneEnvelope envelope = new SceneEnvelope(
                SceneEnvelope.CURRENT_SCHEMA,
                16,
                16,
                new DisplayList(List.of(new DisplayListOp.FillRect(2.0f, 2.0f, 8.0f, 8.0f, Color.SRGB_WHITE)))
        );
        String json = envelope.toCanonicalJson();
        SoftwareSurface first = SceneReplay.replay(json);
        SoftwareSurface second = SceneReplay.replay(SceneEnvelope.parse(json).toCanonicalJson());
        float[] a = first.extendedLinearPremultiplied();
        float[] b = second.extendedLinearPremultiplied();
        if (a.length != b.length) {
            throw new IllegalStateException("Replay pixel counts differ");
        }
        for (int index = 0; index < a.length; index++) {
            if (a[index] != b[index]) {
                throw new IllegalStateException("Replay pixels are not deterministic");
            }
        }
        Path output = Path.of(arguments[0]);
        Files.createDirectories(output);
        Files.writeString(output.resolve("scene.json"), json, StandardCharsets.UTF_8);
        Files.writeString(
                output.resolve("results.json"),
                """
                        {
                          "profile": "m10-replay",
                          "workPackage": "REPLAY-001",
                          "status": "passed",
                          "width": 16,
                          "height": 16,
                          "deterministic": true
                        }
                        """,
                StandardCharsets.UTF_8
        );
    }
}

package org.glavo.himari.graphics;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Verifies canonical scene encoding and parse.
@NotNullByDefault
final class SceneEnvelopeTest {
    /// Round-trips a rectangle display list.
    @Test
    void roundTripsFillRect() {
        DisplayList list = new DisplayList(List.of(
                new DisplayListOp.FillRect(1.0f, 2.0f, 3.0f, 4.0f, Color.srgb(0.1f, 0.2f, 0.3f, 1.0f))
        ));
        SceneEnvelope envelope = new SceneEnvelope(1, 16, 16, list);
        String json = envelope.toCanonicalJson();
        assertEquals(json, SceneEnvelope.parse(json).toCanonicalJson());
    }
}

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
}

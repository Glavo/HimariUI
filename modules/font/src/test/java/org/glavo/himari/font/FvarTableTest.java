package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Verifies `fvar` instance access through [`SfntFont#variationAxes()`].
@NotNullByDefault
final class FvarTableTest {
    /// Reads the constructed `wght` axis and default instance.
    @Test
    void readsWeightAxisDefault() {
        SfntFont font = FvarSampleFont.create();
        List<VariationAxis> axes = font.variationAxes();
        assertEquals(1, axes.size());
        assertEquals(FvarSampleFont.TAG_WGHT, axes.getFirst().tag());
        assertEquals("wght", axes.getFirst().tagString());
        assertEquals(FvarSampleFont.MIN_WEIGHT, axes.getFirst().minValue(), 0.01f);
        assertEquals(FvarSampleFont.DEFAULT_WEIGHT, axes.getFirst().defaultValue(), 0.01f);
        assertEquals(FvarSampleFont.MAX_WEIGHT, axes.getFirst().maxValue(), 0.01f);
        float[] instance = font.defaultVariation();
        assertEquals(1, instance.length);
        assertEquals(FvarSampleFont.DEFAULT_WEIGHT, instance[0], 0.01f);
        assertEquals(FvarSampleFont.GLYPH_A, font.glyphId('A'));
    }
}

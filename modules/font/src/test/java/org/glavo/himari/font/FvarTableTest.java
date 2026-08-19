package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        List<StatAxis> statAxes = font.statAxes();
        assertEquals(2, statAxes.size());
        assertEquals(FvarSampleFont.TAG_WGHT, statAxes.getFirst().tag());
        assertEquals(FvarSampleFont.STAT_AXIS_NAME_ID, statAxes.getFirst().nameId());
        assertEquals(FvarSampleFont.TAG_WDTH, statAxes.get(1).tag());
        assertEquals(FvarSampleFont.STAT_WIDTH_NAME_ID, statAxes.get(1).nameId());
        List<StatNamedInstance> instances = font.statNamedInstances();
        assertEquals(4, instances.size());
        assertEquals(FvarSampleFont.STAT_INSTANCE_NAME_ID, instances.getFirst().nameId());
        assertEquals(0, instances.getFirst().axisIndex());
        assertEquals(1, instances.getFirst().format());
        assertTrue(instances.getFirst().elidableAxisValueName());
        assertEquals(FvarSampleFont.DEFAULT_WEIGHT, instances.getFirst().value(), 0.01f);
        assertEquals(2, instances.get(1).format());
        assertEquals(FvarSampleFont.STAT_LIGHT_NAME_ID, instances.get(1).nameId());
        assertEquals(FvarSampleFont.STAT_LIGHT_VALUE, instances.get(1).value(), 0.01f);
        assertEquals(FvarSampleFont.STAT_LIGHT_MIN, instances.get(1).rangeMin(), 0.01f);
        assertEquals(FvarSampleFont.STAT_LIGHT_MAX, instances.get(1).rangeMax(), 0.01f);
        assertEquals(3, instances.get(2).format());
        assertTrue(instances.get(2).olderSiblingFontAttribute());
        assertEquals(FvarSampleFont.STAT_BOLD_NAME_ID, instances.get(2).nameId());
        assertEquals(FvarSampleFont.STAT_BOLD_VALUE, instances.get(2).value(), 0.01f);
        assertEquals(FvarSampleFont.STAT_BOLD_LINKED, instances.get(2).linkedValue(), 0.01f);
        assertEquals(4, instances.get(3).format());
        assertEquals(FvarSampleFont.STAT_BLACK_NAME_ID, instances.get(3).nameId());
        assertEquals(FvarSampleFont.STAT_BLACK_VALUE, instances.get(3).value(), 0.01f);
        assertEquals(1, instances.get(3).extraAxisIndices().length);
        assertEquals(1, instances.get(3).extraAxisIndices()[0]);
        assertEquals(FvarSampleFont.STAT_BLACK_WIDTH, instances.get(3).extraValues()[0], 0.01f);
        assertEquals(FvarSampleFont.STAT_ELIDED_FALLBACK_NAME_ID, font.statElidedFallbackNameId());
    }
}

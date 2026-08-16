package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/// Verifies COLR v1 flatten through [`SfntFont#colorLayers(int)`].
@NotNullByDefault
final class ColrV1Test {
    /// Flattens `PaintColrLayers` of two `PaintGlyph`/`PaintSolid` pairs.
    @Test
    void flattensPaintColrLayersThroughShippedColorLayers() {
        SfntFont font = ColrV1SampleFont.create();
        assertEquals(ColrV1SampleFont.GLYPH_BASE, font.glyphId('A'));
        List<ColorLayer> layers = font.colorLayers(ColrV1SampleFont.GLYPH_BASE);
        assertEquals(2, layers.size());
        assertEquals(ColrV1SampleFont.GLYPH_BACK, layers.get(0).glyphId());
        PaletteColor red = layers.get(0).color();
        assertNotNull(red);
        assertEquals(255, red.red());
        assertEquals(0, red.green());
        assertEquals(0, red.blue());
        assertEquals(ColrV1SampleFont.GLYPH_FRONT, layers.get(1).glyphId());
        PaletteColor blue = layers.get(1).color();
        assertNotNull(blue);
        assertEquals(0, blue.red());
        assertEquals(0, blue.green());
        assertEquals(255, blue.blue());
        assertEquals(0, font.colorLayers(ColrV1SampleFont.GLYPH_BACK).size());
    }
}

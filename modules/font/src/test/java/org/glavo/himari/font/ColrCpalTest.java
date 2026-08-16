package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies COLR v0 layers through the shipped [`SfntFont#colorLayers(int)`] entry.
@NotNullByDefault
final class ColrCpalTest {
    /// Returns the constructed red then blue layers for `A`.
    @Test
    void layersBaseGlyphFromPalette() {
        SfntFont font = ColrSampleFont.create();
        assertEquals(ColrSampleFont.GLYPH_BASE, font.glyphId('A'));
        List<ColorLayer> layers = font.colorLayers(ColrSampleFont.GLYPH_BASE);
        assertEquals(2, layers.size());
        assertEquals(ColrSampleFont.GLYPH_BACK, layers.get(0).glyphId());
        assertEquals(0, layers.get(0).paletteIndex());
        PaletteColor red = layers.get(0).color();
        assertNotNull(red);
        assertEquals(255, red.red());
        assertEquals(0, red.green());
        assertEquals(0, red.blue());
        assertEquals(255, red.alpha());
        assertEquals(ColrSampleFont.GLYPH_FRONT, layers.get(1).glyphId());
        PaletteColor blue = layers.get(1).color();
        assertNotNull(blue);
        assertEquals(0, blue.red());
        assertEquals(0, blue.green());
        assertEquals(255, blue.blue());
        CollectingPen pen = new CollectingPen();
        font.outline(ColrSampleFont.GLYPH_BACK, pen);
        assertTrue(pen.commands().size() > 1);
        assertTrue(font.colorLayers(ColrSampleFont.GLYPH_BACK).isEmpty());
    }

    /// Treats palette index `0xFFFF` as a foreground layer without a CPAL color.
    @Test
    void foregroundSentinelHasNoPaletteColor() {
        @Nullable PaletteColor color = ColrSampleFont.create().paletteColor(0, PaletteColor.FOREGROUND);
        assertNull(color);
    }
}

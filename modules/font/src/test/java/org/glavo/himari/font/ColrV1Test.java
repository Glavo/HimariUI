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

    /// Unwraps `PaintTranslate` onto the nested `PaintGlyph`/`PaintSolid`.
    @Test
    void flattensPaintTranslateThroughShippedColorLayers() {
        SfntFont font = ColrV1TranslateSampleFont.create();
        assertEquals(ColrV1TranslateSampleFont.GLYPH_BASE, font.glyphId('A'));
        List<ColorLayer> layers = font.colorLayers(ColrV1TranslateSampleFont.GLYPH_BASE);
        assertEquals(1, layers.size());
        assertEquals(ColrV1TranslateSampleFont.GLYPH_BACK, layers.getFirst().glyphId());
        PaletteColor red = layers.getFirst().color();
        assertNotNull(red);
        assertEquals(255, red.red());
        assertEquals(0, red.green());
        assertEquals(0, red.blue());
    }

    /// Fills `PaintGlyph` from the first `PaintLinearGradient` color stop.
    @Test
    void flattensPaintLinearGradientThroughShippedColorLayers() {
        SfntFont font = ColrV1GradientSampleFont.create();
        assertEquals(ColrV1GradientSampleFont.GLYPH_BASE, font.glyphId('A'));
        List<ColorLayer> layers = font.colorLayers(ColrV1GradientSampleFont.GLYPH_BASE);
        assertEquals(1, layers.size());
        assertEquals(ColrV1GradientSampleFont.GLYPH_BACK, layers.getFirst().glyphId());
        PaletteColor red = layers.getFirst().color();
        assertNotNull(red);
        assertEquals(255, red.red());
        assertEquals(0, red.green());
        assertEquals(0, red.blue());
    }

    /// Unwraps `PaintScale` onto the nested `PaintGlyph`/`PaintSolid`.
    @Test
    void flattensPaintScaleThroughShippedColorLayers() {
        SfntFont font = ColrV1ScaleSampleFont.create();
        assertEquals(ColrV1ScaleSampleFont.GLYPH_BASE, font.glyphId('A'));
        List<ColorLayer> layers = font.colorLayers(ColrV1ScaleSampleFont.GLYPH_BASE);
        assertEquals(1, layers.size());
        assertEquals(ColrV1ScaleSampleFont.GLYPH_BACK, layers.getFirst().glyphId());
        PaletteColor red = layers.getFirst().color();
        assertNotNull(red);
        assertEquals(255, red.red());
        assertEquals(0, red.green());
        assertEquals(0, red.blue());
    }

    /// Unwraps `PaintRotate` onto the nested `PaintGlyph`/`PaintSolid`.
    @Test
    void flattensPaintRotateThroughShippedColorLayers() {
        SfntFont font = ColrV1RotateSampleFont.create();
        List<ColorLayer> layers = font.colorLayers(ColrV1RotateSampleFont.GLYPH_BASE);
        assertEquals(1, layers.size());
        assertEquals(ColrV1RotateSampleFont.GLYPH_BACK, layers.getFirst().glyphId());
        PaletteColor red = layers.getFirst().color();
        assertNotNull(red);
        assertEquals(255, red.red());
    }

    /// Unwraps `PaintTransform` onto the nested `PaintGlyph`/`PaintSolid`.
    @Test
    void flattensPaintTransformThroughShippedColorLayers() {
        SfntFont font = ColrV1TransformSampleFont.create();
        List<ColorLayer> layers = font.colorLayers(ColrV1TransformSampleFont.GLYPH_BASE);
        assertEquals(1, layers.size());
        assertEquals(ColrV1TransformSampleFont.GLYPH_BACK, layers.getFirst().glyphId());
        PaletteColor red = layers.getFirst().color();
        assertNotNull(red);
        assertEquals(255, red.red());
    }

    /// Fills `PaintGlyph` from the first `PaintRadialGradient` color stop.
    @Test
    void flattensPaintRadialGradientThroughShippedColorLayers() {
        SfntFont font = ColrV1RadialSampleFont.create();
        List<ColorLayer> layers = font.colorLayers(ColrV1RadialSampleFont.GLYPH_BASE);
        assertEquals(1, layers.size());
        assertEquals(ColrV1RadialSampleFont.GLYPH_BACK, layers.getFirst().glyphId());
        PaletteColor red = layers.getFirst().color();
        assertNotNull(red);
        assertEquals(255, red.red());
    }

    /// Fills `PaintGlyph` from the first `PaintSweepGradient` color stop.
    @Test
    void flattensPaintSweepGradientThroughShippedColorLayers() {
        SfntFont font = ColrV1SweepSampleFont.create();
        List<ColorLayer> layers = font.colorLayers(ColrV1SweepSampleFont.GLYPH_BASE);
        assertEquals(1, layers.size());
        assertEquals(ColrV1SweepSampleFont.GLYPH_BACK, layers.getFirst().glyphId());
        PaletteColor red = layers.getFirst().color();
        assertNotNull(red);
        assertEquals(255, red.red());
    }

    /// Flattens `PaintComposite` backdrop then source through [`SfntFont#colorLayers(int)`].
    @Test
    void flattensPaintCompositeThroughShippedColorLayers() {
        SfntFont font = ColrV1CompositeSampleFont.create();
        List<ColorLayer> layers = font.colorLayers(ColrV1CompositeSampleFont.GLYPH_BASE);
        assertEquals(2, layers.size());
        assertEquals(ColrV1CompositeSampleFont.GLYPH_FRONT, layers.get(0).glyphId());
        PaletteColor blue = layers.get(0).color();
        assertNotNull(blue);
        assertEquals(255, blue.blue());
        assertEquals(ColrV1CompositeSampleFont.GLYPH_BACK, layers.get(1).glyphId());
        PaletteColor red = layers.get(1).color();
        assertNotNull(red);
        assertEquals(255, red.red());
    }

    /// Applies a COLR ItemVariationStore delta to `PaintVarTranslate` `dx` at peak `wght`.
    @Test
    void appliesPaintVarTranslateDeltaThroughShippedColorLayers() {
        SfntFont font = ColrV1VarTranslateDeltaSampleFont.create();
        List<ColorLayer> defaults = font.colorLayers(ColrV1VarTranslateDeltaSampleFont.GLYPH_BASE);
        assertEquals(1, defaults.size());
        assertEquals(ColrV1VarTranslateDeltaSampleFont.BASE_TRANSLATE_X, defaults.getFirst().translateX());
        List<ColorLayer> peak = font.colorLayers(
                ColrV1VarTranslateDeltaSampleFont.GLYPH_BASE,
                0,
                new float[] {ColrV1VarTranslateDeltaSampleFont.MAX_WEIGHT}
        );
        assertEquals(1, peak.size());
        assertEquals(
                ColrV1VarTranslateDeltaSampleFont.BASE_TRANSLATE_X
                        + ColrV1VarTranslateDeltaSampleFont.TRANSLATE_DELTA,
                peak.getFirst().translateX()
        );
    }

    /// Unwraps `PaintVarTranslate` onto the nested `PaintGlyph`/`PaintSolid`.
    @Test
    void flattensPaintVarTranslateThroughShippedColorLayers() {
        SfntFont font = ColrV1VarTranslateSampleFont.create();
        List<ColorLayer> layers = font.colorLayers(ColrV1VarTranslateSampleFont.GLYPH_BASE);
        assertEquals(1, layers.size());
        assertEquals(ColrV1VarTranslateSampleFont.GLYPH_BACK, layers.getFirst().glyphId());
        PaletteColor red = layers.getFirst().color();
        assertNotNull(red);
        assertEquals(255, red.red());
    }

    /// Unwraps `PaintVarScale` onto the nested `PaintGlyph`/`PaintSolid`.
    @Test
    void flattensPaintVarScaleThroughShippedColorLayers() {
        SfntFont font = ColrV1VarScaleSampleFont.create();
        List<ColorLayer> layers = font.colorLayers(ColrV1VarScaleSampleFont.GLYPH_BASE);
        assertEquals(1, layers.size());
        assertEquals(ColrV1VarScaleSampleFont.GLYPH_BACK, layers.getFirst().glyphId());
        PaletteColor red = layers.getFirst().color();
        assertNotNull(red);
        assertEquals(255, red.red());
    }

    /// Unwraps `PaintVarRotate` onto the nested `PaintGlyph`/`PaintSolid`.
    @Test
    void flattensPaintVarRotateThroughShippedColorLayers() {
        SfntFont font = ColrV1VarRotateSampleFont.create();
        List<ColorLayer> layers = font.colorLayers(ColrV1VarRotateSampleFont.GLYPH_BASE);
        assertEquals(1, layers.size());
        assertEquals(ColrV1VarRotateSampleFont.GLYPH_BACK, layers.getFirst().glyphId());
        PaletteColor red = layers.getFirst().color();
        assertNotNull(red);
        assertEquals(255, red.red());
    }

    /// Unwraps `PaintVarTransform` onto the nested `PaintGlyph`/`PaintSolid`.
    @Test
    void flattensPaintVarTransformThroughShippedColorLayers() {
        SfntFont font = ColrV1VarTransformSampleFont.create();
        List<ColorLayer> layers = font.colorLayers(ColrV1VarTransformSampleFont.GLYPH_BASE);
        assertEquals(1, layers.size());
        assertEquals(ColrV1VarTransformSampleFont.GLYPH_BACK, layers.getFirst().glyphId());
        PaletteColor red = layers.getFirst().color();
        assertNotNull(red);
        assertEquals(255, red.red());
    }

    /// Fills `PaintGlyph` from the first `PaintVarLinearGradient` color stop.
    @Test
    void flattensPaintVarLinearGradientThroughShippedColorLayers() {
        SfntFont font = ColrV1VarLinearSampleFont.create();
        List<ColorLayer> layers = font.colorLayers(ColrV1VarLinearSampleFont.GLYPH_BASE);
        assertEquals(1, layers.size());
        assertEquals(ColrV1VarLinearSampleFont.GLYPH_BACK, layers.getFirst().glyphId());
        PaletteColor red = layers.getFirst().color();
        assertNotNull(red);
        assertEquals(255, red.red());
    }

    /// Fills `PaintGlyph` from the first `PaintVarRadialGradient` color stop.
    @Test
    void flattensPaintVarRadialGradientThroughShippedColorLayers() {
        SfntFont font = ColrV1VarRadialSampleFont.create();
        List<ColorLayer> layers = font.colorLayers(ColrV1VarRadialSampleFont.GLYPH_BASE);
        assertEquals(1, layers.size());
        assertEquals(ColrV1VarRadialSampleFont.GLYPH_BACK, layers.getFirst().glyphId());
        PaletteColor red = layers.getFirst().color();
        assertNotNull(red);
        assertEquals(255, red.red());
    }

    /// Fills `PaintGlyph` from the first `PaintVarSweepGradient` color stop.
    @Test
    void flattensPaintVarSweepGradientThroughShippedColorLayers() {
        SfntFont font = ColrV1VarSweepSampleFont.create();
        List<ColorLayer> layers = font.colorLayers(ColrV1VarSweepSampleFont.GLYPH_BASE);
        assertEquals(1, layers.size());
        assertEquals(ColrV1VarSweepSampleFont.GLYPH_BACK, layers.getFirst().glyphId());
        PaletteColor red = layers.getFirst().color();
        assertNotNull(red);
        assertEquals(255, red.red());
    }

    /// Applies a COLR ItemVariationStore delta to a `VarColorLine` first stop at peak `wght`.
    @Test
    void appliesVarColorLineDeltaThroughShippedColorLayers() {
        SfntFont font = ColrV1VarLinearDeltaSampleFont.create();
        List<ColorLayer> defaults = font.colorLayers(ColrV1VarLinearDeltaSampleFont.GLYPH_BASE);
        assertEquals(1, defaults.size());
        assertEquals(0, defaults.getFirst().paletteIndex());
        PaletteColor red = defaults.getFirst().color();
        assertNotNull(red);
        assertEquals(255, red.red());
        List<ColorLayer> peak = font.colorLayers(
                ColrV1VarLinearDeltaSampleFont.GLYPH_BASE,
                0,
                new float[] {ColrV1VarLinearDeltaSampleFont.MAX_WEIGHT}
        );
        assertEquals(1, peak.size());
        assertEquals(1, peak.getFirst().paletteIndex());
        PaletteColor blue = peak.getFirst().color();
        assertNotNull(blue);
        assertEquals(255, blue.blue());
    }

    /// Applies a COLR ItemVariationStore delta to `PaintVarSolid` at peak `wght`.
    @Test
    void appliesPaintVarSolidDeltaThroughShippedColorLayers() {
        SfntFont font = ColrV1VarSolidSampleFont.create();
        List<ColorLayer> defaults = font.colorLayers(ColrV1VarSolidSampleFont.GLYPH_BASE);
        assertEquals(1, defaults.size());
        assertEquals(0, defaults.getFirst().paletteIndex());
        PaletteColor red = defaults.getFirst().color();
        assertNotNull(red);
        assertEquals(255, red.red());
        List<ColorLayer> peak = font.colorLayers(
                ColrV1VarSolidSampleFont.GLYPH_BASE,
                0,
                new float[] {ColrV1VarSolidSampleFont.MAX_WEIGHT}
        );
        assertEquals(1, peak.size());
        assertEquals(1, peak.getFirst().paletteIndex());
        PaletteColor blue = peak.getFirst().color();
        assertNotNull(blue);
        assertEquals(255, blue.blue());
    }

    /// Follows `PaintColrGlyph` into the referenced base paint graph.
    @Test
    void flattensPaintColrGlyphThroughShippedColorLayers() {
        SfntFont font = ColrV1ColrGlyphSampleFont.create();
        assertEquals(ColrV1ColrGlyphSampleFont.GLYPH_BASE, font.glyphId('A'));
        List<ColorLayer> layers = font.colorLayers(ColrV1ColrGlyphSampleFont.GLYPH_BASE);
        assertEquals(1, layers.size());
        assertEquals(ColrV1ColrGlyphSampleFont.GLYPH_BACK, layers.getFirst().glyphId());
        PaletteColor red = layers.getFirst().color();
        assertNotNull(red);
        assertEquals(255, red.red());
        assertEquals(0, red.green());
        assertEquals(0, red.blue());
    }
}

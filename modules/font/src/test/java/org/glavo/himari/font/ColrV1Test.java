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

    /// Applies a COLR ItemVariationStore delta to `PaintVarScale` `scaleX` at peak `wght`.
    @Test
    void appliesPaintVarScaleDeltaThroughShippedColorLayers() {
        SfntFont font = ColrV1VarScaleDeltaSampleFont.create();
        List<ColorLayer> defaults = font.colorLayers(ColrV1VarScaleDeltaSampleFont.GLYPH_BASE);
        assertEquals(1, defaults.size());
        assertEquals(ColrV1VarScaleDeltaSampleFont.BASE_SCALE_X, defaults.getFirst().scaleX());
        List<ColorLayer> peak = font.colorLayers(
                ColrV1VarScaleDeltaSampleFont.GLYPH_BASE,
                0,
                new float[] {ColrV1VarScaleDeltaSampleFont.MAX_WEIGHT}
        );
        assertEquals(1, peak.size());
        assertEquals(
                ColrV1VarScaleDeltaSampleFont.BASE_SCALE_X + ColrV1VarScaleDeltaSampleFont.SCALE_DELTA,
                peak.getFirst().scaleX()
        );
    }

    /// Applies a COLR ItemVariationStore delta to `PaintVarRotate` angle at peak `wght`.
    @Test
    void appliesPaintVarRotateDeltaThroughShippedColorLayers() {
        SfntFont font = ColrV1VarRotateDeltaSampleFont.create();
        List<ColorLayer> defaults = font.colorLayers(ColrV1VarRotateDeltaSampleFont.GLYPH_BASE);
        assertEquals(1, defaults.size());
        assertEquals(ColrV1VarRotateDeltaSampleFont.BASE_ROTATE, defaults.getFirst().rotate());
        List<ColorLayer> peak = font.colorLayers(
                ColrV1VarRotateDeltaSampleFont.GLYPH_BASE,
                0,
                new float[] {ColrV1VarRotateDeltaSampleFont.MAX_WEIGHT}
        );
        assertEquals(1, peak.size());
        assertEquals(
                ColrV1VarRotateDeltaSampleFont.BASE_ROTATE + ColrV1VarRotateDeltaSampleFont.ROTATE_DELTA,
                peak.getFirst().rotate()
        );
    }

    /// Applies a COLR ItemVariationStore delta to `PaintVarTranslate` `dy` at peak `wght`.
    @Test
    void appliesPaintVarTranslateYDeltaThroughShippedColorLayers() {
        SfntFont font = ColrV1VarTranslateYDeltaSampleFont.create();
        List<ColorLayer> defaults = font.colorLayers(ColrV1VarTranslateYDeltaSampleFont.GLYPH_BASE);
        assertEquals(1, defaults.size());
        assertEquals(ColrV1VarTranslateYDeltaSampleFont.BASE_TRANSLATE_Y, defaults.getFirst().translateY());
        List<ColorLayer> peak = font.colorLayers(
                ColrV1VarTranslateYDeltaSampleFont.GLYPH_BASE,
                0,
                new float[] {ColrV1VarTranslateYDeltaSampleFont.MAX_WEIGHT}
        );
        assertEquals(1, peak.size());
        assertEquals(
                ColrV1VarTranslateYDeltaSampleFont.BASE_TRANSLATE_Y
                        + ColrV1VarTranslateYDeltaSampleFont.TRANSLATE_Y_DELTA,
                peak.getFirst().translateY()
        );
    }

    /// Applies a COLR ItemVariationStore delta to `PaintVarSkew` `xSkewAngle` at peak `wght`.
    @Test
    void appliesPaintVarSkewDeltaThroughShippedColorLayers() {
        SfntFont font = ColrV1VarSkewDeltaSampleFont.create();
        List<ColorLayer> defaults = font.colorLayers(ColrV1VarSkewDeltaSampleFont.GLYPH_BASE);
        assertEquals(1, defaults.size());
        assertEquals(ColrV1VarSkewDeltaSampleFont.BASE_SKEW_X, defaults.getFirst().skewX());
        List<ColorLayer> peak = font.colorLayers(
                ColrV1VarSkewDeltaSampleFont.GLYPH_BASE,
                0,
                new float[] {ColrV1VarSkewDeltaSampleFont.MAX_WEIGHT}
        );
        assertEquals(1, peak.size());
        assertEquals(
                ColrV1VarSkewDeltaSampleFont.BASE_SKEW_X + ColrV1VarSkewDeltaSampleFont.SKEW_DELTA,
                peak.getFirst().skewX()
        );
    }

    /// Applies a COLR ItemVariationStore delta to `PaintVarScale` `scaleY` at peak `wght`.
    @Test
    void appliesPaintVarScaleYDeltaThroughShippedColorLayers() {
        SfntFont font = ColrV1VarScaleYDeltaSampleFont.create();
        List<ColorLayer> defaults = font.colorLayers(ColrV1VarScaleYDeltaSampleFont.GLYPH_BASE);
        assertEquals(1, defaults.size());
        assertEquals(ColrV1VarScaleYDeltaSampleFont.BASE_SCALE_Y, defaults.getFirst().scaleY());
        List<ColorLayer> peak = font.colorLayers(
                ColrV1VarScaleYDeltaSampleFont.GLYPH_BASE,
                0,
                new float[] {ColrV1VarScaleYDeltaSampleFont.MAX_WEIGHT}
        );
        assertEquals(1, peak.size());
        assertEquals(
                ColrV1VarScaleYDeltaSampleFont.BASE_SCALE_Y + ColrV1VarScaleYDeltaSampleFont.SCALE_Y_DELTA,
                peak.getFirst().scaleY()
        );
    }

    /// Applies a COLR ItemVariationStore delta to `PaintVarSkew` `ySkewAngle` at peak `wght`.
    @Test
    void appliesPaintVarSkewYDeltaThroughShippedColorLayers() {
        SfntFont font = ColrV1VarSkewYDeltaSampleFont.create();
        List<ColorLayer> defaults = font.colorLayers(ColrV1VarSkewYDeltaSampleFont.GLYPH_BASE);
        assertEquals(1, defaults.size());
        assertEquals(ColrV1VarSkewYDeltaSampleFont.BASE_SKEW_Y, defaults.getFirst().skewY());
        List<ColorLayer> peak = font.colorLayers(
                ColrV1VarSkewYDeltaSampleFont.GLYPH_BASE,
                0,
                new float[] {ColrV1VarSkewYDeltaSampleFont.MAX_WEIGHT}
        );
        assertEquals(1, peak.size());
        assertEquals(
                ColrV1VarSkewYDeltaSampleFont.BASE_SKEW_Y + ColrV1VarSkewYDeltaSampleFont.SKEW_Y_DELTA,
                peak.getFirst().skewY()
        );
    }

    /// Applies a COLR ItemVariationStore delta to `PaintVarTransform` `xx` at peak `wght`.
    @Test
    void appliesPaintVarTransformDeltaThroughShippedColorLayers() {
        SfntFont font = ColrV1VarTransformDeltaSampleFont.create();
        List<ColorLayer> defaults = font.colorLayers(ColrV1VarTransformDeltaSampleFont.GLYPH_BASE);
        assertEquals(1, defaults.size());
        assertEquals(ColrV1VarTransformDeltaSampleFont.BASE_TRANSFORM_XX, defaults.getFirst().transformXx());
        List<ColorLayer> peak = font.colorLayers(
                ColrV1VarTransformDeltaSampleFont.GLYPH_BASE,
                0,
                new float[] {ColrV1VarTransformDeltaSampleFont.MAX_WEIGHT}
        );
        assertEquals(1, peak.size());
        assertEquals(
                ColrV1VarTransformDeltaSampleFont.BASE_TRANSFORM_XX
                        + ColrV1VarTransformDeltaSampleFont.TRANSFORM_DELTA,
                peak.getFirst().transformXx()
        );
    }

    /// Applies a COLR ItemVariationStore delta to `PaintVarRotateAroundCenter` `centerX`.
    @Test
    void appliesPaintVarRotateAroundCenterDeltaThroughShippedColorLayers() {
        SfntFont font = ColrV1VarRotateCenterDeltaSampleFont.create();
        List<ColorLayer> defaults = font.colorLayers(ColrV1VarRotateCenterDeltaSampleFont.GLYPH_BASE);
        assertEquals(1, defaults.size());
        assertEquals(ColrV1VarRotateCenterDeltaSampleFont.BASE_CENTER_X, defaults.getFirst().centerX());
        List<ColorLayer> peak = font.colorLayers(
                ColrV1VarRotateCenterDeltaSampleFont.GLYPH_BASE,
                0,
                new float[] {ColrV1VarRotateCenterDeltaSampleFont.MAX_WEIGHT}
        );
        assertEquals(1, peak.size());
        assertEquals(
                ColrV1VarRotateCenterDeltaSampleFont.BASE_CENTER_X
                        + ColrV1VarRotateCenterDeltaSampleFont.CENTER_DELTA,
                peak.getFirst().centerX()
        );
    }

    /// Applies a COLR ItemVariationStore delta to `PaintVarTransform` `yx` at peak `wght`.
    @Test
    void appliesPaintVarTransformYxDeltaThroughShippedColorLayers() {
        SfntFont font = ColrV1VarTransformYxDeltaSampleFont.create();
        List<ColorLayer> defaults = font.colorLayers(ColrV1VarTransformYxDeltaSampleFont.GLYPH_BASE);
        assertEquals(1, defaults.size());
        assertEquals(ColrV1VarTransformYxDeltaSampleFont.BASE_TRANSFORM_YX, defaults.getFirst().transformYx());
        List<ColorLayer> peak = font.colorLayers(
                ColrV1VarTransformYxDeltaSampleFont.GLYPH_BASE,
                0,
                new float[] {ColrV1VarTransformYxDeltaSampleFont.MAX_WEIGHT}
        );
        assertEquals(1, peak.size());
        assertEquals(
                ColrV1VarTransformYxDeltaSampleFont.BASE_TRANSFORM_YX
                        + ColrV1VarTransformYxDeltaSampleFont.TRANSFORM_DELTA,
                peak.getFirst().transformYx()
        );
    }

    /// Applies a COLR ItemVariationStore delta to `PaintVarRotateAroundCenter` `centerY`.
    @Test
    void appliesPaintVarRotateAroundCenterYDeltaThroughShippedColorLayers() {
        SfntFont font = ColrV1VarRotateCenterYDeltaSampleFont.create();
        List<ColorLayer> defaults = font.colorLayers(ColrV1VarRotateCenterYDeltaSampleFont.GLYPH_BASE);
        assertEquals(1, defaults.size());
        assertEquals(ColrV1VarRotateCenterYDeltaSampleFont.BASE_CENTER_Y, defaults.getFirst().centerY());
        List<ColorLayer> peak = font.colorLayers(
                ColrV1VarRotateCenterYDeltaSampleFont.GLYPH_BASE,
                0,
                new float[] {ColrV1VarRotateCenterYDeltaSampleFont.MAX_WEIGHT}
        );
        assertEquals(1, peak.size());
        assertEquals(
                ColrV1VarRotateCenterYDeltaSampleFont.BASE_CENTER_Y
                        + ColrV1VarRotateCenterYDeltaSampleFont.CENTER_DELTA,
                peak.getFirst().centerY()
        );
    }

    /// Applies a COLR ItemVariationStore delta to `PaintVarScaleUniform` at peak `wght`.
    @Test
    void appliesPaintVarScaleUniformDeltaThroughShippedColorLayers() {
        SfntFont font = ColrV1VarScaleUniformDeltaSampleFont.create();
        List<ColorLayer> defaults = font.colorLayers(ColrV1VarScaleUniformDeltaSampleFont.GLYPH_BASE);
        assertEquals(1, defaults.size());
        assertEquals(ColrV1VarScaleUniformDeltaSampleFont.BASE_SCALE, defaults.getFirst().scaleX());
        assertEquals(ColrV1VarScaleUniformDeltaSampleFont.BASE_SCALE, defaults.getFirst().scaleY());
        List<ColorLayer> peak = font.colorLayers(
                ColrV1VarScaleUniformDeltaSampleFont.GLYPH_BASE,
                0,
                new float[] {ColrV1VarScaleUniformDeltaSampleFont.MAX_WEIGHT}
        );
        assertEquals(1, peak.size());
        assertEquals(
                ColrV1VarScaleUniformDeltaSampleFont.BASE_SCALE + ColrV1VarScaleUniformDeltaSampleFont.SCALE_DELTA,
                peak.getFirst().scaleX()
        );
        assertEquals(
                ColrV1VarScaleUniformDeltaSampleFont.BASE_SCALE + ColrV1VarScaleUniformDeltaSampleFont.SCALE_DELTA,
                peak.getFirst().scaleY()
        );
    }

    /// Applies a COLR ItemVariationStore delta to `PaintVarTransform` `xy` at peak `wght`.
    @Test
    void appliesPaintVarTransformXyDeltaThroughShippedColorLayers() {
        SfntFont font = ColrV1VarTransformXyDeltaSampleFont.create();
        List<ColorLayer> defaults = font.colorLayers(ColrV1VarTransformXyDeltaSampleFont.GLYPH_BASE);
        assertEquals(1, defaults.size());
        assertEquals(ColrV1VarTransformXyDeltaSampleFont.BASE_TRANSFORM_XY, defaults.getFirst().transformXy());
        List<ColorLayer> peak = font.colorLayers(
                ColrV1VarTransformXyDeltaSampleFont.GLYPH_BASE,
                0,
                new float[] {ColrV1VarTransformXyDeltaSampleFont.MAX_WEIGHT}
        );
        assertEquals(1, peak.size());
        assertEquals(
                ColrV1VarTransformXyDeltaSampleFont.BASE_TRANSFORM_XY
                        + ColrV1VarTransformXyDeltaSampleFont.TRANSFORM_DELTA,
                peak.getFirst().transformXy()
        );
    }

    /// Applies a COLR ItemVariationStore delta to `PaintVarTransform` `yy` at peak `wght`.
    @Test
    void appliesPaintVarTransformYyDeltaThroughShippedColorLayers() {
        SfntFont font = ColrV1VarTransformYyDeltaSampleFont.create();
        List<ColorLayer> defaults = font.colorLayers(ColrV1VarTransformYyDeltaSampleFont.GLYPH_BASE);
        assertEquals(1, defaults.size());
        assertEquals(ColrV1VarTransformYyDeltaSampleFont.BASE_TRANSFORM_YY, defaults.getFirst().transformYy());
        List<ColorLayer> peak = font.colorLayers(
                ColrV1VarTransformYyDeltaSampleFont.GLYPH_BASE,
                0,
                new float[] {ColrV1VarTransformYyDeltaSampleFont.MAX_WEIGHT}
        );
        assertEquals(1, peak.size());
        assertEquals(
                ColrV1VarTransformYyDeltaSampleFont.BASE_TRANSFORM_YY
                        + ColrV1VarTransformYyDeltaSampleFont.TRANSFORM_DELTA,
                peak.getFirst().transformYy()
        );
    }

    /// Applies a COLR ItemVariationStore delta to `PaintVarTransform` `dx` at peak `wght`.
    @Test
    void appliesPaintVarTransformDxDeltaThroughShippedColorLayers() {
        SfntFont font = ColrV1VarTransformDxDeltaSampleFont.create();
        List<ColorLayer> defaults = font.colorLayers(ColrV1VarTransformDxDeltaSampleFont.GLYPH_BASE);
        assertEquals(1, defaults.size());
        assertEquals(ColrV1VarTransformDxDeltaSampleFont.BASE_TRANSFORM_DX, defaults.getFirst().transformDx());
        List<ColorLayer> peak = font.colorLayers(
                ColrV1VarTransformDxDeltaSampleFont.GLYPH_BASE,
                0,
                new float[] {ColrV1VarTransformDxDeltaSampleFont.MAX_WEIGHT}
        );
        assertEquals(1, peak.size());
        assertEquals(
                ColrV1VarTransformDxDeltaSampleFont.BASE_TRANSFORM_DX
                        + ColrV1VarTransformDxDeltaSampleFont.TRANSFORM_DELTA,
                peak.getFirst().transformDx()
        );
    }

    /// Applies a COLR ItemVariationStore delta to `PaintVarTransform` `dy` at peak `wght`.
    @Test
    void appliesPaintVarTransformDyDeltaThroughShippedColorLayers() {
        SfntFont font = ColrV1VarTransformDyDeltaSampleFont.create();
        List<ColorLayer> defaults = font.colorLayers(ColrV1VarTransformDyDeltaSampleFont.GLYPH_BASE);
        assertEquals(1, defaults.size());
        assertEquals(ColrV1VarTransformDyDeltaSampleFont.BASE_TRANSFORM_DY, defaults.getFirst().transformDy());
        List<ColorLayer> peak = font.colorLayers(
                ColrV1VarTransformDyDeltaSampleFont.GLYPH_BASE,
                0,
                new float[] {ColrV1VarTransformDyDeltaSampleFont.MAX_WEIGHT}
        );
        assertEquals(1, peak.size());
        assertEquals(
                ColrV1VarTransformDyDeltaSampleFont.BASE_TRANSFORM_DY
                        + ColrV1VarTransformDyDeltaSampleFont.TRANSFORM_DELTA,
                peak.getFirst().transformDy()
        );
    }

    /// Applies a COLR ItemVariationStore delta to `PaintVarScaleAroundCenter` `scaleX`.
    @Test
    void appliesPaintVarScaleAroundCenterDeltaThroughShippedColorLayers() {
        SfntFont font = ColrV1VarScaleAroundCenterDeltaSampleFont.create();
        List<ColorLayer> defaults = font.colorLayers(ColrV1VarScaleAroundCenterDeltaSampleFont.GLYPH_BASE);
        assertEquals(1, defaults.size());
        assertEquals(ColrV1VarScaleAroundCenterDeltaSampleFont.BASE_SCALE, defaults.getFirst().scaleX());
        List<ColorLayer> peak = font.colorLayers(
                ColrV1VarScaleAroundCenterDeltaSampleFont.GLYPH_BASE,
                0,
                new float[] {ColrV1VarScaleAroundCenterDeltaSampleFont.MAX_WEIGHT}
        );
        assertEquals(1, peak.size());
        assertEquals(
                ColrV1VarScaleAroundCenterDeltaSampleFont.BASE_SCALE
                        + ColrV1VarScaleAroundCenterDeltaSampleFont.SCALE_DELTA,
                peak.getFirst().scaleX()
        );
    }

    /// Applies a COLR ItemVariationStore delta to `PaintVarScaleUniformAroundCenter`.
    @Test
    void appliesPaintVarScaleUniformAroundCenterDeltaThroughShippedColorLayers() {
        SfntFont font = ColrV1VarScaleUniformAroundCenterDeltaSampleFont.create();
        List<ColorLayer> defaults = font.colorLayers(ColrV1VarScaleUniformAroundCenterDeltaSampleFont.GLYPH_BASE);
        assertEquals(1, defaults.size());
        assertEquals(ColrV1VarScaleUniformAroundCenterDeltaSampleFont.BASE_SCALE, defaults.getFirst().scaleX());
        assertEquals(ColrV1VarScaleUniformAroundCenterDeltaSampleFont.BASE_SCALE, defaults.getFirst().scaleY());
        List<ColorLayer> peak = font.colorLayers(
                ColrV1VarScaleUniformAroundCenterDeltaSampleFont.GLYPH_BASE,
                0,
                new float[] {ColrV1VarScaleUniformAroundCenterDeltaSampleFont.MAX_WEIGHT}
        );
        assertEquals(1, peak.size());
        assertEquals(
                ColrV1VarScaleUniformAroundCenterDeltaSampleFont.BASE_SCALE
                        + ColrV1VarScaleUniformAroundCenterDeltaSampleFont.SCALE_DELTA,
                peak.getFirst().scaleX()
        );
        assertEquals(
                ColrV1VarScaleUniformAroundCenterDeltaSampleFont.BASE_SCALE
                        + ColrV1VarScaleUniformAroundCenterDeltaSampleFont.SCALE_DELTA,
                peak.getFirst().scaleY()
        );
    }

    /// Applies a COLR ItemVariationStore delta to `PaintVarSkewAroundCenter` `xSkewAngle`.
    @Test
    void appliesPaintVarSkewAroundCenterDeltaThroughShippedColorLayers() {
        SfntFont font = ColrV1VarSkewAroundCenterDeltaSampleFont.create();
        List<ColorLayer> defaults = font.colorLayers(ColrV1VarSkewAroundCenterDeltaSampleFont.GLYPH_BASE);
        assertEquals(1, defaults.size());
        assertEquals(ColrV1VarSkewAroundCenterDeltaSampleFont.BASE_SKEW_X, defaults.getFirst().skewX());
        List<ColorLayer> peak = font.colorLayers(
                ColrV1VarSkewAroundCenterDeltaSampleFont.GLYPH_BASE,
                0,
                new float[] {ColrV1VarSkewAroundCenterDeltaSampleFont.MAX_WEIGHT}
        );
        assertEquals(1, peak.size());
        assertEquals(
                ColrV1VarSkewAroundCenterDeltaSampleFont.BASE_SKEW_X
                        + ColrV1VarSkewAroundCenterDeltaSampleFont.SKEW_DELTA,
                peak.getFirst().skewX()
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

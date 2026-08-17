package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/// Writes SFNT mapping and raster conformance evidence.
@NotNullByDefault
public final class FontConformance {
    /// Prevents instantiation.
    private FontConformance() {
    }

    /// Maps a Latin glyph and writes the report.
    ///
    /// @param arguments one output directory
    /// @throws IOException if the report cannot be written
    public static void main(String[] arguments) throws IOException {
        if (arguments.length != 1) {
            throw new IllegalArgumentException("Expected one output directory");
        }
        SfntFont font = BitmapSfntFont.create();
        int glyph = font.glyphId('A');
        if (glyph <= 0) {
            throw new IllegalStateException("Latin glyph was not mapped");
        }
        GlyphMask mask = GlyphRasterizer.rasterize(font, glyph, 16);
        if (mask.width() <= 0 || mask.height() <= 0) {
            throw new IllegalStateException("Glyph raster was empty");
        }
        Path output = Path.of(arguments[0]);
        Files.createDirectories(output);
        LeftoverEvidence leftovers = leftoverEvidence();
        Files.writeString(output.resolve("leftovers.json"), leftovers.toJson(), StandardCharsets.UTF_8);
        Files.writeString(
                output.resolve("results.json"),
                """
                        {
                          "profile": "m4-font",
                          "workPackage": "FONT-001",
                          "status": "passed",
                          "glyphId": %d,
                          "maskWidth": %d,
                          "maskHeight": %d,
                          "colrLayers": %d,
                          "variationAxes": %d,
                          "gvarPeakMoved": %s,
                          "avarRemapped": %s,
                          "cbdtPresent": %s,
                          "ebdtPresent": %s,
                          "sbixPresent": %s
                        }
                        """.formatted(
                        glyph,
                        mask.width(),
                        mask.height(),
                        leftovers.colrLayers,
                        leftovers.variationAxes,
                        leftovers.gvarPeakMoved,
                        leftovers.avarRemapped,
                        leftovers.cbdtPresent,
                        leftovers.ebdtPresent,
                        leftovers.sbixPresent
                ),
                StandardCharsets.UTF_8
        );
    }

    /// Calls the named first-stable leftover entries on constructed faces.
    private static LeftoverEvidence leftoverEvidence() {
        SfntFont colr = ColrSampleFont.create();
        int layers = colr.colorLayers(ColrSampleFont.GLYPH_BASE).size();
        if (layers < 1) {
            throw new IllegalStateException("COLR leftover did not return layers");
        }
        SfntFont variable = GvarSampleFont.create();
        int axes = variable.variationAxes().size();
        CollectingPen def = new CollectingPen();
        variable.outline(GvarSampleFont.GLYPH_A, def);
        CollectingPen peak = new CollectingPen();
        variable.outline(GvarSampleFont.GLYPH_A, peak, new float[] {GvarSampleFont.MAX_WEIGHT});
        boolean moved = !peak.commands().isEmpty()
                && Math.abs(peak.commands().getFirst().x0() - def.commands().getFirst().x0()
                - GvarSampleFont.CONTOUR_X_DELTA) < 0.01f;
        if (axes < 1 || !moved) {
            throw new IllegalStateException("Variable leftover did not move the outline");
        }
        SfntFont avarFont = AvarSampleFont.create();
        CollectingPen remapped = new CollectingPen();
        avarFont.outline(GvarSampleFont.GLYPH_A, remapped, new float[] {AvarSampleFont.MID_WEIGHT});
        boolean avar = !remapped.commands().isEmpty()
                && Math.abs(remapped.commands().getFirst().x0() - GvarSampleFont.CONTOUR_X_DELTA) < 0.01f;
        if (!avar) {
            throw new IllegalStateException("avar leftover did not remap the mid instance");
        }
        SfntFont cbdtFont = CbdtSampleFont.create();
        boolean cbdt = cbdtFont.colorBitmap(cbdtFont.glyphId('A')) != null;
        SfntFont ebdtFont = EbdtSampleFont.create();
        boolean ebdt = ebdtFont.grayscaleBitmap(ebdtFont.glyphId('A')) != null;
        SfntFont sbixFont = SbixSampleFont.create();
        boolean sbix = sbixFont.embeddedBitmap(sbixFont.glyphId('A')) != null;
        if (!cbdt || !ebdt || !sbix) {
            throw new IllegalStateException("Embedded-bitmap leftover did not return a strike");
        }
        SfntFont colrV1 = ColrV1SampleFont.create();
        int colrV1Layers = colrV1.colorLayers(ColrV1SampleFont.GLYPH_BASE).size();
        if (colrV1Layers != 2) {
            throw new IllegalStateException("COLR v1 leftover did not flatten two layers");
        }
        SfntFont mvar = MvarSampleFont.create();
        int peakAscender = mvar.ascender(new float[] {MvarSampleFont.MAX_WEIGHT});
        if (peakAscender != MvarSampleFont.DEFAULT_ASCENDER + MvarSampleFont.ASCENDER_DELTA) {
            throw new IllegalStateException("MVAR leftover did not apply hasc");
        }
        SfntFont woff = new SfntFont(WoffFile.wrapUncompressed(
                ColrV1SampleFont.bytes().toArray(java.lang.foreign.ValueLayout.JAVA_BYTE)));
        if (woff.colorLayers(ColrV1SampleFont.GLYPH_BASE).size() != 2) {
            throw new IllegalStateException("WOFF leftover did not unwrap COLR v1 layers");
        }
        SfntFont woff2 = new SfntFont(Woff2File.wrap(
                ColrV1SampleFont.bytes().toArray(java.lang.foreign.ValueLayout.JAVA_BYTE)));
        if (woff2.colorLayers(ColrV1SampleFont.GLYPH_BASE).size() != 2) {
            throw new IllegalStateException("WOFF2 leftover did not unwrap COLR v1 layers");
        }
        SfntFont cursive = GposCursiveSampleFont.create();
        int cursiveDelta = cursive.pairAdjustment(GposCursiveSampleFont.GLYPH_A, GposCursiveSampleFont.GLYPH_B);
        if (cursiveDelta != GposCursiveSampleFont.CURSIVE_DELTA) {
            throw new IllegalStateException("GPOS cursive leftover did not apply exit-to-entry");
        }
        SfntFont skip = GposMarkSkipSampleFont.create();
        int skipPair = skip.skipPairAdjustment(GposMarkSkipSampleFont.GLYPH_A, GposMarkSkipSampleFont.GLYPH_C);
        int skipChain = skip.skipChainAdjustment(
                GposMarkSkipSampleFont.GLYPH_A,
                GposMarkSkipSampleFont.GLYPH_C,
                GposMarkSkipSampleFont.GLYPH_D
        );
        if (skipPair != GposMarkSkipSampleFont.PAIR_DELTA || skipChain != GposMarkSkipSampleFont.CHAIN_DELTA) {
            throw new IllegalStateException("GPOS IgnoreMarks leftover did not skip the mark");
        }
        SfntFont attach = GposMarkAttachSampleFont.create();
        int attachPair = attach.attachPairAdjustment(
                GposMarkAttachSampleFont.GLYPH_A,
                GposMarkAttachSampleFont.GLYPH_C,
                GposMarkAttachSampleFont.ATTACH_TYPE
        );
        if (attachPair != GposMarkAttachSampleFont.ATTACH_DELTA
                || attach.markAttachClass(GposMarkAttachSampleFont.GLYPH_B) != 2) {
            throw new IllegalStateException("GPOS MarkAttachmentType leftover did not consult GDEF");
        }
        SfntFont gsubAttach = GsubMarkAttachSampleFont.create();
        int gsubContext = gsubAttach.contextSubstitute(
                new int[] {
                    GsubMarkAttachSampleFont.GLYPH_A,
                    GsubMarkAttachSampleFont.GLYPH_B,
                    GsubMarkAttachSampleFont.GLYPH_C
                },
                0,
                3,
                SfntFont.TAG_CALT
        );
        if (gsubContext != GsubMarkAttachSampleFont.GLYPH_X) {
            throw new IllegalStateException("GSUB MarkAttachmentType leftover did not skip class 2");
        }
        SfntFont ignoreClass = GposIgnoreClassSampleFont.create();
        int baseDelta = ignoreClass.pairAdjustment(
                new int[] {
                    GposIgnoreClassSampleFont.GLYPH_A,
                    GposIgnoreClassSampleFont.GLYPH_B,
                    GposIgnoreClassSampleFont.GLYPH_C
                },
                0,
                3
        );
        if (baseDelta != GposIgnoreClassSampleFont.BASE_DELTA) {
            throw new IllegalStateException("GPOS IgnoreBaseGlyphs leftover did not skip the base");
        }
        SfntFont markSet = GdefMarkSetSampleFont.create();
        if (!markSet.inMarkSet(GdefMarkSetSampleFont.GLYPH_B, 0)
                || markSet.inMarkSet(GdefMarkSetSampleFont.GLYPH_D, 0)) {
            throw new IllegalStateException("GDEF MarkGlyphSets leftover did not isolate set 0");
        }
        SfntFont reverseSkip = GsubReverseSkipSampleFont.create();
        int reversed = reverseSkip.reverseSubstitute(
                new int[] {
                    GsubReverseSkipSampleFont.GLYPH_A,
                    GsubReverseSkipSampleFont.GLYPH_B,
                    GsubReverseSkipSampleFont.GLYPH_C
                },
                0,
                3,
                SfntFont.TAG_CALT
        );
        if (reversed != GsubReverseSkipSampleFont.GLYPH_X) {
            throw new IllegalStateException("GSUB reverse IgnoreMarks leftover did not skip the mark");
        }
        SfntFont pairClass = GposPairClassSampleFont.create();
        if (pairClass.pairAdjustment(GposPairClassSampleFont.GLYPH_A, GposPairClassSampleFont.GLYPH_C)
                != GposPairClassSampleFont.PAIR_DELTA) {
            throw new IllegalStateException("GPOS PairPos format 2 leftover did not expand the class cell");
        }
        SfntFont coverage = GsubCoverageSampleFont.create();
        if (coverage.contextSubstitute(
                GsubCoverageSampleFont.GLYPH_A,
                GsubCoverageSampleFont.GLYPH_B,
                SfntFont.TAG_CALT
        ) != GsubCoverageSampleFont.GLYPH_X) {
            throw new IllegalStateException("GSUB ContextSubst format 3 leftover did not match coverage");
        }
        SfntFont backtrack = GsubReverseBacktrackSampleFont.create();
        SfntFont gposClass = GposContextClassSampleFont.create();
        if (gposClass.pairAdjustment(GposContextClassSampleFont.GLYPH_A, GposContextClassSampleFont.GLYPH_B)
                != GposContextClassSampleFont.CONTEXT_DELTA) {
            throw new IllegalStateException("GPOS ContextPos format 2 leftover did not expand the class rule");
        }
        SfntFont gsubChainClass = GsubChainClassSampleFont.create();
        if (gsubChainClass.chainSubstitute(
                GsubChainClassSampleFont.GLYPH_A,
                GsubChainClassSampleFont.GLYPH_B,
                GsubChainClassSampleFont.GLYPH_C,
                SfntFont.TAG_CALT
        ) != GsubChainClassSampleFont.GLYPH_Y) {
            throw new IllegalStateException("GSUB ChainContext format 2 leftover did not match classes");
        }
        if (backtrack.reverseSubstitute(
                new int[] {
                    GsubReverseBacktrackSampleFont.GLYPH_B,
                    GsubReverseBacktrackSampleFont.GLYPH_A,
                    GsubReverseBacktrackSampleFont.GLYPH_C
                },
                1,
                2,
                SfntFont.TAG_CALT
        ) != GsubReverseBacktrackSampleFont.GLYPH_X) {
            throw new IllegalStateException("GSUB reverse backtrack leftover did not require B");
        }
        SfntFont gposBack = GposChainBacktrackSampleFont.create();
        if (gposBack.chainAdjustment(
                new int[] {
                    GposChainBacktrackSampleFont.GLYPH_D,
                    GposChainBacktrackSampleFont.GLYPH_A,
                    GposChainBacktrackSampleFont.GLYPH_B,
                    GposChainBacktrackSampleFont.GLYPH_C
                },
                1,
                3
        ) != GposChainBacktrackSampleFont.CHAIN_DELTA
                || gposBack.chainAdjustment(
                        GposChainBacktrackSampleFont.GLYPH_A,
                        GposChainBacktrackSampleFont.GLYPH_B,
                        GposChainBacktrackSampleFont.GLYPH_C
                ) != 0) {
            throw new IllegalStateException("GPOS chain format 2 backtrack leftover did not require D");
        }
        SfntFont gsubBack = GsubChainBacktrackSampleFont.create();
        if (gsubBack.chainSubstitute(
                new int[] {
                    GsubChainBacktrackSampleFont.GLYPH_D,
                    GsubChainBacktrackSampleFont.GLYPH_A,
                    GsubChainBacktrackSampleFont.GLYPH_B,
                    GsubChainBacktrackSampleFont.GLYPH_C
                },
                1,
                3,
                SfntFont.TAG_CALT
        ) != GsubChainBacktrackSampleFont.GLYPH_Y) {
            throw new IllegalStateException("GSUB chain format 2 backtrack leftover did not require D");
        }
        SfntFont gasp = GaspSampleFont.create();
        if (gasp.gaspAllowsGrayscale(GaspSampleFont.BINARY_PPEM)
                || !gasp.gaspAllowsGrayscale(GaspSampleFont.GRAY_PPEM)) {
            throw new IllegalStateException("gasp leftover did not withhold grayscale at 8 ppem");
        }
        SfntFont twoBack = GposChainDoubleBacktrackSampleFont.create();
        if (twoBack.chainAdjustment(
                new int[] {
                    GposChainDoubleBacktrackSampleFont.GLYPH_E,
                    GposChainDoubleBacktrackSampleFont.GLYPH_D,
                    GposChainDoubleBacktrackSampleFont.GLYPH_A,
                    GposChainDoubleBacktrackSampleFont.GLYPH_B,
                    GposChainDoubleBacktrackSampleFont.GLYPH_C
                },
                2,
                3
        ) != GposChainDoubleBacktrackSampleFont.CHAIN_DELTA) {
            throw new IllegalStateException("GPOS two-glyph chain backtrack leftover did not require ED");
        }
        SfntFont flaggedBack = GposFlaggedChainBacktrackSampleFont.create();
        if (flaggedBack.chainAdjustment(
                new int[] {
                    GposFlaggedChainBacktrackSampleFont.GLYPH_D,
                    GposFlaggedChainBacktrackSampleFont.GLYPH_A,
                    GposFlaggedChainBacktrackSampleFont.GLYPH_B,
                    GposFlaggedChainBacktrackSampleFont.GLYPH_C,
                    GposFlaggedChainBacktrackSampleFont.GLYPH_E
                },
                1,
                4
        ) != GposFlaggedChainBacktrackSampleFont.CHAIN_DELTA
                || flaggedBack.chainAdjustment(
                        new int[] {
                            GposFlaggedChainBacktrackSampleFont.GLYPH_A,
                            GposFlaggedChainBacktrackSampleFont.GLYPH_B,
                            GposFlaggedChainBacktrackSampleFont.GLYPH_C,
                            GposFlaggedChainBacktrackSampleFont.GLYPH_E
                        },
                        0,
                        4
                ) != 0) {
            throw new IllegalStateException("GPOS flagged chain backtrack leftover dropped preceding D");
        }
        SfntFont attachBack = GposAttachChainBacktrackSampleFont.create();
        if (attachBack.attachChainAdjustment(
                GposAttachChainBacktrackSampleFont.GLYPH_A,
                GposAttachChainBacktrackSampleFont.GLYPH_C,
                GposAttachChainBacktrackSampleFont.GLYPH_E,
                GposAttachChainBacktrackSampleFont.ATTACH_TYPE,
                GposAttachChainBacktrackSampleFont.GLYPH_D,
                0
        ) != GposAttachChainBacktrackSampleFont.CHAIN_DELTA
                || attachBack.attachChainAdjustment(
                        GposAttachChainBacktrackSampleFont.GLYPH_A,
                        GposAttachChainBacktrackSampleFont.GLYPH_C,
                        GposAttachChainBacktrackSampleFont.GLYPH_E,
                        GposAttachChainBacktrackSampleFont.ATTACH_TYPE
                ) != 0) {
            throw new IllegalStateException("GPOS attach chain backtrack leftover dropped preceding D");
        }
        SfntFont triple = GposChainTripleBacktrackSampleFont.create();
        if (triple.chainAdjustment(
                new int[] {
                    GposChainTripleBacktrackSampleFont.GLYPH_F,
                    GposChainTripleBacktrackSampleFont.GLYPH_E,
                    GposChainTripleBacktrackSampleFont.GLYPH_D,
                    GposChainTripleBacktrackSampleFont.GLYPH_A,
                    GposChainTripleBacktrackSampleFont.GLYPH_B,
                    GposChainTripleBacktrackSampleFont.GLYPH_C
                },
                3,
                3
        ) != GposChainTripleBacktrackSampleFont.CHAIN_DELTA) {
            throw new IllegalStateException("GPOS three-glyph chain backtrack leftover did not require FED");
        }
        SfntFont translate = ColrV1TranslateSampleFont.create();
        if (translate.colorLayers(ColrV1TranslateSampleFont.GLYPH_BASE).size() != 1) {
            throw new IllegalStateException("COLR v1 PaintTranslate leftover did not flatten");
        }
        SfntFont gradient = ColrV1GradientSampleFont.create();
        if (gradient.colorLayers(ColrV1GradientSampleFont.GLYPH_BASE).size() != 1) {
            throw new IllegalStateException("COLR v1 PaintLinearGradient leftover did not flatten");
        }
        SfntFont colrGlyph = ColrV1ColrGlyphSampleFont.create();
        if (colrGlyph.colorLayers(ColrV1ColrGlyphSampleFont.GLYPH_BASE).size() != 1) {
            throw new IllegalStateException("COLR v1 PaintColrGlyph leftover did not flatten");
        }
        SfntFont scale = ColrV1ScaleSampleFont.create();
        if (scale.colorLayers(ColrV1ScaleSampleFont.GLYPH_BASE).size() != 1) {
            throw new IllegalStateException("COLR v1 PaintScale leftover did not flatten");
        }
        if (ColrV1RotateSampleFont.create().colorLayers(ColrV1RotateSampleFont.GLYPH_BASE).size() != 1) {
            throw new IllegalStateException("COLR v1 PaintRotate leftover did not flatten");
        }
        if (ColrV1TransformSampleFont.create().colorLayers(ColrV1TransformSampleFont.GLYPH_BASE).size() != 1) {
            throw new IllegalStateException("COLR v1 PaintTransform leftover did not flatten");
        }
        if (ColrV1RadialSampleFont.create().colorLayers(ColrV1RadialSampleFont.GLYPH_BASE).size() != 1) {
            throw new IllegalStateException("COLR v1 PaintRadialGradient leftover did not flatten");
        }
        if (ColrV1SweepSampleFont.create().colorLayers(ColrV1SweepSampleFont.GLYPH_BASE).size() != 1) {
            throw new IllegalStateException("COLR v1 PaintSweepGradient leftover did not flatten");
        }
        if (ColrV1CompositeSampleFont.create().colorLayers(ColrV1CompositeSampleFont.GLYPH_BASE).size() != 2) {
            throw new IllegalStateException("COLR v1 PaintComposite leftover did not flatten two layers");
        }
        SfntFont varSolid = ColrV1VarSolidSampleFont.create();
        if (ColrV1VarTranslateSampleFont.create().colorLayers(ColrV1VarTranslateSampleFont.GLYPH_BASE).size() != 1) {
            throw new IllegalStateException("COLR v1 PaintVarTranslate leftover did not flatten");
        }
        if (ColrV1VarScaleSampleFont.create().colorLayers(ColrV1VarScaleSampleFont.GLYPH_BASE).size() != 1) {
            throw new IllegalStateException("COLR v1 PaintVarScale leftover did not flatten");
        }
        if (ColrV1VarRotateSampleFont.create().colorLayers(ColrV1VarRotateSampleFont.GLYPH_BASE).size() != 1) {
            throw new IllegalStateException("COLR v1 PaintVarRotate leftover did not flatten");
        }
        if (ColrV1VarTransformSampleFont.create().colorLayers(ColrV1VarTransformSampleFont.GLYPH_BASE).size() != 1) {
            throw new IllegalStateException("COLR v1 PaintVarTransform leftover did not flatten");
        }
        if (ColrV1VarTranslateDeltaSampleFont.create()
                        .colorLayers(
                                ColrV1VarTranslateDeltaSampleFont.GLYPH_BASE,
                                0,
                                new float[] {ColrV1VarTranslateDeltaSampleFont.MAX_WEIGHT})
                        .getFirst()
                        .translateX()
                != ColrV1VarTranslateDeltaSampleFont.BASE_TRANSLATE_X
                        + ColrV1VarTranslateDeltaSampleFont.TRANSLATE_DELTA) {
            throw new IllegalStateException("COLR v1 PaintVarTranslate leftover did not apply the store delta");
        }
        if (ColrV1VarScaleDeltaSampleFont.create()
                        .colorLayers(
                                ColrV1VarScaleDeltaSampleFont.GLYPH_BASE,
                                0,
                                new float[] {ColrV1VarScaleDeltaSampleFont.MAX_WEIGHT})
                        .getFirst()
                        .scaleX()
                != ColrV1VarScaleDeltaSampleFont.BASE_SCALE_X + ColrV1VarScaleDeltaSampleFont.SCALE_DELTA) {
            throw new IllegalStateException("COLR v1 PaintVarScale leftover did not apply the store delta");
        }
        if (ColrV1VarRotateDeltaSampleFont.create()
                        .colorLayers(
                                ColrV1VarRotateDeltaSampleFont.GLYPH_BASE,
                                0,
                                new float[] {ColrV1VarRotateDeltaSampleFont.MAX_WEIGHT})
                        .getFirst()
                        .rotate()
                != ColrV1VarRotateDeltaSampleFont.BASE_ROTATE + ColrV1VarRotateDeltaSampleFont.ROTATE_DELTA) {
            throw new IllegalStateException("COLR v1 PaintVarRotate leftover did not apply the store delta");
        }
        if (ColrV1VarTranslateYDeltaSampleFont.create()
                        .colorLayers(
                                ColrV1VarTranslateYDeltaSampleFont.GLYPH_BASE,
                                0,
                                new float[] {ColrV1VarTranslateYDeltaSampleFont.MAX_WEIGHT})
                        .getFirst()
                        .translateY()
                != ColrV1VarTranslateYDeltaSampleFont.BASE_TRANSLATE_Y
                        + ColrV1VarTranslateYDeltaSampleFont.TRANSLATE_Y_DELTA) {
            throw new IllegalStateException("COLR v1 PaintVarTranslate leftover did not apply the dy store delta");
        }
        if (ColrV1VarSkewDeltaSampleFont.create()
                        .colorLayers(
                                ColrV1VarSkewDeltaSampleFont.GLYPH_BASE,
                                0,
                                new float[] {ColrV1VarSkewDeltaSampleFont.MAX_WEIGHT})
                        .getFirst()
                        .skewX()
                != ColrV1VarSkewDeltaSampleFont.BASE_SKEW_X + ColrV1VarSkewDeltaSampleFont.SKEW_DELTA) {
            throw new IllegalStateException("COLR v1 PaintVarSkew leftover did not apply the store delta");
        }
        if (ColrV1VarScaleYDeltaSampleFont.create()
                        .colorLayers(
                                ColrV1VarScaleYDeltaSampleFont.GLYPH_BASE,
                                0,
                                new float[] {ColrV1VarScaleYDeltaSampleFont.MAX_WEIGHT})
                        .getFirst()
                        .scaleY()
                != ColrV1VarScaleYDeltaSampleFont.BASE_SCALE_Y + ColrV1VarScaleYDeltaSampleFont.SCALE_Y_DELTA) {
            throw new IllegalStateException("COLR v1 PaintVarScale leftover did not apply the scaleY store delta");
        }
        if (ColrV1VarSkewYDeltaSampleFont.create()
                        .colorLayers(
                                ColrV1VarSkewYDeltaSampleFont.GLYPH_BASE,
                                0,
                                new float[] {ColrV1VarSkewYDeltaSampleFont.MAX_WEIGHT})
                        .getFirst()
                        .skewY()
                != ColrV1VarSkewYDeltaSampleFont.BASE_SKEW_Y + ColrV1VarSkewYDeltaSampleFont.SKEW_Y_DELTA) {
            throw new IllegalStateException("COLR v1 PaintVarSkew leftover did not apply the ySkew store delta");
        }
        if (ColrV1VarTransformDeltaSampleFont.create()
                        .colorLayers(
                                ColrV1VarTransformDeltaSampleFont.GLYPH_BASE,
                                0,
                                new float[] {ColrV1VarTransformDeltaSampleFont.MAX_WEIGHT})
                        .getFirst()
                        .transformXx()
                != ColrV1VarTransformDeltaSampleFont.BASE_TRANSFORM_XX
                        + ColrV1VarTransformDeltaSampleFont.TRANSFORM_DELTA) {
            throw new IllegalStateException("COLR v1 PaintVarTransform leftover did not apply the xx store delta");
        }
        if (ColrV1VarRotateCenterDeltaSampleFont.create()
                        .colorLayers(
                                ColrV1VarRotateCenterDeltaSampleFont.GLYPH_BASE,
                                0,
                                new float[] {ColrV1VarRotateCenterDeltaSampleFont.MAX_WEIGHT})
                        .getFirst()
                        .centerX()
                != ColrV1VarRotateCenterDeltaSampleFont.BASE_CENTER_X
                        + ColrV1VarRotateCenterDeltaSampleFont.CENTER_DELTA) {
            throw new IllegalStateException("COLR v1 around-center leftover did not apply the centerX store delta");
        }
        if (ColrV1VarLinearSampleFont.create().colorLayers(ColrV1VarLinearSampleFont.GLYPH_BASE).size() != 1) {
            throw new IllegalStateException("COLR v1 PaintVarLinearGradient leftover did not flatten");
        }
        if (ColrV1VarRadialSampleFont.create().colorLayers(ColrV1VarRadialSampleFont.GLYPH_BASE).size() != 1) {
            throw new IllegalStateException("COLR v1 PaintVarRadialGradient leftover did not flatten");
        }
        if (ColrV1VarSweepSampleFont.create().colorLayers(ColrV1VarSweepSampleFont.GLYPH_BASE).size() != 1) {
            throw new IllegalStateException("COLR v1 PaintVarSweepGradient leftover did not flatten");
        }
        SfntFont varLinearDelta = ColrV1VarLinearDeltaSampleFont.create();
        if (varLinearDelta.colorLayers(ColrV1VarLinearDeltaSampleFont.GLYPH_BASE).getFirst().paletteIndex() != 0
                || varLinearDelta.colorLayers(
                                ColrV1VarLinearDeltaSampleFont.GLYPH_BASE,
                                0,
                                new float[] {ColrV1VarLinearDeltaSampleFont.MAX_WEIGHT})
                        .getFirst()
                        .paletteIndex()
                        != 1) {
            throw new IllegalStateException("COLR v1 VarColorLine leftover did not apply the store delta");
        }
        if (varSolid.colorLayers(ColrV1VarSolidSampleFont.GLYPH_BASE).getFirst().paletteIndex() != 0
                || varSolid.colorLayers(
                                ColrV1VarSolidSampleFont.GLYPH_BASE,
                                0,
                                new float[] {ColrV1VarSolidSampleFont.MAX_WEIGHT})
                        .getFirst()
                        .paletteIndex()
                        != 1) {
            throw new IllegalStateException("COLR v1 PaintVarSolid leftover did not apply the store delta");
        }
        SfntFont symmetric = GaspSymmetricGridFitSampleFont.create();
        if (!symmetric.gaspSymmetricGridFits(GaspSymmetricGridFitSampleFont.GRID_PPEM)
                || GlyphRasterizer.rasterize(
                                symmetric,
                                GaspSymmetricGridFitSampleFont.GLYPH_A,
                                GaspSymmetricGridFitSampleFont.GRID_PPEM)
                        .width()
                        != 10) {
            throw new IllegalStateException("gasp SYMMETRIC_GRIDFIT leftover did not snap the outline x-box");
        }
        SfntFont quad = GposChainQuadBacktrackSampleFont.create();
        if (quad.chainAdjustment(
                new int[] {
                    GposChainQuadBacktrackSampleFont.GLYPH_G,
                    GposChainQuadBacktrackSampleFont.GLYPH_F,
                    GposChainQuadBacktrackSampleFont.GLYPH_E,
                    GposChainQuadBacktrackSampleFont.GLYPH_D,
                    GposChainQuadBacktrackSampleFont.GLYPH_A,
                    GposChainQuadBacktrackSampleFont.GLYPH_B,
                    GposChainQuadBacktrackSampleFont.GLYPH_C
                },
                4,
                3
        ) != GposChainQuadBacktrackSampleFont.CHAIN_DELTA) {
            throw new IllegalStateException("GPOS four-glyph chain backtrack leftover did not require GFED");
        }
        SfntFont gsubQuad = GsubChainQuadBacktrackSampleFont.create();
        if (gsubQuad.chainSubstitute(
                new int[] {
                    GsubChainQuadBacktrackSampleFont.GLYPH_G,
                    GsubChainQuadBacktrackSampleFont.GLYPH_F,
                    GsubChainQuadBacktrackSampleFont.GLYPH_E,
                    GsubChainQuadBacktrackSampleFont.GLYPH_D,
                    GsubChainQuadBacktrackSampleFont.GLYPH_A,
                    GsubChainQuadBacktrackSampleFont.GLYPH_B,
                    GsubChainQuadBacktrackSampleFont.GLYPH_C
                },
                4,
                3,
                SfntFont.TAG_CALT
        ) != GsubChainQuadBacktrackSampleFont.GLYPH_Y) {
            throw new IllegalStateException("GSUB four-glyph chain backtrack leftover did not require GFED");
        }
        SfntFont penta = GposChainPentaBacktrackSampleFont.create();
        if (penta.chainAdjustment(
                new int[] {
                    GposChainPentaBacktrackSampleFont.GLYPH_H,
                    GposChainPentaBacktrackSampleFont.GLYPH_G,
                    GposChainPentaBacktrackSampleFont.GLYPH_F,
                    GposChainPentaBacktrackSampleFont.GLYPH_E,
                    GposChainPentaBacktrackSampleFont.GLYPH_D,
                    GposChainPentaBacktrackSampleFont.GLYPH_A,
                    GposChainPentaBacktrackSampleFont.GLYPH_B,
                    GposChainPentaBacktrackSampleFont.GLYPH_C
                },
                5,
                3
        ) != GposChainPentaBacktrackSampleFont.CHAIN_DELTA) {
            throw new IllegalStateException("GPOS five-glyph chain backtrack leftover did not require HGFED");
        }
        SfntFont hexa = GposChainHexaBacktrackSampleFont.create();
        if (hexa.chainAdjustment(
                new int[] {
                    GposChainHexaBacktrackSampleFont.GLYPH_I,
                    GposChainHexaBacktrackSampleFont.GLYPH_H,
                    GposChainHexaBacktrackSampleFont.GLYPH_G,
                    GposChainHexaBacktrackSampleFont.GLYPH_F,
                    GposChainHexaBacktrackSampleFont.GLYPH_E,
                    GposChainHexaBacktrackSampleFont.GLYPH_D,
                    GposChainHexaBacktrackSampleFont.GLYPH_A,
                    GposChainHexaBacktrackSampleFont.GLYPH_B,
                    GposChainHexaBacktrackSampleFont.GLYPH_C
                },
                6,
                3
        ) != GposChainHexaBacktrackSampleFont.CHAIN_DELTA) {
            throw new IllegalStateException("GPOS six-glyph chain backtrack leftover did not require IHGFED");
        }
        SfntFont hepta = GposChainHeptaBacktrackSampleFont.create();
        if (hepta.chainAdjustment(
                new int[] {
                    GposChainHeptaBacktrackSampleFont.GLYPH_J,
                    GposChainHeptaBacktrackSampleFont.GLYPH_I,
                    GposChainHeptaBacktrackSampleFont.GLYPH_H,
                    GposChainHeptaBacktrackSampleFont.GLYPH_G,
                    GposChainHeptaBacktrackSampleFont.GLYPH_F,
                    GposChainHeptaBacktrackSampleFont.GLYPH_E,
                    GposChainHeptaBacktrackSampleFont.GLYPH_D,
                    GposChainHeptaBacktrackSampleFont.GLYPH_A,
                    GposChainHeptaBacktrackSampleFont.GLYPH_B,
                    GposChainHeptaBacktrackSampleFont.GLYPH_C
                },
                7,
                3
        ) != GposChainHeptaBacktrackSampleFont.CHAIN_DELTA) {
            throw new IllegalStateException("GPOS seven-glyph chain backtrack leftover did not require JIHGFED");
        }
        SfntFont gsubHepta = GsubChainHeptaBacktrackSampleFont.create();
        if (gsubHepta.chainSubstitute(
                new int[] {
                    GsubChainHeptaBacktrackSampleFont.GLYPH_J,
                    GsubChainHeptaBacktrackSampleFont.GLYPH_I,
                    GsubChainHeptaBacktrackSampleFont.GLYPH_H,
                    GsubChainHeptaBacktrackSampleFont.GLYPH_G,
                    GsubChainHeptaBacktrackSampleFont.GLYPH_F,
                    GsubChainHeptaBacktrackSampleFont.GLYPH_E,
                    GsubChainHeptaBacktrackSampleFont.GLYPH_D,
                    GsubChainHeptaBacktrackSampleFont.GLYPH_A,
                    GsubChainHeptaBacktrackSampleFont.GLYPH_B,
                    GsubChainHeptaBacktrackSampleFont.GLYPH_C
                },
                7,
                3,
                SfntFont.TAG_CALT
        ) != GsubChainHeptaBacktrackSampleFont.GLYPH_Y) {
            throw new IllegalStateException("GSUB seven-glyph chain backtrack leftover did not require JIHGFED");
        }
        SfntFont octa = GposChainOctaBacktrackSampleFont.create();
        if (octa.chainAdjustment(
                new int[] {
                    GposChainOctaBacktrackSampleFont.GLYPH_K,
                    GposChainOctaBacktrackSampleFont.GLYPH_J,
                    GposChainOctaBacktrackSampleFont.GLYPH_I,
                    GposChainOctaBacktrackSampleFont.GLYPH_H,
                    GposChainOctaBacktrackSampleFont.GLYPH_G,
                    GposChainOctaBacktrackSampleFont.GLYPH_F,
                    GposChainOctaBacktrackSampleFont.GLYPH_E,
                    GposChainOctaBacktrackSampleFont.GLYPH_D,
                    GposChainOctaBacktrackSampleFont.GLYPH_A,
                    GposChainOctaBacktrackSampleFont.GLYPH_B,
                    GposChainOctaBacktrackSampleFont.GLYPH_C
                },
                8,
                3
        ) != GposChainOctaBacktrackSampleFont.CHAIN_DELTA) {
            throw new IllegalStateException("GPOS eight-glyph chain backtrack leftover did not require KJIHGFED");
        }
        SfntFont gsubOcta = GsubChainOctaBacktrackSampleFont.create();
        if (gsubOcta.chainSubstitute(
                new int[] {
                    GsubChainOctaBacktrackSampleFont.GLYPH_K,
                    GsubChainOctaBacktrackSampleFont.GLYPH_J,
                    GsubChainOctaBacktrackSampleFont.GLYPH_I,
                    GsubChainOctaBacktrackSampleFont.GLYPH_H,
                    GsubChainOctaBacktrackSampleFont.GLYPH_G,
                    GsubChainOctaBacktrackSampleFont.GLYPH_F,
                    GsubChainOctaBacktrackSampleFont.GLYPH_E,
                    GsubChainOctaBacktrackSampleFont.GLYPH_D,
                    GsubChainOctaBacktrackSampleFont.GLYPH_A,
                    GsubChainOctaBacktrackSampleFont.GLYPH_B,
                    GsubChainOctaBacktrackSampleFont.GLYPH_C
                },
                8,
                3,
                SfntFont.TAG_CALT
        ) != GsubChainOctaBacktrackSampleFont.GLYPH_Y) {
            throw new IllegalStateException("GSUB eight-glyph chain backtrack leftover did not require KJIHGFED");
        }
        SfntFont nona = GposChainNonaBacktrackSampleFont.create();
        if (nona.chainAdjustment(
                new int[] {
                    GposChainNonaBacktrackSampleFont.GLYPH_L,
                    GposChainNonaBacktrackSampleFont.GLYPH_K,
                    GposChainNonaBacktrackSampleFont.GLYPH_J,
                    GposChainNonaBacktrackSampleFont.GLYPH_I,
                    GposChainNonaBacktrackSampleFont.GLYPH_H,
                    GposChainNonaBacktrackSampleFont.GLYPH_G,
                    GposChainNonaBacktrackSampleFont.GLYPH_F,
                    GposChainNonaBacktrackSampleFont.GLYPH_E,
                    GposChainNonaBacktrackSampleFont.GLYPH_D,
                    GposChainNonaBacktrackSampleFont.GLYPH_A,
                    GposChainNonaBacktrackSampleFont.GLYPH_B,
                    GposChainNonaBacktrackSampleFont.GLYPH_C
                },
                9,
                3
        ) != GposChainNonaBacktrackSampleFont.CHAIN_DELTA) {
            throw new IllegalStateException("GPOS nine-glyph chain backtrack leftover did not require LKJIHGFED");
        }
        SfntFont deca = GposChainDecaBacktrackSampleFont.create();
        if (deca.chainAdjustment(
                new int[] {
                    GposChainDecaBacktrackSampleFont.GLYPH_M,
                    GposChainDecaBacktrackSampleFont.GLYPH_L,
                    GposChainDecaBacktrackSampleFont.GLYPH_K,
                    GposChainDecaBacktrackSampleFont.GLYPH_J,
                    GposChainDecaBacktrackSampleFont.GLYPH_I,
                    GposChainDecaBacktrackSampleFont.GLYPH_H,
                    GposChainDecaBacktrackSampleFont.GLYPH_G,
                    GposChainDecaBacktrackSampleFont.GLYPH_F,
                    GposChainDecaBacktrackSampleFont.GLYPH_E,
                    GposChainDecaBacktrackSampleFont.GLYPH_D,
                    GposChainDecaBacktrackSampleFont.GLYPH_A,
                    GposChainDecaBacktrackSampleFont.GLYPH_B,
                    GposChainDecaBacktrackSampleFont.GLYPH_C
                },
                10,
                3
        ) != GposChainDecaBacktrackSampleFont.CHAIN_DELTA) {
            throw new IllegalStateException("GPOS ten-glyph chain backtrack leftover did not require MLKJIHGFED");
        }
        if (GsubChainDecaBacktrackSampleFont.create()
                        .chainSubstitute(
                                new int[] {
                                    GsubChainDecaBacktrackSampleFont.GLYPH_M,
                                    GsubChainDecaBacktrackSampleFont.GLYPH_L,
                                    GsubChainDecaBacktrackSampleFont.GLYPH_K,
                                    GsubChainDecaBacktrackSampleFont.GLYPH_J,
                                    GsubChainDecaBacktrackSampleFont.GLYPH_I,
                                    GsubChainDecaBacktrackSampleFont.GLYPH_H,
                                    GsubChainDecaBacktrackSampleFont.GLYPH_G,
                                    GsubChainDecaBacktrackSampleFont.GLYPH_F,
                                    GsubChainDecaBacktrackSampleFont.GLYPH_E,
                                    GsubChainDecaBacktrackSampleFont.GLYPH_D,
                                    GsubChainDecaBacktrackSampleFont.GLYPH_A,
                                    GsubChainDecaBacktrackSampleFont.GLYPH_B,
                                    GsubChainDecaBacktrackSampleFont.GLYPH_C
                                },
                                10,
                                3,
                                SfntFont.TAG_CALT
                        )
                != GsubChainDecaBacktrackSampleFont.GLYPH_Y) {
            throw new IllegalStateException("GSUB ten-glyph chain backtrack leftover did not require MLKJIHGFED");
        }
        if (GposChainUndecaBacktrackSampleFont.create()
                        .chainAdjustment(
                                new int[] {
                                    GposChainUndecaBacktrackSampleFont.GLYPH_N,
                                    GposChainUndecaBacktrackSampleFont.GLYPH_M,
                                    GposChainUndecaBacktrackSampleFont.GLYPH_L,
                                    GposChainUndecaBacktrackSampleFont.GLYPH_K,
                                    GposChainUndecaBacktrackSampleFont.GLYPH_J,
                                    GposChainUndecaBacktrackSampleFont.GLYPH_I,
                                    GposChainUndecaBacktrackSampleFont.GLYPH_H,
                                    GposChainUndecaBacktrackSampleFont.GLYPH_G,
                                    GposChainUndecaBacktrackSampleFont.GLYPH_F,
                                    GposChainUndecaBacktrackSampleFont.GLYPH_E,
                                    GposChainUndecaBacktrackSampleFont.GLYPH_D,
                                    GposChainUndecaBacktrackSampleFont.GLYPH_A,
                                    GposChainUndecaBacktrackSampleFont.GLYPH_B,
                                    GposChainUndecaBacktrackSampleFont.GLYPH_C
                                },
                                11,
                                3
                        )
                != GposChainUndecaBacktrackSampleFont.CHAIN_DELTA) {
            throw new IllegalStateException("GPOS eleven-glyph chain backtrack leftover did not require NMLKJIHGFED");
        }
        if (GsubChainUndecaBacktrackSampleFont.create()
                        .chainSubstitute(
                                new int[] {
                                    GsubChainUndecaBacktrackSampleFont.GLYPH_N,
                                    GsubChainUndecaBacktrackSampleFont.GLYPH_M,
                                    GsubChainUndecaBacktrackSampleFont.GLYPH_L,
                                    GsubChainUndecaBacktrackSampleFont.GLYPH_K,
                                    GsubChainUndecaBacktrackSampleFont.GLYPH_J,
                                    GsubChainUndecaBacktrackSampleFont.GLYPH_I,
                                    GsubChainUndecaBacktrackSampleFont.GLYPH_H,
                                    GsubChainUndecaBacktrackSampleFont.GLYPH_G,
                                    GsubChainUndecaBacktrackSampleFont.GLYPH_F,
                                    GsubChainUndecaBacktrackSampleFont.GLYPH_E,
                                    GsubChainUndecaBacktrackSampleFont.GLYPH_D,
                                    GsubChainUndecaBacktrackSampleFont.GLYPH_A,
                                    GsubChainUndecaBacktrackSampleFont.GLYPH_B,
                                    GsubChainUndecaBacktrackSampleFont.GLYPH_C
                                },
                                11,
                                3,
                                SfntFont.TAG_CALT
                        )
                != GsubChainUndecaBacktrackSampleFont.GLYPH_Y) {
            throw new IllegalStateException("GSUB eleven-glyph chain backtrack leftover did not require NMLKJIHGFED");
        }
        if (GposChainDodecaBacktrackSampleFont.create()
                        .chainAdjustment(
                                new int[] {
                                    GposChainDodecaBacktrackSampleFont.GLYPH_O,
                                    GposChainDodecaBacktrackSampleFont.GLYPH_N,
                                    GposChainDodecaBacktrackSampleFont.GLYPH_M,
                                    GposChainDodecaBacktrackSampleFont.GLYPH_L,
                                    GposChainDodecaBacktrackSampleFont.GLYPH_K,
                                    GposChainDodecaBacktrackSampleFont.GLYPH_J,
                                    GposChainDodecaBacktrackSampleFont.GLYPH_I,
                                    GposChainDodecaBacktrackSampleFont.GLYPH_H,
                                    GposChainDodecaBacktrackSampleFont.GLYPH_G,
                                    GposChainDodecaBacktrackSampleFont.GLYPH_F,
                                    GposChainDodecaBacktrackSampleFont.GLYPH_E,
                                    GposChainDodecaBacktrackSampleFont.GLYPH_D,
                                    GposChainDodecaBacktrackSampleFont.GLYPH_A,
                                    GposChainDodecaBacktrackSampleFont.GLYPH_B,
                                    GposChainDodecaBacktrackSampleFont.GLYPH_C
                                },
                                12,
                                3
                        )
                != GposChainDodecaBacktrackSampleFont.CHAIN_DELTA) {
            throw new IllegalStateException("GPOS twelve-glyph chain backtrack leftover did not require ONMLKJIHGFED");
        }
        if (GsubChainDodecaBacktrackSampleFont.create()
                        .chainSubstitute(
                                new int[] {
                                    GsubChainDodecaBacktrackSampleFont.GLYPH_O,
                                    GsubChainDodecaBacktrackSampleFont.GLYPH_N,
                                    GsubChainDodecaBacktrackSampleFont.GLYPH_M,
                                    GsubChainDodecaBacktrackSampleFont.GLYPH_L,
                                    GsubChainDodecaBacktrackSampleFont.GLYPH_K,
                                    GsubChainDodecaBacktrackSampleFont.GLYPH_J,
                                    GsubChainDodecaBacktrackSampleFont.GLYPH_I,
                                    GsubChainDodecaBacktrackSampleFont.GLYPH_H,
                                    GsubChainDodecaBacktrackSampleFont.GLYPH_G,
                                    GsubChainDodecaBacktrackSampleFont.GLYPH_F,
                                    GsubChainDodecaBacktrackSampleFont.GLYPH_E,
                                    GsubChainDodecaBacktrackSampleFont.GLYPH_D,
                                    GsubChainDodecaBacktrackSampleFont.GLYPH_A,
                                    GsubChainDodecaBacktrackSampleFont.GLYPH_B,
                                    GsubChainDodecaBacktrackSampleFont.GLYPH_C
                                },
                                12,
                                3,
                                SfntFont.TAG_CALT
                        )
                != GsubChainDodecaBacktrackSampleFont.GLYPH_Y) {
            throw new IllegalStateException("GSUB twelve-glyph chain backtrack leftover did not require ONMLKJIHGFED");
        }
        if (GposChainTridecaBacktrackSampleFont.create()
                        .chainAdjustment(
                                new int[] {
                                    GposChainTridecaBacktrackSampleFont.GLYPH_P,
                                    GposChainTridecaBacktrackSampleFont.GLYPH_O,
                                    GposChainTridecaBacktrackSampleFont.GLYPH_N,
                                    GposChainTridecaBacktrackSampleFont.GLYPH_M,
                                    GposChainTridecaBacktrackSampleFont.GLYPH_L,
                                    GposChainTridecaBacktrackSampleFont.GLYPH_K,
                                    GposChainTridecaBacktrackSampleFont.GLYPH_J,
                                    GposChainTridecaBacktrackSampleFont.GLYPH_I,
                                    GposChainTridecaBacktrackSampleFont.GLYPH_H,
                                    GposChainTridecaBacktrackSampleFont.GLYPH_G,
                                    GposChainTridecaBacktrackSampleFont.GLYPH_F,
                                    GposChainTridecaBacktrackSampleFont.GLYPH_E,
                                    GposChainTridecaBacktrackSampleFont.GLYPH_D,
                                    GposChainTridecaBacktrackSampleFont.GLYPH_A,
                                    GposChainTridecaBacktrackSampleFont.GLYPH_B,
                                    GposChainTridecaBacktrackSampleFont.GLYPH_C
                                },
                                13,
                                3
                        )
                != GposChainTridecaBacktrackSampleFont.CHAIN_DELTA) {
            throw new IllegalStateException("GPOS thirteen-glyph chain backtrack leftover did not require PONMLKJIHGFED");
        }
        if (GsubChainTridecaBacktrackSampleFont.create()
                        .chainSubstitute(
                                new int[] {
                                    GsubChainTridecaBacktrackSampleFont.GLYPH_P,
                                    GsubChainTridecaBacktrackSampleFont.GLYPH_O,
                                    GsubChainTridecaBacktrackSampleFont.GLYPH_N,
                                    GsubChainTridecaBacktrackSampleFont.GLYPH_M,
                                    GsubChainTridecaBacktrackSampleFont.GLYPH_L,
                                    GsubChainTridecaBacktrackSampleFont.GLYPH_K,
                                    GsubChainTridecaBacktrackSampleFont.GLYPH_J,
                                    GsubChainTridecaBacktrackSampleFont.GLYPH_I,
                                    GsubChainTridecaBacktrackSampleFont.GLYPH_H,
                                    GsubChainTridecaBacktrackSampleFont.GLYPH_G,
                                    GsubChainTridecaBacktrackSampleFont.GLYPH_F,
                                    GsubChainTridecaBacktrackSampleFont.GLYPH_E,
                                    GsubChainTridecaBacktrackSampleFont.GLYPH_D,
                                    GsubChainTridecaBacktrackSampleFont.GLYPH_A,
                                    GsubChainTridecaBacktrackSampleFont.GLYPH_B,
                                    GsubChainTridecaBacktrackSampleFont.GLYPH_C
                                },
                                13,
                                3,
                                SfntFont.TAG_CALT
                        )
                != GsubChainTridecaBacktrackSampleFont.GLYPH_Y) {
            throw new IllegalStateException("GSUB thirteen-glyph chain backtrack leftover did not require PONMLKJIHGFED");
        }
        if (GposChainTetradecaBacktrackSampleFont.create()
                        .chainAdjustment(
                                new int[] {
                                    GposChainTetradecaBacktrackSampleFont.GLYPH_Q,
                                    GposChainTetradecaBacktrackSampleFont.GLYPH_P,
                                    GposChainTetradecaBacktrackSampleFont.GLYPH_O,
                                    GposChainTetradecaBacktrackSampleFont.GLYPH_N,
                                    GposChainTetradecaBacktrackSampleFont.GLYPH_M,
                                    GposChainTetradecaBacktrackSampleFont.GLYPH_L,
                                    GposChainTetradecaBacktrackSampleFont.GLYPH_K,
                                    GposChainTetradecaBacktrackSampleFont.GLYPH_J,
                                    GposChainTetradecaBacktrackSampleFont.GLYPH_I,
                                    GposChainTetradecaBacktrackSampleFont.GLYPH_H,
                                    GposChainTetradecaBacktrackSampleFont.GLYPH_G,
                                    GposChainTetradecaBacktrackSampleFont.GLYPH_F,
                                    GposChainTetradecaBacktrackSampleFont.GLYPH_E,
                                    GposChainTetradecaBacktrackSampleFont.GLYPH_D,
                                    GposChainTetradecaBacktrackSampleFont.GLYPH_A,
                                    GposChainTetradecaBacktrackSampleFont.GLYPH_B,
                                    GposChainTetradecaBacktrackSampleFont.GLYPH_C
                                },
                                14,
                                3
                        )
                != GposChainTetradecaBacktrackSampleFont.CHAIN_DELTA) {
            throw new IllegalStateException("GPOS fourteen-glyph chain backtrack leftover did not require QPONMLKJIHGFED");
        }
        if (GsubChainTetradecaBacktrackSampleFont.create()
                        .chainSubstitute(
                                new int[] {
                                    GsubChainTetradecaBacktrackSampleFont.GLYPH_Q,
                                    GsubChainTetradecaBacktrackSampleFont.GLYPH_P,
                                    GsubChainTetradecaBacktrackSampleFont.GLYPH_O,
                                    GsubChainTetradecaBacktrackSampleFont.GLYPH_N,
                                    GsubChainTetradecaBacktrackSampleFont.GLYPH_M,
                                    GsubChainTetradecaBacktrackSampleFont.GLYPH_L,
                                    GsubChainTetradecaBacktrackSampleFont.GLYPH_K,
                                    GsubChainTetradecaBacktrackSampleFont.GLYPH_J,
                                    GsubChainTetradecaBacktrackSampleFont.GLYPH_I,
                                    GsubChainTetradecaBacktrackSampleFont.GLYPH_H,
                                    GsubChainTetradecaBacktrackSampleFont.GLYPH_G,
                                    GsubChainTetradecaBacktrackSampleFont.GLYPH_F,
                                    GsubChainTetradecaBacktrackSampleFont.GLYPH_E,
                                    GsubChainTetradecaBacktrackSampleFont.GLYPH_D,
                                    GsubChainTetradecaBacktrackSampleFont.GLYPH_A,
                                    GsubChainTetradecaBacktrackSampleFont.GLYPH_B,
                                    GsubChainTetradecaBacktrackSampleFont.GLYPH_C
                                },
                                14,
                                3,
                                SfntFont.TAG_CALT
                        )
                != GsubChainTetradecaBacktrackSampleFont.GLYPH_Y) {
            throw new IllegalStateException("GSUB fourteen-glyph chain backtrack leftover did not require QPONMLKJIHGFED");
        }
        if (GposChainPentadecaBacktrackSampleFont.create()
                        .chainAdjustment(
                                new int[] {
                                    GposChainPentadecaBacktrackSampleFont.GLYPH_R,
                                    GposChainPentadecaBacktrackSampleFont.GLYPH_Q,
                                    GposChainPentadecaBacktrackSampleFont.GLYPH_P,
                                    GposChainPentadecaBacktrackSampleFont.GLYPH_O,
                                    GposChainPentadecaBacktrackSampleFont.GLYPH_N,
                                    GposChainPentadecaBacktrackSampleFont.GLYPH_M,
                                    GposChainPentadecaBacktrackSampleFont.GLYPH_L,
                                    GposChainPentadecaBacktrackSampleFont.GLYPH_K,
                                    GposChainPentadecaBacktrackSampleFont.GLYPH_J,
                                    GposChainPentadecaBacktrackSampleFont.GLYPH_I,
                                    GposChainPentadecaBacktrackSampleFont.GLYPH_H,
                                    GposChainPentadecaBacktrackSampleFont.GLYPH_G,
                                    GposChainPentadecaBacktrackSampleFont.GLYPH_F,
                                    GposChainPentadecaBacktrackSampleFont.GLYPH_E,
                                    GposChainPentadecaBacktrackSampleFont.GLYPH_D,
                                    GposChainPentadecaBacktrackSampleFont.GLYPH_A,
                                    GposChainPentadecaBacktrackSampleFont.GLYPH_B,
                                    GposChainPentadecaBacktrackSampleFont.GLYPH_C
                                },
                                15,
                                3
                        )
                != GposChainPentadecaBacktrackSampleFont.CHAIN_DELTA) {
            throw new IllegalStateException("GPOS fifteen-glyph chain backtrack leftover did not require RQPONMLKJIHGFED");
        }
        if (GsubChainPentadecaBacktrackSampleFont.create()
                        .chainSubstitute(
                                new int[] {
                                    GsubChainPentadecaBacktrackSampleFont.GLYPH_R,
                                    GsubChainPentadecaBacktrackSampleFont.GLYPH_Q,
                                    GsubChainPentadecaBacktrackSampleFont.GLYPH_P,
                                    GsubChainPentadecaBacktrackSampleFont.GLYPH_O,
                                    GsubChainPentadecaBacktrackSampleFont.GLYPH_N,
                                    GsubChainPentadecaBacktrackSampleFont.GLYPH_M,
                                    GsubChainPentadecaBacktrackSampleFont.GLYPH_L,
                                    GsubChainPentadecaBacktrackSampleFont.GLYPH_K,
                                    GsubChainPentadecaBacktrackSampleFont.GLYPH_J,
                                    GsubChainPentadecaBacktrackSampleFont.GLYPH_I,
                                    GsubChainPentadecaBacktrackSampleFont.GLYPH_H,
                                    GsubChainPentadecaBacktrackSampleFont.GLYPH_G,
                                    GsubChainPentadecaBacktrackSampleFont.GLYPH_F,
                                    GsubChainPentadecaBacktrackSampleFont.GLYPH_E,
                                    GsubChainPentadecaBacktrackSampleFont.GLYPH_D,
                                    GsubChainPentadecaBacktrackSampleFont.GLYPH_A,
                                    GsubChainPentadecaBacktrackSampleFont.GLYPH_B,
                                    GsubChainPentadecaBacktrackSampleFont.GLYPH_C
                                },
                                15,
                                3,
                                SfntFont.TAG_CALT
                        )
                != GsubChainPentadecaBacktrackSampleFont.GLYPH_Y) {
            throw new IllegalStateException("GSUB fifteen-glyph chain backtrack leftover did not require RQPONMLKJIHGFED");
        }
        if (GposChainHexadecaBacktrackSampleFont.create()
                        .chainAdjustment(
                                new int[] {
                                    GposChainHexadecaBacktrackSampleFont.GLYPH_S,
                                    GposChainHexadecaBacktrackSampleFont.GLYPH_R,
                                    GposChainHexadecaBacktrackSampleFont.GLYPH_Q,
                                    GposChainHexadecaBacktrackSampleFont.GLYPH_P,
                                    GposChainHexadecaBacktrackSampleFont.GLYPH_O,
                                    GposChainHexadecaBacktrackSampleFont.GLYPH_N,
                                    GposChainHexadecaBacktrackSampleFont.GLYPH_M,
                                    GposChainHexadecaBacktrackSampleFont.GLYPH_L,
                                    GposChainHexadecaBacktrackSampleFont.GLYPH_K,
                                    GposChainHexadecaBacktrackSampleFont.GLYPH_J,
                                    GposChainHexadecaBacktrackSampleFont.GLYPH_I,
                                    GposChainHexadecaBacktrackSampleFont.GLYPH_H,
                                    GposChainHexadecaBacktrackSampleFont.GLYPH_G,
                                    GposChainHexadecaBacktrackSampleFont.GLYPH_F,
                                    GposChainHexadecaBacktrackSampleFont.GLYPH_E,
                                    GposChainHexadecaBacktrackSampleFont.GLYPH_D,
                                    GposChainHexadecaBacktrackSampleFont.GLYPH_A,
                                    GposChainHexadecaBacktrackSampleFont.GLYPH_B,
                                    GposChainHexadecaBacktrackSampleFont.GLYPH_C
                                },
                                16,
                                3
                        )
                != GposChainHexadecaBacktrackSampleFont.CHAIN_DELTA) {
            throw new IllegalStateException("GPOS sixteen-glyph chain backtrack leftover did not require SRQPONMLKJIHGFED");
        }
        if (GsubChainHexadecaBacktrackSampleFont.create()
                        .chainSubstitute(
                                new int[] {
                                    GsubChainHexadecaBacktrackSampleFont.GLYPH_S,
                                    GsubChainHexadecaBacktrackSampleFont.GLYPH_R,
                                    GsubChainHexadecaBacktrackSampleFont.GLYPH_Q,
                                    GsubChainHexadecaBacktrackSampleFont.GLYPH_P,
                                    GsubChainHexadecaBacktrackSampleFont.GLYPH_O,
                                    GsubChainHexadecaBacktrackSampleFont.GLYPH_N,
                                    GsubChainHexadecaBacktrackSampleFont.GLYPH_M,
                                    GsubChainHexadecaBacktrackSampleFont.GLYPH_L,
                                    GsubChainHexadecaBacktrackSampleFont.GLYPH_K,
                                    GsubChainHexadecaBacktrackSampleFont.GLYPH_J,
                                    GsubChainHexadecaBacktrackSampleFont.GLYPH_I,
                                    GsubChainHexadecaBacktrackSampleFont.GLYPH_H,
                                    GsubChainHexadecaBacktrackSampleFont.GLYPH_G,
                                    GsubChainHexadecaBacktrackSampleFont.GLYPH_F,
                                    GsubChainHexadecaBacktrackSampleFont.GLYPH_E,
                                    GsubChainHexadecaBacktrackSampleFont.GLYPH_D,
                                    GsubChainHexadecaBacktrackSampleFont.GLYPH_A,
                                    GsubChainHexadecaBacktrackSampleFont.GLYPH_B,
                                    GsubChainHexadecaBacktrackSampleFont.GLYPH_C
                                },
                                16,
                                3,
                                SfntFont.TAG_CALT
                        )
                != GsubChainHexadecaBacktrackSampleFont.GLYPH_Y) {
            throw new IllegalStateException("GSUB sixteen-glyph chain backtrack leftover did not require SRQPONMLKJIHGFED");
        }
        if (GposChainHeptadecaBacktrackSampleFont.create()
                        .chainAdjustment(
                                new int[] {
                                    GposChainHeptadecaBacktrackSampleFont.GLYPH_T,
                                    GposChainHeptadecaBacktrackSampleFont.GLYPH_S,
                                    GposChainHeptadecaBacktrackSampleFont.GLYPH_R,
                                    GposChainHeptadecaBacktrackSampleFont.GLYPH_Q,
                                    GposChainHeptadecaBacktrackSampleFont.GLYPH_P,
                                    GposChainHeptadecaBacktrackSampleFont.GLYPH_O,
                                    GposChainHeptadecaBacktrackSampleFont.GLYPH_N,
                                    GposChainHeptadecaBacktrackSampleFont.GLYPH_M,
                                    GposChainHeptadecaBacktrackSampleFont.GLYPH_L,
                                    GposChainHeptadecaBacktrackSampleFont.GLYPH_K,
                                    GposChainHeptadecaBacktrackSampleFont.GLYPH_J,
                                    GposChainHeptadecaBacktrackSampleFont.GLYPH_I,
                                    GposChainHeptadecaBacktrackSampleFont.GLYPH_H,
                                    GposChainHeptadecaBacktrackSampleFont.GLYPH_G,
                                    GposChainHeptadecaBacktrackSampleFont.GLYPH_F,
                                    GposChainHeptadecaBacktrackSampleFont.GLYPH_E,
                                    GposChainHeptadecaBacktrackSampleFont.GLYPH_D,
                                    GposChainHeptadecaBacktrackSampleFont.GLYPH_A,
                                    GposChainHeptadecaBacktrackSampleFont.GLYPH_B,
                                    GposChainHeptadecaBacktrackSampleFont.GLYPH_C
                                },
                                17,
                                3
                        )
                != GposChainHeptadecaBacktrackSampleFont.CHAIN_DELTA) {
            throw new IllegalStateException("GPOS seventeen-glyph chain backtrack leftover did not require TSRQPONMLKJIHGFED");
        }
        if (GsubChainHeptadecaBacktrackSampleFont.create()
                        .chainSubstitute(
                                new int[] {
                                    GsubChainHeptadecaBacktrackSampleFont.GLYPH_T,
                                    GsubChainHeptadecaBacktrackSampleFont.GLYPH_S,
                                    GsubChainHeptadecaBacktrackSampleFont.GLYPH_R,
                                    GsubChainHeptadecaBacktrackSampleFont.GLYPH_Q,
                                    GsubChainHeptadecaBacktrackSampleFont.GLYPH_P,
                                    GsubChainHeptadecaBacktrackSampleFont.GLYPH_O,
                                    GsubChainHeptadecaBacktrackSampleFont.GLYPH_N,
                                    GsubChainHeptadecaBacktrackSampleFont.GLYPH_M,
                                    GsubChainHeptadecaBacktrackSampleFont.GLYPH_L,
                                    GsubChainHeptadecaBacktrackSampleFont.GLYPH_K,
                                    GsubChainHeptadecaBacktrackSampleFont.GLYPH_J,
                                    GsubChainHeptadecaBacktrackSampleFont.GLYPH_I,
                                    GsubChainHeptadecaBacktrackSampleFont.GLYPH_H,
                                    GsubChainHeptadecaBacktrackSampleFont.GLYPH_G,
                                    GsubChainHeptadecaBacktrackSampleFont.GLYPH_F,
                                    GsubChainHeptadecaBacktrackSampleFont.GLYPH_E,
                                    GsubChainHeptadecaBacktrackSampleFont.GLYPH_D,
                                    GsubChainHeptadecaBacktrackSampleFont.GLYPH_A,
                                    GsubChainHeptadecaBacktrackSampleFont.GLYPH_B,
                                    GsubChainHeptadecaBacktrackSampleFont.GLYPH_C
                                },
                                17,
                                3,
                                SfntFont.TAG_CALT
                        )
                != GsubChainHeptadecaBacktrackSampleFont.GLYPH_Y) {
            throw new IllegalStateException("GSUB seventeen-glyph chain backtrack leftover did not require TSRQPONMLKJIHGFED");
        }
        if (GposChainOctodecaBacktrackSampleFont.create()
                        .chainAdjustment(
                                new int[] {
                                    GposChainOctodecaBacktrackSampleFont.GLYPH_U,
                                    GposChainOctodecaBacktrackSampleFont.GLYPH_T,
                                    GposChainOctodecaBacktrackSampleFont.GLYPH_S,
                                    GposChainOctodecaBacktrackSampleFont.GLYPH_R,
                                    GposChainOctodecaBacktrackSampleFont.GLYPH_Q,
                                    GposChainOctodecaBacktrackSampleFont.GLYPH_P,
                                    GposChainOctodecaBacktrackSampleFont.GLYPH_O,
                                    GposChainOctodecaBacktrackSampleFont.GLYPH_N,
                                    GposChainOctodecaBacktrackSampleFont.GLYPH_M,
                                    GposChainOctodecaBacktrackSampleFont.GLYPH_L,
                                    GposChainOctodecaBacktrackSampleFont.GLYPH_K,
                                    GposChainOctodecaBacktrackSampleFont.GLYPH_J,
                                    GposChainOctodecaBacktrackSampleFont.GLYPH_I,
                                    GposChainOctodecaBacktrackSampleFont.GLYPH_H,
                                    GposChainOctodecaBacktrackSampleFont.GLYPH_G,
                                    GposChainOctodecaBacktrackSampleFont.GLYPH_F,
                                    GposChainOctodecaBacktrackSampleFont.GLYPH_E,
                                    GposChainOctodecaBacktrackSampleFont.GLYPH_D,
                                    GposChainOctodecaBacktrackSampleFont.GLYPH_A,
                                    GposChainOctodecaBacktrackSampleFont.GLYPH_B,
                                    GposChainOctodecaBacktrackSampleFont.GLYPH_C
                                },
                                18,
                                3
                        )
                != GposChainOctodecaBacktrackSampleFont.CHAIN_DELTA) {
            throw new IllegalStateException("GPOS eighteen-glyph chain backtrack leftover did not require UTSRQPONMLKJIHGFED");
        }
        if (GsubChainOctodecaBacktrackSampleFont.create()
                        .chainSubstitute(
                                new int[] {
                                    GsubChainOctodecaBacktrackSampleFont.GLYPH_U,
                                    GsubChainOctodecaBacktrackSampleFont.GLYPH_T,
                                    GsubChainOctodecaBacktrackSampleFont.GLYPH_S,
                                    GsubChainOctodecaBacktrackSampleFont.GLYPH_R,
                                    GsubChainOctodecaBacktrackSampleFont.GLYPH_Q,
                                    GsubChainOctodecaBacktrackSampleFont.GLYPH_P,
                                    GsubChainOctodecaBacktrackSampleFont.GLYPH_O,
                                    GsubChainOctodecaBacktrackSampleFont.GLYPH_N,
                                    GsubChainOctodecaBacktrackSampleFont.GLYPH_M,
                                    GsubChainOctodecaBacktrackSampleFont.GLYPH_L,
                                    GsubChainOctodecaBacktrackSampleFont.GLYPH_K,
                                    GsubChainOctodecaBacktrackSampleFont.GLYPH_J,
                                    GsubChainOctodecaBacktrackSampleFont.GLYPH_I,
                                    GsubChainOctodecaBacktrackSampleFont.GLYPH_H,
                                    GsubChainOctodecaBacktrackSampleFont.GLYPH_G,
                                    GsubChainOctodecaBacktrackSampleFont.GLYPH_F,
                                    GsubChainOctodecaBacktrackSampleFont.GLYPH_E,
                                    GsubChainOctodecaBacktrackSampleFont.GLYPH_D,
                                    GsubChainOctodecaBacktrackSampleFont.GLYPH_A,
                                    GsubChainOctodecaBacktrackSampleFont.GLYPH_B,
                                    GsubChainOctodecaBacktrackSampleFont.GLYPH_C
                                },
                                18,
                                3,
                                SfntFont.TAG_CALT
                        )
                != GsubChainOctodecaBacktrackSampleFont.GLYPH_Y) {
            throw new IllegalStateException("GSUB eighteen-glyph chain backtrack leftover did not require UTSRQPONMLKJIHGFED");
        }
        if (GposChainEnneadecaBacktrackSampleFont.create()
                        .chainAdjustment(
                                new int[] {
                                    GposChainEnneadecaBacktrackSampleFont.GLYPH_V,
                                    GposChainEnneadecaBacktrackSampleFont.GLYPH_U,
                                    GposChainEnneadecaBacktrackSampleFont.GLYPH_T,
                                    GposChainEnneadecaBacktrackSampleFont.GLYPH_S,
                                    GposChainEnneadecaBacktrackSampleFont.GLYPH_R,
                                    GposChainEnneadecaBacktrackSampleFont.GLYPH_Q,
                                    GposChainEnneadecaBacktrackSampleFont.GLYPH_P,
                                    GposChainEnneadecaBacktrackSampleFont.GLYPH_O,
                                    GposChainEnneadecaBacktrackSampleFont.GLYPH_N,
                                    GposChainEnneadecaBacktrackSampleFont.GLYPH_M,
                                    GposChainEnneadecaBacktrackSampleFont.GLYPH_L,
                                    GposChainEnneadecaBacktrackSampleFont.GLYPH_K,
                                    GposChainEnneadecaBacktrackSampleFont.GLYPH_J,
                                    GposChainEnneadecaBacktrackSampleFont.GLYPH_I,
                                    GposChainEnneadecaBacktrackSampleFont.GLYPH_H,
                                    GposChainEnneadecaBacktrackSampleFont.GLYPH_G,
                                    GposChainEnneadecaBacktrackSampleFont.GLYPH_F,
                                    GposChainEnneadecaBacktrackSampleFont.GLYPH_E,
                                    GposChainEnneadecaBacktrackSampleFont.GLYPH_D,
                                    GposChainEnneadecaBacktrackSampleFont.GLYPH_A,
                                    GposChainEnneadecaBacktrackSampleFont.GLYPH_B,
                                    GposChainEnneadecaBacktrackSampleFont.GLYPH_C
                                },
                                19,
                                3
                        )
                != GposChainEnneadecaBacktrackSampleFont.CHAIN_DELTA) {
            throw new IllegalStateException("GPOS nineteen-glyph chain backtrack leftover did not require VUTSRQPONMLKJIHGFED");
        }
        if (GsubChainEnneadecaBacktrackSampleFont.create()
                        .chainSubstitute(
                                new int[] {
                                    GsubChainEnneadecaBacktrackSampleFont.GLYPH_V,
                                    GsubChainEnneadecaBacktrackSampleFont.GLYPH_U,
                                    GsubChainEnneadecaBacktrackSampleFont.GLYPH_T,
                                    GsubChainEnneadecaBacktrackSampleFont.GLYPH_S,
                                    GsubChainEnneadecaBacktrackSampleFont.GLYPH_R,
                                    GsubChainEnneadecaBacktrackSampleFont.GLYPH_Q,
                                    GsubChainEnneadecaBacktrackSampleFont.GLYPH_P,
                                    GsubChainEnneadecaBacktrackSampleFont.GLYPH_O,
                                    GsubChainEnneadecaBacktrackSampleFont.GLYPH_N,
                                    GsubChainEnneadecaBacktrackSampleFont.GLYPH_M,
                                    GsubChainEnneadecaBacktrackSampleFont.GLYPH_L,
                                    GsubChainEnneadecaBacktrackSampleFont.GLYPH_K,
                                    GsubChainEnneadecaBacktrackSampleFont.GLYPH_J,
                                    GsubChainEnneadecaBacktrackSampleFont.GLYPH_I,
                                    GsubChainEnneadecaBacktrackSampleFont.GLYPH_H,
                                    GsubChainEnneadecaBacktrackSampleFont.GLYPH_G,
                                    GsubChainEnneadecaBacktrackSampleFont.GLYPH_F,
                                    GsubChainEnneadecaBacktrackSampleFont.GLYPH_E,
                                    GsubChainEnneadecaBacktrackSampleFont.GLYPH_D,
                                    GsubChainEnneadecaBacktrackSampleFont.GLYPH_A,
                                    GsubChainEnneadecaBacktrackSampleFont.GLYPH_B,
                                    GsubChainEnneadecaBacktrackSampleFont.GLYPH_C
                                },
                                19,
                                3,
                                SfntFont.TAG_CALT
                        )
                != GsubChainEnneadecaBacktrackSampleFont.GLYPH_Y) {
            throw new IllegalStateException("GSUB nineteen-glyph chain backtrack leftover did not require VUTSRQPONMLKJIHGFED");
        }
        if (GposChainIcosaBacktrackSampleFont.create()
                        .chainAdjustment(
                                new int[] {
                                    GposChainIcosaBacktrackSampleFont.GLYPH_W,
                                    GposChainIcosaBacktrackSampleFont.GLYPH_V,
                                    GposChainIcosaBacktrackSampleFont.GLYPH_U,
                                    GposChainIcosaBacktrackSampleFont.GLYPH_T,
                                    GposChainIcosaBacktrackSampleFont.GLYPH_S,
                                    GposChainIcosaBacktrackSampleFont.GLYPH_R,
                                    GposChainIcosaBacktrackSampleFont.GLYPH_Q,
                                    GposChainIcosaBacktrackSampleFont.GLYPH_P,
                                    GposChainIcosaBacktrackSampleFont.GLYPH_O,
                                    GposChainIcosaBacktrackSampleFont.GLYPH_N,
                                    GposChainIcosaBacktrackSampleFont.GLYPH_M,
                                    GposChainIcosaBacktrackSampleFont.GLYPH_L,
                                    GposChainIcosaBacktrackSampleFont.GLYPH_K,
                                    GposChainIcosaBacktrackSampleFont.GLYPH_J,
                                    GposChainIcosaBacktrackSampleFont.GLYPH_I,
                                    GposChainIcosaBacktrackSampleFont.GLYPH_H,
                                    GposChainIcosaBacktrackSampleFont.GLYPH_G,
                                    GposChainIcosaBacktrackSampleFont.GLYPH_F,
                                    GposChainIcosaBacktrackSampleFont.GLYPH_E,
                                    GposChainIcosaBacktrackSampleFont.GLYPH_D,
                                    GposChainIcosaBacktrackSampleFont.GLYPH_A,
                                    GposChainIcosaBacktrackSampleFont.GLYPH_B,
                                    GposChainIcosaBacktrackSampleFont.GLYPH_C
                                },
                                20,
                                3
                        )
                != GposChainIcosaBacktrackSampleFont.CHAIN_DELTA) {
            throw new IllegalStateException("GPOS twenty-glyph chain backtrack leftover did not require WVUTSRQPONMLKJIHGFED");
        }
        if (GsubChainIcosaBacktrackSampleFont.create()
                        .chainSubstitute(
                                new int[] {
                                    GsubChainIcosaBacktrackSampleFont.GLYPH_W,
                                    GsubChainIcosaBacktrackSampleFont.GLYPH_V,
                                    GsubChainIcosaBacktrackSampleFont.GLYPH_U,
                                    GsubChainIcosaBacktrackSampleFont.GLYPH_T,
                                    GsubChainIcosaBacktrackSampleFont.GLYPH_S,
                                    GsubChainIcosaBacktrackSampleFont.GLYPH_R,
                                    GsubChainIcosaBacktrackSampleFont.GLYPH_Q,
                                    GsubChainIcosaBacktrackSampleFont.GLYPH_P,
                                    GsubChainIcosaBacktrackSampleFont.GLYPH_O,
                                    GsubChainIcosaBacktrackSampleFont.GLYPH_N,
                                    GsubChainIcosaBacktrackSampleFont.GLYPH_M,
                                    GsubChainIcosaBacktrackSampleFont.GLYPH_L,
                                    GsubChainIcosaBacktrackSampleFont.GLYPH_K,
                                    GsubChainIcosaBacktrackSampleFont.GLYPH_J,
                                    GsubChainIcosaBacktrackSampleFont.GLYPH_I,
                                    GsubChainIcosaBacktrackSampleFont.GLYPH_H,
                                    GsubChainIcosaBacktrackSampleFont.GLYPH_G,
                                    GsubChainIcosaBacktrackSampleFont.GLYPH_F,
                                    GsubChainIcosaBacktrackSampleFont.GLYPH_E,
                                    GsubChainIcosaBacktrackSampleFont.GLYPH_D,
                                    GsubChainIcosaBacktrackSampleFont.GLYPH_A,
                                    GsubChainIcosaBacktrackSampleFont.GLYPH_B,
                                    GsubChainIcosaBacktrackSampleFont.GLYPH_C
                                },
                                20,
                                3,
                                SfntFont.TAG_CALT
                        )
                != GsubChainIcosaBacktrackSampleFont.GLYPH_Y) {
            throw new IllegalStateException("GSUB twenty-glyph chain backtrack leftover did not require WVUTSRQPONMLKJIHGFED");
        }
        if (GposChainHenicosaBacktrackSampleFont.create()
                        .chainAdjustment(
                                new int[] {
                                    GposChainHenicosaBacktrackSampleFont.GLYPH_X,
                                    GposChainHenicosaBacktrackSampleFont.GLYPH_W,
                                    GposChainHenicosaBacktrackSampleFont.GLYPH_V,
                                    GposChainHenicosaBacktrackSampleFont.GLYPH_U,
                                    GposChainHenicosaBacktrackSampleFont.GLYPH_T,
                                    GposChainHenicosaBacktrackSampleFont.GLYPH_S,
                                    GposChainHenicosaBacktrackSampleFont.GLYPH_R,
                                    GposChainHenicosaBacktrackSampleFont.GLYPH_Q,
                                    GposChainHenicosaBacktrackSampleFont.GLYPH_P,
                                    GposChainHenicosaBacktrackSampleFont.GLYPH_O,
                                    GposChainHenicosaBacktrackSampleFont.GLYPH_N,
                                    GposChainHenicosaBacktrackSampleFont.GLYPH_M,
                                    GposChainHenicosaBacktrackSampleFont.GLYPH_L,
                                    GposChainHenicosaBacktrackSampleFont.GLYPH_K,
                                    GposChainHenicosaBacktrackSampleFont.GLYPH_J,
                                    GposChainHenicosaBacktrackSampleFont.GLYPH_I,
                                    GposChainHenicosaBacktrackSampleFont.GLYPH_H,
                                    GposChainHenicosaBacktrackSampleFont.GLYPH_G,
                                    GposChainHenicosaBacktrackSampleFont.GLYPH_F,
                                    GposChainHenicosaBacktrackSampleFont.GLYPH_E,
                                    GposChainHenicosaBacktrackSampleFont.GLYPH_D,
                                    GposChainHenicosaBacktrackSampleFont.GLYPH_A,
                                    GposChainHenicosaBacktrackSampleFont.GLYPH_B,
                                    GposChainHenicosaBacktrackSampleFont.GLYPH_C
                                },
                                21,
                                3
                        )
                != GposChainHenicosaBacktrackSampleFont.CHAIN_DELTA) {
            throw new IllegalStateException("GPOS twenty-one-glyph chain backtrack leftover did not require XWVUTSRQPONMLKJIHGFED");
        }
        if (GsubChainHenicosaBacktrackSampleFont.create()
                        .chainSubstitute(
                                new int[] {
                                    GsubChainHenicosaBacktrackSampleFont.GLYPH_X,
                                    GsubChainHenicosaBacktrackSampleFont.GLYPH_W,
                                    GsubChainHenicosaBacktrackSampleFont.GLYPH_V,
                                    GsubChainHenicosaBacktrackSampleFont.GLYPH_U,
                                    GsubChainHenicosaBacktrackSampleFont.GLYPH_T,
                                    GsubChainHenicosaBacktrackSampleFont.GLYPH_S,
                                    GsubChainHenicosaBacktrackSampleFont.GLYPH_R,
                                    GsubChainHenicosaBacktrackSampleFont.GLYPH_Q,
                                    GsubChainHenicosaBacktrackSampleFont.GLYPH_P,
                                    GsubChainHenicosaBacktrackSampleFont.GLYPH_O,
                                    GsubChainHenicosaBacktrackSampleFont.GLYPH_N,
                                    GsubChainHenicosaBacktrackSampleFont.GLYPH_M,
                                    GsubChainHenicosaBacktrackSampleFont.GLYPH_L,
                                    GsubChainHenicosaBacktrackSampleFont.GLYPH_K,
                                    GsubChainHenicosaBacktrackSampleFont.GLYPH_J,
                                    GsubChainHenicosaBacktrackSampleFont.GLYPH_I,
                                    GsubChainHenicosaBacktrackSampleFont.GLYPH_H,
                                    GsubChainHenicosaBacktrackSampleFont.GLYPH_G,
                                    GsubChainHenicosaBacktrackSampleFont.GLYPH_F,
                                    GsubChainHenicosaBacktrackSampleFont.GLYPH_E,
                                    GsubChainHenicosaBacktrackSampleFont.GLYPH_D,
                                    GsubChainHenicosaBacktrackSampleFont.GLYPH_A,
                                    GsubChainHenicosaBacktrackSampleFont.GLYPH_B,
                                    GsubChainHenicosaBacktrackSampleFont.GLYPH_C
                                },
                                21,
                                3,
                                SfntFont.TAG_CALT
                        )
                != GsubChainHenicosaBacktrackSampleFont.GLYPH_Y) {
            throw new IllegalStateException("GSUB twenty-one-glyph chain backtrack leftover did not require XWVUTSRQPONMLKJIHGFED");
        }
        if (GposChainDocosaBacktrackSampleFont.create()
                        .chainAdjustment(
                                new int[] {
                                    GposChainDocosaBacktrackSampleFont.GLYPH_Y,
                                    GposChainDocosaBacktrackSampleFont.GLYPH_X,
                                    GposChainDocosaBacktrackSampleFont.GLYPH_W,
                                    GposChainDocosaBacktrackSampleFont.GLYPH_V,
                                    GposChainDocosaBacktrackSampleFont.GLYPH_U,
                                    GposChainDocosaBacktrackSampleFont.GLYPH_T,
                                    GposChainDocosaBacktrackSampleFont.GLYPH_S,
                                    GposChainDocosaBacktrackSampleFont.GLYPH_R,
                                    GposChainDocosaBacktrackSampleFont.GLYPH_Q,
                                    GposChainDocosaBacktrackSampleFont.GLYPH_P,
                                    GposChainDocosaBacktrackSampleFont.GLYPH_O,
                                    GposChainDocosaBacktrackSampleFont.GLYPH_N,
                                    GposChainDocosaBacktrackSampleFont.GLYPH_M,
                                    GposChainDocosaBacktrackSampleFont.GLYPH_L,
                                    GposChainDocosaBacktrackSampleFont.GLYPH_K,
                                    GposChainDocosaBacktrackSampleFont.GLYPH_J,
                                    GposChainDocosaBacktrackSampleFont.GLYPH_I,
                                    GposChainDocosaBacktrackSampleFont.GLYPH_H,
                                    GposChainDocosaBacktrackSampleFont.GLYPH_G,
                                    GposChainDocosaBacktrackSampleFont.GLYPH_F,
                                    GposChainDocosaBacktrackSampleFont.GLYPH_E,
                                    GposChainDocosaBacktrackSampleFont.GLYPH_D,
                                    GposChainDocosaBacktrackSampleFont.GLYPH_A,
                                    GposChainDocosaBacktrackSampleFont.GLYPH_B,
                                    GposChainDocosaBacktrackSampleFont.GLYPH_C
                                },
                                22,
                                3
                        )
                != GposChainDocosaBacktrackSampleFont.CHAIN_DELTA) {
            throw new IllegalStateException("GPOS twenty-two-glyph chain backtrack leftover did not require YXWVUTSRQPONMLKJIHGFED");
        }
        if (GsubChainDocosaBacktrackSampleFont.create()
                        .chainSubstitute(
                                new int[] {
                                    GsubChainDocosaBacktrackSampleFont.GLYPH_Y,
                                    GsubChainDocosaBacktrackSampleFont.GLYPH_X,
                                    GsubChainDocosaBacktrackSampleFont.GLYPH_W,
                                    GsubChainDocosaBacktrackSampleFont.GLYPH_V,
                                    GsubChainDocosaBacktrackSampleFont.GLYPH_U,
                                    GsubChainDocosaBacktrackSampleFont.GLYPH_T,
                                    GsubChainDocosaBacktrackSampleFont.GLYPH_S,
                                    GsubChainDocosaBacktrackSampleFont.GLYPH_R,
                                    GsubChainDocosaBacktrackSampleFont.GLYPH_Q,
                                    GsubChainDocosaBacktrackSampleFont.GLYPH_P,
                                    GsubChainDocosaBacktrackSampleFont.GLYPH_O,
                                    GsubChainDocosaBacktrackSampleFont.GLYPH_N,
                                    GsubChainDocosaBacktrackSampleFont.GLYPH_M,
                                    GsubChainDocosaBacktrackSampleFont.GLYPH_L,
                                    GsubChainDocosaBacktrackSampleFont.GLYPH_K,
                                    GsubChainDocosaBacktrackSampleFont.GLYPH_J,
                                    GsubChainDocosaBacktrackSampleFont.GLYPH_I,
                                    GsubChainDocosaBacktrackSampleFont.GLYPH_H,
                                    GsubChainDocosaBacktrackSampleFont.GLYPH_G,
                                    GsubChainDocosaBacktrackSampleFont.GLYPH_F,
                                    GsubChainDocosaBacktrackSampleFont.GLYPH_E,
                                    GsubChainDocosaBacktrackSampleFont.GLYPH_D,
                                    GsubChainDocosaBacktrackSampleFont.GLYPH_A,
                                    GsubChainDocosaBacktrackSampleFont.GLYPH_B,
                                    GsubChainDocosaBacktrackSampleFont.GLYPH_C
                                },
                                22,
                                3,
                                SfntFont.TAG_CALT
                        )
                != GsubChainDocosaBacktrackSampleFont.GLYPH_Z) {
            throw new IllegalStateException("GSUB twenty-two-glyph chain backtrack leftover did not require YXWVUTSRQPONMLKJIHGFED");
        }
        if (GposChainTricosaBacktrackSampleFont.create()
                        .chainAdjustment(
                                new int[] {
                                    GposChainTricosaBacktrackSampleFont.GLYPH_Z,
                                    GposChainTricosaBacktrackSampleFont.GLYPH_Y,
                                    GposChainTricosaBacktrackSampleFont.GLYPH_X,
                                    GposChainTricosaBacktrackSampleFont.GLYPH_W,
                                    GposChainTricosaBacktrackSampleFont.GLYPH_V,
                                    GposChainTricosaBacktrackSampleFont.GLYPH_U,
                                    GposChainTricosaBacktrackSampleFont.GLYPH_T,
                                    GposChainTricosaBacktrackSampleFont.GLYPH_S,
                                    GposChainTricosaBacktrackSampleFont.GLYPH_R,
                                    GposChainTricosaBacktrackSampleFont.GLYPH_Q,
                                    GposChainTricosaBacktrackSampleFont.GLYPH_P,
                                    GposChainTricosaBacktrackSampleFont.GLYPH_O,
                                    GposChainTricosaBacktrackSampleFont.GLYPH_N,
                                    GposChainTricosaBacktrackSampleFont.GLYPH_M,
                                    GposChainTricosaBacktrackSampleFont.GLYPH_L,
                                    GposChainTricosaBacktrackSampleFont.GLYPH_K,
                                    GposChainTricosaBacktrackSampleFont.GLYPH_J,
                                    GposChainTricosaBacktrackSampleFont.GLYPH_I,
                                    GposChainTricosaBacktrackSampleFont.GLYPH_H,
                                    GposChainTricosaBacktrackSampleFont.GLYPH_G,
                                    GposChainTricosaBacktrackSampleFont.GLYPH_F,
                                    GposChainTricosaBacktrackSampleFont.GLYPH_E,
                                    GposChainTricosaBacktrackSampleFont.GLYPH_D,
                                    GposChainTricosaBacktrackSampleFont.GLYPH_A,
                                    GposChainTricosaBacktrackSampleFont.GLYPH_B,
                                    GposChainTricosaBacktrackSampleFont.GLYPH_C
                                },
                                23,
                                3
                        )
                != GposChainTricosaBacktrackSampleFont.CHAIN_DELTA) {
            throw new IllegalStateException("GPOS twenty-three-glyph chain backtrack leftover did not require ZYXWVUTSRQPONMLKJIHGFED");
        }
        if (GsubChainTricosaBacktrackSampleFont.create()
                        .chainSubstitute(
                                new int[] {
                                    GsubChainTricosaBacktrackSampleFont.GLYPH_Z,
                                    GsubChainTricosaBacktrackSampleFont.GLYPH_Y,
                                    GsubChainTricosaBacktrackSampleFont.GLYPH_X,
                                    GsubChainTricosaBacktrackSampleFont.GLYPH_W,
                                    GsubChainTricosaBacktrackSampleFont.GLYPH_V,
                                    GsubChainTricosaBacktrackSampleFont.GLYPH_U,
                                    GsubChainTricosaBacktrackSampleFont.GLYPH_T,
                                    GsubChainTricosaBacktrackSampleFont.GLYPH_S,
                                    GsubChainTricosaBacktrackSampleFont.GLYPH_R,
                                    GsubChainTricosaBacktrackSampleFont.GLYPH_Q,
                                    GsubChainTricosaBacktrackSampleFont.GLYPH_P,
                                    GsubChainTricosaBacktrackSampleFont.GLYPH_O,
                                    GsubChainTricosaBacktrackSampleFont.GLYPH_N,
                                    GsubChainTricosaBacktrackSampleFont.GLYPH_M,
                                    GsubChainTricosaBacktrackSampleFont.GLYPH_L,
                                    GsubChainTricosaBacktrackSampleFont.GLYPH_K,
                                    GsubChainTricosaBacktrackSampleFont.GLYPH_J,
                                    GsubChainTricosaBacktrackSampleFont.GLYPH_I,
                                    GsubChainTricosaBacktrackSampleFont.GLYPH_H,
                                    GsubChainTricosaBacktrackSampleFont.GLYPH_G,
                                    GsubChainTricosaBacktrackSampleFont.GLYPH_F,
                                    GsubChainTricosaBacktrackSampleFont.GLYPH_E,
                                    GsubChainTricosaBacktrackSampleFont.GLYPH_D,
                                    GsubChainTricosaBacktrackSampleFont.GLYPH_A,
                                    GsubChainTricosaBacktrackSampleFont.GLYPH_B,
                                    GsubChainTricosaBacktrackSampleFont.GLYPH_C
                                },
                                23,
                                3,
                                SfntFont.TAG_CALT
                        )
                != GsubChainTricosaBacktrackSampleFont.GLYPH_ZERO) {
            throw new IllegalStateException("GSUB twenty-three-glyph chain backtrack leftover did not require ZYXWVUTSRQPONMLKJIHGFED");
        }
        if (GposChainTetracosaBacktrackSampleFont.create()
                        .chainAdjustment(
                                new int[] {
                                    GposChainTetracosaBacktrackSampleFont.GLYPH_ONE,
                                    GposChainTetracosaBacktrackSampleFont.GLYPH_Z,
                                    GposChainTetracosaBacktrackSampleFont.GLYPH_Y,
                                    GposChainTetracosaBacktrackSampleFont.GLYPH_X,
                                    GposChainTetracosaBacktrackSampleFont.GLYPH_W,
                                    GposChainTetracosaBacktrackSampleFont.GLYPH_V,
                                    GposChainTetracosaBacktrackSampleFont.GLYPH_U,
                                    GposChainTetracosaBacktrackSampleFont.GLYPH_T,
                                    GposChainTetracosaBacktrackSampleFont.GLYPH_S,
                                    GposChainTetracosaBacktrackSampleFont.GLYPH_R,
                                    GposChainTetracosaBacktrackSampleFont.GLYPH_Q,
                                    GposChainTetracosaBacktrackSampleFont.GLYPH_P,
                                    GposChainTetracosaBacktrackSampleFont.GLYPH_O,
                                    GposChainTetracosaBacktrackSampleFont.GLYPH_N,
                                    GposChainTetracosaBacktrackSampleFont.GLYPH_M,
                                    GposChainTetracosaBacktrackSampleFont.GLYPH_L,
                                    GposChainTetracosaBacktrackSampleFont.GLYPH_K,
                                    GposChainTetracosaBacktrackSampleFont.GLYPH_J,
                                    GposChainTetracosaBacktrackSampleFont.GLYPH_I,
                                    GposChainTetracosaBacktrackSampleFont.GLYPH_H,
                                    GposChainTetracosaBacktrackSampleFont.GLYPH_G,
                                    GposChainTetracosaBacktrackSampleFont.GLYPH_F,
                                    GposChainTetracosaBacktrackSampleFont.GLYPH_E,
                                    GposChainTetracosaBacktrackSampleFont.GLYPH_D,
                                    GposChainTetracosaBacktrackSampleFont.GLYPH_A,
                                    GposChainTetracosaBacktrackSampleFont.GLYPH_B,
                                    GposChainTetracosaBacktrackSampleFont.GLYPH_C
                                },
                                24,
                                3
                        )
                != GposChainTetracosaBacktrackSampleFont.CHAIN_DELTA) {
            throw new IllegalStateException("GPOS twenty-four-glyph chain backtrack leftover did not require 1ZYXWVUTSRQPONMLKJIHGFED");
        }
        if (GsubChainTetracosaBacktrackSampleFont.create()
                        .chainSubstitute(
                                new int[] {
                                    GsubChainTetracosaBacktrackSampleFont.GLYPH_ONE,
                                    GsubChainTetracosaBacktrackSampleFont.GLYPH_Z,
                                    GsubChainTetracosaBacktrackSampleFont.GLYPH_Y,
                                    GsubChainTetracosaBacktrackSampleFont.GLYPH_X,
                                    GsubChainTetracosaBacktrackSampleFont.GLYPH_W,
                                    GsubChainTetracosaBacktrackSampleFont.GLYPH_V,
                                    GsubChainTetracosaBacktrackSampleFont.GLYPH_U,
                                    GsubChainTetracosaBacktrackSampleFont.GLYPH_T,
                                    GsubChainTetracosaBacktrackSampleFont.GLYPH_S,
                                    GsubChainTetracosaBacktrackSampleFont.GLYPH_R,
                                    GsubChainTetracosaBacktrackSampleFont.GLYPH_Q,
                                    GsubChainTetracosaBacktrackSampleFont.GLYPH_P,
                                    GsubChainTetracosaBacktrackSampleFont.GLYPH_O,
                                    GsubChainTetracosaBacktrackSampleFont.GLYPH_N,
                                    GsubChainTetracosaBacktrackSampleFont.GLYPH_M,
                                    GsubChainTetracosaBacktrackSampleFont.GLYPH_L,
                                    GsubChainTetracosaBacktrackSampleFont.GLYPH_K,
                                    GsubChainTetracosaBacktrackSampleFont.GLYPH_J,
                                    GsubChainTetracosaBacktrackSampleFont.GLYPH_I,
                                    GsubChainTetracosaBacktrackSampleFont.GLYPH_H,
                                    GsubChainTetracosaBacktrackSampleFont.GLYPH_G,
                                    GsubChainTetracosaBacktrackSampleFont.GLYPH_F,
                                    GsubChainTetracosaBacktrackSampleFont.GLYPH_E,
                                    GsubChainTetracosaBacktrackSampleFont.GLYPH_D,
                                    GsubChainTetracosaBacktrackSampleFont.GLYPH_A,
                                    GsubChainTetracosaBacktrackSampleFont.GLYPH_B,
                                    GsubChainTetracosaBacktrackSampleFont.GLYPH_C
                                },
                                24,
                                3,
                                SfntFont.TAG_CALT
                        )
                != GsubChainTetracosaBacktrackSampleFont.GLYPH_ZERO) {
            throw new IllegalStateException("GSUB twenty-four-glyph chain backtrack leftover did not require 1ZYXWVUTSRQPONMLKJIHGFED");
        }
        if (GposChainPentacosaBacktrackSampleFont.create()
                        .chainAdjustment(
                                new int[] {
                                    GposChainPentacosaBacktrackSampleFont.GLYPH_TWO,
                                    GposChainPentacosaBacktrackSampleFont.GLYPH_ONE,
                                    GposChainPentacosaBacktrackSampleFont.GLYPH_Z,
                                    GposChainPentacosaBacktrackSampleFont.GLYPH_Y,
                                    GposChainPentacosaBacktrackSampleFont.GLYPH_X,
                                    GposChainPentacosaBacktrackSampleFont.GLYPH_W,
                                    GposChainPentacosaBacktrackSampleFont.GLYPH_V,
                                    GposChainPentacosaBacktrackSampleFont.GLYPH_U,
                                    GposChainPentacosaBacktrackSampleFont.GLYPH_T,
                                    GposChainPentacosaBacktrackSampleFont.GLYPH_S,
                                    GposChainPentacosaBacktrackSampleFont.GLYPH_R,
                                    GposChainPentacosaBacktrackSampleFont.GLYPH_Q,
                                    GposChainPentacosaBacktrackSampleFont.GLYPH_P,
                                    GposChainPentacosaBacktrackSampleFont.GLYPH_O,
                                    GposChainPentacosaBacktrackSampleFont.GLYPH_N,
                                    GposChainPentacosaBacktrackSampleFont.GLYPH_M,
                                    GposChainPentacosaBacktrackSampleFont.GLYPH_L,
                                    GposChainPentacosaBacktrackSampleFont.GLYPH_K,
                                    GposChainPentacosaBacktrackSampleFont.GLYPH_J,
                                    GposChainPentacosaBacktrackSampleFont.GLYPH_I,
                                    GposChainPentacosaBacktrackSampleFont.GLYPH_H,
                                    GposChainPentacosaBacktrackSampleFont.GLYPH_G,
                                    GposChainPentacosaBacktrackSampleFont.GLYPH_F,
                                    GposChainPentacosaBacktrackSampleFont.GLYPH_E,
                                    GposChainPentacosaBacktrackSampleFont.GLYPH_D,
                                    GposChainPentacosaBacktrackSampleFont.GLYPH_A,
                                    GposChainPentacosaBacktrackSampleFont.GLYPH_B,
                                    GposChainPentacosaBacktrackSampleFont.GLYPH_C
                                },
                                25,
                                3
                        )
                != GposChainPentacosaBacktrackSampleFont.CHAIN_DELTA) {
            throw new IllegalStateException("GPOS twenty-five-glyph chain backtrack leftover did not require 21ZYXWVUTSRQPONMLKJIHGFED");
        }
        if (GsubChainPentacosaBacktrackSampleFont.create()
                        .chainSubstitute(
                                new int[] {
                                    GsubChainPentacosaBacktrackSampleFont.GLYPH_TWO,
                                    GsubChainPentacosaBacktrackSampleFont.GLYPH_ONE,
                                    GsubChainPentacosaBacktrackSampleFont.GLYPH_Z,
                                    GsubChainPentacosaBacktrackSampleFont.GLYPH_Y,
                                    GsubChainPentacosaBacktrackSampleFont.GLYPH_X,
                                    GsubChainPentacosaBacktrackSampleFont.GLYPH_W,
                                    GsubChainPentacosaBacktrackSampleFont.GLYPH_V,
                                    GsubChainPentacosaBacktrackSampleFont.GLYPH_U,
                                    GsubChainPentacosaBacktrackSampleFont.GLYPH_T,
                                    GsubChainPentacosaBacktrackSampleFont.GLYPH_S,
                                    GsubChainPentacosaBacktrackSampleFont.GLYPH_R,
                                    GsubChainPentacosaBacktrackSampleFont.GLYPH_Q,
                                    GsubChainPentacosaBacktrackSampleFont.GLYPH_P,
                                    GsubChainPentacosaBacktrackSampleFont.GLYPH_O,
                                    GsubChainPentacosaBacktrackSampleFont.GLYPH_N,
                                    GsubChainPentacosaBacktrackSampleFont.GLYPH_M,
                                    GsubChainPentacosaBacktrackSampleFont.GLYPH_L,
                                    GsubChainPentacosaBacktrackSampleFont.GLYPH_K,
                                    GsubChainPentacosaBacktrackSampleFont.GLYPH_J,
                                    GsubChainPentacosaBacktrackSampleFont.GLYPH_I,
                                    GsubChainPentacosaBacktrackSampleFont.GLYPH_H,
                                    GsubChainPentacosaBacktrackSampleFont.GLYPH_G,
                                    GsubChainPentacosaBacktrackSampleFont.GLYPH_F,
                                    GsubChainPentacosaBacktrackSampleFont.GLYPH_E,
                                    GsubChainPentacosaBacktrackSampleFont.GLYPH_D,
                                    GsubChainPentacosaBacktrackSampleFont.GLYPH_A,
                                    GsubChainPentacosaBacktrackSampleFont.GLYPH_B,
                                    GsubChainPentacosaBacktrackSampleFont.GLYPH_C
                                },
                                25,
                                3,
                                SfntFont.TAG_CALT
                        )
                != GsubChainPentacosaBacktrackSampleFont.GLYPH_ZERO) {
            throw new IllegalStateException("GSUB twenty-five-glyph chain backtrack leftover did not require 21ZYXWVUTSRQPONMLKJIHGFED");
        }
        if (GposChainHexacosaBacktrackSampleFont.create()
                        .chainAdjustment(
                                new int[] {
                                    GposChainHexacosaBacktrackSampleFont.GLYPH_THREE,
                                    GposChainHexacosaBacktrackSampleFont.GLYPH_TWO,
                                    GposChainHexacosaBacktrackSampleFont.GLYPH_ONE,
                                    GposChainHexacosaBacktrackSampleFont.GLYPH_Z,
                                    GposChainHexacosaBacktrackSampleFont.GLYPH_Y,
                                    GposChainHexacosaBacktrackSampleFont.GLYPH_X,
                                    GposChainHexacosaBacktrackSampleFont.GLYPH_W,
                                    GposChainHexacosaBacktrackSampleFont.GLYPH_V,
                                    GposChainHexacosaBacktrackSampleFont.GLYPH_U,
                                    GposChainHexacosaBacktrackSampleFont.GLYPH_T,
                                    GposChainHexacosaBacktrackSampleFont.GLYPH_S,
                                    GposChainHexacosaBacktrackSampleFont.GLYPH_R,
                                    GposChainHexacosaBacktrackSampleFont.GLYPH_Q,
                                    GposChainHexacosaBacktrackSampleFont.GLYPH_P,
                                    GposChainHexacosaBacktrackSampleFont.GLYPH_O,
                                    GposChainHexacosaBacktrackSampleFont.GLYPH_N,
                                    GposChainHexacosaBacktrackSampleFont.GLYPH_M,
                                    GposChainHexacosaBacktrackSampleFont.GLYPH_L,
                                    GposChainHexacosaBacktrackSampleFont.GLYPH_K,
                                    GposChainHexacosaBacktrackSampleFont.GLYPH_J,
                                    GposChainHexacosaBacktrackSampleFont.GLYPH_I,
                                    GposChainHexacosaBacktrackSampleFont.GLYPH_H,
                                    GposChainHexacosaBacktrackSampleFont.GLYPH_G,
                                    GposChainHexacosaBacktrackSampleFont.GLYPH_F,
                                    GposChainHexacosaBacktrackSampleFont.GLYPH_E,
                                    GposChainHexacosaBacktrackSampleFont.GLYPH_D,
                                    GposChainHexacosaBacktrackSampleFont.GLYPH_A,
                                    GposChainHexacosaBacktrackSampleFont.GLYPH_B,
                                    GposChainHexacosaBacktrackSampleFont.GLYPH_C
                                },
                                26,
                                3
                        )
                != GposChainHexacosaBacktrackSampleFont.CHAIN_DELTA) {
            throw new IllegalStateException("GPOS twenty-six-glyph chain backtrack leftover did not require 321ZYXWVUTSRQPONMLKJIHGFED");
        }
        if (GsubChainHexacosaBacktrackSampleFont.create()
                        .chainSubstitute(
                                new int[] {
                                    GsubChainHexacosaBacktrackSampleFont.GLYPH_THREE,
                                    GsubChainHexacosaBacktrackSampleFont.GLYPH_TWO,
                                    GsubChainHexacosaBacktrackSampleFont.GLYPH_ONE,
                                    GsubChainHexacosaBacktrackSampleFont.GLYPH_Z,
                                    GsubChainHexacosaBacktrackSampleFont.GLYPH_Y,
                                    GsubChainHexacosaBacktrackSampleFont.GLYPH_X,
                                    GsubChainHexacosaBacktrackSampleFont.GLYPH_W,
                                    GsubChainHexacosaBacktrackSampleFont.GLYPH_V,
                                    GsubChainHexacosaBacktrackSampleFont.GLYPH_U,
                                    GsubChainHexacosaBacktrackSampleFont.GLYPH_T,
                                    GsubChainHexacosaBacktrackSampleFont.GLYPH_S,
                                    GsubChainHexacosaBacktrackSampleFont.GLYPH_R,
                                    GsubChainHexacosaBacktrackSampleFont.GLYPH_Q,
                                    GsubChainHexacosaBacktrackSampleFont.GLYPH_P,
                                    GsubChainHexacosaBacktrackSampleFont.GLYPH_O,
                                    GsubChainHexacosaBacktrackSampleFont.GLYPH_N,
                                    GsubChainHexacosaBacktrackSampleFont.GLYPH_M,
                                    GsubChainHexacosaBacktrackSampleFont.GLYPH_L,
                                    GsubChainHexacosaBacktrackSampleFont.GLYPH_K,
                                    GsubChainHexacosaBacktrackSampleFont.GLYPH_J,
                                    GsubChainHexacosaBacktrackSampleFont.GLYPH_I,
                                    GsubChainHexacosaBacktrackSampleFont.GLYPH_H,
                                    GsubChainHexacosaBacktrackSampleFont.GLYPH_G,
                                    GsubChainHexacosaBacktrackSampleFont.GLYPH_F,
                                    GsubChainHexacosaBacktrackSampleFont.GLYPH_E,
                                    GsubChainHexacosaBacktrackSampleFont.GLYPH_D,
                                    GsubChainHexacosaBacktrackSampleFont.GLYPH_A,
                                    GsubChainHexacosaBacktrackSampleFont.GLYPH_B,
                                    GsubChainHexacosaBacktrackSampleFont.GLYPH_C
                                },
                                26,
                                3,
                                SfntFont.TAG_CALT
                        )
                != GsubChainHexacosaBacktrackSampleFont.GLYPH_ZERO) {
            throw new IllegalStateException("GSUB twenty-six-glyph chain backtrack leftover did not require 321ZYXWVUTSRQPONMLKJIHGFED");
        }
        if (GposChainHeptacosaBacktrackSampleFont.create()
                        .chainAdjustment(
                                new int[] {
                                    GposChainHeptacosaBacktrackSampleFont.GLYPH_FOUR,
                                    GposChainHeptacosaBacktrackSampleFont.GLYPH_THREE,
                                    GposChainHeptacosaBacktrackSampleFont.GLYPH_TWO,
                                    GposChainHeptacosaBacktrackSampleFont.GLYPH_ONE,
                                    GposChainHeptacosaBacktrackSampleFont.GLYPH_Z,
                                    GposChainHeptacosaBacktrackSampleFont.GLYPH_Y,
                                    GposChainHeptacosaBacktrackSampleFont.GLYPH_X,
                                    GposChainHeptacosaBacktrackSampleFont.GLYPH_W,
                                    GposChainHeptacosaBacktrackSampleFont.GLYPH_V,
                                    GposChainHeptacosaBacktrackSampleFont.GLYPH_U,
                                    GposChainHeptacosaBacktrackSampleFont.GLYPH_T,
                                    GposChainHeptacosaBacktrackSampleFont.GLYPH_S,
                                    GposChainHeptacosaBacktrackSampleFont.GLYPH_R,
                                    GposChainHeptacosaBacktrackSampleFont.GLYPH_Q,
                                    GposChainHeptacosaBacktrackSampleFont.GLYPH_P,
                                    GposChainHeptacosaBacktrackSampleFont.GLYPH_O,
                                    GposChainHeptacosaBacktrackSampleFont.GLYPH_N,
                                    GposChainHeptacosaBacktrackSampleFont.GLYPH_M,
                                    GposChainHeptacosaBacktrackSampleFont.GLYPH_L,
                                    GposChainHeptacosaBacktrackSampleFont.GLYPH_K,
                                    GposChainHeptacosaBacktrackSampleFont.GLYPH_J,
                                    GposChainHeptacosaBacktrackSampleFont.GLYPH_I,
                                    GposChainHeptacosaBacktrackSampleFont.GLYPH_H,
                                    GposChainHeptacosaBacktrackSampleFont.GLYPH_G,
                                    GposChainHeptacosaBacktrackSampleFont.GLYPH_F,
                                    GposChainHeptacosaBacktrackSampleFont.GLYPH_E,
                                    GposChainHeptacosaBacktrackSampleFont.GLYPH_D,
                                    GposChainHeptacosaBacktrackSampleFont.GLYPH_A,
                                    GposChainHeptacosaBacktrackSampleFont.GLYPH_B,
                                    GposChainHeptacosaBacktrackSampleFont.GLYPH_C,
                                },
                                27,
                                3
                        )
                != GposChainHeptacosaBacktrackSampleFont.CHAIN_DELTA) {
            throw new IllegalStateException("GPOS twenty-seven-glyph chain backtrack leftover did not require 4321ZYXWVUTSRQPONMLKJIHGFED");
        }
        if (GsubChainHeptacosaBacktrackSampleFont.create()
                        .chainSubstitute(
                                new int[] {
                                    GsubChainHeptacosaBacktrackSampleFont.GLYPH_FOUR,
                                    GsubChainHeptacosaBacktrackSampleFont.GLYPH_THREE,
                                    GsubChainHeptacosaBacktrackSampleFont.GLYPH_TWO,
                                    GsubChainHeptacosaBacktrackSampleFont.GLYPH_ONE,
                                    GsubChainHeptacosaBacktrackSampleFont.GLYPH_Z,
                                    GsubChainHeptacosaBacktrackSampleFont.GLYPH_Y,
                                    GsubChainHeptacosaBacktrackSampleFont.GLYPH_X,
                                    GsubChainHeptacosaBacktrackSampleFont.GLYPH_W,
                                    GsubChainHeptacosaBacktrackSampleFont.GLYPH_V,
                                    GsubChainHeptacosaBacktrackSampleFont.GLYPH_U,
                                    GsubChainHeptacosaBacktrackSampleFont.GLYPH_T,
                                    GsubChainHeptacosaBacktrackSampleFont.GLYPH_S,
                                    GsubChainHeptacosaBacktrackSampleFont.GLYPH_R,
                                    GsubChainHeptacosaBacktrackSampleFont.GLYPH_Q,
                                    GsubChainHeptacosaBacktrackSampleFont.GLYPH_P,
                                    GsubChainHeptacosaBacktrackSampleFont.GLYPH_O,
                                    GsubChainHeptacosaBacktrackSampleFont.GLYPH_N,
                                    GsubChainHeptacosaBacktrackSampleFont.GLYPH_M,
                                    GsubChainHeptacosaBacktrackSampleFont.GLYPH_L,
                                    GsubChainHeptacosaBacktrackSampleFont.GLYPH_K,
                                    GsubChainHeptacosaBacktrackSampleFont.GLYPH_J,
                                    GsubChainHeptacosaBacktrackSampleFont.GLYPH_I,
                                    GsubChainHeptacosaBacktrackSampleFont.GLYPH_H,
                                    GsubChainHeptacosaBacktrackSampleFont.GLYPH_G,
                                    GsubChainHeptacosaBacktrackSampleFont.GLYPH_F,
                                    GsubChainHeptacosaBacktrackSampleFont.GLYPH_E,
                                    GsubChainHeptacosaBacktrackSampleFont.GLYPH_D,
                                    GsubChainHeptacosaBacktrackSampleFont.GLYPH_A,
                                    GsubChainHeptacosaBacktrackSampleFont.GLYPH_B,
                                    GsubChainHeptacosaBacktrackSampleFont.GLYPH_C,
                                },
                                27,
                                3,
                                SfntFont.TAG_CALT
                        )
                != GsubChainHeptacosaBacktrackSampleFont.GLYPH_ZERO) {
            throw new IllegalStateException("GSUB twenty-seven-glyph chain backtrack leftover did not require 4321ZYXWVUTSRQPONMLKJIHGFED");
        }
        if (GposChainOctacosaBacktrackSampleFont.create()
                        .chainAdjustment(
                                new int[] {
                                    GposChainOctacosaBacktrackSampleFont.GLYPH_FIVE,
                                    GposChainOctacosaBacktrackSampleFont.GLYPH_FOUR,
                                    GposChainOctacosaBacktrackSampleFont.GLYPH_THREE,
                                    GposChainOctacosaBacktrackSampleFont.GLYPH_TWO,
                                    GposChainOctacosaBacktrackSampleFont.GLYPH_ONE,
                                    GposChainOctacosaBacktrackSampleFont.GLYPH_Z,
                                    GposChainOctacosaBacktrackSampleFont.GLYPH_Y,
                                    GposChainOctacosaBacktrackSampleFont.GLYPH_X,
                                    GposChainOctacosaBacktrackSampleFont.GLYPH_W,
                                    GposChainOctacosaBacktrackSampleFont.GLYPH_V,
                                    GposChainOctacosaBacktrackSampleFont.GLYPH_U,
                                    GposChainOctacosaBacktrackSampleFont.GLYPH_T,
                                    GposChainOctacosaBacktrackSampleFont.GLYPH_S,
                                    GposChainOctacosaBacktrackSampleFont.GLYPH_R,
                                    GposChainOctacosaBacktrackSampleFont.GLYPH_Q,
                                    GposChainOctacosaBacktrackSampleFont.GLYPH_P,
                                    GposChainOctacosaBacktrackSampleFont.GLYPH_O,
                                    GposChainOctacosaBacktrackSampleFont.GLYPH_N,
                                    GposChainOctacosaBacktrackSampleFont.GLYPH_M,
                                    GposChainOctacosaBacktrackSampleFont.GLYPH_L,
                                    GposChainOctacosaBacktrackSampleFont.GLYPH_K,
                                    GposChainOctacosaBacktrackSampleFont.GLYPH_J,
                                    GposChainOctacosaBacktrackSampleFont.GLYPH_I,
                                    GposChainOctacosaBacktrackSampleFont.GLYPH_H,
                                    GposChainOctacosaBacktrackSampleFont.GLYPH_G,
                                    GposChainOctacosaBacktrackSampleFont.GLYPH_F,
                                    GposChainOctacosaBacktrackSampleFont.GLYPH_E,
                                    GposChainOctacosaBacktrackSampleFont.GLYPH_D,
                                    GposChainOctacosaBacktrackSampleFont.GLYPH_A,
                                    GposChainOctacosaBacktrackSampleFont.GLYPH_B,
                                    GposChainOctacosaBacktrackSampleFont.GLYPH_C,
                                },
                                28,
                                3
                        )
                != GposChainOctacosaBacktrackSampleFont.CHAIN_DELTA) {
            throw new IllegalStateException("GPOS twenty-eight-glyph chain backtrack leftover did not require 54321ZYXWVUTSRQPONMLKJIHGFED");
        }
        if (GsubChainOctacosaBacktrackSampleFont.create()
                        .chainSubstitute(
                                new int[] {
                                    GsubChainOctacosaBacktrackSampleFont.GLYPH_FIVE,
                                    GsubChainOctacosaBacktrackSampleFont.GLYPH_FOUR,
                                    GsubChainOctacosaBacktrackSampleFont.GLYPH_THREE,
                                    GsubChainOctacosaBacktrackSampleFont.GLYPH_TWO,
                                    GsubChainOctacosaBacktrackSampleFont.GLYPH_ONE,
                                    GsubChainOctacosaBacktrackSampleFont.GLYPH_Z,
                                    GsubChainOctacosaBacktrackSampleFont.GLYPH_Y,
                                    GsubChainOctacosaBacktrackSampleFont.GLYPH_X,
                                    GsubChainOctacosaBacktrackSampleFont.GLYPH_W,
                                    GsubChainOctacosaBacktrackSampleFont.GLYPH_V,
                                    GsubChainOctacosaBacktrackSampleFont.GLYPH_U,
                                    GsubChainOctacosaBacktrackSampleFont.GLYPH_T,
                                    GsubChainOctacosaBacktrackSampleFont.GLYPH_S,
                                    GsubChainOctacosaBacktrackSampleFont.GLYPH_R,
                                    GsubChainOctacosaBacktrackSampleFont.GLYPH_Q,
                                    GsubChainOctacosaBacktrackSampleFont.GLYPH_P,
                                    GsubChainOctacosaBacktrackSampleFont.GLYPH_O,
                                    GsubChainOctacosaBacktrackSampleFont.GLYPH_N,
                                    GsubChainOctacosaBacktrackSampleFont.GLYPH_M,
                                    GsubChainOctacosaBacktrackSampleFont.GLYPH_L,
                                    GsubChainOctacosaBacktrackSampleFont.GLYPH_K,
                                    GsubChainOctacosaBacktrackSampleFont.GLYPH_J,
                                    GsubChainOctacosaBacktrackSampleFont.GLYPH_I,
                                    GsubChainOctacosaBacktrackSampleFont.GLYPH_H,
                                    GsubChainOctacosaBacktrackSampleFont.GLYPH_G,
                                    GsubChainOctacosaBacktrackSampleFont.GLYPH_F,
                                    GsubChainOctacosaBacktrackSampleFont.GLYPH_E,
                                    GsubChainOctacosaBacktrackSampleFont.GLYPH_D,
                                    GsubChainOctacosaBacktrackSampleFont.GLYPH_A,
                                    GsubChainOctacosaBacktrackSampleFont.GLYPH_B,
                                    GsubChainOctacosaBacktrackSampleFont.GLYPH_C,
                                },
                                28,
                                3,
                                SfntFont.TAG_CALT
                        )
                != GsubChainOctacosaBacktrackSampleFont.GLYPH_ZERO) {
            throw new IllegalStateException("GSUB twenty-eight-glyph chain backtrack leftover did not require 54321ZYXWVUTSRQPONMLKJIHGFED");
        }
        if (GposChainEnneacosaBacktrackSampleFont.create()
                        .chainAdjustment(
                                new int[] {
                                    GposChainEnneacosaBacktrackSampleFont.GLYPH_SIX,
                                    GposChainEnneacosaBacktrackSampleFont.GLYPH_FIVE,
                                    GposChainEnneacosaBacktrackSampleFont.GLYPH_FOUR,
                                    GposChainEnneacosaBacktrackSampleFont.GLYPH_THREE,
                                    GposChainEnneacosaBacktrackSampleFont.GLYPH_TWO,
                                    GposChainEnneacosaBacktrackSampleFont.GLYPH_ONE,
                                    GposChainEnneacosaBacktrackSampleFont.GLYPH_Z,
                                    GposChainEnneacosaBacktrackSampleFont.GLYPH_Y,
                                    GposChainEnneacosaBacktrackSampleFont.GLYPH_X,
                                    GposChainEnneacosaBacktrackSampleFont.GLYPH_W,
                                    GposChainEnneacosaBacktrackSampleFont.GLYPH_V,
                                    GposChainEnneacosaBacktrackSampleFont.GLYPH_U,
                                    GposChainEnneacosaBacktrackSampleFont.GLYPH_T,
                                    GposChainEnneacosaBacktrackSampleFont.GLYPH_S,
                                    GposChainEnneacosaBacktrackSampleFont.GLYPH_R,
                                    GposChainEnneacosaBacktrackSampleFont.GLYPH_Q,
                                    GposChainEnneacosaBacktrackSampleFont.GLYPH_P,
                                    GposChainEnneacosaBacktrackSampleFont.GLYPH_O,
                                    GposChainEnneacosaBacktrackSampleFont.GLYPH_N,
                                    GposChainEnneacosaBacktrackSampleFont.GLYPH_M,
                                    GposChainEnneacosaBacktrackSampleFont.GLYPH_L,
                                    GposChainEnneacosaBacktrackSampleFont.GLYPH_K,
                                    GposChainEnneacosaBacktrackSampleFont.GLYPH_J,
                                    GposChainEnneacosaBacktrackSampleFont.GLYPH_I,
                                    GposChainEnneacosaBacktrackSampleFont.GLYPH_H,
                                    GposChainEnneacosaBacktrackSampleFont.GLYPH_G,
                                    GposChainEnneacosaBacktrackSampleFont.GLYPH_F,
                                    GposChainEnneacosaBacktrackSampleFont.GLYPH_E,
                                    GposChainEnneacosaBacktrackSampleFont.GLYPH_D,
                                    GposChainEnneacosaBacktrackSampleFont.GLYPH_A,
                                    GposChainEnneacosaBacktrackSampleFont.GLYPH_B,
                                    GposChainEnneacosaBacktrackSampleFont.GLYPH_C,
                                },
                                29,
                                3
                        )
                != GposChainEnneacosaBacktrackSampleFont.CHAIN_DELTA) {
            throw new IllegalStateException("GPOS twenty-nine-glyph chain backtrack leftover did not require 654321ZYXWVUTSRQPONMLKJIHGFED");
        }
        if (GsubChainEnneacosaBacktrackSampleFont.create()
                        .chainSubstitute(
                                new int[] {
                                    GsubChainEnneacosaBacktrackSampleFont.GLYPH_SIX,
                                    GsubChainEnneacosaBacktrackSampleFont.GLYPH_FIVE,
                                    GsubChainEnneacosaBacktrackSampleFont.GLYPH_FOUR,
                                    GsubChainEnneacosaBacktrackSampleFont.GLYPH_THREE,
                                    GsubChainEnneacosaBacktrackSampleFont.GLYPH_TWO,
                                    GsubChainEnneacosaBacktrackSampleFont.GLYPH_ONE,
                                    GsubChainEnneacosaBacktrackSampleFont.GLYPH_Z,
                                    GsubChainEnneacosaBacktrackSampleFont.GLYPH_Y,
                                    GsubChainEnneacosaBacktrackSampleFont.GLYPH_X,
                                    GsubChainEnneacosaBacktrackSampleFont.GLYPH_W,
                                    GsubChainEnneacosaBacktrackSampleFont.GLYPH_V,
                                    GsubChainEnneacosaBacktrackSampleFont.GLYPH_U,
                                    GsubChainEnneacosaBacktrackSampleFont.GLYPH_T,
                                    GsubChainEnneacosaBacktrackSampleFont.GLYPH_S,
                                    GsubChainEnneacosaBacktrackSampleFont.GLYPH_R,
                                    GsubChainEnneacosaBacktrackSampleFont.GLYPH_Q,
                                    GsubChainEnneacosaBacktrackSampleFont.GLYPH_P,
                                    GsubChainEnneacosaBacktrackSampleFont.GLYPH_O,
                                    GsubChainEnneacosaBacktrackSampleFont.GLYPH_N,
                                    GsubChainEnneacosaBacktrackSampleFont.GLYPH_M,
                                    GsubChainEnneacosaBacktrackSampleFont.GLYPH_L,
                                    GsubChainEnneacosaBacktrackSampleFont.GLYPH_K,
                                    GsubChainEnneacosaBacktrackSampleFont.GLYPH_J,
                                    GsubChainEnneacosaBacktrackSampleFont.GLYPH_I,
                                    GsubChainEnneacosaBacktrackSampleFont.GLYPH_H,
                                    GsubChainEnneacosaBacktrackSampleFont.GLYPH_G,
                                    GsubChainEnneacosaBacktrackSampleFont.GLYPH_F,
                                    GsubChainEnneacosaBacktrackSampleFont.GLYPH_E,
                                    GsubChainEnneacosaBacktrackSampleFont.GLYPH_D,
                                    GsubChainEnneacosaBacktrackSampleFont.GLYPH_A,
                                    GsubChainEnneacosaBacktrackSampleFont.GLYPH_B,
                                    GsubChainEnneacosaBacktrackSampleFont.GLYPH_C,
                                },
                                29,
                                3,
                                SfntFont.TAG_CALT
                        )
                != GsubChainEnneacosaBacktrackSampleFont.GLYPH_ZERO) {
            throw new IllegalStateException("GSUB twenty-nine-glyph chain backtrack leftover did not require 654321ZYXWVUTSRQPONMLKJIHGFED");
        }
        if (GposChainTriacontaBacktrackSampleFont.create()
                        .chainAdjustment(
                                new int[] {
                                    GposChainTriacontaBacktrackSampleFont.GLYPH_SEVEN,
                                    GposChainTriacontaBacktrackSampleFont.GLYPH_SIX,
                                    GposChainTriacontaBacktrackSampleFont.GLYPH_FIVE,
                                    GposChainTriacontaBacktrackSampleFont.GLYPH_FOUR,
                                    GposChainTriacontaBacktrackSampleFont.GLYPH_THREE,
                                    GposChainTriacontaBacktrackSampleFont.GLYPH_TWO,
                                    GposChainTriacontaBacktrackSampleFont.GLYPH_ONE,
                                    GposChainTriacontaBacktrackSampleFont.GLYPH_Z,
                                    GposChainTriacontaBacktrackSampleFont.GLYPH_Y,
                                    GposChainTriacontaBacktrackSampleFont.GLYPH_X,
                                    GposChainTriacontaBacktrackSampleFont.GLYPH_W,
                                    GposChainTriacontaBacktrackSampleFont.GLYPH_V,
                                    GposChainTriacontaBacktrackSampleFont.GLYPH_U,
                                    GposChainTriacontaBacktrackSampleFont.GLYPH_T,
                                    GposChainTriacontaBacktrackSampleFont.GLYPH_S,
                                    GposChainTriacontaBacktrackSampleFont.GLYPH_R,
                                    GposChainTriacontaBacktrackSampleFont.GLYPH_Q,
                                    GposChainTriacontaBacktrackSampleFont.GLYPH_P,
                                    GposChainTriacontaBacktrackSampleFont.GLYPH_O,
                                    GposChainTriacontaBacktrackSampleFont.GLYPH_N,
                                    GposChainTriacontaBacktrackSampleFont.GLYPH_M,
                                    GposChainTriacontaBacktrackSampleFont.GLYPH_L,
                                    GposChainTriacontaBacktrackSampleFont.GLYPH_K,
                                    GposChainTriacontaBacktrackSampleFont.GLYPH_J,
                                    GposChainTriacontaBacktrackSampleFont.GLYPH_I,
                                    GposChainTriacontaBacktrackSampleFont.GLYPH_H,
                                    GposChainTriacontaBacktrackSampleFont.GLYPH_G,
                                    GposChainTriacontaBacktrackSampleFont.GLYPH_F,
                                    GposChainTriacontaBacktrackSampleFont.GLYPH_E,
                                    GposChainTriacontaBacktrackSampleFont.GLYPH_D,
                                    GposChainTriacontaBacktrackSampleFont.GLYPH_A,
                                    GposChainTriacontaBacktrackSampleFont.GLYPH_B,
                                    GposChainTriacontaBacktrackSampleFont.GLYPH_C,
                                },
                                30,
                                3
                        )
                != GposChainTriacontaBacktrackSampleFont.CHAIN_DELTA) {
            throw new IllegalStateException("GPOS thirty-glyph chain backtrack leftover did not require 7654321ZYXWVUTSRQPONMLKJIHGFED");
        }
        if (GsubChainTriacontaBacktrackSampleFont.create()
                        .chainSubstitute(
                                new int[] {
                                    GsubChainTriacontaBacktrackSampleFont.GLYPH_SEVEN,
                                    GsubChainTriacontaBacktrackSampleFont.GLYPH_SIX,
                                    GsubChainTriacontaBacktrackSampleFont.GLYPH_FIVE,
                                    GsubChainTriacontaBacktrackSampleFont.GLYPH_FOUR,
                                    GsubChainTriacontaBacktrackSampleFont.GLYPH_THREE,
                                    GsubChainTriacontaBacktrackSampleFont.GLYPH_TWO,
                                    GsubChainTriacontaBacktrackSampleFont.GLYPH_ONE,
                                    GsubChainTriacontaBacktrackSampleFont.GLYPH_Z,
                                    GsubChainTriacontaBacktrackSampleFont.GLYPH_Y,
                                    GsubChainTriacontaBacktrackSampleFont.GLYPH_X,
                                    GsubChainTriacontaBacktrackSampleFont.GLYPH_W,
                                    GsubChainTriacontaBacktrackSampleFont.GLYPH_V,
                                    GsubChainTriacontaBacktrackSampleFont.GLYPH_U,
                                    GsubChainTriacontaBacktrackSampleFont.GLYPH_T,
                                    GsubChainTriacontaBacktrackSampleFont.GLYPH_S,
                                    GsubChainTriacontaBacktrackSampleFont.GLYPH_R,
                                    GsubChainTriacontaBacktrackSampleFont.GLYPH_Q,
                                    GsubChainTriacontaBacktrackSampleFont.GLYPH_P,
                                    GsubChainTriacontaBacktrackSampleFont.GLYPH_O,
                                    GsubChainTriacontaBacktrackSampleFont.GLYPH_N,
                                    GsubChainTriacontaBacktrackSampleFont.GLYPH_M,
                                    GsubChainTriacontaBacktrackSampleFont.GLYPH_L,
                                    GsubChainTriacontaBacktrackSampleFont.GLYPH_K,
                                    GsubChainTriacontaBacktrackSampleFont.GLYPH_J,
                                    GsubChainTriacontaBacktrackSampleFont.GLYPH_I,
                                    GsubChainTriacontaBacktrackSampleFont.GLYPH_H,
                                    GsubChainTriacontaBacktrackSampleFont.GLYPH_G,
                                    GsubChainTriacontaBacktrackSampleFont.GLYPH_F,
                                    GsubChainTriacontaBacktrackSampleFont.GLYPH_E,
                                    GsubChainTriacontaBacktrackSampleFont.GLYPH_D,
                                    GsubChainTriacontaBacktrackSampleFont.GLYPH_A,
                                    GsubChainTriacontaBacktrackSampleFont.GLYPH_B,
                                    GsubChainTriacontaBacktrackSampleFont.GLYPH_C,
                                },
                                30,
                                3,
                                SfntFont.TAG_CALT
                        )
                != GsubChainTriacontaBacktrackSampleFont.GLYPH_ZERO) {
            throw new IllegalStateException("GSUB thirty-glyph chain backtrack leftover did not require 7654321ZYXWVUTSRQPONMLKJIHGFED");
        }
        if (GposChainHentricontaBacktrackSampleFont.create()
                        .chainAdjustment(
                                new int[] {
                                    GposChainHentricontaBacktrackSampleFont.GLYPH_EIGHT,
                                    GposChainHentricontaBacktrackSampleFont.GLYPH_SEVEN,
                                    GposChainHentricontaBacktrackSampleFont.GLYPH_SIX,
                                    GposChainHentricontaBacktrackSampleFont.GLYPH_FIVE,
                                    GposChainHentricontaBacktrackSampleFont.GLYPH_FOUR,
                                    GposChainHentricontaBacktrackSampleFont.GLYPH_THREE,
                                    GposChainHentricontaBacktrackSampleFont.GLYPH_TWO,
                                    GposChainHentricontaBacktrackSampleFont.GLYPH_ONE,
                                    GposChainHentricontaBacktrackSampleFont.GLYPH_Z,
                                    GposChainHentricontaBacktrackSampleFont.GLYPH_Y,
                                    GposChainHentricontaBacktrackSampleFont.GLYPH_X,
                                    GposChainHentricontaBacktrackSampleFont.GLYPH_W,
                                    GposChainHentricontaBacktrackSampleFont.GLYPH_V,
                                    GposChainHentricontaBacktrackSampleFont.GLYPH_U,
                                    GposChainHentricontaBacktrackSampleFont.GLYPH_T,
                                    GposChainHentricontaBacktrackSampleFont.GLYPH_S,
                                    GposChainHentricontaBacktrackSampleFont.GLYPH_R,
                                    GposChainHentricontaBacktrackSampleFont.GLYPH_Q,
                                    GposChainHentricontaBacktrackSampleFont.GLYPH_P,
                                    GposChainHentricontaBacktrackSampleFont.GLYPH_O,
                                    GposChainHentricontaBacktrackSampleFont.GLYPH_N,
                                    GposChainHentricontaBacktrackSampleFont.GLYPH_M,
                                    GposChainHentricontaBacktrackSampleFont.GLYPH_L,
                                    GposChainHentricontaBacktrackSampleFont.GLYPH_K,
                                    GposChainHentricontaBacktrackSampleFont.GLYPH_J,
                                    GposChainHentricontaBacktrackSampleFont.GLYPH_I,
                                    GposChainHentricontaBacktrackSampleFont.GLYPH_H,
                                    GposChainHentricontaBacktrackSampleFont.GLYPH_G,
                                    GposChainHentricontaBacktrackSampleFont.GLYPH_F,
                                    GposChainHentricontaBacktrackSampleFont.GLYPH_E,
                                    GposChainHentricontaBacktrackSampleFont.GLYPH_D,
                                    GposChainHentricontaBacktrackSampleFont.GLYPH_A,
                                    GposChainHentricontaBacktrackSampleFont.GLYPH_B,
                                    GposChainHentricontaBacktrackSampleFont.GLYPH_C,
                                },
                                31,
                                3
                        )
                != GposChainHentricontaBacktrackSampleFont.CHAIN_DELTA) {
            throw new IllegalStateException("GPOS thirty-one-glyph chain backtrack leftover did not require 87654321ZYXWVUTSRQPONMLKJIHGFED");
        }
        if (GsubChainHentricontaBacktrackSampleFont.create()
                        .chainSubstitute(
                                new int[] {
                                    GsubChainHentricontaBacktrackSampleFont.GLYPH_EIGHT,
                                    GsubChainHentricontaBacktrackSampleFont.GLYPH_SEVEN,
                                    GsubChainHentricontaBacktrackSampleFont.GLYPH_SIX,
                                    GsubChainHentricontaBacktrackSampleFont.GLYPH_FIVE,
                                    GsubChainHentricontaBacktrackSampleFont.GLYPH_FOUR,
                                    GsubChainHentricontaBacktrackSampleFont.GLYPH_THREE,
                                    GsubChainHentricontaBacktrackSampleFont.GLYPH_TWO,
                                    GsubChainHentricontaBacktrackSampleFont.GLYPH_ONE,
                                    GsubChainHentricontaBacktrackSampleFont.GLYPH_Z,
                                    GsubChainHentricontaBacktrackSampleFont.GLYPH_Y,
                                    GsubChainHentricontaBacktrackSampleFont.GLYPH_X,
                                    GsubChainHentricontaBacktrackSampleFont.GLYPH_W,
                                    GsubChainHentricontaBacktrackSampleFont.GLYPH_V,
                                    GsubChainHentricontaBacktrackSampleFont.GLYPH_U,
                                    GsubChainHentricontaBacktrackSampleFont.GLYPH_T,
                                    GsubChainHentricontaBacktrackSampleFont.GLYPH_S,
                                    GsubChainHentricontaBacktrackSampleFont.GLYPH_R,
                                    GsubChainHentricontaBacktrackSampleFont.GLYPH_Q,
                                    GsubChainHentricontaBacktrackSampleFont.GLYPH_P,
                                    GsubChainHentricontaBacktrackSampleFont.GLYPH_O,
                                    GsubChainHentricontaBacktrackSampleFont.GLYPH_N,
                                    GsubChainHentricontaBacktrackSampleFont.GLYPH_M,
                                    GsubChainHentricontaBacktrackSampleFont.GLYPH_L,
                                    GsubChainHentricontaBacktrackSampleFont.GLYPH_K,
                                    GsubChainHentricontaBacktrackSampleFont.GLYPH_J,
                                    GsubChainHentricontaBacktrackSampleFont.GLYPH_I,
                                    GsubChainHentricontaBacktrackSampleFont.GLYPH_H,
                                    GsubChainHentricontaBacktrackSampleFont.GLYPH_G,
                                    GsubChainHentricontaBacktrackSampleFont.GLYPH_F,
                                    GsubChainHentricontaBacktrackSampleFont.GLYPH_E,
                                    GsubChainHentricontaBacktrackSampleFont.GLYPH_D,
                                    GsubChainHentricontaBacktrackSampleFont.GLYPH_A,
                                    GsubChainHentricontaBacktrackSampleFont.GLYPH_B,
                                    GsubChainHentricontaBacktrackSampleFont.GLYPH_C,
                                },
                                31,
                                3,
                                SfntFont.TAG_CALT
                        )
                != GsubChainHentricontaBacktrackSampleFont.GLYPH_ZERO) {
            throw new IllegalStateException("GSUB thirty-one-glyph chain backtrack leftover did not require 87654321ZYXWVUTSRQPONMLKJIHGFED");
        }
        if (GposChainDotricontaBacktrackSampleFont.create()
                        .chainAdjustment(
                                new int[] {
                                    GposChainDotricontaBacktrackSampleFont.GLYPH_NINE,
                                    GposChainDotricontaBacktrackSampleFont.GLYPH_EIGHT,
                                    GposChainDotricontaBacktrackSampleFont.GLYPH_SEVEN,
                                    GposChainDotricontaBacktrackSampleFont.GLYPH_SIX,
                                    GposChainDotricontaBacktrackSampleFont.GLYPH_FIVE,
                                    GposChainDotricontaBacktrackSampleFont.GLYPH_FOUR,
                                    GposChainDotricontaBacktrackSampleFont.GLYPH_THREE,
                                    GposChainDotricontaBacktrackSampleFont.GLYPH_TWO,
                                    GposChainDotricontaBacktrackSampleFont.GLYPH_ONE,
                                    GposChainDotricontaBacktrackSampleFont.GLYPH_Z,
                                    GposChainDotricontaBacktrackSampleFont.GLYPH_Y,
                                    GposChainDotricontaBacktrackSampleFont.GLYPH_X,
                                    GposChainDotricontaBacktrackSampleFont.GLYPH_W,
                                    GposChainDotricontaBacktrackSampleFont.GLYPH_V,
                                    GposChainDotricontaBacktrackSampleFont.GLYPH_U,
                                    GposChainDotricontaBacktrackSampleFont.GLYPH_T,
                                    GposChainDotricontaBacktrackSampleFont.GLYPH_S,
                                    GposChainDotricontaBacktrackSampleFont.GLYPH_R,
                                    GposChainDotricontaBacktrackSampleFont.GLYPH_Q,
                                    GposChainDotricontaBacktrackSampleFont.GLYPH_P,
                                    GposChainDotricontaBacktrackSampleFont.GLYPH_O,
                                    GposChainDotricontaBacktrackSampleFont.GLYPH_N,
                                    GposChainDotricontaBacktrackSampleFont.GLYPH_M,
                                    GposChainDotricontaBacktrackSampleFont.GLYPH_L,
                                    GposChainDotricontaBacktrackSampleFont.GLYPH_K,
                                    GposChainDotricontaBacktrackSampleFont.GLYPH_J,
                                    GposChainDotricontaBacktrackSampleFont.GLYPH_I,
                                    GposChainDotricontaBacktrackSampleFont.GLYPH_H,
                                    GposChainDotricontaBacktrackSampleFont.GLYPH_G,
                                    GposChainDotricontaBacktrackSampleFont.GLYPH_F,
                                    GposChainDotricontaBacktrackSampleFont.GLYPH_E,
                                    GposChainDotricontaBacktrackSampleFont.GLYPH_D,
                                    GposChainDotricontaBacktrackSampleFont.GLYPH_A,
                                    GposChainDotricontaBacktrackSampleFont.GLYPH_B,
                                    GposChainDotricontaBacktrackSampleFont.GLYPH_C,
                                },
                                32,
                                3
                        )
                != GposChainDotricontaBacktrackSampleFont.CHAIN_DELTA) {
            throw new IllegalStateException("GPOS thirty-two-glyph chain backtrack leftover did not require 987654321ZYXWVUTSRQPONMLKJIHGFED");
        }
        if (GsubChainDotricontaBacktrackSampleFont.create()
                        .chainSubstitute(
                                new int[] {
                                    GsubChainDotricontaBacktrackSampleFont.GLYPH_NINE,
                                    GsubChainDotricontaBacktrackSampleFont.GLYPH_EIGHT,
                                    GsubChainDotricontaBacktrackSampleFont.GLYPH_SEVEN,
                                    GsubChainDotricontaBacktrackSampleFont.GLYPH_SIX,
                                    GsubChainDotricontaBacktrackSampleFont.GLYPH_FIVE,
                                    GsubChainDotricontaBacktrackSampleFont.GLYPH_FOUR,
                                    GsubChainDotricontaBacktrackSampleFont.GLYPH_THREE,
                                    GsubChainDotricontaBacktrackSampleFont.GLYPH_TWO,
                                    GsubChainDotricontaBacktrackSampleFont.GLYPH_ONE,
                                    GsubChainDotricontaBacktrackSampleFont.GLYPH_Z,
                                    GsubChainDotricontaBacktrackSampleFont.GLYPH_Y,
                                    GsubChainDotricontaBacktrackSampleFont.GLYPH_X,
                                    GsubChainDotricontaBacktrackSampleFont.GLYPH_W,
                                    GsubChainDotricontaBacktrackSampleFont.GLYPH_V,
                                    GsubChainDotricontaBacktrackSampleFont.GLYPH_U,
                                    GsubChainDotricontaBacktrackSampleFont.GLYPH_T,
                                    GsubChainDotricontaBacktrackSampleFont.GLYPH_S,
                                    GsubChainDotricontaBacktrackSampleFont.GLYPH_R,
                                    GsubChainDotricontaBacktrackSampleFont.GLYPH_Q,
                                    GsubChainDotricontaBacktrackSampleFont.GLYPH_P,
                                    GsubChainDotricontaBacktrackSampleFont.GLYPH_O,
                                    GsubChainDotricontaBacktrackSampleFont.GLYPH_N,
                                    GsubChainDotricontaBacktrackSampleFont.GLYPH_M,
                                    GsubChainDotricontaBacktrackSampleFont.GLYPH_L,
                                    GsubChainDotricontaBacktrackSampleFont.GLYPH_K,
                                    GsubChainDotricontaBacktrackSampleFont.GLYPH_J,
                                    GsubChainDotricontaBacktrackSampleFont.GLYPH_I,
                                    GsubChainDotricontaBacktrackSampleFont.GLYPH_H,
                                    GsubChainDotricontaBacktrackSampleFont.GLYPH_G,
                                    GsubChainDotricontaBacktrackSampleFont.GLYPH_F,
                                    GsubChainDotricontaBacktrackSampleFont.GLYPH_E,
                                    GsubChainDotricontaBacktrackSampleFont.GLYPH_D,
                                    GsubChainDotricontaBacktrackSampleFont.GLYPH_A,
                                    GsubChainDotricontaBacktrackSampleFont.GLYPH_B,
                                    GsubChainDotricontaBacktrackSampleFont.GLYPH_C,
                                },
                                32,
                                3,
                                SfntFont.TAG_CALT
                        )
                != GsubChainDotricontaBacktrackSampleFont.GLYPH_ZERO) {
            throw new IllegalStateException("GSUB thirty-two-glyph chain backtrack leftover did not require 987654321ZYXWVUTSRQPONMLKJIHGFED");
        }
        if (GposChainTritriacontaBacktrackSampleFont.create()
                        .chainAdjustment(
                                new int[] {
                                    GposChainTritriacontaBacktrackSampleFont.GLYPH_COLON,
                                    GposChainTritriacontaBacktrackSampleFont.GLYPH_NINE,
                                    GposChainTritriacontaBacktrackSampleFont.GLYPH_EIGHT,
                                    GposChainTritriacontaBacktrackSampleFont.GLYPH_SEVEN,
                                    GposChainTritriacontaBacktrackSampleFont.GLYPH_SIX,
                                    GposChainTritriacontaBacktrackSampleFont.GLYPH_FIVE,
                                    GposChainTritriacontaBacktrackSampleFont.GLYPH_FOUR,
                                    GposChainTritriacontaBacktrackSampleFont.GLYPH_THREE,
                                    GposChainTritriacontaBacktrackSampleFont.GLYPH_TWO,
                                    GposChainTritriacontaBacktrackSampleFont.GLYPH_ONE,
                                    GposChainTritriacontaBacktrackSampleFont.GLYPH_Z,
                                    GposChainTritriacontaBacktrackSampleFont.GLYPH_Y,
                                    GposChainTritriacontaBacktrackSampleFont.GLYPH_X,
                                    GposChainTritriacontaBacktrackSampleFont.GLYPH_W,
                                    GposChainTritriacontaBacktrackSampleFont.GLYPH_V,
                                    GposChainTritriacontaBacktrackSampleFont.GLYPH_U,
                                    GposChainTritriacontaBacktrackSampleFont.GLYPH_T,
                                    GposChainTritriacontaBacktrackSampleFont.GLYPH_S,
                                    GposChainTritriacontaBacktrackSampleFont.GLYPH_R,
                                    GposChainTritriacontaBacktrackSampleFont.GLYPH_Q,
                                    GposChainTritriacontaBacktrackSampleFont.GLYPH_P,
                                    GposChainTritriacontaBacktrackSampleFont.GLYPH_O,
                                    GposChainTritriacontaBacktrackSampleFont.GLYPH_N,
                                    GposChainTritriacontaBacktrackSampleFont.GLYPH_M,
                                    GposChainTritriacontaBacktrackSampleFont.GLYPH_L,
                                    GposChainTritriacontaBacktrackSampleFont.GLYPH_K,
                                    GposChainTritriacontaBacktrackSampleFont.GLYPH_J,
                                    GposChainTritriacontaBacktrackSampleFont.GLYPH_I,
                                    GposChainTritriacontaBacktrackSampleFont.GLYPH_H,
                                    GposChainTritriacontaBacktrackSampleFont.GLYPH_G,
                                    GposChainTritriacontaBacktrackSampleFont.GLYPH_F,
                                    GposChainTritriacontaBacktrackSampleFont.GLYPH_E,
                                    GposChainTritriacontaBacktrackSampleFont.GLYPH_D,
                                    GposChainTritriacontaBacktrackSampleFont.GLYPH_A,
                                    GposChainTritriacontaBacktrackSampleFont.GLYPH_B,
                                    GposChainTritriacontaBacktrackSampleFont.GLYPH_C,
                                },
                                33,
                                3
                        )
                != GposChainTritriacontaBacktrackSampleFont.CHAIN_DELTA) {
            throw new IllegalStateException("GPOS thirty-three-glyph chain backtrack leftover did not require :987654321ZYXWVUTSRQPONMLKJIHGFED");
        }
        if (GsubChainTritriacontaBacktrackSampleFont.create()
                        .chainSubstitute(
                                new int[] {
                                    GsubChainTritriacontaBacktrackSampleFont.GLYPH_COLON,
                                    GsubChainTritriacontaBacktrackSampleFont.GLYPH_NINE,
                                    GsubChainTritriacontaBacktrackSampleFont.GLYPH_EIGHT,
                                    GsubChainTritriacontaBacktrackSampleFont.GLYPH_SEVEN,
                                    GsubChainTritriacontaBacktrackSampleFont.GLYPH_SIX,
                                    GsubChainTritriacontaBacktrackSampleFont.GLYPH_FIVE,
                                    GsubChainTritriacontaBacktrackSampleFont.GLYPH_FOUR,
                                    GsubChainTritriacontaBacktrackSampleFont.GLYPH_THREE,
                                    GsubChainTritriacontaBacktrackSampleFont.GLYPH_TWO,
                                    GsubChainTritriacontaBacktrackSampleFont.GLYPH_ONE,
                                    GsubChainTritriacontaBacktrackSampleFont.GLYPH_Z,
                                    GsubChainTritriacontaBacktrackSampleFont.GLYPH_Y,
                                    GsubChainTritriacontaBacktrackSampleFont.GLYPH_X,
                                    GsubChainTritriacontaBacktrackSampleFont.GLYPH_W,
                                    GsubChainTritriacontaBacktrackSampleFont.GLYPH_V,
                                    GsubChainTritriacontaBacktrackSampleFont.GLYPH_U,
                                    GsubChainTritriacontaBacktrackSampleFont.GLYPH_T,
                                    GsubChainTritriacontaBacktrackSampleFont.GLYPH_S,
                                    GsubChainTritriacontaBacktrackSampleFont.GLYPH_R,
                                    GsubChainTritriacontaBacktrackSampleFont.GLYPH_Q,
                                    GsubChainTritriacontaBacktrackSampleFont.GLYPH_P,
                                    GsubChainTritriacontaBacktrackSampleFont.GLYPH_O,
                                    GsubChainTritriacontaBacktrackSampleFont.GLYPH_N,
                                    GsubChainTritriacontaBacktrackSampleFont.GLYPH_M,
                                    GsubChainTritriacontaBacktrackSampleFont.GLYPH_L,
                                    GsubChainTritriacontaBacktrackSampleFont.GLYPH_K,
                                    GsubChainTritriacontaBacktrackSampleFont.GLYPH_J,
                                    GsubChainTritriacontaBacktrackSampleFont.GLYPH_I,
                                    GsubChainTritriacontaBacktrackSampleFont.GLYPH_H,
                                    GsubChainTritriacontaBacktrackSampleFont.GLYPH_G,
                                    GsubChainTritriacontaBacktrackSampleFont.GLYPH_F,
                                    GsubChainTritriacontaBacktrackSampleFont.GLYPH_E,
                                    GsubChainTritriacontaBacktrackSampleFont.GLYPH_D,
                                    GsubChainTritriacontaBacktrackSampleFont.GLYPH_A,
                                    GsubChainTritriacontaBacktrackSampleFont.GLYPH_B,
                                    GsubChainTritriacontaBacktrackSampleFont.GLYPH_C,
                                },
                                33,
                                3,
                                SfntFont.TAG_CALT
                        )
                != GsubChainTritriacontaBacktrackSampleFont.GLYPH_ZERO) {
            throw new IllegalStateException("GSUB thirty-three-glyph chain backtrack leftover did not require :987654321ZYXWVUTSRQPONMLKJIHGFED");
        }
        if (GposChainTetratriacontaBacktrackSampleFont.create()
                        .chainAdjustment(
                                new int[] {
                                    GposChainTetratriacontaBacktrackSampleFont.GLYPH_SEMICOLON,
                                    GposChainTetratriacontaBacktrackSampleFont.GLYPH_COLON,
                                    GposChainTetratriacontaBacktrackSampleFont.GLYPH_NINE,
                                    GposChainTetratriacontaBacktrackSampleFont.GLYPH_EIGHT,
                                    GposChainTetratriacontaBacktrackSampleFont.GLYPH_SEVEN,
                                    GposChainTetratriacontaBacktrackSampleFont.GLYPH_SIX,
                                    GposChainTetratriacontaBacktrackSampleFont.GLYPH_FIVE,
                                    GposChainTetratriacontaBacktrackSampleFont.GLYPH_FOUR,
                                    GposChainTetratriacontaBacktrackSampleFont.GLYPH_THREE,
                                    GposChainTetratriacontaBacktrackSampleFont.GLYPH_TWO,
                                    GposChainTetratriacontaBacktrackSampleFont.GLYPH_ONE,
                                    GposChainTetratriacontaBacktrackSampleFont.GLYPH_Z,
                                    GposChainTetratriacontaBacktrackSampleFont.GLYPH_Y,
                                    GposChainTetratriacontaBacktrackSampleFont.GLYPH_X,
                                    GposChainTetratriacontaBacktrackSampleFont.GLYPH_W,
                                    GposChainTetratriacontaBacktrackSampleFont.GLYPH_V,
                                    GposChainTetratriacontaBacktrackSampleFont.GLYPH_U,
                                    GposChainTetratriacontaBacktrackSampleFont.GLYPH_T,
                                    GposChainTetratriacontaBacktrackSampleFont.GLYPH_S,
                                    GposChainTetratriacontaBacktrackSampleFont.GLYPH_R,
                                    GposChainTetratriacontaBacktrackSampleFont.GLYPH_Q,
                                    GposChainTetratriacontaBacktrackSampleFont.GLYPH_P,
                                    GposChainTetratriacontaBacktrackSampleFont.GLYPH_O,
                                    GposChainTetratriacontaBacktrackSampleFont.GLYPH_N,
                                    GposChainTetratriacontaBacktrackSampleFont.GLYPH_M,
                                    GposChainTetratriacontaBacktrackSampleFont.GLYPH_L,
                                    GposChainTetratriacontaBacktrackSampleFont.GLYPH_K,
                                    GposChainTetratriacontaBacktrackSampleFont.GLYPH_J,
                                    GposChainTetratriacontaBacktrackSampleFont.GLYPH_I,
                                    GposChainTetratriacontaBacktrackSampleFont.GLYPH_H,
                                    GposChainTetratriacontaBacktrackSampleFont.GLYPH_G,
                                    GposChainTetratriacontaBacktrackSampleFont.GLYPH_F,
                                    GposChainTetratriacontaBacktrackSampleFont.GLYPH_E,
                                    GposChainTetratriacontaBacktrackSampleFont.GLYPH_D,
                                    GposChainTetratriacontaBacktrackSampleFont.GLYPH_A,
                                    GposChainTetratriacontaBacktrackSampleFont.GLYPH_B,
                                    GposChainTetratriacontaBacktrackSampleFont.GLYPH_C,
                                },
                                34,
                                3
                        )
                != GposChainTetratriacontaBacktrackSampleFont.CHAIN_DELTA) {
            throw new IllegalStateException("GPOS thirty-four-glyph chain backtrack leftover did not require ;:987654321ZYXWVUTSRQPONMLKJIHGFED");
        }
        if (GsubChainTetratriacontaBacktrackSampleFont.create()
                        .chainSubstitute(
                                new int[] {
                                    GsubChainTetratriacontaBacktrackSampleFont.GLYPH_SEMICOLON,
                                    GsubChainTetratriacontaBacktrackSampleFont.GLYPH_COLON,
                                    GsubChainTetratriacontaBacktrackSampleFont.GLYPH_NINE,
                                    GsubChainTetratriacontaBacktrackSampleFont.GLYPH_EIGHT,
                                    GsubChainTetratriacontaBacktrackSampleFont.GLYPH_SEVEN,
                                    GsubChainTetratriacontaBacktrackSampleFont.GLYPH_SIX,
                                    GsubChainTetratriacontaBacktrackSampleFont.GLYPH_FIVE,
                                    GsubChainTetratriacontaBacktrackSampleFont.GLYPH_FOUR,
                                    GsubChainTetratriacontaBacktrackSampleFont.GLYPH_THREE,
                                    GsubChainTetratriacontaBacktrackSampleFont.GLYPH_TWO,
                                    GsubChainTetratriacontaBacktrackSampleFont.GLYPH_ONE,
                                    GsubChainTetratriacontaBacktrackSampleFont.GLYPH_Z,
                                    GsubChainTetratriacontaBacktrackSampleFont.GLYPH_Y,
                                    GsubChainTetratriacontaBacktrackSampleFont.GLYPH_X,
                                    GsubChainTetratriacontaBacktrackSampleFont.GLYPH_W,
                                    GsubChainTetratriacontaBacktrackSampleFont.GLYPH_V,
                                    GsubChainTetratriacontaBacktrackSampleFont.GLYPH_U,
                                    GsubChainTetratriacontaBacktrackSampleFont.GLYPH_T,
                                    GsubChainTetratriacontaBacktrackSampleFont.GLYPH_S,
                                    GsubChainTetratriacontaBacktrackSampleFont.GLYPH_R,
                                    GsubChainTetratriacontaBacktrackSampleFont.GLYPH_Q,
                                    GsubChainTetratriacontaBacktrackSampleFont.GLYPH_P,
                                    GsubChainTetratriacontaBacktrackSampleFont.GLYPH_O,
                                    GsubChainTetratriacontaBacktrackSampleFont.GLYPH_N,
                                    GsubChainTetratriacontaBacktrackSampleFont.GLYPH_M,
                                    GsubChainTetratriacontaBacktrackSampleFont.GLYPH_L,
                                    GsubChainTetratriacontaBacktrackSampleFont.GLYPH_K,
                                    GsubChainTetratriacontaBacktrackSampleFont.GLYPH_J,
                                    GsubChainTetratriacontaBacktrackSampleFont.GLYPH_I,
                                    GsubChainTetratriacontaBacktrackSampleFont.GLYPH_H,
                                    GsubChainTetratriacontaBacktrackSampleFont.GLYPH_G,
                                    GsubChainTetratriacontaBacktrackSampleFont.GLYPH_F,
                                    GsubChainTetratriacontaBacktrackSampleFont.GLYPH_E,
                                    GsubChainTetratriacontaBacktrackSampleFont.GLYPH_D,
                                    GsubChainTetratriacontaBacktrackSampleFont.GLYPH_A,
                                    GsubChainTetratriacontaBacktrackSampleFont.GLYPH_B,
                                    GsubChainTetratriacontaBacktrackSampleFont.GLYPH_C,
                                },
                                34,
                                3,
                                SfntFont.TAG_CALT
                        )
                != GsubChainTetratriacontaBacktrackSampleFont.GLYPH_ZERO) {
            throw new IllegalStateException("GSUB thirty-four-glyph chain backtrack leftover did not require ;:987654321ZYXWVUTSRQPONMLKJIHGFED");
        }
        if (GposChainPentatriacontaBacktrackSampleFont.create()
                        .chainAdjustment(
                                new int[] {
                                    GposChainPentatriacontaBacktrackSampleFont.GLYPH_LESS,
                                    GposChainPentatriacontaBacktrackSampleFont.GLYPH_SEMICOLON,
                                    GposChainPentatriacontaBacktrackSampleFont.GLYPH_COLON,
                                    GposChainPentatriacontaBacktrackSampleFont.GLYPH_NINE,
                                    GposChainPentatriacontaBacktrackSampleFont.GLYPH_EIGHT,
                                    GposChainPentatriacontaBacktrackSampleFont.GLYPH_SEVEN,
                                    GposChainPentatriacontaBacktrackSampleFont.GLYPH_SIX,
                                    GposChainPentatriacontaBacktrackSampleFont.GLYPH_FIVE,
                                    GposChainPentatriacontaBacktrackSampleFont.GLYPH_FOUR,
                                    GposChainPentatriacontaBacktrackSampleFont.GLYPH_THREE,
                                    GposChainPentatriacontaBacktrackSampleFont.GLYPH_TWO,
                                    GposChainPentatriacontaBacktrackSampleFont.GLYPH_ONE,
                                    GposChainPentatriacontaBacktrackSampleFont.GLYPH_Z,
                                    GposChainPentatriacontaBacktrackSampleFont.GLYPH_Y,
                                    GposChainPentatriacontaBacktrackSampleFont.GLYPH_X,
                                    GposChainPentatriacontaBacktrackSampleFont.GLYPH_W,
                                    GposChainPentatriacontaBacktrackSampleFont.GLYPH_V,
                                    GposChainPentatriacontaBacktrackSampleFont.GLYPH_U,
                                    GposChainPentatriacontaBacktrackSampleFont.GLYPH_T,
                                    GposChainPentatriacontaBacktrackSampleFont.GLYPH_S,
                                    GposChainPentatriacontaBacktrackSampleFont.GLYPH_R,
                                    GposChainPentatriacontaBacktrackSampleFont.GLYPH_Q,
                                    GposChainPentatriacontaBacktrackSampleFont.GLYPH_P,
                                    GposChainPentatriacontaBacktrackSampleFont.GLYPH_O,
                                    GposChainPentatriacontaBacktrackSampleFont.GLYPH_N,
                                    GposChainPentatriacontaBacktrackSampleFont.GLYPH_M,
                                    GposChainPentatriacontaBacktrackSampleFont.GLYPH_L,
                                    GposChainPentatriacontaBacktrackSampleFont.GLYPH_K,
                                    GposChainPentatriacontaBacktrackSampleFont.GLYPH_J,
                                    GposChainPentatriacontaBacktrackSampleFont.GLYPH_I,
                                    GposChainPentatriacontaBacktrackSampleFont.GLYPH_H,
                                    GposChainPentatriacontaBacktrackSampleFont.GLYPH_G,
                                    GposChainPentatriacontaBacktrackSampleFont.GLYPH_F,
                                    GposChainPentatriacontaBacktrackSampleFont.GLYPH_E,
                                    GposChainPentatriacontaBacktrackSampleFont.GLYPH_D,
                                    GposChainPentatriacontaBacktrackSampleFont.GLYPH_A,
                                    GposChainPentatriacontaBacktrackSampleFont.GLYPH_B,
                                    GposChainPentatriacontaBacktrackSampleFont.GLYPH_C,
                                },
                                35,
                                3
                        )
                != GposChainPentatriacontaBacktrackSampleFont.CHAIN_DELTA) {
            throw new IllegalStateException("GPOS thirty-five-glyph chain backtrack leftover did not require <;:987654321ZYXWVUTSRQPONMLKJIHGFED");
        }
        if (GsubChainPentatriacontaBacktrackSampleFont.create()
                        .chainSubstitute(
                                new int[] {
                                    GsubChainPentatriacontaBacktrackSampleFont.GLYPH_LESS,
                                    GsubChainPentatriacontaBacktrackSampleFont.GLYPH_SEMICOLON,
                                    GsubChainPentatriacontaBacktrackSampleFont.GLYPH_COLON,
                                    GsubChainPentatriacontaBacktrackSampleFont.GLYPH_NINE,
                                    GsubChainPentatriacontaBacktrackSampleFont.GLYPH_EIGHT,
                                    GsubChainPentatriacontaBacktrackSampleFont.GLYPH_SEVEN,
                                    GsubChainPentatriacontaBacktrackSampleFont.GLYPH_SIX,
                                    GsubChainPentatriacontaBacktrackSampleFont.GLYPH_FIVE,
                                    GsubChainPentatriacontaBacktrackSampleFont.GLYPH_FOUR,
                                    GsubChainPentatriacontaBacktrackSampleFont.GLYPH_THREE,
                                    GsubChainPentatriacontaBacktrackSampleFont.GLYPH_TWO,
                                    GsubChainPentatriacontaBacktrackSampleFont.GLYPH_ONE,
                                    GsubChainPentatriacontaBacktrackSampleFont.GLYPH_Z,
                                    GsubChainPentatriacontaBacktrackSampleFont.GLYPH_Y,
                                    GsubChainPentatriacontaBacktrackSampleFont.GLYPH_X,
                                    GsubChainPentatriacontaBacktrackSampleFont.GLYPH_W,
                                    GsubChainPentatriacontaBacktrackSampleFont.GLYPH_V,
                                    GsubChainPentatriacontaBacktrackSampleFont.GLYPH_U,
                                    GsubChainPentatriacontaBacktrackSampleFont.GLYPH_T,
                                    GsubChainPentatriacontaBacktrackSampleFont.GLYPH_S,
                                    GsubChainPentatriacontaBacktrackSampleFont.GLYPH_R,
                                    GsubChainPentatriacontaBacktrackSampleFont.GLYPH_Q,
                                    GsubChainPentatriacontaBacktrackSampleFont.GLYPH_P,
                                    GsubChainPentatriacontaBacktrackSampleFont.GLYPH_O,
                                    GsubChainPentatriacontaBacktrackSampleFont.GLYPH_N,
                                    GsubChainPentatriacontaBacktrackSampleFont.GLYPH_M,
                                    GsubChainPentatriacontaBacktrackSampleFont.GLYPH_L,
                                    GsubChainPentatriacontaBacktrackSampleFont.GLYPH_K,
                                    GsubChainPentatriacontaBacktrackSampleFont.GLYPH_J,
                                    GsubChainPentatriacontaBacktrackSampleFont.GLYPH_I,
                                    GsubChainPentatriacontaBacktrackSampleFont.GLYPH_H,
                                    GsubChainPentatriacontaBacktrackSampleFont.GLYPH_G,
                                    GsubChainPentatriacontaBacktrackSampleFont.GLYPH_F,
                                    GsubChainPentatriacontaBacktrackSampleFont.GLYPH_E,
                                    GsubChainPentatriacontaBacktrackSampleFont.GLYPH_D,
                                    GsubChainPentatriacontaBacktrackSampleFont.GLYPH_A,
                                    GsubChainPentatriacontaBacktrackSampleFont.GLYPH_B,
                                    GsubChainPentatriacontaBacktrackSampleFont.GLYPH_C,
                                },
                                35,
                                3,
                                SfntFont.TAG_CALT
                        )
                != GsubChainPentatriacontaBacktrackSampleFont.GLYPH_ZERO) {
            throw new IllegalStateException("GSUB thirty-five-glyph chain backtrack leftover did not require <;:987654321ZYXWVUTSRQPONMLKJIHGFED");
        }
        if (GposChainHexatriacontaBacktrackSampleFont.create()
                        .chainAdjustment(
                                new int[] {
                                    GposChainHexatriacontaBacktrackSampleFont.GLYPH_EQUAL,
                                    GposChainHexatriacontaBacktrackSampleFont.GLYPH_LESS,
                                    GposChainHexatriacontaBacktrackSampleFont.GLYPH_SEMICOLON,
                                    GposChainHexatriacontaBacktrackSampleFont.GLYPH_COLON,
                                    GposChainHexatriacontaBacktrackSampleFont.GLYPH_NINE,
                                    GposChainHexatriacontaBacktrackSampleFont.GLYPH_EIGHT,
                                    GposChainHexatriacontaBacktrackSampleFont.GLYPH_SEVEN,
                                    GposChainHexatriacontaBacktrackSampleFont.GLYPH_SIX,
                                    GposChainHexatriacontaBacktrackSampleFont.GLYPH_FIVE,
                                    GposChainHexatriacontaBacktrackSampleFont.GLYPH_FOUR,
                                    GposChainHexatriacontaBacktrackSampleFont.GLYPH_THREE,
                                    GposChainHexatriacontaBacktrackSampleFont.GLYPH_TWO,
                                    GposChainHexatriacontaBacktrackSampleFont.GLYPH_ONE,
                                    GposChainHexatriacontaBacktrackSampleFont.GLYPH_Z,
                                    GposChainHexatriacontaBacktrackSampleFont.GLYPH_Y,
                                    GposChainHexatriacontaBacktrackSampleFont.GLYPH_X,
                                    GposChainHexatriacontaBacktrackSampleFont.GLYPH_W,
                                    GposChainHexatriacontaBacktrackSampleFont.GLYPH_V,
                                    GposChainHexatriacontaBacktrackSampleFont.GLYPH_U,
                                    GposChainHexatriacontaBacktrackSampleFont.GLYPH_T,
                                    GposChainHexatriacontaBacktrackSampleFont.GLYPH_S,
                                    GposChainHexatriacontaBacktrackSampleFont.GLYPH_R,
                                    GposChainHexatriacontaBacktrackSampleFont.GLYPH_Q,
                                    GposChainHexatriacontaBacktrackSampleFont.GLYPH_P,
                                    GposChainHexatriacontaBacktrackSampleFont.GLYPH_O,
                                    GposChainHexatriacontaBacktrackSampleFont.GLYPH_N,
                                    GposChainHexatriacontaBacktrackSampleFont.GLYPH_M,
                                    GposChainHexatriacontaBacktrackSampleFont.GLYPH_L,
                                    GposChainHexatriacontaBacktrackSampleFont.GLYPH_K,
                                    GposChainHexatriacontaBacktrackSampleFont.GLYPH_J,
                                    GposChainHexatriacontaBacktrackSampleFont.GLYPH_I,
                                    GposChainHexatriacontaBacktrackSampleFont.GLYPH_H,
                                    GposChainHexatriacontaBacktrackSampleFont.GLYPH_G,
                                    GposChainHexatriacontaBacktrackSampleFont.GLYPH_F,
                                    GposChainHexatriacontaBacktrackSampleFont.GLYPH_E,
                                    GposChainHexatriacontaBacktrackSampleFont.GLYPH_D,
                                    GposChainHexatriacontaBacktrackSampleFont.GLYPH_A,
                                    GposChainHexatriacontaBacktrackSampleFont.GLYPH_B,
                                    GposChainHexatriacontaBacktrackSampleFont.GLYPH_C,
                                },
                                36,
                                3
                        )
                != GposChainHexatriacontaBacktrackSampleFont.CHAIN_DELTA) {
            throw new IllegalStateException("GPOS thirty-six-glyph chain backtrack leftover did not require =<;:987654321ZYXWVUTSRQPONMLKJIHGFED");
        }
        if (GsubChainHexatriacontaBacktrackSampleFont.create()
                        .chainSubstitute(
                                new int[] {
                                    GsubChainHexatriacontaBacktrackSampleFont.GLYPH_EQUAL,
                                    GsubChainHexatriacontaBacktrackSampleFont.GLYPH_LESS,
                                    GsubChainHexatriacontaBacktrackSampleFont.GLYPH_SEMICOLON,
                                    GsubChainHexatriacontaBacktrackSampleFont.GLYPH_COLON,
                                    GsubChainHexatriacontaBacktrackSampleFont.GLYPH_NINE,
                                    GsubChainHexatriacontaBacktrackSampleFont.GLYPH_EIGHT,
                                    GsubChainHexatriacontaBacktrackSampleFont.GLYPH_SEVEN,
                                    GsubChainHexatriacontaBacktrackSampleFont.GLYPH_SIX,
                                    GsubChainHexatriacontaBacktrackSampleFont.GLYPH_FIVE,
                                    GsubChainHexatriacontaBacktrackSampleFont.GLYPH_FOUR,
                                    GsubChainHexatriacontaBacktrackSampleFont.GLYPH_THREE,
                                    GsubChainHexatriacontaBacktrackSampleFont.GLYPH_TWO,
                                    GsubChainHexatriacontaBacktrackSampleFont.GLYPH_ONE,
                                    GsubChainHexatriacontaBacktrackSampleFont.GLYPH_Z,
                                    GsubChainHexatriacontaBacktrackSampleFont.GLYPH_Y,
                                    GsubChainHexatriacontaBacktrackSampleFont.GLYPH_X,
                                    GsubChainHexatriacontaBacktrackSampleFont.GLYPH_W,
                                    GsubChainHexatriacontaBacktrackSampleFont.GLYPH_V,
                                    GsubChainHexatriacontaBacktrackSampleFont.GLYPH_U,
                                    GsubChainHexatriacontaBacktrackSampleFont.GLYPH_T,
                                    GsubChainHexatriacontaBacktrackSampleFont.GLYPH_S,
                                    GsubChainHexatriacontaBacktrackSampleFont.GLYPH_R,
                                    GsubChainHexatriacontaBacktrackSampleFont.GLYPH_Q,
                                    GsubChainHexatriacontaBacktrackSampleFont.GLYPH_P,
                                    GsubChainHexatriacontaBacktrackSampleFont.GLYPH_O,
                                    GsubChainHexatriacontaBacktrackSampleFont.GLYPH_N,
                                    GsubChainHexatriacontaBacktrackSampleFont.GLYPH_M,
                                    GsubChainHexatriacontaBacktrackSampleFont.GLYPH_L,
                                    GsubChainHexatriacontaBacktrackSampleFont.GLYPH_K,
                                    GsubChainHexatriacontaBacktrackSampleFont.GLYPH_J,
                                    GsubChainHexatriacontaBacktrackSampleFont.GLYPH_I,
                                    GsubChainHexatriacontaBacktrackSampleFont.GLYPH_H,
                                    GsubChainHexatriacontaBacktrackSampleFont.GLYPH_G,
                                    GsubChainHexatriacontaBacktrackSampleFont.GLYPH_F,
                                    GsubChainHexatriacontaBacktrackSampleFont.GLYPH_E,
                                    GsubChainHexatriacontaBacktrackSampleFont.GLYPH_D,
                                    GsubChainHexatriacontaBacktrackSampleFont.GLYPH_A,
                                    GsubChainHexatriacontaBacktrackSampleFont.GLYPH_B,
                                    GsubChainHexatriacontaBacktrackSampleFont.GLYPH_C,
                                },
                                36,
                                3,
                                SfntFont.TAG_CALT
                        )
                != GsubChainHexatriacontaBacktrackSampleFont.GLYPH_ZERO) {
            throw new IllegalStateException("GSUB thirty-six-glyph chain backtrack leftover did not require =<;:987654321ZYXWVUTSRQPONMLKJIHGFED");
        }
        if (GposChainHeptatriacontaBacktrackSampleFont.create()
                        .chainAdjustment(
                                new int[] {
                                    GposChainHeptatriacontaBacktrackSampleFont.GLYPH_GREATER,
                                    GposChainHeptatriacontaBacktrackSampleFont.GLYPH_EQUAL,
                                    GposChainHeptatriacontaBacktrackSampleFont.GLYPH_LESS,
                                    GposChainHeptatriacontaBacktrackSampleFont.GLYPH_SEMICOLON,
                                    GposChainHeptatriacontaBacktrackSampleFont.GLYPH_COLON,
                                    GposChainHeptatriacontaBacktrackSampleFont.GLYPH_NINE,
                                    GposChainHeptatriacontaBacktrackSampleFont.GLYPH_EIGHT,
                                    GposChainHeptatriacontaBacktrackSampleFont.GLYPH_SEVEN,
                                    GposChainHeptatriacontaBacktrackSampleFont.GLYPH_SIX,
                                    GposChainHeptatriacontaBacktrackSampleFont.GLYPH_FIVE,
                                    GposChainHeptatriacontaBacktrackSampleFont.GLYPH_FOUR,
                                    GposChainHeptatriacontaBacktrackSampleFont.GLYPH_THREE,
                                    GposChainHeptatriacontaBacktrackSampleFont.GLYPH_TWO,
                                    GposChainHeptatriacontaBacktrackSampleFont.GLYPH_ONE,
                                    GposChainHeptatriacontaBacktrackSampleFont.GLYPH_Z,
                                    GposChainHeptatriacontaBacktrackSampleFont.GLYPH_Y,
                                    GposChainHeptatriacontaBacktrackSampleFont.GLYPH_X,
                                    GposChainHeptatriacontaBacktrackSampleFont.GLYPH_W,
                                    GposChainHeptatriacontaBacktrackSampleFont.GLYPH_V,
                                    GposChainHeptatriacontaBacktrackSampleFont.GLYPH_U,
                                    GposChainHeptatriacontaBacktrackSampleFont.GLYPH_T,
                                    GposChainHeptatriacontaBacktrackSampleFont.GLYPH_S,
                                    GposChainHeptatriacontaBacktrackSampleFont.GLYPH_R,
                                    GposChainHeptatriacontaBacktrackSampleFont.GLYPH_Q,
                                    GposChainHeptatriacontaBacktrackSampleFont.GLYPH_P,
                                    GposChainHeptatriacontaBacktrackSampleFont.GLYPH_O,
                                    GposChainHeptatriacontaBacktrackSampleFont.GLYPH_N,
                                    GposChainHeptatriacontaBacktrackSampleFont.GLYPH_M,
                                    GposChainHeptatriacontaBacktrackSampleFont.GLYPH_L,
                                    GposChainHeptatriacontaBacktrackSampleFont.GLYPH_K,
                                    GposChainHeptatriacontaBacktrackSampleFont.GLYPH_J,
                                    GposChainHeptatriacontaBacktrackSampleFont.GLYPH_I,
                                    GposChainHeptatriacontaBacktrackSampleFont.GLYPH_H,
                                    GposChainHeptatriacontaBacktrackSampleFont.GLYPH_G,
                                    GposChainHeptatriacontaBacktrackSampleFont.GLYPH_F,
                                    GposChainHeptatriacontaBacktrackSampleFont.GLYPH_E,
                                    GposChainHeptatriacontaBacktrackSampleFont.GLYPH_D,
                                    GposChainHeptatriacontaBacktrackSampleFont.GLYPH_A,
                                    GposChainHeptatriacontaBacktrackSampleFont.GLYPH_B,
                                    GposChainHeptatriacontaBacktrackSampleFont.GLYPH_C,
                                },
                                37,
                                3
                        )
                != GposChainHeptatriacontaBacktrackSampleFont.CHAIN_DELTA) {
            throw new IllegalStateException("GPOS thirty-seven-glyph chain backtrack leftover did not require >=<;:987654321ZYXWVUTSRQPONMLKJIHGFED");
        }
        if (GsubChainHeptatriacontaBacktrackSampleFont.create()
                        .chainSubstitute(
                                new int[] {
                                    GsubChainHeptatriacontaBacktrackSampleFont.GLYPH_GREATER,
                                    GsubChainHeptatriacontaBacktrackSampleFont.GLYPH_EQUAL,
                                    GsubChainHeptatriacontaBacktrackSampleFont.GLYPH_LESS,
                                    GsubChainHeptatriacontaBacktrackSampleFont.GLYPH_SEMICOLON,
                                    GsubChainHeptatriacontaBacktrackSampleFont.GLYPH_COLON,
                                    GsubChainHeptatriacontaBacktrackSampleFont.GLYPH_NINE,
                                    GsubChainHeptatriacontaBacktrackSampleFont.GLYPH_EIGHT,
                                    GsubChainHeptatriacontaBacktrackSampleFont.GLYPH_SEVEN,
                                    GsubChainHeptatriacontaBacktrackSampleFont.GLYPH_SIX,
                                    GsubChainHeptatriacontaBacktrackSampleFont.GLYPH_FIVE,
                                    GsubChainHeptatriacontaBacktrackSampleFont.GLYPH_FOUR,
                                    GsubChainHeptatriacontaBacktrackSampleFont.GLYPH_THREE,
                                    GsubChainHeptatriacontaBacktrackSampleFont.GLYPH_TWO,
                                    GsubChainHeptatriacontaBacktrackSampleFont.GLYPH_ONE,
                                    GsubChainHeptatriacontaBacktrackSampleFont.GLYPH_Z,
                                    GsubChainHeptatriacontaBacktrackSampleFont.GLYPH_Y,
                                    GsubChainHeptatriacontaBacktrackSampleFont.GLYPH_X,
                                    GsubChainHeptatriacontaBacktrackSampleFont.GLYPH_W,
                                    GsubChainHeptatriacontaBacktrackSampleFont.GLYPH_V,
                                    GsubChainHeptatriacontaBacktrackSampleFont.GLYPH_U,
                                    GsubChainHeptatriacontaBacktrackSampleFont.GLYPH_T,
                                    GsubChainHeptatriacontaBacktrackSampleFont.GLYPH_S,
                                    GsubChainHeptatriacontaBacktrackSampleFont.GLYPH_R,
                                    GsubChainHeptatriacontaBacktrackSampleFont.GLYPH_Q,
                                    GsubChainHeptatriacontaBacktrackSampleFont.GLYPH_P,
                                    GsubChainHeptatriacontaBacktrackSampleFont.GLYPH_O,
                                    GsubChainHeptatriacontaBacktrackSampleFont.GLYPH_N,
                                    GsubChainHeptatriacontaBacktrackSampleFont.GLYPH_M,
                                    GsubChainHeptatriacontaBacktrackSampleFont.GLYPH_L,
                                    GsubChainHeptatriacontaBacktrackSampleFont.GLYPH_K,
                                    GsubChainHeptatriacontaBacktrackSampleFont.GLYPH_J,
                                    GsubChainHeptatriacontaBacktrackSampleFont.GLYPH_I,
                                    GsubChainHeptatriacontaBacktrackSampleFont.GLYPH_H,
                                    GsubChainHeptatriacontaBacktrackSampleFont.GLYPH_G,
                                    GsubChainHeptatriacontaBacktrackSampleFont.GLYPH_F,
                                    GsubChainHeptatriacontaBacktrackSampleFont.GLYPH_E,
                                    GsubChainHeptatriacontaBacktrackSampleFont.GLYPH_D,
                                    GsubChainHeptatriacontaBacktrackSampleFont.GLYPH_A,
                                    GsubChainHeptatriacontaBacktrackSampleFont.GLYPH_B,
                                    GsubChainHeptatriacontaBacktrackSampleFont.GLYPH_C,
                                },
                                37,
                                3,
                                SfntFont.TAG_CALT
                        )
                != GsubChainHeptatriacontaBacktrackSampleFont.GLYPH_ZERO) {
            throw new IllegalStateException("GSUB thirty-seven-glyph chain backtrack leftover did not require >=<;:987654321ZYXWVUTSRQPONMLKJIHGFED");
        }
        if (GposChainOctatriacontaBacktrackSampleFont.create()
                        .chainAdjustment(
                                new int[] {
                                    GposChainOctatriacontaBacktrackSampleFont.GLYPH_QUESTION,
                                    GposChainOctatriacontaBacktrackSampleFont.GLYPH_GREATER,
                                    GposChainOctatriacontaBacktrackSampleFont.GLYPH_EQUAL,
                                    GposChainOctatriacontaBacktrackSampleFont.GLYPH_LESS,
                                    GposChainOctatriacontaBacktrackSampleFont.GLYPH_SEMICOLON,
                                    GposChainOctatriacontaBacktrackSampleFont.GLYPH_COLON,
                                    GposChainOctatriacontaBacktrackSampleFont.GLYPH_NINE,
                                    GposChainOctatriacontaBacktrackSampleFont.GLYPH_EIGHT,
                                    GposChainOctatriacontaBacktrackSampleFont.GLYPH_SEVEN,
                                    GposChainOctatriacontaBacktrackSampleFont.GLYPH_SIX,
                                    GposChainOctatriacontaBacktrackSampleFont.GLYPH_FIVE,
                                    GposChainOctatriacontaBacktrackSampleFont.GLYPH_FOUR,
                                    GposChainOctatriacontaBacktrackSampleFont.GLYPH_THREE,
                                    GposChainOctatriacontaBacktrackSampleFont.GLYPH_TWO,
                                    GposChainOctatriacontaBacktrackSampleFont.GLYPH_ONE,
                                    GposChainOctatriacontaBacktrackSampleFont.GLYPH_Z,
                                    GposChainOctatriacontaBacktrackSampleFont.GLYPH_Y,
                                    GposChainOctatriacontaBacktrackSampleFont.GLYPH_X,
                                    GposChainOctatriacontaBacktrackSampleFont.GLYPH_W,
                                    GposChainOctatriacontaBacktrackSampleFont.GLYPH_V,
                                    GposChainOctatriacontaBacktrackSampleFont.GLYPH_U,
                                    GposChainOctatriacontaBacktrackSampleFont.GLYPH_T,
                                    GposChainOctatriacontaBacktrackSampleFont.GLYPH_S,
                                    GposChainOctatriacontaBacktrackSampleFont.GLYPH_R,
                                    GposChainOctatriacontaBacktrackSampleFont.GLYPH_Q,
                                    GposChainOctatriacontaBacktrackSampleFont.GLYPH_P,
                                    GposChainOctatriacontaBacktrackSampleFont.GLYPH_O,
                                    GposChainOctatriacontaBacktrackSampleFont.GLYPH_N,
                                    GposChainOctatriacontaBacktrackSampleFont.GLYPH_M,
                                    GposChainOctatriacontaBacktrackSampleFont.GLYPH_L,
                                    GposChainOctatriacontaBacktrackSampleFont.GLYPH_K,
                                    GposChainOctatriacontaBacktrackSampleFont.GLYPH_J,
                                    GposChainOctatriacontaBacktrackSampleFont.GLYPH_I,
                                    GposChainOctatriacontaBacktrackSampleFont.GLYPH_H,
                                    GposChainOctatriacontaBacktrackSampleFont.GLYPH_G,
                                    GposChainOctatriacontaBacktrackSampleFont.GLYPH_F,
                                    GposChainOctatriacontaBacktrackSampleFont.GLYPH_E,
                                    GposChainOctatriacontaBacktrackSampleFont.GLYPH_D,
                                    GposChainOctatriacontaBacktrackSampleFont.GLYPH_A,
                                    GposChainOctatriacontaBacktrackSampleFont.GLYPH_B,
                                    GposChainOctatriacontaBacktrackSampleFont.GLYPH_C,
                                },
                                38,
                                3
                        )
                != GposChainOctatriacontaBacktrackSampleFont.CHAIN_DELTA) {
            throw new IllegalStateException("GPOS thirty-eight-glyph chain backtrack leftover did not require ?>=<;:987654321ZYXWVUTSRQPONMLKJIHGFED");
        }
        if (GsubChainOctatriacontaBacktrackSampleFont.create()
                        .chainSubstitute(
                                new int[] {
                                    GsubChainOctatriacontaBacktrackSampleFont.GLYPH_QUESTION,
                                    GsubChainOctatriacontaBacktrackSampleFont.GLYPH_GREATER,
                                    GsubChainOctatriacontaBacktrackSampleFont.GLYPH_EQUAL,
                                    GsubChainOctatriacontaBacktrackSampleFont.GLYPH_LESS,
                                    GsubChainOctatriacontaBacktrackSampleFont.GLYPH_SEMICOLON,
                                    GsubChainOctatriacontaBacktrackSampleFont.GLYPH_COLON,
                                    GsubChainOctatriacontaBacktrackSampleFont.GLYPH_NINE,
                                    GsubChainOctatriacontaBacktrackSampleFont.GLYPH_EIGHT,
                                    GsubChainOctatriacontaBacktrackSampleFont.GLYPH_SEVEN,
                                    GsubChainOctatriacontaBacktrackSampleFont.GLYPH_SIX,
                                    GsubChainOctatriacontaBacktrackSampleFont.GLYPH_FIVE,
                                    GsubChainOctatriacontaBacktrackSampleFont.GLYPH_FOUR,
                                    GsubChainOctatriacontaBacktrackSampleFont.GLYPH_THREE,
                                    GsubChainOctatriacontaBacktrackSampleFont.GLYPH_TWO,
                                    GsubChainOctatriacontaBacktrackSampleFont.GLYPH_ONE,
                                    GsubChainOctatriacontaBacktrackSampleFont.GLYPH_Z,
                                    GsubChainOctatriacontaBacktrackSampleFont.GLYPH_Y,
                                    GsubChainOctatriacontaBacktrackSampleFont.GLYPH_X,
                                    GsubChainOctatriacontaBacktrackSampleFont.GLYPH_W,
                                    GsubChainOctatriacontaBacktrackSampleFont.GLYPH_V,
                                    GsubChainOctatriacontaBacktrackSampleFont.GLYPH_U,
                                    GsubChainOctatriacontaBacktrackSampleFont.GLYPH_T,
                                    GsubChainOctatriacontaBacktrackSampleFont.GLYPH_S,
                                    GsubChainOctatriacontaBacktrackSampleFont.GLYPH_R,
                                    GsubChainOctatriacontaBacktrackSampleFont.GLYPH_Q,
                                    GsubChainOctatriacontaBacktrackSampleFont.GLYPH_P,
                                    GsubChainOctatriacontaBacktrackSampleFont.GLYPH_O,
                                    GsubChainOctatriacontaBacktrackSampleFont.GLYPH_N,
                                    GsubChainOctatriacontaBacktrackSampleFont.GLYPH_M,
                                    GsubChainOctatriacontaBacktrackSampleFont.GLYPH_L,
                                    GsubChainOctatriacontaBacktrackSampleFont.GLYPH_K,
                                    GsubChainOctatriacontaBacktrackSampleFont.GLYPH_J,
                                    GsubChainOctatriacontaBacktrackSampleFont.GLYPH_I,
                                    GsubChainOctatriacontaBacktrackSampleFont.GLYPH_H,
                                    GsubChainOctatriacontaBacktrackSampleFont.GLYPH_G,
                                    GsubChainOctatriacontaBacktrackSampleFont.GLYPH_F,
                                    GsubChainOctatriacontaBacktrackSampleFont.GLYPH_E,
                                    GsubChainOctatriacontaBacktrackSampleFont.GLYPH_D,
                                    GsubChainOctatriacontaBacktrackSampleFont.GLYPH_A,
                                    GsubChainOctatriacontaBacktrackSampleFont.GLYPH_B,
                                    GsubChainOctatriacontaBacktrackSampleFont.GLYPH_C,
                                },
                                38,
                                3,
                                SfntFont.TAG_CALT
                        )
                != GsubChainOctatriacontaBacktrackSampleFont.GLYPH_ZERO) {
            throw new IllegalStateException("GSUB thirty-eight-glyph chain backtrack leftover did not require ?>=<;:987654321ZYXWVUTSRQPONMLKJIHGFED");
        }
        if (GposChainEnneatriacontaBacktrackSampleFont.create()
                        .chainAdjustment(
                                new int[] {
                                    GposChainEnneatriacontaBacktrackSampleFont.GLYPH_AT,
                                    GposChainEnneatriacontaBacktrackSampleFont.GLYPH_QUESTION,
                                    GposChainEnneatriacontaBacktrackSampleFont.GLYPH_GREATER,
                                    GposChainEnneatriacontaBacktrackSampleFont.GLYPH_EQUAL,
                                    GposChainEnneatriacontaBacktrackSampleFont.GLYPH_LESS,
                                    GposChainEnneatriacontaBacktrackSampleFont.GLYPH_SEMICOLON,
                                    GposChainEnneatriacontaBacktrackSampleFont.GLYPH_COLON,
                                    GposChainEnneatriacontaBacktrackSampleFont.GLYPH_NINE,
                                    GposChainEnneatriacontaBacktrackSampleFont.GLYPH_EIGHT,
                                    GposChainEnneatriacontaBacktrackSampleFont.GLYPH_SEVEN,
                                    GposChainEnneatriacontaBacktrackSampleFont.GLYPH_SIX,
                                    GposChainEnneatriacontaBacktrackSampleFont.GLYPH_FIVE,
                                    GposChainEnneatriacontaBacktrackSampleFont.GLYPH_FOUR,
                                    GposChainEnneatriacontaBacktrackSampleFont.GLYPH_THREE,
                                    GposChainEnneatriacontaBacktrackSampleFont.GLYPH_TWO,
                                    GposChainEnneatriacontaBacktrackSampleFont.GLYPH_ONE,
                                    GposChainEnneatriacontaBacktrackSampleFont.GLYPH_Z,
                                    GposChainEnneatriacontaBacktrackSampleFont.GLYPH_Y,
                                    GposChainEnneatriacontaBacktrackSampleFont.GLYPH_X,
                                    GposChainEnneatriacontaBacktrackSampleFont.GLYPH_W,
                                    GposChainEnneatriacontaBacktrackSampleFont.GLYPH_V,
                                    GposChainEnneatriacontaBacktrackSampleFont.GLYPH_U,
                                    GposChainEnneatriacontaBacktrackSampleFont.GLYPH_T,
                                    GposChainEnneatriacontaBacktrackSampleFont.GLYPH_S,
                                    GposChainEnneatriacontaBacktrackSampleFont.GLYPH_R,
                                    GposChainEnneatriacontaBacktrackSampleFont.GLYPH_Q,
                                    GposChainEnneatriacontaBacktrackSampleFont.GLYPH_P,
                                    GposChainEnneatriacontaBacktrackSampleFont.GLYPH_O,
                                    GposChainEnneatriacontaBacktrackSampleFont.GLYPH_N,
                                    GposChainEnneatriacontaBacktrackSampleFont.GLYPH_M,
                                    GposChainEnneatriacontaBacktrackSampleFont.GLYPH_L,
                                    GposChainEnneatriacontaBacktrackSampleFont.GLYPH_K,
                                    GposChainEnneatriacontaBacktrackSampleFont.GLYPH_J,
                                    GposChainEnneatriacontaBacktrackSampleFont.GLYPH_I,
                                    GposChainEnneatriacontaBacktrackSampleFont.GLYPH_H,
                                    GposChainEnneatriacontaBacktrackSampleFont.GLYPH_G,
                                    GposChainEnneatriacontaBacktrackSampleFont.GLYPH_F,
                                    GposChainEnneatriacontaBacktrackSampleFont.GLYPH_E,
                                    GposChainEnneatriacontaBacktrackSampleFont.GLYPH_D,
                                    GposChainEnneatriacontaBacktrackSampleFont.GLYPH_A,
                                    GposChainEnneatriacontaBacktrackSampleFont.GLYPH_B,
                                    GposChainEnneatriacontaBacktrackSampleFont.GLYPH_C,
                                },
                                39,
                                3
                        )
                != GposChainEnneatriacontaBacktrackSampleFont.CHAIN_DELTA) {
            throw new IllegalStateException("GPOS thirty-nine-glyph chain backtrack leftover did not require @?>=<;:987654321ZYXWVUTSRQPONMLKJIHGFED");
        }
        if (GsubChainEnneatriacontaBacktrackSampleFont.create()
                        .chainSubstitute(
                                new int[] {
                                    GsubChainEnneatriacontaBacktrackSampleFont.GLYPH_AT,
                                    GsubChainEnneatriacontaBacktrackSampleFont.GLYPH_QUESTION,
                                    GsubChainEnneatriacontaBacktrackSampleFont.GLYPH_GREATER,
                                    GsubChainEnneatriacontaBacktrackSampleFont.GLYPH_EQUAL,
                                    GsubChainEnneatriacontaBacktrackSampleFont.GLYPH_LESS,
                                    GsubChainEnneatriacontaBacktrackSampleFont.GLYPH_SEMICOLON,
                                    GsubChainEnneatriacontaBacktrackSampleFont.GLYPH_COLON,
                                    GsubChainEnneatriacontaBacktrackSampleFont.GLYPH_NINE,
                                    GsubChainEnneatriacontaBacktrackSampleFont.GLYPH_EIGHT,
                                    GsubChainEnneatriacontaBacktrackSampleFont.GLYPH_SEVEN,
                                    GsubChainEnneatriacontaBacktrackSampleFont.GLYPH_SIX,
                                    GsubChainEnneatriacontaBacktrackSampleFont.GLYPH_FIVE,
                                    GsubChainEnneatriacontaBacktrackSampleFont.GLYPH_FOUR,
                                    GsubChainEnneatriacontaBacktrackSampleFont.GLYPH_THREE,
                                    GsubChainEnneatriacontaBacktrackSampleFont.GLYPH_TWO,
                                    GsubChainEnneatriacontaBacktrackSampleFont.GLYPH_ONE,
                                    GsubChainEnneatriacontaBacktrackSampleFont.GLYPH_Z,
                                    GsubChainEnneatriacontaBacktrackSampleFont.GLYPH_Y,
                                    GsubChainEnneatriacontaBacktrackSampleFont.GLYPH_X,
                                    GsubChainEnneatriacontaBacktrackSampleFont.GLYPH_W,
                                    GsubChainEnneatriacontaBacktrackSampleFont.GLYPH_V,
                                    GsubChainEnneatriacontaBacktrackSampleFont.GLYPH_U,
                                    GsubChainEnneatriacontaBacktrackSampleFont.GLYPH_T,
                                    GsubChainEnneatriacontaBacktrackSampleFont.GLYPH_S,
                                    GsubChainEnneatriacontaBacktrackSampleFont.GLYPH_R,
                                    GsubChainEnneatriacontaBacktrackSampleFont.GLYPH_Q,
                                    GsubChainEnneatriacontaBacktrackSampleFont.GLYPH_P,
                                    GsubChainEnneatriacontaBacktrackSampleFont.GLYPH_O,
                                    GsubChainEnneatriacontaBacktrackSampleFont.GLYPH_N,
                                    GsubChainEnneatriacontaBacktrackSampleFont.GLYPH_M,
                                    GsubChainEnneatriacontaBacktrackSampleFont.GLYPH_L,
                                    GsubChainEnneatriacontaBacktrackSampleFont.GLYPH_K,
                                    GsubChainEnneatriacontaBacktrackSampleFont.GLYPH_J,
                                    GsubChainEnneatriacontaBacktrackSampleFont.GLYPH_I,
                                    GsubChainEnneatriacontaBacktrackSampleFont.GLYPH_H,
                                    GsubChainEnneatriacontaBacktrackSampleFont.GLYPH_G,
                                    GsubChainEnneatriacontaBacktrackSampleFont.GLYPH_F,
                                    GsubChainEnneatriacontaBacktrackSampleFont.GLYPH_E,
                                    GsubChainEnneatriacontaBacktrackSampleFont.GLYPH_D,
                                    GsubChainEnneatriacontaBacktrackSampleFont.GLYPH_A,
                                    GsubChainEnneatriacontaBacktrackSampleFont.GLYPH_B,
                                    GsubChainEnneatriacontaBacktrackSampleFont.GLYPH_C,
                                },
                                39,
                                3,
                                SfntFont.TAG_CALT
                        )
                != GsubChainEnneatriacontaBacktrackSampleFont.GLYPH_ZERO) {
            throw new IllegalStateException("GSUB thirty-nine-glyph chain backtrack leftover did not require @?>=<;:987654321ZYXWVUTSRQPONMLKJIHGFED");
        }
        if (GposChainTetracontaBacktrackSampleFont.create()
                        .chainAdjustment(
                                new int[] {
                                    GposChainTetracontaBacktrackSampleFont.GLYPH_BRACKET,
                                    GposChainTetracontaBacktrackSampleFont.GLYPH_AT,
                                    GposChainTetracontaBacktrackSampleFont.GLYPH_QUESTION,
                                    GposChainTetracontaBacktrackSampleFont.GLYPH_GREATER,
                                    GposChainTetracontaBacktrackSampleFont.GLYPH_EQUAL,
                                    GposChainTetracontaBacktrackSampleFont.GLYPH_LESS,
                                    GposChainTetracontaBacktrackSampleFont.GLYPH_SEMICOLON,
                                    GposChainTetracontaBacktrackSampleFont.GLYPH_COLON,
                                    GposChainTetracontaBacktrackSampleFont.GLYPH_NINE,
                                    GposChainTetracontaBacktrackSampleFont.GLYPH_EIGHT,
                                    GposChainTetracontaBacktrackSampleFont.GLYPH_SEVEN,
                                    GposChainTetracontaBacktrackSampleFont.GLYPH_SIX,
                                    GposChainTetracontaBacktrackSampleFont.GLYPH_FIVE,
                                    GposChainTetracontaBacktrackSampleFont.GLYPH_FOUR,
                                    GposChainTetracontaBacktrackSampleFont.GLYPH_THREE,
                                    GposChainTetracontaBacktrackSampleFont.GLYPH_TWO,
                                    GposChainTetracontaBacktrackSampleFont.GLYPH_ONE,
                                    GposChainTetracontaBacktrackSampleFont.GLYPH_Z,
                                    GposChainTetracontaBacktrackSampleFont.GLYPH_Y,
                                    GposChainTetracontaBacktrackSampleFont.GLYPH_X,
                                    GposChainTetracontaBacktrackSampleFont.GLYPH_W,
                                    GposChainTetracontaBacktrackSampleFont.GLYPH_V,
                                    GposChainTetracontaBacktrackSampleFont.GLYPH_U,
                                    GposChainTetracontaBacktrackSampleFont.GLYPH_T,
                                    GposChainTetracontaBacktrackSampleFont.GLYPH_S,
                                    GposChainTetracontaBacktrackSampleFont.GLYPH_R,
                                    GposChainTetracontaBacktrackSampleFont.GLYPH_Q,
                                    GposChainTetracontaBacktrackSampleFont.GLYPH_P,
                                    GposChainTetracontaBacktrackSampleFont.GLYPH_O,
                                    GposChainTetracontaBacktrackSampleFont.GLYPH_N,
                                    GposChainTetracontaBacktrackSampleFont.GLYPH_M,
                                    GposChainTetracontaBacktrackSampleFont.GLYPH_L,
                                    GposChainTetracontaBacktrackSampleFont.GLYPH_K,
                                    GposChainTetracontaBacktrackSampleFont.GLYPH_J,
                                    GposChainTetracontaBacktrackSampleFont.GLYPH_I,
                                    GposChainTetracontaBacktrackSampleFont.GLYPH_H,
                                    GposChainTetracontaBacktrackSampleFont.GLYPH_G,
                                    GposChainTetracontaBacktrackSampleFont.GLYPH_F,
                                    GposChainTetracontaBacktrackSampleFont.GLYPH_E,
                                    GposChainTetracontaBacktrackSampleFont.GLYPH_D,
                                    GposChainTetracontaBacktrackSampleFont.GLYPH_A,
                                    GposChainTetracontaBacktrackSampleFont.GLYPH_B,
                                    GposChainTetracontaBacktrackSampleFont.GLYPH_C,
                                },
                                40,
                                3
                        )
                != GposChainTetracontaBacktrackSampleFont.CHAIN_DELTA) {
            throw new IllegalStateException("GPOS forty-glyph chain backtrack leftover did not require [@?>=<;:987654321ZYXWVUTSRQPONMLKJIHGFED");
        }
        if (GsubChainTetracontaBacktrackSampleFont.create()
                        .chainSubstitute(
                                new int[] {
                                    GsubChainTetracontaBacktrackSampleFont.GLYPH_BRACKET,
                                    GsubChainTetracontaBacktrackSampleFont.GLYPH_AT,
                                    GsubChainTetracontaBacktrackSampleFont.GLYPH_QUESTION,
                                    GsubChainTetracontaBacktrackSampleFont.GLYPH_GREATER,
                                    GsubChainTetracontaBacktrackSampleFont.GLYPH_EQUAL,
                                    GsubChainTetracontaBacktrackSampleFont.GLYPH_LESS,
                                    GsubChainTetracontaBacktrackSampleFont.GLYPH_SEMICOLON,
                                    GsubChainTetracontaBacktrackSampleFont.GLYPH_COLON,
                                    GsubChainTetracontaBacktrackSampleFont.GLYPH_NINE,
                                    GsubChainTetracontaBacktrackSampleFont.GLYPH_EIGHT,
                                    GsubChainTetracontaBacktrackSampleFont.GLYPH_SEVEN,
                                    GsubChainTetracontaBacktrackSampleFont.GLYPH_SIX,
                                    GsubChainTetracontaBacktrackSampleFont.GLYPH_FIVE,
                                    GsubChainTetracontaBacktrackSampleFont.GLYPH_FOUR,
                                    GsubChainTetracontaBacktrackSampleFont.GLYPH_THREE,
                                    GsubChainTetracontaBacktrackSampleFont.GLYPH_TWO,
                                    GsubChainTetracontaBacktrackSampleFont.GLYPH_ONE,
                                    GsubChainTetracontaBacktrackSampleFont.GLYPH_Z,
                                    GsubChainTetracontaBacktrackSampleFont.GLYPH_Y,
                                    GsubChainTetracontaBacktrackSampleFont.GLYPH_X,
                                    GsubChainTetracontaBacktrackSampleFont.GLYPH_W,
                                    GsubChainTetracontaBacktrackSampleFont.GLYPH_V,
                                    GsubChainTetracontaBacktrackSampleFont.GLYPH_U,
                                    GsubChainTetracontaBacktrackSampleFont.GLYPH_T,
                                    GsubChainTetracontaBacktrackSampleFont.GLYPH_S,
                                    GsubChainTetracontaBacktrackSampleFont.GLYPH_R,
                                    GsubChainTetracontaBacktrackSampleFont.GLYPH_Q,
                                    GsubChainTetracontaBacktrackSampleFont.GLYPH_P,
                                    GsubChainTetracontaBacktrackSampleFont.GLYPH_O,
                                    GsubChainTetracontaBacktrackSampleFont.GLYPH_N,
                                    GsubChainTetracontaBacktrackSampleFont.GLYPH_M,
                                    GsubChainTetracontaBacktrackSampleFont.GLYPH_L,
                                    GsubChainTetracontaBacktrackSampleFont.GLYPH_K,
                                    GsubChainTetracontaBacktrackSampleFont.GLYPH_J,
                                    GsubChainTetracontaBacktrackSampleFont.GLYPH_I,
                                    GsubChainTetracontaBacktrackSampleFont.GLYPH_H,
                                    GsubChainTetracontaBacktrackSampleFont.GLYPH_G,
                                    GsubChainTetracontaBacktrackSampleFont.GLYPH_F,
                                    GsubChainTetracontaBacktrackSampleFont.GLYPH_E,
                                    GsubChainTetracontaBacktrackSampleFont.GLYPH_D,
                                    GsubChainTetracontaBacktrackSampleFont.GLYPH_A,
                                    GsubChainTetracontaBacktrackSampleFont.GLYPH_B,
                                    GsubChainTetracontaBacktrackSampleFont.GLYPH_C,
                                },
                                40,
                                3,
                                SfntFont.TAG_CALT
                        )
                != GsubChainTetracontaBacktrackSampleFont.GLYPH_ZERO) {
            throw new IllegalStateException("GSUB forty-glyph chain backtrack leftover did not require [@?>=<;:987654321ZYXWVUTSRQPONMLKJIHGFED");
        }
        SfntFont grid = GaspGridFitSampleFont.create();
        if (!grid.gaspGridFits(GaspGridFitSampleFont.GRID_PPEM)
                || GlyphRasterizer.rasterize(grid, GaspGridFitSampleFont.GLYPH_A, GaspGridFitSampleFont.GRID_PPEM)
                        .height()
                        != 10) {
            throw new IllegalStateException("gasp GRIDFIT leftover did not snap the outline box");
        }
        return new LeftoverEvidence(
                layers,
                axes,
                moved,
                avar,
                cbdt,
                ebdt,
                sbix,
                colrV1Layers,
                peakAscender,
                true,
                true,
                cursiveDelta,
                skipPair,
                attachPair
        );
    }

    /// Observations from the named font leftovers.
    ///
    /// @param colrLayers COLR layer count
    /// @param variationAxes `fvar` axis count
    /// @param gvarPeakMoved whether the peak instance moved the outline
    /// @param avarRemapped whether `avar` sent the mid instance to the peak
    /// @param cbdtPresent whether CBLC/CBDT returned a strike
    /// @param ebdtPresent whether EBLC/EBDT returned a strike
    /// @param sbixPresent whether `sbix` returned a strike
    /// @param colrV1Layers COLR v1 flattened layer count
    /// @param mvarPeakAscender peak-instance `hasc` ascender
    /// @param woffUnwrapped whether WOFF1 unwrap preserved COLR v1 layers
    /// @param woff2Unwrapped whether WOFF2 unwrap preserved COLR v1 layers
    /// @param cursiveDelta GPOS type-3 `AB` X-advance
    /// @param skipPairDelta GPOS `IgnoreMarks` `AC` X-advance
    /// @param attachPairDelta GPOS `MarkAttachmentType` `AC` X-advance
    private record LeftoverEvidence(
            int colrLayers,
            int variationAxes,
            boolean gvarPeakMoved,
            boolean avarRemapped,
            boolean cbdtPresent,
            boolean ebdtPresent,
            boolean sbixPresent,
            int colrV1Layers,
            int mvarPeakAscender,
            boolean woffUnwrapped,
            boolean woff2Unwrapped,
            int cursiveDelta,
            int skipPairDelta,
            int attachPairDelta
    ) {
        /// Encodes the leftover observation.
        ///
        /// @return JSON
        String toJson() {
            return """
                    {
                      "colrLayers": %d,
                      "variationAxes": %d,
                      "gvarPeakMoved": %s,
                      "avarRemapped": %s,
                      "cbdtPresent": %s,
                      "ebdtPresent": %s,
                      "sbixPresent": %s,
                      "colrV1Layers": %d,
                      "mvarPeakAscender": %d,
                      "woffUnwrapped": %s,
                      "woff2Unwrapped": %s,
                      "cursiveDelta": %d,
                      "skipPairDelta": %d,
                      "attachPairDelta": %d
                    }
                    """.formatted(
                    colrLayers,
                    variationAxes,
                    gvarPeakMoved,
                    avarRemapped,
                    cbdtPresent,
                    ebdtPresent,
                    sbixPresent,
                    colrV1Layers,
                    mvarPeakAscender,
                    woffUnwrapped,
                    woff2Unwrapped,
                    cursiveDelta,
                    skipPairDelta,
                    attachPairDelta
            );
        }
    }
}

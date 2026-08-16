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
                    cursiveDelta,
                    skipPairDelta,
                    attachPairDelta
            );
        }
    }
}

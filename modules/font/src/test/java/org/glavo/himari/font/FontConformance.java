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

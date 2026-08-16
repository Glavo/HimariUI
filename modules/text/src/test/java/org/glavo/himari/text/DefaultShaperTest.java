package org.glavo.himari.text;

import org.glavo.himari.font.BitmapSfntFont;
import org.glavo.himari.font.CffSampleFont;
import org.glavo.himari.font.GposChainSampleFont;
import org.glavo.himari.font.GposContextSampleFont;
import org.glavo.himari.font.GposCursiveSampleFont;
import org.glavo.himari.font.GposMarkAttachSampleFont;
import org.glavo.himari.font.GposMarkMarkSampleFont;
import org.glavo.himari.font.GposMarkSkipSampleFont;
import org.glavo.himari.font.GposSingleSampleFont;
import org.glavo.himari.font.GsubChainSampleFont;
import org.glavo.himari.font.GsubContextMarkSampleFont;
import org.glavo.himari.font.GdefMarkSetSampleFont;
import org.glavo.himari.font.GposChainBacktrackSampleFont;
import org.glavo.himari.font.GposChainDoubleBacktrackSampleFont;
import org.glavo.himari.font.GposChainHeptaBacktrackSampleFont;
import org.glavo.himari.font.GposChainNonaBacktrackSampleFont;
import org.glavo.himari.font.GposChainOctaBacktrackSampleFont;
import org.glavo.himari.font.GposChainHexaBacktrackSampleFont;
import org.glavo.himari.font.GposChainPentaBacktrackSampleFont;
import org.glavo.himari.font.GposChainQuadBacktrackSampleFont;
import org.glavo.himari.font.GposChainTripleBacktrackSampleFont;
import org.glavo.himari.font.GposFlaggedChainBacktrackSampleFont;
import org.glavo.himari.font.GposChainClassSampleFont;
import org.glavo.himari.font.GposContextClassSampleFont;
import org.glavo.himari.font.GposCoverageSampleFont;
import org.glavo.himari.font.GposIgnoreClassSampleFont;
import org.glavo.himari.font.GposPairClassSampleFont;
import org.glavo.himari.font.GsubContextClassSampleFont;
import org.glavo.himari.font.GsubChainBacktrackSampleFont;
import org.glavo.himari.font.GsubChainDoubleBacktrackSampleFont;
import org.glavo.himari.font.GsubChainHeptaBacktrackSampleFont;
import org.glavo.himari.font.GsubChainNonaBacktrackSampleFont;
import org.glavo.himari.font.GsubChainOctaBacktrackSampleFont;
import org.glavo.himari.font.GsubChainHexaBacktrackSampleFont;
import org.glavo.himari.font.GsubChainPentaBacktrackSampleFont;
import org.glavo.himari.font.GsubChainQuadBacktrackSampleFont;
import org.glavo.himari.font.GsubChainTripleBacktrackSampleFont;
import org.glavo.himari.font.GsubChainClassSampleFont;
import org.glavo.himari.font.GsubChainCoverageSampleFont;
import org.glavo.himari.font.GsubCoverageSampleFont;
import org.glavo.himari.font.GsubReverseBacktrackSampleFont;
import org.glavo.himari.font.GsubIgnoreClassSampleFont;
import org.glavo.himari.font.GsubMarkAttachSampleFont;
import org.glavo.himari.font.GsubReverseSkipSampleFont;
import org.glavo.himari.font.GsubContextSampleFont;
import org.glavo.himari.font.GsubLigatureSampleFont;
import org.glavo.himari.font.GsubMultipleSampleFont;
import org.glavo.himari.font.GsubReverseSampleFont;
import org.glavo.himari.font.GdefMarkSampleFont;
import org.glavo.himari.font.GsubSampleFont;
import org.glavo.himari.font.ScriptSampleFont;
import org.glavo.himari.font.SfntFont;
import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies default, Arabic, and Hebrew presentation shaping.
@NotNullByDefault
final class DefaultShaperTest {
    /// Applies a GPOS format-2 class pair through the shipped shaper.
    @Test
    void shapesFormat2ClassPair() {
        SfntFont font = GposPairClassSampleFont.create();
        assertEquals(
                GposPairClassSampleFont.ADVANCE_LETTER + GposPairClassSampleFont.PAIR_DELTA,
                DefaultShaper.shape(font, "AC").get(0).xAdvance()
        );
        assertEquals(GposPairClassSampleFont.ADVANCE_LETTER, DefaultShaper.shape(font, "AB").get(0).xAdvance());
    }

    /// Applies GPOS format-3 context and chain advances through the shipped shaper.
    @Test
    void shapesFormat3ContextAndChain() {
        SfntFont font = GposCoverageSampleFont.create();
        assertEquals(
                GposCoverageSampleFont.ADVANCE_LETTER + GposCoverageSampleFont.CONTEXT_DELTA,
                DefaultShaper.shape(font, "AB").get(0).xAdvance()
        );
        assertEquals(
                GposCoverageSampleFont.ADVANCE_LETTER
                        + GposCoverageSampleFont.CONTEXT_DELTA
                        + GposCoverageSampleFont.CHAIN_DELTA,
                DefaultShaper.shape(font, "ABC").get(0).xAdvance()
        );
    }

    /// Applies GSUB format-2 and format-3 context substitutions through the shipped shaper.
    @Test
    void shapesFormat2And3Context() {
        assertEquals(
                GsubCoverageSampleFont.GLYPH_X,
                DefaultShaper.shape(GsubCoverageSampleFont.create(), "AB").get(0).glyphId()
        );
        assertEquals(
                GsubContextClassSampleFont.GLYPH_X,
                DefaultShaper.shape(GsubContextClassSampleFont.create(), "AB").get(0).glyphId()
        );
        assertEquals(
                GsubChainCoverageSampleFont.GLYPH_Y,
                DefaultShaper.shape(GsubChainCoverageSampleFont.create(), "ABC").get(0).glyphId()
        );
        assertEquals(
                GsubChainClassSampleFont.GLYPH_Y,
                DefaultShaper.shape(GsubChainClassSampleFont.create(), "ABC").get(0).glyphId()
        );
        assertEquals(
                GsubChainBacktrackSampleFont.GLYPH_Y,
                DefaultShaper.shape(GsubChainBacktrackSampleFont.create(), "DABC").get(1).glyphId()
        );
        assertEquals(
                GsubChainBacktrackSampleFont.GLYPH_A,
                DefaultShaper.shape(GsubChainBacktrackSampleFont.create(), "ABC").get(0).glyphId()
        );
        assertEquals(
                GsubChainDoubleBacktrackSampleFont.GLYPH_Y,
                DefaultShaper.shape(GsubChainDoubleBacktrackSampleFont.create(), "EDABC").get(2).glyphId()
        );
        assertEquals(
                GsubChainDoubleBacktrackSampleFont.GLYPH_A,
                DefaultShaper.shape(GsubChainDoubleBacktrackSampleFont.create(), "DABC").get(1).glyphId()
        );
        assertEquals(
                GsubChainTripleBacktrackSampleFont.GLYPH_Y,
                DefaultShaper.shape(GsubChainTripleBacktrackSampleFont.create(), "FEDABC").get(3).glyphId()
        );
        assertEquals(
                GsubChainTripleBacktrackSampleFont.GLYPH_A,
                DefaultShaper.shape(GsubChainTripleBacktrackSampleFont.create(), "EDABC").get(2).glyphId()
        );
        assertEquals(
                GsubChainQuadBacktrackSampleFont.GLYPH_Y,
                DefaultShaper.shape(GsubChainQuadBacktrackSampleFont.create(), "GFEDABC").get(4).glyphId()
        );
        assertEquals(
                GsubChainQuadBacktrackSampleFont.GLYPH_A,
                DefaultShaper.shape(GsubChainQuadBacktrackSampleFont.create(), "FEDABC").get(3).glyphId()
        );
        assertEquals(
                GsubChainPentaBacktrackSampleFont.GLYPH_Y,
                DefaultShaper.shape(GsubChainPentaBacktrackSampleFont.create(), "HGFEDABC").get(5).glyphId()
        );
        assertEquals(
                GsubChainPentaBacktrackSampleFont.GLYPH_A,
                DefaultShaper.shape(GsubChainPentaBacktrackSampleFont.create(), "GFEDABC").get(4).glyphId()
        );
        assertEquals(
                GsubChainHexaBacktrackSampleFont.GLYPH_Y,
                DefaultShaper.shape(GsubChainHexaBacktrackSampleFont.create(), "IHGFEDABC").get(6).glyphId()
        );
        assertEquals(
                GsubChainHexaBacktrackSampleFont.GLYPH_A,
                DefaultShaper.shape(GsubChainHexaBacktrackSampleFont.create(), "HGFEDABC").get(5).glyphId()
        );
        assertEquals(
                GsubChainHeptaBacktrackSampleFont.GLYPH_Y,
                DefaultShaper.shape(GsubChainHeptaBacktrackSampleFont.create(), "JIHGFEDABC").get(7).glyphId()
        );
        assertEquals(
                GsubChainHeptaBacktrackSampleFont.GLYPH_A,
                DefaultShaper.shape(GsubChainHeptaBacktrackSampleFont.create(), "IHGFEDABC").get(6).glyphId()
        );
        assertEquals(
                GsubChainOctaBacktrackSampleFont.GLYPH_Y,
                DefaultShaper.shape(GsubChainOctaBacktrackSampleFont.create(), "KJIHGFEDABC").get(8).glyphId()
        );
        assertEquals(
                GsubChainOctaBacktrackSampleFont.GLYPH_A,
                DefaultShaper.shape(GsubChainOctaBacktrackSampleFont.create(), "JIHGFEDABC").get(7).glyphId()
        );
        assertEquals(
                GsubChainNonaBacktrackSampleFont.GLYPH_Y,
                DefaultShaper.shape(GsubChainNonaBacktrackSampleFont.create(), "LKJIHGFEDABC").get(9).glyphId()
        );
        assertEquals(
                GsubChainNonaBacktrackSampleFont.GLYPH_A,
                DefaultShaper.shape(GsubChainNonaBacktrackSampleFont.create(), "KJIHGFEDABC").get(8).glyphId()
        );
    }

    /// Applies GPOS format-2 class context and chain through the shipped shaper.
    @Test
    void shapesFormat2ClassContextAndChain() {
        SfntFont context = GposContextClassSampleFont.create();
        assertEquals(
                GposContextClassSampleFont.ADVANCE_LETTER + GposContextClassSampleFont.CONTEXT_DELTA,
                DefaultShaper.shape(context, "AB").get(0).xAdvance()
        );
        SfntFont chain = GposChainClassSampleFont.create();
        assertEquals(
                GposChainClassSampleFont.ADVANCE_LETTER + GposChainClassSampleFont.CHAIN_DELTA,
                DefaultShaper.shape(chain, "ABC").get(0).xAdvance()
        );
        assertEquals(GposChainClassSampleFont.ADVANCE_LETTER, DefaultShaper.shape(chain, "AB").get(0).xAdvance());
        SfntFont backtrack = GposChainBacktrackSampleFont.create();
        assertEquals(
                GposChainBacktrackSampleFont.ADVANCE_LETTER + GposChainBacktrackSampleFont.CHAIN_DELTA,
                DefaultShaper.shape(backtrack, "DABC").get(1).xAdvance()
        );
        assertEquals(
                GposChainBacktrackSampleFont.ADVANCE_LETTER,
                DefaultShaper.shape(backtrack, "ABC").get(0).xAdvance()
        );
        SfntFont two = GposChainDoubleBacktrackSampleFont.create();
        assertEquals(
                GposChainDoubleBacktrackSampleFont.ADVANCE_LETTER
                        + GposChainDoubleBacktrackSampleFont.CHAIN_DELTA,
                DefaultShaper.shape(two, "EDABC").get(2).xAdvance()
        );
        assertEquals(
                GposChainDoubleBacktrackSampleFont.ADVANCE_LETTER,
                DefaultShaper.shape(two, "DABC").get(1).xAdvance()
        );
        SfntFont three = GposChainTripleBacktrackSampleFont.create();
        assertEquals(
                GposChainTripleBacktrackSampleFont.ADVANCE_LETTER
                        + GposChainTripleBacktrackSampleFont.CHAIN_DELTA,
                DefaultShaper.shape(three, "FEDABC").get(3).xAdvance()
        );
        assertEquals(
                GposChainTripleBacktrackSampleFont.ADVANCE_LETTER,
                DefaultShaper.shape(three, "EDABC").get(2).xAdvance()
        );
        SfntFont four = GposChainQuadBacktrackSampleFont.create();
        assertEquals(
                GposChainQuadBacktrackSampleFont.ADVANCE_LETTER
                        + GposChainQuadBacktrackSampleFont.CHAIN_DELTA,
                DefaultShaper.shape(four, "GFEDABC").get(4).xAdvance()
        );
        assertEquals(
                GposChainQuadBacktrackSampleFont.ADVANCE_LETTER,
                DefaultShaper.shape(four, "FEDABC").get(3).xAdvance()
        );
        SfntFont five = GposChainPentaBacktrackSampleFont.create();
        assertEquals(
                GposChainPentaBacktrackSampleFont.ADVANCE_LETTER
                        + GposChainPentaBacktrackSampleFont.CHAIN_DELTA,
                DefaultShaper.shape(five, "HGFEDABC").get(5).xAdvance()
        );
        assertEquals(
                GposChainPentaBacktrackSampleFont.ADVANCE_LETTER,
                DefaultShaper.shape(five, "GFEDABC").get(4).xAdvance()
        );
        SfntFont six = GposChainHexaBacktrackSampleFont.create();
        assertEquals(
                GposChainHexaBacktrackSampleFont.ADVANCE_LETTER
                        + GposChainHexaBacktrackSampleFont.CHAIN_DELTA,
                DefaultShaper.shape(six, "IHGFEDABC").get(6).xAdvance()
        );
        assertEquals(
                GposChainHexaBacktrackSampleFont.ADVANCE_LETTER,
                DefaultShaper.shape(six, "HGFEDABC").get(5).xAdvance()
        );
        SfntFont seven = GposChainHeptaBacktrackSampleFont.create();
        assertEquals(
                GposChainHeptaBacktrackSampleFont.ADVANCE_LETTER
                        + GposChainHeptaBacktrackSampleFont.CHAIN_DELTA,
                DefaultShaper.shape(seven, "JIHGFEDABC").get(7).xAdvance()
        );
        assertEquals(
                GposChainHeptaBacktrackSampleFont.ADVANCE_LETTER,
                DefaultShaper.shape(seven, "IHGFEDABC").get(6).xAdvance()
        );
        SfntFont eight = GposChainOctaBacktrackSampleFont.create();
        assertEquals(
                GposChainOctaBacktrackSampleFont.ADVANCE_LETTER
                        + GposChainOctaBacktrackSampleFont.CHAIN_DELTA,
                DefaultShaper.shape(eight, "KJIHGFEDABC").get(8).xAdvance()
        );
        assertEquals(
                GposChainOctaBacktrackSampleFont.ADVANCE_LETTER,
                DefaultShaper.shape(eight, "JIHGFEDABC").get(7).xAdvance()
        );
        SfntFont nine = GposChainNonaBacktrackSampleFont.create();
        assertEquals(
                GposChainNonaBacktrackSampleFont.ADVANCE_LETTER
                        + GposChainNonaBacktrackSampleFont.CHAIN_DELTA,
                DefaultShaper.shape(nine, "LKJIHGFEDABC").get(9).xAdvance()
        );
        assertEquals(
                GposChainNonaBacktrackSampleFont.ADVANCE_LETTER,
                DefaultShaper.shape(nine, "KJIHGFEDABC").get(8).xAdvance()
        );
        SfntFont flagged = GposFlaggedChainBacktrackSampleFont.create();
        assertEquals(
                GposFlaggedChainBacktrackSampleFont.ADVANCE_LETTER
                        + GposFlaggedChainBacktrackSampleFont.CHAIN_DELTA,
                DefaultShaper.shape(flagged, "DABCE").get(1).xAdvance()
        );
        assertEquals(
                GposFlaggedChainBacktrackSampleFont.ADVANCE_LETTER,
                DefaultShaper.shape(flagged, "ABCE").get(0).xAdvance()
        );
    }

    /// Applies reverse backtrack `BAC` through the shipped shaper.
    @Test
    void shapesReverseBacktrack() {
        SfntFont font = GsubReverseBacktrackSampleFont.create();
        assertEquals(GsubReverseBacktrackSampleFont.GLYPH_X, DefaultShaper.shape(font, "BAC").get(1).glyphId());
        assertEquals(GsubReverseBacktrackSampleFont.GLYPH_A, DefaultShaper.shape(font, "AC").get(0).glyphId());
    }

    /// Applies GPOS `IgnoreBaseGlyphs` and `IgnoreLigatures` through the shipped shaper.
    @Test
    void shapesIgnoreBaseAndLigaturePairs() {
        SfntFont font = GposIgnoreClassSampleFont.create();
        assertEquals(
                GposIgnoreClassSampleFont.ADVANCE_LETTER + GposIgnoreClassSampleFont.BASE_DELTA,
                DefaultShaper.shape(font, "ABC").get(0).xAdvance()
        );
        assertEquals(
                GposIgnoreClassSampleFont.ADVANCE_LETTER + GposIgnoreClassSampleFont.LIGA_DELTA,
                DefaultShaper.shape(font, "ADC").get(0).xAdvance()
        );
        assertEquals(
                GposIgnoreClassSampleFont.ADVANCE_LETTER
                        + GposIgnoreClassSampleFont.BASE_DELTA
                        + GposIgnoreClassSampleFont.LIGA_DELTA,
                DefaultShaper.shape(font, "AC").get(0).xAdvance()
        );
    }

    /// Applies GSUB class-skip substitutions through the shipped shaper.
    @Test
    void shapesIgnoreBaseContextAndIgnoreLigature() {
        SfntFont font = GsubIgnoreClassSampleFont.create();
        assertEquals(GsubIgnoreClassSampleFont.GLYPH_X, DefaultShaper.shape(font, "ABC").get(0).glyphId());
        assertEquals(1, DefaultShaper.shape(font, "ADC").size());
        assertEquals(GsubIgnoreClassSampleFont.GLYPH_X, DefaultShaper.shape(font, "ADC").get(0).glyphId());
    }

    /// Applies `UseMarkFilteringSet` pair and context rules through the shipped shaper.
    @Test
    void shapesMarkFilterSet() {
        SfntFont font = GdefMarkSetSampleFont.create();
        assertEquals(GdefMarkSetSampleFont.GLYPH_A, DefaultShaper.shape(font, "ABC").get(0).glyphId());
        assertEquals(GdefMarkSetSampleFont.ADVANCE_LETTER, DefaultShaper.shape(font, "ABC").get(0).xAdvance());
        assertEquals(GdefMarkSetSampleFont.GLYPH_X, DefaultShaper.shape(font, "ADC").get(0).glyphId());
    }

    /// Applies type-8 reverse `IgnoreMarks` through the shipped shaper.
    @Test
    void shapesReverseIgnoreMarks() {
        SfntFont font = GsubReverseSkipSampleFont.create();
        assertEquals(GsubReverseSkipSampleFont.GLYPH_X, DefaultShaper.shape(font, "ABC").get(0).glyphId());
        assertEquals(GsubReverseSkipSampleFont.GLYPH_A, DefaultShaper.shape(font, "AB").get(0).glyphId());
        assertEquals(GsubReverseSkipSampleFont.GLYPH_X, DefaultShaper.shape(font, "AC").get(0).glyphId());
    }

    /// Applies GSUB `MarkAttachmentType` ligatures through the shipped shaper.
    @Test
    void shapesMarkAttachmentTypeLigature() {
        SfntFont font = GsubMarkAttachSampleFont.create();
        java.util.List<ShapedGlyph> across = DefaultShaper.shape(font, "ABC");
        assertEquals(1, across.size());
        assertEquals(GsubMarkAttachSampleFont.GLYPH_X, across.get(0).glyphId());
        assertEquals(GsubMarkAttachSampleFont.ADVANCE_X, across.get(0).xAdvance());
        java.util.List<ShapedGlyph> kept = DefaultShaper.shape(font, "ADC");
        assertEquals(3, kept.size());
        assertEquals(GsubMarkAttachSampleFont.GLYPH_A, kept.get(0).glyphId());
    }

    /// Applies type-5 `IgnoreMarks` through the shipped shaper.
    @Test
    void shapesCaltSkippingGdefMark() {
        SfntFont font = GsubContextMarkSampleFont.create();
        assertEquals(GsubContextMarkSampleFont.GLYPH_X, DefaultShaper.shape(font, "ABC").get(0).glyphId());
        assertEquals(GsubContextMarkSampleFont.GLYPH_A, DefaultShaper.shape(font, "AB").get(0).glyphId());
    }

    /// Applies type-8 reverse `calt` through the shipped shaper.
    @Test
    void shapesReverseCalt() {
        SfntFont font = GsubReverseSampleFont.create();
        assertEquals(GsubReverseSampleFont.GLYPH_X, DefaultShaper.shape(font, "AB").get(0).glyphId());
        assertEquals(GsubReverseSampleFont.GLYPH_A, DefaultShaper.shape(font, "A").get(0).glyphId());
    }

    /// Ligates `AC` across a GDEF mark through the shipped shaper.
    @Test
    void shapesIgnoreMarksLigature() {
        SfntFont font = GdefMarkSampleFont.create();
        java.util.List<ShapedGlyph> glyphs = DefaultShaper.shape(font, "ABC");
        assertEquals(1, glyphs.size());
        assertEquals(GdefMarkSampleFont.GLYPH_X, glyphs.get(0).glyphId());
        assertEquals(GdefMarkSampleFont.ADVANCE_LIGATURE, glyphs.get(0).xAdvance());
    }

    /// Expands `A` through GSUB type-2 `ccmp` in the shipped shaper.
    @Test
    void shapesCcmpDecomposition() {
        SfntFont font = GsubMultipleSampleFont.create();
        java.util.List<ShapedGlyph> glyphs = DefaultShaper.shape(font, "A");
        assertEquals(2, glyphs.size());
        assertEquals(GsubMultipleSampleFont.GLYPH_X, glyphs.get(0).glyphId());
        assertEquals(GsubMultipleSampleFont.GLYPH_Y, glyphs.get(1).glyphId());
        assertEquals(GsubMultipleSampleFont.ADVANCE_X, glyphs.get(0).xAdvance());
        assertEquals(GsubMultipleSampleFont.ADVANCE_Y, glyphs.get(1).xAdvance());
        assertEquals(GsubMultipleSampleFont.GLYPH_Z, font.alternate(font.glyphId('A'), SfntFont.TAG_AALT));
    }

    /// Applies GSUB `calt` type-5 and type-6 substitutions through the shipped shaper.
    @Test
    void shapesCaltContextAndChain() {
        SfntFont context = GsubContextSampleFont.create();
        assertEquals(GsubContextSampleFont.GLYPH_X, DefaultShaper.shape(context, "AB").get(0).glyphId());
        assertEquals(GsubContextSampleFont.GLYPH_A, DefaultShaper.shape(context, "A").get(0).glyphId());
        SfntFont chain = GsubChainSampleFont.create();
        assertEquals(GsubChainSampleFont.GLYPH_Y, DefaultShaper.shape(chain, "ABC").get(0).glyphId());
        assertEquals(GsubChainSampleFont.GLYPH_A, DefaultShaper.shape(chain, "AB").get(0).glyphId());
    }

    /// Applies GPOS `IgnoreMarks` pair and chain advances through the shipped shaper.
    @Test
    void shapesIgnoreMarksPairAndChain() {
        SfntFont font = GposMarkSkipSampleFont.create();
        assertEquals(
                GposMarkSkipSampleFont.ADVANCE_LETTER + GposMarkSkipSampleFont.PAIR_DELTA,
                DefaultShaper.shape(font, "AC").get(0).xAdvance()
        );
        assertEquals(
                GposMarkSkipSampleFont.ADVANCE_LETTER + GposMarkSkipSampleFont.PAIR_DELTA,
                DefaultShaper.shape(font, "ABC").get(0).xAdvance()
        );
        assertEquals(
                GposMarkSkipSampleFont.ADVANCE_LETTER
                        + GposMarkSkipSampleFont.PAIR_DELTA
                        + GposMarkSkipSampleFont.CHAIN_DELTA,
                DefaultShaper.shape(font, "ACD").get(0).xAdvance()
        );
        assertEquals(
                GposMarkSkipSampleFont.ADVANCE_LETTER
                        + GposMarkSkipSampleFont.PAIR_DELTA
                        + GposMarkSkipSampleFont.CHAIN_DELTA,
                DefaultShaper.shape(font, "ABCD").get(0).xAdvance()
        );
        assertEquals(GposMarkSkipSampleFont.ADVANCE_LETTER, DefaultShaper.shape(font, "AB").get(0).xAdvance());
    }

    /// Applies GPOS `MarkAttachmentType` while keeping the matching mark class.
    @Test
    void shapesMarkAttachmentTypePair() {
        SfntFont font = GposMarkAttachSampleFont.create();
        assertEquals(
                GposMarkAttachSampleFont.ADVANCE_LETTER + GposMarkAttachSampleFont.ATTACH_DELTA,
                DefaultShaper.shape(font, "AC").get(0).xAdvance()
        );
        assertEquals(
                GposMarkAttachSampleFont.ADVANCE_LETTER + GposMarkAttachSampleFont.ATTACH_DELTA,
                DefaultShaper.shape(font, "ABC").get(0).xAdvance()
        );
        assertEquals(
                GposMarkAttachSampleFont.ADVANCE_LETTER,
                DefaultShaper.shape(font, "ADC").get(0).xAdvance()
        );
        assertEquals(
                GposMarkAttachSampleFont.ADVANCE_LETTER,
                DefaultShaper.shape(font, "AB").get(0).xAdvance()
        );
    }

    /// Applies type-1, type-7, and type-8 X-advances through the shipped shaper.
    @Test
    void shapesSingleContextAndChainAdvances() {
        SfntFont single = GposSingleSampleFont.create();
        assertEquals(
                GposSingleSampleFont.ADVANCE_LETTER + GposSingleSampleFont.SINGLE_DELTA,
                DefaultShaper.shape(single, "A").getFirst().xAdvance()
        );
        SfntFont context = GposContextSampleFont.create();
        assertEquals(
                GposContextSampleFont.ADVANCE_LETTER + GposContextSampleFont.CONTEXT_DELTA,
                DefaultShaper.shape(context, "AB").get(0).xAdvance()
        );
        SfntFont chain = GposChainSampleFont.create();
        assertEquals(
                GposChainSampleFont.ADVANCE_LETTER + GposChainSampleFont.CHAIN_DELTA,
                DefaultShaper.shape(chain, "ABC").get(0).xAdvance()
        );
        assertEquals(GposChainSampleFont.ADVANCE_LETTER, DefaultShaper.shape(chain, "AB").get(0).xAdvance());
    }

    /// Places kasra on fatha through GPOS type-6 and the shipped shaper.
    @Test
    void shapesMarkToMarkOffset() {
        SfntFont font = GposMarkMarkSampleFont.create();
        List<ShapedGlyph> glyphs = DefaultShaper.shape(font, "\u064E\u0650");
        assertEquals(2, glyphs.size());
        assertEquals(GposMarkMarkSampleFont.GLYPH_KASRA, glyphs.get(1).glyphId());
        assertEquals(GposMarkMarkSampleFont.MARK_X_OFFSET, glyphs.get(1).xOffset());
        assertEquals(GposMarkMarkSampleFont.MARK_Y_OFFSET, glyphs.get(1).yOffset());
    }

    /// Applies a GPOS type-3 cursive X-advance through the shipped shaper.
    @Test
    void shapesCursiveAdvanceOnAb() {
        SfntFont font = GposCursiveSampleFont.create();
        List<ShapedGlyph> glyphs = DefaultShaper.shape(font, "AB");
        assertEquals(2, glyphs.size());
        assertEquals(
                GposCursiveSampleFont.ADVANCE_LETTER + GposCursiveSampleFont.CURSIVE_DELTA,
                glyphs.get(0).xAdvance()
        );
        assertEquals(GposCursiveSampleFont.ADVANCE_LETTER, glyphs.get(1).xAdvance());
    }

    /// Maps Latin one-to-one through the bundled sample font.
    @Test
    void shapesLatinOneToOne() {
        SfntFont font = BitmapSfntFont.create();
        List<ShapedGlyph> glyphs = DefaultShaper.shape(font, "AB");
        assertEquals(2, glyphs.size());
        assertEquals('A', glyphs.get(0).codePoint());
        assertEquals(font.glyphId('A'), glyphs.get(0).glyphId());
        assertEquals(0, glyphs.get(0).cluster());
        assertEquals(1, glyphs.get(1).cluster());
    }

    /// Maps Latin through a CFF 1 face using the shipped shaper.
    @Test
    void shapesCffLatin() {
        SfntFont font = CffSampleFont.create();
        List<ShapedGlyph> glyphs = DefaultShaper.shape(font, "A");
        assertEquals(1, glyphs.size());
        assertEquals('A', glyphs.getFirst().codePoint());
        assertEquals(CffSampleFont.GLYPH_A, glyphs.getFirst().glyphId());
        assertEquals(font.metrics(CffSampleFont.GLYPH_A).advanceWidth(), glyphs.getFirst().xAdvance());
    }

    /// Maps isolated, initial, medial, and final Beh through Presentation Forms-B.
    @Test
    void shapesArabicInitMediFina() {
        SfntFont font = ScriptSampleFont.create();
        List<ShapedGlyph> isolated = DefaultShaper.shape(font, "\u0628");
        assertEquals(1, isolated.size());
        assertEquals(0xFE8F, isolated.getFirst().codePoint());
        assertNotEquals(font.glyphId('\u0628'), isolated.getFirst().glyphId());
        assertTrue(isolated.getFirst().glyphId() > 0);

        List<ShapedGlyph> pair = DefaultShaper.shape(font, "\u0628\u062A");
        assertEquals(0xFE91, pair.get(0).codePoint());
        assertEquals(0xFE96, pair.get(1).codePoint());

        List<ShapedGlyph> triple = DefaultShaper.shape(font, "\u0628\u0628\u0628");
        assertEquals(0xFE91, triple.get(0).codePoint());
        assertEquals(0xFE92, triple.get(1).codePoint());
        assertEquals(0xFE90, triple.get(2).codePoint());
    }

    /// Maps Beh plus Alef to initial plus final forms.
    @Test
    void shapesArabicRightJoiningAlef() {
        SfntFont font = ScriptSampleFont.create();
        List<ShapedGlyph> joined = DefaultShaper.shape(font, "\u0628\u0627");
        assertEquals(0xFE91, joined.get(0).codePoint());
        assertEquals(0xFE8E, joined.get(1).codePoint());
        List<ShapedGlyph> split = DefaultShaper.shape(font, "\u0627\u0628");
        assertEquals(0xFE8D, split.get(0).codePoint());
        assertEquals(0xFE8F, split.get(1).codePoint());
    }

    /// Keeps a fatha in the preceding letter cluster and still joins across it.
    @Test
    void joinsAcrossArabicMark() {
        SfntFont font = ScriptSampleFont.create();
        List<ShapedGlyph> glyphs = DefaultShaper.shape(font, "\u0628\u064E\u062A");
        assertEquals(3, glyphs.size());
        assertEquals(0xFE91, glyphs.get(0).codePoint());
        assertEquals(0x064E, glyphs.get(1).codePoint());
        assertEquals(0, glyphs.get(1).cluster());
        assertEquals(0xFE96, glyphs.get(2).codePoint());
        assertEquals(2, glyphs.get(2).cluster());
        assertEquals(0, glyphs.get(1).xAdvance());
    }

    /// Leaves unmarked Hebrew as one-to-one cmap mapping.
    @Test
    void shapesUnmarkedHebrew() {
        SfntFont font = ScriptSampleFont.create();
        List<ShapedGlyph> glyphs = DefaultShaper.shape(font, "\u05D0\u05D1");
        assertEquals(2, glyphs.size());
        assertEquals(0x05D0, glyphs.get(0).codePoint());
        assertEquals(0x05D1, glyphs.get(1).codePoint());
        assertTrue(glyphs.get(0).glyphId() > 0);
        assertNotEquals(glyphs.get(0).glyphId(), glyphs.get(1).glyphId());
    }

    /// Composes Bet plus dagesh onto a Presentation Forms-A glyph.
    @Test
    void composesHebrewDagesh() {
        SfntFont font = ScriptSampleFont.create();
        List<ShapedGlyph> glyphs = DefaultShaper.shape(font, "\u05D1\u05BC");
        assertEquals(1, glyphs.size());
        assertEquals(0xFB31, glyphs.getFirst().codePoint());
        assertNotEquals(font.glyphId('\u05D1'), glyphs.getFirst().glyphId());
        assertEquals(0, glyphs.getFirst().cluster());
    }

    /// Composes Hangul choseong plus jungseong into `가`.
    @Test
    void composesHangulLvSyllable() {
        SfntFont font = ScriptSampleFont.create();
        List<ShapedGlyph> glyphs = DefaultShaper.shape(font, "\u1100\u1161");
        assertEquals(1, glyphs.size());
        assertEquals(0xAC00, glyphs.getFirst().codePoint());
        assertNotEquals(font.glyphId('\u1100'), glyphs.getFirst().glyphId());
        assertEquals(0, glyphs.getFirst().cluster());
    }

    /// Composes Hangul LVT into `각`.
    @Test
    void composesHangulLvtSyllable() {
        SfntFont font = ScriptSampleFont.create();
        List<ShapedGlyph> glyphs = DefaultShaper.shape(font, "\u1100\u1161\u11A8");
        assertEquals(1, glyphs.size());
        assertEquals(0xAC01, glyphs.getFirst().codePoint());
        assertEquals(0, glyphs.getFirst().cluster());
    }

    /// Decomposes Thai SARA AM through the shipped shaper and `cmap`.
    @Test
    void shapesThaiSaraAm() {
        SfntFont font = ScriptSampleFont.create();
        List<ShapedGlyph> glyphs = DefaultShaper.shape(font, "\u0E01\u0E33");
        assertEquals(3, glyphs.size());
        assertEquals(0x0E01, glyphs.get(0).codePoint());
        assertEquals(0x0E4D, glyphs.get(1).codePoint());
        assertEquals(0x0E32, glyphs.get(2).codePoint());
        assertEquals(0, glyphs.get(0).cluster());
        assertEquals(0, glyphs.get(1).cluster());
        assertEquals(0, glyphs.get(2).cluster());
        assertTrue(glyphs.get(0).glyphId() > 0);
        assertTrue(glyphs.get(1).glyphId() > 0);
        assertNotEquals(glyphs.get(0).glyphId(), glyphs.get(1).glyphId());
        assertEquals(0, glyphs.get(1).xAdvance());
    }

    /// Reorders Nikhahit over MAI TRI on the shipped path.
    @Test
    void shapesThaiSaraAmAfterTone() {
        SfntFont font = ScriptSampleFont.create();
        List<ShapedGlyph> glyphs = DefaultShaper.shape(font, "\u0E14\u0E4B\u0E33");
        assertEquals(4, glyphs.size());
        assertEquals(0x0E14, glyphs.get(0).codePoint());
        assertEquals(0x0E4D, glyphs.get(1).codePoint());
        assertEquals(0x0E4B, glyphs.get(2).codePoint());
        assertEquals(0x0E32, glyphs.get(3).codePoint());
    }

    /// Leaves a Unicode-visual left vowel before its consonant.
    @Test
    void keepsThaiLeftVowelInVisualOrder() {
        SfntFont font = ScriptSampleFont.create();
        List<ShapedGlyph> glyphs = DefaultShaper.shape(font, "\u0E40\u0E01");
        assertEquals(2, glyphs.size());
        assertEquals(0x0E40, glyphs.get(0).codePoint());
        assertEquals(0x0E01, glyphs.get(1).codePoint());
        assertEquals(0, glyphs.get(0).cluster());
        assertEquals(1, glyphs.get(1).cluster());
    }

    /// Decomposes Lao SARA AM through the shipped shaper.
    @Test
    void shapesLaoSaraAm() {
        SfntFont font = ScriptSampleFont.create();
        List<ShapedGlyph> glyphs = DefaultShaper.shape(font, "\u0E81\u0EB3");
        assertEquals(3, glyphs.size());
        assertEquals(0x0E81, glyphs.get(0).codePoint());
        assertEquals(0x0ECD, glyphs.get(1).codePoint());
        assertEquals(0x0EB2, glyphs.get(2).codePoint());
    }

    /// Applies GSUB joining forms when the font has no Presentation Forms-B cmap.
    @Test
    void shapesArabicThroughGsubWhenPresent() {
        SfntFont font = GsubSampleFont.create();
        List<ShapedGlyph> isolated = DefaultShaper.shape(font, "\u0628");
        assertEquals(1, isolated.size());
        assertEquals(0x0628, isolated.getFirst().codePoint());
        assertEquals(GsubSampleFont.GLYPH_ISOL, isolated.getFirst().glyphId());
        assertEquals(GsubSampleFont.ADVANCE_ISOL, isolated.getFirst().xAdvance());

        List<ShapedGlyph> triple = DefaultShaper.shape(font, "\u0628\u0628\u0628");
        assertEquals(3, triple.size());
        assertEquals(GsubSampleFont.GLYPH_INIT, triple.get(0).glyphId());
        assertEquals(GsubSampleFont.GLYPH_MEDI, triple.get(1).glyphId());
        assertEquals(GsubSampleFont.GLYPH_FINA, triple.get(2).glyphId());
        assertEquals(GsubSampleFont.ADVANCE_INIT, triple.get(0).xAdvance());
        assertEquals(GsubSampleFont.ADVANCE_MEDI, triple.get(1).xAdvance());
        assertEquals(GsubSampleFont.ADVANCE_FINA, triple.get(2).xAdvance());
    }

    /// Composes vav plus holam onto `U+FB4B`.
    @Test
    void composesHebrewHolamVav() {
        SfntFont font = ScriptSampleFont.create();
        List<ShapedGlyph> glyphs = DefaultShaper.shape(font, "\u05D5\u05B9");
        assertEquals(1, glyphs.size());
        assertEquals(0xFB4B, glyphs.getFirst().codePoint());
        assertNotEquals(font.glyphId('\u05D5'), glyphs.getFirst().glyphId());
    }

    /// Composes shin plus shin-dot plus dagesh onto `U+FB2C`.
    @Test
    void composesHebrewShinDageshDot() {
        SfntFont font = ScriptSampleFont.create();
        List<ShapedGlyph> glyphs = DefaultShaper.shape(font, "\u05E9\u05C1\u05BC");
        assertEquals(1, glyphs.size());
        assertEquals(0xFB2C, glyphs.getFirst().codePoint());
    }

    /// Composes Shin plus shin-dot onto `U+FB2A`.
    @Test
    void composesHebrewShinDot() {
        SfntFont font = ScriptSampleFont.create();
        List<ShapedGlyph> glyphs = DefaultShaper.shape(font, "\u05E9\u05C1");
        assertEquals(1, glyphs.size());
        assertEquals(0xFB2A, glyphs.getFirst().codePoint());
    }

    /// Composes isolated LAM plus alef onto Presentation Forms-B lam-alef.
    @Test
    void shapesIsolatedLamAlef() {
        SfntFont font = ScriptSampleFont.create();
        List<ShapedGlyph> glyphs = DefaultShaper.shape(font, "\u0644\u0627");
        assertEquals(1, glyphs.size());
        assertEquals(0xFEFB, glyphs.getFirst().codePoint());
        assertNotEquals(font.glyphId('\u0644'), glyphs.getFirst().glyphId());
        assertEquals(0, glyphs.getFirst().cluster());
    }

    /// Composes medial LAM plus alef onto the final lam-alef form after Beh.
    @Test
    void shapesJoinedLamAlef() {
        SfntFont font = ScriptSampleFont.create();
        List<ShapedGlyph> glyphs = DefaultShaper.shape(font, "\u0628\u0644\u0627");
        assertEquals(2, glyphs.size());
        assertEquals(0xFE91, glyphs.get(0).codePoint());
        assertEquals(0xFEFC, glyphs.get(1).codePoint());
        assertEquals(0, glyphs.get(0).cluster());
        assertEquals(1, glyphs.get(1).cluster());
    }

    /// Keeps a fatha between LAM and alef in the LAM cluster.
    @Test
    void shapesLamAlefAcrossMark() {
        SfntFont font = ScriptSampleFont.create();
        List<ShapedGlyph> glyphs = DefaultShaper.shape(font, "\u0644\u064E\u0627");
        assertEquals(2, glyphs.size());
        assertEquals(0xFEFB, glyphs.get(0).codePoint());
        assertEquals(0x064E, glyphs.get(1).codePoint());
        assertEquals(0, glyphs.get(0).cluster());
        assertEquals(0, glyphs.get(1).cluster());
        assertEquals(0, glyphs.get(1).xAdvance());
    }

    /// Composes Hangul Compatibility Jamo `가` through the shipped shaper.
    @Test
    void composesHangulCompatibilityJamo() {
        SfntFont font = ScriptSampleFont.create();
        List<ShapedGlyph> glyphs = DefaultShaper.shape(font, "\u3131\u314F");
        assertEquals(1, glyphs.size());
        assertEquals(0xAC00, glyphs.getFirst().codePoint());
        assertEquals(0, glyphs.getFirst().cluster());
        assertTrue(glyphs.getFirst().glyphId() > 0);
    }

    /// Applies GSUB type-4 `rlig` when the font has no Presentation Forms-B cmap.
    @Test
    void shapesLamAlefThroughGsubLigature() {
        SfntFont font = GsubLigatureSampleFont.create();
        List<ShapedGlyph> glyphs = DefaultShaper.shape(font, "\u0644\u0627");
        assertEquals(1, glyphs.size());
        assertEquals(GsubLigatureSampleFont.GLYPH_LIGATURE, glyphs.getFirst().glyphId());
        assertEquals(GsubLigatureSampleFont.ADVANCE_LIGATURE, glyphs.getFirst().xAdvance());
        assertEquals(0, glyphs.getFirst().cluster());
    }
}

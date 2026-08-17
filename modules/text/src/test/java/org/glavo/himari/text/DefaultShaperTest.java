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
import org.glavo.himari.font.GposChainDecaBacktrackSampleFont;
import org.glavo.himari.font.GposChainUndecaBacktrackSampleFont;
import org.glavo.himari.font.GposChainDodecaBacktrackSampleFont;
import org.glavo.himari.font.GposChainTridecaBacktrackSampleFont;
import org.glavo.himari.font.GposChainTetradecaBacktrackSampleFont;
import org.glavo.himari.font.GposChainPentadecaBacktrackSampleFont;
import org.glavo.himari.font.GposChainHexadecaBacktrackSampleFont;
import org.glavo.himari.font.GposChainHeptadecaBacktrackSampleFont;
import org.glavo.himari.font.GposChainOctodecaBacktrackSampleFont;
import org.glavo.himari.font.GposChainEnneadecaBacktrackSampleFont;
import org.glavo.himari.font.GposChainIcosaBacktrackSampleFont;
import org.glavo.himari.font.GposChainHenicosaBacktrackSampleFont;
import org.glavo.himari.font.GposChainDocosaBacktrackSampleFont;
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
import org.glavo.himari.font.GsubChainDecaBacktrackSampleFont;
import org.glavo.himari.font.GsubChainUndecaBacktrackSampleFont;
import org.glavo.himari.font.GsubChainDodecaBacktrackSampleFont;
import org.glavo.himari.font.GsubChainTridecaBacktrackSampleFont;
import org.glavo.himari.font.GsubChainTetradecaBacktrackSampleFont;
import org.glavo.himari.font.GsubChainPentadecaBacktrackSampleFont;
import org.glavo.himari.font.GsubChainHexadecaBacktrackSampleFont;
import org.glavo.himari.font.GsubChainHeptadecaBacktrackSampleFont;
import org.glavo.himari.font.GsubChainOctodecaBacktrackSampleFont;
import org.glavo.himari.font.GsubChainEnneadecaBacktrackSampleFont;
import org.glavo.himari.font.GsubChainIcosaBacktrackSampleFont;
import org.glavo.himari.font.GsubChainHenicosaBacktrackSampleFont;
import org.glavo.himari.font.GsubChainDocosaBacktrackSampleFont;
import org.glavo.himari.font.GsubChainTricosaBacktrackSampleFont;
import org.glavo.himari.font.GsubChainTetracosaBacktrackSampleFont;
import org.glavo.himari.font.GsubChainPentacosaBacktrackSampleFont;
import org.glavo.himari.font.GsubChainHexacosaBacktrackSampleFont;
import org.glavo.himari.font.GsubChainHeptacosaBacktrackSampleFont;
import org.glavo.himari.font.GsubChainOctacosaBacktrackSampleFont;
import org.glavo.himari.font.GsubChainEnneacosaBacktrackSampleFont;
import org.glavo.himari.font.GsubChainTriacontaBacktrackSampleFont;
import org.glavo.himari.font.GsubChainHentricontaBacktrackSampleFont;
import org.glavo.himari.font.GsubChainDotricontaBacktrackSampleFont;
import org.glavo.himari.font.GsubChainTritriacontaBacktrackSampleFont;
import org.glavo.himari.font.GsubChainTetratriacontaBacktrackSampleFont;
import org.glavo.himari.font.GsubChainPentatriacontaBacktrackSampleFont;
import org.glavo.himari.font.GsubChainHexatriacontaBacktrackSampleFont;
import org.glavo.himari.font.GsubChainHeptatriacontaBacktrackSampleFont;
import org.glavo.himari.font.GsubChainOctatriacontaBacktrackSampleFont;
import org.glavo.himari.font.GsubChainEnneatriacontaBacktrackSampleFont;
import org.glavo.himari.font.GsubChainTetracontaBacktrackSampleFont;
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
        assertEquals(
                GsubChainDecaBacktrackSampleFont.GLYPH_Y,
                DefaultShaper.shape(GsubChainDecaBacktrackSampleFont.create(), "MLKJIHGFEDABC").get(10).glyphId()
        );
        assertEquals(
                GsubChainDecaBacktrackSampleFont.GLYPH_A,
                DefaultShaper.shape(GsubChainDecaBacktrackSampleFont.create(), "LKJIHGFEDABC").get(9).glyphId()
        );
        assertEquals(
                GsubChainUndecaBacktrackSampleFont.GLYPH_Y,
                DefaultShaper.shape(GsubChainUndecaBacktrackSampleFont.create(), "NMLKJIHGFEDABC").get(11).glyphId()
        );
        assertEquals(
                GsubChainUndecaBacktrackSampleFont.GLYPH_A,
                DefaultShaper.shape(GsubChainUndecaBacktrackSampleFont.create(), "MLKJIHGFEDABC").get(10).glyphId()
        );
        assertEquals(
                GsubChainDodecaBacktrackSampleFont.GLYPH_Y,
                DefaultShaper.shape(GsubChainDodecaBacktrackSampleFont.create(), "ONMLKJIHGFEDABC").get(12).glyphId()
        );
        assertEquals(
                GsubChainDodecaBacktrackSampleFont.GLYPH_A,
                DefaultShaper.shape(GsubChainDodecaBacktrackSampleFont.create(), "NMLKJIHGFEDABC").get(11).glyphId()
        );
        assertEquals(
                GsubChainTridecaBacktrackSampleFont.GLYPH_Y,
                DefaultShaper.shape(GsubChainTridecaBacktrackSampleFont.create(), "PONMLKJIHGFEDABC").get(13).glyphId()
        );
        assertEquals(
                GsubChainTridecaBacktrackSampleFont.GLYPH_A,
                DefaultShaper.shape(GsubChainTridecaBacktrackSampleFont.create(), "ONMLKJIHGFEDABC").get(12).glyphId()
        );
        assertEquals(
                GsubChainTetradecaBacktrackSampleFont.GLYPH_Y,
                DefaultShaper.shape(GsubChainTetradecaBacktrackSampleFont.create(), "QPONMLKJIHGFEDABC").get(14).glyphId()
        );
        assertEquals(
                GsubChainTetradecaBacktrackSampleFont.GLYPH_A,
                DefaultShaper.shape(GsubChainTetradecaBacktrackSampleFont.create(), "PONMLKJIHGFEDABC").get(13).glyphId()
        );
        assertEquals(
                GsubChainPentadecaBacktrackSampleFont.GLYPH_Y,
                DefaultShaper.shape(GsubChainPentadecaBacktrackSampleFont.create(), "RQPONMLKJIHGFEDABC").get(15).glyphId()
        );
        assertEquals(
                GsubChainPentadecaBacktrackSampleFont.GLYPH_A,
                DefaultShaper.shape(GsubChainPentadecaBacktrackSampleFont.create(), "QPONMLKJIHGFEDABC").get(14).glyphId()
        );
        assertEquals(
                GsubChainHexadecaBacktrackSampleFont.GLYPH_Y,
                DefaultShaper.shape(GsubChainHexadecaBacktrackSampleFont.create(), "SRQPONMLKJIHGFEDABC").get(16).glyphId()
        );
        assertEquals(
                GsubChainHexadecaBacktrackSampleFont.GLYPH_A,
                DefaultShaper.shape(GsubChainHexadecaBacktrackSampleFont.create(), "RQPONMLKJIHGFEDABC").get(15).glyphId()
        );
        assertEquals(
                GsubChainHeptadecaBacktrackSampleFont.GLYPH_Y,
                DefaultShaper.shape(GsubChainHeptadecaBacktrackSampleFont.create(), "TSRQPONMLKJIHGFEDABC")
                        .get(17)
                        .glyphId()
        );
        assertEquals(
                GsubChainHeptadecaBacktrackSampleFont.GLYPH_A,
                DefaultShaper.shape(GsubChainHeptadecaBacktrackSampleFont.create(), "SRQPONMLKJIHGFEDABC")
                        .get(16)
                        .glyphId()
        );
        assertEquals(
                GsubChainOctodecaBacktrackSampleFont.GLYPH_Y,
                DefaultShaper.shape(GsubChainOctodecaBacktrackSampleFont.create(), "UTSRQPONMLKJIHGFEDABC")
                        .get(18)
                        .glyphId()
        );
        assertEquals(
                GsubChainOctodecaBacktrackSampleFont.GLYPH_A,
                DefaultShaper.shape(GsubChainOctodecaBacktrackSampleFont.create(), "TSRQPONMLKJIHGFEDABC")
                        .get(17)
                        .glyphId()
        );
        assertEquals(
                GsubChainEnneadecaBacktrackSampleFont.GLYPH_Y,
                DefaultShaper.shape(GsubChainEnneadecaBacktrackSampleFont.create(), "VUTSRQPONMLKJIHGFEDABC")
                        .get(19)
                        .glyphId()
        );
        assertEquals(
                GsubChainEnneadecaBacktrackSampleFont.GLYPH_A,
                DefaultShaper.shape(GsubChainEnneadecaBacktrackSampleFont.create(), "UTSRQPONMLKJIHGFEDABC")
                        .get(18)
                        .glyphId()
        );
        assertEquals(
                GsubChainIcosaBacktrackSampleFont.GLYPH_Y,
                DefaultShaper.shape(GsubChainIcosaBacktrackSampleFont.create(), "WVUTSRQPONMLKJIHGFEDABC")
                        .get(20)
                        .glyphId()
        );
        assertEquals(
                GsubChainIcosaBacktrackSampleFont.GLYPH_A,
                DefaultShaper.shape(GsubChainIcosaBacktrackSampleFont.create(), "VUTSRQPONMLKJIHGFEDABC")
                        .get(19)
                        .glyphId()
        );
        assertEquals(
                GsubChainHenicosaBacktrackSampleFont.GLYPH_Y,
                DefaultShaper.shape(GsubChainHenicosaBacktrackSampleFont.create(), "XWVUTSRQPONMLKJIHGFEDABC")
                        .get(21)
                        .glyphId()
        );
        assertEquals(
                GsubChainHenicosaBacktrackSampleFont.GLYPH_A,
                DefaultShaper.shape(GsubChainHenicosaBacktrackSampleFont.create(), "WVUTSRQPONMLKJIHGFEDABC")
                        .get(20)
                        .glyphId()
        );
        assertEquals(
                GsubChainDocosaBacktrackSampleFont.GLYPH_Z,
                DefaultShaper.shape(GsubChainDocosaBacktrackSampleFont.create(), "YXWVUTSRQPONMLKJIHGFEDABC")
                        .get(22)
                        .glyphId()
        );
        assertEquals(
                GsubChainDocosaBacktrackSampleFont.GLYPH_A,
                DefaultShaper.shape(GsubChainDocosaBacktrackSampleFont.create(), "XWVUTSRQPONMLKJIHGFEDABC")
                        .get(21)
                        .glyphId()
        );
        assertEquals(
                GsubChainTricosaBacktrackSampleFont.GLYPH_ZERO,
                DefaultShaper.shape(GsubChainTricosaBacktrackSampleFont.create(), "ZYXWVUTSRQPONMLKJIHGFEDABC")
                        .get(23)
                        .glyphId()
        );
        assertEquals(
                GsubChainTricosaBacktrackSampleFont.GLYPH_A,
                DefaultShaper.shape(GsubChainTricosaBacktrackSampleFont.create(), "YXWVUTSRQPONMLKJIHGFEDABC")
                        .get(22)
                        .glyphId()
        );
        assertEquals(
                GsubChainTetracosaBacktrackSampleFont.GLYPH_ZERO,
                DefaultShaper.shape(GsubChainTetracosaBacktrackSampleFont.create(), "1ZYXWVUTSRQPONMLKJIHGFEDABC")
                        .get(24)
                        .glyphId()
        );
        assertEquals(
                GsubChainTetracosaBacktrackSampleFont.GLYPH_A,
                DefaultShaper.shape(GsubChainTetracosaBacktrackSampleFont.create(), "ZYXWVUTSRQPONMLKJIHGFEDABC")
                        .get(23)
                        .glyphId()
        );
        assertEquals(
                GsubChainPentacosaBacktrackSampleFont.GLYPH_ZERO,
                DefaultShaper.shape(GsubChainPentacosaBacktrackSampleFont.create(), "21ZYXWVUTSRQPONMLKJIHGFEDABC")
                        .get(25)
                        .glyphId()
        );
        assertEquals(
                GsubChainPentacosaBacktrackSampleFont.GLYPH_A,
                DefaultShaper.shape(GsubChainPentacosaBacktrackSampleFont.create(), "1ZYXWVUTSRQPONMLKJIHGFEDABC")
                        .get(24)
                        .glyphId()
        );
        assertEquals(
                GsubChainHexacosaBacktrackSampleFont.GLYPH_ZERO,
                DefaultShaper.shape(GsubChainHexacosaBacktrackSampleFont.create(), "321ZYXWVUTSRQPONMLKJIHGFEDABC")
                        .get(26)
                        .glyphId()
        );
        assertEquals(
                GsubChainHexacosaBacktrackSampleFont.GLYPH_A,
                DefaultShaper.shape(GsubChainHexacosaBacktrackSampleFont.create(), "21ZYXWVUTSRQPONMLKJIHGFEDABC")
                        .get(25)
                        .glyphId()
        );
        assertEquals(
                GsubChainHeptacosaBacktrackSampleFont.GLYPH_ZERO,
                DefaultShaper.shape(GsubChainHeptacosaBacktrackSampleFont.create(), "4321ZYXWVUTSRQPONMLKJIHGFEDABC")
                        .get(27)
                        .glyphId()
        );
        assertEquals(
                GsubChainHeptacosaBacktrackSampleFont.GLYPH_A,
                DefaultShaper.shape(GsubChainHeptacosaBacktrackSampleFont.create(), "321ZYXWVUTSRQPONMLKJIHGFEDABC")
                        .get(26)
                        .glyphId()
        );
        assertEquals(
                GsubChainOctacosaBacktrackSampleFont.GLYPH_ZERO,
                DefaultShaper.shape(GsubChainOctacosaBacktrackSampleFont.create(), "54321ZYXWVUTSRQPONMLKJIHGFEDABC")
                        .get(28)
                        .glyphId()
        );
        assertEquals(
                GsubChainOctacosaBacktrackSampleFont.GLYPH_A,
                DefaultShaper.shape(GsubChainOctacosaBacktrackSampleFont.create(), "4321ZYXWVUTSRQPONMLKJIHGFEDABC")
                        .get(27)
                        .glyphId()
        );
        assertEquals(
                GsubChainEnneacosaBacktrackSampleFont.GLYPH_ZERO,
                DefaultShaper.shape(GsubChainEnneacosaBacktrackSampleFont.create(), "654321ZYXWVUTSRQPONMLKJIHGFEDABC")
                        .get(29)
                        .glyphId()
        );
        assertEquals(
                GsubChainEnneacosaBacktrackSampleFont.GLYPH_A,
                DefaultShaper.shape(GsubChainEnneacosaBacktrackSampleFont.create(), "54321ZYXWVUTSRQPONMLKJIHGFEDABC")
                        .get(28)
                        .glyphId()
        );
        assertEquals(
                GsubChainTriacontaBacktrackSampleFont.GLYPH_ZERO,
                DefaultShaper.shape(GsubChainTriacontaBacktrackSampleFont.create(), "7654321ZYXWVUTSRQPONMLKJIHGFEDABC")
                        .get(30)
                        .glyphId()
        );
        assertEquals(
                GsubChainTriacontaBacktrackSampleFont.GLYPH_A,
                DefaultShaper.shape(GsubChainTriacontaBacktrackSampleFont.create(), "654321ZYXWVUTSRQPONMLKJIHGFEDABC")
                        .get(29)
                        .glyphId()
        );
        assertEquals(
                GsubChainHentricontaBacktrackSampleFont.GLYPH_ZERO,
                DefaultShaper.shape(GsubChainHentricontaBacktrackSampleFont.create(), "87654321ZYXWVUTSRQPONMLKJIHGFEDABC")
                        .get(31)
                        .glyphId()
        );
        assertEquals(
                GsubChainHentricontaBacktrackSampleFont.GLYPH_A,
                DefaultShaper.shape(GsubChainHentricontaBacktrackSampleFont.create(), "7654321ZYXWVUTSRQPONMLKJIHGFEDABC")
                        .get(30)
                        .glyphId()
        );
        assertEquals(
                GsubChainDotricontaBacktrackSampleFont.GLYPH_ZERO,
                DefaultShaper.shape(GsubChainDotricontaBacktrackSampleFont.create(), "987654321ZYXWVUTSRQPONMLKJIHGFEDABC")
                        .get(32)
                        .glyphId()
        );
        assertEquals(
                GsubChainDotricontaBacktrackSampleFont.GLYPH_A,
                DefaultShaper.shape(GsubChainDotricontaBacktrackSampleFont.create(), "87654321ZYXWVUTSRQPONMLKJIHGFEDABC")
                        .get(31)
                        .glyphId()
        );
        assertEquals(
                GsubChainTritriacontaBacktrackSampleFont.GLYPH_ZERO,
                DefaultShaper.shape(GsubChainTritriacontaBacktrackSampleFont.create(), ":987654321ZYXWVUTSRQPONMLKJIHGFEDABC")
                        .get(33)
                        .glyphId()
        );
        assertEquals(
                GsubChainTritriacontaBacktrackSampleFont.GLYPH_A,
                DefaultShaper.shape(GsubChainTritriacontaBacktrackSampleFont.create(), "987654321ZYXWVUTSRQPONMLKJIHGFEDABC")
                        .get(32)
                        .glyphId()
        );
        assertEquals(
                GsubChainTetratriacontaBacktrackSampleFont.GLYPH_ZERO,
                DefaultShaper.shape(GsubChainTetratriacontaBacktrackSampleFont.create(), ";:987654321ZYXWVUTSRQPONMLKJIHGFEDABC")
                        .get(34)
                        .glyphId()
        );
        assertEquals(
                GsubChainTetratriacontaBacktrackSampleFont.GLYPH_A,
                DefaultShaper.shape(GsubChainTetratriacontaBacktrackSampleFont.create(), ":987654321ZYXWVUTSRQPONMLKJIHGFEDABC")
                        .get(33)
                        .glyphId()
        );
        assertEquals(
                GsubChainPentatriacontaBacktrackSampleFont.GLYPH_ZERO,
                DefaultShaper.shape(GsubChainPentatriacontaBacktrackSampleFont.create(), "<;:987654321ZYXWVUTSRQPONMLKJIHGFEDABC")
                        .get(35)
                        .glyphId()
        );
        assertEquals(
                GsubChainPentatriacontaBacktrackSampleFont.GLYPH_A,
                DefaultShaper.shape(GsubChainPentatriacontaBacktrackSampleFont.create(), ";:987654321ZYXWVUTSRQPONMLKJIHGFEDABC")
                        .get(34)
                        .glyphId()
        );
        assertEquals(
                GsubChainHexatriacontaBacktrackSampleFont.GLYPH_ZERO,
                DefaultShaper.shape(GsubChainHexatriacontaBacktrackSampleFont.create(), "=" + "<;:987654321ZYXWVUTSRQPONMLKJIHGFEDABC")
                        .get(36)
                        .glyphId()
        );
        assertEquals(
                GsubChainHexatriacontaBacktrackSampleFont.GLYPH_A,
                DefaultShaper.shape(GsubChainHexatriacontaBacktrackSampleFont.create(), "<;:987654321ZYXWVUTSRQPONMLKJIHGFEDABC")
                        .get(35)
                        .glyphId()
        );
        assertEquals(
                GsubChainHeptatriacontaBacktrackSampleFont.GLYPH_ZERO,
                DefaultShaper.shape(GsubChainHeptatriacontaBacktrackSampleFont.create(), ">" + "=" + "<;:987654321ZYXWVUTSRQPONMLKJIHGFEDABC")
                        .get(37)
                        .glyphId()
        );
        assertEquals(
                GsubChainHeptatriacontaBacktrackSampleFont.GLYPH_A,
                DefaultShaper.shape(GsubChainHeptatriacontaBacktrackSampleFont.create(), "=" + "<;:987654321ZYXWVUTSRQPONMLKJIHGFEDABC")
                        .get(36)
                        .glyphId()
        );
        assertEquals(
                GsubChainOctatriacontaBacktrackSampleFont.GLYPH_ZERO,
                DefaultShaper.shape(GsubChainOctatriacontaBacktrackSampleFont.create(), "?" + ">" + "=" + "<;:987654321ZYXWVUTSRQPONMLKJIHGFEDABC")
                        .get(38)
                        .glyphId()
        );
        assertEquals(
                GsubChainOctatriacontaBacktrackSampleFont.GLYPH_A,
                DefaultShaper.shape(GsubChainOctatriacontaBacktrackSampleFont.create(), ">" + "=" + "<;:987654321ZYXWVUTSRQPONMLKJIHGFEDABC")
                        .get(37)
                        .glyphId()
        );
        assertEquals(
                GsubChainEnneatriacontaBacktrackSampleFont.GLYPH_ZERO,
                DefaultShaper.shape(GsubChainEnneatriacontaBacktrackSampleFont.create(), "@" + "?" + ">" + "=" + "<;:987654321ZYXWVUTSRQPONMLKJIHGFEDABC")
                        .get(39)
                        .glyphId()
        );
        assertEquals(
                GsubChainEnneatriacontaBacktrackSampleFont.GLYPH_A,
                DefaultShaper.shape(GsubChainEnneatriacontaBacktrackSampleFont.create(), "?" + ">" + "=" + "<;:987654321ZYXWVUTSRQPONMLKJIHGFEDABC")
                        .get(38)
                        .glyphId()
        );
        assertEquals(
                GsubChainTetracontaBacktrackSampleFont.GLYPH_ZERO,
                DefaultShaper.shape(GsubChainTetracontaBacktrackSampleFont.create(), "[" + "@" + "?" + ">" + "=" + "<;:987654321ZYXWVUTSRQPONMLKJIHGFEDABC")
                        .get(40)
                        .glyphId()
        );
        assertEquals(
                GsubChainTetracontaBacktrackSampleFont.GLYPH_A,
                DefaultShaper.shape(GsubChainTetracontaBacktrackSampleFont.create(), "@" + "?" + ">" + "=" + "<;:987654321ZYXWVUTSRQPONMLKJIHGFEDABC")
                        .get(39)
                        .glyphId()
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
        SfntFont ten = GposChainDecaBacktrackSampleFont.create();
        assertEquals(
                GposChainDecaBacktrackSampleFont.ADVANCE_LETTER
                        + GposChainDecaBacktrackSampleFont.CHAIN_DELTA,
                DefaultShaper.shape(ten, "MLKJIHGFEDABC").get(10).xAdvance()
        );
        assertEquals(
                GposChainDecaBacktrackSampleFont.ADVANCE_LETTER,
                DefaultShaper.shape(ten, "LKJIHGFEDABC").get(9).xAdvance()
        );
        SfntFont eleven = GposChainUndecaBacktrackSampleFont.create();
        assertEquals(
                GposChainUndecaBacktrackSampleFont.ADVANCE_LETTER
                        + GposChainUndecaBacktrackSampleFont.CHAIN_DELTA,
                DefaultShaper.shape(eleven, "NMLKJIHGFEDABC").get(11).xAdvance()
        );
        assertEquals(
                GposChainUndecaBacktrackSampleFont.ADVANCE_LETTER,
                DefaultShaper.shape(eleven, "MLKJIHGFEDABC").get(10).xAdvance()
        );
        SfntFont twelve = GposChainDodecaBacktrackSampleFont.create();
        assertEquals(
                GposChainDodecaBacktrackSampleFont.ADVANCE_LETTER
                        + GposChainDodecaBacktrackSampleFont.CHAIN_DELTA,
                DefaultShaper.shape(twelve, "ONMLKJIHGFEDABC").get(12).xAdvance()
        );
        assertEquals(
                GposChainDodecaBacktrackSampleFont.ADVANCE_LETTER,
                DefaultShaper.shape(twelve, "NMLKJIHGFEDABC").get(11).xAdvance()
        );
        SfntFont thirteen = GposChainTridecaBacktrackSampleFont.create();
        assertEquals(
                GposChainTridecaBacktrackSampleFont.ADVANCE_LETTER
                        + GposChainTridecaBacktrackSampleFont.CHAIN_DELTA,
                DefaultShaper.shape(thirteen, "PONMLKJIHGFEDABC").get(13).xAdvance()
        );
        assertEquals(
                GposChainTridecaBacktrackSampleFont.ADVANCE_LETTER,
                DefaultShaper.shape(thirteen, "ONMLKJIHGFEDABC").get(12).xAdvance()
        );
        SfntFont fourteen = GposChainTetradecaBacktrackSampleFont.create();
        assertEquals(
                GposChainTetradecaBacktrackSampleFont.ADVANCE_LETTER
                        + GposChainTetradecaBacktrackSampleFont.CHAIN_DELTA,
                DefaultShaper.shape(fourteen, "QPONMLKJIHGFEDABC").get(14).xAdvance()
        );
        assertEquals(
                GposChainTetradecaBacktrackSampleFont.ADVANCE_LETTER,
                DefaultShaper.shape(fourteen, "PONMLKJIHGFEDABC").get(13).xAdvance()
        );
        SfntFont fifteen = GposChainPentadecaBacktrackSampleFont.create();
        assertEquals(
                GposChainPentadecaBacktrackSampleFont.ADVANCE_LETTER
                        + GposChainPentadecaBacktrackSampleFont.CHAIN_DELTA,
                DefaultShaper.shape(fifteen, "RQPONMLKJIHGFEDABC").get(15).xAdvance()
        );
        assertEquals(
                GposChainPentadecaBacktrackSampleFont.ADVANCE_LETTER,
                DefaultShaper.shape(fifteen, "QPONMLKJIHGFEDABC").get(14).xAdvance()
        );
        SfntFont sixteen = GposChainHexadecaBacktrackSampleFont.create();
        assertEquals(
                GposChainHexadecaBacktrackSampleFont.ADVANCE_LETTER
                        + GposChainHexadecaBacktrackSampleFont.CHAIN_DELTA,
                DefaultShaper.shape(sixteen, "SRQPONMLKJIHGFEDABC").get(16).xAdvance()
        );
        assertEquals(
                GposChainHexadecaBacktrackSampleFont.ADVANCE_LETTER,
                DefaultShaper.shape(sixteen, "RQPONMLKJIHGFEDABC").get(15).xAdvance()
        );
        SfntFont seventeen = GposChainHeptadecaBacktrackSampleFont.create();
        assertEquals(
                GposChainHeptadecaBacktrackSampleFont.ADVANCE_LETTER
                        + GposChainHeptadecaBacktrackSampleFont.CHAIN_DELTA,
                DefaultShaper.shape(seventeen, "TSRQPONMLKJIHGFEDABC").get(17).xAdvance()
        );
        assertEquals(
                GposChainHeptadecaBacktrackSampleFont.ADVANCE_LETTER,
                DefaultShaper.shape(seventeen, "SRQPONMLKJIHGFEDABC").get(16).xAdvance()
        );
        SfntFont eighteen = GposChainOctodecaBacktrackSampleFont.create();
        assertEquals(
                GposChainOctodecaBacktrackSampleFont.ADVANCE_LETTER
                        + GposChainOctodecaBacktrackSampleFont.CHAIN_DELTA,
                DefaultShaper.shape(eighteen, "UTSRQPONMLKJIHGFEDABC").get(18).xAdvance()
        );
        assertEquals(
                GposChainOctodecaBacktrackSampleFont.ADVANCE_LETTER,
                DefaultShaper.shape(eighteen, "TSRQPONMLKJIHGFEDABC").get(17).xAdvance()
        );
        SfntFont nineteen = GposChainEnneadecaBacktrackSampleFont.create();
        assertEquals(
                GposChainEnneadecaBacktrackSampleFont.ADVANCE_LETTER
                        + GposChainEnneadecaBacktrackSampleFont.CHAIN_DELTA,
                DefaultShaper.shape(nineteen, "VUTSRQPONMLKJIHGFEDABC").get(19).xAdvance()
        );
        assertEquals(
                GposChainEnneadecaBacktrackSampleFont.ADVANCE_LETTER,
                DefaultShaper.shape(nineteen, "UTSRQPONMLKJIHGFEDABC").get(18).xAdvance()
        );
        SfntFont twenty = GposChainIcosaBacktrackSampleFont.create();
        assertEquals(
                GposChainIcosaBacktrackSampleFont.ADVANCE_LETTER + GposChainIcosaBacktrackSampleFont.CHAIN_DELTA,
                DefaultShaper.shape(twenty, "WVUTSRQPONMLKJIHGFEDABC").get(20).xAdvance()
        );
        assertEquals(
                GposChainIcosaBacktrackSampleFont.ADVANCE_LETTER,
                DefaultShaper.shape(twenty, "VUTSRQPONMLKJIHGFEDABC").get(19).xAdvance()
        );
        SfntFont twentyOne = GposChainHenicosaBacktrackSampleFont.create();
        assertEquals(
                GposChainHenicosaBacktrackSampleFont.ADVANCE_LETTER
                        + GposChainHenicosaBacktrackSampleFont.CHAIN_DELTA,
                DefaultShaper.shape(twentyOne, "XWVUTSRQPONMLKJIHGFEDABC").get(21).xAdvance()
        );
        assertEquals(
                GposChainHenicosaBacktrackSampleFont.ADVANCE_LETTER,
                DefaultShaper.shape(twentyOne, "WVUTSRQPONMLKJIHGFEDABC").get(20).xAdvance()
        );
        SfntFont twentyTwo = GposChainDocosaBacktrackSampleFont.create();
        assertEquals(
                GposChainDocosaBacktrackSampleFont.ADVANCE_LETTER
                        + GposChainDocosaBacktrackSampleFont.CHAIN_DELTA,
                DefaultShaper.shape(twentyTwo, "YXWVUTSRQPONMLKJIHGFEDABC").get(22).xAdvance()
        );
        assertEquals(
                GposChainDocosaBacktrackSampleFont.ADVANCE_LETTER,
                DefaultShaper.shape(twentyTwo, "XWVUTSRQPONMLKJIHGFEDABC").get(21).xAdvance()
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

    /// NFC-composes `e` plus combining acute onto `U+00E9` before `cmap` mapping.
    @Test
    void nfcComposesLatinBeforeCmap() {
        assertEquals("\u00E9", UnicodeNormalize.nfc("e\u0301"));
        assertEquals("e\u0301", UnicodeNormalize.nfd("\u00E9"));
        assertEquals("\u00E9", UnicodeNormalize.nfkc("\u0065\u0301"));
        assertEquals("e\u0301", UnicodeNormalize.nfkd("\u00E9"));
        SfntFont font = ScriptSampleFont.create();
        List<ShapedGlyph> glyphs = DefaultShaper.shape(font, "e\u0301");
        assertEquals(1, glyphs.size());
        assertEquals(0x00E9, glyphs.getFirst().codePoint());
    }

    /// Selects Hebrew final kaf at the end of a word.
    @Test
    void selectsHebrewFinalKaf() {
        SfntFont font = ScriptSampleFont.create();
        List<ShapedGlyph> glyphs = DefaultShaper.shape(font, "\u05D0\u05DB");
        assertEquals(2, glyphs.size());
        assertEquals(0x05D0, glyphs.get(0).codePoint());
        assertEquals(0x05DA, glyphs.get(1).codePoint());
        assertNotEquals(font.glyphId('\u05DB'), glyphs.get(1).glyphId());
    }

    /// Keeps medial kaf when another Hebrew letter follows.
    @Test
    void keepsMedialKafBeforeHebrewLetter() {
        SfntFont font = ScriptSampleFont.create();
        List<ShapedGlyph> glyphs = DefaultShaper.shape(font, "\u05DB\u05D0");
        assertEquals(0x05DB, glyphs.get(0).codePoint());
    }

    /// Composes Arabic shadda plus fatha onto `U+FC60`.
    @Test
    void composesArabicShaddaFatha() {
        SfntFont font = ScriptSampleFont.create();
        List<ShapedGlyph> glyphs = DefaultShaper.shape(font, "\u0651\u064E");
        assertEquals(1, glyphs.size());
        assertEquals(0xFC60, glyphs.getFirst().codePoint());
        assertTrue(glyphs.getFirst().glyphId() > 0);
        List<ShapedGlyph> dammatan = DefaultShaper.shape(font, "\u0651\u064C");
        assertEquals(1, dammatan.size());
        assertEquals(0xFC5E, dammatan.getFirst().codePoint());
        assertTrue(dammatan.getFirst().glyphId() > 0);
    }

    /// Composes Arabic Allah onto `U+FDF2`.
    @Test
    void composesArabicAllah() {
        SfntFont font = ScriptSampleFont.create();
        List<ShapedGlyph> glyphs = DefaultShaper.shape(font, "\u0644\u0644\u0647");
        assertEquals(1, glyphs.size());
        assertEquals(0xFDF2, glyphs.getFirst().codePoint());
        assertTrue(glyphs.getFirst().glyphId() > 0);
        List<ShapedGlyph> marked = DefaultShaper.shape(font, "\u0644\u0644\u0651\u0670\u0647");
        assertEquals(1, marked.size());
        assertEquals(0xFDF2, marked.getFirst().codePoint());
        assertTrue(glyphs.getFirst().unsafeToBreak());
    }

    /// Selects alef-wasla presentation forms and marks ligature clusters unsafe to break.
    @Test
    void shapesArabicAlefWaslaAndMarksLigaturesUnsafeToBreak() {
        SfntFont font = ScriptSampleFont.create();
        List<ShapedGlyph> isolated = DefaultShaper.shape(font, "\u0671");
        assertEquals(1, isolated.size());
        assertEquals(0xFB50, isolated.getFirst().codePoint());
        List<ShapedGlyph> finalForm = DefaultShaper.shape(font, "\u0628\u0671");
        assertEquals(0xFB51, finalForm.get(1).codePoint());
        List<ShapedGlyph> lamAlef = DefaultShaper.shape(font, "\u0644\u0627");
        assertEquals(1, lamAlef.size());
        assertTrue(lamAlef.getFirst().unsafeToBreak());
        List<ShapedGlyph> peh = DefaultShaper.shape(font, "\u067E\u067E");
        assertEquals(0xFB58, peh.get(0).codePoint());
        assertEquals(0xFB57, peh.get(1).codePoint());
        List<ShapedGlyph> tcheh = DefaultShaper.shape(font, "\u0686");
        assertEquals(0xFB7A, tcheh.getFirst().codePoint());
        List<ShapedGlyph> tteh = DefaultShaper.shape(font, "\u0679\u0679");
        assertEquals(0xFB68, tteh.get(0).codePoint());
        assertEquals(0xFB67, tteh.get(1).codePoint());
        List<ShapedGlyph> jeh = DefaultShaper.shape(font, "\u0698");
        assertEquals(0xFB8A, jeh.getFirst().codePoint());
        List<ShapedGlyph> veh = DefaultShaper.shape(font, "\u06A4\u06A4");
        assertEquals(0xFB6C, veh.get(0).codePoint());
        assertEquals(0xFB6B, veh.get(1).codePoint());
        List<ShapedGlyph> keheh = DefaultShaper.shape(font, "\u06A9");
        assertEquals(0xFB8E, keheh.getFirst().codePoint());
        List<ShapedGlyph> gaf = DefaultShaper.shape(font, "\u06AF");
        assertEquals(0xFB92, gaf.getFirst().codePoint());
        List<ShapedGlyph> farsi = DefaultShaper.shape(font, "\u06CC");
        assertEquals(0xFBFC, farsi.getFirst().codePoint());
        List<ShapedGlyph> noon = DefaultShaper.shape(font, "\u06BA");
        assertEquals(0xFB9E, noon.getFirst().codePoint());
        List<ShapedGlyph> barree = DefaultShaper.shape(font, "\u0628\u06D2");
        assertEquals(0xFBAF, barree.get(1).codePoint());
    }

    /// Composes Yiddish double vav onto `U+05F0`.
    @Test
    void composesYiddishDoubleVav() {
        SfntFont font = ScriptSampleFont.create();
        List<ShapedGlyph> glyphs = DefaultShaper.shape(font, "\u05D5\u05D5");
        assertEquals(1, glyphs.size());
        assertEquals(0x05F0, glyphs.getFirst().codePoint());
        assertTrue(glyphs.getFirst().glyphId() > 0);
    }

    /// Decomposes a precomposed Hangul syllable that the font does not map.
    @Test
    void decomposesMissingHangulSyllable() {
        SfntFont font = ScriptSampleFont.create();
        assertEquals(0, font.glyphId(0xAC04));
        List<ShapedGlyph> glyphs = DefaultShaper.shape(font, "\uAC04");
        assertEquals(3, glyphs.size());
        assertEquals(0x1100, glyphs.get(0).codePoint());
        assertEquals(0x1161, glyphs.get(1).codePoint());
        assertEquals(0x11AB, glyphs.get(2).codePoint());
        assertTrue(glyphs.get(0).glyphId() > 0);
        assertTrue(glyphs.get(1).glyphId() > 0);
    }

    /// Composes Lao ho-no onto `U+0EDC`.
    @Test
    void composesLaoHoNo() {
        SfntFont font = ScriptSampleFont.create();
        List<ShapedGlyph> glyphs = DefaultShaper.shape(font, "\u0EAB\u0E99");
        assertEquals(1, glyphs.size());
        assertEquals(0x0EDC, glyphs.getFirst().codePoint());
        assertTrue(glyphs.getFirst().glyphId() > 0);
        List<ShapedGlyph> homo = DefaultShaper.shape(font, "\u0EAB\u0EA1");
        assertEquals(1, homo.size());
        assertEquals(0x0EDD, homo.getFirst().codePoint());
        assertTrue(homo.getFirst().glyphId() > 0);
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
        List<ShapedGlyph> finalNun = DefaultShaper.shape(font, "\u05DF\u05BC");
        assertEquals(1, finalNun.size());
        assertEquals(0xFB3F, finalNun.getFirst().codePoint());
        List<ShapedGlyph> finalTsadi = DefaultShaper.shape(font, "\u05E5\u05BC");
        assertEquals(1, finalTsadi.size());
        assertEquals(0xFB45, finalTsadi.getFirst().codePoint());
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
        List<ShapedGlyph> halfwidth = DefaultShaper.shape(font, "\uFFA1\uFFC2");
        assertEquals(1, halfwidth.size());
        assertEquals(0xAC00, halfwidth.getFirst().codePoint());
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

    /// Composes yod plus hiriq onto `U+FB1D`.
    @Test
    void composesHebrewYodHiriq() {
        SfntFont font = ScriptSampleFont.create();
        List<ShapedGlyph> glyphs = DefaultShaper.shape(font, "\u05D9\u05B4");
        assertEquals(1, glyphs.size());
        assertEquals(0xFB1D, glyphs.getFirst().codePoint());
        assertNotEquals(font.glyphId('\u05D9'), glyphs.getFirst().glyphId());
    }

    /// Composes alef plus patah onto `U+FB2E`.
    @Test
    void composesHebrewAlefPatah() {
        SfntFont font = ScriptSampleFont.create();
        List<ShapedGlyph> glyphs = DefaultShaper.shape(font, "\u05D0\u05B7");
        assertEquals(1, glyphs.size());
        assertEquals(0xFB2E, glyphs.getFirst().codePoint());
        assertNotEquals(font.glyphId('\u05D0'), glyphs.getFirst().glyphId());
    }

    /// Composes alef plus qamats onto `U+FB2F`.
    @Test
    void composesHebrewAlefQamats() {
        SfntFont font = ScriptSampleFont.create();
        List<ShapedGlyph> glyphs = DefaultShaper.shape(font, "\u05D0\u05B8");
        assertEquals(1, glyphs.size());
        assertEquals(0xFB2F, glyphs.getFirst().codePoint());
        assertNotEquals(font.glyphId('\u05D0'), glyphs.getFirst().glyphId());
        List<ShapedGlyph> qatan = DefaultShaper.shape(font, "\u05D0\u05C7");
        assertEquals(1, qatan.size());
        assertEquals(0xFB2F, qatan.getFirst().codePoint());
    }

    /// Composes bet plus rafe onto `U+FB4C`.
    @Test
    void composesHebrewBetRafe() {
        SfntFont font = ScriptSampleFont.create();
        List<ShapedGlyph> glyphs = DefaultShaper.shape(font, "\u05D1\u05BF");
        assertEquals(1, glyphs.size());
        assertEquals(0xFB4C, glyphs.getFirst().codePoint());
        assertNotEquals(font.glyphId('\u05D1'), glyphs.getFirst().glyphId());
    }

    /// Composes alef plus lamed onto `U+FB4F`.
    @Test
    void composesHebrewAlefLamed() {
        SfntFont font = ScriptSampleFont.create();
        List<ShapedGlyph> glyphs = DefaultShaper.shape(font, "\u05D0\u05DC");
        assertEquals(1, glyphs.size());
        assertEquals(0xFB4F, glyphs.getFirst().codePoint());
        assertNotEquals(font.glyphId('\u05D0'), glyphs.getFirst().glyphId());
    }

    /// Composes yod plus yod plus patah onto `U+FB1F`.
    @Test
    void composesHebrewYodYodPatah() {
        SfntFont font = ScriptSampleFont.create();
        List<ShapedGlyph> glyphs = DefaultShaper.shape(font, "\u05D9\u05D9\u05B7");
        assertEquals(1, glyphs.size());
        assertEquals(0xFB1F, glyphs.getFirst().codePoint());
        assertNotEquals(font.glyphId('\u05D9'), glyphs.getFirst().glyphId());
    }

    /// Composes kaf plus rafe onto `U+FB4D`.
    @Test
    void composesHebrewKafRafe() {
        SfntFont font = ScriptSampleFont.create();
        List<ShapedGlyph> glyphs = DefaultShaper.shape(font, "\u05DB\u05BF");
        assertEquals(1, glyphs.size());
        assertEquals(0xFB4D, glyphs.getFirst().codePoint());
    }

    /// Composes pe plus rafe onto `U+FB4E`.
    @Test
    void composesHebrewPeRafe() {
        SfntFont font = ScriptSampleFont.create();
        List<ShapedGlyph> glyphs = DefaultShaper.shape(font, "\u05E4\u05BF");
        assertEquals(1, glyphs.size());
        assertEquals(0xFB4E, glyphs.getFirst().codePoint());
    }

    /// Composes vav plus holam onto `U+FB4B`.
    @Test
    void composesHebrewHolamVav() {
        SfntFont font = ScriptSampleFont.create();
        List<ShapedGlyph> glyphs = DefaultShaper.shape(font, "\u05D5\u05B9");
        assertEquals(1, glyphs.size());
        assertEquals(0xFB4B, glyphs.getFirst().codePoint());
        assertNotEquals(font.glyphId('\u05D5'), glyphs.getFirst().glyphId());
        List<ShapedGlyph> haser = DefaultShaper.shape(font, "\u05D5\u05BA");
        assertEquals(1, haser.size());
        assertEquals(0xFB4B, haser.getFirst().codePoint());
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
        assertEquals(0xFB21, HebrewPresentation.wideForm(0x05D0));
        assertEquals(0xFB28, HebrewPresentation.wideForm(0x05EA));
        assertEquals(0, HebrewPresentation.wideForm(0x05D1));
        assertEquals(0xFB20, HebrewPresentation.alternativeAyin());
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

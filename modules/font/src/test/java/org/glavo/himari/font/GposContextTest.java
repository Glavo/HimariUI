package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import java.lang.foreign.ValueLayout;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Verifies GPOS type-1, type-7, type-8, and TTC unwrap through [`SfntFont`] entries.
@NotNullByDefault
final class GposContextTest {
    /// Applies a type-1 X-advance to `A`.
    @Test
    void singlePosAddsAdvanceToA() {
        SfntFont font = GposSingleSampleFont.create();
        assertEquals(GposSingleSampleFont.SINGLE_DELTA, font.singleAdjustment(GposSingleSampleFont.GLYPH_A));
        assertEquals(0, font.singleAdjustment(0));
    }

    /// Flattens a type-7 `AB` rule into [`SfntFont#pairAdjustment(int, int)`].
    @Test
    void contextRuleAppliesPairDelta() {
        SfntFont font = GposContextSampleFont.create();
        int a = font.glyphId('A');
        int b = font.glyphId('B');
        assertEquals(GposContextSampleFont.CONTEXT_DELTA, font.pairAdjustment(a, b));
        assertEquals(0, font.pairAdjustment(b, a));
        assertEquals(0, font.singleAdjustment(a));
    }

    /// Matches a type-8 `ABC` lookahead rule.
    @Test
    void chainRuleAppliesLookaheadDelta() {
        SfntFont font = GposChainSampleFont.create();
        int a = font.glyphId('A');
        int b = font.glyphId('B');
        int c = font.glyphId('C');
        assertEquals(GposChainSampleFont.CHAIN_DELTA, font.chainAdjustment(a, b, c));
        assertEquals(0, font.chainAdjustment(a, b, a));
        assertEquals(0, font.pairAdjustment(a, b));
    }

    /// Opens the first face of a TTC wrap of the single-pos sample.
    @Test
    void ttcFirstFontOpensThroughSfntFont() {
        byte[] ttc = TtcFile.wrap(GposSingleSampleFont.bytes().toArray(ValueLayout.JAVA_BYTE));
        SfntFont font = new SfntFont(ttc);
        assertEquals(GposSingleSampleFont.SINGLE_DELTA, font.singleAdjustment(GposSingleSampleFont.GLYPH_A));
        assertEquals(GposSingleSampleFont.GLYPH_A, font.glyphId('A'));
    }
}

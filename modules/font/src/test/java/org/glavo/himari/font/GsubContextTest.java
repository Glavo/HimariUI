package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Verifies GSUB type-5 and type-6 `calt` through [`SfntFont`] entries.
@NotNullByDefault
final class GsubContextTest {
    /// Substitutes `A` before `B` through type-5 `calt`.
    @Test
    void contextRuleSubstitutesFirstGlyph() {
        SfntFont font = GsubContextSampleFont.create();
        int a = font.glyphId('A');
        int b = font.glyphId('B');
        int x = font.glyphId('X');
        assertEquals(GsubContextSampleFont.GLYPH_X, x);
        assertEquals(x, font.contextSubstitute(a, b, SfntFont.TAG_CALT));
        assertEquals(a, font.contextSubstitute(a, a, SfntFont.TAG_CALT));
        assertEquals(a, font.substitute(a, SfntFont.TAG_CALT));
    }

    /// Substitutes `A` before `BC` through type-6 `calt`.
    @Test
    void chainRuleSubstitutesWithLookahead() {
        SfntFont font = GsubChainSampleFont.create();
        int a = font.glyphId('A');
        int b = font.glyphId('B');
        int c = font.glyphId('C');
        int y = font.glyphId('Y');
        assertEquals(GsubChainSampleFont.GLYPH_Y, y);
        assertEquals(y, font.chainSubstitute(a, b, c, SfntFont.TAG_CALT));
        assertEquals(a, font.chainSubstitute(a, b, a, SfntFont.TAG_CALT));
    }
}

package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Verifies GSUB type-8 reverse chaining through [`SfntFont`] entries.
@NotNullByDefault
final class GsubReverseTest {
    /// Substitutes `A` before `B` through type-8 `calt`.
    @Test
    void reverseRuleSubstitutesWithLookahead() {
        SfntFont font = GsubReverseSampleFont.create();
        int a = font.glyphId('A');
        int b = font.glyphId('B');
        int x = font.glyphId('X');
        assertEquals(GsubReverseSampleFont.GLYPH_X, x);
        assertEquals(x, font.reverseSubstitute(a, b, SfntFont.TAG_CALT));
        assertEquals(a, font.reverseSubstitute(a, a, SfntFont.TAG_CALT));
        assertEquals(a, font.substitute(a, SfntFont.TAG_CALT));
    }
}

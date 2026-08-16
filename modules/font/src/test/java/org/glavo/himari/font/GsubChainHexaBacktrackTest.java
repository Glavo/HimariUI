package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Verifies six-glyph GSUB chain backtrack through [`SfntFont#chainSubstitute(int[], int, int, int)`].
@NotNullByDefault
final class GsubChainHexaBacktrackTest {
    /// Substitutes `A` only when format-1 backtrack `IHGFED` and lookahead `BC` are present.
    @Test
    void format1BacktrackRequiresIhgfed() {
        SfntFont font = GsubChainHexaBacktrackSampleFont.create();
        int a = font.glyphId('A');
        int b = font.glyphId('B');
        int c = font.glyphId('C');
        int d = font.glyphId('D');
        int e = font.glyphId('E');
        int f = font.glyphId('F');
        int g = font.glyphId('G');
        int h = font.glyphId('H');
        int i = font.glyphId('I');
        int y = font.glyphId('Y');
        assertEquals(GsubChainHexaBacktrackSampleFont.GLYPH_I, i);
        assertEquals(a, font.chainSubstitute(a, b, c, SfntFont.TAG_CALT));
        assertEquals(y, font.chainSubstitute(new int[] {i, h, g, f, e, d, a, b, c}, 6, 3, SfntFont.TAG_CALT));
        assertEquals(a, font.chainSubstitute(new int[] {h, g, f, e, d, a, b, c}, 5, 3, SfntFont.TAG_CALT));
        assertEquals(a, font.chainSubstitute(new int[] {i, g, f, e, d, a, b, c}, 5, 3, SfntFont.TAG_CALT));
    }
}

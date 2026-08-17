package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Verifies ten-glyph GSUB chain backtrack through [`SfntFont#chainSubstitute(int[], int, int, int)`].
@NotNullByDefault
final class GsubChainDecaBacktrackTest {
    /// Substitutes `A` only when format-1 backtrack `MLKJIHGFED` and lookahead `BC` are present.
    @Test
    void format1BacktrackRequiresMlkjihgfed() {
        SfntFont font = GsubChainDecaBacktrackSampleFont.create();
        int a = font.glyphId('A');
        int b = font.glyphId('B');
        int c = font.glyphId('C');
        int d = font.glyphId('D');
        int e = font.glyphId('E');
        int f = font.glyphId('F');
        int g = font.glyphId('G');
        int h = font.glyphId('H');
        int i = font.glyphId('I');
        int j = font.glyphId('J');
        int k = font.glyphId('K');
        int l = font.glyphId('L');
        int m = font.glyphId('M');
        int y = font.glyphId('Y');
        assertEquals(GsubChainDecaBacktrackSampleFont.GLYPH_M, m);
        assertEquals(GsubChainDecaBacktrackSampleFont.GLYPH_Y, y);
        assertEquals(a, font.chainSubstitute(a, b, c, SfntFont.TAG_CALT));
        assertEquals(y, font.chainSubstitute(new int[] {m, l, k, j, i, h, g, f, e, d, a, b, c}, 10, 3, SfntFont.TAG_CALT));
        assertEquals(a, font.chainSubstitute(new int[] {l, k, j, i, h, g, f, e, d, a, b, c}, 9, 3, SfntFont.TAG_CALT));
        assertEquals(a, font.chainSubstitute(new int[] {m, k, j, i, h, g, f, e, d, a, b, c}, 9, 3, SfntFont.TAG_CALT));
    }
}

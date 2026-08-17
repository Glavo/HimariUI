package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Verifies eleven-glyph GSUB chain backtrack through [`SfntFont#chainSubstitute(int[], int, int, int)`].
@NotNullByDefault
final class GsubChainUndecaBacktrackTest {
    /// Substitutes `A` only when format-1 backtrack `NMLKJIHGFED` and lookahead `BC` are present.
    @Test
    void format1BacktrackRequiresNmlkjihgfed() {
        SfntFont font = GsubChainUndecaBacktrackSampleFont.create();
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
        int n = font.glyphId('N');
        int y = font.glyphId('Y');
        assertEquals(GsubChainUndecaBacktrackSampleFont.GLYPH_N, n);
        assertEquals(GsubChainUndecaBacktrackSampleFont.GLYPH_Y, y);
        assertEquals(a, font.chainSubstitute(a, b, c, SfntFont.TAG_CALT));
        assertEquals(
                y,
                font.chainSubstitute(new int[] {n, m, l, k, j, i, h, g, f, e, d, a, b, c}, 11, 3, SfntFont.TAG_CALT)
        );
        assertEquals(a, font.chainSubstitute(new int[] {m, l, k, j, i, h, g, f, e, d, a, b, c}, 10, 3, SfntFont.TAG_CALT));
        assertEquals(a, font.chainSubstitute(new int[] {n, l, k, j, i, h, g, f, e, d, a, b, c}, 10, 3, SfntFont.TAG_CALT));
    }
}

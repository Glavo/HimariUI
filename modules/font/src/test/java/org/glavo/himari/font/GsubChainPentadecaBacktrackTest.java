package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Verifies fifteen-glyph GSUB chain backtrack through [`SfntFont#chainSubstitute(int[], int, int, int)`].
@NotNullByDefault
final class GsubChainPentadecaBacktrackTest {
    /// Substitutes `A` only when format-1 backtrack `RQPONMLKJIHGFED` and lookahead `BC` are present.
    @Test
    void format1BacktrackRequiresRqponmlkjihgfed() {
        SfntFont font = GsubChainPentadecaBacktrackSampleFont.create();
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
        int o = font.glyphId('O');
        int p = font.glyphId('P');
        int q = font.glyphId('Q');
        int r = font.glyphId('R');
        int y = font.glyphId('Y');
        assertEquals(GsubChainPentadecaBacktrackSampleFont.GLYPH_R, r);
        assertEquals(GsubChainPentadecaBacktrackSampleFont.GLYPH_Y, y);
        assertEquals(a, font.chainSubstitute(a, b, c, SfntFont.TAG_CALT));
        assertEquals(
                y,
                font.chainSubstitute(new int[] {r, q, p, o, n, m, l, k, j, i, h, g, f, e, d, a, b, c}, 15, 3, SfntFont.TAG_CALT)
        );
        assertEquals(a, font.chainSubstitute(new int[] {q, p, o, n, m, l, k, j, i, h, g, f, e, d, a, b, c}, 14, 3, SfntFont.TAG_CALT));
        assertEquals(a, font.chainSubstitute(new int[] {r, p, o, n, m, l, k, j, i, h, g, f, e, d, a, b, c}, 14, 3, SfntFont.TAG_CALT));
    }
}

package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Verifies twenty-nine-glyph GSUB chain backtrack through [`SfntFont#chainSubstitute(int[], int, int, int)`].
@NotNullByDefault
final class GsubChainEnneacosaBacktrackTest {
    /// Substitutes `A` only when format-1 backtrack `654321ZYXWVUTSRQPONMLKJIHGFED` and lookahead `BC` are present.
    @Test
    void format1BacktrackRequiresSixFiveFourThreeTwoOneZyxwvutsrqponmlkjihgfed() {
        SfntFont font = GsubChainEnneacosaBacktrackSampleFont.create();
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
        int s = font.glyphId('S');
        int t = font.glyphId('T');
        int u = font.glyphId('U');
        int v = font.glyphId('V');
        int w = font.glyphId('W');
        int x = font.glyphId('X');
        int y = font.glyphId('Y');
        int z = font.glyphId('Z');
        int zero = font.glyphId('0');
        int one = font.glyphId('1');
        int two = font.glyphId('2');
        int three = font.glyphId('3');
        int four = font.glyphId('4');
        int five = font.glyphId('5');
        int six = font.glyphId('6');
        assertEquals(GsubChainEnneacosaBacktrackSampleFont.GLYPH_Y, y);
        assertEquals(GsubChainEnneacosaBacktrackSampleFont.GLYPH_Z, z);
        assertEquals(GsubChainEnneacosaBacktrackSampleFont.GLYPH_ZERO, zero);
        assertEquals(GsubChainEnneacosaBacktrackSampleFont.GLYPH_ONE, one);
        assertEquals(GsubChainEnneacosaBacktrackSampleFont.GLYPH_TWO, two);
        assertEquals(GsubChainEnneacosaBacktrackSampleFont.GLYPH_THREE, three);
        assertEquals(GsubChainEnneacosaBacktrackSampleFont.GLYPH_FOUR, four);
        assertEquals(GsubChainEnneacosaBacktrackSampleFont.GLYPH_FIVE, five);
        assertEquals(GsubChainEnneacosaBacktrackSampleFont.GLYPH_SIX, six);
        assertEquals(a, font.chainSubstitute(a, b, c, SfntFont.TAG_CALT));
        assertEquals(
                zero,
                font.chainSubstitute(
                        new int[] {six, five, four, three, two, one, z, y, x, w, v, u, t, s, r, q, p, o, n, m, l, k, j, i, h, g, f, e, d, a, b, c},
                        29,
                        3,
                        SfntFont.TAG_CALT
                )
        );
        assertEquals(
                a,
                font.chainSubstitute(
                        new int[] {five, four, three, two, one, z, y, x, w, v, u, t, s, r, q, p, o, n, m, l, k, j, i, h, g, f, e, d, a, b, c},
                        28,
                        3,
                        SfntFont.TAG_CALT
                )
        );
        assertEquals(
                a,
                font.chainSubstitute(
                        new int[] {six, four, three, two, one, z, y, x, w, v, u, t, s, r, q, p, o, n, m, l, k, j, i, h, g, f, e, d, a, b, c},
                        28,
                        3,
                        SfntFont.TAG_CALT
                )
        );
    }
}

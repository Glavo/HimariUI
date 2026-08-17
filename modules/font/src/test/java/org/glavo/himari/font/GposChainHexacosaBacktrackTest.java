package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Verifies twenty-six-glyph GPOS chain backtrack through [`SfntFont#chainAdjustment(int[], int, int)`].
@NotNullByDefault
final class GposChainHexacosaBacktrackTest {
    /// Applies the format-1 chain only when backtrack `321ZYXWVUTSRQPONMLKJIHGFED` precedes `ABC`.
    @Test
    void format1BacktrackRequiresThreeTwoOneZyxwvutsrqponmlkjihgfed() {
        SfntFont font = GposChainHexacosaBacktrackSampleFont.create();
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
        int one = font.glyphId('1');
        int two = font.glyphId('2');
        int three = font.glyphId('3');
        assertEquals(GposChainHexacosaBacktrackSampleFont.GLYPH_Y, y);
        assertEquals(GposChainHexacosaBacktrackSampleFont.GLYPH_Z, z);
        assertEquals(GposChainHexacosaBacktrackSampleFont.GLYPH_ONE, one);
        assertEquals(GposChainHexacosaBacktrackSampleFont.GLYPH_TWO, two);
        assertEquals(GposChainHexacosaBacktrackSampleFont.GLYPH_THREE, three);
        assertEquals(0, font.chainAdjustment(a, b, c));
        assertEquals(
                GposChainHexacosaBacktrackSampleFont.CHAIN_DELTA,
                font.chainAdjustment(
                        new int[] {three, two, one, z, y, x, w, v, u, t, s, r, q, p, o, n, m, l, k, j, i, h, g, f, e, d, a, b, c},
                        26,
                        3
                )
        );
        assertEquals(
                0,
                font.chainAdjustment(new int[] {two, one, z, y, x, w, v, u, t, s, r, q, p, o, n, m, l, k, j, i, h, g, f, e, d, a, b, c}, 25, 3)
        );
        assertEquals(
                0,
                font.chainAdjustment(new int[] {three, one, z, y, x, w, v, u, t, s, r, q, p, o, n, m, l, k, j, i, h, g, f, e, d, a, b, c}, 25, 3)
        );
    }
}

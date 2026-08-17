package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Verifies thirty-seven-glyph GPOS chain backtrack through [`SfntFont#chainAdjustment(int[], int, int)`].
@NotNullByDefault
final class GposChainHeptatriacontaBacktrackTest {
    /// Applies the format-1 chain only when backtrack `>=<;:987654321ZYXWVUTSRQPONMLKJIHGFED` precedes `ABC`.
    @Test
    void format1BacktrackRequiresGreaterEqualLessSemicolonColonNineEightSevenSixFiveFourThreeTwoOneZyxwvutsrqponmlkjihgfed() {
        SfntFont font = GposChainHeptatriacontaBacktrackSampleFont.create();
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
        int four = font.glyphId('4');
        int five = font.glyphId('5');
        int six = font.glyphId('6');
        int seven = font.glyphId('7');
        int eight = font.glyphId('8');
        int nine = font.glyphId('9');
        int colon = font.glyphId(':');
        int semicolon = font.glyphId(';');
        int less = font.glyphId('<');
        int equal = font.glyphId('=');
        int greater = font.glyphId('>');
        assertEquals(GposChainHeptatriacontaBacktrackSampleFont.GLYPH_Y, y);
        assertEquals(GposChainHeptatriacontaBacktrackSampleFont.GLYPH_Z, z);
        assertEquals(GposChainHeptatriacontaBacktrackSampleFont.GLYPH_ONE, one);
        assertEquals(GposChainHeptatriacontaBacktrackSampleFont.GLYPH_TWO, two);
        assertEquals(GposChainHeptatriacontaBacktrackSampleFont.GLYPH_THREE, three);
        assertEquals(GposChainHeptatriacontaBacktrackSampleFont.GLYPH_FOUR, four);
        assertEquals(GposChainHeptatriacontaBacktrackSampleFont.GLYPH_FIVE, five);
        assertEquals(GposChainHeptatriacontaBacktrackSampleFont.GLYPH_SIX, six);
        assertEquals(GposChainHeptatriacontaBacktrackSampleFont.GLYPH_SEVEN, seven);
        assertEquals(GposChainHeptatriacontaBacktrackSampleFont.GLYPH_EIGHT, eight);
        assertEquals(GposChainHeptatriacontaBacktrackSampleFont.GLYPH_NINE, nine);
        assertEquals(GposChainHeptatriacontaBacktrackSampleFont.GLYPH_COLON, colon);
        assertEquals(GposChainHeptatriacontaBacktrackSampleFont.GLYPH_SEMICOLON, semicolon);
        assertEquals(GposChainHeptatriacontaBacktrackSampleFont.GLYPH_LESS, less);
        assertEquals(GposChainHeptatriacontaBacktrackSampleFont.GLYPH_EQUAL, equal);
        assertEquals(GposChainHeptatriacontaBacktrackSampleFont.GLYPH_GREATER, greater);
        assertEquals(0, font.chainAdjustment(a, b, c));
        assertEquals(
                GposChainHeptatriacontaBacktrackSampleFont.CHAIN_DELTA,
                font.chainAdjustment(
                        new int[] {greater, equal, less, semicolon, colon, nine, eight, seven, six, five, four, three, two, one, z, y, x, w, v, u, t, s, r, q, p, o, n, m, l, k, j, i, h, g, f, e, d, a, b, c},
                        37,
                        3
                )
        );
        assertEquals(
                0,
                font.chainAdjustment(new int[] {equal, less, semicolon, colon, nine, eight, seven, six, five, four, three, two, one, z, y, x, w, v, u, t, s, r, q, p, o, n, m, l, k, j, i, h, g, f, e, d, a, b, c}, 36, 3)
        );
        assertEquals(
                0,
                font.chainAdjustment(new int[] {greater, less, semicolon, colon, nine, eight, seven, six, five, four, three, two, one, z, y, x, w, v, u, t, s, r, q, p, o, n, m, l, k, j, i, h, g, f, e, d, a, b, c}, 36, 3)
        );
    }
}

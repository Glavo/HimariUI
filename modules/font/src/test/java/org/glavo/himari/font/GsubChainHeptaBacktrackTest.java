package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Verifies seven-glyph GSUB chain backtrack through [`SfntFont#chainSubstitute(int[], int, int, int)`].
@NotNullByDefault
final class GsubChainHeptaBacktrackTest {
    /// Substitutes `A` only when format-1 backtrack `JIHGFED` and lookahead `BC` are present.
    @Test
    void format1BacktrackRequiresJihgfed() {
        SfntFont font = GsubChainHeptaBacktrackSampleFont.create();
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
        int y = font.glyphId('Y');
        assertEquals(GsubChainHeptaBacktrackSampleFont.GLYPH_J, j);
        assertEquals(a, font.chainSubstitute(a, b, c, SfntFont.TAG_CALT));
        assertEquals(y, font.chainSubstitute(new int[] {j, i, h, g, f, e, d, a, b, c}, 7, 3, SfntFont.TAG_CALT));
        assertEquals(a, font.chainSubstitute(new int[] {i, h, g, f, e, d, a, b, c}, 6, 3, SfntFont.TAG_CALT));
        assertEquals(a, font.chainSubstitute(new int[] {j, h, g, f, e, d, a, b, c}, 6, 3, SfntFont.TAG_CALT));
    }
}

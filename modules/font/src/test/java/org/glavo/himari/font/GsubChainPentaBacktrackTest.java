package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Verifies five-glyph GSUB chain backtrack through [`SfntFont#chainSubstitute(int[], int, int, int)`].
@NotNullByDefault
final class GsubChainPentaBacktrackTest {
    /// Substitutes `A` only when format-1 backtrack `HGFED` and lookahead `BC` are present.
    @Test
    void format1BacktrackRequiresHgfed() {
        SfntFont font = GsubChainPentaBacktrackSampleFont.create();
        int a = font.glyphId('A');
        int b = font.glyphId('B');
        int c = font.glyphId('C');
        int d = font.glyphId('D');
        int e = font.glyphId('E');
        int f = font.glyphId('F');
        int g = font.glyphId('G');
        int h = font.glyphId('H');
        int y = font.glyphId('Y');
        assertEquals(GsubChainPentaBacktrackSampleFont.GLYPH_H, h);
        assertEquals(a, font.chainSubstitute(a, b, c, SfntFont.TAG_CALT));
        assertEquals(y, font.chainSubstitute(new int[] {h, g, f, e, d, a, b, c}, 5, 3, SfntFont.TAG_CALT));
        assertEquals(a, font.chainSubstitute(new int[] {g, f, e, d, a, b, c}, 4, 3, SfntFont.TAG_CALT));
        assertEquals(a, font.chainSubstitute(new int[] {h, f, e, d, a, b, c}, 4, 3, SfntFont.TAG_CALT));
    }
}

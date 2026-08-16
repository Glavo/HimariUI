package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Verifies four-glyph GSUB chain backtrack through [`SfntFont#chainSubstitute(int[], int, int, int)`].
@NotNullByDefault
final class GsubChainQuadBacktrackTest {
    /// Substitutes `A` only when format-1 backtrack `GFED` and lookahead `BC` are present.
    @Test
    void format2ClassBacktrackRequiresGfed() {
        SfntFont font = GsubChainQuadBacktrackSampleFont.create();
        int a = font.glyphId('A');
        int b = font.glyphId('B');
        int c = font.glyphId('C');
        int d = font.glyphId('D');
        int e = font.glyphId('E');
        int f = font.glyphId('F');
        int g = font.glyphId('G');
        int y = font.glyphId('Y');
        assertEquals(a, font.chainSubstitute(a, b, c, SfntFont.TAG_CALT));
        assertEquals(y, font.chainSubstitute(new int[] {g, f, e, d, a, b, c}, 4, 3, SfntFont.TAG_CALT));
        assertEquals(a, font.chainSubstitute(new int[] {f, e, d, a, b, c}, 3, 3, SfntFont.TAG_CALT));
        assertEquals(a, font.chainSubstitute(new int[] {g, e, d, a, b, c}, 3, 3, SfntFont.TAG_CALT));
    }
}

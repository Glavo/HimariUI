package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Verifies three-glyph GSUB chain backtrack through [`SfntFont#chainSubstitute(int[], int, int, int)`].
@NotNullByDefault
final class GsubChainTripleBacktrackTest {
    /// Substitutes `A` only when backtrack `FED` and lookahead `BC` are present.
    @Test
    void format2ClassBacktrackRequiresFed() {
        SfntFont font = GsubChainTripleBacktrackSampleFont.create();
        int a = font.glyphId('A');
        int b = font.glyphId('B');
        int c = font.glyphId('C');
        int d = font.glyphId('D');
        int e = font.glyphId('E');
        int f = font.glyphId('F');
        int y = font.glyphId('Y');
        assertEquals(a, font.chainSubstitute(a, b, c, SfntFont.TAG_CALT));
        assertEquals(y, font.chainSubstitute(new int[] {f, e, d, a, b, c}, 3, 3, SfntFont.TAG_CALT));
        assertEquals(a, font.chainSubstitute(new int[] {e, d, a, b, c}, 2, 3, SfntFont.TAG_CALT));
        assertEquals(a, font.chainSubstitute(new int[] {f, d, a, b, c}, 2, 3, SfntFont.TAG_CALT));
    }
}

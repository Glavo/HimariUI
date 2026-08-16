package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Verifies two-glyph GSUB chain backtrack through [`SfntFont#chainSubstitute(int[], int, int, int)`].
@NotNullByDefault
final class GsubChainDoubleBacktrackTest {
    /// Substitutes `A` only when backtrack `ED` and lookahead `BC` are present.
    @Test
    void format2ClassBacktrackRequiresEd() {
        SfntFont font = GsubChainDoubleBacktrackSampleFont.create();
        int a = font.glyphId('A');
        int b = font.glyphId('B');
        int c = font.glyphId('C');
        int d = font.glyphId('D');
        int e = font.glyphId('E');
        int y = font.glyphId('Y');
        assertEquals(a, font.chainSubstitute(a, b, c, SfntFont.TAG_CALT));
        assertEquals(y, font.chainSubstitute(new int[] {e, d, a, b, c}, 2, 3, SfntFont.TAG_CALT));
        assertEquals(a, font.chainSubstitute(new int[] {d, a, b, c}, 1, 3, SfntFont.TAG_CALT));
        assertEquals(a, font.chainSubstitute(new int[] {e, a, b, c}, 1, 3, SfntFont.TAG_CALT));
    }
}

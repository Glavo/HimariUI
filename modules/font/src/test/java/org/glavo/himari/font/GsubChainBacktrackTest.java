package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Verifies GSUB type-6 format-2 class backtrack through [`SfntFont#chainSubstitute(int[], int, int, int)`].
@NotNullByDefault
final class GsubChainBacktrackTest {
    /// Substitutes `A` only when backtrack `D` and lookahead `BC` are present.
    @Test
    void format2ClassBacktrackRequiresPrecedingD() {
        SfntFont font = GsubChainBacktrackSampleFont.create();
        int a = font.glyphId('A');
        int b = font.glyphId('B');
        int c = font.glyphId('C');
        int d = font.glyphId('D');
        int y = font.glyphId('Y');
        assertEquals(a, font.chainSubstitute(a, b, c, SfntFont.TAG_CALT));
        assertEquals(y, font.chainSubstitute(new int[] {d, a, b, c}, 1, 3, SfntFont.TAG_CALT));
        assertEquals(a, font.chainSubstitute(new int[] {a, b, c}, 0, 3, SfntFont.TAG_CALT));
        assertEquals(a, font.chainSubstitute(new int[] {b, a, b, c}, 1, 3, SfntFont.TAG_CALT));
    }
}

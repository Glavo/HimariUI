package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Verifies GSUB type-8 reverse backtrack through [`SfntFont#reverseSubstitute(int[], int, int, int)`].
@NotNullByDefault
final class GsubReverseBacktrackTest {
    /// Substitutes `A` only when backtrack `B` and lookahead `C` are both present.
    @Test
    void reverseBacktrackRequiresPrecedingB() {
        SfntFont font = GsubReverseBacktrackSampleFont.create();
        int a = font.glyphId('A');
        int b = font.glyphId('B');
        int c = font.glyphId('C');
        int x = font.glyphId('X');
        assertEquals(a, font.reverseSubstitute(a, c, SfntFont.TAG_CALT));
        assertEquals(x, font.reverseSubstitute(new int[] {b, a, c}, 1, 2, SfntFont.TAG_CALT));
        assertEquals(a, font.reverseSubstitute(new int[] {a, c}, 0, 2, SfntFont.TAG_CALT));
    }

    /// Substitutes `A` only when two-glyph backtrack `ED` and lookahead `C` are present.
    @Test
    void reverseDoubleBacktrackRequiresEd() {
        SfntFont font = GsubReverseDoubleBacktrackSampleFont.create();
        int a = font.glyphId('A');
        int c = font.glyphId('C');
        int d = font.glyphId('D');
        int e = font.glyphId('E');
        int x = font.glyphId('X');
        assertEquals(x, font.reverseSubstitute(new int[] {e, d, a, c}, 2, 2, SfntFont.TAG_CALT));
        assertEquals(a, font.reverseSubstitute(new int[] {d, a, c}, 1, 2, SfntFont.TAG_CALT));
        assertEquals(a, font.reverseSubstitute(a, c, SfntFont.TAG_CALT));
    }
}

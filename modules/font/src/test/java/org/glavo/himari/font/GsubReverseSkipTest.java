package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Verifies GSUB type-8 `IgnoreMarks` through [`SfntFont#reverseSubstitute(int[], int, int, int)`].
@NotNullByDefault
final class GsubReverseSkipTest {
    /// Substitutes `A` when lookahead `C` is reached across mark `B`.
    @Test
    void reverseIgnoreMarksSkipsLookaheadMark() {
        SfntFont font = GsubReverseSkipSampleFont.create();
        int a = font.glyphId('A');
        int b = font.glyphId('B');
        int c = font.glyphId('C');
        int x = font.glyphId('X');
        assertEquals(GdefTable.CLASS_MARK, font.glyphClass(b));
        assertEquals(a, font.reverseSubstitute(a, b, SfntFont.TAG_CALT));
        assertEquals(x, font.reverseSubstitute(a, c, SfntFont.TAG_CALT));
        assertEquals(x, font.reverseSubstitute(new int[] {a, b, c}, 0, 3, SfntFont.TAG_CALT));
        assertEquals(a, font.reverseSubstitute(new int[] {a, b}, 0, 2, SfntFont.TAG_CALT));
    }
}

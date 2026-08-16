package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies GDEF MarkGlyphSets and `UseMarkFilteringSet` through [`SfntFont`] entries.
@NotNullByDefault
final class GdefMarkSetTest {
    /// Skips marks outside set 0 and keeps marks inside it.
    @Test
    void markFilterSetSkipsUncoveredMark() {
        SfntFont font = GdefMarkSetSampleFont.create();
        int a = font.glyphId('A');
        int b = font.glyphId('B');
        int c = font.glyphId('C');
        int d = font.glyphId('D');
        int x = font.glyphId('X');
        assertTrue(font.isGdefMark(b));
        assertTrue(font.isGdefMark(d));
        assertTrue(font.inMarkSet(b, 0));
        assertFalse(font.inMarkSet(d, 0));
        assertEquals(0, font.pairAdjustment(a, c));
        assertEquals(0, font.pairAdjustment(new int[] {a, b, c}, 0, 3));
        assertEquals(GdefMarkSetSampleFont.PAIR_DELTA, font.pairAdjustment(new int[] {a, d, c}, 0, 3));
        assertEquals(a, font.contextSubstitute(new int[] {a, b, c}, 0, 3, SfntFont.TAG_CALT));
        assertEquals(x, font.contextSubstitute(new int[] {a, d, c}, 0, 3, SfntFont.TAG_CALT));
    }
}

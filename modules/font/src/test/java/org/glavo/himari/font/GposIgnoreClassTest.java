package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies GPOS `IgnoreBaseGlyphs` and `IgnoreLigatures` through [`SfntFont`] entries.
@NotNullByDefault
final class GposIgnoreClassTest {
    /// Applies `AC` across a base and across a ligature, but not the other way around.
    @Test
    void ignoreBaseAndLigatureSkipMatchingClasses() {
        SfntFont font = GposIgnoreClassSampleFont.create();
        int a = font.glyphId('A');
        int b = font.glyphId('B');
        int c = font.glyphId('C');
        int d = font.glyphId('D');
        assertTrue(font.isGdefBase(b));
        assertTrue(font.isGdefLigature(d));
        assertEquals(0, font.pairAdjustment(a, c));
        assertEquals(
                GposIgnoreClassSampleFont.BASE_DELTA,
                font.pairAdjustment(new int[] {a, b, c}, 0, 3)
        );
        assertEquals(
                GposIgnoreClassSampleFont.LIGA_DELTA,
                font.pairAdjustment(new int[] {a, d, c}, 0, 3)
        );
        assertEquals(0, font.pairAdjustment(new int[] {a, b}, 0, 2));
        assertEquals(
                GposIgnoreClassSampleFont.BASE_DELTA + GposIgnoreClassSampleFont.LIGA_DELTA,
                font.pairAdjustment(new int[] {a, c}, 0, 2)
        );
    }
}

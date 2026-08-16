package org.glavo.himari.text;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/// Verifies paragraph-LTR visual reordering for Hebrew/Arabic runs.
@NotNullByDefault
final class BidiOrderTest {
    /// Leaves a Latin string unchanged.
    @Test
    void latinRemainsLogical() {
        assertEquals("Hello", BidiOrder.visual("Hello"));
        assertEquals(BidiOrder.LTR, BidiOrder.level('A'));
    }

    /// Reverses a Hebrew run.
    @Test
    void hebrewRunReverses() {
        assertEquals("בא", BidiOrder.visual("אב"));
        assertEquals(BidiOrder.RTL, BidiOrder.level('\u05D0'));
    }

    /// Reorders a mixed Latin/Hebrew line under a paragraph-LTR base.
    @Test
    void mixedLineReordersRtlRun() {
        assertEquals("abבאcd", BidiOrder.visual("abאבcd"));
    }

    /// Returns the input instance when the paragraph has no RTL letters.
    @Test
    void latinVisualReusesInput() {
        String latin = "Hello";
        assertSame(latin, BidiOrder.visual(latin));
        assertSame("", BidiOrder.visual(""));
        assertArrayEquals(new int[] {0, 0, 0, 0, 0}, BidiOrder.levels(latin));
        assertSame(BidiOrder.levels(""), BidiOrder.levels(""));
    }

    /// Assigns paragraph-LTR to neutrals whose sides disagree.
    @Test
    void neutralsTakeEmbeddingWhenSidesDisagree() {
        assertEquals("בא.", BidiOrder.visual("אב."));
        assertEquals("ab בא cd", BidiOrder.visual("ab אב cd"));
        assertArrayEquals(
                new int[] {0, 0, 0, 1, 1, 0, 0, 0},
                BidiOrder.levels("ab אב cd")
        );
    }

    /// An RTL isolate makes a leading neutral take RTL (N2) and is omitted from visual order.
    @Test
    void rtlIsolateFlipsLeadingNeutralAndStripsControls() {
        assertEquals(".ab", BidiOrder.visual(".ab"));
        assertEquals("ab.", BidiOrder.visual("\u2067.ab\u2069"));
        assertEquals("abבאcd", BidiOrder.visual("ab\u2067\u05D0\u05D1\u2069cd"));
        assertEquals("ab.", BidiOrder.visual("\u202B.ab\u202C"));
        assertEquals(".ab", BidiOrder.visual("\u2068.ab\u2069"));
    }

    /// Reverses RTL slots of an index array using decoded code-point levels.
    @Test
    void reordersIndexArrayForRtlRuns() {
        int[] points = {'a', 'b', 0x05D0, 0x05D1, 'c'};
        int[] levels = BidiOrder.levels(points);
        int[] order = {0, 1, 2, 3, 4};
        BidiOrder.reorderRtlRuns(order, levels);
        assertArrayEquals(new int[] {0, 1, 3, 2, 4}, order);
    }
}

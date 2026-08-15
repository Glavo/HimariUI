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
}

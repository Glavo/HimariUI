package org.glavo.himari.text;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}

package org.glavo.himari.text;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies Unicode Hangul LV/LVT arithmetic.
@NotNullByDefault
final class HangulSyllableTest {
    /// Composes 가 and 각 from modern jamo.
    @Test
    void composesGaAndGag() {
        assertEquals(0xAC00, HangulSyllable.compose(0x1100, 0x1161, 0));
        assertEquals(0xAC01, HangulSyllable.compose(0x1100, 0x1161, 0x11A8));
        assertTrue(HangulSyllable.isLead(0x1100));
        assertTrue(HangulSyllable.isVowel(0x1161));
        assertTrue(HangulSyllable.isTrail(0x11A8));
        assertEquals(0, HangulSyllable.compose('A', 0x1161, 0));
    }
}

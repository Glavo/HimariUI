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

    /// Maps Hangul Compatibility Jamo onto modern L/V/T before composition.
    @Test
    void remapsCompatibilityJamo() {
        assertEquals(0x1100, HangulSyllable.asLead(0x3131));
        assertEquals(0x1161, HangulSyllable.asVowel(0x314F));
        assertEquals(0x11A8, HangulSyllable.asTrail(0x3131));
        assertEquals(0xAC00, HangulSyllable.compose(
                HangulSyllable.asLead(0x3131),
                HangulSyllable.asVowel(0x314F),
                0
        ));
        assertEquals(0xAC01, HangulSyllable.compose(
                HangulSyllable.asLead(0x3131),
                HangulSyllable.asVowel(0x314F),
                HangulSyllable.asTrail(0x3131)
        ));
        assertTrue(HangulSyllable.isCompatibility(0x3131));
        assertEquals(0, HangulSyllable.asLead(0x3133));
    }

    /// Decomposes 가 and 각 back into modern L/V/T.
    @Test
    void decomposesGaAndGag() {
        assertTrue(HangulSyllable.isSyllable(0xAC00));
        int[] ga = HangulSyllable.decompose(0xAC00);
        assertEquals(2, ga.length);
        assertEquals(0x1100, ga[0]);
        assertEquals(0x1161, ga[1]);
        int[] gag = HangulSyllable.decompose(0xAC01);
        assertEquals(3, gag.length);
        assertEquals(0x1100, gag[0]);
        assertEquals(0x1161, gag[1]);
        assertEquals(0x11A8, gag[2]);
    }
}

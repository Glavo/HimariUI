package org.glavo.himari.text;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies Thai and Lao SARA AM decomposition and Nikhahit reordering.
@NotNullByDefault
final class ThaiLaoTest {
    /// Leaves a string without SARA AM unchanged.
    @Test
    void leavesNonSaraAmUnchanged() {
        assertNull(ThaiLao.expand(new int[] {0x0E01, 0x0E40}, 2));
        assertTrue(ThaiLao.isLeftVowel(0x0E40));
        assertTrue(ThaiLao.isLeftVowel(0x0EC0));
    }

    /// Decomposes isolated SARA AM after a consonant.
    @Test
    void decomposesThaiSaraAm() {
        ThaiLao.Expansion expansion = ThaiLao.expand(new int[] {0x0E01, 0x0E33}, 2);
        assertNotNull(expansion);
        assertEquals(3, expansion.count());
        assertArrayEquals(new int[] {0x0E01, 0x0E4D, 0x0E32}, copy(expansion.points(), 3));
        assertArrayEquals(new int[] {0, 0, 0}, copy(expansion.clusters(), 3));
    }

    /// Moves Nikhahit left over MAI TRI, matching the Uniscribe example.
    @Test
    void reordersNikhahitOverAboveMark() {
        ThaiLao.Expansion expansion = ThaiLao.expand(new int[] {0x0E14, 0x0E4B, 0x0E33}, 3);
        assertNotNull(expansion);
        assertEquals(4, expansion.count());
        assertArrayEquals(new int[] {0x0E14, 0x0E4D, 0x0E4B, 0x0E32}, copy(expansion.points(), 4));
        assertArrayEquals(new int[] {0, 0, 0, 0}, copy(expansion.clusters(), 4));
    }

    /// Decomposes Lao SARA AM the same way, offset by `0x80`.
    @Test
    void decomposesLaoSaraAm() {
        ThaiLao.Expansion expansion = ThaiLao.expand(new int[] {0x0E81, 0x0EB3}, 2);
        assertNotNull(expansion);
        assertEquals(3, expansion.count());
        assertArrayEquals(new int[] {0x0E81, 0x0ECD, 0x0EB2}, copy(expansion.points(), 3));
    }

    /// Composes Lao ho-no and ho-mo ligatures.
    @Test
    void composesLaoHoLigatures() {
        assertEquals(0x0EDC, ThaiLao.laoLigature(0x0EAB, 0x0E99));
        assertEquals(0x0EDD, ThaiLao.laoLigature(0x0EAB, 0x0EA1));
        assertEquals(0, ThaiLao.laoLigature(0x0EAB, 0x0E81));
    }

    /// Copies the used prefix of an expansion buffer.
    private static int[] copy(int[] values, int count) {
        int[] copy = new int[count];
        System.arraycopy(values, 0, copy, 0, count);
        return copy;
    }
}

package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/// Verifies GSUB type-2, type-3, and type-7 unwrap through [`SfntFont`] entries.
@NotNullByDefault
final class GsubMultipleTest {
    /// Expands `A` through a type-7 wrapper around type-2 `ccmp`.
    @Test
    void extensionUnwrapsMultipleCcmp() {
        SfntFont font = GsubMultipleSampleFont.create();
        int a = font.glyphId('A');
        int @Nullable [] sequence = font.decompose(a, SfntFont.TAG_CCMP);
        assertNotNull(sequence);
        assertArrayEquals(
                new int[] {GsubMultipleSampleFont.GLYPH_X, GsubMultipleSampleFont.GLYPH_Y},
                sequence
        );
        assertNull(font.decompose(a, SfntFont.TAG_AALT));
        assertEquals(a, font.substitute(a, SfntFont.TAG_CCMP));
    }

    /// Picks the first `aalt` alternate for `A`.
    @Test
    void firstAlternateThroughAalt() {
        SfntFont font = GsubMultipleSampleFont.create();
        int a = font.glyphId('A');
        assertEquals(GsubMultipleSampleFont.GLYPH_Z, font.alternate(a, SfntFont.TAG_AALT));
        assertEquals(a, font.alternate(a, SfntFont.TAG_CCMP));
        int x = font.glyphId('X');
        assertEquals(x, font.alternate(x, SfntFont.TAG_AALT));
    }
}

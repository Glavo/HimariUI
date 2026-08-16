package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/// Verifies `sbix` access through [`SfntFont#embeddedBitmap(int)`].
@NotNullByDefault
final class SbixTableTest {
    /// Returns the constructed PNG payload for `A` and nothing for `.notdef`.
    @Test
    void readsPngStrikeForA() {
        SfntFont font = SbixSampleFont.create();
        assertNull(font.embeddedBitmap(0));
        @Nullable EmbeddedBitmap bitmap = font.embeddedBitmap(font.glyphId('A'));
        assertNotNull(bitmap);
        assertEquals(SbixSampleFont.PPEM, bitmap.ppem());
        assertEquals(0, bitmap.originX());
        assertEquals(SbixSampleFont.ORIGIN_Y, bitmap.originY());
        assertEquals(SbixSampleFont.TAG_PNG, bitmap.graphicType());
        assertArrayEquals(SbixSampleFont.PAYLOAD, bitmap.data());
    }
}

package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/// Verifies CBLC/CBDT access through [`SfntFont#colorBitmap(int)`].
@NotNullByDefault
final class CbdtCblcTest {
    /// Returns the constructed 2×2 coverage for `A` and nothing for `.notdef`.
    @Test
    void readsFormat1StrikeForA() {
        SfntFont font = CbdtSampleFont.create();
        assertNull(font.colorBitmap(0));
        @Nullable EmbeddedBitmap bitmap = font.colorBitmap(font.glyphId('A'));
        assertNotNull(bitmap);
        assertEquals(CbdtSampleFont.PPEM, bitmap.ppem());
        assertEquals(CbdtCblc.TAG_CBDT, bitmap.graphicType());
        assertArrayEquals(CbdtSampleFont.PIXELS, bitmap.data());
        assertEquals(2, bitmap.originY());
    }
}

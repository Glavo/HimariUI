package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/// Verifies EBLC/EBDT access through [`SfntFont#grayscaleBitmap(int)`].
@NotNullByDefault
final class EbdtCblcTest {
    /// Returns the constructed 2×2 coverage for `A` and nothing for `.notdef`.
    @Test
    void readsFormat1StrikeForA() {
        SfntFont font = EbdtSampleFont.create();
        assertNull(font.grayscaleBitmap(0));
        assertNull(font.colorBitmap(font.glyphId('A')));
        @Nullable EmbeddedBitmap bitmap = font.grayscaleBitmap(font.glyphId('A'));
        assertNotNull(bitmap);
        assertEquals(EbdtSampleFont.PPEM, bitmap.ppem());
        assertEquals(CbdtCblc.TAG_EBDT, bitmap.graphicType());
        assertArrayEquals(EbdtSampleFont.PIXELS, bitmap.data());
        assertEquals(2, bitmap.originY());
    }
}

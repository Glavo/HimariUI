package org.glavo.himari.graphics;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/// Verifies uncompressed 24-bit BMP encode and decode through the shipped codec.
@NotNullByDefault
final class BmpImageTest {
    /// Encodes a 2x1 RGBA row and decodes opaque BGR pixels back through [`BmpImage`].
    @Test
    void roundTripsTwoByOneRgb() {
        byte[] rgba = {
            (byte) 255, 0, 0, (byte) 255,
            0, (byte) 255, 0, (byte) 128
        };
        byte[] encoded = BmpImage.encode(2, 1, rgba);
        BmpImage.Decoded decoded = BmpImage.decode(encoded);
        assertEquals(2, decoded.width());
        assertEquals(1, decoded.height());
        byte[] expected = {
            (byte) 255, 0, 0, (byte) 255,
            0, (byte) 255, 0, (byte) 255
        };
        assertArrayEquals(expected, decoded.rgba());
    }

    /// Rejects a truncated stream at the shipped decoder.
    @Test
    void rejectsTruncatedStream() {
        assertThrows(IllegalArgumentException.class, () -> BmpImage.decode(new byte[] {0x42, 0x4D, 0x00}));
    }
}

package org.glavo.himari.graphics;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/// Verifies Quite OK Image encode and decode through the shipped codec.
@NotNullByDefault
final class QoiImageTest {
    /// Encodes a 2x2 RGBA square and decodes it back through [`QoiImage`].
    @Test
    void roundTripsTwoByTwoRgba() {
        byte[] rgba = {
            (byte) 255, 0, 0, (byte) 255,
            0, (byte) 255, 0, (byte) 255,
            0, 0, (byte) 255, (byte) 255,
            (byte) 255, (byte) 255, (byte) 255, (byte) 128
        };
        byte[] encoded = QoiImage.encode(2, 2, rgba);
        QoiImage.Decoded decoded = QoiImage.decode(encoded);
        assertEquals(2, decoded.width());
        assertEquals(2, decoded.height());
        assertArrayEquals(rgba, decoded.rgba());
    }

    /// Encodes a run of identical pixels as a QOI RUN chunk and restores them.
    @Test
    void roundTripsRunOfIdenticalPixels() {
        byte[] rgba = new byte[16];
        for (int index = 0; index < 4; index++) {
            rgba[index * 4] = 10;
            rgba[index * 4 + 1] = 20;
            rgba[index * 4 + 2] = 30;
            rgba[index * 4 + 3] = (byte) 255;
        }
        QoiImage.Decoded decoded = QoiImage.decode(QoiImage.encode(2, 2, rgba));
        assertArrayEquals(rgba, decoded.rgba());
    }

    /// Rejects a truncated stream at the shipped decoder.
    @Test
    void rejectsTruncatedStream() {
        assertThrows(IllegalArgumentException.class, () -> QoiImage.decode(new byte[] {1, 2, 3}));
    }
}

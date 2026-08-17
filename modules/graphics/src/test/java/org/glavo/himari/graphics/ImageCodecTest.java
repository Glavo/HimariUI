package org.glavo.himari.graphics;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies JPEG, GIF, WebP, and AVIF providers through [`ImageCodecs`].
@NotNullByDefault
final class ImageCodecTest {
    /// 2x2 unassociated sRGB pixels used by lossless codecs.
    private static final byte[] RGBA = {
            (byte) 255, 0, 0, (byte) 255,
            0, (byte) 255, 0, (byte) 255,
            0, 0, (byte) 255, (byte) 255,
            (byte) 255, (byte) 255, 0, (byte) 128
    };

    /// Encodes and decodes the 2x2 raster through the PNG provider.
    @Test
    void pngRoundTripsThroughProvider() {
        ImageCodec codec = ImageCodecs.png();
        byte[] encoded = codec.encode(PixelBuffer.srgbUnassociated(2, 2, RGBA));
        assertTrue(codec.recognizes(encoded));
        assertArrayEquals(RGBA, codec.decode(encoded).rgba());
    }

    /// Encodes and decodes the 2x2 raster through the JPEG provider.
    @Test
    void jpegRoundTripsThroughProvider() {
        ImageCodec codec = ImageCodecs.jpeg();
        PixelBuffer source = PixelBuffer.srgbUnassociated(2, 2, RGBA);
        byte[] encoded = codec.encode(source);
        assertTrue(codec.recognizes(encoded));
        PixelBuffer decoded = codec.decode(encoded);
        assertEquals(2, decoded.width());
        assertEquals(2, decoded.height());
        byte[] expected = RGBA.clone();
        expected[15] = (byte) 255;
        assertMaxChannelError(expected, decoded.rgba(), 12);
        assertTrue(ImageCodecs.decode(encoded).width() == 2);
    }

    /// Encodes and decodes the 2x2 raster through the GIF provider.
    @Test
    void gifRoundTripsThroughProvider() {
        ImageCodec codec = ImageCodecs.gif();
        byte[] encoded = codec.encode(PixelBuffer.srgbUnassociated(2, 2, RGBA));
        assertTrue(codec.recognizes(encoded));
        PixelBuffer decoded = codec.decode(encoded);
        assertEquals(2, decoded.width());
        assertEquals(2, decoded.height());
        assertEquals((byte) 255, decoded.rgba()[0]);
        assertEquals((byte) 0, decoded.rgba()[1]);
        assertEquals((byte) 0, decoded.rgba()[2]);
        assertEquals((byte) 255, decoded.rgba()[3]);
        assertEquals((byte) 0, decoded.rgba()[4]);
        assertEquals((byte) 255, decoded.rgba()[5]);
    }

    /// Encodes and decodes the 2x2 raster through the lossless WebP provider.
    @Test
    void webpRoundTripsThroughProvider() {
        ImageCodec codec = ImageCodecs.webp();
        byte[] encoded = codec.encode(PixelBuffer.srgbUnassociated(2, 2, RGBA));
        assertTrue(codec.recognizes(encoded));
        assertArrayEquals(RGBA, codec.decode(encoded).rgba());
    }

    /// Encodes and decodes the 2x2 raster through the AVIF provider.
    @Test
    void avifRoundTripsThroughProvider() {
        ImageCodec codec = ImageCodecs.avif();
        byte[] encoded = codec.encode(PixelBuffer.srgbUnassociated(2, 2, RGBA));
        assertTrue(codec.recognizes(encoded));
        assertArrayEquals(RGBA, codec.decode(encoded).rgba());
    }

    /// Rejects a truncated JPEG at the shipped decoder.
    @Test
    void jpegRejectsTruncatedStream() {
        assertThrows(IllegalArgumentException.class, () -> JpegImage.decode(new byte[] {(byte) 0xFF, (byte) 0xD8}));
    }

    /// Rejects a truncated GIF at the shipped decoder.
    @Test
    void gifRejectsTruncatedStream() {
        assertThrows(IllegalArgumentException.class, () -> GifImage.decode(new byte[] {'G', 'I', 'F', '8', '9', 'a'}));
    }

    /// Returns the registered first-stable providers in probe order, PNG first.
    @Test
    void providersAreJpegGifWebpAvif() {
        assertEquals(5, ImageCodecs.providers().size());
        assertEquals("png", ImageCodecs.providers().get(0).name());
        assertEquals("jpeg", ImageCodecs.providers().get(1).name());
        assertEquals("gif", ImageCodecs.providers().get(2).name());
        assertEquals("webp", ImageCodecs.providers().get(3).name());
        assertEquals("avif", ImageCodecs.providers().get(4).name());
    }

    /// Asserts that no 8-bit channel differs by more than `maxError`.
    private static void assertMaxChannelError(byte[] expected, byte[] actual, int maxError) {
        assertEquals(expected.length, actual.length);
        for (int index = 0; index < expected.length; index++) {
            int delta = Math.abs((expected[index] & 0xFF) - (actual[index] & 0xFF));
            assertTrue(delta <= maxError, "channel " + index + " delta " + delta);
        }
    }
}

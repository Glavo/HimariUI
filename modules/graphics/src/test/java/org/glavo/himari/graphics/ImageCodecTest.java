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

    /// Encodes greyscale-alpha PNG and decodes it through [`PngImage#decode(byte[])`].
    @Test
    void pngGreyscaleAlphaRoundTripsThroughShippedDecoder() {
        byte[] rgba = {
                10, 20, 30, (byte) 255,
                40, 50, 60, 0,
                70, 80, 90, (byte) 128,
                1, 2, 3, (byte) 64
        };
        byte[] encoded = PngImage.encodeGreyscaleAlpha(2, 2, rgba);
        byte[] decoded = PngImage.decode(encoded).rgba();
        assertEquals((byte) 20, decoded[0]);
        assertEquals((byte) 20, decoded[1]);
        assertEquals((byte) 20, decoded[2]);
        assertEquals((byte) 255, decoded[3]);
        assertEquals((byte) 50, decoded[4]);
        assertEquals(0, decoded[7]);
        assertEquals((byte) 128, decoded[11]);
        assertEquals(2, ImageCodecs.decode(encoded).width());
    }

    /// Encodes 16-bit RGBA PNG and recovers the high bytes through [`PngImage#decode(byte[])`].
    @Test
    void png16BitRoundTripsThroughShippedDecoder() {
        byte[] encoded = PngImage.encode16(2, 2, RGBA);
        assertArrayEquals(RGBA, PngImage.decode(encoded).rgba());
        assertArrayEquals(RGBA, ImageCodecs.decode(encoded).rgba());
    }

    /// Encodes RGB plus `tRNS` and decodes the transparent key through [`PngImage#decode(byte[])`].
    @Test
    void pngTrnsRoundTripsThroughShippedDecoder() {
        byte[] rgba = {
                (byte) 255, 0, 0, (byte) 255,
                0, (byte) 255, 0, 0,
                0, 0, (byte) 255, (byte) 255,
                (byte) 255, (byte) 255, 0, (byte) 255
        };
        byte[] encoded = PngImage.encodeRgbWithTransparency(2, 2, rgba);
        byte[] decoded = PngImage.decode(encoded).rgba();
        assertEquals((byte) 255, decoded[0]);
        assertEquals((byte) 255, decoded[3]);
        assertEquals(0, decoded[4]);
        assertEquals((byte) 255, decoded[5]);
        assertEquals(0, decoded[7]);
        assertEquals(2, ImageCodecs.decode(encoded).width());
    }

    /// Encodes indexed PNG and expands `PLTE` through [`PngImage#decode(byte[])`].
    @Test
    void pngIndexedRoundTripsThroughShippedDecoder() {
        byte[] encoded = PngImage.encodeIndexed(2, 2, RGBA);
        assertTrue(PngImage.isPng(encoded));
        byte[] decoded = PngImage.decode(encoded).rgba();
        assertEquals((byte) 255, decoded[3]);
        assertEquals(RGBA[0], decoded[0]);
        assertEquals(RGBA[1], decoded[1]);
        assertEquals(RGBA[2], decoded[2]);
        assertEquals(RGBA[4], decoded[4]);
        assertArrayEquals(
                new byte[] {RGBA[0], RGBA[1], RGBA[2], (byte) 255, RGBA[4], RGBA[5], RGBA[6], (byte) 255,
                        RGBA[8], RGBA[9], RGBA[10], (byte) 255, RGBA[12], RGBA[13], RGBA[14], (byte) 255},
                decoded
        );
        assertEquals(2, ImageCodecs.decode(encoded).width());
    }

    /// Encodes Adam7 PNG and decodes it through [`PngImage#decode(byte[])`].
    @Test
    void pngAdam7RoundTripsThroughShippedDecoder() {
        byte[] rgba = new byte[5 * 5 * 4];
        for (int index = 0; index < 25; index++) {
            int dest = index * 4;
            rgba[dest] = (byte) (index * 9);
            rgba[dest + 1] = (byte) (255 - index * 7);
            rgba[dest + 2] = (byte) (index * 3);
            rgba[dest + 3] = (byte) 255;
        }
        byte[] encoded = PngImage.encodeInterlaced(5, 5, rgba);
        assertTrue(PngImage.isPng(encoded));
        assertArrayEquals(rgba, PngImage.decode(encoded).rgba());
        assertArrayEquals(rgba, ImageCodecs.decode(encoded).rgba());
    }

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

    /// Encodes a two-frame GIF and composites both image blocks through [`GifImage#decode(byte[])`].
    @Test
    void gifAnimationCompositesThroughShippedDecoder() {
        byte[] first = {
                (byte) 255, 0, 0, (byte) 255,
                (byte) 255, 0, 0, (byte) 255,
                (byte) 255, 0, 0, (byte) 255,
                (byte) 255, 0, 0, (byte) 255
        };
        byte[] second = {
                0, (byte) 255, 0, (byte) 255,
                0, (byte) 255, 0, (byte) 255,
                0, (byte) 255, 0, (byte) 255,
                0, (byte) 255, 0, (byte) 255
        };
        byte[] encoded = GifImage.encodeAnimated(2, 2, first, second);
        assertTrue(GifImage.isGif(encoded));
        GifImage.Decoded[] frames = GifImage.decodeFrames(encoded);
        assertEquals(2, frames.length);
        assertEquals((byte) 255, frames[0].rgba()[0]);
        assertEquals(0, frames[0].rgba()[1]);
        assertEquals(0, frames[1].rgba()[0]);
        assertEquals((byte) 255, frames[1].rgba()[1]);
        assertArrayEquals(frames[1].rgba(), GifImage.decode(encoded).rgba());
        assertEquals(2, ImageCodecs.decode(encoded).width());
        assertEquals((byte) 255, ImageCodecs.decode(encoded).rgba()[1]);
    }

    /// Encodes disposal-2 GIF and restores the painted rectangle through [`GifImage#decodeFrames(byte[])`].
    @Test
    void gifDisposal2ClearsThroughShippedDecoder() {
        byte[] first = {
                (byte) 255, 0, 0, (byte) 255,
                (byte) 255, 0, 0, (byte) 255,
                (byte) 255, 0, 0, (byte) 255,
                (byte) 255, 0, 0, (byte) 255
        };
        byte[] second = {
                0, 0, (byte) 255, (byte) 255,
                0, 0, (byte) 255, (byte) 255,
                0, 0, (byte) 255, (byte) 255,
                0, 0, (byte) 255, (byte) 255
        };
        byte[] encoded = GifImage.encodeAnimatedClear(2, 2, first, second);
        assertTrue(GifImage.isGif(encoded));
        assertTrue(containsGifDisposal2(encoded));
        GifImage.Decoded[] frames = GifImage.decodeFrames(encoded);
        assertEquals(2, frames.length);
        assertEquals((byte) 255, frames[0].rgba()[0]);
        assertEquals(0, frames[1].rgba()[0]);
        assertEquals((byte) 255, frames[1].rgba()[2]);
        assertEquals(2, ImageCodecs.decode(encoded).width());
    }

    /// Encodes subtract-green VP8L residuals and decodes them through [`WebpImage#decode(byte[])`].
    @Test
    void webpSubtractGreenRoundTripsThroughShippedDecoder() {
        byte[] rgba = {
                10, 20, 30, (byte) 255,
                10, 20, 30, (byte) 255,
                40, 50, 60, (byte) 255,
                40, 50, 60, (byte) 255
        };
        byte[] encoded = WebpImage.encodeSubtractGreen(2, 2, rgba);
        assertTrue(WebpImage.isWebp(encoded));
        assertArrayEquals(rgba, WebpImage.decode(encoded).rgba());
        assertArrayEquals(rgba, ImageCodecs.decode(encoded).rgba());
    }

    /// Encodes a color-cache VP8L stream and decodes it through the shipped decoder.
    @Test
    void webpColorCacheRoundTripsThroughShippedDecoder() {
        byte[] rgba = {
                (byte) 10, 20, 30, (byte) 255,
                (byte) 10, 20, 30, (byte) 255,
                (byte) 40, 50, 60, (byte) 255,
                (byte) 10, 20, 30, (byte) 255
        };
        byte[] encoded = WebpImage.encodeWithColorCache(2, 2, rgba);
        assertArrayEquals(rgba, WebpImage.decode(encoded).rgba());
    }

    /// Encodes a left-predictor VP8L stream and inverts it through [`WebpImage#decode(byte[])`].
    @Test
    void webpPredictorRoundTripsThroughShippedDecoder() {
        byte[] rgba = {
                10, 20, 30, (byte) 255,
                40, 50, 60, (byte) 255,
                70, 80, 90, (byte) 255,
                11, 22, 33, (byte) 255
        };
        byte[] encoded = WebpImage.encodePredictor(2, 2, rgba);
        assertArrayEquals(rgba, WebpImage.decode(encoded).rgba());
        assertArrayEquals(rgba, ImageCodecs.decode(encoded).rgba());
    }

    /// Encodes a color-transform VP8L stream and inverts it through the shipped decoder.
    @Test
    void webpColorTransformRoundTripsThroughShippedDecoder() {
        byte[] rgba = {
                80, 20, 30, (byte) 255,
                90, 40, 50, (byte) 255,
                60, 10, 70, (byte) 255,
                50, 30, 40, (byte) 255
        };
        byte[] encoded = WebpImage.encodeColor(2, 2, rgba);
        assertArrayEquals(rgba, WebpImage.decode(encoded).rgba());
    }

    /// Encodes a two-color indexing VP8L stream and expands it through the shipped decoder.
    @Test
    void webpIndexingRoundTripsThroughShippedDecoder() {
        byte[] rgba = {
                10, 20, 30, (byte) 255,
                40, 50, 60, (byte) 255,
                10, 20, 30, (byte) 255,
                40, 50, 60, (byte) 255
        };
        byte[] encoded = WebpImage.encodeIndexing(2, 2, rgba);
        assertArrayEquals(rgba, WebpImage.decode(encoded).rgba());
    }

    /// Encodes an LZ77 VP8L stream and decodes the backward copy through the shipped decoder.
    @Test
    void webpLz77RoundTripsThroughShippedDecoder() {
        byte[] rgba = {
                (byte) 9, 8, 7, (byte) 255,
                (byte) 9, 8, 7, (byte) 255,
                (byte) 9, 8, 7, (byte) 255,
                (byte) 9, 8, 7, (byte) 255
        };
        byte[] encoded = WebpImage.encodeWithLz77(4, 1, rgba);
        assertArrayEquals(rgba, WebpImage.decode(encoded).rgba());
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

    /// Encodes AV1 §8.2 intra-left `L(8)` residuals and decodes them through [`AvifImage#decode(byte[])`].
    @Test
    void avifEntropyCodedRasterRoundTripsThroughShippedDecoder() {
        byte[] encoded = AvifImage.encode(2, 2, RGBA);
        assertTrue(AvifImage.isAvif(encoded));
        assertEquals(-1, indexOfTag(encoded, 0x48535431));
        assertArrayEquals(RGBA, AvifImage.decode(encoded).rgba());
        assertArrayEquals(RGBA, ImageCodecs.decode(encoded).rgba());
        assertArrayEquals(RGBA, ImageCodecs.avif().decode(encoded).rgba());
    }

    /// Encodes 12-bit SOF1 and decodes it through [`JpegImage#decode(byte[])`].
    @Test
    void jpeg12BitRoundTripsThroughShippedDecoder() {
        byte[] encoded = JpegImage.encode12(2, 2, RGBA);
        assertTrue(JpegImage.isJpeg(encoded));
        assertTrue(containsSof1(encoded));
        byte[] decoded = JpegImage.decode(encoded).rgba();
        assertEquals(RGBA.length, decoded.length);
        assertEquals((byte) 255, decoded[3]);
        assertMaxChannelError(JpegImage.decode(JpegImage.encode(2, 2, RGBA)).rgba(), decoded, 4);
        assertEquals(2, ImageCodecs.decode(encoded).width());
    }

    /// Encodes sequential arithmetic SOF9 and decodes it through [`JpegImage#decode(byte[])`].
    @Test
    void jpegArithmeticRoundTripsThroughShippedDecoder() {
        byte[] encoded = JpegImage.encodeArithmetic(2, 2, RGBA);
        assertTrue(JpegImage.isJpeg(encoded));
        assertTrue(containsSof9(encoded));
        byte[] decoded = JpegImage.decode(encoded).rgba();
        assertEquals(RGBA.length, decoded.length);
        assertEquals((byte) 255, decoded[3]);
        assertMaxChannelError(JpegImage.decode(JpegImage.encode(2, 2, RGBA)).rgba(), decoded, 4);
        assertEquals(2, ImageCodecs.decode(encoded).width());
    }

    /// Encodes progressive SOF2 spectral selection and decodes it through [`JpegImage#decode(byte[])`].
    @Test
    void jpegProgressiveRoundTripsThroughShippedDecoder() {
        byte[] encoded = JpegImage.encodeProgressive(2, 2, RGBA);
        assertTrue(JpegImage.isJpeg(encoded));
        assertTrue(containsSof2(encoded));
        byte[] decoded = JpegImage.decode(encoded).rgba();
        assertEquals(RGBA.length, decoded.length);
        assertEquals((byte) 255, decoded[3]);
        assertMaxChannelError(JpegImage.decode(JpegImage.encode(2, 2, RGBA)).rgba(), decoded, 2);
        assertEquals(2, ImageCodecs.decode(encoded).width());
    }

    /// Encodes SOF2 DC successive approximation and decodes it through [`JpegImage#decode(byte[])`].
    @Test
    void jpegSuccessiveApproximationRoundTripsThroughShippedDecoder() {
        byte[] encoded = JpegImage.encodeSuccessive(2, 2, RGBA);
        assertTrue(JpegImage.isJpeg(encoded));
        assertTrue(containsSof2(encoded));
        byte[] decoded = JpegImage.decode(encoded).rgba();
        assertEquals(RGBA.length, decoded.length);
        assertEquals((byte) 255, decoded[3]);
        assertMaxChannelError(JpegImage.decode(JpegImage.encode(2, 2, RGBA)).rgba(), decoded, 2);
        assertEquals(2, ImageCodecs.decode(encoded).width());
    }

    /// Encodes PNG `iCCP` and recovers the profile through [`PngImage#iccProfile(byte[])`].
    @Test
    void pngIccpRoundTripsThroughShippedDecoder() {
        byte[] profile = IccProfileTest.minimalSrgbMatrixProfile();
        byte[] encoded = PngImage.encodeIccp(2, 2, RGBA, profile);
        assertTrue(PngImage.isPng(encoded));
        byte[] recovered = PngImage.iccProfile(encoded);
        assertTrue(recovered != null);
        assertArrayEquals(profile, recovered);
        assertEquals("RGB ", IccProfile.parse(recovered).deviceColorSpace());
        assertArrayEquals(RGBA, PngImage.decode(encoded).rgba());
        assertEquals(2, ImageCodecs.decode(encoded).width());
    }

    /// Encodes PNG Up and Average filters and decodes them through [`PngImage#decode(byte[])`].
    @Test
    void pngUpAndAverageRoundTripThroughShippedDecoder() {
        byte[] up = PngImage.encodeUp(2, 2, RGBA);
        byte[] average = PngImage.encodeAverage(2, 2, RGBA);
        assertArrayEquals(RGBA, PngImage.decode(up).rgba());
        assertArrayEquals(RGBA, PngImage.decode(average).rgba());
        assertArrayEquals(RGBA, ImageCodecs.decode(up).rgba());
        assertArrayEquals(RGBA, ImageCodecs.decode(average).rgba());
    }

    /// Encodes JPEG APP2 ICC and recovers the profile through [`JpegImage#iccProfile(byte[])`].
    @Test
    void jpegIccProfileRoundTripsThroughShippedDecoder() {
        byte[] profile = IccProfileTest.minimalSrgbMatrixProfile();
        byte[] encoded = JpegImage.encodeWithIcc(2, 2, RGBA, profile);
        assertTrue(JpegImage.isJpeg(encoded));
        byte[] recovered = JpegImage.iccProfile(encoded);
        assertTrue(recovered != null);
        assertArrayEquals(profile, recovered);
        IccProfile parsed = IccProfile.parse(recovered);
        assertEquals("RGB ", parsed.deviceColorSpace());
        assertEquals(2, JpegImage.decode(encoded).width());
        assertEquals(2, ImageCodecs.decode(encoded).width());
    }

    /// Encodes 4:2:2 and decodes it through [`JpegImage#decode(byte[])`].
    @Test
    void jpeg422RoundTripsThroughShippedDecoder() {
        byte[] rgba = new byte[16 * 8 * 4];
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 16; x++) {
                int dest = (y * 16 + x) * 4;
                rgba[dest] = (byte) (x < 8 ? 255 : 0);
                rgba[dest + 1] = 0;
                rgba[dest + 2] = (byte) (x < 8 ? 0 : 255);
                rgba[dest + 3] = (byte) 255;
            }
        }
        byte[] encoded = JpegImage.encode422(16, 8, rgba);
        assertTrue(JpegImage.isJpeg(encoded));
        JpegImage.Decoded decoded = JpegImage.decode(encoded);
        assertEquals(16, decoded.width());
        assertEquals(8, decoded.height());
        byte[] pixels = decoded.rgba();
        assertTrue((pixels[0] & 0xFF) > 200, "left block should stay red");
        assertTrue((pixels[2] & 0xFF) < 40, "left block should not pick up blue");
        int right = (4 * 16 + 12) * 4;
        assertTrue((pixels[right + 2] & 0xFF) > 200, "right block should stay blue");
        assertTrue((pixels[right] & 0xFF) < 40, "right block should not pick up red");
        assertEquals(16, ImageCodecs.decode(encoded).width());
    }

    /// Encodes SOF0 with `DRI`/`RST` and decodes it through [`JpegImage#decode(byte[])`].
    @Test
    void jpegRestartIntervalRoundTripsThroughShippedDecoder() {
        byte[] rgba = new byte[16 * 8 * 4];
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 16; x++) {
                int dest = (y * 16 + x) * 4;
                rgba[dest] = (byte) (x < 8 ? 255 : 40);
                rgba[dest + 1] = (byte) 20;
                rgba[dest + 2] = (byte) (x < 8 ? 10 : 200);
                rgba[dest + 3] = (byte) 255;
            }
        }
        byte[] encoded = JpegImage.encodeWithRestart(16, 8, rgba);
        assertTrue(JpegImage.isJpeg(encoded));
        assertTrue(containsDri(encoded));
        assertTrue(containsRst(encoded));
        JpegImage.Decoded decoded = JpegImage.decode(encoded);
        assertEquals(16, decoded.width());
        assertEquals(8, decoded.height());
        assertMaxChannelError(JpegImage.decode(JpegImage.encode(16, 8, rgba)).rgba(), decoded.rgba(), 4);
        assertEquals(16, ImageCodecs.decode(encoded).width());
    }

    /// Encodes Paeth-filtered PNG and decodes it through [`PngImage#decode(byte[])`].
    @Test
    void pngPaethRoundTripsThroughShippedDecoder() {
        byte[] encoded = PngImage.encodePaeth(2, 2, RGBA);
        assertTrue(PngImage.isPng(encoded));
        assertArrayEquals(RGBA, PngImage.decode(encoded).rgba());
        assertArrayEquals(RGBA, ImageCodecs.decode(encoded).rgba());
    }

    /// Encodes Sub-filtered PNG and decodes it through [`PngImage#decode(byte[])`].
    @Test
    void pngSubRoundTripsThroughShippedDecoder() {
        byte[] encoded = PngImage.encodeSub(2, 2, RGBA);
        assertTrue(PngImage.isPng(encoded));
        assertArrayEquals(RGBA, PngImage.decode(encoded).rgba());
        assertArrayEquals(RGBA, ImageCodecs.decode(encoded).rgba());
    }

    /// Encodes PNG `cICP` Display-P3 and recovers the tagged encoding through [`PngImage#decode(byte[])`].
    @Test
    void pngCicpRoundTripsThroughShippedDecoder() {
        byte[] encoded = PngImage.encodeCicp(2, 2, RGBA, ColorEncoding.DISPLAY_P3);
        assertTrue(PngImage.isPng(encoded));
        PngImage.Decoded decoded = PngImage.decode(encoded);
        assertArrayEquals(RGBA, decoded.rgba());
        assertEquals(ColorEncoding.DISPLAY_P3, decoded.encoding());
        byte[] pq = PngImage.encodeCicp(2, 2, RGBA, ColorEncoding.BT2100_PQ);
        assertEquals(ColorEncoding.BT2100_PQ, PngImage.decode(pq).encoding());
        assertEquals(2, ImageCodecs.decode(encoded).width());
    }

    /// Encodes 4:4:0 and decodes it through [`JpegImage#decode(byte[])`].
    @Test
    void jpeg440RoundTripsThroughShippedDecoder() {
        byte[] rgba = new byte[8 * 16 * 4];
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 8; x++) {
                int dest = (y * 8 + x) * 4;
                rgba[dest] = (byte) (y < 8 ? 255 : 0);
                rgba[dest + 1] = 0;
                rgba[dest + 2] = (byte) (y < 8 ? 0 : 255);
                rgba[dest + 3] = (byte) 255;
            }
        }
        byte[] encoded = JpegImage.encode440(8, 16, rgba);
        assertTrue(JpegImage.isJpeg(encoded));
        JpegImage.Decoded decoded = JpegImage.decode(encoded);
        assertEquals(8, decoded.width());
        assertEquals(16, decoded.height());
        byte[] pixels = decoded.rgba();
        assertTrue((pixels[0] & 0xFF) > 200, "top block should stay red");
        assertTrue((pixels[2] & 0xFF) < 40, "top block should not pick up blue");
        int bottom = (12 * 8 + 4) * 4;
        assertTrue((pixels[bottom + 2] & 0xFF) > 200, "bottom block should stay blue");
        assertTrue((pixels[bottom] & 0xFF) < 40, "bottom block should not pick up red");
        assertEquals(8, ImageCodecs.decode(encoded).width());
    }

    /// Encodes 4:2:0 and decodes it through [`JpegImage#decode(byte[])`].
    @Test
    void jpeg420RoundTripsThroughShippedDecoder() {
        byte[] rgba = new byte[16 * 16 * 4];
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int dest = (y * 16 + x) * 4;
                rgba[dest] = (byte) (x < 8 ? 255 : 0);
                rgba[dest + 1] = 0;
                rgba[dest + 2] = (byte) (x < 8 ? 0 : 255);
                rgba[dest + 3] = (byte) 255;
            }
        }
        byte[] encoded = JpegImage.encode420(16, 16, rgba);
        assertTrue(JpegImage.isJpeg(encoded));
        JpegImage.Decoded decoded = JpegImage.decode(encoded);
        assertEquals(16, decoded.width());
        assertEquals(16, decoded.height());
        byte[] pixels = decoded.rgba();
        assertTrue((pixels[0] & 0xFF) > 200, "left block should stay red");
        assertTrue((pixels[2] & 0xFF) < 40, "left block should not pick up blue");
        int right = (8 * 16 + 12) * 4;
        assertTrue((pixels[right + 2] & 0xFF) > 200, "right block should stay blue");
        assertTrue((pixels[right] & 0xFF) < 40, "right block should not pick up red");
        assertEquals((byte) 255, pixels[3]);
        assertEquals(16, ImageCodecs.decode(encoded).width());
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

    /// Returns whether `bytes` contain a graphic-control packed field with disposal 2.
    private static boolean containsGifDisposal2(byte[] bytes) {
        for (int index = 0; index + 4 < bytes.length; index++) {
            if ((bytes[index] & 0xFF) == 0x21
                    && (bytes[index + 1] & 0xFF) == 0xF9
                    && ((bytes[index + 3] >>> 2) & 0x07) == 2) {
                return true;
            }
        }
        return false;
    }

    /// Returns whether `bytes` contain a SOF1 marker.
    private static boolean containsSof1(byte[] bytes) {
        return containsMarker(bytes, 0xC1);
    }

    /// Returns whether `bytes` contain a DRI marker.
    private static boolean containsDri(byte[] bytes) {
        return containsMarker(bytes, 0xDD);
    }

    /// Returns whether `bytes` contain an `RST0`..`RST7` marker.
    private static boolean containsRst(byte[] bytes) {
        for (int index = 0; index + 1 < bytes.length; index++) {
            if ((bytes[index] & 0xFF) == 0xFF) {
                int next = bytes[index + 1] & 0xFF;
                if (next >= 0xD0 && next <= 0xD7) {
                    return true;
                }
            }
        }
        return false;
    }

    /// Returns whether `bytes` contain marker `0xFF`/`code`.
    private static boolean containsMarker(byte[] bytes, int code) {
        for (int index = 0; index + 1 < bytes.length; index++) {
            if ((bytes[index] & 0xFF) == 0xFF && (bytes[index + 1] & 0xFF) == code) {
                return true;
            }
        }
        return false;
    }

    /// Returns whether `bytes` contain a SOF9 marker.
    private static boolean containsSof9(byte[] bytes) {
        for (int index = 0; index + 1 < bytes.length; index++) {
            if ((bytes[index] & 0xFF) == 0xFF && (bytes[index + 1] & 0xFF) == 0xC9) {
                return true;
            }
        }
        return false;
    }

    /// Returns whether `bytes` contain a SOF2 marker.
    private static boolean containsSof2(byte[] bytes) {
        for (int index = 0; index + 1 < bytes.length; index++) {
            if ((bytes[index] & 0xFF) == 0xFF && (bytes[index + 1] & 0xFF) == 0xC2) {
                return true;
            }
        }
        return false;
    }

    /// Returns the index of big-endian `tag`, or `-1`.
    private static int indexOfTag(byte[] bytes, int tag) {
        for (int index = 0; index + 4 <= bytes.length; index++) {
            int value = (bytes[index] & 0xFF) << 24
                    | (bytes[index + 1] & 0xFF) << 16
                    | (bytes[index + 2] & 0xFF) << 8
                    | (bytes[index + 3] & 0xFF);
            if (value == tag) {
                return index;
            }
        }
        return -1;
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

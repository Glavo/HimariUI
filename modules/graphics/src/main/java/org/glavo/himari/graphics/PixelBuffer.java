package org.glavo.himari.graphics;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Arrays;
import java.util.Objects;

/// Stores one decoded raster independent of `java.awt.image.BufferedImage`.
///
/// Samples are row-major RGBA8. The tagged encoding and alpha interpretation travel with the
/// pixels so a later color conversion cannot treat the buffer as implicit sRGB.
///
/// @param width the positive pixel width
/// @param height the positive pixel height
/// @param rgba unassociated or premultiplied RGBA8 samples
/// @param encoding the tagged color encoding
/// @param alpha how alpha relates to the color channels
@NotNullByDefault
public record PixelBuffer(
        int width,
        int height,
        byte @Unmodifiable [] rgba,
        ColorEncoding encoding,
        AlphaInterpretation alpha
) {
    /// Maximum accepted width or height.
    public static final int MAX_EDGE = 16_384;

    /// Validates the buffer.
    public PixelBuffer {
        if (width <= 0 || height <= 0 || width > MAX_EDGE || height > MAX_EDGE) {
            throw new IllegalArgumentException("Pixel extents must be in (0, " + MAX_EDGE + "]");
        }
        Objects.requireNonNull(rgba, "rgba");
        Objects.requireNonNull(encoding, "encoding");
        Objects.requireNonNull(alpha, "alpha");
        if (rgba.length != Math.multiplyExact(width, height) * 4) {
            throw new IllegalArgumentException("RGBA length must be width * height * 4");
        }
        rgba = Arrays.copyOf(rgba, rgba.length);
    }

    /// Creates an unassociated sRGB buffer.
    ///
    /// @param width the width
    /// @param height the height
    /// @param rgba RGBA8 samples
    /// @return the buffer
    public static PixelBuffer srgbUnassociated(int width, int height, byte[] rgba) {
        return new PixelBuffer(width, height, rgba, ColorEncoding.SRGB, AlphaInterpretation.UNASSOCIATED);
    }

    /// Encodes this buffer as IEEE 754 binary32 RGBA.
    ///
    /// Each 8-bit sample is mapped to `[0, 1]`. Encoding and alpha tags are unchanged.
    ///
    /// @return `width * height * 16` little-endian float bytes
    public byte @Unmodifiable [] toRgba32f() {
        byte[] floats = new byte[rgba.length * 4];
        for (int index = 0; index < rgba.length; index++) {
            int bits = Float.floatToIntBits((rgba[index] & 0xFF) / 255.0f);
            int at = index * 4;
            floats[at] = (byte) bits;
            floats[at + 1] = (byte) (bits >>> 8);
            floats[at + 2] = (byte) (bits >>> 16);
            floats[at + 3] = (byte) (bits >>> 24);
        }
        return floats;
    }

    /// Decodes IEEE 754 binary32 RGBA into an 8-bit buffer.
    ///
    /// @param width the width
    /// @param height the height
    /// @param floats little-endian float RGBA
    /// @param encoding the tagged encoding
    /// @param alpha the alpha interpretation
    /// @return the 8-bit buffer
    public static PixelBuffer fromRgba32f(
            int width,
            int height,
            byte[] floats,
            ColorEncoding encoding,
            AlphaInterpretation alpha
    ) {
        Objects.requireNonNull(floats, "floats");
        int samples = Math.multiplyExact(width, height) * 4;
        if (floats.length != samples * 4) {
            throw new IllegalArgumentException("RGBA32F length must be width * height * 16");
        }
        byte[] rgba = new byte[samples];
        for (int index = 0; index < samples; index++) {
            int at = index * 4;
            int bits = (floats[at] & 0xFF)
                    | ((floats[at + 1] & 0xFF) << 8)
                    | ((floats[at + 2] & 0xFF) << 16)
                    | ((floats[at + 3] & 0xFF) << 24);
            float unit = Float.intBitsToFloat(bits);
            rgba[index] = (byte) Math.round(Math.min(1.0f, Math.max(0.0f, unit)) * 255.0f);
        }
        return new PixelBuffer(width, height, rgba, encoding, alpha);
    }

    /// Encodes this buffer as a 32-bit `CF_DIB` payload (`BITMAPINFOHEADER` plus bottom-up BGRA).
    ///
    /// @return the DIB bytes
    public byte @Unmodifiable [] toDib() {
        int stride = width * 4;
        byte[] dib = new byte[40 + height * stride];
        putInt(dib, 0, 40);
        putInt(dib, 4, width);
        putInt(dib, 8, height);
        dib[12] = 1;
        dib[14] = 32;
        putInt(dib, 20, height * stride);
        for (int y = 0; y < height; y++) {
            int src = (height - 1 - y) * stride;
            int dst = 40 + y * stride;
            for (int x = 0; x < width; x++) {
                int s = src + x * 4;
                int d = dst + x * 4;
                dib[d] = rgba[s + 2];
                dib[d + 1] = rgba[s + 1];
                dib[d + 2] = rgba[s];
                dib[d + 3] = rgba[s + 3];
            }
        }
        return dib;
    }

    /// Decodes a 32-bit `CF_DIB` payload into an unassociated sRGB buffer.
    ///
    /// @param dib the DIB bytes
    /// @return the buffer
    public static PixelBuffer fromDib(byte[] dib) {
        Objects.requireNonNull(dib, "dib");
        if (dib.length < 40) {
            throw new IllegalArgumentException("DIB must include a BITMAPINFOHEADER");
        }
        int header = getInt(dib, 0);
        int width = getInt(dib, 4);
        int height = Math.abs(getInt(dib, 8));
        int planes = dib[12] & 0xFF | ((dib[13] & 0xFF) << 8);
        int bits = dib[14] & 0xFF | ((dib[15] & 0xFF) << 8);
        int compression = getInt(dib, 16);
        if (header < 40 || width <= 0 || height <= 0 || planes != 1 || bits != 32 || compression != 0) {
            throw new IllegalArgumentException("DIB must be uncompressed 32-bit BGRA");
        }
        int stride = width * 4;
        if (dib.length < header + height * stride) {
            throw new IllegalArgumentException("DIB pixel data is truncated");
        }
        boolean bottomUp = getInt(dib, 8) > 0;
        byte[] rgba = new byte[height * stride];
        for (int y = 0; y < height; y++) {
            int srcY = bottomUp ? height - 1 - y : y;
            int src = header + srcY * stride;
            int dst = y * stride;
            for (int x = 0; x < width; x++) {
                int s = src + x * 4;
                int d = dst + x * 4;
                rgba[d] = dib[s + 2];
                rgba[d + 1] = dib[s + 1];
                rgba[d + 2] = dib[s];
                rgba[d + 3] = dib[s + 3];
            }
        }
        return srgbUnassociated(width, height, rgba);
    }

    /// Writes a little-endian `int` at `offset`.
    private static void putInt(byte[] bytes, int offset, int value) {
        bytes[offset] = (byte) value;
        bytes[offset + 1] = (byte) (value >>> 8);
        bytes[offset + 2] = (byte) (value >>> 16);
        bytes[offset + 3] = (byte) (value >>> 24);
    }

    /// Reads a little-endian `int` at `offset`.
    private static int getInt(byte[] bytes, int offset) {
        return (bytes[offset] & 0xFF)
                | ((bytes[offset + 1] & 0xFF) << 8)
                | ((bytes[offset + 2] & 0xFF) << 16)
                | ((bytes[offset + 3] & 0xFF) << 24);
    }

    /// Encodes this buffer as IEEE 754 binary16 RGBA.
    ///
    /// Each 8-bit sample is mapped to `[0, 1]` then quantized to binary16. Encoding and alpha
    /// tags are unchanged.
    ///
    /// @return `width * height * 8` little-endian half-float bytes
    public byte @Unmodifiable [] toRgba16f() {
        byte[] half = new byte[rgba.length * 2];
        for (int index = 0; index < rgba.length; index++) {
            int bits = Short.toUnsignedInt(toHalf((rgba[index] & 0xFF) / 255.0f));
            int at = index * 2;
            half[at] = (byte) bits;
            half[at + 1] = (byte) (bits >>> 8);
        }
        return half;
    }

    /// Decodes IEEE 754 binary16 RGBA into an 8-bit buffer.
    ///
    /// @param width the width
    /// @param height the height
    /// @param half little-endian half-float RGBA
    /// @param encoding the tagged encoding
    /// @param alpha the alpha interpretation
    /// @return the 8-bit buffer
    public static PixelBuffer fromRgba16f(
            int width,
            int height,
            byte[] half,
            ColorEncoding encoding,
            AlphaInterpretation alpha
    ) {
        Objects.requireNonNull(half, "half");
        int samples = Math.multiplyExact(width, height) * 4;
        if (half.length != samples * 2) {
            throw new IllegalArgumentException("RGBA16F length must be width * height * 8");
        }
        byte[] rgba = new byte[samples];
        for (int index = 0; index < samples; index++) {
            int at = index * 2;
            int bits = (half[at] & 0xFF) | ((half[at + 1] & 0xFF) << 8);
            float unit = fromHalf((short) bits);
            rgba[index] = (byte) Math.round(Math.min(1.0f, Math.max(0.0f, unit)) * 255.0f);
        }
        return new PixelBuffer(width, height, rgba, encoding, alpha);
    }

    /// Encodes this buffer as packed `RGB10A2` little-endian words.
    ///
    /// Channel order is DXGI `R10G10B10A2_UNORM`: bits `0–9` red, `10–19` green, `20–29` blue,
    /// `30–31` alpha. Each 8-bit sample is quantized onto the destination bit depth.
    ///
    /// @return `width * height` packed pixels
    public int @Unmodifiable [] toRgb10a2() {
        int[] packed = new int[width * height];
        for (int pixel = 0; pixel < packed.length; pixel++) {
            int at = pixel * 4;
            packed[pixel] = quantize10(rgba[at])
                    | (quantize10(rgba[at + 1]) << 10)
                    | (quantize10(rgba[at + 2]) << 20)
                    | (quantize2(rgba[at + 3]) << 30);
        }
        return packed;
    }

    /// Decodes packed `RGB10A2` words into an 8-bit buffer.
    ///
    /// @param width the width
    /// @param height the height
    /// @param packed DXGI `R10G10B10A2_UNORM` words
    /// @param encoding the tagged encoding
    /// @param alpha the alpha interpretation
    /// @return the 8-bit buffer
    public static PixelBuffer fromRgb10a2(
            int width,
            int height,
            int[] packed,
            ColorEncoding encoding,
            AlphaInterpretation alpha
    ) {
        Objects.requireNonNull(packed, "packed");
        int pixels = Math.multiplyExact(width, height);
        if (packed.length != pixels) {
            throw new IllegalArgumentException("RGB10A2 length must be width * height");
        }
        byte[] rgba = new byte[pixels * 4];
        for (int pixel = 0; pixel < pixels; pixel++) {
            int word = packed[pixel];
            int at = pixel * 4;
            rgba[at] = dequantize10(word & 0x3FF);
            rgba[at + 1] = dequantize10((word >>> 10) & 0x3FF);
            rgba[at + 2] = dequantize10((word >>> 20) & 0x3FF);
            rgba[at + 3] = dequantize2((word >>> 30) & 0x3);
        }
        return new PixelBuffer(width, height, rgba, encoding, alpha);
    }

    /// Quantizes one 8-bit sample onto 10 bits.
    private static int quantize10(byte sample) {
        return Math.round((sample & 0xFF) * 1023.0f / 255.0f);
    }

    /// Expands one 10-bit sample onto 8 bits.
    private static byte dequantize10(int sample) {
        return (byte) Math.round(sample * 255.0f / 1023.0f);
    }

    /// Quantizes one 8-bit sample onto 2 bits.
    private static int quantize2(byte sample) {
        return Math.round((sample & 0xFF) * 3.0f / 255.0f);
    }

    /// Expands one 2-bit sample onto 8 bits.
    private static byte dequantize2(int sample) {
        return (byte) Math.round(sample * 255.0f / 3.0f);
    }

    /// Encodes one finite unit value as IEEE 754 binary16.
    static short toHalf(float value) {
        int bits = Float.floatToIntBits(value);
        int sign = (bits >>> 16) & 0x8000;
        int exponent = ((bits >>> 23) & 0xFF) - 127 + 15;
        int mantissa = bits & 0x7F_FFFF;
        if (exponent <= 0) {
            return (short) sign;
        }
        if (exponent >= 31) {
            return (short) (sign | 0x7C00);
        }
        return (short) (sign | (exponent << 10) | (mantissa >>> 13));
    }

    /// Decodes one IEEE 754 binary16 value.
    static float fromHalf(short bits) {
        int value = Short.toUnsignedInt(bits);
        int sign = (value & 0x8000) << 16;
        int exponent = (value >>> 10) & 0x1F;
        int mantissa = value & 0x3FF;
        if (exponent == 0) {
            return Float.intBitsToFloat(sign);
        }
        if (exponent == 31) {
            return Float.intBitsToFloat(sign | 0x7F80_0000 | (mantissa << 13));
        }
        return Float.intBitsToFloat(sign | ((exponent - 15 + 127) << 23) | (mantissa << 13));
    }
}

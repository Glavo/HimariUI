package org.glavo.himari.graphics;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Objects;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/// Encodes and decodes PNG rasters as unassociated sRGB RGBA.
///
/// [`#encode(int, int, byte[])`] writes non-interlaced filter-0 IDAT rows. [`#encodeInterlaced(int, int, byte[])`]
/// writes Adam7 interlaced filter-0 passes. [`#encodeIndexed(int, int, byte[])`] writes color-type 3
/// with a `PLTE` of at most 256 colors. [`#encodeGreyscaleAlpha(int, int, byte[])`] writes color-type 4.
/// [`#encode16(int, int, byte[])`] writes 16-bit RGBA. [`#encodeRgbWithTransparency(int, int, byte[])`]
/// writes color-type 2 plus a `tRNS` key. [`#encodePaeth(int, int, byte[])`] writes filter-4
/// scanlines. [`#encodeSub(int, int, byte[])`] writes filter-1 scanlines.
/// [`#encodeUp(int, int, byte[])`] writes filter-2 scanlines.
/// [`#encodeAverage(int, int, byte[])`] writes filter-3 scanlines.
/// [`#encodeCicp(int, int, byte[], ColorEncoding)`] writes a `cICP` chunk.
/// [`#encodeIccp(int, int, byte[], byte[])`] writes a zlib-compressed `iCCP` profile. The decoder
/// accepts 8-bit and 16-bit greyscale, greyscale-alpha, indexed, RGB, and RGBA, including
/// Adam7, Paeth, Sub, Up, Average, `cICP`, `iCCP`, and `tRNS`.
@NotNullByDefault
public final class PngImage {
    /// PNG signature.
    private static final byte[] SIGNATURE = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };

    /// Prevents instantiation.
    private PngImage() {
    }

    /// Returns whether `bytes` begin with the PNG signature.
    ///
    /// @param bytes the candidate stream
    /// @return whether the stream is PNG
    public static boolean isPng(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes");
        if (bytes.length < SIGNATURE.length) {
            return false;
        }
        for (int index = 0; index < SIGNATURE.length; index++) {
            if (bytes[index] != SIGNATURE[index]) {
                return false;
            }
        }
        return true;
    }

    /// Encodes row-major unassociated RGBA8 pixels as PNG.
    ///
    /// @param width the positive pixel width
    /// @param height the positive pixel height
    /// @param rgba `width * height * 4` bytes
    /// @return the PNG bytes
    public static byte @Unmodifiable [] encode(int width, int height, byte[] rgba) {
        Objects.requireNonNull(rgba, "rgba");
        int pixelCount = checkedPixelCount(width, height);
        if (rgba.length != pixelCount * 4) {
            throw new IllegalArgumentException("RGBA length must be width * height * 4");
        }
        byte[] raw = new byte[height * (1 + width * 4)];
        int dest = 0;
        int source = 0;
        for (int row = 0; row < height; row++) {
            raw[dest++] = 0;
            System.arraycopy(rgba, source, raw, dest, width * 4);
            dest += width * 4;
            source += width * 4;
        }
        byte[] deflated = deflate(raw);
        ByteBuffer output = ByteBuffer.allocate(8 + 12 + 13 + 12 + deflated.length + 12)
                .order(ByteOrder.BIG_ENDIAN);
        output.put(SIGNATURE);
        writeChunk(output, "IHDR", ihdr(width, height));
        writeChunk(output, "IDAT", deflated);
        writeChunk(output, "IEND", new byte[0]);
        return Arrays.copyOf(output.array(), output.position());
    }

    /// Encodes row-major unassociated RGBA8 pixels as Adam7 interlaced PNG.
    ///
    /// Each pass uses filter 0. Width and height must lie in
    /// `(0, `[`PixelBuffer#MAX_EDGE`]`)`.
    ///
    /// @param width the positive pixel width
    /// @param height the positive pixel height
    /// @param rgba `width * height * 4` bytes
    /// @return the PNG bytes
    public static byte @Unmodifiable [] encodeInterlaced(int width, int height, byte[] rgba) {
        Objects.requireNonNull(rgba, "rgba");
        int pixelCount = checkedPixelCount(width, height);
        if (rgba.length != pixelCount * 4) {
            throw new IllegalArgumentException("RGBA length must be width * height * 4");
        }
        byte[] raw = adam7Pack(rgba, width, height, 4);
        byte[] deflated = deflate(raw);
        ByteBuffer output = ByteBuffer.allocate(8 + 12 + 13 + 12 + deflated.length + 12)
                .order(ByteOrder.BIG_ENDIAN);
        output.put(SIGNATURE);
        writeChunk(output, "IHDR", ihdr(width, height, 1));
        writeChunk(output, "IDAT", deflated);
        writeChunk(output, "IEND", new byte[0]);
        return Arrays.copyOf(output.array(), output.position());
    }

    /// Encodes RGBA as 8-bit indexed PNG with a `PLTE` of unique opaque colors.
    ///
    /// Distinct colors are limited to 256. Alpha is dropped. Width and height must lie in
    /// `(0, `[`PixelBuffer#MAX_EDGE`]`)`.
    ///
    /// @param width the positive pixel width
    /// @param height the positive pixel height
    /// @param rgba `width * height * 4` bytes
    /// @return the PNG bytes
    public static byte @Unmodifiable [] encodeIndexed(int width, int height, byte[] rgba) {
        Objects.requireNonNull(rgba, "rgba");
        int pixelCount = checkedPixelCount(width, height);
        if (rgba.length != pixelCount * 4) {
            throw new IllegalArgumentException("RGBA length must be width * height * 4");
        }
        int[] palette = new int[256];
        int colors = 0;
        byte[] indices = new byte[pixelCount];
        for (int index = 0; index < pixelCount; index++) {
            int offset = index * 4;
            int rgb = ((rgba[offset] & 0xFF) << 16) | ((rgba[offset + 1] & 0xFF) << 8) | (rgba[offset + 2] & 0xFF);
            int slot = -1;
            for (int color = 0; color < colors; color++) {
                if (palette[color] == rgb) {
                    slot = color;
                    break;
                }
            }
            if (slot < 0) {
                if (colors == 256) {
                    throw new IllegalArgumentException("PNG indexed encode requires at most 256 colors");
                }
                slot = colors;
                palette[colors++] = rgb;
            }
            indices[index] = (byte) slot;
        }
        byte[] plte = new byte[colors * 3];
        for (int color = 0; color < colors; color++) {
            plte[color * 3] = (byte) (palette[color] >>> 16);
            plte[color * 3 + 1] = (byte) (palette[color] >>> 8);
            plte[color * 3 + 2] = (byte) palette[color];
        }
        byte[] raw = new byte[height * (1 + width)];
        int dest = 0;
        int source = 0;
        for (int row = 0; row < height; row++) {
            raw[dest++] = 0;
            System.arraycopy(indices, source, raw, dest, width);
            dest += width;
            source += width;
        }
        byte[] deflated = deflate(raw);
        ByteBuffer output = ByteBuffer.allocate(8 + 12 + 13 + 12 + plte.length + 12 + deflated.length + 12)
                .order(ByteOrder.BIG_ENDIAN);
        output.put(SIGNATURE);
        writeChunk(output, "IHDR", ihdr(width, height, 0, 3));
        writeChunk(output, "PLTE", plte);
        writeChunk(output, "IDAT", deflated);
        writeChunk(output, "IEND", new byte[0]);
        return Arrays.copyOf(output.array(), output.position());
    }

    /// Encodes RGBA as 8-bit greyscale-alpha PNG.
    ///
    /// Grey is the green channel. Width and height must lie in `(0, `[`PixelBuffer#MAX_EDGE`]`)`.
    ///
    /// @param width the positive pixel width
    /// @param height the positive pixel height
    /// @param rgba `width * height * 4` bytes
    /// @return the PNG bytes
    public static byte @Unmodifiable [] encodeGreyscaleAlpha(int width, int height, byte[] rgba) {
        Objects.requireNonNull(rgba, "rgba");
        int pixelCount = checkedPixelCount(width, height);
        if (rgba.length != pixelCount * 4) {
            throw new IllegalArgumentException("RGBA length must be width * height * 4");
        }
        byte[] raw = new byte[height * (1 + width * 2)];
        int dest = 0;
        for (int row = 0; row < height; row++) {
            raw[dest++] = 0;
            for (int x = 0; x < width; x++) {
                int source = (row * width + x) * 4;
                raw[dest++] = rgba[source + 1];
                raw[dest++] = rgba[source + 3];
            }
        }
        byte[] deflated = deflate(raw);
        ByteBuffer output = ByteBuffer.allocate(8 + 12 + 13 + 12 + deflated.length + 12)
                .order(ByteOrder.BIG_ENDIAN);
        output.put(SIGNATURE);
        writeChunk(output, "IHDR", ihdr(width, height, 0, 4));
        writeChunk(output, "IDAT", deflated);
        writeChunk(output, "IEND", new byte[0]);
        return Arrays.copyOf(output.array(), output.position());
    }

    /// Encodes RGBA as 16-bit RGBA PNG, storing each 8-bit sample in the high byte.
    ///
    /// Width and height must lie in `(0, `[`PixelBuffer#MAX_EDGE`]`)`.
    ///
    /// @param width the positive pixel width
    /// @param height the positive pixel height
    /// @param rgba `width * height * 4` bytes
    /// @return the PNG bytes
    public static byte @Unmodifiable [] encode16(int width, int height, byte[] rgba) {
        Objects.requireNonNull(rgba, "rgba");
        int pixelCount = checkedPixelCount(width, height);
        if (rgba.length != pixelCount * 4) {
            throw new IllegalArgumentException("RGBA length must be width * height * 4");
        }
        byte[] raw = new byte[height * (1 + width * 8)];
        int dest = 0;
        for (int row = 0; row < height; row++) {
            raw[dest++] = 0;
            for (int x = 0; x < width; x++) {
                int source = (row * width + x) * 4;
                for (int channel = 0; channel < 4; channel++) {
                    raw[dest++] = rgba[source + channel];
                    raw[dest++] = 0;
                }
            }
        }
        byte[] deflated = deflate(raw);
        ByteBuffer output = ByteBuffer.allocate(8 + 12 + 13 + 12 + deflated.length + 12)
                .order(ByteOrder.BIG_ENDIAN);
        output.put(SIGNATURE);
        writeChunk(output, "IHDR", ihdr(width, height, 0, 6, 16));
        writeChunk(output, "IDAT", deflated);
        writeChunk(output, "IEND", new byte[0]);
        return Arrays.copyOf(output.array(), output.position());
    }

    /// Encodes RGB PNG with a `tRNS` key for the first fully transparent pixel.
    ///
    /// Pixels whose RGB matches that key decode with alpha `0`. If no pixel is fully transparent,
    /// `tRNS` uses RGB `(0, 0, 0)` only when that color is absent; otherwise the stream has no
    /// `tRNS` chunk. Width and height must lie in `(0, `[`PixelBuffer#MAX_EDGE`]`)`.
    ///
    /// @param width the positive pixel width
    /// @param height the positive pixel height
    /// @param rgba `width * height * 4` bytes
    /// @return the PNG bytes
    public static byte @Unmodifiable [] encodeRgbWithTransparency(int width, int height, byte[] rgba) {
        Objects.requireNonNull(rgba, "rgba");
        int pixelCount = checkedPixelCount(width, height);
        if (rgba.length != pixelCount * 4) {
            throw new IllegalArgumentException("RGBA length must be width * height * 4");
        }
        int key = -1;
        for (int index = 0; index < pixelCount; index++) {
            if ((rgba[index * 4 + 3] & 0xFF) == 0) {
                key = ((rgba[index * 4] & 0xFF) << 16)
                        | ((rgba[index * 4 + 1] & 0xFF) << 8)
                        | (rgba[index * 4 + 2] & 0xFF);
                break;
            }
        }
        byte[] raw = new byte[height * (1 + width * 3)];
        int dest = 0;
        for (int row = 0; row < height; row++) {
            raw[dest++] = 0;
            for (int x = 0; x < width; x++) {
                int source = (row * width + x) * 4;
                raw[dest++] = rgba[source];
                raw[dest++] = rgba[source + 1];
                raw[dest++] = rgba[source + 2];
            }
        }
        byte[] deflated = deflate(raw);
        byte[] trns = new byte[0];
        if (key >= 0) {
            trns = new byte[] {
                    0, (byte) (key >>> 16),
                    0, (byte) (key >>> 8),
                    0, (byte) key
            };
        }
        ByteBuffer output = ByteBuffer.allocate(8 + 12 + 13 + (trns.length == 0 ? 0 : 12 + trns.length)
                        + 12 + deflated.length + 12)
                .order(ByteOrder.BIG_ENDIAN);
        output.put(SIGNATURE);
        writeChunk(output, "IHDR", ihdr(width, height, 0, 2));
        if (trns.length != 0) {
            writeChunk(output, "tRNS", trns);
        }
        writeChunk(output, "IDAT", deflated);
        writeChunk(output, "IEND", new byte[0]);
        return Arrays.copyOf(output.array(), output.position());
    }

    /// Encodes RGBA as non-interlaced PNG using the Paeth filter on every scanline.
    ///
    /// Width and height must lie in `(0, `[`PixelBuffer#MAX_EDGE`]`)`.
    ///
    /// @param width the positive pixel width
    /// @param height the positive pixel height
    /// @param rgba `width * height * 4` bytes
    /// @return the PNG bytes
    public static byte @Unmodifiable [] encodePaeth(int width, int height, byte[] rgba) {
        Objects.requireNonNull(rgba, "rgba");
        int pixelCount = checkedPixelCount(width, height);
        if (rgba.length != pixelCount * 4) {
            throw new IllegalArgumentException("RGBA length must be width * height * 4");
        }
        int stride = width * 4;
        byte[] raw = new byte[height * (1 + stride)];
        byte[] prev = new byte[stride];
        int dest = 0;
        for (int row = 0; row < height; row++) {
            raw[dest++] = 4;
            int source = row * stride;
            for (int x = 0; x < stride; x++) {
                int sample = rgba[source + x] & 0xFF;
                int left = x >= 4 ? rgba[source + x - 4] & 0xFF : 0;
                int up = prev[x] & 0xFF;
                int upLeft = x >= 4 ? prev[x - 4] & 0xFF : 0;
                raw[dest++] = (byte) (sample - paeth(left, up, upLeft));
            }
            System.arraycopy(rgba, source, prev, 0, stride);
        }
        byte[] deflated = deflate(raw);
        ByteBuffer output = ByteBuffer.allocate(8 + 12 + 13 + 12 + deflated.length + 12)
                .order(ByteOrder.BIG_ENDIAN);
        output.put(SIGNATURE);
        writeChunk(output, "IHDR", ihdr(width, height));
        writeChunk(output, "IDAT", deflated);
        writeChunk(output, "IEND", new byte[0]);
        return Arrays.copyOf(output.array(), output.position());
    }

    /// Encodes RGBA as non-interlaced PNG using the Sub filter on every scanline.
    ///
    /// Width and height must lie in `(0, `[`PixelBuffer#MAX_EDGE`]`)`.
    ///
    /// @param width the positive pixel width
    /// @param height the positive pixel height
    /// @param rgba `width * height * 4` bytes
    /// @return the PNG bytes
    public static byte @Unmodifiable [] encodeSub(int width, int height, byte[] rgba) {
        Objects.requireNonNull(rgba, "rgba");
        int pixelCount = checkedPixelCount(width, height);
        if (rgba.length != pixelCount * 4) {
            throw new IllegalArgumentException("RGBA length must be width * height * 4");
        }
        int stride = width * 4;
        byte[] raw = new byte[height * (1 + stride)];
        int dest = 0;
        for (int row = 0; row < height; row++) {
            raw[dest++] = 1;
            int source = row * stride;
            for (int x = 0; x < stride; x++) {
                int sample = rgba[source + x] & 0xFF;
                int left = x >= 4 ? rgba[source + x - 4] & 0xFF : 0;
                raw[dest++] = (byte) (sample - left);
            }
        }
        byte[] deflated = deflate(raw);
        ByteBuffer output = ByteBuffer.allocate(8 + 12 + 13 + 12 + deflated.length + 12)
                .order(ByteOrder.BIG_ENDIAN);
        output.put(SIGNATURE);
        writeChunk(output, "IHDR", ihdr(width, height));
        writeChunk(output, "IDAT", deflated);
        writeChunk(output, "IEND", new byte[0]);
        return Arrays.copyOf(output.array(), output.position());
    }

    /// Encodes RGBA as non-interlaced PNG using the Up filter on every scanline.
    ///
    /// Width and height must lie in `(0, `[`PixelBuffer#MAX_EDGE`]`)`.
    ///
    /// @param width the positive pixel width
    /// @param height the positive pixel height
    /// @param rgba `width * height * 4` bytes
    /// @return the PNG bytes
    public static byte @Unmodifiable [] encodeUp(int width, int height, byte[] rgba) {
        Objects.requireNonNull(rgba, "rgba");
        int pixelCount = checkedPixelCount(width, height);
        if (rgba.length != pixelCount * 4) {
            throw new IllegalArgumentException("RGBA length must be width * height * 4");
        }
        int stride = width * 4;
        byte[] raw = new byte[height * (1 + stride)];
        byte[] prev = new byte[stride];
        int dest = 0;
        for (int row = 0; row < height; row++) {
            raw[dest++] = 2;
            int source = row * stride;
            for (int x = 0; x < stride; x++) {
                raw[dest++] = (byte) ((rgba[source + x] & 0xFF) - (prev[x] & 0xFF));
            }
            System.arraycopy(rgba, source, prev, 0, stride);
        }
        byte[] deflated = deflate(raw);
        ByteBuffer output = ByteBuffer.allocate(8 + 12 + 13 + 12 + deflated.length + 12)
                .order(ByteOrder.BIG_ENDIAN);
        output.put(SIGNATURE);
        writeChunk(output, "IHDR", ihdr(width, height));
        writeChunk(output, "IDAT", deflated);
        writeChunk(output, "IEND", new byte[0]);
        return Arrays.copyOf(output.array(), output.position());
    }

    /// Encodes RGBA as non-interlaced PNG using the Average filter on every scanline.
    ///
    /// Width and height must lie in `(0, `[`PixelBuffer#MAX_EDGE`]`)`.
    ///
    /// @param width the positive pixel width
    /// @param height the positive pixel height
    /// @param rgba `width * height * 4` bytes
    /// @return the PNG bytes
    public static byte @Unmodifiable [] encodeAverage(int width, int height, byte[] rgba) {
        Objects.requireNonNull(rgba, "rgba");
        int pixelCount = checkedPixelCount(width, height);
        if (rgba.length != pixelCount * 4) {
            throw new IllegalArgumentException("RGBA length must be width * height * 4");
        }
        int stride = width * 4;
        byte[] raw = new byte[height * (1 + stride)];
        byte[] prev = new byte[stride];
        int dest = 0;
        for (int row = 0; row < height; row++) {
            raw[dest++] = 3;
            int source = row * stride;
            for (int x = 0; x < stride; x++) {
                int sample = rgba[source + x] & 0xFF;
                int left = x >= 4 ? rgba[source + x - 4] & 0xFF : 0;
                int up = prev[x] & 0xFF;
                raw[dest++] = (byte) (sample - ((left + up) / 2));
            }
            System.arraycopy(rgba, source, prev, 0, stride);
        }
        byte[] deflated = deflate(raw);
        ByteBuffer output = ByteBuffer.allocate(8 + 12 + 13 + 12 + deflated.length + 12)
                .order(ByteOrder.BIG_ENDIAN);
        output.put(SIGNATURE);
        writeChunk(output, "IHDR", ihdr(width, height));
        writeChunk(output, "IDAT", deflated);
        writeChunk(output, "IEND", new byte[0]);
        return Arrays.copyOf(output.array(), output.position());
    }

    /// Encodes RGBA PNG with a `cICP` chunk for `encoding`.
    ///
    /// Width and height must lie in `(0, `[`PixelBuffer#MAX_EDGE`]`)`.
    ///
    /// @param width the positive pixel width
    /// @param height the positive pixel height
    /// @param rgba `width * height * 4` bytes
    /// @param encoding the tagged encoding written as ITU-T H.273 codes
    /// @return the PNG bytes
    public static byte @Unmodifiable [] encodeCicp(int width, int height, byte[] rgba, ColorEncoding encoding) {
        Objects.requireNonNull(rgba, "rgba");
        Objects.requireNonNull(encoding, "encoding");
        byte[] png = encode(width, height, rgba);
        byte[] cicp = cicpBytes(encoding);
        ByteBuffer output = ByteBuffer.allocate(png.length + 12 + cicp.length).order(ByteOrder.BIG_ENDIAN);
        int ihdrEnd = 8 + 25;
        output.put(png, 0, ihdrEnd);
        writeChunk(output, "cICP", cicp);
        output.put(png, ihdrEnd, png.length - ihdrEnd);
        return Arrays.copyOf(output.array(), output.position());
    }

    /// Encodes RGBA PNG with a zlib-compressed `iCCP` profile.
    ///
    /// `icc` must parse as an ICC v2/v4 RGB matrix profile. Width and height must lie in
    /// `(0, `[`PixelBuffer#MAX_EDGE`]`)`.
    ///
    /// @param width the positive pixel width
    /// @param height the positive pixel height
    /// @param rgba `width * height * 4` bytes
    /// @param icc the ICC profile bytes
    /// @return the PNG bytes
    public static byte @Unmodifiable [] encodeIccp(int width, int height, byte[] rgba, byte[] icc) {
        Objects.requireNonNull(rgba, "rgba");
        Objects.requireNonNull(icc, "icc");
        IccProfile.parse(icc);
        byte[] png = encode(width, height, rgba);
        byte[] keyword = new byte[] {
                'I', 'C', 'C', ' ', 'P', 'r', 'o', 'f', 'i', 'l', 'e', 0, 0
        };
        byte[] compressed = deflate(icc);
        byte[] payload = new byte[keyword.length + compressed.length];
        System.arraycopy(keyword, 0, payload, 0, keyword.length);
        System.arraycopy(compressed, 0, payload, keyword.length, compressed.length);
        ByteBuffer output = ByteBuffer.allocate(png.length + 12 + payload.length).order(ByteOrder.BIG_ENDIAN);
        int ihdrEnd = 8 + 25;
        output.put(png, 0, ihdrEnd);
        writeChunk(output, "iCCP", payload);
        output.put(png, ihdrEnd, png.length - ihdrEnd);
        return Arrays.copyOf(output.array(), output.position());
    }

    /// Returns the inflated `iCCP` profile, or `null` when the chunk is absent.
    ///
    /// @param bytes the PNG stream
    /// @return the ICC profile bytes, or `null`
    public static byte @Nullable [] iccProfile(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes");
        if (!isPng(bytes)) {
            throw new IllegalArgumentException("PNG signature is missing");
        }
        int offset = 8;
        while (offset + 12 <= bytes.length) {
            int length = readBe(bytes, offset);
            if (length < 0 || offset + 12 + length > bytes.length) {
                throw new IllegalArgumentException("PNG chunk is truncated");
            }
            int type = readBe(bytes, offset + 4);
            if (type == fourcc("iCCP")) {
                int data = offset + 8;
                int end = data + length;
                int cursor = data;
                while (cursor < end && bytes[cursor] != 0) {
                    cursor++;
                }
                if (cursor + 2 > end) {
                    throw new IllegalArgumentException("PNG iCCP is truncated");
                }
                cursor += 2;
                return inflateBounded(Arrays.copyOfRange(bytes, cursor, end), IccProfile.MAX_PROFILE_BYTES);
            }
            if (type == fourcc("IEND")) {
                break;
            }
            offset += 12 + length;
        }
        return null;
    }

    /// Decodes a PNG stream into row-major unassociated RGBA8.
    ///
    /// @param bytes the PNG stream
    /// @return the decoded image
    public static Decoded decode(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes");
        if (!isPng(bytes)) {
            throw new IllegalArgumentException("PNG signature is missing");
        }
        int offset = 8;
        int width = 0;
        int height = 0;
        int bitDepth = 0;
        int colorType = 0;
        int interlaceMethod = 0;
        byte[] idat = new byte[0];
        byte[] palette = new byte[0];
        byte[] trns = new byte[0];
        ColorEncoding encoding = ColorEncoding.SRGB;
        boolean sawIhdr = false;
        while (offset + 12 <= bytes.length) {
            int length = readBe(bytes, offset);
            if (length < 0 || offset + 12 + length > bytes.length) {
                throw new IllegalArgumentException("PNG chunk is truncated");
            }
            int type = readBe(bytes, offset + 4);
            int data = offset + 8;
            if (type == fourcc("IHDR")) {
                if (length < 13) {
                    throw new IllegalArgumentException("PNG IHDR is truncated");
                }
                width = readBe(bytes, data);
                height = readBe(bytes, data + 4);
                bitDepth = bytes[data + 8] & 0xFF;
                colorType = bytes[data + 9] & 0xFF;
                int interlace = bytes[data + 12] & 0xFF;
                if ((bitDepth != 8 && bitDepth != 16)
                        || (interlace != 0 && interlace != 1)
                        || (colorType != 0 && colorType != 2 && colorType != 3 && colorType != 4 && colorType != 6)
                        || (bitDepth == 16 && colorType == 3)) {
                    throw new IllegalArgumentException("PNG decode accepts 8/16-bit grey, GA, RGB, RGBA, and 8-bit indexed");
                }
                interlaceMethod = interlace;
                checkedPixelCount(width, height);
                sawIhdr = true;
            } else if (type == fourcc("PLTE")) {
                if (length == 0 || length % 3 != 0 || length > 768) {
                    throw new IllegalArgumentException("PNG PLTE length is invalid");
                }
                palette = Arrays.copyOfRange(bytes, data, data + length);
            } else if (type == fourcc("tRNS")) {
                trns = Arrays.copyOfRange(bytes, data, data + length);
            } else if (type == fourcc("cICP")) {
                if (length < 4) {
                    throw new IllegalArgumentException("PNG cICP is truncated");
                }
                encoding = encodingFromCicp(bytes[data] & 0xFF, bytes[data + 1] & 0xFF);
            } else if (type == fourcc("IDAT")) {
                byte[] next = new byte[idat.length + length];
                System.arraycopy(idat, 0, next, 0, idat.length);
                System.arraycopy(bytes, data, next, idat.length, length);
                idat = next;
            } else if (type == fourcc("IEND")) {
                break;
            }
            offset += 12 + length;
        }
        if (!sawIhdr) {
            throw new IllegalArgumentException("PNG IHDR is missing");
        }
        int pixelBytes = pixelBytes(colorType, bitDepth);
        if (colorType == 3 && palette.length < 3) {
            throw new IllegalArgumentException("PNG indexed image is missing PLTE");
        }
        if (interlaceMethod == 1) {
            byte[] raw = inflate(idat, adam7RawSize(width, height, pixelBytes));
            return new Decoded(
                    width,
                    height,
                    unfilterAdam7(raw, width, height, colorType, bitDepth, palette, trns),
                    encoding
            );
        }
        byte[] raw = inflate(idat, height * (1 + width * pixelBytes));
        return new Decoded(
                width,
                height,
                unfilter(raw, width, height, colorType, bitDepth, 0, palette, trns),
                encoding
        );
    }

    /// Rebuilds RGBA rows from PNG filtered scanlines starting at `source`.
    private static byte[] unfilter(
            byte[] raw,
            int width,
            int height,
            int colorType,
            int bitDepth,
            int source,
            byte[] palette,
            byte[] trns
    ) {
        int pixelBytes = pixelBytes(colorType, bitDepth);
        int stride = width * pixelBytes;
        byte[] samples = new byte[width * height * pixelBytes];
        byte[] prev = new byte[stride];
        byte[] curr = new byte[stride];
        int dest = 0;
        for (int y = 0; y < height; y++) {
            int filter = raw[source++] & 0xFF;
            System.arraycopy(raw, source, curr, 0, stride);
            source += stride;
            for (int x = 0; x < stride; x++) {
                int left = x >= pixelBytes ? curr[x - pixelBytes] & 0xFF : 0;
                int up = prev[x] & 0xFF;
                int upLeft = x >= pixelBytes ? prev[x - pixelBytes] & 0xFF : 0;
                int sample = curr[x] & 0xFF;
                curr[x] = (byte) switch (filter) {
                    case 0 -> sample;
                    case 1 -> sample + left;
                    case 2 -> sample + up;
                    case 3 -> sample + ((left + up) / 2);
                    case 4 -> sample + paeth(left, up, upLeft);
                    default -> throw new IllegalArgumentException("PNG filter is unsupported");
                };
            }
            System.arraycopy(curr, 0, samples, dest, stride);
            dest += stride;
            System.arraycopy(curr, 0, prev, 0, stride);
        }
        return toRgba(samples, width, height, colorType, bitDepth, palette, trns);
    }

    /// Expands reconstructed samples into unassociated RGBA8.
    private static byte[] toRgba(
            byte[] samples,
            int width,
            int height,
            int colorType,
            int bitDepth,
            byte[] palette,
            byte[] trns
    ) {
        int bytesPerSample = bitDepth / 8;
        int pixelBytes = pixelBytes(colorType, bitDepth);
        byte[] rgba = new byte[width * height * 4];
        for (int index = 0; index < width * height; index++) {
            int src = index * pixelBytes;
            int dest = index * 4;
            int s0 = sample(samples, src, bytesPerSample);
            if (colorType == 0) {
                rgba[dest] = (byte) s0;
                rgba[dest + 1] = (byte) s0;
                rgba[dest + 2] = (byte) s0;
                rgba[dest + 3] = (byte) (greyTransparent(s0, bitDepth, samples, src, trns) ? 0 : 255);
            } else if (colorType == 4) {
                int alpha = sample(samples, src + bytesPerSample, bytesPerSample);
                rgba[dest] = (byte) s0;
                rgba[dest + 1] = (byte) s0;
                rgba[dest + 2] = (byte) s0;
                rgba[dest + 3] = (byte) alpha;
            } else if (colorType == 2) {
                int green = sample(samples, src + bytesPerSample, bytesPerSample);
                int blue = sample(samples, src + 2 * bytesPerSample, bytesPerSample);
                rgba[dest] = (byte) s0;
                rgba[dest + 1] = (byte) green;
                rgba[dest + 2] = (byte) blue;
                rgba[dest + 3] = (byte) (rgbTransparent(s0, green, blue, bitDepth, samples, src, trns) ? 0 : 255);
            } else if (colorType == 3) {
                expandIndexedPixel(rgba, dest, s0, palette, trns);
            } else {
                rgba[dest] = (byte) s0;
                rgba[dest + 1] = (byte) sample(samples, src + bytesPerSample, bytesPerSample);
                rgba[dest + 2] = (byte) sample(samples, src + 2 * bytesPerSample, bytesPerSample);
                rgba[dest + 3] = (byte) sample(samples, src + 3 * bytesPerSample, bytesPerSample);
            }
        }
        return rgba;
    }

    /// Returns the high 8 bits of a 8- or 16-bit sample.
    private static int sample(byte[] samples, int offset, int bytesPerSample) {
        return samples[offset] & 0xFF;
    }

    /// Returns whether a greyscale sample matches `tRNS`.
    private static boolean greyTransparent(int grey, int bitDepth, byte[] samples, int offset, byte[] trns) {
        if (trns.length < 2) {
            return false;
        }
        if (bitDepth == 16) {
            int value = ((samples[offset] & 0xFF) << 8) | (samples[offset + 1] & 0xFF);
            return value == ((trns[0] & 0xFF) << 8 | (trns[1] & 0xFF));
        }
        return grey == (trns[1] & 0xFF);
    }

    /// Returns whether an RGB sample matches `tRNS`.
    private static boolean rgbTransparent(
            int red,
            int green,
            int blue,
            int bitDepth,
            byte[] samples,
            int offset,
            byte[] trns
    ) {
        if (trns.length < 6) {
            return false;
        }
        if (bitDepth == 16) {
            return u16(samples, offset) == u16(trns, 0)
                    && u16(samples, offset + 2) == u16(trns, 2)
                    && u16(samples, offset + 4) == u16(trns, 4);
        }
        return red == (trns[1] & 0xFF) && green == (trns[3] & 0xFF) && blue == (trns[5] & 0xFF);
    }

    /// Writes one indexed pixel, applying `tRNS` alpha when present.
    private static void expandIndexedPixel(byte[] rgba, int dest, int index, byte[] palette, byte[] trns) {
        int colors = palette.length / 3;
        if (index >= colors) {
            rgba[dest] = 0;
            rgba[dest + 1] = 0;
            rgba[dest + 2] = 0;
            rgba[dest + 3] = 0;
            return;
        }
        int src = index * 3;
        rgba[dest] = palette[src];
        rgba[dest + 1] = palette[src + 1];
        rgba[dest + 2] = palette[src + 2];
        rgba[dest + 3] = index < trns.length ? trns[index] : (byte) 255;
    }

    /// Bytes per pixel for `colorType` and `bitDepth`.
    private static int pixelBytes(int colorType, int bitDepth) {
        int channels = switch (colorType) {
            case 0, 3 -> 1;
            case 4 -> 2;
            case 2 -> 3;
            case 6 -> 4;
            default -> throw new IllegalArgumentException("PNG color type is unsupported");
        };
        return channels * (bitDepth / 8);
    }

    /// Reads a big-endian 16-bit sample.
    private static int u16(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) << 8) | (bytes[offset + 1] & 0xFF);
    }

    /// Returns the Paeth predictor.
    private static int paeth(int left, int up, int upLeft) {
        int p = left + up - upLeft;
        int pa = Math.abs(p - left);
        int pb = Math.abs(p - up);
        int pc = Math.abs(p - upLeft);
        if (pa <= pb && pa <= pc) {
            return left;
        }
        if (pb <= pc) {
            return up;
        }
        return upLeft;
    }

    /// Adam7 starting X for each pass.
    private static final int[] ADAM7_X0 = {0, 4, 0, 2, 0, 1, 0};

    /// Adam7 starting Y for each pass.
    private static final int[] ADAM7_Y0 = {0, 0, 4, 0, 2, 0, 1};

    /// Adam7 X step for each pass.
    private static final int[] ADAM7_DX = {8, 8, 4, 4, 2, 2, 1};

    /// Adam7 Y step for each pass.
    private static final int[] ADAM7_DY = {8, 8, 8, 4, 4, 2, 2};

    /// Returns the filtered Adam7 payload size.
    private static int adam7RawSize(int width, int height, int pixelBytes) {
        int size = 0;
        for (int pass = 0; pass < 7; pass++) {
            int passWidth = passExtent(width, ADAM7_X0[pass], ADAM7_DX[pass]);
            int passHeight = passExtent(height, ADAM7_Y0[pass], ADAM7_DY[pass]);
            if (passWidth > 0 && passHeight > 0) {
                size += passHeight * (1 + passWidth * pixelBytes);
            }
        }
        return size;
    }

    /// Returns how many samples a pass covers along one axis.
    private static int passExtent(int size, int origin, int step) {
        if (size <= origin) {
            return 0;
        }
        return (size - origin + step - 1) / step;
    }

    /// Unfilters Adam7 passes and scatters them into an RGBA raster.
    private static byte[] unfilterAdam7(
            byte[] raw,
            int width,
            int height,
            int colorType,
            int bitDepth,
            byte[] palette,
            byte[] trns
    ) {
        int pixelBytes = pixelBytes(colorType, bitDepth);
        byte[] rgba = new byte[width * height * 4];
        int source = 0;
        for (int pass = 0; pass < 7; pass++) {
            int passWidth = passExtent(width, ADAM7_X0[pass], ADAM7_DX[pass]);
            int passHeight = passExtent(height, ADAM7_Y0[pass], ADAM7_DY[pass]);
            if (passWidth == 0 || passHeight == 0) {
                continue;
            }
            byte[] passRgba = unfilter(raw, passWidth, passHeight, colorType, bitDepth, source, palette, trns);
            source += passHeight * (1 + passWidth * pixelBytes);
            for (int py = 0; py < passHeight; py++) {
                int y = ADAM7_Y0[pass] + py * ADAM7_DY[pass];
                for (int px = 0; px < passWidth; px++) {
                    int x = ADAM7_X0[pass] + px * ADAM7_DX[pass];
                    System.arraycopy(passRgba, (py * passWidth + px) * 4, rgba, (y * width + x) * 4, 4);
                }
            }
        }
        return rgba;
    }

    /// Packs RGBA into Adam7 filter-0 scanlines.
    private static byte[] adam7Pack(byte[] rgba, int width, int height, int channels) {
        byte[] raw = new byte[adam7RawSize(width, height, channels)];
        int dest = 0;
        for (int pass = 0; pass < 7; pass++) {
            int passWidth = passExtent(width, ADAM7_X0[pass], ADAM7_DX[pass]);
            int passHeight = passExtent(height, ADAM7_Y0[pass], ADAM7_DY[pass]);
            if (passWidth == 0 || passHeight == 0) {
                continue;
            }
            for (int py = 0; py < passHeight; py++) {
                int y = ADAM7_Y0[pass] + py * ADAM7_DY[pass];
                raw[dest++] = 0;
                for (int px = 0; px < passWidth; px++) {
                    int x = ADAM7_X0[pass] + px * ADAM7_DX[pass];
                    System.arraycopy(rgba, (y * width + x) * 4, raw, dest, channels);
                    dest += channels;
                }
            }
        }
        return raw;
    }

    /// Builds an IHDR payload.
    private static byte[] ihdr(int width, int height) {
        return ihdr(width, height, 0);
    }

    /// Builds an IHDR payload with `interlace`.
    private static byte[] ihdr(int width, int height, int interlace) {
        return ihdr(width, height, interlace, 6);
    }

    /// Builds an IHDR payload with `interlace` and `colorType`.
    private static byte[] ihdr(int width, int height, int interlace, int colorType) {
        return ihdr(width, height, interlace, colorType, 8);
    }

    /// Builds an IHDR payload with `interlace`, `colorType`, and `bitDepth`.
    private static byte[] ihdr(int width, int height, int interlace, int colorType, int bitDepth) {
        ByteBuffer buffer = ByteBuffer.allocate(13).order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(width);
        buffer.putInt(height);
        buffer.put((byte) bitDepth);
        buffer.put((byte) colorType);
        buffer.put((byte) 0);
        buffer.put((byte) 0);
        buffer.put((byte) interlace);
        return buffer.array();
    }

    /// Writes one PNG chunk.
    private static void writeChunk(ByteBuffer output, String type, byte[] payload) {
        output.putInt(payload.length);
        int typeOffset = output.position();
        output.put(type.getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        output.put(payload);
        java.util.zip.CRC32 crc = new java.util.zip.CRC32();
        crc.update(output.array(), typeOffset, 4 + payload.length);
        output.putInt((int) crc.getValue());
    }

    /// Deflates `raw` scanlines.
    private static byte[] deflate(byte[] raw) {
        Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION);
        deflater.setInput(raw);
        deflater.finish();
        byte[] buffer = new byte[raw.length + 64];
        int size = 0;
        while (!deflater.finished()) {
            if (size == buffer.length) {
                buffer = Arrays.copyOf(buffer, buffer.length * 2);
            }
            size += deflater.deflate(buffer, size, buffer.length - size);
        }
        deflater.end();
        return Arrays.copyOf(buffer, size);
    }

    /// Inflates zlib bytes up to `limit` octets.
    private static byte[] inflateBounded(byte[] compressed, int limit) {
        Inflater inflater = new Inflater();
        inflater.setInput(compressed);
        byte[] raw = new byte[Math.min(limit, 4096)];
        int size = 0;
        try {
            while (!inflater.finished()) {
                if (size == raw.length) {
                    if (raw.length >= limit) {
                        throw new IllegalArgumentException("PNG iCCP exceeds the profile limit");
                    }
                    raw = Arrays.copyOf(raw, Math.min(limit, raw.length * 2));
                }
                size += inflater.inflate(raw, size, raw.length - size);
            }
        } catch (DataFormatException exception) {
            throw new IllegalArgumentException("PNG iCCP is invalid", exception);
        } finally {
            inflater.end();
        }
        return Arrays.copyOf(raw, size);
    }

    /// Inflates `idat` to exactly `expected` bytes.
    private static byte[] inflate(byte[] idat, int expected) {
        Inflater inflater = new Inflater();
        inflater.setInput(idat);
        byte[] raw = new byte[expected];
        try {
            int size = inflater.inflate(raw);
            if (size != expected || !inflater.finished()) {
                throw new IllegalArgumentException("PNG IDAT size does not match IHDR");
            }
        } catch (DataFormatException exception) {
            throw new IllegalArgumentException("PNG IDAT is invalid", exception);
        } finally {
            inflater.end();
        }
        return raw;
    }

    /// Reads a big-endian 32-bit integer.
    private static int readBe(byte[] bytes, int offset) {
        return (bytes[offset] & 0xFF) << 24
                | (bytes[offset + 1] & 0xFF) << 16
                | (bytes[offset + 2] & 0xFF) << 8
                | (bytes[offset + 3] & 0xFF);
    }

    /// Returns a four-character code.
    private static int fourcc(String text) {
        byte[] bytes = text.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        return (bytes[0] & 0xFF) << 24 | (bytes[1] & 0xFF) << 16 | (bytes[2] & 0xFF) << 8 | (bytes[3] & 0xFF);
    }

    /// Returns `width * height` after rejecting non-positive or oversized images.
    private static int checkedPixelCount(int width, int height) {
        if (width <= 0 || height <= 0 || width > PixelBuffer.MAX_EDGE || height > PixelBuffer.MAX_EDGE) {
            throw new IllegalArgumentException("PNG dimensions must be in (0, " + PixelBuffer.MAX_EDGE + "]");
        }
        return Math.multiplyExact(width, height);
    }

    /// Stores one decoded PNG image.
    ///
    /// @param width the pixel width
    /// @param height the pixel height
    /// @param rgba RGBA8 pixels
    /// @param encoding the tagged encoding from `cICP`, or sRGB when the chunk is absent
    public record Decoded(int width, int height, byte @Unmodifiable [] rgba, ColorEncoding encoding) {
        /// Validates the decoded image.
        public Decoded {
            if (width <= 0 || height <= 0) {
                throw new IllegalArgumentException("Decoded size must be positive");
            }
            Objects.requireNonNull(rgba, "rgba");
            Objects.requireNonNull(encoding, "encoding");
            if (rgba.length != Math.multiplyExact(width, height) * 4) {
                throw new IllegalArgumentException("RGBA length must match width * height * 4");
            }
            rgba = Arrays.copyOf(rgba, rgba.length);
        }

        /// Creates an sRGB decoded image.
        ///
        /// @param width the pixel width
        /// @param height the pixel height
        /// @param rgba RGBA8 pixels
        public Decoded(int width, int height, byte[] rgba) {
            this(width, height, rgba, ColorEncoding.SRGB);
        }
    }

    /// Packs ITU-T H.273 codes for `encoding`.
    private static byte[] cicpBytes(ColorEncoding encoding) {
        int primaries;
        int transfer;
        switch (encoding) {
            case DISPLAY_P3, LINEAR_DISPLAY_P3 -> {
                primaries = 12;
                transfer = encoding == ColorEncoding.LINEAR_DISPLAY_P3 ? 8 : 13;
            }
            case BT2020, LINEAR_BT2020, BT2100_PQ, BT2100_HLG -> {
                primaries = 9;
                transfer = switch (encoding) {
                    case LINEAR_BT2020 -> 8;
                    case BT2100_PQ -> 16;
                    case BT2100_HLG -> 18;
                    default -> 14;
                };
            }
            case LINEAR_SRGB, LINEAR_BT709, EXTENDED_LINEAR -> {
                primaries = 1;
                transfer = 8;
            }
            default -> {
                primaries = 1;
                transfer = 13;
            }
        }
        return new byte[] {(byte) primaries, (byte) transfer, 0, 1};
    }

    /// Maps H.273 primaries and transfer to a first-stable encoding.
    private static ColorEncoding encodingFromCicp(int primaries, int transfer) {
        if (primaries == 12) {
            return transfer == 8 ? ColorEncoding.LINEAR_DISPLAY_P3 : ColorEncoding.DISPLAY_P3;
        }
        if (primaries == 9) {
            return switch (transfer) {
                case 8 -> ColorEncoding.LINEAR_BT2020;
                case 16 -> ColorEncoding.BT2100_PQ;
                case 18 -> ColorEncoding.BT2100_HLG;
                default -> ColorEncoding.BT2020;
            };
        }
        if (transfer == 8) {
            return ColorEncoding.LINEAR_SRGB;
        }
        return ColorEncoding.SRGB;
    }
}

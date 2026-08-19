package org.glavo.himari.graphics;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

/// Encodes and decodes lossless WebP (VP8L) as unassociated sRGB RGBA.
///
/// [`#encode(int, int, byte[])`] writes a `VP8L` chunk with no transforms and no color cache.
/// [`#encodeSubtractGreen(int, int, byte[])`] writes the subtract-green transform.
/// [`#encodePredictor(int, int, byte[])`] writes the predictor transform.
/// [`#encodeColor(int, int, byte[])`] writes the color transform.
/// [`#encodeIndexing(int, int, byte[])`] writes the color-indexing transform.
/// [`#encodeWithColorCache(int, int, byte[])`] writes a 16-slot color cache.
/// [`#encodeWithLz77(int, int, byte[])`] writes one literal plus a backward copy of the rest.
/// [`#decode(byte[])`] inverts those transforms in reverse stream order, applies the color cache,
/// and expands LZ77 copies.
/// Huffman tables use the simple form when a channel has at most two symbols and a length-limited
/// canonical form otherwise.
@NotNullByDefault
public final class WebpImage {
    /// `RIFF`.
    private static final int RIFF = 0x52494646;

    /// `WEBP`.
    private static final int WEBP = 0x57454250;

    /// `VP8L`.
    private static final int VP8L = 0x5650384C;

    /// VP8L signature byte.
    private static final int VP8L_MAGIC = 0x2F;

    /// Predictor transform type.
    private static final int TRANSFORM_PREDICTOR = 0;

    /// Color transform type.
    private static final int TRANSFORM_COLOR = 1;

    /// Subtract-green transform type.
    private static final int TRANSFORM_SUBTRACT_GREEN = 2;

    /// Color-indexing transform type.
    private static final int TRANSFORM_INDEXING = 3;

    /// Solid black used by predictor mode 0 and the top-left border.
    private static final int ARGB_BLACK = 0xFF000000;

    /// Color-cache hash multiplier from the VP8L specification.
    private static final int CACHE_HASH = 0x1E35A7BD;

    /// LZ77 neighborhood X offsets for distance codes 1 through 120.
    private static final int[] DIST_DX = {
            0, 1, 1, -1, 0, 2, 1,
            -1, 2, -2, 2, -2, 0, 3,
            1, -1, 3, -3, 2, -2, 3,
            -3, 0, 4, 1, -1, 4, -4,
            3, -3, 2, -2, 4, -4, 0,
            3, -3, 4, -4, 5, 1, -1,
            5, -5, 2, -2, 5, -5, 4,
            -4, 3, -3, 5, -5, 0, 6,
            1, -1, 6, -6, 2, -2, 6,
            -6, 4, -4, 5, -5, 3, -3,
            6, -6, 0, 7, 1, -1, 5,
            -5, 7, -7, 4, -4, 6, -6,
            2, -2, 7, -7, 3, -3, 7,
            -7, 5, -5, 6, -6, 8, 4,
            -4, 7, -7, 8, 8, 6, -6,
            8, 5, -5, 7, -7, 8, 6,
            -6, 7, -7, 8, 7, -7, 8,
            8
    };

    /// LZ77 neighborhood Y offsets matching [`DIST_DX`].
    private static final int[] DIST_DY = {
            1, 0, 1, 1, 2, 0, 2,
            2, 1, 1, 2, 2, 3, 0,
            3, 3, 1, 1, 3, 3, 2,
            2, 4, 0, 4, 4, 1, 1,
            3, 3, 4, 4, 2, 2, 5,
            4, 4, 3, 3, 0, 5, 5,
            1, 1, 5, 5, 2, 2, 4,
            4, 5, 5, 3, 3, 6, 0,
            6, 6, 1, 1, 6, 6, 2,
            2, 5, 5, 4, 4, 6, 6,
            3, 3, 7, 0, 7, 7, 5,
            5, 1, 1, 6, 6, 4, 4,
            7, 7, 2, 2, 7, 7, 3,
            3, 6, 6, 5, 5, 0, 7,
            7, 4, 4, 1, 2, 6, 6,
            3, 7, 7, 5, 5, 4, 7,
            7, 6, 6, 5, 7, 7, 6,
            7
    };

    /// Code-length code order from the VP8L specification.
    private static final int[] CODE_LENGTH_ORDER = {
            17, 18, 0, 1, 2, 3, 4, 5, 16, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15
    };

    /// Prevents instantiation.
    private WebpImage() {
    }

    /// Returns whether `bytes` are a RIFF/WEBP container with a `VP8L` chunk.
    ///
    /// @param bytes the candidate stream
    /// @return whether the stream is lossless WebP
    public static boolean isWebp(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes");
        if (bytes.length < 20) {
            return false;
        }
        return readBe(bytes, 0) == RIFF && readBe(bytes, 8) == WEBP && readBe(bytes, 12) == VP8L;
    }

    /// Encodes row-major unassociated RGBA8 pixels as lossless WebP.
    ///
    /// @param width the positive pixel width
    /// @param height the positive pixel height
    /// @param rgba `width * height * 4` bytes
    /// @return the WebP bytes
    public static byte @Unmodifiable [] encode(int width, int height, byte[] rgba) {
        Objects.requireNonNull(rgba, "rgba");
        int pixelCount = checkedPixelCount(width, height);
        if (rgba.length != pixelCount * 4) {
            throw new IllegalArgumentException("RGBA length must be width * height * 4");
        }
        LsbWriter bits = new LsbWriter();
        bits.write(VP8L_MAGIC, 8);
        bits.write(width - 1, 14);
        bits.write(height - 1, 14);
        bits.write(1, 1);
        bits.write(0, 3);
        bits.write(0, 1);
        bits.write(0, 1);
        int[] green = new int[pixelCount];
        int[] red = new int[pixelCount];
        int[] blue = new int[pixelCount];
        int[] alpha = new int[pixelCount];
        for (int index = 0; index < pixelCount; index++) {
            int offset = index * 4;
            red[index] = rgba[offset] & 0xFF;
            green[index] = rgba[offset + 1] & 0xFF;
            blue[index] = rgba[offset + 2] & 0xFF;
            alpha[index] = rgba[offset + 3] & 0xFF;
        }
        Huffman gHuff = Huffman.fromSymbols(green, 280);
        Huffman rHuff = Huffman.fromSymbols(red, 256);
        Huffman bHuff = Huffman.fromSymbols(blue, 256);
        Huffman aHuff = Huffman.fromSymbols(alpha, 256);
        Huffman dHuff = Huffman.singleSymbol(0, 40);
        gHuff.writeTable(bits, 280);
        rHuff.writeTable(bits, 256);
        bHuff.writeTable(bits, 256);
        aHuff.writeTable(bits, 256);
        dHuff.writeTable(bits, 40);
        for (int index = 0; index < pixelCount; index++) {
            gHuff.writeSymbol(bits, green[index]);
            rHuff.writeSymbol(bits, red[index]);
            bHuff.writeSymbol(bits, blue[index]);
            aHuff.writeSymbol(bits, alpha[index]);
        }
        return wrapVp8l(bits.toArray());
    }

    /// Encodes RGBA with the VP8L subtract-green transform.
    ///
    /// @param width the positive pixel width
    /// @param height the positive pixel height
    /// @param rgba `width * height * 4` bytes
    /// @return the WebP bytes
    public static byte @Unmodifiable [] encodeSubtractGreen(int width, int height, byte[] rgba) {
        Objects.requireNonNull(rgba, "rgba");
        int pixelCount = checkedPixelCount(width, height);
        if (rgba.length != pixelCount * 4) {
            throw new IllegalArgumentException("RGBA length must be width * height * 4");
        }
        LsbWriter bits = new LsbWriter();
        bits.write(VP8L_MAGIC, 8);
        bits.write(width - 1, 14);
        bits.write(height - 1, 14);
        bits.write(1, 1);
        bits.write(0, 3);
        bits.write(1, 1);
        bits.write(TRANSFORM_SUBTRACT_GREEN, 2);
        bits.write(0, 1);
        bits.write(0, 1);
        int[] green = new int[pixelCount];
        int[] red = new int[pixelCount];
        int[] blue = new int[pixelCount];
        int[] alpha = new int[pixelCount];
        for (int index = 0; index < pixelCount; index++) {
            int offset = index * 4;
            int g = rgba[offset + 1] & 0xFF;
            red[index] = ((rgba[offset] & 0xFF) - g) & 0xFF;
            green[index] = g;
            blue[index] = ((rgba[offset + 2] & 0xFF) - g) & 0xFF;
            alpha[index] = rgba[offset + 3] & 0xFF;
        }
        Huffman gHuff = Huffman.fromSymbols(green, 280);
        Huffman rHuff = Huffman.fromSymbols(red, 256);
        Huffman bHuff = Huffman.fromSymbols(blue, 256);
        Huffman aHuff = Huffman.fromSymbols(alpha, 256);
        Huffman dHuff = Huffman.singleSymbol(0, 40);
        gHuff.writeTable(bits, 280);
        rHuff.writeTable(bits, 256);
        bHuff.writeTable(bits, 256);
        aHuff.writeTable(bits, 256);
        dHuff.writeTable(bits, 40);
        for (int index = 0; index < pixelCount; index++) {
            gHuff.writeSymbol(bits, green[index]);
            rHuff.writeSymbol(bits, red[index]);
            bHuff.writeSymbol(bits, blue[index]);
            aHuff.writeSymbol(bits, alpha[index]);
        }
        return wrapVp8l(bits.toArray());
    }

    /// Encodes RGBA with a 16-entry VP8L color cache.
    ///
    /// @param width the positive pixel width
    /// @param height the positive pixel height
    /// @param rgba `width * height * 4` bytes
    /// @return the WebP bytes
    public static byte @Unmodifiable [] encodeWithColorCache(int width, int height, byte[] rgba) {
        Objects.requireNonNull(rgba, "rgba");
        int pixelCount = checkedPixelCount(width, height);
        if (rgba.length != pixelCount * 4) {
            throw new IllegalArgumentException("RGBA length must be width * height * 4");
        }
        LsbWriter bits = new LsbWriter();
        bits.write(VP8L_MAGIC, 8);
        bits.write(width - 1, 14);
        bits.write(height - 1, 14);
        bits.write(1, 1);
        bits.write(0, 3);
        bits.write(0, 1);
        bits.write(1, 1);
        bits.write(4, 4);
        int[] green = new int[pixelCount];
        int[] red = new int[pixelCount];
        int[] blue = new int[pixelCount];
        int[] alpha = new int[pixelCount];
        int[] greenCodes = new int[pixelCount];
        boolean[] literal = new boolean[pixelCount];
        int[] cache = new int[16];
        boolean[] filled = new boolean[16];
        for (int index = 0; index < pixelCount; index++) {
            int offset = index * 4;
            red[index] = rgba[offset] & 0xFF;
            green[index] = rgba[offset + 1] & 0xFF;
            blue[index] = rgba[offset + 2] & 0xFF;
            alpha[index] = rgba[offset + 3] & 0xFF;
            int argb = (alpha[index] << 24) | (red[index] << 16) | (green[index] << 8) | blue[index];
            int slot = cacheKey(argb, 4);
            if (filled[slot] && cache[slot] == argb) {
                greenCodes[index] = 280 + slot;
                literal[index] = false;
            } else {
                greenCodes[index] = green[index];
                literal[index] = true;
            }
            insertCache(cache, filled, 4, argb);
        }
        Huffman gHuff = Huffman.fromSymbols(greenCodes, 296);
        Huffman rHuff = Huffman.fromSymbols(usedChannel(red, literal), 256);
        Huffman bHuff = Huffman.fromSymbols(usedChannel(blue, literal), 256);
        Huffman aHuff = Huffman.fromSymbols(usedChannel(alpha, literal), 256);
        Huffman dHuff = Huffman.singleSymbol(0, 40);
        gHuff.writeTable(bits, 296);
        rHuff.writeTable(bits, 256);
        bHuff.writeTable(bits, 256);
        aHuff.writeTable(bits, 256);
        dHuff.writeTable(bits, 40);
        for (int index = 0; index < pixelCount; index++) {
            gHuff.writeSymbol(bits, greenCodes[index]);
            if (literal[index]) {
                rHuff.writeSymbol(bits, red[index]);
                bHuff.writeSymbol(bits, blue[index]);
                aHuff.writeSymbol(bits, alpha[index]);
            }
        }
        return wrapVp8l(bits.toArray());
    }

    /// Encodes RGBA as one literal plus an LZ77 copy of the remaining pixels.
    ///
    /// @param width the positive pixel width
    /// @param height the positive pixel height
    /// @param rgba `width * height * 4` bytes
    /// @return the WebP bytes
    public static byte @Unmodifiable [] encodeWithLz77(int width, int height, byte[] rgba) {
        Objects.requireNonNull(rgba, "rgba");
        int pixelCount = checkedPixelCount(width, height);
        if (rgba.length != pixelCount * 4 || pixelCount < 2) {
            throw new IllegalArgumentException("VP8L LZ77 encode requires at least two RGBA pixels");
        }
        int[] green = {rgba[1] & 0xFF, 256 + lengthPrefix(pixelCount - 1)};
        int[] red = {rgba[0] & 0xFF};
        int[] blue = {rgba[2] & 0xFF};
        int[] alpha = {rgba[3] & 0xFF};
        int[] dist = {1};
        Huffman gHuff = Huffman.fromSymbols(green, 280);
        Huffman rHuff = Huffman.fromSymbols(red, 256);
        Huffman bHuff = Huffman.fromSymbols(blue, 256);
        Huffman aHuff = Huffman.fromSymbols(alpha, 256);
        Huffman dHuff = Huffman.fromSymbols(dist, 40);
        LsbWriter bits = new LsbWriter();
        bits.write(VP8L_MAGIC, 8);
        bits.write(width - 1, 14);
        bits.write(height - 1, 14);
        bits.write(1, 1);
        bits.write(0, 3);
        bits.write(0, 1);
        bits.write(0, 1);
        gHuff.writeTable(bits, 280);
        rHuff.writeTable(bits, 256);
        bHuff.writeTable(bits, 256);
        aHuff.writeTable(bits, 256);
        dHuff.writeTable(bits, 40);
        gHuff.writeSymbol(bits, green[0]);
        rHuff.writeSymbol(bits, red[0]);
        bHuff.writeSymbol(bits, blue[0]);
        aHuff.writeSymbol(bits, alpha[0]);
        gHuff.writeSymbol(bits, green[1]);
        writePrefixExtra(bits, pixelCount - 1);
        dHuff.writeSymbol(bits, 1);
        writePrefixExtra(bits, 2);
        return wrapVp8l(bits.toArray());
    }

    /// Encodes RGBA with the VP8L left predictor on a single 4-pixel tile.
    ///
    /// @param width the positive pixel width
    /// @param height the positive pixel height
    /// @param rgba `width * height * 4` bytes
    /// @return the WebP bytes
    public static byte @Unmodifiable [] encodePredictor(int width, int height, byte[] rgba) {
        Objects.requireNonNull(rgba, "rgba");
        int pixelCount = checkedPixelCount(width, height);
        if (rgba.length != pixelCount * 4) {
            throw new IllegalArgumentException("RGBA length must be width * height * 4");
        }
        int[] argb = rgbaToArgb(rgba);
        int[] residual = new int[pixelCount];
        int[] reconstructed = new int[pixelCount];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = y * width + x;
                int pred = predicted(reconstructed, width, x, y, 1);
                residual[index] = subPixels(argb[index], pred);
                reconstructed[index] = addPixels(residual[index], pred);
            }
        }
        LsbWriter bits = new LsbWriter();
        writeVp8lHeader(bits, width, height);
        bits.write(1, 1);
        bits.write(TRANSFORM_PREDICTOR, 2);
        bits.write(0, 3);
        writeEntropyImage(bits, new int[] {packArgb(255, 0, 1, 0)});
        bits.write(0, 1);
        bits.write(0, 1);
        writePixelData(bits, residual);
        return wrapVp8l(bits.toArray());
    }

    /// Encodes RGBA with a color transform whose `green_to_red` multiplier is 32.
    ///
    /// @param width the positive pixel width
    /// @param height the positive pixel height
    /// @param rgba `width * height * 4` bytes
    /// @return the WebP bytes
    public static byte @Unmodifiable [] encodeColor(int width, int height, byte[] rgba) {
        Objects.requireNonNull(rgba, "rgba");
        int pixelCount = checkedPixelCount(width, height);
        if (rgba.length != pixelCount * 4) {
            throw new IllegalArgumentException("RGBA length must be width * height * 4");
        }
        int[] residual = new int[pixelCount];
        for (int index = 0; index < pixelCount; index++) {
            int offset = index * 4;
            int red = rgba[offset] & 0xFF;
            int green = rgba[offset + 1] & 0xFF;
            int blue = rgba[offset + 2] & 0xFF;
            int alpha = rgba[offset + 3] & 0xFF;
            int newRed = (red - colorDelta(32, green)) & 0xFF;
            int newBlue = (blue - colorDelta(0, green)) & 0xFF;
            newBlue = (newBlue - colorDelta(0, red)) & 0xFF;
            residual[index] = packArgb(alpha, newRed, green, newBlue);
        }
        LsbWriter bits = new LsbWriter();
        writeVp8lHeader(bits, width, height);
        bits.write(1, 1);
        bits.write(TRANSFORM_COLOR, 2);
        bits.write(0, 3);
        writeEntropyImage(bits, new int[] {packArgb(255, 0, 0, 32)});
        bits.write(0, 1);
        bits.write(0, 1);
        writePixelData(bits, residual);
        return wrapVp8l(bits.toArray());
    }

    /// Encodes RGBA with the VP8L color-indexing transform.
    ///
    /// @param width the positive pixel width
    /// @param height the positive pixel height
    /// @param rgba `width * height * 4` bytes
    /// @return the WebP bytes
    public static byte @Unmodifiable [] encodeIndexing(int width, int height, byte[] rgba) {
        Objects.requireNonNull(rgba, "rgba");
        int pixelCount = checkedPixelCount(width, height);
        if (rgba.length != pixelCount * 4) {
            throw new IllegalArgumentException("RGBA length must be width * height * 4");
        }
        int[] argb = rgbaToArgb(rgba);
        int[] table = uniqueColors(argb);
        if (table.length > 256) {
            throw new IllegalArgumentException("VP8L color indexing requires at most 256 colors");
        }
        int widthBits = indexingWidthBits(table.length);
        int packedWidth = divRoundUp(width, 1 << widthBits);
        int bitsPerPixel = 8 >> widthBits;
        int group = 1 << widthBits;
        int[] packed = new int[packedWidth * height];
        for (int y = 0; y < height; y++) {
            for (int packedX = 0; packedX < packedWidth; packedX++) {
                int green = 0;
                for (int slot = 0; slot < group; slot++) {
                    int x = packedX * group + slot;
                    if (x >= width) {
                        break;
                    }
                    green |= indexOf(table, argb[y * width + x]) << (slot * bitsPerPixel);
                }
                packed[y * packedWidth + packedX] = packArgb(255, 0, green, 0);
            }
        }
        LsbWriter bits = new LsbWriter();
        writeVp8lHeader(bits, width, height);
        bits.write(1, 1);
        bits.write(TRANSFORM_INDEXING, 2);
        bits.write(table.length - 1, 8);
        writeEntropyImage(bits, encodeColorTable(table));
        bits.write(0, 1);
        bits.write(0, 1);
        writePixelData(bits, packed);
        return wrapVp8l(bits.toArray());
    }

    /// Decodes a lossless WebP stream into row-major unassociated RGBA8.
    ///
    /// @param bytes the WebP stream
    /// @return the decoded image
    public static Decoded decode(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes");
        if (!isWebp(bytes)) {
            throw new IllegalArgumentException("WebP VP8L signature is missing");
        }
        int payloadSize = readLe(bytes, 16);
        if (payloadSize < 5 || 20 + payloadSize > bytes.length) {
            throw new IllegalArgumentException("WebP VP8L chunk is truncated");
        }
        LsbReader bits = new LsbReader(bytes, 20, payloadSize);
        if (bits.read(8) != VP8L_MAGIC) {
            throw new IllegalArgumentException("VP8L signature byte is missing");
        }
        int width = bits.read(14) + 1;
        int height = bits.read(14) + 1;
        bits.read(1);
        if (bits.read(3) != 0) {
            throw new IllegalArgumentException("VP8L version must be 0");
        }
        checkedPixelCount(width, height);
        int decodeWidth = width;
        int decodeHeight = height;
        ArrayList<PendingTransform> transforms = new ArrayList<>();
        while (bits.read(1) != 0) {
            int type = bits.read(2);
            if (type == TRANSFORM_SUBTRACT_GREEN) {
                transforms.add(PendingTransform.subtractGreen());
            } else if (type == TRANSFORM_PREDICTOR || type == TRANSFORM_COLOR) {
                int sizeBits = bits.read(3) + 2;
                int tilesWide = divRoundUp(decodeWidth, 1 << sizeBits);
                int tilesHigh = divRoundUp(decodeHeight, 1 << sizeBits);
                int[] tiles = decodeArgb(bits, tilesWide, tilesHigh);
                transforms.add(PendingTransform.spatial(type, sizeBits, tilesWide, tiles));
            } else if (type == TRANSFORM_INDEXING) {
                int tableSize = bits.read(8) + 1;
                int[] table = decodeArgb(bits, tableSize, 1);
                invertColorTable(table);
                int widthBits = indexingWidthBits(tableSize);
                transforms.add(PendingTransform.indexing(table, widthBits, decodeWidth));
                decodeWidth = divRoundUp(decodeWidth, 1 << widthBits);
            } else {
                throw new IllegalArgumentException("VP8L transform type is invalid");
            }
        }
        int cacheBits = 0;
        if (bits.read(1) != 0) {
            cacheBits = bits.read(4);
            if (cacheBits < 1 || cacheBits > 11) {
                throw new IllegalArgumentException("VP8L color-cache bits must be in 1..11");
            }
        }
        int[] argb = decodeArgb(bits, decodeWidth, decodeHeight, cacheBits);
        int currentWidth = decodeWidth;
        for (int index = transforms.size() - 1; index >= 0; index--) {
            PendingTransform transform = transforms.get(index);
            if (transform.type == TRANSFORM_SUBTRACT_GREEN) {
                addGreen(argb);
            } else if (transform.type == TRANSFORM_PREDICTOR) {
                invertPredictor(argb, currentWidth, decodeHeight, transform);
            } else if (transform.type == TRANSFORM_COLOR) {
                invertColor(argb, currentWidth, decodeHeight, transform);
            } else if (transform.type == TRANSFORM_INDEXING) {
                argb = invertIndexing(argb, currentWidth, decodeHeight, transform);
                currentWidth = transform.originalWidth;
            }
        }
        byte[] rgba = new byte[argb.length * 4];
        for (int index = 0; index < argb.length; index++) {
            int color = argb[index];
            int offset = index * 4;
            rgba[offset] = (byte) redOf(color);
            rgba[offset + 1] = (byte) greenOf(color);
            rgba[offset + 2] = (byte) blueOf(color);
            rgba[offset + 3] = (byte) alphaOf(color);
        }
        return new Decoded(width, height, rgba);
    }

    /// Decodes one entropy-coded ARGB image used by the main raster or transform data.
    private static int[] decodeArgb(LsbReader bits, int width, int height) {
        int cacheBits = 0;
        if (bits.read(1) != 0) {
            cacheBits = bits.read(4);
        }
        return decodeArgb(bits, width, height, cacheBits);
    }

    /// Decodes pixels with the supplied color-cache size.
    private static int[] decodeArgb(LsbReader bits, int width, int height, int cacheBits) {
        int cacheSize = cacheBits == 0 ? 0 : 1 << cacheBits;
        int greenAlphabet = 256 + 24 + cacheSize;
        Huffman green = Huffman.readTable(bits, greenAlphabet);
        Huffman red = Huffman.readTable(bits, 256);
        Huffman blue = Huffman.readTable(bits, 256);
        Huffman alpha = Huffman.readTable(bits, 256);
        Huffman distance = Huffman.readTable(bits, 40);
        int pixelCount = width * height;
        int[] pixels = new int[pixelCount];
        int[] cache = new int[Math.max(1, cacheSize)];
        boolean[] cacheFilled = new boolean[Math.max(1, cacheSize)];
        int index = 0;
        while (index < pixelCount) {
            int code = green.readSymbol(bits);
            if (code < 256) {
                int redValue = red.readSymbol(bits);
                int blueValue = blue.readSymbol(bits);
                int alphaValue = alpha.readSymbol(bits);
                int color = (alphaValue << 24) | (redValue << 16) | (code << 8) | blueValue;
                pixels[index++] = color;
                insertCache(cache, cacheFilled, cacheBits, color);
                continue;
            }
            if (code < 256 + 24) {
                int length = prefixValue(code - 256, bits);
                int distCode = prefixValue(distance.readSymbol(bits), bits);
                int dist = scanDistance(distCode, width);
                if (dist < 1 || dist > index) {
                    throw new IllegalArgumentException("VP8L LZ77 distance is out of range");
                }
                for (int copied = 0; copied < length && index < pixelCount; copied++) {
                    int color = pixels[index - dist];
                    pixels[index++] = color;
                    insertCache(cache, cacheFilled, cacheBits, color);
                }
                continue;
            }
            int slot = code - 256 - 24;
            if (slot < 0 || slot >= cacheSize) {
                throw new IllegalArgumentException("VP8L color-cache index is out of range");
            }
            pixels[index++] = cache[slot];
        }
        return pixels;
    }

    /// Writes the VP8L signature, size, alpha hint, and version.
    private static void writeVp8lHeader(LsbWriter bits, int width, int height) {
        bits.write(VP8L_MAGIC, 8);
        bits.write(width - 1, 14);
        bits.write(height - 1, 14);
        bits.write(1, 1);
        bits.write(0, 3);
    }

    /// Writes an entropy-coded image used as transform data.
    private static void writeEntropyImage(LsbWriter bits, int[] argb) {
        bits.write(0, 1);
        writePixelData(bits, argb);
    }

    /// Writes Huffman tables and literal pixels with no color cache.
    private static void writePixelData(LsbWriter bits, int[] argb) {
        int[] green = new int[argb.length];
        int[] red = new int[argb.length];
        int[] blue = new int[argb.length];
        int[] alpha = new int[argb.length];
        for (int index = 0; index < argb.length; index++) {
            int color = argb[index];
            red[index] = redOf(color);
            green[index] = greenOf(color);
            blue[index] = blueOf(color);
            alpha[index] = alphaOf(color);
        }
        Huffman gHuff = Huffman.fromSymbols(green, 280);
        Huffman rHuff = Huffman.fromSymbols(red, 256);
        Huffman bHuff = Huffman.fromSymbols(blue, 256);
        Huffman aHuff = Huffman.fromSymbols(alpha, 256);
        Huffman dHuff = Huffman.singleSymbol(0, 40);
        gHuff.writeTable(bits, 280);
        rHuff.writeTable(bits, 256);
        bHuff.writeTable(bits, 256);
        aHuff.writeTable(bits, 256);
        dHuff.writeTable(bits, 40);
        for (int index = 0; index < argb.length; index++) {
            gHuff.writeSymbol(bits, green[index]);
            rHuff.writeSymbol(bits, red[index]);
            bHuff.writeSymbol(bits, blue[index]);
            aHuff.writeSymbol(bits, alpha[index]);
        }
    }

    /// Adds green to red and blue of every pixel.
    private static void addGreen(int[] argb) {
        for (int index = 0; index < argb.length; index++) {
            int color = argb[index];
            int green = greenOf(color);
            argb[index] = packArgb(
                    alphaOf(color),
                    (redOf(color) + green) & 0xFF,
                    green,
                    (blueOf(color) + green) & 0xFF
            );
        }
    }

    /// Reconstructs pixels from predictor residuals in scan-line order.
    private static void invertPredictor(int[] argb, int width, int height, PendingTransform transform) {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int tile = (y >> transform.sizeBits) * transform.blocksWide + (x >> transform.sizeBits);
                int mode = greenOf(transform.data[tile]);
                int pred = predicted(argb, width, x, y, mode);
                int index = y * width + x;
                argb[index] = addPixels(argb[index], pred);
            }
        }
    }

    /// Adds the color-transform deltas stored in `transform`.
    private static void invertColor(int[] argb, int width, int height, PendingTransform transform) {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int tile = (y >> transform.sizeBits) * transform.blocksWide + (x >> transform.sizeBits);
                int element = transform.data[tile];
                int greenToRed = blueOf(element);
                int greenToBlue = greenOf(element);
                int redToBlue = redOf(element);
                int index = y * width + x;
                int color = argb[index];
                int green = greenOf(color);
                int red = (redOf(color) + colorDelta(greenToRed, green)) & 0xFF;
                int blue = (blueOf(color) + colorDelta(greenToBlue, green)) & 0xFF;
                blue = (blue + colorDelta(redToBlue, red)) & 0xFF;
                argb[index] = packArgb(alphaOf(color), red, green, blue);
            }
        }
    }

    /// Expands packed color indices using the reconstructed color table.
    private static int[] invertIndexing(int[] packed, int packedWidth, int height, PendingTransform transform) {
        int width = transform.originalWidth;
        int[] argb = new int[width * height];
        int bitsPerPixel = 8 >> transform.widthBits;
        int mask = (1 << bitsPerPixel) - 1;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int srcX = x >> transform.widthBits;
                int slot = x & ((1 << transform.widthBits) - 1);
                int color = packed[y * packedWidth + srcX];
                int index = (greenOf(color) >> (slot * bitsPerPixel)) & mask;
                argb[y * width + x] = index < transform.data.length ? transform.data[index] : 0;
            }
        }
        return argb;
    }

    /// Applies the inverse delta coding of a color-indexing table.
    private static void invertColorTable(int[] table) {
        addGreen(table);
        for (int index = 1; index < table.length; index++) {
            table[index] = addPixels(table[index], table[index - 1]);
        }
    }

    /// Delta-codes `table` and subtracts green so [`#invertColorTable(int[])`] can recover it.
    private static int[] encodeColorTable(int[] table) {
        int[] stored = new int[table.length];
        stored[0] = table[0];
        for (int index = 1; index < table.length; index++) {
            stored[index] = subPixels(table[index], table[index - 1]);
        }
        for (int index = 0; index < stored.length; index++) {
            int color = stored[index];
            int green = greenOf(color);
            stored[index] = packArgb(
                    alphaOf(color),
                    (redOf(color) - green) & 0xFF,
                    green,
                    (blueOf(color) - green) & 0xFF
            );
        }
        return stored;
    }

    /// Predicts the pixel at `(x, y)` from already reconstructed neighbors.
    private static int predicted(int[] pixels, int width, int x, int y, int mode) {
        if (x == 0 && y == 0) {
            return ARGB_BLACK;
        }
        if (y == 0) {
            return pixels[x - 1];
        }
        if (x == 0) {
            return pixels[(y - 1) * width];
        }
        int left = pixels[y * width + x - 1];
        int top = pixels[(y - 1) * width + x];
        int topLeft = pixels[(y - 1) * width + x - 1];
        int topRight = x + 1 < width
                ? pixels[(y - 1) * width + x + 1]
                : pixels[(y - 1) * width];
        return switch (mode) {
            case 0 -> ARGB_BLACK;
            case 1 -> left;
            case 2 -> top;
            case 3 -> topRight;
            case 4 -> topLeft;
            case 5 -> average2(average2(left, topRight), top);
            case 6 -> average2(left, topLeft);
            case 7 -> average2(left, top);
            case 8 -> average2(topLeft, top);
            case 9 -> average2(top, topRight);
            case 10 -> average2(average2(left, topLeft), average2(top, topRight));
            case 11 -> select(left, top, topLeft);
            case 12 -> clampAddSubtractFull(left, top, topLeft);
            case 13 -> clampAddSubtractHalf(average2(left, top), topLeft);
            default -> throw new IllegalArgumentException("VP8L predictor mode is invalid");
        };
    }

    /// Averages each ARGB channel of `left` and `right`.
    private static int average2(int left, int right) {
        return packArgb(
                (alphaOf(left) + alphaOf(right)) / 2,
                (redOf(left) + redOf(right)) / 2,
                (greenOf(left) + greenOf(right)) / 2,
                (blueOf(left) + blueOf(right)) / 2
        );
    }

    /// Returns the neighbor closer to the `L + T - TL` estimate.
    private static int select(int left, int top, int topLeft) {
        int pAlpha = alphaOf(left) + alphaOf(top) - alphaOf(topLeft);
        int pRed = redOf(left) + redOf(top) - redOf(topLeft);
        int pGreen = greenOf(left) + greenOf(top) - greenOf(topLeft);
        int pBlue = blueOf(left) + blueOf(top) - blueOf(topLeft);
        int pL = Math.abs(pAlpha - alphaOf(left)) + Math.abs(pRed - redOf(left))
                + Math.abs(pGreen - greenOf(left)) + Math.abs(pBlue - blueOf(left));
        int pT = Math.abs(pAlpha - alphaOf(top)) + Math.abs(pRed - redOf(top))
                + Math.abs(pGreen - greenOf(top)) + Math.abs(pBlue - blueOf(top));
        return pL < pT ? left : top;
    }

    /// Clamps `a + b - c` per channel into `0..255`.
    private static int clampAddSubtractFull(int left, int top, int topLeft) {
        return packArgb(
                clamp(alphaOf(left) + alphaOf(top) - alphaOf(topLeft)),
                clamp(redOf(left) + redOf(top) - redOf(topLeft)),
                clamp(greenOf(left) + greenOf(top) - greenOf(topLeft)),
                clamp(blueOf(left) + blueOf(top) - blueOf(topLeft))
        );
    }

    /// Clamps `a + (a - b) / 2` per channel into `0..255`.
    private static int clampAddSubtractHalf(int average, int topLeft) {
        return packArgb(
                clamp(alphaOf(average) + (alphaOf(average) - alphaOf(topLeft)) / 2),
                clamp(redOf(average) + (redOf(average) - redOf(topLeft)) / 2),
                clamp(greenOf(average) + (greenOf(average) - greenOf(topLeft)) / 2),
                clamp(blueOf(average) + (blueOf(average) - blueOf(topLeft)) / 2)
        );
    }

    /// Clamps `value` into `0..255`.
    private static int clamp(int value) {
        return value < 0 ? 0 : Math.min(value, 255);
    }

    /// Signed 3.5-fixed-point color-transform delta.
    private static int colorDelta(int transform, int color) {
        return ((byte) transform * (byte) color) >> 5;
    }

    /// Packs ARGB channels into one pixel.
    private static int packArgb(int alpha, int red, int green, int blue) {
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    /// Adds each channel of `residual` and `pred` modulo 256.
    private static int addPixels(int residual, int pred) {
        return packArgb(
                (alphaOf(residual) + alphaOf(pred)) & 0xFF,
                (redOf(residual) + redOf(pred)) & 0xFF,
                (greenOf(residual) + greenOf(pred)) & 0xFF,
                (blueOf(residual) + blueOf(pred)) & 0xFF
        );
    }

    /// Subtracts each channel of `pred` from `pixel` modulo 256.
    private static int subPixels(int pixel, int pred) {
        return packArgb(
                (alphaOf(pixel) - alphaOf(pred)) & 0xFF,
                (redOf(pixel) - redOf(pred)) & 0xFF,
                (greenOf(pixel) - greenOf(pred)) & 0xFF,
                (blueOf(pixel) - blueOf(pred)) & 0xFF
        );
    }

    /// Converts packed RGBA bytes to packed ARGB pixels.
    private static int[] rgbaToArgb(byte[] rgba) {
        int[] argb = new int[rgba.length / 4];
        for (int index = 0; index < argb.length; index++) {
            int offset = index * 4;
            argb[index] = packArgb(
                    rgba[offset + 3] & 0xFF,
                    rgba[offset] & 0xFF,
                    rgba[offset + 1] & 0xFF,
                    rgba[offset + 2] & 0xFF
            );
        }
        return argb;
    }

    /// Returns distinct colors in first-seen order.
    private static int[] uniqueColors(int[] argb) {
        int[] table = new int[Math.min(256, argb.length)];
        int count = 0;
        for (int color : argb) {
            if (indexOf(table, count, color) < 0) {
                if (count == table.length) {
                    throw new IllegalArgumentException("VP8L color indexing requires at most 256 colors");
                }
                table[count++] = color;
            }
        }
        return Arrays.copyOf(table, count);
    }

    /// Returns the index of `color` in `table`, or `-1`.
    private static int indexOf(int[] table, int color) {
        return indexOf(table, table.length, color);
    }

    /// Returns the index of `color` in the first `length` entries of `table`, or `-1`.
    private static int indexOf(int[] table, int length, int color) {
        for (int index = 0; index < length; index++) {
            if (table[index] == color) {
                return index;
            }
        }
        return -1;
    }

    /// Returns the packed-pixel width bits for a color table of `tableSize`.
    private static int indexingWidthBits(int tableSize) {
        if (tableSize <= 2) {
            return 3;
        }
        if (tableSize <= 4) {
            return 2;
        }
        if (tableSize <= 16) {
            return 1;
        }
        return 0;
    }

    /// Ceiling division of `num` by `den`.
    private static int divRoundUp(int num, int den) {
        return (num + den - 1) / den;
    }

    /// Converts a VP8L distance code into a scan-line distance.
    private static int scanDistance(int distanceCode, int width) {
        if (distanceCode > 120) {
            return distanceCode - 120;
        }
        if (distanceCode < 1 || distanceCode > DIST_DX.length) {
            throw new IllegalArgumentException("VP8L distance code is invalid");
        }
        int dist = DIST_DX[distanceCode - 1] + DIST_DY[distanceCode - 1] * width;
        return Math.max(1, dist);
    }

    /// Reads a VP8L prefix extra-bit integer.
    private static int prefixValue(int prefix, LsbReader bits) {
        if (prefix < 4) {
            return prefix + 1;
        }
        int extra = (prefix - 2) >> 1;
        int offset = (2 + (prefix & 1)) << extra;
        return offset + bits.read(extra) + 1;
    }

    /// Returns the prefix code for `value` without writing extra bits.
    private static int lengthPrefix(int value) {
        if (value < 1) {
            throw new IllegalArgumentException("VP8L prefix value must be positive");
        }
        if (value <= 4) {
            return value - 1;
        }
        int extra = 1;
        while (((2 + 1) << extra) + 1 <= value) {
            extra++;
        }
        extra--;
        int evenOffset = 2 << extra;
        return value < evenOffset + (1 << extra) + 1 ? (extra + 1) * 2 : (extra + 1) * 2 + 1;
    }

    /// Writes extra bits for a VP8L prefix-coded integer.
    private static void writePrefixExtra(LsbWriter bits, int value) {
        if (value <= 4) {
            return;
        }
        int prefix = lengthPrefix(value);
        int extra = (prefix - 2) >> 1;
        int offset = (2 + (prefix & 1)) << extra;
        bits.write(value - offset - 1, extra);
    }

    /// Wraps a VP8L payload in a RIFF/WEBP chunk.
    private static byte[] wrapVp8l(byte[] payload) {
        byte[] output = new byte[20 + payload.length + (payload.length & 1)];
        writeBe(output, 0, RIFF);
        writeLe(output, 4, output.length - 8);
        writeBe(output, 8, WEBP);
        writeBe(output, 12, VP8L);
        writeLe(output, 16, payload.length);
        System.arraycopy(payload, 0, output, 20, payload.length);
        return output;
    }

    /// Inserts `argb` into the color cache when the cache is enabled.
    private static void insertCache(int[] cache, boolean[] filled, int cacheBits, int argb) {
        if (cacheBits == 0) {
            return;
        }
        int slot = cacheKey(argb, cacheBits);
        cache[slot] = argb;
        filled[slot] = true;
    }

    /// Returns the VP8L color-cache slot for `argb`.
    private static int cacheKey(int argb, int cacheBits) {
        return (argb * CACHE_HASH) >>> (32 - cacheBits);
    }

    /// Returns the red channel of a packed ARGB pixel.
    private static int redOf(int argb) {
        return (argb >>> 16) & 0xFF;
    }

    /// Returns the green channel of a packed ARGB pixel.
    private static int greenOf(int argb) {
        return (argb >>> 8) & 0xFF;
    }

    /// Returns the blue channel of a packed ARGB pixel.
    private static int blueOf(int argb) {
        return argb & 0xFF;
    }

    /// Returns the alpha channel of a packed ARGB pixel.
    private static int alphaOf(int argb) {
        return (argb >>> 24) & 0xFF;
    }

    /// Collects channel symbols for pixels emitted as literals.
    private static int[] usedChannel(int[] symbols, boolean[] literal) {
        int used = 0;
        for (boolean present : literal) {
            if (present) {
                used++;
            }
        }
        if (used == 0) {
            return new int[] {0};
        }
        int[] selected = new int[used];
        int dest = 0;
        for (int index = 0; index < symbols.length; index++) {
            if (literal[index]) {
                selected[dest++] = symbols[index];
            }
        }
        return selected;
    }

    /// Returns `width * height` after rejecting non-positive or oversized images.
    private static int checkedPixelCount(int width, int height) {
        if (width <= 0 || height <= 0 || width > PixelBuffer.MAX_EDGE || height > PixelBuffer.MAX_EDGE) {
            throw new IllegalArgumentException("WebP dimensions must be in (0, " + PixelBuffer.MAX_EDGE + "]");
        }
        return Math.multiplyExact(width, height);
    }

    /// Reads a big-endian 32-bit integer.
    private static int readBe(byte[] bytes, int offset) {
        return (bytes[offset] & 0xFF) << 24
                | (bytes[offset + 1] & 0xFF) << 16
                | (bytes[offset + 2] & 0xFF) << 8
                | (bytes[offset + 3] & 0xFF);
    }

    /// Reads a little-endian 32-bit integer.
    private static int readLe(byte[] bytes, int offset) {
        return (bytes[offset] & 0xFF)
                | (bytes[offset + 1] & 0xFF) << 8
                | (bytes[offset + 2] & 0xFF) << 16
                | (bytes[offset + 3] & 0xFF) << 24;
    }

    /// Writes a big-endian 32-bit integer.
    private static void writeBe(byte[] bytes, int offset, int value) {
        bytes[offset] = (byte) (value >>> 24);
        bytes[offset + 1] = (byte) (value >>> 16);
        bytes[offset + 2] = (byte) (value >>> 8);
        bytes[offset + 3] = (byte) value;
    }

    /// Writes a little-endian 32-bit integer.
    private static void writeLe(byte[] bytes, int offset, int value) {
        bytes[offset] = (byte) value;
        bytes[offset + 1] = (byte) (value >>> 8);
        bytes[offset + 2] = (byte) (value >>> 16);
        bytes[offset + 3] = (byte) (value >>> 24);
    }

    /// One VP8L transform decoded from the bitstream, applied after the main raster.
    private static final class PendingTransform {
        /// Transform type from the VP8L bitstream.
        private final int type;

        /// `size_bits` for predictor and color tiles.
        private final int sizeBits;

        /// Number of tiles across the image.
        private final int blocksWide;

        /// Predictor modes, color-transform elements, or the indexing table.
        private final int[] data;

        /// Packed-pixel width bits for color indexing.
        private final int widthBits;

        /// Image width before color indexing subsampled the raster.
        private final int originalWidth;

        /// Creates a transform record.
        private PendingTransform(int type, int sizeBits, int blocksWide, int[] data, int widthBits, int originalWidth) {
            this.type = type;
            this.sizeBits = sizeBits;
            this.blocksWide = blocksWide;
            this.data = data;
            this.widthBits = widthBits;
            this.originalWidth = originalWidth;
        }

        /// Subtract-green has no extra data.
        private static PendingTransform subtractGreen() {
            return new PendingTransform(TRANSFORM_SUBTRACT_GREEN, 0, 0, new int[0], 0, 0);
        }

        /// Predictor or color transform over a tile image.
        private static PendingTransform spatial(int type, int sizeBits, int blocksWide, int[] data) {
            return new PendingTransform(type, sizeBits, blocksWide, data, 0, 0);
        }

        /// Color-indexing transform with a reconstructed table.
        private static PendingTransform indexing(int[] table, int widthBits, int originalWidth) {
            return new PendingTransform(TRANSFORM_INDEXING, 0, 0, table, widthBits, originalWidth);
        }
    }

    /// Stores one decoded WebP image.
    ///
    /// @param width the pixel width
    /// @param height the pixel height
    /// @param rgba RGBA8 pixels
    public record Decoded(int width, int height, byte @Unmodifiable [] rgba) {
        /// Validates the decoded image.
        public Decoded {
            if (width <= 0 || height <= 0) {
                throw new IllegalArgumentException("Decoded size must be positive");
            }
            Objects.requireNonNull(rgba, "rgba");
            if (rgba.length != Math.multiplyExact(width, height) * 4) {
                throw new IllegalArgumentException("RGBA length must match width * height * 4");
            }
            rgba = Arrays.copyOf(rgba, rgba.length);
        }
    }

    /// Canonical Huffman table used by VP8L.
    private static final class Huffman {
        /// Code for each symbol.
        private final int[] codes;

        /// Length for each symbol.
        private final int[] lengths;

        /// Maximum code length.
        private final int maxLength;

        /// Decode table indexed by a `maxLength`-bit peek.
        private final int[] decodeSymbol;

        /// Decode lengths matching [`decodeSymbol`].
        private final int[] decodeLength;

        /// Creates a table from code lengths.
        private Huffman(int[] lengths) {
            this.lengths = lengths;
            this.codes = new int[lengths.length];
            int max = 0;
            for (int length : lengths) {
                max = Math.max(max, length);
            }
            this.maxLength = Math.max(1, max);
            assignCanonical(lengths, codes);
            int size = 1 << maxLength;
            this.decodeSymbol = new int[size];
            this.decodeLength = new int[size];
            Arrays.fill(decodeSymbol, -1);
            for (int symbol = 0; symbol < lengths.length; symbol++) {
                int length = lengths[symbol];
                if (length == 0) {
                    continue;
                }
                int shift = maxLength - length;
                int base = codes[symbol] << shift;
                int span = 1 << shift;
                for (int fill = 0; fill < span; fill++) {
                    int slot = base + fill;
                    if (slot >= size) {
                        throw new IllegalArgumentException(
                                "VP8L Huffman overflow max=" + maxLength
                                        + " code=" + codes[symbol]
                                        + " len=" + length
                                        + " symbol=" + symbol
                        );
                    }
                    decodeSymbol[slot] = symbol;
                    decodeLength[slot] = length;
                }
            }
        }

        /// Builds a table from observed symbols.
        private static Huffman fromSymbols(int[] symbols, int alphabet) {
            int[] freq = new int[alphabet];
            for (int symbol : symbols) {
                freq[symbol]++;
            }
            return fromFrequencies(freq);
        }

        /// Builds a table that always emits `symbol`.
        private static Huffman singleSymbol(int symbol, int alphabet) {
            int[] freq = new int[alphabet];
            freq[symbol] = 1;
            return fromFrequencies(freq);
        }

        /// Builds a length-limited table from frequencies.
        private static Huffman fromFrequencies(int[] freq) {
            ArrayList<Integer> used = new ArrayList<>();
            for (int symbol = 0; symbol < freq.length; symbol++) {
                if (freq[symbol] > 0) {
                    used.add(symbol);
                }
            }
            int[] lengths = new int[freq.length];
            if (used.size() <= 2) {
                if (used.size() == 1) {
                    lengths[used.getFirst()] = 1;
                } else {
                    lengths[used.get(0)] = 1;
                    lengths[used.get(1)] = 1;
                }
                return new Huffman(lengths);
            }
            int packed = 32 - Integer.numberOfLeadingZeros(used.size() - 1);
            int assigned = Math.max(1, Math.min(15, packed));
            for (int symbol : used) {
                lengths[symbol] = assigned;
            }
            return new Huffman(lengths);
        }

        /// Writes this table in VP8L form.
        private void writeTable(LsbWriter bits, int alphabet) {
            ArrayList<Integer> used = new ArrayList<>();
            for (int symbol = 0; symbol < lengths.length; symbol++) {
                if (lengths[symbol] > 0 || (lengths[symbol] == 0 && used.isEmpty() && isSingleZero(symbol))) {
                    used.add(symbol);
                }
            }
            ArrayList<Integer> present = new ArrayList<>();
            for (int symbol = 0; symbol < lengths.length; symbol++) {
                if (usedBy(symbol)) {
                    present.add(symbol);
                }
            }
            if (present.size() <= 2 && present.getLast() <= 255) {
                bits.write(1, 1);
                bits.write(present.size() == 2 ? 1 : 0, 1);
                int first = present.getFirst();
                bits.write(first > 1 ? 1 : 0, 1);
                bits.write(first, first > 1 ? 8 : 1);
                if (present.size() == 2) {
                    bits.write(present.get(1), 8);
                }
                return;
            }
            bits.write(0, 1);
            int[] lengthFreq = new int[19];
            for (int length : lengths) {
                lengthFreq[length]++;
            }
            int numCodeLengths = 4;
            for (int index = 18; index >= 4; index--) {
                if (lengthFreq[CODE_LENGTH_ORDER[index]] > 0) {
                    numCodeLengths = index + 1;
                    break;
                }
            }
            bits.write(numCodeLengths - 4, 4);
            Huffman lengthHuff = fromFrequencies(lengthFreq);
            for (int index = 0; index < numCodeLengths; index++) {
                bits.write(lengthHuff.lengths[CODE_LENGTH_ORDER[index]], 3);
            }
            bits.write(0, 1);
            for (int length : lengths) {
                lengthHuff.writeSymbol(bits, length);
            }
        }

        /// Reads one VP8L Huffman table.
        private static Huffman readTable(LsbReader bits, int alphabet) {
            if (bits.read(1) == 1) {
                int numSymbols = bits.read(1) + 1;
                int firstBits = bits.read(1) == 1 ? 8 : 1;
                int[] lengths = new int[alphabet];
                int symbol0 = bits.read(firstBits);
                if (numSymbols == 1) {
                    lengths[symbol0] = 1;
                } else {
                    int symbol1 = bits.read(8);
                    lengths[symbol0] = 1;
                    lengths[symbol1] = 1;
                }
                return new Huffman(lengths);
            }
            int numCodeLengths = bits.read(4) + 4;
            int[] codeLengthLengths = new int[19];
            for (int index = 0; index < numCodeLengths; index++) {
                codeLengthLengths[CODE_LENGTH_ORDER[index]] = bits.read(3);
            }
            Huffman lengthHuff = new Huffman(codeLengthLengths);
            int maxSymbol = alphabet;
            if (bits.read(1) == 1) {
                int length = bits.read(3) + 2;
                maxSymbol = bits.read(length) + 2;
            }
            int[] lengths = new int[alphabet];
            int symbol = 0;
            int prev = 8;
            while (symbol < maxSymbol && symbol < alphabet) {
                int code = lengthHuff.readSymbol(bits);
                if (code < 16) {
                    lengths[symbol++] = code;
                    prev = code;
                } else if (code == 16) {
                    int repeat = bits.read(2) + 3;
                    for (int index = 0; index < repeat && symbol < alphabet; index++) {
                        lengths[symbol++] = prev;
                    }
                } else if (code == 17) {
                    int repeat = bits.read(3) + 3;
                    symbol += repeat;
                } else {
                    int repeat = bits.read(7) + 11;
                    symbol += repeat;
                }
            }
            return new Huffman(lengths);
        }

        /// Writes `symbol`.
        private void writeSymbol(LsbWriter bits, int symbol) {
            int length = lengths[symbol];
            if (length == 0 && isSingleZero(symbol)) {
                return;
            }
            bits.write(codes[symbol], length);
        }

        /// Reads one symbol.
        private int readSymbol(LsbReader bits) {
            if (maxLength == 1 && isSingleZeroTable()) {
                for (int symbol = 0; symbol < lengths.length; symbol++) {
                    if (usedBy(symbol) && lengths[symbol] == 0) {
                        return symbol;
                    }
                }
            }
            int peek = bits.peek(maxLength);
            int length = decodeLength[peek];
            if (length == 0 && decodeSymbol[peek] >= 0) {
                return decodeSymbol[peek];
            }
            if (length == 0) {
                throw new IllegalArgumentException("VP8L Huffman code is invalid");
            }
            bits.consume(length);
            return decodeSymbol[peek];
        }

        /// Returns whether `symbol` has a code, including the 0-bit singleton.
        private boolean usedBy(int symbol) {
            if (lengths[symbol] > 0) {
                return true;
            }
            return isSingleZero(symbol);
        }

        /// Returns whether this table is a 0-bit singleton at `symbol`.
        private boolean isSingleZero(int symbol) {
            if (lengths[symbol] != 0) {
                return false;
            }
            int used = 0;
            int only = -1;
            for (int index = 0; index < lengths.length; index++) {
                if (lengths[index] > 0) {
                    return false;
                }
            }
            for (int index = 0; index < lengths.length; index++) {
                if (lengths[index] == 0) {
                    // many zeros; singleton is the one we assigned
                }
            }
            for (int index = 0; index < lengths.length; index++) {
                if (codes[index] == 0 && lengths[index] == 0) {
                    used++;
                    only = index;
                }
            }
            return only == symbol && singletonCount() == 1;
        }

        /// Returns whether the table has exactly one 0-bit symbol.
        private boolean isSingleZeroTable() {
            return singletonCount() == 1;
        }

        /// Counts symbols treated as present.
        private int singletonCount() {
            int count = 0;
            for (int length : lengths) {
                if (length > 0) {
                    return 0;
                }
            }
            for (int symbol = 0; symbol < lengths.length; symbol++) {
                if (codes[symbol] == 0 && lengths[symbol] == 0) {
                    count++;
                }
            }
            return count == lengths.length ? 1 : 0;
        }

        /// Assigns canonical codes from lengths.
        private static void assignCanonical(int[] lengths, int[] codes) {
            int max = 0;
            for (int length : lengths) {
                max = Math.max(max, length);
            }
            int[] blCount = new int[max + 1];
            for (int length : lengths) {
                if (length > 0) {
                    blCount[length]++;
                }
            }
            int[] nextCode = new int[max + 1];
            int code = 0;
            for (int bits = 1; bits <= max; bits++) {
                code = (code + blCount[bits - 1]) << 1;
                nextCode[bits] = code;
            }
            for (int symbol = 0; symbol < lengths.length; symbol++) {
                int length = lengths[symbol];
                if (length > 0) {
                    codes[symbol] = nextCode[length]++;
                }
            }
        }
    }

    /// LSB-first bit writer.
    private static final class LsbWriter {
        /// Accumulated bytes.
        private byte[] data = new byte[64];

        /// Number of valid bytes.
        private int size;

        /// Bit buffer.
        private int bitBuffer;

        /// Number of bits in [`bitBuffer`].
        private int bitCount;

        /// Writes `count` low bits of `value`.
        private void write(int value, int count) {
            if (count == 0) {
                return;
            }
            bitBuffer |= (value & ((1 << count) - 1)) << bitCount;
            bitCount += count;
            while (bitCount >= 8) {
                if (size == data.length) {
                    data = Arrays.copyOf(data, data.length * 2);
                }
                data[size++] = (byte) bitBuffer;
                bitBuffer >>>= 8;
                bitCount -= 8;
            }
        }

        /// Returns the packed bytes.
        private byte[] toArray() {
            if (bitCount > 0) {
                if (size == data.length) {
                    data = Arrays.copyOf(data, data.length * 2);
                }
                data[size++] = (byte) bitBuffer;
            }
            return Arrays.copyOf(data, size);
        }
    }

    /// LSB-first bit reader.
    private static final class LsbReader {
        /// Input stream.
        private final byte[] data;

        /// Exclusive end offset.
        private final int end;

        /// Next unread byte.
        private int position;

        /// Bit buffer.
        private int bitBuffer;

        /// Number of bits in [`bitBuffer`].
        private int bitCount;

        /// Creates a reader over `[offset, offset + length)`.
        private LsbReader(byte[] data, int offset, int length) {
            this.data = data;
            this.position = offset;
            this.end = offset + length;
        }

        /// Reads `count` bits.
        private int read(int count) {
            int value = peek(count);
            consume(count);
            return value;
        }

        /// Returns `count` bits without consuming them.
        private int peek(int count) {
            while (bitCount < count) {
                int octet = position < end ? data[position++] & 0xFF : 0;
                bitBuffer |= octet << bitCount;
                bitCount += 8;
            }
            return bitBuffer & ((1 << count) - 1);
        }

        /// Consumes `count` bits.
        private void consume(int count) {
            bitBuffer >>>= count;
            bitCount -= count;
        }
    }
}

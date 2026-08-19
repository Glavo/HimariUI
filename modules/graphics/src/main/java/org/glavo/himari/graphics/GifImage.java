package org.glavo.himari.graphics;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;

/// Encodes and decodes GIF89a images as unassociated sRGB RGBA.
///
/// The encoder builds a global color table of at most 256 unique opaque colors and writes one
/// image descriptor with LZW. [`#encodeAnimated(int, int, byte[]...)`] writes a Netscape loop
/// block and one graphic-control plus image descriptor per frame. [`#decode(byte[])`] composites
/// every image block with disposal methods 1 and 2 and returns the last canvas. Transparent
/// pixels decode as alpha `0`.
@NotNullByDefault
public final class GifImage {
    /// Maximum color-table entries.
    private static final int MAX_COLORS = 256;

    /// Prevents instantiation.
    private GifImage() {
    }

    /// Returns whether `bytes` begin with `GIF87a` or `GIF89a`.
    ///
    /// @param bytes the candidate stream
    /// @return whether the stream is GIF
    public static boolean isGif(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes");
        if (bytes.length < 6) {
            return false;
        }
        return bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == '8'
                && (bytes[4] == '7' || bytes[4] == '9') && bytes[5] == 'a';
    }

    /// Encodes row-major unassociated RGBA8 pixels as GIF89a.
    ///
    /// Distinct colors are limited to 256. Colors beyond that are quantized by dropping the
    /// low 4 bits of each channel.
    ///
    /// @param width the positive pixel width
    /// @param height the positive pixel height
    /// @param rgba `width * height * 4` bytes
    /// @return the GIF bytes
    public static byte @Unmodifiable [] encode(int width, int height, byte[] rgba) {
        Objects.requireNonNull(rgba, "rgba");
        int pixelCount = checkedPixelCount(width, height);
        if (rgba.length != pixelCount * 4) {
            throw new IllegalArgumentException("RGBA length must be width * height * 4");
        }
        int[] colors = new int[pixelCount];
        for (int index = 0; index < pixelCount; index++) {
            int offset = index * 4;
            colors[index] = rgb(rgba[offset], rgba[offset + 1], rgba[offset + 2]);
        }
        int[] palette = palette(colors);
        int[] indexMap = new int[pixelCount];
        for (int index = 0; index < pixelCount; index++) {
            indexMap[index] = nearest(palette, colors[index]);
        }
        int paletteBits = 1;
        while ((1 << paletteBits) < palette.length) {
            paletteBits++;
        }
        paletteBits = Math.max(2, paletteBits);
        int tableSize = 1 << paletteBits;
        ByteSink sink = new ByteSink();
        sink.bytes(new byte[] {'G', 'I', 'F', '8', '9', 'a'});
        sink.u16(width);
        sink.u16(height);
        sink.u8(0x80 | (paletteBits - 1) | ((paletteBits - 1) << 4));
        sink.u8(0);
        sink.u8(0);
        for (int index = 0; index < tableSize; index++) {
            int color = index < palette.length ? palette[index] : 0;
            sink.u8(color >>> 16);
            sink.u8(color >>> 8);
            sink.u8(color);
        }
        sink.u8(0x2C);
        sink.u16(0);
        sink.u16(0);
        sink.u16(width);
        sink.u16(height);
        sink.u8(0);
        int minCode = Math.max(2, paletteBits);
        sink.u8(minCode);
        byte[] packed = lzwEncode(indexMap, minCode);
        int offset = 0;
        while (offset < packed.length) {
            int block = Math.min(255, packed.length - offset);
            sink.u8(block);
            sink.raw(packed, offset, block);
            offset += block;
        }
        sink.u8(0);
        sink.u8(0x3B);
        return sink.toArray();
    }

    /// Encodes two or more full-size frames as a GIF89a animation.
    ///
    /// Each frame uses a local color table and graphic-control disposal 1 (do not dispose).
    /// [`#encodeAnimatedClear(int, int, byte[]...)`] writes disposal 2 (restore to background).
    /// Distinct colors per frame are limited to 256.
    ///
    /// @param width the positive pixel width
    /// @param height the positive pixel height
    /// @param frames at least two `width * height * 4` rasters
    /// @return the GIF bytes
    public static byte @Unmodifiable [] encodeAnimated(int width, int height, byte[]... frames) {
        return encodeAnimatedFrames(width, height, 1, frames);
    }

    /// Encodes two or more full-size frames as a GIF89a animation with disposal method 2.
    ///
    /// After each frame is displayed the painted rectangle is restored to transparent black.
    /// Distinct colors per frame are limited to 256.
    ///
    /// @param width the positive pixel width
    /// @param height the positive pixel height
    /// @param frames at least two `width * height * 4` rasters
    /// @return the GIF bytes
    public static byte @Unmodifiable [] encodeAnimatedClear(int width, int height, byte[]... frames) {
        return encodeAnimatedFrames(width, height, 2, frames);
    }

    /// Encodes animated frames with the given graphic-control disposal method.
    ///
    /// @param width the positive pixel width
    /// @param height the positive pixel height
    /// @param disposal `1` (do not dispose) or `2` (restore to background)
    /// @param frames at least two rasters
    private static byte @Unmodifiable [] encodeAnimatedFrames(
            int width,
            int height,
            int disposal,
            byte[]... frames
    ) {
        Objects.requireNonNull(frames, "frames");
        if (frames.length < 2) {
            throw new IllegalArgumentException("GIF animation requires at least two frames");
        }
        if (disposal != 1 && disposal != 2) {
            throw new IllegalArgumentException("GIF disposal must be 1 or 2");
        }
        int pixelCount = checkedPixelCount(width, height);
        for (byte[] frame : frames) {
            Objects.requireNonNull(frame, "frame");
            if (frame.length != pixelCount * 4) {
                throw new IllegalArgumentException("RGBA length must be width * height * 4");
            }
        }
        ByteSink sink = new ByteSink();
        sink.bytes(new byte[] {'G', 'I', 'F', '8', '9', 'a'});
        sink.u16(width);
        sink.u16(height);
        sink.u8(0x70);
        sink.u8(0);
        sink.u8(0);
        sink.bytes(new byte[] {0x21, (byte) 0xFF, 0x0B});
        sink.bytes(new byte[] {'N', 'E', 'T', 'S', 'C', 'A', 'P', 'E', '2', '.', '0'});
        sink.u8(3);
        sink.u8(1);
        sink.u16(0);
        sink.u8(0);
        for (byte[] frame : frames) {
            int[] colors = new int[pixelCount];
            for (int index = 0; index < pixelCount; index++) {
                int offset = index * 4;
                colors[index] = rgb(frame[offset], frame[offset + 1], frame[offset + 2]);
            }
            int[] palette = palette(colors);
            int[] indexMap = new int[pixelCount];
            for (int index = 0; index < pixelCount; index++) {
                indexMap[index] = nearest(palette, colors[index]);
            }
            int paletteBits = 1;
            while ((1 << paletteBits) < palette.length) {
                paletteBits++;
            }
            paletteBits = Math.max(2, paletteBits);
            int tableSize = 1 << paletteBits;
            sink.u8(0x21);
            sink.u8(0xF9);
            sink.u8(4);
            sink.u8(disposal << 2);
            sink.u16(10);
            sink.u8(0);
            sink.u8(0);
            sink.u8(0x2C);
            sink.u16(0);
            sink.u16(0);
            sink.u16(width);
            sink.u16(height);
            sink.u8(0x80 | (paletteBits - 1));
            for (int index = 0; index < tableSize; index++) {
                int color = index < palette.length ? palette[index] : 0;
                sink.u8(color >>> 16);
                sink.u8(color >>> 8);
                sink.u8(color);
            }
            int minCode = Math.max(2, paletteBits);
            sink.u8(minCode);
            byte[] packed = lzwEncode(indexMap, minCode);
            int offset = 0;
            while (offset < packed.length) {
                int block = Math.min(255, packed.length - offset);
                sink.u8(block);
                sink.raw(packed, offset, block);
                offset += block;
            }
            sink.u8(0);
        }
        sink.u8(0x3B);
        return sink.toArray();
    }

    /// Decodes a GIF87a or GIF89a stream, compositing every image block.
    ///
    /// @param bytes the GIF stream
    /// @return the last composited canvas
    public static Decoded decode(byte[] bytes) {
        Decoded[] frames = decodeFrames(bytes);
        return frames[frames.length - 1];
    }

    /// Decodes every composited frame of a GIF87a or GIF89a stream.
    ///
    /// Disposal method 1 leaves the canvas. Disposal method 2 restores the painted rectangle to
    /// transparent black before the next frame. Other disposal values are treated as method 1.
    ///
    /// @param bytes the GIF stream
    /// @return one canvas snapshot per image block
    public static Decoded @Unmodifiable [] decodeFrames(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes");
        if (!isGif(bytes)) {
            throw new IllegalArgumentException("GIF signature is missing");
        }
        ByteSource source = new ByteSource(bytes);
        source.position = 6;
        int width = source.u16();
        int height = source.u16();
        checkedPixelCount(width, height);
        int packed = source.u8();
        source.u8();
        source.u8();
        int[] global = new int[0];
        if ((packed & 0x80) != 0) {
            global = readTable(source, packed & 0x07);
        }
        int transparent = -1;
        int disposal = 1;
        byte[] canvas = new byte[width * height * 4];
        ArrayList<Decoded> frames = new ArrayList<>();
        while (source.remaining() > 0) {
            int introducer = source.u8();
            if (introducer == 0x3B) {
                break;
            }
            if (introducer == 0x21) {
                int label = source.u8();
                if (label == 0xF9 && source.remaining() >= 6) {
                    int size = source.u8();
                    int flags = source.u8();
                    source.u16();
                    int index = source.u8();
                    source.u8();
                    if (size >= 4) {
                        disposal = (flags >>> 2) & 0x07;
                        transparent = (flags & 0x01) != 0 ? index : -1;
                    }
                } else {
                    skipSubBlocks(source);
                }
                continue;
            }
            if (introducer != 0x2C) {
                throw new IllegalArgumentException("GIF block is unsupported");
            }
            int left = source.u16();
            int top = source.u16();
            int imageWidth = source.u16();
            int imageHeight = source.u16();
            int imagePacked = source.u8();
            int[] local = global;
            if ((imagePacked & 0x80) != 0) {
                local = readTable(source, imagePacked & 0x07);
            }
            int minCode = source.u8();
            byte[] compressed = readSubBlocks(source);
            int[] indices = lzwDecode(compressed, minCode, imageWidth * imageHeight);
            int limitX = Math.min(width - left, imageWidth);
            int limitY = Math.min(height - top, imageHeight);
            for (int y = 0; y < limitY; y++) {
                for (int x = 0; x < limitX; x++) {
                    if (left + x < 0 || top + y < 0) {
                        continue;
                    }
                    int colorIndex = indices[y * imageWidth + x] & 0xFF;
                    int dest = ((top + y) * width + (left + x)) * 4;
                    if (colorIndex == transparent || colorIndex >= local.length) {
                        continue;
                    }
                    int color = local[colorIndex];
                    canvas[dest] = (byte) (color >>> 16);
                    canvas[dest + 1] = (byte) (color >>> 8);
                    canvas[dest + 2] = (byte) color;
                    canvas[dest + 3] = (byte) 255;
                }
            }
            frames.add(new Decoded(width, height, Arrays.copyOf(canvas, canvas.length)));
            if (disposal == 2) {
                for (int y = 0; y < limitY; y++) {
                    for (int x = 0; x < limitX; x++) {
                        if (left + x < 0 || top + y < 0) {
                            continue;
                        }
                        int dest = ((top + y) * width + (left + x)) * 4;
                        canvas[dest] = 0;
                        canvas[dest + 1] = 0;
                        canvas[dest + 2] = 0;
                        canvas[dest + 3] = 0;
                    }
                }
            }
            transparent = -1;
            disposal = 1;
        }
        if (frames.isEmpty()) {
            throw new IllegalArgumentException("GIF image block is missing");
        }
        return frames.toArray(Decoded[]::new);
    }

    /// Builds a palette of at most 256 colors.
    private static int[] palette(int[] colors) {
        HashMap<Integer, Integer> unique = new HashMap<>();
        for (int color : colors) {
            unique.putIfAbsent(color, unique.size());
        }
        if (unique.size() <= MAX_COLORS) {
            int[] palette = new int[unique.size()];
            unique.forEach((color, index) -> palette[index] = color);
            return palette;
        }
        unique.clear();
        for (int color : colors) {
            int quantized = color & 0xF0F0F0;
            unique.putIfAbsent(quantized, unique.size());
        }
        if (unique.size() > MAX_COLORS) {
            unique.clear();
            for (int color : colors) {
                int quantized = color & 0xE0E0E0;
                unique.putIfAbsent(quantized, unique.size());
                if (unique.size() == MAX_COLORS) {
                    break;
                }
            }
        }
        int[] palette = new int[Math.min(MAX_COLORS, unique.size())];
        unique.forEach((color, index) -> {
            if (index < palette.length) {
                palette[index] = color;
            }
        });
        return palette;
    }

    /// Returns the nearest palette index.
    private static int nearest(int[] palette, int color) {
        int best = 0;
        int bestDist = Integer.MAX_VALUE;
        int red = color >>> 16;
        int green = (color >>> 8) & 0xFF;
        int blue = color & 0xFF;
        for (int index = 0; index < palette.length; index++) {
            int candidate = palette[index];
            int dr = red - (candidate >>> 16);
            int dg = green - ((candidate >>> 8) & 0xFF);
            int db = blue - (candidate & 0xFF);
            int dist = dr * dr + dg * dg + db * db;
            if (dist < bestDist) {
                bestDist = dist;
                best = index;
            }
        }
        return best;
    }

    /// Encodes `indices` with GIF LZW.
    private static byte[] lzwEncode(int[] indices, int minCode) {
        int clear = 1 << minCode;
        int eoi = clear + 1;
        int nextCode = eoi + 1;
        int codeSize = minCode + 1;
        HashMap<Long, Integer> dictionary = new HashMap<>();
        BitPacker packer = new BitPacker();
        packer.write(clear, codeSize);
        int w = indices[0];
        for (int index = 1; index < indices.length; index++) {
            int k = indices[index];
            long wk = ((long) w << 16) | k;
            Integer existing = dictionary.get(wk);
            if (existing != null) {
                w = existing;
                continue;
            }
            packer.write(w, codeSize);
            if (nextCode < 4096) {
                dictionary.put(wk, nextCode);
                if (nextCode == (1 << codeSize) && codeSize < 12) {
                    codeSize++;
                }
                nextCode++;
            } else {
                packer.write(clear, codeSize);
                dictionary.clear();
                nextCode = eoi + 1;
                codeSize = minCode + 1;
            }
            w = k;
        }
        packer.write(w, codeSize);
        packer.write(eoi, codeSize);
        return packer.toArray();
    }

    /// Decodes GIF LZW into `expected` indices.
    private static int[] lzwDecode(byte[] compressed, int minCode, int expected) {
        int clear = 1 << minCode;
        int eoi = clear + 1;
        ArrayList<int[]> table = new ArrayList<>();
        int nextCode = eoi + 1;
        int codeSize = minCode + 1;
        BitUnpacker unpacker = new BitUnpacker(compressed);
        int[] output = new int[expected];
        int written = 0;
        int previous = -1;
        while (written < expected) {
            int code = unpacker.read(codeSize);
            if (code == eoi || code < 0) {
                break;
            }
            if (code == clear) {
                table.clear();
                nextCode = eoi + 1;
                codeSize = minCode + 1;
                previous = -1;
                continue;
            }
            int[] sequence;
            if (code < clear) {
                sequence = new int[] {code};
            } else if (code - eoi - 1 < table.size()) {
                sequence = table.get(code - eoi - 1);
            } else if (code == nextCode && previous >= 0) {
                int[] prev = sequenceOf(previous, clear, eoi, table);
                sequence = append(prev, prev[0]);
            } else {
                throw new IllegalArgumentException("GIF LZW code is invalid");
            }
            for (int value : sequence) {
                if (written < expected) {
                    output[written++] = value;
                }
            }
            if (previous >= 0 && nextCode < 4096) {
                int[] prev = sequenceOf(previous, clear, eoi, table);
                table.add(append(prev, sequence[0]));
                nextCode++;
                if (nextCode == (1 << codeSize) && codeSize < 12) {
                    codeSize++;
                }
            }
            previous = code;
        }
        return output;
    }

    /// Returns the index sequence for `code`.
    private static int[] sequenceOf(int code, int clear, int eoi, ArrayList<int[]> table) {
        if (code < clear) {
            return new int[] {code};
        }
        return table.get(code - eoi - 1);
    }

    /// Returns `values` plus `extra`.
    private static int[] append(int[] values, int extra) {
        int[] next = Arrays.copyOf(values, values.length + 1);
        next[values.length] = extra;
        return next;
    }

    /// Reads a color table of `2^(bits+1)` RGB entries.
    private static int[] readTable(ByteSource source, int bits) {
        int count = 1 << (bits + 1);
        int[] table = new int[count];
        for (int index = 0; index < count; index++) {
            table[index] = (source.u8() << 16) | (source.u8() << 8) | source.u8();
        }
        return table;
    }

    /// Reads GIF sub-blocks into one buffer.
    private static byte[] readSubBlocks(ByteSource source) {
        ByteSink sink = new ByteSink();
        int size;
        while ((size = source.u8()) != 0) {
            for (int index = 0; index < size; index++) {
                sink.u8(source.u8());
            }
        }
        return sink.toArray();
    }

    /// Skips GIF sub-blocks.
    private static void skipSubBlocks(ByteSource source) {
        int size;
        while ((size = source.u8()) != 0) {
            source.position += size;
        }
    }

    /// Packs 8-bit RGB into `0xRRGGBB`.
    private static int rgb(byte red, byte green, byte blue) {
        return (red & 0xFF) << 16 | (green & 0xFF) << 8 | (blue & 0xFF);
    }

    /// Returns `width * height` after rejecting non-positive or oversized images.
    private static int checkedPixelCount(int width, int height) {
        if (width <= 0 || height <= 0 || width > PixelBuffer.MAX_EDGE || height > PixelBuffer.MAX_EDGE) {
            throw new IllegalArgumentException("GIF dimensions must be in (0, " + PixelBuffer.MAX_EDGE + "]");
        }
        return Math.multiplyExact(width, height);
    }

    /// Stores one decoded GIF image.
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

    /// Little-endian byte writer.
    private static final class ByteSink {
        /// Accumulated bytes.
        private byte[] data = new byte[64];

        /// Number of valid bytes.
        private int size;

        /// Writes one byte.
        private void u8(int value) {
            if (size == data.length) {
                data = Arrays.copyOf(data, data.length * 2);
            }
            data[size++] = (byte) value;
        }

        /// Writes a 16-bit little-endian integer.
        private void u16(int value) {
            u8(value);
            u8(value >>> 8);
        }

        /// Writes `bytes`.
        private void bytes(byte[] values) {
            raw(values, 0, values.length);
        }

        /// Writes a slice.
        private void raw(byte[] values, int offset, int length) {
            if (size + length > data.length) {
                data = Arrays.copyOf(data, Math.max(data.length * 2, size + length));
            }
            System.arraycopy(values, offset, data, size, length);
            size += length;
        }

        /// Returns the written bytes.
        private byte[] toArray() {
            return Arrays.copyOf(data, size);
        }
    }

    /// Little-endian byte reader.
    private static final class ByteSource {
        /// Input stream.
        private final byte[] data;

        /// Next unread byte.
        private int position;

        /// Creates a reader.
        private ByteSource(byte[] data) {
            this.data = data;
        }

        /// Returns remaining bytes.
        private int remaining() {
            return data.length - position;
        }

        /// Reads one byte.
        private int u8() {
            if (position >= data.length) {
                throw new IllegalArgumentException("GIF stream is truncated");
            }
            return data[position++] & 0xFF;
        }

        /// Reads a 16-bit little-endian integer.
        private int u16() {
            return u8() | (u8() << 8);
        }
    }

    /// Packs LZW codes LSB-first.
    private static final class BitPacker {
        /// Accumulated bytes.
        private byte[] data = new byte[64];

        /// Number of valid bytes.
        private int size;

        /// Bit buffer.
        private int bitBuffer;

        /// Number of bits in [`bitBuffer`].
        private int bitCount;

        /// Writes `code` using `width` bits.
        private void write(int code, int width) {
            bitBuffer |= code << bitCount;
            bitCount += width;
            while (bitCount >= 8) {
                if (size == data.length) {
                    data = Arrays.copyOf(data, data.length * 2);
                }
                data[size++] = (byte) bitBuffer;
                bitBuffer >>>= 8;
                bitCount -= 8;
            }
        }

        /// Returns the packed bytes, including a partial last byte.
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

    /// Unpacks LZW codes LSB-first.
    private static final class BitUnpacker {
        /// Input stream.
        private final byte[] data;

        /// Next unread byte.
        private int position;

        /// Bit buffer.
        private int bitBuffer;

        /// Number of bits in [`bitBuffer`].
        private int bitCount;

        /// Creates an unpacker.
        private BitUnpacker(byte[] data) {
            this.data = data;
        }

        /// Reads `width` bits, or `-1` at the end.
        private int read(int width) {
            while (bitCount < width) {
                if (position >= data.length) {
                    return -1;
                }
                bitBuffer |= (data[position++] & 0xFF) << bitCount;
                bitCount += 8;
            }
            int value = bitBuffer & ((1 << width) - 1);
            bitBuffer >>>= width;
            bitCount -= width;
            return value;
        }
    }
}

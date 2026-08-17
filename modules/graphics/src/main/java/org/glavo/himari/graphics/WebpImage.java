package org.glavo.himari.graphics;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.PriorityQueue;

/// Encodes and decodes lossless WebP (VP8L) as unassociated sRGB RGBA.
///
/// The first-stable encoder writes a `VP8L` chunk with no transforms and no color cache.
/// Pixels are ARGB literals. Huffman tables use the simple form when a channel has at most two
/// symbols and a length-limited canonical form otherwise.
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
        byte[] payload = bits.toArray();
        byte[] output = new byte[20 + payload.length + (payload.length & 1)];
        writeBe(output, 0, RIFF);
        writeLe(output, 4, output.length - 8);
        writeBe(output, 8, WEBP);
        writeBe(output, 12, VP8L);
        writeLe(output, 16, payload.length);
        System.arraycopy(payload, 0, output, 20, payload.length);
        return output;
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
        if (bits.read(1) != 0) {
            throw new IllegalArgumentException("VP8L transforms are not in the first-stable subset");
        }
        if (bits.read(1) != 0) {
            throw new IllegalArgumentException("VP8L color cache is not in the first-stable subset");
        }
        Huffman green = Huffman.readTable(bits, 280);
        Huffman red = Huffman.readTable(bits, 256);
        Huffman blue = Huffman.readTable(bits, 256);
        Huffman alpha = Huffman.readTable(bits, 256);
        Huffman.readTable(bits, 40);
        int pixelCount = width * height;
        byte[] rgba = new byte[pixelCount * 4];
        for (int index = 0; index < pixelCount; index++) {
            int g = green.readSymbol(bits);
            if (g >= 256) {
                throw new IllegalArgumentException("VP8L copy codes are not in the first-stable subset");
            }
            int r = red.readSymbol(bits);
            int b = blue.readSymbol(bits);
            int a = alpha.readSymbol(bits);
            int offset = index * 4;
            rgba[offset] = (byte) r;
            rgba[offset + 1] = (byte) g;
            rgba[offset + 2] = (byte) b;
            rgba[offset + 3] = (byte) a;
        }
        return new Decoded(width, height, rgba);
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
                    decodeSymbol[base + fill] = symbol;
                    decodeLength[base + fill] = length;
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
                    lengths[used.getFirst()] = 0;
                } else {
                    lengths[used.get(0)] = 1;
                    lengths[used.get(1)] = 1;
                }
                return new Huffman(lengths);
            }
            PriorityQueue<Node> queue = new PriorityQueue<>();
            for (int symbol : used) {
                queue.add(new Node(freq[symbol], symbol, null, null));
            }
            while (queue.size() > 1) {
                Node left = queue.poll();
                Node right = queue.poll();
                queue.add(new Node(left.freq + right.freq, -1, left, right));
            }
            fillLengths(queue.peek(), 0, lengths);
            limitLengths(lengths, 15);
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
            if (present.size() <= 2) {
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
                    lengths[symbol0] = 0;
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

        /// Caps code lengths at `limit`.
        private static void limitLengths(int[] lengths, int limit) {
            for (int index = 0; index < lengths.length; index++) {
                if (lengths[index] > limit) {
                    lengths[index] = limit;
                }
            }
        }

        /// Recursively records depths.
        private static void fillLengths(Node node, int depth, int[] lengths) {
            if (node == null) {
                return;
            }
            if (node.symbol >= 0) {
                lengths[node.symbol] = Math.max(1, depth);
                return;
            }
            fillLengths(node.left, depth + 1, lengths);
            fillLengths(node.right, depth + 1, lengths);
        }
    }

    /// Huffman tree node.
    private static final class Node implements Comparable<Node> {
        /// Combined frequency.
        private final int freq;

        /// Symbol, or `-1` for an interior node.
        private final int symbol;

        /// Left child.
        private final Node left;

        /// Right child.
        private final Node right;

        /// Creates a node.
        private Node(int freq, int symbol, Node left, Node right) {
            this.freq = freq;
            this.symbol = symbol;
            this.left = left;
            this.right = right;
        }

        @Override
        public int compareTo(Node other) {
            int cmp = Integer.compare(freq, other.freq);
            if (cmp != 0) {
                return cmp;
            }
            return Integer.compare(symbol, other.symbol);
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

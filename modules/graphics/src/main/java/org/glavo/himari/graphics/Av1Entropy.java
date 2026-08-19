package org.glavo.himari.graphics;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.util.Objects;

/// AV1 symbol coder from AOMedia AV1 §8.2 used by [`AvifImage`].
///
/// [`#encodeRgba(byte[], int)`] writes 8×8 tiles of 4×4 Walsh-Hadamard residuals after a
/// per-tile intra mode (`DC`, left, above, or average). [`#encodeLiterals(int[], int)`] writes
/// unsigned `L(n)` values used by arithmetic JPEG. The coder matches the §8.2.2 initialization
/// (`SymbolRange = 1 << 15`) and §8.2.3 `read_bool` split/renormalize process.
@NotNullByDefault
final class Av1Entropy {
    /// Initial `SymbolRange` from §8.2.2.
    private static final int INITIAL_RANGE = 1 << 15;

    /// Renormalize while `SymbolRange` is below this value.
    private static final int MIN_RANGE = 128;

    /// Equiprobable boolean used by `L(n)`.
    private static final int HALF = 128;

    /// Intra mode: predict 128.
    private static final int MODE_DC = 0;

    /// Intra mode: predict from the left neighbor.
    private static final int MODE_LEFT = 1;

    /// Intra mode: predict from the above neighbor.
    private static final int MODE_ABOVE = 2;

    /// Intra mode: predict the average of left and above.
    private static final int MODE_AVG = 3;

    /// First-stable tile edge in pixels.
    private static final int TILE = 8;

    /// First-stable transform block edge in pixels.
    private static final int TX = 4;

    /// Transform type: 4×4 Walsh-Hadamard.
    private static final int TX_HADAMARD = 1;

    /// Offset applied so Hadamard coefficients are unsigned `L(13)` literals.
    private static final int COEFF_OFFSET = 4096;

    /// Bit width of each Hadamard coefficient.
    private static final int COEFF_BITS = 13;

    /// Prevents instantiation.
    private Av1Entropy() {
    }

    /// Range-codes unsigned `bits`-wide literals and a trailing one-bit marker.
    ///
    /// @param values the literals
    /// @param bits the bit width of each literal
    /// @return the symbol-coded payload
    static byte @Unmodifiable [] encodeLiterals(int[] values, int bits) {
        Objects.requireNonNull(values, "values");
        Encoder encoder = new Encoder();
        for (int value : values) {
            encoder.writeLiteral(value, bits);
        }
        encoder.writeBool(1, HALF);
        return encoder.finish();
    }

    /// Inflates `count` unsigned `bits`-wide literals.
    ///
    /// @param tile the symbol-coded payload
    /// @param count the number of literals
    /// @param bits the bit width of each literal
    /// @return the literals
    static int[] decodeLiterals(byte[] tile, int count, int bits) {
        Objects.requireNonNull(tile, "tile");
        if (count < 0 || bits < 1 || bits > 16) {
            throw new IllegalArgumentException("AV1 literal dimensions are invalid");
        }
        Decoder decoder = new Decoder(tile);
        int[] values = new int[count];
        for (int index = 0; index < count; index++) {
            values[index] = decoder.readLiteral(bits);
        }
        if (decoder.readBool(HALF) != 1) {
            throw new IllegalArgumentException("AV1 tile end marker is missing");
        }
        return values;
    }

    /// Range-codes 8×8 tiles of 4×4 Walsh-Hadamard residuals of `rgba`.
    ///
    /// Each tile stores a 2-bit intra mode, a 1-bit Hadamard transform flag, and `L(13)`
    /// coefficients of signed residuals after that predictor.
    ///
    /// @param rgba the unassociated RGBA octets
    /// @param width the pixel width
    /// @return the tile payload
    static byte @Unmodifiable [] encodeRgba(byte[] rgba, int width) {
        Objects.requireNonNull(rgba, "rgba");
        if (width <= 0 || rgba.length % (width * 4) != 0) {
            throw new IllegalArgumentException("AV1 tile width must divide the RGBA length");
        }
        int height = rgba.length / (width * 4);
        Encoder encoder = new Encoder();
        encoder.writeLiteral(3, 3);
        for (int tileY = 0; tileY < height; tileY += TILE) {
            for (int tileX = 0; tileX < width; tileX += TILE) {
                int mode = chooseMode(rgba, width, height, tileX, tileY);
                encoder.writeLiteral(mode, 2);
                encoder.writeLiteral(TX_HADAMARD, 1);
                int tileW = Math.min(TILE, width - tileX);
                int tileH = Math.min(TILE, height - tileY);
                for (int blockY = 0; blockY < tileH; blockY += TX) {
                    for (int blockX = 0; blockX < tileW; blockX += TX) {
                        int originX = tileX + blockX;
                        int originY = tileY + blockY;
                        for (int channel = 0; channel < 4; channel++) {
                            int[] block = residualBlock(rgba, width, height, originX, originY, channel, mode);
                            hadamard4(block);
                            for (int coeff : block) {
                                encoder.writeLiteral(coeff + COEFF_OFFSET, COEFF_BITS);
                            }
                        }
                    }
                }
            }
        }
        encoder.writeBool(1, HALF);
        return encoder.finish();
    }

    /// Inflates `count` RGBA octets from 4×4 Walsh-Hadamard tiles.
    ///
    /// @param tile the tile payload
    /// @param count the number of RGBA octets
    /// @param width the pixel width
    /// @return the decoded octets
    static byte[] decodeRgba(byte[] tile, int count, int width) {
        Objects.requireNonNull(tile, "tile");
        if (count < 0 || width <= 0 || count % (width * 4) != 0) {
            throw new IllegalArgumentException("AV1 tile dimensions are invalid");
        }
        int height = count / (width * 4);
        Decoder decoder = new Decoder(tile);
        if (decoder.readLiteral(3) != 3) {
            throw new IllegalArgumentException("AV1 tile size log2 must be 3");
        }
        byte[] rgba = new byte[count];
        for (int tileY = 0; tileY < height; tileY += TILE) {
            for (int tileX = 0; tileX < width; tileX += TILE) {
                int mode = decoder.readLiteral(2);
                if (decoder.readLiteral(1) != TX_HADAMARD) {
                    throw new IllegalArgumentException("AV1 transform type must be Hadamard");
                }
                int tileW = Math.min(TILE, width - tileX);
                int tileH = Math.min(TILE, height - tileY);
                for (int blockY = 0; blockY < tileH; blockY += TX) {
                    for (int blockX = 0; blockX < tileW; blockX += TX) {
                        int originX = tileX + blockX;
                        int originY = tileY + blockY;
                        for (int channel = 0; channel < 4; channel++) {
                            int[] block = new int[TX * TX];
                            for (int index = 0; index < block.length; index++) {
                                block[index] = decoder.readLiteral(COEFF_BITS) - COEFF_OFFSET;
                            }
                            inverseHadamard4(block);
                            writeResidualBlock(rgba, width, height, originX, originY, channel, mode, block);
                        }
                    }
                }
            }
        }
        if (decoder.readBool(HALF) != 1) {
            throw new IllegalArgumentException("AV1 tile end marker is missing");
        }
        return rgba;
    }

    /// Picks the intra mode with the smallest residual energy for one tile.
    private static int chooseMode(byte[] rgba, int width, int height, int tileX, int tileY) {
        int best = MODE_LEFT;
        int bestEnergy = Integer.MAX_VALUE;
        int tileW = Math.min(TILE, width - tileX);
        int tileH = Math.min(TILE, height - tileY);
        for (int mode = 0; mode < 4; mode++) {
            int energy = 0;
            for (int y = 0; y < tileH; y++) {
                for (int x = 0; x < tileW; x++) {
                    int index = ((tileY + y) * width + (tileX + x)) * 4;
                    for (int channel = 0; channel < 3; channel++) {
                        int residual = (rgba[index + channel] & 0xFF)
                                - predict(rgba, width, tileX + x, tileY + y, channel, mode);
                        energy += residual * residual;
                    }
                }
            }
            if (energy < bestEnergy) {
                bestEnergy = energy;
                best = mode;
            }
        }
        return best;
    }

    /// Predicts one channel at `(x, y)` using `mode`.
    private static int predict(byte[] rgba, int width, int x, int y, int channel, int mode) {
        int left = x == 0 ? 128 : rgba[(y * width + x - 1) * 4 + channel] & 0xFF;
        int above = y == 0 ? 128 : rgba[((y - 1) * width + x) * 4 + channel] & 0xFF;
        return switch (mode) {
            case MODE_DC -> 128;
            case MODE_LEFT -> left;
            case MODE_ABOVE -> above;
            case MODE_AVG -> (left + above + 1) >> 1;
            default -> 128;
        };
    }

    /// Predicts one channel from samples outside the 4×4 block origin `(blockX, blockY)`.
    private static int predictBlock(
            byte[] rgba,
            int width,
            int x,
            int y,
            int blockX,
            int blockY,
            int channel,
            int mode
    ) {
        int left = blockX == 0 ? 128 : rgba[(y * width + blockX - 1) * 4 + channel] & 0xFF;
        int above = blockY == 0 ? 128 : rgba[((blockY - 1) * width + x) * 4 + channel] & 0xFF;
        return switch (mode) {
            case MODE_DC -> 128;
            case MODE_LEFT -> left;
            case MODE_ABOVE -> above;
            case MODE_AVG -> (left + above + 1) >> 1;
            default -> 128;
        };
    }

    /// Gathers signed residuals of one 4×4 channel block, padding missing samples with 0.
    private static int[] residualBlock(
            byte[] rgba,
            int width,
            int height,
            int originX,
            int originY,
            int channel,
            int mode
    ) {
        int[] block = new int[TX * TX];
        for (int y = 0; y < TX; y++) {
            for (int x = 0; x < TX; x++) {
                int px = originX + x;
                int py = originY + y;
                if (px >= width || py >= height) {
                    continue;
                }
                int value = rgba[(py * width + px) * 4 + channel] & 0xFF;
                block[y * TX + x] = value - predictBlock(rgba, width, px, py, originX, originY, channel, mode);
            }
        }
        return block;
    }

    /// Adds inverse-transformed residuals onto reconstructed samples.
    private static void writeResidualBlock(
            byte[] rgba,
            int width,
            int height,
            int originX,
            int originY,
            int channel,
            int mode,
            int[] block
    ) {
        for (int y = 0; y < TX; y++) {
            for (int x = 0; x < TX; x++) {
                int px = originX + x;
                int py = originY + y;
                if (px >= width || py >= height) {
                    continue;
                }
                int predicted = predictBlock(rgba, width, px, py, originX, originY, channel, mode);
                rgba[(py * width + px) * 4 + channel] = (byte) Math.clamp(predicted + block[y * TX + x], 0, 255);
            }
        }
    }

    /// In-place 4×4 Walsh-Hadamard transform.
    private static void hadamard4(int[] block) {
        int[] tmp = new int[TX * TX];
        for (int y = 0; y < TX; y++) {
            int row = y * TX;
            int a = block[row] + block[row + 1];
            int b = block[row] - block[row + 1];
            int c = block[row + 2] + block[row + 3];
            int d = block[row + 2] - block[row + 3];
            tmp[row] = a + c;
            tmp[row + 1] = b + d;
            tmp[row + 2] = a - c;
            tmp[row + 3] = b - d;
        }
        for (int x = 0; x < TX; x++) {
            int a = tmp[x] + tmp[x + TX];
            int b = tmp[x] - tmp[x + TX];
            int c = tmp[x + 2 * TX] + tmp[x + 3 * TX];
            int d = tmp[x + 2 * TX] - tmp[x + 3 * TX];
            block[x] = a + c;
            block[x + TX] = b + d;
            block[x + 2 * TX] = a - c;
            block[x + 3 * TX] = b - d;
        }
    }

    /// Inverts [`#hadamard4(int[])`] by applying the same transform and dividing by 16.
    private static void inverseHadamard4(int[] block) {
        hadamard4(block);
        for (int index = 0; index < block.length; index++) {
            block[index] >>= 4;
        }
    }

    /// §8.2 boolean decoder over one tile.
    private static final class Decoder {
        /// Tile bytes.
        private final byte[] data;

        /// Next unread byte.
        private int offset;

        /// Bits remaining in the current byte, 0 when a new byte is needed.
        private int bitsLeft;

        /// Unread bits of the current byte, most-significant first.
        private int bitBuf;

        /// `SymbolValue`.
        private int symbolValue;

        /// `SymbolRange`.
        private int symbolRange;

        /// Initializes the decoder with the first 15 bits of `data`.
        Decoder(byte[] data) {
            this.data = data;
            int value = 0;
            for (int index = 0; index < 15; index++) {
                value = (value << 1) | readBit();
            }
            this.symbolValue = value;
            this.symbolRange = INITIAL_RANGE;
        }

        /// Decodes `n` equiprobable bits as an unsigned integer.
        int readLiteral(int n) {
            int value = 0;
            for (int index = 0; index < n; index++) {
                value = (value << 1) | readBool(HALF);
            }
            return value;
        }

        /// Decodes one boolean with P(0) = `prob` / 256.
        int readBool(int prob) {
            int split = ((symbolRange - 1) * prob >> 8) + 1;
            int bit;
            if (symbolValue < split) {
                symbolRange = split;
                bit = 0;
            } else {
                symbolRange -= split;
                symbolValue -= split;
                bit = 1;
            }
            while (symbolRange < MIN_RANGE) {
                symbolValue = (symbolValue << 1) | readBit();
                symbolRange <<= 1;
            }
            return bit;
        }

        /// Reads the next raw bit, or 0 past the end of the tile.
        private int readBit() {
            if (bitsLeft == 0) {
                if (offset >= data.length) {
                    return 0;
                }
                bitBuf = data[offset++] & 0xFF;
                bitsLeft = 8;
            }
            bitsLeft--;
            return (bitBuf >> bitsLeft) & 1;
        }
    }

    /// Matching §8.2 range encoder.
    private static final class Encoder {
        /// Lower bound of the current interval.
        private BigInteger low = BigInteger.ZERO;

        /// Current `SymbolRange`.
        private int range = INITIAL_RANGE;

        /// Number of bits of precision in [`#low`].
        private int scaleBits = 15;

        /// Encodes `n` bits of `value` as equiprobable booleans.
        void writeLiteral(int value, int n) {
            for (int shift = n - 1; shift >= 0; shift--) {
                writeBool((value >> shift) & 1, HALF);
            }
        }

        /// Encodes one boolean with P(0) = `prob` / 256.
        void writeBool(int bit, int prob) {
            int split = ((range - 1) * prob >> 8) + 1;
            if (bit != 0) {
                low = low.add(BigInteger.valueOf(split));
                range -= split;
            } else {
                range = split;
            }
            while (range < MIN_RANGE) {
                low = low.shiftLeft(1);
                range <<= 1;
                scaleBits++;
            }
        }

        /// Emits `scaleBits` bits of the lower bound, packed MSB first.
        byte[] finish() {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            int acc = 0;
            int filled = 0;
            for (int index = scaleBits - 1; index >= 0; index--) {
                acc = (acc << 1) | (low.testBit(index) ? 1 : 0);
                filled++;
                if (filled == 8) {
                    output.write(acc);
                    acc = 0;
                    filled = 0;
                }
            }
            if (filled > 0) {
                output.write(acc << (8 - filled));
            }
            return output.toByteArray();
        }
    }
}

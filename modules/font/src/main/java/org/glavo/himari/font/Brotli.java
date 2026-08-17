package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

/// RFC 7932 Brotli used by [`Woff2File`].
///
/// [`#compress(byte[])`] emits the Section 11.1 trivial stream (uncompressed meta-blocks plus an
/// empty last meta-block). [`#decompress(byte[])`] accepts that form and compressed meta-blocks
/// with simple or complex prefix codes, block switches, and LZ77 copies inside the sliding
/// window. Distances that resolve to the static dictionary are rejected; first-stable WOFF2 wrap
/// never emits them.
@NotNullByDefault
public final class Brotli {
    /// Rejects a decompressed image larger than this many bytes.
    public static final int MAX_OUTPUT = 64 * 1024 * 1024;

    /// Insert extra-bit counts for the 24 insert-length codes.
    private static final int[] INSERT_EXTRA = {
            0, 0, 0, 0, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 7, 8, 9, 10, 12, 14, 24
    };

    /// Insert-length bases for the 24 insert-length codes.
    private static final int[] INSERT_BASE = {
            0, 1, 2, 3, 4, 5, 6, 8, 10, 14, 18, 26, 34, 50, 66, 98,
            130, 194, 322, 578, 1090, 2114, 6210, 22594
    };

    /// Copy extra-bit counts for the 24 copy-length codes.
    private static final int[] COPY_EXTRA = {
            0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 7, 8, 9, 10, 24
    };

    /// Copy-length bases for the 24 copy-length codes.
    private static final int[] COPY_BASE = {
            2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 14, 18, 22, 30, 38, 54,
            70, 102, 134, 198, 326, 582, 1094, 2118
    };

    /// Block-count extra-bit counts for the 26 block-count codes.
    private static final int[] BLOCK_EXTRA = {
            2, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 5, 6, 6, 7, 8, 9, 10, 11, 12, 13, 24
    };

    /// Block-count bases for the 26 block-count codes.
    private static final int[] BLOCK_BASE = {
            1, 5, 9, 13, 17, 25, 33, 41, 49, 65, 81, 97, 113, 145, 177, 209,
            241, 305, 369, 497, 753, 1265, 2289, 4337, 8433, 16625
    };

    /// Code-length symbol order for a complex prefix code.
    private static final int[] CL_ORDER = {
            1, 2, 3, 4, 0, 5, 17, 6, 16, 7, 8, 9, 10, 11, 12, 13, 14, 15
    };

    /// Peek-4 lengths for the static code-length-code prefix.
    private static final int[] CL_PREFIX_LENGTH = {
            2, 2, 2, 3, 2, 2, 2, 4, 2, 2, 2, 3, 2, 2, 2, 4
    };

    /// Peek-4 symbols for the static code-length-code prefix.
    private static final int[] CL_PREFIX_VALUE = {
            0, 4, 3, 2, 0, 4, 3, 1, 0, 4, 3, 2, 0, 4, 3, 5
    };

    /// RFC 7.1 Lut0.
    private static final byte[] LUT0 = new byte[] {
            0, 0, 0, 0, 0, 0, 0, 0, 0, 4, 4, 0, 0, 4, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            8, 12, 16, 12, 12, 20, 12, 16, 24, 28, 12, 12, 32, 12, 36, 12,
            44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 32, 32, 24, 40, 28, 12,
            12, 48, 52, 52, 52, 48, 52, 52, 52, 48, 52, 52, 52, 52, 52, 48,
            52, 52, 52, 52, 52, 48, 52, 52, 52, 52, 52, 24, 12, 28, 12, 12,
            12, 56, 60, 60, 60, 56, 60, 60, 60, 56, 60, 60, 60, 60, 60, 56,
            60, 60, 60, 60, 60, 56, 60, 60, 60, 60, 60, 24, 12, 28, 12, 0,
            0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1,
            0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1,
            0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1,
            0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1,
            2, 3, 2, 3, 2, 3, 2, 3, 2, 3, 2, 3, 2, 3, 2, 3,
            2, 3, 2, 3, 2, 3, 2, 3, 2, 3, 2, 3, 2, 3, 2, 3,
            2, 3, 2, 3, 2, 3, 2, 3, 2, 3, 2, 3, 2, 3, 2, 3,
            2, 3, 2, 3, 2, 3, 2, 3, 2, 3, 2, 3, 2, 3, 2, 3
    };

    /// RFC 7.1 Lut1.
    private static final byte[] LUT1 = new byte[] {
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
            2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, 1, 1, 1, 1, 1,
            1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
            2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, 1, 1, 1, 1,
            1, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3,
            3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 1, 1, 1, 1, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
            2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2
    };

    /// RFC 7.1 Lut2.
    private static final byte[] LUT2 = new byte[] {
            0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
            2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
            2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
            2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
            3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3,
            3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3,
            3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3,
            3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3,
            4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
            4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
            4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
            4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
            5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
            5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
            5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
            6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 7
    };

    /// Prevents instantiation.
    private Brotli() {
    }

    /// Encodes `input` as a trivial RFC 7932 stream of uncompressed meta-blocks.
    ///
    /// @param input the uncompressed bytes
    /// @return a valid Brotli stream
    public static byte[] compress(byte[] input) {
        BitWriter writer = new BitWriter();
        writer.write(0, 1);
        if (input.length == 0) {
            writer.write(1, 1);
            writer.write(1, 1);
            writer.alignByte();
            return writer.toByteArray();
        }
        int offset = 0;
        while (offset < input.length) {
            int chunk = Math.min(input.length - offset, 1 << 16);
            writer.write(0, 1);
            int mlenMinus1 = chunk - 1;
            int nibbles = mlenMinus1 < (1 << 16) ? 0 : mlenMinus1 < (1 << 20) ? 1 : 2;
            writer.write(nibbles, 2);
            writer.write(mlenMinus1, (nibbles + 4) * 4);
            writer.write(1, 1);
            writer.alignByte();
            writer.writeBytes(input, offset, chunk);
            offset += chunk;
        }
        writer.write(1, 1);
        writer.write(1, 1);
        writer.alignByte();
        return writer.toByteArray();
    }

    /// Encodes `input` as a single compressed meta-block with LZ77 copies.
    ///
    /// Used by tests to drive the Huffman and copy paths that [`#compress(byte[])`] does not
    /// emit. The stream is still a valid RFC 7932 payload.
    ///
    /// @param input the uncompressed bytes
    /// @return a valid Brotli stream
    public static byte[] compressCommands(byte[] input) {
        if (input.length == 0) {
            return compress(input);
        }
        BitWriter writer = new BitWriter();
        writer.write(0, 1);
        writer.write(1, 1);
        writer.write(0, 1);
        int mlenMinus1 = input.length - 1;
        int nibbles = mlenMinus1 < (1 << 16) ? 0 : mlenMinus1 < (1 << 20) ? 1 : 2;
        writer.write(nibbles, 2);
        writer.write(mlenMinus1, (nibbles + 4) * 4);
        writer.write(0, 1);
        writer.write(0, 1);
        writer.write(0, 1);
        writer.write(0, 2);
        writer.write(0, 4);
        writer.write(0, 2);
        writer.write(0, 1);
        writer.write(0, 1);
        int[] literalLength = new int[256];
        int uniqueLiterals = 0;
        for (byte value : input) {
            int literal = value & 0xFF;
            if (literalLength[literal] == 0) {
                uniqueLiterals++;
            }
            literalLength[literal] = 1;
        }
        if (uniqueLiterals > 4) {
            Arrays.fill(literalLength, 8);
            writeComplexLengths(writer, literalLength);
        } else {
            prepareAndWritePrefix(writer, literalLength);
        }
        int[] usedCommands = new int[704];
        int[] usedDistances = new int[64];
        Command[] commands = planCommands(input, usedCommands, usedDistances);
        if (usedSymbols(usedCommands).length > 4 || usedSymbols(usedDistances).length > 4) {
            Arrays.fill(usedCommands, 0);
            Arrays.fill(usedDistances, 0);
            commands = planInsertOnly(input, usedCommands);
        }
        prepareAndWritePrefix(writer, usedCommands);
        prepareAndWritePrefix(writer, usedDistances);
        int[] literalCode = canonicalCodes(literalLength);
        int[] commandCode = canonicalCodes(usedCommands);
        int[] distanceCode = canonicalCodes(usedDistances);
        for (Command command : commands) {
            writeSymbol(writer, usedCommands, commandCode, command.code);
            int insertCode = command.insertCode;
            if (INSERT_EXTRA[insertCode] > 0) {
                writer.write(command.insert - INSERT_BASE[insertCode], INSERT_EXTRA[insertCode]);
            }
            int copyCode = command.copyCode;
            if (COPY_EXTRA[copyCode] > 0) {
                writer.write(command.copy - COPY_BASE[copyCode], COPY_EXTRA[copyCode]);
            }
            for (int index = 0; index < command.insert; index++) {
                int literal = input[command.literalOffset + index] & 0xFF;
                writeSymbol(writer, literalLength, literalCode, literal);
            }
            if (command.implicitDistance || command.insert >= input.length - command.literalOffset) {
                continue;
            }
            writeSymbol(writer, usedDistances, distanceCode, command.distanceCode);
            int extra = distanceExtraBits(command.distanceCode, 0, 0);
            if (extra > 0) {
                writer.write(command.distanceExtra, extra);
            }
        }
        writer.alignByte();
        return writer.toByteArray();
    }

    /// Inflates a RFC 7932 stream.
    ///
    /// @param input the Brotli bytes
    /// @return the uncompressed bytes
    public static byte[] decompress(byte[] input) {
        return new Decoder(input).inflate();
    }

    /// Assigns bit lengths and writes the matching prefix code.
    private static void prepareAndWritePrefix(BitWriter writer, int[] lengths) {
        int[] symbols = usedSymbols(lengths);
        if (symbols.length == 0) {
            lengths[0] = 1;
            symbols = new int[] {0};
        }
        Arrays.fill(lengths, 0);
        if (symbols.length <= 4) {
            if (symbols.length == 1) {
                lengths[symbols[0]] = 0;
            } else if (symbols.length == 2) {
                lengths[symbols[0]] = 1;
                lengths[symbols[1]] = 1;
            } else if (symbols.length == 3) {
                lengths[symbols[0]] = 1;
                lengths[symbols[1]] = 2;
                lengths[symbols[2]] = 2;
            } else {
                for (int symbol : symbols) {
                    lengths[symbol] = 2;
                }
            }
            writeSimple(writer, symbols, lengths.length);
            return;
        }
        int bits = 32 - Integer.numberOfLeadingZeros(symbols.length - 1);
        for (int symbol : symbols) {
            lengths[symbol] = bits;
        }
        writeComplexLengths(writer, lengths);
    }

    /// Writes a Section 3.4 simple prefix code.
    private static void writeSimple(BitWriter writer, int[] symbols, int alphabet) {
        writer.write(1, 2);
        writer.write(symbols.length - 1, 2);
        int bits = alphabetBits(alphabet);
        for (int symbol : symbols) {
            writer.write(symbol, bits);
        }
        if (symbols.length == 4) {
            writer.write(0, 1);
        }
    }

    /// Writes a complex prefix whose used symbols share one length.
    private static void writeComplexLengths(BitWriter writer, int[] lengths) {
        writer.write(0, 2);
        int[] cl = new int[18];
        for (int length : lengths) {
            cl[length] = 1;
        }
        int last = 0;
        int nonZero = 0;
        for (int index = 0; index < CL_ORDER.length; index++) {
            if (cl[CL_ORDER[index]] != 0) {
                last = index;
                nonZero++;
            }
        }
        int end = nonZero >= 2 ? last : CL_ORDER.length - 1;
        for (int index = 0; index <= end; index++) {
            writeCodeLengthCodeLength(writer, cl[CL_ORDER[index]]);
        }
        int clUsed = 0;
        for (int symbol = 0; symbol < cl.length; symbol++) {
            if (cl[symbol] != 0) {
                clUsed++;
            }
        }
        int lastSymbol = 0;
        for (int symbol = 0; symbol < lengths.length; symbol++) {
            if (lengths[symbol] != 0) {
                lastSymbol = symbol;
            }
        }
        if (clUsed == 1) {
            return;
        }
        int[] clCodes = canonicalCodes(cl);
        for (int symbol = 0; symbol <= lastSymbol; symbol++) {
            writeSymbol(writer, cl, clCodes, lengths[symbol]);
        }
    }

    /// Writes one 0..5 code-length-code length with the static prefix.
    private static void writeCodeLengthCodeLength(BitWriter writer, int length) {
        switch (length) {
            case 0 -> writer.write(0, 2);
            case 1 -> writer.write(7, 4);
            case 2 -> writer.write(3, 3);
            case 3 -> writer.write(2, 2);
            case 4 -> writer.write(1, 2);
            case 5 -> writer.write(15, 4);
            default -> throw new IllegalArgumentException("Invalid code-length-code length");
        }
    }

    /// Writes `symbol` with a canonical prefix built from `lengths`.
    private static void writeSymbol(BitWriter writer, int[] lengths, int[] codes, int symbol) {
        int length = lengths[symbol];
        if (length == 0) {
            return;
        }
        writer.write(reverseBits(codes[symbol], length), length);
    }

    /// Writes `symbol` when `lengths[symbol]` is the only stored length kind (used or unused).
    private static void writeSymbol(BitWriter writer, int[] lengths, int symbol) {
        int[] codes = canonicalCodes(lengths);
        writeSymbol(writer, lengths, codes, symbol);
    }

    /// Returns the symbols whose length is non-zero, in symbol order.
    private static int[] usedSymbols(int[] lengths) {
        int count = 0;
        for (int length : lengths) {
            if (length != 0) {
                count++;
            }
        }
        int[] symbols = new int[count];
        int write = 0;
        for (int symbol = 0; symbol < lengths.length; symbol++) {
            if (lengths[symbol] != 0) {
                symbols[write++] = symbol;
            }
        }
        return symbols;
    }

    /// Builds canonical Huffman codes from bit lengths.
    private static int[] canonicalCodes(int[] lengths) {
        int[] count = new int[16];
        int max = 0;
        for (int length : lengths) {
            if (length > 0) {
                count[length]++;
                max = Math.max(max, length);
            }
        }
        int[] next = new int[16];
        int code = 0;
        for (int bits = 1; bits <= max; bits++) {
            code = (code + count[bits - 1]) << 1;
            next[bits] = code;
        }
        int[] codes = new int[lengths.length];
        for (int symbol = 0; symbol < lengths.length; symbol++) {
            int length = lengths[symbol];
            if (length > 0) {
                codes[symbol] = next[length]++;
            }
        }
        return codes;
    }

    /// Returns `ceil(log2(alphabet))` bits needed to name a symbol.
    private static int alphabetBits(int alphabet) {
        if (alphabet <= 1) {
            return 0;
        }
        return 32 - Integer.numberOfLeadingZeros(alphabet - 1);
    }

    /// Reverses the low `length` bits of `value`.
    private static int reverseBits(int value, int length) {
        int reversed = 0;
        for (int index = 0; index < length; index++) {
            reversed = (reversed << 1) | ((value >> index) & 1);
        }
        return reversed;
    }

    /// Extra bits that follow a long distance code.
    private static int distanceExtraBits(int code, int npostfix, int ndirect) {
        if (code < 16 + ndirect) {
            return 0;
        }
        return 1 + ((code - ndirect - 16) >> (npostfix + 1));
    }

    /// Plans a single insert-only command that emits `input` as literals.
    private static Command[] planInsertOnly(byte[] input, int[] commandLengths) {
        int insertCode = insertCodeOf(input.length);
        int code = insertCopyCode(insertCode, 0, false);
        commandLengths[code] = 1;
        return new Command[] {
                new Command(code, insertCode, 0, input.length, 2, 0, true, 0, 0)
        };
    }

    /// Plans insert/copy commands and records the codes they need.
    private static Command[] planCommands(byte[] input, int[] commandLengths, int[] distanceLengths) {
        java.util.ArrayList<Command> planned = new java.util.ArrayList<>();
        int index = 0;
        while (index < input.length) {
            int insertStart = index;
            int bestLength = 0;
            int bestDistance = 0;
            int scan = index;
            while (scan < input.length) {
                int match = longestMatch(input, scan);
                if (match >= 2) {
                    bestLength = match >>> 16;
                    bestDistance = match & 0xFFFF;
                    break;
                }
                scan++;
            }
            int insert = scan - insertStart;
            if (scan == input.length) {
                int insertCode = insertCodeOf(insert);
                int copyCode = 0;
                int code = insertCopyCode(insertCode, copyCode, false);
                commandLengths[code] = 1;
                planned.add(new Command(code, insertCode, copyCode, insert, 2, insertStart, true, 0, 0));
                break;
            }
            int insertCode = insertCodeOf(insert);
            int copyCode = copyCodeOf(bestLength);
            boolean implicit = false;
            int code = insertCopyCode(insertCode, copyCode, implicit);
            commandLengths[code] = 1;
            int[] encoded = encodeDistance(bestDistance);
            int distanceCode = encoded[0];
            distanceLengths[distanceCode] = 1;
            planned.add(new Command(
                    code,
                    insertCode,
                    copyCode,
                    insert,
                    bestLength,
                    insertStart,
                    false,
                    distanceCode,
                    encoded[1]
            ));
            index = scan + bestLength;
        }
        if (planned.isEmpty()) {
            throw new IllegalStateException("Brotli command planner produced no commands");
        }
        return planned.toArray(Command[]::new);
    }

    /// Returns `(length << 16) | distance` for the longest match of at least 2 at `index`.
    private static int longestMatch(byte[] input, int index) {
        int bestLength = 0;
        int bestDistance = 0;
        int limit = Math.min(index, 1024);
        for (int distance = 1; distance <= limit; distance++) {
            int length = 0;
            while (index + length < input.length
                    && input[index + length] == input[index - distance + length]
                    && length < 258) {
                length++;
            }
            if (length > bestLength) {
                bestLength = length;
                bestDistance = distance;
            }
        }
        if (bestLength < 2) {
            return 0;
        }
        return (bestLength << 16) | bestDistance;
    }

    /// Insert-length code for `insert`.
    private static int insertCodeOf(int insert) {
        for (int code = 23; code >= 0; code--) {
            int span = 1 << INSERT_EXTRA[code];
            if (insert >= INSERT_BASE[code] && insert < INSERT_BASE[code] + span) {
                return code;
            }
        }
        throw new IllegalArgumentException("Insert length is out of range");
    }

    /// Copy-length code for `copy`.
    private static int copyCodeOf(int copy) {
        for (int code = 23; code >= 0; code--) {
            int span = 1 << COPY_EXTRA[code];
            if (copy >= COPY_BASE[code] && copy < COPY_BASE[code] + span) {
                return code;
            }
        }
        throw new IllegalArgumentException("Copy length is out of range");
    }

    /// Insert-and-copy symbol for the RFC Section 5 cell table.
    private static int insertCopyCode(int insertCode, int copyCode, boolean implicit) {
        int bits = (copyCode & 7) | ((insertCode & 7) << 3);
        int insertCell = insertCode >> 3;
        int copyCell = copyCode >> 3;
        if (implicit) {
            if (insertCell != 0 || copyCell > 1) {
                throw new IllegalArgumentException("Implicit distance is only valid for codes 0-127");
            }
            return copyCell == 0 ? bits : 64 + bits;
        }
        int[][] base = {
                {128, 192, 384},
                {256, 320, 512},
                {448, 576, 640}
        };
        return base[insertCell][copyCell] + bits;
    }

    /// Distance code and extra bits for a backward distance when NPOSTFIX=0 and NDIRECT=0.
    private static int[] encodeDistance(int distance) {
        if (distance <= 0) {
            throw new IllegalArgumentException("Distance must be positive");
        }
        int dcode = 16;
        while (true) {
            int extraBits = distanceExtraBits(dcode, 0, 0);
            int hcode = dcode - 16;
            int offset = ((2 + (hcode & 1)) << extraBits) - 4;
            int base = offset + 1;
            int span = 1 << extraBits;
            if (distance >= base && distance < base + span) {
                return new int[] {dcode, distance - base};
            }
            dcode++;
            if (dcode >= 64) {
                throw new IllegalArgumentException("Distance is too large for NDIRECT=0");
            }
        }
    }

    /// One planned insert/copy command.
    private record Command(
            int code,
            int insertCode,
            int copyCode,
            int insert,
            int copy,
            int literalOffset,
            boolean implicitDistance,
            int distanceCode,
            int distanceExtra
    ) {
    }

    /// LSB-first bit writer.
    private static final class BitWriter {
        /// Pending bits, least-significant first.
        private long acc;

        /// Number of valid bits in [`#acc`].
        private int bits;

        /// Emitted bytes.
        private final ByteArrayOutputStream output = new ByteArrayOutputStream();

        /// Writes the low `count` bits of `value`.
        void write(int value, int count) {
            if (count == 0) {
                return;
            }
            acc |= (value & ((1L << count) - 1)) << bits;
            bits += count;
            while (bits >= 8) {
                output.write((int) (acc & 0xFF));
                acc >>>= 8;
                bits -= 8;
            }
        }

        /// Writes `length` bytes starting at `offset`.
        void writeBytes(byte[] source, int offset, int length) {
            output.write(source, offset, length);
        }

        /// Pads to a byte boundary with zeros.
        void alignByte() {
            if (bits > 0) {
                output.write((int) (acc & 0xFF));
                acc = 0;
                bits = 0;
            }
        }

        /// Returns the emitted stream.
        byte[] toByteArray() {
            return output.toByteArray();
        }
    }

    /// LSB-first bit reader.
    private static final class BitReader {
        /// Source bytes.
        private final byte[] data;

        /// Next unread byte.
        private int offset;

        /// Pending bits, least-significant first.
        private long acc;

        /// Number of valid bits in [`#acc`].
        private int bits;

        /// Creates a reader over `data`.
        BitReader(byte[] data) {
            this.data = data;
        }

        /// Returns whether at least `count` bits can still be produced, including unread bytes.
        boolean hasBits(int count) {
            return bits + ((data.length - offset) * 8) >= count;
        }

        /// Reads `count` bits as an unsigned integer.
        int read(int count) {
            if (count == 0) {
                return 0;
            }
            fill(count);
            int value = (int) (acc & ((1L << count) - 1));
            acc >>>= count;
            bits -= count;
            return value;
        }

        /// Peeks `count` bits without consuming them.
        int peek(int count) {
            if (count == 0) {
                return 0;
            }
            fill(count);
            return (int) (acc & ((1L << count) - 1));
        }

        /// Drops `count` previously peeked bits.
        void consume(int count) {
            if (count == 0) {
                return;
            }
            acc >>>= count;
            bits -= count;
        }

        /// Discards bits up to the next byte boundary.
        void alignByte() {
            int drop = bits & 7;
            if (drop != 0) {
                acc >>>= drop;
                bits -= drop;
            }
        }

        /// Copies `length` aligned bytes into `target` at `dest`.
        void readBytes(byte[] target, int dest, int length) {
            alignByte();
            if (bits != 0) {
                throw new IllegalArgumentException("Brotli byte copy is not aligned");
            }
            if (offset + length > data.length) {
                throw new IllegalArgumentException("Brotli uncompressed payload is truncated");
            }
            System.arraycopy(data, offset, target, dest, length);
            offset += length;
        }

        /// Loads at least `count` bits into the accumulator.
        private void fill(int count) {
            while (bits < count) {
                if (offset >= data.length) {
                    throw new IllegalArgumentException("Brotli stream is truncated");
                }
                acc |= (data[offset++] & 0xFFL) << bits;
                bits += 8;
            }
        }
    }

    /// Canonical Huffman decoder table.
    private static final class Huffman {
        /// Symbol for each peek of [`#maxBits`] bits.
        private final int[] symbols;

        /// Consumed length for each peek of [`#maxBits`] bits.
        private final int[] lengths;

        /// Longest code, or 0 for a one-symbol table.
        private final int maxBits;

        /// Sole symbol when [`#maxBits`] is 0.
        private final int single;

        /// Builds a zero-bit table that always returns `symbol`.
        ///
        /// @param symbol the sole alphabet member
        Huffman(int symbol) {
            this.symbols = new int[1];
            this.lengths = new int[1];
            this.maxBits = 0;
            this.single = symbol;
        }

        /// Builds a table from `codeLengths`.
        Huffman(int[] codeLengths) {
            int used = 0;
            int max = 0;
            int only = 0;
            for (int symbol = 0; symbol < codeLengths.length; symbol++) {
                int length = codeLengths[symbol];
                if (length > 0) {
                    used++;
                    only = symbol;
                    max = Math.max(max, length);
                }
            }
            if (used == 0) {
                throw new IllegalArgumentException("Huffman table has no symbols");
            }
            if (used == 1 && max == 0) {
                this.symbols = new int[1];
                this.lengths = new int[1];
                this.maxBits = 0;
                this.single = only;
                return;
            }
            if (used == 1) {
                this.symbols = new int[1];
                this.lengths = new int[1];
                this.maxBits = 0;
                this.single = only;
                return;
            }
            int[] codes = canonicalCodes(codeLengths);
            int size = 1 << max;
            int[] tableSymbols = new int[size];
            int[] tableLengths = new int[size];
            Arrays.fill(tableSymbols, -1);
            for (int symbol = 0; symbol < codeLengths.length; symbol++) {
                int length = codeLengths[symbol];
                if (length == 0) {
                    continue;
                }
                int reversed = reverseBits(codes[symbol], length);
                int step = 1 << length;
                for (int fill = reversed; fill < size; fill += step) {
                    tableSymbols[fill] = symbol;
                    tableLengths[fill] = length;
                }
            }
            this.symbols = tableSymbols;
            this.lengths = tableLengths;
            this.maxBits = max;
            this.single = only;
        }

        /// Reads one symbol.
        int decode(BitReader reader) {
            if (maxBits == 0) {
                return single;
            }
            int peek = reader.peek(maxBits);
            int symbol = symbols[peek];
            if (symbol < 0) {
                throw new IllegalArgumentException("Brotli Huffman code is invalid");
            }
            reader.consume(lengths[peek]);
            return symbol;
        }
    }

    /// Inflates one stream.
    private static final class Decoder {
        /// Bit source.
        private final BitReader reader;

        /// Sliding-window size in bytes.
        private int window;

        /// Last four distances, newest at index 0.
        private final int[] distances = {16, 15, 11, 4};

        /// Previous two uncompressed bytes, `p1` then `p2`.
        private int previous1;

        /// Second-previous uncompressed byte.
        private int previous2;

        /// Creates a decoder over `input`.
        Decoder(byte[] input) {
            this.reader = new BitReader(input);
        }

        /// Inflates the whole stream.
        byte[] inflate() {
            window = decodeWindowBits();
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            while (true) {
                boolean last = reader.read(1) != 0;
                if (last && reader.read(1) != 0) {
                    break;
                }
                int nibbles = reader.read(2);
                if (nibbles == 3) {
                    skipMetadata();
                    continue;
                }
                int mlen = reader.read((nibbles + 4) * 4) + 1;
                if (output.size() + (long) mlen > MAX_OUTPUT) {
                    throw new IllegalArgumentException("Brotli output exceeds " + MAX_OUTPUT);
                }
                boolean uncompressed = !last && reader.read(1) != 0;
                if (uncompressed) {
                    copyUncompressed(output, mlen);
                } else {
                    inflateCompressed(output, mlen);
                }
                if (last) {
                    break;
                }
            }
            return output.toByteArray();
        }

        /// Reads WBITS and returns the window size in bytes.
        private int decodeWindowBits() {
            if (reader.read(1) == 0) {
                return (1 << 16) - 16;
            }
            int n = reader.read(3);
            if (n != 0) {
                return (1 << (17 + n)) - 16;
            }
            n = reader.read(3);
            if (n == 1) {
                throw new IllegalArgumentException("Large-window Brotli is not accepted");
            }
            if (n != 0) {
                return (1 << (8 + n)) - 16;
            }
            return (1 << 17) - 16;
        }

        /// Skips a metadata meta-block.
        private void skipMetadata() {
            if (reader.read(1) != 0) {
                throw new IllegalArgumentException("Brotli metadata reserved bit must be zero");
            }
            int skipBytes = reader.read(2);
            int skip = 0;
            if (skipBytes != 0) {
                skip = reader.read(skipBytes * 8) + 1;
            }
            reader.alignByte();
            byte[] ignored = new byte[skip];
            reader.readBytes(ignored, 0, skip);
        }

        /// Copies an uncompressed meta-block.
        private void copyUncompressed(ByteArrayOutputStream output, int mlen) {
            reader.alignByte();
            byte[] chunk = new byte[mlen];
            reader.readBytes(chunk, 0, mlen);
            output.write(chunk, 0, mlen);
            if (mlen >= 2) {
                previous2 = chunk[mlen - 2] & 0xFF;
                previous1 = chunk[mlen - 1] & 0xFF;
            } else if (mlen == 1) {
                previous2 = previous1;
                previous1 = chunk[0] & 0xFF;
            }
        }

        /// Inflates a compressed meta-block of `mlen` bytes.
        private void inflateCompressed(ByteArrayOutputStream output, int mlen) {
            int literalTypes = reader.read(1) == 0 ? 1 : decodeVarLenUint8() + 1;
            Huffman literalType = null;
            Huffman literalCount = null;
            int literalBlock = 0;
            int literalPrev = 1;
            int literalRemaining = Integer.MAX_VALUE;
            if (literalTypes >= 2) {
                literalType = readPrefix(literalTypes + 2);
                literalCount = readPrefix(26);
                literalRemaining = readBlockCount(literalCount);
            }
            int insertTypes = reader.read(1) == 0 ? 1 : decodeVarLenUint8() + 1;
            Huffman insertType = null;
            Huffman insertCount = null;
            int insertBlock = 0;
            int insertPrev = 1;
            int insertRemaining = Integer.MAX_VALUE;
            if (insertTypes >= 2) {
                insertType = readPrefix(insertTypes + 2);
                insertCount = readPrefix(26);
                insertRemaining = readBlockCount(insertCount);
            }
            int distanceTypes = reader.read(1) == 0 ? 1 : decodeVarLenUint8() + 1;
            Huffman distanceType = null;
            Huffman distanceCount = null;
            int distanceBlock = 0;
            int distancePrev = 1;
            int distanceRemaining = Integer.MAX_VALUE;
            if (distanceTypes >= 2) {
                distanceType = readPrefix(distanceTypes + 2);
                distanceCount = readPrefix(26);
                distanceRemaining = readBlockCount(distanceCount);
            }
            int npostfix = reader.read(2);
            int ndirect = reader.read(4) << npostfix;
            int[] contextModes = new int[literalTypes];
            for (int index = 0; index < literalTypes; index++) {
                contextModes[index] = reader.read(2);
            }
            int[] literalMap = decodeContextMap(literalTypes * 64);
            int literalTrees = maxValue(literalMap) + 1;
            int[] distanceMap = decodeContextMap(distanceTypes * 4);
            int distanceTrees = maxValue(distanceMap) + 1;
            Huffman[] literals = new Huffman[literalTrees];
            for (int index = 0; index < literalTrees; index++) {
                literals[index] = readPrefix(256);
            }
            Huffman[] commands = new Huffman[insertTypes];
            for (int index = 0; index < insertTypes; index++) {
                commands[index] = readPrefix(704);
            }
            int distanceAlphabet = 16 + ndirect + (48 << npostfix);
            Huffman[] dists = new Huffman[distanceTrees];
            for (int index = 0; index < distanceTrees; index++) {
                dists[index] = readPrefix(distanceAlphabet);
            }
            byte[] ring = output.toByteArray();
            int written = ring.length;
            byte[] produced = new byte[mlen];
            int out = 0;
            while (out < mlen) {
                if (insertTypes >= 2) {
                    if (insertRemaining == 0) {
                        int switched = switchBlock(insertType, insertBlock, insertPrev, insertTypes);
                        insertPrev = insertBlock;
                        insertBlock = switched;
                        insertRemaining = readBlockCount(insertCount);
                    }
                    insertRemaining--;
                }
                int command = commands[insertBlock].decode(reader);
                int insertCode;
                int copyCode;
                boolean implicit;
                int decoded = decodeInsertCopy(command);
                implicit = (decoded & 1) != 0;
                insertCode = (decoded >> 1) & 31;
                copyCode = (decoded >> 6) & 31;
                int insert = INSERT_BASE[insertCode] + reader.read(INSERT_EXTRA[insertCode]);
                int copy = COPY_BASE[copyCode] + reader.read(COPY_EXTRA[copyCode]);
                for (int n = 0; n < insert && out < mlen; n++) {
                    if (literalTypes >= 2) {
                        if (literalRemaining == 0) {
                            int switched = switchBlock(literalType, literalBlock, literalPrev, literalTypes);
                            literalPrev = literalBlock;
                            literalBlock = switched;
                            literalRemaining = readBlockCount(literalCount);
                        }
                        literalRemaining--;
                    }
                    int context = literalContext(contextModes[literalBlock], previous1, previous2);
                    int tree = literalMap[literalBlock * 64 + context];
                    int literal = literals[tree].decode(reader);
                    produced[out++] = (byte) literal;
                    previous2 = previous1;
                    previous1 = literal;
                }
                if (out >= mlen) {
                    break;
                }
                int distance;
                if (implicit) {
                    distance = distances[0];
                } else {
                    if (distanceTypes >= 2) {
                        if (distanceRemaining == 0) {
                            int switched = switchBlock(distanceType, distanceBlock, distancePrev, distanceTypes);
                            distancePrev = distanceBlock;
                            distanceBlock = switched;
                            distanceRemaining = readBlockCount(distanceCount);
                        }
                        distanceRemaining--;
                    }
                    int distContext = copy == 2 ? 0 : copy == 3 ? 1 : copy == 4 ? 2 : 3;
                    int tree = distanceMap[distanceBlock * 4 + distContext];
                    int dcode = dists[tree].decode(reader);
                    distance = resolveDistance(dcode, npostfix, ndirect);
                }
                int available = written + out;
                if (distance > available) {
                    throw new IllegalArgumentException("Brotli static-dictionary distance is not supported");
                }
                if (distance > window) {
                    throw new IllegalArgumentException("Brotli copy distance exceeds the window");
                }
                for (int n = 0; n < copy && out < mlen; n++) {
                    int source = written + out - distance;
                    int value = source < written ? ring[source] & 0xFF : produced[source - written] & 0xFF;
                    produced[out++] = (byte) value;
                    previous2 = previous1;
                    previous1 = value;
                }
            }
            output.write(produced, 0, mlen);
        }

        /// Reads a prefix code of `alphabet` symbols.
        private Huffman readPrefix(int alphabet) {
            int skip = reader.read(2);
            if (skip == 1) {
                return readSimple(alphabet);
            }
            return readComplex(alphabet, skip);
        }

        /// Reads a simple prefix code.
        private Huffman readSimple(int alphabet) {
            int nsym = reader.read(2) + 1;
            int bits = alphabetBits(alphabet);
            int[] symbols = new int[nsym];
            int[] lengths = new int[alphabet];
            for (int index = 0; index < nsym; index++) {
                int symbol = reader.read(bits);
                if (symbol >= alphabet) {
                    throw new IllegalArgumentException("Simple Huffman symbol is outside the alphabet");
                }
                for (int prior = 0; prior < index; prior++) {
                    if (symbols[prior] == symbol) {
                        throw new IllegalArgumentException("Simple Huffman symbols must be unique");
                    }
                }
                symbols[index] = symbol;
            }
            if (nsym == 1) {
                return new Huffman(symbols[0]);
            }
            if (nsym == 2) {
                lengths[symbols[0]] = 1;
                lengths[symbols[1]] = 1;
                return new Huffman(lengths);
            }
            if (nsym == 3) {
                lengths[symbols[0]] = 1;
                lengths[symbols[1]] = 2;
                lengths[symbols[2]] = 2;
                return new Huffman(lengths);
            }
            int select = reader.read(1);
            if (select == 0) {
                for (int symbol : symbols) {
                    lengths[symbol] = 2;
                }
            } else {
                lengths[symbols[0]] = 1;
                lengths[symbols[1]] = 2;
                lengths[symbols[2]] = 3;
                lengths[symbols[3]] = 3;
            }
            return new Huffman(lengths);
        }

        /// Reads a complex prefix code.
        private Huffman readComplex(int alphabet, int hskip) {
            int[] cl = new int[18];
            int start = hskip == 0 ? 0 : hskip;
            int space = 32;
            int nonZero = 0;
            for (int index = start; index < CL_ORDER.length; index++) {
                int peek = reader.peek(4);
                int length = CL_PREFIX_LENGTH[peek];
                int value = CL_PREFIX_VALUE[peek];
                reader.consume(length);
                cl[CL_ORDER[index]] = value;
                if (value != 0) {
                    space -= 32 >> value;
                    nonZero++;
                    if (space <= 0) {
                        break;
                    }
                }
            }
            if (nonZero != 1 && space != 0) {
                throw new IllegalArgumentException("Brotli code-length prefix is not complete");
            }
            Huffman clHuffman = new Huffman(cl);
            int[] lengths = new int[alphabet];
            int symbol = 0;
            int previous = 8;
            int lastRepeat = 0;
            int lastRepeatKind = -1;
            while (symbol < alphabet) {
                int code = clHuffman.decode(reader);
                if (code <= 15) {
                    lengths[symbol++] = code;
                    if (code != 0) {
                        previous = code;
                    }
                    lastRepeat = 0;
                    lastRepeatKind = -1;
                    continue;
                }
                if (code == 16) {
                    int extra = reader.read(2);
                    int repeat;
                    if (lastRepeatKind == 16) {
                        lastRepeat = (4 * (lastRepeat - 2)) + extra + 3;
                        repeat = lastRepeat;
                    } else {
                        repeat = extra + 3;
                        lastRepeat = repeat;
                    }
                    lastRepeatKind = 16;
                    int fill = previous;
                    for (int n = 0; n < repeat; n++) {
                        if (symbol >= alphabet) {
                            throw new IllegalArgumentException("Brotli repeat exceeds the alphabet");
                        }
                        lengths[symbol++] = fill;
                    }
                    continue;
                }
                if (code != 17) {
                    throw new IllegalArgumentException("Brotli code-length symbol is invalid");
                }
                int extra = reader.read(3);
                int repeat;
                if (lastRepeatKind == 17) {
                    lastRepeat = (8 * (lastRepeat - 2)) + extra + 3;
                    repeat = lastRepeat;
                } else {
                    repeat = extra + 3;
                    lastRepeat = repeat;
                }
                lastRepeatKind = 17;
                symbol += repeat;
                if (symbol > alphabet) {
                    throw new IllegalArgumentException("Brotli zero-repeat exceeds the alphabet");
                }
            }
            return new Huffman(lengths);
        }

        /// Reads NTREES and the optional context map.
        private int[] decodeContextMap(int size) {
            int trees = reader.read(1) == 0 ? 1 : decodeVarLenUint8() + 1;
            int[] map = new int[size];
            if (trees <= 1) {
                return map;
            }
            int rleMax;
            if (reader.read(1) != 0) {
                rleMax = reader.read(4) + 1;
            } else {
                rleMax = 0;
            }
            Huffman prefix = readPrefix(trees + rleMax);
            int index = 0;
            while (index < size) {
                int code = prefix.decode(reader);
                if (code == 0) {
                    map[index++] = 0;
                    continue;
                }
                if (code > rleMax) {
                    map[index++] = code - rleMax;
                    continue;
                }
                int reps = (1 << code) + reader.read(code);
                if (index + reps > size) {
                    throw new IllegalArgumentException("Brotli context-map RLE exceeds the map");
                }
                index += reps;
            }
            if (reader.read(1) != 0) {
                inverseMoveToFront(map);
            }
            return map;
        }

        /// Reads a Section 9.2 variable-length integer in 0..255 after a leading 1 bit.
        private int decodeVarLenUint8() {
            int nbits = reader.read(3);
            if (nbits == 0) {
                return 1;
            }
            return (1 << nbits) + reader.read(nbits);
        }

        /// Reads a block count.
        private int readBlockCount(Huffman counts) {
            int code = counts.decode(reader);
            return BLOCK_BASE[code] + reader.read(BLOCK_EXTRA[code]);
        }

        /// Applies a block-type switch and returns the new type.
        private int switchBlock(Huffman types, int current, int previous, int typeCount) {
            int code = types.decode(reader);
            int next;
            if (code == 0) {
                next = previous;
            } else if (code == 1) {
                next = current + 1;
                if (next >= typeCount) {
                    next = 0;
                }
            } else {
                next = code - 2;
                if (next >= typeCount) {
                    next -= typeCount;
                }
            }
            return next;
        }

        /// Decodes an insert-and-copy symbol into implicit flag, insert code, and copy code.
        private int decodeInsertCopy(int code) {
            boolean implicit = code < 128;
            int insertBase;
            int copyBase;
            int local = code;
            if (code < 64) {
                insertBase = 0;
                copyBase = 0;
            } else if (code < 128) {
                insertBase = 0;
                copyBase = 8;
                local -= 64;
            } else if (code < 192) {
                insertBase = 0;
                copyBase = 0;
                local -= 128;
            } else if (code < 256) {
                insertBase = 0;
                copyBase = 8;
                local -= 192;
            } else if (code < 320) {
                insertBase = 8;
                copyBase = 0;
                local -= 256;
            } else if (code < 384) {
                insertBase = 8;
                copyBase = 8;
                local -= 320;
            } else if (code < 448) {
                insertBase = 0;
                copyBase = 16;
                local -= 384;
            } else if (code < 512) {
                insertBase = 16;
                copyBase = 0;
                local -= 448;
            } else if (code < 576) {
                insertBase = 8;
                copyBase = 16;
                local -= 512;
            } else if (code < 640) {
                insertBase = 16;
                copyBase = 8;
                local -= 576;
            } else {
                insertBase = 16;
                copyBase = 16;
                local -= 640;
            }
            int insertCode = insertBase + ((local >> 3) & 7);
            int copyCode = copyBase + (local & 7);
            return (implicit ? 1 : 0) | (insertCode << 1) | (copyCode << 6);
        }

        /// Resolves a distance code to a backward distance and updates the ring.
        private int resolveDistance(int code, int npostfix, int ndirect) {
            int distance;
            if (code < 16) {
                distance = shortDistance(code);
                if (distance <= 0) {
                    throw new IllegalArgumentException("Brotli short distance is not positive");
                }
                if (code != 0) {
                    pushDistance(distance);
                }
                return distance;
            }
            if (code < 16 + ndirect) {
                distance = code - 15;
                pushDistance(distance);
                return distance;
            }
            int extraBits = distanceExtraBits(code, npostfix, ndirect);
            int extra = reader.read(extraBits);
            int hcode = (code - ndirect - 16) >> npostfix;
            int lcode = (code - ndirect - 16) & ((1 << npostfix) - 1);
            int offset = ((2 + (hcode & 1)) << extraBits) - 4;
            distance = ((offset + extra) << npostfix) + lcode + ndirect + 1;
            pushDistance(distance);
            return distance;
        }

        /// Resolves one of the 16 short distance symbols.
        private int shortDistance(int code) {
            return switch (code) {
                case 0 -> distances[0];
                case 1 -> distances[1];
                case 2 -> distances[2];
                case 3 -> distances[3];
                case 4 -> distances[0] - 1;
                case 5 -> distances[0] + 1;
                case 6 -> distances[0] - 2;
                case 7 -> distances[0] + 2;
                case 8 -> distances[0] - 3;
                case 9 -> distances[0] + 3;
                case 10 -> distances[1] - 1;
                case 11 -> distances[1] + 1;
                case 12 -> distances[1] - 2;
                case 13 -> distances[1] + 2;
                case 14 -> distances[1] - 3;
                case 15 -> distances[1] + 3;
                default -> throw new IllegalArgumentException("Short distance code is out of range");
            };
        }

        /// Pushes `distance` as the newest last distance.
        private void pushDistance(int distance) {
            if (distance == distances[0]) {
                return;
            }
            distances[3] = distances[2];
            distances[2] = distances[1];
            distances[1] = distances[0];
            distances[0] = distance;
        }

        /// Literal context ID for `mode`.
        private int literalContext(int mode, int p1, int p2) {
            return switch (mode) {
                case 0 -> p1 & 0x3F;
                case 1 -> p1 >> 2;
                case 2 -> (LUT0[p1] & 0xFF) | (LUT1[p2] & 0xFF);
                default -> ((LUT2[p1] & 0xFF) << 3) | (LUT2[p2] & 0xFF);
            };
        }
    }

    /// One-symbol length table with `symbol` at `length`.
    private static int[] lengthsAt(int alphabet, int symbol, int length) {
        int[] lengths = new int[alphabet];
        lengths[symbol] = length;
        return lengths;
    }

    /// Inverse move-to-front of a context map.
    private static void inverseMoveToFront(int[] map) {
        int[] mtf = new int[256];
        for (int index = 0; index < 256; index++) {
            mtf[index] = index;
        }
        for (int index = 0; index < map.length; index++) {
            int slot = map[index];
            int value = mtf[slot];
            map[index] = value;
            System.arraycopy(mtf, 0, mtf, 1, slot);
            mtf[0] = value;
        }
    }

    /// Maximum value in `values`.
    private static int maxValue(int[] values) {
        int max = 0;
        for (int value : values) {
            if (value > max) {
                max = value;
            }
        }
        return max;
    }
}

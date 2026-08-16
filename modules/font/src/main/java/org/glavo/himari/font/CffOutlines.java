package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

/// Parses `CFF ` and `CFF2` and walks unhinted Type 2 charstrings into an [`OutlinePen`].
///
/// CFF 1 covers a single name-keyed font. CFF2 covers a single Font DICT. Stem hints, hint masks,
/// `vsindex`, and `blend` without variation regions are consumed and discarded. Type 1 charstrings
/// are rejected. Coordinates stay in font units with y upward.
@NotNullByDefault
final class CffOutlines {
    /// Maximum nested `callsubr` / `callgsubr` depth.
    private static final int MAX_SUBR_DEPTH = 10;

    /// CFF 1 stack limit.
    private static final int STACK_CFF1 = 48;

    /// CFF 2 stack limit.
    private static final int STACK_CFF2 = 513;

    /// Two-byte DICT/charstring escape prefix as `0x0C00 | second`.
    private static final int ESCAPE = 0x0C00;

    /// Per-glyph Type 2 programs.
    private final byte[][] charstrings;

    /// Global subroutines.
    private final byte[][] globalSubrs;

    /// Local subroutines of the selected Private DICT.
    private final byte[][] localSubrs;

    /// Whether the table is CFF2.
    private final boolean cff2;

    /// Creates a parsed outline table.
    private CffOutlines(byte[][] charstrings, byte[][] globalSubrs, byte[][] localSubrs, boolean cff2) {
        this.charstrings = charstrings;
        this.globalSubrs = globalSubrs;
        this.localSubrs = localSubrs;
        this.cff2 = cff2;
    }

    /// Parses `CFF2` when present, otherwise `CFF `.
    ///
    /// @param cff the CFF 1 table, or `null`
    /// @param cff2 the CFF2 table, or `null`
    /// @return the outlines
    static CffOutlines parse(@Nullable ByteBuffer cff, @Nullable ByteBuffer cff2) {
        if (cff2 != null) {
            return parseTable(cff2, true);
        }
        if (cff == null) {
            throw new IllegalArgumentException("CFF table is missing");
        }
        return parseTable(cff, false);
    }

    /// Walks `glyphId` into `pen`.
    ///
    /// @param glyphId the glyph identity
    /// @param pen the destination
    void outline(int glyphId, OutlinePen pen) {
        Objects.requireNonNull(pen, "pen");
        if (glyphId < 0 || glyphId >= charstrings.length) {
            throw new IllegalArgumentException("Unknown CFF glyph " + glyphId);
        }
        byte[] program = charstrings[glyphId];
        if (program.length == 0) {
            return;
        }
        Type2Decoder.decode(program, globalSubrs, localSubrs, cff2, pen);
    }

    /// Returns the CharStrings count.
    ///
    /// @return the glyph count
    int glyphCount() {
        return charstrings.length;
    }

    /// Returns whether this table is CFF2.
    ///
    /// @return whether the source was `CFF2`
    boolean isCff2() {
        return cff2;
    }

    /// Parses one CFF or CFF2 table.
    private static CffOutlines parseTable(ByteBuffer table, boolean cff2) {
        ByteBuffer buffer = table.duplicate().order(ByteOrder.BIG_ENDIAN);
        buffer.clear();
        if (buffer.remaining() < 4) {
            throw new IllegalArgumentException("CFF header is truncated");
        }
        int major = buffer.get() & 0xFF;
        buffer.get();
        int headerSize = buffer.get() & 0xFF;
        if (cff2) {
            if (major != 2 || headerSize < 5 || buffer.remaining() < 2) {
                throw new IllegalArgumentException("CFF2 header is invalid");
            }
            int topDictLength = Short.toUnsignedInt(buffer.getShort());
            buffer.position(headerSize);
            if (buffer.remaining() < topDictLength) {
                throw new IllegalArgumentException("CFF2 Top DICT is truncated");
            }
            int topStart = buffer.position();
            ByteBuffer topDict = slice(buffer, topStart, topDictLength);
            buffer.position(topStart + topDictLength);
            Index globalSubrs = readIndex(buffer, true);
            DictValues top = readDict(topDict);
            if (top.charStrings < 0 || top.fdArray < 0) {
                throw new IllegalArgumentException("CFF2 Top DICT must name CharStrings and FDArray");
            }
            Index fdArray = readIndexAt(buffer, top.fdArray, true);
            if (fdArray.count < 1) {
                throw new IllegalArgumentException("CFF2 FDArray is empty");
            }
            DictValues fontDict = readDict(fdArray.object(0));
            byte[][] localSubrs = readPrivateSubrs(buffer, fontDict, true);
            Index charstrings = readIndexAt(buffer, top.charStrings, true);
            return new CffOutlines(charstrings.objects, globalSubrs.objects, localSubrs, true);
        }
        if (major != 1 || headerSize < 4) {
            throw new IllegalArgumentException("CFF header is invalid");
        }
        buffer.get();
        buffer.position(headerSize);
        readIndex(buffer, false);
        Index topIndex = readIndex(buffer, false);
        if (topIndex.count < 1) {
            throw new IllegalArgumentException("CFF Top DICT INDEX is empty");
        }
        readIndex(buffer, false);
        Index globalSubrs = readIndex(buffer, false);
        DictValues top = readDict(topIndex.object(0));
        if (top.charstringType != 2) {
            throw new IllegalArgumentException("Only Type 2 charstrings are supported");
        }
        if (top.charStrings < 0) {
            throw new IllegalArgumentException("CFF Top DICT must name CharStrings");
        }
        byte[][] localSubrs = readPrivateSubrs(buffer, top, false);
        Index charstrings = readIndexAt(buffer, top.charStrings, false);
        return new CffOutlines(charstrings.objects, globalSubrs.objects, localSubrs, false);
    }

    /// Reads Private DICT local subroutines, or an empty list.
    private static byte[][] readPrivateSubrs(ByteBuffer table, DictValues dict, boolean cff2) {
        if (dict.privateSize <= 0 || dict.privateOffset < 0) {
            return new byte[0][];
        }
        ByteBuffer priv = slice(table, dict.privateOffset, dict.privateSize);
        DictValues values = readDict(priv);
        if (values.subrs < 0) {
            return new byte[0][];
        }
        return readIndexAt(table, dict.privateOffset + values.subrs, cff2).objects;
    }

    /// Reads an INDEX at an absolute table offset.
    private static Index readIndexAt(ByteBuffer table, int offset, boolean cff2) {
        if (offset < 0 || offset > table.capacity()) {
            throw new IllegalArgumentException("CFF INDEX offset is out of range");
        }
        ByteBuffer view = table.duplicate().order(ByteOrder.BIG_ENDIAN);
        view.clear();
        view.position(offset);
        return readIndex(view, cff2);
    }

    /// Reads one INDEX from `buffer`.
    private static Index readIndex(ByteBuffer buffer, boolean cff2) {
        if (cff2) {
            if (buffer.remaining() < 4) {
                throw new IllegalArgumentException("CFF2 INDEX is truncated");
            }
            int count = buffer.getInt();
            if (count == 0) {
                return Index.EMPTY;
            }
            if (count < 0) {
                throw new IllegalArgumentException("CFF2 INDEX count is negative");
            }
            return readIndexBody(buffer, count);
        }
        if (buffer.remaining() < 2) {
            throw new IllegalArgumentException("CFF INDEX is truncated");
        }
        int count = Short.toUnsignedInt(buffer.getShort());
        if (count == 0) {
            return Index.EMPTY;
        }
        return readIndexBody(buffer, count);
    }

    /// Reads INDEX offsets and object bytes.
    private static Index readIndexBody(ByteBuffer buffer, int count) {
        if (buffer.remaining() < 1) {
            throw new IllegalArgumentException("CFF INDEX offSize is missing");
        }
        int offSize = buffer.get() & 0xFF;
        if (offSize < 1 || offSize > 4) {
            throw new IllegalArgumentException("CFF INDEX offSize is invalid");
        }
        int offsetBytes = Math.multiplyExact(count + 1, offSize);
        if (buffer.remaining() < offsetBytes) {
            throw new IllegalArgumentException("CFF INDEX offsets are truncated");
        }
        int[] offsets = new int[count + 1];
        for (int index = 0; index <= count; index++) {
            offsets[index] = readOffset(buffer, offSize);
        }
        if (offsets[0] != 1) {
            throw new IllegalArgumentException("CFF INDEX first offset must be 1");
        }
        int dataSize = offsets[count] - 1;
        if (dataSize < 0 || buffer.remaining() < dataSize) {
            throw new IllegalArgumentException("CFF INDEX data is truncated");
        }
        byte[] data = new byte[dataSize];
        buffer.get(data);
        byte[][] objects = new byte[count][];
        for (int index = 0; index < count; index++) {
            int start = offsets[index] - 1;
            int end = offsets[index + 1] - 1;
            if (start < 0 || end < start || end > data.length) {
                throw new IllegalArgumentException("CFF INDEX object range is invalid");
            }
            byte[] object = new byte[end - start];
            System.arraycopy(data, start, object, 0, object.length);
            objects[index] = object;
        }
        return new Index(count, objects);
    }

    /// Reads an unsigned offset of `offSize` bytes.
    private static int readOffset(ByteBuffer buffer, int offSize) {
        int value = 0;
        for (int index = 0; index < offSize; index++) {
            value = (value << 8) | (buffer.get() & 0xFF);
        }
        return value;
    }

    /// Parses a DICT into the operators needed for outlines.
    private static DictValues readDict(ByteBuffer data) {
        ByteBuffer buffer = data.duplicate().order(ByteOrder.BIG_ENDIAN);
        float[] operands = new float[48];
        int count = 0;
        DictValues values = new DictValues();
        while (buffer.hasRemaining()) {
            int lead = buffer.get() & 0xFF;
            if (lead <= 21) {
                int operator = lead;
                if (lead == 12) {
                    if (!buffer.hasRemaining()) {
                        throw new IllegalArgumentException("CFF DICT escape is truncated");
                    }
                    operator = ESCAPE | (buffer.get() & 0xFF);
                }
                applyDict(values, operator, operands, count);
                count = 0;
                continue;
            }
            if (count == operands.length) {
                throw new IllegalArgumentException("CFF DICT operand stack overflow");
            }
            operands[count++] = readDictNumber(buffer, lead);
        }
        return values;
    }

    /// Stores one DICT operator.
    private static void applyDict(DictValues values, int operator, float[] operands, int count) {
        switch (operator) {
            case 17 -> {
                if (count < 1) {
                    throw new IllegalArgumentException("CharStrings operator needs an offset");
                }
                values.charStrings = (int) operands[0];
            }
            case 18 -> {
                if (count < 2) {
                    throw new IllegalArgumentException("Private operator needs size and offset");
                }
                values.privateSize = (int) operands[0];
                values.privateOffset = (int) operands[1];
            }
            case 19 -> {
                if (count < 1) {
                    throw new IllegalArgumentException("Subrs operator needs an offset");
                }
                values.subrs = (int) operands[0];
            }
            case ESCAPE | 6 -> {
                if (count < 1) {
                    throw new IllegalArgumentException("CharstringType needs a value");
                }
                values.charstringType = (int) operands[0];
            }
            case ESCAPE | 36 -> {
                if (count < 1) {
                    throw new IllegalArgumentException("FDArray operator needs an offset");
                }
                values.fdArray = (int) operands[0];
            }
            default -> {
            }
        }
    }

    /// Reads one DICT number whose first byte is `lead`.
    private static float readDictNumber(ByteBuffer buffer, int lead) {
        if (lead == 28) {
            if (buffer.remaining() < 2) {
                throw new IllegalArgumentException("CFF int16 is truncated");
            }
            return buffer.getShort();
        }
        if (lead == 29) {
            if (buffer.remaining() < 4) {
                throw new IllegalArgumentException("CFF int32 is truncated");
            }
            return buffer.getInt();
        }
        if (lead == 30) {
            return readReal(buffer);
        }
        if (lead >= 32 && lead <= 246) {
            return lead - 139;
        }
        if (lead >= 247 && lead <= 250) {
            if (!buffer.hasRemaining()) {
                throw new IllegalArgumentException("CFF number is truncated");
            }
            return (lead - 247) * 256 + (buffer.get() & 0xFF) + 108;
        }
        if (lead >= 251 && lead <= 254) {
            if (!buffer.hasRemaining()) {
                throw new IllegalArgumentException("CFF number is truncated");
            }
            return -(lead - 251) * 256 - (buffer.get() & 0xFF) - 108;
        }
        throw new IllegalArgumentException("Reserved CFF DICT b0 " + lead);
    }

    /// Reads a nibble-encoded real.
    private static float readReal(ByteBuffer buffer) {
        StringBuilder text = new StringBuilder();
        while (buffer.hasRemaining()) {
            int pair = buffer.get() & 0xFF;
            if (!appendNibble(text, pair >> 4) || !appendNibble(text, pair & 0x0F)) {
                break;
            }
        }
        try {
            return Float.parseFloat(text.toString());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("CFF real is malformed", exception);
        }
    }

    /// Appends one real nibble. Returns `false` at the terminator.
    private static boolean appendNibble(StringBuilder text, int nibble) {
        return switch (nibble) {
            case 0x0A -> {
                text.append('.');
                yield true;
            }
            case 0x0B -> {
                text.append('E');
                yield true;
            }
            case 0x0C -> {
                text.append("E-");
                yield true;
            }
            case 0x0E -> {
                text.append('-');
                yield true;
            }
            case 0x0F -> false;
            default -> {
                if (nibble > 9) {
                    throw new IllegalArgumentException("CFF real nibble is reserved");
                }
                text.append((char) ('0' + nibble));
                yield true;
            }
        };
    }

    /// Returns a slice of `table` that does not share the caller's position.
    private static ByteBuffer slice(ByteBuffer table, int offset, int length) {
        if (offset < 0 || length < 0 || (long) offset + (long) length > table.capacity()) {
            throw new IllegalArgumentException("CFF slice is out of range");
        }
        return table.duplicate().order(ByteOrder.BIG_ENDIAN).clear().position(offset).limit(offset + length);
    }

    /// One CFF INDEX.
    private static final class Index {
        /// Shared empty INDEX.
        private static final Index EMPTY = new Index(0, new byte[0][]);

        /// Object count.
        private final int count;

        /// Object payloads.
        private final byte[][] objects;

        /// Creates an INDEX.
        private Index(int count, byte[][] objects) {
            this.count = count;
            this.objects = objects;
        }

        /// Returns object `index`.
        private ByteBuffer object(int index) {
            return ByteBuffer.wrap(objects[index]).order(ByteOrder.BIG_ENDIAN);
        }
    }

    /// Selected DICT operators.
    private static final class DictValues {
        /// CharStrings offset, or `-1`.
        private int charStrings = -1;

        /// Private DICT size.
        private int privateSize;

        /// Private DICT offset, or `-1`.
        private int privateOffset = -1;

        /// Local Subrs offset relative to Private, or `-1`.
        private int subrs = -1;

        /// Charstring type; CFF 1 default is 2.
        private int charstringType = 2;

        /// FDArray offset, or `-1`.
        private int fdArray = -1;
    }

    /// Interprets one Type 2 program.
    private static final class Type2Decoder {
        /// Transient operand stack.
        private final float[] stack;

        /// Stack occupancy.
        private int stackSize;

        /// Global subroutines.
        private final byte[][] globalSubrs;

        /// Local subroutines.
        private final byte[][] localSubrs;

        /// Destination pen.
        private final OutlinePen pen;

        /// Whether width is not used.
        private final boolean cff2;

        /// Current x.
        private float x;

        /// Current y.
        private float y;

        /// Contour start x.
        private float startX;

        /// Contour start y.
        private float startY;

        /// Whether a contour is open.
        private boolean open;

        /// Hint stem count for mask length.
        private int stemCount;

        /// Whether the first drawing operator has been seen.
        private boolean started;

        /// Creates a decoder.
        private Type2Decoder(byte[][] globalSubrs, byte[][] localSubrs, boolean cff2, OutlinePen pen) {
            this.stack = new float[cff2 ? STACK_CFF2 : STACK_CFF1];
            this.globalSubrs = globalSubrs;
            this.localSubrs = localSubrs;
            this.cff2 = cff2;
            this.pen = pen;
        }

        /// Decodes `program`.
        private static void decode(
                byte[] program,
                byte[][] globalSubrs,
                byte[][] localSubrs,
                boolean cff2,
                OutlinePen pen
        ) {
            Type2Decoder decoder = new Type2Decoder(globalSubrs, localSubrs, cff2, pen);
            decoder.run(program, 0);
            if (decoder.open) {
                decoder.closePath();
            }
        }

        /// Interprets `program` at `depth`.
        private void run(byte[] program, int depth) {
            if (depth > MAX_SUBR_DEPTH) {
                throw new IllegalArgumentException("Type 2 subroutine depth exceeds " + MAX_SUBR_DEPTH);
            }
            int index = 0;
            while (index < program.length) {
                int lead = program[index++] & 0xFF;
                if (lead == 28 || lead == 255 || lead >= 32) {
                    push(readCharStringNumber(program, lead, index));
                    index += numberWidth(lead);
                    continue;
                }
                if (lead == 12) {
                    if (index >= program.length) {
                        throw new IllegalArgumentException("Type 2 escape is truncated");
                    }
                    int escaped = program[index++] & 0xFF;
                    index = applyEscape(escaped, program, index);
                    continue;
                }
                index = applyOperator(lead, program, index, depth);
                if (lead == 11 || lead == 14) {
                    return;
                }
            }
        }

        /// Applies a one-byte operator. Returns the next program index.
        private int applyOperator(int operator, byte[] program, int index, int depth) {
            return switch (operator) {
                case 1, 3, 18, 23 -> {
                    consumeStems();
                    yield index;
                }
                case 4 -> {
                    consumeWidth(1);
                    y += pop();
                    move();
                    yield index;
                }
                case 5 -> {
                    rlineto();
                    yield index;
                }
                case 6 -> {
                    alternatingLines(true);
                    yield index;
                }
                case 7 -> {
                    alternatingLines(false);
                    yield index;
                }
                case 8 -> {
                    rrcurveto();
                    yield index;
                }
                case 10 -> {
                    callSubr(localSubrs, depth);
                    yield index;
                }
                case 11 -> index;
                case 14 -> {
                    consumeWidth(0);
                    closePath();
                    yield index;
                }
                case 15 -> {
                    clear();
                    yield index;
                }
                case 16 -> {
                    blend();
                    yield index;
                }
                case 19, 20 -> {
                    consumeStems();
                    int maskBytes = (stemCount + 7) / 8;
                    if (index + maskBytes > program.length) {
                        throw new IllegalArgumentException("Type 2 hint mask is truncated");
                    }
                    yield index + maskBytes;
                }
                case 21 -> {
                    consumeWidth(2);
                    y += pop();
                    x += pop();
                    move();
                    yield index;
                }
                case 22 -> {
                    consumeWidth(1);
                    x += pop();
                    move();
                    yield index;
                }
                case 24 -> {
                    rcurveline();
                    yield index;
                }
                case 25 -> {
                    rlinecurve();
                    yield index;
                }
                case 26 -> {
                    vvcurveto();
                    yield index;
                }
                case 27 -> {
                    hhcurveto();
                    yield index;
                }
                case 29 -> {
                    callSubr(globalSubrs, depth);
                    yield index;
                }
                case 30 -> {
                    vhcurveto();
                    yield index;
                }
                case 31 -> {
                    hvcurveto();
                    yield index;
                }
                default -> throw new IllegalArgumentException("Reserved Type 2 operator " + operator);
            };
        }

        /// Applies a `12` escape operator.
        private int applyEscape(int escaped, byte[] program, int index) {
            return switch (escaped) {
                case 34 -> {
                    hflex();
                    yield index;
                }
                case 35 -> {
                    flex();
                    yield index;
                }
                case 36 -> {
                    hflex1();
                    yield index;
                }
                case 37 -> {
                    flex1();
                    yield index;
                }
                default -> {
                    clear();
                    yield index;
                }
            };
        }

        /// `callsubr` / `callgsubr` helper.
        private void callSubr(byte[][] subrs, int depth) {
            if (stackSize < 1) {
                throw new IllegalArgumentException("Type 2 subroutine call is missing an index");
            }
            int biased = (int) pop() + subrBias(subrs.length);
            if (biased < 0 || biased >= subrs.length) {
                throw new IllegalArgumentException("Type 2 subroutine index is out of range");
            }
            run(subrs[biased], depth + 1);
        }

        /// Subroutine number bias.
        private static int subrBias(int count) {
            if (count < 1240) {
                return 107;
            }
            if (count < 33900) {
                return 1131;
            }
            return 32768;
        }

        /// Treats leftover stem operands as hints.
        private void consumeStems() {
            if (!started && !cff2 && (stackSize & 1) == 1) {
                shift();
            }
            stemCount += stackSize / 2;
            clear();
            started = true;
        }

        /// Drops an optional CFF 1 width when `needed` numbers remain.
        private void consumeWidth(int needed) {
            if (!started && !cff2 && stackSize == needed + 1) {
                shift();
            }
            started = true;
        }

        /// `{dxa dya}+`
        private void rlineto() {
            if ((stackSize & 1) != 0) {
                throw new IllegalArgumentException("rlineto needs coordinate pairs");
            }
            int index = 0;
            while (index < stackSize) {
                x += stack[index++];
                y += stack[index++];
                line();
            }
            clear();
        }

        /// Alternating axis-aligned lines starting on `horizontal`.
        private void alternatingLines(boolean horizontal) {
            int index = 0;
            boolean axis = horizontal;
            while (index < stackSize) {
                if (axis) {
                    x += stack[index++];
                } else {
                    y += stack[index++];
                }
                line();
                axis = !axis;
            }
            clear();
        }

        /// `{dxa dya dxb dyb dxc dyc}+`
        private void rrcurveto() {
            if (stackSize % 6 != 0) {
                throw new IllegalArgumentException("rrcurveto needs 6 numbers per curve");
            }
            int index = 0;
            while (index < stackSize) {
                curve(
                        stack[index],
                        stack[index + 1],
                        stack[index + 2],
                        stack[index + 3],
                        stack[index + 4],
                        stack[index + 5]
                );
                index += 6;
            }
            clear();
        }

        /// Curves followed by one line.
        private void rcurveline() {
            if (stackSize < 8 || (stackSize - 2) % 6 != 0) {
                throw new IllegalArgumentException("rcurveline stack is invalid");
            }
            int index = 0;
            while (index + 2 < stackSize) {
                curve(
                        stack[index],
                        stack[index + 1],
                        stack[index + 2],
                        stack[index + 3],
                        stack[index + 4],
                        stack[index + 5]
                );
                index += 6;
            }
            x += stack[index];
            y += stack[index + 1];
            line();
            clear();
        }

        /// Lines followed by one curve.
        private void rlinecurve() {
            if (stackSize < 8 || (stackSize - 6) % 2 != 0) {
                throw new IllegalArgumentException("rlinecurve stack is invalid");
            }
            int index = 0;
            while (index + 6 < stackSize) {
                x += stack[index++];
                y += stack[index++];
                line();
            }
            curve(
                    stack[index],
                    stack[index + 1],
                    stack[index + 2],
                    stack[index + 3],
                    stack[index + 4],
                    stack[index + 5]
            );
            clear();
        }

        /// `dx1? {dya dxb dyb dyc}+`
        private void vvcurveto() {
            int index = 0;
            if ((stackSize & 1) == 1) {
                x += stack[index++];
            }
            if ((stackSize - index) % 4 != 0) {
                throw new IllegalArgumentException("vvcurveto stack is invalid");
            }
            while (index < stackSize) {
                curve(0.0f, stack[index], stack[index + 1], stack[index + 2], 0.0f, stack[index + 3]);
                index += 4;
            }
            clear();
        }

        /// `dy1? {dxa dxb dyb dxc}+`
        private void hhcurveto() {
            int index = 0;
            if ((stackSize & 1) == 1) {
                y += stack[index++];
            }
            if ((stackSize - index) % 4 != 0) {
                throw new IllegalArgumentException("hhcurveto stack is invalid");
            }
            while (index < stackSize) {
                curve(stack[index], 0.0f, stack[index + 1], stack[index + 2], stack[index + 3], 0.0f);
                index += 4;
            }
            clear();
        }

        /// Alternating vertical-horizontal cubics.
        private void vhcurveto() {
            hvOrVh(false);
        }

        /// Alternating horizontal-vertical cubics.
        private void hvcurveto() {
            hvOrVh(true);
        }

        /// Shared `hvcurveto` / `vhcurveto`.
        private void hvOrVh(boolean horizontalFirst) {
            boolean horizontal = horizontalFirst;
            int index = 0;
            while (stackSize - index >= 4) {
                int remaining = stackSize - index;
                boolean last = remaining == 5 || remaining == 4;
                float extra = last && remaining == 5 ? stack[stackSize - 1] : 0.0f;
                if (horizontal) {
                    curve(
                            stack[index],
                            0.0f,
                            stack[index + 1],
                            stack[index + 2],
                            last ? extra : 0.0f,
                            stack[index + 3]
                    );
                } else {
                    curve(
                            0.0f,
                            stack[index],
                            stack[index + 1],
                            stack[index + 2],
                            stack[index + 3],
                            last ? extra : 0.0f
                    );
                }
                index += 4;
                horizontal = !horizontal;
                if (last && remaining == 5) {
                    break;
                }
            }
            clear();
        }

        /// Two cubics with 13 numbers including flex depth.
        private void flex() {
            require(13);
            curve(stack[0], stack[1], stack[2], stack[3], stack[4], stack[5]);
            curve(stack[6], stack[7], stack[8], stack[9], stack[10], stack[11]);
            clear();
        }

        /// Horizontal flex with 7 numbers.
        private void hflex() {
            require(7);
            curve(stack[0], 0.0f, stack[1], stack[2], stack[3], 0.0f);
            curve(stack[4], 0.0f, stack[5], 0.0f, stack[6], 0.0f);
            clear();
        }

        /// `hflex1` with 9 numbers.
        private void hflex1() {
            require(9);
            curve(stack[0], stack[1], stack[2], stack[3], stack[4], 0.0f);
            curve(stack[5], 0.0f, stack[6], stack[7], stack[8], 0.0f);
            clear();
        }

        /// `flex1` with 11 numbers.
        private void flex1() {
            require(11);
            float dx = stack[0] + stack[2] + stack[4] + stack[6] + stack[8];
            float dy = stack[1] + stack[3] + stack[5] + stack[7] + stack[9];
            float last = stack[10];
            float dx6 = Math.abs(dx) > Math.abs(dy) ? last : 0.0f;
            float dy6 = Math.abs(dx) > Math.abs(dy) ? 0.0f : last;
            curve(stack[0], stack[1], stack[2], stack[3], stack[4], stack[5]);
            curve(stack[6], stack[7], stack[8], stack[9], dx6, dy6);
            clear();
        }

        /// CFF2 blend with zero variation regions leaves the defaults.
        private void blend() {
            if (stackSize < 1) {
                throw new IllegalArgumentException("blend is missing n");
            }
            int n = (int) pop();
            if (n < 0 || n > stackSize) {
                throw new IllegalArgumentException("blend n exceeds the stack");
            }
        }

        /// Emits a relative cubic.
        private void curve(float dx1, float dy1, float dx2, float dy2, float dx3, float dy3) {
            float c1x = x + dx1;
            float c1y = y + dy1;
            float c2x = c1x + dx2;
            float c2y = c1y + dy2;
            x = c2x + dx3;
            y = c2y + dy3;
            if (!open) {
                move();
            }
            pen.cubicTo(c1x, c1y, c2x, c2y, x, y);
        }

        /// Starts a contour at the current point.
        private void move() {
            if (open) {
                closePath();
            }
            startX = x;
            startY = y;
            open = true;
            pen.moveTo(x, y);
        }

        /// Emits a line to the current point.
        private void line() {
            if (!open) {
                move();
            }
            pen.lineTo(x, y);
        }

        /// Closes the open contour.
        private void closePath() {
            if (open) {
                pen.close();
                open = false;
            }
        }

        /// Pushes one number.
        private void push(float value) {
            if (stackSize == stack.length) {
                throw new IllegalArgumentException("Type 2 stack overflow");
            }
            stack[stackSize++] = value;
        }

        /// Pops the top number.
        private float pop() {
            if (stackSize == 0) {
                throw new IllegalArgumentException("Type 2 stack underflow");
            }
            return stack[--stackSize];
        }

        /// Drops the bottom number (width).
        private void shift() {
            if (stackSize == 0) {
                return;
            }
            System.arraycopy(stack, 1, stack, 0, stackSize - 1);
            stackSize--;
        }

        /// Clears the stack.
        private void clear() {
            stackSize = 0;
        }

        /// Requires `count` stack numbers.
        private void require(int count) {
            if (stackSize < count) {
                throw new IllegalArgumentException("Type 2 stack is short");
            }
        }

        /// Reads a Type 2 number that starts at `index` after `lead` has been consumed.
        private static float readCharStringNumber(byte[] program, int lead, int index) {
            if (lead == 28) {
                if (index + 1 >= program.length) {
                    throw new IllegalArgumentException("Type 2 int16 is truncated");
                }
                return (short) ((program[index] << 8) | (program[index + 1] & 0xFF));
            }
            if (lead == 255) {
                if (index + 3 >= program.length) {
                    throw new IllegalArgumentException("Type 2 fixed is truncated");
                }
                int raw = (program[index] << 24)
                        | ((program[index + 1] & 0xFF) << 16)
                        | ((program[index + 2] & 0xFF) << 8)
                        | (program[index + 3] & 0xFF);
                return raw / 65536.0f;
            }
            if (lead >= 32 && lead <= 246) {
                return lead - 139;
            }
            if (lead >= 247 && lead <= 250) {
                if (index >= program.length) {
                    throw new IllegalArgumentException("Type 2 number is truncated");
                }
                return (lead - 247) * 256 + (program[index] & 0xFF) + 108;
            }
            if (lead >= 251 && lead <= 254) {
                if (index >= program.length) {
                    throw new IllegalArgumentException("Type 2 number is truncated");
                }
                return -(lead - 251) * 256 - (program[index] & 0xFF) - 108;
            }
            throw new IllegalArgumentException("Reserved Type 2 b0 " + lead);
        }

        /// Returns extra bytes after `lead` for a Type 2 number.
        private static int numberWidth(int lead) {
            if (lead == 28) {
                return 2;
            }
            if (lead == 255) {
                return 4;
            }
            if (lead >= 247 && lead <= 254) {
                return 1;
            }
            return 0;
        }
    }
}

package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;

import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;

/// Generates an `OTTO` font whose CFF 1 CharString is a Type 2 rectangle.
///
/// Glyph 1 is `A`. The path is `(0,0) → (5,0) → (5,7) → (0,7)` and `endchar` closes it.
@NotNullByDefault
public final class CffSampleFont {
    /// Units per em.
    public static final int UNITS_PER_EM = 8;

    /// `A` glyph.
    public static final int GLYPH_A = 1;

    /// Rectangle width in font units.
    public static final int RECT_WIDTH = 5;

    /// Rectangle height in font units.
    public static final int RECT_HEIGHT = 7;

    /// Glyph count including `.notdef`.
    private static final int GLYPH_COUNT = 2;

    /// Prevents instantiation.
    private CffSampleFont() {
    }

    /// Builds the CFF 1 sample font.
    ///
    /// @return the parsed font
    public static SfntFont create() {
        return new SfntFont(bytes());
    }

    /// Builds the CFF 1 sample font image.
    ///
    /// @return a read-only OTTO file
    public static MemorySegment bytes() {
        LinkedHashMap<String, byte[]> tables = new LinkedHashMap<>();
        tables.put("CFF ", cff());
        tables.put("cmap", cmap());
        tables.put("head", head());
        tables.put("hhea", hhea());
        tables.put("hmtx", hmtx());
        tables.put("maxp", maxp());
        tables.put("name", name("HimariCFF"));
        tables.put("post", post());
        return MemorySegment.ofArray(BitmapSfntFont.wrapOtto(tables)).asReadOnly();
    }

    /// Writes a name-keyed CFF 1 table.
    private static byte[] cff() {
        byte[] name = index1("C".getBytes(StandardCharsets.US_ASCII));
        byte[] strings = emptyIndex1();
        byte[] globalSubrs = emptyIndex1();
        byte[] charset = new byte[] {0, 0, 1};
        byte[] charstrings = index1(new byte[] {0x0E}, rectangle());
        byte[] priv = new byte[0];
        int topDictGuess = 32;
        int header = 4;
        int charsetOffset = header + name.length + (5 + topDictGuess) + strings.length + globalSubrs.length;
        int charStringsOffset = charsetOffset + charset.length;
        int privateOffset = charStringsOffset + charstrings.length;
        byte[] topDict = topDict(charsetOffset, charStringsOffset, priv.length, privateOffset);
        if (topDict.length != topDictGuess) {
            charsetOffset = header + name.length + (5 + topDict.length) + strings.length + globalSubrs.length;
            charStringsOffset = charsetOffset + charset.length;
            privateOffset = charStringsOffset + charstrings.length;
            topDict = topDict(charsetOffset, charStringsOffset, priv.length, privateOffset);
        }
        ByteBuffer table = ByteBuffer.allocate(
                header + name.length + 5 + topDict.length + strings.length + globalSubrs.length
                        + charset.length + charstrings.length + priv.length
        );
        table.put((byte) 1);
        table.put((byte) 0);
        table.put((byte) 4);
        table.put((byte) 1);
        table.put(name);
        table.put(index1(topDict));
        table.put(strings);
        table.put(globalSubrs);
        table.put(charset);
        table.put(charstrings);
        table.put(priv);
        return table.array();
    }

    /// Writes the Top DICT with absolute offsets.
    private static byte[] topDict(int charset, int charStrings, int privateSize, int privateOffset) {
        ByteBuffer buffer = ByteBuffer.allocate(40);
        putCffInt(buffer, 0);
        putCffInt(buffer, 0);
        putCffInt(buffer, RECT_WIDTH);
        putCffInt(buffer, RECT_HEIGHT);
        buffer.put((byte) 5);
        putCffInt32(buffer, charset);
        buffer.put((byte) 15);
        putCffInt32(buffer, charStrings);
        buffer.put((byte) 17);
        putCffInt32(buffer, privateSize);
        putCffInt32(buffer, privateOffset);
        buffer.put((byte) 18);
        return slice(buffer);
    }

    /// Writes a Type 2 rectangle closed by `endchar`.
    static byte[] rectangle() {
        ByteBuffer buffer = ByteBuffer.allocate(16);
        putCffInt(buffer, 0);
        putCffInt(buffer, 0);
        buffer.put((byte) 21);
        putCffInt(buffer, RECT_WIDTH);
        putCffInt(buffer, 0);
        putCffInt(buffer, 0);
        putCffInt(buffer, RECT_HEIGHT);
        putCffInt(buffer, -RECT_WIDTH);
        putCffInt(buffer, 0);
        buffer.put((byte) 5);
        buffer.put((byte) 14);
        return slice(buffer);
    }

    /// Writes a Type 2 cubic from `(0,0)` to `(20,0)` with controls `(0,10)` and `(10,10)`.
    static byte[] cubic() {
        ByteBuffer buffer = ByteBuffer.allocate(16);
        putCffInt(buffer, 0);
        putCffInt(buffer, 0);
        buffer.put((byte) 21);
        putCffInt(buffer, 0);
        putCffInt(buffer, 10);
        putCffInt(buffer, 10);
        putCffInt(buffer, 0);
        putCffInt(buffer, 10);
        putCffInt(buffer, -10);
        buffer.put((byte) 8);
        buffer.put((byte) 14);
        return slice(buffer);
    }

    /// Writes a CFF 1 INDEX of one-byte offsets.
    static byte[] index1(byte[]... objects) {
        int dataLength = 0;
        for (int index = 0; index < objects.length; index++) {
            dataLength += objects[index].length;
        }
        if (dataLength + 1 > 255) {
            throw new IllegalStateException("CFF sample INDEX exceeds one-byte offsets");
        }
        ByteBuffer buffer = ByteBuffer.allocate(3 + objects.length + 1 + dataLength).order(ByteOrder.BIG_ENDIAN);
        buffer.putShort((short) objects.length);
        buffer.put((byte) 1);
        int offset = 1;
        buffer.put((byte) offset);
        for (int index = 0; index < objects.length; index++) {
            offset += objects[index].length;
            buffer.put((byte) offset);
        }
        for (int index = 0; index < objects.length; index++) {
            buffer.put(objects[index]);
        }
        return buffer.array();
    }

    /// Writes an empty CFF 1 INDEX.
    static byte[] emptyIndex1() {
        return new byte[] {0, 0};
    }

    /// Writes an empty CFF2 INDEX.
    static byte[] emptyIndex2() {
        return new byte[] {0, 0, 0, 0};
    }

    /// Writes a CFF2 INDEX of one-byte offsets.
    static byte[] index2(byte[]... objects) {
        int dataLength = 0;
        for (int index = 0; index < objects.length; index++) {
            dataLength += objects[index].length;
        }
        if (dataLength + 1 > 255) {
            throw new IllegalStateException("CFF2 sample INDEX exceeds one-byte offsets");
        }
        ByteBuffer buffer = ByteBuffer.allocate(5 + objects.length + 1 + dataLength).order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(objects.length);
        buffer.put((byte) 1);
        int offset = 1;
        buffer.put((byte) offset);
        for (int index = 0; index < objects.length; index++) {
            offset += objects[index].length;
            buffer.put((byte) offset);
        }
        for (int index = 0; index < objects.length; index++) {
            buffer.put(objects[index]);
        }
        return buffer.array();
    }

    /// Encodes a CFF integer in the one- or three-byte form.
    static void putCffInt(ByteBuffer buffer, int value) {
        if (value >= -107 && value <= 107) {
            buffer.put((byte) (value + 139));
            return;
        }
        if (value >= 108 && value <= 1131) {
            int packed = value - 108;
            buffer.put((byte) ((packed >> 8) + 247));
            buffer.put((byte) packed);
            return;
        }
        if (value >= -1131 && value <= -108) {
            int packed = -value - 108;
            buffer.put((byte) ((packed >> 8) + 251));
            buffer.put((byte) packed);
            return;
        }
        buffer.put((byte) 28);
        buffer.put((byte) (value >> 8));
        buffer.put((byte) value);
    }

    /// Encodes a CFF integer as a five-byte `29` value.
    static void putCffInt32(ByteBuffer buffer, int value) {
        buffer.put((byte) 29);
        buffer.putInt(value);
    }

    /// Copies the written prefix of `buffer`.
    static byte[] slice(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.position()];
        buffer.rewind();
        buffer.get(bytes);
        return bytes;
    }

    /// Writes a format-4 cmap for `A`.
    static byte[] cmap() {
        ByteBuffer buffer = ByteBuffer.allocate(64).order(ByteOrder.BIG_ENDIAN);
        buffer.putShort((short) 0);
        buffer.putShort((short) 1);
        buffer.putShort((short) 3);
        buffer.putShort((short) 1);
        buffer.putInt(12);
        int format4 = buffer.position();
        buffer.putShort((short) 4);
        int lengthPos = buffer.position();
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort((short) 4);
        buffer.putShort((short) 4);
        buffer.putShort((short) 1);
        buffer.putShort((short) 0);
        buffer.putShort((short) 'A');
        buffer.putShort((short) 0xFFFF);
        buffer.putShort((short) 0);
        buffer.putShort((short) 'A');
        buffer.putShort((short) 0xFFFF);
        buffer.putShort((short) (GLYPH_A - 'A'));
        buffer.putShort((short) 1);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort(lengthPos, (short) (buffer.position() - format4));
        return slice(buffer);
    }

    /// Writes the head table.
    static byte[] head() {
        ByteBuffer buffer = ByteBuffer.allocate(54).order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(0x00010000);
        buffer.putInt(0x00010000);
        buffer.putInt(0);
        buffer.putInt(0x5F0F3CF5);
        buffer.putShort((short) 0);
        buffer.putShort((short) UNITS_PER_EM);
        buffer.putLong(0);
        buffer.putLong(0);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort((short) RECT_WIDTH);
        buffer.putShort((short) RECT_HEIGHT);
        buffer.putShort((short) 0);
        buffer.putShort((short) 8);
        buffer.putShort((short) 0);
        buffer.putShort((short) 1);
        buffer.putShort((short) 0);
        return buffer.array();
    }

    /// Writes the hhea table.
    static byte[] hhea() {
        ByteBuffer buffer = ByteBuffer.allocate(36).order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(0x00010000);
        buffer.putShort((short) RECT_HEIGHT);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort((short) 8);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort((short) GLYPH_COUNT);
        return buffer.array();
    }

    /// Writes the hmtx table.
    static byte[] hmtx() {
        ByteBuffer buffer = ByteBuffer.allocate(GLYPH_COUNT * 4).order(ByteOrder.BIG_ENDIAN);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort((short) 8);
        buffer.putShort((short) 0);
        return buffer.array();
    }

    /// Writes a version-0.5 maxp table.
    static byte[] maxp() {
        ByteBuffer buffer = ByteBuffer.allocate(6).order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(0x00005000);
        buffer.putShort((short) GLYPH_COUNT);
        return buffer.array();
    }

    /// Writes a name table with one family name.
    static byte[] name(String familyName) {
        byte[] family = familyName.getBytes(StandardCharsets.US_ASCII);
        ByteBuffer buffer = ByteBuffer.allocate(18 + family.length).order(ByteOrder.BIG_ENDIAN);
        buffer.putShort((short) 0);
        buffer.putShort((short) 1);
        buffer.putShort((short) 18);
        buffer.putShort((short) 3);
        buffer.putShort((short) 1);
        buffer.putShort((short) 0x0409);
        buffer.putShort((short) 1);
        buffer.putShort((short) family.length);
        buffer.putShort((short) 0);
        buffer.put(family);
        return buffer.array();
    }

    /// Writes a dummy post table.
    static byte[] post() {
        ByteBuffer buffer = ByteBuffer.allocate(32).order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(0x00030000);
        return buffer.array();
    }
}

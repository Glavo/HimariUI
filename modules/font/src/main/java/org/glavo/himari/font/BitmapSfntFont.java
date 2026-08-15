package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;

import java.io.ByteArrayOutputStream;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Generates a minimal checked SFNT bitmap-outline font for the first-stable Latin sample set.
@NotNullByDefault
public final class BitmapSfntFont {
    /// First printable code point stored in the font.
    private static final int FIRST = 32;

    /// Last printable code point stored in the font.
    private static final int LAST = 126;

    /// Units per em.
    public static final int UNITS_PER_EM = 8;

    /// Prevents instantiation.
    private BitmapSfntFont() {
    }

    /// Builds the bundled sample font.
    ///
    /// @return the parsed font
    public static SfntFont create() {
        return new SfntFont(bytes());
    }

    /// Builds the bundled sample font image.
    ///
    /// @return a read-only SFNT file
    public static MemorySegment bytes() {
        LinkedHashMap<String, byte[]> tables = new LinkedHashMap<>();
        tables.put("cmap", cmap());
        byte[] glyf = glyf();
        tables.put("glyf", glyf);
        tables.put("head", head());
        tables.put("hhea", hhea());
        tables.put("hmtx", hmtx());
        tables.put("loca", loca(glyf.length));
        tables.put("maxp", maxp());
        tables.put("name", name());
        tables.put("post", post());
        return MemorySegment.ofArray(wrap(tables)).asReadOnly();
    }

    /// Glyph count including `.notdef`.
    ///
    /// @return the count
    static int glyphCount() {
        return LAST - FIRST + 2;
    }

    /// Writes the format-4 cmap.
    ///
    /// @return the table
    private static byte[] cmap() {
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
        buffer.putShort((short) LAST);
        buffer.putShort((short) 0xFFFF);
        buffer.putShort((short) 0);
        buffer.putShort((short) FIRST);
        buffer.putShort((short) 0xFFFF);
        buffer.putShort((short) (1 - FIRST));
        buffer.putShort((short) 1);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort(lengthPos, (short) (buffer.position() - format4));
        return slice(buffer);
    }

    /// Writes glyf data: empty `.notdef`/space and a 5x7 rectangle for every other glyph.
    ///
    /// @return the table
    private static byte[] glyf() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        for (int glyph = 2; glyph < glyphCount(); glyph++) {
            writeSimpleRect(output, 0, 0, 5, 7);
        }
        return output.toByteArray();
    }

    /// Writes long `loca` offsets matching [glyf()].
    ///
    /// @param glyfLength the glyf table length
    /// @return the table
    private static byte[] loca(int glyfLength) {
        int glyphBytes = glyfLength / Math.max(1, glyphCount() - 2);
        ByteBuffer loca = ByteBuffer.allocate((glyphCount() + 1) * 4).order(ByteOrder.BIG_ENDIAN);
        loca.putInt(0);
        loca.putInt(0);
        loca.putInt(0);
        int running = 0;
        for (int glyph = 2; glyph < glyphCount(); glyph++) {
            running += glyphBytes;
            loca.putInt(running);
        }
        return loca.array();
    }

    /// Writes a simple on-curve rectangle glyph.
    ///
    /// @param output the destination
    /// @param x the min x
    /// @param y the min y
    /// @param width the width
    /// @param height the height
    private static void writeSimpleRect(ByteArrayOutputStream output, int x, int y, int width, int height) {
        writeShorts(
                output,
                (short) 1,
                (short) x,
                (short) y,
                (short) (x + width),
                (short) (y + height),
                (short) 3,
                (short) 0
        );
        output.write(0x01);
        output.write(0x01);
        output.write(0x01);
        output.write(0x01);
        writeShorts(output, (short) x, (short) width, (short) 0, (short) (-width));
        writeShorts(output, (short) y, (short) 0, (short) height, (short) 0);
    }

    /// Writes the head table.
    ///
    /// @return the table
    private static byte[] head() {
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
        buffer.putShort((short) 5);
        buffer.putShort((short) 7);
        buffer.putShort((short) 0);
        buffer.putShort((short) 8);
        buffer.putShort((short) 0);
        buffer.putShort((short) 1);
        buffer.putShort((short) 0);
        return buffer.array();
    }

    /// Writes the hhea table.
    ///
    /// @return the table
    private static byte[] hhea() {
        ByteBuffer buffer = ByteBuffer.allocate(36).order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(0x00010000);
        buffer.putShort((short) 7);
        buffer.putShort((short) -1);
        buffer.putShort((short) 0);
        buffer.putShort((short) 6);
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
        buffer.putShort((short) glyphCount());
        return buffer.array();
    }

    /// Writes the hmtx table.
    ///
    /// @return the table
    private static byte[] hmtx() {
        ByteBuffer buffer = ByteBuffer.allocate(glyphCount() * 4).order(ByteOrder.BIG_ENDIAN);
        for (int glyph = 0; glyph < glyphCount(); glyph++) {
            buffer.putShort((short) (glyph <= 1 ? 3 : 6));
            buffer.putShort((short) 0);
        }
        return buffer.array();
    }

    /// Writes the maxp table.
    ///
    /// @return the table
    private static byte[] maxp() {
        ByteBuffer buffer = ByteBuffer.allocate(32).order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(0x00010000);
        buffer.putShort((short) glyphCount());
        buffer.putShort((short) 4);
        buffer.putShort((short) 1);
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
        return buffer.array();
    }

    /// Writes a name table with one family name.
    ///
    /// @return the table
    private static byte[] name() {
        byte[] family = "HimariSample".getBytes(StandardCharsets.US_ASCII);
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
    ///
    /// @return the table
    private static byte[] post() {
        ByteBuffer buffer = ByteBuffer.allocate(32).order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(0x00030000);
        return buffer.array();
    }

    /// Wraps tables in an SFNT container and fixes the `head` checksum.
    ///
    /// @param tables the tables
    /// @return the file
    private static byte[] wrap(Map<String, byte[]> tables) {
        List<Map.Entry<String, byte[]>> ordered = new ArrayList<>(tables.entrySet());
        ordered.sort(Map.Entry.comparingByKey());
        int header = 12 + ordered.size() * 16;
        int offset = header;
        ArrayList<Integer> offsets = new ArrayList<>();
        int fileSize = header;
        for (Map.Entry<String, byte[]> entry : ordered) {
            offsets.add(offset);
            int padded = (entry.getValue().length + 3) & ~3;
            offset += padded;
            fileSize += padded;
        }
        ByteBuffer file = ByteBuffer.allocate(fileSize).order(ByteOrder.BIG_ENDIAN);
        file.putInt(0x00010000);
        file.putShort((short) ordered.size());
        int search = Integer.highestOneBit(ordered.size()) * 16;
        file.putShort((short) search);
        file.putShort((short) Integer.numberOfTrailingZeros(Integer.highestOneBit(ordered.size())));
        file.putShort((short) (ordered.size() * 16 - search));
        for (int index = 0; index < ordered.size(); index++) {
            Map.Entry<String, byte[]> entry = ordered.get(index);
            file.put(entry.getKey().getBytes(StandardCharsets.US_ASCII));
            file.putInt(checksum(entry.getValue()));
            file.putInt(offsets.get(index));
            file.putInt(entry.getValue().length);
        }
        for (int index = 0; index < ordered.size(); index++) {
            file.position(offsets.get(index));
            byte[] payload = ordered.get(index).getValue();
            file.put(payload);
            int padded = (payload.length + 3) & ~3;
            for (int pad = payload.length; pad < padded; pad++) {
                file.put((byte) 0);
            }
        }
        int sum = checksum(file.array());
        int adjustment = (int) (0xB1B0AFBAL - Integer.toUnsignedLong(sum));
        int headOffset = offsets.get(indexOf(ordered, "head"));
        file.putInt(headOffset + 8, adjustment);
        return file.array();
    }

    /// Returns the table index.
    ///
    /// @param ordered the tables
    /// @param tag the tag
    /// @return the index
    private static int indexOf(List<Map.Entry<String, byte[]>> ordered, String tag) {
        for (int index = 0; index < ordered.size(); index++) {
            if (ordered.get(index).getKey().equals(tag)) {
                return index;
            }
        }
        throw new IllegalStateException("Missing table " + tag);
    }

    /// Computes an SFNT checksum.
    ///
    /// @param bytes the bytes
    /// @return the checksum
    private static int checksum(byte[] bytes) {
        int sum = 0;
        for (int index = 0; index < bytes.length; index += 4) {
            int value = 0;
            for (int part = 0; part < 4; part++) {
                value <<= 8;
                if (index + part < bytes.length) {
                    value |= bytes[index + part] & 0xFF;
                }
            }
            sum += value;
        }
        return sum;
    }

    /// Writes big-endian shorts.
    ///
    /// @param output the destination
    /// @param values the values
    private static void writeShorts(ByteArrayOutputStream output, short... values) {
        for (short value : values) {
            output.write((value >>> 8) & 0xFF);
            output.write(value & 0xFF);
        }
    }

    /// Copies the written prefix of a buffer.
    ///
    /// @param buffer the buffer
    /// @return the bytes
    private static byte[] slice(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.position()];
        buffer.rewind();
        buffer.get(bytes);
        return bytes;
    }
}

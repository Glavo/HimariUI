package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;

import java.io.ByteArrayOutputStream;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;

/// Generates a TrueType face whose `A` is a COLR v0 base with two painted layers.
///
/// Glyph 2 is the empty base. Glyph 3 is the red back rectangle. Glyph 4 is the blue front
/// rectangle. Palette `0` stores those two CPAL colors.
@NotNullByDefault
public final class ColrSampleFont {
    /// Units per em.
    public static final int UNITS_PER_EM = 8;

    /// Base glyph mapped from `A`.
    public static final int GLYPH_BASE = 2;

    /// Back-layer glyph.
    public static final int GLYPH_BACK = 3;

    /// Front-layer glyph.
    public static final int GLYPH_FRONT = 4;

    /// Glyph count including `.notdef` and space.
    private static final int GLYPH_COUNT = 5;

    /// Prevents instantiation.
    private ColrSampleFont() {
    }

    /// Builds the COLR sample font.
    ///
    /// @return the parsed font
    public static SfntFont create() {
        return new SfntFont(bytes());
    }

    /// Builds the COLR sample font image.
    ///
    /// @return a read-only SFNT file
    public static MemorySegment bytes() {
        LinkedHashMap<String, byte[]> tables = new LinkedHashMap<>();
        tables.put("cmap", cmap());
        byte[] glyf = glyf();
        tables.put("glyf", glyf);
        tables.put("COLR", colr());
        tables.put("CPAL", cpal());
        tables.put("head", head());
        tables.put("hhea", hhea());
        tables.put("hmtx", hmtx());
        tables.put("loca", loca(glyf.length));
        tables.put("maxp", maxp());
        tables.put("name", name());
        tables.put("post", post());
        return MemorySegment.ofArray(BitmapSfntFont.wrap(tables)).asReadOnly();
    }

    /// Writes a format-4 cmap for space and `A`.
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
        buffer.putShort((short) 6);
        buffer.putShort((short) 4);
        buffer.putShort((short) 1);
        buffer.putShort((short) 2);
        buffer.putShort((short) 32);
        buffer.putShort((short) 'A');
        buffer.putShort((short) 0xFFFF);
        buffer.putShort((short) 0);
        buffer.putShort((short) 32);
        buffer.putShort((short) 'A');
        buffer.putShort((short) 0xFFFF);
        buffer.putShort((short) (1 - 32));
        buffer.putShort((short) (GLYPH_BASE - 'A'));
        buffer.putShort((short) 1);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort(lengthPos, (short) (buffer.position() - format4));
        return slice(buffer);
    }

    /// Writes empty `.notdef`/space/base plus two rectangle layers.
    private static byte[] glyf() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        BitmapSfntFont.writeSimpleRect(output, 0, 0, 5, 7);
        BitmapSfntFont.writeSimpleRect(output, 1, 2, 3, 3);
        return output.toByteArray();
    }

    /// Writes long `loca` offsets. Glyphs 0–2 are empty.
    private static byte[] loca(int glyfLength) {
        int first = glyfLength / 2;
        ByteBuffer loca = ByteBuffer.allocate((GLYPH_COUNT + 1) * 4).order(ByteOrder.BIG_ENDIAN);
        loca.putInt(0);
        loca.putInt(0);
        loca.putInt(0);
        loca.putInt(0);
        loca.putInt(first);
        loca.putInt(glyfLength);
        return loca.array();
    }

    /// Writes COLR v0 with one base and two layers.
    private static byte[] colr() {
        ByteBuffer buffer = ByteBuffer.allocate(28).order(ByteOrder.BIG_ENDIAN);
        buffer.putShort((short) 0);
        buffer.putShort((short) 1);
        buffer.putInt(14);
        buffer.putInt(20);
        buffer.putShort((short) 2);
        buffer.putShort((short) GLYPH_BASE);
        buffer.putShort((short) 0);
        buffer.putShort((short) 2);
        buffer.putShort((short) GLYPH_BACK);
        buffer.putShort((short) 0);
        buffer.putShort((short) GLYPH_FRONT);
        buffer.putShort((short) 1);
        return buffer.array();
    }

    /// Writes one palette of opaque red then opaque blue.
    private static byte[] cpal() {
        ByteBuffer buffer = ByteBuffer.allocate(22).order(ByteOrder.BIG_ENDIAN);
        buffer.putShort((short) 0);
        buffer.putShort((short) 2);
        buffer.putShort((short) 1);
        buffer.putShort((short) 2);
        buffer.putInt(14);
        buffer.putShort((short) 0);
        buffer.put((byte) 0);
        buffer.put((byte) 0);
        buffer.put((byte) 255);
        buffer.put((byte) 255);
        buffer.put((byte) 255);
        buffer.put((byte) 0);
        buffer.put((byte) 0);
        buffer.put((byte) 255);
        return buffer.array();
    }

    /// Writes the head table.
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
    private static byte[] hhea() {
        ByteBuffer buffer = ByteBuffer.allocate(36).order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(0x00010000);
        buffer.putShort((short) 7);
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
    private static byte[] hmtx() {
        ByteBuffer buffer = ByteBuffer.allocate(GLYPH_COUNT * 4).order(ByteOrder.BIG_ENDIAN);
        for (int index = 0; index < GLYPH_COUNT; index++) {
            buffer.putShort((short) (index < 2 ? 0 : 8));
            buffer.putShort((short) 0);
        }
        return buffer.array();
    }

    /// Writes the maxp table.
    private static byte[] maxp() {
        ByteBuffer buffer = ByteBuffer.allocate(32).order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(0x00010000);
        buffer.putShort((short) GLYPH_COUNT);
        buffer.putShort((short) 4);
        buffer.putShort((short) 1);
        return buffer.array();
    }

    /// Writes a name table with one family name.
    private static byte[] name() {
        byte[] family = "HimariColr".getBytes(StandardCharsets.US_ASCII);
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
    private static byte[] post() {
        ByteBuffer buffer = ByteBuffer.allocate(32).order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(0x00030000);
        return buffer.array();
    }

    /// Copies the written prefix of `buffer`.
    private static byte[] slice(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.position()];
        buffer.rewind();
        buffer.get(bytes);
        return bytes;
    }
}

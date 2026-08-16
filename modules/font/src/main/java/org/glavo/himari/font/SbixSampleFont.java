package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;

import java.io.ByteArrayOutputStream;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;

/// Generates a TrueType face with one `sbix` strike for `A`.
///
/// The strike is 8 ppem. Glyph 1 carries graphic type `png ` and four payload bytes.
@NotNullByDefault
public final class SbixSampleFont {
    /// `png ` graphic tag.
    public static final int TAG_PNG = 0x706E6720;

    /// Payload stored after the `sbix` header.
    public static final byte @org.jetbrains.annotations.Unmodifiable [] PAYLOAD = {1, 2, 3, 4};

    /// Origin y stored on the glyph.
    public static final int ORIGIN_Y = 1;

    /// Strike ppem.
    public static final int PPEM = 8;

    /// Glyph of `A`.
    public static final int GLYPH_A = 1;

    /// Glyph count including `.notdef`.
    private static final int GLYPH_COUNT = 2;

    /// Prevents instantiation.
    private SbixSampleFont() {
    }

    /// Builds the sbix sample font.
    ///
    /// @return the parsed font
    public static SfntFont create() {
        return new SfntFont(bytes());
    }

    /// Builds the sbix sample font image.
    ///
    /// @return a read-only SFNT file
    public static MemorySegment bytes() {
        LinkedHashMap<String, byte[]> tables = new LinkedHashMap<>();
        tables.put("cmap", cmap());
        byte[] glyf = glyf();
        tables.put("glyf", glyf);
        tables.put("sbix", sbix());
        tables.put("head", head());
        tables.put("hhea", hhea());
        tables.put("hmtx", hmtx());
        tables.put("loca", loca(glyf.length));
        tables.put("maxp", maxp());
        tables.put("name", name());
        tables.put("post", post());
        return MemorySegment.ofArray(BitmapSfntFont.wrap(tables)).asReadOnly();
    }

    /// Writes a format-4 cmap for `A`.
    private static byte[] cmap() {
        ByteBuffer buffer = ByteBuffer.allocate(48).order(ByteOrder.BIG_ENDIAN);
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

    /// Writes one rectangle for `A`.
    private static byte[] glyf() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        BitmapSfntFont.writeSimpleRect(output, 0, 0, 5, 7);
        return output.toByteArray();
    }

    /// Writes long `loca` offsets.
    private static byte[] loca(int glyfLength) {
        ByteBuffer loca = ByteBuffer.allocate((GLYPH_COUNT + 1) * 4).order(ByteOrder.BIG_ENDIAN);
        loca.putInt(0);
        loca.putInt(0);
        loca.putInt(glyfLength);
        return loca.array();
    }

    /// Writes one 8 ppem strike with payload on glyph 1.
    private static byte[] sbix() {
        int strike = 12;
        int header = 4;
        int offsets = (GLYPH_COUNT + 1) * 4;
        int glyphStart = header + offsets;
        int glyphBytes = 8 + PAYLOAD.length;
        ByteBuffer buffer = ByteBuffer.allocate(strike + glyphStart + glyphBytes).order(ByteOrder.BIG_ENDIAN);
        buffer.putShort((short) 1);
        buffer.putShort((short) 1);
        buffer.putInt(1);
        buffer.putInt(strike);
        buffer.putShort((short) PPEM);
        buffer.putShort((short) 72);
        buffer.putInt(glyphStart);
        buffer.putInt(glyphStart);
        buffer.putInt(glyphStart + glyphBytes);
        buffer.putShort((short) 0);
        buffer.putShort((short) ORIGIN_Y);
        buffer.putInt(TAG_PNG);
        buffer.put(PAYLOAD);
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
        buffer.putShort((short) 8);
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
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort((short) 8);
        buffer.putShort((short) 0);
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
        byte[] family = "HimariSbix".getBytes(StandardCharsets.US_ASCII);
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

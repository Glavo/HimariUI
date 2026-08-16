package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;

import java.io.ByteArrayOutputStream;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;

/// Generates a TrueType face with one `wght` axis and a one-tuple `gvar` for `A`.
///
/// The rectangle for `A` has four on-curve points. At peak `1.0` (weight `900`) each contour
/// point moves `+20` on X and the advance-width phantom moves `+10` on X. The default instance
/// applies no deltas.
@NotNullByDefault
public final class GvarSampleFont {
    /// `wght` tag.
    public static final int TAG_WGHT = 0x77676874;

    /// Default weight.
    public static final float DEFAULT_WEIGHT = 400.0f;

    /// Minimum weight.
    public static final float MIN_WEIGHT = 100.0f;

    /// Maximum weight.
    public static final float MAX_WEIGHT = 900.0f;

    /// Contour X delta at the peak tuple.
    public static final int CONTOUR_X_DELTA = 20;

    /// Advance-width phantom X delta at the peak tuple.
    public static final int ADVANCE_PHANTOM_DELTA = 10;

    /// Default `hmtx` advance of `A`.
    public static final int DEFAULT_ADVANCE = 8;

    /// Glyph of `A`.
    public static final int GLYPH_A = 1;

    /// Glyph count including `.notdef`.
    private static final int GLYPH_COUNT = 2;

    /// Prevents instantiation.
    private GvarSampleFont() {
    }

    /// Builds the variable sample font.
    ///
    /// @return the parsed font
    public static SfntFont create() {
        return new SfntFont(bytes());
    }

    /// Builds the variable sample font image.
    ///
    /// @return a read-only SFNT file
    public static MemorySegment bytes() {
        LinkedHashMap<String, byte[]> tables = new LinkedHashMap<>();
        tables.put("cmap", cmap());
        byte[] glyf = glyf();
        tables.put("glyf", glyf);
        tables.put("fvar", fvar());
        tables.put("gvar", gvar());
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

    /// Writes one `wght` axis.
    private static byte[] fvar() {
        ByteBuffer buffer = ByteBuffer.allocate(36).order(ByteOrder.BIG_ENDIAN);
        buffer.putShort((short) 1);
        buffer.putShort((short) 0);
        buffer.putShort((short) 16);
        buffer.putShort((short) 2);
        buffer.putShort((short) 1);
        buffer.putShort((short) 20);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putInt(TAG_WGHT);
        buffer.putInt(toFixed(MIN_WEIGHT));
        buffer.putInt(toFixed(DEFAULT_WEIGHT));
        buffer.putInt(toFixed(MAX_WEIGHT));
        buffer.putShort((short) 0);
        buffer.putShort((short) 256);
        return buffer.array();
    }

    /// Writes one embedded-peak tuple that moves `A` and its advance phantom.
    private static byte[] gvar() {
        byte[] packed = packedDeltas();
        int headerAndPeak = 10;
        int glyphData = headerAndPeak + packed.length;
        int offsetArray = (GLYPH_COUNT + 1) * 4;
        int dataArrayOffset = 20 + offsetArray;
        ByteBuffer buffer = ByteBuffer.allocate(dataArrayOffset + glyphData).order(ByteOrder.BIG_ENDIAN);
        buffer.putShort((short) 1);
        buffer.putShort((short) 0);
        buffer.putShort((short) 1);
        buffer.putShort((short) 0);
        buffer.putInt(0);
        buffer.putShort((short) GLYPH_COUNT);
        buffer.putShort((short) 1);
        buffer.putInt(dataArrayOffset);
        buffer.putInt(0);
        buffer.putInt(0);
        buffer.putInt(glyphData);
        buffer.putShort((short) 1);
        buffer.putShort((short) headerAndPeak);
        buffer.putShort((short) packed.length);
        buffer.putShort((short) 0x8000);
        buffer.putShort((short) 0x4000);
        buffer.put(packed);
        return buffer.array();
    }

    /// Packs X then Y deltas for four contour points and four phantoms.
    private static byte[] packedDeltas() {
        ByteBuffer buffer = ByteBuffer.allocate(16).order(ByteOrder.BIG_ENDIAN);
        buffer.put((byte) 3);
        buffer.put((byte) CONTOUR_X_DELTA);
        buffer.put((byte) CONTOUR_X_DELTA);
        buffer.put((byte) CONTOUR_X_DELTA);
        buffer.put((byte) CONTOUR_X_DELTA);
        buffer.put((byte) 3);
        buffer.put((byte) 0);
        buffer.put((byte) ADVANCE_PHANTOM_DELTA);
        buffer.put((byte) 0);
        buffer.put((byte) 0);
        buffer.put((byte) (0x80 | 7));
        return slice(buffer);
    }

    /// Encodes a 16.16 fixed value.
    private static int toFixed(float value) {
        return Math.round(value * 65536.0f);
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
        buffer.putShort((short) DEFAULT_ADVANCE);
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
        byte[] family = "HimariGvar".getBytes(StandardCharsets.US_ASCII);
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

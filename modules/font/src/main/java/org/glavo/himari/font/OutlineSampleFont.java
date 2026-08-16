package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;

import java.io.ByteArrayOutputStream;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;

/// Generates a checked SFNT with a quadratic bump, an implied-on-curve pair, and a composite.
///
/// Glyph 2 (`A`) is the closed quadratic `(0,0)` on, `(50,100)` off, `(100,0)` on. The curve peaks
/// at `(50,50)`, so `(50,70)` lies inside the control triangle and outside the filled curve. Glyph 3
/// (`B`) is that bump translated by `(20,10)`. Glyph 4 (`C`) inserts an implied on-curve midpoint
/// between two off-curve points.
@NotNullByDefault
public final class OutlineSampleFont {
    /// Units per em.
    public static final int UNITS_PER_EM = 100;

    /// Glyph identity of the quadratic bump (`A`).
    public static final int GLYPH_BUMP = 2;

    /// Glyph identity of the translated composite (`B`).
    public static final int GLYPH_COMPOSITE = 3;

    /// Glyph identity of the implied-on-curve contour (`C`).
    public static final int GLYPH_IMPLIED = 4;

    /// Composite translation x in font units.
    public static final int COMPOSITE_DX = 20;

    /// Composite translation y in font units.
    public static final int COMPOSITE_DY = 10;

    /// Glyph count including `.notdef` and space.
    private static final int GLYPH_COUNT = 5;

    /// Component arguments are 16-bit XY offsets.
    private static final int COMPOSITE_XY_WORDS = 0x0003;

    /// Prevents instantiation.
    private OutlineSampleFont() {
    }

    /// Builds the outline sample font.
    ///
    /// @return the parsed font
    public static SfntFont create() {
        return new SfntFont(bytes());
    }

    /// Builds the outline sample font image.
    ///
    /// @return a read-only SFNT file
    public static MemorySegment bytes() {
        byte[] bump = quadraticBump();
        byte[] composite = composite();
        byte[] implied = impliedOnCurve();
        LinkedHashMap<String, byte[]> tables = new LinkedHashMap<>();
        tables.put("cmap", cmap());
        tables.put("glyf", concat(bump, composite, implied));
        tables.put("head", head());
        tables.put("hhea", hhea());
        tables.put("hmtx", hmtx());
        tables.put("loca", loca(bump.length, composite.length, implied.length));
        tables.put("maxp", maxp());
        tables.put("name", name());
        tables.put("post", post());
        return MemorySegment.ofArray(BitmapSfntFont.wrap(tables)).asReadOnly();
    }

    /// Writes a format-4 cmap for space, `A`, `B`, and `C`.
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
        buffer.putShort((short) 67);
        buffer.putShort((short) 0xFFFF);
        buffer.putShort((short) 0);
        buffer.putShort((short) 32);
        buffer.putShort((short) 65);
        buffer.putShort((short) 0xFFFF);
        buffer.putShort((short) (1 - 32));
        buffer.putShort((short) (GLYPH_BUMP - 65));
        buffer.putShort((short) 1);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort(lengthPos, (short) (buffer.position() - format4));
        return slice(buffer);
    }

    /// Writes the quadratic bump glyph.
    private static byte[] quadraticBump() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        BitmapSfntFont.writeShorts(
                output,
                (short) 1,
                (short) 0,
                (short) 0,
                (short) 100,
                (short) 100,
                (short) 2,
                (short) 0
        );
        output.write(0x01);
        output.write(0x00);
        output.write(0x01);
        BitmapSfntFont.writeShorts(output, (short) 0, (short) 50, (short) 50);
        BitmapSfntFont.writeShorts(output, (short) 0, (short) 100, (short) -100);
        return output.toByteArray();
    }

    /// Writes the translated composite of [`#GLYPH_BUMP`].
    private static byte[] composite() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        BitmapSfntFont.writeShorts(
                output,
                (short) -1,
                (short) COMPOSITE_DX,
                (short) COMPOSITE_DY,
                (short) (100 + COMPOSITE_DX),
                (short) (100 + COMPOSITE_DY),
                (short) COMPOSITE_XY_WORDS,
                (short) GLYPH_BUMP,
                (short) COMPOSITE_DX,
                (short) COMPOSITE_DY
        );
        return output.toByteArray();
    }

    /// Writes `(0,0)` on, `(20,80)` off, `(80,80)` off, `(100,0)` on.
    private static byte[] impliedOnCurve() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        BitmapSfntFont.writeShorts(
                output,
                (short) 1,
                (short) 0,
                (short) 0,
                (short) 100,
                (short) 80,
                (short) 3,
                (short) 0
        );
        output.write(0x01);
        output.write(0x00);
        output.write(0x00);
        output.write(0x01);
        BitmapSfntFont.writeShorts(output, (short) 0, (short) 20, (short) 60, (short) 20);
        BitmapSfntFont.writeShorts(output, (short) 0, (short) 80, (short) 0, (short) -80);
        return output.toByteArray();
    }

    /// Writes long `loca` offsets.
    private static byte[] loca(int bumpLength, int compositeLength, int impliedLength) {
        ByteBuffer loca = ByteBuffer.allocate((GLYPH_COUNT + 1) * 4).order(ByteOrder.BIG_ENDIAN);
        loca.putInt(0);
        loca.putInt(0);
        loca.putInt(0);
        loca.putInt(bumpLength);
        loca.putInt(bumpLength + compositeLength);
        loca.putInt(bumpLength + compositeLength + impliedLength);
        return loca.array();
    }

    /// Concatenates glyph payloads.
    private static byte[] concat(byte[]... parts) {
        int length = 0;
        for (byte[] part : parts) {
            length += part.length;
        }
        byte[] joined = new byte[length];
        int offset = 0;
        for (byte[] part : parts) {
            System.arraycopy(part, 0, joined, offset, part.length);
            offset += part.length;
        }
        return joined;
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
        buffer.putShort((short) (100 + COMPOSITE_DX));
        buffer.putShort((short) (100 + COMPOSITE_DY));
        buffer.putShort((short) 0);
        buffer.putShort((short) 16);
        buffer.putShort((short) 0);
        buffer.putShort((short) 1);
        buffer.putShort((short) 0);
        return buffer.array();
    }

    /// Writes the hhea table.
    private static byte[] hhea() {
        ByteBuffer buffer = ByteBuffer.allocate(36).order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(0x00010000);
        buffer.putShort((short) 100);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort((short) (100 + COMPOSITE_DX));
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
        int[] advances = {0, 50, 100, 120, 100};
        for (int advance : advances) {
            buffer.putShort((short) advance);
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
        buffer.putShort((short) 3);
        buffer.putShort((short) 1);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort((short) 1);
        buffer.putShort((short) 1);
        return buffer.array();
    }

    /// Writes a name table with one family name.
    private static byte[] name() {
        byte[] family = "HimariOutline".getBytes(StandardCharsets.US_ASCII);
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

    /// Copies the written prefix of a buffer.
    private static byte[] slice(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.position()];
        buffer.rewind();
        buffer.get(bytes);
        return bytes;
    }
}

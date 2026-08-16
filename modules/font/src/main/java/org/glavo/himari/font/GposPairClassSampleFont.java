package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;

import java.io.ByteArrayOutputStream;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;

/// Generates a face whose GPOS type-2 format-2 class matrix tightens `A` before `C`.
///
/// `A` is class 1 on the first ClassDef and `C` is class 1 on the second. `B` is class 2 on the
/// second ClassDef and has a zero cell, so `AB` is unchanged.
@NotNullByDefault
public final class GposPairClassSampleFont {
    /// Isolated letter advance.
    public static final int ADVANCE_LETTER = 10;

    /// Class-pair X-advance applied to `A` before `C`.
    public static final int PAIR_DELTA = -4;

    /// Glyph of `A`.
    public static final int GLYPH_A = 2;

    /// Glyph of `B`.
    public static final int GLYPH_B = 3;

    /// Glyph of `C`.
    public static final int GLYPH_C = 4;

    /// Glyph count including `.notdef` and space.
    private static final int GLYPH_COUNT = 5;

    /// Prevents instantiation.
    private GposPairClassSampleFont() {
    }

    /// Builds the sample font.
    ///
    /// @return the parsed font
    public static SfntFont create() {
        return new SfntFont(bytes());
    }

    /// Builds the font image.
    ///
    /// @return a read-only SFNT file
    public static MemorySegment bytes() {
        LinkedHashMap<String, byte[]> tables = new LinkedHashMap<>();
        tables.put("cmap", cmap());
        byte[] glyf = glyf();
        tables.put("glyf", glyf);
        tables.put("GPOS", gpos());
        tables.put("head", head());
        tables.put("hhea", hhea());
        tables.put("hmtx", hmtx());
        tables.put("loca", loca(glyf.length));
        tables.put("maxp", maxp());
        tables.put("name", name());
        tables.put("post", post());
        return MemorySegment.ofArray(BitmapSfntFont.wrap(tables)).asReadOnly();
    }

    /// Writes a format-4 cmap for space, `A`, `B`, and `C`.
    private static byte[] cmap() {
        ByteBuffer buffer = ByteBuffer.allocate(96).order(ByteOrder.BIG_ENDIAN);
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
        buffer.putShort((short) 10);
        buffer.putShort((short) 8);
        buffer.putShort((short) 2);
        buffer.putShort((short) 0);
        buffer.putShort((short) 32);
        buffer.putShort((short) 'A');
        buffer.putShort((short) 'B');
        buffer.putShort((short) 'C');
        buffer.putShort((short) 0xFFFF);
        buffer.putShort((short) 0);
        buffer.putShort((short) 32);
        buffer.putShort((short) 'A');
        buffer.putShort((short) 'B');
        buffer.putShort((short) 'C');
        buffer.putShort((short) 0xFFFF);
        buffer.putShort((short) (1 - 32));
        buffer.putShort((short) (GLYPH_A - 'A'));
        buffer.putShort((short) (GLYPH_B - 'B'));
        buffer.putShort((short) (GLYPH_C - 'C'));
        buffer.putShort((short) 1);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort(lengthPos, (short) (buffer.position() - format4));
        return slice(buffer);
    }

    /// Writes three rectangles.
    private static byte[] glyf() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        BitmapSfntFont.writeSimpleRect(output, 0, 0, 5, 7);
        BitmapSfntFont.writeSimpleRect(output, 0, 0, 5, 7);
        BitmapSfntFont.writeSimpleRect(output, 0, 0, 5, 7);
        return output.toByteArray();
    }

    /// Writes long `loca` offsets.
    private static byte[] loca(int glyfLength) {
        int step = glyfLength / 3;
        ByteBuffer loca = ByteBuffer.allocate((GLYPH_COUNT + 1) * 4).order(ByteOrder.BIG_ENDIAN);
        loca.putInt(0);
        loca.putInt(0);
        loca.putInt(0);
        loca.putInt(step);
        loca.putInt(step * 2);
        loca.putInt(glyfLength);
        return loca.array();
    }

    /// Writes a type-2 format-2 `kern` lookup.
    private static byte[] gpos() {
        byte[] pair = pairPosFormat2();
        ByteBuffer lookup0 = ByteBuffer.allocate(8 + pair.length).order(ByteOrder.BIG_ENDIAN);
        lookup0.putShort((short) 2);
        lookup0.putShort((short) 0);
        lookup0.putShort((short) 1);
        lookup0.putShort((short) 8);
        lookup0.put(pair);
        byte[] first = lookup0.array();
        ByteBuffer lookupList = ByteBuffer.allocate(4 + first.length).order(ByteOrder.BIG_ENDIAN);
        lookupList.putShort((short) 1);
        lookupList.putShort((short) 4);
        lookupList.put(first);
        ByteBuffer feature = ByteBuffer.allocate(6).order(ByteOrder.BIG_ENDIAN);
        feature.putShort((short) 0);
        feature.putShort((short) 1);
        feature.putShort((short) 0);
        byte[] featureList = featureList(0x6B65726E, feature.array());
        byte[] scriptList = defaultScript();
        byte[] lookups = lookupList.array();
        int header = 10;
        ByteBuffer table = ByteBuffer.allocate(header + scriptList.length + featureList.length + lookups.length)
                .order(ByteOrder.BIG_ENDIAN);
        table.putShort((short) 1);
        table.putShort((short) 0);
        table.putShort((short) header);
        table.putShort((short) (header + scriptList.length));
        table.putShort((short) (header + scriptList.length + featureList.length));
        table.put(scriptList);
        table.put(featureList);
        table.put(lookups);
        return table.array();
    }

    /// Writes PairPos format 2 with a 2×3 class matrix.
    private static byte[] pairPosFormat2() {
        ByteBuffer buffer = ByteBuffer.allocate(60).order(ByteOrder.BIG_ENDIAN);
        buffer.putShort((short) 2);
        buffer.putShort((short) 28);
        buffer.putShort((short) 0x0004);
        buffer.putShort((short) 0);
        buffer.putShort((short) 34);
        buffer.putShort((short) 44);
        buffer.putShort((short) 2);
        buffer.putShort((short) 3);
        for (int class1 = 0; class1 < 2; class1++) {
            for (int class2 = 0; class2 < 3; class2++) {
                short delta = class1 == 1 && class2 == 1 ? (short) PAIR_DELTA : 0;
                buffer.putShort(delta);
            }
        }
        buffer.putShort((short) 1);
        buffer.putShort((short) 1);
        buffer.putShort((short) GLYPH_A);
        buffer.putShort((short) 2);
        buffer.putShort((short) 1);
        buffer.putShort((short) GLYPH_A);
        buffer.putShort((short) GLYPH_A);
        buffer.putShort((short) 1);
        buffer.putShort((short) 2);
        buffer.putShort((short) 2);
        buffer.putShort((short) GLYPH_C);
        buffer.putShort((short) GLYPH_C);
        buffer.putShort((short) 1);
        buffer.putShort((short) GLYPH_B);
        buffer.putShort((short) GLYPH_B);
        buffer.putShort((short) 2);
        return buffer.array();
    }

    /// Writes a one-feature feature list.
    private static byte[] featureList(int tag, byte[] feature) {
        ByteBuffer buffer = ByteBuffer.allocate(8 + feature.length).order(ByteOrder.BIG_ENDIAN);
        buffer.putShort((short) 1);
        buffer.putInt(tag);
        buffer.putShort((short) 8);
        buffer.put(feature);
        return buffer.array();
    }

    /// Writes a `DFLT` script enabling feature 0.
    private static byte[] defaultScript() {
        ByteBuffer langSys = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN);
        langSys.putShort((short) 0);
        langSys.putShort((short) 0xFFFF);
        langSys.putShort((short) 1);
        langSys.putShort((short) 0);
        byte[] lang = langSys.array();
        ByteBuffer script = ByteBuffer.allocate(4 + lang.length).order(ByteOrder.BIG_ENDIAN);
        script.putShort((short) 4);
        script.putShort((short) 0);
        script.put(lang);
        byte[] scriptBytes = script.array();
        ByteBuffer buffer = ByteBuffer.allocate(8 + scriptBytes.length).order(ByteOrder.BIG_ENDIAN);
        buffer.putShort((short) 1);
        buffer.put("DFLT".getBytes(StandardCharsets.US_ASCII));
        buffer.putShort((short) 8);
        buffer.put(scriptBytes);
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
        buffer.putShort((short) ADVANCE_LETTER);
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
        int[] advances = {0, 3, ADVANCE_LETTER, ADVANCE_LETTER, ADVANCE_LETTER};
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
        return buffer.array();
    }

    /// Writes a name table.
    private static byte[] name() {
        byte[] family = "HimariPairClass".getBytes(StandardCharsets.US_ASCII);
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

package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;

import java.io.ByteArrayOutputStream;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;

/// Generates a face whose GDEF MarkGlyphSet 0 covers `B` and whose lookups skip other marks.
///
/// `UseMarkFilteringSet` 0 keeps `B` and skips `D`. Pair and `calt` rules therefore match `ADC`
/// and leave `ABC` unchanged.
@NotNullByDefault
public final class GdefMarkSetSampleFont {
    /// Isolated letter advance.
    public static final int ADVANCE_LETTER = 10;

    /// Type-2 X-advance applied to `A` before `C` when `D` is skipped.
    public static final int PAIR_DELTA = 6;

    /// Substitute advance.
    public static final int ADVANCE_X = 14;

    /// Glyph of `A`.
    public static final int GLYPH_A = 2;

    /// Mark glyph of `B`, covered by set 0.
    public static final int GLYPH_B = 3;

    /// Glyph of `C`.
    public static final int GLYPH_C = 4;

    /// Mark glyph of `D`, absent from set 0.
    public static final int GLYPH_D = 5;

    /// Substitute glyph of `X`.
    public static final int GLYPH_X = 6;

    /// Glyph count including `.notdef` and space.
    private static final int GLYPH_COUNT = 7;

    /// Prevents instantiation.
    private GdefMarkSetSampleFont() {
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
        tables.put("GDEF", gdef());
        byte[] glyf = glyf();
        tables.put("glyf", glyf);
        tables.put("GPOS", gpos());
        tables.put("GSUB", gsub());
        tables.put("head", head());
        tables.put("hhea", hhea());
        tables.put("hmtx", hmtx());
        tables.put("loca", loca(glyf.length));
        tables.put("maxp", maxp());
        tables.put("name", name());
        tables.put("post", post());
        return MemorySegment.ofArray(BitmapSfntFont.wrap(tables)).asReadOnly();
    }

    /// Writes a format-4 cmap for space, `A`, `B`, `C`, `D`, and `X`.
    private static byte[] cmap() {
        ByteBuffer buffer = ByteBuffer.allocate(128).order(ByteOrder.BIG_ENDIAN);
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
        buffer.putShort((short) 14);
        buffer.putShort((short) 8);
        buffer.putShort((short) 2);
        buffer.putShort((short) 0);
        buffer.putShort((short) 32);
        buffer.putShort((short) 'A');
        buffer.putShort((short) 'B');
        buffer.putShort((short) 'C');
        buffer.putShort((short) 'D');
        buffer.putShort((short) 'X');
        buffer.putShort((short) 0xFFFF);
        buffer.putShort((short) 0);
        buffer.putShort((short) 32);
        buffer.putShort((short) 'A');
        buffer.putShort((short) 'B');
        buffer.putShort((short) 'C');
        buffer.putShort((short) 'D');
        buffer.putShort((short) 'X');
        buffer.putShort((short) 0xFFFF);
        buffer.putShort((short) (1 - 32));
        buffer.putShort((short) (GLYPH_A - 'A'));
        buffer.putShort((short) (GLYPH_B - 'B'));
        buffer.putShort((short) (GLYPH_C - 'C'));
        buffer.putShort((short) (GLYPH_D - 'D'));
        buffer.putShort((short) (GLYPH_X - 'X'));
        buffer.putShort((short) 1);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort(lengthPos, (short) (buffer.position() - format4));
        return slice(buffer);
    }

    /// Writes five rectangles.
    private static byte[] glyf() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        BitmapSfntFont.writeSimpleRect(output, 0, 0, 5, 7);
        BitmapSfntFont.writeSimpleRect(output, 0, 0, 2, 3);
        BitmapSfntFont.writeSimpleRect(output, 0, 0, 5, 7);
        BitmapSfntFont.writeSimpleRect(output, 0, 0, 2, 3);
        BitmapSfntFont.writeSimpleRect(output, 0, 0, 8, 7);
        return output.toByteArray();
    }

    /// Writes long `loca` offsets.
    private static byte[] loca(int glyfLength) {
        int step = glyfLength / 5;
        ByteBuffer loca = ByteBuffer.allocate((GLYPH_COUNT + 1) * 4).order(ByteOrder.BIG_ENDIAN);
        loca.putInt(0);
        loca.putInt(0);
        loca.putInt(0);
        loca.putInt(step);
        loca.putInt(step * 2);
        loca.putInt(step * 3);
        loca.putInt(step * 4);
        loca.putInt(glyfLength);
        return loca.array();
    }

    /// Writes GDEF 1.2 with marks `B`/`D` and MarkGlyphSet 0 covering `B`.
    private static byte[] gdef() {
        ByteBuffer buffer = ByteBuffer.allocate(44).order(ByteOrder.BIG_ENDIAN);
        buffer.putShort((short) 1);
        buffer.putShort((short) 2);
        buffer.putShort((short) 14);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort((short) 30);
        buffer.putShort((short) 2);
        buffer.putShort((short) 2);
        buffer.putShort((short) GLYPH_B);
        buffer.putShort((short) GLYPH_B);
        buffer.putShort((short) GdefTable.CLASS_MARK);
        buffer.putShort((short) GLYPH_D);
        buffer.putShort((short) GLYPH_D);
        buffer.putShort((short) GdefTable.CLASS_MARK);
        buffer.putShort((short) 1);
        buffer.putShort((short) 1);
        buffer.putInt(8);
        buffer.putShort((short) 1);
        buffer.putShort((short) 1);
        buffer.putShort((short) GLYPH_B);
        return buffer.array();
    }

    /// Writes a type-2 `UseMarkFilteringSet` 0 pair for `AC`.
    private static byte[] gpos() {
        byte[] pair = pairPos();
        ByteBuffer lookup0 = ByteBuffer.allocate(10 + pair.length).order(ByteOrder.BIG_ENDIAN);
        lookup0.putShort((short) 2);
        lookup0.putShort((short) GdefTable.FLAG_MARK_FILTER);
        lookup0.putShort((short) 1);
        lookup0.putShort((short) 10);
        lookup0.putShort((short) 0);
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
        byte[] scriptList = defaultScript(1);
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

    /// Writes type-5 `UseMarkFilteringSet` 0 for `AC`.
    private static byte[] gsub() {
        byte[] nested = singleSubst();
        ByteBuffer lookup1 = ByteBuffer.allocate(8 + nested.length).order(ByteOrder.BIG_ENDIAN);
        lookup1.putShort((short) 1);
        lookup1.putShort((short) 0);
        lookup1.putShort((short) 1);
        lookup1.putShort((short) 8);
        lookup1.put(nested);
        byte[] context = contextSubst();
        ByteBuffer lookup0 = ByteBuffer.allocate(10 + context.length).order(ByteOrder.BIG_ENDIAN);
        lookup0.putShort((short) 5);
        lookup0.putShort((short) GdefTable.FLAG_MARK_FILTER);
        lookup0.putShort((short) 1);
        lookup0.putShort((short) 10);
        lookup0.putShort((short) 0);
        lookup0.put(context);
        byte[] first = lookup0.array();
        byte[] second = lookup1.array();
        ByteBuffer lookupList = ByteBuffer.allocate(6 + first.length + second.length).order(ByteOrder.BIG_ENDIAN);
        lookupList.putShort((short) 2);
        lookupList.putShort((short) 6);
        lookupList.putShort((short) (6 + first.length));
        lookupList.put(first);
        lookupList.put(second);
        ByteBuffer feature = ByteBuffer.allocate(6).order(ByteOrder.BIG_ENDIAN);
        feature.putShort((short) 0);
        feature.putShort((short) 1);
        feature.putShort((short) 0);
        byte[] featureList = featureList(SfntFont.TAG_CALT, feature.array());
        byte[] scriptList = defaultScript(1);
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

    /// Writes PairPos format 1 for `AC`.
    private static byte[] pairPos() {
        ByteBuffer buffer = ByteBuffer.allocate(24).order(ByteOrder.BIG_ENDIAN);
        buffer.putShort((short) 1);
        buffer.putShort((short) 18);
        buffer.putShort((short) 0x0004);
        buffer.putShort((short) 0);
        buffer.putShort((short) 1);
        buffer.putShort((short) 12);
        buffer.putShort((short) 1);
        buffer.putShort((short) GLYPH_C);
        buffer.putShort((short) PAIR_DELTA);
        buffer.putShort((short) 1);
        buffer.putShort((short) 1);
        buffer.putShort((short) GLYPH_A);
        return buffer.array();
    }

    /// Writes ContextSubst format 1 for `AC`.
    private static byte[] contextSubst() {
        ByteBuffer buffer = ByteBuffer.allocate(28).order(ByteOrder.BIG_ENDIAN);
        buffer.putShort((short) 1);
        buffer.putShort((short) 22);
        buffer.putShort((short) 1);
        buffer.putShort((short) 8);
        buffer.putShort((short) 1);
        buffer.putShort((short) 4);
        buffer.putShort((short) 2);
        buffer.putShort((short) 1);
        buffer.putShort((short) GLYPH_C);
        buffer.putShort((short) 0);
        buffer.putShort((short) 1);
        buffer.putShort((short) 1);
        buffer.putShort((short) 1);
        buffer.putShort((short) GLYPH_A);
        return buffer.array();
    }

    /// Writes SingleSubst format 2 `A` to `X`.
    private static byte[] singleSubst() {
        ByteBuffer buffer = ByteBuffer.allocate(16).order(ByteOrder.BIG_ENDIAN);
        buffer.putShort((short) 2);
        buffer.putShort((short) 8);
        buffer.putShort((short) 1);
        buffer.putShort((short) GLYPH_X);
        buffer.putShort((short) 1);
        buffer.putShort((short) 1);
        buffer.putShort((short) GLYPH_A);
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

    /// Writes a `DFLT` script enabling `featureCount` features.
    private static byte[] defaultScript(int featureCount) {
        ByteBuffer langSys = ByteBuffer.allocate(6 + 2 * featureCount).order(ByteOrder.BIG_ENDIAN);
        langSys.putShort((short) 0);
        langSys.putShort((short) 0xFFFF);
        langSys.putShort((short) featureCount);
        for (int index = 0; index < featureCount; index++) {
            langSys.putShort((short) index);
        }
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
        buffer.putShort((short) ADVANCE_X);
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
        int[] advances = {0, 3, ADVANCE_LETTER, 4, ADVANCE_LETTER, 4, ADVANCE_X};
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
        byte[] family = "HimariMarkSet".getBytes(StandardCharsets.US_ASCII);
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

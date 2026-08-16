package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;

import java.io.ByteArrayOutputStream;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;

/// Generates a checked SFNT that maps Beh only through GSUB `isol`/`init`/`medi`/`fina`.
///
/// The font has no Presentation Forms-B `cmap` entries. Glyph 2 is nominal `U+0628`. Glyphs 3–6
/// are the four joining forms, each with a distinct advance so substitution is observable through
/// [`SfntFont#substitute(int, int)`] and the default shaper.
@NotNullByDefault
public final class GsubSampleFont {
    /// Units per em.
    public static final int UNITS_PER_EM = 8;

    /// Nominal Beh glyph.
    public static final int GLYPH_BEH = 2;

    /// Isolated form glyph.
    public static final int GLYPH_ISOL = 3;

    /// Initial form glyph.
    public static final int GLYPH_INIT = 4;

    /// Medial form glyph.
    public static final int GLYPH_MEDI = 5;

    /// Final form glyph.
    public static final int GLYPH_FINA = 6;

    /// Isolated advance in font units.
    public static final int ADVANCE_ISOL = 11;

    /// Initial advance in font units.
    public static final int ADVANCE_INIT = 12;

    /// Medial advance in font units.
    public static final int ADVANCE_MEDI = 13;

    /// Final advance in font units.
    public static final int ADVANCE_FINA = 14;

    /// Glyph count including `.notdef` and space.
    private static final int GLYPH_COUNT = 7;

    /// Isolated feature tag.
    public static final int TAG_ISOL = 0x69736F6C;

    /// Initial feature tag.
    public static final int TAG_INIT = 0x696E6974;

    /// Medial feature tag.
    public static final int TAG_MEDI = 0x6D656469;

    /// Final feature tag.
    public static final int TAG_FINA = 0x66696E61;

    /// Prevents instantiation.
    private GsubSampleFont() {
    }

    /// Builds the GSUB sample font.
    ///
    /// @return the parsed font
    public static SfntFont create() {
        return new SfntFont(bytes());
    }

    /// Builds the GSUB sample font image.
    ///
    /// @return a read-only SFNT file
    public static MemorySegment bytes() {
        LinkedHashMap<String, byte[]> tables = new LinkedHashMap<>();
        tables.put("cmap", cmap());
        byte[] glyf = glyf();
        tables.put("glyf", glyf);
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

    /// Writes a format-4 cmap for space and `U+0628`.
    private static byte[] cmap() {
        ByteBuffer buffer = ByteBuffer.allocate(80).order(ByteOrder.BIG_ENDIAN);
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
        buffer.putShort((short) 0x0628);
        buffer.putShort((short) 0xFFFF);
        buffer.putShort((short) 0);
        buffer.putShort((short) 32);
        buffer.putShort((short) 0x0628);
        buffer.putShort((short) 0xFFFF);
        buffer.putShort((short) (1 - 32));
        buffer.putShort((short) (GLYPH_BEH - 0x0628));
        buffer.putShort((short) 1);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort(lengthPos, (short) (buffer.position() - format4));
        return slice(buffer);
    }

    /// Writes rectangle outlines for glyphs 2–6.
    private static byte[] glyf() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        for (int glyph = 2; glyph < GLYPH_COUNT; glyph++) {
            BitmapSfntFont.writeSimpleRect(output, 0, 0, 5, 7);
        }
        return output.toByteArray();
    }

    /// Writes long `loca` offsets.
    private static byte[] loca(int glyfLength) {
        int outlined = GLYPH_COUNT - 2;
        int glyphBytes = glyfLength / outlined;
        ByteBuffer loca = ByteBuffer.allocate((GLYPH_COUNT + 1) * 4).order(ByteOrder.BIG_ENDIAN);
        loca.putInt(0);
        loca.putInt(0);
        loca.putInt(0);
        int running = 0;
        for (int glyph = 2; glyph < GLYPH_COUNT; glyph++) {
            running += glyphBytes;
            loca.putInt(running);
        }
        return loca.array();
    }

    /// Writes type-1 format-2 `isol`/`init`/`medi`/`fina` substitutions for Beh.
    private static byte[] gsub() {
        byte[] isol = lookupType1(singleSubst(GLYPH_BEH, GLYPH_ISOL));
        byte[] init = lookupType1(singleSubst(GLYPH_BEH, GLYPH_INIT));
        byte[] medi = lookupType1(singleSubst(GLYPH_BEH, GLYPH_MEDI));
        byte[] fina = lookupType1(singleSubst(GLYPH_BEH, GLYPH_FINA));
        byte[] lookupList = offsetList(isol, init, medi, fina);
        byte[] isolFeature = feature(0);
        byte[] initFeature = feature(1);
        byte[] mediFeature = feature(2);
        byte[] finaFeature = feature(3);
        byte[] featureList = featureList(
                new int[]{TAG_ISOL, TAG_INIT, TAG_MEDI, TAG_FINA},
                isolFeature,
                initFeature,
                mediFeature,
                finaFeature
        );
        byte[] scriptList = dualScript(4);
        int header = 10;
        ByteBuffer table = ByteBuffer.allocate(header + scriptList.length + featureList.length + lookupList.length)
                .order(ByteOrder.BIG_ENDIAN);
        table.putShort((short) 1);
        table.putShort((short) 0);
        table.putShort((short) header);
        table.putShort((short) (header + scriptList.length));
        table.putShort((short) (header + scriptList.length + featureList.length));
        table.put(scriptList);
        table.put(featureList);
        table.put(lookupList);
        return table.array();
    }

    /// Writes a format-2 single substitution with one coverage glyph.
    private static byte[] singleSubst(int from, int to) {
        ByteBuffer buffer = ByteBuffer.allocate(14).order(ByteOrder.BIG_ENDIAN);
        buffer.putShort((short) 2);
        buffer.putShort((short) 8);
        buffer.putShort((short) 1);
        buffer.putShort((short) to);
        buffer.putShort((short) 1);
        buffer.putShort((short) 1);
        buffer.putShort((short) from);
        return buffer.array();
    }

    /// Wraps a type-1 subtable in a lookup.
    private static byte[] lookupType1(byte[] subtable) {
        ByteBuffer buffer = ByteBuffer.allocate(8 + subtable.length).order(ByteOrder.BIG_ENDIAN);
        buffer.putShort((short) 1);
        buffer.putShort((short) 0);
        buffer.putShort((short) 1);
        buffer.putShort((short) 8);
        buffer.put(subtable);
        return buffer.array();
    }

    /// Writes a feature that applies one lookup.
    private static byte[] feature(int lookupIndex) {
        ByteBuffer buffer = ByteBuffer.allocate(6).order(ByteOrder.BIG_ENDIAN);
        buffer.putShort((short) 0);
        buffer.putShort((short) 1);
        buffer.putShort((short) lookupIndex);
        return buffer.array();
    }

    /// Writes a feature list.
    private static byte[] featureList(int[] tags, byte[]... features) {
        int records = 2 + 6 * tags.length;
        int total = records;
        int[] offsets = new int[features.length];
        for (int index = 0; index < features.length; index++) {
            offsets[index] = total;
            total += features[index].length;
        }
        ByteBuffer buffer = ByteBuffer.allocate(total).order(ByteOrder.BIG_ENDIAN);
        buffer.putShort((short) tags.length);
        for (int index = 0; index < tags.length; index++) {
            buffer.putInt(tags[index]);
            buffer.putShort((short) offsets[index]);
        }
        for (byte[] feature : features) {
            buffer.put(feature);
        }
        return buffer.array();
    }

    /// Writes a lookup list.
    private static byte[] offsetList(byte[]... tables) {
        int header = 2 + 2 * tables.length;
        int total = header;
        int[] offsets = new int[tables.length];
        for (int index = 0; index < tables.length; index++) {
            offsets[index] = total;
            total += tables[index].length;
        }
        ByteBuffer buffer = ByteBuffer.allocate(total).order(ByteOrder.BIG_ENDIAN);
        buffer.putShort((short) tables.length);
        for (int offset : offsets) {
            buffer.putShort((short) offset);
        }
        for (byte[] table : tables) {
            buffer.put(table);
        }
        return buffer.array();
    }

    /// Writes `DFLT` and `arab` scripts that enable the first `featureCount` features.
    private static byte[] dualScript(int featureCount) {
        byte[] langSys = langSys(featureCount);
        byte[] script = scriptTable(langSys);
        int records = 14;
        ByteBuffer buffer = ByteBuffer.allocate(records + script.length * 2).order(ByteOrder.BIG_ENDIAN);
        buffer.putShort((short) 2);
        buffer.put("DFLT".getBytes(StandardCharsets.US_ASCII));
        buffer.putShort((short) records);
        buffer.put("arab".getBytes(StandardCharsets.US_ASCII));
        buffer.putShort((short) (records + script.length));
        buffer.put(script);
        buffer.put(script);
        return buffer.array();
    }

    /// Writes a script table with only a default LangSys.
    private static byte[] scriptTable(byte[] langSys) {
        ByteBuffer buffer = ByteBuffer.allocate(4 + langSys.length).order(ByteOrder.BIG_ENDIAN);
        buffer.putShort((short) 4);
        buffer.putShort((short) 0);
        buffer.put(langSys);
        return buffer.array();
    }

    /// Writes a LangSys that enables features `0 .. featureCount-1`.
    private static byte[] langSys(int featureCount) {
        ByteBuffer buffer = ByteBuffer.allocate(6 + 2 * featureCount).order(ByteOrder.BIG_ENDIAN);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0xFFFF);
        buffer.putShort((short) featureCount);
        for (int index = 0; index < featureCount; index++) {
            buffer.putShort((short) index);
        }
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
        buffer.putShort((short) -1);
        buffer.putShort((short) 0);
        buffer.putShort((short) ADVANCE_FINA);
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
        int[] advances = {0, 3, 10, ADVANCE_ISOL, ADVANCE_INIT, ADVANCE_MEDI, ADVANCE_FINA};
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
        return buffer.array();
    }

    /// Writes a name table with one family name.
    private static byte[] name() {
        byte[] family = "HimariGsub".getBytes(StandardCharsets.US_ASCII);
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

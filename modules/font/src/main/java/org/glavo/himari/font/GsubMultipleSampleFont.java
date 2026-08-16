package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;

import java.io.ByteArrayOutputStream;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;

/// Generates a face whose GSUB `ccmp` type-7 wrapper expands `A` into `XY` and `aalt` picks `Z`.
@NotNullByDefault
public final class GsubMultipleSampleFont {
    /// Isolated letter advance.
    public static final int ADVANCE_LETTER = 10;

    /// Substitute `X` advance.
    public static final int ADVANCE_X = 6;

    /// Substitute `Y` advance.
    public static final int ADVANCE_Y = 7;

    /// Alternate `Z` advance.
    public static final int ADVANCE_Z = 16;

    /// Glyph of `A`.
    public static final int GLYPH_A = 2;

    /// First `ccmp` component.
    public static final int GLYPH_X = 3;

    /// Second `ccmp` component.
    public static final int GLYPH_Y = 4;

    /// First `aalt` alternate.
    public static final int GLYPH_Z = 5;

    /// Glyph count including `.notdef` and space.
    private static final int GLYPH_COUNT = 6;

    /// Prevents instantiation.
    private GsubMultipleSampleFont() {
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

    /// Writes a format-4 cmap for space, `A`, `X`, `Y`, and `Z`.
    private static byte[] cmap() {
        ByteBuffer buffer = ByteBuffer.allocate(112).order(ByteOrder.BIG_ENDIAN);
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
        buffer.putShort((short) 12);
        buffer.putShort((short) 8);
        buffer.putShort((short) 2);
        buffer.putShort((short) 0);
        buffer.putShort((short) 32);
        buffer.putShort((short) 'A');
        buffer.putShort((short) 'X');
        buffer.putShort((short) 'Y');
        buffer.putShort((short) 'Z');
        buffer.putShort((short) 0xFFFF);
        buffer.putShort((short) 0);
        buffer.putShort((short) 32);
        buffer.putShort((short) 'A');
        buffer.putShort((short) 'X');
        buffer.putShort((short) 'Y');
        buffer.putShort((short) 'Z');
        buffer.putShort((short) 0xFFFF);
        buffer.putShort((short) (1 - 32));
        buffer.putShort((short) (GLYPH_A - 'A'));
        buffer.putShort((short) (GLYPH_X - 'X'));
        buffer.putShort((short) (GLYPH_Y - 'Y'));
        buffer.putShort((short) (GLYPH_Z - 'Z'));
        buffer.putShort((short) 1);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort(lengthPos, (short) (buffer.position() - format4));
        return slice(buffer);
    }

    /// Writes four rectangles.
    private static byte[] glyf() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        BitmapSfntFont.writeSimpleRect(output, 0, 0, 5, 7);
        BitmapSfntFont.writeSimpleRect(output, 0, 0, 3, 7);
        BitmapSfntFont.writeSimpleRect(output, 0, 0, 3, 7);
        BitmapSfntFont.writeSimpleRect(output, 0, 0, 6, 7);
        return output.toByteArray();
    }

    /// Writes long `loca` offsets.
    private static byte[] loca(int glyfLength) {
        int step = glyfLength / 4;
        ByteBuffer loca = ByteBuffer.allocate((GLYPH_COUNT + 1) * 4).order(ByteOrder.BIG_ENDIAN);
        loca.putInt(0);
        loca.putInt(0);
        loca.putInt(0);
        loca.putInt(step);
        loca.putInt(step * 2);
        loca.putInt(step * 3);
        loca.putInt(glyfLength);
        return loca.array();
    }

    /// Writes lookup 0 (type 7 wrapping type 2 `ccmp`) and lookup 1 (type 3 `aalt`).
    private static byte[] gsub() {
        byte[] multiple = multipleSubst();
        ByteBuffer extension = ByteBuffer.allocate(8 + multiple.length).order(ByteOrder.BIG_ENDIAN);
        extension.putShort((short) 1);
        extension.putShort((short) 2);
        extension.putInt(8);
        extension.put(multiple);
        byte[] wrapped = extension.array();
        ByteBuffer lookup0 = ByteBuffer.allocate(8 + wrapped.length).order(ByteOrder.BIG_ENDIAN);
        lookup0.putShort((short) 7);
        lookup0.putShort((short) 0);
        lookup0.putShort((short) 1);
        lookup0.putShort((short) 8);
        lookup0.put(wrapped);
        byte[] alternate = alternateSubst();
        ByteBuffer lookup1 = ByteBuffer.allocate(8 + alternate.length).order(ByteOrder.BIG_ENDIAN);
        lookup1.putShort((short) 3);
        lookup1.putShort((short) 0);
        lookup1.putShort((short) 1);
        lookup1.putShort((short) 8);
        lookup1.put(alternate);
        byte[] first = lookup0.array();
        byte[] second = lookup1.array();
        ByteBuffer lookupList = ByteBuffer.allocate(6 + first.length + second.length).order(ByteOrder.BIG_ENDIAN);
        lookupList.putShort((short) 2);
        lookupList.putShort((short) 6);
        lookupList.putShort((short) (6 + first.length));
        lookupList.put(first);
        lookupList.put(second);
        byte[] featureList = twoFeatures(SfntFont.TAG_CCMP, 0, SfntFont.TAG_AALT, 1);
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

    /// Writes MultipleSubst format 1 for `A` to `XY`.
    private static byte[] multipleSubst() {
        ByteBuffer buffer = ByteBuffer.allocate(20).order(ByteOrder.BIG_ENDIAN);
        buffer.putShort((short) 1);
        buffer.putShort((short) 14);
        buffer.putShort((short) 1);
        buffer.putShort((short) 8);
        buffer.putShort((short) 2);
        buffer.putShort((short) GLYPH_X);
        buffer.putShort((short) GLYPH_Y);
        buffer.putShort((short) 1);
        buffer.putShort((short) 1);
        buffer.putShort((short) GLYPH_A);
        return buffer.array();
    }

    /// Writes AlternateSubst format 1 for `A` to `Z`.
    private static byte[] alternateSubst() {
        ByteBuffer buffer = ByteBuffer.allocate(18).order(ByteOrder.BIG_ENDIAN);
        buffer.putShort((short) 1);
        buffer.putShort((short) 12);
        buffer.putShort((short) 1);
        buffer.putShort((short) 8);
        buffer.putShort((short) 1);
        buffer.putShort((short) GLYPH_Z);
        buffer.putShort((short) 1);
        buffer.putShort((short) 1);
        buffer.putShort((short) GLYPH_A);
        return buffer.array();
    }

    /// Writes a two-feature list that each enable one lookup.
    private static byte[] twoFeatures(int firstTag, int firstLookup, int secondTag, int secondLookup) {
        ByteBuffer buffer = ByteBuffer.allocate(26).order(ByteOrder.BIG_ENDIAN);
        buffer.putShort((short) 2);
        buffer.putInt(firstTag);
        buffer.putShort((short) 14);
        buffer.putInt(secondTag);
        buffer.putShort((short) 20);
        buffer.putShort((short) 0);
        buffer.putShort((short) 1);
        buffer.putShort((short) firstLookup);
        buffer.putShort((short) 0);
        buffer.putShort((short) 1);
        buffer.putShort((short) secondLookup);
        return buffer.array();
    }

    /// Writes a `DFLT` script enabling features 0 and 1.
    private static byte[] defaultScript() {
        ByteBuffer langSys = ByteBuffer.allocate(10).order(ByteOrder.BIG_ENDIAN);
        langSys.putShort((short) 0);
        langSys.putShort((short) 0xFFFF);
        langSys.putShort((short) 2);
        langSys.putShort((short) 0);
        langSys.putShort((short) 1);
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
        buffer.putShort((short) ADVANCE_Z);
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
        int[] advances = {0, 3, ADVANCE_LETTER, ADVANCE_X, ADVANCE_Y, ADVANCE_Z};
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
        byte[] family = "HimariMultiple".getBytes(StandardCharsets.US_ASCII);
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

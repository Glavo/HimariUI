package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;

import java.io.ByteArrayOutputStream;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;

/// Generates a face whose GSUB `calt` type-6 format-1 rule turns `A` into `Y`
/// only after backtrack `UTSRQPONMLKJIHGFED` and before `BC`.
@NotNullByDefault
public final class GsubChainOctodecaBacktrackSampleFont {
    /// Isolated letter advance.
    public static final int ADVANCE_LETTER = 10;

    /// Substitute advance.
    public static final int ADVANCE_Y = 15;

    /// Glyph of `A`.
    public static final int GLYPH_A = 2;

    /// Glyph of `B`.
    public static final int GLYPH_B = 3;

    /// Glyph of `C`.
    public static final int GLYPH_C = 4;

    /// Nearest backtrack glyph of `D`.
    public static final int GLYPH_D = 5;

    /// Middle backtrack glyph of `E`.
    public static final int GLYPH_E = 6;

    /// Far backtrack glyph of `F`.
    public static final int GLYPH_F = 7;

    /// Fourth backtrack glyph of `G`.
    public static final int GLYPH_G = 8;

    /// Fifth backtrack glyph of `H`.
    public static final int GLYPH_H = 9;

    /// Sixth backtrack glyph of `I`.
    public static final int GLYPH_I = 10;

    /// Seventh backtrack glyph of `J`.
    public static final int GLYPH_J = 11;

    /// Eighth backtrack glyph of `K`.
    public static final int GLYPH_K = 12;

    /// Ninth backtrack glyph of `L`.
    public static final int GLYPH_L = 13;

    /// Tenth backtrack glyph of `M`.
    public static final int GLYPH_M = 14;

    /// Eleventh backtrack glyph of `N`.
    public static final int GLYPH_N = 15;

    /// Twelfth backtrack glyph of `O`.
    public static final int GLYPH_O = 16;

    /// Thirteenth backtrack glyph of `P`.
    public static final int GLYPH_P = 17;

    /// Fourteenth backtrack glyph of `Q`.
    public static final int GLYPH_Q = 18;

    /// Fifteenth backtrack glyph of `R`.
    public static final int GLYPH_R = 19;

    /// Sixteenth backtrack glyph of `S`.
    public static final int GLYPH_S = 20;

    /// Seventeenth backtrack glyph of `T`.
    public static final int GLYPH_T = 21;

    /// Farthest backtrack glyph of `U`.
    public static final int GLYPH_U = 22;

    /// Substitute glyph mapped from `Y`.
    public static final int GLYPH_Y = 23;

    /// Glyph count including `.notdef` and space.
    private static final int GLYPH_COUNT = 24;

    /// Prevents instantiation.
    private GsubChainOctodecaBacktrackSampleFont() {
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

    /// Writes a format-4 cmap for space, the contiguous `A`–`U` range, and `Y`.
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
        buffer.putShort((short) 8);
        buffer.putShort((short) 8);
        buffer.putShort((short) 2);
        buffer.putShort((short) 0);
        buffer.putShort((short) 32);
        buffer.putShort((short) 'U');
        buffer.putShort((short) 'Y');
        buffer.putShort((short) 0xFFFF);
        buffer.putShort((short) 0);
        buffer.putShort((short) 32);
        buffer.putShort((short) 'A');
        buffer.putShort((short) 'Y');
        buffer.putShort((short) 0xFFFF);
        buffer.putShort((short) (1 - 32));
        buffer.putShort((short) (GLYPH_A - 'A'));
        buffer.putShort((short) (GLYPH_Y - 'Y'));
        buffer.putShort((short) 1);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort(lengthPos, (short) (buffer.position() - format4));
        return slice(buffer);
    }

    /// Writes twenty-two rectangles.
    private static byte[] glyf() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        for (int index = 0; index < 21; index++) {
            BitmapSfntFont.writeSimpleRect(output, 0, 0, 5, 7);
        }
        BitmapSfntFont.writeSimpleRect(output, 0, 0, 6, 7);
        return output.toByteArray();
    }

    /// Writes long `loca` offsets.
    private static byte[] loca(int glyfLength) {
        int step = glyfLength / 22;
        ByteBuffer loca = ByteBuffer.allocate((GLYPH_COUNT + 1) * 4).order(ByteOrder.BIG_ENDIAN);
        loca.putInt(0);
        loca.putInt(0);
        loca.putInt(0);
        for (int index = 1; index <= 21; index++) {
            loca.putInt(step * index);
        }
        loca.putInt(glyfLength);
        return loca.array();
    }

    /// Writes lookup 0 (type 6) and lookup 1 (type 1).
    private static byte[] gsub() {
        byte[] nested = singleSubst();
        ByteBuffer lookup1 = ByteBuffer.allocate(8 + nested.length).order(ByteOrder.BIG_ENDIAN);
        lookup1.putShort((short) 1);
        lookup1.putShort((short) 0);
        lookup1.putShort((short) 1);
        lookup1.putShort((short) 8);
        lookup1.put(nested);
        byte[] chain = chainSubst();
        ByteBuffer lookup0 = ByteBuffer.allocate(8 + chain.length).order(ByteOrder.BIG_ENDIAN);
        lookup0.putShort((short) 6);
        lookup0.putShort((short) 0);
        lookup0.putShort((short) 1);
        lookup0.putShort((short) 8);
        lookup0.put(chain);
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

    /// Writes ChainContextSubst format 1 for `UTSRQPONMLKJIHGFEDABC`. Coverage sits after the rule.
    private static byte[] chainSubst() {
        ByteBuffer buffer = ByteBuffer.allocate(70).order(ByteOrder.BIG_ENDIAN);
        buffer.putShort((short) 1);
        buffer.putShort((short) 64);
        buffer.putShort((short) 1);
        buffer.putShort((short) 8);
        buffer.putShort((short) 1);
        buffer.putShort((short) 4);
        buffer.putShort((short) 18);
        buffer.putShort((short) GLYPH_D);
        buffer.putShort((short) GLYPH_E);
        buffer.putShort((short) GLYPH_F);
        buffer.putShort((short) GLYPH_G);
        buffer.putShort((short) GLYPH_H);
        buffer.putShort((short) GLYPH_I);
        buffer.putShort((short) GLYPH_J);
        buffer.putShort((short) GLYPH_K);
        buffer.putShort((short) GLYPH_L);
        buffer.putShort((short) GLYPH_M);
        buffer.putShort((short) GLYPH_N);
        buffer.putShort((short) GLYPH_O);
        buffer.putShort((short) GLYPH_P);
        buffer.putShort((short) GLYPH_Q);
        buffer.putShort((short) GLYPH_R);
        buffer.putShort((short) GLYPH_S);
        buffer.putShort((short) GLYPH_T);
        buffer.putShort((short) GLYPH_U);
        buffer.putShort((short) 2);
        buffer.putShort((short) GLYPH_B);
        buffer.putShort((short) 1);
        buffer.putShort((short) GLYPH_C);
        buffer.putShort((short) 1);
        buffer.putShort((short) 0);
        buffer.putShort((short) 1);
        buffer.putShort((short) 1);
        buffer.putShort((short) 1);
        buffer.putShort((short) GLYPH_A);
        return buffer.array();
    }

    /// Writes SingleSubst format 2 `A` to `Y`.
    private static byte[] singleSubst() {
        ByteBuffer buffer = ByteBuffer.allocate(16).order(ByteOrder.BIG_ENDIAN);
        buffer.putShort((short) 2);
        buffer.putShort((short) 8);
        buffer.putShort((short) 1);
        buffer.putShort((short) GLYPH_Y);
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
        for (int index = 0; index < GLYPH_COUNT; index++) {
            buffer.putShort((short) (index == GLYPH_Y ? ADVANCE_Y : index < 2 ? (index == 0 ? 0 : 3) : ADVANCE_LETTER));
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
        byte[] family = "HimariGsubChain18Back".getBytes(StandardCharsets.US_ASCII);
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

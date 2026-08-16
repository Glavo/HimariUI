package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;

import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;

/// Generates an `OTTO` font whose CFF2 CharString is one Type 2 cubic.
///
/// Glyph 1 is `A`. The curve starts at `(0,0)`, uses controls `(0,10)` and `(10,10)`, and ends at
/// `(20,0)`.
@NotNullByDefault
public final class Cff2SampleFont {
    /// First cubic control x.
    public static final float C1X = 0.0f;

    /// First cubic control y.
    public static final float C1Y = 10.0f;

    /// Second cubic control x.
    public static final float C2X = 10.0f;

    /// Second cubic control y.
    public static final float C2Y = 10.0f;

    /// Destination x.
    public static final float X = 20.0f;

    /// Destination y.
    public static final float Y = 0.0f;

    /// Prevents instantiation.
    private Cff2SampleFont() {
    }

    /// Builds the CFF2 sample font.
    ///
    /// @return the parsed font
    public static SfntFont create() {
        return new SfntFont(bytes());
    }

    /// Builds the CFF2 sample font image.
    ///
    /// @return a read-only OTTO file
    public static MemorySegment bytes() {
        LinkedHashMap<String, byte[]> tables = new LinkedHashMap<>();
        tables.put("CFF2", cff2());
        tables.put("cmap", CffSampleFont.cmap());
        tables.put("head", CffSampleFont.head());
        tables.put("hhea", CffSampleFont.hhea());
        tables.put("hmtx", CffSampleFont.hmtx());
        tables.put("maxp", CffSampleFont.maxp());
        tables.put("name", CffSampleFont.name("HimariCFF2"));
        tables.put("post", CffSampleFont.post());
        return MemorySegment.ofArray(BitmapSfntFont.wrapOtto(tables)).asReadOnly();
    }

    /// Writes a single-FD CFF2 table.
    private static byte[] cff2() {
        byte[] globalSubrs = CffSampleFont.emptyIndex2();
        byte[] charstrings = CffSampleFont.index2(new byte[0], CffSampleFont.cubic());
        byte[] priv = new byte[0];
        int topDictGuess = 16;
        int header = 5;
        int fdArrayOffset = header + topDictGuess + globalSubrs.length;
        byte[] fontDictGuess = fontDict(0, 0);
        byte[] fdArrayGuess = CffSampleFont.index2(fontDictGuess);
        int privateOffset = fdArrayOffset + fdArrayGuess.length;
        int charStringsOffset = privateOffset + priv.length;
        byte[] topDict = topDict(charStringsOffset, fdArrayOffset);
        byte[] fontDict = fontDict(priv.length, privateOffset);
        byte[] fdArray = CffSampleFont.index2(fontDict);
        if (topDict.length != topDictGuess || fdArray.length != fdArrayGuess.length) {
            fdArrayOffset = header + topDict.length + globalSubrs.length;
            privateOffset = fdArrayOffset + fdArray.length;
            charStringsOffset = privateOffset + priv.length;
            topDict = topDict(charStringsOffset, fdArrayOffset);
            fontDict = fontDict(priv.length, privateOffset);
            fdArray = CffSampleFont.index2(fontDict);
        }
        ByteBuffer table = ByteBuffer.allocate(
                header + topDict.length + globalSubrs.length + fdArray.length + priv.length + charstrings.length
        );
        table.put((byte) 2);
        table.put((byte) 0);
        table.put((byte) 5);
        table.putShort((short) topDict.length);
        table.put(topDict);
        table.put(globalSubrs);
        table.put(fdArray);
        table.put(priv);
        table.put(charstrings);
        return table.array();
    }

    /// Writes the CFF2 Top DICT.
    private static byte[] topDict(int charStrings, int fdArray) {
        ByteBuffer buffer = ByteBuffer.allocate(20);
        CffSampleFont.putCffInt32(buffer, charStrings);
        buffer.put((byte) 17);
        CffSampleFont.putCffInt32(buffer, fdArray);
        buffer.put((byte) 12);
        buffer.put((byte) 36);
        return CffSampleFont.slice(buffer);
    }

    /// Writes one Font DICT with a Private reference.
    private static byte[] fontDict(int privateSize, int privateOffset) {
        ByteBuffer buffer = ByteBuffer.allocate(16);
        CffSampleFont.putCffInt32(buffer, privateSize);
        CffSampleFont.putCffInt32(buffer, privateOffset);
        buffer.put((byte) 18);
        return CffSampleFont.slice(buffer);
    }
}

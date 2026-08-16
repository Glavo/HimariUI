package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;

/// Generates the [`GvarSampleFont`] face plus an `avar` map that sends mid-axis `0.5` to `1.0`.
///
/// Weight `650` therefore receives the full peak `gvar` deltas instead of half.
@NotNullByDefault
public final class AvarSampleFont {
    /// Design-space weight whose normalized value is `0.5`.
    public static final float MID_WEIGHT =
            (GvarSampleFont.DEFAULT_WEIGHT + GvarSampleFont.MAX_WEIGHT) * 0.5f;

    /// Prevents instantiation.
    private AvarSampleFont() {
    }

    /// Builds the avar sample font.
    ///
    /// @return the parsed font
    public static SfntFont create() {
        return new SfntFont(bytes());
    }

    /// Builds the avar sample font image.
    ///
    /// @return a read-only SFNT file
    public static MemorySegment bytes() {
        SfntFont base = GvarSampleFont.create();
        LinkedHashMap<String, byte[]> tables = copyTables(base.bytes());
        tables.put("avar", avar());
        return MemorySegment.ofArray(BitmapSfntFont.wrap(tables)).asReadOnly();
    }

    /// Copies the base SFNT tables so `avar` can be inserted.
    private static LinkedHashMap<String, byte[]> copyTables(MemorySegment file) {
        ByteBuffer buffer = file.asByteBuffer().order(ByteOrder.BIG_ENDIAN);
        buffer.getInt();
        int tableCount = Short.toUnsignedInt(buffer.getShort());
        buffer.getShort();
        buffer.getShort();
        buffer.getShort();
        LinkedHashMap<String, byte[]> tables = new LinkedHashMap<>();
        int[] offsets = new int[tableCount];
        int[] lengths = new int[tableCount];
        String[] tags = new String[tableCount];
        for (int index = 0; index < tableCount; index++) {
            byte[] tagBytes = new byte[4];
            buffer.get(tagBytes);
            buffer.getInt();
            offsets[index] = buffer.getInt();
            lengths[index] = buffer.getInt();
            tags[index] = new String(tagBytes, StandardCharsets.US_ASCII);
        }
        byte[] bytes = file.toArray(ValueLayout.JAVA_BYTE);
        for (int index = 0; index < tableCount; index++) {
            byte[] payload = new byte[lengths[index]];
            System.arraycopy(bytes, offsets[index], payload, 0, lengths[index]);
            tables.put(tags[index], payload);
        }
        return tables;
    }

    /// Writes one axis map: `-1→-1`, `0→0`, `0.5→1`, `1→1`.
    private static byte[] avar() {
        ByteBuffer buffer = ByteBuffer.allocate(26).order(ByteOrder.BIG_ENDIAN);
        buffer.putShort((short) 1);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort((short) 1);
        buffer.putShort((short) 4);
        buffer.putShort((short) 0xC000);
        buffer.putShort((short) 0xC000);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0x2000);
        buffer.putShort((short) 0x4000);
        buffer.putShort((short) 0x4000);
        buffer.putShort((short) 0x4000);
        return buffer.array();
    }
}

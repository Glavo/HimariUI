package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/// Parses `sbix` strikes for embedded bitmap access.
///
/// Only the first strike is retained. Empty glyph slots return `null`.
@NotNullByDefault
final class SbixTable {
    /// Shared empty table.
    static final SbixTable EMPTY = new SbixTable(0, 0, new int[0], new byte[0]);

    /// Strike ppem.
    private final int ppem;

    /// Number of glyphs described by the strike offsets.
    private final int glyphCount;

    /// Offsets from the start of the table to each glyph record, length `glyphCount + 1`.
    private final int[] offsets;

    /// Whole `sbix` table copy used to slice glyph data.
    private final byte[] table;

    /// Creates a parsed strike.
    private SbixTable(int ppem, int glyphCount, int[] offsets, byte[] table) {
        this.ppem = ppem;
        this.glyphCount = glyphCount;
        this.offsets = offsets;
        this.table = table;
    }

    /// Parses an optional `sbix` table.
    ///
    /// @param table the table, or `null`
    /// @param glyphCount the face glyph count
    /// @return the first strike, or empty
    static SbixTable parse(@Nullable ByteBuffer table, int glyphCount) {
        if (table == null || glyphCount < 0) {
            return EMPTY;
        }
        ByteBuffer buffer = table.duplicate().order(ByteOrder.BIG_ENDIAN);
        buffer.clear();
        if (buffer.remaining() < 8) {
            return EMPTY;
        }
        int version = Short.toUnsignedInt(buffer.getShort());
        buffer.getShort();
        int strikes = buffer.getInt();
        if (version < 1 || strikes < 1 || buffer.remaining() < 4) {
            return EMPTY;
        }
        int strikeOffset = buffer.getInt();
        if (strikeOffset < 0 || (long) strikeOffset + 4L + (long) (glyphCount + 1) * 4L > buffer.capacity()) {
            throw new IllegalArgumentException("sbix strike is truncated");
        }
        buffer.clear();
        buffer.position(strikeOffset);
        int ppem = Short.toUnsignedInt(buffer.getShort());
        buffer.getShort();
        int[] offsets = new int[glyphCount + 1];
        for (int index = 0; index <= glyphCount; index++) {
            offsets[index] = strikeOffset + buffer.getInt();
        }
        byte[] bytes = new byte[buffer.capacity()];
        buffer.clear();
        buffer.get(bytes);
        return new SbixTable(ppem, glyphCount, offsets, bytes);
    }

    /// Returns the bitmap for `glyphId` in the first strike.
    ///
    /// @param glyphId the glyph
    /// @return the strike, or `null` when the slot is empty
    @Nullable EmbeddedBitmap glyph(int glyphId) {
        if (ppem == 0 || glyphId < 0 || glyphId >= glyphCount) {
            return null;
        }
        int start = offsets[glyphId];
        int end = offsets[glyphId + 1];
        if (end - start < 8) {
            return null;
        }
        if (start < 0 || end > table.length) {
            throw new IllegalArgumentException("sbix glyph range is out of bounds");
        }
        int originX = (short) (((table[start] & 0xFF) << 8) | (table[start + 1] & 0xFF));
        int originY = (short) (((table[start + 2] & 0xFF) << 8) | (table[start + 3] & 0xFF));
        int type = ((table[start + 4] & 0xFF) << 24)
                | ((table[start + 5] & 0xFF) << 16)
                | ((table[start + 6] & 0xFF) << 8)
                | (table[start + 7] & 0xFF);
        byte[] data = new byte[end - start - 8];
        System.arraycopy(table, start + 8, data, 0, data.length);
        return new EmbeddedBitmap(ppem, originX, originY, type, data);
    }
}

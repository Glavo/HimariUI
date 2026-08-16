package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/// Parses CBLC/CBDT or EBLC/EBDT format-1 8-bit strikes.
///
/// Only the first strike and index format 1 / image format 1 are retained. Other formats yield
/// no bitmap. A missing table pair is empty.
@NotNullByDefault
final class CbdtCblc {
    /// Shared empty table.
    static final CbdtCblc EMPTY = new CbdtCblc(0, -1, -1, new int[0], new byte[0], 0);

    /// `CBDT` graphic tag.
    static final int TAG_CBDT = 0x43424454;

    /// `EBDT` graphic tag.
    static final int TAG_EBDT = 0x45424454;

    /// Strike ppem.
    private final int ppem;

    /// First glyph in the strike, or `-1`.
    private final int startGlyph;

    /// Last glyph in the strike, or `-1`.
    private final int endGlyph;

    /// Offsets into [`#cbdt`] for each glyph in `[startGlyph, endGlyph]`, plus one sentinel.
    private final int[] offsets;

    /// CBDT or EBDT table bytes.
    private final byte[] cbdt;

    /// Graphic type written into [`EmbeddedBitmap`].
    private final int graphicType;

    /// Creates a parsed strike.
    private CbdtCblc(int ppem, int startGlyph, int endGlyph, int[] offsets, byte[] cbdt, int graphicType) {
        this.ppem = ppem;
        this.startGlyph = startGlyph;
        this.endGlyph = endGlyph;
        this.offsets = offsets;
        this.cbdt = cbdt;
        this.graphicType = graphicType;
    }

    /// Parses optional `CBLC` and `CBDT` tables.
    ///
    /// @param cblc the location table, or `null`
    /// @param cbdtTable the data table, or `null`
    /// @return the first format-1 strike, or empty
    static CbdtCblc parse(@Nullable ByteBuffer cblc, @Nullable ByteBuffer cbdtTable) {
        return parse(cblc, cbdtTable, TAG_CBDT);
    }

    /// Parses an optional location/data pair using `graphicType` on the returned strike.
    ///
    /// @param location the `CBLC` or `EBLC` table, or `null`
    /// @param dataTable the `CBDT` or `EBDT` table, or `null`
    /// @param graphicType the four-byte graphic tag stored on each bitmap
    /// @return the first format-1 strike, or empty
    static CbdtCblc parse(@Nullable ByteBuffer location, @Nullable ByteBuffer dataTable, int graphicType) {
        if (location == null || dataTable == null) {
            return EMPTY;
        }
        ByteBuffer index = location.duplicate().order(ByteOrder.BIG_ENDIAN);
        ByteBuffer data = dataTable.duplicate().order(ByteOrder.BIG_ENDIAN);
        index.clear();
        data.clear();
        if (index.remaining() < 56 || data.remaining() < 4) {
            return EMPTY;
        }
        int major = Short.toUnsignedInt(index.getShort());
        index.getShort();
        int sizes = index.getInt();
        if (major < 2 || sizes < 1) {
            return EMPTY;
        }
        int arrayOffset = index.getInt();
        index.getInt();
        int subtables = index.getInt();
        index.getInt();
        index.position(index.position() + 24);
        int startGlyph = Short.toUnsignedInt(index.getShort());
        int endGlyph = Short.toUnsignedInt(index.getShort());
        int ppem = index.get() & 0xFF;
        index.get();
        int bitDepth = index.get() & 0xFF;
        if (subtables < 1 || bitDepth != 8 || endGlyph < startGlyph) {
            return EMPTY;
        }
        if (arrayOffset < 0 || arrayOffset + 8 > index.capacity()) {
            throw new IllegalArgumentException("CBLC index array is truncated");
        }
        index.clear();
        index.position(arrayOffset);
        int first = Short.toUnsignedInt(index.getShort());
        int last = Short.toUnsignedInt(index.getShort());
        int extra = index.getInt();
        if (first != startGlyph || last != endGlyph) {
            return EMPTY;
        }
        int subtableOffset = arrayOffset + extra;
        if (subtableOffset < 0 || subtableOffset + 8 > index.capacity()) {
            throw new IllegalArgumentException("CBLC index subtable is truncated");
        }
        index.clear();
        index.position(subtableOffset);
        int indexFormat = Short.toUnsignedInt(index.getShort());
        int imageFormat = Short.toUnsignedInt(index.getShort());
        int imageDataOffset = index.getInt();
        if (indexFormat != 1 || imageFormat != 1) {
            return EMPTY;
        }
        int slots = endGlyph - startGlyph + 2;
        if (index.remaining() < slots * 4) {
            throw new IllegalArgumentException("CBLC sbit offsets are truncated");
        }
        int[] offsets = new int[slots];
        for (int i = 0; i < slots; i++) {
            offsets[i] = imageDataOffset + index.getInt();
        }
        byte[] bytes = new byte[data.capacity()];
        data.clear();
        data.get(bytes);
        return new CbdtCblc(ppem, startGlyph, endGlyph, offsets, bytes, graphicType);
    }

    /// Returns the format-1 bitmap for `glyphId`.
    ///
    /// @param glyphId the glyph
    /// @return the strike, or `null` when uncovered
    @Nullable EmbeddedBitmap glyph(int glyphId) {
        if (ppem == 0 || glyphId < startGlyph || glyphId > endGlyph) {
            return null;
        }
        int slot = glyphId - startGlyph;
        int start = offsets[slot];
        int end = offsets[slot + 1];
        if (end - start < 5) {
            return null;
        }
        if (start < 0 || end > cbdt.length) {
            throw new IllegalArgumentException("CBDT glyph range is out of bounds");
        }
        int height = cbdt[start] & 0xFF;
        int width = cbdt[start + 1] & 0xFF;
        int expected = 5 + width * height;
        if (end - start < expected) {
            throw new IllegalArgumentException("CBDT image is truncated");
        }
        byte[] pixels = new byte[width * height];
        System.arraycopy(cbdt, start + 5, pixels, 0, pixels.length);
        return new EmbeddedBitmap(ppem, cbdt[start + 2], cbdt[start + 3], graphicType, pixels);
    }
}

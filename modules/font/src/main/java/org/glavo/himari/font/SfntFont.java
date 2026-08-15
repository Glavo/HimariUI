package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/// Reads a checked SFNT directory, `cmap`, `hmtx`, `loca`, and `glyf`.
@NotNullByDefault
public final class SfntFont {
    /// The original font bytes.
    private final byte @Unmodifiable [] bytes;

    /// Table directory by tag.
    private final @Unmodifiable Map<String, TableRecord> tables;

    /// Units per em.
    private final int unitsPerEm;

    /// Number of glyphs.
    private final int glyphCount;

    /// Format-4 cmap: start codes.
    private final int[] cmapStart;

    /// Format-4 cmap: end codes.
    private final int[] cmapEnd;

    /// Format-4 cmap: idDeltas.
    private final short[] cmapDelta;

    /// Format-4 cmap: idRangeOffset.
    private final int[] cmapRangeOffset;

    /// Format-4 cmap: glyph id array referenced by range offsets.
    private final int[] cmapGlyphIds;

    /// Advance widths.
    private final int[] advances;

    /// loca offsets into glyf.
    private final int[] loca;

    /// Creates a font from SFNT bytes.
    ///
    /// @param bytes the complete font file
    public SfntFont(byte[] bytes) {
        this.bytes = Objects.requireNonNull(bytes, "bytes").clone();
        ByteBuffer buffer = ByteBuffer.wrap(this.bytes).order(ByteOrder.BIG_ENDIAN);
        if (buffer.remaining() < 12) {
            throw new IllegalArgumentException("SFNT header is truncated");
        }
        buffer.getInt();
        int tableCount = Short.toUnsignedInt(buffer.getShort());
        buffer.getShort();
        buffer.getShort();
        buffer.getShort();
        LinkedHashMap<String, TableRecord> directory = new LinkedHashMap<>();
        for (int index = 0; index < tableCount; index++) {
            if (buffer.remaining() < 16) {
                throw new IllegalArgumentException("SFNT table directory is truncated");
            }
            byte[] tagBytes = new byte[4];
            buffer.get(tagBytes);
            buffer.getInt();
            int offset = buffer.getInt();
            int length = buffer.getInt();
            if (offset < 0 || length < 0 || offset + length > this.bytes.length) {
                throw new IllegalArgumentException("SFNT table is out of range");
            }
            directory.put(new String(tagBytes, java.nio.charset.StandardCharsets.US_ASCII), new TableRecord(offset, length));
        }
        this.tables = Map.copyOf(directory);
        ByteBuffer head = table("head");
        if (head.remaining() < 54) {
            throw new IllegalArgumentException("head table is truncated");
        }
        head.position(18);
        this.unitsPerEm = Short.toUnsignedInt(head.getShort());
        ByteBuffer maxp = table("maxp");
        if (maxp.remaining() < 6) {
            throw new IllegalArgumentException("maxp table is truncated");
        }
        maxp.position(4);
        this.glyphCount = Short.toUnsignedInt(maxp.getShort());
        CmapFormat4 cmap = readCmap();
        this.cmapStart = cmap.startCodes;
        this.cmapEnd = cmap.endCodes;
        this.cmapDelta = cmap.idDeltas;
        this.cmapRangeOffset = cmap.idRangeOffsets;
        this.cmapGlyphIds = cmap.glyphIds;
        this.advances = readAdvances();
        this.loca = readLoca(head);
    }

    /// Returns units per em.
    ///
    /// @return the em size
    public int unitsPerEm() {
        return unitsPerEm;
    }

    /// Maps a Unicode code point through `cmap`.
    ///
    /// @param codePoint the code point
    /// @return the glyph id, or `0`
    public int glyphId(int codePoint) {
        for (int index = 0; index < cmapEnd.length; index++) {
            if (codePoint >= cmapStart[index] && codePoint <= cmapEnd[index]) {
                if (cmapRangeOffset[index] == 0) {
                    return (codePoint + cmapDelta[index]) & 0xFFFF;
                }
                int glyphIndex = cmapRangeOffset[index] / 2 + (codePoint - cmapStart[index]) - (cmapEnd.length - index);
                if (glyphIndex < 0 || glyphIndex >= cmapGlyphIds.length) {
                    return 0;
                }
                int glyphId = cmapGlyphIds[glyphIndex];
                if (glyphId == 0) {
                    return 0;
                }
                return (glyphId + cmapDelta[index]) & 0xFFFF;
            }
        }
        return 0;
    }

    /// Returns horizontal metrics for a glyph.
    ///
    /// @param glyphId the glyph id
    /// @return the metrics
    public GlyphMetrics metrics(int glyphId) {
        if (glyphId < 0 || glyphId >= advances.length) {
            throw new IllegalArgumentException("Unknown glyph " + glyphId);
        }
        return new GlyphMetrics(glyphId, advances[glyphId], 0);
    }

    /// Returns the glyf bytes for a glyph, or an empty buffer for a space.
    ///
    /// @param glyphId the glyph id
    /// @return the glyf slice
    public ByteBuffer glyf(int glyphId) {
        if (glyphId < 0 || glyphId + 1 >= loca.length) {
            throw new IllegalArgumentException("Unknown glyph " + glyphId);
        }
        int start = loca[glyphId];
        int end = loca[glyphId + 1];
        TableRecord glyf = requireTable("glyf");
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
        buffer.position(glyf.offset + start);
        buffer.limit(glyf.offset + end);
        return buffer.slice().order(ByteOrder.BIG_ENDIAN);
    }

    /// Returns a table buffer.
    ///
    /// @param tag the table tag
    /// @return the table
    private ByteBuffer table(String tag) {
        TableRecord record = requireTable(tag);
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
        buffer.position(record.offset);
        buffer.limit(record.offset + record.length);
        return buffer.slice().order(ByteOrder.BIG_ENDIAN);
    }

    /// Requires a table record.
    ///
    /// @param tag the tag
    /// @return the record
    private TableRecord requireTable(String tag) {
        @Nullable TableRecord record = tables.get(tag);
        if (record == null) {
            throw new IllegalArgumentException("Missing SFNT table " + tag);
        }
        return record;
    }

    /// Reads a format-4 cmap.
    ///
    /// @return the cmap
    private CmapFormat4 readCmap() {
        ByteBuffer cmap = table("cmap");
        if (cmap.remaining() < 4) {
            throw new IllegalArgumentException("cmap is truncated");
        }
        cmap.getShort();
        int records = Short.toUnsignedInt(cmap.getShort());
        int format4 = -1;
        for (int index = 0; index < records; index++) {
            int platform = Short.toUnsignedInt(cmap.getShort());
            cmap.getShort();
            int offset = cmap.getInt();
            if ((platform == 0 || platform == 3) && format4 < 0) {
                format4 = offset;
            }
        }
        if (format4 < 0) {
            throw new IllegalArgumentException("cmap has no Unicode record");
        }
        cmap.position(format4);
        if (Short.toUnsignedInt(cmap.getShort()) != 4) {
            throw new IllegalArgumentException("Only cmap format 4 is supported");
        }
        cmap.getShort();
        cmap.getShort();
        int segCount = Short.toUnsignedInt(cmap.getShort()) / 2;
        cmap.getShort();
        cmap.getShort();
        cmap.getShort();
        int[] endCodes = new int[segCount];
        for (int index = 0; index < segCount; index++) {
            endCodes[index] = Short.toUnsignedInt(cmap.getShort());
        }
        cmap.getShort();
        int[] startCodes = new int[segCount];
        short[] deltas = new short[segCount];
        int[] rangeOffsets = new int[segCount];
        for (int index = 0; index < segCount; index++) {
            startCodes[index] = Short.toUnsignedInt(cmap.getShort());
        }
        for (int index = 0; index < segCount; index++) {
            deltas[index] = cmap.getShort();
        }
        for (int index = 0; index < segCount; index++) {
            rangeOffsets[index] = Short.toUnsignedInt(cmap.getShort());
        }
        int remaining = cmap.remaining() / 2;
        int[] glyphIds = new int[remaining];
        for (int index = 0; index < remaining; index++) {
            glyphIds[index] = Short.toUnsignedInt(cmap.getShort());
        }
        return new CmapFormat4(startCodes, endCodes, deltas, rangeOffsets, glyphIds);
    }

    /// Reads advance widths from `hmtx`.
    ///
    /// @return the advances
    private int[] readAdvances() {
        ByteBuffer hhea = table("hhea");
        if (hhea.remaining() < 36) {
            throw new IllegalArgumentException("hhea is truncated");
        }
        hhea.position(34);
        int metricsCount = Short.toUnsignedInt(hhea.getShort());
        ByteBuffer hmtx = table("hmtx");
        int[] values = new int[glyphCount];
        int last = 0;
        for (int index = 0; index < glyphCount; index++) {
            if (index < metricsCount) {
                last = Short.toUnsignedInt(hmtx.getShort());
                hmtx.getShort();
            }
            values[index] = last;
        }
        return values;
    }

    /// Reads `loca` offsets.
    ///
    /// @param head the head table
    /// @return the offsets
    private int[] readLoca(ByteBuffer head) {
        head.position(50);
        int format = head.getShort();
        ByteBuffer locaTable = table("loca");
        int[] offsets = new int[glyphCount + 1];
        for (int index = 0; index < offsets.length; index++) {
            offsets[index] = format == 0
                    ? Short.toUnsignedInt(locaTable.getShort()) * 2
                    : locaTable.getInt();
        }
        return offsets;
    }

    /// Stores one table directory record.
    ///
    /// @param offset the file offset
    /// @param length the length
    private record TableRecord(int offset, int length) {
    }

    /// Stores a parsed format-4 cmap.
    ///
    /// @param startCodes start codes
    /// @param endCodes end codes
    /// @param idDeltas id deltas
    /// @param idRangeOffsets range offsets
    /// @param glyphIds glyph id array
    private record CmapFormat4(
            int[] startCodes,
            int[] endCodes,
            short[] idDeltas,
            int[] idRangeOffsets,
            int[] glyphIds
    ) {
    }
}

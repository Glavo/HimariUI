package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/// Reads a checked SFNT directory, `cmap` format 4/12, `hmtx`, TrueType `loca`/`glyf` or
/// CFF/CFF2 Type 2 outlines, optional GSUB, and GPOS/`kern`.
///
/// The font file is retained as a read-only [MemorySegment] so the same view can back a heap array
/// or a later mapped file. Sequential table decoding uses [ByteBuffer] cursors over those slices.
@NotNullByDefault
public final class SfntFont {
    /// The retained font file.
    private final MemorySegment data;

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

    /// Format-12 cmap: start code points, empty when absent.
    private final int[] cmap12Start;

    /// Format-12 cmap: end code points, empty when absent.
    private final int[] cmap12End;

    /// Format-12 cmap: start glyph ids, empty when absent.
    private final int[] cmap12Glyph;

    /// Advance widths.
    private final int[] advances;

    /// loca offsets into glyf, empty for a CFF face.
    private final int[] loca;

    /// CFF/CFF2 outlines, or `null` for a TrueType face.
    private final @Nullable CffOutlines cff;

    /// GSUB type-1 substitutions, or empty when the table is absent.
    private final GsubSubstitutions gsub;

    /// GPOS type-2 and format-0 `kern` pair adjustments, or empty when both tables are absent.
    private final GposPositioning gpos;

    /// Creates a font from heap SFNT bytes.
    ///
    /// @param bytes the complete font file
    public SfntFont(byte[] bytes) {
        this(MemorySegment.ofArray(Objects.requireNonNull(bytes, "bytes").clone()));
    }

    /// Creates a font from an SFNT memory image.
    ///
    /// The constructor copies `bytes` onto the heap so the font does not retain the caller's arena
    /// or array.
    ///
    /// @param bytes the complete font file
    public SfntFont(MemorySegment bytes) {
        Objects.requireNonNull(bytes, "bytes");
        this.data = MemorySegment.ofArray(bytes.toArray(ValueLayout.JAVA_BYTE)).asReadOnly();
        ByteBuffer buffer = cursor(data);
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
            if (offset < 0 || length < 0 || (long) offset + (long) length > data.byteSize()) {
                throw new IllegalArgumentException("SFNT table is out of range");
            }
            directory.put(new String(tagBytes, StandardCharsets.US_ASCII), new TableRecord(offset, length));
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
        CmapTables cmap = readCmap();
        this.cmapStart = cmap.format4.startCodes;
        this.cmapEnd = cmap.format4.endCodes;
        this.cmapDelta = cmap.format4.idDeltas;
        this.cmapRangeOffset = cmap.format4.idRangeOffsets;
        this.cmapGlyphIds = cmap.format4.glyphIds;
        this.cmap12Start = cmap.format12.startCodes;
        this.cmap12End = cmap.format12.endCodes;
        this.cmap12Glyph = cmap.format12.startGlyphs;
        this.advances = readAdvances();
        @Nullable ByteBuffer cffTable = findTable("CFF ");
        @Nullable ByteBuffer cff2Table = findTable("CFF2");
        if (tables.containsKey("glyf") && tables.containsKey("loca")) {
            this.loca = readLoca(head);
            this.cff = null;
        } else if (cffTable != null || cff2Table != null) {
            this.loca = new int[0];
            this.cff = CffOutlines.parse(cffTable, cff2Table);
        } else {
            throw new IllegalArgumentException("SFNT has neither glyf/loca nor CFF/CFF2");
        }
        this.gsub = GsubSubstitutions.parse(findTable("GSUB"));
        this.gpos = GposPositioning.parse(findTable("GPOS"), findTable("kern"));
    }

    /// Returns the retained font file.
    ///
    /// @return a read-only file image
    public MemorySegment bytes() {
        return data;
    }

    /// Returns units per em.
    ///
    /// @return the em size
    public int unitsPerEm() {
        return unitsPerEm;
    }

    /// Returns whether `cmap` maps `codePoint` to a nonzero glyph.
    ///
    /// Glyph `0` is `.notdef`. A missing mapping does not search another face.
    ///
    /// @param codePoint the code point
    /// @return whether this face covers the code point
    public boolean hasGlyph(int codePoint) {
        return glyphId(codePoint) != 0;
    }

    /// Maps a Unicode code point through `cmap`.
    ///
    /// @param codePoint the code point
    /// @return the glyph id, or `0`
    public int glyphId(int codePoint) {
        int format4 = glyphIdFormat4(codePoint);
        if (format4 != 0) {
            return format4;
        }
        return glyphIdFormat12(codePoint);
    }

    /// Maps through the format-4 table.
    private int glyphIdFormat4(int codePoint) {
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

    /// Maps through the format-12 table.
    private int glyphIdFormat12(int codePoint) {
        for (int index = 0; index < cmap12End.length; index++) {
            if (codePoint >= cmap12Start[index] && codePoint <= cmap12End[index]) {
                long glyphId = (long) cmap12Glyph[index] + (long) (codePoint - cmap12Start[index]);
                if (glyphId <= 0L || glyphId > 0xFFFFL) {
                    return 0;
                }
                return (int) glyphId;
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

    /// Returns whether this face stores TrueType `glyf` outlines.
    ///
    /// @return whether `glyf`/`loca` are present
    public boolean hasTrueTypeOutlines() {
        return cff == null;
    }

    /// Returns whether this face stores CFF2 rather than CFF 1.
    ///
    /// @return whether the `CFF2` table supplied the outlines
    public boolean hasCff2Outlines() {
        return cff != null && cff.isCff2();
    }

    /// Walks the outline for `glyphId` into `pen`.
    ///
    /// Empty glyphs emit no commands. TrueType simple contours include implied on-curve midpoints
    /// as untruncated averages, and composites expand up to 16 nested components. CFF/CFF2 glyphs
    /// emit Type 2 lines and cubics; hints are skipped. Coordinates are font units with y upward.
    ///
    /// @param glyphId the glyph identity
    /// @param pen the destination
    public void outline(int glyphId, OutlinePen pen) {
        if (cff != null) {
            cff.outline(glyphId, pen);
            return;
        }
        OutlineWalker.walk(this, glyphId, pen, 0);
    }

    /// Applies GSUB single substitutions listed by `featureTag`.
    ///
    /// Lookups other than type 1 are skipped. A missing GSUB table or feature returns `glyphId`.
    ///
    /// @param glyphId the input glyph
    /// @param featureTag a four-byte OpenType tag as a big-endian `int`
    /// @return the substituted glyph, or `glyphId`
    public int substitute(int glyphId, int featureTag) {
        return gsub.apply(glyphId, featureTag);
    }

    /// Returns the GPOS/`kern` X-advance delta for the consecutive pair `(left, right)`.
    ///
    /// A missing pair or missing table returns `0`. The delta is in font units and may be negative.
    ///
    /// @param left the first glyph
    /// @param right the second glyph
    /// @return the signed X-advance adjustment applied to `left`
    public int pairAdjustment(int left, int right) {
        return gpos.pairAdjustment(left, right);
    }

    /// Returns whether `glyphId` is covered by a GPOS mark table.
    ///
    /// @param glyphId the glyph
    /// @return whether the glyph is a mark
    public boolean isMark(int glyphId) {
        return gpos.isMark(glyphId);
    }

    /// Returns the mark-to-base placement for `(markGlyph, baseGlyph)`.
    ///
    /// @param markGlyph the mark glyph
    /// @param baseGlyph the base glyph
    /// @return the placement, or `null` when uncovered
    public @Nullable MarkPlacement markPlacement(int markGlyph, int baseGlyph) {
        return gpos.markPlacement(markGlyph, baseGlyph);
    }

    /// Returns a big-endian glyf cursor, empty for a space or `.notdef` with no outline.
    ///
    /// @param glyphId the glyph id
    /// @return the glyf cursor
    public ByteBuffer glyf(int glyphId) {
        if (glyphId < 0 || glyphId + 1 >= loca.length) {
            throw new IllegalArgumentException("Unknown glyph " + glyphId);
        }
        int start = loca[glyphId];
        int end = loca[glyphId + 1];
        if (end < start) {
            throw new IllegalArgumentException("glyf loca range is inverted");
        }
        TableRecord glyf = requireTable("glyf");
        return cursor(data.asSlice((long) glyf.offset + (long) start, (long) end - (long) start));
    }

    /// Returns a big-endian table cursor.
    ///
    /// @param tag the table tag
    /// @return the table
    private ByteBuffer table(String tag) {
        TableRecord record = requireTable(tag);
        return cursor(data.asSlice(record.offset, record.length));
    }

    /// Returns a table cursor when `tag` is present.
    ///
    /// @param tag the table tag
    /// @return the table, or `null`
    private @Nullable ByteBuffer findTable(String tag) {
        @Nullable TableRecord record = tables.get(tag);
        if (record == null) {
            return null;
        }
        return cursor(data.asSlice(record.offset, record.length));
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

    /// Reads Unicode format-4 and format-12 cmap subtables.
    ///
    /// @return the parsed tables; a missing format is empty
    private CmapTables readCmap() {
        ByteBuffer cmap = table("cmap");
        if (cmap.remaining() < 4) {
            throw new IllegalArgumentException("cmap is truncated");
        }
        cmap.getShort();
        int records = Short.toUnsignedInt(cmap.getShort());
        int format4 = -1;
        int format4Score = -1;
        int format12 = -1;
        int format12Score = -1;
        for (int index = 0; index < records; index++) {
            int platform = Short.toUnsignedInt(cmap.getShort());
            int encoding = Short.toUnsignedInt(cmap.getShort());
            int offset = cmap.getInt();
            if (platform != 0 && platform != 3) {
                continue;
            }
            if (offset < 0 || offset + 2 > cmap.limit()) {
                continue;
            }
            int mark = cmap.position();
            cmap.position(offset);
            int format = Short.toUnsignedInt(cmap.getShort());
            cmap.position(mark);
            int candidate = platform == 3 && (encoding == 1 || encoding == 10) ? 3 : platform == 0 ? 2 : 1;
            if (format == 4 && candidate > format4Score) {
                format4Score = candidate;
                format4 = offset;
            } else if (format == 12 && candidate > format12Score) {
                format12Score = candidate;
                format12 = offset;
            }
        }
        if (format4 < 0 && format12 < 0) {
            throw new IllegalArgumentException("cmap has no Unicode format-4 or format-12 record");
        }
        return new CmapTables(
                format4 < 0 ? CmapFormat4.empty() : parseFormat4(cmap, format4),
                format12 < 0 ? CmapFormat12.empty() : parseFormat12(cmap, format12)
        );
    }

    /// Parses a format-4 subtable at `offset`.
    ///
    /// @param cmap the cmap table
    /// @param offset the subtable offset
    /// @return the segments
    private static CmapFormat4 parseFormat4(ByteBuffer cmap, int offset) {
        cmap.position(offset);
        cmap.getShort();
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

    /// Parses a format-12 subtable at `offset`.
    ///
    /// @param cmap the cmap table
    /// @param offset the subtable offset
    /// @return the sequential map groups
    private static CmapFormat12 parseFormat12(ByteBuffer cmap, int offset) {
        cmap.position(offset);
        if (cmap.remaining() < 16) {
            throw new IllegalArgumentException("cmap format 12 is truncated");
        }
        cmap.getShort();
        cmap.getShort();
        cmap.getInt();
        cmap.getInt();
        int groups = cmap.getInt();
        if (groups < 0 || cmap.remaining() < groups * 12) {
            throw new IllegalArgumentException("cmap format 12 groups are truncated");
        }
        int[] startCodes = new int[groups];
        int[] endCodes = new int[groups];
        int[] startGlyphs = new int[groups];
        for (int index = 0; index < groups; index++) {
            startCodes[index] = cmap.getInt();
            endCodes[index] = cmap.getInt();
            startGlyphs[index] = cmap.getInt();
            if (Integer.compareUnsigned(endCodes[index], startCodes[index]) < 0) {
                throw new IllegalArgumentException("cmap format 12 group is inverted");
            }
        }
        return new CmapFormat12(startCodes, endCodes, startGlyphs);
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

    /// Returns a big-endian cursor over `segment`.
    ///
    /// @param segment the slice
    /// @return the cursor
    private static ByteBuffer cursor(MemorySegment segment) {
        return segment.asByteBuffer().order(ByteOrder.BIG_ENDIAN);
    }

    /// Stores one table directory record.
    ///
    /// @param offset the file offset
    /// @param length the length
    private record TableRecord(int offset, int length) {
    }

    /// Stores parsed Unicode cmap subtables.
    ///
    /// @param format4 the format-4 segments
    /// @param format12 the format-12 groups
    private record CmapTables(CmapFormat4 format4, CmapFormat12 format12) {
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
        /// Returns an empty format-4 table.
        ///
        /// @return empty segments
        private static CmapFormat4 empty() {
            return new CmapFormat4(new int[0], new int[0], new short[0], new int[0], new int[0]);
        }
    }

    /// Stores a parsed format-12 cmap.
    ///
    /// @param startCodes start code points
    /// @param endCodes end code points
    /// @param startGlyphs start glyph ids
    private record CmapFormat12(int[] startCodes, int[] endCodes, int[] startGlyphs) {
        /// Returns an empty format-12 table.
        ///
        /// @return empty groups
        private static CmapFormat12 empty() {
            return new CmapFormat12(new int[0], new int[0], new int[0]);
        }
    }
}

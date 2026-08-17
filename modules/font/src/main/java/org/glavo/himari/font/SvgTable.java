package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/// Reads an OpenType `SVG ` table of uncompressed SVG documents.
///
/// First-stable accepts version 0, uncompressed UTF-8 documents, and the first matching
/// glyph-range record. Gzip-compressed documents (`1F 8B`) are rejected. A missing table
/// yields no documents.
@NotNullByDefault
final class SvgTable {
    /// Shared empty table.
    static final SvgTable EMPTY = new SvgTable(new int[0], new int[0], new String[0]);

    /// Inclusive start glyph ids.
    private final int[] startGlyphs;

    /// Inclusive end glyph ids.
    private final int[] endGlyphs;

    /// Document text parallel to the range arrays.
    private final String[] documents;

    /// Creates a table.
    ///
    /// @param startGlyphs the range starts
    /// @param endGlyphs the range ends
    /// @param documents the SVG documents
    private SvgTable(int[] startGlyphs, int[] endGlyphs, String[] documents) {
        this.startGlyphs = startGlyphs;
        this.endGlyphs = endGlyphs;
        this.documents = documents;
    }

    /// Parses an `SVG ` table, or returns [`#EMPTY`].
    ///
    /// @param table the table bytes, or `null`
    /// @return the table
    static SvgTable parse(@Nullable ByteBuffer table) {
        if (table == null || table.remaining() < 10) {
            return EMPTY;
        }
        ByteBuffer buffer = table.duplicate().order(ByteOrder.BIG_ENDIAN);
        int version = Short.toUnsignedInt(buffer.getShort());
        if (version != 0) {
            return EMPTY;
        }
        int indexOffset = buffer.getInt();
        if (indexOffset < 0 || indexOffset + 2 > table.capacity()) {
            return EMPTY;
        }
        buffer.position(indexOffset);
        int count = Short.toUnsignedInt(buffer.getShort());
        if (count < 1 || buffer.remaining() < count * 12L) {
            return EMPTY;
        }
        int[] starts = new int[count];
        int[] ends = new int[count];
        String[] documents = new String[count];
        byte[] whole = new byte[table.capacity()];
        table.duplicate().order(ByteOrder.BIG_ENDIAN).rewind().get(whole);
        for (int index = 0; index < count; index++) {
            starts[index] = Short.toUnsignedInt(buffer.getShort());
            ends[index] = Short.toUnsignedInt(buffer.getShort());
            int relativeOffset = buffer.getInt();
            int length = buffer.getInt();
            long absolute = (long) indexOffset + (long) relativeOffset;
            if (relativeOffset < 0 || length < 1 || absolute + (long) length > whole.length) {
                return EMPTY;
            }
            int offset = (int) absolute;
            if ((whole[offset] & 0xFF) == 0x1F && length > 1 && (whole[offset + 1] & 0xFF) == 0x8B) {
                throw new IllegalArgumentException("Compressed SVG documents are not accepted");
            }
            documents[index] = new String(whole, offset, length, StandardCharsets.UTF_8);
        }
        return new SvgTable(starts, ends, documents);
    }

    /// Returns the first SVG document that covers `glyphId`.
    ///
    /// @param glyphId the glyph
    /// @return the document, or `null` when none matches
    @Nullable String document(int glyphId) {
        for (int index = 0; index < startGlyphs.length; index++) {
            if (glyphId >= startGlyphs[index] && glyphId <= endGlyphs[index]) {
                return documents[index];
            }
        }
        return null;
    }
}

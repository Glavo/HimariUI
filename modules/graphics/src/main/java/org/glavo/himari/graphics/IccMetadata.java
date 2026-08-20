package org.glavo.himari.graphics;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/// Parses a bounded ICC `dictType` (`meta`) metadata table.
///
/// Profiles are untrusted input. Names and values are UTF-16BE strings without a BOM.
/// An optional display name is the first `mluc` record, or `null` when the display
/// offset is zero. Unpaired surrogates are rejected.
///
/// @param entries the dictionary records in table order
@NotNullByDefault
public record IccMetadata(@Unmodifiable List<Entry> entries) {
    /// Tag `'meta'`.
    public static final int SIGNATURE = 0x6D65_7461;

    /// Type `'dict'`.
    public static final int TYPE_DICT = 0x6469_6374;

    /// Type `'mluc'`.
    public static final int TYPE_MLUC = 0x6D6C_7563;

    /// Maximum accepted dictionary entries.
    public static final int MAX_ENTRIES = 32;

    /// Maximum accepted UTF-16 code units per string.
    public static final int MAX_CHARS = 256;

    /// Bytes in the tag header before the first directory entry.
    private static final int HEADER_BYTES = 12;

    /// Bytes in one dictionary directory entry.
    private static final int ENTRY_BYTES = 24;

    /// Required `mluc` record size.
    private static final int MLUC_RECORD_BYTES = 12;

    /// Validates and copies the records.
    public IccMetadata {
        Objects.requireNonNull(entries, "entries");
        if (entries.size() > MAX_ENTRIES) {
            throw new IllegalArgumentException("ICC metadata entry count exceeds the accepted bound");
        }
        entries = List.copyOf(entries);
        for (Entry entry : entries) {
            Objects.requireNonNull(entry, "entry");
        }
    }

    /// Parses one `meta` tag.
    ///
    /// @param bytes the profile bytes
    /// @param offset the tag start
    /// @param size the tag size
    /// @return the dictionary
    public static IccMetadata parse(byte[] bytes, int offset, int size) {
        Objects.requireNonNull(bytes, "bytes");
        if (offset < 0 || size < HEADER_BYTES || offset > bytes.length - size) {
            throw new IllegalArgumentException("ICC meta tag is outside the profile");
        }
        if (u32(bytes, offset) != TYPE_DICT) {
            throw new IllegalArgumentException("ICC meta tag is not dict");
        }
        int count = u32(bytes, offset + 8);
        if (count < 0 || count > MAX_ENTRIES) {
            throw new IllegalArgumentException("ICC metadata entry count is outside the accepted bounds");
        }
        int directory = HEADER_BYTES + count * ENTRY_BYTES;
        if (size < directory) {
            throw new IllegalArgumentException("ICC meta directory is truncated");
        }
        ArrayList<Entry> entries = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            int cursor = offset + HEADER_BYTES + index * ENTRY_BYTES;
            String name = utf16be(bytes, offset, size, u32(bytes, cursor), u32(bytes, cursor + 4));
            String value = utf16be(bytes, offset, size, u32(bytes, cursor + 8), u32(bytes, cursor + 12));
            int displayOffset = u32(bytes, cursor + 16);
            int displaySize = u32(bytes, cursor + 20);
            String displayName = displayOffset == 0
                    ? null
                    : parseMluc(bytes, offset, size, displayOffset, displaySize);
            entries.add(new Entry(name, value, displayName));
        }
        return new IccMetadata(entries);
    }

    /// Finds the first record whose name equals `name`.
    ///
    /// @param name the dictionary key
    /// @return the record, or `null` when absent
    public @Nullable Entry lookup(String name) {
        Objects.requireNonNull(name, "name");
        for (Entry entry : entries) {
            if (entry.name().equals(name)) {
                return entry;
            }
        }
        return null;
    }

    /// Reads one UTF-16BE string at `relative` from the tag origin.
    private static String utf16be(byte[] bytes, int tagOffset, int tagSize, int relative, int byteCount) {
        if (relative < 0 || byteCount < 0 || (byteCount & 1) != 0 || relative > tagSize - byteCount) {
            throw new IllegalArgumentException("ICC metadata string is outside the tag");
        }
        int units = byteCount / 2;
        if (units > MAX_CHARS) {
            throw new IllegalArgumentException("ICC metadata string exceeds the accepted bound");
        }
        int start = tagOffset + relative;
        StringBuilder text = new StringBuilder(units);
        for (int index = 0; index < units; index++) {
            int unit = u16(bytes, start + index * 2);
            if (unit >= 0xD800 && unit <= 0xDFFF) {
                throw new IllegalArgumentException("ICC metadata strings must not contain unpaired surrogates");
            }
            text.append((char) unit);
        }
        return text.toString();
    }

    /// Reads the first `mluc` record as UTF-16BE.
    private static String parseMluc(byte[] bytes, int tagOffset, int tagSize, int relative, int elementSize) {
        if (relative < 0 || elementSize < 28 || relative > tagSize - elementSize) {
            throw new IllegalArgumentException("ICC metadata display name is outside the tag");
        }
        int cursor = tagOffset + relative;
        if (u32(bytes, cursor) != TYPE_MLUC) {
            throw new IllegalArgumentException("ICC metadata display name is not mluc");
        }
        int records = u32(bytes, cursor + 8);
        int recordSize = u32(bytes, cursor + 12);
        if (records < 1 || recordSize != MLUC_RECORD_BYTES || elementSize < 16 + records * MLUC_RECORD_BYTES) {
            throw new IllegalArgumentException("ICC metadata mluc directory is malformed");
        }
        int stringBytes = u32(bytes, cursor + 20);
        int stringRelative = u32(bytes, cursor + 24);
        if (stringRelative < 0 || stringBytes < 0 || stringRelative > elementSize - stringBytes) {
            throw new IllegalArgumentException("ICC metadata mluc string is outside the element");
        }
        return utf16be(bytes, cursor, elementSize, stringRelative, stringBytes);
    }

    /// Reads a big-endian unsigned 32-bit integer as a signed Java `int`.
    private static int u32(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) << 24)
                | ((bytes[offset + 1] & 0xFF) << 16)
                | ((bytes[offset + 2] & 0xFF) << 8)
                | (bytes[offset + 3] & 0xFF);
    }

    /// Reads a big-endian unsigned 16-bit integer.
    private static int u16(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) << 8) | (bytes[offset + 1] & 0xFF);
    }

    /// One metadata key/value pair.
    ///
    /// @param name the dictionary key
    /// @param value the dictionary value
    /// @param displayName the optional localized display name, or `null`
    public record Entry(String name, String value, @Nullable String displayName) {
        /// Validates the record.
        public Entry {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(value, "value");
        }
    }
}

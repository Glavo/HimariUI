package org.glavo.himari.graphics;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/// Parses a bounded ICC `colorantTableType` (`clrt` or `clot`) table.
///
/// Profiles are untrusted input. The parser accepts at most [`#MAX_COLORANTS`] 7-bit ASCII
/// names and stores PCS XYZ as `uInt16 / 32768`.
///
/// @param entries the colorant records in table order
@NotNullByDefault
public record IccColorants(@Unmodifiable List<Entry> entries) {
    /// Type `'clrt'`.
    public static final int TYPE_CLRT = 0x636C_7274;

    /// Tag `'clrt'`.
    public static final int TAG_CLRT = TYPE_CLRT;

    /// Tag `'clot'`.
    public static final int TAG_CLOT = 0x636C_6F74;

    /// Maximum accepted colorant count.
    public static final int MAX_COLORANTS = 16;

    /// Bytes reserved for one 7-bit ASCII name field.
    private static final int NAME_BYTES = 32;

    /// Bytes in the header before the first colorant record.
    private static final int HEADER_BYTES = 12;

    /// Bytes in one colorant record.
    private static final int RECORD_BYTES = NAME_BYTES + 6;

    /// Validates and copies the records.
    public IccColorants {
        Objects.requireNonNull(entries, "entries");
        if (entries.size() > MAX_COLORANTS) {
            throw new IllegalArgumentException("ICC colorant count exceeds the accepted bound");
        }
        entries = List.copyOf(entries);
        for (Entry entry : entries) {
            Objects.requireNonNull(entry, "entry");
        }
    }

    /// Parses one `clrt` or `clot` tag.
    ///
    /// @param bytes the profile bytes
    /// @param offset the tag start
    /// @param size the tag size
    /// @return the table
    public static IccColorants parse(byte[] bytes, int offset, int size) {
        Objects.requireNonNull(bytes, "bytes");
        if (offset < 0 || size < HEADER_BYTES || offset > bytes.length - size) {
            throw new IllegalArgumentException("ICC colorant tag is outside the profile");
        }
        if (u32(bytes, offset) != TYPE_CLRT) {
            throw new IllegalArgumentException("ICC colorant tag is not clrt");
        }
        int count = u32(bytes, offset + 8);
        if (count < 0 || count > MAX_COLORANTS) {
            throw new IllegalArgumentException("ICC colorant count is outside the accepted bounds");
        }
        long required = (long) HEADER_BYTES + (long) count * RECORD_BYTES;
        if (required > size) {
            throw new IllegalArgumentException("ICC colorant records exceed the tag");
        }
        ArrayList<Entry> entries = new ArrayList<>(count);
        int cursor = offset + HEADER_BYTES;
        for (int index = 0; index < count; index++) {
            String name = ascii32(bytes, cursor);
            float pcsX = u16(bytes, cursor + NAME_BYTES) / 32768.0f;
            float pcsY = u16(bytes, cursor + NAME_BYTES + 2) / 32768.0f;
            float pcsZ = u16(bytes, cursor + NAME_BYTES + 4) / 32768.0f;
            entries.add(new Entry(name, pcsX, pcsY, pcsZ));
            cursor += RECORD_BYTES;
        }
        return new IccColorants(entries);
    }

    /// Finds the first record whose name equals `name`.
    ///
    /// @param name the colorant name
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

    /// Reads a 32-byte 7-bit ASCII field up to the first NUL.
    private static String ascii32(byte[] bytes, int offset) {
        int length = 0;
        while (length < NAME_BYTES) {
            int value = bytes[offset + length] & 0xFF;
            if (value == 0) {
                break;
            }
            if (value > 0x7F) {
                throw new IllegalArgumentException("ICC colorant names must be 7-bit ASCII");
            }
            length++;
        }
        return new String(bytes, offset, length, StandardCharsets.US_ASCII);
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

    /// One colorant with a display name and PCS XYZ.
    ///
    /// @param name the 7-bit ASCII colorant name
    /// @param pcsX the PCS X, where `1` is encoded as `32768`
    /// @param pcsY the PCS Y
    /// @param pcsZ the PCS Z
    public record Entry(String name, float pcsX, float pcsY, float pcsZ) {
        /// Validates the record.
        public Entry {
            Objects.requireNonNull(name, "name");
            if (!Float.isFinite(pcsX) || !Float.isFinite(pcsY) || !Float.isFinite(pcsZ)) {
                throw new IllegalArgumentException("ICC colorant PCS coordinates must be finite");
            }
        }
    }
}

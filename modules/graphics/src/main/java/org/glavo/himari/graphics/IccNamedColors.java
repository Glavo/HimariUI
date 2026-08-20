package org.glavo.himari.graphics;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/// Parses a bounded ICC `namedColor2Type` (`ncl2`) or v2 `namedColorType` (`ncl `) table.
///
/// Profiles are untrusted input. The parser accepts at most
/// [`#MAX_COLORS`] root names, stores device coordinates after the PCS triple as
/// `uInt16 / 65535`, and rejects non-ASCII name bytes.
@NotNullByDefault
public final class IccNamedColors {
    /// Maximum accepted named-color count.
    public static final int MAX_COLORS = 256;

    /// Maximum accepted device-coordinate count per color.
    public static final int MAX_DEVICE_COORDS = 16;

    /// Bytes reserved for one 7-bit ASCII name field.
    private static final int NAME_BYTES = 32;

    /// Type `'ncl2'`.
    private static final int TYPE_NCL2 = 0x6E63_6C32;

    /// Type `'ncl '`.
    private static final int TYPE_NCL = 0x6E63_6C20;

    /// Bytes in the v2 `ncl ` header before the first color record.
    private static final int NCL_HEADER_BYTES = 16;

    /// Bytes in one v2 `ncl ` record with no device coordinates.
    private static final int NCL_RECORD_BYTES = NAME_BYTES + 6;

    /// Bytes in the `ncl2` header before the first color record.
    private static final int HEADER_BYTES = 84;

    /// Prefix prepended to each root name for display.
    private final String prefix;

    /// Suffix appended to each root name for display.
    private final String suffix;

    /// Named-color records in table order.
    private final @Unmodifiable List<Entry> entries;

    /// Creates a validated table.
    ///
    /// @param prefix the name prefix
    /// @param suffix the name suffix
    /// @param entries the color records
    public IccNamedColors(String prefix, String suffix, List<Entry> entries) {
        this.prefix = Objects.requireNonNull(prefix, "prefix");
        this.suffix = Objects.requireNonNull(suffix, "suffix");
        Objects.requireNonNull(entries, "entries");
        if (entries.size() > MAX_COLORS) {
            throw new IllegalArgumentException("Named-color count exceeds the accepted bound");
        }
        this.entries = List.copyOf(entries);
        for (Entry entry : this.entries) {
            Objects.requireNonNull(entry, "entry");
        }
    }

    /// Parses one `ncl2` tag.
    ///
    /// PCS XYZ coordinates use the ICC `uInt16Number` encoding where `32768` is `1.0`.
    ///
    /// @param bytes the profile bytes
    /// @param offset the tag start
    /// @param size the tag size
    /// @return the table
    public static IccNamedColors parse(byte[] bytes, int offset, int size) {
        Objects.requireNonNull(bytes, "bytes");
        if (offset < 0 || size < 4 || offset > bytes.length - size) {
            throw new IllegalArgumentException("ICC named-color tag is outside the profile");
        }
        int type = u32(bytes, offset);
        if (type == TYPE_NCL) {
            return parseNcl(bytes, offset, size);
        }
        if (type != TYPE_NCL2) {
            throw new IllegalArgumentException("ICC named-color tag is not ncl2 or ncl");
        }
        if (size < HEADER_BYTES) {
            throw new IllegalArgumentException("ICC ncl2 tag is truncated");
        }
        int count = u32(bytes, offset + 12);
        int deviceCoords = u32(bytes, offset + 16);
        if (count < 0 || count > MAX_COLORS) {
            throw new IllegalArgumentException("ICC named-color count is outside the accepted bounds");
        }
        if (deviceCoords < 0 || deviceCoords > MAX_DEVICE_COORDS) {
            throw new IllegalArgumentException("ICC named-color device count is outside the accepted bounds");
        }
        int recordBytes = NAME_BYTES + 6 + deviceCoords * 2;
        long required = (long) HEADER_BYTES + (long) count * recordBytes;
        if (required > size) {
            throw new IllegalArgumentException("ICC ncl2 records exceed the tag");
        }
        String prefix = ascii32(bytes, offset + 20);
        String suffix = ascii32(bytes, offset + 52);
        ArrayList<Entry> entries = new ArrayList<>(count);
        int cursor = offset + HEADER_BYTES;
        for (int index = 0; index < count; index++) {
            String root = ascii32(bytes, cursor);
            float pcsX = u16(bytes, cursor + NAME_BYTES) / 32768.0f;
            float pcsY = u16(bytes, cursor + NAME_BYTES + 2) / 32768.0f;
            float pcsZ = u16(bytes, cursor + NAME_BYTES + 4) / 32768.0f;
            float[] device = new float[deviceCoords];
            int deviceStart = cursor + NAME_BYTES + 6;
            for (int coord = 0; coord < deviceCoords; coord++) {
                device[coord] = u16(bytes, deviceStart + coord * 2) / 65535.0f;
            }
            entries.add(new Entry(root, pcsX, pcsY, pcsZ, device));
            cursor += recordBytes;
        }
        return new IccNamedColors(prefix, suffix, entries);
    }

    /// Parses the older `namedColorType` (`ncl `) with no prefix/suffix and no device coordinates.
    private static IccNamedColors parseNcl(byte[] bytes, int offset, int size) {
        if (size < NCL_HEADER_BYTES) {
            throw new IllegalArgumentException("ICC ncl tag is truncated");
        }
        int count = u32(bytes, offset + 12);
        if (count < 0 || count > MAX_COLORS) {
            throw new IllegalArgumentException("ICC named-color count is outside the accepted bounds");
        }
        long required = (long) NCL_HEADER_BYTES + (long) count * NCL_RECORD_BYTES;
        if (required > size) {
            throw new IllegalArgumentException("ICC ncl records exceed the tag");
        }
        ArrayList<Entry> entries = new ArrayList<>(count);
        int cursor = offset + NCL_HEADER_BYTES;
        for (int index = 0; index < count; index++) {
            String root = ascii32(bytes, cursor);
            float pcsX = u16(bytes, cursor + NAME_BYTES) / 32768.0f;
            float pcsY = u16(bytes, cursor + NAME_BYTES + 2) / 32768.0f;
            float pcsZ = u16(bytes, cursor + NAME_BYTES + 4) / 32768.0f;
            entries.add(new Entry(root, pcsX, pcsY, pcsZ, new float[0]));
            cursor += NCL_RECORD_BYTES;
        }
        return new IccNamedColors("", "", entries);
    }

    /// Returns the name prefix.
    ///
    /// @return the prefix, possibly empty
    public String prefix() {
        return prefix;
    }

    /// Returns the name suffix.
    ///
    /// @return the suffix, possibly empty
    public String suffix() {
        return suffix;
    }

    /// Returns the color records in table order.
    ///
    /// @return the records
    public @Unmodifiable List<Entry> entries() {
        return entries;
    }

    /// Finds the first record whose root name equals `rootName`.
    ///
    /// @param rootName the root name
    /// @return the record, or `null` when absent
    public @Nullable Entry lookup(String rootName) {
        Objects.requireNonNull(rootName, "rootName");
        for (Entry entry : entries) {
            if (entry.rootName().equals(rootName)) {
                return entry;
            }
        }
        return null;
    }

    /// Reads a 32-byte 7-bit ASCII field up to the first NUL.
    ///
    /// @param bytes the profile bytes
    /// @param offset the field start
    /// @return the decoded string
    private static String ascii32(byte[] bytes, int offset) {
        int length = 0;
        while (length < NAME_BYTES) {
            int value = bytes[offset + length] & 0xFF;
            if (value == 0) {
                break;
            }
            if (value > 0x7F) {
                throw new IllegalArgumentException("ICC named-color names must be 7-bit ASCII");
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

    /// One named color with PCS XYZ and optional device coordinates.
    ///
    /// @param rootName the 7-bit ASCII root name
    /// @param pcsX the PCS X, where `1` is encoded as `32768`
    /// @param pcsY the PCS Y
    /// @param pcsZ the PCS Z
    /// @param device device coordinates in `[0, 1]`, empty when the table stores none
    public record Entry(
            String rootName,
            float pcsX,
            float pcsY,
            float pcsZ,
            float @Unmodifiable [] device
    ) {
        /// Validates the record.
        public Entry {
            Objects.requireNonNull(rootName, "rootName");
            Objects.requireNonNull(device, "device");
            if (!Float.isFinite(pcsX) || !Float.isFinite(pcsY) || !Float.isFinite(pcsZ)) {
                throw new IllegalArgumentException("Named-color PCS coordinates must be finite");
            }
            if (device.length > MAX_DEVICE_COORDS) {
                throw new IllegalArgumentException("Named-color device count exceeds the accepted bound");
            }
            for (float value : device) {
                if (!Float.isFinite(value)) {
                    throw new IllegalArgumentException("Named-color device coordinates must be finite");
                }
            }
            device = device.clone();
        }
    }
}

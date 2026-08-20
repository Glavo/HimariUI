package org.glavo.himari.graphics;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/// Parses a bounded ICC `profileSequenceIdentifierType` (`psid`) table.
///
/// Profiles are untrusted input. Each record stores a 16-byte profile ID and an ASCII
/// `desc` description. Position-table offsets are relative to the start of the tag.
///
/// @param entries the identifier records in table order
@NotNullByDefault
public record IccProfileSequenceIds(@Unmodifiable List<Entry> entries) {
    /// Type and tag `'psid'`.
    public static final int SIGNATURE = 0x7073_6964;

    /// Bytes in one ICC profile ID.
    public static final int PROFILE_ID_BYTES = 16;

    /// Maximum accepted identifier count.
    public static final int MAX_ENTRIES = 16;

    /// Bytes in the tag header before the position table.
    private static final int HEADER_BYTES = 12;

    /// Bytes in one position-table entry.
    private static final int POSITION_BYTES = 8;

    /// Validates and copies the records.
    public IccProfileSequenceIds {
        Objects.requireNonNull(entries, "entries");
        if (entries.size() > MAX_ENTRIES) {
            throw new IllegalArgumentException("ICC profile-sequence identifier count exceeds the accepted bound");
        }
        entries = List.copyOf(entries);
        for (Entry entry : entries) {
            Objects.requireNonNull(entry, "entry");
        }
    }

    /// Parses one `psid` tag.
    ///
    /// @param bytes the profile bytes
    /// @param offset the tag start
    /// @param size the tag size
    /// @return the identifier table
    public static IccProfileSequenceIds parse(byte[] bytes, int offset, int size) {
        Objects.requireNonNull(bytes, "bytes");
        if (offset < 0 || size < HEADER_BYTES || offset > bytes.length - size) {
            throw new IllegalArgumentException("ICC psid tag is outside the profile");
        }
        if (u32(bytes, offset) != SIGNATURE) {
            throw new IllegalArgumentException("ICC profile-sequence identifier tag is not psid");
        }
        int count = u32(bytes, offset + 8);
        if (count < 0 || count > MAX_ENTRIES) {
            throw new IllegalArgumentException("ICC profile-sequence identifier count is outside the accepted bounds");
        }
        int directory = HEADER_BYTES + count * POSITION_BYTES;
        if (size < directory) {
            throw new IllegalArgumentException("ICC psid position table is truncated");
        }
        ArrayList<Entry> entries = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            int relative = u32(bytes, offset + HEADER_BYTES + index * POSITION_BYTES);
            int recordSize = u32(bytes, offset + HEADER_BYTES + index * POSITION_BYTES + 4);
            if (relative < directory || recordSize < PROFILE_ID_BYTES + 12 || relative > size - recordSize) {
                throw new IllegalArgumentException("ICC psid record is outside the tag");
            }
            byte[] profileId = Arrays.copyOfRange(bytes, offset + relative, offset + relative + PROFILE_ID_BYTES);
            IccProfileText.Embedded description = IccProfileText.parseEmbedded(
                    bytes,
                    offset + relative + PROFILE_ID_BYTES,
                    recordSize - PROFILE_ID_BYTES
            );
            entries.add(new Entry(profileId, description.text()));
        }
        return new IccProfileSequenceIds(entries);
    }

    /// Reads a big-endian unsigned 32-bit integer as a signed Java `int`.
    private static int u32(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) << 24)
                | ((bytes[offset + 1] & 0xFF) << 16)
                | ((bytes[offset + 2] & 0xFF) << 8)
                | (bytes[offset + 3] & 0xFF);
    }

    /// One profile identifier in a device-link sequence.
    ///
    /// @param profileId the 16-byte profile ID
    /// @param description the ASCII profile description
    public record Entry(byte @Unmodifiable [] profileId, String description) {
        /// Validates and copies the identifier.
        public Entry {
            Objects.requireNonNull(profileId, "profileId");
            Objects.requireNonNull(description, "description");
            if (profileId.length != PROFILE_ID_BYTES) {
                throw new IllegalArgumentException("ICC profile ID must be 16 bytes");
            }
            profileId = Arrays.copyOf(profileId, PROFILE_ID_BYTES);
        }

        /// Returns the profile ID as lowercase hexadecimal.
        ///
        /// @return 32 hex digits
        public String profileIdHex() {
            return HexFormat.of().formatHex(profileId);
        }
    }
}

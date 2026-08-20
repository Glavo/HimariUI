package org.glavo.himari.graphics;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/// Parses a bounded ICC `profileSequenceDescType` (`pseq`) table.
///
/// Profiles are untrusted input. Each record stores four-character manufacturer, model,
/// and technology signatures plus the ASCII `desc` manufacturer and model names. The
/// eight-byte device attributes are stored as two unsigned 32-bit words.
///
/// @param entries the sequence records in table order
@NotNullByDefault
public record IccProfileSequence(@Unmodifiable List<Entry> entries) {
    /// Type and tag `'pseq'`.
    public static final int SIGNATURE = 0x7073_6571;

    /// Maximum accepted sequence length.
    public static final int MAX_ENTRIES = 16;

    /// Bytes in the tag header before the first record.
    private static final int HEADER_BYTES = 12;

    /// Bytes of signatures and attributes before the two descriptions.
    private static final int RECORD_HEADER_BYTES = 20;

    /// Validates and copies the records.
    public IccProfileSequence {
        Objects.requireNonNull(entries, "entries");
        if (entries.size() > MAX_ENTRIES) {
            throw new IllegalArgumentException("ICC profile-sequence count exceeds the accepted bound");
        }
        entries = List.copyOf(entries);
        for (Entry entry : entries) {
            Objects.requireNonNull(entry, "entry");
        }
    }

    /// Parses one `pseq` tag.
    ///
    /// @param bytes the profile bytes
    /// @param offset the tag start
    /// @param size the tag size
    /// @return the sequence
    public static IccProfileSequence parse(byte[] bytes, int offset, int size) {
        Objects.requireNonNull(bytes, "bytes");
        if (offset < 0 || size < HEADER_BYTES || offset > bytes.length - size) {
            throw new IllegalArgumentException("ICC pseq tag is outside the profile");
        }
        if (u32(bytes, offset) != SIGNATURE) {
            throw new IllegalArgumentException("ICC profile-sequence tag is not pseq");
        }
        int count = u32(bytes, offset + 8);
        if (count < 0 || count > MAX_ENTRIES) {
            throw new IllegalArgumentException("ICC profile-sequence count is outside the accepted bounds");
        }
        ArrayList<Entry> entries = new ArrayList<>(count);
        int cursor = HEADER_BYTES;
        for (int index = 0; index < count; index++) {
            if (cursor + RECORD_HEADER_BYTES > size) {
                throw new IllegalArgumentException("ICC pseq record is truncated");
            }
            int record = offset + cursor;
            String manufacturer = signature(bytes, record);
            String model = signature(bytes, record + 4);
            int attributesHi = u32(bytes, record + 8);
            int attributesLo = u32(bytes, record + 12);
            String technology = signature(bytes, record + 16);
            cursor += RECORD_HEADER_BYTES;
            IccProfileText.Embedded manufacturerDesc = IccProfileText.parseEmbedded(
                    bytes,
                    offset + cursor,
                    size - cursor
            );
            cursor += manufacturerDesc.size();
            IccProfileText.Embedded modelDesc = IccProfileText.parseEmbedded(
                    bytes,
                    offset + cursor,
                    size - cursor
            );
            cursor += modelDesc.size();
            entries.add(new Entry(
                    manufacturer,
                    model,
                    attributesHi,
                    attributesLo,
                    technology,
                    manufacturerDesc.text(),
                    modelDesc.text()
            ));
        }
        return new IccProfileSequence(entries);
    }

    /// Reads a 4-character signature.
    private static String signature(byte[] bytes, int offset) {
        return new String(bytes, offset, 4, StandardCharsets.US_ASCII);
    }

    /// Reads a big-endian unsigned 32-bit integer as a signed Java `int`.
    private static int u32(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) << 24)
                | ((bytes[offset + 1] & 0xFF) << 16)
                | ((bytes[offset + 2] & 0xFF) << 8)
                | (bytes[offset + 3] & 0xFF);
    }

    /// One profile in a device-link sequence.
    ///
    /// @param manufacturer the 4-character device-manufacturer signature
    /// @param model the 4-character device-model signature
    /// @param attributesHi the high 32 bits of the device attributes
    /// @param attributesLo the low 32 bits of the device attributes
    /// @param technology the 4-character technology signature
    /// @param manufacturerDescription the ASCII manufacturer description
    /// @param modelDescription the ASCII model description
    public record Entry(
            String manufacturer,
            String model,
            int attributesHi,
            int attributesLo,
            String technology,
            String manufacturerDescription,
            String modelDescription
    ) {
        /// Validates the record.
        public Entry {
            Objects.requireNonNull(manufacturer, "manufacturer");
            Objects.requireNonNull(model, "model");
            Objects.requireNonNull(technology, "technology");
            Objects.requireNonNull(manufacturerDescription, "manufacturerDescription");
            Objects.requireNonNull(modelDescription, "modelDescription");
            if (manufacturer.length() != 4 || model.length() != 4 || technology.length() != 4) {
                throw new IllegalArgumentException("ICC profile-sequence signatures must be four characters");
            }
        }
    }
}

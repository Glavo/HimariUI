package org.glavo.himari.graphics;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

/// Parses a bounded ICC `crdInfoType` (`crdi`) PostScript CRD companion table.
///
/// Profiles are untrusted input. The product name and four rendering-intent CRD names are
/// counted 7-bit ASCII strings. A zero count stores `null` for that companion name.
///
/// @param productName the PostScript product name
/// @param crdNames four optional CRD names, one per rendering intent
@NotNullByDefault
public record IccCrdInfo(String productName, String @Nullable @Unmodifiable [] crdNames) {
    /// Type `'crdi'`.
    public static final int TYPE_CRDI = 0x6372_6469;

    /// Rendering-intent count.
    public static final int INTENT_COUNT = 4;

    /// Maximum accepted ASCII character count, including the terminating NUL.
    public static final int MAX_ASCII = 256;

    /// Bytes in the tag header before the first counted string.
    private static final int HEADER_BYTES = 8;

    /// Validates the product name and the four companion slots.
    public IccCrdInfo {
        Objects.requireNonNull(productName, "productName");
        Objects.requireNonNull(crdNames, "crdNames");
        if (crdNames.length != INTENT_COUNT) {
            throw new IllegalArgumentException("ICC crdInfo must store four rendering-intent names");
        }
        crdNames = Arrays.copyOf(crdNames, INTENT_COUNT);
    }

    /// Parses one `crdInfoType` tag.
    ///
    /// @param bytes the profile bytes
    /// @param offset the tag start
    /// @param size the tag size
    /// @return the CRD info
    public static IccCrdInfo parse(byte[] bytes, int offset, int size) {
        Objects.requireNonNull(bytes, "bytes");
        if (offset < 0 || size < HEADER_BYTES + 4 || offset > bytes.length - size) {
            throw new IllegalArgumentException("ICC crdi info tag is outside the profile");
        }
        if (u32(bytes, offset) != TYPE_CRDI) {
            throw new IllegalArgumentException("ICC crdInfo tag is not crdi");
        }
        int cursor = HEADER_BYTES;
        CountedString product = readCounted(bytes, offset, size, cursor);
        cursor += product.consumed;
        String[] names = new String[INTENT_COUNT];
        for (int intent = 0; intent < INTENT_COUNT; intent++) {
            CountedString crd = readCounted(bytes, offset, size, cursor);
            names[intent] = crd.text;
            cursor += crd.consumed;
        }
        return new IccCrdInfo(product.text == null ? "" : product.text, names);
    }

    /// Reads one counted ASCII field at `relative` from the tag origin.
    private static CountedString readCounted(byte[] bytes, int tagOffset, int size, int relative) {
        if (relative + 4 > size) {
            throw new IllegalArgumentException("ICC crdInfo string count is truncated");
        }
        int count = u32(bytes, tagOffset + relative);
        if (count < 0 || count > MAX_ASCII) {
            throw new IllegalArgumentException("ICC crdInfo string count is outside the accepted bounds");
        }
        if (count == 0) {
            return new CountedString(null, 4);
        }
        if (relative + 4 + count > size) {
            throw new IllegalArgumentException("ICC crdInfo string exceeds the tag");
        }
        int length = count;
        int start = tagOffset + relative + 4;
        if (bytes[start + count - 1] == 0) {
            length = count - 1;
        }
        for (int index = 0; index < length; index++) {
            int value = bytes[start + index] & 0xFF;
            if (value == 0 || value > 0x7F) {
                throw new IllegalArgumentException("ICC crdInfo strings must be 7-bit ASCII with no interior NUL");
            }
        }
        return new CountedString(new String(bytes, start, length, StandardCharsets.US_ASCII), 4 + count);
    }

    /// Reads a big-endian unsigned 32-bit integer as a signed Java `int`.
    private static int u32(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) << 24)
                | ((bytes[offset + 1] & 0xFF) << 16)
                | ((bytes[offset + 2] & 0xFF) << 8)
                | (bytes[offset + 3] & 0xFF);
    }

    /// One counted string and the bytes it occupied.
    private static final class CountedString {
        /// The ASCII text, or `null` when the count is zero.
        private final @Nullable String text;

        /// Bytes consumed from the tag origin.
        private final int consumed;

        /// Creates one parse result.
        ///
        /// @param text the ASCII text, or `null`
        /// @param consumed the byte count
        private CountedString(@Nullable String text, int consumed) {
            this.text = text;
            this.consumed = consumed;
        }
    }
}

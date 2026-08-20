package org.glavo.himari.graphics;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Arrays;
import java.util.Objects;

/// Parses a bounded ICC `colorantOrderType` (`clro`) table.
///
/// Profiles are untrusted input. Each stored value is a one-based index into the
/// colorant table. The parser accepts at most [`#MAX_COLORANTS`] indices.
///
/// @param indices the one-based colorant indices in layout order
@NotNullByDefault
public record IccColorantOrder(int @Unmodifiable [] indices) {
    /// Type and tag `'clro'`.
    public static final int SIGNATURE = 0x636C_726F;

    /// Maximum accepted colorant count.
    public static final int MAX_COLORANTS = 16;

    /// Bytes in the tag header before the first index.
    private static final int HEADER_BYTES = 12;

    /// Validates and copies the indices.
    public IccColorantOrder {
        Objects.requireNonNull(indices, "indices");
        if (indices.length > MAX_COLORANTS) {
            throw new IllegalArgumentException("ICC colorant-order count exceeds the accepted bound");
        }
        indices = Arrays.copyOf(indices, indices.length);
        for (int index : indices) {
            if (index < 1 || index > MAX_COLORANTS) {
                throw new IllegalArgumentException("ICC colorant-order indices must be 1-based and at most "
                        + MAX_COLORANTS);
            }
        }
    }

    /// Parses one `clro` tag.
    ///
    /// @param bytes the profile bytes
    /// @param offset the tag start
    /// @param size the tag size
    /// @return the order table
    public static IccColorantOrder parse(byte[] bytes, int offset, int size) {
        Objects.requireNonNull(bytes, "bytes");
        if (offset < 0 || size < HEADER_BYTES || offset > bytes.length - size) {
            throw new IllegalArgumentException("ICC clro tag is outside the profile");
        }
        if (u32(bytes, offset) != SIGNATURE) {
            throw new IllegalArgumentException("ICC colorant-order tag is not clro");
        }
        int count = u32(bytes, offset + 8);
        if (count < 0 || count > MAX_COLORANTS) {
            throw new IllegalArgumentException("ICC colorant-order count is outside the accepted bounds");
        }
        if (size < HEADER_BYTES + count) {
            throw new IllegalArgumentException("ICC clro tag is truncated");
        }
        int[] indices = new int[count];
        for (int index = 0; index < count; index++) {
            indices[index] = bytes[offset + HEADER_BYTES + index] & 0xFF;
        }
        return new IccColorantOrder(indices);
    }

    /// Reads a big-endian unsigned 32-bit integer as a signed Java `int`.
    private static int u32(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) << 24)
                | ((bytes[offset + 1] & 0xFF) << 16)
                | ((bytes[offset + 2] & 0xFF) << 8)
                | (bytes[offset + 3] & 0xFF);
    }
}

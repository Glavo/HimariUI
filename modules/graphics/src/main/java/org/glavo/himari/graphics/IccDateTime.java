package org.glavo.himari.graphics;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Parses a bounded ICC `dateTimeType` (`dtim`) value.
///
/// @param year the year
/// @param month the month in `[1, 12]`
/// @param day the day in `[1, 31]`
/// @param hours the hour in `[0, 23]`
/// @param minutes the minute in `[0, 59]`
/// @param seconds the second in `[0, 59]`
@NotNullByDefault
public record IccDateTime(int year, int month, int day, int hours, int minutes, int seconds) {
    /// Type `'dtim'`.
    public static final int TYPE_DTIM = 0x6474_696D;

    /// Minimum accepted tag size.
    private static final int MIN_SIZE = 20;

    /// Validates civil-time fields.
    public IccDateTime {
        if (year < 1 || month < 1 || month > 12 || day < 1 || day > 31
                || hours < 0 || hours > 23 || minutes < 0 || minutes > 59
                || seconds < 0 || seconds > 59) {
            throw new IllegalArgumentException("ICC date-time fields are out of range");
        }
    }

    /// Parses one `dtim` tag.
    ///
    /// @param bytes the profile bytes
    /// @param offset the tag start
    /// @param size the tag size
    /// @return the date-time
    public static IccDateTime parse(byte[] bytes, int offset, int size) {
        Objects.requireNonNull(bytes, "bytes");
        if (offset < 0 || size < MIN_SIZE || offset > bytes.length - size) {
            throw new IllegalArgumentException("ICC dtim tag is outside the profile");
        }
        if (u32(bytes, offset) != TYPE_DTIM) {
            throw new IllegalArgumentException("ICC date-time tag is not dtim");
        }
        return new IccDateTime(
                u16(bytes, offset + 8),
                u16(bytes, offset + 10),
                u16(bytes, offset + 12),
                u16(bytes, offset + 14),
                u16(bytes, offset + 16),
                u16(bytes, offset + 18)
        );
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
}

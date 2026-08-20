package org.glavo.himari.graphics;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/// Parses a bounded ICC `ucrbgType` (`bfd `) under-color-removal and black-generation table.
///
/// Profiles are untrusted input. The UCR and BG curves are `curv` elements. An optional
/// trailing 7-bit ASCII description may follow the second curve.
///
/// @param ucr the under-color-removal curve
/// @param bg the black-generation curve
/// @param description the optional ASCII description, or `null` when absent
@NotNullByDefault
public record IccUcrBg(IccProfile.Curve ucr, IccProfile.Curve bg, @Nullable String description) {
    /// Type and tag `'bfd '`.
    public static final int SIGNATURE = 0x6266_6420;

    /// Type `'curv'`.
    private static final int TYPE_CURV = 0x6375_7276;

    /// Maximum accepted ASCII description length, including the terminating NUL.
    private static final int MAX_ASCII = 256;

    /// Validates the curves.
    public IccUcrBg {
        Objects.requireNonNull(ucr, "ucr");
        Objects.requireNonNull(bg, "bg");
    }

    /// Parses one `bfd ` tag.
    ///
    /// @param bytes the profile bytes
    /// @param offset the tag start
    /// @param size the tag size
    /// @return the UCR/BG table
    public static IccUcrBg parse(byte[] bytes, int offset, int size) {
        Objects.requireNonNull(bytes, "bytes");
        if (offset < 0 || size < 24 || offset > bytes.length - size) {
            throw new IllegalArgumentException("ICC bfd tag is outside the profile");
        }
        if (u32(bytes, offset) != SIGNATURE) {
            throw new IllegalArgumentException("ICC ucrbg tag is not bfd");
        }
        int cursor = 8;
        ParsedCurve ucr = readCurve(bytes, offset, size, cursor);
        cursor += ucr.consumed;
        ParsedCurve bg = readCurve(bytes, offset, size, cursor);
        cursor += bg.consumed;
        String description = null;
        if (cursor < size) {
            description = asciiCString(bytes, offset + cursor, size - cursor);
        }
        return new IccUcrBg(ucr.curve, bg.curve, description);
    }

    /// Reads one `curv` element at `relative` from the tag origin.
    private static ParsedCurve readCurve(byte[] bytes, int tagOffset, int size, int relative) {
        if (relative + 12 > size) {
            throw new IllegalArgumentException("ICC ucrbg curve is truncated");
        }
        int cursor = tagOffset + relative;
        if (u32(bytes, cursor) != TYPE_CURV) {
            throw new IllegalArgumentException("ICC ucrbg curve type is not curv");
        }
        int count = u32(bytes, cursor + 8);
        if (count < 0 || count > IccProfile.MAX_CURVE_ENTRIES) {
            throw new IllegalArgumentException("ICC ucrbg curve count is outside the accepted bounds");
        }
        if (count == 0) {
            return new ParsedCurve(new IccProfile.Curve(1.0f, new float[0]), 12);
        }
        if (count == 1) {
            if (relative + 14 > size) {
                throw new IllegalArgumentException("ICC ucrbg gamma curve is truncated");
            }
            float gamma = u16(bytes, cursor + 12) / 256.0f;
            if (!(gamma > 0.0f) || !Float.isFinite(gamma)) {
                throw new IllegalArgumentException("ICC ucrbg gamma must be finite and positive");
            }
            return new ParsedCurve(new IccProfile.Curve(gamma, new float[0]), 14);
        }
        int required = 12 + count * 2;
        if (relative + required > size) {
            throw new IllegalArgumentException("ICC ucrbg tabulated curve is truncated");
        }
        float[] table = new float[count];
        for (int index = 0; index < count; index++) {
            table[index] = u16(bytes, cursor + 12 + index * 2) / 65535.0f;
        }
        return new ParsedCurve(new IccProfile.Curve(1.0f, table), required);
    }

    /// Reads a NUL-terminated 7-bit ASCII field of at most `limit` bytes.
    private static String asciiCString(byte[] bytes, int offset, int limit) {
        int length = 0;
        while (length < limit && length < MAX_ASCII) {
            int value = bytes[offset + length] & 0xFF;
            if (value == 0) {
                return new String(bytes, offset, length, StandardCharsets.US_ASCII);
            }
            if (value > 0x7F) {
                throw new IllegalArgumentException("ICC ucrbg description must be 7-bit ASCII");
            }
            length++;
        }
        throw new IllegalArgumentException("ICC ucrbg description is not NUL-terminated");
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

    /// One parsed curve and the number of bytes it occupied.
    private static final class ParsedCurve {
        /// The curve.
        private final IccProfile.Curve curve;

        /// Bytes consumed from the tag origin.
        private final int consumed;

        /// Creates one parse result.
        ///
        /// @param curve the curve
        /// @param consumed the byte count
        private ParsedCurve(IccProfile.Curve curve, int consumed) {
            this.curve = curve;
            this.consumed = consumed;
        }
    }
}

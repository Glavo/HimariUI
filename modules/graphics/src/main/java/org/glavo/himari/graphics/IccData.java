package org.glavo.himari.graphics;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

/// Parses a bounded ICC `dataType` (`data`) payload.
///
/// Profiles are untrusted input. Flag `0` stores 7-bit ASCII; flag `1` stores opaque binary
/// bytes. ASCII payloads may include a trailing NUL, which [`#ascii()`] strips.
///
/// @param binary `true` when the flag is `1`
/// @param payload the data bytes after the 12-byte header
@NotNullByDefault
public record IccData(boolean binary, byte @Unmodifiable [] payload) {
    /// Type `'data'`.
    public static final int TYPE_DATA = 0x6461_7461;

    /// Tag `'crdi'`.
    public static final int TAG_CRDI = 0x6372_6469;

    /// Tag `'ps2s'`.
    public static final int TAG_PS2S = 0x7073_3273;

    /// Tag `'ps2i'`.
    public static final int TAG_PS2I = 0x7073_3269;

    /// ASCII data flag.
    public static final int FLAG_ASCII = 0;

    /// Binary data flag.
    public static final int FLAG_BINARY = 1;

    /// Maximum accepted payload size.
    public static final int MAX_PAYLOAD = 4096;

    /// Bytes in the tag header before the payload.
    private static final int HEADER_BYTES = 12;

    /// Validates the payload.
    public IccData {
        Objects.requireNonNull(payload, "payload");
        if (payload.length > MAX_PAYLOAD) {
            throw new IllegalArgumentException("ICC data payload exceeds the accepted bound");
        }
        payload = Arrays.copyOf(payload, payload.length);
        if (!binary) {
            for (byte value : payload) {
                int code = value & 0xFF;
                if (code > 0x7F) {
                    throw new IllegalArgumentException("ICC ASCII data must be 7-bit");
                }
            }
        }
    }

    /// Parses one `data` tag.
    ///
    /// @param bytes the profile bytes
    /// @param offset the tag start
    /// @param size the tag size
    /// @return the payload
    public static IccData parse(byte[] bytes, int offset, int size) {
        Objects.requireNonNull(bytes, "bytes");
        if (offset < 0 || size < HEADER_BYTES || offset > bytes.length - size) {
            throw new IllegalArgumentException("ICC data tag is outside the profile");
        }
        if (u32(bytes, offset) != TYPE_DATA) {
            throw new IllegalArgumentException("ICC data tag is not data");
        }
        int flag = u32(bytes, offset + 8);
        if (flag != FLAG_ASCII && flag != FLAG_BINARY) {
            throw new IllegalArgumentException("ICC data flag must be 0 or 1");
        }
        int payloadSize = size - HEADER_BYTES;
        if (payloadSize > MAX_PAYLOAD) {
            throw new IllegalArgumentException("ICC data payload exceeds the accepted bound");
        }
        byte[] payload = Arrays.copyOfRange(bytes, offset + HEADER_BYTES, offset + size);
        return new IccData(flag == FLAG_BINARY, payload);
    }

    /// Returns the ASCII payload without a trailing NUL, or `null` when the data is binary.
    ///
    /// @return the ASCII text, or `null`
    public @Nullable String ascii() {
        if (binary) {
            return null;
        }
        int length = payload.length;
        if (length > 0 && payload[length - 1] == 0) {
            length--;
        }
        return new String(payload, 0, length, StandardCharsets.US_ASCII);
    }

    /// Reads a big-endian unsigned 32-bit integer as a signed Java `int`.
    private static int u32(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) << 24)
                | ((bytes[offset + 1] & 0xFF) << 16)
                | ((bytes[offset + 2] & 0xFF) << 8)
                | (bytes[offset + 3] & 0xFF);
    }
}

package org.glavo.himari.graphics;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Arrays;
import java.util.Objects;

/// Parses a bounded ICC `screeningType` (`scrn`) table.
///
/// Profiles are untrusted input. Each channel stores a screen frequency in lines per
/// centimetre, an angle in degrees, and a spot-function code. Flag bit `0` is the
/// frequency/period encoding; this parser stores the encoded `s15Fixed16` values as
/// written.
///
/// @param frequencyEncoded `true` when flag bit `0` is set
/// @param channels the per-channel screening records
@NotNullByDefault
public record IccScreening(
        boolean frequencyEncoded,
        Channel @Unmodifiable [] channels
) {
    /// Type and tag `'scrn'`.
    public static final int SIGNATURE = 0x7363_726E;

    /// Default spot function (round).
    public static final int SPOT_ROUND = 1;

    /// Maximum accepted channel count.
    public static final int MAX_CHANNELS = 16;

    /// Maximum accepted spot-function code.
    public static final int MAX_SPOT_FUNCTION = 8;

    /// Bytes in the tag header before the first channel.
    private static final int HEADER_BYTES = 16;

    /// Bytes in one channel record.
    private static final int CHANNEL_BYTES = 12;

    /// Validates the table.
    public IccScreening {
        Objects.requireNonNull(channels, "channels");
        if (channels.length < 1 || channels.length > MAX_CHANNELS) {
            throw new IllegalArgumentException("ICC screening channel count is outside the accepted bounds");
        }
        channels = Arrays.copyOf(channels, channels.length);
        for (Channel channel : channels) {
            Objects.requireNonNull(channel, "channel");
        }
    }

    /// Parses one `scrn` tag.
    ///
    /// @param bytes the profile bytes
    /// @param offset the tag start
    /// @param size the tag size
    /// @return the screening table
    public static IccScreening parse(byte[] bytes, int offset, int size) {
        Objects.requireNonNull(bytes, "bytes");
        if (offset < 0 || size < HEADER_BYTES || offset > bytes.length - size) {
            throw new IllegalArgumentException("ICC scrn tag is outside the profile");
        }
        if (u32(bytes, offset) != SIGNATURE) {
            throw new IllegalArgumentException("ICC screening tag is not scrn");
        }
        int flags = u32(bytes, offset + 8);
        int count = u32(bytes, offset + 12);
        if (count < 1 || count > MAX_CHANNELS) {
            throw new IllegalArgumentException("ICC screening channel count is outside the accepted bounds");
        }
        if (size < HEADER_BYTES + count * CHANNEL_BYTES) {
            throw new IllegalArgumentException("ICC scrn tag is truncated");
        }
        Channel[] channels = new Channel[count];
        int cursor = offset + HEADER_BYTES;
        for (int index = 0; index < count; index++) {
            channels[index] = new Channel(
                    s15(bytes, cursor),
                    s15(bytes, cursor + 4),
                    u32(bytes, cursor + 8)
            );
            cursor += CHANNEL_BYTES;
        }
        return new IccScreening((flags & 1) != 0, channels);
    }

    /// Reads a big-endian unsigned 32-bit integer as a signed Java `int`.
    private static int u32(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) << 24)
                | ((bytes[offset + 1] & 0xFF) << 16)
                | ((bytes[offset + 2] & 0xFF) << 8)
                | (bytes[offset + 3] & 0xFF);
    }

    /// Reads a big-endian `s15Fixed16` number.
    private static float s15(byte[] bytes, int offset) {
        return u32(bytes, offset) / 65536.0f;
    }

    /// One screening channel.
    ///
    /// @param frequency the screen frequency or period
    /// @param angle the screen angle in degrees
    /// @param spotFunction the ICC spot-function code
    public record Channel(float frequency, float angle, int spotFunction) {
        /// Validates finite frequency/angle and a known spot-function range.
        public Channel {
            if (!Float.isFinite(frequency) || !Float.isFinite(angle)) {
                throw new IllegalArgumentException("ICC screening frequency and angle must be finite");
            }
            if (spotFunction < 0 || spotFunction > MAX_SPOT_FUNCTION) {
                throw new IllegalArgumentException("ICC screening spot function must be 0–8");
            }
        }
    }
}

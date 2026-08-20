package org.glavo.himari.graphics;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Arrays;
import java.util.Objects;

/// Parses a bounded ICC `chromaticityType` (`chrm`) table.
///
/// Profiles are untrusted input. Each channel stores CIE xy chromaticity as `s15Fixed16`.
/// The phosphor/colorant type uses the ICC enumeration, where `1` is ITU-R BT.709.
///
/// @param colorantType the phosphor or colorant type code
/// @param x the per-channel x chromaticities
/// @param y the per-channel y chromaticities
@NotNullByDefault
public record IccChromaticity(
        int colorantType,
        float @Unmodifiable [] x,
        float @Unmodifiable [] y
) {
    /// Type and tag `'chrm'`.
    public static final int SIGNATURE = 0x6368_726D;

    /// ITU-R BT.709 phosphor set.
    public static final int PHOSPHOR_BT709 = 1;

    /// Maximum accepted channel count.
    public static final int MAX_CHANNELS = 16;

    /// Maximum accepted colorant-type code.
    public static final int MAX_COLORANT_TYPE = 4;

    /// Minimum accepted tag size.
    private static final int MIN_SIZE = 12;

    /// Validates the table.
    public IccChromaticity {
        Objects.requireNonNull(x, "x");
        Objects.requireNonNull(y, "y");
        x = Arrays.copyOf(x, x.length);
        y = Arrays.copyOf(y, y.length);
        if (x.length != y.length) {
            throw new IllegalArgumentException("ICC chromaticity x and y counts must match");
        }
        if (x.length < 1 || x.length > MAX_CHANNELS) {
            throw new IllegalArgumentException("ICC chromaticity channel count is outside the accepted bounds");
        }
        if (colorantType < 0 || colorantType > MAX_COLORANT_TYPE) {
            throw new IllegalArgumentException("ICC chromaticity colorant type must be 0–4");
        }
        for (int index = 0; index < x.length; index++) {
            if (!Float.isFinite(x[index]) || !Float.isFinite(y[index])) {
                throw new IllegalArgumentException("ICC chromaticity coordinates must be finite");
            }
        }
    }

    /// Returns the number of device channels.
    ///
    /// @return the channel count
    public int channelCount() {
        return x.length;
    }

    /// Returns whether the recorded phosphor set is BT.709.
    ///
    /// @return `true` when [`#colorantType()`] is [`#PHOSPHOR_BT709`]
    public boolean bt709() {
        return colorantType == PHOSPHOR_BT709;
    }

    /// Parses one `chrm` tag.
    ///
    /// @param bytes the profile bytes
    /// @param offset the tag start
    /// @param size the tag size
    /// @return the chromaticities
    public static IccChromaticity parse(byte[] bytes, int offset, int size) {
        Objects.requireNonNull(bytes, "bytes");
        if (offset < 0 || size < MIN_SIZE || offset > bytes.length - size) {
            throw new IllegalArgumentException("ICC chrm tag is outside the profile");
        }
        if (u32(bytes, offset) != SIGNATURE) {
            throw new IllegalArgumentException("ICC chromaticity tag is not chrm");
        }
        int channels = u16(bytes, offset + 8);
        int colorantType = u16(bytes, offset + 10);
        if (channels < 1 || channels > MAX_CHANNELS) {
            throw new IllegalArgumentException("ICC chromaticity channel count is outside the accepted bounds");
        }
        int required = MIN_SIZE + channels * 8;
        if (size < required) {
            throw new IllegalArgumentException("ICC chrm tag is truncated");
        }
        float[] xs = new float[channels];
        float[] ys = new float[channels];
        int cursor = offset + MIN_SIZE;
        for (int channel = 0; channel < channels; channel++) {
            xs[channel] = s15(bytes, cursor);
            ys[channel] = s15(bytes, cursor + 4);
            cursor += 8;
        }
        return new IccChromaticity(colorantType, xs, ys);
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

    /// Reads a big-endian `s15Fixed16` number.
    private static float s15(byte[] bytes, int offset) {
        return u32(bytes, offset) / 65536.0f;
    }
}

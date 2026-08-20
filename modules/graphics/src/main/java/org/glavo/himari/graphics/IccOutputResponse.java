package org.glavo.himari.graphics;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/// Parses a bounded ICC `responseCurveSet16Type` (`resp` / `rcs2`) table.
///
/// Profiles are untrusted input. Each curve set stores one XYZ triple per channel for the
/// maximum colorant amount, then a shared sample count of `response16Number` pairs. Measurement
/// codes are stored as `uInt16 / 65535`. Device values use `s15Fixed16`.
///
/// @param channelCount the number of device channels
/// @param curveSets the measurement-unit curve sets in table order
@NotNullByDefault
public record IccOutputResponse(
        int channelCount,
        @Unmodifiable List<CurveSet> curveSets
) {
    /// Tag `'resp'`.
    public static final int SIGNATURE = 0x7265_7370;

    /// Type `'rcs2'`.
    public static final int TYPE_RCS2 = 0x7263_7332;

    /// Status A densitometry (`StaA`).
    public static final int UNIT_STATUS_A = 0x5374_6141;

    /// Maximum accepted channel count.
    public static final int MAX_CHANNELS = 16;

    /// Maximum accepted measurement-type count.
    public static final int MAX_TYPES = 8;

    /// Maximum accepted samples per channel.
    public static final int MAX_SAMPLES = 256;

    /// Bytes in the fixed header before type signatures.
    private static final int HEADER_BYTES = 12;

    /// Validates the table.
    public IccOutputResponse {
        Objects.requireNonNull(curveSets, "curveSets");
        if (channelCount < 1 || channelCount > MAX_CHANNELS) {
            throw new IllegalArgumentException("ICC response channel count is outside the accepted bounds");
        }
        if (curveSets.size() > MAX_TYPES) {
            throw new IllegalArgumentException("ICC response type count exceeds the accepted bound");
        }
        curveSets = List.copyOf(curveSets);
        for (CurveSet curveSet : curveSets) {
            Objects.requireNonNull(curveSet, "curveSet");
            if (curveSet.channels().length != channelCount) {
                throw new IllegalArgumentException("ICC response curve-set channel count must match the table");
            }
        }
    }

    /// Parses one `resp` tag.
    ///
    /// @param bytes the profile bytes
    /// @param offset the tag start
    /// @param size the tag size
    /// @return the table
    public static IccOutputResponse parse(byte[] bytes, int offset, int size) {
        Objects.requireNonNull(bytes, "bytes");
        if (offset < 0 || size < HEADER_BYTES || offset > bytes.length - size) {
            throw new IllegalArgumentException("ICC resp tag is outside the profile");
        }
        if (u32(bytes, offset) != TYPE_RCS2) {
            throw new IllegalArgumentException("ICC resp tag is not rcs2");
        }
        int channelCount = u16(bytes, offset + 8);
        int typeCount = u16(bytes, offset + 10);
        if (channelCount < 1 || channelCount > MAX_CHANNELS) {
            throw new IllegalArgumentException("ICC response channel count is outside the accepted bounds");
        }
        if (typeCount < 1 || typeCount > MAX_TYPES) {
            throw new IllegalArgumentException("ICC response type count is outside the accepted bounds");
        }
        int directory = HEADER_BYTES + typeCount * 8;
        if (size < directory) {
            throw new IllegalArgumentException("ICC resp directory is truncated");
        }
        ArrayList<CurveSet> curveSets = new ArrayList<>(typeCount);
        for (int type = 0; type < typeCount; type++) {
            int measurementType = u32(bytes, offset + HEADER_BYTES + type * 4);
            int relative = u32(bytes, offset + HEADER_BYTES + typeCount * 4 + type * 4);
            if (relative < directory || relative > size - 4) {
                throw new IllegalArgumentException("ICC resp curve set is outside the tag");
            }
            curveSets.add(readCurveSet(bytes, offset, size, relative, channelCount, measurementType));
        }
        return new IccOutputResponse(channelCount, curveSets);
    }

    /// Reads one curve set at `relative` from the tag origin.
    private static CurveSet readCurveSet(
            byte[] bytes,
            int tagOffset,
            int size,
            int relative,
            int channelCount,
            int measurementType
    ) {
        int cursor = tagOffset + relative;
        int xyzBytes = channelCount * 12;
        if (relative + xyzBytes + 4 > size) {
            throw new IllegalArgumentException("ICC resp curve set is truncated");
        }
        float[] maxX = new float[channelCount];
        float[] maxY = new float[channelCount];
        float[] maxZ = new float[channelCount];
        for (int channel = 0; channel < channelCount; channel++) {
            maxX[channel] = s15(bytes, cursor);
            maxY[channel] = s15(bytes, cursor + 4);
            maxZ[channel] = s15(bytes, cursor + 8);
            cursor += 12;
        }
        int sampleCount = u32(bytes, cursor);
        if (sampleCount < 1 || sampleCount > MAX_SAMPLES) {
            throw new IllegalArgumentException("ICC resp sample count is outside the accepted bounds");
        }
        cursor += 4;
        long required = (long) relative + xyzBytes + 4L + (long) channelCount * sampleCount * 8L;
        if (required > size) {
            throw new IllegalArgumentException("ICC resp samples exceed the tag");
        }
        Channel[] channels = new Channel[channelCount];
        for (int channel = 0; channel < channelCount; channel++) {
            float[] measurements = new float[sampleCount];
            float[] devices = new float[sampleCount];
            for (int sample = 0; sample < sampleCount; sample++) {
                measurements[sample] = u16(bytes, cursor + 2) / 65535.0f;
                devices[sample] = s15(bytes, cursor + 4);
                cursor += 8;
            }
            channels[channel] = new Channel(maxX[channel], maxY[channel], maxZ[channel], measurements, devices);
        }
        return new CurveSet(measurementType, channels);
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

    /// One measurement-unit curve set.
    ///
    /// @param measurementType the ICC measurement-unit signature
    /// @param channels one response channel per device channel
    public record CurveSet(int measurementType, Channel @Unmodifiable [] channels) {
        /// Validates the curve set.
        public CurveSet {
            Objects.requireNonNull(channels, "channels");
            if (channels.length == 0 || channels.length > MAX_CHANNELS) {
                throw new IllegalArgumentException("ICC response curve-set channel count is outside the accepted bounds");
            }
            channels = Arrays.copyOf(channels, channels.length);
            for (Channel channel : channels) {
                Objects.requireNonNull(channel, "channel");
            }
        }
    }

    /// One device-channel response curve.
    ///
    /// @param maxX the maximum-colorant PCS X
    /// @param maxY the maximum-colorant PCS Y
    /// @param maxZ the maximum-colorant PCS Z
    /// @param measurements normalized measurement codes
    /// @param devices device values as `s15Fixed16`
    public record Channel(
            float maxX,
            float maxY,
            float maxZ,
            float @Unmodifiable [] measurements,
            float @Unmodifiable [] devices
    ) {
        /// Validates the channel.
        public Channel {
            Objects.requireNonNull(measurements, "measurements");
            Objects.requireNonNull(devices, "devices");
            measurements = Arrays.copyOf(measurements, measurements.length);
            devices = Arrays.copyOf(devices, devices.length);
            if (measurements.length != devices.length) {
                throw new IllegalArgumentException("ICC response measurement and device counts must match");
            }
            if (measurements.length == 0 || measurements.length > MAX_SAMPLES) {
                throw new IllegalArgumentException("ICC response sample count is outside the accepted bounds");
            }
            if (!Float.isFinite(maxX) || !Float.isFinite(maxY) || !Float.isFinite(maxZ)) {
                throw new IllegalArgumentException("ICC response maximum-colorant XYZ must be finite");
            }
            for (int index = 0; index < measurements.length; index++) {
                if (!Float.isFinite(measurements[index]) || !Float.isFinite(devices[index])) {
                    throw new IllegalArgumentException("ICC response samples must be finite");
                }
            }
        }
    }
}

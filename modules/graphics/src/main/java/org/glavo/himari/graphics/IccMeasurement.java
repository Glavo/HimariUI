package org.glavo.himari.graphics;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Parses a bounded ICC `measurementType` (`meas`) tag.
///
/// @param observer the standard-observer code
/// @param backingX the backing X
/// @param backingY the backing Y
/// @param backingZ the backing Z
/// @param geometry the measurement-geometry code
/// @param flare the 0°/45° flare as an `s15Fixed16` fraction
/// @param illuminant the standard-illuminant code
@NotNullByDefault
public record IccMeasurement(
        int observer,
        float backingX,
        float backingY,
        float backingZ,
        int geometry,
        float flare,
        int illuminant
) {
    /// Type and tag `'meas'`.
    public static final int SIGNATURE = 0x6D65_6173;

    /// CIE 1931 standard observer.
    public static final int OBSERVER_CIE_1931 = 1;

    /// 0°/45° or 45°/0° geometry.
    public static final int GEOMETRY_0_45 = 1;

    /// Minimum accepted tag size.
    private static final int MIN_SIZE = 36;

    /// Validates observer, geometry, illuminant, and finite backing/flare.
    public IccMeasurement {
        if (observer < 0 || observer > 2) {
            throw new IllegalArgumentException("ICC measurement observer must be 0–2");
        }
        if (!Float.isFinite(backingX) || !Float.isFinite(backingY) || !Float.isFinite(backingZ)
                || !Float.isFinite(flare)) {
            throw new IllegalArgumentException("ICC measurement backing and flare must be finite");
        }
        if (backingY < 0.0f || flare < 0.0f) {
            throw new IllegalArgumentException("ICC measurement backing Y and flare must be nonnegative");
        }
        if (geometry < 0 || geometry > 2) {
            throw new IllegalArgumentException("ICC measurement geometry must be 0–2");
        }
        if (illuminant < 0 || illuminant > 8) {
            throw new IllegalArgumentException("ICC measurement illuminant must be 0–8");
        }
    }

    /// Parses one `meas` tag.
    ///
    /// @param bytes the profile bytes
    /// @param offset the tag start
    /// @param size the tag size
    /// @return the measurement
    public static IccMeasurement parse(byte[] bytes, int offset, int size) {
        Objects.requireNonNull(bytes, "bytes");
        if (offset < 0 || size < MIN_SIZE || offset > bytes.length - size) {
            throw new IllegalArgumentException("ICC meas tag is outside the profile");
        }
        if (u32(bytes, offset) != SIGNATURE) {
            throw new IllegalArgumentException("ICC measurement tag is not meas");
        }
        return new IccMeasurement(
                u32(bytes, offset + 8),
                s15(bytes, offset + 12),
                s15(bytes, offset + 16),
                s15(bytes, offset + 20),
                u32(bytes, offset + 24),
                s15(bytes, offset + 28),
                u32(bytes, offset + 32)
        );
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
}

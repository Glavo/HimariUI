package org.glavo.himari.graphics;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Parses a bounded ICC `viewingConditionsType` (`view`) tag.
///
/// Illuminant and surround coordinates are CIE absolute XYZ values. The illuminant type uses
/// the ICC standard-illuminant enumeration, where `2` is D65.
///
/// @param illuminantX the illuminant X
/// @param illuminantY the illuminant Y in candelas per square metre
/// @param illuminantZ the illuminant Z
/// @param surroundX the surround X
/// @param surroundY the surround Y
/// @param surroundZ the surround Z
/// @param illuminantType the ICC standard-illuminant code
@NotNullByDefault
public record IccViewingConditions(
        float illuminantX,
        float illuminantY,
        float illuminantZ,
        float surroundX,
        float surroundY,
        float surroundZ,
        int illuminantType
) {
    /// Type and tag `'view'`.
    public static final int SIGNATURE = 0x7669_6577;

    /// ICC standard illuminant D65.
    public static final int ILLUMINANT_D65 = 2;

    /// Minimum accepted tag size.
    private static final int MIN_SIZE = 36;

    /// Maximum accepted illuminant-type code.
    private static final int MAX_ILLUMINANT_TYPE = 8;

    /// Validates finite coordinates and a known illuminant-type range.
    public IccViewingConditions {
        if (!Float.isFinite(illuminantX) || !Float.isFinite(illuminantY) || !Float.isFinite(illuminantZ)
                || !Float.isFinite(surroundX) || !Float.isFinite(surroundY) || !Float.isFinite(surroundZ)) {
            throw new IllegalArgumentException("ICC viewing-condition coordinates must be finite");
        }
        if (illuminantY < 0.0f || surroundY < 0.0f) {
            throw new IllegalArgumentException("ICC viewing-condition Y must be nonnegative");
        }
        if (illuminantType < 0 || illuminantType > MAX_ILLUMINANT_TYPE) {
            throw new IllegalArgumentException("ICC viewing-condition illuminant type is outside 0–8");
        }
    }

    /// Parses one `view` tag.
    ///
    /// @param bytes the profile bytes
    /// @param offset the tag start
    /// @param size the tag size
    /// @return the viewing conditions
    public static IccViewingConditions parse(byte[] bytes, int offset, int size) {
        Objects.requireNonNull(bytes, "bytes");
        if (offset < 0 || size < MIN_SIZE || offset > bytes.length - size) {
            throw new IllegalArgumentException("ICC view tag is outside the profile");
        }
        if (u32(bytes, offset) != SIGNATURE) {
            throw new IllegalArgumentException("ICC viewing-condition tag is not view");
        }
        return new IccViewingConditions(
                s15(bytes, offset + 8),
                s15(bytes, offset + 12),
                s15(bytes, offset + 16),
                s15(bytes, offset + 20),
                s15(bytes, offset + 24),
                s15(bytes, offset + 28),
                u32(bytes, offset + 32)
        );
    }

    /// Returns whether the recorded illuminant type is D65.
    ///
    /// @return whether [`#illuminantType()`] is [`#ILLUMINANT_D65`]
    public boolean d65() {
        return illuminantType == ILLUMINANT_D65;
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

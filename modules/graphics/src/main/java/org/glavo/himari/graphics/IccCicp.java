package org.glavo.himari.graphics;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Parses a bounded ICC `cicpType` (`cicp`) coding-independent code-point tag.
///
/// The four fields are ITU-T H.273 / ISO/IEC 23091-2 unsigned 8-bit codes. This parser stores
/// the codes; it does not map them onto [`ColorEncoding`].
///
/// @param colorPrimaries the H.273 colour primaries code
/// @param transferCharacteristics the H.273 transfer-characteristics code
/// @param matrixCoefficients the H.273 matrix-coefficients code
/// @param videoFullRangeFlag `0` for narrow range, `1` for full range
@NotNullByDefault
public record IccCicp(
        int colorPrimaries,
        int transferCharacteristics,
        int matrixCoefficients,
        int videoFullRangeFlag
) {
    /// Type and tag `'cicp'`.
    public static final int SIGNATURE = 0x6369_6370;

    /// H.273 colour primaries for BT.709 / sRGB.
    public static final int PRIMARIES_BT709 = 1;

    /// H.273 transfer characteristics for sRGB (IEC 61966-2-1).
    public static final int TRANSFER_SRGB = 13;

    /// H.273 identity matrix coefficients.
    public static final int MATRIX_RGB = 0;

    /// Full-range video samples.
    public static final int RANGE_FULL = 1;

    /// Minimum accepted tag size.
    private static final int MIN_SIZE = 12;

    /// Validates the four unsigned 8-bit codes.
    public IccCicp {
        if (colorPrimaries < 0 || colorPrimaries > 255
                || transferCharacteristics < 0 || transferCharacteristics > 255
                || matrixCoefficients < 0 || matrixCoefficients > 255) {
            throw new IllegalArgumentException("ICC cicp codes must be unsigned 8-bit values");
        }
        if (videoFullRangeFlag != 0 && videoFullRangeFlag != 1) {
            throw new IllegalArgumentException("ICC cicp video full-range flag must be 0 or 1");
        }
    }

    /// Parses one `cicp` tag.
    ///
    /// @param bytes the profile bytes
    /// @param offset the tag start
    /// @param size the tag size
    /// @return the code points
    public static IccCicp parse(byte[] bytes, int offset, int size) {
        Objects.requireNonNull(bytes, "bytes");
        if (offset < 0 || size < MIN_SIZE || offset > bytes.length - size) {
            throw new IllegalArgumentException("ICC cicp tag is outside the profile");
        }
        if (u32(bytes, offset) != SIGNATURE) {
            throw new IllegalArgumentException("ICC cicp tag is not cicp");
        }
        return new IccCicp(
                bytes[offset + 8] & 0xFF,
                bytes[offset + 9] & 0xFF,
                bytes[offset + 10] & 0xFF,
                bytes[offset + 11] & 0xFF
        );
    }

    /// Returns whether the recorded range flag is full range.
    ///
    /// @return `true` when [`#videoFullRangeFlag()`] is [`#RANGE_FULL`]
    public boolean fullRange() {
        return videoFullRangeFlag == RANGE_FULL;
    }

    /// Reads a big-endian unsigned 32-bit integer as a signed Java `int`.
    private static int u32(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) << 24)
                | ((bytes[offset + 1] & 0xFF) << 16)
                | ((bytes[offset + 2] & 0xFF) << 8)
                | (bytes[offset + 3] & 0xFF);
    }
}

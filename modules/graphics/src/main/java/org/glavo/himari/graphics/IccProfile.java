package org.glavo.himari.graphics;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;

/// Parses a bounded ICC v2/v4 matrix/TRC RGB profile and converts samples to extended-linear sRGB.
///
/// Profiles are treated as untrusted input. The parser accepts RGB-to-XYZ matrix profiles with
/// `curv` or type-0 `para` tone curves and rejects larger or malformed tables.
///
/// @param size the declared profile size in bytes
/// @param version the packed ICC version word
/// @param deviceColorSpace the 4-character data color space
/// @param pcs the 4-character profile connection space
/// @param illuminantX the header illuminant X
/// @param illuminantY the header illuminant Y
/// @param illuminantZ the header illuminant Z
/// @param redX the red primary X in PCS XYZ
/// @param redY the red primary Y in PCS XYZ
/// @param redZ the red primary Z in PCS XYZ
/// @param greenX the green primary X in PCS XYZ
/// @param greenY the green primary Y in PCS XYZ
/// @param greenZ the green primary Z in PCS XYZ
/// @param blueX the blue primary X in PCS XYZ
/// @param blueY the blue primary Y in PCS XYZ
/// @param blueZ the blue primary Z in PCS XYZ
/// @param redTrc the red tone curve
/// @param greenTrc the green tone curve
/// @param blueTrc the blue tone curve
/// @param sha256 the lowercase SHA-256 digest of the exact profile bytes
/// @param clut the optional AToB0 `mft2` CLUT, or `null` when the matrix/TRC path is used
@NotNullByDefault
public record IccProfile(
        int size,
        int version,
        String deviceColorSpace,
        String pcs,
        float illuminantX,
        float illuminantY,
        float illuminantZ,
        float redX,
        float redY,
        float redZ,
        float greenX,
        float greenY,
        float greenZ,
        float blueX,
        float blueY,
        float blueZ,
        Curve redTrc,
        Curve greenTrc,
        Curve blueTrc,
        String sha256,
        @Nullable IccClut clut
) {
    /// Maximum accepted profile size.
    public static final int MAX_PROFILE_BYTES = 1_048_576;

    /// Maximum accepted tag-table entries.
    public static final int MAX_TAGS = 64;

    /// Maximum accepted `curv` table entries.
    public static final int MAX_CURVE_ENTRIES = 4096;

    /// ICC `'acsp'` magic.
    private static final int MAGIC_ACSP = 0x6163_7370;

    /// ICC `'RGB '`.
    private static final int SPACE_RGB = 0x5247_4220;

    /// ICC `'XYZ '`.
    private static final int SPACE_XYZ = 0x5859_5A20;

    /// Tag `'rXYZ'`.
    private static final int TAG_R_XYZ = 0x7258_595A;

    /// Tag `'gXYZ'`.
    private static final int TAG_G_XYZ = 0x6758_595A;

    /// Tag `'bXYZ'`.
    private static final int TAG_B_XYZ = 0x6258_595A;

    /// Tag `'rTRC'`.
    private static final int TAG_R_TRC = 0x7254_5243;

    /// Tag `'gTRC'`.
    private static final int TAG_G_TRC = 0x6754_5243;

    /// Tag `'bTRC'`.
    private static final int TAG_B_TRC = 0x6254_5243;

    /// Tag `'A2B0'`.
    private static final int TAG_A2B0 = 0x4132_4230;

    /// Type `'XYZ '`.
    private static final int TYPE_XYZ = 0x5859_5A20;

    /// Type `'curv'`.
    private static final int TYPE_CURV = 0x6375_7276;

    /// Type `'para'`.
    private static final int TYPE_PARA = 0x7061_7261;

    /// Validates the parsed profile.
    public IccProfile {
        Objects.requireNonNull(deviceColorSpace, "deviceColorSpace");
        Objects.requireNonNull(pcs, "pcs");
        Objects.requireNonNull(redTrc, "redTrc");
        Objects.requireNonNull(greenTrc, "greenTrc");
        Objects.requireNonNull(blueTrc, "blueTrc");
        Objects.requireNonNull(sha256, "sha256");
        if (size < 128 || size > MAX_PROFILE_BYTES) {
            throw new IllegalArgumentException("ICC profile size is out of range");
        }
        if (deviceColorSpace.length() != 4 || pcs.length() != 4) {
            throw new IllegalArgumentException("ICC signatures must be four characters");
        }
        if (sha256.length() != 64) {
            throw new IllegalArgumentException("ICC digest must be a 64-digit SHA-256 hex string");
        }
    }

    /// Parses one RGB matrix/TRC profile.
    ///
    /// @param bytes the exact profile bytes
    /// @return the parsed profile
    public static IccProfile parse(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes");
        if (bytes.length < 132 || bytes.length > MAX_PROFILE_BYTES) {
            throw new IllegalArgumentException("ICC profile length is outside the accepted bounds");
        }
        int declared = u32(bytes, 0);
        if (declared != bytes.length) {
            throw new IllegalArgumentException("ICC size field does not match the supplied bytes");
        }
        if (u32(bytes, 36) != MAGIC_ACSP) {
            throw new IllegalArgumentException("ICC magic is not acsp");
        }
        if (u32(bytes, 16) != SPACE_RGB) {
            throw new IllegalArgumentException("Only RGB ICC profiles are accepted");
        }
        if (u32(bytes, 20) != SPACE_XYZ) {
            throw new IllegalArgumentException("Only XYZ PCS ICC profiles are accepted");
        }
        int version = u32(bytes, 8);
        int major = (version >>> 24) & 0xFF;
        if (major != 2 && major != 4) {
            throw new IllegalArgumentException("Only ICC v2 and v4 profiles are accepted");
        }
        int tagCount = u32(bytes, 128);
        if (tagCount <= 0 || tagCount > MAX_TAGS) {
            throw new IllegalArgumentException("ICC tag count is outside the accepted bounds");
        }
        int tableEnd = 132 + tagCount * 12;
        if (tableEnd > bytes.length) {
            throw new IllegalArgumentException("ICC tag table exceeds the profile");
        }
        float[] redXyz = readXyz(bytes, requireTag(bytes, tagCount, TAG_R_XYZ));
        float[] greenXyz = readXyz(bytes, requireTag(bytes, tagCount, TAG_G_XYZ));
        float[] blueXyz = readXyz(bytes, requireTag(bytes, tagCount, TAG_B_XYZ));
        return new IccProfile(
                declared,
                version,
                signature(bytes, 16),
                signature(bytes, 20),
                s15(bytes, 68),
                s15(bytes, 72),
                s15(bytes, 76),
                redXyz[0],
                redXyz[1],
                redXyz[2],
                greenXyz[0],
                greenXyz[1],
                greenXyz[2],
                blueXyz[0],
                blueXyz[1],
                blueXyz[2],
                readCurve(bytes, requireTag(bytes, tagCount, TAG_R_TRC)),
                readCurve(bytes, requireTag(bytes, tagCount, TAG_G_TRC)),
                readCurve(bytes, requireTag(bytes, tagCount, TAG_B_TRC)),
                sha256(bytes),
                readClut(bytes, tagCount)
        );
    }

    /// Converts one RGB sample through this profile into extended-linear sRGB.
    ///
    /// When [`#clut()`] is present, the AToB0 `mft2` table is used. Otherwise the matrix/TRC path
    /// is used. PCS XYZ is chromatically adapted from the header illuminant to D65 when the
    /// illuminant is not already D65-like. The result is not clamped.
    ///
    /// @param red the device red in `[0, 1]`
    /// @param green the device green in `[0, 1]`
    /// @param blue the device blue in `[0, 1]`
    /// @param alpha the linear coverage
    /// @return the extended-linear color
    public Color toExtendedLinear(float red, float green, float blue, float alpha) {
        if (!Float.isFinite(red) || !Float.isFinite(green) || !Float.isFinite(blue) || !Float.isFinite(alpha)) {
            throw new IllegalArgumentException("ICC sample components must be finite");
        }
        if (outsideUnit(red) || outsideUnit(green) || outsideUnit(blue) || outsideUnit(alpha)) {
            throw new IllegalArgumentException("ICC sample components must be in [0, 1]");
        }
        float x;
        float y;
        float z;
        if (clut != null) {
            float[] xyz = clut.transform(red, green, blue);
            x = xyz[0];
            y = xyz[1];
            z = xyz[2];
        } else {
            float linearRed = redTrc.decode(red);
            float linearGreen = greenTrc.decode(green);
            float linearBlue = blueTrc.decode(blue);
            x = redX * linearRed + greenX * linearGreen + blueX * linearBlue;
            y = redY * linearRed + greenY * linearGreen + blueY * linearBlue;
            z = redZ * linearRed + greenZ * linearGreen + blueZ * linearBlue;
        }
        if (!isD65(illuminantX, illuminantY, illuminantZ)) {
            float[] adapted = adaptBradford(x, y, z, illuminantX, illuminantY, illuminantZ, 0.95047f, 1.0f, 1.08883f);
            x = adapted[0];
            y = adapted[1];
            z = adapted[2];
        }
        return Color.xyzD65ToExtended(x, y, z, alpha);
    }

    /// One tone-reproduction curve.
    ///
    /// An empty table with `gamma == 1` is the identity. An empty table with another gamma is
    /// `encoded^gamma`. A non-empty table is linearly interpolated in `[0, 1]`.
    ///
    /// @param gamma the power-law exponent when the table is empty
    /// @param table the normalized curve samples, or an empty array
    @NotNullByDefault
    public record Curve(float gamma, float @Unmodifiable [] table) {
        /// Validates the curve.
        public Curve {
            Objects.requireNonNull(table, "table");
            table = Arrays.copyOf(table, table.length);
            if (!Float.isFinite(gamma) || gamma <= 0.0f) {
                throw new IllegalArgumentException("ICC curve gamma must be finite and positive");
            }
            if (table.length > MAX_CURVE_ENTRIES) {
                throw new IllegalArgumentException("ICC curve table exceeds the accepted bound");
            }
            for (float sample : table) {
                if (!Float.isFinite(sample)) {
                    throw new IllegalArgumentException("ICC curve samples must be finite");
                }
            }
        }

        /// Applies the curve to one encoded component.
        ///
        /// @param encoded the encoded component
        /// @return the linearized component
        public float decode(float encoded) {
            float unit = Math.clamp(encoded, 0.0f, 1.0f);
            if (table.length == 0) {
                if (gamma == 1.0f) {
                    return unit;
                }
                return (float) Math.pow(unit, gamma);
            }
            if (table.length == 1) {
                return (float) Math.pow(unit, table[0]);
            }
            float position = unit * (table.length - 1);
            int index = Math.min((int) position, table.length - 2);
            float fraction = position - index;
            return Math.fma(table[index + 1] - table[index], fraction, table[index]);
        }
    }

    /// Parses an optional `A2B0` `mft2` tag.
    private static @Nullable IccClut readClut(byte[] bytes, int tagCount) {
        int entry = findTag(bytes, tagCount, TAG_A2B0);
        if (entry < 0) {
            return null;
        }
        int offset = u32(bytes, entry + 4);
        int size = u32(bytes, entry + 8);
        return IccClut.parseMft2(bytes, offset, size);
    }

    /// Locates one tag-table entry, or `-1` when absent.
    private static int findTag(byte[] bytes, int tagCount, int signature) {
        for (int index = 0; index < tagCount; index++) {
            int entry = 132 + index * 12;
            if (u32(bytes, entry) == signature) {
                return entry;
            }
        }
        return -1;
    }

    /// Locates one required tag.
    private static int requireTag(byte[] bytes, int tagCount, int signature) {
        for (int index = 0; index < tagCount; index++) {
            int entry = 132 + index * 12;
            if (u32(bytes, entry) == signature) {
                int offset = u32(bytes, entry + 4);
                int size = u32(bytes, entry + 8);
                if (offset < 0 || size < 0 || offset > bytes.length - size) {
                    throw new IllegalArgumentException("ICC tag offset is outside the profile");
                }
                return offset;
            }
        }
        throw new IllegalArgumentException("ICC profile is missing a required matrix/TRC tag");
    }

    /// Reads one `XYZType` tag.
    private static float[] readXyz(byte[] bytes, int offset) {
        if (offset > bytes.length - 20 || u32(bytes, offset) != TYPE_XYZ) {
            throw new IllegalArgumentException("ICC XYZ tag is malformed");
        }
        return new float[] {s15(bytes, offset + 8), s15(bytes, offset + 12), s15(bytes, offset + 16)};
    }

    /// Reads one `curv` or type-0 `para` tag.
    private static Curve readCurve(byte[] bytes, int offset) {
        if (offset > bytes.length - 12) {
            throw new IllegalArgumentException("ICC curve tag is truncated");
        }
        int type = u32(bytes, offset);
        if (type == TYPE_CURV) {
            int count = u32(bytes, offset + 8);
            if (count < 0 || count > MAX_CURVE_ENTRIES) {
                throw new IllegalArgumentException("ICC curv entry count is outside the accepted bounds");
            }
            if (count == 0) {
                return new Curve(1.0f, new float[0]);
            }
            if (count == 1) {
                if (offset > bytes.length - 14) {
                    throw new IllegalArgumentException("ICC gamma curv tag is truncated");
                }
                return new Curve(u16(bytes, offset + 12) / 256.0f, new float[0]);
            }
            int required = 12 + count * 2;
            if (offset > bytes.length - required) {
                throw new IllegalArgumentException("ICC curv table exceeds the profile");
            }
            float[] table = new float[count];
            for (int index = 0; index < count; index++) {
                table[index] = u16(bytes, offset + 12 + index * 2) / 65535.0f;
            }
            return new Curve(1.0f, table);
        }
        if (type == TYPE_PARA) {
            if (offset > bytes.length - 16) {
                throw new IllegalArgumentException("ICC para tag is truncated");
            }
            int function = u16(bytes, offset + 8);
            if (function != 0) {
                throw new IllegalArgumentException("Only parametric ICC curve type 0 is accepted");
            }
            return new Curve(s15(bytes, offset + 12), new float[0]);
        }
        throw new IllegalArgumentException("ICC tone curve type is not accepted");
    }

    /// Chromatically adapts XYZ from one white point to another with Bradford.
    private static float[] adaptBradford(
            float x,
            float y,
            float z,
            float sourceX,
            float sourceY,
            float sourceZ,
            float destinationX,
            float destinationY,
            float destinationZ
    ) {
        float[] source = bradfordCone(sourceX, sourceY, sourceZ);
        float[] destination = bradfordCone(destinationX, destinationY, destinationZ);
        float[] cone = bradfordCone(x, y, z);
        cone[0] *= destination[0] / source[0];
        cone[1] *= destination[1] / source[1];
        cone[2] *= destination[2] / source[2];
        return new float[] {
                0.9869929f * cone[0] + -0.1470543f * cone[1] + 0.1599627f * cone[2],
                0.4323053f * cone[0] + 0.5183603f * cone[1] + 0.0492912f * cone[2],
                -0.0085287f * cone[0] + 0.0400428f * cone[1] + 0.9684867f * cone[2]
        };
    }

    /// Converts XYZ into Bradford cone space.
    private static float[] bradfordCone(float x, float y, float z) {
        return new float[] {
                0.8951f * x + 0.2664f * y + -0.1614f * z,
                -0.7502f * x + 1.7135f * y + 0.0367f * z,
                0.0389f * x + -0.0685f * y + 1.0296f * z
        };
    }

    /// Returns whether the illuminant is D65-like.
    private static boolean isD65(float x, float y, float z) {
        return Math.abs(x - 0.95047f) < 0.02f && Math.abs(y - 1.0f) < 0.02f && Math.abs(z - 1.08883f) < 0.02f;
    }

    /// Reads a 4-character signature.
    private static String signature(byte[] bytes, int offset) {
        return new String(bytes, offset, 4, StandardCharsets.US_ASCII);
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

    /// Digests the profile bytes.
    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException failure) {
            throw new IllegalStateException("SHA-256 is required to address ICC profiles", failure);
        }
    }

    /// Returns whether a component is outside `[0, 1]`.
    private static boolean outsideUnit(float value) {
        return value < 0.0f || value > 1.0f;
    }
}

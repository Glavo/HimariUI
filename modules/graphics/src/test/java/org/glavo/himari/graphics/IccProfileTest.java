package org.glavo.himari.graphics;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies bounded ICC v2 matrix/TRC parsing and D50-to-D65 conversion.
@NotNullByDefault
final class IccProfileTest {
    /// A D50-relative sRGB matrix profile with identity TRCs maps white to extended-linear white.
    @Test
    void matrixTrcWhiteMapsToExtendedLinearWhite() {
        IccProfile profile = IccProfile.parse(minimalSrgbMatrixProfile());
        assertEquals(2, (profile.version() >>> 24) & 0xFF);
        assertEquals("RGB ", profile.deviceColorSpace());
        assertEquals("XYZ ", profile.pcs());
        assertEquals(64, profile.sha256().length());
        Color linear = profile.toExtendedLinear(1.0f, 1.0f, 1.0f, 1.0f);
        assertEquals(ColorEncoding.EXTENDED_LINEAR, linear.encoding());
        assertEquals(1.0f, linear.red(), 0.01f);
        assertEquals(1.0f, linear.green(), 0.01f);
        assertEquals(1.0f, linear.blue(), 0.01f);
    }

    /// Decodes ICC parametric function type 1 as `(aX + b)^g`.
    @Test
    void paraType1DecodesPowerWithScale() {
        IccProfile profile = IccProfile.parse(matrixWithParaType1());
        assertEquals(1, profile.redTrc().paraFunction());
        assertEquals(0.25f, profile.redTrc().decode(0.5f), 0.001f);
        Color linear = profile.toExtendedLinear(0.5f, 0.5f, 0.5f, 1.0f);
        assertTrue(linear.red() > 0.05f);
        assertTrue(linear.red() < 0.5f);
    }

    /// Decodes ICC parametric function type 3 as the piecewise sRGB-style function.
    @Test
    void paraType3UsesLinearToe() {
        IccProfile.Curve curve = new IccProfile.Curve(
                2.0f,
                new float[0],
                3,
                1.0f,
                0.0f,
                0.5f,
                0.5f,
                0.0f,
                0.0f
        );
        assertEquals(0.25f, curve.decode(0.5f), 0.001f);
        assertEquals(0.2f, curve.decode(0.4f), 0.001f);
    }

    /// Decodes ICC parametric function type 2 as a constant toe plus a power branch.
    @Test
    void paraType2UsesConstantToe() {
        IccProfile.Curve curve = new IccProfile.Curve(
                2.0f,
                new float[0],
                2,
                1.0f,
                0.0f,
                0.1f,
                0.0f,
                0.0f,
                0.0f
        );
        assertEquals(0.1f, curve.decode(0.0f), 0.001f);
        assertEquals(0.35f, curve.decode(0.5f), 0.001f);
        assertEquals(0.5f, curve.encode(0.35f), 0.02f);
    }

    /// Decodes ICC parametric function type 4 as a linear toe plus an offset power branch.
    @Test
    void paraType4UsesOffsetPower() {
        IccProfile.Curve curve = new IccProfile.Curve(
                2.0f,
                new float[0],
                4,
                1.0f,
                0.0f,
                0.5f,
                0.5f,
                0.1f,
                0.0f
        );
        assertEquals(0.2f, curve.decode(0.4f), 0.001f);
        assertEquals(0.35f, curve.decode(0.5f), 0.001f);
    }

    /// Rejects a truncated header instead of inventing tags.
    @Test
    void rejectsTruncatedProfile() {
        assertThrows(IllegalArgumentException.class, () -> IccProfile.parse(new byte[64]));
    }

    /// Uses the AToB0 `mft2` CLUT instead of the matrix/TRC path.
    @Test
    void atoB0ClutOverridesMatrixPath() {
        IccProfile matrix = IccProfile.parse(minimalSrgbMatrixProfile());
        IccProfile lut = IccProfile.parse(matrixPlusA2b0());
        assertEquals(null, matrix.clut());
        assertNotNull(lut.clut());
        assertEquals(2, lut.clut().grid());
        Color matrixRed = matrix.toExtendedLinear(1.0f, 0.0f, 0.0f, 1.0f);
        Color lutRed = lut.toExtendedLinear(1.0f, 0.0f, 0.0f, 1.0f);
        assertTrue(Math.abs(matrixRed.red() - lutRed.red()) > 0.05f);
        float[] xyz = lut.clut().transform(1.0f, 0.0f, 0.0f);
        assertEquals(0.2f, xyz[0], 0.001f);
        assertEquals(0.0f, xyz[1], 0.001f);
        assertEquals(0.0f, xyz[2], 0.001f);
    }

    /// Uses AToB1 when AToB0 is absent.
    @Test
    void atoB1ClutIsUsedWhenAToB0IsMissing() {
        IccProfile lut = IccProfile.parse(matrixPlusTaggedLut(0x4132_4231, 13107));
        assertEquals(null, lut.clut());
        assertNotNull(lut.clutAToB1());
        float[] xyz = lut.clutAToB1().transform(1.0f, 0.0f, 0.0f);
        assertEquals(0.2f, xyz[0], 0.001f);
        Color converted = lut.toExtendedLinear(1.0f, 0.0f, 0.0f, 1.0f);
        assertTrue(converted.red() > 0.05f);
    }

    /// Uses AToB2 when AToB0 and AToB1 are absent.
    @Test
    void atoB2ClutIsUsedWhenEarlierIntentsAreMissing() {
        IccProfile lut = IccProfile.parse(matrixPlusTaggedLut(0x4132_4232, 13107));
        assertEquals(null, lut.clut());
        assertEquals(null, lut.clutAToB1());
        assertNotNull(lut.clutAToB2());
        float[] xyz = lut.clutAToB2().transform(1.0f, 0.0f, 0.0f);
        assertEquals(0.2f, xyz[0], 0.001f);
        Color converted = lut.toExtendedLinear(1.0f, 0.0f, 0.0f, 1.0f);
        assertTrue(converted.red() > 0.05f);
    }

    /// Uses BToA2 when BToA0 and BToA1 are absent.
    @Test
    void bToA2ClutIsUsedWhenEarlierIntentsAreMissing() {
        IccProfile lut = IccProfile.parse(matrixPlusTaggedLut(0x4232_4132, 65535));
        assertEquals(null, lut.clutBToA0());
        assertEquals(null, lut.clutBToA1());
        assertNotNull(lut.clutBToA2());
        float[] rgb = lut.clutBToA2().transform(1.0f, 0.0f, 0.0f);
        assertEquals(1.0f, rgb[0], 0.001f);
        Color device = lut.fromExtendedLinear(Color.extendedLinear(1.0f, 0.0f, 0.0f, 1.0f));
        assertTrue(device.red() >= 0.0f);
    }

    /// Uses BToA1 when BToA0 is absent.
    @Test
    void bToA1ClutIsUsedWhenBToA0IsMissing() {
        IccProfile lut = IccProfile.parse(matrixPlusTaggedLut(0x4232_4131, 65535));
        assertEquals(null, lut.clutBToA0());
        assertNotNull(lut.clutBToA1());
        float[] rgb = lut.clutBToA1().transform(1.0f, 0.0f, 0.0f);
        assertEquals(1.0f, rgb[0], 0.001f);
        assertEquals(0.0f, rgb[1], 0.001f);
        assertEquals(0.0f, rgb[2], 0.001f);
        Color device = lut.fromExtendedLinear(Color.extendedLinear(1.0f, 0.0f, 0.0f, 1.0f));
        assertTrue(device.red() >= 0.0f);
    }

    /// Uses BToA0 when inverting a unique CLUT cell.
    @Test
    void bToA0ClutInvertsAUniqueCell() {
        IccProfile lut = IccProfile.parse(matrixPlusB2a0());
        assertNotNull(lut.clutBToA0());
        float[] rgb = lut.clutBToA0().transform(1.0f, 0.0f, 0.0f);
        assertEquals(1.0f, rgb[0], 0.001f);
        assertEquals(0.0f, rgb[1], 0.001f);
        assertEquals(0.0f, rgb[2], 0.001f);
        Color device = lut.fromExtendedLinear(Color.extendedLinear(1.0f, 0.0f, 0.0f, 1.0f));
        assertTrue(device.red() >= 0.0f);
    }

    /// Inverts the matrix/TRC path so encoded white stays near white.
    @Test
    void matrixPathFromExtendedLinearRoundTripsWhite() {
        IccProfile profile = IccProfile.parse(minimalSrgbMatrixProfile());
        Color encoded = profile.fromExtendedLinear(Color.extendedLinear(1.0f, 1.0f, 1.0f, 1.0f));
        assertEquals(1.0f, encoded.red(), 0.05f);
        assertEquals(1.0f, encoded.green(), 0.05f);
        assertEquals(1.0f, encoded.blue(), 0.05f);
    }

    /// Builds the matrix profile plus a 2³ `mft2` AToB0 tag.
    private static byte[] matrixPlusA2b0() {
        return matrixPlusTaggedLut(0x4132_4230, 13107);
    }

    /// Builds the matrix profile plus a 2³ `mft2` BToA0 tag.
    private static byte[] matrixPlusB2a0() {
        return matrixPlusTaggedLut(0x4232_4130, 65535);
    }

    /// Builds the matrix profile plus a 2³ `mft2` tag with `signature`.
    private static byte[] matrixPlusTaggedLut(int signature, int uniqueRed) {
        byte[] base = minimalSrgbMatrixProfile();
        int grid = 2;
        int inputTable = 256 * 3;
        int clutValues = grid * grid * grid * 3;
        int outputTable = 256 * 3;
        int mft2Size = 48 + (inputTable + clutValues + outputTable) * 2;
        int tagCount = 7;
        int table = 132 + tagCount * 12;
        int xyzSize = 20;
        int curveSize = 12;
        int size = table + 3 * xyzSize + 3 * curveSize + mft2Size;
        byte[] bytes = new byte[size];
        System.arraycopy(base, 0, bytes, 0, 128);
        putU32(bytes, 0, size);
        putU32(bytes, 128, tagCount);
        int redXyz = table;
        int greenXyz = redXyz + xyzSize;
        int blueXyz = greenXyz + xyzSize;
        int redTrc = blueXyz + xyzSize;
        int greenTrc = redTrc + curveSize;
        int blueTrc = greenTrc + curveSize;
        int a2b0 = blueTrc + curveSize;
        putTag(bytes, 0, 0x7258_595A, redXyz, xyzSize);
        putTag(bytes, 1, 0x6758_595A, greenXyz, xyzSize);
        putTag(bytes, 2, 0x6258_595A, blueXyz, xyzSize);
        putTag(bytes, 3, 0x7254_5243, redTrc, curveSize);
        putTag(bytes, 4, 0x6754_5243, greenTrc, curveSize);
        putTag(bytes, 5, 0x6254_5243, blueTrc, curveSize);
        putTag(bytes, 6, signature, a2b0, mft2Size);
        putXyz(bytes, redXyz, 0.4360657f, 0.2224884f, 0.0139160f);
        putXyz(bytes, greenXyz, 0.3851477f, 0.7168732f, 0.0971045f);
        putXyz(bytes, blueXyz, 0.1430664f, 0.0606084f, 0.7141733f);
        putIdentityCurve(bytes, redTrc);
        putIdentityCurve(bytes, greenTrc);
        putIdentityCurve(bytes, blueTrc);
        putSignature(bytes, a2b0, "mft2");
        bytes[a2b0 + 8] = 3;
        bytes[a2b0 + 9] = 3;
        bytes[a2b0 + 10] = (byte) grid;
        int cursor = a2b0 + 48;
        for (int index = 0; index < 256; index++) {
            int encoded = index * 257;
            for (int channel = 0; channel < 3; channel++) {
                putU16(bytes, cursor, encoded);
                cursor += 2;
            }
        }
        for (int red = 0; red < grid; red++) {
            for (int green = 0; green < grid; green++) {
                for (int blue = 0; blue < grid; blue++) {
                    boolean uniqueCell = red == 1 && green == 0 && blue == 0;
                    putU16(bytes, cursor, uniqueCell ? uniqueRed : 0);
                    putU16(bytes, cursor + 2, 0);
                    putU16(bytes, cursor + 4, 0);
                    cursor += 6;
                }
            }
        }
        for (int index = 0; index < 256; index++) {
            int encoded = index * 257;
            for (int channel = 0; channel < 3; channel++) {
                putU16(bytes, cursor, encoded);
                cursor += 2;
            }
        }
        return bytes;
    }

    /// Writes a big-endian unsigned 16-bit integer.
    private static void putU16(byte[] bytes, int offset, int value) {
        bytes[offset] = (byte) (value >>> 8);
        bytes[offset + 1] = (byte) value;
    }

    /// Builds a compact ICC v2 RGB matrix/TRC profile.
    ///
    /// @return the profile bytes
    private static byte[] minimalSrgbMatrixProfile() {
        int tagCount = 6;
        int table = 132 + tagCount * 12;
        int xyzSize = 20;
        int curveSize = 12;
        int size = table + 3 * xyzSize + 3 * curveSize;
        byte[] bytes = new byte[size];
        putU32(bytes, 0, size);
        putU32(bytes, 8, 0x0240_0000);
        putSignature(bytes, 12, "mntr");
        putSignature(bytes, 16, "RGB ");
        putSignature(bytes, 20, "XYZ ");
        putSignature(bytes, 36, "acsp");
        putS15(bytes, 68, 0.9642f);
        putS15(bytes, 72, 1.0f);
        putS15(bytes, 76, 0.8249f);
        putU32(bytes, 128, tagCount);
        int redXyz = table;
        int greenXyz = redXyz + xyzSize;
        int blueXyz = greenXyz + xyzSize;
        int redTrc = blueXyz + xyzSize;
        int greenTrc = redTrc + curveSize;
        int blueTrc = greenTrc + curveSize;
        putTag(bytes, 0, 0x7258_595A, redXyz, xyzSize);
        putTag(bytes, 1, 0x6758_595A, greenXyz, xyzSize);
        putTag(bytes, 2, 0x6258_595A, blueXyz, xyzSize);
        putTag(bytes, 3, 0x7254_5243, redTrc, curveSize);
        putTag(bytes, 4, 0x6754_5243, greenTrc, curveSize);
        putTag(bytes, 5, 0x6254_5243, blueTrc, curveSize);
        putXyz(bytes, redXyz, 0.4360657f, 0.2224884f, 0.0139160f);
        putXyz(bytes, greenXyz, 0.3851477f, 0.7168732f, 0.0971045f);
        putXyz(bytes, blueXyz, 0.1430664f, 0.0606084f, 0.7141733f);
        putIdentityCurve(bytes, redTrc);
        putIdentityCurve(bytes, greenTrc);
        putIdentityCurve(bytes, blueTrc);
        return bytes;
    }

    /// Writes one tag-table entry.
    private static void putTag(byte[] bytes, int index, int signature, int offset, int size) {
        int entry = 132 + index * 12;
        putU32(bytes, entry, signature);
        putU32(bytes, entry + 4, offset);
        putU32(bytes, entry + 8, size);
    }

    /// Writes one `XYZType` tag.
    private static void putXyz(byte[] bytes, int offset, float x, float y, float z) {
        putSignature(bytes, offset, "XYZ ");
        putS15(bytes, offset + 8, x);
        putS15(bytes, offset + 12, y);
        putS15(bytes, offset + 16, z);
    }

    /// Builds the matrix profile with type-1 `para` TRCs (`Y = X^2`).
    private static byte[] matrixWithParaType1() {
        int tagCount = 6;
        int table = 132 + tagCount * 12;
        int xyzSize = 20;
        int curveSize = 24;
        int size = table + 3 * xyzSize + 3 * curveSize;
        byte[] bytes = new byte[size];
        putU32(bytes, 0, size);
        putU32(bytes, 8, 0x0240_0000);
        putSignature(bytes, 12, "mntr");
        putSignature(bytes, 16, "RGB ");
        putSignature(bytes, 20, "XYZ ");
        putSignature(bytes, 36, "acsp");
        putS15(bytes, 68, 0.9642f);
        putS15(bytes, 72, 1.0f);
        putS15(bytes, 76, 0.8249f);
        putU32(bytes, 128, tagCount);
        int redXyz = table;
        int greenXyz = redXyz + xyzSize;
        int blueXyz = greenXyz + xyzSize;
        int redTrc = blueXyz + xyzSize;
        int greenTrc = redTrc + curveSize;
        int blueTrc = greenTrc + curveSize;
        putTag(bytes, 0, 0x7258_595A, redXyz, xyzSize);
        putTag(bytes, 1, 0x6758_595A, greenXyz, xyzSize);
        putTag(bytes, 2, 0x6258_595A, blueXyz, xyzSize);
        putTag(bytes, 3, 0x7254_5243, redTrc, curveSize);
        putTag(bytes, 4, 0x6754_5243, greenTrc, curveSize);
        putTag(bytes, 5, 0x6254_5243, blueTrc, curveSize);
        putXyz(bytes, redXyz, 0.4360657f, 0.2224884f, 0.0139160f);
        putXyz(bytes, greenXyz, 0.3851477f, 0.7168732f, 0.0971045f);
        putXyz(bytes, blueXyz, 0.1430664f, 0.0606084f, 0.7141733f);
        putParaType1(bytes, redTrc);
        putParaType1(bytes, greenTrc);
        putParaType1(bytes, blueTrc);
        return bytes;
    }

    /// Writes a type-1 `para` tag `Y = X^2`.
    private static void putParaType1(byte[] bytes, int offset) {
        putSignature(bytes, offset, "para");
        putU16(bytes, offset + 8, 1);
        putS15(bytes, offset + 12, 2.0f);
        putS15(bytes, offset + 16, 1.0f);
        putS15(bytes, offset + 20, 0.0f);
    }

    /// Writes one identity `curv` tag.
    private static void putIdentityCurve(byte[] bytes, int offset) {
        putSignature(bytes, offset, "curv");
        putU32(bytes, offset + 8, 0);
    }

    /// Writes a four-character signature.
    private static void putSignature(byte[] bytes, int offset, String value) {
        byte[] encoded = value.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        System.arraycopy(encoded, 0, bytes, offset, 4);
    }

    /// Writes a big-endian unsigned 32-bit integer.
    private static void putU32(byte[] bytes, int offset, int value) {
        bytes[offset] = (byte) (value >>> 24);
        bytes[offset + 1] = (byte) (value >>> 16);
        bytes[offset + 2] = (byte) (value >>> 8);
        bytes[offset + 3] = (byte) value;
    }

    /// Writes a big-endian `s15Fixed16` number.
    private static void putS15(byte[] bytes, int offset, float value) {
        putU32(bytes, offset, Math.round(value * 65536.0f));
    }
}

package org.glavo.himari.graphics;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

    /// Rejects a truncated header instead of inventing tags.
    @Test
    void rejectsTruncatedProfile() {
        assertThrows(IllegalArgumentException.class, () -> IccProfile.parse(new byte[64]));
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

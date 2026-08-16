package org.glavo.himari.graphics;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Arrays;
import java.util.Objects;

/// Stores one bounded RGB-to-XYZ `mft2` CLUT.
///
/// Only three-channel input and output with a grid of at most [`#MAX_GRID`] are accepted. Sample
/// values are normalized to `[0, 1]`.
///
/// @param grid the CLUT edge length
/// @param samples `grid³ × 3` XYZ samples in RGB cube order
@NotNullByDefault
public record IccClut(int grid, float @Unmodifiable [] samples) {
    /// Maximum accepted grid edge.
    public static final int MAX_GRID = 17;

    /// ICC `'mft2'` type.
    private static final int TYPE_MFT2 = 0x6D66_7432;

    /// Validates the table.
    public IccClut {
        Objects.requireNonNull(samples, "samples");
        samples = Arrays.copyOf(samples, samples.length);
        if (grid < 2 || grid > MAX_GRID) {
            throw new IllegalArgumentException("ICC CLUT grid is outside the accepted bounds");
        }
        int expected = grid * grid * grid * 3;
        if (samples.length != expected) {
            throw new IllegalArgumentException("ICC CLUT sample count does not match the grid");
        }
        for (float sample : samples) {
            if (!Float.isFinite(sample)) {
                throw new IllegalArgumentException("ICC CLUT samples must be finite");
            }
        }
    }

    /// Parses a v2 `lut16Type` (`mft2`) tag with a 3×3 CLUT.
    ///
    /// Input and output tables of 256 entries are read and applied. Grid sizes above
    /// [`#MAX_GRID`] are rejected.
    ///
    /// @param bytes the profile bytes
    /// @param offset the tag offset
    /// @param size the tag size
    /// @return the CLUT
    public static IccClut parseMft2(byte[] bytes, int offset, int size) {
        Objects.requireNonNull(bytes, "bytes");
        if (offset < 0 || size < 52 || offset > bytes.length - size) {
            throw new IllegalArgumentException("ICC mft2 tag is outside the profile");
        }
        if (u32(bytes, offset) != TYPE_MFT2) {
            throw new IllegalArgumentException("ICC AToB0 type is not mft2");
        }
        int inputChannels = bytes[offset + 8] & 0xFF;
        int outputChannels = bytes[offset + 9] & 0xFF;
        int grid = bytes[offset + 10] & 0xFF;
        if (inputChannels != 3 || outputChannels != 3) {
            throw new IllegalArgumentException("Only 3×3 ICC LUTs are accepted");
        }
        if (grid < 2 || grid > MAX_GRID) {
            throw new IllegalArgumentException("ICC CLUT grid is outside the accepted bounds");
        }
        int inputTable = 256 * 3;
        int clutValues = grid * grid * grid * 3;
        int outputTable = 256 * 3;
        int required = 48 + (inputTable + clutValues + outputTable) * 2;
        if (size < required) {
            throw new IllegalArgumentException("ICC mft2 tag is truncated");
        }
        int cursor = offset + 48;
        int[] input = new int[inputTable];
        for (int index = 0; index < inputTable; index++) {
            input[index] = u16(bytes, cursor);
            cursor += 2;
        }
        int[] clut = new int[clutValues];
        for (int index = 0; index < clutValues; index++) {
            clut[index] = u16(bytes, cursor);
            cursor += 2;
        }
        int[] output = new int[outputTable];
        for (int index = 0; index < outputTable; index++) {
            output[index] = u16(bytes, cursor);
            cursor += 2;
        }
        float[] samples = new float[clutValues];
        for (int index = 0; index < clutValues; index++) {
            samples[index] = clut[index] / 65535.0f;
        }
        return new IccClut(grid, samples);
    }

    /// Interpolates one RGB sample into PCS XYZ.
    ///
    /// @param red the device red in `[0, 1]`
    /// @param green the device green in `[0, 1]`
    /// @param blue the device blue in `[0, 1]`
    /// @return `{X, Y, Z}`
    public float[] transform(float red, float green, float blue) {
        if (!Float.isFinite(red) || !Float.isFinite(green) || !Float.isFinite(blue)) {
            throw new IllegalArgumentException("ICC CLUT inputs must be finite");
        }
        float r = Math.clamp(red, 0.0f, 1.0f) * (grid - 1);
        float g = Math.clamp(green, 0.0f, 1.0f) * (grid - 1);
        float b = Math.clamp(blue, 0.0f, 1.0f) * (grid - 1);
        int r0 = (int) r;
        int g0 = (int) g;
        int b0 = (int) b;
        int r1 = Math.min(r0 + 1, grid - 1);
        int g1 = Math.min(g0 + 1, grid - 1);
        int b1 = Math.min(b0 + 1, grid - 1);
        float rf = r - r0;
        float gf = g - g0;
        float bf = b - b0;
        float[] xyz = new float[3];
        for (int channel = 0; channel < 3; channel++) {
            float c000 = sample(r0, g0, b0, channel);
            float c001 = sample(r0, g0, b1, channel);
            float c010 = sample(r0, g1, b0, channel);
            float c011 = sample(r0, g1, b1, channel);
            float c100 = sample(r1, g0, b0, channel);
            float c101 = sample(r1, g0, b1, channel);
            float c110 = sample(r1, g1, b0, channel);
            float c111 = sample(r1, g1, b1, channel);
            float c00 = Math.fma(c001 - c000, bf, c000);
            float c01 = Math.fma(c011 - c010, bf, c010);
            float c10 = Math.fma(c101 - c100, bf, c100);
            float c11 = Math.fma(c111 - c110, bf, c110);
            float c0 = Math.fma(c01 - c00, gf, c00);
            float c1 = Math.fma(c11 - c10, gf, c10);
            xyz[channel] = Math.fma(c1 - c0, rf, c0);
        }
        return xyz;
    }

    /// Reads one CLUT sample.
    private float sample(int red, int green, int blue, int channel) {
        int cell = ((red * grid + green) * grid + blue) * 3 + channel;
        return samples[cell];
    }

    /// Reads a big-endian unsigned 16-bit value.
    private static int u16(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) << 8) | (bytes[offset + 1] & 0xFF);
    }

    /// Reads a big-endian unsigned 32-bit value.
    private static int u32(byte[] bytes, int offset) {
        return (bytes[offset] << 24)
                | ((bytes[offset + 1] & 0xFF) << 16)
                | ((bytes[offset + 2] & 0xFF) << 8)
                | (bytes[offset + 3] & 0xFF);
    }
}

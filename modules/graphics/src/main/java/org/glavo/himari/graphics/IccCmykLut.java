package org.glavo.himari.graphics;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Arrays;
import java.util.Objects;

/// Stores one bounded ICC CMYK `mft1`, `mft2`, `mAB `, or `mBA ` LUT.
///
/// Forward tables have four CMYK inputs and three PCS outputs. Inverse tables have
/// three PCS inputs and four CMYK outputs. Four-dimensional grids are at most
/// [`#MAX_GRID_4D`]. Three-dimensional inverse grids are at most [`#MAX_GRID_3D`].
/// Sample values are normalized to `[0, 1]`.
///
/// @param grid the CLUT edge length
/// @param samples packed CLUT samples
/// @param inputCyan the cyan or first input table, empty for identity
/// @param inputMagenta the magenta or second input table, empty for identity
/// @param inputYellow the yellow or third input table, empty for identity
/// @param inputBlack the black input table for a forward LUT, empty for identity or inverse tables
/// @param outputX the first output table, empty for identity
/// @param outputY the second output table, empty for identity
/// @param outputZ the third output table, empty for identity
/// @param outputBlack the fourth output table for an inverse LUT, empty for identity or forward tables
/// @param matrix optional 3×4 `mAB ` matrix, empty for identity
/// @param inverse `true` when the table is a 3×4 BToA pipeline
@NotNullByDefault
public record IccCmykLut(
        int grid,
        float @Unmodifiable [] samples,
        float @Unmodifiable [] inputCyan,
        float @Unmodifiable [] inputMagenta,
        float @Unmodifiable [] inputYellow,
        float @Unmodifiable [] inputBlack,
        float @Unmodifiable [] outputX,
        float @Unmodifiable [] outputY,
        float @Unmodifiable [] outputZ,
        float @Unmodifiable [] outputBlack,
        float @Unmodifiable [] matrix,
        boolean inverse
) {
    /// Maximum accepted 4D grid edge.
    public static final int MAX_GRID_4D = 5;

    /// Maximum accepted 3D inverse grid edge.
    public static final int MAX_GRID_3D = 17;

    /// ICC `'mft1'` type.
    private static final int TYPE_MFT1 = 0x6D66_7431;

    /// ICC `'mft2'` type.
    private static final int TYPE_MFT2 = 0x6D66_7432;

    /// ICC `'mAB '` type.
    private static final int TYPE_MAB = 0x6D41_4220;

    /// ICC `'mBA '` type.
    private static final int TYPE_MBA = 0x6D42_4120;

    /// Maximum planar table entries.
    private static final int MAX_TABLE_ENTRIES = 256;

    /// Empty identity table.
    private static final float[] EMPTY_TABLE = new float[0];

    /// Validates the table.
    public IccCmykLut {
        Objects.requireNonNull(samples, "samples");
        Objects.requireNonNull(inputCyan, "inputCyan");
        Objects.requireNonNull(inputMagenta, "inputMagenta");
        Objects.requireNonNull(inputYellow, "inputYellow");
        Objects.requireNonNull(inputBlack, "inputBlack");
        Objects.requireNonNull(outputX, "outputX");
        Objects.requireNonNull(outputY, "outputY");
        Objects.requireNonNull(outputZ, "outputZ");
        Objects.requireNonNull(outputBlack, "outputBlack");
        Objects.requireNonNull(matrix, "matrix");
        samples = Arrays.copyOf(samples, samples.length);
        inputCyan = copyTable(inputCyan, "inputCyan");
        inputMagenta = copyTable(inputMagenta, "inputMagenta");
        inputYellow = copyTable(inputYellow, "inputYellow");
        inputBlack = copyTable(inputBlack, "inputBlack");
        outputX = copyTable(outputX, "outputX");
        outputY = copyTable(outputY, "outputY");
        outputZ = copyTable(outputZ, "outputZ");
        outputBlack = copyTable(outputBlack, "outputBlack");
        if (matrix.length != 0 && matrix.length != 12) {
            throw new IllegalArgumentException("ICC CMYK matrix must be empty or twelve entries");
        }
        for (float value : matrix) {
            if (!Float.isFinite(value)) {
                throw new IllegalArgumentException("ICC CMYK matrix entries must be finite");
            }
        }
        matrix = Arrays.copyOf(matrix, matrix.length);
        int maxGrid = inverse ? MAX_GRID_3D : MAX_GRID_4D;
        if (grid < 2 || grid > maxGrid) {
            throw new IllegalArgumentException("ICC CMYK grid is outside the accepted bounds");
        }
        int expected = inverse
                ? grid * grid * grid * 4
                : grid * grid * grid * grid * 3;
        if (samples.length != expected) {
            throw new IllegalArgumentException("ICC CMYK sample count does not match the grid");
        }
        for (float sample : samples) {
            if (!Float.isFinite(sample)) {
                throw new IllegalArgumentException("ICC CMYK samples must be finite");
            }
        }
    }

    /// Parses one CMYK `mft1`, `mft2`, `mAB `, or `mBA ` tag.
    ///
    /// @param bytes the profile bytes
    /// @param offset the tag offset
    /// @param size the tag size
    /// @return the LUT
    public static IccCmykLut parse(byte[] bytes, int offset, int size) {
        Objects.requireNonNull(bytes, "bytes");
        if (offset < 0 || size < 12 || offset > bytes.length - size) {
            throw new IllegalArgumentException("ICC CMYK LUT tag is outside the profile");
        }
        int type = u32(bytes, offset);
        int inputChannels = bytes[offset + 8] & 0xFF;
        int outputChannels = bytes[offset + 9] & 0xFF;
        if (type == TYPE_MFT1) {
            if (inputChannels == 4 && outputChannels == 3) {
                return parseMft1Forward(bytes, offset, size);
            }
            if (inputChannels == 3 && outputChannels == 4) {
                return parseMft1Inverse(bytes, offset, size);
            }
        }
        if (type == TYPE_MFT2) {
            if (inputChannels == 4 && outputChannels == 3) {
                return parseMft2Forward(bytes, offset, size);
            }
            if (inputChannels == 3 && outputChannels == 4) {
                return parseMft2Inverse(bytes, offset, size);
            }
        }
        if (type == TYPE_MAB && inputChannels == 4 && outputChannels == 3) {
            return parseMab(bytes, offset, size, false);
        }
        if (type == TYPE_MBA && inputChannels == 3 && outputChannels == 4) {
            return parseMab(bytes, offset, size, true);
        }
        throw new IllegalArgumentException("Only 4×3 or 3×4 ICC CMYK mft1/mft2/mAB/mBA LUTs are accepted");
    }

    /// Parses a CLUT-centered CMYK `mAB ` or `mBA ` tag.
    ///
    /// A, M, and B curve offsets of `0` are identity. A non-zero 3×4 matrix offset is
    /// applied after the CLUT on the forward path and before the CLUT on the inverse path.
    ///
    /// @param bytes the profile bytes
    /// @param offset the tag offset
    /// @param size the tag size
    /// @param inverse `true` for a 3×4 BToA table
    /// @return the LUT
    private static IccCmykLut parseMab(byte[] bytes, int offset, int size, boolean inverse) {
        if (size < 32) {
            throw new IllegalArgumentException("ICC CMYK mAB tag is truncated");
        }
        int clutRelative = u32(bytes, offset + 24);
        if (clutRelative < 32 || clutRelative > size - 20) {
            throw new IllegalArgumentException("ICC CMYK mAB CLUT is outside the tag");
        }
        int clut = offset + clutRelative;
        int grid = bytes[clut] & 0xFF;
        int inputChannels = inverse ? 3 : 4;
        for (int channel = 1; channel < inputChannels; channel++) {
            if ((bytes[clut + channel] & 0xFF) != grid) {
                throw new IllegalArgumentException("Only cubic ICC CMYK mAB grids are accepted");
            }
        }
        if (inverse) {
            requireGrid3(grid);
        } else {
            requireGrid4(grid);
        }
        int precision = bytes[clut + 16] & 0xFF;
        if (precision != 1 && precision != 2) {
            throw new IllegalArgumentException("Only 8-bit or 16-bit ICC CMYK mAB CLUTs are accepted");
        }
        int clutValues = inverse
                ? grid * grid * grid * 4
                : grid * grid * grid * grid * 3;
        int sampleBytes = precision == 1 ? 1 : 2;
        if (clutRelative + 20 + clutValues * sampleBytes > size) {
            throw new IllegalArgumentException("ICC CMYK mAB tag is truncated");
        }
        float[] samples = precision == 1
                ? readSamples8(bytes, clut + 20, clutValues)
                : readSamples16(bytes, clut + 20, clutValues);
        int limit = offset + size;
        float[][] input = readMabCurves(bytes, offset, u32(bytes, offset + 28), inputChannels, limit);
        float[][] output = readMabCurves(bytes, offset, u32(bytes, offset + 12), inverse ? 4 : 3, limit);
        float[] matrix = readMabMatrix(bytes, offset, u32(bytes, offset + 16), limit);
        return inverse
                ? inverse(grid, samples, input, output, matrix)
                : forward(grid, samples, input, output, matrix);
    }

    /// Reads a 3×4 s15.16 matrix, or an empty array when `relative` is `0`.
    ///
    /// @param bytes the profile bytes
    /// @param tagOffset the tag origin
    /// @param relative the matrix offset from the tag, or `0`
    /// @param limit exclusive end of the tag
    /// @return twelve entries, or empty
    private static float[] readMabMatrix(byte[] bytes, int tagOffset, int relative, int limit) {
        if (relative == 0) {
            return EMPTY_TABLE;
        }
        int cursor = tagOffset + relative;
        if (cursor + 48 > limit) {
            throw new IllegalArgumentException("ICC CMYK mAB matrix is truncated");
        }
        float[] entries = new float[12];
        for (int index = 0; index < 12; index++) {
            entries[index] = u32(bytes, cursor + index * 4) / 65536.0f;
        }
        return entries;
    }

    /// Reads `channels` identity or sequential `curv` tables.
    ///
    /// @param bytes the profile bytes
    /// @param tagOffset the tag offset
    /// @param relative the curve offset from the tag, or `0` for identity
    /// @param channels the number of curves
    /// @param limit exclusive end of the tag
    /// @return one table per channel; empty tables are identity
    private static float[][] readMabCurves(byte[] bytes, int tagOffset, int relative, int channels, int limit) {
        float[][] tables = new float[channels][];
        if (relative == 0) {
            for (int channel = 0; channel < channels; channel++) {
                tables[channel] = EMPTY_TABLE;
            }
            return tables;
        }
        int cursor = tagOffset + relative;
        for (int channel = 0; channel < channels; channel++) {
            if (cursor > limit - 12 || u32(bytes, cursor) != 0x6375_7276) {
                throw new IllegalArgumentException("ICC CMYK mAB curve is malformed");
            }
            int count = u32(bytes, cursor + 8);
            if (count < 0 || count > MAX_TABLE_ENTRIES) {
                throw new IllegalArgumentException("ICC CMYK mAB curve length is outside the accepted bounds");
            }
            if (count <= 1) {
                tables[channel] = EMPTY_TABLE;
                cursor += 12 + (count == 1 ? 2 : 0);
            } else {
                int required = 12 + count * 2;
                if (cursor > limit - required) {
                    throw new IllegalArgumentException("ICC CMYK mAB curve is truncated");
                }
                float[] table = new float[count];
                for (int index = 0; index < count; index++) {
                    table[index] = u16(bytes, cursor + 12 + index * 2) / 65535.0f;
                }
                tables[channel] = table;
                cursor += required;
            }
            cursor = (cursor + 3) & ~3;
        }
        return tables;
    }

    /// Interpolates one CMYK sample into PCS XYZ or Lab units.
    ///
    /// @param cyan the cyan in `[0, 1]`
    /// @param magenta the magenta in `[0, 1]`
    /// @param yellow the yellow in `[0, 1]`
    /// @param black the black in `[0, 1]`
    /// @return `{X, Y, Z}` or Lab units
    public float[] transform(float cyan, float magenta, float yellow, float black) {
        if (inverse) {
            throw new IllegalStateException("ICC inverse CMYK LUT requires three PCS inputs");
        }
        requireFinite(cyan, magenta, yellow, black);
        float c = applyCurve(Math.clamp(cyan, 0.0f, 1.0f), inputCyan);
        float m = applyCurve(Math.clamp(magenta, 0.0f, 1.0f), inputMagenta);
        float y = applyCurve(Math.clamp(yellow, 0.0f, 1.0f), inputYellow);
        float k = applyCurve(Math.clamp(black, 0.0f, 1.0f), inputBlack);
        float[] pcs = applyMatrix(interpolate4(c, m, y, k));
        pcs[0] = applyCurve(pcs[0], outputX);
        pcs[1] = applyCurve(pcs[1], outputY);
        pcs[2] = applyCurve(pcs[2], outputZ);
        return pcs;
    }

    /// Interpolates one PCS sample into CMYK.
    ///
    /// @param x the PCS X or L unit in `[0, 1]`
    /// @param y the PCS Y or a unit in `[0, 1]`
    /// @param z the PCS Z or b unit in `[0, 1]`
    /// @return `{C, M, Y, K}`
    public float[] transformPcs(float x, float y, float z) {
        if (!inverse) {
            throw new IllegalStateException("ICC forward CMYK LUT requires four CMYK inputs");
        }
        if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) {
            throw new IllegalArgumentException("ICC CMYK PCS inputs must be finite");
        }
        float inX = applyCurve(Math.clamp(x, 0.0f, 1.0f), inputCyan);
        float inY = applyCurve(Math.clamp(y, 0.0f, 1.0f), inputMagenta);
        float inZ = applyCurve(Math.clamp(z, 0.0f, 1.0f), inputYellow);
        float[] mapped = applyMatrix(new float[] {inX, inY, inZ});
        float[] cmyk = interpolate3(mapped[0], mapped[1], mapped[2]);
        cmyk[0] = applyCurve(cmyk[0], outputX);
        cmyk[1] = applyCurve(cmyk[1], outputY);
        cmyk[2] = applyCurve(cmyk[2], outputZ);
        cmyk[3] = applyCurve(cmyk[3], outputBlack);
        return cmyk;
    }

    /// Parses a 4×3 `mft1` table.
    private static IccCmykLut parseMft1Forward(byte[] bytes, int offset, int size) {
        if (size < 48) {
            throw new IllegalArgumentException("ICC CMYK mft1 tag is truncated");
        }
        int grid = bytes[offset + 10] & 0xFF;
        requireGrid4(grid);
        int inputTable = 256 * 4;
        int clutValues = grid * grid * grid * grid * 3;
        int outputTable = 256 * 3;
        if (size < 48 + inputTable + clutValues + outputTable) {
            throw new IllegalArgumentException("ICC CMYK mft1 tag is truncated");
        }
        int cursor = offset + 48;
        float[][] input = readPlanar8(bytes, cursor, 4, 256);
        cursor += inputTable;
        float[] samples = readSamples8(bytes, cursor, clutValues);
        cursor += clutValues;
        float[][] output = readPlanar8(bytes, cursor, 3, 256);
        return forward(grid, samples, input, output);
    }

    /// Parses a 3×4 `mft1` table.
    private static IccCmykLut parseMft1Inverse(byte[] bytes, int offset, int size) {
        if (size < 48) {
            throw new IllegalArgumentException("ICC CMYK mft1 tag is truncated");
        }
        int grid = bytes[offset + 10] & 0xFF;
        requireGrid3(grid);
        int inputTable = 256 * 3;
        int clutValues = grid * grid * grid * 4;
        int outputTable = 256 * 4;
        if (size < 48 + inputTable + clutValues + outputTable) {
            throw new IllegalArgumentException("ICC CMYK mft1 tag is truncated");
        }
        int cursor = offset + 48;
        float[][] input = readPlanar8(bytes, cursor, 3, 256);
        cursor += inputTable;
        float[] samples = readSamples8(bytes, cursor, clutValues);
        cursor += clutValues;
        float[][] output = readPlanar8(bytes, cursor, 4, 256);
        return inverse(grid, samples, input, output);
    }

    /// Parses a 4×3 `mft2` table.
    private static IccCmykLut parseMft2Forward(byte[] bytes, int offset, int size) {
        if (size < 52) {
            throw new IllegalArgumentException("ICC CMYK mft2 tag is truncated");
        }
        int grid = bytes[offset + 10] & 0xFF;
        requireGrid4(grid);
        int inputEntries = u16(bytes, offset + 48);
        int outputEntries = u16(bytes, offset + 50);
        requireTableEntries(inputEntries);
        requireTableEntries(outputEntries);
        int inputTable = inputEntries * 4;
        int clutValues = grid * grid * grid * grid * 3;
        int outputTable = outputEntries * 3;
        if (size < 52 + (inputTable + clutValues + outputTable) * 2) {
            throw new IllegalArgumentException("ICC CMYK mft2 tag is truncated");
        }
        int cursor = offset + 52;
        float[][] input = readPlanar16(bytes, cursor, 4, inputEntries);
        cursor += inputTable * 2;
        float[] samples = readSamples16(bytes, cursor, clutValues);
        cursor += clutValues * 2;
        float[][] output = readPlanar16(bytes, cursor, 3, outputEntries);
        return forward(grid, samples, input, output);
    }

    /// Parses a 3×4 `mft2` table.
    private static IccCmykLut parseMft2Inverse(byte[] bytes, int offset, int size) {
        if (size < 52) {
            throw new IllegalArgumentException("ICC CMYK mft2 tag is truncated");
        }
        int grid = bytes[offset + 10] & 0xFF;
        requireGrid3(grid);
        int inputEntries = u16(bytes, offset + 48);
        int outputEntries = u16(bytes, offset + 50);
        requireTableEntries(inputEntries);
        requireTableEntries(outputEntries);
        int inputTable = inputEntries * 3;
        int clutValues = grid * grid * grid * 4;
        int outputTable = outputEntries * 4;
        if (size < 52 + (inputTable + clutValues + outputTable) * 2) {
            throw new IllegalArgumentException("ICC CMYK mft2 tag is truncated");
        }
        int cursor = offset + 52;
        float[][] input = readPlanar16(bytes, cursor, 3, inputEntries);
        cursor += inputTable * 2;
        float[] samples = readSamples16(bytes, cursor, clutValues);
        cursor += clutValues * 2;
        float[][] output = readPlanar16(bytes, cursor, 4, outputEntries);
        return inverse(grid, samples, input, output);
    }

    /// Builds a forward 4×3 table.
    private static IccCmykLut forward(int grid, float[] samples, float[][] input, float[][] output) {
        return forward(grid, samples, input, output, EMPTY_TABLE);
    }

    /// Builds a forward 4×3 table with an optional 3×4 matrix.
    private static IccCmykLut forward(
            int grid,
            float[] samples,
            float[][] input,
            float[][] output,
            float[] matrix
    ) {
        return new IccCmykLut(
                grid,
                samples,
                input[0],
                input[1],
                input[2],
                input[3],
                output[0],
                output[1],
                output[2],
                EMPTY_TABLE,
                matrix,
                false
        );
    }

    /// Builds an inverse 3×4 table.
    private static IccCmykLut inverse(int grid, float[] samples, float[][] input, float[][] output) {
        return inverse(grid, samples, input, output, EMPTY_TABLE);
    }

    /// Builds an inverse 3×4 table with an optional 3×4 matrix.
    private static IccCmykLut inverse(
            int grid,
            float[] samples,
            float[][] input,
            float[][] output,
            float[] matrix
    ) {
        return new IccCmykLut(
                grid,
                samples,
                input[0],
                input[1],
                input[2],
                EMPTY_TABLE,
                output[0],
                output[1],
                output[2],
                output[3],
                matrix,
                true
        );
    }

    /// Applies the 3×4 matrix, or returns `pcs` unchanged when the matrix is identity.
    ///
    /// @param pcs the PCS triple
    /// @return the mapped triple
    private float[] applyMatrix(float[] pcs) {
        if (matrix.length != 12) {
            return pcs;
        }
        float x = pcs[0];
        float y = pcs[1];
        float z = pcs[2];
        return new float[] {
                matrix[0] * x + matrix[1] * y + matrix[2] * z + matrix[3],
                matrix[4] * x + matrix[5] * y + matrix[6] * z + matrix[7],
                matrix[8] * x + matrix[9] * y + matrix[10] * z + matrix[11]
        };
    }

    /// Rejects 4D grids outside [`#MAX_GRID_4D`].
    private static void requireGrid4(int grid) {
        if (grid < 2 || grid > MAX_GRID_4D) {
            throw new IllegalArgumentException("ICC CMYK 4D grid is outside the accepted bounds");
        }
    }

    /// Rejects 3D grids outside [`#MAX_GRID_3D`].
    private static void requireGrid3(int grid) {
        if (grid < 2 || grid > MAX_GRID_3D) {
            throw new IllegalArgumentException("ICC CMYK 3D grid is outside the accepted bounds");
        }
    }

    /// Rejects table lengths outside the accepted bound.
    private static void requireTableEntries(int entries) {
        if (entries < 2 || entries > MAX_TABLE_ENTRIES) {
            throw new IllegalArgumentException("ICC CMYK table length is outside the accepted bounds");
        }
    }

    /// Copies and validates one table.
    private static float @Unmodifiable [] copyTable(float[] table, String name) {
        Objects.requireNonNull(table, name);
        if (table.length > MAX_TABLE_ENTRIES) {
            throw new IllegalArgumentException("ICC CMYK table exceeds the accepted bound");
        }
        if (table.length == 1) {
            throw new IllegalArgumentException("ICC CMYK table must contain at least two samples");
        }
        for (float sample : table) {
            if (!Float.isFinite(sample)) {
                throw new IllegalArgumentException("ICC CMYK table samples must be finite");
            }
        }
        return Arrays.copyOf(table, table.length);
    }

    /// Reads planar 8-bit tables.
    private static float[][] readPlanar8(byte[] bytes, int cursor, int channels, int entries) {
        float[][] tables = new float[channels][entries];
        int offset = cursor;
        for (int channel = 0; channel < channels; channel++) {
            for (int index = 0; index < entries; index++) {
                tables[channel][index] = (bytes[offset++] & 0xFF) / 255.0f;
            }
        }
        return tables;
    }

    /// Reads planar 16-bit tables.
    private static float[][] readPlanar16(byte[] bytes, int cursor, int channels, int entries) {
        float[][] tables = new float[channels][entries];
        int offset = cursor;
        for (int channel = 0; channel < channels; channel++) {
            for (int index = 0; index < entries; index++) {
                tables[channel][index] = u16(bytes, offset) / 65535.0f;
                offset += 2;
            }
        }
        return tables;
    }

    /// Reads 8-bit CLUT samples.
    private static float[] readSamples8(byte[] bytes, int cursor, int count) {
        float[] samples = new float[count];
        for (int index = 0; index < count; index++) {
            samples[index] = (bytes[cursor + index] & 0xFF) / 255.0f;
        }
        return samples;
    }

    /// Reads 16-bit CLUT samples.
    private static float[] readSamples16(byte[] bytes, int cursor, int count) {
        float[] samples = new float[count];
        for (int index = 0; index < count; index++) {
            samples[index] = u16(bytes, cursor + index * 2) / 65535.0f;
        }
        return samples;
    }

    /// Interpolates a 4D CLUT at unit-cube CMYK coordinates.
    private float[] interpolate4(float cyan, float magenta, float yellow, float black) {
        float gc = cyan * (grid - 1);
        float gm = magenta * (grid - 1);
        float gy = yellow * (grid - 1);
        float gk = black * (grid - 1);
        int c0 = (int) gc;
        int m0 = (int) gm;
        int y0 = (int) gy;
        int k0 = (int) gk;
        int c1 = Math.min(c0 + 1, grid - 1);
        int m1 = Math.min(m0 + 1, grid - 1);
        int y1 = Math.min(y0 + 1, grid - 1);
        int k1 = Math.min(k0 + 1, grid - 1);
        float cf = gc - c0;
        float mf = gm - m0;
        float yf = gy - y0;
        float kf = gk - k0;
        float[] pcs = new float[3];
        for (int channel = 0; channel < 3; channel++) {
            float c0000 = sample4(c0, m0, y0, k0, channel);
            float c0001 = sample4(c0, m0, y0, k1, channel);
            float c0010 = sample4(c0, m0, y1, k0, channel);
            float c0011 = sample4(c0, m0, y1, k1, channel);
            float c0100 = sample4(c0, m1, y0, k0, channel);
            float c0101 = sample4(c0, m1, y0, k1, channel);
            float c0110 = sample4(c0, m1, y1, k0, channel);
            float c0111 = sample4(c0, m1, y1, k1, channel);
            float c1000 = sample4(c1, m0, y0, k0, channel);
            float c1001 = sample4(c1, m0, y0, k1, channel);
            float c1010 = sample4(c1, m0, y1, k0, channel);
            float c1011 = sample4(c1, m0, y1, k1, channel);
            float c1100 = sample4(c1, m1, y0, k0, channel);
            float c1101 = sample4(c1, m1, y0, k1, channel);
            float c1110 = sample4(c1, m1, y1, k0, channel);
            float c1111 = sample4(c1, m1, y1, k1, channel);
            float c000 = Math.fma(c0001 - c0000, kf, c0000);
            float c001 = Math.fma(c0011 - c0010, kf, c0010);
            float c010 = Math.fma(c0101 - c0100, kf, c0100);
            float c011 = Math.fma(c0111 - c0110, kf, c0110);
            float c100 = Math.fma(c1001 - c1000, kf, c1000);
            float c101 = Math.fma(c1011 - c1010, kf, c1010);
            float c110 = Math.fma(c1101 - c1100, kf, c1100);
            float c111 = Math.fma(c1111 - c1110, kf, c1110);
            float c00 = Math.fma(c001 - c000, yf, c000);
            float c01 = Math.fma(c011 - c010, yf, c010);
            float c10 = Math.fma(c101 - c100, yf, c100);
            float c11 = Math.fma(c111 - c110, yf, c110);
            float interpM0 = Math.fma(c01 - c00, mf, c00);
            float interpM1 = Math.fma(c11 - c10, mf, c10);
            pcs[channel] = Math.fma(interpM1 - interpM0, cf, interpM0);
        }
        return pcs;
    }

    /// Interpolates a 3D inverse CLUT at unit-cube PCS coordinates.
    private float[] interpolate3(float x, float y, float z) {
        float gx = x * (grid - 1);
        float gy = y * (grid - 1);
        float gz = z * (grid - 1);
        int x0 = (int) gx;
        int y0 = (int) gy;
        int z0 = (int) gz;
        int x1 = Math.min(x0 + 1, grid - 1);
        int y1 = Math.min(y0 + 1, grid - 1);
        int z1 = Math.min(z0 + 1, grid - 1);
        float xf = gx - x0;
        float yf = gy - y0;
        float zf = gz - z0;
        float[] cmyk = new float[4];
        for (int channel = 0; channel < 4; channel++) {
            float c000 = sample3(x0, y0, z0, channel);
            float c001 = sample3(x0, y0, z1, channel);
            float c010 = sample3(x0, y1, z0, channel);
            float c011 = sample3(x0, y1, z1, channel);
            float c100 = sample3(x1, y0, z0, channel);
            float c101 = sample3(x1, y0, z1, channel);
            float c110 = sample3(x1, y1, z0, channel);
            float c111 = sample3(x1, y1, z1, channel);
            float c00 = Math.fma(c001 - c000, zf, c000);
            float c01 = Math.fma(c011 - c010, zf, c010);
            float c10 = Math.fma(c101 - c100, zf, c100);
            float c11 = Math.fma(c111 - c110, zf, c110);
            float c0 = Math.fma(c01 - c00, yf, c00);
            float c1 = Math.fma(c11 - c10, yf, c10);
            cmyk[channel] = Math.fma(c1 - c0, xf, c0);
        }
        return cmyk;
    }

    /// Applies a tabulated curve, or identity when the table is empty.
    private static float applyCurve(float value, float[] table) {
        float unit = Math.clamp(value, 0.0f, 1.0f);
        if (table.length < 2) {
            return unit;
        }
        float position = unit * (table.length - 1);
        int index = Math.min((int) position, table.length - 2);
        float fraction = position - index;
        return Math.fma(table[index + 1] - table[index], fraction, table[index]);
    }

    /// Reads one 4D CLUT sample.
    private float sample4(int cyan, int magenta, int yellow, int black, int channel) {
        int cell = ((((cyan * grid + magenta) * grid + yellow) * grid + black) * 3) + channel;
        return samples[cell];
    }

    /// Reads one 3D inverse CLUT sample.
    private float sample3(int x, int y, int z, int channel) {
        int cell = (((x * grid + y) * grid + z) * 4) + channel;
        return samples[cell];
    }

    /// Rejects non-finite CMYK inputs.
    private static void requireFinite(float cyan, float magenta, float yellow, float black) {
        if (!Float.isFinite(cyan) || !Float.isFinite(magenta) || !Float.isFinite(yellow) || !Float.isFinite(black)) {
            throw new IllegalArgumentException("ICC CMYK inputs must be finite");
        }
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

package org.glavo.himari.graphics;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Arrays;
import java.util.Objects;

/// Stores one bounded ICC `gamt` table that maps PCS coordinates to a gamut-alarm channel.
///
/// The ICC gamut tag is a 3-input, 1-output `mft1`, `mft2`, or `mBA ` LUT. Sample values are
/// normalized to `[0, 1]`. A stored alarm of `0` means the PCS sample is in gamut; any positive
/// alarm means it is out of gamut. For `mft1`/`mft2` tags an optional 3×3 matrix runs before
/// the three planar input tables, and one planar output table runs after interpolation. For
/// `mBA ` tags B curves run first, the optional 3×4 matrix next, M curves next, the CLUT next,
/// and the single A curve last.
///
/// @param grid the CLUT edge length
/// @param samples `grid³` alarm samples in cube order
/// @param matrix nine s15.16 entries for an `mft1`/`mft2` 3×3, twelve for an `mBA ` 3×4, or empty
/// @param inputRed the red/X input table, empty for identity
/// @param inputGreen the green/Y input table, empty for identity
/// @param inputBlue the blue/Z input table, empty for identity
/// @param output the single output table, empty for identity
/// @param mRedTable the `mBA ` M-curve red table, empty for identity
/// @param mGreenTable the `mBA ` M-curve green table, empty for identity
/// @param mBlueTable the `mBA ` M-curve blue table, empty for identity
/// @param bRed the `mBA ` B-curve red gamma; `1` is identity
/// @param bGreen the `mBA ` B-curve green gamma; `1` is identity
/// @param bBlue the `mBA ` B-curve blue gamma; `1` is identity
/// @param mRed the `mBA ` M-curve red gamma; `1` is identity
/// @param mGreen the `mBA ` M-curve green gamma; `1` is identity
/// @param mBlue the `mBA ` M-curve blue gamma; `1` is identity
/// @param aGamma the `mBA ` A-curve gamma; `1` is identity
/// @param inverse `true` when the table is an `mBA ` BToA pipeline
@NotNullByDefault
public record IccGamut(
        int grid,
        float @Unmodifiable [] samples,
        float @Unmodifiable [] matrix,
        float @Unmodifiable [] inputRed,
        float @Unmodifiable [] inputGreen,
        float @Unmodifiable [] inputBlue,
        float @Unmodifiable [] output,
        float @Unmodifiable [] mRedTable,
        float @Unmodifiable [] mGreenTable,
        float @Unmodifiable [] mBlueTable,
        float bRed,
        float bGreen,
        float bBlue,
        float mRed,
        float mGreen,
        float mBlue,
        float aGamma,
        boolean inverse
) {
    /// Tag `'gamt'`.
    public static final int SIGNATURE = 0x6761_6D74;

    /// Maximum accepted grid edge.
    public static final int MAX_GRID = 17;

    /// ICC `'mft1'` type (`lut8Type`).
    private static final int TYPE_MFT1 = 0x6D66_7431;

    /// ICC `'mft2'` type (`lut16Type`).
    private static final int TYPE_MFT2 = 0x6D66_7432;

    /// ICC `'mBA '` type (`lutBToAType`).
    private static final int TYPE_MBA = 0x6D42_4120;

    /// Maximum planar or `curv` entries per channel.
    private static final int MAX_TABLE_ENTRIES = 256;

    /// Empty identity table.
    private static final float[] EMPTY_TABLE = new float[0];

    /// Validates the table.
    public IccGamut {
        Objects.requireNonNull(samples, "samples");
        Objects.requireNonNull(matrix, "matrix");
        Objects.requireNonNull(inputRed, "inputRed");
        Objects.requireNonNull(inputGreen, "inputGreen");
        Objects.requireNonNull(inputBlue, "inputBlue");
        Objects.requireNonNull(output, "output");
        Objects.requireNonNull(mRedTable, "mRedTable");
        Objects.requireNonNull(mGreenTable, "mGreenTable");
        Objects.requireNonNull(mBlueTable, "mBlueTable");
        samples = Arrays.copyOf(samples, samples.length);
        matrix = Arrays.copyOf(matrix, matrix.length);
        inputRed = copyTable(inputRed, "inputRed");
        inputGreen = copyTable(inputGreen, "inputGreen");
        inputBlue = copyTable(inputBlue, "inputBlue");
        output = copyTable(output, "output");
        mRedTable = copyTable(mRedTable, "mRedTable");
        mGreenTable = copyTable(mGreenTable, "mGreenTable");
        mBlueTable = copyTable(mBlueTable, "mBlueTable");
        if (grid < 2 || grid > MAX_GRID) {
            throw new IllegalArgumentException("ICC gamut grid is outside the accepted bounds");
        }
        int expected = grid * grid * grid;
        if (samples.length != expected) {
            throw new IllegalArgumentException("ICC gamut sample count does not match the grid");
        }
        if (matrix.length != 0 && matrix.length != 9 && matrix.length != 12) {
            throw new IllegalArgumentException("ICC gamut matrix must be empty, 9, or 12 entries");
        }
        if (!Float.isFinite(bRed) || bRed <= 0.0f
                || !Float.isFinite(bGreen) || bGreen <= 0.0f
                || !Float.isFinite(bBlue) || bBlue <= 0.0f
                || !Float.isFinite(mRed) || mRed <= 0.0f
                || !Float.isFinite(mGreen) || mGreen <= 0.0f
                || !Float.isFinite(mBlue) || mBlue <= 0.0f
                || !Float.isFinite(aGamma) || aGamma <= 0.0f) {
            throw new IllegalArgumentException("ICC gamut curve gammas must be finite and positive");
        }
        for (float sample : samples) {
            if (!Float.isFinite(sample)) {
                throw new IllegalArgumentException("ICC gamut samples must be finite");
            }
        }
        for (float entry : matrix) {
            if (!Float.isFinite(entry)) {
                throw new IllegalArgumentException("ICC gamut matrix entries must be finite");
            }
        }
    }

    /// Parses one `gamt` tag body.
    ///
    /// @param bytes the profile bytes
    /// @param offset the tag offset
    /// @param size the tag size
    /// @return the gamut table
    public static IccGamut parse(byte[] bytes, int offset, int size) {
        Objects.requireNonNull(bytes, "bytes");
        if (offset < 0 || size < 12 || offset > bytes.length - size) {
            throw new IllegalArgumentException("ICC gamt tag is outside the profile");
        }
        int type = u32(bytes, offset);
        if (type == TYPE_MFT1) {
            return parseMft1(bytes, offset, size);
        }
        if (type == TYPE_MFT2) {
            return parseMft2(bytes, offset, size);
        }
        if (type == TYPE_MBA) {
            return parseMba(bytes, offset, size);
        }
        throw new IllegalArgumentException("ICC gamt type is not mft1, mft2, or mBA");
    }

    /// Evaluates the alarm channel at unit-cube PCS coordinates.
    ///
    /// @param x the PCS X coordinate in `[0, 1]`
    /// @param y the PCS Y coordinate in `[0, 1]`
    /// @param z the PCS Z coordinate in `[0, 1]`
    /// @return the alarm in `[0, 1]`; `0` is in-gamut
    public float transform(float x, float y, float z) {
        if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) {
            throw new IllegalArgumentException("ICC gamut inputs must be finite");
        }
        float inX = Math.clamp(x, 0.0f, 1.0f);
        float inY = Math.clamp(y, 0.0f, 1.0f);
        float inZ = Math.clamp(z, 0.0f, 1.0f);
        if (inverse) {
            inX = applyCurve(inX, bRed, inputRed);
            inY = applyCurve(inY, bGreen, inputGreen);
            inZ = applyCurve(inZ, bBlue, inputBlue);
            if (matrix.length == 12) {
                float[] mapped = applyMatrix3x4(inX, inY, inZ);
                inX = Math.clamp(mapped[0], 0.0f, 1.0f);
                inY = Math.clamp(mapped[1], 0.0f, 1.0f);
                inZ = Math.clamp(mapped[2], 0.0f, 1.0f);
            }
            inX = applyCurve(inX, mRed, mRedTable);
            inY = applyCurve(inY, mGreen, mGreenTable);
            inZ = applyCurve(inZ, mBlue, mBlueTable);
            float alarm = interpolate(inX, inY, inZ);
            return applyCurve(alarm, aGamma, output);
        }
        if (matrix.length == 9) {
            float[] mapped = applyMatrix3x3(inX, inY, inZ);
            inX = Math.clamp(mapped[0], 0.0f, 1.0f);
            inY = Math.clamp(mapped[1], 0.0f, 1.0f);
            inZ = Math.clamp(mapped[2], 0.0f, 1.0f);
        }
        inX = applyCurve(inX, 1.0f, inputRed);
        inY = applyCurve(inY, 1.0f, inputGreen);
        inZ = applyCurve(inZ, 1.0f, inputBlue);
        float alarm = interpolate(inX, inY, inZ);
        return applyCurve(alarm, 1.0f, output);
    }

    /// Returns whether the PCS sample is in gamut.
    ///
    /// A zero alarm is in-gamut. Any positive alarm is out-of-gamut.
    ///
    /// @param x the PCS X coordinate in `[0, 1]`
    /// @param y the PCS Y coordinate in `[0, 1]`
    /// @param z the PCS Z coordinate in `[0, 1]`
    /// @return `true` when the alarm is zero
    public boolean inGamut(float x, float y, float z) {
        return transform(x, y, z) <= 0.0f;
    }

    /// Parses a 3×1 `mft1` gamut table.
    private static IccGamut parseMft1(byte[] bytes, int offset, int size) {
        if (size < 48) {
            throw new IllegalArgumentException("ICC gamt mft1 tag is truncated");
        }
        requireChannels(bytes, offset);
        int grid = bytes[offset + 10] & 0xFF;
        requireGrid(grid);
        int inputTable = 256 * 3;
        int clutValues = grid * grid * grid;
        int outputTable = 256;
        int required = 48 + inputTable + clutValues + outputTable;
        if (size < required) {
            throw new IllegalArgumentException("ICC gamt mft1 tag is truncated");
        }
        int cursor = offset + 48;
        float[][] input = readPlanar8(bytes, cursor, 3, 256);
        cursor += inputTable;
        float[] samples = readSamples8(bytes, cursor, clutValues);
        cursor += clutValues;
        float[][] output = readPlanar8(bytes, cursor, 1, 256);
        return mft(
                grid,
                samples,
                readMft3x3(bytes, offset),
                input[0],
                input[1],
                input[2],
                output[0]
        );
    }

    /// Parses a 3×1 `mft2` gamut table.
    private static IccGamut parseMft2(byte[] bytes, int offset, int size) {
        if (size < 52) {
            throw new IllegalArgumentException("ICC gamt mft2 tag is truncated");
        }
        requireChannels(bytes, offset);
        int grid = bytes[offset + 10] & 0xFF;
        requireGrid(grid);
        int inputEntries = u16(bytes, offset + 48);
        int outputEntries = u16(bytes, offset + 50);
        requireTableEntries(inputEntries);
        requireTableEntries(outputEntries);
        int inputTable = inputEntries * 3;
        int clutValues = grid * grid * grid;
        int outputTable = outputEntries;
        int required = 52 + (inputTable + clutValues + outputTable) * 2;
        if (size < required) {
            throw new IllegalArgumentException("ICC gamt mft2 tag is truncated");
        }
        int cursor = offset + 52;
        float[][] input = readPlanar16(bytes, cursor, 3, inputEntries);
        cursor += inputTable * 2;
        float[] samples = readSamples16(bytes, cursor, clutValues);
        cursor += clutValues * 2;
        float[][] output = readPlanar16(bytes, cursor, 1, outputEntries);
        return mft(
                grid,
                samples,
                readMft3x3(bytes, offset),
                input[0],
                input[1],
                input[2],
                output[0]
        );
    }

    /// Parses a 3×1 `mBA ` gamut table.
    private static IccGamut parseMba(byte[] bytes, int offset, int size) {
        if (size < 32) {
            throw new IllegalArgumentException("ICC gamt mBA tag is truncated");
        }
        requireChannels(bytes, offset);
        int limit = offset + size;
        CurveTriple b = readCurves(bytes, offset, u32(bytes, offset + 12), limit, 3);
        float[] matrix = readMbaMatrix(bytes, offset, u32(bytes, offset + 16), limit);
        CurveTriple m = readCurves(bytes, offset, u32(bytes, offset + 20), limit, 3);
        CurveTriple a = readCurves(bytes, offset, u32(bytes, offset + 28), limit, 1);
        int clutOffset = u32(bytes, offset + 24);
        if (clutOffset < 32 || clutOffset > size - 20) {
            throw new IllegalArgumentException("ICC gamt mBA CLUT is outside the tag");
        }
        int clut = offset + clutOffset;
        int grid = bytes[clut] & 0xFF;
        if (grid != (bytes[clut + 1] & 0xFF) || grid != (bytes[clut + 2] & 0xFF)) {
            throw new IllegalArgumentException("Only cubic ICC gamt grids are accepted");
        }
        requireGrid(grid);
        int precision = bytes[clut + 16] & 0xFF;
        if (precision != 1 && precision != 2) {
            throw new IllegalArgumentException("Only 8-bit or 16-bit ICC gamt CLUTs are accepted");
        }
        int clutValues = grid * grid * grid;
        int sampleBytes = precision == 1 ? 1 : 2;
        if (clutOffset + 20 + clutValues * sampleBytes > size) {
            throw new IllegalArgumentException("ICC gamt mBA tag is truncated");
        }
        float[] samples = precision == 1
                ? readSamples8(bytes, clut + 20, clutValues)
                : readSamples16(bytes, clut + 20, clutValues);
        return new IccGamut(
                grid,
                samples,
                matrix,
                b.tables[0],
                b.tables[1],
                b.tables[2],
                a.tables[0],
                m.tables[0],
                m.tables[1],
                m.tables[2],
                b.gammas[0],
                b.gammas[1],
                b.gammas[2],
                m.gammas[0],
                m.gammas[1],
                m.gammas[2],
                a.gammas[0],
                true
        );
    }

    /// Builds an `mft1`/`mft2` table with identity B/M/A gammas.
    private static IccGamut mft(
            int grid,
            float[] samples,
            float[] matrix,
            float[] inputRed,
            float[] inputGreen,
            float[] inputBlue,
            float[] output
    ) {
        return new IccGamut(
                grid,
                samples,
                matrix,
                inputRed,
                inputGreen,
                inputBlue,
                output,
                EMPTY_TABLE,
                EMPTY_TABLE,
                EMPTY_TABLE,
                1.0f,
                1.0f,
                1.0f,
                1.0f,
                1.0f,
                1.0f,
                1.0f,
                false
        );
    }

    /// Rejects channel counts other than 3×1.
    private static void requireChannels(byte[] bytes, int offset) {
        int inputChannels = bytes[offset + 8] & 0xFF;
        int outputChannels = bytes[offset + 9] & 0xFF;
        if (inputChannels != 3 || outputChannels != 1) {
            throw new IllegalArgumentException("Only 3×1 ICC gamut LUTs are accepted");
        }
    }

    /// Rejects grid sizes outside [`#MAX_GRID`].
    private static void requireGrid(int grid) {
        if (grid < 2 || grid > MAX_GRID) {
            throw new IllegalArgumentException("ICC gamut grid is outside the accepted bounds");
        }
    }

    /// Rejects `mft2` table lengths outside the accepted bound.
    private static void requireTableEntries(int entries) {
        if (entries < 2 || entries > MAX_TABLE_ENTRIES) {
            throw new IllegalArgumentException("ICC gamut table length is outside the accepted bounds");
        }
    }

    /// Copies and validates one table.
    private static float @Unmodifiable [] copyTable(float[] table, String name) {
        Objects.requireNonNull(table, name);
        if (table.length > MAX_TABLE_ENTRIES) {
            throw new IllegalArgumentException("ICC gamut table exceeds the accepted bound");
        }
        if (table.length == 1) {
            throw new IllegalArgumentException("ICC gamut table must contain at least two samples");
        }
        for (float sample : table) {
            if (!Float.isFinite(sample)) {
                throw new IllegalArgumentException("ICC gamut table samples must be finite");
            }
        }
        return Arrays.copyOf(table, table.length);
    }

    /// Reads a 3×3 s15.16 matrix from `mft1`/`mft2` bytes 12–47.
    private static float[] readMft3x3(byte[] bytes, int offset) {
        float[] entries = new float[9];
        boolean present = false;
        for (int index = 0; index < 9; index++) {
            float value = s15(bytes, offset + 12 + index * 4);
            entries[index] = value;
            if (value != 0.0f) {
                present = true;
            }
        }
        return present ? entries : new float[0];
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

    /// Reads `count` `curv` elements, or identity when `relative` is `0`.
    private static CurveTriple readCurves(byte[] bytes, int tagOffset, int relative, int limit, int count) {
        if (relative == 0) {
            float[] gammas = new float[count];
            float[][] tables = new float[count][];
            Arrays.fill(gammas, 1.0f);
            Arrays.fill(tables, EMPTY_TABLE);
            return new CurveTriple(gammas, tables);
        }
        int cursor = tagOffset + relative;
        float[] gammas = new float[count];
        float[][] tables = new float[count][];
        for (int channel = 0; channel < count; channel++) {
            if (cursor + 12 > limit) {
                throw new IllegalArgumentException("ICC gamt curve is truncated");
            }
            if (u32(bytes, cursor) != 0x6375_7276) {
                throw new IllegalArgumentException("ICC gamt curve type is not curv");
            }
            int entries = u32(bytes, cursor + 8);
            if (entries == 0) {
                gammas[channel] = 1.0f;
                tables[channel] = EMPTY_TABLE;
                cursor += 12;
            } else if (entries == 1) {
                if (cursor + 14 > limit) {
                    throw new IllegalArgumentException("ICC gamt gamma curve is truncated");
                }
                float gamma = u16(bytes, cursor + 12) / 256.0f;
                if (!(gamma > 0.0f) || !Float.isFinite(gamma)) {
                    throw new IllegalArgumentException("ICC gamt gamma must be finite and positive");
                }
                gammas[channel] = gamma;
                tables[channel] = EMPTY_TABLE;
                cursor += 14;
            } else {
                if (entries > MAX_TABLE_ENTRIES) {
                    throw new IllegalArgumentException("ICC gamt tabulated curve exceeds the accepted bound");
                }
                int required = 12 + entries * 2;
                if (cursor + required > limit) {
                    throw new IllegalArgumentException("ICC gamt tabulated curve is truncated");
                }
                float[] table = new float[entries];
                for (int index = 0; index < entries; index++) {
                    table[index] = u16(bytes, cursor + 12 + index * 2) / 65535.0f;
                }
                gammas[channel] = 1.0f;
                tables[channel] = table;
                cursor += required;
            }
            cursor = (cursor + 3) & ~3;
        }
        return new CurveTriple(gammas, tables);
    }

    /// Reads one 3×4 s15.16 matrix, or empty when `relative` is `0`.
    private static float[] readMbaMatrix(byte[] bytes, int tagOffset, int relative, int limit) {
        if (relative == 0) {
            return new float[0];
        }
        int cursor = tagOffset + relative;
        if (cursor + 48 > limit) {
            throw new IllegalArgumentException("ICC gamt matrix is truncated");
        }
        float[] entries = new float[12];
        for (int index = 0; index < 12; index++) {
            entries[index] = s15(bytes, cursor + index * 4);
        }
        return entries;
    }

    /// Interpolates the one-channel CLUT at unit-cube coordinates.
    private float interpolate(float x, float y, float z) {
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
        float c000 = sample(x0, y0, z0);
        float c001 = sample(x0, y0, z1);
        float c010 = sample(x0, y1, z0);
        float c011 = sample(x0, y1, z1);
        float c100 = sample(x1, y0, z0);
        float c101 = sample(x1, y0, z1);
        float c110 = sample(x1, y1, z0);
        float c111 = sample(x1, y1, z1);
        float c00 = Math.fma(c001 - c000, zf, c000);
        float c01 = Math.fma(c011 - c010, zf, c010);
        float c10 = Math.fma(c101 - c100, zf, c100);
        float c11 = Math.fma(c111 - c110, zf, c110);
        float c0 = Math.fma(c01 - c00, yf, c00);
        float c1 = Math.fma(c11 - c10, yf, c10);
        return Math.fma(c1 - c0, xf, c0);
    }

    /// Applies a 3×3 `mft1`/`mft2` matrix.
    private float[] applyMatrix3x3(float x, float y, float z) {
        return new float[] {
                Math.fma(matrix[0], x, Math.fma(matrix[1], y, matrix[2] * z)),
                Math.fma(matrix[3], x, Math.fma(matrix[4], y, matrix[5] * z)),
                Math.fma(matrix[6], x, Math.fma(matrix[7], y, matrix[8] * z))
        };
    }

    /// Applies the 3×4 `mBA ` matrix.
    private float[] applyMatrix3x4(float x, float y, float z) {
        return new float[] {
                Math.fma(matrix[0], x, Math.fma(matrix[1], y, Math.fma(matrix[2], z, matrix[9]))),
                Math.fma(matrix[3], x, Math.fma(matrix[4], y, Math.fma(matrix[5], z, matrix[10]))),
                Math.fma(matrix[6], x, Math.fma(matrix[7], y, Math.fma(matrix[8], z, matrix[11])))
        };
    }

    /// Applies a tabulated or power-law curve.
    private static float applyCurve(float value, float gamma, float[] table) {
        float unit = Math.clamp(value, 0.0f, 1.0f);
        if (table.length >= 2) {
            float position = unit * (table.length - 1);
            int index = Math.min((int) position, table.length - 2);
            float fraction = position - index;
            return Math.fma(table[index + 1] - table[index], fraction, table[index]);
        }
        if (gamma == 1.0f) {
            return unit;
        }
        return (float) Math.pow(Math.max(unit, 0.0f), gamma);
    }

    /// Reads one CLUT sample.
    private float sample(int x, int y, int z) {
        return samples[((x * grid + y) * grid + z)];
    }

    /// Parsed B, M, or A curves.
    private static final class CurveTriple {
        /// Identity or gamma exponents.
        private final float[] gammas;

        /// Tabulated samples, empty to use gamma.
        private final float[][] tables;

        /// Creates one bundle.
        ///
        /// @param gammas the exponents
        /// @param tables the tables
        private CurveTriple(float[] gammas, float[][] tables) {
            this.gammas = gammas;
            this.tables = tables;
        }
    }

    /// Reads a big-endian s15.16 value.
    private static float s15(byte[] bytes, int offset) {
        return u32(bytes, offset) / 65536.0f;
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

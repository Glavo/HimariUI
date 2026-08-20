package org.glavo.himari.graphics;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Arrays;
import java.util.Objects;

/// Stores one bounded RGB-to-XYZ `mft1`, `mft2`, or `mAB ` CLUT.
///
/// Only three-channel input and output with a grid of at most [`#MAX_GRID`] are accepted. Sample
/// values are normalized to `[0, 1]`. For `mAB ` tags, A curves run before interpolation, M curves
/// run after interpolation, the optional 3×4 matrix runs next, and B curves run last. For `mBA `
/// tags the order is reversed: B, matrix, M, CLUT, A. For `mft1`/`mft2` tags a 3×3 matrix
/// runs before the input tables. Each curve stage is identity, a single gamma, or a
/// tabulated `curv` of at least two samples.
///
/// @param grid the CLUT edge length
/// @param samples `grid³ × 3` samples in cube order
/// @param aRed the A-curve red gamma; `1` is identity
/// @param aGreen the A-curve green gamma; `1` is identity
/// @param aBlue the A-curve blue gamma; `1` is identity
/// @param matrix nine s15.16 entries for an `mft1`/`mft2` 3×3, twelve for an `mAB ` 3×4, or empty for identity
/// @param bRed the B-curve red gamma; `1` is identity
/// @param bGreen the B-curve green gamma; `1` is identity
/// @param bBlue the B-curve blue gamma; `1` is identity
/// @param mRed the M-curve red gamma; `1` is identity
/// @param mGreen the M-curve green gamma; `1` is identity
/// @param mBlue the M-curve blue gamma; `1` is identity
/// @param inverse whether this table is a `mBA ` BToA pipeline
/// @param tables optional tabulated `curv` samples; empty tables use the matching gamma
/// @param para optional `para` function 1–4 stages; function `0` uses gamma or tables
@NotNullByDefault
public record IccClut(
        int grid,
        float @Unmodifiable [] samples,
        float aRed,
        float aGreen,
        float aBlue,
        float @Unmodifiable [] matrix,
        float bRed,
        float bGreen,
        float bBlue,
        float mRed,
        float mGreen,
        float mBlue,
        boolean inverse,
        Tables tables,
        ParaStages para
) {
    /// Maximum accepted grid edge.
    public static final int MAX_GRID = 17;

    /// ICC `'mAB '` type (`lutAToBType`).
    private static final int TYPE_MAB = 0x6D41_4220;

    /// ICC `'mBA '` type (`lutBToAType`).
    private static final int TYPE_MBA = 0x6D42_4120;

    /// ICC `'mft1'` type (`lut8Type`).
    private static final int TYPE_MFT1 = 0x6D66_7431;

    /// ICC `'mft2'` type.
    private static final int TYPE_MFT2 = 0x6D66_7432;

    /// Maximum tabulated `curv` entries per channel.
    private static final int MAX_CURVE_ENTRIES = 256;

    /// Empty tabulated curve.
    private static final float[] EMPTY_TABLE = new float[0];

    /// Optional tabulated A/B/M curves for one `mAB ` / `mBA ` tag.
    ///
    /// @param aRed tabulated A red samples, empty to use gamma
    /// @param aGreen tabulated A green samples, empty to use gamma
    /// @param aBlue tabulated A blue samples, empty to use gamma
    /// @param bRed tabulated B red samples, empty to use gamma
    /// @param bGreen tabulated B green samples, empty to use gamma
    /// @param bBlue tabulated B blue samples, empty to use gamma
    /// @param mRed tabulated M red samples, empty to use gamma
    /// @param mGreen tabulated M green samples, empty to use gamma
    /// @param mBlue tabulated M blue samples, empty to use gamma
    public record Tables(
            float @Unmodifiable [] aRed,
            float @Unmodifiable [] aGreen,
            float @Unmodifiable [] aBlue,
            float @Unmodifiable [] bRed,
            float @Unmodifiable [] bGreen,
            float @Unmodifiable [] bBlue,
            float @Unmodifiable [] mRed,
            float @Unmodifiable [] mGreen,
            float @Unmodifiable [] mBlue
    ) {
        /// Validates and copies the tables.
        public Tables {
            aRed = copyTable(aRed, "aRed");
            aGreen = copyTable(aGreen, "aGreen");
            aBlue = copyTable(aBlue, "aBlue");
            bRed = copyTable(bRed, "bRed");
            bGreen = copyTable(bGreen, "bGreen");
            bBlue = copyTable(bBlue, "bBlue");
            mRed = copyTable(mRed, "mRed");
            mGreen = copyTable(mGreen, "mGreen");
            mBlue = copyTable(mBlue, "mBlue");
        }

        /// Returns tables that all fall back to gamma.
        ///
        /// @return the empty tables
        public static Tables none() {
            return new Tables(
                    EMPTY_TABLE,
                    EMPTY_TABLE,
                    EMPTY_TABLE,
                    EMPTY_TABLE,
                    EMPTY_TABLE,
                    EMPTY_TABLE,
                    EMPTY_TABLE,
                    EMPTY_TABLE,
                    EMPTY_TABLE
            );
        }

        /// Copies and validates one table.
        private static float @Unmodifiable [] copyTable(float[] table, String name) {
            Objects.requireNonNull(table, name);
            if (table.length > MAX_CURVE_ENTRIES) {
                throw new IllegalArgumentException("ICC tabulated curve exceeds the accepted bound");
            }
            if (table.length == 1) {
                throw new IllegalArgumentException("ICC tabulated curve must contain at least two samples");
            }
            for (float sample : table) {
                if (!Float.isFinite(sample)) {
                    throw new IllegalArgumentException("ICC tabulated curve samples must be finite");
                }
            }
            return Arrays.copyOf(table, table.length);
        }
    }

    /// One ICC parametric curve stage.
    ///
    /// Function `0` defers to the matching gamma or table. Functions `1`–`4` use the ICC
    /// parametric formulas with [`#gamma()`] as `g`.
    ///
    /// @param function `0` through `4`
    /// @param gamma parametric `g`
    /// @param a parametric `a`
    /// @param b parametric `b`
    /// @param c parametric `c`
    /// @param d parametric `d`
    /// @param e parametric `e`
    /// @param f parametric `f`
    public record Para(
            int function,
            float gamma,
            float a,
            float b,
            float c,
            float d,
            float e,
            float f
    ) {
        /// Identity parametric stage.
        public static final Para NONE = new Para(0, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f);

        /// Validates the stage.
        public Para {
            if (function < 0 || function > 4) {
                throw new IllegalArgumentException("ICC para function must be 0 through 4");
            }
            if (!Float.isFinite(gamma) || gamma <= 0.0f
                    || !Float.isFinite(a) || !Float.isFinite(b) || !Float.isFinite(c)
                    || !Float.isFinite(d) || !Float.isFinite(e) || !Float.isFinite(f)) {
                throw new IllegalArgumentException("ICC para parameters must be finite and g must be positive");
            }
        }
    }

    /// Optional parametric A/B/M stages for one `mAB ` / `mBA ` tag.
    ///
    /// @param aRed A red para
    /// @param aGreen A green para
    /// @param aBlue A blue para
    /// @param bRed B red para
    /// @param bGreen B green para
    /// @param bBlue B blue para
    /// @param mRed M red para
    /// @param mGreen M green para
    /// @param mBlue M blue para
    public record ParaStages(
            Para aRed,
            Para aGreen,
            Para aBlue,
            Para bRed,
            Para bGreen,
            Para bBlue,
            Para mRed,
            Para mGreen,
            Para mBlue
    ) {
        /// Validates the stages.
        public ParaStages {
            Objects.requireNonNull(aRed, "aRed");
            Objects.requireNonNull(aGreen, "aGreen");
            Objects.requireNonNull(aBlue, "aBlue");
            Objects.requireNonNull(bRed, "bRed");
            Objects.requireNonNull(bGreen, "bGreen");
            Objects.requireNonNull(bBlue, "bBlue");
            Objects.requireNonNull(mRed, "mRed");
            Objects.requireNonNull(mGreen, "mGreen");
            Objects.requireNonNull(mBlue, "mBlue");
        }

        /// Returns stages that all fall back to gamma or tables.
        ///
        /// @return the identity stages
        public static ParaStages none() {
            return new ParaStages(
                    Para.NONE,
                    Para.NONE,
                    Para.NONE,
                    Para.NONE,
                    Para.NONE,
                    Para.NONE,
                    Para.NONE,
                    Para.NONE,
                    Para.NONE
            );
        }
    }

    /// Validates the table.
    public IccClut {
        Objects.requireNonNull(samples, "samples");
        Objects.requireNonNull(matrix, "matrix");
        Objects.requireNonNull(tables, "tables");
        Objects.requireNonNull(para, "para");
        samples = Arrays.copyOf(samples, samples.length);
        matrix = Arrays.copyOf(matrix, matrix.length);
        if (grid < 2 || grid > MAX_GRID) {
            throw new IllegalArgumentException("ICC CLUT grid is outside the accepted bounds");
        }
        int expected = grid * grid * grid * 3;
        if (samples.length != expected) {
            throw new IllegalArgumentException("ICC CLUT sample count does not match the grid");
        }
        if (matrix.length != 0 && matrix.length != 9 && matrix.length != 12) {
            throw new IllegalArgumentException("ICC matrix must be empty, 9, or 12 entries");
        }
        if (!Float.isFinite(aRed) || aRed <= 0.0f
                || !Float.isFinite(aGreen) || aGreen <= 0.0f
                || !Float.isFinite(aBlue) || aBlue <= 0.0f
                || !Float.isFinite(bRed) || bRed <= 0.0f
                || !Float.isFinite(bGreen) || bGreen <= 0.0f
                || !Float.isFinite(bBlue) || bBlue <= 0.0f
                || !Float.isFinite(mRed) || mRed <= 0.0f
                || !Float.isFinite(mGreen) || mGreen <= 0.0f
                || !Float.isFinite(mBlue) || mBlue <= 0.0f) {
            throw new IllegalArgumentException("ICC mAB curve gammas must be finite and positive");
        }
        for (float sample : samples) {
            if (!Float.isFinite(sample)) {
                throw new IllegalArgumentException("ICC CLUT samples must be finite");
            }
        }
        for (float entry : matrix) {
            if (!Float.isFinite(entry)) {
                throw new IllegalArgumentException("ICC mAB matrix entries must be finite");
            }
        }
    }

    /// Creates a CLUT with identity A/B curves and no matrix.
    ///
    /// @param grid the CLUT edge length
    /// @param samples `grid³ × 3` XYZ samples
    public IccClut(int grid, float[] samples) {
        this(
                grid,
                samples,
                1.0f,
                1.0f,
                1.0f,
                new float[0],
                1.0f,
                1.0f,
                1.0f,
                1.0f,
                1.0f,
                1.0f,
                false,
                Tables.none(),
                ParaStages.none()
        );
    }

    /// Parses a v4 `lutAToBType` (`mAB `) tag with a cubic 8-bit or 16-bit 3×3 CLUT.
    ///
    /// Optional A, M, and B stages may be identity, single-gamma, tabulated `curv`,
    /// or `para` functions 0–4. The optional 3×4 matrix is applied after M curves. Grid sizes above
    /// [`#MAX_GRID`] are rejected.
    ///
    /// @param bytes the profile bytes
    /// @param offset the tag offset
    /// @param size the tag size
    /// @return the CLUT
    public static IccClut parseMab(byte[] bytes, int offset, int size) {
        return parseLutAB(bytes, offset, size, TYPE_MAB, false);
    }

    /// Parses one `mAB ` / `mBA ` tag body.
    ///
    /// @param bytes the profile bytes
    /// @param offset the tag offset
    /// @param size the tag size
    /// @param expectedType the required type signature
    /// @param inverse whether the table is BToA
    /// @return the CLUT
    private static IccClut parseLutAB(
            byte[] bytes,
            int offset,
            int size,
            int expectedType,
            boolean inverse
    ) {
        Objects.requireNonNull(bytes, "bytes");
        if (offset < 0 || size < 32 || offset > bytes.length - size) {
            throw new IllegalArgumentException("ICC mAB tag is outside the profile");
        }
        if (u32(bytes, offset) != expectedType) {
            throw new IllegalArgumentException("ICC LUT type is not mAB");
        }
        int inputChannels = bytes[offset + 8] & 0xFF;
        int outputChannels = bytes[offset + 9] & 0xFF;
        if (inputChannels != 3 || outputChannels != 3) {
            throw new IllegalArgumentException("Only 3×3 ICC LUTs are accepted");
        }
        int limit = offset + size;
        CurveTriple a = readMabCurves(bytes, offset, u32(bytes, offset + 28), limit);
        float[] matrix = readMabMatrix(bytes, offset, u32(bytes, offset + 16), limit);
        CurveTriple m = readMabCurves(bytes, offset, u32(bytes, offset + 20), limit);
        CurveTriple b = readMabCurves(bytes, offset, u32(bytes, offset + 12), limit);
        int clutOffset = u32(bytes, offset + 24);
        if (clutOffset < 32 || clutOffset > size - 20) {
            throw new IllegalArgumentException("ICC mAB CLUT is outside the tag");
        }
        int clut = offset + clutOffset;
        int grid = bytes[clut] & 0xFF;
        if (grid != (bytes[clut + 1] & 0xFF) || grid != (bytes[clut + 2] & 0xFF)) {
            throw new IllegalArgumentException("Only cubic ICC mAB grids are accepted");
        }
        if (grid < 2 || grid > MAX_GRID) {
            throw new IllegalArgumentException("ICC CLUT grid is outside the accepted bounds");
        }
        int precision = bytes[clut + 16] & 0xFF;
        if (precision != 1 && precision != 2) {
            throw new IllegalArgumentException("Only 8-bit or 16-bit ICC mAB CLUTs are accepted");
        }
        int clutValues = grid * grid * grid * 3;
        int sampleBytes = precision == 1 ? 1 : 2;
        if (clutOffset + 20 + clutValues * sampleBytes > size) {
            throw new IllegalArgumentException("ICC mAB tag is truncated");
        }
        float[] samples = new float[clutValues];
        int cursor = clut + 20;
        if (precision == 1) {
            for (int index = 0; index < clutValues; index++) {
                samples[index] = (bytes[cursor] & 0xFF) / 255.0f;
                cursor++;
            }
        } else {
            for (int index = 0; index < clutValues; index++) {
                samples[index] = u16(bytes, cursor) / 65535.0f;
                cursor += 2;
            }
        }
        return new IccClut(
                grid,
                samples,
                a.gammas[0],
                a.gammas[1],
                a.gammas[2],
                matrix,
                b.gammas[0],
                b.gammas[1],
                b.gammas[2],
                m.gammas[0],
                m.gammas[1],
                m.gammas[2],
                inverse,
                new Tables(
                        a.tables[0],
                        a.tables[1],
                        a.tables[2],
                        b.tables[0],
                        b.tables[1],
                        b.tables[2],
                        m.tables[0],
                        m.tables[1],
                        m.tables[2]
                ),
                new ParaStages(
                        a.paras[0],
                        a.paras[1],
                        a.paras[2],
                        b.paras[0],
                        b.paras[1],
                        b.paras[2],
                        m.paras[0],
                        m.paras[1],
                        m.paras[2]
                )
        );
    }

    /// Parses a v4 `lutBToAType` (`mBA `) tag with a cubic 8-bit or 16-bit 3×3 CLUT.
    ///
    /// Optional B, M, and A stages may be identity, single-gamma, tabulated `curv`,
    /// or `para` functions 0–4. The optional 3×4 matrix is applied before M curves. Grid sizes above
    /// [`#MAX_GRID`] are rejected.
    ///
    /// @param bytes the profile bytes
    /// @param offset the tag offset
    /// @param size the tag size
    /// @return the CLUT
    public static IccClut parseMba(byte[] bytes, int offset, int size) {
        return parseLutAB(bytes, offset, size, TYPE_MBA, true);
    }

    /// Parses a v2 `lut8Type` (`mft1`) tag with a 3×3 CLUT.
    ///
    /// An optional 3×3 s15.16 matrix at bytes 12–47 runs before planar input tables of
    /// 256 entries; planar output tables of 256 entries run after interpolation. An
    /// all-zero matrix is treated as identity. Grid sizes above [`#MAX_GRID`] are
    /// rejected. Sample values are normalized from 8-bit codes.
    ///
    /// @param bytes the profile bytes
    /// @param offset the tag offset
    /// @param size the tag size
    /// @return the CLUT
    public static IccClut parseMft1(byte[] bytes, int offset, int size) {
        Objects.requireNonNull(bytes, "bytes");
        if (offset < 0 || size < 48 || offset > bytes.length - size) {
            throw new IllegalArgumentException("ICC mft1 tag is outside the profile");
        }
        if (u32(bytes, offset) != TYPE_MFT1) {
            throw new IllegalArgumentException("ICC LUT type is not mft1");
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
        int required = 48 + inputTable + clutValues + outputTable;
        if (size < required) {
            throw new IllegalArgumentException("ICC mft1 tag is truncated");
        }
        int cursor = offset + 48;
        float[][] input = readPlanar8Tables(bytes, cursor);
        cursor += inputTable;
        float[] samples = new float[clutValues];
        for (int index = 0; index < clutValues; index++) {
            samples[index] = (bytes[cursor] & 0xFF) / 255.0f;
            cursor++;
        }
        float[][] output = readPlanar8Tables(bytes, cursor);
        return new IccClut(
                grid,
                samples,
                1.0f,
                1.0f,
                1.0f,
                readMft3x3(bytes, offset),
                1.0f,
                1.0f,
                1.0f,
                1.0f,
                1.0f,
                1.0f,
                false,
                tablesFromMft(input, output),
                ParaStages.none()
        );
    }

    /// Parses a v2 `lut16Type` (`mft2`) tag with a 3×3 CLUT.
    ///
    /// An optional 3×3 s15.16 matrix at bytes 12–47 runs before planar input tables of
    /// 256 entries; planar output tables of 256 entries run after interpolation. An
    /// all-zero matrix is treated as identity. Grid sizes above [`#MAX_GRID`] are
    /// rejected.
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
        float[][] input = readPlanar16Tables(bytes, cursor);
        cursor += inputTable * 2;
        float[] samples = new float[clutValues];
        for (int index = 0; index < clutValues; index++) {
            samples[index] = u16(bytes, cursor) / 65535.0f;
            cursor += 2;
        }
        float[][] output = readPlanar16Tables(bytes, cursor);
        return new IccClut(
                grid,
                samples,
                1.0f,
                1.0f,
                1.0f,
                readMft3x3(bytes, offset),
                1.0f,
                1.0f,
                1.0f,
                1.0f,
                1.0f,
                1.0f,
                false,
                tablesFromMft(input, output),
                ParaStages.none()
        );
    }

    /// Reads a 3×3 s15.16 matrix from `mft1`/`mft2` bytes 12–47.
    ///
    /// @param bytes the profile bytes
    /// @param offset the tag offset
    /// @return nine entries, or empty when every entry is zero
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

    /// Reads three planar 8-bit 256-entry tables.
    ///
    /// @param bytes the profile bytes
    /// @param cursor the first table byte
    /// @return three 256-entry tables
    private static float[][] readPlanar8Tables(byte[] bytes, int cursor) {
        float[][] tables = new float[3][256];
        int offset = cursor;
        for (int channel = 0; channel < 3; channel++) {
            for (int index = 0; index < 256; index++) {
                tables[channel][index] = (bytes[offset++] & 0xFF) / 255.0f;
            }
        }
        return tables;
    }

    /// Reads three planar 16-bit 256-entry tables.
    ///
    /// @param bytes the profile bytes
    /// @param cursor the first table byte
    /// @return three 256-entry tables
    private static float[][] readPlanar16Tables(byte[] bytes, int cursor) {
        float[][] tables = new float[3][256];
        int offset = cursor;
        for (int channel = 0; channel < 3; channel++) {
            for (int index = 0; index < 256; index++) {
                tables[channel][index] = u16(bytes, offset) / 65535.0f;
                offset += 2;
            }
        }
        return tables;
    }

    /// Builds A/B tables from mft input and output planes.
    ///
    /// @param input planar input tables
    /// @param output planar output tables
    /// @return the stored tables
    private static Tables tablesFromMft(float[][] input, float[][] output) {
        return new Tables(
                input[0],
                input[1],
                input[2],
                output[0],
                output[1],
                output[2],
                EMPTY_TABLE,
                EMPTY_TABLE,
                EMPTY_TABLE
        );
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
        if (inverse) {
            float x = applyCurve(Math.clamp(red, 0.0f, 1.0f), bRed, tables.bRed(), para.bRed());
            float y = applyCurve(Math.clamp(green, 0.0f, 1.0f), bGreen, tables.bGreen(), para.bGreen());
            float z = applyCurve(Math.clamp(blue, 0.0f, 1.0f), bBlue, tables.bBlue(), para.bBlue());
            float[] mapped = applyMatrix(x, y, z);
            mapped[0] = applyCurve(mapped[0], mRed, tables.mRed(), para.mRed());
            mapped[1] = applyCurve(mapped[1], mGreen, tables.mGreen(), para.mGreen());
            mapped[2] = applyCurve(mapped[2], mBlue, tables.mBlue(), para.mBlue());
            float[] device = interpolate(
                    Math.clamp(mapped[0], 0.0f, 1.0f),
                    Math.clamp(mapped[1], 0.0f, 1.0f),
                    Math.clamp(mapped[2], 0.0f, 1.0f)
            );
            device[0] = applyCurve(device[0], aRed, tables.aRed(), para.aRed());
            device[1] = applyCurve(device[1], aGreen, tables.aGreen(), para.aGreen());
            device[2] = applyCurve(device[2], aBlue, tables.aBlue(), para.aBlue());
            return device;
        }
        float inRed = Math.clamp(red, 0.0f, 1.0f);
        float inGreen = Math.clamp(green, 0.0f, 1.0f);
        float inBlue = Math.clamp(blue, 0.0f, 1.0f);
        if (matrix.length == 9) {
            float[] mapped = applyMft3x3(inRed, inGreen, inBlue);
            inRed = Math.clamp(mapped[0], 0.0f, 1.0f);
            inGreen = Math.clamp(mapped[1], 0.0f, 1.0f);
            inBlue = Math.clamp(mapped[2], 0.0f, 1.0f);
        }
        float[] xyz = interpolate(
                applyCurve(inRed, aRed, tables.aRed(), para.aRed()),
                applyCurve(inGreen, aGreen, tables.aGreen(), para.aGreen()),
                applyCurve(inBlue, aBlue, tables.aBlue(), para.aBlue())
        );
        xyz[0] = applyCurve(xyz[0], mRed, tables.mRed(), para.mRed());
        xyz[1] = applyCurve(xyz[1], mGreen, tables.mGreen(), para.mGreen());
        xyz[2] = applyCurve(xyz[2], mBlue, tables.mBlue(), para.mBlue());
        if (matrix.length == 12) {
            xyz = applyMatrix(xyz[0], xyz[1], xyz[2]);
        }
        xyz[0] = applyCurve(xyz[0], bRed, tables.bRed(), para.bRed());
        xyz[1] = applyCurve(xyz[1], bGreen, tables.bGreen(), para.bGreen());
        xyz[2] = applyCurve(xyz[2], bBlue, tables.bBlue(), para.bBlue());
        return xyz;
    }

    /// Interpolates the CLUT at unit-cube coordinates.
    ///
    /// @param red the red coordinate in `[0, 1]`
    /// @param green the green coordinate in `[0, 1]`
    /// @param blue the blue coordinate in `[0, 1]`
    /// @return the interpolated sample
    private float[] interpolate(float red, float green, float blue) {
        float r = red * (grid - 1);
        float g = green * (grid - 1);
        float b = blue * (grid - 1);
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

    /// Applies a 3×3 `mft1`/`mft2` matrix.
    ///
    /// @param x the first component
    /// @param y the second component
    /// @param z the third component
    /// @return the mapped triple
    private float[] applyMft3x3(float x, float y, float z) {
        return new float[] {
                Math.fma(matrix[0], x, Math.fma(matrix[1], y, matrix[2] * z)),
                Math.fma(matrix[3], x, Math.fma(matrix[4], y, matrix[5] * z)),
                Math.fma(matrix[6], x, Math.fma(matrix[7], y, matrix[8] * z))
        };
    }

    /// Applies the 3×4 matrix, or returns the input when the matrix is absent.
    ///
    /// @param x the first component
    /// @param y the second component
    /// @param z the third component
    /// @return the mapped triple
    private float[] applyMatrix(float x, float y, float z) {
        if (matrix.length != 12) {
            return new float[] {x, y, z};
        }
        return new float[] {
                Math.fma(matrix[0], x, Math.fma(matrix[1], y, Math.fma(matrix[2], z, matrix[9]))),
                Math.fma(matrix[3], x, Math.fma(matrix[4], y, Math.fma(matrix[5], z, matrix[10]))),
                Math.fma(matrix[6], x, Math.fma(matrix[7], y, Math.fma(matrix[8], z, matrix[11])))
        };
    }

    /// Applies a parametric, tabulated, or power-law curve.
    ///
    /// @param value the component
    /// @param gamma the exponent used when `table` is empty and `para` is unused
    /// @param table tabulated samples, empty to use `gamma`
    /// @param para parametric stage; function `0` uses `table` or `gamma`
    /// @return the mapped component
    private static float applyCurve(float value, float gamma, float[] table, Para para) {
        float unit = Math.clamp(value, 0.0f, 1.0f);
        if (para.function() > 0) {
            return applyPara(unit, para);
        }
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

    /// Applies ICC parametric function types 1–4.
    ///
    /// @param x the unit component
    /// @param para the stage
    /// @return the mapped component
    private static float applyPara(float x, Para para) {
        float g = para.gamma();
        float a = para.a();
        float b = para.b();
        float c = para.c();
        float d = para.d();
        float e = para.e();
        float f = para.f();
        return switch (para.function()) {
            case 1 -> powNonNeg(Math.fma(a, x, b), g);
            case 2 -> x >= (a == 0.0f ? 0.0f : -b / a) ? powNonNeg(Math.fma(a, x, b), g) + c : c;
            case 3 -> x >= d ? powNonNeg(Math.fma(a, x, b), g) : c * x;
            case 4 -> x >= d ? powNonNeg(Math.fma(a, x, b), g) + e : Math.fma(c, x, f);
            default -> x;
        };
    }

    /// Returns `max(base, 0)^exponent`.
    ///
    /// @param base the power base
    /// @param exponent the exponent
    /// @return the power
    private static float powNonNeg(float base, float exponent) {
        return (float) Math.pow(Math.max(base, 0.0f), exponent);
    }

    /// Parsed A, B, or M curves.
    private static final class CurveTriple {
        /// Identity or gamma exponents.
        private final float[] gammas;

        /// Tabulated samples, empty to use gamma.
        private final float[][] tables;

        /// Parametric stages, [`Para#NONE`] to use gamma or tables.
        private final Para[] paras;

        /// Creates one triple.
        ///
        /// @param gammas three exponents
        /// @param tables three tables
        /// @param paras three parametric stages
        private CurveTriple(float[] gammas, float[][] tables, Para[] paras) {
            this.gammas = gammas;
            this.tables = tables;
            this.paras = paras;
        }
    }

    /// Reads three `curv` elements, or identity when `relative` is `0`.
    ///
    /// @param bytes the profile bytes
    /// @param tagOffset the `mAB ` tag origin
    /// @param relative the offset from the tag origin
    /// @param limit exclusive end of the tag
    /// @return the three curves
    private static CurveTriple readMabCurves(byte[] bytes, int tagOffset, int relative, int limit) {
        if (relative == 0) {
            return new CurveTriple(
                    new float[] {1.0f, 1.0f, 1.0f},
                    new float[][] {EMPTY_TABLE, EMPTY_TABLE, EMPTY_TABLE},
                    new Para[] {Para.NONE, Para.NONE, Para.NONE}
            );
        }
        int cursor = tagOffset + relative;
        float[] gammas = new float[3];
        float[][] tables = new float[3][];
        Para[] paras = new Para[] {Para.NONE, Para.NONE, Para.NONE};
        for (int channel = 0; channel < 3; channel++) {
            if (cursor + 12 > limit) {
                throw new IllegalArgumentException("ICC mAB curve is truncated");
            }
            int type = u32(bytes, cursor);
            if (type == 0x7061_7261) {
                Para parsed = readPara(bytes, cursor, limit);
                gammas[channel] = parsed.gamma();
                tables[channel] = EMPTY_TABLE;
                paras[channel] = parsed.function() == 0 ? Para.NONE : parsed;
                int params = paraParameterCount(parsed.function());
                cursor += 12 + params * 4;
                cursor = (cursor + 3) & ~3;
                continue;
            }
            if (type != 0x6375_7276) {
                throw new IllegalArgumentException("ICC mAB curve type is not curv");
            }
            int count = u32(bytes, cursor + 8);
            if (count == 0) {
                gammas[channel] = 1.0f;
                tables[channel] = EMPTY_TABLE;
                cursor += 12;
            } else if (count == 1) {
                if (cursor + 14 > limit) {
                    throw new IllegalArgumentException("ICC mAB gamma curve is truncated");
                }
                float gamma = u16(bytes, cursor + 12) / 256.0f;
                if (!(gamma > 0.0f) || !Float.isFinite(gamma)) {
                    throw new IllegalArgumentException("ICC mAB gamma must be finite and positive");
                }
                gammas[channel] = gamma;
                tables[channel] = EMPTY_TABLE;
                cursor += 14;
            } else {
                if (count > MAX_CURVE_ENTRIES) {
                    throw new IllegalArgumentException("ICC tabulated curve exceeds the accepted bound");
                }
                int required = 12 + count * 2;
                if (cursor + required > limit) {
                    throw new IllegalArgumentException("ICC tabulated curve is truncated");
                }
                float[] table = new float[count];
                for (int index = 0; index < count; index++) {
                    table[index] = u16(bytes, cursor + 12 + index * 2) / 65535.0f;
                }
                gammas[channel] = 1.0f;
                tables[channel] = table;
                cursor += required;
            }
            cursor = (cursor + 3) & ~3;
        }
        return new CurveTriple(gammas, tables, paras);
    }

    /// Returns the s15.16 parameter count for one `para` function.
    ///
    /// @param function `0` through `4`
    /// @return the parameter count
    private static int paraParameterCount(int function) {
        return switch (function) {
            case 0 -> 1;
            case 1 -> 3;
            case 2 -> 4;
            case 3 -> 5;
            default -> 7;
        };
    }

    /// Reads one `para` element.
    ///
    /// @param bytes the profile bytes
    /// @param cursor the element origin
    /// @param limit exclusive end of the tag
    /// @return the stage
    private static Para readPara(byte[] bytes, int cursor, int limit) {
        int function = u16(bytes, cursor + 8);
        if (function < 0 || function > 4) {
            throw new IllegalArgumentException("ICC para function must be 0 through 4");
        }
        int required = 12 + paraParameterCount(function) * 4;
        if (cursor + required > limit) {
            throw new IllegalArgumentException("ICC mAB para curve is truncated");
        }
        float g = s15(bytes, cursor + 12);
        float a = function >= 1 ? s15(bytes, cursor + 16) : 1.0f;
        float b = function >= 1 ? s15(bytes, cursor + 20) : 0.0f;
        float c = function >= 2 ? s15(bytes, cursor + 24) : 0.0f;
        float d = function >= 3 ? s15(bytes, cursor + 28) : 0.0f;
        float e = function >= 4 ? s15(bytes, cursor + 32) : 0.0f;
        float f = function >= 4 ? s15(bytes, cursor + 36) : 0.0f;
        return new Para(function, g, a, b, c, d, e, f);
    }

    /// Reads one 3×4 s15.16 matrix, or an empty array when `relative` is `0`.
    ///
    /// @param bytes the profile bytes
    /// @param tagOffset the `mAB ` tag origin
    /// @param relative the offset from the tag origin
    /// @param limit exclusive end of the tag
    /// @return twelve entries, or empty
    private static float[] readMabMatrix(byte[] bytes, int tagOffset, int relative, int limit) {
        if (relative == 0) {
            return new float[0];
        }
        int cursor = tagOffset + relative;
        if (cursor + 48 > limit) {
            throw new IllegalArgumentException("ICC mAB matrix is truncated");
        }
        float[] entries = new float[12];
        for (int index = 0; index < 12; index++) {
            entries[index] = s15(bytes, cursor + index * 4);
        }
        return entries;
    }

    /// Reads a big-endian s15.16 value.
    ///
    /// @param bytes the profile bytes
    /// @param offset the value offset
    /// @return the decoded value
    private static float s15(byte[] bytes, int offset) {
        return u32(bytes, offset) / 65536.0f;
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

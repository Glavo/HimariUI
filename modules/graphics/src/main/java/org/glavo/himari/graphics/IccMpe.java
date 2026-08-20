package org.glavo.himari.graphics;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Arrays;
import java.util.Objects;

/// Stores one bounded ICC `multiProcessElementsType` (`mpet`) transform.
///
/// The accepted subset is a 3-channel pipeline of optional `cvst` formula curves (`curf`
/// type 0 gamma or type 1 `Y = aX` with `gamma=1` and `b=c=d=0`) or 1D `samf` tables, an
/// optional 8-bit `clut` cube, and an optional `matf` 3×4 matrix, in tag order. Other
/// process-element signatures are rejected. Sample values are not clamped. The CLUT uses
/// nearest-neighbour lookup at the stored grid corners.
///
/// @param inputChannels the element input channel count
/// @param outputChannels the element output channel count
/// @param firstGammas per-channel type-0 gammas applied before the CLUT, empty when unused
/// @param firstSampled packed 1D `samf` tables applied before the CLUT, empty when unused
/// @param clutGrid the CLUT edge length, or `0` when unused
/// @param clutSamples packed `grid³×3` CLUT samples, empty when unused
/// @param matrix twelve s15.16 `matf` entries (3×4, row-major), empty when unused
/// @param lastGammas per-channel type-0 gammas applied after the matrix, empty when unused
/// @param lastSampled packed 1D `samf` tables applied after the matrix, empty when unused
/// @param firstScale type-1 `a` linear scales applied before the CLUT, empty when unused
/// @param lastScale type-1 `a` linear scales applied after the matrix, empty when unused
@NotNullByDefault
public record IccMpe(
        int inputChannels,
        int outputChannels,
        float @Unmodifiable [] firstGammas,
        float @Unmodifiable [] firstSampled,
        int clutGrid,
        float @Unmodifiable [] clutSamples,
        float @Unmodifiable [] matrix,
        float @Unmodifiable [] lastGammas,
        float @Unmodifiable [] lastSampled,
        float @Unmodifiable [] firstScale,
        float @Unmodifiable [] lastScale
) {
    /// Type `'mpet'`.
    public static final int TYPE_MPET = 0x6D70_6574;

    /// Element `'cvst'`.
    public static final int TYPE_CVST = 0x6376_7374;

    /// Element `'matf'`.
    public static final int TYPE_MATF = 0x6D61_7466;

    /// Element `'clut'`.
    public static final int TYPE_CLUT = 0x636C_7574;

    /// Curve `'curf'`.
    public static final int TYPE_CURF = 0x6375_7266;

    /// Curve `'samf'`.
    public static final int TYPE_SAMF = 0x7361_6D66;

    /// Maximum 1D `samf` entries per channel.
    public static final int MAX_SAMPLED = 256;

    /// Tag `'B2D0'`.
    public static final int TAG_B2D0 = 0x4232_4430;

    /// Tag `'B2D1'`.
    public static final int TAG_B2D1 = 0x4232_4431;

    /// Tag `'B2D2'`.
    public static final int TAG_B2D2 = 0x4232_4432;

    /// Tag `'B2D3'`.
    public static final int TAG_B2D3 = 0x4232_4433;

    /// Tag `'D2B0'`.
    public static final int TAG_D2B0 = 0x4432_4230;

    /// Tag `'D2B1'`.
    public static final int TAG_D2B1 = 0x4432_4231;

    /// Tag `'D2B2'`.
    public static final int TAG_D2B2 = 0x4432_4232;

    /// Tag `'D2B3'`.
    public static final int TAG_D2B3 = 0x4432_4233;

    /// Maximum accepted channels.
    public static final int MAX_CHANNELS = 3;

    /// Maximum accepted processing elements.
    public static final int MAX_ELEMENTS = 4;

    /// Maximum accepted CLUT edge.
    public static final int MAX_CLUT_GRID = 5;

    /// Empty optional table.
    private static final float[] EMPTY = new float[0];

    /// Minimum `mpet` header size including one empty offset slot.
    private static final int MIN_SIZE = 16;

    /// Validates the pipeline.
    public IccMpe {
        Objects.requireNonNull(firstGammas, "firstGammas");
        Objects.requireNonNull(firstSampled, "firstSampled");
        Objects.requireNonNull(clutSamples, "clutSamples");
        Objects.requireNonNull(matrix, "matrix");
        Objects.requireNonNull(lastGammas, "lastGammas");
        Objects.requireNonNull(lastSampled, "lastSampled");
        Objects.requireNonNull(firstScale, "firstScale");
        Objects.requireNonNull(lastScale, "lastScale");
        if (inputChannels != MAX_CHANNELS || outputChannels != MAX_CHANNELS) {
            throw new IllegalArgumentException("ICC mpet must be 3×3");
        }
        firstGammas = copyGammas(firstGammas, "firstGammas");
        lastGammas = copyGammas(lastGammas, "lastGammas");
        firstSampled = copySampled(firstSampled, "firstSampled");
        lastSampled = copySampled(lastSampled, "lastSampled");
        firstScale = copyScale(firstScale, "firstScale");
        lastScale = copyScale(lastScale, "lastScale");
        clutSamples = copyClut(clutGrid, clutSamples);
        if (matrix.length != 0 && matrix.length != 12) {
            throw new IllegalArgumentException("ICC matf must be empty or twelve entries");
        }
        for (float value : matrix) {
            if (!Float.isFinite(value)) {
                throw new IllegalArgumentException("ICC matf entries must be finite");
            }
        }
        matrix = Arrays.copyOf(matrix, matrix.length);
    }

    /// Parses one `mpet` tag.
    ///
    /// @param bytes the profile bytes
    /// @param offset the tag start
    /// @param size the tag size
    /// @return the pipeline
    public static IccMpe parse(byte[] bytes, int offset, int size) {
        Objects.requireNonNull(bytes, "bytes");
        if (offset < 0 || size < MIN_SIZE || offset > bytes.length - size) {
            throw new IllegalArgumentException("ICC mpet tag is outside the profile");
        }
        if (u32(bytes, offset) != TYPE_MPET) {
            throw new IllegalArgumentException("ICC mpet tag is not mpet");
        }
        int inputChannels = u16(bytes, offset + 8);
        int outputChannels = u16(bytes, offset + 10);
        int elements = u16(bytes, offset + 12);
        if (inputChannels != MAX_CHANNELS || outputChannels != MAX_CHANNELS) {
            throw new IllegalArgumentException("ICC mpet must be 3×3");
        }
        if (elements <= 0 || elements > MAX_ELEMENTS) {
            throw new IllegalArgumentException("ICC mpet element count is outside the accepted bounds");
        }
        int table = 16 + elements * 8;
        if (size < table) {
            throw new IllegalArgumentException("ICC mpet element table exceeds the tag");
        }
        float[] firstGammas = EMPTY;
        float[] firstSampled = EMPTY;
        float[] matrix = EMPTY;
        float[] lastGammas = EMPTY;
        float[] lastSampled = EMPTY;
        float[] firstScale = EMPTY;
        float[] lastScale = EMPTY;
        int clutGrid = 0;
        float[] clutSamples = EMPTY;
        boolean seenMatrix = false;
        boolean seenClut = false;
        for (int index = 0; index < elements; index++) {
            int elementOffset = u32(bytes, offset + 16 + index * 8);
            int elementSize = u32(bytes, offset + 20 + index * 8);
            if (elementOffset < 0 || elementSize < 8 || elementOffset > size - elementSize) {
                throw new IllegalArgumentException("ICC mpet element is outside the tag");
            }
            int start = offset + elementOffset;
            int type = u32(bytes, start);
            if (type == TYPE_CVST) {
                ParsedCurves curves = parseCurveSet(bytes, start, elementSize, inputChannels);
                if (!seenMatrix && !seenClut && firstGammas.length == 0 && firstSampled.length == 0
                        && firstScale.length == 0) {
                    firstGammas = curves.gammas();
                    firstSampled = curves.sampled();
                    firstScale = curves.scale();
                } else if ((seenMatrix || seenClut) && lastGammas.length == 0 && lastSampled.length == 0
                        && lastScale.length == 0) {
                    lastGammas = curves.gammas();
                    lastSampled = curves.sampled();
                    lastScale = curves.scale();
                } else {
                    throw new IllegalArgumentException("ICC mpet has too many cvst elements");
                }
            } else if (type == TYPE_MATF) {
                if (seenMatrix) {
                    throw new IllegalArgumentException("ICC mpet has more than one matf element");
                }
                matrix = parseMatrix(bytes, start, elementSize);
                seenMatrix = true;
            } else if (type == TYPE_CLUT) {
                if (seenClut) {
                    throw new IllegalArgumentException("ICC mpet has more than one clut element");
                }
                ParsedClut parsed = parseClut(bytes, start, elementSize);
                clutGrid = parsed.grid();
                clutSamples = parsed.samples();
                seenClut = true;
            } else {
                throw new IllegalArgumentException("ICC mpet element is not cvst, clut, or matf");
            }
        }
        return new IccMpe(
                inputChannels,
                outputChannels,
                firstGammas,
                firstSampled,
                clutGrid,
                clutSamples,
                matrix,
                lastGammas,
                lastSampled,
                firstScale,
                lastScale
        );
    }

    /// Transforms one 3-channel sample through the stored element order.
    ///
    /// Curves before the CLUT run first, then nearest-neighbour CLUT lookup, then the
    /// matrix, then trailing curves.
    ///
    /// @param c0 the first channel
    /// @param c1 the second channel
    /// @param c2 the third channel
    /// @return the three outputs
    public float[] transform(float c0, float c1, float c2) {
        if (!Float.isFinite(c0) || !Float.isFinite(c1) || !Float.isFinite(c2)) {
            throw new IllegalArgumentException("ICC mpet sample must be finite");
        }
        float[] sample = {c0, c1, c2};
        applyCurves(sample, firstGammas, firstSampled, firstScale);
        applyClut(sample, clutGrid, clutSamples);
        applyMatrix(sample, matrix);
        applyCurves(sample, lastGammas, lastSampled, lastScale);
        return sample;
    }

    /// One parsed `cvst` of type-0 `curf` gammas, type-1 linear scales, or packed `samf` tables.
    ///
    /// @param gammas type-0 exponents, empty when sampled or scaled
    /// @param sampled packed 1D tables, empty when gamma or scaled
    /// @param scale type-1 `a` coefficients, empty when gamma or sampled
    private record ParsedCurves(float[] gammas, float[] sampled, float[] scale) {
    }

    /// Parses one `cvst` of type-0 `curf` gammas, type-1 linear `aX`, or 1D `samf` tables.
    private static ParsedCurves parseCurveSet(byte[] bytes, int offset, int size, int channels) {
        if (size < 8 + channels * 16) {
            throw new IllegalArgumentException("ICC cvst is truncated");
        }
        int kind = u32(bytes, offset + 8);
        if (kind == TYPE_SAMF) {
            return new ParsedCurves(EMPTY, parseSampled(bytes, offset + 8, size - 8, channels), EMPTY);
        }
        int function = u16(bytes, offset + 16);
        if (function == 1) {
            return new ParsedCurves(EMPTY, EMPTY, parseType1(bytes, offset + 8, size - 8, channels));
        }
        float[] gammas = new float[channels];
        int cursor = offset + 8;
        for (int index = 0; index < channels; index++) {
            if (u32(bytes, cursor) != TYPE_CURF) {
                throw new IllegalArgumentException("ICC cvst curve is not curf or samf");
            }
            if (u16(bytes, cursor + 8) != 0) {
                throw new IllegalArgumentException("ICC curf function type must be 0 or 1");
            }
            float gamma = s15(bytes, cursor + 12);
            if (!Float.isFinite(gamma) || gamma <= 0.0f) {
                throw new IllegalArgumentException("ICC curf gamma must be finite and positive");
            }
            gammas[index] = gamma;
            cursor += 16;
        }
        return new ParsedCurves(gammas, EMPTY, EMPTY);
    }

    /// Parses type-1 `curf` curves `Y = aX` (`gamma=1`, `b=c=d=0`).
    private static float[] parseType1(byte[] bytes, int offset, int size, int channels) {
        if (size < channels * 32) {
            throw new IllegalArgumentException("ICC curf type 1 is truncated");
        }
        float[] scale = new float[channels];
        int cursor = offset;
        for (int index = 0; index < channels; index++) {
            if (u32(bytes, cursor) != TYPE_CURF) {
                throw new IllegalArgumentException("ICC cvst type-1 curve is not curf");
            }
            if (u16(bytes, cursor + 8) != 1) {
                throw new IllegalArgumentException("ICC curf type-1 function type must be 1");
            }
            float gamma = s15(bytes, cursor + 12);
            float a = s15(bytes, cursor + 16);
            float b = s15(bytes, cursor + 20);
            float c = s15(bytes, cursor + 24);
            float d = s15(bytes, cursor + 28);
            if (Math.abs(gamma - 1.0f) > 1.0e-4f || Math.abs(b) > 1.0e-4f
                    || Math.abs(c) > 1.0e-4f || Math.abs(d) > 1.0e-4f) {
                throw new IllegalArgumentException("ICC curf type 1 must be Y=aX");
            }
            if (!Float.isFinite(a)) {
                throw new IllegalArgumentException("ICC curf type 1 a must be finite");
            }
            scale[index] = a;
            cursor += 32;
        }
        return scale;
    }

    /// Parses `channels` packed 1D `samf` tables.
    private static float[] parseSampled(byte[] bytes, int offset, int size, int channels) {
        float[] sampled = new float[0];
        int cursor = offset;
        int remaining = size;
        for (int channel = 0; channel < channels; channel++) {
            if (remaining < 16 || u32(bytes, cursor) != TYPE_SAMF) {
                throw new IllegalArgumentException("ICC cvst sampled curve is not samf");
            }
            int count = u16(bytes, cursor + 8);
            if (count < 2 || count > MAX_SAMPLED) {
                throw new IllegalArgumentException("ICC samf count is outside the accepted bounds");
            }
            int bytesForSamples = count * 2;
            int padded = (bytesForSamples + 3) & ~3;
            if (remaining < 12 + padded) {
                throw new IllegalArgumentException("ICC samf is truncated");
            }
            if (channel == 0) {
                sampled = new float[channels * count];
            } else if (count != sampled.length / channels) {
                throw new IllegalArgumentException("ICC samf tables must share one length");
            }
            int sampleCursor = cursor + 12;
            for (int index = 0; index < count; index++) {
                sampled[channel * count + index] = u16(bytes, sampleCursor) / 65535.0f;
                sampleCursor += 2;
            }
            cursor += 12 + padded;
            remaining -= 12 + padded;
        }
        return sampled;
    }

    /// Parses one 3×4 `matf`.
    private static float[] parseMatrix(byte[] bytes, int offset, int size) {
        if (size < 8 + 12 * 4) {
            throw new IllegalArgumentException("ICC matf is truncated");
        }
        float[] matrix = new float[12];
        for (int index = 0; index < 12; index++) {
            matrix[index] = s15(bytes, offset + 8 + index * 4);
            if (!Float.isFinite(matrix[index])) {
                throw new IllegalArgumentException("ICC matf entries must be finite");
            }
        }
        return matrix;
    }

    /// One parsed 8-bit cubic CLUT.
    ///
    /// @param grid the edge length
    /// @param samples packed samples
    private record ParsedClut(int grid, float[] samples) {
    }

    /// Parses one 8-bit 3-channel `clut`.
    ///
    /// @return the grid and samples
    private static ParsedClut parseClut(byte[] bytes, int offset, int size) {
        if (size < 28) {
            throw new IllegalArgumentException("ICC clut is truncated");
        }
        int grid = bytes[offset + 8] & 0xFF;
        if (grid < 2 || grid > MAX_CLUT_GRID) {
            throw new IllegalArgumentException("ICC clut grid is outside the accepted bounds");
        }
        if ((bytes[offset + 9] & 0xFF) != grid || (bytes[offset + 10] & 0xFF) != grid) {
            throw new IllegalArgumentException("ICC clut must be a cubic 3-channel grid");
        }
        if ((bytes[offset + 24] & 0xFF) != 1) {
            throw new IllegalArgumentException("ICC clut precision must be 8-bit");
        }
        int count = grid * grid * grid * MAX_CHANNELS;
        if (size < 28 + count) {
            throw new IllegalArgumentException("ICC clut samples exceed the element");
        }
        float[] samples = new float[count];
        int cursor = offset + 28;
        for (int index = 0; index < count; index++) {
            samples[index] = (bytes[cursor++] & 0xFF) / 255.0f;
        }
        return new ParsedClut(grid, samples);
    }

    /// Applies sampled 1D tables, type-1 linear scales, or type-0 gammas.
    private static void applyCurves(float[] sample, float[] gammas, float[] sampled, float[] scale) {
        if (sampled.length > 0) {
            applySampled(sample, sampled);
            return;
        }
        if (scale.length > 0) {
            for (int index = 0; index < sample.length; index++) {
                sample[index] *= scale[index];
            }
            return;
        }
        applyGammas(sample, gammas);
    }

    /// Interpolates packed per-channel 1D tables.
    private static void applySampled(float[] sample, float[] sampled) {
        int count = sampled.length / MAX_CHANNELS;
        if (count < 2) {
            return;
        }
        float last = count - 1;
        for (int channel = 0; channel < MAX_CHANNELS; channel++) {
            float t = Math.clamp(sample[channel], 0.0f, 1.0f) * last;
            int lo = (int) t;
            int hi = Math.min(lo + 1, count - 1);
            float f = t - lo;
            float a = sampled[channel * count + lo];
            float b = sampled[channel * count + hi];
            sample[channel] = a + f * (b - a);
        }
    }

    /// Applies per-channel `encoded^gamma`.
    private static void applyGammas(float[] sample, float[] gammas) {
        if (gammas.length == 0) {
            return;
        }
        for (int index = 0; index < sample.length; index++) {
            float gamma = gammas[index];
            if (gamma == 1.0f) {
                continue;
            }
            sample[index] = (float) Math.pow(Math.max(sample[index], 0.0f), gamma);
        }
    }

    /// Applies nearest-neighbour CLUT lookup, or does nothing when unused.
    private static void applyClut(float[] sample, int grid, float[] samples) {
        if (grid < 2) {
            return;
        }
        int last = grid - 1;
        int i0 = Math.clamp(Math.round(sample[0] * last), 0, last);
        int i1 = Math.clamp(Math.round(sample[1] * last), 0, last);
        int i2 = Math.clamp(Math.round(sample[2] * last), 0, last);
        int index = ((i0 * grid + i1) * grid + i2) * MAX_CHANNELS;
        sample[0] = samples[index];
        sample[1] = samples[index + 1];
        sample[2] = samples[index + 2];
    }

    /// Applies a 3×4 row-major matrix, or does nothing when empty.
    private static void applyMatrix(float[] sample, float[] matrix) {
        if (matrix.length == 0) {
            return;
        }
        float c0 = sample[0];
        float c1 = sample[1];
        float c2 = sample[2];
        sample[0] = matrix[0] * c0 + matrix[1] * c1 + matrix[2] * c2 + matrix[3];
        sample[1] = matrix[4] * c0 + matrix[5] * c1 + matrix[6] * c2 + matrix[7];
        sample[2] = matrix[8] * c0 + matrix[9] * c1 + matrix[10] * c2 + matrix[11];
    }

    /// Copies type-1 linear scales or returns the shared empty array.
    private static float[] copyScale(float[] scale, String name) {
        if (scale.length == 0) {
            return EMPTY;
        }
        if (scale.length != MAX_CHANNELS) {
            throw new IllegalArgumentException("ICC mpet " + name + " must be empty or three entries");
        }
        for (float value : scale) {
            if (!Float.isFinite(value)) {
                throw new IllegalArgumentException("ICC mpet scales must be finite");
            }
        }
        return Arrays.copyOf(scale, scale.length);
    }

    /// Copies packed 1D tables or returns the shared empty array.
    private static float[] copySampled(float[] sampled, String name) {
        if (sampled.length == 0) {
            return EMPTY;
        }
        if (sampled.length % MAX_CHANNELS != 0) {
            throw new IllegalArgumentException("ICC mpet " + name + " must be packed 3-channel tables");
        }
        int count = sampled.length / MAX_CHANNELS;
        if (count < 2 || count > MAX_SAMPLED) {
            throw new IllegalArgumentException("ICC mpet " + name + " length is outside the accepted bounds");
        }
        for (float value : sampled) {
            if (!Float.isFinite(value)) {
                throw new IllegalArgumentException("ICC mpet sampled entries must be finite");
            }
        }
        return Arrays.copyOf(sampled, sampled.length);
    }

    /// Copies a gamma table or returns the shared empty array.
    private static float[] copyGammas(float[] gammas, String name) {
        if (gammas.length == 0) {
            return EMPTY;
        }
        if (gammas.length != MAX_CHANNELS) {
            throw new IllegalArgumentException("ICC mpet " + name + " must be empty or three entries");
        }
        for (float gamma : gammas) {
            if (!Float.isFinite(gamma) || gamma <= 0.0f) {
                throw new IllegalArgumentException("ICC mpet gammas must be finite and positive");
            }
        }
        return Arrays.copyOf(gammas, gammas.length);
    }

    /// Copies a CLUT table or returns the shared empty array.
    private static float[] copyClut(int grid, float[] samples) {
        if (grid == 0) {
            if (samples.length != 0) {
                throw new IllegalArgumentException("ICC mpet clut samples must be empty when unused");
            }
            return EMPTY;
        }
        if (grid < 2 || grid > MAX_CLUT_GRID) {
            throw new IllegalArgumentException("ICC mpet clut grid is outside the accepted bounds");
        }
        int expected = grid * grid * grid * MAX_CHANNELS;
        if (samples.length != expected) {
            throw new IllegalArgumentException("ICC mpet clut sample count does not match the grid");
        }
        for (float value : samples) {
            if (!Float.isFinite(value)) {
                throw new IllegalArgumentException("ICC mpet clut samples must be finite");
            }
        }
        return Arrays.copyOf(samples, samples.length);
    }

    /// Reads a big-endian unsigned 32-bit integer.
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
}

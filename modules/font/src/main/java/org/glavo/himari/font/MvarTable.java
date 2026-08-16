package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/// Parses a first-stable `MVAR` subset for the `hasc` horizontal-ascender delta.
///
/// Supported stores use one value record, one item-variation-data subtable, one variation
/// region, and 8-bit deltas. A missing table or missing `hasc` record returns `0`.
@NotNullByDefault
final class MvarTable {
    /// `hasc` tag.
    static final int TAG_HASC = 0x68617363;

    /// Shared empty table.
    static final MvarTable EMPTY = new MvarTable(new Region[0], new byte[0], -1);

    /// Regions referenced by the first item-variation-data subtable.
    private final Region[] regions;

    /// One 8-bit delta per inner index.
    private final byte[] deltas;

    /// Inner index of the `hasc` record, or `-1`.
    private final int hascInner;

    /// Creates a parsed table.
    private MvarTable(Region[] regions, byte[] deltas, int hascInner) {
        this.regions = regions;
        this.deltas = deltas;
        this.hascInner = hascInner;
    }

    /// Parses an optional `MVAR` table.
    ///
    /// @param table the table, or `null`
    /// @param axisCount the `fvar` axis count
    /// @return the parsed table
    static MvarTable parse(@Nullable ByteBuffer table, int axisCount) {
        if (table == null || axisCount < 1) {
            return EMPTY;
        }
        ByteBuffer buffer = table.duplicate().order(ByteOrder.BIG_ENDIAN);
        buffer.clear();
        if (buffer.remaining() < 12) {
            return EMPTY;
        }
        int major = Short.toUnsignedInt(buffer.getShort());
        buffer.getShort();
        buffer.getShort();
        int recordSize = Short.toUnsignedInt(buffer.getShort());
        int recordCount = Short.toUnsignedInt(buffer.getShort());
        int storeOffset = Short.toUnsignedInt(buffer.getShort());
        if (major != 1 || recordSize < 8 || recordCount < 1 || storeOffset < 12) {
            return EMPTY;
        }
        int hascInner = -1;
        for (int index = 0; index < recordCount; index++) {
            if (buffer.remaining() < recordSize) {
                return EMPTY;
            }
            int tag = buffer.getInt();
            buffer.getShort();
            int inner = Short.toUnsignedInt(buffer.getShort());
            int skip = recordSize - 8;
            if (skip > 0) {
                buffer.position(buffer.position() + skip);
            }
            if (tag == TAG_HASC) {
                hascInner = inner;
            }
        }
        if (hascInner < 0 || storeOffset + 12 > buffer.capacity()) {
            return EMPTY;
        }
        buffer.clear();
        buffer.position(storeOffset);
        int format = Short.toUnsignedInt(buffer.getShort());
        int regionListOffset = buffer.getInt();
        int dataCount = Short.toUnsignedInt(buffer.getShort());
        if (format != 1 || dataCount < 1 || buffer.remaining() < 4) {
            return EMPTY;
        }
        int dataOffset = buffer.getInt();
        int regionAbs = storeOffset + regionListOffset;
        int dataAbs = storeOffset + dataOffset;
        if (regionAbs < 0 || dataAbs < 0 || regionAbs + 4 > buffer.capacity()) {
            return EMPTY;
        }
        buffer.clear();
        buffer.position(regionAbs);
        int declaredAxes = Short.toUnsignedInt(buffer.getShort());
        int regionCount = Short.toUnsignedInt(buffer.getShort());
        if (declaredAxes != axisCount || regionCount < 1 || buffer.remaining() < regionCount * axisCount * 6) {
            return EMPTY;
        }
        Region[] allRegions = new Region[regionCount];
        for (int region = 0; region < regionCount; region++) {
            float[] start = new float[axisCount];
            float[] peak = new float[axisCount];
            float[] end = new float[axisCount];
            for (int axis = 0; axis < axisCount; axis++) {
                start[axis] = f2dot14(buffer.getShort());
                peak[axis] = f2dot14(buffer.getShort());
                end[axis] = f2dot14(buffer.getShort());
            }
            allRegions[region] = new Region(start, peak, end);
        }
        if (dataAbs + 8 > buffer.capacity()) {
            return EMPTY;
        }
        buffer.clear();
        buffer.position(dataAbs);
        int itemCount = Short.toUnsignedInt(buffer.getShort());
        int wordDeltaCount = Short.toUnsignedInt(buffer.getShort());
        int regionIndexCount = Short.toUnsignedInt(buffer.getShort());
        if (itemCount < 1 || (wordDeltaCount & 0x7FFF) != 0 || regionIndexCount != 1
                || buffer.remaining() < 2 + itemCount) {
            return EMPTY;
        }
        int regionIndex = Short.toUnsignedInt(buffer.getShort());
        if (regionIndex >= allRegions.length) {
            return EMPTY;
        }
        byte[] deltas = new byte[itemCount];
        buffer.get(deltas);
        return new MvarTable(new Region[] {allRegions[regionIndex]}, deltas, hascInner);
    }

    /// Returns the scaled `hasc` delta.
    ///
    /// @param normalized avar-mapped coordinates
    /// @return the signed delta
    int hascDelta(float[] normalized) {
        if (hascInner < 0 || hascInner >= deltas.length || regions.length == 0) {
            return 0;
        }
        float scalar = regions[0].scalar(normalized);
        if (scalar == 0.0f) {
            return 0;
        }
        return Math.round(scalar * deltas[hascInner]);
    }

    /// Converts an F2DOT14 value to float.
    private static float f2dot14(short value) {
        return value / 16384.0f;
    }

    /// One variation region.
    ///
    /// @param start per-axis start
    /// @param peak per-axis peak
    /// @param end per-axis end
    private record Region(float[] start, float[] peak, float[] end) {
        /// Computes the combined region scalar.
        float scalar(float[] normalized) {
            int axes = Math.min(normalized.length, peak.length);
            float factor = 1.0f;
            for (int index = 0; index < axes; index++) {
                float peakValue = peak[index];
                if (peakValue == 0.0f) {
                    continue;
                }
                float coord = normalized[index];
                float startValue = start[index];
                float endValue = end[index];
                if (coord < startValue || coord > endValue) {
                    return 0.0f;
                }
                if (coord == peakValue) {
                    continue;
                }
                if (coord < peakValue) {
                    float span = peakValue - startValue;
                    if (span == 0.0f) {
                        return 0.0f;
                    }
                    factor *= (coord - startValue) / span;
                } else {
                    float span = endValue - peakValue;
                    if (span == 0.0f) {
                        return 0.0f;
                    }
                    factor *= (endValue - coord) / span;
                }
            }
            return factor;
        }
    }
}

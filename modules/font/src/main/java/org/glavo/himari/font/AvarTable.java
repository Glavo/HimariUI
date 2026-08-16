package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/// Parses an `avar` axis-mapping table and remaps normalized coordinates.
///
/// Only version 1 piecewise-linear maps are retained. A missing or empty table leaves
/// normalized coordinates unchanged. Each axis map is a strictly increasing `from` sequence.
@NotNullByDefault
final class AvarTable {
    /// Shared empty table.
    static final AvarTable EMPTY = new AvarTable(new AxisMap[0]);

    /// One piecewise map per `fvar` axis, or empty.
    private final AxisMap[] axes;

    /// Creates a parsed table.
    private AvarTable(AxisMap[] axes) {
        this.axes = axes;
    }

    /// Parses an optional `avar` table.
    ///
    /// @param table the table, or `null`
    /// @param axisCount the `fvar` axis count
    /// @return the maps
    static AvarTable parse(@Nullable ByteBuffer table, int axisCount) {
        if (table == null || axisCount < 1) {
            return EMPTY;
        }
        ByteBuffer buffer = table.duplicate().order(ByteOrder.BIG_ENDIAN);
        buffer.clear();
        if (buffer.remaining() < 8) {
            return EMPTY;
        }
        int major = Short.toUnsignedInt(buffer.getShort());
        buffer.getShort();
        buffer.getShort();
        int declared = Short.toUnsignedInt(buffer.getShort());
        if (major != 1 || declared != axisCount) {
            return EMPTY;
        }
        AxisMap[] axes = new AxisMap[axisCount];
        for (int axis = 0; axis < axisCount; axis++) {
            if (buffer.remaining() < 2) {
                throw new IllegalArgumentException("avar axis map is truncated");
            }
            int count = Short.toUnsignedInt(buffer.getShort());
            if (buffer.remaining() < count * 4) {
                throw new IllegalArgumentException("avar position map is truncated");
            }
            float[] from = new float[count];
            float[] to = new float[count];
            for (int index = 0; index < count; index++) {
                from[index] = f2dot14(buffer.getShort());
                to[index] = f2dot14(buffer.getShort());
                if (index > 0 && from[index] < from[index - 1]) {
                    throw new IllegalArgumentException("avar from-coordinates must be nondecreasing");
                }
            }
            axes[axis] = new AxisMap(from, to);
        }
        return new AvarTable(axes);
    }

    /// Remaps normalized `[-1, 1]` coordinates through each axis map.
    ///
    /// Extra coordinates are copied. Missing trailing maps leave that coordinate unchanged.
    ///
    /// @param normalized coordinates from [`FvarTable#normalize(float[])`]
    /// @return a new mapped array, or `normalized` when this table is empty
    float[] map(float[] normalized) {
        if (axes.length == 0 || normalized.length == 0) {
            return normalized;
        }
        float[] mapped = new float[normalized.length];
        for (int index = 0; index < normalized.length; index++) {
            mapped[index] = index < axes.length ? axes[index].map(normalized[index]) : normalized[index];
        }
        return mapped;
    }

    /// Converts an F2DOT14 value to float.
    private static float f2dot14(short value) {
        return value / 16384.0f;
    }

    /// One axis piecewise-linear map.
    private record AxisMap(float[] from, float[] to) {
        /// Interpolates `value` through the map.
        float map(float value) {
            if (from.length == 0) {
                return value;
            }
            if (from.length == 1 || value <= from[0]) {
                return to[0];
            }
            int last = from.length - 1;
            if (value >= from[last]) {
                return to[last];
            }
            for (int index = 0; index < last; index++) {
                float left = from[index];
                float right = from[index + 1];
                if (value > right) {
                    continue;
                }
                float span = right - left;
                if (span == 0.0f) {
                    return to[index + 1];
                }
                float t = (value - left) / span;
                return to[index] + t * (to[index + 1] - to[index]);
            }
            return to[last];
        }
    }
}

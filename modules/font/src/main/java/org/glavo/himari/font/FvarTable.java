package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/// Parses the `fvar` axis array used for variable-font instance access.
///
/// Named instances are ignored. A missing table yields no axes and an empty default instance.
@NotNullByDefault
final class FvarTable {
    /// Shared empty table.
    static final FvarTable EMPTY = new FvarTable(new VariationAxis[0]);

    /// Shared empty normalized instance.
    private static final float[] EMPTY_NORMALIZED = new float[0];

    /// Axes in file order.
    private final VariationAxis[] axes;

    /// Creates a parsed table.
    private FvarTable(VariationAxis[] axes) {
        this.axes = axes;
    }

    /// Parses an optional `fvar` table.
    ///
    /// @param table the table, or `null`
    /// @return the axes
    static FvarTable parse(@Nullable ByteBuffer table) {
        if (table == null) {
            return EMPTY;
        }
        ByteBuffer buffer = table.duplicate().order(ByteOrder.BIG_ENDIAN);
        buffer.clear();
        if (buffer.remaining() < 16) {
            throw new IllegalArgumentException("fvar header is truncated");
        }
        int major = Short.toUnsignedInt(buffer.getShort());
        buffer.getShort();
        int axesOffset = Short.toUnsignedInt(buffer.getShort());
        buffer.getShort();
        int axisCount = Short.toUnsignedInt(buffer.getShort());
        int axisSize = Short.toUnsignedInt(buffer.getShort());
        if (major != 1 || axisSize < 20) {
            throw new IllegalArgumentException("Unsupported fvar version or axis size");
        }
        if (axesOffset < 0 || (long) axesOffset + (long) axisCount * (long) axisSize > buffer.capacity()) {
            throw new IllegalArgumentException("fvar axis array is out of range");
        }
        buffer.clear();
        buffer.position(axesOffset);
        VariationAxis[] axes = new VariationAxis[axisCount];
        for (int index = 0; index < axisCount; index++) {
            int start = buffer.position();
            int tag = buffer.getInt();
            float min = fixedToFloat(buffer.getInt());
            float def = fixedToFloat(buffer.getInt());
            float max = fixedToFloat(buffer.getInt());
            axes[index] = new VariationAxis(tag, min, def, max);
            buffer.position(start + axisSize);
        }
        return new FvarTable(axes);
    }

    /// Returns the axes.
    ///
    /// @return the axis list
    @Unmodifiable List<VariationAxis> axes() {
        return Collections.unmodifiableList(Arrays.asList(axes));
    }

    /// Returns one default coordinate per axis.
    ///
    /// @return the default instance
    float @Unmodifiable [] defaultInstance() {
        float[] values = new float[axes.length];
        for (int index = 0; index < axes.length; index++) {
            values[index] = axes[index].defaultValue();
        }
        return values;
    }

    /// Converts design-space `axisValues` into `[-1, 1]` coordinates.
    ///
    /// Missing trailing values use the axis default. Extra values are ignored. Each coordinate is
    /// clamped to the axis min/max before normalization. The default maps to `0`, the min to `-1`
    /// when it is below the default, and the max to `1` when it is above the default.
    ///
    /// @param axisValues design-space coordinates in axis order
    /// @return one normalized coordinate per axis
    float[] normalize(float[] axisValues) {
        if (axes.length == 0) {
            return EMPTY_NORMALIZED;
        }
        float[] normalized = new float[axes.length];
        for (int index = 0; index < axes.length; index++) {
            VariationAxis axis = axes[index];
            float value = index < axisValues.length ? axisValues[index] : axis.defaultValue();
            if (value < axis.minValue()) {
                value = axis.minValue();
            } else if (value > axis.maxValue()) {
                value = axis.maxValue();
            }
            float def = axis.defaultValue();
            if (value < def) {
                float span = def - axis.minValue();
                normalized[index] = span == 0.0f ? 0.0f : (value - def) / span;
            } else if (value > def) {
                float span = axis.maxValue() - def;
                normalized[index] = span == 0.0f ? 0.0f : (value - def) / span;
            }
        }
        return normalized;
    }

    /// Converts a 16.16 fixed value to float.
    private static float fixedToFloat(int value) {
        return value / 65536.0f;
    }
}

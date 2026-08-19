package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/// Parses a first-stable `STAT` design-axis array and axis-value formats 1–4.
///
/// A missing or truncated table yields no axes, no instances, and elided-fallback name ID `0`.
@NotNullByDefault
final class StatTable {
    /// Shared empty table.
    static final StatTable EMPTY = new StatTable(new StatAxis[0], new StatNamedInstance[0], 0);

    /// Design axes in file order.
    private final StatAxis[] axes;

    /// Named instances in file order.
    private final StatNamedInstance[] instances;

    /// `elidedFallbackNameID` from version 1.1+, or `0` when absent.
    private final int elidedFallbackNameId;

    /// Creates a parsed table.
    private StatTable(StatAxis[] axes, StatNamedInstance[] instances, int elidedFallbackNameId) {
        this.axes = axes;
        this.instances = instances;
        this.elidedFallbackNameId = elidedFallbackNameId;
    }

    /// Parses an optional `STAT` table.
    ///
    /// @param table the table, or `null`
    /// @return the axes and named instances
    static StatTable parse(@Nullable ByteBuffer table) {
        if (table == null || table.remaining() < 16) {
            return EMPTY;
        }
        ByteBuffer buffer = table.duplicate().order(ByteOrder.BIG_ENDIAN);
        buffer.clear();
        int major = Short.toUnsignedInt(buffer.getShort());
        int minor = Short.toUnsignedInt(buffer.getShort());
        int axisSize = Short.toUnsignedInt(buffer.getShort());
        int axisCount = Short.toUnsignedInt(buffer.getShort());
        long axesOffset = Integer.toUnsignedLong(buffer.getInt());
        int valueCount = Short.toUnsignedInt(buffer.getShort());
        long valuesOffset = Integer.toUnsignedLong(buffer.getInt());
        int fallbackNameId = 0;
        if (major == 1 && minor >= 1 && buffer.remaining() >= 2) {
            fallbackNameId = Short.toUnsignedInt(buffer.getShort());
        }
        if (major != 1 || axisSize < 8) {
            return EMPTY;
        }
        if (axesOffset < 0 || (long) axisCount * (long) axisSize > Integer.MAX_VALUE
                || axesOffset + (long) axisCount * (long) axisSize > buffer.capacity()) {
            return EMPTY;
        }
        StatAxis[] axes = new StatAxis[axisCount];
        buffer.clear();
        buffer.position((int) axesOffset);
        for (int index = 0; index < axisCount; index++) {
            int start = buffer.position();
            int tag = buffer.getInt();
            int nameId = Short.toUnsignedInt(buffer.getShort());
            int ordering = Short.toUnsignedInt(buffer.getShort());
            axes[index] = new StatAxis(tag, nameId, ordering);
            buffer.position(start + axisSize);
        }
        if (valuesOffset < 0 || valuesOffset + (long) valueCount * 2L > buffer.capacity()) {
            return new StatTable(axes, new StatNamedInstance[0], fallbackNameId);
        }
        List<StatNamedInstance> instances = new ArrayList<>();
        for (int index = 0; index < valueCount; index++) {
            buffer.clear();
            buffer.position((int) valuesOffset + index * 2);
            int tableOffset = Short.toUnsignedInt(buffer.getShort());
            if (tableOffset < 0 || (long) tableOffset + 8L > buffer.capacity()) {
                continue;
            }
            buffer.clear();
            buffer.position(tableOffset);
            int format = Short.toUnsignedInt(buffer.getShort());
            if (format == 1) {
                if ((long) tableOffset + 12L > buffer.capacity()) {
                    continue;
                }
                int axisIndex = Short.toUnsignedInt(buffer.getShort());
                int flags = Short.toUnsignedInt(buffer.getShort());
                int nameId = Short.toUnsignedInt(buffer.getShort());
                float value = buffer.getInt() / 65536.0f;
                if (axisIndex < axisCount) {
                    instances.add(new StatNamedInstance(nameId, axisIndex, value, 1, value, value, 0.0f, flags));
                }
            } else if (format == 2) {
                if ((long) tableOffset + 20L > buffer.capacity()) {
                    continue;
                }
                int axisIndex = Short.toUnsignedInt(buffer.getShort());
                int flags = Short.toUnsignedInt(buffer.getShort());
                int nameId = Short.toUnsignedInt(buffer.getShort());
                float value = buffer.getInt() / 65536.0f;
                float rangeMin = buffer.getInt() / 65536.0f;
                float rangeMax = buffer.getInt() / 65536.0f;
                if (axisIndex < axisCount) {
                    instances.add(new StatNamedInstance(nameId, axisIndex, value, 2, rangeMin, rangeMax, 0.0f, flags));
                }
            } else if (format == 3) {
                if ((long) tableOffset + 16L > buffer.capacity()) {
                    continue;
                }
                int axisIndex = Short.toUnsignedInt(buffer.getShort());
                int flags = Short.toUnsignedInt(buffer.getShort());
                int nameId = Short.toUnsignedInt(buffer.getShort());
                float value = buffer.getInt() / 65536.0f;
                float linkedValue = buffer.getInt() / 65536.0f;
                if (axisIndex < axisCount) {
                    instances.add(new StatNamedInstance(nameId, axisIndex, value, 3, value, value, linkedValue, flags));
                }
            } else if (format == 4) {
                int pairCount = Short.toUnsignedInt(buffer.getShort());
                if (pairCount < 1 || (long) tableOffset + 8L + (long) pairCount * 6L > buffer.capacity()) {
                    continue;
                }
                int flags = Short.toUnsignedInt(buffer.getShort());
                int nameId = Short.toUnsignedInt(buffer.getShort());
                int axisIndex = Short.toUnsignedInt(buffer.getShort());
                float value = buffer.getInt() / 65536.0f;
                int[] extraIndices = new int[pairCount - 1];
                float[] extraValues = new float[pairCount - 1];
                boolean pairsValid = axisIndex < axisCount;
                for (int pair = 0; pair < extraIndices.length; pair++) {
                    extraIndices[pair] = Short.toUnsignedInt(buffer.getShort());
                    extraValues[pair] = buffer.getInt() / 65536.0f;
                    if (extraIndices[pair] >= axisCount) {
                        pairsValid = false;
                    }
                }
                if (pairsValid) {
                    instances.add(new StatNamedInstance(
                            nameId,
                            axisIndex,
                            value,
                            4,
                            value,
                            value,
                            0.0f,
                            flags,
                            extraIndices,
                            extraValues
                    ));
                }
            }
        }
        return new StatTable(axes, instances.toArray(StatNamedInstance[]::new), fallbackNameId);
    }

    /// Returns the design axes.
    ///
    /// @return the axes
    @Unmodifiable List<StatAxis> axes() {
        return Collections.unmodifiableList(Arrays.asList(axes));
    }

    /// Returns the named instances.
    ///
    /// @return the instances
    @Unmodifiable List<StatNamedInstance> namedInstances() {
        return Collections.unmodifiableList(Arrays.asList(instances));
    }

    /// Returns the elided-fallback name ID.
    ///
    /// @return the name ID, or `0` when absent
    int elidedFallbackNameId() {
        return elidedFallbackNameId;
    }
}

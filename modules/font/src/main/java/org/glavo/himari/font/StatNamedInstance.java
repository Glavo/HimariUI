package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Arrays;
import java.util.Objects;

/// Stores one `STAT` named instance from axis-value format 1, 2, 3, or 4.
///
/// @param nameId the `name` table ID for the instance name
/// @param axisIndex the design-axis index; format 4 reports the first axis
/// @param value the design-space coordinate; format 2 reports the nominal value
/// @param format the axis-value table format
/// @param rangeMin the format-2 range minimum; otherwise equal to [`#value()`]
/// @param rangeMax the format-2 range maximum; otherwise equal to [`#value()`]
/// @param linkedValue the format-3 linked value; `0` when the format has no link
/// @param flags the axis-value flags
/// @param extraAxisIndices format-4 axis indices after the first pair; empty otherwise
/// @param extraValues format-4 coordinates after the first pair; empty otherwise
@NotNullByDefault
public record StatNamedInstance(
        int nameId,
        int axisIndex,
        float value,
        int format,
        float rangeMin,
        float rangeMax,
        float linkedValue,
        int flags,
        int @Unmodifiable [] extraAxisIndices,
        float @Unmodifiable [] extraValues
) {
    /// `OlderSiblingFontAttribute`.
    public static final int FLAG_OLDER_SIBLING_FONT_ATTRIBUTE = 0x0001;

    /// `ElidableAxisValueName`.
    public static final int FLAG_ELIDABLE_AXIS_VALUE_NAME = 0x0002;
    /// Validates the instance identity.
    public StatNamedInstance {
        if (nameId < 0 || axisIndex < 0 || flags < 0) {
            throw new IllegalArgumentException("STAT instance nameId, axisIndex, and flags must be nonnegative");
        }
        if (format < 1 || format > 4) {
            throw new IllegalArgumentException("STAT instance format must be 1, 2, 3, or 4");
        }
        if (!Float.isFinite(value) || !Float.isFinite(rangeMin) || !Float.isFinite(rangeMax)
                || !Float.isFinite(linkedValue)) {
            throw new IllegalArgumentException("STAT instance values must be finite");
        }
        Objects.requireNonNull(extraAxisIndices, "extraAxisIndices");
        Objects.requireNonNull(extraValues, "extraValues");
        if (extraAxisIndices.length != extraValues.length) {
            throw new IllegalArgumentException("STAT extra axis pairs must be the same length");
        }
        extraAxisIndices = extraAxisIndices.clone();
        extraValues = extraValues.clone();
        for (int index = 0; index < extraAxisIndices.length; index++) {
            if (extraAxisIndices[index] < 0 || !Float.isFinite(extraValues[index])) {
                throw new IllegalArgumentException("STAT extra axis pairs must be nonnegative and finite");
            }
        }
    }

    /// Creates a format-1 named instance.
    ///
    /// @param nameId the `name` table ID
    /// @param axisIndex the design-axis index
    /// @param value the design-space coordinate
    public StatNamedInstance(int nameId, int axisIndex, float value) {
        this(nameId, axisIndex, value, 1, value, value, 0.0f, 0, new int[0], new float[0]);
    }

    /// Creates a named instance with no extra format-4 axis pairs.
    ///
    /// @param nameId the `name` table ID
    /// @param axisIndex the design-axis index
    /// @param value the design-space coordinate
    /// @param format the axis-value table format
    /// @param rangeMin the format-2 range minimum
    /// @param rangeMax the format-2 range maximum
    /// @param linkedValue the format-3 linked value
    public StatNamedInstance(
            int nameId,
            int axisIndex,
            float value,
            int format,
            float rangeMin,
            float rangeMax,
            float linkedValue
    ) {
        this(nameId, axisIndex, value, format, rangeMin, rangeMax, linkedValue, 0, new int[0], new float[0]);
    }

    /// Creates a named instance with flags and no extra format-4 axis pairs.
    ///
    /// @param nameId the `name` table ID
    /// @param axisIndex the design-axis index
    /// @param value the design-space coordinate
    /// @param format the axis-value table format
    /// @param rangeMin the format-2 range minimum
    /// @param rangeMax the format-2 range maximum
    /// @param linkedValue the format-3 linked value
    /// @param flags the axis-value flags
    public StatNamedInstance(
            int nameId,
            int axisIndex,
            float value,
            int format,
            float rangeMin,
            float rangeMax,
            float linkedValue,
            int flags
    ) {
        this(nameId, axisIndex, value, format, rangeMin, rangeMax, linkedValue, flags, new int[0], new float[0]);
    }

    /// Returns whether `ElidableAxisValueName` is set.
    ///
    /// @return `true` when the name may be omitted
    public boolean elidableAxisValueName() {
        return (flags & FLAG_ELIDABLE_AXIS_VALUE_NAME) != 0;
    }

    /// Returns whether `OlderSiblingFontAttribute` is set.
    ///
    /// @return `true` when the value is an older-sibling attribute
    public boolean olderSiblingFontAttribute() {
        return (flags & FLAG_OLDER_SIBLING_FONT_ATTRIBUTE) != 0;
    }

    /// Returns a copy of the extra format-4 axis indices.
    ///
    /// @return the extra indices
    public int @Unmodifiable [] extraAxisIndices() {
        return Arrays.copyOf(extraAxisIndices, extraAxisIndices.length);
    }

    /// Returns a copy of the extra format-4 coordinates.
    ///
    /// @return the extra values
    public float @Unmodifiable [] extraValues() {
        return Arrays.copyOf(extraValues, extraValues.length);
    }
}

package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;

/// Stores one `fvar` axis.
///
/// Values are 16.16 fixed-point converted to `float`.
///
/// @param tag the four-byte axis tag
/// @param minValue the minimum coordinate
/// @param defaultValue the default coordinate
/// @param maxValue the maximum coordinate
@NotNullByDefault
public record VariationAxis(int tag, float minValue, float defaultValue, float maxValue) {
    /// Validates the axis extents.
    public VariationAxis {
        if (minValue > defaultValue || defaultValue > maxValue) {
            throw new IllegalArgumentException("Variation axis extents must be ordered");
        }
    }

    /// Returns the tag as four ASCII bytes when they are printable.
    ///
    /// @return the tag string
    public String tagString() {
        return new String(new char[] {
                (char) ((tag >>> 24) & 0xFF),
                (char) ((tag >>> 16) & 0xFF),
                (char) ((tag >>> 8) & 0xFF),
                (char) (tag & 0xFF)
        });
    }
}

package org.glavo.himari.platform.api;

import org.jetbrains.annotations.NotNullByDefault;

/// Describes a chromaticity using CIE 1931 `x` and `y` coordinates.
///
/// Finite coordinates are preserved without restricting them to the unit triangle so the value can
/// represent wide-gamut and synthetic primaries used by future color models.
///
/// @param x the finite CIE `x` coordinate
/// @param y the finite CIE `y` coordinate
@NotNullByDefault
public record Chromaticity(double x, double y) {
    /// Creates a chromaticity.
    ///
    /// @throws IllegalArgumentException if either coordinate is non-finite
    public Chromaticity {
        if (!Double.isFinite(x) || !Double.isFinite(y)) {
            throw new IllegalArgumentException("Chromaticity coordinates must be finite");
        }
    }
}

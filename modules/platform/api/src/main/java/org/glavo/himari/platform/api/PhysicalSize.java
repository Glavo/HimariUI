package org.glavo.himari.platform.api;

import org.jetbrains.annotations.NotNullByDefault;

/// Describes a two-dimensional extent in physical pixels.
///
/// @param width the nonnegative width in pixels
/// @param height the nonnegative height in pixels
@NotNullByDefault
public record PhysicalSize(int width, int height) {
    /// Creates a physical size.
    ///
    /// @throws IllegalArgumentException if either extent is negative
    public PhysicalSize {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Physical extents must be nonnegative");
        }
    }
}

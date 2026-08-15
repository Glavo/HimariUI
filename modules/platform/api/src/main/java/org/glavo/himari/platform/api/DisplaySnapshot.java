package org.glavo.himari.platform.api;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Captures one display's target-neutral geometry, scale, and color capabilities.
///
/// @param id the stable session-local display identifier
/// @param enumerationIndex the nonnegative position in the session's current display enumeration
/// @param bounds the display bounds in the session's logical desktop coordinates
/// @param workArea the usable area contained by `bounds`
/// @param physicalSize the positive current display mode size in physical pixels
/// @param scaleFactor the finite positive physical-pixels-per-logical-pixel scale
/// @param primary whether this is the session's primary display
/// @param configurationGeneration the nonnegative generation advanced for any semantic display
/// configuration change
/// @param colorCapabilities the display color description and its independently tracked generation
@NotNullByDefault
public record DisplaySnapshot(
        DisplayId id,
        int enumerationIndex,
        LogicalRect bounds,
        LogicalRect workArea,
        PhysicalSize physicalSize,
        double scaleFactor,
        boolean primary,
        long configurationGeneration,
        DisplayColorCapabilities colorCapabilities
) {
    /// Creates a validated display snapshot.
    ///
    /// @throws IllegalArgumentException if geometry, scale, size, or generation is invalid
    public DisplaySnapshot {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(bounds, "bounds");
        Objects.requireNonNull(workArea, "workArea");
        Objects.requireNonNull(physicalSize, "physicalSize");
        Objects.requireNonNull(colorCapabilities, "colorCapabilities");
        if (enumerationIndex < 0) {
            throw new IllegalArgumentException("Display enumeration index must be nonnegative");
        }
        if (bounds.width() <= 0.0 || bounds.height() <= 0.0) {
            throw new IllegalArgumentException("Display bounds must have positive extents");
        }
        if (!bounds.contains(workArea)) {
            throw new IllegalArgumentException("Display work area must be contained by display bounds");
        }
        if (physicalSize.width() <= 0 || physicalSize.height() <= 0) {
            throw new IllegalArgumentException("Display physical size must have positive extents");
        }
        if (!Double.isFinite(scaleFactor) || scaleFactor <= 0.0) {
            throw new IllegalArgumentException("Display scale factor must be finite and positive");
        }
        if (configurationGeneration < 0L) {
            throw new IllegalArgumentException("Display configuration generation must be nonnegative");
        }
    }
}

package org.glavo.himari.platform.headless;

import org.glavo.himari.platform.api.DisplayColorDescription;
import org.glavo.himari.platform.api.DisplayId;
import org.glavo.himari.platform.api.LogicalRect;
import org.glavo.himari.platform.api.PhysicalSize;
import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Configures one deterministic virtual display.
///
/// @param id the stable display identifier
/// @param bounds the positive logical bounds in virtual desktop coordinates
/// @param workArea the logical work area contained by `bounds`
/// @param physicalSize the positive virtual display mode size in physical pixels
/// @param scaleFactor the finite positive physical-pixels-per-logical-pixel scale
/// @param primary whether this is the sole primary display in its topology
/// @param colorDescription the programmable primaries, luminance, reference white, headroom, and
/// presentation modes
@NotNullByDefault
public record HeadlessDisplayConfiguration(
        DisplayId id,
        LogicalRect bounds,
        LogicalRect workArea,
        PhysicalSize physicalSize,
        double scaleFactor,
        boolean primary,
        DisplayColorDescription colorDescription
) {
    /// The stable identifier of the default virtual display.
    public static final DisplayId DEFAULT_DISPLAY_ID = new DisplayId("headless-0");

    /// Creates a validated virtual display configuration.
    ///
    /// @throws IllegalArgumentException if geometry, size, or scale is invalid
    public HeadlessDisplayConfiguration {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(bounds, "bounds");
        Objects.requireNonNull(workArea, "workArea");
        Objects.requireNonNull(physicalSize, "physicalSize");
        Objects.requireNonNull(colorDescription, "colorDescription");
        if (bounds.width() <= 0.0 || bounds.height() <= 0.0) {
            throw new IllegalArgumentException("Virtual display bounds must have positive extents");
        }
        if (!bounds.contains(workArea)) {
            throw new IllegalArgumentException("Virtual display work area must be contained by its bounds");
        }
        if (physicalSize.width() <= 0 || physicalSize.height() <= 0) {
            throw new IllegalArgumentException("Virtual display physical size must have positive extents");
        }
        if (!Double.isFinite(scaleFactor) || scaleFactor <= 0.0) {
            throw new IllegalArgumentException("Virtual display scale factor must be finite and positive");
        }
    }

    /// Returns the default 1920×1080 logical and physical sRGB SDR display at scale `1.0`.
    ///
    /// @return the default display configuration
    public static HeadlessDisplayConfiguration defaultDisplay() {
        LogicalRect bounds = new LogicalRect(0.0, 0.0, 1920.0, 1080.0);
        return new HeadlessDisplayConfiguration(
                DEFAULT_DISPLAY_ID,
                bounds,
                bounds,
                new PhysicalSize(1920, 1080),
                1.0,
                true,
                DisplayColorDescription.SRGB_SDR
        );
    }
}

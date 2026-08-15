package org.glavo.himari.platform.api;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/// Describes the programmable color volume and presentation modes of a display.
///
/// This capability value deliberately keeps gamut, luminance, reference white, relative headroom,
/// and presentation modes independent. It does not select a surface pixel format or reinterpret
/// application color values.
///
/// @param primaries the display-native primaries and white point
/// @param minimumLuminanceNits the finite nonnegative minimum luminance in cd/m²
/// @param maximumLuminanceNits the finite positive maximum luminance in cd/m²
/// @param sdrReferenceWhiteNits the finite positive SDR reference white in cd/m²
/// @param relativeHeadroom the finite relative maximum above reference white, at least `1.0`
/// @param presentationModes the nonempty, duplicate-free supported modes, including
/// [PresentationMode#SDR], in deterministic preference order
@NotNullByDefault
public record DisplayColorDescription(
        DisplayPrimaries primaries,
        double minimumLuminanceNits,
        double maximumLuminanceNits,
        double sdrReferenceWhiteNits,
        double relativeHeadroom,
        @Unmodifiable List<PresentationMode> presentationModes
) {
    /// A deterministic sRGB SDR display with a 100 cd/m² reference white.
    public static final DisplayColorDescription SRGB_SDR = new DisplayColorDescription(
            DisplayPrimaries.SRGB,
            0.0,
            100.0,
            100.0,
            1.0,
            List.of(PresentationMode.SDR)
    );

    /// Creates a display color description and takes an immutable snapshot of the mode list.
    ///
    /// @throws IllegalArgumentException if luminance or headroom is invalid, if the mode list is
    /// empty or contains duplicates, or if SDR presentation is absent
    public DisplayColorDescription {
        Objects.requireNonNull(primaries, "primaries");
        Objects.requireNonNull(presentationModes, "presentationModes");
        if (!Double.isFinite(minimumLuminanceNits) || minimumLuminanceNits < 0.0) {
            throw new IllegalArgumentException("Minimum luminance must be finite and nonnegative");
        }
        if (!Double.isFinite(maximumLuminanceNits) || maximumLuminanceNits <= 0.0
                || maximumLuminanceNits < minimumLuminanceNits) {
            throw new IllegalArgumentException("Maximum luminance must be finite and no less than the minimum");
        }
        if (!Double.isFinite(sdrReferenceWhiteNits) || sdrReferenceWhiteNits <= 0.0
                || sdrReferenceWhiteNits > maximumLuminanceNits) {
            throw new IllegalArgumentException("SDR reference white must be positive and within display luminance");
        }
        if (!Double.isFinite(relativeHeadroom) || relativeHeadroom < 1.0) {
            throw new IllegalArgumentException("Relative headroom must be finite and at least 1.0");
        }

        presentationModes = List.copyOf(presentationModes);
        if (presentationModes.isEmpty()) {
            throw new IllegalArgumentException("At least one presentation mode is required");
        }
        if (new HashSet<>(presentationModes).size() != presentationModes.size()) {
            throw new IllegalArgumentException("Presentation modes must not contain duplicates");
        }
        if (!presentationModes.contains(PresentationMode.SDR)) {
            throw new IllegalArgumentException("SDR presentation is required as an explicit fallback");
        }
    }
}

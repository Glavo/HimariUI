package org.glavo.himari.platform.api;

import org.jetbrains.annotations.NotNullByDefault;

/// Describes display-native RGB primaries and white point without implying a transfer function.
///
/// @param red the red primary
/// @param green the green primary
/// @param blue the blue primary
/// @param white the white point
@NotNullByDefault
public record DisplayPrimaries(
        Chromaticity red,
        Chromaticity green,
        Chromaticity blue,
        Chromaticity white
) {
    /// The standard sRGB/Rec.709 primaries with a D65 white point.
    public static final DisplayPrimaries SRGB = new DisplayPrimaries(
            new Chromaticity(0.6400, 0.3300),
            new Chromaticity(0.3000, 0.6000),
            new Chromaticity(0.1500, 0.0600),
            new Chromaticity(0.3127, 0.3290)
    );

    /// Creates a display-primary description.
    public DisplayPrimaries {
        java.util.Objects.requireNonNull(red, "red");
        java.util.Objects.requireNonNull(green, "green");
        java.util.Objects.requireNonNull(blue, "blue");
        java.util.Objects.requireNonNull(white, "white");
    }
}

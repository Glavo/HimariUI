package org.glavo.himari.graphics;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Maps tagged colors onto versioned SDR sRGB without hard-clipping highlight chroma first.
///
/// Version 1 converts into the extended-linear working encoding, peak-normalizes when the largest
/// component exceeds reference white `1.0`, clips remaining out-of-gamut components, then encodes
/// sRGB. Reference-white and darker values stay on the identity path.
@NotNullByDefault
public final class SdrFallback {
    /// Published mapping version. Later EETF revisions must bump this constant.
    public static final int VERSION = 1;

    /// Prevents instantiation.
    private SdrFallback() {
    }

    /// Maps `source` onto encoded sRGB using [`#VERSION`].
    ///
    /// @param source the tagged color
    /// @return the SDR sRGB color
    public static Color map(Color source) {
        Objects.requireNonNull(source, "source");
        Color linear = source.toExtendedLinear();
        float peak = Math.max(linear.red(), Math.max(linear.green(), linear.blue()));
        float red = linear.red();
        float green = linear.green();
        float blue = linear.blue();
        if (peak > 1.0f) {
            float scale = 1.0f / peak;
            red *= scale;
            green *= scale;
            blue *= scale;
        }
        return Color.srgb(encode(red), encode(green), encode(blue), linear.alpha());
    }

    /// Encodes one linear component after out-of-gamut clipping.
    ///
    /// @param linear the linear component
    /// @return the sRGB component
    private static float encode(float linear) {
        return Color.encodeSrgb(Math.clamp(linear, 0.0f, 1.0f));
    }
}

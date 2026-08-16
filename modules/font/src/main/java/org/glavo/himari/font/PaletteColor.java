package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;

/// Stores one CPAL color as unpremultiplied 8-bit sRGB plus alpha.
///
/// @param red the red channel in `[0, 255]`
/// @param green the green channel in `[0, 255]`
/// @param blue the blue channel in `[0, 255]`
/// @param alpha the opacity in `[0, 255]`
@NotNullByDefault
public record PaletteColor(int red, int green, int blue, int alpha) {
    /// Foreground palette index in a COLR layer; the face supplies no CPAL color.
    public static final int FOREGROUND = 0xFFFF;

    /// Validates the channels.
    public PaletteColor {
        if (outsideByte(red) || outsideByte(green) || outsideByte(blue) || outsideByte(alpha)) {
            throw new IllegalArgumentException("Palette channels must be in [0, 255]");
        }
    }

    /// Returns whether `value` is outside an 8-bit channel.
    ///
    /// @param value the channel
    /// @return whether the value is out of range
    private static boolean outsideByte(int value) {
        return value < 0 || value > 255;
    }
}

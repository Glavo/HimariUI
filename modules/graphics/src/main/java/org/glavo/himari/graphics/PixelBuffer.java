package org.glavo.himari.graphics;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Arrays;
import java.util.Objects;

/// Stores one decoded raster independent of `java.awt.image.BufferedImage`.
///
/// Samples are row-major RGBA8. The tagged encoding and alpha interpretation travel with the
/// pixels so a later color conversion cannot treat the buffer as implicit sRGB.
///
/// @param width the positive pixel width
/// @param height the positive pixel height
/// @param rgba unassociated or premultiplied RGBA8 samples
/// @param encoding the tagged color encoding
/// @param alpha how alpha relates to the color channels
@NotNullByDefault
public record PixelBuffer(
        int width,
        int height,
        byte @Unmodifiable [] rgba,
        ColorEncoding encoding,
        AlphaInterpretation alpha
) {
    /// Maximum accepted width or height.
    public static final int MAX_EDGE = 16_384;

    /// Validates the buffer.
    public PixelBuffer {
        if (width <= 0 || height <= 0 || width > MAX_EDGE || height > MAX_EDGE) {
            throw new IllegalArgumentException("Pixel extents must be in (0, " + MAX_EDGE + "]");
        }
        Objects.requireNonNull(rgba, "rgba");
        Objects.requireNonNull(encoding, "encoding");
        Objects.requireNonNull(alpha, "alpha");
        if (rgba.length != Math.multiplyExact(width, height) * 4) {
            throw new IllegalArgumentException("RGBA length must be width * height * 4");
        }
        rgba = Arrays.copyOf(rgba, rgba.length);
    }

    /// Creates an unassociated sRGB buffer.
    ///
    /// @param width the width
    /// @param height the height
    /// @param rgba RGBA8 samples
    /// @return the buffer
    public static PixelBuffer srgbUnassociated(int width, int height, byte[] rgba) {
        return new PixelBuffer(width, height, rgba, ColorEncoding.SRGB, AlphaInterpretation.UNASSOCIATED);
    }
}

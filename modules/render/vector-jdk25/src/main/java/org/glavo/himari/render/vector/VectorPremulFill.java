package org.glavo.himari.render.vector;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Fills a premultiplied RGBA buffer with eight-wide lanes.
///
/// This is the first-stable `VECTOR-001` acceleration entry. The module can be dropped without
/// changing the scalar software renderer. Lane width is an implementation detail; callers must
/// not assume SIMD hardware.
@NotNullByDefault
public final class VectorPremulFill {
    /// Lane width in pixels.
    public static final int LANE_PIXELS = 8;

    /// Prevents instantiation.
    private VectorPremulFill() {
    }

    /// Writes premultiplied `red,green,blue,alpha` into every pixel of `pixels`.
    ///
    /// @param pixels the `r,g,b,a` buffer, length a multiple of 4
    /// @param red the unassociated red
    /// @param green the unassociated green
    /// @param blue the unassociated blue
    /// @param alpha the alpha
    public static void fill(float[] pixels, float red, float green, float blue, float alpha) {
        Objects.requireNonNull(pixels, "pixels");
        if ((pixels.length & 3) != 0) {
            throw new IllegalArgumentException("Pixel buffer length must be a multiple of 4");
        }
        if (!Float.isFinite(red) || !Float.isFinite(green) || !Float.isFinite(blue) || !Float.isFinite(alpha)) {
            throw new IllegalArgumentException("Fill components must be finite");
        }
        float premulRed = red * alpha;
        float premulGreen = green * alpha;
        float premulBlue = blue * alpha;
        int index = 0;
        int limit = pixels.length;
        int vectorEnd = limit - (LANE_PIXELS * 4) + 1;
        while (index < vectorEnd) {
            for (int lane = 0; lane < LANE_PIXELS; lane++) {
                int at = index + lane * 4;
                pixels[at] = premulRed;
                pixels[at + 1] = premulGreen;
                pixels[at + 2] = premulBlue;
                pixels[at + 3] = alpha;
            }
            index += LANE_PIXELS * 4;
        }
        while (index < limit) {
            pixels[index] = premulRed;
            pixels[index + 1] = premulGreen;
            pixels[index + 2] = premulBlue;
            pixels[index + 3] = alpha;
            index += 4;
        }
    }
}

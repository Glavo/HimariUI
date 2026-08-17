package org.glavo.himari.render.vector;

import org.glavo.himari.graphics.Color;
import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Drives [`VectorPremulFill#fill(float[], float, float, float, float)`].
@NotNullByDefault
final class VectorPremulFillTest {
    /// Eight-wide fill matches a scalar premultiplied write, including a tail pixel.
    @Test
    void fillsPremultipliedLanesAndTail() {
        Color source = Color.srgb(0.5f, 0.25f, 0.0f, 0.5f);
        Color linear = source.toExtendedLinear();
        float[] pixels = new float[9 * 4];
        VectorPremulFill.fill(pixels, linear.red(), linear.green(), linear.blue(), linear.alpha());
        float red = linear.red() * linear.alpha();
        float green = linear.green() * linear.alpha();
        float blue = linear.blue() * linear.alpha();
        for (int pixel = 0; pixel < 9; pixel++) {
            int at = pixel * 4;
            assertEquals(red, pixels[at], 1.0e-6f);
            assertEquals(green, pixels[at + 1], 1.0e-6f);
            assertEquals(blue, pixels[at + 2], 1.0e-6f);
            assertEquals(linear.alpha(), pixels[at + 3], 1.0e-6f);
        }
    }
}

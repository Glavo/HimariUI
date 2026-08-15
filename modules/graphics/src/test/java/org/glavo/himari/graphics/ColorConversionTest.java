package org.glavo.himari.graphics;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies tagged sRGB and Display-P3 conversions into the extended-linear working encoding.
@NotNullByDefault
final class ColorConversionTest {
    /// Absolute tolerance for primary conversions.
    private static final float EPSILON = 0.0015f;

    /// D65 white in Display-P3 stays D65 white in extended-linear sRGB.
    @Test
    void displayP3WhiteMapsToExtendedLinearWhite() {
        Color linear = new Color(ColorEncoding.DISPLAY_P3, 1.0f, 1.0f, 1.0f, 1.0f).toExtendedLinear();
        assertEquals(ColorEncoding.EXTENDED_LINEAR, linear.encoding());
        assertEquals(1.0f, linear.red(), EPSILON);
        assertEquals(1.0f, linear.green(), EPSILON);
        assertEquals(1.0f, linear.blue(), EPSILON);
    }

    /// Linear Display-P3 red is outside the sRGB gamut and is preserved until SDR encoding.
    @Test
    void displayP3RedExceedsSrgbGamut() {
        Color linear = new Color(ColorEncoding.LINEAR_DISPLAY_P3, 1.0f, 0.0f, 0.0f, 1.0f).toExtendedLinear();
        assertTrue(linear.red() > 1.0f);
        assertTrue(linear.green() < 0.0f);
        assertTrue(linear.blue() < 0.0f);
        Color srgb = linear.toSrgb();
        assertEquals(1.0f, srgb.red(), EPSILON);
        assertEquals(0.0f, srgb.green(), EPSILON);
        assertEquals(0.0f, srgb.blue(), EPSILON);
    }

    /// Encoded sRGB mid-gray decodes through the IEC transfer.
    @Test
    void srgbMidGrayDecodesThroughTransfer() {
        Color linear = Color.srgb(0.5f, 0.5f, 0.5f, 1.0f).toExtendedLinear();
        assertEquals(0.21404114f, linear.red(), 0.0001f);
    }
}

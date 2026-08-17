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

    /// Linear BT.2020 D65 white stays D65 white in extended-linear sRGB.
    @Test
    void bt2020WhiteMapsToExtendedLinearWhite() {
        Color linear = new Color(ColorEncoding.LINEAR_BT2020, 1.0f, 1.0f, 1.0f, 1.0f).toExtendedLinear();
        assertEquals(1.0f, linear.red(), EPSILON);
        assertEquals(1.0f, linear.green(), EPSILON);
        assertEquals(1.0f, linear.blue(), EPSILON);
    }

    /// Linear BT.2020 red is outside the sRGB gamut and is preserved until SDR encoding.
    @Test
    void bt2020RedExceedsSrgbGamut() {
        Color linear = new Color(ColorEncoding.LINEAR_BT2020, 1.0f, 0.0f, 0.0f, 1.0f).toExtendedLinear();
        assertTrue(linear.red() > 1.0f);
        assertTrue(linear.green() < 0.0f);
        assertTrue(linear.blue() < 0.0f);
        Color srgb = linear.toSrgb();
        assertEquals(1.0f, srgb.red(), EPSILON);
        assertEquals(0.0f, srgb.green(), EPSILON);
        assertEquals(0.0f, srgb.blue(), EPSILON);
    }

    /// BT.2100 PQ 100-nit white maps to extended-linear 1.0.
    @Test
    void pqHundredNitWhiteMapsToExtendedLinearWhite() {
        float encoded = Color.encodePq(Color.PQ_REFERENCE_WHITE_NITS);
        Color linear = new Color(ColorEncoding.BT2100_PQ, encoded, encoded, encoded, 1.0f).toExtendedLinear();
        assertEquals(1.0f, linear.red(), 0.002f);
        assertEquals(1.0f, linear.green(), 0.002f);
        assertEquals(1.0f, linear.blue(), 0.002f);
    }

    /// Versioned SDR fallback keeps highlight chroma that hard-clip `toSrgb` would flatten.
    @Test
    void sdrFallbackPreservesHighlightChroma() {
        Color linear = Color.extendedLinear(10.0f, 2.0f, 2.0f, 1.0f);
        Color clipped = linear.toSrgb();
        assertEquals(1.0f, clipped.red(), EPSILON);
        assertEquals(1.0f, clipped.green(), EPSILON);
        assertEquals(1.0f, clipped.blue(), EPSILON);
        Color mapped = SdrFallback.map(linear);
        assertEquals(SdrFallback.VERSION, 1);
        assertEquals(ColorEncoding.SRGB, mapped.encoding());
        assertEquals(1.0f, mapped.red(), EPSILON);
        assertTrue(mapped.green() < 0.5f);
        assertTrue(mapped.blue() < 0.5f);
        Color white = SdrFallback.map(Color.extendedLinear(1.0f, 1.0f, 1.0f, 1.0f));
        assertEquals(1.0f, white.red(), EPSILON);
        assertEquals(1.0f, white.green(), EPSILON);
        assertEquals(1.0f, white.blue(), EPSILON);
    }

    /// HLG 0.5 decodes through the inverse OETF before the BT.2020 primary conversion.
    @Test
    void hlgMidSignalUsesInverseOetf() {
        Color linear = new Color(ColorEncoding.BT2100_HLG, 0.5f, 0.5f, 0.5f, 1.0f).toExtendedLinear();
        assertEquals(Color.decodeHlg(0.5f), linear.red(), 0.002f);
        assertEquals(linear.red(), linear.green(), 0.0001f);
        assertEquals(linear.red(), linear.blue(), 0.0001f);
    }

    /// Encodes extended-linear white into linear BT.2020 white.
    @Test
    void extendedLinearWhiteEncodesToLinearBt2020White() {
        Color encoded = Color.extendedLinear(1.0f, 1.0f, 1.0f, 1.0f).toLinearBt2020();
        assertEquals(ColorEncoding.LINEAR_BT2020, encoded.encoding());
        assertEquals(1.0f, encoded.red(), EPSILON);
        assertEquals(1.0f, encoded.green(), EPSILON);
        assertEquals(1.0f, encoded.blue(), EPSILON);
    }

    /// Encodes and decodes linear BT.2020 red through the shipped BT.2020 OETF.
    @Test
    void bt2020RedRoundTripsThroughEncode() {
        Color source = new Color(ColorEncoding.LINEAR_BT2020, 1.0f, 0.0f, 0.0f, 1.0f);
        Color encoded = source.toBt2020();
        assertEquals(ColorEncoding.BT2020, encoded.encoding());
        Color linear = encoded.toLinearBt2020();
        assertEquals(1.0f, linear.red(), EPSILON);
        assertEquals(0.0f, linear.green(), EPSILON);
        assertEquals(0.0f, linear.blue(), EPSILON);
    }

    /// Encodes extended-linear 100-nit white as BT.2100 PQ and decodes it back.
    @Test
    void pqHundredNitWhiteRoundTripsThroughEncode() {
        Color encoded = Color.extendedLinear(1.0f, 1.0f, 1.0f, 1.0f).toBt2100Pq();
        assertEquals(ColorEncoding.BT2100_PQ, encoded.encoding());
        assertEquals(Color.encodePq(Color.PQ_REFERENCE_WHITE_NITS), encoded.red(), 0.002f);
        Color linear = encoded.toExtendedLinear();
        assertEquals(1.0f, linear.red(), 0.002f);
        assertEquals(1.0f, linear.green(), 0.002f);
        assertEquals(1.0f, linear.blue(), 0.002f);
    }

    /// Encodes and decodes linear Display-P3 red through the shipped P3 transfer.
    @Test
    void displayP3RedRoundTripsThroughEncode() {
        Color source = new Color(ColorEncoding.LINEAR_DISPLAY_P3, 1.0f, 0.0f, 0.0f, 1.0f);
        Color encoded = source.toDisplayP3();
        assertEquals(ColorEncoding.DISPLAY_P3, encoded.encoding());
        Color linear = encoded.toLinearDisplayP3();
        assertEquals(1.0f, linear.red(), EPSILON);
        assertEquals(0.0f, linear.green(), EPSILON);
        assertEquals(0.0f, linear.blue(), EPSILON);
    }

    /// Encodes extended-linear white into linear Display-P3 white.
    @Test
    void extendedLinearWhiteEncodesToLinearDisplayP3White() {
        Color encoded = Color.extendedLinear(1.0f, 1.0f, 1.0f, 1.0f).toLinearDisplayP3();
        assertEquals(ColorEncoding.LINEAR_DISPLAY_P3, encoded.encoding());
        assertEquals(1.0f, encoded.red(), EPSILON);
        assertEquals(1.0f, encoded.green(), EPSILON);
        assertEquals(1.0f, encoded.blue(), EPSILON);
    }

    /// Encodes HLG mid-signal scene-linear `1/12` back to `0.5`.
    @Test
    void hlgMidSignalRoundTripsThroughEncode() {
        float mid = 1.0f / 12.0f;
        Color encoded = Color.extendedLinear(mid, mid, mid, 1.0f).toBt2100Hlg();
        assertEquals(ColorEncoding.BT2100_HLG, encoded.encoding());
        assertEquals(0.5f, encoded.red(), 0.002f);
        Color linear = encoded.toExtendedLinear();
        assertEquals(mid, linear.red(), 0.002f);
    }

    /// Bradford maps D65 white onto D50 white.
    @Test
    void bradfordAdaptsD65WhiteToD50() {
        float[] d65 = Color.SRGB_WHITE.toXyzD65();
        float[] d50 = ChromaticAdaptation.bradford(
                d65[0],
                d65[1],
                d65[2],
                ChromaticAdaptation.D65_X,
                ChromaticAdaptation.D65_Y,
                ChromaticAdaptation.D65_Z,
                ChromaticAdaptation.D50_X,
                ChromaticAdaptation.D50_Y,
                ChromaticAdaptation.D50_Z
        );
        assertEquals(ChromaticAdaptation.D50_X, d50[0], EPSILON);
        assertEquals(ChromaticAdaptation.D50_Y, d50[1], EPSILON);
        assertEquals(ChromaticAdaptation.D50_Z, d50[2], EPSILON);
    }

    /// CAT02 maps D65 white onto illuminant A and disagrees with Bradford on a non-white stimulus.
    @Test
    void cat02AdaptsD65WhiteToIlluminantAAndDiffersFromBradford() {
        float[] white = ChromaticAdaptation.cat02(
                ChromaticAdaptation.D65_X,
                ChromaticAdaptation.D65_Y,
                ChromaticAdaptation.D65_Z,
                ChromaticAdaptation.D65_X,
                ChromaticAdaptation.D65_Y,
                ChromaticAdaptation.D65_Z,
                ChromaticAdaptation.A_X,
                ChromaticAdaptation.A_Y,
                ChromaticAdaptation.A_Z
        );
        assertEquals(ChromaticAdaptation.A_X, white[0], EPSILON);
        assertEquals(ChromaticAdaptation.A_Y, white[1], EPSILON);
        assertEquals(ChromaticAdaptation.A_Z, white[2], EPSILON);
        Color red = new Color(ColorEncoding.LINEAR_SRGB, 1.0f, 0.0f, 0.0f, 1.0f);
        float[] xyz = red.toXyzD65();
        float[] bradford = ChromaticAdaptation.bradford(
                xyz[0],
                xyz[1],
                xyz[2],
                ChromaticAdaptation.D65_X,
                ChromaticAdaptation.D65_Y,
                ChromaticAdaptation.D65_Z,
                ChromaticAdaptation.A_X,
                ChromaticAdaptation.A_Y,
                ChromaticAdaptation.A_Z
        );
        float[] cat02 = ChromaticAdaptation.cat02(
                xyz[0],
                xyz[1],
                xyz[2],
                ChromaticAdaptation.D65_X,
                ChromaticAdaptation.D65_Y,
                ChromaticAdaptation.D65_Z,
                ChromaticAdaptation.A_X,
                ChromaticAdaptation.A_Y,
                ChromaticAdaptation.A_Z
        );
        assertTrue(
                Math.abs(bradford[0] - cat02[0])
                                + Math.abs(bradford[1] - cat02[1])
                                + Math.abs(bradford[2] - cat02[2])
                        > 0.001f
        );
    }
}

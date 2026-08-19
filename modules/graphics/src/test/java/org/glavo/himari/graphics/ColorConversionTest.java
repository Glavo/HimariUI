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

    /// CAT16 leaves D65 white unchanged and differs from Bradford on linear sRGB red.
    @Test
    void cat16AdaptsAndDiffersFromBradford() {
        float[] white = ChromaticAdaptation.cat16(
                ChromaticAdaptation.D65_X,
                ChromaticAdaptation.D65_Y,
                ChromaticAdaptation.D65_Z,
                ChromaticAdaptation.D65_X,
                ChromaticAdaptation.D65_Y,
                ChromaticAdaptation.D65_Z,
                ChromaticAdaptation.D65_X,
                ChromaticAdaptation.D65_Y,
                ChromaticAdaptation.D65_Z
        );
        assertEquals(ChromaticAdaptation.D65_X, white[0], EPSILON);
        assertEquals(ChromaticAdaptation.D65_Y, white[1], EPSILON);
        assertEquals(ChromaticAdaptation.D65_Z, white[2], EPSILON);
        Color red = Color.linearBt2020(1.0f, 0.0f, 0.0f, 1.0f);
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
        float[] cat16 = ChromaticAdaptation.cat16(
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
                Math.abs(bradford[0] - cat16[0])
                                + Math.abs(bradford[1] - cat16[1])
                                + Math.abs(bradford[2] - cat16[2])
                        > 0.001f
        );
    }

    /// Hunt-Pointer-Estevez von Kries maps D65 white onto D50 and disagrees with Bradford on red.
    @Test
    void vonKriesAdaptsAndDiffersFromBradford() {
        float[] white = ChromaticAdaptation.vonKries(
                ChromaticAdaptation.D65_X,
                ChromaticAdaptation.D65_Y,
                ChromaticAdaptation.D65_Z,
                ChromaticAdaptation.D65_X,
                ChromaticAdaptation.D65_Y,
                ChromaticAdaptation.D65_Z,
                ChromaticAdaptation.D50_X,
                ChromaticAdaptation.D50_Y,
                ChromaticAdaptation.D50_Z
        );
        assertEquals(ChromaticAdaptation.D50_X, white[0], EPSILON);
        assertEquals(ChromaticAdaptation.D50_Y, white[1], EPSILON);
        assertEquals(ChromaticAdaptation.D50_Z, white[2], EPSILON);
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
        float[] vonKries = ChromaticAdaptation.vonKries(
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
                Math.abs(bradford[0] - vonKries[0])
                                + Math.abs(bradford[1] - vonKries[1])
                                + Math.abs(bradford[2] - vonKries[2])
                        > 0.001f
        );
    }

    /// Pixel formats and presentation configurations are first-stable tagged values.
    @Test
    void presentationConfigurationTagsFormatAndMapping() {
        assertEquals(4, PixelFormat.RGBA8.bytesPerPixel());
        assertEquals(4, PixelFormat.RGB10A2.bytesPerPixel());
        assertEquals(8, PixelFormat.RGBA16F.bytesPerPixel());
        assertEquals(16, PixelFormat.RGBA32F.bytesPerPixel());
        PresentationColorConfiguration sdr = PresentationColorConfiguration.SDR_SRGB;
        assertEquals(PixelFormat.RGBA8, sdr.format());
        assertEquals(ColorEncoding.SRGB, sdr.encoding());
        assertEquals(MappingOwner.FRAMEWORK, sdr.mappingOwner());
        assertEquals(LuminanceRange.SDR, sdr.luminance());
        assertEquals(ContentLightMetadata.NONE, sdr.metadata());
        assertEquals(1L, sdr.capabilityGeneration());
    }

    /// First-stable profile values tag encodings without changing conversion math.
    @Test
    void colorProfileValuesTagFirstStableEncodings() {
        assertEquals(ChromaticAdaptation.D65_X, WhitePoint.D65.x(), EPSILON);
        assertEquals(WhitePoint.D65, ColorPrimaries.SRGB.white());
        assertEquals(ColorEncoding.SRGB, ColorProfile.SRGB.encoding());
        assertEquals(ColorEncoding.DISPLAY_P3, ColorProfile.DISPLAY_P3.encoding());
        assertEquals(ColorEncoding.BT2100_PQ, ColorProfile.BT2100_PQ.encoding());
        assertEquals(TransferFunction.HLG, ColorProfile.BT2100_HLG.transfer());
        assertEquals(100.0f, LuminanceRange.SDR.referenceWhiteNits(), EPSILON);
        assertEquals(0.0f, ContentLightMetadata.NONE.maxCll(), EPSILON);
        Color tagged = new Color(ColorProfile.BT2020.encoding(), 0.5f, 0.5f, 0.5f, 1.0f);
        assertEquals(ColorEncoding.LINEAR_BT2020, tagged.toLinearBt2020().encoding());
    }

    /// D65 sRGB white encodes to CIE Lab `100,0,0` and decodes back through [`Color#fromCieLab`].
    @Test
    void cieLabRoundTripsD65WhiteThroughShippedEntries() {
        float[] lab = Color.SRGB_WHITE.toCieLab();
        assertEquals(100.0f, lab[0], 0.05f);
        assertEquals(0.0f, lab[1], 0.05f);
        assertEquals(0.0f, lab[2], 0.05f);
        Color decoded = Color.fromCieLab(lab[0], lab[1], lab[2], 1.0f);
        assertEquals(ColorEncoding.EXTENDED_LINEAR, decoded.encoding());
        assertEquals(1.0f, decoded.red(), 0.01f);
        assertEquals(1.0f, decoded.green(), 0.01f);
        assertEquals(1.0f, decoded.blue(), 0.01f);
    }

    /// Half-float and packed RGB10A2 quantization use [`PixelBuffer`] entry points.
    @Test
    void pixelBufferQuantizesHalfFloatAndRgb10a2ThroughShippedEntries() {
        byte[] rgba = {(byte) 255, (byte) 128, 0, (byte) 255};
        PixelBuffer source = PixelBuffer.srgbUnassociated(1, 1, rgba);
        PixelBuffer half = PixelBuffer.fromRgba16f(
                1,
                1,
                source.toRgba16f(),
                ColorEncoding.SRGB,
                AlphaInterpretation.UNASSOCIATED
        );
        assertEquals((byte) 255, half.rgba()[0]);
        assertEquals((byte) 128, half.rgba()[1]);
        assertEquals(0, half.rgba()[2]);
        assertEquals((byte) 255, half.rgba()[3]);
        PixelBuffer packed = PixelBuffer.fromRgb10a2(
                1,
                1,
                source.toRgb10a2(),
                ColorEncoding.SRGB,
                AlphaInterpretation.UNASSOCIATED
        );
        assertEquals((byte) 255, packed.rgba()[0]);
        assertEquals((byte) 128, packed.rgba()[1]);
        assertEquals(0, packed.rgba()[2]);
        assertEquals((byte) 255, packed.rgba()[3]);
        assertEquals(ColorEncoding.SRGB, packed.encoding());
        PixelBuffer floats = PixelBuffer.fromRgba32f(
                1,
                1,
                source.toRgba32f(),
                ColorEncoding.SRGB,
                AlphaInterpretation.UNASSOCIATED
        );
        assertEquals((byte) 255, floats.rgba()[0]);
        assertEquals((byte) 128, floats.rgba()[1]);
        assertEquals(0, floats.rgba()[2]);
        assertEquals((byte) 255, floats.rgba()[3]);
    }

    /// D65 white is CIE LCh `100,0,0` and decodes back through [`Color#fromCieLch`].
    @Test
    void cieLchRoundTripsD65WhiteThroughShippedEntries() {
        float[] lch = Color.SRGB_WHITE.toCieLch();
        assertEquals(100.0f, lch[0], 0.05f);
        assertEquals(0.0f, lch[1], 0.05f);
        assertEquals(0.0f, lch[2], 0.05f);
        Color decoded = Color.fromCieLch(lch[0], lch[1], lch[2], 1.0f);
        assertEquals(ColorEncoding.EXTENDED_LINEAR, decoded.encoding());
        assertEquals(1.0f, decoded.red(), 0.01f);
        assertEquals(1.0f, decoded.green(), 0.01f);
        assertEquals(1.0f, decoded.blue(), 0.01f);
        float[] red = Color.srgb(1.0f, 0.0f, 0.0f, 1.0f).toCieLch();
        assertTrue(red[1] > 50.0f);
        Color restored = Color.fromCieLch(red[0], red[1], red[2], 1.0f).toSrgb();
        assertEquals(1.0f, restored.red(), 0.02f);
        assertEquals(0.0f, restored.green(), 0.02f);
        assertEquals(0.0f, restored.blue(), 0.02f);
    }

    /// D65 white is CIE Luv `100,0,0` and decodes back through [`Color#fromCieLuv`].
    @Test
    void cieLuvRoundTripsD65WhiteThroughShippedEntries() {
        float[] luv = Color.SRGB_WHITE.toCieLuv();
        assertEquals(100.0f, luv[0], 0.05f);
        assertEquals(0.0f, luv[1], 0.15f);
        assertEquals(0.0f, luv[2], 0.15f);
        Color decoded = Color.fromCieLuv(luv[0], luv[1], luv[2], 1.0f);
        assertEquals(ColorEncoding.EXTENDED_LINEAR, decoded.encoding());
        assertEquals(1.0f, decoded.red(), 0.01f);
        assertEquals(1.0f, decoded.green(), 0.01f);
        assertEquals(1.0f, decoded.blue(), 0.01f);
        float[] red = Color.srgb(1.0f, 0.0f, 0.0f, 1.0f).toCieLuv();
        assertTrue(red[1] > 50.0f);
        Color restored = Color.fromCieLuv(red[0], red[1], red[2], 1.0f).toSrgb();
        assertEquals(1.0f, restored.red(), 0.03f);
        assertEquals(0.0f, restored.green(), 0.03f);
        assertEquals(0.0f, restored.blue(), 0.03f);
    }

    /// D65 white adapts to D50 white through [`Color#toXyzD50`].
    @Test
    void xyzD50AdaptsD65WhiteThroughBradford() {
        float[] d50 = Color.SRGB_WHITE.toXyzD50();
        assertEquals(ChromaticAdaptation.D50_X, d50[0], 0.01f);
        assertEquals(ChromaticAdaptation.D50_Y, d50[1], 0.01f);
        assertEquals(ChromaticAdaptation.D50_Z, d50[2], 0.01f);
        Color decoded = Color.fromXyzD50(d50[0], d50[1], d50[2], 1.0f);
        assertEquals(1.0f, decoded.red(), 0.01f);
        assertEquals(1.0f, decoded.green(), 0.01f);
        assertEquals(1.0f, decoded.blue(), 0.01f);
        Color d65 = Color.fromXyzD65(
                ChromaticAdaptation.D65_X,
                ChromaticAdaptation.D65_Y,
                ChromaticAdaptation.D65_Z,
                1.0f
        );
        assertEquals(1.0f, d65.red(), 0.01f);
        assertEquals(1.0f, d65.green(), 0.01f);
        assertEquals(1.0f, d65.blue(), 0.01f);
    }

    /// D65 white is OKLab `1,0,0` and decodes back through [`Color#fromOklab`].
    @Test
    void oklabRoundTripsD65WhiteThroughShippedEntries() {
        float[] lab = Color.SRGB_WHITE.toOklab();
        assertEquals(1.0f, lab[0], 0.01f);
        assertEquals(0.0f, lab[1], 0.02f);
        assertEquals(0.0f, lab[2], 0.02f);
        Color decoded = Color.fromOklab(lab[0], lab[1], lab[2], 1.0f);
        assertEquals(1.0f, decoded.red(), 0.02f);
        assertEquals(1.0f, decoded.green(), 0.02f);
        assertEquals(1.0f, decoded.blue(), 0.02f);
        float[] red = Color.srgb(1.0f, 0.0f, 0.0f, 1.0f).toOklab();
        Color restored = Color.fromOklab(red[0], red[1], red[2], 1.0f).toSrgb();
        assertEquals(1.0f, restored.red(), 0.03f);
        assertEquals(0.0f, restored.green(), 0.03f);
        assertEquals(0.0f, restored.blue(), 0.03f);
    }

    /// D65 white is OKLCH `1,0,0` and decodes back through [`Color#fromOklch`].
    @Test
    void oklchRoundTripsD65WhiteThroughShippedEntries() {
        float[] lch = Color.SRGB_WHITE.toOklch();
        assertEquals(1.0f, lch[0], 0.01f);
        assertEquals(0.0f, lch[1], 0.02f);
        assertEquals(0.0f, lch[2], 0.02f);
        Color decoded = Color.fromOklch(lch[0], lch[1], lch[2], 1.0f);
        assertEquals(1.0f, decoded.red(), 0.02f);
        assertEquals(1.0f, decoded.green(), 0.02f);
        assertEquals(1.0f, decoded.blue(), 0.02f);
        float[] red = Color.srgb(1.0f, 0.0f, 0.0f, 1.0f).toOklch();
        assertTrue(red[1] > 0.1f);
        Color restored = Color.fromOklch(red[0], red[1], red[2], 1.0f).toSrgb();
        assertEquals(1.0f, restored.red(), 0.03f);
        assertEquals(0.0f, restored.green(), 0.03f);
        assertEquals(0.0f, restored.blue(), 0.03f);
    }

    /// Linear interpolation uses [`Color#interpolate`] in the extended-linear working encoding.
    @Test
    void interpolateMixesExtendedLinearThroughShippedEntry() {
        Color mid = Color.SRGB_BLACK.interpolate(Color.SRGB_WHITE, 0.5f);
        assertEquals(ColorEncoding.EXTENDED_LINEAR, mid.encoding());
        assertEquals(0.5f, mid.red(), 0.001f);
        assertEquals(0.5f, mid.green(), 0.001f);
        assertEquals(0.5f, mid.blue(), 0.001f);
        Color highlight = Color.extendedLinear(2.0f, 0.0f, 0.0f, 1.0f)
                .interpolate(Color.extendedLinear(0.0f, 0.0f, 0.0f, 1.0f), 0.25f);
        assertEquals(1.5f, highlight.red(), 0.001f);
    }

    /// Tagged BT.2020 and BT.2100 factories decode through the shipped conversion entry points.
    @Test
    void taggedHdrFactoriesRoundTripThroughExtendedLinear() {
        Color pq = Color.bt2100Pq(0.5f, 0.5f, 0.5f, 1.0f);
        Color linear = pq.toExtendedLinear();
        assertEquals(ColorEncoding.EXTENDED_LINEAR, linear.encoding());
        assertTrue(linear.red() > 0.0f);
        Color hlg = Color.bt2100Hlg(0.5f, 0.5f, 0.5f, 1.0f).toExtendedLinear();
        assertTrue(hlg.red() > 0.0f);
        Color encoded = Color.bt2020(0.5f, 0.5f, 0.5f, 1.0f).toLinearBt2020();
        assertEquals(ColorEncoding.LINEAR_BT2020, encoded.encoding());
        Color a98White = Color.linearA98(1.0f, 1.0f, 1.0f, 1.0f).toExtendedLinear();
        assertEquals(1.0f, a98White.red(), EPSILON);
        assertEquals(1.0f, a98White.green(), EPSILON);
        assertEquals(1.0f, a98White.blue(), EPSILON);
        Color a98Red = Color.linearA98(1.0f, 0.0f, 0.0f, 1.0f);
        Color encodedA98 = a98Red.toA98();
        assertEquals(ColorEncoding.A98, encodedA98.encoding());
        Color back = encodedA98.toLinearA98();
        assertEquals(1.0f, back.red(), EPSILON);
        assertEquals(0.0f, back.green(), EPSILON);
        assertEquals(0.0f, back.blue(), EPSILON);
        assertEquals(ColorPrimaries.A98.redX(), 0.640f, 0.0001f);
        Color proPhotoWhite = Color.linearProPhoto(1.0f, 1.0f, 1.0f, 1.0f).toExtendedLinear();
        assertEquals(1.0f, proPhotoWhite.red(), 0.01f);
        assertEquals(1.0f, proPhotoWhite.green(), 0.01f);
        assertEquals(1.0f, proPhotoWhite.blue(), 0.01f);
        Color proPhotoRed = Color.linearProPhoto(1.0f, 0.0f, 0.0f, 1.0f);
        Color encodedProPhoto = proPhotoRed.toProPhoto();
        assertEquals(ColorEncoding.PROPHOTO, encodedProPhoto.encoding());
        Color backProPhoto = encodedProPhoto.toLinearProPhoto();
        assertEquals(1.0f, backProPhoto.red(), EPSILON);
        assertEquals(0.0f, backProPhoto.green(), EPSILON);
        assertEquals(0.0f, backProPhoto.blue(), EPSILON);
        assertEquals(0.0f, Color.SRGB_WHITE.deltaE76(Color.SRGB_WHITE), 0.001f);
        assertTrue(Color.SRGB_WHITE.deltaE76(Color.SRGB_BLACK) > 90.0f);
        assertEquals(ColorPrimaries.PROPHOTO.white(), WhitePoint.D50);
        Color rec709White = Color.linearBt709(1.0f, 1.0f, 1.0f, 1.0f).toExtendedLinear();
        assertEquals(1.0f, rec709White.red(), EPSILON);
        assertEquals(1.0f, rec709White.green(), EPSILON);
        assertEquals(1.0f, rec709White.blue(), EPSILON);
        Color rec709Red = Color.linearBt709(1.0f, 0.0f, 0.0f, 1.0f);
        Color encodedRec709 = rec709Red.toBt709();
        assertEquals(ColorEncoding.BT709, encodedRec709.encoding());
        Color backRec709 = encodedRec709.toLinearBt709();
        assertEquals(1.0f, backRec709.red(), EPSILON);
        assertEquals(0.0f, backRec709.green(), EPSILON);
        assertEquals(0.0f, backRec709.blue(), EPSILON);
        assertEquals(0.0f, Color.SRGB_WHITE.deltaE2000(Color.SRGB_WHITE), 0.001f);
        Color sharmaA = Color.fromCieLab(50.0f, 2.6772f, -79.7751f, 1.0f);
        Color sharmaB = Color.fromCieLab(50.0f, 0.0f, -82.7485f, 1.0f);
        assertEquals(2.0425f, sharmaA.deltaE2000(sharmaB), 0.01f);
        assertEquals(0.0f, Color.SRGB_WHITE.deltaE94(Color.SRGB_WHITE), 0.001f);
        assertTrue(Color.SRGB_WHITE.deltaE94(Color.SRGB_BLACK) > 90.0f);
        assertEquals(0.5f, Color.SRGB_WHITE.withAlpha(0.5f).alpha(), 0.0001f);
        assertEquals(ColorEncoding.SRGB, Color.SRGB_WHITE.withAlpha(0.5f).encoding());
        Color gray = Color.srgb(1.0f, 0.0f, 0.0f, 1.0f).grayscale();
        assertEquals(ColorEncoding.EXTENDED_LINEAR, gray.encoding());
        assertEquals(gray.red(), gray.green(), 0.0001f);
        assertEquals(gray.red(), gray.blue(), 0.0001f);
        assertEquals(Color.srgb(1.0f, 0.0f, 0.0f, 1.0f).relativeLuminance(), gray.red(), 0.001f);
        Color oklabMid = Color.SRGB_WHITE.interpolateOklab(Color.SRGB_BLACK, 0.5f);
        assertEquals(ColorEncoding.EXTENDED_LINEAR, oklabMid.encoding());
        assertTrue(oklabMid.red() > 0.0f && oklabMid.red() < 1.0f);
        assertEquals(oklabMid.red(), oklabMid.green(), 0.001f);
        assertEquals(0xFFFFFFFF, Color.SRGB_WHITE.toArgb());
        assertEquals(0xFF000000, Color.SRGB_BLACK.toArgb());
        Color fromPacked = Color.fromArgb(0x80FF0000);
        assertEquals(ColorEncoding.SRGB, fromPacked.encoding());
        assertEquals(1.0f, fromPacked.red(), 0.001f);
        assertEquals(0.0f, fromPacked.green(), 0.001f);
        assertEquals(0.5f, fromPacked.alpha(), 0.01f);
        assertEquals(0x80FF0000, fromPacked.toArgb());
        float[] whiteHsl = Color.SRGB_WHITE.toHsl();
        assertEquals(0.0f, whiteHsl[0], 0.01f);
        assertEquals(0.0f, whiteHsl[1], 0.01f);
        assertEquals(1.0f, whiteHsl[2], 0.01f);
        float[] redHsl = Color.srgb(1.0f, 0.0f, 0.0f, 1.0f).toHsl();
        assertEquals(0.0f, redHsl[0], 0.01f);
        assertEquals(1.0f, redHsl[1], 0.01f);
        assertEquals(0.5f, redHsl[2], 0.01f);
        Color hslRed = Color.fromHsl(redHsl[0], redHsl[1], redHsl[2], 1.0f);
        assertEquals(1.0f, hslRed.red(), 0.001f);
        assertEquals(0.0f, hslRed.green(), 0.001f);
        assertEquals(0.0f, hslRed.blue(), 0.001f);
        float[] redHsv = Color.srgb(1.0f, 0.0f, 0.0f, 1.0f).toHsv();
        assertEquals(0.0f, redHsv[0], 0.01f);
        assertEquals(1.0f, redHsv[1], 0.01f);
        assertEquals(1.0f, redHsv[2], 0.01f);
        Color hsvLime = Color.fromHsv(120.0f, 1.0f, 1.0f, 1.0f);
        assertEquals(0.0f, hsvLime.red(), 0.001f);
        assertEquals(1.0f, hsvLime.green(), 0.001f);
        assertEquals(0.0f, hsvLime.blue(), 0.001f);
        Color inverted = Color.SRGB_WHITE.invert();
        assertEquals(0.0f, inverted.red(), 0.001f);
        assertEquals(0.0f, inverted.green(), 0.001f);
        assertEquals(0.0f, inverted.blue(), 0.001f);
        Color complement = Color.srgb(1.0f, 0.0f, 0.0f, 1.0f).complementary();
        assertEquals(0.0f, complement.red(), 0.001f);
        assertEquals(1.0f, complement.green(), 0.001f);
        assertEquals(1.0f, complement.blue(), 0.001f);
        assertEquals("#FFFFFF", Color.SRGB_WHITE.toHex());
        assertEquals("#80FF0000", Color.fromArgb(0x80FF0000).toHex());
        Color hexRed = Color.fromHex("#FF0000");
        assertEquals(1.0f, hexRed.red(), 0.001f);
        assertEquals(0.0f, hexRed.green(), 0.001f);
        assertEquals(0.0f, hexRed.blue(), 0.001f);
        assertEquals(1.0f, hexRed.alpha(), 0.001f);
        assertEquals(0.5f, Color.fromHex("80FF0000").alpha(), 0.01f);
        Color midGray = Color.srgb(0.5f, 0.5f, 0.5f, 1.0f);
        assertTrue(midGray.lighten(0.2f).toHsl()[2] > midGray.toHsl()[2]);
        assertTrue(midGray.darken(0.2f).toHsl()[2] < midGray.toHsl()[2]);
        assertEquals(1.0f, Color.SRGB_WHITE.lighten(0.5f).toHsl()[2], 0.001f);
        assertEquals(0.0f, Color.SRGB_BLACK.darken(0.5f).toHsl()[2], 0.001f);
        Color washed = Color.fromHsl(0.0f, 0.4f, 0.5f, 1.0f);
        assertTrue(washed.saturate(0.2f).toHsl()[1] > washed.toHsl()[1]);
        assertTrue(washed.desaturate(0.2f).toHsl()[1] < washed.toHsl()[1]);
        assertEquals(0.0f, Color.SRGB_WHITE.desaturate(1.0f).toHsl()[1], 0.001f);
        Color lime = Color.fromHsl(120.0f, 1.0f, 0.5f, 1.0f).hueRotate(120.0f);
        assertEquals(240.0f, lime.toHsl()[0], 0.5f);
        assertEquals(0.0f, lime.red(), 0.001f);
        assertEquals(0.0f, lime.green(), 0.001f);
        assertEquals(1.0f, lime.blue(), 0.001f);
        Color redHue = Color.fromHsl(120.0f, 1.0f, 0.5f, 1.0f).withHue(0.0f);
        assertEquals(0.0f, redHue.toHsl()[0], 0.5f);
        assertEquals(1.0f, redHue.red(), 0.001f);
        assertEquals(0.0f, redHue.green(), 0.001f);
        assertEquals(0.0f, redHue.blue(), 0.001f);
        Color grayed = Color.fromHsl(0.0f, 1.0f, 0.5f, 1.0f).withSaturation(0.0f);
        assertEquals(0.0f, grayed.toHsl()[1], 0.001f);
        assertEquals(grayed.red(), grayed.green(), 0.001f);
        Color mid = Color.fromHsl(0.0f, 1.0f, 0.25f, 1.0f).withLightness(0.5f);
        assertEquals(0.5f, mid.toHsl()[2], 0.001f);
        Color red = Color.srgb(1.0f, 0.0f, 0.0f, 1.0f);
        Color grayedOklch = red.withChroma(0.0f);
        assertEquals(0.0f, grayedOklch.toOklch()[1], 0.01f);
        assertTrue(red.toOklch()[1] > 0.1f);
        Color rotatedOklch = red.withOklchHue(red.toOklch()[2] + 180.0f);
        assertTrue(rotatedOklch.toOklch()[1] > 0.05f);
        assertTrue(Math.abs(rotatedOklch.toOklch()[2] - red.toOklch()[2]) > 90.0f);
        Color dim = red.withOklchLightness(Math.max(0.0f, red.toOklch()[0] - 0.2f));
        assertTrue(dim.toOklch()[0] < red.toOklch()[0] - 0.05f);
        Color midOklch = red.interpolateOklch(Color.srgb(0.0f, 0.0f, 1.0f, 1.0f), 0.5f);
        float redL = red.toOklch()[0];
        float blueL = Color.srgb(0.0f, 0.0f, 1.0f, 1.0f).toOklch()[0];
        float mixedL = midOklch.toOklch()[0];
        assertTrue(mixedL > Math.min(redL, blueL) - 0.02f);
        assertTrue(mixedL < Math.max(redL, blueL) + 0.02f);
        Color dimLab = red.withOklabL(Math.max(0.0f, red.toOklab()[0] - 0.2f));
        assertTrue(dimLab.toOklab()[0] < red.toOklab()[0] - 0.05f);
        Color flattened = red.withOklabA(0.0f);
        assertEquals(0.0f, flattened.toOklab()[1], 0.02f);
        Color noYellow = red.withOklabB(0.0f);
        assertEquals(0.0f, noYellow.toOklab()[2], 0.02f);
        Color grayedLch = red.withCieLchChroma(0.0f);
        assertEquals(0.0f, grayedLch.toCieLch()[1], 0.1f);
        Color rotatedLch = red.withCieLchHue(red.toCieLch()[2] + 180.0f);
        assertTrue(rotatedLch.toCieLch()[1] > 5.0f);
        assertTrue(Math.abs(rotatedLch.toCieLch()[2] - red.toCieLch()[2]) > 90.0f);
        Color dimLch = red.withCieLchLightness(Math.max(0.0f, red.toCieLch()[0] - 20.0f));
        assertTrue(dimLch.toCieLch()[0] < red.toCieLch()[0] - 5.0f);
        Color midCieLch = red.interpolateCieLch(Color.srgb(0.0f, 0.0f, 1.0f, 1.0f), 0.5f);
        float redCieL = red.toCieLch()[0];
        float blueCieL = Color.srgb(0.0f, 0.0f, 1.0f, 1.0f).toCieLch()[0];
        float mixedCieL = midCieLch.toCieLch()[0];
        assertTrue(mixedCieL > Math.min(redCieL, blueCieL) - 2.0f);
        assertTrue(mixedCieL < Math.max(redCieL, blueCieL) + 2.0f);
        Color dimCieLab = red.withCieLabL(Math.max(0.0f, red.toCieLab()[0] - 20.0f));
        assertTrue(dimCieLab.toCieLab()[0] < red.toCieLab()[0] - 5.0f);
        Color flattenedLab = red.withCieLabA(0.0f);
        assertEquals(0.0f, flattenedLab.toCieLab()[1], 2.0f);
        Color noYellowLab = red.withCieLabB(0.0f);
        assertEquals(0.0f, noYellowLab.toCieLab()[2], 2.0f);
        Color midLab = red.interpolateCieLab(Color.srgb(0.0f, 0.0f, 1.0f, 1.0f), 0.5f);
        float redLabL = red.toCieLab()[0];
        float blueLabL = Color.srgb(0.0f, 0.0f, 1.0f, 1.0f).toCieLab()[0];
        float mixedLabL = midLab.toCieLab()[0];
        assertTrue(mixedLabL > Math.min(redLabL, blueLabL) - 2.0f);
        assertTrue(mixedLabL < Math.max(redLabL, blueLabL) + 2.0f);
        Color dimLuv = red.withCieLuvL(Math.max(0.0f, red.toCieLuv()[0] - 20.0f));
        assertTrue(dimLuv.toCieLuv()[0] < red.toCieLuv()[0] - 5.0f);
        Color flattenedLuv = red.withCieLuvU(0.0f);
        assertEquals(0.0f, flattenedLuv.toCieLuv()[1], 5.0f);
        Color noYellowLuv = red.withCieLuvV(0.0f);
        assertEquals(0.0f, noYellowLuv.toCieLuv()[2], 5.0f);
        Color midLuv = red.interpolateCieLuv(Color.srgb(0.0f, 0.0f, 1.0f, 1.0f), 0.5f);
        float redLuvL = red.toCieLuv()[0];
        float blueLuvL = Color.srgb(0.0f, 0.0f, 1.0f, 1.0f).toCieLuv()[0];
        float mixedLuvL = midLuv.toCieLuv()[0];
        assertTrue(mixedLuvL > Math.min(redLuvL, blueLuvL) - 2.0f);
        assertTrue(mixedLuvL < Math.max(redLuvL, blueLuvL) + 2.0f);
        Color midXyzD50 = red.interpolateXyzD50(Color.srgb(0.0f, 0.0f, 1.0f, 1.0f), 0.5f);
        float redY = red.toXyzD50()[1];
        float blueY = Color.srgb(0.0f, 0.0f, 1.0f, 1.0f).toXyzD50()[1];
        float mixedY = midXyzD50.toXyzD50()[1];
        assertTrue(mixedY > Math.min(redY, blueY) - 0.05f);
        assertTrue(mixedY < Math.max(redY, blueY) + 0.05f);
        Color midXyzD65 = red.interpolateXyzD65(Color.srgb(0.0f, 0.0f, 1.0f, 1.0f), 0.5f);
        float redY65 = red.toXyzD65()[1];
        float blueY65 = Color.srgb(0.0f, 0.0f, 1.0f, 1.0f).toXyzD65()[1];
        float mixedY65 = midXyzD65.toXyzD65()[1];
        assertTrue(mixedY65 > Math.min(redY65, blueY65) - 0.05f);
        assertTrue(mixedY65 < Math.max(redY65, blueY65) + 0.05f);
        float[] xyz50 = red.toXyzD50();
        Color dimXyz50 = red.withXyzD50(xyz50[0], Math.max(0.0f, xyz50[1] * 0.5f), xyz50[2]);
        assertTrue(dimXyz50.toXyzD50()[1] < xyz50[1] - 0.02f);
        float[] xyz65 = red.toXyzD65();
        Color dimXyz65 = red.withXyzD65(xyz65[0], Math.max(0.0f, xyz65[1] * 0.5f), xyz65[2]);
        assertTrue(dimXyz65.toXyzD65()[1] < xyz65[1] - 0.02f);
        assertTrue(red.withXyzD50Y(Math.max(0.0f, xyz50[1] * 0.5f)).toXyzD50()[1] < xyz50[1] - 0.02f);
        assertTrue(red.withXyzD65Y(Math.max(0.0f, xyz65[1] * 0.5f)).toXyzD65()[1] < xyz65[1] - 0.02f);
        assertEquals(0.1f, red.withXyzD50X(0.1f).toXyzD50()[0], 0.02f);
        assertEquals(0.1f, red.withXyzD65X(0.1f).toXyzD65()[0], 0.02f);
        assertEquals(0.2f, red.withXyzD50Z(0.2f).toXyzD50()[2], 0.02f);
        assertEquals(0.2f, red.withXyzD65Z(0.2f).toXyzD65()[2], 0.02f);
        Color retinted = red.withGreen(0.5f).withBlue(0.25f);
        assertEquals(1.0f, retinted.red(), EPSILON);
        assertEquals(0.5f, retinted.green(), EPSILON);
        assertEquals(0.25f, retinted.blue(), EPSILON);
        assertEquals(ColorEncoding.SRGB, retinted.encoding());
        assertEquals(0.0f, Color.srgb(1.0f, 0.5f, 0.25f, 1.0f).withRed(0.0f).red(), EPSILON);
        Color recast = Color.srgb(1.0f, 0.0f, 0.0f, 0.75f).withRgb(0.1f, 0.2f, 0.3f);
        assertEquals(0.1f, recast.red(), EPSILON);
        assertEquals(0.2f, recast.green(), EPSILON);
        assertEquals(0.3f, recast.blue(), EPSILON);
        assertEquals(0.75f, recast.alpha(), EPSILON);
        assertEquals(ColorEncoding.SRGB, recast.encoding());
        Color recastAlpha = recast.withRgba(0.4f, 0.5f, 0.6f, 0.2f);
        assertEquals(0.4f, recastAlpha.red(), EPSILON);
        assertEquals(0.5f, recastAlpha.green(), EPSILON);
        assertEquals(0.6f, recastAlpha.blue(), EPSILON);
        assertEquals(0.2f, recastAlpha.alpha(), EPSILON);
        assertEquals(ColorEncoding.SRGB, recastAlpha.encoding());
        Color associated = Color.srgb(1.0f, 0.0f, 0.0f, 0.5f).premultiply();
        assertEquals(ColorEncoding.EXTENDED_LINEAR, associated.encoding());
        assertEquals(0.5f, associated.red(), EPSILON);
        assertEquals(0.0f, associated.green(), EPSILON);
        assertEquals(0.0f, associated.blue(), EPSILON);
        assertEquals(0.5f, associated.alpha(), EPSILON);
        Color restored = associated.unpremultiply();
        assertEquals(ColorEncoding.EXTENDED_LINEAR, restored.encoding());
        assertEquals(1.0f, restored.red(), EPSILON);
        assertEquals(0.0f, restored.green(), EPSILON);
        assertEquals(0.0f, restored.blue(), EPSILON);
        assertEquals(0.5f, restored.alpha(), EPSILON);
        Color clear = Color.srgb(1.0f, 0.25f, 0.0f, 0.0f).premultiply().unpremultiply();
        assertEquals(0.0f, clear.red(), EPSILON);
        assertEquals(0.0f, clear.alpha(), EPSILON);
        Color covered = Color.srgb(1.0f, 0.0f, 0.0f, 1.0f).over(Color.srgb(0.0f, 0.0f, 1.0f, 1.0f));
        assertEquals(ColorEncoding.EXTENDED_LINEAR, covered.encoding());
        assertEquals(1.0f, covered.red(), EPSILON);
        assertEquals(0.0f, covered.blue(), EPSILON);
        assertEquals(1.0f, covered.alpha(), EPSILON);
        Color mixed = Color.srgb(1.0f, 1.0f, 1.0f, 0.5f).over(Color.srgb(0.0f, 0.0f, 0.0f, 1.0f));
        assertEquals(0.5f, mixed.red(), EPSILON);
        assertEquals(0.5f, mixed.green(), EPSILON);
        assertEquals(0.5f, mixed.blue(), EPSILON);
        assertEquals(1.0f, mixed.alpha(), EPSILON);
        Color punched = Color.srgb(1.0f, 0.0f, 0.0f, 0.5f).in(Color.srgb(0.0f, 0.0f, 1.0f, 1.0f));
        assertEquals(ColorEncoding.EXTENDED_LINEAR, punched.encoding());
        assertEquals(1.0f, punched.red(), EPSILON);
        assertEquals(0.0f, punched.blue(), EPSILON);
        assertEquals(0.5f, punched.alpha(), EPSILON);
        Color cleared = Color.srgb(1.0f, 0.0f, 0.0f, 1.0f).out(Color.srgb(0.0f, 0.0f, 1.0f, 1.0f));
        assertEquals(0.0f, cleared.alpha(), EPSILON);
        Color trimmed = Color.srgb(1.0f, 0.0f, 0.0f, 1.0f).out(Color.srgb(0.0f, 0.0f, 1.0f, 0.5f));
        assertEquals(1.0f, trimmed.red(), EPSILON);
        assertEquals(0.5f, trimmed.alpha(), EPSILON);
        Color atop = Color.srgb(1.0f, 0.0f, 0.0f, 1.0f).atop(Color.srgb(0.0f, 0.0f, 1.0f, 1.0f));
        assertEquals(1.0f, atop.red(), EPSILON);
        assertEquals(0.0f, atop.blue(), EPSILON);
        assertEquals(1.0f, atop.alpha(), EPSILON);
        Color veiled = Color.srgb(1.0f, 1.0f, 1.0f, 0.5f).atop(Color.srgb(0.0f, 0.0f, 0.0f, 1.0f));
        assertEquals(0.5f, veiled.red(), EPSILON);
        assertEquals(1.0f, veiled.alpha(), EPSILON);
        Color behind = Color.srgb(1.0f, 0.0f, 0.0f, 1.0f).destOver(Color.srgb(0.0f, 0.0f, 1.0f, 1.0f));
        assertEquals(ColorEncoding.EXTENDED_LINEAR, behind.encoding());
        assertEquals(0.0f, behind.red(), EPSILON);
        assertEquals(1.0f, behind.blue(), EPSILON);
        assertEquals(1.0f, behind.alpha(), EPSILON);
        Color exclusive = Color.srgb(1.0f, 0.0f, 0.0f, 1.0f).xor(Color.srgb(0.0f, 0.0f, 1.0f, 1.0f));
        assertEquals(0.0f, exclusive.alpha(), EPSILON);
        Color kept = Color.srgb(1.0f, 0.0f, 0.0f, 1.0f).xor(Color.srgb(0.0f, 0.0f, 1.0f, 0.0f));
        assertEquals(1.0f, kept.red(), EPSILON);
        assertEquals(1.0f, kept.alpha(), EPSILON);
        Color masked = Color.srgb(1.0f, 0.0f, 0.0f, 0.5f).destIn(Color.srgb(0.0f, 0.0f, 1.0f, 1.0f));
        assertEquals(1.0f, masked.blue(), EPSILON);
        assertEquals(0.0f, masked.red(), EPSILON);
        assertEquals(0.5f, masked.alpha(), EPSILON);
        Color destCleared = Color.srgb(1.0f, 0.0f, 0.0f, 0.5f).destOut(Color.srgb(0.0f, 0.0f, 1.0f, 1.0f));
        assertEquals(1.0f, destCleared.blue(), EPSILON);
        assertEquals(0.5f, destCleared.alpha(), EPSILON);
        Color destCovered = Color.srgb(1.0f, 0.0f, 0.0f, 1.0f).destAtop(Color.srgb(0.0f, 0.0f, 1.0f, 1.0f));
        assertEquals(1.0f, destCovered.blue(), EPSILON);
        assertEquals(0.0f, destCovered.red(), EPSILON);
        assertEquals(1.0f, destCovered.alpha(), EPSILON);
        Color added = Color.srgb(1.0f, 0.0f, 0.0f, 0.5f).plus(Color.srgb(0.0f, 0.0f, 1.0f, 0.5f));
        assertEquals(ColorEncoding.EXTENDED_LINEAR, added.encoding());
        assertEquals(0.5f, added.red(), EPSILON);
        assertEquals(0.5f, added.blue(), EPSILON);
        assertEquals(1.0f, added.alpha(), EPSILON);
        Color erased = Color.srgb(1.0f, 0.0f, 0.0f, 1.0f).clear(Color.srgb(0.0f, 0.0f, 1.0f, 1.0f));
        assertEquals(0.0f, erased.red(), EPSILON);
        assertEquals(0.0f, erased.blue(), EPSILON);
        assertEquals(0.0f, erased.alpha(), EPSILON);
        Color srcOnly = Color.srgb(1.0f, 0.0f, 0.0f, 1.0f).source(Color.srgb(0.0f, 0.0f, 1.0f, 1.0f));
        assertEquals(ColorEncoding.EXTENDED_LINEAR, srcOnly.encoding());
        assertEquals(1.0f, srcOnly.red(), EPSILON);
        assertEquals(0.0f, srcOnly.blue(), EPSILON);
        Color dstOnly = Color.srgb(1.0f, 0.0f, 0.0f, 1.0f).dest(Color.srgb(0.0f, 0.0f, 1.0f, 1.0f));
        assertEquals(ColorEncoding.EXTENDED_LINEAR, dstOnly.encoding());
        assertEquals(0.0f, dstOnly.red(), EPSILON);
        assertEquals(1.0f, dstOnly.blue(), EPSILON);
        Color product = Color.srgb(1.0f, 0.0f, 0.0f, 1.0f).multiply(Color.srgb(0.0f, 0.0f, 1.0f, 1.0f));
        assertEquals(ColorEncoding.EXTENDED_LINEAR, product.encoding());
        assertEquals(0.0f, product.red(), EPSILON);
        assertEquals(0.0f, product.blue(), EPSILON);
        assertEquals(1.0f, product.alpha(), EPSILON);
        Color screened = Color.srgb(1.0f, 0.0f, 0.0f, 1.0f).screen(Color.srgb(0.0f, 0.0f, 1.0f, 1.0f));
        assertEquals(1.0f, screened.red(), EPSILON);
        assertEquals(1.0f, screened.blue(), EPSILON);
        assertEquals(1.0f, screened.alpha(), EPSILON);
        Color overlaid = Color.extendedLinear(1.0f, 0.0f, 0.0f, 1.0f)
                .overlay(Color.extendedLinear(0.5f, 0.5f, 0.5f, 1.0f));
        assertEquals(ColorEncoding.EXTENDED_LINEAR, overlaid.encoding());
        assertEquals(1.0f, overlaid.red(), EPSILON);
        assertEquals(0.0f, overlaid.green(), EPSILON);
        assertEquals(0.0f, overlaid.blue(), EPSILON);
        assertEquals(1.0f, overlaid.alpha(), EPSILON);
        Color lit = Color.extendedLinear(0.5f, 0.5f, 0.5f, 1.0f)
                .hardLight(Color.extendedLinear(1.0f, 0.0f, 0.0f, 1.0f));
        assertEquals(1.0f, lit.red(), EPSILON);
        assertEquals(0.0f, lit.green(), EPSILON);
        assertEquals(1.0f, lit.alpha(), EPSILON);
        Color softened = Color.extendedLinear(0.0f, 0.0f, 0.0f, 1.0f)
                .softLight(Color.extendedLinear(0.5f, 0.5f, 0.5f, 1.0f));
        assertEquals(ColorEncoding.EXTENDED_LINEAR, softened.encoding());
        assertEquals(0.25f, softened.red(), EPSILON);
        assertEquals(0.25f, softened.green(), EPSILON);
        assertEquals(1.0f, softened.alpha(), EPSILON);
        Color dodged = Color.extendedLinear(0.5f, 0.0f, 0.0f, 1.0f)
                .colorDodge(Color.extendedLinear(0.25f, 0.25f, 0.25f, 1.0f));
        assertEquals(0.5f, dodged.red(), EPSILON);
        assertEquals(0.25f, dodged.green(), EPSILON);
        assertEquals(1.0f, dodged.alpha(), EPSILON);
        Color burned = Color.extendedLinear(1.0f, 0.0f, 0.0f, 1.0f)
                .colorBurn(Color.extendedLinear(0.5f, 0.5f, 0.5f, 1.0f));
        assertEquals(0.5f, burned.red(), EPSILON);
        assertEquals(0.0f, burned.green(), EPSILON);
        assertEquals(1.0f, burned.alpha(), EPSILON);
        Color differenced = Color.extendedLinear(1.0f, 0.0f, 0.0f, 1.0f)
                .difference(Color.extendedLinear(0.0f, 0.0f, 1.0f, 1.0f));
        assertEquals(ColorEncoding.EXTENDED_LINEAR, differenced.encoding());
        assertEquals(1.0f, differenced.red(), EPSILON);
        assertEquals(0.0f, differenced.green(), EPSILON);
        assertEquals(1.0f, differenced.blue(), EPSILON);
        assertEquals(1.0f, differenced.alpha(), EPSILON);
        Color excluded = Color.extendedLinear(1.0f, 0.0f, 0.0f, 1.0f)
                .exclusion(Color.extendedLinear(1.0f, 1.0f, 1.0f, 1.0f));
        assertEquals(0.0f, excluded.red(), EPSILON);
        assertEquals(1.0f, excluded.green(), EPSILON);
        assertEquals(1.0f, excluded.blue(), EPSILON);
        assertEquals(1.0f, excluded.alpha(), EPSILON);
        Color hued = Color.srgb(1.0f, 0.0f, 0.0f, 1.0f).hue(Color.srgb(0.0f, 0.0f, 1.0f, 1.0f));
        assertEquals(0.0f, hued.toHsl()[0], 1.0f);
        assertEquals(1.0f, hued.toHsl()[1], 0.05f);
        Color saturated = Color.srgb(1.0f, 0.0f, 0.0f, 1.0f)
                .saturation(Color.srgb(0.5f, 0.5f, 0.5f, 1.0f));
        assertTrue(saturated.toHsl()[1] > 0.9f);
        Color litHue = Color.srgb(1.0f, 1.0f, 1.0f, 1.0f).luminosity(Color.srgb(1.0f, 0.0f, 0.0f, 1.0f));
        assertTrue(litHue.toHsl()[2] > 0.9f);
        Color tinted = Color.srgb(1.0f, 0.0f, 0.0f, 1.0f).color(Color.srgb(0.5f, 0.5f, 0.5f, 1.0f));
        assertEquals(0.0f, tinted.toHsl()[0], 1.0f);
        assertTrue(tinted.toHsl()[1] > 0.9f);
        assertEquals(0.5f, tinted.toHsl()[2], 0.05f);
        Color darkened = Color.extendedLinear(1.0f, 0.0f, 0.0f, 1.0f)
                .darken(Color.extendedLinear(0.0f, 0.0f, 1.0f, 1.0f));
        assertEquals(0.0f, darkened.red(), EPSILON);
        assertEquals(0.0f, darkened.blue(), EPSILON);
        Color lightened = Color.extendedLinear(1.0f, 0.0f, 0.0f, 1.0f)
                .lighten(Color.extendedLinear(0.0f, 0.0f, 1.0f, 1.0f));
        assertEquals(1.0f, lightened.red(), EPSILON);
        assertEquals(1.0f, lightened.blue(), EPSILON);
        Color pinned = Color.extendedLinear(0.75f, 0.25f, 0.25f, 1.0f)
                .pinLight(Color.extendedLinear(0.4f, 0.4f, 0.8f, 1.0f));
        assertEquals(ColorEncoding.EXTENDED_LINEAR, pinned.encoding());
        assertEquals(0.5f, pinned.red(), EPSILON);
        assertEquals(0.4f, pinned.green(), EPSILON);
        assertEquals(0.5f, pinned.blue(), EPSILON);
        Color vivid = Color.extendedLinear(1.0f, 0.0f, 0.0f, 1.0f)
                .vividLight(Color.extendedLinear(0.25f, 0.25f, 0.25f, 1.0f));
        assertEquals(1.0f, vivid.red(), EPSILON);
        assertEquals(0.0f, vivid.green(), EPSILON);
        Color linearLit = Color.extendedLinear(0.75f, 0.25f, 0.5f, 1.0f)
                .linearLight(Color.extendedLinear(0.4f, 0.4f, 0.4f, 1.0f));
        assertEquals(0.9f, linearLit.red(), EPSILON);
        assertEquals(-0.1f, linearLit.green(), EPSILON);
        assertEquals(0.4f, linearLit.blue(), EPSILON);
        Color hardMixed = Color.extendedLinear(1.0f, 0.0f, 0.0f, 1.0f)
                .hardMix(Color.extendedLinear(0.25f, 0.25f, 0.25f, 1.0f));
        assertEquals(ColorEncoding.EXTENDED_LINEAR, hardMixed.encoding());
        assertEquals(1.0f, hardMixed.red(), EPSILON);
        assertEquals(0.0f, hardMixed.green(), EPSILON);
        assertEquals(0.0f, hardMixed.blue(), EPSILON);
        assertEquals(1.0f, hardMixed.alpha(), EPSILON);
        Color plusDark = Color.extendedLinear(0.75f, 0.25f, 0.5f, 1.0f)
                .plusDarker(Color.extendedLinear(0.75f, 0.5f, 0.25f, 1.0f));
        assertEquals(ColorEncoding.EXTENDED_LINEAR, plusDark.encoding());
        assertEquals(0.5f, plusDark.red(), EPSILON);
        assertEquals(-0.25f, plusDark.green(), EPSILON);
        assertEquals(-0.25f, plusDark.blue(), EPSILON);
        Color plusLight = Color.extendedLinear(0.4f, 0.1f, 0.2f, 1.0f)
                .plusLighter(Color.extendedLinear(0.3f, 0.2f, 0.1f, 1.0f));
        assertEquals(0.7f, plusLight.red(), EPSILON);
        assertEquals(0.3f, plusLight.green(), EPSILON);
        assertEquals(0.3f, plusLight.blue(), EPSILON);
        Color negated = Color.extendedLinear(0.25f, 0.75f, 0.0f, 1.0f)
                .negation(Color.extendedLinear(0.25f, 0.25f, 1.0f, 1.0f));
        assertEquals(ColorEncoding.EXTENDED_LINEAR, negated.encoding());
        assertEquals(0.5f, negated.red(), EPSILON);
        assertEquals(1.0f, negated.green(), EPSILON);
        assertEquals(1.0f, negated.blue(), EPSILON);
        Color phoenixed = Color.extendedLinear(0.25f, 0.75f, 0.5f, 1.0f)
                .phoenix(Color.extendedLinear(0.75f, 0.25f, 0.5f, 1.0f));
        assertEquals(0.5f, phoenixed.red(), EPSILON);
        assertEquals(0.5f, phoenixed.green(), EPSILON);
        assertEquals(1.0f, phoenixed.blue(), EPSILON);
        Color reflected = Color.extendedLinear(0.5f, 1.0f, 0.0f, 1.0f)
                .reflect(Color.extendedLinear(0.5f, 0.25f, 0.4f, 1.0f));
        assertEquals(0.5f, reflected.red(), EPSILON);
        assertEquals(1.0f, reflected.green(), EPSILON);
        assertEquals(0.16f, reflected.blue(), EPSILON);
        Color glowed = Color.extendedLinear(0.5f, 0.4f, 0.4f, 1.0f)
                .glow(Color.extendedLinear(0.5f, 0.0f, 1.0f, 1.0f));
        assertEquals(ColorEncoding.EXTENDED_LINEAR, glowed.encoding());
        assertEquals(0.5f, glowed.red(), EPSILON);
        assertEquals(0.16f, glowed.green(), EPSILON);
        assertEquals(1.0f, glowed.blue(), EPSILON);
        Color frozen = Color.extendedLinear(0.5f, 0.0f, 1.0f, 1.0f)
                .freeze(Color.extendedLinear(0.5f, 0.5f, 1.0f, 1.0f));
        assertEquals(0.5f, frozen.red(), EPSILON);
        assertEquals(0.0f, frozen.green(), EPSILON);
        assertEquals(1.0f, frozen.blue(), EPSILON);
        Color heated = Color.extendedLinear(0.5f, 0.5f, 1.0f, 1.0f)
                .heat(Color.extendedLinear(0.5f, 0.0f, 1.0f, 1.0f));
        assertEquals(0.5f, heated.red(), EPSILON);
        assertEquals(0.0f, heated.green(), EPSILON);
        assertEquals(1.0f, heated.blue(), EPSILON);
        Color averaged = Color.extendedLinear(0.2f, 0.4f, 0.8f, 1.0f)
                .average(Color.extendedLinear(0.6f, 0.2f, 0.0f, 1.0f));
        assertEquals(ColorEncoding.EXTENDED_LINEAR, averaged.encoding());
        assertEquals(0.4f, averaged.red(), EPSILON);
        assertEquals(0.3f, averaged.green(), EPSILON);
        assertEquals(0.4f, averaged.blue(), EPSILON);
        Color subtracted = Color.extendedLinear(0.2f, 0.4f, 0.1f, 1.0f)
                .subtract(Color.extendedLinear(0.6f, 0.4f, 0.5f, 1.0f));
        assertEquals(0.4f, subtracted.red(), EPSILON);
        assertEquals(0.0f, subtracted.green(), EPSILON);
        assertEquals(0.4f, subtracted.blue(), EPSILON);
        Color divided = Color.extendedLinear(0.5f, 0.0f, 0.2f, 1.0f)
                .divide(Color.extendedLinear(0.4f, 0.0f, 0.1f, 1.0f));
        assertEquals(0.8f, divided.red(), EPSILON);
        assertEquals(0.0f, divided.green(), EPSILON);
        assertEquals(0.5f, divided.blue(), EPSILON);
        Color extracted = Color.extendedLinear(0.2f, 0.4f, 0.1f, 1.0f)
                .grainExtract(Color.extendedLinear(0.6f, 0.4f, 0.5f, 1.0f));
        assertEquals(ColorEncoding.EXTENDED_LINEAR, extracted.encoding());
        assertEquals(0.9f, extracted.red(), EPSILON);
        assertEquals(0.5f, extracted.green(), EPSILON);
        assertEquals(0.9f, extracted.blue(), EPSILON);
        Color merged = Color.extendedLinear(0.2f, 0.4f, 0.1f, 1.0f)
                .grainMerge(Color.extendedLinear(0.6f, 0.4f, 0.5f, 1.0f));
        assertEquals(0.3f, merged.red(), EPSILON);
        assertEquals(0.3f, merged.green(), EPSILON);
        assertEquals(0.1f, merged.blue(), EPSILON);
        Color inverse = Color.extendedLinear(0.7f, 0.4f, 0.2f, 1.0f)
                .inverseSubtract(Color.extendedLinear(0.2f, 0.4f, 0.5f, 1.0f));
        assertEquals(0.5f, inverse.red(), EPSILON);
        assertEquals(0.0f, inverse.green(), EPSILON);
        assertEquals(-0.3f, inverse.blue(), EPSILON);
        Color valued = Color.srgb(1.0f, 1.0f, 1.0f, 1.0f)
                .hsvValue(Color.srgb(0.5f, 0.5f, 0.5f, 1.0f));
        assertEquals(1.0f, valued.toHsv()[2], 0.05f);
        Color hsvHued = Color.srgb(1.0f, 0.0f, 0.0f, 1.0f)
                .hsvHue(Color.srgb(0.0f, 0.0f, 1.0f, 1.0f));
        assertEquals(0.0f, hsvHued.toHsv()[0], 1.0f);
        Color hsvSaturated = Color.srgb(0.5f, 0.5f, 0.5f, 1.0f)
                .hsvSaturation(Color.srgb(1.0f, 0.0f, 0.0f, 1.0f));
        assertTrue(hsvSaturated.toHsv()[1] < 0.1f);
        assertEquals(1.0f, hsvSaturated.toHsv()[2], 0.05f);
        Color hsvTinted = Color.srgb(1.0f, 0.0f, 0.0f, 1.0f)
                .hsvColor(Color.srgb(0.5f, 0.5f, 0.5f, 1.0f));
        assertEquals(0.0f, hsvTinted.toHsv()[0], 1.0f);
        assertTrue(hsvTinted.toHsv()[1] > 0.9f);
        assertEquals(0.5f, hsvTinted.toHsv()[2], 0.05f);
        Color valuedCopy = Color.srgb(1.0f, 0.0f, 0.0f, 1.0f).withHsvValue(0.5f);
        assertEquals(0.5f, valuedCopy.toHsv()[2], 0.05f);
        Color huedCopy = Color.srgb(1.0f, 0.0f, 0.0f, 1.0f).withHsvHue(240.0f);
        assertEquals(240.0f, huedCopy.toHsv()[0], 1.0f);
        Color satCopy = Color.srgb(1.0f, 0.0f, 0.0f, 1.0f).withHsvSaturation(0.5f);
        assertEquals(0.5f, satCopy.toHsv()[1], 0.05f);
        Color hsvComp = Color.srgb(1.0f, 0.0f, 0.0f, 1.0f).hsvComplementary();
        assertEquals(180.0f, hsvComp.toHsv()[0], 1.0f);
        Color hsvRot = Color.srgb(1.0f, 0.0f, 0.0f, 1.0f).hsvRotate(120.0f);
        assertEquals(120.0f, hsvRot.toHsv()[0], 1.0f);
        Color satUp = Color.srgb(0.5f, 0.5f, 0.5f, 1.0f).hsvSaturate(0.5f);
        assertEquals(0.5f, satUp.toHsv()[1], 0.05f);
        Color satDown = Color.srgb(1.0f, 0.0f, 0.0f, 1.0f).hsvDesaturate(0.5f);
        assertEquals(0.5f, satDown.toHsv()[1], 0.05f);
        Color bright = Color.srgb(1.0f, 0.0f, 0.0f, 1.0f).withHsvValue(0.4f).hsvBrighten(0.2f);
        assertEquals(0.6f, bright.toHsv()[2], 0.05f);
        Color hsvDarkened = Color.srgb(1.0f, 0.0f, 0.0f, 1.0f).withHsvValue(0.6f).hsvDarken(0.2f);
        assertEquals(0.4f, hsvDarkened.toHsv()[2], 0.05f);
        Color hsvMid = Color.srgb(1.0f, 0.0f, 0.0f, 1.0f)
                .interpolateHsv(Color.srgb(0.0f, 0.0f, 1.0f, 1.0f), 0.5f)
                .toSrgb();
        assertEquals(300.0f, hsvMid.toHsv()[0], 1.0f);
        Color hslMid = Color.srgb(1.0f, 0.0f, 0.0f, 1.0f)
                .interpolateHsl(Color.srgb(0.0f, 0.0f, 1.0f, 1.0f), 0.5f)
                .toSrgb();
        assertEquals(300.0f, hslMid.toHsl()[0], 1.0f);
        Color valueInv = Color.srgb(1.0f, 0.0f, 0.0f, 1.0f).withHsvValue(0.25f).hsvInvertValue();
        assertEquals(0.75f, valueInv.toHsv()[2], 0.05f);
        Color satInv = Color.srgb(1.0f, 0.0f, 0.0f, 1.0f).withHsvSaturation(0.25f).hsvInvertSaturation();
        assertEquals(0.75f, satInv.toHsv()[1], 0.05f);
        Color hsvSet = Color.srgb(1.0f, 0.0f, 0.0f, 1.0f).withHsv(120.0f, 0.5f, 0.4f);
        assertEquals(120.0f, hsvSet.toHsv()[0], 1.0f);
        assertEquals(0.5f, hsvSet.toHsv()[1], 0.05f);
        assertEquals(0.4f, hsvSet.toHsv()[2], 0.05f);
        Color hslLightInv = Color.srgb(1.0f, 0.0f, 0.0f, 1.0f).withLightness(0.25f).hslInvertLightness();
        assertEquals(0.75f, hslLightInv.toHsl()[2], 0.05f);
        Color hslSatInv = Color.srgb(1.0f, 0.0f, 0.0f, 1.0f).withSaturation(0.25f).hslInvertSaturation();
        assertEquals(0.75f, hslSatInv.toHsl()[1], 0.05f);
        Color hslSet = Color.srgb(1.0f, 0.0f, 0.0f, 1.0f).withHsl(120.0f, 0.5f, 0.4f);
        assertEquals(120.0f, hslSet.toHsl()[0], 1.0f);
        assertEquals(0.5f, hslSet.toHsl()[1], 0.05f);
        assertEquals(0.4f, hslSet.toHsl()[2], 0.05f);
        Color hsvHueInv = Color.srgb(1.0f, 1.0f, 0.0f, 1.0f).hsvInvertHue();
        assertEquals(300.0f, hsvHueInv.toHsv()[0], 1.0f);
        Color hslHueInv = Color.srgb(1.0f, 1.0f, 0.0f, 1.0f).hslInvertHue();
        assertEquals(300.0f, hslHueInv.toHsl()[0], 1.0f);
        Color oklchHueInv = Color.fromOklch(0.6f, 0.15f, 40.0f, 1.0f).oklchInvertHue();
        assertEquals(320.0f, oklchHueInv.toOklch()[2], 1.0f);
        Color oklchLightInv = Color.fromOklch(0.6f, 0.15f, 40.0f, 1.0f).oklchInvertLightness();
        assertEquals(0.4f, oklchLightInv.toOklch()[0], 0.05f);
        Color cieLabLInv = Color.fromCieLab(25.0f, 10.0f, 5.0f, 1.0f).cieLabInvertL();
        assertEquals(75.0f, cieLabLInv.toCieLab()[0], 0.5f);
        Color cieLchHueInv = Color.fromCieLch(50.0f, 20.0f, 40.0f, 1.0f).cieLchInvertHue();
        assertEquals(320.0f, cieLchHueInv.toCieLch()[2], 1.0f);
        Color cieLabAInv = Color.fromCieLab(50.0f, 20.0f, 10.0f, 1.0f).cieLabInvertA();
        assertEquals(-20.0f, cieLabAInv.toCieLab()[1], 0.5f);
        Color cieLabBInv = Color.fromCieLab(50.0f, 20.0f, 10.0f, 1.0f).cieLabInvertB();
        assertEquals(-10.0f, cieLabBInv.toCieLab()[2], 0.5f);
        Color oklabAInv = Color.fromOklab(0.6f, 0.12f, 0.04f, 1.0f).oklabInvertA();
        assertEquals(-0.12f, oklabAInv.toOklab()[1], 0.02f);
        Color oklabBInv = Color.fromOklab(0.6f, 0.12f, 0.04f, 1.0f).oklabInvertB();
        assertEquals(-0.04f, oklabBInv.toOklab()[2], 0.02f);
        Color oklabLightInv = Color.fromOklab(0.6f, 0.12f, 0.04f, 1.0f).oklabInvertLightness();
        assertEquals(0.4f, oklabLightInv.toOklab()[0], 0.05f);
        Color cieLabAbInv = Color.fromCieLab(50.0f, 20.0f, 10.0f, 1.0f).cieLabInvertAb();
        assertEquals(-20.0f, cieLabAbInv.toCieLab()[1], 0.5f);
        assertEquals(-10.0f, cieLabAbInv.toCieLab()[2], 0.5f);
        Color oklabAbInv = Color.fromOklab(0.7f, 0.06f, 0.03f, 1.0f).oklabInvertAb();
        assertEquals(-0.06f, oklabAbInv.toOklab()[1], 0.05f);
        assertEquals(-0.03f, oklabAbInv.toOklab()[2], 0.05f);
        Color oklchComp = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchComplementary();
        assertEquals(220.0f, oklchComp.toOklch()[2], 8.0f);
        Color cieLchComp = Color.fromCieLch(60.0f, 12.0f, 40.0f, 1.0f).cieLchComplementary();
        assertEquals(220.0f, cieLchComp.toCieLch()[2], 8.0f);
        Color cieLchLightInv = Color.fromCieLch(30.0f, 8.0f, 40.0f, 1.0f).cieLchInvertLightness();
        assertEquals(70.0f, cieLchLightInv.toCieLch()[0], 1.0f);
        Color cieLuvLInv = Color.fromCieLuv(25.0f, 10.0f, 5.0f, 1.0f).cieLuvInvertL();
        assertEquals(75.0f, cieLuvLInv.toCieLuv()[0], 1.0f);
        Color cieLuvUInv = Color.fromCieLuv(50.0f, 20.0f, 10.0f, 1.0f).cieLuvInvertU();
        assertEquals(-20.0f, cieLuvUInv.toCieLuv()[1], 1.0f);
        Color cieLuvVInv = Color.fromCieLuv(50.0f, 8.0f, 12.0f, 1.0f).cieLuvInvertV();
        assertEquals(-12.0f, cieLuvVInv.toCieLuv()[2], 1.0f);
        Color cieLuvUvInv = Color.fromCieLuv(50.0f, 8.0f, 6.0f, 1.0f).cieLuvInvertUv();
        assertEquals(-8.0f, cieLuvUvInv.toCieLuv()[1], 1.0f);
        assertEquals(-6.0f, cieLuvUvInv.toCieLuv()[2], 1.0f);
        Color cieLuvLvInv = Color.fromCieLuv(25.0f, 8.0f, 6.0f, 1.0f).cieLuvInvertLv();
        assertEquals(75.0f, cieLuvLvInv.toCieLuv()[0], 1.0f);
        assertEquals(-6.0f, cieLuvLvInv.toCieLuv()[2], 1.0f);
        Color cieLuvLuInv = Color.fromCieLuv(30.0f, 8.0f, 6.0f, 1.0f).cieLuvInvertLu();
        assertEquals(70.0f, cieLuvLuInv.toCieLuv()[0], 1.0f);
        assertEquals(-8.0f, cieLuvLuInv.toCieLuv()[1], 1.0f);
        Color cieLuvLuvInv = Color.fromCieLuv(30.0f, 8.0f, 6.0f, 1.0f).cieLuvInvertLuv();
        assertEquals(70.0f, cieLuvLuvInv.toCieLuv()[0], 1.0f);
        assertEquals(-8.0f, cieLuvLuvInv.toCieLuv()[1], 1.0f);
        assertEquals(-6.0f, cieLuvLuvInv.toCieLuv()[2], 1.0f);
        Color oklabLaInv = Color.fromOklab(0.7f, 0.06f, 0.03f, 1.0f).oklabInvertLa();
        assertEquals(0.3f, oklabLaInv.toOklab()[0], 0.05f);
        assertEquals(-0.06f, oklabLaInv.toOklab()[1], 0.05f);
        Color oklabLbInv = Color.fromOklab(0.7f, 0.06f, 0.03f, 1.0f).oklabInvertLb();
        assertEquals(0.3f, oklabLbInv.toOklab()[0], 0.05f);
        assertEquals(-0.03f, oklabLbInv.toOklab()[2], 0.05f);
        Color oklabLabInv = Color.fromOklab(0.7f, 0.06f, 0.03f, 1.0f).oklabInvertLab();
        assertEquals(0.3f, oklabLabInv.toOklab()[0], 0.05f);
        assertEquals(-0.06f, oklabLabInv.toOklab()[1], 0.05f);
        assertEquals(-0.03f, oklabLabInv.toOklab()[2], 0.05f);
        Color cieLabLaInv = Color.fromCieLab(30.0f, 8.0f, 6.0f, 1.0f).cieLabInvertLa();
        assertEquals(70.0f, cieLabLaInv.toCieLab()[0], 1.0f);
        assertEquals(-8.0f, cieLabLaInv.toCieLab()[1], 1.0f);
        Color cieLabLbInv = Color.fromCieLab(30.0f, 8.0f, 6.0f, 1.0f).cieLabInvertLb();
        assertEquals(70.0f, cieLabLbInv.toCieLab()[0], 1.0f);
        assertEquals(-6.0f, cieLabLbInv.toCieLab()[2], 1.0f);
        Color cieLabLabInv = Color.fromCieLab(30.0f, 8.0f, 6.0f, 1.0f).cieLabInvertLab();
        assertEquals(70.0f, cieLabLabInv.toCieLab()[0], 1.0f);
        assertEquals(-8.0f, cieLabLabInv.toCieLab()[1], 1.0f);
        assertEquals(-6.0f, cieLabLabInv.toCieLab()[2], 1.0f);
        Color cieLchLhInv = Color.fromCieLch(30.0f, 8.0f, 40.0f, 1.0f).cieLchInvertLh();
        assertEquals(70.0f, cieLchLhInv.toCieLch()[0], 1.0f);
        assertEquals(320.0f, cieLchLhInv.toCieLch()[2], 1.0f);
        Color oklchLhInv = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchInvertLh();
        assertEquals(0.3f, oklchLhInv.toOklch()[0], 0.05f);
        assertEquals(320.0f, oklchLhInv.toOklch()[2], 8.0f);
        Color hsvHvInv = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvInvertHv();
        assertEquals(320.0f, hsvHvInv.toHsv()[0], 1.0f);
        assertEquals(0.3f, hsvHvInv.toHsv()[2], 0.05f);
        Color hslHlInv = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslInvertHl();
        assertEquals(320.0f, hslHlInv.toHsl()[0], 1.0f);
        assertEquals(0.3f, hslHlInv.toHsl()[2], 0.05f);
        Color hsvHsInv = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvInvertHs();
        assertEquals(320.0f, hsvHsInv.toHsv()[0], 1.0f);
        assertEquals(0.6f, hsvHsInv.toHsv()[1], 0.05f);
        Color hslHsInv = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslInvertHs();
        assertEquals(320.0f, hslHsInv.toHsl()[0], 1.0f);
        assertEquals(0.6f, hslHsInv.toHsl()[1], 0.05f);
        Color hsvSvInv = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvInvertSv();
        assertEquals(0.6f, hsvSvInv.toHsv()[1], 0.05f);
        assertEquals(0.3f, hsvSvInv.toHsv()[2], 0.05f);
        Color hslSlInv = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslInvertSl();
        assertEquals(0.6f, hslSlInv.toHsl()[1], 0.05f);
        assertEquals(0.3f, hslSlInv.toHsl()[2], 0.05f);
        Color hsvHsvInv = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvInvertHsv();
        assertEquals(320.0f, hsvHsvInv.toHsv()[0], 1.0f);
        assertEquals(0.6f, hsvHsvInv.toHsv()[1], 0.05f);
        assertEquals(0.3f, hsvHsvInv.toHsv()[2], 0.05f);
        Color hslHslInv = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslInvertHsl();
        assertEquals(320.0f, hslHslInv.toHsl()[0], 1.0f);
        assertEquals(0.6f, hslHslInv.toHsl()[1], 0.05f);
        assertEquals(0.3f, hslHslInv.toHsl()[2], 0.05f);
        Color oklchLcInv = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchInvertLc();
        assertEquals(0.3f, oklchLcInv.toOklch()[0], 0.05f);
        assertEquals(0.0f, oklchLcInv.toOklch()[1], 0.05f);
        Color cieLchLcInv = Color.fromCieLch(30.0f, 8.0f, 40.0f, 1.0f).cieLchInvertLc();
        assertEquals(70.0f, cieLchLcInv.toCieLch()[0], 1.0f);
        assertEquals(0.0f, cieLchLcInv.toCieLch()[1], 1.0f);
        Color oklabLcInv = Color.fromOklab(0.7f, 0.06f, 0.03f, 1.0f).oklabInvertLc();
        assertEquals(0.3f, oklabLcInv.toOklab()[0], 0.05f);
        assertEquals(0.0f, oklabLcInv.toOklab()[1], 0.05f);
        assertEquals(0.0f, oklabLcInv.toOklab()[2], 0.05f);
        Color cieLabLcInv = Color.fromCieLab(30.0f, 8.0f, 6.0f, 1.0f).cieLabInvertLc();
        assertEquals(70.0f, cieLabLcInv.toCieLab()[0], 1.0f);
        assertEquals(0.0f, cieLabLcInv.toCieLab()[1], 1.0f);
        assertEquals(0.0f, cieLabLcInv.toCieLab()[2], 1.0f);
        Color cieLuvLcInv = Color.fromCieLuv(30.0f, 8.0f, 6.0f, 1.0f).cieLuvInvertLc();
        assertEquals(70.0f, cieLuvLcInv.toCieLuv()[0], 1.0f);
        assertEquals(0.0f, cieLuvLcInv.toCieLuv()[1], 1.0f);
        assertEquals(0.0f, cieLuvLcInv.toCieLuv()[2], 1.0f);
        Color cieLchCInv = Color.fromCieLch(50.0f, 8.0f, 40.0f, 1.0f).cieLchInvertC();
        assertEquals(50.0f, cieLchCInv.toCieLch()[0], 1.0f);
        assertEquals(0.0f, cieLchCInv.toCieLch()[1], 1.0f);
        Color oklchCInv = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchInvertC();
        assertEquals(0.7f, oklchCInv.toOklch()[0], 0.05f);
        assertEquals(0.0f, oklchCInv.toOklch()[1], 0.05f);
        Color oklabCInv = Color.fromOklab(0.7f, 0.06f, 0.03f, 1.0f).oklabInvertC();
        assertEquals(0.7f, oklabCInv.toOklab()[0], 0.05f);
        assertEquals(0.0f, oklabCInv.toOklab()[1], 0.05f);
        assertEquals(0.0f, oklabCInv.toOklab()[2], 0.05f);
        Color cieLabCOnly = Color.fromCieLab(50.0f, 8.0f, 6.0f, 1.0f).cieLabInvertC();
        assertEquals(50.0f, cieLabCOnly.toCieLab()[0], 1.0f);
        assertEquals(0.0f, cieLabCOnly.toCieLab()[1], 1.0f);
        assertEquals(0.0f, cieLabCOnly.toCieLab()[2], 1.0f);
        Color cieLuvCOnly = Color.fromCieLuv(50.0f, 8.0f, 6.0f, 1.0f).cieLuvInvertC();
        assertEquals(50.0f, cieLuvCOnly.toCieLuv()[0], 1.0f);
        assertEquals(0.0f, cieLuvCOnly.toCieLuv()[1], 1.0f);
        assertEquals(0.0f, cieLuvCOnly.toCieLuv()[2], 1.0f);
        Color hsvCInv = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvInvertC();
        assertEquals(0.0f, hsvCInv.toHsv()[1], 0.05f);
        assertEquals(0.7f, hsvCInv.toHsv()[2], 0.05f);
        Color hslCInv = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslInvertC();
        assertEquals(0.0f, hslCInv.toHsl()[1], 0.05f);
        assertEquals(0.7f, hslCInv.toHsl()[2], 0.05f);
        Color oklchChInv = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchInvertCh();
        assertEquals(0.7f, oklchChInv.toOklch()[0], 0.05f);
        assertEquals(0.0f, oklchChInv.toOklch()[1], 0.05f);
        Color cieLchChInv = Color.fromCieLch(50.0f, 8.0f, 40.0f, 1.0f).cieLchInvertCh();
        assertEquals(50.0f, cieLchChInv.toCieLch()[0], 1.0f);
        assertEquals(0.0f, cieLchChInv.toCieLch()[1], 1.0f);
        Color hsvChInv = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvInvertCh();
        assertEquals(0.0f, hsvChInv.toHsv()[1], 0.05f);
        assertEquals(0.7f, hsvChInv.toHsv()[2], 0.05f);
        Color hslChInv = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslInvertCh();
        assertEquals(0.0f, hslChInv.toHsl()[1], 0.05f);
        assertEquals(0.7f, hslChInv.toHsl()[2], 0.05f);
        Color oklchLchInv = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchInvertLch();
        assertEquals(0.3f, oklchLchInv.toOklch()[0], 0.05f);
        assertEquals(0.0f, oklchLchInv.toOklch()[1], 0.05f);
        Color cieLchLchInv = Color.fromCieLch(30.0f, 8.0f, 40.0f, 1.0f).cieLchInvertLch();
        assertEquals(70.0f, cieLchLchInv.toCieLch()[0], 1.0f);
        assertEquals(0.0f, cieLchLchInv.toCieLch()[1], 1.0f);
        Color hsvLchInv = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvInvertLch();
        assertEquals(0.0f, hsvLchInv.toHsv()[1], 0.05f);
        assertEquals(0.3f, hsvLchInv.toHsv()[2], 0.05f);
        Color hslLchInv = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslInvertLch();
        assertEquals(0.0f, hslLchInv.toHsl()[1], 0.05f);
        assertEquals(0.3f, hslLchInv.toHsl()[2], 0.05f);
        Color hsvLcInv = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvInvertLc();
        assertEquals(0.0f, hsvLcInv.toHsv()[1], 0.05f);
        assertEquals(0.3f, hsvLcInv.toHsv()[2], 0.05f);
        Color hslLcInv = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslInvertLc();
        assertEquals(0.0f, hslLcInv.toHsl()[1], 0.05f);
        assertEquals(0.3f, hslLcInv.toHsl()[2], 0.05f);
        Color oklchCompLInv = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchComplementaryInvertL();
        assertEquals(0.3f, oklchCompLInv.toOklch()[0], 0.05f);
        assertEquals(220.0f, oklchCompLInv.toOklch()[2], 8.0f);
        Color cieLchCompLInv = Color.fromCieLch(30.0f, 8.0f, 40.0f, 1.0f).cieLchComplementaryInvertL();
        assertEquals(70.0f, cieLchCompLInv.toCieLch()[0], 1.0f);
        assertEquals(220.0f, cieLchCompLInv.toCieLch()[2], 8.0f);
        Color oklchCompCInv = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchComplementaryInvertC();
        assertEquals(0.7f, oklchCompCInv.toOklch()[0], 0.05f);
        assertEquals(0.0f, oklchCompCInv.toOklch()[1], 0.05f);
        Color cieLchCompCInv = Color.fromCieLch(50.0f, 8.0f, 40.0f, 1.0f).cieLchComplementaryInvertC();
        assertEquals(50.0f, cieLchCompCInv.toCieLch()[0], 1.0f);
        assertEquals(0.0f, cieLchCompCInv.toCieLch()[1], 1.0f);
        Color hsvCompCInv = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvComplementaryInvertC();
        assertEquals(0.0f, hsvCompCInv.toHsv()[1], 0.05f);
        assertEquals(0.7f, hsvCompCInv.toHsv()[2], 0.05f);
        Color hslCompCInv = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslComplementaryInvertC();
        assertEquals(0.0f, hslCompCInv.toHsl()[1], 0.05f);
        assertEquals(0.7f, hslCompCInv.toHsl()[2], 0.05f);
        Color hsvCompVInv = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvComplementaryInvertV();
        assertEquals(220.0f, hsvCompVInv.toHsv()[0], 8.0f);
        assertEquals(0.3f, hsvCompVInv.toHsv()[2], 0.05f);
        Color hslCompLInv = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslComplementaryInvertL();
        assertEquals(220.0f, hslCompLInv.toHsl()[0], 8.0f);
        assertEquals(0.3f, hslCompLInv.toHsl()[2], 0.05f);
        Color hsvCompLcInv = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvComplementaryInvertLc();
        assertEquals(0.0f, hsvCompLcInv.toHsv()[1], 0.05f);
        assertEquals(0.3f, hsvCompLcInv.toHsv()[2], 0.05f);
        Color hslCompLcInv = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslComplementaryInvertLc();
        assertEquals(0.0f, hslCompLcInv.toHsl()[1], 0.05f);
        assertEquals(0.3f, hslCompLcInv.toHsl()[2], 0.05f);
        Color oklchCompLcInv = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchComplementaryInvertLc();
        assertEquals(0.3f, oklchCompLcInv.toOklch()[0], 0.05f);
        assertEquals(0.0f, oklchCompLcInv.toOklch()[1], 0.05f);
        Color cieLchCompLcInv = Color.fromCieLch(30.0f, 8.0f, 40.0f, 1.0f).cieLchComplementaryInvertLc();
        assertEquals(70.0f, cieLchCompLcInv.toCieLch()[0], 1.0f);
        assertEquals(0.0f, cieLchCompLcInv.toCieLch()[1], 1.0f);
        Color hsvCompSInv = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvComplementaryInvertS();
        assertEquals(220.0f, hsvCompSInv.toHsv()[0], 8.0f);
        assertEquals(0.6f, hsvCompSInv.toHsv()[1], 0.05f);
        Color hslCompSInv = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslComplementaryInvertS();
        assertEquals(220.0f, hslCompSInv.toHsl()[0], 8.0f);
        assertEquals(0.6f, hslCompSInv.toHsl()[1], 0.05f);
        Color hsvCompSvInv = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvComplementaryInvertSv();
        assertEquals(0.6f, hsvCompSvInv.toHsv()[1], 0.05f);
        assertEquals(0.3f, hsvCompSvInv.toHsv()[2], 0.05f);
        Color hslCompSlInv = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslComplementaryInvertSl();
        assertEquals(0.6f, hslCompSlInv.toHsl()[1], 0.05f);
        assertEquals(0.3f, hslCompSlInv.toHsl()[2], 0.05f);
        Color oklchRot90 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate90();
        assertEquals(0.7f, oklchRot90.toOklch()[0], 0.05f);
        assertEquals(130.0f, oklchRot90.toOklch()[2], 8.0f);
        Color cieLchRot90 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate90();
        assertEquals(50.0f, cieLchRot90.toCieLch()[0], 1.0f);
        assertEquals(130.0f, cieLchRot90.toCieLch()[2], 8.0f);
        Color hsvRot90 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate90();
        assertEquals(130.0f, hsvRot90.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot90.toHsv()[2], 0.05f);
        Color hslRot90 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate90();
        assertEquals(130.0f, hslRot90.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot90.toHsl()[2], 0.05f);
        Color oklchRot270 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate270();
        assertEquals(0.7f, oklchRot270.toOklch()[0], 0.05f);
        assertEquals(310.0f, oklchRot270.toOklch()[2], 8.0f);
        Color cieLchRot270 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate270();
        assertEquals(50.0f, cieLchRot270.toCieLch()[0], 1.0f);
        assertEquals(310.0f, cieLchRot270.toCieLch()[2], 8.0f);
        Color hsvRot270 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate270();
        assertEquals(310.0f, hsvRot270.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot270.toHsv()[2], 0.05f);
        Color hslRot270 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate270();
        assertEquals(310.0f, hslRot270.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot270.toHsl()[2], 0.05f);
        Color oklchRot45 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate45();
        assertEquals(0.7f, oklchRot45.toOklch()[0], 0.05f);
        assertEquals(85.0f, oklchRot45.toOklch()[2], 8.0f);
        Color cieLchRot45 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate45();
        assertEquals(50.0f, cieLchRot45.toCieLch()[0], 1.0f);
        assertEquals(85.0f, cieLchRot45.toCieLch()[2], 8.0f);
        Color hsvRot45 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate45();
        assertEquals(85.0f, hsvRot45.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot45.toHsv()[2], 0.05f);
        Color hslRot45 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate45();
        assertEquals(85.0f, hslRot45.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot45.toHsl()[2], 0.05f);
        Color oklchRot135 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate135();
        assertEquals(0.7f, oklchRot135.toOklch()[0], 0.05f);
        assertEquals(175.0f, oklchRot135.toOklch()[2], 8.0f);
        Color cieLchRot135 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate135();
        assertEquals(50.0f, cieLchRot135.toCieLch()[0], 1.0f);
        assertEquals(175.0f, cieLchRot135.toCieLch()[2], 8.0f);
        Color hsvRot135 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate135();
        assertEquals(175.0f, hsvRot135.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot135.toHsv()[2], 0.05f);
        Color hslRot135 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate135();
        assertEquals(175.0f, hslRot135.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot135.toHsl()[2], 0.05f);
        Color oklchRot225 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate225();
        assertEquals(0.7f, oklchRot225.toOklch()[0], 0.05f);
        assertEquals(265.0f, oklchRot225.toOklch()[2], 8.0f);
        Color cieLchRot225 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate225();
        assertEquals(50.0f, cieLchRot225.toCieLch()[0], 1.0f);
        assertEquals(265.0f, cieLchRot225.toCieLch()[2], 8.0f);
        Color hsvRot225 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate225();
        assertEquals(265.0f, hsvRot225.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot225.toHsv()[2], 0.05f);
        Color hslRot225 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate225();
        assertEquals(265.0f, hslRot225.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot225.toHsl()[2], 0.05f);
        Color oklchRot315 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate315();
        assertEquals(0.7f, oklchRot315.toOklch()[0], 0.05f);
        assertEquals(355.0f, oklchRot315.toOklch()[2], 8.0f);
        Color cieLchRot315 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate315();
        assertEquals(50.0f, cieLchRot315.toCieLch()[0], 1.0f);
        assertEquals(355.0f, cieLchRot315.toCieLch()[2], 8.0f);
        Color hsvRot315 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate315();
        assertEquals(355.0f, hsvRot315.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot315.toHsv()[2], 0.05f);
        Color hslRot315 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate315();
        assertEquals(355.0f, hslRot315.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot315.toHsl()[2], 0.05f);
        Color oklchRot30 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate30();
        assertEquals(0.7f, oklchRot30.toOklch()[0], 0.05f);
        assertEquals(70.0f, oklchRot30.toOklch()[2], 8.0f);
        Color cieLchRot30 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate30();
        assertEquals(50.0f, cieLchRot30.toCieLch()[0], 1.0f);
        assertEquals(70.0f, cieLchRot30.toCieLch()[2], 8.0f);
        Color hsvRot30 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate30();
        assertEquals(70.0f, hsvRot30.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot30.toHsv()[2], 0.05f);
        Color hslRot30 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate30();
        assertEquals(70.0f, hslRot30.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot30.toHsl()[2], 0.05f);
        Color oklchRot60 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate60();
        assertEquals(0.7f, oklchRot60.toOklch()[0], 0.05f);
        assertEquals(100.0f, oklchRot60.toOklch()[2], 8.0f);
        Color cieLchRot60 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate60();
        assertEquals(50.0f, cieLchRot60.toCieLch()[0], 1.0f);
        assertEquals(100.0f, cieLchRot60.toCieLch()[2], 8.0f);
        Color hsvRot60 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate60();
        assertEquals(100.0f, hsvRot60.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot60.toHsv()[2], 0.05f);
        Color hslRot60 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate60();
        assertEquals(100.0f, hslRot60.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot60.toHsl()[2], 0.05f);
        Color oklchRot120 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate120();
        assertEquals(0.7f, oklchRot120.toOklch()[0], 0.05f);
        assertEquals(160.0f, oklchRot120.toOklch()[2], 8.0f);
        Color cieLchRot120 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate120();
        assertEquals(50.0f, cieLchRot120.toCieLch()[0], 1.0f);
        assertEquals(160.0f, cieLchRot120.toCieLch()[2], 8.0f);
        Color hsvRot120 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate120();
        assertEquals(160.0f, hsvRot120.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot120.toHsv()[2], 0.05f);
        Color hslRot120 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate120();
        assertEquals(160.0f, hslRot120.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot120.toHsl()[2], 0.05f);
        Color oklchRot150 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate150();
        assertEquals(0.7f, oklchRot150.toOklch()[0], 0.05f);
        assertEquals(190.0f, oklchRot150.toOklch()[2], 8.0f);
        Color cieLchRot150 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate150();
        assertEquals(50.0f, cieLchRot150.toCieLch()[0], 1.0f);
        assertEquals(190.0f, cieLchRot150.toCieLch()[2], 8.0f);
        Color hsvRot150 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate150();
        assertEquals(190.0f, hsvRot150.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot150.toHsv()[2], 0.05f);
        Color hslRot150 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate150();
        assertEquals(190.0f, hslRot150.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot150.toHsl()[2], 0.05f);
        Color oklchRot75 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate75();
        assertEquals(0.7f, oklchRot75.toOklch()[0], 0.05f);
        assertEquals(115.0f, oklchRot75.toOklch()[2], 8.0f);
        Color cieLchRot75 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate75();
        assertEquals(50.0f, cieLchRot75.toCieLch()[0], 1.0f);
        assertEquals(115.0f, cieLchRot75.toCieLch()[2], 8.0f);
        Color hsvRot75 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate75();
        assertEquals(115.0f, hsvRot75.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot75.toHsv()[2], 0.05f);
        Color hslRot75 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate75();
        assertEquals(115.0f, hslRot75.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot75.toHsl()[2], 0.05f);
        Color oklchRot105 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate105();
        assertEquals(0.7f, oklchRot105.toOklch()[0], 0.05f);
        assertEquals(145.0f, oklchRot105.toOklch()[2], 8.0f);
        Color cieLchRot105 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate105();
        assertEquals(50.0f, cieLchRot105.toCieLch()[0], 1.0f);
        assertEquals(145.0f, cieLchRot105.toCieLch()[2], 8.0f);
        Color hsvRot105 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate105();
        assertEquals(145.0f, hsvRot105.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot105.toHsv()[2], 0.05f);
        Color hslRot105 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate105();
        assertEquals(145.0f, hslRot105.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot105.toHsl()[2], 0.05f);
        Color oklchRot165 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate165();
        assertEquals(0.7f, oklchRot165.toOklch()[0], 0.05f);
        assertEquals(205.0f, oklchRot165.toOklch()[2], 8.0f);
        Color cieLchRot165 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate165();
        assertEquals(50.0f, cieLchRot165.toCieLch()[0], 1.0f);
        assertEquals(205.0f, cieLchRot165.toCieLch()[2], 8.0f);
        Color hsvRot165 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate165();
        assertEquals(205.0f, hsvRot165.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot165.toHsv()[2], 0.05f);
        Color hslRot165 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate165();
        assertEquals(205.0f, hslRot165.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot165.toHsl()[2], 0.05f);
        Color oklchRot15 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate15();
        assertEquals(0.7f, oklchRot15.toOklch()[0], 0.05f);
        assertEquals(55.0f, oklchRot15.toOklch()[2], 8.0f);
        Color cieLchRot15 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate15();
        assertEquals(50.0f, cieLchRot15.toCieLch()[0], 1.0f);
        assertEquals(55.0f, cieLchRot15.toCieLch()[2], 8.0f);
        Color hsvRot15 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate15();
        assertEquals(55.0f, hsvRot15.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot15.toHsv()[2], 0.05f);
        Color hslRot15 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate15();
        assertEquals(55.0f, hslRot15.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot15.toHsl()[2], 0.05f);
        Color oklchRot195 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate195();
        assertEquals(0.7f, oklchRot195.toOklch()[0], 0.05f);
        assertEquals(235.0f, oklchRot195.toOklch()[2], 8.0f);
        Color cieLchRot195 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate195();
        assertEquals(50.0f, cieLchRot195.toCieLch()[0], 1.0f);
        assertEquals(235.0f, cieLchRot195.toCieLch()[2], 8.0f);
        Color hsvRot195 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate195();
        assertEquals(235.0f, hsvRot195.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot195.toHsv()[2], 0.05f);
        Color hslRot195 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate195();
        assertEquals(235.0f, hslRot195.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot195.toHsl()[2], 0.05f);
        Color oklchRot210 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate210();
        assertEquals(0.7f, oklchRot210.toOklch()[0], 0.05f);
        assertEquals(250.0f, oklchRot210.toOklch()[2], 8.0f);
        Color cieLchRot210 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate210();
        assertEquals(50.0f, cieLchRot210.toCieLch()[0], 1.0f);
        assertEquals(250.0f, cieLchRot210.toCieLch()[2], 8.0f);
        Color hsvRot210 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate210();
        assertEquals(250.0f, hsvRot210.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot210.toHsv()[2], 0.05f);
        Color hslRot210 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate210();
        assertEquals(250.0f, hslRot210.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot210.toHsl()[2], 0.05f);
        Color oklchRot240 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate240();
        assertEquals(0.7f, oklchRot240.toOklch()[0], 0.05f);
        assertEquals(280.0f, oklchRot240.toOklch()[2], 8.0f);
        Color cieLchRot240 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate240();
        assertEquals(50.0f, cieLchRot240.toCieLch()[0], 1.0f);
        assertEquals(280.0f, cieLchRot240.toCieLch()[2], 8.0f);
        Color hsvRot240 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate240();
        assertEquals(280.0f, hsvRot240.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot240.toHsv()[2], 0.05f);
        Color hslRot240 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate240();
        assertEquals(280.0f, hslRot240.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot240.toHsl()[2], 0.05f);
        Color oklchRot255 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate255();
        assertEquals(0.7f, oklchRot255.toOklch()[0], 0.05f);
        assertEquals(295.0f, oklchRot255.toOklch()[2], 8.0f);
        Color cieLchRot255 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate255();
        assertEquals(50.0f, cieLchRot255.toCieLch()[0], 1.0f);
        assertEquals(295.0f, cieLchRot255.toCieLch()[2], 8.0f);
        Color hsvRot255 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate255();
        assertEquals(295.0f, hsvRot255.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot255.toHsv()[2], 0.05f);
        Color hslRot255 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate255();
        assertEquals(295.0f, hslRot255.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot255.toHsl()[2], 0.05f);
        Color oklchRot300 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate300();
        assertEquals(0.7f, oklchRot300.toOklch()[0], 0.05f);
        assertEquals(340.0f, oklchRot300.toOklch()[2], 8.0f);
        Color cieLchRot300 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate300();
        assertEquals(50.0f, cieLchRot300.toCieLch()[0], 1.0f);
        assertEquals(340.0f, cieLchRot300.toCieLch()[2], 8.0f);
        Color hsvRot300 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate300();
        assertEquals(340.0f, hsvRot300.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot300.toHsv()[2], 0.05f);
        Color hslRot300 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate300();
        assertEquals(340.0f, hslRot300.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot300.toHsl()[2], 0.05f);
        Color oklchRot330 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate330();
        assertEquals(0.7f, oklchRot330.toOklch()[0], 0.05f);
        assertEquals(10.0f, oklchRot330.toOklch()[2], 8.0f);
        Color cieLchRot330 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate330();
        assertEquals(50.0f, cieLchRot330.toCieLch()[0], 1.0f);
        assertEquals(10.0f, cieLchRot330.toCieLch()[2], 8.0f);
        Color hsvRot330 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate330();
        assertEquals(10.0f, hsvRot330.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot330.toHsv()[2], 0.05f);
        Color hslRot330 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate330();
        assertEquals(10.0f, hslRot330.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot330.toHsl()[2], 0.05f);
        Color oklchRot345 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate345();
        assertEquals(0.7f, oklchRot345.toOklch()[0], 0.05f);
        assertEquals(25.0f, oklchRot345.toOklch()[2], 8.0f);
        Color cieLchRot345 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate345();
        assertEquals(50.0f, cieLchRot345.toCieLch()[0], 1.0f);
        assertEquals(25.0f, cieLchRot345.toCieLch()[2], 8.0f);
        Color hsvRot345 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate345();
        assertEquals(25.0f, hsvRot345.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot345.toHsv()[2], 0.05f);
        Color hslRot345 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate345();
        assertEquals(25.0f, hslRot345.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot345.toHsl()[2], 0.05f);
        Color oklchRot5 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate5();
        assertEquals(0.7f, oklchRot5.toOklch()[0], 0.05f);
        assertEquals(45.0f, oklchRot5.toOklch()[2], 8.0f);
        Color cieLchRot5 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate5();
        assertEquals(50.0f, cieLchRot5.toCieLch()[0], 1.0f);
        assertEquals(45.0f, cieLchRot5.toCieLch()[2], 8.0f);
        Color hsvRot5 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate5();
        assertEquals(45.0f, hsvRot5.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot5.toHsv()[2], 0.05f);
        Color hslRot5 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate5();
        assertEquals(45.0f, hslRot5.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot5.toHsl()[2], 0.05f);
        Color oklchRot10 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate10();
        assertEquals(0.7f, oklchRot10.toOklch()[0], 0.05f);
        assertEquals(50.0f, oklchRot10.toOklch()[2], 8.0f);
        Color cieLchRot10 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate10();
        assertEquals(50.0f, cieLchRot10.toCieLch()[0], 1.0f);
        assertEquals(50.0f, cieLchRot10.toCieLch()[2], 8.0f);
        Color hsvRot10 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate10();
        assertEquals(50.0f, hsvRot10.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot10.toHsv()[2], 0.05f);
        Color hslRot10 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate10();
        assertEquals(50.0f, hslRot10.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot10.toHsl()[2], 0.05f);
        Color oklchRot20 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate20();
        assertEquals(0.7f, oklchRot20.toOklch()[0], 0.05f);
        assertEquals(60.0f, oklchRot20.toOklch()[2], 8.0f);
        Color cieLchRot20 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate20();
        assertEquals(50.0f, cieLchRot20.toCieLch()[0], 1.0f);
        assertEquals(60.0f, cieLchRot20.toCieLch()[2], 8.0f);
        Color hsvRot20 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate20();
        assertEquals(60.0f, hsvRot20.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot20.toHsv()[2], 0.05f);
        Color hslRot20 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate20();
        assertEquals(60.0f, hslRot20.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot20.toHsl()[2], 0.05f);
        Color oklchRot25 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate25();
        assertEquals(0.7f, oklchRot25.toOklch()[0], 0.05f);
        assertEquals(65.0f, oklchRot25.toOklch()[2], 8.0f);
        Color cieLchRot25 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate25();
        assertEquals(50.0f, cieLchRot25.toCieLch()[0], 1.0f);
        assertEquals(65.0f, cieLchRot25.toCieLch()[2], 8.0f);
        Color hsvRot25 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate25();
        assertEquals(65.0f, hsvRot25.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot25.toHsv()[2], 0.05f);
        Color hslRot25 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate25();
        assertEquals(65.0f, hslRot25.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot25.toHsl()[2], 0.05f);
        Color oklchRot35 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate35();
        assertEquals(0.7f, oklchRot35.toOklch()[0], 0.05f);
        assertEquals(75.0f, oklchRot35.toOklch()[2], 8.0f);
        Color cieLchRot35 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate35();
        assertEquals(50.0f, cieLchRot35.toCieLch()[0], 1.0f);
        assertEquals(75.0f, cieLchRot35.toCieLch()[2], 8.0f);
        Color hsvRot35 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate35();
        assertEquals(75.0f, hsvRot35.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot35.toHsv()[2], 0.05f);
        Color hslRot35 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate35();
        assertEquals(75.0f, hslRot35.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot35.toHsl()[2], 0.05f);
        Color oklchRot40 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate40();
        assertEquals(0.7f, oklchRot40.toOklch()[0], 0.05f);
        assertEquals(80.0f, oklchRot40.toOklch()[2], 8.0f);
        Color cieLchRot40 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate40();
        assertEquals(50.0f, cieLchRot40.toCieLch()[0], 1.0f);
        assertEquals(80.0f, cieLchRot40.toCieLch()[2], 8.0f);
        Color hsvRot40 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate40();
        assertEquals(80.0f, hsvRot40.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot40.toHsv()[2], 0.05f);
        Color hslRot40 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate40();
        assertEquals(80.0f, hslRot40.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot40.toHsl()[2], 0.05f);
        Color oklchRot50 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate50();
        assertEquals(0.7f, oklchRot50.toOklch()[0], 0.05f);
        assertEquals(90.0f, oklchRot50.toOklch()[2], 8.0f);
        Color cieLchRot50 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate50();
        assertEquals(50.0f, cieLchRot50.toCieLch()[0], 1.0f);
        assertEquals(90.0f, cieLchRot50.toCieLch()[2], 8.0f);
        Color hsvRot50 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate50();
        assertEquals(90.0f, hsvRot50.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot50.toHsv()[2], 0.05f);
        Color hslRot50 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate50();
        assertEquals(90.0f, hslRot50.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot50.toHsl()[2], 0.05f);
        Color oklchRot55 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate55();
        assertEquals(0.7f, oklchRot55.toOklch()[0], 0.05f);
        assertEquals(95.0f, oklchRot55.toOklch()[2], 8.0f);
        Color cieLchRot55 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate55();
        assertEquals(50.0f, cieLchRot55.toCieLch()[0], 1.0f);
        assertEquals(95.0f, cieLchRot55.toCieLch()[2], 8.0f);
        Color hsvRot55 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate55();
        assertEquals(95.0f, hsvRot55.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot55.toHsv()[2], 0.05f);
        Color hslRot55 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate55();
        assertEquals(95.0f, hslRot55.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot55.toHsl()[2], 0.05f);
        Color oklchRot65 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate65();
        assertEquals(0.7f, oklchRot65.toOklch()[0], 0.05f);
        assertEquals(105.0f, oklchRot65.toOklch()[2], 8.0f);
        Color cieLchRot65 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate65();
        assertEquals(50.0f, cieLchRot65.toCieLch()[0], 1.0f);
        assertEquals(105.0f, cieLchRot65.toCieLch()[2], 8.0f);
        Color hsvRot65 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate65();
        assertEquals(105.0f, hsvRot65.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot65.toHsv()[2], 0.05f);
        Color hslRot65 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate65();
        assertEquals(105.0f, hslRot65.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot65.toHsl()[2], 0.05f);
        Color oklchRot70 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate70();
        assertEquals(0.7f, oklchRot70.toOklch()[0], 0.05f);
        assertEquals(110.0f, oklchRot70.toOklch()[2], 8.0f);
        Color cieLchRot70 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate70();
        assertEquals(50.0f, cieLchRot70.toCieLch()[0], 1.0f);
        assertEquals(110.0f, cieLchRot70.toCieLch()[2], 8.0f);
        Color hsvRot70 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate70();
        assertEquals(110.0f, hsvRot70.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot70.toHsv()[2], 0.05f);
        Color hslRot70 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate70();
        assertEquals(110.0f, hslRot70.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot70.toHsl()[2], 0.05f);
        Color oklchRot80 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate80();
        assertEquals(0.7f, oklchRot80.toOklch()[0], 0.05f);
        assertEquals(120.0f, oklchRot80.toOklch()[2], 8.0f);
        Color cieLchRot80 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate80();
        assertEquals(50.0f, cieLchRot80.toCieLch()[0], 1.0f);
        assertEquals(120.0f, cieLchRot80.toCieLch()[2], 8.0f);
        Color hsvRot80 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate80();
        assertEquals(120.0f, hsvRot80.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot80.toHsv()[2], 0.05f);
        Color hslRot80 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate80();
        assertEquals(120.0f, hslRot80.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot80.toHsl()[2], 0.05f);
        Color oklchRot85 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate85();
        assertEquals(0.7f, oklchRot85.toOklch()[0], 0.05f);
        assertEquals(125.0f, oklchRot85.toOklch()[2], 8.0f);
        Color cieLchRot85 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate85();
        assertEquals(50.0f, cieLchRot85.toCieLch()[0], 1.0f);
        assertEquals(125.0f, cieLchRot85.toCieLch()[2], 8.0f);
        Color hsvRot85 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate85();
        assertEquals(125.0f, hsvRot85.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot85.toHsv()[2], 0.05f);
        Color hslRot85 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate85();
        assertEquals(125.0f, hslRot85.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot85.toHsl()[2], 0.05f);
        Color oklchRot95 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate95();
        assertEquals(0.7f, oklchRot95.toOklch()[0], 0.05f);
        assertEquals(135.0f, oklchRot95.toOklch()[2], 8.0f);
        Color cieLchRot95 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate95();
        assertEquals(50.0f, cieLchRot95.toCieLch()[0], 1.0f);
        assertEquals(135.0f, cieLchRot95.toCieLch()[2], 8.0f);
        Color hsvRot95 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate95();
        assertEquals(135.0f, hsvRot95.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot95.toHsv()[2], 0.05f);
        Color hslRot95 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate95();
        assertEquals(135.0f, hslRot95.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot95.toHsl()[2], 0.05f);
        Color oklchRot100 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate100();
        assertEquals(0.7f, oklchRot100.toOklch()[0], 0.05f);
        assertEquals(140.0f, oklchRot100.toOklch()[2], 8.0f);
        Color cieLchRot100 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate100();
        assertEquals(50.0f, cieLchRot100.toCieLch()[0], 1.0f);
        assertEquals(140.0f, cieLchRot100.toCieLch()[2], 8.0f);
        Color hsvRot100 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate100();
        assertEquals(140.0f, hsvRot100.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot100.toHsv()[2], 0.05f);
        Color hslRot100 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate100();
        assertEquals(140.0f, hslRot100.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot100.toHsl()[2], 0.05f);
        Color oklchRot110 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate110();
        assertEquals(0.7f, oklchRot110.toOklch()[0], 0.05f);
        assertEquals(150.0f, oklchRot110.toOklch()[2], 8.0f);
        Color cieLchRot110 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate110();
        assertEquals(50.0f, cieLchRot110.toCieLch()[0], 1.0f);
        assertEquals(150.0f, cieLchRot110.toCieLch()[2], 8.0f);
        Color hsvRot110 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate110();
        assertEquals(150.0f, hsvRot110.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot110.toHsv()[2], 0.05f);
        Color hslRot110 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate110();
        assertEquals(150.0f, hslRot110.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot110.toHsl()[2], 0.05f);
        Color oklchRot115 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate115();
        assertEquals(0.7f, oklchRot115.toOklch()[0], 0.05f);
        assertEquals(155.0f, oklchRot115.toOklch()[2], 8.0f);
        Color cieLchRot115 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate115();
        assertEquals(50.0f, cieLchRot115.toCieLch()[0], 1.0f);
        assertEquals(155.0f, cieLchRot115.toCieLch()[2], 8.0f);
        Color hsvRot115 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate115();
        assertEquals(155.0f, hsvRot115.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot115.toHsv()[2], 0.05f);
        Color hslRot115 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate115();
        assertEquals(155.0f, hslRot115.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot115.toHsl()[2], 0.05f);
        Color oklchRot125 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate125();
        assertEquals(0.7f, oklchRot125.toOklch()[0], 0.05f);
        assertEquals(165.0f, oklchRot125.toOklch()[2], 8.0f);
        Color cieLchRot125 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate125();
        assertEquals(50.0f, cieLchRot125.toCieLch()[0], 1.0f);
        assertEquals(165.0f, cieLchRot125.toCieLch()[2], 8.0f);
        Color hsvRot125 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate125();
        assertEquals(165.0f, hsvRot125.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot125.toHsv()[2], 0.05f);
        Color hslRot125 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate125();
        assertEquals(165.0f, hslRot125.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot125.toHsl()[2], 0.05f);
        Color oklchRot130 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate130();
        assertEquals(0.7f, oklchRot130.toOklch()[0], 0.05f);
        assertEquals(170.0f, oklchRot130.toOklch()[2], 8.0f);
        Color cieLchRot130 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate130();
        assertEquals(50.0f, cieLchRot130.toCieLch()[0], 1.0f);
        assertEquals(170.0f, cieLchRot130.toCieLch()[2], 8.0f);
        Color hsvRot130 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate130();
        assertEquals(170.0f, hsvRot130.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot130.toHsv()[2], 0.05f);
        Color hslRot130 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate130();
        assertEquals(170.0f, hslRot130.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot130.toHsl()[2], 0.05f);
        Color oklchRot140 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate140();
        assertEquals(0.7f, oklchRot140.toOklch()[0], 0.05f);
        assertEquals(180.0f, oklchRot140.toOklch()[2], 8.0f);
        Color cieLchRot140 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate140();
        assertEquals(50.0f, cieLchRot140.toCieLch()[0], 1.0f);
        assertEquals(180.0f, cieLchRot140.toCieLch()[2], 8.0f);
        Color hsvRot140 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate140();
        assertEquals(180.0f, hsvRot140.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot140.toHsv()[2], 0.05f);
        Color hslRot140 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate140();
        assertEquals(180.0f, hslRot140.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot140.toHsl()[2], 0.05f);
        Color oklchRot145 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate145();
        assertEquals(0.7f, oklchRot145.toOklch()[0], 0.05f);
        assertEquals(185.0f, oklchRot145.toOklch()[2], 8.0f);
        Color cieLchRot145 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate145();
        assertEquals(50.0f, cieLchRot145.toCieLch()[0], 1.0f);
        assertEquals(185.0f, cieLchRot145.toCieLch()[2], 8.0f);
        Color hsvRot145 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate145();
        assertEquals(185.0f, hsvRot145.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot145.toHsv()[2], 0.05f);
        Color hslRot145 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate145();
        assertEquals(185.0f, hslRot145.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot145.toHsl()[2], 0.05f);
        Color oklchRot155 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate155();
        assertEquals(0.7f, oklchRot155.toOklch()[0], 0.05f);
        assertEquals(195.0f, oklchRot155.toOklch()[2], 8.0f);
        Color cieLchRot155 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate155();
        assertEquals(50.0f, cieLchRot155.toCieLch()[0], 1.0f);
        assertEquals(195.0f, cieLchRot155.toCieLch()[2], 8.0f);
        Color hsvRot155 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate155();
        assertEquals(195.0f, hsvRot155.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot155.toHsv()[2], 0.05f);
        Color hslRot155 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate155();
        assertEquals(195.0f, hslRot155.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot155.toHsl()[2], 0.05f);
        Color oklchRot160 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate160();
        assertEquals(0.7f, oklchRot160.toOklch()[0], 0.05f);
        assertEquals(200.0f, oklchRot160.toOklch()[2], 8.0f);
        Color cieLchRot160 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate160();
        assertEquals(50.0f, cieLchRot160.toCieLch()[0], 1.0f);
        assertEquals(200.0f, cieLchRot160.toCieLch()[2], 8.0f);
        Color hsvRot160 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate160();
        assertEquals(200.0f, hsvRot160.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot160.toHsv()[2], 0.05f);
        Color hslRot160 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate160();
        assertEquals(200.0f, hslRot160.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot160.toHsl()[2], 0.05f);
        Color oklchRot170 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate170();
        assertEquals(0.7f, oklchRot170.toOklch()[0], 0.05f);
        assertEquals(210.0f, oklchRot170.toOklch()[2], 8.0f);
        Color cieLchRot170 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate170();
        assertEquals(50.0f, cieLchRot170.toCieLch()[0], 1.0f);
        assertEquals(210.0f, cieLchRot170.toCieLch()[2], 8.0f);
        Color hsvRot170 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate170();
        assertEquals(210.0f, hsvRot170.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot170.toHsv()[2], 0.05f);
        Color hslRot170 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate170();
        assertEquals(210.0f, hslRot170.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot170.toHsl()[2], 0.05f);
        Color oklchRot175 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate175();
        assertEquals(0.7f, oklchRot175.toOklch()[0], 0.05f);
        assertEquals(215.0f, oklchRot175.toOklch()[2], 8.0f);
        Color cieLchRot175 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate175();
        assertEquals(50.0f, cieLchRot175.toCieLch()[0], 1.0f);
        assertEquals(215.0f, cieLchRot175.toCieLch()[2], 8.0f);
        Color hsvRot175 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate175();
        assertEquals(215.0f, hsvRot175.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot175.toHsv()[2], 0.05f);
        Color hslRot175 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate175();
        assertEquals(215.0f, hslRot175.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot175.toHsl()[2], 0.05f);
        Color oklchRot185 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate185();
        assertEquals(0.7f, oklchRot185.toOklch()[0], 0.05f);
        assertEquals(225.0f, oklchRot185.toOklch()[2], 8.0f);
        Color cieLchRot185 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate185();
        assertEquals(50.0f, cieLchRot185.toCieLch()[0], 1.0f);
        assertEquals(225.0f, cieLchRot185.toCieLch()[2], 8.0f);
        Color hsvRot185 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate185();
        assertEquals(225.0f, hsvRot185.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot185.toHsv()[2], 0.05f);
        Color hslRot185 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate185();
        assertEquals(225.0f, hslRot185.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot185.toHsl()[2], 0.05f);
        Color oklchRot190 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate190();
        assertEquals(0.7f, oklchRot190.toOklch()[0], 0.05f);
        assertEquals(230.0f, oklchRot190.toOklch()[2], 8.0f);
        Color cieLchRot190 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate190();
        assertEquals(50.0f, cieLchRot190.toCieLch()[0], 1.0f);
        assertEquals(230.0f, cieLchRot190.toCieLch()[2], 8.0f);
        Color hsvRot190 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate190();
        assertEquals(230.0f, hsvRot190.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot190.toHsv()[2], 0.05f);
        Color hslRot190 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate190();
        assertEquals(230.0f, hslRot190.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot190.toHsl()[2], 0.05f);
        Color oklchRot200 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate200();
        assertEquals(0.7f, oklchRot200.toOklch()[0], 0.05f);
        assertEquals(240.0f, oklchRot200.toOklch()[2], 8.0f);
        Color cieLchRot200 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate200();
        assertEquals(50.0f, cieLchRot200.toCieLch()[0], 1.0f);
        assertEquals(240.0f, cieLchRot200.toCieLch()[2], 8.0f);
        Color hsvRot200 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate200();
        assertEquals(240.0f, hsvRot200.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot200.toHsv()[2], 0.05f);
        Color hslRot200 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate200();
        assertEquals(240.0f, hslRot200.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot200.toHsl()[2], 0.05f);
        Color oklchRot205 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate205();
        assertEquals(0.7f, oklchRot205.toOklch()[0], 0.05f);
        assertEquals(245.0f, oklchRot205.toOklch()[2], 8.0f);
        Color cieLchRot205 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate205();
        assertEquals(50.0f, cieLchRot205.toCieLch()[0], 1.0f);
        assertEquals(245.0f, cieLchRot205.toCieLch()[2], 8.0f);
        Color hsvRot205 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate205();
        assertEquals(245.0f, hsvRot205.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot205.toHsv()[2], 0.05f);
        Color hslRot205 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate205();
        assertEquals(245.0f, hslRot205.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot205.toHsl()[2], 0.05f);
        Color oklchRot215 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate215();
        assertEquals(0.7f, oklchRot215.toOklch()[0], 0.05f);
        assertEquals(255.0f, oklchRot215.toOklch()[2], 8.0f);
        Color cieLchRot215 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate215();
        assertEquals(50.0f, cieLchRot215.toCieLch()[0], 1.0f);
        assertEquals(255.0f, cieLchRot215.toCieLch()[2], 8.0f);
        Color hsvRot215 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate215();
        assertEquals(255.0f, hsvRot215.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot215.toHsv()[2], 0.05f);
        Color hslRot215 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate215();
        assertEquals(255.0f, hslRot215.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot215.toHsl()[2], 0.05f);
        Color oklchRot220 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate220();
        assertEquals(0.7f, oklchRot220.toOklch()[0], 0.05f);
        assertEquals(260.0f, oklchRot220.toOklch()[2], 8.0f);
        Color cieLchRot220 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate220();
        assertEquals(50.0f, cieLchRot220.toCieLch()[0], 1.0f);
        assertEquals(260.0f, cieLchRot220.toCieLch()[2], 8.0f);
        Color hsvRot220 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate220();
        assertEquals(260.0f, hsvRot220.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot220.toHsv()[2], 0.05f);
        Color hslRot220 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate220();
        assertEquals(260.0f, hslRot220.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot220.toHsl()[2], 0.05f);
        Color oklchRot230 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate230();
        assertEquals(0.7f, oklchRot230.toOklch()[0], 0.05f);
        assertEquals(270.0f, oklchRot230.toOklch()[2], 8.0f);
        Color cieLchRot230 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate230();
        assertEquals(50.0f, cieLchRot230.toCieLch()[0], 1.0f);
        assertEquals(270.0f, cieLchRot230.toCieLch()[2], 8.0f);
        Color hsvRot230 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate230();
        assertEquals(270.0f, hsvRot230.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot230.toHsv()[2], 0.05f);
        Color hslRot230 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate230();
        assertEquals(270.0f, hslRot230.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot230.toHsl()[2], 0.05f);
        Color oklchRot235 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate235();
        assertEquals(0.7f, oklchRot235.toOklch()[0], 0.05f);
        assertEquals(275.0f, oklchRot235.toOklch()[2], 8.0f);
        Color cieLchRot235 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate235();
        assertEquals(50.0f, cieLchRot235.toCieLch()[0], 1.0f);
        assertEquals(275.0f, cieLchRot235.toCieLch()[2], 8.0f);
        Color hsvRot235 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate235();
        assertEquals(275.0f, hsvRot235.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot235.toHsv()[2], 0.05f);
        Color hslRot235 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate235();
        assertEquals(275.0f, hslRot235.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot235.toHsl()[2], 0.05f);
        Color oklchRot245 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate245();
        assertEquals(0.7f, oklchRot245.toOklch()[0], 0.05f);
        assertEquals(285.0f, oklchRot245.toOklch()[2], 8.0f);
        Color cieLchRot245 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate245();
        assertEquals(50.0f, cieLchRot245.toCieLch()[0], 1.0f);
        assertEquals(285.0f, cieLchRot245.toCieLch()[2], 8.0f);
        Color hsvRot245 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate245();
        assertEquals(285.0f, hsvRot245.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot245.toHsv()[2], 0.05f);
        Color hslRot245 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate245();
        assertEquals(285.0f, hslRot245.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot245.toHsl()[2], 0.05f);
        Color oklchRot250 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate250();
        assertEquals(0.7f, oklchRot250.toOklch()[0], 0.05f);
        assertEquals(290.0f, oklchRot250.toOklch()[2], 8.0f);
        Color cieLchRot250 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate250();
        assertEquals(50.0f, cieLchRot250.toCieLch()[0], 1.0f);
        assertEquals(290.0f, cieLchRot250.toCieLch()[2], 8.0f);
        Color hsvRot250 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate250();
        assertEquals(290.0f, hsvRot250.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot250.toHsv()[2], 0.05f);
        Color hslRot250 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate250();
        assertEquals(290.0f, hslRot250.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot250.toHsl()[2], 0.05f);
        Color oklchRot260 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate260();
        assertEquals(0.7f, oklchRot260.toOklch()[0], 0.05f);
        assertEquals(300.0f, oklchRot260.toOklch()[2], 8.0f);
        Color cieLchRot260 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate260();
        assertEquals(50.0f, cieLchRot260.toCieLch()[0], 1.0f);
        assertEquals(300.0f, cieLchRot260.toCieLch()[2], 8.0f);
        Color hsvRot260 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate260();
        assertEquals(300.0f, hsvRot260.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot260.toHsv()[2], 0.05f);
        Color hslRot260 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate260();
        assertEquals(300.0f, hslRot260.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot260.toHsl()[2], 0.05f);
        Color oklchRot265 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate265();
        assertEquals(0.7f, oklchRot265.toOklch()[0], 0.05f);
        assertEquals(305.0f, oklchRot265.toOklch()[2], 8.0f);
        Color cieLchRot265 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate265();
        assertEquals(50.0f, cieLchRot265.toCieLch()[0], 1.0f);
        assertEquals(305.0f, cieLchRot265.toCieLch()[2], 8.0f);
        Color hsvRot265 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate265();
        assertEquals(305.0f, hsvRot265.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot265.toHsv()[2], 0.05f);
        Color hslRot265 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate265();
        assertEquals(305.0f, hslRot265.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot265.toHsl()[2], 0.05f);
        Color oklchRot275 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate275();
        assertEquals(0.7f, oklchRot275.toOklch()[0], 0.05f);
        assertEquals(315.0f, oklchRot275.toOklch()[2], 8.0f);
        Color cieLchRot275 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate275();
        assertEquals(50.0f, cieLchRot275.toCieLch()[0], 1.0f);
        assertEquals(315.0f, cieLchRot275.toCieLch()[2], 8.0f);
        Color hsvRot275 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate275();
        assertEquals(315.0f, hsvRot275.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot275.toHsv()[2], 0.05f);
        Color hslRot275 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate275();
        assertEquals(315.0f, hslRot275.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot275.toHsl()[2], 0.05f);
        Color oklchRot280 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate280();
        assertEquals(0.7f, oklchRot280.toOklch()[0], 0.05f);
        assertEquals(320.0f, oklchRot280.toOklch()[2], 8.0f);
        Color cieLchRot280 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate280();
        assertEquals(50.0f, cieLchRot280.toCieLch()[0], 1.0f);
        assertEquals(320.0f, cieLchRot280.toCieLch()[2], 8.0f);
        Color hsvRot280 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate280();
        assertEquals(320.0f, hsvRot280.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot280.toHsv()[2], 0.05f);
        Color hslRot280 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate280();
        assertEquals(320.0f, hslRot280.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot280.toHsl()[2], 0.05f);
        Color oklchRot285 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate285();
        assertEquals(0.7f, oklchRot285.toOklch()[0], 0.05f);
        assertEquals(325.0f, oklchRot285.toOklch()[2], 8.0f);
        Color cieLchRot285 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate285();
        assertEquals(50.0f, cieLchRot285.toCieLch()[0], 1.0f);
        assertEquals(325.0f, cieLchRot285.toCieLch()[2], 8.0f);
        Color hsvRot285 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate285();
        assertEquals(325.0f, hsvRot285.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot285.toHsv()[2], 0.05f);
        Color hslRot285 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate285();
        assertEquals(325.0f, hslRot285.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot285.toHsl()[2], 0.05f);
        Color oklchRot290 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate290();
        assertEquals(0.7f, oklchRot290.toOklch()[0], 0.05f);
        assertEquals(330.0f, oklchRot290.toOklch()[2], 8.0f);
        Color cieLchRot290 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate290();
        assertEquals(50.0f, cieLchRot290.toCieLch()[0], 1.0f);
        assertEquals(330.0f, cieLchRot290.toCieLch()[2], 8.0f);
        Color hsvRot290 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate290();
        assertEquals(330.0f, hsvRot290.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot290.toHsv()[2], 0.05f);
        Color hslRot290 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate290();
        assertEquals(330.0f, hslRot290.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot290.toHsl()[2], 0.05f);
        Color oklchRot295 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate295();
        assertEquals(0.7f, oklchRot295.toOklch()[0], 0.05f);
        assertEquals(335.0f, oklchRot295.toOklch()[2], 8.0f);
        Color cieLchRot295 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate295();
        assertEquals(50.0f, cieLchRot295.toCieLch()[0], 1.0f);
        assertEquals(335.0f, cieLchRot295.toCieLch()[2], 8.0f);
        Color hsvRot295 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate295();
        assertEquals(335.0f, hsvRot295.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot295.toHsv()[2], 0.05f);
        Color hslRot295 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate295();
        assertEquals(335.0f, hslRot295.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot295.toHsl()[2], 0.05f);
        Color oklchRot305 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate305();
        assertEquals(0.7f, oklchRot305.toOklch()[0], 0.05f);
        assertEquals(345.0f, oklchRot305.toOklch()[2], 8.0f);
        Color cieLchRot305 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate305();
        assertEquals(50.0f, cieLchRot305.toCieLch()[0], 1.0f);
        assertEquals(345.0f, cieLchRot305.toCieLch()[2], 8.0f);
        Color hsvRot305 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate305();
        assertEquals(345.0f, hsvRot305.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot305.toHsv()[2], 0.05f);
        Color hslRot305 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate305();
        assertEquals(345.0f, hslRot305.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot305.toHsl()[2], 0.05f);
        Color oklchRot310 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate310();
        assertEquals(0.7f, oklchRot310.toOklch()[0], 0.05f);
        assertEquals(350.0f, oklchRot310.toOklch()[2], 8.0f);
        Color cieLchRot310 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate310();
        assertEquals(50.0f, cieLchRot310.toCieLch()[0], 1.0f);
        assertEquals(350.0f, cieLchRot310.toCieLch()[2], 8.0f);
        Color hsvRot310 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate310();
        assertEquals(350.0f, hsvRot310.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot310.toHsv()[2], 0.05f);
        Color hslRot310 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate310();
        assertEquals(350.0f, hslRot310.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot310.toHsl()[2], 0.05f);
        Color oklchRot320 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate320();
        assertEquals(0.7f, oklchRot320.toOklch()[0], 0.05f);
        assertEquals(0.0f, oklchRot320.toOklch()[2], 8.0f);
        Color cieLchRot320 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate320();
        assertEquals(50.0f, cieLchRot320.toCieLch()[0], 1.0f);
        assertEquals(0.0f, cieLchRot320.toCieLch()[2], 8.0f);
        Color hsvRot320 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate320();
        assertEquals(0.0f, hsvRot320.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot320.toHsv()[2], 0.05f);
        Color hslRot320 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate320();
        assertEquals(0.0f, hslRot320.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot320.toHsl()[2], 0.05f);
        Color oklchRot325 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate325();
        assertEquals(0.7f, oklchRot325.toOklch()[0], 0.05f);
        assertEquals(5.0f, oklchRot325.toOklch()[2], 8.0f);
        Color cieLchRot325 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate325();
        assertEquals(50.0f, cieLchRot325.toCieLch()[0], 1.0f);
        assertEquals(5.0f, cieLchRot325.toCieLch()[2], 8.0f);
        Color hsvRot325 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate325();
        assertEquals(5.0f, hsvRot325.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot325.toHsv()[2], 0.05f);
        Color hslRot325 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate325();
        assertEquals(5.0f, hslRot325.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot325.toHsl()[2], 0.05f);
        Color oklchRot335 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate335();
        assertEquals(0.7f, oklchRot335.toOklch()[0], 0.05f);
        assertEquals(15.0f, oklchRot335.toOklch()[2], 8.0f);
        Color cieLchRot335 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate335();
        assertEquals(50.0f, cieLchRot335.toCieLch()[0], 1.0f);
        assertEquals(15.0f, cieLchRot335.toCieLch()[2], 8.0f);
        Color hsvRot335 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate335();
        assertEquals(15.0f, hsvRot335.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot335.toHsv()[2], 0.05f);
        Color hslRot335 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate335();
        assertEquals(15.0f, hslRot335.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot335.toHsl()[2], 0.05f);
        Color oklchRot340 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate340();
        assertEquals(0.7f, oklchRot340.toOklch()[0], 0.05f);
        assertEquals(20.0f, oklchRot340.toOklch()[2], 8.0f);
        Color cieLchRot340 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate340();
        assertEquals(50.0f, cieLchRot340.toCieLch()[0], 1.0f);
        assertEquals(20.0f, cieLchRot340.toCieLch()[2], 8.0f);
        Color hsvRot340 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate340();
        assertEquals(20.0f, hsvRot340.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot340.toHsv()[2], 0.05f);
        Color hslRot340 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate340();
        assertEquals(20.0f, hslRot340.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot340.toHsl()[2], 0.05f);
        Color oklchRot350 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate350();
        assertEquals(0.7f, oklchRot350.toOklch()[0], 0.05f);
        assertEquals(30.0f, oklchRot350.toOklch()[2], 8.0f);
        Color cieLchRot350 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate350();
        assertEquals(50.0f, cieLchRot350.toCieLch()[0], 1.0f);
        assertEquals(30.0f, cieLchRot350.toCieLch()[2], 8.0f);
        Color hsvRot350 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate350();
        assertEquals(30.0f, hsvRot350.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot350.toHsv()[2], 0.05f);
        Color hslRot350 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate350();
        assertEquals(30.0f, hslRot350.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot350.toHsl()[2], 0.05f);
        Color oklchRot355 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate355();
        assertEquals(0.7f, oklchRot355.toOklch()[0], 0.05f);
        assertEquals(35.0f, oklchRot355.toOklch()[2], 8.0f);
        Color cieLchRot355 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate355();
        assertEquals(50.0f, cieLchRot355.toCieLch()[0], 1.0f);
        assertEquals(35.0f, cieLchRot355.toCieLch()[2], 8.0f);
        Color hsvRot355 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate355();
        assertEquals(35.0f, hsvRot355.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot355.toHsv()[2], 0.05f);
        Color hslRot355 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate355();
        assertEquals(35.0f, hslRot355.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot355.toHsl()[2], 0.05f);
        Color oklchRot1 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate1();
        assertEquals(0.7f, oklchRot1.toOklch()[0], 0.05f);
        assertEquals(41.0f, oklchRot1.toOklch()[2], 8.0f);
        Color cieLchRot1 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate1();
        assertEquals(50.0f, cieLchRot1.toCieLch()[0], 1.0f);
        assertEquals(41.0f, cieLchRot1.toCieLch()[2], 8.0f);
        Color hsvRot1 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate1();
        assertEquals(41.0f, hsvRot1.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot1.toHsv()[2], 0.05f);
        Color hslRot1 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate1();
        assertEquals(41.0f, hslRot1.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot1.toHsl()[2], 0.05f);
        Color oklchRot2 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate2();
        assertEquals(0.7f, oklchRot2.toOklch()[0], 0.05f);
        assertEquals(42.0f, oklchRot2.toOklch()[2], 8.0f);
        Color cieLchRot2 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate2();
        assertEquals(50.0f, cieLchRot2.toCieLch()[0], 1.0f);
        assertEquals(42.0f, cieLchRot2.toCieLch()[2], 8.0f);
        Color hsvRot2 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate2();
        assertEquals(42.0f, hsvRot2.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot2.toHsv()[2], 0.05f);
        Color hslRot2 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate2();
        assertEquals(42.0f, hslRot2.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot2.toHsl()[2], 0.05f);
        Color oklchRot3 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate3();
        assertEquals(0.7f, oklchRot3.toOklch()[0], 0.05f);
        assertEquals(43.0f, oklchRot3.toOklch()[2], 8.0f);
        Color cieLchRot3 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate3();
        assertEquals(50.0f, cieLchRot3.toCieLch()[0], 1.0f);
        assertEquals(43.0f, cieLchRot3.toCieLch()[2], 8.0f);
        Color hsvRot3 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate3();
        assertEquals(43.0f, hsvRot3.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot3.toHsv()[2], 0.05f);
        Color hslRot3 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate3();
        assertEquals(43.0f, hslRot3.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot3.toHsl()[2], 0.05f);
        Color oklchRot4 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate4();
        assertEquals(0.7f, oklchRot4.toOklch()[0], 0.05f);
        assertEquals(44.0f, oklchRot4.toOklch()[2], 8.0f);
        Color cieLchRot4 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate4();
        assertEquals(50.0f, cieLchRot4.toCieLch()[0], 1.0f);
        assertEquals(44.0f, cieLchRot4.toCieLch()[2], 8.0f);
        Color hsvRot4 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate4();
        assertEquals(44.0f, hsvRot4.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot4.toHsv()[2], 0.05f);
        Color hslRot4 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate4();
        assertEquals(44.0f, hslRot4.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot4.toHsl()[2], 0.05f);
        Color oklchRot6 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate6();
        assertEquals(0.7f, oklchRot6.toOklch()[0], 0.05f);
        assertEquals(46.0f, oklchRot6.toOklch()[2], 8.0f);
        Color cieLchRot6 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate6();
        assertEquals(50.0f, cieLchRot6.toCieLch()[0], 1.0f);
        assertEquals(46.0f, cieLchRot6.toCieLch()[2], 8.0f);
        Color hsvRot6 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate6();
        assertEquals(46.0f, hsvRot6.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot6.toHsv()[2], 0.05f);
        Color hslRot6 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate6();
        assertEquals(46.0f, hslRot6.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot6.toHsl()[2], 0.05f);
        Color oklchRot7 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate7();
        assertEquals(0.7f, oklchRot7.toOklch()[0], 0.05f);
        assertEquals(47.0f, oklchRot7.toOklch()[2], 8.0f);
        Color cieLchRot7 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate7();
        assertEquals(50.0f, cieLchRot7.toCieLch()[0], 1.0f);
        assertEquals(47.0f, cieLchRot7.toCieLch()[2], 8.0f);
        Color hsvRot7 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate7();
        assertEquals(47.0f, hsvRot7.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot7.toHsv()[2], 0.05f);
        Color hslRot7 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate7();
        assertEquals(47.0f, hslRot7.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot7.toHsl()[2], 0.05f);
        Color oklchRot8 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate8();
        assertEquals(0.7f, oklchRot8.toOklch()[0], 0.05f);
        assertEquals(48.0f, oklchRot8.toOklch()[2], 8.0f);
        Color cieLchRot8 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate8();
        assertEquals(50.0f, cieLchRot8.toCieLch()[0], 1.0f);
        assertEquals(48.0f, cieLchRot8.toCieLch()[2], 8.0f);
        Color hsvRot8 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate8();
        assertEquals(48.0f, hsvRot8.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot8.toHsv()[2], 0.05f);
        Color hslRot8 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate8();
        assertEquals(48.0f, hslRot8.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot8.toHsl()[2], 0.05f);
        Color oklchRot9 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate9();
        assertEquals(0.7f, oklchRot9.toOklch()[0], 0.05f);
        assertEquals(49.0f, oklchRot9.toOklch()[2], 8.0f);
        Color cieLchRot9 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate9();
        assertEquals(50.0f, cieLchRot9.toCieLch()[0], 1.0f);
        assertEquals(49.0f, cieLchRot9.toCieLch()[2], 8.0f);
        Color hsvRot9 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate9();
        assertEquals(49.0f, hsvRot9.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot9.toHsv()[2], 0.05f);
        Color hslRot9 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate9();
        assertEquals(49.0f, hslRot9.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot9.toHsl()[2], 0.05f);
        Color oklchRot11 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate11();
        assertEquals(0.7f, oklchRot11.toOklch()[0], 0.05f);
        assertEquals(51.0f, oklchRot11.toOklch()[2], 8.0f);
        Color cieLchRot11 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate11();
        assertEquals(50.0f, cieLchRot11.toCieLch()[0], 1.0f);
        assertEquals(51.0f, cieLchRot11.toCieLch()[2], 8.0f);
        Color hsvRot11 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate11();
        assertEquals(51.0f, hsvRot11.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot11.toHsv()[2], 0.05f);
        Color hslRot11 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate11();
        assertEquals(51.0f, hslRot11.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot11.toHsl()[2], 0.05f);
        Color oklchRot12 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate12();
        assertEquals(0.7f, oklchRot12.toOklch()[0], 0.05f);
        assertEquals(52.0f, oklchRot12.toOklch()[2], 8.0f);
        Color cieLchRot12 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate12();
        assertEquals(50.0f, cieLchRot12.toCieLch()[0], 1.0f);
        assertEquals(52.0f, cieLchRot12.toCieLch()[2], 8.0f);
        Color hsvRot12 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate12();
        assertEquals(52.0f, hsvRot12.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot12.toHsv()[2], 0.05f);
        Color hslRot12 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate12();
        assertEquals(52.0f, hslRot12.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot12.toHsl()[2], 0.05f);
        Color oklchRot13 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate13();
        assertEquals(0.7f, oklchRot13.toOklch()[0], 0.05f);
        assertEquals(53.0f, oklchRot13.toOklch()[2], 8.0f);
        Color cieLchRot13 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate13();
        assertEquals(50.0f, cieLchRot13.toCieLch()[0], 1.0f);
        assertEquals(53.0f, cieLchRot13.toCieLch()[2], 8.0f);
        Color hsvRot13 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate13();
        assertEquals(53.0f, hsvRot13.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot13.toHsv()[2], 0.05f);
        Color hslRot13 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate13();
        assertEquals(53.0f, hslRot13.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot13.toHsl()[2], 0.05f);
        Color oklchRot14 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate14();
        assertEquals(0.7f, oklchRot14.toOklch()[0], 0.05f);
        assertEquals(54.0f, oklchRot14.toOklch()[2], 8.0f);
        Color cieLchRot14 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate14();
        assertEquals(50.0f, cieLchRot14.toCieLch()[0], 1.0f);
        assertEquals(54.0f, cieLchRot14.toCieLch()[2], 8.0f);
        Color hsvRot14 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate14();
        assertEquals(54.0f, hsvRot14.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot14.toHsv()[2], 0.05f);
        Color hslRot14 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate14();
        assertEquals(54.0f, hslRot14.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot14.toHsl()[2], 0.05f);
        Color oklchRot16 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate16();
        assertEquals(0.7f, oklchRot16.toOklch()[0], 0.05f);
        assertEquals(56.0f, oklchRot16.toOklch()[2], 8.0f);
        Color cieLchRot16 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate16();
        assertEquals(50.0f, cieLchRot16.toCieLch()[0], 1.0f);
        assertEquals(56.0f, cieLchRot16.toCieLch()[2], 8.0f);
        Color hsvRot16 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate16();
        assertEquals(56.0f, hsvRot16.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot16.toHsv()[2], 0.05f);
        Color hslRot16 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate16();
        assertEquals(56.0f, hslRot16.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot16.toHsl()[2], 0.05f);
        Color oklchRot17 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate17();
        assertEquals(0.7f, oklchRot17.toOklch()[0], 0.05f);
        assertEquals(57.0f, oklchRot17.toOklch()[2], 8.0f);
        Color cieLchRot17 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate17();
        assertEquals(50.0f, cieLchRot17.toCieLch()[0], 1.0f);
        assertEquals(57.0f, cieLchRot17.toCieLch()[2], 8.0f);
        Color hsvRot17 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate17();
        assertEquals(57.0f, hsvRot17.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot17.toHsv()[2], 0.05f);
        Color hslRot17 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate17();
        assertEquals(57.0f, hslRot17.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot17.toHsl()[2], 0.05f);
        Color oklchRot18 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate18();
        assertEquals(0.7f, oklchRot18.toOklch()[0], 0.05f);
        assertEquals(58.0f, oklchRot18.toOklch()[2], 8.0f);
        Color cieLchRot18 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate18();
        assertEquals(50.0f, cieLchRot18.toCieLch()[0], 1.0f);
        assertEquals(58.0f, cieLchRot18.toCieLch()[2], 8.0f);
        Color hsvRot18 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate18();
        assertEquals(58.0f, hsvRot18.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot18.toHsv()[2], 0.05f);
        Color hslRot18 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate18();
        assertEquals(58.0f, hslRot18.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot18.toHsl()[2], 0.05f);
        Color oklchRot19 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate19();
        assertEquals(0.7f, oklchRot19.toOklch()[0], 0.05f);
        assertEquals(59.0f, oklchRot19.toOklch()[2], 8.0f);
        Color cieLchRot19 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate19();
        assertEquals(50.0f, cieLchRot19.toCieLch()[0], 1.0f);
        assertEquals(59.0f, cieLchRot19.toCieLch()[2], 8.0f);
        Color hsvRot19 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate19();
        assertEquals(59.0f, hsvRot19.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot19.toHsv()[2], 0.05f);
        Color hslRot19 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate19();
        assertEquals(59.0f, hslRot19.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot19.toHsl()[2], 0.05f);
        Color oklchRot21 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate21();
        assertEquals(0.7f, oklchRot21.toOklch()[0], 0.05f);
        assertEquals(61.0f, oklchRot21.toOklch()[2], 8.0f);
        Color cieLchRot21 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate21();
        assertEquals(50.0f, cieLchRot21.toCieLch()[0], 1.0f);
        assertEquals(61.0f, cieLchRot21.toCieLch()[2], 8.0f);
        Color hsvRot21 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate21();
        assertEquals(61.0f, hsvRot21.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot21.toHsv()[2], 0.05f);
        Color hslRot21 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate21();
        assertEquals(61.0f, hslRot21.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot21.toHsl()[2], 0.05f);
        Color oklchRot22 = Color.fromOklch(0.7f, 0.06f, 40.0f, 1.0f).oklchRotate22();
        assertEquals(0.7f, oklchRot22.toOklch()[0], 0.05f);
        assertEquals(62.0f, oklchRot22.toOklch()[2], 8.0f);
        Color cieLchRot22 = Color.fromCieLch(50.0f, 12.0f, 40.0f, 1.0f).cieLchRotate22();
        assertEquals(50.0f, cieLchRot22.toCieLch()[0], 1.0f);
        assertEquals(62.0f, cieLchRot22.toCieLch()[2], 8.0f);
        Color hsvRot22 = Color.fromHsv(40.0f, 0.4f, 0.7f, 1.0f).hsvRotate22();
        assertEquals(62.0f, hsvRot22.toHsv()[0], 8.0f);
        assertEquals(0.7f, hsvRot22.toHsv()[2], 0.05f);
        Color hslRot22 = Color.fromHsl(40.0f, 0.4f, 0.7f, 1.0f).hslRotate22();
        assertEquals(62.0f, hslRot22.toHsl()[0], 8.0f);
        assertEquals(0.7f, hslRot22.toHsl()[2], 0.05f);
    }

    /// D65 white has relative luminance `1`.
    @Test
    void relativeLuminanceReportsCieYThroughShippedEntry() {
        assertEquals(1.0f, Color.SRGB_WHITE.relativeLuminance(), 0.001f);
        assertEquals(0.0f, Color.SRGB_BLACK.relativeLuminance(), 0.001f);
        float red = Color.srgb(1.0f, 0.0f, 0.0f, 1.0f).relativeLuminance();
        assertTrue(red > 0.2f && red < 0.25f);
        assertEquals(21.0f, Color.SRGB_WHITE.contrastRatio(Color.SRGB_BLACK), 0.05f);
        assertEquals(1.0f, Color.SRGB_WHITE.contrastRatio(Color.SRGB_WHITE), 0.001f);
    }

    /// `CF_DIB` encode/decode preserves a 1x1 RGBA sample through [`PixelBuffer#toDib()`].
    @Test
    void pixelBufferRoundTripsCfDibThroughShippedEntries() {
        PixelBuffer source = PixelBuffer.srgbUnassociated(1, 1, new byte[] {(byte) 10, (byte) 20, (byte) 30, (byte) 40});
        byte[] dib = source.toDib();
        assertEquals(44, dib.length);
        PixelBuffer restored = PixelBuffer.fromDib(dib);
        assertEquals(1, restored.width());
        assertEquals(1, restored.height());
        assertEquals(10, restored.rgba()[0] & 0xFF);
        assertEquals(20, restored.rgba()[1] & 0xFF);
        assertEquals(30, restored.rgba()[2] & 0xFF);
        assertEquals(40, restored.rgba()[3] & 0xFF);
    }
}

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

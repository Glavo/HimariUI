package org.glavo.himari.graphics;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies bounded ICC v2 matrix/TRC parsing and D50-to-D65 conversion.
@NotNullByDefault
final class IccProfileTest {
    /// A D50-relative sRGB matrix profile with identity TRCs maps white to extended-linear white.
    @Test
    void matrixTrcWhiteMapsToExtendedLinearWhite() {
        IccProfile profile = IccProfile.parse(minimalSrgbMatrixProfile());
        assertEquals(2, (profile.version() >>> 24) & 0xFF);
        assertEquals("RGB ", profile.deviceColorSpace());
        assertEquals("XYZ ", profile.pcs());
        assertEquals(64, profile.sha256().length());
        Color linear = profile.toExtendedLinear(1.0f, 1.0f, 1.0f, 1.0f);
        assertEquals(ColorEncoding.EXTENDED_LINEAR, linear.encoding());
        assertEquals(1.0f, linear.red(), 0.01f);
        assertEquals(1.0f, linear.green(), 0.01f);
        assertEquals(1.0f, linear.blue(), 0.01f);
        assertEquals(null, profile.bToD0());
        assertEquals(null, profile.dToB0());
        assertEquals(null, profile.gamutBoundary());
    }

    /// Uses a `B2D0` `mpet` matrix that doubles the first device channel.
    @Test
    void bToD0MpeDoublesFirstChannelOnParse() {
        IccProfile profile = IccProfile.parse(matrixPlusB2d0());
        assertNotNull(profile.bToD0());
        float[] pcs = profile.bToD0().transform(0.5f, 0.0f, 0.0f);
        assertEquals(1.0f, pcs[0], 0.001f);
        assertEquals(0.0f, pcs[1], 0.001f);
        assertEquals(0.0f, pcs[2], 0.001f);
        Color throughMpe = profile.toExtendedLinear(0.5f, 0.0f, 0.0f, 1.0f);
        Color throughMatrix = IccProfile.parse(minimalSrgbMatrixProfile()).toExtendedLinear(0.5f, 0.0f, 0.0f, 1.0f);
        assertTrue(Math.abs(throughMpe.red() - throughMatrix.red()) > 0.05f);
    }

    /// Uses a `D2B0` `mpet` matrix that halves the first PCS channel.
    @Test
    void dToB0MpeHalvesFirstChannelOnParse() {
        IccProfile profile = IccProfile.parse(matrixPlusD2b0());
        assertNotNull(profile.dToB0());
        float[] device = profile.dToB0().transform(1.0f, 0.0f, 0.0f);
        assertEquals(0.5f, device[0], 0.001f);
        assertEquals(0.0f, device[1], 0.001f);
        assertEquals(0.0f, device[2], 0.001f);
        Color encoded = profile.fromExtendedLinear(Color.xyzD65ToExtended(1.0f, 0.0f, 0.0f, 1.0f));
        Color matrix = IccProfile.parse(minimalSrgbMatrixProfile())
                .fromExtendedLinear(Color.xyzD65ToExtended(1.0f, 0.0f, 0.0f, 1.0f));
        assertTrue(Math.abs(encoded.red() - matrix.red()) > 0.05f);
    }

    /// Reads a single `gbd ` D50 vertex.
    @Test
    void gbdReadsD50Vertex() {
        IccProfile profile = IccProfile.parse(matrixPlusGbd());
        assertNotNull(profile.gamutBoundary());
        assertEquals(1, profile.gamutBoundary().vertexCount());
        assertEquals(0.9642f, profile.gamutBoundary().x(0), 0.001f);
        assertEquals(1.0f, profile.gamutBoundary().y(0), 0.001f);
        assertEquals(0.8249f, profile.gamutBoundary().z(0), 0.001f);
    }

    /// Uses `B2D3` when earlier BToD tags are absent.
    @Test
    void bToD3UsedWhenEarlierBToDAbsent() {
        IccProfile profile = IccProfile.parse(matrixPlusMpe(0x4232_4433, false, 2.0f));
        assertNotNull(profile.bToD0());
        float[] pcs = profile.bToD0().transform(0.5f, 0.0f, 0.0f);
        assertEquals(1.0f, pcs[0], 0.001f);
        Color throughMpe = profile.toExtendedLinear(0.5f, 0.0f, 0.0f, 1.0f);
        Color throughMatrix = IccProfile.parse(minimalSrgbMatrixProfile()).toExtendedLinear(0.5f, 0.0f, 0.0f, 1.0f);
        assertTrue(Math.abs(throughMpe.red() - throughMatrix.red()) > 0.05f);
    }

    /// Uses `D2B3` when earlier DToB tags are absent.
    @Test
    void dToB3UsedWhenEarlierDToBAbsent() {
        IccProfile profile = IccProfile.parse(matrixPlusMpe(0x4432_4233, true, 0.5f));
        assertNotNull(profile.dToB0());
        float[] device = profile.dToB0().transform(1.0f, 0.0f, 0.0f);
        assertEquals(0.5f, device[0], 0.001f);
        Color encoded = profile.fromExtendedLinear(Color.xyzD65ToExtended(1.0f, 0.0f, 0.0f, 1.0f));
        Color matrix = IccProfile.parse(minimalSrgbMatrixProfile())
                .fromExtendedLinear(Color.xyzD65ToExtended(1.0f, 0.0f, 0.0f, 1.0f));
        assertTrue(Math.abs(encoded.red() - matrix.red()) > 0.05f);
    }

    /// Looks up a unique `mpet` `clut` cell on `B2D0`.
    @Test
    void bToD0ClutLooksUpUniqueCell() {
        IccProfile profile = IccProfile.parse(matrixPlusB2d0Clut());
        assertNotNull(profile.bToD0());
        assertEquals(2, profile.bToD0().clutGrid());
        float[] pcs = profile.bToD0().transform(1.0f, 0.0f, 0.0f);
        assertEquals(0.0f, pcs[0], 0.001f);
        assertEquals(1.0f, pcs[1], 0.001f);
        assertEquals(0.0f, pcs[2], 0.001f);
        Color throughMpe = profile.toExtendedLinear(1.0f, 0.0f, 0.0f, 1.0f);
        Color throughMatrix = IccProfile.parse(minimalSrgbMatrixProfile()).toExtendedLinear(1.0f, 0.0f, 0.0f, 1.0f);
        assertTrue(Math.abs(throughMpe.green() - throughMatrix.green()) > 0.05f);
    }

    /// Applies a `samf` 1D table that halves the first channel on `B2D0`.
    @Test
    void bToD0SamfHalvesFirstChannel() {
        IccProfile profile = IccProfile.parse(matrixPlusB2d0Samf());
        assertNotNull(profile.bToD0());
        assertEquals(6, profile.bToD0().firstSampled().length);
        float[] pcs = profile.bToD0().transform(1.0f, 0.0f, 0.0f);
        assertEquals(0.5f, pcs[0], 0.001f);
        assertEquals(0.0f, pcs[1], 0.001f);
        assertEquals(0.0f, pcs[2], 0.001f);
        Color throughMpe = profile.toExtendedLinear(1.0f, 0.0f, 0.0f, 1.0f);
        Color throughMatrix = IccProfile.parse(minimalSrgbMatrixProfile()).toExtendedLinear(1.0f, 0.0f, 0.0f, 1.0f);
        assertTrue(Math.abs(throughMpe.red() - throughMatrix.red()) > 0.05f);
    }

    /// Applies type-1 `curf` `Y=aX` that doubles the first channel on `B2D0`.
    @Test
    void bToD0CurfType1DoublesFirstChannel() {
        IccProfile profile = IccProfile.parse(matrixPlusB2d0CurfType1());
        assertNotNull(profile.bToD0());
        assertEquals(2.0f, profile.bToD0().firstScale()[0], 0.001f);
        float[] pcs = profile.bToD0().transform(0.5f, 0.0f, 0.0f);
        assertEquals(1.0f, pcs[0], 0.001f);
        assertEquals(0.0f, pcs[1], 0.001f);
        assertEquals(0.0f, pcs[2], 0.001f);
        Color throughMpe = profile.toExtendedLinear(0.5f, 0.0f, 0.0f, 1.0f);
        Color throughMatrix = IccProfile.parse(minimalSrgbMatrixProfile()).toExtendedLinear(0.5f, 0.0f, 0.0f, 1.0f);
        assertTrue(Math.abs(throughMpe.red() - throughMatrix.red()) > 0.05f);
    }

    /// Applies trailing type-1 `curf` `Y=aX` after an identity `matf` on `B2D0`.
    @Test
    void bToD0CurfType1AfterMatrixDoublesFirstChannel() {
        IccProfile profile = IccProfile.parse(matrixPlusB2d0CurfType1AfterMatrix());
        assertNotNull(profile.bToD0());
        assertEquals(2.0f, profile.bToD0().lastScale()[0], 0.001f);
        float[] pcs = profile.bToD0().transform(0.5f, 0.0f, 0.0f);
        assertEquals(1.0f, pcs[0], 0.001f);
        assertEquals(0.0f, pcs[1], 0.001f);
        assertEquals(0.0f, pcs[2], 0.001f);
        Color throughMpe = profile.toExtendedLinear(0.5f, 0.0f, 0.0f, 1.0f);
        Color throughMatrix = IccProfile.parse(minimalSrgbMatrixProfile()).toExtendedLinear(0.5f, 0.0f, 0.0f, 1.0f);
        assertTrue(Math.abs(throughMpe.red() - throughMatrix.red()) > 0.05f);
    }

    /// Looks up a D50-white `ncl2` named color and adapts it to extended-linear white.
    @Test
    void namedColorLooksUpAdaptedPcsWhite() {
        IccProfile profile = IccProfile.parse(matrixWithNamedColor());
        assertNotNull(profile.namedColors());
        assertEquals("Ink", profile.namedColors().prefix());
        IccNamedColors.Entry entry = profile.namedColors().lookup("SpotRed");
        assertNotNull(entry);
        assertEquals(0.9642f, entry.pcsX(), 0.001f);
        assertEquals(1.0f, entry.pcsY(), 0.001f);
        assertEquals(0.8249f, entry.pcsZ(), 0.001f);
        Color linear = profile.namedColorToExtendedLinear("SpotRed", 1.0f);
        assertNotNull(linear);
        assertEquals(ColorEncoding.EXTENDED_LINEAR, linear.encoding());
        assertEquals(1.0f, linear.red(), 0.02f);
        assertEquals(1.0f, linear.green(), 0.02f);
        assertEquals(1.0f, linear.blue(), 0.02f);
        assertEquals(null, profile.namedColorToExtendedLinear("Missing", 1.0f));
        assertEquals(null, IccProfile.parse(minimalSrgbMatrixProfile()).namedColors());
    }

    /// Looks up a v2 `ncl ` named color with empty prefix and suffix.
    @Test
    void namedColorTypeLooksUpPcsWhite() {
        IccProfile profile = IccProfile.parse(matrixWithNamedColorV2());
        assertNotNull(profile.namedColors());
        assertEquals("", profile.namedColors().prefix());
        IccNamedColors.Entry entry = profile.namedColors().lookup("Patch");
        assertNotNull(entry);
        Color linear = profile.namedColorToExtendedLinear("Patch", 1.0f);
        assertNotNull(linear);
        assertEquals(1.0f, linear.red(), 0.02f);
        assertEquals(1.0f, linear.green(), 0.02f);
        assertEquals(1.0f, linear.blue(), 0.02f);
    }

    /// Reads three `ncl2` device coordinates after the PCS triple.
    @Test
    void namedColorReadsDeviceCoordinates() {
        IccProfile profile = IccProfile.parse(matrixWithNamedColorDevice());
        IccNamedColors.Entry entry = profile.namedColors().lookup("SpotRed");
        assertNotNull(entry);
        assertEquals(3, entry.device().length);
        assertEquals(1.0f, entry.device()[0], 0.001f);
        assertEquals(0.0f, entry.device()[1], 0.001f);
        assertEquals(0.5f, entry.device()[2], 0.001f);
        Color device = profile.namedColorDeviceRgb("SpotRed", 1.0f);
        assertNotNull(device);
        assertEquals(ColorEncoding.SRGB, device.encoding());
        assertEquals(1.0f, device.red(), 0.001f);
        assertEquals(0.0f, device.green(), 0.001f);
        assertEquals(0.5f, device.blue(), 0.001f);
        assertEquals(null, profile.namedColorDeviceRgb("Missing", 1.0f));
        assertEquals(null, IccProfile.parse(matrixWithNamedColor()).namedColorDeviceRgb("SpotRed", 1.0f));
    }

    /// Applies a `chad` matrix that halves PCS X before D50-to-D65 adaptation.
    @Test
    void chadMatrixScalesPcsXBeforeAdaptation() {
        Color baseline = IccProfile.parse(minimalSrgbMatrixProfile()).toExtendedLinear(1.0f, 1.0f, 1.0f, 1.0f);
        IccProfile profile = IccProfile.parse(matrixWithChadScaleX(0.5f));
        assertNotNull(profile.chromaticAdaptation());
        assertEquals(0.5f, profile.chromaticAdaptation()[0], 0.001f);
        Color scaled = profile.toExtendedLinear(1.0f, 1.0f, 1.0f, 1.0f);
        assertTrue(scaled.red() < baseline.red() - 0.05f);
        Color roundTrip = profile.fromExtendedLinear(scaled);
        assertEquals(1.0f, roundTrip.red(), 0.05f);
        assertEquals(1.0f, roundTrip.green(), 0.05f);
        assertEquals(1.0f, roundTrip.blue(), 0.05f);
    }

    /// Uses `wtpt` D65 instead of the D50 header illuminant for Bradford adaptation.
    @Test
    void mediaWhitePointSkipsBradfordWhenD65() {
        Color baseline = IccProfile.parse(minimalSrgbMatrixProfile()).toExtendedLinear(1.0f, 1.0f, 1.0f, 1.0f);
        IccProfile profile = IccProfile.parse(matrixWithMediaWhiteD65());
        assertNotNull(profile.mediaWhite());
        assertEquals(0.95047f, profile.mediaWhite()[0], 0.001f);
        Color adapted = profile.toExtendedLinear(1.0f, 1.0f, 1.0f, 1.0f);
        assertTrue(Math.abs(adapted.red() - baseline.red()) > 0.01f
                || Math.abs(adapted.green() - baseline.green()) > 0.01f
                || Math.abs(adapted.blue() - baseline.blue()) > 0.01f);
    }

    /// Subtracts `bkpt` from PCS XYZ before D50-to-D65 adaptation.
    @Test
    void mediaBlackPointLowersAdaptedWhite() {
        Color baseline = IccProfile.parse(minimalSrgbMatrixProfile()).toExtendedLinear(1.0f, 1.0f, 1.0f, 1.0f);
        IccProfile profile = IccProfile.parse(matrixWithMediaBlack());
        assertNotNull(profile.mediaBlack());
        assertEquals(0.05f, profile.mediaBlack()[1], 0.001f);
        Color adapted = profile.toExtendedLinear(1.0f, 1.0f, 1.0f, 1.0f);
        assertTrue(adapted.green() < baseline.green() - 0.02f);
    }

    /// Reads `lumi` Y as candelas per square metre.
    @Test
    void luminanceTagReportsNits() {
        IccProfile profile = IccProfile.parse(matrixWithLuminance(80.0f));
        assertNotNull(profile.luminance());
        assertEquals(80.0f, profile.luminanceNits(), 0.01f);
        assertEquals(0.0f, IccProfile.parse(minimalSrgbMatrixProfile()).luminanceNits());
    }

    /// Reads `view` illuminant type D65 and the absolute illuminant Y.
    @Test
    void viewingConditionsTagReadsD65Illuminant() {
        IccProfile profile = IccProfile.parse(matrixWithViewingConditions());
        IccViewingConditions view = profile.viewingConditions();
        assertNotNull(view);
        assertTrue(view.d65());
        assertEquals(IccViewingConditions.ILLUMINANT_D65, view.illuminantType());
        assertEquals(80.0f, view.illuminantY(), 0.01f);
        assertEquals(16.0f, view.surroundY(), 0.01f);
        assertEquals(80.0f, profile.viewingIlluminantNits(), 0.01f);
        assertEquals("sRGB", profile.description());
        assertEquals("D65", profile.viewingDescription());
        assertEquals("ICC", profile.copyright());
        assertEquals("CRT ", profile.technology());
        assertEquals("IEC", profile.deviceManufacturer());
        assertEquals("sRGB", profile.deviceModel());
        IccDateTime calibrated = profile.calibrationDate();
        assertNotNull(calibrated);
        assertEquals(2026, calibrated.year());
        assertEquals(8, calibrated.month());
        assertEquals(20, calibrated.day());
        IccMeasurement measurement = profile.measurement();
        assertNotNull(measurement);
        assertEquals(IccMeasurement.OBSERVER_CIE_1931, measurement.observer());
        assertEquals(IccMeasurement.GEOMETRY_0_45, measurement.geometry());
        assertEquals(IccViewingConditions.ILLUMINANT_D65, measurement.illuminant());
        assertEquals(0.0f, measurement.flare(), 0.001f);
        assertEquals(null, IccProfile.parse(minimalSrgbMatrixProfile()).viewingConditions());
        assertEquals(0.0f, IccProfile.parse(minimalSrgbMatrixProfile()).viewingIlluminantNits());
    }

    /// Marks the unique `gamt` `mft1` cell as out of gamut and leaves the origin in gamut.
    @Test
    void gamutTagMarksUniquePcsCellOutOfGamut() {
        IccProfile baseline = IccProfile.parse(minimalSrgbMatrixProfile());
        assertEquals(null, baseline.gamut());
        assertTrue(baseline.inGamut(1.0f, 0.0f, 0.0f));
        assertEquals(0.0f, baseline.gamutAlarm(1.0f, 0.0f, 0.0f), 0.0f);
        IccProfile profile = IccProfile.parse(matrixPlusGamtMft1());
        assertNotNull(profile.gamut());
        assertEquals(2, profile.gamut().grid());
        assertTrue(profile.inGamut(0.0f, 0.0f, 0.0f));
        assertEquals(0.0f, profile.gamutAlarm(0.0f, 0.0f, 0.0f), 0.001f);
        assertFalse(profile.inGamut(1.0f, 0.0f, 0.0f));
        assertEquals(1.0f, profile.gamutAlarm(1.0f, 0.0f, 0.0f), 0.001f);
    }

    /// Marks the unique `gamt` `mft2` cell as out of gamut.
    @Test
    void gamutTag16BitMarksUniquePcsCellOutOfGamut() {
        IccProfile profile = IccProfile.parse(matrixPlusGamtMft2());
        assertNotNull(profile.gamut());
        assertTrue(profile.inGamut(0.0f, 0.0f, 0.0f));
        assertFalse(profile.inGamut(1.0f, 0.0f, 0.0f));
        assertEquals(1.0f, profile.gamutAlarm(1.0f, 0.0f, 0.0f), 0.001f);
    }

    /// Marks the unique `gamt` `mBA ` cell as out of gamut.
    @Test
    void gamutTagMbaMarksUniquePcsCellOutOfGamut() {
        IccProfile profile = IccProfile.parse(matrixPlusGamtMba());
        assertNotNull(profile.gamut());
        assertTrue(profile.gamut().inverse());
        assertTrue(profile.inGamut(0.0f, 0.0f, 0.0f));
        assertFalse(profile.inGamut(1.0f, 0.0f, 0.0f));
        assertEquals(1.0f, profile.gamutAlarm(1.0f, 0.0f, 0.0f), 0.001f);
    }

    /// Doubles PCS X through the `gamt` `mft1` matrix so half-X hits the unique cell.
    @Test
    void gamutTagMatrixLooksUpUniqueCellFromHalf() {
        IccProfile identity = IccProfile.parse(matrixPlusGamtMft1());
        IccProfile scaled = IccProfile.parse(matrixPlusGamtMft1Matrix());
        assertEquals(0.5f, identity.gamutAlarm(0.5f, 0.0f, 0.0f), 0.001f);
        assertEquals(1.0f, scaled.gamutAlarm(0.5f, 0.0f, 0.0f), 0.001f);
        assertFalse(scaled.inGamut(0.5f, 0.0f, 0.0f));
    }

    /// Rejects a 3×3 LUT stored under the `gamt` signature.
    @Test
    void gamutTagRejectsThreeChannelOutput() {
        assertThrows(IllegalArgumentException.class, () -> IccProfile.parse(matrixPlusInvalidThreeChannelGamt()));
    }

    /// Reads `cicp` BT.709 / sRGB full-range code points.
    @Test
    void cicpTagReadsBt709SrgbCodes() {
        IccProfile profile = IccProfile.parse(matrixWithCicp());
        IccCicp cicp = profile.cicp();
        assertNotNull(cicp);
        assertEquals(IccCicp.PRIMARIES_BT709, cicp.colorPrimaries());
        assertEquals(IccCicp.TRANSFER_SRGB, cicp.transferCharacteristics());
        assertEquals(IccCicp.MATRIX_RGB, cicp.matrixCoefficients());
        assertTrue(cicp.fullRange());
        assertEquals(null, IccProfile.parse(minimalSrgbMatrixProfile()).cicp());
    }

    /// Uses the `pre0` preview LUT instead of leaving the optional table null.
    @Test
    void preview0ClutLooksUpUniqueCell() {
        IccProfile profile = IccProfile.parse(matrixPlusPreview0());
        assertNotNull(profile.preview0());
        assertEquals(null, profile.preview1());
        assertEquals(null, profile.preview2());
        float[] xyz = profile.preview0().transform(1.0f, 0.0f, 0.0f);
        assertEquals(51 / 255.0f, xyz[0], 0.001f);
        assertEquals(0.0f, xyz[1], 0.001f);
        assertEquals(0.0f, xyz[2], 0.001f);
    }

    /// Reads a Status A `resp` curve for one RGB channel.
    @Test
    void outputResponseTagReadsStatusASample() {
        IccProfile profile = IccProfile.parse(matrixWithOutputResponse());
        IccOutputResponse response = profile.outputResponse();
        assertNotNull(response);
        assertEquals(3, response.channelCount());
        assertEquals(1, response.curveSets().size());
        IccOutputResponse.CurveSet curveSet = response.curveSets().get(0);
        assertEquals(IccOutputResponse.UNIT_STATUS_A, curveSet.measurementType());
        IccOutputResponse.Channel red = curveSet.channels()[0];
        assertEquals(0.4360657f, red.maxX(), 0.001f);
        assertEquals(2, red.measurements().length);
        assertEquals(0.0f, red.measurements()[0], 0.001f);
        assertEquals(1.0f, red.measurements()[1], 0.001f);
        assertEquals(0.0f, red.devices()[0], 0.001f);
        assertEquals(1.0f, red.devices()[1], 0.001f);
        assertEquals(null, IccProfile.parse(minimalSrgbMatrixProfile()).outputResponse());
    }

    /// Looks up a `clrt` colorant and a distinct `clot` output colorant.
    @Test
    void colorantTablesLookupPcsValues() {
        IccProfile profile = IccProfile.parse(matrixWithColorants());
        assertNotNull(profile.colorants());
        IccColorants.Entry cyan = profile.colorants().lookup("Cyan");
        assertNotNull(cyan);
        assertEquals(0.1f, cyan.pcsX(), 0.001f);
        assertEquals(0.2f, cyan.pcsY(), 0.001f);
        assertEquals(0.3f, cyan.pcsZ(), 0.001f);
        assertNotNull(profile.colorantsOut());
        IccColorants.Entry paper = profile.colorantsOut().lookup("Paper");
        assertNotNull(paper);
        assertEquals(0.9f, paper.pcsX(), 0.001f);
        assertEquals(1.0f, paper.pcsY(), 0.001f);
        assertEquals(0.8f, paper.pcsZ(), 0.001f);
        assertEquals(null, profile.colorants().lookup("Paper"));
        assertEquals(null, IccProfile.parse(minimalSrgbMatrixProfile()).colorants());
    }

    /// Reads one `pseq` record with ASCII manufacturer and model descriptions.
    @Test
    void profileSequenceTagReadsManufacturerAndModel() {
        IccProfile profile = IccProfile.parse(matrixWithProfileSequence());
        IccProfileSequence sequence = profile.profileSequence();
        assertNotNull(sequence);
        assertEquals(1, sequence.entries().size());
        IccProfileSequence.Entry entry = sequence.entries().get(0);
        assertEquals("IEC ", entry.manufacturer());
        assertEquals("sRGB", entry.model());
        assertEquals("CRT ", entry.technology());
        assertEquals("IEC", entry.manufacturerDescription());
        assertEquals("sRGB", entry.modelDescription());
        assertEquals(null, IccProfile.parse(minimalSrgbMatrixProfile()).profileSequence());
    }

    /// Reads one `psid` record with a distinctive 16-byte profile ID.
    @Test
    void profileSequenceIdsTagReadsProfileIdAndDescription() {
        IccProfile profile = IccProfile.parse(matrixWithProfileSequenceIds());
        IccProfileSequenceIds ids = profile.profileSequenceIds();
        assertNotNull(ids);
        assertEquals(1, ids.entries().size());
        IccProfileSequenceIds.Entry entry = ids.entries().get(0);
        assertEquals("000102030405060708090a0b0c0d0e0f", entry.profileIdHex());
        assertEquals("sRGB", entry.description());
        assertEquals(null, IccProfile.parse(minimalSrgbMatrixProfile()).profileSequenceIds());
    }

    /// Looks up a `meta` dictionary entry and its `mluc` display name.
    @Test
    void metadataTagLooksUpUtf16NameAndDisplayName() {
        IccProfile profile = IccProfile.parse(matrixWithMetadata());
        IccMetadata metadata = profile.metadata();
        assertNotNull(metadata);
        IccMetadata.Entry entry = metadata.lookup("Pref");
        assertNotNull(entry);
        assertEquals("sRGB", entry.value());
        assertEquals("sRGB Profile", entry.displayName());
        assertEquals(null, metadata.lookup("Missing"));
        assertEquals(null, IccProfile.parse(minimalSrgbMatrixProfile()).metadata());
    }

    /// Reads BT.709 `chrm` xy chromaticities for three RGB channels.
    @Test
    void chromaticityTagReadsBt709Primaries() {
        IccProfile profile = IccProfile.parse(matrixWithChromaticity());
        IccChromaticity chromaticity = profile.chromaticity();
        assertNotNull(chromaticity);
        assertTrue(chromaticity.bt709());
        assertEquals(3, chromaticity.channelCount());
        assertEquals(0.64f, chromaticity.x()[0], 0.001f);
        assertEquals(0.33f, chromaticity.y()[0], 0.001f);
        assertEquals(0.30f, chromaticity.x()[1], 0.001f);
        assertEquals(0.60f, chromaticity.y()[1], 0.001f);
        assertEquals(0.15f, chromaticity.x()[2], 0.001f);
        assertEquals(0.06f, chromaticity.y()[2], 0.001f);
        assertEquals(null, IccProfile.parse(minimalSrgbMatrixProfile()).chromaticity());
    }

    /// Reads a `clro` layout order that permutes three colorants.
    @Test
    void colorantOrderTagReadsOneBasedIndices() {
        IccProfile profile = IccProfile.parse(matrixWithColorantOrder());
        IccColorantOrder order = profile.colorantOrder();
        assertNotNull(order);
        assertEquals(3, order.indices().length);
        assertEquals(3, order.indices()[0]);
        assertEquals(1, order.indices()[1]);
        assertEquals(2, order.indices()[2]);
        assertEquals(null, IccProfile.parse(minimalSrgbMatrixProfile()).colorantOrder());
    }

    /// Reads a v4 `mluc` profile description from the `desc` tag.
    @Test
    void descriptionTagReadsMlucRecord() {
        IccProfile profile = IccProfile.parse(matrixWithMlucDescription());
        assertEquals("v4sRGB", profile.description());
        assertEquals(null, IccProfile.parse(minimalSrgbMatrixProfile()).description());
    }

    /// Reads a frequency-encoded `scrn` channel with a round spot function.
    @Test
    void screeningTagReadsFrequencyAndRoundSpot() {
        IccProfile profile = IccProfile.parse(matrixWithScreening());
        IccScreening screening = profile.screening();
        assertNotNull(screening);
        assertTrue(screening.frequencyEncoded());
        assertEquals(1, screening.channels().length);
        IccScreening.Channel channel = screening.channels()[0];
        assertEquals(150.0f, channel.frequency(), 0.01f);
        assertEquals(45.0f, channel.angle(), 0.01f);
        assertEquals(IccScreening.SPOT_ROUND, channel.spotFunction());
        assertEquals(null, IccProfile.parse(minimalSrgbMatrixProfile()).screening());
    }

    /// Reads `bfd ` UCR/BG gamma curves and the trailing ASCII description.
    @Test
    void ucrBgTagReadsGammaCurvesAndDescription() {
        IccProfile profile = IccProfile.parse(matrixWithUcrBg());
        IccUcrBg ucrBg = profile.ucrBg();
        assertNotNull(ucrBg);
        assertEquals(2.0f, ucrBg.ucr().gamma(), 0.001f);
        assertEquals(0.25f, ucrBg.ucr().decode(0.5f), 0.001f);
        assertEquals(1.0f, ucrBg.bg().gamma(), 0.001f);
        assertEquals(0.5f, ucrBg.bg().decode(0.5f), 0.001f);
        assertEquals("UCR", ucrBg.description());
        assertEquals(null, IccProfile.parse(minimalSrgbMatrixProfile()).ucrBg());
    }

    /// Reads a `targ` characterization-target name.
    @Test
    void characterizationTargetTagReadsIt8Name() {
        IccProfile profile = IccProfile.parse(matrixWithCharacterizationTarget());
        assertEquals("IT8.7/2", profile.characterizationTarget());
        assertEquals(null, IccProfile.parse(minimalSrgbMatrixProfile()).characterizationTarget());
    }

    /// Reads an ASCII `crdi` color-rendering dictionary payload.
    @Test
    void colorRenderingDictTagReadsAsciiData() {
        IccProfile profile = IccProfile.parse(matrixWithColorRenderingDict());
        IccData dict = profile.colorRenderingDict();
        assertNotNull(dict);
        assertEquals(false, dict.binary());
        assertEquals("CRD", dict.ascii());
        assertEquals(null, IccProfile.parse(minimalSrgbMatrixProfile()).colorRenderingDict());
    }

    /// Reads ASCII `ps2s` and `ps2i` PostScript payloads.
    @Test
    void postScript2DataTagsReadAsciiPayloads() {
        IccProfile profile = IccProfile.parse(matrixWithPostScript2Data());
        assertNotNull(profile.postScript2Csa());
        assertEquals("CSA", profile.postScript2Csa().ascii());
        assertNotNull(profile.postScript2Crd());
        assertEquals("CRD2", profile.postScript2Crd().ascii());
        assertEquals(null, IccProfile.parse(minimalSrgbMatrixProfile()).postScript2Csa());
    }

    /// Reads `psd0` through `psd3` PostScript descriptions.
    @Test
    void postScriptDescTagsReadIntentLabels() {
        IccProfile profile = IccProfile.parse(matrixWithPostScriptDescs());
        assertEquals("Perceptual", profile.postScriptDesc0());
        assertEquals("Relative", profile.postScriptDesc1());
        assertEquals("Saturation", profile.postScriptDesc2());
        assertEquals("Absolute", profile.postScriptDesc3());
        assertEquals(null, IccProfile.parse(minimalSrgbMatrixProfile()).postScriptDesc0());
    }

    /// Reads a Microsoft `devs` combination with glossy media and 300×600 dpi.
    @Test
    void deviceSettingsTagReadsMsftResolutionAndMedia() {
        IccProfile profile = IccProfile.parse(matrixWithDeviceSettings());
        IccDeviceSettings settings = profile.deviceSettings();
        assertNotNull(settings);
        IccDeviceSettings.Platform msft = settings.platform(IccDeviceSettings.PLATFORM_MSFT);
        assertNotNull(msft);
        assertEquals(1, msft.combinations().size());
        IccDeviceSettings.Combination combination = msft.combinations().get(0);
        IccDeviceSettings.Setting resolution = combination.setting(IccDeviceSettings.SETTING_RESOLUTION);
        assertNotNull(resolution);
        assertEquals(300, resolution.resolutionX());
        assertEquals(600, resolution.resolutionY());
        IccDeviceSettings.Setting media = combination.setting(IccDeviceSettings.SETTING_MEDIA);
        assertNotNull(media);
        assertEquals(IccDeviceSettings.MEDIA_GLOSSY, media.firstU32());
        assertEquals(null, IccProfile.parse(minimalSrgbMatrixProfile()).deviceSettings());
    }

    /// Reads a `crdInfoType` product name and the perceptual companion CRD.
    @Test
    void crdInfoTypeReadsProductAndPerceptualCompanion() {
        IccProfile profile = IccProfile.parse(matrixWithCrdInfo());
        IccCrdInfo info = profile.crdInfo();
        assertNotNull(info);
        assertEquals("Himari", info.productName());
        assertEquals("PerceptualCRD", info.crdNames()[0]);
        assertEquals(null, info.crdNames()[1]);
        assertEquals(null, IccProfile.parse(minimalSrgbMatrixProfile()).crdInfo());
        assertEquals(null, IccProfile.parse(matrixWithColorRenderingDict()).crdInfo());
    }

    /// Reads a `scrd` screening description.
    @Test
    void screeningDescriptionTagReadsAsciiName() {
        IccProfile profile = IccProfile.parse(matrixWithScreeningDescription());
        assertEquals("Default screens", profile.screeningDescription());
        assertEquals(null, IccProfile.parse(minimalSrgbMatrixProfile()).screeningDescription());
    }

    /// Maps GRAY identity `kTRC` white through the media white point.
    @Test
    void grayMatrixWhiteMapsToExtendedLinearWhite() {
        IccProfile profile = IccProfile.parse(minimalGrayMatrixProfile());
        assertEquals("GRAY", profile.deviceColorSpace());
        Color linear = profile.toExtendedLinear(1.0f, 1.0f, 1.0f, 1.0f);
        assertEquals(1.0f, linear.red(), 0.02f);
        assertEquals(1.0f, linear.green(), 0.02f);
        assertEquals(1.0f, linear.blue(), 0.02f);
        Color mid = profile.toExtendedLinear(0.5f, 0.5f, 0.5f, 1.0f);
        assertTrue(mid.green() < linear.green() - 0.2f);
    }

    /// Accepts a Lab PCS header on a matrix/TRC profile and still maps encoded white.
    @Test
    void labPcsMatrixWhiteMapsToExtendedLinearWhite() {
        IccProfile profile = IccProfile.parse(matrixWithLabPcs());
        assertTrue(profile.labPcs());
        assertEquals("Lab ", profile.pcs());
        Color linear = profile.toExtendedLinear(1.0f, 1.0f, 1.0f, 1.0f);
        assertEquals(1.0f, linear.red(), 0.02f);
        assertEquals(1.0f, linear.green(), 0.02f);
        assertEquals(1.0f, linear.blue(), 0.02f);
    }

    /// Decodes an 8-bit Lab AToB0 unique cell of D50 white.
    @Test
    void labPcsClutDecodesD50White() {
        IccProfile xyz = IccProfile.parse(matrixPlusLabA2b0Mft1(false));
        IccProfile lab = IccProfile.parse(matrixPlusLabA2b0Mft1(true));
        assertFalse(xyz.labPcs());
        assertTrue(lab.labPcs());
        Color xyzColor = xyz.toExtendedLinear(1.0f, 0.0f, 0.0f, 1.0f);
        Color labColor = lab.toExtendedLinear(1.0f, 0.0f, 0.0f, 1.0f);
        assertTrue(Math.abs(xyzColor.red() - labColor.red()) > 0.2f
                || Math.abs(xyzColor.green() - labColor.green()) > 0.2f
                || Math.abs(xyzColor.blue() - labColor.blue()) > 0.2f);
        assertEquals(1.0f, labColor.red(), 0.05f);
        assertEquals(1.0f, labColor.green(), 0.05f);
        assertEquals(1.0f, labColor.blue(), 0.05f);
    }

    /// Looks up a Lab PCS `ncl2` named color encoded as 16-bit ICC L*a*b* white.
    @Test
    void labPcsNamedColorLooksUpAdaptedWhite() {
        IccProfile profile = IccProfile.parse(matrixWithLabNamedColor());
        assertTrue(profile.labPcs());
        Color linear = profile.namedColorToExtendedLinear("SpotRed", 1.0f);
        assertNotNull(linear);
        assertEquals(1.0f, linear.red(), 0.05f);
        assertEquals(1.0f, linear.green(), 0.05f);
        assertEquals(1.0f, linear.blue(), 0.05f);
    }

    /// Decodes a 16-bit Lab AToB0 unique cell of D50 white.
    @Test
    void labPcs16BitClutDecodesD50White() {
        IccProfile profile = IccProfile.parse(matrixPlusLabA2b0Mft2());
        assertTrue(profile.labPcs());
        Color linear = profile.toExtendedLinear(1.0f, 0.0f, 0.0f, 1.0f);
        assertEquals(1.0f, linear.red(), 0.05f);
        assertEquals(1.0f, linear.green(), 0.05f);
        assertEquals(1.0f, linear.blue(), 0.05f);
    }

    /// Inverts Lab PCS white through a BToA0 unique red plane.
    @Test
    void labPcsBToA0LooksUpWhiteAsUniqueRed() {
        IccProfile profile = IccProfile.parse(matrixPlusLabB2a0());
        assertTrue(profile.labPcs());
        Color device = profile.fromExtendedLinear(Color.extendedLinear(1.0f, 1.0f, 1.0f, 1.0f));
        assertEquals(1.0f, device.red(), 0.05f);
        assertEquals(0.0f, device.green(), 0.05f);
        assertEquals(0.0f, device.blue(), 0.05f);
    }

    /// Parses a CMYK `mft1` AToB0 unique cyan cell through [`IccProfile#parse(byte[])`].
    @Test
    void cmykMft1AToB0LooksUpUniqueCyanCell() {
        IccProfile profile = IccProfile.parse(cmykMft1A2b0());
        assertEquals("CMYK", profile.deviceColorSpace());
        assertNotNull(profile.cmyk());
        assertEquals(null, profile.clut());
        assertEquals(2, profile.cmyk().grid());
        assertEquals(false, profile.cmyk().inverse());
        float[] pcs = profile.cmyk().transform(1.0f, 0.0f, 0.0f, 0.0f);
        assertEquals(51 / 255.0f, pcs[0], 0.001f);
        assertEquals(0.0f, pcs[1], 0.001f);
        assertEquals(0.0f, pcs[2], 0.001f);
        Color linear = profile.toExtendedLinear(1.0f, 0.0f, 0.0f, 1.0f);
        assertEquals(ColorEncoding.EXTENDED_LINEAR, linear.encoding());
        Color zero = profile.toExtendedLinearCmyk(0.0f, 0.0f, 0.0f, 0.0f, 1.0f);
        assertTrue(linear.red() > zero.red() + 0.05f);
    }

    /// Uses the black axis of a parsed CMYK AToB0 table, not only C/M/Y with K=`0`.
    @Test
    void cmykMft1AToB0UsesBlackChannel() {
        IccProfile profile = IccProfile.parse(cmykMft1A2b0());
        float[] cyan = profile.cmyk().transform(1.0f, 0.0f, 0.0f, 0.0f);
        float[] black = profile.cmyk().transform(0.0f, 0.0f, 0.0f, 1.0f);
        assertEquals(51 / 255.0f, cyan[0], 0.001f);
        assertEquals(0.0f, cyan[1], 0.001f);
        assertEquals(0.0f, black[0], 0.001f);
        assertEquals(51 / 255.0f, black[1], 0.001f);
        Color cyanColor = profile.toExtendedLinearCmyk(1.0f, 0.0f, 0.0f, 0.0f, 1.0f);
        Color blackColor = profile.toExtendedLinearCmyk(0.0f, 0.0f, 0.0f, 1.0f, 1.0f);
        assertTrue(Math.abs(cyanColor.red() - blackColor.red()) > 0.05f
                || Math.abs(cyanColor.green() - blackColor.green()) > 0.05f);
    }

    /// Parses a compact CMYK `mft2` AToB0 table with two-entry identity curves.
    @Test
    void cmykMft2AToB0LooksUpUniqueCyanCell() {
        IccProfile profile = IccProfile.parse(cmykMft2A2b0());
        assertNotNull(profile.cmyk());
        assertEquals(2, profile.cmyk().grid());
        float[] pcs = profile.cmyk().transform(1.0f, 0.0f, 0.0f, 0.0f);
        assertEquals(13107 / 65535.0f, pcs[0], 0.001f);
        assertEquals(0.0f, pcs[1], 0.001f);
        Color linear = profile.toExtendedLinearCmyk(1.0f, 0.0f, 0.0f, 0.0f, 1.0f);
        Color zero = profile.toExtendedLinearCmyk(0.0f, 0.0f, 0.0f, 0.0f, 1.0f);
        assertTrue(linear.red() > zero.red() + 0.02f);
    }

    /// Uses AToB1 when AToB0 is absent on a CMYK profile.
    @Test
    void cmykAToB1UsedWhenAToB0Absent() {
        IccProfile profile = IccProfile.parse(cmykMft1A2b1());
        assertNotNull(profile.cmyk());
        float[] pcs = profile.cmyk().transform(1.0f, 0.0f, 0.0f, 0.0f);
        assertEquals(51 / 255.0f, pcs[0], 0.001f);
    }

    /// Inverts D65 white through a CMYK BToA0 unique cyan/black cell.
    @Test
    void cmykBToA0InvertsWhiteToUniqueCyan() {
        IccProfile profile = IccProfile.parse(cmykMft1A2b0AndB2a0());
        assertNotNull(profile.cmykBToA());
        assertEquals(true, profile.cmykBToA().inverse());
        float[] device = profile.fromExtendedLinearCmyk(Color.extendedLinear(1.0f, 1.0f, 1.0f, 1.0f));
        assertEquals(1.0f, device[0], 0.05f);
        assertEquals(0.0f, device[1], 0.05f);
        assertEquals(0.0f, device[2], 0.05f);
        assertEquals(128 / 255.0f, device[3], 0.05f);
        Color cmy = profile.fromExtendedLinear(Color.extendedLinear(1.0f, 1.0f, 1.0f, 1.0f));
        assertEquals(1.0f, cmy.red(), 0.05f);
        assertEquals(0.0f, cmy.green(), 0.05f);
        assertEquals(0.0f, cmy.blue(), 0.05f);
    }

    /// Decodes an 8-bit Lab CMYK AToB0 unique cell of D50 white.
    @Test
    void cmykLabPcsAToB0DecodesD50White() {
        IccProfile profile = IccProfile.parse(cmykMft1LabA2b0());
        assertTrue(profile.labPcs());
        Color linear = profile.toExtendedLinearCmyk(1.0f, 0.0f, 0.0f, 0.0f, 1.0f);
        assertEquals(1.0f, linear.red(), 0.05f);
        assertEquals(1.0f, linear.green(), 0.05f);
        assertEquals(1.0f, linear.blue(), 0.05f);
    }

    /// Rejects a CMYK header that has no 4×3 AToB LUT.
    @Test
    void cmykProfileRejectsMissingAToB() {
        assertThrows(IllegalArgumentException.class, () -> IccProfile.parse(cmykHeaderWithoutLut()));
    }

    /// Uses AToB3 when AToB0 through AToB2 are absent on a CMYK profile.
    @Test
    void cmykAToB3UsedWhenEarlierAToBAbsent() {
        IccProfile profile = IccProfile.parse(cmykMft1Forward(0x4132_4233, false));
        assertNotNull(profile.cmyk());
        float[] pcs = profile.cmyk().transform(1.0f, 0.0f, 0.0f, 0.0f);
        assertEquals(51 / 255.0f, pcs[0], 0.001f);
        Color linear = profile.toExtendedLinearCmyk(1.0f, 0.0f, 0.0f, 0.0f, 1.0f);
        Color zero = profile.toExtendedLinearCmyk(0.0f, 0.0f, 0.0f, 0.0f, 1.0f);
        assertTrue(linear.red() > zero.red() + 0.05f);
    }

    /// Parses a CMYK `mAB ` AToB0 unique cyan cell through [`IccProfile#parse(byte[])`].
    @Test
    void cmykMabAToB0LooksUpUniqueCyanCell() {
        IccProfile profile = IccProfile.parse(cmykMabA2b0());
        assertEquals("CMYK", profile.deviceColorSpace());
        assertNotNull(profile.cmyk());
        assertEquals(false, profile.cmyk().inverse());
        assertEquals(2, profile.cmyk().grid());
        float[] pcs = profile.cmyk().transform(1.0f, 0.0f, 0.0f, 0.0f);
        assertEquals(51 / 255.0f, pcs[0], 0.001f);
        assertEquals(0.0f, pcs[1], 0.001f);
        assertEquals(0.0f, pcs[2], 0.001f);
        Color linear = profile.toExtendedLinearCmyk(1.0f, 0.0f, 0.0f, 0.0f, 1.0f);
        Color black = profile.toExtendedLinearCmyk(0.0f, 0.0f, 0.0f, 1.0f, 1.0f);
        assertTrue(Math.abs(linear.red() - black.red()) > 0.05f
                || Math.abs(linear.green() - black.green()) > 0.05f);
    }

    /// Inverts D65 white through a CMYK `mBA ` BToA0 unique cyan/black plane.
    @Test
    void cmykMbaBToA0InvertsWhiteToUniqueCyan() {
        IccProfile profile = IccProfile.parse(cmykMabA2b0AndMbaB2a0());
        assertNotNull(profile.cmykBToA());
        assertEquals(true, profile.cmykBToA().inverse());
        float[] device = profile.fromExtendedLinearCmyk(Color.extendedLinear(1.0f, 1.0f, 1.0f, 1.0f));
        assertEquals(1.0f, device[0], 0.05f);
        assertEquals(0.0f, device[1], 0.05f);
        assertEquals(0.0f, device[2], 0.05f);
        assertEquals(128 / 255.0f, device[3], 0.05f);
    }

    /// Applies a 3×4 `mAB ` matrix after the CMYK CLUT on [`IccProfile#parse(byte[])`].
    @Test
    void cmykMabMatrixDoublesUniqueCyan() {
        IccProfile identity = IccProfile.parse(cmykMabA2b0());
        IccProfile scaled = IccProfile.parse(cmykMabA2b0Matrix());
        float[] plain = identity.cmyk().transform(1.0f, 0.0f, 0.0f, 0.0f);
        float[] doubled = scaled.cmyk().transform(1.0f, 0.0f, 0.0f, 0.0f);
        assertEquals(51 / 255.0f, plain[0], 0.001f);
        assertEquals(102 / 255.0f, doubled[0], 0.001f);
        assertEquals(12, scaled.cmyk().matrix().length);
        assertEquals(2.0f, scaled.cmyk().matrix()[0], 0.001f);
    }

    /// Uses the AToB3 `mft2` CLUT when AToB0 through AToB2 are absent.
    @Test
    void atoB3ClutOverridesMatrixPath() {
        IccProfile matrix = IccProfile.parse(minimalSrgbMatrixProfile());
        IccProfile lut = IccProfile.parse(matrixPlusTaggedLut(0x4132_4233, 13107));
        assertEquals(null, lut.clut());
        assertEquals(null, lut.clutAToB1());
        assertEquals(null, lut.clutAToB2());
        assertNotNull(lut.clutAToB3());
        assertEquals(2, lut.clutAToB3().grid());
        Color matrixRed = matrix.toExtendedLinear(1.0f, 0.0f, 0.0f, 1.0f);
        Color lutRed = lut.toExtendedLinear(1.0f, 0.0f, 0.0f, 1.0f);
        assertTrue(Math.abs(matrixRed.red() - lutRed.red()) > 0.05f);
        float[] xyz = lut.clutAToB3().transform(1.0f, 0.0f, 0.0f);
        assertEquals(13107 / 65535.0f, xyz[0], 0.001f);
        assertEquals(0.0f, xyz[1], 0.001f);
        assertEquals(0.0f, xyz[2], 0.001f);
    }

    /// Uses BToA3 when inverting a unique CLUT cell.
    @Test
    void bToA3ClutInvertsAUniqueCell() {
        IccProfile lut = IccProfile.parse(matrixPlusTaggedLut(0x4232_4133, 65535));
        assertNotNull(lut.clutBToA3());
        float[] rgb = lut.clutBToA3().transform(1.0f, 0.0f, 0.0f);
        assertEquals(1.0f, rgb[0], 0.001f);
        assertEquals(0.0f, rgb[1], 0.001f);
        assertEquals(0.0f, rgb[2], 0.001f);
        Color device = lut.fromExtendedLinear(Color.extendedLinear(1.0f, 0.0f, 0.0f, 1.0f));
        assertTrue(device.red() >= 0.0f);
    }

    /// Decodes ICC parametric function type 1 as `(aX + b)^g`.
    @Test
    void paraType1DecodesPowerWithScale() {
        IccProfile profile = IccProfile.parse(matrixWithParaType1());
        assertEquals(1, profile.redTrc().paraFunction());
        assertEquals(0.25f, profile.redTrc().decode(0.5f), 0.001f);
        Color linear = profile.toExtendedLinear(0.5f, 0.5f, 0.5f, 1.0f);
        assertTrue(linear.red() > 0.05f);
        assertTrue(linear.red() < 0.5f);
    }

    /// Decodes ICC parametric function type 3 as the piecewise sRGB-style function.
    @Test
    void paraType3UsesLinearToe() {
        IccProfile.Curve curve = new IccProfile.Curve(
                2.0f,
                new float[0],
                3,
                1.0f,
                0.0f,
                0.5f,
                0.5f,
                0.0f,
                0.0f
        );
        assertEquals(0.25f, curve.decode(0.5f), 0.001f);
        assertEquals(0.2f, curve.decode(0.4f), 0.001f);
    }

    /// Decodes ICC parametric function type 2 as a constant toe plus a power branch.
    @Test
    void paraType2UsesConstantToe() {
        IccProfile.Curve curve = new IccProfile.Curve(
                2.0f,
                new float[0],
                2,
                1.0f,
                0.0f,
                0.1f,
                0.0f,
                0.0f,
                0.0f
        );
        assertEquals(0.1f, curve.decode(0.0f), 0.001f);
        assertEquals(0.35f, curve.decode(0.5f), 0.001f);
        assertEquals(0.5f, curve.encode(0.35f), 0.02f);
    }

    /// Decodes ICC parametric function type 4 as a linear toe plus an offset power branch.
    @Test
    void paraType4UsesOffsetPower() {
        IccProfile.Curve curve = new IccProfile.Curve(
                2.0f,
                new float[0],
                4,
                1.0f,
                0.0f,
                0.5f,
                0.5f,
                0.1f,
                0.0f
        );
        assertEquals(0.2f, curve.decode(0.4f), 0.001f);
        assertEquals(0.35f, curve.decode(0.5f), 0.001f);
    }

    /// Rejects a truncated header instead of inventing tags.
    @Test
    void rejectsTruncatedProfile() {
        assertThrows(IllegalArgumentException.class, () -> IccProfile.parse(new byte[64]));
    }

    /// Uses the AToB0 CLUT-only `mAB ` tag instead of the matrix/TRC path.
    @Test
    void atoB0MabClutOverridesMatrixPath() {
        IccProfile matrix = IccProfile.parse(minimalSrgbMatrixProfile());
        IccProfile lut = IccProfile.parse(matrixPlusA2b0Mab());
        assertNotNull(lut.clut());
        assertEquals(2, lut.clut().grid());
        Color matrixRed = matrix.toExtendedLinear(1.0f, 0.0f, 0.0f, 1.0f);
        Color lutRed = lut.toExtendedLinear(1.0f, 0.0f, 0.0f, 1.0f);
        assertTrue(Math.abs(matrixRed.red() - lutRed.red()) > 0.05f);
        float[] xyz = lut.clut().transform(1.0f, 0.0f, 0.0f);
        assertEquals(51 / 255.0f, xyz[0], 0.001f);
        assertEquals(0.0f, xyz[1], 0.001f);
        assertEquals(0.0f, xyz[2], 0.001f);
    }

    /// Scales the unique AToB0 cell through the mAB 3×4 matrix.
    @Test
    void atoB0MabMatrixScalesUniqueCell() {
        IccProfile lut = IccProfile.parse(matrixPlusA2b0MabMatrix());
        float[] xyz = lut.clut().transform(1.0f, 0.0f, 0.0f);
        assertEquals(2.0f * 51 / 255.0f, xyz[0], 0.001f);
        assertEquals(0.0f, xyz[1], 0.001f);
        assertEquals(0.0f, xyz[2], 0.001f);
        Color converted = lut.toExtendedLinear(1.0f, 0.0f, 0.0f, 1.0f);
        assertTrue(converted.red() > 0.05f);
    }

    /// Applies A-curve gamma 2 before CLUT interpolation.
    @Test
    void atoB0MabACurveMapsHalfRed() {
        IccProfile identity = IccProfile.parse(matrixPlusA2b0Mab());
        IccProfile gamma = IccProfile.parse(matrixPlusA2b0MabAGamma());
        float[] linear = identity.clut().transform(0.5f, 0.0f, 0.0f);
        float[] curved = gamma.clut().transform(0.5f, 0.0f, 0.0f);
        assertEquals(0.5f * 51 / 255.0f, linear[0], 0.001f);
        assertEquals(0.25f * 51 / 255.0f, curved[0], 0.001f);
        Color converted = gamma.toExtendedLinear(0.5f, 0.0f, 0.0f, 1.0f);
        assertTrue(converted.red() >= 0.0f);
    }

    /// Applies B-curve gamma 2 after CLUT interpolation.
    @Test
    void atoB0MabBCurveSquaresUniqueCell() {
        IccProfile lut = IccProfile.parse(matrixPlusA2b0MabBGamma());
        float unique = 51 / 255.0f;
        float[] xyz = lut.clut().transform(1.0f, 0.0f, 0.0f);
        assertEquals(unique * unique, xyz[0], 0.001f);
        Color converted = lut.toExtendedLinear(1.0f, 0.0f, 0.0f, 1.0f);
        assertTrue(converted.red() >= 0.0f);
    }

    /// Applies A-curve `para` function 0 (gamma 2) before CLUT interpolation.
    @Test
    void atoB0MabAParaMapsHalfRed() {
        IccProfile lut = IccProfile.parse(matrixPlusA2b0MabAPara());
        float[] xyz = lut.clut().transform(0.5f, 0.0f, 0.0f);
        assertEquals(0.25f * 51 / 255.0f, xyz[0], 0.001f);
    }

    /// Applies A-curve `para` function 1 `Y = (aX + b)^g` before CLUT interpolation.
    @Test
    void atoB0MabAParaType1MapsOneToHalf() {
        IccProfile lut = IccProfile.parse(matrixPlusLutAbPara(0x4132_4230, "mAB ", 28, 1, 1.0f, 0.5f, 0.0f));
        float[] xyz = lut.clut().transform(1.0f, 0.0f, 0.0f);
        assertEquals(0.5f * 51 / 255.0f, xyz[0], 0.001f);
    }

    /// Applies A-curve `para` function 2 with a constant offset before CLUT interpolation.
    @Test
    void atoB0MabAParaType2AddsOffset() {
        IccProfile lut = IccProfile.parse(matrixPlusLutAbPara(0x4132_4230, "mAB ", 28, 2, 1.0f, 1.0f, 0.0f, 0.25f));
        float[] xyz = lut.clut().transform(0.75f, 0.0f, 0.0f);
        assertEquals(51 / 255.0f, xyz[0], 0.001f);
    }

    /// Applies A-curve `para` function 3 linear branch before CLUT interpolation.
    @Test
    void atoB0MabAParaType3UsesLinearBranch() {
        IccProfile lut = IccProfile.parse(matrixPlusLutAbPara(
                0x4132_4230, "mAB ", 28, 3, 1.0f, 1.0f, 0.0f, 0.5f, 2.0f));
        float[] xyz = lut.clut().transform(1.0f, 0.0f, 0.0f);
        assertEquals(0.5f * 51 / 255.0f, xyz[0], 0.001f);
    }

    /// Applies A-curve `para` function 4 linear branch before CLUT interpolation.
    @Test
    void atoB0MabAParaType4UsesLinearBranch() {
        IccProfile lut = IccProfile.parse(matrixPlusLutAbPara(
                0x4132_4230, "mAB ", 28, 4, 1.0f, 1.0f, 0.0f, 0.5f, 2.0f, 0.0f, 0.0f));
        float[] xyz = lut.clut().transform(1.0f, 0.0f, 0.0f);
        assertEquals(0.5f * 51 / 255.0f, xyz[0], 0.001f);
    }

    /// Applies B-curve `para` function 1 before mBA CLUT interpolation.
    @Test
    void bToA0MbaBParaType1MapsOneToHalf() {
        IccProfile lut = IccProfile.parse(matrixPlusLutAbPara(0x4232_4130, "mBA ", 12, 1, 1.0f, 0.5f, 0.0f));
        float[] device = lut.clutBToA0().transform(1.0f, 0.0f, 0.0f);
        assertEquals(0.5f * 51 / 255.0f, device[0], 0.001f);
    }

    /// Applies a two-sample tabulated A-curve before CLUT interpolation.
    @Test
    void atoB0MabTabulatedACurveMapsOneToHalf() {
        IccProfile lut = IccProfile.parse(matrixPlusA2b0MabATable());
        float[] xyz = lut.clut().transform(1.0f, 0.0f, 0.0f);
        assertEquals(0.5f * 51 / 255.0f, xyz[0], 0.001f);
        Color converted = lut.toExtendedLinear(1.0f, 0.0f, 0.0f, 1.0f);
        assertTrue(converted.red() >= 0.0f);
    }

    /// Applies a two-sample tabulated B-curve before mBA CLUT interpolation.
    @Test
    void bToA0MbaTabulatedBCurveMapsOneToHalf() {
        IccProfile lut = IccProfile.parse(matrixPlusB2a0MbaBTable());
        float[] device = lut.clutBToA0().transform(1.0f, 0.0f, 0.0f);
        assertEquals(0.5f * 51 / 255.0f, device[0], 0.001f);
        Color converted = lut.fromExtendedLinear(Color.extendedLinear(1.0f, 0.0f, 0.0f, 1.0f));
        assertTrue(converted.red() >= 0.0f);
    }

    /// Uses a 16-bit mAB CLUT instead of the 8-bit table.
    @Test
    void atoB0Mab16BitClutOverridesMatrixPath() {
        IccProfile lut = IccProfile.parse(matrixPlusA2b0Mab16());
        float[] xyz = lut.clut().transform(1.0f, 0.0f, 0.0f);
        assertEquals(13107 / 65535.0f, xyz[0], 0.001f);
        assertEquals(0.0f, xyz[1], 0.001f);
        assertEquals(0.0f, xyz[2], 0.001f);
        Color converted = lut.toExtendedLinear(1.0f, 0.0f, 0.0f, 1.0f);
        assertTrue(converted.red() > 0.05f);
    }

    /// Applies M-curve gamma 2 after CLUT interpolation.
    @Test
    void atoB0MabMCurveSquaresUniqueCell() {
        IccProfile lut = IccProfile.parse(matrixPlusA2b0MabMGamma());
        float unique = 51 / 255.0f;
        float[] xyz = lut.clut().transform(1.0f, 0.0f, 0.0f);
        assertEquals(unique * unique, xyz[0], 0.001f);
        Color converted = lut.toExtendedLinear(1.0f, 0.0f, 0.0f, 1.0f);
        assertTrue(converted.red() >= 0.0f);
    }

    /// Applies the mBA matrix before CLUT interpolation.
    @Test
    void bToA0MbaMatrixLooksUpUniqueCellFromHalf() {
        IccProfile identity = IccProfile.parse(matrixPlusB2a0Mba());
        IccProfile scaled = IccProfile.parse(matrixPlusB2a0MbaMatrix());
        float[] half = identity.clutBToA0().transform(0.5f, 0.0f, 0.0f);
        float[] unique = scaled.clutBToA0().transform(0.5f, 0.0f, 0.0f);
        assertEquals(0.5f * 51 / 255.0f, half[0], 0.001f);
        assertEquals(51 / 255.0f, unique[0], 0.001f);
        Color device = scaled.fromExtendedLinear(Color.extendedLinear(0.5f, 0.0f, 0.0f, 1.0f));
        assertTrue(device.red() >= 0.0f);
    }

    /// Uses the AToB0 `mft1` CLUT instead of the matrix/TRC path.
    @Test
    void atoB0Mft1ClutOverridesMatrixPath() {
        IccProfile matrix = IccProfile.parse(minimalSrgbMatrixProfile());
        IccProfile lut = IccProfile.parse(matrixPlusA2b0Mft1());
        assertNotNull(lut.clut());
        assertEquals(2, lut.clut().grid());
        Color matrixRed = matrix.toExtendedLinear(1.0f, 0.0f, 0.0f, 1.0f);
        Color lutRed = lut.toExtendedLinear(1.0f, 0.0f, 0.0f, 1.0f);
        assertTrue(Math.abs(matrixRed.red() - lutRed.red()) > 0.05f);
        float[] xyz = lut.clut().transform(1.0f, 0.0f, 0.0f);
        assertEquals(51 / 255.0f, xyz[0], 0.001f);
        assertEquals(0.0f, xyz[1], 0.001f);
        assertEquals(0.0f, xyz[2], 0.001f);
    }

    /// Applies a planar mft1 input table before CLUT interpolation.
    @Test
    void atoB0Mft1InputTableMapsOneToHalf() {
        IccProfile lut = IccProfile.parse(matrixPlusA2b0Mft1InputHalf());
        float[] xyz = lut.clut().transform(1.0f, 0.0f, 0.0f);
        assertEquals((128 / 255.0f) * (51 / 255.0f), xyz[0], 0.001f);
    }

    /// Applies a planar mft2 output table after CLUT interpolation.
    @Test
    void atoB0Mft2OutputTableRemapsUniqueCell() {
        IccProfile lut = IccProfile.parse(matrixPlusA2b0Mft2OutputOne());
        float[] xyz = lut.clut().transform(1.0f, 0.0f, 0.0f);
        assertEquals(1.0f, xyz[0], 0.001f);
        assertEquals(0.0f, xyz[1], 0.001f);
    }

    /// Applies the `mft1` 3×3 matrix before input tables and CLUT lookup.
    @Test
    void atoB0Mft1MatrixLooksUpUniqueCellFromHalf() {
        IccProfile identity = IccProfile.parse(matrixPlusA2b0Mft1());
        IccProfile scaled = IccProfile.parse(matrixPlusA2b0Mft1Matrix());
        float[] half = identity.clut().transform(0.5f, 0.0f, 0.0f);
        float[] unique = scaled.clut().transform(0.5f, 0.0f, 0.0f);
        assertEquals(0.5f * 51 / 255.0f, half[0], 0.001f);
        assertEquals(51 / 255.0f, unique[0], 0.001f);
    }

    /// Applies the `mft2` 3×3 matrix before input tables and CLUT lookup.
    @Test
    void atoB0Mft2MatrixLooksUpUniqueCellFromHalf() {
        IccProfile identity = IccProfile.parse(matrixPlusA2b0());
        IccProfile scaled = IccProfile.parse(matrixPlusA2b0Mft2Matrix());
        float[] half = identity.clut().transform(0.5f, 0.0f, 0.0f);
        float[] unique = scaled.clut().transform(0.5f, 0.0f, 0.0f);
        assertEquals(0.1f, half[0], 0.001f);
        assertEquals(0.2f, unique[0], 0.001f);
    }

    /// Uses the AToB0 `mft2` CLUT instead of the matrix/TRC path.
    @Test
    void atoB0ClutOverridesMatrixPath() {
        IccProfile matrix = IccProfile.parse(minimalSrgbMatrixProfile());
        IccProfile lut = IccProfile.parse(matrixPlusA2b0());
        assertEquals(null, matrix.clut());
        assertNotNull(lut.clut());
        assertEquals(2, lut.clut().grid());
        Color matrixRed = matrix.toExtendedLinear(1.0f, 0.0f, 0.0f, 1.0f);
        Color lutRed = lut.toExtendedLinear(1.0f, 0.0f, 0.0f, 1.0f);
        assertTrue(Math.abs(matrixRed.red() - lutRed.red()) > 0.05f);
        float[] xyz = lut.clut().transform(1.0f, 0.0f, 0.0f);
        assertEquals(0.2f, xyz[0], 0.001f);
        assertEquals(0.0f, xyz[1], 0.001f);
        assertEquals(0.0f, xyz[2], 0.001f);
    }

    /// Uses AToB1 when AToB0 is absent.
    @Test
    void atoB1ClutIsUsedWhenAToB0IsMissing() {
        IccProfile lut = IccProfile.parse(matrixPlusTaggedLut(0x4132_4231, 13107));
        assertEquals(null, lut.clut());
        assertNotNull(lut.clutAToB1());
        float[] xyz = lut.clutAToB1().transform(1.0f, 0.0f, 0.0f);
        assertEquals(0.2f, xyz[0], 0.001f);
        Color converted = lut.toExtendedLinear(1.0f, 0.0f, 0.0f, 1.0f);
        assertTrue(converted.red() > 0.05f);
    }

    /// Uses AToB2 when AToB0 and AToB1 are absent.
    @Test
    void atoB2ClutIsUsedWhenEarlierIntentsAreMissing() {
        IccProfile lut = IccProfile.parse(matrixPlusTaggedLut(0x4132_4232, 13107));
        assertEquals(null, lut.clut());
        assertEquals(null, lut.clutAToB1());
        assertNotNull(lut.clutAToB2());
        float[] xyz = lut.clutAToB2().transform(1.0f, 0.0f, 0.0f);
        assertEquals(0.2f, xyz[0], 0.001f);
        Color converted = lut.toExtendedLinear(1.0f, 0.0f, 0.0f, 1.0f);
        assertTrue(converted.red() > 0.05f);
    }

    /// Uses BToA2 when BToA0 and BToA1 are absent.
    @Test
    void bToA2ClutIsUsedWhenEarlierIntentsAreMissing() {
        IccProfile lut = IccProfile.parse(matrixPlusTaggedLut(0x4232_4132, 65535));
        assertEquals(null, lut.clutBToA0());
        assertEquals(null, lut.clutBToA1());
        assertNotNull(lut.clutBToA2());
        float[] rgb = lut.clutBToA2().transform(1.0f, 0.0f, 0.0f);
        assertEquals(1.0f, rgb[0], 0.001f);
        Color device = lut.fromExtendedLinear(Color.extendedLinear(1.0f, 0.0f, 0.0f, 1.0f));
        assertTrue(device.red() >= 0.0f);
    }

    /// Uses BToA1 when BToA0 is absent.
    @Test
    void bToA1ClutIsUsedWhenBToA0IsMissing() {
        IccProfile lut = IccProfile.parse(matrixPlusTaggedLut(0x4232_4131, 65535));
        assertEquals(null, lut.clutBToA0());
        assertNotNull(lut.clutBToA1());
        float[] rgb = lut.clutBToA1().transform(1.0f, 0.0f, 0.0f);
        assertEquals(1.0f, rgb[0], 0.001f);
        assertEquals(0.0f, rgb[1], 0.001f);
        assertEquals(0.0f, rgb[2], 0.001f);
        Color device = lut.fromExtendedLinear(Color.extendedLinear(1.0f, 0.0f, 0.0f, 1.0f));
        assertTrue(device.red() >= 0.0f);
    }

    /// Uses BToA0 when inverting a unique CLUT cell.
    @Test
    void bToA0ClutInvertsAUniqueCell() {
        IccProfile lut = IccProfile.parse(matrixPlusB2a0());
        assertNotNull(lut.clutBToA0());
        float[] rgb = lut.clutBToA0().transform(1.0f, 0.0f, 0.0f);
        assertEquals(1.0f, rgb[0], 0.001f);
        assertEquals(0.0f, rgb[1], 0.001f);
        assertEquals(0.0f, rgb[2], 0.001f);
        Color device = lut.fromExtendedLinear(Color.extendedLinear(1.0f, 0.0f, 0.0f, 1.0f));
        assertTrue(device.red() >= 0.0f);
    }

    /// Inverts the matrix/TRC path so encoded white stays near white.
    @Test
    void matrixPathFromExtendedLinearRoundTripsWhite() {
        IccProfile profile = IccProfile.parse(minimalSrgbMatrixProfile());
        Color encoded = profile.fromExtendedLinear(Color.extendedLinear(1.0f, 1.0f, 1.0f, 1.0f));
        assertEquals(1.0f, encoded.red(), 0.05f);
        assertEquals(1.0f, encoded.green(), 0.05f);
        assertEquals(1.0f, encoded.blue(), 0.05f);
    }

    /// Builds the matrix profile plus a CLUT-only `mAB ` AToB0 tag.
    private static byte[] matrixPlusA2b0Mab() {
        byte[] base = minimalSrgbMatrixProfile();
        int grid = 2;
        int clutValues = grid * grid * grid * 3;
        int mabSize = 32 + 20 + clutValues;
        int tagCount = 7;
        int table = 132 + tagCount * 12;
        int xyzSize = 20;
        int curveSize = 12;
        int size = table + 3 * xyzSize + 3 * curveSize + mabSize;
        byte[] bytes = new byte[size];
        System.arraycopy(base, 0, bytes, 0, 128);
        putU32(bytes, 0, size);
        putU32(bytes, 128, tagCount);
        int redXyz = table;
        int greenXyz = redXyz + xyzSize;
        int blueXyz = greenXyz + xyzSize;
        int redTrc = blueXyz + xyzSize;
        int greenTrc = redTrc + curveSize;
        int blueTrc = greenTrc + curveSize;
        int a2b0 = blueTrc + curveSize;
        putTag(bytes, 0, 0x7258_595A, redXyz, xyzSize);
        putTag(bytes, 1, 0x6758_595A, greenXyz, xyzSize);
        putTag(bytes, 2, 0x6258_595A, blueXyz, xyzSize);
        putTag(bytes, 3, 0x7254_5243, redTrc, curveSize);
        putTag(bytes, 4, 0x6754_5243, greenTrc, curveSize);
        putTag(bytes, 5, 0x6254_5243, blueTrc, curveSize);
        putTag(bytes, 6, 0x4132_4230, a2b0, mabSize);
        putXyz(bytes, redXyz, 0.4360657f, 0.2224884f, 0.0139160f);
        putXyz(bytes, greenXyz, 0.3851477f, 0.7168732f, 0.0971045f);
        putXyz(bytes, blueXyz, 0.1430664f, 0.0606084f, 0.7141733f);
        putIdentityCurve(bytes, redTrc);
        putIdentityCurve(bytes, greenTrc);
        putIdentityCurve(bytes, blueTrc);
        putSignature(bytes, a2b0, "mAB ");
        bytes[a2b0 + 8] = 3;
        bytes[a2b0 + 9] = 3;
        putU32(bytes, a2b0 + 24, 32);
        int clut = a2b0 + 32;
        bytes[clut] = (byte) grid;
        bytes[clut + 1] = (byte) grid;
        bytes[clut + 2] = (byte) grid;
        bytes[clut + 16] = 1;
        int cursor = clut + 20;
        for (int red = 0; red < grid; red++) {
            for (int green = 0; green < grid; green++) {
                for (int blue = 0; blue < grid; blue++) {
                    boolean uniqueCell = red == 1 && green == 0 && blue == 0;
                    bytes[cursor++] = (byte) (uniqueCell ? 51 : 0);
                    bytes[cursor++] = 0;
                    bytes[cursor++] = 0;
                }
            }
        }
        return bytes;
    }

    /// Builds the matrix profile plus an `mAB ` AToB0 tag whose matrix doubles X.
    private static byte[] matrixPlusA2b0MabMatrix() {
        return matrixPlusA2b0MabWithStages(true, false, false);
    }

    /// Builds the matrix profile plus an `mAB ` AToB0 tag whose A curves are gamma 2.
    private static byte[] matrixPlusA2b0MabAGamma() {
        return matrixPlusA2b0MabWithStages(false, true, false);
    }

    /// Builds the matrix profile plus an `mAB ` AToB0 tag whose B curves are gamma 2.
    private static byte[] matrixPlusA2b0MabBGamma() {
        return matrixPlusLutAb(0x4132_4230, "mAB ", false, false, false, true);
    }

    /// Builds an `mAB ` AToB0 tag whose A curves are two-sample tables mapping 1 to 0.5.
    private static byte[] matrixPlusA2b0MabATable() {
        return matrixPlusLutAbTable(0x4132_4230, "mAB ", 28);
    }

    /// Builds an `mAB ` AToB0 tag whose A curves are `para` function 0 with gamma 2.
    private static byte[] matrixPlusA2b0MabAPara() {
        return matrixPlusLutAbPara(0x4132_4230, "mAB ", 28, 0, 2.0f);
    }

    /// Builds one `mAB ` / `mBA ` tag with identical `para` curves at `curveHeader`.
    ///
    /// @param signature the AToB0 or BToA0 tag signature
    /// @param type the four-character LUT type
    /// @param curveHeader the tag-relative offset of the curve pointer field
    /// @param function the ICC para function
    /// @param params s15.16 parameters starting with `g`
    /// @return the profile bytes
    private static byte[] matrixPlusLutAbPara(
            int signature,
            String type,
            int curveHeader,
            int function,
            float... params
    ) {
        byte[] base = minimalSrgbMatrixProfile();
        int grid = 2;
        int clutValues = grid * grid * grid * 3;
        int curveBytes = (12 + params.length * 4 + 3) & ~3;
        int aSize = curveBytes + 32;
        int clutStart = 32 + aSize;
        int mabSize = clutStart + 20 + clutValues;
        int tagCount = 7;
        int table = 132 + tagCount * 12;
        int xyzSize = 20;
        int identityCurve = 12;
        int size = table + 3 * xyzSize + 3 * identityCurve + mabSize;
        byte[] bytes = new byte[size];
        System.arraycopy(base, 0, bytes, 0, 128);
        putU32(bytes, 0, size);
        putU32(bytes, 128, tagCount);
        int redXyz = table;
        int greenXyz = redXyz + xyzSize;
        int blueXyz = greenXyz + xyzSize;
        int redTrc = blueXyz + xyzSize;
        int greenTrc = redTrc + identityCurve;
        int blueTrc = greenTrc + identityCurve;
        int lut = blueTrc + identityCurve;
        putTag(bytes, 0, 0x7258_595A, redXyz, xyzSize);
        putTag(bytes, 1, 0x6758_595A, greenXyz, xyzSize);
        putTag(bytes, 2, 0x6258_595A, blueXyz, xyzSize);
        putTag(bytes, 3, 0x7254_5243, redTrc, identityCurve);
        putTag(bytes, 4, 0x6754_5243, greenTrc, identityCurve);
        putTag(bytes, 5, 0x6254_5243, blueTrc, identityCurve);
        putTag(bytes, 6, signature, lut, mabSize);
        putXyz(bytes, redXyz, 0.4360657f, 0.2224884f, 0.0139160f);
        putXyz(bytes, greenXyz, 0.3851477f, 0.7168732f, 0.0971045f);
        putXyz(bytes, blueXyz, 0.1430664f, 0.0606084f, 0.7141733f);
        putIdentityCurve(bytes, redTrc);
        putIdentityCurve(bytes, greenTrc);
        putIdentityCurve(bytes, blueTrc);
        putSignature(bytes, lut, type);
        bytes[lut + 8] = 3;
        bytes[lut + 9] = 3;
        putU32(bytes, lut + curveHeader, 32);
        int cursor = lut + 32;
        putSignature(bytes, cursor, "para");
        putU16(bytes, cursor + 8, function);
        putU16(bytes, cursor + 10, 0);
        for (int index = 0; index < params.length; index++) {
            putS15(bytes, cursor + 12 + index * 4, params[index]);
        }
        cursor += curveBytes;
        for (int channel = 1; channel < 3; channel++) {
            putSignature(bytes, cursor, "para");
            putU16(bytes, cursor + 8, 0);
            putU16(bytes, cursor + 10, 0);
            putS15(bytes, cursor + 12, 1.0f);
            cursor += 16;
        }
        putU32(bytes, lut + 24, clutStart);
        int clut = lut + clutStart;
        bytes[clut] = (byte) grid;
        bytes[clut + 1] = (byte) grid;
        bytes[clut + 2] = (byte) grid;
        bytes[clut + 16] = 1;
        int sample = clut + 20;
        for (int red = 0; red < grid; red++) {
            for (int green = 0; green < grid; green++) {
                for (int blue = 0; blue < grid; blue++) {
                    boolean uniqueCell = red == 1 && green == 0 && blue == 0;
                    bytes[sample++] = (byte) (uniqueCell ? 51 : 0);
                    bytes[sample++] = 0;
                    bytes[sample++] = 0;
                }
            }
        }
        return bytes;
    }

    /// Builds an `mBA ` BToA0 tag whose B curves are two-sample tables mapping 1 to 0.5.
    private static byte[] matrixPlusB2a0MbaBTable() {
        return matrixPlusLutAbTable(0x4232_4130, "mBA ", 12);
    }

    /// Builds one `mAB ` / `mBA ` tag with a two-sample `curv` table at `curveHeader`.
    ///
    /// @param signature the AToB0 or BToA0 tag signature
    /// @param type the four-character LUT type
    /// @param curveHeader the tag-relative offset of the curve pointer field
    /// @return the profile bytes
    private static byte[] matrixPlusLutAbTable(int signature, String type, int curveHeader) {
        byte[] base = minimalSrgbMatrixProfile();
        int grid = 2;
        int clutValues = grid * grid * grid * 3;
        int curveSize = 48;
        int clutStart = 32 + curveSize;
        int mabSize = clutStart + 20 + clutValues;
        int tagCount = 7;
        int table = 132 + tagCount * 12;
        int xyzSize = 20;
        int identityCurve = 12;
        int size = table + 3 * xyzSize + 3 * identityCurve + mabSize;
        byte[] bytes = new byte[size];
        System.arraycopy(base, 0, bytes, 0, 128);
        putU32(bytes, 0, size);
        putU32(bytes, 128, tagCount);
        int redXyz = table;
        int greenXyz = redXyz + xyzSize;
        int blueXyz = greenXyz + xyzSize;
        int redTrc = blueXyz + xyzSize;
        int greenTrc = redTrc + identityCurve;
        int blueTrc = greenTrc + identityCurve;
        int lut = blueTrc + identityCurve;
        putTag(bytes, 0, 0x7258_595A, redXyz, xyzSize);
        putTag(bytes, 1, 0x6758_595A, greenXyz, xyzSize);
        putTag(bytes, 2, 0x6258_595A, blueXyz, xyzSize);
        putTag(bytes, 3, 0x7254_5243, redTrc, identityCurve);
        putTag(bytes, 4, 0x6754_5243, greenTrc, identityCurve);
        putTag(bytes, 5, 0x6254_5243, blueTrc, identityCurve);
        putTag(bytes, 6, signature, lut, mabSize);
        putXyz(bytes, redXyz, 0.4360657f, 0.2224884f, 0.0139160f);
        putXyz(bytes, greenXyz, 0.3851477f, 0.7168732f, 0.0971045f);
        putXyz(bytes, blueXyz, 0.1430664f, 0.0606084f, 0.7141733f);
        putIdentityCurve(bytes, redTrc);
        putIdentityCurve(bytes, greenTrc);
        putIdentityCurve(bytes, blueTrc);
        putSignature(bytes, lut, type);
        bytes[lut + 8] = 3;
        bytes[lut + 9] = 3;
        putU32(bytes, lut + curveHeader, 32);
        int cursor = lut + 32;
        for (int channel = 0; channel < 3; channel++) {
            putSignature(bytes, cursor, "curv");
            putU32(bytes, cursor + 8, 2);
            putU16(bytes, cursor + 12, 0);
            putU16(bytes, cursor + 14, 32768);
            cursor += 16;
        }
        putU32(bytes, lut + 24, clutStart);
        int clut = lut + clutStart;
        bytes[clut] = (byte) grid;
        bytes[clut + 1] = (byte) grid;
        bytes[clut + 2] = (byte) grid;
        bytes[clut + 16] = 1;
        int sample = clut + 20;
        for (int red = 0; red < grid; red++) {
            for (int green = 0; green < grid; green++) {
                for (int blue = 0; blue < grid; blue++) {
                    boolean uniqueCell = red == 1 && green == 0 && blue == 0;
                    bytes[sample++] = (byte) (uniqueCell ? 51 : 0);
                    bytes[sample++] = 0;
                    bytes[sample++] = 0;
                }
            }
        }
        return bytes;
    }

    /// Builds a 16-bit CLUT-only `mAB ` AToB0 tag.
    private static byte[] matrixPlusA2b0Mab16() {
        return matrixPlusA2b0Mab16Bit();
    }

    /// Writes a 16-bit cubic `mAB ` AToB0 CLUT.
    private static byte[] matrixPlusA2b0Mab16Bit() {
        byte[] base = minimalSrgbMatrixProfile();
        int grid = 2;
        int clutValues = grid * grid * grid * 3;
        int mabSize = 32 + 20 + clutValues * 2;
        int tagCount = 7;
        int table = 132 + tagCount * 12;
        int xyzSize = 20;
        int curveSize = 12;
        int size = table + 3 * xyzSize + 3 * curveSize + mabSize;
        byte[] bytes = new byte[size];
        System.arraycopy(base, 0, bytes, 0, 128);
        putU32(bytes, 0, size);
        putU32(bytes, 128, tagCount);
        int redXyz = table;
        int greenXyz = redXyz + xyzSize;
        int blueXyz = greenXyz + xyzSize;
        int redTrc = blueXyz + xyzSize;
        int greenTrc = redTrc + curveSize;
        int blueTrc = greenTrc + curveSize;
        int a2b0 = blueTrc + curveSize;
        putTag(bytes, 0, 0x7258_595A, redXyz, xyzSize);
        putTag(bytes, 1, 0x6758_595A, greenXyz, xyzSize);
        putTag(bytes, 2, 0x6258_595A, blueXyz, xyzSize);
        putTag(bytes, 3, 0x7254_5243, redTrc, curveSize);
        putTag(bytes, 4, 0x6754_5243, greenTrc, curveSize);
        putTag(bytes, 5, 0x6254_5243, blueTrc, curveSize);
        putTag(bytes, 6, 0x4132_4230, a2b0, mabSize);
        putXyz(bytes, redXyz, 0.4360657f, 0.2224884f, 0.0139160f);
        putXyz(bytes, greenXyz, 0.3851477f, 0.7168732f, 0.0971045f);
        putXyz(bytes, blueXyz, 0.1430664f, 0.0606084f, 0.7141733f);
        putIdentityCurve(bytes, redTrc);
        putIdentityCurve(bytes, greenTrc);
        putIdentityCurve(bytes, blueTrc);
        putSignature(bytes, a2b0, "mAB ");
        bytes[a2b0 + 8] = 3;
        bytes[a2b0 + 9] = 3;
        putU32(bytes, a2b0 + 24, 32);
        int clut = a2b0 + 32;
        bytes[clut] = (byte) grid;
        bytes[clut + 1] = (byte) grid;
        bytes[clut + 2] = (byte) grid;
        bytes[clut + 16] = 2;
        int cursor = clut + 20;
        for (int red = 0; red < grid; red++) {
            for (int green = 0; green < grid; green++) {
                for (int blue = 0; blue < grid; blue++) {
                    boolean uniqueCell = red == 1 && green == 0 && blue == 0;
                    putU16(bytes, cursor, uniqueCell ? 13107 : 0);
                    putU16(bytes, cursor + 2, 0);
                    putU16(bytes, cursor + 4, 0);
                    cursor += 6;
                }
            }
        }
        return bytes;
    }

    /// Builds the matrix profile plus an `mAB ` AToB0 tag whose M curves are gamma 2.
    private static byte[] matrixPlusA2b0MabMGamma() {
        return matrixPlusA2b0MabWithStages(false, false, true);
    }

    /// Builds a CLUT-only `mBA ` BToA0 tag.
    private static byte[] matrixPlusB2a0Mba() {
        return matrixPlusLutAb(0x4232_4130, "mBA ", false, false, false, false);
    }

    /// Builds an `mBA ` BToA0 tag whose matrix doubles X before the CLUT.
    private static byte[] matrixPlusB2a0MbaMatrix() {
        return matrixPlusLutAb(0x4232_4130, "mBA ", true, false, false, false);
    }

    /// Builds an `mAB ` AToB0 tag with optional matrix, A-curve, and M-curve stages.
    ///
    /// @param matrix whether to write a 2× X-scale matrix
    /// @param aGamma whether to write gamma-2 A curves
    /// @param mGamma whether to write gamma-2 M curves
    /// @return the profile bytes
    private static byte[] matrixPlusA2b0MabWithStages(boolean matrix, boolean aGamma, boolean mGamma) {
        return matrixPlusLutAb(0x4132_4230, "mAB ", matrix, aGamma, mGamma, false);
    }

    /// Builds one `mAB ` / `mBA ` LUT tag with optional stages.
    ///
    /// @param signature the AToB0 or BToA0 tag signature
    /// @param type the four-character LUT type
    /// @param matrix whether to write a 2× X-scale matrix
    /// @param aGamma whether to write gamma-2 A curves
    /// @param mGamma whether to write gamma-2 M curves
    /// @param bGamma whether to write gamma-2 B curves
    /// @return the profile bytes
    private static byte[] matrixPlusLutAb(
            int signature,
            String type,
            boolean matrix,
            boolean aGamma,
            boolean mGamma,
            boolean bGamma
    ) {
        byte[] base = minimalSrgbMatrixProfile();
        int grid = 2;
        int clutValues = grid * grid * grid * 3;
        int matrixSize = matrix ? 48 : 0;
        int aSize = aGamma ? 48 : 0;
        int mSize = mGamma ? 48 : 0;
        int bSize = bGamma ? 48 : 0;
        int clutStart = 32 + matrixSize + aSize + mSize + bSize;
        int mabSize = clutStart + 20 + clutValues;
        int tagCount = 7;
        int table = 132 + tagCount * 12;
        int xyzSize = 20;
        int curveSize = 12;
        int size = table + 3 * xyzSize + 3 * curveSize + mabSize;
        byte[] bytes = new byte[size];
        System.arraycopy(base, 0, bytes, 0, 128);
        putU32(bytes, 0, size);
        putU32(bytes, 128, tagCount);
        int redXyz = table;
        int greenXyz = redXyz + xyzSize;
        int blueXyz = greenXyz + xyzSize;
        int redTrc = blueXyz + xyzSize;
        int greenTrc = redTrc + curveSize;
        int blueTrc = greenTrc + curveSize;
        int a2b0 = blueTrc + curveSize;
        putTag(bytes, 0, 0x7258_595A, redXyz, xyzSize);
        putTag(bytes, 1, 0x6758_595A, greenXyz, xyzSize);
        putTag(bytes, 2, 0x6258_595A, blueXyz, xyzSize);
        putTag(bytes, 3, 0x7254_5243, redTrc, curveSize);
        putTag(bytes, 4, 0x6754_5243, greenTrc, curveSize);
        putTag(bytes, 5, 0x6254_5243, blueTrc, curveSize);
        putTag(bytes, 6, signature, a2b0, mabSize);
        putXyz(bytes, redXyz, 0.4360657f, 0.2224884f, 0.0139160f);
        putXyz(bytes, greenXyz, 0.3851477f, 0.7168732f, 0.0971045f);
        putXyz(bytes, blueXyz, 0.1430664f, 0.0606084f, 0.7141733f);
        putIdentityCurve(bytes, redTrc);
        putIdentityCurve(bytes, greenTrc);
        putIdentityCurve(bytes, blueTrc);
        putSignature(bytes, a2b0, type);
        bytes[a2b0 + 8] = 3;
        bytes[a2b0 + 9] = 3;
        int cursor = a2b0 + 32;
        if (matrix) {
            putU32(bytes, a2b0 + 16, 32);
            putS15(bytes, cursor, 2.0f);
            putS15(bytes, cursor + 16, 1.0f);
            putS15(bytes, cursor + 32, 1.0f);
            cursor += 48;
        }
        if (aGamma) {
            putU32(bytes, a2b0 + 28, cursor - a2b0);
            for (int channel = 0; channel < 3; channel++) {
                putSignature(bytes, cursor, "curv");
                putU32(bytes, cursor + 8, 1);
                putU16(bytes, cursor + 12, 512);
                cursor += 16;
            }
        }
        if (mGamma) {
            putU32(bytes, a2b0 + 20, cursor - a2b0);
            for (int channel = 0; channel < 3; channel++) {
                putSignature(bytes, cursor, "curv");
                putU32(bytes, cursor + 8, 1);
                putU16(bytes, cursor + 12, 512);
                cursor += 16;
            }
        }
        if (bGamma) {
            putU32(bytes, a2b0 + 12, cursor - a2b0);
            for (int channel = 0; channel < 3; channel++) {
                putSignature(bytes, cursor, "curv");
                putU32(bytes, cursor + 8, 1);
                putU16(bytes, cursor + 12, 512);
                cursor += 16;
            }
        }
        putU32(bytes, a2b0 + 24, clutStart);
        int clut = a2b0 + clutStart;
        bytes[clut] = (byte) grid;
        bytes[clut + 1] = (byte) grid;
        bytes[clut + 2] = (byte) grid;
        bytes[clut + 16] = 1;
        int sample = clut + 20;
        for (int red = 0; red < grid; red++) {
            for (int green = 0; green < grid; green++) {
                for (int blue = 0; blue < grid; blue++) {
                    boolean uniqueCell = red == 1 && green == 0 && blue == 0;
                    bytes[sample++] = (byte) (uniqueCell ? 51 : 0);
                    bytes[sample++] = 0;
                    bytes[sample++] = 0;
                }
            }
        }
        return bytes;
    }

    /// Builds the matrix profile plus a 2³ `mft1` AToB0 tag.
    private static byte[] matrixPlusA2b0Mft1() {
        byte[] base = minimalSrgbMatrixProfile();
        int grid = 2;
        int inputTable = 256 * 3;
        int clutValues = grid * grid * grid * 3;
        int outputTable = 256 * 3;
        int mft1Size = 48 + inputTable + clutValues + outputTable;
        int tagCount = 7;
        int table = 132 + tagCount * 12;
        int xyzSize = 20;
        int curveSize = 12;
        int size = table + 3 * xyzSize + 3 * curveSize + mft1Size;
        byte[] bytes = new byte[size];
        System.arraycopy(base, 0, bytes, 0, 128);
        putU32(bytes, 0, size);
        putU32(bytes, 128, tagCount);
        int redXyz = table;
        int greenXyz = redXyz + xyzSize;
        int blueXyz = greenXyz + xyzSize;
        int redTrc = blueXyz + xyzSize;
        int greenTrc = redTrc + curveSize;
        int blueTrc = greenTrc + curveSize;
        int a2b0 = blueTrc + curveSize;
        putTag(bytes, 0, 0x7258_595A, redXyz, xyzSize);
        putTag(bytes, 1, 0x6758_595A, greenXyz, xyzSize);
        putTag(bytes, 2, 0x6258_595A, blueXyz, xyzSize);
        putTag(bytes, 3, 0x7254_5243, redTrc, curveSize);
        putTag(bytes, 4, 0x6754_5243, greenTrc, curveSize);
        putTag(bytes, 5, 0x6254_5243, blueTrc, curveSize);
        putTag(bytes, 6, 0x4132_4230, a2b0, mft1Size);
        putXyz(bytes, redXyz, 0.4360657f, 0.2224884f, 0.0139160f);
        putXyz(bytes, greenXyz, 0.3851477f, 0.7168732f, 0.0971045f);
        putXyz(bytes, blueXyz, 0.1430664f, 0.0606084f, 0.7141733f);
        putIdentityCurve(bytes, redTrc);
        putIdentityCurve(bytes, greenTrc);
        putIdentityCurve(bytes, blueTrc);
        putSignature(bytes, a2b0, "mft1");
        bytes[a2b0 + 8] = 3;
        bytes[a2b0 + 9] = 3;
        bytes[a2b0 + 10] = (byte) grid;
        int cursor = a2b0 + 48;
        cursor = writePlanar8Identity(bytes, cursor);
        for (int red = 0; red < grid; red++) {
            for (int green = 0; green < grid; green++) {
                for (int blue = 0; blue < grid; blue++) {
                    boolean uniqueCell = red == 1 && green == 0 && blue == 0;
                    bytes[cursor++] = (byte) (uniqueCell ? 51 : 0);
                    bytes[cursor++] = 0;
                    bytes[cursor++] = 0;
                }
            }
        }
        writePlanar8Identity(bytes, cursor);
        return bytes;
    }

    /// Builds an `mft1` AToB0 tag whose red input table maps 1 to 128/255.
    private static byte[] matrixPlusA2b0Mft1InputHalf() {
        byte[] bytes = matrixPlusA2b0Mft1();
        int tagCount = 7;
        int table = 132 + tagCount * 12;
        int a2b0 = table + 3 * 20 + 3 * 12;
        int redInput = a2b0 + 48;
        bytes[redInput + 255] = (byte) 128;
        return bytes;
    }

    /// Builds an `mft1` AToB0 tag whose 3×3 matrix doubles X.
    private static byte[] matrixPlusA2b0Mft1Matrix() {
        byte[] bytes = matrixPlusA2b0Mft1();
        int tagCount = 7;
        int table = 132 + tagCount * 12;
        int a2b0 = table + 3 * 20 + 3 * 12;
        putS15(bytes, a2b0 + 12, 2.0f);
        putS15(bytes, a2b0 + 28, 1.0f);
        putS15(bytes, a2b0 + 44, 1.0f);
        return bytes;
    }

    /// Builds an `mft2` AToB0 tag whose 3×3 matrix doubles X.
    private static byte[] matrixPlusA2b0Mft2Matrix() {
        byte[] bytes = matrixPlusA2b0();
        int tagCount = 7;
        int table = 132 + tagCount * 12;
        int a2b0 = table + 3 * 20 + 3 * 12;
        putS15(bytes, a2b0 + 12, 2.0f);
        putS15(bytes, a2b0 + 28, 1.0f);
        putS15(bytes, a2b0 + 44, 1.0f);
        return bytes;
    }

    /// Builds the matrix profile plus a 2³ `mft2` AToB0 tag.
    private static byte[] matrixPlusA2b0() {
        return matrixPlusTaggedLut(0x4132_4230, 13107);
    }

    /// Builds the matrix profile plus a 2³ `mft2` BToA0 tag.
    private static byte[] matrixPlusB2a0() {
        return matrixPlusTaggedLut(0x4232_4130, 65535);
    }

    /// Builds the matrix profile plus a 2³ `mft2` tag with `signature`.
    private static byte[] matrixPlusTaggedLut(int signature, int uniqueRed) {
        byte[] base = minimalSrgbMatrixProfile();
        int grid = 2;
        int inputTable = 256 * 3;
        int clutValues = grid * grid * grid * 3;
        int outputTable = 256 * 3;
        int mft2Size = 48 + (inputTable + clutValues + outputTable) * 2;
        int tagCount = 7;
        int table = 132 + tagCount * 12;
        int xyzSize = 20;
        int curveSize = 12;
        int size = table + 3 * xyzSize + 3 * curveSize + mft2Size;
        byte[] bytes = new byte[size];
        System.arraycopy(base, 0, bytes, 0, 128);
        putU32(bytes, 0, size);
        putU32(bytes, 128, tagCount);
        int redXyz = table;
        int greenXyz = redXyz + xyzSize;
        int blueXyz = greenXyz + xyzSize;
        int redTrc = blueXyz + xyzSize;
        int greenTrc = redTrc + curveSize;
        int blueTrc = greenTrc + curveSize;
        int a2b0 = blueTrc + curveSize;
        putTag(bytes, 0, 0x7258_595A, redXyz, xyzSize);
        putTag(bytes, 1, 0x6758_595A, greenXyz, xyzSize);
        putTag(bytes, 2, 0x6258_595A, blueXyz, xyzSize);
        putTag(bytes, 3, 0x7254_5243, redTrc, curveSize);
        putTag(bytes, 4, 0x6754_5243, greenTrc, curveSize);
        putTag(bytes, 5, 0x6254_5243, blueTrc, curveSize);
        putTag(bytes, 6, signature, a2b0, mft2Size);
        putXyz(bytes, redXyz, 0.4360657f, 0.2224884f, 0.0139160f);
        putXyz(bytes, greenXyz, 0.3851477f, 0.7168732f, 0.0971045f);
        putXyz(bytes, blueXyz, 0.1430664f, 0.0606084f, 0.7141733f);
        putIdentityCurve(bytes, redTrc);
        putIdentityCurve(bytes, greenTrc);
        putIdentityCurve(bytes, blueTrc);
        putSignature(bytes, a2b0, "mft2");
        bytes[a2b0 + 8] = 3;
        bytes[a2b0 + 9] = 3;
        bytes[a2b0 + 10] = (byte) grid;
        int cursor = a2b0 + 48;
        cursor = writePlanar16Identity(bytes, cursor);
        for (int red = 0; red < grid; red++) {
            for (int green = 0; green < grid; green++) {
                for (int blue = 0; blue < grid; blue++) {
                    boolean uniqueCell = red == 1 && green == 0 && blue == 0;
                    putU16(bytes, cursor, uniqueCell ? uniqueRed : 0);
                    putU16(bytes, cursor + 2, 0);
                    putU16(bytes, cursor + 4, 0);
                    cursor += 6;
                }
            }
        }
        writePlanar16Identity(bytes, cursor);
        return bytes;
    }

    /// Builds an `mft2` AToB0 tag whose red output table maps the unique cell to 1.
    private static byte[] matrixPlusA2b0Mft2OutputOne() {
        byte[] bytes = matrixPlusA2b0();
        int tagCount = 7;
        int table = 132 + tagCount * 12;
        int a2b0 = table + 3 * 20 + 3 * 12;
        int inputTable = 256 * 3;
        int clutValues = 2 * 2 * 2 * 3;
        int redOutput = a2b0 + 48 + inputTable * 2 + clutValues * 2;
        putU16(bytes, redOutput + 51 * 2, 65535);
        return bytes;
    }

    /// Writes three planar 8-bit identity tables.
    ///
    /// @param bytes the profile bytes
    /// @param cursor the first table byte
    /// @return the cursor after the tables
    private static int writePlanar8Identity(byte[] bytes, int cursor) {
        int offset = cursor;
        for (int channel = 0; channel < 3; channel++) {
            for (int index = 0; index < 256; index++) {
                bytes[offset++] = (byte) index;
            }
        }
        return offset;
    }

    /// Writes three planar 16-bit identity tables.
    ///
    /// @param bytes the profile bytes
    /// @param cursor the first table byte
    /// @return the cursor after the tables
    private static int writePlanar16Identity(byte[] bytes, int cursor) {
        int offset = cursor;
        for (int channel = 0; channel < 3; channel++) {
            for (int index = 0; index < 256; index++) {
                putU16(bytes, offset, index * 257);
                offset += 2;
            }
        }
        return offset;
    }

    /// Writes a big-endian unsigned 16-bit integer.
    private static void putU16(byte[] bytes, int offset, int value) {
        bytes[offset] = (byte) (value >>> 8);
        bytes[offset + 1] = (byte) value;
    }

    /// Builds a compact ICC v2 RGB matrix/TRC profile.
    ///
    /// @return the profile bytes
    static byte[] minimalSrgbMatrixProfile() {
        int tagCount = 6;
        int table = 132 + tagCount * 12;
        int xyzSize = 20;
        int curveSize = 12;
        int size = table + 3 * xyzSize + 3 * curveSize;
        byte[] bytes = new byte[size];
        putU32(bytes, 0, size);
        putU32(bytes, 8, 0x0240_0000);
        putSignature(bytes, 12, "mntr");
        putSignature(bytes, 16, "RGB ");
        putSignature(bytes, 20, "XYZ ");
        putSignature(bytes, 36, "acsp");
        putS15(bytes, 68, 0.9642f);
        putS15(bytes, 72, 1.0f);
        putS15(bytes, 76, 0.8249f);
        putU32(bytes, 128, tagCount);
        int redXyz = table;
        int greenXyz = redXyz + xyzSize;
        int blueXyz = greenXyz + xyzSize;
        int redTrc = blueXyz + xyzSize;
        int greenTrc = redTrc + curveSize;
        int blueTrc = greenTrc + curveSize;
        putTag(bytes, 0, 0x7258_595A, redXyz, xyzSize);
        putTag(bytes, 1, 0x6758_595A, greenXyz, xyzSize);
        putTag(bytes, 2, 0x6258_595A, blueXyz, xyzSize);
        putTag(bytes, 3, 0x7254_5243, redTrc, curveSize);
        putTag(bytes, 4, 0x6754_5243, greenTrc, curveSize);
        putTag(bytes, 5, 0x6254_5243, blueTrc, curveSize);
        putXyz(bytes, redXyz, 0.4360657f, 0.2224884f, 0.0139160f);
        putXyz(bytes, greenXyz, 0.3851477f, 0.7168732f, 0.0971045f);
        putXyz(bytes, blueXyz, 0.1430664f, 0.0606084f, 0.7141733f);
        putIdentityCurve(bytes, redTrc);
        putIdentityCurve(bytes, greenTrc);
        putIdentityCurve(bytes, blueTrc);
        return bytes;
    }

    /// Builds a matrix/TRC profile that also carries one D50-white `ncl2` named color.
    private static byte[] matrixWithNamedColor() {
        int tagCount = 7;
        int table = 132 + tagCount * 12;
        int xyzSize = 20;
        int curveSize = 12;
        int ncl2Size = 124;
        int size = table + 3 * xyzSize + 3 * curveSize + ncl2Size;
        byte[] bytes = new byte[size];
        putU32(bytes, 0, size);
        putU32(bytes, 8, 0x0240_0000);
        putSignature(bytes, 12, "mntr");
        putSignature(bytes, 16, "RGB ");
        putSignature(bytes, 20, "XYZ ");
        putSignature(bytes, 36, "acsp");
        putS15(bytes, 68, 0.9642f);
        putS15(bytes, 72, 1.0f);
        putS15(bytes, 76, 0.8249f);
        putU32(bytes, 128, tagCount);
        int redXyz = table;
        int greenXyz = redXyz + xyzSize;
        int blueXyz = greenXyz + xyzSize;
        int redTrc = blueXyz + xyzSize;
        int greenTrc = redTrc + curveSize;
        int blueTrc = greenTrc + curveSize;
        int ncl2 = blueTrc + curveSize;
        putTag(bytes, 0, 0x7258_595A, redXyz, xyzSize);
        putTag(bytes, 1, 0x6758_595A, greenXyz, xyzSize);
        putTag(bytes, 2, 0x6258_595A, blueXyz, xyzSize);
        putTag(bytes, 3, 0x7254_5243, redTrc, curveSize);
        putTag(bytes, 4, 0x6754_5243, greenTrc, curveSize);
        putTag(bytes, 5, 0x6254_5243, blueTrc, curveSize);
        putTag(bytes, 6, 0x6E63_6C32, ncl2, ncl2Size);
        putXyz(bytes, redXyz, 0.4360657f, 0.2224884f, 0.0139160f);
        putXyz(bytes, greenXyz, 0.3851477f, 0.7168732f, 0.0971045f);
        putXyz(bytes, blueXyz, 0.1430664f, 0.0606084f, 0.7141733f);
        putIdentityCurve(bytes, redTrc);
        putIdentityCurve(bytes, greenTrc);
        putIdentityCurve(bytes, blueTrc);
        putSignature(bytes, ncl2, "ncl2");
        putU32(bytes, ncl2 + 12, 1);
        putU32(bytes, ncl2 + 16, 0);
        putAscii32(bytes, ncl2 + 20, "Ink");
        putAscii32(bytes, ncl2 + 52, "");
        putAscii32(bytes, ncl2 + 84, "SpotRed");
        putU16(bytes, ncl2 + 116, Math.round(0.9642f * 32768.0f));
        putU16(bytes, ncl2 + 118, 32768);
        putU16(bytes, ncl2 + 120, Math.round(0.8249f * 32768.0f));
        return bytes;
    }

    /// Builds a matrix/TRC profile with one `ncl2` color that also stores three device coordinates.
    private static byte[] matrixWithNamedColorDevice() {
        int tagCount = 7;
        int table = 132 + tagCount * 12;
        int xyzSize = 20;
        int curveSize = 12;
        int ncl2Size = 132;
        int size = table + 3 * xyzSize + 3 * curveSize + ncl2Size;
        byte[] bytes = new byte[size];
        putU32(bytes, 0, size);
        putU32(bytes, 8, 0x0240_0000);
        putSignature(bytes, 12, "mntr");
        putSignature(bytes, 16, "RGB ");
        putSignature(bytes, 20, "XYZ ");
        putSignature(bytes, 36, "acsp");
        putS15(bytes, 68, 0.9642f);
        putS15(bytes, 72, 1.0f);
        putS15(bytes, 76, 0.8249f);
        putU32(bytes, 128, tagCount);
        int redXyz = table;
        int greenXyz = redXyz + xyzSize;
        int blueXyz = greenXyz + xyzSize;
        int redTrc = blueXyz + xyzSize;
        int greenTrc = redTrc + curveSize;
        int blueTrc = greenTrc + curveSize;
        int ncl2 = blueTrc + curveSize;
        putTag(bytes, 0, 0x7258_595A, redXyz, xyzSize);
        putTag(bytes, 1, 0x6758_595A, greenXyz, xyzSize);
        putTag(bytes, 2, 0x6258_595A, blueXyz, xyzSize);
        putTag(bytes, 3, 0x7254_5243, redTrc, curveSize);
        putTag(bytes, 4, 0x6754_5243, greenTrc, curveSize);
        putTag(bytes, 5, 0x6254_5243, blueTrc, curveSize);
        putTag(bytes, 6, 0x6E63_6C32, ncl2, ncl2Size);
        putXyz(bytes, redXyz, 0.4360657f, 0.2224884f, 0.0139160f);
        putXyz(bytes, greenXyz, 0.3851477f, 0.7168732f, 0.0971045f);
        putXyz(bytes, blueXyz, 0.1430664f, 0.0606084f, 0.7141733f);
        putIdentityCurve(bytes, redTrc);
        putIdentityCurve(bytes, greenTrc);
        putIdentityCurve(bytes, blueTrc);
        putSignature(bytes, ncl2, "ncl2");
        putU32(bytes, ncl2 + 12, 1);
        putU32(bytes, ncl2 + 16, 3);
        putAscii32(bytes, ncl2 + 20, "Ink");
        putAscii32(bytes, ncl2 + 52, "");
        putAscii32(bytes, ncl2 + 84, "SpotRed");
        putU16(bytes, ncl2 + 116, Math.round(0.9642f * 32768.0f));
        putU16(bytes, ncl2 + 118, 32768);
        putU16(bytes, ncl2 + 120, Math.round(0.8249f * 32768.0f));
        putU16(bytes, ncl2 + 122, 65535);
        putU16(bytes, ncl2 + 124, 0);
        putU16(bytes, ncl2 + 126, 32768);
        return bytes;
    }

    /// Builds a matrix/TRC profile that carries one D50-white v2 `ncl ` named color.
    private static byte[] matrixWithNamedColorV2() {
        int tagCount = 7;
        int table = 132 + tagCount * 12;
        int xyzSize = 20;
        int curveSize = 12;
        int nclSize = 56;
        int size = table + 3 * xyzSize + 3 * curveSize + nclSize;
        byte[] bytes = new byte[size];
        putU32(bytes, 0, size);
        putU32(bytes, 8, 0x0240_0000);
        putSignature(bytes, 12, "mntr");
        putSignature(bytes, 16, "RGB ");
        putSignature(bytes, 20, "XYZ ");
        putSignature(bytes, 36, "acsp");
        putS15(bytes, 68, 0.9642f);
        putS15(bytes, 72, 1.0f);
        putS15(bytes, 76, 0.8249f);
        putU32(bytes, 128, tagCount);
        int redXyz = table;
        int greenXyz = redXyz + xyzSize;
        int blueXyz = greenXyz + xyzSize;
        int redTrc = blueXyz + xyzSize;
        int greenTrc = redTrc + curveSize;
        int blueTrc = greenTrc + curveSize;
        int ncl = blueTrc + curveSize;
        putTag(bytes, 0, 0x7258_595A, redXyz, xyzSize);
        putTag(bytes, 1, 0x6758_595A, greenXyz, xyzSize);
        putTag(bytes, 2, 0x6258_595A, blueXyz, xyzSize);
        putTag(bytes, 3, 0x7254_5243, redTrc, curveSize);
        putTag(bytes, 4, 0x6754_5243, greenTrc, curveSize);
        putTag(bytes, 5, 0x6254_5243, blueTrc, curveSize);
        putTag(bytes, 6, 0x6E63_6C20, ncl, nclSize);
        putXyz(bytes, redXyz, 0.4360657f, 0.2224884f, 0.0139160f);
        putXyz(bytes, greenXyz, 0.3851477f, 0.7168732f, 0.0971045f);
        putXyz(bytes, blueXyz, 0.1430664f, 0.0606084f, 0.7141733f);
        putIdentityCurve(bytes, redTrc);
        putIdentityCurve(bytes, greenTrc);
        putIdentityCurve(bytes, blueTrc);
        putSignature(bytes, ncl, "ncl ");
        putU32(bytes, ncl + 12, 1);
        putAscii32(bytes, ncl + 16, "Patch");
        putU16(bytes, ncl + 48, Math.round(0.9642f * 32768.0f));
        putU16(bytes, ncl + 50, 32768);
        putU16(bytes, ncl + 52, Math.round(0.8249f * 32768.0f));
        return bytes;
    }

    /// Builds a matrix/TRC profile with a diagonal `chad` that scales PCS X.
    private static byte[] matrixWithChadScaleX(float scaleX) {
        int tagCount = 7;
        int table = 132 + tagCount * 12;
        int xyzSize = 20;
        int curveSize = 12;
        int chadSize = 44;
        int size = table + 3 * xyzSize + 3 * curveSize + chadSize;
        byte[] bytes = new byte[size];
        putU32(bytes, 0, size);
        putU32(bytes, 8, 0x0440_0000);
        putSignature(bytes, 12, "mntr");
        putSignature(bytes, 16, "RGB ");
        putSignature(bytes, 20, "XYZ ");
        putSignature(bytes, 36, "acsp");
        putS15(bytes, 68, 0.9642f);
        putS15(bytes, 72, 1.0f);
        putS15(bytes, 76, 0.8249f);
        putU32(bytes, 128, tagCount);
        int redXyz = table;
        int greenXyz = redXyz + xyzSize;
        int blueXyz = greenXyz + xyzSize;
        int redTrc = blueXyz + xyzSize;
        int greenTrc = redTrc + curveSize;
        int blueTrc = greenTrc + curveSize;
        int chad = blueTrc + curveSize;
        putTag(bytes, 0, 0x7258_595A, redXyz, xyzSize);
        putTag(bytes, 1, 0x6758_595A, greenXyz, xyzSize);
        putTag(bytes, 2, 0x6258_595A, blueXyz, xyzSize);
        putTag(bytes, 3, 0x7254_5243, redTrc, curveSize);
        putTag(bytes, 4, 0x6754_5243, greenTrc, curveSize);
        putTag(bytes, 5, 0x6254_5243, blueTrc, curveSize);
        putTag(bytes, 6, 0x6368_6164, chad, chadSize);
        putXyz(bytes, redXyz, 0.4360657f, 0.2224884f, 0.0139160f);
        putXyz(bytes, greenXyz, 0.3851477f, 0.7168732f, 0.0971045f);
        putXyz(bytes, blueXyz, 0.1430664f, 0.0606084f, 0.7141733f);
        putIdentityCurve(bytes, redTrc);
        putIdentityCurve(bytes, greenTrc);
        putIdentityCurve(bytes, blueTrc);
        putSignature(bytes, chad, "sf32");
        putS15(bytes, chad + 8, scaleX);
        putS15(bytes, chad + 24, 1.0f);
        putS15(bytes, chad + 40, 1.0f);
        return bytes;
    }

    /// Builds a matrix/TRC profile whose `wtpt` is D65 while the header illuminant stays D50.
    private static byte[] matrixWithMediaWhiteD65() {
        int tagCount = 7;
        int table = 132 + tagCount * 12;
        int xyzSize = 20;
        int curveSize = 12;
        int size = table + 4 * xyzSize + 3 * curveSize;
        byte[] bytes = new byte[size];
        putU32(bytes, 0, size);
        putU32(bytes, 8, 0x0440_0000);
        putSignature(bytes, 12, "mntr");
        putSignature(bytes, 16, "RGB ");
        putSignature(bytes, 20, "XYZ ");
        putSignature(bytes, 36, "acsp");
        putS15(bytes, 68, 0.9642f);
        putS15(bytes, 72, 1.0f);
        putS15(bytes, 76, 0.8249f);
        putU32(bytes, 128, tagCount);
        int redXyz = table;
        int greenXyz = redXyz + xyzSize;
        int blueXyz = greenXyz + xyzSize;
        int redTrc = blueXyz + xyzSize;
        int greenTrc = redTrc + curveSize;
        int blueTrc = greenTrc + curveSize;
        int wtpt = blueTrc + curveSize;
        putTag(bytes, 0, 0x7258_595A, redXyz, xyzSize);
        putTag(bytes, 1, 0x6758_595A, greenXyz, xyzSize);
        putTag(bytes, 2, 0x6258_595A, blueXyz, xyzSize);
        putTag(bytes, 3, 0x7254_5243, redTrc, curveSize);
        putTag(bytes, 4, 0x6754_5243, greenTrc, curveSize);
        putTag(bytes, 5, 0x6254_5243, blueTrc, curveSize);
        putTag(bytes, 6, 0x7774_7074, wtpt, xyzSize);
        putXyz(bytes, redXyz, 0.4360657f, 0.2224884f, 0.0139160f);
        putXyz(bytes, greenXyz, 0.3851477f, 0.7168732f, 0.0971045f);
        putXyz(bytes, blueXyz, 0.1430664f, 0.0606084f, 0.7141733f);
        putIdentityCurve(bytes, redTrc);
        putIdentityCurve(bytes, greenTrc);
        putIdentityCurve(bytes, blueTrc);
        putXyz(bytes, wtpt, 0.95047f, 1.0f, 1.08883f);
        return bytes;
    }

    /// Builds a matrix/TRC profile whose `bkpt` raises the media black Y.
    private static byte[] matrixWithMediaBlack() {
        int tagCount = 7;
        int table = 132 + tagCount * 12;
        int xyzSize = 20;
        int curveSize = 12;
        int size = table + 4 * xyzSize + 3 * curveSize;
        byte[] bytes = new byte[size];
        putU32(bytes, 0, size);
        putU32(bytes, 8, 0x0240_0000);
        putSignature(bytes, 12, "mntr");
        putSignature(bytes, 16, "RGB ");
        putSignature(bytes, 20, "XYZ ");
        putSignature(bytes, 36, "acsp");
        putS15(bytes, 68, 0.9642f);
        putS15(bytes, 72, 1.0f);
        putS15(bytes, 76, 0.8249f);
        putU32(bytes, 128, tagCount);
        int redXyz = table;
        int greenXyz = redXyz + xyzSize;
        int blueXyz = greenXyz + xyzSize;
        int redTrc = blueXyz + xyzSize;
        int greenTrc = redTrc + curveSize;
        int blueTrc = greenTrc + curveSize;
        int bkpt = blueTrc + curveSize;
        putTag(bytes, 0, 0x7258_595A, redXyz, xyzSize);
        putTag(bytes, 1, 0x6758_595A, greenXyz, xyzSize);
        putTag(bytes, 2, 0x6258_595A, blueXyz, xyzSize);
        putTag(bytes, 3, 0x7254_5243, redTrc, curveSize);
        putTag(bytes, 4, 0x6754_5243, greenTrc, curveSize);
        putTag(bytes, 5, 0x6254_5243, blueTrc, curveSize);
        putTag(bytes, 6, 0x626B_7074, bkpt, xyzSize);
        putXyz(bytes, redXyz, 0.4360657f, 0.2224884f, 0.0139160f);
        putXyz(bytes, greenXyz, 0.3851477f, 0.7168732f, 0.0971045f);
        putXyz(bytes, blueXyz, 0.1430664f, 0.0606084f, 0.7141733f);
        putIdentityCurve(bytes, redTrc);
        putIdentityCurve(bytes, greenTrc);
        putIdentityCurve(bytes, blueTrc);
        putXyz(bytes, bkpt, 0.0f, 0.05f, 0.0f);
        return bytes;
    }

    /// Builds a matrix/TRC profile whose `lumi` Y is `nits`.
    private static byte[] matrixWithLuminance(float nits) {
        int tagCount = 7;
        int table = 132 + tagCount * 12;
        int xyzSize = 20;
        int curveSize = 12;
        int size = table + 4 * xyzSize + 3 * curveSize;
        byte[] bytes = new byte[size];
        putU32(bytes, 0, size);
        putU32(bytes, 8, 0x0240_0000);
        putSignature(bytes, 12, "mntr");
        putSignature(bytes, 16, "RGB ");
        putSignature(bytes, 20, "XYZ ");
        putSignature(bytes, 36, "acsp");
        putS15(bytes, 68, 0.9642f);
        putS15(bytes, 72, 1.0f);
        putS15(bytes, 76, 0.8249f);
        putU32(bytes, 128, tagCount);
        int redXyz = table;
        int greenXyz = redXyz + xyzSize;
        int blueXyz = greenXyz + xyzSize;
        int redTrc = blueXyz + xyzSize;
        int greenTrc = redTrc + curveSize;
        int blueTrc = greenTrc + curveSize;
        int lumi = blueTrc + curveSize;
        putTag(bytes, 0, 0x7258_595A, redXyz, xyzSize);
        putTag(bytes, 1, 0x6758_595A, greenXyz, xyzSize);
        putTag(bytes, 2, 0x6258_595A, blueXyz, xyzSize);
        putTag(bytes, 3, 0x7254_5243, redTrc, curveSize);
        putTag(bytes, 4, 0x6754_5243, greenTrc, curveSize);
        putTag(bytes, 5, 0x6254_5243, blueTrc, curveSize);
        putTag(bytes, 6, 0x6C75_6D69, lumi, xyzSize);
        putXyz(bytes, redXyz, 0.4360657f, 0.2224884f, 0.0139160f);
        putXyz(bytes, greenXyz, 0.3851477f, 0.7168732f, 0.0971045f);
        putXyz(bytes, blueXyz, 0.1430664f, 0.0606084f, 0.7141733f);
        putIdentityCurve(bytes, redTrc);
        putIdentityCurve(bytes, greenTrc);
        putIdentityCurve(bytes, blueTrc);
        putXyz(bytes, lumi, 0.0f, nits, 0.0f);
        return bytes;
    }

    /// Builds a matrix/TRC profile with `view`, `desc`, `vued`, and `cprt` tags.
    private static byte[] matrixWithViewingConditions() {
        int tagCount = 15;
        int table = 132 + tagCount * 12;
        int xyzSize = 20;
        int curveSize = 12;
        int viewSize = 36;
        int descSize = 20;
        int vuedSize = 20;
        int cprtSize = 12;
        int techSize = 12;
        int dmndSize = 16;
        int dmddSize = 20;
        int caltSize = 20;
        int measSize = 36;
        int size = table + 3 * xyzSize + 3 * curveSize + viewSize + descSize + vuedSize + cprtSize
                + techSize + dmndSize + dmddSize + caltSize + measSize;
        byte[] bytes = new byte[size];
        putU32(bytes, 0, size);
        putU32(bytes, 8, 0x0240_0000);
        putSignature(bytes, 12, "mntr");
        putSignature(bytes, 16, "RGB ");
        putSignature(bytes, 20, "XYZ ");
        putSignature(bytes, 36, "acsp");
        putS15(bytes, 68, 0.9642f);
        putS15(bytes, 72, 1.0f);
        putS15(bytes, 76, 0.8249f);
        putU32(bytes, 128, tagCount);
        int redXyz = table;
        int greenXyz = redXyz + xyzSize;
        int blueXyz = greenXyz + xyzSize;
        int redTrc = blueXyz + xyzSize;
        int greenTrc = redTrc + curveSize;
        int blueTrc = greenTrc + curveSize;
        int view = blueTrc + curveSize;
        int desc = view + viewSize;
        int vued = desc + descSize;
        int cprt = vued + vuedSize;
        int tech = cprt + cprtSize;
        int dmnd = tech + techSize;
        int dmdd = dmnd + dmndSize;
        int calt = dmdd + dmddSize;
        int meas = calt + caltSize;
        putTag(bytes, 0, 0x7258_595A, redXyz, xyzSize);
        putTag(bytes, 1, 0x6758_595A, greenXyz, xyzSize);
        putTag(bytes, 2, 0x6258_595A, blueXyz, xyzSize);
        putTag(bytes, 3, 0x7254_5243, redTrc, curveSize);
        putTag(bytes, 4, 0x6754_5243, greenTrc, curveSize);
        putTag(bytes, 5, 0x6254_5243, blueTrc, curveSize);
        putTag(bytes, 6, 0x7669_6577, view, viewSize);
        putTag(bytes, 7, 0x6465_7363, desc, descSize);
        putTag(bytes, 8, 0x7675_6564, vued, vuedSize);
        putTag(bytes, 9, 0x6370_7274, cprt, cprtSize);
        putTag(bytes, 10, 0x7465_6368, tech, techSize);
        putTag(bytes, 11, 0x646D_6E64, dmnd, dmndSize);
        putTag(bytes, 12, 0x646D_6464, dmdd, dmddSize);
        putTag(bytes, 13, 0x6361_6C74, calt, caltSize);
        putTag(bytes, 14, 0x6D65_6173, meas, measSize);
        putXyz(bytes, redXyz, 0.4360657f, 0.2224884f, 0.0139160f);
        putXyz(bytes, greenXyz, 0.3851477f, 0.7168732f, 0.0971045f);
        putXyz(bytes, blueXyz, 0.1430664f, 0.0606084f, 0.7141733f);
        putIdentityCurve(bytes, redTrc);
        putIdentityCurve(bytes, greenTrc);
        putIdentityCurve(bytes, blueTrc);
        putSignature(bytes, view, "view");
        putS15(bytes, view + 8, 76.0376f);
        putS15(bytes, view + 12, 80.0f);
        putS15(bytes, view + 16, 87.1064f);
        putS15(bytes, view + 20, 15.2075f);
        putS15(bytes, view + 24, 16.0f);
        putS15(bytes, view + 28, 17.4213f);
        putU32(bytes, view + 32, IccViewingConditions.ILLUMINANT_D65);
        putSignature(bytes, desc, "desc");
        putU32(bytes, desc + 8, 5);
        putAscii32(bytes, desc + 12, "sRGB");
        putSignature(bytes, vued, "desc");
        putU32(bytes, vued + 8, 4);
        putAscii32(bytes, vued + 12, "D65");
        putSignature(bytes, cprt, "text");
        putAscii32(bytes, cprt + 8, "ICC");
        putSignature(bytes, tech, "sig ");
        putSignature(bytes, tech + 8, "CRT ");
        putSignature(bytes, dmnd, "desc");
        putU32(bytes, dmnd + 8, 4);
        putAscii32(bytes, dmnd + 12, "IEC");
        putSignature(bytes, dmdd, "desc");
        putU32(bytes, dmdd + 8, 5);
        putAscii32(bytes, dmdd + 12, "sRGB");
        putSignature(bytes, calt, "dtim");
        putU16(bytes, calt + 8, 2026);
        putU16(bytes, calt + 10, 8);
        putU16(bytes, calt + 12, 20);
        putU16(bytes, calt + 14, 0);
        putU16(bytes, calt + 16, 0);
        putU16(bytes, calt + 18, 0);
        putSignature(bytes, meas, "meas");
        putU32(bytes, meas + 8, IccMeasurement.OBSERVER_CIE_1931);
        putS15(bytes, meas + 12, 0.0f);
        putS15(bytes, meas + 16, 0.0f);
        putS15(bytes, meas + 20, 0.0f);
        putU32(bytes, meas + 24, IccMeasurement.GEOMETRY_0_45);
        putS15(bytes, meas + 28, 0.0f);
        putU32(bytes, meas + 32, IccViewingConditions.ILLUMINANT_D65);
        return bytes;
    }

    /// Builds the matrix profile plus a 2³ 3×1 `mft1` `gamt` tag.
    private static byte[] matrixPlusGamtMft1() {
        byte[] base = minimalSrgbMatrixProfile();
        int grid = 2;
        int inputTable = 256 * 3;
        int clutValues = grid * grid * grid;
        int outputTable = 256;
        int gamtSize = 48 + inputTable + clutValues + outputTable;
        int tagCount = 7;
        int table = 132 + tagCount * 12;
        int xyzSize = 20;
        int curveSize = 12;
        int size = table + 3 * xyzSize + 3 * curveSize + gamtSize;
        byte[] bytes = new byte[size];
        System.arraycopy(base, 0, bytes, 0, 128);
        putU32(bytes, 0, size);
        putU32(bytes, 128, tagCount);
        int redXyz = table;
        int greenXyz = redXyz + xyzSize;
        int blueXyz = greenXyz + xyzSize;
        int redTrc = blueXyz + xyzSize;
        int greenTrc = redTrc + curveSize;
        int blueTrc = greenTrc + curveSize;
        int gamt = blueTrc + curveSize;
        putTag(bytes, 0, 0x7258_595A, redXyz, xyzSize);
        putTag(bytes, 1, 0x6758_595A, greenXyz, xyzSize);
        putTag(bytes, 2, 0x6258_595A, blueXyz, xyzSize);
        putTag(bytes, 3, 0x7254_5243, redTrc, curveSize);
        putTag(bytes, 4, 0x6754_5243, greenTrc, curveSize);
        putTag(bytes, 5, 0x6254_5243, blueTrc, curveSize);
        putTag(bytes, 6, IccGamut.SIGNATURE, gamt, gamtSize);
        putXyz(bytes, redXyz, 0.4360657f, 0.2224884f, 0.0139160f);
        putXyz(bytes, greenXyz, 0.3851477f, 0.7168732f, 0.0971045f);
        putXyz(bytes, blueXyz, 0.1430664f, 0.0606084f, 0.7141733f);
        putIdentityCurve(bytes, redTrc);
        putIdentityCurve(bytes, greenTrc);
        putIdentityCurve(bytes, blueTrc);
        putSignature(bytes, gamt, "mft1");
        bytes[gamt + 8] = 3;
        bytes[gamt + 9] = 1;
        bytes[gamt + 10] = (byte) grid;
        int cursor = gamt + 48;
        cursor = writePlanar8Identity(bytes, cursor);
        for (int red = 0; red < grid; red++) {
            for (int green = 0; green < grid; green++) {
                for (int blue = 0; blue < grid; blue++) {
                    boolean uniqueCell = red == 1 && green == 0 && blue == 0;
                    bytes[cursor++] = (byte) (uniqueCell ? 255 : 0);
                }
            }
        }
        for (int index = 0; index < 256; index++) {
            bytes[cursor++] = (byte) index;
        }
        return bytes;
    }

    /// Builds a `gamt` `mft1` tag whose 3×3 matrix doubles X.
    private static byte[] matrixPlusGamtMft1Matrix() {
        byte[] bytes = matrixPlusGamtMft1();
        int tagCount = 7;
        int table = 132 + tagCount * 12;
        int gamt = table + 3 * 20 + 3 * 12;
        putS15(bytes, gamt + 12, 2.0f);
        putS15(bytes, gamt + 28, 1.0f);
        putS15(bytes, gamt + 44, 1.0f);
        return bytes;
    }

    /// Builds the matrix profile plus a 2³ 3×1 `mft2` `gamt` tag.
    private static byte[] matrixPlusGamtMft2() {
        byte[] base = minimalSrgbMatrixProfile();
        int grid = 2;
        int inputEntries = 256;
        int outputEntries = 256;
        int inputTable = inputEntries * 3;
        int clutValues = grid * grid * grid;
        int gamtSize = 52 + (inputTable + clutValues + outputEntries) * 2;
        int tagCount = 7;
        int table = 132 + tagCount * 12;
        int xyzSize = 20;
        int curveSize = 12;
        int size = table + 3 * xyzSize + 3 * curveSize + gamtSize;
        byte[] bytes = new byte[size];
        System.arraycopy(base, 0, bytes, 0, 128);
        putU32(bytes, 0, size);
        putU32(bytes, 128, tagCount);
        int redXyz = table;
        int greenXyz = redXyz + xyzSize;
        int blueXyz = greenXyz + xyzSize;
        int redTrc = blueXyz + xyzSize;
        int greenTrc = redTrc + curveSize;
        int blueTrc = greenTrc + curveSize;
        int gamt = blueTrc + curveSize;
        putTag(bytes, 0, 0x7258_595A, redXyz, xyzSize);
        putTag(bytes, 1, 0x6758_595A, greenXyz, xyzSize);
        putTag(bytes, 2, 0x6258_595A, blueXyz, xyzSize);
        putTag(bytes, 3, 0x7254_5243, redTrc, curveSize);
        putTag(bytes, 4, 0x6754_5243, greenTrc, curveSize);
        putTag(bytes, 5, 0x6254_5243, blueTrc, curveSize);
        putTag(bytes, 6, IccGamut.SIGNATURE, gamt, gamtSize);
        putXyz(bytes, redXyz, 0.4360657f, 0.2224884f, 0.0139160f);
        putXyz(bytes, greenXyz, 0.3851477f, 0.7168732f, 0.0971045f);
        putXyz(bytes, blueXyz, 0.1430664f, 0.0606084f, 0.7141733f);
        putIdentityCurve(bytes, redTrc);
        putIdentityCurve(bytes, greenTrc);
        putIdentityCurve(bytes, blueTrc);
        putSignature(bytes, gamt, "mft2");
        bytes[gamt + 8] = 3;
        bytes[gamt + 9] = 1;
        bytes[gamt + 10] = (byte) grid;
        putU16(bytes, gamt + 48, inputEntries);
        putU16(bytes, gamt + 50, outputEntries);
        int cursor = gamt + 52;
        cursor = writePlanar16Identity(bytes, cursor);
        for (int red = 0; red < grid; red++) {
            for (int green = 0; green < grid; green++) {
                for (int blue = 0; blue < grid; blue++) {
                    boolean uniqueCell = red == 1 && green == 0 && blue == 0;
                    putU16(bytes, cursor, uniqueCell ? 65535 : 0);
                    cursor += 2;
                }
            }
        }
        for (int index = 0; index < outputEntries; index++) {
            putU16(bytes, cursor, index * 257);
            cursor += 2;
        }
        return bytes;
    }

    /// Builds the matrix profile plus a CLUT-only 3×1 `mBA ` `gamt` tag.
    private static byte[] matrixPlusGamtMba() {
        byte[] base = minimalSrgbMatrixProfile();
        int grid = 2;
        int clutValues = grid * grid * grid;
        int gamtSize = 32 + 20 + clutValues;
        int tagCount = 7;
        int table = 132 + tagCount * 12;
        int xyzSize = 20;
        int curveSize = 12;
        int size = table + 3 * xyzSize + 3 * curveSize + gamtSize;
        byte[] bytes = new byte[size];
        System.arraycopy(base, 0, bytes, 0, 128);
        putU32(bytes, 0, size);
        putU32(bytes, 128, tagCount);
        int redXyz = table;
        int greenXyz = redXyz + xyzSize;
        int blueXyz = greenXyz + xyzSize;
        int redTrc = blueXyz + xyzSize;
        int greenTrc = redTrc + curveSize;
        int blueTrc = greenTrc + curveSize;
        int gamt = blueTrc + curveSize;
        putTag(bytes, 0, 0x7258_595A, redXyz, xyzSize);
        putTag(bytes, 1, 0x6758_595A, greenXyz, xyzSize);
        putTag(bytes, 2, 0x6258_595A, blueXyz, xyzSize);
        putTag(bytes, 3, 0x7254_5243, redTrc, curveSize);
        putTag(bytes, 4, 0x6754_5243, greenTrc, curveSize);
        putTag(bytes, 5, 0x6254_5243, blueTrc, curveSize);
        putTag(bytes, 6, IccGamut.SIGNATURE, gamt, gamtSize);
        putXyz(bytes, redXyz, 0.4360657f, 0.2224884f, 0.0139160f);
        putXyz(bytes, greenXyz, 0.3851477f, 0.7168732f, 0.0971045f);
        putXyz(bytes, blueXyz, 0.1430664f, 0.0606084f, 0.7141733f);
        putIdentityCurve(bytes, redTrc);
        putIdentityCurve(bytes, greenTrc);
        putIdentityCurve(bytes, blueTrc);
        putSignature(bytes, gamt, "mBA ");
        bytes[gamt + 8] = 3;
        bytes[gamt + 9] = 1;
        putU32(bytes, gamt + 24, 32);
        int clut = gamt + 32;
        bytes[clut] = (byte) grid;
        bytes[clut + 1] = (byte) grid;
        bytes[clut + 2] = (byte) grid;
        bytes[clut + 16] = 1;
        int cursor = clut + 20;
        for (int red = 0; red < grid; red++) {
            for (int green = 0; green < grid; green++) {
                for (int blue = 0; blue < grid; blue++) {
                    boolean uniqueCell = red == 1 && green == 0 && blue == 0;
                    bytes[cursor++] = (byte) (uniqueCell ? 255 : 0);
                }
            }
        }
        return bytes;
    }

    /// Builds a `gamt` tag that illegally stores a 3×3 `mft1` table.
    private static byte[] matrixPlusInvalidThreeChannelGamt() {
        byte[] bytes = matrixPlusA2b0Mft1();
        int tagCount = 7;
        int table = 132 + tagCount * 12;
        int a2b0 = table + 3 * 20 + 3 * 12;
        putTag(bytes, 6, IccGamut.SIGNATURE, a2b0, u32(bytes, 132 + 6 * 12 + 8));
        return bytes;
    }

    /// Builds a matrix/TRC profile that also carries `cicp` sRGB code points.
    private static byte[] matrixWithCicp() {
        int tagCount = 7;
        int table = 132 + tagCount * 12;
        int xyzSize = 20;
        int curveSize = 12;
        int cicpSize = 12;
        int size = table + 3 * xyzSize + 3 * curveSize + cicpSize;
        byte[] bytes = new byte[size];
        putU32(bytes, 0, size);
        putU32(bytes, 8, 0x0240_0000);
        putSignature(bytes, 12, "mntr");
        putSignature(bytes, 16, "RGB ");
        putSignature(bytes, 20, "XYZ ");
        putSignature(bytes, 36, "acsp");
        putS15(bytes, 68, 0.9642f);
        putS15(bytes, 72, 1.0f);
        putS15(bytes, 76, 0.8249f);
        putU32(bytes, 128, tagCount);
        int redXyz = table;
        int greenXyz = redXyz + xyzSize;
        int blueXyz = greenXyz + xyzSize;
        int redTrc = blueXyz + xyzSize;
        int greenTrc = redTrc + curveSize;
        int blueTrc = greenTrc + curveSize;
        int cicp = blueTrc + curveSize;
        putTag(bytes, 0, 0x7258_595A, redXyz, xyzSize);
        putTag(bytes, 1, 0x6758_595A, greenXyz, xyzSize);
        putTag(bytes, 2, 0x6258_595A, blueXyz, xyzSize);
        putTag(bytes, 3, 0x7254_5243, redTrc, curveSize);
        putTag(bytes, 4, 0x6754_5243, greenTrc, curveSize);
        putTag(bytes, 5, 0x6254_5243, blueTrc, curveSize);
        putTag(bytes, 6, IccCicp.SIGNATURE, cicp, cicpSize);
        putXyz(bytes, redXyz, 0.4360657f, 0.2224884f, 0.0139160f);
        putXyz(bytes, greenXyz, 0.3851477f, 0.7168732f, 0.0971045f);
        putXyz(bytes, blueXyz, 0.1430664f, 0.0606084f, 0.7141733f);
        putIdentityCurve(bytes, redTrc);
        putIdentityCurve(bytes, greenTrc);
        putIdentityCurve(bytes, blueTrc);
        putSignature(bytes, cicp, "cicp");
        bytes[cicp + 8] = (byte) IccCicp.PRIMARIES_BT709;
        bytes[cicp + 9] = (byte) IccCicp.TRANSFER_SRGB;
        bytes[cicp + 10] = (byte) IccCicp.MATRIX_RGB;
        bytes[cicp + 11] = (byte) IccCicp.RANGE_FULL;
        return bytes;
    }

    /// Builds the matrix profile plus a 2³ `mft1` `pre0` preview tag.
    private static byte[] matrixPlusPreview0() {
        byte[] bytes = matrixPlusA2b0Mft1();
        int tagCount = 7;
        int table = 132 + tagCount * 12;
        int a2b0 = table + 3 * 20 + 3 * 12;
        putTag(bytes, 6, 0x7072_6530, a2b0, u32(bytes, 132 + 6 * 12 + 8));
        return bytes;
    }

    /// Builds a matrix/TRC profile with one Status A `resp` curve set.
    private static byte[] matrixWithOutputResponse() {
        int tagCount = 7;
        int table = 132 + tagCount * 12;
        int xyzSize = 20;
        int curveSize = 12;
        int channels = 3;
        int samples = 2;
        int respHeader = 20;
        int respSize = respHeader + channels * 12 + 4 + channels * samples * 8;
        int size = table + 3 * xyzSize + 3 * curveSize + respSize;
        byte[] bytes = new byte[size];
        putU32(bytes, 0, size);
        putU32(bytes, 8, 0x0240_0000);
        putSignature(bytes, 12, "mntr");
        putSignature(bytes, 16, "RGB ");
        putSignature(bytes, 20, "XYZ ");
        putSignature(bytes, 36, "acsp");
        putS15(bytes, 68, 0.9642f);
        putS15(bytes, 72, 1.0f);
        putS15(bytes, 76, 0.8249f);
        putU32(bytes, 128, tagCount);
        int redXyz = table;
        int greenXyz = redXyz + xyzSize;
        int blueXyz = greenXyz + xyzSize;
        int redTrc = blueXyz + xyzSize;
        int greenTrc = redTrc + curveSize;
        int blueTrc = greenTrc + curveSize;
        int resp = blueTrc + curveSize;
        putTag(bytes, 0, 0x7258_595A, redXyz, xyzSize);
        putTag(bytes, 1, 0x6758_595A, greenXyz, xyzSize);
        putTag(bytes, 2, 0x6258_595A, blueXyz, xyzSize);
        putTag(bytes, 3, 0x7254_5243, redTrc, curveSize);
        putTag(bytes, 4, 0x6754_5243, greenTrc, curveSize);
        putTag(bytes, 5, 0x6254_5243, blueTrc, curveSize);
        putTag(bytes, 6, IccOutputResponse.SIGNATURE, resp, respSize);
        putXyz(bytes, redXyz, 0.4360657f, 0.2224884f, 0.0139160f);
        putXyz(bytes, greenXyz, 0.3851477f, 0.7168732f, 0.0971045f);
        putXyz(bytes, blueXyz, 0.1430664f, 0.0606084f, 0.7141733f);
        putIdentityCurve(bytes, redTrc);
        putIdentityCurve(bytes, greenTrc);
        putIdentityCurve(bytes, blueTrc);
        putSignature(bytes, resp, "rcs2");
        putU16(bytes, resp + 8, channels);
        putU16(bytes, resp + 10, 1);
        putU32(bytes, resp + 12, IccOutputResponse.UNIT_STATUS_A);
        putU32(bytes, resp + 16, respHeader);
        int cursor = resp + respHeader;
        putS15(bytes, cursor, 0.4360657f);
        putS15(bytes, cursor + 4, 0.2224884f);
        putS15(bytes, cursor + 8, 0.0139160f);
        putS15(bytes, cursor + 12, 0.3851477f);
        putS15(bytes, cursor + 16, 0.7168732f);
        putS15(bytes, cursor + 20, 0.0971045f);
        putS15(bytes, cursor + 24, 0.1430664f);
        putS15(bytes, cursor + 28, 0.0606084f);
        putS15(bytes, cursor + 32, 0.7141733f);
        cursor += channels * 12;
        putU32(bytes, cursor, samples);
        cursor += 4;
        for (int channel = 0; channel < channels; channel++) {
            putU16(bytes, cursor + 2, 0);
            putS15(bytes, cursor + 4, 0.0f);
            cursor += 8;
            putU16(bytes, cursor + 2, 65535);
            putS15(bytes, cursor + 4, 1.0f);
            cursor += 8;
        }
        return bytes;
    }

    /// Builds a matrix/TRC profile with one `clrt` colorant and one `clot` colorant.
    private static byte[] matrixWithColorants() {
        int tagCount = 8;
        int table = 132 + tagCount * 12;
        int xyzSize = 20;
        int curveSize = 12;
        int colorantSize = 12 + 38;
        int size = table + 3 * xyzSize + 3 * curveSize + 2 * colorantSize;
        byte[] bytes = new byte[size];
        putU32(bytes, 0, size);
        putU32(bytes, 8, 0x0240_0000);
        putSignature(bytes, 12, "mntr");
        putSignature(bytes, 16, "RGB ");
        putSignature(bytes, 20, "XYZ ");
        putSignature(bytes, 36, "acsp");
        putS15(bytes, 68, 0.9642f);
        putS15(bytes, 72, 1.0f);
        putS15(bytes, 76, 0.8249f);
        putU32(bytes, 128, tagCount);
        int redXyz = table;
        int greenXyz = redXyz + xyzSize;
        int blueXyz = greenXyz + xyzSize;
        int redTrc = blueXyz + xyzSize;
        int greenTrc = redTrc + curveSize;
        int blueTrc = greenTrc + curveSize;
        int clrt = blueTrc + curveSize;
        int clot = clrt + colorantSize;
        putTag(bytes, 0, 0x7258_595A, redXyz, xyzSize);
        putTag(bytes, 1, 0x6758_595A, greenXyz, xyzSize);
        putTag(bytes, 2, 0x6258_595A, blueXyz, xyzSize);
        putTag(bytes, 3, 0x7254_5243, redTrc, curveSize);
        putTag(bytes, 4, 0x6754_5243, greenTrc, curveSize);
        putTag(bytes, 5, 0x6254_5243, blueTrc, curveSize);
        putTag(bytes, 6, IccColorants.TAG_CLRT, clrt, colorantSize);
        putTag(bytes, 7, IccColorants.TAG_CLOT, clot, colorantSize);
        putXyz(bytes, redXyz, 0.4360657f, 0.2224884f, 0.0139160f);
        putXyz(bytes, greenXyz, 0.3851477f, 0.7168732f, 0.0971045f);
        putXyz(bytes, blueXyz, 0.1430664f, 0.0606084f, 0.7141733f);
        putIdentityCurve(bytes, redTrc);
        putIdentityCurve(bytes, greenTrc);
        putIdentityCurve(bytes, blueTrc);
        putColorant(bytes, clrt, "Cyan", 0.1f, 0.2f, 0.3f);
        putColorant(bytes, clot, "Paper", 0.9f, 1.0f, 0.8f);
        return bytes;
    }

    /// Builds a matrix/TRC profile with one `pseq` device-link record.
    private static byte[] matrixWithProfileSequence() {
        int tagCount = 7;
        int table = 132 + tagCount * 12;
        int xyzSize = 20;
        int curveSize = 12;
        int descIec = descSize("IEC");
        int descSrgb = descSize("sRGB");
        int pseqSize = 12 + 20 + descIec + descSrgb;
        int size = table + 3 * xyzSize + 3 * curveSize + pseqSize;
        byte[] bytes = new byte[size];
        putU32(bytes, 0, size);
        putU32(bytes, 8, 0x0240_0000);
        putSignature(bytes, 12, "mntr");
        putSignature(bytes, 16, "RGB ");
        putSignature(bytes, 20, "XYZ ");
        putSignature(bytes, 36, "acsp");
        putS15(bytes, 68, 0.9642f);
        putS15(bytes, 72, 1.0f);
        putS15(bytes, 76, 0.8249f);
        putU32(bytes, 128, tagCount);
        int redXyz = table;
        int greenXyz = redXyz + xyzSize;
        int blueXyz = greenXyz + xyzSize;
        int redTrc = blueXyz + xyzSize;
        int greenTrc = redTrc + curveSize;
        int blueTrc = greenTrc + curveSize;
        int pseq = blueTrc + curveSize;
        putTag(bytes, 0, 0x7258_595A, redXyz, xyzSize);
        putTag(bytes, 1, 0x6758_595A, greenXyz, xyzSize);
        putTag(bytes, 2, 0x6258_595A, blueXyz, xyzSize);
        putTag(bytes, 3, 0x7254_5243, redTrc, curveSize);
        putTag(bytes, 4, 0x6754_5243, greenTrc, curveSize);
        putTag(bytes, 5, 0x6254_5243, blueTrc, curveSize);
        putTag(bytes, 6, IccProfileSequence.SIGNATURE, pseq, pseqSize);
        putXyz(bytes, redXyz, 0.4360657f, 0.2224884f, 0.0139160f);
        putXyz(bytes, greenXyz, 0.3851477f, 0.7168732f, 0.0971045f);
        putXyz(bytes, blueXyz, 0.1430664f, 0.0606084f, 0.7141733f);
        putIdentityCurve(bytes, redTrc);
        putIdentityCurve(bytes, greenTrc);
        putIdentityCurve(bytes, blueTrc);
        putSignature(bytes, pseq, "pseq");
        putU32(bytes, pseq + 8, 1);
        putSignature(bytes, pseq + 12, "IEC ");
        putSignature(bytes, pseq + 16, "sRGB");
        putSignature(bytes, pseq + 28, "CRT ");
        int cursor = pseq + 32;
        cursor += putDesc(bytes, cursor, "IEC");
        putDesc(bytes, cursor, "sRGB");
        return bytes;
    }

    /// Builds a matrix/TRC profile with one `psid` identifier.
    private static byte[] matrixWithProfileSequenceIds() {
        int tagCount = 7;
        int table = 132 + tagCount * 12;
        int xyzSize = 20;
        int curveSize = 12;
        int desc = descSize("sRGB");
        int record = 16 + desc;
        int psidSize = 20 + record;
        int size = table + 3 * xyzSize + 3 * curveSize + psidSize;
        byte[] bytes = new byte[size];
        putU32(bytes, 0, size);
        putU32(bytes, 8, 0x0240_0000);
        putSignature(bytes, 12, "mntr");
        putSignature(bytes, 16, "RGB ");
        putSignature(bytes, 20, "XYZ ");
        putSignature(bytes, 36, "acsp");
        putS15(bytes, 68, 0.9642f);
        putS15(bytes, 72, 1.0f);
        putS15(bytes, 76, 0.8249f);
        putU32(bytes, 128, tagCount);
        int redXyz = table;
        int greenXyz = redXyz + xyzSize;
        int blueXyz = greenXyz + xyzSize;
        int redTrc = blueXyz + xyzSize;
        int greenTrc = redTrc + curveSize;
        int blueTrc = greenTrc + curveSize;
        int psid = blueTrc + curveSize;
        putTag(bytes, 0, 0x7258_595A, redXyz, xyzSize);
        putTag(bytes, 1, 0x6758_595A, greenXyz, xyzSize);
        putTag(bytes, 2, 0x6258_595A, blueXyz, xyzSize);
        putTag(bytes, 3, 0x7254_5243, redTrc, curveSize);
        putTag(bytes, 4, 0x6754_5243, greenTrc, curveSize);
        putTag(bytes, 5, 0x6254_5243, blueTrc, curveSize);
        putTag(bytes, 6, IccProfileSequenceIds.SIGNATURE, psid, psidSize);
        putXyz(bytes, redXyz, 0.4360657f, 0.2224884f, 0.0139160f);
        putXyz(bytes, greenXyz, 0.3851477f, 0.7168732f, 0.0971045f);
        putXyz(bytes, blueXyz, 0.1430664f, 0.0606084f, 0.7141733f);
        putIdentityCurve(bytes, redTrc);
        putIdentityCurve(bytes, greenTrc);
        putIdentityCurve(bytes, blueTrc);
        putSignature(bytes, psid, "psid");
        putU32(bytes, psid + 8, 1);
        putU32(bytes, psid + 12, 20);
        putU32(bytes, psid + 16, record);
        for (int index = 0; index < 16; index++) {
            bytes[psid + 20 + index] = (byte) index;
        }
        putDesc(bytes, psid + 36, "sRGB");
        return bytes;
    }

    /// Builds a matrix/TRC profile with one `meta` dictionary entry.
    private static byte[] matrixWithMetadata() {
        int tagCount = 7;
        int table = 132 + tagCount * 12;
        int xyzSize = 20;
        int curveSize = 12;
        int nameBytes = 8;
        int valueBytes = 8;
        int mlucHeader = 28;
        int displayBytes = 24;
        int mlucSize = mlucHeader + displayBytes;
        int dictSize = 36 + nameBytes + valueBytes + mlucSize;
        int size = table + 3 * xyzSize + 3 * curveSize + dictSize;
        byte[] bytes = new byte[size];
        putU32(bytes, 0, size);
        putU32(bytes, 8, 0x0240_0000);
        putSignature(bytes, 12, "mntr");
        putSignature(bytes, 16, "RGB ");
        putSignature(bytes, 20, "XYZ ");
        putSignature(bytes, 36, "acsp");
        putS15(bytes, 68, 0.9642f);
        putS15(bytes, 72, 1.0f);
        putS15(bytes, 76, 0.8249f);
        putU32(bytes, 128, tagCount);
        int redXyz = table;
        int greenXyz = redXyz + xyzSize;
        int blueXyz = greenXyz + xyzSize;
        int redTrc = blueXyz + xyzSize;
        int greenTrc = redTrc + curveSize;
        int blueTrc = greenTrc + curveSize;
        int meta = blueTrc + curveSize;
        putTag(bytes, 0, 0x7258_595A, redXyz, xyzSize);
        putTag(bytes, 1, 0x6758_595A, greenXyz, xyzSize);
        putTag(bytes, 2, 0x6258_595A, blueXyz, xyzSize);
        putTag(bytes, 3, 0x7254_5243, redTrc, curveSize);
        putTag(bytes, 4, 0x6754_5243, greenTrc, curveSize);
        putTag(bytes, 5, 0x6254_5243, blueTrc, curveSize);
        putTag(bytes, 6, IccMetadata.SIGNATURE, meta, dictSize);
        putXyz(bytes, redXyz, 0.4360657f, 0.2224884f, 0.0139160f);
        putXyz(bytes, greenXyz, 0.3851477f, 0.7168732f, 0.0971045f);
        putXyz(bytes, blueXyz, 0.1430664f, 0.0606084f, 0.7141733f);
        putIdentityCurve(bytes, redTrc);
        putIdentityCurve(bytes, greenTrc);
        putIdentityCurve(bytes, blueTrc);
        putSignature(bytes, meta, "dict");
        putU32(bytes, meta + 8, 1);
        putU32(bytes, meta + 12, 36);
        putU32(bytes, meta + 16, nameBytes);
        putU32(bytes, meta + 20, 44);
        putU32(bytes, meta + 24, valueBytes);
        putU32(bytes, meta + 28, 52);
        putU32(bytes, meta + 32, mlucSize);
        putUtf16be(bytes, meta + 36, "Pref");
        putUtf16be(bytes, meta + 44, "sRGB");
        putSignature(bytes, meta + 52, "mluc");
        putU32(bytes, meta + 60, 1);
        putU32(bytes, meta + 64, 12);
        putU16(bytes, meta + 68, 0x656E);
        putU16(bytes, meta + 70, 0x5553);
        putU32(bytes, meta + 72, displayBytes);
        putU32(bytes, meta + 76, mlucHeader);
        putUtf16be(bytes, meta + 80, "sRGB Profile");
        return bytes;
    }

    /// Builds a matrix/TRC profile with BT.709 `chrm` primaries.
    private static byte[] matrixWithChromaticity() {
        int tagCount = 7;
        int table = 132 + tagCount * 12;
        int xyzSize = 20;
        int curveSize = 12;
        int chrmSize = 36;
        int size = table + 3 * xyzSize + 3 * curveSize + chrmSize;
        byte[] bytes = new byte[size];
        putU32(bytes, 0, size);
        putU32(bytes, 8, 0x0240_0000);
        putSignature(bytes, 12, "mntr");
        putSignature(bytes, 16, "RGB ");
        putSignature(bytes, 20, "XYZ ");
        putSignature(bytes, 36, "acsp");
        putS15(bytes, 68, 0.9642f);
        putS15(bytes, 72, 1.0f);
        putS15(bytes, 76, 0.8249f);
        putU32(bytes, 128, tagCount);
        int redXyz = table;
        int greenXyz = redXyz + xyzSize;
        int blueXyz = greenXyz + xyzSize;
        int redTrc = blueXyz + xyzSize;
        int greenTrc = redTrc + curveSize;
        int blueTrc = greenTrc + curveSize;
        int chrm = blueTrc + curveSize;
        putTag(bytes, 0, 0x7258_595A, redXyz, xyzSize);
        putTag(bytes, 1, 0x6758_595A, greenXyz, xyzSize);
        putTag(bytes, 2, 0x6258_595A, blueXyz, xyzSize);
        putTag(bytes, 3, 0x7254_5243, redTrc, curveSize);
        putTag(bytes, 4, 0x6754_5243, greenTrc, curveSize);
        putTag(bytes, 5, 0x6254_5243, blueTrc, curveSize);
        putTag(bytes, 6, IccChromaticity.SIGNATURE, chrm, chrmSize);
        putXyz(bytes, redXyz, 0.4360657f, 0.2224884f, 0.0139160f);
        putXyz(bytes, greenXyz, 0.3851477f, 0.7168732f, 0.0971045f);
        putXyz(bytes, blueXyz, 0.1430664f, 0.0606084f, 0.7141733f);
        putIdentityCurve(bytes, redTrc);
        putIdentityCurve(bytes, greenTrc);
        putIdentityCurve(bytes, blueTrc);
        putSignature(bytes, chrm, "chrm");
        putU16(bytes, chrm + 8, 3);
        putU16(bytes, chrm + 10, IccChromaticity.PHOSPHOR_BT709);
        putS15(bytes, chrm + 12, 0.64f);
        putS15(bytes, chrm + 16, 0.33f);
        putS15(bytes, chrm + 20, 0.30f);
        putS15(bytes, chrm + 24, 0.60f);
        putS15(bytes, chrm + 28, 0.15f);
        putS15(bytes, chrm + 32, 0.06f);
        return bytes;
    }

    /// Builds a matrix/TRC profile with a three-channel `clro` order.
    private static byte[] matrixWithColorantOrder() {
        int tagCount = 7;
        int table = 132 + tagCount * 12;
        int xyzSize = 20;
        int curveSize = 12;
        int clroSize = 15;
        int size = table + 3 * xyzSize + 3 * curveSize + clroSize;
        byte[] bytes = new byte[size];
        putU32(bytes, 0, size);
        putU32(bytes, 8, 0x0240_0000);
        putSignature(bytes, 12, "mntr");
        putSignature(bytes, 16, "RGB ");
        putSignature(bytes, 20, "XYZ ");
        putSignature(bytes, 36, "acsp");
        putS15(bytes, 68, 0.9642f);
        putS15(bytes, 72, 1.0f);
        putS15(bytes, 76, 0.8249f);
        putU32(bytes, 128, tagCount);
        int redXyz = table;
        int greenXyz = redXyz + xyzSize;
        int blueXyz = greenXyz + xyzSize;
        int redTrc = blueXyz + xyzSize;
        int greenTrc = redTrc + curveSize;
        int blueTrc = greenTrc + curveSize;
        int clro = blueTrc + curveSize;
        putTag(bytes, 0, 0x7258_595A, redXyz, xyzSize);
        putTag(bytes, 1, 0x6758_595A, greenXyz, xyzSize);
        putTag(bytes, 2, 0x6258_595A, blueXyz, xyzSize);
        putTag(bytes, 3, 0x7254_5243, redTrc, curveSize);
        putTag(bytes, 4, 0x6754_5243, greenTrc, curveSize);
        putTag(bytes, 5, 0x6254_5243, blueTrc, curveSize);
        putTag(bytes, 6, IccColorantOrder.SIGNATURE, clro, clroSize);
        putXyz(bytes, redXyz, 0.4360657f, 0.2224884f, 0.0139160f);
        putXyz(bytes, greenXyz, 0.3851477f, 0.7168732f, 0.0971045f);
        putXyz(bytes, blueXyz, 0.1430664f, 0.0606084f, 0.7141733f);
        putIdentityCurve(bytes, redTrc);
        putIdentityCurve(bytes, greenTrc);
        putIdentityCurve(bytes, blueTrc);
        putSignature(bytes, clro, "clro");
        putU32(bytes, clro + 8, 3);
        bytes[clro + 12] = 3;
        bytes[clro + 13] = 1;
        bytes[clro + 14] = 2;
        return bytes;
    }

    /// Builds a matrix/TRC profile whose `desc` tag is a v4 `mluc` record.
    private static byte[] matrixWithMlucDescription() {
        int tagCount = 7;
        int table = 132 + tagCount * 12;
        int xyzSize = 20;
        int curveSize = 12;
        int stringBytes = 12;
        int mlucSize = 28 + stringBytes;
        int size = table + 3 * xyzSize + 3 * curveSize + mlucSize;
        byte[] bytes = new byte[size];
        putU32(bytes, 0, size);
        putU32(bytes, 8, 0x0440_0000);
        putSignature(bytes, 12, "mntr");
        putSignature(bytes, 16, "RGB ");
        putSignature(bytes, 20, "XYZ ");
        putSignature(bytes, 36, "acsp");
        putS15(bytes, 68, 0.9642f);
        putS15(bytes, 72, 1.0f);
        putS15(bytes, 76, 0.8249f);
        putU32(bytes, 128, tagCount);
        int redXyz = table;
        int greenXyz = redXyz + xyzSize;
        int blueXyz = greenXyz + xyzSize;
        int redTrc = blueXyz + xyzSize;
        int greenTrc = redTrc + curveSize;
        int blueTrc = greenTrc + curveSize;
        int desc = blueTrc + curveSize;
        putTag(bytes, 0, 0x7258_595A, redXyz, xyzSize);
        putTag(bytes, 1, 0x6758_595A, greenXyz, xyzSize);
        putTag(bytes, 2, 0x6258_595A, blueXyz, xyzSize);
        putTag(bytes, 3, 0x7254_5243, redTrc, curveSize);
        putTag(bytes, 4, 0x6754_5243, greenTrc, curveSize);
        putTag(bytes, 5, 0x6254_5243, blueTrc, curveSize);
        putTag(bytes, 6, 0x6465_7363, desc, mlucSize);
        putXyz(bytes, redXyz, 0.4360657f, 0.2224884f, 0.0139160f);
        putXyz(bytes, greenXyz, 0.3851477f, 0.7168732f, 0.0971045f);
        putXyz(bytes, blueXyz, 0.1430664f, 0.0606084f, 0.7141733f);
        putIdentityCurve(bytes, redTrc);
        putIdentityCurve(bytes, greenTrc);
        putIdentityCurve(bytes, blueTrc);
        putSignature(bytes, desc, "mluc");
        putU32(bytes, desc + 8, 1);
        putU32(bytes, desc + 12, 12);
        putU16(bytes, desc + 16, 0x656E);
        putU16(bytes, desc + 18, 0x5553);
        putU32(bytes, desc + 20, stringBytes);
        putU32(bytes, desc + 24, 28);
        putUtf16be(bytes, desc + 28, "v4sRGB");
        return bytes;
    }

    /// Builds a matrix/TRC profile with one frequency-encoded `scrn` channel.
    private static byte[] matrixWithScreening() {
        int tagCount = 7;
        int table = 132 + tagCount * 12;
        int xyzSize = 20;
        int curveSize = 12;
        int scrnSize = 28;
        int size = table + 3 * xyzSize + 3 * curveSize + scrnSize;
        byte[] bytes = new byte[size];
        putU32(bytes, 0, size);
        putU32(bytes, 8, 0x0240_0000);
        putSignature(bytes, 12, "mntr");
        putSignature(bytes, 16, "RGB ");
        putSignature(bytes, 20, "XYZ ");
        putSignature(bytes, 36, "acsp");
        putS15(bytes, 68, 0.9642f);
        putS15(bytes, 72, 1.0f);
        putS15(bytes, 76, 0.8249f);
        putU32(bytes, 128, tagCount);
        int redXyz = table;
        int greenXyz = redXyz + xyzSize;
        int blueXyz = greenXyz + xyzSize;
        int redTrc = blueXyz + xyzSize;
        int greenTrc = redTrc + curveSize;
        int blueTrc = greenTrc + curveSize;
        int scrn = blueTrc + curveSize;
        putTag(bytes, 0, 0x7258_595A, redXyz, xyzSize);
        putTag(bytes, 1, 0x6758_595A, greenXyz, xyzSize);
        putTag(bytes, 2, 0x6258_595A, blueXyz, xyzSize);
        putTag(bytes, 3, 0x7254_5243, redTrc, curveSize);
        putTag(bytes, 4, 0x6754_5243, greenTrc, curveSize);
        putTag(bytes, 5, 0x6254_5243, blueTrc, curveSize);
        putTag(bytes, 6, IccScreening.SIGNATURE, scrn, scrnSize);
        putXyz(bytes, redXyz, 0.4360657f, 0.2224884f, 0.0139160f);
        putXyz(bytes, greenXyz, 0.3851477f, 0.7168732f, 0.0971045f);
        putXyz(bytes, blueXyz, 0.1430664f, 0.0606084f, 0.7141733f);
        putIdentityCurve(bytes, redTrc);
        putIdentityCurve(bytes, greenTrc);
        putIdentityCurve(bytes, blueTrc);
        putSignature(bytes, scrn, "scrn");
        putU32(bytes, scrn + 8, 1);
        putU32(bytes, scrn + 12, 1);
        putS15(bytes, scrn + 16, 150.0f);
        putS15(bytes, scrn + 20, 45.0f);
        putU32(bytes, scrn + 24, IccScreening.SPOT_ROUND);
        return bytes;
    }

    /// Builds a matrix/TRC profile with UCR gamma 2, identity BG, and an ASCII label.
    private static byte[] matrixWithUcrBg() {
        int tagCount = 7;
        int table = 132 + tagCount * 12;
        int xyzSize = 20;
        int curveSize = 12;
        int bfdSize = 8 + 14 + 12 + 4;
        int size = table + 3 * xyzSize + 3 * curveSize + bfdSize;
        byte[] bytes = new byte[size];
        putU32(bytes, 0, size);
        putU32(bytes, 8, 0x0240_0000);
        putSignature(bytes, 12, "mntr");
        putSignature(bytes, 16, "RGB ");
        putSignature(bytes, 20, "XYZ ");
        putSignature(bytes, 36, "acsp");
        putS15(bytes, 68, 0.9642f);
        putS15(bytes, 72, 1.0f);
        putS15(bytes, 76, 0.8249f);
        putU32(bytes, 128, tagCount);
        int redXyz = table;
        int greenXyz = redXyz + xyzSize;
        int blueXyz = greenXyz + xyzSize;
        int redTrc = blueXyz + xyzSize;
        int greenTrc = redTrc + curveSize;
        int blueTrc = greenTrc + curveSize;
        int bfd = blueTrc + curveSize;
        putTag(bytes, 0, 0x7258_595A, redXyz, xyzSize);
        putTag(bytes, 1, 0x6758_595A, greenXyz, xyzSize);
        putTag(bytes, 2, 0x6258_595A, blueXyz, xyzSize);
        putTag(bytes, 3, 0x7254_5243, redTrc, curveSize);
        putTag(bytes, 4, 0x6754_5243, greenTrc, curveSize);
        putTag(bytes, 5, 0x6254_5243, blueTrc, curveSize);
        putTag(bytes, 6, IccUcrBg.SIGNATURE, bfd, bfdSize);
        putXyz(bytes, redXyz, 0.4360657f, 0.2224884f, 0.0139160f);
        putXyz(bytes, greenXyz, 0.3851477f, 0.7168732f, 0.0971045f);
        putXyz(bytes, blueXyz, 0.1430664f, 0.0606084f, 0.7141733f);
        putIdentityCurve(bytes, redTrc);
        putIdentityCurve(bytes, greenTrc);
        putIdentityCurve(bytes, blueTrc);
        putSignature(bytes, bfd, "bfd ");
        putSignature(bytes, bfd + 8, "curv");
        putU32(bytes, bfd + 16, 1);
        putU16(bytes, bfd + 20, 512);
        putSignature(bytes, bfd + 22, "curv");
        putU32(bytes, bfd + 30, 0);
        putAscii32(bytes, bfd + 34, "UCR");
        return bytes;
    }

    /// Builds a matrix/TRC profile with a `targ` IT8 target name.
    private static byte[] matrixWithCharacterizationTarget() {
        int tagCount = 7;
        int table = 132 + tagCount * 12;
        int xyzSize = 20;
        int curveSize = 12;
        int targSize = 16;
        int size = table + 3 * xyzSize + 3 * curveSize + targSize;
        byte[] bytes = new byte[size];
        putU32(bytes, 0, size);
        putU32(bytes, 8, 0x0240_0000);
        putSignature(bytes, 12, "mntr");
        putSignature(bytes, 16, "RGB ");
        putSignature(bytes, 20, "XYZ ");
        putSignature(bytes, 36, "acsp");
        putS15(bytes, 68, 0.9642f);
        putS15(bytes, 72, 1.0f);
        putS15(bytes, 76, 0.8249f);
        putU32(bytes, 128, tagCount);
        int redXyz = table;
        int greenXyz = redXyz + xyzSize;
        int blueXyz = greenXyz + xyzSize;
        int redTrc = blueXyz + xyzSize;
        int greenTrc = redTrc + curveSize;
        int blueTrc = greenTrc + curveSize;
        int targ = blueTrc + curveSize;
        putTag(bytes, 0, 0x7258_595A, redXyz, xyzSize);
        putTag(bytes, 1, 0x6758_595A, greenXyz, xyzSize);
        putTag(bytes, 2, 0x6258_595A, blueXyz, xyzSize);
        putTag(bytes, 3, 0x7254_5243, redTrc, curveSize);
        putTag(bytes, 4, 0x6754_5243, greenTrc, curveSize);
        putTag(bytes, 5, 0x6254_5243, blueTrc, curveSize);
        putTag(bytes, 6, 0x7461_7267, targ, targSize);
        putXyz(bytes, redXyz, 0.4360657f, 0.2224884f, 0.0139160f);
        putXyz(bytes, greenXyz, 0.3851477f, 0.7168732f, 0.0971045f);
        putXyz(bytes, blueXyz, 0.1430664f, 0.0606084f, 0.7141733f);
        putIdentityCurve(bytes, redTrc);
        putIdentityCurve(bytes, greenTrc);
        putIdentityCurve(bytes, blueTrc);
        putSignature(bytes, targ, "text");
        putAscii32(bytes, targ + 8, "IT8.7/2");
        return bytes;
    }

    /// Builds a matrix/TRC profile with an ASCII `crdi` payload.
    private static byte[] matrixWithColorRenderingDict() {
        int tagCount = 7;
        int table = 132 + tagCount * 12;
        int xyzSize = 20;
        int curveSize = 12;
        int crdiSize = 16;
        int size = table + 3 * xyzSize + 3 * curveSize + crdiSize;
        byte[] bytes = new byte[size];
        putU32(bytes, 0, size);
        putU32(bytes, 8, 0x0240_0000);
        putSignature(bytes, 12, "mntr");
        putSignature(bytes, 16, "RGB ");
        putSignature(bytes, 20, "XYZ ");
        putSignature(bytes, 36, "acsp");
        putS15(bytes, 68, 0.9642f);
        putS15(bytes, 72, 1.0f);
        putS15(bytes, 76, 0.8249f);
        putU32(bytes, 128, tagCount);
        int redXyz = table;
        int greenXyz = redXyz + xyzSize;
        int blueXyz = greenXyz + xyzSize;
        int redTrc = blueXyz + xyzSize;
        int greenTrc = redTrc + curveSize;
        int blueTrc = greenTrc + curveSize;
        int crdi = blueTrc + curveSize;
        putTag(bytes, 0, 0x7258_595A, redXyz, xyzSize);
        putTag(bytes, 1, 0x6758_595A, greenXyz, xyzSize);
        putTag(bytes, 2, 0x6258_595A, blueXyz, xyzSize);
        putTag(bytes, 3, 0x7254_5243, redTrc, curveSize);
        putTag(bytes, 4, 0x6754_5243, greenTrc, curveSize);
        putTag(bytes, 5, 0x6254_5243, blueTrc, curveSize);
        putTag(bytes, 6, IccData.TAG_CRDI, crdi, crdiSize);
        putXyz(bytes, redXyz, 0.4360657f, 0.2224884f, 0.0139160f);
        putXyz(bytes, greenXyz, 0.3851477f, 0.7168732f, 0.0971045f);
        putXyz(bytes, blueXyz, 0.1430664f, 0.0606084f, 0.7141733f);
        putIdentityCurve(bytes, redTrc);
        putIdentityCurve(bytes, greenTrc);
        putIdentityCurve(bytes, blueTrc);
        putSignature(bytes, crdi, "data");
        putU32(bytes, crdi + 8, IccData.FLAG_ASCII);
        putAscii32(bytes, crdi + 12, "CRD");
        return bytes;
    }

    /// Builds a matrix/TRC profile with `ps2s` and `ps2i` ASCII payloads.
    private static byte[] matrixWithPostScript2Data() {
        int tagCount = 8;
        int table = 132 + tagCount * 12;
        int xyzSize = 20;
        int curveSize = 12;
        int ps2sSize = 16;
        int ps2iSize = 17;
        int size = table + 3 * xyzSize + 3 * curveSize + ps2sSize + ps2iSize;
        byte[] bytes = new byte[size];
        putU32(bytes, 0, size);
        putU32(bytes, 8, 0x0240_0000);
        putSignature(bytes, 12, "mntr");
        putSignature(bytes, 16, "RGB ");
        putSignature(bytes, 20, "XYZ ");
        putSignature(bytes, 36, "acsp");
        putS15(bytes, 68, 0.9642f);
        putS15(bytes, 72, 1.0f);
        putS15(bytes, 76, 0.8249f);
        putU32(bytes, 128, tagCount);
        int redXyz = table;
        int greenXyz = redXyz + xyzSize;
        int blueXyz = greenXyz + xyzSize;
        int redTrc = blueXyz + xyzSize;
        int greenTrc = redTrc + curveSize;
        int blueTrc = greenTrc + curveSize;
        int ps2s = blueTrc + curveSize;
        int ps2i = ps2s + ps2sSize;
        putTag(bytes, 0, 0x7258_595A, redXyz, xyzSize);
        putTag(bytes, 1, 0x6758_595A, greenXyz, xyzSize);
        putTag(bytes, 2, 0x6258_595A, blueXyz, xyzSize);
        putTag(bytes, 3, 0x7254_5243, redTrc, curveSize);
        putTag(bytes, 4, 0x6754_5243, greenTrc, curveSize);
        putTag(bytes, 5, 0x6254_5243, blueTrc, curveSize);
        putTag(bytes, 6, IccData.TAG_PS2S, ps2s, ps2sSize);
        putTag(bytes, 7, IccData.TAG_PS2I, ps2i, ps2iSize);
        putXyz(bytes, redXyz, 0.4360657f, 0.2224884f, 0.0139160f);
        putXyz(bytes, greenXyz, 0.3851477f, 0.7168732f, 0.0971045f);
        putXyz(bytes, blueXyz, 0.1430664f, 0.0606084f, 0.7141733f);
        putIdentityCurve(bytes, redTrc);
        putIdentityCurve(bytes, greenTrc);
        putIdentityCurve(bytes, blueTrc);
        putSignature(bytes, ps2s, "data");
        putU32(bytes, ps2s + 8, IccData.FLAG_ASCII);
        putAscii32(bytes, ps2s + 12, "CSA");
        putSignature(bytes, ps2i, "data");
        putU32(bytes, ps2i + 8, IccData.FLAG_ASCII);
        putAscii32(bytes, ps2i + 12, "CRD2");
        return bytes;
    }

    /// Builds a matrix/TRC profile with `psd0`–`psd3` text descriptions.
    private static byte[] matrixWithPostScriptDescs() {
        int tagCount = 10;
        int table = 132 + tagCount * 12;
        int xyzSize = 20;
        int curveSize = 12;
        int desc0Size = 19;
        int desc1Size = 17;
        int desc2Size = 19;
        int desc3Size = 17;
        int size = table + 3 * xyzSize + 3 * curveSize + desc0Size + desc1Size + desc2Size + desc3Size;
        byte[] bytes = new byte[size];
        putU32(bytes, 0, size);
        putU32(bytes, 8, 0x0240_0000);
        putSignature(bytes, 12, "mntr");
        putSignature(bytes, 16, "RGB ");
        putSignature(bytes, 20, "XYZ ");
        putSignature(bytes, 36, "acsp");
        putS15(bytes, 68, 0.9642f);
        putS15(bytes, 72, 1.0f);
        putS15(bytes, 76, 0.8249f);
        putU32(bytes, 128, tagCount);
        int redXyz = table;
        int greenXyz = redXyz + xyzSize;
        int blueXyz = greenXyz + xyzSize;
        int redTrc = blueXyz + xyzSize;
        int greenTrc = redTrc + curveSize;
        int blueTrc = greenTrc + curveSize;
        int psd0 = blueTrc + curveSize;
        int psd1 = psd0 + desc0Size;
        int psd2 = psd1 + desc1Size;
        int psd3 = psd2 + desc2Size;
        putTag(bytes, 0, 0x7258_595A, redXyz, xyzSize);
        putTag(bytes, 1, 0x6758_595A, greenXyz, xyzSize);
        putTag(bytes, 2, 0x6258_595A, blueXyz, xyzSize);
        putTag(bytes, 3, 0x7254_5243, redTrc, curveSize);
        putTag(bytes, 4, 0x6754_5243, greenTrc, curveSize);
        putTag(bytes, 5, 0x6254_5243, blueTrc, curveSize);
        putTag(bytes, 6, 0x7073_6430, psd0, desc0Size);
        putTag(bytes, 7, 0x7073_6431, psd1, desc1Size);
        putTag(bytes, 8, 0x7073_6432, psd2, desc2Size);
        putTag(bytes, 9, 0x7073_6433, psd3, desc3Size);
        putXyz(bytes, redXyz, 0.4360657f, 0.2224884f, 0.0139160f);
        putXyz(bytes, greenXyz, 0.3851477f, 0.7168732f, 0.0971045f);
        putXyz(bytes, blueXyz, 0.1430664f, 0.0606084f, 0.7141733f);
        putIdentityCurve(bytes, redTrc);
        putIdentityCurve(bytes, greenTrc);
        putIdentityCurve(bytes, blueTrc);
        putSignature(bytes, psd0, "text");
        putAscii32(bytes, psd0 + 8, "Perceptual");
        putSignature(bytes, psd1, "text");
        putAscii32(bytes, psd1 + 8, "Relative");
        putSignature(bytes, psd2, "text");
        putAscii32(bytes, psd2 + 8, "Saturation");
        putSignature(bytes, psd3, "text");
        putAscii32(bytes, psd3 + 8, "Absolute");
        return bytes;
    }

    /// Builds a matrix/TRC profile with one Microsoft `devs` combination.
    private static byte[] matrixWithDeviceSettings() {
        int tagCount = 7;
        int table = 132 + tagCount * 12;
        int xyzSize = 20;
        int curveSize = 12;
        int rslnSize = 20;
        int mtypSize = 16;
        int combinationSize = 8 + rslnSize + mtypSize;
        int platformSize = 12 + combinationSize;
        int devsSize = 12 + platformSize;
        int size = table + 3 * xyzSize + 3 * curveSize + devsSize;
        byte[] bytes = new byte[size];
        putU32(bytes, 0, size);
        putU32(bytes, 8, 0x0240_0000);
        putSignature(bytes, 12, "mntr");
        putSignature(bytes, 16, "RGB ");
        putSignature(bytes, 20, "XYZ ");
        putSignature(bytes, 36, "acsp");
        putS15(bytes, 68, 0.9642f);
        putS15(bytes, 72, 1.0f);
        putS15(bytes, 76, 0.8249f);
        putU32(bytes, 128, tagCount);
        int redXyz = table;
        int greenXyz = redXyz + xyzSize;
        int blueXyz = greenXyz + xyzSize;
        int redTrc = blueXyz + xyzSize;
        int greenTrc = redTrc + curveSize;
        int blueTrc = greenTrc + curveSize;
        int devs = blueTrc + curveSize;
        putTag(bytes, 0, 0x7258_595A, redXyz, xyzSize);
        putTag(bytes, 1, 0x6758_595A, greenXyz, xyzSize);
        putTag(bytes, 2, 0x6258_595A, blueXyz, xyzSize);
        putTag(bytes, 3, 0x7254_5243, redTrc, curveSize);
        putTag(bytes, 4, 0x6754_5243, greenTrc, curveSize);
        putTag(bytes, 5, 0x6254_5243, blueTrc, curveSize);
        putTag(bytes, 6, IccDeviceSettings.SIGNATURE, devs, devsSize);
        putXyz(bytes, redXyz, 0.4360657f, 0.2224884f, 0.0139160f);
        putXyz(bytes, greenXyz, 0.3851477f, 0.7168732f, 0.0971045f);
        putXyz(bytes, blueXyz, 0.1430664f, 0.0606084f, 0.7141733f);
        putIdentityCurve(bytes, redTrc);
        putIdentityCurve(bytes, greenTrc);
        putIdentityCurve(bytes, blueTrc);
        putSignature(bytes, devs, "devs");
        putU32(bytes, devs + 8, 1);
        putSignature(bytes, devs + 12, IccDeviceSettings.PLATFORM_MSFT);
        putU32(bytes, devs + 16, platformSize);
        putU32(bytes, devs + 20, 1);
        putU32(bytes, devs + 24, combinationSize);
        putU32(bytes, devs + 28, 2);
        putSignature(bytes, devs + 32, IccDeviceSettings.SETTING_RESOLUTION);
        putU32(bytes, devs + 36, 8);
        putU32(bytes, devs + 40, 1);
        putU32(bytes, devs + 44, 600);
        putU32(bytes, devs + 48, 300);
        putSignature(bytes, devs + 52, IccDeviceSettings.SETTING_MEDIA);
        putU32(bytes, devs + 56, 4);
        putU32(bytes, devs + 60, 1);
        putU32(bytes, devs + 64, IccDeviceSettings.MEDIA_GLOSSY);
        return bytes;
    }

    /// Builds a matrix/TRC profile with a `crdInfoType` product and perceptual CRD name.
    private static byte[] matrixWithCrdInfo() {
        int tagCount = 7;
        int table = 132 + tagCount * 12;
        int xyzSize = 20;
        int curveSize = 12;
        int crdiSize = 8 + 4 + 7 + 4 + 14 + 4 + 4 + 4;
        int size = table + 3 * xyzSize + 3 * curveSize + crdiSize;
        byte[] bytes = new byte[size];
        putU32(bytes, 0, size);
        putU32(bytes, 8, 0x0240_0000);
        putSignature(bytes, 12, "mntr");
        putSignature(bytes, 16, "RGB ");
        putSignature(bytes, 20, "XYZ ");
        putSignature(bytes, 36, "acsp");
        putS15(bytes, 68, 0.9642f);
        putS15(bytes, 72, 1.0f);
        putS15(bytes, 76, 0.8249f);
        putU32(bytes, 128, tagCount);
        int redXyz = table;
        int greenXyz = redXyz + xyzSize;
        int blueXyz = greenXyz + xyzSize;
        int redTrc = blueXyz + xyzSize;
        int greenTrc = redTrc + curveSize;
        int blueTrc = greenTrc + curveSize;
        int crdi = blueTrc + curveSize;
        putTag(bytes, 0, 0x7258_595A, redXyz, xyzSize);
        putTag(bytes, 1, 0x6758_595A, greenXyz, xyzSize);
        putTag(bytes, 2, 0x6258_595A, blueXyz, xyzSize);
        putTag(bytes, 3, 0x7254_5243, redTrc, curveSize);
        putTag(bytes, 4, 0x6754_5243, greenTrc, curveSize);
        putTag(bytes, 5, 0x6254_5243, blueTrc, curveSize);
        putTag(bytes, 6, IccCrdInfo.TYPE_CRDI, crdi, crdiSize);
        putXyz(bytes, redXyz, 0.4360657f, 0.2224884f, 0.0139160f);
        putXyz(bytes, greenXyz, 0.3851477f, 0.7168732f, 0.0971045f);
        putXyz(bytes, blueXyz, 0.1430664f, 0.0606084f, 0.7141733f);
        putIdentityCurve(bytes, redTrc);
        putIdentityCurve(bytes, greenTrc);
        putIdentityCurve(bytes, blueTrc);
        putSignature(bytes, crdi, "crdi");
        putU32(bytes, crdi + 8, 7);
        putAscii32(bytes, crdi + 12, "Himari");
        putU32(bytes, crdi + 19, 14);
        putAscii32(bytes, crdi + 23, "PerceptualCRD");
        putU32(bytes, crdi + 37, 0);
        putU32(bytes, crdi + 41, 0);
        putU32(bytes, crdi + 45, 0);
        return bytes;
    }

    /// Builds a matrix/TRC profile with a `scrd` screening description.
    private static byte[] matrixWithScreeningDescription() {
        int tagCount = 7;
        int table = 132 + tagCount * 12;
        int xyzSize = 20;
        int curveSize = 12;
        int scrdSize = 24;
        int size = table + 3 * xyzSize + 3 * curveSize + scrdSize;
        byte[] bytes = new byte[size];
        putU32(bytes, 0, size);
        putU32(bytes, 8, 0x0240_0000);
        putSignature(bytes, 12, "mntr");
        putSignature(bytes, 16, "RGB ");
        putSignature(bytes, 20, "XYZ ");
        putSignature(bytes, 36, "acsp");
        putS15(bytes, 68, 0.9642f);
        putS15(bytes, 72, 1.0f);
        putS15(bytes, 76, 0.8249f);
        putU32(bytes, 128, tagCount);
        int redXyz = table;
        int greenXyz = redXyz + xyzSize;
        int blueXyz = greenXyz + xyzSize;
        int redTrc = blueXyz + xyzSize;
        int greenTrc = redTrc + curveSize;
        int blueTrc = greenTrc + curveSize;
        int scrd = blueTrc + curveSize;
        putTag(bytes, 0, 0x7258_595A, redXyz, xyzSize);
        putTag(bytes, 1, 0x6758_595A, greenXyz, xyzSize);
        putTag(bytes, 2, 0x6258_595A, blueXyz, xyzSize);
        putTag(bytes, 3, 0x7254_5243, redTrc, curveSize);
        putTag(bytes, 4, 0x6754_5243, greenTrc, curveSize);
        putTag(bytes, 5, 0x6254_5243, blueTrc, curveSize);
        putTag(bytes, 6, 0x7363_7264, scrd, scrdSize);
        putXyz(bytes, redXyz, 0.4360657f, 0.2224884f, 0.0139160f);
        putXyz(bytes, greenXyz, 0.3851477f, 0.7168732f, 0.0971045f);
        putXyz(bytes, blueXyz, 0.1430664f, 0.0606084f, 0.7141733f);
        putIdentityCurve(bytes, redTrc);
        putIdentityCurve(bytes, greenTrc);
        putIdentityCurve(bytes, blueTrc);
        putSignature(bytes, scrd, "text");
        putAscii32(bytes, scrd + 8, "Default screens");
        return bytes;
    }

    /// Builds a CMYK header plus one `mft1` AToB0 tag.
    private static byte[] cmykMft1A2b0() {
        return cmykMft1Forward(0x4132_4230, false);
    }

    /// Builds a CMYK header plus a CLUT-only `mAB ` AToB0 tag.
    private static byte[] cmykMabA2b0() {
        int grid = 2;
        int clutValues = grid * grid * grid * grid * 3;
        int mabSize = 32 + 20 + clutValues;
        int tagCount = 1;
        int table = 132 + tagCount * 12;
        int size = table + mabSize;
        byte[] bytes = new byte[size];
        writeCmykHeader(bytes, size, tagCount, false);
        putTag(bytes, 0, 0x4132_4230, table, mabSize);
        writeCmykMabForward(bytes, table, grid);
        return bytes;
    }

    /// Builds a CMYK `mAB ` AToB0 tag whose 3×4 matrix doubles X.
    private static byte[] cmykMabA2b0Matrix() {
        int grid = 2;
        int clutValues = grid * grid * grid * grid * 3;
        int clutSize = 20 + clutValues;
        int matrixSize = 48;
        int mabSize = 32 + matrixSize + clutSize;
        int tagCount = 1;
        int table = 132 + tagCount * 12;
        int size = table + mabSize;
        byte[] bytes = new byte[size];
        writeCmykHeader(bytes, size, tagCount, false);
        putTag(bytes, 0, 0x4132_4230, table, mabSize);
        putSignature(bytes, table, "mAB ");
        bytes[table + 8] = 4;
        bytes[table + 9] = 3;
        putU32(bytes, table + 16, 32);
        putU32(bytes, table + 24, 32 + matrixSize);
        putS15(bytes, table + 32, 2.0f);
        putS15(bytes, table + 36, 0.0f);
        putS15(bytes, table + 40, 0.0f);
        putS15(bytes, table + 44, 0.0f);
        putS15(bytes, table + 48, 0.0f);
        putS15(bytes, table + 52, 1.0f);
        putS15(bytes, table + 56, 0.0f);
        putS15(bytes, table + 60, 0.0f);
        putS15(bytes, table + 64, 0.0f);
        putS15(bytes, table + 68, 0.0f);
        putS15(bytes, table + 72, 1.0f);
        putS15(bytes, table + 76, 0.0f);
        int clut = table + 32 + matrixSize;
        bytes[clut] = (byte) grid;
        bytes[clut + 1] = (byte) grid;
        bytes[clut + 2] = (byte) grid;
        bytes[clut + 3] = (byte) grid;
        bytes[clut + 16] = 1;
        int cursor = clut + 20;
        for (int cyan = 0; cyan < grid; cyan++) {
            for (int magenta = 0; magenta < grid; magenta++) {
                for (int yellow = 0; yellow < grid; yellow++) {
                    for (int black = 0; black < grid; black++) {
                        boolean uniqueCyan = cyan == 1 && magenta == 0 && yellow == 0 && black == 0;
                        bytes[cursor++] = (byte) (uniqueCyan ? 51 : 0);
                        bytes[cursor++] = 0;
                        bytes[cursor++] = 0;
                    }
                }
            }
        }
        return bytes;
    }

    /// Builds a CMYK profile with `mAB ` AToB0 and `mBA ` BToA0 tags.
    private static byte[] cmykMabA2b0AndMbaB2a0() {
        int grid = 2;
        int a2bClut = grid * grid * grid * grid * 3;
        int a2bSize = 32 + 20 + a2bClut;
        int b2aClut = grid * grid * grid * 4;
        int b2aSize = 32 + 20 + b2aClut;
        int tagCount = 2;
        int table = 132 + tagCount * 12;
        int size = table + a2bSize + b2aSize;
        byte[] bytes = new byte[size];
        writeCmykHeader(bytes, size, tagCount, false);
        int a2b0 = table;
        int b2a0 = a2b0 + a2bSize;
        putTag(bytes, 0, 0x4132_4230, a2b0, a2bSize);
        putTag(bytes, 1, 0x4232_4130, b2a0, b2aSize);
        writeCmykMabForward(bytes, a2b0, grid);
        writeCmykMbaInverse(bytes, b2a0, grid);
        return bytes;
    }

    /// Writes a CLUT-only 4×3 `mAB ` tag with unique cyan and black cells.
    ///
    /// @param bytes the profile bytes
    /// @param offset the tag offset
    /// @param grid the CLUT edge
    private static void writeCmykMabForward(byte[] bytes, int offset, int grid) {
        putSignature(bytes, offset, "mAB ");
        bytes[offset + 8] = 4;
        bytes[offset + 9] = 3;
        putU32(bytes, offset + 24, 32);
        int clut = offset + 32;
        bytes[clut] = (byte) grid;
        bytes[clut + 1] = (byte) grid;
        bytes[clut + 2] = (byte) grid;
        bytes[clut + 3] = (byte) grid;
        bytes[clut + 16] = 1;
        int cursor = clut + 20;
        for (int cyan = 0; cyan < grid; cyan++) {
            for (int magenta = 0; magenta < grid; magenta++) {
                for (int yellow = 0; yellow < grid; yellow++) {
                    for (int black = 0; black < grid; black++) {
                        boolean uniqueCyan = cyan == 1 && magenta == 0 && yellow == 0 && black == 0;
                        boolean uniqueBlack = cyan == 0 && magenta == 0 && yellow == 0 && black == 1;
                        bytes[cursor++] = (byte) (uniqueCyan ? 51 : 0);
                        bytes[cursor++] = (byte) (uniqueBlack ? 51 : 0);
                        bytes[cursor++] = 0;
                    }
                }
            }
        }
    }

    /// Writes a CLUT-only 3×4 `mBA ` tag whose Y=`1` plane is unique cyan/black.
    ///
    /// @param bytes the profile bytes
    /// @param offset the tag offset
    /// @param grid the CLUT edge
    private static void writeCmykMbaInverse(byte[] bytes, int offset, int grid) {
        putSignature(bytes, offset, "mBA ");
        bytes[offset + 8] = 3;
        bytes[offset + 9] = 4;
        putU32(bytes, offset + 24, 32);
        int clut = offset + 32;
        bytes[clut] = (byte) grid;
        bytes[clut + 1] = (byte) grid;
        bytes[clut + 2] = (byte) grid;
        bytes[clut + 16] = 1;
        int cursor = clut + 20;
        for (int x = 0; x < grid; x++) {
            for (int y = 0; y < grid; y++) {
                for (int z = 0; z < grid; z++) {
                    boolean unique = y == 1;
                    bytes[cursor++] = (byte) (unique ? 255 : 0);
                    bytes[cursor++] = 0;
                    bytes[cursor++] = 0;
                    bytes[cursor++] = (byte) (unique ? 128 : 0);
                }
            }
        }
    }

    /// Builds a CMYK header plus one `mft1` AToB1 tag.
    private static byte[] cmykMft1A2b1() {
        return cmykMft1Forward(0x4132_4231, false);
    }

    /// Builds a Lab PCS CMYK `mft1` AToB0 unique cell of 8-bit D50 white.
    private static byte[] cmykMft1LabA2b0() {
        return cmykMft1Forward(0x4132_4230, true);
    }

    /// Builds a CMYK `mft1` AToB0 tag, optionally with Lab PCS D50 white.
    ///
    /// @param signature AToB0 or AToB1
    /// @param labWhite `true` to write Lab D50 white in the unique cyan cell
    /// @return the profile bytes
    private static byte[] cmykMft1Forward(int signature, boolean labWhite) {
        int grid = 2;
        int inputTable = 256 * 4;
        int clutValues = grid * grid * grid * grid * 3;
        int outputTable = 256 * 3;
        int mft1Size = 48 + inputTable + clutValues + outputTable;
        int tagCount = 1;
        int table = 132 + tagCount * 12;
        int size = table + mft1Size;
        byte[] bytes = new byte[size];
        writeCmykHeader(bytes, size, tagCount, labWhite);
        int a2b0 = table;
        putTag(bytes, 0, signature, a2b0, mft1Size);
        writeCmykMft1Forward(bytes, a2b0, grid, labWhite);
        return bytes;
    }

    /// Builds a compact CMYK `mft2` AToB0 tag with two-entry identity tables.
    private static byte[] cmykMft2A2b0() {
        int grid = 2;
        int inputEntries = 2;
        int outputEntries = 2;
        int inputTable = inputEntries * 4;
        int clutValues = grid * grid * grid * grid * 3;
        int outputTable = outputEntries * 3;
        int mft2Size = 52 + (inputTable + clutValues + outputTable) * 2;
        int tagCount = 1;
        int table = 132 + tagCount * 12;
        int size = table + mft2Size;
        byte[] bytes = new byte[size];
        writeCmykHeader(bytes, size, tagCount, false);
        int a2b0 = table;
        putTag(bytes, 0, 0x4132_4230, a2b0, mft2Size);
        putSignature(bytes, a2b0, "mft2");
        bytes[a2b0 + 8] = 4;
        bytes[a2b0 + 9] = 3;
        bytes[a2b0 + 10] = (byte) grid;
        putU16(bytes, a2b0 + 48, inputEntries);
        putU16(bytes, a2b0 + 50, outputEntries);
        int cursor = a2b0 + 52;
        cursor = writePlanar16IdentityEntries(bytes, cursor, 4, inputEntries);
        for (int cyan = 0; cyan < grid; cyan++) {
            for (int magenta = 0; magenta < grid; magenta++) {
                for (int yellow = 0; yellow < grid; yellow++) {
                    for (int black = 0; black < grid; black++) {
                        boolean uniqueCyan = cyan == 1 && magenta == 0 && yellow == 0 && black == 0;
                        boolean uniqueBlack = cyan == 0 && magenta == 0 && yellow == 0 && black == 1;
                        putU16(bytes, cursor, uniqueCyan ? 13107 : 0);
                        putU16(bytes, cursor + 2, uniqueBlack ? 13107 : 0);
                        putU16(bytes, cursor + 4, 0);
                        cursor += 6;
                    }
                }
            }
        }
        writePlanar16IdentityEntries(bytes, cursor, 3, outputEntries);
        return bytes;
    }

    /// Builds a CMYK profile with both `mft1` AToB0 and BToA0 tags.
    private static byte[] cmykMft1A2b0AndB2a0() {
        int grid = 2;
        int a2bInput = 256 * 4;
        int a2bClut = grid * grid * grid * grid * 3;
        int a2bOutput = 256 * 3;
        int a2bSize = 48 + a2bInput + a2bClut + a2bOutput;
        int b2aInput = 256 * 3;
        int b2aClut = grid * grid * grid * 4;
        int b2aOutput = 256 * 4;
        int b2aSize = 48 + b2aInput + b2aClut + b2aOutput;
        int tagCount = 2;
        int table = 132 + tagCount * 12;
        int size = table + a2bSize + b2aSize;
        byte[] bytes = new byte[size];
        writeCmykHeader(bytes, size, tagCount, false);
        int a2b0 = table;
        int b2a0 = a2b0 + a2bSize;
        putTag(bytes, 0, 0x4132_4230, a2b0, a2bSize);
        putTag(bytes, 1, 0x4232_4130, b2a0, b2aSize);
        writeCmykMft1Forward(bytes, a2b0, grid, false);
        putSignature(bytes, b2a0, "mft1");
        bytes[b2a0 + 8] = 3;
        bytes[b2a0 + 9] = 4;
        bytes[b2a0 + 10] = (byte) grid;
        int cursor = writePlanar8IdentityChannels(bytes, b2a0 + 48, 3);
        for (int x = 0; x < grid; x++) {
            for (int y = 0; y < grid; y++) {
                for (int z = 0; z < grid; z++) {
                    boolean unique = y == 1;
                    bytes[cursor++] = (byte) (unique ? 255 : 0);
                    bytes[cursor++] = 0;
                    bytes[cursor++] = 0;
                    bytes[cursor++] = (byte) (unique ? 128 : 0);
                }
            }
        }
        writePlanar8IdentityChannels(bytes, cursor, 4);
        return bytes;
    }

    /// Builds a CMYK header with no LUT tags.
    private static byte[] cmykHeaderWithoutLut() {
        int tagCount = 1;
        int table = 132 + tagCount * 12;
        int curveSize = 12;
        int size = table + curveSize;
        byte[] bytes = new byte[size];
        writeCmykHeader(bytes, size, tagCount, false);
        putTag(bytes, 0, 0x6B54_5243, table, curveSize);
        putIdentityCurve(bytes, table);
        return bytes;
    }

    /// Writes a CMYK `mft1` 4×3 AToB table with unique cyan and black cells.
    ///
    /// @param bytes the profile bytes
    /// @param offset the tag offset
    /// @param grid the CLUT edge
    /// @param labWhite `true` to encode Lab D50 white in the unique cyan cell
    private static void writeCmykMft1Forward(byte[] bytes, int offset, int grid, boolean labWhite) {
        putSignature(bytes, offset, "mft1");
        bytes[offset + 8] = 4;
        bytes[offset + 9] = 3;
        bytes[offset + 10] = (byte) grid;
        int cursor = writePlanar8IdentityChannels(bytes, offset + 48, 4);
        for (int cyan = 0; cyan < grid; cyan++) {
            for (int magenta = 0; magenta < grid; magenta++) {
                for (int yellow = 0; yellow < grid; yellow++) {
                    for (int black = 0; black < grid; black++) {
                        boolean uniqueCyan = cyan == 1 && magenta == 0 && yellow == 0 && black == 0;
                        boolean uniqueBlack = cyan == 0 && magenta == 0 && yellow == 0 && black == 1;
                        if (labWhite && uniqueCyan) {
                            bytes[cursor++] = (byte) 255;
                            bytes[cursor++] = (byte) 128;
                            bytes[cursor++] = (byte) 128;
                        } else {
                            bytes[cursor++] = (byte) (uniqueCyan ? 51 : 0);
                            bytes[cursor++] = (byte) (uniqueBlack ? 51 : 0);
                            bytes[cursor++] = 0;
                        }
                    }
                }
            }
        }
        writePlanar8IdentityChannels(bytes, cursor, 3);
    }

    /// Writes a CMYK printer header with D50 illuminant.
    ///
    /// @param bytes the profile bytes
    /// @param size the declared profile size
    /// @param tagCount the tag-table length
    /// @param labPcs `true` to write a Lab PCS signature
    private static void writeCmykHeader(byte[] bytes, int size, int tagCount, boolean labPcs) {
        putU32(bytes, 0, size);
        putU32(bytes, 8, 0x0240_0000);
        putSignature(bytes, 12, "prtr");
        putSignature(bytes, 16, "CMYK");
        putSignature(bytes, 20, labPcs ? "Lab " : "XYZ ");
        putSignature(bytes, 36, "acsp");
        putS15(bytes, 68, 0.9642f);
        putS15(bytes, 72, 1.0f);
        putS15(bytes, 76, 0.8249f);
        putU32(bytes, 128, tagCount);
    }

    /// Writes `channels` planar 8-bit identity tables of 256 entries.
    ///
    /// @param bytes the profile bytes
    /// @param cursor the first table byte
    /// @param channels the number of tables
    /// @return the cursor after the tables
    private static int writePlanar8IdentityChannels(byte[] bytes, int cursor, int channels) {
        int offset = cursor;
        for (int channel = 0; channel < channels; channel++) {
            for (int index = 0; index < 256; index++) {
                bytes[offset++] = (byte) index;
            }
        }
        return offset;
    }

    /// Writes planar 16-bit identity tables of `entries` samples.
    ///
    /// @param bytes the profile bytes
    /// @param cursor the first table byte
    /// @param channels the number of tables
    /// @param entries samples per table
    /// @return the cursor after the tables
    private static int writePlanar16IdentityEntries(byte[] bytes, int cursor, int channels, int entries) {
        int offset = cursor;
        for (int channel = 0; channel < channels; channel++) {
            for (int index = 0; index < entries; index++) {
                int encoded = entries == 1 ? 0 : Math.round(index * 65535.0f / (entries - 1));
                putU16(bytes, offset, encoded);
                offset += 2;
            }
        }
        return offset;
    }

    /// Builds a compact ICC v2 GRAY matrix/TRC profile with identity `kTRC`.
    private static byte[] minimalGrayMatrixProfile() {
        int tagCount = 1;
        int table = 132 + tagCount * 12;
        int curveSize = 12;
        int size = table + curveSize;
        byte[] bytes = new byte[size];
        putU32(bytes, 0, size);
        putU32(bytes, 8, 0x0240_0000);
        putSignature(bytes, 12, "mntr");
        putSignature(bytes, 16, "GRAY");
        putSignature(bytes, 20, "XYZ ");
        putSignature(bytes, 36, "acsp");
        putS15(bytes, 68, 0.9642f);
        putS15(bytes, 72, 1.0f);
        putS15(bytes, 76, 0.8249f);
        putU32(bytes, 128, tagCount);
        int grayTrc = table;
        putTag(bytes, 0, 0x6B54_5243, grayTrc, curveSize);
        putIdentityCurve(bytes, grayTrc);
        return bytes;
    }

    /// Builds the sRGB matrix profile with a Lab PCS header.
    private static byte[] matrixWithLabPcs() {
        byte[] bytes = minimalSrgbMatrixProfile();
        putSignature(bytes, 20, "Lab ");
        return bytes;
    }

    /// Builds an `mft1` AToB0 tag whose unique cell is 8-bit ICC Lab D50 white.
    private static byte[] matrixPlusLabA2b0Mft1(boolean labPcs) {
        byte[] bytes = matrixPlusA2b0Mft1();
        if (labPcs) {
            putSignature(bytes, 20, "Lab ");
        }
        int tagCount = 7;
        int table = 132 + tagCount * 12;
        int a2b0 = table + 3 * 20 + 3 * 12;
        int clut = a2b0 + 48 + 256 * 3;
        int unique = clut + ((1 * 2 + 0) * 2 + 0) * 3;
        bytes[unique] = (byte) 255;
        bytes[unique + 1] = (byte) 128;
        bytes[unique + 2] = (byte) 128;
        return bytes;
    }

    /// Builds a Lab PCS matrix profile with one D50-white `ncl2` named color.
    private static byte[] matrixWithLabNamedColor() {
        byte[] bytes = matrixWithNamedColor();
        putSignature(bytes, 20, "Lab ");
        int tagCount = 7;
        int table = 132 + tagCount * 12;
        int ncl2 = table + 3 * 20 + 3 * 12;
        putU16(bytes, ncl2 + 116, 65280);
        putU16(bytes, ncl2 + 118, 32768);
        putU16(bytes, ncl2 + 120, 32768);
        return bytes;
    }

    /// Builds a Lab PCS `mft2` AToB0 tag whose unique cell is 16-bit ICC Lab D50 white.
    private static byte[] matrixPlusLabA2b0Mft2() {
        byte[] bytes = matrixPlusA2b0();
        putSignature(bytes, 20, "Lab ");
        int tagCount = 7;
        int table = 132 + tagCount * 12;
        int a2b0 = table + 3 * 20 + 3 * 12;
        int clut = a2b0 + 48 + 256 * 3 * 2;
        int unique = clut + ((1 * 2 + 0) * 2 + 0) * 6;
        putU16(bytes, unique, 65280);
        putU16(bytes, unique + 2, 32768);
        putU16(bytes, unique + 4, 32768);
        return bytes;
    }

    /// Builds a Lab PCS `mft2` BToA0 tag whose red=1 plane is unique device red.
    private static byte[] matrixPlusLabB2a0() {
        byte[] bytes = matrixPlusB2a0();
        putSignature(bytes, 20, "Lab ");
        int tagCount = 7;
        int table = 132 + tagCount * 12;
        int a2b0 = table + 3 * 20 + 3 * 12;
        int clut = a2b0 + 48 + 256 * 3 * 2;
        int grid = 2;
        for (int green = 0; green < grid; green++) {
            for (int blue = 0; blue < grid; blue++) {
                int cell = clut + ((1 * grid + green) * grid + blue) * 6;
                putU16(bytes, cell, 65535);
                putU16(bytes, cell + 2, 0);
                putU16(bytes, cell + 4, 0);
            }
        }
        return bytes;
    }

    /// Writes a UTF-16BE string without a BOM or terminating NUL.
    private static void putUtf16be(byte[] bytes, int offset, String value) {
        byte[] encoded = value.getBytes(java.nio.charset.StandardCharsets.UTF_16BE);
        System.arraycopy(encoded, 0, bytes, offset, encoded.length);
    }

    /// Returns the stored size of one `desc` element with a Unicode/ScriptCode tail.
    private static int descSize(String text) {
        return 12 + text.length() + 1 + 8 + 70;
    }

    /// Writes one `desc` element with an empty Unicode tail and a 67-byte ScriptCode field.
    private static int putDesc(byte[] bytes, int offset, String text) {
        byte[] encoded = text.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        putSignature(bytes, offset, "desc");
        putU32(bytes, offset + 8, encoded.length + 1);
        System.arraycopy(encoded, 0, bytes, offset + 12, encoded.length);
        return descSize(text);
    }

    /// Writes one `clrt`/`clot` colorant record.
    private static void putColorant(byte[] bytes, int offset, String name, float x, float y, float z) {
        putSignature(bytes, offset, "clrt");
        putU32(bytes, offset + 8, 1);
        putAscii32(bytes, offset + 12, name);
        putU16(bytes, offset + 44, Math.round(x * 32768.0f));
        putU16(bytes, offset + 46, Math.round(y * 32768.0f));
        putU16(bytes, offset + 48, Math.round(z * 32768.0f));
    }

    /// Reads a big-endian unsigned 32-bit integer from a test fixture.
    private static int u32(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) << 24)
                | ((bytes[offset + 1] & 0xFF) << 16)
                | ((bytes[offset + 2] & 0xFF) << 8)
                | (bytes[offset + 3] & 0xFF);
    }

    /// Writes a 32-byte 7-bit ASCII field.
    private static void putAscii32(byte[] bytes, int offset, String value) {
        byte[] encoded = value.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        System.arraycopy(encoded, 0, bytes, offset, encoded.length);
    }

    /// Builds the matrix profile plus a `B2D0` `mpet` that doubles the first channel.
    private static byte[] matrixPlusB2d0() {
        return matrixPlusMpe(0x4232_4430, false, 2.0f);
    }

    /// Builds the matrix profile plus a `D2B0` `mpet` that halves the first channel.
    private static byte[] matrixPlusD2b0() {
        return matrixPlusMpe(0x4432_4230, true, 0.5f);
    }

    /// Builds a 3-channel `mpet` of identity `cvst` plus a scale-on-X `matf`.
    private static byte[] matrixPlusMpe(int signature, boolean matrixFirst, float scale) {
        int tagCount = 7;
        int table = 132 + tagCount * 12;
        int xyzSize = 20;
        int curveSize = 12;
        int mpetSize = 144;
        int size = table + 3 * xyzSize + 3 * curveSize + mpetSize;
        byte[] bytes = new byte[size];
        System.arraycopy(minimalSrgbMatrixProfile(), 0, bytes, 0, 128);
        putU32(bytes, 0, size);
        putU32(bytes, 128, tagCount);
        int redXyz = table;
        int greenXyz = redXyz + xyzSize;
        int blueXyz = greenXyz + xyzSize;
        int redTrc = blueXyz + xyzSize;
        int greenTrc = redTrc + curveSize;
        int blueTrc = greenTrc + curveSize;
        int mpet = blueTrc + curveSize;
        putTag(bytes, 0, 0x7258_595A, redXyz, xyzSize);
        putTag(bytes, 1, 0x6758_595A, greenXyz, xyzSize);
        putTag(bytes, 2, 0x6258_595A, blueXyz, xyzSize);
        putTag(bytes, 3, 0x7254_5243, redTrc, curveSize);
        putTag(bytes, 4, 0x6754_5243, greenTrc, curveSize);
        putTag(bytes, 5, 0x6254_5243, blueTrc, curveSize);
        putTag(bytes, 6, signature, mpet, mpetSize);
        putXyz(bytes, redXyz, 0.4360657f, 0.2224884f, 0.0139160f);
        putXyz(bytes, greenXyz, 0.3851477f, 0.7168732f, 0.0971045f);
        putXyz(bytes, blueXyz, 0.1430664f, 0.0606084f, 0.7141733f);
        putIdentityCurve(bytes, redTrc);
        putIdentityCurve(bytes, greenTrc);
        putIdentityCurve(bytes, blueTrc);
        putSignature(bytes, mpet, "mpet");
        putU16(bytes, mpet + 8, 3);
        putU16(bytes, mpet + 10, 3);
        putU16(bytes, mpet + 12, 2);
        int first = 32;
        int second = 88;
        putU32(bytes, mpet + 16, first);
        putU32(bytes, mpet + 20, 56);
        putU32(bytes, mpet + 24, second);
        putU32(bytes, mpet + 28, 56);
        if (matrixFirst) {
            putMatf(bytes, mpet + first, scale);
            putCvstIdentity(bytes, mpet + second);
        } else {
            putCvstIdentity(bytes, mpet + first);
            putMatf(bytes, mpet + second, scale);
        }
        return bytes;
    }

    /// Builds the matrix profile plus a one-vertex `gbd ` D50 point.
    private static byte[] matrixPlusGbd() {
        int tagCount = 7;
        int table = 132 + tagCount * 12;
        int xyzSize = 20;
        int curveSize = 12;
        int gbdSize = 24;
        int size = table + 3 * xyzSize + 3 * curveSize + gbdSize;
        byte[] bytes = new byte[size];
        System.arraycopy(minimalSrgbMatrixProfile(), 0, bytes, 0, 128);
        putU32(bytes, 0, size);
        putU32(bytes, 128, tagCount);
        int redXyz = table;
        int greenXyz = redXyz + xyzSize;
        int blueXyz = greenXyz + xyzSize;
        int redTrc = blueXyz + xyzSize;
        int greenTrc = redTrc + curveSize;
        int blueTrc = greenTrc + curveSize;
        int gbd = blueTrc + curveSize;
        putTag(bytes, 0, 0x7258_595A, redXyz, xyzSize);
        putTag(bytes, 1, 0x6758_595A, greenXyz, xyzSize);
        putTag(bytes, 2, 0x6258_595A, blueXyz, xyzSize);
        putTag(bytes, 3, 0x7254_5243, redTrc, curveSize);
        putTag(bytes, 4, 0x6754_5243, greenTrc, curveSize);
        putTag(bytes, 5, 0x6254_5243, blueTrc, curveSize);
        putTag(bytes, 6, 0x6762_6420, gbd, gbdSize);
        putXyz(bytes, redXyz, 0.4360657f, 0.2224884f, 0.0139160f);
        putXyz(bytes, greenXyz, 0.3851477f, 0.7168732f, 0.0971045f);
        putXyz(bytes, blueXyz, 0.1430664f, 0.0606084f, 0.7141733f);
        putIdentityCurve(bytes, redTrc);
        putIdentityCurve(bytes, greenTrc);
        putIdentityCurve(bytes, blueTrc);
        putSignature(bytes, gbd, "gbd ");
        putU32(bytes, gbd + 8, 1);
        putS15(bytes, gbd + 12, 0.9642f);
        putS15(bytes, gbd + 16, 1.0f);
        putS15(bytes, gbd + 20, 0.8249f);
        return bytes;
    }

    /// Builds the matrix profile plus a `B2D0` 2³ `clut` with a unique green cell.
    private static byte[] matrixPlusB2d0Clut() {
        int tagCount = 7;
        int table = 132 + tagCount * 12;
        int xyzSize = 20;
        int curveSize = 12;
        int grid = 2;
        int clutSize = 28 + grid * grid * grid * 3;
        int mpetSize = 24 + clutSize;
        int size = table + 3 * xyzSize + 3 * curveSize + mpetSize;
        byte[] bytes = new byte[size];
        System.arraycopy(minimalSrgbMatrixProfile(), 0, bytes, 0, 128);
        putU32(bytes, 0, size);
        putU32(bytes, 128, tagCount);
        int redXyz = table;
        int greenXyz = redXyz + xyzSize;
        int blueXyz = greenXyz + xyzSize;
        int redTrc = blueXyz + xyzSize;
        int greenTrc = redTrc + curveSize;
        int blueTrc = greenTrc + curveSize;
        int mpet = blueTrc + curveSize;
        putTag(bytes, 0, 0x7258_595A, redXyz, xyzSize);
        putTag(bytes, 1, 0x6758_595A, greenXyz, xyzSize);
        putTag(bytes, 2, 0x6258_595A, blueXyz, xyzSize);
        putTag(bytes, 3, 0x7254_5243, redTrc, curveSize);
        putTag(bytes, 4, 0x6754_5243, greenTrc, curveSize);
        putTag(bytes, 5, 0x6254_5243, blueTrc, curveSize);
        putTag(bytes, 6, 0x4232_4430, mpet, mpetSize);
        putXyz(bytes, redXyz, 0.4360657f, 0.2224884f, 0.0139160f);
        putXyz(bytes, greenXyz, 0.3851477f, 0.7168732f, 0.0971045f);
        putXyz(bytes, blueXyz, 0.1430664f, 0.0606084f, 0.7141733f);
        putIdentityCurve(bytes, redTrc);
        putIdentityCurve(bytes, greenTrc);
        putIdentityCurve(bytes, blueTrc);
        putSignature(bytes, mpet, "mpet");
        putU16(bytes, mpet + 8, 3);
        putU16(bytes, mpet + 10, 3);
        putU16(bytes, mpet + 12, 1);
        putU32(bytes, mpet + 16, 24);
        putU32(bytes, mpet + 20, clutSize);
        int clut = mpet + 24;
        putSignature(bytes, clut, "clut");
        bytes[clut + 8] = (byte) grid;
        bytes[clut + 9] = (byte) grid;
        bytes[clut + 10] = (byte) grid;
        bytes[clut + 24] = 1;
        int cursor = clut + 28;
        for (int red = 0; red < grid; red++) {
            for (int green = 0; green < grid; green++) {
                for (int blue = 0; blue < grid; blue++) {
                    boolean unique = red == 1 && green == 0 && blue == 0;
                    bytes[cursor++] = 0;
                    bytes[cursor++] = (byte) (unique ? 255 : 0);
                    bytes[cursor++] = 0;
                }
            }
        }
        return bytes;
    }

    /// Builds the matrix profile plus a `B2D0` `samf` table that halves the first channel.
    private static byte[] matrixPlusB2d0Samf() {
        byte[] bytes = matrixPlusMpe(0x4232_4430, false, 1.0f);
        int mpet = 132 + 7 * 12 + 3 * 20 + 3 * 12;
        putCvstSampledHalf(bytes, mpet + 32);
        return bytes;
    }

    /// Writes one 3-channel `cvst` of 2-entry `samf` tables that map 1 to 0.5.
    private static void putCvstSampledHalf(byte[] bytes, int offset) {
        putSignature(bytes, offset, "cvst");
        int cursor = offset + 8;
        for (int index = 0; index < 3; index++) {
            putSignature(bytes, cursor, "samf");
            putU16(bytes, cursor + 8, 2);
            putU16(bytes, cursor + 12, 0);
            putU16(bytes, cursor + 14, 32768);
            cursor += 16;
        }
    }

    /// Builds the matrix profile plus a `B2D0` type-1 `curf` that doubles the first channel.
    private static byte[] matrixPlusB2d0CurfType1() {
        int tagCount = 7;
        int table = 132 + tagCount * 12;
        int xyzSize = 20;
        int curveSize = 12;
        int cvstSize = 8 + 3 * 32;
        int mpetSize = 24 + cvstSize;
        int size = table + 3 * xyzSize + 3 * curveSize + mpetSize;
        byte[] bytes = new byte[size];
        System.arraycopy(minimalSrgbMatrixProfile(), 0, bytes, 0, 128);
        putU32(bytes, 0, size);
        putU32(bytes, 128, tagCount);
        int redXyz = table;
        int greenXyz = redXyz + xyzSize;
        int blueXyz = greenXyz + xyzSize;
        int redTrc = blueXyz + xyzSize;
        int greenTrc = redTrc + curveSize;
        int blueTrc = greenTrc + curveSize;
        int mpet = blueTrc + curveSize;
        putTag(bytes, 0, 0x7258_595A, redXyz, xyzSize);
        putTag(bytes, 1, 0x6758_595A, greenXyz, xyzSize);
        putTag(bytes, 2, 0x6258_595A, blueXyz, xyzSize);
        putTag(bytes, 3, 0x7254_5243, redTrc, curveSize);
        putTag(bytes, 4, 0x6754_5243, greenTrc, curveSize);
        putTag(bytes, 5, 0x6254_5243, blueTrc, curveSize);
        putTag(bytes, 6, 0x4232_4430, mpet, mpetSize);
        putXyz(bytes, redXyz, 0.4360657f, 0.2224884f, 0.0139160f);
        putXyz(bytes, greenXyz, 0.3851477f, 0.7168732f, 0.0971045f);
        putXyz(bytes, blueXyz, 0.1430664f, 0.0606084f, 0.7141733f);
        putIdentityCurve(bytes, redTrc);
        putIdentityCurve(bytes, greenTrc);
        putIdentityCurve(bytes, blueTrc);
        putSignature(bytes, mpet, "mpet");
        putU16(bytes, mpet + 8, 3);
        putU16(bytes, mpet + 10, 3);
        putU16(bytes, mpet + 12, 1);
        putU32(bytes, mpet + 16, 24);
        putU32(bytes, mpet + 20, cvstSize);
        putCvstType1(bytes, mpet + 24, 2.0f);
        return bytes;
    }

    /// Builds the matrix profile plus identity `matf` then type-1 `curf` on `B2D0`.
    private static byte[] matrixPlusB2d0CurfType1AfterMatrix() {
        int tagCount = 7;
        int table = 132 + tagCount * 12;
        int xyzSize = 20;
        int curveSize = 12;
        int cvstSize = 8 + 3 * 32;
        int matfSize = 56;
        int first = 32;
        int second = first + matfSize;
        int mpetSize = second + cvstSize;
        int size = table + 3 * xyzSize + 3 * curveSize + mpetSize;
        byte[] bytes = new byte[size];
        System.arraycopy(minimalSrgbMatrixProfile(), 0, bytes, 0, 128);
        putU32(bytes, 0, size);
        putU32(bytes, 128, tagCount);
        int redXyz = table;
        int greenXyz = redXyz + xyzSize;
        int blueXyz = greenXyz + xyzSize;
        int redTrc = blueXyz + xyzSize;
        int greenTrc = redTrc + curveSize;
        int blueTrc = greenTrc + curveSize;
        int mpet = blueTrc + curveSize;
        putTag(bytes, 0, 0x7258_595A, redXyz, xyzSize);
        putTag(bytes, 1, 0x6758_595A, greenXyz, xyzSize);
        putTag(bytes, 2, 0x6258_595A, blueXyz, xyzSize);
        putTag(bytes, 3, 0x7254_5243, redTrc, curveSize);
        putTag(bytes, 4, 0x6754_5243, greenTrc, curveSize);
        putTag(bytes, 5, 0x6254_5243, blueTrc, curveSize);
        putTag(bytes, 6, 0x4232_4430, mpet, mpetSize);
        putXyz(bytes, redXyz, 0.4360657f, 0.2224884f, 0.0139160f);
        putXyz(bytes, greenXyz, 0.3851477f, 0.7168732f, 0.0971045f);
        putXyz(bytes, blueXyz, 0.1430664f, 0.0606084f, 0.7141733f);
        putIdentityCurve(bytes, redTrc);
        putIdentityCurve(bytes, greenTrc);
        putIdentityCurve(bytes, blueTrc);
        putSignature(bytes, mpet, "mpet");
        putU16(bytes, mpet + 8, 3);
        putU16(bytes, mpet + 10, 3);
        putU16(bytes, mpet + 12, 2);
        putU32(bytes, mpet + 16, first);
        putU32(bytes, mpet + 20, matfSize);
        putU32(bytes, mpet + 24, second);
        putU32(bytes, mpet + 28, cvstSize);
        putMatf(bytes, mpet + first, 1.0f);
        putCvstType1(bytes, mpet + second, 2.0f);
        return bytes;
    }

    /// Writes one 3-channel `cvst` of type-1 `curf` curves `Y = aX`.
    private static void putCvstType1(byte[] bytes, int offset, float firstScale) {
        putSignature(bytes, offset, "cvst");
        int cursor = offset + 8;
        for (int index = 0; index < 3; index++) {
            putSignature(bytes, cursor, "curf");
            putU16(bytes, cursor + 8, 1);
            putS15(bytes, cursor + 12, 1.0f);
            putS15(bytes, cursor + 16, index == 0 ? firstScale : 1.0f);
            putS15(bytes, cursor + 20, 0.0f);
            putS15(bytes, cursor + 24, 0.0f);
            putS15(bytes, cursor + 28, 0.0f);
            cursor += 32;
        }
    }

    /// Writes one identity 3-channel `cvst` of type-0 `curf` curves.
    private static void putCvstIdentity(byte[] bytes, int offset) {
        putSignature(bytes, offset, "cvst");
        int cursor = offset + 8;
        for (int index = 0; index < 3; index++) {
            putSignature(bytes, cursor, "curf");
            putS15(bytes, cursor + 12, 1.0f);
            cursor += 16;
        }
    }

    /// Writes one 3×4 `matf` that scales the first channel.
    private static void putMatf(byte[] bytes, int offset, float scale) {
        putSignature(bytes, offset, "matf");
        putS15(bytes, offset + 8, scale);
        putS15(bytes, offset + 28, 1.0f);
        putS15(bytes, offset + 48, 1.0f);
    }

    /// Writes one tag-table entry.
    private static void putTag(byte[] bytes, int index, int signature, int offset, int size) {
        int entry = 132 + index * 12;
        putU32(bytes, entry, signature);
        putU32(bytes, entry + 4, offset);
        putU32(bytes, entry + 8, size);
    }

    /// Writes one `XYZType` tag.
    private static void putXyz(byte[] bytes, int offset, float x, float y, float z) {
        putSignature(bytes, offset, "XYZ ");
        putS15(bytes, offset + 8, x);
        putS15(bytes, offset + 12, y);
        putS15(bytes, offset + 16, z);
    }

    /// Builds the matrix profile with type-1 `para` TRCs (`Y = X^2`).
    private static byte[] matrixWithParaType1() {
        int tagCount = 6;
        int table = 132 + tagCount * 12;
        int xyzSize = 20;
        int curveSize = 24;
        int size = table + 3 * xyzSize + 3 * curveSize;
        byte[] bytes = new byte[size];
        putU32(bytes, 0, size);
        putU32(bytes, 8, 0x0240_0000);
        putSignature(bytes, 12, "mntr");
        putSignature(bytes, 16, "RGB ");
        putSignature(bytes, 20, "XYZ ");
        putSignature(bytes, 36, "acsp");
        putS15(bytes, 68, 0.9642f);
        putS15(bytes, 72, 1.0f);
        putS15(bytes, 76, 0.8249f);
        putU32(bytes, 128, tagCount);
        int redXyz = table;
        int greenXyz = redXyz + xyzSize;
        int blueXyz = greenXyz + xyzSize;
        int redTrc = blueXyz + xyzSize;
        int greenTrc = redTrc + curveSize;
        int blueTrc = greenTrc + curveSize;
        putTag(bytes, 0, 0x7258_595A, redXyz, xyzSize);
        putTag(bytes, 1, 0x6758_595A, greenXyz, xyzSize);
        putTag(bytes, 2, 0x6258_595A, blueXyz, xyzSize);
        putTag(bytes, 3, 0x7254_5243, redTrc, curveSize);
        putTag(bytes, 4, 0x6754_5243, greenTrc, curveSize);
        putTag(bytes, 5, 0x6254_5243, blueTrc, curveSize);
        putXyz(bytes, redXyz, 0.4360657f, 0.2224884f, 0.0139160f);
        putXyz(bytes, greenXyz, 0.3851477f, 0.7168732f, 0.0971045f);
        putXyz(bytes, blueXyz, 0.1430664f, 0.0606084f, 0.7141733f);
        putParaType1(bytes, redTrc);
        putParaType1(bytes, greenTrc);
        putParaType1(bytes, blueTrc);
        return bytes;
    }

    /// Writes a type-1 `para` tag `Y = X^2`.
    private static void putParaType1(byte[] bytes, int offset) {
        putSignature(bytes, offset, "para");
        putU16(bytes, offset + 8, 1);
        putS15(bytes, offset + 12, 2.0f);
        putS15(bytes, offset + 16, 1.0f);
        putS15(bytes, offset + 20, 0.0f);
    }

    /// Writes one identity `curv` tag.
    private static void putIdentityCurve(byte[] bytes, int offset) {
        putSignature(bytes, offset, "curv");
        putU32(bytes, offset + 8, 0);
    }

    /// Writes a four-character signature.
    private static void putSignature(byte[] bytes, int offset, String value) {
        byte[] encoded = value.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        System.arraycopy(encoded, 0, bytes, offset, 4);
    }

    /// Writes a big-endian unsigned 32-bit integer.
    private static void putU32(byte[] bytes, int offset, int value) {
        bytes[offset] = (byte) (value >>> 24);
        bytes[offset + 1] = (byte) (value >>> 16);
        bytes[offset + 2] = (byte) (value >>> 8);
        bytes[offset + 3] = (byte) value;
    }

    /// Writes a big-endian `s15Fixed16` number.
    private static void putS15(byte[] bytes, int offset, float value) {
        putU32(bytes, offset, Math.round(value * 65536.0f));
    }
}

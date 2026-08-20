package org.glavo.himari.graphics;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;

/// Parses a bounded ICC v2/v4 matrix/TRC RGB or GRAY profile, or a CMYK LUT profile, and converts samples to extended-linear sRGB.
///
/// Profiles are treated as untrusted input. The parser accepts RGB-to-XYZ or RGB-to-Lab matrix profiles with
/// `curv` or parametric `para` tone curves (function types 0–4), optional `mft1`/`mft2`
/// AToB/BToA CLUTs, optional `ncl2` named colors, optional `chad` chromatic-adaptation
/// matrices, optional `wtpt`/`bkpt` media white and black points, optional `lumi`
/// luminance, optional `view` viewing conditions, optional `desc`/`vued`/`cprt` ASCII
/// descriptions (`desc`/`text`/`mluc`), optional `tech`/`dmnd`/`dmdd` device identity, optional `calt`
/// calibration date, optional `meas` measurement, optional `gamt` gamut table,
/// optional `pre0`/`pre1`/`pre2` preview LUTs, optional `cicp` code points,
/// optional `resp` output-response curves, optional `clrt`/`clot` colorant tables,
/// optional `pseq` profile-sequence descriptions, optional `psid` profile identifiers,
/// optional `meta` dictionary metadata, optional `chrm` chromaticities,
/// optional `clro` colorant order, optional `scrn` screening, optional `bfd ` UCR/BG curves,
/// optional `targ` characterization target, optional `crdi`/`ps2s`/`ps2i` `data` payloads,
/// optional `psd0`/`psd1`/`psd2`/`psd3` PostScript descriptions, optional `devs` device
/// settings, optional `crdInfoType` companion names, optional `scrd` screening description,
/// optional CMYK `mft1`/`mft2`/`mAB `/`mBA ` 4×3 and 3×4 LUTs, optional
/// `B2D0`–`B2D3`/`D2B0`–`D2B3` `mpet` 3-channel curve (`curf` type 0/1 or `samf`)/CLUT/matrix
/// pipelines, optional `gbd `
/// gamut-boundary vertices, and
/// rejects larger or malformed tables.
///
/// @param size the declared profile size in bytes
/// @param version the packed ICC version word
/// @param deviceColorSpace the 4-character data color space
/// @param pcs the 4-character profile connection space
/// @param illuminantX the header illuminant X
/// @param illuminantY the header illuminant Y
/// @param illuminantZ the header illuminant Z
/// @param redX the red primary X in PCS XYZ
/// @param redY the red primary Y in PCS XYZ
/// @param redZ the red primary Z in PCS XYZ
/// @param greenX the green primary X in PCS XYZ
/// @param greenY the green primary Y in PCS XYZ
/// @param greenZ the green primary Z in PCS XYZ
/// @param blueX the blue primary X in PCS XYZ
/// @param blueY the blue primary Y in PCS XYZ
/// @param blueZ the blue primary Z in PCS XYZ
/// @param redTrc the red tone curve
/// @param greenTrc the green tone curve
/// @param blueTrc the blue tone curve
/// @param sha256 the lowercase SHA-256 digest of the exact profile bytes
/// @param clut the optional AToB0 `mft2` CLUT, or `null` when unused
/// @param clutAToB1 the optional AToB1 `mft2` CLUT, or `null` when unused
/// @param clutBToA0 the optional BToA0 `mft2` CLUT, or `null` when unused
/// @param clutBToA1 the optional BToA1 `mft2` CLUT, or `null` when unused
/// @param clutAToB2 the optional AToB2 `mft2` CLUT, or `null` when unused
/// @param clutBToA2 the optional BToA2 `mft2` CLUT, or `null` when unused
/// @param clutAToB3 the optional AToB3 `mft2` CLUT, or `null` when unused
/// @param clutBToA3 the optional BToA3 `mft2` CLUT, or `null` when unused
/// @param namedColors the optional `ncl2` table, or `null` when unused
/// @param chromaticAdaptation the optional `chad` 3×3 row-major matrix, or `null` when unused
/// @param mediaWhite the optional `wtpt` XYZ, or `null` when unused
/// @param mediaBlack the optional `bkpt` XYZ, or `null` when unused
/// @param luminance the optional `lumi` XYZ, or `null` when unused
/// @param viewingConditions the optional `view` tag, or `null` when unused
/// @param description the optional `desc` ASCII profile description, or `null` when unused
/// @param viewingDescription the optional `vued` ASCII viewing-condition description, or `null`
/// @param copyright the optional `cprt` ASCII copyright, or `null` when unused
/// @param technology the optional `tech` 4-character signature, or `null` when unused
/// @param deviceManufacturer the optional `dmnd` ASCII manufacturer, or `null` when unused
/// @param deviceModel the optional `dmdd` ASCII model, or `null` when unused
/// @param calibrationDate the optional `calt` calibration date-time, or `null` when unused
/// @param measurement the optional `meas` measurement, or `null` when unused
/// @param gamut the optional `gamt` 3×1 table, or `null` when unused
/// @param preview0 the optional `pre0` preview LUT, or `null` when unused
/// @param preview1 the optional `pre1` preview LUT, or `null` when unused
/// @param preview2 the optional `pre2` preview LUT, or `null` when unused
/// @param cicp the optional `cicp` code points, or `null` when unused
/// @param outputResponse the optional `resp` curves, or `null` when unused
/// @param colorants the optional `clrt` table, or `null` when unused
/// @param colorantsOut the optional `clot` table, or `null` when unused
/// @param profileSequence the optional `pseq` sequence, or `null` when unused
/// @param profileSequenceIds the optional `psid` identifiers, or `null` when unused
/// @param metadata the optional `meta` dictionary, or `null` when unused
/// @param chromaticity the optional `chrm` chromaticities, or `null` when unused
/// @param colorantOrder the optional `clro` order, or `null` when unused
/// @param screening the optional `scrn` table, or `null` when unused
/// @param ucrBg the optional `bfd ` UCR/BG curves, or `null` when unused
/// @param characterizationTarget the optional `targ` ASCII target name, or `null` when unused
/// @param colorRenderingDict the optional `crdi` data, or `null` when unused
/// @param postScript2Csa the optional `ps2s` data, or `null` when unused
/// @param postScript2Crd the optional `ps2i` data, or `null` when unused
/// @param postScriptDesc0 the optional `psd0` ASCII description, or `null` when unused
/// @param postScriptDesc1 the optional `psd1` ASCII description, or `null` when unused
/// @param postScriptDesc2 the optional `psd2` ASCII description, or `null` when unused
/// @param postScriptDesc3 the optional `psd3` ASCII description, or `null` when unused
/// @param deviceSettings the optional `devs` table, or `null` when unused
/// @param crdInfo the optional `crdInfoType` companion names, or `null` when unused
/// @param screeningDescription the optional `scrd` ASCII description, or `null` when unused
/// @param cmyk the optional CMYK AToB LUT, or `null` when unused
/// @param cmykBToA the optional CMYK BToA LUT, or `null` when unused
/// @param bToD0 the first present `B2D0`–`B2D3` `mpet` pipeline, or `null` when unused
/// @param dToB0 the first present `D2B0`–`D2B3` `mpet` pipeline, or `null` when unused
/// @param gamutBoundary the optional `gbd ` vertices, or `null` when unused
@NotNullByDefault
public record IccProfile(
        int size,
        int version,
        String deviceColorSpace,
        String pcs,
        float illuminantX,
        float illuminantY,
        float illuminantZ,
        float redX,
        float redY,
        float redZ,
        float greenX,
        float greenY,
        float greenZ,
        float blueX,
        float blueY,
        float blueZ,
        Curve redTrc,
        Curve greenTrc,
        Curve blueTrc,
        String sha256,
        @Nullable IccClut clut,
        @Nullable IccClut clutAToB1,
        @Nullable IccClut clutBToA0,
        @Nullable IccClut clutBToA1,
        @Nullable IccClut clutAToB2,
        @Nullable IccClut clutBToA2,
        @Nullable IccClut clutAToB3,
        @Nullable IccClut clutBToA3,
        @Nullable IccNamedColors namedColors,
        float @Nullable @Unmodifiable [] chromaticAdaptation,
        float @Nullable @Unmodifiable [] mediaWhite,
        float @Nullable @Unmodifiable [] mediaBlack,
        float @Nullable @Unmodifiable [] luminance,
        @Nullable IccViewingConditions viewingConditions,
        @Nullable String description,
        @Nullable String viewingDescription,
        @Nullable String copyright,
        @Nullable String technology,
        @Nullable String deviceManufacturer,
        @Nullable String deviceModel,
        @Nullable IccDateTime calibrationDate,
        @Nullable IccMeasurement measurement,
        @Nullable IccGamut gamut,
        @Nullable IccClut preview0,
        @Nullable IccClut preview1,
        @Nullable IccClut preview2,
        @Nullable IccCicp cicp,
        @Nullable IccOutputResponse outputResponse,
        @Nullable IccColorants colorants,
        @Nullable IccColorants colorantsOut,
        @Nullable IccProfileSequence profileSequence,
        @Nullable IccProfileSequenceIds profileSequenceIds,
        @Nullable IccMetadata metadata,
        @Nullable IccChromaticity chromaticity,
        @Nullable IccColorantOrder colorantOrder,
        @Nullable IccScreening screening,
        @Nullable IccUcrBg ucrBg,
        @Nullable String characterizationTarget,
        @Nullable IccData colorRenderingDict,
        @Nullable IccData postScript2Csa,
        @Nullable IccData postScript2Crd,
        @Nullable String postScriptDesc0,
        @Nullable String postScriptDesc1,
        @Nullable String postScriptDesc2,
        @Nullable String postScriptDesc3,
        @Nullable IccDeviceSettings deviceSettings,
        @Nullable IccCrdInfo crdInfo,
        @Nullable String screeningDescription,
        @Nullable IccCmykLut cmyk,
        @Nullable IccCmykLut cmykBToA,
        @Nullable IccMpe bToD0,
        @Nullable IccMpe dToB0,
        @Nullable IccGamutBoundary gamutBoundary
) {
    /// Maximum accepted profile size.
    public static final int MAX_PROFILE_BYTES = 1_048_576;

    /// Maximum accepted tag-table entries.
    public static final int MAX_TAGS = 64;

    /// Maximum accepted `curv` table entries.
    public static final int MAX_CURVE_ENTRIES = 4096;

    /// ICC `'acsp'` magic.
    private static final int MAGIC_ACSP = 0x6163_7370;

    /// ICC `'RGB '`.
    private static final int SPACE_RGB = 0x5247_4220;

    /// ICC `'GRAY'`.
    private static final int SPACE_GRAY = 0x4752_4159;

    /// ICC `'CMYK'`.
    private static final int SPACE_CMYK = 0x434D_594B;

    /// ICC `'XYZ '`.
    private static final int SPACE_XYZ = 0x5859_5A20;

    /// ICC `'Lab '`.
    private static final int SPACE_LAB = 0x4C61_6220;

    /// CIE Lab `ε` = `216/24389`.
    private static final float LAB_EPSILON = 216.0f / 24389.0f;

    /// CIE Lab `κ` = `24389/27`.
    private static final float LAB_KAPPA = 24389.0f / 27.0f;

    /// Tag `'rXYZ'`.
    private static final int TAG_R_XYZ = 0x7258_595A;

    /// Tag `'gXYZ'`.
    private static final int TAG_G_XYZ = 0x6758_595A;

    /// Tag `'bXYZ'`.
    private static final int TAG_B_XYZ = 0x6258_595A;

    /// Tag `'rTRC'`.
    private static final int TAG_R_TRC = 0x7254_5243;

    /// Tag `'gTRC'`.
    private static final int TAG_G_TRC = 0x6754_5243;

    /// Tag `'bTRC'`.
    private static final int TAG_B_TRC = 0x6254_5243;

    /// Tag `'kTRC'`.
    private static final int TAG_K_TRC = 0x6B54_5243;

    /// Tag `'A2B0'`.
    private static final int TAG_A2B0 = 0x4132_4230;

    /// Tag `'A2B1'`.
    private static final int TAG_A2B1 = 0x4132_4231;

    /// Tag `'B2A0'`.
    private static final int TAG_B2A0 = 0x4232_4130;

    /// Tag `'B2A1'`.
    private static final int TAG_B2A1 = 0x4232_4131;

    /// Tag `'A2B2'`.
    private static final int TAG_A2B2 = 0x4132_4232;

    /// Tag `'B2A2'`.
    private static final int TAG_B2A2 = 0x4232_4132;

    /// Tag `'A2B3'`.
    private static final int TAG_A2B3 = 0x4132_4233;

    /// Tag `'B2A3'`.
    private static final int TAG_B2A3 = 0x4232_4133;

    /// Tag `'ncl2'`.
    private static final int TAG_NCL2 = 0x6E63_6C32;

    /// Tag `'ncl '`.
    private static final int TAG_NCL = 0x6E63_6C20;

    /// Tag `'chad'`.
    private static final int TAG_CHAD = 0x6368_6164;

    /// Tag `'wtpt'`.
    private static final int TAG_WTPT = 0x7774_7074;

    /// Tag `'bkpt'`.
    private static final int TAG_BKPT = 0x626B_7074;

    /// Tag `'lumi'`.
    private static final int TAG_LUMI = 0x6C75_6D69;

    /// Tag `'view'`.
    private static final int TAG_VIEW = IccViewingConditions.SIGNATURE;

    /// Tag `'desc'`.
    private static final int TAG_DESC = 0x6465_7363;

    /// Tag `'vued'`.
    private static final int TAG_VUED = 0x7675_6564;

    /// Tag `'cprt'`.
    private static final int TAG_CPRT = 0x6370_7274;

    /// Tag `'tech'`.
    private static final int TAG_TECH = 0x7465_6368;

    /// Tag `'dmnd'`.
    private static final int TAG_DMND = 0x646D_6E64;

    /// Tag `'dmdd'`.
    private static final int TAG_DMDD = 0x646D_6464;

    /// Tag `'calt'`.
    private static final int TAG_CALT = 0x6361_6C74;

    /// Tag `'meas'`.
    private static final int TAG_MEAS = IccMeasurement.SIGNATURE;

    /// Tag `'gamt'`.
    private static final int TAG_GAMT = IccGamut.SIGNATURE;

    /// Tag `'pre0'`.
    private static final int TAG_PRE0 = 0x7072_6530;

    /// Tag `'pre1'`.
    private static final int TAG_PRE1 = 0x7072_6531;

    /// Tag `'pre2'`.
    private static final int TAG_PRE2 = 0x7072_6532;

    /// Tag `'cicp'`.
    private static final int TAG_CICP = IccCicp.SIGNATURE;

    /// Tag `'resp'`.
    private static final int TAG_RESP = IccOutputResponse.SIGNATURE;

    /// Tag `'clrt'`.
    private static final int TAG_CLRT = IccColorants.TAG_CLRT;

    /// Tag `'clot'`.
    private static final int TAG_CLOT = IccColorants.TAG_CLOT;

    /// Tag `'pseq'`.
    private static final int TAG_PSEQ = IccProfileSequence.SIGNATURE;

    /// Tag `'psid'`.
    private static final int TAG_PSID = IccProfileSequenceIds.SIGNATURE;

    /// Tag `'meta'`.
    private static final int TAG_META = IccMetadata.SIGNATURE;

    /// Tag `'chrm'`.
    private static final int TAG_CHRM = IccChromaticity.SIGNATURE;

    /// Tag `'clro'`.
    private static final int TAG_CLRO = IccColorantOrder.SIGNATURE;

    /// Tag `'scrn'`.
    private static final int TAG_SCRN = IccScreening.SIGNATURE;

    /// Tag `'bfd '`.
    private static final int TAG_BFD = IccUcrBg.SIGNATURE;

    /// Tag `'targ'`.
    private static final int TAG_TARG = 0x7461_7267;

    /// Tag `'crdi'`.
    private static final int TAG_CRDI = IccData.TAG_CRDI;

    /// Tag `'ps2s'`.
    private static final int TAG_PS2S = IccData.TAG_PS2S;

    /// Tag `'ps2i'`.
    private static final int TAG_PS2I = IccData.TAG_PS2I;

    /// Tag `'psd0'`.
    private static final int TAG_PSD0 = 0x7073_6430;

    /// Tag `'psd1'`.
    private static final int TAG_PSD1 = 0x7073_6431;

    /// Tag `'psd2'`.
    private static final int TAG_PSD2 = 0x7073_6432;

    /// Tag `'psd3'`.
    private static final int TAG_PSD3 = 0x7073_6433;

    /// Tag `'devs'`.
    private static final int TAG_DEVS = IccDeviceSettings.SIGNATURE;

    /// Tag `'scrd'`.
    private static final int TAG_SCRD = 0x7363_7264;

    /// Tag `'B2D0'`.
    private static final int TAG_B2D0 = IccMpe.TAG_B2D0;

    /// Tag `'B2D1'`.
    private static final int TAG_B2D1 = IccMpe.TAG_B2D1;

    /// Tag `'B2D2'`.
    private static final int TAG_B2D2 = IccMpe.TAG_B2D2;

    /// Tag `'B2D3'`.
    private static final int TAG_B2D3 = IccMpe.TAG_B2D3;

    /// Tag `'D2B0'`.
    private static final int TAG_D2B0 = IccMpe.TAG_D2B0;

    /// Tag `'D2B1'`.
    private static final int TAG_D2B1 = IccMpe.TAG_D2B1;

    /// Tag `'D2B2'`.
    private static final int TAG_D2B2 = IccMpe.TAG_D2B2;

    /// Tag `'D2B3'`.
    private static final int TAG_D2B3 = IccMpe.TAG_D2B3;

    /// Tag `'gbd '`.
    private static final int TAG_GBD = IccGamutBoundary.SIGNATURE;

    /// Type `'sig '`.
    private static final int TYPE_SIG = 0x7369_6720;

    /// Type `'sf32'`.
    private static final int TYPE_SF32 = 0x7366_3332;

    /// D50 X used when a `chad` matrix has already mapped PCS to D50.
    private static final float D50_X = 0.9642f;

    /// D50 Y.
    private static final float D50_Y = 1.0f;

    /// D50 Z.
    private static final float D50_Z = 0.8249f;

    /// Type `'XYZ '`.
    private static final int TYPE_XYZ = 0x5859_5A20;

    /// Type `'curv'`.
    private static final int TYPE_CURV = 0x6375_7276;

    /// Type `'para'`.
    private static final int TYPE_PARA = 0x7061_7261;

    /// Validates the parsed profile.
    public IccProfile {
        Objects.requireNonNull(deviceColorSpace, "deviceColorSpace");
        Objects.requireNonNull(pcs, "pcs");
        Objects.requireNonNull(redTrc, "redTrc");
        Objects.requireNonNull(greenTrc, "greenTrc");
        Objects.requireNonNull(blueTrc, "blueTrc");
        Objects.requireNonNull(sha256, "sha256");
        if (size < 128 || size > MAX_PROFILE_BYTES) {
            throw new IllegalArgumentException("ICC profile size is out of range");
        }
        if (deviceColorSpace.length() != 4 || pcs.length() != 4) {
            throw new IllegalArgumentException("ICC signatures must be four characters");
        }
        if (sha256.length() != 64) {
            throw new IllegalArgumentException("ICC digest must be a 64-digit SHA-256 hex string");
        }
        if (chromaticAdaptation != null) {
            if (chromaticAdaptation.length != 9) {
                throw new IllegalArgumentException("ICC chad matrix must have nine entries");
            }
            for (float value : chromaticAdaptation) {
                if (!Float.isFinite(value)) {
                    throw new IllegalArgumentException("ICC chad matrix entries must be finite");
                }
            }
            chromaticAdaptation = chromaticAdaptation.clone();
        }
        if (mediaWhite != null) {
            if (mediaWhite.length != 3) {
                throw new IllegalArgumentException("ICC wtpt must have three XYZ coordinates");
            }
            for (float value : mediaWhite) {
                if (!Float.isFinite(value)) {
                    throw new IllegalArgumentException("ICC wtpt coordinates must be finite");
                }
            }
            mediaWhite = mediaWhite.clone();
        }
        if (mediaBlack != null) {
            if (mediaBlack.length != 3) {
                throw new IllegalArgumentException("ICC bkpt must have three XYZ coordinates");
            }
            for (float value : mediaBlack) {
                if (!Float.isFinite(value)) {
                    throw new IllegalArgumentException("ICC bkpt coordinates must be finite");
                }
            }
            mediaBlack = mediaBlack.clone();
        }
        if (luminance != null) {
            if (luminance.length != 3) {
                throw new IllegalArgumentException("ICC lumi must have three XYZ coordinates");
            }
            for (float value : luminance) {
                if (!Float.isFinite(value) || value < 0.0f) {
                    throw new IllegalArgumentException("ICC lumi coordinates must be finite and nonnegative");
                }
            }
            luminance = luminance.clone();
        }
        if (technology != null && technology.length() != 4) {
            throw new IllegalArgumentException("ICC technology signature must be four characters");
        }
    }

    /// Parses one RGB or GRAY matrix/TRC profile, or a CMYK LUT profile.
    ///
    /// @param bytes the exact profile bytes
    /// @return the parsed profile
    public static IccProfile parse(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes");
        if (bytes.length < 132 || bytes.length > MAX_PROFILE_BYTES) {
            throw new IllegalArgumentException("ICC profile length is outside the accepted bounds");
        }
        int declared = u32(bytes, 0);
        if (declared != bytes.length) {
            throw new IllegalArgumentException("ICC size field does not match the supplied bytes");
        }
        if (u32(bytes, 36) != MAGIC_ACSP) {
            throw new IllegalArgumentException("ICC magic is not acsp");
        }
        int space = u32(bytes, 16);
        if (space != SPACE_RGB && space != SPACE_GRAY && space != SPACE_CMYK) {
            throw new IllegalArgumentException("Only RGB, GRAY, or CMYK ICC profiles are accepted");
        }
        int pcsSpace = u32(bytes, 20);
        if (pcsSpace != SPACE_XYZ && pcsSpace != SPACE_LAB) {
            throw new IllegalArgumentException("Only XYZ or Lab PCS ICC profiles are accepted");
        }
        int version = u32(bytes, 8);
        int major = (version >>> 24) & 0xFF;
        if (major != 2 && major != 4) {
            throw new IllegalArgumentException("Only ICC v2 and v4 profiles are accepted");
        }
        int tagCount = u32(bytes, 128);
        if (tagCount <= 0 || tagCount > MAX_TAGS) {
            throw new IllegalArgumentException("ICC tag count is outside the accepted bounds");
        }
        int tableEnd = 132 + tagCount * 12;
        if (tableEnd > bytes.length) {
            throw new IllegalArgumentException("ICC tag table exceeds the profile");
        }
        float[] redXyz;
        float[] greenXyz;
        float[] blueXyz;
        Curve redTrc;
        Curve greenTrc;
        Curve blueTrc;
        IccCmykLut parsedCmyk = null;
        IccCmykLut parsedCmykBToA = null;
        if (space == SPACE_GRAY) {
            Curve gray = readCurve(bytes, requireTag(bytes, tagCount, TAG_K_TRC));
            float[] white = readMediaWhite(bytes, tagCount);
            if (white == null) {
                white = new float[] {s15(bytes, 68), s15(bytes, 72), s15(bytes, 76)};
            }
            redXyz = new float[] {white[0] / 3.0f, white[1] / 3.0f, white[2] / 3.0f};
            greenXyz = new float[] {white[0] / 3.0f, white[1] / 3.0f, white[2] / 3.0f};
            blueXyz = new float[] {white[0] / 3.0f, white[1] / 3.0f, white[2] / 3.0f};
            redTrc = gray;
            greenTrc = gray;
            blueTrc = gray;
        } else if (space == SPACE_CMYK) {
            parsedCmyk = firstCmykLut(bytes, tagCount, TAG_A2B0, TAG_A2B1, TAG_A2B2, TAG_A2B3);
            if (parsedCmyk == null || parsedCmyk.inverse()) {
                throw new IllegalArgumentException("CMYK ICC profiles require a 4×3 AToB LUT");
            }
            parsedCmykBToA = firstCmykLut(bytes, tagCount, TAG_B2A0, TAG_B2A1, TAG_B2A2, TAG_B2A3);
            if (parsedCmykBToA != null && !parsedCmykBToA.inverse()) {
                throw new IllegalArgumentException("CMYK BToA LUTs must be 3×4");
            }
            Curve identity = new Curve(1.0f, new float[0]);
            redXyz = new float[] {0.0f, 0.0f, 0.0f};
            greenXyz = new float[] {0.0f, 0.0f, 0.0f};
            blueXyz = new float[] {0.0f, 0.0f, 0.0f};
            redTrc = identity;
            greenTrc = identity;
            blueTrc = identity;
        } else {
            redXyz = readXyz(bytes, requireTag(bytes, tagCount, TAG_R_XYZ));
            greenXyz = readXyz(bytes, requireTag(bytes, tagCount, TAG_G_XYZ));
            blueXyz = readXyz(bytes, requireTag(bytes, tagCount, TAG_B_XYZ));
            redTrc = readCurve(bytes, requireTag(bytes, tagCount, TAG_R_TRC));
            greenTrc = readCurve(bytes, requireTag(bytes, tagCount, TAG_G_TRC));
            blueTrc = readCurve(bytes, requireTag(bytes, tagCount, TAG_B_TRC));
        }
        return new IccProfile(
                declared,
                version,
                signature(bytes, 16),
                signature(bytes, 20),
                s15(bytes, 68),
                s15(bytes, 72),
                s15(bytes, 76),
                redXyz[0],
                redXyz[1],
                redXyz[2],
                greenXyz[0],
                greenXyz[1],
                greenXyz[2],
                blueXyz[0],
                blueXyz[1],
                blueXyz[2],
                redTrc,
                greenTrc,
                blueTrc,
                sha256(bytes),
                space == SPACE_CMYK ? null : readClut(bytes, tagCount, TAG_A2B0),
                space == SPACE_CMYK ? null : readClut(bytes, tagCount, TAG_A2B1),
                space == SPACE_CMYK ? null : readClut(bytes, tagCount, TAG_B2A0),
                space == SPACE_CMYK ? null : readClut(bytes, tagCount, TAG_B2A1),
                space == SPACE_CMYK ? null : readClut(bytes, tagCount, TAG_A2B2),
                space == SPACE_CMYK ? null : readClut(bytes, tagCount, TAG_B2A2),
                space == SPACE_CMYK ? null : readClut(bytes, tagCount, TAG_A2B3),
                space == SPACE_CMYK ? null : readClut(bytes, tagCount, TAG_B2A3),
                readNamedColors(bytes, tagCount),
                readChad(bytes, tagCount),
                readMediaWhite(bytes, tagCount),
                readMediaBlack(bytes, tagCount),
                readLuminance(bytes, tagCount),
                readViewingConditions(bytes, tagCount),
                readText(bytes, tagCount, TAG_DESC),
                readText(bytes, tagCount, TAG_VUED),
                readText(bytes, tagCount, TAG_CPRT),
                readSignature(bytes, tagCount, TAG_TECH),
                readText(bytes, tagCount, TAG_DMND),
                readText(bytes, tagCount, TAG_DMDD),
                readDateTime(bytes, tagCount, TAG_CALT),
                readMeasurement(bytes, tagCount),
                readGamut(bytes, tagCount),
                readClut(bytes, tagCount, TAG_PRE0),
                readClut(bytes, tagCount, TAG_PRE1),
                readClut(bytes, tagCount, TAG_PRE2),
                readCicp(bytes, tagCount),
                readOutputResponse(bytes, tagCount),
                readColorants(bytes, tagCount, TAG_CLRT),
                readColorants(bytes, tagCount, TAG_CLOT),
                readProfileSequence(bytes, tagCount),
                readProfileSequenceIds(bytes, tagCount),
                readMetadata(bytes, tagCount),
                readChromaticity(bytes, tagCount),
                readColorantOrder(bytes, tagCount),
                readScreening(bytes, tagCount),
                readUcrBg(bytes, tagCount),
                readText(bytes, tagCount, TAG_TARG),
                readData(bytes, tagCount, TAG_CRDI),
                readData(bytes, tagCount, TAG_PS2S),
                readData(bytes, tagCount, TAG_PS2I),
                readText(bytes, tagCount, TAG_PSD0),
                readText(bytes, tagCount, TAG_PSD1),
                readText(bytes, tagCount, TAG_PSD2),
                readText(bytes, tagCount, TAG_PSD3),
                readDeviceSettings(bytes, tagCount),
                readCrdInfo(bytes, tagCount),
                readText(bytes, tagCount, TAG_SCRD),
                parsedCmyk,
                parsedCmykBToA,
                firstMpe(bytes, tagCount, TAG_B2D0, TAG_B2D1, TAG_B2D2, TAG_B2D3),
                firstMpe(bytes, tagCount, TAG_D2B0, TAG_D2B1, TAG_D2B2, TAG_D2B3),
                readGamutBoundary(bytes, tagCount)
        );
    }

    /// Converts one RGB sample through this profile into extended-linear sRGB.
    ///
    /// When [`#bToD0()`] is present (the first of `B2D0`–`B2D3`), that `mpet` pipeline is used
    /// and treated as XYZ PCS.
    /// Otherwise when [`#clut()`] is present, the AToB0 `mft2` table is used. Otherwise [`#clutAToB1()`]
    /// is used when present. Otherwise [`#clutAToB2()`] is used when present. Otherwise
    /// [`#clutAToB3()`] is used when present. Otherwise the
    /// matrix/TRC path is used. A `GRAY` profile stores the `kTRC` curve as all three tone curves
    /// and one-third of the media white in each primary, so callers pass equal RGB codes. A `CMYK`
    /// profile uses [`#cmyk()`] with K=`0`; callers that have a black channel must use
    /// [`#toExtendedLinearCmyk(float, float, float, float, float)`]. When the
    /// PCS is `Lab `, AToB CLUT outputs are 8-bit ICC L*a*b* and are converted to D50 XYZ before
    /// adaptation. Matrix/TRC primaries remain XYZ. PCS XYZ is chromatically
    /// adapted from the header illuminant to D65 when the illuminant is not already D65-like.
    /// The result is not clamped.
    ///
    /// @param red the device red in `[0, 1]`
    /// @param green the device green in `[0, 1]`
    /// @param blue the device blue in `[0, 1]`
    /// @param alpha the linear coverage
    /// @return the extended-linear color
    public Color toExtendedLinear(float red, float green, float blue, float alpha) {
        if (!Float.isFinite(red) || !Float.isFinite(green) || !Float.isFinite(blue) || !Float.isFinite(alpha)) {
            throw new IllegalArgumentException("ICC sample components must be finite");
        }
        if (outsideUnit(red) || outsideUnit(green) || outsideUnit(blue) || outsideUnit(alpha)) {
            throw new IllegalArgumentException("ICC sample components must be in [0, 1]");
        }
        float x;
        float y;
        float z;
        if (cmyk != null) {
            return toExtendedLinearCmyk(red, green, blue, 0.0f, alpha);
        }
        IccClut forward = clut != null ? clut
                : clutAToB1 != null ? clutAToB1
                : clutAToB2 != null ? clutAToB2
                : clutAToB3;
        if (bToD0 != null) {
            float[] pcs = bToD0.transform(red, green, blue);
            x = pcs[0];
            y = pcs[1];
            z = pcs[2];
        } else if (forward != null) {
            float[] pcs = forward.transform(red, green, blue);
            if (labPcs()) {
                float[] xyz = lab8ToXyz(pcs[0], pcs[1], pcs[2]);
                x = xyz[0];
                y = xyz[1];
                z = xyz[2];
            } else {
                x = pcs[0];
                y = pcs[1];
                z = pcs[2];
            }
        } else {
            float linearRed = redTrc.decode(red);
            float linearGreen = greenTrc.decode(green);
            float linearBlue = blueTrc.decode(blue);
            x = redX * linearRed + greenX * linearGreen + blueX * linearBlue;
            y = redY * linearRed + greenY * linearGreen + blueY * linearBlue;
            z = redZ * linearRed + greenZ * linearGreen + blueZ * linearBlue;
        }
        float[] adapted = adaptToD65(x, y, z);
        return Color.xyzD65ToExtended(adapted[0], adapted[1], adapted[2], alpha);
    }

    /// Converts one CMYK sample through this profile into extended-linear sRGB.
    ///
    /// Requires [`#cmyk()`]. Lab PCS outputs use 8-bit ICC L*a*b* units. The result is
    /// chromatically adapted from the header illuminant to D65 when that illuminant is not
    /// already D65-like. The result is not clamped.
    ///
    /// @param cyan the device cyan in `[0, 1]`
    /// @param magenta the device magenta in `[0, 1]`
    /// @param yellow the device yellow in `[0, 1]`
    /// @param black the device black in `[0, 1]`
    /// @param alpha the linear coverage
    /// @return the extended-linear color
    public Color toExtendedLinearCmyk(float cyan, float magenta, float yellow, float black, float alpha) {
        if (cmyk == null) {
            throw new IllegalStateException("ICC profile has no CMYK AToB LUT");
        }
        if (!Float.isFinite(cyan) || !Float.isFinite(magenta) || !Float.isFinite(yellow)
                || !Float.isFinite(black) || !Float.isFinite(alpha)) {
            throw new IllegalArgumentException("ICC sample components must be finite");
        }
        if (outsideUnit(cyan) || outsideUnit(magenta) || outsideUnit(yellow)
                || outsideUnit(black) || outsideUnit(alpha)) {
            throw new IllegalArgumentException("ICC sample components must be in [0, 1]");
        }
        float[] pcs = cmyk.transform(cyan, magenta, yellow, black);
        float x;
        float y;
        float z;
        if (labPcs()) {
            float[] xyz = lab8ToXyz(pcs[0], pcs[1], pcs[2]);
            x = xyz[0];
            y = xyz[1];
            z = xyz[2];
        } else {
            x = pcs[0];
            y = pcs[1];
            z = pcs[2];
        }
        float[] adapted = adaptToD65(x, y, z);
        return Color.xyzD65ToExtended(adapted[0], adapted[1], adapted[2], alpha);
    }

    /// Returns the `lumi` Y coordinate in candelas per square metre, or `0` when absent.
    ///
    /// @return the nonnegative luminance in nits
    public float luminanceNits() {
        return luminance == null ? 0.0f : luminance[1];
    }

    /// Returns the `view` illuminant Y in candelas per square metre, or `0` when absent.
    ///
    /// @return the nonnegative viewing illuminant luminance
    public float viewingIlluminantNits() {
        return viewingConditions == null ? 0.0f : viewingConditions.illuminantY();
    }

    /// Returns the `gamt` alarm at unit-cube PCS coordinates, or `0` when the tag is absent.
    ///
    /// @param x the PCS X coordinate in `[0, 1]`
    /// @param y the PCS Y coordinate in `[0, 1]`
    /// @param z the PCS Z coordinate in `[0, 1]`
    /// @return the alarm in `[0, 1]`; `0` is in-gamut
    public float gamutAlarm(float x, float y, float z) {
        return gamut == null ? 0.0f : gamut.transform(x, y, z);
    }

    /// Returns whether the PCS sample is in gamut according to [`#gamut()`].
    ///
    /// A missing `gamt` tag is treated as fully in-gamut. A zero alarm is in-gamut; any
    /// positive alarm is out-of-gamut.
    ///
    /// @param x the PCS X coordinate in `[0, 1]`
    /// @param y the PCS Y coordinate in `[0, 1]`
    /// @param z the PCS Z coordinate in `[0, 1]`
    /// @return `true` when the sample is in gamut
    public boolean inGamut(float x, float y, float z) {
        return gamut == null || gamut.inGamut(x, y, z);
    }

    /// Converts a named color's PCS into extended-linear sRGB.
    ///
    /// XYZ PCS values are used directly. Lab PCS values use the ICC 16-bit L*a*b* encoding
    /// stored as `uInt16 / 32768`. The result is chromatically adapted from the header
    /// illuminant to D65 when that illuminant is not already D65-like. Missing names return `null`.
    ///
    /// @param rootName the `ncl2` root name
    /// @param alpha the linear coverage
    /// @return the color, or `null` when the name is absent
    public @Nullable Color namedColorToExtendedLinear(String rootName, float alpha) {
        Objects.requireNonNull(rootName, "rootName");
        if (!Float.isFinite(alpha) || outsideUnit(alpha)) {
            throw new IllegalArgumentException("ICC named-color alpha must be finite and in [0, 1]");
        }
        if (namedColors == null) {
            return null;
        }
        IccNamedColors.Entry entry = namedColors.lookup(rootName);
        if (entry == null) {
            return null;
        }
        float[] xyz;
        if (labPcs()) {
            xyz = lab16ToXyz(entry.pcsX(), entry.pcsY(), entry.pcsZ());
        } else {
            xyz = new float[] {entry.pcsX(), entry.pcsY(), entry.pcsZ()};
        }
        float[] adapted = adaptToD65(xyz[0], xyz[1], xyz[2]);
        return Color.xyzD65ToExtended(adapted[0], adapted[1], adapted[2], alpha);
    }

    /// Returns the first three device coordinates of a named color as encoded sRGB.
    ///
    /// Missing names or tables with fewer than three device coordinates return `null`.
    ///
    /// @param rootName the `ncl2` root name
    /// @param alpha the linear coverage
    /// @return the device RGB color, or `null`
    public @Nullable Color namedColorDeviceRgb(String rootName, float alpha) {
        Objects.requireNonNull(rootName, "rootName");
        if (!Float.isFinite(alpha) || outsideUnit(alpha)) {
            throw new IllegalArgumentException("ICC named-color alpha must be finite and in [0, 1]");
        }
        if (namedColors == null) {
            return null;
        }
        IccNamedColors.Entry entry = namedColors.lookup(rootName);
        if (entry == null || entry.device().length < 3) {
            return null;
        }
        return Color.srgb(
                clamp01(entry.device()[0]),
                clamp01(entry.device()[1]),
                clamp01(entry.device()[2]),
                alpha
        );
    }

    /// Converts one extended-linear color into encoded device RGB through this profile.
    ///
    /// When [`#cmykBToA()`] is present, that 3×4 table is used and the returned color stores
    /// C, M, and Y. Callers that need K must use [`#fromExtendedLinearCmyk(Color)`].
    /// Otherwise when [`#clutBToA0()`] is present, that table is used with PCS XYZ, or 8-bit ICC L*a*b*
    /// when the PCS is `Lab `. Otherwise [`#dToB0()`] is used when present (the first of
    /// `D2B0`–`D2B3`). Otherwise
    /// [`#clutBToA1()`] is used when present. Otherwise
    /// [`#clutBToA2()`] is used when present. Otherwise [`#clutBToA3()`] is used when present.
    /// Otherwise the inverse
    /// matrix/TRC path is used. D65 working XYZ is adapted to the
    /// header illuminant when that illuminant is not already D65-like. Encoded components are
    /// clamped to `[0, 1]`.
    ///
    /// @param color the source color
    /// @return the encoded sRGB-tagged device RGB
    public Color fromExtendedLinear(Color color) {
        Objects.requireNonNull(color, "color");
        Color linear = color.toExtendedLinear();
        float[] xyz = Color.extendedToXyzD65(linear.red(), linear.green(), linear.blue());
        float[] adapted = adaptFromD65(xyz[0], xyz[1], xyz[2]);
        float x = adapted[0];
        float y = adapted[1];
        float z = adapted[2];
        float red;
        float green;
        float blue;
        if (cmykBToA != null) {
            float[] device = fromExtendedLinearCmyk(color);
            return Color.srgb(clamp01(device[0]), clamp01(device[1]), clamp01(device[2]), linear.alpha());
        }
        if (cmyk != null) {
            throw new IllegalStateException("ICC CMYK profile has no BToA LUT");
        }
        IccClut inverseClut = clutBToA0 != null ? clutBToA0
                : clutBToA1 != null ? clutBToA1
                : clutBToA2 != null ? clutBToA2
                : clutBToA3;
        if (dToB0 != null) {
            float[] rgb = dToB0.transform(x, y, z);
            red = rgb[0];
            green = rgb[1];
            blue = rgb[2];
        } else if (inverseClut != null) {
            float[] pcs = labPcs() ? xyzToLab8(x, y, z) : new float[] {x, y, z};
            float[] rgb = inverseClut.transform(pcs[0], pcs[1], pcs[2]);
            red = rgb[0];
            green = rgb[1];
            blue = rgb[2];
        } else {
            float[] inverse = invertMatrix();
            float linearRed = inverse[0] * x + inverse[1] * y + inverse[2] * z;
            float linearGreen = inverse[3] * x + inverse[4] * y + inverse[5] * z;
            float linearBlue = inverse[6] * x + inverse[7] * y + inverse[8] * z;
            red = redTrc.encode(linearRed);
            green = greenTrc.encode(linearGreen);
            blue = blueTrc.encode(linearBlue);
        }
        return Color.srgb(clamp01(red), clamp01(green), clamp01(blue), linear.alpha());
    }

    /// Converts one extended-linear color into device CMYK through this profile.
    ///
    /// Requires [`#cmykBToA()`]. D65 working XYZ is adapted to the header illuminant.
    /// Lab PCS uses 8-bit ICC L*a*b* units as BToA inputs. Components are not clamped
    /// beyond the LUT domain.
    ///
    /// @param color the source color
    /// @return `{C, M, Y, K}`
    public float[] fromExtendedLinearCmyk(Color color) {
        Objects.requireNonNull(color, "color");
        if (cmykBToA == null) {
            throw new IllegalStateException("ICC profile has no CMYK BToA LUT");
        }
        Color linear = color.toExtendedLinear();
        float[] xyz = Color.extendedToXyzD65(linear.red(), linear.green(), linear.blue());
        float[] adapted = adaptFromD65(xyz[0], xyz[1], xyz[2]);
        float[] pcs = labPcs()
                ? xyzToLab8(adapted[0], adapted[1], adapted[2])
                : new float[] {adapted[0], adapted[1], adapted[2]};
        return cmykBToA.transformPcs(pcs[0], pcs[1], pcs[2]);
    }

    /// Inverts the 3×3 PCS matrix whose columns are the RGB primaries.
    private float[] invertMatrix() {
        float det = redX * (greenY * blueZ - blueY * greenZ)
                - greenX * (redY * blueZ - blueY * redZ)
                + blueX * (redY * greenZ - greenY * redZ);
        if (!Float.isFinite(det) || Math.abs(det) < 1.0e-8f) {
            throw new IllegalStateException("ICC primary matrix is not invertible");
        }
        float inv = 1.0f / det;
        return new float[] {
            inv * (greenY * blueZ - blueY * greenZ),
            inv * (blueX * greenZ - greenX * blueZ),
            inv * (greenX * blueY - blueX * greenY),
            inv * (blueY * redZ - redY * blueZ),
            inv * (redX * blueZ - blueX * redZ),
            inv * (blueX * redY - redX * blueY),
            inv * (redY * greenZ - greenY * redZ),
            inv * (greenX * redZ - redX * greenZ),
            inv * (redX * greenY - greenX * redY)
        };
    }

    /// Clamps one component into `[0, 1]`.
    private static float clamp01(float value) {
        return Math.clamp(value, 0.0f, 1.0f);
    }

    /// One tone-reproduction curve.
    ///
    /// An empty table with `gamma == 1` and `paraFunction == 0` is the identity. An empty table
    /// with another gamma is `encoded^gamma`. A non-empty table is linearly interpolated in
    /// `[0, 1]`. `paraFunction` 1–4 apply the ICC parametric functions.
    ///
    /// @param gamma the power-law exponent when the table is empty
    /// @param table the normalized curve samples, or an empty array
    /// @param paraFunction `0` for the gamma/table path, or ICC para type `1`–`4`
    /// @param a parametric `a`
    /// @param b parametric `b`
    /// @param c parametric `c`
    /// @param d parametric `d`
    /// @param e parametric `e`
    /// @param f parametric `f`
    @NotNullByDefault
    public record Curve(
            float gamma,
            float @Unmodifiable [] table,
            int paraFunction,
            float a,
            float b,
            float c,
            float d,
            float e,
            float f
    ) {
        /// Validates the curve.
        public Curve {
            Objects.requireNonNull(table, "table");
            table = Arrays.copyOf(table, table.length);
            if (!Float.isFinite(gamma) || gamma <= 0.0f) {
                throw new IllegalArgumentException("ICC curve gamma must be finite and positive");
            }
            if (paraFunction < 0 || paraFunction > 4) {
                throw new IllegalArgumentException("ICC para function must be 0 through 4");
            }
            if (table.length > MAX_CURVE_ENTRIES) {
                throw new IllegalArgumentException("ICC curve table exceeds the accepted bound");
            }
            for (float sample : table) {
                if (!Float.isFinite(sample)) {
                    throw new IllegalArgumentException("ICC curve samples must be finite");
                }
            }
        }

        /// Creates a gamma or sampled curve.
        ///
        /// @param gamma the power-law exponent
        /// @param table the samples, or empty
        public Curve(float gamma, float[] table) {
            this(gamma, table, 0, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f);
        }

        /// Applies the curve to one encoded component.
        ///
        /// @param encoded the encoded component
        /// @return the linearized component
        public float decode(float encoded) {
            float unit = Math.clamp(encoded, 0.0f, 1.0f);
            if (paraFunction > 0) {
                return decodePara(unit);
            }
            if (table.length == 0) {
                if (gamma == 1.0f) {
                    return unit;
                }
                return (float) Math.pow(unit, gamma);
            }
            if (table.length == 1) {
                return (float) Math.pow(unit, table[0]);
            }
            float position = unit * (table.length - 1);
            int index = Math.min((int) position, table.length - 2);
            float fraction = position - index;
            return Math.fma(table[index + 1] - table[index], fraction, table[index]);
        }

        /// Applies the inverse curve to one linearized component.
        ///
        /// @param linear the linearized component
        /// @return the encoded component
        public float encode(float linear) {
            float unit = Math.clamp(linear, 0.0f, 1.0f);
            if (paraFunction > 0) {
                return encodePara(unit);
            }
            if (table.length == 0) {
                if (gamma == 1.0f) {
                    return unit;
                }
                return (float) Math.pow(unit, 1.0 / gamma);
            }
            if (table.length == 1) {
                float exponent = table[0];
                if (exponent == 0.0f) {
                    return 0.0f;
                }
                return (float) Math.pow(unit, 1.0 / exponent);
            }
            if (unit <= table[0]) {
                return 0.0f;
            }
            if (unit >= table[table.length - 1]) {
                return 1.0f;
            }
            int index = 0;
            while (index < table.length - 2 && table[index + 1] < unit) {
                index++;
            }
            float span = table[index + 1] - table[index];
            float fraction = span == 0.0f ? 0.0f : (unit - table[index]) / span;
            return (index + fraction) / (table.length - 1);
        }

        /// Applies ICC parametric function types 1–4.
        private float decodePara(float x) {
            return switch (paraFunction) {
                case 1 -> powNonNeg(Math.fma(a, x, b), gamma);
                case 2 -> x >= threshold() ? powNonNeg(Math.fma(a, x, b), gamma) + c : c;
                case 3 -> x >= d ? powNonNeg(Math.fma(a, x, b), gamma) : c * x;
                case 4 -> x >= d ? powNonNeg(Math.fma(a, x, b), gamma) + e : Math.fma(c, x, f);
                default -> x;
            };
        }

        /// Inverts ICC parametric function types 1–4 where the branch is unique.
        private float encodePara(float y) {
            return switch (paraFunction) {
                case 1 -> invertPower(y);
                case 2 -> y <= c ? 0.0f : invertPower(y - c);
                case 3 -> y >= powNonNeg(Math.fma(a, d, b), gamma) ? invertPower(y) : (c == 0.0f ? 0.0f : y / c);
                case 4 -> y >= powNonNeg(Math.fma(a, d, b), gamma) + e
                        ? invertPower(y - e)
                        : (c == 0.0f ? 0.0f : (y - f) / c);
                default -> y;
            };
        }

        /// Returns `-b/a` for type 2, or `0` when `a` is zero.
        private float threshold() {
            return a == 0.0f ? 0.0f : -b / a;
        }

        /// Inverts `Y = (aX + b)^g` for `X`.
        private float invertPower(float y) {
            if (a == 0.0f) {
                return 0.0f;
            }
            float root = (float) Math.pow(Math.max(y, 0.0f), 1.0 / gamma);
            return (root - b) / a;
        }

        /// Returns `max(base, 0)^exponent`.
        private static float powNonNeg(float base, float exponent) {
            return (float) Math.pow(Math.max(base, 0.0f), exponent);
        }
    }

    /// Parses the first present CMYK `mft1` or `mft2` tag among `signatures`.
    ///
    /// @param bytes the profile bytes
    /// @param tagCount the tag-table length
    /// @param signatures AToB or BToA signatures in preference order
    /// @return the LUT, or `null` when none of the tags are present
    private static @Nullable IccCmykLut firstCmykLut(byte[] bytes, int tagCount, int... signatures) {
        for (int signature : signatures) {
            int entry = findTag(bytes, tagCount, signature);
            if (entry < 0) {
                continue;
            }
            int offset = u32(bytes, entry + 4);
            int size = u32(bytes, entry + 8);
            return IccCmykLut.parse(bytes, offset, size);
        }
        return null;
    }

    /// Parses the first present `mpet` tag among `signatures`.
    private static @Nullable IccMpe firstMpe(byte[] bytes, int tagCount, int... signatures) {
        for (int signature : signatures) {
            IccMpe parsed = readMpe(bytes, tagCount, signature);
            if (parsed != null) {
                return parsed;
            }
        }
        return null;
    }

    /// Parses an optional `mpet` tag with `signature`.
    private static @Nullable IccMpe readMpe(byte[] bytes, int tagCount, int signature) {
        int entry = findTag(bytes, tagCount, signature);
        if (entry < 0) {
            return null;
        }
        int offset = u32(bytes, entry + 4);
        int size = u32(bytes, entry + 8);
        return IccMpe.parse(bytes, offset, size);
    }

    /// Parses an optional `gbd ` tag.
    private static @Nullable IccGamutBoundary readGamutBoundary(byte[] bytes, int tagCount) {
        int entry = findTag(bytes, tagCount, TAG_GBD);
        if (entry < 0) {
            return null;
        }
        int offset = u32(bytes, entry + 4);
        int size = u32(bytes, entry + 8);
        return IccGamutBoundary.parse(bytes, offset, size);
    }

    /// Parses an optional `mft1` or `mft2` tag with `signature`.
    private static @Nullable IccClut readClut(byte[] bytes, int tagCount, int signature) {
        int entry = findTag(bytes, tagCount, signature);
        if (entry < 0) {
            return null;
        }
        int offset = u32(bytes, entry + 4);
        int size = u32(bytes, entry + 8);
        if (offset < 0 || size < 4 || offset > bytes.length - size) {
            throw new IllegalArgumentException("ICC LUT tag is outside the profile");
        }
        int type = u32(bytes, offset);
        if (type == 0x6D41_4220) {
            return IccClut.parseMab(bytes, offset, size);
        }
        if (type == 0x6D42_4120) {
            return IccClut.parseMba(bytes, offset, size);
        }
        if (type == 0x6D66_7431) {
            return IccClut.parseMft1(bytes, offset, size);
        }
        return IccClut.parseMft2(bytes, offset, size);
    }

    /// Parses an optional `ncl2` tag.
    private static @Nullable IccNamedColors readNamedColors(byte[] bytes, int tagCount) {
        int entry = findTag(bytes, tagCount, TAG_NCL2);
        if (entry < 0) {
            entry = findTag(bytes, tagCount, TAG_NCL);
        }
        if (entry < 0) {
            return null;
        }
        int offset = u32(bytes, entry + 4);
        int size = u32(bytes, entry + 8);
        return IccNamedColors.parse(bytes, offset, size);
    }

    /// Parses an optional `chad` `sf32` 3×3 matrix.
    private static float @Nullable [] readChad(byte[] bytes, int tagCount) {
        int entry = findTag(bytes, tagCount, TAG_CHAD);
        if (entry < 0) {
            return null;
        }
        int offset = u32(bytes, entry + 4);
        int size = u32(bytes, entry + 8);
        if (offset < 0 || size < 44 || offset > bytes.length - size) {
            throw new IllegalArgumentException("ICC chad tag is outside the profile");
        }
        if (u32(bytes, offset) != TYPE_SF32) {
            throw new IllegalArgumentException("ICC chad tag is not sf32");
        }
        float[] matrix = new float[9];
        for (int index = 0; index < 9; index++) {
            matrix[index] = s15(bytes, offset + 8 + index * 4);
        }
        return matrix;
    }

    /// Parses an optional `wtpt` XYZ tag.
    private static float @Nullable [] readMediaWhite(byte[] bytes, int tagCount) {
        int entry = findTag(bytes, tagCount, TAG_WTPT);
        if (entry < 0) {
            return null;
        }
        int offset = u32(bytes, entry + 4);
        int size = u32(bytes, entry + 8);
        if (offset < 0 || size < 20 || offset > bytes.length - size) {
            throw new IllegalArgumentException("ICC wtpt tag is outside the profile");
        }
        return readXyz(bytes, offset);
    }

    /// Parses an optional `bkpt` XYZ tag.
    private static float @Nullable [] readMediaBlack(byte[] bytes, int tagCount) {
        int entry = findTag(bytes, tagCount, TAG_BKPT);
        if (entry < 0) {
            return null;
        }
        int offset = u32(bytes, entry + 4);
        int size = u32(bytes, entry + 8);
        if (offset < 0 || size < 20 || offset > bytes.length - size) {
            throw new IllegalArgumentException("ICC bkpt tag is outside the profile");
        }
        return readXyz(bytes, offset);
    }

    /// Parses an optional `lumi` XYZ tag.
    private static float @Nullable [] readLuminance(byte[] bytes, int tagCount) {
        int entry = findTag(bytes, tagCount, TAG_LUMI);
        if (entry < 0) {
            return null;
        }
        int offset = u32(bytes, entry + 4);
        int size = u32(bytes, entry + 8);
        if (offset < 0 || size < 20 || offset > bytes.length - size) {
            throw new IllegalArgumentException("ICC lumi tag is outside the profile");
        }
        return readXyz(bytes, offset);
    }

    /// Parses an optional `view` tag.
    private static @Nullable IccViewingConditions readViewingConditions(byte[] bytes, int tagCount) {
        int entry = findTag(bytes, tagCount, TAG_VIEW);
        if (entry < 0) {
            return null;
        }
        int offset = u32(bytes, entry + 4);
        int size = u32(bytes, entry + 8);
        return IccViewingConditions.parse(bytes, offset, size);
    }

    /// Parses an optional `desc`, `vued`, or `cprt` ASCII tag.
    private static @Nullable String readText(byte[] bytes, int tagCount, int signature) {
        int entry = findTag(bytes, tagCount, signature);
        if (entry < 0) {
            return null;
        }
        int offset = u32(bytes, entry + 4);
        int size = u32(bytes, entry + 8);
        return IccProfileText.parse(bytes, offset, size);
    }

    /// Parses an optional `signatureType` tag such as `tech`.
    private static @Nullable String readSignature(byte[] bytes, int tagCount, int signature) {
        int entry = findTag(bytes, tagCount, signature);
        if (entry < 0) {
            return null;
        }
        int offset = u32(bytes, entry + 4);
        int size = u32(bytes, entry + 8);
        if (offset < 0 || size < 12 || offset > bytes.length - size) {
            throw new IllegalArgumentException("ICC signature tag is outside the profile");
        }
        if (u32(bytes, offset) != TYPE_SIG) {
            throw new IllegalArgumentException("ICC signature tag is not sig");
        }
        return signature(bytes, offset + 8);
    }

    /// Parses an optional `dtim` tag such as `calt`.
    private static @Nullable IccDateTime readDateTime(byte[] bytes, int tagCount, int signature) {
        int entry = findTag(bytes, tagCount, signature);
        if (entry < 0) {
            return null;
        }
        int offset = u32(bytes, entry + 4);
        int size = u32(bytes, entry + 8);
        return IccDateTime.parse(bytes, offset, size);
    }

    /// Parses an optional `meas` tag.
    private static @Nullable IccMeasurement readMeasurement(byte[] bytes, int tagCount) {
        int entry = findTag(bytes, tagCount, TAG_MEAS);
        if (entry < 0) {
            return null;
        }
        int offset = u32(bytes, entry + 4);
        int size = u32(bytes, entry + 8);
        return IccMeasurement.parse(bytes, offset, size);
    }

    /// Parses an optional `gamt` tag.
    private static @Nullable IccGamut readGamut(byte[] bytes, int tagCount) {
        int entry = findTag(bytes, tagCount, TAG_GAMT);
        if (entry < 0) {
            return null;
        }
        int offset = u32(bytes, entry + 4);
        int size = u32(bytes, entry + 8);
        return IccGamut.parse(bytes, offset, size);
    }

    /// Parses an optional `cicp` tag.
    private static @Nullable IccCicp readCicp(byte[] bytes, int tagCount) {
        int entry = findTag(bytes, tagCount, TAG_CICP);
        if (entry < 0) {
            return null;
        }
        int offset = u32(bytes, entry + 4);
        int size = u32(bytes, entry + 8);
        return IccCicp.parse(bytes, offset, size);
    }

    /// Parses an optional `resp` tag.
    private static @Nullable IccOutputResponse readOutputResponse(byte[] bytes, int tagCount) {
        int entry = findTag(bytes, tagCount, TAG_RESP);
        if (entry < 0) {
            return null;
        }
        int offset = u32(bytes, entry + 4);
        int size = u32(bytes, entry + 8);
        return IccOutputResponse.parse(bytes, offset, size);
    }

    /// Parses an optional `clrt` or `clot` tag.
    private static @Nullable IccColorants readColorants(byte[] bytes, int tagCount, int signature) {
        int entry = findTag(bytes, tagCount, signature);
        if (entry < 0) {
            return null;
        }
        int offset = u32(bytes, entry + 4);
        int size = u32(bytes, entry + 8);
        return IccColorants.parse(bytes, offset, size);
    }

    /// Parses an optional `pseq` tag.
    private static @Nullable IccProfileSequence readProfileSequence(byte[] bytes, int tagCount) {
        int entry = findTag(bytes, tagCount, TAG_PSEQ);
        if (entry < 0) {
            return null;
        }
        int offset = u32(bytes, entry + 4);
        int size = u32(bytes, entry + 8);
        return IccProfileSequence.parse(bytes, offset, size);
    }

    /// Parses an optional `psid` tag.
    private static @Nullable IccProfileSequenceIds readProfileSequenceIds(byte[] bytes, int tagCount) {
        int entry = findTag(bytes, tagCount, TAG_PSID);
        if (entry < 0) {
            return null;
        }
        int offset = u32(bytes, entry + 4);
        int size = u32(bytes, entry + 8);
        return IccProfileSequenceIds.parse(bytes, offset, size);
    }

    /// Parses an optional `meta` tag.
    private static @Nullable IccMetadata readMetadata(byte[] bytes, int tagCount) {
        int entry = findTag(bytes, tagCount, TAG_META);
        if (entry < 0) {
            return null;
        }
        int offset = u32(bytes, entry + 4);
        int size = u32(bytes, entry + 8);
        return IccMetadata.parse(bytes, offset, size);
    }

    /// Parses an optional `chrm` tag.
    private static @Nullable IccChromaticity readChromaticity(byte[] bytes, int tagCount) {
        int entry = findTag(bytes, tagCount, TAG_CHRM);
        if (entry < 0) {
            return null;
        }
        int offset = u32(bytes, entry + 4);
        int size = u32(bytes, entry + 8);
        return IccChromaticity.parse(bytes, offset, size);
    }

    /// Parses an optional `clro` tag.
    private static @Nullable IccColorantOrder readColorantOrder(byte[] bytes, int tagCount) {
        int entry = findTag(bytes, tagCount, TAG_CLRO);
        if (entry < 0) {
            return null;
        }
        int offset = u32(bytes, entry + 4);
        int size = u32(bytes, entry + 8);
        return IccColorantOrder.parse(bytes, offset, size);
    }

    /// Parses an optional `scrn` tag.
    private static @Nullable IccScreening readScreening(byte[] bytes, int tagCount) {
        int entry = findTag(bytes, tagCount, TAG_SCRN);
        if (entry < 0) {
            return null;
        }
        int offset = u32(bytes, entry + 4);
        int size = u32(bytes, entry + 8);
        return IccScreening.parse(bytes, offset, size);
    }

    /// Parses an optional `bfd ` tag.
    private static @Nullable IccUcrBg readUcrBg(byte[] bytes, int tagCount) {
        int entry = findTag(bytes, tagCount, TAG_BFD);
        if (entry < 0) {
            return null;
        }
        int offset = u32(bytes, entry + 4);
        int size = u32(bytes, entry + 8);
        return IccUcrBg.parse(bytes, offset, size);
    }

    /// Parses an optional `data` tag such as `crdi`, `ps2s`, or `ps2i`.
    private static @Nullable IccData readData(byte[] bytes, int tagCount, int signature) {
        int entry = findTag(bytes, tagCount, signature);
        if (entry < 0) {
            return null;
        }
        int offset = u32(bytes, entry + 4);
        int size = u32(bytes, entry + 8);
        if (offset < 0 || size < 4 || offset > bytes.length - size) {
            throw new IllegalArgumentException("ICC data tag is outside the profile");
        }
        if (u32(bytes, offset) != IccData.TYPE_DATA) {
            return null;
        }
        return IccData.parse(bytes, offset, size);
    }

    /// Parses an optional `devs` tag.
    private static @Nullable IccDeviceSettings readDeviceSettings(byte[] bytes, int tagCount) {
        int entry = findTag(bytes, tagCount, TAG_DEVS);
        if (entry < 0) {
            return null;
        }
        int offset = u32(bytes, entry + 4);
        int size = u32(bytes, entry + 8);
        return IccDeviceSettings.parse(bytes, offset, size);
    }

    /// Parses an optional `crdInfoType` stored under `crdi`.
    private static @Nullable IccCrdInfo readCrdInfo(byte[] bytes, int tagCount) {
        int entry = findTag(bytes, tagCount, TAG_CRDI);
        if (entry < 0) {
            return null;
        }
        int offset = u32(bytes, entry + 4);
        int size = u32(bytes, entry + 8);
        if (offset < 0 || size < 4 || offset > bytes.length - size) {
            throw new IllegalArgumentException("ICC crdInfo tag is outside the profile");
        }
        if (u32(bytes, offset) != IccCrdInfo.TYPE_CRDI) {
            return null;
        }
        return IccCrdInfo.parse(bytes, offset, size);
    }

    /// Returns whether the header PCS is `Lab `.
    ///
    /// @return `true` when [`#pcs()`] is `Lab `
    public boolean labPcs() {
        return "Lab ".equals(pcs);
    }

    /// Converts 8-bit ICC Lab units in `[0, 1]` to D50 XYZ.
    private float[] lab8ToXyz(float lUnit, float aUnit, float bUnit) {
        return labToXyzD50(lUnit * 100.0f, aUnit * 255.0f - 128.0f, bUnit * 255.0f - 128.0f);
    }

    /// Converts `ncl2` Lab values stored as `uInt16 / 32768` to D50 XYZ.
    private float[] lab16ToXyz(float pcsX, float pcsY, float pcsZ) {
        return labToXyzD50(
                pcsX * 32768.0f / 65280.0f * 100.0f,
                pcsY * 32768.0f / 65280.0f * 255.0f - 128.0f,
                pcsZ * 32768.0f / 65280.0f * 255.0f - 128.0f
        );
    }

    /// Converts D50 XYZ to 8-bit ICC Lab units in `[0, 1]`.
    private float[] xyzToLab8(float x, float y, float z) {
        float[] lab = xyzD50ToLab(x, y, z);
        return new float[] {
                clamp01(lab[0] / 100.0f),
                clamp01((lab[1] + 128.0f) / 255.0f),
                clamp01((lab[2] + 128.0f) / 255.0f)
        };
    }

    /// Converts CIE L*a*b* to D50 XYZ.
    private static float[] labToXyzD50(float lStar, float aStar, float bStar) {
        float fy = (lStar + 16.0f) / 116.0f;
        float fx = aStar / 500.0f + fy;
        float fz = fy - bStar / 200.0f;
        return new float[] {
                inverseLabF(fx) * D50_X,
                inverseLabF(fy) * D50_Y,
                inverseLabF(fz) * D50_Z
        };
    }

    /// Converts D50 XYZ to CIE L*a*b*.
    private static float[] xyzD50ToLab(float x, float y, float z) {
        float fx = labF(x / D50_X);
        float fy = labF(y / D50_Y);
        float fz = labF(z / D50_Z);
        return new float[] {
                116.0f * fy - 16.0f,
                500.0f * (fx - fy),
                200.0f * (fy - fz)
        };
    }

    /// Applies the CIE Lab `f` function.
    private static float labF(float ratio) {
        return ratio > LAB_EPSILON ? (float) Math.cbrt(ratio) : (LAB_KAPPA * ratio + 16.0f) / 116.0f;
    }

    /// Inverts the CIE Lab `f` function.
    private static float inverseLabF(float f) {
        float cubed = f * f * f;
        return cubed > LAB_EPSILON ? cubed : (116.0f * f - 16.0f) / LAB_KAPPA;
    }

    /// Returns the white point used for Bradford adaptation when `chad` is absent.
    private float sourceWhiteX() {
        return mediaWhite != null ? mediaWhite[0] : illuminantX;
    }

    /// Returns the white-point Y used for Bradford adaptation when `chad` is absent.
    private float sourceWhiteY() {
        return mediaWhite != null ? mediaWhite[1] : illuminantY;
    }

    /// Returns the white-point Z used for Bradford adaptation when `chad` is absent.
    private float sourceWhiteZ() {
        return mediaWhite != null ? mediaWhite[2] : illuminantZ;
    }

    /// Maps profile PCS XYZ to D65 working XYZ.
    ///
    /// When [`#chromaticAdaptation()`] is present, the `chad` matrix is applied first and the
    /// result is treated as D50. Otherwise Bradford adaptation uses [`#mediaWhite()`] when
    /// present, or the header illuminant.
    private float[] adaptToD65(float x, float y, float z) {
        if (mediaBlack != null) {
            x -= mediaBlack[0];
            y -= mediaBlack[1];
            z -= mediaBlack[2];
        }
        if (chromaticAdaptation != null) {
            float[] pcs = multiply3x3(chromaticAdaptation, x, y, z);
            return adaptBradford(pcs[0], pcs[1], pcs[2], D50_X, D50_Y, D50_Z, 0.95047f, 1.0f, 1.08883f);
        }
        float whiteX = sourceWhiteX();
        float whiteY = sourceWhiteY();
        float whiteZ = sourceWhiteZ();
        if (!isD65(whiteX, whiteY, whiteZ)) {
            return adaptBradford(x, y, z, whiteX, whiteY, whiteZ, 0.95047f, 1.0f, 1.08883f);
        }
        return new float[] {x, y, z};
    }

    /// Maps D65 working XYZ back to profile PCS XYZ.
    private float[] adaptFromD65(float x, float y, float z) {
        float[] pcs;
        if (chromaticAdaptation != null) {
            float[] d50 = adaptBradford(x, y, z, 0.95047f, 1.0f, 1.08883f, D50_X, D50_Y, D50_Z);
            pcs = multiply3x3(invert3x3(chromaticAdaptation), d50[0], d50[1], d50[2]);
        } else {
            float whiteX = sourceWhiteX();
            float whiteY = sourceWhiteY();
            float whiteZ = sourceWhiteZ();
            pcs = !isD65(whiteX, whiteY, whiteZ)
                    ? adaptBradford(x, y, z, 0.95047f, 1.0f, 1.08883f, whiteX, whiteY, whiteZ)
                    : new float[] {x, y, z};
        }
        if (mediaBlack != null) {
            pcs[0] += mediaBlack[0];
            pcs[1] += mediaBlack[1];
            pcs[2] += mediaBlack[2];
        }
        return pcs;
    }

    /// Multiplies a row-major 3×3 matrix by a column vector.
    private static float[] multiply3x3(float[] matrix, float x, float y, float z) {
        return new float[] {
                matrix[0] * x + matrix[1] * y + matrix[2] * z,
                matrix[3] * x + matrix[4] * y + matrix[5] * z,
                matrix[6] * x + matrix[7] * y + matrix[8] * z
        };
    }

    /// Inverts a row-major 3×3 matrix.
    private static float[] invert3x3(float[] matrix) {
        float det = matrix[0] * (matrix[4] * matrix[8] - matrix[5] * matrix[7])
                - matrix[1] * (matrix[3] * matrix[8] - matrix[5] * matrix[6])
                + matrix[2] * (matrix[3] * matrix[7] - matrix[4] * matrix[6]);
        if (!Float.isFinite(det) || Math.abs(det) < 1.0e-8f) {
            throw new IllegalStateException("ICC chad matrix is not invertible");
        }
        float inv = 1.0f / det;
        return new float[] {
                inv * (matrix[4] * matrix[8] - matrix[5] * matrix[7]),
                inv * (matrix[2] * matrix[7] - matrix[1] * matrix[8]),
                inv * (matrix[1] * matrix[5] - matrix[2] * matrix[4]),
                inv * (matrix[5] * matrix[6] - matrix[3] * matrix[8]),
                inv * (matrix[0] * matrix[8] - matrix[2] * matrix[6]),
                inv * (matrix[2] * matrix[3] - matrix[0] * matrix[5]),
                inv * (matrix[3] * matrix[7] - matrix[4] * matrix[6]),
                inv * (matrix[1] * matrix[6] - matrix[0] * matrix[7]),
                inv * (matrix[0] * matrix[4] - matrix[1] * matrix[3])
        };
    }

    /// Locates one tag-table entry, or `-1` when absent.
    private static int findTag(byte[] bytes, int tagCount, int signature) {
        for (int index = 0; index < tagCount; index++) {
            int entry = 132 + index * 12;
            if (u32(bytes, entry) == signature) {
                return entry;
            }
        }
        return -1;
    }

    /// Locates one required tag.
    private static int requireTag(byte[] bytes, int tagCount, int signature) {
        for (int index = 0; index < tagCount; index++) {
            int entry = 132 + index * 12;
            if (u32(bytes, entry) == signature) {
                int offset = u32(bytes, entry + 4);
                int size = u32(bytes, entry + 8);
                if (offset < 0 || size < 0 || offset > bytes.length - size) {
                    throw new IllegalArgumentException("ICC tag offset is outside the profile");
                }
                return offset;
            }
        }
        throw new IllegalArgumentException("ICC profile is missing a required matrix/TRC tag");
    }

    /// Reads one `XYZType` tag.
    private static float[] readXyz(byte[] bytes, int offset) {
        if (offset > bytes.length - 20 || u32(bytes, offset) != TYPE_XYZ) {
            throw new IllegalArgumentException("ICC XYZ tag is malformed");
        }
        return new float[] {s15(bytes, offset + 8), s15(bytes, offset + 12), s15(bytes, offset + 16)};
    }

    /// Reads one `curv` or parametric `para` tag.
    private static Curve readCurve(byte[] bytes, int offset) {
        if (offset > bytes.length - 12) {
            throw new IllegalArgumentException("ICC curve tag is truncated");
        }
        int type = u32(bytes, offset);
        if (type == TYPE_CURV) {
            int count = u32(bytes, offset + 8);
            if (count < 0 || count > MAX_CURVE_ENTRIES) {
                throw new IllegalArgumentException("ICC curv entry count is outside the accepted bounds");
            }
            if (count == 0) {
                return new Curve(1.0f, new float[0]);
            }
            if (count == 1) {
                if (offset > bytes.length - 14) {
                    throw new IllegalArgumentException("ICC gamma curv tag is truncated");
                }
                return new Curve(u16(bytes, offset + 12) / 256.0f, new float[0]);
            }
            int required = 12 + count * 2;
            if (offset > bytes.length - required) {
                throw new IllegalArgumentException("ICC curv table exceeds the profile");
            }
            float[] table = new float[count];
            for (int index = 0; index < count; index++) {
                table[index] = u16(bytes, offset + 12 + index * 2) / 65535.0f;
            }
            return new Curve(1.0f, table);
        }
        if (type == TYPE_PARA) {
            if (offset > bytes.length - 16) {
                throw new IllegalArgumentException("ICC para tag is truncated");
            }
            int function = u16(bytes, offset + 8);
            int params = switch (function) {
                case 0 -> 1;
                case 1 -> 3;
                case 2 -> 4;
                case 3 -> 5;
                case 4 -> 7;
                default -> throw new IllegalArgumentException("ICC para function must be 0 through 4");
            };
            int required = 12 + params * 4;
            if (offset > bytes.length - required) {
                throw new IllegalArgumentException("ICC para tag is truncated");
            }
            float g = s15(bytes, offset + 12);
            if (function == 0) {
                return new Curve(g, new float[0]);
            }
            float a = params > 1 ? s15(bytes, offset + 16) : 1.0f;
            float b = params > 2 ? s15(bytes, offset + 20) : 0.0f;
            float c = params > 3 ? s15(bytes, offset + 24) : 0.0f;
            float d = params > 4 ? s15(bytes, offset + 28) : 0.0f;
            float e = params > 5 ? s15(bytes, offset + 32) : 0.0f;
            float f = params > 6 ? s15(bytes, offset + 36) : 0.0f;
            return new Curve(g, new float[0], function, a, b, c, d, e, f);
        }
        throw new IllegalArgumentException("ICC tone curve type is not accepted");
    }

    /// Chromatically adapts XYZ from one white point to another with Bradford.
    private static float[] adaptBradford(
            float x,
            float y,
            float z,
            float sourceX,
            float sourceY,
            float sourceZ,
            float destinationX,
            float destinationY,
            float destinationZ
    ) {
        float[] source = bradfordCone(sourceX, sourceY, sourceZ);
        float[] destination = bradfordCone(destinationX, destinationY, destinationZ);
        float[] cone = bradfordCone(x, y, z);
        cone[0] *= destination[0] / source[0];
        cone[1] *= destination[1] / source[1];
        cone[2] *= destination[2] / source[2];
        return new float[] {
                0.9869929f * cone[0] + -0.1470543f * cone[1] + 0.1599627f * cone[2],
                0.4323053f * cone[0] + 0.5183603f * cone[1] + 0.0492912f * cone[2],
                -0.0085287f * cone[0] + 0.0400428f * cone[1] + 0.9684867f * cone[2]
        };
    }

    /// Converts XYZ into Bradford cone space.
    private static float[] bradfordCone(float x, float y, float z) {
        return new float[] {
                0.8951f * x + 0.2664f * y + -0.1614f * z,
                -0.7502f * x + 1.7135f * y + 0.0367f * z,
                0.0389f * x + -0.0685f * y + 1.0296f * z
        };
    }

    /// Returns whether the illuminant is D65-like.
    private static boolean isD65(float x, float y, float z) {
        return Math.abs(x - 0.95047f) < 0.02f && Math.abs(y - 1.0f) < 0.02f && Math.abs(z - 1.08883f) < 0.02f;
    }

    /// Reads a 4-character signature.
    private static String signature(byte[] bytes, int offset) {
        return new String(bytes, offset, 4, StandardCharsets.US_ASCII);
    }

    /// Reads a big-endian unsigned 32-bit integer as a signed Java `int`.
    private static int u32(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) << 24)
                | ((bytes[offset + 1] & 0xFF) << 16)
                | ((bytes[offset + 2] & 0xFF) << 8)
                | (bytes[offset + 3] & 0xFF);
    }

    /// Reads a big-endian unsigned 16-bit integer.
    private static int u16(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) << 8) | (bytes[offset + 1] & 0xFF);
    }

    /// Reads a big-endian `s15Fixed16` number.
    private static float s15(byte[] bytes, int offset) {
        return u32(bytes, offset) / 65536.0f;
    }

    /// Digests the profile bytes.
    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException failure) {
            throw new IllegalStateException("SHA-256 is required to address ICC profiles", failure);
        }
    }

    /// Returns whether a component is outside `[0, 1]`.
    private static boolean outsideUnit(float value) {
        return value < 0.0f || value > 1.0f;
    }
}

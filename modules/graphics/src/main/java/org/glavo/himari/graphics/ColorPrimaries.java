package org.glavo.himari.graphics;

import org.jetbrains.annotations.NotNullByDefault;

/// CIE xy chromaticities for one RGB primary set plus its white point.
///
/// @param white the adapting white
/// @param redX the red x chromaticity
/// @param redY the red y chromaticity
/// @param greenX the green x chromaticity
/// @param greenY the green y chromaticity
/// @param blueX the blue x chromaticity
/// @param blueY the blue y chromaticity
@NotNullByDefault
public record ColorPrimaries(
        WhitePoint white,
        float redX,
        float redY,
        float greenX,
        float greenY,
        float blueX,
        float blueY
) {
    /// sRGB / BT.709 primaries on D65.
    public static final ColorPrimaries SRGB = new ColorPrimaries(
            WhitePoint.D65,
            0.640f,
            0.330f,
            0.300f,
            0.600f,
            0.150f,
            0.060f
    );

    /// Display-P3 primaries on D65.
    public static final ColorPrimaries DISPLAY_P3 = new ColorPrimaries(
            WhitePoint.D65,
            0.680f,
            0.320f,
            0.265f,
            0.690f,
            0.150f,
            0.060f
    );

    /// ProPhoto RGB / ROMM primaries on D50.
    public static final ColorPrimaries PROPHOTO = new ColorPrimaries(
            WhitePoint.D50,
            0.7347f,
            0.2653f,
            0.1596f,
            0.8404f,
            0.0366f,
            0.0001f
    );

    /// Adobe RGB (1998) primaries on D65.
    public static final ColorPrimaries A98 = new ColorPrimaries(
            WhitePoint.D65,
            0.640f,
            0.330f,
            0.210f,
            0.710f,
            0.150f,
            0.060f
    );

    /// BT.2020 primaries on D65.
    public static final ColorPrimaries BT2020 = new ColorPrimaries(
            WhitePoint.D65,
            0.708f,
            0.292f,
            0.170f,
            0.797f,
            0.131f,
            0.046f
    );

    /// Validates finite chromaticities.
    public ColorPrimaries {
        if (!Float.isFinite(redX) || !Float.isFinite(redY)
                || !Float.isFinite(greenX) || !Float.isFinite(greenY)
                || !Float.isFinite(blueX) || !Float.isFinite(blueY)) {
            throw new IllegalArgumentException("Primary chromaticities must be finite");
        }
    }
}

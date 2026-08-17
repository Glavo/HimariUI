package org.glavo.himari.graphics;

import org.jetbrains.annotations.NotNullByDefault;

/// Tags a first-stable color encoding as primaries plus a transfer function.
///
/// @param primaries the RGB primaries and white
/// @param transfer the electro-optical transfer
@NotNullByDefault
public record ColorProfile(ColorPrimaries primaries, TransferFunction transfer) {
    /// Encoded sRGB.
    public static final ColorProfile SRGB = new ColorProfile(ColorPrimaries.SRGB, TransferFunction.SRGB);

    /// Linear-light sRGB.
    public static final ColorProfile LINEAR_SRGB = new ColorProfile(ColorPrimaries.SRGB, TransferFunction.LINEAR);

    /// Encoded Display-P3.
    public static final ColorProfile DISPLAY_P3 = new ColorProfile(
            ColorPrimaries.DISPLAY_P3,
            TransferFunction.SRGB
    );

    /// Encoded BT.2020.
    public static final ColorProfile BT2020 = new ColorProfile(ColorPrimaries.BT2020, TransferFunction.BT2020);

    /// BT.2100 PQ on BT.2020 primaries.
    public static final ColorProfile BT2100_PQ = new ColorProfile(ColorPrimaries.BT2020, TransferFunction.PQ);

    /// BT.2100 HLG on BT.2020 primaries.
    public static final ColorProfile BT2100_HLG = new ColorProfile(ColorPrimaries.BT2020, TransferFunction.HLG);

    /// Returns the tagged encoding that matches this profile, or [`ColorEncoding#EXTENDED_LINEAR`].
    ///
    /// @return the matching encoding
    public ColorEncoding encoding() {
        if (primaries == ColorPrimaries.SRGB && transfer == TransferFunction.SRGB) {
            return ColorEncoding.SRGB;
        }
        if (primaries == ColorPrimaries.SRGB && transfer == TransferFunction.LINEAR) {
            return ColorEncoding.LINEAR_SRGB;
        }
        if (primaries == ColorPrimaries.DISPLAY_P3 && transfer == TransferFunction.SRGB) {
            return ColorEncoding.DISPLAY_P3;
        }
        if (primaries == ColorPrimaries.DISPLAY_P3 && transfer == TransferFunction.LINEAR) {
            return ColorEncoding.LINEAR_DISPLAY_P3;
        }
        if (primaries == ColorPrimaries.BT2020 && transfer == TransferFunction.BT2020) {
            return ColorEncoding.BT2020;
        }
        if (primaries == ColorPrimaries.BT2020 && transfer == TransferFunction.LINEAR) {
            return ColorEncoding.LINEAR_BT2020;
        }
        if (primaries == ColorPrimaries.BT2020 && transfer == TransferFunction.PQ) {
            return ColorEncoding.BT2100_PQ;
        }
        if (primaries == ColorPrimaries.BT2020 && transfer == TransferFunction.HLG) {
            return ColorEncoding.BT2100_HLG;
        }
        return ColorEncoding.EXTENDED_LINEAR;
    }
}

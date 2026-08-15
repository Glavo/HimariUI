package org.glavo.himari.graphics;

import org.jetbrains.annotations.NotNullByDefault;

/// Identifies a tagged color encoding.
@NotNullByDefault
public enum ColorEncoding {
    /// Encoded sRGB primaries and transfer.
    SRGB,

    /// Linear-light sRGB primaries.
    LINEAR_SRGB,

    /// Encoded Display-P3.
    DISPLAY_P3,

    /// Linear-light Display-P3.
    LINEAR_DISPLAY_P3,

    /// Encoded BT.2020 with the BT.2020/BT.709 OETF.
    BT2020,

    /// Linear-light BT.2020.
    LINEAR_BT2020,

    /// BT.2100 PQ encoded on BT.2020 primaries. Conversion uses a 100-nit reference white.
    BT2100_PQ,

    /// BT.2100 HLG encoded on BT.2020 primaries. Conversion uses the HLG inverse OETF.
    BT2100_HLG,

    /// First-stable extended-linear working encoding. Components may be finite and outside `[0, 1]`.
    EXTENDED_LINEAR
}

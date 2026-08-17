package org.glavo.himari.graphics;

import org.jetbrains.annotations.NotNullByDefault;

/// Identifies a first-stable electro-optical transfer function.
@NotNullByDefault
public enum TransferFunction {
    /// Encoded sRGB / Display-P3 transfer.
    SRGB,

    /// Linear light.
    LINEAR,

    /// BT.2020 / BT.709 OETF.
    BT2020,

    /// BT.2100 PQ.
    PQ,

    /// BT.2100 HLG.
    HLG
}

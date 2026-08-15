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

    /// First-stable extended-linear working encoding. Components may be finite and outside `[0, 1]`.
    EXTENDED_LINEAR
}

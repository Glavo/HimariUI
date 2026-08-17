package org.glavo.himari.graphics;

import org.jetbrains.annotations.NotNullByDefault;

/// Describes how alpha is stored in a [`PixelBuffer`].
@NotNullByDefault
public enum AlphaInterpretation {
    /// Color channels are not premultiplied by alpha.
    UNASSOCIATED,

    /// Color channels are premultiplied by alpha.
    PREMULTIPLIED,

    /// Alpha is opaque and may be ignored.
    OPAQUE
}

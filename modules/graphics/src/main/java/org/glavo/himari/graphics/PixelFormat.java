package org.glavo.himari.graphics;

import org.jetbrains.annotations.NotNullByDefault;

/// Identifies a first-stable pixel storage format independent of color encoding.
@NotNullByDefault
public enum PixelFormat {
    /// 8-bit unorm RGBA.
    RGBA8,

    /// Packed 10-bit RGB plus 2-bit alpha.
    RGB10A2,

    /// 16-bit float RGBA.
    RGBA16F,

    /// 32-bit float RGBA.
    RGBA32F;

    /// Returns the storage bytes for one pixel.
    ///
    /// @return the stride
    public int bytesPerPixel() {
        return switch (this) {
            case RGBA8, RGB10A2 -> 4;
            case RGBA16F -> 8;
            case RGBA32F -> 16;
        };
    }
}

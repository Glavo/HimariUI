package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Objects;

/// Stores one `sbix` glyph strike payload.
///
/// @param ppem the strike pixels-per-em
/// @param originX the origin x offset in pixels
/// @param originY the origin y offset in pixels
/// @param graphicType the four-byte graphic tag such as `png `
/// @param data the graphic bytes
@NotNullByDefault
public record EmbeddedBitmap(
        int ppem,
        int originX,
        int originY,
        int graphicType,
        byte @Unmodifiable [] data
) {
    /// Validates the strike.
    public EmbeddedBitmap {
        Objects.requireNonNull(data, "data");
        if (ppem <= 0) {
            throw new IllegalArgumentException("sbix ppem must be positive");
        }
        data = data.clone();
    }
}

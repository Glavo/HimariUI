package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

/// Stores one COLR v0 layer: a glyph painted with a palette entry or the foreground.
///
/// @param glyphId the layer glyph
/// @param paletteIndex the CPAL entry, or [`PaletteColor#FOREGROUND`]
/// @param color the palette color, or `null` when the layer uses the foreground
/// @param translateX the applied `PaintVarTranslate` X offset in font units after variation
@NotNullByDefault
public record ColorLayer(int glyphId, int paletteIndex, @Nullable PaletteColor color, int translateX) {
    /// Validates the layer.
    public ColorLayer {
        if (glyphId < 0 || paletteIndex < 0 || paletteIndex > PaletteColor.FOREGROUND) {
            throw new IllegalArgumentException("Color layer glyph and palette index must be in range");
        }
        if (paletteIndex == PaletteColor.FOREGROUND && color != null) {
            throw new IllegalArgumentException("Foreground layers must not carry a palette color");
        }
        if (paletteIndex != PaletteColor.FOREGROUND && color == null) {
            throw new IllegalArgumentException("Palette layers must carry a color");
        }
    }
}

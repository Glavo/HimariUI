package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

/// Stores one COLR v0 layer: a glyph painted with a palette entry or the foreground.
///
/// @param glyphId the layer glyph
/// @param paletteIndex the CPAL entry, or [`PaletteColor#FOREGROUND`]
/// @param color the palette color, or `null` when the layer uses the foreground
/// @param translateX the applied `PaintVarTranslate` X offset in font units after variation
/// @param scaleX the applied `PaintVarScale` X factor as `F2DOT14` after variation, or `0`
/// @param rotate the applied `PaintVarRotate` angle as `F2DOT14` after variation, or `0`
/// @param translateY the applied `PaintVarTranslate` Y offset in font units after variation
/// @param skewX the applied `PaintVarSkew` X angle as `F2DOT14` after variation, or `0`
/// @param scaleY the applied `PaintVarScale` Y factor as `F2DOT14` after variation, or `0`
/// @param skewY the applied `PaintVarSkew` Y angle as `F2DOT14` after variation, or `0`
/// @param transformXx the applied `PaintVarTransform` `xx` as `16.16` after variation, or `0`
/// @param centerX the applied around-center X in font units after variation, or `0`
/// @param transformYx the applied `PaintVarTransform` `yx` as `16.16` after variation, or `0`
/// @param centerY the applied around-center Y in font units after variation, or `0`
/// @param transformXy the applied `PaintVarTransform` `xy` as `16.16` after variation, or `0`
/// @param transformYy the applied `PaintVarTransform` `yy` as `16.16` after variation, or `0`
/// @param transformDx the applied `PaintVarTransform` `dx` as `16.16` after variation, or `0`
/// @param transformDy the applied `PaintVarTransform` `dy` as `16.16` after variation, or `0`
@NotNullByDefault
public record ColorLayer(
        int glyphId,
        int paletteIndex,
        @Nullable PaletteColor color,
        int translateX,
        int scaleX,
        int rotate,
        int translateY,
        int skewX,
        int scaleY,
        int skewY,
        int transformXx,
        int centerX,
        int transformYx,
        int centerY,
        int transformXy,
        int transformYy,
        int transformDx,
        int transformDy
) {
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

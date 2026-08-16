package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;

/// Locates one packed grayscale glyph inside a [`GlyphAtlas`] sheet.
///
/// @param x the left edge in sheet pixels
/// @param y the bottom edge in sheet pixels, matching [`GlyphMask`] row 0
/// @param width the nonnegative packed width
/// @param height the nonnegative packed height
@NotNullByDefault
public record AtlasGlyph(int x, int y, int width, int height) {
    /// Validates the packed rectangle.
    public AtlasGlyph {
        if (x < 0 || y < 0 || width < 0 || height < 0) {
            throw new IllegalArgumentException("Atlas glyph rectangle must be nonnegative");
        }
    }
}

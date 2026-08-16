package org.glavo.himari.text;

import org.glavo.himari.font.SfntFont;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Objects;

/// Stores shaped glyphs whose [`ShapedGlyph#fontIndex()`] indexes [`#fonts()`].
///
/// @param fonts the document face table
/// @param glyphs the glyphs in logical order
@NotNullByDefault
public record ShapedText(SfntFont @Unmodifiable [] fonts, @Unmodifiable List<ShapedGlyph> glyphs) {
    /// Copies the table and glyph list.
    public ShapedText {
        Objects.requireNonNull(fonts, "fonts");
        Objects.requireNonNull(glyphs, "glyphs");
        fonts = fonts.clone();
        glyphs = List.copyOf(glyphs);
    }

    /// Returns the face that supplied `glyph`.
    ///
    /// @param glyph a glyph from [`#glyphs()`]
    /// @return the face
    public SfntFont fontOf(ShapedGlyph glyph) {
        Objects.requireNonNull(glyph, "glyph");
        int index = glyph.fontIndex();
        if (index >= fonts.length) {
            throw new IllegalArgumentException("Glyph font index exceeds the document table");
        }
        return fonts[index];
    }
}

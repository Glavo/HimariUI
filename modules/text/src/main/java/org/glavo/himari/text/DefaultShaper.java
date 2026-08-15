package org.glavo.himari.text;

import org.glavo.himari.font.SfntFont;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/// Maps one-to-one clusters through `cmap` and `hmtx` without OpenType substitution.
@NotNullByDefault
public final class DefaultShaper {
    /// Prevents instantiation.
    private DefaultShaper() {
    }

    /// Shapes a string with the default Latin/Greek/Cyrillic path.
    ///
    /// @param font the font
    /// @param text the source text
    /// @return the shaped glyphs
    public static @Unmodifiable List<ShapedGlyph> shape(SfntFont font, String text) {
        Objects.requireNonNull(font, "font");
        Objects.requireNonNull(text, "text");
        ArrayList<ShapedGlyph> glyphs = new ArrayList<>();
        text.codePoints().forEachOrdered(codePoint -> {
            int glyphId = font.glyphId(codePoint);
            glyphs.add(new ShapedGlyph(
                    codePoint,
                    glyphId,
                    glyphs.size(),
                    font.metrics(glyphId).advanceWidth()
            ));
        });
        return List.copyOf(glyphs);
    }
}

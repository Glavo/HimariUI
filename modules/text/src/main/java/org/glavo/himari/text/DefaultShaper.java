package org.glavo.himari.text;

import org.glavo.himari.font.SfntFont;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Arrays;
import java.util.Collections;
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
        int utf16Length = text.length();
        if (utf16Length == 0) {
            return List.of();
        }
        int count = text.codePointCount(0, utf16Length);
        ShapedGlyph[] glyphs = new ShapedGlyph[count];
        int cluster = 0;
        for (int index = 0; index < utf16Length; ) {
            int codePoint = text.codePointAt(index);
            int glyphId = font.glyphId(codePoint);
            glyphs[cluster] = new ShapedGlyph(
                    codePoint,
                    glyphId,
                    cluster,
                    font.metrics(glyphId).advanceWidth()
            );
            cluster++;
            index += Character.charCount(codePoint);
        }
        return Collections.unmodifiableList(Arrays.asList(glyphs));
    }
}

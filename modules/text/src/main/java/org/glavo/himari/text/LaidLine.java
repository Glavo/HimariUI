package org.glavo.himari.text;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Objects;

/// Stores one wrapped line of already-shaped glyphs.
///
/// @param glyphs the glyphs on this line, in visual-left-to-right order
/// @param width the sum of glyph X-advances in font units
/// @param startCluster the first source cluster on the line
/// @param endClusterExclusive the first cluster after the line
@NotNullByDefault
public record LaidLine(
        @Unmodifiable List<ShapedGlyph> glyphs,
        int width,
        int startCluster,
        int endClusterExclusive
) {
    /// Validates the line.
    public LaidLine {
        Objects.requireNonNull(glyphs, "glyphs");
        if (width < 0 || startCluster < 0 || endClusterExclusive < startCluster) {
            throw new IllegalArgumentException("Laid line extents must be nonnegative and ordered");
        }
        glyphs = List.copyOf(glyphs);
    }

    /// Returns the X origin of the caret before `cluster`, in font units from the line start.
    ///
    /// Clusters at or past [`#endClusterExclusive()`] return [`#width()`].
    ///
    /// @param cluster the source cluster
    /// @return the nonnegative caret X
    public int caretX(int cluster) {
        if (cluster <= startCluster) {
            return 0;
        }
        int x = 0;
        for (ShapedGlyph glyph : glyphs) {
            if (glyph.cluster() >= cluster) {
                return x;
            }
            x += glyph.xAdvance();
        }
        return width;
    }
}

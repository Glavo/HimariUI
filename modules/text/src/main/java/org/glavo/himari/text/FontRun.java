package org.glavo.himari.text;

import org.glavo.himari.font.SfntFont;
import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// One maximal substring that resolved to a single font.
///
/// @param startCluster the first source code-point index
/// @param endClusterExclusive the first cluster after the run
/// @param startUtf16 the UTF-16 start index in the source string
/// @param endUtf16 the UTF-16 end index, exclusive
/// @param font the resolved face
/// @param fontIndex the index in the [`FontCollection`]
/// @param missingGlyph whether any cluster in the run mapped to `.notdef`
@NotNullByDefault
public record FontRun(
        int startCluster,
        int endClusterExclusive,
        int startUtf16,
        int endUtf16,
        SfntFont font,
        int fontIndex,
        boolean missingGlyph
) {
    /// Validates the run extents.
    public FontRun {
        Objects.requireNonNull(font, "font");
        if (startCluster < 0
                || endClusterExclusive < startCluster
                || startUtf16 < 0
                || endUtf16 < startUtf16
                || fontIndex < 0) {
            throw new IllegalArgumentException("Font run extents must be nonnegative and ordered");
        }
    }
}

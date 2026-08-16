package org.glavo.himari.text;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/// Shapes a string through [`FontCollection`] runs and [`DefaultShaper`].
///
/// Each run is shaped independently so OpenType joining does not cross a face boundary. Glyph
/// clusters are shifted by the run's starting cluster. Advances and GPOS offsets are converted to
/// the primary face's units per em with integer truncation toward zero. A single primary run
/// returns [`DefaultShaper#shape(SfntFont, String)`] unchanged.
@NotNullByDefault
public final class FallbackShaper {
    /// Prevents instantiation.
    private FallbackShaper() {
    }

    /// Shapes `text` with fallback segmentation.
    ///
    /// @param fonts the ordered faces
    /// @param text the source text
    /// @return the shaped glyphs in logical order
    public static @Unmodifiable List<ShapedGlyph> shape(FontCollection fonts, String text) {
        Objects.requireNonNull(fonts, "fonts");
        Objects.requireNonNull(text, "text");
        if (text.isEmpty()) {
            return List.of();
        }
        List<FontRun> runs = fonts.segment(text);
        if (runs.size() == 1 && !runs.getFirst().missingGlyph() && runs.getFirst().fontIndex() == 0) {
            return DefaultShaper.shape(fonts.primary(), text);
        }
        int primaryEm = fonts.primary().unitsPerEm();
        ShapedGlyph[] glyphs = new ShapedGlyph[text.length()];
        int written = 0;
        for (int runIndex = 0; runIndex < runs.size(); runIndex++) {
            FontRun run = runs.get(runIndex);
            String slice = text.substring(run.startUtf16(), run.endUtf16());
            List<ShapedGlyph> shaped = DefaultShaper.shape(run.font(), slice);
            int fromEm = run.font().unitsPerEm();
            for (int glyphIndex = 0; glyphIndex < shaped.size(); glyphIndex++) {
                ShapedGlyph glyph = shaped.get(glyphIndex);
                if (written == glyphs.length) {
                    glyphs = Arrays.copyOf(glyphs, glyphs.length * 2);
                }
                glyphs[written++] = remap(glyph, run.startCluster(), run.fontIndex(), fromEm, primaryEm);
            }
        }
        if (written != glyphs.length) {
            glyphs = Arrays.copyOf(glyphs, written);
        }
        return Collections.unmodifiableList(Arrays.asList(glyphs));
    }

    /// Shifts cluster and font index and converts units to the primary em.
    ///
    /// @param glyph the run-local glyph
    /// @param clusterBase the run's first source cluster
    /// @param fontIndex the collection index
    /// @param fromEm the run face's units per em
    /// @param toEm the primary units per em
    /// @return the paragraph-scoped glyph
    private static ShapedGlyph remap(
            ShapedGlyph glyph,
            int clusterBase,
            int fontIndex,
            int fromEm,
            int toEm
    ) {
        int cluster = glyph.cluster() + clusterBase;
        int xAdvance = scale(glyph.xAdvance(), fromEm, toEm);
        int xOffset = scale(glyph.xOffset(), fromEm, toEm);
        int yOffset = scale(glyph.yOffset(), fromEm, toEm);
        if (cluster == glyph.cluster()
                && fontIndex == glyph.fontIndex()
                && xAdvance == glyph.xAdvance()
                && xOffset == glyph.xOffset()
                && yOffset == glyph.yOffset()) {
            return glyph;
        }
        return new ShapedGlyph(
                glyph.codePoint(),
                glyph.glyphId(),
                cluster,
                xAdvance,
                xOffset,
                yOffset,
                fontIndex
        );
    }

    /// Converts a signed font-unit value to `toEm` with truncation toward zero.
    ///
    /// @param value the value in `fromEm` units
    /// @param fromEm the source units per em
    /// @param toEm the destination units per em
    /// @return the converted value
    static int scale(int value, int fromEm, int toEm) {
        if (value == 0 || fromEm == toEm) {
            return value;
        }
        if (fromEm <= 0 || toEm <= 0) {
            throw new IllegalArgumentException("unitsPerEm must be positive");
        }
        return (int) ((long) value * toEm / fromEm);
    }
}

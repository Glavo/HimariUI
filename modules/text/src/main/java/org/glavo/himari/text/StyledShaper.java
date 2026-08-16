package org.glavo.himari.text;

import org.glavo.himari.font.SfntFont;
import org.jetbrains.annotations.NotNullByDefault;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;

/// Shapes consecutive [`TextSpan`] values into one document glyph stream.
///
/// Each span is shaped through [`FallbackShaper`]. Clusters increase across spans by each span's
/// code-point count. Face identities are interned into one table so [`ShapedGlyph#fontIndex()`]
/// is document-scoped. Advances and offsets are converted to the first span's primary units per
/// em with truncation toward zero. Empty input yields an empty table and no glyphs.
@NotNullByDefault
public final class StyledShaper {
    /// Prevents instantiation.
    private StyledShaper() {
    }

    /// Shapes `spans` in order.
    ///
    /// @param spans the styled runs
    /// @return the document glyphs
    public static ShapedText shape(TextSpan... spans) {
        Objects.requireNonNull(spans, "spans");
        if (spans.length == 0) {
            return new ShapedText(new SfntFont[0], List.of());
        }
        int layoutEm = 0;
        IdentityHashMap<SfntFont, Integer> indexOf = new IdentityHashMap<>();
        ArrayList<SfntFont> table = new ArrayList<>();
        ArrayList<ShapedGlyph> glyphs = new ArrayList<>();
        int clusterBase = 0;
        for (int spanIndex = 0; spanIndex < spans.length; spanIndex++) {
            TextSpan span = Objects.requireNonNull(spans[spanIndex], "span");
            if (layoutEm == 0) {
                layoutEm = span.fonts().primary().unitsPerEm();
            }
            List<ShapedGlyph> shaped = FallbackShaper.shape(span.fonts(), span.text());
            int[] remap = intern(span.fonts(), indexOf, table);
            int fromEm = span.fonts().primary().unitsPerEm();
            for (int glyphIndex = 0; glyphIndex < shaped.size(); glyphIndex++) {
                ShapedGlyph glyph = shaped.get(glyphIndex);
                glyphs.add(new ShapedGlyph(
                        glyph.codePoint(),
                        glyph.glyphId(),
                        glyph.cluster() + clusterBase,
                        FallbackShaper.scale(glyph.xAdvance(), fromEm, layoutEm),
                        FallbackShaper.scale(glyph.xOffset(), fromEm, layoutEm),
                        FallbackShaper.scale(glyph.yOffset(), fromEm, layoutEm),
                        remap[glyph.fontIndex()]
                ));
            }
            clusterBase += span.text().codePointCount(0, span.text().length());
        }
        return new ShapedText(table.toArray(SfntFont[]::new), glyphs);
    }

    /// Assigns document indices to the faces in `fonts`.
    ///
    /// @param fonts the span collection
    /// @param indexOf identity map of already interned faces
    /// @param table the document face table
    /// @return span-local index to document index
    private static int[] intern(
            FontCollection fonts,
            IdentityHashMap<SfntFont, Integer> indexOf,
            List<SfntFont> table
    ) {
        int[] remap = new int[fonts.size()];
        for (int index = 0; index < remap.length; index++) {
            SfntFont font = fonts.font(index);
            Integer existing = indexOf.get(font);
            if (existing != null) {
                remap[index] = existing;
            } else {
                int documentIndex = table.size();
                indexOf.put(font, documentIndex);
                table.add(font);
                remap[index] = documentIndex;
            }
        }
        return remap;
    }
}

package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/// Verifies GDEF mark classes and `IgnoreMarks` ligature matching.
@NotNullByDefault
final class GdefTableTest {
    /// Classifies `B` as a mark and ligates `A` plus `C` across it.
    @Test
    void ignoreMarksSkipsGdefMarkBetweenLigatureComponents() {
        SfntFont font = GdefMarkSampleFont.create();
        int a = font.glyphId('A');
        int b = font.glyphId('B');
        int c = font.glyphId('C');
        assertEquals(0, font.glyphClass(a));
        assertEquals(GdefTable.CLASS_MARK, font.glyphClass(b));
        @Nullable GlyphLigature across = font.ligature(
                new int[] {a, b, c},
                0,
                3,
                SfntFont.TAG_RLIG
        );
        assertNotNull(across);
        assertEquals(GdefMarkSampleFont.GLYPH_X, across.glyphId());
        assertEquals(3, across.consumed());
        @Nullable GlyphLigature direct = font.ligature(new int[] {a, c}, 0, 2, SfntFont.TAG_RLIG);
        assertNotNull(direct);
        assertEquals(GdefMarkSampleFont.GLYPH_X, direct.glyphId());
        assertEquals(2, direct.consumed());
    }
}

package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies GPOS type-5 mark-to-ligature anchors on the constructed font.
@NotNullByDefault
final class GposMarkLigTest {
    /// Attaches fatha to ligature `A` at the constructed anchor difference.
    @Test
    void placesFathaOnLigatureA() {
        SfntFont font = GposMarkLigSampleFont.create();
        int liga = font.glyphId('A');
        int fatha = font.glyphId('\u064E');
        assertEquals(GposMarkLigSampleFont.GLYPH_LIGA, liga);
        assertEquals(GposMarkLigSampleFont.GLYPH_FATHA, fatha);
        assertTrue(font.isMark(fatha));
        assertFalse(font.isMark(liga));
        @Nullable MarkPlacement placement = font.markPlacement(fatha, liga);
        assertNotNull(placement);
        assertEquals(GposMarkLigSampleFont.MARK_X_OFFSET, placement.xOffset());
        assertEquals(GposMarkLigSampleFont.MARK_Y_OFFSET, placement.yOffset());
    }
}

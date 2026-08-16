package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies GPOS type-4 mark-to-base anchors on the constructed font.
@NotNullByDefault
final class GposMarkTest {
    /// Attaches fatha to Beh at the constructed anchor difference.
    @Test
    void placesFathaOnBeh() {
        SfntFont font = GposMarkSampleFont.create();
        int beh = font.glyphId('\u0628');
        int fatha = font.glyphId('\u064E');
        assertEquals(GposMarkSampleFont.GLYPH_BEH, beh);
        assertEquals(GposMarkSampleFont.GLYPH_FATHA, fatha);
        assertTrue(font.isMark(fatha));
        assertFalse(font.isMark(beh));
        @Nullable MarkPlacement placement = font.markPlacement(fatha, beh);
        assertNotNull(placement);
        assertEquals(GposMarkSampleFont.MARK_X_OFFSET, placement.xOffset());
        assertEquals(GposMarkSampleFont.MARK_Y_OFFSET, placement.yOffset());
        assertNull(font.markPlacement(beh, fatha));
    }
}

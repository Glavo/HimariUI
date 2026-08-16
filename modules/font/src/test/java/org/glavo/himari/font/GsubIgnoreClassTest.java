package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/// Verifies GSUB `IgnoreBaseGlyphs` and `IgnoreLigatures` through [`SfntFont`] entries.
@NotNullByDefault
final class GsubIgnoreClassTest {
    /// Substitutes across a base and ligates across a ligature.
    @Test
    void ignoreBaseAndLigatureSkipMatchingClasses() {
        SfntFont font = GsubIgnoreClassSampleFont.create();
        int a = font.glyphId('A');
        int b = font.glyphId('B');
        int c = font.glyphId('C');
        int d = font.glyphId('D');
        int x = font.glyphId('X');
        assertEquals(GdefTable.CLASS_BASE, font.glyphClass(b));
        assertEquals(GdefTable.CLASS_LIGATURE, font.glyphClass(d));
        assertEquals(x, font.contextSubstitute(new int[] {a, b, c}, 0, 3, SfntFont.TAG_CALT));
        assertEquals(a, font.contextSubstitute(new int[] {a, d, c}, 0, 3, SfntFont.TAG_CALT));
        @Nullable GlyphLigature across = font.ligature(new int[] {a, d, c}, 0, 3, SfntFont.TAG_RLIG);
        assertNotNull(across);
        assertEquals(x, across.glyphId());
        assertEquals(3, across.consumed());
        assertNull(font.ligature(new int[] {a, b, c}, 0, 3, SfntFont.TAG_RLIG));
    }
}

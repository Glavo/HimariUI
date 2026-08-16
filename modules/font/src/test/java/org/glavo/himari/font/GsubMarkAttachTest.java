package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/// Verifies GSUB `MarkAttachmentType` through [`SfntFont`] ligature and context entries.
@NotNullByDefault
final class GsubMarkAttachTest {
    /// Substitutes and ligates `A` plus `C` across attach class 2, but not across class 1.
    @Test
    void markAttachmentTypeSkipsOtherMarkClass() {
        SfntFont font = GsubMarkAttachSampleFont.create();
        int a = font.glyphId('A');
        int b = font.glyphId('B');
        int c = font.glyphId('C');
        int d = font.glyphId('D');
        int x = font.glyphId('X');
        assertEquals(2, font.markAttachClass(b));
        assertEquals(GsubMarkAttachSampleFont.ATTACH_TYPE, font.markAttachClass(d));
        assertEquals(a, font.contextSubstitute(a, b, SfntFont.TAG_CALT));
        assertEquals(x, font.contextSubstitute(new int[] {a, b, c}, 0, 3, SfntFont.TAG_CALT));
        assertEquals(a, font.contextSubstitute(new int[] {a, d, c}, 0, 3, SfntFont.TAG_CALT));
        @Nullable GlyphLigature across = font.ligature(new int[] {a, b, c}, 0, 3, SfntFont.TAG_RLIG);
        assertNotNull(across);
        assertEquals(x, across.glyphId());
        assertEquals(3, across.consumed());
        assertNull(font.ligature(new int[] {a, d, c}, 0, 3, SfntFont.TAG_RLIG));
    }
}

package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies cmap format-12 supplementary-plane mapping.
@NotNullByDefault
final class Cmap12Test {
    /// Maps `U+1F600` through a format-12-only face.
    @Test
    void mapsSupplementaryPlaneGlyph() {
        SfntFont font = Cmap12SampleFont.create();
        assertEquals(Cmap12SampleFont.GLYPH_SPACE, font.glyphId(' '));
        assertEquals(Cmap12SampleFont.GLYPH_GRIN, font.glyphId(Cmap12SampleFont.GRIN));
        assertTrue(font.hasGlyph(Cmap12SampleFont.GRIN));
        assertFalse(font.hasGlyph('A'));
        assertEquals(0, font.glyphId('A'));
        assertEquals(8, font.metrics(Cmap12SampleFont.GLYPH_GRIN).advanceWidth());
    }

    /// Keeps format-4 Latin mapping on a Windows TrueType face that also has format 12.
    @Test
    void format4StillMapsArial() {
        java.nio.file.Path directory = FontDirectories.windowsFonts();
        if (directory == null) {
            return;
        }
        java.nio.file.Path arial = directory.resolve("arial.ttf");
        if (!java.nio.file.Files.isRegularFile(arial)) {
            return;
        }
        SfntFont font = FontDirectories.tryOpen(arial);
        if (font == null) {
            return;
        }
        assertTrue(font.hasGlyph('A'));
        assertTrue(font.glyphId('A') > 0);
    }
}

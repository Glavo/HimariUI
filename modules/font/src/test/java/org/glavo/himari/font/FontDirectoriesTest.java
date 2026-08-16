package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies host font-directory listing and SFNT open on Windows.
@NotNullByDefault
final class FontDirectoriesTest {
    /// Finds `%WINDIR%\Fonts` and opens at least one TrueType face that maps `A`.
    @Test
    @EnabledOnOs(OS.WINDOWS)
    void opensWindowsTrueTypeLatin() {
        @Nullable Path directory = FontDirectories.windowsFonts();
        assertNotNull(directory);
        List<Path> files = FontDirectories.listSfnt(directory);
        assertFalse(files.isEmpty());
        @Nullable SfntFont opened = null;
        for (int index = 0; index < files.size(); index++) {
            @Nullable SfntFont font = FontDirectories.tryOpen(files.get(index));
            if (font != null && font.hasGlyph('A')) {
                opened = font;
                break;
            }
        }
        assertNotNull(opened);
        assertTrue(opened.hasGlyph('A'));
        assertTrue(opened.glyphId('A') > 0);
        assertTrue(opened.unitsPerEm() > 0);
    }

    /// Recognizes listed SFNT extensions without opening a file.
    @Test
    void acceptsSfntExtensions() {
        assertTrue(FontDirectories.isSfntName("Arial.ttf"));
        assertTrue(FontDirectories.isSfntName("segoeui.OTF"));
        assertTrue(FontDirectories.isSfntName("msgothic.ttc"));
        assertFalse(FontDirectories.isSfntName("readme.txt"));
        assertFalse(FontDirectories.isSfntName("font"));
    }
}

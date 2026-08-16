package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/// Locates installed font files without calling DirectWrite, CoreText, or Pango.
///
/// Windows uses `%WINDIR%\Fonts`. Other hosts currently report no system directory. Listing is
/// one directory level and does not follow a recursive tree. [`#tryOpen(Path)`] parses a single
/// SFNT face and returns `null` for TrueType collections, CFF-only faces, and other files this
/// loader cannot read.
@NotNullByDefault
public final class FontDirectories {
    /// TrueType collection tag `ttcf`.
    private static final int TAG_TTCF = 0x74746366;

    /// Prevents instantiation.
    private FontDirectories() {
    }

    /// Returns the Windows Fonts directory when this process is a Windows host and the directory
    /// exists.
    ///
    /// @return the directory, or `null`
    public static @Nullable Path windowsFonts() {
        @Nullable String windir = System.getenv("WINDIR");
        if (windir == null || windir.isEmpty()) {
            return null;
        }
        Path directory = Path.of(windir, "Fonts");
        if (!Files.isDirectory(directory)) {
            return null;
        }
        return directory;
    }

    /// Lists `.ttf`, `.otf`, and `.ttc` files directly under `directory`.
    ///
    /// @param directory the font directory
    /// @return paths sorted by file name
    public static @Unmodifiable List<Path> listSfnt(Path directory) {
        Objects.requireNonNull(directory, "directory");
        if (!Files.isDirectory(directory)) {
            throw new IllegalArgumentException("Font directory does not exist: " + directory);
        }
        ArrayList<Path> files = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            for (Path path : stream) {
                if (Files.isRegularFile(path) && isSfntName(path.getFileName().toString())) {
                    files.add(path);
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot list font directory " + directory, exception);
        }
        files.sort(Comparator.comparing(path -> path.getFileName().toString().toLowerCase(Locale.ROOT)));
        return List.copyOf(files);
    }

    /// Parses `path` as a single-font SFNT when this loader can read it.
    ///
    /// @param path the font file
    /// @return the face, or `null` when the file is a collection or unsupported
    public static @Nullable SfntFont tryOpen(Path path) {
        Objects.requireNonNull(path, "path");
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(path);
        } catch (IOException exception) {
            return null;
        }
        if (bytes.length < 4) {
            return null;
        }
        int tag = ((bytes[0] & 0xFF) << 24)
                | ((bytes[1] & 0xFF) << 16)
                | ((bytes[2] & 0xFF) << 8)
                | (bytes[3] & 0xFF);
        if (tag == TAG_TTCF) {
            return null;
        }
        try {
            return new SfntFont(bytes);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    /// Returns whether `name` uses a listed SFNT extension.
    ///
    /// @param name the file name
    /// @return whether the extension is `.ttf`, `.otf`, or `.ttc`
    static boolean isSfntName(String name) {
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return false;
        }
        String extension = name.substring(dot + 1).toLowerCase(Locale.ROOT);
        return extension.equals("ttf") || extension.equals("otf") || extension.equals("ttc");
    }
}

package org.glavo.himari.spikes.win32;

import org.jetbrains.annotations.NotNullByDefault;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/// Provides the small deterministic JSON surface required by the Win32 evidence writer.
@NotNullByDefault
final class JsonSupport {
    /// Prevents instantiation of this utility class.
    private JsonSupport() {
    }

    /// Quotes one string as a JSON string literal.
    ///
    /// @param value the source string
    /// @return the escaped JSON literal
    static String quote(String value) {
        StringBuilder result = new StringBuilder(value.length() + 2).append('"');
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"' -> result.append("\\\"");
                case '\\' -> result.append("\\\\");
                case '\b' -> result.append("\\b");
                case '\f' -> result.append("\\f");
                case '\n' -> result.append("\\n");
                case '\r' -> result.append("\\r");
                case '\t' -> result.append("\\t");
                default -> {
                    if (character < 0x20) {
                        result.append(String.format(Locale.ROOT, "\\u%04x", (int) character));
                    } else {
                        result.append(character);
                    }
                }
            }
        }
        return result.append('"').toString();
    }

    /// Formats a finite float as a JSON number or a non-finite value as `null`.
    ///
    /// @param value the native floating-point value
    /// @return a JSON number or `null`
    static String number(float value) {
        return Float.isFinite(value) ? Float.toString(value) : "null";
    }

    /// Writes one UTF-8 JSON artifact after creating its parent directory.
    ///
    /// @param path the artifact path
    /// @param json the complete JSON document
    /// @throws IllegalStateException if the artifact cannot be written
    static void write(Path path, String json) {
        try {
            Files.createDirectories(path.toAbsolutePath().normalize().getParent());
            Files.writeString(path, json, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot write Win32 conformance evidence " + path, exception);
        }
    }
}

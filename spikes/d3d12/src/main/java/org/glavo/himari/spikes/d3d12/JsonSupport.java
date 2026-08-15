package org.glavo.himari.spikes.d3d12;

import org.jetbrains.annotations.NotNullByDefault;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/// Provides deterministic JSON and text output for D3D12 conformance evidence.
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

    /// Writes one UTF-8 evidence artifact after creating its parent directory.
    ///
    /// @param path the artifact path
    /// @param content the complete artifact content
    /// @throws IllegalStateException if the artifact cannot be written
    static void write(Path path, String content) {
        try {
            Path normalized = path.toAbsolutePath().normalize();
            Files.createDirectories(normalized.getParent());
            Files.writeString(normalized, content, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot write D3D12 conformance evidence " + path, exception);
        }
    }
}

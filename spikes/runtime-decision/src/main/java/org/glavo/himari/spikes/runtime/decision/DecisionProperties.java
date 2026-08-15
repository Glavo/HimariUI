package org.glavo.himari.spikes.runtime.decision;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.TreeMap;

/// Reads and writes deterministic UTF-8 property evidence and computes artifact digests.
@NotNullByDefault
final class DecisionProperties {
    /// Prevents construction.
    private DecisionProperties() {
    }

    /// Reads a property file into an immutable key-sorted map.
    ///
    /// @param path the property file
    /// @return the decoded immutable properties
    /// @throws IOException if the file cannot be read
    static @Unmodifiable Map<String, String> read(Path path) throws IOException {
        String text = Files.readString(Objects.requireNonNull(path, "path"), StandardCharsets.UTF_8);
        Properties properties = new Properties();
        try (Reader reader = new StringReader(text)) {
            properties.load(reader);
        }
        TreeMap<String, String> values = new TreeMap<>();
        for (String name : properties.stringPropertyNames()) {
            values.put(name, properties.getProperty(name));
        }
        return Collections.unmodifiableMap(values);
    }

    /// Writes an immutable key-sorted property map without a timestamp header.
    ///
    /// @param path the output file
    /// @param values the property values
    /// @throws IOException if the file cannot be written
    static void write(Path path, Map<String, String> values) throws IOException {
        Objects.requireNonNull(path, "path");
        TreeMap<String, String> sorted = new TreeMap<>(Objects.requireNonNull(values, "values"));
        StringBuilder output = new StringBuilder();
        for (Map.Entry<String, String> entry : sorted.entrySet()) {
            output.append(escape(entry.getKey(), true))
                    .append('=')
                    .append(escape(entry.getValue(), false))
                    .append('\n');
        }
        Path parent = path.toAbsolutePath().normalize().getParent();
        if (parent == null) {
            throw new IllegalArgumentException("Property file has no parent directory: " + path);
        }
        Files.createDirectories(parent);
        Files.writeString(path, output, StandardCharsets.UTF_8);
    }

    /// Returns one required property value.
    ///
    /// @param values the property map
    /// @param key the required key
    /// @return the nonblank value
    /// @throws IllegalArgumentException if the key is absent or blank
    static String require(Map<String, String> values, String key) {
        String value = Objects.requireNonNull(values, "values").get(Objects.requireNonNull(key, "key"));
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required evidence property: " + key);
        }
        return value;
    }

    /// Parses one required nonnegative decimal long.
    ///
    /// @param values the property map
    /// @param key the required key
    /// @return the parsed value
    /// @throws IllegalArgumentException if the value is absent, malformed, or negative
    static long requireNonNegativeLong(Map<String, String> values, String key) {
        long value;
        try {
            value = Long.parseLong(require(values, key));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Evidence property is not a decimal long: " + key, exception);
        }
        if (value < 0L) {
            throw new IllegalArgumentException("Evidence property must be nonnegative: " + key);
        }
        return value;
    }

    /// Parses one required Boolean written as lowercase `true` or `false`.
    ///
    /// @param values the property map
    /// @param key the required key
    /// @return the parsed value
    /// @throws IllegalArgumentException if the value has another spelling
    static boolean requireBoolean(Map<String, String> values, String key) {
        return switch (require(values, key)) {
            case "true" -> true;
            case "false" -> false;
            default -> throw new IllegalArgumentException("Evidence property is not a Boolean: " + key);
        };
    }

    /// Computes the lowercase SHA-256 digest of one file.
    ///
    /// @param path the input file
    /// @return the 64-character hexadecimal digest
    /// @throws IOException if the file cannot be read
    static String sha256(Path path) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new AssertionError("The Java runtime does not provide SHA-256", exception);
        }
        try (var input = Files.newInputStream(Objects.requireNonNull(path, "path"))) {
            byte[] buffer = new byte[64 * 1024];
            while (true) {
                int count = input.read(buffer);
                if (count < 0) {
                    break;
                }
                digest.update(buffer, 0, count);
            }
        }
        StringBuilder result = new StringBuilder(64);
        for (byte value : digest.digest()) {
            result.append(Character.forDigit((value >>> 4) & 0xf, 16));
            result.append(Character.forDigit(value & 0xf, 16));
        }
        return result.toString();
    }

    /// Escapes one key or value according to the `Properties.load` text grammar.
    ///
    /// @param value the unescaped text
    /// @param key whether the text is a property key
    /// @return the escaped text
    private static String escape(String value, boolean key) {
        Objects.requireNonNull(value, "property text");
        StringBuilder result = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '\\' -> result.append("\\\\");
                case '\n' -> result.append("\\n");
                case '\r' -> result.append("\\r");
                case '\t' -> result.append("\\t");
                case '=', ':' -> {
                    if (key) {
                        result.append('\\');
                    }
                    result.append(character);
                }
                case ' ' -> {
                    if (key || index == 0) {
                        result.append('\\');
                    }
                    result.append(' ');
                }
                default -> result.append(character);
            }
        }
        return result.toString();
    }
}

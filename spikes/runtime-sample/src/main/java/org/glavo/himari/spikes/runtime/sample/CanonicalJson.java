package org.glavo.himari.spikes.runtime.sample;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;

/// Encodes report value trees as deterministic UTF-8-ready JSON with lexicographically sorted keys.
@NotNullByDefault
public final class CanonicalJson {
    /// Hexadecimal digits used for control-character escaping.
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    /// Prevents construction.
    private CanonicalJson() {
    }

    /// Encodes a supported value and appends one trailing line feed.
    ///
    /// Supported values are `null`, strings, booleans, integral numbers, maps with string keys, and
    /// collections. Floating-point values are deliberately excluded from the comparison protocol.
    ///
    /// @param value the value tree
    /// @return canonical JSON
    public static String write(@Nullable Object value) {
        StringBuilder output = new StringBuilder();
        append(output, value, 0);
        output.append('\n');
        return output.toString();
    }

    /// Appends one JSON value.
    ///
    /// @param output the output buffer
    /// @param value the value
    /// @param indent the current indentation
    private static void append(StringBuilder output, @Nullable Object value, int indent) {
        if (value == null) {
            output.append("null");
        } else if (value instanceof String text) {
            appendString(output, text);
        } else if (value instanceof Boolean || value instanceof Byte || value instanceof Short
                || value instanceof Integer || value instanceof Long) {
            output.append(value);
        } else if (value instanceof Map<?, ?> map) {
            appendMap(output, map, indent);
        } else if (value instanceof Collection<?> collection) {
            appendCollection(output, collection, indent);
        } else {
            throw new IllegalArgumentException("Unsupported canonical JSON value: " + value.getClass().getName());
        }
    }

    /// Appends a key-sorted JSON object.
    ///
    /// @param output the output buffer
    /// @param map the map
    /// @param indent the current indentation
    private static void appendMap(StringBuilder output, Map<?, ?> map, int indent) {
        ArrayList<Map.Entry<?, ?>> entries = new ArrayList<>(map.entrySet());
        entries.sort(Comparator.comparing(entry -> {
            if (!(entry.getKey() instanceof String key)) {
                throw new IllegalArgumentException("Canonical JSON object keys must be strings");
            }
            return key;
        }));
        output.append('{');
        if (!entries.isEmpty()) {
            output.append('\n');
            for (int index = 0; index < entries.size(); index++) {
                Map.Entry<?, ?> entry = entries.get(index);
                appendIndent(output, indent + 2);
                appendString(output, (String) entry.getKey());
                output.append(": ");
                append(output, entry.getValue(), indent + 2);
                output.append(index + 1 == entries.size() ? '\n' : ',').append(index + 1 == entries.size() ? "" : "\n");
            }
            appendIndent(output, indent);
        }
        output.append('}');
    }

    /// Appends a JSON array in source iteration order.
    ///
    /// @param output the output buffer
    /// @param collection the collection
    /// @param indent the current indentation
    private static void appendCollection(StringBuilder output, Collection<?> collection, int indent) {
        output.append('[');
        if (!collection.isEmpty()) {
            output.append('\n');
            int index = 0;
            for (@Nullable Object element : collection) {
                appendIndent(output, indent + 2);
                append(output, element, indent + 2);
                output.append(++index == collection.size() ? '\n' : ',').append(index == collection.size() ? "" : "\n");
            }
            appendIndent(output, indent);
        }
        output.append(']');
    }

    /// Appends a JSON string with mandatory escapes.
    ///
    /// @param output the output buffer
    /// @param value the string
    private static void appendString(StringBuilder output, String value) {
        output.append('"');
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"' -> output.append("\\\"");
                case '\\' -> output.append("\\\\");
                case '\b' -> output.append("\\b");
                case '\f' -> output.append("\\f");
                case '\n' -> output.append("\\n");
                case '\r' -> output.append("\\r");
                case '\t' -> output.append("\\t");
                default -> {
                    if (character < 0x20 || character == '\u2028' || character == '\u2029') {
                        appendUnicodeEscape(output, character);
                    } else {
                        output.append(character);
                    }
                }
            }
        }
        output.append('"');
    }

    /// Appends a four-digit lowercase Unicode escape.
    ///
    /// @param output the output buffer
    /// @param value the UTF-16 code unit
    private static void appendUnicodeEscape(StringBuilder output, char value) {
        output.append("\\u")
                .append(HEX[(value >>> 12) & 0xf])
                .append(HEX[(value >>> 8) & 0xf])
                .append(HEX[(value >>> 4) & 0xf])
                .append(HEX[value & 0xf]);
    }

    /// Appends spaces for one indentation level.
    ///
    /// @param output the output buffer
    /// @param count the nonnegative space count
    private static void appendIndent(StringBuilder output, int count) {
        output.repeat(' ', count);
    }
}

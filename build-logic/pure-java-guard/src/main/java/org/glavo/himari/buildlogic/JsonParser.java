package org.glavo.himari.buildlogic;

import org.gradle.api.GradleException;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Parses the JSON subset used by repository governance documents without adding a runtime dependency.
@NotNullByDefault
final class JsonParser {
    /// The source path included in parse failures.
    private final Path sourcePath;

    /// The complete JSON source.
    private final String source;

    /// The next source offset to consume.
    private int offset;

    /// Creates a parser for one source document.
    ///
    /// @param sourcePath the path used to identify parse failures
    /// @param source the complete JSON source
    private JsonParser(Path sourcePath, String source) {
        this.sourcePath = sourcePath;
        this.source = source;
    }

    /// Reads and parses a JSON document whose root must be an object.
    ///
    /// @param path the UTF-8 JSON file
    /// @return the parsed root object
    static JsonObject parseObject(Path path) {
        String source;
        try {
            source = Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new GradleException("Cannot read JSON document " + path, exception);
        }

        JsonParser parser = new JsonParser(path, source);
        JsonValue value = parser.parseValue();
        parser.skipWhitespace();
        if (parser.offset != source.length()) {
            throw parser.failure("Unexpected trailing content");
        }
        if (value instanceof JsonObject object) {
            return object;
        }
        throw parser.failure("The document root must be an object");
    }

    /// Parses the next JSON value.
    ///
    /// @return the parsed value
    private JsonValue parseValue() {
        skipWhitespace();
        if (offset == source.length()) {
            throw failure("Expected a value");
        }

        return switch (source.charAt(offset)) {
            case '{' -> parseObject();
            case '[' -> parseArray();
            case '"' -> new JsonString(parseString());
            case 't' -> parseLiteral("true", new JsonBoolean(true));
            case 'f' -> parseLiteral("false", new JsonBoolean(false));
            case 'n' -> parseLiteral("null", JsonNull.INSTANCE);
            default -> parseNumber();
        };
    }

    /// Parses an object beginning at the current offset.
    ///
    /// @return the parsed object
    private JsonObject parseObject() {
        expect('{');
        skipWhitespace();
        Map<String, JsonValue> members = new LinkedHashMap<>();
        if (consume('}')) {
            return new JsonObject(Map.copyOf(members));
        }

        while (true) {
            skipWhitespace();
            if (offset == source.length() || source.charAt(offset) != '"') {
                throw failure("Expected an object member name");
            }
            String name = parseString();
            skipWhitespace();
            expect(':');
            @Nullable JsonValue previous = members.put(name, parseValue());
            if (previous != null) {
                throw failure("Duplicate object member '" + name + "'");
            }
            skipWhitespace();
            if (consume('}')) {
                return new JsonObject(Map.copyOf(members));
            }
            expect(',');
        }
    }

    /// Parses an array beginning at the current offset.
    ///
    /// @return the parsed array
    private JsonArray parseArray() {
        expect('[');
        skipWhitespace();
        List<JsonValue> elements = new ArrayList<>();
        if (consume(']')) {
            return new JsonArray(List.copyOf(elements));
        }

        while (true) {
            elements.add(parseValue());
            skipWhitespace();
            if (consume(']')) {
                return new JsonArray(List.copyOf(elements));
            }
            expect(',');
        }
    }

    /// Parses a string beginning at the current offset.
    ///
    /// @return the decoded string
    private String parseString() {
        expect('"');
        StringBuilder result = new StringBuilder();
        while (offset < source.length()) {
            char current = source.charAt(offset++);
            if (current == '"') {
                return result.toString();
            }
            if (current == '\\') {
                result.append(parseEscape());
            } else {
                if (current < 0x20) {
                    throw failure("Unescaped control character in string");
                }
                result.append(current);
            }
        }
        throw failure("Unterminated string");
    }

    /// Parses one escape sequence after its leading backslash.
    ///
    /// @return the decoded character
    private char parseEscape() {
        if (offset == source.length()) {
            throw failure("Unterminated escape sequence");
        }
        return switch (source.charAt(offset++)) {
            case '"' -> '"';
            case '\\' -> '\\';
            case '/' -> '/';
            case 'b' -> '\b';
            case 'f' -> '\f';
            case 'n' -> '\n';
            case 'r' -> '\r';
            case 't' -> '\t';
            case 'u' -> parseUnicodeEscape();
            default -> throw failure("Invalid escape sequence");
        };
    }

    /// Parses four hexadecimal digits in a Unicode escape.
    ///
    /// @return the decoded UTF-16 code unit
    private char parseUnicodeEscape() {
        if (offset + 4 > source.length()) {
            throw failure("Incomplete Unicode escape");
        }
        int value = 0;
        for (int index = 0; index < 4; index++) {
            int digit = Character.digit(source.charAt(offset++), 16);
            if (digit < 0) {
                throw failure("Invalid Unicode escape");
            }
            value = (value << 4) | digit;
        }
        return (char) value;
    }

    /// Parses a JSON number beginning at the current offset.
    ///
    /// @return the parsed numeric value
    private JsonNumber parseNumber() {
        int start = offset;
        consume('-');
        if (consume('0')) {
            if (offset < source.length() && Character.isDigit(source.charAt(offset))) {
                throw failure("A number cannot contain a leading zero");
            }
        } else {
            consumeDigits(true);
        }

        boolean integral = true;
        if (consume('.')) {
            integral = false;
            consumeDigits(true);
        }
        if (offset < source.length() && (source.charAt(offset) == 'e' || source.charAt(offset) == 'E')) {
            integral = false;
            offset++;
            if (offset < source.length() && (source.charAt(offset) == '+' || source.charAt(offset) == '-')) {
                offset++;
            }
            consumeDigits(true);
        }

        if (start == offset) {
            throw failure("Expected a JSON value");
        }
        String token = source.substring(start, offset);
        try {
            Number value;
            if (integral) {
                value = Long.valueOf(token);
            } else {
                value = Double.valueOf(token);
            }
            if (value instanceof Double floating && !Double.isFinite(floating)) {
                throw failure("A JSON number must be finite");
            }
            return new JsonNumber(value, integral);
        } catch (NumberFormatException exception) {
            throw failure("Invalid number '" + token + "'");
        }
    }

    /// Consumes a consecutive run of decimal digits.
    ///
    /// @param required whether at least one digit must be present
    private void consumeDigits(boolean required) {
        int start = offset;
        while (offset < source.length() && Character.isDigit(source.charAt(offset))) {
            offset++;
        }
        if (required && start == offset) {
            throw failure("Expected a decimal digit");
        }
    }

    /// Parses a fixed literal beginning at the current offset.
    ///
    /// @param token the literal source text
    /// @param value the value represented by the literal
    /// @return the supplied literal value
    private JsonValue parseLiteral(String token, JsonValue value) {
        if (!source.startsWith(token, offset)) {
            throw failure("Expected '" + token + "'");
        }
        offset += token.length();
        return value;
    }

    /// Skips JSON whitespace beginning at the current offset.
    private void skipWhitespace() {
        while (offset < source.length()) {
            char current = source.charAt(offset);
            if (current != ' ' && current != '\t' && current != '\n' && current != '\r') {
                return;
            }
            offset++;
        }
    }

    /// Consumes a character when it occurs at the current offset.
    ///
    /// @param expected the candidate character
    /// @return whether the character was consumed
    private boolean consume(char expected) {
        if (offset < source.length() && source.charAt(offset) == expected) {
            offset++;
            return true;
        }
        return false;
    }

    /// Requires and consumes one character.
    ///
    /// @param expected the required character
    private void expect(char expected) {
        if (!consume(expected)) {
            throw failure("Expected '" + expected + "'");
        }
    }

    /// Creates a parse failure with a stable line and column.
    ///
    /// @param message the failure detail
    /// @return the exception to throw
    private GradleException failure(String message) {
        int line = 1;
        int column = 1;
        for (int index = 0; index < Math.min(offset, source.length()); index++) {
            if (source.charAt(index) == '\n') {
                line++;
                column = 1;
            } else {
                column++;
            }
        }
        return new GradleException(sourcePath + ":" + line + ":" + column + ": " + message);
    }

    /// Represents any parsed JSON value.
    @NotNullByDefault
    sealed interface JsonValue permits JsonArray, JsonBoolean, JsonNull, JsonNumber, JsonObject, JsonString {
    }

    /// Represents a JSON object.
    ///
    /// @param members the immutable member map
    @NotNullByDefault
    record JsonObject(@Unmodifiable Map<String, JsonValue> members) implements JsonValue {
        /// Creates an object with a stable immutable member map.
        ///
        /// @param members the object members
        JsonObject {
            members = Map.copyOf(members);
        }
    }

    /// Represents a JSON array.
    ///
    /// @param elements the immutable element list
    @NotNullByDefault
    record JsonArray(@Unmodifiable List<JsonValue> elements) implements JsonValue {
        /// Creates an array with a stable immutable element list.
        ///
        /// @param elements the array elements
        JsonArray {
            elements = List.copyOf(elements);
        }
    }

    /// Represents a JSON string.
    ///
    /// @param value the decoded string value
    @NotNullByDefault
    record JsonString(String value) implements JsonValue {
    }

    /// Represents a JSON number.
    ///
    /// @param value the parsed Java number
    /// @param integral whether the source used integer syntax
    @NotNullByDefault
    record JsonNumber(Number value, boolean integral) implements JsonValue {
    }

    /// Represents a JSON Boolean.
    ///
    /// @param value the Boolean value
    @NotNullByDefault
    record JsonBoolean(boolean value) implements JsonValue {
    }

    /// Represents the JSON null literal.
    @NotNullByDefault
    enum JsonNull implements JsonValue {
        /// The sole null sentinel.
        INSTANCE
    }
}

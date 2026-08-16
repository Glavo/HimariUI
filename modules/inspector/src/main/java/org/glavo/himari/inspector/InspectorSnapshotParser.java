package org.glavo.himari.inspector;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/// Parses a canonical [InspectorSnapshot] JSON document without a live layout tree.
@NotNullByDefault
final class InspectorSnapshotParser {
    /// The document.
    private final String json;

    /// The current index.
    private int index;

    /// Creates one parser.
    ///
    /// @param json the document
    InspectorSnapshotParser(String json) {
        this.json = json;
    }

    /// Parses the complete snapshot.
    ///
    /// @return the snapshot
    InspectorSnapshot parse() {
        expect('{');
        expectKey("schema");
        String schema = string();
        if (!"himari-inspector-v1".equals(schema)) {
            throw new IllegalArgumentException("Unsupported inspector schema: " + schema);
        }
        expect(',');
        expectKey("focusedId");
        @Nullable Long focusedId = nullableLong();
        expect(',');
        expectKey("nodes");
        expect('[');
        ArrayList<InspectorNode> nodes = new ArrayList<>();
        if (peek() != ']') {
            while (true) {
                nodes.add(parseNode());
                if (peek() == ',') {
                    index++;
                    continue;
                }
                break;
            }
        }
        expect(']');
        expect(',');
        expectKey("trace");
        @Nullable String trace = rawValue();
        expect('}');
        skipWhitespace();
        if (index != json.length()) {
            throw new IllegalArgumentException("Trailing inspector document content");
        }
        return new InspectorSnapshot(List.copyOf(nodes), focusedId, trace);
    }

    /// Parses one node object.
    private InspectorNode parseNode() {
        expect('{');
        expectKey("id");
        long id = (long) number();
        expect(',');
        expectKey("name");
        String name = string();
        expect(',');
        expectKey("role");
        String role = string();
        expect(',');
        expectKey("label");
        String label = string();
        expect(',');
        expectKey("x");
        float x = (float) number();
        expect(',');
        expectKey("y");
        float y = (float) number();
        expect(',');
        expectKey("width");
        float width = (float) number();
        expect(',');
        expectKey("height");
        float height = (float) number();
        expect(',');
        expectKey("focused");
        boolean focused = bool();
        expect(',');
        expectKey("liveRegion");
        String liveRegion = string();
        expect(',');
        expectKey("textStart");
        int textStart = (int) number();
        expect(',');
        expectKey("textEnd");
        int textEnd = (int) number();
        expect(',');
        expectKey("caret");
        int caret = (int) number();
        expect('}');
        return new InspectorNode(id, name, role, label, x, y, width, height, focused, liveRegion, textStart, textEnd, caret);
    }

    /// Reads `null` or a JSON value as its raw source text.
    private @Nullable String rawValue() {
        skipWhitespace();
        if (json.startsWith("null", index)) {
            index += 4;
            return null;
        }
        int start = index;
        if (peek() == '{') {
            int depth = 0;
            while (index < json.length()) {
                char character = json.charAt(index++);
                if (character == '{') {
                    depth++;
                } else if (character == '}') {
                    depth--;
                    if (depth == 0) {
                        return json.substring(start, index);
                    }
                } else if (character == '"') {
                    index--;
                    string();
                }
            }
            throw new IllegalArgumentException("Unterminated inspector trace object");
        }
        throw new IllegalArgumentException("Inspector trace must be an object or null");
    }

    /// Reads a JSON number or `null`.
    private @Nullable Long nullableLong() {
        skipWhitespace();
        if (json.startsWith("null", index)) {
            index += 4;
            return null;
        }
        return (long) number();
    }

    /// Reads a JSON boolean.
    private boolean bool() {
        skipWhitespace();
        if (json.startsWith("true", index)) {
            index += 4;
            return true;
        }
        if (json.startsWith("false", index)) {
            index += 5;
            return false;
        }
        throw new IllegalArgumentException("Expected a boolean at " + index);
    }

    /// Reads a JSON string.
    private String string() {
        expect('"');
        StringBuilder text = new StringBuilder();
        while (index < json.length()) {
            char character = json.charAt(index++);
            if (character == '"') {
                return text.toString();
            }
            if (character == '\\') {
                if (index >= json.length()) {
                    throw new IllegalArgumentException("Unterminated escape");
                }
                char escaped = json.charAt(index++);
                text.append(switch (escaped) {
                    case '"' -> '"';
                    case '\\' -> '\\';
                    case 'n' -> '\n';
                    case 'r' -> '\r';
                    case 't' -> '\t';
                    default -> throw new IllegalArgumentException("Unsupported escape \\" + escaped);
                });
                continue;
            }
            text.append(character);
        }
        throw new IllegalArgumentException("Unterminated string");
    }

    /// Reads a JSON number.
    private double number() {
        skipWhitespace();
        int start = index;
        if (index < json.length() && json.charAt(index) == '-') {
            index++;
        }
        while (index < json.length()) {
            char character = json.charAt(index);
            if ((character >= '0' && character <= '9') || character == '.') {
                index++;
                continue;
            }
            break;
        }
        if (start == index) {
            throw new IllegalArgumentException("Expected a number at " + start);
        }
        return Double.parseDouble(json.substring(start, index));
    }

    /// Expects a quoted object key.
    private void expectKey(String key) {
        if (!key.equals(string())) {
            throw new IllegalArgumentException("Expected key " + key);
        }
        expect(':');
    }

    /// Expects one character after whitespace.
    private void expect(char expected) {
        skipWhitespace();
        if (index >= json.length() || json.charAt(index) != expected) {
            throw new IllegalArgumentException("Expected '" + expected + "' at " + index);
        }
        index++;
    }

    /// Returns the next non-whitespace character.
    private char peek() {
        skipWhitespace();
        if (index >= json.length()) {
            throw new IllegalArgumentException("Unexpected end of inspector document");
        }
        return json.charAt(index);
    }

    /// Advances past ASCII whitespace.
    private void skipWhitespace() {
        while (index < json.length()) {
            char character = json.charAt(index);
            if (character == ' ' || character == '\n' || character == '\r' || character == '\t') {
                index++;
                continue;
            }
            break;
        }
    }
}

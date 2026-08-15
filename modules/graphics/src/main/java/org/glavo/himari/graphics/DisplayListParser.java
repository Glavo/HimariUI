package org.glavo.himari.graphics;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.ArrayList;
import java.util.List;

/// Parses a canonical [SceneEnvelope] JSON document.
@NotNullByDefault
final class DisplayListParser {
    /// The document.
    private final String json;

    /// The current index.
    private int index;

    /// Creates one parser.
    ///
    /// @param json the document
    DisplayListParser(String json) {
        this.json = json;
    }

    /// Parses the complete envelope.
    ///
    /// @return the envelope
    SceneEnvelope parseEnvelope() {
        expect('{');
        expectKey("schemaVersion");
        int schema = (int) number();
        expect(',');
        expectKey("width");
        int width = (int) number();
        expect(',');
        expectKey("height");
        int height = (int) number();
        expect(',');
        expectKey("ops");
        expect('[');
        ArrayList<DisplayListOp> ops = new ArrayList<>();
        if (peek() != ']') {
            while (true) {
                ops.add(parseOp());
                if (peek() == ',') {
                    index++;
                    continue;
                }
                break;
            }
        }
        expect(']');
        expect('}');
        skipWhitespace();
        if (index != json.length()) {
            throw new IllegalArgumentException("Trailing scene document content");
        }
        return new SceneEnvelope(schema, width, height, new DisplayList(List.copyOf(ops)));
    }

    /// Parses one command object.
    ///
    /// @return the command
    private DisplayListOp parseOp() {
        expect('{');
        expectKey("op");
        String op = string();
        DisplayListOp result = switch (op) {
            case "fillRect" -> {
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
                expectKey("color");
                yield new DisplayListOp.FillRect(x, y, width, height, parseColor());
            }
            case "fillPath" -> {
                expect(',');
                expectKey("path");
                Path path = parsePath();
                expect(',');
                expectKey("color");
                yield new DisplayListOp.FillPath(path, parseColor());
            }
            case "drawGlyph" -> {
                expect(',');
                expectKey("x");
                float x = (float) number();
                expect(',');
                expectKey("y");
                float y = (float) number();
                expect(',');
                expectKey("width");
                int width = (int) number();
                expect(',');
                expectKey("height");
                int height = (int) number();
                expect(',');
                expectKey("coverage");
                byte[] coverage = unhex(string());
                expect(',');
                expectKey("color");
                yield new DisplayListOp.DrawGlyph(x, y, width, height, coverage, parseColor());
            }
            default -> throw new IllegalArgumentException("Unknown display-list op: " + op);
        };
        expect('}');
        return result;
    }

    /// Parses one color object.
    ///
    /// @return the color
    private Color parseColor() {
        expect('{');
        expectKey("encoding");
        ColorEncoding encoding = ColorEncoding.valueOf(string());
        expect(',');
        expectKey("r");
        float red = (float) number();
        expect(',');
        expectKey("g");
        float green = (float) number();
        expect(',');
        expectKey("b");
        float blue = (float) number();
        expect(',');
        expectKey("a");
        float alpha = (float) number();
        expect('}');
        return new Color(encoding, red, green, blue, alpha);
    }

    /// Parses one path object.
    ///
    /// @return the path
    private Path parsePath() {
        expect('{');
        expectKey("verbs");
        expect('[');
        ArrayList<PathVerb> verbs = new ArrayList<>();
        if (peek() != ']') {
            while (true) {
                verbs.add(PathVerb.valueOf(string()));
                if (peek() == ',') {
                    index++;
                    continue;
                }
                break;
            }
        }
        expect(']');
        expect(',');
        expectKey("points");
        expect('[');
        ArrayList<Float> points = new ArrayList<>();
        if (peek() != ']') {
            while (true) {
                points.add((float) number());
                if (peek() == ',') {
                    index++;
                    continue;
                }
                break;
            }
        }
        expect(']');
        expect('}');
        float[] packed = new float[points.size()];
        for (int offset = 0; offset < points.size(); offset++) {
            packed[offset] = points.get(offset);
        }
        return new Path(verbs, packed);
    }

    /// Expects a JSON object key.
    ///
    /// @param key the key
    private void expectKey(String key) {
        if (!string().equals(key)) {
            throw new IllegalArgumentException("Expected key " + key);
        }
        expect(':');
    }

    /// Parses a JSON string.
    ///
    /// @return the string
    private String string() {
        skipWhitespace();
        expect('"');
        StringBuilder text = new StringBuilder();
        while (index < json.length()) {
            char character = json.charAt(index++);
            if (character == '"') {
                return text.toString();
            }
            if (character == '\\') {
                throw new IllegalArgumentException("Escapes are not used in canonical scene strings except hex");
            }
            text.append(character);
        }
        throw new IllegalArgumentException("Unterminated string");
    }

    /// Parses a JSON number.
    ///
    /// @return the number
    private double number() {
        skipWhitespace();
        int start = index;
        if (peek() == '-') {
            index++;
        }
        while (index < json.length()) {
            char character = json.charAt(index);
            if ((character >= '0' && character <= '9') || character == '.' || character == 'e' || character == 'E'
                    || character == '+' || character == '-') {
                index++;
            } else {
                break;
            }
        }
        return Double.parseDouble(json.substring(start, index));
    }

    /// Expects one character after optional whitespace.
    ///
    /// @param expected the expected character
    private void expect(char expected) {
        skipWhitespace();
        if (index >= json.length() || json.charAt(index) != expected) {
            throw new IllegalArgumentException("Expected '" + expected + "'");
        }
        index++;
    }

    /// Returns the next non-whitespace character without consuming it.
    ///
    /// @return the character
    private char peek() {
        skipWhitespace();
        if (index >= json.length()) {
            throw new IllegalArgumentException("Unexpected end of scene document");
        }
        return json.charAt(index);
    }

    /// Skips JSON whitespace.
    private void skipWhitespace() {
        while (index < json.length()) {
            char character = json.charAt(index);
            if (character == ' ' || character == '\n' || character == '\r' || character == '\t') {
                index++;
            } else {
                break;
            }
        }
    }

    /// Decodes lowercase hex.
    ///
    /// @param hex the hex text
    /// @return the bytes
    private static byte[] unhex(String hex) {
        if ((hex.length() & 1) != 0) {
            throw new IllegalArgumentException("Hex coverage must have even length");
        }
        byte[] bytes = new byte[hex.length() / 2];
        for (int index = 0; index < bytes.length; index++) {
            bytes[index] = (byte) Integer.parseInt(hex.substring(index * 2, index * 2 + 2), 16);
        }
        return bytes;
    }
}

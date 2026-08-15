package org.glavo.himari.graphics;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.Locale;
import java.util.Objects;

/// Stores one canonical scene snapshot for offline replay.
///
/// @param schemaVersion the positive schema version
/// @param width the positive surface width in pixels
/// @param height the positive surface height in pixels
/// @param displayList the pointer-free display list
@NotNullByDefault
public record SceneEnvelope(int schemaVersion, int width, int height, DisplayList displayList) {
    /// The current scene schema.
    public static final int CURRENT_SCHEMA = 1;

    /// Validates the envelope.
    public SceneEnvelope {
        if (schemaVersion != CURRENT_SCHEMA) {
            throw new IllegalArgumentException("Unsupported scene schema: " + schemaVersion);
        }
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Scene extents must be positive");
        }
        Objects.requireNonNull(displayList, "displayList");
    }

    /// Encodes this envelope as canonical JSON.
    ///
    /// @return the document
    public String toCanonicalJson() {
        StringBuilder json = new StringBuilder();
        json.append("{\"schemaVersion\":").append(schemaVersion)
                .append(",\"width\":").append(width)
                .append(",\"height\":").append(height)
                .append(",\"ops\":[");
        for (int index = 0; index < displayList.ops().size(); index++) {
            if (index > 0) {
                json.append(',');
            }
            appendOp(json, displayList.ops().get(index));
        }
        json.append("]}");
        return json.toString();
    }

    /// Parses a canonical scene document.
    ///
    /// @param json the document
    /// @return the envelope
    public static SceneEnvelope parse(String json) {
        Objects.requireNonNull(json, "json");
        DisplayListParser parser = new DisplayListParser(json);
        return parser.parseEnvelope();
    }

    /// Appends one command.
    ///
    /// @param json the destination
    /// @param op the command
    private static void appendOp(StringBuilder json, DisplayListOp op) {
        switch (op) {
            case DisplayListOp.FillRect rect -> json.append("{\"op\":\"fillRect\",\"x\":")
                    .append(format(rect.x()))
                    .append(",\"y\":").append(format(rect.y()))
                    .append(",\"width\":").append(format(rect.width()))
                    .append(",\"height\":").append(format(rect.height()))
                    .append(",\"color\":").append(colorJson(rect.color()))
                    .append('}');
            case DisplayListOp.FillPath path -> json.append("{\"op\":\"fillPath\",\"path\":")
                    .append(pathJson(path.path()))
                    .append(",\"color\":").append(colorJson(path.color()))
                    .append('}');
            case DisplayListOp.DrawGlyph glyph -> json.append("{\"op\":\"drawGlyph\",\"x\":")
                    .append(format(glyph.x()))
                    .append(",\"y\":").append(format(glyph.y()))
                    .append(",\"width\":").append(glyph.width())
                    .append(",\"height\":").append(glyph.height())
                    .append(",\"coverage\":\"").append(hex(glyph.coverage()))
                    .append("\",\"color\":").append(colorJson(glyph.color()))
                    .append('}');
        }
    }

    /// Encodes a color object.
    ///
    /// @param color the color
    /// @return the JSON object
    private static String colorJson(Color color) {
        return "{\"encoding\":\"" + color.encoding().name()
                + "\",\"r\":" + format(color.red())
                + ",\"g\":" + format(color.green())
                + ",\"b\":" + format(color.blue())
                + ",\"a\":" + format(color.alpha())
                + '}';
    }

    /// Encodes a path object.
    ///
    /// @param path the path
    /// @return the JSON object
    private static String pathJson(Path path) {
        StringBuilder json = new StringBuilder("{\"verbs\":[");
        for (int index = 0; index < path.verbs().size(); index++) {
            if (index > 0) {
                json.append(',');
            }
            json.append('"').append(path.verbs().get(index).name()).append('"');
        }
        json.append("],\"points\":[");
        for (int index = 0; index < path.points().length; index++) {
            if (index > 0) {
                json.append(',');
            }
            json.append(format(path.points()[index]));
        }
        json.append("]}");
        return json.toString();
    }

    /// Formats a float without scientific notation.
    ///
    /// @param value the value
    /// @return the decimal text
    static String format(float value) {
        return String.format(Locale.ROOT, "%.6f", value);
    }

    /// Encodes bytes as lowercase hex.
    ///
    /// @param bytes the bytes
    /// @return the hex text
    static String hex(byte[] bytes) {
        StringBuilder text = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            text.append(Character.forDigit((value >>> 4) & 0xF, 16));
            text.append(Character.forDigit(value & 0xF, 16));
        }
        return text.toString();
    }
}

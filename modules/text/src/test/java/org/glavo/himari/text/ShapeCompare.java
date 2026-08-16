package org.glavo.himari.text;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/// Compares default-shaper output to HarfBuzz-style glyph records.
@NotNullByDefault
final class ShapeCompare {
    /// Maximum allowed advance delta in font units.
    static final int ADVANCE_TOLERANCE = 1;

    /// Prevents instantiation.
    private ShapeCompare() {
    }

    /// Writes shaped glyphs as a JSON array.
    ///
    /// @param glyphs the glyphs
    /// @return the JSON
    static String toJson(List<ShapedGlyph> glyphs) {
        Objects.requireNonNull(glyphs, "glyphs");
        StringBuilder json = new StringBuilder();
        json.append('[');
        for (int index = 0; index < glyphs.size(); index++) {
            if (index > 0) {
                json.append(',');
            }
            ShapedGlyph glyph = glyphs.get(index);
            json.append("{\"id\":")
                    .append(glyph.glyphId())
                    .append(",\"cluster\":")
                    .append(glyph.cluster())
                    .append(",\"xAdvance\":")
                    .append(glyph.xAdvance())
                    .append(",\"yAdvance\":0,\"xOffset\":0,\"yOffset\":0}");
        }
        json.append(']');
        return json.toString();
    }

    /// Compares glyph id, cluster, and advance sequences.
    ///
    /// @param left the first sequence
    /// @param right the second sequence
    /// @return `null` when they match, otherwise a difference description
    static @Nullable String difference(List<ShapedGlyph> left, List<ShapedGlyph> right) {
        if (left.size() != right.size()) {
            return "glyph count %d != %d".formatted(left.size(), right.size());
        }
        for (int index = 0; index < left.size(); index++) {
            ShapedGlyph a = left.get(index);
            ShapedGlyph b = right.get(index);
            if (a.glyphId() != b.glyphId()) {
                return "glyph %d id %d != %d".formatted(index, a.glyphId(), b.glyphId());
            }
            if (a.cluster() != b.cluster()) {
                return "glyph %d cluster %d != %d".formatted(index, a.cluster(), b.cluster());
            }
            if (Math.abs(a.xAdvance() - b.xAdvance()) > ADVANCE_TOLERANCE) {
                return "glyph %d advance %d != %d".formatted(index, a.xAdvance(), b.xAdvance());
            }
        }
        return null;
    }

    /// Parses a HarfBuzz-style glyph JSON array.
    ///
    /// @param json the JSON
    /// @return the glyphs
    static List<ShapedGlyph> parse(String json) {
        Objects.requireNonNull(json, "json");
        java.util.ArrayList<ShapedGlyph> glyphs = new java.util.ArrayList<>();
        int cursor = 0;
        while (true) {
            int idIndex = indexOfKey(json, "id", cursor);
            int gidIndex = indexOfKey(json, "gid", cursor);
            int start = minIndex(idIndex, gidIndex);
            if (start < 0) {
                break;
            }
            int glyphId = (int) number(json, start == idIndex ? "id" : "gid", start);
            int cluster = (int) number(json, "cluster", start);
            int advance = (int) number(json, "xAdvance", start);
            if (json.indexOf("\"xAdvance\"", start) < 0) {
                advance = (int) number(json, "ax", start);
            }
            glyphs.add(new ShapedGlyph(0, glyphId, Math.max(0, cluster), Math.max(0, advance)));
            cursor = start + 1;
        }
        return List.copyOf(glyphs);
    }

    /// Finds `"key"` at or after `from`.
    private static int indexOfKey(String json, String key, int from) {
        return json.toLowerCase(Locale.ROOT).indexOf('"' + key.toLowerCase(Locale.ROOT) + '"', from);
    }

    /// Returns the lesser nonnegative index.
    private static int minIndex(int left, int right) {
        if (left < 0) {
            return right;
        }
        if (right < 0) {
            return left;
        }
        return Math.min(left, right);
    }

    /// Reads the next JSON number for `key` at or after `from`.
    private static float number(String json, String key, int from) {
        int keyIndex = indexOfKey(json, key, from);
        if (keyIndex < 0) {
            return 0.0f;
        }
        int colon = json.indexOf(':', keyIndex);
        if (colon < 0) {
            return 0.0f;
        }
        int start = colon + 1;
        while (start < json.length() && json.charAt(start) <= ' ') {
            start++;
        }
        int end = start;
        while (end < json.length()) {
            char ch = json.charAt(end);
            if ((ch >= '0' && ch <= '9') || ch == '-' || ch == '+' || ch == '.' || ch == 'e' || ch == 'E') {
                end++;
            } else {
                break;
            }
        }
        if (end == start) {
            return 0.0f;
        }
        return Float.parseFloat(json.substring(start, end));
    }
}

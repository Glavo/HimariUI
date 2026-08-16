package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/// Regularizes and compares outline command lists the way fauntlet compares FreeType to Skrifa.
///
/// Close commands become an explicit line when the current point is not the contour start.
/// Degenerate lines that end at the current point are dropped. Unscaled comparison may truncate
/// coordinates to integers to match `FT_LOAD_NO_SCALE` implicit-midpoint truncation.
@NotNullByDefault
final class OutlineCompare {
    /// Default coordinate tolerance in font units after regularization.
    static final float DEFAULT_TOLERANCE = 0.5f;

    /// Prevents instantiation.
    private OutlineCompare() {
    }

    /// Regularizes `commands` for comparison.
    ///
    /// @param commands the recorded commands
    /// @param scaled whether coordinates stay fractional
    /// @return the regularized commands
    static List<OutlineOp> regularize(List<OutlineOp> commands, boolean scaled) {
        Objects.requireNonNull(commands, "commands");
        ArrayList<OutlineOp> regularized = new ArrayList<>(commands.size());
        @Nullable OutlineOp pendingMove = null;
        float startX = 0.0f;
        float startY = 0.0f;
        float lastX = 0.0f;
        float lastY = 0.0f;
        boolean havePoint = false;
        for (OutlineOp command : commands) {
            switch (command.verb()) {
                case MOVE -> {
                    float x = coord(command.x0(), scaled);
                    float y = coord(command.y0(), scaled);
                    pendingMove = OutlineOp.move(x, y);
                    startX = x;
                    startY = y;
                    lastX = x;
                    lastY = y;
                    havePoint = true;
                }
                case LINE -> {
                    float x = coord(command.x0(), scaled);
                    float y = coord(command.y0(), scaled);
                    if (!havePoint || x != lastX || y != lastY) {
                        flush(regularized, pendingMove);
                        pendingMove = null;
                        regularized.add(OutlineOp.line(x, y));
                        lastX = x;
                        lastY = y;
                        havePoint = true;
                    }
                }
                case QUAD -> {
                    float cx = coord(command.x0(), scaled);
                    float cy = coord(command.y0(), scaled);
                    float x = coord(command.x1(), scaled);
                    float y = coord(command.y1(), scaled);
                    flush(regularized, pendingMove);
                    pendingMove = null;
                    regularized.add(OutlineOp.quad(cx, cy, x, y));
                    lastX = x;
                    lastY = y;
                    havePoint = true;
                }
                case CUBIC -> {
                    float c1x = coord(command.x0(), scaled);
                    float c1y = coord(command.y0(), scaled);
                    float c2x = coord(command.x1(), scaled);
                    float c2y = coord(command.y1(), scaled);
                    float x = coord(command.x2(), scaled);
                    float y = coord(command.y2(), scaled);
                    flush(regularized, pendingMove);
                    pendingMove = null;
                    regularized.add(OutlineOp.cubic(c1x, c1y, c2x, c2y, x, y));
                    lastX = x;
                    lastY = y;
                    havePoint = true;
                }
                case CLOSE -> {
                    if (havePoint && (lastX != startX || lastY != startY)) {
                        flush(regularized, pendingMove);
                        pendingMove = null;
                        regularized.add(OutlineOp.line(startX, startY));
                        lastX = startX;
                        lastY = startY;
                    }
                }
            }
        }
        return List.copyOf(regularized);
    }

    /// Compares two outline recordings after regularization.
    ///
    /// @param left the first recording
    /// @param right the second recording
    /// @param scaled whether coordinates stay fractional
    /// @param tolerance the maximum absolute delta per coordinate
    /// @return `null` when they match, otherwise a difference description
    static @Nullable String difference(
            List<OutlineOp> left,
            List<OutlineOp> right,
            boolean scaled,
            float tolerance
    ) {
        List<OutlineOp> first = regularize(left, scaled);
        List<OutlineOp> second = regularize(right, scaled);
        if (first.size() != second.size()) {
            return "command count %d != %d".formatted(first.size(), second.size());
        }
        for (int index = 0; index < first.size(); index++) {
            OutlineOp a = first.get(index);
            OutlineOp b = second.get(index);
            if (a.verb() != b.verb()) {
                return "command %d verb %s != %s".formatted(index, a.verb(), b.verb());
            }
            if (delta(a.x0(), b.x0()) > tolerance
                    || delta(a.y0(), b.y0()) > tolerance
                    || delta(a.x1(), b.x1()) > tolerance
                    || delta(a.y1(), b.y1()) > tolerance) {
                return "command %d coordinates %s != %s".formatted(index, a, b);
            }
        }
        return null;
    }

    /// Parses a compact command JSON array produced by the FreeType oracle.
    ///
    /// @param json the JSON object or array text
    /// @return the commands
    static List<OutlineOp> parseCommands(String json) {
        Objects.requireNonNull(json, "json");
        ArrayList<OutlineOp> commands = new ArrayList<>();
        int cursor = 0;
        while (true) {
            int opIndex = json.indexOf("\"op\"", cursor);
            if (opIndex < 0) {
                break;
            }
            int colon = json.indexOf(':', opIndex);
            int quote = json.indexOf('"', colon + 1);
            int endQuote = json.indexOf('"', quote + 1);
            if (colon < 0 || quote < 0 || endQuote < 0) {
                break;
            }
            String op = json.substring(quote + 1, endQuote).toLowerCase(Locale.ROOT);
            cursor = endQuote + 1;
            switch (op) {
                case "move" -> commands.add(OutlineOp.move(number(json, "x", cursor), number(json, "y", cursor)));
                case "line" -> commands.add(OutlineOp.line(number(json, "x", cursor), number(json, "y", cursor)));
                case "quad" -> commands.add(OutlineOp.quad(
                        number(json, "cx", cursor),
                        number(json, "cy", cursor),
                        number(json, "x", cursor),
                        number(json, "y", cursor)
                ));
                case "close" -> commands.add(OutlineOp.close());
                default -> {
                }
            }
        }
        return List.copyOf(commands);
    }

    /// Writes commands as a JSON array.
    ///
    /// @param commands the commands
    /// @return the JSON
    static String toJson(List<OutlineOp> commands) {
        StringBuilder json = new StringBuilder();
        json.append('[');
        for (int index = 0; index < commands.size(); index++) {
            if (index > 0) {
                json.append(',');
            }
            OutlineOp command = commands.get(index);
            switch (command.verb()) {
                case MOVE -> json.append("{\"op\":\"move\",\"x\":")
                        .append(command.x0())
                        .append(",\"y\":")
                        .append(command.y0())
                        .append('}');
                case LINE -> json.append("{\"op\":\"line\",\"x\":")
                        .append(command.x0())
                        .append(",\"y\":")
                        .append(command.y0())
                        .append('}');
                case QUAD -> json.append("{\"op\":\"quad\",\"cx\":")
                        .append(command.x0())
                        .append(",\"cy\":")
                        .append(command.y0())
                        .append(",\"x\":")
                        .append(command.x1())
                        .append(",\"y\":")
                        .append(command.y1())
                        .append('}');
                case CLOSE -> json.append("{\"op\":\"close\"}");
            }
        }
        json.append(']');
        return json.toString();
    }

    /// Flushes a pending move.
    private static void flush(List<OutlineOp> commands, @Nullable OutlineOp pendingMove) {
        if (pendingMove != null) {
            commands.add(pendingMove);
        }
    }

    /// Truncates unscaled coordinates.
    private static float coord(float value, boolean scaled) {
        return scaled ? value : (float) (int) value;
    }

    /// Returns the absolute delta.
    private static float delta(float left, float right) {
        return Math.abs(left - right);
    }

    /// Reads the next JSON number for `key` at or after `from`.
    private static float number(String json, String key, int from) {
        int keyIndex = json.indexOf('"' + key + '"', from);
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

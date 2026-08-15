package org.glavo.himari.runtime.trace;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/// Collects a deterministic, pointer-free runtime trace.
///
/// Records are appended in call order. Canonical JSON uses a fixed field order and escaped strings
/// so two traces with the same events compare equal as text.
@NotNullByDefault
public final class RuntimeTrace {
    /// Appended records in call order.
    private final ArrayList<TraceEvent> events = new ArrayList<>();

    /// The next sequence identity.
    private long nextSequence;

    /// Creates an empty trace.
    public RuntimeTrace() {
    }

    /// Appends one record using the next sequence identity.
    ///
    /// @param timestampNanos the nonnegative sampled clock
    /// @param kind the record kind
    /// @param ownerPath the deterministic owner path
    /// @param detail the pointer-free payload
    /// @return the appended record
    public TraceEvent record(long timestampNanos, TraceEventKind kind, String ownerPath, String detail) {
        TraceEvent event = new TraceEvent(nextSequence, timestampNanos, kind, ownerPath, detail);
        events.add(event);
        nextSequence = Math.incrementExact(nextSequence);
        return event;
    }

    /// Appends a fully specified record and advances the sequence past its identity.
    ///
    /// @param event the record
    public void append(TraceEvent event) {
        Objects.requireNonNull(event, "event");
        if (event.sequence() < nextSequence) {
            throw new IllegalArgumentException("Trace event sequence must be monotonic");
        }
        events.add(event);
        nextSequence = Math.incrementExact(event.sequence());
    }

    /// Returns the committed records in sequence order.
    ///
    /// @return the immutable snapshot
    public @Unmodifiable List<TraceEvent> events() {
        return List.copyOf(events);
    }

    /// Encodes this trace as canonical JSON.
    ///
    /// @return the deterministic JSON document
    public String toCanonicalJson() {
        StringBuilder json = new StringBuilder();
        json.append("{\"schema\":\"himari-runtime-trace-v1\",\"events\":[");
        for (int index = 0; index < events.size(); index++) {
            if (index > 0) {
                json.append(',');
            }
            TraceEvent event = events.get(index);
            json.append("{\"sequence\":").append(event.sequence())
                    .append(",\"timestampNanos\":").append(event.timestampNanos())
                    .append(",\"kind\":\"").append(event.kind().name())
                    .append("\",\"ownerPath\":").append(quote(event.ownerPath()))
                    .append(",\"detail\":").append(quote(event.detail()))
                    .append('}');
        }
        json.append("]}");
        return json.toString();
    }

    /// Parses a canonical JSON document produced by [#toCanonicalJson()].
    ///
    /// @param json the document
    /// @return the reconstructed trace
    public static RuntimeTrace parse(String json) {
        Objects.requireNonNull(json, "json");
        if (!json.startsWith("{\"schema\":\"himari-runtime-trace-v1\",\"events\":[")
                || !json.endsWith("]}")) {
            throw new IllegalArgumentException("Unsupported runtime trace document");
        }
        RuntimeTrace trace = new RuntimeTrace();
        String body = json.substring("{\"schema\":\"himari-runtime-trace-v1\",\"events\":[".length(), json.length() - 2);
        if (body.isEmpty()) {
            return trace;
        }
        int cursor = 0;
        while (cursor < body.length()) {
            if (body.charAt(cursor) != '{') {
                throw new IllegalArgumentException("Malformed runtime trace event");
            }
            int end = body.indexOf('}', cursor);
            if (end < 0) {
                throw new IllegalArgumentException("Unterminated runtime trace event");
            }
            trace.append(parseEvent(body.substring(cursor, end + 1)));
            cursor = end + 1;
            if (cursor < body.length()) {
                if (body.charAt(cursor) != ',') {
                    throw new IllegalArgumentException("Malformed runtime trace event list");
                }
                cursor++;
            }
        }
        return trace;
    }

    /// Parses one event object.
    ///
    /// @param object the object text
    /// @return the event
    private static TraceEvent parseEvent(String object) {
        return new TraceEvent(
                Long.parseLong(field(object, "sequence")),
                Long.parseLong(field(object, "timestampNanos")),
                TraceEventKind.valueOf(unquote(field(object, "kind"))),
                unquote(field(object, "ownerPath")),
                unquote(field(object, "detail"))
        );
    }

    /// Returns the raw field text after `\"name\":`.
    ///
    /// @param object the object text
    /// @param name the field name
    /// @return the raw value text
    private static String field(String object, String name) {
        String needle = "\"" + name + "\":";
        int start = object.indexOf(needle);
        if (start < 0) {
            throw new IllegalArgumentException("Missing trace field: " + name);
        }
        start += needle.length();
        if (start < object.length() && object.charAt(start) == '"') {
            int end = start + 1;
            while (end < object.length()) {
                char character = object.charAt(end);
                if (character == '\\') {
                    end += 2;
                    continue;
                }
                if (character == '"') {
                    return object.substring(start, end + 1);
                }
                end++;
            }
            throw new IllegalArgumentException("Unterminated trace string: " + name);
        }
        int end = start;
        while (end < object.length() && object.charAt(end) != ',' && object.charAt(end) != '}') {
            end++;
        }
        return object.substring(start, end);
    }

    /// Quotes a JSON string.
    ///
    /// @param value the raw text
    /// @return the quoted string
    private static String quote(String value) {
        StringBuilder text = new StringBuilder(value.length() + 2);
        text.append('"');
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"' -> text.append("\\\"");
                case '\\' -> text.append("\\\\");
                case '\n' -> text.append("\\n");
                case '\r' -> text.append("\\r");
                case '\t' -> text.append("\\t");
                default -> text.append(character);
            }
        }
        text.append('"');
        return text.toString();
    }

    /// Unquotes a JSON string.
    ///
    /// @param quoted the quoted string
    /// @return the raw text
    private static String unquote(String quoted) {
        if (quoted.length() < 2 || quoted.charAt(0) != '"' || quoted.charAt(quoted.length() - 1) != '"') {
            throw new IllegalArgumentException("Expected a JSON string");
        }
        StringBuilder text = new StringBuilder(quoted.length() - 2);
        for (int index = 1; index < quoted.length() - 1; index++) {
            char character = quoted.charAt(index);
            if (character != '\\') {
                text.append(character);
                continue;
            }
            index++;
            if (index >= quoted.length() - 1) {
                throw new IllegalArgumentException("Dangling JSON escape");
            }
            char escaped = quoted.charAt(index);
            text.append(switch (escaped) {
                case '"' -> '"';
                case '\\' -> '\\';
                case 'n' -> '\n';
                case 'r' -> '\r';
                case 't' -> '\t';
                default -> throw new IllegalArgumentException("Unsupported JSON escape");
            });
        }
        return text.toString();
    }
}

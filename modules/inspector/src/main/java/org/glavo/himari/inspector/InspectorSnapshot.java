package org.glavo.himari.inspector;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Objects;

/// Stores one inspector capture of layout, semantics, and an optional runtime trace.
///
/// @param nodes the inspected nodes in document order
/// @param focusedId the focused identity, or `null`
/// @param traceJson the canonical runtime trace, or `null` when no trace was supplied
@NotNullByDefault
public record InspectorSnapshot(
        @Unmodifiable List<InspectorNode> nodes,
        @Nullable Long focusedId,
        @Nullable String traceJson
) {
    /// Validates the snapshot.
    public InspectorSnapshot {
        nodes = List.copyOf(nodes);
    }

    /// Parses a canonical inspector document without a live layout tree or producer objects.
    ///
    /// @param json the document produced by [`#toCanonicalJson()`]
    /// @return the snapshot
    public static InspectorSnapshot parse(String json) {
        Objects.requireNonNull(json, "json");
        String lower = json.toLowerCase();
        if (lower.contains("hwnd")
                || lower.contains("memorysegment")
                || lower.contains("nativehandle")
                || lower.contains("systemfont")
                || lower.contains("hdc")
                || lower.contains("hmonitor")
                || lower.contains("id3d12")
                || lower.contains("vkdevice")
                || lower.contains("hbitmap")
                || lower.contains("hfont")
                || lower.contains("hicon")
                || lower.contains("himc")
                || lower.contains("hcursor")
                || lower.contains("hmenu")
                || lower.contains("hbrush")
                || lower.contains("hrgn")
                || lower.contains("hpalette")
                || lower.contains("hpen")
                || lower.contains("haccel")
                || lower.contains("hinstance")
                || lower.contains("hhook")
                || lower.contains("hkl")
                || lower.contains("hglobal")
                || lower.contains("hmodule")
                || lower.contains("hthread")
                || lower.contains("hprocess")
                || lower.contains("hfile")
                || lower.contains("hkey")
                || lower.contains("hdrop")
                || lower.contains("hdcmem")
                || lower.contains("hdesk")
                || lower.contains("htoken")
                || lower.contains("hmutex")
                || lower.contains("idxgi")) {
            throw new IllegalArgumentException("Inspector snapshot rejects producer-process handles");
        }
        return new InspectorSnapshotParser(json).parse();
    }

    /// Encodes this snapshot as canonical JSON.
    ///
    /// @return the document
    public String toCanonicalJson() {
        StringBuilder json = new StringBuilder();
        json.append("{\"schema\":\"himari-inspector-v1\",\"focusedId\":");
        if (focusedId == null) {
            json.append("null");
        } else {
            json.append(focusedId);
        }
        json.append(",\"nodes\":[");
        for (int index = 0; index < nodes.size(); index++) {
            if (index > 0) {
                json.append(',');
            }
            InspectorNode node = nodes.get(index);
            json.append("{\"id\":").append(node.id())
                    .append(",\"name\":").append(quote(node.name()))
                    .append(",\"role\":").append(quote(node.role()))
                    .append(",\"label\":").append(quote(node.label()))
                    .append(",\"x\":").append(node.x())
                    .append(",\"y\":").append(node.y())
                    .append(",\"width\":").append(node.width())
                    .append(",\"height\":").append(node.height())
                    .append(",\"focused\":").append(node.focused())
                    .append(",\"liveRegion\":").append(quote(node.liveRegion()))
                    .append(",\"textStart\":").append(node.textStart())
                    .append(",\"textEnd\":").append(node.textEnd())
                    .append(",\"caret\":").append(node.caret())
                    .append('}');
        }
        json.append("],\"trace\":");
        if (traceJson == null) {
            json.append("null");
        } else {
            json.append(traceJson);
        }
        json.append('}');
        return json.toString();
    }

    /// Quotes one JSON string.
    ///
    /// @param value the raw string
    /// @return the quoted token
    private static String quote(String value) {
        Objects.requireNonNull(value, "value");
        StringBuilder quoted = new StringBuilder(value.length() + 2);
        quoted.append('"');
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"' -> quoted.append("\\\"");
                case '\\' -> quoted.append("\\\\");
                case '\n' -> quoted.append("\\n");
                case '\r' -> quoted.append("\\r");
                case '\t' -> quoted.append("\\t");
                default -> quoted.append(character);
            }
        }
        quoted.append('"');
        return quoted.toString();
    }
}

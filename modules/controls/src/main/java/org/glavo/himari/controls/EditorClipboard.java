package org.glavo.himari.controls;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Holds one first-stable editor clipboard payload.
///
/// The bag stores Unicode text and an optional HTML flavor. Password editors must not write
/// committed plaintext into this bag.
@NotNullByDefault
public final class EditorClipboard {
    /// Current text flavor, possibly empty.
    private String text = "";

    /// Current HTML flavor, possibly empty.
    private String html = "";

    /// Current RTF flavor, possibly empty.
    private String rtf = "";

    /// Creates an empty clipboard.
    public EditorClipboard() {
    }

    /// Replaces the Unicode text flavor without touching HTML.
    ///
    /// @param text the payload
    public void setText(String text) {
        this.text = Objects.requireNonNull(text, "text");
    }

    /// Replaces the Unicode text flavor and a matching HTML fragment.
    ///
    /// The HTML flavor is a single `<div>` wrapping XML-escaped `text`.
    ///
    /// @param text the payload
    public void setTextAndHtml(String text) {
        this.text = Objects.requireNonNull(text, "text");
        this.html = htmlFragment(text);
        this.rtf = rtfFragment(text);
    }

    /// Replaces the RTF flavor without touching text or HTML.
    ///
    /// @param rtf the payload
    public void setRtf(String rtf) {
        this.rtf = Objects.requireNonNull(rtf, "rtf");
    }

    /// Returns the RTF flavor.
    ///
    /// @return the payload, possibly empty
    public String rtf() {
        return rtf;
    }

    /// Returns the Unicode text flavor.
    ///
    /// @return the payload, possibly empty
    public String text() {
        return text;
    }

    /// Replaces the HTML flavor.
    ///
    /// @param html the payload
    public void setHtml(String html) {
        this.html = Objects.requireNonNull(html, "html");
    }

    /// Returns the HTML flavor.
    ///
    /// @return the payload, possibly empty
    public String html() {
        return html;
    }

    /// Builds a first-stable HTML fragment for `text`.
    ///
    /// @param text the Unicode payload
    /// @return a single escaped `<div>`
    public static String htmlFragment(String text) {
        Objects.requireNonNull(text, "text");
        return "<div>" + escape(text) + "</div>";
    }

    /// Builds a first-stable RTF fragment for `text`.
    ///
    /// @param text the Unicode payload
    /// @return a single ANSI RTF group
    public static String rtfFragment(String text) {
        Objects.requireNonNull(text, "text");
        return "{\\rtf1 " + escapeRtf(text) + "}";
    }

    /// Escapes `\`, `{`, and `}` for an RTF text run.
    private static String escapeRtf(String text) {
        return text.replace("\\", "\\\\").replace("{", "\\{").replace("}", "\\}");
    }

    /// Escapes `&`, `<`, and `>` for an HTML text node.
    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}

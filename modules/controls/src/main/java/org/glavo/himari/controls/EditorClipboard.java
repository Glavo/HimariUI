package org.glavo.himari.controls;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Holds one first-stable editor clipboard payload.
///
/// The bag stores a single Unicode text flavor. Password editors must not write committed
/// plaintext into this bag.
@NotNullByDefault
public final class EditorClipboard {
    /// Current text flavor, possibly empty.
    private String text = "";

    /// Creates an empty clipboard.
    public EditorClipboard() {
    }

    /// Replaces the Unicode text flavor.
    ///
    /// @param text the payload
    public void setText(String text) {
        this.text = Objects.requireNonNull(text, "text");
    }

    /// Returns the Unicode text flavor.
    ///
    /// @return the payload, possibly empty
    public String text() {
        return text;
    }
}

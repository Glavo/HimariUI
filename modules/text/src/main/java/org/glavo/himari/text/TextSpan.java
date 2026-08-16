package org.glavo.himari.text;

import org.glavo.himari.font.SfntFont;
import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// One UTF-16 span that shares a font collection.
///
/// @param text the source characters
/// @param fonts the faces used to shape this span
@NotNullByDefault
public record TextSpan(String text, FontCollection fonts) {
    /// Validates the span.
    public TextSpan {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(fonts, "fonts");
    }

    /// Creates a span that uses a single face.
    ///
    /// @param text the source characters
    /// @param font the face
    public TextSpan(String text, SfntFont font) {
        this(text, new FontCollection(font));
    }
}

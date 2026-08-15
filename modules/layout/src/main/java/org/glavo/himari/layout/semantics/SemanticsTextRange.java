package org.glavo.himari.layout.semantics;

import org.jetbrains.annotations.NotNullByDefault;

/// Publishes a UTF-16 text selection and caret for one editor.
///
/// Offsets are measured in the node's displayed text, including live composition.
///
/// @param start the inclusive selection start
/// @param end the exclusive selection end
/// @param caret the caret offset
@NotNullByDefault
public record SemanticsTextRange(int start, int end, int caret) {
    /// Validates nonnegative ordered offsets.
    public SemanticsTextRange {
        if (start < 0 || end < start || caret < 0) {
            throw new IllegalArgumentException("Text-range offsets must be nonnegative and ordered");
        }
    }
}

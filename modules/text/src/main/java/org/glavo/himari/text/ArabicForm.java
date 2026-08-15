package org.glavo.himari.text;

import org.jetbrains.annotations.NotNullByDefault;

/// Selects one Arabic presentation form after joining analysis.
@NotNullByDefault
public enum ArabicForm {
    /// The character is not an Arabic joining letter.
    NONE,

    /// Isolated form (`isol`).
    ISOLATED,

    /// Initial form (`init`).
    INITIAL,

    /// Medial form (`medi`).
    MEDIAL,

    /// Final form (`fina`).
    FINAL
}

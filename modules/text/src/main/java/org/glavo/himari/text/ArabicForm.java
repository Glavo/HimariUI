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
    FINAL;

    /// Returns the OpenType feature tag for this form.
    ///
    /// @return a big-endian tag, or `0` for [`#NONE`]
    public int featureTag() {
        return switch (this) {
            case NONE -> 0;
            case ISOLATED -> 0x69736F6C;
            case INITIAL -> 0x696E6974;
            case MEDIAL -> 0x6D656469;
            case FINAL -> 0x66696E61;
        };
    }
}

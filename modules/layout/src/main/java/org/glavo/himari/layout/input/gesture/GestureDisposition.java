package org.glavo.himari.layout.input.gesture;

import org.jetbrains.annotations.NotNullByDefault;

/// Records whether one recognizer may still win the current pointer sequence.
@NotNullByDefault
public enum GestureDisposition {
    /// The recognizer has not yet accepted or rejected the sequence.
    POSSIBLE,

    /// The recognizer won the arena.
    ACCEPTED,

    /// The recognizer lost the arena.
    REJECTED,

    /// The sequence was cancelled before a winner could complete.
    CANCELLED
}

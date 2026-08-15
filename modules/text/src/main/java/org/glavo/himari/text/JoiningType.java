package org.glavo.himari.text;

import org.jetbrains.annotations.NotNullByDefault;

/// Unicode Arabic joining class used by [`ArabicJoining`].
@NotNullByDefault
public enum JoiningType {
    /// Does not join.
    NON_JOINING,

    /// Joins only toward the previous logical neighbor (`R`).
    RIGHT,

    /// Joins only toward the next logical neighbor (`L`).
    LEFT,

    /// Joins in both logical directions (`D`).
    DUAL,

    /// Forces a join, such as tatweel or ZWJ (`C`).
    JOIN_CAUSING,

    /// Ignored when looking for joining neighbors (`T`).
    TRANSPARENT
}

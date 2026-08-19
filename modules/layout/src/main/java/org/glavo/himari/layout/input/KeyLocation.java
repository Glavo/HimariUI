package org.glavo.himari.layout.input;

import org.jetbrains.annotations.NotNullByDefault;

/// Distinguishes standard, left, right, and numeric-keypad physical key locations.
@NotNullByDefault
public enum KeyLocation {
    /// The unshifted / non-keypad location, or an unspecified host location.
    STANDARD,

    /// The left-hand member of a modifier pair.
    LEFT,

    /// The right-hand member of a modifier pair.
    RIGHT,

    /// The numeric keypad, including unextended navigation cluster keys.
    NUMPAD
}

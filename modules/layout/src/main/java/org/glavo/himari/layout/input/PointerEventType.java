package org.glavo.himari.layout.input;

import org.jetbrains.annotations.NotNullByDefault;

/// Identifies one pointer event kind.
@NotNullByDefault
public enum PointerEventType {
    /// The pointer pressed a button.
    DOWN,

    /// The pointer released a button.
    UP,

    /// The pointer moved.
    MOVE,

    /// The pointer reported a vertical wheel notch.
    WHEEL,

    /// The pointer reported a horizontal wheel notch.
    WHEEL_HORIZONTAL,

    /// The secondary (right) button pressed.
    SECONDARY_DOWN,

    /// The secondary (right) button released.
    SECONDARY_UP,

    /// The middle (wheel) button pressed.
    MIDDLE_DOWN,

    /// The middle (wheel) button released.
    MIDDLE_UP
}

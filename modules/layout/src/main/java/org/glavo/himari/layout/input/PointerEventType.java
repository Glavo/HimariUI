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
    MOVE
}

package org.glavo.himari.layout.input;

import org.jetbrains.annotations.NotNullByDefault;

/// Identifies one keyboard event kind.
@NotNullByDefault
public enum KeyEventType {
    /// The key was pressed.
    DOWN,

    /// The key was released.
    UP
}

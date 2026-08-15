package org.glavo.himari.layout.input.gesture;

import org.jetbrains.annotations.NotNullByDefault;

/// Identifies the exclusive winner of one pointer sequence.
@NotNullByDefault
public enum GestureKind {
    /// A press and release inside slop and tap timeout.
    TAP,

    /// A move that exceeded the arena slop.
    DRAG,

    /// A stationary hold that reached the long-press timeout.
    LONG_PRESS
}

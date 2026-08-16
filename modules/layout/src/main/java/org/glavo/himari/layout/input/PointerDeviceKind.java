package org.glavo.himari.layout.input;

import org.jetbrains.annotations.NotNullByDefault;

/// Identifies the physical pointer that produced one event.
@NotNullByDefault
public enum PointerDeviceKind {
    /// A mouse or other relative pointer.
    MOUSE,

    /// A touch contact delivered through a pointer or touch message.
    TOUCH,

    /// A stylus or pen.
    PEN
}

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

    /// The pointer entered the window hit-test region.
    ENTER,

    /// The pointer left the window hit-test region.
    LEAVE,

    /// The window lost pointer capture (`WM_POINTERCAPTURECHANGED`).
    CAPTURE_CHANGED,

    /// An inactive window received a pointer activation request (`WM_POINTERACTIVATE`).
    ACTIVATE,

    /// A non-client-area pointer update (`WM_NCPOINTERUPDATE`).
    NON_CLIENT_MOVE,

    /// A non-client-area pointer press (`WM_NCPOINTERDOWN`).
    NON_CLIENT_DOWN,

    /// A non-client-area pointer release (`WM_NCPOINTERUP`).
    NON_CLIENT_UP,

    /// A pointer was routed to this window (`WM_POINTERROUTEDTO`).
    ROUTED_TO,

    /// A pointer was routed away from this window (`WM_POINTERROUTEDAWAY`).
    ROUTED_AWAY,

    /// A routed pointer was released (`WM_POINTERROUTEDRELEASED`).
    ROUTED_RELEASED,

    /// The secondary (right) button pressed.
    SECONDARY_DOWN,

    /// The secondary (right) button released.
    SECONDARY_UP,

    /// The middle (wheel) button pressed.
    MIDDLE_DOWN,

    /// The middle (wheel) button released.
    MIDDLE_UP
}

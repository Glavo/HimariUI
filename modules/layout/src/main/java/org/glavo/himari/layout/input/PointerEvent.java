package org.glavo.himari.layout.input;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Stores one normalized pointer event in root logical coordinates.
///
/// @param type the event kind
/// @param x the horizontal coordinate
/// @param y the vertical coordinate
/// @param device the physical pointer
@NotNullByDefault
public record PointerEvent(PointerEventType type, float x, float y, PointerDeviceKind device) {
    /// Validates the event.
    public PointerEvent {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(device, "device");
        if (!Float.isFinite(x) || !Float.isFinite(y)) {
            throw new IllegalArgumentException("Pointer coordinates must be finite");
        }
    }

    /// Creates a mouse pointer event.
    ///
    /// @param type the event kind
    /// @param x the horizontal coordinate
    /// @param y the vertical coordinate
    public PointerEvent(PointerEventType type, float x, float y) {
        this(type, x, y, PointerDeviceKind.MOUSE);
    }
}

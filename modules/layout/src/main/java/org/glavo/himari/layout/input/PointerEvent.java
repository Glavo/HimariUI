package org.glavo.himari.layout.input;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Stores one normalized pointer event in root logical coordinates.
///
/// @param type the event kind
/// @param x the horizontal coordinate
/// @param y the vertical coordinate
@NotNullByDefault
public record PointerEvent(PointerEventType type, float x, float y) {
    /// Validates the event.
    public PointerEvent {
        Objects.requireNonNull(type, "type");
        if (!Float.isFinite(x) || !Float.isFinite(y)) {
            throw new IllegalArgumentException("Pointer coordinates must be finite");
        }
    }
}

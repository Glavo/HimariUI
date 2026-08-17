package org.glavo.himari.layout.input;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Stores one normalized pointer event in root logical coordinates.
///
/// @param type the event kind
/// @param x the horizontal coordinate
/// @param y the vertical coordinate
/// @param device the physical pointer
/// @param wheelDelta signed wheel notches; `1.0` is one `WHEEL_DELTA` (120)
/// @param pointerId the host pointer identity; `0` is the primary contact
/// @param pressure normalized pen pressure in `[0, 1]`; `0` when the host does not report it
/// @param tiltX pen tilt from the YZ plane in degrees, in `[-90, 90]`
/// @param tiltY pen tilt from the XZ plane in degrees, in `[-90, 90]`
@NotNullByDefault
public record PointerEvent(
        PointerEventType type,
        float x,
        float y,
        PointerDeviceKind device,
        float wheelDelta,
        int pointerId,
        float pressure,
        float tiltX,
        float tiltY
) {
    /// Validates the event.
    public PointerEvent {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(device, "device");
        if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(wheelDelta)
                || !Float.isFinite(pressure) || !Float.isFinite(tiltX) || !Float.isFinite(tiltY)) {
            throw new IllegalArgumentException("Pointer coordinates and pen axes must be finite");
        }
        if (pressure < 0.0f || pressure > 1.0f) {
            throw new IllegalArgumentException("pressure must be in [0, 1]");
        }
        if (tiltX < -90.0f || tiltX > 90.0f || tiltY < -90.0f || tiltY > 90.0f) {
            throw new IllegalArgumentException("tilt must be in [-90, 90] degrees");
        }
    }

    /// Creates a mouse pointer event with no wheel delta.
    ///
    /// @param type the event kind
    /// @param x the horizontal coordinate
    /// @param y the vertical coordinate
    public PointerEvent(PointerEventType type, float x, float y) {
        this(type, x, y, PointerDeviceKind.MOUSE, 0.0f, 0, 0.0f, 0.0f, 0.0f);
    }

    /// Creates a pointer event with no wheel delta.
    ///
    /// @param type the event kind
    /// @param x the horizontal coordinate
    /// @param y the vertical coordinate
    /// @param device the physical pointer
    public PointerEvent(PointerEventType type, float x, float y, PointerDeviceKind device) {
        this(type, x, y, device, 0.0f, 0, 0.0f, 0.0f, 0.0f);
    }

    /// Creates a pointer event on the primary contact.
    ///
    /// @param type the event kind
    /// @param x the horizontal coordinate
    /// @param y the vertical coordinate
    /// @param device the physical pointer
    /// @param wheelDelta signed wheel notches
    public PointerEvent(
            PointerEventType type,
            float x,
            float y,
            PointerDeviceKind device,
            float wheelDelta
    ) {
        this(type, x, y, device, wheelDelta, 0, 0.0f, 0.0f, 0.0f);
    }

    /// Creates a pointer event with a host pointer identity and no pen axes.
    ///
    /// @param type the event kind
    /// @param x the horizontal coordinate
    /// @param y the vertical coordinate
    /// @param device the physical pointer
    /// @param wheelDelta signed wheel notches
    /// @param pointerId the host pointer identity
    public PointerEvent(
            PointerEventType type,
            float x,
            float y,
            PointerDeviceKind device,
            float wheelDelta,
            int pointerId
    ) {
        this(type, x, y, device, wheelDelta, pointerId, 0.0f, 0.0f, 0.0f);
    }
}

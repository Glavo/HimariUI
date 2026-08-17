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
/// @param rotation clockwise barrel rotation in degrees, in `[0, 359]`; `0` when unreported
/// @param timestampMillis monotonic host message time in milliseconds; `0` when unreported
/// @param buttons currently pressed button mask using [`#BUTTON_PRIMARY`] and siblings
/// @param sequenceId host delivery sequence for this window; `0` when unreported
/// @param synthetic whether the event was injected rather than produced by the host
/// @param inverted whether the stylus is inverted / eraser-end
/// @param eraser whether `PEN_FLAG_ERASER` is set
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
        float tiltY,
        float rotation,
        long timestampMillis,
        int buttons,
        int sequenceId,
        boolean synthetic,
        boolean inverted,
        boolean eraser
) {
    /// Primary / left button bit.
    public static final int BUTTON_PRIMARY = 1;

    /// Secondary / right button bit.
    public static final int BUTTON_SECONDARY = 2;

    /// Middle button bit.
    public static final int BUTTON_MIDDLE = 4;

    /// Extra button 1 (`XBUTTON1`) bit.
    public static final int BUTTON_X1 = 8;

    /// Extra button 2 (`XBUTTON2`) bit.
    public static final int BUTTON_X2 = 16;

    /// Validates the event.
    public PointerEvent {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(device, "device");
        if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(wheelDelta)
                || !Float.isFinite(pressure) || !Float.isFinite(tiltX) || !Float.isFinite(tiltY)
                || !Float.isFinite(rotation)) {
            throw new IllegalArgumentException("Pointer coordinates and pen axes must be finite");
        }
        if (pressure < 0.0f || pressure > 1.0f) {
            throw new IllegalArgumentException("pressure must be in [0, 1]");
        }
        if (tiltX < -90.0f || tiltX > 90.0f || tiltY < -90.0f || tiltY > 90.0f) {
            throw new IllegalArgumentException("tilt must be in [-90, 90] degrees");
        }
        if (rotation < 0.0f || rotation > 359.0f) {
            throw new IllegalArgumentException("rotation must be in [0, 359] degrees");
        }
        if (timestampMillis < 0L) {
            throw new IllegalArgumentException("timestampMillis must be non-negative");
        }
        if (buttons < 0) {
            throw new IllegalArgumentException("buttons must be non-negative");
        }
        if (sequenceId < 0) {
            throw new IllegalArgumentException("sequenceId must be non-negative");
        }
    }

    /// Creates a mouse pointer event with no wheel delta.
    ///
    /// @param type the event kind
    /// @param x the horizontal coordinate
    /// @param y the vertical coordinate
    public PointerEvent(PointerEventType type, float x, float y) {
        this(type, x, y, PointerDeviceKind.MOUSE, 0.0f, 0, 0.0f, 0.0f, 0.0f, 0.0f, 0L, 0, 0, true, false, false);
    }

    /// Creates a pointer event with no wheel delta.
    ///
    /// @param type the event kind
    /// @param x the horizontal coordinate
    /// @param y the vertical coordinate
    /// @param device the physical pointer
    public PointerEvent(PointerEventType type, float x, float y, PointerDeviceKind device) {
        this(type, x, y, device, 0.0f, 0, 0.0f, 0.0f, 0.0f, 0.0f, 0L, 0, 0, true, false, false);
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
        this(type, x, y, device, wheelDelta, 0, 0.0f, 0.0f, 0.0f, 0.0f, 0L, 0, 0, true, false, false);
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
        this(type, x, y, device, wheelDelta, pointerId, 0.0f, 0.0f, 0.0f, 0.0f, 0L, 0, 0, true, false, false);
    }

    /// Creates a pointer event with pen axes and no host timestamp.
    ///
    /// @param type the event kind
    /// @param x the horizontal coordinate
    /// @param y the vertical coordinate
    /// @param device the physical pointer
    /// @param wheelDelta signed wheel notches
    /// @param pointerId the host pointer identity
    /// @param pressure normalized pen pressure
    /// @param tiltX pen tilt from the YZ plane
    /// @param tiltY pen tilt from the XZ plane
    /// @param rotation clockwise barrel rotation
    public PointerEvent(
            PointerEventType type,
            float x,
            float y,
            PointerDeviceKind device,
            float wheelDelta,
            int pointerId,
            float pressure,
            float tiltX,
            float tiltY,
            float rotation
    ) {
        this(type, x, y, device, wheelDelta, pointerId, pressure, tiltX, tiltY, rotation, 0L, 0, 0, true, false, false);
    }

    /// Creates a pointer event with host timestamp, buttons, and sequence, and no invert bit.
    ///
    /// @param type the event kind
    /// @param x the horizontal coordinate
    /// @param y the vertical coordinate
    /// @param device the physical pointer
    /// @param wheelDelta signed wheel notches
    /// @param pointerId the host pointer identity
    /// @param pressure normalized pen pressure
    /// @param tiltX pen tilt from the YZ plane
    /// @param tiltY pen tilt from the XZ plane
    /// @param rotation clockwise barrel rotation
    /// @param timestampMillis host message time
    /// @param buttons pressed-button mask
    /// @param sequenceId host delivery sequence
    /// @param synthetic whether the event was injected
    public PointerEvent(
            PointerEventType type,
            float x,
            float y,
            PointerDeviceKind device,
            float wheelDelta,
            int pointerId,
            float pressure,
            float tiltX,
            float tiltY,
            float rotation,
            long timestampMillis,
            int buttons,
            int sequenceId,
            boolean synthetic
    ) {
        this(
                type,
                x,
                y,
                device,
                wheelDelta,
                pointerId,
                pressure,
                tiltX,
                tiltY,
                rotation,
                timestampMillis,
                buttons,
                sequenceId,
                synthetic,
                false,
                false
        );
    }

    /// Creates a pointer event with invert and no eraser bit.
    ///
    /// @param type the event kind
    /// @param x the horizontal coordinate
    /// @param y the vertical coordinate
    /// @param device the physical pointer
    /// @param wheelDelta signed wheel notches
    /// @param pointerId the host pointer identity
    /// @param pressure normalized pen pressure
    /// @param tiltX pen tilt from the YZ plane
    /// @param tiltY pen tilt from the XZ plane
    /// @param rotation clockwise barrel rotation
    /// @param timestampMillis host message time
    /// @param buttons pressed-button mask
    /// @param sequenceId host delivery sequence
    /// @param synthetic whether the event was injected
    /// @param inverted whether the stylus is inverted
    public PointerEvent(
            PointerEventType type,
            float x,
            float y,
            PointerDeviceKind device,
            float wheelDelta,
            int pointerId,
            float pressure,
            float tiltX,
            float tiltY,
            float rotation,
            long timestampMillis,
            int buttons,
            int sequenceId,
            boolean synthetic,
            boolean inverted
    ) {
        this(
                type,
                x,
                y,
                device,
                wheelDelta,
                pointerId,
                pressure,
                tiltX,
                tiltY,
                rotation,
                timestampMillis,
                buttons,
                sequenceId,
                synthetic,
                inverted,
                false
        );
    }
}

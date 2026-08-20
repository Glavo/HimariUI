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
/// @param contactWidth contact ellipse width in logical pixels; `0` when unreported
/// @param contactHeight contact ellipse height in logical pixels; `0` when unreported
/// @param orientation clockwise touch contact angle in degrees, in `[0, 359]`; `0` when unreported
/// @param inRange whether `POINTER_FLAG_INRANGE` is set
/// @param inContact whether `POINTER_FLAG_INCONTACT` is set
/// @param frameId host `POINTER_INFO.frameId`; `0` when unreported
/// @param canceled whether `POINTER_FLAG_CANCELED` is set
/// @param primary whether `POINTER_FLAG_PRIMARY` is set
/// @param firstButton whether `POINTER_FLAG_FIRSTBUTTON` is set
/// @param secondButton whether `POINTER_FLAG_SECONDBUTTON` is set
/// @param thirdButton whether `POINTER_FLAG_THIRDBUTTON` is set
/// @param fourthButton whether `POINTER_FLAG_FOURTHBUTTON` is set
/// @param fifthButton whether `POINTER_FLAG_FIFTHBUTTON` is set
/// @param newPointer whether `POINTER_FLAG_NEW` is set
/// @param confidence whether `POINTER_FLAG_CONFIDENCE` is set
/// @param down whether `POINTER_FLAG_DOWN` is set
/// @param update whether `POINTER_FLAG_UPDATE` is set
/// @param wheel whether `POINTER_FLAG_WHEEL` is set
/// @param horizontalWheel whether `POINTER_FLAG_HWHEEL` is set
/// @param captureChanged whether `POINTER_FLAG_CAPTURECHANGED` is set
/// @param hasTransform whether `POINTER_FLAG_HASTRANSFORM` is set
/// @param up whether `POINTER_FLAG_UP` is set
/// @param historyCount host `POINTER_INFO.historyCount`; `0` when unreported
/// @param keyStates host `POINTER_INFO.dwKeyStates`; `0` when unreported
/// @param buttonChangeType host `POINTER_INFO.ButtonChangeType`; `0` when unreported
/// @param inputData host `POINTER_INFO.InputData`; `0` when unreported
/// @param performanceCount host `POINTER_INFO.PerformanceCount`; `0` when unreported
/// @param rawX host `POINTER_INFO.ptPixelLocationRaw.x`; `0` when unreported
/// @param rawY host `POINTER_INFO.ptPixelLocationRaw.y`; `0` when unreported
/// @param himetricX host `POINTER_INFO.ptHimetricLocation.x`; `0` when unreported
/// @param himetricY host `POINTER_INFO.ptHimetricLocation.y`; `0` when unreported
/// @param himetricRawX host `POINTER_INFO.ptHimetricLocationRaw.x`; `0` when unreported
/// @param himetricRawY host `POINTER_INFO.ptHimetricLocationRaw.y`; `0` when unreported
/// @param pointerTime host `POINTER_INFO.dwTime`; `0` when unreported
/// @param sourceDevice host `POINTER_INFO.sourceDevice` handle address; `0` when unreported
/// @param hwndTarget host `POINTER_INFO.hwndTarget` handle address; `0` when unreported
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
        boolean eraser,
        float contactWidth,
        float contactHeight,
        float orientation,
        boolean inRange,
        boolean inContact,
        int frameId,
        boolean canceled,
        boolean primary,
        boolean firstButton,
        boolean secondButton,
        boolean thirdButton,
        boolean fourthButton,
        boolean fifthButton,
        boolean newPointer,
        boolean confidence,
        boolean down,
        boolean update,
        boolean wheel,
        boolean horizontalWheel,
        boolean captureChanged,
        boolean hasTransform,
        boolean up,
        int historyCount,
        int keyStates,
        int buttonChangeType,
        int inputData,
        long performanceCount,
        int rawX,
        int rawY,
        int himetricX,
        int himetricY,
        int himetricRawX,
        int himetricRawY,
        int pointerTime,
        long sourceDevice,
        long hwndTarget
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
                || !Float.isFinite(rotation) || !Float.isFinite(contactWidth) || !Float.isFinite(contactHeight)
                || !Float.isFinite(orientation)) {
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
        if (contactWidth < 0.0f || contactHeight < 0.0f) {
            throw new IllegalArgumentException("contact size must be nonnegative");
        }
        if (orientation < 0.0f || orientation > 359.0f) {
            throw new IllegalArgumentException("orientation must be in [0, 359] degrees");
        }
        if (frameId < 0) {
            throw new IllegalArgumentException("frameId must be non-negative");
        }
        if (historyCount < 0) {
            throw new IllegalArgumentException("historyCount must be non-negative");
        }
        if (keyStates < 0) {
            throw new IllegalArgumentException("keyStates must be non-negative");
        }
        if (buttonChangeType < 0) {
            throw new IllegalArgumentException("buttonChangeType must be non-negative");
        }
        if (performanceCount < 0L) {
            throw new IllegalArgumentException("performanceCount must be non-negative");
        }
        if (pointerTime < 0) {
            throw new IllegalArgumentException("pointerTime must be non-negative");
        }
    }

    /// Creates a mouse pointer event with no wheel delta.
    ///
    /// @param type the event kind
    /// @param x the horizontal coordinate
    /// @param y the vertical coordinate
    public PointerEvent(PointerEventType type, float x, float y) {
        this(type, x, y, PointerDeviceKind.MOUSE, 0.0f, 0, 0.0f, 0.0f, 0.0f, 0.0f, 0L, 0, 0, true, false, false, 0.0f, 0.0f);
    }

    /// Creates a pointer event with no wheel delta.
    ///
    /// @param type the event kind
    /// @param x the horizontal coordinate
    /// @param y the vertical coordinate
    /// @param device the physical pointer
    public PointerEvent(PointerEventType type, float x, float y, PointerDeviceKind device) {
        this(type, x, y, device, 0.0f, 0, 0.0f, 0.0f, 0.0f, 0.0f, 0L, 0, 0, true, false, false, 0.0f, 0.0f);
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
        this(type, x, y, device, wheelDelta, 0, 0.0f, 0.0f, 0.0f, 0.0f, 0L, 0, 0, true, false, false, 0.0f, 0.0f);
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
        this(type, x, y, device, wheelDelta, pointerId, 0.0f, 0.0f, 0.0f, 0.0f, 0L, 0, 0, true, false, false, 0.0f, 0.0f);
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
        this(type, x, y, device, wheelDelta, pointerId, pressure, tiltX, tiltY, rotation, 0L, 0, 0, true, false, false, 0.0f, 0.0f);
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

    /// Creates a pointer event with invert and eraser, and no contact ellipse.
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
    /// @param eraser whether `PEN_FLAG_ERASER` is set
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
            boolean inverted,
            boolean eraser
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
                eraser,
                0.0f,
                0.0f
        );
    }

    /// Creates a pointer event with a contact ellipse and no touch orientation.
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
    /// @param eraser whether `PEN_FLAG_ERASER` is set
    /// @param contactWidth contact ellipse width
    /// @param contactHeight contact ellipse height
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
            boolean inverted,
            boolean eraser,
            float contactWidth,
            float contactHeight
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
                eraser,
                contactWidth,
                contactHeight,
                0.0f
        );
    }

    /// Creates a pointer event with orientation and no pointer-info flags.
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
    /// @param eraser whether `PEN_FLAG_ERASER` is set
    /// @param contactWidth contact ellipse width
    /// @param contactHeight contact ellipse height
    /// @param orientation clockwise contact angle
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
            boolean inverted,
            boolean eraser,
            float contactWidth,
            float contactHeight,
            float orientation
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
                eraser,
                contactWidth,
                contactHeight,
                orientation,
                false,
                false
        );
    }

    /// Creates a pointer event with hover and contact bits, and no host frame id.
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
    /// @param eraser whether `PEN_FLAG_ERASER` is set
    /// @param contactWidth contact ellipse width
    /// @param contactHeight contact ellipse height
    /// @param orientation clockwise contact angle
    /// @param inRange whether the pointer is in range
    /// @param inContact whether the pointer is in contact
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
            boolean inverted,
            boolean eraser,
            float contactWidth,
            float contactHeight,
            float orientation,
            boolean inRange,
            boolean inContact
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
                eraser,
                contactWidth,
                contactHeight,
                orientation,
                inRange,
                inContact,
                0
        );
    }

    /// Creates a pointer event with a host frame id and no canceled bit.
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
    /// @param eraser whether `PEN_FLAG_ERASER` is set
    /// @param contactWidth contact ellipse width
    /// @param contactHeight contact ellipse height
    /// @param orientation clockwise contact angle
    /// @param inRange whether the pointer is in range
    /// @param inContact whether the pointer is in contact
    /// @param frameId the host frame identity
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
            boolean inverted,
            boolean eraser,
            float contactWidth,
            float contactHeight,
            float orientation,
            boolean inRange,
            boolean inContact,
            int frameId
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
                eraser,
                contactWidth,
                contactHeight,
                orientation,
                inRange,
                inContact,
                frameId,
                false
        );
    }

    /// Creates a pointer event with a canceled bit and no primary-contact bit.
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
    /// @param eraser whether `PEN_FLAG_ERASER` is set
    /// @param contactWidth contact ellipse width
    /// @param contactHeight contact ellipse height
    /// @param orientation clockwise contact angle
    /// @param inRange whether the pointer is in range
    /// @param inContact whether the pointer is in contact
    /// @param frameId the host frame identity
    /// @param canceled whether the contact was canceled
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
            boolean inverted,
            boolean eraser,
            float contactWidth,
            float contactHeight,
            float orientation,
            boolean inRange,
            boolean inContact,
            int frameId,
            boolean canceled
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
                eraser,
                contactWidth,
                contactHeight,
                orientation,
                inRange,
                inContact,
                frameId,
                canceled,
                false
        );
    }

    /// Creates a pointer event with a primary-contact bit and no first-button bit.
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
    /// @param eraser whether `PEN_FLAG_ERASER` is set
    /// @param contactWidth contact ellipse width
    /// @param contactHeight contact ellipse height
    /// @param orientation clockwise contact angle
    /// @param inRange whether the pointer is in range
    /// @param inContact whether the pointer is in contact
    /// @param frameId the host frame identity
    /// @param canceled whether the contact was canceled
    /// @param primary whether this is the primary contact
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
            boolean inverted,
            boolean eraser,
            float contactWidth,
            float contactHeight,
            float orientation,
            boolean inRange,
            boolean inContact,
            int frameId,
            boolean canceled,
            boolean primary
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
                eraser,
                contactWidth,
                contactHeight,
                orientation,
                inRange,
                inContact,
                frameId,
                canceled,
                primary,
                false
        );
    }

    /// Creates a pointer event with a first-button bit and no second-button bit.
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
    /// @param eraser whether `PEN_FLAG_ERASER` is set
    /// @param contactWidth contact ellipse width
    /// @param contactHeight contact ellipse height
    /// @param orientation clockwise contact angle
    /// @param inRange whether the pointer is in range
    /// @param inContact whether the pointer is in contact
    /// @param frameId the host frame identity
    /// @param canceled whether the contact was canceled
    /// @param primary whether this is the primary contact
    /// @param firstButton whether the first button is down
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
            boolean inverted,
            boolean eraser,
            float contactWidth,
            float contactHeight,
            float orientation,
            boolean inRange,
            boolean inContact,
            int frameId,
            boolean canceled,
            boolean primary,
            boolean firstButton
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
                eraser,
                contactWidth,
                contactHeight,
                orientation,
                inRange,
                inContact,
                frameId,
                canceled,
                primary,
                firstButton,
                false
        );
    }

    /// Creates a pointer event with a second-button bit and no third-button bit.
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
    /// @param eraser whether `PEN_FLAG_ERASER` is set
    /// @param contactWidth contact ellipse width
    /// @param contactHeight contact ellipse height
    /// @param orientation clockwise contact angle
    /// @param inRange whether the pointer is in range
    /// @param inContact whether the pointer is in contact
    /// @param frameId the host frame identity
    /// @param canceled whether the contact was canceled
    /// @param primary whether this is the primary contact
    /// @param firstButton whether the first button is down
    /// @param secondButton whether the second button is down
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
            boolean inverted,
            boolean eraser,
            float contactWidth,
            float contactHeight,
            float orientation,
            boolean inRange,
            boolean inContact,
            int frameId,
            boolean canceled,
            boolean primary,
            boolean firstButton,
            boolean secondButton
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
                eraser,
                contactWidth,
                contactHeight,
                orientation,
                inRange,
                inContact,
                frameId,
                canceled,
                primary,
                firstButton,
                secondButton,
                false
        );
    }

    /// Creates a pointer event with a third-button bit and no fourth-button bit.
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
    /// @param eraser whether `PEN_FLAG_ERASER` is set
    /// @param contactWidth contact ellipse width
    /// @param contactHeight contact ellipse height
    /// @param orientation clockwise contact angle
    /// @param inRange whether the pointer is in range
    /// @param inContact whether the pointer is in contact
    /// @param frameId the host frame identity
    /// @param canceled whether the contact was canceled
    /// @param primary whether this is the primary contact
    /// @param firstButton whether the first button is down
    /// @param secondButton whether the second button is down
    /// @param thirdButton whether the third button is down
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
            boolean inverted,
            boolean eraser,
            float contactWidth,
            float contactHeight,
            float orientation,
            boolean inRange,
            boolean inContact,
            int frameId,
            boolean canceled,
            boolean primary,
            boolean firstButton,
            boolean secondButton,
            boolean thirdButton
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
                eraser,
                contactWidth,
                contactHeight,
                orientation,
                inRange,
                inContact,
                frameId,
                canceled,
                primary,
                firstButton,
                secondButton,
                thirdButton,
                false
        );
    }

    /// Creates a pointer event with a fourth-button bit and no fifth-button bit.
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
    /// @param eraser whether `PEN_FLAG_ERASER` is set
    /// @param contactWidth contact ellipse width
    /// @param contactHeight contact ellipse height
    /// @param orientation clockwise contact angle
    /// @param inRange whether the pointer is in range
    /// @param inContact whether the pointer is in contact
    /// @param frameId the host frame identity
    /// @param canceled whether the contact was canceled
    /// @param primary whether this is the primary contact
    /// @param firstButton whether the first button is down
    /// @param secondButton whether the second button is down
    /// @param thirdButton whether the third button is down
    /// @param fourthButton whether the fourth button is down
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
            boolean inverted,
            boolean eraser,
            float contactWidth,
            float contactHeight,
            float orientation,
            boolean inRange,
            boolean inContact,
            int frameId,
            boolean canceled,
            boolean primary,
            boolean firstButton,
            boolean secondButton,
            boolean thirdButton,
            boolean fourthButton
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
                eraser,
                contactWidth,
                contactHeight,
                orientation,
                inRange,
                inContact,
                frameId,
                canceled,
                primary,
                firstButton,
                secondButton,
                thirdButton,
                fourthButton,
                false
        );
    }

    /// Creates a pointer event with a fifth-button bit and no new-pointer bit.
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
    /// @param eraser whether `PEN_FLAG_ERASER` is set
    /// @param contactWidth contact ellipse width
    /// @param contactHeight contact ellipse height
    /// @param orientation clockwise contact angle
    /// @param inRange whether the pointer is in range
    /// @param inContact whether the pointer is in contact
    /// @param frameId the host frame identity
    /// @param canceled whether the contact was canceled
    /// @param primary whether this is the primary contact
    /// @param firstButton whether the first button is down
    /// @param secondButton whether the second button is down
    /// @param thirdButton whether the third button is down
    /// @param fourthButton whether the fourth button is down
    /// @param fifthButton whether the fifth button is down
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
            boolean inverted,
            boolean eraser,
            float contactWidth,
            float contactHeight,
            float orientation,
            boolean inRange,
            boolean inContact,
            int frameId,
            boolean canceled,
            boolean primary,
            boolean firstButton,
            boolean secondButton,
            boolean thirdButton,
            boolean fourthButton,
            boolean fifthButton
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
                eraser,
                contactWidth,
                contactHeight,
                orientation,
                inRange,
                inContact,
                frameId,
                canceled,
                primary,
                firstButton,
                secondButton,
                thirdButton,
                fourthButton,
                fifthButton,
                false
        );
    }

    /// Creates a pointer event with a new-pointer bit and no confidence bit.
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
    /// @param eraser whether `PEN_FLAG_ERASER` is set
    /// @param contactWidth contact ellipse width
    /// @param contactHeight contact ellipse height
    /// @param orientation clockwise contact angle
    /// @param inRange whether the pointer is in range
    /// @param inContact whether the pointer is in contact
    /// @param frameId the host frame identity
    /// @param canceled whether the contact was canceled
    /// @param primary whether this is the primary contact
    /// @param firstButton whether the first button is down
    /// @param secondButton whether the second button is down
    /// @param thirdButton whether the third button is down
    /// @param fourthButton whether the fourth button is down
    /// @param fifthButton whether the fifth button is down
    /// @param newPointer whether this is a newly sighted pointer
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
            boolean inverted,
            boolean eraser,
            float contactWidth,
            float contactHeight,
            float orientation,
            boolean inRange,
            boolean inContact,
            int frameId,
            boolean canceled,
            boolean primary,
            boolean firstButton,
            boolean secondButton,
            boolean thirdButton,
            boolean fourthButton,
            boolean fifthButton,
            boolean newPointer
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
                eraser,
                contactWidth,
                contactHeight,
                orientation,
                inRange,
                inContact,
                frameId,
                canceled,
                primary,
                firstButton,
                secondButton,
                thirdButton,
                fourthButton,
                fifthButton,
                newPointer,
                false
        );
    }

    /// Creates a pointer event with a confidence bit and no remaining `POINTER_INFO` flags.
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
    /// @param eraser whether `PEN_FLAG_ERASER` is set
    /// @param contactWidth contact ellipse width
    /// @param contactHeight contact ellipse height
    /// @param orientation clockwise contact angle
    /// @param inRange whether the pointer is in range
    /// @param inContact whether the pointer is in contact
    /// @param frameId the host frame identity
    /// @param canceled whether the contact was canceled
    /// @param primary whether this is the primary contact
    /// @param firstButton whether the first button is down
    /// @param secondButton whether the second button is down
    /// @param thirdButton whether the third button is down
    /// @param fourthButton whether the fourth button is down
    /// @param fifthButton whether the fifth button is down
    /// @param newPointer whether this is a newly sighted pointer
    /// @param confidence whether the host reports a confident contact
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
            boolean inverted,
            boolean eraser,
            float contactWidth,
            float contactHeight,
            float orientation,
            boolean inRange,
            boolean inContact,
            int frameId,
            boolean canceled,
            boolean primary,
            boolean firstButton,
            boolean secondButton,
            boolean thirdButton,
            boolean fourthButton,
            boolean fifthButton,
            boolean newPointer,
            boolean confidence
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
                eraser,
                contactWidth,
                contactHeight,
                orientation,
                inRange,
                inContact,
                frameId,
                canceled,
                primary,
                firstButton,
                secondButton,
                thirdButton,
                fourthButton,
                fifthButton,
                newPointer,
                confidence,
                false,
                false,
                false,
                false,
                false,
                false
        );
    }

    /// Creates a pointer event with remaining `POINTER_INFO` flags except `POINTER_FLAG_UP`.
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
    /// @param eraser whether `PEN_FLAG_ERASER` is set
    /// @param contactWidth contact ellipse width
    /// @param contactHeight contact ellipse height
    /// @param orientation clockwise contact angle
    /// @param inRange whether the pointer is in range
    /// @param inContact whether the pointer is in contact
    /// @param frameId the host frame identity
    /// @param canceled whether the contact was canceled
    /// @param primary whether this is the primary contact
    /// @param firstButton whether the first button is down
    /// @param secondButton whether the second button is down
    /// @param thirdButton whether the third button is down
    /// @param fourthButton whether the fourth button is down
    /// @param fifthButton whether the fifth button is down
    /// @param newPointer whether this is a newly sighted pointer
    /// @param confidence whether the host reports a confident contact
    /// @param down whether the contact is beginning
    /// @param update whether this is an update
    /// @param wheel whether a vertical wheel tick is present
    /// @param horizontalWheel whether a horizontal wheel tick is present
    /// @param captureChanged whether capture changed
    /// @param hasTransform whether a pointer transform is present
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
            boolean inverted,
            boolean eraser,
            float contactWidth,
            float contactHeight,
            float orientation,
            boolean inRange,
            boolean inContact,
            int frameId,
            boolean canceled,
            boolean primary,
            boolean firstButton,
            boolean secondButton,
            boolean thirdButton,
            boolean fourthButton,
            boolean fifthButton,
            boolean newPointer,
            boolean confidence,
            boolean down,
            boolean update,
            boolean wheel,
            boolean horizontalWheel,
            boolean captureChanged,
            boolean hasTransform
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
                eraser,
                contactWidth,
                contactHeight,
                orientation,
                inRange,
                inContact,
                frameId,
                canceled,
                primary,
                firstButton,
                secondButton,
                thirdButton,
                fourthButton,
                fifthButton,
                newPointer,
                confidence,
                down,
                update,
                wheel,
                horizontalWheel,
                captureChanged,
                hasTransform,
                false
        );
    }

    /// Creates a pointer event with `POINTER_FLAG_UP` and no reported history count.
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
    /// @param eraser whether `PEN_FLAG_ERASER` is set
    /// @param contactWidth contact ellipse width
    /// @param contactHeight contact ellipse height
    /// @param orientation clockwise contact angle
    /// @param inRange whether the pointer is in range
    /// @param inContact whether the pointer is in contact
    /// @param frameId the host frame identity
    /// @param canceled whether the contact was canceled
    /// @param primary whether this is the primary contact
    /// @param firstButton whether the first button is down
    /// @param secondButton whether the second button is down
    /// @param thirdButton whether the third button is down
    /// @param fourthButton whether the fourth button is down
    /// @param fifthButton whether the fifth button is down
    /// @param newPointer whether this is a newly sighted pointer
    /// @param confidence whether the host reports a confident contact
    /// @param down whether the contact is beginning
    /// @param update whether this is an update
    /// @param wheel whether a vertical wheel tick is present
    /// @param horizontalWheel whether a horizontal wheel tick is present
    /// @param captureChanged whether capture changed
    /// @param hasTransform whether a pointer transform is present
    /// @param up whether the contact is ending
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
            boolean inverted,
            boolean eraser,
            float contactWidth,
            float contactHeight,
            float orientation,
            boolean inRange,
            boolean inContact,
            int frameId,
            boolean canceled,
            boolean primary,
            boolean firstButton,
            boolean secondButton,
            boolean thirdButton,
            boolean fourthButton,
            boolean fifthButton,
            boolean newPointer,
            boolean confidence,
            boolean down,
            boolean update,
            boolean wheel,
            boolean horizontalWheel,
            boolean captureChanged,
            boolean hasTransform,
            boolean up
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
                eraser,
                contactWidth,
                contactHeight,
                orientation,
                inRange,
                inContact,
                frameId,
                canceled,
                primary,
                firstButton,
                secondButton,
                thirdButton,
                fourthButton,
                fifthButton,
                newPointer,
                confidence,
                down,
                update,
                wheel,
                horizontalWheel,
                captureChanged,
                hasTransform,
                up,
                0
        );
    }

    /// Creates a pointer event with a reported history count and no key-state or button-change fields.
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
    /// @param eraser whether `PEN_FLAG_ERASER` is set
    /// @param contactWidth contact ellipse width
    /// @param contactHeight contact ellipse height
    /// @param orientation clockwise contact angle
    /// @param inRange whether the pointer is in range
    /// @param inContact whether the pointer is in contact
    /// @param frameId the host frame identity
    /// @param canceled whether the contact was canceled
    /// @param primary whether this is the primary contact
    /// @param firstButton whether the first button is down
    /// @param secondButton whether the second button is down
    /// @param thirdButton whether the third button is down
    /// @param fourthButton whether the fourth button is down
    /// @param fifthButton whether the fifth button is down
    /// @param newPointer whether this is a newly sighted pointer
    /// @param confidence whether the host reports a confident contact
    /// @param down whether the contact is beginning
    /// @param update whether this is an update
    /// @param wheel whether a vertical wheel tick is present
    /// @param horizontalWheel whether a horizontal wheel tick is present
    /// @param captureChanged whether capture changed
    /// @param hasTransform whether a pointer transform is present
    /// @param up whether the contact is ending
    /// @param historyCount host history count
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
            boolean inverted,
            boolean eraser,
            float contactWidth,
            float contactHeight,
            float orientation,
            boolean inRange,
            boolean inContact,
            int frameId,
            boolean canceled,
            boolean primary,
            boolean firstButton,
            boolean secondButton,
            boolean thirdButton,
            boolean fourthButton,
            boolean fifthButton,
            boolean newPointer,
            boolean confidence,
            boolean down,
            boolean update,
            boolean wheel,
            boolean horizontalWheel,
            boolean captureChanged,
            boolean hasTransform,
            boolean up,
            int historyCount
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
                eraser,
                contactWidth,
                contactHeight,
                orientation,
                inRange,
                inContact,
                frameId,
                canceled,
                primary,
                firstButton,
                secondButton,
                thirdButton,
                fourthButton,
                fifthButton,
                newPointer,
                confidence,
                down,
                update,
                wheel,
                horizontalWheel,
                captureChanged,
                hasTransform,
                up,
                historyCount,
                0,
                0
        );
    }

    /// Creates a pointer event with key-state and button-change fields, and no remaining `POINTER_INFO` integers.
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
    /// @param eraser whether `PEN_FLAG_ERASER` is set
    /// @param contactWidth contact ellipse width
    /// @param contactHeight contact ellipse height
    /// @param orientation clockwise contact angle
    /// @param inRange whether the pointer is in range
    /// @param inContact whether the pointer is in contact
    /// @param frameId the host frame identity
    /// @param canceled whether the contact was canceled
    /// @param primary whether this is the primary contact
    /// @param firstButton whether the first button is down
    /// @param secondButton whether the second button is down
    /// @param thirdButton whether the third button is down
    /// @param fourthButton whether the fourth button is down
    /// @param fifthButton whether the fifth button is down
    /// @param newPointer whether this is a newly sighted pointer
    /// @param confidence whether the host reports a confident contact
    /// @param down whether the contact is beginning
    /// @param update whether this is an update
    /// @param wheel whether a vertical wheel tick is present
    /// @param horizontalWheel whether a horizontal wheel tick is present
    /// @param captureChanged whether capture changed
    /// @param hasTransform whether a pointer transform is present
    /// @param up whether the contact is ending
    /// @param historyCount host history count
    /// @param keyStates host modifier bits
    /// @param buttonChangeType host button-change kind
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
            boolean inverted,
            boolean eraser,
            float contactWidth,
            float contactHeight,
            float orientation,
            boolean inRange,
            boolean inContact,
            int frameId,
            boolean canceled,
            boolean primary,
            boolean firstButton,
            boolean secondButton,
            boolean thirdButton,
            boolean fourthButton,
            boolean fifthButton,
            boolean newPointer,
            boolean confidence,
            boolean down,
            boolean update,
            boolean wheel,
            boolean horizontalWheel,
            boolean captureChanged,
            boolean hasTransform,
            boolean up,
            int historyCount,
            int keyStates,
            int buttonChangeType
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
                eraser,
                contactWidth,
                contactHeight,
                orientation,
                inRange,
                inContact,
                frameId,
                canceled,
                primary,
                firstButton,
                secondButton,
                thirdButton,
                fourthButton,
                fifthButton,
                newPointer,
                confidence,
                down,
                update,
                wheel,
                horizontalWheel,
                captureChanged,
                hasTransform,
                up,
                historyCount,
                keyStates,
                buttonChangeType,
                0,
                0L
        );
    }

    /// Creates a pointer event with input-data and performance-count, and no raw or himetric locations.
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
    /// @param eraser whether `PEN_FLAG_ERASER` is set
    /// @param contactWidth contact ellipse width
    /// @param contactHeight contact ellipse height
    /// @param orientation clockwise contact angle
    /// @param inRange whether the pointer is in range
    /// @param inContact whether the pointer is in contact
    /// @param frameId the host frame identity
    /// @param canceled whether the contact was canceled
    /// @param primary whether this is the primary contact
    /// @param firstButton whether the first button is down
    /// @param secondButton whether the second button is down
    /// @param thirdButton whether the third button is down
    /// @param fourthButton whether the fourth button is down
    /// @param fifthButton whether the fifth button is down
    /// @param newPointer whether this is a newly sighted pointer
    /// @param confidence whether the host reports a confident contact
    /// @param down whether the contact is beginning
    /// @param update whether this is an update
    /// @param wheel whether a vertical wheel tick is present
    /// @param horizontalWheel whether a horizontal wheel tick is present
    /// @param captureChanged whether capture changed
    /// @param hasTransform whether a pointer transform is present
    /// @param up whether the contact is ending
    /// @param historyCount host history count
    /// @param keyStates host modifier bits
    /// @param buttonChangeType host button-change kind
    /// @param inputData host extra input data
    /// @param performanceCount host performance counter
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
            boolean inverted,
            boolean eraser,
            float contactWidth,
            float contactHeight,
            float orientation,
            boolean inRange,
            boolean inContact,
            int frameId,
            boolean canceled,
            boolean primary,
            boolean firstButton,
            boolean secondButton,
            boolean thirdButton,
            boolean fourthButton,
            boolean fifthButton,
            boolean newPointer,
            boolean confidence,
            boolean down,
            boolean update,
            boolean wheel,
            boolean horizontalWheel,
            boolean captureChanged,
            boolean hasTransform,
            boolean up,
            int historyCount,
            int keyStates,
            int buttonChangeType,
            int inputData,
            long performanceCount
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
                eraser,
                contactWidth,
                contactHeight,
                orientation,
                inRange,
                inContact,
                frameId,
                canceled,
                primary,
                firstButton,
                secondButton,
                thirdButton,
                fourthButton,
                fifthButton,
                newPointer,
                confidence,
                down,
                update,
                wheel,
                horizontalWheel,
                captureChanged,
                hasTransform,
                up,
                historyCount,
                keyStates,
                buttonChangeType,
                inputData,
                performanceCount,
                0,
                0,
                0,
                0
        );
    }

    /// Creates a pointer event with raw and himetric locations, and no raw himetric locations.
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
    /// @param eraser whether `PEN_FLAG_ERASER` is set
    /// @param contactWidth contact ellipse width
    /// @param contactHeight contact ellipse height
    /// @param orientation clockwise contact angle
    /// @param inRange whether the pointer is in range
    /// @param inContact whether the pointer is in contact
    /// @param frameId the host frame identity
    /// @param canceled whether the contact was canceled
    /// @param primary whether this is the primary contact
    /// @param firstButton whether the first button is down
    /// @param secondButton whether the second button is down
    /// @param thirdButton whether the third button is down
    /// @param fourthButton whether the fourth button is down
    /// @param fifthButton whether the fifth button is down
    /// @param newPointer whether this is a newly sighted pointer
    /// @param confidence whether the host reports a confident contact
    /// @param down whether the contact is beginning
    /// @param update whether this is an update
    /// @param wheel whether a vertical wheel tick is present
    /// @param horizontalWheel whether a horizontal wheel tick is present
    /// @param captureChanged whether capture changed
    /// @param hasTransform whether a pointer transform is present
    /// @param up whether the contact is ending
    /// @param historyCount host history count
    /// @param keyStates host modifier bits
    /// @param buttonChangeType host button-change kind
    /// @param inputData host extra input data
    /// @param performanceCount host performance counter
    /// @param rawX raw pixel X
    /// @param rawY raw pixel Y
    /// @param himetricX himetric X
    /// @param himetricY himetric Y
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
            boolean inverted,
            boolean eraser,
            float contactWidth,
            float contactHeight,
            float orientation,
            boolean inRange,
            boolean inContact,
            int frameId,
            boolean canceled,
            boolean primary,
            boolean firstButton,
            boolean secondButton,
            boolean thirdButton,
            boolean fourthButton,
            boolean fifthButton,
            boolean newPointer,
            boolean confidence,
            boolean down,
            boolean update,
            boolean wheel,
            boolean horizontalWheel,
            boolean captureChanged,
            boolean hasTransform,
            boolean up,
            int historyCount,
            int keyStates,
            int buttonChangeType,
            int inputData,
            long performanceCount,
            int rawX,
            int rawY,
            int himetricX,
            int himetricY
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
                eraser,
                contactWidth,
                contactHeight,
                orientation,
                inRange,
                inContact,
                frameId,
                canceled,
                primary,
                firstButton,
                secondButton,
                thirdButton,
                fourthButton,
                fifthButton,
                newPointer,
                confidence,
                down,
                update,
                wheel,
                horizontalWheel,
                captureChanged,
                hasTransform,
                up,
                historyCount,
                keyStates,
                buttonChangeType,
                inputData,
                performanceCount,
                rawX,
                rawY,
                himetricX,
                himetricY,
                0,
                0
        );
    }

    /// Creates a pointer event with raw himetric locations and no `POINTER_INFO.dwTime`.
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
    /// @param eraser whether `PEN_FLAG_ERASER` is set
    /// @param contactWidth contact ellipse width
    /// @param contactHeight contact ellipse height
    /// @param orientation clockwise contact angle
    /// @param inRange whether the pointer is in range
    /// @param inContact whether the pointer is in contact
    /// @param frameId the host frame identity
    /// @param canceled whether the contact was canceled
    /// @param primary whether this is the primary contact
    /// @param firstButton whether the first button is down
    /// @param secondButton whether the second button is down
    /// @param thirdButton whether the third button is down
    /// @param fourthButton whether the fourth button is down
    /// @param fifthButton whether the fifth button is down
    /// @param newPointer whether this is a newly sighted pointer
    /// @param confidence whether the host reports a confident contact
    /// @param down whether the contact is beginning
    /// @param update whether this is an update
    /// @param wheel whether a vertical wheel tick is present
    /// @param horizontalWheel whether a horizontal wheel tick is present
    /// @param captureChanged whether capture changed
    /// @param hasTransform whether a pointer transform is present
    /// @param up whether the contact is ending
    /// @param historyCount host history count
    /// @param keyStates host modifier bits
    /// @param buttonChangeType host button-change kind
    /// @param inputData host extra input data
    /// @param performanceCount host performance counter
    /// @param rawX raw pixel X
    /// @param rawY raw pixel Y
    /// @param himetricX himetric X
    /// @param himetricY himetric Y
    /// @param himetricRawX raw himetric X
    /// @param himetricRawY raw himetric Y
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
            boolean inverted,
            boolean eraser,
            float contactWidth,
            float contactHeight,
            float orientation,
            boolean inRange,
            boolean inContact,
            int frameId,
            boolean canceled,
            boolean primary,
            boolean firstButton,
            boolean secondButton,
            boolean thirdButton,
            boolean fourthButton,
            boolean fifthButton,
            boolean newPointer,
            boolean confidence,
            boolean down,
            boolean update,
            boolean wheel,
            boolean horizontalWheel,
            boolean captureChanged,
            boolean hasTransform,
            boolean up,
            int historyCount,
            int keyStates,
            int buttonChangeType,
            int inputData,
            long performanceCount,
            int rawX,
            int rawY,
            int himetricX,
            int himetricY,
            int himetricRawX,
            int himetricRawY
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
                eraser,
                contactWidth,
                contactHeight,
                orientation,
                inRange,
                inContact,
                frameId,
                canceled,
                primary,
                firstButton,
                secondButton,
                thirdButton,
                fourthButton,
                fifthButton,
                newPointer,
                confidence,
                down,
                update,
                wheel,
                horizontalWheel,
                captureChanged,
                hasTransform,
                up,
                historyCount,
                keyStates,
                buttonChangeType,
                inputData,
                performanceCount,
                rawX,
                rawY,
                himetricX,
                himetricY,
                himetricRawX,
                himetricRawY,
                0
        );
    }

    /// Creates an event with `POINTER_INFO.dwTime` and no producer handles.
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
    /// @param timestampMillis monotonic host message time
    /// @param buttons currently pressed button mask
    /// @param sequenceId host delivery sequence
    /// @param synthetic whether the event was injected
    /// @param inverted whether the stylus is inverted
    /// @param eraser whether `PEN_FLAG_ERASER` is set
    /// @param contactWidth contact ellipse width
    /// @param contactHeight contact ellipse height
    /// @param orientation clockwise touch contact angle
    /// @param inRange whether `POINTER_FLAG_INRANGE` is set
    /// @param inContact whether `POINTER_FLAG_INCONTACT` is set
    /// @param frameId host `POINTER_INFO.frameId`
    /// @param canceled whether `POINTER_FLAG_CANCELED` is set
    /// @param primary whether `POINTER_FLAG_PRIMARY` is set
    /// @param firstButton whether `POINTER_FLAG_FIRSTBUTTON` is set
    /// @param secondButton whether `POINTER_FLAG_SECONDBUTTON` is set
    /// @param thirdButton whether `POINTER_FLAG_THIRDBUTTON` is set
    /// @param fourthButton whether `POINTER_FLAG_FOURTHBUTTON` is set
    /// @param fifthButton whether `POINTER_FLAG_FIFTHBUTTON` is set
    /// @param newPointer whether `POINTER_FLAG_NEW` is set
    /// @param confidence whether `POINTER_FLAG_CONFIDENCE` is set
    /// @param down whether `POINTER_FLAG_DOWN` is set
    /// @param update whether `POINTER_FLAG_UPDATE` is set
    /// @param wheel whether `POINTER_FLAG_WHEEL` is set
    /// @param horizontalWheel whether `POINTER_FLAG_HWHEEL` is set
    /// @param captureChanged whether `POINTER_FLAG_CAPTURECHANGED` is set
    /// @param hasTransform whether `POINTER_FLAG_HASTRANSFORM` is set
    /// @param up whether `POINTER_FLAG_UP` is set
    /// @param historyCount host `POINTER_INFO.historyCount`
    /// @param keyStates host `POINTER_INFO.dwKeyStates`
    /// @param buttonChangeType host `POINTER_INFO.ButtonChangeType`
    /// @param inputData host `POINTER_INFO.InputData`
    /// @param performanceCount host `POINTER_INFO.PerformanceCount`
    /// @param rawX host `POINTER_INFO.ptPixelLocationRaw.x`
    /// @param rawY host `POINTER_INFO.ptPixelLocationRaw.y`
    /// @param himetricX host `POINTER_INFO.ptHimetricLocation.x`
    /// @param himetricY host `POINTER_INFO.ptHimetricLocation.y`
    /// @param himetricRawX host `POINTER_INFO.ptHimetricLocationRaw.x`
    /// @param himetricRawY host `POINTER_INFO.ptHimetricLocationRaw.y`
    /// @param pointerTime host `POINTER_INFO.dwTime`
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
            boolean inverted,
            boolean eraser,
            float contactWidth,
            float contactHeight,
            float orientation,
            boolean inRange,
            boolean inContact,
            int frameId,
            boolean canceled,
            boolean primary,
            boolean firstButton,
            boolean secondButton,
            boolean thirdButton,
            boolean fourthButton,
            boolean fifthButton,
            boolean newPointer,
            boolean confidence,
            boolean down,
            boolean update,
            boolean wheel,
            boolean horizontalWheel,
            boolean captureChanged,
            boolean hasTransform,
            boolean up,
            int historyCount,
            int keyStates,
            int buttonChangeType,
            int inputData,
            long performanceCount,
            int rawX,
            int rawY,
            int himetricX,
            int himetricY,
            int himetricRawX,
            int himetricRawY,
            int pointerTime
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
                eraser,
                contactWidth,
                contactHeight,
                orientation,
                inRange,
                inContact,
                frameId,
                canceled,
                primary,
                firstButton,
                secondButton,
                thirdButton,
                fourthButton,
                fifthButton,
                newPointer,
                confidence,
                down,
                update,
                wheel,
                horizontalWheel,
                captureChanged,
                hasTransform,
                up,
                historyCount,
                keyStates,
                buttonChangeType,
                inputData,
                performanceCount,
                rawX,
                rawY,
                himetricX,
                himetricY,
                himetricRawX,
                himetricRawY,
                pointerTime,
                0L,
                0L
        );
    }
}

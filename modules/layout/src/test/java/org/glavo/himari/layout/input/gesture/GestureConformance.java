package org.glavo.himari.layout.input.gesture;

import org.glavo.himari.layout.input.PointerEvent;
import org.glavo.himari.layout.input.PointerEventType;
import org.jetbrains.annotations.NotNullByDefault;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/// Writes M9 gesture-arena evidence.
@NotNullByDefault
public final class GestureConformance {
    /// Prevents instantiation.
    private GestureConformance() {
    }

    /// Exercises tap, drag, velocity, and long-press competition.
    ///
    /// @param arguments one output directory
    /// @throws Exception if the profile fails
    public static void main(String[] arguments) throws Exception {
        if (arguments.length != 1) {
            throw new IllegalArgumentException("Expected one output directory");
        }
        GestureArena tap = new GestureArena();
        tap.dispatch(new PointerEvent(PointerEventType.DOWN, 8.0f, 8.0f), 0L);
        tap.dispatch(new PointerEvent(PointerEventType.UP, 8.0f, 8.0f), 40_000_000L);
        if (!tap.tapAccepted()) {
            throw new IllegalStateException("Tap did not win a short stationary press");
        }
        GestureArena drag = new GestureArena();
        drag.dispatch(new PointerEvent(PointerEventType.DOWN, 0.0f, 0.0f), 0L);
        drag.dispatch(new PointerEvent(PointerEventType.MOVE, 0.0f, 16.0f), 16_000_000L);
        drag.dispatch(new PointerEvent(PointerEventType.MOVE, 0.0f, 32.0f), 32_000_000L);
        if (!drag.dragAccepted() || drag.translationY() != 32.0f || drag.lastDeltaY() != 16.0f
                || drag.velocity().y() <= 0.0f) {
            throw new IllegalStateException("Drag did not win or track velocity");
        }
        if (drag.disposition(GestureKind.TAP) != GestureDisposition.REJECTED) {
            throw new IllegalStateException("Tap was not rejected after drag slop");
        }
        GestureArena hold = new GestureArena();
        hold.dispatch(new PointerEvent(PointerEventType.DOWN, 2.0f, 2.0f), 0L);
        hold.tick(GestureArena.LONG_PRESS_NANOS - 1L);
        if (hold.winner() != null) {
            throw new IllegalStateException("Long press won before the hold elapsed");
        }
        hold.tick(GestureArena.LONG_PRESS_NANOS);
        if (!hold.longPressAccepted()) {
            throw new IllegalStateException("Long press did not win a stationary hold");
        }
        GestureArena doubleTap = new GestureArena();
        doubleTap.dispatch(new PointerEvent(PointerEventType.DOWN, 8.0f, 8.0f), 0L);
        doubleTap.dispatch(new PointerEvent(PointerEventType.UP, 8.0f, 8.0f), 40_000_000L);
        doubleTap.dispatch(new PointerEvent(PointerEventType.DOWN, 8.0f, 8.0f), 80_000_000L);
        doubleTap.dispatch(new PointerEvent(PointerEventType.UP, 8.0f, 8.0f), 120_000_000L);
        if (!doubleTap.doubleTapAccepted()) {
            throw new IllegalStateException("Double tap did not win two short presses");
        }
        GestureArena scroll = new GestureArena();
        scroll.dispatch(
                new PointerEvent(PointerEventType.WHEEL, 0.0f, 0.0f, org.glavo.himari.layout.input.PointerDeviceKind.MOUSE, 1.0f),
                0L
        );
        if (!scroll.scrollAccepted() || scroll.lastScrollDelta() != 1.0f) {
            throw new IllegalStateException("Scroll did not win a wheel notch");
        }
        GestureArena scale = new GestureArena();
        scale.dispatch(new PointerEvent(PointerEventType.DOWN, 0.0f, 0.0f, org.glavo.himari.layout.input.PointerDeviceKind.TOUCH, 0.0f, 1), 0L);
        scale.dispatch(new PointerEvent(PointerEventType.DOWN, 100.0f, 0.0f, org.glavo.himari.layout.input.PointerDeviceKind.TOUCH, 0.0f, 2), 1L);
        scale.dispatch(new PointerEvent(PointerEventType.MOVE, 130.0f, 0.0f, org.glavo.himari.layout.input.PointerDeviceKind.TOUCH, 0.0f, 2), 16_000_000L);
        if (!scale.scaleAccepted() || Math.abs(scale.scale() - 1.3f) > 0.001f) {
            throw new IllegalStateException("Scale did not win a pinch");
        }
        GestureArena rotation = new GestureArena();
        rotation.dispatch(new PointerEvent(PointerEventType.DOWN, 0.0f, 0.0f, org.glavo.himari.layout.input.PointerDeviceKind.TOUCH, 0.0f, 1), 0L);
        rotation.dispatch(new PointerEvent(PointerEventType.DOWN, 100.0f, 0.0f, org.glavo.himari.layout.input.PointerDeviceKind.TOUCH, 0.0f, 2), 1L);
        rotation.dispatch(new PointerEvent(PointerEventType.MOVE, 0.0f, 100.0f, org.glavo.himari.layout.input.PointerDeviceKind.TOUCH, 0.0f, 2), 16_000_000L);
        if (!rotation.rotationAccepted() || Math.abs(rotation.rotation() - Math.PI / 2.0) > 0.01) {
            throw new IllegalStateException("Rotation did not win a twist");
        }
        Path output = Path.of(arguments[0]);
        Files.createDirectories(output);
        Files.writeString(
                output.resolve("results.json"),
                """
                        {
                          "profile": "m9-gestures",
                          "workPackage": "GESTURE-001",
                          "status": "passed",
                          "tapAccepted": true,
                          "dragAccepted": true,
                          "dragTranslationY": %s,
                          "dragVelocityY": %s,
                          "longPressAccepted": true,
                          "doubleTapAccepted": true,
                          "scrollAccepted": true,
                          "scaleAccepted": true,
                          "rotationAccepted": true
                        }
                        """.formatted(drag.translationY(), drag.velocity().y()),
                StandardCharsets.UTF_8
        );
    }
}

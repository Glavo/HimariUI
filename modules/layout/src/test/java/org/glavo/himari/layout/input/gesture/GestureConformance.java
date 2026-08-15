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
                          "longPressAccepted": true
                        }
                        """.formatted(drag.translationY(), drag.velocity().y()),
                StandardCharsets.UTF_8
        );
    }
}

package org.glavo.himari.layout.input.gesture;

import org.glavo.himari.layout.input.PointerEvent;
import org.glavo.himari.layout.input.PointerEventType;
import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies tap, drag, and long-press competition on injected timestamps.
@NotNullByDefault
final class GestureArenaTest {
    /// Accepts a short stationary press as a tap.
    @Test
    void acceptsTapInsideSlopAndTimeout() {
        GestureArena arena = new GestureArena();
        arena.dispatch(new PointerEvent(PointerEventType.DOWN, 10.0f, 10.0f), 0L);
        assertSame(GestureDisposition.POSSIBLE, arena.disposition(GestureKind.TAP));
        arena.dispatch(new PointerEvent(PointerEventType.UP, 11.0f, 10.0f), 80_000_000L);
        assertTrue(arena.tapAccepted());
        assertSame(GestureDisposition.REJECTED, arena.disposition(GestureKind.DRAG));
        assertSame(GestureDisposition.REJECTED, arena.disposition(GestureKind.LONG_PRESS));
    }

    /// Accepts a drag once movement exceeds slop and rejects tap.
    @Test
    void acceptsDragWhenSlopIsExceeded() {
        GestureArena arena = new GestureArena();
        arena.dispatch(new PointerEvent(PointerEventType.DOWN, 0.0f, 0.0f), 0L);
        arena.dispatch(new PointerEvent(PointerEventType.MOVE, 0.0f, 20.0f), 16_000_000L);
        assertTrue(arena.dragAccepted());
        assertEquals(20.0f, arena.translationY());
        assertEquals(20.0f, arena.lastDeltaY());
        arena.dispatch(new PointerEvent(PointerEventType.MOVE, 0.0f, 30.0f), 32_000_000L);
        assertEquals(10.0f, arena.lastDeltaY());
        assertEquals(30.0f, arena.translationY());
        assertTrue(arena.velocity().y() > 0.0f);
        assertSame(GestureDisposition.REJECTED, arena.disposition(GestureKind.TAP));
    }

    /// Accepts a long press after the hold timeout without movement.
    @Test
    void acceptsLongPressOnStationaryHold() {
        GestureArena arena = new GestureArena();
        arena.dispatch(new PointerEvent(PointerEventType.DOWN, 4.0f, 4.0f), 1_000_000L);
        arena.tick(GestureArena.LONG_PRESS_NANOS);
        assertNull(arena.winner());
        arena.tick(1_000_000L + GestureArena.LONG_PRESS_NANOS);
        assertTrue(arena.longPressAccepted());
        assertSame(GestureDisposition.REJECTED, arena.disposition(GestureKind.TAP));
    }

    /// Rejects a tap when the press is held past the tap timeout without a long-press tick.
    @Test
    void rejectsExpiredTapWithoutLongPressTick() {
        GestureArena arena = new GestureArena();
        arena.dispatch(new PointerEvent(PointerEventType.DOWN, 0.0f, 0.0f), 0L);
        arena.dispatch(
                new PointerEvent(PointerEventType.UP, 0.0f, 0.0f),
                GestureArena.TAP_TIMEOUT_NANOS + 1L
        );
        assertNull(arena.winner());
        assertSame(GestureDisposition.REJECTED, arena.disposition(GestureKind.TAP));
    }
}

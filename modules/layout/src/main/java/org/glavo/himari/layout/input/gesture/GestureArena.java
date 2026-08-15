package org.glavo.himari.layout.input.gesture;

import org.glavo.himari.layout.input.PointerEvent;
import org.glavo.himari.layout.input.PointerEventType;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/// Resolves tap, drag, and long-press competition for one pointer sequence.
///
/// Members start as [GestureDisposition#POSSIBLE]. The first recognizer that meets its accept
/// criterion wins and the others are rejected. Time is taken from caller-supplied timestamps so
/// Headless tests can replay traces without sleeping.
@NotNullByDefault
public final class GestureArena {
    /// Maximum movement, in logical pixels, that still counts as a tap or long press.
    public static final float SLOP = 8.0f;

    /// Maximum press duration, in nanoseconds, that still counts as a tap.
    public static final long TAP_TIMEOUT_NANOS = 300_000_000L;

    /// Hold duration, in nanoseconds, after which a stationary press becomes a long press.
    public static final long LONG_PRESS_NANOS = 500_000_000L;

    /// Whether a pointer is currently down.
    private boolean pointerDown;

    /// Origin of the current sequence.
    private float startX;

    /// Origin of the current sequence.
    private float startY;

    /// Timestamp of the current down.
    private long startNanos;

    /// Last observed position.
    private float lastX;

    /// Last observed position.
    private float lastY;

    /// Incremental horizontal delta of the last move.
    private float lastDeltaX;

    /// Incremental vertical delta of the last move.
    private float lastDeltaY;

    /// Exclusive winner, or `null` while the sequence is unresolved.
    private @Nullable GestureKind winner;

    /// Velocity estimator for the current sequence.
    private final VelocityTracker tracker = new VelocityTracker();

    /// Creates an empty arena.
    public GestureArena() {
    }

    /// Clears winner, samples, and pointer state.
    public void reset() {
        pointerDown = false;
        startX = 0.0f;
        startY = 0.0f;
        startNanos = 0L;
        lastX = 0.0f;
        lastY = 0.0f;
        lastDeltaX = 0.0f;
        lastDeltaY = 0.0f;
        winner = null;
        tracker.clear();
    }

    /// Advances the arena with one pointer event.
    ///
    /// @param event the pointer event
    /// @param timestampNanos the nonnegative event timestamp
    /// @return whether the event changed arena state
    public boolean dispatch(PointerEvent event, long timestampNanos) {
        Objects.requireNonNull(event, "event");
        if (timestampNanos < 0L) {
            throw new IllegalArgumentException("timestampNanos must be nonnegative");
        }
        return switch (event.type()) {
            case DOWN -> onDown(event.x(), event.y(), timestampNanos);
            case MOVE -> onMove(event.x(), event.y(), timestampNanos);
            case UP -> onUp(event.x(), event.y(), timestampNanos);
        };
    }

    /// Applies time-based recognizers without a pointer event.
    ///
    /// A stationary hold becomes [GestureKind#LONG_PRESS] once [LONG_PRESS_NANOS] elapses.
    ///
    /// @param timestampNanos the nonnegative clock reading
    public void tick(long timestampNanos) {
        if (timestampNanos < 0L) {
            throw new IllegalArgumentException("timestampNanos must be nonnegative");
        }
        if (!pointerDown || winner != null) {
            return;
        }
        if (timestampNanos - startNanos >= LONG_PRESS_NANOS && withinSlop(lastX, lastY)) {
            winner = GestureKind.LONG_PRESS;
        }
    }

    /// Returns the exclusive winner, or `null` while unresolved.
    ///
    /// @return the winner
    public @Nullable GestureKind winner() {
        return winner;
    }

    /// Returns whether a tap won the current sequence.
    ///
    /// @return whether the winner is a tap
    public boolean tapAccepted() {
        return winner == GestureKind.TAP;
    }

    /// Returns whether a drag won the current sequence.
    ///
    /// @return whether the winner is a drag
    public boolean dragAccepted() {
        return winner == GestureKind.DRAG;
    }

    /// Returns whether a long press won the current sequence.
    ///
    /// @return whether the winner is a long press
    public boolean longPressAccepted() {
        return winner == GestureKind.LONG_PRESS;
    }

    /// Returns the total translation from the down origin.
    ///
    /// @return the horizontal translation
    public float translationX() {
        return lastX - startX;
    }

    /// Returns the total translation from the down origin.
    ///
    /// @return the vertical translation
    public float translationY() {
        return lastY - startY;
    }

    /// Returns the incremental horizontal delta of the last move, or `0` otherwise.
    ///
    /// @return the delta
    public float lastDeltaX() {
        return lastDeltaX;
    }

    /// Returns the incremental vertical delta of the last move, or `0` otherwise.
    ///
    /// @return the delta
    public float lastDeltaY() {
        return lastDeltaY;
    }

    /// Returns the latest estimated velocity in logical pixels per second.
    ///
    /// @return the velocity
    public GestureVelocity velocity() {
        return tracker.velocity();
    }

    /// Returns the disposition of one recognizer after the latest event or tick.
    ///
    /// @param kind the recognizer
    /// @return the disposition
    public GestureDisposition disposition(GestureKind kind) {
        Objects.requireNonNull(kind, "kind");
        if (winner == null) {
            return pointerDown ? GestureDisposition.POSSIBLE : GestureDisposition.REJECTED;
        }
        return winner == kind ? GestureDisposition.ACCEPTED : GestureDisposition.REJECTED;
    }

    /// Starts a new sequence.
    ///
    /// @param x the down x
    /// @param y the down y
    /// @param timestampNanos the down timestamp
    /// @return `true`
    private boolean onDown(float x, float y, long timestampNanos) {
        reset();
        pointerDown = true;
        startX = x;
        startY = y;
        startNanos = timestampNanos;
        lastX = x;
        lastY = y;
        tracker.add(x, y, timestampNanos);
        return true;
    }

    /// Updates translation and accepts a drag when slop is exceeded.
    ///
    /// @param x the move x
    /// @param y the move y
    /// @param timestampNanos the move timestamp
    /// @return whether a pointer is down
    private boolean onMove(float x, float y, long timestampNanos) {
        if (!pointerDown) {
            return false;
        }
        lastDeltaX = x - lastX;
        lastDeltaY = y - lastY;
        lastX = x;
        lastY = y;
        tracker.add(x, y, timestampNanos);
        if (winner == null && !withinSlop(x, y)) {
            winner = GestureKind.DRAG;
        }
        return true;
    }

    /// Completes a tap or drag when no earlier winner exists.
    ///
    /// @param x the up x
    /// @param y the up y
    /// @param timestampNanos the up timestamp
    /// @return whether a pointer was down
    private boolean onUp(float x, float y, long timestampNanos) {
        if (!pointerDown) {
            return false;
        }
        lastDeltaX = x - lastX;
        lastDeltaY = y - lastY;
        lastX = x;
        lastY = y;
        tracker.add(x, y, timestampNanos);
        if (winner == null) {
            long held = timestampNanos - startNanos;
            if (withinSlop(x, y) && held <= TAP_TIMEOUT_NANOS) {
                winner = GestureKind.TAP;
            } else if (!withinSlop(x, y)) {
                winner = GestureKind.DRAG;
            }
        }
        pointerDown = false;
        return true;
    }

    /// Returns whether `(x, y)` remains inside the tap slop of the down origin.
    ///
    /// @param x the candidate x
    /// @param y the candidate y
    /// @return whether the point is inside slop
    private boolean withinSlop(float x, float y) {
        float dx = x - startX;
        float dy = y - startY;
        return dx * dx + dy * dy <= SLOP * SLOP;
    }
}

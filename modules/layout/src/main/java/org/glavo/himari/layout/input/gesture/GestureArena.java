package org.glavo.himari.layout.input.gesture;

import org.glavo.himari.layout.input.PointerEvent;
import org.glavo.himari.layout.input.PointerEventType;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/// Resolves tap, double-tap, drag, long-press, scroll, scale, and rotation competition.
///
/// Members start as [GestureDisposition#POSSIBLE]. The first recognizer that meets its accept
/// criterion wins and the others are rejected, except teammates joined by [#joinTeam], which
/// stay accepted with the winner. Time is taken from caller-supplied timestamps so Headless
/// tests can replay traces without sleeping.
@NotNullByDefault
public final class GestureArena {
    /// Maximum movement, in logical pixels, that still counts as a tap or long press.
    public static final float SLOP = 8.0f;

    /// Maximum press duration, in nanoseconds, that still counts as a tap.
    public static final long TAP_TIMEOUT_NANOS = 300_000_000L;

    /// Hold duration, in nanoseconds, after which a stationary press becomes a long press.
    public static final long LONG_PRESS_NANOS = 500_000_000L;

    /// Maximum interval, in nanoseconds, between two taps that still counts as a double tap.
    public static final long DOUBLE_TAP_NANOS = 300_000_000L;

    /// Relative distance change that accepts a pinch.
    public static final float SCALE_SLOP = 0.12f;

    /// Absolute angle change, in radians, that accepts a rotation.
    public static final float ROTATION_SLOP = 0.25f;

    /// Maximum simultaneous contacts tracked for scale and rotation.
    private static final int MAX_POINTERS = 2;

    /// Active contact count.
    private int pointerCount;

    /// Host pointer identities for the active contacts.
    private final int[] pointerIds = new int[MAX_POINTERS];

    /// Current x of each active contact.
    private final float[] xs = new float[MAX_POINTERS];

    /// Current y of each active contact.
    private final float[] ys = new float[MAX_POINTERS];

    /// Down x of each active contact.
    private final float[] startXs = new float[MAX_POINTERS];

    /// Down y of each active contact.
    private final float[] startYs = new float[MAX_POINTERS];

    /// Whether a pointer is currently down.
    private boolean pointerDown;

    /// Origin of the current single-pointer sequence.
    private float startX;

    /// Origin of the current single-pointer sequence.
    private float startY;

    /// Timestamp of the current down.
    private long startNanos;

    /// Last observed single-pointer position.
    private float lastX;

    /// Last observed single-pointer position.
    private float lastY;

    /// Incremental horizontal delta of the last move.
    private float lastDeltaX;

    /// Incremental vertical delta of the last move.
    private float lastDeltaY;

    /// Exclusive winner, or `null` while the sequence is unresolved.
    private @Nullable GestureKind winner;

    /// Whether the current sequence was cancelled.
    private boolean cancelled;

    /// Velocity estimator for the current sequence.
    private final VelocityTracker tracker = new VelocityTracker();

    /// Timestamp of the last accepted tap, or `Long.MIN_VALUE`.
    private long lastTapNanos = Long.MIN_VALUE;

    /// Position of the last accepted tap.
    private float lastTapX;

    /// Position of the last accepted tap.
    private float lastTapY;

    /// Whether the current sequence is a second tap inside the double-tap window.
    private boolean doubleTapCandidate;

    /// Contact distance when the second pointer landed.
    private float startDistance;

    /// Contact angle when the second pointer landed.
    private float startAngle;

    /// Latest scale relative to [startDistance], or `1` when unused.
    private float lastScale = 1.0f;

    /// Latest rotation in radians relative to [startAngle].
    private float lastRotation;

    /// Latest wheel delta that accepted [GestureKind#SCROLL].
    private float lastScrollDelta;

    /// Team identifier per [GestureKind#ordinal], or `0` when the kind is unteamed.
    private final int[] teamOf = new int[GestureKind.values().length];

    /// Next unused team identifier.
    private int nextTeam = 1;

    /// Creates an empty arena.
    public GestureArena() {
    }

    /// Puts `first`, `second`, and `more` on one team so they accept together.
    ///
    /// Kinds already on another team are merged into this team. Teams survive [#reset].
    ///
    /// @param first the first teammate
    /// @param second the second teammate
    /// @param more additional teammates
    public void joinTeam(GestureKind first, GestureKind second, GestureKind... more) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        Objects.requireNonNull(more, "more");
        int team = nextTeam++;
        adopt(first, team);
        adopt(second, team);
        for (GestureKind kind : more) {
            adopt(Objects.requireNonNull(kind, "kind"), team);
        }
    }

    /// Clears winner, samples, and pointer state. Remembers the last tap for double-tap.
    public void reset() {
        resetSequence();
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
        if (event.canceled()) {
            return cancel();
        }
        return switch (event.type()) {
            case DOWN -> onDown(event.pointerId(), event.x(), event.y(), timestampNanos);
            case MOVE -> onMove(event.pointerId(), event.x(), event.y(), timestampNanos);
            case UP -> onUp(event.pointerId(), event.x(), event.y(), timestampNanos);
            case WHEEL, WHEEL_HORIZONTAL -> onWheel(event.wheelDelta());
            case CAPTURE_CHANGED -> cancel();
            case SECONDARY_DOWN, SECONDARY_UP, MIDDLE_DOWN, MIDDLE_UP, ENTER, LEAVE, ACTIVATE,
                    NON_CLIENT_MOVE, NON_CLIENT_DOWN, NON_CLIENT_UP, ROUTED_TO, ROUTED_AWAY, ROUTED_RELEASED -> false;
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
        if (!pointerDown || winner != null || pointerCount != 1) {
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

    /// Returns whether the current sequence was cancelled.
    ///
    /// @return whether [#cancel()] ran or a cancelled event arrived
    public boolean cancelled() {
        return cancelled;
    }

    /// Cancels the current sequence and rejects every recognizer.
    ///
    /// @return `true`
    public boolean cancel() {
        resetSequence();
        cancelled = true;
        return true;
    }

    /// Returns whether a tap won the current sequence.
    ///
    /// @return whether the winner is a tap
    public boolean tapAccepted() {
        return accepted(GestureKind.TAP);
    }

    /// Returns whether a drag won the current sequence.
    ///
    /// @return whether the winner is a drag
    public boolean dragAccepted() {
        return accepted(GestureKind.DRAG);
    }

    /// Returns whether a long press won the current sequence.
    ///
    /// @return whether the winner is a long press
    public boolean longPressAccepted() {
        return accepted(GestureKind.LONG_PRESS);
    }

    /// Returns whether a double tap won the current sequence.
    ///
    /// @return whether the winner is a double tap
    public boolean doubleTapAccepted() {
        return accepted(GestureKind.DOUBLE_TAP);
    }

    /// Returns whether a wheel notch won.
    ///
    /// @return whether the winner is a scroll
    public boolean scrollAccepted() {
        return accepted(GestureKind.SCROLL);
    }

    /// Returns whether a pinch won.
    ///
    /// @return whether the winner is a scale
    public boolean scaleAccepted() {
        return accepted(GestureKind.SCALE);
    }

    /// Returns whether a two-pointer twist won.
    ///
    /// @return whether the winner is a rotation
    public boolean rotationAccepted() {
        return accepted(GestureKind.ROTATION);
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

    /// Returns the latest accepted wheel delta, or `0` when unused.
    ///
    /// @return the wheel notches
    public float lastScrollDelta() {
        return lastScrollDelta;
    }

    /// Returns the latest pinch scale relative to the second-pointer landing distance.
    ///
    /// @return the scale; `1` when unused
    public float scale() {
        return lastScale;
    }

    /// Returns the latest two-pointer rotation in radians.
    ///
    /// @return the rotation
    public float rotation() {
        return lastRotation;
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
        if (cancelled) {
            return GestureDisposition.CANCELLED;
        }
        if (winner == null) {
            return pointerDown || kind == GestureKind.SCROLL
                    ? GestureDisposition.POSSIBLE
                    : GestureDisposition.REJECTED;
        }
        if (winner == kind || teammates(winner, kind)) {
            return GestureDisposition.ACCEPTED;
        }
        return GestureDisposition.REJECTED;
    }

    /// Returns whether `kind` is accepted as the winner or a teammate.
    ///
    /// @param kind the recognizer
    /// @return whether the disposition is accepted
    private boolean accepted(GestureKind kind) {
        return disposition(kind) == GestureDisposition.ACCEPTED;
    }

    /// Merges `kind` into `team`, absorbing any previous team.
    ///
    /// @param kind the recognizer
    /// @param team the team identifier
    private void adopt(GestureKind kind, int team) {
        int index = kind.ordinal();
        int existing = teamOf[index];
        if (existing != 0 && existing != team) {
            for (int slot = 0; slot < teamOf.length; slot++) {
                if (teamOf[slot] == existing) {
                    teamOf[slot] = team;
                }
            }
        }
        teamOf[index] = team;
    }

    /// Returns whether two kinds share a nonzero team.
    ///
    /// @param first the first kind
    /// @param second the second kind
    /// @return whether they are teammates
    private boolean teammates(GestureKind first, GestureKind second) {
        int team = teamOf[first.ordinal()];
        return team != 0 && team == teamOf[second.ordinal()];
    }

    /// Starts or extends a press sequence.
    ///
    /// @param pointerId the host pointer identity
    /// @param x the down x
    /// @param y the down y
    /// @param timestampNanos the down timestamp
    /// @return `true`
    private boolean onDown(int pointerId, float x, float y, long timestampNanos) {
        int existing = indexOf(pointerId);
        if (existing >= 0) {
            xs[existing] = x;
            ys[existing] = y;
            return true;
        }
        if (pointerCount == 0) {
            resetSequence();
            pointerCount = 1;
            pointerIds[0] = pointerId;
            startXs[0] = x;
            startYs[0] = y;
            xs[0] = x;
            ys[0] = y;
            pointerDown = true;
            startX = x;
            startY = y;
            startNanos = timestampNanos;
            lastX = x;
            lastY = y;
            tracker.add(x, y, timestampNanos);
            doubleTapCandidate = lastTapNanos != Long.MIN_VALUE
                    && timestampNanos - lastTapNanos <= DOUBLE_TAP_NANOS
                    && withinTapHistory(x, y);
            return true;
        }
        if (pointerCount == 1) {
            pointerIds[1] = pointerId;
            startXs[1] = x;
            startYs[1] = y;
            xs[1] = x;
            ys[1] = y;
            pointerCount = 2;
            winner = null;
            doubleTapCandidate = false;
            startDistance = distance(xs[0], ys[0], xs[1], ys[1]);
            startAngle = angle(xs[0], ys[0], xs[1], ys[1]);
            lastScale = 1.0f;
            lastRotation = 0.0f;
            return true;
        }
        return false;
    }

    /// Updates translation and accepts drag, scale, or rotation.
    ///
    /// @param pointerId the host pointer identity
    /// @param x the move x
    /// @param y the move y
    /// @param timestampNanos the move timestamp
    /// @return whether a tracked pointer moved
    private boolean onMove(int pointerId, float x, float y, long timestampNanos) {
        int index = indexOf(pointerId);
        if (index < 0) {
            return false;
        }
        if (pointerCount == 1) {
            lastDeltaX = x - lastX;
            lastDeltaY = y - lastY;
            lastX = x;
            lastY = y;
            xs[0] = x;
            ys[0] = y;
            tracker.add(x, y, timestampNanos);
            if (winner == null && !withinSlop(x, y)) {
                winner = GestureKind.DRAG;
            }
            return true;
        }
        xs[index] = x;
        ys[index] = y;
        float currentDistance = distance(xs[0], ys[0], xs[1], ys[1]);
        float currentAngle = angle(xs[0], ys[0], xs[1], ys[1]);
        if (startDistance > 0.0f) {
            lastScale = currentDistance / startDistance;
        }
        lastRotation = wrapAngle(currentAngle - startAngle);
        if (winner == null) {
            if (startDistance > 0.0f && Math.abs(lastScale - 1.0f) >= SCALE_SLOP) {
                winner = GestureKind.SCALE;
            } else if (Math.abs(lastRotation) >= ROTATION_SLOP) {
                winner = GestureKind.ROTATION;
            }
        }
        return true;
    }

    /// Completes a tap, double tap, or drag when no earlier winner exists.
    ///
    /// @param pointerId the host pointer identity
    /// @param x the up x
    /// @param y the up y
    /// @param timestampNanos the up timestamp
    /// @return whether a tracked pointer was released
    private boolean onUp(int pointerId, float x, float y, long timestampNanos) {
        int index = indexOf(pointerId);
        if (index < 0) {
            return false;
        }
        xs[index] = x;
        ys[index] = y;
        if (pointerCount == 2) {
            removePointer(index);
            return true;
        }
        lastDeltaX = x - lastX;
        lastDeltaY = y - lastY;
        lastX = x;
        lastY = y;
        tracker.add(x, y, timestampNanos);
        if (winner == null) {
            long held = timestampNanos - startNanos;
            if (withinSlop(x, y) && held <= TAP_TIMEOUT_NANOS) {
                winner = doubleTapCandidate ? GestureKind.DOUBLE_TAP : GestureKind.TAP;
            } else if (!withinSlop(x, y)) {
                winner = GestureKind.DRAG;
            }
        }
        if (winner == GestureKind.TAP || winner == GestureKind.DOUBLE_TAP) {
            lastTapNanos = timestampNanos;
            lastTapX = x;
            lastTapY = y;
        } else {
            lastTapNanos = Long.MIN_VALUE;
        }
        pointerCount = 0;
        pointerDown = false;
        return true;
    }

    /// Accepts a wheel notch as [GestureKind#SCROLL].
    ///
    /// @param wheelDelta signed wheel notches
    /// @return `true`
    private boolean onWheel(float wheelDelta) {
        winner = GestureKind.SCROLL;
        lastScrollDelta = wheelDelta;
        pointerDown = false;
        pointerCount = 0;
        return true;
    }

    /// Clears the current sequence without forgetting the last tap.
    private void resetSequence() {
        pointerCount = 0;
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
        doubleTapCandidate = false;
        startDistance = 0.0f;
        startAngle = 0.0f;
        lastScale = 1.0f;
        lastRotation = 0.0f;
        lastScrollDelta = 0.0f;
        cancelled = false;
    }

    /// Returns the slot of `pointerId`, or `-1`.
    private int indexOf(int pointerId) {
        for (int index = 0; index < pointerCount; index++) {
            if (pointerIds[index] == pointerId) {
                return index;
            }
        }
        return -1;
    }

    /// Drops the contact at `index` and keeps the remaining contact as the primary.
    private void removePointer(int index) {
        int remaining = 1 - index;
        pointerIds[0] = pointerIds[remaining];
        startXs[0] = startXs[remaining];
        startYs[0] = startYs[remaining];
        xs[0] = xs[remaining];
        ys[0] = ys[remaining];
        startX = startXs[0];
        startY = startYs[0];
        lastX = xs[0];
        lastY = ys[0];
        pointerCount = 1;
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

    /// Returns whether `(x, y)` is inside slop of the last accepted tap.
    private boolean withinTapHistory(float x, float y) {
        float dx = x - lastTapX;
        float dy = y - lastTapY;
        return dx * dx + dy * dy <= SLOP * SLOP;
    }

    /// Returns the Euclidean distance between two contacts.
    private static float distance(float x0, float y0, float x1, float y1) {
        return (float) Math.hypot(x1 - x0, y1 - y0);
    }

    /// Returns the angle of the vector from the first contact to the second.
    private static float angle(float x0, float y0, float x1, float y1) {
        return (float) Math.atan2(y1 - y0, x1 - x0);
    }

    /// Wraps an angle into `(-π, π]`.
    private static float wrapAngle(float radians) {
        double wrapped = Math.atan2(Math.sin(radians), Math.cos(radians));
        return (float) wrapped;
    }
}

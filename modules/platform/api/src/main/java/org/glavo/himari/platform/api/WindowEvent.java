package org.glavo.himari.platform.api;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/// Reports one immutable window transition in session event order.
///
/// @param sequence the positive session-wide event sequence shared with display events
/// @param timestampNanos the nonnegative timestamp in the session clock domain
/// @param type the event type
/// @param snapshot the current snapshot associated with the event
/// @param previousSnapshot the previous snapshot for configuration and close transitions, otherwise
/// `null`
@NotNullByDefault
public record WindowEvent(
        long sequence,
        long timestampNanos,
        WindowEventType type,
        WindowSnapshot snapshot,
        @Nullable WindowSnapshot previousSnapshot
) {
    /// Creates a validated window event.
    ///
    /// @throws IllegalArgumentException if counters, lifecycle, or previous-snapshot presence do not
    /// match the event type
    public WindowEvent {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(snapshot, "snapshot");
        if (sequence <= 0L || timestampNanos < 0L) {
            throw new IllegalArgumentException("Window event sequence must be positive and timestamp nonnegative");
        }
        boolean requiresPrevious = type == WindowEventType.CONFIGURATION_CHANGED
                || type == WindowEventType.CLOSED;
        if (requiresPrevious != (previousSnapshot != null)) {
            throw new IllegalArgumentException("Window event previous-snapshot presence does not match its type");
        }
        if (previousSnapshot != null && !previousSnapshot.id().equals(snapshot.id())) {
            throw new IllegalArgumentException("Window event snapshots must identify the same window");
        }
        if (type == WindowEventType.CREATED && snapshot.lifecycle() != WindowLifecycle.OPEN) {
            throw new IllegalArgumentException("A created event requires an open window");
        }
        if (type == WindowEventType.CLOSED && snapshot.lifecycle() != WindowLifecycle.CLOSED) {
            throw new IllegalArgumentException("A closed event requires a closed window");
        }
        if (type != WindowEventType.CLOSED && snapshot.lifecycle() != WindowLifecycle.OPEN) {
            throw new IllegalArgumentException("Only a closed event may carry a closed current snapshot");
        }
        if (previousSnapshot != null
                && snapshot.configurationGeneration() <= previousSnapshot.configurationGeneration()) {
            throw new IllegalArgumentException("A snapshot transition must advance its configuration generation");
        }
        if (type == WindowEventType.CLOSED
                && previousSnapshot != null
                && previousSnapshot.lifecycle() != WindowLifecycle.OPEN) {
            throw new IllegalArgumentException("A closed event requires an open previous snapshot");
        }
    }
}

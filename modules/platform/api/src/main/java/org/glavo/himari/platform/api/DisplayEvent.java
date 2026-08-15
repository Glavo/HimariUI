package org.glavo.himari.platform.api;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/// Reports one ordered display-topology transition.
///
/// @param sequence the positive session-wide event sequence
/// @param timestampNanos the nonnegative event timestamp in the session clock domain
/// @param topologyGeneration the positive topology generation produced by the atomic replacement
/// @param type the transition type
/// @param previous the prior snapshot for `CHANGED` or `REMOVED`, otherwise `null`
/// @param current the new snapshot for `ADDED` or `CHANGED`, otherwise `null`
@NotNullByDefault
public record DisplayEvent(
        long sequence,
        long timestampNanos,
        long topologyGeneration,
        DisplayEventType type,
        @Nullable DisplaySnapshot previous,
        @Nullable DisplaySnapshot current
) {
    /// Creates a validated display event.
    ///
    /// @throws IllegalArgumentException if counters or snapshot presence do not match `type`
    public DisplayEvent {
        Objects.requireNonNull(type, "type");
        if (sequence <= 0L || timestampNanos < 0L || topologyGeneration <= 0L) {
            throw new IllegalArgumentException("Display event counters must be positive and timestamp nonnegative");
        }
        switch (type) {
            case ADDED -> {
                if (previous != null || current == null) {
                    throw new IllegalArgumentException("ADDED requires only a current snapshot");
                }
            }
            case CHANGED -> {
                if (previous == null || current == null || !previous.id().equals(current.id())) {
                    throw new IllegalArgumentException("CHANGED requires matching previous and current snapshots");
                }
            }
            case REMOVED -> {
                if (previous == null || current != null) {
                    throw new IllegalArgumentException("REMOVED requires only a previous snapshot");
                }
            }
        }
    }
}

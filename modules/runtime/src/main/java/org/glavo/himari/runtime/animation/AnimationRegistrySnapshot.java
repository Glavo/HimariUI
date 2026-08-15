package org.glavo.himari.runtime.animation;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Objects;

/// Captures one application animation registry at an atomic presentation publication boundary.
///
/// @param closed whether the registry stopped accepting work
/// @param presentationEpoch the nonnegative atomic presentation epoch
/// @param lastTimestampNanos the last sampled or committed timestamp, or `-1` before either
/// @param activeAnimationCount the number of active scalar timelines
/// @param nextWakeupNanos the earliest delayed start, current ready timestamp, or `-1` when idle
/// @param pendingCompletionEvents terminal events waiting to be drained
/// @param reservedCompletionSlots active plus queued exactly-once completion reservations
/// @param lastPhaseImpact the phases changed by the latest presentation epoch
/// @param properties immutable live property snapshots in creation order
@NotNullByDefault
public record AnimationRegistrySnapshot(
        boolean closed,
        long presentationEpoch,
        long lastTimestampNanos,
        int activeAnimationCount,
        long nextWakeupNanos,
        int pendingCompletionEvents,
        int reservedCompletionSlots,
        AnimationPhaseImpact lastPhaseImpact,
        @Unmodifiable List<AnimatedScalarSnapshot> properties
) {
    /// Validates counters and defensively copies property snapshots.
    ///
    /// @throws IllegalArgumentException if a timestamp sentinel, count, reservation, or wakeup
    /// invariant is invalid
    public AnimationRegistrySnapshot {
        if (presentationEpoch < 0L || lastTimestampNanos < -1L || nextWakeupNanos < -1L) {
            throw new IllegalArgumentException("Animation epoch and timestamp sentinels are invalid");
        }
        if (activeAnimationCount < 0 || pendingCompletionEvents < 0 || reservedCompletionSlots < 0) {
            throw new IllegalArgumentException("Animation registry counts must be non-negative");
        }
        if (pendingCompletionEvents > reservedCompletionSlots) {
            throw new IllegalArgumentException("Queued completions must have reserved slots");
        }
        if ((activeAnimationCount == 0) != (nextWakeupNanos == -1L)) {
            throw new IllegalArgumentException("Wakeup presence must match active animation presence");
        }
        Objects.requireNonNull(lastPhaseImpact, "lastPhaseImpact");
        Objects.requireNonNull(properties, "properties");
        properties = List.copyOf(properties);
    }
}

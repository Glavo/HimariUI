package org.glavo.himari.runtime;

import org.glavo.himari.platform.api.WindowId;
import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Summarizes one window scheduler's coalescing and lifecycle state.
///
/// @param windowId the scheduled window
/// @param requestedGeneration the latest accepted explicit request generation
/// @param deliveredGeneration the latest generation represented by a dispatched frame
/// @param pendingRequestCount requests waiting for a later redraw
/// @param hostRedrawOutstanding whether a requested host redraw has not yet been dispatched
/// @param frameRunning whether its frame callback is currently executing
/// @param closed whether this scheduler stopped accepting requests
@NotNullByDefault
public record WindowFrameSchedulerSnapshot(
        WindowId windowId,
        long requestedGeneration,
        long deliveredGeneration,
        long pendingRequestCount,
        boolean hostRedrawOutstanding,
        boolean frameRunning,
        boolean closed
) {
    /// Validates identifiers, generations, and closed-state pending-work cleanup.
    public WindowFrameSchedulerSnapshot {
        Objects.requireNonNull(windowId, "windowId");
        if (requestedGeneration < 0L || deliveredGeneration < 0L || pendingRequestCount < 0L) {
            throw new IllegalArgumentException("Frame scheduler counts must be non-negative");
        }
        if (deliveredGeneration > requestedGeneration) {
            throw new IllegalArgumentException("Delivered generation must not exceed requested generation");
        }
        if (closed && (pendingRequestCount != 0L || hostRedrawOutstanding)) {
            throw new IllegalArgumentException("A closed frame scheduler must retain no pending request");
        }
    }
}

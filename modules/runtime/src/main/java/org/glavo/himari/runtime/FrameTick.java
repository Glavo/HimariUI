package org.glavo.himari.runtime;

import org.glavo.himari.platform.api.WindowId;
import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Describes one window frame dispatched from a host redraw event.
///
/// @param windowId the window whose frame is executing
/// @param timestampNanos the single nonnegative [org.glavo.himari.platform.api.FrameClock]
/// timestamp sampled at frame entry
/// @param eventSequence the positive platform event sequence that admitted the frame
/// @param requestGeneration the latest explicit request generation represented by this frame, or
/// zero before the first explicit request
/// @param coalescedRequestCount the number of explicit frame requests consumed by this frame;
/// zero identifies an unsolicited host redraw
@NotNullByDefault
public record FrameTick(
        WindowId windowId,
        long timestampNanos,
        long eventSequence,
        long requestGeneration,
        long coalescedRequestCount
) {
    /// Validates identifiers and counters.
    public FrameTick {
        Objects.requireNonNull(windowId, "windowId");
        if (timestampNanos < 0L) {
            throw new IllegalArgumentException("timestampNanos must be non-negative");
        }
        if (eventSequence <= 0L) {
            throw new IllegalArgumentException("eventSequence must be positive");
        }
        if (requestGeneration < 0L || coalescedRequestCount < 0L) {
            throw new IllegalArgumentException("Frame request counters must be non-negative");
        }
        if (coalescedRequestCount > requestGeneration) {
            throw new IllegalArgumentException("Coalesced requests must not exceed the request generation");
        }
    }

    /// Returns whether application or runtime work explicitly requested this frame.
    ///
    /// @return whether at least one request was coalesced
    public boolean explicitlyRequested() {
        return coalescedRequestCount != 0L;
    }
}

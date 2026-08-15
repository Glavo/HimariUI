package org.glavo.himari.spikes.runtime.sample;

import org.jetbrains.annotations.NotNullByDefault;

/// Reports current-thread allocation bytes for one steady-state window.
///
/// @param available whether the JVM exposes an enabled current-thread allocation counter
/// @param bytes the nonnegative byte count, or zero when unavailable
@NotNullByDefault
public record AllocationMeasurement(boolean available, long bytes) {
    /// Creates a validated measurement.
    public AllocationMeasurement {
        ComparisonContracts.requireNonNegative(bytes, "allocation bytes");
        if (!available && bytes != 0L) {
            throw new IllegalArgumentException("unavailable allocation measurement must report zero bytes");
        }
    }
}

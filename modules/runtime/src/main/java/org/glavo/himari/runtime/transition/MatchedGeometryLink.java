package org.glavo.himari.runtime.transition;

import org.glavo.himari.platform.api.LogicalRect;
import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Stores one accepted source and destination geometry pair.
///
/// @param key the matched-geometry identity
/// @param source the source bounds captured after its layout pass
/// @param destination the destination bounds captured after its layout pass
@NotNullByDefault
public record MatchedGeometryLink(
        MatchedGeometryKey key,
        LogicalRect source,
        LogicalRect destination
) {
    /// Validates the link.
    public MatchedGeometryLink {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(destination, "destination");
    }

    /// Interpolates source toward destination at unit progress.
    ///
    /// @param progress the finite progress in `[0, 1]`
    /// @return the interpolated rectangle
    public LogicalRect interpolate(double progress) {
        if (!Double.isFinite(progress) || progress < 0.0 || progress > 1.0) {
            throw new IllegalArgumentException("Matched-geometry progress must lie in [0, 1]");
        }
        return new LogicalRect(
                lerp(source.x(), destination.x(), progress),
                lerp(source.y(), destination.y(), progress),
                lerp(source.width(), destination.width(), progress),
                lerp(source.height(), destination.height(), progress)
        );
    }

    /// Interpolates one finite coordinate.
    ///
    /// @param start the start value
    /// @param end the end value
    /// @param progress the unit progress
    /// @return the interpolated value
    private static double lerp(double start, double end, double progress) {
        return start + (end - start) * progress;
    }
}

package org.glavo.himari.spikes.runtime.sample;

import org.jetbrains.annotations.NotNullByDefault;

/// Reports significant ordinary-Java source lines for one candidate application file.
///
/// @param relativePath the repository-relative source path
/// @param stage the earliest comparison checkpoint requiring the source
/// @param sourceLines the nonnegative significant physical source-line count
@NotNullByDefault
public record SourceFileMetrics(String relativePath, FixtureStage stage, long sourceLines) {
    /// Creates validated per-file metrics.
    public SourceFileMetrics {
        relativePath = ComparisonContracts.requireText(relativePath, "source path");
        java.util.Objects.requireNonNull(stage, "stage");
        ComparisonContracts.requireNonNegative(sourceLines, "sourceLines");
    }
}

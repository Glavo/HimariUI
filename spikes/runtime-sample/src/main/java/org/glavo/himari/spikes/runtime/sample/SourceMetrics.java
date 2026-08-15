package org.glavo.himari.spikes.runtime.sample;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/// Reports auditable ordinary-Java source ceremony for one candidate corpus.
///
/// @param sourceLines total significant physical source lines
/// @param files immutable per-file source-line counts in corpus order
/// @param ceremonyCounts immutable counts for every ceremony category
/// @param markers immutable audited source locations
@NotNullByDefault
public record SourceMetrics(
        long sourceLines,
        @Unmodifiable List<SourceFileMetrics> files,
        @Unmodifiable Map<String, Long> ceremonyCounts,
        @Unmodifiable List<SourceMarker> markers
) {
    /// Creates an immutable metrics snapshot.
    public SourceMetrics {
        ComparisonContracts.requireNonNegative(sourceLines, "sourceLines");
        Objects.requireNonNull(files, "files");
        files = List.copyOf(files);
        for (SourceFileMetrics file : files) {
            Objects.requireNonNull(file, "source file metrics");
        }
        ceremonyCounts = ComparisonContracts.immutableSortedMap(ceremonyCounts, "ceremony counts");
        for (Map.Entry<String, Long> entry : ceremonyCounts.entrySet()) {
            ComparisonContracts.requireNonNegative(entry.getValue(), "ceremony count " + entry.getKey());
        }
        Objects.requireNonNull(markers, "markers");
        markers = List.copyOf(markers);
        for (SourceMarker marker : markers) {
            Objects.requireNonNull(marker, "source marker");
        }
    }
}

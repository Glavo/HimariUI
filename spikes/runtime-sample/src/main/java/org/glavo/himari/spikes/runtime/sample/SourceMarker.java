package org.glavo.himari.spikes.runtime.sample;

import org.jetbrains.annotations.NotNullByDefault;

import java.nio.file.Path;
import java.util.Objects;

/// Identifies one reviewable source location that contributes to an API-ceremony metric.
///
/// @param relativePath the forward-slash repository-relative Java source path
/// @param line the one-based physical line containing the ceremony
/// @param kind the measured ceremony category
/// @param rationale the reason this location belongs to the category
@NotNullByDefault
public record SourceMarker(
        String relativePath,
        int line,
        SourceCeremonyKind kind,
        String rationale
) {
    /// Creates a validated marker.
    public SourceMarker {
        Objects.requireNonNull(relativePath, "relativePath");
        Path path = Path.of(relativePath);
        if (relativePath.isBlank() || path.isAbsolute() || relativePath.contains("\\")
                || path.normalize().startsWith("..") || !relativePath.endsWith(".java")) {
            throw new IllegalArgumentException("relativePath must be a normalized forward-slash Java path");
        }
        if (line <= 0) {
            throw new IllegalArgumentException("marker line must be positive");
        }
        Objects.requireNonNull(kind, "kind");
        rationale = ComparisonContracts.requireText(rationale, "marker rationale");
    }
}

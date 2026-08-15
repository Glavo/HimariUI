package org.glavo.himari.spikes.runtime.sample;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/// Selects the ordinary-Java application sources and auditable ceremony markers for one candidate.
///
/// Source files must contain only the comparison applications and candidate-facing API use. Runtime
/// implementation files are excluded so the source metrics describe user ceremony rather than
/// implementation size.
///
/// @param repositoryRoot the absolute repository root used to resolve source paths
/// @param sourceUnits immutable repository-relative Java source files and their checkpoints
/// @param markers immutable ceremony markers within the selected files
@NotNullByDefault
public record SourceCorpus(
        Path repositoryRoot,
        @Unmodifiable List<SourceUnit> sourceUnits,
        @Unmodifiable List<SourceMarker> markers
) {
    /// Creates an immutable corpus and validates path containment and marker uniqueness.
    public SourceCorpus {
        Objects.requireNonNull(repositoryRoot, "repositoryRoot");
        repositoryRoot = repositoryRoot.toAbsolutePath().normalize();
        Objects.requireNonNull(sourceUnits, "sourceUnits");
        sourceUnits = List.copyOf(sourceUnits);
        if (sourceUnits.isEmpty()) {
            throw new IllegalArgumentException("source corpus must contain at least one Java file");
        }
        HashSet<String> files = new HashSet<>();
        for (SourceUnit sourceUnit : sourceUnits) {
            Objects.requireNonNull(sourceUnit, "source unit");
            String relativeFile = sourceUnit.relativePath();
            if (!files.add(relativeFile)) {
                throw new IllegalArgumentException("source corpus repeats " + relativeFile);
            }
            Path resolved = repositoryRoot.resolve(relativeFile).normalize();
            if (!resolved.startsWith(repositoryRoot)) {
                throw new IllegalArgumentException("source corpus path escapes repository root: " + relativeFile);
            }
        }
        Objects.requireNonNull(markers, "markers");
        markers = List.copyOf(markers);
        Set<String> identities = new HashSet<>();
        for (SourceMarker marker : markers) {
            Objects.requireNonNull(marker, "source marker");
            if (!files.contains(marker.relativePath())) {
                throw new IllegalArgumentException("source marker is outside the corpus: " + marker.relativePath());
            }
            String identity = marker.relativePath() + ':' + marker.line() + ':' + marker.kind();
            if (!identities.add(identity)) {
                throw new IllegalArgumentException("duplicate source marker " + identity);
            }
        }
    }

}

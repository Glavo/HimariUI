package org.glavo.himari.spikes.runtime.sample;

import org.jetbrains.annotations.NotNullByDefault;

import java.nio.file.Path;
import java.util.Objects;

/// Assigns one ordinary-Java application source file to a comparison checkpoint.
///
/// @param relativePath the normalized forward-slash repository-relative Java path
/// @param stage the earliest checkpoint that requires the file
@NotNullByDefault
public record SourceUnit(String relativePath, FixtureStage stage) {
    /// Creates a validated source unit.
    public SourceUnit {
        Objects.requireNonNull(relativePath, "relativePath");
        Path path = Path.of(relativePath);
        if (relativePath.isBlank() || path.isAbsolute() || relativePath.contains("\\")
                || path.normalize().startsWith("..") || !relativePath.endsWith(".java")
                || !path.normalize().toString().replace('\\', '/').equals(relativePath)) {
            throw new IllegalArgumentException("relativePath must be a normalized forward-slash Java path");
        }
        Objects.requireNonNull(stage, "stage");
    }
}

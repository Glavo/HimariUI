package org.glavo.himari.runtime.reload;

import org.jetbrains.annotations.NotNullByDefault;

import java.lang.foreign.MemorySegment;
import java.util.Objects;

/// Stores one published theme, style, image, or font generation.
///
/// @param kind the resource family
/// @param key the consumer key
/// @param bytes the published payload
/// @param generation the generation that published this payload
@NotNullByDefault
public record ResourceSnapshot(ResourceKind kind, String key, MemorySegment bytes, int generation) {
    /// Validates the snapshot.
    public ResourceSnapshot {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(bytes, "bytes");
        if (key.isEmpty()) {
            throw new IllegalArgumentException("Resource key must not be empty");
        }
        if (generation < 1) {
            throw new IllegalArgumentException("Resource generation must be positive");
        }
        bytes = bytes.asReadOnly();
    }
}

package org.glavo.himari.state;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Stores one producer and the semantic version observed by a successful consumer execution.
///
/// @param producer the producer read by the consumer
/// @param observedVersion the semantic version observed after the producer was pulled
@NotNullByDefault
record ReactiveDependency(ReactiveProducerNode producer, long observedVersion) {
    /// Validates the producer reference and version.
    ReactiveDependency {
        Objects.requireNonNull(producer, "producer");
        if (observedVersion < 0L) {
            throw new IllegalArgumentException("observedVersion must be non-negative");
        }
    }
}

package org.glavo.himari.runtime.transition;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Identifies one matched-geometry or shared-element pairing.
///
/// Geometry matching links presentation only. It does not transfer application state or element
/// ownership.
///
/// @param namespace the geometry namespace
/// @param id the stable identity within the namespace
@NotNullByDefault
public record MatchedGeometryKey(String namespace, String id) {
    /// Validates the key pair.
    public MatchedGeometryKey {
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(id, "id");
        if (namespace.isEmpty() || id.isEmpty()) {
            throw new IllegalArgumentException("Matched-geometry namespace and id must be non-empty");
        }
    }
}

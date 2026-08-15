package org.glavo.himari.runtime.transition;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Identifies one structural branch or collection member across updates.
///
/// Stable identity determines whether a later update retargets an existing presentation or
/// creates a new element. Duplicate claims in one frame are diagnostics, not a traversal-order
/// winner.
///
/// @param namespace the identity namespace
/// @param id the stable identity within the namespace
@NotNullByDefault
public record TransitionIdentity(String namespace, String id) {
    /// Validates the identity pair.
    public TransitionIdentity {
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(id, "id");
        if (namespace.isEmpty() || id.isEmpty()) {
            throw new IllegalArgumentException("Transition identity namespace and id must be non-empty");
        }
    }
}

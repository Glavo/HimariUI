package org.glavo.himari.spikes.runtime.sample;

import org.jetbrains.annotations.NotNullByDefault;

/// Classifies an auditable source location that contributes to ordinary-Java API ceremony.
@NotNullByDefault
public enum SourceCeremonyKind {
    /// An application-supplied key required for semantic identity.
    EXPLICIT_KEY,

    /// A deferred getter or lambda required solely to preserve a reactive read site.
    DEFERRED_GETTER,

    /// An explicit conditional, collection, or other topology-control primitive.
    STRUCTURAL_CONTROL,

    /// A runtime group boundary that does not express application-domain structure.
    GROUP_BOUNDARY,

    /// Explicit generic syntax required by the candidate API beyond the application's value types.
    GENERIC_TYPE_NOISE,

    /// A wrapper callback or adapter required only by the runtime API shape.
    CALLBACK_WRAPPER
}

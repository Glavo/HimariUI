package org.glavo.himari.state;

import org.jetbrains.annotations.NotNullByDefault;

/// Adapts one atomically published source state to the reactive producer graph.
@NotNullByDefault
final class SourceReactiveNode extends ReactiveProducerNode {
    /// The source whose publication version this node exposes.
    private final AbstractStateSource source;

    /// Creates a producer for one stable source slot.
    ///
    /// @param source the source state
    SourceReactiveNode(AbstractStateSource source) {
        super(
                source.owningDomain().reactiveGraph(),
                "StateSource#" + source.slot()
        );
        this.source = source;
    }

    /// Source publications are already current when invalidation begins.
    @Override
    void ensureCurrent() {
    }

    /// Returns the source version in the latest atomic publication.
    ///
    /// @return the source version
    @Override
    long semanticVersion() {
        return source.publishedVersion();
    }
}

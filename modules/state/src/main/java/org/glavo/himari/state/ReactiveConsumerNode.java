package org.glavo.himari.state;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

/// Represents one graph consumer independently of its runtime execution kind.
///
/// Derived values use [DerivedConsumerNode]. Later binding, phase, and effect consumers can reuse
/// the same producer edges without becoming derived producers themselves.
@NotNullByDefault
abstract class ReactiveConsumerNode {
    /// The graph containing this consumer.
    private final ReactiveGraph graph;

    /// Creates a consumer in one graph.
    ///
    /// @param graph the owning graph
    ReactiveConsumerNode(ReactiveGraph graph) {
        this.graph = graph;
    }

    /// Returns the owning graph.
    ///
    /// @return the graph
    final ReactiveGraph graph() {
        return graph;
    }

    /// Marks this consumer for dependency-version polling.
    ///
    /// @return whether the consumer changed from clean to check-required
    abstract boolean markCheckRequired();

    /// Returns the producer whose downstream consumers must also become dirty.
    ///
    /// A leaf consumer returns `null`.
    ///
    /// @return the corresponding producer, or `null`
    abstract @Nullable ReactiveProducerNode downstreamProducer();
}

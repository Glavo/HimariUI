package org.glavo.himari.state;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.ArrayDeque;
import java.util.LinkedHashSet;

/// Shares dependency-edge storage between source and derived reactive producers.
@NotNullByDefault
abstract class ReactiveProducerNode {
    /// The graph that owns this producer.
    private final ReactiveGraph graph;

    /// The stable diagnostic name used in cycle reports.
    private final String diagnosticName;

    /// The live direct consumers in deterministic attachment order.
    private final LinkedHashSet<ReactiveConsumerNode> consumers = new LinkedHashSet<>();

    /// Creates a producer in one reactive graph.
    ///
    /// @param graph the owning graph
    /// @param diagnosticName the stable non-empty diagnostic name
    ReactiveProducerNode(ReactiveGraph graph, String diagnosticName) {
        this.graph = graph;
        this.diagnosticName = diagnosticName;
    }

    /// Returns the owning graph.
    ///
    /// @return the graph
    final ReactiveGraph graph() {
        return graph;
    }

    /// Returns the stable diagnostic name.
    ///
    /// @return the name
    final String diagnosticName() {
        return diagnosticName;
    }

    /// Adds one direct consumer if it is not already attached.
    ///
    /// @param consumer the consumer to attach
    final void addConsumer(ReactiveConsumerNode consumer) {
        consumers.add(consumer);
    }

    /// Removes one direct consumer.
    ///
    /// @param consumer the consumer to detach
    final void removeConsumer(ReactiveConsumerNode consumer) {
        consumers.remove(consumer);
    }

    /// Appends all current direct consumers to an invalidation work queue.
    ///
    /// @param queue the destination queue
    final void appendConsumers(ArrayDeque<ReactiveConsumerNode> queue) {
        queue.addAll(consumers);
    }

    /// Removes all direct-consumer references.
    final void clearConsumers() {
        consumers.clear();
    }

    /// Returns the number of live direct consumers.
    ///
    /// @return the direct-consumer count
    final int consumerCount() {
        return consumers.size();
    }

    /// Pulls this producer until its semantic version is current.
    abstract void ensureCurrent();

    /// Returns the current semantic version without initiating a pull.
    ///
    /// @return the semantic version
    abstract long semanticVersion();
}

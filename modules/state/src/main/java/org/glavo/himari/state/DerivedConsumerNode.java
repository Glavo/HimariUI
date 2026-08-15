package org.glavo.himari.state;

import org.jetbrains.annotations.NotNullByDefault;

/// Adapts one [DerivedState] to the generic reactive-consumer edge model.
@NotNullByDefault
final class DerivedConsumerNode extends ReactiveConsumerNode {
    /// The derived state receiving invalidation.
    private final DerivedState<?> state;

    /// Creates a consumer adapter.
    ///
    /// @param state the derived state
    DerivedConsumerNode(DerivedState<?> state) {
        super(state.graph());
        this.state = state;
    }

    /// Delegates dirty-state transition to the derived state.
    ///
    /// @return whether the state became check-required
    @Override
    boolean markCheckRequired() {
        return state.markCheckRequired();
    }

    /// Returns the derived state as the producer for downstream propagation.
    ///
    /// @return the corresponding producer
    @Override
    ReactiveProducerNode downstreamProducer() {
        return state;
    }
}

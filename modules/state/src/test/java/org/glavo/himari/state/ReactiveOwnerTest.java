package org.glavo.himari.state;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies deterministic owner disposal and producer/consumer edge liveness.
@NotNullByDefault
final class ReactiveOwnerTest {
    /// Verifies that individual derived disposal removes upstream edges without closing siblings.
    @Test
    void disposesOneDerivedStateWithoutClosingItsOwner() {
        StateDomain domain = new StateDomain();
        IntState source = domain.intState(1);
        ReactiveOwner owner = domain.reactiveGraph().createOwner();
        DerivedState<Integer> first = owner.derivedState(() -> source.get() + 1);
        DerivedState<Integer> second = owner.derivedState(() -> source.get() + 2);

        assertEquals(2, first.get());
        assertEquals(3, second.get());
        assertEquals(2, source.reactiveNode().consumerCount());
        first.close();

        assertTrue(first.isDisposed());
        assertEquals(1, source.reactiveNode().consumerCount());
        assertFalse(owner.isDisposed());
        source.set(2);
        assertEquals(4, second.get());
        assertThrows(IllegalStateException.class, first::get);
    }

    /// Verifies child-first, reverse-creation disposal and root-owner release.
    @Test
    void closesOwnerTreeAndDetachesEveryDependency() {
        StateDomain domain = new StateDomain();
        IntState source = domain.intState(1);
        ReactiveGraph graph = domain.reactiveGraph();
        ReactiveOwner root = graph.createOwner();
        ReactiveOwner child = root.createChild();
        DerivedState<Integer> rootState = root.derivedState(source::get);
        DerivedState<Integer> childState = child.derivedState(() -> rootState.get() + source.get());

        assertEquals(2, childState.get());
        assertEquals(1, graph.ownerCount());
        assertEquals(2, source.reactiveNode().consumerCount());
        assertEquals(1, rootState.consumerCount());

        root.close();
        root.close();

        assertTrue(root.isDisposed());
        assertTrue(child.isDisposed());
        assertTrue(rootState.isDisposed());
        assertTrue(childState.isDisposed());
        assertEquals(0, graph.ownerCount());
        assertEquals(0, source.reactiveNode().consumerCount());
        assertEquals(0, rootState.consumerCount());
    }

    /// Verifies that closing an upstream owner invalidates a live cross-owner consumer.
    @Test
    void invalidatesConsumersBeforeDisposingAProducer() {
        StateDomain domain = new StateDomain();
        IntState source = domain.intState(1);
        ReactiveOwner producerOwner = domain.reactiveGraph().createOwner();
        ReactiveOwner consumerOwner = domain.reactiveGraph().createOwner();
        DerivedState<Integer> producer = producerOwner.derivedState(source::get);
        DerivedState<Integer> consumer = consumerOwner.derivedState(() -> producer.get() + 1);

        assertEquals(2, consumer.get());
        producerOwner.close();
        assertTrue(consumer.isDirty());
        assertThrows(IllegalStateException.class, consumer::get);
        assertEquals(1, consumer.dependencyCount());

        consumerOwner.close();
        assertEquals(0, domain.reactiveGraph().ownerCount());
    }

    /// Verifies that disposed owners reject further ownership and computation creation.
    @Test
    void rejectsCreationAfterOwnerDisposal() {
        StateDomain domain = new StateDomain();
        ReactiveOwner owner = domain.reactiveGraph().createOwner();
        owner.close();

        assertThrows(IllegalStateException.class, owner::createChild);
        assertThrows(IllegalStateException.class, () -> owner.derivedState(() -> 1));
    }

    /// Verifies that non-transactional ownership changes cannot leak from a source transaction.
    @Test
    void rejectsOwnershipMutationInsideStateTransactions() {
        StateDomain domain = new StateDomain();
        ReactiveGraph graph = domain.reactiveGraph();
        ReactiveOwner owner = graph.createOwner();
        DerivedState<Integer> state = owner.derivedState(() -> 1);

        StateTransaction.run(domain, () -> {
            assertThrows(IllegalStateException.class, graph::createOwner);
            assertThrows(IllegalStateException.class, owner::createChild);
            assertThrows(IllegalStateException.class, () -> owner.derivedState(() -> 2));
            assertThrows(IllegalStateException.class, state::close);
            assertThrows(IllegalStateException.class, owner::close);
        });

        assertEquals(1, graph.ownerCount());
        assertFalse(owner.isDisposed());
        assertFalse(state.isDisposed());
    }
}

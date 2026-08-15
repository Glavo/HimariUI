package org.glavo.himari.state;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies dynamic edges, untracked reads, cycle diagnostics, failure retry, and write barriers.
@NotNullByDefault
final class ReactiveDependencyTest {
    /// Verifies dependency reconciliation when a branch changes which source is read.
    @Test
    void reconcilesDynamicBranchDependencies() {
        StateDomain domain = new StateDomain();
        BooleanState useFirst = domain.booleanState(true);
        IntState first = domain.intState(1);
        IntState second = domain.intState(10);
        ReactiveOwner owner = domain.reactiveGraph().createOwner();
        AtomicInteger computations = new AtomicInteger();
        DerivedState<Integer> selected = owner.derivedState(() -> {
            computations.incrementAndGet();
            return useFirst.get() ? first.get() : second.get();
        });

        assertEquals(1, selected.get());
        assertEquals(2, selected.dependencyCount());
        second.set(11);
        assertFalse(selected.isDirty());
        assertEquals(1, selected.get());
        assertEquals(1, computations.get());

        useFirst.set(false);
        assertEquals(11, selected.get());
        assertEquals(2, selected.dependencyCount());
        first.set(2);
        assertFalse(selected.isDirty());
        second.set(12);
        assertTrue(selected.isDirty());
        assertEquals(12, selected.get());
        assertEquals(3, computations.get());
    }

    /// Verifies explicit non-tracking and that a later continuation inherits no capture context.
    ///
    /// @throws InterruptedException if the test thread is interrupted while joining the continuation
    @Test
    void excludesUntrackedAndLaterReadsFromDependencies() throws InterruptedException {
        StateDomain domain = new StateDomain();
        IntState tracked = domain.intState(1);
        IntState untracked = domain.intState(10);
        IntState later = domain.intState(100);
        ReactiveOwner owner = domain.reactiveGraph().createOwner();
        AtomicInteger computations = new AtomicInteger();
        AtomicReference<@Nullable Runnable> continuation = new AtomicReference<>();
        DerivedState<Integer> derived = owner.derivedState(() -> {
            computations.incrementAndGet();
            continuation.set(later::get);
            return tracked.get() + ReactiveReads.untracked(untracked::get);
        });

        assertEquals(11, derived.get());
        assertEquals(1, derived.dependencyCount());
        Runnable saved = continuation.get();
        if (saved == null) {
            throw new AssertionError("Expected continuation capture");
        }
        Thread continuationThread = Thread.ofPlatform().name("reactive-continuation").start(saved);
        continuationThread.join();
        later.set(101);
        untracked.set(20);
        assertFalse(derived.isDirty());
        assertEquals(11, derived.get());
        assertEquals(1, computations.get());

        tracked.set(2);
        assertEquals(22, derived.get());
        assertEquals(2, computations.get());
    }

    /// Verifies deterministic closed-path diagnostics for a two-node dependency cycle.
    @Test
    void reportsDeterministicCyclePath() {
        StateDomain domain = new StateDomain();
        ReactiveOwner owner = domain.reactiveGraph().createOwner();
        AtomicReference<@Nullable DerivedState<Integer>> firstReference = new AtomicReference<>();
        AtomicReference<@Nullable DerivedState<Integer>> secondReference = new AtomicReference<>();
        DerivedState<Integer> first = owner.derivedState(() -> require(secondReference).get() + 1);
        DerivedState<Integer> second = owner.derivedState(() -> require(firstReference).get() + 1);
        firstReference.set(first);
        secondReference.set(second);

        ReactiveCycleException failure = assertThrows(ReactiveCycleException.class, first::get);
        assertEquals(
                List.of(first.debugName(), second.debugName(), first.debugName()),
                failure.path()
        );
        assertEquals(
                "Reactive dependency cycle: " + first.debugName() + " -> "
                        + second.debugName() + " -> " + first.debugName(),
                failure.getMessage()
        );
        assertTrue(first.isDirty());
        assertTrue(second.isDirty());
    }

    /// Verifies that failed attempts retain old cache and edges while every later pull retries.
    @Test
    void retriesFailuresWithoutInstallingPartialDependencies() {
        StateDomain domain = new StateDomain();
        IntState source = domain.intState(0);
        BooleanState fail = domain.booleanState(true);
        ReactiveOwner owner = domain.reactiveGraph().createOwner();
        AtomicInteger attempts = new AtomicInteger();
        DerivedState<Integer> derived = owner.derivedState(() -> {
            attempts.incrementAndGet();
            int value = source.get();
            if (fail.get()) {
                throw new IllegalStateException("planned failure");
            }
            return value;
        });

        assertThrows(IllegalStateException.class, derived::get);
        assertEquals(0, derived.dependencyCount());
        assertEquals(0, source.reactiveNode().consumerCount());
        assertEquals(0, fail.reactiveNode().consumerCount());

        fail.set(false);
        assertEquals(0, derived.get());
        assertEquals(2, attempts.get());
        assertEquals(2, derived.dependencyCount());
        assertEquals(0L, derived.version());

        StateTransaction.run(domain, () -> {
            source.set(1);
            fail.set(true);
        });
        assertThrows(IllegalStateException.class, derived::get);
        assertEquals(2, derived.dependencyCount());
        assertEquals(0L, derived.semanticVersion());

        fail.set(false);
        assertEquals(1, derived.get());
        assertEquals(4, attempts.get());
        assertEquals(1L, derived.version());
    }

    /// Verifies that equality-policy failure leaves the previous cache, version, and edges intact.
    @Test
    void retriesEqualityPolicyFailuresWithoutPublishing() {
        StateDomain domain = new StateDomain();
        IntState source = domain.intState(0);
        AtomicBoolean failPolicy = new AtomicBoolean();
        ReactiveOwner owner = domain.reactiveGraph().createOwner();
        DerivedState<Integer> derived = owner.derivedState(
                source::get,
                (previous, next) -> {
                    if (failPolicy.get()) {
                        throw new IllegalStateException("planned equality failure");
                    }
                    return previous.equals(next);
                }
        );

        assertEquals(0, derived.get());
        failPolicy.set(true);
        source.set(1);
        assertThrows(IllegalStateException.class, derived::get);
        assertEquals(0L, derived.semanticVersion());
        assertEquals(1, derived.dependencyCount());
        assertEquals(1, source.reactiveNode().consumerCount());

        failPolicy.set(false);
        assertEquals(1, derived.get());
        assertEquals(1L, derived.version());
    }

    /// Verifies that derived computation cannot write state or mutate reactive ownership.
    @Test
    void rejectsStateWritesAndOwnershipMutationFromDerivations() {
        StateDomain domain = new StateDomain();
        IntState source = domain.intState(0);
        ReactiveOwner owner = domain.reactiveGraph().createOwner();
        DerivedState<Integer> writer = owner.derivedState(() -> {
            source.set(1);
            return source.get();
        });
        DerivedState<Integer> ownerMutation = owner.derivedState(() -> {
            owner.createChild();
            return 1;
        });

        IllegalStateException writeFailure = assertThrows(IllegalStateException.class, writer::get);
        assertTrue(writeFailure.getMessage().contains("cannot write application state"));
        assertEquals(0, source.get());
        assertEquals(0L, domain.epoch());
        assertThrows(IllegalStateException.class, ownerMutation::get);
        assertEquals(1, domain.reactiveGraph().ownerCount());
    }

    /// Verifies that a derivation cannot attach a producer from another application graph.
    @Test
    void rejectsCrossDomainDependencies() {
        StateDomain firstDomain = new StateDomain();
        StateDomain secondDomain = new StateDomain();
        IntState foreign = secondDomain.intState(1);
        ReactiveOwner owner = firstDomain.reactiveGraph().createOwner();
        DerivedState<Integer> derived = owner.derivedState(foreign::get);

        IllegalStateException failure = assertThrows(IllegalStateException.class, derived::get);
        assertEquals("Reactive dependencies cannot cross state domains", failure.getMessage());
        assertEquals(0, derived.dependencyCount());
        assertEquals(0, foreign.reactiveNode().consumerCount());
    }

    /// Returns the non-null value stored by a test reference.
    ///
    /// @param reference the reference to inspect
    /// @param <T> the referenced type
    /// @return the referenced value
    private static <T> T require(AtomicReference<@Nullable T> reference) {
        @Nullable T value = reference.get();
        if (value == null) {
            throw new AssertionError("Expected initialized test reference");
        }
        return value;
    }
}

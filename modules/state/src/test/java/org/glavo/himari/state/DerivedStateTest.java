package org.glavo.himari.state;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies lazy pull, semantic versions, equality suppression, diamonds, and transaction-local reads.
@NotNullByDefault
final class DerivedStateTest {
    /// Verifies that source publication pushes only dirtiness until the value is pulled.
    @Test
    void recomputesLazilyAfterSourcePublication() {
        StateDomain domain = new StateDomain();
        IntState source = domain.intState(1);
        ReactiveOwner owner = domain.reactiveGraph().createOwner();
        AtomicInteger computations = new AtomicInteger();
        DerivedState<Integer> doubled = owner.derivedState(() -> {
            computations.incrementAndGet();
            return source.get() * 2;
        });

        source.set(2);
        assertFalse(doubled.isInitialized());
        assertEquals(0, computations.get());

        assertEquals(4, doubled.get());
        assertEquals(1, computations.get());
        assertEquals(0L, doubled.version());
        assertFalse(doubled.isDirty());

        source.set(3);
        assertTrue(doubled.isDirty());
        assertEquals(1, computations.get());
        assertEquals(6, doubled.get());
        assertEquals(2, computations.get());
        assertEquals(1L, doubled.version());
    }

    /// Verifies that an equal upstream derivation prevents execution of its downstream consumer.
    @Test
    void skipsDownstreamWhenPulledDependencyVersionIsUnchanged() {
        StateDomain domain = new StateDomain();
        IntState source = domain.intState(0);
        ReactiveOwner owner = domain.reactiveGraph().createOwner();
        AtomicInteger parityComputations = new AtomicInteger();
        AtomicInteger labelComputations = new AtomicInteger();
        DerivedState<Integer> parity = owner.derivedState(() -> {
            parityComputations.incrementAndGet();
            return source.get() & 1;
        });
        DerivedState<String> label = owner.derivedState(() -> {
            labelComputations.incrementAndGet();
            return parity.get() == 0 ? "even" : "odd";
        });

        assertEquals("even", label.get());
        assertEquals(1, parityComputations.get());
        assertEquals(1, labelComputations.get());

        source.set(2);
        assertTrue(parity.isDirty());
        assertTrue(label.isDirty());
        assertEquals("even", label.get());
        assertEquals(2, parityComputations.get());
        assertEquals(1, labelComputations.get());
        assertEquals(0L, parity.version());
        assertEquals(0L, label.version());
        assertFalse(label.isDirty());
    }

    /// Verifies glitch-free pull through two branches of a diamond after one atomic source epoch.
    @Test
    void stabilizesDiamondDependenciesBeforeConsumerExecution() {
        StateDomain domain = new StateDomain();
        IntState source = domain.intState(1);
        ReactiveOwner owner = domain.reactiveGraph().createOwner();
        AtomicInteger leftComputations = new AtomicInteger();
        AtomicInteger rightComputations = new AtomicInteger();
        AtomicInteger totalComputations = new AtomicInteger();
        DerivedState<Integer> left = owner.derivedState(() -> {
            leftComputations.incrementAndGet();
            return source.get() + 1;
        });
        DerivedState<Integer> right = owner.derivedState(() -> {
            rightComputations.incrementAndGet();
            return source.get() * 2;
        });
        DerivedState<Integer> total = owner.derivedState(() -> {
            totalComputations.incrementAndGet();
            int leftValue = left.get();
            int rightValue = right.get();
            assertEquals((leftValue - 1) * 2, rightValue);
            return leftValue + rightValue;
        });

        assertEquals(4, total.get());
        StateTransaction.run(domain, () -> source.set(2));
        assertEquals(1, leftComputations.get());
        assertEquals(1, rightComputations.get());
        assertEquals(1, totalComputations.get());

        assertEquals(7, total.get());
        assertEquals(2, leftComputations.get());
        assertEquals(2, rightComputations.get());
        assertEquals(2, totalComputations.get());
    }

    /// Verifies that transaction-local derived reads neither publish nor replace persistent cache state.
    @Test
    void evaluatesEphemerallyInsideStateTransactions() {
        StateDomain domain = new StateDomain();
        IntState source = domain.intState(1);
        ReactiveOwner owner = domain.reactiveGraph().createOwner();
        AtomicInteger computations = new AtomicInteger();
        DerivedState<Integer> doubled = owner.derivedState(() -> {
            computations.incrementAndGet();
            return source.get() * 2;
        });

        assertEquals(2, doubled.get());
        assertEquals(1, computations.get());
        StateTransaction.run(domain, () -> {
            source.set(5);
            assertEquals(10, doubled.get());
            assertEquals(10, doubled.get());
            assertEquals(0L, doubled.version());
            assertTrue(doubled.isInitialized());
        });

        assertEquals(3, computations.get());
        assertTrue(doubled.isDirty());
        assertEquals(10, doubled.get());
        assertEquals(4, computations.get());
        assertEquals(1L, doubled.version());
    }

    /// Verifies that a rolled-back transaction leaves the persistent derived cache clean and unchanged.
    @Test
    void discardsEphemeralValuesWhenTransactionsRollBack() {
        StateDomain domain = new StateDomain();
        IntState source = domain.intState(1);
        ReactiveOwner owner = domain.reactiveGraph().createOwner();
        AtomicInteger computations = new AtomicInteger();
        DerivedState<Integer> doubled = owner.derivedState(() -> {
            computations.incrementAndGet();
            return source.get() * 2;
        });
        assertEquals(2, doubled.get());

        assertThrows(IllegalStateException.class, () -> StateTransaction.run(domain, () -> {
            source.set(5);
            assertEquals(10, doubled.get());
            throw new IllegalStateException("rollback");
        }));

        assertEquals(1, source.get());
        assertFalse(doubled.isDirty());
        assertEquals(2, doubled.get());
        assertEquals(2, computations.get());
        assertEquals(0L, doubled.version());
    }

    /// Verifies nullable results and runtime rejection of a null result from a non-null derivation.
    @Test
    void enforcesDerivedNullPolicy() {
        StateDomain domain = new StateDomain();
        BooleanState present = domain.booleanState(false);
        ReactiveOwner owner = domain.reactiveGraph().createOwner();
        DerivedState<@Nullable String> nullable = owner.nullableDerivedState(
                () -> present.get() ? "value" : null
        );
        AtomicReference<@Nullable String> result = new AtomicReference<>();
        DerivedState<String> invalid = owner.derivedState(result::get);

        assertNull(nullable.get());
        present.set(true);
        assertEquals("value", nullable.get());
        assertThrows(NullPointerException.class, invalid::get);
        result.set("recovered");
        assertEquals("recovered", invalid.get());
    }

    /// Verifies structural, identity, and never-equal semantic policies.
    @Test
    void appliesConfiguredEqualityPolicies() {
        StateDomain domain = new StateDomain();
        IntState trigger = domain.intState(0);
        ReactiveOwner owner = domain.reactiveGraph().createOwner();
        DerivedState<String> structural = owner.derivedState(() -> {
            trigger.get();
            return new String("stable");
        });
        DerivedState<String> identity = owner.derivedState(
                () -> {
                    trigger.get();
                    return new String("stable");
                },
                EqualityPolicy.identity()
        );
        DerivedState<Integer> alwaysChanged = owner.derivedState(
                () -> {
                    trigger.get();
                    return 1;
                },
                EqualityPolicy.neverEqual()
        );

        structural.get();
        identity.get();
        alwaysChanged.get();
        trigger.set(1);
        structural.get();
        identity.get();
        alwaysChanged.get();

        assertEquals(0L, structural.version());
        assertEquals(1L, identity.version());
        assertEquals(1L, alwaysChanged.version());
    }

    /// Verifies that atomic source snapshots do not misrepresent lazy derived caches as epoch data.
    @Test
    void excludesDerivedCachesFromSourceSnapshots() {
        StateDomain domain = new StateDomain();
        IntState source = domain.intState(1);
        ReactiveOwner owner = domain.reactiveGraph().createOwner();
        DerivedState<Integer> derived = owner.derivedState(() -> source.get() + 1);
        StateSnapshot snapshot = domain.snapshot();

        assertEquals(2, derived.get());
        assertThrows(IllegalArgumentException.class, () -> snapshot.get(derived));
        assertThrows(IllegalArgumentException.class, () -> snapshot.version(derived));
    }
}

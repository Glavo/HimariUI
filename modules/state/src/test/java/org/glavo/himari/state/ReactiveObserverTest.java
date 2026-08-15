package org.glavo.himari.state;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies detached dependency capture and leaf-observer invalidation semantics.
@NotNullByDefault
final class ReactiveObserverTest {
    /// Verifies that structural-style observation cannot publish state mutations.
    @Test
    void rejectsStateWritesDuringCapture() {
        StateDomain domain = new StateDomain();
        IntState source = domain.intState(1);
        try (ReactiveOwner owner = domain.reactiveGraph().createOwner();
             ReactiveObserver observer = owner.createObserver()) {
            assertThrows(IllegalStateException.class, () -> observer.capture(() -> source.set(2)));
            assertEquals(1, source.get());
            assertTrue(observer.isInvalidated());
        }
    }

    /// Verifies that only a committed observation mutates producer edges.
    @Test
    void publishesDependenciesOnlyWhenObservationCommits() {
        StateDomain domain = new StateDomain();
        IntState source = domain.intState(1);
        try (ReactiveOwner owner = domain.reactiveGraph().createOwner();
             ReactiveObserver observer = owner.createObserver()) {
            ReactiveObservation stale = observer.capture(source::get);
            source.set(2);
            assertThrows(IllegalStateException.class, stale::commit);
            stale.close();
            assertTrue(observer.isInvalidated());

            try (ReactiveObservation observation = observer.capture(source::get)) {
                observation.commit();
            }
            assertFalse(observer.isInvalidated());

            source.set(2);
            assertFalse(observer.isInvalidated());
            source.set(3);
            assertTrue(observer.isInvalidated());
        }
    }

    /// Verifies dynamic dependency replacement and derived equality suppression.
    @Test
    void pollsSemanticVersionsAndReplacesDynamicDependencies() {
        StateDomain domain = new StateDomain();
        BooleanState useLeft = domain.booleanState(true);
        IntState left = domain.intState(1);
        IntState right = domain.intState(2);
        try (ReactiveOwner owner = domain.reactiveGraph().createOwner()) {
            DerivedState<Integer> parity = owner.derivedState(
                    () -> (useLeft.get() ? left.get() : right.get()) & 1
            );
            try (ReactiveObserver observer = owner.createObserver()) {
                try (ReactiveObservation observation = observer.capture(parity::get)) {
                    observation.commit();
                }
                assertFalse(observer.isInvalidated());

                left.set(3);
                assertFalse(observer.isInvalidated());

                left.set(4);
                assertTrue(observer.isInvalidated());
                try (ReactiveObservation observation = observer.capture(parity::get)) {
                    observation.commit();
                }

                useLeft.set(false);
                assertFalse(observer.isInvalidated());
                left.set(6);
                assertFalse(observer.isInvalidated());
                right.set(3);
                assertTrue(observer.isInvalidated());
            }
        }
    }

    /// Verifies explicit deactivation, stale-capture rejection, and owner disposal.
    @Test
    void clearsAndDisposesObserversDeterministically() {
        StateDomain domain = new StateDomain();
        IntState source = domain.intState(1);
        ReactiveOwner owner = domain.reactiveGraph().createOwner();
        ReactiveObserver observer = owner.createObserver();

        ReactiveObservation stale = observer.capture(source::get);
        observer.clearDependencies();
        assertThrows(IllegalStateException.class, stale::commit);
        stale.close();
        assertTrue(observer.isInvalidated());

        try (ReactiveObservation discarded = observer.capture(source::get)) {
            assertFalse(discarded.isResolved());
        }
        assertTrue(observer.isInvalidated());

        owner.close();
        assertTrue(observer.isDisposed());
        assertThrows(IllegalStateException.class, observer::isInvalidated);
        observer.close();
    }
}

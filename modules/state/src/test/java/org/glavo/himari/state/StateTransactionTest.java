package org.glavo.himari.state;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/// Verifies atomic publication, coalescing, nested savepoints, and transaction failure cleanup.
@NotNullByDefault
final class StateTransactionTest {
    /// Verifies that transaction-local reads see staged values while snapshots remain published.
    @Test
    void publishesSeveralSourcesAsOneEpoch() {
        StateDomain domain = new StateDomain();
        IntState first = domain.intState(0);
        IntState second = domain.intState(0);
        StateSnapshot before = domain.snapshot();

        StateTransaction.run(domain, () -> {
            first.set(1);
            second.set(2);
            first.set(3);

            assertEquals(3, first.get());
            assertEquals(2, second.get());
            assertEquals(0L, first.version());
            assertEquals(0L, domain.epoch());
            assertEquals(0, domain.snapshot().get(first));
        });

        assertEquals(1L, domain.epoch());
        assertEquals(3, first.get());
        assertEquals(2, second.get());
        assertEquals(1L, first.version());
        assertEquals(1L, second.version());
        assertEquals(0, before.get(first));
        assertEquals(0, before.get(second));
    }

    /// Verifies that repeated writes ending at the published value do not create an epoch.
    @Test
    void elidesTransactionsWithNoFinalSemanticChange() {
        StateDomain domain = new StateDomain();
        IntState state = domain.intState(10);

        StateTransaction.run(domain, () -> {
            state.set(11);
            state.set(12);
            state.set(10);
        });

        assertEquals(10, state.get());
        assertEquals(0L, state.version());
        assertEquals(0L, domain.epoch());
    }

    /// Verifies flattening of successful nesting and savepoint rollback of failed nesting.
    @Test
    void restoresOnlyTheFailedNestedSavepoint() {
        StateDomain domain = new StateDomain();
        IntState first = domain.intState(0);
        IntState second = domain.intState(0);

        StateTransaction.run(domain, () -> {
            first.set(1);
            StateTransaction.run(domain, () -> {
                first.set(2);
                second.set(2);
            });

            assertThrows(IllegalArgumentException.class, () -> StateTransaction.run(domain, () -> {
                first.set(30);
                second.set(30);
                throw new IllegalArgumentException("rollback nested work");
            }));

            assertEquals(2, first.get());
            assertEquals(2, second.get());
            second.set(4);
        });

        assertEquals(2, first.get());
        assertEquals(4, second.get());
        assertEquals(1L, domain.epoch());
        assertEquals(1L, first.version());
        assertEquals(1L, second.version());
    }

    /// Verifies that an outer failure discards successful nested work and later writes.
    @Test
    void rollsBackTheWholeOutermostTransaction() {
        StateDomain domain = new StateDomain();
        MutableState<String> state = domain.mutableState("before");

        assertThrows(IllegalStateException.class, () -> StateTransaction.run(domain, () -> {
            StateTransaction.run(domain, () -> state.set("nested"));
            state.set("after");
            throw new IllegalStateException("abort outer transaction");
        }));

        assertEquals("before", state.get());
        assertEquals(0L, state.version());
        assertEquals(0L, domain.epoch());
    }

    /// Verifies that cross-domain nesting fails without publishing either domain.
    @Test
    void rejectsOverlappingDomains() {
        StateDomain firstDomain = new StateDomain();
        StateDomain secondDomain = new StateDomain();
        IntState first = firstDomain.intState(0);
        IntState second = secondDomain.intState(0);

        assertThrows(IllegalStateException.class, () -> StateTransaction.run(firstDomain, () -> {
            first.set(1);
            StateTransaction.run(secondDomain, () -> second.set(1));
        }));

        assertEquals(0, first.get());
        assertEquals(0, second.get());
        assertEquals(0L, firstDomain.epoch());
        assertEquals(0L, secondDomain.epoch());
    }

    /// Verifies that non-transactional registration cannot leak through transaction rollback.
    @Test
    void rejectsSourceRegistrationAndQueueDrainInsideTransactions() {
        StateDomain domain = new StateDomain();

        StateTransaction.run(domain, () -> {
            assertThrows(IllegalStateException.class, () -> domain.intState(1));
            assertThrows(IllegalStateException.class, () -> domain.externalCommits().drain());
        });

        assertEquals(0, domain.snapshot().sourceCount());
        assertEquals(0L, domain.epoch());
    }

    /// Verifies that an object update callback and any writes it performs share one transaction.
    @Test
    void runsObjectUpdateCallbacksInsideTheTransaction() {
        StateDomain domain = new StateDomain();
        MutableState<String> text = domain.mutableState("before");
        IntState sideWrite = domain.intState(0);

        text.update(value -> {
            sideWrite.set(1);
            return value + "!";
        });

        assertEquals("before!", text.get());
        assertEquals(1, sideWrite.get());
        assertEquals(1L, text.version());
        assertEquals(1L, sideWrite.version());
        assertEquals(1L, domain.epoch());
    }

    /// Verifies that user equality code cannot reenter source mutation during commit.
    @Test
    void rejectsWritesReenteredFromObjectEquality() {
        StateDomain domain = new StateDomain();
        IntState sideWrite = domain.intState(0);
        ReentrantValue initial = new ReentrantValue("initial", () -> sideWrite.set(1));
        MutableState<ReentrantValue> state = domain.mutableState(initial);

        assertThrows(
                IllegalStateException.class,
                () -> state.set(new ReentrantValue("replacement", () -> { }))
        );

        assertSame(initial, state.get());
        assertEquals(0, sideWrite.get());
        assertEquals(0L, state.version());
        assertEquals(0L, sideWrite.version());
        assertEquals(0L, domain.epoch());
    }

    /// Supplies equality behavior that deliberately attempts one state write.
    @NotNullByDefault
    private static final class ReentrantValue {
        /// The logical value identifier.
        private final String id;

        /// The action invoked before equality is evaluated.
        private final Runnable equalityAction;

        /// Creates a value with an equality side effect.
        ///
        /// @param id the logical identifier
        /// @param equalityAction the action invoked by [#equals(Object)]
        private ReentrantValue(String id, Runnable equalityAction) {
            this.id = id;
            this.equalityAction = equalityAction;
        }

        /// Invokes the configured action and compares logical identifiers.
        ///
        /// @param other the candidate value, which may be `null`
        /// @return whether the candidate has the same identifier
        @Override
        public boolean equals(@Nullable Object other) {
            equalityAction.run();
            return other instanceof ReentrantValue value && id.equals(value.id);
        }

        /// Returns the logical identifier hash.
        ///
        /// @return the identifier hash
        @Override
        public int hashCode() {
            return Objects.hashCode(id);
        }
    }
}

package org.glavo.himari.state;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/// Verifies source construction, semantic versions, owner-thread rules, and immutable snapshots.
@NotNullByDefault
final class StateDomainTest {
    /// Verifies initial values and source metadata for every state kind.
    @Test
    void createsObjectAndPrimitiveSources() {
        StateDomain domain = new StateDomain();
        MutableState<String> object = domain.mutableState("value");
        MutableState<@Nullable String> nullable = domain.nullableState(null);
        IntState integer = domain.intState(12);
        LongState longValue = domain.longState(34L);
        FloatState floatValue = domain.floatState(5.5f);
        BooleanState booleanValue = domain.booleanState(true);

        StateSnapshot snapshot = domain.snapshot();
        assertEquals(0L, snapshot.epoch());
        assertEquals(6, snapshot.sourceCount());
        assertEquals("value", snapshot.get(object));
        assertNull(snapshot.get(nullable));
        assertEquals(12, snapshot.get(integer));
        assertEquals(34L, snapshot.get(longValue));
        assertEquals(5.5f, snapshot.get(floatValue));
        assertEquals(true, snapshot.get(booleanValue));
        for (StateSource source : List.of(object, nullable, integer, longValue, floatValue, booleanValue)) {
            assertSame(domain, source.domain());
            assertEquals(0L, source.version());
            assertEquals(0L, snapshot.version(source));
        }
    }

    /// Verifies default object equality and the documented float bit semantics.
    @Test
    void advancesEpochOnlyForSemanticChanges() {
        StateDomain domain = new StateDomain();
        MutableState<String> object = domain.mutableState("same");
        FloatState floatValue = domain.floatState(Float.NaN);

        object.set(new String("same"));
        floatValue.set(Float.intBitsToFloat(0x7f80_0001));
        assertEquals(0L, domain.epoch());
        assertEquals(0L, object.version());
        assertEquals(0L, floatValue.version());

        object.update(value -> value + "!");
        assertEquals(1L, domain.epoch());
        assertEquals(1L, object.version());
        assertEquals(0L, floatValue.version());

        floatValue.set(-0.0f);
        assertEquals(2L, domain.epoch());
        floatValue.set(0.0f);
        assertEquals(3L, domain.epoch());
        assertEquals(2L, floatValue.version());
    }

    /// Verifies runtime enforcement of nullable and non-null object factories.
    @Test
    void enforcesObjectNullPolicy() {
        StateDomain domain = new StateDomain();
        MutableState<String> nonNull = domain.mutableState("value");
        MutableState<@Nullable String> nullable = domain.nullableState("value");

        assertThrows(NullPointerException.class, () -> nonNull.set(null));
        nullable.set(null);
        assertNull(nullable.get());
        assertEquals(1L, nullable.version());
    }

    /// Verifies that old chunked publications remain stable and reject later source slots.
    @Test
    void snapshotsRemainStableAcrossChunksAndRegistrations() {
        StateDomain domain = new StateDomain();
        List<IntState> states = new ArrayList<>();
        for (int index = 0; index < 100; index++) {
            states.add(domain.intState(index));
        }
        StateSnapshot before = domain.snapshot();

        StateTransaction.run(domain, () -> {
            states.get(0).set(1000);
            states.get(31).set(1031);
            states.get(32).set(1032);
            states.get(99).set(1099);
        });
        StateSnapshot after = domain.snapshot();

        assertEquals(0, before.get(states.get(0)));
        assertEquals(31, before.get(states.get(31)));
        assertEquals(32, before.get(states.get(32)));
        assertEquals(99, before.get(states.get(99)));
        assertEquals(1000, after.get(states.get(0)));
        assertEquals(1031, after.get(states.get(31)));
        assertEquals(1032, after.get(states.get(32)));
        assertEquals(1099, after.get(states.get(99)));

        IntState later = domain.intState(200);
        assertThrows(IllegalArgumentException.class, () -> before.get(later));
        StateDomain otherDomain = new StateDomain();
        IntState foreign = otherDomain.intState(1);
        assertThrows(IllegalArgumentException.class, () -> after.get(foreign));
    }

    /// Verifies that published reads are allowed off-thread while writes and registration are not.
    ///
    /// @throws InterruptedException if the test thread is interrupted while joining the worker
    @Test
    void restrictsMutationToOwnerThread() throws InterruptedException {
        StateDomain domain = new StateDomain();
        IntState state = domain.intState(7);
        AtomicInteger observed = new AtomicInteger();
        AtomicReference<@Nullable Throwable> writeFailure = new AtomicReference<>();
        AtomicReference<@Nullable Throwable> registrationFailure = new AtomicReference<>();

        Thread worker = Thread.ofPlatform().name("state-worker").start(() -> {
            observed.set(state.get());
            try {
                state.set(8);
            } catch (Throwable failure) {
                writeFailure.set(failure);
            }
            try {
                domain.intState(9);
            } catch (Throwable failure) {
                registrationFailure.set(failure);
            }
        });
        worker.join();

        assertEquals(7, observed.get());
        assertInstanceOf(IllegalStateException.class, writeFailure.get());
        assertInstanceOf(IllegalStateException.class, registrationFailure.get());
        assertEquals(7, state.get());
        assertEquals(0L, domain.epoch());
    }
}

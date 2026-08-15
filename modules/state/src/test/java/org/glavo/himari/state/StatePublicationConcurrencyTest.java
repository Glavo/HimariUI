package org.glavo.himari.state;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies atomic publication visibility to a concurrent snapshot reader.
@NotNullByDefault
final class StatePublicationConcurrencyTest {
    /// Verifies that no reader observes values from two different committed epochs.
    ///
    /// @throws InterruptedException if the test thread is interrupted while coordinating the reader
    @Test
    void exposesOnlyCompleteCrossSourcePublications() throws InterruptedException {
        StateDomain domain = new StateDomain();
        IntState positive = domain.intState(0);
        IntState negative = domain.intState(0);
        AtomicBoolean stop = new AtomicBoolean();
        AtomicReference<@Nullable String> mismatch = new AtomicReference<>();
        CountDownLatch started = new CountDownLatch(1);

        Thread reader = Thread.ofPlatform().name("snapshot-reader").start(() -> {
            started.countDown();
            while (!stop.get()) {
                StateSnapshot snapshot = domain.snapshot();
                int first = snapshot.get(positive);
                int second = snapshot.get(negative);
                if (second != -first) {
                    mismatch.compareAndSet(null, first + ":" + second + "@" + snapshot.epoch());
                    return;
                }
            }
        });

        started.await();
        for (int value = 1; value <= 10_000; value++) {
            int committedValue = value;
            StateTransaction.run(domain, () -> {
                positive.set(committedValue);
                negative.set(-committedValue);
            });
        }
        stop.set(true);
        reader.join();

        assertNull(mismatch.get());
        assertEquals(10_000L, domain.epoch());
        assertEquals(10_000L, positive.version());
        assertEquals(10_000L, negative.version());
        assertTrue(domain.snapshot().epoch() > 0L);
    }
}

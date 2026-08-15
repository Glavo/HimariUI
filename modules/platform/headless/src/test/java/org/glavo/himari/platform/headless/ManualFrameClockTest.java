package org.glavo.himari.platform.headless;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/// Verifies deterministic monotonic clock advancement and owner-thread confinement.
@NotNullByDefault
final class ManualFrameClockTest {
    /// Verifies exact absolute and relative advancement without wall-clock participation.
    @Test
    void advancesOnlyForwardWithoutSleeping() {
        ManualFrameClock clock = new ManualFrameClock(10L);

        assertEquals(10L, clock.nowNanos());
        clock.advanceTo(10L);
        clock.advanceBy(5L);
        assertEquals(15L, clock.nowNanos());
        assertThrows(IllegalArgumentException.class, () -> clock.advanceTo(14L));
        assertThrows(IllegalArgumentException.class, () -> clock.advanceBy(-1L));

        ManualFrameClock exhausted = new ManualFrameClock(Long.MAX_VALUE);
        assertThrows(ArithmeticException.class, () -> exhausted.advanceBy(1L));
        assertEquals(Long.MAX_VALUE, exhausted.nowNanos());
    }

    /// Verifies cross-thread reads and rejects cross-thread advancement.
    ///
    /// @throws InterruptedException if the test thread is interrupted while joining the worker
    @Test
    void publishesReadsButConfinesAdvancementToOwner() throws InterruptedException {
        ManualFrameClock clock = new ManualFrameClock(42L);
        AtomicLong observed = new AtomicLong();
        AtomicReference<@Nullable Throwable> failure = new AtomicReference<>();
        Thread worker = Thread.ofPlatform().name("headless-clock-worker").start(() -> {
            observed.set(clock.nowNanos());
            try {
                clock.advanceTo(43L);
            } catch (Throwable thrown) {
                failure.set(thrown);
            }
        });
        worker.join();

        assertEquals(42L, observed.get());
        assertInstanceOf(IllegalStateException.class, failure.get());
        assertEquals(42L, clock.nowNanos());
    }
}

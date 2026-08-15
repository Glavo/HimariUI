package org.glavo.himari.runtime.effect;

import org.glavo.himari.state.IntState;
import org.glavo.himari.state.StateDomain;
import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies post-commit keyed effect mount, dependency-keyed update, and cleanup.
@NotNullByDefault
final class EffectHostTest {
    /// Verifies mount, update-once, and cleanup order for a changing dependency.
    @Test
    void mountsUpdatesAndCleansUpOncePerApply() {
        StateDomain domain = new StateDomain();
        List<String> log = new ArrayList<>();
        EffectKey key = new EffectKey("root", "load");
        try (EffectHost host = new EffectHost(domain)) {
            host.declare(key, EffectDependencies.of("a"), callbacks(log, "a"));
            EffectApplyResult first = host.apply();
            assertEquals(EffectApplyStatus.APPLIED, first.status());
            assertEquals(1, first.mountedCount());
            assertEquals(EffectApplyStatus.ALREADY_APPLIED, host.apply().status());

            host.declare(key, EffectDependencies.of("b"), callbacks(log, "b"));
            EffectApplyResult updated = host.apply();
            assertEquals(1, updated.updatedCount());
            assertEquals(0, updated.mountedCount());

            IntState epochAdvance = domain.intState(0);
            epochAdvance.set(1);
            EffectApplyResult cleaned = host.apply();
            assertEquals(1, cleaned.cleanedCount());
            assertEquals(List.of("mount:a", "update:b", "cleanup"), log);
        }
    }

    /// Verifies that asynchronous work is cancelled when the effect is cleaned up.
    @Test
    void cancelsAsyncWorkOnCleanup() throws InterruptedException {
        StateDomain domain = new StateDomain();
        CountDownLatch started = new CountDownLatch(1);
        AtomicInteger completions = new AtomicInteger();
        EffectKey key = new EffectKey("root", "async");
        try (EffectHost host = new EffectHost(domain)) {
            host.declare(key, EffectDependencies.NONE, new EffectCallbacks() {
                @Override
                public void onMount(EffectSession session) {
                    session.launch(() -> {
                        started.countDown();
                        try {
                            Thread.sleep(10_000L);
                            completions.incrementAndGet();
                        } catch (InterruptedException interrupted) {
                            Thread.currentThread().interrupt();
                        }
                    });
                }

                @Override
                public void onUpdate(EffectSession session) {
                }

                @Override
                public void onCleanup() {
                }
            });
            host.apply();
            assertTrue(started.await(2L, TimeUnit.SECONDS));
        }
        assertEquals(0, completions.get());
    }

    /// Creates callbacks that record lifecycle identity.
    ///
    /// @param log the destination log
    /// @param token the current dependency token
    /// @return the callbacks
    private static EffectCallbacks callbacks(List<String> log, String token) {
        return new EffectCallbacks() {
            @Override
            public void onMount(EffectSession session) {
                log.add("mount:" + token);
            }

            @Override
            public void onUpdate(EffectSession session) {
                log.add("update:" + token);
            }

            @Override
            public void onCleanup() {
                log.add("cleanup");
            }
        };
    }
}

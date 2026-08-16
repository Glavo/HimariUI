package org.glavo.himari.runtime.reload;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies Headless reload generations through [`ReloadCoordinator`].
@NotNullByDefault
final class ReloadCoordinatorTest {
    /// Keeps the counter, installs a new callback, and restarts effects.
    @Test
    void compatibleGenerationRetainsKeyedState() {
        ReloadCoordinator coordinator = new ReloadCoordinator();
        coordinator.retain("count", 2);
        AtomicInteger callbackGeneration = new AtomicInteger();
        AtomicInteger effects = new AtomicInteger();
        ReloadOutcome outcome = coordinator.applyCompatible(callbackGeneration::set, effects::incrementAndGet);
        assertEquals(1, outcome.generation());
        assertTrue(outcome.stateRetained());
        assertTrue(outcome.callbackReplaced());
        assertTrue(outcome.effectRestarted());
        assertEquals(ReloadFallback.NONE, outcome.fallback());
        assertEquals(2, coordinator.retained("count"));
        assertEquals(1, callbackGeneration.get());
        assertEquals(1, effects.get());
    }

    /// Leaves generation and state unchanged when verification fails.
    @Test
    void rejectedGenerationDoesNotPublish() {
        ReloadCoordinator coordinator = new ReloadCoordinator();
        coordinator.retain("count", 2);
        ReloadOutcome outcome = coordinator.rejectUnverified();
        assertTrue(outcome.failed());
        assertEquals(0, outcome.generation());
        assertEquals(2, coordinator.retained("count"));
        assertNull(coordinator.callback());
    }

    /// Drops keyed state on subtree reset.
    @Test
    void subtreeResetDropsRetainedState() {
        ReloadCoordinator coordinator = new ReloadCoordinator();
        coordinator.retain("count", 2);
        coordinator.applyCompatible(generation -> {
        }, () -> {
        });
        ReloadOutcome outcome = coordinator.applyIncompatible(ReloadFallback.SUBTREE_RESET);
        assertEquals(2, outcome.generation());
        assertFalse(outcome.stateRetained());
        assertEquals(ReloadFallback.SUBTREE_RESET, outcome.fallback());
        assertNull(coordinator.retained("count"));
    }
}

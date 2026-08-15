package org.glavo.himari.spikes.ffi.ffm;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies the local fixed-signature FFM scenarios against the host C runtime.
@NotNullByDefault
final class FfiFfmScenariosTest {
    /// Executes every profile fixture with a short repetition count.
    @Test
    void executesCompleteFixtureSet() {
        FfiFfmScenarios.Summary summary = FfiFfmScenarios.run(10, Duration.ZERO);

        assertEquals(10, summary.repetitions());
        assertTrue(summary.callbackInvocations() > 0);
        assertEquals(summary.callbackInvocations() * 2, summary.reentrantDowncalls());
        assertTrue(summary.threadConfined());
        assertTrue(summary.callbackArenaLifetimeRejected());
        assertTrue(summary.exceptionContained());
    }
}

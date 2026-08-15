package org.glavo.himari.spikes.runtime.sample;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/// Verifies balanced identity instrumentation, window resets, trace scoring, and confinement.
@NotNullByDefault
final class ComparisonProbeTest {
    /// Verifies counters, active registrations, reset baselines, and balanced cleanup.
    @Test
    void recordsAndBalancesRuntimeOwnership() {
        ComparisonProbe probe = new ComparisonProbe();
        Object edge = new Object();
        Object retained = new Object();

        probe.callbackExecuted(RuntimeCallbackKind.STRUCTURE);
        probe.nodesVisited(3L);
        probe.dependencyAttached(edge);
        probe.retained(retained, 48L);
        probe.phaseInvalidated(RuntimePhase.MEASURE);
        ProbeMetrics first = probe.metrics();
        assertEquals(1L, first.callbacksExecuted().get("structure"));
        assertEquals(3L, first.nodesVisited());
        assertEquals(1L, first.activeDependencyEdges());
        assertEquals(48L, first.retainedBytes());

        probe.resetMeasurementWindow();
        ProbeMetrics reset = probe.metrics();
        assertEquals(0L, reset.callbacksExecuted().get("structure"));
        assertEquals(1L, reset.peakDependencyEdges());
        assertEquals(48L, reset.peakRetainedBytes());

        probe.dependencyDetached(edge);
        probe.released(retained);
        ProbeMetrics closed = probe.metrics();
        assertEquals(0L, closed.activeDependencyEdges());
        assertEquals(0L, closed.retainedBytes());
        assertThrows(IllegalStateException.class, () -> probe.dependencyDetached(edge));
        assertThrows(IllegalStateException.class, () -> probe.released(retained));
    }

    /// Verifies the frozen zero-to-four trace-quality scale.
    @Test
    void scoresStructuredTraceQuality() {
        assertEquals(1, new DiagnosticTrace("failure", "message", null, null, null, null).qualityScore());
        assertEquals(2, new DiagnosticTrace("failure", "message", "App.java:1", null, null, null).qualityScore());
        assertEquals(3, new DiagnosticTrace(
                "failure", "message", "App.java:1", "root/item:a", null, null
        ).qualityScore());
        assertEquals(4, new DiagnosticTrace(
                "failure", "message", "App.java:1", "root/item:a", "state->item", null
        ).qualityScore());
    }

    /// Verifies that candidate instrumentation cannot mutate the probe from another thread.
    ///
    /// @throws InterruptedException if the test thread is interrupted while joining the worker
    @Test
    void confinesMutationToOwnerThread() throws InterruptedException {
        ComparisonProbe probe = new ComparisonProbe();
        AtomicReference<@Nullable Throwable> failure = new AtomicReference<>();
        Thread worker = Thread.ofPlatform().start(() -> {
            try {
                probe.nodesVisited(1L);
            } catch (Throwable thrown) {
                failure.set(thrown);
            }
        });
        worker.join();

        assertInstanceOf(IllegalStateException.class, failure.get());
    }
}

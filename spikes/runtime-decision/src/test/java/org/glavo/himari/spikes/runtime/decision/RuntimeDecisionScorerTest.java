package org.glavo.himari.spikes.runtime.decision;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/// Verifies the frozen normalization arithmetic independently of candidate measurements.
@NotNullByDefault
final class RuntimeDecisionScorerTest {
    /// Verifies zero handling and ordinary best-over-value normalization.
    @Test
    void normalizesLowerIsBetterMetrics() {
        assertEquals(RuntimeDecisionScorer.MAX_SCORE, RuntimeDecisionScorer.lowerIsBetterScore(0L, 0L));
        assertEquals(20_000_000L, RuntimeDecisionScorer.lowerIsBetterScore(4L, 0L));
        assertEquals(50_000_000L, RuntimeDecisionScorer.lowerIsBetterScore(10L, 5L));
        assertThrows(
                IllegalArgumentException.class,
                () -> RuntimeDecisionScorer.lowerIsBetterScore(4L, 5L)
        );
    }

    /// Verifies geometric aggregation and its non-compensatory zero behavior.
    @Test
    void aggregatesMetricsGeometrically() {
        assertEquals(
                50_000_000L,
                RuntimeDecisionScorer.geometricMean(List.of(100_000_000L, 25_000_000L))
        );
        assertEquals(0L, RuntimeDecisionScorer.geometricMean(List.of(100_000_000L, 0L)));
        assertThrows(IllegalArgumentException.class, () -> RuntimeDecisionScorer.geometricMean(List.of()));
    }
}

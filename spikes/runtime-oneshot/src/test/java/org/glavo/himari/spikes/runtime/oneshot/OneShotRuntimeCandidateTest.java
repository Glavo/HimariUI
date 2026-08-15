package org.glavo.himari.spikes.runtime.oneshot;

import org.glavo.himari.spikes.runtime.sample.ComparisonReport;
import org.glavo.himari.spikes.runtime.sample.ComparisonStatus;
import org.glavo.himari.spikes.runtime.sample.FixtureStatus;
import org.glavo.himari.spikes.runtime.sample.RuntimeComparisonRunner;
import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies the complete one-shot candidate against the frozen neutral suite.
@NotNullByDefault
final class OneShotRuntimeCandidateTest {
    /// Verifies all correctness gates, cleanup, and deliberately incomplete external evidence.
    @Test
    void passesEveryFrozenFixtureWithoutClaimingExternalEvidence() {
        ComparisonReport report = RuntimeComparisonRunner.run(new OneShotRuntimeCandidate(repositoryRoot()));

        assertEquals(ComparisonStatus.INCOMPLETE, report.status(), report.toString());
        assertEquals(13, report.fixtures().size());
        assertTrue(report.fixtures().stream().allMatch(result -> result.status() == FixtureStatus.PASSED));
        assertTrue(report.disqualifications().isEmpty());
        assertTrue(report.sourceMetrics().sourceLines() > 0L);
        assertTrue(report.sourceMetrics().ceremonyCounts().get("deferredGetters") > 0L);
        assertTrue(report.sourceMetrics().ceremonyCounts().get("structuralControls") > 0L);
    }

    /// Returns the repository root injected by Gradle.
    ///
    /// @return the absolute repository root
    private static Path repositoryRoot() {
        return Path.of(Objects.requireNonNull(System.getProperty("himari.repository.root"))).toAbsolutePath();
    }
}

package org.glavo.himari.spikes.runtime.sample;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies complete harness replay, canonical reports, disqualification, and early stopping.
@NotNullByDefault
final class RuntimeComparisonRunnerTest {
    /// Verifies that the ineligible protocol adapter exercises every fixture and report field.
    @Test
    void runsCompleteSelfTest() {
        ComparisonReport report = RuntimeComparisonRunner.run(new ReferenceProtocolCandidate(repositoryRoot()));

        assertEquals(ComparisonStatus.SELF_TEST_PASSED, report.status());
        assertEquals(13, report.fixtures().size());
        assertTrue(report.fixtures().stream().allMatch(result -> result.status() == FixtureStatus.PASSED));
        assertTrue(report.disqualifications().isEmpty());
        String json = ComparisonJson.report(report);
        assertEquals(json, ComparisonJson.report(report));
        assertTrue(json.contains("\"steadyStateAllocation\""));
        assertTrue(json.contains("\"maximumTraceQuality\": 4"));
    }

    /// Verifies that an oracle mismatch disqualifies the candidate and skips later checkpoints.
    @Test
    void retainsPartialEvidenceAfterMicroFailure() {
        RuntimeCandidate candidate = new CorruptingCandidate(new ReferenceProtocolCandidate(repositoryRoot()));

        ComparisonReport report = RuntimeComparisonRunner.run(candidate);

        assertEquals(ComparisonStatus.DISQUALIFIED, report.status());
        assertEquals(FixtureStatus.FAILED, report.fixtures().getFirst().status());
        assertEquals(5, report.fixtures().stream()
                .filter(result -> result.stage() == FixtureStage.MICRO)
                .filter(result -> result.status() == FixtureStatus.PASSED)
                .count());
        assertEquals(7, report.fixtures().stream()
                .filter(result -> result.status() == FixtureStatus.SKIPPED)
                .count());
        assertEquals("fixture-failed", report.disqualifications().getFirst().code());
    }

    /// Returns the repository root injected by the module build.
    ///
    /// @return the absolute repository root
    private static Path repositoryRoot() {
        return Path.of(Objects.requireNonNull(System.getProperty("himari.repository.root"))).toAbsolutePath();
    }

    /// Delegates every operation except the first mounted observation, which is deliberately corrupt.
    @NotNullByDefault
    private static final class CorruptingCandidate implements RuntimeCandidate {
        /// The valid protocol adapter.
        private final RuntimeCandidate delegate;

        /// Creates a corrupting wrapper.
        ///
        /// @param delegate the valid delegate
        private CorruptingCandidate(RuntimeCandidate delegate) {
            this.delegate = delegate;
        }

        /// Returns the delegate descriptor.
        ///
        /// @return the descriptor
        @Override
        public CandidateDescriptor descriptor() {
            return delegate.descriptor();
        }

        /// Returns the delegate evidence.
        ///
        /// @return the evidence
        @Override
        public CandidateEvidence evidence() {
            return delegate.evidence();
        }

        /// Returns the delegate source corpus.
        ///
        /// @return the source corpus
        @Override
        public SourceCorpus sourceCorpus() {
            return delegate.sourceCorpus();
        }

        /// Opens a session and corrupts only the first fixture's first observation.
        ///
        /// @param fixture the fixture
        /// @param environment the environment
        /// @param probe the probe
        /// @return the wrapped or unmodified session
        @Override
        public RuntimeFixtureSession open(
                FixtureDefinition fixture,
                ComparisonEnvironment environment,
                ComparisonProbe probe
        ) {
            RuntimeFixtureSession session = delegate.open(fixture, environment, probe);
            return fixture.id().equals("counter-derived-handler") ? new CorruptingSession(session) : session;
        }
    }

    /// Replaces the first observation with a stable mismatch and otherwise delegates exactly.
    @NotNullByDefault
    private static final class CorruptingSession implements RuntimeFixtureSession {
        /// The valid session.
        private final RuntimeFixtureSession delegate;

        /// Whether the corrupt observation has been returned.
        private boolean corrupted;

        /// Creates a corrupting session.
        ///
        /// @param delegate the valid session
        private CorruptingSession(RuntimeFixtureSession delegate) {
            this.delegate = delegate;
        }

        /// Delegates command execution.
        ///
        /// @param command the command
        @Override
        public void execute(FixtureCommand command) {
            delegate.execute(command);
        }

        /// Returns one corrupt observation, then delegates.
        ///
        /// @return the observation
        @Override
        public FixtureObservation observation() {
            FixtureObservation actual = delegate.observation();
            if (!corrupted) {
                corrupted = true;
                return new FixtureObservation(
                        java.util.Map.of("corrupt", "true"),
                        List.of("corrupt"),
                        List.of(),
                        List.of()
                );
            }
            return actual;
        }

        /// Delegates benchmark entry.
        @Override
        public void beginBenchmark() {
            delegate.beginBenchmark();
        }

        /// Returns delegate health.
        ///
        /// @return the health snapshot
        @Override
        public RuntimeHealth health() {
            return delegate.health();
        }

        /// Closes the delegate.
        @Override
        public void close() {
            delegate.close();
        }
    }
}

package org.glavo.himari.spikes.runtime.sample;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/// Replays the checked-in oracle solely to exercise the comparison harness and canonical report path.
///
/// This adapter is deliberately marked ineligible and is not a structural-runtime prototype or
/// decision baseline. It implements no reconciliation algorithm and must never be included in model
/// scoring.
@NotNullByDefault
final class ReferenceProtocolCandidate implements RuntimeCandidate {
    /// The source path measured by the harness self-test.
    private static final String SOURCE_PATH =
            "spikes/runtime-sample/src/main/java/org/glavo/himari/spikes/runtime/sample/ReferenceProtocolCandidate.java";

    /// Capabilities chosen to exercise measure-time and preemptive conditional steps.
    private static final CandidateCapabilities CAPABILITIES = new CandidateCapabilities(
            MeasureMaterializationMode.SCOPED_MEASURE_TIME,
            CancellationSupport.PREEMPTIVE,
            false
    );

    /// The repository root used for source analysis.
    private final Path repositoryRoot;

    /// Creates the ineligible self-test adapter.
    ///
    /// @param repositoryRoot the repository root
    ReferenceProtocolCandidate(Path repositoryRoot) {
        this.repositoryRoot = repositoryRoot.toAbsolutePath().normalize();
    }

    /// Returns the ineligible harness descriptor.
    ///
    /// @return the descriptor
    @Override
    public CandidateDescriptor descriptor() {
        return new CandidateDescriptor(
                "harness-oracle-replay",
                "Harness oracle replay",
                "No structural model; exact protocol replay for harness validation only",
                CAPABILITIES,
                false,
                false
        );
    }

    /// Returns evidence statuses that do not apply to an ineligible harness adapter.
    ///
    /// @return the evidence snapshot
    @Override
    public CandidateEvidence evidence() {
        return new CandidateEvidence(
                EvidenceStatus.NOT_APPLICABLE,
                EvidenceStatus.NOT_APPLICABLE,
                EvidenceStatus.NOT_APPLICABLE,
                Map.of()
        );
    }

    /// Returns the single self-test source file with no claimed candidate ceremony.
    ///
    /// @return the source corpus
    @Override
    public SourceCorpus sourceCorpus() {
        return new SourceCorpus(
                repositoryRoot,
                List.of(new SourceUnit(SOURCE_PATH, FixtureStage.MICRO)),
                List.of()
        );
    }

    /// Opens one exact replay session.
    ///
    /// @param fixture the fixture
    /// @param environment the unused shared environment
    /// @param probe the shared probe
    /// @return the replay session
    @Override
    public RuntimeFixtureSession open(
            FixtureDefinition fixture,
            ComparisonEnvironment environment,
            ComparisonProbe probe
    ) {
        Objects.requireNonNull(environment, "environment");
        return new ReplaySession(fixture, probe);
    }

    /// Replays applicable steps and emits representative instrumentation.
    @NotNullByDefault
    private static final class ReplaySession implements RuntimeFixtureSession {
        /// The applicable correctness steps.
        private final List<FixtureStep> steps;

        /// The shared instrumentation sink.
        private final ComparisonProbe probe;

        /// Stable dependency-edge identity registered while mounted.
        private final Object edgeToken = new Object();

        /// Stable retained-object identity registered while mounted.
        private final Object retainedToken = new Object();

        /// The latest oracle observation, or `null` before mount.
        private @Nullable FixtureObservation observation;

        /// The next correctness-step index.
        private int nextStep;

        /// Whether benchmark commands should leave the mounted oracle unchanged.
        private boolean benchmark;

        /// Whether mount registered live resources.
        private boolean mounted;

        /// Whether closure completed.
        private boolean closed;

        /// Creates a replay session for applicable capability paths.
        ///
        /// @param fixture the fixture
        /// @param probe the probe
        private ReplaySession(FixtureDefinition fixture, ComparisonProbe probe) {
            this.steps = fixture.steps().stream().filter(step -> step.appliesTo(CAPABILITIES)).toList();
            this.probe = Objects.requireNonNull(probe, "probe");
        }

        /// Replays the next correctness oracle or one state-restoring benchmark command.
        ///
        /// @param command the command
        @Override
        public void execute(FixtureCommand command) {
            checkOpen();
            Objects.requireNonNull(command, "command");
            probe.callbackExecuted(benchmark ? RuntimeCallbackKind.BINDING : RuntimeCallbackKind.EVENT);
            probe.nodesVisited(1L);
            if (benchmark) {
                probe.phaseInvalidated(RuntimePhase.PAINT);
                return;
            }
            if (nextStep >= steps.size()) {
                throw new IllegalArgumentException("oracle replay received an extra command " + command.operation());
            }
            FixtureStep step = steps.get(nextStep);
            if (!step.command().equals(command)) {
                throw new IllegalArgumentException(
                        "oracle replay expected " + step.command() + " but received " + command
                );
            }
            nextStep++;
            if (!mounted) {
                if (!command.operation().equals("mount")) {
                    throw new IllegalStateException("first replay command must mount");
                }
                mounted = true;
                probe.dependencyAttached(edgeToken);
                probe.retained(retainedToken, 64L);
                probe.callbackExecuted(RuntimeCallbackKind.STRUCTURE);
            }
            for (RuntimePhase phase : step.phases().required()) {
                probe.phaseInvalidated(phase);
            }
            for (String diagnostic : step.expected().diagnostics()) {
                probe.trace(new DiagnosticTrace(
                        diagnostic,
                        "Harness replay emitted " + diagnostic,
                        SOURCE_PATH + ":1",
                        "fixture/" + step.id(),
                        "source->consumer",
                        "rollback-and-preserve-commit"
                ));
            }
            observation = step.expected();
        }

        /// Returns the latest replayed observation.
        ///
        /// @return the observation
        @Override
        public FixtureObservation observation() {
            checkOpen();
            @Nullable FixtureObservation current = observation;
            if (current == null) {
                throw new IllegalStateException("fixture has not mounted");
            }
            return current;
        }

        /// Enters a state-preserving benchmark path.
        @Override
        public void beginBenchmark() {
            checkOpen();
            if (!mounted || nextStep != 1) {
                throw new IllegalStateException("benchmark must begin immediately after mount");
            }
            benchmark = true;
            observation = steps.getFirst().expected();
        }

        /// Returns live resources before close and a clean snapshot afterwards.
        ///
        /// @return the health snapshot
        @Override
        public RuntimeHealth health() {
            return closed || !mounted
                    ? RuntimeHealth.CLEAN
                    : new RuntimeHealth(1L, 1L, 0L, 0L, 0L);
        }

        /// Releases replay-owned probe registrations.
        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;
            if (mounted) {
                probe.callbackExecuted(RuntimeCallbackKind.CLEANUP);
                probe.dependencyDetached(edgeToken);
                probe.released(retainedToken);
                mounted = false;
            }
        }

        /// Verifies that the replay session is open.
        ///
        /// @throws IllegalStateException after closure
        private void checkOpen() {
            if (closed) {
                throw new IllegalStateException("replay session is closed");
            }
        }
    }
}

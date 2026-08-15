package org.glavo.himari.spikes.runtime.sample;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/// Executes one candidate against the frozen fixtures without exposing candidate structure to the oracle.
@NotNullByDefault
public final class RuntimeComparisonRunner {
    /// The current report schema version.
    private static final int REPORT_SCHEMA_VERSION = 1;

    /// Prevents construction.
    private RuntimeComparisonRunner() {
    }

    /// Runs source analysis, correctness checkpoints, steady-state instrumentation, and cleanup gates.
    ///
    /// A failure in a micro-fixture skips integration and realistic fixtures. A failure in an
    /// integration fixture skips the realistic fixture. Partial results and the exact early-stop
    /// reason remain in the report.
    ///
    /// @param candidate the candidate adapter
    /// @return the complete immutable report
    public static ComparisonReport run(RuntimeCandidate candidate) {
        Objects.requireNonNull(candidate, "candidate");
        DecisionRubric.validate();
        CandidateDescriptor descriptor = Objects.requireNonNull(candidate.descriptor(), "candidate descriptor");
        CandidateEvidence evidence = Objects.requireNonNull(candidate.evidence(), "candidate evidence");
        ArrayList<Disqualification> disqualifications = new ArrayList<>();

        SourceMetrics sourceMetrics;
        boolean sourceValid = true;
        try {
            sourceMetrics = SourceMetricsAnalyzer.analyze(
                    Objects.requireNonNull(candidate.sourceCorpus(), "candidate source corpus")
            );
        } catch (RuntimeException failure) {
            sourceValid = false;
            sourceMetrics = emptySourceMetrics();
            disqualifications.add(new Disqualification(
                    "source-corpus-invalid",
                    null,
                    failureDetail(failure)
            ));
        }
        if (descriptor.applicationCodeTransformed()) {
            disqualifications.add(new Disqualification(
                    "application-code-transformed",
                    null,
                    "Candidate declares generated or transformed application code"
            ));
        }

        ArrayList<FixtureResult> results = new ArrayList<>();
        boolean microFailed = !sourceValid || descriptor.applicationCodeTransformed();
        boolean integrationFailed = false;
        for (FixtureDefinition fixture : FixtureCatalog.fixtures()) {
            if (fixture.stage() == FixtureStage.INTEGRATION && microFailed) {
                results.add(FixtureResult.skipped(fixture, "micro checkpoint did not pass"));
                integrationFailed = true;
                continue;
            }
            if (fixture.stage() == FixtureStage.REALISTIC && (microFailed || integrationFailed)) {
                results.add(FixtureResult.skipped(fixture, "integration checkpoint did not pass"));
                continue;
            }
            if (!sourceValid || descriptor.applicationCodeTransformed()) {
                results.add(FixtureResult.skipped(fixture, "compile and source-evidence gate did not pass"));
                continue;
            }

            FixtureResult result = runFixture(candidate, descriptor, fixture);
            results.add(result);
            if (result.status() == FixtureStatus.FAILED) {
                disqualifications.add(new Disqualification(
                        "fixture-failed",
                        fixture.id(),
                        String.join(" | ", result.failures())
                ));
                if (fixture.stage() == FixtureStage.MICRO) {
                    microFailed = true;
                } else if (fixture.stage() == FixtureStage.INTEGRATION) {
                    integrationFailed = true;
                }
            }
        }

        ComparisonStatus status = status(descriptor, evidence, results, disqualifications);
        return new ComparisonReport(
                REPORT_SCHEMA_VERSION,
                FixtureCatalog.VERSION,
                DecisionRubric.VERSION,
                status,
                descriptor,
                evidence,
                ComparisonEnvironmentRecord.current(),
                sourceMetrics,
                results,
                disqualifications
        );
    }

    /// Runs one correctness session and optional independent benchmark session.
    ///
    /// @param candidate the candidate
    /// @param descriptor the candidate descriptor
    /// @param fixture the fixture
    /// @return the fixture result
    private static FixtureResult runFixture(
            RuntimeCandidate candidate,
            CandidateDescriptor descriptor,
            FixtureDefinition fixture
    ) {
        ScenarioOutcome scenario = runScenario(candidate, descriptor, fixture);
        ArrayList<String> failures = new ArrayList<>(scenario.failures());
        @Nullable BenchmarkMetrics benchmark = null;
        @Nullable BenchmarkPlan plan = fixture.benchmark();
        if (failures.isEmpty() && plan != null) {
            BenchmarkOutcome outcome = runBenchmark(candidate, descriptor, fixture, plan);
            benchmark = outcome.metrics();
            failures.addAll(outcome.failures());
        }
        FixtureStatus status = failures.isEmpty() ? FixtureStatus.PASSED : FixtureStatus.FAILED;
        return new FixtureResult(
                fixture.id(),
                fixture.stage(),
                status,
                scenario.steps(),
                scenario.probe(),
                benchmark,
                scenario.health(),
                failures
        );
    }

    /// Runs every applicable correctness step and enforces post-close cleanup.
    ///
    /// @param candidate the candidate
    /// @param descriptor the descriptor
    /// @param fixture the fixture
    /// @return the scenario outcome
    private static ScenarioOutcome runScenario(
            RuntimeCandidate candidate,
            CandidateDescriptor descriptor,
            FixtureDefinition fixture
    ) {
        ComparisonProbe probe = new ComparisonProbe();
        ThreadAllocationMeter allocationMeter = new ThreadAllocationMeter();
        ArrayList<StepResult> steps = new ArrayList<>();
        ArrayList<String> failures = new ArrayList<>();
        RuntimeHealth health = RuntimeHealth.CLEAN;
        @Nullable RuntimeFixtureSession session = null;

        try (ComparisonEnvironment environment = ComparisonEnvironment.open()) {
            try {
                session = Objects.requireNonNull(
                        candidate.open(fixture, environment, probe),
                        "candidate fixture session"
                );
                for (FixtureStep step : fixture.steps()) {
                    if (!step.appliesTo(descriptor.capabilities())) {
                        continue;
                    }
                    StepResult result = runStep(session, environment, probe, allocationMeter, step);
                    steps.add(result);
                    if (!result.passed()) {
                        failures.add("step " + step.id() + ": " + result.failure());
                        break;
                    }
                }
            } catch (RuntimeException | AssertionError failure) {
                failures.add("scenario setup: " + failureDetail(failure));
            } finally {
                if (session != null) {
                    health = closeAndInspect(session, environment, probe, failures, "scenario");
                }
            }
        } catch (RuntimeException | AssertionError failure) {
            failures.add("scenario environment: " + failureDetail(failure));
        }

        ProbeMetrics metrics = probe.metrics();
        if (metrics.activeDependencyEdges() != 0L) {
            failures.add("scenario cleanup retained " + metrics.activeDependencyEdges() + " dependency edges");
        }
        if (metrics.retainedBytes() != 0L) {
            failures.add("scenario cleanup retained " + metrics.retainedBytes() + " registered bytes");
        }
        return new ScenarioOutcome(steps, metrics, health, failures);
    }

    /// Executes one command, Headless drain, observation, phase check, and diagnostic check.
    ///
    /// @param session the candidate session
    /// @param environment the fixture environment
    /// @param probe the instrumentation probe
    /// @param allocationMeter the current-thread allocation meter
    /// @param step the shared step
    /// @return the step result
    private static StepResult runStep(
            RuntimeFixtureSession session,
            ComparisonEnvironment environment,
            ComparisonProbe probe,
            ThreadAllocationMeter allocationMeter,
            FixtureStep step
    ) {
        ProbeMetrics before = probe.metrics();
        long allocationStart = allocationMeter.start();
        @Nullable FixtureObservation actual = null;
        @Nullable String failure = null;
        try {
            session.execute(step.command());
            environment.drain();
            actual = Objects.requireNonNull(session.observation(), "candidate observation");
            if (!step.expected().equals(actual)) {
                failure = "observation mismatch; expected=" + step.expected() + ", actual=" + actual;
            }
        } catch (RuntimeException | AssertionError thrown) {
            failure = "execution failed: " + failureDetail(thrown);
        }
        AllocationMeasurement allocation = allocationMeter.finish(allocationStart);
        ProbeMetrics after = probe.metrics();
        StepMetrics metrics = stepMetrics(before, after, allocation);

        if (failure == null) {
            for (RuntimePhase phase : step.phases().required()) {
                if (after.phaseDeltaFrom(before, phase) == 0L) {
                    failure = "required phase was not invalidated: " + phase;
                    break;
                }
            }
        }
        if (failure == null) {
            for (String diagnostic : step.expected().diagnostics()) {
                boolean traced = metrics.traces().stream().anyMatch(trace -> trace.code().equals(diagnostic));
                if (!traced) {
                    failure = "required diagnostic trace was not emitted: " + diagnostic;
                    break;
                }
            }
        }
        return new StepResult(step.id(), failure == null, actual, metrics, failure);
    }

    /// Runs one fixed steady-state cycle in a fresh mounted session.
    ///
    /// @param candidate the candidate
    /// @param descriptor the descriptor
    /// @param fixture the fixture
    /// @param plan the benchmark plan
    /// @return the benchmark outcome
    private static BenchmarkOutcome runBenchmark(
            RuntimeCandidate candidate,
            CandidateDescriptor descriptor,
            FixtureDefinition fixture,
            BenchmarkPlan plan
    ) {
        ComparisonProbe probe = new ComparisonProbe();
        ThreadAllocationMeter allocationMeter = new ThreadAllocationMeter();
        ArrayList<String> failures = new ArrayList<>();
        @Nullable BenchmarkMetrics metrics = null;
        @Nullable RuntimeFixtureSession session = null;

        try (ComparisonEnvironment environment = ComparisonEnvironment.open()) {
            try {
                session = Objects.requireNonNull(
                        candidate.open(fixture, environment, probe),
                        "candidate fixture session"
                );
                FixtureStep mount = firstApplicableStep(fixture, descriptor.capabilities());
                session.execute(mount.command());
                environment.drain();
                if (!mount.expected().equals(session.observation())) {
                    throw new IllegalStateException("benchmark mount does not match the initial oracle");
                }
                session.beginBenchmark();
                environment.drain();
                if (!mount.expected().equals(session.observation())) {
                    throw new IllegalStateException("beginBenchmark did not restore the mounted initial state");
                }
                executeCycles(session, environment, plan.cycle(), plan.warmupIterations());
                probe.resetMeasurementWindow();

                long allocationStart = allocationMeter.start();
                long started = System.nanoTime();
                executeCycles(session, environment, plan.cycle(), plan.measuredIterations());
                long elapsed = elapsedNanos(started, System.nanoTime());
                AllocationMeasurement allocation = allocationMeter.finish(allocationStart);
                if (!mount.expected().equals(session.observation())) {
                    throw new IllegalStateException("benchmark cycle did not restore the mounted initial state");
                }
                metrics = new BenchmarkMetrics(
                        plan.warmupIterations(),
                        plan.measuredIterations(),
                        plan.cycle().size(),
                        elapsed,
                        allocation,
                        probe.metrics()
                );
            } catch (RuntimeException | AssertionError failure) {
                failures.add("benchmark execution: " + failureDetail(failure));
            } finally {
                if (session != null) {
                    closeAndInspect(session, environment, probe, failures, "benchmark");
                }
            }
        } catch (RuntimeException | AssertionError failure) {
            failures.add("benchmark environment: " + failureDetail(failure));
        }
        return new BenchmarkOutcome(metrics, failures);
    }

    /// Executes a benchmark command cycle repeatedly.
    ///
    /// @param session the candidate session
    /// @param environment the fixture environment
    /// @param cycle the commands in one cycle
    /// @param iterations the cycle count
    private static void executeCycles(
            RuntimeFixtureSession session,
            ComparisonEnvironment environment,
            @Unmodifiable List<FixtureCommand> cycle,
            int iterations
    ) {
        for (int iteration = 0; iteration < iterations; iteration++) {
            for (int commandIndex = 0; commandIndex < cycle.size(); commandIndex++) {
                FixtureCommand command = cycle.get(commandIndex);
                session.execute(command);
                environment.drain();
            }
        }
    }

    /// Returns the first applicable step, which the fixture contract requires to be `mount`.
    ///
    /// @param fixture the fixture
    /// @param capabilities the candidate capabilities
    /// @return the mount step
    private static FixtureStep firstApplicableStep(
            FixtureDefinition fixture,
            CandidateCapabilities capabilities
    ) {
        for (FixtureStep step : fixture.steps()) {
            if (step.appliesTo(capabilities)) {
                if (!step.command().operation().equals("mount")) {
                    throw new IllegalStateException("first applicable fixture step is not mount");
                }
                return step;
            }
        }
        throw new IllegalStateException("fixture has no applicable mount step");
    }

    /// Closes a candidate session, drains cleanup, and records health and queue leaks.
    ///
    /// @param session the session
    /// @param environment the environment
    /// @param probe the probe
    /// @param failures mutable failure collection
    /// @param context the diagnostic context
    /// @return the post-close health snapshot
    private static RuntimeHealth closeAndInspect(
            RuntimeFixtureSession session,
            ComparisonEnvironment environment,
            ComparisonProbe probe,
            List<String> failures,
            String context
    ) {
        RuntimeHealth health = RuntimeHealth.CLEAN;
        try {
            session.close();
            environment.drain();
            health = Objects.requireNonNull(session.health(), "candidate post-close health");
            if (!health.clean()) {
                failures.add(context + " cleanup health is not clean: " + health);
            }
            int pending = environment.platform().eventLoop().pendingTaskCount();
            if (pending != 0) {
                failures.add(context + " cleanup left " + pending + " Headless callbacks pending");
            }
            ProbeMetrics metrics = probe.metrics();
            if (metrics.activeDependencyEdges() != 0L) {
                failures.add(context + " cleanup left dependency edges active");
            }
            if (metrics.retainedBytes() != 0L) {
                failures.add(context + " cleanup left registered retained bytes");
            }
        } catch (RuntimeException | AssertionError failure) {
            failures.add(context + " cleanup failed: " + failureDetail(failure));
        }
        return health;
    }

    /// Computes a step-local delta from two probe snapshots.
    ///
    /// @param before the earlier snapshot
    /// @param after the later snapshot
    /// @param allocation the allocation measurement
    /// @return the step metrics
    private static StepMetrics stepMetrics(
            ProbeMetrics before,
            ProbeMetrics after,
            AllocationMeasurement allocation
    ) {
        Map<String, Long> callbackDelta = metricDelta(before.callbacksExecuted(), after.callbacksExecuted());
        Map<String, Long> phaseDelta = metricDelta(before.phaseInvalidations(), after.phaseInvalidations());
        if (after.traces().size() < before.traces().size()) {
            throw new IllegalArgumentException("probe traces moved backwards");
        }
        List<DiagnosticTrace> traces = after.traces().subList(before.traces().size(), after.traces().size());
        return new StepMetrics(
                callbackDelta,
                subtract(after.nodesVisited(), before.nodesVisited(), "nodesVisited"),
                subtract(after.dependencyEdgesAttached(), before.dependencyEdgesAttached(), "edgesAttached"),
                subtract(after.dependencyEdgesDetached(), before.dependencyEdgesDetached(), "edgesDetached"),
                after.activeDependencyEdges(),
                after.retainedBytes(),
                phaseDelta,
                allocation,
                traces
        );
    }

    /// Computes a nonnegative key-wise metric delta.
    ///
    /// @param before the earlier metrics
    /// @param after the later metrics
    /// @return the immutable delta map
    private static @Unmodifiable Map<String, Long> metricDelta(
            @Unmodifiable Map<String, Long> before,
            @Unmodifiable Map<String, Long> after
    ) {
        HashMap<String, Long> delta = new HashMap<>();
        for (Map.Entry<String, Long> entry : after.entrySet()) {
            delta.put(entry.getKey(), subtract(
                    entry.getValue(),
                    before.getOrDefault(entry.getKey(), 0L),
                    entry.getKey()
            ));
        }
        for (Map.Entry<String, Long> entry : before.entrySet()) {
            if (!after.containsKey(entry.getKey()) && entry.getValue() != 0L) {
                throw new IllegalArgumentException("probe metric disappeared: " + entry.getKey());
            }
        }
        return ComparisonContracts.immutableSortedMap(delta, "metric delta");
    }

    /// Subtracts ordered counters.
    ///
    /// @param after the later value
    /// @param before the earlier value
    /// @param name the diagnostic metric name
    /// @return the nonnegative delta
    private static long subtract(long after, long before, String name) {
        if (after < before) {
            throw new IllegalArgumentException("probe metric moved backwards: " + name);
        }
        return after - before;
    }

    /// Computes a nonnegative elapsed duration despite the theoretical `nanoTime` wrap boundary.
    ///
    /// @param started the start timestamp
    /// @param finished the finish timestamp
    /// @return the nonnegative elapsed duration
    private static long elapsedNanos(long started, long finished) {
        long elapsed = finished - started;
        return elapsed < 0L ? 0L : elapsed;
    }

    /// Selects the top-level report status.
    ///
    /// @param descriptor the candidate descriptor
    /// @param evidence the external evidence
    /// @param results the fixture results
    /// @param disqualifications the correctness failures
    /// @return the report status
    private static ComparisonStatus status(
            CandidateDescriptor descriptor,
            CandidateEvidence evidence,
            @Unmodifiable List<FixtureResult> results,
            @Unmodifiable List<Disqualification> disqualifications
    ) {
        if (!disqualifications.isEmpty()) {
            return ComparisonStatus.DISQUALIFIED;
        }
        if (!descriptor.comparisonEligible()) {
            return ComparisonStatus.SELF_TEST_PASSED;
        }
        boolean allocationUnavailable = results.stream()
                .map(FixtureResult::benchmark)
                .filter(Objects::nonNull)
                .anyMatch(benchmark -> !benchmark.allocation().available());
        if (evidence.nativeImage() != EvidenceStatus.PASSED
                || evidence.ceremonyReview() != EvidenceStatus.PASSED
                || allocationUnavailable
                || descriptor.capabilities().reloadIdentityClaimed()
                && evidence.reloadIdentity() != EvidenceStatus.PASSED) {
            return ComparisonStatus.INCOMPLETE;
        }
        return ComparisonStatus.PASSED;
    }

    /// Returns an empty source snapshot for a malformed corpus report.
    ///
    /// @return the empty snapshot
    private static SourceMetrics emptySourceMetrics() {
        HashMap<String, Long> counts = new HashMap<>();
        for (SourceCeremonyKind kind : SourceCeremonyKind.values()) {
            counts.put(switch (kind) {
                case EXPLICIT_KEY -> "explicitKeys";
                case DEFERRED_GETTER -> "deferredGetters";
                case STRUCTURAL_CONTROL -> "structuralControls";
                case GROUP_BOUNDARY -> "groupBoundaries";
                case GENERIC_TYPE_NOISE -> "genericTypeNoise";
                case CALLBACK_WRAPPER -> "callbackWrappers";
            }, 0L);
        }
        return new SourceMetrics(0L, List.of(), counts, List.of());
    }

    /// Produces deterministic exception detail without stack traces or machine-specific paths.
    ///
    /// @param failure the failure
    /// @return the class name and optional message
    private static String failureDetail(Throwable failure) {
        @Nullable String message = failure.getMessage();
        return failure.getClass().getName() + (message == null || message.isBlank() ? "" : ": " + message);
    }

    /// Captures a correctness-session result before conversion to the public fixture report.
    ///
    /// @param steps the step results
    /// @param probe the final probe metrics
    /// @param health the post-close health
    /// @param failures fixture-level failures
    @NotNullByDefault
    private record ScenarioOutcome(
            @Unmodifiable List<StepResult> steps,
            ProbeMetrics probe,
            RuntimeHealth health,
            @Unmodifiable List<String> failures
    ) {
        /// Creates an immutable scenario outcome.
        private ScenarioOutcome {
            steps = List.copyOf(steps);
            Objects.requireNonNull(probe, "probe");
            Objects.requireNonNull(health, "health");
            failures = List.copyOf(failures);
        }
    }

    /// Captures an optional benchmark result and any benchmark-specific failures.
    ///
    /// @param metrics the completed metrics, or `null` after failure
    /// @param failures benchmark failures
    @NotNullByDefault
    private record BenchmarkOutcome(
            @Nullable BenchmarkMetrics metrics,
            @Unmodifiable List<String> failures
    ) {
        /// Creates an immutable benchmark outcome.
        private BenchmarkOutcome {
            failures = List.copyOf(failures);
        }
    }
}

package org.glavo.himari.spikes.runtime.sample;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/// Serializes the frozen suite, rubric, and candidate reports into canonical JSON value trees.
@NotNullByDefault
public final class ComparisonJson {
    /// Prevents construction.
    private ComparisonJson() {
    }

    /// Encodes the complete fixture contract.
    ///
    /// @return canonical suite JSON
    public static String suite() {
        return CanonicalJson.write(map(
                "schemaVersion", 1,
                "suiteVersion", FixtureCatalog.VERSION,
                "fixtures", FixtureCatalog.fixtures().stream().map(ComparisonJson::fixtureDefinition).toList()
        ));
    }

    /// Encodes the frozen decision rubric.
    ///
    /// @return canonical rubric JSON
    public static String rubric() {
        DecisionRubric.validate();
        return CanonicalJson.write(map(
                "schemaVersion", 1,
                "rubricVersion", DecisionRubric.VERSION,
                "selectionOrder", List.of(
                        "correctness-disqualifiers",
                        "evidence-completeness",
                        "ordinary-java-ceremony-review",
                        "pareto-review",
                        "fixed-weight-score",
                        "tie-breakers"
                ),
                "lowerIsBetterNormalization",
                "equal zero values score 100; nonzero against zero best scores 100/(1+value); otherwise score 100*best/value",
                "withinDimensionAggregation", "geometric-mean",
                "crossDimensionAggregation", "fixed-weight-arithmetic-mean",
                "tieBreakers", List.of(
                        "lower accidental ceremony",
                        "narrower phase invalidation",
                        "lower peak retained memory",
                        "simpler documented structural semantics"
                ),
                "rules", DecisionRubric.rules().stream().map(ComparisonJson::rubricRule).toList(),
                "dimensions", DecisionRubric.dimensions().stream().map(ComparisonJson::rubricDimension).toList(),
                "checkpoints", DecisionRubric.checkpoints().stream().map(ComparisonJson::rubricCheckpoint).toList()
        ));
    }

    /// Encodes one candidate report conforming to `schema/runtime-comparison-report.schema.json`.
    ///
    /// @param report the report
    /// @return canonical report JSON
    public static String report(ComparisonReport report) {
        return CanonicalJson.write(map(
                "$schema", "schema/runtime-comparison-report.schema.json",
                "schemaVersion", report.schemaVersion(),
                "suiteVersion", report.suiteVersion(),
                "rubricVersion", report.rubricVersion(),
                "status", canonical(report.status()),
                "candidate", candidate(report.candidate()),
                "evidence", evidence(report.evidence()),
                "environment", environment(report.environment()),
                "sourceMetrics", sourceMetrics(report.sourceMetrics()),
                "fixtures", report.fixtures().stream().map(ComparisonJson::fixtureResult).toList(),
                "summary", summary(report.fixtures()),
                "disqualifications", report.disqualifications().stream()
                        .map(ComparisonJson::disqualification)
                        .toList()
        ));
    }

    /// Converts one fixture definition.
    ///
    /// @param fixture the fixture
    /// @return the JSON value
    private static @Unmodifiable Map<String, Object> fixtureDefinition(FixtureDefinition fixture) {
        LinkedHashMap<String, Object> value = mutableMap(
                "id", fixture.id(),
                "stage", canonical(fixture.stage()),
                "description", fixture.description(),
                "correctnessTags", fixture.correctnessTags().stream().sorted().toList(),
                "steps", fixture.steps().stream().map(ComparisonJson::fixtureStep).toList()
        );
        if (fixture.benchmark() != null) {
            value.put("benchmark", benchmarkPlan(fixture.benchmark()));
        }
        return Map.copyOf(value);
    }

    /// Converts one fixture step.
    ///
    /// @param step the step
    /// @return the JSON value
    private static @Unmodifiable Map<String, Object> fixtureStep(FixtureStep step) {
        LinkedHashMap<String, Object> value = mutableMap(
                "id", step.id(),
                "command", command(step.command()),
                "expected", observation(step.expected()),
                "requiredPhases", step.phases().required().stream().map(ComparisonJson::canonical).sorted().toList()
        );
        if (step.requirement() != null) {
            value.put("requirement", map(
                    "capability", step.requirement().capability(),
                    "value", step.requirement().value()
            ));
        }
        return Map.copyOf(value);
    }

    /// Converts one benchmark plan.
    ///
    /// @param plan the plan
    /// @return the JSON value
    private static @Unmodifiable Map<String, Object> benchmarkPlan(BenchmarkPlan plan) {
        return map(
                "warmupIterations", plan.warmupIterations(),
                "measuredIterations", plan.measuredIterations(),
                "cycle", plan.cycle().stream().map(ComparisonJson::command).toList()
        );
    }

    /// Converts one command.
    ///
    /// @param command the command
    /// @return the JSON value
    private static @Unmodifiable Map<String, Object> command(FixtureCommand command) {
        return map("operation", command.operation(), "arguments", command.arguments());
    }

    /// Converts one observation.
    ///
    /// @param observation the observation
    /// @return the JSON value
    private static @Unmodifiable Map<String, Object> observation(FixtureObservation observation) {
        return map(
                "values", observation.values(),
                "mountedNodes", observation.mountedNodes(),
                "events", observation.events(),
                "diagnostics", observation.diagnostics()
        );
    }

    /// Converts one rubric rule.
    ///
    /// @param rule the rule
    /// @return the JSON value
    private static @Unmodifiable Map<String, Object> rubricRule(RubricRule rule) {
        return map(
                "id", rule.id(),
                "category", rule.category(),
                "description", rule.description(),
                "disqualifying", rule.disqualifying()
        );
    }

    /// Converts one rubric dimension.
    ///
    /// @param dimension the dimension
    /// @return the JSON value
    private static @Unmodifiable Map<String, Object> rubricDimension(RubricDimension dimension) {
        return map(
                "id", dimension.id(),
                "weight", dimension.weight(),
                "metrics", dimension.metrics(),
                "method", dimension.method()
        );
    }

    /// Converts one rubric checkpoint.
    ///
    /// @param checkpoint the checkpoint
    /// @return the JSON value
    private static @Unmodifiable Map<String, Object> rubricCheckpoint(RubricCheckpoint checkpoint) {
        return map(
                "id", checkpoint.id(),
                "completedStage", canonical(checkpoint.completedStage()),
                "continuationRule", checkpoint.continuationRule()
        );
    }

    /// Converts a candidate descriptor.
    ///
    /// @param candidate the descriptor
    /// @return the JSON value
    private static @Unmodifiable Map<String, Object> candidate(CandidateDescriptor candidate) {
        return map(
                "id", candidate.id(),
                "displayName", candidate.displayName(),
                "structuralModel", candidate.structuralModel(),
                "capabilities", candidate.capabilities().asMap(),
                "applicationCodeTransformed", candidate.applicationCodeTransformed(),
                "comparisonEligible", candidate.comparisonEligible()
        );
    }

    /// Converts external evidence.
    ///
    /// @param evidence the evidence
    /// @return the JSON value
    private static @Unmodifiable Map<String, Object> evidence(CandidateEvidence evidence) {
        return map(
                "nativeImage", canonical(evidence.nativeImage()),
                "reloadIdentity", canonical(evidence.reloadIdentity()),
                "ceremonyReview", canonical(evidence.ceremonyReview()),
                "artifacts", evidence.artifacts()
        );
    }

    /// Converts the process environment.
    ///
    /// @param environment the environment
    /// @return the JSON value
    private static @Unmodifiable Map<String, Object> environment(ComparisonEnvironmentRecord environment) {
        return map(
                "javaRuntimeVersion", environment.javaRuntimeVersion(),
                "vmName", environment.vmName(),
                "osName", environment.osName(),
                "osArchitecture", environment.osArchitecture(),
                "availableProcessors", environment.availableProcessors()
        );
    }

    /// Converts source metrics.
    ///
    /// @param metrics the metrics
    /// @return the JSON value
    private static @Unmodifiable Map<String, Object> sourceMetrics(SourceMetrics metrics) {
        return map(
                "sourceLines", metrics.sourceLines(),
                "files", metrics.files().stream().map(file -> map(
                        "relativePath", file.relativePath(),
                        "stage", canonical(file.stage()),
                        "sourceLines", file.sourceLines()
                )).toList(),
                "ceremonyCounts", metrics.ceremonyCounts(),
                "markers", metrics.markers().stream().map(marker -> map(
                        "relativePath", marker.relativePath(),
                        "line", marker.line(),
                        "kind", canonical(marker.kind()),
                        "rationale", marker.rationale()
                )).toList()
        );
    }

    /// Converts one fixture result.
    ///
    /// @param result the result
    /// @return the JSON value
    private static @Unmodifiable Map<String, Object> fixtureResult(FixtureResult result) {
        LinkedHashMap<String, Object> value = mutableMap(
                "id", result.id(),
                "stage", canonical(result.stage()),
                "status", canonical(result.status()),
                "steps", result.steps().stream().map(ComparisonJson::stepResult).toList(),
                "scenarioProbe", probe(result.scenarioProbe()),
                "postCloseHealth", health(result.postCloseHealth()),
                "failures", result.failures()
        );
        if (result.benchmark() != null) {
            value.put("benchmark", benchmark(result.benchmark()));
        }
        return Map.copyOf(value);
    }

    /// Converts one step result.
    ///
    /// @param result the result
    /// @return the JSON value
    private static @Unmodifiable Map<String, Object> stepResult(StepResult result) {
        LinkedHashMap<String, Object> value = mutableMap(
                "id", result.id(),
                "passed", result.passed(),
                "metrics", stepMetrics(result.metrics())
        );
        if (result.actual() != null) {
            value.put("actual", observation(result.actual()));
        }
        if (result.failure() != null) {
            value.put("failure", result.failure());
        }
        return Map.copyOf(value);
    }

    /// Converts step metrics.
    ///
    /// @param metrics the metrics
    /// @return the JSON value
    private static @Unmodifiable Map<String, Object> stepMetrics(StepMetrics metrics) {
        return map(
                "callbacksExecuted", metrics.callbacksExecuted(),
                "nodesVisited", metrics.nodesVisited(),
                "dependencyEdgesAttached", metrics.dependencyEdgesAttached(),
                "dependencyEdgesDetached", metrics.dependencyEdgesDetached(),
                "activeDependencyEdges", metrics.activeDependencyEdges(),
                "retainedBytes", metrics.retainedBytes(),
                "phaseInvalidations", metrics.phaseInvalidations(),
                "allocation", allocation(metrics.allocation()),
                "traces", metrics.traces().stream().map(ComparisonJson::trace).toList()
        );
    }

    /// Converts benchmark metrics.
    ///
    /// @param metrics the metrics
    /// @return the JSON value
    private static @Unmodifiable Map<String, Object> benchmark(BenchmarkMetrics metrics) {
        return map(
                "warmupIterations", metrics.warmupIterations(),
                "measuredIterations", metrics.measuredIterations(),
                "commandsPerCycle", metrics.commandsPerCycle(),
                "elapsedNanos", metrics.elapsedNanos(),
                "allocation", allocation(metrics.allocation()),
                "probe", probe(metrics.probe())
        );
    }

    /// Converts probe metrics.
    ///
    /// @param metrics the metrics
    /// @return the JSON value
    private static @Unmodifiable Map<String, Object> probe(ProbeMetrics metrics) {
        return map(
                "callbacksExecuted", metrics.callbacksExecuted(),
                "nodesVisited", metrics.nodesVisited(),
                "dependencyEdgesAttached", metrics.dependencyEdgesAttached(),
                "dependencyEdgesDetached", metrics.dependencyEdgesDetached(),
                "activeDependencyEdges", metrics.activeDependencyEdges(),
                "peakDependencyEdges", metrics.peakDependencyEdges(),
                "retainedBytes", metrics.retainedBytes(),
                "peakRetainedBytes", metrics.peakRetainedBytes(),
                "phaseInvalidations", metrics.phaseInvalidations(),
                "maximumTraceQuality", metrics.maximumTraceQuality(),
                "traces", metrics.traces().stream().map(ComparisonJson::trace).toList()
        );
    }

    /// Converts one diagnostic trace, omitting absent optional fields.
    ///
    /// @param trace the trace
    /// @return the JSON value
    private static @Unmodifiable Map<String, Object> trace(DiagnosticTrace trace) {
        LinkedHashMap<String, Object> value = mutableMap(
                "code", trace.code(),
                "message", trace.message(),
                "qualityScore", trace.qualityScore()
        );
        putIfNotNull(value, "sourceLocation", trace.sourceLocation());
        putIfNotNull(value, "ownerPath", trace.ownerPath());
        putIfNotNull(value, "dependencyPath", trace.dependencyPath());
        putIfNotNull(value, "recoveryAction", trace.recoveryAction());
        return Map.copyOf(value);
    }

    /// Converts an allocation measurement.
    ///
    /// @param measurement the measurement
    /// @return the JSON value
    private static @Unmodifiable Map<String, Object> allocation(AllocationMeasurement measurement) {
        return map("available", measurement.available(), "bytes", measurement.bytes());
    }

    /// Converts post-close health.
    ///
    /// @param health the health snapshot
    /// @return the JSON value
    private static @Unmodifiable Map<String, Object> health(RuntimeHealth health) {
        return map(
                "liveNodes", health.liveNodes(),
                "liveOwners", health.liveOwners(),
                "liveEffects", health.liveEffects(),
                "stagedMutations", health.stagedMutations(),
                "pendingCallbacks", health.pendingCallbacks(),
                "clean", health.clean()
        );
    }

    /// Converts one disqualification.
    ///
    /// @param disqualification the disqualification
    /// @return the JSON value
    private static @Unmodifiable Map<String, Object> disqualification(Disqualification disqualification) {
        LinkedHashMap<String, Object> value = mutableMap(
                "code", disqualification.code(),
                "detail", disqualification.detail()
        );
        putIfNotNull(value, "fixtureId", disqualification.fixtureId());
        return Map.copyOf(value);
    }

    /// Computes aggregate fixture and benchmark totals without discarding per-fixture detail.
    ///
    /// @param fixtures the fixture results
    /// @return the JSON value
    private static @Unmodifiable Map<String, Object> summary(@Unmodifiable List<FixtureResult> fixtures) {
        HashMap<String, Long> fixtureCounts = new HashMap<>();
        for (FixtureStatus status : FixtureStatus.values()) {
            fixtureCounts.put(canonical(status), 0L);
        }
        long measuredCycles = 0L;
        long measuredCommands = 0L;
        long elapsedNanos = 0L;
        long allocatedBytes = 0L;
        boolean allocationAvailable = true;
        int benchmarkFixtures = 0;
        int maximumTraceQuality = 0;
        ArrayList<ProbeMetrics> scenarioProbes = new ArrayList<>();
        ArrayList<ProbeMetrics> benchmarkProbes = new ArrayList<>();
        for (FixtureResult fixture : fixtures) {
            fixtureCounts.compute(canonical(fixture.status()), (ignored, count) -> Math.incrementExact(count));
            scenarioProbes.add(fixture.scenarioProbe());
            maximumTraceQuality = Math.max(maximumTraceQuality, fixture.scenarioProbe().maximumTraceQuality());
            @Nullable BenchmarkMetrics benchmark = fixture.benchmark();
            if (benchmark != null) {
                benchmarkFixtures++;
                measuredCycles = Math.addExact(measuredCycles, benchmark.measuredIterations());
                measuredCommands = Math.addExact(
                        measuredCommands,
                        Math.multiplyExact((long) benchmark.measuredIterations(), benchmark.commandsPerCycle())
                );
                elapsedNanos = Math.addExact(elapsedNanos, benchmark.elapsedNanos());
                allocationAvailable &= benchmark.allocation().available();
                allocatedBytes = Math.addExact(allocatedBytes, benchmark.allocation().bytes());
                benchmarkProbes.add(benchmark.probe());
                maximumTraceQuality = Math.max(maximumTraceQuality, benchmark.probe().maximumTraceQuality());
            }
        }
        return map(
                "fixtureCounts", fixtureCounts,
                "benchmarkFixtures", benchmarkFixtures,
                "measuredCycles", measuredCycles,
                "measuredCommands", measuredCommands,
                "steadyStateElapsedNanos", elapsedNanos,
                "steadyStateAllocation", map(
                        "available", allocationAvailable && benchmarkFixtures > 0,
                        "bytes", allocationAvailable && benchmarkFixtures > 0 ? allocatedBytes : 0L
                ),
                "scenarioTotals", probe(sumProbes(scenarioProbes)),
                "benchmarkProbeTotals", probe(sumProbes(benchmarkProbes)),
                "maximumTraceQuality", maximumTraceQuality
        );
    }

    /// Sums independent probe windows while retaining their complete trace order.
    ///
    /// Active and peak counts are summed because each fixture is an independently mounted workload;
    /// no claim is made that the fixtures were live simultaneously.
    ///
    /// @param probes the probe windows
    /// @return the aggregate metrics
    private static ProbeMetrics sumProbes(@Unmodifiable List<ProbeMetrics> probes) {
        HashMap<String, Long> callbacks = new HashMap<>();
        HashMap<String, Long> phases = new HashMap<>();
        ArrayList<DiagnosticTrace> traces = new ArrayList<>();
        long nodesVisited = 0L;
        long attached = 0L;
        long detached = 0L;
        long activeEdges = 0L;
        long peakEdges = 0L;
        long retainedBytes = 0L;
        long peakRetainedBytes = 0L;
        for (ProbeMetrics probe : probes) {
            addMetrics(callbacks, probe.callbacksExecuted());
            addMetrics(phases, probe.phaseInvalidations());
            traces.addAll(probe.traces());
            nodesVisited = Math.addExact(nodesVisited, probe.nodesVisited());
            attached = Math.addExact(attached, probe.dependencyEdgesAttached());
            detached = Math.addExact(detached, probe.dependencyEdgesDetached());
            activeEdges = Math.addExact(activeEdges, probe.activeDependencyEdges());
            peakEdges = Math.addExact(peakEdges, probe.peakDependencyEdges());
            retainedBytes = Math.addExact(retainedBytes, probe.retainedBytes());
            peakRetainedBytes = Math.addExact(peakRetainedBytes, probe.peakRetainedBytes());
        }
        return new ProbeMetrics(
                callbacks,
                nodesVisited,
                attached,
                detached,
                activeEdges,
                peakEdges,
                retainedBytes,
                peakRetainedBytes,
                phases,
                traces
        );
    }

    /// Adds one metric map into a mutable aggregate.
    ///
    /// @param target the aggregate
    /// @param values the values to add
    private static void addMetrics(Map<String, Long> target, @Unmodifiable Map<String, Long> values) {
        for (Map.Entry<String, Long> entry : values.entrySet()) {
            target.compute(entry.getKey(), (ignored, current) -> Math.addExact(
                    current == null ? 0L : current,
                    entry.getValue()
            ));
        }
    }

    /// Adds an optional string only when present.
    ///
    /// @param target the target map
    /// @param key the key
    /// @param value the optional value
    private static void putIfNotNull(Map<String, Object> target, String key, @Nullable String value) {
        if (value != null) {
            target.put(key, value);
        }
    }

    /// Creates an immutable map from alternating keys and non-null values.
    ///
    /// @param entries alternating keys and values
    /// @return the immutable map
    private static @Unmodifiable Map<String, Object> map(Object... entries) {
        return Map.copyOf(mutableMap(entries));
    }

    /// Creates a mutable map from alternating keys and non-null values.
    ///
    /// @param entries alternating keys and values
    /// @return the mutable map
    private static LinkedHashMap<String, Object> mutableMap(Object... entries) {
        if (entries.length % 2 != 0) {
            throw new IllegalArgumentException("map entries must alternate keys and values");
        }
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        for (int index = 0; index < entries.length; index += 2) {
            if (!(entries[index] instanceof String key)) {
                throw new IllegalArgumentException("map key must be a string");
            }
            Object value = java.util.Objects.requireNonNull(entries[index + 1], "map value");
            if (result.put(key, value) != null) {
                throw new IllegalArgumentException("duplicate map key " + key);
            }
        }
        return result;
    }

    /// Returns a lower-kebab-case enum spelling.
    ///
    /// @param value the enum value
    /// @return the report spelling
    private static String canonical(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }
}

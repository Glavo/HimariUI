package org.glavo.himari.spikes.runtime.decision;

import org.glavo.himari.spikes.runtime.sample.BenchmarkMetrics;
import org.glavo.himari.spikes.runtime.sample.CandidateCapabilities;
import org.glavo.himari.spikes.runtime.sample.ComparisonEnvironmentRecord;
import org.glavo.himari.spikes.runtime.sample.ComparisonReport;
import org.glavo.himari.spikes.runtime.sample.ComparisonStatus;
import org.glavo.himari.spikes.runtime.sample.EvidenceStatus;
import org.glavo.himari.spikes.runtime.sample.FixtureCatalog;
import org.glavo.himari.spikes.runtime.sample.FixtureDefinition;
import org.glavo.himari.spikes.runtime.sample.FixtureResult;
import org.glavo.himari.spikes.runtime.sample.FixtureStep;
import org.glavo.himari.spikes.runtime.sample.ProbeMetrics;
import org.glavo.himari.spikes.runtime.sample.StepResult;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/// Captures the compact, lossless inputs consumed by the frozen decision score.
///
/// @param candidateId the stable candidate descriptor identifier
/// @param displayName the candidate display name
/// @param structuralModel the documented structural semantics
/// @param measureMaterializationMode the canonical ADR-020 capability value
/// @param cancellationSupport the canonical cancellation capability value
/// @param reloadIdentityClaimed whether reload identity is claimed
/// @param status the evidence-backed JVM report status
/// @param environment the comparable JVM environment
/// @param allocationAvailable whether every benchmark allocation counter was available
/// @param measuredCommands total commands in steady-state benchmark windows
/// @param metrics immutable raw counters and evidence values
/// @param reportSha256 the complete canonical report digest
@NotNullByDefault
record CandidateMeasurements(
        String candidateId,
        String displayName,
        String structuralModel,
        String measureMaterializationMode,
        String cancellationSupport,
        boolean reloadIdentityClaimed,
        ComparisonStatus status,
        ComparisonEnvironmentRecord environment,
        boolean allocationAvailable,
        long measuredCommands,
        @Unmodifiable Map<String, Long> metrics,
        String reportSha256
) {
    /// The complete metric key set accepted by the decision scorer.
    static final @Unmodifiable Set<String> METRIC_KEYS = Set.of(
            "sourceLines",
            "explicitKeys",
            "deferredGetters",
            "structuralControls",
            "groupBoundaries",
            "genericTypeNoise",
            "callbackWrappers",
            "traceQuality",
            "requiredDiagnosticMatched",
            "requiredDiagnosticExpected",
            "deterministicRecoveryMatched",
            "deterministicRecoveryExpected",
            "callbacksExecuted",
            "nodesVisited",
            "dependencyEdges",
            "phaseInvalidations",
            "steadyStateAllocatedBytes",
            "peakRetainedBytes",
            "postCloseRetainedBytes",
            "steadyStateElapsedNanos",
            "nativeImage",
            "reloadIdentity"
    );

    /// Creates and validates one compact measurement record.
    CandidateMeasurements {
        candidateId = requireText(candidateId, "candidateId");
        displayName = requireText(displayName, "displayName");
        structuralModel = requireText(structuralModel, "structuralModel");
        measureMaterializationMode = requireText(measureMaterializationMode, "measureMaterializationMode");
        cancellationSupport = requireText(cancellationSupport, "cancellationSupport");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(environment, "environment");
        if (measuredCommands < 0L) {
            throw new IllegalArgumentException("measuredCommands must be nonnegative");
        }
        Objects.requireNonNull(metrics, "metrics");
        TreeMap<String, Long> copy = new TreeMap<>(metrics);
        if (!copy.keySet().equals(METRIC_KEYS)) {
            throw new IllegalArgumentException("Measurement metric keys do not match the frozen decision schema");
        }
        for (Map.Entry<String, Long> entry : copy.entrySet()) {
            if (entry.getValue() < 0L) {
                throw new IllegalArgumentException("Measurement metric is negative: " + entry.getKey());
            }
        }
        metrics = Collections.unmodifiableMap(copy);
        if (!reportSha256.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("reportSha256 must be a lowercase SHA-256 digest");
        }
    }

    /// Extracts the frozen scoring inputs from one complete report.
    ///
    /// @param report the evidence-backed comparison report
    /// @param reportSha256 the canonical report digest
    /// @return the compact measurements
    static CandidateMeasurements from(ComparisonReport report, String reportSha256) {
        Objects.requireNonNull(report, "report");
        TreeMap<String, Long> metrics = new TreeMap<>();
        metrics.put("sourceLines", report.sourceMetrics().sourceLines());
        for (String key : List.of(
                "explicitKeys", "deferredGetters", "structuralControls", "groupBoundaries",
                "genericTypeNoise", "callbackWrappers"
        )) {
            metrics.put(key, report.sourceMetrics().ceremonyCounts().getOrDefault(key, 0L));
        }

        long callbacksExecuted = 0L;
        long nodesVisited = 0L;
        long dependencyEdges = 0L;
        long phaseInvalidations = 0L;
        long allocatedBytes = 0L;
        long peakRetainedBytes = 0L;
        long postCloseRetainedBytes = 0L;
        long elapsedNanos = 0L;
        long measuredCommands = 0L;
        int maximumTraceQuality = 0;
        boolean allocationAvailable = true;
        long diagnosticsMatched = 0L;
        long diagnosticsExpected = 0L;
        long recoveriesMatched = 0L;
        long recoveriesExpected = 0L;

        Map<String, FixtureDefinition> definitions = new HashMap<>();
        for (FixtureDefinition definition : FixtureCatalog.fixtures()) {
            definitions.put(definition.id(), definition);
        }
        for (FixtureResult fixture : report.fixtures()) {
            ProbeTotals scenario = totals(fixture.scenarioProbe());
            callbacksExecuted = Math.addExact(callbacksExecuted, scenario.callbacks());
            nodesVisited = Math.addExact(nodesVisited, scenario.nodes());
            dependencyEdges = Math.addExact(dependencyEdges, scenario.edges());
            phaseInvalidations = Math.addExact(phaseInvalidations, scenario.invalidations());
            peakRetainedBytes = Math.max(peakRetainedBytes, fixture.scenarioProbe().peakRetainedBytes());
            postCloseRetainedBytes = Math.addExact(postCloseRetainedBytes, fixture.scenarioProbe().retainedBytes());
            maximumTraceQuality = Math.max(maximumTraceQuality, fixture.scenarioProbe().maximumTraceQuality());

            @Nullable BenchmarkMetrics benchmark = fixture.benchmark();
            if (benchmark != null) {
                ProbeTotals benchmarkTotals = totals(benchmark.probe());
                callbacksExecuted = Math.addExact(callbacksExecuted, benchmarkTotals.callbacks());
                nodesVisited = Math.addExact(nodesVisited, benchmarkTotals.nodes());
                dependencyEdges = Math.addExact(dependencyEdges, benchmarkTotals.edges());
                phaseInvalidations = Math.addExact(phaseInvalidations, benchmarkTotals.invalidations());
                peakRetainedBytes = Math.max(peakRetainedBytes, benchmark.probe().peakRetainedBytes());
                maximumTraceQuality = Math.max(maximumTraceQuality, benchmark.probe().maximumTraceQuality());
                allocationAvailable &= benchmark.allocation().available();
                allocatedBytes = Math.addExact(allocatedBytes, benchmark.allocation().bytes());
                elapsedNanos = Math.addExact(elapsedNanos, benchmark.elapsedNanos());
                measuredCommands = Math.addExact(
                        measuredCommands,
                        Math.multiplyExact((long) benchmark.measuredIterations(), benchmark.commandsPerCycle())
                );
            }

            FixtureDefinition definition = Objects.requireNonNull(
                    definitions.get(fixture.id()),
                    "fixture definition " + fixture.id()
            );
            Map<String, FixtureStep> steps = new HashMap<>();
            for (FixtureStep step : definition.steps()) {
                steps.put(step.id(), step);
            }
            for (StepResult result : fixture.steps()) {
                FixtureStep step = Objects.requireNonNull(steps.get(result.id()), "fixture step " + result.id());
                List<String> requiredDiagnostics = step.expected().diagnostics();
                diagnosticsExpected = Math.addExact(diagnosticsExpected, requiredDiagnostics.size());
                if (result.actual() != null) {
                    for (String diagnostic : requiredDiagnostics) {
                        if (result.actual().diagnostics().contains(diagnostic)) {
                            diagnosticsMatched = Math.incrementExact(diagnosticsMatched);
                        }
                    }
                }
                if (isRecoveryStep(result.id())) {
                    recoveriesExpected = Math.incrementExact(recoveriesExpected);
                    if (result.passed()) {
                        recoveriesMatched = Math.incrementExact(recoveriesMatched);
                    }
                }
            }
        }
        metrics.put("traceQuality", (long) maximumTraceQuality);
        metrics.put("requiredDiagnosticMatched", diagnosticsMatched);
        metrics.put("requiredDiagnosticExpected", diagnosticsExpected);
        metrics.put("deterministicRecoveryMatched", recoveriesMatched);
        metrics.put("deterministicRecoveryExpected", recoveriesExpected);
        metrics.put("callbacksExecuted", callbacksExecuted);
        metrics.put("nodesVisited", nodesVisited);
        metrics.put("dependencyEdges", dependencyEdges);
        metrics.put("phaseInvalidations", phaseInvalidations);
        metrics.put("steadyStateAllocatedBytes", allocatedBytes);
        metrics.put("peakRetainedBytes", peakRetainedBytes);
        metrics.put("postCloseRetainedBytes", postCloseRetainedBytes);
        metrics.put("steadyStateElapsedNanos", elapsedNanos);
        metrics.put("nativeImage", report.evidence().nativeImage() == EvidenceStatus.PASSED ? 1L : 0L);
        metrics.put("reloadIdentity", report.evidence().reloadIdentity() == EvidenceStatus.PASSED ? 1L : 0L);

        CandidateCapabilities capabilities = report.candidate().capabilities();
        return new CandidateMeasurements(
                report.candidate().id(),
                report.candidate().displayName(),
                report.candidate().structuralModel(),
                Objects.requireNonNull(capabilities.value(CandidateCapabilities.MEASURE_MATERIALIZATION)),
                Objects.requireNonNull(capabilities.value(CandidateCapabilities.CANCELLATION)),
                capabilities.reloadIdentityClaimed(),
                report.status(),
                report.environment(),
                allocationAvailable,
                measuredCommands,
                metrics,
                reportSha256
        );
    }

    /// Writes this measurement as deterministic properties.
    ///
    /// @param path the output file
    /// @throws IOException if the file cannot be written
    void write(Path path) throws IOException {
        TreeMap<String, String> values = new TreeMap<>();
        values.put("schemaVersion", "1");
        values.put("candidateId", candidateId);
        values.put("displayName", displayName);
        values.put("structuralModel", structuralModel);
        values.put("measureMaterializationMode", measureMaterializationMode);
        values.put("cancellationSupport", cancellationSupport);
        values.put("reloadIdentityClaimed", Boolean.toString(reloadIdentityClaimed));
        values.put("status", status.name());
        values.put("environment.javaRuntimeVersion", environment.javaRuntimeVersion());
        values.put("environment.vmName", environment.vmName());
        values.put("environment.osName", environment.osName());
        values.put("environment.osArchitecture", environment.osArchitecture());
        values.put("environment.availableProcessors", Integer.toString(environment.availableProcessors()));
        values.put("allocationAvailable", Boolean.toString(allocationAvailable));
        values.put("measuredCommands", Long.toString(measuredCommands));
        values.put("reportSha256", reportSha256);
        for (Map.Entry<String, Long> entry : metrics.entrySet()) {
            values.put("metric." + entry.getKey(), Long.toString(entry.getValue()));
        }
        DecisionProperties.write(path, values);
    }

    /// Reads a deterministic measurement file.
    ///
    /// @param path the input file
    /// @return the decoded measurements
    /// @throws IOException if the file cannot be read
    static CandidateMeasurements read(Path path) throws IOException {
        Map<String, String> values = DecisionProperties.read(path);
        if (!DecisionProperties.require(values, "schemaVersion").equals("1")) {
            throw new IllegalArgumentException("Unsupported candidate measurement schema");
        }
        TreeMap<String, Long> metrics = new TreeMap<>();
        for (String key : METRIC_KEYS) {
            metrics.put(key, DecisionProperties.requireNonNegativeLong(values, "metric." + key));
        }
        long processors = DecisionProperties.requireNonNegativeLong(values, "environment.availableProcessors");
        if (processors < 1L || processors > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Invalid available processor count");
        }
        return new CandidateMeasurements(
                DecisionProperties.require(values, "candidateId"),
                DecisionProperties.require(values, "displayName"),
                DecisionProperties.require(values, "structuralModel"),
                DecisionProperties.require(values, "measureMaterializationMode"),
                DecisionProperties.require(values, "cancellationSupport"),
                DecisionProperties.requireBoolean(values, "reloadIdentityClaimed"),
                ComparisonStatus.valueOf(DecisionProperties.require(values, "status")),
                new ComparisonEnvironmentRecord(
                        DecisionProperties.require(values, "environment.javaRuntimeVersion"),
                        DecisionProperties.require(values, "environment.vmName"),
                        DecisionProperties.require(values, "environment.osName"),
                        DecisionProperties.require(values, "environment.osArchitecture"),
                        Math.toIntExact(processors)
                ),
                DecisionProperties.requireBoolean(values, "allocationAvailable"),
                DecisionProperties.requireNonNegativeLong(values, "measuredCommands"),
                metrics,
                DecisionProperties.require(values, "reportSha256")
        );
    }

    /// Returns one required raw metric.
    ///
    /// @param name the metric name
    /// @return the nonnegative value
    long metric(String name) {
        Long value = metrics.get(name);
        if (value == null) {
            throw new IllegalArgumentException("Unknown decision metric: " + name);
        }
        return value;
    }

    /// Returns a copy carrying one exact reviewed benchmark sample.
    ///
    /// All deterministic source, correctness, diagnostic, lifecycle, and capability measurements
    /// remain unchanged. The returned record replaces only the two environment-sensitive
    /// steady-state counters used to reproduce a reviewed scoring run.
    ///
    /// @param allocatedBytes steady-state bytes allocated by the reviewed run
    /// @param elapsedNanos steady-state elapsed nanoseconds from the reviewed run
    /// @return a measurement snapshot suitable for reproducing the reviewed score
    CandidateMeasurements withBenchmarkSample(long allocatedBytes, long elapsedNanos) {
        TreeMap<String, Long> reviewedMetrics = new TreeMap<>(metrics);
        reviewedMetrics.put("steadyStateAllocatedBytes", allocatedBytes);
        reviewedMetrics.put("steadyStateElapsedNanos", elapsedNanos);
        return new CandidateMeasurements(
                candidateId,
                displayName,
                structuralModel,
                measureMaterializationMode,
                cancellationSupport,
                reloadIdentityClaimed,
                status,
                environment,
                allocationAvailable,
                measuredCommands,
                reviewedMetrics,
                reportSha256
        );
    }

    /// Returns the total accidental-ceremony marker count used by the first tie-breaker.
    ///
    /// @return deferred getters, group boundaries, generic noise, and callback wrappers
    long accidentalCeremony() {
        return Math.addExact(
                Math.addExact(metric("deferredGetters"), metric("groupBoundaries")),
                Math.addExact(metric("genericTypeNoise"), metric("callbackWrappers"))
        );
    }

    /// Reduces one probe snapshot to the totals named by the scoring rubric.
    ///
    /// @param probe the probe snapshot
    /// @return aggregate callback, node, edge, and invalidation counts
    private static ProbeTotals totals(ProbeMetrics probe) {
        long callbacks = sum(probe.callbacksExecuted());
        long edges = Math.addExact(probe.dependencyEdgesAttached(), probe.dependencyEdgesDetached());
        return new ProbeTotals(callbacks, probe.nodesVisited(), edges, sum(probe.phaseInvalidations()));
    }

    /// Sums a nonnegative counter map with overflow checking.
    ///
    /// @param values the counters
    /// @return their exact sum
    private static long sum(@Unmodifiable Map<String, Long> values) {
        long result = 0L;
        for (long value : values.values()) {
            result = Math.addExact(result, value);
        }
        return result;
    }

    /// Returns whether a fixture step exercises deterministic recovery or cleanup convergence.
    ///
    /// @param stepId the fixture-local step identifier
    /// @return whether the step contributes to recovery coverage
    private static boolean isRecoveryStep(String stepId) {
        return stepId.startsWith("retry-")
                || stepId.startsWith("recover-")
                || stepId.startsWith("reset-")
                || stepId.startsWith("cancel-");
    }

    /// Requires nonblank measurement text.
    ///
    /// @param value the text
    /// @param name the diagnostic field name
    /// @return the unchanged text
    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    /// Holds the runtime totals extracted from one probe snapshot.
    ///
    /// @param callbacks callbacks executed
    /// @param nodes logical nodes visited
    /// @param edges dependency edges attached and detached
    /// @param invalidations phase invalidations
    @NotNullByDefault
    private record ProbeTotals(long callbacks, long nodes, long edges, long invalidations) {
        /// Creates nonnegative probe totals.
        private ProbeTotals {
            if (callbacks < 0L || nodes < 0L || edges < 0L || invalidations < 0L) {
                throw new IllegalArgumentException("Probe totals must be nonnegative");
            }
        }
    }
}

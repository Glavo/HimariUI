package org.glavo.himari.spikes.runtime.sample;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Objects;

/// Collects candidate execution, dependency, retained-memory, phase, and diagnostic instrumentation.
///
/// Every mutation is confined to the thread that creates the probe. Edge and retained-object tokens
/// use identity semantics. Candidates must attach and detach the same token and must register each
/// framework-owned retained object with an auditable deterministic shallow-byte estimate.
@NotNullByDefault
public final class ComparisonProbe {
    /// The thread permitted to mutate this probe.
    private final Thread ownerThread = Thread.currentThread();

    /// Callback counters by callback kind.
    private final long[] callbacks = new long[RuntimeCallbackKind.values().length];

    /// Phase invalidation counters by phase.
    private final long[] phases = new long[RuntimePhase.values().length];

    /// Active dependency-edge identity tokens.
    private final IdentityHashMap<Object, Boolean> dependencyEdges = new IdentityHashMap<>();

    /// Active retained-object identity tokens and their shallow-byte estimates.
    private final IdentityHashMap<Object, Long> retainedObjects = new IdentityHashMap<>();

    /// Diagnostic traces in emission order.
    private final ArrayList<DiagnosticTrace> traces = new ArrayList<>();

    /// Total logical node visits in the current measurement window.
    private long nodesVisited;

    /// Total dependency attachments in the current measurement window.
    private long edgesAttached;

    /// Total dependency detachments in the current measurement window.
    private long edgesDetached;

    /// Maximum active edge count in the current measurement window.
    private long peakEdges;

    /// Current registered shallow bytes.
    private long retainedBytes;

    /// Maximum registered shallow bytes in the current measurement window.
    private long peakRetainedBytes;

    /// Creates an empty probe owned by the current thread.
    public ComparisonProbe() {
    }

    /// Records one candidate callback execution.
    ///
    /// @param kind the callback kind
    public void callbackExecuted(RuntimeCallbackKind kind) {
        checkOwnerThread();
        Objects.requireNonNull(kind, "kind");
        int index = kind.ordinal();
        callbacks[index] = Math.incrementExact(callbacks[index]);
    }

    /// Records logical visits to mounted or staged nodes.
    ///
    /// @param count the positive number of visits
    /// @throws IllegalArgumentException if `count` is not positive
    public void nodesVisited(long count) {
        checkOwnerThread();
        if (count <= 0L) {
            throw new IllegalArgumentException("node visit count must be positive");
        }
        nodesVisited = Math.addExact(nodesVisited, count);
    }

    /// Attaches one candidate dependency edge.
    ///
    /// @param token the stable edge identity
    /// @throws IllegalStateException if the token is already active
    public void dependencyAttached(Object token) {
        checkOwnerThread();
        Objects.requireNonNull(token, "token");
        if (dependencyEdges.put(token, Boolean.TRUE) != null) {
            throw new IllegalStateException("dependency edge is already attached");
        }
        edgesAttached = Math.incrementExact(edgesAttached);
        peakEdges = Math.max(peakEdges, dependencyEdges.size());
    }

    /// Detaches one candidate dependency edge.
    ///
    /// @param token the stable edge identity used for attachment
    /// @throws IllegalStateException if the token is not active
    public void dependencyDetached(Object token) {
        checkOwnerThread();
        Objects.requireNonNull(token, "token");
        if (dependencyEdges.remove(token) == null) {
            throw new IllegalStateException("dependency edge is not attached");
        }
        edgesDetached = Math.incrementExact(edgesDetached);
    }

    /// Registers one framework-owned object retained by the candidate runtime.
    ///
    /// @param token the object or stable ownership token
    /// @param shallowBytes the positive deterministic shallow-byte estimate
    /// @throws IllegalArgumentException if `shallowBytes` is not positive
    /// @throws IllegalStateException if the token is already registered
    public void retained(Object token, long shallowBytes) {
        checkOwnerThread();
        Objects.requireNonNull(token, "token");
        if (shallowBytes <= 0L) {
            throw new IllegalArgumentException("retained shallow bytes must be positive");
        }
        if (retainedObjects.put(token, shallowBytes) != null) {
            throw new IllegalStateException("retained object is already registered");
        }
        retainedBytes = Math.addExact(retainedBytes, shallowBytes);
        peakRetainedBytes = Math.max(peakRetainedBytes, retainedBytes);
    }

    /// Releases one previously registered retained object.
    ///
    /// @param token the object or ownership token used for registration
    /// @throws IllegalStateException if the token is not registered
    public void released(Object token) {
        checkOwnerThread();
        Objects.requireNonNull(token, "token");
        @Nullable Long bytes = retainedObjects.remove(token);
        if (bytes == null) {
            throw new IllegalStateException("retained object is not registered");
        }
        retainedBytes = Math.subtractExact(retainedBytes, bytes);
    }

    /// Records one phase invalidation.
    ///
    /// @param phase the invalidated phase
    public void phaseInvalidated(RuntimePhase phase) {
        checkOwnerThread();
        Objects.requireNonNull(phase, "phase");
        int index = phase.ordinal();
        phases[index] = Math.incrementExact(phases[index]);
    }

    /// Records one deterministic diagnostic trace.
    ///
    /// @param trace the trace
    public void trace(DiagnosticTrace trace) {
        checkOwnerThread();
        traces.add(Objects.requireNonNull(trace, "trace"));
    }

    /// Returns an immutable snapshot of the current measurement window.
    ///
    /// @return the metrics snapshot
    public ProbeMetrics metrics() {
        checkOwnerThread();
        HashMap<String, Long> callbackMetrics = new HashMap<>();
        for (RuntimeCallbackKind kind : RuntimeCallbackKind.values()) {
            callbackMetrics.put(canonical(kind), callbacks[kind.ordinal()]);
        }
        HashMap<String, Long> phaseMetrics = new HashMap<>();
        for (RuntimePhase phase : RuntimePhase.values()) {
            phaseMetrics.put(canonical(phase), phases[phase.ordinal()]);
        }
        return new ProbeMetrics(
                callbackMetrics,
                nodesVisited,
                edgesAttached,
                edgesDetached,
                dependencyEdges.size(),
                peakEdges,
                retainedBytes,
                peakRetainedBytes,
                phaseMetrics,
                traces
        );
    }

    /// Starts a new counter window while preserving active edge and retained-object registrations.
    ///
    /// Existing registrations become the baseline and therefore contribute to current and peak
    /// retained metrics without being counted as new attachments.
    void resetMeasurementWindow() {
        checkOwnerThread();
        Arrays.fill(callbacks, 0L);
        Arrays.fill(phases, 0L);
        traces.clear();
        nodesVisited = 0L;
        edgesAttached = 0L;
        edgesDetached = 0L;
        peakEdges = dependencyEdges.size();
        peakRetainedBytes = retainedBytes;
    }

    /// Verifies that the caller owns probe mutation.
    ///
    /// @throws IllegalStateException if called from another thread
    private void checkOwnerThread() {
        if (Thread.currentThread() != ownerThread) {
            throw new IllegalStateException("Comparison probe may be mutated only on its owner thread");
        }
    }

    /// Returns a lower-camel report key for an enum constant.
    ///
    /// @param value the enum value
    /// @return the report key
    private static String canonical(Enum<?> value) {
        String[] words = value.name().toLowerCase(Locale.ROOT).split("_");
        StringBuilder result = new StringBuilder(words[0]);
        for (int index = 1; index < words.length; index++) {
            String word = words[index];
            result.append(Character.toUpperCase(word.charAt(0))).append(word, 1, word.length());
        }
        return result.toString();
    }
}

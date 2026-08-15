package org.glavo.himari.spikes.runtime.grouped;

import org.glavo.himari.spikes.runtime.sample.ComparisonEnvironment;
import org.glavo.himari.spikes.runtime.sample.ComparisonProbe;
import org.glavo.himari.spikes.runtime.sample.DiagnosticTrace;
import org.glavo.himari.spikes.runtime.sample.FixtureCommand;
import org.glavo.himari.spikes.runtime.sample.FixtureObservation;
import org.glavo.himari.spikes.runtime.sample.RuntimeCallbackKind;
import org.glavo.himari.spikes.runtime.sample.RuntimeFixtureSession;
import org.glavo.himari.spikes.runtime.sample.RuntimeHealth;
import org.glavo.himari.state.StateDomain;
import org.glavo.himari.state.StateTransaction;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/// Provides command, observation, transaction, diagnostic, and cleanup plumbing for grouped apps.
@NotNullByDefault
abstract class GroupedFixtureSession implements RuntimeFixtureSession {
    /// The fresh state domain supplied by the neutral comparison environment.
    protected final StateDomain domain;

    /// The shared instrumentation probe.
    protected final ComparisonProbe probe;

    /// Events emitted by the latest application command.
    private final ArrayList<String> events = new ArrayList<>();

    /// Diagnostics emitted by the latest application command.
    private final ArrayList<String> diagnostics = new ArrayList<>();

    /// The candidate-specific explicit grouped runtime.
    private final GroupedRuntime runtime;

    /// Whether the initial structure has committed.
    private boolean mounted;

    /// Whether lifecycle events are suppressed for mount, benchmark, or close work.
    private boolean eventsMuted;

    /// Whether commands are executing in the steady-state benchmark path.
    private boolean benchmarking;

    /// Whether closure completed.
    private boolean closed;

    /// Creates an unmounted fixture session.
    ///
    /// @param environment the fresh neutral environment
    /// @param probe the shared instrumentation sink
    protected GroupedFixtureSession(ComparisonEnvironment environment, ComparisonProbe probe) {
        Objects.requireNonNull(environment, "environment");
        this.domain = environment.stateDomain();
        this.probe = Objects.requireNonNull(probe, "probe");
        this.runtime = new GroupedRuntime(this::compose, probe);
    }

    /// Executes a fixture command and, when requested by the application, recomposes atomically.
    ///
    /// @param command the application-domain command
    @Override
    public final void execute(FixtureCommand command) {
        Objects.requireNonNull(command, "command");
        checkOpen();
        events.clear();
        diagnostics.clear();
        if (command.operation().equals("mount")) {
            if (mounted) {
                throw new IllegalStateException("Fixture is already mounted");
            }
            eventsMuted = true;
            try {
                runtime.recompose();
                mounted = true;
            } finally {
                eventsMuted = false;
            }
            return;
        }
        if (!mounted) {
            throw new IllegalStateException("Fixture must be mounted before commands execute");
        }
        probe.callbackExecuted(RuntimeCallbackKind.EVENT);
        try {
            if (handle(command)) {
                runtime.recompose();
            }
        } catch (GroupedRuntime.GroupedCompositionException failure) {
            onCompositionFailure(failure);
        }
    }

    /// Returns the application values and committed runtime nodes after the latest command.
    ///
    /// @return the exact neutral observation
    @Override
    public final FixtureObservation observation() {
        checkOpen();
        if (!mounted) {
            throw new IllegalStateException("Fixture is not mounted");
        }
        return new FixtureObservation(values(), runtime.mountedNodes(), events, diagnostics);
    }

    /// Enters benchmark mode and suppresses semantically irrelevant lifecycle event accumulation.
    @Override
    public final void beginBenchmark() {
        checkOpen();
        if (!mounted) {
            throw new IllegalStateException("Fixture is not mounted");
        }
        events.clear();
        diagnostics.clear();
        eventsMuted = true;
        benchmarking = true;
    }

    /// Returns live grouped-runtime resources, or the clean sentinel after closure.
    ///
    /// @return the health snapshot
    @Override
    public final RuntimeHealth health() {
        return closed ? RuntimeHealth.CLEAN : runtime.health();
    }

    /// Releases structural and application-owned resources.
    ///
    /// Closure is idempotent.
    @Override
    public final void close() {
        if (closed) {
            return;
        }
        eventsMuted = true;
        runtime.close();
        closeApplicationResources();
        events.clear();
        diagnostics.clear();
        mounted = false;
        closed = true;
    }

    /// Declares the complete application structure for one grouped execution.
    ///
    /// @param scope the explicit grouped scope
    protected abstract void compose(GroupedRuntime.Scope scope);

    /// Applies one non-mount application command.
    ///
    /// @param command the command
    /// @return whether the runtime must recompose after the mutation
    protected abstract boolean handle(FixtureCommand command);

    /// Returns the current application-owned observable values.
    ///
    /// @return the immutable value map
    protected abstract @Unmodifiable Map<String, String> values();

    /// Handles a contained composition failure after the prior committed tree was preserved.
    ///
    /// The default records the stable code in the fixture observation.
    ///
    /// @param failure the contained failure
    protected void onCompositionFailure(GroupedRuntime.GroupedCompositionException failure) {
        diagnostic(failure.code());
    }

    /// Releases application resources not owned by the grouped tree.
    protected void closeApplicationResources() {
    }

    /// Runs a group of source writes as one state-domain publication.
    ///
    /// @param mutation the owner-thread mutation
    protected final void transaction(Runnable mutation) {
        StateTransaction.run(domain, Objects.requireNonNull(mutation, "mutation"));
    }

    /// Returns whether the runner has entered the steady-state benchmark path.
    ///
    /// @return whether benchmark commands are executing
    protected final boolean benchmarking() {
        return benchmarking;
    }

    /// Emits one event unless the session is mounting, benchmarking, or closing.
    ///
    /// @param event the stable event text
    protected final void emit(String event) {
        if (!eventsMuted) {
            events.add(Objects.requireNonNull(event, "event"));
        }
    }

    /// Adds one stable diagnostic code to the current observation.
    ///
    /// @param code the diagnostic code
    protected final void diagnostic(String code) {
        diagnostics.add(Objects.requireNonNull(code, "code"));
    }

    /// Emits a quality-four structured trace and adds its stable diagnostic code.
    ///
    /// @param code the stable code
    /// @param ownerPath the application owner path
    /// @param recoveryAction the explicit recovery action
    protected final void traceDiagnostic(String code, String ownerPath, String recoveryAction) {
        diagnostic(code);
        probe.trace(new DiagnosticTrace(
                code,
                "Grouped application callback failed inside its declared boundary",
                "grouped-app",
                ownerPath,
                null,
                recoveryAction
        ));
    }

    /// Returns one required command argument.
    ///
    /// @param command the command
    /// @param key the argument key
    /// @return the argument value
    /// @throws IllegalArgumentException if the argument is absent
    protected static String argument(FixtureCommand command, String key) {
        @Nullable String value = command.arguments().get(Objects.requireNonNull(key, "key"));
        if (value == null) {
            throw new IllegalArgumentException("Missing command argument: " + key);
        }
        return value;
    }

    /// Parses one required decimal integer command argument.
    ///
    /// @param command the command
    /// @param key the argument key
    /// @return the integer value
    protected static int intArgument(FixtureCommand command, String key) {
        return Integer.parseInt(argument(command, key));
    }

    /// Creates a key-sorted observation map from alternating string keys and values.
    ///
    /// @param entries alternating keys and values
    /// @return the immutable map
    protected static @Unmodifiable Map<String, String> valuesOf(String... entries) {
        if (entries.length % 2 != 0) {
            throw new IllegalArgumentException("Observation values require alternating keys and values");
        }
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        for (int index = 0; index < entries.length; index += 2) {
            String key = Objects.requireNonNull(entries[index], "value key");
            String value = Objects.requireNonNull(entries[index + 1], "value");
            if (values.put(key, value) != null) {
                throw new IllegalArgumentException("Duplicate observation key: " + key);
            }
        }
        return Map.copyOf(values);
    }

    /// Rejects unknown commands deterministically.
    ///
    /// @param command the unsupported command
    /// @return an exception suitable for `throw`
    protected static IllegalArgumentException unknown(FixtureCommand command) {
        return new IllegalArgumentException("Unsupported command: " + command.operation());
    }

    /// Verifies that the session remains open.
    private void checkOpen() {
        if (closed) {
            throw new IllegalStateException("Fixture session is closed");
        }
    }
}

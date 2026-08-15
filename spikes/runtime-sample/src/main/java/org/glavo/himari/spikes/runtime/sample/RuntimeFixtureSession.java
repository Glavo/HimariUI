package org.glavo.himari.spikes.runtime.sample;

import org.jetbrains.annotations.NotNullByDefault;

/// Adapts one candidate-specific ordinary-Java application to a neutral fixture command stream.
///
/// A session begins unmounted. [#execute(FixtureCommand)] and [#observation()] run on the owning
/// comparison thread. [#close()] must be idempotent and release every node, owner, effect,
/// dependency edge, retained-memory token, staged mutation, and candidate-private callback.
@NotNullByDefault
public interface RuntimeFixtureSession extends AutoCloseable {
    /// Executes one application-domain command.
    ///
    /// The runner drains the shared Headless event loop immediately after this method returns.
    ///
    /// @param command the fixture command
    void execute(FixtureCommand command);

    /// Returns the exact observable application state after the latest command and Headless drain.
    ///
    /// @return the fixture observation
    FixtureObservation observation();

    /// Enters the steady-state benchmark path after the mounted initial observation was verified.
    ///
    /// The session must return to the post-mount state before returning. This hook separates
    /// benchmark commands from semantically similar correctness steps without exposing runtime
    /// structure to the shared runner. It is called at most once per session.
    void beginBenchmark();

    /// Returns candidate-owned live-resource counts.
    ///
    /// This method remains valid after [#close()] so the runner can enforce cleanup.
    ///
    /// @return the health snapshot
    RuntimeHealth health();

    /// Releases the fixture and every owned resource.
    ///
    /// Repeated calls must have no effect.
    @Override
    void close();
}

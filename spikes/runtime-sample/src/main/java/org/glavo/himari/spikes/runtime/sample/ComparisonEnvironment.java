package org.glavo.himari.spikes.runtime.sample;

import org.glavo.himari.platform.headless.HeadlessBackend;
import org.glavo.himari.platform.headless.HeadlessPlatform;
import org.glavo.himari.state.StateDomain;
import org.jetbrains.annotations.NotNullByDefault;

import java.util.concurrent.CompletionStage;

/// Owns the fresh state domain and deterministic Headless host used by one fixture attempt.
///
/// The environment is confined to the thread that opens it. Commands run synchronously on that
/// thread, and the runner drains the Headless event loop after every command before observing state.
@NotNullByDefault
public final class ComparisonEnvironment implements AutoCloseable {
    /// The application state and reactive graph domain.
    private final StateDomain stateDomain;

    /// The deterministic Headless platform session.
    private final HeadlessPlatform platform;

    /// Whether closure completed.
    private boolean closed;

    /// Creates an environment from already initialized components.
    ///
    /// @param stateDomain the fresh state domain
    /// @param platform the fresh Headless session
    private ComparisonEnvironment(StateDomain stateDomain, HeadlessPlatform platform) {
        this.stateDomain = stateDomain;
        this.platform = platform;
    }

    /// Opens a fresh environment at manual timestamp zero on the calling thread.
    ///
    /// @return the environment
    public static ComparisonEnvironment open() {
        StateDomain domain = new StateDomain();
        HeadlessPlatform platform = new HeadlessBackend().open().toCompletableFuture().join();
        return new ComparisonEnvironment(domain, platform);
    }

    /// Returns the fixture's state domain.
    ///
    /// @return the state domain
    /// @throws IllegalStateException if the environment is closed
    public StateDomain stateDomain() {
        checkOpen();
        return stateDomain;
    }

    /// Returns the fixture's Headless platform session.
    ///
    /// @return the Headless session
    /// @throws IllegalStateException if the environment is closed
    public HeadlessPlatform platform() {
        checkOpen();
        return platform;
    }

    /// Drains all Headless work ready at the current manual timestamp.
    ///
    /// @return the number of callbacks begun
    /// @throws IllegalStateException if the environment is closed or called off the owner thread
    public long drain() {
        checkOpen();
        return platform.eventLoop().runUntilIdle();
    }

    /// Advances manual time and drains newly ready work.
    ///
    /// @param deltaNanos the nonnegative duration
    /// @return the number of callbacks begun
    /// @throws IllegalArgumentException if `deltaNanos` is negative
    /// @throws IllegalStateException if the environment is closed or called off the owner thread
    public long advanceBy(long deltaNanos) {
        checkOpen();
        return platform.eventLoop().advanceBy(deltaNanos);
    }

    /// Closes the Headless session and drains its ordered shutdown work.
    ///
    /// Closure is idempotent.
    @Override
    public void close() {
        if (closed) {
            return;
        }
        platform.eventLoop().checkOwnerThread();
        CompletionStage<Void> completion = platform.closeAsync();
        platform.eventLoop().runUntilIdle();
        completion.toCompletableFuture().join();
        closed = true;
    }

    /// Verifies that this environment still accepts fixture work.
    ///
    /// @throws IllegalStateException if closure completed
    private void checkOpen() {
        if (closed) {
            throw new IllegalStateException("Comparison environment is closed");
        }
        platform.eventLoop().checkOwnerThread();
    }
}

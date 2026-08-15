package org.glavo.himari.state;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Objects;

/// Holds one detached synchronous dependency capture for a [ReactiveObserver].
///
/// An observation owns no graph edges until [#commit()] succeeds. Closing an uncommitted
/// observation discards it. Each observation may resolve exactly once and cannot be committed after
/// its observer changes revision, clears dependencies, or closes.
@NotNullByDefault
public final class ReactiveObservation implements AutoCloseable {
    /// The observer that created this capture.
    private final ReactiveObserver observer;

    /// The unique producers and versions captured in first-read order.
    private final @Unmodifiable List<ReactiveDependency> dependencies;

    /// The observer revision at capture time.
    private final long captureRevision;

    /// Whether this observation has committed or been discarded.
    private boolean resolved;

    /// Creates one detached observation.
    ///
    /// @param observer the creating observer
    /// @param dependencies the immutable captured dependencies
    /// @param captureRevision the observer revision at capture time
    ReactiveObservation(
            ReactiveObserver observer,
            @Unmodifiable List<ReactiveDependency> dependencies,
            long captureRevision
    ) {
        this.observer = Objects.requireNonNull(observer, "observer");
        this.dependencies = List.copyOf(dependencies);
        if (captureRevision < 0L) {
            throw new IllegalArgumentException("captureRevision must be nonnegative");
        }
        this.captureRevision = captureRevision;
    }

    /// Replaces the observer's committed dependency set with this capture.
    ///
    /// A successful call resolves this observation. A failed call leaves it unresolved and leaves
    /// the observer's previous dependencies unchanged.
    ///
    /// @throws IllegalStateException if this observation is already resolved, its observer changed
    /// revision, a dependency version changed, or graph mutation is not currently permitted
    public void commit() {
        checkUnresolved();
        observer.commit(this);
        resolved = true;
    }

    /// Returns whether this observation has committed or been discarded.
    ///
    /// @return whether no further commit is permitted
    public boolean isResolved() {
        return resolved;
    }

    /// Discards this observation without changing the observer or graph.
    ///
    /// Closing an already resolved observation has no effect.
    @Override
    public void close() {
        resolved = true;
    }

    /// Returns the creating observer for package-level commit validation.
    ///
    /// @return the creating observer
    ReactiveObserver observer() {
        return observer;
    }

    /// Returns the captured dependency list for package-level commit application.
    ///
    /// @return the immutable dependencies
    @Unmodifiable List<ReactiveDependency> dependencies() {
        return dependencies;
    }

    /// Returns the observer revision captured by this observation.
    ///
    /// @return the nonnegative revision
    long captureRevision() {
        return captureRevision;
    }

    /// Rejects a second commit attempt.
    private void checkUnresolved() {
        if (resolved) {
            throw new IllegalStateException("Reactive observation is already resolved");
        }
    }
}

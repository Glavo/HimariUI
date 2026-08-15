package org.glavo.himari.state;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;

/// Owns one leaf consumer's committed reactive dependencies.
///
/// An observer separates synchronous dependency capture from graph mutation. [#capture(Runnable)]
/// runs code under this observer and returns a detached [ReactiveObservation]. Committing that
/// observation replaces the observer's dependency edges atomically from the caller's perspective;
/// closing it discards the capture without changing the graph. This permits a UI runtime to stage
/// reactive reads together with topology and publish them only after the complete UI attempt is
/// known to be valid.
///
/// All methods except graph-driven invalidation are confined to the owning [StateDomain] thread.
/// Source publication only marks this observer for version polling and never executes application
/// code.
@NotNullByDefault
public final class ReactiveObserver implements AutoCloseable {
    /// The owner that bounds this observer's lifetime.
    private final ReactiveOwner owner;

    /// The graph node attached to observed producers.
    private final ObserverConsumerNode consumerNode;

    /// The committed dependencies in first-read order.
    private @Unmodifiable List<ReactiveDependency> dependencies = List.of();

    /// Whether a producer notification requires dependency-version polling.
    private boolean checkRequired = true;

    /// Whether this observer is currently capturing synchronous reads.
    private boolean capturing;

    /// Whether this observer has released its dependency edges.
    private boolean disposed;

    /// The mutation revision used to reject stale observation commits.
    private long revision;

    /// Creates an initially invalidated observer owned by one reactive owner.
    ///
    /// @param owner the lifetime owner
    ReactiveObserver(ReactiveOwner owner) {
        this.owner = Objects.requireNonNull(owner, "owner");
        this.consumerNode = new ObserverConsumerNode(this, owner.graph());
    }

    /// Captures every unique producer read by one synchronous action.
    ///
    /// Capture temporarily becomes the innermost reactive consumer on the current thread. Nested
    /// observers receive their own reads; after they return, this observer resumes collection.
    /// A thrown exception or error restores the enclosing capture and leaves this observer's
    /// committed dependencies unchanged.
    ///
    /// @param action the synchronous computation whose reads are captured
    /// @return a detached observation that must be committed or closed
    /// @throws IllegalStateException if called off the owner thread, after disposal, reentrantly on
    /// this observer, from a state transaction, or from a derived computation
    public ReactiveObservation capture(Runnable action) {
        Objects.requireNonNull(action, "action");
        owner.graph().checkOwnershipMutationAllowed();
        checkOpen();
        if (capturing) {
            throw new IllegalStateException("Reactive observer capture cannot be reentered");
        }

        capturing = true;
        ReactiveTracking.Capture capture = ReactiveTracking.begin(consumerNode);
        try {
            action.run();
            return new ReactiveObservation(this, capture.finish(), revision);
        } finally {
            ReactiveTracking.end(capture);
            capturing = false;
        }
    }

    /// Returns whether at least one committed dependency changed semantically.
    ///
    /// A pushed invalidation first requires polling. This method pulls invalidated derived
    /// dependencies and suppresses the observer invalidation when every semantic version remains
    /// unchanged. An observer with no committed observation remains invalidated.
    ///
    /// @return whether the consumer callback must execute
    /// @throws IllegalStateException if called off the owner thread or after disposal
    public boolean isInvalidated() {
        owner.graph().domain().checkOwnerThread();
        checkOpen();
        if (!checkRequired) {
            return false;
        }
        if (dependencies.isEmpty()) {
            return true;
        }
        for (ReactiveDependency dependency : dependencies) {
            ReactiveProducerNode producer = dependency.producer();
            producer.ensureCurrent();
            if (producer.semanticVersion() != dependency.observedVersion()) {
                return true;
            }
        }
        checkRequired = false;
        return false;
    }

    /// Detaches every committed dependency while keeping the observer reusable.
    ///
    /// The observer becomes invalidated so a later activation must execute and establish a fresh
    /// dependency set. Repeated calls are permitted.
    ///
    /// @throws IllegalStateException if called off the owner thread, during capture, from a state
    /// transaction, from a derived computation, or after disposal
    public void clearDependencies() {
        owner.graph().checkOwnershipMutationAllowed();
        checkOpen();
        if (capturing) {
            throw new IllegalStateException("Reactive observer dependencies cannot clear during capture");
        }
        long nextRevision = Math.incrementExact(revision);
        detachAll();
        checkRequired = true;
        revision = nextRevision;
    }

    /// Returns whether this observer has completed disposal.
    ///
    /// @return whether the observer is disposed
    public boolean isDisposed() {
        owner.graph().domain().checkOwnerThread();
        return disposed;
    }

    /// Disposes this observer and detaches every committed producer edge.
    ///
    /// Closure is idempotent.
    ///
    /// @throws IllegalStateException if called off the owner thread, during capture, from a state
    /// transaction, or from a derived computation
    @Override
    public void close() {
        if (disposed) {
            owner.graph().domain().checkOwnerThread();
            return;
        }
        if (capturing) {
            throw new IllegalStateException("Reactive observer cannot close during capture");
        }
        owner.disposeObserver(this);
    }

    /// Installs one detached observation after validating its capture revision and versions.
    ///
    /// @param observation the observation to publish
    void commit(ReactiveObservation observation) {
        owner.graph().checkOwnershipMutationAllowed();
        checkOpen();
        if (capturing) {
            throw new IllegalStateException("Reactive observation cannot commit during capture");
        }
        if (observation.observer() != this) {
            throw new IllegalArgumentException("Reactive observation belongs to another observer");
        }
        if (observation.captureRevision() != revision) {
            throw new IllegalStateException("Reactive observation was captured from a stale observer revision");
        }

        @Unmodifiable List<ReactiveDependency> nextDependencies = observation.dependencies();
        for (ReactiveDependency dependency : nextDependencies) {
            ReactiveProducerNode producer = dependency.producer();
            producer.ensureCurrent();
            if (producer.semanticVersion() != dependency.observedVersion()) {
                throw new IllegalStateException("Reactive observation became stale before commit");
            }
        }

        long nextRevision = Math.incrementExact(revision);
        IdentityHashMap<ReactiveProducerNode, Boolean> next = new IdentityHashMap<>();
        for (ReactiveDependency dependency : nextDependencies) {
            next.put(dependency.producer(), Boolean.TRUE);
        }
        for (ReactiveDependency dependency : dependencies) {
            if (!next.containsKey(dependency.producer())) {
                dependency.producer().removeConsumer(consumerNode);
            }
        }
        IdentityHashMap<ReactiveProducerNode, Boolean> previous = new IdentityHashMap<>();
        for (ReactiveDependency dependency : dependencies) {
            previous.put(dependency.producer(), Boolean.TRUE);
        }
        for (ReactiveDependency dependency : nextDependencies) {
            if (!previous.containsKey(dependency.producer())) {
                dependency.producer().addConsumer(consumerNode);
            }
        }
        dependencies = nextDependencies;
        checkRequired = false;
        revision = nextRevision;
    }

    /// Marks this observer for later dependency-version polling.
    ///
    /// @return whether this call changed a clean observer to check-required
    boolean markCheckRequired() {
        if (disposed || checkRequired) {
            return false;
        }
        checkRequired = true;
        return true;
    }

    /// Disposes this observer while its owner is already performing child-first teardown.
    void disposeFromOwner() {
        if (disposed) {
            return;
        }
        if (capturing) {
            throw new IllegalStateException("Reactive observer cannot dispose during capture");
        }
        long nextRevision = Math.incrementExact(revision);
        detachAll();
        disposed = true;
        checkRequired = false;
        revision = nextRevision;
    }

    /// Detaches all producer edges and clears the dependency list.
    private void detachAll() {
        for (ReactiveDependency dependency : dependencies) {
            dependency.producer().removeConsumer(consumerNode);
        }
        dependencies = List.of();
    }

    /// Verifies that this observer remains active.
    private void checkOpen() {
        if (disposed) {
            throw new IllegalStateException("Reactive observer is disposed");
        }
    }

    /// Adapts an observer to the graph's internal leaf-consumer protocol.
    @NotNullByDefault
    private static final class ObserverConsumerNode extends ReactiveConsumerNode {
        /// The application-visible observer whose dirty bit this node owns.
        private final ReactiveObserver observer;

        /// Creates one leaf node.
        ///
        /// @param observer the observer to invalidate
        /// @param graph the owning graph
        private ObserverConsumerNode(ReactiveObserver observer, ReactiveGraph graph) {
            super(graph);
            this.observer = observer;
        }

        /// {@inheritDoc}
        @Override
        boolean markCheckRequired() {
            return observer.markCheckRequired();
        }

        /// {@inheritDoc}
        @Override
        @Nullable ReactiveProducerNode downstreamProducer() {
            return null;
        }
    }
}

package org.glavo.himari.state;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.Objects;

/// Owns one application's source-state slots, publication epochs, and external commit queue.
///
/// A domain is bound to the thread that constructs it. State creation, state mutation, transaction
/// execution, and external-queue draining must occur on that owner thread. Published reads,
/// [#epoch()], [#snapshot()], and external enqueue operations are safe from other threads.
///
/// Each successful transaction that changes at least one semantic value publishes one immutable
/// state set and advances the epoch exactly once. Source creation does not advance the epoch.
@NotNullByDefault
public final class StateDomain {
    /// The thread on which mutations and commits are permitted.
    private final Thread ownerThread;

    /// The latest immutable publication, replaced atomically on commit.
    private volatile StatePublication publication;

    /// The fine-grained reactive graph fed by source publications.
    private final ReactiveGraph reactiveGraph;

    /// The queue through which non-owner callbacks submit state work.
    private final ExternalStateCommitQueue externalCommits;

    /// Whether the owner thread is comparing staged values or replacing the current publication.
    private boolean committing;

    /// Creates an empty domain owned by the current thread.
    public StateDomain() {
        this.ownerThread = Thread.currentThread();
        this.publication = StatePublication.empty();
        this.reactiveGraph = new ReactiveGraph(this);
        this.externalCommits = new ExternalStateCommitQueue(this);
    }

    /// Creates a non-null object state at version zero.
    ///
    /// Source creation must not occur inside a state transaction because registration itself is not
    /// transactional.
    ///
    /// @param initialValue the non-null initial value
    /// @param <T> the value type
    /// @return a mutable source owned by this domain
    /// @throws IllegalStateException if called outside the owner thread or inside a state transaction
    /// @throws NullPointerException if `initialValue` is `null`
    public <T> MutableState<T> mutableState(T initialValue) {
        return new ObjectMutableState<>(this, Objects.requireNonNull(initialValue, "initialValue"), false);
    }

    /// Creates a nullable object state at version zero.
    ///
    /// Source creation must not occur inside a state transaction because registration itself is not
    /// transactional.
    ///
    /// @param initialValue the initial value, which may be `null`
    /// @param <T> the non-null portion of the value type
    /// @return a nullable mutable source owned by this domain
    /// @throws IllegalStateException if called outside the owner thread or inside a state transaction
    public <T> MutableState<@Nullable T> nullableState(@Nullable T initialValue) {
        return new ObjectMutableState<>(this, initialValue, true);
    }

    /// Creates an integer state at version zero.
    ///
    /// @param initialValue the initial value
    /// @return a mutable integer source owned by this domain
    /// @throws IllegalStateException if called outside the owner thread or inside a state transaction
    public IntState intState(int initialValue) {
        return new IntState(this, initialValue);
    }

    /// Creates a long state at version zero.
    ///
    /// @param initialValue the initial value
    /// @return a mutable long source owned by this domain
    /// @throws IllegalStateException if called outside the owner thread or inside a state transaction
    public LongState longState(long initialValue) {
        return new LongState(this, initialValue);
    }

    /// Creates a float state at version zero.
    ///
    /// @param initialValue the initial value
    /// @return a mutable float source owned by this domain
    /// @throws IllegalStateException if called outside the owner thread or inside a state transaction
    public FloatState floatState(float initialValue) {
        return new FloatState(this, initialValue);
    }

    /// Creates a boolean state at version zero.
    ///
    /// @param initialValue the initial value
    /// @return a mutable boolean source owned by this domain
    /// @throws IllegalStateException if called outside the owner thread or inside a state transaction
    public BooleanState booleanState(boolean initialValue) {
        return new BooleanState(this, initialValue);
    }

    /// Returns the latest published domain epoch.
    ///
    /// @return the latest epoch
    public long epoch() {
        return publication.epoch();
    }

    /// Captures an immutable view of all sources registered at the capture point.
    ///
    /// A snapshot remains stable across later commits and may be read from any thread. It represents
    /// only published state; values staged by an active transaction are excluded.
    ///
    /// @return the captured publication
    public StateSnapshot snapshot() {
        return new StateSnapshot(this, publication);
    }

    /// Returns the queue used to transfer state work from external callbacks to the owner thread.
    ///
    /// @return this domain's external commit queue
    public ExternalStateCommitQueue externalCommits() {
        return externalCommits;
    }

    /// Returns the fine-grained reactive graph owned by this application domain.
    ///
    /// @return this domain's reactive graph
    public ReactiveGraph reactiveGraph() {
        return reactiveGraph;
    }

    /// Returns whether the calling thread is this domain's owner thread.
    ///
    /// @return whether the caller owns the domain
    public boolean isOwnerThread() {
        return Thread.currentThread() == ownerThread;
    }

    /// Verifies that the calling thread owns this domain.
    ///
    /// @throws IllegalStateException if called from another thread
    public void checkOwnerThread() {
        if (!isOwnerThread()) {
            throw new IllegalStateException(
                    "State domain is owned by thread '" + ownerThread.getName()
                            + "' but was accessed from '" + Thread.currentThread().getName() + "'"
            );
        }
    }

    /// Returns whether the owner thread is currently executing a transaction for this domain.
    ///
    /// This method is intended for owner-context lifecycle checks that must not begin an independent
    /// publication boundary inside an existing transaction.
    ///
    /// @return whether this domain has an active transaction on the calling owner thread
    /// @throws IllegalStateException if called outside the owner thread
    public boolean hasActiveTransaction() {
        checkOwnerThread();
        return StateTransaction.isActive(this);
    }

    /// Returns the latest publication.
    ///
    /// @return the current immutable publication
    StatePublication currentPublication() {
        return publication;
    }

    /// Registers one source without advancing the epoch.
    ///
    /// @param initialValue the initial value, which may be `null`
    /// @return the source's stable publication slot
    int registerSource(@Nullable Object initialValue) {
        checkOwnerThread();
        checkWriteAllowed();
        StateTransaction.checkRegistrationAllowed(this);
        StatePublication current = publication;
        int slot = current.size();
        publication = current.append(initialValue);
        return slot;
    }

    /// Publishes the semantic changes staged by an outermost transaction.
    ///
    /// @param stagedValues the final staged values by source identity
    /// @return the resulting epoch, which is unchanged when no value changed semantically
    long commit(IdentityHashMap<AbstractStateSource, @Nullable Object> stagedValues) {
        checkOwnerThread();
        if (stagedValues.isEmpty()) {
            return publication.epoch();
        }

        checkWriteAllowed();
        committing = true;
        try {
            StatePublication current = publication;
            IdentityHashMap<AbstractStateSource, @Nullable Object> changes = new IdentityHashMap<>();
            for (var entry : stagedValues.entrySet()) {
                AbstractStateSource source = entry.getKey();
                if (source.owningDomain() != this) {
                    throw new IllegalArgumentException("Transaction contains a source from another domain");
                }
                @Nullable Object oldValue = current.value(source.slot());
                @Nullable Object newValue = entry.getValue();
                if (!source.valuesEqual(oldValue, newValue)) {
                    changes.put(source, newValue);
                }
            }
            if (changes.isEmpty()) {
                return current.epoch();
            }
            if (current.epoch() == Long.MAX_VALUE) {
                throw new IllegalStateException("State domain epoch is exhausted");
            }
            publication = current.publish(changes, current.epoch() + 1L);
            reactiveGraph.sourcesChanged(changes);
            return publication.epoch();
        } finally {
            committing = false;
        }
    }

    /// Verifies that no commit-time equality or publication operation is currently running.
    ///
    /// @throws IllegalStateException if a write would reenter commit processing
    void checkWriteAllowed() {
        if (committing) {
            throw new IllegalStateException("State writes cannot reenter commit processing");
        }
        reactiveGraph.checkStateWriteAllowed();
        ReactiveTracking.checkStateWriteAllowed();
    }
}

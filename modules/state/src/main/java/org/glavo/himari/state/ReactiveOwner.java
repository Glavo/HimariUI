package org.glavo.himari.state;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Supplier;

/// Owns derived computations and nested owners independently of mounted-tree representation.
///
/// An owner retains its computations until explicit disposal. Closing is idempotent, closes child
/// owners from newest to oldest, then disposes leaf observers and derived states from newest to
/// oldest, detaching every upstream dependency edge. Creating, closing, or mutating ownership must
/// occur on the associated [StateDomain] owner thread and outside derived computation.
@NotNullByDefault
public final class ReactiveOwner implements AutoCloseable {
    /// The graph containing this owner and its computations.
    private final ReactiveGraph graph;

    /// The enclosing owner, or `null` for a root owner.
    private final @Nullable ReactiveOwner parent;

    /// The active child owners in creation order.
    private final ArrayList<ReactiveOwner> children = new ArrayList<>();

    /// The active directly owned derived states in creation order.
    private final ArrayList<DerivedState<?>> derivedStates = new ArrayList<>();

    /// The active directly owned leaf observers in creation order.
    private final ArrayList<ReactiveObserver> observers = new ArrayList<>();

    /// Whether this owner has completed disposal.
    private boolean disposed;

    /// Creates an owner attached to a graph or parent.
    ///
    /// @param graph the owning graph
    /// @param parent the parent owner, or `null`
    ReactiveOwner(ReactiveGraph graph, @Nullable ReactiveOwner parent) {
        this.graph = graph;
        this.parent = parent;
    }

    /// Returns the graph containing this owner.
    ///
    /// @return the owning graph
    public ReactiveGraph graph() {
        return graph;
    }

    /// Creates a child whose lifetime is bounded by this owner.
    ///
    /// @return the new active child
    /// @throws IllegalStateException if this owner is disposed, the caller is not on the domain
    /// owner thread, or a derived computation is active
    public ReactiveOwner createChild() {
        checkCanCreate();
        ReactiveOwner child = new ReactiveOwner(graph, this);
        children.add(child);
        return child;
    }

    /// Creates a non-null derived state with structural equality.
    ///
    /// @param computation the side-effect-free synchronous computation
    /// @param <T> the result type
    /// @return the uninitialized derived state
    public <T> DerivedState<T> derivedState(Supplier<? extends T> computation) {
        return derivedState(computation, EqualityPolicy.structural());
    }

    /// Creates a non-null derived state with an explicit equality policy.
    ///
    /// @param computation the side-effect-free synchronous computation
    /// @param equalityPolicy the semantic equality policy
    /// @param <T> the result type
    /// @return the uninitialized derived state
    public <T> DerivedState<T> derivedState(
            Supplier<? extends T> computation,
            EqualityPolicy<? super T> equalityPolicy
    ) {
        checkCanCreate();
        DerivedState<T> state = new DerivedState<>(
                this,
                Objects.requireNonNull(computation, "computation"),
                Objects.requireNonNull(equalityPolicy, "equalityPolicy"),
                false
        );
        derivedStates.add(state);
        return state;
    }

    /// Creates a nullable derived state with structural equality.
    ///
    /// @param computation the side-effect-free synchronous computation
    /// @param <T> the non-null portion of the result type
    /// @return the uninitialized nullable derived state
    public <T> DerivedState<@Nullable T> nullableDerivedState(Supplier<@Nullable T> computation) {
        return nullableDerivedState(computation, EqualityPolicy.structural());
    }

    /// Creates a nullable derived state with an explicit equality policy.
    ///
    /// @param computation the side-effect-free synchronous computation
    /// @param equalityPolicy the semantic equality policy
    /// @param <T> the non-null portion of the result type
    /// @return the uninitialized nullable derived state
    public <T> DerivedState<@Nullable T> nullableDerivedState(
            Supplier<@Nullable T> computation,
            EqualityPolicy<@Nullable T> equalityPolicy
    ) {
        checkCanCreate();
        DerivedState<@Nullable T> state = new DerivedState<>(
                this,
                Objects.requireNonNull(computation, "computation"),
                Objects.requireNonNull(equalityPolicy, "equalityPolicy"),
                true
        );
        derivedStates.add(state);
        return state;
    }

    /// Creates a leaf observer whose captured dependencies can be committed transactionally.
    ///
    /// The observer initially reports itself invalidated and owns no dependency edges. Capturing
    /// reads does not mutate the graph; the returned [ReactiveObservation] installs them only when
    /// explicitly committed.
    ///
    /// @return the new active observer
    /// @throws IllegalStateException if this owner is disposed, the caller is not on the domain
    /// owner thread, a state transaction is active, or a derived computation is active
    public ReactiveObserver createObserver() {
        checkCanCreate();
        ReactiveObserver observer = new ReactiveObserver(this);
        observers.add(observer);
        return observer;
    }

    /// Returns whether this owner has completed disposal.
    ///
    /// @return whether the owner is disposed
    public boolean isDisposed() {
        graph.domain().checkOwnerThread();
        return disposed;
    }

    /// Disposes this owner, all child owners, and all directly owned derived states.
    ///
    /// Repeated calls have no effect.
    ///
    /// @throws IllegalStateException if called outside the domain owner thread or during derived
    /// computation
    @Override
    public void close() {
        graph.checkOwnershipMutationAllowed();
        disposeTree();
    }

    /// Removes and disposes one directly owned derived state.
    ///
    /// @param state the state requesting individual disposal
    void disposeDerived(DerivedState<?> state) {
        graph.checkOwnershipMutationAllowed();
        if (derivedStates.remove(state)) {
            state.disposeFromOwner();
        }
    }

    /// Removes and disposes one directly owned observer.
    ///
    /// @param observer the observer requesting individual disposal
    void disposeObserver(ReactiveObserver observer) {
        graph.checkOwnershipMutationAllowed();
        if (observers.remove(observer)) {
            observer.disposeFromOwner();
        }
    }

    /// Recursively disposes this owner without repeating public precondition checks.
    private void disposeTree() {
        if (disposed) {
            return;
        }
        disposed = true;
        while (!children.isEmpty()) {
            children.getLast().disposeTree();
        }
        while (!observers.isEmpty()) {
            ReactiveObserver observer = observers.removeLast();
            observer.disposeFromOwner();
        }
        while (!derivedStates.isEmpty()) {
            DerivedState<?> state = derivedStates.removeLast();
            state.disposeFromOwner();
        }
        if (parent == null) {
            graph.removeRootOwner(this);
        } else {
            parent.children.remove(this);
        }
    }

    /// Verifies creation preconditions.
    ///
    /// @throws IllegalStateException if ownership cannot be extended
    private void checkCanCreate() {
        graph.checkOwnershipMutationAllowed();
        if (disposed) {
            throw new IllegalStateException("Reactive owner is disposed");
        }
    }
}

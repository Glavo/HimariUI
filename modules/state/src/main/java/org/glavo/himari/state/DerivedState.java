package org.glavo.himari.state;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/// Lazily derives a cached semantic value from dynamically discovered reactive dependencies.
///
/// A source commit only marks this state as requiring dependency-version polling. Pulling through
/// [#get()] first stabilizes upstream derived states. The computation executes only when at least
/// one observed semantic version changed, on first use, or after a failed attempt. A successful
/// equal result keeps both the previous cached object and semantic version. A failed attempt keeps
/// the last successful value, version, and dependency edges but throws to the caller; every later
/// pull retries until one attempt succeeds.
///
/// Reads made inside a [StateTransaction] execute the computation against transaction-visible
/// values without changing this cache, version, or graph edges. The state remains usable until its
/// [ReactiveOwner] or this state is closed.
///
/// @param <T> the result type
@NotNullByDefault
public final class DerivedState<T> extends ReactiveProducerNode implements State<T>, AutoCloseable {
    /// The owner responsible for this state's lifetime, cleared on disposal.
    private @Nullable ReactiveOwner owner;

    /// The generic consumer node attached to dependency producers.
    private final DerivedConsumerNode consumerNode;

    /// The computation, cleared on disposal to release captured application objects.
    private @Nullable Supplier<? extends T> computation;

    /// The equality policy, cleared on disposal to release captured application objects.
    private @Nullable EqualityPolicy<? super T> equalityPolicy;

    /// Whether successful computation may return `null`.
    private final boolean nullable;

    /// The dependencies installed by the last successful computation.
    private @Unmodifiable List<ReactiveDependency> dependencies = List.of();

    /// The last successful cached value, or `null` before initialization or for a nullable result.
    private @Nullable T cachedValue;

    /// The monotonically advancing semantic version.
    private long version;

    /// Whether a successful value has been cached.
    private boolean initialized;

    /// Whether dependencies must be pulled and their versions polled.
    private boolean checkRequired = true;

    /// Whether the previous attempted computation failed and must be retried unconditionally.
    private boolean retryRequired;

    /// Whether this state has been disposed.
    private boolean disposed;

    /// Creates an uninitialized derived state owned by one graph scope.
    ///
    /// @param owner the lifetime owner
    /// @param computation the synchronous computation
    /// @param equalityPolicy the semantic equality policy
    /// @param nullable whether `null` is a valid result
    DerivedState(
            ReactiveOwner owner,
            Supplier<? extends T> computation,
            EqualityPolicy<? super T> equalityPolicy,
            boolean nullable
    ) {
        super(owner.graph(), owner.graph().allocateDerivedName());
        this.owner = owner;
        this.consumerNode = new DerivedConsumerNode(this);
        this.computation = computation;
        this.equalityPolicy = equalityPolicy;
        this.nullable = nullable;
    }

    /// Returns the current derived value and records this producer in an enclosing computation.
    ///
    /// @return the transaction-local ephemeral value, or the current cached semantic value
    /// @throws IllegalStateException if called outside the domain owner thread or after disposal
    /// @throws ReactiveCycleException if pulling encounters a dependency cycle
    @Override
    public T get() {
        graph().domain().checkOwnerThread();
        checkUsable();
        if (StateTransaction.isActive(graph().domain())) {
            return computeEphemeral();
        }
        ensureCurrent();
        ReactiveTracking.recordRead(this);
        return currentCachedValue();
    }

    /// Returns the owning state domain.
    ///
    /// @return the owning domain
    @Override
    public StateDomain domain() {
        return graph().domain();
    }

    /// Pulls this state and returns its current semantic version.
    ///
    /// Inside a state transaction this method returns the last published semantic version without
    /// evaluating transaction-visible values or changing the cache.
    ///
    /// @return the current semantic version
    /// @throws IllegalStateException if called outside the domain owner thread or after disposal
    /// @throws ReactiveCycleException if pulling encounters a dependency cycle
    @Override
    public long version() {
        graph().domain().checkOwnerThread();
        checkUsable();
        if (!StateTransaction.isActive(graph().domain())) {
            ensureCurrent();
        }
        return version;
    }

    /// Returns the stable graph-allocated diagnostic name.
    ///
    /// @return the diagnostic name
    public String debugName() {
        return diagnosticName();
    }

    /// Returns whether a successful value has been cached.
    ///
    /// Transaction-local ephemeral evaluation does not initialize this state.
    ///
    /// @return whether the persistent cache is initialized
    public boolean isInitialized() {
        graph().domain().checkOwnerThread();
        return initialized;
    }

    /// Returns whether a future pull must poll or recompute this state.
    ///
    /// @return whether the state is invalidated or awaiting failure retry
    public boolean isDirty() {
        graph().domain().checkOwnerThread();
        return checkRequired || retryRequired;
    }

    /// Returns whether this state has been disposed.
    ///
    /// @return whether the state is disposed
    public boolean isDisposed() {
        graph().domain().checkOwnerThread();
        return disposed;
    }

    /// Disposes this state and detaches it from its last successful dependencies.
    ///
    /// Repeated calls have no effect. Direct consumers are invalidated before this producer releases
    /// its references; pulling such a consumer will then fail when it reaches the disposed state.
    ///
    /// @throws IllegalStateException if called outside the owner thread or during derived computation
    @Override
    public void close() {
        @Nullable ReactiveOwner currentOwner = owner;
        if (currentOwner != null) {
            currentOwner.disposeDerived(this);
        }
    }

    /// Pulls dependencies and recomputes when required.
    @Override
    void ensureCurrent() {
        graph().domain().checkOwnerThread();
        checkUsable();
        graph().checkNotEvaluating(this);
        if (StateTransaction.isActive(graph().domain())) {
            throw new IllegalStateException("Persistent derived-state pull cannot run inside a state transaction");
        }
        if (!initialized || retryRequired) {
            recompute();
            return;
        }
        if (!checkRequired) {
            return;
        }
        for (ReactiveDependency dependency : dependencies) {
            ReactiveProducerNode producer = dependency.producer();
            producer.ensureCurrent();
            if (producer.semanticVersion() != dependency.observedVersion()) {
                recompute();
                return;
            }
        }
        checkRequired = false;
    }

    /// Returns the current semantic version without initiating a pull.
    ///
    /// @return the semantic version
    @Override
    long semanticVersion() {
        return version;
    }

    /// Marks this state for dependency polling if it was previously clean.
    ///
    /// @return whether the state changed from clean to check-required
    boolean markCheckRequired() {
        if (disposed || checkRequired) {
            return false;
        }
        checkRequired = true;
        return true;
    }

    /// Executes and atomically installs one successful persistent recomputation.
    private void recompute() {
        graph().enterEvaluation(this);
        ReactiveTracking.Capture capture = ReactiveTracking.begin(consumerNode);
        try {
            T nextValue = requireComputation().get();
            validateResult(nextValue);
            @Unmodifiable List<ReactiveDependency> nextDependencies = capture.finish();

            boolean changed = initialized
                    && !requireEqualityPolicy().equivalent(currentCachedValue(), nextValue);
            if (changed && version == Long.MAX_VALUE) {
                throw new IllegalStateException("Derived state semantic version is exhausted");
            }

            installDependencies(nextDependencies);
            if (!initialized || changed) {
                cachedValue = nextValue;
            }
            if (changed) {
                version++;
            }
            initialized = true;
            checkRequired = false;
            retryRequired = false;

            if (changed) {
                graph().derivedChanged(this);
            }
        } catch (RuntimeException | Error failure) {
            retryRequired = true;
            checkRequired = true;
            throw failure;
        } finally {
            ReactiveTracking.end(capture);
            graph().exitEvaluation(this);
        }
    }

    /// Computes a transaction-visible value without changing persistent reactive state.
    ///
    /// @return the ephemeral value
    private T computeEphemeral() {
        graph().enterEvaluation(this);
        try {
            T value = requireComputation().get();
            validateResult(value);
            return value;
        } finally {
            graph().exitEvaluation(this);
        }
    }

    /// Reconciles old and newly captured dependencies by producer identity.
    ///
    /// @param nextDependencies the dependencies from a successful computation
    private void installDependencies(@Unmodifiable List<ReactiveDependency> nextDependencies) {
        IdentityHashMap<ReactiveProducerNode, Boolean> nextSet = new IdentityHashMap<>();
        for (ReactiveDependency dependency : nextDependencies) {
            nextSet.put(dependency.producer(), Boolean.TRUE);
        }
        IdentityHashMap<ReactiveProducerNode, Boolean> oldSet = new IdentityHashMap<>();
        for (ReactiveDependency dependency : dependencies) {
            ReactiveProducerNode producer = dependency.producer();
            oldSet.put(producer, Boolean.TRUE);
            if (!nextSet.containsKey(producer)) {
                producer.removeConsumer(consumerNode);
            }
        }
        for (ReactiveDependency dependency : nextDependencies) {
            ReactiveProducerNode producer = dependency.producer();
            if (!oldSet.containsKey(producer)) {
                producer.addConsumer(consumerNode);
            }
        }
        dependencies = nextDependencies;
    }

    /// Returns the current computation after checking disposal state.
    ///
    /// @return the computation
    private Supplier<? extends T> requireComputation() {
        return Objects.requireNonNull(computation, "Derived state is disposed");
    }

    /// Returns the current equality policy after checking disposal state.
    ///
    /// @return the equality policy
    private EqualityPolicy<? super T> requireEqualityPolicy() {
        return Objects.requireNonNull(equalityPolicy, "Derived state is disposed");
    }

    /// Returns the successful cached value.
    ///
    /// @return the cached value
    /// @throws IllegalStateException if no successful value exists
    private T currentCachedValue() {
        if (!initialized) {
            throw new IllegalStateException("Derived state has no successful cached value");
        }
        return cachedValue;
    }

    /// Enforces this state's runtime null policy.
    ///
    /// @param value the computed value, which may be `null` for a nullable state
    private void validateResult(@Nullable T value) {
        if (!nullable) {
            Objects.requireNonNull(value, "Derived computation returned null");
        }
    }

    /// Verifies that this state remains active.
    ///
    /// @throws IllegalStateException if this state is disposed
    private void checkUsable() {
        if (disposed) {
            throw new IllegalStateException("Derived state " + diagnosticName() + " is disposed");
        }
    }

    /// Disposes this state after its owner has removed the ownership reference.
    void disposeFromOwner() {
        if (disposed) {
            return;
        }
        disposed = true;
        graph().derivedChanged(this);
        clearConsumers();
        for (ReactiveDependency dependency : dependencies) {
            dependency.producer().removeConsumer(consumerNode);
        }
        dependencies = List.of();
        owner = null;
        computation = null;
        equalityPolicy = null;
        cachedValue = null;
        initialized = false;
        checkRequired = false;
        retryRequired = false;
    }

    /// Returns the number of installed dependencies for package-level liveness diagnostics.
    ///
    /// @return the dependency count
    int dependencyCount() {
        return dependencies.size();
    }
}

package org.glavo.himari.state;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

/// Owns the fine-grained producer/consumer graph for one [StateDomain].
///
/// Graph mutation and derived computation are confined to the domain owner thread. Source commits
/// push only dirtiness through this graph; they never execute derived computations. A later pull
/// stabilizes upstream producers, compares dependency versions, and executes only consumers whose
/// semantic inputs changed.
@NotNullByDefault
public final class ReactiveGraph {
    /// The state domain whose epochs feed this graph.
    private final StateDomain domain;

    /// Root owners retained until explicit disposal.
    private final ArrayList<ReactiveOwner> rootOwners = new ArrayList<>();

    /// The active derived-evaluation stack in deterministic call order.
    private final ArrayList<DerivedState<?>> evaluationStack = new ArrayList<>();

    /// The next positive derived-state diagnostic identifier.
    private long nextDerivedId = 1L;

    /// Creates an empty graph for a state domain.
    ///
    /// @param domain the owning domain
    ReactiveGraph(StateDomain domain) {
        this.domain = domain;
    }

    /// Returns the state domain that owns this graph.
    ///
    /// @return the owning domain
    public StateDomain domain() {
        return domain;
    }

    /// Creates an independently disposable root owner.
    ///
    /// @return the new active owner
    /// @throws IllegalStateException if called outside the domain owner thread or from a derived
    /// computation
    public ReactiveOwner createOwner() {
        checkOwnershipMutationAllowed();
        ReactiveOwner owner = new ReactiveOwner(this, null);
        rootOwners.add(owner);
        return owner;
    }

    /// Allocates a stable derived-state diagnostic name.
    ///
    /// @return the next name
    String allocateDerivedName() {
        if (nextDerivedId == Long.MAX_VALUE) {
            throw new IllegalStateException("Reactive derived-state identifiers are exhausted");
        }
        String name = "DerivedState#" + nextDerivedId;
        nextDerivedId++;
        return name;
    }

    /// Removes a disposed root owner.
    ///
    /// @param owner the disposed owner
    void removeRootOwner(ReactiveOwner owner) {
        rootOwners.remove(owner);
    }

    /// Pushes dirtiness from every source changed by one atomic publication.
    ///
    /// @param changedSources the changed source identities
    void sourcesChanged(IdentityHashMap<AbstractStateSource, ?> changedSources) {
        ArrayDeque<ReactiveConsumerNode> queue = new ArrayDeque<>();
        for (AbstractStateSource source : changedSources.keySet()) {
            source.reactiveNode().appendConsumers(queue);
        }
        propagate(queue);
    }

    /// Pushes dirtiness from one derived producer whose semantic version advanced.
    ///
    /// @param producer the changed derived producer
    void derivedChanged(ReactiveProducerNode producer) {
        ArrayDeque<ReactiveConsumerNode> queue = new ArrayDeque<>();
        producer.appendConsumers(queue);
        propagate(queue);
    }

    /// Iteratively marks every reachable clean consumer as requiring version polling.
    ///
    /// @param queue the initial direct consumers
    private static void propagate(ArrayDeque<ReactiveConsumerNode> queue) {
        while (!queue.isEmpty()) {
            ReactiveConsumerNode consumer = queue.removeFirst();
            if (consumer.markCheckRequired()) {
                @Nullable ReactiveProducerNode downstreamProducer = consumer.downstreamProducer();
                if (downstreamProducer != null) {
                    downstreamProducer.appendConsumers(queue);
                }
            }
        }
    }

    /// Enters one derived computation or throws a deterministic cycle exception.
    ///
    /// @param state the computation to enter
    void enterEvaluation(DerivedState<?> state) {
        int cycleStart = indexOnEvaluationStack(state);
        if (cycleStart >= 0) {
            throw cycleException(cycleStart, state);
        }
        evaluationStack.add(state);
    }

    /// Leaves the current derived computation.
    ///
    /// @param state the computation expected at the top of the stack
    void exitEvaluation(DerivedState<?> state) {
        if (evaluationStack.isEmpty() || evaluationStack.getLast() != state) {
            throw new IllegalStateException("Reactive evaluation stack is unbalanced");
        }
        evaluationStack.removeLast();
    }

    /// Throws a cycle exception if a state is already being evaluated.
    ///
    /// @param state the candidate state
    void checkNotEvaluating(DerivedState<?> state) {
        int cycleStart = indexOnEvaluationStack(state);
        if (cycleStart >= 0) {
            throw cycleException(cycleStart, state);
        }
    }

    /// Returns the identity-based stack position of a state.
    ///
    /// @param state the candidate state
    /// @return its position, or `-1` when absent
    private int indexOnEvaluationStack(DerivedState<?> state) {
        for (int index = 0; index < evaluationStack.size(); index++) {
            if (evaluationStack.get(index) == state) {
                return index;
            }
        }
        return -1;
    }

    /// Creates a closed deterministic cycle path from the evaluation stack.
    ///
    /// @param cycleStart the first repeated stack position
    /// @param repeated the repeated state
    /// @return the cycle exception
    private ReactiveCycleException cycleException(int cycleStart, DerivedState<?> repeated) {
        List<String> path = new ArrayList<>(evaluationStack.size() - cycleStart + 1);
        for (int index = cycleStart; index < evaluationStack.size(); index++) {
            path.add(evaluationStack.get(index).diagnosticName());
        }
        path.add(repeated.diagnosticName());
        return new ReactiveCycleException(path);
    }

    /// Rejects graph ownership changes during derived evaluation.
    ///
    /// @throws IllegalStateException if a derived computation is active
    void checkMutationAllowed() {
        if (!evaluationStack.isEmpty()) {
            throw new IllegalStateException(
                    "Reactive ownership cannot change while evaluating "
                            + evaluationStack.getLast().diagnosticName()
            );
        }
    }

    /// Rejects owner-topology changes that cannot participate in source transaction rollback.
    ///
    /// @throws IllegalStateException if the caller is off-thread, a source transaction is active,
    /// or a derived computation is active
    void checkOwnershipMutationAllowed() {
        domain.checkOwnerThread();
        StateTransaction.checkNoActiveTransaction(domain, "Reactive ownership mutation");
        checkMutationAllowed();
    }

    /// Rejects application-state writes during derived evaluation.
    ///
    /// @throws IllegalStateException if a derived computation is active
    void checkStateWriteAllowed() {
        if (!evaluationStack.isEmpty()) {
            throw new IllegalStateException(
                    "Derived computation " + evaluationStack.getLast().diagnosticName()
                            + " cannot write application state"
            );
        }
    }

    /// Returns the current number of active root owners for package-level diagnostics.
    ///
    /// @return the active root-owner count
    int ownerCount() {
        return rootOwners.size();
    }
}

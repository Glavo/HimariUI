package org.glavo.himari.runtime.mount;

import org.glavo.himari.runtime.animation.AnimationPhaseImpact;
import org.glavo.himari.runtime.animation.AnimationTransaction;
import org.glavo.himari.state.ReactiveObservation;
import org.glavo.himari.state.ReactiveOwner;
import org.glavo.himari.state.StateDomain;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/// Owns mounted elements, typed property bindings, and incremental phase apply.
///
/// Topology changes are staged during a structural attempt and become visible only through one
/// [UiCommitTransaction]. Property bindings are independent ADR-015 consumers: a source read
/// performed by a binding invalidates only that binding and the phases declared by its impact.
/// Binding apply is failure-atomic and leaves previous committed model targets unchanged when
/// capture or publication fails.
@NotNullByDefault
public final class MountTree implements AutoCloseable {
    /// The state domain whose epochs feed this tree.
    private final StateDomain domain;

    /// The root reactive owner for every mounted element.
    private final ReactiveOwner rootOwner;

    /// Committed elements in first-declaration order.
    private final LinkedHashMap<MountIdentity, MountedNode> elements = new LinkedHashMap<>();

    /// Draft declarations collected during the current attempt.
    private final LinkedHashMap<MountIdentity, DraftDeclaration> draft = new LinkedHashMap<>();

    /// Group identities visited by the current attempt.
    private final LinkedHashSet<Long> visitedGroups = new LinkedHashSet<>();

    /// Whether a structural attempt is currently staging mount declarations.
    private boolean staging;

    /// Whether this tree has been closed.
    private boolean closed;

    /// The latest committed mount revision.
    private long revision;

    /// The state-domain epoch represented by the latest property apply.
    private long appliedStateEpoch;

    /// The phase union published by the latest successful apply.
    private AnimationPhaseImpact lastAppliedPhases = AnimationPhaseImpact.NONE;

    /// Creates an empty mount tree bound to one state domain.
    ///
    /// @param domain the state domain on its owner thread
    /// @throws IllegalStateException if called off the owner thread or inside a state transaction
    public MountTree(StateDomain domain) {
        this.domain = Objects.requireNonNull(domain, "domain");
        domain.checkOwnerThread();
        if (domain.hasActiveTransaction()) {
            throw new IllegalStateException("Mount tree cannot be created inside a state transaction");
        }
        this.rootOwner = domain.reactiveGraph().createOwner();
        this.appliedStateEpoch = domain.epoch();
    }

    /// Begins one structural attempt that may declare mount topology.
    ///
    /// @throws IllegalStateException if called off the owner thread, after closure, or reentrantly
    public void beginAttempt() {
        checkMutationEntry();
        if (staging) {
            throw new IllegalStateException("Mount attempt is already active");
        }
        staging = true;
        draft.clear();
        visitedGroups.clear();
    }

    /// Marks one structural group as visited by the current attempt.
    ///
    /// A visited group that declares no mounts loses every previously committed element.
    ///
    /// @param groupId the positive group identity
    public void visitGroup(long groupId) {
        checkStaging();
        if (groupId <= 0L) {
            throw new IllegalArgumentException("groupId must be positive");
        }
        visitedGroups.add(groupId);
    }

    /// Declares one mounted element owned by a visited structural group.
    ///
    /// @param groupId the positive owning group identity
    /// @param groupPath the deterministic group path
    /// @param mountKey the nonblank group-local mount key
    /// @param content the binding declaration callback
    public void declare(long groupId, String groupPath, String mountKey, MountedElementContent content) {
        checkStaging();
        Objects.requireNonNull(groupPath, "groupPath");
        Objects.requireNonNull(content, "content");
        visitGroup(groupId);
        MountIdentity identity = new MountIdentity(groupId, mountKey);
        if (draft.containsKey(identity)) {
            throw new IllegalArgumentException("Duplicate mount key: " + mountKey);
        }
        MountedElementScope scope = new MountedElementScope();
        try {
            content.compose(scope);
        } finally {
            scope.deactivate();
        }
        draft.put(identity, new DraftDeclaration(identity, identity.ownerPath(groupPath), scope.bindings()));
    }

    /// Publishes staged topology against the committed live-group set.
    ///
    /// Elements whose groups remain live but were not visited keep their previous bindings.
    /// Elements whose groups left the committed tree are disposed.
    ///
    /// @param liveGroupIds every currently committed structural group identity
    /// @return the topology commit result
    public UiCommitResult commitAttempt(@Unmodifiable Set<Long> liveGroupIds) {
        checkStaging();
        Objects.requireNonNull(liveGroupIds, "liveGroupIds");
        UiCommitTransaction transaction = new UiCommitTransaction(this);
        LinkedHashMap<MountIdentity, MountedNode> next = new LinkedHashMap<>();
        for (Map.Entry<MountIdentity, MountedNode> entry : elements.entrySet()) {
            MountedNode node = entry.getValue();
            long groupId = node.identity().groupId();
            if (!liveGroupIds.contains(groupId)) {
                transaction.stageRemove(node);
                continue;
            }
            if (!visitedGroups.contains(groupId)) {
                next.put(entry.getKey(), node);
            }
        }
        for (DraftDeclaration declaration : draft.values()) {
            if (!liveGroupIds.contains(declaration.identity.groupId())) {
                continue;
            }
            @Nullable MountedNode existing = elements.get(declaration.identity);
            MountedNode node = existing != null
                    ? existing
                    : new MountedNode(
                            declaration.identity,
                            declaration.ownerPath,
                            rootOwner.createChild()
                    );
            ArrayList<PropertyBinding<?>> replaced = new ArrayList<>();
            node.reconcile(declaration.bindings, replaced);
            if (existing == null) {
                transaction.stageInsert(node);
            }
            next.put(declaration.identity, node);
        }
        for (Map.Entry<MountIdentity, MountedNode> entry : elements.entrySet()) {
            MountIdentity identity = entry.getKey();
            if (visitedGroups.contains(identity.groupId()) && !next.containsKey(identity)
                    && liveGroupIds.contains(identity.groupId())) {
                transaction.stageRemove(entry.getValue());
            }
        }
        elements.clear();
        elements.putAll(next);
        staging = false;
        draft.clear();
        visitedGroups.clear();
        return transaction.commit();
    }

    /// Discards staged mount declarations without changing committed topology.
    public void abortAttempt() {
        domain.checkOwnerThread();
        if (!staging) {
            return;
        }
        staging = false;
        draft.clear();
        visitedGroups.clear();
    }

    /// Returns whether at least one committed binding requires a reader execution.
    ///
    /// @return whether [#apply()] may publish property targets
    public boolean needsApply() {
        checkOpen();
        domain.checkOwnerThread();
        for (MountedNode node : elements.values()) {
            for (PropertyBinding<?> binding : node.bindings().values()) {
                if (binding.isInvalidated()) {
                    return true;
                }
            }
        }
        return false;
    }

    /// Captures invalidated bindings and publishes changed property targets atomically.
    ///
    /// @return the apply result
    /// @throws IllegalStateException if called off the owner thread, after closure, during staging,
    /// or inside a state transaction
    public MountApplyResult apply() {
        checkMutationEntry();
        if (staging) {
            throw new IllegalStateException("Mounted properties cannot apply during a structural attempt");
        }
        long epoch = domain.epoch();
        ArrayList<ReactiveObservation> observations = new ArrayList<>();
        UiCommitTransaction transaction = new UiCommitTransaction(this);
        int changed = 0;
        try {
            for (MountedNode node : elements.values()) {
                for (PropertyBinding<?> binding : node.bindings().values()) {
                    if (captureBinding(binding, observations, transaction)) {
                        changed++;
                    }
                }
            }
            if (changed == 0) {
                for (ReactiveObservation observation : observations) {
                    observation.commit();
                }
                transaction.abort();
                appliedStateEpoch = epoch;
                lastAppliedPhases = AnimationPhaseImpact.NONE;
                return new MountApplyResult(
                        MountApplyStatus.NO_CHANGES,
                        revision,
                        epoch,
                        0,
                        AnimationPhaseImpact.NONE,
                        null
                );
            }
            for (ReactiveObservation observation : observations) {
                observation.commit();
            }
            UiCommitResult committed = transaction.commit();
            appliedStateEpoch = epoch;
            lastAppliedPhases = committed.appliedPhases();
            return new MountApplyResult(
                    MountApplyStatus.COMMITTED,
                    committed.revision(),
                    epoch,
                    committed.changedBindingCount(),
                    committed.appliedPhases(),
                    null
            );
        } catch (RuntimeException | Error failure) {
            for (ReactiveObservation observation : observations) {
                observation.close();
            }
            transaction.abort();
            return new MountApplyResult(
                    MountApplyStatus.FAILED,
                    revision,
                    epoch,
                    0,
                    AnimationPhaseImpact.NONE,
                    failure.getClass().getSimpleName()
            );
        }
    }

    /// Returns a snapshot of committed elements and the latest apply phases.
    ///
    /// @return the snapshot
    public MountSnapshot snapshot() {
        checkOpen();
        domain.checkOwnerThread();
        ArrayList<MountedElement> snapshots = new ArrayList<>();
        for (MountedNode node : elements.values()) {
            snapshots.add(node.snapshot());
        }
        return new MountSnapshot(revision, appliedStateEpoch, List.copyOf(snapshots), lastAppliedPhases);
    }

    /// Returns the latest committed mount revision.
    ///
    /// @return the revision
    public long revision() {
        checkOpen();
        domain.checkOwnerThread();
        return revision;
    }

    /// Disposes every mounted element and observer.
    ///
    /// Closure is idempotent.
    @Override
    public void close() {
        domain.checkOwnerThread();
        if (closed) {
            return;
        }
        abortAttempt();
        for (MountedNode node : List.copyOf(elements.values())) {
            node.dispose();
        }
        elements.clear();
        rootOwner.close();
        closed = true;
    }

    /// Records a successful topology or property commit.
    ///
    /// @param inserted inserted element count
    /// @param removed removed element count
    /// @param changed changed binding count
    /// @param phases published phase union
    /// @param animation staged animation metadata
    /// @return the commit result
    UiCommitResult recordCommit(
            int inserted,
            int removed,
            int changed,
            AnimationPhaseImpact phases,
            @Nullable AnimationTransaction animation
    ) {
        if (inserted > 0 || removed > 0 || changed > 0) {
            if (revision == Long.MAX_VALUE) {
                throw new IllegalStateException("Mount revision is exhausted");
            }
            revision++;
        }
        if (changed > 0) {
            lastAppliedPhases = phases;
        }
        return new UiCommitResult(revision, inserted, removed, changed, phases, animation);
    }

    /// Captures one binding and stages a property change when the semantic value differs.
    ///
    /// @param binding the binding
    /// @param observations detached observations to commit together
    /// @param transaction the open UI commit
    /// @param <T> the property type
    /// @return whether a property target was staged
    private <T> boolean captureBinding(
            PropertyBinding<T> binding,
            List<ReactiveObservation> observations,
            UiCommitTransaction transaction
    ) {
        if (!binding.isInvalidated()) {
            return false;
        }
        PropertyBinding.CapturedValue<T> captured = binding.capture();
        observations.add(captured.observation());
        if (binding.hasSameValue(captured.value())) {
            return false;
        }
        transaction.stageProperty(binding, captured.value());
        return true;
    }

    /// Verifies mutation entry conditions.
    private void checkMutationEntry() {
        checkOpen();
        domain.checkOwnerThread();
        if (domain.hasActiveTransaction()) {
            throw new IllegalStateException("Mount tree cannot mutate inside a state transaction");
        }
    }

    /// Verifies that a structural attempt is staging declarations.
    private void checkStaging() {
        checkMutationEntry();
        if (!staging) {
            throw new IllegalStateException("Mount declaration requires an active attempt");
        }
    }

    /// Verifies that the tree remains open.
    private void checkOpen() {
        if (closed) {
            throw new IllegalStateException("Mount tree is closed");
        }
    }

    /// Stores one staged element declaration.
    ///
    /// @param identity the mount identity
    /// @param ownerPath the owner path
    /// @param bindings declared binding specifications
    @NotNullByDefault
    private record DraftDeclaration(
            MountIdentity identity,
            String ownerPath,
            LinkedHashMap<String, MountedElementScope.BindingSpec<?>> bindings
    ) {
    }
}

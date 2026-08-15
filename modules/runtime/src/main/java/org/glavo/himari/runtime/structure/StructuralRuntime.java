package org.glavo.himari.runtime.structure;

import org.glavo.himari.state.ReactiveObservation;
import org.glavo.himari.state.ReactiveObserver;
import org.glavo.himari.state.ReactiveOwner;
import org.glavo.himari.state.StateDomain;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/// Reconciles explicit ordinary-Java structural groups through failure-atomic private drafts.
///
/// Each group owns an independent reactive observer. A normal update reruns only invalidated groups
/// whose active ancestors are clean, while execution of a selected group also executes every child
/// it declares. Topology, dependency captures, ambient edges, local memory, and effect declarations
/// publish as one owner-thread revision. Failed and cooperatively cancelled attempts retain the
/// previous committed snapshot.
///
/// The runtime is bound to its [StateDomain] owner thread. Application callbacks are synchronous,
/// non-reentrant, and may not write state. The first implementation is deliberately non-preemptive;
/// cancellation is observed only through explicit checkpoints.
@NotNullByDefault
public final class StructuralRuntime implements AutoCloseable {
    /// The stable semantic key used internally by conditional branches.
    private static final Object BRANCH_KEY = new Object();

    /// The state domain whose epochs and reactive graph drive this runtime.
    private final StateDomain domain;

    /// Bounded diagnostic and materialization configuration.
    private final StructuralRuntimeConfig config;

    /// The root reactive owner for all structural groups and callback guarding.
    private final ReactiveOwner reactiveRootOwner;

    /// A detached observer used to reject state writes in lifecycle callbacks.
    private final ReactiveObserver callbackGuard;

    /// The stable root group.
    private final GroupNode root;

    /// Boundary recovery records keyed with application identity semantics.
    private final IdentityHashMap<ErrorBoundaryKey, BoundaryState> boundaries = new IdentityHashMap<>();

    /// Committed current-measure groups keyed with application identity semantics.
    private final IdentityHashMap<MeasureMaterializationKey<?>, GroupNode> measureGroups =
            new IdentityHashMap<>();

    /// Retained bounded diagnostics in sequence order.
    private final ArrayDeque<StructuralFailure> failures = new ArrayDeque<>();

    /// The currently executing composer, or `null` outside a structural callback.
    private @Nullable Composer currentComposer;

    /// Whether a normal, fallback, or measure attempt is currently staging.
    private boolean staging;

    /// The current lifecycle status.
    private StructuralRuntimeStatus status = StructuralRuntimeStatus.ACTIVE;

    /// The latest committed structural revision.
    private long revision;

    /// The state-domain epoch represented by the latest committed revision.
    private long committedStateEpoch;

    /// The next positive group identity.
    private long nextGroupId = 1L;

    /// The next positive remembered-slot identity.
    private long nextMemoryId = 1L;

    /// The next positive diagnostic sequence.
    private long nextFailureSequence = 1L;

    /// The most recently recorded failure, or `null` before the first failure.
    private @Nullable StructuralFailure latestFailure;

    /// Creates a structural runtime with development defaults.
    ///
    /// Construction creates ownership records but does not execute application code. The first
    /// [#update()] performs the initial root composition.
    ///
    /// @param domain the state domain on its owner thread
    /// @param content the stable root content callback
    /// @throws IllegalStateException if called off the domain owner thread or inside a state
    /// transaction
    public StructuralRuntime(StateDomain domain, StructuralContent content) {
        this(domain, StructuralRuntimeConfig.defaults(), content);
    }

    /// Creates a configured structural runtime.
    ///
    /// Construction creates ownership records but does not execute application code. The first
    /// [#update()] performs the initial root composition.
    ///
    /// @param domain the state domain on its owner thread
    /// @param config the bounded runtime configuration
    /// @param content the stable root content callback
    /// @throws IllegalStateException if called off the domain owner thread or inside a state
    /// transaction
    public StructuralRuntime(StateDomain domain, StructuralRuntimeConfig config, StructuralContent content) {
        this.domain = Objects.requireNonNull(domain, "domain");
        this.config = Objects.requireNonNull(config, "config");
        StructuralContent checkedContent = Objects.requireNonNull(content, "content");
        domain.checkOwnerThread();
        if (domain.hasActiveTransaction()) {
            throw new IllegalStateException("Structural runtime cannot be created inside a state transaction");
        }
        reactiveRootOwner = domain.reactiveGraph().createOwner();
        callbackGuard = reactiveRootOwner.createObserver();
        root = new GroupNode(
                allocateGroupId(),
                "root",
                null,
                0,
                null,
                GroupKind.ROOT,
                null,
                reactiveRootOwner,
                reactiveRootOwner.createObserver()
        );
        root.content = checkedContent;
        committedStateEpoch = domain.epoch();
    }

    /// Executes every currently invalidated active structural group.
    ///
    /// @return the attempt result
    /// @throws IllegalStateException if called off the owner thread, after closure, reentrantly, or
    /// inside a state transaction
    public StructuralAttemptResult update() {
        return updateInternal(null);
    }

    /// Executes every currently invalidated active structural group with cooperative cancellation.
    ///
    /// @param cancellation the any-thread cancellation flag observed at explicit checkpoints
    /// @return the attempt result
    /// @throws IllegalStateException if called off the owner thread, after closure, reentrantly, or
    /// inside a state transaction
    public StructuralAttemptResult update(StructuralCancellation cancellation) {
        return updateInternal(Objects.requireNonNull(cancellation, "cancellation"));
    }

    /// Materializes one declared current-measure scope using the current input.
    ///
    /// @param key the committed materialization identity
    /// @param input the current non-null constraints or viewport input
    /// @param <I> the immutable input type
    /// @return the attempt result
    /// @throws IllegalArgumentException if the key has no active committed declaration
    /// @throws IllegalStateException if called off the owner thread, after closure, reentrantly, or
    /// inside a state transaction
    public <I> StructuralAttemptResult materialize(MeasureMaterializationKey<I> key, I input) {
        return materializeInternal(key, input, null);
    }

    /// Materializes one declared current-measure scope with cooperative cancellation.
    ///
    /// @param key the committed materialization identity
    /// @param input the current non-null constraints or viewport input
    /// @param cancellation the any-thread cancellation flag observed at explicit checkpoints
    /// @param <I> the immutable input type
    /// @return the attempt result
    /// @throws IllegalArgumentException if the key has no active committed declaration
    /// @throws IllegalStateException if called off the owner thread, after closure, reentrantly, or
    /// inside a state transaction
    public <I> StructuralAttemptResult materialize(
            MeasureMaterializationKey<I> key,
            I input,
            StructuralCancellation cancellation
    ) {
        return materializeInternal(
                key,
                input,
                Objects.requireNonNull(cancellation, "cancellation")
        );
    }

    /// Returns whether a committed active measure group requires current-input materialization.
    ///
    /// State dependencies are pulled before the answer is returned, so an invalidated derived value
    /// whose semantic result is unchanged does not require materialization.
    ///
    /// @param key the materialization identity
    /// @return whether the group exists, is active, and requires materialization
    /// @throws IllegalStateException if called off the owner thread, after closure, or during an
    /// attempt
    public boolean needsMaterialization(MeasureMaterializationKey<?> key) {
        Objects.requireNonNull(key, "key");
        checkOwnerThread();
        checkNotClosed();
        if (staging) {
            throw new IllegalStateException("Materialization state cannot be pulled during an attempt");
        }
        @Nullable GroupNode group = measureGroups.get(key);
        return group != null
                && isActive(group)
                && (group.needsMaterialization || group.observer.isInvalidated());
    }

    /// Resets one failed boundary and every failed or escalated ancestor needed to reach it.
    ///
    /// Resetting never executes application code. A later [#update()] performs one fresh normal
    /// attempt from the highest attached reset boundary, or from the root when the boundary is not
    /// currently mounted.
    ///
    /// @param key the application boundary identity
    /// @return whether a retained boundary record was found
    /// @throws IllegalStateException if called off the owner thread, after closure, during an
    /// attempt, or inside a state transaction
    public boolean resetBoundary(ErrorBoundaryKey key) {
        Objects.requireNonNull(key, "key");
        checkOperationEntry();
        @Nullable BoundaryState boundary = boundaries.get(key);
        if (boundary == null) {
            return false;
        }
        @Nullable GroupNode highest = null;
        for (@Nullable BoundaryState current = boundary; current != null; current = current.parent) {
            current.status = ErrorBoundaryStatus.HEALTHY;
            if (current.group != null && isActive(current.group)) {
                highest = current.group;
            }
        }
        status = StructuralRuntimeStatus.ACTIVE;
        (highest == null ? root : highest).forceDirty = true;
        return true;
    }

    /// Resets root containment and all retained boundary recovery states.
    ///
    /// A later [#update()] performs one fresh root attempt.
    ///
    /// @throws IllegalStateException if called off the owner thread, after closure, during an
    /// attempt, or inside a state transaction
    public void resetRoot() {
        checkOperationEntry();
        status = StructuralRuntimeStatus.ACTIVE;
        for (BoundaryState boundary : boundaries.values()) {
            boundary.status = ErrorBoundaryStatus.HEALTHY;
        }
        root.forceDirty = true;
    }

    /// Returns the retained status of one known boundary.
    ///
    /// @param key the application boundary identity
    /// @return the recovery state, or `null` when the key has never committed or failed
    /// @throws IllegalStateException if called off the owner thread
    public @Nullable ErrorBoundaryStatus boundaryStatus(ErrorBoundaryKey key) {
        Objects.requireNonNull(key, "key");
        checkOwnerThread();
        @Nullable BoundaryState boundary = boundaries.get(key);
        return boundary == null ? null : boundary.status;
    }

    /// Captures the committed structural topology and bounded diagnostic count.
    ///
    /// @return an immutable snapshot
    /// @throws IllegalStateException if called off the owner thread
    public StructuralSnapshot snapshot() {
        checkOwnerThread();
        List<StructuralGroupSnapshot> groups = new ArrayList<>();
        if (status != StructuralRuntimeStatus.CLOSED) {
            snapshotGroup(root, StructuralGroupState.ACTIVE, groups);
        }
        return new StructuralSnapshot(
                revision,
                committedStateEpoch,
                status,
                groups,
                failures.size()
        );
    }

    /// Removes and returns every retained diagnostic in sequence order.
    ///
    /// @return the immutable drained diagnostic list
    /// @throws IllegalStateException if called off the owner thread or during an attempt
    public @Unmodifiable List<StructuralFailure> drainFailures() {
        checkOwnerThread();
        if (staging) {
            throw new IllegalStateException("Structural failures cannot drain during an attempt");
        }
        List<StructuralFailure> drained = List.copyOf(failures);
        failures.clear();
        return drained;
    }

    /// Returns the runtime lifecycle status.
    ///
    /// @return the current status
    /// @throws IllegalStateException if called off the owner thread
    public StructuralRuntimeStatus status() {
        checkOwnerThread();
        return status;
    }

    /// Returns the latest committed structural revision.
    ///
    /// @return the nonnegative revision
    /// @throws IllegalStateException if called off the owner thread
    public long revision() {
        checkOwnerThread();
        return revision;
    }

    /// Releases all active and retained groups child-before-parent.
    ///
    /// Cleanup failures are retained diagnostically and do not skip later cleanup. Closure is
    /// idempotent and must occur on the state-domain owner thread outside an attempt or transaction.
    ///
    /// @throws IllegalStateException if called off the owner thread, during an attempt, or inside a
    /// state transaction
    @Override
    public void close() {
        checkOwnerThread();
        if (status == StructuralRuntimeStatus.CLOSED) {
            return;
        }
        if (staging) {
            throw new IllegalStateException("Structural runtime cannot close during an attempt");
        }
        if (domain.hasActiveTransaction()) {
            throw new IllegalStateException("Structural runtime cannot close inside a state transaction");
        }
        status = StructuralRuntimeStatus.CLOSED;
        staging = true;
        try {
            ArrayList<StructuralCleanupFailure> cleanupFailures = new ArrayList<>();
            releaseDescendants(root, cleanupFailures);
            disposeDirectEffects(root, cleanupFailures);
            disposeDirectMemories(root, cleanupFailures);
            detachManualDependencies(root);
            root.observer.clearDependencies();
            reactiveRootOwner.close();
            measureGroups.clear();
            boundaries.clear();
            if (!cleanupFailures.isEmpty()) {
                recordCleanupDiagnostic(cleanupFailures);
            }
        } finally {
            currentComposer = null;
            staging = false;
        }
    }

    /// Returns one local value and records a dependency for the current callback when present.
    ///
    /// @param local the local cell
    /// @param <T> the value type
    /// @return the current value
    <T> T readLocal(StructuralLocal<T> local) {
        checkOwnerThread();
        if (local.disposed) {
            throw new IllegalStateException("Structural local is disposed");
        }
        @Nullable Composer composer = currentComposer;
        if (composer != null) {
            composer.recordLocalRead(local);
        }
        return local.value;
    }

    /// Writes one local value outside structural attempts and invalidates its active readers.
    ///
    /// @param local the local cell
    /// @param value the checked replacement
    /// @param <T> the value type
    <T> void writeLocal(StructuralLocal<T> local, T value) {
        checkOwnerThread();
        checkNotClosed();
        if (staging) {
            throw new IllegalStateException("Structural locals cannot change during an attempt");
        }
        if (domain.hasActiveTransaction()) {
            throw new IllegalStateException("Structural locals cannot change inside a state transaction");
        }
        if (local.disposed) {
            throw new IllegalStateException("Structural local is disposed");
        }
        boolean equal;
        staging = true;
        try {
            equal = guardedEquals(local.value, value);
        } finally {
            staging = false;
        }
        if (equal) {
            return;
        }
        local.value = value;
        for (GroupNode consumer : List.copyOf(local.consumers.keySet())) {
            if (isActive(consumer)) {
                invalidateManualConsumer(consumer);
            }
        }
    }

    /// Verifies that the caller is the state-domain owner thread.
    void checkOwnerThread() {
        domain.checkOwnerThread();
    }

    /// Executes one normal update with an optional cooperative-cancellation flag.
    private StructuralAttemptResult updateInternal(@Nullable StructuralCancellation cancellation) {
        checkOperationEntry();
        long revisionBefore = revision;
        long epoch = domain.epoch();
        if (status == StructuralRuntimeStatus.FAILED) {
            return failedResult(revisionBefore, epoch, 0);
        }

        final ArrayList<GroupNode> targets;
        try {
            targets = collectDirtyTargets();
        } catch (AttemptProblem problem) {
            return recoverFromProblem(problem, cancellation, revisionBefore, epoch, 0);
        }
        if (targets.isEmpty()) {
            return new StructuralAttemptResult(
                    StructuralAttemptStatus.NO_CHANGES,
                    revisionBefore,
                    revision,
                    epoch,
                    0,
                    0,
                    null
            );
        }

        staging = true;
        AttemptContext context = new AttemptContext(epoch, cancellation);
        try {
            runNormalAttempt(targets, context);
            CommitOutcome outcome = commit(context);
            return committedResult(
                    StructuralAttemptStatus.COMMITTED,
                    revisionBefore,
                    epoch,
                    targets.size(),
                    outcome,
                    null
            );
        } catch (CancellationSignal cancellationSignal) {
            @Nullable StructuralFailure cleanup = abort(context, null);
            pruneDetachedHealthyBoundaries();
            return new StructuralAttemptResult(
                    StructuralAttemptStatus.CANCELLED,
                    revisionBefore,
                    revision,
                    epoch,
                    targets.size(),
                    0,
                    cleanup
            );
        } catch (AttemptProblem problem) {
            abort(context, problem);
            return recoverFromProblem(problem, cancellation, revisionBefore, epoch, targets.size());
        } finally {
            currentComposer = null;
            staging = false;
        }
    }

    /// Executes one current-measure materialization with optional cooperative cancellation.
    private <I> StructuralAttemptResult materializeInternal(
            MeasureMaterializationKey<I> key,
            I input,
            @Nullable StructuralCancellation cancellation
    ) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(input, "input");
        checkOperationEntry();
        long revisionBefore = revision;
        long epoch = domain.epoch();
        if (status == StructuralRuntimeStatus.FAILED) {
            return failedResult(revisionBefore, epoch, 0);
        }
        @Nullable GroupNode group = measureGroups.get(key);
        if (group == null || !isActive(group)) {
            throw new IllegalArgumentException("Materialization key is not declared by an active group");
        }
        I checkedInput = key.inputType().cast(input);

        staging = true;
        AttemptContext context = new AttemptContext(epoch, cancellation);
        try {
            runMeasureAttempt(group, checkedInput, context);
            CommitOutcome outcome = commit(context);
            return committedResult(
                    StructuralAttemptStatus.COMMITTED,
                    revisionBefore,
                    epoch,
                    1,
                    outcome,
                    null
            );
        } catch (CancellationSignal cancellationSignal) {
            group.needsMaterialization = true;
            @Nullable StructuralFailure cleanup = abort(context, null);
            pruneDetachedHealthyBoundaries();
            return new StructuralAttemptResult(
                    StructuralAttemptStatus.CANCELLED,
                    revisionBefore,
                    revision,
                    epoch,
                    1,
                    0,
                    cleanup
            );
        } catch (AttemptProblem problem) {
            group.needsMaterialization = true;
            abort(context, problem);
            return recoverFromProblem(problem, cancellation, revisionBefore, epoch, 1);
        } finally {
            currentComposer = null;
            staging = false;
        }
    }

    /// Performs fresh boundary fallback attempts until one commits or root containment is reached.
    private StructuralAttemptResult recoverFromProblem(
            AttemptProblem initialProblem,
            @Nullable StructuralCancellation cancellation,
            long revisionBefore,
            long epoch,
            int attemptedGroups
    ) {
        boolean establishedStaging = !staging;
        if (establishedStaging) {
            staging = true;
        }
        AttemptProblem problem = initialProblem;
        @Nullable StructuralFailure originatingFailure = null;
        int totalAttemptedGroups = attemptedGroups;
        try {
            while (true) {
                StructuralFailure diagnostic = recordProblem(problem, List.of());
                if (originatingFailure == null) {
                    originatingFailure = diagnostic;
                }
                @Nullable BoundaryState boundary = problem.boundary;
                if (boundary == null) {
                    status = StructuralRuntimeStatus.FAILED;
                    pruneDetachedHealthyBoundaries();
                    return new StructuralAttemptResult(
                            StructuralAttemptStatus.ROOT_FAILED,
                            revisionBefore,
                            revision,
                            epoch,
                            totalAttemptedGroups,
                            0,
                            originatingFailure
                    );
                }
                boundary.status = ErrorBoundaryStatus.FAILED;
                GroupNode target = boundary.group != null && isActive(boundary.group)
                        ? boundary.group
                        : root;
                totalAttemptedGroups++;
                AttemptContext context = new AttemptContext(epoch, cancellation);
                try {
                    runNormalAttempt(List.of(target), context);
                    CommitOutcome outcome = commit(context);
                    status = StructuralRuntimeStatus.ACTIVE;
                    return committedResult(
                            StructuralAttemptStatus.CONTAINED_FAILURE,
                            revisionBefore,
                            epoch,
                            totalAttemptedGroups,
                            outcome,
                            originatingFailure
                    );
                } catch (CancellationSignal cancellationSignal) {
                    @Nullable StructuralFailure cleanup = abort(context, null);
                    pruneDetachedHealthyBoundaries();
                    return new StructuralAttemptResult(
                            StructuralAttemptStatus.CANCELLED,
                            revisionBefore,
                            revision,
                            epoch,
                            totalAttemptedGroups,
                            0,
                            cleanup == null ? originatingFailure : cleanup
                    );
                } catch (AttemptProblem fallbackProblem) {
                    @Nullable StructuralFailure cleanup = abort(context, fallbackProblem);
                    if (cleanup != null) {
                        recordFailure(cleanup);
                    }
                    problem = fallbackProblem;
                }
            }
        } finally {
            currentComposer = null;
            if (establishedStaging) {
                staging = false;
            }
        }
    }

    /// Runs one set of pairwise non-overlapping normal root drafts.
    private void runNormalAttempt(@Unmodifiable List<GroupNode> targets, AttemptContext context) {
        for (GroupNode target : targets) {
            GroupDraft draft = GroupDraft.fromCommitted(target, context);
            context.rootDrafts.add(draft);
            Composer composer = new Composer(context, draft, StructuralCallbackPhase.STRUCTURE);
            composer.executeCurrent();
        }
    }

    /// Runs one restricted current-measure draft.
    private <I> void runMeasureAttempt(GroupNode group, I input, AttemptContext context) {
        if (group.kind != GroupKind.MEASURE || group.measureContent == null || group.measureKey == null) {
            throw new IllegalStateException("Committed materialization group is incomplete");
        }
        GroupDraft draft = GroupDraft.fromCommitted(group, context);
        draft.materializing = true;
        context.rootDrafts.add(draft);
        Composer composer = new Composer(context, draft, StructuralCallbackPhase.MEASURE_MATERIALIZATION);
        composer.executeMeasure(input);
    }

    /// Publishes one completely staged attempt and then performs irreversible cleanup.
    private CommitOutcome commit(AttemptContext context) {
        if (domain.epoch() != context.stateEpoch) {
            throw new AttemptProblem(
                    "state-epoch-changed",
                    StructuralCallbackPhase.STRUCTURE,
                    "root",
                    null,
                    null
            );
        }
        for (GroupDraft draft : context.rootDrafts) {
            prepareProviders(draft);
        }
        mountNewEffects(context);
        for (ReactiveObservation observation : context.observations) {
            observation.commit();
        }

        CommitPlan plan = new CommitPlan();
        for (GroupDraft draft : context.rootDrafts) {
            applyDraft(draft, plan);
        }
        if (revision == Long.MAX_VALUE) {
            throw new IllegalStateException("Structural revision is exhausted");
        }
        revision++;
        committedStateEpoch = context.stateEpoch;

        for (ProviderCell provider : plan.changedProviders.keySet()) {
            for (GroupNode consumer : List.copyOf(provider.consumers.keySet())) {
                if (!plan.committedGroups.containsKey(consumer) && isActive(consumer)) {
                    invalidateManualConsumer(consumer);
                }
            }
        }

        ArrayList<StructuralCleanupFailure> cleanupFailures = new ArrayList<>();
        for (CleanupTask cleanupTask : plan.cleanupTasks) {
            cleanupTask.run(cleanupFailures);
        }
        @Nullable StructuralFailure cleanupDiagnostic = cleanupFailures.isEmpty()
                ? null
                : recordCleanupDiagnostic(cleanupFailures);
        pruneDetachedHealthyBoundaries();
        return new CommitOutcome(context.allDrafts.size(), cleanupDiagnostic);
    }

    /// Validates staged provider equality and version capacity before irreversible publication.
    private void prepareProviders(GroupDraft draft) {
        if (draft.providerCell != null) {
            ProviderCell provider = draft.providerCell;
            Object nextValue = Objects.requireNonNull(draft.providerValue, "providerValue");
            if (provider.committed) {
                try {
                    draft.providerChanged = !guardedEquals(provider.value, nextValue);
                } catch (RuntimeException | Error failure) {
                    escalateDraftFallback(draft);
                    throw new AttemptProblem(
                            "ambient-value-equality-failed",
                            StructuralCallbackPhase.STRUCTURE,
                            draft.group.ownerPath(),
                            draft.failureBoundary,
                            failure
                    );
                }
                if (draft.providerChanged && provider.version == Long.MAX_VALUE) {
                    escalateDraftFallback(draft);
                    throw new AttemptProblem(
                            "ambient-version-exhausted",
                            StructuralCallbackPhase.STRUCTURE,
                            draft.group.ownerPath(),
                            draft.failureBoundary,
                            null
                    );
                }
            }
        }
        for (GroupDraft child : draft.activeChildren) {
            prepareProviders(child);
        }
    }

    /// Escalates the failed fallback owning one post-callback draft failure when required.
    private static void escalateDraftFallback(GroupDraft draft) {
        if (draft.escalatesFallback && draft.fallbackBoundary != null) {
            escalateFailedBoundaryChain(draft.fallbackBoundary);
        }
    }

    /// Mounts every newly declared effect before dependency and topology publication.
    private void mountNewEffects(AttemptContext context) {
        for (GroupDraft rootDraft : context.rootDrafts) {
            collectAndMountEffects(rootDraft, context);
        }
    }

    /// Traverses one draft in parent-before-child mount order.
    private void collectAndMountEffects(GroupDraft draft, AttemptContext context) {
        if (!draft.declarationOnly) {
            for (EffectDeclaration declaration : draft.effects.values()) {
                if (declaration.previous == null) {
                    OwnedEffect effect = new OwnedEffect(
                            draft.group,
                            declaration.key,
                            declaration.mount,
                            declaration.cleanup
                    );
                    declaration.created = effect;
                    context.mountedEffects.add(effect);
                    try {
                        runGuarded(effect.mount);
                        effect.mounted = true;
                    } catch (RuntimeException | Error failure) {
                        if (draft.escalatesFallback && draft.fallbackBoundary != null) {
                            escalateFailedBoundaryChain(draft.fallbackBoundary);
                        }
                        throw new AttemptProblem(
                                "effect-mount-failed",
                                StructuralCallbackPhase.EFFECT_MOUNT,
                                effect.ownerPath(),
                                draft.failureBoundary,
                                failure
                        );
                    }
                }
            }
        }
        for (GroupDraft child : draft.activeChildren) {
            collectAndMountEffects(child, context);
        }
    }

    /// Applies one draft without executing application callbacks.
    private void applyDraft(GroupDraft draft, CommitPlan plan) {
        GroupNode group = draft.group;
        plan.committedGroups.put(group, Boolean.TRUE);

        if (draft.declarationOnly) {
            applyDescriptors(draft, plan);
            group.forceDirty = false;
            group.needsMaterialization = true;
            return;
        }

        for (GroupDraft child : draft.activeChildren) {
            applyDraft(child, plan);
        }

        IdentityHashMap<GroupNode, Boolean> preserved = new IdentityHashMap<>();
        for (GroupDraft child : draft.activeChildren) {
            preserved.put(child.group, Boolean.TRUE);
        }
        for (GroupNode child : draft.dormantChildren) {
            preserved.put(child, Boolean.TRUE);
        }
        for (GroupNode child : group.activeChildren) {
            if (!preserved.containsKey(child)) {
                plan.cleanupTasks.add(failures -> releaseTree(child, failures));
            } else if (containsIdentity(draft.dormantChildren, child)) {
                plan.cleanupTasks.add(failures -> deactivateTree(child, failures));
            }
        }
        for (GroupNode child : group.dormantChildren) {
            if (!preserved.containsKey(child)) {
                plan.cleanupTasks.add(failures -> releaseTree(child, failures));
            }
        }

        ArrayList<GroupNode> activeChildren = new ArrayList<>(draft.activeChildren.size());
        for (GroupDraft child : draft.activeChildren) {
            activeChildren.add(child.group);
        }
        group.activeChildren = activeChildren;
        group.dormantChildren = new ArrayList<>(draft.dormantChildren);

        for (Map.Entry<String, OwnedEffect> entry : group.effects.entrySet()) {
            if (!draft.effects.containsKey(entry.getKey())) {
                OwnedEffect effect = entry.getValue();
                plan.cleanupTasks.add(failures -> disposeEffect(effect, failures));
            }
        }
        LinkedHashMap<String, OwnedEffect> effects = new LinkedHashMap<>();
        for (EffectDeclaration declaration : draft.effects.values()) {
            OwnedEffect effect;
            if (declaration.previous == null) {
                effect = Objects.requireNonNull(declaration.created, "created effect");
            } else {
                effect = declaration.previous;
                effect.mount = declaration.mount;
                effect.cleanup = declaration.cleanup;
            }
            effects.put(declaration.key, effect);
        }
        group.effects = effects;

        for (MemorySlot memory : group.memories) {
            if (!containsIdentity(draft.memories, memory)) {
                plan.cleanupTasks.add(failures -> disposeMemory(memory, failures));
            }
        }
        group.memories = new ArrayList<>(draft.memories);

        reconcileManualDependencies(group, draft);
        applyDescriptors(draft, plan);
        group.forceDirty = false;
        if (draft.materializing) {
            group.needsMaterialization = false;
        }
    }

    /// Applies callback, ambient, boundary, and measure descriptors for one group.
    private void applyDescriptors(GroupDraft draft, CommitPlan plan) {
        GroupNode group = draft.group;
        group.content = draft.content;
        if (draft.providerCell != null) {
            ProviderCell provider = draft.providerCell;
            Object nextValue = Objects.requireNonNull(draft.providerValue, "providerValue");
            if (provider.committed && draft.providerChanged) {
                provider.value = nextValue;
                provider.version++;
                plan.changedProviders.put(provider, Boolean.TRUE);
            } else {
                provider.value = nextValue;
            }
            provider.committed = true;
            group.providerCell = provider;
        }
        if (draft.boundaryState != null) {
            BoundaryState boundary = draft.boundaryState;
            boundary.group = group;
            boundary.parent = draft.boundaryParent;
            group.boundaryState = boundary;
            group.normalContent = draft.normalContent;
            group.fallbackContent = draft.fallbackContent;
            boundaries.put(boundary.key, boundary);
        }
        if (draft.measureKey != null) {
            group.measureKey = draft.measureKey;
            group.measureContent = draft.measureContent;
            measureGroups.put(draft.measureKey, group);
        }
    }

    /// Replaces ambient and local dependency edges for one committed group.
    private static void reconcileManualDependencies(GroupNode group, GroupDraft draft) {
        for (ProviderCell provider : group.ambientDependencies.keySet()) {
            if (!draft.ambientDependencies.containsKey(provider)) {
                provider.consumers.remove(group);
            }
        }
        for (ProviderCell provider : draft.ambientDependencies.keySet()) {
            if (!group.ambientDependencies.containsKey(provider)) {
                provider.consumers.put(group, Boolean.TRUE);
            }
        }
        group.ambientDependencies = new IdentityHashMap<>(draft.ambientDependencies);

        for (StructuralLocal<?> local : group.localDependencies.keySet()) {
            if (!draft.localDependencies.containsKey(local)) {
                local.consumers.remove(group);
            }
        }
        for (StructuralLocal<?> local : draft.localDependencies.keySet()) {
            if (!group.localDependencies.containsKey(local)) {
                local.consumers.put(group, Boolean.TRUE);
            }
        }
        group.localDependencies = new IdentityHashMap<>(draft.localDependencies);
    }

    /// Aborts one context and returns a cleanup diagnostic when abort cleanup failed.
    private @Nullable StructuralFailure abort(
            AttemptContext context,
            @Nullable AttemptProblem problem
    ) {
        for (ReactiveObservation observation : context.observations) {
            observation.close();
        }
        ArrayList<StructuralCleanupFailure> cleanupFailures = new ArrayList<>();
        ArrayList<AbortCleanupTask> cleanupTasks = new ArrayList<>(
                context.mountedEffects.size() + context.newMemories.size()
        );
        int cleanupOrder = 0;
        for (OwnedEffect effect : context.mountedEffects) {
            cleanupTasks.add(new AbortCleanupTask(
                    groupDepth(effect.owner),
                    cleanupOrder++,
                    failures -> disposeEffect(effect, failures)
            ));
        }
        for (MemorySlot memory : context.newMemories) {
            cleanupTasks.add(new AbortCleanupTask(
                    groupDepth(memory.owner),
                    cleanupOrder++,
                    failures -> disposeMemory(memory, failures)
            ));
        }
        cleanupTasks.sort((left, right) -> {
            int depthComparison = Integer.compare(right.depth(), left.depth());
            return depthComparison != 0
                    ? depthComparison
                    : Integer.compare(right.declarationOrder(), left.declarationOrder());
        });
        for (AbortCleanupTask cleanupTask : cleanupTasks) {
            cleanupTask.task().run(cleanupFailures);
        }
        for (int index = context.newGroups.size() - 1; index >= 0; index--) {
            GroupNode group = context.newGroups.get(index);
            if (!group.reactiveOwner.isDisposed()) {
                group.reactiveOwner.close();
            }
        }
        if (cleanupFailures.isEmpty()) {
            return null;
        }
        if (problem != null) {
            problem.cleanupFailures.addAll(cleanupFailures);
            return null;
        }
        return recordCleanupDiagnostic(cleanupFailures);
    }

    /// Collects active minimal dirty roots in depth-first order.
    private ArrayList<GroupNode> collectDirtyTargets() {
        ArrayList<GroupNode> targets = new ArrayList<>();
        collectDirtyTargets(root, false, targets);
        return targets;
    }

    /// Recursively selects dirty groups while suppressing descendants of a selected ancestor.
    private void collectDirtyTargets(GroupNode group, boolean ancestorSelected, ArrayList<GroupNode> targets) {
        boolean selected = false;
        if (!ancestorSelected && group.kind != GroupKind.MEASURE) {
            try {
                selected = group.forceDirty || group.observer.isInvalidated();
            } catch (RuntimeException | Error failure) {
                @Nullable BoundaryState failedFallback = fallbackEscalatedByFailure(group);
                if (failedFallback != null) {
                    escalateFailedBoundaryChain(failedFallback);
                }
                throw new AttemptProblem(
                        "dependency-pull-failed",
                        StructuralCallbackPhase.STRUCTURE,
                        group.ownerPath(),
                        nearestHealthyBoundary(group),
                        failure
                );
            }
            if (selected) {
                targets.add(group);
            }
        }
        for (GroupNode child : group.activeChildren) {
            collectDirtyTargets(child, ancestorSelected || selected, targets);
        }
    }

    /// Returns the nearest healthy committed boundary containing one group.
    private static @Nullable BoundaryState nearestHealthyBoundary(GroupNode group) {
        for (@Nullable GroupNode current = group; current != null; current = current.parent) {
            @Nullable BoundaryState boundary = current.boundaryState;
            if (boundary != null && boundary.status == ErrorBoundaryStatus.HEALTHY) {
                return boundary;
            }
        }
        return null;
    }

    /// Marks a normal group dirty or a measure group as requiring current-input materialization.
    private static void invalidateManualConsumer(GroupNode consumer) {
        if (consumer.kind == GroupKind.MEASURE) {
            consumer.needsMaterialization = true;
        } else {
            consumer.forceDirty = true;
        }
    }

    /// Returns the nearest failed boundary whose committed children are fallback topology.
    private static @Nullable BoundaryState fallbackEscalatedByFailure(GroupNode group) {
        for (@Nullable GroupNode current = group.parent; current != null; current = current.parent) {
            @Nullable BoundaryState boundary = current.boundaryState;
            if (boundary != null) {
                if (boundary.status == ErrorBoundaryStatus.HEALTHY) {
                    return null;
                }
                if (boundary.status == ErrorBoundaryStatus.FAILED) {
                    return boundary;
                }
            }
        }
        return null;
    }

    /// Marks a failed fallback and every failed declared ancestor as escalated.
    private static void escalateFailedBoundaryChain(BoundaryState boundary) {
        for (@Nullable BoundaryState current = boundary;
             current != null && current.status == ErrorBoundaryStatus.FAILED;
             current = current.parent) {
            current.status = ErrorBoundaryStatus.ESCALATED;
        }
    }

    /// Builds one successful attempt result.
    private StructuralAttemptResult committedResult(
            StructuralAttemptStatus attemptStatus,
            long revisionBefore,
            long epoch,
            int attemptedGroups,
            CommitOutcome outcome,
            @Nullable StructuralFailure failure
    ) {
        return new StructuralAttemptResult(
                attemptStatus,
                revisionBefore,
                revision,
                epoch,
                attemptedGroups,
                outcome.committedGroups,
                failure == null ? outcome.cleanupFailure : failure
        );
    }

    /// Builds a result for a runtime stopped by root containment.
    private StructuralAttemptResult failedResult(long revisionBefore, long epoch, int attemptedGroups) {
        return new StructuralAttemptResult(
                StructuralAttemptStatus.ROOT_FAILED,
                revisionBefore,
                revision,
                epoch,
                attemptedGroups,
                0,
                latestFailure
        );
    }

    /// Converts and retains one attempt problem.
    private StructuralFailure recordProblem(
            AttemptProblem problem,
            @Unmodifiable List<StructuralCleanupFailure> additionalCleanupFailures
    ) {
        ArrayList<StructuralCleanupFailure> cleanupFailures = new ArrayList<>(problem.cleanupFailures);
        cleanupFailures.addAll(additionalCleanupFailures);
        String boundaryPath = problem.boundary == null
                ? "root"
                : problem.boundary.ownerPath();
        StructuralFailure failure = newFailure(
                problem.code,
                problem.phase,
                problem.ownerPath,
                boundaryPath,
                problem.boundary == null
                        ? "reset-root-before-retry"
                        : "compose-boundary-fallback",
                problem.getCause(),
                cleanupFailures
        );
        recordFailure(failure);
        return failure;
    }

    /// Creates and retains one aggregated cleanup diagnostic.
    private StructuralFailure recordCleanupDiagnostic(
            @Unmodifiable List<StructuralCleanupFailure> cleanupFailures
    ) {
        StructuralFailure failure = newFailure(
                "cleanup-failed",
                StructuralCallbackPhase.CLEANUP,
                cleanupFailures.getFirst().ownerPath(),
                cleanupFailures.getFirst().boundaryPath(),
                "cleanup-completed-with-aggregation",
                null,
                cleanupFailures
        );
        recordFailure(failure);
        return failure;
    }

    /// Creates one immutable diagnostic according to the configured detail mode.
    private StructuralFailure newFailure(
            String code,
            StructuralCallbackPhase phase,
            String ownerPath,
            String boundaryPath,
            String recoveryAction,
            @Nullable Throwable cause,
            @Unmodifiable List<StructuralCleanupFailure> cleanupFailures
    ) {
        if (nextFailureSequence == Long.MAX_VALUE) {
            throw new IllegalStateException("Structural diagnostic sequence is exhausted");
        }
        long sequence = nextFailureSequence++;
        @Nullable Throwable retainedCause = config.diagnosticsMode() == StructuralDiagnosticsMode.DEBUG
                ? cause
                : null;
        List<StructuralCleanupFailure> retainedCleanup;
        if (config.diagnosticsMode() == StructuralDiagnosticsMode.DEBUG) {
            retainedCleanup = List.copyOf(cleanupFailures);
        } else {
            ArrayList<StructuralCleanupFailure> redacted = new ArrayList<>(cleanupFailures.size());
            for (StructuralCleanupFailure cleanupFailure : cleanupFailures) {
                redacted.add(new StructuralCleanupFailure(
                        cleanupFailure.ownerPath(),
                        cleanupFailure.boundaryPath(),
                        null
                ));
            }
            retainedCleanup = List.copyOf(redacted);
        }
        return new StructuralFailure(
                sequence,
                code,
                phase,
                ownerPath,
                boundaryPath,
                recoveryAction,
                retainedCause,
                retainedCleanup
        );
    }

    /// Retains one diagnostic under the configured count bound.
    private void recordFailure(StructuralFailure failure) {
        while (failures.size() >= config.maximumRetainedFailures()) {
            failures.removeFirst();
        }
        failures.addLast(failure);
        latestFailure = failure;
    }

    /// Runs one lifecycle callback under the state-write capture guard.
    private void runGuarded(Runnable callback) {
        ReactiveObservation observation = callbackGuard.capture(callback);
        // The capture is deliberately discarded; only its state-write guard is required.
        observation.close();
    }

    /// Compares two application values under the lifecycle state-write guard.
    private boolean guardedEquals(Object left, Object right) {
        boolean[] result = new boolean[1];
        runGuarded(() -> result[0] = Objects.equals(left, right));
        return result[0];
    }

    /// Deactivates a retained subtree child-before-parent without releasing memory or identity.
    private void deactivateTree(GroupNode group, ArrayList<StructuralCleanupFailure> cleanupFailures) {
        for (int index = group.activeChildren.size() - 1; index >= 0; index--) {
            deactivateTree(group.activeChildren.get(index), cleanupFailures);
        }
        for (int index = group.dormantChildren.size() - 1; index >= 0; index--) {
            deactivateTree(group.dormantChildren.get(index), cleanupFailures);
        }
        disposeDirectEffects(group, cleanupFailures);
        detachManualDependencies(group);
        group.observer.clearDependencies();
        if (group.kind == GroupKind.MEASURE) {
            group.needsMaterialization = true;
        }
    }

    /// Releases one removed subtree child-before-parent.
    private void releaseTree(GroupNode group, ArrayList<StructuralCleanupFailure> cleanupFailures) {
        releaseDescendants(group, cleanupFailures);
        disposeDirectEffects(group, cleanupFailures);
        disposeDirectMemories(group, cleanupFailures);
        detachManualDependencies(group);
        if (group.providerCell != null) {
            group.providerCell.consumers.clear();
        }
        if (group.measureKey != null && measureGroups.get(group.measureKey) == group) {
            measureGroups.remove(group.measureKey);
        }
        if (group.boundaryState != null && group.boundaryState.group == group) {
            group.boundaryState.group = null;
        }
        group.reactiveOwner.close();
        group.activeChildren.clear();
        group.dormantChildren.clear();
    }

    /// Releases every descendant of one retained group in reverse sibling order.
    private void releaseDescendants(GroupNode group, ArrayList<StructuralCleanupFailure> cleanupFailures) {
        for (int index = group.activeChildren.size() - 1; index >= 0; index--) {
            releaseTree(group.activeChildren.get(index), cleanupFailures);
        }
        for (int index = group.dormantChildren.size() - 1; index >= 0; index--) {
            releaseTree(group.dormantChildren.get(index), cleanupFailures);
        }
        group.activeChildren.clear();
        group.dormantChildren.clear();
    }

    /// Disposes every effect directly owned by one group in reverse declaration order.
    private void disposeDirectEffects(GroupNode group, ArrayList<StructuralCleanupFailure> cleanupFailures) {
        ArrayList<OwnedEffect> effects = new ArrayList<>(group.effects.values());
        for (int index = effects.size() - 1; index >= 0; index--) {
            disposeEffect(effects.get(index), cleanupFailures);
        }
        group.effects.clear();
    }

    /// Disposes one mounted effect once and aggregates a callback failure.
    private void disposeEffect(OwnedEffect effect, ArrayList<StructuralCleanupFailure> cleanupFailures) {
        if (effect.disposed) {
            return;
        }
        effect.disposed = true;
        try {
            runGuarded(effect.cleanup);
        } catch (RuntimeException | Error failure) {
                cleanupFailures.add(cleanupFailure(effect.owner, effect.ownerPath(), failure));
        }
    }

    /// Disposes every remembered slot directly owned by one group in reverse position order.
    private void disposeDirectMemories(GroupNode group, ArrayList<StructuralCleanupFailure> cleanupFailures) {
        for (int index = group.memories.size() - 1; index >= 0; index--) {
            disposeMemory(group.memories.get(index), cleanupFailures);
        }
        group.memories.clear();
    }

    /// Disposes one remembered resource or local cell once.
    private void disposeMemory(MemorySlot memory, ArrayList<StructuralCleanupFailure> cleanupFailures) {
        if (memory.disposed) {
            return;
        }
        memory.disposed = true;
        if (memory.value instanceof StructuralLocal<?> local) {
            for (GroupNode consumer : List.copyOf(local.consumers.keySet())) {
                consumer.localDependencies.remove(local);
            }
            local.consumers.clear();
            local.disposed = true;
        }
        if (memory.disposer != null) {
            try {
                Consumer<Object> disposer = memory.disposer;
                runGuarded(() -> disposer.accept(memory.value));
            } catch (RuntimeException | Error failure) {
                cleanupFailures.add(cleanupFailure(memory.owner, memory.ownerPath(), failure));
            }
        }
    }

    /// Creates one cleanup record using the configured cause-retention mode.
    private StructuralCleanupFailure cleanupFailure(GroupNode owner, String ownerPath, Throwable cause) {
        @Nullable BoundaryState boundary = nearestDeclaredBoundary(owner);
        return new StructuralCleanupFailure(
                ownerPath,
                boundary == null ? "root" : boundary.ownerPath(),
                config.diagnosticsMode() == StructuralDiagnosticsMode.DEBUG ? cause : null
        );
    }

    /// Returns the nearest declared boundary containing one cleanup owner.
    private static @Nullable BoundaryState nearestDeclaredBoundary(GroupNode owner) {
        for (@Nullable GroupNode current = owner; current != null; current = current.parent) {
            if (current.boundaryState != null) {
                return current.boundaryState;
            }
        }
        return null;
    }

    /// Detaches every ambient and local dependency directly owned by one group.
    private static void detachManualDependencies(GroupNode group) {
        for (ProviderCell provider : group.ambientDependencies.keySet()) {
            provider.consumers.remove(group);
        }
        group.ambientDependencies.clear();
        for (StructuralLocal<?> local : group.localDependencies.keySet()) {
            local.consumers.remove(group);
        }
        group.localDependencies.clear();
    }

    /// Appends one group and its descendants to a public snapshot.
    private void snapshotGroup(
            GroupNode group,
            StructuralGroupState inheritedState,
            List<StructuralGroupSnapshot> snapshots
    ) {
        ArrayList<Long> memories = new ArrayList<>(group.memories.size());
        for (MemorySlot memory : group.memories) {
            memories.add(memory.id);
        }
        snapshots.add(new StructuralGroupSnapshot(
                group.id,
                group.ownerPath(),
                group.sourceIdentity,
                group.semanticText,
                inheritedState,
                group.kind == GroupKind.MEASURE,
                group.boundaryState == null ? null : group.boundaryState.status,
                memories,
                group.activeChildren.size(),
                group.dormantChildren.size()
        ));
        for (GroupNode child : group.activeChildren) {
            snapshotGroup(child, inheritedState, snapshots);
        }
        for (GroupNode child : group.dormantChildren) {
            snapshotGroup(child, StructuralGroupState.DORMANT, snapshots);
        }
    }

    /// Removes healthy boundary records that have no committed declaration.
    private void pruneDetachedHealthyBoundaries() {
        boundaries.entrySet().removeIf(entry -> {
            BoundaryState boundary = entry.getValue();
            return boundary.group == null && boundary.status == ErrorBoundaryStatus.HEALTHY;
        });
    }

    /// Returns whether one group and every ancestor participate in active structure.
    private static boolean isActive(GroupNode group) {
        for (@Nullable GroupNode current = group; current != null && current.parent != null; current = current.parent) {
            if (!containsIdentity(current.parent.activeChildren, current)) {
                return false;
            }
        }
        return true;
    }

    /// Tests group-list membership with identity semantics.
    private static boolean containsIdentity(List<GroupNode> groups, GroupNode candidate) {
        for (GroupNode group : groups) {
            if (group == candidate) {
                return true;
            }
        }
        return false;
    }

    /// Tests memory-list membership with identity semantics.
    private static boolean containsIdentity(List<MemorySlot> memories, MemorySlot candidate) {
        for (MemorySlot memory : memories) {
            if (memory == candidate) {
                return true;
            }
        }
        return false;
    }

    /// Returns one group's depth below the root.
    private static int groupDepth(GroupNode group) {
        int depth = 0;
        for (@Nullable GroupNode current = group.parent; current != null; current = current.parent) {
            depth++;
        }
        return depth;
    }

    /// Allocates one positive group identity.
    private long allocateGroupId() {
        if (nextGroupId == Long.MAX_VALUE) {
            throw new IllegalStateException("Structural group identifiers are exhausted");
        }
        return nextGroupId++;
    }

    /// Allocates one positive remembered-slot identity.
    private long allocateMemoryId() {
        if (nextMemoryId == Long.MAX_VALUE) {
            throw new IllegalStateException("Structural memory identifiers are exhausted");
        }
        return nextMemoryId++;
    }

    /// Verifies entry to a callback-executing operation.
    private void checkOperationEntry() {
        checkMutationEntry();
        if (staging) {
            throw new IllegalStateException("Structural runtime cannot be reentered");
        }
    }

    /// Verifies entry to an owner-thread structural mutation.
    private void checkMutationEntry() {
        checkOwnerThread();
        checkNotClosed();
        if (domain.hasActiveTransaction()) {
            throw new IllegalStateException("Structural runtime cannot operate inside a state transaction");
        }
    }

    /// Verifies that closure has not completed.
    private void checkNotClosed() {
        if (status == StructuralRuntimeStatus.CLOSED) {
            throw new IllegalStateException("Structural runtime is closed");
        }
    }

    /// Implements one nested structural or current-measure draft traversal.
    @NotNullByDefault
    private final class Composer {
        /// The shared private attempt.
        private final AttemptContext context;

        /// The active group-draft stack.
        private final ArrayList<GroupDraft> stack = new ArrayList<>();

        /// The active ambient values by key identity.
        private final IdentityHashMap<AmbientKey<?>, AmbientValue> ambientValues = new IdentityHashMap<>();

        /// Healthy boundaries eligible to contain a callback failure.
        private final ArrayList<BoundaryState> healthyBoundaries = new ArrayList<>();

        /// The nearest failed boundary whose fallback topology is executing, or `null`.
        private @Nullable BoundaryState fallbackBoundary;

        /// Healthy-boundary stack depth immediately outside the active fallback.
        private int fallbackHealthyDepth = -1;

        /// The callback phase used for application-code failures.
        private final StructuralCallbackPhase phase;

        /// Creates a traversal rooted at one committed group draft.
        ///
        /// @param context the shared attempt
        /// @param rootDraft the selected root draft
        /// @param phase the callback phase
        private Composer(AttemptContext context, GroupDraft rootDraft, StructuralCallbackPhase phase) {
            this.context = context;
            this.phase = phase;
            seedAncestorContext(rootDraft.group);
            stack.add(rootDraft);
            rootDraft.failureBoundary = nearestBoundary();
            rootDraft.fallbackBoundary = fallbackBoundary;
            rootDraft.escalatesFallback = shouldEscalateFallback();
        }

        /// Executes the selected normal root according to its committed descriptor.
        private void executeCurrent() {
            executeDraft(stack.getLast());
        }

        /// Executes one restricted measure callback with a checked input.
        private <I> void executeMeasure(I input) {
            GroupDraft draft = stack.getLast();
            @Nullable MeasureStructuralContent<?> content = draft.measureContent;
            if (content == null) {
                throw new IllegalStateException("Measure content is missing");
            }
            MeasureSession session = new MeasureSession(this, draft);
            MeasureStructuralScope scope = new MeasureStructuralScope(session);
            ReactiveObservation observation;
            @Nullable Composer previousComposer = currentComposer;
            currentComposer = this;
            try {
                observation = draft.group.observer.capture(() -> {
                    try {
                        invokeMeasure(content, scope, input);
                    } catch (CancellationSignal | AttemptProblem signal) {
                        throw signal;
                    } catch (RuntimeException | Error failure) {
                        throw problem("measure-callback-failed", draft, failure);
                    }
                });
            } finally {
                scope.deactivate();
                currentComposer = previousComposer;
            }
            draft.observation = observation;
            context.observations.add(observation);
        }

        /// Executes one draft callback according to its group kind.
        private void executeDraft(GroupDraft draft) {
            switch (draft.kind) {
                case ROOT, NORMAL, BRANCH -> executeContent(draft, requireContent(draft), null);
                case PROVIDER -> executeProvider(draft);
                case BOUNDARY -> executeBoundary(draft);
                case MEASURE -> {
                    if (!draft.declarationOnly) {
                        throw new IllegalStateException("Measure groups execute only through materialize");
                    }
                }
            }
        }

        /// Executes one ambient provider callback under its staged override.
        private void executeProvider(GroupDraft draft) {
            ProviderCell provider = Objects.requireNonNull(draft.providerCell, "providerCell");
            Object value = Objects.requireNonNull(draft.providerValue, "providerValue");
            @Nullable AmbientValue previous = ambientValues.put(provider.key, new AmbientValue(provider, value));
            try {
                executeContent(draft, requireContent(draft), null);
            } finally {
                if (previous == null) {
                    ambientValues.remove(provider.key);
                } else {
                    ambientValues.put(provider.key, previous);
                }
            }
        }

        /// Executes normal or fallback boundary content under the boundary recovery state.
        private void executeBoundary(GroupDraft draft) {
            BoundaryState boundary = Objects.requireNonNull(draft.boundaryState, "boundaryState");
            if (boundary.status == ErrorBoundaryStatus.HEALTHY) {
                healthyBoundaries.add(boundary);
                draft.failureBoundary = boundary;
                draft.escalatesFallback = false;
                try {
                    executeContent(draft, Objects.requireNonNull(draft.normalContent, "normalContent"), boundary);
                } catch (AttemptProblem problem) {
                    boundary.parent = draft.boundaryParent;
                    throw problem;
                } finally {
                    healthyBoundaries.removeLast();
                }
                return;
            }
            if (boundary.status == ErrorBoundaryStatus.FAILED) {
                draft.failureBoundary = nearestBoundary();
                @Nullable BoundaryState previousFallback = fallbackBoundary;
                int previousFallbackHealthyDepth = fallbackHealthyDepth;
                fallbackBoundary = boundary;
                fallbackHealthyDepth = healthyBoundaries.size();
                draft.fallbackBoundary = boundary;
                draft.escalatesFallback = true;
                try {
                    executeContent(draft, Objects.requireNonNull(draft.fallbackContent, "fallbackContent"), null);
                } catch (CancellationSignal cancellationSignal) {
                    throw cancellationSignal;
                } catch (AttemptProblem problem) {
                    boundary.parent = draft.boundaryParent;
                    boundary.status = ErrorBoundaryStatus.ESCALATED;
                    throw problem;
                } finally {
                    fallbackBoundary = previousFallback;
                    fallbackHealthyDepth = previousFallbackHealthyDepth;
                }
                return;
            }
            throw problem("boundary-escalated", draft, null);
        }

        /// Captures one normal structural content callback and its nested declarations.
        private void executeContent(
                GroupDraft draft,
                StructuralContent content,
                @Nullable BoundaryState callbackBoundary
        ) {
            ScopeSession session = new ScopeSession(this, draft);
            StructuralScope scope = new StructuralScope(session);
            ReactiveObservation observation;
            @Nullable Composer previousComposer = currentComposer;
            currentComposer = this;
            try {
                observation = draft.group.observer.capture(() -> {
                    try {
                        content.compose(scope);
                    } catch (CancellationSignal | AttemptProblem signal) {
                        throw signal;
                    } catch (RuntimeException | Error failure) {
                        throw problem("structure-callback-failed", draft, failure);
                    }
                });
            } finally {
                scope.deactivate();
                currentComposer = previousComposer;
            }
            draft.failureBoundary = callbackBoundary == null ? draft.failureBoundary : callbackBoundary;
            draft.observation = observation;
            context.observations.add(observation);
        }

        /// Returns the required normal callback from one draft.
        private StructuralContent requireContent(GroupDraft draft) {
            return Objects.requireNonNull(draft.content, "Structural content is missing");
        }

        /// Declares and executes one positional or semantic child.
        private void enter(
                GroupDraft parent,
                String sourceIdentity,
                @Nullable Object semanticKey,
                GroupKind kind,
                StructuralContent content
        ) {
            String source = StructuralContracts.requireName(sourceIdentity, "sourceIdentity");
            Objects.requireNonNull(content, "content");
            @Nullable GroupNode previous = semanticKey == null
                    ? parent.selectPositional(source, kind, this)
                    : parent.selectSemantic(source, semanticKey, kind, this);
            GroupNode group = previous == null
                    ? createGroup(parent.group, source, semanticKey, kind)
                    : previous;
            GroupDraft child = GroupDraft.fromCommitted(group, context);
            child.content = content;
            child.failureBoundary = nearestBoundary();
            child.fallbackBoundary = fallbackBoundary;
            child.escalatesFallback = shouldEscalateFallback();
            parent.activeChildren.add(child);
            stack.add(child);
            try {
                executeDraft(child);
            } finally {
                stack.removeLast();
            }
        }

        /// Declares one conditional child or retains its inactive identity.
        private void branch(
                GroupDraft parent,
                String sourceIdentity,
                boolean visible,
                BranchRetention retention,
                StructuralContent content
        ) {
            String source = StructuralContracts.requireName(sourceIdentity, "sourceIdentity");
            Objects.requireNonNull(retention, "retention");
            Objects.requireNonNull(content, "content");
            parent.declareSemantic(source, BRANCH_KEY, this);
            @Nullable GroupNode previous = parent.findSemantic(source, BRANCH_KEY, GroupKind.BRANCH, this);
            if (!visible) {
                if (retention == BranchRetention.RETAIN && previous != null) {
                    parent.used.put(previous, Boolean.TRUE);
                    parent.dormantChildren.add(previous);
                }
                return;
            }
            GroupNode group = previous == null
                    ? createGroup(parent.group, source, BRANCH_KEY, GroupKind.BRANCH)
                    : previous;
            parent.used.put(group, Boolean.TRUE);
            GroupDraft child = GroupDraft.fromCommitted(group, context);
            child.content = content;
            child.failureBoundary = nearestBoundary();
            child.fallbackBoundary = fallbackBoundary;
            child.escalatesFallback = shouldEscalateFallback();
            parent.activeChildren.add(child);
            stack.add(child);
            try {
                executeDraft(child);
            } finally {
                stack.removeLast();
            }
        }

        /// Declares and executes one positional ambient provider.
        private <T> void provider(
                GroupDraft parent,
                String sourceIdentity,
                AmbientKey<T> key,
                T value,
                StructuralContent content
        ) {
            String source = StructuralContracts.requireName(sourceIdentity, "sourceIdentity");
            Objects.requireNonNull(key, "key");
            T checkedValue = key.valueType().cast(Objects.requireNonNull(value, "value"));
            @Nullable GroupNode previous = parent.selectPositional(source, GroupKind.PROVIDER, this);
            if (previous != null && previous.providerCell != null && previous.providerCell.key != key) {
                throw problem("positional-provider-key-changed", parent, null);
            }
            GroupNode group = previous == null
                    ? createGroup(parent.group, source, null, GroupKind.PROVIDER)
                    : previous;
            GroupDraft child = GroupDraft.fromCommitted(group, context);
            child.content = Objects.requireNonNull(content, "content");
            child.providerCell = group.providerCell == null ? new ProviderCell(key, checkedValue) : group.providerCell;
            child.providerValue = checkedValue;
            child.failureBoundary = nearestBoundary();
            child.fallbackBoundary = fallbackBoundary;
            child.escalatesFallback = shouldEscalateFallback();
            parent.activeChildren.add(child);
            stack.add(child);
            try {
                executeDraft(child);
            } finally {
                stack.removeLast();
            }
        }

        /// Declares and executes one positional error boundary.
        private void boundary(
                GroupDraft parent,
                String sourceIdentity,
                ErrorBoundaryKey key,
                StructuralContent content,
                StructuralContent fallback
        ) {
            String source = StructuralContracts.requireName(sourceIdentity, "sourceIdentity");
            Objects.requireNonNull(key, "key");
            @Nullable GroupNode previous = parent.selectPositional(source, GroupKind.BOUNDARY, this);
            if (previous != null
                    && previous.boundaryState != null
                    && previous.boundaryState.key != key) {
                throw problem("positional-boundary-key-changed", parent, null);
            }
            GroupNode group = previous == null
                    ? createGroup(parent.group, source, null, GroupKind.BOUNDARY)
                    : previous;
            BoundaryState state = context.declareBoundary(key, group, this);
            if (group.boundaryState == null) {
                group.boundaryState = state;
            }
            GroupDraft child = GroupDraft.fromCommitted(group, context);
            child.boundaryState = state;
            child.boundaryParent = nearestDeclaredBoundary();
            child.normalContent = Objects.requireNonNull(content, "content");
            child.fallbackContent = Objects.requireNonNull(fallback, "fallback");
            child.failureBoundary = nearestBoundary();
            child.fallbackBoundary = fallbackBoundary;
            child.escalatesFallback = shouldEscalateFallback();
            parent.activeChildren.add(child);
            stack.add(child);
            try {
                executeDraft(child);
            } finally {
                stack.removeLast();
            }
        }

        /// Declares one positional current-measure group while preserving its current viewport.
        private <I> void measure(
                GroupDraft parent,
                String sourceIdentity,
                MeasureMaterializationKey<I> key,
                MeasureStructuralContent<I> content
        ) {
            String source = StructuralContracts.requireName(sourceIdentity, "sourceIdentity");
            Objects.requireNonNull(key, "key");
            @Nullable GroupNode previous = parent.selectPositional(source, GroupKind.MEASURE, this);
            if (previous != null && previous.measureKey != null && previous.measureKey != key) {
                throw problem("positional-measure-key-changed", parent, null);
            }
            GroupNode group = previous == null
                    ? createGroup(parent.group, source, null, GroupKind.MEASURE)
                    : previous;
            context.declareMeasure(key, group, this);
            GroupDraft child = GroupDraft.fromCommitted(group, context);
            child.declarationOnly = true;
            child.measureKey = key;
            child.measureContent = Objects.requireNonNull(content, "content");
            child.failureBoundary = nearestBoundary();
            child.fallbackBoundary = fallbackBoundary;
            child.escalatesFallback = shouldEscalateFallback();
            parent.activeChildren.add(child);
        }

        /// Creates one uncommitted group and its reactive observer.
        private GroupNode createGroup(
                GroupNode parent,
                String sourceIdentity,
                @Nullable Object semanticKey,
                GroupKind kind
        ) {
            int semanticHash = semanticKey == null ? 0 : semanticHash(semanticKey, stack.getLast());
            @Nullable String semanticText = semanticKey == null
                    ? null
                    : semanticKey == BRANCH_KEY ? "branch" : String.valueOf(semanticKey);
            long groupId = allocateGroupId();
            ReactiveOwner owner = parent.reactiveOwner.createChild();
            final GroupNode group;
            try {
                group = new GroupNode(
                        groupId,
                        sourceIdentity,
                        semanticKey,
                        semanticHash,
                        semanticText,
                        kind,
                        parent,
                        owner,
                        owner.createObserver()
                );
            } catch (RuntimeException | Error failure) {
                owner.close();
                throw failure;
            }
            context.newGroups.add(group);
            return group;
        }

        /// Returns one positional remembered value or creates its first slot.
        private <T> T remember(
                GroupDraft draft,
                Class<T> valueType,
                Supplier<? extends T> factory,
                @Nullable Consumer<? super T> disposer
        ) {
            Objects.requireNonNull(valueType, "valueType");
            int position = draft.memoryCursor++;
            if (position < draft.group.memories.size()) {
                MemorySlot memory = draft.group.memories.get(position);
                boolean expectsResource = disposer != null;
                if (memory.valueType != valueType
                        || memory.resource != expectsResource
                        || memory.local) {
                    throw problem("positional-memory-contract-changed", draft, null);
                }
                draft.memories.add(memory);
                return valueType.cast(memory.value);
            }

            long memoryId = allocateMemoryId();
            T value = valueType.cast(Objects.requireNonNull(factory.get(), "remembered value"));
            @Nullable Consumer<Object> erasedDisposer = null;
            if (disposer != null) {
                erasedDisposer = eraseDisposer(disposer);
            }
            MemorySlot memory = new MemorySlot(
                    memoryId,
                    draft.group,
                    position,
                    valueType,
                    value,
                    erasedDisposer,
                    disposer != null,
                    false
            );
            draft.memories.add(memory);
            context.newMemories.add(memory);
            return value;
        }

        /// Returns one positional local cell or creates its first slot.
        private <T> StructuralLocal<T> rememberLocal(GroupDraft draft, Class<T> valueType, T initialValue) {
            Objects.requireNonNull(valueType, "valueType");
            T checkedInitialValue = valueType.cast(Objects.requireNonNull(initialValue, "initialValue"));
            int position = draft.memoryCursor++;
            if (position < draft.group.memories.size()) {
                MemorySlot memory = draft.group.memories.get(position);
                if (memory.valueType != valueType || !memory.local) {
                    throw problem("positional-memory-contract-changed", draft, null);
                }
                draft.memories.add(memory);
                return castLocal(memory.value);
            }

            long memoryId = allocateMemoryId();
            StructuralLocal<T> local = new StructuralLocal<>(
                    StructuralRuntime.this,
                    draft.group,
                    valueType,
                    checkedInitialValue
            );
            MemorySlot memory = new MemorySlot(
                    memoryId,
                    draft.group,
                    position,
                    valueType,
                    local,
                    null,
                    false,
                    true
            );
            draft.memories.add(memory);
            context.newMemories.add(memory);
            return local;
        }

        /// Records one local dependency in the current group draft.
        private void recordLocalRead(StructuralLocal<?> local) {
            GroupDraft draft = stack.getLast();
            draft.localDependencies.put(local, Boolean.TRUE);
        }

        /// Reads one ambient value and records the selected provider edge.
        private <T> T ambient(GroupDraft draft, AmbientKey<T> key) {
            Objects.requireNonNull(key, "key");
            @Nullable AmbientValue ambient = ambientValues.get(key);
            if (ambient == null) {
                return key.defaultValue();
            }
            draft.ambientDependencies.put(ambient.provider, Boolean.TRUE);
            return key.valueType().cast(ambient.value);
        }

        /// Declares one effect in its current group.
        private void effect(GroupDraft draft, String key, Runnable mount, Runnable cleanup) {
            String checkedKey = StructuralContracts.requireName(key, "key");
            if (draft.effects.containsKey(checkedKey)) {
                throw problem("duplicate-effect-key", draft, null);
            }
            @Nullable OwnedEffect previous = draft.group.effects.get(checkedKey);
            draft.effects.put(checkedKey, new EffectDeclaration(
                    checkedKey,
                    Objects.requireNonNull(mount, "mount"),
                    Objects.requireNonNull(cleanup, "cleanup"),
                    previous
            ));
        }

        /// Checks one explicit cooperative-cancellation point.
        private void checkpoint() {
            if (context.cancellation != null && context.cancellation.isCancelled()) {
                throw CancellationSignal.INSTANCE;
            }
        }

        /// Creates one problem attributed to the current eligible boundary.
        private AttemptProblem problem(String code, GroupDraft draft, @Nullable Throwable cause) {
            if (shouldEscalateFallback() && fallbackBoundary != null) {
                escalateFailedBoundaryChain(fallbackBoundary);
            }
            return new AttemptProblem(
                    StructuralContracts.requireName(code, "code"),
                    phase,
                    draft.group.ownerPath(),
                    nearestBoundary(),
                    cause
            );
        }

        /// Returns the innermost healthy boundary eligible for containment.
        private @Nullable BoundaryState nearestBoundary() {
            return healthyBoundaries.isEmpty() ? null : healthyBoundaries.getLast();
        }

        /// Returns whether a failure at the current frame escapes the active fallback boundary.
        private boolean shouldEscalateFallback() {
            return fallbackBoundary != null && healthyBoundaries.size() <= fallbackHealthyDepth;
        }

        /// Returns the nearest declared boundary regardless of its recovery status.
        private @Nullable BoundaryState nearestDeclaredBoundary() {
            for (int index = stack.size() - 1; index >= 0; index--) {
                @Nullable BoundaryState boundary = stack.get(index).boundaryState;
                if (boundary != null) {
                    return boundary;
                }
            }
            for (@Nullable GroupNode current = stack.getFirst().group.parent;
                 current != null;
                 current = current.parent) {
                if (current.boundaryState != null) {
                    return current.boundaryState;
                }
            }
            return null;
        }

        /// Seeds committed ambient providers and healthy boundaries from root to parent.
        private void seedAncestorContext(GroupNode group) {
            ArrayList<GroupNode> ancestors = new ArrayList<>();
            for (@Nullable GroupNode current = group.parent; current != null; current = current.parent) {
                ancestors.add(current);
            }
            for (int index = ancestors.size() - 1; index >= 0; index--) {
                GroupNode ancestor = ancestors.get(index);
                if (ancestor.providerCell != null) {
                    ProviderCell provider = ancestor.providerCell;
                    ambientValues.put(provider.key, new AmbientValue(provider, provider.value));
                }
                if (ancestor.boundaryState != null
                        && ancestor.boundaryState.status == ErrorBoundaryStatus.HEALTHY) {
                    healthyBoundaries.add(ancestor.boundaryState);
                } else if (ancestor.boundaryState != null
                        && ancestor.boundaryState.status == ErrorBoundaryStatus.FAILED) {
                    fallbackBoundary = ancestor.boundaryState;
                    fallbackHealthyDepth = healthyBoundaries.size();
                }
            }
        }

        /// Returns a semantic-key hash or reports application key failure.
        private int semanticHash(Object key, GroupDraft draft) {
            try {
                return key.hashCode();
            } catch (RuntimeException | Error failure) {
                throw problem("semantic-key-hash-failed", draft, failure);
            }
        }

        /// Compares two semantic keys or reports application key failure.
        private boolean semanticEquals(Object left, Object right, GroupDraft draft) {
            try {
                return left.equals(right);
            } catch (RuntimeException | Error failure) {
                throw problem("semantic-key-equality-failed", draft, failure);
            }
        }

        /// Invokes a wildcard measure callback after runtime input validation.
        @SuppressWarnings("unchecked")
        private <I> void invokeMeasure(
                MeasureStructuralContent<?> content,
                MeasureStructuralScope scope,
                I input
        ) {
            ((MeasureStructuralContent<I>) content).materialize(scope, input);
        }
    }

    /// Implements one callback-local normal structural scope.
    @NotNullByDefault
    private final class ScopeSession implements StructuralScopeSession {
        /// The active composer.
        private final Composer composer;

        /// The group draft owned by this scope.
        private final GroupDraft draft;

        /// Creates one scope session.
        ///
        /// @param composer the active composer
        /// @param draft the owned draft
        private ScopeSession(Composer composer, GroupDraft draft) {
            this.composer = composer;
            this.draft = draft;
        }

        /// {@inheritDoc}
        @Override
        public void group(String sourceIdentity, StructuralContent content) {
            checkCurrent();
            composer.enter(draft, sourceIdentity, null, GroupKind.NORMAL, content);
        }

        /// {@inheritDoc}
        @Override
        public void keyedGroup(String sourceIdentity, Object semanticKey, StructuralContent content) {
            checkCurrent();
            composer.enter(
                    draft,
                    sourceIdentity,
                    Objects.requireNonNull(semanticKey, "semanticKey"),
                    GroupKind.NORMAL,
                    content
            );
        }

        /// {@inheritDoc}
        @Override
        public void branch(
                String sourceIdentity,
                boolean visible,
                BranchRetention retention,
                StructuralContent content
        ) {
            checkCurrent();
            composer.branch(draft, sourceIdentity, visible, retention, content);
        }

        /// {@inheritDoc}
        @Override
        public <T> T remember(Class<T> valueType, Supplier<? extends T> factory) {
            checkCurrent();
            return composer.remember(
                    draft,
                    valueType,
                    Objects.requireNonNull(factory, "factory"),
                    null
            );
        }

        /// {@inheritDoc}
        @Override
        public <T> StructuralLocal<T> rememberLocal(Class<T> valueType, T initialValue) {
            checkCurrent();
            return composer.rememberLocal(draft, valueType, initialValue);
        }

        /// {@inheritDoc}
        @Override
        public <T> T rememberResource(
                Class<T> valueType,
                Supplier<? extends T> factory,
                Consumer<? super T> disposer
        ) {
            checkCurrent();
            return composer.remember(
                    draft,
                    valueType,
                    Objects.requireNonNull(factory, "factory"),
                    Objects.requireNonNull(disposer, "disposer")
            );
        }

        /// {@inheritDoc}
        @Override
        public void effect(String key, Runnable mount, Runnable cleanup) {
            checkCurrent();
            composer.effect(draft, key, mount, cleanup);
        }

        /// {@inheritDoc}
        @Override
        public <T> T ambient(AmbientKey<T> key) {
            checkCurrent();
            return composer.ambient(draft, key);
        }

        /// {@inheritDoc}
        @Override
        public <T> void provideAmbient(
                String sourceIdentity,
                AmbientKey<T> key,
                T value,
                StructuralContent content
        ) {
            checkCurrent();
            composer.provider(draft, sourceIdentity, key, value, content);
        }

        /// {@inheritDoc}
        @Override
        public void errorBoundary(
                String sourceIdentity,
                ErrorBoundaryKey key,
                StructuralContent content,
                StructuralContent fallback
        ) {
            checkCurrent();
            composer.boundary(draft, sourceIdentity, key, content, fallback);
        }

        /// {@inheritDoc}
        @Override
        public <I> void measureGroup(
                String sourceIdentity,
                MeasureMaterializationKey<I> key,
                MeasureStructuralContent<I> content
        ) {
            checkCurrent();
            composer.measure(draft, sourceIdentity, key, content);
        }

        /// {@inheritDoc}
        @Override
        public void checkpoint() {
            checkCurrent();
            composer.checkpoint();
        }

        /// {@inheritDoc}
        @Override
        public void fail(String code) {
            checkCurrent();
            throw composer.problem(StructuralContracts.requireName(code, "code"), draft, null);
        }

        /// Verifies that this scope's group remains the active callback frame.
        private void checkCurrent() {
            if (composer.stack.isEmpty() || composer.stack.getLast() != draft) {
                throw new IllegalStateException("Structural scope is not the current callback scope");
            }
        }
    }

    /// Implements one restricted current-measure scope.
    @NotNullByDefault
    private final class MeasureSession implements MeasureStructuralScopeSession {
        /// The active composer.
        private final Composer composer;

        /// The materialization group draft.
        private final GroupDraft draft;

        /// The number of direct children declared so far.
        private int childCount;

        /// Creates one measure session.
        ///
        /// @param composer the active composer
        /// @param draft the materialization draft
        private MeasureSession(Composer composer, GroupDraft draft) {
            this.composer = composer;
            this.draft = draft;
        }

        /// {@inheritDoc}
        @Override
        public void keyedGroup(String sourceIdentity, Object semanticKey, StructuralContent content) {
            checkCurrent();
            childCount++;
            if (childCount > config.maximumMaterializedChildren()) {
                throw composer.problem("materialization-child-budget-exceeded", draft, null);
            }
            composer.enter(
                    draft,
                    sourceIdentity,
                    Objects.requireNonNull(semanticKey, "semanticKey"),
                    GroupKind.NORMAL,
                    content
            );
        }

        /// {@inheritDoc}
        @Override
        public <T> T ambient(AmbientKey<T> key) {
            checkCurrent();
            return composer.ambient(draft, key);
        }

        /// {@inheritDoc}
        @Override
        public void checkpoint() {
            checkCurrent();
            composer.checkpoint();
        }

        /// {@inheritDoc}
        @Override
        public void fail(String code) {
            checkCurrent();
            throw composer.problem(StructuralContracts.requireName(code, "code"), draft, null);
        }

        /// Verifies that the materialization group remains current.
        private void checkCurrent() {
            if (composer.stack.isEmpty() || composer.stack.getLast() != draft) {
                throw new IllegalStateException("Measure structural scope is not current");
            }
        }
    }

    /// Casts an erased remembered local cell.
    @SuppressWarnings("unchecked")
    private static <T> StructuralLocal<T> castLocal(Object value) {
        return (StructuralLocal<T>) value;
    }

    /// Erases one typed disposer after its resource has been checked by its class token.
    @SuppressWarnings("unchecked")
    private static <T> Consumer<Object> eraseDisposer(Consumer<? super T> disposer) {
        return value -> ((Consumer<T>) disposer).accept((T) value);
    }

    /// Distinguishes the structural contract of a stable group identity.
    @NotNullByDefault
    private enum GroupKind {
        /// The stable runtime root.
        ROOT,

        /// An ordinary positional or semantic-keyed group.
        NORMAL,

        /// An explicit conditional branch.
        BRANCH,

        /// An inherited-value provider.
        PROVIDER,

        /// An application error boundary.
        BOUNDARY,

        /// A layout-owned current-measure scope.
        MEASURE
    }

    /// Stores committed identity, ownership, callbacks, memory, dependencies, and descendants.
    @NotNullByDefault
    static final class GroupNode {
        /// The positive runtime-local identity.
        final long id;

        /// The handwritten source identity.
        final String sourceIdentity;

        /// The semantic key object, or `null` for positional identity.
        final @Nullable Object semanticKey;

        /// The semantic key hash captured at creation.
        final int semanticHash;

        /// The diagnostic semantic text captured at creation, or `null`.
        final @Nullable String semanticText;

        /// The fixed group contract kind.
        final GroupKind kind;

        /// The stable parent, or `null` for the root.
        final @Nullable GroupNode parent;

        /// The reactive owner bounded by this group.
        final ReactiveOwner reactiveOwner;

        /// The structural dependency observer.
        final ReactiveObserver observer;

        /// Direct active children in committed order.
        ArrayList<GroupNode> activeChildren = new ArrayList<>();

        /// Direct retained inactive children in committed order.
        ArrayList<GroupNode> dormantChildren = new ArrayList<>();

        /// Direct positional memory slots.
        ArrayList<MemorySlot> memories = new ArrayList<>();

        /// Direct active effects by local key.
        LinkedHashMap<String, OwnedEffect> effects = new LinkedHashMap<>();

        /// Committed ambient dependencies.
        IdentityHashMap<ProviderCell, Boolean> ambientDependencies = new IdentityHashMap<>();

        /// Committed local-value dependencies.
        IdentityHashMap<StructuralLocal<?>, Boolean> localDependencies = new IdentityHashMap<>();

        /// The ordinary callback, or `null` for specialized groups before declaration.
        @Nullable StructuralContent content;

        /// The ambient provider cell for a provider group, or `null`.
        @Nullable ProviderCell providerCell;

        /// The recovery record for a boundary group, or `null`.
        @Nullable BoundaryState boundaryState;

        /// Normal boundary content, or `null`.
        @Nullable StructuralContent normalContent;

        /// Boundary fallback content, or `null`.
        @Nullable StructuralContent fallbackContent;

        /// The current-measure key, or `null`.
        @Nullable MeasureMaterializationKey<?> measureKey;

        /// The current-measure callback, or `null`.
        @Nullable MeasureStructuralContent<?> measureContent;

        /// Whether non-reactive invalidation requires this group to rerun.
        boolean forceDirty;

        /// Whether a measure group needs current-input materialization.
        boolean needsMaterialization;

        /// Creates one stable group record.
        ///
        /// @param id the positive identity
        /// @param sourceIdentity the source identity
        /// @param semanticKey the semantic key, or `null`
        /// @param semanticHash the captured semantic hash
        /// @param semanticText the captured diagnostic key text, or `null`
        /// @param kind the fixed contract kind
        /// @param parent the stable parent, or `null`
        /// @param reactiveOwner the reactive owner
        /// @param observer the structural observer
        GroupNode(
                long id,
                String sourceIdentity,
                @Nullable Object semanticKey,
                int semanticHash,
                @Nullable String semanticText,
                GroupKind kind,
                @Nullable GroupNode parent,
                ReactiveOwner reactiveOwner,
                ReactiveObserver observer
        ) {
            this.id = id;
            this.sourceIdentity = sourceIdentity;
            this.semanticKey = semanticKey;
            this.semanticHash = semanticHash;
            this.semanticText = semanticText;
            this.kind = kind;
            this.parent = parent;
            this.reactiveOwner = reactiveOwner;
            this.observer = observer;
        }

        /// Returns the deterministic source/key path.
        ///
        /// @return the owner path
        String ownerPath() {
            if (parent == null) {
                return "root";
            }
            String suffix = semanticText == null ? sourceIdentity : sourceIdentity + '[' + semanticText + ']';
            return parent.ownerPath() + '/' + suffix;
        }
    }

    /// Stores one private mutable group draft.
    @NotNullByDefault
    private static final class GroupDraft {
        /// The stable committed or newly allocated identity.
        private final GroupNode group;

        /// The shared attempt that owns this draft.
        private final AttemptContext context;

        /// The fixed group kind.
        private final GroupKind kind;

        /// Active child drafts in next order.
        private final ArrayList<GroupDraft> activeChildren = new ArrayList<>();

        /// Retained inactive child identities in next order.
        private final ArrayList<GroupNode> dormantChildren = new ArrayList<>();

        /// Selected positional memories.
        private final ArrayList<MemorySlot> memories = new ArrayList<>();

        /// Declared effects by local key.
        private final LinkedHashMap<String, EffectDeclaration> effects = new LinkedHashMap<>();

        /// Draft ambient dependencies.
        private final IdentityHashMap<ProviderCell, Boolean> ambientDependencies = new IdentityHashMap<>();

        /// Draft local dependencies.
        private final IdentityHashMap<StructuralLocal<?>, Boolean> localDependencies = new IdentityHashMap<>();

        /// Old children already consumed by this draft.
        private final IdentityHashMap<GroupNode, Boolean> used = new IdentityHashMap<>();

        /// Semantic declarations used for duplicate detection.
        private final ArrayList<SemanticDeclaration> semanticDeclarations = new ArrayList<>();

        /// The next unkeyed child position.
        private int positionalCursor;

        /// The next memory position.
        private int memoryCursor;

        /// The selected callback.
        private @Nullable StructuralContent content;

        /// The staged provider cell.
        private @Nullable ProviderCell providerCell;

        /// The staged provider value.
        private @Nullable Object providerValue;

        /// The staged boundary record.
        private @Nullable BoundaryState boundaryState;

        /// The staged boundary parent.
        private @Nullable BoundaryState boundaryParent;

        /// The staged normal boundary callback.
        private @Nullable StructuralContent normalContent;

        /// The staged fallback boundary callback.
        private @Nullable StructuralContent fallbackContent;

        /// The staged measure identity.
        private @Nullable MeasureMaterializationKey<?> measureKey;

        /// The staged measure callback.
        private @Nullable MeasureStructuralContent<?> measureContent;

        /// The nearest boundary eligible for effect-mount containment.
        private @Nullable BoundaryState failureBoundary;

        /// The failed boundary whose fallback owns this draft, or `null`.
        private @Nullable BoundaryState fallbackBoundary;

        /// Whether a later effect failure escapes the owning fallback.
        private boolean escalatesFallback;

        /// The detached structural observation.
        private @Nullable ReactiveObservation observation;

        /// Whether this only refreshes a measure declaration and preserves its viewport.
        private boolean declarationOnly;

        /// Whether this draft is executing measure materialization.
        private boolean materializing;

        /// Whether the staged provider value differs from its committed value.
        private boolean providerChanged;

        /// Creates one draft from a committed or new group.
        ///
        /// @param group the stable identity
        /// @param context the owning attempt
        private GroupDraft(GroupNode group, AttemptContext context) {
            this.group = group;
            this.context = context;
            this.kind = group.kind;
            this.content = group.content;
            this.providerCell = group.providerCell;
            this.providerValue = group.providerCell == null ? null : group.providerCell.value;
            this.boundaryState = group.boundaryState;
            this.boundaryParent = group.boundaryState == null ? null : group.boundaryState.parent;
            this.normalContent = group.normalContent;
            this.fallbackContent = group.fallbackContent;
            this.measureKey = group.measureKey;
            this.measureContent = group.measureContent;
            context.allDrafts.add(this);
        }

        /// Creates one draft from a stable group record.
        private static GroupDraft fromCommitted(GroupNode group, AttemptContext context) {
            return new GroupDraft(group, context);
        }

        /// Selects one positional child with an unchanged source and kind.
        private @Nullable GroupNode selectPositional(
                String sourceIdentity,
                GroupKind expectedKind,
                Composer composer
        ) {
            int targetPosition = positionalCursor++;
            int unkeyedPosition = 0;
            for (GroupNode candidate : group.activeChildren) {
                if (candidate.semanticKey != null) {
                    continue;
                }
                if (unkeyedPosition == targetPosition) {
                    if (candidate.sourceIdentity.equals(sourceIdentity)
                            && candidate.kind == expectedKind
                            && used.put(candidate, Boolean.TRUE) == null) {
                        return candidate;
                    }
                    return null;
                }
                unkeyedPosition++;
            }
            return null;
        }

        /// Selects one semantic child after duplicate and key-stability validation.
        private @Nullable GroupNode selectSemantic(
                String sourceIdentity,
                Object semanticKey,
                GroupKind expectedKind,
                Composer composer
        ) {
            declareSemantic(sourceIdentity, semanticKey, composer);
            @Nullable GroupNode candidate = findSemantic(sourceIdentity, semanticKey, expectedKind, composer);
            if (candidate != null) {
                used.put(candidate, Boolean.TRUE);
            }
            return candidate;
        }

        /// Records one semantic declaration or rejects a duplicate.
        private void declareSemantic(String sourceIdentity, Object semanticKey, Composer composer) {
            int hash = composer.semanticHash(semanticKey, this);
            for (SemanticDeclaration declaration : semanticDeclarations) {
                if (declaration.sourceIdentity.equals(sourceIdentity)
                        && composer.semanticEquals(declaration.semanticKey, semanticKey, this)) {
                    if (declaration.hash != hash) {
                        throw composer.problem("semantic-key-hash-inconsistent", this, null);
                    }
                    throw composer.problem("duplicate-semantic-key", this, null);
                }
            }
            semanticDeclarations.add(new SemanticDeclaration(sourceIdentity, semanticKey, hash));
        }

        /// Finds an unused active or dormant semantic child.
        private @Nullable GroupNode findSemantic(
                String sourceIdentity,
                Object semanticKey,
                GroupKind expectedKind,
                Composer composer
        ) {
            @Nullable GroupNode active = findSemantic(
                    group.activeChildren,
                    sourceIdentity,
                    semanticKey,
                    expectedKind,
                    composer
            );
            return active != null ? active : findSemantic(
                    group.dormantChildren,
                    sourceIdentity,
                    semanticKey,
                    expectedKind,
                    composer
            );
        }

        /// Finds one matching semantic child in a supplied list.
        private @Nullable GroupNode findSemantic(
                List<GroupNode> children,
                String sourceIdentity,
                Object semanticKey,
                GroupKind expectedKind,
                Composer composer
        ) {
            int currentHash = composer.semanticHash(semanticKey, this);
            for (GroupNode child : children) {
                if (used.containsKey(child)
                        || child.kind != expectedKind
                        || !child.sourceIdentity.equals(sourceIdentity)
                        || child.semanticKey == null) {
                    continue;
                }
                if (child.semanticHash != composer.semanticHash(child.semanticKey, this)) {
                    throw composer.problem("semantic-key-mutated", this, null);
                }
                if (composer.semanticEquals(child.semanticKey, semanticKey, this)) {
                    if (child.semanticHash != currentHash) {
                        throw composer.problem("semantic-key-hash-inconsistent", this, null);
                    }
                    return child;
                }
            }
            return null;
        }
    }

    /// Stores one declared semantic identity for duplicate detection.
    ///
    /// @param sourceIdentity the collection source identity
    /// @param semanticKey the application key
    /// @param hash the key hash observed at declaration
    @NotNullByDefault
    private record SemanticDeclaration(String sourceIdentity, Object semanticKey, int hash) {
        /// Validates one semantic declaration.
        private SemanticDeclaration {
            Objects.requireNonNull(sourceIdentity, "sourceIdentity");
            Objects.requireNonNull(semanticKey, "semanticKey");
        }
    }

    /// Stores one positional value and its optional cleanup contract.
    @NotNullByDefault
    private static final class MemorySlot {
        /// The positive slot identity.
        private final long id;

        /// The owning group.
        private final GroupNode owner;

        /// The positional index used for diagnostics.
        private final int position;

        /// The declared application value type.
        private final Class<?> valueType;

        /// The retained non-null value.
        private final Object value;

        /// The erased disposer, or `null` for unmanaged values and local cells.
        private final @Nullable Consumer<Object> disposer;

        /// Whether this slot owns a disposable resource.
        private final boolean resource;

        /// Whether this slot contains a [StructuralLocal].
        private final boolean local;

        /// Whether disposal completed or was attempted.
        private boolean disposed;

        /// Creates one staged slot.
        private MemorySlot(
                long id,
                GroupNode owner,
                int position,
                Class<?> valueType,
                Object value,
                @Nullable Consumer<Object> disposer,
                boolean resource,
                boolean local
        ) {
            this.id = id;
            this.owner = owner;
            this.position = position;
            this.valueType = valueType;
            this.value = value;
            this.disposer = disposer;
            this.resource = resource;
            this.local = local;
        }

        /// Returns the deterministic slot owner path.
        private String ownerPath() {
            return owner.ownerPath() + "#memory[" + position + ']';
        }
    }

    /// Stores one committed structural effect.
    @NotNullByDefault
    private static final class OwnedEffect {
        /// The owning group.
        private final GroupNode owner;

        /// The group-local key.
        private final String key;

        /// The current mount callback.
        private Runnable mount;

        /// The current cleanup callback.
        private Runnable cleanup;

        /// Whether mount completed.
        private boolean mounted;

        /// Whether cleanup was attempted.
        private boolean disposed;

        /// Creates one staged effect.
        private OwnedEffect(GroupNode owner, String key, Runnable mount, Runnable cleanup) {
            this.owner = owner;
            this.key = key;
            this.mount = mount;
            this.cleanup = cleanup;
        }

        /// Returns the deterministic effect owner path.
        private String ownerPath() {
            return owner.ownerPath() + "#effect[" + key + ']';
        }
    }

    /// Stores one next effect declaration and its reusable or newly mounted record.
    @NotNullByDefault
    private static final class EffectDeclaration {
        /// The local effect key.
        private final String key;

        /// The next mount callback.
        private final Runnable mount;

        /// The next cleanup callback.
        private final Runnable cleanup;

        /// The prior committed effect, or `null` for a new key.
        private final @Nullable OwnedEffect previous;

        /// The newly mounted record, or `null` until preparation.
        private @Nullable OwnedEffect created;

        /// Creates one declaration.
        private EffectDeclaration(
                String key,
                Runnable mount,
                Runnable cleanup,
                @Nullable OwnedEffect previous
        ) {
            this.key = key;
            this.mount = mount;
            this.cleanup = cleanup;
            this.previous = previous;
        }
    }

    /// Stores one inherited-value version and its structural readers.
    @NotNullByDefault
    private static final class ProviderCell {
        /// The ambient key identity.
        private final AmbientKey<?> key;

        /// Structural readers using identity semantics.
        private final IdentityHashMap<GroupNode, Boolean> consumers = new IdentityHashMap<>();

        /// The committed non-null value.
        private Object value;

        /// The committed semantic version.
        private long version;

        /// Whether this cell has participated in a commit.
        private boolean committed;

        /// Creates one staged provider cell.
        private ProviderCell(AmbientKey<?> key, Object value) {
            this.key = key;
            this.value = value;
        }
    }

    /// Associates one staged ambient value with its dependency cell.
    ///
    /// @param provider the provider cell
    /// @param value the staged non-null value
    @NotNullByDefault
    private record AmbientValue(ProviderCell provider, Object value) {
        /// Validates one staged ambient value.
        private AmbientValue {
            Objects.requireNonNull(provider, "provider");
            Objects.requireNonNull(value, "value");
        }
    }

    /// Stores one boundary's recovery state independently of transient mounted topology.
    @NotNullByDefault
    private static final class BoundaryState {
        /// The application boundary identity.
        private final ErrorBoundaryKey key;

        /// The current recovery status.
        private ErrorBoundaryStatus status = ErrorBoundaryStatus.HEALTHY;

        /// The currently committed group, or `null` while unmounted after escalation.
        private @Nullable GroupNode group;

        /// The nearest declared parent boundary, or `null`.
        private @Nullable BoundaryState parent;

        /// Creates one healthy recovery record.
        private BoundaryState(ErrorBoundaryKey key) {
            this.key = key;
        }

        /// Returns the best deterministic boundary path available.
        private String ownerPath() {
            return group == null ? "boundary[" + key.diagnosticName() + ']' : group.ownerPath();
        }
    }

    /// Owns every allocation and detached dependency capture in one private attempt.
    @NotNullByDefault
    private final class AttemptContext {
        /// The stable state-domain epoch used by the attempt.
        private final long stateEpoch;

        /// The optional cooperative cancellation flag.
        private final @Nullable StructuralCancellation cancellation;

        /// Pairwise non-overlapping attempt roots.
        private final ArrayList<GroupDraft> rootDrafts = new ArrayList<>();

        /// Every draft published by a successful attempt.
        private final ArrayList<GroupDraft> allDrafts = new ArrayList<>();

        /// Detached observations to commit together.
        private final ArrayList<ReactiveObservation> observations = new ArrayList<>();

        /// Groups allocated only by this attempt.
        private final ArrayList<GroupNode> newGroups = new ArrayList<>();

        /// Memories allocated only by this attempt.
        private final ArrayList<MemorySlot> newMemories = new ArrayList<>();

        /// Effects mounted during commit preparation.
        private final ArrayList<OwnedEffect> mountedEffects = new ArrayList<>();

        /// Boundary keys declared in this attempt.
        private final IdentityHashMap<ErrorBoundaryKey, GroupNode> declaredBoundaries = new IdentityHashMap<>();

        /// Measure keys declared in this attempt.
        private final IdentityHashMap<MeasureMaterializationKey<?>, GroupNode> declaredMeasures =
                new IdentityHashMap<>();

        /// Creates one empty attempt.
        private AttemptContext(long stateEpoch, @Nullable StructuralCancellation cancellation) {
            this.stateEpoch = stateEpoch;
            this.cancellation = cancellation;
        }

        /// Declares one unique boundary key and returns its retained recovery record.
        private BoundaryState declareBoundary(ErrorBoundaryKey key, GroupNode group, Composer composer) {
            @Nullable GroupNode duplicate = declaredBoundaries.put(key, group);
            if (duplicate != null && duplicate != group) {
                throw composer.problem("duplicate-boundary-key", composer.stack.getLast(), null);
            }
            @Nullable BoundaryState state = boundaries.get(key);
            if (state != null && state.group != null && state.group != group) {
                throw composer.problem("duplicate-boundary-key", composer.stack.getLast(), null);
            }
            if (state == null) {
                state = new BoundaryState(key);
                boundaries.put(key, state);
            }
            return state;
        }

        /// Declares one unique current-measure key.
        private void declareMeasure(MeasureMaterializationKey<?> key, GroupNode group, Composer composer) {
            @Nullable GroupNode duplicate = declaredMeasures.put(key, group);
            if (duplicate != null && duplicate != group) {
                throw composer.problem("duplicate-measure-key", composer.stack.getLast(), null);
            }
            @Nullable GroupNode committed = measureGroups.get(key);
            if (committed != null && committed != group) {
                throw composer.problem("duplicate-measure-key", composer.stack.getLast(), null);
            }
        }
    }

    /// Accumulates irreversible cleanup and provider invalidation after logical publication.
    @NotNullByDefault
    private static final class CommitPlan {
        /// Postorder cleanup work preserving child-before-parent ownership.
        private final ArrayList<CleanupTask> cleanupTasks = new ArrayList<>();

        /// Providers whose committed semantic value changed.
        private final IdentityHashMap<ProviderCell, Boolean> changedProviders = new IdentityHashMap<>();

        /// Groups whose draft already consumed a changed provider value.
        private final IdentityHashMap<GroupNode, Boolean> committedGroups = new IdentityHashMap<>();

        /// Creates an empty commit plan.
        private CommitPlan() {
        }
    }

    /// Performs one irreversible post-commit cleanup step while aggregating failure.
    @FunctionalInterface
    @NotNullByDefault
    private interface CleanupTask {
        /// Runs one cleanup step.
        ///
        /// @param failures the mutable aggregate receiving callback failures
        void run(ArrayList<StructuralCleanupFailure> failures);
    }

    /// Orders one failed-attempt cleanup by owner depth and reverse declaration order.
    ///
    /// @param depth owner depth below the root
    /// @param declarationOrder attempt-local creation or mount order
    /// @param task cleanup operation
    @NotNullByDefault
    private record AbortCleanupTask(int depth, int declarationOrder, CleanupTask task) {
        /// Validates one cleanup-order record.
        private AbortCleanupTask {
            if (depth < 0 || declarationOrder < 0) {
                throw new IllegalArgumentException("Cleanup depth and order must be nonnegative");
            }
            Objects.requireNonNull(task, "task");
        }
    }

    /// Summarizes one successful publication.
    ///
    /// @param committedGroups number of published group drafts
    /// @param cleanupFailure aggregated cleanup diagnostic, or `null`
    @NotNullByDefault
    private record CommitOutcome(int committedGroups, @Nullable StructuralFailure cleanupFailure) {
        /// Validates one successful commit outcome.
        private CommitOutcome {
            if (committedGroups < 0) {
                throw new IllegalArgumentException("committedGroups must be nonnegative");
            }
        }
    }

    /// Reports one callback, key, budget, or effect-mount failure inside an attempt.
    @NotNullByDefault
    private static final class AttemptProblem extends RuntimeException {
        /// The serialization identifier.
        private static final long serialVersionUID = 1L;

        /// The stable diagnostic code.
        private final String code;

        /// The callback phase.
        private final StructuralCallbackPhase phase;

        /// The failure owner path.
        private final String ownerPath;

        /// The nearest healthy boundary, or `null` for root containment.
        private final transient @Nullable BoundaryState boundary;

        /// Cleanup failures appended while aborting this problem's draft.
        private final ArrayList<StructuralCleanupFailure> cleanupFailures = new ArrayList<>();

        /// Creates one internal attempt problem.
        private AttemptProblem(
                String code,
                StructuralCallbackPhase phase,
                String ownerPath,
                @Nullable BoundaryState boundary,
                @Nullable Throwable cause
        ) {
            super(code, cause);
            this.code = code;
            this.phase = phase;
            this.ownerPath = ownerPath;
            this.boundary = boundary;
        }
    }

    /// Signals cooperative cancellation without treating it as application failure.
    @NotNullByDefault
    private static final class CancellationSignal extends RuntimeException {
        /// The serialization identifier.
        private static final long serialVersionUID = 1L;

        /// The allocation-free cancellation signal.
        private static final CancellationSignal INSTANCE = new CancellationSignal();

        /// Creates the singleton without an expensive stack trace.
        private CancellationSignal() {
            super(null, null, false, false);
        }
    }
}

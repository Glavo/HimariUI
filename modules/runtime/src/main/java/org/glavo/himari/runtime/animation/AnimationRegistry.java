package org.glavo.himari.runtime.animation;

import org.glavo.himari.platform.api.PlatformEventLoop;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Consumer;

/// Owns model targets, active scalar motion, atomic presentation epochs, and completion outcomes.
///
/// Registry mutation is confined to the borrowed [PlatformEventLoop] owner context. Diagnostics and
/// individual property reads may occur from any thread and synchronize on one publication monitor.
/// Sampling first computes every active property into reusable primitive arrays, then publishes all
/// changes as at most one presentation epoch. It never writes application state or invokes an
/// application completion callback.
///
/// The registry does not own or close its event loop. A future `UiCommitTransaction` may feed its
/// staged property targets into [#commit(AnimationTransaction, Consumer)] without changing these
/// transaction, sampling, or interruption semantics.
@NotNullByDefault
public final class AnimationRegistry implements AutoCloseable {
    /// The default maximum active-or-undrained exactly-once completion reservations.
    public static final int DEFAULT_COMPLETION_CAPACITY = 4_096;

    /// The shared immutable empty scalar staging array used after closure.
    private static final double @Unmodifiable [] EMPTY_SCALARS = new double[0];

    /// The shared immutable empty flag staging array used after closure.
    private static final boolean @Unmodifiable [] EMPTY_FLAGS = new boolean[0];

    /// The owner context and monotonic clock borrowed by this registry.
    private final PlatformEventLoop eventLoop;

    /// The monitor protecting every property, publication, group, event, and lifecycle field.
    private final Object lock = new Object();

    /// Live properties by stable slot, with `null` entries for closed properties.
    private final ArrayList<@Nullable AnimatedScalar> properties = new ArrayList<>();

    /// Closed property slots available for deterministic first-closed-first-reused allocation.
    private final ArrayDeque<Integer> freePropertySlots = new ArrayDeque<>();

    /// Active completion groups keyed by transaction identity.
    private final HashMap<Long, CompletionGroup> completionGroups = new HashMap<>();

    /// Exactly-once terminal completion events waiting for the owner to drain them.
    private final ArrayDeque<AnimationCompletionEvent> completionEvents = new ArrayDeque<>();

    /// Reusable scalar values staged for one atomic sampling attempt.
    private double[] stagedValues = EMPTY_SCALARS;

    /// Reusable scalar velocities staged for one atomic sampling attempt.
    private double[] stagedVelocities = EMPTY_SCALARS;

    /// Reusable flags identifying active properties staged in the current attempt.
    private boolean[] stagedPresent = EMPTY_FLAGS;

    /// Reusable flags identifying semantic presentation-value changes.
    private boolean[] stagedValueChanged = EMPTY_FLAGS;

    /// Reusable flags identifying exact velocity changes.
    private boolean[] stagedVelocityChanged = EMPTY_FLAGS;

    /// Reusable flags identifying timelines that reached their terminal condition.
    private boolean[] stagedCompleted = EMPTY_FLAGS;

    /// The single mutable motion result reused for every scalar sample.
    private final MutableMotionSample motionSample = new MutableMotionSample();

    /// The positive completion reservation capacity.
    private final int completionCapacity;

    /// Whether an application commit callback is currently staging targets.
    private boolean commitInProgress;

    /// Whether the registry permanently stopped accepting properties, commits, and samples.
    private boolean closed;

    /// The latest accepted transaction identity.
    private long lastTransactionId;

    /// The next positive property identity; `Long.MAX_VALUE` denotes exhaustion.
    private long nextPropertyId = 1L;

    /// The latest atomically published presentation epoch.
    private long presentationEpoch;

    /// The latest successful sample or commit timestamp, or `-1` before either.
    private long lastTimestampNanos = -1L;

    /// The number of active scalar timelines.
    private int activeAnimationCount;

    /// Active groups plus queued terminal events whose capacity is reserved.
    private int reservedCompletionSlots;

    /// The phase impact published by the latest presentation epoch.
    private AnimationPhaseImpact lastPhaseImpact = AnimationPhaseImpact.NONE;

    /// Whether the currently staged active sample changes presentation state.
    private boolean stagedStateChanged;

    /// The union phase mask of value changes in the currently staged active sample.
    private int stagedPhaseMask;

    /// Creates an animation registry with the default completion capacity.
    ///
    /// @param eventLoop the event loop owned by the calling thread
    /// @throws IllegalStateException if the caller does not own the loop or the loop is closed
    public AnimationRegistry(PlatformEventLoop eventLoop) {
        this(eventLoop, DEFAULT_COMPLETION_CAPACITY);
    }

    /// Creates an animation registry with an explicit completion reservation capacity.
    ///
    /// @param eventLoop the event loop owned by the calling thread
    /// @param completionCapacity the positive maximum active-or-undrained completion groups
    /// @throws IllegalArgumentException if `completionCapacity` is not positive
    /// @throws IllegalStateException if the caller does not own the loop or the loop is closed
    public AnimationRegistry(PlatformEventLoop eventLoop, int completionCapacity) {
        this.eventLoop = Objects.requireNonNull(eventLoop, "eventLoop");
        if (completionCapacity <= 0) {
            throw new IllegalArgumentException("completionCapacity must be positive");
        }
        eventLoop.checkOwnerThread();
        if (eventLoop.isClosed()) {
            throw new IllegalStateException("Cannot create an animation registry for a closed event loop");
        }
        this.completionCapacity = completionCapacity;
    }

    /// Returns the borrowed owner event loop and clock source.
    ///
    /// @return the event loop
    public PlatformEventLoop eventLoop() {
        return eventLoop;
    }

    /// Creates a live scalar property whose model and presentation values initially agree.
    ///
    /// @param debugName the nonempty stable diagnostic name
    /// @param initialValue the finite initial scalar value
    /// @param adapter the allocation-free normalization and equality adapter
    /// @param phaseImpact the phases affected by presentation changes
    /// @return the live registry-owned property
    /// @throws IllegalArgumentException if the name or value is invalid
    /// @throws IllegalStateException if called outside the owner context, while staging a commit,
    /// after registry closure, or after property identities are exhausted
    public AnimatedScalar createScalar(
            String debugName,
            double initialValue,
            ScalarAnimationAdapter adapter,
            AnimationPhaseImpact phaseImpact
    ) {
        Objects.requireNonNull(debugName, "debugName");
        Objects.requireNonNull(adapter, "adapter");
        Objects.requireNonNull(phaseImpact, "phaseImpact");
        if (debugName.isBlank()) {
            throw new IllegalArgumentException("debugName must not be blank");
        }
        double normalized = adapter.normalize(initialValue);
        eventLoop.checkOwnerThread();
        synchronized (lock) {
            checkOpenUnderLock();
            checkNoCommitCallbackUnderLock("Property creation");
            if (nextPropertyId == Long.MAX_VALUE) {
                throw new IllegalStateException("Animated scalar property identities are exhausted");
            }
            int slot;
            if (freePropertySlots.isEmpty()) {
                slot = properties.size();
                ensureStagingCapacity(slot + 1);
            } else {
                slot = freePropertySlots.removeFirst();
            }
            AnimatedScalar property = new AnimatedScalar(
                    this,
                    slot,
                    nextPropertyId,
                    debugName,
                    adapter,
                    phaseImpact,
                    normalized
            );
            nextPropertyId++;
            if (slot == properties.size()) {
                properties.add(property);
            } else {
                properties.set(slot, property);
            }
            return property;
        }
    }

    /// Stages and atomically commits model targets with explicit immutable animation metadata.
    ///
    /// The callback may call only methods on its [AnimationCommit] argument. If it throws, no model
    /// or presentation value changes, its transaction emits one [AnimationCompletionOutcome#FAILED]
    /// event, and the original failure is rethrown. Transaction identities must increase strictly so
    /// trace and replacement order do not depend on allocation or thread identity.
    ///
    /// @param transaction the immutable transaction metadata
    /// @param action the bounded synchronous target-staging callback
    /// @return the successful atomic commit result
    /// @throws RejectedExecutionException if no exactly-once completion slot is available
    /// @throws IllegalArgumentException if transaction order, property ownership, target values,
    /// velocity handoff, or timeline range is invalid
    /// @throws IllegalStateException if called outside the owner context, reentrantly, after registry
    /// or event-loop closure, after the clock moves backwards, or after presentation epochs are
    /// exhausted
    public AnimationCommitResult commit(
            AnimationTransaction transaction,
            Consumer<AnimationCommit> action
    ) {
        Objects.requireNonNull(transaction, "transaction");
        Objects.requireNonNull(action, "action");
        eventLoop.checkOwnerThread();
        long timestampNanos = eventLoop.clock().nowNanos();
        AnimationCommit commit;
        synchronized (lock) {
            checkOpenUnderLock();
            validateTimestampUnderLock(timestampNanos);
            if (commitInProgress) {
                throw new IllegalStateException("Animation commit staging cannot be reentered");
            }
            if (transaction.transactionId() <= lastTransactionId) {
                throw new IllegalArgumentException("Animation transaction identifiers must increase strictly");
            }
            if (reservedCompletionSlots == completionCapacity) {
                throw new RejectedExecutionException("Animation completion capacity is full");
            }
            commitInProgress = true;
            lastTransactionId = transaction.transactionId();
            reservedCompletionSlots++;
            commit = new AnimationCommit(this, transaction);
        }

        try {
            action.accept(commit);
            commit.finish();
        } catch (RuntimeException | Error failure) {
            commit.abort();
            synchronized (lock) {
                commitInProgress = false;
                emitCompletionUnderLock(
                        transaction.transactionId(),
                        timestampNanos,
                        AnimationCompletionOutcome.FAILED,
                        0
                );
            }
            throw failure;
        }

        synchronized (lock) {
            try {
                stageActiveAnimationsUnderLock(timestampNanos);
                int changedTargetCount = validateStagedCommitUnderLock(transaction, commit, timestampNanos);
                boolean willChangePresentationState = stagedStateChanged || changedTargetCount != 0;
                if (willChangePresentationState && presentationEpoch == Long.MAX_VALUE) {
                    throw new IllegalStateException("Animation presentation epoch is exhausted");
                }

                applyStagedAnimationsUnderLock(timestampNanos);
                CompletionGroup newGroup = new CompletionGroup(
                        transaction.transactionId(),
                        changedTargetCount
                );
                int phaseMask = stagedPhaseMask;
                phaseMask |= applyStagedTargetsUnderLock(transaction, commit, newGroup, timestampNanos);

                @Nullable AnimationCompletionOutcome immediateOutcome = null;
                if (newGroup.remainingCount == 0) {
                    immediateOutcome = AnimationCompletionOutcome.SKIPPED;
                    emitCompletionUnderLock(
                            transaction.transactionId(),
                            timestampNanos,
                            immediateOutcome,
                            changedTargetCount
                    );
                } else {
                    completionGroups.put(transaction.transactionId(), newGroup);
                }

                if (willChangePresentationState) {
                    presentationEpoch++;
                    lastPhaseImpact = AnimationPhaseImpact.canonical(phaseMask);
                }
                lastTimestampNanos = timestampNanos;
                return new AnimationCommitResult(
                        transaction.transactionId(),
                        timestampNanos,
                        changedTargetCount,
                        activeAnimationCount,
                        presentationEpoch,
                        AnimationPhaseImpact.canonical(phaseMask),
                        immediateOutcome
                );
            } catch (RuntimeException | Error failure) {
                emitCompletionUnderLock(
                        transaction.transactionId(),
                        timestampNanos,
                        AnimationCompletionOutcome.FAILED,
                        0
                );
                throw failure;
            } finally {
                commitInProgress = false;
            }
        }
    }

    /// Samples every active timeline at the event loop clock's current timestamp.
    ///
    /// Sampling is allocation-free until a timeline completes and must emit its reserved completion
    /// event. It advances directly to the current timestamp after a skipped or delayed frame and
    /// publishes all changed properties as at most one presentation epoch.
    ///
    /// @return whether a new presentation epoch was published
    /// @throws IllegalStateException if called outside the owner context, during commit staging,
    /// after close, after the clock moves backwards, after presentation epochs are exhausted, or if
    /// a motion produces a non-finite sample
    public boolean sample() {
        eventLoop.checkOwnerThread();
        long timestampNanos = eventLoop.clock().nowNanos();
        synchronized (lock) {
            checkOpenUnderLock();
            checkNoCommitCallbackUnderLock("Animation sampling");
            validateTimestampUnderLock(timestampNanos);
            stageActiveAnimationsUnderLock(timestampNanos);
            if (stagedStateChanged && presentationEpoch == Long.MAX_VALUE) {
                throw new IllegalStateException("Animation presentation epoch is exhausted");
            }
            applyStagedAnimationsUnderLock(timestampNanos);
            lastTimestampNanos = timestampNanos;
            if (!stagedStateChanged) {
                return false;
            }
            presentationEpoch++;
            lastPhaseImpact = AnimationPhaseImpact.canonical(stagedPhaseMask);
            return true;
        }
    }

    /// Returns the current presentation epoch.
    ///
    /// @return the nonnegative epoch
    public long presentationEpoch() {
        synchronized (lock) {
            return presentationEpoch;
        }
    }

    /// Returns whether at least one motion timeline requires a later frame or delayed wakeup.
    ///
    /// @return whether active motion remains
    public boolean hasActiveAnimations() {
        synchronized (lock) {
            return activeAnimationCount != 0;
        }
    }

    /// Returns the latest phase impact published by a presentation epoch.
    ///
    /// @return the immutable impact, or [AnimationPhaseImpact#NONE] before any changed epoch
    public AnimationPhaseImpact lastPhaseImpact() {
        synchronized (lock) {
            return lastPhaseImpact;
        }
    }

    /// Returns an immutable snapshot of queued completion events without removing them.
    ///
    /// @return events in terminal order
    public @Unmodifiable List<AnimationCompletionEvent> completionEvents() {
        synchronized (lock) {
            return List.copyOf(completionEvents);
        }
    }

    /// Removes and returns every terminal completion event in deterministic order.
    ///
    /// Draining releases their reserved completion capacity. It remains available after registry
    /// closure so cancellation outcomes cannot be lost.
    ///
    /// @return the drained immutable event list
    /// @throws IllegalStateException if called outside the owner context or during commit staging
    public @Unmodifiable List<AnimationCompletionEvent> drainCompletionEvents() {
        eventLoop.checkOwnerThread();
        synchronized (lock) {
            checkNoCommitCallbackUnderLock("Completion draining");
            if (completionEvents.isEmpty()) {
                return List.of();
            }
            List<AnimationCompletionEvent> result = List.copyOf(completionEvents);
            completionEvents.clear();
            reservedCompletionSlots -= result.size();
            return result;
        }
    }

    /// Captures all live properties at one atomic presentation publication boundary.
    ///
    /// @return the immutable registry snapshot
    public AnimationRegistrySnapshot snapshot() {
        synchronized (lock) {
            ArrayList<AnimatedScalarSnapshot> scalarSnapshots = new ArrayList<>();
            for (@Nullable AnimatedScalar property : properties) {
                if (property != null) {
                    scalarSnapshots.add(snapshotUnderLock(property));
                }
            }
            scalarSnapshots.sort(Comparator.comparingLong(AnimatedScalarSnapshot::propertyId));
            return new AnimationRegistrySnapshot(
                    closed,
                    presentationEpoch,
                    lastTimestampNanos,
                    activeAnimationCount,
                    nextWakeupUnderLock(),
                    completionEvents.size(),
                    reservedCompletionSlots,
                    lastPhaseImpact,
                    scalarSnapshots
            );
        }
    }

    /// Cancels active motion, closes every property, and stops accepting work.
    ///
    /// Closure is idempotent and owner-context confined. Active groups emit exactly one cancelled or
    /// previously accumulated replacement outcome; queued events remain drainable. The borrowed
    /// event loop is not closed.
    ///
    /// @throws IllegalStateException if called outside the owner context, during commit staging,
    /// after the clock moves backwards, or after presentation epochs are exhausted
    @Override
    public void close() {
        eventLoop.checkOwnerThread();
        long timestampNanos = eventLoop.clock().nowNanos();
        synchronized (lock) {
            if (closed) {
                return;
            }
            checkNoCommitCallbackUnderLock("Animation registry closure");
            validateTimestampUnderLock(timestampNanos);
            boolean hasLiveProperty = false;
            for (@Nullable AnimatedScalar property : properties) {
                if (property != null) {
                    hasLiveProperty = true;
                    break;
                }
            }
            if (hasLiveProperty && presentationEpoch == Long.MAX_VALUE) {
                throw new IllegalStateException("Animation presentation epoch is exhausted");
            }
            for (int slot = 0; slot < properties.size(); slot++) {
                @Nullable AnimatedScalar property = properties.get(slot);
                if (property == null) {
                    continue;
                }
                if (property.effectiveMotion != null) {
                    finishGroupMemberUnderLock(
                            property.transactionId,
                            AnimationCompletionOutcome.CANCELLED,
                            timestampNanos
                    );
                    activeAnimationCount--;
                }
                property.effectiveMotion = null;
                property.transactionId = 0L;
                property.velocity = 0.0;
                property.closed = true;
                properties.set(slot, null);
            }
            if (!completionGroups.isEmpty() || activeAnimationCount != 0) {
                throw new IllegalStateException("Animation completion ownership became unbalanced");
            }
            if (hasLiveProperty) {
                presentationEpoch++;
                lastPhaseImpact = AnimationPhaseImpact.NONE;
            }
            properties.clear();
            freePropertySlots.clear();
            stagedValues = EMPTY_SCALARS;
            stagedVelocities = EMPTY_SCALARS;
            stagedPresent = EMPTY_FLAGS;
            stagedValueChanged = EMPTY_FLAGS;
            stagedVelocityChanged = EMPTY_FLAGS;
            stagedCompleted = EMPTY_FLAGS;
            lastTimestampNanos = timestampNanos;
            closed = true;
        }
    }

    /// Verifies that a callback-scoped commit may stage one property.
    ///
    /// @param property the candidate property
    void checkStagingProperty(AnimatedScalar property) {
        eventLoop.checkOwnerThread();
        synchronized (lock) {
            checkOpenUnderLock();
            if (!commitInProgress) {
                throw new IllegalStateException("No animation commit callback is active");
            }
            requireOwnedPropertyUnderLock(property);
            if (property.closed) {
                throw new IllegalStateException("Animated scalar property is closed");
            }
        }
    }

    /// Returns one property's model target under the publication monitor.
    ///
    /// @param property the property
    /// @return the model target
    double modelTarget(AnimatedScalar property) {
        synchronized (lock) {
            requireKnownPropertyUnderLock(property);
            return property.modelTarget;
        }
    }

    /// Returns one property's presentation value under the publication monitor.
    ///
    /// @param property the property
    /// @return the presentation value
    double presentationValue(AnimatedScalar property) {
        synchronized (lock) {
            requireKnownPropertyUnderLock(property);
            return property.presentationValue;
        }
    }

    /// Returns one property's velocity under the publication monitor.
    ///
    /// @param property the property
    /// @return the scalar-per-second velocity
    double velocity(AnimatedScalar property) {
        synchronized (lock) {
            requireKnownPropertyUnderLock(property);
            return property.velocity;
        }
    }

    /// Returns whether one property has active motion.
    ///
    /// @param property the property
    /// @return whether motion is active
    boolean isActive(AnimatedScalar property) {
        synchronized (lock) {
            requireKnownPropertyUnderLock(property);
            return property.effectiveMotion != null;
        }
    }

    /// Returns whether one property is closed.
    ///
    /// @param property the property
    /// @return whether the property is closed
    boolean isPropertyClosed(AnimatedScalar property) {
        synchronized (lock) {
            requireKnownPropertyUnderLock(property);
            return property.closed;
        }
    }

    /// Captures one known property under the publication monitor.
    ///
    /// @param property the property
    /// @return the property snapshot
    AnimatedScalarSnapshot snapshot(AnimatedScalar property) {
        synchronized (lock) {
            requireKnownPropertyUnderLock(property);
            return snapshotUnderLock(property);
        }
    }

    /// Closes one property and cancels its active completion member.
    ///
    /// @param property the property
    void closeProperty(AnimatedScalar property) {
        eventLoop.checkOwnerThread();
        long timestampNanos = eventLoop.clock().nowNanos();
        synchronized (lock) {
            requireKnownPropertyUnderLock(property);
            if (property.closed) {
                return;
            }
            checkOpenUnderLock();
            checkNoCommitCallbackUnderLock("Animated scalar closure");
            validateTimestampUnderLock(timestampNanos);
            if (presentationEpoch == Long.MAX_VALUE) {
                throw new IllegalStateException("Animation presentation epoch is exhausted");
            }
            if (property.effectiveMotion != null) {
                finishGroupMemberUnderLock(
                        property.transactionId,
                        AnimationCompletionOutcome.CANCELLED,
                        timestampNanos
                );
                activeAnimationCount--;
            }
            property.effectiveMotion = null;
            property.transactionId = 0L;
            property.velocity = 0.0;
            property.closed = true;
            properties.set(property.slot, null);
            freePropertySlots.addLast(property.slot);
            presentationEpoch++;
            lastTimestampNanos = timestampNanos;
            lastPhaseImpact = AnimationPhaseImpact.NONE;
        }
    }

    /// Validates staged targets and returns their semantic model-change count.
    ///
    /// @param transaction the transaction
    /// @param commit the closed staged commit
    /// @param timestampNanos the commit timestamp
    /// @return the semantic target-change count
    private int validateStagedCommitUnderLock(
            AnimationTransaction transaction,
            AnimationCommit commit,
            long timestampNanos
    ) {
        int changedTargetCount = 0;
        for (AnimatedScalar property : commit.orderedProperties()) {
            requireOwnedPropertyUnderLock(property);
            if (property.closed) {
                throw new IllegalStateException("Animated scalar property closed during commit staging");
            }
            AnimationCommit.StagedTarget stagedTarget = commit.targetFor(property);
            if (property.adapter.equivalent(property.modelTarget, stagedTarget.target())) {
                continue;
            }
            if (property.replacementGeneration == Long.MAX_VALUE) {
                throw new IllegalStateException("Animated scalar replacement generation is exhausted");
            }
            changedTargetCount++;
            MotionSpec effectiveMotion = transaction.effectiveMotion();
            boolean immediate = effectiveMotion.isImmediate()
                    || transaction.replacementPolicy() == AnimationReplacementPolicy.SNAP;
            if (immediate) {
                continue;
            }
            if (stagedTarget.hasInitialVelocity()
                    && transaction.replacementPolicy() != AnimationReplacementPolicy.RESTART
                    && !effectiveMotion.supportsVelocityRetargeting()) {
                throw new IllegalArgumentException(
                        "Effective motion does not accept explicit initial velocity"
                );
            }
            double currentValue = stagedPresent[property.slot]
                    ? stagedValues[property.slot]
                    : property.presentationValue;
            double startValue = transaction.replacementPolicy() == AnimationReplacementPolicy.RESTART
                    ? property.modelTarget
                    : currentValue;
            double displacement = stagedTarget.target() - startValue;
            if (!Double.isFinite(displacement)) {
                throw new IllegalArgumentException("Animated scalar displacement must remain finite");
            }
            validateTimelineRange(timestampNanos, effectiveMotion);
        }
        return changedTargetCount;
    }

    /// Applies staged target changes and returns their immediate phase-impact mask.
    ///
    /// @param transaction the transaction
    /// @param commit the closed staged commit
    /// @param newGroup the completion group under construction
    /// @param timestampNanos the commit timestamp
    /// @return the phase-impact mask
    private int applyStagedTargetsUnderLock(
            AnimationTransaction transaction,
            AnimationCommit commit,
            CompletionGroup newGroup,
            long timestampNanos
    ) {
        int phaseMask = 0;
        for (AnimatedScalar property : commit.orderedProperties()) {
            AnimationCommit.StagedTarget stagedTarget = commit.targetFor(property);
            double target = stagedTarget.target();
            if (property.adapter.equivalent(property.modelTarget, target)) {
                continue;
            }

            double previousModelTarget = property.modelTarget;
            @Nullable MotionSpec previousMotion = property.effectiveMotion;
            double currentPresentation = property.presentationValue;
            double currentVelocity = property.velocity;
            if (previousMotion != null) {
                finishGroupMemberUnderLock(
                        property.transactionId,
                        AnimationCompletionOutcome.REPLACED,
                        timestampNanos
                );
                activeAnimationCount--;
                property.effectiveMotion = null;
                property.transactionId = 0L;
            }

            property.modelTarget = target;
            property.replacementGeneration++;
            MotionSpec effectiveMotion = transaction.effectiveMotion();
            boolean snap = effectiveMotion.isImmediate()
                    || transaction.replacementPolicy() == AnimationReplacementPolicy.SNAP;
            if (snap) {
                if (!sameBits(property.presentationValue, target)) {
                    property.presentationValue = target;
                    phaseMask |= property.phaseImpact.mask();
                }
                property.velocity = 0.0;
                continue;
            }

            double startValue;
            double initialVelocity;
            switch (transaction.replacementPolicy()) {
                case RESTART -> {
                    startValue = previousModelTarget;
                    initialVelocity = 0.0;
                }
                case PRESERVE_VALUE -> {
                    startValue = currentPresentation;
                    initialVelocity = stagedTarget.hasInitialVelocity()
                            ? stagedTarget.initialVelocity()
                            : 0.0;
                }
                case PRESERVE_VELOCITY -> {
                    startValue = currentPresentation;
                    if (stagedTarget.hasInitialVelocity()) {
                        initialVelocity = stagedTarget.initialVelocity();
                    } else if (previousMotion != null
                            && previousMotion.supportsVelocityRetargeting()
                            && effectiveMotion.supportsVelocityRetargeting()) {
                        initialVelocity = currentVelocity;
                    } else {
                        initialVelocity = 0.0;
                    }
                }
                case SNAP -> throw new IllegalStateException("Active snap replacement was not applied immediately");
                default -> throw new IllegalStateException("Unknown animation replacement policy");
            }

            if (!property.adapter.equivalent(property.presentationValue, startValue)) {
                property.presentationValue = startValue;
                phaseMask |= property.phaseImpact.mask();
            }
            property.velocity = initialVelocity;
            if (property.adapter.equivalent(startValue, target)) {
                if (!sameBits(property.presentationValue, target)) {
                    property.presentationValue = target;
                    phaseMask |= property.phaseImpact.mask();
                }
                property.velocity = 0.0;
                continue;
            }

            property.startTimestampNanos = timestampNanos;
            property.startValue = startValue;
            property.initialVelocity = initialVelocity;
            property.effectiveMotion = effectiveMotion;
            property.transactionId = transaction.transactionId();
            activeAnimationCount++;
            newGroup.remainingCount++;
        }
        return phaseMask;
    }

    /// Computes all active properties into reusable arrays without publishing partial results.
    ///
    /// @param timestampNanos the monotonic sample timestamp
    private void stageActiveAnimationsUnderLock(long timestampNanos) {
        validateTimestampUnderLock(timestampNanos);
        stagedStateChanged = false;
        stagedPhaseMask = 0;
        for (int slot = 0; slot < properties.size(); slot++) {
            stagedPresent[slot] = false;
            stagedValueChanged[slot] = false;
            stagedVelocityChanged[slot] = false;
            stagedCompleted[slot] = false;
            @Nullable AnimatedScalar property = properties.get(slot);
            if (property == null || property.effectiveMotion == null) {
                continue;
            }

            sampleMotion(property, timestampNanos, motionSample);
            double normalizedValue = property.adapter.normalize(motionSample.value);
            double normalizedVelocity = property.adapter.normalizeVelocity(motionSample.velocity);
            if (!sameBits(normalizedValue, motionSample.value)) {
                normalizedVelocity = 0.0;
            }
            boolean valueChanged = motionSample.completed
                    ? !sameBits(property.presentationValue, normalizedValue)
                    : !property.adapter.equivalent(property.presentationValue, normalizedValue);
            boolean velocityChanged = !sameBits(property.velocity, normalizedVelocity);

            stagedPresent[slot] = true;
            stagedValues[slot] = normalizedValue;
            stagedVelocities[slot] = normalizedVelocity;
            stagedValueChanged[slot] = valueChanged;
            stagedVelocityChanged[slot] = velocityChanged;
            stagedCompleted[slot] = motionSample.completed;
            if (valueChanged) {
                stagedPhaseMask |= property.phaseImpact.mask();
            }
            if (valueChanged || velocityChanged || motionSample.completed) {
                stagedStateChanged = true;
            }
        }
    }

    /// Publishes a successfully staged active-animation sample.
    ///
    /// @param timestampNanos the sample timestamp used for completion events
    private void applyStagedAnimationsUnderLock(long timestampNanos) {
        for (int slot = 0; slot < properties.size(); slot++) {
            if (!stagedPresent[slot]) {
                continue;
            }
            AnimatedScalar property = Objects.requireNonNull(properties.get(slot), "property");
            if (stagedValueChanged[slot]) {
                property.presentationValue = stagedValues[slot];
            }
            if (stagedVelocityChanged[slot]) {
                property.velocity = stagedVelocities[slot];
            }
            if (stagedCompleted[slot]) {
                finishGroupMemberUnderLock(
                        property.transactionId,
                        AnimationCompletionOutcome.COMPLETED,
                        timestampNanos
                );
                property.effectiveMotion = null;
                property.transactionId = 0L;
                property.presentationValue = property.modelTarget;
                property.velocity = 0.0;
                activeAnimationCount--;
            }
        }
    }

    /// Samples one framework-defined motion without allocation.
    ///
    /// @param property the active property
    /// @param timestampNanos the sample timestamp
    /// @param output the reusable output holder
    private static void sampleMotion(
            AnimatedScalar property,
            long timestampNanos,
            MutableMotionSample output
    ) {
        MotionSpec motion = Objects.requireNonNull(property.effectiveMotion, "effectiveMotion");
        if (motion instanceof TweenSpec tween) {
            sampleTween(property, tween, timestampNanos, output);
        } else if (motion instanceof SpringSpec spring) {
            sampleSpring(property, spring, timestampNanos, output);
        } else {
            throw new IllegalStateException("An immediate motion must not remain active");
        }
        if (!Double.isFinite(output.value) || !Double.isFinite(output.velocity)) {
            throw new IllegalStateException("Animation sampling produced a non-finite scalar");
        }
    }

    /// Samples one fixed-duration tween.
    ///
    /// @param property the active property
    /// @param tween the tween specification
    /// @param timestampNanos the sample timestamp
    /// @param output the reusable output holder
    private static void sampleTween(
            AnimatedScalar property,
            TweenSpec tween,
            long timestampNanos,
            MutableMotionSample output
    ) {
        long activeStart = Math.addExact(property.startTimestampNanos, tween.delayNanos());
        long activeEnd = Math.addExact(activeStart, tween.durationNanos());
        if (timestampNanos < activeStart) {
            output.set(property.startValue, 0.0, false);
            return;
        }
        if (timestampNanos >= activeEnd) {
            output.set(property.modelTarget, 0.0, true);
            return;
        }
        long elapsedNanos = timestampNanos - activeStart;
        double progress = (double) elapsedNanos / (double) tween.durationNanos();
        double curveValue = tween.curve().value(progress);
        double displacement = property.modelTarget - property.startValue;
        double value = property.startValue + displacement * curveValue;
        double velocity = displacement * tween.curve().slope(progress)
                * 1_000_000_000.0 / (double) tween.durationNanos();
        output.set(value, velocity, false);
    }

    /// Samples one analytic underdamped, critically damped, or overdamped spring.
    ///
    /// @param property the active property
    /// @param spring the spring specification
    /// @param timestampNanos the sample timestamp
    /// @param output the reusable output holder
    private static void sampleSpring(
            AnimatedScalar property,
            SpringSpec spring,
            long timestampNanos,
            MutableMotionSample output
    ) {
        long elapsedNanos = timestampNanos - property.startTimestampNanos;
        if (elapsedNanos >= spring.maximumDurationNanos()) {
            output.set(property.modelTarget, 0.0, true);
            return;
        }

        double elapsedSeconds = (double) elapsedNanos / 1_000_000_000.0;
        double displacement = property.startValue - property.modelTarget;
        double initialVelocity = property.initialVelocity;
        double naturalFrequency = StrictMath.sqrt(spring.stiffness() / spring.mass());
        double dampingRatio = spring.damping()
                / (2.0 * StrictMath.sqrt(spring.stiffness() * spring.mass()));
        double valueDisplacement;
        double velocity;

        if (dampingRatio < 1.0 - 1.0e-9) {
            double dampedFrequency = naturalFrequency
                    * StrictMath.sqrt(1.0 - dampingRatio * dampingRatio);
            double first = displacement;
            double second = (initialVelocity + dampingRatio * naturalFrequency * displacement)
                    / dampedFrequency;
            double exponential = StrictMath.exp(-dampingRatio * naturalFrequency * elapsedSeconds);
            double cosine = StrictMath.cos(dampedFrequency * elapsedSeconds);
            double sine = StrictMath.sin(dampedFrequency * elapsedSeconds);
            double oscillation = first * cosine + second * sine;
            valueDisplacement = exponential * oscillation;
            velocity = exponential * (
                    -dampingRatio * naturalFrequency * oscillation
                            - first * dampedFrequency * sine
                            + second * dampedFrequency * cosine
            );
        } else if (dampingRatio <= 1.0 + 1.0e-9) {
            double second = initialVelocity + naturalFrequency * displacement;
            double exponential = StrictMath.exp(-naturalFrequency * elapsedSeconds);
            valueDisplacement = (displacement + second * elapsedSeconds) * exponential;
            velocity = (second - naturalFrequency * (displacement + second * elapsedSeconds))
                    * exponential;
        } else {
            double root = StrictMath.sqrt(dampingRatio * dampingRatio - 1.0);
            double firstRate = -naturalFrequency * (dampingRatio - root);
            double secondRate = -naturalFrequency * (dampingRatio + root);
            double first = (initialVelocity - secondRate * displacement) / (firstRate - secondRate);
            double second = displacement - first;
            double firstTerm = first * StrictMath.exp(firstRate * elapsedSeconds);
            double secondTerm = second * StrictMath.exp(secondRate * elapsedSeconds);
            valueDisplacement = firstTerm + secondTerm;
            velocity = firstRate * firstTerm + secondRate * secondTerm;
        }

        boolean settled = StrictMath.abs(valueDisplacement) <= spring.displacementThreshold()
                && StrictMath.abs(velocity) <= spring.velocityThreshold();
        if (settled) {
            output.set(property.modelTarget, 0.0, true);
        } else {
            output.set(property.modelTarget + valueDisplacement, velocity, false);
        }
    }

    /// Completes one active property membership and emits its group event when all members finish.
    ///
    /// @param transactionId the owning group
    /// @param outcome the member outcome
    /// @param timestampNanos the terminal timestamp
    private void finishGroupMemberUnderLock(
            long transactionId,
            AnimationCompletionOutcome outcome,
            long timestampNanos
    ) {
        CompletionGroup group = completionGroups.get(transactionId);
        if (group == null) {
            throw new IllegalStateException("Active animation has no completion group");
        }
        group.record(outcome);
        group.remainingCount--;
        if (group.remainingCount < 0) {
            throw new IllegalStateException("Animation completion group finished too many members");
        }
        if (group.remainingCount == 0) {
            completionGroups.remove(transactionId);
            emitCompletionUnderLock(
                    transactionId,
                    timestampNanos,
                    group.outcome,
                    group.targetCount
            );
        }
    }

    /// Appends one terminal event whose capacity was reserved when its transaction was accepted.
    ///
    /// @param transactionId the transaction identity
    /// @param timestampNanos the terminal timestamp
    /// @param outcome the terminal outcome
    /// @param targetCount the semantic target count
    private void emitCompletionUnderLock(
            long transactionId,
            long timestampNanos,
            AnimationCompletionOutcome outcome,
            int targetCount
    ) {
        completionEvents.addLast(new AnimationCompletionEvent(
                transactionId,
                timestampNanos,
                outcome,
                targetCount
        ));
    }

    /// Returns one immutable property snapshot while holding [#lock].
    ///
    /// @param property the known property
    /// @return the snapshot
    private static AnimatedScalarSnapshot snapshotUnderLock(AnimatedScalar property) {
        return new AnimatedScalarSnapshot(
                property.propertyId,
                property.debugName,
                property.closed,
                property.modelTarget,
                property.presentationValue,
                property.velocity,
                property.effectiveMotion != null,
                property.transactionId,
                property.replacementGeneration,
                property.phaseImpact,
                property.effectiveMotion
        );
    }

    /// Returns the earliest active wakeup or ready timestamp while holding [#lock].
    ///
    /// @return the nonnegative wakeup, or `-1` when idle
    private long nextWakeupUnderLock() {
        if (activeAnimationCount == 0) {
            return -1L;
        }
        long result = Long.MAX_VALUE;
        for (@Nullable AnimatedScalar property : properties) {
            if (property == null || property.effectiveMotion == null) {
                continue;
            }
            long candidate = lastTimestampNanos;
            if (property.effectiveMotion instanceof TweenSpec tween) {
                long activeStart = Math.addExact(property.startTimestampNanos, tween.delayNanos());
                if (lastTimestampNanos < activeStart) {
                    candidate = activeStart;
                }
            }
            result = Math.min(result, candidate);
        }
        if (result == Long.MAX_VALUE) {
            throw new IllegalStateException("Active animation count has no live timeline");
        }
        return result;
    }

    /// Ensures all reusable staging arrays can address a property count.
    ///
    /// @param required the required element count
    private void ensureStagingCapacity(int required) {
        if (stagedValues.length >= required) {
            return;
        }
        int capacity = Math.max(8, stagedValues.length);
        while (capacity < required) {
            capacity = Math.multiplyExact(capacity, 2);
        }
        stagedValues = Arrays.copyOf(stagedValues, capacity);
        stagedVelocities = Arrays.copyOf(stagedVelocities, capacity);
        stagedPresent = Arrays.copyOf(stagedPresent, capacity);
        stagedValueChanged = Arrays.copyOf(stagedValueChanged, capacity);
        stagedVelocityChanged = Arrays.copyOf(stagedVelocityChanged, capacity);
        stagedCompleted = Arrays.copyOf(stagedCompleted, capacity);
    }

    /// Verifies that a property belongs to this registry and remains in its live slot.
    ///
    /// @param property the candidate property
    private void requireOwnedPropertyUnderLock(AnimatedScalar property) {
        requireKnownPropertyUnderLock(property);
        if (property.closed
                || property.slot >= properties.size()
                || properties.get(property.slot) != property) {
            throw new IllegalStateException("Animated scalar property is not live");
        }
    }

    /// Verifies that a property was created by this registry, including after closure.
    ///
    /// @param property the candidate property
    private void requireKnownPropertyUnderLock(AnimatedScalar property) {
        Objects.requireNonNull(property, "property");
        if (property.registry != this) {
            throw new IllegalArgumentException("Animated scalar belongs to another registry");
        }
    }

    /// Verifies that this registry remains open.
    private void checkOpenUnderLock() {
        if (closed) {
            throw new IllegalStateException("Animation registry is closed");
        }
        if (eventLoop.isClosed()) {
            throw new IllegalStateException("Animation registry event loop is closed");
        }
    }

    /// Rejects lifecycle or sampling work from an application commit callback.
    ///
    /// @param operation the diagnostic operation name
    private void checkNoCommitCallbackUnderLock(String operation) {
        if (commitInProgress) {
            throw new IllegalStateException(operation + " cannot run during animation commit staging");
        }
    }

    /// Validates monotonic registry time.
    ///
    /// @param timestampNanos the candidate timestamp
    private void validateTimestampUnderLock(long timestampNanos) {
        if (timestampNanos < 0L) {
            throw new IllegalStateException("Animation clock returned a negative timestamp");
        }
        if (timestampNanos < lastTimestampNanos) {
            throw new IllegalStateException("Animation clock moved backwards");
        }
    }

    /// Verifies that a motion's complete bounded timeline fits the nanosecond domain.
    ///
    /// @param timestampNanos the motion start timestamp
    /// @param motion the effective motion
    private static void validateTimelineRange(long timestampNanos, MotionSpec motion) {
        long duration;
        if (motion instanceof TweenSpec tween) {
            duration = Math.addExact(tween.delayNanos(), tween.durationNanos());
        } else if (motion instanceof SpringSpec spring) {
            duration = spring.maximumDurationNanos();
        } else {
            return;
        }
        if (Long.MAX_VALUE - timestampNanos < duration) {
            throw new IllegalArgumentException("Animation timeline exceeds nanosecond range");
        }
    }

    /// Returns whether two doubles have identical canonical bit representations.
    ///
    /// @param first the first scalar
    /// @param second the second scalar
    /// @return whether the canonical bits agree
    private static boolean sameBits(double first, double second) {
        return Double.doubleToLongBits(first) == Double.doubleToLongBits(second);
    }

    /// Holds one active transaction's exactly-once completion aggregation.
    @NotNullByDefault
    private static final class CompletionGroup {
        /// The positive transaction identity.
        private final long transactionId;

        /// The number of semantically changed targets in the transaction.
        private final int targetCount;

        /// The number of active property members yet to terminate.
        private int remainingCount;

        /// The highest-severity member outcome observed so far.
        private AnimationCompletionOutcome outcome = AnimationCompletionOutcome.COMPLETED;

        /// Creates a group before active property members are assigned.
        ///
        /// @param transactionId the transaction identity
        /// @param targetCount the semantic target count
        private CompletionGroup(long transactionId, int targetCount) {
            this.transactionId = transactionId;
            this.targetCount = targetCount;
        }

        /// Merges one terminal member outcome into this group.
        ///
        /// @param memberOutcome the member outcome
        private void record(AnimationCompletionOutcome memberOutcome) {
            Objects.requireNonNull(memberOutcome, "memberOutcome");
            if (severity(memberOutcome) > severity(outcome)) {
                outcome = memberOutcome;
            }
        }

        /// Returns deterministic terminal-outcome precedence.
        ///
        /// @param candidate the outcome
        /// @return its severity rank
        private static int severity(AnimationCompletionOutcome candidate) {
            return switch (candidate) {
                case COMPLETED -> 0;
                case SKIPPED -> 1;
                case REPLACED -> 2;
                case CANCELLED -> 3;
                case FAILED -> 4;
            };
        }
    }

    /// Stores one allocation-free scalar motion sample.
    @NotNullByDefault
    private static final class MutableMotionSample {
        /// The sampled scalar value.
        private double value;

        /// The sampled scalar-per-second velocity.
        private double velocity;

        /// Whether the timeline reached its terminal condition.
        private boolean completed;

        /// Creates an initially zero reusable sample.
        private MutableMotionSample() {
        }

        /// Replaces every sample field.
        ///
        /// @param value the sampled value
        /// @param velocity the sampled velocity
        /// @param completed whether the motion completed
        private void set(double value, double velocity, boolean completed) {
            this.value = value;
            this.velocity = velocity;
            this.completed = completed;
        }
    }
}

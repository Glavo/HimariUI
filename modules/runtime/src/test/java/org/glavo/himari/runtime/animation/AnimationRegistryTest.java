package org.glavo.himari.runtime.animation;

import org.glavo.himari.platform.headless.HeadlessEventLoop;
import org.glavo.himari.platform.headless.ManualFrameClock;
import org.glavo.himari.state.IntState;
import org.glavo.himari.state.StateDomain;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies atomic animation commits, deterministic sampling, retargeting, and terminal outcomes.
@NotNullByDefault
final class AnimationRegistryTest {
    /// Verifies that a staged batch publishes all model targets without writing application state.
    @Test
    void commitsModelTargetsAtomicallyWithoutWritingApplicationState() {
        try (HeadlessEventLoop eventLoop = new HeadlessEventLoop();
             AnimationRegistry registry = new AnimationRegistry(eventLoop)) {
            StateDomain stateDomain = new StateDomain();
            IntState applicationState = stateDomain.intState(7);
            AnimatedScalar width = registry.createScalar(
                    "width",
                    100.0,
                    ScalarAnimationAdapter.NON_NEGATIVE,
                    AnimationPhaseImpact.MEASURE
            );
            AnimatedScalar opacity = registry.createScalar(
                    "opacity",
                    1.0,
                    ScalarAnimationAdapter.UNIT_INTERVAL,
                    AnimationPhaseImpact.COMPOSITE
            );

            AnimationCommitResult result = registry.commit(
                    transaction(1L, TweenSpec.linear(1_000_000_000L)),
                    commit -> {
                        commit.setTarget(width, 200.0);
                        assertEquals(100.0, width.modelTarget());
                        assertEquals(1.0, opacity.modelTarget());
                        commit.setTarget(opacity, 0.25);
                        assertEquals(100.0, width.modelTarget());
                        assertEquals(1.0, opacity.modelTarget());
                    }
            );

            assertEquals(2, result.changedTargetCount());
            assertEquals(2, result.activeAnimationCount());
            assertEquals(1L, result.presentationEpoch());
            assertTrue(result.phaseImpact().isEmpty());
            assertEquals(200.0, width.modelTarget());
            assertEquals(100.0, width.presentationValue());
            assertEquals(0.25, opacity.modelTarget());
            assertEquals(1.0, opacity.presentationValue());
            assertEquals(7, applicationState.get());
            assertEquals(0L, stateDomain.epoch());
            assertTrue(registry.completionEvents().isEmpty());
        }
    }

    /// Verifies one timestamp, epoch, and phase union for a coordinated tween group.
    @Test
    void samplesCoordinatedTweensAtOnePresentationEpoch() {
        try (HeadlessEventLoop eventLoop = new HeadlessEventLoop();
             AnimationRegistry registry = new AnimationRegistry(eventLoop)) {
            AnimatedScalar first = registry.createScalar(
                    "first",
                    0.0,
                    ScalarAnimationAdapter.UNBOUNDED,
                    AnimationPhaseImpact.MEASURE
            );
            AnimatedScalar second = registry.createScalar(
                    "second",
                    0.0,
                    ScalarAnimationAdapter.UNBOUNDED,
                    AnimationPhaseImpact.COMPOSITE
            );
            registry.commit(transaction(1L, TweenSpec.linear(1_000_000_000L)), commit -> {
                commit.setTarget(first, 10.0);
                commit.setTarget(second, 20.0);
            });

            eventLoop.clock().advanceTo(250_000_000L);
            assertTrue(registry.sample());
            AnimationRegistrySnapshot quarter = registry.snapshot();
            assertEquals(2L, quarter.presentationEpoch());
            assertEquals(250_000_000L, quarter.lastTimestampNanos());
            assertEquals(2.5, first.presentationValue());
            assertEquals(5.0, second.presentationValue());
            assertEquals(AnimationPhaseImpact.MEASURE, quarter.lastPhaseImpact());

            eventLoop.clock().advanceTo(1_000_000_000L);
            assertTrue(registry.sample());
            assertFalse(registry.sample());
            assertEquals(10.0, first.presentationValue());
            assertEquals(20.0, second.presentationValue());
            assertFalse(first.isActive());
            assertFalse(second.isActive());
            assertEquals(3L, registry.presentationEpoch());
            AnimationCompletionEvent completion = registry.completionEvents().getFirst();
            assertEquals(1L, completion.transactionId());
            assertEquals(1_000_000_000L, completion.timestampNanos());
            assertEquals(AnimationCompletionOutcome.COMPLETED, completion.outcome());
            assertEquals(2, completion.targetCount());
        }
    }

    /// Verifies finite endpoint velocities for curves with coincident endpoint control points.
    @Test
    void evaluatesCubicBezierEndpointSlopesByTheirFiniteLimits() {
        assertEquals(1.0 / 0.58, CubicBezierCurve.EASE_OUT.slope(0.0), 1.0e-15);
        assertEquals(1.0 / 0.58, CubicBezierCurve.EASE_IN.slope(1.0), 1.0e-15);
        assertEquals(1.0, CubicBezierCurve.LINEAR.slope(0.0));
        assertEquals(1.0, CubicBezierCurve.LINEAR.slope(1.0));
        assertThrows(
                IllegalArgumentException.class,
                () -> new CubicBezierCurve(0.0, 1.0, 0.5, 1.0)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new SpringSpec(
                        Double.MIN_VALUE,
                        Double.MAX_VALUE,
                        0.0,
                        0.0,
                        0.0,
                        1L
                )
        );
    }

    /// Verifies the reference critically damped spring value and velocity at an exact timestamp.
    @Test
    void samplesAnalyticSpringValueAndVelocity() {
        try (HeadlessEventLoop eventLoop = new HeadlessEventLoop();
             AnimationRegistry registry = new AnimationRegistry(eventLoop)) {
            AnimatedScalar value = registry.createScalar(
                    "critical",
                    0.0,
                    ScalarAnimationAdapter.UNBOUNDED,
                    AnimationPhaseImpact.PAINT
            );
            SpringSpec spring = new SpringSpec(
                    1.0,
                    100.0,
                    20.0,
                    0.0,
                    0.0,
                    10_000_000_000L
            );
            registry.commit(transaction(1L, spring), commit -> commit.setTarget(value, 1.0));

            eventLoop.clock().advanceTo(100_000_000L);
            assertTrue(registry.sample());

            assertEquals(0.26424111765711533, value.presentationValue(), 1.0e-15);
            assertEquals(3.6787944117144233, value.velocity(), 1.0e-14);
            assertEquals(AnimationPhaseImpact.PAINT, registry.lastPhaseImpact());
            assertTrue(value.isActive());
        }
    }

    /// Verifies that elapsed-time spring sampling is independent of intermediate frame frequency.
    @Test
    void producesFrequencyIndependentSpringSamples() {
        ManualFrameClock denseClock = new ManualFrameClock();
        ManualFrameClock sparseClock = new ManualFrameClock();
        try (HeadlessEventLoop denseLoop = new HeadlessEventLoop(denseClock);
             HeadlessEventLoop sparseLoop = new HeadlessEventLoop(sparseClock);
             AnimationRegistry denseRegistry = new AnimationRegistry(denseLoop);
             AnimationRegistry sparseRegistry = new AnimationRegistry(sparseLoop)) {
            SpringSpec spring = new SpringSpec(
                    1.0,
                    120.0,
                    9.0,
                    0.0,
                    0.0,
                    10_000_000_000L
            );
            AnimatedScalar dense = denseRegistry.createScalar(
                    "dense",
                    -2.0,
                    ScalarAnimationAdapter.UNBOUNDED,
                    AnimationPhaseImpact.PLACE
            );
            AnimatedScalar sparse = sparseRegistry.createScalar(
                    "sparse",
                    -2.0,
                    ScalarAnimationAdapter.UNBOUNDED,
                    AnimationPhaseImpact.PLACE
            );
            denseRegistry.commit(transaction(1L, spring), commit -> commit.setTarget(dense, 7.0));
            sparseRegistry.commit(transaction(1L, spring), commit -> commit.setTarget(sparse, 7.0));

            for (long timestamp = 10_000_000L;
                 timestamp <= 500_000_000L;
                 timestamp += 10_000_000L) {
                denseClock.advanceTo(timestamp);
                denseRegistry.sample();
            }
            sparseClock.advanceTo(500_000_000L);
            sparseRegistry.sample();

            assertEquals(
                    Double.doubleToLongBits(dense.presentationValue()),
                    Double.doubleToLongBits(sparse.presentationValue())
            );
            assertEquals(
                    Double.doubleToLongBits(dense.velocity()),
                    Double.doubleToLongBits(sparse.velocity())
            );
        }
    }

    /// Verifies value and velocity continuity across compatible spring retargeting.
    @Test
    void preservesValueAndVelocityForCompatibleSpringRetargeting() {
        try (HeadlessEventLoop eventLoop = new HeadlessEventLoop();
             AnimationRegistry registry = new AnimationRegistry(eventLoop)) {
            AnimatedScalar value = registry.createScalar(
                    "retarget",
                    0.0,
                    ScalarAnimationAdapter.UNBOUNDED,
                    AnimationPhaseImpact.PLACE
            );
            registry.commit(transaction(1L, SpringSpec.DEFAULT), commit -> commit.setTarget(value, 10.0));
            eventLoop.clock().advanceTo(200_000_000L);
            registry.sample();
            double beforeValue = value.presentationValue();
            double beforeVelocity = value.velocity();

            registry.commit(transaction(2L, SpringSpec.DEFAULT), commit -> commit.setTarget(value, 20.0));

            assertEquals(
                    Double.doubleToLongBits(beforeValue),
                    Double.doubleToLongBits(value.presentationValue())
            );
            assertEquals(
                    Double.doubleToLongBits(beforeVelocity),
                    Double.doubleToLongBits(value.velocity())
            );
            assertEquals(2L, value.snapshot().transactionId());
            assertEquals(2L, value.snapshot().replacementGeneration());
            AnimationCompletionEvent replaced = registry.completionEvents().getFirst();
            assertEquals(1L, replaced.transactionId());
            assertEquals(AnimationCompletionOutcome.REPLACED, replaced.outcome());
            assertEquals(200_000_000L, replaced.timestampNanos());
        }
    }

    /// Verifies that incompatible replacement preserves value while discarding meaningless velocity.
    @Test
    void preservesOnlyValueForIncompatibleRetargeting() {
        try (HeadlessEventLoop eventLoop = new HeadlessEventLoop();
             AnimationRegistry registry = new AnimationRegistry(eventLoop)) {
            AnimatedScalar value = registry.createScalar(
                    "incompatible",
                    0.0,
                    ScalarAnimationAdapter.UNBOUNDED,
                    AnimationPhaseImpact.PAINT
            );
            registry.commit(transaction(1L, TweenSpec.linear(1_000_000_000L)),
                    commit -> commit.setTarget(value, 10.0));
            eventLoop.clock().advanceTo(500_000_000L);
            registry.sample();
            assertEquals(5.0, value.presentationValue());
            assertEquals(10.0, value.velocity());

            registry.commit(transaction(2L, SpringSpec.DEFAULT), commit -> commit.setTarget(value, 20.0));

            assertEquals(5.0, value.presentationValue());
            assertEquals(0.0, value.velocity());
            assertEquals(AnimationCompletionOutcome.REPLACED,
                    registry.completionEvents().getFirst().outcome());
        }
    }

    /// Verifies explicit gesture velocity handoff into a physical spring.
    @Test
    void acceptsExplicitGestureVelocityForSpringMotion() {
        try (HeadlessEventLoop eventLoop = new HeadlessEventLoop();
             AnimationRegistry registry = new AnimationRegistry(eventLoop)) {
            AnimatedScalar value = registry.createScalar(
                    "gesture",
                    0.0,
                    ScalarAnimationAdapter.UNBOUNDED,
                    AnimationPhaseImpact.PLACE
            );
            registry.commit(transaction(1L, SpringSpec.DEFAULT),
                    commit -> commit.setTargetWithVelocity(value, 10.0, 7.5));

            assertEquals(7.5, value.velocity());
            assertFalse(registry.sample());
            assertEquals(7.5, value.velocity());
            assertEquals(0.0, value.presentationValue());
        }
    }

    /// Verifies explicit restart discontinuity and active-replacement snapping outcomes.
    @Test
    void appliesRestartAndSnapReplacementPoliciesExplicitly() {
        try (HeadlessEventLoop eventLoop = new HeadlessEventLoop();
             AnimationRegistry registry = new AnimationRegistry(eventLoop)) {
            AnimatedScalar value = registry.createScalar(
                    "policies",
                    0.0,
                    ScalarAnimationAdapter.UNBOUNDED,
                    AnimationPhaseImpact.PAINT
            );
            registry.commit(transaction(1L, TweenSpec.linear(1_000_000_000L)),
                    commit -> commit.setTarget(value, 10.0));
            eventLoop.clock().advanceTo(500_000_000L);
            registry.sample();
            assertEquals(5.0, value.presentationValue());

            registry.commit(
                    transaction(2L, TweenSpec.linear(1_000_000_000L), AnimationReplacementPolicy.RESTART),
                    commit -> commit.setTarget(value, 20.0)
            );
            assertEquals(10.0, value.presentationValue());
            assertEquals(0.0, value.velocity());
            assertEquals(AnimationPhaseImpact.PAINT, registry.lastPhaseImpact());

            AnimationCommitResult snap = registry.commit(
                    transaction(3L, TweenSpec.linear(1_000_000_000L), AnimationReplacementPolicy.SNAP),
                    commit -> commit.setTarget(value, 30.0)
            );
            assertSame(AnimationCompletionOutcome.SKIPPED, snap.immediateOutcome());
            assertEquals(30.0, value.modelTarget());
            assertEquals(30.0, value.presentationValue());
            assertEquals(0.0, value.velocity());
            assertFalse(value.isActive());
            AnimationCommitResult inactiveSnap = registry.commit(
                    transaction(4L, TweenSpec.linear(1_000_000_000L), AnimationReplacementPolicy.SNAP),
                    commit -> commit.setTarget(value, 40.0)
            );
            assertSame(AnimationCompletionOutcome.SKIPPED, inactiveSnap.immediateOutcome());
            assertEquals(40.0, value.presentationValue());
            assertFalse(value.isActive());
            assertEquals(
                    List.of(
                            AnimationCompletionOutcome.REPLACED,
                            AnimationCompletionOutcome.REPLACED,
                            AnimationCompletionOutcome.SKIPPED,
                            AnimationCompletionOutcome.SKIPPED
                    ),
                    registry.completionEvents().stream().map(AnimationCompletionEvent::outcome).toList()
            );
        }
    }

    /// Verifies disabled-motion substitution, final state, and its exactly-once skipped outcome.
    @Test
    void snapsDisabledMotionWithSkippedCompletion() {
        try (HeadlessEventLoop eventLoop = new HeadlessEventLoop();
             AnimationRegistry registry = new AnimationRegistry(eventLoop)) {
            AnimatedScalar value = registry.createScalar(
                    "reduced-motion",
                    0.0,
                    ScalarAnimationAdapter.UNBOUNDED,
                    AnimationPhaseImpact.COMPOSITE
            );
            TweenSpec requested = TweenSpec.easeInOut(2_000_000_000L);
            AnimationTransaction transaction = AnimationTransaction.disabled(1L, 17L, 3L, requested);

            AnimationCommitResult result = registry.commit(transaction, commit -> commit.setTarget(value, 1.0));

            assertSame(requested, transaction.requestedMotion());
            assertSame(SnapMotionSpec.INSTANCE, transaction.effectiveMotion());
            assertSame(AnimationMotionDisposition.DISABLED, transaction.motionDisposition());
            assertSame(AnimationCompletionOutcome.SKIPPED, result.immediateOutcome());
            assertEquals(1.0, value.modelTarget());
            assertEquals(1.0, value.presentationValue());
            assertFalse(value.isActive());
            assertEquals(AnimationPhaseImpact.COMPOSITE, result.phaseImpact());
            assertEquals(1, registry.completionEvents().size());
            assertEquals(AnimationCompletionOutcome.SKIPPED,
                    registry.completionEvents().getFirst().outcome());
        }
    }

    /// Verifies that callback and validation failures leave all targets unchanged and remain usable.
    @Test
    void keepsFailedCommitsAtomicAndReportsThemExactlyOnce() {
        try (HeadlessEventLoop eventLoop = new HeadlessEventLoop();
             AnimationRegistry registry = new AnimationRegistry(eventLoop)) {
            AnimatedScalar normal = registry.createScalar(
                    "normal",
                    0.0,
                    ScalarAnimationAdapter.UNBOUNDED,
                    AnimationPhaseImpact.PAINT
            );
            AnimatedScalar huge = registry.createScalar(
                    "huge",
                    -Double.MAX_VALUE,
                    ScalarAnimationAdapter.UNBOUNDED,
                    AnimationPhaseImpact.PAINT
            );

            assertThrows(IllegalStateException.class, () -> registry.commit(
                    transaction(1L, TweenSpec.linear(1_000_000_000L)),
                    commit -> {
                        commit.setTarget(normal, 5.0);
                        throw new IllegalStateException("planned staging failure");
                    }
            ));
            assertEquals(0.0, normal.modelTarget());
            assertEquals(0.0, normal.presentationValue());

            assertThrows(IllegalArgumentException.class, () -> registry.commit(
                    transaction(2L, TweenSpec.linear(1_000_000_000L)),
                    commit -> commit.setTarget(huge, Double.MAX_VALUE)
            ));
            assertEquals(-Double.MAX_VALUE, huge.modelTarget());
            assertEquals(-Double.MAX_VALUE, huge.presentationValue());
            assertEquals(0L, registry.presentationEpoch());
            assertEquals(
                    List.of(AnimationCompletionOutcome.FAILED, AnimationCompletionOutcome.FAILED),
                    registry.completionEvents().stream().map(AnimationCompletionEvent::outcome).toList()
            );
            assertEquals(2, registry.snapshot().reservedCompletionSlots());

            assertEquals(2, registry.drainCompletionEvents().size());
            assertThrows(IllegalArgumentException.class, () -> registry.commit(
                    transaction(3L, TweenSpec.linear(1_000_000_000L)),
                    commit -> commit.setTargetWithVelocity(normal, 5.0, 2.0)
            ));
            assertEquals(AnimationCompletionOutcome.FAILED,
                    registry.drainCompletionEvents().getFirst().outcome());
            AnimationCommitResult recovery = registry.commit(
                    transaction(4L, SnapMotionSpec.INSTANCE),
                    commit -> commit.setTarget(normal, 5.0)
            );
            assertSame(AnimationCompletionOutcome.SKIPPED, recovery.immediateOutcome());
            assertEquals(5.0, normal.presentationValue());
        }
    }

    /// Verifies bounded completion backpressure and reservation release only after draining.
    @Test
    void boundsExactlyOnceCompletionReservations() {
        try (HeadlessEventLoop eventLoop = new HeadlessEventLoop();
             AnimationRegistry registry = new AnimationRegistry(eventLoop, 1)) {
            AnimatedScalar value = registry.createScalar(
                    "capacity",
                    0.0,
                    ScalarAnimationAdapter.UNBOUNDED,
                    AnimationPhaseImpact.COMPOSITE
            );
            registry.commit(transaction(1L, TweenSpec.linear(100_000_000L)),
                    commit -> commit.setTarget(value, 1.0));
            eventLoop.clock().advanceTo(100_000_000L);
            registry.sample();
            assertEquals(1, registry.snapshot().reservedCompletionSlots());
            AtomicBoolean callbackRan = new AtomicBoolean();

            assertThrows(RejectedExecutionException.class, () -> registry.commit(
                    transaction(2L, SnapMotionSpec.INSTANCE),
                    commit -> callbackRan.set(true)
            ));
            assertFalse(callbackRan.get());

            assertEquals(1, registry.drainCompletionEvents().size());
            AnimationCommitResult accepted = registry.commit(
                    transaction(2L, SnapMotionSpec.INSTANCE),
                    commit -> callbackRan.set(true)
            );
            assertTrue(callbackRan.get());
            assertSame(AnimationCompletionOutcome.SKIPPED, accepted.immediateOutcome());
        }
    }

    /// Verifies grouped cancellation, post-close diagnostics, and borrowed-loop ownership.
    @Test
    void closesPropertiesAndRegistryWithOneCancelledGroupOutcome() {
        HeadlessEventLoop eventLoop = new HeadlessEventLoop();
        AnimationRegistry registry = new AnimationRegistry(eventLoop);
        AnimatedScalar first = registry.createScalar(
                "first-close",
                0.0,
                ScalarAnimationAdapter.UNBOUNDED,
                AnimationPhaseImpact.PLACE
        );
        AnimatedScalar second = registry.createScalar(
                "second-close",
                0.0,
                ScalarAnimationAdapter.UNBOUNDED,
                AnimationPhaseImpact.PLACE
        );
        registry.commit(transaction(1L, SpringSpec.DEFAULT), commit -> {
            commit.setTarget(first, 1.0);
            commit.setTarget(second, 2.0);
        });

        first.close();
        assertTrue(first.isClosed());
        assertTrue(registry.completionEvents().isEmpty());
        AnimatedScalar replacement = registry.createScalar(
                "reused-slot",
                3.0,
                ScalarAnimationAdapter.UNBOUNDED,
                AnimationPhaseImpact.COMPOSITE
        );
        assertEquals(first.slot, replacement.slot);
        assertEquals(3L, replacement.propertyId());
        assertEquals(
                List.of(2L, 3L),
                registry.snapshot().properties().stream()
                        .map(AnimatedScalarSnapshot::propertyId)
                        .toList()
        );
        registry.close();
        registry.close();

        assertTrue(second.isClosed());
        assertTrue(replacement.isClosed());
        assertTrue(registry.snapshot().closed());
        assertTrue(registry.snapshot().properties().isEmpty());
        assertFalse(eventLoop.isClosed());
        AnimationCompletionEvent completion = registry.drainCompletionEvents().getFirst();
        assertEquals(AnimationCompletionOutcome.CANCELLED, completion.outcome());
        assertEquals(2, completion.targetCount());
        assertTrue(registry.completionEvents().isEmpty());
        eventLoop.close();
    }

    /// Verifies that cross-thread observers never see half of a coordinated presentation epoch.
    ///
    /// @throws InterruptedException if the test thread is interrupted while coordinating its reader
    @Test
    void publishesCoordinatedSnapshotsAtomicallyAcrossThreads() throws InterruptedException {
        try (HeadlessEventLoop eventLoop = new HeadlessEventLoop();
             AnimationRegistry registry = new AnimationRegistry(eventLoop)) {
            AnimatedScalar positive = registry.createScalar(
                    "positive",
                    0.0,
                    ScalarAnimationAdapter.UNBOUNDED,
                    AnimationPhaseImpact.COMPOSITE
            );
            AnimatedScalar negative = registry.createScalar(
                    "negative",
                    0.0,
                    ScalarAnimationAdapter.UNBOUNDED,
                    AnimationPhaseImpact.COMPOSITE
            );
            registry.commit(transaction(1L, TweenSpec.linear(1_000_000_000L)), commit -> {
                commit.setTarget(positive, 100.0);
                commit.setTarget(negative, -100.0);
            });

            CountDownLatch ready = new CountDownLatch(1);
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch firstRead = new CountDownLatch(1);
            AtomicBoolean stop = new AtomicBoolean();
            AtomicInteger reads = new AtomicInteger();
            AtomicReference<@Nullable Throwable> failure = new AtomicReference<>();
            Thread reader = Thread.ofPlatform().name("animation-snapshot-reader").start(() -> {
                ready.countDown();
                try {
                    start.await();
                    long previousEpoch = -1L;
                    while (!stop.get()) {
                        AnimationRegistrySnapshot snapshot = registry.snapshot();
                        @Unmodifiable List<AnimatedScalarSnapshot> properties = snapshot.properties();
                        if (properties.size() != 2) {
                            throw new AssertionError("Coordinated properties disappeared");
                        }
                        double firstValue = properties.get(0).presentationValue();
                        double secondValue = properties.get(1).presentationValue();
                        if (secondValue != -firstValue) {
                            throw new AssertionError("Observed a partial coordinated publication");
                        }
                        if (snapshot.presentationEpoch() < previousEpoch) {
                            throw new AssertionError("Presentation epochs moved backwards");
                        }
                        previousEpoch = snapshot.presentationEpoch();
                        reads.incrementAndGet();
                        firstRead.countDown();
                    }
                } catch (Throwable throwable) {
                    failure.set(throwable);
                    firstRead.countDown();
                }
            });

            ready.await();
            start.countDown();
            firstRead.await();
            for (long timestamp = 1_000_000L;
                 timestamp <= 1_000_000_000L;
                 timestamp += 1_000_000L) {
                eventLoop.clock().advanceTo(timestamp);
                registry.sample();
            }
            stop.set(true);
            reader.join();

            @Nullable Throwable observedFailure = failure.get();
            if (observedFailure != null) {
                throw new AssertionError("Cross-thread snapshot validation failed", observedFailure);
            }
            assertTrue(reads.get() > 0);
            assertEquals(100.0, positive.presentationValue());
            assertEquals(-100.0, negative.presentationValue());
        }
    }

    /// Snaps nonessential motion when reduced-motion policy is active.
    @Test
    void snapsNonessentialMotionUnderReducedMotionPolicy() {
        try (HeadlessEventLoop eventLoop = new HeadlessEventLoop();
             AnimationRegistry registry = new AnimationRegistry(eventLoop)) {
            AnimatedScalar value = registry.createScalar(
                    "opacity",
                    0.0,
                    ScalarAnimationAdapter.UNIT_INTERVAL,
                    AnimationPhaseImpact.COMPOSITE
            );
            TweenSpec requested = TweenSpec.linear(1_000_000_000L);
            AnimationTransaction transaction = MotionPolicy.resolve(
                    true,
                    MotionImportance.NONESSENTIAL,
                    1L,
                    11L,
                    1L,
                    requested,
                    AnimationReplacementPolicy.PRESERVE_VELOCITY
            );
            AnimationCommitResult result = registry.commit(
                    transaction,
                    commit -> commit.setTarget(value, 1.0)
            );
            assertSame(requested, transaction.requestedMotion());
            assertSame(SnapMotionSpec.INSTANCE, transaction.effectiveMotion());
            assertSame(AnimationMotionDisposition.DISABLED, transaction.motionDisposition());
            assertSame(AnimationCompletionOutcome.SKIPPED, result.immediateOutcome());
            assertEquals(1.0, value.presentationValue());
            assertFalse(value.isActive());
        }
    }

    /// Shortens essential tweens and still samples them under reduced-motion policy.
    @Test
    void shortensEssentialTweenUnderReducedMotionPolicy() {
        try (HeadlessEventLoop eventLoop = new HeadlessEventLoop();
             AnimationRegistry registry = new AnimationRegistry(eventLoop)) {
            AnimatedScalar value = registry.createScalar(
                    "focus-ring",
                    0.0,
                    ScalarAnimationAdapter.UNBOUNDED,
                    AnimationPhaseImpact.PAINT
            );
            TweenSpec requested = TweenSpec.linear(1_000_000_000L);
            AnimationTransaction transaction = MotionPolicy.resolve(
                    true,
                    MotionImportance.ESSENTIAL,
                    1L,
                    12L,
                    1L,
                    requested,
                    AnimationReplacementPolicy.PRESERVE_VELOCITY
            );
            assertSame(AnimationMotionDisposition.REDUCED, transaction.motionDisposition());
            assertTrue(transaction.effectiveMotion() instanceof TweenSpec);
            TweenSpec effective = (TweenSpec) transaction.effectiveMotion();
            assertEquals(MotionPolicy.REDUCED_TWEEN_MAX_NANOS, effective.durationNanos());
            assertEquals(0L, effective.delayNanos());
            registry.commit(transaction, commit -> commit.setTarget(value, 100.0));
            eventLoop.clock().advanceTo(MotionPolicy.REDUCED_TWEEN_MAX_NANOS / 2L);
            assertTrue(registry.sample());
            assertEquals(50.0, value.presentationValue());
            assertTrue(value.isActive());
        }
    }

    /// Removes bounce from essential springs under reduced-motion policy.
    @Test
    void removesEssentialSpringBounceUnderReducedMotionPolicy() {
        SpringSpec requested = new SpringSpec(1.0, 170.0, 4.0, 1.0e-4, 1.0e-4, 10_000_000_000L);
        AnimationTransaction transaction = MotionPolicy.resolve(
                true,
                MotionImportance.ESSENTIAL,
                1L,
                13L,
                1L,
                requested,
                AnimationReplacementPolicy.PRESERVE_VELOCITY
        );
        assertTrue(transaction.effectiveMotion() instanceof SpringSpec);
        SpringSpec effective = (SpringSpec) transaction.effectiveMotion();
        double critical = 2.0 * StrictMath.sqrt(requested.stiffness() * requested.mass());
        assertEquals(critical, effective.damping());
        assertTrue(effective.damping() > requested.damping());
        try (HeadlessEventLoop eventLoop = new HeadlessEventLoop();
             AnimationRegistry registry = new AnimationRegistry(eventLoop)) {
            AnimatedScalar value = registry.createScalar(
                    "offset",
                    0.0,
                    ScalarAnimationAdapter.UNBOUNDED,
                    AnimationPhaseImpact.PLACE
            );
            registry.commit(transaction, commit -> commit.setTarget(value, 10.0));
            for (long timestamp = 5_000_000L; timestamp <= 400_000_000L; timestamp += 5_000_000L) {
                eventLoop.clock().advanceTo(timestamp);
                registry.sample();
                assertTrue(value.presentationValue() <= 10.0);
            }
        }
    }

    /// Leaves requested motion unchanged when reduced-motion policy is off.
    @Test
    void preservesRequestedMotionWhenReducedMotionIsOff() {
        TweenSpec requested = TweenSpec.easeInOut(250_000_000L);
        AnimationTransaction transaction = MotionPolicy.resolve(
                false,
                MotionImportance.NONESSENTIAL,
                1L,
                14L,
                1L,
                requested,
                AnimationReplacementPolicy.PRESERVE_VELOCITY
        );
        assertSame(requested, transaction.effectiveMotion());
        assertSame(AnimationMotionDisposition.STANDARD, transaction.motionDisposition());
    }

    /// Verifies that a compositor-only property never reports an earlier pipeline phase.
    @Test
    void preservesExactCompositorOnlyPhaseImpact() {
        try (HeadlessEventLoop eventLoop = new HeadlessEventLoop();
             AnimationRegistry registry = new AnimationRegistry(eventLoop)) {
            AnimatedScalar opacity = registry.createScalar(
                    "compositor-opacity",
                    0.0,
                    ScalarAnimationAdapter.UNIT_INTERVAL,
                    AnimationPhaseImpact.COMPOSITE
            );
            registry.commit(transaction(1L, TweenSpec.linear(1_000_000_000L)),
                    commit -> commit.setTarget(opacity, 1.0));
            eventLoop.clock().advanceTo(500_000_000L);
            registry.sample();

            AnimationPhaseImpact impact = registry.lastPhaseImpact();
            assertEquals(List.of(AnimationPhase.COMPOSITE), impact.phases());
            assertFalse(impact.includes(AnimationPhase.STRUCTURE));
            assertFalse(impact.includes(AnimationPhase.MEASURE));
            assertFalse(impact.includes(AnimationPhase.PLACE));
            assertFalse(impact.includes(AnimationPhase.PAINT));
        }
    }

    /// Creates a standard preserve-velocity animation transaction.
    ///
    /// @param transactionId the positive transaction identity
    /// @param motion the effective motion
    /// @return the transaction
    private static AnimationTransaction transaction(long transactionId, MotionSpec motion) {
        return transaction(transactionId, motion, AnimationReplacementPolicy.PRESERVE_VELOCITY);
    }

    /// Creates a standard animation transaction with an explicit replacement policy.
    ///
    /// @param transactionId the positive transaction identity
    /// @param motion the effective motion
    /// @param replacementPolicy the replacement policy
    /// @return the transaction
    private static AnimationTransaction transaction(
            long transactionId,
            MotionSpec motion,
            AnimationReplacementPolicy replacementPolicy
    ) {
        return AnimationTransaction.standard(
                transactionId,
                transactionId * 10L,
                1L,
                motion,
                replacementPolicy
        );
    }
}

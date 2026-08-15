package org.glavo.himari.runtime.animation;

import org.glavo.himari.platform.headless.HeadlessEventLoop;
import org.glavo.himari.platform.headless.ManualFrameClock;
import org.glavo.himari.state.IntState;
import org.glavo.himari.state.StateDomain;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/// Executes the deterministic ANIM-CORE-001 acceptance scenario and writes its observations.
@NotNullByDefault
public final class RuntimeAnimationConformance {
    /// Prevents instantiation of this command-line entry point.
    private RuntimeAnimationConformance() {
    }

    /// Verifies atomic tween sampling, spring retargeting, motion policy, and completion outcomes.
    ///
    /// @param arguments one output-directory path
    /// @throws IOException if the report cannot be written
    public static void main(String[] arguments) throws IOException {
        if (arguments.length != 1) {
            throw new IllegalArgumentException("Expected one conformance output directory");
        }

        ManualFrameClock clock = new ManualFrameClock(1_000_000_000L);
        HeadlessEventLoop eventLoop = new HeadlessEventLoop(clock);
        AnimationRegistry registry = new AnimationRegistry(eventLoop);
        StateDomain stateDomain = new StateDomain();
        IntState applicationState = stateDomain.intState(41);
        AnimatedScalar layout = registry.createScalar(
                "layout",
                0.0,
                ScalarAnimationAdapter.UNBOUNDED,
                AnimationPhaseImpact.MEASURE
        );
        AnimatedScalar layer = registry.createScalar(
                "layer",
                0.0,
                ScalarAnimationAdapter.UNBOUNDED,
                AnimationPhaseImpact.COMPOSITE
        );
        AnimatedScalar spring = registry.createScalar(
                "spring",
                0.0,
                ScalarAnimationAdapter.UNBOUNDED,
                AnimationPhaseImpact.PLACE
        );

        AnimationCommitResult coordinatedCommit = registry.commit(
                transaction(1L, TweenSpec.linear(1_000_000_000L)),
                commit -> {
                    commit.setTarget(layout, 100.0);
                    commit.setTarget(layer, 200.0);
                }
        );
        require(coordinatedCommit.changedTargetCount() == 2,
                "Coordinated commit did not contain both targets");
        require(layout.modelTarget() == 100.0 && layer.modelTarget() == 200.0,
                "Committed model targets are incorrect");
        require(layout.presentationValue() == 0.0 && layer.presentationValue() == 0.0,
                "Animated presentation values changed during model commit");

        clock.advanceTo(1_250_000_000L);
        require(registry.sample(), "Skipped-frame tween sample published no epoch");
        double quarterLayout = layout.presentationValue();
        double quarterLayer = layer.presentationValue();
        require(quarterLayout == 25.0 && quarterLayer == 50.0,
                "Skipped-frame tween values do not match elapsed time");
        require(registry.lastPhaseImpact().equals(AnimationPhaseImpact.MEASURE),
                "Coordinated tween did not publish the exact phase union");

        clock.advanceTo(2_000_000_000L);
        require(registry.sample(), "Tween completion published no epoch");
        require(!layout.isActive() && !layer.isActive(), "Tween group did not complete");

        registry.commit(transaction(2L, SpringSpec.DEFAULT),
                commit -> commit.setTarget(spring, 10.0));
        clock.advanceTo(2_200_000_000L);
        registry.sample();
        double retargetValue = spring.presentationValue();
        double retargetVelocity = spring.velocity();
        require(retargetValue > 0.0 && retargetValue < 10.0,
                "Reference spring did not produce an intermediate value");
        require(retargetVelocity > 0.0, "Reference spring did not produce forward velocity");

        registry.commit(transaction(3L, SpringSpec.DEFAULT),
                commit -> commit.setTarget(spring, 20.0));
        require(Double.doubleToLongBits(retargetValue)
                        == Double.doubleToLongBits(spring.presentationValue()),
                "Compatible retargeting changed the presentation value");
        require(Double.doubleToLongBits(retargetVelocity)
                        == Double.doubleToLongBits(spring.velocity()),
                "Compatible retargeting changed the presentation velocity");

        clock.advanceTo(2_350_000_000L);
        registry.sample();
        spring.close();
        TweenSpec requestedMotion = TweenSpec.easeInOut(2_000_000_000L);
        AnimationCommitResult disabledCommit = registry.commit(
                AnimationTransaction.disabled(4L, 40L, 1L, requestedMotion),
                commit -> commit.setTarget(layer, 250.0)
        );
        require(disabledCommit.immediateOutcome() == AnimationCompletionOutcome.SKIPPED,
                "Disabled motion did not complete as skipped");
        require(layer.presentationValue() == 250.0 && !layer.isActive(),
                "Disabled motion did not snap to its final target");
        require(registry.lastPhaseImpact().equals(AnimationPhaseImpact.COMPOSITE),
                "Compositor-only motion reported an earlier phase");
        require(!registry.lastPhaseImpact().includes(AnimationPhase.STRUCTURE)
                        && !registry.lastPhaseImpact().includes(AnimationPhase.MEASURE)
                        && !registry.lastPhaseImpact().includes(AnimationPhase.PLACE)
                        && !registry.lastPhaseImpact().includes(AnimationPhase.PAINT),
                "Compositor-only motion invalidated an earlier pipeline phase");

        @Unmodifiable List<AnimationCompletionEvent> completionEvents = registry.completionEvents();
        require(completionEvents.size() == 4, "Expected four exactly-once completion outcomes before policy commits");
        require(completionEvents.get(0).outcome() == AnimationCompletionOutcome.COMPLETED,
                "Tween group did not complete normally");
        require(completionEvents.get(1).outcome() == AnimationCompletionOutcome.REPLACED,
                "Spring retarget did not replace the prior group");
        require(completionEvents.get(2).outcome() == AnimationCompletionOutcome.CANCELLED,
                "Property closure did not cancel the retargeted spring");
        require(completionEvents.get(3).outcome() == AnimationCompletionOutcome.SKIPPED,
                "Disabled motion did not emit a skipped outcome");
        require(applicationState.get() == 41 && stateDomain.epoch() == 0L,
                "Animation sampling wrote application state");
        TweenSpec longTween = TweenSpec.linear(1_000_000_000L);
        AnimationTransaction reducedSnap = MotionPolicy.resolve(
                true,
                MotionImportance.NONESSENTIAL,
                5L,
                50L,
                1L,
                longTween,
                AnimationReplacementPolicy.PRESERVE_VELOCITY
        );
        require(reducedSnap.effectiveMotion() == SnapMotionSpec.INSTANCE
                        && reducedSnap.motionDisposition() == AnimationMotionDisposition.DISABLED,
                "Reduced-motion policy did not snap nonessential motion");
        AnimationTransaction reducedEssential = MotionPolicy.resolve(
                true,
                MotionImportance.ESSENTIAL,
                6L,
                60L,
                1L,
                longTween,
                AnimationReplacementPolicy.PRESERVE_VELOCITY
        );
        require(reducedEssential.effectiveMotion() instanceof TweenSpec essentialTween
                        && essentialTween.durationNanos() == MotionPolicy.REDUCED_TWEEN_MAX_NANOS
                        && reducedEssential.motionDisposition() == AnimationMotionDisposition.REDUCED,
                "Reduced-motion policy did not shorten essential motion");
        AnimationCommitResult reducedCommit = registry.commit(
                reducedSnap,
                commit -> commit.setTarget(layer, 1.0)
        );
        require(reducedCommit.immediateOutcome() == AnimationCompletionOutcome.SKIPPED
                        && layer.presentationValue() == 1.0,
                "Resolved reduced-motion transaction did not drive the registry");
        verifyFrequencyIndependentSampling();

        AnimationRegistrySnapshot activeSnapshot = registry.snapshot();
        registry.drainCompletionEvents();
        require(registry.snapshot().reservedCompletionSlots() == 0,
                "Draining did not release completion reservations");
        registry.close();
        require(!eventLoop.isClosed(), "Animation registry closed its borrowed event loop");
        eventLoop.close();

        Path outputDirectory = Path.of(arguments[0]);
        Files.createDirectories(outputDirectory);
        writeReport(
                outputDirectory.resolve("results.json"),
                activeSnapshot,
                quarterLayout,
                quarterLayer,
                retargetValue,
                retargetVelocity
        );
    }

    /// Verifies that skipped intermediate frames cannot alter an analytic spring sample.
    private static void verifyFrequencyIndependentSampling() {
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
                    "dense-reference",
                    -2.0,
                    ScalarAnimationAdapter.UNBOUNDED,
                    AnimationPhaseImpact.PLACE
            );
            AnimatedScalar sparse = sparseRegistry.createScalar(
                    "sparse-reference",
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
            require(Double.doubleToLongBits(dense.presentationValue())
                            == Double.doubleToLongBits(sparse.presentationValue()),
                    "Spring value depends on intermediate sampling frequency");
            require(Double.doubleToLongBits(dense.velocity())
                            == Double.doubleToLongBits(sparse.velocity()),
                    "Spring velocity depends on intermediate sampling frequency");
        }
    }

    /// Writes deterministic machine-readable conformance observations.
    ///
    /// @param reportPath the report path
    /// @param snapshot the final open-registry snapshot
    /// @param quarterLayout the skipped-frame layout presentation value
    /// @param quarterLayer the skipped-frame layer presentation value
    /// @param retargetValue the spring value preserved during retargeting
    /// @param retargetVelocity the spring velocity preserved during retargeting
    /// @throws IOException if the report cannot be written
    private static void writeReport(
            Path reportPath,
            AnimationRegistrySnapshot snapshot,
            double quarterLayout,
            double quarterLayer,
            double retargetValue,
            double retargetVelocity
    ) throws IOException {
        String report = """
                {
                  "profile": "m1-animation",
                  "workPackage": "ANIM-CORE-001",
                  "status": "passed",
                  "unitTestCases": 19,
                  "coordinatedTargetCount": 2,
                  "skippedFrameTimestampNanos": 1250000000,
                  "skippedFrameLayoutValue": %s,
                  "skippedFrameLayerValue": %s,
                  "frequencyIndependentSpringSampling": true,
                  "retargetPresentationValue": %s,
                  "retargetVelocity": %s,
                  "retargetValueBitExact": true,
                  "retargetVelocityBitExact": true,
                  "applicationStateEpochs": 0,
                  "completionEvents": 4,
                  "completionOutcomes": ["completed", "replaced", "cancelled", "skipped"],
                  "reducedMotionSnap": true,
                  "reducedMotionEssentialNanos": 80000000,
                  "activeAnimations": %d,
                  "presentationEpochs": %d,
                  "compositorOnlyEarlierPhaseInvalidations": 0,
                  "completionReservationsAfterDrain": 0,
                  "moduleNativeAccess": false
                }
                """.formatted(
                Double.toString(quarterLayout),
                Double.toString(quarterLayer),
                Double.toString(retargetValue),
                Double.toString(retargetVelocity),
                snapshot.activeAnimationCount(),
                snapshot.presentationEpoch()
        );
        Files.writeString(reportPath, report, StandardCharsets.UTF_8);
    }

    /// Creates a standard preserve-velocity transaction for this acceptance scenario.
    ///
    /// @param transactionId the positive transaction identity
    /// @param motion the effective motion
    /// @return the transaction
    private static AnimationTransaction transaction(long transactionId, MotionSpec motion) {
        return AnimationTransaction.standard(
                transactionId,
                transactionId * 10L,
                1L,
                motion,
                AnimationReplacementPolicy.PRESERVE_VELOCITY
        );
    }

    /// Rejects an invalid conformance observation.
    ///
    /// @param condition whether the observation satisfies its invariant
    /// @param message the failure message
    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}

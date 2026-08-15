package org.glavo.himari.runtime.structure;

import org.glavo.himari.runtime.animation.AnimationPhaseImpact;
import org.glavo.himari.runtime.effect.EffectApplyStatus;
import org.glavo.himari.runtime.effect.EffectCallbacks;
import org.glavo.himari.runtime.effect.EffectDependencies;
import org.glavo.himari.runtime.effect.EffectSession;
import org.glavo.himari.runtime.mount.MountApplyStatus;
import org.glavo.himari.state.IntState;
import org.glavo.himari.state.StateDomain;
import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Verifies that structural groups host independent bindings and keyed post-commit effects.
@NotNullByDefault
final class StructuralMountEffectTest {
    /// Verifies that binding reads do not rerun the owning structural group.
    @Test
    void bindingInvalidationDoesNotRerunStructure() {
        StateDomain domain = new StateDomain();
        IntState count = domain.intState(0);
        AtomicInteger groupRuns = new AtomicInteger();
        AtomicInteger labelApplies = new AtomicInteger();
        try (StructuralRuntime runtime = new StructuralRuntime(domain, scope -> {
            groupRuns.incrementAndGet();
            scope.mount("counter", element -> element.bind(
                    "label",
                    String.class,
                    AnimationPhaseImpact.MEASURE,
                    () -> "Count: " + count.get(),
                    ignored -> labelApplies.incrementAndGet()
            ));
        })) {
            assertEquals(StructuralAttemptStatus.COMMITTED, runtime.update().status());
            assertEquals(MountApplyStatus.COMMITTED, runtime.applyMountedProperties().status());
            assertEquals(1, groupRuns.get());
            count.set(4);
            assertEquals(StructuralAttemptStatus.NO_CHANGES, runtime.update().status());
            assertEquals(MountApplyStatus.COMMITTED, runtime.applyMountedProperties().status());
            assertEquals(1, groupRuns.get());
            assertEquals(2, labelApplies.get());
            assertEquals(
                    "Count: 4",
                    runtime.mounts().snapshot().elements().getFirst().property("label").value()
            );
        }
    }

    /// Verifies that a keyed effect updates after commit when its dependency changes.
    @Test
    void keyedEffectUpdatesAfterSuccessfulCommit() {
        StateDomain domain = new StateDomain();
        IntState token = domain.intState(1);
        List<String> log = new ArrayList<>();
        AtomicInteger groupRuns = new AtomicInteger();
        try (StructuralRuntime runtime = new StructuralRuntime(domain, scope -> {
            groupRuns.incrementAndGet();
            int current = token.get();
            scope.keyedEffect("load", EffectDependencies.of(current), new EffectCallbacks() {
                @Override
                public void onMount(EffectSession session) {
                    log.add("mount:" + current);
                }

                @Override
                public void onUpdate(EffectSession session) {
                    log.add("update:" + current);
                }

                @Override
                public void onCleanup() {
                    log.add("cleanup");
                }
            });
        })) {
            runtime.update();
            assertEquals(List.of("mount:1"), log);
            token.set(2);
            runtime.update();
            assertEquals(List.of("mount:1", "update:2"), log);
            assertEquals(2, groupRuns.get());
            assertEquals(EffectApplyStatus.ALREADY_APPLIED, runtime.applyKeyedEffects().status());
        }
    }
}

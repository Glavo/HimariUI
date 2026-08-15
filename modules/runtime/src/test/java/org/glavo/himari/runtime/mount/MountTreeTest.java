package org.glavo.himari.runtime.mount;

import org.glavo.himari.runtime.animation.AnimationPhase;
import org.glavo.himari.runtime.animation.AnimationPhaseImpact;
import org.glavo.himari.state.IntState;
import org.glavo.himari.state.StateDomain;
import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies fine-grained property bindings, phase-isolated apply, and failure-atomic commits.
@NotNullByDefault
final class MountTreeTest {
    /// Verifies that a bound source invalidates only the binding, not an unrelated property.
    @Test
    void appliesOnlyInvalidatedBindingsAndDeclaredPhases() {
        StateDomain domain = new StateDomain();
        IntState count = domain.intState(0);
        IntState unused = domain.intState(1);
        AtomicInteger labelApplies = new AtomicInteger();
        AtomicInteger enabledApplies = new AtomicInteger();
        try (MountTree tree = new MountTree(domain)) {
            tree.beginAttempt();
            tree.declare(1L, "root", "counter", element -> {
                element.bind(
                        "label",
                        String.class,
                        AnimationPhaseImpact.MEASURE,
                        () -> "Count: " + count.get(),
                        ignored -> labelApplies.incrementAndGet()
                );
                element.bind(
                        "enabled",
                        Boolean.class,
                        AnimationPhaseImpact.SEMANTICS,
                        () -> unused.get() > 0,
                        ignored -> enabledApplies.incrementAndGet()
                );
            });
            tree.commitAttempt(Set.of(1L));
            assertEquals(MountApplyStatus.COMMITTED, tree.apply().status());
            assertEquals(1, labelApplies.get());
            assertEquals(1, enabledApplies.get());

            count.set(2);
            MountApplyResult result = tree.apply();
            assertEquals(MountApplyStatus.COMMITTED, result.status());
            assertEquals(1, result.changedBindingCount());
            assertTrue(result.appliedPhases().includes(AnimationPhase.MEASURE));
            assertFalse(result.appliedPhases().includes(AnimationPhase.STRUCTURE));
            assertEquals(2, labelApplies.get());
            assertEquals(1, enabledApplies.get());
            assertEquals("Count: 2", tree.snapshot().elements().getFirst().property("label").value());
        }
    }

    /// Verifies that a failed reader leaves previously committed targets unchanged.
    @Test
    void failedApplyPreservesPreviousTargets() {
        StateDomain domain = new StateDomain();
        IntState count = domain.intState(1);
        try (MountTree tree = new MountTree(domain)) {
            tree.beginAttempt();
            tree.declare(1L, "root", "label", element -> element.bind(
                    "text",
                    String.class,
                    AnimationPhaseImpact.PAINT,
                    () -> {
                        if (count.get() < 0) {
                            throw new IllegalStateException("negative");
                        }
                        return "n=" + count.get();
                    }
            ));
            tree.commitAttempt(Set.of(1L));
            assertEquals(MountApplyStatus.COMMITTED, tree.apply().status());
            count.set(-1);
            MountApplyResult failed = tree.apply();
            assertEquals(MountApplyStatus.FAILED, failed.status());
            assertEquals("n=1", tree.snapshot().elements().getFirst().property("text").value());
        }
    }

    /// Verifies that removing a live group disposes its mounted element.
    @Test
    void removingGroupDisposesMountedElement() {
        StateDomain domain = new StateDomain();
        List<String> applied = new ArrayList<>();
        try (MountTree tree = new MountTree(domain)) {
            tree.beginAttempt();
            tree.declare(1L, "root", "button", element -> element.bind(
                    "label",
                    String.class,
                    AnimationPhaseImpact.PAINT,
                    () -> "ok",
                    applied::add
            ));
            tree.commitAttempt(Set.of(1L));
            tree.apply();
            tree.beginAttempt();
            tree.visitGroup(1L);
            tree.commitAttempt(Set.of());
            assertTrue(tree.snapshot().elements().isEmpty());
            assertEquals(List.of("ok"), applied);
        }
    }
}

package org.glavo.himari.runtime.structure;

import org.glavo.himari.state.BooleanState;
import org.glavo.himari.state.IntState;
import org.glavo.himari.state.MutableState;
import org.glavo.himari.state.StateDomain;
import org.glavo.himari.state.StateTransaction;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies production grouped recomposition, identity, lifecycle, materialization, and recovery.
@NotNullByDefault
final class StructuralRuntimeTest {
    /// Verifies automatic dependency capture and minimal group selection for sources and locals.
    @Test
    void rerunsOnlyInvalidatedGroupsAndTracksLocalReaders() {
        StateDomain domain = new StateDomain();
        IntState left = domain.intState(1);
        IntState right = domain.intState(10);
        AtomicInteger rootRuns = new AtomicInteger();
        AtomicInteger leftRuns = new AtomicInteger();
        AtomicInteger rightRuns = new AtomicInteger();
        AtomicReference<@Nullable StructuralLocal<Integer>> localReference = new AtomicReference<>();

        try (StructuralRuntime runtime = new StructuralRuntime(domain, scope -> {
            rootRuns.incrementAndGet();
            scope.group("left", child -> {
                leftRuns.incrementAndGet();
                left.get();
                StructuralLocal<Integer> local = child.rememberLocal(Integer.class, 0);
                local.get();
                localReference.set(local);
            });
            scope.group("right", child -> {
                rightRuns.incrementAndGet();
                right.get();
            });
        })) {
            assertEquals(StructuralAttemptStatus.COMMITTED, runtime.update().status());
            assertEquals(List.of(1, 1, 1), List.of(rootRuns.get(), leftRuns.get(), rightRuns.get()));
            assertEquals(StructuralAttemptStatus.NO_CHANGES, runtime.update().status());

            left.set(2);
            StructuralAttemptResult leftResult = runtime.update();
            assertEquals(StructuralAttemptStatus.COMMITTED, leftResult.status());
            assertEquals(1, leftResult.attemptedGroupCount());
            assertEquals(List.of(1, 2, 1), List.of(rootRuns.get(), leftRuns.get(), rightRuns.get()));

            StructuralLocal<Integer> local = Objects.requireNonNull(localReference.get());
            local.set(7);
            runtime.update();
            assertEquals(List.of(1, 3, 1), List.of(rootRuns.get(), leftRuns.get(), rightRuns.get()));
            assertThrows(IllegalStateException.class, () -> StateTransaction.run(domain, () -> local.set(8)));
            assertEquals(7, local.get());

            right.set(11);
            runtime.update();
            assertEquals(List.of(1, 3, 2), List.of(rootRuns.get(), leftRuns.get(), rightRuns.get()));
        }
    }

    /// Verifies semantic-key reorder, positional-memory survival, and duplicate-draft rollback.
    @Test
    void preservesKeyedIdentityAndRejectsDuplicateDraft() {
        StateDomain domain = new StateDomain();
        MutableState<List<Integer>> order = domain.mutableState(List.of(1, 2, 3));
        StructuralRuntime runtime = new StructuralRuntime(domain, scope -> {
            for (int key : order.get()) {
                scope.keyedGroup("item", key, child -> child.remember(String.class, () -> "item-" + key));
            }
        });
        try (runtime) {
            runtime.update();
            Map<String, IdentitySnapshot> first = keyedIdentities(runtime.snapshot(), "item");

            order.set(List.of(3, 1, 2));
            runtime.update();
            Map<String, IdentitySnapshot> reordered = keyedIdentities(runtime.snapshot(), "item");
            assertEquals(List.of("3", "1", "2"), List.copyOf(reordered.keySet()));
            assertEquals(first, reordered);

            long revisionBeforeFailure = runtime.revision();
            order.set(List.of(1, 1));
            StructuralAttemptResult failure = runtime.update();
            assertEquals(StructuralAttemptStatus.ROOT_FAILED, failure.status());
            assertEquals(revisionBeforeFailure, runtime.revision());
            assertEquals(reordered, keyedIdentities(runtime.snapshot(), "item"));
            StructuralFailure duplicateFailure = failure.failure();
            assertNotNull(duplicateFailure);
            assertEquals("duplicate-semantic-key", duplicateFailure.code());

            order.set(List.of(2, 3));
            runtime.resetRoot();
            assertEquals(StructuralAttemptStatus.COMMITTED, runtime.update().status());
            assertEquals(List.of("2", "3"), List.copyOf(keyedIdentities(runtime.snapshot(), "item").keySet()));
        }
    }

    /// Verifies retain-on-hide deactivation and later dispose-on-hide teardown.
    @Test
    void retainsDormantMemoryButDeactivatesEffectsAndDependencies() {
        StateDomain domain = new StateDomain();
        BooleanState visible = domain.booleanState(true);
        BooleanState retain = domain.booleanState(true);
        IntState branchInput = domain.intState(1);
        AtomicInteger branchRuns = new AtomicInteger();
        AtomicInteger effectMounts = new AtomicInteger();
        AtomicInteger effectCleanups = new AtomicInteger();
        AtomicInteger resourceCleanups = new AtomicInteger();
        AtomicReference<@Nullable StructuralLocal<Integer>> localReference = new AtomicReference<>();
        AtomicReference<@Nullable Object> resourceReference = new AtomicReference<>();

        try (StructuralRuntime runtime = new StructuralRuntime(domain, scope -> scope.branch(
                "details",
                visible.get(),
                retain.get() ? BranchRetention.RETAIN : BranchRetention.DISPOSE,
                branch -> {
                    branchRuns.incrementAndGet();
                    branchInput.get();
                    StructuralLocal<Integer> local = branch.rememberLocal(Integer.class, 0);
                    Object resource = branch.rememberResource(
                            Object.class,
                            Object::new,
                            ignored -> resourceCleanups.incrementAndGet()
                    );
                    local.get();
                    localReference.set(local);
                    resourceReference.set(resource);
                    branch.effect(
                            "active",
                            effectMounts::incrementAndGet,
                            effectCleanups::incrementAndGet
                    );
                }
        ))) {
            runtime.update();
            StructuralLocal<Integer> local = Objects.requireNonNull(localReference.get());
            Object resource = Objects.requireNonNull(resourceReference.get());
            local.set(9);
            runtime.update();
            assertEquals(2, branchRuns.get());
            assertEquals(1, effectMounts.get());

            visible.set(false);
            runtime.update();
            assertEquals(1, effectCleanups.get());
            assertEquals(0, resourceCleanups.get());
            assertFalse(local.isDisposed());
            assertEquals(StructuralGroupState.DORMANT, findGroup(runtime.snapshot(), "details").state());

            branchInput.set(2);
            assertEquals(StructuralAttemptStatus.NO_CHANGES, runtime.update().status());

            visible.set(true);
            runtime.update();
            assertSame(local, localReference.get());
            assertSame(resource, resourceReference.get());
            assertEquals(2, effectMounts.get());
            assertEquals(9, local.get());

            retain.set(false);
            visible.set(false);
            runtime.update();
            assertTrue(local.isDisposed());
            assertEquals(2, effectCleanups.get());
            assertEquals(1, resourceCleanups.get());
        }
    }

    /// Verifies nearest-boundary fallback, explicit retry, and state-write rejection.
    @Test
    void containsNormalFailuresAndRequiresExplicitReset() {
        StateDomain domain = new StateDomain();
        BooleanState fail = domain.booleanState(false);
        IntState protectedState = domain.intState(1);
        ErrorBoundaryKey boundaryKey = ErrorBoundaryKey.create("screen");
        AtomicInteger normalRuns = new AtomicInteger();
        AtomicInteger fallbackRuns = new AtomicInteger();

        try (StructuralRuntime runtime = new StructuralRuntime(domain, scope -> scope.errorBoundary(
                "screen-boundary",
                boundaryKey,
                content -> {
                    normalRuns.incrementAndGet();
                    if (fail.get()) {
                        protectedState.set(2);
                    }
                },
                fallback -> fallbackRuns.incrementAndGet()
        ))) {
            runtime.update();
            fail.set(true);
            long revisionBeforeFailure = runtime.revision();
            StructuralAttemptResult result = runtime.update();
            assertEquals(StructuralAttemptStatus.CONTAINED_FAILURE, result.status());
            assertEquals(revisionBeforeFailure + 1L, runtime.revision());
            assertEquals(ErrorBoundaryStatus.FAILED, runtime.boundaryStatus(boundaryKey));
            assertEquals(1, protectedState.get());
            assertEquals(1, fallbackRuns.get());
            assertNotNull(result.failure());

            fail.set(false);
            assertEquals(StructuralAttemptStatus.NO_CHANGES, runtime.update().status());
            assertTrue(runtime.resetBoundary(boundaryKey));
            runtime.update();
            assertEquals(ErrorBoundaryStatus.HEALTHY, runtime.boundaryStatus(boundaryKey));
            assertEquals(3, normalRuns.get());
        }
    }

    /// Verifies that a failed fallback escalates exactly once to its declared parent boundary.
    @Test
    void escalatesFailedFallbackToParentBoundary() {
        StateDomain domain = new StateDomain();
        IntState mode = domain.intState(0);
        ErrorBoundaryKey outerKey = ErrorBoundaryKey.create("outer");
        ErrorBoundaryKey innerKey = ErrorBoundaryKey.create("inner");
        AtomicInteger outerFallbackRuns = new AtomicInteger();

        try (StructuralRuntime runtime = new StructuralRuntime(domain, scope -> scope.errorBoundary(
                "outer-boundary",
                outerKey,
                outer -> outer.errorBoundary(
                        "inner-boundary",
                        innerKey,
                        inner -> {
                            if (mode.get() == 1) {
                                inner.fail("inner-normal-failed");
                            }
                        },
                        fallback -> fallback.fail("inner-fallback-failed")
                ),
                fallback -> outerFallbackRuns.incrementAndGet()
        ))) {
            runtime.update();
            mode.set(1);
            StructuralAttemptResult result = runtime.update();
            assertEquals(StructuralAttemptStatus.CONTAINED_FAILURE, result.status());
            assertEquals(ErrorBoundaryStatus.ESCALATED, runtime.boundaryStatus(innerKey));
            assertEquals(ErrorBoundaryStatus.FAILED, runtime.boundaryStatus(outerKey));
            assertEquals(1, outerFallbackRuns.get());
            assertEquals(
                    List.of("inner-normal-failed", "inner-fallback-failed"),
                    runtime.drainFailures().stream().map(StructuralFailure::code).toList()
            );

            mode.set(0);
            assertTrue(runtime.resetBoundary(innerKey));
            StructuralAttemptResult recovery = runtime.update();
            assertEquals(StructuralAttemptStatus.COMMITTED, recovery.status());
            assertEquals(
                    ErrorBoundaryStatus.HEALTHY,
                    findGroup(runtime.snapshot(), "inner-boundary").boundaryStatus()
            );
            assertEquals(ErrorBoundaryStatus.HEALTHY, runtime.boundaryStatus(innerKey));
            assertEquals(ErrorBoundaryStatus.HEALTHY, runtime.boundaryStatus(outerKey));
        }
    }

    /// Verifies inherited defaults, nested override shadowing, and provider-value publication.
    @Test
    void resolvesAmbientOverridesWithIdentityShadowing() {
        StateDomain domain = new StateDomain();
        MutableState<String> outerValue = domain.mutableState("outer-1");
        AmbientKey<String> key = AmbientKey.of("theme", String.class, "default");
        AtomicReference<String> outsideRead = new AtomicReference<>("");
        AtomicReference<String> outerRead = new AtomicReference<>("");
        AtomicReference<String> innerRead = new AtomicReference<>("");

        try (StructuralRuntime runtime = new StructuralRuntime(domain, scope -> {
            outsideRead.set(scope.ambient(key));
            scope.provideAmbient("outer-provider", key, outerValue.get(), outer -> {
                outer.group("outer-reader", reader -> outerRead.set(reader.ambient(key)));
                outer.provideAmbient("inner-provider", key, "inner", inner ->
                        inner.group("inner-reader", reader -> innerRead.set(reader.ambient(key)))
                );
            });
        })) {
            runtime.update();
            assertEquals("default", outsideRead.get());
            assertEquals("outer-1", outerRead.get());
            assertEquals("inner", innerRead.get());

            outerValue.set("outer-2");
            runtime.update();
            assertEquals("outer-2", outerRead.get());
            assertEquals("inner", innerRead.get());
        }
    }

    /// Verifies that effect-mount failure discards staged resources before boundary fallback.
    @Test
    void rollsBackResourcesAndEffectsWhenMountFails() {
        StateDomain domain = new StateDomain();
        BooleanState failMount = domain.booleanState(false);
        ErrorBoundaryKey boundaryKey = ErrorBoundaryKey.create("effect-boundary");
        AtomicInteger stagedResources = new AtomicInteger();
        AtomicInteger stagedResourceCleanups = new AtomicInteger();
        AtomicInteger stagedEffectMounts = new AtomicInteger();
        AtomicInteger stagedEffectCleanups = new AtomicInteger();
        AtomicInteger stableEffectCleanups = new AtomicInteger();

        try (StructuralRuntime runtime = new StructuralRuntime(domain, scope -> scope.errorBoundary(
                "effect-boundary",
                boundaryKey,
                content -> {
                    boolean shouldFail = failMount.get();
                    content.group("stable", stable -> stable.effect(
                            "stable",
                            () -> { },
                            stableEffectCleanups::incrementAndGet
                    ));
                    if (shouldFail) {
                        content.group("staged", staged -> {
                            staged.rememberResource(
                                    Object.class,
                                    () -> {
                                        stagedResources.incrementAndGet();
                                        return new Object();
                                    },
                                    ignored -> stagedResourceCleanups.incrementAndGet()
                            );
                            staged.effect("failing", () -> {
                                stagedEffectMounts.incrementAndGet();
                                throw new IllegalStateException("planned mount failure");
                            }, stagedEffectCleanups::incrementAndGet);
                        });
                    }
                },
                fallback -> { }
        ))) {
            runtime.update();
            failMount.set(true);
            StructuralAttemptResult result = runtime.update();

            assertEquals(StructuralAttemptStatus.CONTAINED_FAILURE, result.status());
            assertEquals(StructuralCallbackPhase.EFFECT_MOUNT, Objects.requireNonNull(result.failure()).phase());
            assertEquals(1, stagedResources.get());
            assertEquals(1, stagedResourceCleanups.get());
            assertEquals(1, stagedEffectMounts.get());
            assertEquals(1, stagedEffectCleanups.get());
            assertEquals(1, stableEffectCleanups.get());
            assertFalse(runtime.snapshot().groups().stream()
                    .anyMatch(group -> group.sourceIdentity().equals("staged")));
        }
    }

    /// Verifies callback-scope lifetime and non-reentrant runtime mutation.
    @Test
    void rejectsStaleScopesAndReentrantUpdates() {
        StateDomain domain = new StateDomain();
        ErrorBoundaryKey boundaryKey = ErrorBoundaryKey.create("reentrant");
        AtomicReference<@Nullable StructuralScope> leakedScope = new AtomicReference<>();
        AtomicReference<@Nullable StructuralRuntime> runtimeReference = new AtomicReference<>();
        AtomicInteger fallbackRuns = new AtomicInteger();
        StructuralRuntime runtime = new StructuralRuntime(domain, scope -> {
            leakedScope.set(scope);
            scope.errorBoundary("reentrant-boundary", boundaryKey, content -> {
                StructuralRuntime current = runtimeReference.get();
                if (current != null) {
                    current.update();
                }
            }, fallback -> fallbackRuns.incrementAndGet());
        });
        runtimeReference.set(runtime);
        try (runtime) {
            StructuralAttemptResult result = runtime.update();
            assertEquals(StructuralAttemptStatus.CONTAINED_FAILURE, result.status());
            assertEquals(1, fallbackRuns.get());
            StructuralScope stale = Objects.requireNonNull(leakedScope.get());
            assertThrows(IllegalStateException.class, () -> stale.group("late", ignored -> { }));
        }
    }

    /// Verifies current-input keyed materialization, survivor identity, cancellation, and rollback.
    @Test
    void materializesCurrentViewportAtomically() {
        StateDomain domain = new StateDomain();
        MeasureMaterializationKey<Viewport> key = MeasureMaterializationKey.create("viewport", Viewport.class);
        AtomicReference<@Nullable StructuralLocal<Integer>> measureDependency = new AtomicReference<>();
        StructuralRuntime runtime = new StructuralRuntime(domain, scope -> scope.group(
                "lazy-owner",
                owner -> {
                    StructuralLocal<Integer> dependency = owner.rememberLocal(Integer.class, 0);
                    measureDependency.set(dependency);
                    owner.measureGroup("lazy-items", key, (measure, viewport) -> {
                        Objects.requireNonNull(measureDependency.get()).get();
                        for (int item : viewport.keys()) {
                            measure.checkpoint();
                            measure.keyedGroup(
                                    "visible-item",
                                    item,
                                    child -> child.rememberLocal(Integer.class, item)
                            );
                        }
                    });
                }
        ));
        try (runtime) {
            runtime.update();
            assertTrue(runtime.needsMaterialization(key));
            runtime.materialize(key, new Viewport(List.of(1, 2, 3)));
            assertFalse(runtime.needsMaterialization(key));
            Map<String, IdentitySnapshot> first = keyedIdentities(runtime.snapshot(), "visible-item");

            Objects.requireNonNull(measureDependency.get()).set(1);
            assertTrue(runtime.needsMaterialization(key));
            runtime.materialize(key, new Viewport(List.of(3, 2, 4)));
            assertFalse(runtime.needsMaterialization(key));
            Map<String, IdentitySnapshot> second = keyedIdentities(runtime.snapshot(), "visible-item");
            assertEquals(first.get("2"), second.get("2"));
            assertEquals(first.get("3"), second.get("3"));
            assertFalse(second.containsKey("1"));

            StructuralCancellation cancellation = new StructuralCancellation();
            cancellation.cancel();
            long revisionBeforeCancellation = runtime.revision();
            StructuralAttemptResult cancelled = runtime.materialize(
                    key,
                    new Viewport(List.of(5, 6)),
                    cancellation
            );
            assertEquals(StructuralAttemptStatus.CANCELLED, cancelled.status());
            assertEquals(revisionBeforeCancellation, runtime.revision());
            assertEquals(second, keyedIdentities(runtime.snapshot(), "visible-item"));

            StructuralAttemptResult failed = runtime.materialize(key, new Viewport(List.of(3, 3)));
            assertEquals(StructuralAttemptStatus.ROOT_FAILED, failed.status());
            assertEquals(revisionBeforeCancellation, runtime.revision());
            assertEquals(second, keyedIdentities(runtime.snapshot(), "visible-item"));

            runtime.resetRoot();
            runtime.update();
            assertTrue(runtime.needsMaterialization(key));
            runtime.materialize(key, new Viewport(List.of(2, 5)));
            assertEquals(List.of("2", "5"), List.copyOf(keyedIdentities(
                    runtime.snapshot(),
                    "visible-item"
            ).keySet()));
        }
    }

    /// Verifies the configured direct-child budget rejects a complete measure draft atomically.
    @Test
    void enforcesCurrentMeasureChildBudget() {
        StateDomain domain = new StateDomain();
        MeasureMaterializationKey<Viewport> key = MeasureMaterializationKey.create("bounded", Viewport.class);
        StructuralRuntimeConfig config = new StructuralRuntimeConfig(StructuralDiagnosticsMode.DEBUG, 8, 2);
        StructuralRuntime runtime = new StructuralRuntime(domain, config, scope -> scope.measureGroup(
                "bounded-items",
                key,
                (measure, viewport) -> {
                    for (int item : viewport.keys()) {
                        measure.keyedGroup("bounded-item", item, ignored -> { });
                    }
                }
        ));
        try (runtime) {
            runtime.update();
            runtime.materialize(key, new Viewport(List.of(1, 2)));
            StructuralSnapshot committed = runtime.snapshot();

            StructuralAttemptResult result = runtime.materialize(key, new Viewport(List.of(1, 2, 3)));
            assertEquals(StructuralAttemptStatus.ROOT_FAILED, result.status());
            assertEquals("materialization-child-budget-exceeded", Objects.requireNonNull(result.failure()).code());
            assertEquals(committed.revision(), runtime.revision());
            assertEquals(
                    keyedIdentities(committed, "bounded-item"),
                    keyedIdentities(runtime.snapshot(), "bounded-item")
            );
        }
    }

    /// Verifies release diagnostics redact causes without changing containment semantics.
    @Test
    void redactsReleaseFailureCauses() {
        StateDomain domain = new StateDomain();
        StructuralRuntimeConfig config = new StructuralRuntimeConfig(
                StructuralDiagnosticsMode.RELEASE,
                8,
                8
        );
        try (StructuralRuntime runtime = new StructuralRuntime(domain, config, scope -> {
            throw new IllegalArgumentException("sensitive detail");
        })) {
            StructuralAttemptResult result = runtime.update();
            assertEquals(StructuralAttemptStatus.ROOT_FAILED, result.status());
            StructuralFailure failure = result.failure();
            assertNotNull(failure);
            assertNull(failure.cause());
            assertEquals("structure-callback-failed", failure.code());
        }
    }

    /// Verifies child-before-parent cleanup and failure aggregation during shutdown.
    @Test
    void aggregatesCleanupFailuresWithoutSkippingOwnedCleanup() {
        StateDomain domain = new StateDomain();
        ArrayList<String> cleanupOrder = new ArrayList<>();
        StructuralRuntime runtime = new StructuralRuntime(domain, scope -> {
            scope.rememberResource(
                    Object.class,
                    Object::new,
                    ignored -> failCleanup(cleanupOrder, "parent-resource")
            );
            scope.effect("parent", () -> { }, () -> failCleanup(cleanupOrder, "parent-effect"));
            scope.group("child", child -> {
                child.rememberResource(
                        Object.class,
                        Object::new,
                        ignored -> failCleanup(cleanupOrder, "child-resource")
                );
                child.effect("child", () -> { }, () -> failCleanup(cleanupOrder, "child-effect"));
            });
        });
        runtime.update();
        runtime.close();

        assertEquals(
                List.of("child-effect", "child-resource", "parent-effect", "parent-resource"),
                cleanupOrder
        );
        List<StructuralFailure> failures = runtime.drainFailures();
        assertEquals(1, failures.size());
        assertEquals(4, failures.getFirst().cleanupFailures().size());
        assertEquals(StructuralRuntimeStatus.CLOSED, runtime.status());
    }

    /// Finds one group by source identity.
    private static StructuralGroupSnapshot findGroup(StructuralSnapshot snapshot, String sourceIdentity) {
        for (StructuralGroupSnapshot group : snapshot.groups()) {
            if (group.sourceIdentity().equals(sourceIdentity)) {
                return group;
            }
        }
        throw new AssertionError("Group not found: " + sourceIdentity);
    }

    /// Returns keyed identity and first-memory identity in committed traversal order.
    private static Map<String, IdentitySnapshot> keyedIdentities(
            StructuralSnapshot snapshot,
            String sourceIdentity
    ) {
        LinkedHashMap<String, IdentitySnapshot> identities = new LinkedHashMap<>();
        for (StructuralGroupSnapshot group : snapshot.groups()) {
            if (group.sourceIdentity().equals(sourceIdentity) && group.semanticKey() != null) {
                long memoryId = group.rememberedSlotIds().isEmpty()
                        ? 0L
                        : group.rememberedSlotIds().getFirst();
                identities.put(group.semanticKey(), new IdentitySnapshot(group.groupId(), memoryId));
            }
        }
        return identities;
    }

    /// Records and throws one deterministic cleanup failure.
    private static void failCleanup(ArrayList<String> order, String name) {
        order.add(name);
        throw new IllegalStateException(name);
    }

    /// Captures stable group and positional-memory identities.
    ///
    /// @param groupId the group identity
    /// @param memoryId the first remembered-slot identity, or zero when absent
    @NotNullByDefault
    private record IdentitySnapshot(long groupId, long memoryId) {
        /// Validates stable nonnegative identities.
        private IdentitySnapshot {
            if (groupId < 1L || memoryId < 0L) {
                throw new IllegalArgumentException("Identity snapshot values are invalid");
            }
        }
    }

    /// Supplies one immutable current-measure viewport.
    ///
    /// @param keys immutable visible semantic keys in placement order
    @NotNullByDefault
    private record Viewport(@Unmodifiable List<Integer> keys) {
        /// Snapshots the visible keys.
        private Viewport {
            keys = List.copyOf(keys);
        }
    }
}

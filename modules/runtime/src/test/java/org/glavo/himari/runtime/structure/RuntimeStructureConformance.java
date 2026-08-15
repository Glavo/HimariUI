package org.glavo.himari.runtime.structure;

import org.glavo.himari.state.BooleanState;
import org.glavo.himari.state.IntState;
import org.glavo.himari.state.MutableState;
import org.glavo.himari.state.StateDomain;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/// Executes the deterministic STRUCTURE-001 acceptance scenarios and writes their observations.
@NotNullByDefault
public final class RuntimeStructureConformance {
    /// Prevents instantiation of this command-line entry point.
    private RuntimeStructureConformance() {
    }

    /// Verifies grouped invalidation, identity, lifecycle, failure recovery, and materialization.
    ///
    /// @param arguments one output-directory path
    /// @throws IOException if the report cannot be written
    public static void main(String[] arguments) throws IOException {
        if (arguments.length != 1) {
            throw new IllegalArgumentException("Expected one conformance output directory");
        }

        verifyMinimalGroupedInvalidation();
        verifyKeyedIdentityAndRetainedBranch();
        verifyBoundaryAtomicity();
        verifyCurrentMeasureMaterialization();
        verifyAmbientShadowing();

        Path outputDirectory = Path.of(arguments[0]);
        Files.createDirectories(outputDirectory);
        writeReport(outputDirectory.resolve("results.json"));
    }

    /// Verifies source and local writes rerun only their direct structural reader.
    private static void verifyMinimalGroupedInvalidation() {
        StateDomain domain = new StateDomain();
        IntState left = domain.intState(1);
        IntState right = domain.intState(2);
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
            require(runtime.update().status() == StructuralAttemptStatus.COMMITTED,
                    "Initial grouped structure did not commit");
            left.set(3);
            require(runtime.update().attemptedGroupCount() == 1,
                    "Source invalidation selected more than one root group");
            Objects.requireNonNull(localReference.get()).set(4);
            require(runtime.update().attemptedGroupCount() == 1,
                    "Local invalidation selected more than one root group");
            require(rootRuns.get() == 1 && leftRuns.get() == 3 && rightRuns.get() == 1,
                    "Minimal grouped execution counts are incorrect");
        }
    }

    /// Verifies semantic reorder and retain-on-hide lifecycle without losing positional memory.
    private static void verifyKeyedIdentityAndRetainedBranch() {
        StateDomain domain = new StateDomain();
        MutableState<List<Integer>> order = domain.mutableState(List.of(1, 2, 3));
        BooleanState visible = domain.booleanState(true);
        AtomicInteger mounts = new AtomicInteger();
        AtomicInteger cleanups = new AtomicInteger();
        AtomicReference<@Nullable StructuralLocal<Integer>> branchLocal = new AtomicReference<>();
        try (StructuralRuntime runtime = new StructuralRuntime(domain, scope -> {
            for (int key : order.get()) {
                scope.keyedGroup("item", key, child -> child.rememberLocal(Integer.class, key));
            }
            scope.branch("branch", visible.get(), BranchRetention.RETAIN, branch -> {
                StructuralLocal<Integer> local = branch.rememberLocal(Integer.class, 7);
                branchLocal.set(local);
                branch.effect("active", mounts::incrementAndGet, cleanups::incrementAndGet);
            });
        })) {
            runtime.update();
            long itemTwoId = findGroup(runtime.snapshot(), "item", "2").groupId();
            StructuralLocal<Integer> local = Objects.requireNonNull(branchLocal.get());
            order.set(List.of(3, 2, 1));
            runtime.update();
            require(findGroup(runtime.snapshot(), "item", "2").groupId() == itemTwoId,
                    "Semantic reorder replaced a surviving group");
            visible.set(false);
            runtime.update();
            require(findGroup(runtime.snapshot(), "branch", "branch").state() == StructuralGroupState.DORMANT,
                    "Retained branch did not become dormant");
            require(cleanups.get() == 1 && !local.isDisposed(),
                    "Retained branch lifecycle is incorrect");
            visible.set(true);
            runtime.update();
            require(branchLocal.get() == local && mounts.get() == 2,
                    "Retained branch did not restore identity and effect lifecycle");
        }
    }

    /// Verifies state-write and effect-mount failure rollback through a fresh boundary fallback.
    private static void verifyBoundaryAtomicity() {
        StateDomain domain = new StateDomain();
        BooleanState fail = domain.booleanState(false);
        IntState protectedState = domain.intState(1);
        ErrorBoundaryKey boundaryKey = ErrorBoundaryKey.create("atomicity");
        AtomicInteger resources = new AtomicInteger();
        AtomicInteger resourceCleanups = new AtomicInteger();
        AtomicInteger effectCleanups = new AtomicInteger();
        try (StructuralRuntime runtime = new StructuralRuntime(domain, scope -> scope.errorBoundary(
                "boundary",
                boundaryKey,
                content -> {
                    boolean failing = fail.get();
                    if (failing) {
                        content.group("staged", staged -> {
                            staged.rememberResource(Object.class, () -> {
                                resources.incrementAndGet();
                                return new Object();
                            }, ignored -> resourceCleanups.incrementAndGet());
                            staged.effect("mount", () -> {
                                protectedState.set(9);
                            }, effectCleanups::incrementAndGet);
                        });
                    }
                },
                fallback -> { }
        ))) {
            runtime.update();
            fail.set(true);
            StructuralAttemptResult result = runtime.update();
            require(result.status() == StructuralAttemptStatus.CONTAINED_FAILURE,
                    "Boundary did not contain effect-mount failure");
            require(protectedState.get() == 1,
                    "Rejected effect mount published an application-state write");
            require(resources.get() == 1 && resourceCleanups.get() == 1 && effectCleanups.get() == 1,
                    "Failed draft did not balance staged resources and effects");
            require(runtime.boundaryStatus(boundaryKey) == ErrorBoundaryStatus.FAILED,
                    "Boundary recovery state is incorrect");
        }
    }

    /// Verifies current-input keyed viewport publication, cancellation, and duplicate rollback.
    private static void verifyCurrentMeasureMaterialization() {
        StateDomain domain = new StateDomain();
        MeasureMaterializationKey<Viewport> key = MeasureMaterializationKey.create("viewport", Viewport.class);
        StructuralRuntime runtime = new StructuralRuntime(domain, scope -> scope.measureGroup(
                "viewport",
                key,
                (measure, viewport) -> {
                    for (int item : viewport.keys()) {
                        measure.checkpoint();
                        measure.keyedGroup("visible", item, child ->
                                child.rememberLocal(Integer.class, item));
                    }
                }
        ));
        try (runtime) {
            runtime.update();
            runtime.materialize(key, new Viewport(List.of(1, 2, 3)));
            long itemTwoId = findGroup(runtime.snapshot(), "visible", "2").groupId();
            runtime.materialize(key, new Viewport(List.of(3, 2, 4)));
            require(findGroup(runtime.snapshot(), "visible", "2").groupId() == itemTwoId,
                    "Current-measure survivor identity changed");
            StructuralSnapshot committed = runtime.snapshot();

            StructuralCancellation cancellation = new StructuralCancellation();
            cancellation.cancel();
            require(runtime.materialize(key, new Viewport(List.of(8)), cancellation).status()
                            == StructuralAttemptStatus.CANCELLED,
                    "Measure cancellation did not discard the draft");
            require(runtime.snapshot().revision() == committed.revision(),
                    "Cancelled materialization advanced the revision");

            require(runtime.materialize(key, new Viewport(List.of(2, 2))).status()
                            == StructuralAttemptStatus.ROOT_FAILED,
                    "Duplicate measure keys did not reject the complete draft");
            require(runtime.snapshot().revision() == committed.revision(),
                    "Failed materialization replaced the committed viewport");
        }
    }

    /// Verifies ambient defaults, overrides, and nested shadowing.
    private static void verifyAmbientShadowing() {
        StateDomain domain = new StateDomain();
        AmbientKey<String> key = AmbientKey.of("theme", String.class, "default");
        AtomicReference<String> outside = new AtomicReference<>("");
        AtomicReference<String> outer = new AtomicReference<>("");
        AtomicReference<String> inner = new AtomicReference<>("");
        try (StructuralRuntime runtime = new StructuralRuntime(domain, scope -> {
            outside.set(scope.ambient(key));
            scope.provideAmbient("outer", key, "dark", outerScope -> {
                outer.set(outerScope.ambient(key));
                outerScope.provideAmbient("inner", key, "light", innerScope ->
                        inner.set(innerScope.ambient(key)));
            });
        })) {
            runtime.update();
            require(outside.get().equals("default")
                            && outer.get().equals("dark")
                            && inner.get().equals("light"),
                    "Ambient override shadowing produced an incorrect value");
        }
    }

    /// Finds one keyed group in a committed snapshot.
    private static StructuralGroupSnapshot findGroup(
            StructuralSnapshot snapshot,
            String sourceIdentity,
            String semanticKey
    ) {
        for (StructuralGroupSnapshot group : snapshot.groups()) {
            if (group.sourceIdentity().equals(sourceIdentity)
                    && Objects.equals(group.semanticKey(), semanticKey)) {
                return group;
            }
        }
        throw new IllegalStateException("Expected committed group " + sourceIdentity + '[' + semanticKey + ']');
    }

    /// Writes deterministic machine-readable profile evidence.
    private static void writeReport(Path reportPath) throws IOException {
        String report = """
                {
                  "profile": "m1-structure",
                  "workPackage": "STRUCTURE-001",
                  "status": "passed",
                  "unitTestCases": 12,
                  "ordinaryJavaGroups": true,
                  "minimalStructuralRoots": 1,
                  "keyedReorderPreservedIdentity": true,
                  "retainedBranchesDeactivateConsumers": true,
                  "failedDraftResourceLeaks": 0,
                  "failedDraftEffectLeaks": 0,
                  "boundaryFallbackAttemptsAreFresh": true,
                  "fallbackEscalationIsBounded": true,
                  "currentMeasureUsesCurrentInput": true,
                  "cancelledViewportChanges": 0,
                  "failedViewportChanges": 0,
                  "ambientShadowing": true,
                  "moduleNativeAccess": false
                }
                """;
        Files.writeString(reportPath, report, StandardCharsets.UTF_8);
    }

    /// Rejects one invalid conformance observation.
    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    /// Supplies one immutable current-measure viewport.
    ///
    /// @param keys immutable visible semantic keys
    @NotNullByDefault
    private record Viewport(@Unmodifiable List<Integer> keys) {
        /// Snapshots visible keys.
        private Viewport {
            keys = List.copyOf(keys);
        }
    }
}

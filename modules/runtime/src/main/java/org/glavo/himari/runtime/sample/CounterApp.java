package org.glavo.himari.runtime.sample;

import org.glavo.himari.runtime.animation.AnimationPhaseImpact;
import org.glavo.himari.runtime.effect.EffectCallbacks;
import org.glavo.himari.runtime.effect.EffectDependencies;
import org.glavo.himari.runtime.effect.EffectSession;
import org.glavo.himari.runtime.mount.MountApplyResult;
import org.glavo.himari.runtime.structure.StructuralAttemptResult;
import org.glavo.himari.runtime.structure.StructuralRuntime;
import org.glavo.himari.runtime.trace.RuntimeTrace;
import org.glavo.himari.runtime.trace.TraceEventKind;
import org.glavo.himari.state.IntState;
import org.glavo.himari.state.StateDomain;
import org.jetbrains.annotations.NotNullByDefault;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/// Runs the deterministic Headless counter sample required by `SAMPLE-001`.
///
/// The sample uses the selected grouped structural runtime, independent property bindings, a keyed
/// post-commit effect, and the runtime trace. It loads no native library.
@NotNullByDefault
public final class CounterApp {
    /// Prevents instantiation of this command-line entry point.
    private CounterApp() {
    }

    /// Increments the counter from injected activations and writes the sample snapshot.
    ///
    /// @param arguments optional output directory, defaulting to `build/conformance/m1-sample`
    /// @throws IOException if the snapshot cannot be written
    public static void main(String[] arguments) throws IOException {
        Path outputDirectory = arguments.length == 0
                ? Path.of("build/conformance/m1-sample")
                : Path.of(arguments[0]);
        Files.createDirectories(outputDirectory);
        SampleSnapshot snapshot = run(2);
        Files.writeString(outputDirectory.resolve("counter.json"), snapshot.toJson(), StandardCharsets.UTF_8);
        Files.writeString(outputDirectory.resolve("trace.json"), snapshot.traceJson(), StandardCharsets.UTF_8);
        System.out.println("SAMPLE-001 CounterApp count=" + snapshot.count() + " label=" + snapshot.label());
    }

    /// Executes the injected click sequence and returns the deterministic snapshot.
    ///
    /// @param activations the number of injected increment actions
    /// @return the snapshot
    public static SampleSnapshot run(int activations) {
        if (activations < 0) {
            throw new IllegalArgumentException("activations must be nonnegative");
        }
        StateDomain domain = new StateDomain();
        IntState count = domain.intState(0);
        int[] effectMounts = new int[1];
        try (StructuralRuntime runtime = new StructuralRuntime(domain, scope -> {
            scope.mount("counter", element -> element.bind(
                    "label",
                    String.class,
                    AnimationPhaseImpact.MEASURE,
                    () -> "Count: " + count.get()
            ));
            scope.keyedEffect("announce", EffectDependencies.of(count.get()), new EffectCallbacks() {
                @Override
                public void onMount(EffectSession session) {
                    effectMounts[0]++;
                }

                @Override
                public void onUpdate(EffectSession session) {
                    effectMounts[0]++;
                }

                @Override
                public void onCleanup() {
                }
            });
        })) {
            StructuralAttemptResult initial = runtime.update();
            runtime.applyMountedProperties();
            runtime.trace().record(0L, TraceEventKind.STATE_EPOCH, "root", "epoch=" + domain.epoch());
            for (int index = 0; index < activations; index++) {
                count.set(count.get() + 1);
                runtime.update();
                runtime.applyMountedProperties();
            }
            MountApplyResult mounts = runtime.applyMountedProperties();
            RuntimeTrace trace = runtime.trace();
            String label = String.valueOf(
                    runtime.mounts().snapshot().elements().getFirst().property("label").value()
            );
            return new SampleSnapshot(
                    count.get(),
                    label,
                    initial.status().name(),
                    mounts.status().name(),
                    effectMounts[0],
                    runtime.revision(),
                    trace.toCanonicalJson()
            );
        }
    }

    /// Stores one deterministic CounterApp observation.
    ///
    /// @param count the committed counter
    /// @param label the mounted label
    /// @param structureStatus the initial structural attempt status
    /// @param mountStatus the latest mount apply status
    /// @param effectLifecycleCount mount-plus-update callbacks
    /// @param revision the structural revision
    /// @param traceJson the canonical runtime trace
    @NotNullByDefault
    public record SampleSnapshot(
            int count,
            String label,
            String structureStatus,
            String mountStatus,
            int effectLifecycleCount,
            long revision,
            String traceJson
    ) {
        /// Validates one snapshot.
        public SampleSnapshot {
            Objects.requireNonNull(label, "label");
            Objects.requireNonNull(structureStatus, "structureStatus");
            Objects.requireNonNull(mountStatus, "mountStatus");
            Objects.requireNonNull(traceJson, "traceJson");
            if (count < 0 || effectLifecycleCount < 0 || revision < 0L) {
                throw new IllegalArgumentException("Snapshot counters must be nonnegative");
            }
        }

        /// Encodes this snapshot as deterministic JSON.
        ///
        /// @return the document
        public String toJson() {
            return """
                    {
                      "profile": "m1-sample",
                      "workPackage": "SAMPLE-001",
                      "status": "passed",
                      "count": %d,
                      "label": "%s",
                      "structureStatus": "%s",
                      "mountStatus": "%s",
                      "effectLifecycleCount": %d,
                      "revision": %d,
                      "nativeLibraryLoaded": false
                    }
                    """.formatted(
                    count,
                    label,
                    structureStatus,
                    mountStatus,
                    effectLifecycleCount,
                    revision
            );
        }
    }
}

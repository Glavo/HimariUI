package org.glavo.himari.runtime.mbt;

import org.glavo.himari.runtime.animation.AnimationPhaseImpact;
import org.glavo.himari.runtime.mount.MountedElement;
import org.glavo.himari.runtime.structure.BranchRetention;
import org.glavo.himari.runtime.structure.StructuralGroupSnapshot;
import org.glavo.himari.runtime.structure.StructuralRuntime;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

/// Runs randomized structural and mount operations against a recompute-everything reference.
@NotNullByDefault
public final class RuntimeModelBasedConformance {
    /// The number of randomized sequences executed by the harness.
    private static final int SEQUENCE_COUNT = 32;

    /// The number of operations in each sequence.
    private static final int OPERATIONS_PER_SEQUENCE = 48;

    /// Prevents instantiation of this command-line entry point.
    private RuntimeModelBasedConformance() {
    }

    /// Executes the differential harness and writes the observation report.
    ///
    /// @param arguments one output-directory path
    /// @throws IOException if the report cannot be written
    public static void main(String[] arguments) throws IOException {
        if (arguments.length != 1) {
            throw new IllegalArgumentException("Expected one conformance output directory");
        }
        int comparedSteps = 0;
        for (int sequence = 0; sequence < SEQUENCE_COUNT; sequence++) {
            comparedSteps += runSequence(sequence);
        }
        Path outputDirectory = Path.of(arguments[0]);
        Files.createDirectories(outputDirectory);
        Files.writeString(
                outputDirectory.resolve("results.json"),
                """
                        {
                          "profile": "m1-runtime-mbt",
                          "workPackage": "RUNTIME-MBT-001",
                          "status": "passed",
                          "sequences": %d,
                          "operationsPerSequence": %d,
                          "comparedSteps": %d,
                          "divergences": 0
                        }
                        """.formatted(SEQUENCE_COUNT, OPERATIONS_PER_SEQUENCE, comparedSteps),
                StandardCharsets.UTF_8
        );
    }

    /// Runs one deterministic sequence and returns the number of compared steps.
    ///
    /// @param sequence the sequence identity
    /// @return the compared step count
    private static int runSequence(int sequence) {
        RandomGenerator random = RandomGeneratorFactory.getDefault().create(0x48494D4152490000L + sequence);
        StateDomain domain = new StateDomain();
        MutableState<List<Integer>> order = domain.mutableState(new ArrayList<>(List.of(1, 2, 3)));
        BooleanState visible = domain.booleanState(true);
        IntState count = domain.intState(0);
        NaiveReference reference = new NaiveReference();
        try (StructuralRuntime runtime = new StructuralRuntime(domain, scope -> {
            for (int key : order.get()) {
                scope.keyedGroup("item", key, child -> child.rememberLocal(Integer.class, key));
            }
            scope.branch("badge", visible.get(), BranchRetention.RETAIN, badge -> badge.mount(
                    "label",
                    element -> element.bind(
                            "text",
                            String.class,
                            AnimationPhaseImpact.PAINT,
                            () -> "Count: " + count.get()
                    )
            ));
        })) {
            runtime.update();
            runtime.applyMountedProperties();
            LinkedHashMap<String, Long> previousIdentities = new LinkedHashMap<>();
            compare("initial", reference.snapshot(), observe(runtime), previousIdentities);
            int compared = 1;
            for (int step = 0; step < OPERATIONS_PER_SEQUENCE; step++) {
                apply(random.nextInt(4), random, domain, order, visible, count, runtime, reference);
                compare(
                        "sequence-" + sequence + "-step-" + step,
                        reference.snapshot(),
                        observe(runtime),
                        previousIdentities
                );
                compared++;
            }
            return compared;
        }
    }

    /// Applies one randomized operation to both the production runtime and the reference.
    ///
    /// @param opcode the operation selector
    /// @param random the sequence generator
    /// @param domain the production domain
    /// @param order the keyed-collection source
    /// @param visible the branch visibility source
    /// @param count the mounted-label source
    /// @param runtime the production runtime
    /// @param reference the naive evaluator
    private static void apply(
            int opcode,
            RandomGenerator random,
            StateDomain domain,
            MutableState<List<Integer>> order,
            BooleanState visible,
            IntState count,
            StructuralRuntime runtime,
            NaiveReference reference
    ) {
        switch (opcode) {
            case 0 -> {
                int next = count.get() + 1 + random.nextInt(3);
                count.set(next);
                reference.count = next;
                runtime.update();
                runtime.applyMountedProperties();
            }
            case 1 -> {
                boolean next = !visible.get();
                visible.set(next);
                reference.visible = next;
                runtime.update();
                runtime.applyMountedProperties();
            }
            case 2 -> {
                ArrayList<Integer> next = new ArrayList<>(order.get());
                if (next.size() >= 2) {
                    int index = random.nextInt(next.size() - 1);
                    Integer first = next.get(index);
                    next.set(index, next.get(index + 1));
                    next.set(index + 1, first);
                }
                order.set(List.copyOf(next));
                reference.order = List.copyOf(next);
                runtime.update();
                runtime.applyMountedProperties();
            }
            default -> {
                ArrayList<Integer> next = new ArrayList<>(order.get());
                if (random.nextBoolean() && next.size() < 5) {
                    int candidate = 1 + random.nextInt(8);
                    if (!next.contains(candidate)) {
                        next.add(candidate);
                    }
                } else if (next.size() > 1) {
                    next.remove(random.nextInt(next.size()));
                }
                order.set(List.copyOf(next));
                reference.order = List.copyOf(next);
                runtime.update();
                runtime.applyMountedProperties();
            }
        }
    }

    /// Observes production topology and mounted labels.
    ///
    /// @param runtime the production runtime
    /// @return the comparable snapshot
    private static ModelSnapshot observe(StructuralRuntime runtime) {
        ArrayList<String> keys = new ArrayList<>();
        LinkedHashMap<String, Long> identities = new LinkedHashMap<>();
        boolean visible = false;
        for (StructuralGroupSnapshot group : runtime.snapshot().groups()) {
            if ("item".equals(group.sourceIdentity()) && group.semanticKey() != null) {
                keys.add(group.semanticKey());
                identities.put(group.semanticKey(), group.groupId());
            }
            if ("badge".equals(group.sourceIdentity())) {
                visible = group.state() == org.glavo.himari.runtime.structure.StructuralGroupState.ACTIVE;
            }
        }
        String label = "";
        for (MountedElement element : runtime.mounts().snapshot().elements()) {
            if ("label".equals(element.identity().mountKey()) && !element.properties().isEmpty()) {
                label = String.valueOf(element.properties().getFirst().value());
            }
        }
        return new ModelSnapshot(List.copyOf(keys), identities, visible, label);
    }

    /// Rejects one mismatched production-versus-reference observation.
    ///
    /// Surviving keyed identities are compared for stability against the previous production
    /// observation rather than against the reference's synthetic identifiers.
    ///
    /// @param step the step name
    /// @param expected the reference snapshot
    /// @param actual the production snapshot
    /// @param previousIdentities the previous production identities, updated on success
    private static void compare(
            String step,
            ModelSnapshot expected,
            ModelSnapshot actual,
            LinkedHashMap<String, Long> previousIdentities
    ) {
        if (!expected.keys().equals(actual.keys())
                || expected.visible() != actual.visible()
                || !expected.label().equals(actual.label())) {
            throw new IllegalStateException("MBT divergence at " + step + ": expected " + expected
                    + " but was " + actual);
        }
        for (String key : actual.keys()) {
            @Nullable Long previous = previousIdentities.get(key);
            @Nullable Long current = actual.identities().get(key);
            if (previous != null && !previous.equals(current)) {
                throw new IllegalStateException("MBT identity changed at " + step + " for key " + key);
            }
        }
        previousIdentities.clear();
        previousIdentities.putAll(actual.identities());
    }

    /// Recomputes observable structure from the complete model after every operation.
    @NotNullByDefault
    private static final class NaiveReference {
        /// The keyed-collection order.
        private @Unmodifiable List<Integer> order = List.of(1, 2, 3);

        /// Whether the mounted badge is visible.
        private boolean visible = true;

        /// The counter used by the mounted label.
        private int count;

        /// Stable identities for surviving keys.
        private final LinkedHashMap<Integer, Long> identities = new LinkedHashMap<>();

        /// The next synthetic identity.
        private long nextId = 1L;

        /// Returns the recompute-everything observation.
        ///
        /// @return the snapshot
        private ModelSnapshot snapshot() {
            ArrayList<String> keys = new ArrayList<>();
            LinkedHashMap<String, Long> live = new LinkedHashMap<>();
            LinkedHashMap<Integer, Long> surviving = new LinkedHashMap<>();
            for (int key : order) {
                long identity = identities.computeIfAbsent(key, ignored -> nextId++);
                surviving.put(key, identity);
                keys.add(String.valueOf(key));
                live.put(String.valueOf(key), identity);
            }
            identities.clear();
            identities.putAll(surviving);
            return new ModelSnapshot(List.copyOf(keys), live, visible, "Count: " + count);
        }
    }

    /// Stores one comparable observation.
    ///
    /// @param keys keyed-collection order
    /// @param identities surviving key identities
    /// @param visible whether the badge is active
    /// @param label the mounted label, or an empty string when hidden
    @NotNullByDefault
    private record ModelSnapshot(
            @Unmodifiable List<String> keys,
            @Unmodifiable Map<String, Long> identities,
            boolean visible,
            String label
    ) {
        /// Validates and copies the observation.
        private ModelSnapshot {
            keys = List.copyOf(keys);
            identities = Map.copyOf(identities);
            Objects.requireNonNull(label, "label");
        }
    }
}

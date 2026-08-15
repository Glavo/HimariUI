package org.glavo.himari.runtime.mount;

import org.glavo.himari.runtime.animation.AnimationPhaseImpact;
import org.glavo.himari.state.IntState;
import org.glavo.himari.state.StateDomain;
import org.jetbrains.annotations.NotNullByDefault;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/// Executes the deterministic MOUNT-001 acceptance scenarios and writes their observations.
@NotNullByDefault
public final class RuntimeMountConformance {
    /// Prevents instantiation of this command-line entry point.
    private RuntimeMountConformance() {
    }

    /// Verifies independent bindings, incremental phases, and failure-atomic apply.
    ///
    /// @param arguments one output-directory path
    /// @throws IOException if the report cannot be written
    public static void main(String[] arguments) throws IOException {
        if (arguments.length != 1) {
            throw new IllegalArgumentException("Expected one conformance output directory");
        }
        verifyIndependentBindings();
        verifyFailedApplyIsAtomic();
        Path outputDirectory = Path.of(arguments[0]);
        Files.createDirectories(outputDirectory);
        Files.writeString(
                outputDirectory.resolve("results.json"),
                """
                        {
                          "profile": "m1-mount",
                          "workPackage": "MOUNT-001",
                          "status": "passed",
                          "independentBindings": true,
                          "incrementalPhaseApply": true,
                          "failedApplyPreservesTargets": true
                        }
                        """,
                StandardCharsets.UTF_8
        );
    }

    /// Verifies that one source invalidates only its binding.
    private static void verifyIndependentBindings() {
        StateDomain domain = new StateDomain();
        IntState count = domain.intState(0);
        AtomicInteger applies = new AtomicInteger();
        try (MountTree tree = new MountTree(domain)) {
            tree.beginAttempt();
            tree.declare(1L, "root", "counter", element -> element.bind(
                    "label",
                    String.class,
                    AnimationPhaseImpact.MEASURE,
                    () -> "Count: " + count.get(),
                    ignored -> applies.incrementAndGet()
            ));
            tree.commitAttempt(Set.of(1L));
            require(tree.apply().status() == MountApplyStatus.COMMITTED, "Initial mount apply failed");
            count.set(3);
            MountApplyResult result = tree.apply();
            require(result.status() == MountApplyStatus.COMMITTED && result.changedBindingCount() == 1,
                    "Binding apply was not incremental");
            require("Count: 3".equals(tree.snapshot().elements().getFirst().property("label").value()),
                    "Mounted label is incorrect");
            require(applies.get() == 2, "Property applier count is incorrect");
        }
    }

    /// Verifies that a failed reader does not publish a partial property set.
    private static void verifyFailedApplyIsAtomic() {
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
            tree.apply();
            count.set(-4);
            require(tree.apply().status() == MountApplyStatus.FAILED, "Failed apply was not reported");
            require("n=1".equals(tree.snapshot().elements().getFirst().property("text").value()),
                    "Failed apply mutated committed targets");
        }
    }

    /// Rejects one invalid conformance observation.
    ///
    /// @param condition the required condition
    /// @param message the diagnostic
    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
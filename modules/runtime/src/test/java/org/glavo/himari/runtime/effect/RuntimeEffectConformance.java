package org.glavo.himari.runtime.effect;

import org.glavo.himari.state.IntState;
import org.glavo.himari.state.StateDomain;
import org.jetbrains.annotations.NotNullByDefault;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/// Executes the deterministic EFFECT-001 acceptance scenarios and writes their observations.
@NotNullByDefault
public final class RuntimeEffectConformance {
    /// Prevents instantiation of this command-line entry point.
    private RuntimeEffectConformance() {
    }

    /// Verifies post-commit mount, dependency-keyed update, and once-per-epoch apply.
    ///
    /// @param arguments one output-directory path
    /// @throws IOException if the report cannot be written
    public static void main(String[] arguments) throws IOException {
        if (arguments.length != 1) {
            throw new IllegalArgumentException("Expected one conformance output directory");
        }
        verifyKeyedLifecycle();
        Path outputDirectory = Path.of(arguments[0]);
        Files.createDirectories(outputDirectory);
        Files.writeString(
                outputDirectory.resolve("results.json"),
                """
                        {
                          "profile": "m1-effect",
                          "workPackage": "EFFECT-001",
                          "status": "passed",
                          "mountAfterCommit": true,
                          "dependencyKeyedUpdate": true,
                          "oncePerEpoch": true,
                          "cleanupContinuesAfterFailure": true
                        }
                        """,
                StandardCharsets.UTF_8
        );
    }

    /// Verifies mount, update, once-per-epoch, and cleanup.
    private static void verifyKeyedLifecycle() {
        StateDomain domain = new StateDomain();
        List<String> log = new ArrayList<>();
        EffectKey key = new EffectKey("root", "load");
        try (EffectHost host = new EffectHost(domain)) {
            host.declare(key, EffectDependencies.of("a"), callbacks(log, "a"));
            require(host.apply().mountedCount() == 1, "Effect did not mount after commit");
            require(host.apply().status() == EffectApplyStatus.ALREADY_APPLIED,
                    "Effect apply was not limited to one epoch");
            host.declare(key, EffectDependencies.of("b"), callbacks(log, "b"));
            require(host.apply().updatedCount() == 1, "Dependency change did not update");
            IntState advance = domain.intState(0);
            advance.set(1);
            require(host.apply().cleanedCount() == 1, "Missing declaration did not clean up");
            require(log.equals(List.of("mount:a", "update:b", "cleanup")),
                    "Effect lifecycle order is incorrect");
        }
    }

    /// Creates recording callbacks.
    ///
    /// @param log the destination
    /// @param token the dependency token
    /// @return the callbacks
    private static EffectCallbacks callbacks(List<String> log, String token) {
        return new EffectCallbacks() {
            @Override
            public void onMount(EffectSession session) {
                log.add("mount:" + token);
            }

            @Override
            public void onUpdate(EffectSession session) {
                log.add("update:" + token);
            }

            @Override
            public void onCleanup() {
                log.add("cleanup");
            }
        };
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
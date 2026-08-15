package org.glavo.himari.spikes.runtime.hybrid;

import org.glavo.himari.spikes.runtime.sample.ComparisonProbe;
import org.glavo.himari.spikes.runtime.sample.RuntimePhase;
import org.glavo.himari.state.BooleanState;
import org.glavo.himari.state.IntState;
import org.glavo.himari.state.MutableState;
import org.glavo.himari.state.StateDomain;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies hybrid owner, binding, retention, and staged-reconciliation invariants.
@NotNullByDefault
final class HybridRuntimeTest {
    /// Verifies that source updates execute bindings without rerunning an owner initializer.
    @Test
    void updatesBoundPropertyWithoutRerunningInitializer() {
        StateDomain domain = new StateDomain();
        IntState source = domain.intState(4);
        AtomicInteger initializations = new AtomicInteger();
        AtomicInteger property = new AtomicInteger();
        ComparisonProbe probe = new ComparisonProbe();

        try (HybridRuntime runtime = new HybridRuntime(probe)) {
            runtime.mount(root -> {
                initializations.incrementAndGet();
                root.node("root");
                root.bind(source, "value", () -> property.set(source.get()), RuntimePhase.PAINT);
            });

            assertEquals(1, initializations.get());
            assertEquals(4, property.get());
            source.set(9);
            runtime.flush();
            assertEquals(1, initializations.get());
            assertEquals(9, property.get());
            assertEquals(List.of("root"), runtime.mountedNodes());
        }

        assertEquals(0L, probe.metrics().activeDependencyEdges());
        assertEquals(0L, probe.metrics().retainedBytes());
    }

    /// Verifies retained-owner identity and disposable-owner recreation at stable anchors.
    @Test
    void appliesExplicitStructuralRetentionPolicies() {
        StateDomain domain = new StateDomain();
        BooleanState retainedVisible = domain.booleanState(false);
        BooleanState disposedVisible = domain.booleanState(false);
        ControllerBox box = new ControllerBox();
        ComparisonProbe probe = new ComparisonProbe();

        try (HybridRuntime runtime = new HybridRuntime(probe)) {
            runtime.mount(root -> {
                root.node("root");
                box.retained = root.structure(
                        "retained",
                        retainedVisible,
                        scope -> {
                            if (retainedVisible.get()) {
                                scope.fragment("branch", HybridRuntime.Retention.RETAIN, owner -> {
                                    owner.node("retained");
                                    return owner.localInt(0);
                                });
                            }
                        },
                        RuntimePhase.STRUCTURE
                );
                box.disposed = root.structure(
                        "disposed",
                        disposedVisible,
                        scope -> {
                            if (disposedVisible.get()) {
                                scope.fragment("branch", HybridRuntime.Retention.DISPOSE, owner -> {
                                    owner.node("disposed");
                                    return owner.localInt(0);
                                });
                            }
                        },
                        RuntimePhase.STRUCTURE
                );
            });

            retainedVisible.set(true);
            runtime.flush();
            HybridRuntime.LocalInt retained = Objects.requireNonNull(box.retained().value("branch"));
            retained.increment();
            retainedVisible.set(false);
            runtime.flush();
            retainedVisible.set(true);
            runtime.flush();
            assertSame(retained, box.retained().value("branch"));
            assertEquals(1, retained.get());

            disposedVisible.set(true);
            runtime.flush();
            HybridRuntime.LocalInt firstDisposed = Objects.requireNonNull(box.disposed().value("branch"));
            disposedVisible.set(false);
            runtime.flush();
            assertFalse(box.disposed().visible("branch"));
            disposedVisible.set(true);
            runtime.flush();
            assertTrue(firstDisposed != box.disposed().value("branch"));
        }

        assertEquals(0L, probe.metrics().activeDependencyEdges());
        assertEquals(0L, probe.metrics().retainedBytes());
    }

    /// Verifies that every newly staged keyed owner is aborted after a later initializer fails.
    @Test
    void rollsBackAllNewKeyedOwnersAfterStagingFailure() {
        StateDomain domain = new StateDomain();
        MutableState<List<String>> keys = domain.mutableState(List.of("a"));
        AtomicBoolean failY = new AtomicBoolean();
        AtomicInteger aborts = new AtomicInteger();
        KeyedBox box = new KeyedBox();
        ComparisonProbe probe = new ComparisonProbe();

        try (HybridRuntime runtime = new HybridRuntime(probe)) {
            runtime.mount(root -> {
                root.node("root");
                box.items = root.structure(
                        "items",
                        keys,
                        scope -> {
                            for (String key : keys.get()) {
                                scope.fragment(key, HybridRuntime.Retention.DISPOSE, owner -> {
                                    owner.onAbort(aborts::incrementAndGet);
                                    owner.node("item:" + key);
                                    if (failY.get() && key.equals("y")) {
                                        owner.fail("test-stage-failure");
                                    }
                                    return owner.localInt(0);
                                });
                            }
                        },
                        RuntimePhase.STRUCTURE
                );
            });
            HybridRuntime.LocalInt survivor = Objects.requireNonNull(box.items().value("a"));

            failY.set(true);
            keys.set(List.of("a", "x", "y"));
            HybridRuntime.HybridMutationException failure = assertThrows(
                    HybridRuntime.HybridMutationException.class,
                    runtime::flush
            );
            assertEquals("test-stage-failure", failure.code());
            assertEquals(2, aborts.get());
            assertEquals(List.of("root", "item:a"), runtime.mountedNodes());
            assertSame(survivor, box.items().value("a"));
            assertEquals(0L, runtime.health().stagedMutations());

            failY.set(false);
            runtime.flush();
            assertEquals(List.of("root", "item:a", "item:x", "item:y"), runtime.mountedNodes());
            assertSame(survivor, box.items().value("a"));
        }

        assertEquals(0L, probe.metrics().activeDependencyEdges());
        assertEquals(0L, probe.metrics().retainedBytes());
    }

    /// Stores conditional controllers initialized from a mount callback.
    @NotNullByDefault
    private static final class ControllerBox {
        /// The retained controller, or `null` before mount.
        private @Nullable HybridRuntime.StructuralScope<String, HybridRuntime.LocalInt> retained;

        /// The disposable controller, or `null` before mount.
        private @Nullable HybridRuntime.StructuralScope<String, HybridRuntime.LocalInt> disposed;

        /// Creates an empty box.
        private ControllerBox() {
        }

        /// Returns the mounted retained controller.
        ///
        /// @return the controller
        private HybridRuntime.StructuralScope<String, HybridRuntime.LocalInt> retained() {
            if (retained == null) {
                throw new IllegalStateException("Retained controller is unavailable");
            }
            return retained;
        }

        /// Returns the mounted disposable controller.
        ///
        /// @return the controller
        private HybridRuntime.StructuralScope<String, HybridRuntime.LocalInt> disposed() {
            if (disposed == null) {
                throw new IllegalStateException("Disposable controller is unavailable");
            }
            return disposed;
        }
    }

    /// Stores a keyed controller initialized from a mount callback.
    @NotNullByDefault
    private static final class KeyedBox {
        /// The keyed controller, or `null` before mount.
        private @Nullable HybridRuntime.StructuralScope<String, HybridRuntime.LocalInt> items;

        /// Creates an empty box.
        private KeyedBox() {
        }

        /// Returns the mounted keyed controller.
        ///
        /// @return the controller
        private HybridRuntime.StructuralScope<String, HybridRuntime.LocalInt> items() {
            if (items == null) {
                throw new IllegalStateException("Keyed controller is unavailable");
            }
            return items;
        }
    }
}


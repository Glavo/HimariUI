package org.glavo.himari.spikes.runtime.hybrid;

import org.glavo.himari.spikes.runtime.sample.ComparisonEnvironment;
import org.glavo.himari.spikes.runtime.sample.ComparisonProbe;
import org.glavo.himari.spikes.runtime.sample.FixtureCommand;
import org.glavo.himari.spikes.runtime.sample.RuntimePhase;
import org.glavo.himari.state.BooleanState;
import org.glavo.himari.state.DerivedState;
import org.glavo.himari.state.IntState;
import org.glavo.himari.state.MutableState;
import org.glavo.himari.state.ReactiveOwner;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Supplies the six ordinary-Java micro applications for the hybrid candidate.
@NotNullByDefault
final class HybridMicroApplications {
    /// Prevents construction.
    private HybridMicroApplications() {
    }

    /// Opens a micro application by frozen fixture identifier.
    ///
    /// @param fixtureId the fixture identifier
    /// @param environment the fresh environment
    /// @param probe the shared probe
    /// @return the application, or `null` when the fixture is not a micro fixture
    static @Nullable HybridFixtureSession open(
            String fixtureId,
            ComparisonEnvironment environment,
            ComparisonProbe probe
    ) {
        return switch (fixtureId) {
            case "counter-derived-handler" -> new CounterApplication(environment, probe);
            case "diamond-glitch" -> new DiamondApplication(environment, probe);
            case "conditional-lifecycle" -> new ConditionalApplication(environment, probe);
            case "changing-component-input" -> new ChangingInputApplication(environment, probe);
            case "keyed-list-identity" -> new KeyedListApplication(environment, probe);
            case "phase-impact-burst" -> new PhaseImpactApplication(environment, probe);
            default -> null;
        };
    }

    /// Implements a dynamic handler input and a property-bound derived counter label.
    @NotNullByDefault
    private static final class CounterApplication extends HybridFixtureSession {
        /// The application count.
        private final IntState count;

        /// The current handler increment.
        private final IntState step;

        /// The label property last written by its fine-grained binding.
        private String label = "";

        /// Creates the counter application.
        ///
        /// @param environment the fresh environment
        /// @param probe the shared probe
        private CounterApplication(ComparisonEnvironment environment, ComparisonProbe probe) {
            super(environment, probe);
            count = domain.intState(0);
            step = domain.intState(1);
        }

        /// Mounts persistent nodes and one count-to-label binding.
        ///
        /// @param root the root owner
        @Override
        protected void initialize(HybridRuntime.Owner root) {
            root.node("root");
            root.component("counter-content", content -> {
                content.bind(
                        count,
                        "label-count",
                        () -> label = "Count: " + count.get(),
                        RuntimePhase.PAINT,
                        RuntimePhase.SEMANTICS
                );
                content.node("counter-label");
                content.node("increment-button");
            });
        }

        /// Applies counter commands without recreating the component owner.
        ///
        /// @param command the command
        /// @return whether a source may require a binding flush
        @Override
        protected boolean handle(FixtureCommand command) {
            switch (command.operation()) {
                case "activate-increment" -> {
                    transaction(() -> count.set(Math.addExact(count.get(), step.get())));
                    emit("handler:increment");
                }
                case "set-step" -> step.set(intArgument(command, "value"));
                case "set-count" -> count.set(intArgument(command, "value"));
                default -> throw unknown(command);
            }
            return true;
        }

        /// Returns counter state and the bound label property.
        ///
        /// @return the values
        @Override
        protected @Unmodifiable Map<String, String> values() {
            return valuesOf(
                    "count", Integer.toString(count.get()),
                    "label", label,
                    "step", Integer.toString(step.get())
            );
        }
    }

    /// Implements a lazily stabilized diamond observed by one property binding.
    @NotNullByDefault
    private static final class DiamondApplication extends HybridFixtureSession {
        /// The diamond source.
        private final IntState source;

        /// The owner of all derived computations.
        private final ReactiveOwner reactiveOwner;

        /// The doubled branch.
        private final DerivedState<Integer> left;

        /// The tripled branch.
        private final DerivedState<Integer> right;

        /// The stabilized downstream sum.
        private final DerivedState<Integer> sum;

        /// The sum property last written by the binding.
        private int renderedSum;

        /// Creates the diamond application.
        ///
        /// @param environment the fresh environment
        /// @param probe the shared probe
        private DiamondApplication(ComparisonEnvironment environment, ComparisonProbe probe) {
            super(environment, probe);
            source = domain.intState(1);
            reactiveOwner = domain.reactiveGraph().createOwner();
            left = reactiveOwner.derivedState(() -> Math.multiplyExact(source.get(), 2));
            right = reactiveOwner.derivedState(() -> Math.multiplyExact(source.get(), 3));
            sum = reactiveOwner.derivedState(() -> Math.addExact(left.get(), right.get()));
        }

        /// Mounts one output node and its stabilized downstream binding.
        ///
        /// @param root the root owner
        @Override
        protected void initialize(HybridRuntime.Owner root) {
            root.node("root");
            root.bind(
                    sum,
                    "diamond-sum",
                    () -> renderedSum = sum.get(),
                    RuntimePhase.PAINT,
                    RuntimePhase.SEMANTICS
            );
            root.node("diamond-output");
        }

        /// Publishes one source value and emits only a changed stable sum.
        ///
        /// @param command the command
        /// @return always `true`
        @Override
        protected boolean handle(FixtureCommand command) {
            if (!command.operation().equals("set-source")) {
                throw unknown(command);
            }
            int before = sum.get();
            source.set(intArgument(command, "value"));
            int after = sum.get();
            if (after != before) {
                emit("observer:sum=" + after);
            }
            return true;
        }

        /// Returns the source, both branches, and the bound stable sum.
        ///
        /// @return the values
        @Override
        protected @Unmodifiable Map<String, String> values() {
            return valuesOf(
                    "source", Integer.toString(source.get()),
                    "left", Integer.toString(left.get()),
                    "right", Integer.toString(right.get()),
                    "sum", Integer.toString(renderedSum)
            );
        }

        /// Disposes the derived graph and its dependency edges.
        @Override
        protected void closeApplicationResources() {
            reactiveOwner.close();
        }
    }

    /// Implements explicit retained and disposed conditional-owner policies.
    @NotNullByDefault
    private static final class ConditionalApplication extends HybridFixtureSession {
        /// Whether the retained branch is visible.
        private final BooleanState retainedVisible;

        /// Whether the disposable branch is visible.
        private final BooleanState disposedVisible;

        /// The retained structural scope, or `null` before mount.
        private @Nullable HybridRuntime.StructuralScope<String, HybridRuntime.LocalInt> retained;

        /// The disposable structural scope, or `null` before mount.
        private @Nullable HybridRuntime.StructuralScope<String, HybridRuntime.LocalInt> disposed;

        /// Creates the conditional application.
        ///
        /// @param environment the fresh environment
        /// @param probe the shared probe
        private ConditionalApplication(ComparisonEnvironment environment, ComparisonProbe probe) {
            super(environment, probe);
            retainedVisible = domain.booleanState(false);
            disposedVisible = domain.booleanState(false);
        }

        /// Mounts two small structural scopes with different hidden lifetimes.
        ///
        /// @param root the root owner
        @Override
        protected void initialize(HybridRuntime.Owner root) {
            root.node("root");
            root.node("branch-controls");
            retained = root.structure(
                    "retained-panel",
                    retainedVisible,
                    scope -> {
                        if (retainedVisible.get()) {
                            scope.fragment("panel", HybridRuntime.Retention.RETAIN, branch -> {
                                HybridRuntime.LocalInt local = branch.localInt(0);
                                branch.node("retained-panel");
                                branch.effect(
                                        () -> emit("effect-mount:retained"),
                                        () -> emit("effect-dispose:retained")
                                );
                                return local;
                            });
                        }
                    },
                    RuntimePhase.STRUCTURE
            );
            disposed = root.structure(
                    "disposed-panel",
                    disposedVisible,
                    scope -> {
                        if (disposedVisible.get()) {
                            scope.fragment("panel", HybridRuntime.Retention.DISPOSE, branch -> {
                                HybridRuntime.LocalInt local = branch.localInt(0);
                                branch.node("disposed-panel");
                                branch.effect(
                                        () -> emit("effect-mount:disposed"),
                                        () -> {
                                            emit("effect-dispose:disposed");
                                            emit("owner-dispose:disposed");
                                        }
                                );
                                return local;
                            });
                        }
                    },
                    RuntimePhase.STRUCTURE
            );
        }

        /// Applies branch visibility and owner-local state commands.
        ///
        /// @param command the command
        /// @return whether a visibility source changed
        @Override
        protected boolean handle(FixtureCommand command) {
            return switch (command.operation()) {
                case "show-retained" -> {
                    retainedVisible.set(true);
                    yield true;
                }
                case "hide-retained" -> {
                    retainedVisible.set(false);
                    yield true;
                }
                case "increment-retained" -> {
                    requireLocal(retained().value("panel"), "retained").increment();
                    yield false;
                }
                case "show-disposed" -> {
                    disposedVisible.set(true);
                    yield true;
                }
                case "hide-disposed" -> {
                    disposedVisible.set(false);
                    yield true;
                }
                case "increment-disposed" -> {
                    requireLocal(disposed().value("panel"), "disposed").increment();
                    yield false;
                }
                default -> throw unknown(command);
            };
        }

        /// Returns conditional visibility and owner-local values.
        ///
        /// @return the values
        @Override
        protected @Unmodifiable Map<String, String> values() {
            @Nullable HybridRuntime.LocalInt retainedLocal = retained().value("panel");
            @Nullable HybridRuntime.LocalInt disposedLocal = disposed().value("panel");
            String retainedValue = retainedLocal == null || benchmarking() && !retainedVisible.get()
                    ? "absent" : Integer.toString(retainedLocal.get());
            String disposedValue = disposedLocal == null ? "absent" : Integer.toString(disposedLocal.get());
            return valuesOf(
                    "disposed.local", disposedValue,
                    "disposed.visible", Boolean.toString(disposedVisible.get()),
                    "retained.local", retainedValue,
                    "retained.visible", Boolean.toString(retainedVisible.get())
            );
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

        /// Returns a required conditional owner-local cell.
        ///
        /// @param local the possibly absent local
        /// @param name the branch name
        /// @return the local
        private static HybridRuntime.LocalInt requireLocal(
                @Nullable HybridRuntime.LocalInt local,
                String name
        ) {
            if (local == null) {
                throw new IllegalStateException(name + " branch is not mounted");
            }
            return local;
        }
    }

    /// Implements changing child input with a stable hybrid component owner.
    @NotNullByDefault
    private static final class ChangingInputApplication extends HybridFixtureSession {
        /// The parent-owned child input.
        private final MutableState<String> input;

        /// The child-local cell, or `null` before mount.
        private @Nullable HybridRuntime.LocalInt childLocal;

        /// The child property last written by its binding.
        private String renderedInput = "";

        /// Creates the changing-input application.
        ///
        /// @param environment the fresh environment
        /// @param probe the shared probe
        private ChangingInputApplication(ComparisonEnvironment environment, ComparisonProbe probe) {
            super(environment, probe);
            input = domain.mutableState("alpha");
        }

        /// Mounts a parent and a child whose initializer never reruns.
        ///
        /// @param root the root owner
        @Override
        protected void initialize(HybridRuntime.Owner root) {
            root.node("root");
            root.component("parent", parent -> {
                parent.node("parent");
                parent.component("child", child -> {
                    childLocal = child.localInt(0);
                    child.bind(
                            input,
                            "child-input",
                            () -> renderedInput = input.get(),
                            RuntimePhase.PAINT,
                            RuntimePhase.SEMANTICS
                    );
                    child.node("child:child-1");
                });
            });
        }

        /// Mutates child-local state or replaces its parent-owned input.
        ///
        /// @param command the command
        /// @return whether a reactive source changed
        @Override
        protected boolean handle(FixtureCommand command) {
            return switch (command.operation()) {
                case "increment-child" -> {
                    local().increment();
                    yield false;
                }
                case "set-child-input" -> {
                    String previous = input.get();
                    String next = argument(command, "value");
                    input.set(next);
                    if (!previous.equals(next)) {
                        emit("child-update:" + previous + "->" + next);
                    }
                    yield true;
                }
                default -> throw unknown(command);
            };
        }

        /// Returns child identity, bound input, and local state.
        ///
        /// @return the values
        @Override
        protected @Unmodifiable Map<String, String> values() {
            return valuesOf(
                    "child.id", "child-1",
                    "child.input", renderedInput,
                    "child.local", Integer.toString(local().get())
            );
        }

        /// Returns the mounted child-local cell.
        ///
        /// @return the cell
        private HybridRuntime.LocalInt local() {
            if (childLocal == null) {
                throw new IllegalStateException("Child local state is unavailable");
            }
            return childLocal;
        }
    }

    /// Implements explicit keyed reconciliation with failure-atomic duplicate detection.
    @NotNullByDefault
    private static final class KeyedListApplication extends HybridFixtureSession {
        /// The requested semantic item order.
        private final MutableState<@Unmodifiable List<String>> order;

        /// The keyed structural scope, or `null` before mount.
        private @Nullable HybridRuntime.StructuralScope<String, HybridRuntime.LocalInt> items;

        /// Creates the keyed-list application.
        ///
        /// @param environment the fresh environment
        /// @param probe the shared probe
        private KeyedListApplication(ComparisonEnvironment environment, ComparisonProbe probe) {
            super(environment, probe);
            order = domain.mutableState(List.of("a", "b", "c"));
        }

        /// Mounts one small structural scope keyed by application item identity.
        ///
        /// @param root the root owner
        @Override
        protected void initialize(HybridRuntime.Owner root) {
            root.node("root");
            root.node("list");
            items = root.structure(
                    "list-items",
                    order,
                    scope -> {
                        for (String key : order.get()) {
                            scope.fragment(key, HybridRuntime.Retention.DISPOSE, item -> {
                                HybridRuntime.LocalInt local = item.localInt(0);
                                item.node("item:" + key);
                                item.effect(
                                        () -> emit("mount:" + key),
                                        () -> emit("dispose:" + key)
                                );
                                return local;
                            });
                        }
                    },
                    RuntimePhase.STRUCTURE
            );
        }

        /// Applies keyed order and item-local commands.
        ///
        /// @param command the command
        /// @return whether keyed reconciliation is required
        @Override
        protected boolean handle(FixtureCommand command) {
            return switch (command.operation()) {
                case "increment-item" -> {
                    local(argument(command, "key")).increment();
                    yield false;
                }
                case "set-order" -> {
                    order.set(List.copyOf(Arrays.asList(argument(command, "keys").split(","))));
                    yield true;
                }
                default -> throw unknown(command);
            };
        }

        /// Restores the last committed controller keys after a rejected mutation.
        ///
        /// @param failure the contained failure
        @Override
        protected void onMutationFailure(HybridRuntime.HybridMutationException failure) {
            order.set(controller().keys());
            super.onMutationFailure(failure);
        }

        /// Returns each committed keyed item-local value.
        ///
        /// @return the values
        @Override
        protected @Unmodifiable Map<String, String> values() {
            LinkedHashMap<String, String> result = new LinkedHashMap<>();
            for (String key : controller().keys()) {
                result.put("local." + key, Integer.toString(local(key).get()));
            }
            return Map.copyOf(result);
        }

        /// Returns the mounted collection controller.
        ///
        /// @return the controller
        private HybridRuntime.StructuralScope<String, HybridRuntime.LocalInt> controller() {
            if (items == null) {
                throw new IllegalStateException("Keyed controller is unavailable");
            }
            return items;
        }

        /// Returns one committed item-local cell.
        ///
        /// @param key the item key
        /// @return the cell
        private HybridRuntime.LocalInt local(String key) {
            @Nullable HybridRuntime.LocalInt local = controller().value(key);
            if (local == null) {
                throw new IllegalArgumentException("Unknown item key: " + key);
            }
            return local;
        }
    }

    /// Implements phase-attributed property bindings and a transaction-coalesced burst.
    @NotNullByDefault
    private static final class PhaseImpactApplication extends HybridFixtureSession {
        /// The displayed text source.
        private final MutableState<String> text;

        /// The paint color source.
        private final MutableState<String> color;

        /// The measured size source.
        private final IntState size;

        /// The placed offset source.
        private final IntState offset;

        /// The text property written by its binding.
        private String renderedText = "";

        /// The color property written by its binding.
        private String renderedColor = "";

        /// The size property written by its binding.
        private int renderedSize;

        /// The offset property written by its binding.
        private int renderedOffset;

        /// The update count exposed only by the burst checkpoint.
        private int updates;

        /// Whether the update-count value belongs in the observation.
        private boolean burstApplied;

        /// Creates the phase-impact application.
        ///
        /// @param environment the fresh environment
        /// @param probe the shared probe
        private PhaseImpactApplication(ComparisonEnvironment environment, ComparisonProbe probe) {
            super(environment, probe);
            text = domain.mutableState("alpha");
            color = domain.mutableState("red");
            size = domain.intState(10);
            offset = domain.intState(0);
        }

        /// Mounts one persistent node with four phase-specific bindings.
        ///
        /// @param root the root owner
        @Override
        protected void initialize(HybridRuntime.Owner root) {
            root.node("root");
            root.component("phase-target-owner", target -> {
                target.bind(
                        text,
                        "text",
                        () -> renderedText = text.get(),
                        RuntimePhase.MEASURE,
                        RuntimePhase.PAINT,
                        RuntimePhase.SEMANTICS
                );
                target.bind(color, "color", () -> renderedColor = color.get(), RuntimePhase.PAINT);
                target.bind(size, "size", () -> renderedSize = size.get(), RuntimePhase.MEASURE);
                target.bind(
                        offset,
                        "offset",
                        () -> renderedOffset = offset.get(),
                        RuntimePhase.PLACE,
                        RuntimePhase.HIT_TEST
                );
                target.node("phase-target");
            });
        }

        /// Applies individual values, a coalesced burst, or a benchmark variant.
        ///
        /// @param command the command
        /// @return always `true`
        @Override
        protected boolean handle(FixtureCommand command) {
            switch (command.operation()) {
                case "set-text" -> text.set(argument(command, "value"));
                case "set-color" -> color.set(argument(command, "value"));
                case "set-size" -> size.set(intArgument(command, "value"));
                case "set-offset" -> offset.set(intArgument(command, "value"));
                case "apply-burst" -> {
                    updates = intArgument(command, "updates");
                    transaction(() -> {
                        text.set("gamma");
                        color.set("green");
                        size.set(24);
                        offset.set(9);
                    });
                    burstApplied = true;
                }
                case "set-variant" -> {
                    boolean alternate = argument(command, "value").equals("b");
                    transaction(() -> {
                        text.set(alternate ? "beta" : "alpha");
                        color.set(alternate ? "blue" : "red");
                        size.set(alternate ? 20 : 10);
                        offset.set(alternate ? 5 : 0);
                    });
                    burstApplied = false;
                }
                default -> throw unknown(command);
            }
            return true;
        }

        /// Returns bound phase-target values and the optional burst count.
        ///
        /// @return the values
        @Override
        protected @Unmodifiable Map<String, String> values() {
            LinkedHashMap<String, String> result = new LinkedHashMap<>();
            result.put("color", renderedColor);
            result.put("offset", Integer.toString(renderedOffset));
            result.put("size", Integer.toString(renderedSize));
            result.put("text", renderedText);
            if (burstApplied) {
                result.put("updates", Integer.toString(updates));
            }
            return Map.copyOf(result);
        }
    }
}


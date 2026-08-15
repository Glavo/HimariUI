package org.glavo.himari.spikes.runtime.grouped;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Supplies the six ordinary-Java micro applications for the grouped candidate.
@NotNullByDefault
final class GroupedMicroApplications {
    /// Prevents construction.
    private GroupedMicroApplications() {
    }

    /// Opens a micro application by frozen fixture identifier.
    ///
    /// @param fixtureId the fixture identifier
    /// @param environment the fresh environment
    /// @param probe the shared probe
    /// @return the application, or `null` when the fixture is not a micro fixture
    static @Nullable GroupedFixtureSession open(
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

    /// Implements changing handler input and a derived counter label.
    @NotNullByDefault
    private static final class CounterApplication extends GroupedFixtureSession {
        /// The application count.
        private final IntState count;

        /// The current handler increment.
        private final IntState step;

        /// Creates the counter application.
        ///
        /// @param environment the fresh environment
        /// @param probe the shared probe
        private CounterApplication(ComparisonEnvironment environment, ComparisonProbe probe) {
            super(environment, probe);
            count = domain.intState(0);
            step = domain.intState(1);
        }

        /// Declares the counter groups and phase-attributed label read.
        ///
        /// @param scope the grouped scope
        @Override
        protected void compose(GroupedRuntime.Scope scope) {
            scope.node("root");
            scope.group("counter-content", () -> {
                scope.binding(count, "label-count", RuntimePhase.PAINT, RuntimePhase.SEMANTICS);
                scope.node("counter-label");
                scope.node("increment-button");
            });
        }

        /// Applies counter commands.
        ///
        /// @param command the command
        /// @return always `true`
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

        /// Returns current counter values.
        ///
        /// @return the values
        @Override
        protected @Unmodifiable Map<String, String> values() {
            return valuesOf(
                    "count", Integer.toString(count.get()),
                    "label", "Count: " + count.get(),
                    "step", Integer.toString(step.get())
            );
        }
    }

    /// Implements a lazily stabilized diamond on the shared push-pull value graph.
    @NotNullByDefault
    private static final class DiamondApplication extends GroupedFixtureSession {
        /// The diamond source.
        private final IntState source;

        /// The owner of the three derived computations.
        private final ReactiveOwner owner;

        /// The doubled branch.
        private final DerivedState<Integer> left;

        /// The tripled branch.
        private final DerivedState<Integer> right;

        /// The stable downstream sum.
        private final DerivedState<Integer> sum;

        /// Creates the diamond application.
        ///
        /// @param environment the fresh environment
        /// @param probe the shared probe
        private DiamondApplication(ComparisonEnvironment environment, ComparisonProbe probe) {
            super(environment, probe);
            source = domain.intState(1);
            owner = domain.reactiveGraph().createOwner();
            left = owner.derivedState(() -> Math.multiplyExact(source.get(), 2));
            right = owner.derivedState(() -> Math.multiplyExact(source.get(), 3));
            sum = owner.derivedState(() -> Math.addExact(left.get(), right.get()));
        }

        /// Declares the single downstream diamond binding.
        ///
        /// @param scope the grouped scope
        @Override
        protected void compose(GroupedRuntime.Scope scope) {
            scope.node("root");
            scope.group("diamond-output", () -> {
                scope.binding(sum, "sum", RuntimePhase.PAINT, RuntimePhase.SEMANTICS);
                sum.get();
                scope.node("diamond-output");
            });
        }

        /// Publishes a source value and reports only a changed stable sum.
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

        /// Returns the stable diamond values.
        ///
        /// @return the values
        @Override
        protected @Unmodifiable Map<String, String> values() {
            return valuesOf(
                    "source", Integer.toString(source.get()),
                    "left", Integer.toString(left.get()),
                    "right", Integer.toString(right.get()),
                    "sum", Integer.toString(sum.get())
            );
        }

        /// Disposes the derived graph owner and all dependency edges it owns.
        @Override
        protected void closeApplicationResources() {
            owner.close();
        }
    }

    /// Implements explicit retained and disposed conditional-memory policies.
    @NotNullByDefault
    private static final class ConditionalApplication extends GroupedFixtureSession {
        /// Whether the retain-on-hide branch is visible.
        private final BooleanState retainedVisible;

        /// Whether the dispose-on-hide branch is visible.
        private final BooleanState disposedVisible;

        /// The latest committed retained local cell, or `null` before its first mount.
        private @Nullable GroupedRuntime.LocalInt retainedLocal;

        /// The latest committed disposable local cell, or `null` while absent.
        private @Nullable GroupedRuntime.LocalInt disposedLocal;

        /// Creates the conditional application.
        ///
        /// @param environment the fresh environment
        /// @param probe the shared probe
        private ConditionalApplication(ComparisonEnvironment environment, ComparisonProbe probe) {
            super(environment, probe);
            retainedVisible = domain.booleanState(false);
            disposedVisible = domain.booleanState(false);
        }

        /// Declares both conditional branches and their explicit memory policies.
        ///
        /// @param scope the grouped scope
        @Override
        protected void compose(GroupedRuntime.Scope scope) {
            scope.node("root");
            scope.node("branch-controls");
            scope.binding(retainedVisible, "retained-visible", RuntimePhase.STRUCTURE);
            scope.branch("retained-panel", retainedVisible.get(), true, () -> {
                GroupedRuntime.LocalInt local = scope.rememberInt(0);
                scope.onCommit(() -> retainedLocal = local);
                scope.node("retained-panel");
                scope.effect(
                        "panel-effect",
                        () -> emit("effect-mount:retained"),
                        () -> emit("effect-dispose:retained")
                );
            });
            scope.binding(disposedVisible, "disposed-visible", RuntimePhase.STRUCTURE);
            if (!disposedVisible.get()) {
                scope.onCommit(() -> disposedLocal = null);
            }
            scope.branch("disposed-panel", disposedVisible.get(), false, () -> {
                GroupedRuntime.LocalInt local = scope.rememberInt(0);
                scope.onCommit(() -> disposedLocal = local);
                scope.node("disposed-panel");
                scope.effect(
                        "panel-effect",
                        () -> emit("effect-mount:disposed"),
                        () -> {
                            emit("effect-dispose:disposed");
                            emit("owner-dispose:disposed");
                        }
                );
            });
        }

        /// Applies visibility and local-state commands.
        ///
        /// @param command the command
        /// @return always `true`
        @Override
        protected boolean handle(FixtureCommand command) {
            switch (command.operation()) {
                case "show-retained" -> retainedVisible.set(true);
                case "hide-retained" -> retainedVisible.set(false);
                case "increment-retained" -> requireLocal(retainedLocal, "retained").increment();
                case "show-disposed" -> disposedVisible.set(true);
                case "hide-disposed" -> disposedVisible.set(false);
                case "increment-disposed" -> requireLocal(disposedLocal, "disposed").increment();
                default -> throw unknown(command);
            }
            return true;
        }

        /// Returns conditional visibility and memory values.
        ///
        /// @return the values
        @Override
        protected @Unmodifiable Map<String, String> values() {
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

        /// Returns a required mounted local cell.
        ///
        /// @param local the possibly absent cell
        /// @param name the branch name
        /// @return the present cell
        private static GroupedRuntime.LocalInt requireLocal(
                @Nullable GroupedRuntime.LocalInt local,
                String name
        ) {
            if (local == null) {
                throw new IllegalStateException(name + " branch is not mounted");
            }
            return local;
        }
    }

    /// Implements changing child input with stable positional identity and local state.
    @NotNullByDefault
    private static final class ChangingInputApplication extends GroupedFixtureSession {
        /// The parent-owned child input.
        private final MutableState<String> input;

        /// The committed child-local cell.
        private @Nullable GroupedRuntime.LocalInt childLocal;

        /// Creates the changing-input application.
        ///
        /// @param environment the fresh environment
        /// @param probe the shared probe
        private ChangingInputApplication(ComparisonEnvironment environment, ComparisonProbe probe) {
            super(environment, probe);
            input = domain.mutableState("alpha");
        }

        /// Declares a stable child group below an explicit parent group.
        ///
        /// @param scope the grouped scope
        @Override
        protected void compose(GroupedRuntime.Scope scope) {
            scope.node("root");
            scope.group("parent", () -> {
                scope.node("parent");
                scope.group("child", () -> {
                    GroupedRuntime.LocalInt local = scope.rememberInt(0);
                    scope.onCommit(() -> childLocal = local);
                    scope.binding(input, "child-input", RuntimePhase.PAINT, RuntimePhase.SEMANTICS);
                    scope.node("child:child-1");
                });
            });
        }

        /// Mutates local state or replaces the parent input.
        ///
        /// @param command the command
        /// @return always `true`
        @Override
        protected boolean handle(FixtureCommand command) {
            switch (command.operation()) {
                case "increment-child" -> local().increment();
                case "set-child-input" -> {
                    String previous = input.get();
                    String next = argument(command, "value");
                    input.set(next);
                    if (!previous.equals(next)) {
                        emit("child-update:" + previous + "->" + next);
                    }
                }
                default -> throw unknown(command);
            }
            return true;
        }

        /// Returns child identity, input, and local state.
        ///
        /// @return the values
        @Override
        protected @Unmodifiable Map<String, String> values() {
            return valuesOf(
                    "child.id", "child-1",
                    "child.input", input.get(),
                    "child.local", Integer.toString(local().get())
            );
        }

        /// Returns the mounted child local.
        ///
        /// @return the local cell
        private GroupedRuntime.LocalInt local() {
            if (childLocal == null) {
                throw new IllegalStateException("Child local state is unavailable");
            }
            return childLocal;
        }
    }

    /// Implements keyed reconciliation with duplicate-key failure atomicity.
    @NotNullByDefault
    private static final class KeyedListApplication extends GroupedFixtureSession {
        /// The requested item order.
        private final MutableState<@Unmodifiable List<String>> order;

        /// The last successfully committed item order.
        private @Unmodifiable List<String> committedOrder = List.of("a", "b", "c");

        /// The local cell for each currently committed keyed item.
        private @Unmodifiable Map<String, GroupedRuntime.LocalInt> itemLocals = Map.of();

        /// Creates the keyed-list application.
        ///
        /// @param environment the fresh environment
        /// @param probe the shared probe
        private KeyedListApplication(ComparisonEnvironment environment, ComparisonProbe probe) {
            super(environment, probe);
            order = domain.mutableState(List.of("a", "b", "c"));
        }

        /// Declares each item through an explicit semantic-keyed group.
        ///
        /// @param scope the grouped scope
        @Override
        protected void compose(GroupedRuntime.Scope scope) {
            scope.node("root");
            scope.node("list");
            ArrayList<String> keys = new ArrayList<>(order.get());
            LinkedHashMap<String, GroupedRuntime.LocalInt> nextLocals = new LinkedHashMap<>();
            for (String key : keys) {
                scope.keyedGroup("list-item", key, () -> {
                    GroupedRuntime.LocalInt local = scope.rememberInt(0);
                    nextLocals.put(key, local);
                    scope.node("item:" + key);
                    scope.effect(
                            "item-lifetime",
                            () -> emit("mount:" + key),
                            () -> emit("dispose:" + key)
                    );
                });
            }
            scope.onCommit(() -> {
                committedOrder = List.copyOf(keys);
                itemLocals = Map.copyOf(nextLocals);
            });
        }

        /// Applies keyed order and local-state commands.
        ///
        /// @param command the command
        /// @return always `true`
        @Override
        protected boolean handle(FixtureCommand command) {
            switch (command.operation()) {
                case "increment-item" -> local(argument(command, "key")).increment();
                case "set-order" -> order.set(List.copyOf(Arrays.asList(argument(command, "keys").split(","))));
                default -> throw unknown(command);
            }
            return true;
        }

        /// Restores the last committed model order after a duplicate-key draft is rejected.
        ///
        /// @param failure the contained failure
        @Override
        protected void onCompositionFailure(GroupedRuntime.GroupedCompositionException failure) {
            order.set(committedOrder);
            super.onCompositionFailure(failure);
        }

        /// Returns each committed keyed local value.
        ///
        /// @return the values
        @Override
        protected @Unmodifiable Map<String, String> values() {
            LinkedHashMap<String, String> result = new LinkedHashMap<>();
            for (String key : committedOrder) {
                result.put("local." + key, Integer.toString(local(key).get()));
            }
            return Map.copyOf(result);
        }

        /// Returns one committed keyed local cell.
        ///
        /// @param key the item key
        /// @return the local cell
        private GroupedRuntime.LocalInt local(String key) {
            @Nullable GroupedRuntime.LocalInt local = itemLocals.get(key);
            if (local == null) {
                throw new IllegalArgumentException("Unknown item key: " + key);
            }
            return local;
        }
    }

    /// Implements phase-attributed bindings and a transaction-coalesced update burst.
    @NotNullByDefault
    private static final class PhaseImpactApplication extends GroupedFixtureSession {
        /// The displayed text.
        private final MutableState<String> text;

        /// The paint color.
        private final MutableState<String> color;

        /// The measured size.
        private final IntState size;

        /// The placed offset.
        private final IntState offset;

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

        /// Declares each value at its actual consuming phases.
        ///
        /// @param scope the grouped scope
        @Override
        protected void compose(GroupedRuntime.Scope scope) {
            scope.node("root");
            scope.group("phase-target", () -> {
                scope.binding(text, "text", RuntimePhase.MEASURE, RuntimePhase.PAINT, RuntimePhase.SEMANTICS);
                scope.binding(color, "color", RuntimePhase.PAINT);
                scope.binding(size, "size", RuntimePhase.MEASURE);
                scope.binding(offset, "offset", RuntimePhase.PLACE, RuntimePhase.HIT_TEST);
                scope.node("phase-target");
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

        /// Returns phase-target values and the optional burst count.
        ///
        /// @return the values
        @Override
        protected @Unmodifiable Map<String, String> values() {
            LinkedHashMap<String, String> result = new LinkedHashMap<>();
            result.put("color", color.get());
            result.put("offset", Integer.toString(offset.get()));
            result.put("size", Integer.toString(size.get()));
            result.put("text", text.get());
            if (burstApplied) {
                result.put("updates", Integer.toString(updates));
            }
            return Map.copyOf(result);
        }
    }
}

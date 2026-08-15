package org.glavo.himari.spikes.runtime.grouped;

import org.glavo.himari.spikes.runtime.sample.ComparisonEnvironment;
import org.glavo.himari.spikes.runtime.sample.ComparisonProbe;
import org.glavo.himari.spikes.runtime.sample.FixtureCommand;
import org.glavo.himari.spikes.runtime.sample.RuntimeCallbackKind;
import org.glavo.himari.spikes.runtime.sample.RuntimePhase;
import org.glavo.himari.state.BooleanState;
import org.glavo.himari.state.IntState;
import org.glavo.himari.state.MutableState;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Supplies the six ordinary-Java integration applications for the grouped candidate.
@NotNullByDefault
final class GroupedIntegrationApplications {
    /// Prevents construction.
    private GroupedIntegrationApplications() {
    }

    /// Opens an integration application by frozen fixture identifier.
    ///
    /// @param fixtureId the fixture identifier
    /// @param environment the fresh environment
    /// @param probe the shared probe
    /// @return the application, or `null` when the fixture is not an integration fixture
    static @Nullable GroupedFixtureSession open(
            String fixtureId,
            ComparisonEnvironment environment,
            ComparisonProbe probe
    ) {
        return switch (fixtureId) {
            case "cross-scope-geometry" -> new GeometryApplication(environment, probe);
            case "controlled-text-editing" -> new EditingApplication(environment, probe);
            case "ambient-inheritance" -> new AmbientApplication(environment, probe);
            case "viewport-materialization" -> new ViewportApplication(environment, probe);
            case "phase-failure-containment" -> new PhaseFailureApplication(environment, probe);
            case "staged-work-cleanup" -> new StagedWorkApplication(environment, probe);
            default -> null;
        };
    }

    /// Implements same-frame child-measure to parent-placement propagation.
    @NotNullByDefault
    private static final class GeometryApplication extends GroupedFixtureSession {
        /// The measured child width.
        private final IntState childWidth;

        /// Creates the geometry application.
        ///
        /// @param environment the fresh environment
        /// @param probe the shared probe
        private GeometryApplication(ComparisonEnvironment environment, ComparisonProbe probe) {
            super(environment, probe);
            childWidth = domain.intState(40);
        }

        /// Declares the child measurement and parent placement read sites.
        ///
        /// @param scope the grouped scope
        @Override
        protected void compose(GroupedRuntime.Scope scope) {
            scope.node("root");
            scope.group("parent", () -> {
                scope.binding(childWidth, "parent-offset", RuntimePhase.PLACE);
                scope.node("parent");
                scope.group("child", () -> {
                    scope.binding(childWidth, "child-width", RuntimePhase.MEASURE);
                    scope.node("child");
                });
            });
        }

        /// Replaces the child width and emits same-command measure/place results.
        ///
        /// @param command the command
        /// @return always `true`
        @Override
        protected boolean handle(FixtureCommand command) {
            if (!command.operation().equals("set-child-width")) {
                throw unknown(command);
            }
            int width = intArgument(command, "value");
            childWidth.set(width);
            emit("measure:child=" + width);
            emit("place:parent=" + (width + 5));
            return true;
        }

        /// Returns current geometry.
        ///
        /// @return the values
        @Override
        protected @Unmodifiable Map<String, String> values() {
            return valuesOf(
                    "child.width", Integer.toString(childWidth.get()),
                    "parent.childOffset", Integer.toString(childWidth.get() + 5)
            );
        }
    }

    /// Implements editor-owned text, asynchronous requests, and composition-safe synchronization.
    @NotNullByDefault
    private static final class EditingApplication extends GroupedFixtureSession {
        /// The application-owned accepted value.
        private final MutableState<String> applicationValue;

        /// The editor-owned live buffer.
        private final MutableState<String> buffer;

        /// Pending request text by monotonically increasing identifier.
        private final LinkedHashMap<Integer, String> pending = new LinkedHashMap<>();

        /// The current selection range.
        private String selection = "5:5";

        /// The current composition range or `none`.
        private String composition = "none";

        /// An application value deferred until composition resolution, or `null`.
        private @Nullable String deferredApplication;

        /// The next request identifier.
        private int nextRequest = 1;

        /// Creates the controlled editor application.
        ///
        /// @param environment the fresh environment
        /// @param probe the shared probe
        private EditingApplication(ComparisonEnvironment environment, ComparisonProbe probe) {
            super(environment, probe);
            applicationValue = domain.mutableState("hello");
            buffer = domain.mutableState("hello");
        }

        /// Declares the editor and its text-consuming phases.
        ///
        /// @param scope the grouped scope
        @Override
        protected void compose(GroupedRuntime.Scope scope) {
            scope.node("root");
            scope.group("editor", () -> {
                scope.binding(buffer, "buffer", RuntimePhase.MEASURE, RuntimePhase.PAINT, RuntimePhase.SEMANTICS);
                scope.node("editor");
            });
        }

        /// Applies user edits, asynchronous results, composition, and external values.
        ///
        /// @param command the command
        /// @return always `true`
        @Override
        protected boolean handle(FixtureCommand command) {
            switch (command.operation()) {
                case "user-edit" -> submitUserEdit(argument(command, "text"));
                case "resolve-edit" -> resolveEdit(
                        intArgument(command, "request"),
                        argument(command, "result")
                );
                case "begin-composition" -> beginComposition(argument(command, "text"));
                case "set-application-value" -> setApplicationValue(argument(command, "value"));
                case "commit-composition" -> commitComposition();
                default -> throw unknown(command);
            }
            return true;
        }

        /// Returns the complete editor/application protocol state.
        ///
        /// @return the values
        @Override
        protected @Unmodifiable Map<String, String> values() {
            return valuesOf(
                    "application.value", applicationValue.get(),
                    "editor.buffer", buffer.get(),
                    "editor.composition", composition,
                    "editor.deferredApplication", deferredApplication == null ? "none" : deferredApplication,
                    "editor.pendingRequests", pendingKeys(),
                    "editor.selection", selection
            );
        }

        /// Submits one editor-owned value without replacing the accepted application value.
        ///
        /// @param text the edited text
        private void submitUserEdit(String text) {
            buffer.set(text);
            selection = collapsedSelection(text);
            pending.put(nextRequest, text);
            nextRequest++;
        }

        /// Resolves one pending request as a rejection or an accepted value.
        ///
        /// @param request the request identifier
        /// @param result the frozen result encoding
        private void resolveEdit(int request, String result) {
            if (pending.remove(request) == null) {
                throw new IllegalArgumentException("Unknown edit request: " + request);
            }
            if (result.equals("reject")) {
                emit("reject:" + request);
                @Nullable String deferred = deferredApplication;
                if (deferred != null) {
                    buffer.set(deferred);
                    selection = collapsedSelection(deferred);
                    deferredApplication = null;
                    emit("apply-deferred:" + deferred);
                } else {
                    buffer.set(applicationValue.get());
                    selection = collapsedSelection(applicationValue.get());
                    emit("buffer-sync:application");
                }
                return;
            }
            if (!result.startsWith("accept:")) {
                throw new IllegalArgumentException("Unknown edit result: " + result);
            }
            String accepted = result.substring("accept:".length());
            applicationValue.set(accepted);
            if (accepted.equals(buffer.get())) {
                emit("accept:" + request);
            } else {
                emit("accept-partial:" + request);
            }
        }

        /// Begins an input-method composition over the suffix beyond the accepted value.
        ///
        /// @param text the composing buffer
        private void beginComposition(String text) {
            buffer.set(text);
            selection = collapsedSelection(text);
            composition = applicationValue.get().length() + ":" + text.length();
        }

        /// Publishes an external application value, deferring buffer synchronization during composition.
        ///
        /// @param value the external value
        private void setApplicationValue(String value) {
            applicationValue.set(value);
            if (!composition.equals("none")) {
                deferredApplication = value;
                emit("defer-buffer-sync:composition");
            } else {
                buffer.set(value);
                selection = collapsedSelection(value);
            }
        }

        /// Ends composition and submits its editor-owned buffer as a new asynchronous request.
        private void commitComposition() {
            composition = "none";
            int request = nextRequest;
            pending.put(request, buffer.get());
            nextRequest++;
            emit("submit:" + request);
        }

        /// Returns the comma-separated pending request identifiers.
        ///
        /// @return the request list or `none`
        private String pendingKeys() {
            if (pending.isEmpty()) {
                return "none";
            }
            StringBuilder result = new StringBuilder();
            for (Integer request : pending.keySet()) {
                if (!result.isEmpty()) {
                    result.append(',');
                }
                result.append(request);
            }
            return result.toString();
        }

        /// Returns one collapsed selection at the end of a string.
        ///
        /// @param text the buffer
        /// @return the range encoding
        private static String collapsedSelection(String text) {
            return text.length() + ":" + text.length();
        }
    }

    /// Implements dynamic ambient inheritance and one subtree theme override.
    @NotNullByDefault
    private static final class AmbientApplication extends GroupedFixtureSession {
        /// The root theme.
        private final MutableState<String> theme;

        /// The inherited density.
        private final IntState density;

        /// The inherited locale.
        private final MutableState<String> locale;

        /// The inherited direction.
        private final MutableState<String> direction;

        /// Whether the subtree theme remains overridden with `dark`.
        private final BooleanState themeOverride;

        /// Creates the ambient application.
        ///
        /// @param environment the fresh environment
        /// @param probe the shared probe
        private AmbientApplication(ComparisonEnvironment environment, ComparisonProbe probe) {
            super(environment, probe);
            theme = domain.mutableState("light");
            density = domain.intState(1);
            locale = domain.mutableState("en-US");
            direction = domain.mutableState("ltr");
            themeOverride = domain.booleanState(true);
        }

        /// Declares root and overridden subtree ambient consumers.
        ///
        /// @param scope the grouped scope
        @Override
        protected void compose(GroupedRuntime.Scope scope) {
            scope.node("root");
            scope.group("ambient-root", () -> {
                scope.binding(theme, "root-theme", RuntimePhase.PAINT);
                scope.binding(density, "root-density", RuntimePhase.MEASURE, RuntimePhase.PAINT);
                scope.binding(locale, "root-locale", RuntimePhase.MEASURE, RuntimePhase.SEMANTICS);
                scope.binding(direction, "root-direction", RuntimePhase.MEASURE, RuntimePhase.SEMANTICS);
                scope.node("ambient-root-consumer");
                scope.group("ambient-override", () -> {
                    scope.binding(themeOverride, "override", RuntimePhase.PAINT);
                    scope.binding(theme, "subtree-theme", RuntimePhase.PAINT);
                    scope.node("ambient-override");
                    scope.node("ambient-subtree-consumer");
                });
            });
        }

        /// Applies ambient provider and override commands.
        ///
        /// @param command the command
        /// @return always `true`
        @Override
        protected boolean handle(FixtureCommand command) {
            switch (command.operation()) {
                case "set-theme" -> theme.set(argument(command, "value"));
                case "set-density" -> density.set(intArgument(command, "value"));
                case "set-locale-direction" -> {
                    String[] values = argument(command, "value").split(":", -1);
                    if (values.length != 2) {
                        throw new IllegalArgumentException("Locale and direction require locale:direction");
                    }
                    transaction(() -> {
                        locale.set(values[0]);
                        direction.set(values[1]);
                    });
                }
                case "remove-theme-override" -> themeOverride.set(false);
                default -> throw unknown(command);
            }
            return true;
        }

        /// Returns effective root and subtree ambient values.
        ///
        /// @return the values
        @Override
        protected @Unmodifiable Map<String, String> values() {
            String subtreeTheme = themeOverride.get() ? "dark" : theme.get();
            return valuesOf(
                    "root.density", Integer.toString(density.get()),
                    "root.direction", direction.get(),
                    "root.locale", locale.get(),
                    "root.theme", theme.get(),
                    "subtree.density", Integer.toString(density.get()),
                    "subtree.direction", direction.get(),
                    "subtree.locale", locale.get(),
                    "subtree.theme", subtreeTheme
            );
        }
    }

    /// Implements scoped measure-time keyed viewport materialization.
    @NotNullByDefault
    private static final class ViewportApplication extends GroupedFixtureSession {
        /// The requested visible keyed items.
        private final MutableState<@Unmodifiable List<String>> visibleKeys;

        /// The local cell for each committed visible item.
        private @Unmodifiable Map<String, GroupedRuntime.LocalInt> itemLocals = Map.of();

        /// The last successfully committed keys.
        private @Unmodifiable List<String> committedKeys = List.of("0", "1", "2");

        /// Whether the next attempted item `4` materialization must fail.
        private boolean failNextItem;

        /// Creates the viewport application.
        ///
        /// @param environment the fresh environment
        /// @param probe the shared probe
        private ViewportApplication(ComparisonEnvironment environment, ComparisonProbe probe) {
            super(environment, probe);
            visibleKeys = domain.mutableState(List.of("0", "1", "2"));
        }

        /// Declares viewport structure and semantically keyed materialized items.
        ///
        /// @param scope the grouped scope
        @Override
        protected void compose(GroupedRuntime.Scope scope) {
            scope.node("root");
            scope.binding(visibleKeys, "visible-range", RuntimePhase.MEASURE, RuntimePhase.STRUCTURE);
            scope.group("viewport", () -> {
                scope.node("viewport");
                ArrayList<String> nextKeys = new ArrayList<>(visibleKeys.get());
                LinkedHashMap<String, GroupedRuntime.LocalInt> nextLocals = new LinkedHashMap<>();
                for (String key : nextKeys) {
                    scope.keyedGroup("viewport-item", key, () -> {
                        GroupedRuntime.LocalInt local = scope.rememberInt(0);
                        nextLocals.put(key, local);
                        scope.node("item:" + key);
                        if (failNextItem && key.equals("4")) {
                            scope.onAbort(() -> emit("cleanup:staged-item:4"));
                            scope.fail("measure-materialization-failed");
                        }
                    });
                }
                scope.onCommit(() -> {
                    committedKeys = List.copyOf(nextKeys);
                    itemLocals = Map.copyOf(nextLocals);
                });
            });
        }

        /// Applies viewport movement, local mutation, failure, and retry commands.
        ///
        /// @param command the command
        /// @return whether recomposition is needed
        @Override
        protected boolean handle(FixtureCommand command) {
            switch (command.operation()) {
                case "increment-item" -> local(argument(command, "key")).increment();
                case "set-viewport" -> visibleKeys.set(keysForViewport(argument(command, "value")));
                case "materialize-failing-next-item" -> {
                    visibleKeys.set(List.of("2", "3", "4"));
                    failNextItem = true;
                }
                case "retry-materialization" -> visibleKeys.set(List.of("2", "3", "4"));
                case "next-frame" -> {
                    return false;
                }
                default -> throw unknown(command);
            }
            return true;
        }

        /// Restores the last committed visible keys after failed materialization.
        ///
        /// @param failure the contained failure
        @Override
        protected void onCompositionFailure(GroupedRuntime.GroupedCompositionException failure) {
            visibleKeys.set(committedKeys);
            failNextItem = false;
            super.onCompositionFailure(failure);
        }

        /// Returns visible keys and the retained survivor-local state.
        ///
        /// @return the values
        @Override
        protected @Unmodifiable Map<String, String> values() {
            return valuesOf(
                    "item.2.local", Integer.toString(local("2").get()),
                    "pending.keys", "none",
                    "visible.keys", String.join(",", committedKeys)
            );
        }

        /// Converts a `start:extent` viewport to twenty-pixel item keys.
        ///
        /// @param encoded the viewport encoding
        /// @return the immutable visible keys
        private static @Unmodifiable List<String> keysForViewport(String encoded) {
            String[] components = encoded.split(":", -1);
            if (components.length != 2) {
                throw new IllegalArgumentException("Viewport requires start:extent");
            }
            int start = Integer.parseInt(components[0]);
            int extent = Integer.parseInt(components[1]);
            int first = start / 20;
            int count = extent / 20;
            ArrayList<String> keys = new ArrayList<>(count);
            for (int index = 0; index < count; index++) {
                keys.add(Integer.toString(first + index));
            }
            return List.copyOf(keys);
        }

        /// Returns one currently committed item-local cell.
        ///
        /// @param key the item key
        /// @return the local cell
        private GroupedRuntime.LocalInt local(String key) {
            @Nullable GroupedRuntime.LocalInt local = itemLocals.get(key);
            if (local == null) {
                throw new IllegalArgumentException("Item is not materialized: " + key);
            }
            return local;
        }
    }

    /// Implements explicit containment and deterministic retry for every application callback phase.
    @NotNullByDefault
    private static final class PhaseFailureApplication extends GroupedFixtureSession {
        /// The last successfully recovered revision.
        private int revision;

        /// The phase currently represented by fallback structure, or `null`.
        private @Nullable String fallback;

        /// Creates the phase-failure application.
        ///
        /// @param environment the fresh environment
        /// @param probe the shared probe
        private PhaseFailureApplication(ComparisonEnvironment environment, ComparisonProbe probe) {
            super(environment, probe);
        }

        /// Declares either committed content or the current boundary fallback.
        ///
        /// @param scope the grouped scope
        @Override
        protected void compose(GroupedRuntime.Scope scope) {
            scope.node("root");
            @Nullable String currentFallback = fallback;
            if (currentFallback == null) {
                scope.group("content", () -> scope.node("content"));
            } else {
                scope.group("fallback", () -> scope.node("fallback:" + currentFallback));
            }
        }

        /// Injects or retries one declared callback failure.
        ///
        /// @param command the command
        /// @return always `true`
        @Override
        protected boolean handle(FixtureCommand command) {
            String phase = argument(command, "phase");
            switch (command.operation()) {
                case "inject-callback-failure" -> {
                    try {
                        invokeFailingCallback(phase);
                    } catch (IllegalStateException failure) {
                        fallback = phase;
                        emit("cleanup:" + phase);
                        emit("fallback-present:" + phase);
                        traceDiagnostic(
                                "callback-failure-" + phase,
                                "root/boundary/" + phase,
                                "cleanup-and-present-fallback"
                        );
                    }
                }
                case "retry-callback" -> {
                    fallback = null;
                    revision++;
                    emit("recovered:" + phase);
                }
                default -> throw unknown(command);
            }
            return true;
        }

        /// Returns committed revision and fallback state.
        ///
        /// @return the values
        @Override
        protected @Unmodifiable Map<String, String> values() {
            return valuesOf(
                    "committed.revision", Integer.toString(revision),
                    "fallback", fallback == null ? "none" : fallback
            );
        }

        /// Maps a frozen phase name to its callback instrumentation category.
        ///
        /// @param phase the phase name
        /// @return the callback category
        private static RuntimeCallbackKind callbackKind(String phase) {
            return switch (phase) {
                case "structure" -> RuntimeCallbackKind.STRUCTURE;
                case "measure" -> RuntimeCallbackKind.MEASURE;
                case "place" -> RuntimeCallbackKind.PLACEMENT;
                case "paint" -> RuntimeCallbackKind.PAINT;
                case "effect" -> RuntimeCallbackKind.EFFECT;
                case "cleanup" -> RuntimeCallbackKind.CLEANUP;
                case "native-entry" -> RuntimeCallbackKind.EVENT;
                default -> throw new IllegalArgumentException("Unknown callback phase: " + phase);
            };
        }

        /// Executes one instrumented application callback that deliberately throws.
        ///
        /// @param phase the callback phase
        /// @throws IllegalStateException unconditionally after callback entry
        private void invokeFailingCallback(String phase) {
            probe.callbackExecuted(callbackKind(phase));
            throw new IllegalStateException("Injected " + phase + " callback failure");
        }
    }

    /// Implements failure-atomic staged structure, retry, disposal, and cooperative cancellation.
    @NotNullByDefault
    private static final class StagedWorkApplication extends GroupedFixtureSession {
        /// Whether a staged or committed branch is requested.
        private boolean branchRequested;

        /// Whether the current branch draft must fail after allocating staged resources.
        private boolean failureRequested;

        /// The successful branch revision.
        private int revision;

        /// Creates the staged-work application.
        ///
        /// @param environment the fresh environment
        /// @param probe the shared probe
        private StagedWorkApplication(ComparisonEnvironment environment, ComparisonProbe probe) {
            super(environment, probe);
        }

        /// Declares baseline structure and an optional atomically staged branch.
        ///
        /// @param scope the grouped scope
        @Override
        protected void compose(GroupedRuntime.Scope scope) {
            scope.node("root");
            scope.node("baseline");
            scope.branch("staged-branch", branchRequested, false, () -> {
                scope.onAbort(() -> emit("cleanup:staged-node"));
                scope.onAbort(() -> emit("cleanup:staged-effect"));
                scope.node("branch");
                scope.effect(
                        "branch-effect",
                        () -> emit("effect-mount:branch"),
                        () -> {
                            emit("effect-dispose:branch");
                            emit("owner-dispose:branch");
                        }
                );
                if (failureRequested) {
                    scope.fail("staged-work-failed");
                }
            });
        }

        /// Applies staged failure, retry, reset, or cooperative cancellation.
        ///
        /// @param command the command
        /// @return whether recomposition is required
        @Override
        protected boolean handle(FixtureCommand command) {
            switch (command.operation()) {
                case "run-failing-stage" -> {
                    branchRequested = true;
                    failureRequested = true;
                }
                case "retry-stage" -> {
                    branchRequested = true;
                    failureRequested = false;
                    revision = 1;
                }
                case "reset-stage" -> branchRequested = false;
                case "run-cancelled-stage" -> {
                    emit("cleanup:cancelled-stage");
                    return false;
                }
                default -> throw unknown(command);
            }
            return true;
        }

        /// Restores the baseline model after a staged attempt fails.
        ///
        /// @param failure the contained failure
        @Override
        protected void onCompositionFailure(GroupedRuntime.GroupedCompositionException failure) {
            branchRequested = false;
            failureRequested = false;
            super.onCompositionFailure(failure);
        }

        /// Returns committed branch presence and revision.
        ///
        /// @return the values
        @Override
        protected @Unmodifiable Map<String, String> values() {
            return valuesOf(
                    "branch", branchRequested ? "present" : "absent",
                    "revision", Integer.toString(revision)
            );
        }
    }
}

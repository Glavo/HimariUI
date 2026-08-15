package org.glavo.himari.spikes.runtime.oneshot;

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

/// Supplies the six ordinary-Java integration applications for the one-shot candidate.
@NotNullByDefault
final class OneShotIntegrationApplications {
    /// Prevents construction.
    private OneShotIntegrationApplications() {
    }

    /// Opens an integration application by frozen fixture identifier.
    ///
    /// @param fixtureId the fixture identifier
    /// @param environment the fresh environment
    /// @param probe the shared probe
    /// @return the application, or `null` when the fixture is not an integration fixture
    static @Nullable OneShotFixtureSession open(
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
    private static final class GeometryApplication extends OneShotFixtureSession {
        /// The measured child-width source.
        private final IntState childWidth;

        /// The child width written by the measure binding.
        private int measuredWidth;

        /// The child offset written by the placement binding.
        private int placedOffset;

        /// Creates the geometry application.
        ///
        /// @param environment the fresh environment
        /// @param probe the shared probe
        private GeometryApplication(ComparisonEnvironment environment, ComparisonProbe probe) {
            super(environment, probe);
            childWidth = domain.intState(40);
        }

        /// Mounts persistent parent and child owners with phase-local bindings.
        ///
        /// @param root the root owner
        @Override
        protected void initialize(OneShotRuntime.Owner root) {
            root.node("root");
            root.component("parent-owner", parent -> {
                parent.bind(
                        childWidth,
                        "parent-offset",
                        () -> placedOffset = Math.addExact(childWidth.get(), 5),
                        RuntimePhase.PLACE
                );
                parent.node("parent");
                parent.component("child-owner", child -> {
                    child.bind(
                            childWidth,
                            "child-width",
                            () -> measuredWidth = childWidth.get(),
                            RuntimePhase.MEASURE
                    );
                    child.node("child");
                });
            });
        }

        /// Replaces the child width and reports same-command measure/place results.
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

        /// Returns bound measure and placement properties.
        ///
        /// @return the values
        @Override
        protected @Unmodifiable Map<String, String> values() {
            return valuesOf(
                    "child.width", Integer.toString(measuredWidth),
                    "parent.childOffset", Integer.toString(placedOffset)
            );
        }
    }

    /// Implements editor-owned text, asynchronous requests, and composition-safe synchronization.
    @NotNullByDefault
    private static final class EditingApplication extends OneShotFixtureSession {
        /// The application-owned accepted value.
        private final MutableState<String> applicationValue;

        /// The editor-owned live buffer.
        private final MutableState<String> buffer;

        /// Pending request text by monotonically increasing identifier.
        private final LinkedHashMap<Integer, String> pending = new LinkedHashMap<>();

        /// The buffer property last written by the editor binding.
        private String renderedBuffer = "";

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

        /// Mounts the persistent editor node and its text property binding.
        ///
        /// @param root the root owner
        @Override
        protected void initialize(OneShotRuntime.Owner root) {
            root.node("root");
            root.bind(
                    buffer,
                    "editor-buffer",
                    () -> renderedBuffer = buffer.get(),
                    RuntimePhase.MEASURE,
                    RuntimePhase.PAINT,
                    RuntimePhase.SEMANTICS
            );
            root.node("editor");
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
                    "editor.buffer", renderedBuffer,
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

        /// Publishes an external application value, deferring synchronization during composition.
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

        /// Ends composition and submits its editor-owned buffer as an asynchronous request.
        private void commitComposition() {
            composition = "none";
            int request = nextRequest;
            pending.put(request, buffer.get());
            nextRequest++;
            emit("submit:" + request);
        }

        /// Returns comma-separated pending request identifiers.
        ///
        /// @return the request identifiers or `none`
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
    private static final class AmbientApplication extends OneShotFixtureSession {
        /// The root theme source.
        private final MutableState<String> theme;

        /// The inherited density source.
        private final IntState density;

        /// The inherited locale source.
        private final MutableState<String> locale;

        /// The inherited direction source.
        private final MutableState<String> direction;

        /// Whether the subtree theme remains overridden with `dark`.
        private final BooleanState themeOverride;

        /// The root theme property.
        private String rootTheme = "";

        /// The effective subtree theme property.
        private String subtreeTheme = "";

        /// The inherited density property.
        private int renderedDensity;

        /// The inherited locale property.
        private String renderedLocale = "";

        /// The inherited direction property.
        private String renderedDirection = "";

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

        /// Mounts root and subtree consumers with direct ambient property bindings.
        ///
        /// @param root the root owner
        @Override
        protected void initialize(OneShotRuntime.Owner root) {
            root.node("root");
            root.component("ambient-root-owner", ambientRoot -> {
                ambientRoot.bind(theme, "root-theme", () -> rootTheme = theme.get(), RuntimePhase.PAINT);
                ambientRoot.bind(
                        density,
                        "root-density",
                        () -> renderedDensity = density.get(),
                        RuntimePhase.MEASURE,
                        RuntimePhase.PAINT
                );
                ambientRoot.bind(
                        locale,
                        "root-locale",
                        () -> renderedLocale = locale.get(),
                        RuntimePhase.MEASURE,
                        RuntimePhase.SEMANTICS
                );
                ambientRoot.bind(
                        direction,
                        "root-direction",
                        () -> renderedDirection = direction.get(),
                        RuntimePhase.MEASURE,
                        RuntimePhase.SEMANTICS
                );
                ambientRoot.node("ambient-root-consumer");
                ambientRoot.component("ambient-override-owner", override -> {
                    override.bind(theme, "subtree-theme-source", this::updateSubtreeTheme, RuntimePhase.PAINT);
                    override.bind(themeOverride, "subtree-theme-override", this::updateSubtreeTheme, RuntimePhase.PAINT);
                    override.node("ambient-override");
                    override.node("ambient-subtree-consumer");
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

        /// Returns effective root and subtree ambient properties.
        ///
        /// @return the values
        @Override
        protected @Unmodifiable Map<String, String> values() {
            return valuesOf(
                    "root.density", Integer.toString(renderedDensity),
                    "root.direction", renderedDirection,
                    "root.locale", renderedLocale,
                    "root.theme", rootTheme,
                    "subtree.density", Integer.toString(renderedDensity),
                    "subtree.direction", renderedDirection,
                    "subtree.locale", renderedLocale,
                    "subtree.theme", subtreeTheme
            );
        }

        /// Updates the subtree theme from the root source and the local override.
        private void updateSubtreeTheme() {
            subtreeTheme = themeOverride.get() ? "dark" : theme.get();
        }
    }

    /// Implements explicit previous-viewport materialization with one-frame convergence.
    @NotNullByDefault
    private static final class ViewportApplication extends OneShotFixtureSession {
        /// The latest viewport-derived key request.
        private final MutableState<@Unmodifiable List<String>> requestedKeys;

        /// The keys published to the structural controller on a frame boundary.
        private final MutableState<@Unmodifiable List<String>> materializedKeys;

        /// The next-frame key set, or `null` when the request already matches committed structure.
        private @Nullable @Unmodifiable List<String> pendingKeys;

        /// The explicit keyed viewport controller, or `null` before mount.
        private @Nullable OneShotRuntime.KeyedItems<String, OneShotRuntime.LocalInt> items;

        /// Whether the next attempted item `4` initializer must fail.
        private boolean failNextItem;

        /// Creates the previous-viewport application.
        ///
        /// @param environment the fresh environment
        /// @param probe the shared probe
        private ViewportApplication(ComparisonEnvironment environment, ComparisonProbe probe) {
            super(environment, probe);
            requestedKeys = domain.mutableState(List.of("0", "1", "2"));
            materializedKeys = domain.mutableState(List.of("0", "1", "2"));
        }

        /// Mounts the viewport request binding and its separate keyed materialization anchor.
        ///
        /// @param root the root owner
        @Override
        protected void initialize(OneShotRuntime.Owner root) {
            root.node("root");
            root.node("viewport");
            root.bind(
                    requestedKeys,
                    "viewport-request",
                    this::updatePendingKeys,
                    RuntimePhase.MEASURE
            );
            items = root.forEach(
                    "viewport-items",
                    materializedKeys,
                    materializedKeys::get,
                    (item, key) -> {
                        OneShotRuntime.LocalInt local = item.localInt(0);
                        item.node("item:" + key);
                        if (failNextItem && key.equals("4")) {
                            item.onAbort(() -> emit("cleanup:staged-item:4"));
                            item.fail("measure-materialization-failed");
                        }
                        return local;
                    },
                    RuntimePhase.STRUCTURE
            );
        }

        /// Applies viewport movement, local mutation, failure, retry, and frame settlement.
        ///
        /// @param command the command
        /// @return whether a source change must be flushed
        @Override
        protected boolean handle(FixtureCommand command) {
            return switch (command.operation()) {
                case "increment-item" -> {
                    local(argument(command, "key")).increment();
                    yield false;
                }
                case "set-viewport" -> {
                    requestedKeys.set(keysForViewport(argument(command, "value")));
                    yield true;
                }
                case "next-frame" -> {
                    @Nullable List<String> pending = pendingKeys;
                    if (pending != null) {
                        pendingKeys = null;
                        materializedKeys.set(pending);
                    }
                    yield true;
                }
                case "materialize-failing-next-item" -> {
                    pendingKeys = null;
                    failNextItem = true;
                    materializedKeys.set(List.of("2", "3", "4"));
                    yield true;
                }
                case "retry-materialization" -> {
                    failNextItem = false;
                    materializedKeys.set(List.of("2", "3", "4"));
                    yield true;
                }
                default -> throw unknown(command);
            };
        }

        /// Restores the materialization source after a rejected staged item owner.
        ///
        /// @param failure the contained failure
        @Override
        protected void onMutationFailure(OneShotRuntime.OneShotMutationException failure) {
            materializedKeys.set(controller().keys());
            pendingKeys = null;
            failNextItem = false;
            super.onMutationFailure(failure);
        }

        /// Returns committed and pending viewport keys plus survivor-local state.
        ///
        /// @return the values
        @Override
        protected @Unmodifiable Map<String, String> values() {
            @Nullable List<String> pending = pendingKeys;
            return valuesOf(
                    "item.2.local", Integer.toString(local("2").get()),
                    "pending.keys", pending == null ? "none" : String.join(",", pending),
                    "visible.keys", String.join(",", controller().keys())
            );
        }

        /// Recomputes the next-frame request without changing committed item owners.
        private void updatePendingKeys() {
            List<String> requested = requestedKeys.get();
            pendingKeys = requested.equals(controller().keys()) ? null : requested;
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
            int first = Integer.parseInt(components[0]) / 20;
            int count = Integer.parseInt(components[1]) / 20;
            ArrayList<String> keys = new ArrayList<>(count);
            for (int index = 0; index < count; index++) {
                keys.add(Integer.toString(first + index));
            }
            return List.copyOf(keys);
        }

        /// Returns the mounted keyed viewport controller.
        ///
        /// @return the controller
        private OneShotRuntime.KeyedItems<String, OneShotRuntime.LocalInt> controller() {
            if (items == null) {
                throw new IllegalStateException("Viewport controller is unavailable");
            }
            return items;
        }

        /// Returns one currently committed item-local cell.
        ///
        /// @param key the item key
        /// @return the cell
        private OneShotRuntime.LocalInt local(String key) {
            @Nullable OneShotRuntime.LocalInt local = controller().value(key);
            if (local == null) {
                throw new IllegalArgumentException("Item is not materialized: " + key);
            }
            return local;
        }
    }

    /// Implements explicit containment and deterministic retry for every callback phase.
    @NotNullByDefault
    private static final class PhaseFailureApplication extends OneShotFixtureSession {
        /// The current fallback phase or the sentinel `none`.
        private final MutableState<String> fallback;

        /// The last successfully recovered revision.
        private int revision;

        /// Creates the phase-failure application.
        ///
        /// @param environment the fresh environment
        /// @param probe the shared probe
        private PhaseFailureApplication(ComparisonEnvironment environment, ComparisonProbe probe) {
            super(environment, probe);
            fallback = domain.mutableState("none");
        }

        /// Mounts mutually exclusive content and fallback structural controls.
        ///
        /// @param root the root owner
        @Override
        protected void initialize(OneShotRuntime.Owner root) {
            root.node("root");
            root.show(
                    "stable-content",
                    fallback,
                    () -> fallback.get().equals("none"),
                    OneShotRuntime.Retention.DISPOSE,
                    content -> {
                        content.node("content");
                        return Boolean.TRUE;
                    },
                    RuntimePhase.STRUCTURE
            );
            root.forEach(
                    "fallback-content",
                    fallback,
                    () -> fallback.get().equals("none") ? List.of() : List.of(fallback.get()),
                    (content, phase) -> {
                        content.node("fallback:" + phase);
                        return Boolean.TRUE;
                    },
                    RuntimePhase.STRUCTURE
            );
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
                        fallback.set(phase);
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
                    fallback.set("none");
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
                    "fallback", fallback.get()
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

    /// Implements failure-atomic staged structure, deterministic retry, and disposal.
    @NotNullByDefault
    private static final class StagedWorkApplication extends OneShotFixtureSession {
        /// Whether a branch owner is requested.
        private final BooleanState branchRequested;

        /// Whether the next branch initializer must fail after staging resources.
        private boolean failureRequested;

        /// The successful branch revision.
        private int revision;

        /// The staged branch controller, or `null` before mount.
        private @Nullable OneShotRuntime.Show<Boolean> branch;

        /// Creates the staged-work application.
        ///
        /// @param environment the fresh environment
        /// @param probe the shared probe
        private StagedWorkApplication(ComparisonEnvironment environment, ComparisonProbe probe) {
            super(environment, probe);
            branchRequested = domain.booleanState(false);
        }

        /// Mounts baseline structure and one atomically staged branch anchor.
        ///
        /// @param root the root owner
        @Override
        protected void initialize(OneShotRuntime.Owner root) {
            root.node("root");
            root.node("baseline");
            branch = root.show(
                    "staged-branch",
                    branchRequested,
                    branchRequested::get,
                    OneShotRuntime.Retention.DISPOSE,
                    staged -> {
                        staged.onAbort(() -> emit("cleanup:staged-node"));
                        staged.onAbort(() -> emit("cleanup:staged-effect"));
                        staged.node("branch");
                        staged.effect(
                                () -> emit("effect-mount:branch"),
                                () -> {
                                    emit("effect-dispose:branch");
                                    emit("owner-dispose:branch");
                                }
                        );
                        if (failureRequested) {
                            staged.fail("staged-work-failed");
                        }
                        return Boolean.TRUE;
                    },
                    RuntimePhase.STRUCTURE
            );
        }

        /// Applies staged failure, retry, reset, or an unclaimed cancellation command.
        ///
        /// @param command the command
        /// @return whether branch reconciliation is required
        @Override
        protected boolean handle(FixtureCommand command) {
            switch (command.operation()) {
                case "run-failing-stage" -> {
                    failureRequested = true;
                    branchRequested.set(true);
                }
                case "retry-stage" -> {
                    failureRequested = false;
                    revision = 1;
                    branchRequested.set(true);
                }
                case "reset-stage" -> branchRequested.set(false);
                case "run-cancelled-stage" -> {
                    emit("cleanup:cancelled-stage");
                    return false;
                }
                default -> throw unknown(command);
            }
            return true;
        }

        /// Restores the request model after a staged branch initializer fails.
        ///
        /// @param failure the contained failure
        @Override
        protected void onMutationFailure(OneShotRuntime.OneShotMutationException failure) {
            branchRequested.set(false);
            failureRequested = false;
            super.onMutationFailure(failure);
        }

        /// Returns committed branch presence and revision.
        ///
        /// @return the values
        @Override
        protected @Unmodifiable Map<String, String> values() {
            return valuesOf(
                    "branch", controller().visible() ? "present" : "absent",
                    "revision", Integer.toString(revision)
            );
        }

        /// Returns the mounted staged branch controller.
        ///
        /// @return the controller
        private OneShotRuntime.Show<Boolean> controller() {
            if (branch == null) {
                throw new IllegalStateException("Staged branch controller is unavailable");
            }
            return branch;
        }
    }
}

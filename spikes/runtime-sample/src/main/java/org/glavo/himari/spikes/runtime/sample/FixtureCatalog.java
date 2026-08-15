package org.glavo.himari.spikes.runtime.sample;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/// Provides the versioned behavior contract shared by every M1 structural-runtime candidate.
@NotNullByDefault
public final class FixtureCatalog {
    /// The suite version written into every report and evidence artifact.
    public static final String VERSION = "runtime-comparison-suite-v1";

    /// The immutable fixture order used by reports and decision checkpoints.
    private static final @Unmodifiable List<FixtureDefinition> FIXTURES = List.of(
            counterFixture(),
            diamondFixture(),
            conditionalFixture(),
            changingInputFixture(),
            keyedListFixture(),
            phaseImpactFixture(),
            geometryPropagationFixture(),
            controlledEditingFixture(),
            ambientFixture(),
            viewportFixture(),
            phaseFailureFixture(),
            stagedFailureFixture(),
            realisticApplicationFixture()
    );

    /// Prevents construction.
    private FixtureCatalog() {
    }

    /// Returns every frozen fixture in canonical execution order.
    ///
    /// @return the immutable fixture list
    public static @Unmodifiable List<FixtureDefinition> fixtures() {
        return FIXTURES;
    }

    /// Returns one fixture by stable identifier.
    ///
    /// @param id the fixture identifier
    /// @return the fixture
    /// @throws IllegalArgumentException if no fixture has that identifier
    public static FixtureDefinition fixture(String id) {
        Objects.requireNonNull(id, "id");
        for (FixtureDefinition fixture : FIXTURES) {
            if (fixture.id().equals(id)) {
                return fixture;
            }
        }
        throw new IllegalArgumentException("Unknown runtime comparison fixture: " + id);
    }

    /// Creates the counter, derived-label, and event-handler fixture.
    ///
    /// @return the fixture
    private static FixtureDefinition counterFixture() {
        return fixture(
                "counter-derived-handler",
                FixtureStage.MICRO,
                "Counter events update source state and one derived label without freezing the handler input.",
                Set.of("changing-value", "derived-value", "event-handler"),
                List.of(
                        step("mount", cmd("mount"), obs(
                                values("count", "0", "label", "Count: 0", "step", "1"),
                                nodes("root", "counter-label", "increment-button")
                        )),
                        step("increment-one", cmd("activate-increment"), obs(
                                values("count", "1", "label", "Count: 1", "step", "1"),
                                nodes("root", "counter-label", "increment-button"),
                                events("handler:increment")
                        )),
                        step("change-handler-input", cmd("set-step", "value", "2"), obs(
                                values("count", "1", "label", "Count: 1", "step", "2"),
                                nodes("root", "counter-label", "increment-button")
                        )),
                        step("increment-two", cmd("activate-increment"), obs(
                                values("count", "3", "label", "Count: 3", "step", "2"),
                                nodes("root", "counter-label", "increment-button"),
                                events("handler:increment")
                        ))
                ),
                benchmark(cmd("set-count", "value", "1"), cmd("set-count", "value", "0"))
        );
    }

    /// Creates the glitch-free diamond fixture.
    ///
    /// @return the fixture
    private static FixtureDefinition diamondFixture() {
        return fixture(
                "diamond-glitch",
                FixtureStage.MICRO,
                "A diamond graph publishes only the stable downstream value for one source epoch.",
                Set.of("glitch-free", "semantic-version", "transaction"),
                List.of(
                        step("mount", cmd("mount"), obs(
                                values("source", "1", "left", "2", "right", "3", "sum", "5"),
                                nodes("root", "diamond-output")
                        )),
                        step("publish-two", cmd("set-source", "value", "2"), obs(
                                values("source", "2", "left", "4", "right", "6", "sum", "10"),
                                nodes("root", "diamond-output"),
                                events("observer:sum=10")
                        )),
                        step("semantic-noop", cmd("set-source", "value", "2"), obs(
                                values("source", "2", "left", "4", "right", "6", "sum", "10"),
                                nodes("root", "diamond-output")
                        ))
                ),
                benchmark(cmd("set-source", "value", "3"), cmd("set-source", "value", "1"))
        );
    }

    /// Creates the explicit retain-versus-dispose conditional fixture.
    ///
    /// @return the fixture
    private static FixtureDefinition conditionalFixture() {
        return fixture(
                "conditional-lifecycle",
                FixtureStage.MICRO,
                "Conditional scopes honor explicit retained and disposed local-state policies while effects unmount.",
                Set.of("branch-identity", "effect-disposal", "local-state"),
                List.of(
                        step("mount", cmd("mount"), obs(
                                values(
                                        "disposed.local", "absent", "disposed.visible", "false",
                                        "retained.local", "absent", "retained.visible", "false"
                                ),
                                nodes("root", "branch-controls")
                        )),
                        step("show-retained", cmd("show-retained"), obs(
                                values(
                                        "disposed.local", "absent", "disposed.visible", "false",
                                        "retained.local", "0", "retained.visible", "true"
                                ),
                                nodes("root", "branch-controls", "retained-panel"),
                                events("effect-mount:retained")
                        )),
                        step("mutate-retained", cmd("increment-retained"), obs(
                                values(
                                        "disposed.local", "absent", "disposed.visible", "false",
                                        "retained.local", "1", "retained.visible", "true"
                                ),
                                nodes("root", "branch-controls", "retained-panel")
                        )),
                        step("hide-retained", cmd("hide-retained"), obs(
                                values(
                                        "disposed.local", "absent", "disposed.visible", "false",
                                        "retained.local", "1", "retained.visible", "false"
                                ),
                                nodes("root", "branch-controls"),
                                events("effect-dispose:retained")
                        )),
                        step("restore-retained", cmd("show-retained"), obs(
                                values(
                                        "disposed.local", "absent", "disposed.visible", "false",
                                        "retained.local", "1", "retained.visible", "true"
                                ),
                                nodes("root", "branch-controls", "retained-panel"),
                                events("effect-mount:retained")
                        )),
                        step("show-disposed", cmd("show-disposed"), obs(
                                values(
                                        "disposed.local", "0", "disposed.visible", "true",
                                        "retained.local", "1", "retained.visible", "true"
                                ),
                                nodes("root", "branch-controls", "retained-panel", "disposed-panel"),
                                events("effect-mount:disposed")
                        )),
                        step("mutate-disposed", cmd("increment-disposed"), obs(
                                values(
                                        "disposed.local", "1", "disposed.visible", "true",
                                        "retained.local", "1", "retained.visible", "true"
                                ),
                                nodes("root", "branch-controls", "retained-panel", "disposed-panel")
                        )),
                        step("hide-disposed", cmd("hide-disposed"), obs(
                                values(
                                        "disposed.local", "absent", "disposed.visible", "false",
                                        "retained.local", "1", "retained.visible", "true"
                                ),
                                nodes("root", "branch-controls", "retained-panel"),
                                events("effect-dispose:disposed", "owner-dispose:disposed")
                        )),
                        step("recreate-disposed", cmd("show-disposed"), obs(
                                values(
                                        "disposed.local", "0", "disposed.visible", "true",
                                        "retained.local", "1", "retained.visible", "true"
                                ),
                                nodes("root", "branch-controls", "retained-panel", "disposed-panel"),
                                events("effect-mount:disposed")
                        ))
                ),
                benchmark(cmd("show-retained"), cmd("hide-retained"))
        );
    }

    /// Creates the changing nested-component input fixture.
    ///
    /// @return the fixture
    private static FixtureDefinition changingInputFixture() {
        return fixture(
                "changing-component-input",
                FixtureStage.MICRO,
                "A mounted child observes changing parent input while preserving its identity and local state.",
                Set.of("component-input", "local-state", "stable-identity"),
                List.of(
                        step("mount", cmd("mount"), obs(
                                values("child.id", "child-1", "child.input", "alpha", "child.local", "0"),
                                nodes("root", "parent", "child:child-1")
                        )),
                        step("mutate-child", cmd("increment-child"), obs(
                                values("child.id", "child-1", "child.input", "alpha", "child.local", "1"),
                                nodes("root", "parent", "child:child-1")
                        )),
                        step("replace-input", cmd("set-child-input", "value", "beta"), obs(
                                values("child.id", "child-1", "child.input", "beta", "child.local", "1"),
                                nodes("root", "parent", "child:child-1"),
                                events("child-update:alpha->beta")
                        ))
                ),
                benchmark(
                        cmd("set-child-input", "value", "beta"),
                        cmd("set-child-input", "value", "alpha")
                )
        );
    }

    /// Creates the keyed list identity fixture.
    ///
    /// @return the fixture
    private static FixtureDefinition keyedListFixture() {
        return fixture(
                "keyed-list-identity",
                FixtureStage.MICRO,
                "Keyed insertion, deletion, and reorder preserve item-local state and diagnose duplicate keys atomically.",
                Set.of("duplicate-key", "keyed-identity", "local-state"),
                List.of(
                        step("mount", cmd("mount"), obs(
                                values("local.a", "0", "local.b", "0", "local.c", "0"),
                                nodes("root", "list", "item:a", "item:b", "item:c")
                        )),
                        step("mutate-b", cmd("increment-item", "key", "b"), obs(
                                values("local.a", "0", "local.b", "1", "local.c", "0"),
                                nodes("root", "list", "item:a", "item:b", "item:c")
                        )),
                        step("insert-x", cmd("set-order", "keys", "x,a,b,c"), obs(
                                values("local.a", "0", "local.b", "1", "local.c", "0", "local.x", "0"),
                                nodes("root", "list", "item:x", "item:a", "item:b", "item:c"),
                                events("mount:x")
                        )),
                        step("reorder", cmd("set-order", "keys", "c,b,x,a"), obs(
                                values("local.a", "0", "local.b", "1", "local.c", "0", "local.x", "0"),
                                nodes("root", "list", "item:c", "item:b", "item:x", "item:a")
                        )),
                        step("delete-a", cmd("set-order", "keys", "c,b,x"), obs(
                                values("local.b", "1", "local.c", "0", "local.x", "0"),
                                nodes("root", "list", "item:c", "item:b", "item:x"),
                                events("dispose:a")
                        )),
                        step("duplicate-key", cmd("set-order", "keys", "c,b,b,x"), obs(
                                values("local.b", "1", "local.c", "0", "local.x", "0"),
                                nodes("root", "list", "item:c", "item:b", "item:x"),
                                List.of(),
                                diagnostics("duplicate-key")
                        ))
                ),
                benchmark(cmd("set-order", "keys", "c,b,a"), cmd("set-order", "keys", "a,b,c"))
        );
    }

    /// Creates the phase-impact fixture.
    ///
    /// @return the fixture
    private static FixtureDefinition phaseImpactFixture() {
        return fixture(
                "phase-impact-burst",
                FixtureStage.MICRO,
                "High-frequency text, color, size, and offset changes retain their phase read sites.",
                Set.of("phase-attribution", "phase-coalescing", "reactive-read-site"),
                List.of(
                        step("mount", cmd("mount"), obs(
                                values("color", "red", "offset", "0", "size", "10", "text", "alpha"),
                                nodes("root", "phase-target")
                        )),
                        step(
                                "change-text",
                                cmd("set-text", "value", "beta"),
                                obs(
                                        values("color", "red", "offset", "0", "size", "10", "text", "beta"),
                                        nodes("root", "phase-target")
                                ),
                                PhaseExpectation.of(RuntimePhase.MEASURE, RuntimePhase.PAINT, RuntimePhase.SEMANTICS)
                        ),
                        step(
                                "change-color",
                                cmd("set-color", "value", "blue"),
                                obs(
                                        values("color", "blue", "offset", "0", "size", "10", "text", "beta"),
                                        nodes("root", "phase-target")
                                ),
                                PhaseExpectation.of(RuntimePhase.PAINT)
                        ),
                        step(
                                "change-size",
                                cmd("set-size", "value", "20"),
                                obs(
                                        values("color", "blue", "offset", "0", "size", "20", "text", "beta"),
                                        nodes("root", "phase-target")
                                ),
                                PhaseExpectation.of(RuntimePhase.MEASURE)
                        ),
                        step(
                                "change-offset",
                                cmd("set-offset", "value", "5"),
                                obs(
                                        values("color", "blue", "offset", "5", "size", "20", "text", "beta"),
                                        nodes("root", "phase-target")
                                ),
                                PhaseExpectation.of(RuntimePhase.PLACE, RuntimePhase.HIT_TEST)
                        ),
                        step(
                                "coalesced-burst",
                                cmd("apply-burst", "updates", "100"),
                                obs(
                                        values(
                                                "color", "green", "offset", "9", "size", "24", "text", "gamma",
                                                "updates", "100"
                                        ),
                                        nodes("root", "phase-target")
                                ),
                                PhaseExpectation.of(
                                        RuntimePhase.MEASURE,
                                        RuntimePhase.PLACE,
                                        RuntimePhase.PAINT,
                                        RuntimePhase.SEMANTICS,
                                        RuntimePhase.HIT_TEST
                                )
                        )
                ),
                benchmark(cmd("set-variant", "value", "b"), cmd("set-variant", "value", "a"))
        );
    }

    /// Creates the cross-scope geometry propagation fixture.
    ///
    /// @return the fixture
    private static FixtureDefinition geometryPropagationFixture() {
        return fixture(
                "cross-scope-geometry",
                FixtureStage.INTEGRATION,
                "A simulated child measure result feeds a parent placement consumer without an intermediate frame.",
                Set.of("cross-scope", "geometry-propagation", "same-frame"),
                List.of(
                        step("mount", cmd("mount"), obs(
                                values("child.width", "40", "parent.childOffset", "45"),
                                nodes("root", "parent", "child")
                        )),
                        step(
                                "grow-child",
                                cmd("set-child-width", "value", "70"),
                                obs(
                                        values("child.width", "70", "parent.childOffset", "75"),
                                        nodes("root", "parent", "child"),
                                        events("measure:child=70", "place:parent=75")
                                ),
                                PhaseExpectation.of(RuntimePhase.MEASURE, RuntimePhase.PLACE)
                        )
                ),
                benchmark(cmd("set-child-width", "value", "70"), cmd("set-child-width", "value", "40"))
        );
    }

    /// Creates the controlled editing and asynchronous acceptance fixture.
    ///
    /// @return the fixture
    private static FixtureDefinition controlledEditingFixture() {
        return fixture(
                "controlled-text-editing",
                FixtureStage.INTEGRATION,
                "Editor-owned text survives asynchronous partial acceptance, rejection, and composition-safe external updates.",
                Set.of("asynchronous-filter", "composition", "controlled-editing", "transaction"),
                List.of(
                        step("mount", cmd("mount"), editingObservation(
                                "hello", "hello", "5:5", "none", "none", "none"
                        )),
                        step("submit-one", cmd("user-edit", "text", "hello world"), editingObservation(
                                "hello", "hello world", "11:11", "none", "1", "none"
                        )),
                        step("submit-two", cmd("user-edit", "text", "hello world!"), editingObservation(
                                "hello", "hello world!", "12:12", "none", "1,2", "none"
                        )),
                        step("partial-accept-one", cmd(
                                "resolve-edit", "request", "1", "result", "accept:hello wor"
                        ), editingObservation(
                                "hello wor", "hello world!", "12:12", "none", "2", "none",
                                events("accept-partial:1")
                        )),
                        step("reject-two", cmd("resolve-edit", "request", "2", "result", "reject"), editingObservation(
                                "hello wor", "hello wor", "9:9", "none", "none", "none",
                                events("reject:2", "buffer-sync:application")
                        )),
                        step("begin-composition", cmd("begin-composition", "text", "hello wor中"), editingObservation(
                                "hello wor", "hello wor中", "10:10", "9:10", "none", "none"
                        )),
                        step("external-during-composition", cmd(
                                "set-application-value", "value", "remote"
                        ), editingObservation(
                                "remote", "hello wor中", "10:10", "9:10", "none", "remote",
                                events("defer-buffer-sync:composition")
                        )),
                        step("commit-stale-composition", cmd("commit-composition"), editingObservation(
                                "remote", "hello wor中", "10:10", "none", "3", "remote",
                                events("submit:3")
                        )),
                        step("reject-stale-composition", cmd(
                                "resolve-edit", "request", "3", "result", "reject"
                        ), editingObservation(
                                "remote", "remote", "6:6", "none", "none", "none",
                                events("reject:3", "apply-deferred:remote")
                        )),
                        step("accept-current-edit", cmd("user-edit", "text", "remote!"), editingObservation(
                                "remote", "remote!", "7:7", "none", "4", "none"
                        )),
                        step("resolve-current-edit", cmd(
                                "resolve-edit", "request", "4", "result", "accept:remote!"
                        ), editingObservation(
                                "remote!", "remote!", "7:7", "none", "none", "none",
                                events("accept:4")
                        ))
                ),
                null
        );
    }

    /// Creates the ambient inherited-value fixture.
    ///
    /// @return the fixture
    private static FixtureDefinition ambientFixture() {
        return fixture(
                "ambient-inheritance",
                FixtureStage.INTEGRATION,
                "Typed ambient values propagate dynamically while subtree overrides preserve narrow invalidation scope.",
                Set.of("ambient-override", "dynamic-ambient", "invalidation-scope"),
                List.of(
                        step("mount", cmd("mount"), ambientObservation("light", "dark", "1", "en-US", "ltr")),
                        step(
                                "change-root-theme",
                                cmd("set-theme", "value", "sepia"),
                                ambientObservation("sepia", "dark", "1", "en-US", "ltr"),
                                PhaseExpectation.of(RuntimePhase.PAINT)
                        ),
                        step(
                                "change-density",
                                cmd("set-density", "value", "2"),
                                ambientObservation("sepia", "dark", "2", "en-US", "ltr"),
                                PhaseExpectation.of(RuntimePhase.MEASURE, RuntimePhase.PAINT)
                        ),
                        step(
                                "change-locale-direction",
                                cmd("set-locale-direction", "value", "ar:rtl"),
                                ambientObservation("sepia", "dark", "2", "ar", "rtl"),
                                PhaseExpectation.of(RuntimePhase.MEASURE, RuntimePhase.SEMANTICS)
                        ),
                        step(
                                "remove-theme-override",
                                cmd("remove-theme-override"),
                                ambientObservation("sepia", "sepia", "2", "ar", "rtl"),
                                PhaseExpectation.of(RuntimePhase.PAINT)
                        )
                ),
                benchmark(cmd("set-theme", "value", "sepia"), cmd("set-theme", "value", "light"))
        );
    }

    /// Creates the capability-dependent viewport materialization fixture.
    ///
    /// @return the fixture
    private static FixtureDefinition viewportFixture() {
        CapabilityRequirement measureTime = requirement(
                CandidateCapabilities.MEASURE_MATERIALIZATION,
                "scoped-measure-time"
        );
        CapabilityRequirement previousViewport = requirement(
                CandidateCapabilities.MEASURE_MATERIALIZATION,
                "previous-viewport"
        );
        return fixture(
                "viewport-materialization",
                FixtureStage.INTEGRATION,
                "Viewport-driven keyed children preserve surviving identity and commit or lag according to the declared ADR-020 strategy.",
                Set.of("failure-atomicity", "keyed-identity", "measure-time-structure", "viewport"),
                List.of(
                        step("mount", cmd("mount"), viewportObservation("0,1,2", "none", "0")),
                        step("mutate-survivor", cmd("increment-item", "key", "2"), viewportObservation("0,1,2", "none", "1")),
                        step(
                                "scroll-measure-time",
                                cmd("set-viewport", "value", "40:60"),
                                measureTime,
                                viewportObservation("2,3,4", "none", "1"),
                                PhaseExpectation.of(RuntimePhase.MEASURE, RuntimePhase.STRUCTURE)
                        ),
                        step(
                                "scroll-previous-viewport",
                                cmd("set-viewport", "value", "40:60"),
                                previousViewport,
                                viewportObservation("0,1,2", "2,3,4", "1"),
                                PhaseExpectation.of(RuntimePhase.MEASURE)
                        ),
                        step(
                                "settle-scroll-previous-viewport",
                                cmd("next-frame"),
                                previousViewport,
                                viewportObservation("2,3,4", "none", "1"),
                                PhaseExpectation.of(RuntimePhase.STRUCTURE)
                        ),
                        step(
                                "shrink-measure-time",
                                cmd("set-viewport", "value", "40:40"),
                                measureTime,
                                viewportObservation("2,3", "none", "1"),
                                PhaseExpectation.of(RuntimePhase.MEASURE, RuntimePhase.STRUCTURE)
                        ),
                        step(
                                "shrink-previous-viewport",
                                cmd("set-viewport", "value", "40:40"),
                                previousViewport,
                                viewportObservation("2,3,4", "2,3", "1"),
                                PhaseExpectation.of(RuntimePhase.MEASURE)
                        ),
                        step(
                                "settle-shrink-previous-viewport",
                                cmd("next-frame"),
                                previousViewport,
                                viewportObservation("2,3", "none", "1"),
                                PhaseExpectation.of(RuntimePhase.STRUCTURE)
                        ),
                        step("failed-materialization", cmd("materialize-failing-next-item"), obs(
                                values("item.2.local", "1", "pending.keys", "none", "visible.keys", "2,3"),
                                nodes("root", "viewport", "item:2", "item:3"),
                                events("cleanup:staged-item:4"),
                                diagnostics("measure-materialization-failed")
                        )),
                        step("retry-materialization", cmd("retry-materialization"), viewportObservation("2,3,4", "none", "1"))
                ),
                benchmark(cmd("set-viewport", "value", "40:60"), cmd("set-viewport", "value", "0:60"))
        );
    }

    /// Creates application-callback containment fixtures for structure through paint.
    ///
    /// @return the fixture
    private static FixtureDefinition phaseFailureFixture() {
        ArrayList<FixtureStep> steps = new ArrayList<>();
        steps.add(step("mount", cmd("mount"), failureStableObservation("0")));
        String[] phases = {"structure", "measure", "place", "paint", "effect", "cleanup", "native-entry"};
        int revision = 0;
        for (String phase : phases) {
            steps.add(step("fail-" + phase, cmd("inject-callback-failure", "phase", phase), obs(
                    values("committed.revision", Integer.toString(revision), "fallback", phase),
                    nodes("root", "fallback:" + phase),
                    events("cleanup:" + phase, "fallback-present:" + phase),
                    diagnostics("callback-failure-" + phase)
            )));
            revision++;
            steps.add(step("recover-" + phase, cmd("retry-callback", "phase", phase), failureStableObservation(
                    Integer.toString(revision), events("recovered:" + phase)
            )));
        }
        return fixture(
                "phase-failure-containment",
                FixtureStage.INTEGRATION,
                "Structure, measure, placement, paint, effect, cleanup, and native-entry failures remain inside an explicit boundary with deterministic fallback and retry.",
                Set.of(
                        "diagnostics", "error-boundary", "failure-atomicity", "fallback",
                        "native-entry-containment", "retry"
                ),
                steps,
                null
        );
    }

    /// Creates staged failure, retry, and claimed-cancellation cleanup fixtures.
    ///
    /// @return the fixture
    private static FixtureDefinition stagedFailureFixture() {
        CapabilityRequirement cooperative = requirement(CandidateCapabilities.CANCELLATION, "cooperative");
        CapabilityRequirement preemptive = requirement(CandidateCapabilities.CANCELLATION, "preemptive");
        return fixture(
                "staged-work-cleanup",
                FixtureStage.INTEGRATION,
                "Failed and claimed-cancelled staged work leaks no nodes, owners, effects, edges, or mutations and retries deterministically.",
                Set.of("cancellation", "failure-atomicity", "retry", "staged-cleanup"),
                List.of(
                        step("mount", cmd("mount"), stagedBaselineObservation()),
                        step("fail-stage", cmd("run-failing-stage"), obs(
                                values("branch", "absent", "revision", "0"),
                                nodes("root", "baseline"),
                                events("cleanup:staged-node", "cleanup:staged-effect"),
                                diagnostics("staged-work-failed")
                        )),
                        step("retry-stage", cmd("retry-stage"), obs(
                                values("branch", "present", "revision", "1"),
                                nodes("root", "baseline", "branch"),
                                events("effect-mount:branch")
                        )),
                        step("reset-stage", cmd("reset-stage"), obs(
                                values("branch", "absent", "revision", "1"),
                                nodes("root", "baseline"),
                                events("effect-dispose:branch", "owner-dispose:branch")
                        )),
                        step("cancel-cooperative", cmd("run-cancelled-stage"), cooperative, obs(
                                values("branch", "absent", "revision", "1"),
                                nodes("root", "baseline"),
                                events("cleanup:cancelled-stage")
                        ), PhaseExpectation.none()),
                        step("cancel-preemptive", cmd("run-cancelled-stage"), preemptive, obs(
                                values("branch", "absent", "revision", "1"),
                                nodes("root", "baseline"),
                                events("cleanup:cancelled-stage")
                        ), PhaseExpectation.none())
                ),
                null
        );
    }

    /// Creates the realistic settings-and-chat API-charter fixture.
    ///
    /// @return the fixture
    private static FixtureDefinition realisticApplicationFixture() {
        return fixture(
                "settings-chat-application",
                FixtureStage.REALISTIC,
                "A roughly five-hundred-line settings form and chat list combines inputs, branches, effects, ambients, editing, and keyed identity.",
                Set.of("api-charter", "realistic-ceremony", "settings-form", "chat-list"),
                List.of(
                        step("mount", cmd("mount"), realisticObservation(
                                "system", "true", "", "all", "m1,m2,m3", "0", "false", "absent"
                        )),
                        step("change-settings", cmd("set-preferences", "value", "dark:false"), realisticObservation(
                                "dark", "false", "", "all", "m1,m2,m3", "0", "false", "absent",
                                events("preferences-commit")
                        )),
                        step("edit-draft", cmd("set-draft", "value", "hello"), realisticObservation(
                                "dark", "false", "hello", "all", "m1,m2,m3", "0", "false", "absent"
                        )),
                        step("send-message", cmd("send-draft"), realisticObservation(
                                "dark", "false", "", "all", "m1,m2,m3,m4", "0", "false", "absent",
                                events("mount-message:m4", "effect:send:m4")
                        )),
                        step("react-to-message", cmd("increment-reaction", "key", "m2"), realisticObservation(
                                "dark", "false", "", "all", "m1,m2,m3,m4", "1", "false", "absent"
                        )),
                        step("filter-unread", cmd("set-filter", "value", "unread"), realisticObservation(
                                "dark", "false", "", "unread", "m2,m4", "1", "false", "absent"
                        )),
                        step("show-advanced", cmd("show-advanced"), realisticObservation(
                                "dark", "false", "", "unread", "m2,m4", "1", "true", "0",
                                events("effect-mount:advanced")
                        )),
                        step("mutate-advanced", cmd("increment-advanced"), realisticObservation(
                                "dark", "false", "", "unread", "m2,m4", "1", "true", "1"
                        )),
                        step("restore-all", cmd("set-filter", "value", "all"), realisticObservation(
                                "dark", "false", "", "all", "m1,m2,m3,m4", "1", "true", "1"
                        ))
                ),
                benchmark(
                        cmd("set-preferences", "value", "dark:false"),
                        cmd("set-preferences", "value", "system:true"),
                        cmd("set-filter", "value", "unread"),
                        cmd("set-filter", "value", "all")
                )
        );
    }

    /// Creates a fixture definition.
    ///
    /// @param id the fixture identifier
    /// @param stage the fixture stage
    /// @param description the fixture description
    /// @param tags the correctness tags
    /// @param steps the ordered steps
    /// @param benchmark the benchmark plan, or `null`
    /// @return the definition
    private static FixtureDefinition fixture(
            String id,
            FixtureStage stage,
            String description,
            @Unmodifiable Set<String> tags,
            @Unmodifiable List<FixtureStep> steps,
            @Nullable BenchmarkPlan benchmark
    ) {
        return new FixtureDefinition(id, stage, description, tags, steps, benchmark);
    }

    /// Creates an unconditional step without required phases.
    ///
    /// @param id the step identifier
    /// @param command the command
    /// @param expected the oracle
    /// @return the step
    private static FixtureStep step(String id, FixtureCommand command, FixtureObservation expected) {
        return step(id, command, null, expected, PhaseExpectation.none());
    }

    /// Creates an unconditional step with phase requirements.
    ///
    /// @param id the step identifier
    /// @param command the command
    /// @param expected the oracle
    /// @param phases the phase requirements
    /// @return the step
    private static FixtureStep step(
            String id,
            FixtureCommand command,
            FixtureObservation expected,
            PhaseExpectation phases
    ) {
        return step(id, command, null, expected, phases);
    }

    /// Creates a fully specified step.
    ///
    /// @param id the step identifier
    /// @param command the command
    /// @param requirement the capability requirement, or `null`
    /// @param expected the oracle
    /// @param phases the phase requirements
    /// @return the step
    private static FixtureStep step(
            String id,
            FixtureCommand command,
            @Nullable CapabilityRequirement requirement,
            FixtureObservation expected,
            PhaseExpectation phases
    ) {
        return new FixtureStep(id, command, requirement, expected, phases);
    }

    /// Creates a fixture command from alternating key/value arguments.
    ///
    /// @param operation the operation
    /// @param arguments alternating keys and values
    /// @return the command
    private static FixtureCommand cmd(String operation, String... arguments) {
        return new FixtureCommand(operation, values(arguments));
    }

    /// Creates an observation without events or diagnostics.
    ///
    /// @param values the observed values
    /// @param nodes the mounted nodes
    /// @return the observation
    private static FixtureObservation obs(
            @Unmodifiable Map<String, String> values,
            @Unmodifiable List<String> nodes
    ) {
        return new FixtureObservation(values, nodes, List.of(), List.of());
    }

    /// Creates an observation with events and no diagnostics.
    ///
    /// @param values the observed values
    /// @param nodes the mounted nodes
    /// @param events the events
    /// @return the observation
    private static FixtureObservation obs(
            @Unmodifiable Map<String, String> values,
            @Unmodifiable List<String> nodes,
            @Unmodifiable List<String> events
    ) {
        return new FixtureObservation(values, nodes, events, List.of());
    }

    /// Creates a complete observation.
    ///
    /// @param values the observed values
    /// @param nodes the mounted nodes
    /// @param events the events
    /// @param diagnostics the diagnostic codes
    /// @return the observation
    private static FixtureObservation obs(
            @Unmodifiable Map<String, String> values,
            @Unmodifiable List<String> nodes,
            @Unmodifiable List<String> events,
            @Unmodifiable List<String> diagnostics
    ) {
        return new FixtureObservation(values, nodes, events, diagnostics);
    }

    /// Creates a key-sorted string map from alternating keys and values.
    ///
    /// @param entries alternating keys and values
    /// @return the immutable map
    private static @Unmodifiable Map<String, String> values(String... entries) {
        if (entries.length % 2 != 0) {
            throw new IllegalArgumentException("values require alternating keys and values");
        }
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        for (int index = 0; index < entries.length; index += 2) {
            @Nullable String previous = values.put(entries[index], entries[index + 1]);
            if (previous != null) {
                throw new IllegalArgumentException("duplicate value key " + entries[index]);
            }
        }
        return ComparisonContracts.immutableSortedMap(values, "fixture values");
    }

    /// Creates an immutable node list.
    ///
    /// @param nodes the node identifiers
    /// @return the list
    private static @Unmodifiable List<String> nodes(String... nodes) {
        return List.of(nodes);
    }

    /// Creates an immutable event list.
    ///
    /// @param events the event identifiers
    /// @return the list
    private static @Unmodifiable List<String> events(String... events) {
        return List.of(events);
    }

    /// Creates an immutable diagnostic-code list.
    ///
    /// @param diagnostics the diagnostic codes
    /// @return the list
    private static @Unmodifiable List<String> diagnostics(String... diagnostics) {
        return List.of(diagnostics);
    }

    /// Creates the fixed steady-state iteration plan.
    ///
    /// @param cycle the state-restoring command cycle
    /// @return the plan
    private static BenchmarkPlan benchmark(FixtureCommand... cycle) {
        return new BenchmarkPlan(100, 1_000, List.of(cycle));
    }

    /// Creates a capability requirement.
    ///
    /// @param capability the capability key
    /// @param value the required value
    /// @return the requirement
    private static CapabilityRequirement requirement(String capability, String value) {
        return new CapabilityRequirement(capability, value);
    }

    /// Creates a controlled-editing observation.
    ///
    /// @param applicationValue the application-owned value
    /// @param buffer the editor-owned buffer
    /// @param selection the selection range
    /// @param composition the composition range or `none`
    /// @param pending the pending request identifiers or `none`
    /// @param deferred the deferred application value or `none`
    /// @param events the events since the previous checkpoint
    /// @return the observation
    private static FixtureObservation editingObservation(
            String applicationValue,
            String buffer,
            String selection,
            String composition,
            String pending,
            String deferred,
            @Unmodifiable List<String> events
    ) {
        return obs(values(
                "application.value", applicationValue,
                "editor.buffer", buffer,
                "editor.composition", composition,
                "editor.deferredApplication", deferred,
                "editor.pendingRequests", pending,
                "editor.selection", selection
        ), nodes("root", "editor"), events);
    }

    /// Creates a controlled-editing observation without events.
    ///
    /// @param applicationValue the application-owned value
    /// @param buffer the editor-owned buffer
    /// @param selection the selection range
    /// @param composition the composition range or `none`
    /// @param pending the pending request identifiers or `none`
    /// @param deferred the deferred application value or `none`
    /// @return the observation
    private static FixtureObservation editingObservation(
            String applicationValue,
            String buffer,
            String selection,
            String composition,
            String pending,
            String deferred
    ) {
        return editingObservation(
                applicationValue,
                buffer,
                selection,
                composition,
                pending,
                deferred,
                List.of()
        );
    }

    /// Creates an ambient-value observation.
    ///
    /// @param rootTheme the root theme
    /// @param subtreeTheme the effective subtree theme
    /// @param density the inherited density
    /// @param locale the inherited locale
    /// @param direction the inherited text direction
    /// @return the observation
    private static FixtureObservation ambientObservation(
            String rootTheme,
            String subtreeTheme,
            String density,
            String locale,
            String direction
    ) {
        return obs(values(
                "root.density", density,
                "root.direction", direction,
                "root.locale", locale,
                "root.theme", rootTheme,
                "subtree.density", density,
                "subtree.direction", direction,
                "subtree.locale", locale,
                "subtree.theme", subtreeTheme
        ), nodes("root", "ambient-root-consumer", "ambient-override", "ambient-subtree-consumer"));
    }

    /// Creates a viewport observation.
    ///
    /// @param visibleKeys visible keyed items
    /// @param pendingKeys pending keys or `none`
    /// @param itemTwoLocal item two's retained local state
    /// @return the observation
    private static FixtureObservation viewportObservation(
            String visibleKeys,
            String pendingKeys,
            String itemTwoLocal
    ) {
        ArrayList<String> mounted = new ArrayList<>();
        mounted.add("root");
        mounted.add("viewport");
        for (String key : visibleKeys.split(",")) {
            mounted.add("item:" + key);
        }
        return obs(values(
                "item.2.local", itemTwoLocal,
                "pending.keys", pendingKeys,
                "visible.keys", visibleKeys
        ), mounted);
    }

    /// Creates a stable post-recovery phase-failure observation.
    ///
    /// @param revision the committed revision
    /// @param events the recovery events
    /// @return the observation
    private static FixtureObservation failureStableObservation(
            String revision,
            @Unmodifiable List<String> events
    ) {
        return obs(
                values("committed.revision", revision, "fallback", "none"),
                nodes("root", "content"),
                events
        );
    }

    /// Creates a stable phase-failure observation without events.
    ///
    /// @param revision the committed revision
    /// @return the observation
    private static FixtureObservation failureStableObservation(String revision) {
        return failureStableObservation(revision, List.of());
    }

    /// Creates the staged-work baseline observation.
    ///
    /// @return the observation
    private static FixtureObservation stagedBaselineObservation() {
        return obs(values("branch", "absent", "revision", "0"), nodes("root", "baseline"));
    }

    /// Creates a realistic application observation.
    ///
    /// @param theme the selected theme
    /// @param notifications the notification toggle
    /// @param draft the editor draft
    /// @param filter the chat filter
    /// @param messages visible message keys
    /// @param reactionM2 local reaction state for message `m2`
    /// @param advancedVisible whether the advanced panel is visible
    /// @param advancedLocal the advanced panel local state or `absent`
    /// @param events events since the previous checkpoint
    /// @return the observation
    private static FixtureObservation realisticObservation(
            String theme,
            String notifications,
            String draft,
            String filter,
            String messages,
            String reactionM2,
            String advancedVisible,
            String advancedLocal,
            @Unmodifiable List<String> events
    ) {
        ArrayList<String> mounted = new ArrayList<>(List.of(
                "root", "settings", "theme-field", "notification-toggle", "chat", "draft-editor"
        ));
        for (String message : messages.split(",")) {
            mounted.add("message:" + message);
        }
        if (advancedVisible.equals("true")) {
            mounted.add("advanced-settings");
        }
        return obs(values(
                "advanced.local", advancedLocal,
                "advanced.visible", advancedVisible,
                "chat.draft", draft,
                "chat.filter", filter,
                "chat.messages", messages,
                "message.m2.reactions", reactionM2,
                "settings.notifications", notifications,
                "settings.theme", theme
        ), mounted, events);
    }

    /// Creates a realistic application observation without events.
    ///
    /// @param theme the selected theme
    /// @param notifications the notification toggle
    /// @param draft the editor draft
    /// @param filter the chat filter
    /// @param messages visible message keys
    /// @param reactionM2 local reaction state for message `m2`
    /// @param advancedVisible whether the advanced panel is visible
    /// @param advancedLocal the advanced panel local state or `absent`
    /// @return the observation
    private static FixtureObservation realisticObservation(
            String theme,
            String notifications,
            String draft,
            String filter,
            String messages,
            String reactionM2,
            String advancedVisible,
            String advancedLocal
    ) {
        return realisticObservation(
                theme,
                notifications,
                draft,
                filter,
                messages,
                reactionM2,
                advancedVisible,
                advancedLocal,
                List.of()
        );
    }
}

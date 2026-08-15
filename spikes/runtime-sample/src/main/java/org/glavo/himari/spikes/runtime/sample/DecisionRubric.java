package org.glavo.himari.spikes.runtime.sample;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/// Provides the frozen non-compensatory M1 structural-runtime decision rubric.
///
/// Correctness disqualifiers and evidence completeness are evaluated before scoring. Quantitative
/// lower-is-better metrics use the best eligible candidate as the reference: equal zero values score
/// 100, a nonzero value against a zero best scores `100 / (1 + value)`, and otherwise the score is
/// `100 * best / value`. Metrics within a dimension use a geometric mean, then fixed dimension
/// weights form the final arithmetic mean. Performance cannot compensate for a disqualifier or a
/// failed ordinary-Java ceremony review.
@NotNullByDefault
public final class DecisionRubric {
    /// The rubric version written into every result.
    public static final String VERSION = "runtime-decision-rubric-v1";

    /// Frozen correctness and evidence rules.
    private static final @Unmodifiable List<RubricRule> RULES = List.of(
            new RubricRule(
                    "ordinary-java-only",
                    "correctness",
                    "Application correctness must compile and run without generated or transformed application code.",
                    true
            ),
            new RubricRule(
                    "fixture-oracle",
                    "correctness",
                    "Every applicable command must match the exact shared observation oracle.",
                    true
            ),
            new RubricRule(
                    "glitch-free-epochs",
                    "correctness",
                    "No observer may see an intermediate diamond value, mixed state epoch, or partial UI commit.",
                    true
            ),
            new RubricRule(
                    "identity-lifecycle",
                    "correctness",
                    "Branch, keyed-list, component-input, local-state, and effect lifetimes must match the fixture contract.",
                    true
            ),
            new RubricRule(
                    "failure-cleanup",
                    "correctness",
                    "Failed or claimed-cancelled attempts must leave no staged node, owner, edge, effect, callback, or retained token.",
                    true
            ),
            new RubricRule(
                    "phase-attribution",
                    "correctness",
                    "Every phase required by an observable update must be invalidated; extra invalidations remain measured.",
                    true
            ),
            new RubricRule(
                    "declared-diagnostics",
                    "correctness",
                    "Duplicate-key, measure-materialization, per-phase callback, and staged-work failures must emit the stable fixture code and a deterministic trace.",
                    true
            ),
            new RubricRule(
                    "deterministic-replay",
                    "correctness",
                    "Repeated command streams must produce identical semantic observations and traces; encoding the same result object must be byte-identical, while timing and allocation samples are not goldens.",
                    true
            ),
            new RubricRule(
                    "native-image-evidence",
                    "evidence",
                    "A final decision candidate must compile and execute the suite under the selected Native Image toolchain.",
                    false
            ),
            new RubricRule(
                    "reload-identity-evidence",
                    "evidence",
                    "A candidate claiming reload identity must preserve compatible branch, item, and local-state identity in a recorded run.",
                    false
            )
    );

    /// Frozen score dimensions whose weights sum to one hundred.
    private static final @Unmodifiable List<RubricDimension> DIMENSIONS = List.of(
            new RubricDimension(
                    "ordinary-java-ergonomics",
                    30,
                    List.of(
                            "sourceLines", "explicitKeys", "deferredGetters", "structuralControls",
                            "groupBoundaries", "genericTypeNoise", "callbackWrappers"
                    ),
                    "Geometric mean of lower-is-better normalized metrics; semantic keys remain visible but are reviewed separately from accidental ceremony."
            ),
            new RubricDimension(
                    "diagnostics-and-recovery",
                    15,
                    List.of("traceQuality", "requiredDiagnosticCoverage", "deterministicRecovery"),
                    "Trace quality uses the frozen zero-to-four scale; coverage and recovery are pass percentages."
            ),
            new RubricDimension(
                    "execution-and-invalidation",
                    20,
                    List.of("callbacksExecuted", "nodesVisited", "dependencyEdges", "phaseInvalidations"),
                    "Geometric mean of lower-is-better totals, with every fixture and phase also retained for review."
            ),
            new RubricDimension(
                    "allocation-and-retention",
                    15,
                    List.of("steadyStateAllocatedBytes", "peakRetainedBytes", "postCloseRetainedBytes"),
                    "Geometric mean of lower-is-better totals; unavailable allocation counters make the evidence incomplete rather than scoring zero."
            ),
            new RubricDimension(
                    "steady-state-throughput",
                    10,
                    List.of("steadyStateElapsedNanos"),
                    "Lower-is-better normalization over identical command counts; reviewed only with matching JVM flags and environment records."
            ),
            new RubricDimension(
                    "portability-and-tooling",
                    10,
                    List.of("nativeImage", "reloadIdentity"),
                    "Native Image and any claimed reload behavior must pass; an unclaimed reload feature receives no reload credit but is not a correctness failure."
            )
    );

    /// Frozen early-stop and progression checkpoints.
    private static final @Unmodifiable List<RubricCheckpoint> CHECKPOINTS = List.of(
            new RubricCheckpoint(
                    "compile-gate",
                    FixtureStage.MICRO,
                    "Stop before fixture execution if application code requires generation, transformation, or a compiler plugin."
            ),
            new RubricCheckpoint(
                    "micro-correctness-gate",
                    FixtureStage.MICRO,
                    "Stop after micro-fixtures on any correctness disqualifier and retain the partial report."
            ),
            new RubricCheckpoint(
                    "micro-ceremony-review",
                    FixtureStage.MICRO,
                    "Blind-review all application sources when accidental markers reach 20 percent of significant micro lines or micro source lines exceed 1.75 times the smallest passing candidate; stop when two of three reviewers independently classify ceremony as pervasive."
            ),
            new RubricCheckpoint(
                    "integration-gate",
                    FixtureStage.INTEGRATION,
                    "Stop before the realistic port on any integration oracle, containment, cleanup, or required-diagnostic failure."
            ),
            new RubricCheckpoint(
                    "decision-evidence-gate",
                    FixtureStage.REALISTIC,
                    "Score only candidates with a complete realistic port, comparable benchmark environment, Native Image result, and all claimed tooling evidence."
            )
    );

    /// Prevents construction.
    private DecisionRubric() {
    }

    /// Returns all frozen correctness and evidence rules.
    ///
    /// @return the immutable rules
    public static @Unmodifiable List<RubricRule> rules() {
        return RULES;
    }

    /// Returns all frozen score dimensions.
    ///
    /// @return the immutable dimensions
    public static @Unmodifiable List<RubricDimension> dimensions() {
        return DIMENSIONS;
    }

    /// Returns all frozen progression checkpoints.
    ///
    /// @return the immutable checkpoints
    public static @Unmodifiable List<RubricCheckpoint> checkpoints() {
        return CHECKPOINTS;
    }

    /// Validates identifier uniqueness and the fixed one-hundred-point weight total.
    ///
    /// @throws IllegalStateException if the checked-in rubric is internally inconsistent
    public static void validate() {
        requireUniqueIds(RULES.stream().map(RubricRule::id).toList(), "rubric rules");
        requireUniqueIds(DIMENSIONS.stream().map(RubricDimension::id).toList(), "rubric dimensions");
        requireUniqueIds(CHECKPOINTS.stream().map(RubricCheckpoint::id).toList(), "rubric checkpoints");
        int weight = DIMENSIONS.stream().mapToInt(RubricDimension::weight).sum();
        if (weight != 100) {
            throw new IllegalStateException("Rubric dimension weights must sum to 100, found " + weight);
        }
    }

    /// Requires unique identifiers in one rubric section.
    ///
    /// @param identifiers the identifiers
    /// @param section the diagnostic section name
    /// @throws IllegalStateException if an identifier repeats
    private static void requireUniqueIds(@Unmodifiable List<String> identifiers, String section) {
        Objects.requireNonNull(identifiers, "identifiers");
        Set<String> unique = new HashSet<>(identifiers);
        if (unique.size() != identifiers.size()) {
            throw new IllegalStateException(section + " contain duplicate identifiers");
        }
    }
}

# M1 Structural Runtime Decision Rubric

Version: `runtime-decision-rubric-v1`

This rubric is frozen before the grouped, one-shot, and hybrid candidates are implemented. A change to a fixture, metric, threshold, weight, or selection rule requires a version change and invalidates comparison reports generated under the previous version. A candidate may not change this file or the shared fixture catalog as part of its implementation.

## Decision order

The decision is non-compensatory and proceeds in this order:

1. Apply correctness disqualifiers.
2. Require complete evidence for the checkpoint being reviewed.
3. Complete the ordinary-Java ceremony review.
4. Review Pareto dominance and material outliers before calculating a score.
5. Calculate the frozen weighted score only for remaining candidates.
6. Resolve a score difference of at most three points by the fixed tie-breakers.

Performance, allocation, or memory results cannot compensate for a correctness failure, missing required diagnostics, transformed application code, or pervasive accidental ceremony.

## Correctness disqualifiers

A candidate is disqualified when any of these occurs:

- application correctness depends on generated source, bytecode transformation, a compiler plugin, or an annotation processor that changes method behavior;
- an applicable fixture observation differs from the checked-in oracle;
- a diamond observer sees an intermediate value, UI work mixes state epochs, or a partial staged commit becomes observable;
- branch, keyed item, component input, local state, effect, or cleanup identity differs from the declared fixture behavior;
- failed or claimed-cancelled work leaves a node, owner, effect, dependency edge, retained-memory registration, staged mutation, or callback live;
- an action fails to invalidate a phase required for its observable effect;
- a required stable diagnostic code or structured trace is missing;
- a candidate produces nondeterministic semantic observations for the same command stream or a callback failure escapes the declared session/native-entry boundary.

Additional callbacks, node visits, edges, or phase invalidations are measured rather than rejected unless they make an observable contract fail.

## Early checkpoints

The compile gate rejects transformed application code before fixture work. All micro-fixtures then run so a failed candidate retains useful partial evidence; any micro correctness failure stops the integration and realistic stages.

After micro correctness passes, reviewers inspect the complete micro source corpus without candidate names. A mandatory three-person review is triggered when either:

- accidental ceremony markers are at least 20 percent of significant micro source lines; or
- micro source lines exceed 1.75 times the smallest passing candidate.

`DEFERRED_GETTER`, `GROUP_BOUNDARY`, `GENERIC_TYPE_NOISE`, and `CALLBACK_WRAPPER` are accidental-ceremony markers for this trigger. `EXPLICIT_KEY` and `STRUCTURAL_CONTROL` remain measured but are not automatically accidental because they can encode application semantics. If two of three reviewers independently classify the ceremony as pervasive, the candidate stops before the realistic port and the partial result is retained.

Any integration oracle, containment, cleanup, or diagnostic failure stops the realistic port. Final scoring requires the realistic application, a comparable JVM benchmark environment, a Native Image result, the ceremony review, and all evidence for capabilities the candidate claims.

## Source measurement

The candidate declares only ordinary-Java application and API-charter files, each assigned to its earliest checkpoint. Runtime implementation files are excluded. Significant physical lines exclude blank lines, comments, package/import declarations, literal payload, and brace-or-punctuation-only lines.

Every ceremony count is backed by a checked source marker containing the repository-relative file, one-based line, category, and rationale. This avoids candidate-specific method-name heuristics and keeps every count reviewable. The report records:

- significant source lines;
- explicit semantic keys;
- deferred getters;
- structural-control primitives;
- non-semantic group boundaries;
- generic type noise;
- runtime-only callback wrappers.

## Runtime measurement

The shared probe records callbacks by kind, logical nodes visited, dependency-edge attach/detach/current/peak counts, retained shallow bytes, phase invalidations, and structured traces. Hot callback and phase counters use primitive storage, and the measured command loop uses indexed traversal so routine probe bookkeeping does not manufacture per-command allocation. Edge and retained-object registrations use identity tokens and must balance at cleanup.

Retained bytes are deterministic shallow-size estimates for framework-owned objects registered by the candidate. A final candidate must document the estimator and audit it against a heap histogram or equivalent retained-graph evidence; unregistered ownership is an evidence failure. JVM steady-state allocation uses the enabled current-thread allocation counter. If that counter is unavailable, allocation evidence is incomplete rather than silently reported as zero.

Each benchmark uses a fresh mounted session. It executes 100 unmeasured cycles, resets probe counters while preserving live registrations, then executes 1,000 measured cycles. The command cycle must restore the exact post-mount observation. Reports retain raw iterations, commands, elapsed nanoseconds, allocation bytes, execution counts, edge counts, retained bytes, and phase invalidations. Throughput comparisons are valid only with matching JVM flags and environment records; elapsed time is not a deterministic golden.

## Diagnostic quality

Structured traces use a fixed zero-to-four scale:

- 0: no trace;
- 1: stable code and message;
- 2: source location or owner/scope/key identity;
- 3: both source location and owner/scope/key identity;
- 4: both identities plus a dependency path or explicit recovery action.

Required fixture diagnostic codes are correctness gates. Trace quality is scored only after those gates pass.

## Scoring

The dimensions and weights are:

- ordinary-Java ergonomics: 30 percent;
- diagnostics and recovery: 15 percent;
- execution and invalidation: 20 percent;
- allocation and retention: 15 percent;
- steady-state throughput: 10 percent;
- portability and optional tooling: 10 percent.

For a lower-is-better metric, equal zero values score 100. A nonzero value against a zero best scores `100 / (1 + value)`. Otherwise the score is `100 * best / value`. Metrics within a dimension use a geometric mean so one extreme cannot be hidden by several good results. Dimension scores use the fixed weighted arithmetic mean.

Before scoring, reviewers must record Pareto dominance, benchmark noise, and any semantic difference permitted by a capability path. A final score is evidence, not an automatic architectural decision. A difference of at most three points is resolved in this order: lower accidental ceremony, narrower phase invalidation, lower peak retained memory, then simpler documented structural semantics. If the tie remains, `RUNTIME-ADR-001` records the unresolved tradeoff rather than changing this rubric retroactively.

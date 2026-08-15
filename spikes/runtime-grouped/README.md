# Explicit grouped-recomposition candidate

This module is the `RUNTIME-SPIKE-GROUPED-001` decision candidate. It is intentionally not a production runtime and does not establish the result of `RUNTIME-ADR-001`.

## Structural model

Application structure is a rerunnable ordinary-Java callback. The application must declare every group boundary itself; no compiler plugin supplies source keys, restart lambdas, change masks, or lambda memoization.

- An unkeyed group preserves positional memory only while its handwritten source identity remains at the same unkeyed sibling position.
- A keyed group preserves memory by the pair of collection source identity and application semantic key, independently of sibling order.
- A conditional branch explicitly chooses retain-on-hide or dispose-on-hide memory behavior. Effects and dependency edges are inactive while a retained branch is hidden.
- A composition attempt builds a private draft. Duplicate keys and application-declared failures run draft cleanup and preserve the previous committed nodes, groups, memory, effects, and edges.
- Effects mount only after a successful structural commit and clean up child-first when their group is removed.

The implementation is structurally independent of the one-shot and hybrid candidates. Neither candidate reuses this module's group, positional-memory, or reconciliation records.

## Value and phase integration

Application values use the shared `StateDomain` and `DerivedState` implementation. Each grouped binding records a `StateSource` version and the phases in which that source is consumed. A later grouped execution compares the semantic version at the same binding site and reports only the declared phase invalidations. The diamond application therefore uses the shared glitch-free push/pull graph, while mounted identity remains owned by this candidate.

The viewport application selects the `SCOPED_MEASURE_TIME` capability path from ADR-020. It atomically reconciles the current visible keys and preserves surviving keyed memory. Failed item materialization discards the draft and keeps the prior visible range. This is evidence for one candidate only and does not resolve ADR-020.

The candidate claims only cooperative staged-work cancellation. It does not claim preemptive cancellation or reload identity.

## Instrumentation

The probe uses deterministic shallow estimates of 96 bytes per group record, 32 bytes per positional-memory slot, and 48 bytes per active effect record. Dependency edges use stable identity tokens. Every registration must balance when a session closes; the conformance runner rejects any remaining node, group owner, effect, edge, retained byte, draft mutation, or callback.

The source corpus contains only the three ordinary-Java application files. Runtime implementation and neutral fixture-adapter plumbing are excluded. The marker builder enumerates every reviewed group, key, branch, effect, post-commit, abort-cleanup, and topology-control site; every reported ceremony count therefore resolves to a concrete significant source line rather than a sample or name-based estimate.

## Evidence status

All thirteen frozen correctness fixtures and their steady-state benchmark paths must pass. The generated report remains `INCOMPLETE` until the independent Native Image run and blind ceremony review are recorded. An incomplete report with no disqualifications is the expected result of this work package; it is not a waiver for missing final decision evidence.

Run the candidate with:

```text
./gradlew -g .gradle-user-home :spikes:runtime-grouped:conformance
```

The canonical report is written to `build/conformance/m1-runtime-grouped/report.json`.

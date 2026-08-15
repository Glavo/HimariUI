# Fine-grained structural-scope candidate

This module is the `RUNTIME-SPIKE-HYBRID-001` decision candidate. It is intentionally not a production runtime and does not establish the result of `RUNTIME-ADR-001`.

## Structural model

Stable application owners run their ordinary-Java initializers exactly once. Direct bindings update stable node properties without rerunning a component. Only an explicit small `structure(...)` callback may reread topology and declare semantic-keyed fragments.

- A structural scope owns a stable anchor and reruns only after its declared topology source advances.
- Ordinary Java `if` and `for` statements select fragments; the runtime does not require specialized `Show` or `ForEach` controllers.
- Every fragment has a semantic key and an explicit retain-on-omit or dispose-on-omit policy. Retained owners preserve local state while their bindings, effects, and nested scopes are inactive.
- Existing active or retained fragments are reused without rerunning their initializer. New fragment owners are initialized off-tree and registered only after the complete scope draft succeeds.
- Duplicate keys, retention-policy changes, and application-declared failures abort all new fragments and preserve the previously committed topology.
- Effects mount only after registration and clean up child-first when a fragment is omitted or the runtime closes.

The representation is independent of both comparison alternatives. It imports neither grouped positional records nor one-shot `Show` and `ForEach` controllers.

## Value, phase, and viewport integration

Application values use the shared `StateDomain` and `DerivedState` implementation. Each direct binding polls one `StateSource` semantic version and invalidates only its declared phases. A structural scope separately polls its topology source, reruns only its small declaration, and invalidates phases only when committed fragment identity or order changes.

The viewport application selects ADR-020's `SCOPED_MEASURE_TIME` capability path. One bounded viewport scope reconciles current visible keys in the same attempt, preserves surviving item-local state, attributes topology changes to measure and structure, and discards a failed item draft before deterministic retry. This is candidate evidence, not the ADR-020 decision.

The candidate makes no cooperative or preemptive cancellation claim and does not claim reload identity.

## Instrumentation

The probe uses deterministic shallow estimates of 80 bytes per owner, 48 bytes per node, 24 bytes per local integer, 64 bytes per binding, 48 bytes per effect, 40 bytes per anchor, 88 bytes per structural scope, and 48 bytes per fragment. Dependency edges use stable identity tokens. Every retained record and edge must balance when a session closes.

The measured source corpus contains only the three ordinary-Java application files. Runtime implementation and neutral fixture-adapter plumbing are excluded. Every component, binding, structural scope, fragment key, effect, commit callback, abort cleanup, and application topology loop reported as ceremony resolves to a checked significant source line.

## Evidence status

All thirteen frozen correctness fixtures, the scoped-measure-time viewport path, and every steady-state benchmark path pass. Post-close nodes, owners, effects, staged mutations, pending callbacks, dependency edges, and retained bytes are zero. The report remains `INCOMPLETE` until an independent Native Image run and blind ceremony review are recorded.

Run the candidate with:

```text
./gradlew -g .gradle-user-home :spikes:runtime-hybrid:conformance
```

The canonical report is written to `build/conformance/m1-runtime-hybrid/report.json`.

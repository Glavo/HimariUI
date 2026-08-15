# One-shot signal-ownership candidate

This module is the `RUNTIME-SPIKE-ONESHOT-001` decision candidate. It is intentionally not a production runtime and does not establish the result of `RUNTIME-ADR-001`.

## Structural model

Each application owner runs its ordinary-Java initializer exactly once. Later source versions are polled by direct property bindings; component initializers are not rerun. Only explicit stable anchors may change topology.

- `Show` owns zero or one branch and requires an explicit retain-on-hide or dispose-on-hide policy. Retained owners preserve local state while their bindings, effects, and dependency edges are inactive.
- `ForEach` reconciles application semantic keys, preserves surviving item owners across reorder, and rejects duplicate keys before changing the committed collection.
- New conditional and keyed owners are fully initialized off-tree. An initializer failure runs every staged abort cleanup and preserves the previous anchors, nodes, owners, effects, bindings, and item order.
- Effects mount only after owner registration and clean up child-first when an owner is hidden or disposed.
- Changing component inputs remain reactive properties; they are not captured as frozen initializer arguments.

The implementation is structurally independent of the grouped candidate. It imports none of that module's group, positional-memory, draft, or reconciliation types.

## Value, phase, and viewport integration

Application values use the shared `StateDomain` and `DerivedState` implementation. A binding records a `StateSource` semantic version and its consuming phases, then writes only its stable target property when that version changes. Structural controllers separately poll their condition or key source and invalidate only their declared topology phases.

The viewport application selects ADR-020's `PREVIOUS_VIEWPORT` capability path. A measure binding exposes requested keys as pending while the existing keyed owners remain mounted; `next-frame` publishes those keys to `ForEach` and invalidates structure. Returning to the currently committed viewport clears the pending request without structural work. A failed item initializer preserves the prior viewport and survivor-local state.

The candidate makes no cooperative or preemptive cancellation claim and does not claim reload identity.

## Instrumentation

The probe uses deterministic shallow estimates of 80 bytes per owner, 48 bytes per node, 24 bytes per local integer, 64 bytes per binding, 48 bytes per effect, 40 bytes per anchor, and 72 bytes per structural controller. Dependency edges use stable identity tokens. Every retained record and edge must balance when a session closes.

The measured source corpus contains only the three ordinary-Java application files. Runtime implementation and neutral fixture-adapter plumbing are excluded. Every component boundary, binding callback, `Show`, `ForEach`, semantic-key source, effect, commit callback, abort cleanup, and application topology loop reported as ceremony resolves to a checked significant source line.

## Evidence status

All thirteen frozen correctness fixtures, their applicable previous-viewport path, and every steady-state benchmark path pass. Post-close nodes, owners, effects, staged mutations, pending callbacks, dependency edges, and retained bytes are zero. The report remains `INCOMPLETE` until an independent Native Image run and blind ceremony review are recorded.

Run the candidate with:

```text
./gradlew -g .gradle-user-home :spikes:runtime-oneshot:conformance
```

The canonical report is written to `build/conformance/m1-runtime-oneshot/report.json`.

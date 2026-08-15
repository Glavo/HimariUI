# HimariUI structural runtime, scheduling, and animation

`himari-runtime` provides the grouped structural runtime defined by `STRUCTURE-001`, the
application/window scheduling layer defined by `SCHED-001`, and the transactional scalar animation
kernel defined by `ANIM-CORE-001`. Scheduling and animation use the target-neutral platform event
loop and its monotonic `FrameClock` without owning platform resources.

## Grouped structural updates

`StructuralRuntime` implements ADR-023's compiler-independent explicit groups. Every application
group supplies a handwritten source identity. Unkeyed children and remembered values retain
identity only at a stable positional call site; reorderable siblings use semantic keys. Each group
captures ordinary `State` and `DerivedState` reads with its own `ReactiveObserver`, so an update
selects only invalidated groups whose active ancestors are otherwise clean.

Each attempt builds private topology, memory, dependency, ambient, effect, boundary, and
materialization records against one `StateDomain` epoch. A successful attempt publishes one
structural revision. Failure or cooperative cancellation closes detached observations and staged
ownership, disposes newly created resources and mounted effects, and leaves the prior committed
snapshot unchanged. Structural callbacks and lifecycle cleanup are synchronous, owner-thread,
non-reentrant operations and may not write application state directly.

`StructuralLocal` provides remembered non-null local state without registering failed draft slots
in the application `StateDomain`. Reads are attributed to the reading group, and an equality-changing
write outside every attempt invalidates only active readers. `BranchRetention.RETAIN` preserves
local memory and resources while hiding a branch, but detaches reactive and ambient edges and runs
effect cleanup; reactivation recaptures dependencies and remounts effects.

The structural `effect` operation is the minimal mount/cleanup primitive required to prove topology
ownership and failure cleanup in M1. Dependency-keyed effect updates, asynchronous work, and the
broader effect scheduling API remain in `EFFECT-001`.

## Boundaries, ambient values, and current measure

Application error boundaries use identity `ErrorBoundaryKey` values. The nearest healthy boundary
contains callback or effect-mount failure, and its fallback runs as a fresh atomic attempt. A failed
fallback becomes `ESCALATED` and targets its declared parent once. Retry is explicit through
`resetBoundary` or `resetRoot`. Debug and release configurations share topology, retry, ownership,
and cleanup behavior; release mode only redacts retained causes.

`AmbientKey` values use identity keys and nearest-override lookup. Structural readers attach to the
selected provider cell, so a nested provider shadows the outer dependency as well as its value.
`MeasureMaterializationKey` declares ADR-020's narrow measure-owned exception: `materialize` may
reconcile only bounded direct keyed children using current input. Successful children are visible in
the same revision; duplicate keys, failure, or an explicit cancellation checkpoint preserve the
last viewport.

## Ownership and scope

- `UiScheduler` is application-scoped and must be created on the shared owner thread of its
  `PlatformEventLoop` and `StateDomain`.
- `WindowFrameScheduler` is window-scoped. Each registered `WindowId` has an independent request
  generation, pending count, and host-redraw state. An open window receives at most one scheduler
  during its lifetime, preventing a stale queued redraw from reaching a replacement scheduler.
- Ordered `WindowEvent` values are routed explicitly through `UiScheduler.handleWindowEvent`.
  Routing selects only the matching window and never broadcasts invalidation.
- Platform event-loop, state-domain, window, and session closure remain the caller's responsibility.

## State ingress

`UiScheduler.enqueueStateUpdate` is a bounded any-thread entry point. The first pending callback
posts one owner-context drain. That drain detaches the complete scheduler-owned FIFO batch, appends
it to the domain external-commit queue, and publishes successful writes as at most one epoch. Each
callback receives an independent nested-transaction savepoint, so one failure rolls back only that
callback and does not stop later callbacks. Work submitted during a drain is assigned to a later
host callback and transaction.

The domain external-commit queue is application-scoped. A scheduler drain appends its detached
callbacks and then drains the shared queue, so it also executes any callbacks already pending there;
the scheduler's state-attempt and failure counters describe the complete returned queue batch.

The scheduler deliberately does not infer which windows observe a state publication. Reactive
consumers request frames from the affected window schedulers; a state commit never redraws every
window implicitly.

## Frame admission

`WindowFrameScheduler.requestFrame` may be called from any thread. Multiple requests before a host
redraw are represented by one `FrameTick` with the exact coalesced request count and latest request
generation. The frame samples the event-loop `FrameClock` once. Requests made while the callback is
running cause exactly one follow-up host redraw and are represented by the next tick.

Frame callbacks execute serially on the platform owner context. Callback failures and failed
deferred redraw requests are contained in the application's bounded diagnostic stream. They do not
escape into the platform event loop or prevent unrelated windows from advancing.

## Closure

Closing `UiScheduler` on the owner context outside every state transaction stops admission, cancels
its pending state-drain task, synchronously settles accepted state ingress, and closes every
registered window scheduler. Frame closure drops requests that have not entered a callback but does
not interrupt one already running. Scheduler closure does not close the event loop, state domain,
platform windows, or platform session.

## Animation transactions and publication

`AnimationRegistry` is application-scoped and owner-context confined for mutation. An
`AnimationTransaction` carries requested and effective motion, replacement policy, reduced-motion
disposition, scope, and trace identity explicitly; no global or thread-local animation context is
installed. `AnimationRegistry.commit` stages every scalar target before publishing it, so callback
or validation failure leaves model and presentation values unchanged and emits one `FAILED`
completion event.

Each `AnimatedScalar` keeps its authoritative committed model target separate from its current
presentation value and velocity. A successful animated commit publishes all model targets in one
registry epoch while retaining the prior presentation values. Sampling computes every active
timeline into reusable primitive arrays at one clock timestamp and publishes all successful results
at one presentation epoch. Sampling neither writes application `State` nor invokes application
effects or completion callbacks.

## Motion and interruption

The M1 reference path provides zero-delay or delayed cubic Bézier tweens, analytic underdamped,
critically damped, and overdamped physical springs, and immediate snap motion. Motion advances from
elapsed nanoseconds rather than frame count, so variable refresh and skipped frames do not replay
intermediate steps. The spring reference uses reproducible strict math and preserves value and
velocity when both interrupted and replacement motions support velocity retargeting. Incompatible
motion preserves value and resets velocity; explicit policies may restart or snap instead. A
gesture may supply initial velocity only to a compatible physical motion.

Requested motion remains recorded when accessibility policy substitutes a reduced or disabled
effective specification. Disabled motion snaps to the final model target and completes with a
`SKIPPED` outcome.

## Phase impact and completion

Each scalar declares an exact `AnimationPhaseImpact`. A changed sample unions only the affected
phases; a compositor-only scalar never reports structure, measure, place, or paint work. Phase masks
are canonicalized, and steady-state scalar sampling allocates no per-property objects.

Completion is bounded data, not code executed by the sampler. One reserved transaction group emits
exactly one `COMPLETED`, `REPLACED`, `CANCELLED`, `FAILED`, or `SKIPPED` event. The owner drains these
events at a later stabilization boundary, which releases the corresponding backpressure capacity.
Registry and property closure cancel active group members without closing the borrowed event loop.

This package intentionally stops at the `ANIM-CORE-001` scalar reference kernel. Fixed-width value
adapters, decay/keyframe/sequence specifications, implicit-property precedence, UI commit wiring,
authoritative animated hit testing, compositor program offload, structural transitions, and matched
geometry remain in later `MOUNT-001`, `ANIM-001`, and `TRANSITION-001` work.

## Conformance

Run the deterministic grouped-structure profile with:

```text
./gradlew -g .gradle-user-home :modules:runtime:structureConformance
```

Run the deterministic scheduler profile with:

```text
./gradlew -g .gradle-user-home :modules:runtime:conformance
```

Run the deterministic animation profile with:

```text
./gradlew -g .gradle-user-home :modules:runtime:animationConformance
```

All three tasks run their applicable unit, concurrency, and Pure Java guards before their
deterministic acceptance scenarios. They write `build/conformance/m1-structure/results.json`,
`build/conformance/m1-scheduler/results.json`, and `build/conformance/m1-animation/results.json`,
respectively.

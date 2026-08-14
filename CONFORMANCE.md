# HimariUI Conformance Policy

`PLATFORM_CONFORMANCE.yaml` is the machine-readable register for platform, target, renderer, and packaging profiles. This policy defines how a profile becomes release evidence. Merely adding a profile or running one command is not a pass.

The register uses the JSON subset of YAML 1.2 so the build can validate it without a YAML runtime dependency. Comments, anchors, aliases, tags, duplicate keys, and non-JSON scalar syntax are not permitted.

## Profile lifecycle

Each profile has exactly one status:

- `planned`: its contract is registered, but the implementation or required environments are incomplete;
- `active`: all commands and fixtures exist and the profile is collecting evidence;
- `passed`: every required environment passed the exact profile revision and all required evidence is recorded;
- `failed`: the current implementation or evidence violates the profile;
- `waived`: a reviewed, time-bounded waiver permits a specifically named failure without claiming that capability passed.

A profile marked `required: true` blocks its owning milestone while it is `planned`, `active`, `failed`, or `waived`, unless the milestone itself explicitly permits that waiver. Optional profiles never weaken a required profile and cannot be substituted for it.

## Profile contract

Every profile records:

- a stable `id`, owning `workPackage`, milestone, required flag, status, and owner role;
- target environments, including operating system/runtime and architecture where applicable;
- reproducible repository-relative commands and immutable fixture identifiers;
- required behavioral assertions and capability/fallback modes;
- numeric/image tolerances with units and comparison semantics;
- timeout, soak, and repetition durations;
- heap, native-memory, handle, and thread budgets;
- required and recorded evidence artifacts;
- reviewed waivers.

The initial budgets are conservative safety ceilings, not performance targets. A performance claim requires a separately reviewed benchmark budget. Tightening a tolerance or budget is compatible; weakening one requires evidence, review, and a profile revision.

## Commands and fixtures

Commands run from the repository root with the workspace-local Gradle home. A command named by a `planned` profile may refer to a task owned by a later work package; before the profile becomes `active`, every command and fixture must exist and be reproducible from a clean checkout.

Fixtures use stable IDs rather than transient build paths. Generated fixtures record their generator command and provenance. Oracle output is evidence, not production runtime input, and Oracle/native dependencies remain isolated from production configurations.

## Tolerances

`numericAbsolute` and `numericRelative` bound scalar comparisons. `imageMaxChannelDelta` is expressed in normalized destination-channel units. An exact protocol/ABI profile uses zero for all three. A nonzero tolerance must be justified by the profile's assertions and may not hide a categorical capability, format, lifetime, or validation failure.

## Durations and budgets

`timeoutSeconds` is the hard duration for one command attempt. `soakSeconds` is the continuous stability interval after setup. `repetitions` is the minimum number of independently initialized attempts.

Budgets count peak process resources attributable to the test profile. Driver- or compositor-owned allocations that cannot be measured portably must be named in evidence instead of silently excluded. A budget overrun fails the profile unless covered by a valid waiver.

## Evidence

Required evidence paths are repository-relative logical output names under `build/conformance/`; generated evidence itself is not committed unless a profile explicitly requires a reviewed golden or report. A `passed` profile must list a recorded artifact for every required artifact and record:

- source revision and dirty-state status;
- Java, Gradle, Native Image, OS, SDK, driver, and hardware versions as applicable;
- executed command and fixture IDs;
- timestamps, duration, budgets, and measured peaks;
- capability snapshots, fallback reasons, validation messages, and artifact hashes;
- reviewer identity and review date.

Evidence from a dirty tree, an unpinned Oracle, a different profile revision, or an undeclared environment cannot close a required profile.

## Waivers

A waiver contains a unique ID, exact failed assertion, issue reference, reviewer, approval and expiry dates, affected environments, and rationale. It must not broaden a profile, suppress unrelated failures, or relabel an unavailable capability as supported. Expired or incomplete waivers fail validation.

Security boundaries, native payload/runtime isolation, canonical decoder bounds, incorrect input semantics, data corruption, and uncontained native callback failures are not waivable for a release profile.

## Initial M0 profiles

The initial register covers:

- canonical FFI schema generation, typed FFM downcall/upcall execution, and C ABI comparison;
- Wayland window/event/surface presentation and Vulkan surface/device/swapchain presentation;
- Win32 window/`WndProc` and D3D12 device/swapchain presentation;
- macOS `NSWindow`/`CAMetalLayer` and Metal device/queue presentation;
- Objective-C block invocation, lifetime, reentrancy, and exception containment;
- Native Image execution using the same generated FFM bindings.

All initial M0 profiles begin as `planned`. Their existence satisfies registration and schema work only; M0 closes after the commands, fixtures, evidence, and cross-platform exit criteria pass.

## Updating the register

Change a profile in the same review as the implementation or evidence that motivates it. Preserve profile IDs. A semantic contract change increments `profileVersion`; a schema-wide policy change increments `policyVersion` and updates this document and the schema together. The `verifyPlatformConformance` task validates structure, required M0 coverage, lifecycle/evidence consistency, budgets, dates, and waivers.

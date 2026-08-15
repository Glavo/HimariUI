# RUNTIME-SAMPLE-001

This module freezes the ordinary-Java behavior suite, decision rubric, instrumentation API, and canonical report format used to compare the M1 grouped, one-shot, and hybrid structural-runtime prototypes.

It intentionally contains no shared structural runtime. Candidates may share `himari-state`, the Headless host, fixture commands, observations, and `ComparisonProbe`; they must independently implement grouping, anchoring, structural scopes, identity, and reconciliation.

Each candidate implements `RuntimeCandidate` and returns one `RuntimeFixtureSession` per fixture. The runner sends application-domain commands, drains the deterministic Headless event loop, compares the complete observation, checks required phases and diagnostic traces, then verifies post-close health and probe balance. Candidate application sources are compiled as ordinary Java and listed in `SourceCorpus` with auditable ceremony markers.

The checked-in fixture catalog contains twelve micro/integration behaviors plus the realistic settings-and-chat API charter. Capability-conditional steps cover both ADR-020 viewport strategies and cancellation only when claimed. Benchmarks run in independent sessions with a state-restoring command cycle.

Run the harness conformance profile with:

```text
./gradlew -g .gradle-user-home :spikes:runtime-sample:conformance
```

The task writes `build/conformance/m1-runtime-sample/suite.json`, `rubric.json`, and `self-test-report.json`. The self-test report uses an explicitly ineligible oracle replay; it validates the harness and must never be scored as a runtime candidate. Candidate reports conform to `schema/runtime-comparison-report.schema.json`.

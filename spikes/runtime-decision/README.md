# M1 structural-runtime decision

This module completes `RUNTIME-ADR-001` without promoting any spike implementation into production. It regenerates blinded ordinary-Java ceremony packets, validates the checked review against their hashes, builds and runs all three real candidates in one GraalVM Native Image executable, produces evidence-backed JVM reports in separate processes, applies the frozen `runtime-decision-rubric-v1` score, and verifies the selected architecture against the reviewed decision record.

Run the complete decision profile with:

```text
./gradlew -g .gradle-user-home :spikes:runtime-decision:conformance
```

The task requires `GRAALVM_HOME` or `-PnativeImageHome` to name a GraalVM 25 installation. Generated packets, Native Image build metadata, complete candidate reports, compact measurements, and the current decision report are written below `build/conformance/m1-runtime-decision`. The checked review and reviewed-run record live under `evidence/m1-runtime-decision`.

The candidate source and benchmark protocol remain owned by their original spike modules. This module may verify and select them, but it must not change the frozen fixture or rubric versions.

# M1 structural-runtime decision evidence

This directory contains the checked qualitative evidence that cannot be inferred from benchmark counters alone.

`ceremony-review.properties` records a review of freshly generated, mechanically redacted complete micro-source packets. Packet labels do not reveal candidate names. Their hashes, source counts, threshold results, and reviewer-count requirement are regenerated and verified by `:spikes:runtime-decision:ceremonyPackets` and the complete decision conformance task. None of the three candidates triggers the frozen mandatory three-person threshold, so one recorded review is sufficient.

`reviewed-decision.properties` records the accepted interpretation of the complete reports: three-run benchmark ranges, the exact environment-sensitive sample used for the reviewed score, material-outlier review, semantic capability differences, Pareto review, the selected candidate, and the frozen tie-break rule that selected it. Conformance combines that exact sample with freshly regenerated deterministic counters to reproduce the accepted score, then separately requires the current measurements to select the same candidate and rule. Generated full reports and the current score remain build artifacts because elapsed time and allocation samples are environment observations rather than deterministic goldens.

Run the complete evidence chain with:

```text
./gradlew -g .gradle-user-home :spikes:runtime-decision:conformance
```

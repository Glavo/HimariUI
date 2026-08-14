# Contributing to HimariUI

HimariUI accepts focused changes that preserve the architecture and evidence requirements in `PLANS.md`. Open an issue or design discussion before starting work that changes a public contract, an accepted ADR, a platform boundary, a file format, or a milestone exit criterion.

## Contribution license

Original HimariUI code is licensed under Apache-2.0. Unless explicitly marked otherwise, a contribution submitted for inclusion is licensed under the same terms (inbound equals outbound). The project currently requires neither a contributor license agreement nor a Developer Certificate of Origin sign-off.

Contributors must have the right to submit their work. Preserve upstream copyright, license, and NOTICE material. A rewrite, translation, generated table, minimized fixture, or AI-assisted implementation does not remove the obligation to record its source and license.

## Change requirements

Before requesting review:

1. Keep the change within one named work package or explain the dependency between packages.
2. Follow the repository's Java 25, nullability, documentation, FFM, JPMS, and pure-Java distribution rules.
3. Add deterministic tests or conformance evidence proportional to the behavior changed.
4. Update the applicable ADR, provenance entry, reference lock, schema, or conformance profile when its contract changes.
5. Run `./gradlew -g .gradle-user-home build` and every narrower command named by the affected conformance profile.
6. Describe known limitations and do not report inactive or unavailable capabilities as passing.

Changes to an accepted ADR require a new ADR that supersedes it. Editing an accepted ADR is limited to corrections that do not alter its decision; record material corrections in its evidence section.

## Ports and generated data

A port or behavioral translation must identify its specification or upstream implementation, pin the reviewed version in `REFERENCES.lock`, map relevant symbols, and record all incorporated files in `PROVENANCE.json`. Preserve a readable reference path and use an independent adversarial review role in addition to implementation and test authorship.

Generated bindings, tables, shaders, test fonts, and binary fixtures must be reproducible from a named command or carry a documented retrieval procedure and cryptographic hash. Do not commit proprietary system fonts, SDK binaries, or unredistributable fixtures.

## Review and commits

Keep commits coherent and reviewable. A pull request must identify its work package, tests, affected conformance profiles, and any reviewed waiver. Reviewers should reject changes that weaken a gate, hide a fallback, introduce an undeclared native/runtime dependency, or rely on a downstream deliverable whose exit criteria have not passed.
